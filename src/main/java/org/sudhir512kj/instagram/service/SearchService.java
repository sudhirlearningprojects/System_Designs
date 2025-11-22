package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.instagram.dto.SearchResponse;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final UserRepository userRepository;
    private final org.sudhir512kj.instagram.elasticsearch.ElasticsearchService elasticsearchService;

    @Transactional(readOnly = true)
    @Cacheable(value = "userSearch", key = "#query + '_' + #limit")
    public List<SearchResponse> searchUsers(String query, int limit) {
        try {
            return elasticsearchService.searchUsers(query, limit);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed, falling back to PostgreSQL", e);
            List<User> users = userRepository.searchByUsernameOrFullName(query, PageRequest.of(0, limit));
            return users.stream().map(this::buildSearchResponse).collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public List<SearchResponse> getSuggestedUsers(Long userId, int limit) {
        List<User> users = userRepository.findTopByOrderByFollowerCountDesc(PageRequest.of(0, limit));
        
        return users.stream()
            .filter(u -> !u.getUserId().equals(userId))
            .map(this::buildSearchResponse)
            .collect(Collectors.toList());
    }

    private SearchResponse buildSearchResponse(User user) {
        return SearchResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .profilePictureUrl(user.getProfilePictureUrl())
            .isVerified(user.getIsVerified())
            .followerCount(user.getFollowerCount())
            .build();
    }
}
