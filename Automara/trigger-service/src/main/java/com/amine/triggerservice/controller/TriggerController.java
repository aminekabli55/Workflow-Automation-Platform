package com.amine.triggerservice.controller;

import com.amine.triggerservice.model.Trigger;
import com.amine.triggerservice.model.TriggerConfig;
import com.amine.triggerservice.service.TriggerManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/triggers")
@Slf4j
public class TriggerController {

    @Autowired
    private TriggerManagementService triggerManagementService;

    /**
     * Create a new trigger
     */
    @PostMapping
    public ResponseEntity<Trigger> createTrigger(@RequestBody Map<String, Object> request) {
        String workflowId = (String) request.get("workflowId");
        String userId = (String) request.get("userId");
        String triggerType = (String) request.get("triggerType");

        // Parse config
        TriggerConfig config = new TriggerConfig();
        Map<String, Object> configMap = (Map<String, Object>) request.get("config");

        if (configMap != null) {
            if ("cron".equals(triggerType)) {
                config.setCronExpression((String) configMap.get("cronExpression"));
                config.setTimezone((String) configMap.getOrDefault("timezone", "UTC"));
            } else if ("webhook".equals(triggerType)) {
                config.setWebhookUrl((String) configMap.get("webhookUrl"));
                config.setWebhookSecret((String) configMap.get("webhookSecret"));
            }
        }

        Trigger trigger = triggerManagementService.createTrigger(workflowId, userId, triggerType, config);
        return ResponseEntity.ok(trigger);
    }

    /**
     * Manual trigger
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, String>> triggerManually(@RequestBody Map<String, String> request) {
        String workflowId = request.get("workflowId");
        String userId = request.get("userId");

        triggerManagementService.triggerWorkflowManually(workflowId, userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Workflow triggered"));
    }

    /**
     * Get workflow triggers
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<Trigger>> getWorkflowTriggers(@PathVariable String workflowId) {
        List<Trigger> triggers = triggerManagementService.getWorkflowTriggers(workflowId);
        return ResponseEntity.ok(triggers);
    }

    /**
     * Get user triggers
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Trigger>> getUserTriggers(@PathVariable String userId) {
        List<Trigger> triggers = triggerManagementService.getUserTriggers(userId);
        return ResponseEntity.ok(triggers);
    }

    /**
     * Update trigger status
     */
    @PatchMapping("/{triggerId}/status")
    public ResponseEntity<Trigger> updateStatus(
            @PathVariable String triggerId,
            @RequestBody Map<String, String> request
    ) {
        String status = request.get("status");
        Trigger trigger = triggerManagementService.updateTriggerStatus(triggerId, status);
        return ResponseEntity.ok(trigger);
    }

    /**
     * Delete trigger
     */
    @DeleteMapping("/{triggerId}")
    public ResponseEntity<Void> deleteTrigger(@PathVariable String triggerId) {
        triggerManagementService.deleteTrigger(triggerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Trigger Service is running");
    }
}