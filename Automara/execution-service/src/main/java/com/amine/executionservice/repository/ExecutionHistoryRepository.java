package com.amine.executionservice.repository;


import com.amine.executionservice.model.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, String> {

    List<ExecutionHistory> findByWorkflowIdOrderByStartTimeDesc(String workflowId);

    List<ExecutionHistory> findByUserIdOrderByStartTimeDesc(String userId);
}