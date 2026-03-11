package com.amine.triggerservice.controller;

import com.amine.triggerservice.model.WorkflowTriggerEvent;
import com.amine.triggerservice.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class WebhookController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Webhook endpoint
     */
    @PostMapping("/{workflowId}")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @PathVariable String workflowId,
            @RequestParam(required = false) String userId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        log.info("Received webhook for workflow: {}", workflowId);

        WorkflowTriggerEvent event = new WorkflowTriggerEvent();
        event.setWorkflowId(workflowId);
        event.setExecutionId("exec-" + UUID.randomUUID().toString());
        event.setUserId(userId != null ? userId : "webhook-user");
        event.setTriggeredBy("webhook");
        event.setTimestamp(LocalDateTime.now());
        event.setTriggerData(payload);

        kafkaProducerService.sendTriggerEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Workflow triggered",
                "executionId", event.getExecutionId()
        ));
    }
}
