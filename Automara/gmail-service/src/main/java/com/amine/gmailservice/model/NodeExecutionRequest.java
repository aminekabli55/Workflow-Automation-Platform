package com.amine.gmailservice.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionRequest {
    private String nodeId;
    private String nodeType;
    private Map<String, Object> config;
    private Map<String, Object> inputs;
    private String userId;
}