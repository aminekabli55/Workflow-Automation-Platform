# 🎯 Building the Execution Service - Complete Guide

## Table of Contents
1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Maven Dependencies](#maven-dependencies)
4. [Core Components](#core-components)
5. [Step-by-Step Implementation](#step-by-step-implementation)
6. [Testing](#testing)
7. [Docker Setup](#docker-setup)

---

## Overview

The **Execution Service** is the orchestrator that:
- ✅ Consumes workflow trigger events from Kafka
- ✅ Fetches workflow definitions from PostgreSQL
- ✅ Builds execution graph (DAG) from nodes and edges
- ✅ Executes nodes in correct order
- ✅ Manages execution context (data passing between nodes)
- ✅ Handles errors and retries
- ✅ Stores execution history

---

## Project Structure

```
execution-service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── workflow/
│   │   │           └── execution/
│   │   │               ├── ExecutionServiceApplication.java
│   │   │               ├── config/
│   │   │               │   ├── KafkaConsumerConfig.java
│   │   │               │   └── RestTemplateConfig.java
│   │   │               ├── model/
│   │   │               │   ├── WorkflowDefinition.java
│   │   │               │   ├── Node.java
│   │   │               │   ├── Edge.java
│   │   │               │   ├── ExecutionContext.java
│   │   │               │   ├── ExecutionResult.java
│   │   │               │   └── NodeExecutionRequest.java
│   │   │               ├── service/
│   │   │               │   ├── WorkflowExecutor.java
│   │   │               │   ├── GraphBuilder.java
│   │   │               │   ├── TemplateResolver.java
│   │   │               │   └── NodeExecutorClient.java
│   │   │               ├── kafka/
│   │   │               │   └── WorkflowTriggerListener.java
│   │   │               └── repository/
│   │   │                   ├── WorkflowRepository.java
│   │   │                   └── ExecutionHistoryRepository.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── schema.sql
│   └── test/
│       └── java/
│           └── com/
│               └── workflow/
│                   └── execution/
│                       └── service/
│                           └── WorkflowExecutorTest.java
└── Dockerfile
```

---

## Maven Dependencies

**pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.workflow</groupId>
    <artifactId>execution-service</artifactId>
    <version>1.0.0</version>
    <name>Execution Service</name>
    <description>Workflow Execution Orchestrator</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Spring Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Spring Cloud Netflix Eureka Client (Service Discovery) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Spring Boot Actuator (Health Checks) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok (Reduce Boilerplate) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Jackson for JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Apache Commons Lang (Utilities) -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- Spring Retry (For Retry Logic) -->
        <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>

        <!-- Spring AOP (Required for Retry) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Core Components

### 1. Main Application Class

**ExecutionServiceApplication.java**

```java
package com.workflow.execution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableDiscoveryClient  // Enable Eureka service registration
@EnableRetry           // Enable @Retryable annotations
public class ExecutionServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ExecutionServiceApplication.class, args);
    }
}
```

---

### 2. Configuration Files

**application.yml**

```yaml
server:
  port: 8082

spring:
  application:
    name: execution-service
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: execution-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# Eureka Configuration
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

# Custom Configuration
workflow:
  execution:
    max-retries: 3
    retry-delay-ms: 1000
```

---

### 3. Model Classes

**WorkflowDefinition.java**

```java
package com.workflow.execution.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "workflows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {
    
    @Id
    private String workflowId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String userId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Node> nodes;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Edge> edges;
    
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, PAUSED, ARCHIVED
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**Node.java**

```java
package com.workflow.execution.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private String id;
    private String type;  // "trigger", "gmail", "gemini", "http"
    private Map<String, Object> config;
    private Position position;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private int x;
        private int y;
    }
}
```

**Edge.java**

```java
package com.workflow.execution.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Edge {
    private String from;
    private String to;
}
```

**ExecutionContext.java**

```java
package com.workflow.execution.model;

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
```

**ExecutionResult.java**

```java
package com.workflow.execution.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private boolean success;
    private Map<String, Object> output;
    private String error;
    
    public static ExecutionResult success(Map<String, Object> output) {
        return new ExecutionResult(true, output, null);
    }
    
    public static ExecutionResult failure(String error) {
        return new ExecutionResult(false, null, error);
    }
}
```

**NodeExecutionRequest.java**

```java
package com.workflow.execution.model;

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
    private Map<String, Object> inputs;  // Data from previous nodes
    private String userId;
}
```

**WorkflowTriggerEvent.java**

```java
package com.workflow.execution.model;

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
```

**ExecutionHistory.java**

```java
package com.workflow.execution.model;

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
```

---

### 4. Repository Classes

**WorkflowRepository.java**

```java
package com.workflow.execution.repository;

import com.workflow.execution.model.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, String> {
    
    Optional<WorkflowDefinition> findByWorkflowIdAndStatus(String workflowId, String status);
}
```

**ExecutionHistoryRepository.java**

```java
package com.workflow.execution.repository;

import com.workflow.execution.model.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, String> {
    
    List<ExecutionHistory> findByWorkflowIdOrderByStartTimeDesc(String workflowId);
    
    List<ExecutionHistory> findByUserIdOrderByStartTimeDesc(String userId);
}
```

---

### 5. Service Classes - The Core Logic

**GraphBuilder.java** - Builds execution order from nodes and edges

```java
package com.workflow.execution.service;

import com.workflow.execution.model.Edge;
import com.workflow.execution.model.Node;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphBuilder {
    
    /**
     * Build execution order using Topological Sort (Kahn's Algorithm)
     * Ensures nodes are executed in dependency order
     */
    public List<String> buildExecutionOrder(List<Node> nodes, List<Edge> edges) {
        // Build adjacency list and in-degree map
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        // Initialize all nodes
        for (Node node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        
        // Build graph from edges
        for (Edge edge : edges) {
            graph.get(edge.getFrom()).add(edge.getTo());
            inDegree.put(edge.getTo(), inDegree.get(edge.getTo()) + 1);
        }
        
        // Topological sort using Kahn's algorithm
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<String> executionOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            executionOrder.add(nodeId);
            
            for (String neighbor : graph.get(nodeId)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // Check for cycles
        if (executionOrder.size() != nodes.size()) {
            throw new IllegalStateException("Workflow contains a cycle - cannot execute");
        }
        
        return executionOrder;
    }
    
    /**
     * Get the trigger node (node with no incoming edges)
     */
    public String getTriggerNode(List<Node> nodes, List<Edge> edges) {
        Set<String> nodesWithIncomingEdges = new HashSet<>();
        for (Edge edge : edges) {
            nodesWithIncomingEdges.add(edge.getTo());
        }
        
        for (Node node : nodes) {
            if (!nodesWithIncomingEdges.contains(node.getId())) {
                return node.getId();
            }
        }
        
        throw new IllegalStateException("No trigger node found");
    }
}
```

**TemplateResolver.java** - Resolves variable templates like {{node-2.output.body}}

```java
package com.workflow.execution.service;

import com.workflow.execution.model.ExecutionContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateResolver {
    
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Resolve template variables in node config
     * Example: "{{node-2.output.subject}}" -> "Project Update from Client"
     */
    public Map<String, Object> resolveConfig(Map<String, Object> config, ExecutionContext context) {
        Map<String, Object> resolved = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof String) {
                resolved.put(entry.getKey(), resolveString((String) value, context));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                resolved.put(entry.getKey(), resolveConfig(nestedMap, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        
        return resolved;
    }
    
    /**
     * Resolve a single string template
     */
    private String resolveString(String template, ExecutionContext context) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String replacement = resolveExpression(expression, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Resolve a single expression like "node-2.output.subject"
     */
    private String resolveExpression(String expression, ExecutionContext context) {
        String[] parts = expression.split("\\.");
        
        if (parts.length < 3) {
            return "{{" + expression + "}}";  // Cannot resolve
        }
        
        String nodeId = parts[0];  // "node-2"
        // Skip "output" part - parts[1]
        String field = parts[2];   // "subject"
        
        Object nodeOutput = context.getNodeOutput(nodeId);
        if (nodeOutput == null) {
            return "{{" + expression + "}}";  // Node not executed yet
        }
        
        // Extract field from output
        if (nodeOutput instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputMap = (Map<String, Object>) nodeOutput;
            Object fieldValue = outputMap.get(field);
            return fieldValue != null ? fieldValue.toString() : "";
        }
        
        return "";
    }
}
```

**NodeExecutorClient.java** - Makes REST calls to integration services

```java
package com.workflow.execution.service;

import com.workflow.execution.model.ExecutionResult;
import com.workflow.execution.model.NodeExecutionRequest;
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
    
    /**
     * Execute a node by calling its integration service
     * Retries up to 3 times on failure
     */
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
        
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (!instances.isEmpty()) {
                ServiceInstance instance = instances.get(0);
                return instance.getUri().toString();
            }
        } catch (Exception e) {
            log.warn("Service discovery failed for {}, using fallback URL", serviceName);
        }
        
        // Fallback to hardcoded URLs (for development)
        return switch (nodeType) {
            case "gmail" -> "http://localhost:8091";
            case "gemini" -> "http://localhost:8092";
            case "http" -> "http://localhost:8093";
            case "trigger" -> "http://localhost:8083";
            default -> throw new IllegalArgumentException("Unknown node type: " + nodeType);
        };
    }
}
```

**WorkflowExecutor.java** - Main orchestration logic

```java
package com.workflow.execution.service;

import com.workflow.execution.model.*;
import com.workflow.execution.repository.ExecutionHistoryRepository;
import com.workflow.execution.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class WorkflowExecutor {
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private ExecutionHistoryRepository executionHistoryRepository;
    
    @Autowired
    private GraphBuilder graphBuilder;
    
    @Autowired
    private TemplateResolver templateResolver;
    
    @Autowired
    private NodeExecutorClient nodeExecutorClient;
    
    /**
     * Main method to execute a workflow
     */
    public void executeWorkflow(WorkflowTriggerEvent event) {
        log.info("Starting workflow execution for workflowId: {}", event.getWorkflowId());
        
        // Create execution context
        ExecutionContext context = new ExecutionContext(
            event.getExecutionId(),
            event.getWorkflowId(),
            event.getUserId()
        );
        
        // Create execution history record
        ExecutionHistory history = new ExecutionHistory();
        history.setExecutionId(event.getExecutionId());
        history.setWorkflowId(event.getWorkflowId());
        history.setUserId(event.getUserId());
        history.setStatus("RUNNING");
        history.setStartTime(LocalDateTime.now());
        history.setTriggeredBy(event.getTriggeredBy());
        executionHistoryRepository.save(history);
        
        try {
            // 1. Fetch workflow definition
            WorkflowDefinition workflow = workflowRepository
                .findByWorkflowIdAndStatus(event.getWorkflowId(), "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + event.getWorkflowId()));
            
            log.info("Workflow loaded: {} with {} nodes", workflow.getName(), workflow.getNodes().size());
            
            // 2. Build execution order
            List<String> executionOrder = graphBuilder.buildExecutionOrder(
                workflow.getNodes(),
                workflow.getEdges()
            );
            
            log.info("Execution order: {}", executionOrder);
            
            // 3. Create node lookup map
            Map<String, Node> nodeMap = new HashMap<>();
            for (Node node : workflow.getNodes()) {
                nodeMap.put(node.getId(), node);
            }
            
            // 4. Execute nodes in order
            for (String nodeId : executionOrder) {
                Node node = nodeMap.get(nodeId);
                
                // Skip trigger node (it's already fired)
                if ("trigger".equals(node.getType())) {
                    log.info("Skipping trigger node: {}", nodeId);
                    continue;
                }
                
                log.info("Executing node: {} (type: {})", nodeId, node.getType());
                
                boolean success = executeNode(node, context);
                
                if (!success) {
                    throw new RuntimeException("Node execution failed: " + nodeId);
                }
            }
            
            // 5. Mark as successful
            history.setStatus("SUCCESS");
            history.setEndTime(LocalDateTime.now());
            history.setNodeResults(context.getNodeOutputs());
            executionHistoryRepository.save(history);
            
            log.info("Workflow execution completed successfully: {}", event.getWorkflowId());
            
        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage(), e);
            
            // Mark as failed
            history.setStatus("FAILED");
            history.setEndTime(LocalDateTime.now());
            history.setErrorMessage(e.getMessage());
            history.setNodeResults(context.getNodeOutputs());
            executionHistoryRepository.save(history);
        }
    }
    
    /**
     * Execute a single node
     */
    private boolean executeNode(Node node, ExecutionContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Resolve template variables in config
            Map<String, Object> resolvedConfig = templateResolver.resolveConfig(
                node.getConfig(),
                context
            );
            
            log.info("Resolved config for node {}: {}", node.getId(), resolvedConfig);
            
            // 2. Create execution request
            NodeExecutionRequest request = new NodeExecutionRequest();
            request.setNodeId(node.getId());
            request.setNodeType(node.getType());
            request.setConfig(resolvedConfig);
            request.setInputs(context.getNodeOutputs());
            request.setUserId(context.getUserId());
            
            // 3. Execute node via REST call
            ExecutionResult result = nodeExecutorClient.executeNode(request);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 4. Store result in context
            if (result.isSuccess()) {
                context.setNodeOutput(node.getId(), result.getOutput());
                
                ExecutionContext.NodeExecutionStatus status = new ExecutionContext.NodeExecutionStatus();
                status.setStatus("SUCCESS");
                status.setDurationMs(duration);
                status.setExecutedAt(LocalDateTime.now());
                context.setNodeStatus(node.getId(), status);
                
                log.info("Node {} executed in {}ms", node.getId(), duration);
                return true;
                
            } else {
                ExecutionContext.NodeExecutionStatus status = new ExecutionContext.NodeExecutionStatus();
                status.setStatus("FAILED");
                status.setError(result.getError());
                status.setDurationMs(duration);
                status.setExecutedAt(LocalDateTime.now());
                context.setNodeStatus(node.getId(), status);
                
                log.error("Node {} failed: {}", node.getId(), result.getError());
                return false;
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            ExecutionContext.NodeExecutionStatus status = new ExecutionContext.NodeExecutionStatus();
            status.setStatus("FAILED");
            status.setError(e.getMessage());
            status.setDurationMs(duration);
            status.setExecutedAt(LocalDateTime.now());
            context.setNodeStatus(node.getId(), status);
            
            log.error("Node {} execution error: {}", node.getId(), e.getMessage());
            return false;
        }
    }
}
```

---

### 6. Kafka Consumer

**WorkflowTriggerListener.java**

```java
package com.workflow.execution.kafka;

import com.workflow.execution.model.WorkflowTriggerEvent;
import com.workflow.execution.service.WorkflowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkflowTriggerListener {
    
    @Autowired
    private WorkflowExecutor workflowExecutor;
    
    /**
     * Listen to workflow-trigger topic
     * Consumes trigger events and starts workflow execution
     */
    @KafkaListener(
        topics = "workflow-trigger",
        groupId = "execution-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWorkflowTrigger(WorkflowTriggerEvent event) {
        log.info("Received workflow trigger event: {}", event);
        
        try {
            // Execute workflow asynchronously (could use @Async for true async)
            workflowExecutor.executeWorkflow(event);
            
        } catch (Exception e) {
            log.error("Error handling workflow trigger: {}", e.getMessage(), e);
            // In production, you might want to:
            // - Publish to dead-letter queue
            // - Send alert notification
            // - Retry with exponential backoff
        }
    }
}
```

---

### 7. Configuration Classes

**KafkaConsumerConfig.java**

```java
package com.workflow.execution.config;

import com.workflow.execution.model.WorkflowTriggerEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ConsumerFactory<String, WorkflowTriggerEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "execution-service-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, WorkflowTriggerEvent.class);
        
        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JsonDeserializer<>(WorkflowTriggerEvent.class)
        );
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkflowTriggerEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkflowTriggerEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

**RestTemplateConfig.java**

```java
package com.workflow.execution.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
}
```

---

This is the complete Execution Service implementation! The key flows are:

1. **Kafka listener** receives trigger event
2. **WorkflowExecutor** orchestrates the entire execution
3. **GraphBuilder** determines node execution order
4. **TemplateResolver** resolves data references between nodes
5. **NodeExecutorClient** calls each integration service via REST

Would you like me to also create:
1. The integration service template (Gmail, Gemini, HTTP)?
2. Docker Compose file for the entire stack?
3. Unit tests for the Execution Service?
