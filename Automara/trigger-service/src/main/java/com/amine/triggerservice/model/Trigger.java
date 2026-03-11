package com.amine.triggerservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "triggers")
@Data
public class Trigger {

    @Id
    @Column(name = "trigger_id")
    private String triggerId;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType; // cron, webhook, manual

    @Column(name = "config", columnDefinition = "TEXT")
    private String config; // JSON

    @Column(name = "status", nullable = false)
    private String status; // ACTIVE, PAUSED, DISABLED

    @Column(name = "next_execution")
    private LocalDateTime nextExecution;

    @Column(name = "last_execution")
    private LocalDateTime lastExecution;

    @Column(name = "execution_count")
    private Long executionCount = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}