package com.amine.executionservice.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTriggerEvent {
    private String workflowId;
    private String executionId;
    private String userId;
    private String triggeredBy;  // "cron", "webhook", "manual"
    private LocalDateTime timestamp;
}