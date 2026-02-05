# Smart Receipt Analyzer - System Design

## Table of Contents
1. [Requirements](#requirements)
2. [High-Level Design](#high-level-design)
3. [Low-Level Design](#low-level-design)
4. [Data Flow](#data-flow)
5. [Implementation](#implementation)

## Requirements

### Functional Requirements
1. **Receipt Upload**: Users can upload receipt images (JPG, PNG, PDF)
2. **Text Extraction**: Automatically extract merchant, amount, date, items from receipts
3. **Expense Storage**: Store structured expense data in database
4. **Categorization**: Auto-categorize expenses (Food, Transport, Shopping, etc.)
5. **Budget Alerts**: Notify users when spending exceeds monthly budget
6. **Query API**: Retrieve expenses by date, category, merchant
7. **Analytics**: Monthly summaries, category breakdowns, trends

### Non-Functional Requirements
1. **Scalability**: Handle 10K+ receipts/day
2. **Availability**: 99.9% uptime
3. **Latency**: Receipt processing < 5 seconds
4. **Cost**: < $0.01 per receipt processed
5. **Security**: Encrypted storage, secure API access
6. **Accuracy**: 95%+ OCR accuracy

### Scale Estimates
- **Users**: 100K active users
- **Receipts**: 1000 receipts/day = 30K/month
- **Storage**: 2MB avg per receipt = 60GB/month
- **API Requests**: 50K queries/day

## High-Level Design

### Architecture Components

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │  Mobile  │  │   Web    │  │  Alexa   │                  │
│  │   App    │  │   App    │  │  Skill   │                  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘                  │
└───────┼─────────────┼─────────────┼────────────────────────┘
        │             │             │
        └─────────────┴─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │      API Gateway           │
        │  (Authentication, Rate     │
        │   Limiting, Routing)       │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────────────────────┐
        │           Application Layer                │
        │                                            │
        │  ┌──────────────┐    ┌─────────────────┐ │
        │  │   Lambda 1   │    │    Lambda 2     │ │
        │  │   Receipt    │    │  Budget Alert   │ │
        │  │  Processor   │    │    Service      │ │
        │  └──────┬───────┘    └────────┬────────┘ │
        │         │                     │          │
        │  ┌──────▼───────┐    ┌───────▼────────┐ │
        │  │   Lambda 3   │    │    Lambda 4    │ │
        │  │  Query API   │    │  Analytics     │ │
        │  └──────────────┘    └────────────────┘ │
        └────────────────────────────────────────┘
                      │
        ┌─────────────▼─────────────────────────────┐
        │            Data Layer                      │
        │                                            │
        │  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
        │  │    S3    │  │ DynamoDB │  │  SNS    │ │
        │  │ (Images) │  │(Expenses)│  │(Alerts) │ │
        │  └──────────┘  └──────────┘  └─────────┘ │
        │                                            │
        │  ┌──────────┐  ┌──────────┐               │
        │  │Textract  │  │CloudWatch│               │
        │  │  (OCR)   │  │  (Logs)  │               │
        │  └──────────┘  └──────────┘               │
        └────────────────────────────────────────────┘
```

### Component Responsibilities

#### 1. API Gateway
- **Authentication**: Validate JWT tokens
- **Rate Limiting**: 1000 requests/min per user
- **Request Routing**: Route to appropriate Lambda
- **CORS**: Enable cross-origin requests

#### 2. Lambda Functions

**Lambda 1: Receipt Processor**
- Triggered by S3 upload event
- Download image from S3
- Call Textract for OCR
- Parse extracted text
- Store in DynamoDB
- Return processing status

**Lambda 2: Budget Alert Service**
- Triggered by DynamoDB Stream
- Calculate monthly total
- Compare against budget limit
- Send SNS notification if exceeded

**Lambda 3: Query API**
- Handle GET requests for expenses
- Query DynamoDB by user/date/category
- Return paginated results
- Cache frequent queries

**Lambda 4: Analytics Service**
- Generate monthly reports
- Calculate category breakdowns
- Identify spending trends
- Store reports in S3

#### 3. Data Stores

**S3 Bucket Structure:**
```
receipts-bucket/
├── uploads/
│   └── {userId}/
│       └── {receiptId}.jpg
├── processed/
│   └── {userId}/
│       └── {receiptId}.jpg
└── reports/
    └── {userId}/
        └── {month}-report.pdf
```

**DynamoDB Schema:**
```
Table: expenses
PK: USER#user123
SK: EXPENSE#2024-01-15#uuid

Attributes:
- merchant (String)
- amount (Number)
- currency (String)
- date (String)
- category (String)
- items (List)
- receiptUrl (String)
- createdAt (String)

GSI: CategoryIndex
PK: CATEGORY#Food & Dining
SK: 2024-01-15
```

## Low-Level Design

### Lambda 1: Receipt Processor

```python
import boto3
import json
from datetime import datetime
import uuid
import re

s3 = boto3.client('s3')
textract = boto3.client('textract')
dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('expenses')

def lambda_handler(event, context):
    try:
        # Extract S3 details
        bucket = event['Records'][0]['s3']['bucket']['name']
        key = event['Records'][0]['s3']['object']['key']
        user_id = extract_user_id(key)
        
        # Validate file type
        if not key.lower().endswith(('.jpg', '.jpeg', '.png', '.pdf')):
            return error_response('Invalid file type')
        
        # Extract text using Textract
        response = textract.detect_document_text(
            Document={'S3Object': {'Bucket': bucket, 'Name': key}}
        )
        
        # Parse receipt data
        text_lines = [block['Text'] for block in response['Blocks'] 
                      if block['BlockType'] == 'LINE']
        
        receipt_data = parse_receipt(text_lines)
        receipt_data['userId'] = user_id
        receipt_data['receiptUrl'] = f's3://{bucket}/{key}'
        
        # Store in DynamoDB
        expense_id = str(uuid.uuid4())
        date = receipt_data.get('date', datetime.now().strftime('%Y-%m-%d'))
        
        item = {
            'PK': f'USER#{user_id}',
            'SK': f'EXPENSE#{date}#{expense_id}',
            'expenseId': expense_id,
            'merchant': receipt_data['merchant'],
            'amount': receipt_data['amount'],
            'currency': receipt_data.get('currency', 'USD'),
            'date': date,
            'category': categorize(receipt_data['merchant']),
            'items': receipt_data.get('items', []),
            'receiptUrl': receipt_data['receiptUrl'],
            'createdAt': datetime.now().isoformat(),
            'GSI1PK': f"CATEGORY#{categorize(receipt_data['merchant'])}",
            'GSI1SK': date
        }
        
        table.put_item(Item=item)
        
        return success_response({
            'expenseId': expense_id,
            'amount': receipt_data['amount'],
            'merchant': receipt_data['merchant']
        })
        
    except Exception as e:
        print(f"Error: {str(e)}")
        return error_response(str(e))

def extract_user_id(key):
    # uploads/user123/receipt.jpg -> user123
    parts = key.split('/')
    return parts[1] if len(parts) > 1 else 'unknown'

def parse_receipt(lines):
    merchant = lines[0] if lines else 'Unknown'
    amount = extract_amount(lines)
    date = extract_date(lines)
    items = extract_items(lines)
    
    return {
        'merchant': merchant,
        'amount': amount,
        'date': date,
        'items': items
    }

def extract_amount(lines):
    for line in lines:
        # Match patterns like $15.50, 15.50, $15
        match = re.search(r'\$?(\d+\.?\d*)', line)
        if match and 'total' in line.lower():
            return float(match.group(1))
    
    # Fallback: find largest number
    amounts = []
    for line in lines:
        match = re.search(r'\$?(\d+\.?\d*)', line)
        if match:
            amounts.append(float(match.group(1)))
    
    return max(amounts) if amounts else 0.0

def extract_date(lines):
    for line in lines:
        # Match MM/DD/YYYY or DD-MM-YYYY
        match = re.search(r'(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})', line)
        if match:
            return normalize_date(match.group(1))
    
    return datetime.now().strftime('%Y-%m-%d')

def normalize_date(date_str):
    # Convert to YYYY-MM-DD format
    try:
        from dateutil import parser
        dt = parser.parse(date_str)
        return dt.strftime('%Y-%m-%d')
    except:
        return datetime.now().strftime('%Y-%m-%d')

def extract_items(lines):
    items = []
    for line in lines:
        # Skip lines with totals, dates, addresses
        if any(word in line.lower() for word in ['total', 'subtotal', 'tax', 'street', 'ave']):
            continue
        # Look for item lines (usually have price)
        if re.search(r'\$?\d+\.?\d*', line):
            items.append(line)
    
    return items[:10]  # Limit to 10 items

def categorize(merchant):
    categories = {
        'starbucks': 'Food & Dining',
        'mcdonalds': 'Food & Dining',
        'uber': 'Transportation',
        'lyft': 'Transportation',
        'amazon': 'Shopping',
        'walmart': 'Shopping',
        'shell': 'Gas & Fuel',
        'chevron': 'Gas & Fuel',
        'cvs': 'Health & Pharmacy',
        'walgreens': 'Health & Pharmacy'
    }
    
    merchant_lower = merchant.lower()
    for key, category in categories.items():
        if key in merchant_lower:
            return category
    
    return 'Other'

def success_response(data):
    return {
        'statusCode': 200,
        'body': json.dumps(data),
        'headers': {'Content-Type': 'application/json'}
    }

def error_response(message):
    return {
        'statusCode': 500,
        'body': json.dumps({'error': message}),
        'headers': {'Content-Type': 'application/json'}
    }
```

### Lambda 2: Budget Alert Service

```python
import boto3
from decimal import Decimal
import json

dynamodb = boto3.resource('dynamodb')
sns = boto3.client('sns')
table = dynamodb.Table('expenses')

BUDGET_LIMITS = {
    'Food & Dining': 500,
    'Transportation': 300,
    'Shopping': 400,
    'Total': 2000
}

def lambda_handler(event, context):
    for record in event['Records']:
        if record['eventName'] == 'INSERT':
            process_new_expense(record['dynamodb']['NewImage'])
    
    return {'statusCode': 200}

def process_new_expense(new_image):
    user_id = new_image['PK']['S'].split('#')[1]
    amount = Decimal(new_image['amount']['N'])
    category = new_image['category']['S']
    date = new_image['date']['S']
    
    # Get current month
    month = date[:7]  # 2024-01
    
    # Query monthly expenses
    response = table.query(
        KeyConditionExpression='PK = :pk AND begins_with(SK, :month)',
        ExpressionAttributeValues={
            ':pk': f'USER#{user_id}',
            ':month': f'EXPENSE#{month}'
        }
    )
    
    # Calculate totals
    total = sum(Decimal(item['amount']) for item in response['Items'])
    category_total = sum(Decimal(item['amount']) for item in response['Items'] 
                         if item['category'] == category)
    
    # Check budget limits
    alerts = []
    
    if total > BUDGET_LIMITS['Total']:
        alerts.append(f"Monthly budget exceeded: ${total} / ${BUDGET_LIMITS['Total']}")
    
    if category in BUDGET_LIMITS and category_total > BUDGET_LIMITS[category]:
        alerts.append(f"{category} budget exceeded: ${category_total} / ${BUDGET_LIMITS[category]}")
    
    # Send alerts
    if alerts:
        send_alert(user_id, alerts, total)

def send_alert(user_id, alerts, total):
    message = f"Budget Alert for {user_id}\n\n"
    message += "\n".join(alerts)
    message += f"\n\nTotal spending this month: ${total}"
    
    sns.publish(
        TopicArn='arn:aws:sns:us-east-1:ACCOUNT_ID:budget-alerts',
        Subject='Budget Alert',
        Message=message
    )
```

### Lambda 3: Query API

```python
import boto3
import json
from decimal import Decimal

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('expenses')

def lambda_handler(event, context):
    http_method = event['httpMethod']
    path = event['path']
    
    if http_method == 'GET' and '/expenses/' in path:
        return get_expenses(event)
    elif http_method == 'GET' and '/analytics/' in path:
        return get_analytics(event)
    else:
        return error_response('Invalid endpoint', 404)

def get_expenses(event):
    user_id = event['pathParameters']['userId']
    query_params = event.get('queryStringParameters', {}) or {}
    
    month = query_params.get('month')
    category = query_params.get('category')
    
    if category:
        # Query by category using GSI
        response = table.query(
            IndexName='CategoryIndex',
            KeyConditionExpression='GSI1PK = :cat',
            ExpressionAttributeValues={':cat': f'CATEGORY#{category}'}
        )
    elif month:
        # Query by month
        response = table.query(
            KeyConditionExpression='PK = :pk AND begins_with(SK, :month)',
            ExpressionAttributeValues={
                ':pk': f'USER#{user_id}',
                ':month': f'EXPENSE#{month}'
            }
        )
    else:
        # Get all expenses
        response = table.query(
            KeyConditionExpression='PK = :pk',
            ExpressionAttributeValues={':pk': f'USER#{user_id}'}
        )
    
    expenses = response['Items']
    total = sum(Decimal(item['amount']) for item in expenses)
    
    return success_response({
        'expenses': json.loads(json.dumps(expenses, default=decimal_default)),
        'total': float(total),
        'count': len(expenses)
    })

def get_analytics(event):
    user_id = event['pathParameters']['userId']
    month = event['queryStringParameters'].get('month', '2024-01')
    
    response = table.query(
        KeyConditionExpression='PK = :pk AND begins_with(SK, :month)',
        ExpressionAttributeValues={
            ':pk': f'USER#{user_id}',
            ':month': f'EXPENSE#{month}'
        }
    )
    
    expenses = response['Items']
    
    # Calculate category breakdown
    categories = {}
    for expense in expenses:
        cat = expense['category']
        amount = Decimal(expense['amount'])
        categories[cat] = categories.get(cat, Decimal(0)) + amount
    
    total = sum(categories.values())
    
    return success_response({
        'month': month,
        'total': float(total),
        'categories': {k: float(v) for k, v in categories.items()},
        'transactionCount': len(expenses)
    })

def decimal_default(obj):
    if isinstance(obj, Decimal):
        return float(obj)
    raise TypeError

def success_response(data):
    return {
        'statusCode': 200,
        'body': json.dumps(data),
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        }
    }

def error_response(message, code=500):
    return {
        'statusCode': code,
        'body': json.dumps({'error': message}),
        'headers': {'Content-Type': 'application/json'}
    }
```

## Data Flow

### Receipt Upload Flow

```
1. User uploads receipt image via mobile app
   ↓
2. App requests pre-signed S3 URL from API Gateway
   ↓
3. Lambda generates pre-signed URL with 5-min expiry
   ↓
4. App uploads image directly to S3 using pre-signed URL
   ↓
5. S3 triggers Lambda (Receipt Processor) via event
   ↓
6. Lambda downloads image and calls Textract
   ↓
7. Textract returns extracted text
   ↓
8. Lambda parses text and extracts structured data
   ↓
9. Lambda stores expense in DynamoDB
   ↓
10. DynamoDB Stream triggers Budget Alert Lambda
   ↓
11. Budget Alert Lambda checks monthly total
   ↓
12. If budget exceeded, SNS sends notification
```

### Query Flow

```
1. User requests expenses via mobile app
   ↓
2. App sends GET request to API Gateway
   ↓
3. API Gateway validates JWT token
   ↓
4. API Gateway routes to Query Lambda
   ↓
5. Lambda queries DynamoDB
   ↓
6. DynamoDB returns expenses
   ↓
7. Lambda formats response
   ↓
8. API Gateway returns JSON to app
```

## Implementation

See code sections above for complete Lambda implementations.

### Deployment Steps

1. **Create S3 Bucket**
2. **Create DynamoDB Table with GSI**
3. **Create SNS Topic**
4. **Deploy Lambda Functions**
5. **Configure S3 Event Trigger**
6. **Enable DynamoDB Streams**
7. **Create API Gateway**
8. **Configure IAM Roles**

### Testing

```bash
# Upload test receipt
aws s3 cp test-receipt.jpg s3://receipts-bucket/uploads/user123/

# Query expenses
curl https://api.example.com/expenses/user123?month=2024-01

# Get analytics
curl https://api.example.com/analytics/user123?month=2024-01
```

## Performance Optimization

1. **Caching**: Cache frequent queries in Lambda memory
2. **Batch Processing**: Process multiple receipts in parallel
3. **Image Compression**: Compress images before Textract
4. **DynamoDB Optimization**: Use sparse indexes, batch writes
5. **Lambda Provisioned Concurrency**: Reduce cold starts

## Cost Optimization

1. **S3 Lifecycle**: Move old receipts to Glacier after 90 days
2. **DynamoDB On-Demand**: Pay only for actual usage
3. **Lambda Memory**: Optimize memory allocation (512MB optimal)
4. **Textract**: Use DetectDocumentText (cheaper) vs AnalyzeDocument
5. **CloudWatch Logs**: Set retention to 7 days
