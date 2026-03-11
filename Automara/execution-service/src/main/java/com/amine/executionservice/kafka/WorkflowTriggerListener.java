package com.amine.executionservice.kafka;


import com.amine.executionservice.model.WorkflowTriggerEvent;
import com.amine.executionservice.service.WorkflowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkflowTriggerListener {

    @Autowired
    private WorkflowExecutor workflowExecutor;

    /**
     * Listen to workflow-trigger topic
     * Consumes trigger events and starts workflow execution
     */
    @KafkaListener(
            topics = "workflow-trigger",
            groupId = "execution-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWorkflowTrigger(WorkflowTriggerEvent event) {
        log.info("Received workflow trigger event: {}", event);

        try {
            workflowExecutor.executeWorkflow(event);

        } catch (Exception e) {
            log.error("Error handling workflow trigger: {}", e.getMessage(), e);

        }
    }
}