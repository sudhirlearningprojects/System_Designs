# Digital Payment Platform - API Documentation

## Base URL
```
Production: https://api.digitalpayment.com
Development: http://localhost:8084
```

## Authentication
All API requests require authentication via JWT tokens in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## API Endpoints

### 1. Payment Initiation

**Endpoint:** `POST /api/payments/initiate`

**Description:** Initiates a new payment transaction with idempotency support.

**Request Body:**
```json
{
  "senderId": "user123",
  "receiverId": "user456",
  "amount": 1000.00,
  "type": "P2P",
  "paymentMethod": "UPI",
  "description": "Payment for dinner",
  "idempotencyKey": "unique-key-123"
}
```

**Response:**
```json
{
  "transactionId": "txn_abc123def456",
  "status": "SUCCESS",
  "message": "Payment initiated successfully"
}
```

**Status Codes:**
- `200 OK` - Payment initiated successfully
- `400 Bad Request` - Invalid request parameters
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - System error

---

### 2. Transaction Status

**Endpoint:** `GET /api/payments/status/{transactionId}`

**Description:** Retrieves the current status of a transaction.

**Path Parameters:**
- `transactionId` (string) - Unique transaction identifier

**Response:**
```json
{
  "transactionId": "txn_abc123def456",
  "status": "SUCCESS",
  "message": "Transaction completed successfully"
}
```

**Possible Status Values:**
- `PENDING` - Transaction is being processed
- `SUCCESS` - Transaction completed successfully
- `FAILED` - Transaction failed
- `REVERSED` - Transaction was reversed
- `EXPIRED` - Transaction expired

---

### 3. Wallet Balance

**Endpoint:** `GET /api/payments/balance/{userId}`

**Description:** Retrieves the current wallet balance for a user.

**Path Parameters:**
- `userId` (string) - Unique user identifier

**Response:**
```json
2500.75
```

**Status Codes:**
- `200 OK` - Balance retrieved successfully
- `404 Not Found` - User or wallet not found

---

### 4. Transaction History

**Endpoint:** `GET /api/payments/history/{userId}`

**Description:** Retrieves paginated transaction history for a user.

**Path Parameters:**
- `userId` (string) - Unique user identifier

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 0)
- `size` (integer, optional) - Page size (default: 20)

**Response:**
```json
{
  "content": [
    {
      "transactionId": "txn_abc123def456",
      "senderId": "user123",
      "receiverId": "user456",
      "amount": 1000.00,
      "currency": "INR",
      "type": "P2P",
      "status": "SUCCESS",
      "paymentMethod": "UPI",
      "description": "Payment for dinner",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:05Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

---

### 5. Payment Callback (Webhook)

**Endpoint:** `POST /api/payments/callback`

**Description:** Handles callbacks from Payment Service Providers (PSPs).

**Request Body:**
```json
{
  "transactionId": "txn_abc123def456",
  "pspTransactionId": "psp_xyz789",
  "status": "SUCCESS",
  "message": "Payment completed successfully"
}
```

**Response:**
```json
"Callback processed successfully"
```

**Status Codes:**
- `200 OK` - Callback processed successfully
- `400 Bad Request` - Invalid callback data

---

## Request/Response Examples

### Successful P2P Transfer
```bash
curl -X POST http://localhost:8084/api/payments/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -d '{
    "senderId": "user123",
    "receiverId": "user456", 
    "amount": 500.00,
    "type": "P2P",
    "paymentMethod": "WALLET",
    "description": "Lunch payment",
    "idempotencyKey": "lunch-payment-001"
  }'
```

### Check Transaction Status
```bash
curl -X GET http://localhost:8084/api/payments/status/txn_abc123def456 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### Get Wallet Balance
```bash
curl -X GET http://localhost:8084/api/payments/balance/user123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### Get Transaction History
```bash
curl -X GET "http://localhost:8084/api/payments/history/user123?page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## Error Handling

### Standard Error Response Format
```json
{
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Insufficient wallet balance for this transaction",
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/payments/initiate"
  }
}
```

### Common Error Codes
- `INSUFFICIENT_BALANCE` - Not enough funds in wallet
- `INVALID_PAYMENT_METHOD` - Unsupported payment method
- `FRAUD_DETECTED` - Transaction blocked by fraud detection
- `RATE_LIMIT_EXCEEDED` - Too many requests
- `DUPLICATE_TRANSACTION` - Duplicate idempotency key
- `USER_NOT_FOUND` - Invalid user ID
- `TRANSACTION_NOT_FOUND` - Invalid transaction ID

## Rate Limiting

### Limits per User
- Payment initiation: 50 requests per hour
- Status checks: 1000 requests per hour
- Balance checks: 500 requests per hour
- History requests: 100 requests per hour

### Rate Limit Headers
```
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1642248600
```

## Idempotency

### Idempotency Key Requirements
- Must be unique per request
- Maximum length: 255 characters
- Valid for 24 hours
- Recommended format: UUID or timestamp-based

### Idempotent Operations
- Payment initiation
- Wallet transfers
- Transaction status updates

## Webhook Security

### Signature Verification
All webhook requests include a signature header for verification:
```
X-Signature: sha256=<hmac_signature>
```

### Verification Process
1. Extract the signature from the header
2. Compute HMAC-SHA256 of the request body using your webhook secret
3. Compare the computed signature with the received signature

## SDK Examples

### Java SDK
```java
DigitalPaymentClient client = new DigitalPaymentClient("your-api-key");

PaymentRequest request = PaymentRequest.builder()
    .senderId("user123")
    .receiverId("user456")
    .amount(new BigDecimal("1000.00"))
    .type("P2P")
    .paymentMethod("UPI")
    .idempotencyKey(UUID.randomUUID().toString())
    .build();

PaymentResponse response = client.initiatePayment(request);
```

### Python SDK
```python
from digitalpayment import DigitalPaymentClient

client = DigitalPaymentClient(api_key="your-api-key")

response = client.initiate_payment(
    sender_id="user123",
    receiver_id="user456",
    amount=1000.00,
    type="P2P",
    payment_method="UPI",
    idempotency_key=str(uuid.uuid4())
)
```

This API documentation provides comprehensive guidance for integrating with the digital payment platform, ensuring secure and reliable payment processing.