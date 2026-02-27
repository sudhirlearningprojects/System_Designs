package org.sudhir512kj.alertmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sudhir512kj.alertmanager.model.Alert;
import org.sudhir512kj.alertmanager.model.Channel;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

@Service
@Slf4j
public class WebhookChannelHandler implements ChannelHandler {
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Channel getChannelType() {
        return Channel.WEBHOOK;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String webhookUrl = channel.getConfiguration().get("webhookUrl");
        restTemplate.postForEntity(webhookUrl, alert, String.class);
        log.info("Sent webhook notification for alert {}", alert.getId());
    }
}
