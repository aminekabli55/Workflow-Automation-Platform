package com.amine.triggerservice.service;

import com.amine.triggerservice.model.Trigger;
import com.amine.triggerservice.model.TriggerConfig;
import com.amine.triggerservice.model.WorkflowTriggerEvent;
import com.amine.triggerservice.repository.TriggerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TriggerManagementService {

    @Autowired
    private TriggerRepository triggerRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a new trigger
     */
    public Trigger createTrigger(String workflowId, String userId, String triggerType, TriggerConfig config) {
        try {
            Trigger trigger = new Trigger();
            trigger.setTriggerId("trigger-" + UUID.randomUUID().toString());
            trigger.setWorkflowId(workflowId);
            trigger.setUserId(userId);
            trigger.setTriggerType(triggerType);
            trigger.setConfig(objectMapper.writeValueAsString(config));
            trigger.setStatus("ACTIVE");
            trigger.setCreatedAt(LocalDateTime.now());
            trigger.setUpdatedAt(LocalDateTime.now());
            trigger.setExecutionCount(0L);

            // Calculate next execution for cron triggers
            if ("cron".equals(triggerType) && config.getCronExpression() != null) {
                CronExpression cron = CronExpression.parse(config.getCronExpression());
                trigger.setNextExecution(cron.next(LocalDateTime.now()));
            }

            triggerRepository.save(trigger);

            log.info("Created trigger: {} for workflow: {}", trigger.getTriggerId(), workflowId);
            return trigger;

        } catch (Exception e) {
            log.error("Error creating trigger: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create trigger", e);
        }
    }

    /**
     * Trigger workflow manually
     */
    public void triggerWorkflowManually(String workflowId, String userId) {
        log.info("Manually triggering workflow: {}", workflowId);

        WorkflowTriggerEvent event = new WorkflowTriggerEvent();
        event.setWorkflowId(workflowId);
        event.setExecutionId("exec-" + UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setTriggeredBy("manual");
        event.setTimestamp(LocalDateTime.now());

        kafkaProducerService.sendTriggerEvent(event);
    }

    /**
     * Get all triggers for a workflow
     */
    public List<Trigger> getWorkflowTriggers(String workflowId) {
        return triggerRepository.findByWorkflowId(workflowId);
    }

    /**
     * Get all triggers for a user
     */
    public List<Trigger> getUserTriggers(String userId) {
        return triggerRepository.findByUserId(userId);
    }

    /**
     * Update trigger status
     */
    public Trigger updateTriggerStatus(String triggerId, String status) {
        Trigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new RuntimeException("Trigger not found"));

        trigger.setStatus(status);
        trigger.setUpdatedAt(LocalDateTime.now());

        triggerRepository.save(trigger);

        log.info("Updated trigger {} status to: {}", triggerId, status);
        return trigger;
    }

    /**
     * Delete trigger
     */
    public void deleteTrigger(String triggerId) {
        triggerRepository.deleteById(triggerId);
        log.info("Deleted trigger: {}", triggerId);
    }
}
