package com.amine.credentialsservice.controller;


import com.amine.credentialsservice.service.CredentialsService;
import com.amine.credentialsservice.service.GmailOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/api/oauth")
@Slf4j
public class OAuthCallbackController {

    @Autowired
    private GmailOAuthService gmailOAuthService;

    @Autowired
    private CredentialsService credentialsService;

    /**
     * Initiate OAuth flow for Gmail
     */
    @GetMapping("/connect/gmail")
    public String connectGmail(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "My Gmail") String credentialName
    ) {
        String state = userId + ":" + credentialName;
        String authUrl = gmailOAuthService.getAuthorizationUrl(userId, state);
        return "redirect:" + authUrl;
    }

    /**
     * OAuth callback - receives authorization code
     */
    @GetMapping("/callback/gmail")
    public String oauthCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            // Parse state (userId:credentialName)
            String[] parts = state.split(":", 2);
            String userId = parts[0];
            String credentialName = parts.length > 1 ? parts[1] : "My Gmail";

            // Exchange code for tokens
            Map<String, Object> tokens = gmailOAuthService.exchangeCodeForTokens(code);

            // Store credentials
            credentialsService.storeCredential(userId, "gmail", credentialName, tokens);

            log.info("OAuth callback successful for user: {}", userId);

            // Redirect to success page (you'll need to create this in frontend)
            return "redirect:http://localhost:4200/credentials/success";

        } catch (Exception e) {
            log.error("OAuth callback error: {}", e.getMessage(), e);
            return "redirect:http://localhost:4200/credentials/error";
        }
    }
}