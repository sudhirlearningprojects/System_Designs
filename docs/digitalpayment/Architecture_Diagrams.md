# Digital Payment Platform - Architecture Diagrams

## Understanding Payment System Architecture

### What Makes Payment Architecture Different?
Payment systems require unique architectural considerations compared to regular applications:

1. **Financial Accuracy**: Never lose or duplicate money
2. **Regulatory Compliance**: Meet banking and financial regulations
3. **Security**: Protect against fraud and unauthorized access
4. **Atomicity**: Transactions must be all-or-nothing
5. **Auditability**: Complete transaction trails for compliance

### Key Architectural Principles

#### Microservices for Payment Systems
- **Payment Service**: Orchestrates payment flow
- **Ledger Service**: Manages account balances with ACID properties
- **Fraud Detection**: Real-time risk assessment
- **User Service**: Authentication and user management
- **Transaction History**: Audit trails and reporting

#### Why Separate Ledger Service?
```java
// Wrong: Payment service directly updating balances
public class PaymentService {
    public void transfer(String from, String to, BigDecimal amount) {
        Account fromAccount = accountRepo.findById(from);
        Account toAccount = accountRepo.findById(to);
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepo.save(fromAccount);
        accountRepo.save(toAccount); // Risk: Partial failure!
    }
}

// Correct: Dedicated ledger service with atomic operations
public class LedgerService {
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void atomicTransfer(String from, String to, BigDecimal amount) {
        // Acquire locks in consistent order to prevent deadlocks
        Account fromAccount = accountRepo.findByIdForUpdate(from);
        Account toAccount = accountRepo.findByIdForUpdate(to);
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        // Both updates happen atomically
        accountRepo.saveAll(Arrays.asList(fromAccount, toAccount));
    }
}
```

#### API Gateway for Payment Security
- **Authentication**: JWT token validation
- **Rate Limiting**: Prevent abuse and DDoS
- **Request Validation**: Input sanitization
- **Audit Logging**: Track all API calls

### Payment Flow Patterns

#### Synchronous vs Asynchronous Processing

##### Synchronous Flow (Simple but Limited)
```
Client → API Gateway → Payment Service → PSP → Response → Client
                                    ↓
                              Blocking Wait
```
**Problems**: 
- Timeout issues with slow PSPs
- Poor user experience
- Limited throughput

##### Asynchronous Flow (Scalable)
```
Client → API Gateway → Payment Service → Queue → PSP Processor
                            ↓                        ↓
                      Immediate Response         Callback/Webhook
```
**Benefits**:
- Fast response to user
- Higher throughput
- Better fault tolerance

### Database Design for Financial Systems

#### Why PostgreSQL for Payments?
1. **ACID Compliance**: Guaranteed transaction integrity
2. **Mature Ecosystem**: Well-tested for financial applications
3. **Complex Queries**: Support for financial reporting
4. **Concurrent Control**: Proper locking mechanisms

#### Schema Design Principles
```sql
-- Immutable transaction records (never update, only insert)
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    -- Never add updated_at for financial records!
    idempotency_key VARCHAR(255) UNIQUE NOT NULL
);

-- Versioned account balances for optimistic locking
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1, -- For optimistic locking
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

#### Idempotency in Database Design
```sql
-- Prevent duplicate transactions
CREATE UNIQUE INDEX idx_idempotency_key ON transactions(idempotency_key);

-- Function to handle duplicate requests
CREATE OR REPLACE FUNCTION process_payment(
    p_idempotency_key VARCHAR(255),
    p_from_account UUID,
    p_to_account UUID,
    p_amount DECIMAL(15,2)
) RETURNS UUID AS $$
DECLARE
    existing_txn_id UUID;
    new_txn_id UUID;
BEGIN
    -- Check if transaction already exists
    SELECT id INTO existing_txn_id 
    FROM transactions 
    WHERE idempotency_key = p_idempotency_key;
    
    IF existing_txn_id IS NOT NULL THEN
        RETURN existing_txn_id; -- Return existing transaction
    END IF;
    
    -- Create new transaction
    INSERT INTO transactions (id, from_account_id, to_account_id, amount, status, idempotency_key)
    VALUES (gen_random_uuid(), p_from_account, p_to_account, p_amount, 'PENDING', p_idempotency_key)
    RETURNING id INTO new_txn_id;
    
    RETURN new_txn_id;
END;
$$ LANGUAGE plpgsql;
```

## High-Level System Architecture

### Architecture Explanation
This diagram shows the complete payment platform architecture with clear separation of concerns:

1. **Client Layer**: Mobile and web applications
2. **API Gateway**: Single entry point with security controls
3. **Microservices**: Specialized services for different functions
4. **External Services**: Payment service providers (PSPs)
5. **Data Layer**: Persistent storage with different databases for different needs
6. **Message Queue**: Asynchronous communication between services

```mermaid
graph TB
    subgraph "Client Layer"
        MA[Mobile App]
        WA[Web App]
    end
    
    subgraph "API Gateway Layer"
        AG[API Gateway<br/>Authentication<br/>Rate Limiting<br/>Load Balancing]
    end
    
    subgraph "Microservices Layer"
        PS[Payment Service]
        LS[Ledger Service]
        FDS[Fraud Detection Service]
        US[User Service]
        THS[Transaction History Service]
    end
    
    subgraph "External Services"
        UPI[UPI Gateway]
        CARD[Card Gateway]
        NB[Net Banking Gateway]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Transactions)]
        RD[(Redis<br/>Cache)]
        CS[(Cassandra<br/>Logs)]
    end
    
    subgraph "Message Queue"
        KF[Apache Kafka]
    end
    
    MA --> AG
    WA --> AG
    AG --> PS
    AG --> US
    AG --> THS
    
    PS --> LS
    PS --> FDS
    PS --> UPI
    PS --> CARD
    PS --> NB
    
    PS --> PG
    LS --> PG
    FDS --> RD
    THS --> CS
    
    PS --> KF
    KF --> FDS
    KF --> THS
```

## Payment Flow Sequence Diagram

### Understanding the Payment Flow
This sequence diagram illustrates the complete payment processing flow with all security checks and data consistency measures:

#### Step-by-Step Flow Analysis

1. **Request Initiation**: Client sends payment request with idempotency key
2. **Gateway Processing**: API Gateway validates authentication and routes request
3. **Idempotency Check**: Prevent duplicate processing of same request
4. **Fraud Detection**: Real-time risk assessment before processing
5. **Payment Processing**: Different flows for wallet vs external PSP payments
6. **Result Caching**: Store result for future duplicate requests

#### Critical Decision Points

##### Idempotency Check Logic
```java
public PaymentResult processPayment(PaymentRequest request) {
    String idempotencyKey = request.getIdempotencyKey();
    
    // Check Redis cache first (fast)
    PaymentResult cachedResult = redisTemplate.opsForValue().get(idempotencyKey);
    if (cachedResult != null) {
        return cachedResult; // Return cached result immediately
    }
    
    // Check database for older requests
    Optional<Transaction> existingTxn = transactionRepo.findByIdempotencyKey(idempotencyKey);
    if (existingTxn.isPresent()) {
        PaymentResult result = PaymentResult.fromTransaction(existingTxn.get());
        // Cache for future requests
        redisTemplate.opsForValue().set(idempotencyKey, result, Duration.ofHours(24));
        return result;
    }
    
    // Process new payment
    return executeNewPayment(request);
}
```

##### Fraud Detection Decision Tree
```java
public FraudCheckResult validateTransaction(PaymentRequest request) {
    FraudScore score = new FraudScore();
    
    // Check daily transaction limit
    BigDecimal dailyTotal = getDailyTransactionTotal(request.getUserId());
    if (dailyTotal.add(request.getAmount()).compareTo(DAILY_LIMIT) > 0) {
        return FraudCheckResult.block("Daily limit exceeded");
    }
    
    // Check transaction frequency
    int hourlyCount = getHourlyTransactionCount(request.getUserId());
    if (hourlyCount >= MAX_HOURLY_TRANSACTIONS) {
        return FraudCheckResult.block("Too many transactions");
    }
    
    // Check amount threshold
    if (request.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
        score.addRisk("HIGH_AMOUNT", 30);
    }
    
    return score.getTotalScore() > FRAUD_THRESHOLD ? 
           FraudCheckResult.block("High fraud risk") : 
           FraudCheckResult.allow();
}
```

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Payment_Service
    participant Fraud_Detection
    participant Ledger_Service
    participant PSP_Gateway
    participant Database
    participant Redis
    
    Client->>API_Gateway: Initiate Payment Request
    API_Gateway->>Payment_Service: Route Request
    
    Payment_Service->>Redis: Check Idempotency Key
    Redis-->>Payment_Service: Key Status
    
    Payment_Service->>Fraud_Detection: Validate Transaction
    Fraud_Detection->>Redis: Check Limits & Patterns
    Redis-->>Fraud_Detection: User Activity Data
    Fraud_Detection-->>Payment_Service: Validation Result
    
    alt Fraud Check Passed
        Payment_Service->>Database: Create Transaction Record
        Database-->>Payment_Service: Transaction Created
        
        alt Wallet Payment
            Payment_Service->>Ledger_Service: Transfer Funds
            Ledger_Service->>Database: Atomic Balance Update
            Database-->>Ledger_Service: Update Successful
            Ledger_Service-->>Payment_Service: Transfer Complete
        else External PSP
            Payment_Service->>PSP_Gateway: Process Payment
            PSP_Gateway-->>Payment_Service: PSP Response
        end
        
        Payment_Service->>Database: Update Transaction Status
        Payment_Service->>Redis: Cache Result (Idempotency)
        Payment_Service-->>Client: Payment Response
    else Fraud Check Failed
        Payment_Service-->>Client: Transaction Blocked
    end
```

## Database Schema Design

```mermaid
erDiagram
    USER {
        string user_id PK
        string name
        string phone_number UK
        string email UK
        string pin
        enum status
        timestamp created_at
    }
    
    ACCOUNT {
        string account_id PK
        string user_id FK
        string bank_name
        string account_number
        string ifsc_code
        decimal balance
        enum type
        enum status
        long version
        timestamp created_at
    }
    
    WALLET {
        string wallet_id PK
        string user_id FK
        decimal balance
        long version
        timestamp last_updated
    }
    
    TRANSACTION {
        string transaction_id PK
        string sender_id
        string receiver_id
        decimal amount
        string currency
        enum type
        enum status
        enum payment_method
        string psp_transaction_id
        string description
        string idempotency_key UK
        timestamp created_at
        timestamp updated_at
    }
    
    USER ||--o{ ACCOUNT : has
    USER ||--|| WALLET : owns
    USER ||--o{ TRANSACTION : sends
    USER ||--o{ TRANSACTION : receives
```

## Concurrency Control Architecture

```mermaid
graph TB
    subgraph "Concurrent Requests"
        R1[Request 1<br/>Transfer ₹100]
        R2[Request 2<br/>Transfer ₹200]
        R3[Request 3<br/>Check Balance]
    end
    
    subgraph "Ledger Service"
        LS[Ledger Service<br/>@Transactional<br/>SERIALIZABLE]
    end
    
    subgraph "Database Layer"
        PL[Pessimistic Lock<br/>SELECT FOR UPDATE]
        WR[Wallet Record<br/>Balance: ₹500<br/>Version: 1]
    end
    
    R1 --> LS
    R2 --> LS
    R3 --> LS
    
    LS --> PL
    PL --> WR
    
    note1[Request 1 acquires lock first]
    note2[Request 2 waits for lock release]
    note3[Request 3 reads after updates]
```

## Fraud Detection Flow

```mermaid
graph TD
    TR[Transaction Request] --> FD[Fraud Detection Service]
    
    FD --> DL{Daily Limit<br/>Check}
    FD --> FL{Frequency<br/>Check}
    FD --> AL{Amount<br/>Threshold}
    
    DL -->|< ₹1,00,000| DL_OK[✓ Pass]
    DL -->|≥ ₹1,00,000| DL_FAIL[✗ Block]
    
    FL -->|< 50/hour| FL_OK[✓ Pass]
    FL -->|≥ 50/hour| FL_FAIL[✗ Block]
    
    AL -->|< ₹50,000| AL_OK[✓ Pass]
    AL -->|≥ ₹50,000| AL_FLAG[⚠ Flag]
    
    DL_OK --> ALLOW[Allow Transaction]
    FL_OK --> ALLOW
    AL_OK --> ALLOW
    AL_FLAG --> ALLOW
    
    DL_FAIL --> BLOCK[Block Transaction]
    FL_FAIL --> BLOCK
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[NGINX/AWS ALB]
    end
    
    subgraph "Application Tier (Kubernetes)"
        subgraph "Payment Service Pods"
            PS1[Payment Service 1]
            PS2[Payment Service 2]
            PS3[Payment Service 3]
        end
        
        subgraph "Ledger Service Pods"
            LS1[Ledger Service 1]
            LS2[Ledger Service 2]
        end
        
        subgraph "Fraud Detection Pods"
            FD1[Fraud Detection 1]
            FD2[Fraud Detection 2]
        end
    end
    
    subgraph "Data Tier"
        subgraph "PostgreSQL Cluster"
            PG_M[(Primary)]
            PG_R1[(Replica 1)]
            PG_R2[(Replica 2)]
        end
        
        subgraph "Redis Cluster"
            RD_M[(Redis Master)]
            RD_S1[(Redis Slave 1)]
            RD_S2[(Redis Slave 2)]
        end
    end
    
    LB --> PS1
    LB --> PS2
    LB --> PS3
    
    PS1 --> LS1
    PS2 --> LS2
    PS3 --> FD1
    
    LS1 --> PG_M
    LS2 --> PG_M
    FD1 --> RD_M
    FD2 --> RD_M
    
    PG_M --> PG_R1
    PG_M --> PG_R2
    RD_M --> RD_S1
    RD_M --> RD_S2
```

These diagrams illustrate the comprehensive architecture of the digital payment platform, showing the flow of data, security measures, and scalability considerations.