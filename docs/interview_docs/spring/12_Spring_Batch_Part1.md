# Spring Batch - Part 1: Core Concepts & Configuration

[← Back to Index](README.md) | [← Previous: Spring AOP](11_Spring_AOP.md) | [Next: Part 2 - Readers & Writers →](13_Spring_Batch_Part2.md)

## Table of Contents
- [Theory: Understanding Batch Processing](#theory-understanding-batch-processing)
- [What is Spring Batch?](#what-is-spring-batch)
- [Core Annotations](#core-annotations)
- [Job Configuration](#job-configuration)
- [Step Configuration](#step-configuration)

---

## Theory: Understanding Batch Processing

### What is Batch Processing?

Batch processing is **processing large volumes of data** in groups (batches) without user interaction.

**Characteristics**:
- Processes millions of records
- Runs during off-peak hours
- Long-running operations
- No real-time user interaction
- Scheduled or triggered

### Batch vs Real-Time Processing

| Aspect | Batch Processing | Real-Time Processing |
|--------|-----------------|---------------------|
| Timing | Scheduled/Periodic | Immediate |
| Volume | Large datasets | Individual records |
| Latency | Minutes to hours | Milliseconds |
| User Interaction | None | Required |
| Examples | Payroll, Reports | API requests, Payments |

### Common Use Cases

**1. ETL (Extract, Transform, Load)**
- Extract data from source systems
- Transform/clean data
- Load into data warehouse

**2. Report Generation**
- Daily/monthly reports
- Aggregate calculations
- PDF/Excel generation

**3. Data Migration**
- Move data between systems
- Database upgrades
- Cloud migrations

**4. Bulk Operations**
- Send bulk emails
- Process invoices
- Update inventory

### Spring Batch Architecture

```
Job (Container)
  │
  ├─ Step 1 (Independent unit of work)
  │    │
  │    ├─ ItemReader (Read data)
  │    │
  │    ├─ ItemProcessor (Transform data)
  │    │
  │    └─ ItemWriter (Write data)
  │
  ├─ Step 2
  │
  └─ Step 3

JobRepository (Metadata storage)
  - Job execution history
  - Step status
  - Restart capability
```

### Core Components

**1. Job**
- Container for steps
- Defines execution flow
- Can have multiple steps

**2. Step**
- Independent unit of work
- Contains Reader, Processor, Writer
- Can be chunk-based or tasklet-based

**3. ItemReader**
- Reads data from source
- One item at a time
- Returns null when done

**4. ItemProcessor**
- Transforms/validates data
- Optional (can skip)
- Return null to filter item

**5. ItemWriter**
- Writes data to destination
- Receives chunk of items
- Batch write for performance

**6. JobRepository**
- Stores job metadata
- Enables restart/recovery
- Tracks execution status

**7. JobLauncher**
- Starts job execution
- Passes job parameters
- Returns JobExecution

### Chunk-Oriented Processing

```
Read 100 items (chunk size)
  ↓
Process each item
  ↓
Write all 100 items in one transaction
  ↓
Commit transaction
  ↓
Repeat until no more data
```

**Benefits**:
- Efficient (batch writes)
- Transactional (all or nothing per chunk)
- Restartable (tracks progress)

### Job Execution Flow

```
1. JobLauncher.run(job, parameters)
2. JobRepository creates JobExecution
3. For each Step:
   a. Create StepExecution
   b. Execute Step
   c. Update StepExecution status
4. Update JobExecution status
5. Return JobExecution
```

### Restart & Recovery

Spring Batch supports **automatic restart**:
- Tracks last successful chunk
- Skips already processed data
- Continues from failure point
- Configurable retry logic

### Performance Considerations

**Chunk Size**:
- Too small: Many transactions, slow
- Too large: Memory issues, long rollback
- Typical: 100-1000 items

**Parallel Processing**:
- Multi-threaded steps
- Partitioning (split data)
- Remote chunking (distributed)

**Optimization**:
- Use batch inserts
- Minimize database calls
- Use pagination for reads
- Configure connection pools

---

## What is Spring Batch?

Spring Batch is a framework for **batch processing** - processing large volumes of data in chunks.

**Use Cases**: ETL operations, Report generation, Data migration, Bulk email sending, File processing

---

## Core Annotations

### @EnableBatchProcessing
```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // Provides: JobRepository, JobLauncher, JobBuilderFactory, StepBuilderFactory
}
```

### @StepScope & @JobScope
```java
@Bean
@StepScope
public FlatFileItemReader<User> reader(
        @Value("#{jobParameters['inputFile']}") String inputFile) {
    return new FlatFileItemReaderBuilder<User>()
        .resource(new FileSystemResource(inputFile))
        .delimited()
        .names("id", "name", "email")
        .targetType(User.class)
        .build();
}
```

### Lifecycle Annotations
```java
@Component
public class JobListener implements JobExecutionListener {
    
    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("Job started: " + jobExecution.getJobInstance().getJobName());
    }
    
    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        System.out.println("Job finished with status: " + jobExecution.getStatus());
    }
}

@Component
public class StepListener implements StepExecutionListener {
    
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("Step started");
    }
    
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        System.out.println("Records read: " + stepExecution.getReadCount());
        return stepExecution.getExitStatus();
    }
}
```

---

## Job Configuration

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job importUserJob(Step step1) {
        return jobBuilderFactory.get("importUserJob")
            .start(step1)
            .incrementer(new RunIdIncrementer())
            .listener(jobExecutionListener())
            .build();
    }
}
```

---

## Step Configuration

```java
@Bean
public Step step1(ItemReader<User> reader,
                 ItemProcessor<User, User> processor,
                 ItemWriter<User> writer) {
    return stepBuilderFactory.get("step1")
        .<User, User>chunk(100) // Process 100 records at a time
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skipLimit(10)
        .skip(ValidationException.class)
        .retryLimit(3)
        .retry(NetworkException.class)
        .listener(stepExecutionListener())
        .build();
}
```

---

## Multi-Step Job

```java
@Bean
public Job multiStepJob() {
    return jobBuilderFactory.get("multiStepJob")
        .start(extractStep())
        .next(transformStep())
        .next(loadStep())
        .build();
}

// Conditional flow
@Bean
public Job conditionalJob() {
    return jobBuilderFactory.get("conditionalJob")
        .start(validationStep())
        .on("FAILED").to(errorHandlingStep())
        .from(validationStep())
        .on("COMPLETED").to(processingStep())
        .end()
        .build();
}
```

---

## Job Parameters

```java
@Component
public class UserProcessor implements ItemProcessor<User, User> {
    
    @Value("#{jobParameters['date']}")
    private String date;
    
    @Value("#{jobParameters['minAge']}")
    private Integer minAge;
    
    @Override
    public User process(User user) {
        if (user.getAge() < minAge) {
            return null;
        }
        return user;
    }
}

// Run job with parameters
JobParameters params = new JobParametersBuilder()
    .addString("date", LocalDate.now().toString())
    .addLong("minAge", 18L)
    .addLong("time", System.currentTimeMillis())
    .toJobParameters();

jobLauncher.run(importUserJob, params);
```

---

[← Previous: Spring AOP](11_Spring_AOP.md) | [Next: Part 2 - Readers & Writers →](13_Spring_Batch_Part2.md)
