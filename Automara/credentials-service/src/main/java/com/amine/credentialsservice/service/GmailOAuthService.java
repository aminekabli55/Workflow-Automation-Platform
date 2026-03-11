package com.amine.credentialsservice.service;

import com.amine.credentialsservice.model.OAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class GmailOAuthService {

    @Autowired
    private OAuthConfig oauthConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String AUTHORIZATION_SERVER_URL =
            "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_SERVER_URL =
            "https://oauth2.googleapis.com/token";

    /**
     * Generate OAuth authorization URL
     */
    public String getAuthorizationUrl(String userId, String state) {
        String authUrl = UriComponentsBuilder
                .fromHttpUrl(AUTHORIZATION_SERVER_URL)
                .queryParam("client_id", oauthConfig.getClientId())
                .queryParam("redirect_uri", oauthConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", oauthConfig.getScopes()))
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();

        log.info("Generated OAuth URL for user: {}", userId);
        return authUrl;
    }

    /**
     * Exchange authorization code for tokens
     */

    public Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
        try {
            log.info("Exchanging authorization code for tokens");

            // Prepare form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("code", authorizationCode);
            formData.add("client_id", oauthConfig.getClientId());
            formData.add("client_secret", oauthConfig.getClientSecret());
            formData.add("redirect_uri", oauthConfig.getRedirectUri());
            formData.add("grant_type", "authorization_code");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Create request
            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(formData, headers);

            // Make POST request
            ResponseEntity<Map> response = restTemplate.exchange(
                    TOKEN_SERVER_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from token endpoint");
            }

            log.info("Successfully exchanged code for tokens");
            return response.getBody();

        } catch (Exception e) {
            log.error("Error exchanging code for tokens: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to exchange code for tokens: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        try {
            log.info("Refreshing access token");

            // Prepare form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("refresh_token", refreshToken);
            formData.add("client_id", oauthConfig.getClientId());
            formData.add("client_secret", oauthConfig.getClientSecret());
            formData.add("grant_type", "refresh_token");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Create request
            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(formData, headers);

            // Make POST request
            ResponseEntity<Map> response = restTemplate.exchange(
                    TOKEN_SERVER_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from token endpoint");
            }

            log.info("Successfully refreshed access token");
            return response.getBody();

        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh access token: " + e.getMessage(), e);
        }
    }
}