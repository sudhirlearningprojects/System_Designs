# JUnit Testing in Spring Boot - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Setup and Dependencies](#setup-and-dependencies)
3. [Testing Annotations](#testing-annotations)
4. [Unit Testing](#unit-testing)
5. [Integration Testing](#integration-testing)
6. [Repository Testing](#repository-testing)
7. [Service Layer Testing](#service-layer-testing)
8. [Controller Testing](#controller-testing)
9. [MockMvc Testing](#mockmvc-testing)
10. [Mocking with Mockito](#mocking-with-mockito)
11. [Test Configuration](#test-configuration)
12. [Best Practices](#best-practices)

---

## Introduction

### What is JUnit?
JUnit is a popular testing framework for Java applications. JUnit 5 (Jupiter) is the latest version with modern features and better integration with Spring Boot.

### Testing Pyramid
```
        /\
       /  \      E2E Tests (Few)
      /____\
     /      \    Integration Tests (Some)
    /________\
   /          \  Unit Tests (Many)
  /____________\
```

### Types of Tests in Spring Boot
- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test multiple components together
- **End-to-End Tests**: Test complete application flow

---

## Setup and Dependencies

### Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starter Test (includes JUnit 5, Mockito, AssertJ, etc.) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 (Jupiter) - Already included in spring-boot-starter-test -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito - Already included in spring-boot-starter-test -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ - Already included in spring-boot-starter-test -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- H2 Database for testing -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- TestContainers (for integration testing with real databases) -->
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
</dependencies>
```

### Gradle Dependencies (build.gradle)

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'com.h2database:h2'
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
    testImplementation 'org.testcontainers:postgresql:1.19.3'
}

test {
    useJUnitPlatform()
}
```

---

## Testing Annotations

### Core JUnit 5 Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Test` | Marks a method as a test method | `@Test void testMethod()` |
| `@BeforeEach` | Runs before each test method | Setup test data |
| `@AfterEach` | Runs after each test method | Cleanup resources |
| `@BeforeAll` | Runs once before all tests (static) | Initialize expensive resources |
| `@AfterAll` | Runs once after all tests (static) | Cleanup expensive resources |
| `@Disabled` | Disables a test | Skip temporarily |
| `@DisplayName` | Custom test name | Better readability |
| `@Tag` | Tag tests for filtering | `@Tag("integration")` |
| `@Nested` | Nested test classes | Group related tests |
| `@ParameterizedTest` | Run test with multiple parameters | Data-driven tests |
| `@RepeatedTest` | Repeat test N times | Flaky test detection |
| `@Timeout` | Fail if test exceeds time | Performance tests |

### Spring Boot Testing Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@SpringBootTest` | Load full application context | Integration tests |
| `@WebMvcTest` | Test MVC controllers only | Controller tests |
| `@DataJpaTest` | Test JPA repositories only | Repository tests |
| `@RestClientTest` | Test REST clients | External API tests |
| `@JsonTest` | Test JSON serialization | DTO tests |
| `@MockBean` | Add mock to Spring context | Mock dependencies |
| `@SpyBean` | Add spy to Spring context | Partial mocking |
| `@TestConfiguration` | Additional test configuration | Custom test beans |
| `@AutoConfigureMockMvc` | Auto-configure MockMvc | Controller tests |
| `@Sql` | Execute SQL scripts before tests | Database setup |

---

## Unit Testing

### Basic Unit Test Example

```java
package org.sudhir512kj.payment.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    
    private Calculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new Calculator();
        System.out.println("Setting up test");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("Cleaning up test");
    }
    
    @Test
    @DisplayName("Should add two numbers correctly")
    void testAdd() {
        // Arrange
        int a = 5;
        int b = 3;
        
        // Act
        int result = calculator.add(a, b);
        
        // Assert
        assertEquals(8, result);
    }
    
    @Test
    @DisplayName("Should subtract two numbers correctly")
    void testSubtract() {
        int result = calculator.subtract(10, 4);
        assertEquals(6, result);
    }
    
    @Test
    @DisplayName("Should throw exception when dividing by zero")
    void testDivideByZero() {
        assertThrows(ArithmeticException.class, () -> {
            calculator.divide(10, 0);
        });
    }
    
    @Test
    @Disabled("Not implemented yet")
    void testMultiply() {
        // TODO: Implement this test
    }
}
```

---

### Assertions

#### JUnit 5 Assertions

```java
import static org.junit.jupiter.api.Assertions.*;

class AssertionsExampleTest {
    
    @Test
    void testAssertions() {
        // Basic assertions
        assertEquals(4, 2 + 2);
        assertNotEquals(5, 2 + 2);
        assertTrue(5 > 3);
        assertFalse(5 < 3);
        assertNull(null);
        assertNotNull("value");
        
        // Array assertions
        int[] expected = {1, 2, 3};
        int[] actual = {1, 2, 3};
        assertArrayEquals(expected, actual);
        
        // Exception assertions
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("Invalid argument");
        });
        assertEquals("Invalid argument", exception.getMessage());
        
        // Timeout assertions
        assertTimeout(Duration.ofSeconds(1), () -> {
            // Code that should complete within 1 second
            Thread.sleep(500);
        });
        
        // Grouped assertions
        assertAll("person",
            () -> assertEquals("John", person.getFirstName()),
            () -> assertEquals("Doe", person.getLastName()),
            () -> assertEquals(30, person.getAge())
        );
    }
}
```

#### AssertJ Assertions (More Readable)

```java
import static org.assertj.core.api.Assertions.*;

class AssertJExampleTest {
    
    @Test
    void testAssertJ() {
        // Basic assertions
        assertThat(2 + 2).isEqualTo(4);
        assertThat(5).isGreaterThan(3);
        assertThat("Hello").startsWith("He").endsWith("lo");
        
        // Collection assertions
        List<String> names = Arrays.asList("John", "Jane", "Bob");
        assertThat(names)
            .hasSize(3)
            .contains("John", "Jane")
            .doesNotContain("Alice");
        
        // Object assertions
        Person person = new Person("John", "Doe", 30);
        assertThat(person)
            .isNotNull()
            .extracting(Person::getFirstName, Person::getAge)
            .containsExactly("John", 30);
        
        // Exception assertions
        assertThatThrownBy(() -> {
            throw new IllegalArgumentException("Invalid");
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid");
    }
}
```

---

## Service Layer Testing

### Simple Service Test (No Dependencies)

```java
package org.sudhir512kj.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class PriceCalculatorServiceTest {
    
    private PriceCalculatorService priceCalculator;
    
    @BeforeEach
    void setUp() {
        priceCalculator = new PriceCalculatorService();
    }
    
    @Test
    @DisplayName("Should calculate price with 10% discount")
    void testCalculateDiscountedPrice() {
        // Arrange
        double originalPrice = 100.0;
        double discountPercent = 10.0;
        
        // Act
        double result = priceCalculator.calculateDiscountedPrice(originalPrice, discountPercent);
        
        // Assert
        assertThat(result).isEqualTo(90.0);
    }
    
    @Test
    @DisplayName("Should throw exception for negative price")
    void testNegativePrice() {
        assertThatThrownBy(() -> 
            priceCalculator.calculateDiscountedPrice(-100.0, 10.0)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Price cannot be negative");
    }
    
    @Test
    @DisplayName("Should throw exception for discount > 100%")
    void testInvalidDiscount() {
        assertThatThrownBy(() -> 
            priceCalculator.calculateDiscountedPrice(100.0, 150.0)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Discount cannot exceed 100%");
    }
}
```

---

### Service Test with Mockito (With Dependencies)

```java
package org.sudhir512kj.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private PaymentGateway paymentGateway;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private Payment payment;
    
    @BeforeEach
    void setUp() {
        payment = Payment.builder()
            .id("PAY-123")
            .amount(100.0)
            .currency("USD")
            .status(PaymentStatus.PENDING)
            .build();
    }
    
    @Test
    @DisplayName("Should process payment successfully")
    void testProcessPaymentSuccess() {
        // Arrange
        when(paymentGateway.charge(any(Payment.class)))
            .thenReturn(PaymentResponse.success("TXN-456"));
        when(paymentRepository.save(any(Payment.class)))
            .thenReturn(payment);
        
        // Act
        Payment result = paymentService.processPayment(payment);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        
        // Verify interactions
        verify(paymentGateway, times(1)).charge(payment);
        verify(paymentRepository, times(1)).save(payment);
        verify(notificationService, times(1)).sendPaymentConfirmation(payment);
    }
    
    @Test
    @DisplayName("Should handle payment failure")
    void testProcessPaymentFailure() {
        // Arrange
        when(paymentGateway.charge(any(Payment.class)))
            .thenThrow(new PaymentGatewayException("Insufficient funds"));
        
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processPayment(payment))
            .isInstanceOf(PaymentProcessingException.class)
            .hasMessageContaining("Insufficient funds");
        
        // Verify
        verify(paymentGateway, times(1)).charge(payment);
        verify(paymentRepository, never()).save(any());
        verify(notificationService, never()).sendPaymentConfirmation(any());
    }
    
    @Test
    @DisplayName("Should retry payment on temporary failure")
    void testPaymentRetry() {
        // Arrange
        when(paymentGateway.charge(any(Payment.class)))
            .thenThrow(new TemporaryException("Network timeout"))
            .thenThrow(new TemporaryException("Network timeout"))
            .thenReturn(PaymentResponse.success("TXN-789"));
        
        // Act
        Payment result = paymentService.processPaymentWithRetry(payment);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentGateway, times(3)).charge(payment);
    }
    
    @Test
    @DisplayName("Should find payment by id")
    void testFindPaymentById() {
        // Arrange
        when(paymentRepository.findById("PAY-123"))
            .thenReturn(Optional.of(payment));
        
        // Act
        Optional<Payment> result = paymentService.findById("PAY-123");
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("PAY-123");
        verify(paymentRepository, times(1)).findById("PAY-123");
    }
    
    @Test
    @DisplayName("Should return empty when payment not found")
    void testFindPaymentByIdNotFound() {
        // Arrange
        when(paymentRepository.findById("INVALID"))
            .thenReturn(Optional.empty());
        
        // Act
        Optional<Payment> result = paymentService.findById("INVALID");
        
        // Assert
        assertThat(result).isEmpty();
    }
}
```

---

### Parameterized Tests

```java
package org.sudhir512kj.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ParameterizedTestExamples {
    
    // Test with multiple values
    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t", "\n"})
    @DisplayName("Should validate empty strings")
    void testEmptyStrings(String input) {
        assertThat(input.trim()).isEmpty();
    }
    
    // Test with multiple integers
    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, 7, 9})
    @DisplayName("Should validate odd numbers")
    void testOddNumbers(int number) {
        assertThat(number % 2).isEqualTo(1);
    }
    
    // Test with CSV source
    @ParameterizedTest
    @CsvSource({
        "100, 10, 90",
        "200, 20, 160",
        "50, 5, 47.5"
    })
    @DisplayName("Should calculate discounted price")
    void testDiscountCalculation(double price, double discount, double expected) {
        PriceCalculatorService calculator = new PriceCalculatorService();
        double result = calculator.calculateDiscountedPrice(price, discount);
        assertThat(result).isEqualTo(expected);
    }
    
    // Test with method source
    @ParameterizedTest
    @MethodSource("providePaymentTestData")
    @DisplayName("Should validate payment amounts")
    void testPaymentValidation(Payment payment, boolean expectedValid) {
        PaymentValidator validator = new PaymentValidator();
        boolean result = validator.isValid(payment);
        assertThat(result).isEqualTo(expectedValid);
    }
    
    private static Stream<Arguments> providePaymentTestData() {
        return Stream.of(
            Arguments.of(new Payment(100.0, "USD"), true),
            Arguments.of(new Payment(-50.0, "USD"), false),
            Arguments.of(new Payment(0.0, "USD"), false),
            Arguments.of(new Payment(1000.0, null), false)
        );
    }
    
    // Test with enum source
    @ParameterizedTest
    @EnumSource(PaymentStatus.class)
    @DisplayName("Should handle all payment statuses")
    void testPaymentStatuses(PaymentStatus status) {
        assertThat(status).isNotNull();
        assertThat(status.name()).isNotEmpty();
    }
    
    // Test with CSV file
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data/payments.csv", numLinesToSkip = 1)
    @DisplayName("Should process payments from CSV")
    void testPaymentsFromCsv(String id, double amount, String currency) {
        Payment payment = new Payment(id, amount, currency);
        assertThat(payment.getAmount()).isPositive();
    }
}
```

---

### Nested Tests

```java
package org.sudhir512kj.payment.service;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment Service Tests")
class PaymentServiceNestedTest {
    
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }
    
    @Nested
    @DisplayName("When payment is valid")
    class ValidPaymentTests {
        
        private Payment validPayment;
        
        @BeforeEach
        void setUp() {
            validPayment = Payment.builder()
                .amount(100.0)
                .currency("USD")
                .build();
        }
        
        @Test
        @DisplayName("Should process payment successfully")
        void testProcessPayment() {
            Payment result = paymentService.processPayment(validPayment);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
        
        @Test
        @DisplayName("Should generate transaction ID")
        void testTransactionId() {
            Payment result = paymentService.processPayment(validPayment);
            assertThat(result.getTransactionId()).isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("When payment is invalid")
    class InvalidPaymentTests {
        
        @Test
        @DisplayName("Should reject negative amount")
        void testNegativeAmount() {
            Payment payment = Payment.builder()
                .amount(-100.0)
                .currency("USD")
                .build();
            
            assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(InvalidPaymentException.class);
        }
        
        @Test
        @DisplayName("Should reject null currency")
        void testNullCurrency() {
            Payment payment = Payment.builder()
                .amount(100.0)
                .currency(null)
                .build();
            
            assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(InvalidPaymentException.class);
        }
    }
    
    @Nested
    @DisplayName("When payment gateway fails")
    class GatewayFailureTests {
        
        @Test
        @DisplayName("Should retry on temporary failure")
        void testRetryOnFailure() {
            // Test retry logic
        }
        
        @Test
        @DisplayName("Should fail after max retries")
        void testMaxRetries() {
            // Test max retry limit
        }
    }
}
```

---

This completes Part 1. Part 2 will continue with Repository Testing, Controller Testing, MockMvc, and more advanced topics.

## Repository Testing

### @DataJpaTest - Testing JPA Repositories

```java
package org.sudhir512kj.payment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest  // Configures in-memory database and JPA components
class PaymentRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;  // For test data setup
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    private Payment payment1;
    private Payment payment2;
    
    @BeforeEach
    void setUp() {
        payment1 = Payment.builder()
            .amount(100.0)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .userId("user-1")
            .build();
        
        payment2 = Payment.builder()
            .amount(200.0)
            .currency("EUR")
            .status(PaymentStatus.PENDING)
            .userId("user-2")
            .build();
        
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.flush();
    }
    
    @Test
    void testFindById() {
        Optional<Payment> found = paymentRepository.findById(payment1.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(100.0);
        assertThat(found.get().getCurrency()).isEqualTo("USD");
    }
    
    @Test
    void testFindAll() {
        List<Payment> payments = paymentRepository.findAll();
        
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getAmount)
            .containsExactlyInAnyOrder(100.0, 200.0);
    }
    
    @Test
    void testSave() {
        Payment newPayment = Payment.builder()
            .amount(300.0)
            .currency("GBP")
            .status(PaymentStatus.PENDING)
            .userId("user-3")
            .build();
        
        Payment saved = paymentRepository.save(newPayment);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualTo(300.0);
    }
    
    @Test
    void testUpdate() {
        payment1.setStatus(PaymentStatus.REFUNDED);
        Payment updated = paymentRepository.save(payment1);
        
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
    
    @Test
    void testDelete() {
        paymentRepository.delete(payment1);
        
        Optional<Payment> found = paymentRepository.findById(payment1.getId());
        assertThat(found).isEmpty();
    }
    
    @Test
    void testFindByUserId() {
        List<Payment> payments = paymentRepository.findByUserId("user-1");
        
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getUserId()).isEqualTo("user-1");
    }
    
    @Test
    void testFindByStatus() {
        List<Payment> completedPayments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);
        
        assertThat(completedPayments).hasSize(1);
        assertThat(completedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
    
    @Test
    void testCustomQuery() {
        List<Payment> payments = paymentRepository.findPaymentsByAmountGreaterThan(150.0);
        
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmount()).isEqualTo(200.0);
    }
}
```

---

### Repository with Custom Queries

```java
package org.sudhir512kj.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    List<Payment> findByUserId(String userId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.amount > :amount")
    List<Payment> findPaymentsByAmountGreaterThan(@Param("amount") Double amount);
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :start AND :end")
    List<Payment> findPaymentsBetweenDates(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query(value = "SELECT * FROM payments WHERE user_id = ?1 AND status = ?2", 
           nativeQuery = true)
    List<Payment> findByUserIdAndStatusNative(String userId, String status);
}
```

---

### Testing with @Sql Scripts

```java
package org.sudhir512kj.payment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class PaymentRepositorySqlTest {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Test
    @Sql("/test-data/payments.sql")  // Execute SQL before test
    void testWithSqlScript() {
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(5);
    }
    
    @Test
    @Sql(scripts = {"/test-data/schema.sql", "/test-data/data.sql"})
    void testWithMultipleSqlScripts() {
        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);
        assertThat(payments).isNotEmpty();
    }
    
    @Test
    @Sql(scripts = "/test-data/payments.sql", 
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/test-data/cleanup.sql", 
         executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testWithCleanup() {
        // Test logic
    }
}
```

---

## Controller Testing

### @WebMvcTest - Testing REST Controllers

```java
package org.sudhir512kj.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(PaymentController.class)  // Only load PaymentController
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PaymentService paymentService;
    
    private Payment payment;
    private PaymentRequest paymentRequest;
    
    @BeforeEach
    void setUp() {
        payment = Payment.builder()
            .id("PAY-123")
            .amount(100.0)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .build();
        
        paymentRequest = PaymentRequest.builder()
            .amount(100.0)
            .currency("USD")
            .userId("user-1")
            .build();
    }
    
    @Test
    void testGetPaymentById() throws Exception {
        when(paymentService.findById("PAY-123"))
            .thenReturn(Optional.of(payment));
        
        mockMvc.perform(get("/api/payments/{id}", "PAY-123"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("PAY-123"))
            .andExpect(jsonPath("$.amount").value(100.0))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
        
        verify(paymentService, times(1)).findById("PAY-123");
    }
    
    @Test
    void testGetPaymentByIdNotFound() throws Exception {
        when(paymentService.findById("INVALID"))
            .thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/payments/{id}", "INVALID"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void testCreatePayment() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class)))
            .thenReturn(payment);
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("PAY-123"))
            .andExpect(jsonPath("$.amount").value(100.0));
        
        verify(paymentService, times(1)).createPayment(any(PaymentRequest.class));
    }
    
    @Test
    void testCreatePaymentInvalidRequest() throws Exception {
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .amount(-100.0)  // Invalid negative amount
            .currency("USD")
            .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetAllPayments() throws Exception {
        Payment payment2 = Payment.builder()
            .id("PAY-456")
            .amount(200.0)
            .currency("EUR")
            .status(PaymentStatus.PENDING)
            .build();
        
        when(paymentService.findAll())
            .thenReturn(Arrays.asList(payment, payment2));
        
        mockMvc.perform(get("/api/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value("PAY-123"))
            .andExpect(jsonPath("$[1].id").value("PAY-456"));
    }
    
    @Test
    void testUpdatePayment() throws Exception {
        Payment updatedPayment = Payment.builder()
            .id("PAY-123")
            .amount(150.0)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .build();
        
        when(paymentService.updatePayment(eq("PAY-123"), any(PaymentRequest.class)))
            .thenReturn(updatedPayment);
        
        mockMvc.perform(put("/api/payments/{id}", "PAY-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(150.0));
    }
    
    @Test
    void testDeletePayment() throws Exception {
        doNothing().when(paymentService).deletePayment("PAY-123");
        
        mockMvc.perform(delete("/api/payments/{id}", "PAY-123"))
            .andExpect(status().isNoContent());
        
        verify(paymentService, times(1)).deletePayment("PAY-123");
    }
    
    @Test
    void testGetPaymentsByUserId() throws Exception {
        when(paymentService.findByUserId("user-1"))
            .thenReturn(Arrays.asList(payment));
        
        mockMvc.perform(get("/api/payments/user/{userId}", "user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value("PAY-123"));
    }
}
```

---

### Controller with Request Validation

```java
package org.sudhir512kj.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.*;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable String id) {
        return paymentService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// DTO with validation
class PaymentRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    // Getters and setters
}
```

---

### Testing Validation

```java
@WebMvcTest(PaymentController.class)
class PaymentControllerValidationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PaymentService paymentService;
    
    @Test
    void testCreatePaymentWithNullAmount() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
            .amount(null)  // Null amount
            .currency("USD")
            .userId("user-1")
            .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].field").value(hasItem("amount")))
            .andExpect(jsonPath("$.errors[*].message").value(hasItem("Amount is required")));
    }
    
    @Test
    void testCreatePaymentWithNegativeAmount() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
            .amount(-100.0)  // Negative amount
            .currency("USD")
            .userId("user-1")
            .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].message").value(hasItem("Amount must be positive")));
    }
    
    @Test
    void testCreatePaymentWithInvalidCurrency() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
            .amount(100.0)
            .currency("US")  // Invalid currency (not 3 chars)
            .userId("user-1")
            .build();
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].field").value(hasItem("currency")));
    }
}
```

---

## MockMvc Testing

### Advanced MockMvc Examples

```java
package org.sudhir512kj.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerMockMvcTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testWithHeaders() throws Exception {
        mockMvc.perform(get("/api/payments/PAY-123")
                .header("Authorization", "Bearer token123")
                .header("X-Request-ID", "req-456"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Response-Time"));
    }
    
    @Test
    void testWithQueryParams() throws Exception {
        mockMvc.perform(get("/api/payments")
                .param("status", "COMPLETED")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    void testWithPathVariables() throws Exception {
        mockMvc.perform(get("/api/payments/{id}/refund", "PAY-123"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testWithCookies() throws Exception {
        mockMvc.perform(get("/api/payments")
                .cookie(new Cookie("session", "abc123")))
            .andExpect(status().isOk());
    }
    
    @Test
    void testWithPrintResults() throws Exception {
        mockMvc.perform(get("/api/payments/PAY-123"))
            .andDo(print())  // Print request/response details
            .andExpect(status().isOk());
    }
    
    @Test
    void testWithMvcResult() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/payments/PAY-123"))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        System.out.println("Response: " + content);
    }
    
    @Test
    void testFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "receipt.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "PDF content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/payments/PAY-123/receipt")
                .file(file))
            .andExpect(status().isOk());
    }
    
    @Test
    void testAsyncRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/payments/async"))
            .andExpect(request().asyncStarted())
            .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
```

---

## Integration Testing

### @SpringBootTest - Full Integration Tests

```java
package org.sudhir512kj.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/payments";
        paymentRepository.deleteAll();
    }
    
    @Test
    void testCreatePayment() {
        PaymentRequest request = PaymentRequest.builder()
            .amount(100.0)
            .currency("USD")
            .userId("user-1")
            .build();
        
        ResponseEntity<Payment> response = restTemplate.postForEntity(
            baseUrl,
            request,
            Payment.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getAmount()).isEqualTo(100.0);
    }
    
    @Test
    void testGetPayment() {
        // Create payment first
        Payment payment = paymentRepository.save(
            Payment.builder()
                .amount(100.0)
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .build()
        );
        
        // Get payment
        ResponseEntity<Payment> response = restTemplate.getForEntity(
            baseUrl + "/" + payment.getId(),
            Payment.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(payment.getId());
    }
    
    @Test
    void testGetPaymentNotFound() {
        ResponseEntity<Payment> response = restTemplate.getForEntity(
            baseUrl + "/INVALID",
            Payment.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void testUpdatePayment() {
        Payment payment = paymentRepository.save(
            Payment.builder()
                .amount(100.0)
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .build()
        );
        
        PaymentRequest updateRequest = PaymentRequest.builder()
            .amount(150.0)
            .currency("USD")
            .userId("user-1")
            .build();
        
        restTemplate.put(
            baseUrl + "/" + payment.getId(),
            updateRequest
        );
        
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getAmount()).isEqualTo(150.0);
    }
    
    @Test
    void testDeletePayment() {
        Payment payment = paymentRepository.save(
            Payment.builder()
                .amount(100.0)
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .build()
        );
        
        restTemplate.delete(baseUrl + "/" + payment.getId());
        
        assertThat(paymentRepository.findById(payment.getId())).isEmpty();
    }
    
    @Test
    void testCompletePaymentFlow() {
        // 1. Create payment
        PaymentRequest request = PaymentRequest.builder()
            .amount(100.0)
            .currency("USD")
            .userId("user-1")
            .build();
        
        ResponseEntity<Payment> createResponse = restTemplate.postForEntity(
            baseUrl,
            request,
            Payment.class
        );
        
        String paymentId = createResponse.getBody().getId();
        
        // 2. Get payment
        ResponseEntity<Payment> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + paymentId,
            Payment.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 3. Process payment
        restTemplate.postForEntity(
            baseUrl + "/" + paymentId + "/process",
            null,
            Payment.class
        );
        
        // 4. Verify status
        Payment processed = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
```

---

This completes Part 2. Part 3 will continue with Mocking, Test Configuration, TestContainers, and Best Practices.

## Mocking with Mockito

### Basic Mockito Usage

```java
package org.sudhir512kj.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MockitoExamplesTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private PaymentGateway paymentGateway;
    
    @InjectMocks
    private PaymentService paymentService;
    
    // 1. Basic stubbing
    @Test
    void testBasicStubbing() {
        Payment payment = new Payment("PAY-123", 100.0);
        
        when(paymentRepository.findById("PAY-123"))
            .thenReturn(Optional.of(payment));
        
        Optional<Payment> result = paymentService.findById("PAY-123");
        
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("PAY-123");
    }
    
    // 2. Argument matchers
    @Test
    void testArgumentMatchers() {
        when(paymentRepository.save(any(Payment.class)))
            .thenReturn(new Payment("PAY-456", 200.0));
        
        when(paymentGateway.charge(anyDouble(), eq("USD")))
            .thenReturn(true);
        
        Payment result = paymentService.createPayment(100.0, "USD");
        
        assertThat(result.getId()).isEqualTo("PAY-456");
    }
    
    // 3. Verify interactions
    @Test
    void testVerifyInteractions() {
        Payment payment = new Payment("PAY-123", 100.0);
        
        paymentService.processPayment(payment);
        
        verify(paymentGateway, times(1)).charge(100.0, "USD");
        verify(paymentRepository, times(1)).save(payment);
        verifyNoMoreInteractions(paymentGateway);
    }
    
    // 4. Verify order of invocations
    @Test
    void testVerifyOrder() {
        Payment payment = new Payment("PAY-123", 100.0);
        
        paymentService.processPayment(payment);
        
        InOrder inOrder = inOrder(paymentGateway, paymentRepository);
        inOrder.verify(paymentGateway).charge(100.0, "USD");
        inOrder.verify(paymentRepository).save(payment);
    }
    
    // 5. Throw exceptions
    @Test
    void testThrowException() {
        when(paymentGateway.charge(anyDouble(), anyString()))
            .thenThrow(new PaymentGatewayException("Connection failed"));
        
        assertThatThrownBy(() -> paymentService.processPayment(new Payment("PAY-123", 100.0)))
            .isInstanceOf(PaymentProcessingException.class)
            .hasMessageContaining("Connection failed");
    }
    
    // 6. Multiple return values
    @Test
    void testMultipleReturnValues() {
        when(paymentGateway.charge(anyDouble(), anyString()))
            .thenReturn(false)
            .thenReturn(false)
            .thenReturn(true);
        
        boolean result1 = paymentGateway.charge(100.0, "USD");
        boolean result2 = paymentGateway.charge(100.0, "USD");
        boolean result3 = paymentGateway.charge(100.0, "USD");
        
        assertThat(result1).isFalse();
        assertThat(result2).isFalse();
        assertThat(result3).isTrue();
    }
    
    // 7. Answer with custom logic
    @Test
    void testAnswer() {
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId("GENERATED-ID");
                return payment;
            });
        
        Payment payment = new Payment(null, 100.0);
        Payment saved = paymentRepository.save(payment);
        
        assertThat(saved.getId()).isEqualTo("GENERATED-ID");
    }
    
    // 8. Capture arguments
    @Test
    void testArgumentCaptor() {
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        
        Payment payment = new Payment("PAY-123", 100.0);
        paymentService.processPayment(payment);
        
        verify(paymentRepository).save(captor.capture());
        
        Payment captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
    
    // 9. Spy - partial mocking
    @Test
    void testSpy() {
        PaymentService realService = new PaymentService(paymentRepository, paymentGateway);
        PaymentService spyService = spy(realService);
        
        doReturn(true).when(spyService).validatePayment(any());
        
        // Real method is called for processPayment
        // Mocked method is called for validatePayment
    }
    
    // 10. Void methods
    @Test
    void testVoidMethods() {
        doNothing().when(paymentGateway).sendNotification(anyString());
        
        paymentGateway.sendNotification("Payment completed");
        
        verify(paymentGateway, times(1)).sendNotification("Payment completed");
    }
    
    // 11. Consecutive calls
    @Test
    void testConsecutiveCalls() {
        when(paymentGateway.getStatus("PAY-123"))
            .thenReturn("PENDING")
            .thenReturn("PROCESSING")
            .thenReturn("COMPLETED");
        
        assertThat(paymentGateway.getStatus("PAY-123")).isEqualTo("PENDING");
        assertThat(paymentGateway.getStatus("PAY-123")).isEqualTo("PROCESSING");
        assertThat(paymentGateway.getStatus("PAY-123")).isEqualTo("COMPLETED");
    }
}
```

---

### @MockBean and @SpyBean in Spring

```java
package org.sudhir512kj.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class MockBeanExampleTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean  // Adds mock to Spring context
    private PaymentService paymentService;
    
    @SpyBean  // Adds spy to Spring context (partial mocking)
    private PaymentValidator paymentValidator;
    
    @Test
    void testWithMockBean() throws Exception {
        Payment payment = new Payment("PAY-123", 100.0);
        
        when(paymentService.findById("PAY-123"))
            .thenReturn(Optional.of(payment));
        
        mockMvc.perform(get("/api/payments/PAY-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("PAY-123"));
    }
    
    @Test
    void testWithSpyBean() throws Exception {
        // Real validation logic is called
        // But you can override specific methods
        doReturn(true).when(paymentValidator).isValidAmount(anyDouble());
        
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": -100, \"currency\": \"USD\"}"))
            .andExpect(status().isOk());  // Validation bypassed
    }
}
```

---

## Test Configuration

### Custom Test Configuration

```java
package org.sudhir512kj.payment.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary  // Override production bean
    public PaymentGateway testPaymentGateway() {
        return new MockPaymentGateway();
    }
    
    @Bean
    public TestDataBuilder testDataBuilder() {
        return new TestDataBuilder();
    }
}
```

**Usage:**
```java
@SpringBootTest
@Import(TestConfig.class)  // Import test configuration
class PaymentServiceIntegrationTest {
    
    @Autowired
    private PaymentGateway paymentGateway;  // Uses MockPaymentGateway
    
    @Autowired
    private TestDataBuilder testDataBuilder;
    
    @Test
    void testWithCustomConfig() {
        // Test logic
    }
}
```

---

### Test Properties

**application-test.yml:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  redis:
    host: localhost
    port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092

logging:
  level:
    org.sudhir512kj: DEBUG
    org.springframework.web: DEBUG
```

**Usage:**
```java
@SpringBootTest
@ActiveProfiles("test")  // Use application-test.yml
class PaymentServiceTest {
    // Tests
}
```

---

### @TestPropertySource

```java
@SpringBootTest
@TestPropertySource(properties = {
    "payment.gateway.url=http://mock-gateway.com",
    "payment.gateway.timeout=5000",
    "payment.retry.max-attempts=3"
})
class PaymentServicePropertyTest {
    
    @Value("${payment.gateway.url}")
    private String gatewayUrl;
    
    @Test
    void testWithCustomProperties() {
        assertThat(gatewayUrl).isEqualTo("http://mock-gateway.com");
    }
}
```

---

## TestContainers - Real Database Testing

### PostgreSQL TestContainer

```java
package org.sudhir512kj.payment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTestContainersTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
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
    private PaymentRepository paymentRepository;
    
    @Test
    void testWithRealPostgres() {
        Payment payment = Payment.builder()
            .amount(100.0)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .build();
        
        Payment saved = paymentRepository.save(payment);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(paymentRepository.findById(saved.getId())).isPresent();
    }
}
```

---

### Redis TestContainer

```java
@SpringBootTest
@Testcontainers
class PaymentCacheTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private PaymentCacheService cacheService;
    
    @Test
    void testRedisCache() {
        Payment payment = new Payment("PAY-123", 100.0);
        
        cacheService.cache(payment);
        
        Optional<Payment> cached = cacheService.get("PAY-123");
        assertThat(cached).isPresent();
    }
}
```

---

### Kafka TestContainer

```java
@SpringBootTest
@Testcontainers
class PaymentEventPublisherTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private PaymentEventPublisher eventPublisher;
    
    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    @Test
    void testPublishEvent() {
        PaymentEvent event = new PaymentEvent("PAY-123", "COMPLETED");
        
        eventPublisher.publish(event);
        
        // Verify event was published
    }
}
```

---

## Best Practices

### 1. Test Naming Conventions

```java
// Good naming
@Test
void shouldCreatePaymentWhenValidRequest() { }

@Test
void shouldThrowExceptionWhenAmountIsNegative() { }

@Test
void shouldReturnEmptyWhenPaymentNotFound() { }

// Use @DisplayName for better readability
@Test
@DisplayName("Should process payment successfully when gateway is available")
void testPaymentProcessing() { }
```

---

### 2. AAA Pattern (Arrange-Act-Assert)

```java
@Test
void testPaymentCreation() {
    // Arrange - Setup test data
    PaymentRequest request = PaymentRequest.builder()
        .amount(100.0)
        .currency("USD")
        .build();
    
    when(paymentGateway.charge(any())).thenReturn(true);
    
    // Act - Execute the method under test
    Payment result = paymentService.createPayment(request);
    
    // Assert - Verify the results
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    verify(paymentGateway, times(1)).charge(any());
}
```

---

### 3. Test Data Builders

```java
public class PaymentTestDataBuilder {
    
    private String id = "PAY-123";
    private Double amount = 100.0;
    private String currency = "USD";
    private PaymentStatus status = PaymentStatus.PENDING;
    
    public static PaymentTestDataBuilder aPayment() {
        return new PaymentTestDataBuilder();
    }
    
    public PaymentTestDataBuilder withId(String id) {
        this.id = id;
        return this;
    }
    
    public PaymentTestDataBuilder withAmount(Double amount) {
        this.amount = amount;
        return this;
    }
    
    public PaymentTestDataBuilder withStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }
    
    public Payment build() {
        return Payment.builder()
            .id(id)
            .amount(amount)
            .currency(currency)
            .status(status)
            .build();
    }
}

// Usage
@Test
void testWithBuilder() {
    Payment payment = aPayment()
        .withAmount(200.0)
        .withStatus(PaymentStatus.COMPLETED)
        .build();
    
    assertThat(payment.getAmount()).isEqualTo(200.0);
}
```

---

### 4. Don't Test Framework Code

```java
// Bad - Testing Spring Data JPA (framework code)
@Test
void testSave() {
    Payment payment = new Payment();
    Payment saved = paymentRepository.save(payment);
    assertThat(saved).isNotNull();
}

// Good - Test custom query logic
@Test
void testFindPaymentsByAmountGreaterThan() {
    paymentRepository.save(new Payment(100.0));
    paymentRepository.save(new Payment(200.0));
    
    List<Payment> result = paymentRepository.findByAmountGreaterThan(150.0);
    
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAmount()).isEqualTo(200.0);
}
```

---

### 5. Test One Thing at a Time

```java
// Bad - Testing multiple things
@Test
void testPaymentProcessing() {
    Payment payment = paymentService.createPayment(request);
    assertThat(payment.getId()).isNotNull();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    
    Payment found = paymentService.findById(payment.getId()).get();
    assertThat(found).isNotNull();
    
    paymentService.refundPayment(payment.getId());
    Payment refunded = paymentService.findById(payment.getId()).get();
    assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
}

// Good - Separate tests
@Test
void shouldCreatePaymentWithGeneratedId() {
    Payment payment = paymentService.createPayment(request);
    assertThat(payment.getId()).isNotNull();
}

@Test
void shouldSetStatusToCompletedAfterCreation() {
    Payment payment = paymentService.createPayment(request);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
}

@Test
void shouldRefundPaymentSuccessfully() {
    Payment payment = createTestPayment();
    paymentService.refundPayment(payment.getId());
    
    Payment refunded = paymentService.findById(payment.getId()).get();
    assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
}
```

---

### 6. Use @BeforeEach for Common Setup

```java
class PaymentServiceTest {
    
    private PaymentService paymentService;
    private PaymentRepository paymentRepository;
    private Payment testPayment;
    
    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentService = new PaymentService(paymentRepository);
        
        testPayment = Payment.builder()
            .id("PAY-123")
            .amount(100.0)
            .currency("USD")
            .build();
    }
    
    @Test
    void test1() {
        // Use testPayment
    }
    
    @Test
    void test2() {
        // Use testPayment
    }
}
```

---

### 7. Clean Up Resources

```java
class PaymentServiceTest {
    
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(5);
    }
    
    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }
    
    @Test
    void testAsyncPayment() {
        // Test logic
    }
}
```

---

### 8. Test Coverage Goals

```
Minimum Coverage Targets:
- Unit Tests: 80%+ code coverage
- Integration Tests: Critical paths covered
- Controller Tests: All endpoints tested
- Repository Tests: Custom queries tested
```

**Maven Plugin for Coverage:**
```xml
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
    </executions>
</plugin>
```

---

## Quick Reference

### Common Annotations

| Annotation | Purpose |
|------------|---------|
| `@Test` | Mark test method |
| `@SpringBootTest` | Full integration test |
| `@WebMvcTest` | Controller layer test |
| `@DataJpaTest` | Repository layer test |
| `@MockBean` | Mock Spring bean |
| `@Mock` | Mock object (Mockito) |
| `@InjectMocks` | Inject mocks into object |
| `@BeforeEach` | Setup before each test |
| `@AfterEach` | Cleanup after each test |
| `@DisplayName` | Custom test name |
| `@Disabled` | Skip test |
| `@ParameterizedTest` | Data-driven test |

---

### Mockito Cheat Sheet

```java
// Stubbing
when(mock.method()).thenReturn(value);
when(mock.method()).thenThrow(exception);
doReturn(value).when(mock).method();
doThrow(exception).when(mock).method();

// Verification
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();
verify(mock, atLeast(1)).method();
verify(mock, atMost(3)).method();

// Argument matchers
any(), anyString(), anyInt(), anyDouble()
eq(value), isNull(), isNotNull()
argThat(matcher)

// Argument captor
ArgumentCaptor<Type> captor = ArgumentCaptor.forClass(Type.class);
verify(mock).method(captor.capture());
Type value = captor.getValue();
```

---

### AssertJ Cheat Sheet

```java
// Basic assertions
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();
assertThat(actual).isInstanceOf(Type.class);

// String assertions
assertThat(string).startsWith("prefix");
assertThat(string).endsWith("suffix");
assertThat(string).contains("substring");
assertThat(string).matches("regex");

// Collection assertions
assertThat(list).hasSize(3);
assertThat(list).contains(element);
assertThat(list).containsExactly(e1, e2, e3);
assertThat(list).isEmpty();

// Exception assertions
assertThatThrownBy(() -> method())
    .isInstanceOf(Exception.class)
    .hasMessage("message");
```

---

## Running Tests

### Maven Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentServiceTest

# Run specific test method
mvn test -Dtest=PaymentServiceTest#testCreatePayment

# Run tests with coverage
mvn clean test jacoco:report

# Skip tests
mvn install -DskipTests

# Run only integration tests
mvn verify -P integration-tests
```

### Gradle Commands

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests PaymentServiceTest

# Run specific test method
./gradlew test --tests PaymentServiceTest.testCreatePayment

# Run with coverage
./gradlew test jacocoTestReport

# Skip tests
./gradlew build -x test
```

---

## Summary

### Testing Layers

| Layer | Annotation | Purpose | Speed |
|-------|-----------|---------|-------|
| Unit | `@ExtendWith(MockitoExtension.class)` | Test single class | Fast |
| Repository | `@DataJpaTest` | Test data access | Medium |
| Controller | `@WebMvcTest` | Test REST endpoints | Medium |
| Integration | `@SpringBootTest` | Test full application | Slow |

### Test Pyramid

```
     E2E (Few, Slow)
    /              \
   Integration      
  /   (Some)        \
 /                   \
Unit Tests (Many, Fast)
```

---

## References

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [TestContainers](https://www.testcontainers.org/)

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: System Designs Collection
