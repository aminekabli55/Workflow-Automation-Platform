package com.amine.executionservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "execution_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionHistory {

    @Id
    private String executionId;

    @Column(nullable = false)
    private String workflowId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String status;  // SUCCESS, FAILED, RUNNING

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> nodeResults;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}