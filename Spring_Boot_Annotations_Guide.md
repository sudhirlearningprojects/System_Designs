# Spring Boot Annotations Guide - Top 100 Interview & Production Ready Annotations

A comprehensive guide to the most important Spring Boot annotations used in production and frequently asked in interviews.

## Table of Contents

1. [Core Spring Boot Annotations](#core-spring-boot-annotations)
2. [Web Layer Annotations](#web-layer-annotations)
3. [Data Access Annotations](#data-access-annotations)
4. [Security Annotations](#security-annotations)
5. [Testing Annotations](#testing-annotations)
6. [Configuration Annotations](#configuration-annotations)
7. [Validation Annotations](#validation-annotations)
8. [Caching Annotations](#caching-annotations)
9. [Scheduling Annotations](#scheduling-annotations)
10. [Messaging Annotations](#messaging-annotations)

---

## Core Spring Boot Annotations

### 1. @SpringBootApplication
**Theory**: This is a convenience meta-annotation that combines three essential Spring Boot annotations:
- @Configuration: Tells Spring this class contains bean definitions
- @EnableAutoConfiguration: Enables Spring Boot's auto-configuration mechanism that automatically configures beans based on classpath dependencies
- @ComponentScan: Scans the current package and sub-packages for Spring components (@Component, @Service, @Repository, @Controller)

When Spring Boot starts, it looks for this annotation to bootstrap the application. It's the entry point that tells Spring Boot "start here and configure everything automatically based on what's available in the classpath".

**Use**: Main application class entry point.
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. @Component
**Theory**: This is the most basic stereotype annotation in Spring. When you annotate a class with @Component, you're telling Spring's IoC (Inversion of Control) container to manage this class as a bean. Spring will:
- Create an instance of this class during application startup
- Manage its lifecycle (creation, initialization, destruction)
- Make it available for dependency injection into other beans
- Apply any configured aspects (like transactions, security, etc.)

It's the parent annotation for all other stereotype annotations (@Service, @Repository, @Controller). Spring scans for @Component during component scanning and registers it in the application context.

**Use**: Mark any class as Spring bean.
```java
@Component
public class EmailService {
    public void sendEmail(String message) {}
}
```

### 3. @Service
**Theory**: A specialized form of @Component that indicates this class belongs to the service layer of your application architecture. While functionally identical to @Component, @Service provides semantic meaning:
- Indicates this class contains business logic
- Makes code more readable and self-documenting
- Allows for future Spring enhancements specific to service layer
- Helps with architectural clarity in layered applications
- Can be targeted by specific aspects or configurations

Service layer typically sits between the controller (presentation) layer and repository (data access) layer, containing the core business rules and orchestrating data operations.

**Use**: Business logic layer.
```java
@Service
public class UserService {
    public User findById(Long id) { return null; }
}
```

### 4. @Repository
**Theory**: A specialized @Component for data access layer classes with additional functionality:
- **Exception Translation**: Automatically translates database-specific exceptions (like SQLException) into Spring's DataAccessException hierarchy
- **Persistence Layer Indicator**: Clearly marks classes that interact with databases or external data sources
- **Future Enhancements**: Enables Spring to apply data-access-specific optimizations
- **AOP Integration**: Can be targeted by aspects for logging, caching, or transaction management

The exception translation is crucial because it abstracts away database-specific error codes and provides a consistent exception hierarchy regardless of the underlying database technology (MySQL, PostgreSQL, Oracle, etc.).

**Use**: Data access objects.
```java
@Repository
public class UserRepository {
    public User save(User user) { return user; }
}
```

### 5. @Controller
**Theory**: A specialized @Component that marks a class as a Spring MVC controller in the presentation layer. Key characteristics:
- **Request Handling**: Methods can handle HTTP requests when combined with @RequestMapping annotations
- **View Resolution**: Return values are interpreted as view names (JSP, Thymeleaf templates, etc.)
- **Model Binding**: Can populate model objects that are passed to views
- **Exception Handling**: Can define exception handlers for graceful error handling
- **Interceptor Integration**: Can be intercepted by Spring MVC interceptors

Unlike @RestController, @Controller is designed for traditional MVC applications where you return view names rather than data directly.

**Use**: MVC controllers.
```java
@Controller
public class HomeController {
    @RequestMapping("/")
    public String home() { return "index"; }
}
```

### 6. @RestController
**Theory**: A convenience annotation that combines @Controller and @ResponseBody, specifically designed for RESTful web services:
- **Automatic Serialization**: All method return values are automatically serialized to JSON/XML and written directly to HTTP response body
- **No View Resolution**: Bypasses Spring's view resolution mechanism entirely
- **Content Negotiation**: Supports automatic content type negotiation based on Accept headers
- **HTTP Method Mapping**: Works seamlessly with @GetMapping, @PostMapping, etc.
- **Exception Handling**: Can use @ExceptionHandler for API error responses

This annotation eliminates the need to annotate every method with @ResponseBody, making REST API development more concise.

**Use**: RESTful web services.
```java
@RestController
public class ApiController {
    @GetMapping("/users")
    public List<User> getUsers() { return Arrays.asList(); }
}
```

### 7. @Autowired
**Theory**: Spring's primary annotation for dependency injection, implementing the Inversion of Control principle:
- **By Type Matching**: Searches for beans in the application context that match the field/parameter type
- **Automatic Resolution**: Spring automatically injects the matching bean without manual wiring
- **Multiple Locations**: Can be used on fields, constructors, setter methods, or any method
- **Required by Default**: Injection fails if no matching bean is found (unless required=false)
- **Collection Injection**: Can inject all beans of a type into a List or Map

This eliminates the need for manual object creation and promotes loose coupling between components. Spring handles the entire object graph construction.

**Use**: Inject dependencies automatically.
```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
}
```

### 8. @Qualifier
**Theory**: Resolves ambiguity when multiple beans of the same type exist in the application context:
- **Bean Selection**: Specifies which exact bean to inject by name or qualifier value
- **Disambiguation**: Prevents NoUniqueBeanDefinitionException when multiple candidates exist
- **Custom Qualifiers**: Can create custom qualifier annotations for more semantic bean selection
- **Collection Filtering**: When injecting collections, can filter beans by qualifier
- **Primary Alternative**: Alternative to @Primary for more explicit bean selection

Without @Qualifier, Spring would throw an exception if it finds multiple beans of the same type and doesn't know which one to inject.

**Use**: Specify which bean to inject.
```java
@Autowired
@Qualifier("emailNotification")
private NotificationService notificationService;
```

### 9. @Primary
**Theory**: Designates a bean as the primary candidate for injection when multiple beans of the same type exist:
- **Default Selection**: When multiple beans match, Spring chooses the @Primary one by default
- **Ambiguity Resolution**: Prevents NoUniqueBeanDefinitionException without requiring @Qualifier
- **Override Mechanism**: Can be overridden by explicit @Qualifier usage
- **Single Primary**: Only one bean of a type should be marked as @Primary
- **Fallback Strategy**: Provides a sensible default while allowing specific overrides

This is useful for providing default implementations while allowing specialized beans for specific use cases.

**Use**: Default bean when multiple exist.
```java
@Service
@Primary
public class DefaultPaymentService implements PaymentService {}
```

### 10. @Configuration
**Theory**: Marks a class as a source of bean definitions for the Spring IoC container, replacing XML configuration:
- **Bean Factory**: Methods annotated with @Bean become bean definitions
- **Proxy Creation**: Spring creates CGLIB proxies to ensure singleton behavior and handle inter-bean dependencies
- **Lifecycle Management**: Manages the complete lifecycle of beans defined within
- **Conditional Configuration**: Can be combined with @Conditional annotations for environment-specific configs
- **Import Support**: Can import other configuration classes using @Import
- **Property Integration**: Works with @PropertySource and @Value for externalized configuration

This enables type-safe, refactor-friendly configuration compared to XML, with full IDE support and compile-time checking.

**Use**: Java-based configuration.
```java
@Configuration
public class AppConfig {
    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }
}
```

---

## Web Layer Annotations

### 11. @RequestMapping
**Theory**: The foundational annotation for mapping HTTP requests to controller methods in Spring MVC:
- **URL Mapping**: Maps specific URLs or URL patterns to handler methods
- **HTTP Method Support**: Can specify which HTTP methods (GET, POST, PUT, DELETE) the method handles
- **Parameter Matching**: Can match based on request parameters, headers, and content types
- **Path Variables**: Supports dynamic URL segments using {variable} syntax
- **Content Negotiation**: Can produce different response types based on Accept header
- **Inheritance**: Class-level mappings are inherited by method-level mappings

This is the base annotation that all other mapping annotations (@GetMapping, @PostMapping, etc.) extend. It provides the most flexibility but requires more verbose configuration.

**Use**: Define request mappings.
```java
@RequestMapping(value = "/users", method = RequestMethod.GET)
public List<User> getUsers() { return userService.findAll(); }
```

### 12. @GetMapping
**Theory**: A composed annotation that simplifies GET request mapping:
- **HTTP GET Only**: Specifically handles GET requests, making intent clear
- **Idempotent Operations**: GET requests should be safe and idempotent (no side effects)
- **Caching Friendly**: GET requests can be cached by browsers and proxies
- **URL Parameters**: Commonly used with @RequestParam for query parameters
- **Path Variables**: Often combined with @PathVariable for RESTful URLs
- **Read Operations**: Typically used for data retrieval operations

GET requests should never modify server state and should be safe to call multiple times with the same result.

**Use**: Handle GET requests.
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { return userService.findById(id); }
```

### 13. @PostMapping
**Theory**: Specialized annotation for handling POST requests, typically for data creation:
- **Data Creation**: Primary method for creating new resources in REST APIs
- **Request Body**: Usually accepts data in request body (JSON, XML, form data)
- **Non-Idempotent**: POST requests can have side effects and change server state
- **Security Considerations**: Should include CSRF protection in web applications
- **Status Codes**: Typically returns 201 (Created) for successful resource creation
- **Location Header**: Should include location of newly created resource

POST is the most flexible HTTP method and can be used for any operation that doesn't fit other HTTP methods.

**Use**: Handle POST requests.
```java
@PostMapping("/users")
public User createUser(@RequestBody User user) { return userService.save(user); }
```

### 14. @PutMapping
**Theory**: Handles PUT requests for complete resource replacement or creation:
- **Idempotent**: Multiple identical PUT requests should have the same effect
- **Complete Replacement**: Should replace the entire resource, not partial updates
- **Create or Update**: Can create resource if it doesn't exist, or update if it does
- **Request Body Required**: Typically requires complete resource representation in request body
- **Status Codes**: Returns 200 (OK) for updates, 201 (Created) for new resources
- **Overwrite Semantics**: Overwrites existing resource completely

PUT is different from PATCH - PUT replaces the entire resource while PATCH applies partial modifications.

**Use**: Handle PUT requests.
```java
@PutMapping("/users/{id}")
public User updateUser(@PathVariable Long id, @RequestBody User user) {
    return userService.update(id, user);
}
```

### 15. @DeleteMapping
**Theory**: Handles DELETE requests for resource removal:
- **Idempotent**: Multiple DELETE requests should have the same effect (resource remains deleted)
- **Resource Removal**: Primary purpose is to delete/remove resources
- **Status Codes**: Returns 204 (No Content) for successful deletion, 404 if resource doesn't exist
- **Soft vs Hard Delete**: Implementation can choose between permanent removal or marking as deleted
- **Cascade Considerations**: May need to handle related resource cleanup
- **Authorization**: Often requires special permissions due to destructive nature

DELETE operations should be carefully designed with proper authorization and potentially confirmation mechanisms.

**Use**: Handle DELETE requests.
```java
@DeleteMapping("/users/{id}")
public void deleteUser(@PathVariable Long id) { userService.delete(id); }
```

### 16. @PatchMapping
**Theory**: Handles PATCH requests for partial resource modifications:
- **Partial Updates**: Modifies only specified fields of a resource, not the entire resource
- **Non-Idempotent**: PATCH operations may not be idempotent depending on implementation
- **Flexible Format**: Can accept various formats (JSON Patch, JSON Merge Patch, custom formats)
- **Efficiency**: More efficient than PUT for small changes as it sends only modified fields
- **Validation**: Requires careful validation to ensure partial updates maintain data integrity
- **Atomic Operations**: Should ensure all changes in a PATCH are applied atomically

PATCH is ideal for scenarios like updating user profiles where only a few fields change.

**Use**: Handle PATCH requests.
```java
@PatchMapping("/users/{id}")
public User patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
    return userService.patch(id, updates);
}
```

### 17. @PathVariable
**Theory**: Extracts values from URI path segments defined as variables in @RequestMapping:
- **URI Templates**: Works with URI templates like /users/{id} where {id} is a path variable
- **Type Conversion**: Automatically converts path values to method parameter types
- **Required by Default**: Path variables are required unless specified otherwise
- **Multiple Variables**: Can extract multiple path variables in a single method
- **Regular Expressions**: Supports regex patterns for path variable validation
- **RESTful Design**: Essential for RESTful API design following resource-based URLs

Path variables make URLs more readable and SEO-friendly compared to query parameters for resource identification.

**Use**: Capture path parameters.
```java
@GetMapping("/users/{id}/orders/{orderId}")
public Order getOrder(@PathVariable Long id, @PathVariable Long orderId) {
    return orderService.findByUserAndId(id, orderId);
}
```

### 18. @RequestParam
**Theory**: Extracts query parameters from the request URL (the part after '?' in URLs):
- **Query String Parsing**: Parses key=value pairs from URL query string
- **Type Conversion**: Automatically converts string values to target parameter types
- **Optional Parameters**: Can specify default values and make parameters optional
- **Multiple Values**: Supports multiple values for the same parameter name (arrays/lists)
- **Validation**: Can be combined with validation annotations
- **Form Data**: Also works with form-encoded POST data

Query parameters are ideal for optional filters, pagination, sorting, and search criteria in GET requests.

**Use**: Handle query strings.
```java
@GetMapping("/users")
public List<User> getUsers(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size) {
    return userService.findAll(page, size);
}
```

### 19. @RequestBody
**Theory**: Deserializes HTTP request body content into Java objects:
- **Content Deserialization**: Converts JSON, XML, or other formats into Java objects using HttpMessageConverters
- **Automatic Binding**: Spring automatically maps request body fields to object properties
- **Content-Type Aware**: Uses Content-Type header to determine appropriate converter
- **Validation Integration**: Can be combined with @Valid for automatic validation
- **Single Use**: Only one @RequestBody parameter allowed per method
- **Stream Processing**: Provides access to raw request body as InputStream if needed

This is essential for REST APIs that accept complex data structures in request bodies.

**Use**: Handle JSON/XML payloads.
```java
@PostMapping("/users")
public User createUser(@RequestBody @Valid User user) {
    return userService.save(user);
}
```

### 20. @ResponseBody
**Theory**: Serializes method return values directly to HTTP response body:
- **Bypass View Resolution**: Skips Spring's view resolution mechanism entirely
- **Content Serialization**: Converts Java objects to JSON, XML, or other formats using HttpMessageConverters
- **Content Negotiation**: Chooses appropriate format based on Accept header and available converters
- **Direct Response**: Writes serialized content directly to HTTP response
- **Status Code Control**: Can be combined with ResponseEntity for status code control
- **REST API Essential**: Fundamental for REST API endpoints that return data

@RestController automatically applies @ResponseBody to all methods, eliminating the need for explicit annotation.

**Use**: Return JSON/XML responses.
```java
@RequestMapping("/api/users")
@ResponseBody
public List<User> getUsers() { return userService.findAll(); }
```

### 21. @RequestHeader
**Theory**: Extracts values from HTTP request headers for processing in controller methods:
- **Header Access**: Provides access to any HTTP header sent by the client
- **Type Conversion**: Automatically converts header string values to target parameter types
- **Optional Headers**: Can specify default values for missing headers
- **Case Insensitive**: HTTP headers are case-insensitive, Spring handles this automatically
- **Multiple Values**: Some headers can have multiple values (like Accept)
- **Security Headers**: Commonly used for authentication tokens, API keys, or custom security headers

Headers are useful for metadata that doesn't belong in the URL or request body, such as authentication, content preferences, or client information.

**Use**: Access request headers.
```java
@GetMapping("/users")
public List<User> getUsers(@RequestHeader("Authorization") String token) {
    return userService.findAllForUser(token);
}
```

### 22. @CookieValue
**Theory**: Extracts cookie values from HTTP requests for server-side processing:
- **Cookie Parsing**: Automatically parses cookies from Cookie header
- **Type Conversion**: Converts cookie string values to appropriate Java types
- **Optional Cookies**: Can specify default values for missing cookies
- **Session Management**: Commonly used for session IDs, user preferences, or tracking
- **Security Considerations**: Should validate cookie values for security
- **Browser Compatibility**: Works with all standard HTTP cookies

Cookies provide a way to maintain state between HTTP requests and are essential for session management and user preferences.

**Use**: Access HTTP cookies.
```java
@GetMapping("/profile")
public User getProfile(@CookieValue("sessionId") String sessionId) {
    return userService.findBySession(sessionId);
}
```

### 23. @ModelAttribute
**Theory**: Binds request parameters to a model object and adds it to the Spring MVC model:
- **Data Binding**: Automatically maps request parameters to object properties by name
- **Type Conversion**: Handles type conversion from strings to object property types
- **Validation Support**: Can be combined with validation annotations for form validation
- **Model Population**: Automatically adds the bound object to the model for view access
- **Form Handling**: Primary mechanism for handling HTML form submissions
- **Nested Properties**: Supports binding to nested object properties using dot notation

This is essential for traditional MVC applications where forms submit data that needs to be bound to domain objects.

**Use**: Form data binding.
```java
@PostMapping("/users")
public String createUser(@ModelAttribute User user, Model model) {
    userService.save(user);
    return "redirect:/users";
}
```

### 24. @SessionAttribute
**Theory**: Retrieves objects stored in the HTTP session for use in controller methods:
- **Session Access**: Provides direct access to objects stored in HttpSession
- **Type Safety**: Ensures type safety when retrieving session objects
- **Required by Default**: Throws exception if session attribute doesn't exist (unless required=false)
- **Stateful Web Apps**: Essential for maintaining user state across requests
- **Authentication**: Commonly used for storing user authentication information
- **Shopping Carts**: Typical use case for e-commerce applications

Session attributes persist across multiple requests from the same user until the session expires or is invalidated.

**Use**: Retrieve session data.
```java
@GetMapping("/dashboard")
public String dashboard(@SessionAttribute("user") User user, Model model) {
    model.addAttribute("user", user);
    return "dashboard";
}
```

### 25. @CrossOrigin
**Theory**: Configures Cross-Origin Resource Sharing (CORS) to allow web pages from different domains to access your API:
- **Same-Origin Policy**: Browsers block requests from different origins (protocol, domain, port) by default
- **CORS Headers**: Automatically adds appropriate CORS headers to responses
- **Origin Control**: Specifies which origins are allowed to access the resource
- **Method Control**: Can restrict which HTTP methods are allowed for cross-origin requests
- **Credential Support**: Can allow or deny credentials (cookies, authorization headers) in cross-origin requests
- **Preflight Handling**: Handles CORS preflight requests automatically

Essential for modern web applications where frontend and backend are served from different domains or ports.

**Use**: Handle cross-origin requests.
```java
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ApiController {
    @GetMapping("/data")
    public Data getData() { return new Data(); }
}
```

---

## Data Access Annotations

### 26. @Entity
**Theory**: Marks a Java class as a JPA (Java Persistence API) entity that maps to a database table:
- **ORM Mapping**: Establishes the connection between Java objects and database tables
- **Persistence Context**: Makes instances manageable by JPA EntityManager
- **Lifecycle Management**: JPA manages entity lifecycle (transient, persistent, detached, removed)
- **Automatic Table Creation**: Can automatically create corresponding database tables
- **Identity Management**: Requires an @Id field for primary key mapping
- **Relationship Support**: Enables relationships with other entities (@OneToMany, @ManyToOne, etc.)
- **Query Integration**: Allows entities to be used in JPQL and Criteria API queries

Entities are the core building blocks of JPA applications, representing business objects that need to be persisted.

**Use**: Database table mapping.
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
```

### 27. @Table
**Theory**: Provides detailed configuration for the database table that an entity maps to:
- **Table Name**: Specifies exact table name if different from entity class name
- **Schema/Catalog**: Defines database schema or catalog for the table
- **Unique Constraints**: Defines unique constraints across multiple columns
- **Indexes**: Specifies database indexes for performance optimization
- **Naming Strategy**: Overrides default naming conventions
- **Database Portability**: Helps maintain consistency across different database systems

Without @Table, JPA uses default naming conventions (usually class name converted to table naming convention).

**Use**: Custom table mapping.
```java
@Entity
@Table(name = "user_profiles", schema = "public")
public class UserProfile {
    @Id private Long id;
}
```

### 28. @Id
**Theory**: Designates a field or property as the primary key of an entity:
- **Unique Identification**: Ensures each entity instance can be uniquely identified
- **Database Constraint**: Maps to primary key constraint in database table
- **Required Field**: Every entity must have exactly one @Id field (or @EmbeddedId)
- **Equality/HashCode**: Used in equals() and hashCode() implementations
- **Caching Key**: JPA uses ID for first-level cache (persistence context) management
- **Relationship References**: Other entities reference this entity using this ID
- **Query Performance**: Primary keys are automatically indexed for fast lookups

The ID field is fundamental to JPA's identity management and object-relational mapping.

**Use**: Entity identifier.
```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
```

### 29. @GeneratedValue
**Theory**: Configures how primary key values are automatically generated by the persistence provider:
- **Generation Strategies**: AUTO (provider chooses), IDENTITY (auto-increment), SEQUENCE (database sequences), TABLE (separate table)
- **Database Independence**: AUTO strategy provides database portability
- **Performance Considerations**: Different strategies have different performance characteristics
- **Batch Operations**: Some strategies work better with batch inserts
- **Custom Generators**: Can specify custom generator implementations
- **Sequence Configuration**: Works with @SequenceGenerator for detailed sequence control

Proper ID generation strategy is crucial for performance, especially in high-throughput applications.

**Use**: Auto-generated IDs.
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_sequence")
private Long id;
```

### 30. @Column
**Theory**: Provides detailed mapping configuration between entity fields and database columns:
- **Column Name**: Specifies exact database column name if different from field name
- **Data Type Control**: Influences SQL data type generation (length, precision, scale)
- **Constraints**: Defines nullable, unique constraints at column level
- **Default Values**: Can specify default values for columns
- **Update/Insert Control**: Controls whether column is included in UPDATE/INSERT statements
- **DDL Generation**: Affects automatic schema generation
- **Validation**: Works with Bean Validation for data integrity

Fine-grained column control is essential for mapping to existing databases or enforcing specific data constraints.

**Use**: Custom column properties.
```java
@Column(name = "email_address", nullable = false, unique = true, length = 255)
private String email;
```

### 31. @JoinColumn
**Theory**: Configures the foreign key column used in entity relationships:
- **Foreign Key Mapping**: Specifies which column holds the foreign key reference
- **Relationship Control**: Used with @OneToOne, @ManyToOne, @OneToMany, @ManyToMany
- **Column Naming**: Defines exact foreign key column name
- **Referential Integrity**: Maps to database foreign key constraints
- **Cascade Operations**: Works with cascade settings for related operations
- **Join Table Alternative**: Alternative to @JoinTable for simpler relationships
- **Bidirectional Relationships**: Coordinates with mappedBy for bidirectional associations

Proper foreign key configuration is essential for maintaining data integrity and relationship navigation.

**Use**: Relationship mapping.
```java
@ManyToOne
@JoinColumn(name = "department_id", referencedColumnName = "id")
private Department department;
```

### 32. @OneToMany
**Theory**: Maps a one-to-many relationship where one entity is associated with multiple instances of another entity:
- **Parent-Child Modeling**: Represents hierarchical relationships (User -> Orders, Department -> Employees)
- **Collection Mapping**: Maps to Java collections (List, Set, Map)
- **Cascade Operations**: Can cascade persist, merge, remove operations to child entities
- **Fetch Strategies**: LAZY (default) or EAGER loading of related entities
- **Orphan Removal**: Can automatically delete child entities when removed from collection
- **Bidirectional Support**: Can be bidirectional with @ManyToOne on the other side
- **Performance Impact**: Lazy loading prevents N+1 query problems

Careful configuration of fetch type and cascade operations is crucial for performance and data consistency.

**Use**: Parent-child relationships.
```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Order> orders = new ArrayList<>();
```

### 33. @ManyToOne
**Theory**: Maps a many-to-one relationship where multiple entities reference a single entity:
- **Foreign Key Relationship**: Typically results in a foreign key column in the database
- **Reference Mapping**: Maps object references between entities
- **Fetch Strategies**: EAGER (default) or LAZY loading of referenced entity
- **Cascade Operations**: Can cascade operations to the referenced entity
- **Null Handling**: Can be optional (nullable) or required (non-null)
- **Proxy Creation**: JPA may create proxies for lazy loading
- **Query Joins**: Enables join queries across related entities

This is the most common relationship type and forms the foundation of relational data modeling in JPA.

**Use**: Child-parent relationships.
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

### 34. @ManyToMany
**Theory**: Maps many-to-many relationships where entities on both sides can be associated with multiple entities on the other side:
- **Join Table**: Requires an intermediate join table to store the relationships
- **Bidirectional Support**: Can be bidirectional with one side as the owner
- **Collection Types**: Maps to Set, List, or Map collections
- **Cascade Considerations**: Cascade operations affect all related entities
- **Performance Impact**: Can be expensive due to join table queries
- **Alternative Approaches**: Sometimes better modeled as two @OneToMany relationships with an intermediate entity
- **Fetch Strategies**: Usually LAZY to avoid loading large collections

Many-to-many relationships should be used judiciously due to their complexity and performance implications.

**Use**: Complex relationships.
```java
@ManyToMany
@JoinTable(name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id"))
private Set<Role> roles = new HashSet<>();
```

### 35. @OneToOne
**Theory**: Maps one-to-one relationships where each entity instance is associated with exactly one instance of another entity:
- **Unique Association**: Each entity can be related to at most one instance of the other entity
- **Shared Primary Key**: Can share primary keys or use foreign key approach
- **Optional Relationships**: Can be optional on one or both sides
- **Lazy Loading**: Supports lazy loading, though it can be tricky to implement efficiently
- **Cascade Operations**: Often uses cascade ALL for tightly coupled entities
- **Database Design**: Can be implemented as separate tables or table splitting
- **Performance**: Generally efficient but lazy loading can cause proxy issues

One-to-one relationships are ideal for splitting large entities or representing optional detailed information.

**Use**: Unique relationships.
```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "profile_id", referencedColumnName = "id")
private UserProfile profile;
```

### 36. @Transactional
**Theory**: Enables declarative transaction management, automatically handling transaction boundaries:
- **ACID Properties**: Ensures Atomicity, Consistency, Isolation, and Durability of database operations
- **Automatic Rollback**: Rolls back transactions on unchecked exceptions by default
- **Propagation Control**: Defines how transactions behave when called from other transactional methods
- **Isolation Levels**: Controls concurrent access to data (READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE)
- **Read-Only Optimization**: Read-only transactions can be optimized by the database
- **Timeout Management**: Can specify transaction timeout to prevent long-running transactions
- **Proxy-Based**: Uses AOP proxies, so self-invocation doesn't trigger transaction management

**Transaction Propagation Types**:
- **REQUIRED** (default): Join existing transaction or create new one if none exists
- **REQUIRES_NEW**: Always create a new transaction, suspending current one if exists
- **SUPPORTS**: Join existing transaction if present, execute non-transactionally if none
- **NOT_SUPPORTED**: Execute non-transactionally, suspending current transaction if exists
- **MANDATORY**: Must execute within existing transaction, throw exception if none exists
- **NEVER**: Must execute non-transactionally, throw exception if transaction exists
- **NESTED**: Execute within nested transaction if supported, behaves like REQUIRED otherwise

**Isolation Levels**:
- **DEFAULT**: Use database default isolation level
- **READ_UNCOMMITTED**: Allows dirty reads, non-repeatable reads, phantom reads
- **READ_COMMITTED**: Prevents dirty reads, allows non-repeatable reads and phantom reads
- **REPEATABLE_READ**: Prevents dirty and non-repeatable reads, allows phantom reads
- **SERIALIZABLE**: Prevents all concurrency issues, highest isolation level

Transactions are fundamental for data consistency and are essential in multi-user applications.

**Use**: Method-level transactions.
```java
@Service
@Transactional
public class OrderService {
    @Transactional(readOnly = true)
    public Order findById(Long id) { return orderRepository.findById(id); }
    
    @Transactional(rollbackFor = Exception.class)
    public Order save(Order order) { return orderRepository.save(order); }
}
```

### 37. @Query
**Theory**: Allows definition of custom JPQL (Java Persistence Query Language) or native SQL queries in repository methods:
- **JPQL Support**: Object-oriented query language that works with entities rather than tables
- **Native SQL**: Can execute raw SQL for database-specific features or complex queries
- **Parameter Binding**: Supports both positional (?1, ?2) and named (:name) parameters
- **Dynamic Queries**: Can build queries programmatically using SpEL expressions
- **Result Mapping**: Automatically maps query results to entity objects or DTOs
- **Performance**: Custom queries can be optimized for specific use cases
- **Database Independence**: JPQL queries are portable across different databases

Custom queries are essential when Spring Data's derived queries are insufficient for complex business requirements.

**Use**: Complex database queries.
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmail(String email);
    
    @Query(value = "SELECT * FROM users WHERE created_at > ?1", nativeQuery = true)
    List<User> findRecentUsers(LocalDateTime date);
}
```

### 38. @Modifying
**Theory**: Indicates that a @Query method performs data modification (INSERT, UPDATE, DELETE) rather than selection:
- **DML Operations**: Enables Data Manipulation Language operations in repository methods
- **Bulk Operations**: Performs bulk updates/deletes that bypass the persistence context
- **Performance**: More efficient than loading entities and modifying them individually
- **Cache Invalidation**: May require clearing the persistence context after execution
- **Transaction Requirement**: Must be executed within a transaction
- **Return Types**: Can return int/Integer (affected rows) or void
- **Batch Processing**: Ideal for processing large datasets efficiently

Modifying queries are crucial for bulk operations and performance-critical data modifications.

**Use**: Data modification queries.
```java
@Modifying
@Query("UPDATE User u SET u.lastLogin = ?1 WHERE u.id = ?2")
int updateLastLogin(LocalDateTime lastLogin, Long userId);
```

### 39. @Param
**Theory**: Binds method parameters to named parameters in JPQL or native SQL queries:
- **Named Parameters**: Provides readable parameter names instead of positional numbers
- **Type Safety**: Ensures compile-time checking of parameter names
- **Query Readability**: Makes queries more self-documenting and maintainable
- **Parameter Reuse**: Same parameter can be used multiple times in a query
- **IDE Support**: IDEs can provide better autocomplete and refactoring support
- **Debugging**: Easier to debug queries with meaningful parameter names
- **SpEL Integration**: Can be used with Spring Expression Language for dynamic queries

Named parameters significantly improve query maintainability and reduce errors in complex queries.

**Use**: Named parameters in queries.
```java
@Query("SELECT u FROM User u WHERE u.name = :name AND u.age > :age")
List<User> findByNameAndAgeGreaterThan(@Param("name") String name, @Param("age") int age);
```

### 40. @EnableJpaRepositories
**Theory**: Activates Spring Data JPA repository infrastructure and configures repository scanning:
- **Repository Scanning**: Automatically discovers and creates implementations for repository interfaces
- **Base Package Configuration**: Specifies which packages to scan for repository interfaces
- **Custom Implementation**: Supports custom repository implementations alongside generated ones
- **EntityManager Configuration**: Configures which EntityManagerFactory to use
- **Transaction Manager**: Specifies which transaction manager to use for repositories
- **Repository Factory**: Can customize the repository factory bean for advanced configurations
- **Bootstrapping**: Essential for Spring Data JPA to function properly

This annotation is typically used in configuration classes to bootstrap the entire Spring Data JPA infrastructure.

**Use**: Repository configuration.
```java
@Configuration
@EnableJpaRepositories(basePackages = "com.example.repository")
public class JpaConfig {}
```

---

## Security Annotations

### 41. @EnableWebSecurity
**Theory**: Activates Spring Security's web security features and enables security configuration:
- **Security Filter Chain**: Sets up the Spring Security filter chain for web requests
- **Authentication**: Enables various authentication mechanisms (form login, HTTP basic, OAuth, etc.)
- **Authorization**: Provides URL-based and method-level authorization
- **CSRF Protection**: Enables Cross-Site Request Forgery protection by default
- **Session Management**: Configures session handling, session fixation protection, and concurrent sessions
- **Security Headers**: Automatically adds security headers (X-Frame-Options, X-Content-Type-Options, etc.)
- **Custom Configuration**: Allows customization of security behavior through SecurityConfigurer

This annotation is essential for any application that needs authentication and authorization.

**Use**: Security configuration.
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).build();
    }
}
```

### 42. @PreAuthorize
**Theory**: Provides method-level security by evaluating Spring Expression Language (SpEL) expressions before method execution:
- **Expression-Based**: Uses powerful SpEL expressions for complex authorization logic
- **Context Access**: Can access method parameters, authentication object, and security context
- **Role-Based**: Supports role-based access control with hasRole() and hasAuthority()
- **Custom Logic**: Allows custom authorization logic using bean references and method calls
- **Parameter Validation**: Can validate method parameters as part of authorization
- **Flexible Conditions**: Supports complex boolean expressions with AND, OR, NOT operators
- **AOP Integration**: Uses Spring AOP to intercept method calls

This provides fine-grained, flexible authorization that can adapt to complex business rules.

**Use**: Authorization before method execution.
```java
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public User getUserById(@PathVariable Long userId) {
    return userService.findById(userId);
}
```

### 43. @PostAuthorize
**Theory**: Evaluates authorization expressions after method execution, with access to the return value:
- **Return Value Access**: Can make authorization decisions based on the method's return value
- **Data Filtering**: Can filter or modify returned data based on user permissions
- **Conditional Access**: Allows access to methods but controls what data is returned
- **Performance Consideration**: Method executes before authorization check, so use carefully
- **Exception Handling**: Throws AccessDeniedException if authorization fails after execution
- **Complex Scenarios**: Useful when authorization depends on data retrieved during method execution
- **Security Context**: Still has access to authentication and security context

This is particularly useful for scenarios where you need to check ownership or permissions on returned data.

**Use**: Authorization on return value.
```java
@PostAuthorize("returnObject.owner == authentication.name")
public Document getDocument(Long id) {
    return documentService.findById(id);
}
```

### 44. @Secured
**Theory**: Provides simple role-based method-level security using a list of required roles:
- **Role-Based Only**: Limited to simple role checking, no complex expressions
- **Multiple Roles**: Can specify multiple roles, user needs ANY of the specified roles
- **String-Based**: Uses string literals for role names (typically prefixed with ROLE_)
- **JSR-250 Alternative**: Simpler alternative to @RolesAllowed with Spring Security integration
- **No Parameters**: Cannot access method parameters or return values
- **Performance**: Slightly faster than @PreAuthorize due to simpler evaluation
- **Legacy Support**: Older annotation, @PreAuthorize is generally preferred for new development

Best used for simple role-based access control where complex logic isn't needed.

**Use**: Simple role checking.
```java
@Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
public void deleteUser(Long userId) {
    userService.delete(userId);
}
```

### 45. @RolesAllowed
**Theory**: JSR-250 standard annotation for role-based security, providing portable security across different frameworks:
- **Standard Compliance**: Part of JSR-250 specification, not Spring-specific
- **Framework Portability**: Can be used with different security frameworks, not just Spring Security
- **Role-Based Access**: Simple role-based access control using role names
- **Multiple Roles**: User must have ANY of the specified roles to access the method
- **No Expressions**: Cannot use SpEL expressions, limited to role checking
- **Container Integration**: Originally designed for Java EE container security
- **Spring Integration**: Spring Security provides support for JSR-250 annotations

Useful when you need standard-compliant security annotations or plan to use multiple security frameworks.

**Use**: Standard role authorization.
```java
@RolesAllowed("ADMIN")
public void performAdminTask() {
    // Admin only operation
}
```

### 46. @EnableGlobalMethodSecurity
**Theory**: Activates method-level security annotations globally across the application:
- **Annotation Support**: Enables @PreAuthorize, @PostAuthorize, @Secured, @RolesAllowed annotations
- **AOP Integration**: Sets up AOP infrastructure for method security interception
- **Global Configuration**: Applies to all beans in the application context
- **Expression Handler**: Configures SpEL expression evaluation for security expressions
- **Custom Voters**: Allows custom AccessDecisionVoter implementations
- **Performance Impact**: Adds AOP overhead to secured methods
- **Proxy Requirements**: Requires proper proxy configuration for self-invocation scenarios

This is a prerequisite for using any method-level security annotations in Spring Security.

**Use**: Global security configuration.
```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityConfig {}
```

### 47. @AuthenticationPrincipal
**Theory**: Injects the current authenticated user's principal object directly into controller method parameters:
- **Direct Access**: Provides direct access to the authenticated user without manual SecurityContext lookup
- **Type Safety**: Automatically casts to the expected principal type (UserDetails, custom user objects)
- **Null Safety**: Can handle cases where no user is authenticated
- **Custom Principals**: Works with custom principal objects, not just UserDetails
- **Expression Support**: Can use SpEL expressions to extract specific properties from the principal
- **Convenience**: Eliminates boilerplate code for accessing current user information
- **Integration**: Works seamlessly with Spring Security's authentication mechanisms

This greatly simplifies accessing current user information in web controllers and REST endpoints.

**Use**: Access current user in controllers.
```java
@GetMapping("/profile")
public UserProfile getProfile(@AuthenticationPrincipal UserDetails user) {
    return profileService.findByUsername(user.getUsername());
}
```

---

## Testing Annotations

### 48. @SpringBootTest
**Theory**: Creates a complete Spring application context for integration testing, simulating the full application startup:
- **Full Context Loading**: Loads the entire Spring application context with all beans and configurations
- **Auto-Configuration**: Applies Spring Boot's auto-configuration just like in production
- **Web Environment**: Can start an embedded web server for testing web layers
- **Test Slicing Alternative**: More comprehensive than test slice annotations but slower
- **Property Override**: Allows overriding application properties for testing
- **Profile Activation**: Can activate specific profiles for test scenarios
- **Bean Override**: Supports @MockBean and @SpyBean for replacing real beans with mocks
- **Lifecycle Management**: Manages complete application lifecycle during tests

Ideal for end-to-end integration tests that need to verify the entire application stack.

**Use**: Integration testing.
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
}
```

### 49. @WebMvcTest
**Theory**: Creates a test slice that focuses only on Spring MVC components, providing fast and focused web layer testing:
- **Limited Context**: Loads only web layer beans (@Controller, @RestController, @ControllerAdvice, etc.)
- **MockMvc Integration**: Automatically configures MockMvc for testing HTTP requests and responses
- **Fast Execution**: Much faster than @SpringBootTest due to limited context loading
- **Auto-Configuration**: Includes relevant auto-configuration for web testing
- **Security Integration**: Can include Spring Security configuration for authentication testing
- **JSON Testing**: Provides JSON assertion capabilities for REST API testing
- **Mock Dependencies**: Requires @MockBean for service layer dependencies
- **Validation Testing**: Can test request validation and error handling

Perfect for unit testing controllers in isolation from the rest of the application.

**Use**: Controller testing.
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
}
```

### 50. @DataJpaTest
**Theory**: Creates a test slice focused on JPA repositories with an embedded, in-memory database:
- **JPA Focus**: Loads only JPA-related components (@Repository, @Entity, EntityManager)
- **In-Memory Database**: Automatically configures H2, Derby, or HSQL for testing
- **TestEntityManager**: Provides TestEntityManager for direct entity manipulation in tests
- **Transaction Rollback**: Automatically rolls back transactions after each test
- **SQL Logging**: Can enable SQL logging to verify generated queries
- **Fast Execution**: Very fast due to in-memory database and limited context
- **Data Initialization**: Supports @Sql for test data setup
- **Validation Testing**: Tests entity validation and constraint violations

Ideal for testing repository methods, custom queries, and entity mappings in isolation.

**Use**: Repository testing.
```java
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
}
```

### 51. @MockBean
**Theory**: Creates Mockito mocks and adds them to the Spring application context, replacing real beans:
- **Context Integration**: Mocks are registered as beans in the Spring context
- **Dependency Injection**: Mocked beans are automatically injected where the real beans would be
- **Lifecycle Management**: Spring manages the mock lifecycle along with other beans
- **Test Isolation**: Each test class gets fresh mocks, preventing test interference
- **Stubbing Support**: Full Mockito stubbing capabilities (when(), thenReturn(), etc.)
- **Verification**: Can verify interactions with mocked dependencies
- **Performance**: Eliminates need for real implementations, making tests faster
- **External Dependencies**: Perfect for mocking external services, databases, or complex components

Essential for isolating units under test from their dependencies in Spring integration tests.

**Use**: Mock dependencies in tests.
```java
@SpringBootTest
class OrderServiceTest {
    @MockBean
    private PaymentService paymentService;
    
    @Autowired
    private OrderService orderService;
}
```

### 52. @SpyBean
**Theory**: Wraps existing Spring beans with Mockito spies, allowing partial mocking of real objects:
- **Real Object Wrapping**: Creates a spy around the actual bean instance, not a mock
- **Selective Stubbing**: Can stub specific methods while keeping others unchanged
- **Real Method Execution**: Unstubbed methods execute the real implementation
- **Verification Capability**: Can verify method calls on the real bean
- **State Preservation**: Maintains the real object's state and behavior
- **Integration Testing**: Useful when you need mostly real behavior with some controlled responses
- **Performance Monitoring**: Can verify performance-related method calls
- **Debugging Aid**: Helps understand how components interact in integration tests

Ideal when you need to verify interactions with real beans or partially control their behavior.

**Use**: Partial mocking of real beans.
```java
@SpringBootTest
class NotificationServiceTest {
    @SpyBean
    private EmailService emailService;
}
```

### 53. @TestConfiguration
**Theory**: Defines configuration classes specifically for testing, separate from production configuration:
- **Test-Only Beans**: Creates beans that exist only in test contexts
- **Configuration Override**: Can override or supplement production configuration
- **Bean Replacement**: Provides alternative implementations for testing
- **Test Utilities**: Can define test-specific utilities and helpers
- **Environment Simulation**: Helps simulate different environments or conditions
- **Isolation**: Keeps test configuration separate from production code
- **Reusability**: Can be reused across multiple test classes
- **Conditional Loading**: Only loaded when explicitly imported or component-scanned

Useful for creating test-specific beans, overriding configurations, or setting up test environments.

**Use**: Test configuration classes.
```java
@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
}
```

### 54. @ActiveProfiles
**Theory**: Activates specific Spring profiles during test execution, enabling profile-specific configuration:
- **Profile Activation**: Activates one or more profiles for the duration of the test
- **Environment Simulation**: Simulates different deployment environments (dev, test, prod)
- **Configuration Selection**: Enables profile-specific beans and properties
- **Test Isolation**: Different test classes can use different profiles
- **Multiple Profiles**: Can activate multiple profiles simultaneously
- **Override Capability**: Can override default active profiles
- **Integration Testing**: Essential for testing profile-specific behavior
- **Configuration Validation**: Helps validate that profile-specific configurations work correctly

Crucial for testing applications that behave differently in different environments.

**Use**: Profile-specific testing.
```java
@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrationTest {}
```

### 55. @DirtiesContext
**Theory**: Marks the Spring application context as 'dirty', forcing it to be closed and recreated:
- **Context Cleanup**: Forces Spring to create a fresh context for subsequent tests
- **State Isolation**: Prevents test interference when tests modify shared state
- **Cache Clearing**: Clears any caches or singletons that might affect other tests
- **Performance Trade-off**: Slower test execution due to context recreation
- **Granular Control**: Can be applied at method or class level with different modes
- **Shared State Problems**: Solves issues with tests that modify global state
- **Integration Testing**: Particularly useful in integration tests with stateful components
- **Last Resort**: Should be used sparingly as it impacts test performance

Use when tests modify application state in ways that could affect other tests.

**Use**: Context isolation between tests.
```java
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CacheTest {}
```

### 56. @Sql
**Theory**: Executes SQL scripts before or after test methods to set up or clean up database state:
- **Script Execution**: Runs SQL scripts from classpath or file system
- **Timing Control**: Can execute before or after test methods
- **Multiple Scripts**: Supports multiple scripts in a specific order
- **Transaction Control**: Scripts can run in separate transactions or within test transactions
- **Error Handling**: Can configure how SQL errors are handled
- **Data Setup**: Perfect for creating test data that's too complex for @DataJpaTest
- **Database State**: Ensures consistent database state for each test
- **Cleanup**: Can clean up test data after test execution

Essential for integration tests that require specific database state or complex test data.

**Use**: Database setup for tests.
```java
@Test
@Sql("/test-data.sql")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void testWithData() {}
```

### 57. @Rollback
**Theory**: Controls whether transactions in test methods are rolled back or committed:
- **Transaction Control**: Overrides the default rollback behavior in tests
- **Data Persistence**: When set to false, allows test data to persist after test completion
- **Default Behavior**: Tests normally rollback transactions to maintain isolation
- **Integration Testing**: Useful when you need to verify data persistence across transactions
- **Performance Impact**: Committed transactions are slower than rolled-back ones
- **Test Isolation**: Disabling rollback can cause test interference
- **Debugging Aid**: Helpful for debugging by leaving test data in the database
- **Cleanup Responsibility**: When rollback is disabled, tests must handle their own cleanup

Use carefully as it can break test isolation and cause tests to interfere with each other.

**Use**: Data persistence in tests.
```java
@Test
@Transactional
@Rollback(false)
void testDataPersistence() {}
```

---

## Configuration Annotations

### 58. @ConfigurationProperties
**Theory**: Binds external configuration properties to strongly-typed Java objects, providing type-safe configuration management:
- **Type Safety**: Converts string properties to appropriate Java types with validation
- **Hierarchical Binding**: Maps nested properties to nested objects automatically
- **Validation Support**: Integrates with Bean Validation for configuration validation
- **IDE Support**: Provides autocomplete and validation in IDE for configuration properties
- **Relaxed Binding**: Supports various naming conventions (camelCase, kebab-case, snake_case)
- **Collection Binding**: Can bind to Lists, Sets, Maps, and arrays
- **Conditional Properties**: Can be combined with @ConditionalOnProperty for conditional configuration
- **Documentation**: Generates metadata for configuration properties documentation

This is the preferred way to handle complex configuration in Spring Boot applications.

**Use**: Type-safe configuration.
```java
@ConfigurationProperties(prefix = "app.database")
@Component
public class DatabaseProperties {
    private String url;
    private String username;
    private String password;
    // getters and setters
}
```

### 59. @Value
**Theory**: Injects values from various sources (properties files, environment variables, system properties) into fields or method parameters:
- **Property Resolution**: Resolves values from Spring's Environment abstraction
- **Default Values**: Supports default values using colon syntax (${property:defaultValue})
- **SpEL Integration**: Can use Spring Expression Language for complex value resolution
- **Type Conversion**: Automatically converts string values to target field types
- **Multiple Sources**: Can resolve from application.properties, environment variables, system properties
- **Runtime Resolution**: Values are resolved at bean creation time
- **Placeholder Support**: Supports property placeholders and expression evaluation
- **Simple Use Cases**: Best for simple property injection, use @ConfigurationProperties for complex scenarios

Ideal for injecting simple configuration values directly into beans.

**Use**: Simple property injection.
```java
@Component
public class AppConfig {
    @Value("${app.name:MyApp}")
    private String appName;
    
    @Value("#{systemProperties['user.home']}")
    private String userHome;
}
```

### 60. @Profile
**Theory**: Conditionally registers beans based on active Spring profiles, enabling environment-specific configuration:
- **Environment Separation**: Allows different beans for different environments (dev, test, prod)
- **Conditional Registration**: Beans are only created when their profile is active
- **Multiple Profiles**: Can specify multiple profiles with AND/OR logic
- **Negation Support**: Can exclude beans when certain profiles are active using ! operator
- **Runtime Switching**: Profiles can be activated at runtime via properties or programmatically
- **Configuration Classes**: Can be applied to entire configuration classes
- **Method Level**: Can be applied to individual @Bean methods
- **Default Profile**: Spring has a 'default' profile for beans without explicit profiles

Essential for creating environment-specific configurations without code duplication.

**Use**: Environment-specific beans.
```java
@Configuration
@Profile("production")
public class ProductionConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
```

### 61. @Conditional
**Theory**: Provides fine-grained control over bean creation using custom condition classes that implement the Condition interface:
- **Custom Logic**: Allows complex conditional logic beyond simple profile or property checks
- **Runtime Evaluation**: Conditions are evaluated at application startup time
- **Context Access**: Condition classes have access to full application context and environment
- **Multiple Conditions**: Can combine multiple conditions with AND logic
- **Extensibility**: Foundation for Spring Boot's auto-configuration conditions
- **Performance**: Conditions are evaluated once during context creation
- **Debugging**: Can be complex to debug when conditions fail
- **Advanced Use Cases**: Used for sophisticated configuration scenarios

Provides the most flexibility for conditional bean creation but requires custom condition implementations.

**Use**: Complex conditional logic.
```java
@Component
@Conditional(WindowsCondition.class)
public class WindowsService {
    // Windows-specific implementation
}
```

### 62. @ConditionalOnProperty
**Theory**: Creates beans conditionally based on the presence and values of configuration properties:
- **Property Existence**: Can check if a property exists or has a specific value
- **Value Matching**: Supports exact value matching or presence checking
- **Multiple Properties**: Can check multiple properties with AND logic
- **Default Behavior**: Can specify behavior when property is missing
- **Relaxed Binding**: Uses Spring Boot's relaxed binding for property names
- **Feature Toggles**: Perfect for implementing feature flags and toggles
- **Environment Configuration**: Enables/disables features based on configuration
- **Auto-Configuration**: Heavily used in Spring Boot's auto-configuration classes

Ideal for creating feature flags and environment-specific bean creation.

**Use**: Property-driven configuration.
```java
@Service
@ConditionalOnProperty(name = "feature.email.enabled", havingValue = "true")
public class EmailService {
    public void sendEmail(String message) {}
}
```

### 63. @ConditionalOnClass
**Theory**: Creates beans only when specific classes are present on the classpath, enabling library-dependent configuration:
- **Classpath Detection**: Checks for class presence without actually loading the class
- **Library Integration**: Automatically configures beans when optional dependencies are available
- **Graceful Degradation**: Allows applications to work with or without optional libraries
- **Auto-Configuration**: Core mechanism behind Spring Boot's auto-configuration
- **Multiple Classes**: Can check for multiple classes simultaneously
- **Performance**: Uses string-based class names to avoid ClassNotFoundException
- **Optional Dependencies**: Perfect for handling optional Maven/Gradle dependencies
- **Modular Design**: Enables modular application design with optional components

Essential for creating auto-configuration that adapts to available libraries.

**Use**: Library-dependent configuration.
```java
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return new RedisTemplate<>();
    }
}
```

### 64. @ConditionalOnMissingBean
**Theory**: Creates a bean only when no other bean of the same type (or specified type) exists in the application context:
- **Default Implementations**: Provides default beans that can be overridden by user-defined beans
- **Fallback Strategy**: Creates fallback beans when specific implementations aren't provided
- **Type Checking**: Can check by type, name, or annotation
- **Auto-Configuration**: Prevents conflicts between auto-configured and user-defined beans
- **Customization**: Allows users to override default behavior by providing their own beans
- **Bean Hierarchy**: Respects bean inheritance and interface implementations
- **Performance**: Efficient as it only checks the bean registry
- **Flexibility**: Supports complex matching criteria beyond simple type matching

Crucial for providing sensible defaults while allowing user customization.

**Use**: Default bean creation.
```java
@Bean
@ConditionalOnMissingBean(DataSource.class)
public DataSource defaultDataSource() {
    return new EmbeddedDatabaseBuilder().build();
}
```

### 65. @EnableAutoConfiguration
**Theory**: Activates Spring Boot's auto-configuration mechanism, which automatically configures beans based on classpath contents:
- **Intelligent Defaults**: Automatically configures common scenarios based on available libraries
- **Classpath Scanning**: Analyzes classpath to determine what to configure
- **Conditional Configuration**: Uses various @Conditional annotations to make smart decisions
- **Exclusion Support**: Allows excluding specific auto-configurations
- **Override Capability**: Auto-configurations can be overridden by user-defined beans
- **Performance Optimization**: Only configures what's actually needed
- **Convention over Configuration**: Reduces boilerplate configuration code
- **Extensibility**: Custom auto-configurations can be created and distributed

This is what makes Spring Boot "opinionated" and reduces configuration overhead.

**Use**: Automatic configuration.
```java
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class CustomConfig {}
```

### 66. @Import
**Theory**: Imports additional configuration classes, enabling modular and reusable configuration:
- **Configuration Composition**: Combines multiple configuration classes into a cohesive setup
- **Reusability**: Allows sharing configuration classes across different applications
- **Modularity**: Breaks large configurations into smaller, focused modules
- **Selective Import**: Can import specific configurations as needed
- **Dependency Management**: Imported configurations can have their own dependencies
- **Order Control**: Import order can affect bean creation order
- **Conditional Import**: Can be combined with @Conditional for selective importing
- **Third-Party Integration**: Useful for integrating third-party configuration classes

Essential for organizing complex configurations and promoting code reuse.

**Use**: Modular configuration.
```java
@Configuration
@Import({DatabaseConfig.class, SecurityConfig.class})
public class MainConfig {}
```

### 67. @PropertySource
**Theory**: Adds property files to Spring's Environment, making their properties available for injection and configuration:
- **External Configuration**: Loads properties from external files (classpath or file system)
- **Multiple Sources**: Can specify multiple property files with different priorities
- **Encoding Support**: Supports different character encodings for property files
- **Placeholder Resolution**: Properties can reference other properties using placeholders
- **Environment Integration**: Properties become available through Spring's Environment abstraction
- **Override Capability**: Later property sources can override earlier ones
- **Conditional Loading**: Can be combined with @Profile for environment-specific properties
- **Resource Patterns**: Supports Spring's resource loading patterns and wildcards

Useful for loading configuration from custom property files beyond application.properties.

**Use**: External property files.
```java
@Configuration
@PropertySource("classpath:custom.properties")
public class CustomConfig {}
```

---

## Validation Annotations

### 68. @Valid
**Theory**: Triggers Bean Validation (JSR-303/JSR-380) on method parameters, return values, or nested objects:
- **Cascade Validation**: Validates nested objects and collections recursively
- **Standard Compliance**: Part of Java Bean Validation specification, not Spring-specific
- **Automatic Triggering**: Validation occurs automatically when the annotation is present
- **Exception Handling**: Throws ConstraintViolationException when validation fails
- **Integration**: Works seamlessly with Spring MVC for request validation
- **Nested Objects**: Validates complex object graphs with nested validation
- **Collection Support**: Can validate elements within collections and arrays
- **Framework Agnostic**: Can be used with any JSR-303 compliant validation framework

Essential for ensuring data integrity and input validation in web applications.

**Use**: Bean validation.
```java
@PostMapping("/users")
public User createUser(@Valid @RequestBody User user) {
    return userService.save(user);
}
```

### 69. @Validated
**Theory**: Spring's enhanced version of @Valid that adds support for validation groups and method-level validation:
- **Validation Groups**: Supports JSR-303 validation groups for conditional validation
- **Method-Level**: Can be applied at class level to enable method parameter validation
- **Group Sequences**: Supports validation group sequences for ordered validation
- **Spring Integration**: Provides better integration with Spring's validation infrastructure
- **AOP-Based**: Uses Spring AOP for method-level validation
- **Flexible Scenarios**: Enables different validation rules for different scenarios (create vs update)
- **Exception Handling**: Integrates with Spring's exception handling mechanisms
- **Performance**: Can optimize validation by only running relevant group validations

Ideal for complex validation scenarios that require different rules in different contexts.

**Use**: Validation groups.
```java
@Service
@Validated
public class UserService {
    public User save(@Valid User user) {
        return userRepository.save(user);
    }
}
```

### 70. @NotNull
**Theory**: Validates that a field, method parameter, or return value is not null:
- **Null Check**: Performs simple null reference validation
- **Primitive Types**: Not applicable to primitive types (int, boolean, etc.) as they can't be null
- **Object References**: Validates object references, collections, arrays, and strings
- **Empty vs Null**: Allows empty strings, empty collections - only checks for null
- **Database Integration**: Often corresponds to NOT NULL database constraints
- **Required Fields**: Indicates mandatory fields in forms and APIs
- **Early Validation**: Catches null pointer exceptions early in the validation process
- **Message Customization**: Supports custom error messages for better user experience

Fundamental validation annotation for ensuring required fields are provided.

**Use**: Null validation.
```java
public class User {
    @NotNull(message = "Name cannot be null")
    private String name;
}
```

### 71. @NotEmpty
**Theory**: Validates that collections, arrays, maps, or strings are not null and not empty:
- **Null and Empty Check**: Combines null check with emptiness check
- **Collection Support**: Works with Collections, Maps, arrays, and CharSequences
- **Size Validation**: Ensures collections have at least one element
- **String Validation**: Ensures strings have at least one character (including whitespace)
- **Whitespace Allowed**: Allows strings with only whitespace characters
- **API Validation**: Perfect for validating that required lists or arrays contain data
- **Form Validation**: Ensures users provide meaningful input in form fields
- **Business Logic**: Enforces business rules that require non-empty data

Useful when you need to ensure collections or strings contain actual data, not just non-null values.

**Use**: Empty validation for collections/strings.
```java
public class User {
    @NotEmpty(message = "Email cannot be empty")
    private String email;
}
```

### 72. @NotBlank
**Theory**: Validates that strings are not null, not empty, and contain at least one non-whitespace character:
- **Comprehensive String Check**: Combines null, empty, and whitespace-only validation
- **Trimming Logic**: Effectively validates that string.trim().length() > 0
- **User Input**: Perfect for validating user input where whitespace-only input is meaningless
- **String-Specific**: Only applicable to CharSequence types (String, StringBuilder, etc.)
- **Meaningful Content**: Ensures strings contain actual meaningful content
- **Form Validation**: Prevents users from submitting forms with only spaces
- **Data Quality**: Maintains data quality by rejecting meaningless string values
- **Database Storage**: Prevents storing empty or whitespace-only values in databases

Most restrictive string validation, ensuring strings contain actual meaningful content.

**Use**: String validation.
```java
public class User {
    @NotBlank(message = "Username cannot be blank")
    private String username;
}
```

### 73. @Size
**Theory**: Validates the size/length of strings, collections, maps, and arrays within specified bounds:
- **Range Validation**: Supports minimum and maximum size constraints
- **Multiple Types**: Works with strings (character count), collections (element count), arrays (length)
- **Flexible Bounds**: Can specify min only, max only, or both
- **Inclusive Bounds**: Min and max values are inclusive in the validation
- **Performance**: Efficient validation using built-in size/length methods
- **User Experience**: Provides clear feedback about acceptable input lengths
- **Database Constraints**: Often mirrors database column length constraints
- **Business Rules**: Enforces business rules about data size limitations

Essential for enforcing length constraints and preventing oversized data input.

**Use**: Length/size constraints.
```java
public class User {
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @Size(max = 5, message = "Maximum 5 roles allowed")
    private List<Role> roles;
}
```

### 74. @Min / @Max
**Theory**: Validates that numeric values fall within specified minimum and maximum bounds:
- **Numeric Validation**: Works with all numeric types (int, long, double, BigDecimal, etc.)
- **Inclusive Bounds**: Min and max values are inclusive in the validation
- **Range Enforcement**: Prevents values outside acceptable business ranges
- **Type Safety**: Automatically handles different numeric types and precision
- **Business Rules**: Enforces business constraints like age limits, price ranges, quantities
- **Database Constraints**: Often corresponds to database check constraints
- **User Input**: Validates form inputs for numeric fields
- **API Validation**: Ensures API parameters are within acceptable ranges

Crucial for validating numeric inputs and enforcing business range constraints.

**Use**: Number range validation.
```java
public class Product {
    @Min(value = 0, message = "Price cannot be negative")
    @Max(value = 10000, message = "Price cannot exceed 10000")
    private BigDecimal price;
}
```

### 75. @Email
**Theory**: Validates that string values conform to email address format according to RFC standards:
- **Format Validation**: Checks email format using regular expressions or RFC-compliant validators
- **Standard Compliance**: Follows internet standards for email address format
- **Local and Domain**: Validates both local part (before @) and domain part (after @)
- **Special Characters**: Handles special characters allowed in email addresses
- **Internationalization**: Modern implementations support international domain names
- **User Registration**: Essential for user registration and contact forms
- **Data Quality**: Ensures email addresses are properly formatted for delivery
- **Business Logic**: Prevents invalid email addresses from entering the system

Note: Format validation doesn't guarantee the email address actually exists or is deliverable.

**Use**: Email validation.
```java
public class User {
    @Email(message = "Invalid email format")
    private String email;
}
```

### 76. @Pattern
**Theory**: Validates string values against custom regular expressions, providing flexible format validation:
- **Regex Validation**: Uses Java regular expressions for pattern matching
- **Custom Formats**: Enables validation of any custom string format (phone numbers, IDs, codes)
- **Flexible Matching**: Supports complex patterns with groups, quantifiers, and character classes
- **Case Sensitivity**: Can control case-sensitive or case-insensitive matching
- **Performance**: Regex compilation is cached for performance
- **Internationalization**: Can handle Unicode patterns for international formats
- **Business Rules**: Enforces specific business format requirements
- **Security**: Can prevent injection attacks by validating input formats

Most flexible validation annotation for custom string format requirements.

**Use**: Custom format validation.
```java
public class User {
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;
}
```

---

## Caching Annotations

### 77. @EnableCaching
**Theory**: Enable Spring's annotation-driven cache management.
**Use**: Cache configuration.
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "products");
    }
}
```

### 78. @Cacheable
**Theory**: Cache method results.
**Use**: Read-through caching.
```java
@Service
public class UserService {
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id);
    }
}
```

### 79. @CacheEvict
**Theory**: Remove entries from cache.
**Use**: Cache invalidation.
```java
@CacheEvict(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}

@CacheEvict(value = "users", allEntries = true)
public void clearAllUsers() {}
```

### 80. @CachePut
**Theory**: Always execute method and cache result.
**Use**: Write-through caching.
```java
@CachePut(value = "users", key = "#user.id")
public User saveUser(User user) {
    return userRepository.save(user);
}
```

### 81. @Caching
**Theory**: Group multiple cache operations.
**Use**: Complex cache operations.
```java
@Caching(
    cacheable = @Cacheable(value = "users", key = "#id"),
    evict = @CacheEvict(value = "userStats", key = "#id")
)
public User getUserWithStats(Long id) {
    return userService.findByIdWithStats(id);
}
```

---

## Scheduling Annotations

### 82. @EnableScheduling
**Theory**: Enable Spring's scheduled task execution capability.
**Use**: Task scheduling configuration.
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {}
```

### 83. @Scheduled
**Theory**: Mark method for scheduled execution.
**Use**: Periodic task execution.
```java
@Component
public class ScheduledTasks {
    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        System.out.println("Current time: " + new Date());
    }
    
    @Scheduled(cron = "0 0 1 * * ?")
    public void performDailyTask() {
        // Daily task at 1 AM
    }
    
    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void delayedTask() {
        // Execute with delay
    }
}
```

### 84. @Async
**Theory**: Mark method for asynchronous execution.
**Use**: Non-blocking operations.
```java
@Service
public class EmailService {
    @Async
    public CompletableFuture<String> sendEmail(String recipient, String message) {
        // Send email asynchronously
        return CompletableFuture.completedFuture("Email sent");
    }
}
```

### 85. @EnableAsync
**Theory**: Enable Spring's asynchronous method execution capability.
**Use**: Async configuration.
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        return executor;
    }
}
```

---

## Messaging Annotations

### 86. @JmsListener
**Theory**: Mark method as JMS message listener.
**Use**: Message consumption.
```java
@Component
public class OrderMessageListener {
    @JmsListener(destination = "order.queue")
    public void processOrder(Order order) {
        orderService.process(order);
    }
}
```

### 87. @RabbitListener
**Theory**: Mark method as RabbitMQ message listener.
**Use**: RabbitMQ message consumption.
```java
@Component
public class NotificationListener {
    @RabbitListener(queues = "notification.queue")
    public void handleNotification(NotificationMessage message) {
        notificationService.send(message);
    }
}
```

### 88. @KafkaListener
**Theory**: Mark method as Kafka message listener.
**Use**: Kafka message consumption.
```java
@Component
public class EventListener {
    @KafkaListener(topics = "user.events", groupId = "user-service")
    public void handleUserEvent(UserEvent event) {
        userEventService.process(event);
    }
}
```

### 89. @EventListener
**Theory**: Mark method as application event listener.
**Use**: Internal event handling.
```java
@Component
public class UserEventHandler {
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        emailService.sendWelcomeEmail(event.getUser());
    }
    
    @EventListener(condition = "#event.user.premium")
    public void handlePremiumUser(UserCreatedEvent event) {
        premiumService.setupPremiumFeatures(event.getUser());
    }
}
```

---

## Additional Important Annotations

### 90. @Bean
**Theory**: Mark method as bean producer.
**Use**: Bean definition in configuration classes.
```java
@Configuration
public class AppConfig {
    @Bean
    @Scope("prototype")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### 91. @Scope
**Theory**: Define bean scope (singleton, prototype, request, session).
**Use**: Bean lifecycle management.
```java
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrototypeBean {}

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean {}
```

### 92. @Lazy
**Theory**: Lazy initialization of beans.
**Use**: Defer bean creation until needed.
```java
@Component
@Lazy
public class ExpensiveService {
    public ExpensiveService() {
        // Expensive initialization
    }
}
```

### 93. @DependsOn
**Theory**: Specify bean initialization order.
**Use**: Control bean creation sequence.
```java
@Component
@DependsOn({"databaseService", "cacheService"})
public class ApplicationService {}
```

### 94. @Order
**Theory**: Define execution order for components.
**Use**: Order beans in collections.
```java
@Component
@Order(1)
public class FirstProcessor implements MessageProcessor {}

@Component
@Order(2)
public class SecondProcessor implements MessageProcessor {}
```

### 95. @PostConstruct
**Theory**: Mark method to execute after dependency injection.
**Use**: Bean initialization.
```java
@Component
public class DatabaseService {
    @PostConstruct
    public void init() {
        // Initialize database connections
    }
}
```

### 96. @PreDestroy
**Theory**: Mark method to execute before bean destruction.
**Use**: Bean cleanup.
```java
@Component
public class ConnectionService {
    @PreDestroy
    public void cleanup() {
        // Close connections
    }
}
```

### 97. @ComponentScan
**Theory**: Configure component scanning.
**Use**: Specify packages to scan for components.
```java
@Configuration
@ComponentScan(basePackages = {"com.example.service", "com.example.repository"})
public class AppConfig {}
```

### 98. @EnableConfigurationProperties
**Theory**: Enable @ConfigurationProperties beans.
**Use**: Configuration properties support.
```java
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class Config {}
```

### 99. @JsonIgnore
**Theory**: Ignore field during JSON serialization/deserialization.
**Use**: Control JSON output.
```java
public class User {
    private String name;
    
    @JsonIgnore
    private String password;
}
```

### 100. @JsonProperty
**Theory**: Map JSON property to field with different name.
**Use**: JSON field mapping.
```java
public class User {
    @JsonProperty("full_name")
    private String name;
    
    @JsonProperty("user_id")
    private Long id;
}
```

---

## Interview Tips & Best Practices

### Common Interview Questions

1. **Difference between @Component, @Service, @Repository, @Controller?**
   - All are stereotypes of @Component
   - @Service: Business logic layer
   - @Repository: Data access layer with exception translation
   - @Controller: Presentation layer

2. **@Autowired vs @Resource vs @Inject?**
   - @Autowired: Spring-specific, by type
   - @Resource: JSR-250, by name then type
   - @Inject: JSR-330, by type

3. **@RequestParam vs @PathVariable vs @RequestBody?**
   - @RequestParam: Query parameters (?name=value)
   - @PathVariable: URI path segments (/users/{id})
   - @RequestBody: HTTP request body (JSON/XML)

4. **@Transactional propagation and isolation?**
   ```java
   @Transactional(
       propagation = Propagation.REQUIRED,
       isolation = Isolation.READ_COMMITTED,
       rollbackFor = Exception.class,
       timeout = 30
   )
   ```

5. **@Cacheable vs @CachePut vs @CacheEvict?**
   - @Cacheable: Cache if not present
   - @CachePut: Always cache
   - @CacheEvict: Remove from cache

### Performance Considerations

1. **Use @Lazy for expensive beans**
2. **Prefer constructor injection over field injection**
3. **Use @Async for non-blocking operations**
4. **Configure proper cache eviction strategies**
5. **Use @Transactional(readOnly = true) for read operations**

### Security Best Practices

1. **Always validate input with @Valid**
2. **Use method-level security with @PreAuthorize**
3. **Sanitize @RequestParam and @PathVariable**
4. **Use @JsonIgnore for sensitive fields**
5. **Implement proper CORS with @CrossOrigin**

---

## Conclusion

This guide covers the 100 most important Spring Boot annotations used in production applications and frequently asked in interviews. Understanding these annotations and their proper usage is crucial for building robust, scalable Spring Boot applications.

**Key Takeaways:**
- Use appropriate stereotype annotations (@Service, @Repository, @Controller)
- Leverage validation annotations for data integrity
- Implement proper caching strategies
- Use security annotations for access control
- Apply testing annotations for comprehensive test coverage
- Configure applications using @ConfigurationProperties
- Handle asynchronous operations with @Async
- Implement proper transaction management with @Transactional

Remember to always consider performance, security, and maintainability when using these annotations in your applications.