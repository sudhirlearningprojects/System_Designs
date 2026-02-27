package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ThumbnailService {
    
    public String generatePersonalizedThumbnail(UUID userId, UUID contentId) {
        return String.format("https://cdn.netflix.com/thumbnails/%s/%s.jpg", contentId, userId.hashCode() % 10);
    }
    
    public List<String> generateThumbnailVariants(UUID contentId, int count) {
        List<String> variants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            variants.add(String.format("https://cdn.netflix.com/thumbnails/%s/variant_%d.jpg", contentId, i));
        }
        return variants;
    }
}
