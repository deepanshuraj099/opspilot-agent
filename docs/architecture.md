# OpsPilot Architecture

## Purpose and scope

OpsPilot is a single-process Java 21 Spring Boot operations assistant. It lets
an on-call engineer ask a natural-language question about an order, workflow,
incident, ticket, or runbook. The agent detects an `ORD-<digits>` identifier,
uses domain tools to assemble observations, searches a local runbook index,
and composes a grounded response. It keeps the last six conversation entries
for each in-memory session.

The design intentionally does **not** let a live LLM choose tools. `AgentService`
uses regex and keyword rules. A retry only changes a local workflow row to
`RETRYING`; it does not call a real orchestrator. Embeddings are searched in
JVM memory even with the `pgvector` profile. Authentication, authorization,
audit persistence, and durable memory are out of scope.

## HLD: system context and flow

Actors are the on-call engineer, the OpsPilot agent, an external MCP client,
the domain store (`customer_orders`, `workflow_runs`, `ops_tickets`), and the
classpath runbook corpus. The browser chat calls JSON or SSE endpoints; the
agent calls JPA-backed tools, an in-memory RAG retriever, conversation memory,
and an answer composer. MCP clients call the same tools directly.

```text
Browser -> ChatController -> AgentService
                              |- OpsTools -> JPA -> H2 / PostgreSQL
                              |- RagRetriever -> local embedding index
                              |- ConversationMemory -> JVM map, 6 entries
                              `- LlmClient -> TemplateLlmClient by default

MCP client -> Spring AI MCP server -> OpsTools
```

One chat turn: the browser generates a session UUID and opens
`GET /api/chat/stream?sessionId&message`. The controller invokes
`AgentService.answer`. The agent extracts the first order ID, gathers order
and workflow facts, conditionally retries or opens a ticket, searches runbooks,
builds a prompt including history from before the current turn, then stores the
user and assistant messages. The SSE controller replays collected decision
steps at 120 ms intervals and sends the final result. `POST /api/chat` returns
the same result synchronously.

## LLD: agent planning and public interfaces

`AgentResult` contains `sessionId`, `question`, ordered `AgentStep` values,
the answer, distinct source citations, latency, and provider. Steps are
`THOUGHT`, `ACTION`, and `OBSERVATION` records.

`AgentService` uses the first case-insensitive `ORD-\\d+` match. With an order
ID it calls `lookupOrder` and `getWorkflowStatus`. It also calls
`retryStuckWorkflow` for `retry`, `re-run`, `rerun`, `fix`, `resolve`,
`unstick`, `re-queue`, or `requeue`; and calls `raiseTicket` for `ticket`,
`raise`, `escalate`, or `log a`. Without an order ID, incident terms such as
`incident`, `stuck order`, `failing`, `what is broken`, or `outage` call
`listRecentIncidents`. RAG always runs using the raw question; the default top
K is four.

`ConversationMemory` is a `ConcurrentHashMap<String, Deque<String>>`. It
synchronizes individual deques, stores `role: text`, and evicts oldest items
after six entries. It has no TTL, authentication, or restart persistence.

The default `TemplateLlmClient` is explicitly deterministic: it formats
observations and retrieved guidance and returns its provider as
`template (offline, deterministic)`. The `openai` profile is documentation for
a future adapter; this repository does not include one.

## Domain model and tool contracts

`CustomerOrder` uses business key `order_id`; `WorkflowRun` has an identity
key and is selected by newest `updatedAt`; `OpsTicket` is insert-only with an
identity key. There is deliberately no entity foreign key between them.

| Tool | Behavior |
| --- | --- |
| `lookupOrder(orderId)` | Returns customer, product, region, status, and creation time; unknown IDs return a not-found sentence. |
| `getWorkflowStatus(orderId)` | Returns latest step, state, attempts, reason, and update time. |
| `retryStuckWorkflow(orderId)` | Transactionally changes only `STUCK` or `FAILED` to `RETRYING`, increments attempts, clears the reason, and updates time. Other states are no-ops. |
| `raiseTicket(orderId, summary)` | Transactionally inserts a ticket. Priority is P1 only when the summary contains `sla`; otherwise P2. |
| `listRecentIncidents()` | Lists `STUCK` and `FAILED` workflows newest first. |
| `searchKnowledgeBase(query)` | Returns three highest-scoring runbook chunks separated by `---`. |

Seed data contains five orders: `ORD-1001` is stuck at `PROVISION_ACCESS`,
`ORD-1002` failed at `SEND_NOTIFICATION`, `ORD-1003` is completed,
`ORD-1004` is running, and `ORD-1005` is cancelled. A retry moves a stuck or
failed row to `RETRYING`; no transition back is implemented.

## RAG design

At startup, `RagRetriever` reads `classpath:knowledge/*.md`, normalizes and
chunks each document (700 characters with 120-character overlap), SHA-256
deduplicates chunks, embeds unique text with Spring AI's local transformer
model, and retains `KnowledgeChunk(source, text, vector)` records in memory.
Search embeds the query, scores every chunk with cosine similarity, sorts
descending, and returns the requested count without a score threshold.

The intended corpus comprises `order-lifecycle.md`, `stuck-order-runbook.md`,
`retry-policy.md`, `notification-troubleshooting.md`, and
`escalation-and-sla.md`. These files explain workflow states, retry/backoff
guidance, notification remediation, and SLA-based escalation. They are not yet
committed, so a full application startup currently requires that delivery
milestone.

## Runtime profiles and operations

The default profile uses H2 in PostgreSQL compatibility mode, `create-drop`,
disabled Open Session in View, the H2 console, template responses, and port
8080. The `pgvector` profile uses PostgreSQL with Hibernate `update`; its name
does not imply vector persistence. The documented `openai` profile selects
`gpt-4o-mini`, but no OpenAI client bean exists.

Actuator exposes `health`, `info`, and `metrics`; health details are enabled.
The delivery design calls for a multi-stage Temurin 21 Docker image, a
PostgreSQL pgvector Compose service, and GitHub Actions running `mvn -B verify`.
Those deployment files are planned rather than implemented.

## MCP and web delivery status

The dependency configuration includes Spring AI's WebMVC MCP server starter,
and each domain method is annotated as a tool. The remaining configuration is
an `McpToolConfig` bean using `MethodToolCallbackProvider` to publish
`OpsTools` under MCP server name `opspilot-mcp`, version `0.1.0`.

The remaining browser/API deliverable is a `ChatController` with `/api/chat`,
`/api/chat/stream`, and `/api/info`, plus `index.html`, `styles.css`, and
`app.js`. SSE must have a 60-second timeout and replay completed agent steps;
this is decision-trace streaming, not token streaming.

## Error, safety, and test expectations

Unknown order IDs produce tool-specific not-found text; a retry or ticket can
still be requested for an unknown ID. Empty observations and RAG context yield
a fixed cannot-find-guidance answer. SSE work continues after a client
disconnect. A cold local model download may exceed the SSE timeout.

Required tests are: agent planning and retry idempotency, Spring context/tool
contracts, chunk overlap/SHA-256 stability/full coverage, and live embedding
retrieval metrics. Production hardening requires role checks, audit events,
input limits, durable memory, and integration with a real workflow executor.
