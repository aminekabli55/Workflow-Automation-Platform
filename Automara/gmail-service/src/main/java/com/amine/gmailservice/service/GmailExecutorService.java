package com.amine.gmailservice.service;


import com.amine.gmailservice.model.EmailData;
import com.amine.gmailservice.model.ExecutionResult;
import com.amine.gmailservice.model.NodeExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GmailExecutorService {

    @Autowired
    private GmailApiClient gmailApiClient;

    public ExecutionResult execute(NodeExecutionRequest request) {
        try {
            String action = (String) request.getConfig().getOrDefault("action", "read");

            return switch (action) {
                case "read" -> readEmails(request);
                case "send" -> sendEmail(request);
                case "search" -> searchEmails(request);
                case "markAsRead" -> markAsRead(request);
                case "addLabel" -> addLabel(request);
                default -> ExecutionResult.failure("Unknown action: " + action);
            };

        } catch (Exception e) {
            log.error("Error executing Gmail action: {}", e.getMessage(), e);
            return ExecutionResult.failure(e.getMessage());
        }
    }

    private ExecutionResult readEmails(NodeExecutionRequest request) {
        try {
            Map<String, Object> config = request.getConfig();
            Map<String, Object> filters = (Map<String, Object>) config.get("filters");

            String fromFilter = filters != null ? (String) filters.get("from") : null;
            Boolean isUnread = filters != null ? (Boolean) filters.get("isUnread") : null;
            Integer maxResults = (Integer) config.getOrDefault("maxResults", 10);

            log.info("Reading emails for user: {} with filters - from: {}, unread: {}, max: {}",
                    request.getUserId(), fromFilter, isUnread, maxResults);

            List<EmailData> emails = gmailApiClient.listMessages(
                    request.getUserId(),
                    fromFilter,
                    isUnread,
                    maxResults
            );

            Map<String, Object> output = new HashMap<>();
            output.put("emails", emails);
            output.put("count", emails.size());

            if (!emails.isEmpty()) {
                EmailData first = emails.get(0);
                output.put("subject", first.getSubject());
                output.put("body", first.getBody());
                output.put("from", first.getFrom());
                output.put("to", first.getTo());
                output.put("emailId", first.getId());
            }

            log.info("Successfully read {} emails for user {}", emails.size(), request.getUserId());
            return ExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Error reading emails: {}", e.getMessage(), e);
            return ExecutionResult.failure("Failed to read emails: " + e.getMessage());
        }
    }

    private ExecutionResult sendEmail(NodeExecutionRequest request) {
        return ExecutionResult.failure("Send not implemented yet");
    }

    private ExecutionResult searchEmails(NodeExecutionRequest request) {
        return ExecutionResult.failure("Search not implemented yet");
    }

    private ExecutionResult markAsRead(NodeExecutionRequest request) {
        return ExecutionResult.failure("Mark as read not implemented yet");
    }

    private ExecutionResult addLabel(NodeExecutionRequest request) {
        return ExecutionResult.failure("Add label not implemented yet");
    }

    public Map<String, Object> getNodeSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("nodeType", "gmail");
        schema.put("displayName", "Gmail");
        schema.put("description", "Read and send emails via Gmail");
        schema.put("category", "communication");
        schema.put("icon", "📧");
        return schema;
    }
}