package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SurgePricingService {
    private static final Logger log = LoggerFactory.getLogger(SurgePricingService.class);
    private final RedisTemplate<String, String> redisTemplate;
    
    public SurgePricingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BigDecimal calculateSurgeMultiplier(Location location) {
        String geohash = getGeohash(location);
        
        // Get demand and supply from Redis
        String demandKey = "surge:demand:" + geohash;
        String supplyKey = "surge:supply:" + geohash;
        
        String demandStr = redisTemplate.opsForValue().get(demandKey);
        String supplyStr = redisTemplate.opsForValue().get(supplyKey);
        
        int demand = demandStr != null ? Integer.parseInt(demandStr) : 0;
        int supply = supplyStr != null ? Integer.parseInt(supplyStr) : 1;
        
        // Calculate surge: min(demand/supply, 3.0)
        double ratio = supply > 0 ? (double) demand / supply : 1.0;
        double surge = Math.min(ratio, 3.0);
        
        log.debug("Surge pricing for {}: demand={}, supply={}, multiplier={}", 
            geohash, demand, supply, surge);
        
        return BigDecimal.valueOf(Math.max(surge, 1.0)).setScale(2, RoundingMode.HALF_UP);
    }
    
    public void updateDemand(Location location, int delta) {
        if (location == null) return;
        String geohash = getGeohash(location);
        String key = "surge:demand:" + geohash;
        redisTemplate.opsForValue().increment(key, delta);
    }
    
    public void updateSupply(Location location, int delta) {
        if (location == null) return;
        String geohash = getGeohash(location);
        String key = "surge:supply:" + geohash;
        redisTemplate.opsForValue().increment(key, delta);
    }
    
    private String getGeohash(Location location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return "0_0";
        }
        return String.format("%d_%d", location.getLatitude().intValue() * 100, location.getLongitude().intValue() * 100);
    }
}
