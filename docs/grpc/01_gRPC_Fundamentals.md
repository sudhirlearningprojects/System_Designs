# gRPC Complete Guide - Part 1: Fundamentals & Protocol Buffers

## 📋 Table of Contents
1. [Introduction](#introduction)
2. [Installation & Setup](#installation--setup)
3. [Protocol Buffers](#protocol-buffers)
4. [Service Definition](#service-definition)
5. [Basic Implementation](#basic-implementation)

---

## Introduction

gRPC is a high-performance, open-source RPC framework that uses HTTP/2 and Protocol Buffers for efficient communication.

### Key Features
- **High Performance**: Binary protocol, HTTP/2 multiplexing
- **Language Agnostic**: Support for 10+ languages
- **Streaming**: Bidirectional streaming support
- **Strongly Typed**: Protocol Buffers schema
- **Code Generation**: Auto-generate client/server code
- **Deadline/Timeout**: Built-in timeout support

### gRPC vs REST
```
Feature          | gRPC              | REST
-----------------|-------------------|------------------
Protocol         | HTTP/2            | HTTP/1.1
Payload          | Protobuf (binary) | JSON (text)
Performance      | 5-10x faster      | Slower
Streaming        | Bidirectional     | Limited
Browser Support  | Limited           | Full
Human Readable   | No                | Yes
```

### When to Use gRPC
✅ Microservices communication  
✅ Real-time streaming  
✅ High-performance requirements  
✅ Polyglot environments  
✅ Mobile-to-backend communication  

❌ Browser-based applications (use REST)  
❌ Public APIs (use REST for compatibility)  

---

## Installation & Setup

### Prerequisites
```bash
# Java 17+
java -version

# Maven 3.8+
mvn -version

# Protocol Buffers Compiler
brew install protobuf
protoc --version
```

### Maven Dependencies
```xml
<properties>
    <grpc.version>1.60.0</grpc.version>
    <protobuf.version>3.25.1</protobuf.version>
</properties>

<dependencies>
    <!-- gRPC -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    
    <!-- Protocol Buffers -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
    </dependency>
    
    <!-- Annotations -->
    <dependency>
        <groupId>javax.annotation</groupId>
        <artifactId>javax.annotation-api</artifactId>
        <version>1.3.2</version>
    </dependency>
</dependencies>

<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>
                    com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}
                </protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>
                    io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}
                </pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Protocol Buffers

### Basic Syntax
```protobuf
// user.proto
syntax = "proto3";

package com.example.grpc;

option java_multiple_files = true;
option java_package = "com.example.grpc";
option java_outer_classname = "UserProto";

// Message definition
message User {
    string user_id = 1;
    string email = 2;
    string name = 3;
    int32 age = 4;
    repeated string tags = 5;
}
```

### Data Types
```protobuf
message DataTypes {
    // Numeric
    int32 int_field = 1;
    int64 long_field = 2;
    float float_field = 3;
    double double_field = 4;
    
    // Boolean
    bool bool_field = 5;
    
    // String
    string string_field = 6;
    bytes bytes_field = 7;
    
    // Repeated (array)
    repeated string tags = 8;
    
    // Map
    map<string, string> attributes = 9;
    
    // Nested message
    Address address = 10;
    
    // Enum
    Status status = 11;
}

message Address {
    string street = 1;
    string city = 2;
    string zip_code = 3;
}

enum Status {
    UNKNOWN = 0;
    ACTIVE = 1;
    INACTIVE = 2;
}
```

### Field Rules
```protobuf
message FieldRules {
    // Optional (default in proto3)
    string optional_field = 1;
    
    // Repeated (array/list)
    repeated string repeated_field = 2;
    
    // Map
    map<string, int32> map_field = 3;
    
    // Oneof (union type)
    oneof payment_method {
        string credit_card = 4;
        string paypal = 5;
        string bank_account = 6;
    }
}
```

---

## Service Definition

### Unary RPC (Request-Response)
```protobuf
// user_service.proto
syntax = "proto3";

package com.example.grpc;

option java_multiple_files = true;
option java_package = "com.example.grpc";

service UserService {
    // Unary RPC
    rpc GetUser(GetUserRequest) returns (GetUserResponse);
    rpc CreateUser(CreateUserRequest) returns (CreateUserResponse);
    rpc UpdateUser(UpdateUserRequest) returns (UpdateUserResponse);
    rpc DeleteUser(DeleteUserRequest) returns (DeleteUserResponse);
}

message GetUserRequest {
    string user_id = 1;
}

message GetUserResponse {
    User user = 1;
}

message CreateUserRequest {
    string email = 1;
    string name = 2;
    int32 age = 3;
}

message CreateUserResponse {
    User user = 1;
}

message User {
    string user_id = 1;
    string email = 2;
    string name = 3;
    int32 age = 4;
}
```

### Server Streaming
```protobuf
service OrderService {
    // Server streaming: One request, multiple responses
    rpc ListOrders(ListOrdersRequest) returns (stream Order);
}

message ListOrdersRequest {
    string user_id = 1;
}

message Order {
    string order_id = 1;
    string user_id = 2;
    double total_amount = 3;
}
```

### Client Streaming
```protobuf
service MetricsService {
    // Client streaming: Multiple requests, one response
    rpc RecordMetrics(stream Metric) returns (MetricsSummary);
}

message Metric {
    string name = 1;
    double value = 2;
    int64 timestamp = 3;
}

message MetricsSummary {
    int32 total_count = 1;
    double average_value = 2;
}
```

### Bidirectional Streaming
```protobuf
service ChatService {
    // Bidirectional streaming: Multiple requests and responses
    rpc Chat(stream ChatMessage) returns (stream ChatMessage);
}

message ChatMessage {
    string user_id = 1;
    string message = 2;
    int64 timestamp = 3;
}
```

---

## Basic Implementation

### Generate Code
```bash
# Compile proto files
mvn clean compile

# Generated files location
target/generated-sources/protobuf/java/
target/generated-sources/protobuf/grpc-java/
```

### Server Implementation
```java
// UserServiceImpl.java
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        String userId = request.getUserId();
        User user = users.get(userId);
        
        if (user == null) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("User not found: " + userId)
                    .asRuntimeException()
            );
            return;
        }
        
        GetUserResponse response = GetUserResponse.newBuilder()
            .setUser(user)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        String userId = UUID.randomUUID().toString();
        
        User user = User.newBuilder()
            .setUserId(userId)
            .setEmail(request.getEmail())
            .setName(request.getName())
            .setAge(request.getAge())
            .build();
        
        users.put(userId, user);
        
        CreateUserResponse response = CreateUserResponse.newBuilder()
            .setUser(user)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

### Start Server
```java
public class GrpcServer {
    
    private Server server;
    
    public void start() throws IOException {
        int port = 9090;
        
        server = ServerBuilder.forPort(port)
            .addService(new UserServiceImpl())
            .build()
            .start();
        
        System.out.println("Server started on port " + port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server");
            GrpcServer.this.stop();
        }));
    }
    
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    
    public static void main(String[] args) throws Exception {
        GrpcServer server = new GrpcServer();
        server.start();
        server.blockUntilShutdown();
    }
}
```

### Client Implementation
```java
public class GrpcClient {
    
    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    
    public GrpcClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
        
        blockingStub = UserServiceGrpc.newBlockingStub(channel);
    }
    
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    
    public User getUser(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
            .setUserId(userId)
            .build();
        
        GetUserResponse response = blockingStub.getUser(request);
        return response.getUser();
    }
    
    public User createUser(String email, String name, int age) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setEmail(email)
            .setName(name)
            .setAge(age)
            .build();
        
        CreateUserResponse response = blockingStub.createUser(request);
        return response.getUser();
    }
    
    public static void main(String[] args) throws Exception {
        GrpcClient client = new GrpcClient("localhost", 9090);
        
        try {
            // Create user
            User user = client.createUser("john@example.com", "John Doe", 30);
            System.out.println("Created user: " + user.getUserId());
            
            // Get user
            User retrieved = client.getUser(user.getUserId());
            System.out.println("Retrieved user: " + retrieved.getName());
            
        } finally {
            client.shutdown();
        }
    }
}
```

---

## Practical Example: Product Service

### Proto Definition
```protobuf
// product_service.proto
syntax = "proto3";

package com.example.grpc;

option java_multiple_files = true;
option java_package = "com.example.grpc";

service ProductService {
    rpc GetProduct(GetProductRequest) returns (GetProductResponse);
    rpc CreateProduct(CreateProductRequest) returns (CreateProductResponse);
    rpc ListProducts(ListProductsRequest) returns (stream Product);
    rpc UpdateStock(UpdateStockRequest) returns (UpdateStockResponse);
}

message Product {
    string product_id = 1;
    string name = 2;
    double price = 3;
    int32 stock = 4;
    string category = 5;
}

message GetProductRequest {
    string product_id = 1;
}

message GetProductResponse {
    Product product = 1;
}

message CreateProductRequest {
    string name = 1;
    double price = 2;
    int32 stock = 3;
    string category = 4;
}

message CreateProductResponse {
    Product product = 1;
}

message ListProductsRequest {
    string category = 1;
}

message UpdateStockRequest {
    string product_id = 1;
    int32 quantity = 2;
}

message UpdateStockResponse {
    Product product = 1;
}
```

### Server Implementation
```java
public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    
    @Override
    public void createProduct(CreateProductRequest request, 
                             StreamObserver<CreateProductResponse> responseObserver) {
        String productId = UUID.randomUUID().toString();
        
        Product product = Product.newBuilder()
            .setProductId(productId)
            .setName(request.getName())
            .setPrice(request.getPrice())
            .setStock(request.getStock())
            .setCategory(request.getCategory())
            .build();
        
        products.put(productId, product);
        
        CreateProductResponse response = CreateProductResponse.newBuilder()
            .setProduct(product)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void listProducts(ListProductsRequest request, 
                            StreamObserver<Product> responseObserver) {
        String category = request.getCategory();
        
        products.values().stream()
            .filter(p -> category.isEmpty() || p.getCategory().equals(category))
            .forEach(responseObserver::onNext);
        
        responseObserver.onCompleted();
    }
}
```

---

## Next Steps

Continue to [Part 2: Advanced Features & Streaming](./02_gRPC_Advanced_Features.md)
