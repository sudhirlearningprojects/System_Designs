# gRPC Complete Guide - Part 4: Production Best Practices

## 📋 Table of Contents
1. [Performance Optimization](#performance-optimization)
2. [Security](#security)
3. [Monitoring & Observability](#monitoring--observability)
4. [Load Balancing](#load-balancing)
5. [Deployment](#deployment)

---

## Performance Optimization

### Connection Pooling
```java
@Configuration
public class GrpcClientConfig {
    
    @Bean
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 9090)
            .usePlaintext()
            .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .build();
    }
}
```

### Message Compression
```java
// Server
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        // Enable compression
        Context.current()
            .withValue(GrpcUtil.MESSAGE_ENCODING_KEY, "gzip")
            .run(() -> {
                // Process request
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            });
    }
}

// Client
UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel)
    .withCompression("gzip");
```

### Batch Requests
```protobuf
service UserService {
    rpc BatchGetUsers(BatchGetUsersRequest) returns (BatchGetUsersResponse);
}

message BatchGetUsersRequest {
    repeated string user_ids = 1;
}

message BatchGetUsersResponse {
    repeated User users = 1;
}
```

```java
@Override
public void batchGetUsers(BatchGetUsersRequest request, 
                         StreamObserver<BatchGetUsersResponse> responseObserver) {
    List<User> users = request.getUserIdsList().stream()
        .map(userRepository::findById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::toProto)
        .collect(Collectors.toList());
    
    BatchGetUsersResponse response = BatchGetUsersResponse.newBuilder()
        .addAllUsers(users)
        .build();
    
    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

---

## Security

### TLS/SSL Configuration
```yaml
# Server
grpc:
  server:
    port: 9090
    security:
      enabled: true
      certificate-chain: file:/path/to/server.crt
      private-key: file:/path/to/server.key
      client-auth: REQUIRE
      trust-cert-collection: file:/path/to/ca.crt
```

```java
// Programmatic TLS
Server server = NettyServerBuilder.forPort(9090)
    .useTransportSecurity(
        new File("server.crt"),
        new File("server.key")
    )
    .addService(new UserServiceImpl())
    .build();
```

### Authentication
```java
// JWT Authentication Interceptor
@GrpcGlobalServerInterceptor
public class AuthInterceptor implements ServerInterceptor {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String token = headers.get(
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
        );
        
        if (token == null || !token.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing token"), new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }
        
        try {
            String jwt = token.substring(7);
            String userId = tokenProvider.validateToken(jwt);
            
            Context context = Context.current()
                .withValue(USER_ID_KEY, userId);
            
            return Contexts.interceptCall(context, call, headers, next);
            
        } catch (Exception e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }
    }
    
    private static final Context.Key<String> USER_ID_KEY = Context.key("userId");
}
```

### Authorization
```java
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    @Override
    public void deleteUser(DeleteUserRequest request, 
                          StreamObserver<DeleteUserResponse> responseObserver) {
        String currentUserId = USER_ID_KEY.get();
        String targetUserId = request.getUserId();
        
        if (!currentUserId.equals(targetUserId) && !isAdmin(currentUserId)) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("Not authorized to delete this user")
                    .asRuntimeException()
            );
            return;
        }
        
        // Process deletion
    }
}
```

---

## Monitoring & Observability

### Metrics with Micrometer
```java
@Configuration
public class GrpcMetricsConfig {
    
    @Bean
    public MonitoringServerInterceptor monitoringInterceptor(MeterRegistry registry) {
        return MonitoringServerInterceptor.create(
            Configuration.allMetrics()
                .withLatencyBuckets(0.001, 0.01, 0.1, 1, 10)
        );
    }
}

@GrpcGlobalServerInterceptor
public class MetricsInterceptor implements ServerInterceptor {
    
    @Autowired
    private MeterRegistry registry;
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        Timer.Sample sample = Timer.start(registry);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            
            @Override
            public void onComplete() {
                sample.stop(Timer.builder("grpc.server.calls")
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(registry));
                super.onComplete();
            }
            
            @Override
            public void onCancel() {
                sample.stop(Timer.builder("grpc.server.calls")
                    .tag("method", methodName)
                    .tag("status", "cancelled")
                    .register(registry));
                super.onCancel();
            }
        };
    }
}
```

### Distributed Tracing
```java
@GrpcGlobalServerInterceptor
public class TracingInterceptor implements ServerInterceptor {
    
    @Autowired
    private Tracer tracer;
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        Span span = tracer.nextSpan().name(methodName).start();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            return next.startCall(call, headers);
        } finally {
            span.end();
        }
    }
}
```

### Logging
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
        long startTime = System.currentTimeMillis();
        
        log.info("gRPC call started: {}", methodName);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            
            @Override
            public void onComplete() {
                long duration = System.currentTimeMillis() - startTime;
                log.info("gRPC call completed: {} in {}ms", methodName, duration);
                super.onComplete();
            }
            
            @Override
            public void onCancel() {
                log.warn("gRPC call cancelled: {}", methodName);
                super.onCancel();
            }
        };
    }
}
```

---

## Load Balancing

### Client-Side Load Balancing
```yaml
grpc:
  client:
    user-service:
      address: 'dns:///user-service:9090'
      negotiation-type: plaintext
      load-balancing-policy: round_robin
```

```java
ManagedChannel channel = ManagedChannelBuilder
    .forTarget("dns:///user-service:9090")
    .defaultLoadBalancingPolicy("round_robin")
    .usePlaintext()
    .build();
```

### Service Discovery (Kubernetes)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
    - protocol: TCP
      port: 9090
      targetPort: 9090
  type: ClusterIP
```

### Retry Policy
```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 9090)
    .enableRetry()
    .maxRetryAttempts(3)
    .usePlaintext()
    .build();

UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel)
    .withOption(
        CallOptions.Key.create("grpc.retry_policy"),
        RetryPolicy.newBuilder()
            .setMaxAttempts(3)
            .setInitialBackoff(Duration.ofMillis(100))
            .setMaxBackoff(Duration.ofSeconds(1))
            .setBackoffMultiplier(2.0)
            .build()
    );
```

---

## Deployment

### Docker
```dockerfile
# Dockerfile
FROM openjdk:17-slim

WORKDIR /app

COPY target/grpc-service.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: user-service:latest
        ports:
        - containerPort: 9090
          name: grpc
        env:
        - name: GRPC_SERVER_PORT
          value: "9090"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          exec:
            command: ["/bin/grpc_health_probe", "-addr=:9090"]
          initialDelaySeconds: 10
        readinessProbe:
          exec:
            command: ["/bin/grpc_health_probe", "-addr=:9090"]
          initialDelaySeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
  - port: 9090
    targetPort: 9090
    name: grpc
  type: ClusterIP
```

### Health Check
```protobuf
syntax = "proto3";

package grpc.health.v1;

service Health {
    rpc Check(HealthCheckRequest) returns (HealthCheckResponse);
    rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
}

message HealthCheckRequest {
    string service = 1;
}

message HealthCheckResponse {
    enum ServingStatus {
        UNKNOWN = 0;
        SERVING = 1;
        NOT_SERVING = 2;
    }
    ServingStatus status = 1;
}
```

```java
@GrpcService
public class HealthServiceImpl extends HealthGrpc.HealthImplBase {
    
    @Override
    public void check(HealthCheckRequest request, 
                     StreamObserver<HealthCheckResponse> responseObserver) {
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
            .setStatus(HealthCheckResponse.ServingStatus.SERVING)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

---

## Performance Benchmarks

### gRPC vs REST
```
Metric              | gRPC      | REST
--------------------|-----------|----------
Latency (p50)       | 2ms       | 15ms
Latency (p99)       | 10ms      | 50ms
Throughput          | 50K RPS   | 10K RPS
Payload Size        | 100 bytes | 500 bytes
CPU Usage           | 20%       | 40%
Memory Usage        | 200MB     | 300MB
```

### Optimization Tips
```
1. Use streaming for large datasets
2. Enable compression for large messages
3. Batch requests when possible
4. Use connection pooling
5. Set appropriate timeouts
6. Monitor and tune thread pools
7. Use async stubs for non-blocking calls
```

---

## Troubleshooting

### Common Issues

#### 1. Connection Refused
```java
// Check server is running
// Check port is correct
// Check firewall rules
```

#### 2. Deadline Exceeded
```java
// Increase timeout
stub.withDeadlineAfter(10, TimeUnit.SECONDS);

// Check server performance
// Check network latency
```

#### 3. Resource Exhausted
```java
// Increase max message size
ServerBuilder.forPort(9090)
    .maxInboundMessageSize(10 * 1024 * 1024)
    .build();
```

#### 4. Unavailable
```java
// Check service health
// Check load balancer
// Implement retry logic
```

---

## README

Create comprehensive README:
