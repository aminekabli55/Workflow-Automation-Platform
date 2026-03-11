package com.amine.credentialsservice.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialData {
    private String credentialId;
    private String userId;
    private String serviceType;
    private String credentialName;
    private Map<String, Object> data;  // Decrypted data
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}