package org.sudhir512kj.uber.service;

import com.uber.h3core.H3Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import org.sudhir512kj.uber.repository.DriverRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * H3-based Geo-Spatial Service
 * Uses Uber's H3 hexagonal hierarchical spatial index for efficient driver matching
 * 
 * Benefits over traditional Geohash:
 * - Uniform cell sizes (no edge distortion)
 * - Better neighbor lookup (6 adjacent hexagons)
 * - Hierarchical resolutions (16 levels)
 * - 10x faster proximity queries
 */
@Service
public class H3GeoService {
    private static final Logger log = LoggerFactory.getLogger(H3GeoService.class);
    private final H3Core h3;
    private final RedisTemplate<String, String> redisTemplate;
    private final DriverRepository driverRepository;
    
    // H3 Resolutions (Uber's production setup)
    private static final int CITY_RESOLUTION = 5;      // ~252 km² - City-level sharding
    private static final int NEIGHBORHOOD_RESOLUTION = 7; // ~5.16 km² - Neighborhood matching
    private static final int MATCHING_RESOLUTION = 9;  // ~0.105 km² - Driver search (100m radius)
    private static final int PRECISE_RESOLUTION = 11;  // ~0.0016 km² - Precise tracking
    
    public H3GeoService(RedisTemplate<String, String> redisTemplate, 
                       DriverRepository driverRepository) throws IOException {
        this.h3 = H3Core.newInstance();
        this.redisTemplate = redisTemplate;
        this.driverRepository = driverRepository;
    }

    /**
     * Update driver location using H3 indexing
     * Stores driver in multiple resolution levels for efficient querying
     */
    public void updateDriverLocation(UUID driverId, Location location) {
        try {
            // Get H3 cell IDs at different resolutions
            String matchingCell = getH3Cell(location, MATCHING_RESOLUTION);
            String neighborhoodCell = getH3Cell(location, NEIGHBORHOOD_RESOLUTION);
            String cityCell = getH3Cell(location, CITY_RESOLUTION);
            
            // Store in Redis Sets for O(1) lookup
            // Resolution 9: For immediate matching (100m radius)
            redisTemplate.opsForSet().add("drivers:h3:r9:" + matchingCell, driverId.toString());
            
            // Resolution 7: For expanded search (5km radius)
            redisTemplate.opsForSet().add("drivers:h3:r7:" + neighborhoodCell, driverId.toString());
            
            // Resolution 5: For city-level analytics
            redisTemplate.opsForSet().add("drivers:h3:r5:" + cityCell, driverId.toString());
            
            // Store driver metadata
            Map<String, String> driverData = Map.of(
                "lat", location.getLatitude().toString(),
                "lng", location.getLongitude().toString(),
                "h3_r9", matchingCell,
                "h3_r7", neighborhoodCell,
                "updated_at", String.valueOf(System.currentTimeMillis())
            );
            redisTemplate.opsForHash().putAll("driver:" + driverId, driverData);
            
            log.debug("Updated driver {} location: h3_r9={}", driverId, matchingCell);
        } catch (Exception e) {
            log.error("Failed to update driver location", e);
        }
    }

    /**
     * Find nearby drivers using H3 hierarchical search
     * 
     * Algorithm:
     * 1. Get rider's H3 cell (resolution 9)
     * 2. Search in center cell + 6 neighbors (7 cells total)
     * 3. If < 10 drivers, expand to resolution 7 (5km radius)
     * 4. Return up to 'limit' drivers
     */
    public List<UUID> findNearbyDrivers(Location location, double radiusKm, int limit) {
        try {
            Set<UUID> driverIds = new HashSet<>();
            
            // Level 1: Search in resolution 9 cells (100m radius)
            String centerCell = getH3Cell(location, MATCHING_RESOLUTION);
            List<String> searchCells = getNeighborCells(centerCell);
            searchCells.add(centerCell); // Include center
            
            for (String cell : searchCells) {
                Set<String> cellDrivers = redisTemplate.opsForSet()
                    .members("drivers:h3:r9:" + cell);
                if (cellDrivers != null) {
                    cellDrivers.stream()
                        .map(UUID::fromString)
                        .forEach(driverIds::add);
                }
                
                if (driverIds.size() >= limit) break;
            }
            
            // Level 2: If not enough drivers, expand to resolution 7 (5km radius)
            if (driverIds.size() < 10 && radiusKm >= 5.0) {
                String neighborhoodCell = getH3Cell(location, NEIGHBORHOOD_RESOLUTION);
                List<String> expandedCells = getNeighborCells(neighborhoodCell);
                expandedCells.add(neighborhoodCell);
                
                for (String cell : expandedCells) {
                    Set<String> cellDrivers = redisTemplate.opsForSet()
                        .members("drivers:h3:r7:" + cell);
                    if (cellDrivers != null) {
                        cellDrivers.stream()
                            .map(UUID::fromString)
                            .forEach(driverIds::add);
                    }
                    
                    if (driverIds.size() >= limit) break;
                }
            }
            
            log.info("Found {} drivers near location using H3", driverIds.size());
            return new ArrayList<>(driverIds).subList(0, Math.min(driverIds.size(), limit));
            
        } catch (Exception e) {
            log.error("Failed to find nearby drivers", e);
            return Collections.emptyList();
        }
    }

    /**
     * Remove driver from H3 index when going offline
     */
    public void removeDriver(UUID driverId) {
        try {
            // Get driver's current H3 cells
            Map<Object, Object> driverData = redisTemplate.opsForHash()
                .entries("driver:" + driverId);
            
            if (driverData.isEmpty()) return;
            
            String h3R9 = (String) driverData.get("h3_r9");
            String h3R7 = (String) driverData.get("h3_r7");
            
            // Remove from all resolution levels
            if (h3R9 != null) {
                redisTemplate.opsForSet().remove("drivers:h3:r9:" + h3R9, driverId.toString());
            }
            if (h3R7 != null) {
                redisTemplate.opsForSet().remove("drivers:h3:r7:" + h3R7, driverId.toString());
            }
            
            // Remove driver metadata
            redisTemplate.delete("driver:" + driverId);
            
            log.info("Removed driver {} from H3 index", driverId);
        } catch (Exception e) {
            log.error("Failed to remove driver from H3 index", e);
        }
    }

    /**
     * Get H3 cell ID for a location at specified resolution
     */
    private String getH3Cell(Location location, int resolution) {
        try {
            long cellId = h3.latLngToCell(
                location.getLatitude(), 
                location.getLongitude(), 
                resolution
            );
            return h3.h3ToString(cellId);
        } catch (Exception e) {
            log.error("Failed to get H3 cell", e);
            return "0";
        }
    }

    /**
     * Get 6 neighboring hexagonal cells (H3's k-ring with k=1)
     */
    private List<String> getNeighborCells(String cellId) {
        try {
            return new ArrayList<>(h3.gridDisk(cellId, 1));
        } catch (Exception e) {
            log.error("Failed to get neighbor cells", e);
            return Collections.emptyList();
        }
    }


}
