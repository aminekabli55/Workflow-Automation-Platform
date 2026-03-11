package com.amine.geminiservice.service;

import com.amine.geminiservice.model.ExecutionResult;
import com.amine.geminiservice.model.NodeExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GeminiExecutorService {

    @Autowired
    private GeminiApiClient geminiApiClient;

    public ExecutionResult execute(NodeExecutionRequest request) {
        try {
            String action = (String) request.getConfig().getOrDefault("action", "generate");

            return switch (action) {
                case "generate" -> generateText(request);
                case "summarize" -> summarize(request);
                default -> ExecutionResult.failure("Unknown action: " + action);
            };

        } catch (Exception e) {
            log.error("Error executing Gemini action: {}", e.getMessage(), e);
            return ExecutionResult.failure(e.getMessage());
        }
    }

    private ExecutionResult generateText(NodeExecutionRequest request) {
        try {
            String prompt = (String) request.getConfig().get("prompt");

            if (prompt == null || prompt.isEmpty()) {
                return ExecutionResult.failure("Prompt is required");
            }

            log.info("Generating text for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));

            String result = geminiApiClient.generateText(prompt);

            Map<String, Object> output = new HashMap<>();
            output.put("text", result);
            output.put("prompt", prompt);
            output.put("model", "gemini-1.5-flash");

            return ExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Error generating text: {}", e.getMessage(), e);
            return ExecutionResult.failure("Failed to generate text: " + e.getMessage());
        }
    }

    private ExecutionResult summarize(NodeExecutionRequest request) {
        try {
            String text = (String) request.getConfig().get("text");
            String style = (String) request.getConfig().getOrDefault("style", "brief");

            if (text == null || text.isEmpty()) {
                return ExecutionResult.failure("Text is required");
            }

            log.info("Summarizing text with style: {}", style);

            String summary = geminiApiClient.summarize(text, style);

            Map<String, Object> output = new HashMap<>();
            output.put("summary", summary);
            output.put("originalLength", text.length());
            output.put("style", style);

            return ExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Error summarizing: {}", e.getMessage(), e);
            return ExecutionResult.failure("Failed to summarize: " + e.getMessage());
        }
    }

    public Map<String, Object> getNodeSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("nodeType", "gemini");
        schema.put("displayName", "Gemini AI");
        schema.put("description", "Generate text and summaries using Google Gemini AI");
        schema.put("category", "ai");
        schema.put("icon", "🤖");
        return schema;
    }
}