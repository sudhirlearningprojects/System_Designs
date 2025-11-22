# Testing Spring WebFlux Applications

## Overview

Testing reactive applications requires special tools and techniques. Spring provides WebTestClient and Reactor Test for comprehensive testing.

## Dependencies

```xml
<dependencies>
    <!-- WebFlux Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Reactor Test -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Unit Testing with StepVerifier

### Testing Mono

```java
@Test
void testMonoSuccess() {
    Mono<String> mono = Mono.just("Hello");
    
    StepVerifier.create(mono)
        .expectNext("Hello")
        .verifyComplete();
}

@Test
void testMonoEmpty() {
    Mono<String> mono = Mono.empty();
    
    StepVerifier.create(mono)
        .verifyComplete();
}

@Test
void testMonoError() {
    Mono<String> mono = Mono.error(new RuntimeException("Error"));
    
    StepVerifier.create(mono)
        .expectError(RuntimeException.class)
        .verify();
}
```

### Testing Flux

```java
@Test
void testFluxMultipleElements() {
    Flux<Integer> flux = Flux.just(1, 2, 3, 4, 5);
    
    StepVerifier.create(flux)
        .expectNext(1)
        .expectNext(2)
        .expectNext(3)
        .expectNext(4)
        .expectNext(5)
        .verifyComplete();
}

@Test
void testFluxWithPredicate() {
    Flux<Integer> flux = Flux.range(1, 10);
    
    StepVerifier.create(flux)
        .expectNextMatches(n -> n == 1)
        .expectNextMatches(n -> n == 2)
        .expectNextCount(8)
        .verifyComplete();
}

@Test
void testFluxError() {
    Flux<Integer> flux = Flux.range(1, 5)
        .map(i -> {
            if (i == 3) throw new RuntimeException("Error at 3");
            return i;
        });
    
    StepVerifier.create(flux)
        .expectNext(1, 2)
        .expectError(RuntimeException.class)
        .verify();
}
```

### Testing with Virtual Time

```java
@Test
void testDelayedFlux() {
    Flux<Long> flux = Flux.interval(Duration.ofSeconds(1)).take(3);
    
    StepVerifier.withVirtualTime(() -> flux)
        .expectSubscription()
        .expectNoEvent(Duration.ofSeconds(1))
        .expectNext(0L)
        .thenAwait(Duration.ofSeconds(1))
        .expectNext(1L)
        .thenAwait(Duration.ofSeconds(1))
        .expectNext(2L)
        .verifyComplete();
}
```

## Testing Repositories

```java
@DataR2dbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }
    
    @Test
    void testSaveUser() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        
        StepVerifier.create(userRepository.save(user))
            .assertNext(saved -> {
                assertNotNull(saved.getId());
                assertEquals("john", saved.getUsername());
                assertEquals("john@example.com", saved.getEmail());
            })
            .verifyComplete();
    }
    
    @Test
    void testFindByUsername() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        
        userRepository.save(user).block();
        
        StepVerifier.create(userRepository.findByUsername("john"))
            .assertNext(found -> {
                assertEquals("john", found.getUsername());
                assertEquals("john@example.com", found.getEmail());
            })
            .verifyComplete();
    }
    
    @Test
    void testFindAll() {
        User user1 = new User();
        user1.setUsername("john");
        user1.setEmail("john@example.com");
        
        User user2 = new User();
        user2.setUsername("jane");
        user2.setEmail("jane@example.com");
        
        userRepository.saveAll(Arrays.asList(user1, user2)).blockLast();
        
        StepVerifier.create(userRepository.findAll())
            .expectNextCount(2)
            .verifyComplete();
    }
}
```

## Testing Services

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testCreateUser() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("john");
        savedUser.setEmail("john@example.com");
        
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        
        StepVerifier.create(userService.createUser(user))
            .assertNext(result -> {
                assertEquals(1L, result.getId());
                assertEquals("john", result.getUsername());
            })
            .verifyComplete();
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testGetUserById_Found() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        
        when(userRepository.findById(1L)).thenReturn(Mono.just(user));
        
        StepVerifier.create(userService.getUserById(1L))
            .assertNext(result -> assertEquals("john", result.getUsername()))
            .verifyComplete();
    }
    
    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Mono.empty());
        
        StepVerifier.create(userService.getUserById(1L))
            .expectError(UserNotFoundException.class)
            .verify();
    }
    
    @Test
    void testUpdateUser() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("john");
        existing.setEmail("john@example.com");
        
        User updated = new User();
        updated.setUsername("john_updated");
        updated.setEmail("john.new@example.com");
        
        when(userRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(existing));
        
        StepVerifier.create(userService.updateUser(1L, updated))
            .assertNext(result -> {
                assertEquals("john_updated", result.getUsername());
                assertEquals("john.new@example.com", result.getEmail());
            })
            .verifyComplete();
    }
}
```

## Testing Controllers with WebTestClient

### Setup

```java
@WebFluxTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private UserService userService;
    
    // Tests here
}
```

### GET Requests

```java
@Test
void testGetUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("john");
    user.setEmail("john@example.com");
    
    when(userService.getUserById(1L)).thenReturn(Mono.just(user));
    
    webTestClient.get()
        .uri("/api/users/1")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.id").isEqualTo(1)
        .jsonPath("$.username").isEqualTo("john")
        .jsonPath("$.email").isEqualTo("john@example.com");
}

@Test
void testGetUser_NotFound() {
    when(userService.getUserById(1L)).thenReturn(Mono.error(new UserNotFoundException(1L)));
    
    webTestClient.get()
        .uri("/api/users/1")
        .exchange()
        .expectStatus().isNotFound();
}

@Test
void testGetAllUsers() {
    List<User> users = Arrays.asList(
        new User(1L, "john", "john@example.com"),
        new User(2L, "jane", "jane@example.com")
    );
    
    when(userService.getAllUsers()).thenReturn(Flux.fromIterable(users));
    
    webTestClient.get()
        .uri("/api/users")
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(User.class)
        .hasSize(2)
        .contains(users.toArray(new User[0]));
}
```

### POST Requests

```java
@Test
void testCreateUser() {
    User user = new User();
    user.setUsername("john");
    user.setEmail("john@example.com");
    
    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("john");
    savedUser.setEmail("john@example.com");
    
    when(userService.createUser(any(User.class))).thenReturn(Mono.just(savedUser));
    
    webTestClient.post()
        .uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(user)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isEqualTo(1)
        .jsonPath("$.username").isEqualTo("john");
}

@Test
void testCreateUser_ValidationError() {
    User user = new User();
    user.setUsername(""); // Invalid
    
    webTestClient.post()
        .uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(user)
        .exchange()
        .expectStatus().isBadRequest();
}
```

### PUT Requests

```java
@Test
void testUpdateUser() {
    User user = new User();
    user.setUsername("john_updated");
    user.setEmail("john.new@example.com");
    
    User updatedUser = new User();
    updatedUser.setId(1L);
    updatedUser.setUsername("john_updated");
    updatedUser.setEmail("john.new@example.com");
    
    when(userService.updateUser(eq(1L), any(User.class))).thenReturn(Mono.just(updatedUser));
    
    webTestClient.put()
        .uri("/api/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(user)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.username").isEqualTo("john_updated");
}
```

### DELETE Requests

```java
@Test
void testDeleteUser() {
    when(userService.deleteUser(1L)).thenReturn(Mono.empty());
    
    webTestClient.delete()
        .uri("/api/users/1")
        .exchange()
        .expectStatus().isNoContent();
}
```

## Testing Streaming Endpoints

```java
@Test
void testStreamUsers() {
    Flux<User> users = Flux.interval(Duration.ofMillis(100))
        .take(3)
        .map(i -> new User(i, "user" + i, "user" + i + "@example.com"));
    
    when(userService.streamUsers()).thenReturn(users);
    
    webTestClient.get()
        .uri("/api/users/stream")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
        .expectBodyList(User.class)
        .hasSize(3);
}
```

## Testing WebClient

```java
@Test
void testWebClientGetUser() {
    MockWebServer mockServer = new MockWebServer();
    mockServer.enqueue(new MockResponse()
        .setBody("{\"id\":1,\"username\":\"john\"}")
        .addHeader("Content-Type", "application/json"));
    
    WebClient webClient = WebClient.create(mockServer.url("/").toString());
    
    Mono<User> result = webClient.get()
        .uri("/users/1")
        .retrieve()
        .bodyToMono(User.class);
    
    StepVerifier.create(result)
        .assertNext(user -> {
            assertEquals(1L, user.getId());
            assertEquals("john", user.getUsername());
        })
        .verifyComplete();
    
    mockServer.shutdown();
}

@Test
void testWebClientError() {
    MockWebServer mockServer = new MockWebServer();
    mockServer.enqueue(new MockResponse().setResponseCode(404));
    
    WebClient webClient = WebClient.create(mockServer.url("/").toString());
    
    Mono<User> result = webClient.get()
        .uri("/users/1")
        .retrieve()
        .bodyToMono(User.class);
    
    StepVerifier.create(result)
        .expectError(WebClientResponseException.NotFound.class)
        .verify();
    
    mockServer.shutdown();
}
```

## Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UserIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }
    
    @Test
    void testCreateAndGetUser() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        
        // Create user
        User created = webTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(User.class)
            .returnResult()
            .getResponseBody();
        
        assertNotNull(created);
        assertNotNull(created.getId());
        
        // Get user
        webTestClient.get()
            .uri("/api/users/" + created.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.username").isEqualTo("john");
    }
}
```

## Testing Error Scenarios

```java
@Test
void testErrorHandling() {
    when(userService.getUserById(1L))
        .thenReturn(Mono.error(new UserNotFoundException(1L)));
    
    webTestClient.get()
        .uri("/api/users/1")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.error").isEqualTo("Not Found")
        .jsonPath("$.message").exists();
}

@Test
void testValidationError() {
    User user = new User();
    user.setUsername(""); // Invalid
    user.setEmail("invalid-email"); // Invalid
    
    webTestClient.post()
        .uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(user)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.errors.username").exists()
        .jsonPath("$.errors.email").exists();
}
```

## Performance Testing

```java
@Test
void testConcurrentRequests() {
    User user = new User(1L, "john", "john@example.com");
    when(userService.getUserById(1L)).thenReturn(Mono.just(user));
    
    int concurrentRequests = 100;
    CountDownLatch latch = new CountDownLatch(concurrentRequests);
    
    for (int i = 0; i < concurrentRequests; i++) {
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(User.class)
            .consumeWith(result -> latch.countDown());
    }
    
    assertDoesNotThrow(() -> latch.await(10, TimeUnit.SECONDS));
}
```

## Best Practices

1. **Use StepVerifier**: For testing Mono and Flux
2. **Mock dependencies**: Use @MockBean for services
3. **Test error scenarios**: Include negative test cases
4. **Use WebTestClient**: For integration tests
5. **Test streaming**: Verify SSE and streaming endpoints
6. **Virtual time**: For testing delays and timeouts
7. **Clean up**: Reset state between tests
8. **Test concurrency**: Verify thread safety
9. **Integration tests**: Test full request/response cycle
10. **Performance tests**: Verify scalability

## Common Patterns

### Testing Timeout

```java
@Test
void testTimeout() {
    Mono<User> delayed = Mono.delay(Duration.ofSeconds(10))
        .thenReturn(new User());
    
    when(userService.getUserById(1L)).thenReturn(delayed);
    
    StepVerifier.create(delayed.timeout(Duration.ofSeconds(1)))
        .expectError(TimeoutException.class)
        .verify();
}
```

### Testing Retry

```java
@Test
void testRetry() {
    AtomicInteger attempts = new AtomicInteger(0);
    
    Mono<User> mono = Mono.defer(() -> {
        if (attempts.incrementAndGet() < 3) {
            return Mono.error(new RuntimeException("Temporary error"));
        }
        return Mono.just(new User());
    }).retry(3);
    
    StepVerifier.create(mono)
        .expectNextCount(1)
        .verifyComplete();
    
    assertEquals(3, attempts.get());
}
```

### Testing Backpressure

```java
@Test
void testBackpressure() {
    Flux<Integer> flux = Flux.range(1, 100);
    
    StepVerifier.create(flux, 10)
        .expectNextCount(10)
        .thenRequest(10)
        .expectNextCount(10)
        .thenCancel()
        .verify();
}
```

## Next Steps

- [Performance](Performance.md) - Performance optimization
- [Examples](Examples.md) - Real-world examples
