# Module 11: Geospatial Queries

## 🎯 Learning Objectives

- Store and query locations with Redis Geospatial
- Find nearby stores/warehouses
- Calculate distances
- Implement delivery radius check

---

## 11.1 How It Works

Redis Geo uses a Sorted Set internally with geohash as score.

```
GEOADD geo:stores longitude latitude "store-name"
→ Internally: ZADD geo:stores <geohash_score> "store-name"
```

---

## 11.2 Store Locator Service

```java
@Service
@RequiredArgsConstructor
public class StoreLocatorService {

    private final StringRedisTemplate redis;
    private static final String GEO_KEY = "geo:stores";

    // Add store location
    public void addStore(String storeId, double lat, double lng) {
        redis.opsForGeo().add(GEO_KEY, new Point(lng, lat), storeId);
    }

    // Add multiple stores
    public void addStores(Map<String, Point> stores) {
        stores.forEach((id, point) ->
            redis.opsForGeo().add(GEO_KEY, point, id)
        );
    }

    // Find stores within radius
    public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearby(
            double lat, double lng, double radiusKm) {

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
            redis.opsForGeo().radius(
                GEO_KEY,
                new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending()
                    .limit(20)
            );

        return results != null ? results.getContent() : List.of();
    }

    // Distance between two stores
    public Distance getDistance(String store1, String store2) {
        return redis.opsForGeo().distance(GEO_KEY, store1, store2, Metrics.KILOMETERS);
    }

    // Get store position
    public List<Point> getPosition(String storeId) {
        return redis.opsForGeo().position(GEO_KEY, storeId);
    }

    // Check if delivery is possible (within range)
    public boolean isDeliverable(double userLat, double userLng, double maxKm) {
        var nearby = findNearby(userLat, userLng, maxKm);
        return !nearby.isEmpty();
    }
}
```

---

## 11.3 REST Controller

```java
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreLocatorService storeService;

    @GetMapping("/nearby")
    public List<Map<String, Object>> findNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {

        return storeService.findNearby(lat, lng, radiusKm).stream()
            .map(result -> Map.<String, Object>of(
                "storeId", result.getContent().getName(),
                "distance", result.getDistance().getValue(),
                "unit", "km"
            ))
            .toList();
    }
}
```

---

## 11.4 Limitations

| Limitation | Detail |
|-----------|--------|
| Earth model | Assumes perfect sphere (slight inaccuracy at poles) |
| No polygons | Can't define complex delivery zones |
| No altitude | 2D only |
| Member size | Each member is a String (keep short) |
| Update = re-add | No in-place coordinate update |
| Max accuracy | ~0.5% error at edges of radius |

---

## ⏭️ Next: [Module 12: Search & Indexing](12_Search_Indexing.md)
