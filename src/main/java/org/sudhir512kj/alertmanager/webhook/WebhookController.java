package org.sudhir512kj.alertmanager.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.alertmanager.integration.JiraWebhookService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {
    private final JiraWebhookService jiraWebhookService;

    @PostMapping("/jira")
    public ResponseEntity<String> handleJiraWebhook(@RequestBody Map<String, Object> payload) {
        jiraWebhookService.handleJiraWebhook(payload);
        return ResponseEntity.ok("Webhook received");
    }
}
