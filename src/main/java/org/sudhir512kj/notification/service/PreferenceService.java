package org.sudhir512kj.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.sudhir512kj.notification.model.*;
import org.sudhir512kj.notification.repository.UserPreferenceRepository;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PreferenceService {
    private final UserPreferenceRepository preferenceRepository;
    
    @Cacheable(value = "user-preferences", key = "#userId")
    public UserPreference getUserPreference(String userId) {
        return preferenceRepository.findById(userId)
            .orElseGet(() -> createDefaultPreference(userId));
    }
    
    public boolean shouldSendNotification(String userId, NotificationType type, Channel channel) {
        UserPreference pref = getUserPreference(userId);
        
        if (!pref.getGlobalChannelSettings().getOrDefault(channel, true)) {
            return false;
        }
        
        Set<Channel> enabledChannels = pref.getEnabledChannels().get(type);
        if (enabledChannels == null || !enabledChannels.contains(channel)) {
            return false;
        }
        
        if (isInQuietHours(pref) && type != NotificationType.ALERT) {
            return false;
        }
        
        return true;
    }
    
    private boolean isInQuietHours(UserPreference pref) {
        if (pref.getQuietHours() == null) return false;
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(pref.getTimezone()));
        LocalTime currentTime = now.toLocalTime();
        
        return currentTime.isAfter(pref.getQuietHours().getStart()) &&
               currentTime.isBefore(pref.getQuietHours().getEnd());
    }
    
    private UserPreference createDefaultPreference(String userId) {
        UserPreference pref = new UserPreference();
        pref.setUserId(userId);
        
        Map<NotificationType, Set<Channel>> defaults = new HashMap<>();
        defaults.put(NotificationType.TRANSACTIONAL, Set.of(Channel.EMAIL, Channel.SMS, Channel.PUSH));
        defaults.put(NotificationType.ALERT, Set.of(Channel.EMAIL, Channel.SMS, Channel.PUSH));
        defaults.put(NotificationType.PROMOTIONAL, Set.of(Channel.EMAIL));
        defaults.put(NotificationType.SYSTEM, Set.of(Channel.EMAIL));
        pref.setEnabledChannels(defaults);
        
        Map<Channel, Boolean> globalSettings = new HashMap<>();
        for (Channel channel : Channel.values()) {
            globalSettings.put(channel, true);
        }
        pref.setGlobalChannelSettings(globalSettings);
        
        return preferenceRepository.save(pref);
    }
    
    public UserPreference updatePreference(String userId, UserPreference preference) {
        preference.setUserId(userId);
        preference.setUpdatedAt(Instant.now());
        return preferenceRepository.save(preference);
    }
}
