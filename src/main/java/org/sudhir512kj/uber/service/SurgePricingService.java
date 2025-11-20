package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Surge Pricing Service - Uber's Dynamic Pricing Algorithm
 * 
 * Calculates surge multiplier based on real-time demand/supply ratio
 * Formula: surge = 1.0 + (demand/supply - 1.0) * sensitivity
 * 
 * Features:
 * - Real-time demand/supply tracking per H3 cell
 * - Time-based adjustments (peak hours)
 * - Exponential moving average smoothing
 * - Min: 1.0x, Max: 3.0x
 */
@Service
public class SurgePricingService {
    private static final Logger log = LoggerFactory.getLogger(SurgePricingService.class);
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final double MIN_MULTIPLIER = 1.0;
    private static final double MAX_MULTIPLIER = 3.0;
    private static final double SURGE_SENSITIVITY = 0.5;
    private static final double SMOOTHING_FACTOR = 0.3;
    private static final long CACHE_TTL_SECONDS = 60;
    
    public SurgePricingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BigDecimal calculateSurgeMultiplier(Location location) {
        String geohash = getGeohash(location);
        String cacheKey = "surge:" + geohash;
        
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new BigDecimal(cached);
        }
        
        int demand = getDemand(geohash);
        int supply = getSupply(geohash);
        
        double multiplier = calculateMultiplier(demand, supply);
        multiplier = applyTimeBasedAdjustment(multiplier);
        multiplier = applySmoothingEMA(geohash, multiplier);
        multiplier = Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, multiplier));
        
        BigDecimal result = BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(cacheKey, result.toString(), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        
        log.info("Surge for {}: demand={}, supply={}, multiplier={}", geohash, demand, supply, result);
        return result;
    }
    
    private double calculateMultiplier(int demand, int supply) {
        if (supply == 0) return MAX_MULTIPLIER;
        if (demand == 0) return MIN_MULTIPLIER;
        
        double ratio = (double) demand / supply;
        if (ratio <= 1.0) return MIN_MULTIPLIER;
        
        return 1.0 + (ratio - 1.0) * SURGE_SENSITIVITY;
    }
    
    private double applyTimeBasedAdjustment(double base) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int day = now.getDayOfWeek().getValue();
        
        double adj = 1.0;
        if (hour >= 7 && hour < 9) adj = 1.1;           // Morning rush
        else if (hour >= 17 && hour < 19) adj = 1.15;   // Evening rush
        else if (hour >= 23 || hour < 2) adj = 1.2;     // Late night
        
        if ((day == 5 || day == 6) && (hour >= 20 || hour < 2)) {
            adj = Math.max(adj, 1.1);  // Weekend nights
        }
        
        return base * adj;
    }
    
    private double applySmoothingEMA(String geohash, double newValue) {
        String emaKey = "surge:ema:" + geohash;
        String prev = redisTemplate.opsForValue().get(emaKey);
        
        if (prev == null) {
            redisTemplate.opsForValue().set(emaKey, String.valueOf(newValue), CACHE_TTL_SECONDS * 2, TimeUnit.SECONDS);
            return newValue;
        }
        
        double prevEMA = Double.parseDouble(prev);
        double smoothed = SMOOTHING_FACTOR * newValue + (1 - SMOOTHING_FACTOR) * prevEMA;
        redisTemplate.opsForValue().set(emaKey, String.valueOf(smoothed), CACHE_TTL_SECONDS * 2, TimeUnit.SECONDS);
        return smoothed;
    }
    
    private int getDemand(String geohash) {
        String key = "surge:demand:" + geohash;
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }
    
    private int getSupply(String geohash) {
        String key = "surge:supply:" + geohash;
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 1;
    }
    
    public void incrementDemand(Location location) {
        if (location == null) return;
        String key = "surge:demand:" + getGeohash(location);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }
    
    public void decrementDemand(Location location) {
        if (location == null) return;
        String key = "surge:demand:" + getGeohash(location);
        redisTemplate.opsForValue().decrement(key);
    }
    
    public void updateSupply(Location location, int delta) {
        if (location == null) return;
        String key = "surge:supply:" + getGeohash(location);
        redisTemplate.opsForValue().increment(key, delta);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }
    
    public Map<String, Object> getSurgeInfo(Location location) {
        String geohash = getGeohash(location);
        int demand = getDemand(geohash);
        int supply = getSupply(geohash);
        BigDecimal multiplier = calculateSurgeMultiplier(location);
        
        return Map.of(
            "demand", demand,
            "supply", supply,
            "surge_multiplier", multiplier,
            "is_surge_active", multiplier.compareTo(BigDecimal.valueOf(1.2)) > 0
        );
    }
    
    private String getGeohash(Location location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return "0_0";
        }
        return String.format("%d_%d", location.getLatitude().intValue() * 100, location.getLongitude().intValue() * 100);
    }
}
