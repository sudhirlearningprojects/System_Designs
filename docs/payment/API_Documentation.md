# Payment Service - API Documentation

## Base URL
```
https://api.payment-service.com/v1
```

## Authentication
All API endpoints require JWT authentication via the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Idempotency
Payment operations require an idempotency key to prevent duplicate processing:
```
Idempotency-Key: <unique_key_16_to_255_chars>
```

## Core Payment Operations

### 1. Process Payment

**Endpoint:** `POST /payment/process`

**Description:** Process a new payment with exactly-once guarantee

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer <jwt_token>
Idempotency-Key: payment_12345_20240115_103000
```

**Request Body:**
```json
{
  "merchantId": "merchant_123",
  "userId": "user_456",
  "amount": 100.00,
  "currency": "USD",
  "paymentMethod": {
    "type": "CARD",
    "cardToken": "tok_1234567890abcdef"
  },
  "metadata": {
    "orderId": "order_789",
    "description": "Product purchase",
    "customerEmail": "customer@example.com"
  }
}
```

**Response (Success - 200):**
```json
{
  "transactionId": "txn_abc123def456",
  "status": "COMPLETED",
  "amount": 100.00,
  "currency": "USD",
  "processorTransactionId": "pi_1234567890",
  "createdAt": "2024-01-15T10:30:00Z",
  "completedAt": "2024-01-15T10:30:02Z"
}
```

**Response (Processing - 202):**
```json
{
  "transactionId": "txn_abc123def456",
  "status": "PROCESSING",
  "amount": 100.00,
  "currency": "USD",
  "createdAt": "2024-01-15T10:30:00Z",
  "estimatedCompletionTime": "2024-01-15T10:30:30Z"
}
```

**Response (Failure - 400):**
```json
{
  "error": {
    "code": "PAYMENT_FAILED",
    "message": "Insufficient funds",
    "transactionId": "txn_abc123def456",
    "details": {
      "processorCode": "card_declined",
      "processorMessage": "Your card was declined"
    }
  }
}
```

### 2. Get Transaction Status

**Endpoint:** `GET /payment/transactions/{transactionId}`

**Description:** Retrieve the current status of a payment transaction

**Request:**
```http
GET /payment/transactions/txn_abc123def456
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "transactionId": "txn_abc123def456",
  "status": "COMPLETED",
  "amount": 100.00,
  "currency": "USD",
  "paymentMethod": "CARD",
  "processor": "STRIPE",
  "processorTransactionId": "pi_1234567890",
  "merchantId": "merchant_123",
  "userId": "user_456",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:02Z",
  "completedAt": "2024-01-15T10:30:02Z"
}
```

### 3. Process Refund

**Endpoint:** `POST /payment/refund`

**Description:** Process a full or partial refund for a completed payment

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer <jwt_token>
Idempotency-Key: refund_12345_20240115_110000
```

**Request Body:**
```json
{
  "originalTransactionId": "txn_abc123def456",
  "amount": 50.00,
  "reason": "Customer request",
  "metadata": {
    "refundRequestId": "ref_789",
    "customerServiceTicket": "CS-12345"
  }
}
```

**Response:**
```json
{
  "refundId": "ref_xyz789abc123",
  "originalTransactionId": "txn_abc123def456",
  "status": "COMPLETED",
  "amount": 50.00,
  "currency": "USD",
  "processorRefundId": "re_1234567890",
  "reason": "Customer request",
  "createdAt": "2024-01-15T11:00:00Z",
  "completedAt": "2024-01-15T11:00:03Z"
}
```

### 4. List Transactions

**Endpoint:** `GET /payment/transactions`

**Description:** List transactions with filtering and pagination

**Query Parameters:**
- `merchantId` (optional): Filter by merchant
- `status` (optional): Filter by status (PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED)
- `startDate` (optional): Filter transactions after this date (ISO 8601)
- `endDate` (optional): Filter transactions before this date (ISO 8601)
- `limit` (optional): Number of results (default: 50, max: 100)
- `offset` (optional): Pagination offset (default: 0)

**Request:**
```http
GET /payment/transactions?merchantId=merchant_123&status=COMPLETED&limit=20&offset=0
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "transactions": [
    {
      "transactionId": "txn_abc123def456",
      "status": "COMPLETED",
      "amount": 100.00,
      "currency": "USD",
      "createdAt": "2024-01-15T10:30:00Z",
      "completedAt": "2024-01-15T10:30:02Z"
    }
  ],
  "pagination": {
    "limit": 20,
    "offset": 0,
    "total": 150,
    "hasMore": true
  }
}
```

## Webhook Endpoints

### 1. Payment Status Webhook

**Endpoint:** `POST /payment/webhooks/status`

**Description:** Receive payment status updates from external processors

**Request Body:**
```json
{
  "eventType": "payment.completed",
  "transactionId": "txn_abc123def456",
  "processorTransactionId": "pi_1234567890",
  "status": "COMPLETED",
  "timestamp": "2024-01-15T10:30:02Z",
  "processor": "STRIPE",
  "signature": "webhook_signature_hash"
}
```

## Health and Monitoring

### 1. Health Check

**Endpoint:** `GET /payment/health`

**Description:** Service health status

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "components": {
    "database": {
      "status": "UP",
      "responseTime": "15ms"
    },
    "redis": {
      "status": "UP",
      "responseTime": "2ms"
    },
    "kafka": {
      "status": "UP",
      "responseTime": "5ms"
    },
    "externalProcessors": {
      "stripe": {
        "status": "UP",
        "responseTime": "120ms"
      },
      "paypal": {
        "status": "DOWN",
        "responseTime": "timeout"
      }
    }
  }
}
```

### 2. Metrics

**Endpoint:** `GET /payment/metrics`

**Description:** Service metrics in Prometheus format

**Response:**
```
# HELP payment_transactions_total Total number of payment transactions
# TYPE payment_transactions_total counter
payment_transactions_total{status="completed"} 1250
payment_transactions_total{status="failed"} 45

# HELP payment_processing_duration_seconds Payment processing duration
# TYPE payment_processing_duration_seconds histogram
payment_processing_duration_seconds_bucket{le="0.1"} 100
payment_processing_duration_seconds_bucket{le="0.5"} 850
payment_processing_duration_seconds_bucket{le="1.0"} 1200
```

## Error Responses

### Standard Error Format
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "transactionId": "txn_abc123def456",
    "timestamp": "2024-01-15T10:30:00Z",
    "details": {
      "field": "Additional error details"
    }
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_IDEMPOTENCY_KEY` | 400 | Idempotency key is missing or invalid |
| `DUPLICATE_TRANSACTION` | 409 | Transaction already processed |
| `INSUFFICIENT_FUNDS` | 400 | Payment declined due to insufficient funds |
| `INVALID_PAYMENT_METHOD` | 400 | Payment method is invalid or expired |
| `PROCESSOR_UNAVAILABLE` | 503 | External payment processor is unavailable |
| `TRANSACTION_NOT_FOUND` | 404 | Transaction ID not found |
| `REFUND_NOT_ALLOWED` | 400 | Refund not allowed for this transaction |
| `AMOUNT_EXCEEDS_LIMIT` | 400 | Transaction amount exceeds limits |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Internal server error |

## Rate Limiting

API endpoints are rate limited per merchant:

- **Payment Processing**: 100 requests per minute
- **Transaction Queries**: 1000 requests per minute
- **Refund Processing**: 50 requests per minute

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248660
```

## Security Requirements

### 1. TLS/SSL
- All API calls must use HTTPS (TLS 1.3)
- Certificate pinning recommended for mobile clients

### 2. Authentication
- JWT tokens with RS256 signing
- Token expiration: 1 hour
- Refresh tokens for long-lived sessions

### 3. PCI DSS Compliance
- Never send raw card data to payment endpoints
- Use tokenized payment methods only
- Implement proper data encryption

### 4. Request Signing (Optional)
For high-security environments, implement request signing:

```http
X-Signature: sha256=<hmac_signature>
X-Timestamp: 1642248600
```

## SDK Examples

### JavaScript/Node.js
```javascript
const paymentClient = new PaymentClient({
  apiKey: 'your_api_key',
  baseUrl: 'https://api.payment-service.com/v1'
});

const payment = await paymentClient.processPayment({
  merchantId: 'merchant_123',
  amount: 100.00,
  currency: 'USD',
  paymentMethod: {
    type: 'CARD',
    cardToken: 'tok_1234567890'
  }
}, {
  idempotencyKey: 'payment_12345_' + Date.now()
});
```

### Python
```python
from payment_client import PaymentClient

client = PaymentClient(
    api_key='your_api_key',
    base_url='https://api.payment-service.com/v1'
)

payment = client.process_payment(
    merchant_id='merchant_123',
    amount=100.00,
    currency='USD',
    payment_method={
        'type': 'CARD',
        'card_token': 'tok_1234567890'
    },
    idempotency_key=f'payment_12345_{int(time.time())}'
)
```

### Java
```java
PaymentClient client = PaymentClient.builder()
    .apiKey("your_api_key")
    .baseUrl("https://api.payment-service.com/v1")
    .build();

PaymentRequest request = PaymentRequest.builder()
    .merchantId("merchant_123")
    .amount(new BigDecimal("100.00"))
    .currency("USD")
    .paymentMethod(PaymentMethod.card("tok_1234567890"))
    .build();

PaymentResponse response = client.processPayment(request, 
    "payment_12345_" + System.currentTimeMillis());
```