package org.sudhir512kj.alertmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.alertmanager.model.Alert;
import org.sudhir512kj.alertmanager.model.Channel;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

@Service
@Slf4j
public class SMSChannelHandler implements ChannelHandler {
    @Override
    public Channel getChannelType() {
        return Channel.SMS;
    }

    @Override
    public void send(Alert alert, NotificationChannel channel) throws Exception {
        String phoneNumbers = channel.getConfiguration().get("phoneNumbers");
        log.info("Sending SMS to {} for alert {}: {}", phoneNumbers, alert.getId(), alert.getMessage());
        // Integration with SMS service (Twilio, AWS SNS, etc.)
    }
}
