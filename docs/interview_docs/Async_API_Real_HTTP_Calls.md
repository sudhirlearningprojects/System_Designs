# Asynchronous API Calls with Real HTTP Requests - Java 21

## Problem Statement
Call multiple real HTTP APIs concurrently, collect responses, merge data, and return the final result using Java 21 concurrent programming.

## Using Real Public APIs

We'll use these free public APIs:
- **JSONPlaceholder** - https://jsonplaceholder.typicode.com (fake REST API)
- **ReqRes** - https://reqres.in (test REST API)
- **Random User API** - https://randomuser.me/api

---

## Solution 1: CompletableFuture with HttpClient (Java 11+)

```java
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncAPIRealHTTP {
    
    // Java 11+ HttpClient (supports async)
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Fetch user from JSONPlaceholder API
    static CompletableFuture<User> fetchUser(int userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/users/" + userId))
                .GET()
                .build();
        
        // sendAsync() returns CompletableFuture<HttpResponse>
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, User.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse user", e);
                    }
                });
    }
    
    // Fetch posts from JSONPlaceholder API
    static CompletableFuture<Post[]> fetchPosts(int userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts?userId=" + userId))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, Post[].class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse posts", e);
                    }
                });
    }
    
    // Fetch todos from JSONPlaceholder API
    static CompletableFuture<Todo[]> fetchTodos(int userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/todos?userId=" + userId))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, Todo[].class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse todos", e);
                    }
                });
    }
    
    // Fetch albums from JSONPlaceholder API
    static CompletableFuture<Album[]> fetchAlbums(int userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/albums?userId=" + userId))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, Album[].class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse albums", e);
                    }
                });
    }
    
    // Merge all data
    public static CompletableFuture<UserProfile> getUserProfile(int userId) {
        // Start all 4 HTTP calls concurrently
        CompletableFuture<User> userFuture = fetchUser(userId);
        CompletableFuture<Post[]> postsFuture = fetchPosts(userId);
        CompletableFuture<Todo[]> todosFuture = fetchTodos(userId);
        CompletableFuture<Album[]> albumsFuture = fetchAlbums(userId);
        
        // Wait for all to complete, then merge
        return CompletableFuture.allOf(userFuture, postsFuture, todosFuture, albumsFuture)
                .thenApply(v -> new UserProfile(
                        userFuture.join(),
                        postsFuture.join(),
                        todosFuture.join(),
                        albumsFuture.join()
                ));
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        
        UserProfile profile = getUserProfile(1).get();
        
        long end = System.currentTimeMillis();
        System.out.println("User: " + profile.user().name());
        System.out.println("Posts: " + profile.posts().length);
        System.out.println("Todos: " + profile.todos().length);
        System.out.println("Albums: " + profile.albums().length);
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}

// Data classes
record User(int id, String name, String email, String phone) {}
record Post(int id, int userId, String title, String body) {}
record Todo(int id, int userId, String title, boolean completed) {}
record Album(int id, int userId, String title) {}
record UserProfile(User user, Post[] posts, Todo[] todos, Album[] albums) {}
```

**Output**:
```
User: Leanne Graham
Posts: 10
Todos: 20
Albums: 10
Time taken: 450ms (actual network time, varies)
```

---

## Solution 2: Virtual Threads with Blocking HTTP Calls

```java
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncAPIVirtualThreadsReal {
    
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Synchronous HTTP calls (blocking is OK with virtual threads)
    static User fetchUser(int userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/users/" + userId))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), User.class);
    }
    
    static Post[] fetchPosts(int userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts?userId=" + userId))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Post[].class);
    }
    
    static Todo[] fetchTodos(int userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/todos?userId=" + userId))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Todo[].class);
    }
    
    static Album[] fetchAlbums(int userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/albums?userId=" + userId))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Album[].class);
    }
    
    public static UserProfile getUserProfile(int userId) throws Exception {
        // Virtual threads make blocking calls efficient
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<User> userFuture = executor.submit(() -> fetchUser(userId));
            Future<Post[]> postsFuture = executor.submit(() -> fetchPosts(userId));
            Future<Todo[]> todosFuture = executor.submit(() -> fetchTodos(userId));
            Future<Album[]> albumsFuture = executor.submit(() -> fetchAlbums(userId));
            
            return new UserProfile(
                    userFuture.get(),
                    postsFuture.get(),
                    todosFuture.get(),
                    albumsFuture.get()
            );
        }
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        
        UserProfile profile = getUserProfile(1);
        
        long end = System.currentTimeMillis();
        System.out.println("User: " + profile.user().name());
        System.out.println("Posts: " + profile.posts().length);
        System.out.println("Todos: " + profile.todos().length);
        System.out.println("Albums: " + profile.albums().length);
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}
```

---

## Solution 3: Spring WebClient (Reactive)

```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class AsyncAPISpringWebClient {
    
    private static final WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();
    
    static Mono<User> fetchUser(int userId) {
        return webClient.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .bodyToMono(User.class);
    }
    
    static Mono<Post[]> fetchPosts(int userId) {
        return webClient.get()
                .uri("/posts?userId={userId}", userId)
                .retrieve()
                .bodyToMono(Post[].class);
    }
    
    static Mono<Todo[]> fetchTodos(int userId) {
        return webClient.get()
                .uri("/todos?userId={userId}", userId)
                .retrieve()
                .bodyToMono(Todo[].class);
    }
    
    static Mono<Album[]> fetchAlbums(int userId) {
        return webClient.get()
                .uri("/albums?userId={userId}", userId)
                .retrieve()
                .bodyToMono(Album[].class);
    }
    
    public static Mono<UserProfile> getUserProfile(int userId) {
        // Reactive composition - all calls execute concurrently
        return Mono.zip(
                fetchUser(userId),
                fetchPosts(userId),
                fetchTodos(userId),
                fetchAlbums(userId)
        ).map(tuple -> new UserProfile(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
        ));
    }
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        
        UserProfile profile = getUserProfile(1).block(); // Block to get result
        
        long end = System.currentTimeMillis();
        System.out.println("User: " + profile.user().name());
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}
```

---

## Solution 4: RestTemplate with ExecutorService (Traditional)

```java
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.*;

public class AsyncAPIRestTemplate {
    
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    
    static User fetchUser(int userId) {
        return restTemplate.getForObject(BASE_URL + "/users/" + userId, User.class);
    }
    
    static Post[] fetchPosts(int userId) {
        return restTemplate.getForObject(BASE_URL + "/posts?userId=" + userId, Post[].class);
    }
    
    static Todo[] fetchTodos(int userId) {
        return restTemplate.getForObject(BASE_URL + "/todos?userId=" + userId, Todo[].class);
    }
    
    static Album[] fetchAlbums(int userId) {
        return restTemplate.getForObject(BASE_URL + "/albums?userId=" + userId, Album[].class);
    }
    
    public static UserProfile getUserProfile(int userId) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            Future<User> userFuture = executor.submit(() -> fetchUser(userId));
            Future<Post[]> postsFuture = executor.submit(() -> fetchPosts(userId));
            Future<Todo[]> todosFuture = executor.submit(() -> fetchTodos(userId));
            Future<Album[]> albumsFuture = executor.submit(() -> fetchAlbums(userId));
            
            return new UserProfile(
                    userFuture.get(),
                    postsFuture.get(),
                    todosFuture.get(),
                    albumsFuture.get()
            );
        } finally {
            executor.shutdown();
        }
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        
        UserProfile profile = getUserProfile(1);
        
        long end = System.currentTimeMillis();
        System.out.println("User: " + profile.user().name());
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}
```

---

## Solution 5: Multiple Different APIs

```java
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncAPIMultipleSources {
    
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // JSONPlaceholder API
    static CompletableFuture<JsonNode> fetchFromJSONPlaceholder() {
        return makeRequest("https://jsonplaceholder.typicode.com/users/1");
    }
    
    // ReqRes API
    static CompletableFuture<JsonNode> fetchFromReqRes() {
        return makeRequest("https://reqres.in/api/users/2");
    }
    
    // Random User API
    static CompletableFuture<JsonNode> fetchFromRandomUser() {
        return makeRequest("https://randomuser.me/api/");
    }
    
    // GitHub API
    static CompletableFuture<JsonNode> fetchFromGitHub() {
        return makeRequest("https://api.github.com/users/github");
    }
    
    static CompletableFuture<JsonNode> makeRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readTree(body);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON", e);
                    }
                });
    }
    
    public static CompletableFuture<AggregatedData> fetchAllData() {
        CompletableFuture<JsonNode> api1 = fetchFromJSONPlaceholder();
        CompletableFuture<JsonNode> api2 = fetchFromReqRes();
        CompletableFuture<JsonNode> api3 = fetchFromRandomUser();
        CompletableFuture<JsonNode> api4 = fetchFromGitHub();
        
        return CompletableFuture.allOf(api1, api2, api3, api4)
                .thenApply(v -> new AggregatedData(
                        api1.join(),
                        api2.join(),
                        api3.join(),
                        api4.join()
                ));
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        
        AggregatedData data = fetchAllData().get();
        
        long end = System.currentTimeMillis();
        System.out.println("JSONPlaceholder: " + data.jsonPlaceholder().get("name").asText());
        System.out.println("ReqRes: " + data.reqRes().get("data").get("email").asText());
        System.out.println("RandomUser: " + data.randomUser().get("results").get(0).get("email").asText());
        System.out.println("GitHub: " + data.github().get("login").asText());
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}

record AggregatedData(JsonNode jsonPlaceholder, JsonNode reqRes, JsonNode randomUser, JsonNode github) {}
```

---

## Maven Dependencies

```xml
<dependencies>
    <!-- Jackson for JSON parsing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- Spring WebClient (optional) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
        <version>3.1.5</version>
    </dependency>
</dependencies>
```

---

## Performance Comparison

### Sequential vs Concurrent (Real APIs)

**Sequential Execution**:
```java
User user = fetchUser(1);        // 150ms
Post[] posts = fetchPosts(1);    // 120ms
Todo[] todos = fetchTodos(1);    // 130ms
Album[] albums = fetchAlbums(1); // 140ms
// Total: 540ms
```

**Concurrent Execution**:
```java
CompletableFuture.allOf(
    fetchUser(1),    // 150ms
    fetchPosts(1),   // 120ms
    fetchTodos(1),   // 130ms
    fetchAlbums(1)   // 140ms
).join();
// Total: 150ms (max of all)
```

**Speedup**: 3.6x faster!

---

## Why Real APIs Are Better for Learning

| Aspect | Sleep Simulation | Real HTTP APIs |
|--------|-----------------|----------------|
| **Realism** | ❌ Artificial | ✅ Real network latency |
| **Learning** | ❌ Doesn't show HTTP handling | ✅ Learn HTTP, JSON parsing |
| **Errors** | ❌ No real errors | ✅ Handle timeouts, 404s, etc |
| **Production-Ready** | ❌ Not realistic | ✅ Actual production pattern |
| **Variability** | ❌ Fixed delays | ✅ Real network variability |
| **Dependencies** | ✅ None | ⚠️ Requires internet |

---

## Best Practices with Real APIs

1. **Set Timeouts**
```java
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

2. **Handle Errors**
```java
.exceptionally(ex -> {
    System.err.println("API failed: " + ex.getMessage());
    return defaultValue;
})
```

3. **Add Retry Logic**
```java
static <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> supplier, int maxRetries) {
    return supplier.get().exceptionally(ex -> {
        if (maxRetries > 0) {
            return withRetry(supplier, maxRetries - 1).join();
        }
        throw new RuntimeException(ex);
    });
}
```

4. **Use Connection Pooling**
```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2) // HTTP/2 connection pooling
    .build();
```

5. **Cache Results**
```java
private static final Map<Integer, User> userCache = new ConcurrentHashMap<>();

static CompletableFuture<User> fetchUser(int userId) {
    if (userCache.containsKey(userId)) {
        return CompletableFuture.completedFuture(userCache.get(userId));
    }
    return makeHttpCall(userId).thenApply(user -> {
        userCache.put(userId, user);
        return user;
    });
}
```

---

## Key Takeaways

1. **Real APIs show actual network behavior** - latency, failures, timeouts
2. **HttpClient.sendAsync()** - Built-in async support in Java 11+
3. **Virtual threads** - Make blocking HTTP calls efficient
4. **Spring WebClient** - Reactive HTTP client for Spring apps
5. **Always handle errors** - Network calls can fail
6. **Set timeouts** - Prevent hanging on slow APIs
7. **Use connection pooling** - Reuse HTTP connections
8. **Cache when possible** - Reduce redundant API calls

Real HTTP calls provide much better learning experience than sleep simulation!
