# Probability Management System - API Documentation

## Base URL
```
Production: https://api.predictionmarket.com/v1
Staging: https://staging-api.predictionmarket.com/v1
Local: http://localhost:8099/api/v1
```

## Authentication
All authenticated endpoints require a Bearer token:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 1. Market APIs

### 1.1 Create Market
```http
POST /markets
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "question": "Will Bitcoin reach $100K by Dec 31, 2025?",
  "description": "Resolves YES if BTC/USD >= $100,000 on any major exchange",
  "marketType": "BINARY",
  "outcomes": ["YES", "NO"],
  "endDate": "2025-12-31T23:59:59Z",
  "liquidityParameter": 100.0,
  "resolutionSource": "CHAINLINK_BTC_USD",
  "category": "CRYPTO"
}

Response 201:
{
  "marketId": "550e8400-e29b-41d4-a716-446655440000",
  "question": "Will Bitcoin reach $100K by Dec 31, 2025?",
  "status": "OPEN",
  "outcomes": [
    {
      "outcomeId": "660e8400-e29b-41d4-a716-446655440001",
      "name": "YES",
      "probability": 0.5000,
      "outstandingShares": 0
    },
    {
      "outcomeId": "660e8400-e29b-41d4-a716-446655440002",
      "name": "NO",
      "probability": 0.5000,
      "outstandingShares": 0
    }
  ],
  "totalVolume": 0,
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### 1.2 Get Market Details
```http
GET /markets/{marketId}

Response 200:
{
  "marketId": "550e8400-e29b-41d4-a716-446655440000",
  "question": "Will Bitcoin reach $100K by Dec 31, 2025?",
  "description": "...",
  "marketType": "BINARY",
  "status": "OPEN",
  "endDate": "2025-12-31T23:59:59Z",
  "outcomes": [...],
  "totalVolume": 150000.00,
  "liquidityParameter": 100.0,
  "creatorId": "user-123",
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### 1.3 List Markets
```http
GET /markets?status=OPEN&category=CRYPTO&page=0&size=20&sort=volume,desc

Response 200:
{
  "markets": [
    {
      "marketId": "...",
      "question": "...",
      "status": "OPEN",
      "outcomes": [...],
      "totalVolume": 150000.00,
      "endDate": "2025-12-31T23:59:59Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 50,
  "totalElements": 1000
}
```

### 1.4 Get Market Depth
```http
GET /markets/{marketId}/depth?outcomeId={outcomeId}

Response 200:
{
  "marketId": "...",
  "outcomeId": "...",
  "bids": [
    {"price": 0.6500, "quantity": 100, "orders": 3},
    {"price": 0.6400, "quantity": 250, "orders": 5},
    {"price": 0.6300, "quantity": 500, "orders": 8}
  ],
  "asks": [
    {"price": 0.6600, "quantity": 150, "orders": 4},
    {"price": 0.6700, "quantity": 300, "orders": 6},
    {"price": 0.6800, "quantity": 450, "orders": 7}
  ],
  "spread": 0.0100,
  "midPrice": 0.6550
}
```

---

## 2. Trading APIs

### 2.1 Place Order
```http
POST /orders
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "marketId": "550e8400-e29b-41d4-a716-446655440000",
  "outcomeId": "660e8400-e29b-41d4-a716-446655440001",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 0.6500,
  "quantity": 100,
  "idempotencyKey": "order-12345"
}

Response 201:
{
  "orderId": "770e8400-e29b-41d4-a716-446655440003",
  "status": "PENDING",
  "filledQuantity": 0,
  "avgFillPrice": 0,
  "remainingQuantity": 100,
  "createdAt": "2024-01-15T10:05:00Z"
}
```

### 2.2 Place Market Order
```http
POST /orders
Authorization: Bearer {token}

Request:
{
  "marketId": "550e8400-e29b-41d4-a716-446655440000",
  "outcomeId": "660e8400-e29b-41d4-a716-446655440001",
  "orderType": "MARKET",
  "side": "BUY",
  "quantity": 50
}

Response 201:
{
  "orderId": "880e8400-e29b-41d4-a716-446655440004",
  "status": "FILLED",
  "filledQuantity": 50,
  "avgFillPrice": 0.6523,
  "totalCost": 32.615,
  "trades": [
    {
      "tradeId": "...",
      "price": 0.6500,
      "quantity": 30,
      "executedAt": "2024-01-15T10:06:00.123Z"
    },
    {
      "tradeId": "...",
      "price": 0.6550,
      "quantity": 20,
      "executedAt": "2024-01-15T10:06:00.125Z"
    }
  ],
  "createdAt": "2024-01-15T10:06:00Z"
}
```

### 2.3 Get Order Status
```http
GET /orders/{orderId}
Authorization: Bearer {token}

Response 200:
{
  "orderId": "770e8400-e29b-41d4-a716-446655440003",
  "marketId": "...",
  "outcomeId": "...",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 0.6500,
  "quantity": 100,
  "filledQuantity": 60,
  "remainingQuantity": 40,
  "status": "PARTIAL",
  "avgFillPrice": 0.6500,
  "createdAt": "2024-01-15T10:05:00Z",
  "updatedAt": "2024-01-15T10:07:00Z"
}
```

### 2.4 Cancel Order
```http
DELETE /orders/{orderId}
Authorization: Bearer {token}

Response 200:
{
  "orderId": "770e8400-e29b-41d4-a716-446655440003",
  "status": "CANCELLED",
  "filledQuantity": 60,
  "cancelledQuantity": 40,
  "cancelledAt": "2024-01-15T10:10:00Z"
}
```

### 2.5 Get User Orders
```http
GET /orders?userId={userId}&status=PENDING&page=0&size=20
Authorization: Bearer {token}

Response 200:
{
  "orders": [
    {
      "orderId": "...",
      "marketId": "...",
      "question": "Will Bitcoin reach $100K?",
      "outcomeName": "YES",
      "orderType": "LIMIT",
      "side": "BUY",
      "price": 0.6500,
      "quantity": 100,
      "filledQuantity": 60,
      "status": "PARTIAL",
      "createdAt": "2024-01-15T10:05:00Z"
    }
  ],
  "totalElements": 15
}
```

---

## 3. Position APIs

### 3.1 Get User Positions
```http
GET /positions?userId={userId}
Authorization: Bearer {token}

Response 200:
{
  "positions": [
    {
      "positionId": "990e8400-e29b-41d4-a716-446655440005",
      "marketId": "...",
      "question": "Will Bitcoin reach $100K?",
      "outcomeId": "...",
      "outcomeName": "YES",
      "shares": 100,
      "avgCost": 0.6500,
      "currentPrice": 0.7200,
      "unrealizedPnl": 7.00,
      "unrealizedPnlPercent": 10.77,
      "realizedPnl": 0,
      "totalInvested": 65.00
    }
  ],
  "summary": {
    "totalUnrealizedPnl": 7.00,
    "totalRealizedPnl": 0,
    "totalInvested": 65.00,
    "totalValue": 72.00
  }
}
```

### 3.2 Get Position by Market
```http
GET /positions/{marketId}?userId={userId}
Authorization: Bearer {token}

Response 200:
{
  "marketId": "...",
  "question": "Will Bitcoin reach $100K?",
  "positions": [
    {
      "outcomeId": "...",
      "outcomeName": "YES",
      "shares": 100,
      "avgCost": 0.6500,
      "currentPrice": 0.7200,
      "unrealizedPnl": 7.00
    }
  ]
}
```

---

## 4. Balance APIs

### 4.1 Get User Balance
```http
GET /balances/{userId}
Authorization: Bearer {token}

Response 200:
{
  "userId": "user-123",
  "availableBalance": 1000.00,
  "lockedBalance": 65.00,
  "totalBalance": 1065.00,
  "totalDeposited": 2000.00,
  "totalWithdrawn": 500.00,
  "unrealizedPnl": 7.00,
  "realizedPnl": 0,
  "updatedAt": "2024-01-15T10:15:00Z"
}
```

### 4.2 Deposit Funds
```http
POST /balances/deposit
Authorization: Bearer {token}

Request:
{
  "userId": "user-123",
  "amount": 500.00,
  "paymentMethod": "CREDIT_CARD",
  "idempotencyKey": "deposit-67890"
}

Response 201:
{
  "transactionId": "tx-123",
  "userId": "user-123",
  "amount": 500.00,
  "newBalance": 1500.00,
  "status": "COMPLETED",
  "createdAt": "2024-01-15T10:20:00Z"
}
```

### 4.3 Withdraw Funds
```http
POST /balances/withdraw
Authorization: Bearer {token}

Request:
{
  "userId": "user-123",
  "amount": 200.00,
  "withdrawalMethod": "BANK_TRANSFER",
  "idempotencyKey": "withdraw-11111"
}

Response 201:
{
  "transactionId": "tx-124",
  "userId": "user-123",
  "amount": 200.00,
  "newBalance": 1300.00,
  "status": "PENDING",
  "estimatedCompletionDate": "2024-01-17T10:20:00Z",
  "createdAt": "2024-01-15T10:25:00Z"
}
```

---

## 5. Analytics APIs

### 5.1 Get Market Chart
```http
GET /analytics/markets/{marketId}/chart?outcomeId={outcomeId}&interval=1h&from=2024-01-01&to=2024-01-15

Response 200:
{
  "marketId": "...",
  "outcomeId": "...",
  "dataPoints": [
    {
      "timestamp": "2024-01-01T00:00:00Z",
      "price": 0.5000,
      "volume": 1000.00
    },
    {
      "timestamp": "2024-01-01T01:00:00Z",
      "price": 0.5100,
      "volume": 1500.00
    }
  ]
}
```

### 5.2 Get Market Statistics
```http
GET /analytics/markets/{marketId}/stats

Response 200:
{
  "marketId": "...",
  "totalVolume": 150000.00,
  "volume24h": 5000.00,
  "uniqueTraders": 1250,
  "totalTrades": 8500,
  "avgTradeSize": 17.65,
  "highestPrice": 0.8500,
  "lowestPrice": 0.4200,
  "priceChange24h": 0.0500,
  "priceChangePercent24h": 7.46
}
```

### 5.3 Get Leaderboard
```http
GET /analytics/leaderboard?period=30d&page=0&size=50

Response 200:
{
  "leaderboard": [
    {
      "rank": 1,
      "userId": "user-456",
      "username": "CryptoTrader",
      "totalPnl": 15000.00,
      "pnlPercent": 150.00,
      "totalTrades": 500,
      "winRate": 0.68
    }
  ]
}
```

---

## 6. Settlement APIs

### 6.1 Resolve Market (Admin/Creator)
```http
POST /markets/{marketId}/resolve
Authorization: Bearer {admin_token}

Request:
{
  "winningOutcomeId": "660e8400-e29b-41d4-a716-446655440001",
  "resolutionSource": "CHAINLINK",
  "resolutionData": {
    "btcPrice": 105000.00,
    "timestamp": "2025-12-31T23:59:59Z"
  }
}

Response 200:
{
  "marketId": "...",
  "status": "RESOLVED",
  "winningOutcomeId": "...",
  "winningOutcomeName": "YES",
  "totalPayouts": 75000.00,
  "winnersCount": 750,
  "resolvedAt": "2026-01-01T00:05:00Z"
}
```

### 6.2 Get Settlement Details
```http
GET /settlements/{marketId}?userId={userId}
Authorization: Bearer {token}

Response 200:
{
  "marketId": "...",
  "userId": "user-123",
  "settlements": [
    {
      "settlementId": "...",
      "outcomeId": "...",
      "outcomeName": "YES",
      "shares": 100,
      "payout": 100.00,
      "settledAt": "2026-01-01T00:05:00Z"
    }
  ],
  "totalPayout": 100.00
}
```

---

## 7. WebSocket APIs

### 7.1 Connect to WebSocket
```javascript
const socket = new WebSocket('wss://api.predictionmarket.com/ws');

socket.onopen = () => {
  // Authenticate
  socket.send(JSON.stringify({
    type: 'AUTH',
    token: 'Bearer eyJhbGci...'
  }));
};
```

### 7.2 Subscribe to Market Updates
```javascript
// Subscribe to market price updates
socket.send(JSON.stringify({
  type: 'SUBSCRIBE',
  channel: 'market',
  marketId: '550e8400-e29b-41d4-a716-446655440000'
}));

// Receive updates
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  /*
  {
    "type": "PRICE_UPDATE",
    "marketId": "...",
    "outcomeId": "...",
    "price": 0.7200,
    "timestamp": "2024-01-15T10:30:00Z"
  }
  */
};
```

### 7.3 Subscribe to Order Updates
```javascript
// Subscribe to user's order updates
socket.send(JSON.stringify({
  type: 'SUBSCRIBE',
  channel: 'orders',
  userId: 'user-123'
}));

// Receive order updates
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  /*
  {
    "type": "ORDER_FILLED",
    "orderId": "...",
    "filledQuantity": 50,
    "avgFillPrice": 0.6500,
    "status": "FILLED",
    "timestamp": "2024-01-15T10:31:00Z"
  }
  */
};
```

### 7.4 Subscribe to Trade Stream
```javascript
// Subscribe to real-time trades
socket.send(JSON.stringify({
  type: 'SUBSCRIBE',
  channel: 'trades',
  marketId: '550e8400-e29b-41d4-a716-446655440000'
}));

// Receive trade updates
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  /*
  {
    "type": "TRADE_EXECUTED",
    "tradeId": "...",
    "marketId": "...",
    "outcomeId": "...",
    "price": 0.6500,
    "quantity": 50,
    "side": "BUY",
    "timestamp": "2024-01-15T10:32:00.123Z"
  }
  */
};
```

---

## 8. Error Responses

### 8.1 Standard Error Format
```json
{
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Insufficient balance to place order",
    "details": {
      "required": 65.00,
      "available": 50.00
    },
    "timestamp": "2024-01-15T10:35:00Z",
    "requestId": "req-12345"
  }
}
```

### 8.2 Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_REQUEST` | 400 | Malformed request body |
| `UNAUTHORIZED` | 401 | Missing or invalid token |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `MARKET_CLOSED` | 400 | Market is closed for trading |
| `INSUFFICIENT_BALANCE` | 400 | Not enough funds |
| `INVALID_PRICE` | 400 | Price out of range (0-1) |
| `ORDER_NOT_FOUND` | 404 | Order does not exist |
| `DUPLICATE_ORDER` | 409 | Idempotency key conflict |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |

---

## 9. Rate Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| `POST /orders` | 100 req | 1 minute |
| `GET /markets` | 300 req | 1 minute |
| `GET /positions` | 200 req | 1 minute |
| `WebSocket connections` | 10 connections | per user |

---

## 10. SDK Examples

### 10.1 JavaScript/TypeScript
```typescript
import { PredictionMarketClient } from '@predictionmarket/sdk';

const client = new PredictionMarketClient({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.predictionmarket.com/v1'
});

// Place order
const order = await client.orders.create({
  marketId: 'market-123',
  outcomeId: 'outcome-456',
  orderType: 'LIMIT',
  side: 'BUY',
  price: 0.65,
  quantity: 100
});

// Get positions
const positions = await client.positions.list({ userId: 'user-123' });
```

### 10.2 Python
```python
from predictionmarket import Client

client = Client(api_key='your-api-key')

# Place order
order = client.orders.create(
    market_id='market-123',
    outcome_id='outcome-456',
    order_type='LIMIT',
    side='BUY',
    price=0.65,
    quantity=100
)

# Get positions
positions = client.positions.list(user_id='user-123')
```

---

**Next**: See [Scale_Calculations.md](Scale_Calculations.md) for performance analysis.
