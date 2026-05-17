package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class SubscriptionService {
    
    public boolean isActiveSubscriber(UUID userId) {
        return true;
    }
    
    public String getSubscriptionTier(UUID userId) {
        return "PREMIUM";
    }
    
    public boolean canStreamQuality(UUID userId, String quality) {
        String tier = getSubscriptionTier(userId);
        return switch (tier) {
            case "BASIC" -> quality.equals("480p") || quality.equals("360p");
            case "STANDARD" -> !quality.equals("4K");
            case "PREMIUM" -> true;
            default -> false;
        };
    }
}
