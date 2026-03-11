package com.amine.executionservice.service;


import com.amine.executionservice.model.ExecutionContext;
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