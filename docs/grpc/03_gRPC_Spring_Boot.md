# gRPC Complete Guide - Part 3: Spring Boot Integration

## 📋 Table of Contents
1. [Project Setup](#project-setup)
2. [Server Implementation](#server-implementation)
3. [Client Implementation](#client-implementation)
4. [Service Integration](#service-integration)
5. [Testing](#testing)

---

## Project Setup

### Maven Dependencies
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- gRPC Spring Boot Starter -->
    <dependency>
        <groupId>net.devh</groupId>
        <artifactId>grpc-spring-boot-starter</artifactId>
        <version>2.15.0.RELEASE</version>
    </dependency>
    
    <!-- gRPC Client Spring Boot Starter -->
    <dependency>
        <groupId>net.devh</groupId>
        <artifactId>grpc-client-spring-boot-starter</artifactId>
        <version>2.15.0.RELEASE</version>
    </dependency>
</dependencies>
```

### Configuration
```yaml
# application.yml
grpc:
  server:
    port: 9090
    
  client:
    user-service:
      address: 'static://localhost:9090'
      negotiation-type: plaintext
```

---

## Server Implementation

### Service Implementation
```java
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        String userId = request.getUserId();
        
        User user = userRepository.findById(userId)
            .map(this::toProto)
            .orElseThrow(() -> 
                Status.NOT_FOUND
                    .withDescription("User not found")
                    .asRuntimeException()
            );
        
        GetUserResponse response = GetUserResponse.newBuilder()
            .setUser(user)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        com.example.entity.User entity = new com.example.entity.User();
        entity.setEmail(request.getEmail());
        entity.setName(request.getName());
        entity.setAge(request.getAge());
        
        entity = userRepository.save(entity);
        
        User user = toProto(entity);
        
        CreateUserResponse response = CreateUserResponse.newBuilder()
            .setUser(user)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    private User toProto(com.example.entity.User entity) {
        return User.newBuilder()
            .setUserId(entity.getId())
            .setEmail(entity.getEmail())
            .setName(entity.getName())
            .setAge(entity.getAge())
            .build();
    }
}
```

### Global Exception Handler
```java
@GrpcAdvice
public class GrpcExceptionHandler {
    
    @GrpcExceptionHandler(IllegalArgumentException.class)
    public Status handleIllegalArgument(IllegalArgumentException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
    }
    
    @GrpcExceptionHandler(EntityNotFoundException.class)
    public Status handleNotFound(EntityNotFoundException e) {
        return Status.NOT_FOUND.withDescription(e.getMessage());
    }
    
    @GrpcExceptionHandler(Exception.class)
    public Status handleGeneral(Exception e) {
        return Status.INTERNAL.withDescription("Internal server error");
    }
}
```

### Interceptor
```java
@GrpcGlobalServerInterceptor
public class LoggingInterceptor implements ServerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        log.info("Received gRPC call: {}", methodName);
        
        return next.startCall(call, headers);
    }
}
```

---

## Client Implementation

### gRPC Client
```java
@Service
public class UserGrpcClient {
    
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
    
    public User getUser(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
            .setUserId(userId)
            .build();
        
        try {
            GetUserResponse response = userServiceStub.getUser(request);
            return response.getUser();
            
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Failed to get user: " + e.getStatus().getDescription());
        }
    }
    
    public User createUser(String email, String name, int age) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setEmail(email)
            .setName(name)
            .setAge(age)
            .build();
        
        CreateUserResponse response = userServiceStub.createUser(request);
        return response.getUser();
    }
}
```

### Async Client
```java
@Service
public class AsyncUserGrpcClient {
    
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceFutureStub futureStub;
    
    public CompletableFuture<User> getUserAsync(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
            .setUserId(userId)
            .build();
        
        ListenableFuture<GetUserResponse> future = futureStub.getUser(request);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get().getUser();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

---

## Service Integration

### REST Controller with gRPC Client
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserGrpcClient userGrpcClient;
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        User user = userGrpcClient.getUser(userId);
        
        UserDto dto = UserDto.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .name(user.getName())
            .age(user.getAge())
            .build();
        
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto request) {
        User user = userGrpcClient.createUser(
            request.getEmail(),
            request.getName(),
            request.getAge()
        );
        
        UserDto dto = UserDto.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .name(user.getName())
            .age(user.getAge())
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
```

### Service Layer
```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserGrpcClient userGrpcClient;
    
    @Autowired
    private ProductGrpcClient productGrpcClient;
    
    public Order createOrder(CreateOrderRequest request) {
        // Validate user via gRPC
        User user = userGrpcClient.getUser(request.getUserId());
        
        // Validate products via gRPC
        List<OrderItem> items = new ArrayList<>();
        double totalAmount = 0;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productGrpcClient.getProduct(itemRequest.getProductId());
            
            OrderItem item = new OrderItem();
            item.setProductId(product.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(product.getPrice());
            
            items.add(item);
            totalAmount += product.getPrice() * itemRequest.getQuantity();
        }
        
        // Create order
        Order order = new Order();
        order.setUserId(user.getUserId());
        order.setItems(items);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        
        return orderRepository.save(order);
    }
}
```

---

## Testing

### Unit Test
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void testGetUser() {
        // Given
        String userId = "user123";
        com.example.entity.User entity = new com.example.entity.User();
        entity.setId(userId);
        entity.setEmail("john@example.com");
        entity.setName("John Doe");
        entity.setAge(30);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        
        GetUserRequest request = GetUserRequest.newBuilder()
            .setUserId(userId)
            .build();
        
        StreamObserver<GetUserResponse> responseObserver = mock(StreamObserver.class);
        
        // When
        userService.getUser(request, responseObserver);
        
        // Then
        verify(responseObserver).onNext(any(GetUserResponse.class));
        verify(responseObserver).onCompleted();
    }
}
```

### Integration Test
```java
@SpringBootTest
@DirtiesContext
class UserServiceIntegrationTest {
    
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
    
    @Test
    void testCreateAndGetUser() {
        // Create user
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
            .setEmail("test@example.com")
            .setName("Test User")
            .setAge(25)
            .build();
        
        CreateUserResponse createResponse = userServiceStub.createUser(createRequest);
        assertNotNull(createResponse.getUser().getUserId());
        
        // Get user
        GetUserRequest getRequest = GetUserRequest.newBuilder()
            .setUserId(createResponse.getUser().getUserId())
            .build();
        
        GetUserResponse getResponse = userServiceStub.getUser(getRequest);
        assertEquals("test@example.com", getResponse.getUser().getEmail());
    }
}
```

---

## Production Configuration

### Security (TLS)
```yaml
grpc:
  server:
    port: 9090
    security:
      enabled: true
      certificate-chain: classpath:server.crt
      private-key: classpath:server.key
      
  client:
    user-service:
      address: 'static://localhost:9090'
      negotiation-type: tls
      security:
        trust-cert-collection: classpath:ca.crt
```

### Load Balancing
```yaml
grpc:
  client:
    user-service:
      address: 'dns:///user-service:9090'
      negotiation-type: plaintext
      load-balancing-policy: round_robin
```

### Health Check
```java
@GrpcService
public class HealthServiceImpl extends HealthGrpc.HealthImplBase {
    
    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
            .setStatus(HealthCheckResponse.ServingStatus.SERVING)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

---

## Next Steps

Continue to [Part 4: Production Best Practices](./04_gRPC_Production.md)
