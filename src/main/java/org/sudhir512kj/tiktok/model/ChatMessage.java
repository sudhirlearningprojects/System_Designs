package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_stream_timestamp", columnList = "streamId,timestamp")
})
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;
    
    @Column(nullable = false)
    private Long streamId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 500)
    private String content;
    
    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.TEXT;
    
    @CreationTimestamp
    private LocalDateTime timestamp;
    
    public enum MessageType {
        TEXT, LIKE, GIFT, JOIN, LEAVE
    }
}
