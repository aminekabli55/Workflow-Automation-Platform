package com.amine.httpservice.controller;

import com.amine.httpservice.model.ExecutionResult;
import com.amine.httpservice.model.NodeExecutionRequest;
import com.amine.httpservice.service.HttpExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/http")
@Slf4j
public class HttpExecutorController {

    @Autowired
    private HttpExecutorService httpExecutorService;

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> execute(@RequestBody NodeExecutionRequest request) {
        log.info("Received HTTP execution request for node: {}", request.getNodeId());

        try {
            ExecutionResult result = httpExecutorService.execute(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing HTTP node: {}", e.getMessage(), e);
            return ResponseEntity.ok(ExecutionResult.failure(e.getMessage()));
        }
    }

    @GetMapping("/schema")
    public ResponseEntity<Object> getSchema() {
        return ResponseEntity.ok(httpExecutorService.getNodeSchema());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("HTTP Service is running");
    }
}
