package org.sudhir512kj.alertmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sudhir512kj.alertmanager.model.Alert;
import org.sudhir512kj.alertmanager.model.Channel;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

import java.util.Map;

@Service
@Slf4j
public class MSTeamsChannelHandler implements ChannelHandler {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Channel getChannelType() {
        return Channel.MS_TEAMS;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String webhookUrl = channel.getConfiguration().get("webhookUrl");
        
        Map<String, Object> payload = Map.of(
            "@type", "MessageCard",
            "title", alert.getEventType().toString(),
            "text", alert.getMessage(),
            "sections", new Object[]{
                Map.of("facts", new Object[]{
                    Map.of("name", "Ticket ID", "value", alert.getTicketId()),
                    Map.of("name", "Project", "value", alert.getProjectKey())
                })
            }
        );
        
        restTemplate.postForEntity(webhookUrl, payload, String.class);
        log.info("Sent MS Teams notification for alert {}", alert.getId());
    }
}
