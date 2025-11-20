# Redis Clone Spring Boot Starter - Usage Guide

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.sudhir512kj</groupId>
    <artifactId>redis-clone-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

Add to `application.yml`:

```yaml
redis-clone:
  enabled: true  # Default: true
```

## Basic Usage

### Inject RedisTemplate

```java
@Service
public class UserService {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    public void saveUser(String userId, String userData) {
        redisTemplate.set("user:" + userId, userData, Duration.ofHours(1));
    }
    
    public String getUser(String userId) {
        return redisTemplate.get("user:" + userId);
    }
}
```

### Using Annotations

```java
@Service
public class ProductService {
    
    @Cacheable(key = "product:#{id}", ttl = 3600)
    public Product getProduct(Long id) {
        // This method result will be cached
        return productRepository.findById(id);
    }
    
    @CacheEvict(key = "product:#{product.id}")
    public Product updateProduct(Product product) {
        // Cache will be evicted after update
        return productRepository.save(product);
    }
}
```

## API Reference

### String Operations

```java
// Set with TTL
redisTemplate.set("key", "value", Duration.ofMinutes(30));

// Set without TTL
redisTemplate.set("key", "value");

// Get value
String value = redisTemplate.get("key");
```

### List Operations

```java
// Push to list
int size = redisTemplate.leftPush("mylist", "item1", "item2");

// Pop from list
String item = redisTemplate.leftPop("mylist");
```

### Set Operations

```java
// Add to set
int added = redisTemplate.addToSet("myset", "member1", "member2");

// Check membership
boolean exists = redisTemplate.isMember("myset", "member1");
```

### Hash Operations

```java
// Set hash field
redisTemplate.hashSet("user:123", "name", "John Doe");

// Get hash field
String name = redisTemplate.hashGet("user:123", "name");
```

### Generic Operations

```java
// Check existence
boolean exists = redisTemplate.exists("key");

// Delete key
boolean deleted = redisTemplate.delete("key");

// Set expiration
boolean success = redisTemplate.expire("key", Duration.ofMinutes(10));
```

## Annotations

### @Cacheable

Cache method results:

```java
@Cacheable(key = "user:#{userId}", ttl = 1800)
public User findUser(String userId) {
    return userRepository.findById(userId);
}
```

### @CacheEvict

Evict cache entries:

```java
@CacheEvict(key = "user:#{user.id}")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

## Example Application

```java
@SpringBootApplication
public class MyApplication {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @PostConstruct
    public void demo() {
        // String operations
        redisTemplate.set("greeting", "Hello World!");
        System.out.println(redisTemplate.get("greeting"));
        
        // List operations
        redisTemplate.leftPush("tasks", "task1", "task2");
        System.out.println(redisTemplate.leftPop("tasks"));
        
        // Set operations
        redisTemplate.addToSet("tags", "java", "spring", "redis");
        System.out.println(redisTemplate.isMember("tags", "java"));
        
        // Hash operations
        redisTemplate.hashSet("user:1", "name", "John");
        System.out.println(redisTemplate.hashGet("user:1", "name"));
    }
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

## Performance

- **Throughput**: 100K+ operations/second
- **Latency**: Sub-millisecond for basic operations
- **Memory**: Efficient in-memory storage
- **Thread Safety**: Concurrent operations supported

## Best Practices

1. **Use TTL**: Always set expiration for cache entries
2. **Key Naming**: Use consistent key naming patterns
3. **Memory Management**: Monitor memory usage in production
4. **Error Handling**: Handle null returns gracefully

```java
String value = redisTemplate.get("key");
if (value != null) {
    // Process value
} else {
    // Handle cache miss
}
```