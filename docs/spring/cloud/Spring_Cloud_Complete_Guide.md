# Spring Cloud - Complete Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Microservices Architecture](#microservices-architecture)
3. [Service Discovery](#service-discovery)
4. [API Gateway](#api-gateway)
5. [Configuration Management](#configuration-management)
6. [Circuit Breaker](#circuit-breaker)
7. [Load Balancing](#load-balancing)
8. [Distributed Tracing](#distributed-tracing)
9. [Interview Questions](#interview-questions)
10. [Best Practices](#best-practices)

## Introduction

Spring Cloud provides tools for building distributed systems and microservices, offering solutions for common patterns like service discovery, configuration management, circuit breakers, and distributed tracing.

### Core Components
- **Service Discovery**: Eureka, Consul, Zookeeper
- **API Gateway**: Spring Cloud Gateway, Zuul
- **Configuration**: Spring Cloud Config
- **Circuit Breaker**: Hystrix, Resilience4j
- **Load Balancing**: Ribbon, Spring Cloud LoadBalancer
- **Tracing**: Sleuth, Zipkin

## Microservices Architecture

### Service Structure

```
┌─────────────────────────────────────────────────────────┐
│                  API Gateway                           │
│              (Spring Cloud Gateway)                    │
└─────────────────────┬───────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼─────┐ ┌────▼─────┐
│ User Service │ │Order Svc │ │Product   │
│              │ │          │ │Service   │
└──────────────┘ └──────────┘ └──────────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │      Service Registry     │
        │        (Eureka)           │
        └───────────────────────────┘
```

### Basic Microservice Setup

```java
// User Service
@SpringBootApplication
@EnableEurekaClient
@EnableJpaRepositories
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}

# application.yml
server:
  port: 8081

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: user
    password: password

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

## Service Discovery

### Eureka Server

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}

# eureka-server application.yml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 4000
```

### Service Registration and Discovery

```java
// Service Client
@Component
public class OrderServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    // Using service name (load balanced)
    public User getUserById(Long userId) {
        String url = "http://user-service/api/users/" + userId;
        return restTemplate.getForObject(url, User.class);
    }
    
    // Manual service discovery
    public List<ServiceInstance> getUserServiceInstances() {
        return discoveryClient.getInstances("user-service");
    }
    
    // Using Feign Client
    @FeignClient(name = "user-service")
    public interface UserServiceClient {
        
        @GetMapping("/api/users/{id}")
        User getUserById(@PathVariable("id") Long id);
        
        @PostMapping("/api/users")
        User createUser(@RequestBody CreateUserRequest request);
    }
}

@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### Health Checks and Monitoring

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public Health health() {
        try {
            long userCount = userRepository.count();
            
            if (userCount >= 0) {
                return Health.up()
                    .withDetail("userCount", userCount)
                    .withDetail("status", "Database connection successful")
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "Unable to connect to database")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "Database connection failed")
                .withException(e)
                .build();
        }
    }
}

# Health check configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true

eureka:
  instance:
    health-check-url-path: /actuator/health
    status-page-url-path: /actuator/info
```

## API Gateway

### Spring Cloud Gateway

```java
@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service Routes
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway", "Spring-Cloud-Gateway")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("user-service-cb")
                        .setFallbackUri("forward:/fallback/users")
                    )
                )
                .uri("lb://user-service")
            )
            
            // Order Service Routes
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .rewritePath("/api/orders/(?<segment>.*)", "/orders/${segment}")
                    .retry(config -> config
                        .setRetries(3)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)
                    )
                )
                .uri("lb://order-service")
            )
            
            // Rate Limiting
            .route("product-service", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())
                    )
                )
                .uri("lb://product-service")
            )
            .build();
    }
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1); // replenishRate, burstCapacity, requestedTokens
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getRequest().getHeaders()
            .getFirst("X-User-Id") != null ? 
            Mono.just(exchange.getRequest().getHeaders().getFirst("X-User-Id")) :
            Mono.just("anonymous");
    }
}
```

### Custom Filters

```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        
        String token = extractToken(request);
        
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        
        // Add user info to headers
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-User-Id", userId)
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
    
    @Override
    public int getOrder() {
        return -100; // High priority
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") || 
               path.startsWith("/api/public/") ||
               path.equals("/actuator/health");
    }
    
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        logger.info("Request: {} {} from {}", 
            request.getMethod(), 
            request.getPath(), 
            request.getRemoteAddress());
        
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(
            Mono.fromRunnable(() -> {
                long endTime = System.currentTimeMillis();
                ServerHttpResponse response = exchange.getResponse();
                
                logger.info("Response: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getPath(),
                    response.getStatusCode(),
                    endTime - startTime);
            })
        );
    }
    
    @Override
    public int getOrder() {
        return -1;
    }
}
```

## Configuration Management

### Spring Cloud Config Server

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

# config-server application.yml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/config-repo
          search-paths: '{application}'
          clone-on-start: true
        health:
          repositories:
            myrepo:
              label: main
              name: user-service
              profiles: dev,prod

management:
  endpoints:
    web:
      exposure:
        include: '*'
```

### Config Client

```java
@SpringBootApplication
@EnableEurekaClient
@RefreshScope
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

@RestController
@RefreshScope
public class ConfigController {
    
    @Value("${app.message:Default Message}")
    private String message;
    
    @Value("${app.database.max-connections:10}")
    private int maxConnections;
    
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("message", message);
        config.put("maxConnections", maxConnections);
        return config;
    }
}

# bootstrap.yml (client)
spring:
  application:
    name: user-service
  profiles:
    active: dev
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
        max-interval: 2000
        multiplier: 1.1

management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info
```

### Dynamic Configuration Updates

```java
@Component
@ConfigurationProperties(prefix = "app.feature")
@RefreshScope
public class FeatureConfig {
    
    private boolean enableNewFeature = false;
    private int maxRetries = 3;
    private Duration timeout = Duration.ofSeconds(30);
    
    // Getters and setters
}

@Service
@RefreshScope
public class FeatureService {
    
    @Autowired
    private FeatureConfig featureConfig;
    
    public void processRequest() {
        if (featureConfig.isEnableNewFeature()) {
            // Use new feature implementation
            processWithNewFeature();
        } else {
            // Use legacy implementation
            processWithLegacyFeature();
        }
    }
}

@RestController
public class AdminController {
    
    @Autowired
    private RefreshEndpoint refreshEndpoint;
    
    @PostMapping("/admin/refresh-config")
    public Collection<String> refreshConfig() {
        return refreshEndpoint.refresh();
    }
}
```

## Circuit Breaker

### Resilience4j Implementation

```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker userServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("user-service");
    }
    
    @Bean
    public TimeLimiter userServiceTimeLimiter() {
        return TimeLimiter.of(Duration.ofSeconds(3));
    }
    
    @Bean
    public Retry userServiceRetry() {
        return Retry.ofDefaults("user-service");
    }
}

@Service
public class OrderService {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Retry retry;
    
    public OrderService(CircuitBreaker userServiceCircuitBreaker,
                       TimeLimiter userServiceTimeLimiter,
                       Retry userServiceRetry) {
        this.circuitBreaker = userServiceCircuitBreaker;
        this.timeLimiter = userServiceTimeLimiter;
        this.retry = userServiceRetry;
    }
    
    public Order createOrder(CreateOrderRequest request) {
        // Get user with circuit breaker, retry, and timeout
        User user = getUserWithResilience(request.getUserId());
        
        Order order = new Order();
        order.setUserId(user.getId());
        order.setItems(request.getItems());
        
        return orderRepository.save(order);
    }
    
    private User getUserWithResilience(Long userId) {
        Supplier<CompletableFuture<User>> futureSupplier = () -> 
            CompletableFuture.supplyAsync(() -> userServiceClient.getUserById(userId));
        
        Supplier<CompletableFuture<User>> decoratedSupplier = Decorators
            .ofSupplier(futureSupplier)
            .withCircuitBreaker(circuitBreaker)
            .withTimeLimiter(timeLimiter)
            .withRetry(retry)
            .withFallback(Arrays.asList(Exception.class), 
                throwable -> CompletableFuture.completedFuture(getDefaultUser(userId)))
            .decorate();
        
        try {
            return decoratedSupplier.get().get();
        } catch (Exception e) {
            throw new ServiceUnavailableException("User service unavailable", e);
        }
    }
    
    private User getDefaultUser(Long userId) {
        User defaultUser = new User();
        defaultUser.setId(userId);
        defaultUser.setUsername("unknown");
        defaultUser.setEmail("unknown@example.com");
        return defaultUser;
    }
}
```

### Circuit Breaker Configuration

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        wait-duration-in-open-state: 5s
        failure-rate-threshold: 50
        event-consumer-buffer-size: 10
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - com.example.exception.BusinessException
  
  retry:
    instances:
      user-service:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
  
  timelimiter:
    instances:
      user-service:
        timeout-duration: 3s
        cancel-running-future: true

management:
  health:
    circuitbreakers:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,retries
```

## Load Balancing

### Spring Cloud LoadBalancer

```java
@Configuration
public class LoadBalancerConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> userServiceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        
        return new RoundRobinLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name
        );
    }
}

@Component
public class CustomLoadBalancerConfiguration {
    
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withHealthChecks()
            .withCaching()
            .build(context);
    }
}

// Custom Load Balancer
public class WeightedLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
            .getIfAvailable(NoopServiceInstanceListSupplier::new);
        
        return supplier.get(request)
            .next()
            .map(serviceInstances -> processInstanceResponse(serviceInstances, request));
    }
    
    private Response<ServiceInstance> processInstanceResponse(
            List<ServiceInstance> serviceInstances, Request request) {
        
        if (serviceInstances.isEmpty()) {
            return new EmptyResponse();
        }
        
        // Weighted selection based on metadata
        ServiceInstance selected = selectByWeight(serviceInstances);
        return new DefaultResponse(selected);
    }
    
    private ServiceInstance selectByWeight(List<ServiceInstance> instances) {
        int totalWeight = instances.stream()
            .mapToInt(instance -> getWeight(instance))
            .sum();
        
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;
        
        for (ServiceInstance instance : instances) {
            currentWeight += getWeight(instance);
            if (randomWeight < currentWeight) {
                return instance;
            }
        }
        
        return instances.get(0); // Fallback
    }
    
    private int getWeight(ServiceInstance instance) {
        String weight = instance.getMetadata().get("weight");
        return weight != null ? Integer.parseInt(weight) : 1;
    }
}
```

## Distributed Tracing

### Spring Cloud Sleuth with Zipkin

```java
@Configuration
public class TracingConfig {
    
    @Bean
    public Sender sender() {
        return OkHttpSender.create("http://localhost:9411/api/v2/spans");
    }
    
    @Bean
    public AsyncReporter<Span> spanReporter() {
        return AsyncReporter.create(sender());
    }
    
    @Bean
    public Tracing tracing() {
        return Tracing.newBuilder()
            .localServiceName("order-service")
            .spanReporter(spanReporter())
            .sampler(Sampler.create(1.0f)) // Sample 100% of traces
            .build();
    }
}

@RestController
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private Tracer tracer;
    
    @PostMapping("/api/orders")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Span span = tracer.nextSpan()
            .name("create-order")
            .tag("user.id", request.getUserId().toString())
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            Order order = orderService.createOrder(request);
            span.tag("order.id", order.getId().toString());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}

@Service
public class OrderService {
    
    @NewSpan("user-lookup")
    public User getUser(@SpanTag("userId") Long userId) {
        return userServiceClient.getUserById(userId);
    }
    
    @ContinueSpan
    public Order processOrder(@SpanTag("orderId") Long orderId) {
        // Processing logic with continued span
        return orderRepository.findById(orderId);
    }
}
```

### Custom Tracing

```java
@Component
public class CustomTraceFilter implements Filter {
    
    private final Tracer tracer;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        Span span = tracer.nextSpan()
            .name("http-request")
            .tag("http.method", httpRequest.getMethod())
            .tag("http.url", httpRequest.getRequestURL().toString())
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            chain.doFilter(request, response);
            
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            span.tag("http.status_code", String.valueOf(httpResponse.getStatus()));
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}

@Aspect
@Component
public class TracingAspect {
    
    @Autowired
    private Tracer tracer;
    
    @Around("@annotation(Traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Span span = tracer.nextSpan()
            .name(className + "." + methodName)
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return joinPoint.proceed();
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
}
```

## Interview Questions

### Basic Level

**Q1: What is Spring Cloud and what problems does it solve?**

**Answer:** Spring Cloud provides tools for building distributed systems and microservices:
- **Service Discovery**: Automatic service registration and discovery
- **Configuration Management**: Centralized configuration with dynamic updates
- **API Gateway**: Single entry point with routing, filtering, and security
- **Circuit Breaker**: Fault tolerance and resilience patterns
- **Load Balancing**: Distribute requests across service instances
- **Distributed Tracing**: Track requests across multiple services

**Q2: Explain the difference between Eureka and Consul for service discovery.**

**Answer:**
| Feature | Eureka | Consul |
|---------|--------|--------|
| **Health Checks** | Basic HTTP/TCP | Advanced health checks |
| **Consistency** | AP (Eventually consistent) | CP (Strongly consistent) |
| **Multi-DC** | Limited support | Native multi-datacenter |
| **Key-Value Store** | No | Yes |
| **Service Mesh** | No | Yes (Connect) |
| **Complexity** | Simple | More features, complex |

### Intermediate Level

**Q3: How does Spring Cloud Gateway differ from Zuul?**

**Answer:**
```java
// Spring Cloud Gateway (Reactive)
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("user-service", r -> r
            .path("/api/users/**")
            .filters(f -> f
                .circuitBreaker(config -> config.setName("user-cb"))
                .retry(3)
            )
            .uri("lb://user-service")
        )
        .build();
}

// Zuul (Servlet-based)
@Component
public class UserServiceFilter extends ZuulFilter {
    @Override
    public String filterType() { return "pre"; }
    
    @Override
    public int filterOrder() { return 1; }
    
    @Override
    public boolean shouldFilter() { return true; }
    
    @Override
    public Object run() {
        // Filter logic
        return null;
    }
}
```

**Key Differences:**
- **Architecture**: Gateway is reactive (WebFlux), Zuul is servlet-based
- **Performance**: Gateway has better performance and lower memory usage
- **Features**: Gateway has built-in circuit breaker, rate limiting
- **Maintenance**: Zuul 1.x is in maintenance mode

**Q4: Explain circuit breaker patterns and their states.**

**Answer:**
```java
// Circuit Breaker States
public enum CircuitBreakerState {
    CLOSED,    // Normal operation, requests pass through
    OPEN,      // Failing fast, requests rejected immediately
    HALF_OPEN  // Testing if service recovered
}

// State Transitions
CLOSED → OPEN: When failure rate exceeds threshold
OPEN → HALF_OPEN: After wait duration expires
HALF_OPEN → CLOSED: When test requests succeed
HALF_OPEN → OPEN: When test requests fail
```

### Advanced Level

**Q5: Design a resilient microservices communication pattern.**

**Answer:**
```java
@Service
public class ResilientOrderService {
    
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final Bulkhead bulkhead;
    
    public Order createOrder(CreateOrderRequest request) {
        return executeWithResilience(() -> {
            // Validate inventory
            validateInventory(request.getItems());
            
            // Process payment
            PaymentResult payment = processPayment(request.getPayment());
            
            // Create order
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setPaymentId(payment.getId());
            
            return orderRepository.save(order);
        });
    }
    
    private <T> T executeWithResilience(Supplier<T> operation) {
        Supplier<T> decoratedSupplier = Decorators.ofSupplier(operation)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withTimeLimiter(timeLimiter)
            .withBulkhead(bulkhead)
            .withFallback(Arrays.asList(Exception.class), 
                throwable -> handleFallback(throwable))
            .decorate();
        
        return decoratedSupplier.get();
    }
    
    private Order handleFallback(Throwable throwable) {
        // Log error, send to DLQ, return cached response, etc.
        throw new ServiceUnavailableException("Order service temporarily unavailable");
    }
}

// Configuration
resilience4j:
  circuitbreaker:
    instances:
      order-service:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  
  retry:
    instances:
      order-service:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
  
  bulkhead:
    instances:
      order-service:
        max-concurrent-calls: 10
        max-wait-duration: 1s
```

**Q6: Implement distributed configuration with encryption and profiles.**

**Answer:**
```java
// Config Server with encryption
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    
    @Bean
    public TextEncryptor textEncryptor() {
        return new BasicTextEncryptor() {{
            setPassword(System.getenv("CONFIG_ENCRYPT_KEY"));
        }};
    }
}

// Encrypted properties
# application-prod.yml
spring:
  datasource:
    password: '{cipher}AQA...' # Encrypted value
    
app:
  api-key: '{cipher}BQB...'   # Encrypted API key

// Client with profile-specific config
@Component
@ConfigurationProperties(prefix = "app")
@RefreshScope
public class AppConfig {
    
    private String apiKey;
    private DatabaseConfig database;
    private FeatureFlags features;
    
    @PostConstruct
    public void init() {
        log.info("Loaded configuration for profile: {}", 
            environment.getActiveProfiles());
    }
}

// Dynamic configuration updates
@EventListener
public void handleRefreshEvent(RefreshRemoteApplicationEvent event) {
    log.info("Configuration refreshed for services: {}", event.getDestinationService());
    
    // Notify dependent services
    applicationEventPublisher.publishEvent(
        new ConfigurationUpdatedEvent(event.getDestinationService())
    );
}
```

## Best Practices

### Service Design

```java
// Good microservice design
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        Order order = orderService.createOrder(request, userId);
        OrderResponse response = OrderMapper.toResponse(order);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("Location", "/api/v1/orders/" + order.getId())
            .body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return orderService.getOrder(id)
            .map(OrderMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// Service layer with proper error handling
@Service
@Transactional
public class OrderService {
    
    public Order createOrder(CreateOrderRequest request, String userId) {
        try {
            // Validate business rules
            validateOrderRequest(request);
            
            // Check inventory (with circuit breaker)
            inventoryService.reserveItems(request.getItems());
            
            // Process payment (with retry)
            PaymentResult payment = paymentService.processPayment(request.getPayment());
            
            // Create order
            Order order = buildOrder(request, userId, payment);
            Order savedOrder = orderRepository.save(order);
            
            // Publish event
            eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));
            
            return savedOrder;
            
        } catch (InsufficientInventoryException e) {
            throw new BadRequestException("Insufficient inventory", e);
        } catch (PaymentException e) {
            throw new PaymentProcessingException("Payment failed", e);
        }
    }
}
```

### Configuration Management

```yaml
# Proper configuration structure
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  cloud:
    config:
      uri: ${CONFIG_SERVER_URL:http://localhost:8888}
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6

# Environment-specific configurations
---
spring:
  profiles: dev
  
app:
  database:
    max-connections: 10
  features:
    new-checkout: true
  external-services:
    payment-service:
      url: http://localhost:8082
      timeout: 5s

---
spring:
  profiles: prod
  
app:
  database:
    max-connections: 50
  features:
    new-checkout: false
  external-services:
    payment-service:
      url: https://payment-service.prod.com
      timeout: 10s
```

### Monitoring and Observability

```java
@Component
public class ServiceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCreatedCounter;
    private final Timer orderProcessingTimer;
    
    public ServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .register(meterRegistry);
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(meterRegistry);
    }
    
    public void recordOrderCreated(String userId, String orderType) {
        orderCreatedCounter.increment(
            Tags.of(
                Tag.of("user.id", userId),
                Tag.of("order.type", orderType)
            )
        );
    }
    
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }
}

// Distributed tracing best practices
@Service
public class OrderService {
    
    @NewSpan("create-order")
    public Order createOrder(@SpanTag("userId") String userId, 
                           CreateOrderRequest request) {
        
        Span currentSpan = tracer.currentSpan();
        currentSpan.tag("order.items.count", String.valueOf(request.getItems().size()));
        
        try {
            Order order = processOrder(request);
            currentSpan.tag("order.id", order.getId().toString());
            return order;
        } catch (Exception e) {
            currentSpan.tag("error", e.getMessage());
            throw e;
        }
    }
}
```

This comprehensive Spring Cloud guide covers microservices architecture, service discovery, API gateway, configuration management, circuit breakers, load balancing, and distributed tracing with practical examples and best practices.