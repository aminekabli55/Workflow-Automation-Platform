package com.amine.gmailservice.model;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailData {
    private String id;
    private String threadId;
    private String subject;
    private String from;
    private String to;
    private String body;
    private String bodyHtml;
    private Long timestamp;
    private List<String> labels;
    private boolean unread;
}