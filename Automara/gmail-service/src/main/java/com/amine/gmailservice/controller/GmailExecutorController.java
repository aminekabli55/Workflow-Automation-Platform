package com.amine.gmailservice.controller;



import com.amine.gmailservice.model.ExecutionResult;
import com.amine.gmailservice.model.NodeExecutionRequest;
import com.amine.gmailservice.service.GmailExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gmail")
@Slf4j
public class GmailExecutorController {

    @Autowired
    private GmailExecutorService gmailExecutorService;

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> execute(@RequestBody NodeExecutionRequest request) {
        log.info("Received execution request for node: {} from user: {}",
                request.getNodeId(), request.getUserId());

        try {
            ExecutionResult result = gmailExecutorService.execute(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            return ResponseEntity.ok(ExecutionResult.failure(e.getMessage()));
        }
    }

    @GetMapping("/schema")
    public ResponseEntity<Object> getSchema() {
        return ResponseEntity.ok(gmailExecutorService.getNodeSchema());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gmail Service is running");
    }
}