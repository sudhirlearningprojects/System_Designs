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
