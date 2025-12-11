# Database Scaling for Read-Heavy E-commerce Platform

## Problem Statement
Your e-commerce platform (like Flipkart) sees massive read traffic (users browsing products), but relatively fewer writes. How would you scale your database?

## Read:Write Ratio
- **Reads**: 95% (product browsing, search, catalog views)
- **Writes**: 5% (orders, inventory updates, reviews)

---

## 1. Read Replicas (Primary Strategy)

### Architecture
```
Primary DB (Master)          Read Replicas (Slaves)
     ↓ writes                    ↑ reads (90%+)
     |                          /  |  \
     └─→ Async Replication ────→  R1  R2  R3
```

### Configuration
- **1 Primary** for all writes
- **5-10 Read Replicas** for reads
- **Async Replication** (eventual consistency acceptable)
- **Load Balancer** distributes read traffic

### Benefits
- Handles 10x-100x more read traffic
- Horizontal scaling by adding replicas
- Geographic distribution (replicas in different regions)
- Isolated read workload from writes

### Implementation
```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://primary-db:5432/ecommerce")
            .build();
    }
    
    @Bean
    public DataSource readReplicaDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://read-replica:5432/ecommerce")
            .build();
    }
}

// Route reads to replicas
@Transactional(readOnly = true)
public List<Product> getProducts() {
    return productRepository.findAll(); // Goes to read replica
}

// Route writes to primary
@Transactional
public Product createProduct(Product product) {
    return productRepository.save(product); // Goes to primary
}
```

---

## 2. Multi-Layer Caching Strategy

### Request Flow
```
User → CDN → Application Cache → Redis → Read Replica → Primary DB
       ↓        ↓                  ↓         ↓            ↓
      95%      4%                 0.8%      0.19%       0.01%
```

### L1 - Application Cache (Caffeine/Guava)
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("products", "categories");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats());
        return cacheManager;
    }
}

@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#productId")
    public Product getProduct(String productId) {
        return productRepository.findById(productId);
    }
}
```

### L2 - Redis (Distributed Cache)
```java
@Service
public class ProductCacheService {
    
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    // Hot products (trending items)
    public void cacheHotProduct(String productId, Product product) {
        redisTemplate.opsForValue().set(
            "product:" + productId, 
            product, 
            1, 
            TimeUnit.HOURS
        );
    }
    
    // Product catalog by category
    public void cacheCatalog(String categoryId, List<Product> products) {
        redisTemplate.opsForHash().put("catalog", categoryId, products);
    }
    
    // Search results
    public void cacheSearchResults(String query, List<Product> results) {
        redisTemplate.opsForValue().set(
            "search:" + query, 
            results, 
            30, 
            TimeUnit.MINUTES
        );
    }
}
```

### Cache Strategy by Data Temperature
| Data Type | TTL | Hit Rate | Example |
|-----------|-----|----------|---------|
| Hot Data | 1 hour | 95% | Top 1000 products |
| Warm Data | 6 hours | 80% | Popular categories |
| Cold Data | 24 hours | 50% | Long-tail products |

---

## 3. Database Sharding

### Sharding Strategy 1: Category-Based
```
Shard by Category:
- Shard 1: Electronics (high traffic)
- Shard 2: Fashion (high traffic)
- Shard 3: Home & Kitchen
- Shard 4: Books & Media
```

### Sharding Strategy 2: Hash-Based
```java
@Service
public class ShardingService {
    
    private static final int NUM_SHARDS = 8;
    
    @Autowired
    private Map<Integer, DataSource> shardRegistry;
    
    public DataSource getShardForProduct(String productId) {
        int shardId = Math.abs(productId.hashCode()) % NUM_SHARDS;
        return shardRegistry.get(shardId);
    }
    
    public Product getProduct(String productId) {
        DataSource shard = getShardForProduct(productId);
        // Execute query on specific shard
        return jdbcTemplate.queryForObject(
            shard, 
            "SELECT * FROM products WHERE id = ?", 
            productId
        );
    }
}
```

### Benefits
- Distribute 100M+ products across shards
- Each shard handles 10-20M products
- Parallel query execution
- Reduced contention and lock wait times

---

## 4. CQRS Pattern (Command Query Responsibility Segregation)

### Architecture
```
Write Path:                    Read Path:
User → API → Primary DB       User → API → Read-Optimized Store
              ↓                              ↑
              └─→ Event Bus ─────────────────┘
                  (Kafka)
```

### Write Model (Normalized)
```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    
    @ManyToOne
    private Category category;
    
    @ManyToOne
    private Brand brand;
}
```

### Read Model (Denormalized)
```java
@Document(collection = "product_catalog")
public class ProductView {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    
    // Denormalized fields
    private String categoryName;
    private String categoryPath;
    private String brandName;
    private List<String> imageUrls;
    
    // Pre-calculated aggregations
    private Double avgRating;
    private Integer reviewCount;
    private Integer stockCount;
    private Boolean inStock;
}
```

### Event-Driven Sync
```java
@Service
public class ProductEventHandler {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @KafkaListener(topics = "product-events")
    public void handleProductEvent(ProductEvent event) {
        switch (event.getType()) {
            case CREATED:
            case UPDATED:
                ProductView view = buildProductView(event.getProduct());
                mongoTemplate.save(view);
                break;
            case DELETED:
                mongoTemplate.remove(
                    Query.query(Criteria.where("id").is(event.getProductId())),
                    ProductView.class
                );
                break;
        }
    }
}
```

### Benefits
- Optimized read models (denormalized, pre-aggregated)
- Use MongoDB/Elasticsearch for read models
- Primary DB only handles writes
- Independent scaling of read and write paths

---

## 5. Materialized Views for Complex Queries

### Pre-compute Expensive Aggregations
```sql
-- Create materialized view
CREATE MATERIALIZED VIEW product_stats AS
SELECT 
    p.product_id,
    p.name,
    p.price,
    c.name as category_name,
    b.name as brand_name,
    AVG(r.rating) as avg_rating,
    COUNT(r.id) as review_count,
    COUNT(DISTINCT o.user_id) as purchase_count,
    SUM(o.quantity) as total_sold
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN brands b ON p.brand_id = b.id
LEFT JOIN reviews r ON p.product_id = r.product_id
LEFT JOIN order_items o ON p.product_id = o.product_id
GROUP BY p.product_id, p.name, p.price, c.name, b.name;

-- Create index for fast lookups
CREATE INDEX idx_product_stats_category ON product_stats(category_name);
CREATE INDEX idx_product_stats_rating ON product_stats(avg_rating DESC);

-- Refresh periodically (every 15 minutes)
REFRESH MATERIALIZED VIEW CONCURRENTLY product_stats;
```

### Scheduled Refresh
```java
@Service
public class MaterializedViewRefreshService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void refreshProductStats() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY product_stats");
    }
}
```

---

## 6. Search Engine for Product Discovery

### Elasticsearch Integration
```java
@Document(indexName = "products")
public class ProductSearchDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Keyword)
    private String brand;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    @Field(type = FieldType.Double)
    private Double rating;
    
    @Field(type = FieldType.Boolean)
    private Boolean inStock;
}

@Service
public class ProductSearchService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    public List<Product> searchProducts(String query, SearchFilters filters) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery(query, "name", "description"))
            .withFilter(buildFilters(filters))
            .withSort(SortBuilders.fieldSort("rating").order(SortOrder.DESC))
            .withPageable(PageRequest.of(0, 20))
            .build();
        
        return elasticsearchOperations.search(searchQuery, ProductSearchDocument.class)
            .stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }
}
```

### Benefits
- Full-text search on product names/descriptions
- Faceted search (filters by price, brand, rating)
- Autocomplete suggestions
- Handles 100K+ search queries/sec
- Sub-second response times

---

## 7. Connection Pooling Optimization

### HikariCP Configuration
```yaml
spring:
  datasource:
    hikari:
      # Connection pool settings
      maximum-pool-size: 50        # Per replica
      minimum-idle: 10
      connection-timeout: 20000    # 20 seconds
      idle-timeout: 300000         # 5 minutes
      max-lifetime: 1200000        # 20 minutes
      
      # Performance tuning
      leak-detection-threshold: 60000
      pool-name: EcommerceHikariPool
      
      # Connection test query
      connection-test-query: SELECT 1
```

### Dynamic Pool Sizing
```java
@Configuration
public class DynamicDataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Calculate pool size based on available cores
        int cores = Runtime.getRuntime().availableProcessors();
        config.setMaximumPoolSize(cores * 2 + 1);
        config.setMinimumIdle(cores);
        
        return new HikariDataSource(config);
    }
}
```

---

## 8. Database Partitioning

### Partition by Date (Archive Old Data)
```sql
-- Partition orders table by date
CREATE TABLE orders (
    order_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    total_amount DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(50)
) PARTITION BY RANGE (created_at);

-- Create partitions for each quarter
CREATE TABLE orders_2024_q1 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE orders_2024_q2 PARTITION OF orders
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE orders_2024_q3 PARTITION OF orders
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE orders_2024_q4 PARTITION OF orders
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

-- Create indexes on each partition
CREATE INDEX idx_orders_2024_q1_user ON orders_2024_q1(user_id);
CREATE INDEX idx_orders_2024_q2_user ON orders_2024_q2(user_id);
```

### Benefits
- Faster queries (partition pruning)
- Easy archival of old data
- Improved maintenance operations
- Better index performance

---

## Complete Architecture

```
                    ┌─────────────┐
                    │   CDN       │ (Static assets, images)
                    └─────────────┘
                           ↓
                    ┌─────────────┐
                    │ Load Balancer│
                    └─────────────┘
                           ↓
        ┌──────────────────┴──────────────────┐
        ↓                                      ↓
┌───────────────┐                    ┌───────────────┐
│ App Server 1  │                    │ App Server N  │
│ (L1 Cache)    │                    │ (L1 Cache)    │
└───────────────┘                    └───────────────┘
        ↓                                      ↓
        └──────────────────┬───────────────────┘
                           ↓
                    ┌─────────────┐
                    │   Redis     │ (L2 Cache)
                    │  Cluster    │
                    └─────────────┘
                           ↓
        ┌──────────────────┴──────────────────┐
        ↓                  ↓                   ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Read Replica │  │ Read Replica │  │ Read Replica │
│   (Slave 1)  │  │   (Slave 2)  │  │   (Slave N)  │
└──────────────┘  └──────────────┘  └──────────────┘
        ↑                  ↑                   ↑
        └──────────────────┴───────────────────┘
                           ↑
                    ┌─────────────┐
                    │  Primary DB │ (Writes only)
                    │   (Master)  │
                    └─────────────┘
                           ↓
                    ┌─────────────┐
                    │    Kafka    │ (Event streaming)
                    └─────────────┘
                           ↓
        ┌──────────────────┴──────────────────┐
        ↓                                      ↓
┌───────────────┐                    ┌───────────────┐
│  MongoDB      │                    │ Elasticsearch │
│ (Read Model)  │                    │   (Search)    │
└───────────────┘                    └───────────────┘
```

---

## Performance Impact

| Strategy | Read Capacity | Latency | Cost/Month |
|----------|--------------|---------|------------|
| Single DB | 1K QPS | 50ms | $100 |
| + Read Replicas (5x) | 50K QPS | 20ms | $600 |
| + Redis Cache | 500K QPS | 2ms | $800 |
| + App Cache | 1M QPS | <1ms | $850 |
| + Elasticsearch | 1M+ QPS | <1ms | $1,200 |
| + CQRS | 2M+ QPS | <1ms | $1,500 |

---

## Key Metrics for Flipkart Scale

### Traffic
- **100M+ products** in catalog
- **500K+ concurrent users** browsing
- **Read:Write ratio** = 95:5
- **1M+ reads/sec** during peak hours

### Performance
- **Cache hit rate** = 95%+
- **Database load** reduced by 20x
- **Response time** < 100ms (p99)
- **Availability** = 99.99%

### Storage
- **Primary DB**: 500GB (normalized data)
- **Read Replicas**: 500GB each × 5 = 2.5TB
- **Redis Cache**: 100GB (hot data)
- **MongoDB**: 1TB (denormalized read models)
- **Elasticsearch**: 500GB (search indexes)

---

## Implementation Checklist

### Phase 1: Basic Scaling (Week 1-2)
- [ ] Set up read replicas (3-5 replicas)
- [ ] Configure connection pooling
- [ ] Implement application-level caching (Caffeine)
- [ ] Add Redis for distributed caching

### Phase 2: Advanced Scaling (Week 3-4)
- [ ] Implement CQRS pattern
- [ ] Set up Elasticsearch for search
- [ ] Create materialized views
- [ ] Add database partitioning

### Phase 3: Optimization (Week 5-6)
- [ ] Implement sharding strategy
- [ ] Fine-tune cache TTLs
- [ ] Optimize query performance
- [ ] Add monitoring and alerting

---

## Monitoring & Observability

### Key Metrics to Track
```java
@Component
public class DatabaseMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void recordQueryLatency(String queryType, long latency) {
        meterRegistry.timer("db.query.latency", "type", queryType)
            .record(latency, TimeUnit.MILLISECONDS);
    }
    
    public void recordCacheHit(String cacheLayer, boolean hit) {
        meterRegistry.counter("cache.requests", 
            "layer", cacheLayer,
            "result", hit ? "hit" : "miss"
        ).increment();
    }
    
    public void recordReplicationLag(String replica, long lagMs) {
        meterRegistry.gauge("db.replication.lag", 
            Tags.of("replica", replica), 
            lagMs
        );
    }
}
```

### Alerts
- Replication lag > 5 seconds
- Cache hit rate < 90%
- Query latency p99 > 100ms
- Connection pool exhaustion
- Replica health check failures

---

## Best Practices

1. **Always use read replicas for read-only queries**
2. **Cache aggressively with appropriate TTLs**
3. **Monitor replication lag continuously**
4. **Use connection pooling with proper sizing**
5. **Implement circuit breakers for database calls**
6. **Use async replication for better write performance**
7. **Partition large tables by date or category**
8. **Use CQRS for complex read patterns**
9. **Implement proper retry logic with exponential backoff**
10. **Monitor and optimize slow queries regularly**

---

## Conclusion

This architecture handles **1M+ reads/sec** while keeping writes on a single primary database with minimal replication lag. The multi-layer caching strategy reduces database load by 95%, and read replicas provide horizontal scalability for read-heavy workloads.
