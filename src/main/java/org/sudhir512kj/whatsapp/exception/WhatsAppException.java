package org.sudhir512kj.whatsapp.exception;

public class WhatsAppException extends RuntimeException {
    
    public WhatsAppException(String message) {
        super(message);
    }
    
    public WhatsAppException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static class UserNotFoundException extends WhatsAppException {
        public UserNotFoundException(String userId) {
            super("User not found: " + userId);
        }
    }
    
    public static class ChatNotFoundException extends WhatsAppException {
        public ChatNotFoundException(String chatId) {
            super("Chat not found: " + chatId);
        }
    }
    
    public static class MessageNotFoundException extends WhatsAppException {
        public MessageNotFoundException(String messageId) {
            super("Message not found: " + messageId);
        }
    }
    
    public static class UnauthorizedException extends WhatsAppException {
        public UnauthorizedException(String message) {
            super("Unauthorized: " + message);
        }
    }
    
    public static class InvalidOperationException extends WhatsAppException {
        public InvalidOperationException(String message) {
            super("Invalid operation: " + message);
        }
    }
}