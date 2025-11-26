# Amazon DynamoDB - Deep Dive (Part 4: AWS Service Integrations)

## Table of Contents
1. [Lambda Integration](#lambda-integration)
2. [API Gateway Integration](#api-gateway-integration)
3. [S3 Integration](#s3-integration)
4. [Kinesis Integration](#kinesis-integration)
5. [Step Functions Integration](#step-functions-integration)
6. [EventBridge Integration](#eventbridge-integration)
7. [AppSync Integration](#appsync-integration)
8. [Cognito Integration](#cognito-integration)

---

## Lambda Integration

### DynamoDB Streams + Lambda

```java
// Lambda function triggered by DynamoDB Streams
public class OrderStreamHandler implements RequestHandler<DynamodbEvent, String> {
    
    private final SNSClient snsClient = SNSClient.create();
    private final SESClient sesClient = SESClient.create();
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        for (DynamodbStreamRecord record : event.getRecords()) {
            processRecord(record);
        }
        return "Success";
    }
    
    private void processRecord(DynamodbStreamRecord record) {
        String eventName = record.getEventName();
        Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
        
        if ("INSERT".equals(eventName)) {
            // New order created
            String orderId = newImage.get("orderId").s();
            String customerEmail = newImage.get("customerEmail").s();
            
            // Send confirmation email
            sendOrderConfirmation(customerEmail, orderId);
            
            // Publish to SNS for downstream processing
            publishOrderEvent(orderId, "ORDER_CREATED");
        }
    }
    
    private void sendOrderConfirmation(String email, String orderId) {
        SendEmailRequest request = SendEmailRequest.builder()
            .destination(Destination.builder().toAddresses(email).build())
            .message(Message.builder()
                .subject(Content.builder().data("Order Confirmation").build())
                .body(Body.builder()
                    .text(Content.builder()
                        .data("Your order " + orderId + " has been confirmed")
                        .build())
                    .build())
                .build())
            .source("orders@example.com")
            .build();
        
        sesClient.sendEmail(request);
    }
}
```

### Lambda CRUD Operations

```java
// Lambda function for API operations
public class UserAPIHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String httpMethod = request.getHttpMethod();
        String path = request.getPath();
        
        try {
            switch (httpMethod) {
                case "GET":
                    return handleGet(request);
                case "POST":
                    return handlePost(request);
                case "PUT":
                    return handlePut(request);
                case "DELETE":
                    return handleDelete(request);
                default:
                    return createResponse(405, "Method not allowed");
            }
        } catch (Exception e) {
            return createResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    private APIGatewayProxyResponseEvent handleGet(APIGatewayProxyRequestEvent request) {
        String userId = request.getPathParameters().get("userId");
        
        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
            .tableName("Users")
            .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
            .build());
        
        if (!response.hasItem()) {
            return createResponse(404, "User not found");
        }
        
        return createResponse(200, convertToJson(response.item()));
    }
    
    private APIGatewayProxyResponseEvent handlePost(APIGatewayProxyRequestEvent request) throws Exception {
        User user = objectMapper.readValue(request.getBody(), User.class);
        
        dynamoDb.putItem(PutItemRequest.builder()
            .tableName("Users")
            .item(Map.of(
                "userId", AttributeValue.builder().s(user.getUserId()).build(),
                "name", AttributeValue.builder().s(user.getName()).build(),
                "email", AttributeValue.builder().s(user.getEmail()).build()
            ))
            .build());
        
        return createResponse(201, "User created");
    }
}
```

---

## API Gateway Integration

### Direct Integration (VTL)

```java
// API Gateway integration request mapping template
{
    "TableName": "Users",
    "Key": {
        "userId": {
            "S": "$input.params('userId')"
        }
    }
}

// Integration response mapping template
#set($item = $input.path('$.Item'))
{
    "userId": "$item.userId.S",
    "name": "$item.name.S",
    "email": "$item.email.S"
}
```

### REST API with Lambda Proxy

```yaml
# SAM template
Resources:
  UsersAPI:
    Type: AWS::Serverless::Api
    Properties:
      StageName: prod
      
  GetUserFunction:
    Type: AWS::Serverless::Function
    Properties:
      Handler: com.example.UserHandler::handleRequest
      Runtime: java17
      Policies:
        - DynamoDBReadPolicy:
            TableName: !Ref UsersTable
      Events:
        GetUser:
          Type: Api
          Properties:
            RestApiId: !Ref UsersAPI
            Path: /users/{userId}
            Method: GET
            
  UsersTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: Users
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: userId
          AttributeType: S
      KeySchema:
        - AttributeName: userId
          KeyType: HASH
```

---

## S3 Integration

### Export DynamoDB to S3

```java
@Service
public class DynamoDBExportService {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    public String exportTableToS3(String tableName, String s3Bucket) {
        ExportTableToPointInTimeRequest request = ExportTableToPointInTimeRequest.builder()
            .tableArn("arn:aws:dynamodb:us-east-1:123456789012:table/" + tableName)
            .s3Bucket(s3Bucket)
            .s3Prefix("exports/" + tableName + "/")
            .exportFormat(ExportFormat.DYNAMODB_JSON)
            .build();
        
        ExportTableToPointInTimeResponse response = dynamoDbClient.exportTableToPointInTime(request);
        return response.exportDescription().exportArn();
    }
}
```

### Import S3 Data to DynamoDB

```java
@Service
public class S3ToDynamoDBImporter {
    
    @Autowired
    private S3Client s3Client;
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    public void importFromS3(String bucket, String key, String tableName) {
        // Read from S3
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
        
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object))) {
            List<WriteRequest> writeRequests = new ArrayList<>();
            String line;
            
            while ((line = reader.readLine()) != null) {
                Map<String, AttributeValue> item = parseJsonToItem(line);
                
                writeRequests.add(WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build());
                
                // Batch write every 25 items
                if (writeRequests.size() == 25) {
                    batchWrite(tableName, writeRequests);
                    writeRequests.clear();
                }
            }
            
            // Write remaining items
            if (!writeRequests.isEmpty()) {
                batchWrite(tableName, writeRequests);
            }
        } catch (IOException e) {
            throw new RuntimeException("Import failed", e);
        }
    }
    
    private void batchWrite(String tableName, List<WriteRequest> requests) {
        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
            .requestItems(Map.of(tableName, requests))
            .build();
        
        dynamoDbClient.batchWriteItem(request);
    }
}
```

### Store Large Objects in S3

```java
@Service
public class LargeObjectService {
    
    @Autowired
    private S3Client s3Client;
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Store large files in S3, reference in DynamoDB
    public void saveDocument(String documentId, byte[] content, Map<String, String> metadata) {
        // Upload to S3
        String s3Key = "documents/" + documentId;
        PutObjectRequest s3Request = PutObjectRequest.builder()
            .bucket("my-documents-bucket")
            .key(s3Key)
            .build();
        
        s3Client.putObject(s3Request, RequestBody.fromBytes(content));
        
        // Store metadata in DynamoDB
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("documentId", AttributeValue.builder().s(documentId).build());
        item.put("s3Bucket", AttributeValue.builder().s("my-documents-bucket").build());
        item.put("s3Key", AttributeValue.builder().s(s3Key).build());
        item.put("size", AttributeValue.builder().n(String.valueOf(content.length)).build());
        metadata.forEach((k, v) -> item.put(k, AttributeValue.builder().s(v).build()));
        
        PutItemRequest dynamoRequest = PutItemRequest.builder()
            .tableName("Documents")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(dynamoRequest);
    }
    
    public byte[] getDocument(String documentId) {
        // Get metadata from DynamoDB
        GetItemResponse dynamoResponse = dynamoDbClient.getItem(GetItemRequest.builder()
            .tableName("Documents")
            .key(Map.of("documentId", AttributeValue.builder().s(documentId).build()))
            .build());
        
        String bucket = dynamoResponse.item().get("s3Bucket").s();
        String key = dynamoResponse.item().get("s3Key").s();
        
        // Download from S3
        GetObjectRequest s3Request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
        
        return s3Client.getObjectAsBytes(s3Request).asByteArray();
    }
}
```

---

## Kinesis Integration

### DynamoDB Streams to Kinesis

```java
@Component
public class StreamToKinesisProcessor implements RequestHandler<DynamodbEvent, String> {
    
    private final KinesisClient kinesisClient = KinesisClient.create();
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        List<PutRecordsRequestEntry> records = new ArrayList<>();
        
        for (DynamodbStreamRecord streamRecord : event.getRecords()) {
            String data = convertToJson(streamRecord);
            
            PutRecordsRequestEntry entry = PutRecordsRequestEntry.builder()
                .data(SdkBytes.fromUtf8String(data))
                .partitionKey(streamRecord.getDynamodb().getKeys().get("orderId").s())
                .build();
            
            records.add(entry);
        }
        
        // Batch put to Kinesis
        PutRecordsRequest request = PutRecordsRequest.builder()
            .streamName("order-events-stream")
            .records(records)
            .build();
        
        kinesisClient.putRecords(request);
        
        return "Processed " + records.size() + " records";
    }
}
```

### Kinesis Data Firehose to DynamoDB

```java
// Process Kinesis stream and write to DynamoDB
@Component
public class KinesisToDynamoDBProcessor implements RequestHandler<KinesisEvent, String> {
    
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    
    @Override
    public String handleRequest(KinesisEvent event, Context context) {
        List<WriteRequest> writeRequests = new ArrayList<>();
        
        for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
            String data = new String(record.getKinesis().getData().array());
            Map<String, AttributeValue> item = parseToItem(data);
            
            writeRequests.add(WriteRequest.builder()
                .putRequest(PutRequest.builder().item(item).build())
                .build());
            
            if (writeRequests.size() == 25) {
                batchWrite(writeRequests);
                writeRequests.clear();
            }
        }
        
        if (!writeRequests.isEmpty()) {
            batchWrite(writeRequests);
        }
        
        return "Success";
    }
    
    private void batchWrite(List<WriteRequest> requests) {
        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
            .requestItems(Map.of("Events", requests))
            .build();
        
        dynamoDbClient.batchWriteItem(request);
    }
}
```

---

## Step Functions Integration

### Orchestrate DynamoDB Operations

```json
{
  "Comment": "Order processing workflow",
  "StartAt": "CreateOrder",
  "States": {
    "CreateOrder": {
      "Type": "Task",
      "Resource": "arn:aws:states:::dynamodb:putItem",
      "Parameters": {
        "TableName": "Orders",
        "Item": {
          "orderId": {"S.$": "$.orderId"},
          "customerId": {"S.$": "$.customerId"},
          "status": {"S": "PENDING"}
        }
      },
      "Next": "ProcessPayment"
    },
    "ProcessPayment": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:ProcessPayment",
      "Next": "UpdateOrderStatus"
    },
    "UpdateOrderStatus": {
      "Type": "Task",
      "Resource": "arn:aws:states:::dynamodb:updateItem",
      "Parameters": {
        "TableName": "Orders",
        "Key": {
          "orderId": {"S.$": "$.orderId"}
        },
        "UpdateExpression": "SET #status = :status",
        "ExpressionAttributeNames": {
          "#status": "status"
        },
        "ExpressionAttributeValues": {
          ":status": {"S": "COMPLETED"}
        }
      },
      "End": true
    }
  }
}
```

### Saga Pattern with Step Functions

```java
// Step Functions state machine for distributed transaction
@Service
public class OrderSagaService {
    
    @Autowired
    private SfnClient stepFunctionsClient;
    
    public String startOrderSaga(OrderRequest request) {
        String input = convertToJson(request);
        
        StartExecutionRequest executionRequest = StartExecutionRequest.builder()
            .stateMachineArn("arn:aws:states:us-east-1:123456789012:stateMachine:OrderSaga")
            .input(input)
            .build();
        
        StartExecutionResponse response = stepFunctionsClient.startExecution(executionRequest);
        return response.executionArn();
    }
}
```

---

## EventBridge Integration

### DynamoDB Streams to EventBridge

```java
@Component
public class StreamToEventBridgeProcessor implements RequestHandler<DynamodbEvent, String> {
    
    private final EventBridgeClient eventBridgeClient = EventBridgeClient.create();
    
    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        List<PutEventsRequestEntry> entries = new ArrayList<>();
        
        for (DynamodbStreamRecord record : event.getRecords()) {
            String eventType = determineEventType(record);
            String detail = convertToJson(record.getDynamodb());
            
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .source("dynamodb.orders")
                .detailType(eventType)
                .detail(detail)
                .eventBusName("default")
                .build();
            
            entries.add(entry);
        }
        
        PutEventsRequest request = PutEventsRequest.builder()
            .entries(entries)
            .build();
        
        eventBridgeClient.putEvents(request);
        
        return "Published " + entries.size() + " events";
    }
    
    private String determineEventType(DynamodbStreamRecord record) {
        switch (record.getEventName()) {
            case "INSERT": return "OrderCreated";
            case "MODIFY": return "OrderUpdated";
            case "REMOVE": return "OrderDeleted";
            default: return "OrderEvent";
        }
    }
}
```

### EventBridge Rule for DynamoDB Events

```yaml
# CloudFormation template
OrderCreatedRule:
  Type: AWS::Events::Rule
  Properties:
    EventBusName: default
    EventPattern:
      source:
        - dynamodb.orders
      detail-type:
        - OrderCreated
    Targets:
      - Arn: !GetAtt NotificationFunction.Arn
        Id: NotificationTarget
      - Arn: !GetAtt AnalyticsFunction.Arn
        Id: AnalyticsTarget
```

---

## AppSync Integration

### GraphQL API with DynamoDB

```graphql
# Schema
type User {
    userId: ID!
    name: String!
    email: String!
    orders: [Order]
}

type Order {
    orderId: ID!
    customerId: ID!
    totalAmount: Float!
    status: String!
}

type Query {
    getUser(userId: ID!): User
    listOrders(customerId: ID!): [Order]
}

type Mutation {
    createUser(input: CreateUserInput!): User
    updateOrder(orderId: ID!, status: String!): Order
}
```

```java
// Resolver mapping template (VTL)
// GetUser resolver
{
    "version": "2017-02-28",
    "operation": "GetItem",
    "key": {
        "userId": $util.dynamodb.toDynamoDBJson($ctx.args.userId)
    }
}

// ListOrders resolver
{
    "version": "2017-02-28",
    "operation": "Query",
    "query": {
        "expression": "customerId = :customerId",
        "expressionValues": {
            ":customerId": $util.dynamodb.toDynamoDBJson($ctx.args.customerId)
        }
    }
}
```

### AppSync with Lambda Resolver

```java
@Component
public class AppSyncResolver {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    public Map<String, Object> resolve(Map<String, Object> event) {
        String fieldName = (String) event.get("fieldName");
        Map<String, Object> arguments = (Map<String, Object>) event.get("arguments");
        
        switch (fieldName) {
            case "getUser":
                return getUser((String) arguments.get("userId"));
            case "createUser":
                return createUser((Map<String, Object>) arguments.get("input"));
            default:
                throw new RuntimeException("Unknown field: " + fieldName);
        }
    }
    
    private Map<String, Object> getUser(String userId) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
            .tableName("Users")
            .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
            .build());
        
        return convertToMap(response.item());
    }
}
```

---

## Cognito Integration

### User Pool with DynamoDB

```java
@Service
public class CognitoUserService {
    
    @Autowired
    private CognitoIdentityProviderClient cognitoClient;
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    // Sync Cognito user to DynamoDB
    public void createUserProfile(String cognitoUserId, String email, String name) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(cognitoUserId).build());
        item.put("email", AttributeValue.builder().s(email).build());
        item.put("name", AttributeValue.builder().s(name).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName("UserProfiles")
            .item(item)
            .build();
        
        dynamoDbClient.putItem(request);
    }
    
    // Lambda trigger for Cognito post-confirmation
    public void handlePostConfirmation(Map<String, Object> event) {
        Map<String, Object> request = (Map<String, Object>) event.get("request");
        Map<String, String> userAttributes = (Map<String, String>) request.get("userAttributes");
        
        String userId = userAttributes.get("sub");
        String email = userAttributes.get("email");
        String name = userAttributes.get("name");
        
        createUserProfile(userId, email, name);
    }
}
```

### Fine-Grained Access Control

```java
// IAM policy for user-specific access
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:UpdateItem",
                "dynamodb:DeleteItem"
            ],
            "Resource": "arn:aws:dynamodb:us-east-1:123456789012:table/UserData",
            "Condition": {
                "ForAllValues:StringEquals": {
                    "dynamodb:LeadingKeys": ["${cognito-identity.amazonaws.com:sub}"]
                }
            }
        }
    ]
}
```

---

## Best Practices

### 1. Use Lambda Layers for Shared Code

```java
// Shared DynamoDB utilities in Lambda Layer
public class DynamoDBUtils {
    public static Map<String, AttributeValue> toItem(Object obj) {
        // Conversion logic
    }
    
    public static <T> T fromItem(Map<String, AttributeValue> item, Class<T> clazz) {
        // Conversion logic
    }
}
```

### 2. Implement Retry Logic

```java
@Service
public class ResilientDynamoDBService {
    
    private static final int MAX_RETRIES = 3;
    
    public <T> T executeWithRetry(Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                return operation.get();
            } catch (ProvisionedThroughputExceededException e) {
                if (++attempt >= MAX_RETRIES) throw e;
                sleep(100 * (long) Math.pow(2, attempt));
            }
        }
    }
}
```

### 3. Use Environment Variables

```java
public class Config {
    private static final String TABLE_NAME = System.getenv("DYNAMODB_TABLE_NAME");
    private static final String REGION = System.getenv("AWS_REGION");
}
```

### 4. Enable X-Ray Tracing

```java
@XRayEnabled
public class TracedDynamoDBService {
    // Automatically traced by X-Ray
}
```

---

## Next Steps

Continue to:
- **Part 5**: Enterprise Use Cases and Patterns
