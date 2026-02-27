package org.sudhir512kj.alertmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sudhir512kj.alertmanager.model.Alert;
import org.sudhir512kj.alertmanager.model.Channel;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

import java.util.Map;

@Service
@Slf4j
public class OpsGenieChannelHandler implements ChannelHandler {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Channel getChannelType() {
        return Channel.OPSGENIE;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String apiKey = channel.getConfiguration().get("apiKey");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "GenieKey " + apiKey);
        
        Map<String, Object> payload = Map.of(
            "message", alert.getMessage(),
            "description", String.format("Ticket: %s | Event: %s", alert.getTicketId(), alert.getEventType()),
            "priority", "P3",
            "details", alert.getMetadata()
        );
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForEntity("https://api.opsgenie.com/v2/alerts", request, String.class);
        log.info("Sent OpsGenie notification for alert {}", alert.getId());
    }
}
