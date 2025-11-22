# Getting Started with Spring WebFlux

## Project Setup

### Maven Dependencies

```xml
<dependencies>
    <!-- Spring WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Reactive Data Access -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>
    
    <!-- PostgreSQL R2DBC Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Application Configuration

```yaml
# application.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/reactive_db
    username: postgres
    password: password
  
  webflux:
    base-path: /api
  
server:
  port: 8080
  
logging:
  level:
    org.springframework.r2dbc: DEBUG
```

## First Reactive Application

### 1. Domain Model

```java
@Data
@Table("users")
public class User {
    @Id
    private Long id;
    
    @NotBlank
    private String username;
    
    @Email
    private String email;
    
    private LocalDateTime createdAt;
}
```

### 2. Repository

```java
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    
    Mono<User> findByUsername(String username);
    
    Flux<User> findByEmailContaining(String email);
    
    @Query("SELECT * FROM users WHERE created_at > :date")
    Flux<User> findRecentUsers(LocalDateTime date);
}
```

### 3. Service Layer

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public Mono<User> createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    public Mono<User> getUserById(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }
    
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Mono<User> updateUser(Long id, User user) {
        return userRepository.findById(id)
            .flatMap(existing -> {
                existing.setUsername(user.getUsername());
                existing.setEmail(user.getEmail());
                return userRepository.save(existing);
            })
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }
    
    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }
}
```

### 4. Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }
    
    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @PutMapping("/{id}")
    public Mono<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        return userService.updateUser(id, user);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
}
```

### 5. Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(WebExchangeBindException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

## Database Schema

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

## Running the Application

```bash
# Start PostgreSQL
docker run -d \
  --name postgres-reactive \
  -e POSTGRES_DB=reactive_db \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:14

# Run application
mvn spring-boot:run
```

## Testing with cURL

```bash
# Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com"}'

# Get user
curl http://localhost:8080/api/users/1

# Get all users
curl http://localhost:8080/api/users

# Update user
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"john_updated","email":"john.new@example.com"}'

# Delete user
curl -X DELETE http://localhost:8080/api/users/1
```

## Key Takeaways

1. **Non-blocking**: All operations return Mono or Flux
2. **Reactive Repositories**: Use ReactiveCrudRepository
3. **Error Handling**: Use switchIfEmpty() and onErrorResume()
4. **Validation**: Works with @Valid annotation
5. **Testing**: Use WebTestClient for integration tests

## Next Steps

- [Annotated Controllers](Annotated_Controllers.md) - Deep dive into controllers
- [Functional Endpoints](Functional_Endpoints.md) - Alternative programming model
- [WebClient](WebClient.md) - Making reactive HTTP calls
