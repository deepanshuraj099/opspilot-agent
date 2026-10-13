package com.deepanshuraj.opspilot.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="workflow_runs") public class WorkflowRun {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; private String orderId; private String currentStep; private String state; private String stuckReason; private int attempts; private Instant updatedAt;
 protected WorkflowRun(){} public WorkflowRun(String order,String step,String state,int attempts,String reason,Instant at){this.orderId=order;this.currentStep=step;this.state=state;this.attempts=attempts;this.stuckReason=reason;this.updatedAt=at;}
 public String getOrderId(){return orderId;} public String getCurrentStep(){return currentStep;} public String getState(){return state;} public String getStuckReason(){return stuckReason;} public int getAttempts(){return attempts;} public Instant getUpdatedAt(){return updatedAt;}
 public void retry(){state="RETRYING";attempts++;stuckReason=null;updatedAt=Instant.now();}
}
