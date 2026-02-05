# Probability Management System - System Design

## Table of Contents
1. [Overview](#overview)
2. [Requirements](#requirements)
3. [High-Level Design](#high-level-design)
4. [Low-Level Design](#low-level-design)
5. [Database Schema](#database-schema)
6. [API Design](#api-design)
7. [Algorithms](#algorithms)
8. [Scale & Performance](#scale--performance)

---

## 1. Overview

A **Probability Management System** (Prediction Market Platform) allows users to trade on the outcome of future events. Similar to Polymarket, Kalshi, and PredictIt, users buy/sell shares representing probabilities of events occurring.

### Real-World Examples
- **Polymarket**: Crypto-based prediction market ($1B+ volume)
- **Kalshi**: CFTC-regulated prediction exchange
- **PredictIt**: Political prediction market
- **Augur**: Decentralized prediction protocol

### Key Concepts
- **Market**: A question about a future event (e.g., "Will Bitcoin reach $100K by 2025?")
- **Outcome**: Possible results (YES/NO for binary, multiple for categorical)
- **Share**: Represents probability (1 share = $1 if outcome occurs, $0 otherwise)
- **Order Book**: Buy/sell orders at different price points
- **Automated Market Maker (AMM)**: Algorithmic liquidity provider using LMSR/CPMM

---

## 2. Requirements

### 2.1 Functional Requirements

#### Core Features
1. **Market Creation**
   - Create binary (YES/NO) and categorical markets
   - Set resolution criteria and end date
   - Define market maker parameters (liquidity, spread)

2. **Trading**
   - Place limit orders (buy/sell at specific price)
   - Place market orders (instant execution)
   - Cancel pending orders
   - Partial order fills

3. **Market Making**
   - Automated Market Maker (AMM) using LMSR algorithm
   - Dynamic pricing based on supply/demand
   - Liquidity provision and withdrawal

4. **Settlement**
   - Oracle-based resolution (Chainlink, UMA, manual)
   - Payout calculation and distribution
   - Dispute resolution mechanism

5. **Portfolio Management**
   - View positions across markets
   - Calculate unrealized P&L
   - Transaction history

6. **Analytics**
   - Real-time probability charts
   - Volume and liquidity metrics
   - Market depth visualization

### 2.2 Non-Functional Requirements

| Requirement | Target | Notes |
|-------------|--------|-------|
| **Availability** | 99.99% | 52 min downtime/year |
| **Latency** | <100ms | Order placement |
| **Throughput** | 10K orders/sec | Peak trading |
| **Consistency** | Strong | No double-spending |
| **Scalability** | 10M users | Horizontal scaling |
| **Data Retention** | Indefinite | Audit trail |

### 2.3 Scale Estimates

- **Users**: 10M registered, 1M DAU
- **Markets**: 100K active markets
- **Orders**: 1B orders/day (11.5K/sec avg, 50K/sec peak)
- **Trades**: 100M trades/day
- **Storage**: 50TB (orders, trades, positions)

---

## 3. High-Level Design

### 3.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  Web App (React) | Mobile (React Native) | Trading Bots (API)   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   API Gateway   │
                    │  (Rate Limit)   │
                    └────────┬────────┘
                             │
        ┏━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━┓
        ┃                                           ┃
┌───────▼────────┐                      ┌──────────▼─────────┐
│  Market Service │                      │  Trading Service   │
│  - Create       │                      │  - Order matching  │
│  - List         │                      │  - Position mgmt   │
│  - Resolve      │                      │  - AMM execution   │
└───────┬────────┘                      └──────────┬─────────┘
        │                                           │
        │                                  ┌────────▼────────┐
        │                                  │ Matching Engine │
        │                                  │  (In-Memory)    │
        │                                  └────────┬────────┘
        │                                           │
┌───────▼────────┐                      ┌──────────▼─────────┐
│ Oracle Service │                      │ Settlement Service │
│  - Chainlink   │                      │  - Payout calc     │
│  - Manual      │                      │  - Distribution    │
└───────┬────────┘                      └──────────┬─────────┘
        │                                           │
        └───────────────────┬───────────────────────┘
                            │
                ┌───────────▼──────────┐
                │   Event Bus (Kafka)  │
                │  - OrderPlaced       │
                │  - TradeExecuted     │
                │  - MarketResolved    │
                └───────────┬──────────┘
                            │
        ┏━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━┓
        ┃                                         ┃
┌───────▼────────┐                    ┌──────────▼─────────┐
│   PostgreSQL   │                    │      Redis         │
│  - Markets     │                    │  - Order book      │
│  - Orders      │                    │  - Positions       │
│  - Trades      │                    │  - Market prices   │
│  - Positions   │                    │  - User balances   │
└────────────────┘                    └────────────────────┘
```

### 3.2 Core Components

#### 3.2.1 Market Service
- Create and manage prediction markets
- Store market metadata (question, outcomes, end date)
- Handle market lifecycle (OPEN → LOCKED → RESOLVED)

#### 3.2.2 Trading Service
- Order placement and validation
- Balance checks and holds
- Position tracking

#### 3.2.3 Matching Engine
- In-memory order book (Redis Sorted Sets)
- Price-time priority matching
- Partial fills and order cancellation
- AMM integration for instant liquidity

#### 3.2.4 AMM (Automated Market Maker)
- **LMSR** (Logarithmic Market Scoring Rule) for binary markets
- **CPMM** (Constant Product Market Maker) for multi-outcome
- Dynamic pricing based on outstanding shares

#### 3.2.5 Settlement Service
- Oracle integration (Chainlink, UMA)
- Payout calculation (winning shares × $1)
- Batch distribution to winners

#### 3.2.6 Analytics Service
- Real-time probability aggregation
- Volume and liquidity metrics
- Market depth snapshots

---

## 4. Low-Level Design

### 4.1 Market Creation Flow

```
User → API Gateway → Market Service
                          ↓
                    Validate Request
                    (question, outcomes, end_date)
                          ↓
                    Create Market Record
                    (status = OPEN)
                          ↓
                    Initialize AMM
                    (liquidity pool, b parameter)
                          ↓
                    Publish MarketCreated Event
                          ↓
                    Return Market ID
```

### 4.2 Order Placement Flow

```
User → Trading Service
         ↓
    Validate Order
    (market exists, sufficient balance)
         ↓
    Lock User Balance
    (amount = shares × price)
         ↓
    Add to Order Book (Redis)
    (ZADD orders:market:BUY price order_id)
         ↓
    Trigger Matching Engine
         ↓
    Match Orders
    (price-time priority)
         ↓
    Execute Trades
    (update positions, balances)
         ↓
    Publish TradeExecuted Event
         ↓
    Return Order Status
```

### 4.3 AMM Execution Flow

```
User → Market Order → Trading Service
                           ↓
                      No Limit Orders?
                           ↓
                      Route to AMM
                           ↓
                   Calculate Price (LMSR)
                   p = exp(q_yes/b) / (exp(q_yes/b) + exp(q_no/b))
                           ↓
                   Update Share Quantities
                   (q_yes += shares_bought)
                           ↓
                   Calculate Cost
                   cost = b × ln(sum(exp(q_i/b)))
                           ↓
                   Update User Position
                           ↓
                   Return Execution Price
```

### 4.4 Settlement Flow

```
Market End Date Reached
         ↓
    Lock Market (status = LOCKED)
         ↓
    Fetch Oracle Result
    (Chainlink price feed / Manual input)
         ↓
    Determine Winning Outcome
         ↓
    Calculate Payouts
    (winning_shares × $1)
         ↓
    Batch Update Balances
    (PostgreSQL transaction)
         ↓
    Update Market Status (RESOLVED)
         ↓
    Publish MarketResolved Event
```

---

## 5. Database Schema

### 5.1 PostgreSQL Schema

```sql
-- Markets table
CREATE TABLE markets (
    market_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    description TEXT,
    market_type VARCHAR(20) NOT NULL, -- BINARY, CATEGORICAL
    status VARCHAR(20) NOT NULL, -- OPEN, LOCKED, RESOLVED, CANCELLED
    end_date TIMESTAMP NOT NULL,
    resolution_date TIMESTAMP,
    winning_outcome_id UUID,
    creator_id UUID NOT NULL,
    liquidity_parameter DECIMAL(10,2), -- 'b' for LMSR
    total_volume DECIMAL(20,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_status (status),
    INDEX idx_end_date (end_date)
);

-- Outcomes table
CREATE TABLE outcomes (
    outcome_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    market_id UUID NOT NULL REFERENCES markets(market_id),
    name VARCHAR(100) NOT NULL, -- YES, NO, or custom
    outstanding_shares DECIMAL(20,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_market (market_id)
);

-- Orders table
CREATE TABLE orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    market_id UUID NOT NULL REFERENCES markets(market_id),
    outcome_id UUID NOT NULL REFERENCES outcomes(outcome_id),
    user_id UUID NOT NULL,
    order_type VARCHAR(10) NOT NULL, -- LIMIT, MARKET
    side VARCHAR(10) NOT NULL, -- BUY, SELL
    price DECIMAL(10,4) NOT NULL, -- 0.0000 to 1.0000
    quantity DECIMAL(20,2) NOT NULL,
    filled_quantity DECIMAL(20,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL, -- PENDING, PARTIAL, FILLED, CANCELLED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_market_outcome (market_id, outcome_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
);

-- Trades table
CREATE TABLE trades (
    trade_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    market_id UUID NOT NULL REFERENCES markets(market_id),
    outcome_id UUID NOT NULL REFERENCES outcomes(outcome_id),
    buy_order_id UUID NOT NULL REFERENCES orders(order_id),
    sell_order_id UUID NOT NULL REFERENCES orders(order_id),
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    price DECIMAL(10,4) NOT NULL,
    quantity DECIMAL(20,2) NOT NULL,
    executed_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_market (market_id),
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_executed_at (executed_at)
);

-- Positions table
CREATE TABLE positions (
    position_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    market_id UUID NOT NULL REFERENCES markets(market_id),
    outcome_id UUID NOT NULL REFERENCES outcomes(outcome_id),
    shares DECIMAL(20,2) NOT NULL DEFAULT 0,
    avg_cost DECIMAL(10,4) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(20,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, market_id, outcome_id),
    INDEX idx_user (user_id),
    INDEX idx_market (market_id)
);

-- User balances table
CREATE TABLE user_balances (
    user_id UUID PRIMARY KEY,
    available_balance DECIMAL(20,2) NOT NULL DEFAULT 0,
    locked_balance DECIMAL(20,2) NOT NULL DEFAULT 0,
    total_deposited DECIMAL(20,2) DEFAULT 0,
    total_withdrawn DECIMAL(20,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Settlements table
CREATE TABLE settlements (
    settlement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    market_id UUID NOT NULL REFERENCES markets(market_id),
    user_id UUID NOT NULL,
    outcome_id UUID NOT NULL REFERENCES outcomes(outcome_id),
    shares DECIMAL(20,2) NOT NULL,
    payout DECIMAL(20,2) NOT NULL,
    settled_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_market (market_id),
    INDEX idx_user (user_id)
);
```

### 5.2 Redis Data Structures

```
# Order book (Sorted Sets)
ZADD orders:market:{market_id}:outcome:{outcome_id}:BUY {price} {order_id}
ZADD orders:market:{market_id}:outcome:{outcome_id}:SELL {price} {order_id}

# Order details (Hash)
HSET order:{order_id} user_id {user_id} price {price} quantity {quantity} ...

# User positions (Hash)
HSET position:{user_id}:{market_id}:{outcome_id} shares {shares} avg_cost {avg_cost}

# Market prices (Hash)
HSET market:{market_id}:prices outcome:{outcome_id} {current_price}

# User balance (Hash)
HSET balance:{user_id} available {amount} locked {amount}

# Market depth cache (String - JSON)
SET market:{market_id}:depth {json_depth_data} EX 60

# Recent trades (List)
LPUSH market:{market_id}:trades {trade_json}
LTRIM market:{market_id}:trades 0 99
```

---

## 6. API Design

### 6.1 Market APIs

#### Create Market
```http
POST /api/v1/markets
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "Will Bitcoin reach $100K by Dec 31, 2025?",
  "description": "Resolves YES if BTC/USD >= $100,000 on any exchange",
  "marketType": "BINARY",
  "outcomes": ["YES", "NO"],
  "endDate": "2025-12-31T23:59:59Z",
  "liquidityParameter": 100.0,
  "resolutionSource": "CHAINLINK_BTC_USD"
}

Response 201:
{
  "marketId": "uuid",
  "question": "...",
  "status": "OPEN",
  "outcomes": [
    {"outcomeId": "uuid", "name": "YES", "probability": 0.5},
    {"outcomeId": "uuid", "name": "NO", "probability": 0.5}
  ],
  "createdAt": "2024-01-15T10:00:00Z"
}
```

#### List Markets
```http
GET /api/v1/markets?status=OPEN&page=0&size=20&sort=volume,desc

Response 200:
{
  "markets": [
    {
      "marketId": "uuid",
      "question": "...",
      "status": "OPEN",
      "endDate": "2025-12-31T23:59:59Z",
      "totalVolume": 150000.00,
      "outcomes": [...]
    }
  ],
  "totalPages": 50,
  "totalElements": 1000
}
```

### 6.2 Trading APIs

#### Place Order
```http
POST /api/v1/orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "marketId": "uuid",
  "outcomeId": "uuid",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 0.65,
  "quantity": 100
}

Response 201:
{
  "orderId": "uuid",
  "status": "PENDING",
  "filledQuantity": 0,
  "avgFillPrice": 0,
  "createdAt": "2024-01-15T10:05:00Z"
}
```

#### Cancel Order
```http
DELETE /api/v1/orders/{orderId}
Authorization: Bearer {token}

Response 200:
{
  "orderId": "uuid",
  "status": "CANCELLED",
  "cancelledAt": "2024-01-15T10:10:00Z"
}
```

### 6.3 Position APIs

#### Get User Positions
```http
GET /api/v1/positions?userId={userId}
Authorization: Bearer {token}

Response 200:
{
  "positions": [
    {
      "marketId": "uuid",
      "question": "...",
      "outcomeId": "uuid",
      "outcomeName": "YES",
      "shares": 100,
      "avgCost": 0.65,
      "currentPrice": 0.72,
      "unrealizedPnl": 7.00,
      "realizedPnl": 0
    }
  ],
  "totalUnrealizedPnl": 7.00,
  "totalRealizedPnl": 0
}
```

---

## 7. Algorithms

### 7.1 LMSR (Logarithmic Market Scoring Rule)

**Purpose**: Automated market maker for binary/categorical markets

**Formula**:
```
Cost Function:
C(q) = b × ln(Σ exp(q_i / b))

Price for outcome i:
p_i = exp(q_i / b) / Σ exp(q_j / b)

Where:
- q_i = outstanding shares for outcome i
- b = liquidity parameter (higher = more liquidity, lower spread)
```

**Example**:
```
Market: "Will it rain tomorrow?" (YES/NO)
b = 100 (liquidity parameter)
q_yes = 50, q_no = 50 (initial)

Price of YES:
p_yes = exp(50/100) / (exp(50/100) + exp(50/100))
      = exp(0.5) / (2 × exp(0.5))
      = 0.5 (50% probability)

User buys 10 YES shares:
q_yes = 60, q_no = 50

New price:
p_yes = exp(60/100) / (exp(60/100) + exp(50/100))
      = exp(0.6) / (exp(0.6) + exp(0.5))
      = 1.822 / (1.822 + 1.649)
      = 0.525 (52.5% probability)

Cost = C(60,50) - C(50,50)
     = 100 × ln(exp(0.6) + exp(0.5)) - 100 × ln(2 × exp(0.5))
     = 100 × ln(3.471) - 100 × ln(3.297)
     = 124.4 - 119.3
     = $5.10
```

### 7.2 Order Matching Algorithm

**Price-Time Priority**:
```java
public List<Trade> matchOrder(Order incomingOrder) {
    List<Trade> trades = new ArrayList<>();
    String oppositeBookKey = getOppositeBookKey(incomingOrder);
    
    while (incomingOrder.getRemainingQuantity() > 0) {
        // Get best opposite order (highest buy / lowest sell)
        Order bestOpposite = getBestOrder(oppositeBookKey);
        
        if (bestOpposite == null || !pricesMatch(incomingOrder, bestOpposite)) {
            break; // No match possible
        }
        
        // Calculate trade quantity
        BigDecimal tradeQty = incomingOrder.getRemainingQuantity()
            .min(bestOpposite.getRemainingQuantity());
        
        // Execute trade at maker's price
        Trade trade = executeTrade(incomingOrder, bestOpposite, tradeQty);
        trades.add(trade);
        
        // Update order quantities
        incomingOrder.addFilledQuantity(tradeQty);
        bestOpposite.addFilledQuantity(tradeQty);
        
        // Remove fully filled orders
        if (bestOpposite.isFilled()) {
            removeFromOrderBook(bestOpposite);
        }
    }
    
    return trades;
}

private boolean pricesMatch(Order incoming, Order existing) {
    if (incoming.getSide() == OrderSide.BUY) {
        return incoming.getPrice().compareTo(existing.getPrice()) >= 0;
    } else {
        return incoming.getPrice().compareTo(existing.getPrice()) <= 0;
    }
}
```

### 7.3 Settlement Algorithm

```java
public void settleMarket(UUID marketId, UUID winningOutcomeId) {
    // 1. Lock market
    marketRepository.updateStatus(marketId, MarketStatus.LOCKED);
    
    // 2. Get all positions for winning outcome
    List<Position> winningPositions = positionRepository
        .findByMarketAndOutcome(marketId, winningOutcomeId);
    
    // 3. Calculate payouts (1 share = $1)
    List<Settlement> settlements = winningPositions.stream()
        .map(pos -> new Settlement(
            pos.getUserId(),
            pos.getShares(), // shares
            pos.getShares().multiply(BigDecimal.ONE) // payout = shares × $1
        ))
        .collect(Collectors.toList());
    
    // 4. Batch update balances (single transaction)
    transactionTemplate.execute(status -> {
        settlements.forEach(settlement -> {
            userBalanceRepository.addBalance(
                settlement.getUserId(),
                settlement.getPayout()
            );
        });
        settlementRepository.saveAll(settlements);
        marketRepository.updateStatus(marketId, MarketStatus.RESOLVED);
        return null;
    });
    
    // 5. Publish event
    kafkaTemplate.send("market-resolved", new MarketResolvedEvent(marketId));
}
```

---

## 8. Scale & Performance

### 8.1 Capacity Planning

**Users & Traffic**:
- 10M registered users
- 1M DAU (10% active)
- 100 orders/user/day = 100M orders/day
- Peak: 5x average = 500M orders/day (5.8K/sec)

**Storage**:
```
Markets: 100K × 1KB = 100MB
Orders: 1B × 500B = 500GB
Trades: 100M × 300B = 30GB
Positions: 10M users × 10 markets × 200B = 20GB
Total: ~550GB (with indexes: ~1TB)
```

**Bandwidth**:
```
Order placement: 5.8K/sec × 1KB = 5.8 MB/sec
WebSocket updates: 1M connections × 100B/sec = 100 MB/sec
Total: ~106 MB/sec = 848 Mbps
```

### 8.2 Performance Optimizations

#### 8.2.1 In-Memory Order Book
- Redis Sorted Sets for O(log N) operations
- Sub-millisecond order matching
- Periodic snapshots to PostgreSQL

#### 8.2.2 Multi-Layer Caching
```
L1: Application Cache (Caffeine) - 10ms
L2: Redis Cache - 1-5ms
L3: PostgreSQL - 10-50ms
```

#### 8.2.3 Database Sharding
- Shard by market_id (hash-based)
- 10 shards × 100GB = 1TB total
- Parallel query execution

#### 8.2.4 Read Replicas
- 1 primary + 3 replicas
- Route reads to replicas (95% of traffic)
- Async replication (<100ms lag)

#### 8.2.5 Event-Driven Architecture
- Kafka for async processing
- Decouple order placement from settlement
- 1M events/sec throughput

### 8.3 Fault Tolerance

#### 8.3.1 Database
- PostgreSQL with streaming replication
- Automatic failover with Patroni
- Point-in-time recovery (PITR)

#### 8.3.2 Redis
- Redis Cluster (3 masters + 3 replicas)
- Automatic failover
- AOF persistence for durability

#### 8.3.3 Application
- Kubernetes with 3+ replicas per service
- Circuit breaker for external APIs
- Graceful degradation (disable AMM if overloaded)

### 8.4 Security

#### 8.4.1 Authentication
- JWT tokens with 1-hour expiry
- Refresh tokens with 30-day expiry
- Rate limiting: 100 req/min per user

#### 8.4.2 Authorization
- Role-based access control (RBAC)
- Market creators can resolve their markets
- Admins can override resolutions

#### 8.4.3 Financial Security
- Pessimistic locking for balance updates
- Idempotency keys for order placement
- Audit logs for all transactions
- Daily reconciliation jobs

#### 8.4.4 Market Manipulation Prevention
- Max position size limits
- Wash trading detection
- Suspicious activity monitoring

---

## 9. Monitoring & Observability

### 9.1 Metrics
- Order placement latency (p50, p95, p99)
- Matching engine throughput
- Trade execution rate
- Market liquidity depth
- User balance accuracy

### 9.2 Alerts
- High order rejection rate (>5%)
- Matching engine lag (>100ms)
- Database replication lag (>1sec)
- Balance reconciliation failures

### 9.3 Logging
- Structured logs (JSON)
- Distributed tracing (Jaeger)
- Audit trail for all financial operations

---

## 10. Future Enhancements

1. **Blockchain Integration**: On-chain settlement for transparency
2. **Advanced Order Types**: Stop-loss, take-profit, iceberg orders
3. **Market Maker Incentives**: Liquidity mining rewards
4. **Social Features**: Follow traders, leaderboards
5. **Mobile Apps**: iOS/Android native apps
6. **API for Bots**: WebSocket API for algorithmic trading
7. **Conditional Markets**: Multi-leg predictions
8. **Insurance Fund**: Protect against oracle failures

---

**Next**: See [API_Documentation.md](API_Documentation.md) for complete API reference.
