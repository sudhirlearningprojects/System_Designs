# Spring vs Spring Boot - Complete Comparison

## Quick Summary

| Aspect | Spring Framework | Spring Boot |
|--------|-----------------|-------------|
| **Configuration** | Manual XML/Java config | Auto-configuration |
| **Setup Time** | Hours/Days | Minutes |
| **Embedded Server** | ❌ Need external server | ✅ Built-in (Tomcat/Jetty) |
| **Dependency Management** | Manual version management | Starter dependencies |
| **Production Ready** | Need manual setup | Built-in actuator, metrics |
| **Learning Curve** | Steep | Gentle |
| **Use Case** | Full control needed | Rapid development |

---

## What is Spring Framework?

**Spring Framework** is a comprehensive framework for enterprise Java applications providing:
- Dependency Injection (IoC)
- AOP (Aspect-Oriented Programming)
- Transaction Management
- MVC Framework
- Data Access (JDBC, ORM)

**Problem**: Requires extensive configuration and setup.

---

## What is Spring Boot?

**Spring Boot** is built on top of Spring Framework to simplify development by:
- Auto-configuration
- Embedded servers
- Starter dependencies
- Production-ready features

**Goal**: "Just run" - minimal configuration, maximum productivity.

---

## Key Differences

### 1. Configuration

#### Spring Framework (Manual Configuration)

**XML Configuration**:
```xml
<!-- applicationContext.xml -->
<beans>
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
    </bean>
    
    <bean id="sessionFactory" class="org.springframework.orm.hibernate5.LocalSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="packagesToScan" value="com.example.model"/>
        <property name="hibernateProperties">
            <props>
                <prop key="hibernate.dialect">org.hibernate.dialect.MySQL5Dialect</prop>
                <prop key="hibernate.show_sql">true</prop>
            </props>
        </property>
    </bean>
    
    <bean id="transactionManager" class="org.springframework.orm.hibernate5.HibernateTransactionManager">
        <property name="sessionFactory" ref="sessionFactory"/>
    </bean>
</beans>
```

**Java Configuration**:
```java
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "com.example")
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        return dataSource;
    }
    
    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setPackagesToScan("com.example.model");
        
        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
        hibernateProperties.put("hibernate.show_sql", "true");
        sessionFactory.setHibernateProperties(hibernateProperties);
        
        return sessionFactory;
    }
    
    @Bean
    public HibernateTransactionManager transactionManager() {
        HibernateTransactionManager txManager = new HibernateTransactionManager();
        txManager.setSessionFactory(sessionFactory().getObject());
        return txManager;
    }
}
```

#### Spring Boot (Auto-Configuration)

**application.properties**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Main Class**:
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**That's it!** Spring Boot auto-configures DataSource, JPA, Transaction Manager, etc.

---

### 2. Dependency Management

#### Spring Framework (Manual)

**pom.xml**:
```xml
<dependencies>
    <!-- Need to specify exact versions -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-core</artifactId>
        <version>5.3.20</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>5.3.20</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <version>5.3.20</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>5.3.20</version>
    </dependency>
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-core</artifactId>
        <version>5.6.9.Final</version>
    </dependency>
    <!-- Many more dependencies... -->
</dependencies>
```

**Problem**: Version conflicts, compatibility issues.

#### Spring Boot (Starter Dependencies)

**pom.xml**:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
</parent>

<dependencies>
    <!-- Single starter includes all needed dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>
```

**Benefit**: Compatible versions managed automatically.

---

### 3. Embedded Server

#### Spring Framework

**Requires External Server**:
1. Install Tomcat/JBoss/WebLogic
2. Configure server
3. Package as WAR
4. Deploy to server

**web.xml**:
```xml
<web-app>
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/spring-servlet.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
```

#### Spring Boot

**Embedded Server (Tomcat/Jetty/Undertow)**:
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Run: `java -jar myapp.jar`

**Benefit**: No external server needed, runs anywhere.

---

### 4. Creating REST API

#### Spring Framework

**Controller**:
```java
@Controller
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @ResponseBody
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
    
    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    @ResponseBody
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
```

**Configuration**:
```java
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.example")
public class WebConfig implements WebMvcConfigurer {
    
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }
    
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
```

#### Spring Boot

**Controller**:
```java
@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
```

**That's it!** No additional configuration needed.

---

### 5. Database Configuration

#### Spring Framework

**Multiple Files Needed**:

**persistence.xml**:
```xml
<persistence>
    <persistence-unit name="myPU">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <properties>
            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL5Dialect"/>
            <property name="hibernate.hbm2ddl.auto" value="update"/>
            <property name="hibernate.show_sql" value="true"/>
        </properties>
    </persistence-unit>
</persistence>
```

**Java Config**:
```java
@Configuration
@EnableJpaRepositories(basePackages = "com.example.repository")
public class JpaConfig {
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.example.model");
        
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());
        
        return em;
    }
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        return dataSource;
    }
    
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.hbm2ddl.auto", "update");
        return properties;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }
}
```

#### Spring Boot

**application.properties**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
```

**Entity**:
```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // getters/setters
}
```

**Repository**:
```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

**Done!** Everything auto-configured.

---

### 6. Testing

#### Spring Framework

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    public void testGetUser() {
        User user = userService.getUserById(1L);
        assertNotNull(user);
    }
}
```

#### Spring Boot

```java
@SpringBootTest
public class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    public void testGetUser() {
        User user = userService.getUserById(1L);
        assertNotNull(user);
    }
}
```

**Simpler**: `@SpringBootTest` loads entire application context automatically.

---

### 7. Production Features

#### Spring Framework

**Manual Setup Required**:
- Health checks - implement yourself
- Metrics - integrate Micrometer manually
- Monitoring - configure manually
- Logging - setup Log4j/Logback

#### Spring Boot

**Built-in Actuator**:

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Endpoints Available**:
- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application info
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Logger configuration

**application.properties**:
```properties
management.endpoints.web.exposure.include=health,metrics,info
```

---

## Complete Example Comparison

### Spring Framework Application

**Project Structure**:
```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── config/
│   │       │   ├── AppConfig.java
│   │       │   ├── WebConfig.java
│   │       │   └── JpaConfig.java
│   │       ├── controller/
│   │       │   └── UserController.java
│   │       ├── service/
│   │       │   └── UserService.java
│   │       └── model/
│   │           └── User.java
│   ├── resources/
│   │   └── applicationContext.xml
│   └── webapp/
│       └── WEB-INF/
│           ├── web.xml
│           └── spring-servlet.xml
```

**Lines of Configuration**: ~200-300 lines

---

### Spring Boot Application

**Project Structure**:
```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── Application.java
│   │       ├── controller/
│   │       │   └── UserController.java
│   │       ├── service/
│   │       │   └── UserService.java
│   │       ├── repository/
│   │       │   └── UserRepository.java
│   │       └── model/
│   │           └── User.java
│   └── resources/
│       └── application.properties
```

**Lines of Configuration**: ~10-20 lines

---

## When to Use What?

### Use Spring Framework When:
✅ Need fine-grained control over configuration  
✅ Working with legacy applications  
✅ Specific custom requirements  
✅ Learning Spring internals  
✅ Integrating with existing enterprise systems

### Use Spring Boot When:
✅ Starting new projects (99% of cases)  
✅ Microservices architecture  
✅ Rapid prototyping  
✅ Cloud-native applications  
✅ REST APIs  
✅ Production-ready features needed  
✅ Want to focus on business logic

---

## Migration: Spring to Spring Boot

### Step 1: Add Spring Boot Parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
</parent>
```

### Step 2: Replace Dependencies

**Before**:
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
```

**After**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### Step 3: Create Main Class

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Step 4: Move Properties

Move XML/Java config to `application.properties`

### Step 5: Remove web.xml

Not needed with embedded server.

---

## Common Misconceptions

### Myth 1: "Spring Boot is a separate framework"
❌ **False**: Spring Boot is built on top of Spring Framework

### Myth 2: "Spring Boot is only for microservices"
❌ **False**: Can build any type of application

### Myth 3: "Spring Boot is slower than Spring"
❌ **False**: Same performance, just easier setup

### Myth 4: "Can't customize Spring Boot"
❌ **False**: Fully customizable, can override any auto-configuration

### Myth 5: "Spring Framework is outdated"
❌ **False**: Still actively maintained, Spring Boot uses it

---

## Architecture Comparison

### Spring Framework Architecture

```
Your Application
       ↓
Spring Framework (Core, MVC, Data, etc.)
       ↓
Manual Configuration (XML/Java)
       ↓
External Server (Tomcat/JBoss)
```

### Spring Boot Architecture

```
Your Application
       ↓
Spring Boot (Auto-configuration)
       ↓
Spring Framework (Core, MVC, Data, etc.)
       ↓
Embedded Server (Tomcat/Jetty)
```

---

## Starter Dependencies

### Common Spring Boot Starters

| Starter | Purpose |
|---------|---------|
| `spring-boot-starter-web` | Web applications, REST APIs |
| `spring-boot-starter-data-jpa` | JPA with Hibernate |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-test` | Testing (JUnit, Mockito) |
| `spring-boot-starter-actuator` | Production monitoring |
| `spring-boot-starter-data-mongodb` | MongoDB |
| `spring-boot-starter-data-redis` | Redis |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-mail` | Email support |
| `spring-boot-starter-cache` | Caching |

---

## Auto-Configuration Magic

### How Spring Boot Auto-Configuration Works

```java
@SpringBootApplication
// Equivalent to:
@Configuration          // Marks as configuration class
@EnableAutoConfiguration // Enables auto-configuration
@ComponentScan          // Scans for components
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Behind the Scenes**:
1. Scans classpath for dependencies
2. Checks conditions (`@ConditionalOnClass`, `@ConditionalOnMissingBean`)
3. Auto-configures beans based on dependencies
4. Allows overriding with custom configuration

**Example**:
```java
// Spring Boot sees spring-boot-starter-data-jpa
// Automatically configures:
// - DataSource
// - EntityManagerFactory
// - TransactionManager
// - JPA repositories
```

---

## Key Takeaways

1. **Spring Boot = Spring Framework + Auto-configuration + Embedded Server + Starters**
2. **Spring Framework**: Full control, more configuration
3. **Spring Boot**: Convention over configuration, rapid development
4. **Not competing**: Spring Boot uses Spring Framework internally
5. **Modern choice**: Spring Boot for new projects (industry standard)
6. **Learning path**: Learn Spring Framework concepts, use Spring Boot for development
7. **Production ready**: Spring Boot has built-in monitoring, health checks
8. **Microservices**: Spring Boot is the de facto standard

---

## Quick Reference

### Spring Framework
```java
// Lots of configuration
@Configuration
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan
public class AppConfig {
    @Bean
    public DataSource dataSource() { /* config */ }
    @Bean
    public EntityManagerFactory emf() { /* config */ }
    // Many more beans...
}
```

### Spring Boot
```java
// Minimal configuration
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Bottom Line**: Spring Boot makes Spring Framework development faster and easier without sacrificing power or flexibility!
