package org.sudhir512kj.alertmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String projectKey; // Jira project key or ticket system identifier

    @ElementCollection
    @CollectionTable(name = "alert_rule_events", joinColumns = @JoinColumn(name = "rule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private List<TicketEventType> triggerEvents;

    @ElementCollection
    @CollectionTable(name = "alert_rule_channels", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "channel_id")
    private List<String> channelIds;

    @Column(nullable = false)
    private Boolean enabled = true;

    private String filterCondition; // JSON or SpEL expression for advanced filtering

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
