package org.sudhir512kj.webclient.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.webclient.model.Post;
import org.sudhir512kj.webclient.service.PostService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public Mono<Post> getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    @GetMapping
    public Flux<Post> getAllPosts() {
        return postService.getAllPosts();
    }

    @PostMapping
    public Mono<Post> createPost(@RequestBody Post post) {
        return postService.createPost(post);
    }

    @GetMapping("/stats")
    public Mono<String> getStats() {
        return postService.getPostsStats();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamTitles() {
        return postService.streamPostTitles();
    }
}