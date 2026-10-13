package com.deepanshuraj.opspilot.domain;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkflowRunRepository extends JpaRepository<WorkflowRun,Long>{ Optional<WorkflowRun> findFirstByOrderIdOrderByUpdatedAtDesc(String orderId); List<WorkflowRun> findByStateInOrderByUpdatedAtDesc(List<String> states); }
