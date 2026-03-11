package com.amine.gmailservice.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.amine.gmailservice.model.CredentialData;
import com.amine.gmailservice.model.EmailData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GmailApiClient {

    @Value("${credentials.service.url}")
    private String credentialsServiceUrl;

    @Value("${gmail.application-name}")
    private String applicationName;

    @Value("${gmail.credentials-name:My Gmail}")
    private String credentialsName;

    private final RestTemplate restTemplate = new RestTemplate();

    private Gmail getGmailService(String userId) throws Exception {
        try {
            String url = String.format("%s/api/credentials/%s/gmail/%s",
                    credentialsServiceUrl, userId, credentialsName);

            log.info("Fetching credentials from: {}", url);

            ResponseEntity<CredentialData> response = restTemplate.getForEntity(url, CredentialData.class);

            if (response.getBody() == null || response.getBody().getData() == null) {
                throw new RuntimeException("No credentials found for user: " + userId);
            }

            String accessToken = (String) response.getBody().getData().get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("No access token in credentials");
            }

            log.info("Successfully fetched credentials for user: {}", userId);

            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName(applicationName).build();

        } catch (Exception e) {
            log.error("Error getting Gmail service for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get Gmail service: " + e.getMessage(), e);
        }
    }

    public List<EmailData> listMessages(String userId, String fromFilter, Boolean isUnread, Integer maxResults) {
        try {
            Gmail service = getGmailService(userId);

            StringBuilder query = new StringBuilder();
            if (fromFilter != null && !fromFilter.isEmpty()) {
                query.append("from:").append(fromFilter);
            }
            if (isUnread != null && isUnread) {
                if (query.length() > 0) query.append(" ");
                query.append("is:unread");
            }

            log.info("Listing messages for user {} with query: '{}'", userId, query);

            ListMessagesResponse response = service.users().messages()
                    .list("me")
                    .setQ(query.toString())
                    .setMaxResults(maxResults.longValue())
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No messages found for user: {}", userId);
                return Collections.emptyList();
            }

            return messages.stream()
                    .map(msg -> {
                        try {
                            Message fullMessage = service.users().messages()
                                    .get("me", msg.getId())
                                    .setFormat("full")
                                    .execute();
                            return parseMessage(fullMessage);
                        } catch (Exception e) {
                            log.error("Error fetching message {}: {}", msg.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error listing messages for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to list messages: " + e.getMessage(), e);
        }
    }

    private EmailData parseMessage(Message message) {
        EmailData email = new EmailData();
        email.setId(message.getId());
        email.setThreadId(message.getThreadId());

        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                String name = header.getName().toLowerCase();
                String value = header.getValue();

                switch (name) {
                    case "subject" -> email.setSubject(value);
                    case "from" -> email.setFrom(value);
                    case "to" -> email.setTo(value);
                }
            }
        }

        email.setBody(getBody(message.getPayload()));
        email.setLabels(message.getLabelIds());
        email.setUnread(message.getLabelIds() != null && message.getLabelIds().contains("UNREAD"));
        email.setTimestamp(message.getInternalDate());

        return email;
    }

    private String getBody(MessagePart payload) {
        if (payload == null) return "";

        if (payload.getBody() != null && payload.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(payload.getBody().getData()));
        }

        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                if ("text/plain".equals(part.getMimeType())) {
                    if (part.getBody() != null && part.getBody().getData() != null) {
                        return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
                    }
                }
            }
        }

        return "";
    }

    public String sendMessage(String userId, String to, String subject, String body) {
        throw new UnsupportedOperationException("Send not implemented yet");
    }

    public List<EmailData> searchMessages(String userId, String query, Integer maxResults) {
        return listMessages(userId, null, null, maxResults);
    }

    public void markAsRead(String userId, String messageId) throws Exception {
        Gmail service = getGmailService(userId);
        ModifyMessageRequest request = new ModifyMessageRequest()
                .setRemoveLabelIds(Collections.singletonList("UNREAD"));
        service.users().messages().modify("me", messageId, request).execute();
        log.info("Marked message {} as read for user {}", messageId, userId);
    }

    public void addLabel(String userId, String messageId, String labelName) throws Exception {
        Gmail service = getGmailService(userId);
        String labelId = findOrCreateLabel(service, labelName);
        ModifyMessageRequest request = new ModifyMessageRequest()
                .setAddLabelIds(Collections.singletonList(labelId));
        service.users().messages().modify("me", messageId, request).execute();
        log.info("Added label {} to message {} for user {}", labelName, messageId, userId);
    }

    private String findOrCreateLabel(Gmail service, String labelName) throws Exception {
        ListLabelsResponse response = service.users().labels().list("me").execute();
        for (Label label : response.getLabels()) {
            if (label.getName().equalsIgnoreCase(labelName)) {
                return label.getId();
            }
        }

        Label newLabel = new Label()
                .setName(labelName)
                .setLabelListVisibility("labelShow")
                .setMessageListVisibility("show");
        return service.users().labels().create("me", newLabel).execute().getId();
    }
}
