package org.sudhir512kj.webclient.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.sudhir512kj.webclient.model.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;

@Service
public class PostService {

    private final WebClient webClient;

    public PostService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Basic GET request
    public Mono<Post> getPost(Long id) {
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .timeout(Duration.ofSeconds(5));
    }

    // GET all posts with filtering and transformation
    public Flux<Post> getAllPosts() {
        return webClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class)
                .filter(post -> post.title().length() > 10)
                .take(5);
    }

    // POST request with retry and error handling
    public Mono<Post> createPost(Post post) {
        return webClient.post()
                .uri("/posts")
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .onErrorReturn(new Post(0L, 0L, "Error", "Failed to create"));
    }

    // Parallel requests and combining results
    public Mono<String> getPostsStats() {
        Mono<Long> totalPosts = webClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class)
                .count();

        Mono<Post> firstPost = getPost(1L);

        return Mono.zip(totalPosts, firstPost)
                .map(tuple -> String.format("Total: %d, First: %s", 
                    tuple.getT1(), tuple.getT2().title()));
    }

    // Stream processing with backpressure
    public Flux<String> streamPostTitles() {
        return webClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class)
                .map(Post::title)
                .map(String::toUpperCase)
                .delayElements(Duration.ofMillis(100))
                .onBackpressureBuffer(10);
    }
}