# Spotify Clone - Music Streaming Platform

## Table of Contents
1. [Overview](#overview)
2. [Requirements](#requirements)
3. [High-Level Design](#high-level-design)
4. [Low-Level Design](#low-level-design)
5. [Database Design](#database-design)
6. [API Design](#api-design)
7. [Scale Calculations](#scale-calculations)

## Overview

Spotify is a global music streaming platform that allows users to:
- Search and stream millions of songs
- Create and manage playlists
- Download songs for offline listening
- Follow artists and discover new music
- Artists can upload and manage their music catalog

### Key Features
- **Music Streaming**: Stream audio in multiple qualities (96kbps to lossless FLAC)
- **Search**: Full-text search across tracks, albums, artists, playlists
- **Playlists**: Create, edit, share playlists
- **Offline Mode**: Download tracks for offline listening
- **User Library**: Save favorite tracks, albums, artists
- **Artist Dashboard**: Upload tracks, manage catalog, view analytics
- **Recommendations**: Personalized music recommendations
- **Social Features**: Follow artists, share playlists

## Requirements

### Functional Requirements

#### For Listeners
1. User registration and authentication
2. Search songs, albums, artists, playlists
3. Stream audio in multiple qualities
4. Create and manage playlists
5. Add songs to favorites/library
6. Download songs for offline listening
7. View listening history
8. Follow artists
9. Share playlists

#### For Artists/Creators
1. Upload songs in multiple formats
2. Create albums and manage catalog
3. Add metadata (lyrics, genre, ISRC)
4. Upload multiple audio qualities
5. View analytics (play counts, listeners)
6. Manage artist profile

### Non-Functional Requirements
1. **Availability**: 99.99% uptime
2. **Latency**: 
   - Search: <200ms
   - Stream start: <500ms
   - API calls: <100ms
3. **Scalability**: Support 500M users, 100M DAU
4. **Storage**: Handle 100M+ tracks
5. **Bandwidth**: Support 10M concurrent streams
6. **Consistency**: Eventual consistency for play counts, strong for payments

## High-Level Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CDN (CloudFront)                         │
│                    Audio Files Distribution                      │
└─────────────────────────────────────────────────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                      Load Balancer (ALB)                         │
└─────────────────────────────────────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼────────┐    ┌─────────▼────────┐    ┌─────────▼────────┐
│  API Gateway   │    │  Streaming       │    │  Search Service  │
│   Service      │    │    Service       │    │  (Elasticsearch) │
└───────┬────────┘    └─────────┬────────┘    └─────────┬────────┘
        │                       │                        │
        │             ┌─────────▼────────┐              │
        │             │  Redis Cache     │              │
        │             │  (Hot Tracks)    │              │
        │             └──────────────────┘              │
        │                                                │
┌───────▼─────────────────────────────────────────────────────────┐
│                    Application Services                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  Track   │  │ Playlist │  │ Library  │  │ Download │       │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
└─────────────────────────────────────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼────────┐    ┌─────────▼────────┐    ┌─────────▼────────┐
│  PostgreSQL    │    │  Cassandra       │    │    S3/Object     │
│  (Metadata)    │    │  (History)       │    │    Storage       │
│                │    │                  │    │  (Audio Files)   │
└────────────────┘    └──────────────────┘    └──────────────────┘
```

### Component Breakdown

#### 1. API Gateway
- Request routing
- Authentication/Authorization
- Rate limiting
- Request validation

#### 2. Streaming Service
- Audio file delivery
- Adaptive bitrate streaming
- Chunk-based delivery
- CDN integration

#### 3. Search Service (Elasticsearch)
- Full-text search
- Fuzzy matching
- Autocomplete
- Filters (genre, year, artist)

#### 4. Track Service
- Track upload and management
- Metadata management
- Audio transcoding
- Play count tracking

#### 5. Playlist Service
- Create/update/delete playlists
- Add/remove tracks
- Collaborative playlists
- Playlist sharing

#### 6. Library Service
- Favorite tracks/albums/artists
- Recently played
- User preferences

#### 7. Download Service
- Offline download management
- Quality selection
- Storage management
- DRM (if needed)

## Low-Level Design

### 1. Audio Storage and Streaming

#### Storage Strategy
```
S3 Bucket Structure:
/tracks/
  /{track_id}/
    /96kbps.mp3      (Low quality - Free tier)
    /160kbps.mp3     (Medium quality - Premium)
    /320kbps.mp3     (High quality - Premium)
    /lossless.flac   (Lossless - HiFi tier)
    /metadata.json
    /cover.jpg
```

#### Streaming Flow
```
1. Client requests track with quality preference
2. API validates user subscription tier
3. Generate signed URL for CDN
4. CDN serves audio chunks
5. Client buffers and plays
6. Record listening event to Cassandra
7. Update play count asynchronously
```

#### Audio Transcoding Pipeline
```
Upload → Validation → Transcode Queue (Kafka)
                            ↓
                    Worker Pool (FFmpeg)
                            ↓
                    Store Multiple Qualities
                            ↓
                    Update Track Status
```

### 2. Search Architecture

#### Elasticsearch Index Structure
```json
{
  "tracks": {
    "mappings": {
      "properties": {
        "id": {"type": "keyword"},
        "title": {"type": "text", "boost": 3},
        "artistName": {"type": "text", "boost": 2},
        "albumName": {"type": "text"},
        "genre": {"type": "keyword"},
        "releaseDate": {"type": "date"},
        "playCount": {"type": "long"},
        "isExplicit": {"type": "boolean"}
      }
    }
  }
}
```

#### Search Query
```java
SearchResponse<Track> response = elasticsearchClient.search(s -> s
    .index("tracks")
    .query(q -> q
        .multiMatch(m -> m
            .query(searchQuery)
            .fields("title^3", "artistName^2", "albumName", "genre")
            .fuzziness("AUTO")
        )
    )
    .sort(sort -> sort
        .field(f -> f.field("playCount").order(SortOrder.Desc))
    )
    .size(20),
    Track.class
);
```

### 3. Playlist Management

#### Data Model
```
Playlist (PostgreSQL)
├── id
├── name
├── userId
├── isPublic
├── trackCount
└── followerCount

PlaylistTrack (Junction Table)
├── playlistId
├── trackId
├── position
├── addedBy
└── addedAt
```

#### Add Track to Playlist
```java
@Transactional
public void addTrackToPlaylist(String playlistId, String trackId, String userId) {
    // 1. Validate playlist exists and user has permission
    Playlist playlist = getPlaylist(playlistId);
    validatePermission(playlist, userId);
    
    // 2. Check if track already exists
    if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, trackId)) {
        throw new DuplicateTrackException();
    }
    
    // 3. Add track at end of playlist
    int position = playlist.getTrackCount();
    PlaylistTrack pt = new PlaylistTrack(playlistId, trackId, position, userId);
    playlistTrackRepository.save(pt);
    
    // 4. Update playlist metadata
    playlist.setTrackCount(playlist.getTrackCount() + 1);
    playlistRepository.save(playlist);
    
    // 5. Invalidate cache
    cacheService.invalidatePlaylist(playlistId);
}
```

### 4. Offline Download System

#### Download Flow
```
1. User requests download
2. Validate subscription tier (Premium only)
3. Check device storage limit (10,000 tracks)
4. Generate encrypted audio file
5. Store download metadata
6. Sync across user devices
```

#### Download Metadata
```java
@Entity
public class DownloadedTrack {
    String userId;
    String trackId;
    String deviceId;
    AudioQuality quality;
    LocalDateTime downloadedAt;
    LocalDateTime lastAccessedAt;
    Long fileSizeBytes;
}
```

### 5. Listening History (Cassandra)

#### Schema Design
```cql
CREATE TABLE listening_history (
    user_id text,
    played_at timestamp,
    track_id text,
    duration_played_ms int,
    device_type text,
    country text,
    is_offline boolean,
    audio_quality text,
    PRIMARY KEY ((user_id), played_at, track_id)
) WITH CLUSTERING ORDER BY (played_at DESC);
```

#### Why Cassandra?
- Time-series data (billions of events)
- Write-heavy workload
- Partition by user_id for fast queries
- TTL for data retention (90 days)
- Horizontal scalability

### 6. Caching Strategy

#### Multi-Layer Cache
```
L1: Application Cache (Caffeine)
    - Hot tracks (top 1000)
    - User sessions
    - TTL: 5 minutes

L2: Redis Cache
    - Track metadata
    - Playlist data
    - User library
    - TTL: 1 hour

L3: CDN Cache (CloudFront)
    - Audio files
    - Cover images
    - TTL: 7 days
```

#### Cache Invalidation
```java
// Write-through cache
public Track updateTrack(String trackId, TrackUpdate update) {
    Track track = trackRepository.save(update);
    
    // Invalidate all cache layers
    caffeineCache.invalidate(trackId);
    redisCache.delete("track:" + trackId);
    cdnCache.purge("/tracks/" + trackId);
    
    return track;
}
```

## Database Design

### PostgreSQL Schema

#### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- FREE, PREMIUM, ARTIST
    subscription_plan VARCHAR(20),
    subscription_expiry_date TIMESTAMP,
    display_name VARCHAR(255),
    profile_image_url TEXT,
    country VARCHAR(2),
    date_of_birth DATE,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    artist_bio TEXT,
    monthly_listeners INTEGER,
    is_verified_artist BOOLEAN DEFAULT false
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

#### Tracks Table
```sql
CREATE TABLE tracks (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    artist_id UUID NOT NULL REFERENCES users(id),
    album_id UUID REFERENCES albums(id),
    isrc VARCHAR(12) UNIQUE,
    duration_ms INTEGER,
    lyrics TEXT,
    track_type VARCHAR(20) NOT NULL, -- SONG, PODCAST, AUDIOBOOK
    is_explicit BOOLEAN DEFAULT false,
    release_date TIMESTAMP,
    play_count BIGINT DEFAULT 0,
    genre VARCHAR(100),
    language VARCHAR(50),
    audio_file_low_quality TEXT,
    audio_file_medium_quality TEXT,
    audio_file_high_quality TEXT,
    audio_file_lossless TEXT,
    cover_image_url TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_tracks_artist ON tracks(artist_id);
CREATE INDEX idx_tracks_album ON tracks(album_id);
CREATE INDEX idx_tracks_isrc ON tracks(isrc);
CREATE INDEX idx_tracks_genre ON tracks(genre);
```

#### Albums Table
```sql
CREATE TABLE albums (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    artist_id UUID NOT NULL REFERENCES users(id),
    album_type VARCHAR(20) NOT NULL, -- SINGLE, EP, ALBUM, COMPILATION
    release_date TIMESTAMP,
    cover_image_url TEXT,
    genre VARCHAR(100),
    total_tracks INTEGER,
    label VARCHAR(255),
    copyright TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_albums_artist ON albums(artist_id);
```

#### Playlists Table
```sql
CREATE TABLE playlists (
    id UUID PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    user_id UUID NOT NULL REFERENCES users(id),
    is_public BOOLEAN DEFAULT true,
    is_collaborative BOOLEAN DEFAULT false,
    cover_image_url TEXT,
    track_count INTEGER DEFAULT 0,
    follower_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_playlists_user ON playlists(user_id);
```

#### Playlist Tracks Table
```sql
CREATE TABLE playlist_tracks (
    id UUID PRIMARY KEY,
    playlist_id UUID NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    track_id UUID NOT NULL REFERENCES tracks(id),
    position INTEGER NOT NULL,
    added_by UUID NOT NULL REFERENCES users(id),
    added_at TIMESTAMP NOT NULL,
    UNIQUE(playlist_id, track_id)
);

CREATE INDEX idx_playlist_tracks_playlist ON playlist_tracks(playlist_id);
CREATE INDEX idx_playlist_tracks_track ON playlist_tracks(track_id);
```

#### User Library Table
```sql
CREATE TABLE user_library (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    entity_type VARCHAR(20) NOT NULL, -- TRACK, ALBUM, PLAYLIST, ARTIST
    entity_id UUID NOT NULL,
    added_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, entity_type, entity_id)
);

CREATE INDEX idx_user_library_user ON user_library(user_id, entity_type);
```

#### Downloaded Tracks Table
```sql
CREATE TABLE downloaded_tracks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    track_id UUID NOT NULL REFERENCES tracks(id),
    device_id VARCHAR(255) NOT NULL,
    audio_quality VARCHAR(20) NOT NULL,
    downloaded_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP,
    file_size_bytes BIGINT,
    UNIQUE(user_id, track_id, device_id)
);

CREATE INDEX idx_downloaded_tracks_user_device ON downloaded_tracks(user_id, device_id);
```

### Cassandra Schema

#### Listening History
```cql
CREATE TABLE listening_history (
    user_id text,
    played_at timestamp,
    track_id text,
    duration_played_ms int,
    device_type text,
    country text,
    is_offline boolean,
    audio_quality text,
    PRIMARY KEY ((user_id), played_at, track_id)
) WITH CLUSTERING ORDER BY (played_at DESC)
  AND default_time_to_live = 7776000; -- 90 days
```

## API Design

See [API_Documentation.md](API_Documentation.md) for complete API reference.

## Scale Calculations

See [Scale_Calculations.md](Scale_Calculations.md) for detailed capacity planning.
