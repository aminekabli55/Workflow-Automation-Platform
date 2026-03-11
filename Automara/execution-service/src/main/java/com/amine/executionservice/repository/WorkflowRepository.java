package com.amine.executionservice.repository;


 import com.amine.executionservice.model.WorkflowDefinition;
 import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, String> {

    Optional<WorkflowDefinition> findByWorkflowIdAndStatus(String workflowId, String status);
}