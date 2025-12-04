# Spotify Clone - Music Streaming Platform

## Overview

A production-ready music streaming platform similar to Spotify that supports:
- **Music Streaming**: Stream audio in multiple qualities (96kbps to lossless FLAC)
- **Search**: Full-text search with Elasticsearch
- **Playlists**: Create, manage, and share playlists
- **Offline Downloads**: Download tracks for offline listening (Premium)
- **Artist Dashboard**: Upload and manage music catalog
- **User Library**: Save favorite tracks, albums, artists
- **Listening History**: Track and analyze listening patterns

## Key Features

### For Listeners
✅ Search songs, albums, artists, playlists  
✅ Stream audio in multiple qualities (96/160/320 kbps, FLAC)  
✅ Create and manage playlists  
✅ Add songs to favorites/library  
✅ Download songs for offline listening  
✅ View listening history  
✅ Follow artists  
✅ Share playlists  

### For Artists/Creators
✅ Upload songs with metadata  
✅ Create albums and manage catalog  
✅ Add lyrics and ISRC codes  
✅ Upload multiple audio qualities  
✅ View play counts and analytics  
✅ Manage artist profile  

## Architecture Highlights

### Technology Stack
- **Backend**: Spring Boot 3.2, Java 17
- **Metadata DB**: PostgreSQL (users, tracks, playlists)
- **Time-Series DB**: Cassandra (listening history)
- **Cache**: Redis (hot tracks, sessions)
- **Search**: Elasticsearch (full-text search)
- **Storage**: S3/Local filesystem (audio files)
- **CDN**: CloudFront (audio delivery)

### Design Patterns
- **Microservices**: Separate services for tracks, playlists, streaming
- **Multi-Quality Storage**: Store audio in 4 qualities (96/160/320 kbps, FLAC)
- **Multi-Layer Caching**: Application cache → Redis → CDN
- **Async Processing**: Kafka for audio transcoding pipeline
- **Sharding**: PostgreSQL sharded by user_id
- **Time-Series**: Cassandra for billions of listening events

## Quick Start

### Prerequisites
```bash
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+
- Cassandra 4+
- Elasticsearch 8+
- Kafka 3.0+
```

### 1. Start Infrastructure
```bash
# PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=spotify \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 postgres:14

# Redis
docker run -d --name redis -p 6379:6379 redis:6

# Cassandra
docker run -d --name cassandra -p 9042:9042 cassandra:4

# Elasticsearch
docker run -d --name elasticsearch \
  -e "discovery.type=single-node" \
  -p 9200:9200 elasticsearch:8.11.0

# Kafka
docker run -d --name kafka \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -p 9092:9092 confluentinc/cp-kafka:latest
```

### 2. Configure Application
```bash
# Create storage directory
mkdir -p /tmp/spotify

# Set environment variables
export SPOTIFY_STORAGE_PATH=/tmp/spotify
export DB_PASSWORD=password
```

### 3. Build and Run
```bash
# Build
mvn clean install

# Run with Spotify profile
mvn spring-boot:run -Dspring-boot.run.profiles=spotify

# Or use the convenience script
./run-systems.sh spotify
```

### 4. Verify
```bash
# Check health
curl http://localhost:8098/actuator/health

# Search tracks
curl "http://localhost:8098/api/v1/tracks/search?query=rock"
```

## API Examples

### Upload Track
```bash
curl -X POST http://localhost:8098/api/v1/tracks/upload \
  -F "metadata={
    \"title\": \"Bohemian Rhapsody\",
    \"artistId\": \"artist-123\",
    \"durationMs\": 354000,
    \"trackType\": \"SONG\",
    \"genre\": \"Rock\"
  }" \
  -F "audioFile=@song.mp3"
```

### Stream Track
```bash
curl -X POST http://localhost:8098/api/v1/stream \
  -H "Content-Type: application/json" \
  -d '{
    "trackId": "track-123",
    "userId": "user-456",
    "deviceId": "device-789",
    "audioQuality": "HIGH"
  }' \
  --output song.mp3
```

### Create Playlist
```bash
curl -X POST "http://localhost:8098/api/v1/playlists?userId=user-123&name=My%20Favorites&isPublic=true"
```

### Add to Library
```bash
curl -X POST "http://localhost:8098/api/v1/library?userId=user-123&entityId=track-456&entityType=TRACK"
```

### Download for Offline
```bash
curl -X POST "http://localhost:8098/api/v1/downloads?userId=user-123&trackId=track-456&deviceId=device-789&quality=HIGH"
```

## Scale & Performance

### Capacity
- **Users**: 500M total, 100M DAU
- **Tracks**: 100M+ tracks
- **Storage**: 13.4 PB (audio files)
- **Bandwidth**: 3 Tbps peak, 6.5 EB/month
- **Concurrent Streams**: 12.5M peak

### Performance
- **Search Latency**: <200ms (p99)
- **Stream Start**: <500ms (p99)
- **API Latency**: <100ms (p99)
- **Availability**: 99.99% uptime

### Database Performance
- **PostgreSQL**: 198K reads/sec, 179K writes/sec
- **Cassandra**: 174K writes/sec, 3.5K reads/sec
- **Redis**: 10M ops/sec
- **Elasticsearch**: 1.7K queries/sec

## Documentation

- **[System Design](System_Design.md)**: Complete HLD/LLD with architecture
- **[API Documentation](API_Documentation.md)**: REST API reference
- **[Scale Calculations](Scale_Calculations.md)**: Capacity planning and costs

## Project Structure

```
src/main/java/org/sudhir512kj/spotify/
├── model/
│   ├── User.java                    # User entity (listeners, artists)
│   ├── Track.java                   # Track entity with multi-quality support
│   ├── Album.java                   # Album entity
│   ├── Playlist.java                # Playlist entity
│   ├── PlaylistTrack.java           # Playlist-track junction
│   ├── UserLibrary.java             # User favorites
│   ├── DownloadedTrack.java         # Offline downloads
│   └── ListeningHistory.java        # Cassandra time-series
│
├── repository/
│   ├── UserRepository.java
│   ├── TrackRepository.java
│   ├── AlbumRepository.java
│   ├── PlaylistRepository.java
│   ├── PlaylistTrackRepository.java
│   ├── UserLibraryRepository.java
│   ├── DownloadedTrackRepository.java
│   └── ListeningHistoryRepository.java
│
├── service/
│   ├── TrackService.java            # Track upload and management
│   ├── StorageService.java          # Audio file storage (S3/local)
│   ├── StreamingService.java        # Audio streaming logic
│   ├── SearchService.java           # Elasticsearch integration
│   ├── PlaylistService.java         # Playlist management
│   ├── UserLibraryService.java      # Favorites management
│   ├── DownloadService.java         # Offline downloads
│   └── CacheService.java            # Redis caching
│
├── controller/
│   ├── TrackController.java         # Track APIs
│   ├── StreamingController.java     # Streaming APIs
│   ├── PlaylistController.java      # Playlist APIs
│   ├── LibraryController.java       # Library APIs
│   └── DownloadController.java      # Download APIs
│
├── dto/
│   ├── TrackDTO.java
│   ├── PlaylistDTO.java
│   ├── StreamRequest.java
│   └── UploadTrackRequest.java
│
└── config/
    └── SpotifyConfig.java           # Elasticsearch & Cassandra config
```

## Key Design Decisions

### 1. Multi-Quality Audio Storage
Store each track in 4 qualities to support different subscription tiers:
- **96 kbps**: Free tier
- **160 kbps**: Premium tier
- **320 kbps**: Premium tier
- **FLAC**: HiFi tier

### 2. Database Selection
- **PostgreSQL**: Structured data (users, tracks, playlists) with ACID guarantees
- **Cassandra**: Time-series data (listening history) with high write throughput
- **Redis**: Hot data caching (popular tracks, user sessions)
- **Elasticsearch**: Full-text search with fuzzy matching

### 3. Streaming Architecture
- **CDN**: CloudFront for global audio delivery (80% cache hit rate)
- **Chunked Transfer**: Stream audio in 1MB chunks
- **Adaptive Bitrate**: Adjust quality based on network conditions

### 4. Offline Downloads
- Premium-only feature
- Limit: 10,000 tracks per device
- Encrypted storage
- Sync across devices

### 5. Scalability
- **Horizontal Scaling**: Stateless application servers
- **Database Sharding**: PostgreSQL sharded by user_id
- **Cassandra Cluster**: 45 nodes for 90TB storage
- **CDN**: 200+ edge locations worldwide

## Cost Optimization

### 1. Audio Compression
Use Opus codec (30% better compression):
- **Savings**: 4 PB storage, $84K/month

### 2. CDN Optimization
Increase cache hit ratio to 90%:
- **Savings**: $434K/month bandwidth

### 3. Adaptive Bitrate
Dynamically adjust quality:
- **Savings**: $33.3M/month bandwidth

### 4. Deduplication
Remove duplicate tracks (15%):
- **Savings**: 2 PB storage, $42K/month

## Monitoring & Observability

### Metrics
- Stream start latency (p50, p99)
- Search query latency
- API response times
- Cache hit rates
- Database query performance
- Audio transcoding queue depth

### Alerts
- High error rates (>1%)
- Slow queries (>1s)
- Low cache hit rate (<70%)
- High CPU/memory usage (>80%)
- Database replication lag (>5s)

## Security

### Authentication
- JWT tokens with 1-hour expiry
- Refresh tokens with 30-day expiry
- OAuth2 integration (Google, Facebook, Apple)

### Authorization
- Role-based access control (Listener, Artist, Admin)
- Playlist permissions (Owner, Collaborator, Viewer)
- Premium feature gating

### Data Protection
- Audio files encrypted at rest (S3 SSE)
- HTTPS for all API calls
- DRM for offline downloads
- PII encryption in database

## Future Enhancements

### Phase 2
- [ ] Podcasts and audiobooks
- [ ] Live audio rooms
- [ ] Lyrics synchronization
- [ ] Social features (follow friends, activity feed)
- [ ] Collaborative playlists

### Phase 3
- [ ] AI-powered recommendations
- [ ] Personalized radio stations
- [ ] Concert discovery
- [ ] Artist analytics dashboard
- [ ] Monetization for artists

### Phase 4
- [ ] Video content
- [ ] Live streaming concerts
- [ ] NFT integration
- [ ] Web3 features
- [ ] Spatial audio

## Contributing

1. Fork the repository
2. Create feature branch
3. Follow code style guidelines
4. Add tests for new features
5. Submit pull request

## License

MIT License - see LICENSE file for details
