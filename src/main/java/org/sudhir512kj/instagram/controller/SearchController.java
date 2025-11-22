package org.sudhir512kj.instagram.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.dto.SearchResponse;
import org.sudhir512kj.instagram.service.SearchService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<SearchResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        List<SearchResponse> results = searchService.searchUsers(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/suggested")
    public ResponseEntity<ApiResponse<List<SearchResponse>>> getSuggestedUsers(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<SearchResponse> results = searchService.getSuggestedUsers(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
