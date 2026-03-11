package com.amine.credentialsservice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EncryptionService {

    private final AES256TextEncryptor encryptor;
    private final ObjectMapper objectMapper;

    public EncryptionService(@Value("${jasypt.encryptor.password}") String encryptionKey) {
        this.encryptor = new AES256TextEncryptor();
        this.encryptor.setPassword(encryptionKey);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Encrypt credential data
     */
    public String encrypt(Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return encryptor.encrypt(json);
        } catch (JsonProcessingException e) {
            log.error("Error encrypting data: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt credential data
     */
    public Map<String, Object> decrypt(String encryptedData) {
        try {
            String json = encryptor.decrypt(encryptedData);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Error decrypting data: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }
}