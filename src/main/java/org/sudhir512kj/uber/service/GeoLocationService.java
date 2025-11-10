package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeoLocationService {
    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaProducerService kafkaProducerService;
    private final CassandraLocationService cassandraLocationService;
    private static final String DRIVERS_ONLINE_KEY = "drivers:online";
    
    public GeoLocationService(RedisTemplate<String, String> redisTemplate, 
                             KafkaProducerService kafkaProducerService,
                             CassandraLocationService cassandraLocationService) {
        this.redisTemplate = redisTemplate;
        this.kafkaProducerService = kafkaProducerService;
        this.cassandraLocationService = cassandraLocationService;
    }

    public void updateDriverLocation(UUID driverId, Location location) {
        log.info("Updating location for driver {}: lat={}, lng={}", driverId, location.getLatitude(), location.getLongitude());
        
        // 1. Update Redis (hot data) - for real-time matching
        String key = getGeoKey(location);
        Point point = new Point(location.getLongitude(), location.getLatitude());
        redisTemplate.opsForGeo().add(key, point, driverId.toString());
        
        Map<String, String> driverInfo = Map.of(
            "latitude", location.getLatitude().toString(),
            "longitude", location.getLongitude().toString(),
            "updated_at", String.valueOf(System.currentTimeMillis())
        );
        redisTemplate.opsForHash().putAll("driver:" + driverId, driverInfo);
        
        // 2. Publish to Kafka (event streaming) - for analytics
        if (kafkaProducerService != null) {
            kafkaProducerService.publishLocationUpdate(driverId, location);
        }
        
        // 3. Archive to Cassandra (cold storage) - for historical analysis
        if (cassandraLocationService != null) {
            cassandraLocationService.saveLocationHistory(driverId, location);
        }
    }

    public List<UUID> findNearbyDrivers(Location location, double radiusKm, int limit) {
        String key = getGeoKey(location);
        Point point = new Point(location.getLongitude(), location.getLatitude());
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        
        Circle circle = new Circle(point, distance);
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
            .newGeoRadiusArgs()
            .includeDistance()
            .sortAscending()
            .limit(limit);
        
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
            redisTemplate.opsForGeo().radius(key, circle, args);
        
        if (results == null) return Collections.emptyList();
        
        return results.getContent().stream()
            .map(result -> UUID.fromString(result.getContent().getName()))
            .collect(Collectors.toList());
    }

    public void removeDriver(UUID driverId, Location location) {
        String key = getGeoKey(location);
        redisTemplate.opsForGeo().remove(key, driverId.toString());
        redisTemplate.delete("driver:" + driverId);
        log.info("Removed driver {} from geo-index", driverId);
    }

    private String getGeoKey(Location location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return DRIVERS_ONLINE_KEY + ":cell:0_0";
        }
        String geohash = String.format("%d_%d", location.getLatitude().intValue() * 100, location.getLongitude().intValue() * 100);
        return DRIVERS_ONLINE_KEY + ":cell:" + geohash;
    }
}
