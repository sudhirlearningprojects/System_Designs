package org.sudhir512kj.instagram.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.elasticsearch.ElasticsearchService;
import org.sudhir512kj.instagram.model.Post;
import org.sudhir512kj.instagram.repository.PostRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
public class HashtagController {
    private final ElasticsearchService elasticsearchService;
    private final PostRepository postRepository;

    @GetMapping("/{hashtag}/posts")
    public ResponseEntity<ApiResponse<List<Post>>> getPostsByHashtag(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "20") int limit) {
        
        List<String> postIds = elasticsearchService.searchPostsByHashtag(hashtag, limit);
        List<Post> posts = postRepository.findAllById(postIds);
        
        return ResponseEntity.ok(ApiResponse.success(posts, "Posts retrieved successfully"));
    }
}
