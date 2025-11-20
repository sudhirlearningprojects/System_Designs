package org.sudhir512kj.whatsapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
    
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    private MessageType type;
    
    // For media messages
    private String mediaUrl;
    private String mediaType;
    private Long mediaSize;
    private String thumbnailUrl;
    
    // For location messages
    private Double latitude;
    private Double longitude;
    
    // For reply messages
    @ManyToOne
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;
    
    // For forwarded messages
    private Boolean isForwarded;
    
    @Enumerated(EnumType.STRING)
    private MessageStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime editedAt;
    
    public enum MessageType {
        TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT, STICKER
    }
    
    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
}