package org.sudhir512kj.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sudhir512kj.whatsapp.model.Message;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String chatId;
    private String content;
    private Message.MessageType type;
    private String mediaUrl;
    private String mediaType;
    private Long mediaSize;
    private String thumbnailUrl;
    private Double latitude;
    private Double longitude;
    private String replyToMessageId;
    private Boolean isForwarded;
}