# Testing Documentation

Comprehensive testing guides for Java 21 and Spring Boot applications.

## 📚 Documentation Structure

### [Testing Deep Dive](Testing_Deep_Dive.md)
Complete guide covering:
- Testing Pyramid and test types
- JUnit 5 (Jupiter) with all features
- Mockito for mocking and stubbing
- Spring Boot Test annotations
- AssertJ for fluent assertions
- Testcontainers for integration testing

### [Testing Deep Dive - Part 2](Testing_Deep_Dive_Part2.md)
Advanced topics including:
- REST API testing (MockMvc, RestAssured, WebTestClient)
- Reactive testing with Project Reactor
- Performance testing (JMH, Gatling)
- Best practices and patterns
- Test coverage and quality

## 🎯 Quick Reference

### Test Types Distribution
```
Unit Tests:        70% - Fast, isolated, mocked dependencies
Integration Tests: 20% - Database, messaging, external APIs
E2E Tests:         10% - Full application stack
```

### Essential Dependencies

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

### Common Test Patterns

#### Unit Test
```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
    
    @Test
    void testMethod() {
        when(repository.find()).thenReturn(data);
        var result = service.process();
        assertThat(result).isNotNull();
    }
}
```

#### Integration Test
```java
@SpringBootTest
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private Service service;
    
    @Test
    void testWithRealDatabase() {
        var result = service.save(data);
        assertThat(result.getId()).isNotNull();
    }
}
```

#### API Test
```java
@WebMvcTest(Controller.class)
class ApiTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private Service service;
    
    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/api/resource"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("value"));
    }
}
```

## 🚀 Getting Started

1. **Read the fundamentals**: Start with [Testing Deep Dive](Testing_Deep_Dive.md)
2. **Explore advanced topics**: Continue with [Part 2](Testing_Deep_Dive_Part2.md)
3. **Apply patterns**: Use examples in your projects
4. **Measure coverage**: Aim for >80% code coverage
5. **Follow best practices**: Write clean, maintainable tests

## 📊 Testing Frameworks Comparison

| Framework | Purpose | Use Case |
|-----------|---------|----------|
| JUnit 5 | Test execution | All tests |
| Mockito | Mocking | Unit tests |
| Spring Boot Test | Spring integration | Integration tests |
| AssertJ | Assertions | All tests |
| Testcontainers | Real infrastructure | Integration tests |
| RestAssured | API testing | E2E tests |
| WebTestClient | Reactive API testing | WebFlux tests |
| StepVerifier | Reactive testing | Reactor tests |
| JMH | Microbenchmarks | Performance tests |
| Gatling | Load testing | Performance tests |

## 🎓 Learning Path

### Beginner
1. JUnit 5 basics (@Test, assertions)
2. Mockito fundamentals (mock, when, verify)
3. Spring Boot Test (@SpringBootTest)
4. Basic API testing (MockMvc)

### Intermediate
1. Parameterized tests
2. Advanced mocking (ArgumentCaptor, Spy)
3. @WebMvcTest, @DataJpaTest
4. Testcontainers basics
5. AssertJ fluent assertions

### Advanced
1. Reactive testing (StepVerifier)
2. Performance testing (JMH, Gatling)
3. Custom test annotations
4. Test architecture patterns
5. CI/CD integration

## 💡 Best Practices Summary

1. **Follow AAA Pattern**: Arrange, Act, Assert
2. **One assertion per test**: Focus on single behavior
3. **Independent tests**: No shared state
4. **Fast execution**: Unit tests <1ms, integration <1s
5. **Clear naming**: Describe what is tested
6. **Use builders**: Create test data easily
7. **Avoid flakiness**: No random, time-dependent logic
8. **Mock external dependencies**: Control test environment
9. **Test edge cases**: Null, empty, boundary values
10. **Document complex tests**: Explain non-obvious logic

## 🔗 Related Documentation

- [Spring WebFlux Testing](../webflux/Testing.md)
- [WebClient Testing](../webclient/Testing.md)
- System-specific test examples in each service documentation

## 📈 Metrics to Track

- **Code Coverage**: >80% line coverage
- **Test Execution Time**: <5 minutes for full suite
- **Flaky Test Rate**: <1%
- **Test-to-Code Ratio**: 1:1 or higher
- **Bug Escape Rate**: <5% to production

## 🛠️ Tools

- **IDE Plugins**: JUnit, Coverage (IntelliJ IDEA, VS Code)
- **Build Tools**: Maven Surefire, Gradle Test
- **Coverage**: JaCoCo, Cobertura
- **CI/CD**: GitHub Actions, Jenkins, GitLab CI
- **Reporting**: Allure, Surefire Reports

---

**Last Updated**: January 2024  
**Java Version**: 21  
**Spring Boot Version**: 3.2.x
