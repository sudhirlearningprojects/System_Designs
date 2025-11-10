package org.sudhir512kj.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sudhir512kj.notification.model.Channel;
import org.sudhir512kj.notification.model.NotificationPriority;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final MeterRegistry registry;
    
    public void recordNotificationSent(Channel channel, NotificationPriority priority) {
        registry.counter("notifications.sent",
            "channel", channel.name(),
            "priority", priority.name()
        ).increment();
    }
    
    public void recordDeliveryLatency(Channel channel, long latencyMs) {
        registry.timer("notifications.delivery.latency",
            "channel", channel.name()
        ).record(latencyMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordFailure(Channel channel, String reason) {
        registry.counter("notifications.failed",
            "channel", channel.name(),
            "reason", reason
        ).increment();
    }
    
    public void recordDLQEntry(String reason) {
        registry.counter("notifications.dlq",
            "reason", reason
        ).increment();
    }
}
