package org.sudhir512kj.whatsapp.constants;

public final class WhatsAppConstants {
    
    private WhatsAppConstants() {
        // Utility class
    }
    
    // Cache Keys
    public static final String MESSAGE_CACHE_KEY = "message:";
    public static final String PRESENCE_KEY = "presence:";
    public static final String HEARTBEAT_KEY = "heartbeat:";
    public static final String USER_SERVER_KEY = "user_server:";
    public static final String SERVER_USERS_KEY = "server_users:";
    public static final String OFFLINE_MESSAGES_KEY = "offline_messages:";
    public static final String CHAT_METADATA_KEY = "chat_metadata:";
    public static final String CHAT_PARTICIPANTS_KEY = "chat_participants:";
    public static final String USER_CHATS_KEY = "user_chats:";
    public static final String RECENT_MESSAGES_KEY = "recent_messages:";
    
    // Kafka Topics
    public static final String MESSAGES_TOPIC = "whatsapp.messages";
    public static final String TYPING_TOPIC = "whatsapp.typing";
    
    // WebSocket Destinations
    public static final String CHAT_TOPIC = "/topic/chat/";
    public static final String USER_QUEUE = "/queue/messages";
    public static final String PRESENCE_TOPIC = "/topic/presence/";
    
    // Timeouts and Limits
    public static final int MESSAGE_DELETE_HOURS = 1;
    public static final int MAX_GROUP_PARTICIPANTS = 256;
    public static final int RECENT_MESSAGES_LIMIT = 50;
    public static final int PRESENCE_TTL_MINUTES = 5;
    public static final int OFFLINE_MESSAGES_TTL_DAYS = 7;
    
    // Default Values
    public static final String DEFAULT_ABOUT = "Hey there! I am using WhatsApp.";
}