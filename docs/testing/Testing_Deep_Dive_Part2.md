# Testing Deep Dive - Part 2

## REST API Testing

### MockMvc

```java
@WebMvcTest(UserController.class)
class UserControllerMockMvcTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetAllUsers() throws Exception {
        List<User> users = List.of(
            new User(1L, "alice@example.com", "Alice"),
            new User(2L, "bob@example.com", "Bob")
        );
        
        when(userService.findAll()).thenReturn(users);
        
        mockMvc.perform(get("/api/users")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].email").value("alice@example.com"))
            .andExpect(jsonPath("$[1].email").value("bob@example.com"))
            .andDo(print());
    }
    
    @Test
    void testCreateUserValidation() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "invalid-email",
                        "name": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.name").exists());
    }
    
    @Test
    void testUpdateUser() throws Exception {
        User updated = new User(1L, "john@example.com", "John Updated");
        when(userService.update(eq(1L), any())).thenReturn(updated);
        
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "john@example.com",
                        "name": "John Updated"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John Updated"));
    }
    
    @Test
    void testDeleteUser() throws Exception {
        doNothing().when(userService).delete(1L);
        
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());
        
        verify(userService).delete(1L);
    }
    
    @Test
    void testUserNotFound() throws Exception {
        when(userService.findById(999L))
            .thenThrow(new UserNotFoundException("User not found"));
        
        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found"));
    }
}
```

### RestAssured

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiRestAssuredTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }
    
    @Test
    void testGetUser() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/users/1")
        .then()
            .statusCode(200)
            .body("email", equalTo("john@example.com"))
            .body("name", equalTo("John"));
    }
    
    @Test
    void testCreateUser() {
        String requestBody = """
            {
                "email": "new@example.com",
                "name": "New User"
            }
            """;
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .header("Location", notNullValue())
            .body("id", notNullValue())
            .body("email", equalTo("new@example.com"));
    }
    
    @Test
    void testSearchUsers() {
        given()
            .queryParam("name", "John")
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/users/search")
        .then()
            .statusCode(200)
            .body("content", hasSize(greaterThan(0)))
            .body("content[0].name", containsString("John"));
    }
    
    @Test
    void testAuthenticatedRequest() {
        String token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        given()
            .header("Authorization", token)
        .when()
            .get("/users/me")
        .then()
            .statusCode(200)
            .body("email", notNullValue());
    }
}
```

### WebTestClient (Reactive)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiWebTestClientTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void testGetUser() {
        webTestClient.get()
            .uri("/api/users/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.email").isEqualTo("john@example.com")
            .jsonPath("$.name").isEqualTo("John");
    }
    
    @Test
    void testCreateUser() {
        UserDTO dto = new UserDTO("new@example.com", "New User");
        
        webTestClient.post()
            .uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dto)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().exists("Location")
            .expectBody(User.class)
            .value(user -> {
                assertThat(user.getId()).isNotNull();
                assertThat(user.getEmail()).isEqualTo("new@example.com");
            });
    }
    
    @Test
    void testGetAllUsers() {
        webTestClient.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(User.class)
            .hasSize(2)
            .consumeWith(response -> {
                List<User> users = response.getResponseBody();
                assertThat(users).extracting(User::getEmail)
                    .contains("alice@example.com", "bob@example.com");
            });
    }
}
```

---

## Reactive Testing

### Reactor Test

```xml
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Testing Mono

```java
class MonoTest {
    
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
    
    @Test
    void testMonoWithDelay() {
        Mono<String> mono = Mono.just("Delayed")
            .delayElement(Duration.ofSeconds(1));
        
        StepVerifier.create(mono)
            .expectNext("Delayed")
            .expectComplete()
            .verify(Duration.ofSeconds(2));
    }
}
```

### Testing Flux

```java
class FluxTest {
    
    @Test
    void testFluxElements() {
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
    
    @Test
    void testFluxBackpressure() {
        Flux<Integer> flux = Flux.range(1, 100);
        
        StepVerifier.create(flux, 10)
            .expectNextCount(10)
            .thenRequest(10)
            .expectNextCount(10)
            .thenCancel()
            .verify();
    }
}
```

### Testing Reactive Services

```java
@ExtendWith(MockitoExtension.class)
class ReactiveUserServiceTest {
    
    @Mock
    private ReactiveUserRepository repository;
    
    @InjectMocks
    private ReactiveUserService service;
    
    @Test
    void testFindById() {
        User user = new User(1L, "john@example.com", "John");
        when(repository.findById(1L)).thenReturn(Mono.just(user));
        
        StepVerifier.create(service.findById(1L))
            .expectNext(user)
            .verifyComplete();
    }
    
    @Test
    void testFindAll() {
        List<User> users = List.of(
            new User(1L, "alice@example.com", "Alice"),
            new User(2L, "bob@example.com", "Bob")
        );
        when(repository.findAll()).thenReturn(Flux.fromIterable(users));
        
        StepVerifier.create(service.findAll())
            .expectNext(users.get(0))
            .expectNext(users.get(1))
            .verifyComplete();
    }
    
    @Test
    void testSaveUser() {
        User user = new User(null, "new@example.com", "New");
        User saved = new User(1L, "new@example.com", "New");
        
        when(repository.save(user)).thenReturn(Mono.just(saved));
        
        StepVerifier.create(service.save(user))
            .assertNext(u -> {
                assertThat(u.getId()).isEqualTo(1L);
                assertThat(u.getEmail()).isEqualTo("new@example.com");
            })
            .verifyComplete();
    }
}
```

### Testing WebFlux Controllers

```java
@WebFluxTest(UserController.class)
class ReactiveUserControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private ReactiveUserService userService;
    
    @Test
    void testGetUser() {
        User user = new User(1L, "john@example.com", "John");
        when(userService.findById(1L)).thenReturn(Mono.just(user));
        
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(User.class)
            .isEqualTo(user);
    }
    
    @Test
    void testStreamUsers() {
        Flux<User> users = Flux.just(
            new User(1L, "alice@example.com", "Alice"),
            new User(2L, "bob@example.com", "Bob")
        );
        when(userService.streamAll()).thenReturn(users);
        
        webTestClient.get()
            .uri("/api/users/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectBodyList(User.class)
            .hasSize(2);
    }
}
```

---

## Performance Testing

### JMH (Java Microbenchmark Harness)

```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.37</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-generator-annprocess</artifactId>
    <version>1.37</version>
    <scope>test</scope>
</dependency>
```

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class StringConcatenationBenchmark {
    
    private static final int ITERATIONS = 1000;
    
    @Benchmark
    public String testStringConcat() {
        String result = "";
        for (int i = 0; i < ITERATIONS; i++) {
            result += "test";
        }
        return result;
    }
    
    @Benchmark
    public String testStringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ITERATIONS; i++) {
            sb.append("test");
        }
        return sb.toString();
    }
    
    @Benchmark
    public String testStringBuffer() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ITERATIONS; i++) {
            sb.append("test");
        }
        return sb.toString();
    }
    
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
```

### Load Testing with Gatling

```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.10.3</version>
    <scope>test</scope>
</dependency>
```

```java
public class UserApiLoadTest extends Simulation {
    
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");
    
    ScenarioBuilder scn = scenario("User API Load Test")
        .exec(http("Get All Users")
            .get("/api/users")
            .check(status().is(200)))
        .pause(1)
        .exec(http("Get User by ID")
            .get("/api/users/1")
            .check(status().is(200))
            .check(jsonPath("$.email").exists()))
        .pause(1)
        .exec(http("Create User")
            .post("/api/users")
            .body(StringBody("""
                {
                    "email": "test@example.com",
                    "name": "Test User"
                }
                """))
            .check(status().is(201)));
    
    {
        setUp(
            scn.injectOpen(
                rampUsers(100).during(Duration.ofSeconds(30)),
                constantUsersPerSec(50).during(Duration.ofMinutes(2))
            )
        ).protocols(httpProtocol);
    }
}
```

### Concurrent Testing

```java
class ConcurrentTest {
    
    @Test
    void testConcurrentAccess() throws InterruptedException {
        Counter counter = new Counter();
        int threadCount = 100;
        int incrementsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(counter.getValue())
            .isEqualTo(threadCount * incrementsPerThread);
    }
    
    @Test
    void testVirtualThreads() throws InterruptedException {
        // Java 21 Virtual Threads
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 10_000;
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(taskCount);
            
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(100);
                        counter.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(30, TimeUnit.SECONDS);
        }
        
        assertThat(counter.get()).isEqualTo(taskCount);
    }
}
```

---

## Best Practices

### Test Naming Conventions

```java
class TestNamingExamples {
    
    // Pattern: methodName_stateUnderTest_expectedBehavior
    @Test
    void calculateTotal_withValidItems_returnsCorrectSum() {
        // Test implementation
    }
    
    // Pattern: given_when_then
    @Test
    void givenEmptyCart_whenAddingItem_thenCartContainsOneItem() {
        // Test implementation
    }
    
    // Pattern: should_expectedBehavior_when_stateUnderTest
    @Test
    void should_throwException_when_dividingByZero() {
        // Test implementation
    }
    
    // Using @DisplayName for readability
    @Test
    @DisplayName("Should calculate discount correctly for premium members")
    void testPremiumDiscount() {
        // Test implementation
    }
}
```

### AAA Pattern (Arrange-Act-Assert)

```java
class AAAPatternTest {
    
    @Test
    void testUserRegistration() {
        // Arrange
        UserService service = new UserService(userRepository, emailService);
        UserDTO dto = new UserDTO("john@example.com", "John Doe");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(new User(1L, dto.email(), dto.name()));
        
        // Act
        User result = service.register(dto);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(emailService).sendWelcomeEmail(dto.email());
    }
}
```

### Test Data Builders

```java
class UserTestDataBuilder {
    private Long id = 1L;
    private String email = "test@example.com";
    private String name = "Test User";
    private int age = 25;
    private boolean active = true;
    
    public UserTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }
    
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public UserTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public UserTestDataBuilder inactive() {
        this.active = false;
        return this;
    }
    
    public User build() {
        return new User(id, email, name, age, active);
    }
}

// Usage
@Test
void testWithBuilder() {
    User user = new UserTestDataBuilder()
        .withEmail("john@example.com")
        .withName("John")
        .inactive()
        .build();
    
    assertThat(user.isActive()).isFalse();
}
```

### Test Fixtures

```java
class TestFixtures {
    
    public static User createDefaultUser() {
        return new User(1L, "test@example.com", "Test User");
    }
    
    public static User createUser(String email, String name) {
        return new User(null, email, name);
    }
    
    public static List<User> createUserList(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new User(
                (long) i,
                "user" + i + "@example.com",
                "User " + i
            ))
            .toList();
    }
}

// Usage
@Test
void testWithFixtures() {
    User user = TestFixtures.createDefaultUser();
    List<User> users = TestFixtures.createUserList(10);
    
    assertThat(users).hasSize(10);
}
```

### Avoid Test Interdependence

```java
// BAD: Tests depend on execution order
class BadTest {
    private static User user;
    
    @Test
    void test1_createUser() {
        user = userService.create(new User("test@example.com", "Test"));
    }
    
    @Test
    void test2_updateUser() {
        user.setName("Updated"); // Depends on test1
        userService.update(user);
    }
}

// GOOD: Each test is independent
class GoodTest {
    
    @Test
    void testCreateUser() {
        User user = userService.create(new User("test@example.com", "Test"));
        assertThat(user.getId()).isNotNull();
    }
    
    @Test
    void testUpdateUser() {
        User user = userService.create(new User("test@example.com", "Test"));
        user.setName("Updated");
        User updated = userService.update(user);
        assertThat(updated.getName()).isEqualTo("Updated");
    }
}
```

### Test Coverage

```xml
<!-- JaCoCo for code coverage -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Flaky Test Prevention

```java
class FlakyTestPrevention {
    
    // BAD: Time-dependent test
    @Test
    void badTimeTest() {
        LocalDateTime now = LocalDateTime.now();
        // Test logic using current time - flaky!
    }
    
    // GOOD: Inject clock for deterministic time
    @Test
    void goodTimeTest() {
        Clock fixedClock = Clock.fixed(
            Instant.parse("2024-01-01T00:00:00Z"),
            ZoneId.of("UTC")
        );
        TimeService service = new TimeService(fixedClock);
        
        LocalDateTime result = service.getCurrentTime();
        assertThat(result).isEqualTo(
            LocalDateTime.of(2024, 1, 1, 0, 0)
        );
    }
    
    // BAD: Random-dependent test
    @Test
    void badRandomTest() {
        int random = new Random().nextInt(100);
        // Test logic using random - flaky!
    }
    
    // GOOD: Inject random with seed
    @Test
    void goodRandomTest() {
        Random random = new Random(12345L);
        RandomService service = new RandomService(random);
        
        int result = service.getRandomNumber();
        assertThat(result).isEqualTo(51); // Deterministic
    }
}
```

### Test Documentation

```java
/**
 * Tests for UserService registration functionality.
 * 
 * Covers:
 * - Successful user registration
 * - Duplicate email prevention
 * - Email validation
 * - Welcome email sending
 */
@DisplayName("User Registration Tests")
class UserRegistrationTest {
    
    /**
     * Given: Valid user data with unique email
     * When: User registers
     * Then: User is created and welcome email is sent
     */
    @Test
    @DisplayName("Should successfully register new user with valid data")
    void testSuccessfulRegistration() {
        // Test implementation
    }
    
    /**
     * Given: Email already exists in database
     * When: User tries to register with duplicate email
     * Then: DuplicateEmailException is thrown
     */
    @Test
    @DisplayName("Should throw exception when email already exists")
    void testDuplicateEmail() {
        // Test implementation
    }
}
```

---

## Summary

### Key Takeaways

1. **Test Pyramid**: 70% unit, 20% integration, 10% E2E
2. **JUnit 5**: Modern testing framework with rich features
3. **Mockito**: Essential for mocking dependencies
4. **Spring Boot Test**: Comprehensive testing support
5. **AssertJ**: Fluent assertions for readability
6. **Testcontainers**: Real infrastructure in tests
7. **Reactive Testing**: StepVerifier for reactive streams
8. **Performance**: JMH for microbenchmarks, Gatling for load testing

### Testing Checklist

- [ ] Unit tests for all business logic
- [ ] Integration tests for database operations
- [ ] API tests for all endpoints
- [ ] Validation tests for input constraints
- [ ] Error handling tests for edge cases
- [ ] Security tests for authentication/authorization
- [ ] Performance tests for critical paths
- [ ] Concurrent tests for thread-safe code
- [ ] Test coverage > 80%
- [ ] No flaky tests
- [ ] Fast test execution (<5 minutes)
- [ ] Clear test names and documentation

### Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Testcontainers](https://www.testcontainers.org/)
- [Project Reactor Testing](https://projectreactor.io/docs/core/release/reference/#testing)

