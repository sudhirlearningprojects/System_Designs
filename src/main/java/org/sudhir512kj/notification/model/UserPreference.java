package org.sudhir512kj.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "user_preferences")
@Data
public class UserPreference {
    @Id
    private String userId;
    
    @ElementCollection
    @CollectionTable(name = "user_channel_preferences")
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "notification_type")
    @Column(name = "enabled_channels")
    private Map<NotificationType, Set<Channel>> enabledChannels = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "user_global_channel_settings")
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "channel")
    @Column(name = "enabled")
    private Map<Channel, Boolean> globalChannelSettings = new HashMap<>();
    
    @Embedded
    private QuietHours quietHours;
    
    private String timezone = "UTC";
    private Instant updatedAt = Instant.now();
}
