# gRPC Complete Guide - Part 2: Advanced Features & Streaming

## 📋 Table of Contents
1. [Streaming Patterns](#streaming-patterns)
2. [Error Handling](#error-handling)
3. [Interceptors](#interceptors)
4. [Metadata & Headers](#metadata--headers)
5. [Deadlines & Timeouts](#deadlines--timeouts)

---

## Streaming Patterns

### Server Streaming
```protobuf
service OrderService {
    rpc StreamOrders(StreamOrdersRequest) returns (stream Order);
}
```

```java
// Server
@Override
public void streamOrders(StreamOrdersRequest request, StreamObserver<Order> responseObserver) {
    String userId = request.getUserId();
    
    orders.values().stream()
        .filter(order -> order.getUserId().equals(userId))
        .forEach(order -> {
            responseObserver.onNext(order);
            try {
                Thread.sleep(100); // Simulate delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    
    responseObserver.onCompleted();
}

// Client
Iterator<Order> orders = blockingStub.streamOrders(request);
while (orders.hasNext()) {
    Order order = orders.next();
    System.out.println("Received order: " + order.getOrderId());
}
```

### Client Streaming
```protobuf
service MetricsService {
    rpc UploadMetrics(stream Metric) returns (MetricsSummary);
}
```

```java
// Server
@Override
public StreamObserver<Metric> uploadMetrics(StreamObserver<MetricsSummary> responseObserver) {
    return new StreamObserver<Metric>() {
        private int count = 0;
        private double sum = 0;
        
        @Override
        public void onNext(Metric metric) {
            count++;
            sum += metric.getValue();
        }
        
        @Override
        public void onError(Throwable t) {
            System.err.println("Error: " + t.getMessage());
        }
        
        @Override
        public void onCompleted() {
            MetricsSummary summary = MetricsSummary.newBuilder()
                .setTotalCount(count)
                .setAverageValue(sum / count)
                .build();
            
            responseObserver.onNext(summary);
            responseObserver.onCompleted();
        }
    };
}

// Client
StreamObserver<MetricsSummary> responseObserver = new StreamObserver<MetricsSummary>() {
    @Override
    public void onNext(MetricsSummary summary) {
        System.out.println("Summary: " + summary.getTotalCount() + " metrics");
    }
    
    @Override
    public void onError(Throwable t) {
        System.err.println("Error: " + t.getMessage());
    }
    
    @Override
    public void onCompleted() {
        System.out.println("Upload completed");
    }
};

StreamObserver<Metric> requestObserver = asyncStub.uploadMetrics(responseObserver);

for (int i = 0; i < 100; i++) {
    Metric metric = Metric.newBuilder()
        .setName("cpu_usage")
        .setValue(Math.random() * 100)
        .setTimestamp(System.currentTimeMillis())
        .build();
    
    requestObserver.onNext(metric);
}

requestObserver.onCompleted();
```

### Bidirectional Streaming
```protobuf
service ChatService {
    rpc Chat(stream ChatMessage) returns (stream ChatMessage);
}
```

```java
// Server
@Override
public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
    return new StreamObserver<ChatMessage>() {
        @Override
        public void onNext(ChatMessage message) {
            // Broadcast to all connected clients
            ChatMessage response = ChatMessage.newBuilder()
                .setUserId(message.getUserId())
                .setMessage("Echo: " + message.getMessage())
                .setTimestamp(System.currentTimeMillis())
                .build();
            
            responseObserver.onNext(response);
        }
        
        @Override
        public void onError(Throwable t) {
            System.err.println("Chat error: " + t.getMessage());
        }
        
        @Override
        public void onCompleted() {
            responseObserver.onCompleted();
        }
    };
}

// Client
StreamObserver<ChatMessage> responseObserver = new StreamObserver<ChatMessage>() {
    @Override
    public void onNext(ChatMessage message) {
        System.out.println(message.getUserId() + ": " + message.getMessage());
    }
    
    @Override
    public void onError(Throwable t) {
        System.err.println("Error: " + t.getMessage());
    }
    
    @Override
    public void onCompleted() {
        System.out.println("Chat ended");
    }
};

StreamObserver<ChatMessage> requestObserver = asyncStub.chat(responseObserver);

// Send messages
requestObserver.onNext(ChatMessage.newBuilder()
    .setUserId("user1")
    .setMessage("Hello!")
    .build());
```

---

## Error Handling

### Status Codes
```java
// Server-side error handling
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    String userId = request.getUserId();
    
    if (userId == null || userId.isEmpty()) {
        responseObserver.onError(
            Status.INVALID_ARGUMENT
                .withDescription("User ID is required")
                .asRuntimeException()
        );
        return;
    }
    
    User user = users.get(userId);
    if (user == null) {
        responseObserver.onError(
            Status.NOT_FOUND
                .withDescription("User not found: " + userId)
                .asRuntimeException()
        );
        return;
    }
    
    // Success
    responseObserver.onNext(GetUserResponse.newBuilder().setUser(user).build());
    responseObserver.onCompleted();
}
```

### Client-side Error Handling
```java
try {
    User user = blockingStub.getUser(request);
    System.out.println("User: " + user.getName());
    
} catch (StatusRuntimeException e) {
    Status status = e.getStatus();
    
    switch (status.getCode()) {
        case NOT_FOUND:
            System.err.println("User not found");
            break;
        case INVALID_ARGUMENT:
            System.err.println("Invalid request: " + status.getDescription());
            break;
        case UNAVAILABLE:
            System.err.println("Service unavailable");
            break;
        default:
            System.err.println("Error: " + status.getDescription());
    }
}
```

### Custom Error Details
```protobuf
message ErrorDetails {
    string error_code = 1;
    string message = 2;
    map<string, string> metadata = 3;
}
```

```java
// Server
ErrorDetails details = ErrorDetails.newBuilder()
    .setErrorCode("USER_NOT_FOUND")
    .setMessage("User does not exist")
    .putMetadata("user_id", userId)
    .build();

Metadata trailers = new Metadata();
trailers.put(
    Metadata.Key.of("error-details-bin", Metadata.BINARY_BYTE_MARSHALLER),
    details.toByteArray()
);

responseObserver.onError(
    Status.NOT_FOUND
        .withDescription("User not found")
        .asRuntimeException(trailers)
);
```

---

## Interceptors

### Server Interceptor
```java
public class LoggingInterceptor implements ServerInterceptor {
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        System.out.println("Received call: " + methodName);
        
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendMessage(RespT message) {
                System.out.println("Sending response for: " + methodName);
                super.sendMessage(message);
            }
        }, headers);
    }
}

// Register interceptor
Server server = ServerBuilder.forPort(9090)
    .addService(ServerInterceptors.intercept(new UserServiceImpl(), new LoggingInterceptor()))
    .build();
```

### Client Interceptor
```java
public class AuthInterceptor implements ClientInterceptor {
    
    private final String token;
    
    public AuthInterceptor(String token) {
        this.token = token;
    }
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(
                    Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                    "Bearer " + token
                );
                super.start(responseListener, headers);
            }
        };
    }
}

// Register interceptor
ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
    .usePlaintext()
    .intercept(new AuthInterceptor("my-token"))
    .build();
```

---

## Metadata & Headers

### Send Metadata (Client)
```java
Metadata metadata = new Metadata();
metadata.put(
    Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER),
    "user123"
);
metadata.put(
    Metadata.Key.of("request-id", Metadata.ASCII_STRING_MARSHALLER),
    UUID.randomUUID().toString()
);

UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
stub = MetadataUtils.attachHeaders(stub, metadata);

User user = stub.getUser(request);
```

### Read Metadata (Server)
```java
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    Context context = Context.current();
    Metadata headers = HEADERS_KEY.get(context);
    
    String userId = headers.get(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER));
    String requestId = headers.get(Metadata.Key.of("request-id", Metadata.ASCII_STRING_MARSHALLER));
    
    System.out.println("User ID: " + userId + ", Request ID: " + requestId);
    
    // Process request
}

// Context key
private static final Context.Key<Metadata> HEADERS_KEY = Context.key("headers");
```

---

## Deadlines & Timeouts

### Set Deadline (Client)
```java
// Deadline in 5 seconds
UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel)
    .withDeadlineAfter(5, TimeUnit.SECONDS);

try {
    User user = stub.getUser(request);
} catch (StatusRuntimeException e) {
    if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        System.err.println("Request timed out");
    }
}
```

### Check Deadline (Server)
```java
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    Context context = Context.current();
    
    if (context.isCancelled()) {
        responseObserver.onError(
            Status.CANCELLED
                .withDescription("Request cancelled by client")
                .asRuntimeException()
        );
        return;
    }
    
    // Check if deadline exceeded
    Deadline deadline = context.getDeadline();
    if (deadline != null && deadline.isExpired()) {
        responseObserver.onError(
            Status.DEADLINE_EXCEEDED
                .withDescription("Deadline exceeded")
                .asRuntimeException()
        );
        return;
    }
    
    // Process request
}
```

---

## Real-World Example: Order Service

### Proto Definition
```protobuf
syntax = "proto3";

package com.example.grpc;

service OrderService {
    rpc CreateOrder(CreateOrderRequest) returns (CreateOrderResponse);
    rpc GetOrder(GetOrderRequest) returns (GetOrderResponse);
    rpc StreamOrderUpdates(StreamOrderUpdatesRequest) returns (stream OrderUpdate);
    rpc CancelOrder(CancelOrderRequest) returns (CancelOrderResponse);
}

message CreateOrderRequest {
    string user_id = 1;
    repeated OrderItem items = 2;
}

message CreateOrderResponse {
    Order order = 1;
}

message Order {
    string order_id = 1;
    string user_id = 2;
    repeated OrderItem items = 3;
    double total_amount = 4;
    OrderStatus status = 5;
    int64 created_at = 6;
}

message OrderItem {
    string product_id = 1;
    int32 quantity = 2;
    double price = 3;
}

enum OrderStatus {
    PENDING = 0;
    PROCESSING = 1;
    SHIPPED = 2;
    DELIVERED = 3;
    CANCELLED = 4;
}

message OrderUpdate {
    string order_id = 1;
    OrderStatus status = 2;
    int64 timestamp = 3;
}
```

### Server Implementation
```java
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {
    
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, List<StreamObserver<OrderUpdate>>> subscribers = new ConcurrentHashMap<>();
    
    @Override
    public void createOrder(CreateOrderRequest request, 
                           StreamObserver<CreateOrderResponse> responseObserver) {
        String orderId = UUID.randomUUID().toString();
        
        double totalAmount = request.getItemsList().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        
        Order order = Order.newBuilder()
            .setOrderId(orderId)
            .setUserId(request.getUserId())
            .addAllItems(request.getItemsList())
            .setTotalAmount(totalAmount)
            .setStatus(OrderStatus.PENDING)
            .setCreatedAt(System.currentTimeMillis())
            .build();
        
        orders.put(orderId, order);
        
        CreateOrderResponse response = CreateOrderResponse.newBuilder()
            .setOrder(order)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        
        // Notify subscribers
        notifySubscribers(orderId, OrderStatus.PENDING);
    }
    
    @Override
    public void streamOrderUpdates(StreamOrderUpdatesRequest request,
                                   StreamObserver<OrderUpdate> responseObserver) {
        String orderId = request.getOrderId();
        
        subscribers.computeIfAbsent(orderId, k -> new ArrayList<>())
            .add(responseObserver);
        
        // Send current status
        Order order = orders.get(orderId);
        if (order != null) {
            OrderUpdate update = OrderUpdate.newBuilder()
                .setOrderId(orderId)
                .setStatus(order.getStatus())
                .setTimestamp(System.currentTimeMillis())
                .build();
            
            responseObserver.onNext(update);
        }
    }
    
    private void notifySubscribers(String orderId, OrderStatus status) {
        List<StreamObserver<OrderUpdate>> observers = subscribers.get(orderId);
        if (observers != null) {
            OrderUpdate update = OrderUpdate.newBuilder()
                .setOrderId(orderId)
                .setStatus(status)
                .setTimestamp(System.currentTimeMillis())
                .build();
            
            observers.forEach(observer -> observer.onNext(update));
        }
    }
}
```

---

## Next Steps

Continue to [Part 3: Spring Boot Integration](./03_gRPC_Spring_Boot.md)
