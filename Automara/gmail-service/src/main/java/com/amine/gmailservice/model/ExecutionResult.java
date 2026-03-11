package com.amine.gmailservice.model;

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