package org.sudhir512kj.redis.protocol;

import lombok.RequiredArgsConstructor;
import org.sudhir512kj.redis.service.RedisService;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RedisProtocolHandler {
    private final RedisService redisService;
    
    public String processCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) return "-ERR empty command\r\n";
        
        String cmd = parts[0].toUpperCase();
        
        try {
            return switch (cmd) {
                case "SET" -> handleSet(parts);
                case "GET" -> handleGet(parts);
                case "DEL" -> handleDel(parts);
                case "EXISTS" -> handleExists(parts);
                case "EXPIRE" -> handleExpire(parts);
                case "LPUSH" -> handleLpush(parts);
                case "LPOP" -> handleLpop(parts);
                case "SADD" -> handleSadd(parts);
                case "SISMEMBER" -> handleSismember(parts);
                case "HSET" -> handleHset(parts);
                case "HGET" -> handleHget(parts);
                case "PING" -> "+PONG\r\n";
                default -> "-ERR unknown command '" + cmd + "'\r\n";
            };
        } catch (Exception e) {
            return "-ERR " + e.getMessage() + "\r\n";
        }
    }
    
    private String handleSet(String[] parts) {
        if (parts.length < 3) return "-ERR wrong number of arguments\r\n";
        
        String key = parts[1];
        String value = parts[2];
        Duration ttl = null;
        
        if (parts.length >= 5 && "EX".equalsIgnoreCase(parts[3])) {
            ttl = Duration.ofSeconds(Long.parseLong(parts[4]));
        }
        
        redisService.set(key, value, ttl);
        return "+OK\r\n";
    }
    
    private String handleGet(String[] parts) {
        if (parts.length != 2) return "-ERR wrong number of arguments\r\n";
        
        String value = redisService.get(parts[1]);
        return value != null ? "+" + value + "\r\n" : "$-1\r\n";
    }
    
    private String handleDel(String[] parts) {
        if (parts.length != 2) return "-ERR wrong number of arguments\r\n";
        
        boolean deleted = redisService.del(parts[1]);
        return ":" + (deleted ? 1 : 0) + "\r\n";
    }
    
    private String handleExists(String[] parts) {
        if (parts.length != 2) return "-ERR wrong number of arguments\r\n";
        
        boolean exists = redisService.exists(parts[1]);
        return ":" + (exists ? 1 : 0) + "\r\n";
    }
    
    private String handleExpire(String[] parts) {
        if (parts.length != 3) return "-ERR wrong number of arguments\r\n";
        
        String key = parts[1];
        int seconds = Integer.parseInt(parts[2]);
        boolean result = redisService.expire(key, Duration.ofSeconds(seconds));
        return ":" + (result ? 1 : 0) + "\r\n";
    }
    
    private String handleLpush(String[] parts) {
        if (parts.length < 3) return "-ERR wrong number of arguments\r\n";
        
        String key = parts[1];
        String[] values = Arrays.copyOfRange(parts, 2, parts.length);
        int size = redisService.lpush(key, values);
        return ":" + size + "\r\n";
    }
    
    private String handleLpop(String[] parts) {
        if (parts.length != 2) return "-ERR wrong number of arguments\r\n";
        
        String value = redisService.lpop(parts[1]);
        return value != null ? "+" + value + "\r\n" : "$-1\r\n";
    }
    
    private String handleSadd(String[] parts) {
        if (parts.length < 3) return "-ERR wrong number of arguments\r\n";
        
        String key = parts[1];
        String[] members = Arrays.copyOfRange(parts, 2, parts.length);
        int added = redisService.sadd(key, members);
        return ":" + added + "\r\n";
    }
    
    private String handleSismember(String[] parts) {
        if (parts.length != 3) return "-ERR wrong number of arguments\r\n";
        
        boolean exists = redisService.sismember(parts[1], parts[2]);
        return ":" + (exists ? 1 : 0) + "\r\n";
    }
    
    private String handleHset(String[] parts) {
        if (parts.length != 4) return "-ERR wrong number of arguments\r\n";
        
        redisService.hset(parts[1], parts[2], parts[3]);
        return "+OK\r\n";
    }
    
    private String handleHget(String[] parts) {
        if (parts.length != 3) return "-ERR wrong number of arguments\r\n";
        
        String value = redisService.hget(parts[1], parts[2]);
        return value != null ? "+" + value + "\r\n" : "$-1\r\n";
    }
}