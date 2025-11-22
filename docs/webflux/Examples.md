# Real-World Spring WebFlux Examples

## 1. E-Commerce Product Catalog

### Domain Model

```java
@Data
@Table("products")
public class Product {
    @Id
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stock;
    private LocalDateTime createdAt;
}

@Data
@Table("reviews")
public class Review {
    @Id
    private Long id;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
```

### Repository

```java
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {
    
    Flux<Product> findByCategory(String category);
    
    @Query("SELECT * FROM products WHERE price BETWEEN :min AND :max")
    Flux<Product> findByPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max);
    
    @Query("SELECT * FROM products WHERE stock > 0 ORDER BY created_at DESC LIMIT :limit")
    Flux<Product> findAvailableProducts(@Param("limit") int limit);
}

public interface ReviewRepository extends ReactiveCrudRepository<Review, Long> {
    
    Flux<Review> findByProductId(Long productId);
    
    @Query("SELECT AVG(rating) FROM reviews WHERE product_id = :productId")
    Mono<Double> getAverageRating(@Param("productId") Long productId);
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ReactiveRedisTemplate<String, Product> redisTemplate;
    
    public Mono<ProductDetails> getProductDetails(Long id) {
        Mono<Product> product = getProductFromCache(id);
        Mono<List<Review>> reviews = reviewRepository.findByProductId(id).collectList();
        Mono<Double> avgRating = reviewRepository.getAverageRating(id);
        
        return Mono.zip(product, reviews, avgRating)
            .map(tuple -> ProductDetails.builder()
                .product(tuple.getT1())
                .reviews(tuple.getT2())
                .averageRating(tuple.getT3())
                .build());
    }
    
    private Mono<Product> getProductFromCache(Long id) {
        String key = "product:" + id;
        
        return redisTemplate.opsForValue().get(key)
            .switchIfEmpty(
                productRepository.findById(id)
                    .flatMap(product -> 
                        redisTemplate.opsForValue()
                            .set(key, product, Duration.ofMinutes(10))
                            .thenReturn(product)
                    )
            );
    }
    
    public Flux<Product> searchProducts(ProductFilter filter) {
        return productRepository.findAll()
            .filter(p -> filter.getCategory() == null || p.getCategory().equals(filter.getCategory()))
            .filter(p -> filter.getMinPrice() == null || p.getPrice().compareTo(filter.getMinPrice()) >= 0)
            .filter(p -> filter.getMaxPrice() == null || p.getPrice().compareTo(filter.getMaxPrice()) <= 0)
            .filter(p -> filter.getInStock() == null || filter.getInStock() && p.getStock() > 0);
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping("/{id}")
    public Mono<ProductDetails> getProduct(@PathVariable Long id) {
        return productService.getProductDetails(id);
    }
    
    @GetMapping("/search")
    public Flux<Product> searchProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean inStock
    ) {
        ProductFilter filter = ProductFilter.builder()
            .category(category)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .inStock(inStock)
            .build();
        
        return productService.searchProducts(filter);
    }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Product> streamNewProducts() {
        return productService.streamNewProducts()
            .delayElements(Duration.ofSeconds(1));
    }
}
```

## 2. Real-Time Chat Application

### Domain Model

```java
@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String chatId;
    private String senderId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private boolean read;
}

@Data
@Document(collection = "chats")
public class Chat {
    @Id
    private String id;
    private List<String> participants;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
}
```

### Repository

```java
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {
    
    Flux<Message> findByChatIdOrderByTimestampDesc(String chatId);
    
    @Tailable
    @Query("{ 'chatId': ?0 }")
    Flux<Message> streamMessages(String chatId);
}

public interface ChatRepository extends ReactiveMongoRepository<Chat, String> {
    
    Flux<Chat> findByParticipantsContaining(String userId);
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    public Mono<Message> sendMessage(Message message) {
        message.setTimestamp(LocalDateTime.now());
        
        return messageRepository.save(message)
            .flatMap(saved -> 
                chatRepository.findById(message.getChatId())
                    .flatMap(chat -> {
                        chat.setLastMessage(message.getContent());
                        chat.setLastMessageTime(message.getTimestamp());
                        return chatRepository.save(chat);
                    })
                    .thenReturn(saved)
            )
            .doOnSuccess(saved -> 
                messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId(), saved)
            );
    }
    
    public Flux<Message> getMessages(String chatId, int page, int size) {
        return messageRepository.findByChatIdOrderByTimestampDesc(chatId)
            .skip((long) page * size)
            .take(size);
    }
    
    public Flux<Message> streamMessages(String chatId) {
        return messageRepository.streamMessages(chatId);
    }
}
```

### WebSocket Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    @PostMapping("/messages")
    public Mono<Message> sendMessage(@RequestBody Message message) {
        return chatService.sendMessage(message);
    }
    
    @GetMapping("/messages/{chatId}")
    public Flux<Message> getMessages(
        @PathVariable String chatId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return chatService.getMessages(chatId, page, size);
    }
    
    @GetMapping(value = "/messages/{chatId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Message> streamMessages(@PathVariable String chatId) {
        return chatService.streamMessages(chatId);
    }
}
```

## 3. Payment Processing System

### Domain Model

```java
@Data
@Table("payments")
public class Payment {
    @Id
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

public enum PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
}
```

### Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final WebClient paymentGatewayClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Transactional
    public Mono<Payment> processPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        
        return paymentRepository.save(payment)
            .flatMap(this::processWithGateway)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof TransientException))
            .onErrorResume(this::handlePaymentError);
    }
    
    private Mono<Payment> processWithGateway(Payment payment) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        
        return paymentGatewayClient.post()
            .uri("/charge")
            .bodyValue(Map.of(
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "paymentMethod", payment.getPaymentMethod()
            ))
            .retrieve()
            .bodyToMono(GatewayResponse.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .flatMap(response -> {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(response.getTransactionId());
                payment.setCompletedAt(LocalDateTime.now());
                return paymentRepository.save(payment);
            })
            .timeout(Duration.ofSeconds(30));
    }
    
    private Mono<Payment> handlePaymentError(Throwable error) {
        log.error("Payment processing failed", error);
        
        return Mono.defer(() -> {
            Payment failedPayment = new Payment();
            failedPayment.setStatus(PaymentStatus.FAILED);
            return paymentRepository.save(failedPayment);
        });
    }
    
    public Mono<Payment> refundPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .flatMap(payment -> {
                if (payment.getStatus() != PaymentStatus.COMPLETED) {
                    return Mono.error(new IllegalStateException("Cannot refund non-completed payment"));
                }
                
                return paymentGatewayClient.post()
                    .uri("/refund")
                    .bodyValue(Map.of("transactionId", payment.getTransactionId()))
                    .retrieve()
                    .bodyToMono(GatewayResponse.class)
                    .flatMap(response -> {
                        payment.setStatus(PaymentStatus.REFUNDED);
                        return paymentRepository.save(payment);
                    });
            });
    }
}
```

## 4. Notification Service

### Domain Model

```java
@Data
@Table("notifications")
public class Notification {
    @Id
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}

public enum NotificationType {
    EMAIL, SMS, PUSH
}

public enum NotificationStatus {
    PENDING, SENT, FAILED
}
```

### Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushService pushService;
    
    public Flux<Notification> sendBulkNotifications(List<Long> userIds, String title, String message) {
        return Flux.fromIterable(userIds)
            .flatMap(userId -> {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(NotificationType.EMAIL);
                notification.setStatus(NotificationStatus.PENDING);
                notification.setCreatedAt(LocalDateTime.now());
                
                return notificationRepository.save(notification);
            })
            .parallel(4)
            .runOn(Schedulers.parallel())
            .flatMap(this::sendNotification)
            .sequential()
            .onErrorContinue((error, notification) -> 
                log.error("Failed to send notification: {}", notification, error));
    }
    
    private Mono<Notification> sendNotification(Notification notification) {
        return switch (notification.getType()) {
            case EMAIL -> emailService.send(notification);
            case SMS -> smsService.send(notification);
            case PUSH -> pushService.send(notification);
        }
        .flatMap(sent -> {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            return notificationRepository.save(notification);
        })
        .onErrorResume(error -> {
            log.error("Failed to send notification", error);
            notification.setStatus(NotificationStatus.FAILED);
            return notificationRepository.save(notification);
        });
    }
}
```

## 5. API Gateway with Rate Limiting

### Rate Limiter

```java
@Component
@RequiredArgsConstructor
public class RateLimiter {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    public Mono<Boolean> isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "rate_limit:" + key;
        long windowSeconds = window.getSeconds();
        
        return redisTemplate.opsForValue().increment(redisKey)
            .flatMap(count -> {
                if (count == 1) {
                    return redisTemplate.expire(redisKey, window)
                        .thenReturn(true);
                }
                return Mono.just(count <= maxRequests);
            });
    }
}
```

### Filter

```java
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements WebFilter {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        
        return rateLimiter.isAllowed(clientIp, 100, Duration.ofMinutes(1))
            .flatMap(allowed -> {
                if (allowed) {
                    return chain.filter(exchange);
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
            });
    }
}
```

### Gateway Routes

```java
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user_service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .retry(3)
                    .circuitBreaker(c -> c.setName("userService")))
                .uri("http://user-service:8080"))
            
            .route("product_service", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .retry(3)
                    .circuitBreaker(c -> c.setName("productService")))
                .uri("http://product-service:8081"))
            
            .build();
    }
}
```

## Best Practices Demonstrated

1. **Caching**: Redis caching for frequently accessed data
2. **Error Handling**: Comprehensive error handling with fallbacks
3. **Retry Logic**: Exponential backoff for transient failures
4. **Circuit Breaker**: Prevent cascading failures
5. **Parallel Processing**: Efficient bulk operations
6. **Streaming**: Real-time data streaming with SSE
7. **WebSocket**: Bidirectional communication
8. **Rate Limiting**: Protect APIs from abuse
9. **Transactions**: Atomic operations with @Transactional
10. **Monitoring**: Logging and metrics

## Next Steps

- [Getting Started](Getting_Started.md) - Build your first app
- [Performance](Performance.md) - Optimize your application
- [Testing](Testing.md) - Test reactive code
