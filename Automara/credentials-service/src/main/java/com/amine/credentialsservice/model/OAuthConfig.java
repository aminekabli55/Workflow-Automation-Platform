package com.amine.credentialsservice.model;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "oauth.gmail")
@Data
public class OAuthConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private List<String> scopes;
}
