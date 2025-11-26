# Spring Boot Testing Annotations - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Core Testing Annotations](#core-testing-annotations)
3. [Spring Boot Test Annotations](#spring-boot-test-annotations)
4. [Slice Test Annotations](#slice-test-annotations)
5. [Mocking Annotations](#mocking-annotations)
6. [JUnit 5 Annotations](#junit-5-annotations)
7. [Configuration Annotations](#configuration-annotations)
8. [Real-World Examples](#real-world-examples)

---

## Introduction

### Testing Layers in Spring Boot

```
┌─────────────────────────────────────┐
│     Controller Layer Tests          │  @WebMvcTest
├─────────────────────────────────────┤
│     Service Layer Tests              │  @ExtendWith(MockitoExtension.class)
├─────────────────────────────────────┤
│     Repository Layer Tests           │  @DataJpaTest
├─────────────────────────────────────┤
│     Integration Tests                │  @SpringBootTest
└─────────────────────────────────────┘
```

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Core Testing Annotations

### @SpringBootTest

**Purpose:** Loads complete application context for integration tests.

**Usage:**
```java
@SpringBootTest
class ApplicationIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testFullApplicationContext() {
        assertNotNull(userService);
    }
}
```

**With Web Environment:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testRestEndpoint() {
        String url = "http://localhost:" + port + "/api/users";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

**Options:**
- `MOCK` - Mock web environment (default)
- `RANDOM_PORT` - Starts server on random port
- `DEFINED_PORT` - Uses application.properties port
- `NONE` - No web environment

---

### @Test

**Purpose:** Marks method as test case.

```java
@Test
void shouldReturnUser() {
    User user = userService.findById(1L);
    assertNotNull(user);
}

@Test
void shouldThrowException() {
    assertThrows(UserNotFoundException.class, () -> {
        userService.findById(999L);
    });
}
```

---

### @BeforeEach / @AfterEach

**Purpose:** Setup and teardown before/after each test.

```java
class UserServiceTest {
    
    private UserService userService;
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup resources
    }
    
    @Test
    void testUser() {
        // Test logic
    }
}
```

---

### @BeforeAll / @AfterAll

**Purpose:** Setup and teardown once before/after all tests.

```java
class DatabaseTest {
    
    private static Database database;
    
    @BeforeAll
    static void initDatabase() {
        database = new Database();
        database.connect();
    }
    
    @AfterAll
    static void closeDatabase() {
        database.disconnect();
    }
    
    @Test
    void testQuery() {
        // Use database
    }
}
```

---

## Spring Boot Test Annotations

### @WebMvcTest

**Purpose:** Tests only web layer (controllers) without full context.

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUser() throws Exception {
        User user = new User(1L, "John");
        when(userService.findById(1L)).thenReturn(user);
        
        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("John"));
    }
}
```

**What it loads:**
- Controllers
- @ControllerAdvice
- @JsonComponent
- Filters
- WebMvcConfigurer

**What it doesn't load:**
- @Service
- @Repository
- @Component

---

### @DataJpaTest

**Purpose:** Tests JPA repositories with in-memory database.

```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void shouldFindUserByEmail() {
        // Given
        User user = new User("john@example.com", "John");
        entityManager.persist(user);
        entityManager.flush();
        
        // When
        User found = userRepository.findByEmail("john@example.com");
        
        // Then
        assertNotNull(found);
        assertEquals("John", found.getName());
    }
}
```

**Features:**
- Configures H2 in-memory database
- Scans @Entity classes
- Configures Spring Data JPA repositories
- Configures TestEntityManager
- Transactional (rolls back after each test)

---

### @WebFluxTest

**Purpose:** Tests reactive web layer.

```java
@WebFluxTest(UserController.class)
class UserControllerWebFluxTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUser() {
        User user = new User(1L, "John");
        when(userService.findById(1L)).thenReturn(Mono.just(user));
        
        webTestClient.get()
                     .uri("/api/users/1")
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(User.class)
                     .value(u -> assertEquals("John", u.getName()));
    }
}
```

---

### @RestClientTest

**Purpose:** Tests REST clients.

```java
@RestClientTest(UserClient.class)
class UserClientTest {
    
    @Autowired
    private UserClient userClient;
    
    @Autowired
    private MockRestServiceServer server;
    
    @Test
    void shouldFetchUser() {
        server.expect(requestTo("/api/users/1"))
              .andRespond(withSuccess("{\"id\":1,\"name\":\"John\"}", 
                                     MediaType.APPLICATION_JSON));
        
        User user = userClient.getUser(1L);
        
        assertEquals("John", user.getName());
    }
}
```

---

### @JsonTest

**Purpose:** Tests JSON serialization/deserialization.

```java
@JsonTest
class UserJsonTest {
    
    @Autowired
    private JacksonTester<User> json;
    
    @Test
    void shouldSerialize() throws Exception {
        User user = new User(1L, "John");
        
        assertThat(json.write(user))
            .hasJsonPathStringValue("$.name")
            .extractingJsonPathStringValue("$.name")
            .isEqualTo("John");
    }
    
    @Test
    void shouldDeserialize() throws Exception {
        String content = "{\"id\":1,\"name\":\"John\"}";
        
        assertThat(json.parse(content))
            .isEqualTo(new User(1L, "John"));
    }
}
```

---

## Slice Test Annotations

### @DataMongoTest

**Purpose:** Tests MongoDB repositories.

```java
@DataMongoTest
class UserMongoRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldSaveUser() {
        User user = new User("john@example.com", "John");
        User saved = userRepository.save(user);
        
        assertNotNull(saved.getId());
    }
}
```

---

### @DataRedisTest

**Purpose:** Tests Redis operations.

```java
@DataRedisTest
class CacheTest {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Test
    void shouldCacheValue() {
        redisTemplate.opsForValue().set("key", "value");
        String value = redisTemplate.opsForValue().get("key");
        
        assertEquals("value", value);
    }
}
```

---

### @DataJdbcTest

**Purpose:** Tests Spring Data JDBC.

```java
@DataJdbcTest
class UserJdbcRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindAll() {
        List<User> users = userRepository.findAll();
        assertNotNull(users);
    }
}
```

---

## Mocking Annotations

### @MockBean

**Purpose:** Adds mock to Spring context, replaces existing bean.

```java
@SpringBootTest
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private UserRepository userRepository;
    
    @Test
    void shouldReturnUser() {
        User user = new User(1L, "John");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        User found = userService.findById(1L);
        
        assertEquals("John", found.getName());
    }
}
```

---

### @SpyBean

**Purpose:** Wraps existing bean with spy, allows partial mocking.

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @SpyBean
    private EmailService emailService;
    
    @Test
    void shouldSendEmail() {
        userService.registerUser(new User("john@example.com", "John"));
        
        verify(emailService).sendWelcomeEmail("john@example.com");
    }
}
```

---

### @Mock (Mockito)

**Purpose:** Creates mock object (not added to Spring context).

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void shouldReturnUser() {
        User user = new User(1L, "John");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        User found = userService.findById(1L);
        
        assertEquals("John", found.getName());
    }
}
```

---

### @InjectMocks

**Purpose:** Creates instance and injects mocks into it.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private PaymentService paymentService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void shouldCreateOrder() {
        Order order = new Order();
        when(orderRepository.save(any())).thenReturn(order);
        
        Order created = orderService.createOrder(order);
        
        verify(paymentService).processPayment(order);
    }
}
```

---

## JUnit 5 Annotations

### @DisplayName

**Purpose:** Custom test name for better readability.

```java
@DisplayName("User Service Tests")
class UserServiceTest {
    
    @Test
    @DisplayName("Should return user when valid ID is provided")
    void shouldReturnUser() {
        // Test logic
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowException() {
        // Test logic
    }
}
```

---

### @Disabled

**Purpose:** Disable test temporarily.

```java
@Test
@Disabled("Temporarily disabled due to bug #123")
void testFeature() {
    // Test logic
}
```

---

### @ParameterizedTest

**Purpose:** Run test with multiple parameters.

```java
@ParameterizedTest
@ValueSource(strings = {"john@example.com", "jane@example.com"})
void shouldValidateEmail(String email) {
    assertTrue(emailValidator.isValid(email));
}

@ParameterizedTest
@CsvSource({
    "1, John",
    "2, Jane",
    "3, Bob"
})
void shouldReturnUser(Long id, String name) {
    User user = userService.findById(id);
    assertEquals(name, user.getName());
}
```

---

### @RepeatedTest

**Purpose:** Repeat test multiple times.

```java
@RepeatedTest(5)
void shouldGenerateUniqueId() {
    String id = idGenerator.generate();
    assertNotNull(id);
}
```

---

### @Timeout

**Purpose:** Fail test if exceeds time limit.

```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void shouldCompleteQuickly() {
    // Must complete within 5 seconds
    userService.processUsers();
}
```

---

### @Tag

**Purpose:** Categorize tests for selective execution.

```java
@Test
@Tag("integration")
void integrationTest() {
    // Integration test
}

@Test
@Tag("unit")
void unitTest() {
    // Unit test
}
```

Run specific tags:
```bash
mvn test -Dgroups="integration"
```

---

## Configuration Annotations

### @TestConfiguration

**Purpose:** Additional configuration for tests.

```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    public UserService userService() {
        return new UserService();
    }
}

@SpringBootTest
@Import(TestConfig.class)
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testService() {
        assertNotNull(userService);
    }
}
```

---

### @TestPropertySource

**Purpose:** Override properties for tests.

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "app.feature.enabled=true"
})
class ApplicationTest {
    
    @Value("${app.feature.enabled}")
    private boolean featureEnabled;
    
    @Test
    void testProperty() {
        assertTrue(featureEnabled);
    }
}
```

---

### @ActiveProfiles

**Purpose:** Activate specific profiles for tests.

```java
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testWithTestProfile() {
        // Uses application-test.properties
    }
}
```

---

### @DirtiesContext

**Purpose:** Marks context as dirty, forces reload.

```java
@SpringBootTest
class CacheTest {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    @DirtiesContext
    void shouldClearCache() {
        cacheManager.getCache("users").clear();
        // Context will be reloaded for next test
    }
}
```

---

### @Sql

**Purpose:** Execute SQL scripts before tests.

```java
@SpringBootTest
@Sql("/test-data.sql")
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindUsers() {
        List<User> users = userRepository.findAll();
        assertEquals(3, users.size());
    }
}

@Test
@Sql(scripts = "/insert-users.sql", 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", 
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void testWithSetupAndCleanup() {
    // Test logic
}
```

---

### @AutoConfigureMockMvc

**Purpose:** Auto-configure MockMvc.

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturnUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
               .andExpect(status().isOk());
    }
}
```

---

### @AutoConfigureTestDatabase

**Purpose:** Configure test database.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    // Uses real database instead of H2
}
```

---

## Real-World Examples

### Example 1: Controller Test

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @DisplayName("GET /api/users/{id} - Success")
    void shouldReturnUser() throws Exception {
        // Given
        User user = new User(1L, "John", "john@example.com");
        when(userService.findById(1L)).thenReturn(user);
        
        // When & Then
        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.name").value("John"))
               .andExpect(jsonPath("$.email").value("john@example.com"));
        
        verify(userService).findById(1L);
    }
    
    @Test
    @DisplayName("GET /api/users/{id} - Not Found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Given
        when(userService.findById(999L))
            .thenThrow(new UserNotFoundException("User not found"));
        
        // When & Then
        mockMvc.perform(get("/api/users/999"))
               .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("POST /api/users - Create User")
    void shouldCreateUser() throws Exception {
        // Given
        User user = new User(null, "John", "john@example.com");
        User saved = new User(1L, "John", "john@example.com");
        when(userService.save(any(User.class))).thenReturn(saved);
        
        // When & Then
        mockMvc.perform(post("/api/users")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("{\"name\":\"John\",\"email\":\"john@example.com\"}"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(1));
    }
}
```

---

### Example 2: Service Test

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // Given
        User user = new User(1L, "John", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // When
        User found = userService.findById(1L);
        
        // Then
        assertNotNull(found);
        assertEquals("John", found.getName());
        verify(userRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            userService.findById(999L);
        });
    }
    
    @Test
    @DisplayName("Should register user and send welcome email")
    void shouldRegisterUser() {
        // Given
        User user = new User(null, "John", "john@example.com");
        User saved = new User(1L, "John", "john@example.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        
        // When
        User registered = userService.registerUser(user);
        
        // Then
        assertNotNull(registered.getId());
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail("john@example.com");
    }
}
```

---

### Example 3: Repository Test

```java
@DataJpaTest
@DisplayName("User Repository Tests")
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        User user = new User("john@example.com", "John");
        entityManager.persist(user);
        entityManager.flush();
        
        // When
        User found = userRepository.findByEmail("john@example.com");
        
        // Then
        assertNotNull(found);
        assertEquals("John", found.getName());
    }
    
    @Test
    @DisplayName("Should return null when email not found")
    void shouldReturnNullWhenEmailNotFound() {
        // When
        User found = userRepository.findByEmail("notfound@example.com");
        
        // Then
        assertNull(found);
    }
    
    @Test
    @DisplayName("Should find users by name containing")
    void shouldFindUsersByNameContaining() {
        // Given
        entityManager.persist(new User("john@example.com", "John Doe"));
        entityManager.persist(new User("jane@example.com", "Jane Doe"));
        entityManager.persist(new User("bob@example.com", "Bob Smith"));
        entityManager.flush();
        
        // When
        List<User> users = userRepository.findByNameContaining("Doe");
        
        // Then
        assertEquals(2, users.size());
    }
}
```

---

### Example 4: Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("User API Integration Tests")
class UserIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should create and retrieve user")
    void shouldCreateAndRetrieveUser() {
        // Create user
        User user = new User(null, "John", "john@example.com");
        ResponseEntity<User> createResponse = restTemplate.postForEntity(
            "/api/users", user, User.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody().getId());
        
        // Retrieve user
        Long userId = createResponse.getBody().getId();
        ResponseEntity<User> getResponse = restTemplate.getForEntity(
            "/api/users/" + userId, User.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("John", getResponse.getBody().getName());
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/users/999", String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

---

## Summary

### Quick Reference

| Annotation | Purpose | Scope |
|------------|---------|-------|
| @SpringBootTest | Full application context | Integration |
| @WebMvcTest | Controller layer only | Unit |
| @DataJpaTest | JPA repositories | Unit |
| @MockBean | Mock Spring bean | Any |
| @Mock | Mock object (Mockito) | Unit |
| @InjectMocks | Inject mocks | Unit |
| @Test | Test method | Any |
| @BeforeEach | Setup before each test | Any |
| @DisplayName | Custom test name | Any |
| @ParameterizedTest | Multiple parameters | Any |

### Best Practices

1. **Use slice tests** (@WebMvcTest, @DataJpaTest) for faster unit tests
2. **Use @SpringBootTest** only for integration tests
3. **Mock external dependencies** with @MockBean
4. **Use @DisplayName** for readable test names
5. **Clean up** with @BeforeEach/@AfterEach
6. **Use @ActiveProfiles** for test-specific configuration
7. **Parameterize** repetitive tests with @ParameterizedTest

---

**Last Updated**: 2024  
**Spring Boot Version**: 3.x
