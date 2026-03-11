package com.amine.credentialsservice.controller;


import com.amine.credentialsservice.model.CredentialData;
import com.amine.credentialsservice.service.CredentialsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credentials")
@Slf4j
public class CredentialsController {

    @Autowired
    private CredentialsService credentialsService;

    /**
     * Store credential
     */
    @PostMapping
    public ResponseEntity<CredentialData> storeCredential(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String serviceType = (String) request.get("serviceType");
        String credentialName = (String) request.get("credentialName");
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        CredentialData credential = credentialsService.storeCredential(
                userId, serviceType, credentialName, data
        );

        return ResponseEntity.ok(credential);
    }

    /**
     * Get credential
     */
    @GetMapping("/{userId}/{serviceType}/{credentialName}")
    public ResponseEntity<CredentialData> getCredential(
            @PathVariable String userId,
            @PathVariable String serviceType,
            @PathVariable String credentialName
    ) {
        CredentialData credential = credentialsService.getCredential(
                userId, serviceType, credentialName
        );
        return ResponseEntity.ok(credential);
    }

    /**
     * Get all credentials for user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<CredentialData>> getUserCredentials(@PathVariable String userId) {
        List<CredentialData> credentials = credentialsService.getUserCredentials(userId);
        return ResponseEntity.ok(credentials);
    }

    /**
     * Update credential
     */
    @PutMapping("/{credentialId}")
    public ResponseEntity<CredentialData> updateCredential(
            @PathVariable String credentialId,
            @RequestBody Map<String, Object> data
    ) {
        CredentialData credential = credentialsService.updateCredential(credentialId, data);
        return ResponseEntity.ok(credential);
    }

    /**
     * Delete credential
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<Void> deleteCredential(@PathVariable String credentialId) {
        credentialsService.deleteCredential(credentialId);
        return ResponseEntity.ok().build();
    }
}
