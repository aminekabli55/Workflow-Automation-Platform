package com.amine.credentialsservice.service;


import com.amine.credentialsservice.model.Credential;
import com.amine.credentialsservice.repository.CredentialRepository;
import com.amine.credentialsservice.model.CredentialData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CredentialsService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private EncryptionService encryptionService;

    /**
     * Store credential for user
     */
    @Transactional
    public CredentialData storeCredential(String userId, String serviceType,
                                          String credentialName, Map<String, Object> data) {
        try {
            // Generate credential ID
            String credentialId = UUID.randomUUID().toString();

            // Encrypt data
            String encryptedData = encryptionService.encrypt(data);

            // Calculate expiry (if access token has expiry)
            LocalDateTime expiresAt = null;
            if (data.containsKey("expires_in")) {
                long expiresIn = ((Number) data.get("expires_in")).longValue();
                expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
            }

            // Create credential entity
            Credential credential = new Credential();
            credential.setCredentialId(credentialId);
            credential.setUserId(userId);
            credential.setServiceType(serviceType);
            credential.setCredentialName(credentialName);
            credential.setEncryptedData(encryptedData);
            credential.setCreatedAt(LocalDateTime.now());
            credential.setUpdatedAt(LocalDateTime.now());
            credential.setExpiresAt(expiresAt);

            credentialRepository.save(credential);

            log.info("Stored credential for user: {}, service: {}", userId, serviceType);


            return toCredentialData(credential, data);

        } catch (Exception e) {
            log.error("Error storing credential: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store credential", e);
        }
    }

    /**
     * Get credential by user, service, and name
     */
    public CredentialData getCredential(String userId, String serviceType, String credentialName) {
        Credential credential = credentialRepository
                .findByUserIdAndServiceTypeAndCredentialName(userId, serviceType, credentialName)
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        // Decrypt data
        Map<String, Object> decryptedData = encryptionService.decrypt(credential.getEncryptedData());

        return toCredentialData(credential, decryptedData);
    }

    /**
     * Get all credentials for user
     */
    public List<CredentialData> getUserCredentials(String userId) {
        List<Credential> credentials = credentialRepository.findByUserId(userId);

        return credentials.stream()
                .map(cred -> {
                    Map<String, Object> data = encryptionService.decrypt(cred.getEncryptedData());
                    return toCredentialData(cred, data);
                })
                .collect(Collectors.toList());
    }

    /**
     * Update credential
     */
    @Transactional
    public CredentialData updateCredential(String credentialId, Map<String, Object> newData) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        String encryptedData = encryptionService.encrypt(newData);

        credential.setEncryptedData(encryptedData);
        credential.setUpdatedAt(LocalDateTime.now());

        if (newData.containsKey("expires_in")) {
            long expiresIn = ((Number) newData.get("expires_in")).longValue();
            credential.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        }

        credentialRepository.save(credential);

        log.info("Updated credential: {}", credentialId);

        return toCredentialData(credential, newData);
    }

    /**
     * Delete credential
     */
    @Transactional
    public void deleteCredential(String credentialId) {
        credentialRepository.deleteById(credentialId);
        log.info("Deleted credential: {}", credentialId);
    }

    /**
     * Convert entity to DTO
     */
    private CredentialData toCredentialData(Credential credential, Map<String, Object> data) {
        CredentialData dto = new CredentialData();
        dto.setCredentialId(credential.getCredentialId());
        dto.setUserId(credential.getUserId());
        dto.setServiceType(credential.getServiceType());
        dto.setCredentialName(credential.getCredentialName());
        dto.setData(data);
        dto.setCreatedAt(credential.getCreatedAt());
        dto.setExpiresAt(credential.getExpiresAt());
        return dto;
    }
}