package com.amine.httpservice.service;


import com.amine.httpservice.model.ExecutionResult;
import com.amine.httpservice.model.NodeExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class HttpExecutorService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    public ExecutionResult execute(NodeExecutionRequest request) {
        try {
            Map<String, Object> config = request.getConfig();

            // Extract configuration
            String method = ((String) config.getOrDefault("method", "GET")).toUpperCase();
            String url = (String) config.get("url");
            Map<String, String> headers = (Map<String, String>) config.get("headers");
            Object body = config.get("body");
            Map<String, String> queryParams = (Map<String, String>) config.get("queryParams");
            String authType = (String) config.get("authType");
            Map<String, String> auth = (Map<String, String>) config.get("auth");

            if (url == null || url.isEmpty()) {
                return ExecutionResult.failure("URL is required");
            }

            log.info("Making {} request to: {}", method, url);

            // Build and execute request
            String response = makeHttpRequest(method, url, headers, body, queryParams, authType, auth);

            // Build output
            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", 200);
            output.put("responseBody", response);
            output.put("url", url);
            output.put("method", method);

            log.info("HTTP request completed successfully");
            return ExecutionResult.success(output);

        } catch (Exception e) {
            log.error("HTTP request failed: {}", e.getMessage(), e);
            return ExecutionResult.failure("HTTP request failed: " + e.getMessage());
        }
    }

    private String makeHttpRequest(String method, String url, Map<String, String> headers,
                                   Object body, Map<String, String> queryParams,
                                   String authType, Map<String, String> auth) {

        // Build WebClient
        WebClient.Builder builder = webClientBuilder.baseUrl(url);

        // Add headers
        if (headers != null && !headers.isEmpty()) {
            builder.defaultHeaders(httpHeaders -> {
                headers.forEach(httpHeaders::add);
            });
        }

        // Add authentication
        if (authType != null && auth != null) {
            builder = addAuthentication(builder, authType, auth);
        }

        WebClient client = builder.build();

        // Build request
        WebClient.RequestHeadersUriSpec<?> requestSpec = switch (method) {
            case "GET" -> client.get();
            case "POST" -> client.post();
            case "PUT" -> client.put();
            case "DELETE" -> client.delete();
            case "PATCH" -> client.patch();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };

        // Add query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            WebClient.RequestHeadersUriSpec<?> uriSpec = (WebClient.RequestHeadersUriSpec<?>) requestSpec;
            queryParams.forEach((key, value) -> {
                uriSpec.uri(uriBuilder -> uriBuilder.queryParam(key, value).build());
            });
        }

        // Add body for POST/PUT/PATCH
        WebClient.RequestHeadersSpec<?> headersSpec;
        if ((method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) && body != null) {
            headersSpec = ((WebClient.RequestBodyUriSpec) requestSpec)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        } else {
            headersSpec = (WebClient.RequestHeadersSpec<?>) requestSpec;
        }

        // Execute request
        return headersSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    private WebClient.Builder addAuthentication(WebClient.Builder builder, String authType, Map<String, String> auth) {
        return switch (authType.toLowerCase()) {
            case "bearer" -> {
                String token = auth.get("token");
                yield builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            case "basic" -> {
                String username = auth.get("username");
                String password = auth.get("password");
                String credentials = username + ":" + password;
                String encoded = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
                yield builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            }
            case "apikey" -> {
                String apiKey = auth.get("apiKey");
                String headerName = auth.getOrDefault("headerName", "X-API-Key");
                yield builder.defaultHeader(headerName, apiKey);
            }
            default -> builder;
        };
    }

    public Map<String, Object> getNodeSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("nodeType", "http");
        schema.put("displayName", "HTTP Request");
        schema.put("description", "Make HTTP requests to external APIs and webhooks");
        schema.put("category", "integration");
        schema.put("icon", "🌐");
        return schema;
    }
}