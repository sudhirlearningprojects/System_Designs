package org.sudhir512kj.whatsapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    @CreationTimestamp
    private LocalDateTime deliveredAt;
    
    private LocalDateTime readAt;
    
    public enum DeliveryStatus {
        SENT, DELIVERED, READ
    }
}