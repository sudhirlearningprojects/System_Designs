# Deployment Guide - Smart Receipt Analyzer

## Prerequisites

### Required Tools
- AWS Account with admin access
- AWS CLI v2.x installed and configured
- Python 3.11+
- Node.js 18+ (for frontend)
- Git

### AWS CLI Configuration
```bash
aws configure
# AWS Access Key ID: YOUR_ACCESS_KEY
# AWS Secret Access Key: YOUR_SECRET_KEY
# Default region: us-east-1
# Default output format: json
```

## Step-by-Step Deployment

### Step 1: Clone Repository
```bash
git clone https://github.com/sudhir512kj/receipt-analyzer.git
cd receipt-analyzer
```

### Step 2: Set Environment Variables
```bash
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=us-east-1
export PROJECT_NAME=receipt-analyzer
export BUCKET_NAME=${PROJECT_NAME}-${AWS_ACCOUNT_ID}
```

### Step 3: Create S3 Bucket
```bash
# Create bucket
aws s3 mb s3://${BUCKET_NAME} --region ${AWS_REGION}

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket ${BUCKET_NAME} \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket ${BUCKET_NAME} \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      },
      "BucketKeyEnabled": true
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket ${BUCKET_NAME} \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Enable CORS
aws s3api put-bucket-cors \
  --bucket ${BUCKET_NAME} \
  --cors-configuration file://config/s3-cors.json
```

**config/s3-cors.json:**
```json
{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3000
  }]
}
```

### Step 4: Create DynamoDB Table
```bash
aws dynamodb create-table \
  --table-name expenses \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes '[
    {
      "IndexName": "CategoryIndex",
      "KeySchema": [
        {"AttributeName": "GSI1PK", "KeyType": "HASH"},
        {"AttributeName": "GSI1SK", "KeyType": "RANGE"}
      ],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' \
  --billing-mode PAY_PER_REQUEST \
  --stream-specification StreamEnabled=true,StreamViewType=NEW_IMAGE \
  --tags Key=Project,Value=${PROJECT_NAME}

# Enable encryption
aws dynamodb update-table \
  --table-name expenses \
  --sse-specification Enabled=true,SSEType=KMS

# Enable point-in-time recovery
aws dynamodb update-continuous-backups \
  --table-name expenses \
  --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true

# Wait for table to be active
aws dynamodb wait table-exists --table-name expenses
```

### Step 5: Create SNS Topic
```bash
# Create topic
TOPIC_ARN=$(aws sns create-topic \
  --name budget-alerts \
  --tags Key=Project,Value=${PROJECT_NAME} \
  --query TopicArn --output text)

echo "Topic ARN: ${TOPIC_ARN}"

# Subscribe email
aws sns subscribe \
  --topic-arn ${TOPIC_ARN} \
  --protocol email \
  --notification-endpoint your-email@example.com

# Confirm subscription via email
```

### Step 6: Create IAM Roles

#### Receipt Processor Role
```bash
# Create trust policy
cat > /tmp/lambda-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "lambda.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}
EOF

# Create role
aws iam create-role \
  --role-name ReceiptProcessorRole \
  --assume-role-policy-document file:///tmp/lambda-trust-policy.json

# Attach policies
aws iam attach-role-policy \
  --role-name ReceiptProcessorRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Create custom policy
cat > /tmp/receipt-processor-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject"],
      "Resource": "arn:aws:s3:::${BUCKET_NAME}/*"
    },
    {
      "Effect": "Allow",
      "Action": ["textract:DetectDocumentText", "textract:AnalyzeDocument"],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["dynamodb:PutItem", "dynamodb:GetItem", "dynamodb:Query"],
      "Resource": [
        "arn:aws:dynamodb:${AWS_REGION}:${AWS_ACCOUNT_ID}:table/expenses",
        "arn:aws:dynamodb:${AWS_REGION}:${AWS_ACCOUNT_ID}:table/expenses/index/*"
      ]
    }
  ]
}
EOF

aws iam put-role-policy \
  --role-name ReceiptProcessorRole \
  --policy-name ReceiptProcessorPolicy \
  --policy-document file:///tmp/receipt-processor-policy.json
```

#### Budget Alert Role
```bash
aws iam create-role \
  --role-name BudgetAlertRole \
  --assume-role-policy-document file:///tmp/lambda-trust-policy.json

aws iam attach-role-policy \
  --role-name BudgetAlertRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

cat > /tmp/budget-alert-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetRecords",
        "dynamodb:GetShardIterator",
        "dynamodb:DescribeStream",
        "dynamodb:ListStreams",
        "dynamodb:Query"
      ],
      "Resource": "arn:aws:dynamodb:${AWS_REGION}:${AWS_ACCOUNT_ID}:table/expenses/stream/*"
    },
    {
      "Effect": "Allow",
      "Action": "sns:Publish",
      "Resource": "${TOPIC_ARN}"
    }
  ]
}
EOF

aws iam put-role-policy \
  --role-name BudgetAlertRole \
  --policy-name BudgetAlertPolicy \
  --policy-document file:///tmp/budget-alert-policy.json
```

### Step 7: Deploy Lambda Functions

#### Lambda 1: Receipt Processor
```bash
cd lambda/receipt-processor

# Install dependencies
pip install -r requirements.txt -t .

# Package
zip -r function.zip . -x "*.pyc" -x "__pycache__/*"

# Create function
aws lambda create-function \
  --function-name receipt-processor \
  --runtime python3.11 \
  --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/ReceiptProcessorRole \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://function.zip \
  --timeout 60 \
  --memory-size 512 \
  --environment Variables="{BUCKET_NAME=${BUCKET_NAME},TABLE_NAME=expenses}" \
  --tags Project=${PROJECT_NAME}

# Add S3 trigger permission
aws lambda add-permission \
  --function-name receipt-processor \
  --statement-id s3-trigger \
  --action lambda:InvokeFunction \
  --principal s3.amazonaws.com \
  --source-arn arn:aws:s3:::${BUCKET_NAME}

# Configure S3 event notification
aws s3api put-bucket-notification-configuration \
  --bucket ${BUCKET_NAME} \
  --notification-configuration '{
    "LambdaFunctionConfigurations": [{
      "LambdaFunctionArn": "arn:aws:lambda:'${AWS_REGION}':'${AWS_ACCOUNT_ID}':function:receipt-processor",
      "Events": ["s3:ObjectCreated:*"],
      "Filter": {
        "Key": {
          "FilterRules": [{"Name": "prefix", "Value": "uploads/"}]
        }
      }
    }]
  }'

cd ../..
```

#### Lambda 2: Budget Alert
```bash
cd lambda/budget-alert

pip install -r requirements.txt -t .
zip -r function.zip .

# Get DynamoDB Stream ARN
STREAM_ARN=$(aws dynamodb describe-table \
  --table-name expenses \
  --query Table.LatestStreamArn --output text)

aws lambda create-function \
  --function-name budget-alert \
  --runtime python3.11 \
  --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/BudgetAlertRole \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://function.zip \
  --timeout 30 \
  --memory-size 256 \
  --environment Variables="{TABLE_NAME=expenses,TOPIC_ARN=${TOPIC_ARN}}" \
  --tags Project=${PROJECT_NAME}

# Create event source mapping
aws lambda create-event-source-mapping \
  --function-name budget-alert \
  --event-source-arn ${STREAM_ARN} \
  --starting-position LATEST \
  --batch-size 10

cd ../..
```

#### Lambda 3: Query API
```bash
cd lambda/query-api

pip install -r requirements.txt -t .
zip -r function.zip .

aws lambda create-function \
  --function-name query-api \
  --runtime python3.11 \
  --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/ReceiptProcessorRole \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://function.zip \
  --timeout 30 \
  --memory-size 256 \
  --environment Variables="{TABLE_NAME=expenses}" \
  --tags Project=${PROJECT_NAME}

cd ../..
```

### Step 8: Create API Gateway
```bash
# Create REST API
API_ID=$(aws apigateway create-rest-api \
  --name receipt-analyzer-api \
  --description "Receipt Analyzer API" \
  --endpoint-configuration types=REGIONAL \
  --query id --output text)

echo "API ID: ${API_ID}"

# Get root resource
ROOT_ID=$(aws apigateway get-resources \
  --rest-api-id ${API_ID} \
  --query 'items[0].id' --output text)

# Create /expenses resource
EXPENSES_ID=$(aws apigateway create-resource \
  --rest-api-id ${API_ID} \
  --parent-id ${ROOT_ID} \
  --path-part expenses \
  --query id --output text)

# Create /expenses/{userId} resource
USER_ID_RESOURCE=$(aws apigateway create-resource \
  --rest-api-id ${API_ID} \
  --parent-id ${EXPENSES_ID} \
  --path-part '{userId}' \
  --query id --output text)

# Create GET method
aws apigateway put-method \
  --rest-api-id ${API_ID} \
  --resource-id ${USER_ID_RESOURCE} \
  --http-method GET \
  --authorization-type NONE

# Integrate with Lambda
aws apigateway put-integration \
  --rest-api-id ${API_ID} \
  --resource-id ${USER_ID_RESOURCE} \
  --http-method GET \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri arn:aws:apigateway:${AWS_REGION}:lambda:path/2015-03-31/functions/arn:aws:lambda:${AWS_REGION}:${AWS_ACCOUNT_ID}:function:query-api/invocations

# Grant API Gateway permission to invoke Lambda
aws lambda add-permission \
  --function-name query-api \
  --statement-id apigateway-invoke \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:${AWS_REGION}:${AWS_ACCOUNT_ID}:${API_ID}/*/*"

# Deploy API
aws apigateway create-deployment \
  --rest-api-id ${API_ID} \
  --stage-name prod

# Get API endpoint
API_ENDPOINT="https://${API_ID}.execute-api.${AWS_REGION}.amazonaws.com/prod"
echo "API Endpoint: ${API_ENDPOINT}"
```

### Step 9: Test Deployment

#### Upload Test Receipt
```bash
# Create test receipt
echo "Test receipt" > test-receipt.txt

# Upload to S3
aws s3 cp test-receipt.txt s3://${BUCKET_NAME}/uploads/test-user/test-receipt.txt

# Check Lambda logs
aws logs tail /aws/lambda/receipt-processor --follow
```

#### Query Expenses
```bash
curl ${API_ENDPOINT}/expenses/test-user
```

### Step 10: Configure Monitoring

#### CloudWatch Dashboard
```bash
aws cloudwatch put-dashboard \
  --dashboard-name receipt-analyzer \
  --dashboard-body file://config/dashboard.json
```

#### CloudWatch Alarms
```bash
# Lambda errors
aws cloudwatch put-metric-alarm \
  --alarm-name receipt-processor-errors \
  --alarm-description "Alert on Lambda errors" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --dimensions Name=FunctionName,Value=receipt-processor \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1

# DynamoDB throttles
aws cloudwatch put-metric-alarm \
  --alarm-name dynamodb-throttles \
  --metric-name UserErrors \
  --namespace AWS/DynamoDB \
  --dimensions Name=TableName,Value=expenses \
  --statistic Sum \
  --period 60 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold
```

## Cleanup

To delete all resources:

```bash
# Delete Lambda functions
aws lambda delete-function --function-name receipt-processor
aws lambda delete-function --function-name budget-alert
aws lambda delete-function --function-name query-api

# Delete API Gateway
aws apigateway delete-rest-api --rest-api-id ${API_ID}

# Delete DynamoDB table
aws dynamodb delete-table --table-name expenses

# Delete SNS topic
aws sns delete-topic --topic-arn ${TOPIC_ARN}

# Delete S3 bucket (must be empty)
aws s3 rm s3://${BUCKET_NAME} --recursive
aws s3 rb s3://${BUCKET_NAME}

# Delete IAM roles
aws iam delete-role-policy --role-name ReceiptProcessorRole --policy-name ReceiptProcessorPolicy
aws iam detach-role-policy --role-name ReceiptProcessorRole --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam delete-role --role-name ReceiptProcessorRole

aws iam delete-role-policy --role-name BudgetAlertRole --policy-name BudgetAlertPolicy
aws iam detach-role-policy --role-name BudgetAlertRole --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam delete-role --role-name BudgetAlertRole
```

## Troubleshooting

### Lambda Not Triggered by S3
```bash
# Check S3 event configuration
aws s3api get-bucket-notification-configuration --bucket ${BUCKET_NAME}

# Check Lambda permissions
aws lambda get-policy --function-name receipt-processor
```

### DynamoDB Access Denied
```bash
# Verify IAM role permissions
aws iam get-role-policy --role-name ReceiptProcessorRole --policy-name ReceiptProcessorPolicy
```

### API Gateway 403 Error
```bash
# Check Lambda permission
aws lambda get-policy --function-name query-api
```

## Next Steps

1. Set up CI/CD pipeline with GitHub Actions
2. Configure custom domain with Route 53
3. Add CloudFront CDN for S3 images
4. Implement authentication with Cognito
5. Deploy frontend application
