package com.amine.triggerservice.service;

import com.amine.triggerservice.model.Trigger;
import com.amine.triggerservice.model.TriggerConfig;
import com.amine.triggerservice.model.WorkflowTriggerEvent;
import com.amine.triggerservice.repository.TriggerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CronSchedulerService {

    @Autowired
    private TriggerRepository triggerRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check for due cron triggers every minute
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkDueTriggers() {
        log.debug("Checking for due cron triggers...");

        LocalDateTime now = LocalDateTime.now();
        List<Trigger> dueTriggers = triggerRepository.findDueCronTriggers(now);

        if (!dueTriggers.isEmpty()) {
            log.info("Found {} due triggers", dueTriggers.size());
        }

        for (Trigger trigger : dueTriggers) {
            try {
                executeTrigger(trigger);
            } catch (Exception e) {
                log.error("Error executing trigger {}: {}", trigger.getTriggerId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Execute a trigger
     */
    private void executeTrigger(Trigger trigger) {
        try {
            log.info("Executing trigger: {} for workflow: {}",
                    trigger.getTriggerId(), trigger.getWorkflowId());

            // Create trigger event
            WorkflowTriggerEvent event = new WorkflowTriggerEvent();
            event.setWorkflowId(trigger.getWorkflowId());
            event.setExecutionId("exec-" + UUID.randomUUID().toString());
            event.setUserId(trigger.getUserId());
            event.setTriggeredBy("cron");
            event.setTimestamp(LocalDateTime.now());

            // Send to Kafka
            kafkaProducerService.sendTriggerEvent(event);

            // Update trigger
            trigger.setLastExecution(LocalDateTime.now());
            trigger.setExecutionCount(trigger.getExecutionCount() + 1);

            // Calculate next execution
            TriggerConfig config = objectMapper.readValue(trigger.getConfig(), TriggerConfig.class);
            LocalDateTime nextExecution = calculateNextExecution(config.getCronExpression());
            trigger.setNextExecution(nextExecution);

            trigger.setUpdatedAt(LocalDateTime.now());
            triggerRepository.save(trigger);

            log.info("Trigger executed successfully. Next execution: {}", nextExecution);

        } catch (Exception e) {
            log.error("Error executing trigger: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate next execution time from cron expression
     */
    private LocalDateTime calculateNextExecution(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            return cron.next(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error parsing cron expression: {}", e.getMessage());
            return LocalDateTime.now().plusDays(1);
        }
    }
}