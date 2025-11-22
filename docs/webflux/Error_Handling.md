# Error Handling in Spring WebFlux

## Overview

Reactive error handling requires different approaches than traditional Spring MVC due to the asynchronous nature of reactive streams.

## Error Handling Strategies

### 1. onErrorReturn

Returns a fallback value when an error occurs.

```java
public Mono<User> getUserById(Long id) {
    return userRepository.findById(id)
        .onErrorReturn(new User()); // Return empty user on error
}

// With condition
public Mono<User> getUserById(Long id) {
    return userRepository.findById(id)
        .onErrorReturn(DatabaseException.class, new User());
}
```

### 2. onErrorResume

Provides an alternative Mono/Flux when an error occurs.

```java
public Mono<User> getUserById(Long id) {
    return userRepository.findById(id)
        .onErrorResume(e -> {
            log.error("Error fetching user: {}", e.getMessage());
            return Mono.error(new UserNotFoundException(id));
        });
}

// With fallback service
public Mono<User> getUserById(Long id) {
    return primaryUserService.findById(id)
        .onErrorResume(e -> fallbackUserService.findById(id));
}
```

### 3. onErrorMap

Transforms one error into another.

```java
public Mono<User> getUserById(Long id) {
    return userRepository.findById(id)
        .onErrorMap(R2dbcException.class, e -> 
            new DatabaseException("Database error: " + e.getMessage(), e));
}
```

### 4. onErrorContinue

Continues processing remaining elements after an error.

```java
public Flux<User> processUsers(List<Long> userIds) {
    return Flux.fromIterable(userIds)
        .flatMap(userRepository::findById)
        .onErrorContinue((error, userId) -> 
            log.error("Failed to process user {}: {}", userId, error.getMessage()));
}
```

### 5. doOnError

Performs side effects when an error occurs without handling it.

```java
public Mono<User> getUserById(Long id) {
    return userRepository.findById(id)
        .doOnError(e -> log.error("Error fetching user {}: {}", id, e.getMessage()));
}
```

## Exception Hierarchy

```java
// Base exception
public class ApplicationException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;
    
    public ApplicationException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}

// Specific exceptions
public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(Long id) {
        super("User not found: " + id, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}

public class ValidationException extends ApplicationException {
    private final Map<String, String> errors;
    
    public ValidationException(Map<String, String> errors) {
        super("Validation failed", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }
}

public class DatabaseException extends ApplicationException {
    public DatabaseException(String message, Throwable cause) {
        super(message, "DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
```

## Global Exception Handler

### Using @RestControllerAdvice

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserNotFound(UserNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }
    
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ValidationErrorResponse>> handleValidation(ValidationException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        ValidationErrorResponse error = ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .errors(ex.getErrors())
            .build();
        
        return Mono.just(ResponseEntity.badRequest().body(error));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ValidationErrorResponse>> handleBindException(
        WebExchangeBindException ex
    ) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
            ));
        
        ValidationErrorResponse error = ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Validation failed")
            .errors(errors)
            .build();
        
        return Mono.just(ResponseEntity.badRequest().body(error));
    }
    
    @ExceptionHandler(DatabaseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDatabase(DatabaseException ex) {
        log.error("Database error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("A database error occurred")
            .errorCode(ex.getErrorCode())
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}
```

### Error Response DTOs

```java
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String path;
}

@Data
@Builder
public class ValidationErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;
}
```

## Custom Error WebExceptionHandler

```java
@Component
@Order(-2) // Higher priority than DefaultErrorWebExceptionHandler
public class CustomErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
    
    public CustomErrorWebExceptionHandler(
        ErrorAttributes errorAttributes,
        ResourceProperties resourceProperties,
        ApplicationContext applicationContext,
        ServerCodecConfigurer configurer
    ) {
        super(errorAttributes, resourceProperties, applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }
    
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }
    
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        
        if (error instanceof UserNotFoundException) {
            return handleUserNotFound((UserNotFoundException) error);
        } else if (error instanceof ValidationException) {
            return handleValidation((ValidationException) error);
        } else if (error instanceof DatabaseException) {
            return handleDatabase((DatabaseException) error);
        } else {
            return handleGeneric(error);
        }
    }
    
    private Mono<ServerResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .build();
        
        return ServerResponse.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(error);
    }
    
    private Mono<ServerResponse> handleValidation(ValidationException ex) {
        ValidationErrorResponse error = ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .errors(ex.getErrors())
            .build();
        
        return ServerResponse.badRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(error);
    }
    
    private Mono<ServerResponse> handleDatabase(DatabaseException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("A database error occurred")
            .errorCode(ex.getErrorCode())
            .build();
        
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(error);
    }
    
    private Mono<ServerResponse> handleGeneric(Throwable ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .build();
        
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(error);
    }
}
```

## Service Layer Error Handling

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final Validator validator;
    
    public Mono<User> createUser(User user) {
        return validateUser(user)
            .flatMap(userRepository::save)
            .onErrorMap(R2dbcDataIntegrityViolationException.class, e ->
                new ValidationException(Map.of("username", "Username already exists")))
            .doOnError(e -> log.error("Error creating user: {}", e.getMessage()));
    }
    
    public Mono<User> getUserById(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
            .onErrorMap(R2dbcException.class, e ->
                new DatabaseException("Error fetching user", e));
    }
    
    public Mono<User> updateUser(Long id, User user) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
            .flatMap(existing -> {
                existing.setUsername(user.getUsername());
                existing.setEmail(user.getEmail());
                return validateUser(existing);
            })
            .flatMap(userRepository::save)
            .onErrorMap(R2dbcException.class, e ->
                new DatabaseException("Error updating user", e));
    }
    
    private Mono<User> validateUser(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            Map<String, String> errors = violations.stream()
                .collect(Collectors.toMap(
                    v -> v.getPropertyPath().toString(),
                    ConstraintViolation::getMessage
                ));
            return Mono.error(new ValidationException(errors));
        }
        return Mono.just(user);
    }
}
```

## WebClient Error Handling

```java
@Service
@RequiredArgsConstructor
public class ExternalUserService {
    
    private final WebClient webClient;
    
    public Mono<User> getExternalUser(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response ->
                response.bodyToMono(String.class)
                    .flatMap(body -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new UserNotFoundException(id));
                        }
                        return Mono.error(new ExternalServiceException(body));
                    }))
            .onStatus(HttpStatus::is5xxServerError, response ->
                Mono.error(new ExternalServiceException("External service error")))
            .bodyToMono(User.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorMap(TimeoutException.class, e ->
                new ExternalServiceException("Request timeout"))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof WebClientException));
    }
}
```

## Timeout Handling

```java
public Mono<User> getUserWithTimeout(Long id) {
    return userRepository.findById(id)
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(TimeoutException.class, e ->
            new ServiceUnavailableException("Request timeout"));
}

// With fallback
public Mono<User> getUserWithTimeoutAndFallback(Long id) {
    return userRepository.findById(id)
        .timeout(Duration.ofSeconds(5), Mono.just(new User()))
        .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
}
```

## Retry Logic

```java
public Mono<User> getUserWithRetry(Long id) {
    return userRepository.findById(id)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof TransientException)
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                new ServiceUnavailableException("Max retries exceeded")));
}

// Custom retry
public Mono<User> getUserWithCustomRetry(Long id) {
    return userRepository.findById(id)
        .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
            .filter(throwable -> !(throwable instanceof UserNotFoundException))
            .doBeforeRetry(signal -> 
                log.warn("Retrying... attempt: {}", signal.totalRetries() + 1)));
}
```

## Circuit Breaker

```java
@Service
@RequiredArgsConstructor
public class ResilientUserService {
    
    private final UserRepository userRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<User> getUserById(Long id) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        
        return userRepository.findById(id)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(CallNotPermittedException.class, e ->
                Mono.error(new ServiceUnavailableException("Circuit breaker open")));
    }
}

// Configuration
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();
        
        return CircuitBreakerRegistry.of(config);
    }
}
```

## Validation

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final Validator validator;
    
    public Mono<User> createUser(User user) {
        return Mono.just(user)
            .flatMap(this::validate)
            .flatMap(userRepository::save);
    }
    
    private Mono<User> validate(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        if (!violations.isEmpty()) {
            Map<String, String> errors = violations.stream()
                .collect(Collectors.toMap(
                    v -> v.getPropertyPath().toString(),
                    ConstraintViolation::getMessage
                ));
            return Mono.error(new ValidationException(errors));
        }
        
        return Mono.just(user);
    }
}
```

## Best Practices

1. **Use specific exceptions**: Create custom exception hierarchy
2. **Handle errors at appropriate level**: Service layer vs controller
3. **Log errors properly**: Include context and stack traces
4. **Return meaningful messages**: Help clients understand errors
5. **Use proper HTTP status codes**: 4xx for client errors, 5xx for server errors
6. **Implement retry logic**: For transient failures
7. **Use circuit breakers**: Prevent cascading failures
8. **Set timeouts**: Avoid hanging requests
9. **Validate input**: Early validation prevents errors
10. **Test error scenarios**: Include error cases in tests

## Common Patterns

### Fallback Pattern

```java
public Mono<User> getUserWithFallback(Long id) {
    return primaryService.getUser(id)
        .onErrorResume(e -> secondaryService.getUser(id))
        .onErrorResume(e -> Mono.just(getDefaultUser()));
}
```

### Fail-Fast Pattern

```java
public Mono<User> getUserFailFast(Long id) {
    if (id == null || id <= 0) {
        return Mono.error(new ValidationException("Invalid user ID"));
    }
    return userRepository.findById(id);
}
```

### Error Aggregation

```java
public Mono<List<User>> getUsersWithErrorAggregation(List<Long> ids) {
    List<Throwable> errors = new ArrayList<>();
    
    return Flux.fromIterable(ids)
        .flatMap(id -> userRepository.findById(id)
            .onErrorResume(e -> {
                errors.add(e);
                return Mono.empty();
            }))
        .collectList()
        .flatMap(users -> {
            if (!errors.isEmpty()) {
                log.error("Errors occurred: {}", errors);
            }
            return Mono.just(users);
        });
}
```

## Next Steps

- [Testing](Testing.md) - Testing error scenarios
- [Performance](Performance.md) - Error handling performance
- [Examples](Examples.md) - Real-world error handling examples
