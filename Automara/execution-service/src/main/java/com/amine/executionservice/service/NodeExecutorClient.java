package com.amine.executionservice.service;


import com.amine.executionservice.model.ExecutionResult;
import com.amine.executionservice.model.NodeExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class NodeExecutorClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;


    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ExecutionResult executeNode(NodeExecutionRequest request) {
        try {
            String serviceUrl = getServiceUrl(request.getNodeType());
            String executeUrl = serviceUrl + "/execute";

            log.info("Executing node {} of type {} at URL: {}",
                    request.getNodeId(), request.getNodeType(), executeUrl);

            ExecutionResult result = restTemplate.postForObject(
                    executeUrl,
                    request,
                    ExecutionResult.class
            );

            log.info("Node {} executed successfully", request.getNodeId());
            return result;

        } catch (Exception e) {
            log.error("Failed to execute node {}: {}", request.getNodeId(), e.getMessage());
            return ExecutionResult.failure("Node execution failed: " + e.getMessage());
        }
    }

    /**
     * Get service URL using Eureka service discovery
     * Falls back to hardcoded URLs if service discovery fails
     */
    private String getServiceUrl(String nodeType) {
        String serviceName = nodeType + "-service";

//        try {
//            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
//            if (!instances.isEmpty()) {
//                ServiceInstance instance = instances.get(0);
//                return instance.getUri().toString();
//            }
//        } catch (Exception e) {
//            log.warn("Service discovery failed for {}, using fallback URL", serviceName);
//        }

        // Fallback to hardcoded URLs (for development)
        return switch (nodeType) {
            case "gmail" -> "http://localhost:8091/api/gmail";
            case "gemini" -> "http://localhost:8092";
            case "http" -> "http://localhost:8093";
            case "trigger" -> "http://localhost:8083";
            default -> throw new IllegalArgumentException("Unknown node type: " + nodeType);
        };
    }
}