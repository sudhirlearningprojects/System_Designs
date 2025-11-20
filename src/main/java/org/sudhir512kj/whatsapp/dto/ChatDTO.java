package org.sudhir512kj.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sudhir512kj.whatsapp.model.Chat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {
    private String id;
    private Chat.ChatType type;
    private String name;
    private String description;
    private String groupIcon;
    private String createdBy;
    private List<UserDTO> participants;
    private List<UserDTO> admins;
    private MessageDTO lastMessage;
    private Integer unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}