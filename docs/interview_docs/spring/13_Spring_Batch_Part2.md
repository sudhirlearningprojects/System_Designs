# Spring Batch - Part 2: Readers, Processors & Writers

[← Back to Index](README.md) | [← Previous: Part 1](12_Spring_Batch_Part1.md) | [Next: Spring Integration →](14_Spring_Integration.md)

## Table of Contents
- [ItemReader](#itemreader)
- [ItemProcessor](#itemprocessor)
- [ItemWriter](#itemwriter)
- [Real-World Example](#real-world-example)

---

## ItemReader

### Understanding ItemReader

**ItemReader**: Reads data one item at a time from source

**Contract**:
```java
public interface ItemReader<T> {
    T read() throws Exception; // Returns null when done
}
```

### CSV/Flat File Reader

```java
@Bean
@StepScope
public FlatFileItemReader<User> csvReader(
        @Value("#{jobParameters['inputFile']}") String inputFile) {
    return new FlatFileItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource(inputFile))
        .delimited()
        .delimiter(",")
        .names("id", "name", "email", "age")
        .linesToSkip(1) // Skip header
        .targetType(User.class)
        .build();
}
```

**Input CSV**:
```csv
id,name,email,age
1,John Doe,john@example.com,30
2,Jane Smith,jane@example.com,25
```

### Fixed-Width File Reader

```java
@Bean
public FlatFileItemReader<User> fixedWidthReader() {
    return new FlatFileItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource("users.txt"))
        .fixedLength()
        .columns(new Range(1, 10), new Range(11, 30), new Range(31, 50))
        .names("id", "name", "email")
        .targetType(User.class)
        .build();
}
```

### Custom FieldSetMapper

```java
@Bean
public FlatFileItemReader<User> csvReaderWithMapper() {
    return new FlatFileItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource("users.csv"))
        .delimited()
        .names("id", "firstName", "lastName", "email", "age")
        .fieldSetMapper(fieldSet -> {
            User user = new User();
            user.setId(fieldSet.readLong("id"));
            user.setFirstName(fieldSet.readString("firstName"));
            user.setLastName(fieldSet.readString("lastName"));
            user.setEmail(fieldSet.readString("email"));
            user.setAge(fieldSet.readInt("age"));
            user.setFullName(fieldSet.readString("firstName") + " " + fieldSet.readString("lastName"));
            return user;
        })
        .build();
}
```

### JDBC Cursor Reader

**Best for**: Small to medium datasets

```java
@Bean
public JdbcCursorItemReader<User> jdbcCursorReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<User>()
        .name("userReader")
        .dataSource(dataSource)
        .sql("SELECT id, name, email, age FROM users WHERE active = true ORDER BY id")
        .rowMapper(new BeanPropertyRowMapper<>(User.class))
        .build();
}

// Custom RowMapper
@Bean
public JdbcCursorItemReader<User> jdbcCursorReaderCustom(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<User>()
        .name("userReader")
        .dataSource(dataSource)
        .sql("SELECT id, first_name, last_name, email, age FROM users")
        .rowMapper((rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setEmail(rs.getString("email"));
            user.setAge(rs.getInt("age"));
            return user;
        })
        .build();
}
```

### JDBC Paging Reader

**Best for**: Large datasets (millions of records)

```java
@Bean
public JdbcPagingItemReader<User> jdbcPagingReader(DataSource dataSource) {
    Map<String, Order> sortKeys = new HashMap<>();
    sortKeys.put("id", Order.ASCENDING);
    
    return new JdbcPagingItemReaderBuilder<User>()
        .name("userReader")
        .dataSource(dataSource)
        .selectClause("SELECT id, name, email, age")
        .fromClause("FROM users")
        .whereClause("WHERE active = true")
        .sortKeys(sortKeys)
        .pageSize(1000)
        .rowMapper(new BeanPropertyRowMapper<>(User.class))
        .build();
}
```

### JPA Reader

```java
@Bean
public JpaPagingItemReader<User> jpaReader(EntityManagerFactory emf) {
    return new JpaPagingItemReaderBuilder<User>()
        .name("userReader")
        .entityManagerFactory(emf)
        .queryString("SELECT u FROM User u WHERE u.active = true ORDER BY u.id")
        .pageSize(100)
        .build();
}

// With parameters
@Bean
@StepScope
public JpaPagingItemReader<User> jpaReaderWithParams(
        EntityManagerFactory emf,
        @Value("#{jobParameters['minAge']}") Integer minAge) {
    
    Map<String, Object> params = new HashMap<>();
    params.put("minAge", minAge);
    
    return new JpaPagingItemReaderBuilder<User>()
        .name("userReader")
        .entityManagerFactory(emf)
        .queryString("SELECT u FROM User u WHERE u.age >= :minAge ORDER BY u.id")
        .parameterValues(params)
        .pageSize(100)
        .build();
}
```

### MongoDB Reader

```java
@Bean
public MongoItemReader<User> mongoReader(MongoTemplate mongoTemplate) {
    return new MongoItemReaderBuilder<User>()
        .name("userReader")
        .template(mongoTemplate)
        .jsonQuery("{active: true}")
        .targetType(User.class)
        .sorts(Collections.singletonMap("_id", Sort.Direction.ASC))
        .pageSize(100)
        .build();
}
```

### JSON Reader

```java
@Bean
public JsonItemReader<User> jsonReader() {
    return new JsonItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource("users.json"))
        .jsonObjectReader(new JacksonJsonObjectReader<>(User.class))
        .build();
}
```

**Input JSON**:
```json
[
  {"id": 1, "name": "John", "email": "john@example.com"},
  {"id": 2, "name": "Jane", "email": "jane@example.com"}
]
```

### XML Reader

```java
@Bean
public StaxEventItemReader<User> xmlReader() {
    return new StaxEventItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource("users.xml"))
        .addFragmentRootElements("user")
        .unmarshaller(userUnmarshaller())
        .build();
}

@Bean
public Jaxb2Marshaller userUnmarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(User.class);
    return marshaller;
}
```

### Multi-Resource Reader

**Read from multiple files**:

```java
@Bean
public MultiResourceItemReader<User> multiFileReader() {
    return new MultiResourceItemReaderBuilder<User>()
        .name("multiFileReader")
        .resources(new FileSystemResource("users1.csv"),
                   new FileSystemResource("users2.csv"),
                   new FileSystemResource("users3.csv"))
        .delegate(csvReader())
        .build();
}
```

### Custom ItemReader

```java
public class CustomItemReader implements ItemReader<User> {
    
    private List<User> users;
    private int index = 0;
    
    public CustomItemReader(List<User> users) {
        this.users = users;
    }
    
    @Override
    public User read() {
        if (index < users.size()) {
            return users.get(index++);
        }
        return null; // Signals end of data
    }
}
```

### Reader Comparison

| Reader | Use Case | Performance | Memory |
|--------|----------|-------------|--------|
| FlatFile | CSV/Text files | Fast | Low |
| JdbcCursor | Small datasets | Medium | Low |
| JdbcPaging | Large datasets | Fast | Low |
| JPA | ORM entities | Medium | Medium |
| MongoDB | NoSQL | Fast | Low |
| JSON | JSON files | Medium | Medium |
| XML | XML files | Slow | High |

---

## ItemProcessor

### Understanding ItemProcessor

**ItemProcessor**: Transforms/validates data

**Contract**:
```java
public interface ItemProcessor<I, O> {
    O process(I item) throws Exception; // Return null to filter
}
```

### Basic Processor

```java
@Component
public class UserProcessor implements ItemProcessor<User, User> {
    
    @Override
    public User process(User user) throws Exception {
        // Transform
        user.setName(user.getName().toUpperCase());
        user.setEmail(user.getEmail().toLowerCase());
        
        // Validate - return null to skip
        if (user.getAge() < 18) {
            return null;
        }
        
        // Enrich
        user.setProcessedDate(LocalDateTime.now());
        user.setStatus("PROCESSED");
        
        return user;
    }
}
```

### Filtering Items

```java
@Component
public class FilteringProcessor implements ItemProcessor<User, User> {
    
    @Override
    public User process(User user) {
        // Filter out inactive users
        if (!user.isActive()) {
            return null; // Item will be skipped
        }
        
        // Filter by age
        if (user.getAge() < 18 || user.getAge() > 65) {
            return null;
        }
        
        return user;
    }
}
```

### Validation Processor

```java
@Component
public class ValidationProcessor implements ItemProcessor<User, User> {
    
    @Override
    public User process(User user) throws ValidationException {
        // Email validation
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new ValidationException("Invalid email: " + user.getEmail());
        }
        
        // Name validation
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }
        
        // Age validation
        if (user.getAge() < 0 || user.getAge() > 150) {
            throw new ValidationException("Invalid age: " + user.getAge());
        }
        
        return user;
    }
}
```

### Transformation Processor

```java
@Component
public class TransformationProcessor implements ItemProcessor<User, User> {
    
    @Override
    public User process(User user) {
        // Normalize data
        user.setName(user.getName().trim());
        user.setEmail(user.getEmail().toLowerCase().trim());
        
        // Calculate derived fields
        user.setFullName(user.getFirstName() + " " + user.getLastName());
        user.setAgeGroup(calculateAgeGroup(user.getAge()));
        
        // Format data
        user.setPhone(formatPhone(user.getPhone()));
        
        return user;
    }
    
    private String calculateAgeGroup(int age) {
        if (age < 18) return "MINOR";
        if (age < 30) return "YOUNG_ADULT";
        if (age < 50) return "ADULT";
        return "SENIOR";
    }
    
    private String formatPhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}
```

### Enrichment Processor

```java
@Component
public class EnrichmentProcessor implements ItemProcessor<User, User> {
    
    @Autowired
    private AddressService addressService;
    
    @Autowired
    private ScoreService scoreService;
    
    @Override
    public User process(User user) {
        // Enrich with external data
        Address address = addressService.getAddress(user.getZipCode());
        user.setCity(address.getCity());
        user.setState(address.getState());
        
        // Calculate score
        int score = scoreService.calculateScore(user);
        user.setScore(score);
        
        // Add metadata
        user.setProcessedBy("BATCH_JOB");
        user.setProcessedDate(LocalDateTime.now());
        
        return user;
    }
}
```

### Type Conversion Processor

```java
@Component
public class UserToAccountProcessor implements ItemProcessor<User, Account> {
    
    @Override
    public Account process(User user) {
        // Convert User to Account
        Account account = new Account();
        account.setUserId(user.getId());
        account.setUsername(user.getEmail());
        account.setAccountType("STANDARD");
        account.setCreatedDate(LocalDateTime.now());
        account.setStatus("ACTIVE");
        return account;
    }
}
```

### Composite Processor

**Chain multiple processors**:

```java
@Bean
public CompositeItemProcessor<User, User> compositeProcessor() {
    CompositeItemProcessor<User, User> processor = new CompositeItemProcessor<>();
    processor.setDelegates(Arrays.asList(
        validationProcessor(),
        transformationProcessor(),
        enrichmentProcessor()
    ));
    return processor;
}

@Bean
public ItemProcessor<User, User> validationProcessor() {
    return user -> {
        if (user.getEmail() == null) throw new ValidationException("Email required");
        return user;
    };
}

@Bean
public ItemProcessor<User, User> transformationProcessor() {
    return user -> {
        user.setName(user.getName().toUpperCase());
        return user;
    };
}

@Bean
public ItemProcessor<User, User> enrichmentProcessor() {
    return user -> {
        user.setProcessedDate(LocalDateTime.now());
        return user;
    };
}
```

### Async Processor

**Process items in parallel**:

```java
@Bean
public AsyncItemProcessor<User, User> asyncProcessor() {
    AsyncItemProcessor<User, User> asyncProcessor = new AsyncItemProcessor<>();
    asyncProcessor.setDelegate(userProcessor());
    asyncProcessor.setTaskExecutor(taskExecutor());
    return asyncProcessor;
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("batch-");
    executor.initialize();
    return executor;
}

// Use with AsyncItemWriter
@Bean
public Step asyncStep() {
    return stepBuilderFactory.get("asyncStep")
        .<User, Future<User>>chunk(100)
        .reader(reader())
        .processor(asyncProcessor())
        .writer(asyncWriter())
        .build();
}

@Bean
public AsyncItemWriter<User> asyncWriter() {
    AsyncItemWriter<User> asyncWriter = new AsyncItemWriter<>();
    asyncWriter.setDelegate(writer());
    return asyncWriter;
}
```

### Conditional Processor

```java
@Component
public class ConditionalProcessor implements ItemProcessor<User, User> {
    
    @Value("#{jobParameters['processType']}")
    private String processType;
    
    @Override
    public User process(User user) {
        if ("PREMIUM".equals(processType)) {
            return processPremium(user);
        } else if ("STANDARD".equals(processType)) {
            return processStandard(user);
        }
        return user;
    }
    
    private User processPremium(User user) {
        user.setAccountType("PREMIUM");
        user.setDiscount(0.20);
        return user;
    }
    
    private User processStandard(User user) {
        user.setAccountType("STANDARD");
        user.setDiscount(0.10);
        return user;
    }
}
```

### Best Practices

✅ **DO**:
- Keep processors stateless
- Return null to filter items
- Throw exceptions for validation errors
- Use CompositeItemProcessor for multiple operations

❌ **DON'T**:
- Store state in processor
- Perform database writes (use writer)
- Catch all exceptions
- Make processors too complex

---

## ItemWriter

### Understanding ItemWriter

**ItemWriter**: Writes chunk of items to destination

**Contract**:
```java
public interface ItemWriter<T> {
    void write(List<? extends T> items) throws Exception;
}
```

### JDBC Batch Writer

```java
@Bean
public JdbcBatchItemWriter<User> jdbcWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<User>()
        .dataSource(dataSource)
        .sql("INSERT INTO processed_users (id, name, email, age, status) " +
             "VALUES (:id, :name, :email, :age, :status)")
        .beanMapped()
        .build();
}

// With ItemSqlParameterSourceProvider
@Bean
public JdbcBatchItemWriter<User> jdbcWriterCustom(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<User>()
        .dataSource(dataSource)
        .sql("INSERT INTO users (id, full_name, email, created_date) " +
             "VALUES (:id, :fullName, :email, :createdDate)")
        .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
        .build();
}
```

### JPA Writer

```java
@Bean
public JpaItemWriter<User> jpaWriter(EntityManagerFactory emf) {
    JpaItemWriter<User> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(emf);
    return writer;
}
```

### MongoDB Writer

```java
@Bean
public MongoItemWriter<User> mongoWriter(MongoTemplate mongoTemplate) {
    return new MongoItemWriterBuilder<User>()
        .template(mongoTemplate)
        .collection("users")
        .build();
}
```

### File Writer (CSV)

```java
@Bean
public FlatFileItemWriter<User> csvWriter() {
    return new FlatFileItemWriterBuilder<User>()
        .name("userWriter")
        .resource(new FileSystemResource("output/users.csv"))
        .delimited()
        .delimiter(",")
        .names("id", "name", "email", "age", "status")
        .headerCallback(writer -> writer.write("ID,Name,Email,Age,Status"))
        .footerCallback(writer -> writer.write("Total records: " + writer.getLineCount()))
        .build();
}
```

### File Writer (Fixed Width)

```java
@Bean
public FlatFileItemWriter<User> fixedWidthWriter() {
    return new FlatFileItemWriterBuilder<User>()
        .name("userWriter")
        .resource(new FileSystemResource("output/users.txt"))
        .formatted()
        .format("%10d%30s%50s%5d")
        .names("id", "name", "email", "age")
        .build();
}
```

### JSON Writer

```java
@Bean
public JsonFileItemWriter<User> jsonWriter() {
    return new JsonFileItemWriterBuilder<User>()
        .name("userWriter")
        .resource(new FileSystemResource("output/users.json"))
        .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
        .build();
}
```

### XML Writer

```java
@Bean
public StaxEventItemWriter<User> xmlWriter() {
    return new StaxEventItemWriterBuilder<User>()
        .name("userWriter")
        .resource(new FileSystemResource("output/users.xml"))
        .marshaller(userMarshaller())
        .rootTagName("users")
        .build();
}

@Bean
public Jaxb2Marshaller userMarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(User.class);
    return marshaller;
}
```

### Composite Writer

**Write to multiple destinations**:

```java
@Bean
public CompositeItemWriter<User> compositeWriter() {
    CompositeItemWriter<User> writer = new CompositeItemWriter<>();
    writer.setDelegates(Arrays.asList(
        jdbcWriter(dataSource()),
        csvWriter(),
        kafkaWriter()
    ));
    return writer;
}
```

### Kafka Writer

```java
@Bean
public KafkaItemWriter<String, User> kafkaWriter(KafkaTemplate<String, User> kafkaTemplate) {
    return new KafkaItemWriterBuilder<String, User>()
        .kafkaTemplate(kafkaTemplate)
        .itemKeyMapper(user -> user.getId().toString())
        .build();
}
```

### Custom ItemWriter

```java
@Component
public class CustomItemWriter implements ItemWriter<User> {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void write(List<? extends User> items) throws Exception {
        System.out.println("Writing " + items.size() + " items");
        
        for (User user : items) {
            // Custom logic
            user.setLastUpdated(LocalDateTime.now());
        }
        
        userRepository.saveAll(items);
    }
}
```

### Conditional Writer

```java
@Component
public class ConditionalWriter implements ItemWriter<User> {
    
    @Autowired
    private JdbcBatchItemWriter<User> databaseWriter;
    
    @Autowired
    private FlatFileItemWriter<User> fileWriter;
    
    @Value("#{jobParameters['outputType']}")
    private String outputType;
    
    @Override
    public void write(List<? extends User> items) throws Exception {
        if ("DATABASE".equals(outputType)) {
            databaseWriter.write(items);
        } else if ("FILE".equals(outputType)) {
            fileWriter.write(items);
        }
    }
}
```

### Multi-Resource Writer

**Write to multiple files**:

```java
@Bean
public MultiResourceItemWriter<User> multiFileWriter() {
    return new MultiResourceItemWriterBuilder<User>()
        .name("multiFileWriter")
        .resource(new FileSystemResource("output/users"))
        .itemCountLimitPerResource(1000) // 1000 records per file
        .resourceSuffixCreator(index -> index + ".csv")
        .delegate(csvWriter())
        .build();
}
```

### Writer with Callback

```java
@Bean
public FlatFileItemWriter<User> writerWithCallback() {
    return new FlatFileItemWriterBuilder<User>()
        .name("userWriter")
        .resource(new FileSystemResource("output/users.csv"))
        .delimited()
        .names("id", "name", "email")
        .headerCallback(writer -> {
            writer.write("# Generated on: " + LocalDateTime.now());
            writer.write("ID,Name,Email");
        })
        .footerCallback(writer -> {
            writer.write("# Total: " + writer.getLineCount() + " records");
        })
        .build();
}
```

### Writer Comparison

| Writer | Use Case | Performance | Transaction |
|--------|----------|-------------|-------------|
| JdbcBatch | Database | Fast | Yes |
| JPA | ORM entities | Medium | Yes |
| FlatFile | CSV/Text | Fast | No |
| JSON | JSON files | Medium | No |
| XML | XML files | Slow | No |
| MongoDB | NoSQL | Fast | Yes |
| Kafka | Messaging | Fast | No |

### Best Practices

✅ **DO**:
- Use batch operations
- Configure appropriate chunk size
- Handle exceptions properly
- Use transactions for database writes

❌ **DON'T**:
- Write items one by one
- Ignore write failures
- Mix different write types in same writer
- Forget to close resources

---

## Real-World Example

```java
@Configuration
@EnableBatchProcessing
public class CsvToDatabaseBatchConfig {
    
    @Bean
    public Job csvToDatabaseJob() {
        return jobBuilderFactory.get("csvToDatabaseJob")
            .start(csvToDatabaseStep())
            .listener(jobCompletionListener())
            .build();
    }
    
    @Bean
    public Step csvToDatabaseStep() {
        return stepBuilderFactory.get("csvToDatabaseStep")
            .<User, User>chunk(1000)
            .reader(csvReader())
            .processor(userProcessor())
            .writer(databaseWriter())
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(100)
            .listener(stepListener())
            .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<User> csvReader() {
        return new FlatFileItemReaderBuilder<User>()
            .name("userReader")
            .resource(new FileSystemResource("input/users.csv"))
            .delimited()
            .names("id", "firstName", "lastName", "email", "age")
            .linesToSkip(1)
            .fieldSetMapper(fieldSet -> {
                User user = new User();
                user.setId(fieldSet.readLong("id"));
                user.setFirstName(fieldSet.readString("firstName"));
                user.setLastName(fieldSet.readString("lastName"));
                user.setEmail(fieldSet.readString("email"));
                user.setAge(fieldSet.readInt("age"));
                return user;
            })
            .build();
    }
    
    @Bean
    public ItemProcessor<User, User> userProcessor() {
        return user -> {
            if (user.getEmail() == null || !user.getEmail().contains("@")) {
                throw new ValidationException("Invalid email");
            }
            
            user.setFullName(user.getFirstName() + " " + user.getLastName());
            user.setEmail(user.getEmail().toLowerCase());
            user.setCreatedDate(LocalDateTime.now());
            
            return user;
        };
    }
    
    @Bean
    public JdbcBatchItemWriter<User> databaseWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<User>()
            .dataSource(dataSource)
            .sql("INSERT INTO users (id, full_name, email, age, created_date) " +
                 "VALUES (:id, :fullName, :email, :age, :createdDate)")
            .beanMapped()
            .build();
    }
}
```

---

[← Previous: Part 1](12_Spring_Batch_Part1.md) | [Next: Spring Integration →](14_Spring_Integration.md)
