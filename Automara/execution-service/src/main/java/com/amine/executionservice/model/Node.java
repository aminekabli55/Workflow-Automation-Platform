package com.amine.executionservice.model;

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