package org.sudhir512kj.alertmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.alertmanager.dto.TicketEventRequest;
import org.sudhir512kj.alertmanager.model.*;
import org.sudhir512kj.alertmanager.repository.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
    private final AlertRuleRepository alertRuleRepository;
    private final NotificationChannelRepository channelRepository;
    private final AlertRepository alertRepository;
    private final AlertDeliveryRepository deliveryRepository;
    private final List<ChannelHandler> channelHandlers;

    @Transactional
    public void processTicketEvent(TicketEventRequest request) {
        log.info("Processing ticket event: {} for ticket {}", request.getEventType(), request.getTicketId());

        List<AlertRule> rules = alertRuleRepository.findByProjectKeyAndEnabledTrue(request.getProjectKey());
        
        for (AlertRule rule : rules) {
            if (rule.getTriggerEvents().contains(request.getEventType())) {
                Alert alert = createAlert(request);
                alertRepository.save(alert);
                
                sendNotifications(alert, rule.getChannelIds());
            }
        }
    }

    private Alert createAlert(TicketEventRequest request) {
        return Alert.builder()
            .ticketId(request.getTicketId())
            .projectKey(request.getProjectKey())
            .eventType(request.getEventType())
            .message(buildMessage(request))
            .metadata(request.getMetadata())
            .build();
    }

    private String buildMessage(TicketEventRequest request) {
        return String.format("[%s] %s - %s", 
            request.getProjectKey(), 
            request.getSummary(), 
            request.getDescription());
    }

    @Async
    public void sendNotifications(Alert alert, List<String> channelIds) {
        List<NotificationChannel> channels = channelRepository.findByIdInAndEnabledTrue(channelIds);
        Map<Channel, ChannelHandler> handlerMap = channelHandlers.stream()
            .collect(Collectors.toMap(ChannelHandler::getChannelType, Function.identity()));

        for (NotificationChannel channel : channels) {
            AlertDelivery delivery = AlertDelivery.builder()
                .alertId(alert.getId())
                .channelId(channel.getId())
                .channelType(channel.getType())
                .status(DeliveryStatus.PENDING)
                .build();

            try {
                ChannelHandler handler = handlerMap.get(channel.getType());
                if (handler != null) {
                    handler.send(alert, channel);
                    delivery.setStatus(DeliveryStatus.DELIVERED);
                    delivery.setDeliveredAt(java.time.LocalDateTime.now());
                }
            } catch (Exception e) {
                log.error("Failed to send notification via {}: {}", channel.getType(), e.getMessage());
                delivery.setStatus(DeliveryStatus.FAILED);
                delivery.setErrorMessage(e.getMessage());
            }

            deliveryRepository.save(delivery);
        }
    }
}
