# Spring Batch - Part 2: Readers, Processors & Writers

[← Back to Index](README.md) | [← Previous: Part 1](12_Spring_Batch_Part1.md) | [Next: Spring Integration →](14_Spring_Integration.md)

## Table of Contents
- [ItemReader](#itemreader)
- [ItemProcessor](#itemprocessor)
- [ItemWriter](#itemwriter)
- [Real-World Example](#real-world-example)

---

## ItemReader

### CSV Reader
```java
@Bean
@StepScope
public FlatFileItemReader<User> csvReader(
        @Value("#{jobParameters['inputFile']}") String inputFile) {
    return new FlatFileItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource(inputFile))
        .delimited()
        .names("id", "name", "email", "age")
        .linesToSkip(1)
        .targetType(User.class)
        .build();
}
```

### Database Reader
```java
@Bean
public JdbcCursorItemReader<User> jdbcReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<User>()
        .name("userReader")
        .dataSource(dataSource)
        .sql("SELECT id, name, email, age FROM users WHERE active = true")
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
        .queryString("SELECT u FROM User u WHERE u.active = true")
        .pageSize(100)
        .build();
}
```

---

## ItemProcessor

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

// Composite processor
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
```

---

## ItemWriter

### JDBC Writer
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

### File Writer
```java
@Bean
public FlatFileItemWriter<User> csvWriter() {
    return new FlatFileItemWriterBuilder<User>()
        .name("userWriter")
        .resource(new FileSystemResource("output/users.csv"))
        .delimited()
        .names("id", "name", "email", "age", "status")
        .headerCallback(writer -> writer.write("ID,Name,Email,Age,Status"))
        .build();
}
```

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
