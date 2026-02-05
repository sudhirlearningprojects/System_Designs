# Spring Cloud - Microservices Architecture

[← Back to Index](README.md) | [← Previous: Spring Security](08_Spring_Security.md) | [Next: Spring Kafka →](10_Spring_Kafka.md)

## Table of Contents
- [Service Discovery (Eureka)](#service-discovery-eureka)
- [Load Balancing (Feign)](#load-balancing-feign)
- [Circuit Breaker (Resilience4j)](#circuit-breaker-resilience4j)
- [API Gateway](#api-gateway)

---

## Service Discovery (Eureka)

### Eureka Server
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```properties
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### Eureka Client
```java
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

```properties
spring.application.name=user-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

---

## Load Balancing (Feign)

```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable Long id);
    
    @PostMapping("/api/users")
    User createUser(@RequestBody User user);
}

@Service
public class OrderService {
    @Autowired
    private UserServiceClient userServiceClient;
    
    public Order createOrder(Long userId, Order order) {
        User user = userServiceClient.getUser(userId);
        order.setUser(user);
        return orderRepository.save(order);
    }
}
```

---

## Circuit Breaker (Resilience4j)

```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService")
    public PaymentResponse processPayment(Payment payment) {
        return paymentGateway.charge(payment);
    }
    
    private PaymentResponse paymentFallback(Payment payment, Exception e) {
        return new PaymentResponse("FAILED", "Service unavailable");
    }
}
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
  
  retry:
    instances:
      paymentService:
        max-attempts: 3
        wait-duration: 1000
```

---

## API Gateway

```java
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r.path("/api/users/**")
                .filters(f -> f.addRequestHeader("X-Gateway", "true"))
                .uri("lb://user-service"))
            
            .route("order-service", r -> r.path("/api/orders/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("orderService")))
                .uri("lb://order-service"))
            
            .build();
    }
}
```

---

[← Previous: Spring Security](08_Spring_Security.md) | [Next: Spring Kafka →](10_Spring_Kafka.md)
