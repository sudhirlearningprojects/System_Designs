# Spring Boot Exception Handling: @ControllerAdvice vs ResponseEntity

## Table of Contents
1. [Overview](#overview)
2. [ResponseEntity Approach](#responseentity-approach)
3. [ControllerAdvice Approach](#controlleradvice-approach)
4. [Comparison & When to Use](#comparison--when-to-use)
5. [Real-World Production Examples](#real-world-production-examples)

---

## Overview

### Exception Handling Flow in Spring Boot

```
Request → DispatcherServlet → Controller
                                   ↓
                            Exception Thrown
                                   ↓
                    ┌──────────────┴──────────────┐
                    ↓                             ↓
            @ControllerAdvice              ResponseEntity
         (Global Handler)              (Method-Level Handler)
                    ↓                             ↓
            Error Response                Error Response
```

---

## ResponseEntity Approach

### What is ResponseEntity?

`ResponseEntity` represents the entire HTTP response: status code, headers, and body.

```java
public class ResponseEntity<T> extends HttpEntity<T> {
    private final HttpStatus status;
    
    // Constructor
    public ResponseEntity(T body, HttpStatus status) {
        this.body = body;
        this.status = status;
    }
}
```

### Basic Usage

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### With Custom Error Response

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "User not found",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
```

### Pros and Cons

**Pros:**
- ✅ Fine-grained control per endpoint
- ✅ Can return different response types
- ✅ Explicit error handling
- ✅ Easy to customize headers

**Cons:**
- ❌ Code duplication across controllers
- ❌ Verbose try-catch blocks
- ❌ Mixes business logic with error handling
- ❌ Hard to maintain consistency

---

## ControllerAdvice Approach

### What is @ControllerAdvice?

`@ControllerAdvice` is a global exception handler that intercepts exceptions from all controllers.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "User not found",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### Complete Example

```java
// Custom Exception
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

// Error Response DTO
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    
    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}

// Global Exception Handler
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "User Not Found",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Internal server error", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Clean Controller
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id); // Throws UserNotFoundException
        return ResponseEntity.ok(user);
    }
}
```

### Pros and Cons

**Pros:**
- ✅ Centralized exception handling
- ✅ Clean controller code
- ✅ Consistent error responses
- ✅ Easy to maintain
- ✅ Reduces code duplication

**Cons:**
- ❌ Less control per endpoint
- ❌ Can be harder to debug
- ❌ May need multiple @ControllerAdvice classes for different modules

---

## Comparison & When to Use

### Side-by-Side Comparison

| Feature | ResponseEntity | @ControllerAdvice |
|---------|---------------|-------------------|
| **Scope** | Method-level | Application-wide |
| **Code Location** | Inside controller | Separate class |
| **Reusability** | Low | High |
| **Maintainability** | Low | High |
| **Flexibility** | High | Medium |
| **Code Duplication** | High | Low |
| **Best For** | Specific cases | General cases |

### When to Use ResponseEntity

✅ **Use ResponseEntity when:**

1. **Endpoint-specific error handling**
```java
@PostMapping("/upload")
public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {
    if (file.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("File is empty"));
    }
    if (file.getSize() > MAX_SIZE) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ErrorResponse("File too large"));
    }
    // Process file
    return ResponseEntity.ok(new UploadResponse("Success"));
}
```

2. **Custom headers needed**
```java
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ResponseEntity.ok()
        .header("X-Custom-Header", "value")
        .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
        .body(user);
}
```

3. **Different response types**
```java
@GetMapping("/download/{id}")
public ResponseEntity<?> downloadFile(@PathVariable Long id) {
    try {
        byte[] data = fileService.getFile(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    } catch (FileNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}
```

4. **Validation with immediate response**
```java
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody UserDTO dto) {
    if (userService.emailExists(dto.getEmail())) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Email already exists"));
    }
    User user = userService.register(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
}
```

### When to Use @ControllerAdvice

✅ **Use @ControllerAdvice when:**

1. **Application-wide exception handling**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

2. **Consistent error format across all endpoints**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorResponse> handleAll(Exception ex) {
        StandardErrorResponse error = StandardErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

3. **Business exceptions**
```java
@ControllerAdvice
public class BusinessExceptionHandler {
    
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Insufficient balance", ex.getMessage()));
    }
    
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(new ErrorResponse("Payment failed", ex.getMessage()));
    }
}
```

4. **Validation errors**
```java
@ControllerAdvice
public class ValidationExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );
        return ResponseEntity.badRequest().body(response);
    }
}
```

---

## Real-World Production Examples

### Example 1: E-Commerce Application

```java
// Custom Exceptions
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }
}

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String product, int available, int requested) {
        super(String.format("Insufficient stock for %s. Available: %d, Requested: %d", 
            product, available, requested));
    }
}

public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String code) {
        super("Invalid or expired coupon: " + code);
    }
}

// Global Exception Handler
@ControllerAdvice
@Slf4j
public class ECommerceExceptionHandler {
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Product Not Found",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Insufficient Stock",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(InvalidCouponException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCoupon(InvalidCouponException ex) {
        log.warn("Invalid coupon: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Coupon",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Clean Controller
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Product product = productService.findById(id); // Throws ProductNotFoundException
        return ResponseEntity.ok(product);
    }
    
    @PostMapping("/order")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        // Throws InsufficientStockException if stock unavailable
        // Throws InvalidCouponException if coupon invalid
        Order order = productService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
```

### Example 2: Banking Application (Mixed Approach)

```java
// Global handler for common exceptions
@ControllerAdvice
@Slf4j
public class BankingExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "Account Not Found", ex.getMessage()));
    }
    
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Insufficient Balance", ex.getMessage()));
    }
}

// Controller with specific ResponseEntity handling
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    
    // Use @ControllerAdvice for business exceptions
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        // Throws AccountNotFoundException, InsufficientBalanceException
        Transaction transaction = transactionService.transfer(request);
        return ResponseEntity.ok(new TransactionResponse(transaction));
    }
    
    // Use ResponseEntity for specific validation
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody WithdrawRequest request) {
        // Specific validation for withdrawal limits
        if (request.getAmount() > 10000) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Withdrawal Limit Exceeded", 
                    "Daily withdrawal limit is $10,000"));
        }
        
        if (request.getAmount() % 10 != 0) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Invalid Amount", 
                    "Amount must be in multiples of $10"));
        }
        
        // Business exceptions handled by @ControllerAdvice
        Transaction transaction = transactionService.withdraw(request);
        return ResponseEntity.ok(new TransactionResponse(transaction));
    }
}
```

### Example 3: Microservices with Module-Specific Handlers

```java
// Base Exception Handler (Common)
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class BaseExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "Internal Server Error", ex.getMessage()));
    }
}

// User Module Exception Handler
@ControllerAdvice(basePackages = "com.example.user")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "User Not Found", ex.getMessage()));
    }
    
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, "Email Already Exists", ex.getMessage()));
    }
}

// Order Module Exception Handler
@ControllerAdvice(basePackages = "com.example.order")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "Order Not Found", ex.getMessage()));
    }
    
    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancellation(OrderCancellationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Cannot Cancel Order", ex.getMessage()));
    }
}
```

### Example 4: REST API with Detailed Error Responses

```java
// Detailed Error Response
@Data
@Builder
public class ApiErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<ValidationError> validationErrors;
    private String traceId;
    
    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}

// Comprehensive Exception Handler
@ControllerAdvice
@Slf4j
public class ApiExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        List<ApiErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ApiErrorResponse.ValidationError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());
        
        ApiErrorResponse response = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .timestamp(LocalDateTime.now())
            .validationErrors(validationErrors)
            .traceId(UUID.randomUUID().toString())
            .build();
        
        log.warn("Validation failed: {}", validationErrors);
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Malformed JSON")
            .message("Invalid JSON format in request body")
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .timestamp(LocalDateTime.now())
            .traceId(UUID.randomUUID().toString())
            .build();
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You don't have permission to access this resource")
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .timestamp(LocalDateTime.now())
            .traceId(UUID.randomUUID().toString())
            .build();
        
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
```

---

## Best Practices

### 1. Create Custom Exception Hierarchy

```java
// Base exception
public abstract class BusinessException extends RuntimeException {
    private final HttpStatus status;
    
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}

// Specific exceptions
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s not found with id: %d", resource, id), HttpStatus.NOT_FOUND);
    }
}

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String resource, String field, String value) {
        super(String.format("%s already exists with %s: %s", resource, field, value), 
            HttpStatus.CONFLICT);
    }
}

// Generic handler
@ControllerAdvice
public class BusinessExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(error);
    }
}
```

### 2. Use @ResponseStatus for Simple Cases

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// No need for @ControllerAdvice handler
// Spring automatically returns 404 with exception message
```

### 3. Combine Both Approaches

```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    // Use ResponseEntity for specific file operations
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("File cannot be empty"));
        }
        
        if (!isValidFileType(file)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("Invalid file type. Only PDF, PNG, JPG allowed"));
        }
        
        // Business exceptions handled by @ControllerAdvice
        FileMetadata metadata = fileService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }
    
    // Let @ControllerAdvice handle business exceptions
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadata> getFile(@PathVariable Long id) {
        FileMetadata metadata = fileService.findById(id); // Throws FileNotFoundException
        return ResponseEntity.ok(metadata);
    }
}
```

### 4. Log Appropriately

```java
@ControllerAdvice
@Slf4j
public class LoggingExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage()); // WARN for expected errors
        return ResponseEntity.status(ex.getStatus())
            .body(new ErrorResponse(ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error", ex); // ERROR with stack trace for unexpected errors
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred"));
    }
}
```

---

## Summary

### Quick Decision Guide

```
┌─────────────────────────────────────────────────────────┐
│              Exception Handling Decision Tree            │
└─────────────────────────────────────────────────────────┘

Is it a business/domain exception?
    ├─ YES → Use @ControllerAdvice
    │         (Consistent handling across app)
    │
    └─ NO → Is it endpoint-specific validation?
            ├─ YES → Use ResponseEntity
            │         (Custom logic per endpoint)
            │
            └─ NO → Is it a validation error?
                    ├─ YES → Use @ControllerAdvice
                    │         (Consistent validation format)
                    │
                    └─ NO → Use @ControllerAdvice
                              (Default fallback)
```

### Key Takeaways

1. **@ControllerAdvice**: Use for application-wide, consistent error handling
2. **ResponseEntity**: Use for endpoint-specific, custom responses
3. **Combine Both**: Use @ControllerAdvice for business exceptions, ResponseEntity for specific validations
4. **Create Exception Hierarchy**: Organize exceptions by domain/module
5. **Log Appropriately**: WARN for expected errors, ERROR for unexpected
6. **Return Consistent Format**: Use standard error response DTOs
7. **Include Trace IDs**: For debugging in production
8. **Don't Expose Internals**: Hide sensitive error details from clients
