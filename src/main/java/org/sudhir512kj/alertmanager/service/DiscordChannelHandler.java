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
public class DiscordChannelHandler implements ChannelHandler {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Channel getChannelType() {
        return Channel.DISCORD;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String webhookUrl = channel.getConfiguration().get("webhookUrl");
        
        Map<String, Object> payload = Map.of(
            "content", String.format("**%s**\n%s\nTicket: `%s` | Project: `%s`", 
                alert.getEventType(), alert.getMessage(), alert.getTicketId(), alert.getProjectKey())
        );
        
        restTemplate.postForEntity(webhookUrl, payload, String.class);
        log.info("Sent Discord notification for alert {}", alert.getId());
    }
}
