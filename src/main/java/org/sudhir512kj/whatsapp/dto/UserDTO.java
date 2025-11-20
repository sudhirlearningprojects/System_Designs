package org.sudhir512kj.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sudhir512kj.whatsapp.model.User;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String phoneNumber;
    private String name;
    private String profilePicture;
    private String about;
    private User.UserStatus status;
    private LocalDateTime lastSeen;
}