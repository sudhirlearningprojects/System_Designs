package org.sudhir512kj.spotify.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.spotify.model.Track;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final ElasticsearchClient elasticsearchClient;
    
    public List<Track> searchTracks(String query) {
        try {
            SearchResponse<Track> response = elasticsearchClient.search(s -> s
                .index("tracks")
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .fields("title^3", "artistName^2", "albumName", "genre")
                    )
                ),
                Track.class
            );
            
            return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Search failed", e);
            return List.of();
        }
    }
    
    public void indexTrack(Track track) {
        try {
            elasticsearchClient.index(i -> i
                .index("tracks")
                .id(track.getId())
                .document(track)
            );
        } catch (Exception e) {
            log.error("Failed to index track", e);
        }
    }
}
