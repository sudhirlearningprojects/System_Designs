# Service Discovery & Service Registry Annotations in Java

## Overview
Service Discovery and Service Registry are fundamental patterns in microservices architecture that enable services to find and communicate with each other dynamically without hardcoding network locations.

---

## Table of Contents
1. [Core Concepts](#core-concepts)
2. [Spring Cloud Netflix Eureka](#spring-cloud-netflix-eureka)
3. [Spring Cloud Consul](#spring-cloud-consul)
4. [Load Balancing Annotations](#load-balancing-annotations)
5. [Feign Client](#feign-client)
6. [Service Instance Metadata](#service-instance-metadata)
7. [Health Check Annotations](#health-check-annotations)
8. [Discovery Client Usage](#discovery-client-usage)
9. [Complete Examples](#complete-examples)
10. [Configuration](#configuration)

---

## Core Concepts

### Service Registry
A centralized database of service instances and their locations. Popular implementations:
- **Netflix Eureka**
- **HashiCorp Consul**
- **Apache Zookeeper**
- **etcd**

### Service Discovery
The mechanism by which services locate other services in the network:
- **Client-Side Discovery**: Client queries registry and chooses instance
- **Server-Side Discovery**: Load balancer queries registry

---

## Spring Cloud Netflix Eureka

### 1. Enable Eureka Server (Service Registry)

```java
package org.sudhir512kj.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer  // Enables Eureka Server as Service Registry
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**application.yml for Eureka Server:**
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false  # Don't register itself
    fetch-registry: false         # Don't fetch registry
  server:
    enable-self-preservation: true
```

---

### 2. Enable Eureka Client (Service Registration)

```java
package org.sudhir512kj.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient  // Registers this service with Eureka Server
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

---

### 3. Generic Discovery Client

```java
package org.sudhir512kj.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // Generic - works with Eureka, Consul, Zookeeper
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

**Key Difference:**
- `@EnableEurekaClient`: Specific to Eureka
- `@EnableDiscoveryClient`: Generic, works with any registry

---

## Spring Cloud Consul

### Enable Consul Discovery

```java
package org.sudhir512kj.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // Registers with Consul
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
```

**application.yml for Consul:**
```yaml
spring:
  application:
    name: inventory-service
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        health-check-path: /actuator/health
        health-check-interval: 10s
```

---

## Load Balancing Annotations

### 1. @LoadBalanced with RestTemplate

```java
package org.sudhir512kj.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced  // Enables client-side load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Usage:**
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @PostMapping
    public OrderDTO createOrder(@RequestBody OrderRequest request) {
        // Use service name instead of hardcoded URL
        String url = "http://payment-service/api/payments";
        PaymentDTO payment = restTemplate.postForObject(url, request, PaymentDTO.class);
        return new OrderDTO(payment);
    }
}
```

---

### 2. @LoadBalanced with WebClient

```java
package org.sudhir512kj.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced  // Enables reactive load balancing
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

**Usage:**
```java
@Service
public class OrderService {
    
    private final WebClient webClient;
    
    public OrderService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    public Mono<PaymentDTO> processPayment(PaymentRequest request) {
        return webClient.post()
            .uri("http://payment-service/api/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentDTO.class);
    }
}
```

---

## Feign Client

### 1. Enable Feign Clients

```java
package org.sudhir512kj.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients  // Enables Feign client scanning
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

### 2. Define Feign Client Interface

```java
package org.sudhir512kj.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "payment-service")  // Service name from registry
public interface PaymentClient {
    
    @GetMapping("/api/payments/{id}")
    PaymentDTO getPayment(@PathVariable("id") String id);
    
    @PostMapping("/api/payments")
    PaymentDTO createPayment(@RequestBody PaymentRequest request);
    
    @PutMapping("/api/payments/{id}/status")
    void updatePaymentStatus(@PathVariable("id") String id, 
                            @RequestParam("status") String status);
}
```

---

### 3. Feign Client with Fallback

```java
package org.sudhir512kj.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "payment-service",
    fallback = PaymentClientFallback.class  // Fallback on failure
)
public interface PaymentClient {
    
    @PostMapping("/api/payments")
    PaymentDTO createPayment(@RequestBody PaymentRequest request);
}

// Fallback implementation
@Component
public class PaymentClientFallback implements PaymentClient {
    
    @Override
    public PaymentDTO createPayment(PaymentRequest request) {
        // Return default response or throw exception
        return PaymentDTO.builder()
            .status("PENDING")
            .message("Payment service unavailable")
            .build();
    }
}
```

---

### 4. Feign Client with Custom Configuration

```java
@FeignClient(
    name = "payment-service",
    configuration = PaymentClientConfig.class,
    fallback = PaymentClientFallback.class
)
public interface PaymentClient {
    @PostMapping("/api/payments")
    PaymentDTO createPayment(@RequestBody PaymentRequest request);
}

@Configuration
public class PaymentClientConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // Log all request/response
    }
    
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);  // Retry logic
    }
}
```

---

## Service Instance Metadata

### Add Custom Metadata to Service Instance

```java
package org.sudhir512kj.payment.config;

import com.netflix.appinfo.EurekaInstanceConfig;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EurekaConfig {
    
    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfig() {
        EurekaInstanceConfigBean config = new EurekaInstanceConfigBean();
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "us-east-1a");
        metadata.put("version", "v1.0.0");
        metadata.put("environment", "production");
        metadata.put("team", "payments");
        
        config.setMetadataMap(metadata);
        return config;
    }
}
```

**Access Metadata:**
```java
@Service
public class ServiceDiscoveryService {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    public void printServiceMetadata(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        for (ServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            System.out.println("Zone: " + metadata.get("zone"));
            System.out.println("Version: " + metadata.get("version"));
        }
    }
}
```

---

## Health Check Annotations

### 1. Custom Health Indicator

```java
package org.sudhir512kj.payment.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Custom health check logic
        boolean isHealthy = checkDatabaseConnection() && checkExternalAPI();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("service", "payment-service")
                .withDetail("status", "running")
                .withDetail("database", "connected")
                .build();
        } else {
            return Health.down()
                .withDetail("service", "payment-service")
                .withDetail("status", "degraded")
                .withDetail("error", "Database connection failed")
                .build();
        }
    }
    
    private boolean checkDatabaseConnection() {
        // Check database connectivity
        return true;
    }
    
    private boolean checkExternalAPI() {
        // Check external payment gateway
        return true;
    }
}
```

---

### 2. Spring Boot Actuator Health Endpoint

```java
// Add dependency in pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

**Access:** `http://localhost:8080/actuator/health`

---

This is Part 1 of the document. Part 2 will continue with Discovery Client Usage, Complete Examples, and Configuration.

## Discovery Client Usage

### 1. Inject and Use DiscoveryClient

```java
package org.sudhir512kj.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceDiscoveryService {
    
    @Autowired
    private DiscoveryClient discoveryClient;  // Inject discovery client
    
    // Get all instances of a service
    public List<ServiceInstance> getServiceInstances(String serviceName) {
        return discoveryClient.getInstances(serviceName);
    }
    
    // Get all registered services
    public List<String> getAllServices() {
        return discoveryClient.getServices();
    }
    
    // Get specific instance details
    public void printInstanceDetails(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        for (ServiceInstance instance : instances) {
            System.out.println("Instance ID: " + instance.getInstanceId());
            System.out.println("Host: " + instance.getHost());
            System.out.println("Port: " + instance.getPort());
            System.out.println("URI: " + instance.getUri());
            System.out.println("Metadata: " + instance.getMetadata());
        }
    }
    
    // Manual load balancing
    public ServiceInstance chooseInstance(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            throw new RuntimeException("No instances available for " + serviceName);
        }
        // Simple round-robin (use proper load balancer in production)
        return instances.get(0);
    }
}
```

---

### 2. REST Controller with Discovery Client

```java
package org.sudhir512kj.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @GetMapping("/services")
    public List<String> getAllServices() {
        return discoveryClient.getServices();
    }
    
    @GetMapping("/services/{serviceName}/instances")
    public List<ServiceInstance> getInstances(@PathVariable String serviceName) {
        return discoveryClient.getInstances(serviceName);
    }
}
```

---

## Complete Examples

### Example 1: Order Service with Multiple Integrations

```java
package org.sudhir512kj.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient      // Register with service registry
@EnableFeignClients         // Enable Feign clients
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    
    @Bean
    @LoadBalanced           // Enable load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

### Example 2: Order Controller with All Integration Methods

```java
package org.sudhir512kj.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private RestTemplate restTemplate;  // Load-balanced RestTemplate
    
    @Autowired
    private PaymentClient paymentClient;  // Feign client
    
    @Autowired
    private InventoryClient inventoryClient;  // Feign client
    
    @Autowired
    private DiscoveryClient discoveryClient;  // Discovery client
    
    // Method 1: Using RestTemplate with service name
    @PostMapping("/method1")
    public OrderDTO createOrderWithRestTemplate(@RequestBody OrderRequest request) {
        // Check inventory
        String inventoryUrl = "http://inventory-service/api/inventory/check";
        Boolean available = restTemplate.postForObject(inventoryUrl, request, Boolean.class);
        
        if (!available) {
            throw new RuntimeException("Product not available");
        }
        
        // Process payment
        String paymentUrl = "http://payment-service/api/payments";
        PaymentDTO payment = restTemplate.postForObject(paymentUrl, request, PaymentDTO.class);
        
        return OrderDTO.builder()
            .orderId("ORD-123")
            .paymentId(payment.getPaymentId())
            .status("CONFIRMED")
            .build();
    }
    
    // Method 2: Using Feign clients
    @PostMapping("/method2")
    public OrderDTO createOrderWithFeign(@RequestBody OrderRequest request) {
        // Check inventory using Feign
        Boolean available = inventoryClient.checkAvailability(request.getProductId());
        
        if (!available) {
            throw new RuntimeException("Product not available");
        }
        
        // Process payment using Feign
        PaymentDTO payment = paymentClient.createPayment(
            PaymentRequest.builder()
                .amount(request.getAmount())
                .currency("USD")
                .build()
        );
        
        return OrderDTO.builder()
            .orderId("ORD-124")
            .paymentId(payment.getPaymentId())
            .status("CONFIRMED")
            .build();
    }
    
    // Method 3: Manual service discovery
    @PostMapping("/method3")
    public OrderDTO createOrderManual(@RequestBody OrderRequest request) {
        // Get payment service instance manually
        List<ServiceInstance> instances = discoveryClient.getInstances("payment-service");
        
        if (instances.isEmpty()) {
            throw new RuntimeException("Payment service not available");
        }
        
        ServiceInstance instance = instances.get(0);
        String paymentUrl = instance.getUri() + "/api/payments";
        
        // Call without load balancing
        RestTemplate simpleRestTemplate = new RestTemplate();
        PaymentDTO payment = simpleRestTemplate.postForObject(paymentUrl, request, PaymentDTO.class);
        
        return OrderDTO.builder()
            .orderId("ORD-125")
            .paymentId(payment.getPaymentId())
            .status("CONFIRMED")
            .build();
    }
    
    // Get all registered services
    @GetMapping("/services")
    public List<String> getAllServices() {
        return discoveryClient.getServices();
    }
    
    // Get instances of a specific service
    @GetMapping("/services/{serviceName}/instances")
    public List<ServiceInstance> getServiceInstances(@PathVariable String serviceName) {
        return discoveryClient.getInstances(serviceName);
    }
}
```

---

### Example 3: Feign Client Definitions

```java
package org.sudhir512kj.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// Payment Service Client
@FeignClient(
    name = "payment-service",
    fallback = PaymentClientFallback.class
)
public interface PaymentClient {
    
    @GetMapping("/api/payments/{id}")
    PaymentDTO getPayment(@PathVariable("id") String id);
    
    @PostMapping("/api/payments")
    PaymentDTO createPayment(@RequestBody PaymentRequest request);
    
    @PutMapping("/api/payments/{id}/refund")
    void refundPayment(@PathVariable("id") String id);
}

// Inventory Service Client
@FeignClient(
    name = "inventory-service",
    fallback = InventoryClientFallback.class
)
public interface InventoryClient {
    
    @GetMapping("/api/inventory/check/{productId}")
    Boolean checkAvailability(@PathVariable("productId") String productId);
    
    @PostMapping("/api/inventory/reserve")
    void reserveInventory(@RequestBody InventoryRequest request);
    
    @PostMapping("/api/inventory/release")
    void releaseInventory(@RequestBody InventoryRequest request);
}

// Notification Service Client
@FeignClient(name = "notification-service")
public interface NotificationClient {
    
    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);
}
```

---

## Spring Cloud LoadBalancer (Modern)

### 1. Configuration

```java
package org.sudhir512kj.order.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@LoadBalancerClient(name = "payment-service", configuration = LoadBalancerConfig.class)
public class LoadBalancerConfiguration {
}

class LoadBalancerConfig {
    
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withHealthChecks()  // Filter unhealthy instances
            .build(context);
    }
}
```

---

### 2. Custom Load Balancing Strategy

```java
package org.sudhir512kj.order.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@LoadBalancerClient(name = "payment-service", configuration = CustomLoadBalancerConfig.class)
public class CustomLoadBalancerConfiguration {
}

class CustomLoadBalancerConfig {
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name
        );
    }
}
```

---

## Configuration Files

### 1. Eureka Client Configuration

**application.yml:**
```yaml
spring:
  application:
    name: order-service

server:
  port: 8084

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
    registry-fetch-interval-seconds: 30
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
    metadata-map:
      zone: us-east-1a
      version: v1.0.0
      environment: production

# Feign configuration
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: full
  circuitbreaker:
    enabled: true

# Actuator for health checks
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

---

### 2. Consul Client Configuration

**application.yml:**
```yaml
spring:
  application:
    name: order-service
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        register: true
        instance-id: ${spring.application.name}:${random.value}
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30s
        prefer-ip-address: true
        tags:
          - version=v1.0.0
          - environment=production

server:
  port: 8084

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

### 3. Multiple Service Registry Configuration

**application.yml:**
```yaml
spring:
  application:
    name: order-service
  cloud:
    # Eureka
    eureka:
      client:
        enabled: true
        service-url:
          defaultZone: http://localhost:8761/eureka/
    
    # Consul
    consul:
      enabled: false
      host: localhost
      port: 8500

# Profile-specific configuration
---
spring:
  config:
    activate:
      on-profile: consul

  cloud:
    eureka:
      client:
        enabled: false
    consul:
      enabled: true
```

---

## Maven Dependencies

**pom.xml:**
```xml
<dependencies>
    <!-- Spring Cloud Starter -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter</artifactId>
    </dependency>
    
    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Eureka Server (for registry service) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    
    <!-- Consul Discovery -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-consul-discovery</artifactId>
    </dependency>
    
    <!-- OpenFeign -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    
    <!-- Load Balancer -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
    
    <!-- Actuator for health checks -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Circuit Breaker (Resilience4j) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Key Annotations Summary

| Annotation | Purpose | Framework | Usage |
|------------|---------|-----------|-------|
| `@EnableEurekaServer` | Enable Eureka registry server | Spring Cloud Netflix | Registry service |
| `@EnableEurekaClient` | Register with Eureka (Eureka-specific) | Spring Cloud Netflix | Microservice |
| `@EnableDiscoveryClient` | Register with any registry (generic) | Spring Cloud | Microservice |
| `@LoadBalanced` | Enable client-side load balancing | Spring Cloud | RestTemplate/WebClient |
| `@FeignClient` | Declarative REST client | Spring Cloud OpenFeign | Interface |
| `@EnableFeignClients` | Enable Feign client scanning | Spring Cloud OpenFeign | Main application |
| `@LoadBalancerClient` | Configure Spring Cloud LoadBalancer | Spring Cloud | Configuration class |
| `@RibbonClient` | Configure Ribbon (deprecated) | Spring Cloud Netflix | Configuration class |

---

## Comparison: Eureka vs Consul vs Zookeeper

| Feature | Eureka | Consul | Zookeeper |
|---------|--------|--------|-----------|
| **CAP Theorem** | AP (Availability + Partition tolerance) | CP (Consistency + Partition tolerance) | CP |
| **Health Checks** | Client heartbeat | HTTP/TCP/Script | Ephemeral nodes |
| **Service Discovery** | REST API | REST API + DNS | Native client |
| **Load Balancing** | Client-side (Ribbon) | Client-side | Client-side |
| **Multi-DC Support** | Limited | Excellent | Good |
| **UI Dashboard** | Yes | Yes | No (third-party) |
| **Language Support** | Java-focused | Language agnostic | Language agnostic |
| **Complexity** | Low | Medium | High |

---

## Best Practices

### 1. Use Generic Annotations
```java
// Prefer this (works with any registry)
@EnableDiscoveryClient

// Over this (Eureka-specific)
@EnableEurekaClient
```

### 2. Configure Health Checks
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 30  # Heartbeat interval
    lease-expiration-duration-in-seconds: 90  # Eviction timeout
```

### 3. Use Feign with Fallbacks
```java
@FeignClient(name = "payment-service", fallback = PaymentClientFallback.class)
public interface PaymentClient {
    // Methods
}
```

### 4. Enable Circuit Breaker
```yaml
feign:
  circuitbreaker:
    enabled: true
```

### 5. Configure Timeouts
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
```

---

## Modern Stack Recommendation (2024)

✅ **Service Registry**: Spring Cloud Consul or Eureka  
✅ **Service Discovery**: `@EnableDiscoveryClient`  
✅ **Load Balancing**: Spring Cloud LoadBalancer (not Ribbon - deprecated)  
✅ **REST Client**: OpenFeign with `@FeignClient`  
✅ **Health Checks**: Spring Boot Actuator  
✅ **Circuit Breaker**: Resilience4j with `@CircuitBreaker`  
✅ **API Gateway**: Spring Cloud Gateway  
✅ **Configuration**: Spring Cloud Config Server  

---

## Troubleshooting

### Issue 1: Service Not Registering
**Solution:**
```yaml
eureka:
  client:
    register-with-eureka: true  # Ensure this is true
    fetch-registry: true
```

### Issue 2: Load Balancing Not Working
**Solution:**
```java
// Ensure @LoadBalanced annotation is present
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

### Issue 3: Feign Client Not Found
**Solution:**
```java
// Add @EnableFeignClients to main application
@SpringBootApplication
@EnableFeignClients(basePackages = "org.sudhir512kj.order.client")
public class OrderServiceApplication {
    // ...
}
```

---

## References

- [Spring Cloud Netflix Documentation](https://spring.io/projects/spring-cloud-netflix)
- [Spring Cloud Consul Documentation](https://spring.io/projects/spring-cloud-consul)
- [Spring Cloud OpenFeign Documentation](https://spring.io/projects/spring-cloud-openfeign)
- [Spring Cloud LoadBalancer Documentation](https://spring.io/projects/spring-cloud-commons)
- [Eureka Wiki](https://github.com/Netflix/eureka/wiki)
- [HashiCorp Consul](https://www.consul.io/)

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: System Designs Collection
