package org.sudhir512kj.alertmanager.service;

import org.sudhir512kj.alertmanager.model.Alert;
import org.sudhir512kj.alertmanager.model.Channel;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

public interface ChannelHandler {
    Channel getChannelType();
    void send(Alert alert, NotificationChannel channel) throws Exception;
}
