# OpsPilot Agent

OpsPilot is a Java 21 operations copilot that helps on-call engineers understand
stuck orders, inspect workflow state, find the relevant runbook guidance, and
safely request a retry or ticket through a single conversational interface.

## Why it matters

Built a grounded operations-assistance service that combines order data,
workflow state, runbook retrieval, and deterministic tool selection to reduce
the context-switching required during an incident. The design keeps every
recommendation traceable to observed data and cited internal guidance, while
guarding workflow retries so only `STUCK` and `FAILED` runs can be re-queued.

For the on-call question _“Why is ORD-1001 stuck and can you retry it?”_,
OpsPilot identifies the order, reads its latest workflow, performs an
idempotent retry when allowed, retrieves runbook context, and returns an
answer with its sources and decision trace.

## Architecture at a glance

```text
Browser chat / JSON / SSE
          |
     AgentService
      /     |      \
OpsTools   RAG    ConversationMemory
   |        |            |
 JPA     local ONNX     last 6 messages
   |
H2 (default) or PostgreSQL
```

The same six operational tools are also intended to be published to external
MCP clients. See the full [HLD and LLD](docs/architecture.md) for contracts,
data model, profiles, state transitions, and delivery plan.

## Capabilities

- Looks up orders and their latest workflow run.
- Retries only `STUCK` or `FAILED` workflows; repeated retries after the state
  changes to `RETRYING` are no-ops.
- Raises operational tickets and lists active incidents.
- Chunks, hashes, embeds, and searches Markdown runbooks in memory.
- Plans deterministic tool calls from order IDs and incident/retry keywords.
- Retains the most recent six user/assistant messages per in-process session.

## Tech stack

- Java 21, Spring Boot 3.5, Spring Web MVC, Spring Data JPA, Actuator
- Spring AI Transformers for local ONNX embeddings and Spring AI MCP server
- H2 for local development; PostgreSQL profile for persistent domain rows
- Maven, Docker, and GitHub Actions (delivery assets are planned)

## Current status

Implemented in the current codebase: application bootstrap and profiles,
JPA entities/repositories, five seeded orders, six domain tools, RAG ingestion
and cosine retrieval primitives, deterministic agent orchestration, and
in-process conversation memory.

Still planned: the browser UI, REST/SSE chat controller, MCP callback
configuration, bundled runbook Markdown files, Docker/Compose/CI assets, and
the test suite described in the design. The project intentionally uses an
offline deterministic answer composer until an approved LLM client is added.

## Run locally

Requirements: Java 21+ and Maven 3.9+.

```bash
mvn verify
mvn spring-boot:run
```

The default profile uses an in-memory H2 database on port `8080`. The local
embedding model is downloaded and cached on first startup. Use the `pgvector`
profile to point domain storage at PostgreSQL:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=pgvector
```

> The HTTP chat endpoints and browser experience are documented but not yet
> committed; consult the roadmap before treating this as a production service.

## Roadmap

1. Add the runbook corpus and complete the API/SSE/browser chat experience.
2. Publish the domain tools through the MCP callback provider.
3. Add unit, context, and embedding retrieval tests.
4. Add Docker Compose for PostgreSQL/pgvector and a GitHub Actions verification
   workflow.
5. Add an approved production LLM adapter, authentication, authorization, and
   audit logging.

