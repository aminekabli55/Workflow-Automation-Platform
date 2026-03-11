package com.amine.credentialsservice.service;

import com.amine.credentialsservice.model.Credential;
import com.amine.credentialsservice.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TokenRefreshService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private GmailOAuthService gmailOAuthService;

    @Autowired
    private CredentialsService credentialsService;

    /**
     * Check and refresh expired tokens every hour
     */
    @Scheduled(fixedRate = 3600000)  // Every hour
    public void refreshExpiredTokens() {
        log.info("Checking for expired tokens...");

        List<Credential> allCredentials = credentialRepository.findAll();

        for (Credential credential : allCredentials) {
            // Skip if not expired or no expiry set
            if (credential.getExpiresAt() == null ||
                    credential.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
                continue;
            }

            try {
                // Decrypt to get refresh token
                Map<String, Object> data = encryptionService.decrypt(credential.getEncryptedData());

                if (!data.containsKey("refresh_token")) {
                    log.warn("No refresh token for credential: {}", credential.getCredentialId());
                    continue;
                }

                String refreshToken = (String) data.get("refresh_token");

                // Refresh based on service type
                if ("gmail".equals(credential.getServiceType())) {
                    Map<String, Object> newTokens = gmailOAuthService.refreshAccessToken(refreshToken);

                    // Update with new tokens (keep refresh token)
                    data.put("access_token", newTokens.get("access_token"));
                    data.put("expires_in", newTokens.get("expires_in"));

                    credentialsService.updateCredential(credential.getCredentialId(), data);

                    log.info("Refreshed token for credential: {}", credential.getCredentialId());
                }

            } catch (Exception e) {
                log.error("Error refreshing token for credential {}: {}",
                        credential.getCredentialId(), e.getMessage());
            }
        }
    }
}