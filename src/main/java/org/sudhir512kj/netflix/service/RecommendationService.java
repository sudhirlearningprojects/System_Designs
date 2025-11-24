package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.sudhir512kj.netflix.model.ContentMetadata;
import org.sudhir512kj.netflix.repository.ViewingSessionRepository;
import java.time.Duration;

@Service
public class RecommendationService {
    
    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ViewingSessionRepository sessionRepository;
    
    public Flux<ContentMetadata> getPersonalizedRecommendations(Long userId) {
        String cacheKey = "recommendations:" + userId;
        
        return redisTemplate.opsForList().range(cacheKey, 0, 19)
            .switchIfEmpty(generateRecommendations(userId)
                .map(ContentMetadata::getContentId)
                .collectList()
                .flatMapMany(contentIds -> {
                    return redisTemplate.opsForList().rightPushAll(cacheKey, contentIds)
                        .then(redisTemplate.expire(cacheKey, Duration.ofHours(1)))
                        .thenMany(Flux.fromIterable(contentIds));
                }))
            .flatMap(this::getContentMetadata);
    }
    
    private Flux<ContentMetadata> generateRecommendations(Long userId) {
        return sessionRepository.findByUserId(userId)
            .take(20)
            .map(session -> new ContentMetadata(session.getContentId(), "Sample Title", "MOVIE"));
    }
    
    private Mono<ContentMetadata> getContentMetadata(String contentId) {
        return Mono.just(new ContentMetadata(contentId, "Sample Title", "MOVIE"));
    }
}