# Video Platform Storage & Search at Scale (YouTube-like)

## Problem Statement
Your video platform (like YouTube) stores billions of videos with metadata (id, title, tags). How do you scale storage + search?

## Scale Requirements
- **5B videos** in catalog
- **500 hours** of video uploaded per minute
- **1B searches** per day
- **Average video size**: 500MB
- **Total storage**: 2.5 Exabytes (2,500 PB)
- **Metadata**: 50KB per video = 250TB
- **Search latency**: <100ms

---

## Key Challenges

1. **Massive storage** - 2.5 EB of video files
2. **Fast search** - Full-text search across billions of records
3. **Hot/cold data** - 80% of views on 20% of videos
4. **Global distribution** - Low latency worldwide
5. **Metadata updates** - Views, likes, comments change frequently
6. **Complex queries** - Multi-field search with filters

---

## Solution Architecture

### Two-Tier Storage Strategy

```
Storage Layer:
- Video Files → Object Storage (S3/GCS) + CDN
- Metadata → PostgreSQL (sharded) + Elasticsearch

Search Layer:
- Elasticsearch → Full-text search
- Redis → Hot metadata cache
- PostgreSQL → Source of truth
```

---

## 1. Video File Storage: Object Storage + CDN

### Architecture

```
Upload Flow:
User → API → S3 (Original) → Transcoding Pipeline → S3 (Multiple Resolutions) → CloudFront CDN

Playback Flow:
User → CDN Edge (Cache Hit 90%) → S3 (Cache Miss 10%)
```

### Implementation

```java
@Service
public class VideoStorageService {
    
    @Autowired
    private AmazonS3 s3Client;
    
    @Autowired
    private CloudFrontClient cloudFrontClient;
    
    private static final String BUCKET_NAME = "youtube-videos";
    private static final String CDN_DOMAIN = "d1234.cloudfront.net";
    
    // Upload video to S3
    public String uploadVideo(MultipartFile file, String videoId) {
        String key = generateS3Key(videoId, "original");
        
        // Upload to S3 with metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        metadata.addUserMetadata("video-id", videoId);
        
        PutObjectRequest request = new PutObjectRequest(
            BUCKET_NAME,
            key,
            file.getInputStream(),
            metadata
        );
        
        // Use S3 Transfer Acceleration for faster uploads
        request.withBucketKeyEnabled(true);
        
        s3Client.putObject(request);
        
        // Trigger transcoding pipeline
        triggerTranscoding(videoId, key);
        
        return key;
    }
    
    // Generate CDN URL for playback
    public String getVideoUrl(String videoId, String resolution) {
        String key = generateS3Key(videoId, resolution);
        
        // Return CloudFront URL (not direct S3)
        return String.format("https://%s/%s", CDN_DOMAIN, key);
    }
    
    private String generateS3Key(String videoId, String resolution) {
        // Partition by first 2 chars of video ID to avoid hot partitions
        String prefix = videoId.substring(0, 2);
        return String.format("%s/%s/%s.mp4", prefix, videoId, resolution);
    }
    
    // Lifecycle policy for cold storage
    public void configureLifecyclePolicy() {
        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
            .withId("archive-old-videos")
            .withStatus(BucketLifecycleConfiguration.ENABLED)
            .withTransitions(Arrays.asList(
                // Move to Infrequent Access after 30 days
                new BucketLifecycleConfiguration.Transition()
                    .withDays(30)
                    .withStorageClass(StorageClass.StandardInfrequentAccess),
                // Move to Glacier after 90 days
                new BucketLifecycleConfiguration.Transition()
                    .withDays(90)
                    .withStorageClass(StorageClass.Glacier)
            ));
        
        s3Client.setBucketLifecycleConfiguration(BUCKET_NAME, 
            new BucketLifecycleConfiguration().withRules(rule));
    }
}
```

### S3 Storage Classes

| Storage Class | Use Case | Cost | Retrieval Time |
|--------------|----------|------|----------------|
| S3 Standard | Hot videos (< 30 days) | $0.023/GB | Instant |
| S3 IA | Warm videos (30-90 days) | $0.0125/GB | Instant |
| S3 Glacier | Cold videos (> 90 days) | $0.004/GB | Minutes-Hours |
| S3 Deep Archive | Archive (> 1 year) | $0.00099/GB | 12 hours |

---

## 2. Metadata Storage: Sharded PostgreSQL

### Schema Design

```sql
-- Videos table (sharded by video_id)
CREATE TABLE videos (
    video_id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    duration_seconds INT NOT NULL,
    upload_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL, -- PROCESSING, PUBLISHED, DELETED
    
    -- Denormalized stats (updated frequently)
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    
    -- Storage metadata
    file_size_bytes BIGINT NOT NULL,
    resolutions VARCHAR[] NOT NULL, -- ['360p', '720p', '1080p']
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_videos_user_id ON videos(user_id, upload_date DESC);
CREATE INDEX idx_videos_upload_date ON videos(upload_date DESC);
CREATE INDEX idx_videos_status ON videos(status) WHERE status = 'PUBLISHED';

-- Tags table (many-to-many)
CREATE TABLE video_tags (
    video_id VARCHAR(20) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (video_id, tag)
);

CREATE INDEX idx_video_tags_tag ON video_tags(tag, video_id);

-- Categories table
CREATE TABLE video_categories (
    video_id VARCHAR(20) PRIMARY KEY,
    category_id INT NOT NULL,
    category_name VARCHAR(100) NOT NULL
);

CREATE INDEX idx_video_categories_category ON video_categories(category_id, video_id);
```

### Sharding Strategy

```java
@Service
public class VideoShardingService {
    
    private static final int SHARD_COUNT = 100;
    
    @Autowired
    private Map<Integer, DataSource> shardDataSources;
    
    // Hash-based sharding
    private int getShardId(String videoId) {
        return Math.abs(videoId.hashCode()) % SHARD_COUNT;
    }
    
    public Video getVideo(String videoId) {
        int shardId = getShardId(videoId);
        JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(shardId));
        
        return jdbc.queryForObject(
            "SELECT * FROM videos WHERE video_id = ?",
            videoRowMapper,
            videoId
        );
    }
    
    public void saveVideo(Video video) {
        int shardId = getShardId(video.getVideoId());
        JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(shardId));
        
        jdbc.update(
            "INSERT INTO videos (video_id, user_id, title, description, duration_seconds, upload_date, status, file_size_bytes, resolutions) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            video.getVideoId(), video.getUserId(), video.getTitle(), video.getDescription(),
            video.getDurationSeconds(), video.getUploadDate(), video.getStatus(),
            video.getFileSizeBytes(), video.getResolutions()
        );
    }
    
    // Scatter-gather for queries across all shards
    public List<Video> getRecentVideos(int limit) {
        List<CompletableFuture<List<Video>>> futures = new ArrayList<>();
        
        // Query all shards in parallel
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            int finalShardId = shardId;
            CompletableFuture<List<Video>> future = CompletableFuture.supplyAsync(() -> {
                JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(finalShardId));
                return jdbc.query(
                    "SELECT * FROM videos WHERE status = 'PUBLISHED' ORDER BY upload_date DESC LIMIT ?",
                    videoRowMapper,
                    limit
                );
            });
            futures.add(future);
        }
        
        // Merge results from all shards
        List<Video> allVideos = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .sorted(Comparator.comparing(Video::getUploadDate).reversed())
            .limit(limit)
            .collect(Collectors.toList());
        
        return allVideos;
    }
}
```

---

## 3. Search Engine: Elasticsearch

### Index Mapping

```json
{
  "mappings": {
    "properties": {
      "video_id": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": { "type": "keyword" },
          "autocomplete": {
            "type": "text",
            "analyzer": "autocomplete"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "standard"
      },
      "tags": {
        "type": "keyword"
      },
      "category": {
        "type": "keyword"
      },
      "user_id": { "type": "keyword" },
      "upload_date": { "type": "date" },
      "duration_seconds": { "type": "integer" },
      "view_count": { "type": "long" },
      "like_count": { "type": "long" },
      "relevance_score": { "type": "float" }
    }
  },
  "settings": {
    "number_of_shards": 50,
    "number_of_replicas": 2,
    "analysis": {
      "analyzer": {
        "autocomplete": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "autocomplete_filter"]
        }
      },
      "filter": {
        "autocomplete_filter": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 20
        }
      }
    }
  }
}
```

### Implementation

```java
@Service
public class VideoSearchService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // Index video for search
    public void indexVideo(Video video) {
        VideoSearchDocument doc = VideoSearchDocument.builder()
            .videoId(video.getVideoId())
            .title(video.getTitle())
            .description(video.getDescription())
            .tags(video.getTags())
            .category(video.getCategory())
            .userId(video.getUserId())
            .uploadDate(video.getUploadDate())
            .durationSeconds(video.getDurationSeconds())
            .viewCount(video.getViewCount())
            .likeCount(video.getLikeCount())
            .relevanceScore(calculateRelevanceScore(video))
            .build();
        
        elasticsearchOperations.save(doc);
    }
    
    // Search videos with filters
    public SearchResult searchVideos(String query, SearchFilters filters, int page, int size) {
        // Check cache first
        String cacheKey = "search:" + query + ":" + filters.hashCode() + ":" + page;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);
        }
        
        // Build Elasticsearch query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Multi-match query on title and description
        if (query != null && !query.isEmpty()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(query, "title^3", "description", "tags^2")
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .fuzziness(Fuzziness.AUTO));
        }
        
        // Filters
        if (filters.getCategory() != null) {
            boolQuery.filter(QueryBuilders.termQuery("category", filters.getCategory()));
        }
        
        if (filters.getMinDuration() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("duration_seconds").gte(filters.getMinDuration()));
        }
        
        if (filters.getMaxDuration() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("duration_seconds").lte(filters.getMaxDuration()));
        }
        
        if (filters.getUploadDateFrom() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("upload_date").gte(filters.getUploadDateFrom()));
        }
        
        // Boost by relevance score (views, likes, recency)
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery)
            .add(ScoreFunctionBuilders.fieldValueFactorFunction("relevance_score").modifier(FieldValueFactorFunction.Modifier.LOG1P))
            .add(ScoreFunctionBuilders.gaussDecayFunction("upload_date", "now", "30d"))
            .scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)
            .boostMode(CombineFunction.MULTIPLY);
        
        // Execute search
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(functionScoreQuery)
            .withPageable(PageRequest.of(page, size))
            .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
            .build();
        
        SearchHits<VideoSearchDocument> hits = elasticsearchOperations.search(searchQuery, VideoSearchDocument.class);
        
        List<VideoSearchResult> results = hits.stream()
            .map(hit -> new VideoSearchResult(hit.getContent(), hit.getScore()))
            .collect(Collectors.toList());
        
        SearchResult searchResult = new SearchResult(results, hits.getTotalHits());
        
        // Cache for 5 minutes
        redisTemplate.opsForValue().set(cacheKey, serialize(searchResult), 5, TimeUnit.MINUTES);
        
        return searchResult;
    }
    
    // Autocomplete suggestions
    public List<String> autocomplete(String prefix) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchQuery("title.autocomplete", prefix))
            .withPageable(PageRequest.of(0, 10))
            .build();
        
        SearchHits<VideoSearchDocument> hits = elasticsearchOperations.search(searchQuery, VideoSearchDocument.class);
        
        return hits.stream()
            .map(hit -> hit.getContent().getTitle())
            .distinct()
            .collect(Collectors.toList());
    }
    
    // Calculate relevance score (for ranking)
    private float calculateRelevanceScore(Video video) {
        long ageInDays = ChronoUnit.DAYS.between(video.getUploadDate(), Instant.now());
        float recencyScore = (float) Math.exp(-ageInDays / 30.0); // Decay over 30 days
        
        float engagementScore = (float) Math.log1p(video.getViewCount() + video.getLikeCount() * 10);
        
        return recencyScore * engagementScore;
    }
}
```

---

## 4. Caching Strategy: Multi-Layer Cache

### Architecture

```
Request Flow:
User → CDN (Static) → Redis (Hot Metadata) → Elasticsearch (Search) → PostgreSQL (Source of Truth)
       95%              4%                     0.8%                    0.2%
```

### Implementation

```java
@Service
public class VideoCacheService {
    
    @Autowired
    private RedisTemplate<String, Video> redisTemplate;
    
    @Autowired
    private VideoShardingService shardingService;
    
    private static final String VIDEO_KEY = "video:";
    private static final String TRENDING_KEY = "trending:videos";
    
    // Get video with caching
    public Video getVideo(String videoId) {
        // L1: Redis cache
        String cacheKey = VIDEO_KEY + videoId;
        Video cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // L2: Database
        Video video = shardingService.getVideo(videoId);
        
        // Cache for 1 hour (hot videos) or 24 hours (cold videos)
        int ttl = video.getViewCount() > 1_000_000 ? 1 : 24;
        redisTemplate.opsForValue().set(cacheKey, video, ttl, TimeUnit.HOURS);
        
        return video;
    }
    
    // Cache trending videos
    public List<Video> getTrendingVideos() {
        // Check cache
        List<Video> cached = redisTemplate.opsForList().range(TRENDING_KEY, 0, 49);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // Query Elasticsearch for trending
        List<Video> trending = searchService.getTrendingVideos(50);
        
        // Cache for 10 minutes
        redisTemplate.delete(TRENDING_KEY);
        redisTemplate.opsForList().rightPushAll(TRENDING_KEY, trending);
        redisTemplate.expire(TRENDING_KEY, 10, TimeUnit.MINUTES);
        
        return trending;
    }
    
    // Increment view count (async)
    @Async
    public void incrementViewCount(String videoId) {
        // Increment in Redis (fast)
        String countKey = "view_count:" + videoId;
        redisTemplate.opsForValue().increment(countKey);
        
        // Batch update to database every 1 minute
        // (handled by scheduled job)
    }
}
```

---

## 5. Data Synchronization: PostgreSQL → Elasticsearch

### Change Data Capture (CDC) with Debezium

```java
@Service
public class VideoSyncService {
    
    @Autowired
    private VideoSearchService searchService;
    
    @KafkaListener(topics = "postgres.public.videos", groupId = "video-sync")
    public void handleVideoChange(ChangeEvent event) {
        switch (event.getOperation()) {
            case "INSERT":
            case "UPDATE":
                Video video = event.getAfter();
                searchService.indexVideo(video);
                break;
            case "DELETE":
                String videoId = event.getBefore().getVideoId();
                searchService.deleteVideo(videoId);
                break;
        }
    }
}
```

### Scheduled Batch Sync (Fallback)

```java
@Service
public class VideoIndexingService {
    
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void reindexAllVideos() {
        log.info("Starting full reindex...");
        
        int batchSize = 10000;
        int offset = 0;
        
        while (true) {
            List<Video> batch = videoRepository.findAll(PageRequest.of(offset / batchSize, batchSize));
            if (batch.isEmpty()) break;
            
            // Bulk index to Elasticsearch
            BulkRequest bulkRequest = new BulkRequest();
            batch.forEach(video -> {
                bulkRequest.add(new IndexRequest("videos")
                    .id(video.getVideoId())
                    .source(toJson(video), XContentType.JSON));
            });
            
            elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            
            offset += batchSize;
        }
        
        log.info("Reindex completed. Total videos: {}", offset);
    }
}
```

---

## 6. Complete Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User Requests                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  CloudFront CDN (Global)                     │
│              (Video Playback - 95% Cache Hit)                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                               │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│  Video Service   │                  │  Search Service  │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│  Redis Cache     │                  │  Redis Cache     │
│  (Hot Metadata)  │                  │  (Search Cache)  │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│  PostgreSQL      │                  │  Elasticsearch   │
│  (100 Shards)    │──CDC (Debezium)─→│  (50 Shards)     │
│  - videos        │                  │  - Full-text     │
│  - tags          │                  │  - Autocomplete  │
│  - categories    │                  │  - Filters       │
└──────────────────┘                  └──────────────────┘
        ↓                                       
┌──────────────────┐                  
│  Amazon S3       │                  
│  (Video Files)   │                  
│  - Standard      │                  
│  - IA            │                  
│  - Glacier       │                  
└──────────────────┘                  
```

---

## 7. Storage Cost Optimization

### Tiered Storage Strategy

```java
@Service
public class StorageTierService {
    
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM daily
    public void optimizeStorageTiers() {
        // Move videos to appropriate storage class based on views
        List<Video> videos = videoRepository.findVideosForTierOptimization();
        
        videos.forEach(video -> {
            long daysSinceUpload = ChronoUnit.DAYS.between(video.getUploadDate(), Instant.now());
            long viewsLast30Days = analyticsService.getViewsLast30Days(video.getVideoId());
            
            String targetStorageClass;
            if (viewsLast30Days > 10000) {
                targetStorageClass = "STANDARD"; // Hot
            } else if (daysSinceUpload < 90) {
                targetStorageClass = "STANDARD_IA"; // Warm
            } else if (daysSinceUpload < 365) {
                targetStorageClass = "GLACIER"; // Cold
            } else {
                targetStorageClass = "DEEP_ARCHIVE"; // Archive
            }
            
            s3Client.copyObject(new CopyObjectRequest()
                .withSourceBucketName(BUCKET_NAME)
                .withSourceKey(video.getS3Key())
                .withDestinationBucketName(BUCKET_NAME)
                .withDestinationKey(video.getS3Key())
                .withStorageClass(targetStorageClass));
        });
    }
}
```

### Cost Breakdown (5B videos, 2.5 EB)

| Storage Tier | Videos | Size | Cost/GB/Month | Total Cost/Month |
|-------------|--------|------|---------------|------------------|
| S3 Standard (Hot) | 500M (10%) | 250 PB | $0.023 | $5,750,000 |
| S3 IA (Warm) | 1B (20%) | 500 PB | $0.0125 | $6,250,000 |
| S3 Glacier (Cold) | 2B (40%) | 1000 PB | $0.004 | $4,000,000 |
| S3 Deep Archive | 1.5B (30%) | 750 PB | $0.00099 | $742,500 |

**Total Storage Cost: ~$16.7M/month**

---

## 8. Performance Optimization

### Elasticsearch Tuning

```yaml
# elasticsearch.yml
cluster.name: youtube-search
node.name: search-node-1

# Memory settings
bootstrap.memory_lock: true
indices.memory.index_buffer_size: 30%

# Query cache
indices.queries.cache.size: 20%

# Shard settings
index.number_of_shards: 50
index.number_of_replicas: 2

# Refresh interval (trade-off: freshness vs performance)
index.refresh_interval: 30s

# Merge policy
index.merge.policy.max_merged_segment: 5gb
```

### Query Optimization

```java
// Use filters instead of queries when possible (cacheable)
BoolQueryBuilder query = QueryBuilders.boolQuery()
    .must(QueryBuilders.matchQuery("title", searchTerm))
    .filter(QueryBuilders.termQuery("category", category)) // Cached
    .filter(QueryBuilders.rangeQuery("upload_date").gte("now-7d")); // Cached
```

---

## 9. Monitoring & Metrics

```java
@Component
public class VideoMetrics {
    
    @Autowired
    private MeterRegistry registry;
    
    public void recordSearch(String query, long latency, long resultCount) {
        registry.timer("video.search.latency").record(latency, TimeUnit.MILLISECONDS);
        registry.counter("video.search.requests").increment();
        registry.gauge("video.search.results", resultCount);
    }
    
    public void recordCacheHit(String cacheLayer, boolean hit) {
        registry.counter("video.cache", 
            "layer", cacheLayer,
            "result", hit ? "hit" : "miss"
        ).increment();
    }
    
    public void recordStorageTier(String tier, long sizeBytes) {
        registry.gauge("video.storage.size", 
            Tags.of("tier", tier), 
            sizeBytes
        );
    }
}
```

### Critical Alerts
- Search latency p99 > 200ms
- Elasticsearch cluster health != green
- Cache hit rate < 90%
- S3 request rate > 5000/sec (throttling risk)
- CDN cache hit rate < 90%

---

## 10. Scalability Benchmarks

| Metric | Current | Target | Strategy |
|--------|---------|--------|----------|
| Videos | 5B | 10B | Add PostgreSQL shards |
| Searches/day | 1B | 5B | Scale Elasticsearch cluster |
| Storage | 2.5 EB | 10 EB | S3 auto-scales |
| Search latency | 50ms | <100ms | More ES replicas |
| Upload rate | 500 hrs/min | 1000 hrs/min | Parallel transcoding |

---

## Key Takeaways

1. **Object Storage (S3)** for video files with tiered storage
2. **Sharded PostgreSQL** for metadata (100 shards)
3. **Elasticsearch** for full-text search (50 shards)
4. **Redis cache** for hot metadata (95% hit rate)
5. **CloudFront CDN** for video delivery (95% hit rate)
6. **CDC (Debezium)** for PostgreSQL → Elasticsearch sync
7. **Lifecycle policies** to move cold videos to Glacier
8. **Multi-field search** with relevance scoring
9. **Autocomplete** with edge n-grams
10. **Cost optimization** through tiered storage ($16.7M/month)

---

## Interview Answer Summary

**Question**: How to scale storage + search for billions of videos?

**Answer**:

**Storage**:
1. **S3 for video files** - 2.5 EB with tiered storage (Standard → IA → Glacier)
2. **Sharded PostgreSQL** for metadata - 100 shards, 250TB
3. **CloudFront CDN** - 95% cache hit rate for playback
4. **Lifecycle policies** - Auto-move cold videos to cheaper storage

**Search**:
1. **Elasticsearch** - 50 shards for full-text search
2. **Multi-field queries** - Title^3, description, tags^2
3. **Relevance scoring** - Views, likes, recency decay
4. **Redis cache** - 5-minute cache for search results
5. **Autocomplete** - Edge n-grams for suggestions
6. **CDC sync** - Debezium for PostgreSQL → Elasticsearch

**Performance**:
- Search latency: <100ms (p99)
- Storage cost: $16.7M/month (optimized)
- Cache hit rate: 95% (CDN + Redis)
- Scalability: 5B → 10B videos ready

This architecture handles YouTube-scale with optimized costs and sub-100ms search latency.
