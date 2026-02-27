package org.sudhir512kj.alertmanager.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.alertmanager.dto.TicketEventRequest;
import org.sudhir512kj.alertmanager.model.TicketEventType;
import org.sudhir512kj.alertmanager.service.AlertService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraWebhookService {
    private final AlertService alertService;

    public void handleJiraWebhook(Map<String, Object> payload) {
        String webhookEvent = (String) payload.get("webhookEvent");
        Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
        
        if (issue == null) return;

        String issueKey = (String) issue.get("key");
        Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
        Map<String, Object> project = (Map<String, Object>) fields.get("project");
        
        TicketEventRequest request = TicketEventRequest.builder()
            .ticketId(issueKey)
            .projectKey((String) project.get("key"))
            .eventType(mapJiraEventType(webhookEvent))
            .summary((String) fields.get("summary"))
            .description((String) fields.get("description"))
            .metadata(extractMetadata(fields, payload))
            .build();

        alertService.processTicketEvent(request);
    }

    private TicketEventType mapJiraEventType(String webhookEvent) {
        return switch (webhookEvent) {
            case "jira:issue_created" -> TicketEventType.CREATED;
            case "jira:issue_updated" -> TicketEventType.UPDATED;
            case "comment_created" -> TicketEventType.COMMENTED;
            default -> TicketEventType.UPDATED;
        };
    }

    private Map<String, String> extractMetadata(Map<String, Object> fields, Map<String, Object> payload) {
        Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
        Map<String, Object> status = (Map<String, Object>) fields.get("status");
        Map<String, Object> priority = (Map<String, Object>) fields.get("priority");
        
        return Map.of(
            "assignee", assignee != null ? (String) assignee.get("displayName") : "Unassigned",
            "status", status != null ? (String) status.get("name") : "Unknown",
            "priority", priority != null ? (String) priority.get("name") : "Medium"
        );
    }
}
