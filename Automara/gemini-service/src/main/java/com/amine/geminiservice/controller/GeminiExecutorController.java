package com.amine.geminiservice.controller;



import com.amine.geminiservice.model.ExecutionResult;
import com.amine.geminiservice.model.NodeExecutionRequest;
import com.amine.geminiservice.service.GeminiExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/gemini")
@Slf4j
public class GeminiExecutorController {

    @Autowired
    private GeminiExecutorService geminiExecutorService;

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> execute(@RequestBody NodeExecutionRequest request) {
        log.info("Received Gemini execution request for node: {}", request.getNodeId());

        try {
            ExecutionResult result = geminiExecutorService.execute(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.ok(ExecutionResult.failure(e.getMessage()));
        }
    }

    @GetMapping("/schema")
    public ResponseEntity<Object> getSchema() {
        return ResponseEntity.ok(geminiExecutorService.getNodeSchema());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gemini Service is running");
    }
}