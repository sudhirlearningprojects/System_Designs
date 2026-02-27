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
public class PagerDutyChannelHandler implements ChannelHandler {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Channel getChannelType() {
        return Channel.PAGERDUTY;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String integrationKey = channel.getConfiguration().get("integrationKey");
        
        Map<String, Object> payload = Map.of(
            "routing_key", integrationKey,
            "event_action", "trigger",
            "payload", Map.of(
                "summary", alert.getMessage(),
                "severity", "warning",
                "source", alert.getProjectKey(),
                "custom_details", alert.getMetadata()
            )
        );
        
        restTemplate.postForEntity("https://events.pagerduty.com/v2/enqueue", payload, String.class);
        log.info("Sent PagerDuty notification for alert {}", alert.getId());
    }
}
