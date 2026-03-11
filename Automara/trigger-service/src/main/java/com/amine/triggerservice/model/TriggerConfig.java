package com.amine.triggerservice.model;


import lombok.Data;

@Data
public class TriggerConfig {

    // For CRON triggers
    private String cronExpression;
    private String timezone;

    // For WEBHOOK triggers
    private String webhookUrl;
    private String webhookSecret;

    // Common
    private Boolean enabled = true;
    private Integer maxExecutions;
}