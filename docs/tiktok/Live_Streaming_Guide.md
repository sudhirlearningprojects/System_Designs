# TikTok Live Streaming - Complete Guide

## Overview

This guide covers the complete live streaming implementation for TikTok, including RTMP ingestion, multi-bitrate transcoding, HLS delivery, real-time chat, and analytics.

## Architecture Components

### 1. Media Server Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    Broadcaster (OBS)                         │
│                    RTMP Stream                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Nginx-RTMP Media Server                         │
│  - RTMP Ingestion (Port 1935)                               │
│  - Stream Authentication                                     │
│  - Recording to S3                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              FFmpeg Transcoding Workers                      │
│  - 360p (640x360 @ 500 Kbps)                                │
│  - 480p (854x480 @ 1 Mbps)                                  │
│  - 720p (1280x720 @ 2 Mbps)                                 │
│  - 1080p (1920x1080 @ 4 Mbps)                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              HLS Packaging Service                           │
│  - 3-second segments                                         │
│  - Master playlist (m3u8)                                    │
│  - Variant playlists                                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              CDN Distribution (CloudFront)                   │
│  - 200+ Edge Locations                                       │
│  - <50ms Latency                                             │
│  - Adaptive Bitrate Streaming                                │
└─────────────────────────────────────────────────────────────┘
```

## Setup Instructions

### 1. Install Nginx-RTMP

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install nginx libnginx-mod-rtmp

# macOS
brew install nginx-full --with-rtmp-module

# Verify installation
nginx -V
```

### 2. Configure Nginx-RTMP

Create `/etc/nginx/nginx.conf`:

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;
        max_message 10M;
        
        application live {
            live on;
            record off;
            
            # Authentication callback
            on_publish http://localhost:8095/api/v1/live/auth;
            on_publish_done http://localhost:8095/api/v1/live/end;
            
            # HLS configuration
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3s;
            hls_playlist_length 60s;
            hls_continuous on;
            hls_cleanup on;
            hls_nested on;
            
            # Transcoding
            exec ffmpeg -i rtmp://localhost/$app/$name
                # 1080p
                -c:v libx264 -b:v 4000k -s 1920x1080 -preset veryfast 
                -profile:v high -level 4.1 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 128k -ar 44100 
                -f flv rtmp://localhost/hls/$name_1080p
                
                # 720p
                -c:v libx264 -b:v 2000k -s 1280x720 -preset veryfast 
                -profile:v high -level 4.0 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 128k -ar 44100 
                -f flv rtmp://localhost/hls/$name_720p
                
                # 480p
                -c:v libx264 -b:v 1000k -s 854x480 -preset veryfast 
                -profile:v main -level 3.1 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 96k -ar 44100 
                -f flv rtmp://localhost/hls/$name_480p
                
                # 360p
                -c:v libx264 -b:v 500k -s 640x360 -preset veryfast 
                -profile:v main -level 3.0 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 64k -ar 44100 
                -f flv rtmp://localhost/hls/$name_360p;
        }
        
        application hls {
            live on;
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3s;
            hls_playlist_length 60s;
            hls_nested on;
            
            # Variant playlist
            hls_variant _1080p BANDWIDTH=4000000,RESOLUTION=1920x1080;
            hls_variant _720p BANDWIDTH=2000000,RESOLUTION=1280x720;
            hls_variant _480p BANDWIDTH=1000000,RESOLUTION=854x480;
            hls_variant _360p BANDWIDTH=500000,RESOLUTION=640x360;
        }
    }
}

http {
    server {
        listen 8080;
        
        location /hls {
            types {
                application/vnd.apple.mpegurl m3u8;
                video/mp2t ts;
            }
            root /tmp;
            add_header Cache-Control no-cache;
            add_header Access-Control-Allow-Origin *;
        }
    }
}
```

### 3. Start Nginx

```bash
sudo nginx
# or reload config
sudo nginx -s reload
```

## Broadcasting with OBS

### 1. Download OBS Studio

- Download from: https://obsproject.com/
- Install and launch OBS

### 2. Configure Stream Settings

1. Go to **Settings** → **Stream**
2. Set **Service**: Custom
3. Set **Server**: `rtmp://localhost:1935/live`
4. Set **Stream Key**: Your stream key from API (e.g., `abc123-def456-ghi789`)

### 3. Start Streaming

1. Click **Start Streaming** in OBS
2. Your stream will be available at: `http://localhost:8080/hls/{streamKey}/index.m3u8`

## API Usage

### 1. Create Live Stream

```bash
curl -X POST http://localhost:8095/api/v1/live/create?userId=123 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Live Q&A Session",
    "description": "Ask me anything!"
  }'
```

Response:
```json
{
  "streamId": 789,
  "streamKey": "abc123-def456-ghi789",
  "rtmpUrl": "rtmp://localhost:1935/live/abc123-def456-ghi789",
  "hlsUrl": "http://localhost:8080/hls/abc123-def456-ghi789/index.m3u8",
  "status": "SCHEDULED"
}
```

### 2. Watch Live Stream

Use any HLS player (VLC, Video.js, HLS.js):

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
</head>
<body>
    <video id="video" controls width="640" height="360"></video>
    <script>
        var video = document.getElementById('video');
        var videoSrc = 'http://localhost:8080/hls/abc123-def456-ghi789/index.m3u8';
        
        if (Hls.isSupported()) {
            var hls = new Hls();
            hls.loadSource(videoSrc);
            hls.attachMedia(video);
            hls.on(Hls.Events.MANIFEST_PARSED, function() {
                video.play();
            });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = videoSrc;
            video.addEventListener('loadedmetadata', function() {
                video.play();
            });
        }
    </script>
</body>
</html>
```

### 3. Real-Time Chat

Connect via WebSocket:

```javascript
const ws = new WebSocket('ws://localhost:8095/ws/live?streamId=789&userId=123');

// Send message
ws.send(JSON.stringify({
    type: 'COMMENT',
    content: 'Great stream!'
}));

// Receive messages
ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
};
```

### 4. Get Stream Analytics

```bash
curl http://localhost:8095/api/v1/live/789/analytics
```

Response:
```json
{
  "streamId": 789,
  "totalViewers": 15000,
  "peakViewers": 5000,
  "totalChatMessages": 2500,
  "totalLikes": 10000,
  "streamDuration": "01:30:00"
}
```

## Performance Optimization

### 1. CDN Configuration

Upload HLS segments to S3 and serve via CloudFront:

```bash
# Sync HLS files to S3
aws s3 sync /tmp/hls s3://tiktok-live-streams/ \
  --exclude "*" \
  --include "*.m3u8" \
  --include "*.ts" \
  --cache-control "max-age=3"

# CloudFront distribution
aws cloudfront create-distribution \
  --origin-domain-name tiktok-live-streams.s3.amazonaws.com \
  --default-cache-behavior "MinTTL=0,MaxTTL=3,DefaultTTL=3"
```

### 2. Auto-Scaling Media Servers

```yaml
# AWS Auto Scaling Group
AutoScalingGroup:
  MinSize: 2
  MaxSize: 20
  TargetCPUUtilization: 70%
  ScaleUpPolicy:
    AdjustmentType: ChangeInCapacity
    ScalingAdjustment: 2
    Cooldown: 300
```

### 3. Redis Clustering

```yaml
# Redis Cluster for chat and viewer tracking
redis:
  cluster:
    nodes:
      - redis-1:6379
      - redis-2:6379
      - redis-3:6379
    replicas: 2
```

## Monitoring & Alerts

### Key Metrics

1. **Stream Health**
   - Active streams count
   - Stream start/end rate
   - Failed stream attempts

2. **Viewer Metrics**
   - Concurrent viewers
   - Peak viewers per stream
   - Average watch duration

3. **Chat Metrics**
   - Messages per second
   - Moderation block rate
   - WebSocket connection count

4. **Infrastructure**
   - Media server CPU/memory
   - Transcoding queue depth
   - CDN bandwidth usage

### Alerting Rules

```yaml
alerts:
  - name: HighMediaServerCPU
    condition: cpu_usage > 80%
    duration: 5m
    action: scale_up
    
  - name: StreamFailureRate
    condition: failed_streams / total_streams > 0.05
    duration: 10m
    action: page_oncall
    
  - name: ChatServiceDown
    condition: websocket_connections == 0
    duration: 1m
    action: page_oncall
```

## Troubleshooting

### Stream Not Starting

1. Check Nginx logs: `tail -f /var/log/nginx/error.log`
2. Verify stream key is valid
3. Check firewall allows port 1935
4. Verify FFmpeg is installed: `ffmpeg -version`

### High Latency

1. Reduce HLS segment duration (3s → 2s)
2. Use lower latency protocols (WebRTC)
3. Optimize CDN edge locations
4. Check network bandwidth

### Chat Messages Delayed

1. Check Redis connection
2. Verify WebSocket connections
3. Review rate limiting settings
4. Scale chat service horizontally

## Cost Optimization

### 1. Transcoding Costs

- Use GPU instances for transcoding (10x faster)
- Implement adaptive bitrate based on viewer count
- Cache popular streams at edge

### 2. Storage Costs

- Delete recordings after 30 days
- Use S3 Intelligent-Tiering
- Compress archived streams

### 3. Bandwidth Costs

- Optimize video bitrates
- Use CDN with better pricing
- Implement P2P delivery for popular streams

## Security Best Practices

1. **Stream Key Rotation**: Rotate after each stream
2. **Rate Limiting**: Limit stream creation per user
3. **Content Moderation**: AI + human review
4. **DDoS Protection**: CloudFlare/AWS Shield
5. **Encryption**: TLS for all connections

## References

- [Nginx-RTMP Module](https://github.com/arut/nginx-rtmp-module)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [HLS Specification](https://datatracker.ietf.org/doc/html/rfc8216)
- [WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455)
