# API Gateway Pattern - Deep Dive

## Table of Contents
1. [Introduction](#introduction)
2. [Theory and Concepts](#theory-and-concepts)
3. [Core Responsibilities](#core-responsibilities)
4. [Implementation Patterns](#implementation-patterns)
5. [Practical Examples](#practical-examples)
6. [Best Practices](#best-practices)
7. [Common Pitfalls](#common-pitfalls)

---

## Introduction

The **API Gateway Pattern** is a design pattern that provides a single entry point for all client requests to a microservices architecture. It acts as a reverse proxy, routing requests to appropriate backend services while handling cross-cutting concerns.

### Why API Gateway?

In microservices architecture:
- Clients need to call multiple services
- Each service has different protocols and APIs
- Cross-cutting concerns are duplicated
- Security and authentication are complex
- Network chattiness increases latency

**API Gateway solves this by:**
- Providing single entry point
- Routing requests to appropriate services
- Aggregating responses from multiple services
- Handling authentication, authorization, rate limiting
- Protocol translation (REST to gRPC, etc.)

---

## Theory and Concepts

### Architecture Overview

```
┌─────────┐
│ Client  │
└────┬────┘
     │
     ↓
┌────────────────┐
│  API Gateway   │ ← Single Entry Point
│                │
│ - Routing      │
│ - Auth         │
│ - Rate Limit   │
│ - Aggregation  │
└────┬───────────┘
     │
     ├──────┬──────┬──────┐
     ↓      ↓      ↓      ↓
┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│Svc A│ │Svc B│ │Svc C│ │Svc D│
└─────┘ └─────┘ └─────┘ └─────┘
```

### Key Concepts

1. **Single Entry Point**: All client requests go through gateway
2. **Request Routing**: Routes to appropriate backend service
3. **Response Aggregation**: Combines multiple service responses
4. **Protocol Translation**: Converts between protocols
5. **Cross-Cutting Concerns**: Centralized handling

### Gateway Types

1. **Backend for Frontend (BFF)**: Separate gateway per client type
2. **Unified Gateway**: Single gateway for all clients
3. **Micro Gateway**: Lightweight gateway per service group

---

## Core Responsibilities

### 1. Request Routing

Route requests to appropriate backend services based on URL, headers, or other criteria.

```java
@Configuration
public class GatewayRouting {
    
    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service
            .route("user-service", r -> r
                .path("/api/users/**")
                .uri("lb://user-service"))
            
            // Order Service
            .route("order-service", r -> r
                .path("/api/orders/**")
                .uri("lb://order-service"))
            
            // Payment Service
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .uri("lb://payment-service"))
            
            .build();
    }
}
```

### 2. Authentication & Authorization

Centralized security handling for all services.

```java
@Component
public class AuthenticationFilter implements GatewayFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Extract token
        String token = extractToken(request);
        if (token == null) {
            return unauthorized(exchange);
        }
        
        // Validate token
        if (!tokenProvider.validateToken(token)) {
            return unauthorized(exchange);
        }
        
        // Extract user info and add to headers
        String userId = tokenProvider.getUserId(token);
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-User-Id", userId)
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
    
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
```

### 3. Rate Limiting

Protect backend services from overload.

```java
@Component
public class RateLimitingFilter implements GatewayFilter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = getClientId(exchange);
        String key = "rate_limit:" + clientId;
        
        return Mono.fromCallable(() -> {
            Long requests = redisTemplate.opsForValue().increment(key);
            
            if (requests == 1) {
                redisTemplate.expire(key, WINDOW);
            }
            
            if (requests > MAX_REQUESTS) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                return false;
            }
            
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS - requests));
            return true;
        })
        .flatMap(allowed -> allowed ? chain.filter(exchange) : exchange.getResponse().setComplete());
    }
    
    private String getClientId(ServerWebExchange exchange) {
        // Extract from API key, user ID, or IP address
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey != null) return apiKey;
        
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
```

### 4. Request/Response Transformation

Modify requests and responses as needed.

```java
@Component
public class RequestTransformationFilter implements GatewayFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Add correlation ID
        String correlationId = UUID.randomUUID().toString();
        
        // Add custom headers
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-Correlation-Id", correlationId)
            .header("X-Gateway-Timestamp", String.valueOf(System.currentTimeMillis()))
            .build();
        
        // Modify response
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-Correlation-Id", correlationId);
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
}
```

### 5. Response Aggregation

Combine responses from multiple services.

```java
@Service
public class ResponseAggregationService {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    public Mono<UserProfile> getUserProfile(String userId) {
        WebClient webClient = webClientBuilder.build();
        
        // Parallel calls to multiple services
        Mono<User> userMono = webClient.get()
            .uri("http://user-service/users/{id}", userId)
            .retrieve()
            .bodyToMono(User.class);
        
        Mono<List<Order>> ordersMono = webClient.get()
            .uri("http://order-service/users/{id}/orders", userId)
            .retrieve()
            .bodyToFlux(Order.class)
            .collectList();
        
        Mono<PaymentMethod> paymentMono = webClient.get()
            .uri("http://payment-service/users/{id}/payment-methods", userId)
            .retrieve()
            .bodyToMono(PaymentMethod.class);
        
        // Combine results
        return Mono.zip(userMono, ordersMono, paymentMono)
            .map(tuple -> UserProfile.builder()
                .user(tuple.getT1())
                .orders(tuple.getT2())
                .paymentMethod(tuple.getT3())
                .build());
    }
}
```

### 6. Load Balancing

Distribute requests across service instances.

```java
@Configuration
public class LoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public RouteLocator loadBalancedRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("userServiceCB")
                        .setFallbackUri("forward:/fallback/users"))
                )
                .uri("lb://user-service"))
            .build();
    }
}
```

---

## Implementation Patterns

### Pattern 1: Spring Cloud Gateway

```java
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service Routes
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Gateway", "API-Gateway")
                    .circuitBreaker(c -> c.setName("userCB"))
                    .retry(retryConfig -> retryConfig.setRetries(3))
                )
                .uri("lb://user-service"))
            
            // Order Service Routes
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                )
                .uri("lb://order-service"))
            
            .build();
    }
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 200); // replenishRate, burstCapacity
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("X-User-Id")
        );
    }
}
```

### Pattern 2: Netflix Zuul (Legacy)

```java
@EnableZuulProxy
@SpringBootApplication
public class ZuulGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZuulGatewayApplication.class, args);
    }
}

@Component
public class AuthenticationFilter extends ZuulFilter {
    
    @Override
    public String filterType() {
        return "pre";
    }
    
    @Override
    public int filterOrder() {
        return 1;
    }
    
    @Override
    public boolean shouldFilter() {
        return true;
    }
    
    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        
        String token = request.getHeader("Authorization");
        if (token == null || !validateToken(token)) {
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(401);
            return null;
        }
        
        return null;
    }
}
```

### Pattern 3: Custom Gateway with WebFlux

```java
@RestController
@RequestMapping("/gateway")
public class CustomGatewayController {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @GetMapping("/users/{id}")
    public Mono<ResponseEntity<User>> getUser(@PathVariable String id) {
        return webClientBuilder.build()
            .get()
            .uri("http://user-service/users/{id}", id)
            .retrieve()
            .toEntity(User.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(503).build()));
    }
    
    @PostMapping("/orders")
    public Mono<ResponseEntity<Order>> createOrder(@RequestBody OrderRequest request) {
        return webClientBuilder.build()
            .post()
            .uri("http://order-service/orders")
            .bodyValue(request)
            .retrieve()
            .toEntity(Order.class);
    }
}
```

### Pattern 4: Backend for Frontend (BFF)

```java
// Mobile BFF
@RestController
@RequestMapping("/mobile/api")
public class MobileBFFController {
    
    @GetMapping("/home")
    public Mono<MobileHomeResponse> getHomeScreen(@RequestHeader("X-User-Id") String userId) {
        // Optimized for mobile - minimal data
        return Mono.zip(
            userService.getBasicInfo(userId),
            orderService.getRecentOrders(userId, 5),
            notificationService.getUnreadCount(userId)
        ).map(tuple -> MobileHomeResponse.builder()
            .user(tuple.getT1())
            .recentOrders(tuple.getT2())
            .unreadNotifications(tuple.getT3())
            .build());
    }
}

// Web BFF
@RestController
@RequestMapping("/web/api")
public class WebBFFController {
    
    @GetMapping("/dashboard")
    public Mono<WebDashboardResponse> getDashboard(@RequestHeader("X-User-Id") String userId) {
        // Optimized for web - more detailed data
        return Mono.zip(
            userService.getFullProfile(userId),
            orderService.getAllOrders(userId),
            analyticsService.getUserAnalytics(userId),
            recommendationService.getRecommendations(userId)
        ).map(tuple -> WebDashboardResponse.builder()
            .profile(tuple.getT1())
            .orders(tuple.getT2())
            .analytics(tuple.getT3())
            .recommendations(tuple.getT4())
            .build());
    }
}
```

---

## Practical Examples

### Example 1: E-Commerce API Gateway

```java
@Configuration
public class EcommerceGatewayConfig {
    
    @Bean
    public RouteLocator ecommerceRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // Product Catalog
            .route("products", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("productCB")
                        .setFallbackUri("forward:/fallback/products"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter(1000, 2000)))
                )
                .uri("lb://product-service"))
            
            // Shopping Cart
            .route("cart", r -> r
                .path("/api/cart/**")
                .filters(f -> f
                    .filter(authenticationFilter())
                    .circuitBreaker(c -> c.setName("cartCB"))
                )
                .uri("lb://cart-service"))
            
            // Checkout
            .route("checkout", r -> r
                .path("/api/checkout/**")
                .filters(f -> f
                    .filter(authenticationFilter())
                    .filter(paymentValidationFilter())
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter(10, 20)))
                )
                .uri("lb://checkout-service"))
            
            // Order History
            .route("orders", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .filter(authenticationFilter())
                )
                .uri("lb://order-service"))
            
            .build();
    }
}
```

### Example 2: Microservices Aggregation

```java
@RestController
@RequestMapping("/api/aggregated")
public class AggregationController {
    
    @Autowired
    private WebClient.Builder webClient;
    
    @GetMapping("/product/{id}/details")
    public Mono<ProductDetails> getProductDetails(@PathVariable String id) {
        // Aggregate from multiple services
        Mono<Product> productMono = webClient.build()
            .get()
            .uri("http://product-service/products/{id}", id)
            .retrieve()
            .bodyToMono(Product.class);
        
        Mono<Inventory> inventoryMono = webClient.build()
            .get()
            .uri("http://inventory-service/products/{id}/inventory", id)
            .retrieve()
            .bodyToMono(Inventory.class);
        
        Mono<List<Review>> reviewsMono = webClient.build()
            .get()
            .uri("http://review-service/products/{id}/reviews", id)
            .retrieve()
            .bodyToFlux(Review.class)
            .collectList();
        
        Mono<PriceInfo> priceMono = webClient.build()
            .get()
            .uri("http://pricing-service/products/{id}/price", id)
            .retrieve()
            .bodyToMono(PriceInfo.class);
        
        return Mono.zip(productMono, inventoryMono, reviewsMono, priceMono)
            .map(tuple -> ProductDetails.builder()
                .product(tuple.getT1())
                .inventory(tuple.getT2())
                .reviews(tuple.getT3())
                .pricing(tuple.getT4())
                .build())
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> Mono.just(ProductDetails.error(id, e.getMessage())));
    }
}
```

### Example 3: Protocol Translation

```java
@Service
public class ProtocolTranslationService {
    
    @Autowired
    private GrpcClient grpcClient;
    
    // REST to gRPC
    public Mono<UserResponse> getUserViaGrpc(String userId) {
        return Mono.fromCallable(() -> {
            // Convert REST request to gRPC
            UserRequest grpcRequest = UserRequest.newBuilder()
                .setUserId(userId)
                .build();
            
            // Call gRPC service
            UserProto grpcResponse = grpcClient.getUser(grpcRequest);
            
            // Convert gRPC response to REST
            return UserResponse.builder()
                .id(grpcResponse.getId())
                .name(grpcResponse.getName())
                .email(grpcResponse.getEmail())
                .build();
        });
    }
    
    // GraphQL to REST
    public Mono<GraphQLResponse> executeGraphQLQuery(GraphQLRequest request) {
        // Parse GraphQL query
        String query = request.getQuery();
        
        // Determine which REST endpoints to call
        List<Mono<?>> serviceCalls = parseAndCreateServiceCalls(query);
        
        // Execute and aggregate
        return Mono.zip(serviceCalls, results -> 
            buildGraphQLResponse(results)
        );
    }
}
```

### Example 4: API Versioning

```java
@Configuration
public class VersioningGatewayConfig {
    
    @Bean
    public RouteLocator versionedRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // V1 API
            .route("users-v1", r -> r
                .path("/api/v1/users/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://user-service-v1"))
            
            // V2 API
            .route("users-v2", r -> r
                .path("/api/v2/users/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://user-service-v2"))
            
            // Header-based versioning
            .route("users-header", r -> r
                .path("/api/users/**")
                .and()
                .header("API-Version", "2.0")
                .filters(f -> f.stripPrefix(1))
                .uri("lb://user-service-v2"))
            
            .build();
    }
}
```

---

## Best Practices

### 1. Implement Health Checks

```java
@RestController
@RequestMapping("/actuator")
public class GatewayHealthController {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @GetMapping("/health")
    public Mono<HealthResponse> health() {
        return Mono.fromCallable(() -> {
            Map<String, ServiceHealth> services = new HashMap<>();
            
            discoveryClient.getServices().forEach(serviceName -> {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                services.put(serviceName, ServiceHealth.builder()
                    .status(instances.isEmpty() ? "DOWN" : "UP")
                    .instanceCount(instances.size())
                    .build());
            });
            
            return HealthResponse.builder()
                .status("UP")
                .services(services)
                .build();
        });
    }
}
```

### 2. Implement Caching

```java
@Component
public class CachingFilter implements GatewayFilter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String cacheKey = generateCacheKey(exchange.getRequest());
        
        // Check cache
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            exchange.getResponse().getHeaders().add("X-Cache", "HIT");
            return writeResponse(exchange, cached);
        }
        
        // Cache miss - proceed with request
        exchange.getResponse().getHeaders().add("X-Cache", "MISS");
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Cache response (simplified)
            redisTemplate.opsForValue().set(cacheKey, "response", Duration.ofMinutes(5));
        }));
    }
}
```

### 3. Implement Request Logging

```java
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();
        
        log.info("Request: {} {} from {}", 
            request.getMethod(), 
            request.getURI(), 
            request.getRemoteAddress());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();
            
            log.info("Response: {} {} - Status: {} - Duration: {}ms",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode(),
                duration);
        }));
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

### 4. Implement Timeout Handling

```java
@Configuration
public class TimeoutConfig {
    
    @Bean
    public RouteLocator timeoutRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("timeout-route", r -> r
                .path("/api/**")
                .filters(f -> f
                    .hystrix(config -> config
                        .setName("timeoutCB")
                        .setFallbackUri("forward:/timeout-fallback"))
                )
                .metadata("response-timeout", 5000)
                .uri("lb://backend-service"))
            .build();
    }
    
    @RestController
    public class FallbackController {
        
        @GetMapping("/timeout-fallback")
        public Mono<ResponseEntity<ErrorResponse>> timeoutFallback() {
            return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ErrorResponse.builder()
                    .error("Service Timeout")
                    .message("The request took too long to process")
                    .build()));
        }
    }
}
```

### 5. Implement CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("https://example.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
```

---

## Common Pitfalls

### 1. ❌ Gateway Becomes Monolith

**Problem:**
```java
// BAD: Too much business logic in gateway
@RestController
public class GatewayController {
    
    @PostMapping("/api/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        // Validate order
        validateOrder(request);
        
        // Calculate pricing
        BigDecimal price = calculatePrice(request);
        
        // Apply discounts
        BigDecimal discount = applyDiscounts(request);
        
        // Check inventory
        checkInventory(request);
        
        // Process payment
        processPayment(request);
        
        // Create order
        return orderService.create(request);
    }
}
```

**Solution:**
```java
// GOOD: Gateway only routes and handles cross-cutting concerns
@Configuration
public class GatewayRoutes {
    
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("orders", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .filter(authFilter())
                    .filter(rateLimitFilter())
                )
                .uri("lb://order-service"))
            .build();
    }
}
```

### 2. ❌ No Timeout Configuration

**Problem:**
```java
// BAD: No timeout - can hang indefinitely
webClient.get()
    .uri("http://slow-service/data")
    .retrieve()
    .bodyToMono(Data.class)
    .block(); // Can block forever!
```

**Solution:**
```java
// GOOD: Always set timeouts
webClient.get()
    .uri("http://slow-service/data")
    .retrieve()
    .bodyToMono(Data.class)
    .timeout(Duration.ofSeconds(5))
    .onErrorResume(TimeoutException.class, e -> 
        Mono.just(Data.fallback())
    );
```

### 3. ❌ Synchronous Aggregation

**Problem:**
```java
// BAD: Sequential calls - slow!
public UserProfile getUserProfile(String userId) {
    User user = userService.getUser(userId);           // 100ms
    List<Order> orders = orderService.getOrders(userId); // 150ms
    Payment payment = paymentService.getPayment(userId); // 120ms
    // Total: 370ms
    
    return new UserProfile(user, orders, payment);
}
```

**Solution:**
```java
// GOOD: Parallel calls - fast!
public Mono<UserProfile> getUserProfile(String userId) {
    return Mono.zip(
        userService.getUser(userId),
        orderService.getOrders(userId),
        paymentService.getPayment(userId)
    ).map(tuple -> new UserProfile(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    // Total: ~150ms (slowest service)
}
```

### 4. ❌ No Circuit Breaker

**Problem:**
```java
// BAD: No protection against failing services
.route("orders", r -> r
    .path("/api/orders/**")
    .uri("lb://order-service"))
```

**Solution:**
```java
// GOOD: Circuit breaker protects gateway
.route("orders", r -> r
    .path("/api/orders/**")
    .filters(f -> f
        .circuitBreaker(c -> c
            .setName("orderCB")
            .setFallbackUri("forward:/fallback/orders"))
    )
    .uri("lb://order-service"))
```

### 5. ❌ Exposing Internal Service URLs

**Problem:**
```java
// BAD: Exposes internal structure
GET /api/user-service/users/123
GET /api/order-service/orders/456
```

**Solution:**
```java
// GOOD: Clean, unified API
GET /api/users/123
GET /api/orders/456
```

---

## Real-World Examples

### Netflix Zuul
- Handles billions of requests daily
- Dynamic routing and filtering
- Multi-region failover
- Now migrating to Spring Cloud Gateway

### Amazon API Gateway
- Fully managed service
- Handles millions of requests per second
- Built-in DDoS protection
- Pay-per-use pricing

### Kong Gateway
- Open-source API gateway
- Plugin architecture
- Used by companies like NASA, Samsung
- Supports REST, GraphQL, gRPC

---

## Conclusion

API Gateway is essential for microservices architecture. Key takeaways:

- **Single entry point** for all client requests
- **Centralize cross-cutting concerns** (auth, rate limiting, logging)
- **Aggregate responses** from multiple services
- **Use async/reactive** programming for better performance
- **Implement circuit breakers** for resilience
- **Keep gateway thin** - avoid business logic
- **Monitor and log** all requests

Remember: Gateway should be a smart router, not a monolith!
