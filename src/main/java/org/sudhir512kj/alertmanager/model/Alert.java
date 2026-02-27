package org.sudhir512kj.alertmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_ticket_id", columnList = "ticketId"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String projectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketEventType eventType;

    @Column(nullable = false, length = 2000)
    private String message;

    @ElementCollection
    @CollectionTable(name = "alert_metadata", joinColumns = @JoinColumn(name = "alert_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", length = 1000)
    private Map<String, String> metadata; // assignee, priority, status, etc.

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
