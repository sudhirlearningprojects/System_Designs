# Module 12: Search & Indexing (RediSearch)

## 🎯 Learning Objectives

- Full-text search with RediSearch module
- Create indexes on Hash/JSON data
- Autocomplete suggestions
- Aggregations and filtering

---

## 12.1 Setup RediSearch

```yaml
# docker-compose.yml - use Redis Stack (includes RediSearch)
services:
  redis:
    image: redis/redis-stack:latest  # Includes RediSearch, RedisJSON
    ports:
      - "6379:6379"
      - "8001:8001"  # RedisInsight built-in
```

---

## 12.2 Creating Indexes

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final StringRedisTemplate redis;

    /**
     * Create search index on product hashes.
     * Run once at startup.
     */
    @PostConstruct
    public void createIndex() {
        try {
            // FT.CREATE idx:products ON HASH PREFIX 1 product:
            //   SCHEMA name TEXT SORTABLE
            //          category TAG
            //          price NUMERIC SORTABLE
            //          description TEXT
            redis.execute((RedisCallback<Object>) connection -> {
                connection.execute("FT.CREATE", 
                    "idx:products".getBytes(),
                    "ON".getBytes(), "HASH".getBytes(),
                    "PREFIX".getBytes(), "1".getBytes(), "product:".getBytes(),
                    "SCHEMA".getBytes(),
                    "name".getBytes(), "TEXT".getBytes(), "SORTABLE".getBytes(),
                    "category".getBytes(), "TAG".getBytes(),
                    "price".getBytes(), "NUMERIC".getBytes(), "SORTABLE".getBytes(),
                    "description".getBytes(), "TEXT".getBytes(),
                    "brand".getBytes(), "TAG".getBytes()
                );
                return null;
            });
            log.info("Search index created: idx:products");
        } catch (Exception e) {
            log.debug("Index already exists or error: {}", e.getMessage());
        }
    }

    /**
     * Index a product (just save as Hash - auto-indexed)
     */
    public void indexProduct(Long id, String name, String category,
                             double price, String description, String brand) {
        Map<String, String> fields = Map.of(
            "name", name,
            "category", category,
            "price", String.valueOf(price),
            "description", description,
            "brand", brand
        );
        redis.opsForHash().putAll("product:" + id, fields);
    }

    /**
     * Full-text search: FT.SEARCH idx:products "wireless headphones"
     */
    public List<Map<String, String>> search(String query) {
        // Using Jedis or Lettuce raw commands for FT.SEARCH
        // Spring Data Redis doesn't have native RediSearch support yet
        // Use redis-om-spring or raw commands

        List<Object> results = redis.execute((RedisCallback<List<Object>>) connection -> {
            return (List<Object>) connection.execute("FT.SEARCH",
                "idx:products".getBytes(),
                query.getBytes(),
                "LIMIT".getBytes(), "0".getBytes(), "20".getBytes()
            );
        });

        return parseSearchResults(results);
    }

    /**
     * Filter by category + price range
     * FT.SEARCH idx:products "@category:{electronics} @price:[100 5000]"
     */
    public List<Map<String, String>> searchWithFilters(
            String text, String category, Double minPrice, Double maxPrice) {

        StringBuilder query = new StringBuilder();
        if (text != null) query.append(text).append(" ");
        if (category != null) query.append("@category:{").append(category).append("} ");
        if (minPrice != null && maxPrice != null) {
            query.append("@price:[").append(minPrice).append(" ").append(maxPrice).append("]");
        }

        return search(query.toString().trim());
    }
}
```

---

## 12.3 Autocomplete

```java
@Service
@RequiredArgsConstructor
public class AutocompleteService {

    private final StringRedisTemplate redis;
    private static final String SUGGEST_KEY = "suggest:products";

    // Add suggestion
    public void addSuggestion(String term, double score) {
        // FT.SUGADD suggest:products "iPhone 15 Pro" 100
        redis.execute((RedisCallback<Object>) connection -> {
            connection.execute("FT.SUGADD",
                SUGGEST_KEY.getBytes(),
                term.getBytes(),
                String.valueOf(score).getBytes()
            );
            return null;
        });
    }

    // Get suggestions
    public List<String> getSuggestions(String prefix, int max) {
        // FT.SUGGET suggest:products "iph" FUZZY MAX 5
        List<Object> results = redis.execute((RedisCallback<List<Object>>) connection -> {
            return (List<Object>) connection.execute("FT.SUGGET",
                SUGGEST_KEY.getBytes(),
                prefix.getBytes(),
                "FUZZY".getBytes(),
                "MAX".getBytes(),
                String.valueOf(max).getBytes()
            );
        });
        // Parse results to List<String>
        return results != null ? results.stream()
            .map(Object::toString).toList() : List.of();
    }
}
```

---

## 12.4 Limitations

| Limitation | Detail |
|-----------|--------|
| Module required | RediSearch must be loaded (use Redis Stack) |
| Memory overhead | Index consumes additional RAM |
| No joins | Can't search across related entities |
| Cluster support | Index must be on same shard as data |
| Spring support | No official Spring Data RediSearch (use redis-om-spring) |

---

## ⏭️ Next: [Module 13: Persistence & HA](13_Persistence_HA.md)
