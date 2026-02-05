# API Documentation - Smart Receipt Analyzer

## Base URL
```
Production: https://api.receipt-analyzer.com/v1
Staging: https://staging-api.receipt-analyzer.com/v1
```

## Authentication

All API requests require authentication using JWT tokens.

### Get Access Token
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "userId": "user123"
}
```

### Using Token
```http
GET /expenses/user123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### 1. Upload Receipt

#### Get Pre-signed Upload URL
```http
POST /upload-url
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "user123",
  "fileName": "receipt.jpg",
  "contentType": "image/jpeg"
}
```

**Response:**
```json
{
  "uploadUrl": "https://receipts-bucket.s3.amazonaws.com/uploads/user123/uuid.jpg?X-Amz-Algorithm=...",
  "receiptId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "expiresIn": 300
}
```

#### Upload Image to S3
```http
PUT {uploadUrl}
Content-Type: image/jpeg
Body: <binary image data>
```

**Response:**
```
200 OK
```

### 2. Get Expenses

#### Get All Expenses for User
```http
GET /expenses/{userId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "expenses": [
    {
      "expenseId": "a1b2c3d4",
      "merchant": "Starbucks",
      "amount": 15.50,
      "currency": "USD",
      "date": "2024-01-15",
      "category": "Food & Dining",
      "items": ["Latte", "Croissant"],
      "receiptUrl": "s3://receipts/user123/receipt.jpg",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "total": 15.50,
  "count": 1
}
```

#### Get Expenses by Month
```http
GET /expenses/{userId}?month=2024-01
Authorization: Bearer {token}
```

**Query Parameters:**
- `month` (string): YYYY-MM format

**Response:**
```json
{
  "expenses": [...],
  "total": 450.75,
  "count": 23,
  "month": "2024-01"
}
```

#### Get Expenses by Category
```http
GET /expenses/{userId}?category=Food%20%26%20Dining
Authorization: Bearer {token}
```

**Query Parameters:**
- `category` (string): Category name

**Response:**
```json
{
  "expenses": [...],
  "total": 250.00,
  "count": 15,
  "category": "Food & Dining"
}
```

#### Get Single Expense
```http
GET /expenses/{userId}/{expenseId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "expenseId": "a1b2c3d4",
  "merchant": "Starbucks",
  "amount": 15.50,
  "currency": "USD",
  "date": "2024-01-15",
  "category": "Food & Dining",
  "items": ["Latte", "Croissant"],
  "receiptUrl": "s3://receipts/user123/receipt.jpg",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 3. Update Expense

```http
PUT /expenses/{userId}/{expenseId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "category": "Business Meals",
  "notes": "Client meeting"
}
```

**Response:**
```json
{
  "expenseId": "a1b2c3d4",
  "updated": true
}
```

### 4. Delete Expense

```http
DELETE /expenses/{userId}/{expenseId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "expenseId": "a1b2c3d4",
  "deleted": true
}
```

### 5. Analytics

#### Get Monthly Summary
```http
GET /analytics/{userId}?month=2024-01
Authorization: Bearer {token}
```

**Response:**
```json
{
  "month": "2024-01",
  "total": 1850.75,
  "transactionCount": 45,
  "categories": {
    "Food & Dining": 450.00,
    "Transportation": 320.50,
    "Shopping": 680.25,
    "Gas & Fuel": 200.00,
    "Other": 200.00
  },
  "dailyAverage": 59.70,
  "topMerchants": [
    {"merchant": "Amazon", "amount": 350.00, "count": 5},
    {"merchant": "Starbucks", "amount": 180.00, "count": 12}
  ]
}
```

#### Get Category Breakdown
```http
GET /analytics/{userId}/categories?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {token}
```

**Response:**
```json
{
  "categories": [
    {
      "category": "Food & Dining",
      "amount": 450.00,
      "percentage": 24.3,
      "count": 15
    },
    {
      "category": "Shopping",
      "amount": 680.25,
      "percentage": 36.7,
      "count": 8
    }
  ],
  "total": 1850.75
}
```

#### Get Spending Trends
```http
GET /analytics/{userId}/trends?months=6
Authorization: Bearer {token}
```

**Response:**
```json
{
  "trends": [
    {"month": "2023-08", "total": 1650.00},
    {"month": "2023-09", "total": 1720.50},
    {"month": "2023-10", "total": 1890.25},
    {"month": "2023-11", "total": 1950.00},
    {"month": "2023-12", "total": 2100.75},
    {"month": "2024-01", "total": 1850.75}
  ],
  "average": 1860.38,
  "trend": "decreasing"
}
```

### 6. Budget Management

#### Get Budget Settings
```http
GET /budget/{userId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "userId": "user123",
  "budgets": {
    "Total": 2000,
    "Food & Dining": 500,
    "Transportation": 300,
    "Shopping": 400
  },
  "alertThreshold": 0.9
}
```

#### Update Budget
```http
PUT /budget/{userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "budgets": {
    "Total": 2500,
    "Food & Dining": 600
  },
  "alertThreshold": 0.85
}
```

**Response:**
```json
{
  "userId": "user123",
  "updated": true
}
```

### 7. Export

#### Export to CSV
```http
GET /export/{userId}/csv?month=2024-01
Authorization: Bearer {token}
```

**Response:**
```csv
Date,Merchant,Category,Amount,Currency
2024-01-15,Starbucks,Food & Dining,15.50,USD
2024-01-16,Uber,Transportation,25.00,USD
```

#### Export to PDF
```http
GET /export/{userId}/pdf?month=2024-01
Authorization: Bearer {token}
```

**Response:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="expenses-2024-01.pdf"

<PDF binary data>
```

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "Missing required field: userId",
  "code": "INVALID_REQUEST"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "code": "UNAUTHORIZED"
}
```

### 403 Forbidden
```json
{
  "error": "Forbidden",
  "message": "You don't have permission to access this resource",
  "code": "FORBIDDEN"
}
```

### 404 Not Found
```json
{
  "error": "Not found",
  "message": "Expense not found",
  "code": "NOT_FOUND"
}
```

### 429 Too Many Requests
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later",
  "code": "RATE_LIMIT_EXCEEDED",
  "retryAfter": 60
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred",
  "code": "INTERNAL_ERROR",
  "requestId": "abc123"
}
```

## Rate Limits

| Endpoint | Rate Limit |
|----------|-----------|
| Upload | 100 requests/hour |
| Query | 1000 requests/hour |
| Analytics | 500 requests/hour |
| Export | 50 requests/hour |

## Webhooks

Subscribe to events via webhooks.

### Register Webhook
```http
POST /webhooks
Authorization: Bearer {token}
Content-Type: application/json

{
  "url": "https://your-app.com/webhook",
  "events": ["expense.created", "budget.exceeded"]
}
```

### Webhook Events

#### expense.created
```json
{
  "event": "expense.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "expenseId": "a1b2c3d4",
    "userId": "user123",
    "amount": 15.50,
    "merchant": "Starbucks"
  }
}
```

#### budget.exceeded
```json
{
  "event": "budget.exceeded",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "userId": "user123",
    "category": "Food & Dining",
    "limit": 500,
    "current": 520.50
  }
}
```

## SDKs

### JavaScript/TypeScript
```bash
npm install @receipt-analyzer/sdk
```

```javascript
import { ReceiptAnalyzer } from '@receipt-analyzer/sdk';

const client = new ReceiptAnalyzer({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.receipt-analyzer.com/v1'
});

// Upload receipt
const result = await client.uploadReceipt('user123', imageFile);

// Get expenses
const expenses = await client.getExpenses('user123', { month: '2024-01' });

// Get analytics
const analytics = await client.getAnalytics('user123', { month: '2024-01' });
```

### Python
```bash
pip install receipt-analyzer
```

```python
from receipt_analyzer import ReceiptAnalyzer

client = ReceiptAnalyzer(
    api_key='your-api-key',
    base_url='https://api.receipt-analyzer.com/v1'
)

# Upload receipt
result = client.upload_receipt('user123', 'receipt.jpg')

# Get expenses
expenses = client.get_expenses('user123', month='2024-01')

# Get analytics
analytics = client.get_analytics('user123', month='2024-01')
```

## Postman Collection

Import our Postman collection for easy testing:

```
https://api.receipt-analyzer.com/postman/collection.json
```

## OpenAPI Specification

View our OpenAPI 3.0 specification:

```
https://api.receipt-analyzer.com/openapi.yaml
```
