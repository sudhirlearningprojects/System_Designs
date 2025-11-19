# Spring WebFlux - Complete Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Reactive Programming](#reactive-programming)
3. [WebFlux Architecture](#webflux-architecture)
4. [Reactive Controllers](#reactive-controllers)
5. [Functional Endpoints](#functional-endpoints)
6. [Reactive Data Access](#reactive-data-access)
7. [Error Handling](#error-handling)
8. [Testing](#testing)
9. [Interview Questions](#interview-questions)
10. [Best Practices](#best-practices)

## Introduction

Spring WebFlux is a reactive web framework built on Project Reactor, designed for non-blocking, asynchronous programming with better resource utilization and scalability.

### Key Features
- **Non-blocking I/O**: Better resource utilization
- **Backpressure**: Handle overwhelming data streams
- **Functional Programming**: Functional routing and handlers
- **Reactive Streams**: Publisher/Subscriber pattern
- **Netty Support**: High-performance server

### WebFlux vs Spring MVC

| Feature | Spring MVC | Spring WebFlux |
|---------|------------|----------------|
| **Programming Model** | Imperative | Reactive |
| **Threading** | One thread per request | Event loop |
| **Blocking** | Blocking I/O | Non-blocking I/O |
| **Scalability** | Limited by threads | High concurrency |
| **Learning Curve** | Easier | Steeper |

## Reactive Programming

### Core Concepts

```java
// Mono - 0 or 1 element
Mono<String> mono = Mono.just("Hello")
    .map(String::toUpperCase)
    .filter(s -> s.length() > 3)
    .defaultIfEmpty("Default");

// Flux - 0 to N elements
Flux<Integer> flux = Flux.range(1, 10)
    .filter(i -> i % 2 == 0)
    .map(i -> i * 2)
    .take(3);

// Cold vs Hot Publishers
Flux<String> coldFlux = Flux.just("A", "B", "C"); // Cold - starts on subscription
Flux<String> hotFlux = Flux.just("A", "B", "C").share(); // Hot - shared among subscribers
```

### Reactive Operators

```java
@Service
public class ReactiveDataService {
    
    // Transformation operators
    public Flux<UserDto> transformUsers(Flux<User> users) {
        return users
            .map(this::convertToDto)           // Transform each element
            .flatMap(this::enrichWithProfile)  // Async transformation
            .filter(dto -> dto.isActive())     // Filter elements
            .distinct()                        // Remove duplicates
            .sort(Comparator.comparing(UserDto::getName));
    }
    
    // Combination operators
    public Mono<OrderSummary> combineOrderData(Long orderId) {
        Mono<Order> order = orderRepository.findById(orderId);
        Mono<User> user = order.flatMap(o -> userRepository.findById(o.getUserId()));
        Mono<List<OrderItem>> items = orderItemRepository.findByOrderId(orderId).collectList();
        
        return Mono.zip(order, user, items)
            .map(tuple -> new OrderSummary(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }
    
    // Error handling operators
    public Flux<Product> getProductsWithFallback() {
        return productRepository.findAll()
            .onErrorResume(DatabaseException.class, 
                ex -> getCachedProducts())
            .onErrorReturn(Collections.emptyList())
            .retry(3)
            .timeout(Duration.ofSeconds(5));
    }
    
    // Backpressure handling
    public Flux<String> handleBackpressure() {
        return Flux.interval(Duration.ofMillis(1))
            .onBackpressureBuffer(1000)        // Buffer up to 1000 elements
            .onBackpressureDrop()              // Drop elements when overwhelmed
            .onBackpressureLatest()            // Keep only latest element
            .map(i -> "Item " + i);
    }
}
```

## WebFlux Architecture

### Server Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   HTTP Request                         │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                 Netty Server                           │
│              (Event Loop Model)                        │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              WebFlux Framework                         │
│  ┌─────────────────┐    ┌─────────────────────────────┐ │
│  │   Annotated     │    │     Functional             │ │
│  │  Controllers    │    │     Endpoints              │ │
│  └─────────────────┘    └─────────────────────────────┘ │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                Reactive Stack                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │   Reactor   │ │   Netty     │ │   Reactive      │   │
│  │    Core     │ │   Client    │ │   Repositories  │   │
│  └─────────────┘ └─────────────┘ └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Configuration

```java
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
        configurer.defaultCodecs().enableLoggingRequestDetails(true);
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
    
    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        factory.setPort(8080);
        
        factory.addServerCustomizers(server -> 
            server.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                  .option(ChannelOption.SO_KEEPALIVE, true)
                  .childOption(ChannelOption.TCP_NODELAY, true)
        );
        
        return factory;
    }
}
```

## Reactive Controllers

### Basic Controllers

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public Flux<UserDto> getAllUsers() {
        return userService.findAll()
            .map(this::convertToDto);
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserDto>> getUser(@PathVariable String id) {
        return userService.findById(id)
            .map(this::convertToDto)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public Mono<ResponseEntity<UserDto>> createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request)
            .map(this::convertToDto)
            .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserDto>> updateUser(@PathVariable String id, 
                                                   @RequestBody @Valid UpdateUserRequest request) {
        return userService.updateUser(id, request)
            .map(this::convertToDto)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable String id) {
        return userService.deleteUser(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
```

### Streaming Endpoints

```java
@RestController
@RequestMapping("/api/stream")
public class StreamingController {
    
    @GetMapping(value = "/users", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UserDto> streamUsers() {
        return userService.findAll()
            .map(this::convertToDto)
            .delayElements(Duration.ofSeconds(1)); // Simulate real-time updates
    }
    
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationDto>> streamNotifications(
            @RequestParam String userId) {
        
        return notificationService.getNotificationStream(userId)
            .map(notification -> ServerSentEvent.<NotificationDto>builder()
                .id(notification.getId())
                .event("notification")
                .data(convertToDto(notification))
                .build());
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<UploadResponse>> uploadFile(
            @RequestPart("file") Mono<FilePart> filePartMono) {
        
        return filePartMono
            .flatMap(filePart -> {
                String filename = filePart.filename();
                return filePart.transferTo(Paths.get("uploads/" + filename))
                    .then(Mono.just(new UploadResponse(filename, "Success")));
            })
            .map(ResponseEntity::ok);
    }
    
    @GetMapping(value = "/download/{filename}")
    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable String filename) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get("uploads/" + filename);
            Resource resource = new FileSystemResource(path);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
        });
    }
}
```

### WebSocket Support

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig {
    
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/websocket/chat", new ChatWebSocketHandler());
        map.put("/websocket/notifications", new NotificationWebSocketHandler());
        
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }
}

@Component
public class ChatWebSocketHandler implements WebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        return session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(message -> {
                // Broadcast message to all connected sessions
                return Flux.fromIterable(sessions.values())
                    .flatMap(s -> s.send(Mono.just(s.textMessage(message))))
                    .then();
            })
            .doFinally(signalType -> sessions.remove(sessionId));
    }
}
```

## Functional Endpoints

### Router Functions

```java
@Configuration
public class RouterConfig {
    
    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions
            .route(GET("/api/users"), userHandler::getAllUsers)
            .andRoute(GET("/api/users/{id}"), userHandler::getUser)
            .andRoute(POST("/api/users"), userHandler::createUser)
            .andRoute(PUT("/api/users/{id}"), userHandler::updateUser)
            .andRoute(DELETE("/api/users/{id}"), userHandler::deleteUser);
    }
    
    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler productHandler) {
        return RouterFunctions
            .nest(path("/api/products"),
                RouterFunctions
                    .route(GET(""), productHandler::getAllProducts)
                    .andRoute(GET("/{id}"), productHandler::getProduct)
                    .andRoute(POST(""), productHandler::createProduct)
                    .andNest(path("/{id}"),
                        RouterFunctions
                            .route(PUT(""), productHandler::updateProduct)
                            .andRoute(DELETE(""), productHandler::deleteProduct)
                            .andRoute(GET("/reviews"), productHandler::getProductReviews)
                    )
            );
    }
    
    @Bean
    public RouterFunction<ServerResponse> conditionalRoutes(ConditionalHandler handler) {
        return RouterFunctions
            .route(RequestPredicates.GET("/api/data")
                .and(accept(MediaType.APPLICATION_JSON))
                .and(headers(h -> h.containsKey("X-API-Version"))), 
                handler::handleVersionedRequest)
            .andRoute(RequestPredicates.POST("/api/data")
                .and(contentType(MediaType.APPLICATION_JSON))
                .and(queryParam("validate", "true")), 
                handler::handleValidatedRequest);
    }
}
```

### Handler Functions

```java
@Component
public class UserHandler {
    
    @Autowired
    private UserService userService;
    
    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
        
        Flux<UserDto> users = userService.findAll(page, size)
            .map(this::convertToDto);
        
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(users, UserDto.class);
    }
    
    public Mono<ServerResponse> getUser(ServerRequest request) {
        String id = request.pathVariable("id");
        
        return userService.findById(id)
            .map(this::convertToDto)
            .flatMap(user -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(CreateUserRequest.class)
            .flatMap(userService::createUser)
            .map(this::convertToDto)
            .flatMap(user -> ServerResponse.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user))
            .onErrorResume(ValidationException.class, 
                ex -> ServerResponse.badRequest().bodyValue(ex.getMessage()));
    }
    
    public Mono<ServerResponse> updateUser(ServerRequest request) {
        String id = request.pathVariable("id");
        
        return request.bodyToMono(UpdateUserRequest.class)
            .flatMap(updateRequest -> userService.updateUser(id, updateRequest))
            .map(this::convertToDto)
            .flatMap(user -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        String id = request.pathVariable("id");
        
        return userService.deleteUser(id)
            .then(ServerResponse.noContent().build())
            .onErrorResume(UserNotFoundException.class, 
                ex -> ServerResponse.notFound().build());
    }
}
```

### Advanced Routing

```java
@Configuration
public class AdvancedRoutingConfig {
    
    @Bean
    public RouterFunction<ServerResponse> apiRoutes() {
        return RouterFunctions
            .nest(path("/api/v1"),
                userRoutes()
                    .andOther(productRoutes())
                    .andOther(orderRoutes())
            )
            .filter(loggingFilter())
            .filter(authenticationFilter())
            .filter(rateLimitingFilter());
    }
    
    private RouterFunction<ServerResponse> userRoutes() {
        return RouterFunctions
            .route(GET("/users").and(accept(MediaType.APPLICATION_JSON)), 
                userHandler::getAllUsers)
            .andRoute(GET("/users/{id}").and(accept(MediaType.APPLICATION_JSON)), 
                userHandler::getUser);
    }
    
    private HandlerFilterFunction<ServerResponse, ServerResponse> loggingFilter() {
        return (request, next) -> {
            long startTime = System.currentTimeMillis();
            
            return next.handle(request)
                .doOnNext(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Request: {} {} - Status: {} - Duration: {}ms",
                        request.method(), request.path(), 
                        response.statusCode(), duration);
                });
        };
    }
    
    private HandlerFilterFunction<ServerResponse, ServerResponse> authenticationFilter() {
        return (request, next) -> {
            String token = request.headers().firstHeader("Authorization");
            
            if (token == null || !token.startsWith("Bearer ")) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            return jwtTokenProvider.validateToken(token.substring(7))
                .flatMap(isValid -> {
                    if (isValid) {
                        return next.handle(request);
                    } else {
                        return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                    }
                });
        };
    }
}
```

## Reactive Data Access

### Reactive Repositories

```java
public interface UserRepository extends ReactiveCrudRepository<User, String> {
    
    Flux<User> findByStatus(UserStatus status);
    
    Flux<User> findByEmailContaining(String email);
    
    @Query("SELECT * FROM users WHERE created_at > :date")
    Flux<User> findUsersCreatedAfter(LocalDateTime date);
    
    @Modifying
    @Query("UPDATE users SET status = :status WHERE id = :id")
    Mono<Integer> updateUserStatus(String id, UserStatus status);
    
    Mono<Long> countByStatus(UserStatus status);
    
    Mono<Boolean> existsByEmail(String email);
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public Flux<User> findAll() {
        return userRepository.findAll()
            .onErrorResume(ex -> {
                log.error("Error fetching users", ex);
                return Flux.empty();
            });
    }
    
    public Mono<User> findById(String id) {
        // Try cache first
        return getCachedUser(id)
            .switchIfEmpty(
                userRepository.findById(id)
                    .flatMap(user -> cacheUser(user).thenReturn(user))
            );
    }
    
    public Mono<User> createUser(CreateUserRequest request) {
        return validateUserRequest(request)
            .then(checkEmailUniqueness(request.getEmail()))
            .then(Mono.fromCallable(() -> buildUser(request)))
            .flatMap(userRepository::save)
            .flatMap(user -> cacheUser(user).thenReturn(user));
    }
    
    private Mono<User> getCachedUser(String id) {
        return Mono.fromCallable(() -> 
            (User) redisTemplate.opsForValue().get("user:" + id))
            .onErrorResume(ex -> Mono.empty());
    }
    
    private Mono<Void> cacheUser(User user) {
        return Mono.fromRunnable(() -> 
            redisTemplate.opsForValue().set("user:" + user.getId(), user, 
                Duration.ofMinutes(30)))
            .then();
    }
}
```

### Database Transactions

```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private TransactionalOperator transactionalOperator;
    
    public Mono<Order> createOrder(CreateOrderRequest request) {
        return transactionalOperator.transactional(
            validateOrderRequest(request)
                .then(reserveInventory(request.getItems()))
                .then(processPayment(request.getPayment()))
                .then(createOrderEntity(request))
                .flatMap(orderRepository::save)
                .doOnSuccess(order -> publishOrderCreatedEvent(order))
                .onErrorResume(this::handleOrderCreationError)
        );
    }
    
    private Mono<Void> validateOrderRequest(CreateOrderRequest request) {
        return Mono.fromRunnable(() -> {
            if (request.getItems().isEmpty()) {
                throw new ValidationException("Order must have at least one item");
            }
        });
    }
    
    private Mono<Void> reserveInventory(List<OrderItem> items) {
        return Flux.fromIterable(items)
            .flatMap(item -> inventoryService.reserveItem(item.getProductId(), item.getQuantity()))
            .then();
    }
    
    private Mono<Order> handleOrderCreationError(Throwable error) {
        log.error("Order creation failed", error);
        
        if (error instanceof ValidationException) {
            return Mono.error(new BadRequestException(error.getMessage()));
        } else if (error instanceof InsufficientInventoryException) {
            return Mono.error(new ConflictException("Insufficient inventory"));
        } else {
            return Mono.error(new InternalServerErrorException("Order creation failed"));
        }
    }
}
```

## Error Handling

### Global Error Handler

```java
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        
        response.getHeaders().add("Content-Type", "application/json");
        
        ErrorResponse errorResponse;
        HttpStatus status;
        
        if (ex instanceof ValidationException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = new ErrorResponse("VALIDATION_ERROR", ex.getMessage());
        } else if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorResponse = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        } else if (ex instanceof WebExchangeBindException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = handleValidationErrors((WebExchangeBindException) ex);
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
            log.error("Unexpected error", ex);
        }
        
        response.setStatusCode(status);
        
        String errorJson;
        try {
            errorJson = objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            errorJson = "{\"error\":\"SERIALIZATION_ERROR\",\"message\":\"Error serializing response\"}";
        }
        
        DataBuffer buffer = response.bufferFactory().wrap(errorJson.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
    
    private ErrorResponse handleValidationErrors(WebExchangeBindException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        
        return new ErrorResponse("VALIDATION_ERROR", "Validation failed", errors);
    }
}
```

### Controller Error Handling

```java
@RestController
public class UserController {
    
    @GetMapping("/api/users/{id}")
    public Mono<ResponseEntity<UserDto>> getUser(@PathVariable String id) {
        return userService.findById(id)
            .map(this::convertToDto)
            .map(ResponseEntity::ok)
            .onErrorResume(UserNotFoundException.class, 
                ex -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(DatabaseException.class,
                ex -> {
                    log.error("Database error while fetching user", ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                });
    }
    
    @PostMapping("/api/users")
    public Mono<ResponseEntity<UserDto>> createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request)
            .map(this::convertToDto)
            .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
            .onErrorResume(DuplicateEmailException.class,
                ex -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()))
            .onErrorResume(ValidationException.class,
                ex -> Mono.just(ResponseEntity.badRequest().build()));
    }
}
```

## Testing

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void shouldFindUserById() {
        // Given
        String userId = "123";
        User user = new User(userId, "john@example.com", "John Doe");
        
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        
        // When
        Mono<User> result = userService.findById(userId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(user)
            .verifyComplete();
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        // Given
        String userId = "999";
        when(userRepository.findById(userId)).thenReturn(Mono.empty());
        
        // When
        Mono<User> result = userService.findById(userId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }
    
    @Test
    void shouldHandleErrorWhenFindingUser() {
        // Given
        String userId = "123";
        when(userRepository.findById(userId))
            .thenReturn(Mono.error(new DatabaseException("Connection failed")));
        
        // When
        Mono<User> result = userService.findById(userId);
        
        // Then
        StepVerifier.create(result)
            .expectError(DatabaseException.class)
            .verify();
    }
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserRequest request = new CreateUserRequest("john@example.com", "John Doe");
        User savedUser = new User("123", "john@example.com", "John Doe");
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        
        // When
        Mono<User> result = userService.createUser(request);
        
        // Then
        StepVerifier.create(result)
            .expectNext(savedUser)
            .verifyComplete();
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class UserControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }
    
    @Test
    void shouldGetAllUsers() {
        // Given
        User user1 = new User("1", "john@example.com", "John Doe");
        User user2 = new User("2", "jane@example.com", "Jane Doe");
        
        userRepository.saveAll(Arrays.asList(user1, user2)).blockLast();
        
        // When & Then
        webTestClient.get()
            .uri("/api/users")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(UserDto.class)
            .hasSize(2);
    }
    
    @Test
    void shouldCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest("john@example.com", "John Doe");
        
        // When & Then
        webTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(UserDto.class)
            .value(user -> {
                assertThat(user.getEmail()).isEqualTo("john@example.com");
                assertThat(user.getName()).isEqualTo("John Doe");
            });
    }
    
    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        webTestClient.get()
            .uri("/api/users/999")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound();
    }
}
```

### WebTestClient Testing

```java
@WebFluxTest(UserController.class)
class UserControllerWebFluxTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldStreamUsers() {
        // Given
        Flux<User> userFlux = Flux.just(
            new User("1", "john@example.com", "John"),
            new User("2", "jane@example.com", "Jane")
        ).delayElements(Duration.ofMillis(100));
        
        when(userService.findAll()).thenReturn(userFlux);
        
        // When & Then
        webTestClient.get()
            .uri("/api/stream/users")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectBodyList(UserDto.class)
            .hasSize(2);
    }
    
    @Test
    void shouldHandleValidationErrors() {
        // Given
        CreateUserRequest invalidRequest = new CreateUserRequest("", ""); // Invalid data
        
        // When & Then
        webTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(error -> {
                assertThat(error.getError()).isEqualTo("VALIDATION_ERROR");
                assertThat(error.getDetails()).containsKeys("email", "name");
            });
    }
}
```

## Interview Questions

### Basic Level

**Q1: What is Spring WebFlux and how does it differ from Spring MVC?**

**Answer:** Spring WebFlux is a reactive web framework:
- **Non-blocking I/O**: Uses event loops instead of thread-per-request
- **Reactive Streams**: Built on Mono/Flux reactive types
- **Better Scalability**: Handles more concurrent connections with fewer threads
- **Backpressure**: Handles overwhelming data streams gracefully
- **Functional Programming**: Supports functional routing alongside annotations

**Q2: Explain Mono and Flux in Project Reactor.**

**Answer:**
```java
// Mono - 0 or 1 element
Mono<String> mono = Mono.just("Hello")
    .map(String::toUpperCase)
    .filter(s -> s.length() > 3);

// Flux - 0 to N elements  
Flux<Integer> flux = Flux.range(1, 5)
    .filter(i -> i % 2 == 0)
    .map(i -> i * 2);

// Key differences:
// - Mono: Single value or empty
// - Flux: Stream of values
// - Both are lazy (cold) by default
// - Support backpressure and error handling
```

### Intermediate Level

**Q3: How do you handle backpressure in WebFlux?**

**Answer:**
```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> handleBackpressure() {
    return Flux.interval(Duration.ofMillis(1))
        .map(i -> "Item " + i)
        .onBackpressureBuffer(1000)        // Buffer up to 1000 items
        .onBackpressureDrop(item -> 
            log.warn("Dropped item: {}", item))  // Drop excess items
        .onBackpressureLatest()            // Keep only latest item
        .take(Duration.ofSeconds(10));     // Limit duration
}

// Strategies:
// 1. Buffer: Store excess items in memory
// 2. Drop: Discard excess items
// 3. Latest: Keep only the most recent item
// 4. Error: Throw error when overwhelmed
```

**Q4: Explain error handling in reactive streams.**

**Answer:**
```java
public Mono<User> getUserWithErrorHandling(String id) {
    return userRepository.findById(id)
        .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
        .onErrorResume(DatabaseException.class, 
            ex -> getCachedUser(id))           // Fallback to cache
        .onErrorReturn(User.getDefaultUser())  // Default value
        .retry(3)                              // Retry on error
        .timeout(Duration.ofSeconds(5))        // Timeout handling
        .doOnError(ex -> log.error("Error getting user", ex));
}

// Error operators:
// - onErrorResume: Switch to alternative stream
// - onErrorReturn: Return default value
// - onErrorMap: Transform error
// - retry: Retry on failure
// - timeout: Handle timeouts
```

### Advanced Level

**Q5: Design a reactive microservice with proper error handling and resilience.**

**Answer:**
```java
@Service
public class ResilientOrderService {
    
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Retry retry;
    
    public Mono<Order> createOrder(CreateOrderRequest request) {
        return validateRequest(request)
            .then(checkInventory(request.getItems()))
            .then(processPayment(request.getPayment()))
            .then(saveOrder(request))
            .transform(addResilience())
            .doOnSuccess(order -> publishEvent(new OrderCreatedEvent(order)))
            .onErrorResume(this::handleOrderError);
    }
    
    private <T> Function<Mono<T>, Mono<T>> addResilience() {
        return mono -> mono
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
            .transformDeferred(RetryOperator.of(retry));
    }
    
    private Mono<Order> handleOrderError(Throwable error) {
        if (error instanceof ValidationException) {
            return Mono.error(new BadRequestException(error.getMessage()));
        } else if (error instanceof InsufficientInventoryException) {
            return Mono.error(new ConflictException("Insufficient inventory"));
        } else if (error instanceof PaymentException) {
            // Send to DLQ for manual processing
            return sendToDeadLetterQueue(error)
                .then(Mono.error(new ServiceUnavailableException("Payment service unavailable")));
        } else {
            return Mono.error(new InternalServerErrorException("Order creation failed"));
        }
    }
}

// Configuration
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker orderServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("order-service");
    }
    
    @Bean
    public Retry orderServiceRetry() {
        return Retry.ofDefaults("order-service");
    }
    
    @Bean
    public TimeLimiter orderServiceTimeLimiter() {
        return TimeLimiter.of(Duration.ofSeconds(3));
    }
}
```

**Q6: Implement reactive caching with Redis.**

**Answer:**
```java
@Service
public class ReactiveUserService {
    
    private final UserRepository userRepository;
    private final ReactiveRedisTemplate<String, User> redisTemplate;
    
    public Mono<User> findById(String id) {
        String cacheKey = "user:" + id;
        
        return redisTemplate.opsForValue().get(cacheKey)
            .cast(User.class)
            .switchIfEmpty(
                userRepository.findById(id)
                    .flatMap(user -> cacheUser(cacheKey, user).thenReturn(user))
            )
            .onErrorResume(RedisException.class, 
                ex -> {
                    log.warn("Redis error, falling back to database", ex);
                    return userRepository.findById(id);
                });
    }
    
    public Mono<User> save(User user) {
        return userRepository.save(user)
            .flatMap(savedUser -> {
                String cacheKey = "user:" + savedUser.getId();
                return cacheUser(cacheKey, savedUser)
                    .thenReturn(savedUser);
            });
    }
    
    public Mono<Void> deleteById(String id) {
        String cacheKey = "user:" + id;
        
        return userRepository.deleteById(id)
            .then(redisTemplate.delete(cacheKey))
            .then();
    }
    
    private Mono<Boolean> cacheUser(String key, User user) {
        return redisTemplate.opsForValue()
            .set(key, user, Duration.ofMinutes(30))
            .onErrorResume(ex -> {
                log.warn("Failed to cache user", ex);
                return Mono.just(false);
            });
    }
}

@Configuration
public class ReactiveRedisConfig {
    
    @Bean
    public ReactiveRedisTemplate<String, User> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        RedisSerializationContext<String, User> serializationContext = 
            RedisSerializationContext.<String, User>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(new GenericJackson2JsonRedisSerializer())
                .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
```

## Best Practices

### Performance Optimization

```java
// Good practices for reactive programming
@Service
public class OptimizedUserService {
    
    // Use appropriate operators
    public Flux<UserDto> getActiveUsers() {
        return userRepository.findByStatus(UserStatus.ACTIVE)
            .map(this::convertToDto)           // Transform
            .filter(dto -> dto.isVerified())   // Filter after transformation
            .take(100)                         // Limit results
            .cache(Duration.ofMinutes(5));     // Cache results
    }
    
    // Avoid blocking operations
    public Mono<User> createUserAsync(CreateUserRequest request) {
        return Mono.fromCallable(() -> validateRequest(request))  // CPU-intensive
            .subscribeOn(Schedulers.boundedElastic())             // Use appropriate scheduler
            .then(userRepository.save(buildUser(request)))        // Non-blocking I/O
            .publishOn(Schedulers.parallel())                     // Switch context if needed
            .doOnNext(user -> sendWelcomeEmail(user));           // Side effect
    }
    
    // Proper error handling
    public Mono<User> getUserWithFallback(String id) {
        return userRepository.findById(id)
            .onErrorResume(DatabaseException.class, 
                ex -> getCachedUser(id))
            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
            .timeout(Duration.ofSeconds(5))
            .retry(2);
    }
    
    // Efficient data processing
    public Flux<UserSummary> processUsersInBatches() {
        return userRepository.findAll()
            .buffer(100)                       // Process in batches
            .flatMap(batch -> 
                Flux.fromIterable(batch)
                    .parallel(4)               // Parallel processing
                    .runOn(Schedulers.parallel())
                    .map(this::processUser)
                    .sequential()
            );
    }
}
```

### Memory Management

```java
@Configuration
public class WebFluxOptimizationConfig {
    
    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        
        factory.addServerCustomizers(server -> 
            server.option(ChannelOption.SO_BACKLOG, 1024)
                  .childOption(ChannelOption.SO_KEEPALIVE, true)
                  .childOption(ChannelOption.TCP_NODELAY, true)
                  .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        );
        
        return factory;
    }
    
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB limit
        configurer.defaultCodecs().enableLoggingRequestDetails(false); // Disable in production
    }
}

// Proper resource management
@RestController
public class FileController {
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadFile(@RequestPart("file") Mono<FilePart> filePartMono) {
        return filePartMono
            .flatMap(filePart -> {
                Path tempFile = null;
                try {
                    tempFile = Files.createTempFile("upload-", filePart.filename());
                    return filePart.transferTo(tempFile)
                        .then(processFile(tempFile))
                        .doFinally(signalType -> {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException e) {
                                log.warn("Failed to delete temp file", e);
                            }
                        });
                } catch (IOException e) {
                    return Mono.error(e);
                }
            })
            .map(result -> ResponseEntity.ok("File processed successfully"));
    }
}
```

This comprehensive Spring WebFlux guide covers reactive programming concepts, WebFlux architecture, controllers, functional endpoints, data access, error handling, testing, and production-ready best practices.