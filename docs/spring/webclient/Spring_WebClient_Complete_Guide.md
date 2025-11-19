# Spring WebClient - Complete Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [WebClient Basics](#webclient-basics)
3. [Configuration](#configuration)
4. [HTTP Operations](#http-operations)
5. [Error Handling](#error-handling)
6. [Authentication](#authentication)
7. [Testing](#testing)
8. [Interview Questions](#interview-questions)
9. [Best Practices](#best-practices)

## Introduction

Spring WebClient is a reactive, non-blocking HTTP client that replaces RestTemplate. It's built on top of Reactor and provides a fluent API for making HTTP requests in reactive applications.

### Key Features
- **Non-blocking I/O**: Reactive and asynchronous
- **Fluent API**: Builder pattern for requests
- **Streaming Support**: Handle large responses efficiently
- **Backpressure**: Built-in reactive streams support
- **Customizable**: Extensive configuration options

### WebClient vs RestTemplate

| Feature | RestTemplate | WebClient |
|---------|--------------|-----------|
| **I/O Model** | Blocking | Non-blocking |
| **Threading** | Thread per request | Event loop |
| **Reactive Support** | No | Yes |
| **Streaming** | Limited | Full support |
| **Memory Usage** | Higher | Lower |
| **Future** | Deprecated | Active development |

## WebClient Basics

### Creating WebClient

```java
// Simple WebClient
WebClient webClient = WebClient.create();

// With base URL
WebClient webClient = WebClient.create("https://api.example.com");

// Using builder
WebClient webClient = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .defaultHeader(HttpHeaders.USER_AGENT, "MyApp/1.0")
    .build();
```

### Basic Operations

```java
@Service
public class ApiService {
    
    private final WebClient webClient;
    
    public ApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();
    }
    
    // GET request
    public Mono<User> getUser(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // GET with query parameters
    public Flux<Post> getPostsByUser(Long userId) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/posts")
                .queryParam("userId", userId)
                .build())
            .retrieve()
            .bodyToFlux(Post.class);
    }
    
    // POST request
    public Mono<User> createUser(CreateUserRequest request) {
        return webClient.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // PUT request
    public Mono<User> updateUser(Long id, UpdateUserRequest request) {
        return webClient.put()
            .uri("/users/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // DELETE request
    public Mono<Void> deleteUser(Long id) {
        return webClient.delete()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(Void.class);
    }
}
```

## Configuration

### Advanced Configuration

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(10))
                    .addHandlerLast(new WriteTimeoutHandler(10)));
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
                configurer.defaultCodecs().enableLoggingRequestDetails(true);
            })
            .build();
    }
    
    @Bean
    public WebClient secureWebClient() {
        SslContext sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        
        HttpClient httpClient = HttpClient.create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
    
    @Bean
    public WebClient customWebClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
            .maxConnections(100)
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();
        
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true);
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .filter(retryFilter())
            .build();
    }
}
```

### Custom Filters

```java
@Component
public class WebClientFilters {
    
    // Logging filter
    public ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> 
                values.forEach(value -> log.info("{}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }
    
    public ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
    
    // Retry filter
    public ExchangeFilterFunction retryFilter() {
        return (request, next) -> {
            return next.exchange(request)
                .flatMap(clientResponse -> {
                    if (clientResponse.statusCode().is5xxServerError()) {
                        return clientResponse.createException()
                            .flatMap(Mono::error);
                    }
                    return Mono.just(clientResponse);
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> throwable instanceof WebClientResponseException.InternalServerError));
        };
    }
    
    // Authentication filter
    public ExchangeFilterFunction bearerTokenFilter(String token) {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
            return Mono.just(authorizedRequest);
        });
    }
    
    // Rate limiting filter
    public ExchangeFilterFunction rateLimitFilter(RateLimiter rateLimiter) {
        return (request, next) -> {
            return Mono.fromCallable(() -> rateLimiter.acquirePermission())
                .flatMap(permitted -> {
                    if (permitted) {
                        return next.exchange(request);
                    } else {
                        return Mono.error(new RateLimitExceededException("Rate limit exceeded"));
                    }
                });
        };
    }
}
```

## HTTP Operations

### Request Building

```java
@Service
public class AdvancedApiService {
    
    private final WebClient webClient;
    
    // Complex URI building
    public Mono<SearchResult> searchUsers(UserSearchCriteria criteria) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/users/search")
                .queryParam("name", criteria.getName())
                .queryParam("email", criteria.getEmail())
                .queryParam("status", criteria.getStatus())
                .queryParam("page", criteria.getPage())
                .queryParam("size", criteria.getSize())
                .queryParamIfPresent("sort", Optional.ofNullable(criteria.getSort()))
                .build())
            .retrieve()
            .bodyToMono(SearchResult.class);
    }
    
    // Request with headers
    public Mono<ApiResponse> callApiWithHeaders(String data) {
        return webClient.post()
            .uri("/api/data")
            .header("X-API-Version", "v1")
            .header("X-Request-ID", UUID.randomUUID().toString())
            .header("X-Timestamp", Instant.now().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(data)
            .retrieve()
            .bodyToMono(ApiResponse.class);
    }
    
    // Multipart form data
    public Mono<UploadResponse> uploadFile(String filename, byte[] fileData, Map<String, String> metadata) {
        MultiValueMap<String, HttpEntity<?>> parts = new LinkedMultiValueMap<>();
        
        // File part
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        parts.add("file", new HttpEntity<>(fileData, fileHeaders));
        
        // Metadata parts
        metadata.forEach((key, value) -> 
            parts.add(key, new HttpEntity<>(value)));
        
        return webClient.post()
            .uri("/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(parts)
            .retrieve()
            .bodyToMono(UploadResponse.class);
    }
    
    // Streaming response
    public Flux<DataChunk> streamLargeData() {
        return webClient.get()
            .uri("/large-dataset")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .retrieve()
            .bodyToFlux(DataChunk.class)
            .onBackpressureBuffer(1000)
            .doOnNext(chunk -> log.debug("Received chunk: {}", chunk.getId()));
    }
    
    // Custom exchange
    public Mono<CustomResponse> customExchange(String data) {
        return webClient.post()
            .uri("/custom")
            .bodyValue(data)
            .exchangeToMono(response -> {
                if (response.statusCode().equals(HttpStatus.OK)) {
                    return response.bodyToMono(CustomResponse.class);
                } else if (response.statusCode().is4xxClientError()) {
                    return response.bodyToMono(ErrorResponse.class)
                        .flatMap(errorResponse -> Mono.error(
                            new ClientException(errorResponse.getMessage())));
                } else {
                    return response.createException()
                        .flatMap(Mono::error);
                }
            });
    }
}
```

### Response Handling

```java
@Service
public class ResponseHandlingService {
    
    private final WebClient webClient;
    
    // Handle different response types
    public Mono<String> handleTextResponse() {
        return webClient.get()
            .uri("/text-endpoint")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .bodyToMono(String.class);
    }
    
    // Handle binary data
    public Mono<byte[]> downloadFile(String fileId) {
        return webClient.get()
            .uri("/files/{id}/download", fileId)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .bodyToMono(byte[].class);
    }
    
    // Handle response headers
    public Mono<ResponseEntity<User>> getUserWithHeaders(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .toEntity(User.class)
            .doOnNext(responseEntity -> {
                HttpHeaders headers = responseEntity.getHeaders();
                log.info("ETag: {}", headers.getETag());
                log.info("Last-Modified: {}", headers.getLastModified());
            });
    }
    
    // Handle paginated responses
    public Flux<User> getAllUsersPagewise() {
        return Flux.range(0, Integer.MAX_VALUE)
            .concatMap(page -> getUserPage(page))
            .takeWhile(page -> !page.isEmpty())
            .flatMap(Flux::fromIterable);
    }
    
    private Mono<List<User>> getUserPage(int page) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/users")
                .queryParam("page", page)
                .queryParam("size", 20)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<User>>() {});
    }
    
    // Handle conditional requests
    public Mono<User> getUserIfModified(Long id, String etag) {
        return webClient.get()
            .uri("/users/{id}", id)
            .header(HttpHeaders.IF_NONE_MATCH, etag)
            .retrieve()
            .onStatus(HttpStatus.NOT_MODIFIED::equals, 
                response -> Mono.empty())
            .bodyToMono(User.class);
    }
}
```

## Error Handling

### Comprehensive Error Handling

```java
@Service
public class ErrorHandlingService {
    
    private final WebClient webClient;
    
    // Basic error handling
    public Mono<User> getUserWithErrorHandling(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::equals, 
                response -> Mono.error(new UserNotFoundException("User not found: " + id)))
            .onStatus(HttpStatus.UNAUTHORIZED::equals,
                response -> Mono.error(new UnauthorizedException("Authentication required")))
            .onStatus(HttpStatus::is5xxServerError,
                response -> response.bodyToMono(String.class)
                    .flatMap(errorBody -> Mono.error(new ServerException("Server error: " + errorBody))))
            .bodyToMono(User.class)
            .onErrorResume(WebClientRequestException.class, 
                ex -> {
                    log.error("Network error", ex);
                    return getCachedUser(id);
                })
            .timeout(Duration.ofSeconds(10))
            .retry(3);
    }
    
    // Advanced error handling with custom exceptions
    public Mono<ApiResponse> callApiWithAdvancedErrorHandling(String data) {
        return webClient.post()
            .uri("/api/process")
            .bodyValue(data)
            .exchangeToMono(response -> {
                HttpStatus status = response.statusCode();
                
                if (status.is2xxSuccessful()) {
                    return response.bodyToMono(ApiResponse.class);
                } else if (status.equals(HttpStatus.BAD_REQUEST)) {
                    return response.bodyToMono(ValidationErrorResponse.class)
                        .flatMap(errorResponse -> Mono.error(
                            new ValidationException(errorResponse.getErrors())));
                } else if (status.equals(HttpStatus.CONFLICT)) {
                    return response.bodyToMono(ConflictErrorResponse.class)
                        .flatMap(errorResponse -> Mono.error(
                            new ConflictException(errorResponse.getMessage())));
                } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
                    String retryAfter = response.headers().header("Retry-After").stream()
                        .findFirst().orElse("60");
                    return Mono.error(new RateLimitException(
                        "Rate limit exceeded. Retry after: " + retryAfter + " seconds"));
                } else {
                    return response.createException()
                        .flatMap(Mono::error);
                }
            })
            .onErrorResume(this::handleCommonErrors);
    }
    
    private Mono<ApiResponse> handleCommonErrors(Throwable error) {
        if (error instanceof TimeoutException) {
            log.warn("Request timeout, using fallback");
            return getFallbackResponse();
        } else if (error instanceof ConnectException) {
            log.error("Connection failed, service unavailable");
            return Mono.error(new ServiceUnavailableException("Service temporarily unavailable"));
        } else {
            log.error("Unexpected error", error);
            return Mono.error(error);
        }
    }
    
    // Retry with exponential backoff
    public Mono<User> getUserWithRetry(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .filter(throwable -> 
                    throwable instanceof WebClientResponseException.InternalServerError ||
                    throwable instanceof TimeoutException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
                    new ServiceUnavailableException("Service unavailable after retries")));
    }
    
    // Circuit breaker pattern
    public Mono<User> getUserWithCircuitBreaker(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(CallNotPermittedException.class, 
                ex -> {
                    log.warn("Circuit breaker open, using cached data");
                    return getCachedUser(id);
                });
    }
}
```

## Authentication

### Various Authentication Methods

```java
@Service
public class AuthenticatedApiService {
    
    private final WebClient webClient;
    
    // Basic Authentication
    public Mono<User> getUserWithBasicAuth(Long id, String username, String password) {
        return webClient.get()
            .uri("/users/{id}", id)
            .headers(headers -> headers.setBasicAuth(username, password))
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // Bearer Token Authentication
    public Mono<User> getUserWithBearerToken(Long id, String token) {
        return webClient.get()
            .uri("/users/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // OAuth2 Authentication
    @Autowired
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    
    public Mono<User> getUserWithOAuth2(Long id, String clientRegistrationId) {
        return authorizedClientManager
            .authorize(OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                .principal("user")
                .build())
            .flatMap(authorizedClient -> {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                
                return webClient.get()
                    .uri("/users/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(User.class);
            });
    }
    
    // JWT Token with automatic refresh
    @Component
    public static class JwtTokenManager {
        
        private volatile String currentToken;
        private volatile Instant tokenExpiry;
        
        public Mono<String> getValidToken() {
            if (currentToken != null && tokenExpiry.isAfter(Instant.now().plusSeconds(60))) {
                return Mono.just(currentToken);
            }
            
            return refreshToken();
        }
        
        private Mono<String> refreshToken() {
            return webClient.post()
                .uri("/auth/refresh")
                .bodyValue(new RefreshTokenRequest(refreshToken))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnNext(response -> {
                    this.currentToken = response.getAccessToken();
                    this.tokenExpiry = Instant.now().plusSeconds(response.getExpiresIn());
                })
                .map(TokenResponse::getAccessToken);
        }
    }
    
    public Mono<User> getUserWithJwt(Long id) {
        return jwtTokenManager.getValidToken()
            .flatMap(token -> 
                webClient.get()
                    .uri("/users/{id}", id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(User.class)
            );
    }
    
    // API Key Authentication
    public Mono<User> getUserWithApiKey(Long id, String apiKey) {
        return webClient.get()
            .uri("/users/{id}", id)
            .header("X-API-Key", apiKey)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // Custom Authentication Header
    public Mono<User> getUserWithCustomAuth(Long id, String signature, String timestamp) {
        return webClient.get()
            .uri("/users/{id}", id)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .header("X-Auth-Version", "1.0")
            .retrieve()
            .bodyToMono(User.class);
    }
}
```

## Testing

### WebClient Testing

```java
@ExtendWith(MockitoExtension.class)
class ApiServiceTest {
    
    private MockWebServer mockWebServer;
    private ApiService apiService;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        
        apiService = new ApiService(webClient);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void shouldGetUser() {
        // Given
        User expectedUser = new User(1L, "John Doe", "john@example.com");
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedUser))
            .addHeader("Content-Type", "application/json"));
        
        // When
        Mono<User> result = apiService.getUser(1L);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedUser)
            .verifyComplete();
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/users/1", recordedRequest.getPath());
    }
    
    @Test
    void shouldHandleNotFoundError() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("User not found"));
        
        // When
        Mono<User> result = apiService.getUser(999L);
        
        // Then
        StepVerifier.create(result)
            .expectError(UserNotFoundException.class)
            .verify();
    }
    
    @Test
    void shouldRetryOnServerError() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(new User(1L, "John", "john@example.com")))
            .addHeader("Content-Type", "application/json"));
        
        // When
        Mono<User> result = apiService.getUserWithRetry(1L);
        
        // Then
        StepVerifier.create(result)
            .expectNextMatches(user -> user.getId().equals(1L))
            .verifyComplete();
        
        assertEquals(3, mockWebServer.getRequestCount());
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "external.api.base-url=http://localhost:${wiremock.server.port}"
})
class ApiServiceIntegrationTest {
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8089))
        .build();
    
    @Autowired
    private ApiService apiService;
    
    @Test
    void shouldIntegrateWithExternalApi() {
        // Given
        User expectedUser = new User(1L, "John Doe", "john@example.com");
        
        wireMock.stubFor(get(urlEqualTo("/users/1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(expectedUser))));
        
        // When
        Mono<User> result = apiService.getUser(1L);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedUser)
            .verifyComplete();
        
        wireMock.verify(getRequestedFor(urlEqualTo("/users/1")));
    }
    
    @Test
    void shouldHandleNetworkTimeout() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/users/1"))
            .willReturn(aResponse()
                .withFixedDelay(15000))); // Longer than timeout
        
        // When
        Mono<User> result = apiService.getUserWithTimeout(1L);
        
        // Then
        StepVerifier.create(result)
            .expectError(TimeoutException.class)
            .verify();
    }
}
```

## Interview Questions

### Basic Level

**Q1: What is Spring WebClient and how does it differ from RestTemplate?**

**Answer:** Spring WebClient is a reactive HTTP client:
- **Non-blocking**: Uses reactive streams and event loops
- **Fluent API**: Builder pattern for request construction
- **Reactive Support**: Returns Mono/Flux instead of blocking calls
- **Better Performance**: Lower memory usage and higher concurrency
- **Future-proof**: RestTemplate is deprecated in favor of WebClient

```java
// RestTemplate (blocking)
User user = restTemplate.getForObject("/users/1", User.class);

// WebClient (reactive)
Mono<User> user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class);
```

**Q2: How do you configure WebClient with custom settings?**

**Answer:**
```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .responseTimeout(Duration.ofSeconds(10));
    
    return WebClient.builder()
        .baseUrl("https://api.example.com")
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
        .build();
}
```

### Intermediate Level

**Q3: How do you handle different types of errors in WebClient?**

**Answer:**
```java
public Mono<User> getUserWithErrorHandling(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .onStatus(HttpStatus.NOT_FOUND::equals, 
            response -> Mono.error(new UserNotFoundException("User not found")))
        .onStatus(HttpStatus::is4xxClientError,
            response -> response.bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new ClientException(errorBody))))
        .onStatus(HttpStatus::is5xxServerError,
            response -> Mono.error(new ServerException("Server error")))
        .bodyToMono(User.class)
        .onErrorResume(WebClientRequestException.class, 
            ex -> getCachedUser(id))
        .timeout(Duration.ofSeconds(5))
        .retry(3);
}
```

**Q4: Explain the difference between retrieve() and exchange() methods.**

**Answer:**
- **retrieve()**: Simplified API for common use cases
- **exchange()**: Full control over response handling (deprecated)
- **exchangeToMono()/exchangeToFlux()**: Recommended replacement for exchange()

```java
// retrieve() - simple
Mono<User> user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class);

// exchangeToMono() - full control
Mono<User> user = webClient.get()
    .uri("/users/1")
    .exchangeToMono(response -> {
        if (response.statusCode().equals(HttpStatus.OK)) {
            return response.bodyToMono(User.class);
        } else {
            return response.createException().flatMap(Mono::error);
        }
    });
```

### Advanced Level

**Q5: Design a resilient HTTP client with circuit breaker, retry, and caching.**

**Answer:**
```java
@Service
public class ResilientApiClient {
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Cache<String, User> cache;
    
    public Mono<User> getUser(Long id) {
        String cacheKey = "user:" + id;
        
        return Mono.fromCallable(() -> cache.getIfPresent(cacheKey))
            .switchIfEmpty(
                webClient.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .bodyToMono(User.class)
                    .transform(CircuitBreakerOperator.of(circuitBreaker))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                    .doOnNext(user -> cache.put(cacheKey, user))
                    .onErrorResume(CallNotPermittedException.class, 
                        ex -> Mono.fromCallable(() -> cache.getIfPresent(cacheKey))
                            .switchIfEmpty(Mono.error(new ServiceUnavailableException("Service unavailable"))))
            );
    }
    
    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof WebClientResponseException.InternalServerError ||
               throwable instanceof TimeoutException ||
               throwable instanceof ConnectException;
    }
}

// Configuration
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker userServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("user-service");
    }
    
    @Bean
    public Cache<String, User> userCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }
}
```

**Q6: Implement OAuth2 authentication with automatic token refresh.**

**Answer:**
```java
@Component
public class OAuth2WebClientService {
    
    private final WebClient webClient;
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    
    public Mono<User> getUser(Long id) {
        return getAuthorizedWebClient()
            .flatMap(client -> 
                client.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .bodyToMono(User.class)
            );
    }
    
    private Mono<WebClient> getAuthorizedWebClient() {
        return authorizedClientManager
            .authorize(OAuth2AuthorizeRequest.withClientRegistrationId("my-client")
                .principal("user")
                .build())
            .map(authorizedClient -> {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                
                return webClient.mutate()
                    .filter(ExchangeFilterFunctions.bearerToken(accessToken))
                    .build();
            });
    }
}

@Configuration
@EnableWebFluxSecurity
public class OAuth2Config {
    
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .clientCredentials()
                .build();
        
        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
            new DefaultReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        
        return authorizedClientManager;
    }
}
```

## Best Practices

### Performance Optimization

```java
@Configuration
public class WebClientOptimizationConfig {
    
    @Bean
    public WebClient optimizedWebClient() {
        // Connection pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("optimized")
            .maxConnections(100)
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();
        
        // HTTP client configuration
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(30))
            .compress(true);
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024); // 2MB
                configurer.defaultCodecs().enableLoggingRequestDetails(false); // Disable in production
            })
            .build();
    }
}
```

### Error Handling Best Practices

```java
@Service
public class BestPracticesApiService {
    
    // Comprehensive error handling
    public Mono<User> getUser(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::equals, 
                response -> Mono.error(new UserNotFoundException("User not found: " + id)))
            .onStatus(HttpStatus.UNAUTHORIZED::equals,
                response -> Mono.error(new UnauthorizedException("Authentication required")))
            .onStatus(HttpStatus.FORBIDDEN::equals,
                response -> Mono.error(new ForbiddenException("Access denied")))
            .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                response -> {
                    String retryAfter = response.headers().header("Retry-After").stream()
                        .findFirst().orElse("60");
                    return Mono.error(new RateLimitException("Rate limit exceeded. Retry after: " + retryAfter));
                })
            .onStatus(HttpStatus::is5xxServerError,
                response -> response.bodyToMono(String.class)
                    .defaultIfEmpty("Unknown server error")
                    .flatMap(errorBody -> Mono.error(new ServerException(errorBody))))
            .bodyToMono(User.class)
            .timeout(Duration.ofSeconds(10))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(this::isRetryableException))
            .onErrorResume(TimeoutException.class, 
                ex -> getCachedUser(id))
            .doOnError(ex -> log.error("Error getting user {}", id, ex));
    }
    
    // Resource management
    public Flux<DataChunk> streamLargeData() {
        return webClient.get()
            .uri("/large-dataset")
            .accept(MediaType.APPLICATION_STREAM_JSON)
            .retrieve()
            .bodyToFlux(DataChunk.class)
            .onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_LATEST)
            .doOnNext(chunk -> processChunk(chunk))
            .doOnError(ex -> log.error("Error streaming data", ex))
            .doFinally(signalType -> log.info("Stream completed with signal: {}", signalType));
    }
    
    // Proper request/response logging
    private ExchangeFilterFunction logRequestResponse() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> 
                    log.debug("Request header: {}={}", name, values));
            }
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response status: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        }));
    }
}
```

This comprehensive Spring WebClient guide covers configuration, HTTP operations, error handling, authentication, testing, and production-ready best practices for building resilient reactive HTTP clients.