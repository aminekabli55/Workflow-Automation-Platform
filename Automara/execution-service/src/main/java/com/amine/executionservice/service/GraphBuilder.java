package com.amine.executionservice.service;


import com.amine.executionservice.model.Edge;
import com.amine.executionservice.model.Node;
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