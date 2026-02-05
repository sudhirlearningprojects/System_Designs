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

**What it does**: Enables Spring Batch infrastructure

**Provides**:
- JobRepository (metadata storage)
- JobLauncher (job execution)
- JobBuilderFactory (job creation)
- StepBuilderFactory (step creation)
- JobExplorer (query job metadata)
- JobRegistry (job registration)

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // All batch infrastructure beans auto-configured
}
```

**What happens behind the scenes**:
```java
// Spring creates these beans automatically
@Bean
public JobRepository jobRepository() { }

@Bean
public JobLauncher jobLauncher() { }

@Bean
public JobBuilderFactory jobBuilderFactory(JobRepository jobRepository) { }

@Bean
public StepBuilderFactory stepBuilderFactory(JobRepository jobRepository, 
                                             PlatformTransactionManager transactionManager) { }
```

### @StepScope

**What**: Late binding of beans at step execution time

**Why**: Access job parameters, step context

**When**: ItemReader, ItemWriter, ItemProcessor need runtime values

```java
@Bean
@StepScope
public FlatFileItemReader<User> reader(
        @Value("#{jobParameters['inputFile']}") String inputFile,
        @Value("#{jobParameters['date']}") String date) {
    
    System.out.println("Creating reader for file: " + inputFile);
    
    return new FlatFileItemReaderBuilder<User>()
        .name("userReader")
        .resource(new FileSystemResource(inputFile))
        .delimited()
        .names("id", "name", "email")
        .targetType(User.class)
        .build();
}

// Without @StepScope - ERROR!
@Bean
public FlatFileItemReader<User> reader(
        @Value("#{jobParameters['inputFile']}") String inputFile) {
    // jobParameters not available at bean creation time
    // Throws exception
}
```

**Accessing Step Context**:
```java
@Bean
@StepScope
public ItemProcessor<User, User> processor(
        @Value("#{stepExecutionContext['minAge']}") Integer minAge) {
    return user -> user.getAge() >= minAge ? user : null;
}
```

### @JobScope

**What**: Late binding at job execution time

**Difference from @StepScope**: Created once per job, not per step

```java
@Bean
@JobScope
public JobExecutionListener listener(
        @Value("#{jobParameters['notifyEmail']}") String email) {
    return new JobExecutionListener() {
        @Override
        public void afterJob(JobExecution jobExecution) {
            sendEmail(email, jobExecution.getStatus());
        }
    };
}
```

### Lifecycle Annotations

#### Job Lifecycle

```java
@Component
public class JobListener implements JobExecutionListener {
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("Job started: " + jobExecution.getJobInstance().getJobName());
        System.out.println("Job ID: " + jobExecution.getId());
        System.out.println("Parameters: " + jobExecution.getJobParameters());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("Job finished with status: " + jobExecution.getStatus());
        System.out.println("Duration: " + 
            Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()));
        
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            System.out.println("Job completed successfully");
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            System.out.println("Job failed: " + jobExecution.getAllFailureExceptions());
        }
    }
}
```

#### Step Lifecycle

```java
@Component
public class StepListener implements StepExecutionListener {
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("Step started: " + stepExecution.getStepName());
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        System.out.println("Records read: " + stepExecution.getReadCount());
        System.out.println("Records written: " + stepExecution.getWriteCount());
        System.out.println("Records skipped: " + stepExecution.getSkipCount());
        System.out.println("Commit count: " + stepExecution.getCommitCount());
        
        return stepExecution.getExitStatus();
    }
}
```

#### Chunk Lifecycle

```java
@Component
public class ChunkListener implements ChunkListener {
    
    @Override
    public void beforeChunk(ChunkContext context) {
        System.out.println("Starting chunk");
    }
    
    @Override
    public void afterChunk(ChunkContext context) {
        System.out.println("Chunk completed");
    }
    
    @Override
    public void afterChunkError(ChunkContext context) {
        System.out.println("Chunk failed");
    }
}
```

#### Item Lifecycle

```java
@Component
public class ItemListener implements ItemReadListener<User>, 
                                     ItemProcessListener<User, User>,
                                     ItemWriteListener<User> {
    
    // Read listeners
    @Override
    public void beforeRead() { }
    
    @Override
    public void afterRead(User item) {
        System.out.println("Read: " + item);
    }
    
    @Override
    public void onReadError(Exception ex) {
        System.err.println("Read error: " + ex.getMessage());
    }
    
    // Process listeners
    @Override
    public void beforeProcess(User item) { }
    
    @Override
    public void afterProcess(User item, User result) {
        if (result == null) {
            System.out.println("Item filtered: " + item);
        }
    }
    
    @Override
    public void onProcessError(User item, Exception e) {
        System.err.println("Process error for " + item + ": " + e.getMessage());
    }
    
    // Write listeners
    @Override
    public void beforeWrite(List<? extends User> items) {
        System.out.println("Writing " + items.size() + " items");
    }
    
    @Override
    public void afterWrite(List<? extends User> items) { }
    
    @Override
    public void onWriteError(Exception exception, List<? extends User> items) {
        System.err.println("Write error: " + exception.getMessage());
    }
}
```

### Annotation-Based Listeners

```java
@Component
public class AnnotationBasedListener {
    
    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("Job starting");
    }
    
    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        System.out.println("Job finished");
    }
    
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("Step starting");
    }
    
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }
    
    @BeforeChunk
    public void beforeChunk(ChunkContext context) { }
    
    @AfterChunk
    public void afterChunk(ChunkContext context) { }
    
    @BeforeRead
    public void beforeRead() { }
    
    @AfterRead
    public void afterRead(Object item) { }
    
    @OnReadError
    public void onReadError(Exception ex) { }
    
    @BeforeProcess
    public void beforeProcess(Object item) { }
    
    @AfterProcess
    public void afterProcess(Object item, Object result) { }
    
    @OnProcessError
    public void onProcessError(Object item, Exception e) { }
    
    @BeforeWrite
    public void beforeWrite(List<?> items) { }
    
    @AfterWrite
    public void afterWrite(List<?> items) { }
    
    @OnWriteError
    public void onWriteError(Exception exception, List<?> items) { }
}
```

---

## Job Configuration

### Understanding Job

**Job**: Container for steps that defines batch process

**Components**:
- Name (unique identifier)
- Steps (execution units)
- JobParameters (runtime inputs)
- Listeners (lifecycle hooks)
- Incrementer (parameter generation)
- Validator (parameter validation)

### Basic Job

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
            .build();
    }
}
```

### Job with Incrementer

**Why**: Generate unique job parameters for each run

```java
@Bean
public Job importUserJob(Step step1) {
    return jobBuilderFactory.get("importUserJob")
        .start(step1)
        .incrementer(new RunIdIncrementer()) // Adds run.id parameter
        .build();
}

// Custom incrementer
public class TimestampIncrementer implements JobParametersIncrementer {
    @Override
    public JobParameters getNext(JobParameters parameters) {
        return new JobParametersBuilder(parameters)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
    }
}
```

### Job with Listener

```java
@Bean
public Job importUserJob(Step step1) {
    return jobBuilderFactory.get("importUserJob")
        .start(step1)
        .listener(jobExecutionListener())
        .build();
}

@Bean
public JobExecutionListener jobExecutionListener() {
    return new JobExecutionListener() {
        @Override
        public void beforeJob(JobExecution jobExecution) {
            System.out.println("Job started at: " + LocalDateTime.now());
        }
        
        @Override
        public void afterJob(JobExecution jobExecution) {
            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                System.out.println("Job completed successfully");
            } else {
                System.out.println("Job failed");
            }
        }
    };
}
```

### Job with Validator

```java
@Bean
public Job importUserJob(Step step1) {
    return jobBuilderFactory.get("importUserJob")
        .start(step1)
        .validator(jobParametersValidator())
        .build();
}

@Bean
public JobParametersValidator jobParametersValidator() {
    return new JobParametersValidator() {
        @Override
        public void validate(JobParameters parameters) throws JobParametersInvalidException {
            String inputFile = parameters.getString("inputFile");
            if (inputFile == null || inputFile.isEmpty()) {
                throw new JobParametersInvalidException("inputFile parameter is required");
            }
            
            if (!new File(inputFile).exists()) {
                throw new JobParametersInvalidException("File does not exist: " + inputFile);
            }
        }
    };
}

// Or use DefaultJobParametersValidator
@Bean
public JobParametersValidator validator() {
    DefaultJobParametersValidator validator = new DefaultJobParametersValidator();
    validator.setRequiredKeys(new String[]{"inputFile", "date"});
    validator.setOptionalKeys(new String[]{"outputFile"});
    return validator;
}
```

### Multi-Step Job (Sequential)

```java
@Bean
public Job multiStepJob() {
    return jobBuilderFactory.get("multiStepJob")
        .start(extractStep())
        .next(transformStep())
        .next(loadStep())
        .build();
}

@Bean
public Step extractStep() {
    return stepBuilderFactory.get("extractStep")
        .tasklet((contribution, chunkContext) -> {
            System.out.println("Extracting data...");
            return RepeatStatus.FINISHED;
        })
        .build();
}

@Bean
public Step transformStep() {
    return stepBuilderFactory.get("transformStep")
        .tasklet((contribution, chunkContext) -> {
            System.out.println("Transforming data...");
            return RepeatStatus.FINISHED;
        })
        .build();
}

@Bean
public Step loadStep() {
    return stepBuilderFactory.get("loadStep")
        .tasklet((contribution, chunkContext) -> {
            System.out.println("Loading data...");
            return RepeatStatus.FINISHED;
        })
        .build();
}
```

### Conditional Flow

```java
@Bean
public Job conditionalJob() {
    return jobBuilderFactory.get("conditionalJob")
        .start(validationStep())
        .on("FAILED").to(errorHandlingStep())
        .from(validationStep())
        .on("COMPLETED").to(processingStep())
        .from(processingStep())
        .on("*").to(cleanupStep())
        .end()
        .build();
}

// Custom exit status
@Bean
public Step validationStep() {
    return stepBuilderFactory.get("validationStep")
        .tasklet((contribution, chunkContext) -> {
            boolean isValid = validateData();
            if (!isValid) {
                contribution.setExitStatus(new ExitStatus("FAILED"));
            }
            return RepeatStatus.FINISHED;
        })
        .build();
}
```

### Parallel Steps (Split)

```java
@Bean
public Job parallelJob() {
    Flow flow1 = new FlowBuilder<Flow>("flow1")
        .start(step1())
        .build();
    
    Flow flow2 = new FlowBuilder<Flow>("flow2")
        .start(step2())
        .build();
    
    return jobBuilderFactory.get("parallelJob")
        .start(flow1)
        .split(new SimpleAsyncTaskExecutor())
        .add(flow2)
        .end()
        .build();
}
```

### Decision-Based Flow

```java
@Bean
public Job decisionJob() {
    return jobBuilderFactory.get("decisionJob")
        .start(step1())
        .next(decider())
        .on("WEEKDAY").to(weekdayStep())
        .from(decider())
        .on("WEEKEND").to(weekendStep())
        .end()
        .build();
}

@Bean
public JobExecutionDecider decider() {
    return (jobExecution, stepExecution) -> {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return new FlowExecutionStatus("WEEKEND");
        }
        return new FlowExecutionStatus("WEEKDAY");
    };
}
```

### Job Restart Configuration

```java
@Bean
public Job restartableJob() {
    return jobBuilderFactory.get("restartableJob")
        .start(step1())
        .next(step2())
        .preventRestart() // Disable restart
        .build();
}

// Or allow restart with limit
@Bean
public Step step1() {
    return stepBuilderFactory.get("step1")
        .<User, User>chunk(100)
        .reader(reader())
        .writer(writer())
        .startLimit(3) // Max 3 restart attempts
        .build();
}
```

---

## Step Configuration

### Understanding Step

**Step**: Independent unit of work in a job

**Types**:
1. **Chunk-oriented**: Read-Process-Write pattern
2. **Tasklet**: Single operation

### Chunk-Oriented Step

```java
@Bean
public Step chunkStep(ItemReader<User> reader,
                      ItemProcessor<User, User> processor,
                      ItemWriter<User> writer) {
    return stepBuilderFactory.get("chunkStep")
        .<User, User>chunk(100)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
}
```

### Tasklet Step

```java
@Bean
public Step taskletStep() {
    return stepBuilderFactory.get("taskletStep")
        .tasklet((contribution, chunkContext) -> {
            System.out.println("Executing tasklet");
            return RepeatStatus.FINISHED;
        })
        .build();
}
```

### Fault Tolerance

```java
@Bean
public Step faultTolerantStep() {
    return stepBuilderFactory.get("faultTolerantStep")
        .<User, User>chunk(100)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .faultTolerant()
        .skipLimit(10)
        .skip(ValidationException.class)
        .retryLimit(3)
        .retry(NetworkException.class)
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
