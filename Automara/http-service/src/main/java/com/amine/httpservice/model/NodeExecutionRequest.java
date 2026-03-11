package com.amine.httpservice.model;

import lombok.Data;
import java.util.Map;

@Data
public class NodeExecutionRequest {
    private String nodeId;
    private String nodeType;
    private Map<String, Object> config;
    private Map<String, Object> inputs;
    private String userId;
}