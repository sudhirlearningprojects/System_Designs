package org.sudhir512kj.alertmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.alertmanager.dto.NotificationChannelRequest;
import org.sudhir512kj.alertmanager.model.NotificationChannel;
import org.sudhir512kj.alertmanager.repository.NotificationChannelRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationChannelService {
    private final NotificationChannelRepository channelRepository;

    @Transactional
    public NotificationChannel createChannel(NotificationChannelRequest request) {
        NotificationChannel channel = NotificationChannel.builder()
            .name(request.getName())
            .type(request.getType())
            .configuration(request.getConfiguration())
            .enabled(request.getEnabled() != null ? request.getEnabled() : true)
            .build();
        
        return channelRepository.save(channel);
    }

    @Transactional
    public NotificationChannel updateChannel(String id, NotificationChannelRequest request) {
        NotificationChannel channel = channelRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Channel not found"));
        
        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setConfiguration(request.getConfiguration());
        channel.setEnabled(request.getEnabled());
        
        return channelRepository.save(channel);
    }

    public List<NotificationChannel> getAllChannels() {
        return channelRepository.findAll();
    }

    public NotificationChannel getChannel(String id) {
        return channelRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Channel not found"));
    }

    @Transactional
    public void deleteChannel(String id) {
        channelRepository.deleteById(id);
    }
}
