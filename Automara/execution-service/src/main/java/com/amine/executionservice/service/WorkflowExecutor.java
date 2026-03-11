package com.amine.executionservice.service;


import com.amine.executionservice.model.WorkflowTriggerEvent;
import com.amine.executionservice.model.*;
import com.amine.executionservice.repository.ExecutionHistoryRepository;
import com.amine.executionservice.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            // 4. Execute nodes                               in order
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
