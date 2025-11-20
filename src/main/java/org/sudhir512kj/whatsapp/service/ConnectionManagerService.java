package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionManagerService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, String> userConnections = new ConcurrentHashMap<>();
    
    public void registerConnection(String userId, String serverId) {
        // Store user -> server mapping in Redis
        redisTemplate.opsForValue().set(WhatsAppConstants.USER_SERVER_KEY + userId, serverId, Duration.ofMinutes(10));
        
        // Store server -> users mapping
        redisTemplate.opsForSet().add(WhatsAppConstants.SERVER_USERS_KEY + serverId, userId);
        redisTemplate.expire(WhatsAppConstants.SERVER_USERS_KEY + serverId, Duration.ofMinutes(10));
        
        // Local cache for this server
        userConnections.put(userId, serverId);
        
        log.info("User {} connected to server {}", userId, serverId);
    }
    
    public void unregisterConnection(String userId) {
        String serverId = userConnections.remove(userId);
        if (serverId != null) {
            // Remove from Redis
            redisTemplate.delete(WhatsAppConstants.USER_SERVER_KEY + userId);
            redisTemplate.opsForSet().remove(WhatsAppConstants.SERVER_USERS_KEY + serverId, userId);
            
            log.info("User {} disconnected from server {}", userId, serverId);
        }
    }
    
    public String getUserServer(String userId) {
        // Check local cache first
        String serverId = userConnections.get(userId);
        if (serverId != null) {
            return serverId;
        }
        
        // Check Redis
        return (String) redisTemplate.opsForValue().get(WhatsAppConstants.USER_SERVER_KEY + userId);
    }
    
    public Set<Object> getServerUsers(String serverId) {
        return redisTemplate.opsForSet().members(WhatsAppConstants.SERVER_USERS_KEY + serverId);
    }
    
    public boolean isUserOnline(String userId) {
        return getUserServer(userId) != null;
    }
    
    public void heartbeat(String userId, String serverId) {
        // Extend TTL for user connection
        redisTemplate.expire(WhatsAppConstants.USER_SERVER_KEY + userId, Duration.ofMinutes(10));
        redisTemplate.expire(WhatsAppConstants.SERVER_USERS_KEY + serverId, Duration.ofMinutes(10));
    }
}