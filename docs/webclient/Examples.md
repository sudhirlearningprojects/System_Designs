# Real-World WebClient Examples

## 1. Microservices Communication

### User Service Client

```java
@Service
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final WebClient userServiceWebClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<User> getUser(Long id) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("userService");
        
        return userServiceWebClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .timeout(Duration.ofSeconds(3))
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
            .transformDeferred(CircuitBreakerOperator.of(cb));
    }
    
    public Flux<User> searchUsers(String query) {
        return userServiceWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/users/search")
                .queryParam("q", query)
                .build())
            .retrieve()
            .bodyToFlux(User.class);
    }
    
    public Mono<User> createUser(User user) {
        return userServiceWebClient.post()
            .uri("/users")
            .bodyValue(user)
            .retrieve()
            .bodyToMono(User.class);
    }
}
```

### Order Service Orchestration

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    
    public Mono<OrderConfirmation> createOrder(OrderRequest request) {
        // Parallel validation
        Mono<User> user = userServiceClient.getUser(request.getUserId());
        Mono<Product> product = productServiceClient.getProduct(request.getProductId());
        Mono<Boolean> available = inventoryServiceClient.checkAvailability(
            request.getProductId(), request.getQuantity());
        
        return Mono.zip(user, product, available)
            .flatMap(tuple -> {
                if (!tuple.getT3()) {
                    return Mono.error(new OutOfStockException());
                }
                
                // Sequential processing
                return saveOrder(request)
                    .flatMap(order -> 
                        inventoryServiceClient.reserveStock(
                            request.getProductId(), request.getQuantity())
                            .thenReturn(order))
                    .flatMap(order -> 
                        paymentServiceClient.processPayment(
                            new PaymentRequest(order.getId(), order.getTotal()))
                            .map(payment -> new OrderConfirmation(order, payment)));
            });
    }
}
```

## 2. Payment Gateway Integration

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {
    
    private final WebClient stripeWebClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("stripe");
        
        return stripeWebClient.post()
            .uri("/v1/charges")
            .header("Authorization", "Bearer " + getApiKey())
            .bodyValue(Map.of(
                "amount", request.getAmount(),
                "currency", request.getCurrency(),
                "source", request.getToken()
            ))
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response ->
                response.bodyToMono(StripeError.class)
                    .flatMap(error -> Mono.error(new PaymentException(error.getMessage()))))
            .bodyToMono(StripeCharge.class)
            .map(this::toPaymentResponse)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof WebClientException))
            .transformDeferred(CircuitBreakerOperator.of(cb))
            .doOnSuccess(response -> log.info("Payment processed: {}", response.getId()))
            .doOnError(error -> log.error("Payment failed", error));
    }
    
    public Mono<RefundResponse> refundPayment(String chargeId, BigDecimal amount) {
        return stripeWebClient.post()
            .uri("/v1/refunds")
            .header("Authorization", "Bearer " + getApiKey())
            .bodyValue(Map.of(
                "charge", chargeId,
                "amount", amount
            ))
            .retrieve()
            .bodyToMono(StripeRefund.class)
            .map(this::toRefundResponse);
    }
}
```

## 3. External API Integration with Caching

```java
@Service
@RequiredArgsConstructor
public class WeatherApiService {
    
    private final WebClient weatherApiClient;
    private final ReactiveRedisTemplate<String, WeatherData> redisTemplate;
    
    public Mono<WeatherData> getWeather(String city) {
        String cacheKey = "weather:" + city;
        
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                fetchWeatherFromApi(city)
                    .flatMap(weather -> 
                        redisTemplate.opsForValue()
                            .set(cacheKey, weather, Duration.ofMinutes(30))
                            .thenReturn(weather))
            );
    }
    
    private Mono<WeatherData> fetchWeatherFromApi(String city) {
        return weatherApiClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/weather")
                .queryParam("q", city)
                .queryParam("appid", getApiKey())
                .build())
            .retrieve()
            .bodyToMono(WeatherApiResponse.class)
            .map(this::toWeatherData)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)));
    }
}
```

## 4. Batch Processing

```java
@Service
@RequiredArgsConstructor
public class BatchUserService {
    
    private final WebClient webClient;
    
    public Flux<User> getUsersBatch(List<Long> userIds) {
        return Flux.fromIterable(userIds)
            .buffer(50) // Process in batches of 50
            .flatMap(batch -> 
                webClient.post()
                    .uri("/users/batch")
                    .bodyValue(batch)
                    .retrieve()
                    .bodyToFlux(User.class))
            .onErrorContinue((error, userId) -> 
                log.error("Failed to fetch user: {}", userId, error));
    }
    
    public Mono<BatchResult> updateUsersBatch(List<User> users) {
        return Flux.fromIterable(users)
            .parallel(4)
            .runOn(Schedulers.parallel())
            .flatMap(user -> 
                webClient.put()
                    .uri("/users/{id}", user.getId())
                    .bodyValue(user)
                    .retrieve()
                    .bodyToMono(User.class)
                    .onErrorResume(error -> {
                        log.error("Failed to update user: {}", user.getId(), error);
                        return Mono.empty();
                    }))
            .sequential()
            .collectList()
            .map(updated -> new BatchResult(updated.size(), users.size() - updated.size()));
    }
}
```

## 5. File Upload/Download

```java
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final WebClient webClient;
    
    public Mono<String> uploadFile(FilePart filePart) {
        return filePart.content()
            .reduce(DataBuffer::write)
            .flatMap(dataBuffer -> {
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("file", dataBuffer.asInputStream())
                    .filename(filePart.filename());
                
                return webClient.post()
                    .uri("/files/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(FileUploadResponse.class)
                    .map(FileUploadResponse::getFileId);
            });
    }
    
    public Mono<Void> downloadFile(String fileId, Path destination) {
        return webClient.get()
            .uri("/files/{id}/download", fileId)
            .retrieve()
            .bodyToFlux(DataBuffer.class)
            .flatMap(dataBuffer -> {
                try {
                    AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                        destination,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE
                    );
                    return DataBufferUtils.write(Flux.just(dataBuffer), channel)
                        .then(Mono.fromRunnable(() -> {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                log.error("Error closing file", e);
                            }
                        }));
                } catch (IOException e) {
                    return Mono.error(e);
                }
            })
            .then();
    }
}
```

## 6. Server-Sent Events (SSE)

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final WebClient webClient;
    
    public Flux<Notification> streamNotifications(String userId) {
        return webClient.get()
            .uri("/notifications/stream/{userId}", userId)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(Notification.class)
            .doOnNext(notification -> 
                log.info("Received notification: {}", notification))
            .doOnError(error -> 
                log.error("Stream error", error))
            .retry();
    }
    
    public Flux<StockPrice> streamStockPrices(String symbol) {
        return webClient.get()
            .uri("/stocks/{symbol}/stream", symbol)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(StockPrice.class)
            .timeout(Duration.ofMinutes(5))
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5)));
    }
}
```

## 7. OAuth2 Integration

```java
@Service
@RequiredArgsConstructor
public class OAuth2ApiService {
    
    private final WebClient webClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    
    public Mono<UserProfile> getUserProfile() {
        return webClient.get()
            .uri("/user/profile")
            .attributes(oauth2AuthorizedClient("google"))
            .retrieve()
            .bodyToMono(UserProfile.class);
    }
    
    private Consumer<Map<String, Object>> oauth2AuthorizedClient(String clientRegistrationId) {
        return attrs -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal("user")
                .build();
            
            OAuth2AuthorizedClient authorizedClient = 
                authorizedClientManager.authorize(authorizeRequest);
            
            if (authorizedClient != null) {
                attrs.put(OAuth2AuthorizedClient.class.getName(), authorizedClient);
            }
        };
    }
}
```

## 8. GraphQL Client

```java
@Service
@RequiredArgsConstructor
public class GraphQLClient {
    
    private final WebClient webClient;
    
    public Mono<User> getUserWithPosts(Long userId) {
        String query = """
            query GetUser($id: ID!) {
                user(id: $id) {
                    id
                    name
                    email
                    posts {
                        id
                        title
                        content
                    }
                }
            }
            """;
        
        Map<String, Object> request = Map.of(
            "query", query,
            "variables", Map.of("id", userId)
        );
        
        return webClient.post()
            .uri("/graphql")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(GraphQLResponse.class)
            .map(response -> response.getData().getUser());
    }
}
```

## 9. Webhook Handler

```java
@Service
@RequiredArgsConstructor
public class WebhookService {
    
    private final WebClient webClient;
    
    public Mono<Void> sendWebhook(String url, WebhookPayload payload) {
        return webClient.post()
            .uri(url)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(10))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
            .onErrorResume(error -> {
                log.error("Webhook failed: {}", url, error);
                return saveFailedWebhook(url, payload);
            });
    }
    
    public Flux<Void> sendWebhooksBatch(List<WebhookSubscription> subscriptions, 
                                        WebhookPayload payload) {
        return Flux.fromIterable(subscriptions)
            .parallel(10)
            .runOn(Schedulers.parallel())
            .flatMap(subscription -> 
                sendWebhook(subscription.getUrl(), payload))
            .sequential();
    }
}
```

## 10. API Aggregation

```java
@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final AnalyticsServiceClient analyticsServiceClient;
    
    public Mono<Dashboard> getDashboard(Long userId) {
        Mono<User> user = userServiceClient.getUser(userId);
        
        Mono<List<Order>> recentOrders = orderServiceClient
            .getRecentOrders(userId, 10)
            .collectList();
        
        Mono<Analytics> analytics = analyticsServiceClient
            .getUserAnalytics(userId);
        
        Mono<List<Recommendation>> recommendations = 
            analyticsServiceClient.getRecommendations(userId)
                .collectList();
        
        return Mono.zip(user, recentOrders, analytics, recommendations)
            .map(tuple -> Dashboard.builder()
                .user(tuple.getT1())
                .recentOrders(tuple.getT2())
                .analytics(tuple.getT3())
                .recommendations(tuple.getT4())
                .build())
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(error -> {
                log.error("Dashboard error", error);
                return Mono.just(getDefaultDashboard(userId));
            });
    }
}
```

## Best Practices Demonstrated

1. **Circuit Breaker**: Prevent cascading failures
2. **Retry Logic**: Handle transient failures
3. **Timeouts**: Prevent hanging requests
4. **Caching**: Reduce load on external services
5. **Batch Processing**: Efficient bulk operations
6. **Error Handling**: Graceful degradation
7. **Parallel Execution**: Improve performance
8. **Streaming**: Handle real-time data
9. **OAuth2**: Secure API access
10. **Monitoring**: Log and track requests

## Next Steps

- [Testing](Testing.md) - Testing WebClient integrations
- [Performance](Performance.md) - Optimization strategies
- [Configuration](Configuration.md) - Advanced configuration
