package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.whatsapp.dto.ChatDTO;
import org.sudhir512kj.whatsapp.dto.UserDTO;
import org.sudhir512kj.whatsapp.exception.WhatsAppException;
import org.sudhir512kj.whatsapp.model.Chat;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.repository.ChatRepository;
import org.sudhir512kj.whatsapp.repository.MessageRepository;
import org.sudhir512kj.whatsapp.repository.UserRepository;
import org.sudhir512kj.whatsapp.util.ValidationUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ChatCacheService chatCacheService;
    
    public ChatDTO createIndividualChat(String userId1, String userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId1));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId2));
        
        // Check if chat already exists
        return chatRepository.findIndividualChat(user1, user2)
                .map(this::convertToDTO)
                .orElseGet(() -> {
                    Chat chat = Chat.builder()
                            .type(Chat.ChatType.INDIVIDUAL)
                            .participants(Set.of(user1, user2))
                            .createdBy(user1)
                            .build();
                    
                    chat = chatRepository.save(chat);
                    log.info("Individual chat created: {}", chat.getId());
                    return convertToDTO(chat);
                });
    }
    
    public ChatDTO createGroupChat(String creatorId, String name, String description, List<String> participantIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(creatorId));
        
        if (!ValidationUtils.isValidGroupSize(participantIds.size() + 1)) {
            throw new WhatsAppException.InvalidOperationException("Invalid group size");
        }
        
        List<User> participants = userRepository.findAllById(participantIds);
        participants.add(creator);
        
        Chat chat = Chat.builder()
                .type(Chat.ChatType.GROUP)
                .name(name)
                .description(description)
                .createdBy(creator)
                .participants(Set.copyOf(participants))
                .admins(Set.of(creator))
                .build();
        
        chat = chatRepository.save(chat);
        log.info("Group chat created: {} with {} participants", chat.getId(), participants.size());
        return convertToDTO(chat);
    }
    
    public List<ChatDTO> getUserChats(String userId) {
        // Try cache first
        List<Object> cachedChats = chatCacheService.getCachedUserChats(userId);
        if (cachedChats != null && !cachedChats.isEmpty()) {
            return cachedChats.stream()
                    .filter(obj -> obj instanceof ChatDTO)
                    .map(obj -> (ChatDTO) obj)
                    .collect(Collectors.toList());
        }
        
        // Fetch from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId));
        List<ChatDTO> chats = chatRepository.findChatsByUser(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // Cache the result
        chatCacheService.cacheUserChats(userId, chats);
        
        return chats;
    }
    
    public void addParticipant(String chatId, String userId, String adminId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new WhatsAppException.ChatNotFoundException(chatId));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(adminId));
        User newParticipant = userRepository.findById(userId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId));
        
        if (!chat.getAdmins().contains(admin)) {
            throw new WhatsAppException.UnauthorizedException("Only admins can add participants");
        }
        
        chat.getParticipants().add(newParticipant);
        chatRepository.save(chat);
        log.info("User {} added to chat {}", userId, chatId);
    }
    
    public void removeParticipant(String chatId, String userId, String adminId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new WhatsAppException.ChatNotFoundException(chatId));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(adminId));
        User participant = userRepository.findById(userId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId));
        
        if (!chat.getAdmins().contains(admin) && !userId.equals(adminId)) {
            throw new WhatsAppException.UnauthorizedException("Only admins can remove participants");
        }
        
        chat.getParticipants().remove(participant);
        chat.getAdmins().remove(participant);
        chatRepository.save(chat);
        log.info("User {} removed from chat {}", userId, chatId);
    }
    
    private ChatDTO convertToDTO(Chat chat) {
        ChatDTO chatDTO = ChatDTO.builder()
                .id(chat.getId())
                .type(chat.getType())
                .name(chat.getName())
                .description(chat.getDescription())
                .groupIcon(chat.getGroupIcon())
                .createdBy(chat.getCreatedBy().getId())
                .participants(chat.getParticipants().stream()
                        .map(this::convertUserToDTO)
                        .collect(Collectors.toList()))
                .admins(chat.getAdmins().stream()
                        .map(this::convertUserToDTO)
                        .collect(Collectors.toList()))
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
        
        // Cache the chat
        chatCacheService.cacheChat(chatDTO);
        
        return chatDTO;
    }
    
    private UserDTO convertUserToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .profilePicture(user.getProfilePicture())
                .about(user.getAbout())
                .status(user.getStatus())
                .lastSeen(user.getLastSeen())
                .build();
    }
}