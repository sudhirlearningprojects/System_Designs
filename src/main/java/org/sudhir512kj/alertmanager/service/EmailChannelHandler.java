package org.sudhir512kj.alertmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.alertmanager.model.Alert;
import org.sudhir512kj.alertmanager.model.Channel;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

@Service
@Slf4j
public class EmailChannelHandler implements ChannelHandler {
    @Override
    public Channel getChannelType() {
        return Channel.EMAIL;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String recipients = channel.getConfiguration().get("recipients");
        log.info("Sending email to {} for alert {}: {}", recipients, alert.getId(), alert.getMessage());
        // Integration with email service (SendGrid, AWS SES, etc.)
    }
}
