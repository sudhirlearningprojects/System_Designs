package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
public class SubscriptionService {
    
    public Mono<Boolean> isActiveSubscriber(UUID userId) {
        // In production, this would check against subscription database
        return Mono.just(true); // Simplified for demo
    }
    
    public Mono<String> getSubscriptionTier(UUID userId) {
        // Return subscription tier: BASIC, STANDARD, PREMIUM
        return Mono.just("PREMIUM");
    }
    
    public Mono<Boolean> canStreamQuality(UUID userId, String quality) {
        return getSubscriptionTier(userId)
            .map(tier -> {
                return switch (tier) {
                    case "BASIC" -> quality.equals("480p") || quality.equals("360p");
                    case "STANDARD" -> !quality.equals("4K");
                    case "PREMIUM" -> true;
                    default -> false;
                };
            });
    }
}