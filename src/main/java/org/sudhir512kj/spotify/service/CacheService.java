package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.spotify.model.Track;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final ReactiveRedisTemplate<String, Track> redisTemplate;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    
    public Optional<Track> getTrack(String trackId) {
        return redisTemplate.opsForValue()
            .get("track:" + trackId)
            .blockOptional();
    }
    
    public void cacheTrack(Track track) {
        redisTemplate.opsForValue()
            .set("track:" + track.getId(), track, CACHE_TTL)
            .subscribe();
    }
    
    public void invalidateTrack(String trackId) {
        redisTemplate.delete("track:" + trackId).subscribe();
    }
}
