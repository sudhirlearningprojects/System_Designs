# Testing Deep Dive - Java 21 & Spring Boot

## Table of Contents
1. [Introduction](#introduction)
2. [Testing Pyramid](#testing-pyramid)
3. [JUnit 5 (Jupiter)](#junit-5-jupiter)
4. [Mockito](#mockito)
5. [Spring Boot Test](#spring-boot-test)
6. [AssertJ](#assertj)
7. [Testcontainers](#testcontainers)
8. [REST API Testing](#rest-api-testing)
9. [Reactive Testing](#reactive-testing)
10. [Performance Testing](#performance-testing)
11. [Best Practices](#best-practices)

---

## Introduction

Testing is critical for building reliable, maintainable software. This guide covers modern testing approaches using Java 21 features and Spring Boot 3.x.

### Why Testing Matters
- **Confidence**: Deploy with certainty
- **Documentation**: Tests describe behavior
- **Refactoring**: Change code safely
- **Quality**: Catch bugs early
- **Cost**: Fix issues before production

### Java 21 Testing Features
- **Virtual Threads**: Test concurrent code efficiently
- **Pattern Matching**: Cleaner test assertions
- **Record Patterns**: Simplified test data
- **Sequenced Collections**: Better test data setup

---

## Testing Pyramid

```
        /\
       /  \
      / UI \          10% - End-to-End Tests
     /______\
    /        \
   /Integration\      20% - Integration Tests
  /____________\
 /              \
/   Unit Tests   \    70% - Unit Tests
/__________________\
```

### Test Types

**Unit Tests** (70%)
- Test single class/method in isolation
- Fast execution (<1ms per test)
- Mock external dependencies
- High code coverage

**Integration Tests** (20%)
- Test multiple components together
- Database, messaging, external APIs
- Slower execution (100ms-1s per test)
- Verify component interactions

**End-to-End Tests** (10%)
- Test complete user workflows
- Full application stack
- Slowest execution (1s-10s per test)
- Verify business scenarios

---

## JUnit 5 (Jupiter)

### Maven Dependency

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
    <scope>test</scope>
</dependency>
```

### Basic Test Structure

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    
    private Calculator calculator;
    
    @BeforeAll
    static void setupAll() {
        // Runs once before all tests
        System.out.println("Starting Calculator tests");
    }
    
    @BeforeEach
    void setup() {
        // Runs before each test
        calculator = new Calculator();
    }
    
    @Test
    @DisplayName("Should add two positive numbers")
    void testAddition() {
        int result = calculator.add(2, 3);
        assertEquals(5, result);
    }
    
    @Test
    void testDivision() {
        assertThrows(ArithmeticException.class, () -> {
            calculator.divide(10, 0);
        });
    }
    
    @AfterEach
    void tearDown() {
        // Runs after each test
        calculator = null;
    }
    
    @AfterAll
    static void tearDownAll() {
        // Runs once after all tests
        System.out.println("Completed Calculator tests");
    }
}
```

### Parameterized Tests

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class StringUtilsTest {
    
    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t", "\n"})
    void testIsBlank(String input) {
        assertTrue(StringUtils.isBlank(input));
    }
    
    @ParameterizedTest
    @CsvSource({
        "apple, APPLE",
        "hello, HELLO",
        "test, TEST"
    })
    void testToUpperCase(String input, String expected) {
        assertEquals(expected, input.toUpperCase());
    }
    
    @ParameterizedTest
    @MethodSource("provideEmailTestCases")
    void testEmailValidation(String email, boolean expected) {
        assertEquals(expected, EmailValidator.isValid(email));
    }
    
    static Stream<Arguments> provideEmailTestCases() {
        return Stream.of(
            Arguments.of("test@example.com", true),
            Arguments.of("invalid-email", false),
            Arguments.of("user@domain", false)
        );
    }
}
```

### Nested Tests

```java
@DisplayName("User Service Tests")
class UserServiceTest {
    
    @Nested
    @DisplayName("When user is new")
    class NewUserTests {
        
        @Test
        void shouldCreateUser() {
            // Test user creation
        }
        
        @Test
        void shouldSendWelcomeEmail() {
            // Test welcome email
        }
    }
    
    @Nested
    @DisplayName("When user exists")
    class ExistingUserTests {
        
        @Test
        void shouldUpdateUser() {
            // Test user update
        }
        
        @Test
        void shouldNotDuplicateEmail() {
            // Test duplicate prevention
        }
    }
}
```

### Conditional Tests

```java
class ConditionalTest {
    
    @Test
    @EnabledOnOs(OS.LINUX)
    void testOnLinux() {
        // Only runs on Linux
    }
    
    @Test
    @EnabledOnJre(JRE.JAVA_21)
    void testOnJava21() {
        // Only runs on Java 21
    }
    
    @Test
    @EnabledIf("customCondition")
    void testWithCustomCondition() {
        // Runs if condition is true
    }
    
    boolean customCondition() {
        return System.getenv("ENV").equals("test");
    }
}
```

### Timeout Tests

```java
class TimeoutTest {
    
    @Test
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testWithTimeout() {
        // Must complete within 100ms
        performFastOperation();
    }
    
    @Test
    void testWithAssertTimeout() {
        assertTimeout(Duration.ofSeconds(1), () -> {
            // Code must complete within 1 second
            performOperation();
        });
    }
}
```

---

## Mockito

### Maven Dependency

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.8.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.8.0</version>
    <scope>test</scope>
</dependency>
```

### Basic Mocking

```java
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testCreateUser() {
        // Arrange
        User user = new User("john@example.com", "John Doe");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Act
        User created = userService.createUser(user);
        
        // Assert
        assertNotNull(created);
        assertEquals("john@example.com", created.getEmail());
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail(user.getEmail());
    }
}
```

### Argument Matchers

```java
class ArgumentMatcherTest {
    
    @Test
    void testWithMatchers() {
        UserRepository repo = mock(UserRepository.class);
        
        // Any matcher
        when(repo.findById(anyLong())).thenReturn(Optional.of(new User()));
        
        // Specific value
        when(repo.findById(1L)).thenReturn(Optional.of(new User("user1")));
        
        // Custom matcher
        when(repo.findByEmail(argThat(email -> email.endsWith("@example.com"))))
            .thenReturn(Optional.of(new User()));
        
        // Null matcher
        when(repo.findByEmail(isNull())).thenThrow(IllegalArgumentException.class);
    }
}
```

### Stubbing Methods

```java
class StubbingTest {
    
    @Test
    void testStubbing() {
        PaymentService service = mock(PaymentService.class);
        
        // Return value
        when(service.processPayment(100.0)).thenReturn(true);
        
        // Throw exception
        when(service.processPayment(-1.0))
            .thenThrow(new IllegalArgumentException("Invalid amount"));
        
        // Multiple calls
        when(service.getBalance())
            .thenReturn(100.0)
            .thenReturn(90.0)
            .thenReturn(80.0);
        
        // Callback
        when(service.processPayment(anyDouble())).thenAnswer(invocation -> {
            Double amount = invocation.getArgument(0);
            return amount > 0;
        });
    }
}
```

### Verification

```java
class VerificationTest {
    
    @Test
    void testVerification() {
        EmailService service = mock(EmailService.class);
        
        service.sendEmail("test@example.com", "Hello");
        service.sendEmail("test@example.com", "World");
        
        // Verify called once
        verify(service).sendEmail("test@example.com", "Hello");
        
        // Verify called twice
        verify(service, times(2)).sendEmail(eq("test@example.com"), anyString());
        
        // Verify never called
        verify(service, never()).sendEmail("other@example.com", anyString());
        
        // Verify at least once
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString());
        
        // Verify order
        InOrder inOrder = inOrder(service);
        inOrder.verify(service).sendEmail("test@example.com", "Hello");
        inOrder.verify(service).sendEmail("test@example.com", "World");
    }
}
```

### Spying

```java
class SpyTest {
    
    @Test
    void testSpy() {
        List<String> list = new ArrayList<>();
        List<String> spyList = spy(list);
        
        // Real method called
        spyList.add("one");
        spyList.add("two");
        
        // Stub specific method
        when(spyList.size()).thenReturn(100);
        
        assertEquals(100, spyList.size());
        assertEquals("one", spyList.get(0)); // Real method
        
        verify(spyList).add("one");
    }
}
```

### Argument Captors

```java
class ArgumentCaptorTest {
    
    @Test
    void testArgumentCaptor() {
        EmailService service = mock(EmailService.class);
        UserService userService = new UserService(service);
        
        userService.registerUser("john@example.com", "John");
        
        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(service).send(captor.capture());
        
        Email captured = captor.getValue();
        assertEquals("john@example.com", captured.getTo());
        assertTrue(captured.getSubject().contains("Welcome"));
    }
}
```

---

## Spring Boot Test

### Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### @SpringBootTest

```java
@SpringBootTest
class ApplicationIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testFullApplicationContext() {
        User user = new User("test@example.com", "Test User");
        User saved = userService.createUser(user);
        
        assertNotNull(saved.getId());
        assertTrue(userRepository.existsById(saved.getId()));
    }
}
```

### @WebMvcTest

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetUser() throws Exception {
        User user = new User(1L, "john@example.com", "John");
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.name").value("John"));
    }
    
    @Test
    void testCreateUser() throws Exception {
        UserDTO dto = new UserDTO("jane@example.com", "Jane");
        User user = new User(2L, "jane@example.com", "Jane");
        
        when(userService.createUser(any())).thenReturn(user);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "jane@example.com",
                        "name": "Jane"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(2));
    }
}
```

### @DataJpaTest

```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testFindByEmail() {
        User user = new User("test@example.com", "Test");
        entityManager.persist(user);
        entityManager.flush();
        
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getName());
    }
    
    @Test
    void testCustomQuery() {
        entityManager.persist(new User("user1@example.com", "User1"));
        entityManager.persist(new User("user2@example.com", "User2"));
        entityManager.flush();
        
        List<User> users = userRepository.findByEmailDomain("example.com");
        
        assertEquals(2, users.size());
    }
}
```

### @MockBean vs @Mock

```java
// @Mock - Pure Mockito, no Spring context
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
}

// @MockBean - Spring context, replaces bean
@SpringBootTest
class IntegrationTest {
    @MockBean
    private ExternalService externalService;
    
    @Autowired
    private MyService myService; // Uses mocked ExternalService
}
```

### Test Configuration

```java
@TestConfiguration
class TestConfig {
    
    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
    
    @Bean
    public Clock fixedClock() {
        return Clock.fixed(
            Instant.parse("2024-01-01T00:00:00Z"),
            ZoneId.of("UTC")
        );
    }
}

@SpringBootTest
@Import(TestConfig.class)
class ServiceWithTestConfigTest {
    // Uses test configuration
}
```

### Test Properties

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "app.feature.enabled=true"
})
class PropertyTest {
    
    @Value("${app.feature.enabled}")
    private boolean featureEnabled;
    
    @Test
    void testProperty() {
        assertTrue(featureEnabled);
    }
}
```

---

## AssertJ

### Maven Dependency

```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.25.1</version>
    <scope>test</scope>
</dependency>
```

### Basic Assertions

```java
import static org.assertj.core.api.Assertions.*;

class AssertJTest {
    
    @Test
    void testBasicAssertions() {
        String name = "John Doe";
        
        assertThat(name)
            .isNotNull()
            .isNotEmpty()
            .startsWith("John")
            .endsWith("Doe")
            .contains("oh")
            .hasSize(8);
    }
    
    @Test
    void testNumberAssertions() {
        int age = 25;
        
        assertThat(age)
            .isPositive()
            .isGreaterThan(18)
            .isLessThan(100)
            .isBetween(20, 30);
    }
    
    @Test
    void testCollectionAssertions() {
        List<String> names = List.of("Alice", "Bob", "Charlie");
        
        assertThat(names)
            .hasSize(3)
            .contains("Alice", "Bob")
            .doesNotContain("David")
            .containsExactly("Alice", "Bob", "Charlie")
            .containsExactlyInAnyOrder("Bob", "Alice", "Charlie");
    }
}
```

### Object Assertions

```java
class ObjectAssertionTest {
    
    @Test
    void testObjectAssertions() {
        User user = new User(1L, "john@example.com", "John", 25);
        
        assertThat(user)
            .isNotNull()
            .extracting("email", "name", "age")
            .containsExactly("john@example.com", "John", 25);
        
        assertThat(user)
            .hasFieldOrPropertyWithValue("email", "john@example.com")
            .hasNoNullFieldsOrProperties();
    }
    
    @Test
    void testListOfObjects() {
        List<User> users = List.of(
            new User(1L, "alice@example.com", "Alice", 25),
            new User(2L, "bob@example.com", "Bob", 30)
        );
        
        assertThat(users)
            .extracting(User::getName)
            .containsExactly("Alice", "Bob");
        
        assertThat(users)
            .filteredOn(user -> user.getAge() > 25)
            .hasSize(1)
            .extracting(User::getName)
            .containsExactly("Bob");
    }
}
```

### Exception Assertions

```java
class ExceptionAssertionTest {
    
    @Test
    void testExceptionAssertions() {
        assertThatThrownBy(() -> {
            throw new IllegalArgumentException("Invalid input");
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid input")
            .hasMessageContaining("Invalid");
        
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> {
                validateInput(null);
            })
            .withMessage("Input cannot be null");
    }
}
```

### Soft Assertions

```java
class SoftAssertionTest {
    
    @Test
    void testSoftAssertions() {
        User user = new User(1L, "john@example.com", "John", 25);
        
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(user.getEmail()).isEqualTo("john@example.com");
        softly.assertThat(user.getName()).isEqualTo("John");
        softly.assertThat(user.getAge()).isEqualTo(25);
        softly.assertAll(); // All assertions checked, reports all failures
    }
    
    @Test
    void testAutoSoftAssertions() {
        User user = new User(1L, "john@example.com", "John", 25);
        
        assertSoftly(softly -> {
            softly.assertThat(user.getEmail()).isEqualTo("john@example.com");
            softly.assertThat(user.getName()).isEqualTo("John");
            softly.assertThat(user.getAge()).isEqualTo(25);
        });
    }
}
```

---

## Testcontainers

### Maven Dependency

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### PostgreSQL Container

```java
@Testcontainers
@SpringBootTest
class PostgresIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testWithRealDatabase() {
        User user = new User("test@example.com", "Test");
        User saved = userRepository.save(user);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }
}
```

### Redis Container

```java
@Testcontainers
@SpringBootTest
class RedisIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Test
    void testRedisOperations() {
        redisTemplate.opsForValue().set("key", "value");
        String value = redisTemplate.opsForValue().get("key");
        
        assertThat(value).isEqualTo("value");
    }
}
```

### Kafka Container

```java
@Testcontainers
@SpringBootTest
class KafkaIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void testKafkaProducer() throws Exception {
        String topic = "test-topic";
        String message = "Hello Kafka";
        
        kafkaTemplate.send(topic, message).get();
        
        // Verify message sent
        assertThat(true).isTrue();
    }
}
```

### Multiple Containers

```java
@Testcontainers
@SpringBootTest
class MultiContainerTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Test
    void testFullStack() {
        // Test with all infrastructure
    }
}
```

### Docker Compose

```java
@Testcontainers
@SpringBootTest
class DockerComposeTest {
    
    @Container
    static ComposeContainer compose = new ComposeContainer(
        new File("src/test/resources/docker-compose-test.yml")
    )
        .withExposedService("postgres", 5432)
        .withExposedService("redis", 6379);
    
    @Test
    void testWithDockerCompose() {
        // Test with docker-compose services
    }
}
```

