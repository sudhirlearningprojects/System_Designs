package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ABTestingService {
    private final Map<String, String> userVariants = new ConcurrentHashMap<>();
    private final Map<String, ExperimentConfig> experiments = new ConcurrentHashMap<>();
    
    public void createExperiment(String experimentId, List<String> variants, Map<String, Integer> distribution) {
        experiments.put(experimentId, new ExperimentConfig(variants, distribution));
    }
    
    public String assignVariant(String experimentId, UUID userId) {
        ExperimentConfig config = experiments.get(experimentId);
        if (config == null) return "default";
        
        String key = userId + ":" + experimentId;
        return userVariants.computeIfAbsent(key, k -> {
            int hash = Math.abs(userId.hashCode()) % 100;
            int cumulative = 0;
            for (Map.Entry<String, Integer> entry : config.distribution.entrySet()) {
                cumulative += entry.getValue();
                if (hash < cumulative) {
                    return entry.getKey();
                }
            }
            return config.variants.get(0);
        });
    }
    
    public String getVariant(UUID userId, String experimentName) {
        return assignVariant(experimentName, userId);
    }
    
    public boolean isVariantA(UUID userId, String experimentName) {
        return "A".equals(getVariant(userId, experimentName));
    }
    
    public void trackEvent(UUID userId, String experimentName, String event) {
        // Track experiment events
    }
    
    private static class ExperimentConfig {
        final List<String> variants;
        final Map<String, Integer> distribution;
        
        ExperimentConfig(List<String> variants, Map<String, Integer> distribution) {
            this.variants = variants;
            this.distribution = distribution;
        }
    }
}