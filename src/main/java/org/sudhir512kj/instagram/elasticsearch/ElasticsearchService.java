package org.sudhir512kj.instagram.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.instagram.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {
    private final ElasticsearchClient elasticsearchClient;

    public void indexUser(User user) {
        try {
            Map<String, Object> document = Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "profilePictureUrl", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "",
                "isVerified", user.getIsVerified(),
                "followerCount", user.getFollowerCount()
            );

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index("users")
                .id(user.getUserId().toString())
                .document(document)
            );

            elasticsearchClient.index(request);
            log.info("Indexed user: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to index user: {}", user.getUserId(), e);
        }
    }

    public List<org.sudhir512kj.instagram.dto.SearchResponse> searchUsers(String query, int limit) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("users")
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .fields("username^2", "fullName")
                        .fuzziness("AUTO")
                    )
                )
                .size(limit)
                .sort(sort -> sort
                    .field(f -> f.field("followerCount").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
                )
            );

            co.elastic.clients.elasticsearch.core.SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            
            List<org.sudhir512kj.instagram.dto.SearchResponse> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    results.add(org.sudhir512kj.instagram.dto.SearchResponse.builder()
                        .userId(((Number) source.get("userId")).longValue())
                        .username((String) source.get("username"))
                        .fullName((String) source.get("fullName"))
                        .profilePictureUrl((String) source.get("profilePictureUrl"))
                        .isVerified((Boolean) source.get("isVerified"))
                        .followerCount(((Number) source.get("followerCount")).intValue())
                        .build());
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
            return new ArrayList<>();
        }
    }

    public void indexPost(String postId, Long userId, String content, List<String> hashtags) {
        try {
            Map<String, Object> document = Map.of(
                "postId", postId,
                "userId", userId,
                "content", content != null ? content : "",
                "hashtags", hashtags != null ? hashtags : List.of()
            );

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index("posts")
                .id(postId)
                .document(document)
            );

            elasticsearchClient.index(request);
            log.info("Indexed post: {}", postId);
        } catch (Exception e) {
            log.error("Failed to index post: {}", postId, e);
        }
    }

    public List<String> searchPostsByHashtag(String hashtag, int limit) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("posts")
                .query(q -> q
                    .term(t -> t
                        .field("hashtags")
                        .value(hashtag)
                    )
                )
                .size(limit)
            );

            co.elastic.clients.elasticsearch.core.SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            
            List<String> postIds = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    postIds.add((String) source.get("postId"));
                }
            }
            
            return postIds;
        } catch (Exception e) {
            log.error("Hashtag search failed: {}", hashtag, e);
            return new ArrayList<>();
        }
    }
}
