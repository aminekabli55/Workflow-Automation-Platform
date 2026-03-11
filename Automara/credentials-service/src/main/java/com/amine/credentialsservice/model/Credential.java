package com.amine.credentialsservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "credentials",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "service_type", "credential_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

    @Id
    @Column(name = "credential_id")
    private String credentialId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "credential_name", nullable = false)
    private String credentialName;

    @Column(name = "encrypted_data", columnDefinition = "TEXT", nullable = false)
    private String encryptedData;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}