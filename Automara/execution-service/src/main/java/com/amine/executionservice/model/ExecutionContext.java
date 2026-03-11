package com.amine.executionservice.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class ExecutionContext {
    private String executionId;
    private String workflowId;
    private String userId;
    private LocalDateTime startTime;
    private Map<String, Object> nodeOutputs;  // nodeId -> output data
    private Map<String, NodeExecutionStatus> nodeStatuses;

    public ExecutionContext(String executionId, String workflowId, String userId) {
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.userId = userId;
        this.startTime = LocalDateTime.now();
        this.nodeOutputs = new HashMap<>();
        this.nodeStatuses = new HashMap<>();
    }

    public void setNodeOutput(String nodeId, Object output) {
        this.nodeOutputs.put(nodeId, output);
    }

    public Object getNodeOutput(String nodeId) {
        return this.nodeOutputs.get(nodeId);
    }

    public void setNodeStatus(String nodeId, NodeExecutionStatus status) {
        this.nodeStatuses.put(nodeId, status);
    }

    @Data
    public static class NodeExecutionStatus {
        private String status;  // SUCCESS, FAILED, RUNNING
        private String error;
        private long durationMs;
        private LocalDateTime executedAt;
    }
}