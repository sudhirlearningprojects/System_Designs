package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.whatsapp.dto.MessageDTO;
import org.sudhir512kj.whatsapp.dto.SendMessageRequest;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;
import org.sudhir512kj.whatsapp.exception.WhatsAppException;
import org.sudhir512kj.whatsapp.model.*;
import org.sudhir512kj.whatsapp.repository.*;
import org.sudhir512kj.whatsapp.util.ValidationUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageQueueService messageQueueService;
    private final MessageDeliveryService messageDeliveryService;
    private final RedisTemplate<String, Object> redisTemplate;
    

    
    public MessageDTO sendMessage(String senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(senderId));
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new WhatsAppException.ChatNotFoundException(request.getChatId()));
        
        // Verify sender is participant
        if (!chat.getParticipants().contains(sender)) {
            throw new WhatsAppException.UnauthorizedException("User not authorized to send message to this chat");
        }
        
        // Validate message content
        if (!ValidationUtils.isValidMessageContent(request.getContent())) {
            throw new WhatsAppException.InvalidOperationException("Invalid message content");
        }
        
        // Generate message ID for idempotency
        String messageId = UUID.randomUUID().toString();
        
        // Check for duplicate message (idempotency)
        String idempotencyKey = "msg_idempotency:" + senderId + ":" + request.hashCode();
        if (redisTemplate.hasKey(idempotencyKey)) {
            String existingMessageId = (String) redisTemplate.opsForValue().get(idempotencyKey);
            Message existingMessage = messageRepository.findById(existingMessageId).orElse(null);
            if (existingMessage != null) {
                return convertToDTO(existingMessage);
            }
        }
        
        Message replyToMessage = null;
        if (request.getReplyToMessageId() != null) {
            replyToMessage = messageRepository.findById(request.getReplyToMessageId()).orElse(null);
        }
        
        Message message = Message.builder()
                .id(messageId)
                .chat(chat)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .mediaSize(request.getMediaSize())
                .thumbnailUrl(request.getThumbnailUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .replyToMessage(replyToMessage)
                .isForwarded(request.getIsForwarded())
                .status(Message.MessageStatus.SENT)
                .build();
        
        final Message savedMessage = messageRepository.save(message);
        
        // Store idempotency key
        redisTemplate.opsForValue().set(idempotencyKey, messageId, Duration.ofMinutes(5));
        
        // Create delivery records for all participants except sender
        chat.getParticipants().stream()
                .filter(participant -> !participant.equals(sender))
                .forEach(participant -> {
                    MessageDelivery delivery = MessageDelivery.builder()
                            .message(savedMessage)
                            .user(participant)
                            .status(MessageDelivery.DeliveryStatus.SENT)
                            .build();
                    messageDeliveryRepository.save(delivery);
                });
        
        MessageDTO messageDTO = convertToDTO(savedMessage);
        
        // Cache message for quick retrieval
        redisTemplate.opsForValue().set(WhatsAppConstants.MESSAGE_CACHE_KEY + messageId, messageDTO, Duration.ofHours(1));
        
        // Publish to message queue for reliable delivery
        messageQueueService.publishMessage(messageDTO);
        
        // Deliver to online users
        messageDeliveryService.deliverMessage(messageDTO, chat.getParticipants());
        
        log.info("Message sent: {} in chat {}", savedMessage.getId(), chat.getId());
        return messageDTO;
    }
    
    public Page<MessageDTO> getChatMessages(String chatId, int page, int size) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new WhatsAppException.ChatNotFoundException(chatId));
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for recent messages
        if (page == 0) {
            String cacheKey = "recent_messages:" + chatId;
            List<Object> cachedMessages = redisTemplate.opsForList().range(cacheKey, 0, size - 1);
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                log.debug("Retrieved {} messages from cache for chat {}", cachedMessages.size(), chatId);
            }
        }
        
        return messageRepository.findByChatOrderByCreatedAtDesc(chat, pageable)
                .map(this::convertToDTO);
    }
    
    public void markMessagesAsRead(String chatId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId));
        messageDeliveryRepository.markMessagesAsRead(chatId, user, LocalDateTime.now());
        
        // Update read status in cache
        String readStatusKey = "read_status:" + chatId + ":" + userId;
        redisTemplate.opsForValue().set(readStatusKey, LocalDateTime.now().toString(), Duration.ofDays(1));
        
        // Notify sender about read receipts via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/read", userId);
        
        log.info("Messages marked as read for user {} in chat {}", userId, chatId);
    }
    
    public void deleteMessage(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new WhatsAppException.MessageNotFoundException(messageId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WhatsAppException.UserNotFoundException(userId));
        
        if (!message.getSender().equals(user)) {
            throw new WhatsAppException.UnauthorizedException("Only sender can delete message");
        }
        
        // Check if message is older than allowed time
        if (message.getCreatedAt().isBefore(LocalDateTime.now().minusHours(WhatsAppConstants.MESSAGE_DELETE_HOURS))) {
            throw new WhatsAppException.InvalidOperationException("Cannot delete message older than " + WhatsAppConstants.MESSAGE_DELETE_HOURS + " hour(s)");
        }
        
        message.setContent("This message was deleted");
        message.setType(Message.MessageType.TEXT);
        message.setMediaUrl(null);
        messageRepository.save(message);
        
        // Notify via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId() + "/delete", messageId);
        
        log.info("Message deleted: {}", messageId);
    }
    
    public List<MessageDTO> searchMessages(String chatId, String query) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new WhatsAppException.ChatNotFoundException(chatId));
        return messageRepository.searchMessagesInChat(chat, query)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public MessageDTO getMessageFromCache(String messageId) {
        return (MessageDTO) redisTemplate.opsForValue().get(WhatsAppConstants.MESSAGE_CACHE_KEY + messageId);
    }
    
    public void cacheRecentMessages(String chatId, List<MessageDTO> messages) {
        String cacheKey = "recent_messages:" + chatId;
        redisTemplate.delete(cacheKey);
        messages.forEach(msg -> redisTemplate.opsForList().rightPush(cacheKey, msg));
        redisTemplate.expire(cacheKey, Duration.ofMinutes(30));
    }
    
    private MessageDTO convertToDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .type(message.getType())
                .mediaUrl(message.getMediaUrl())
                .mediaType(message.getMediaType())
                .mediaSize(message.getMediaSize())
                .thumbnailUrl(message.getThumbnailUrl())
                .latitude(message.getLatitude())
                .longitude(message.getLongitude())
                .replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
                .isForwarded(message.getIsForwarded())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .build();
    }
}