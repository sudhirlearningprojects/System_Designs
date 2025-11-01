package org.sudhir512kj.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.payment.dto.PaymentResponse;
import org.sudhir512kj.payment.model.IdempotencyCache;
import org.sudhir512kj.payment.repository.IdempotencyCacheRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    private final IdempotencyCacheRepository cacheRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String REDIS_PREFIX = "payment:idempotency:";
    
    public PaymentResponse getCachedResponse(String idempotencyKey) {
        try {
            // First check Redis cache
            String redisKey = REDIS_PREFIX + idempotencyKey;
            String cachedJson = redisTemplate.opsForValue().get(redisKey);
            
            if (cachedJson != null) {
                log.debug("Found cached response in Redis for key: {}", idempotencyKey);
                return objectMapper.readValue(cachedJson, PaymentResponse.class);
            }
            
            // Fallback to database
            Optional<IdempotencyCache> dbCache = cacheRepository.findById(idempotencyKey);
            if (dbCache.isPresent() && dbCache.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                log.debug("Found cached response in DB for key: {}", idempotencyKey);
                PaymentResponse response = objectMapper.readValue(
                    dbCache.get().getResponseData(), PaymentResponse.class);
                
                // Populate Redis cache
                redisTemplate.opsForValue().set(redisKey, dbCache.get().getResponseData(), CACHE_TTL);
                
                return response;
            }
            
        } catch (Exception e) {
            log.error("Error retrieving cached response for key: {}", idempotencyKey, e);
        }
        
        return null;
    }
    
    public void cacheResponse(String idempotencyKey, PaymentResponse response, UUID transactionId) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            
            // Cache in Redis
            String redisKey = REDIS_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(redisKey, responseJson, CACHE_TTL);
            
            // Cache in Database for durability
            IdempotencyCache dbCache = new IdempotencyCache();
            dbCache.setKey(idempotencyKey);
            dbCache.setTransactionId(transactionId);
            dbCache.setResponseData(responseJson);
            dbCache.setExpiresAt(LocalDateTime.now().plus(CACHE_TTL));
            
            cacheRepository.save(dbCache);
            
            log.debug("Cached response for idempotency key: {}", idempotencyKey);
            
        } catch (Exception e) {
            log.error("Error caching response for key: {}", idempotencyKey, e);
        }
    }
    
    public boolean isValidIdempotencyKey(String key) {
        return key != null && key.length() >= 16 && key.length() <= 255;
    }
    
    public void cleanupExpiredEntries() {
        try {
            cacheRepository.deleteExpiredEntries(LocalDateTime.now());
            log.debug("Cleaned up expired idempotency cache entries");
        } catch (Exception e) {
            log.error("Error cleaning up expired cache entries", e);
        }
    }
}