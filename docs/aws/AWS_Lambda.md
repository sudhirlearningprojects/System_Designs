# AWS Lambda - Complete Guide

## Table of Contents
1. [Introduction to Lambda](#introduction)
2. [Core Concepts](#core-concepts)
3. [Creating Lambda Functions](#creating-functions)
4. [Lambda Triggers & Event Sources](#triggers)
5. [Java/Spring Boot Integration](#java-integration)
6. [Container-Based Lambda](#container-lambda)
7. [IAM Roles & Permissions](#iam-permissions)
8. [Best Practices](#best-practices)
9. [Monitoring & Debugging](#monitoring)
10. [Lambda Lifecycle Stages](#lifecycle)
11. [Lambda Limitations - Deep Dive](#limitations)
12. [Development & Deployment Methods - Deep Dive](#development-deployment)

---

## 1. Introduction to Lambda {#introduction}

AWS Lambda is a serverless compute service that runs code in response to events without provisioning or managing servers.

### Key Features
- **Serverless**: No server management required
- **Auto-scaling**: Automatically scales from zero to thousands of concurrent executions
- **Pay-per-use**: Charged only for compute time consumed
- **Event-driven**: Triggered by AWS services or HTTP requests
- **Multiple runtimes**: Java, Python, Node.js, Go, .NET, Ruby, Custom Runtime

### Use Cases
- Real-time file processing (S3 uploads)
- Stream processing (Kinesis, DynamoDB Streams)
- API backends (API Gateway + Lambda)
- Scheduled tasks (EventBridge cron)
- Data transformation and ETL
- IoT backends
- Chatbots and Alexa skills

### Pricing
- **Requests**: $0.20 per 1M requests
- **Duration**: $0.0000166667 per GB-second
- **Free Tier**: 1M requests + 400,000 GB-seconds per month

---

## 2. Core Concepts {#core-concepts}

### Execution Model
- **Cold Start**: First invocation or after idle period (100ms - 10s)
- **Warm Start**: Subsequent invocations reuse execution environment (<1ms)
- **Concurrency**: Number of simultaneous executions
- **Timeout**: Max 15 minutes (900 seconds)

### Memory & CPU
- Memory: 128 MB to 10,240 MB (10 GB)
- CPU scales proportionally with memory
- At 1,769 MB = 1 vCPU
- At 10,240 MB = 6 vCPUs

### Execution Environment
- **Ephemeral storage**: /tmp directory (512 MB - 10 GB)
- **Environment variables**: Configuration data
- **Layers**: Shared code and dependencies (up to 5 layers)
- **Extensions**: Integrate monitoring, security, governance tools

### Invocation Types
1. **Synchronous**: Caller waits for response (API Gateway, SDK)
2. **Asynchronous**: Lambda queues request, returns immediately (S3, SNS)
3. **Event Source Mapping**: Lambda polls source (SQS, Kinesis, DynamoDB Streams)

---

## 3. Creating Lambda Functions {#creating-functions}

### 3.1 Console-Based Creation

**Step 1: Create Function**
```
AWS Console → Lambda → Create function
- Function name: MyLambdaFunction
- Runtime: Java 17
- Architecture: x86_64 or arm64
- Permissions: Create new role with basic Lambda permissions
```

**Step 2: Configure Function**
```
Configuration → General configuration
- Memory: 512 MB
- Timeout: 30 seconds
- Ephemeral storage: 512 MB
```

**Step 3: Add Environment Variables**
```
Configuration → Environment variables
- DB_HOST: mydb.amazonaws.com
- DB_NAME: mydb
- API_KEY: encrypted-value
```

### 3.2 AWS CLI Creation

```bash
# Create function from ZIP file
aws lambda create-function \
  --function-name MyLambdaFunction \
  --runtime java17 \
  --role arn:aws:iam::123456789012:role/lambda-execution-role \
  --handler com.example.Handler::handleRequest \
  --zip-file fileb://function.zip \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables={DB_HOST=mydb.amazonaws.com,DB_NAME=mydb}

# Update function code
aws lambda update-function-code \
  --function-name MyLambdaFunction \
  --zip-file fileb://function.zip

# Update function configuration
aws lambda update-function-configuration \
  --function-name MyLambdaFunction \
  --timeout 60 \
  --memory-size 1024

# Invoke function
aws lambda invoke \
  --function-name MyLambdaFunction \
  --payload '{"key":"value"}' \
  response.json
```

### 3.3 Java SDK Creation

```java
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.core.SdkBytes;

public class LambdaManager {
    
    private final LambdaClient lambdaClient;
    
    public LambdaManager() {
        this.lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }
    
    // Create Lambda function
    public String createFunction(String functionName, String roleArn, byte[] zipFile) {
        FunctionCode code = FunctionCode.builder()
                .zipFile(SdkBytes.fromByteArray(zipFile))
                .build();
        
        CreateFunctionRequest request = CreateFunctionRequest.builder()
                .functionName(functionName)
                .runtime(Runtime.JAVA17)
                .role(roleArn)
                .handler("com.example.Handler::handleRequest")
                .code(code)
                .timeout(30)
                .memorySize(512)
                .build();
        
        CreateFunctionResponse response = lambdaClient.createFunction(request);
        return response.functionArn();
    }
    
    // Invoke Lambda function synchronously
    public String invokeFunction(String functionName, String payload) {
        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();
        
        InvokeResponse response = lambdaClient.invoke(request);
        return response.payload().asUtf8String();
    }
    
    // Invoke Lambda function asynchronously
    public void invokeFunctionAsync(String functionName, String payload) {
        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();
        
        lambdaClient.invoke(request);
    }
    
    // Update function code
    public void updateFunctionCode(String functionName, byte[] zipFile) {
        UpdateFunctionCodeRequest request = UpdateFunctionCodeRequest.builder()
                .functionName(functionName)
                .zipFile(SdkBytes.fromByteArray(zipFile))
                .build();
        
        lambdaClient.updateFunctionCode(request);
    }
    
    // Delete function
    public void deleteFunction(String functionName) {
        DeleteFunctionRequest request = DeleteFunctionRequest.builder()
                .functionName(functionName)
                .build();
        
        lambdaClient.deleteFunction(request);
    }
}
```

### 3.4 CloudFormation Template

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  MyLambdaFunction:
    Type: AWS::Lambda::Function
    Properties:
      FunctionName: MyLambdaFunction
      Runtime: java17
      Handler: com.example.Handler::handleRequest
      Role: !GetAtt LambdaExecutionRole.Arn
      Code:
        S3Bucket: my-lambda-code-bucket
        S3Key: function.zip
      Timeout: 30
      MemorySize: 512
      Environment:
        Variables:
          DB_HOST: mydb.amazonaws.com
          DB_NAME: mydb
      
  LambdaExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Principal:
              Service: lambda.amazonaws.com
            Action: sts:AssumeRole
      ManagedPolicyArns:
        - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

---

## 4. Lambda Triggers & Event Sources {#triggers}

### 4.1 S3 Trigger

**Console Setup:**
```
S3 → Bucket → Properties → Event notifications → Create event notification
- Event types: All object create events
- Destination: Lambda function
- Lambda function: MyLambdaFunction
```

**Lambda Handler:**
```java
public class S3EventHandler implements RequestHandler<S3Event, String> {
    
    private final S3Client s3Client = S3Client.create();
    
    @Override
    public String handleRequest(S3Event event, Context context) {
        event.getRecords().forEach(record -> {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();
            
            context.getLogger().log("Processing file: " + bucket + "/" + key);
            
            // Download and process file
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            byte[] content = response.asByteArray();
            
            // Process content
            processFile(content);
        });
        
        return "Success";
    }
}
```

### 4.2 API Gateway Trigger

**Console Setup:**
```
API Gateway → Create API → REST API
- Create resource: /users
- Create method: POST
- Integration type: Lambda Function
- Lambda Function: MyLambdaFunction
- Deploy API
```

**Lambda Handler:**
```java
public class ApiGatewayHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String body = request.getBody();
            User user = objectMapper.readValue(body, User.class);
            
            // Process user
            String result = processUser(user);
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(objectMapper.writeValueAsString(Map.of("message", result)));
                    
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
```

### 4.3 SQS Trigger

**Console Setup:**
```
Lambda → Add trigger → SQS
- SQS queue: my-queue
- Batch size: 10
- Batch window: 5 seconds
```

**Lambda Handler:**
```java
public class SQSEventHandler implements RequestHandler<SQSEvent, String> {
    
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            String body = message.getBody();
            context.getLogger().log("Processing message: " + body);
            
            try {
                processMessage(body);
            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
                throw new RuntimeException("Failed to process message", e);
            }
        }
        return "Success";
    }
}
```

### 4.4 DynamoDB Streams Trigger

**Console Setup:**
```
DynamoDB → Table → Exports and streams → DynamoDB stream details → Enable
Lambda → Add trigger → DynamoDB
- DynamoDB table: my-table
- Batch size: 100
- Starting position: LATEST
```

**Lambda Handler:**
```java
public class DynamoDBStreamHandler implements RequestHandler<DynamodbEvent, String> {
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            String eventName = record.getEventName();
            
            if ("INSERT".equals(eventName)) {
                Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
                handleInsert(newImage);
            } else if ("MODIFY".equals(eventName)) {
                Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
                Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
                handleUpdate(oldImage, newImage);
            } else if ("REMOVE".equals(eventName)) {
                Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
                handleDelete(oldImage);
            }
        }
        return "Success";
    }
}
```

### 4.5 EventBridge (CloudWatch Events) Trigger

**Console Setup:**
```
EventBridge → Rules → Create rule
- Event source: Schedule
- Schedule pattern: cron(0 12 * * ? *) // Daily at 12 PM UTC
- Target: Lambda function
- Function: MyLambdaFunction
```

**Lambda Handler:**
```java
public class ScheduledEventHandler implements RequestHandler<ScheduledEvent, String> {
    
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        context.getLogger().log("Scheduled event triggered at: " + event.getTime());
        
        // Perform scheduled task
        performDailyTask();
        
        return "Success";
    }
}
```

### 4.6 SNS Trigger

**Console Setup:**
```
SNS → Topics → Create topic → Standard
Lambda → Add trigger → SNS
- SNS topic: my-topic
```

**Lambda Handler:**
```java
public class SNSEventHandler implements RequestHandler<SNSEvent, String> {
    
    @Override
    public String handleRequest(SNSEvent event, Context context) {
        for (SNSEvent.SNSRecord record : event.getRecords()) {
            String message = record.getSNS().getMessage();
            String subject = record.getSNS().getSubject();
            
            context.getLogger().log("Received SNS message: " + subject);
            processNotification(message);
        }
        return "Success";
    }
}
```

### 4.7 Kinesis Trigger

**Lambda Handler:**
```java
public class KinesisEventHandler implements RequestHandler<KinesisEvent, String> {
    
    @Override
    public String handleRequest(KinesisEvent event, Context context) {
        for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
            byte[] data = record.getKinesis().getData().array();
            String payload = new String(data, StandardCharsets.UTF_8);
            
            context.getLogger().log("Processing Kinesis record: " + payload);
            processStreamData(payload);
        }
        return "Success";
    }
}
```

### 4.8 ALB (Application Load Balancer) Trigger

**Lambda Handler:**
```java
public class ALBHandler implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    
    @Override
    public ApplicationLoadBalancerResponseEvent handleRequest(ApplicationLoadBalancerRequestEvent request, Context context) {
        String path = request.getPath();
        String method = request.getHttpMethod();
        
        ApplicationLoadBalancerResponseEvent response = new ApplicationLoadBalancerResponseEvent();
        response.setStatusCode(200);
        response.setStatusDescription("200 OK");
        response.setHeaders(Map.of("Content-Type", "application/json"));
        response.setBody("{\"message\":\"Hello from Lambda\"}");
        
        return response;
    }
}
```

---

## 5. Java/Spring Boot Integration {#java-integration}

### Maven Dependencies

```xml
<dependencies>
    <!-- AWS Lambda Java Core -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-core</artifactId>
        <version>1.2.3</version>
    </dependency>
    
    <!-- AWS Lambda Java Events -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-events</artifactId>
        <version>3.11.3</version>
    </dependency>
    
    <!-- AWS SDK v2 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>lambda</artifactId>
        <version>2.20.26</version>
    </dependency>
    
    <!-- Spring Cloud Function -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-function-adapter-aws</artifactId>
        <version>4.0.5</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Basic Lambda Handler

```java
package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class HelloWorldHandler implements RequestHandler<Map<String, Object>, String> {
    
    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        context.getLogger().log("Input: " + input);
        return "Hello, " + input.get("name");
    }
}
```

### POJO-Based Handler

```java
public class User {
    private String id;
    private String name;
    private String email;
    // getters and setters
}

public class UserHandler implements RequestHandler<User, String> {
    
    @Override
    public String handleRequest(User user, Context context) {
        context.getLogger().log("Processing user: " + user.getName());
        
        // Business logic
        saveUser(user);
        
        return "User created: " + user.getId();
    }
}
```

### Spring Cloud Function

```java
@SpringBootApplication
public class LambdaApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LambdaApplication.class, args);
    }
    
    @Bean
    public Function<User, String> createUser() {
        return user -> {
            // Business logic
            return "User created: " + user.getId();
        };
    }
    
    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> apiHandler() {
        return request -> {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody("{\"message\":\"Success\"}");
            return response;
        };
    }
}
```

### Building and Deploying

```bash
# Build JAR
mvn clean package

# Deploy to Lambda
aws lambda update-function-code \
  --function-name MyLambdaFunction \
  --zip-file fileb://target/lambda-function.jar

# Test locally with SAM
sam local invoke MyLambdaFunction -e event.json
```

---

## 6. Container-Based Lambda {#container-lambda}

### Dockerfile for Java Lambda

```dockerfile
FROM public.ecr.aws/lambda/java:17

# Copy function code
COPY target/lambda-function.jar ${LAMBDA_TASK_ROOT}/lib/

# Set handler
CMD ["com.example.Handler::handleRequest"]
```

### Build and Push to ECR

```bash
# Build Docker image
docker build -t my-lambda-function .

# Create ECR repository
aws ecr create-repository --repository-name my-lambda-function

# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Tag image
docker tag my-lambda-function:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:latest

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:latest

# Create Lambda function from container
aws lambda create-function \
  --function-name MyContainerFunction \
  --package-type Image \
  --code ImageUri=123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:latest \
  --role arn:aws:iam::123456789012:role/lambda-execution-role
```

### Multi-Stage Dockerfile

```dockerfile
# Build stage
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM public.ecr.aws/lambda/java:17
COPY --from=build /app/target/lambda-function.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["com.example.Handler::handleRequest"]
```

---

## 7. IAM Roles & Permissions {#iam-permissions}

### Basic Execution Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### S3 Access Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### DynamoDB Access Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:123456789012:table/MyTable"
    }
  ]
}
```

### VPC Access Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface"
      ],
      "Resource": "*"
    }
  ]
}
```

### Resource-Based Policy (Allow S3 to Invoke Lambda)

```bash
aws lambda add-permission \
  --function-name MyLambdaFunction \
  --statement-id s3-invoke \
  --action lambda:InvokeFunction \
  --principal s3.amazonaws.com \
  --source-arn arn:aws:s3:::my-bucket
```

---

## 8. Best Practices {#best-practices}

### Performance
1. **Minimize cold starts**: Use provisioned concurrency for latency-sensitive apps
2. **Optimize package size**: Remove unused dependencies
3. **Reuse connections**: Initialize SDK clients outside handler
4. **Use environment variables**: For configuration
5. **Leverage /tmp**: For temporary file storage (up to 10 GB)

### Code Example - Connection Reuse

```java
public class OptimizedHandler implements RequestHandler<String, String> {
    
    // Initialize outside handler - reused across invocations
    private static final S3Client s3Client = S3Client.create();
    private static final DynamoDbClient dynamoClient = DynamoDbClient.create();
    
    @Override
    public String handleRequest(String input, Context context) {
        // Use pre-initialized clients
        return processRequest(input);
    }
}
```

### Security
1. **Least privilege IAM**: Grant minimum required permissions
2. **Encrypt environment variables**: Use KMS encryption
3. **Use Secrets Manager**: For sensitive data
4. **Enable VPC**: For private resource access
5. **Validate input**: Always sanitize user input

### Cost Optimization
1. **Right-size memory**: Test different memory configurations
2. **Use ARM architecture**: Graviton2 is 20% cheaper
3. **Set appropriate timeout**: Avoid paying for hung functions
4. **Use reserved concurrency**: Control costs
5. **Monitor unused functions**: Delete or archive

### Reliability
1. **Implement retry logic**: With exponential backoff
2. **Use DLQ**: For failed async invocations
3. **Enable X-Ray**: For distributed tracing
4. **Set concurrency limits**: Prevent downstream throttling
5. **Use aliases and versions**: For safe deployments

---

## 9. Monitoring & Debugging {#monitoring}

### CloudWatch Metrics
- **Invocations**: Number of times function is invoked
- **Duration**: Execution time
- **Errors**: Number of failed invocations
- **Throttles**: Number of throttled invocations
- **ConcurrentExecutions**: Number of concurrent executions
- **DeadLetterErrors**: Failed async invocations sent to DLQ

### CloudWatch Logs

```java
public class LoggingHandler implements RequestHandler<String, String> {
    
    @Override
    public String handleRequest(String input, Context context) {
        LambdaLogger logger = context.getLogger();
        
        logger.log("INFO: Processing request");
        logger.log("Request ID: " + context.getRequestId());
        logger.log("Memory limit: " + context.getMemoryLimitInMB() + " MB");
        logger.log("Remaining time: " + context.getRemainingTimeInMillis() + " ms");
        
        return "Success";
    }
}
```

### X-Ray Tracing

```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-xray-recorder-sdk-core</artifactId>
    <version>2.14.0</version>
</dependency>
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-xray-recorder-sdk-aws-sdk-v2</artifactId>
    <version>2.14.0</version>
</dependency>
```

```java
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

public class XRayHandler implements RequestHandler<String, String> {
    
    @Override
    public String handleRequest(String input, Context context) {
        Subsegment subsegment = AWSXRay.beginSubsegment("CustomOperation");
        try {
            // Your code
            performOperation();
            return "Success";
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }
}
```

### Error Handling

```java
public class ErrorHandler implements RequestHandler<String, String> {
    
    @Override
    public String handleRequest(String input, Context context) {
        try {
            return processRequest(input);
        } catch (ValidationException e) {
            context.getLogger().log("Validation error: " + e.getMessage());
            throw new RuntimeException("Invalid input", e);
        } catch (Exception e) {
            context.getLogger().log("Unexpected error: " + e.getMessage());
            // Send to monitoring system
            sendAlert(e);
            throw new RuntimeException("Processing failed", e);
        }
    }
}
```

### Dead Letter Queue Configuration

```bash
aws lambda update-function-configuration \
  --function-name MyLambdaFunction \
  --dead-letter-config TargetArn=arn:aws:sqs:us-east-1:123456789012:lambda-dlq
```

---

## Additional Resources

- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [AWS Lambda Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/)
- [AWS Lambda Pricing](https://aws.amazon.com/lambda/pricing/)
- [AWS Serverless Application Model (SAM)](https://aws.amazon.com/serverless/sam/)
- [AWS Lambda Power Tuning](https://github.com/alexcasalboni/aws-lambda-power-tuning)


---

## 10. Lambda Lifecycle Stages {#lifecycle}

### 10.1 Complete Lifecycle Overview

AWS Lambda functions go through distinct lifecycle phases from invocation to termination. Understanding these phases is critical for optimization.

```
┌─────────────────────────────────────────────────────────────────┐
│                    LAMBDA LIFECYCLE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. INIT Phase (Cold Start)                                     │
│     ├── Download code package                                   │
│     ├── Start execution environment                             │
│     ├── Load runtime                                            │
│     ├── Run initialization code (outside handler)               │
│     └── Duration: 100ms - 10s (Java: 1-5s typical)             │
│                                                                  │
│  2. INVOKE Phase                                                │
│     ├── Execute handler function                                │
│     ├── Process event                                           │
│     ├── Return response                                         │
│     └── Duration: Based on your code                            │
│                                                                  │
│  3. SHUTDOWN Phase (Optional)                                   │
│     ├── Runtime shutdown hook (if implemented)                  │
│     ├── Cleanup resources                                       │
│     └── Duration: Up to 2 seconds                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 10.2 INIT Phase (Cold Start)

The INIT phase occurs when Lambda creates a new execution environment.

**What Happens:**
1. **Download Code** (50-500ms)
   - Lambda downloads deployment package from S3
   - Larger packages = longer download time
   - Container images: Pull from ECR

2. **Start Execution Environment** (100-200ms)
   - Create isolated execution environment (Firecracker microVM)
   - Allocate memory and CPU
   - Mount /tmp storage

3. **Load Runtime** (50-200ms)
   - Initialize language runtime (JVM, Python interpreter, etc.)
   - Java: JVM startup is slowest (500ms-2s)
   - Python/Node.js: Faster (50-200ms)

4. **Run Initialization Code** (Variable)
   - Code outside handler function
   - Import dependencies
   - Initialize SDK clients
   - Establish database connections
   - Load configuration

**Java Example - Initialization Code:**
```java
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

public class MyHandler implements RequestHandler<Map<String, Object>, String> {
    
    // INIT PHASE: These run during cold start (outside handler)
    private static final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private static final S3Client s3Client = S3Client.create();
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    
    static {
        // Static initialization block - runs during INIT
        System.out.println("Static initialization - INIT phase");
        // Load heavy resources here
    }
    
    // Constructor - runs during INIT
    public MyHandler() {
        System.out.println("Constructor - INIT phase");
    }
    
    // INVOKE PHASE: This runs on every invocation
    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        System.out.println("Handler - INVOKE phase");
        
        // Use pre-initialized clients (no cold start penalty)
        dynamoDb.getItem(/* ... */);
        
        return "Success";
    }
}
```

**Cold Start Duration by Runtime:**
| Runtime | Typical Cold Start | Notes |
|---------|-------------------|-------|
| Python 3.x | 100-300ms | Fastest |
| Node.js 18.x | 150-400ms | Fast |
| Go 1.x | 200-500ms | Compiled, fast |
| Java 17 | 1-5 seconds | JVM startup overhead |
| .NET 6 | 500ms-2s | CLR startup |
| Custom Runtime | Variable | Depends on implementation |

**Factors Affecting Cold Start:**
- **Package Size**: Larger = slower (50MB vs 5MB = 2x slower)
- **Memory Allocation**: More memory = faster CPU = faster init
- **VPC Configuration**: VPC adds 1-3s for ENI creation
- **Dependencies**: More imports = longer initialization
- **Runtime**: Java/C# slower than Python/Node.js

### 10.3 INVOKE Phase

The INVOKE phase executes your handler function.

**What Happens:**
1. Lambda receives invocation request
2. Deserializes event payload
3. Calls handler function
4. Executes your business logic
5. Serializes response
6. Returns response to caller

**Execution Context Reuse:**
```java
public class OptimizedHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    // Reused across invocations (INIT phase)
    private static final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private static int invocationCount = 0;
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        invocationCount++; // Persists across warm invocations
        
        context.getLogger().log("Invocation #" + invocationCount);
        context.getLogger().log("Remaining time: " + context.getRemainingTimeInMillis() + "ms");
        
        // Your business logic
        String result = processRequest(event);
        
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(result);
    }
}
```

**Warm vs Cold Invocations:**
```
Cold Start (First invocation):
INIT (2000ms) + INVOKE (500ms) = 2500ms total

Warm Start (Subsequent invocations):
INVOKE (500ms) = 500ms total

Performance Improvement: 5x faster
```

**Context Object Properties:**
```java
public void logContextInfo(Context context) {
    System.out.println("Function Name: " + context.getFunctionName());
    System.out.println("Function Version: " + context.getFunctionVersion());
    System.out.println("Request ID: " + context.getAwsRequestId());
    System.out.println("Memory Limit: " + context.getMemoryLimitInMB() + "MB");
    System.out.println("Remaining Time: " + context.getRemainingTimeInMillis() + "ms");
    System.out.println("Log Group: " + context.getLogGroupName());
    System.out.println("Log Stream: " + context.getLogStreamName());
}
```

### 10.4 Execution Environment Reuse

Lambda keeps execution environments warm for subsequent invocations.

**Reuse Duration:**
- **Active Period**: 5-15 minutes after last invocation
- **Idle Timeout**: Environment destroyed after inactivity
- **No Guarantee**: AWS may terminate anytime

**What Persists Between Invocations:**
```java
public class PersistenceExample implements RequestHandler<String, String> {
    
    // ✅ Persists across invocations
    private static final S3Client s3 = S3Client.create();
    private static int counter = 0;
    private static Map<String, String> cache = new HashMap<>();
    
    @Override
    public String handleRequest(String input, Context context) {
        counter++; // Increments across warm invocations
        
        // /tmp directory persists (up to 512MB-10GB)
        File tmpFile = new File("/tmp/data.txt");
        if (tmpFile.exists()) {
            System.out.println("Found cached file from previous invocation");
        }
        
        // Cache data in memory
        if (!cache.containsKey(input)) {
            cache.put(input, fetchFromDatabase(input));
        }
        
        return "Invocation #" + counter + ", Cache size: " + cache.size();
    }
}
```

**What Does NOT Persist:**
- Background processes/threads (terminated after handler returns)
- Uncommitted database transactions
- Network connections (may timeout)
- Temporary credentials (expire)

### 10.5 SHUTDOWN Phase

Lambda may invoke a shutdown hook before terminating the environment.

**Runtime Shutdown Hook (Java):**
```java
public class ShutdownHandler implements RequestHandler<String, String> {
    
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    static {
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered - cleaning up resources");
            
            // Graceful shutdown
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            
            // Close database connections
            closeConnections();
            
            System.out.println("Cleanup complete");
        }));
    }
    
    @Override
    public String handleRequest(String input, Context context) {
        // Submit async task
        executor.submit(() -> processAsync(input));
        return "Submitted";
    }
}
```

**Lambda Extensions Shutdown:**
```bash
# Extension receives SHUTDOWN event
{
  "eventType": "SHUTDOWN",
  "shutdownReason": "spindown", # or "timeout" or "failure"
  "deadlineMs": 1234567890
}
```

### 10.6 Provisioned Concurrency

Eliminates cold starts by keeping execution environments initialized.

**Enable Provisioned Concurrency:**
```bash
# AWS CLI
aws lambda put-provisioned-concurrency-config \
  --function-name MyFunction \
  --provisioned-concurrent-executions 10 \
  --qualifier $LATEST

# CloudFormation
Resources:
  ProvisionedConcurrency:
    Type: AWS::Lambda::ProvisionedConcurrencyConfig
    Properties:
      FunctionName: !Ref MyFunction
      ProvisionedConcurrentExecutions: 10
      Qualifier: !Ref MyFunctionVersion
```

**Cost Implications:**
- **On-Demand**: $0.20 per 1M requests + $0.0000166667 per GB-second
- **Provisioned**: $0.0000041667 per GB-second (always running)
- **Example**: 10 instances × 1GB × 30 days = $108/month (before requests)

**When to Use:**
- Latency-sensitive applications (< 100ms requirement)
- Predictable traffic patterns
- High-value transactions (payment processing)
- Real-time APIs

### 10.7 SnapStart (Java Only)

AWS Lambda SnapStart reduces cold start time for Java functions by 90%.

**How It Works:**
1. Lambda initializes function
2. Takes snapshot of memory/disk state
3. Caches snapshot
4. Restores from snapshot on cold start (150ms vs 5s)

**Enable SnapStart:**
```bash
aws lambda update-function-configuration \
  --function-name MyJavaFunction \
  --snap-start ApplyOn=PublishedVersions
```

**Code Considerations:**
```java
// ❌ BAD: Generates unique ID during INIT (same for all invocations)
public class BadHandler implements RequestHandler<String, String> {
    private static final String REQUEST_ID = UUID.randomUUID().toString();
    
    @Override
    public String handleRequest(String input, Context context) {
        return REQUEST_ID; // Same ID for all requests!
    }
}

// ✅ GOOD: Generate unique ID during INVOKE
public class GoodHandler implements RequestHandler<String, String> {
    
    @Override
    public String handleRequest(String input, Context context) {
        String requestId = UUID.randomUUID().toString();
        return requestId; // Unique per request
    }
}

// ✅ GOOD: Use afterRestore hook
public class SnapStartHandler implements RequestHandler<String, String> {
    
    private static DatabaseConnection connection;
    
    static {
        connection = new DatabaseConnection();
        
        // Re-establish connection after snapshot restore
        CRaC.getContext().register(() -> {
            connection.reconnect();
        });
    }
    
    @Override
    public String handleRequest(String input, Context context) {
        return connection.query(input);
    }
}
```

**SnapStart Limitations:**
- Java 11+ only
- Published versions only (not $LATEST)
- Network connections must be re-established
- Random seeds must be regenerated
- Timestamps must be refreshed

### 10.8 Lifecycle Optimization Strategies

**1. Minimize Cold Start Impact:**
```java
// Use lightweight dependencies
import com.amazonaws.services.lambda.runtime.RequestHandler;
// Avoid: import org.springframework.boot.SpringApplication; (heavy)

// Lazy initialization for rarely-used resources
public class LazyHandler implements RequestHandler<String, String> {
    private static S3Client s3Client;
    
    private S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.create();
        }
        return s3Client;
    }
    
    @Override
    public String handleRequest(String input, Context context) {
        if (input.contains("s3")) {
            return getS3Client().listBuckets().toString();
        }
        return "No S3 needed";
    }
}
```

**2. Keep Functions Warm:**
```java
// Scheduled EventBridge rule (every 5 minutes)
{
  "source": ["aws.events"],
  "detail-type": ["Scheduled Event"],
  "resources": ["arn:aws:events:us-east-1:123456789012:rule/keep-warm"]
}

// Handler detects warmup
public String handleRequest(Map<String, Object> event, Context context) {
    if (event.containsKey("source") && "aws.events".equals(event.get("source"))) {
        return "Warmup ping";
    }
    
    // Normal processing
    return processRequest(event);
}
```

**3. Optimize Package Size:**
```bash
# Use Lambda layers for dependencies
aws lambda publish-layer-version \
  --layer-name my-dependencies \
  --zip-file fileb://layer.zip

# Exclude unnecessary files
zip -r function.zip . -x "*.git*" "*.idea*" "*test*" "*.md"

# Use ProGuard/R8 for Java (reduce JAR size by 50-70%)
```

**4. Monitor Lifecycle Metrics:**
```java
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

public class MetricsHandler implements RequestHandler<String, String> {
    
    private static boolean isColdStart = true;
    
    @Override
    public String handleRequest(String input, Context context) {
        MetricsLogger metrics = new MetricsLogger();
        
        if (isColdStart) {
            metrics.putMetric("ColdStart", 1, Unit.COUNT);
            isColdStart = false;
        } else {
            metrics.putMetric("WarmStart", 1, Unit.COUNT);
        }
        
        long startTime = System.currentTimeMillis();
        String result = processRequest(input);
        long duration = System.currentTimeMillis() - startTime;
        
        metrics.putMetric("ProcessingTime", duration, Unit.MILLISECONDS);
        metrics.flush();
        
        return result;
    }
}
```



---

## 11. Lambda Limitations - Deep Dive {#limitations}

### 11.1 Execution Limits

#### 11.1.1 Timeout Limit (15 Minutes Maximum)

**Hard Limit:** 900 seconds (15 minutes)

**Impact:**
- Long-running batch jobs cannot run in Lambda
- Video transcoding of large files may timeout
- Complex ML model training impossible
- Large data migrations fail

**Workarounds:**
```java
// ❌ BAD: Single long-running function
public String processLargeDataset(List<String> items, Context context) {
    for (String item : items) {
        processItem(item); // May exceed 15 minutes
    }
    return "Done";
}

// ✅ GOOD: Break into smaller chunks with Step Functions
public String processChunk(List<String> chunk, Context context) {
    for (String item : chunk) {
        processItem(item);
    }
    return "Chunk complete";
}

// ✅ GOOD: Use SQS for long-running workflows
public String handleRequest(SQSEvent event, Context context) {
    for (SQSEvent.SQSMessage msg : event.getRecords()) {
        processItem(msg.getBody());
        
        // Check remaining time
        if (context.getRemainingTimeInMillis() < 30000) {
            // Re-queue remaining items
            requeueMessages(event.getRecords());
            break;
        }
    }
    return "Processed";
}
```

**Alternatives for Long-Running Tasks:**
- **AWS Batch**: Hours-long jobs
- **ECS Fargate**: Containerized long-running tasks
- **EC2**: Full control over execution time
- **Step Functions**: Orchestrate multiple Lambda invocations (1 year max)

#### 11.1.2 Memory Limit (10 GB Maximum)

**Range:** 128 MB to 10,240 MB (10 GB)

**Impact:**
- Large in-memory data processing limited
- Cannot load large ML models (> 10GB)
- Big data analytics constrained
- Image/video processing of large files problematic

**Memory vs CPU Allocation:**
| Memory | vCPU | Network Bandwidth |
|--------|------|-------------------|
| 128 MB | 0.07 vCPU | Low |
| 512 MB | 0.29 vCPU | Low |
| 1,769 MB | 1 vCPU | Moderate |
| 3,008 MB | 2 vCPU | High |
| 10,240 MB | 6 vCPU | Very High |

**Workarounds:**
```java
// ❌ BAD: Load entire file into memory
public String processFile(String s3Key, Context context) {
    byte[] fileContent = s3Client.getObjectAsBytes(/* ... */).asByteArray();
    // OutOfMemoryError if file > 10GB
    return processContent(fileContent);
}

// ✅ GOOD: Stream processing
public String processFileStreaming(String s3Key, Context context) {
    try (InputStream stream = s3Client.getObject(/* ... */)) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            processLine(line); // Process incrementally
        }
    }
    return "Done";
}

// ✅ GOOD: Use external storage
public String processLargeData(String dataKey, Context context) {
    // Store intermediate results in S3/DynamoDB
    List<String> chunk = fetchChunk(dataKey, 0, 1000);
    String result = processChunk(chunk);
    storeResult(dataKey, result);
    
    // Trigger next Lambda for next chunk
    invokeLambda("processLargeData", Map.of("dataKey", dataKey, "offset", 1000));
    return "Chunk processed";
}
```

#### 11.1.3 Ephemeral Storage (/tmp) Limit

**Range:** 512 MB to 10,240 MB (10 GB)

**Impact:**
- Cannot store large temporary files
- Limited space for downloads/uploads
- Insufficient for large data transformations

**Characteristics:**
- Persists across warm invocations
- Cleared on cold start
- Shared across concurrent executions (isolated)
- Costs $0.0000000309 per GB-second

**Workarounds:**
```java
// ✅ Use /tmp for small temporary files
public String processTempFile(String data, Context context) {
    File tmpFile = new File("/tmp/data-" + context.getAwsRequestId() + ".txt");
    Files.writeString(tmpFile.toPath(), data);
    
    // Process file
    String result = processFile(tmpFile);
    
    // Cleanup (optional, but recommended)
    tmpFile.delete();
    
    return result;
}

// ✅ Use EFS for larger persistent storage
public String processWithEFS(String data, Context context) {
    // Mount EFS at /mnt/efs
    File efsFile = new File("/mnt/efs/data.txt");
    Files.writeString(efsFile.toPath(), data);
    
    // EFS persists across invocations and functions
    return "Stored in EFS";
}

// ✅ Use S3 for large files
public String processLargeFile(String inputKey, Context context) {
    // Download from S3, process, upload result
    File tmpFile = new File("/tmp/input.dat");
    s3Client.getObject(GetObjectRequest.builder()
            .bucket("my-bucket")
            .key(inputKey)
            .build(), tmpFile.toPath());
    
    // Process
    File outputFile = new File("/tmp/output.dat");
    processFile(tmpFile, outputFile);
    
    // Upload result
    s3Client.putObject(PutObjectRequest.builder()
            .bucket("my-bucket")
            .key("output/" + inputKey)
            .build(), outputFile.toPath());
    
    return "Processed";
}
```

#### 11.1.4 Deployment Package Size Limits

**Limits:**
- **ZIP file (direct upload):** 50 MB
- **ZIP file (via S3):** 250 MB (uncompressed)
- **Container image:** 10 GB
- **Layers:** 5 layers max, 250 MB total uncompressed

**Impact:**
- Cannot include large dependencies
- ML models must be external
- Large frameworks (Spring Boot) challenging

**Workarounds:**
```bash
# ✅ Use Lambda Layers for dependencies
# Layer 1: AWS SDK
# Layer 2: Common utilities
# Layer 3: Business logic libraries

# ✅ Use container images for large dependencies
FROM public.ecr.aws/lambda/java:17
COPY target/my-function.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["com.example.Handler::handleRequest"]

# ✅ Download models from S3 at runtime
```

```java
// ✅ Lazy-load large resources
public class ModelHandler implements RequestHandler<String, String> {
    
    private static byte[] model;
    
    private byte[] getModel() {
        if (model == null) {
            // Download from S3 on first invocation
            model = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket("my-models")
                    .key("model.bin")
                    .build()).asByteArray();
        }
        return model;
    }
    
    @Override
    public String handleRequest(String input, Context context) {
        byte[] modelData = getModel();
        return runInference(modelData, input);
    }
}
```

### 11.2 Concurrency Limits

#### 11.2.1 Account-Level Concurrency Limit

**Default Limit:** 1,000 concurrent executions per region

**Impact:**
- Sudden traffic spikes throttled
- Multiple functions share limit
- Burst capacity: 500-3,000 (region-dependent)

**Throttling Behavior:**
```
Synchronous invocation: Returns 429 TooManyRequestsException
Asynchronous invocation: Retries automatically (2 times)
Event source mapping: Retries until success or data expires
```

**Request Limit Increase:**
```bash
# AWS Support ticket required
# Can increase to 10,000+ concurrent executions
# Soft limit, can be raised
```

**Workarounds:**
```java
// ✅ Implement exponential backoff
public String invokeWithRetry(String functionName, String payload) {
    int maxRetries = 5;
    int attempt = 0;
    
    while (attempt < maxRetries) {
        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());
            return response.payload().asUtf8String();
        } catch (TooManyRequestsException e) {
            attempt++;
            int backoff = (int) Math.pow(2, attempt) * 100;
            Thread.sleep(backoff);
        }
    }
    throw new RuntimeException("Max retries exceeded");
}

// ✅ Use reserved concurrency to protect critical functions
aws lambda put-function-concurrency \
  --function-name CriticalFunction \
  --reserved-concurrent-executions 100
```

#### 11.2.2 Reserved vs Unreserved Concurrency

**Reserved Concurrency:**
- Guarantees capacity for specific function
- Reduces account-level pool
- Prevents one function from consuming all capacity

**Example:**
```
Account limit: 1,000
Function A reserved: 200
Function B reserved: 300
Unreserved pool: 500 (shared by all other functions)
```

**Provisioned Concurrency:**
- Pre-initialized execution environments
- Eliminates cold starts
- Costs more ($0.0000041667 per GB-second)

### 11.3 Payload Size Limits

#### 11.3.1 Request/Response Payload Limits

**Limits:**
- **Synchronous invocation:** 6 MB request, 6 MB response
- **Asynchronous invocation:** 256 KB
- **Event source mapping:** Varies by source

**Impact:**
- Cannot pass large data directly
- API Gateway + Lambda limited to 6 MB
- Large file uploads must use S3

**Workarounds:**
```java
// ❌ BAD: Pass large data in payload
public String processData(LargeDataRequest request, Context context) {
    byte[] data = request.getData(); // May exceed 6 MB
    return processLargeData(data);
}

// ✅ GOOD: Use S3 for large data
public String processData(S3Reference request, Context context) {
    // Request only contains S3 key (few bytes)
    byte[] data = s3Client.getObjectAsBytes(GetObjectRequest.builder()
            .bucket(request.getBucket())
            .key(request.getKey())
            .build()).asByteArray();
    
    String result = processLargeData(data);
    
    // Store result in S3
    s3Client.putObject(PutObjectRequest.builder()
            .bucket(request.getBucket())
            .key("results/" + request.getKey())
            .build(), RequestBody.fromString(result));
    
    return "s3://bucket/results/" + request.getKey();
}

// ✅ GOOD: Stream large responses via S3 presigned URL
public APIGatewayProxyResponseEvent generateReport(Map<String, Object> request, Context context) {
    String report = generateLargeReport(); // > 6 MB
    
    // Upload to S3
    String key = "reports/" + UUID.randomUUID() + ".pdf";
    s3Client.putObject(PutObjectRequest.builder()
            .bucket("my-reports")
            .key(key)
            .build(), RequestBody.fromString(report));
    
    // Generate presigned URL (valid for 1 hour)
    String presignedUrl = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(1))
            .getObjectRequest(GetObjectRequest.builder()
                    .bucket("my-reports")
                    .key(key)
                    .build())
            .build()).url().toString();
    
    return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody("{\"downloadUrl\": \"" + presignedUrl + "\"}");
}
```

### 11.4 Network and VPC Limitations

#### 11.4.1 VPC Cold Start Penalty

**Impact:**
- VPC-enabled Lambda: +1-3 seconds cold start
- ENI (Elastic Network Interface) creation delay
- Hyperplane ENI (2019+): Reduced to ~100ms

**When to Use VPC:**
- Access RDS databases
- Access ElastiCache
- Access internal services
- Compliance requirements

**Workarounds:**
```java
// ✅ Use RDS Proxy to reduce connections
public String queryDatabase(String query, Context context) {
    // RDS Proxy manages connection pooling
    String rdsProxyEndpoint = System.getenv("RDS_PROXY_ENDPOINT");
    Connection conn = DriverManager.getConnection(rdsProxyEndpoint, user, password);
    // Execute query
    return result;
}

// ✅ Use Secrets Manager for credentials
public String getDatabaseCredentials() {
    GetSecretValueResponse secret = secretsClient.getSecretValue(GetSecretValueRequest.builder()
            .secretId("db-credentials")
            .build());
    return secret.secretString();
}
```

#### 11.4.2 Outbound Internet Access in VPC

**Problem:** Lambda in private subnet cannot access internet

**Solution:** NAT Gateway (costs $0.045/hour + data transfer)

**Workarounds:**
```bash
# ✅ Use VPC Endpoints for AWS services (no NAT needed)
aws ec2 create-vpc-endpoint \
  --vpc-id vpc-12345 \
  --service-name com.amazonaws.us-east-1.s3 \
  --route-table-ids rtb-12345

# Supported services: S3, DynamoDB, SQS, SNS, Lambda, etc.
```

### 11.5 State Management Limitations

#### 11.5.1 Stateless Execution Model

**Impact:**
- No persistent state between invocations (unless warm)
- Cannot maintain WebSocket connections
- Cannot run background threads
- Session management challenging

**Workarounds:**
```java
// ✅ Use DynamoDB for state
public String handleRequest(Map<String, Object> event, Context context) {
    String userId = (String) event.get("userId");
    
    // Load state from DynamoDB
    GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
            .tableName("user-sessions")
            .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
            .build());
    
    Map<String, AttributeValue> session = response.item();
    
    // Process request
    String result = processWithState(event, session);
    
    // Save state
    dynamoDb.putItem(PutItemRequest.builder()
            .tableName("user-sessions")
            .item(Map.of(
                    "userId", AttributeValue.builder().s(userId).build(),
                    "data", AttributeValue.builder().s(result).build()
            ))
            .build());
    
    return result;
}

// ✅ Use ElastiCache for session management
public String handleRequest(Map<String, Object> event, Context context) {
    String sessionId = (String) event.get("sessionId");
    
    // Load from Redis
    Jedis jedis = new Jedis(redisEndpoint);
    String sessionData = jedis.get("session:" + sessionId);
    
    // Process
    String result = processWithSession(event, sessionData);
    
    // Save with TTL
    jedis.setex("session:" + sessionId, 3600, result);
    
    return result;
}
```

### 11.6 Cold Start Limitations

#### 11.6.1 Unpredictable Cold Starts

**Causes:**
- First invocation
- After idle period (5-15 minutes)
- Scaling up (new concurrent executions)
- Code/configuration updates
- AWS infrastructure maintenance

**Impact:**
- Inconsistent latency (100ms vs 5s)
- Poor user experience for latency-sensitive apps
- SLA violations

**Mitigation:**
```java
// ✅ Provisioned Concurrency (eliminates cold starts)
aws lambda put-provisioned-concurrency-config \
  --function-name MyFunction \
  --provisioned-concurrent-executions 10 \
  --qualifier $LATEST

// ✅ Scheduled warmup (keep functions warm)
// EventBridge rule: rate(5 minutes)
public String handleRequest(Map<String, Object> event, Context context) {
    if (event.containsKey("warmup") && (Boolean) event.get("warmup")) {
        return "Warmup successful";
    }
    
    // Normal processing
    return processRequest(event);
}

// ✅ SnapStart for Java (90% faster cold starts)
aws lambda update-function-configuration \
  --function-name MyJavaFunction \
  --snap-start ApplyOn=PublishedVersions
```

### 11.7 Monitoring and Debugging Limitations

#### 11.7.1 Limited Debugging Capabilities

**Challenges:**
- No SSH access
- No interactive debugging
- Limited log retention (CloudWatch Logs)
- No profiling tools

**Workarounds:**
```java
// ✅ Enhanced logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugHandler implements RequestHandler<Map<String, Object>, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(DebugHandler.class);
    
    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        logger.info("Request ID: {}", context.getAwsRequestId());
        logger.info("Event: {}", event);
        logger.info("Remaining time: {}ms", context.getRemainingTimeInMillis());
        
        try {
            String result = processRequest(event);
            logger.info("Result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error processing request", e);
            throw e;
        }
    }
}

// ✅ X-Ray tracing
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

public String handleRequest(Map<String, Object> event, Context context) {
    Subsegment subsegment = AWSXRay.beginSubsegment("ProcessRequest");
    try {
        subsegment.putAnnotation("userId", event.get("userId").toString());
        subsegment.putMetadata("event", event);
        
        String result = processRequest(event);
        
        subsegment.putMetadata("result", result);
        return result;
    } finally {
        AWSXRay.endSubsegment();
    }
}

// ✅ CloudWatch Embedded Metrics Format
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

public String handleRequest(Map<String, Object> event, Context context) {
    MetricsLogger metrics = new MetricsLogger();
    
    long startTime = System.currentTimeMillis();
    String result = processRequest(event);
    long duration = System.currentTimeMillis() - startTime;
    
    metrics.putMetric("ProcessingTime", duration, Unit.MILLISECONDS);
    metrics.putDimensions(DimensionSet.of("FunctionName", context.getFunctionName()));
    metrics.flush();
    
    return result;
}
```

### 11.8 Cost Limitations

#### 11.8.1 Expensive for High-Frequency, Long-Running Tasks

**Cost Comparison:**

**Lambda:**
```
1M requests/day × 1GB memory × 5s duration
= 1,000,000 × 5 GB-seconds = 5,000,000 GB-seconds/day
= 5,000,000 × $0.0000166667 = $83.33/day = $2,500/month
+ 1M requests × $0.20/1M = $0.20/day = $6/month
Total: $2,506/month
```

**EC2 (t3.medium):**
```
$0.0416/hour × 24 hours × 30 days = $29.95/month
```

**When Lambda is Expensive:**
- High request volume (> 10M/day)
- Long execution time (> 1 minute average)
- High memory requirements (> 2GB)
- Constant traffic (24/7)

**When Lambda is Cost-Effective:**
- Sporadic traffic
- Short execution time (< 1 second)
- Low request volume (< 1M/day)
- Event-driven workloads

### 11.9 Language and Runtime Limitations

#### 11.9.1 Limited Runtime Support

**Supported Runtimes:**
- Python 3.8, 3.9, 3.10, 3.11, 3.12
- Node.js 16.x, 18.x, 20.x
- Java 8, 11, 17, 21
- .NET 6, .NET 8
- Go 1.x
- Ruby 3.2, 3.3
- Custom Runtime (Runtime API)

**Unsupported:**
- PHP (use custom runtime)
- Rust (use custom runtime)
- C/C++ (use custom runtime)
- Older versions (Python 2.7, Node.js 12, Java 8 AL1)

**Custom Runtime Example:**
```bash
# bootstrap file
#!/bin/sh
set -euo pipefail

# Initialize
HANDLER="_HANDLER"
RUNTIME_API="AWS_LAMBDA_RUNTIME_API"

# Processing loop
while true; do
  # Get next event
  EVENT_DATA=$(curl -sS "http://${RUNTIME_API}/2018-06-01/runtime/invocation/next")
  REQUEST_ID=$(echo $EVENT_DATA | jq -r '.requestId')
  
  # Process event
  RESPONSE=$(echo $EVENT_DATA | ./my-custom-runtime)
  
  # Send response
  curl -sS -X POST "http://${RUNTIME_API}/2018-06-01/runtime/invocation/${REQUEST_ID}/response" \
    -d "$RESPONSE"
done
```

### 11.10 Security Limitations

#### 11.10.1 Shared Responsibility Model

**AWS Manages:**
- Infrastructure security
- Runtime patching
- Network isolation
- Physical security

**You Manage:**
- Code vulnerabilities
- Dependency vulnerabilities
- IAM permissions
- Secrets management
- Input validation

**Security Best Practices:**
```java
// ✅ Use least privilege IAM roles
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "dynamodb:GetItem",
      "dynamodb:PutItem"
    ],
    "Resource": "arn:aws:dynamodb:us-east-1:123456789012:table/MyTable"
  }]
}

// ✅ Encrypt environment variables
aws lambda update-function-configuration \
  --function-name MyFunction \
  --kms-key-arn arn:aws:kms:us-east-1:123456789012:key/12345678

// ✅ Use Secrets Manager for sensitive data
public String getSecret(String secretName) {
    GetSecretValueResponse response = secretsClient.getSecretValue(
        GetSecretValueRequest.builder()
            .secretId(secretName)
            .build()
    );
    return response.secretString();
}

// ✅ Validate input
public String handleRequest(Map<String, Object> event, Context context) {
    String userId = (String) event.get("userId");
    
    if (userId == null || !userId.matches("^[a-zA-Z0-9-]+$")) {
        throw new IllegalArgumentException("Invalid userId");
    }
    
    return processRequest(userId);
}
```

### 11.11 Summary: When NOT to Use Lambda

**Avoid Lambda for:**
1. **Long-running tasks** (> 15 minutes)
2. **High-memory workloads** (> 10 GB)
3. **Stateful applications** (WebSocket servers, game servers)
4. **Consistent high traffic** (24/7 at scale)
5. **Large deployment packages** (> 250 MB uncompressed)
6. **Low-latency requirements** (< 10ms) with unpredictable cold starts
7. **Complex debugging needs** (interactive debugging required)
8. **GPU-intensive workloads** (ML training, video rendering)
9. **Legacy applications** (difficult to refactor)
10. **Cost-sensitive high-volume** (> 10M requests/day with long duration)

**Use Lambda for:**
1. **Event-driven workloads** (S3 uploads, DynamoDB streams)
2. **API backends** (REST APIs, GraphQL)
3. **Scheduled tasks** (cron jobs, batch processing)
4. **Stream processing** (Kinesis, Kafka)
5. **Microservices** (small, independent services)
6. **Serverless applications** (no infrastructure management)
7. **Sporadic traffic** (unpredictable load)
8. **Short-lived tasks** (< 5 minutes)
9. **Prototyping** (fast iteration)
10. **Cost optimization** (pay-per-use for low traffic)

---

**End of AWS Lambda Complete Guide**



---

## 12. Development & Deployment Methods - Deep Dive {#development-deployment}

### 12.1 Development Approaches Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│              LAMBDA DEVELOPMENT METHODS                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Console-Based (Quick Prototyping)                               │
│  2. AWS CLI (Scripted Deployment)                                   │
│  3. AWS SAM (Serverless Application Model)                          │
│  4. Serverless Framework (Multi-Cloud)                              │
│  5. Terraform (Infrastructure as Code)                              │
│  6. CDK (Cloud Development Kit)                                     │
│  7. CloudFormation (Native IaC)                                     │
│  8. Container Images (Docker)                                       │
│  9. IDE Integration (VS Code, IntelliJ)                             │
│  10. CI/CD Pipelines (CodePipeline, GitHub Actions, Jenkins)        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 12.2 Method 1: AWS Console (Web-Based Development)

**Best For:** Quick prototyping, learning, simple functions

**Pros:**
- No local setup required
- Instant testing
- Visual interface
- Built-in code editor

**Cons:**
- No version control
- Limited to small functions
- No local testing
- Manual deployment

**Step-by-Step:**

```bash
# 1. Navigate to Lambda Console
AWS Console → Lambda → Create function

# 2. Choose authoring method
- Author from scratch
- Use a blueprint
- Container image
- Browse serverless app repository

# 3. Configure function
Function name: MyConsoleFunction
Runtime: Java 17
Architecture: x86_64

# 4. Write code inline (for small functions)
# Or upload ZIP file (for larger functions)

# 5. Configure test event
{
  "userId": "user123",
  "action": "getData"
}

# 6. Test and deploy
```

**Example: Inline Python Function**
```python
import json

def lambda_handler(event, context):
    user_id = event.get('userId')
    action = event.get('action')
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'message': f'Processing {action} for {user_id}'
        })
    }
```

**Limitations:**
- Code editor limited to 3 MB
- No dependency management
- No local debugging
- Not suitable for production

### 12.3 Method 2: AWS CLI (Command-Line Deployment)

**Best For:** Scripted deployments, automation, CI/CD integration

**Setup:**
```bash
# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configure credentials
aws configure
AWS Access Key ID: YOUR_ACCESS_KEY
AWS Secret Access Key: YOUR_SECRET_KEY
Default region: us-east-1
Default output format: json
```

**Java Lambda Development:**

**Project Structure:**
```
my-lambda/
├── src/
│   └── main/
│       └── java/
│           └── com/example/
│               └── Handler.java
├── pom.xml
└── deploy.sh
```

**pom.xml:**
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-lambda</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.2.3</version>
        </dependency>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-events</artifactId>
            <version>3.11.3</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**Handler.java:**
```java
package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, String> {
    
    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Event: " + event);
        return "Success";
    }
}
```

**deploy.sh:**
```bash
#!/bin/bash

FUNCTION_NAME="MyLambdaFunction"
ROLE_ARN="arn:aws:iam::123456789012:role/lambda-execution-role"
HANDLER="com.example.Handler::handleRequest"
RUNTIME="java17"

# Build
mvn clean package

# Create or update function
if aws lambda get-function --function-name $FUNCTION_NAME 2>/dev/null; then
    echo "Updating existing function..."
    aws lambda update-function-code \
        --function-name $FUNCTION_NAME \
        --zip-file fileb://target/my-lambda-1.0.0.jar
else
    echo "Creating new function..."
    aws lambda create-function \
        --function-name $FUNCTION_NAME \
        --runtime $RUNTIME \
        --role $ROLE_ARN \
        --handler $HANDLER \
        --zip-file fileb://target/my-lambda-1.0.0.jar \
        --timeout 30 \
        --memory-size 512
fi

# Test function
aws lambda invoke \
    --function-name $FUNCTION_NAME \
    --payload '{"userId":"user123"}' \
    response.json

cat response.json
```

**Advanced CLI Operations:**
```bash
# Create function with environment variables
aws lambda create-function \
    --function-name MyFunction \
    --runtime java17 \
    --role $ROLE_ARN \
    --handler com.example.Handler \
    --zip-file fileb://function.jar \
    --environment Variables={DB_HOST=localhost,DB_PORT=5432}

# Update function configuration
aws lambda update-function-configuration \
    --function-name MyFunction \
    --timeout 60 \
    --memory-size 1024 \
    --environment Variables={DB_HOST=prod.db.com}

# Create alias
aws lambda create-alias \
    --function-name MyFunction \
    --name prod \
    --function-version 1

# Update alias (blue-green deployment)
aws lambda update-alias \
    --function-name MyFunction \
    --name prod \
    --function-version 2 \
    --routing-config AdditionalVersionWeights={"1"=0.1}

# Add permission for API Gateway
aws lambda add-permission \
    --function-name MyFunction \
    --statement-id apigateway-invoke \
    --action lambda:InvokeFunction \
    --principal apigateway.amazonaws.com

# List functions
aws lambda list-functions --max-items 10

# Get function configuration
aws lambda get-function-configuration --function-name MyFunction

# Delete function
aws lambda delete-function --function-name MyFunction
```

### 12.4 Method 3: AWS SAM (Serverless Application Model)

**Best For:** Serverless applications, local testing, production deployments

**Installation:**
```bash
# Install SAM CLI
brew install aws-sam-cli  # macOS
pip install aws-sam-cli   # Python

# Verify installation
sam --version
```

**Initialize Project:**
```bash
# Create new SAM project
sam init

# Choose options:
# 1. AWS Quick Start Templates
# 2. Java 17
# 3. Maven
# 4. Hello World Example

cd sam-app
```

**Project Structure:**
```
sam-app/
├── template.yaml          # SAM template
├── HelloWorldFunction/
│   ├── src/
│   │   └── main/java/helloworld/
│   │       └── App.java
│   └── pom.xml
├── events/
│   └── event.json
└── samconfig.toml
```

**template.yaml:**
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: SAM Lambda Application

Globals:
  Function:
    Timeout: 30
    MemorySize: 512
    Runtime: java17
    Architectures:
      - x86_64
    Environment:
      Variables:
        TABLE_NAME: !Ref UsersTable

Resources:
  # Lambda Function
  HelloWorldFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: HelloWorldFunction
      Handler: helloworld.App::handleRequest
      Events:
        HelloWorld:
          Type: Api
          Properties:
            Path: /hello
            Method: get
      Policies:
        - DynamoDBCrudPolicy:
            TableName: !Ref UsersTable
  
  # DynamoDB Table
  UsersTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: users
      AttributeDefinitions:
        - AttributeName: userId
          AttributeType: S
      KeySchema:
        - AttributeName: userId
          KeyType: HASH
      BillingMode: PAY_PER_REQUEST

Outputs:
  HelloWorldApi:
    Description: "API Gateway endpoint URL"
    Value: !Sub "https://${ServerlessRestApi}.execute-api.${AWS::Region}.amazonaws.com/Prod/hello/"
  HelloWorldFunction:
    Description: "Lambda Function ARN"
    Value: !GetAtt HelloWorldFunction.Arn
```

**App.java:**
```java
package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final String tableName = System.getenv("TABLE_NAME");
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String userId = input.getQueryStringParameters().get("userId");
        
        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
                .build());
        
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody("{\"message\":\"Hello " + userId + "\"}");
    }
}
```

**SAM Commands:**
```bash
# Build application
sam build

# Test locally
sam local invoke HelloWorldFunction -e events/event.json

# Start local API Gateway
sam local start-api
curl http://localhost:3000/hello?userId=user123

# Start local Lambda endpoint
sam local start-lambda
aws lambda invoke --function-name HelloWorldFunction \
    --endpoint-url http://127.0.0.1:3001 \
    --payload '{"userId":"user123"}' response.json

# Validate template
sam validate

# Deploy (guided first time)
sam deploy --guided

# Deploy (subsequent)
sam deploy

# View logs
sam logs -n HelloWorldFunction --tail

# Delete stack
sam delete
```

**samconfig.toml:**
```toml
version = 0.1
[default.deploy.parameters]
stack_name = "sam-app"
s3_bucket = "my-sam-deployment-bucket"
s3_prefix = "sam-app"
region = "us-east-1"
capabilities = "CAPABILITY_IAM"
parameter_overrides = "Environment=prod"
```

**Advanced SAM Features:**

**1. Layers:**
```yaml
Resources:
  MyLayer:
    Type: AWS::Serverless::LayerVersion
    Properties:
      LayerName: my-dependencies
      ContentUri: dependencies/
      CompatibleRuntimes:
        - java17
  
  MyFunction:
    Type: AWS::Serverless::Function
    Properties:
      Layers:
        - !Ref MyLayer
```

**2. Environment-Specific Configs:**
```yaml
Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues:
      - dev
      - staging
      - prod

Mappings:
  EnvironmentConfig:
    dev:
      MemorySize: 512
      Timeout: 30
    prod:
      MemorySize: 2048
      Timeout: 60

Resources:
  MyFunction:
    Type: AWS::Serverless::Function
    Properties:
      MemorySize: !FindInMap [EnvironmentConfig, !Ref Environment, MemorySize]
      Timeout: !FindInMap [EnvironmentConfig, !Ref Environment, Timeout]
```

**3. Canary Deployments:**
```yaml
Resources:
  MyFunction:
    Type: AWS::Serverless::Function
    Properties:
      AutoPublishAlias: live
      DeploymentPreference:
        Type: Canary10Percent5Minutes
        Alarms:
          - !Ref ErrorAlarm
        Hooks:
          PreTraffic: !Ref PreTrafficHook
          PostTraffic: !Ref PostTrafficHook
```

### 12.5 Method 4: Serverless Framework

**Best For:** Multi-cloud, plugin ecosystem, rapid development

**Installation:**
```bash
npm install -g serverless

# Verify
serverless --version
```

**Initialize Project:**
```bash
# Create new project
serverless create --template aws-java-maven --path my-service
cd my-service
```

**Project Structure:**
```
my-service/
├── serverless.yml
├── src/
│   └── main/java/com/serverless/
│       └── Handler.java
└── pom.xml
```

**serverless.yml:**
```yaml
service: my-service

provider:
  name: aws
  runtime: java17
  region: us-east-1
  stage: ${opt:stage, 'dev'}
  memorySize: 512
  timeout: 30
  environment:
    TABLE_NAME: ${self:service}-${self:provider.stage}-users
  iam:
    role:
      statements:
        - Effect: Allow
          Action:
            - dynamodb:GetItem
            - dynamodb:PutItem
          Resource: !GetAtt UsersTable.Arn

package:
  artifact: target/my-service-1.0.0.jar

functions:
  hello:
    handler: com.serverless.Handler
    events:
      - http:
          path: hello
          method: get
          cors: true
      - schedule:
          rate: rate(5 minutes)
          enabled: true
  
  processS3:
    handler: com.serverless.S3Handler
    events:
      - s3:
          bucket: my-uploads
          event: s3:ObjectCreated:*
          rules:
            - prefix: uploads/
            - suffix: .jpg

resources:
  Resources:
    UsersTable:
      Type: AWS::DynamoDB::Table
      Properties:
        TableName: ${self:provider.environment.TABLE_NAME}
        AttributeDefinitions:
          - AttributeName: userId
            AttributeType: S
        KeySchema:
          - AttributeName: userId
            KeyType: HASH
        BillingMode: PAY_PER_REQUEST

plugins:
  - serverless-offline
  - serverless-plugin-canary-deployments
```

**Handler.java:**
```java
package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
    
    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setBody("{\"message\":\"Hello World\"}")
                .build();
    }
}
```

**Serverless Commands:**
```bash
# Deploy entire service
serverless deploy

# Deploy single function (faster)
serverless deploy function -f hello

# Deploy to specific stage
serverless deploy --stage prod --region us-west-2

# Invoke function
serverless invoke -f hello -d '{"userId":"user123"}'

# Invoke locally
serverless invoke local -f hello -d '{"userId":"user123"}'

# View logs
serverless logs -f hello --tail

# Remove service
serverless remove

# Info about deployed service
serverless info
```

**Advanced Features:**

**1. Multiple Stages:**
```yaml
custom:
  stages:
    dev:
      memorySize: 512
    prod:
      memorySize: 2048

provider:
  memorySize: ${self:custom.stages.${self:provider.stage}.memorySize}
```

**2. VPC Configuration:**
```yaml
provider:
  vpc:
    securityGroupIds:
      - sg-12345678
    subnetIds:
      - subnet-12345678
      - subnet-87654321
```

**3. Plugins:**
```bash
# Install plugins
npm install --save-dev serverless-offline
npm install --save-dev serverless-plugin-warmup

# serverless.yml
plugins:
  - serverless-offline
  - serverless-plugin-warmup

custom:
  warmup:
    default:
      enabled: true
      events:
        - schedule: rate(5 minutes)
```



### 12.6 Method 5: Terraform (Infrastructure as Code)

**Best For:** Multi-cloud infrastructure, existing Terraform workflows

**Installation:**
```bash
# Install Terraform
brew install terraform  # macOS
# or download from terraform.io

terraform --version
```

**Project Structure:**
```
terraform-lambda/
├── main.tf
├── variables.tf
├── outputs.tf
├── lambda/
│   └── function.jar
└── terraform.tfvars
```

**main.tf:**
```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "${var.function_name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

# Attach basic execution policy
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# DynamoDB policy
resource "aws_iam_role_policy" "lambda_dynamodb" {
  name = "${var.function_name}-dynamodb-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem"
      ]
      Resource = aws_dynamodb_table.users.arn
    }]
  })
}

# Lambda Function
resource "aws_lambda_function" "main" {
  filename         = "lambda/function.jar"
  function_name    = var.function_name
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.example.Handler::handleRequest"
  source_code_hash = filebase64sha256("lambda/function.jar")
  runtime         = "java17"
  timeout         = 30
  memory_size     = 512

  environment {
    variables = {
      TABLE_NAME = aws_dynamodb_table.users.name
      STAGE      = var.stage
    }
  }

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = var.security_group_ids
  }

  tags = {
    Environment = var.stage
    ManagedBy   = "Terraform"
  }
}

# Lambda Alias
resource "aws_lambda_alias" "main" {
  name             = var.stage
  function_name    = aws_lambda_function.main.function_name
  function_version = aws_lambda_function.main.version
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = 7
}

# DynamoDB Table
resource "aws_dynamodb_table" "users" {
  name           = "${var.function_name}-users"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  tags = {
    Environment = var.stage
  }
}

# API Gateway
resource "aws_apigatewayv2_api" "main" {
  name          = "${var.function_name}-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "lambda" {
  api_id           = aws_apigatewayv2_api.main.id
  integration_type = "AWS_PROXY"
  integration_uri  = aws_lambda_function.main.invoke_arn
}

resource "aws_apigatewayv2_route" "default" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /hello"
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = var.stage
  auto_deploy = true
}

# Lambda permission for API Gateway
resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.main.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.main.execution_arn}/*/*"
}

# S3 Trigger
resource "aws_s3_bucket" "uploads" {
  bucket = "${var.function_name}-uploads"
}

resource "aws_lambda_permission" "s3" {
  statement_id  = "AllowS3Invoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.main.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.uploads.arn
}

resource "aws_s3_bucket_notification" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.main.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "uploads/"
  }
}
```

**variables.tf:**
```hcl
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "function_name" {
  description = "Lambda function name"
  type        = string
}

variable "stage" {
  description = "Deployment stage"
  type        = string
  default     = "dev"
}

variable "subnet_ids" {
  description = "VPC subnet IDs"
  type        = list(string)
  default     = []
}

variable "security_group_ids" {
  description = "VPC security group IDs"
  type        = list(string)
  default     = []
}
```

**outputs.tf:**
```hcl
output "function_arn" {
  value = aws_lambda_function.main.arn
}

output "api_endpoint" {
  value = aws_apigatewayv2_stage.default.invoke_url
}

output "dynamodb_table" {
  value = aws_dynamodb_table.users.name
}
```

**terraform.tfvars:**
```hcl
function_name = "my-lambda-function"
stage         = "prod"
aws_region    = "us-east-1"
```

**Terraform Commands:**
```bash
# Initialize
terraform init

# Plan changes
terraform plan

# Apply changes
terraform apply

# Apply with auto-approve
terraform apply -auto-approve

# Destroy infrastructure
terraform destroy

# Show current state
terraform show

# Format code
terraform fmt

# Validate configuration
terraform validate
```

### 12.7 Method 6: AWS CDK (Cloud Development Kit)

**Best For:** Type-safe infrastructure, programmatic IaC, complex applications

**Installation:**
```bash
npm install -g aws-cdk

# Verify
cdk --version
```

**Initialize Project:**
```bash
# Create new CDK project
mkdir cdk-lambda && cd cdk-lambda
cdk init app --language java

# Project structure
cdk-lambda/
├── src/
│   └── main/java/com/myorg/
│       ├── CdkLambdaApp.java
│       └── CdkLambdaStack.java
├── lambda/
│   └── Handler.java
├── pom.xml
└── cdk.json
```

**CdkLambdaStack.java:**
```java
package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.apigatewayv2.alpha.*;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.notifications.*;
import software.amazon.awscdk.services.iam.*;
import constructs.Construct;

public class CdkLambdaStack extends Stack {
    
    public CdkLambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        
        // DynamoDB Table
        Table usersTable = Table.Builder.create(this, "UsersTable")
                .tableName("users")
                .partitionKey(Attribute.builder()
                        .name("userId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        
        // Lambda Function
        Function lambdaFunction = Function.Builder.create(this, "MyFunction")
                .functionName("my-cdk-function")
                .runtime(Runtime.JAVA_17)
                .code(Code.fromAsset("lambda/target/function.jar"))
                .handler("com.example.Handler::handleRequest")
                .timeout(Duration.seconds(30))
                .memorySize(512)
                .environment(Map.of(
                        "TABLE_NAME", usersTable.getTableName(),
                        "STAGE", "prod"
                ))
                .logRetention(RetentionDays.ONE_WEEK)
                .build();
        
        // Grant DynamoDB permissions
        usersTable.grantReadWriteData(lambdaFunction);
        
        // API Gateway
        HttpApi httpApi = HttpApi.Builder.create(this, "MyApi")
                .apiName("my-cdk-api")
                .build();
        
        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/hello")
                .methods(List.of(HttpMethod.GET))
                .integration(new HttpLambdaIntegration("LambdaIntegration", lambdaFunction))
                .build());
        
        // S3 Bucket with Lambda trigger
        Bucket uploadsBucket = Bucket.Builder.create(this, "UploadsBucket")
                .bucketName("my-cdk-uploads")
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .build();
        
        uploadsBucket.addEventNotification(
                EventType.OBJECT_CREATED,
                new LambdaDestination(lambdaFunction),
                NotificationKeyFilter.builder()
                        .prefix("uploads/")
                        .suffix(".jpg")
                        .build()
        );
        
        // Lambda Layer
        LayerVersion layer = LayerVersion.Builder.create(this, "DependenciesLayer")
                .layerVersionName("my-dependencies")
                .code(Code.fromAsset("layer/"))
                .compatibleRuntimes(List.of(Runtime.JAVA_17))
                .build();
        
        lambdaFunction.addLayers(layer);
        
        // Outputs
        CfnOutput.Builder.create(this, "ApiEndpoint")
                .value(httpApi.getApiEndpoint())
                .build();
        
        CfnOutput.Builder.create(this, "FunctionArn")
                .value(lambdaFunction.getFunctionArn())
                .build();
    }
}
```

**CdkLambdaApp.java:**
```java
package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkLambdaApp {
    public static void main(final String[] args) {
        App app = new App();
        
        new CdkLambdaStack(app, "CdkLambdaStack", StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                .build());
        
        app.synth();
    }
}
```

**CDK Commands:**
```bash
# Bootstrap CDK (first time only)
cdk bootstrap

# Synthesize CloudFormation template
cdk synth

# Deploy stack
cdk deploy

# Deploy with auto-approve
cdk deploy --require-approval never

# Diff changes
cdk diff

# Destroy stack
cdk destroy

# List stacks
cdk list
```

**Advanced CDK Patterns:**

**1. Multi-Stack Application:**
```java
public class MultiStackApp {
    public static void main(String[] args) {
        App app = new App();
        
        // Network stack
        NetworkStack networkStack = new NetworkStack(app, "NetworkStack");
        
        // Database stack (depends on network)
        DatabaseStack dbStack = new DatabaseStack(app, "DatabaseStack", 
                networkStack.getVpc());
        
        // Lambda stack (depends on database)
        LambdaStack lambdaStack = new LambdaStack(app, "LambdaStack",
                networkStack.getVpc(),
                dbStack.getTable());
        
        app.synth();
    }
}
```

**2. Custom Constructs:**
```java
public class LambdaApiConstruct extends Construct {
    
    private final Function function;
    private final HttpApi api;
    
    public LambdaApiConstruct(Construct scope, String id, LambdaApiProps props) {
        super(scope, id);
        
        this.function = Function.Builder.create(this, "Function")
                .runtime(Runtime.JAVA_17)
                .code(Code.fromAsset(props.getCodePath()))
                .handler(props.getHandler())
                .build();
        
        this.api = HttpApi.Builder.create(this, "Api")
                .defaultIntegration(new HttpLambdaIntegration("Integration", function))
                .build();
    }
    
    public Function getFunction() { return function; }
    public HttpApi getApi() { return api; }
}
```

### 12.8 Method 7: Container Images (Docker)

**Best For:** Large dependencies, custom runtimes, existing Docker workflows

**Dockerfile:**
```dockerfile
# Use AWS Lambda base image
FROM public.ecr.aws/lambda/java:17

# Copy function code
COPY target/function.jar ${LAMBDA_TASK_ROOT}/lib/

# Copy dependencies
COPY target/dependency/*.jar ${LAMBDA_TASK_ROOT}/lib/

# Set handler
CMD ["com.example.Handler::handleRequest"]
```

**Multi-stage Build (Optimized):**
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM public.ecr.aws/lambda/java:17
COPY --from=build /app/target/function.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["com.example.Handler::handleRequest"]
```

**Build and Deploy:**
```bash
# Build image
docker build -t my-lambda-function .

# Test locally
docker run -p 9000:8080 my-lambda-function

# Invoke locally
curl -XPOST "http://localhost:9000/2015-03-31/functions/function/invocations" \
  -d '{"userId":"user123"}'

# Create ECR repository
aws ecr create-repository --repository-name my-lambda-function

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Tag image
docker tag my-lambda-function:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:latest

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:latest

# Create Lambda function from container
aws lambda create-function \
  --function-name my-container-function \
  --package-type Image \
  --code ImageUri=123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:latest \
  --role arn:aws:iam::123456789012:role/lambda-execution-role

# Update function code
aws lambda update-function-code \
  --function-name my-container-function \
  --image-uri 123456789012.dkr.ecr.us-east-1.amazonaws.com/my-lambda-function:v2
```

**Container-Specific Handler:**
```java
package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Handler implements RequestHandler<Map<String, Object>, String> {
    
    // Container-specific initialization
    static {
        System.out.println("Container initialized");
        // Load large ML models, etc.
    }
    
    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        return "Response from container";
    }
}
```

### 12.9 Method 8: IDE Integration

**VS Code with AWS Toolkit:**

**Installation:**
```bash
# Install AWS Toolkit extension
# VS Code → Extensions → Search "AWS Toolkit"
```

**Features:**
- Create Lambda functions from templates
- Local debugging with SAM
- Deploy directly from IDE
- View CloudWatch logs
- Invoke functions remotely

**launch.json (Debug Configuration):**
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "aws-sam",
      "request": "direct-invoke",
      "name": "Debug Lambda",
      "invokeTarget": {
        "target": "template",
        "templatePath": "${workspaceFolder}/template.yaml",
        "logicalId": "HelloWorldFunction"
      },
      "lambda": {
        "payload": {
          "json": {
            "userId": "user123"
          }
        },
        "environmentVariables": {
          "TABLE_NAME": "users"
        }
      }
    }
  ]
}
```

**IntelliJ IDEA with AWS Toolkit:**

**Installation:**
```
IntelliJ IDEA → Preferences → Plugins → Search "AWS Toolkit"
```

**Features:**
- Create Lambda functions from templates
- Run/Debug locally
- Deploy to AWS
- View CloudWatch logs
- S3 browser
- DynamoDB browser

**Run Configuration:**
```
Run → Edit Configurations → + → AWS Lambda
- Handler: com.example.Handler::handleRequest
- Runtime: Java 17
- Input: {"userId":"user123"}
- Environment variables: TABLE_NAME=users
```

### 12.10 Method 9: CI/CD Pipelines

**GitHub Actions:**

**.github/workflows/deploy.yml:**
```yaml
name: Deploy Lambda

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Deploy to Lambda
        run: |
          aws lambda update-function-code \
            --function-name my-function \
            --zip-file fileb://target/function.jar
      
      - name: Run tests
        run: |
          aws lambda invoke \
            --function-name my-function \
            --payload '{"test":true}' \
            response.json
          cat response.json
```

**AWS CodePipeline:**

**buildspec.yml:**
```yaml
version: 0.2

phases:
  install:
    runtime-versions:
      java: corretto17
  
  build:
    commands:
      - echo Build started on `date`
      - mvn clean package
  
  post_build:
    commands:
      - echo Build completed on `date`
      - aws lambda update-function-code --function-name my-function --zip-file fileb://target/function.jar

artifacts:
  files:
    - target/function.jar
  discard-paths: yes
```

**CloudFormation (CodePipeline):**
```yaml
Resources:
  Pipeline:
    Type: AWS::CodePipeline::Pipeline
    Properties:
      RoleArn: !GetAtt PipelineRole.Arn
      Stages:
        - Name: Source
          Actions:
            - Name: SourceAction
              ActionTypeId:
                Category: Source
                Owner: AWS
                Provider: CodeCommit
                Version: 1
              Configuration:
                RepositoryName: my-lambda-repo
                BranchName: main
              OutputArtifacts:
                - Name: SourceOutput
        
        - Name: Build
          Actions:
            - Name: BuildAction
              ActionTypeId:
                Category: Build
                Owner: AWS
                Provider: CodeBuild
                Version: 1
              Configuration:
                ProjectName: !Ref BuildProject
              InputArtifacts:
                - Name: SourceOutput
              OutputArtifacts:
                - Name: BuildOutput
        
        - Name: Deploy
          Actions:
            - Name: DeployAction
              ActionTypeId:
                Category: Deploy
                Owner: AWS
                Provider: CloudFormation
                Version: 1
              Configuration:
                ActionMode: CREATE_UPDATE
                StackName: my-lambda-stack
                TemplatePath: BuildOutput::template.yaml
              InputArtifacts:
                - Name: BuildOutput
```

**Jenkins Pipeline:**

**Jenkinsfile:**
```groovy
pipeline {
    agent any
    
    environment {
        AWS_REGION = 'us-east-1'
        FUNCTION_NAME = 'my-lambda-function'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Deploy') {
            steps {
                withAWS(credentials: 'aws-credentials', region: env.AWS_REGION) {
                    sh """
                        aws lambda update-function-code \
                            --function-name ${FUNCTION_NAME} \
                            --zip-file fileb://target/function.jar
                    """
                }
            }
        }
        
        stage('Smoke Test') {
            steps {
                withAWS(credentials: 'aws-credentials', region: env.AWS_REGION) {
                    sh """
                        aws lambda invoke \
                            --function-name ${FUNCTION_NAME} \
                            --payload '{"test":true}' \
                            response.json
                        cat response.json
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
```

### 12.11 Comparison Matrix

| Method | Best For | Pros | Cons | Learning Curve |
|--------|----------|------|------|----------------|
| **Console** | Prototyping | Quick, visual | No version control | Low |
| **AWS CLI** | Scripting | Simple, scriptable | Manual | Low |
| **SAM** | Serverless apps | Local testing, AWS-native | AWS-only | Medium |
| **Serverless** | Multi-cloud | Plugin ecosystem | Extra abstraction | Medium |
| **Terraform** | Multi-cloud IaC | Mature, multi-cloud | Verbose | Medium-High |
| **CDK** | Type-safe IaC | Programmatic, reusable | Complex | High |
| **CloudFormation** | AWS-native IaC | Native, comprehensive | YAML/JSON verbose | Medium |
| **Container** | Large deps | Flexible, 10GB limit | Slower cold start | Medium |
| **IDE** | Development | Debugging, productivity | Setup required | Low-Medium |
| **CI/CD** | Production | Automated, tested | Initial setup | Medium-High |

### 12.12 Recommended Workflow

**Development Phase:**
```
1. Local development with IDE (VS Code/IntelliJ)
2. Local testing with SAM CLI
3. Unit tests with JUnit
4. Integration tests with LocalStack
```

**Deployment Phase:**
```
1. Infrastructure: SAM/CDK/Terraform
2. CI/CD: GitHub Actions/CodePipeline
3. Staging deployment with canary
4. Production deployment with rollback capability
```

**Production Best Practices:**
```
1. Use IaC (SAM/CDK/Terraform)
2. Implement CI/CD pipelines
3. Version control everything
4. Automated testing
5. Blue-green or canary deployments
6. Monitoring and alerting
7. Cost optimization
```

---

**End of Development & Deployment Methods Deep Dive**

