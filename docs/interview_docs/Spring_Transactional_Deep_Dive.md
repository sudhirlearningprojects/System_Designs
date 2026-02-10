# Spring @Transactional Deep Dive - Isolation & Propagation Levels

## Table of Contents
1. [What is a Transaction?](#what-is-a-transaction)
2. [ACID Properties](#acid-properties)
3. [@Transactional Annotation](#transactional-annotation)
4. [Isolation Levels](#isolation-levels)
5. [Propagation Levels](#propagation-levels)
6. [Default Values](#default-values)
7. [Real-World Examples](#real-world-examples)

---

## What is a Transaction?

A **transaction** is a sequence of database operations that are treated as a single unit of work. Either all operations succeed (commit) or all fail (rollback).

### Example: Bank Transfer

```java
@Service
public class BankService {
    
    @Transactional
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        // Step 1: Debit from source account
        accountRepository.debit(fromAccount, amount);
        
        // Step 2: Credit to destination account
        accountRepository.credit(toAccount, amount);
        
        // If any step fails, both operations are rolled back
    }
}
```

**Without Transaction**:
```
Step 1: Debit $100 from Account A ✓
Step 2: Credit $100 to Account B ✗ (System crash)
Result: $100 lost! ❌
```

**With Transaction**:
```
Step 1: Debit $100 from Account A ✓
Step 2: Credit $100 to Account B ✗ (System crash)
Result: Both operations rolled back, no money lost ✓
```

---

## ACID Properties

### 1. Atomicity
All operations succeed or all fail (no partial completion).

```java
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);           // Operation 1
    inventoryService.reduceStock(order);   // Operation 2
    paymentService.processPayment(order);  // Operation 3
    
    // If Operation 3 fails, Operations 1 & 2 are rolled back
}
```

### 2. Consistency
Database moves from one valid state to another.

```java
@Transactional
public void transferMoney(String from, String to, BigDecimal amount) {
    // Before: Account A = $1000, Account B = $500, Total = $1500
    debit(from, amount);   // Account A = $900
    credit(to, amount);    // Account B = $600
    // After: Account A = $900, Account B = $600, Total = $1500 ✓
}
```

### 3. Isolation
Concurrent transactions don't interfere with each other.

```java
// Transaction 1
@Transactional(isolation = Isolation.SERIALIZABLE)
public void updateBalance(String accountId) {
    Account account = accountRepository.findById(accountId);
    account.setBalance(account.getBalance().add(new BigDecimal("100")));
    accountRepository.save(account);
}

// Transaction 2 waits until Transaction 1 completes
```

### 4. Durability
Committed changes are permanent (survive system failures).

```java
@Transactional
public void saveOrder(Order order) {
    orderRepository.save(order);
    // After commit, data is written to disk
    // Even if system crashes, data persists
}
```

---

## @Transactional Annotation

### Basic Usage

```java
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    
    // Method-level transaction
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
        inventoryService.updateStock(order);
    }
}
```

### Class-level Transaction

```java
@Service
@Transactional // All methods are transactional
public class UserService {
    
    public void createUser(User user) {
        userRepository.save(user);
    }
    
    public void updateUser(User user) {
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true) // Override for read-only
    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

### @Transactional Attributes

```java
@Transactional(
    isolation = Isolation.READ_COMMITTED,      // Isolation level
    propagation = Propagation.REQUIRED,        // Propagation behavior
    timeout = 30,                              // Timeout in seconds
    readOnly = false,                          // Read-only flag
    rollbackFor = Exception.class,             // Rollback on these exceptions
    noRollbackFor = IllegalArgumentException.class // Don't rollback on these
)
public void complexOperation() {
    // Transaction logic
}
```

---

## Isolation Levels

Isolation levels control how transaction changes are visible to other concurrent transactions.

### Problems Without Proper Isolation

#### 1. Dirty Read
Reading uncommitted data from another transaction.

```java
// Transaction 1
@Transactional
public void updateBalance() {
    account.setBalance(1000);
    // Not yet committed
}

// Transaction 2 (Dirty Read)
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readBalance() {
    int balance = account.getBalance(); // Reads 1000
    // Transaction 1 rolls back
    // balance is now invalid!
}
```

#### 2. Non-Repeatable Read
Reading same data twice gives different results.

```java
// Transaction 1
@Transactional(isolation = Isolation.READ_COMMITTED)
public void processOrder() {
    int stock = inventory.getStock(); // Reads 100
    
    // Transaction 2 updates stock to 50
    
    int stock2 = inventory.getStock(); // Reads 50
    // Same query, different result!
}
```

#### 3. Phantom Read
New rows appear between queries.

```java
// Transaction 1
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void countOrders() {
    int count1 = orderRepository.countByStatus("PENDING"); // Returns 10
    
    // Transaction 2 inserts new PENDING order
    
    int count2 = orderRepository.countByStatus("PENDING"); // Returns 11
    // New row appeared!
}
```

---

### Isolation Level 1: READ_UNCOMMITTED (Level 0)

**Lowest isolation, highest performance, most problems.**

```java
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
    // Can read uncommitted changes from other transactions
}
```

**Problems**:
- ✗ Dirty Read: YES
- ✗ Non-Repeatable Read: YES
- ✗ Phantom Read: YES

**Use Case**: Rarely used, only for non-critical data where performance is critical.

**Example**:
```java
// Transaction 1
@Transactional
public void updatePrice() {
    product.setPrice(100);
    // Not committed yet
    Thread.sleep(5000);
    // Rollback
}

// Transaction 2 (Dirty Read)
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readPrice() {
    int price = product.getPrice(); // Reads 100 (uncommitted)
    // Transaction 1 rolls back, price is invalid!
}
```

---

### Isolation Level 2: READ_COMMITTED (Level 1)

**Prevents dirty reads. Default for most databases (PostgreSQL, Oracle, SQL Server).**

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void readData() {
    // Only reads committed data
}
```

**Problems**:
- ✓ Dirty Read: NO
- ✗ Non-Repeatable Read: YES
- ✗ Phantom Read: YES

**Use Case**: Most common isolation level for general-purpose applications.

**Example**:
```java
// Transaction 1
@Transactional(isolation = Isolation.READ_COMMITTED)
public void processOrder() {
    Product product = productRepository.findById(1L);
    int stock1 = product.getStock(); // Reads 100
    
    // Transaction 2 commits: stock = 50
    
    product = productRepository.findById(1L);
    int stock2 = product.getStock(); // Reads 50
    // Non-repeatable read!
}
```

---

### Isolation Level 3: REPEATABLE_READ (Level 2)

**Prevents dirty reads and non-repeatable reads. Default for MySQL.**

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void readData() {
    // Same row always returns same data within transaction
}
```

**Problems**:
- ✓ Dirty Read: NO
- ✓ Non-Repeatable Read: NO
- ✗ Phantom Read: YES (in theory, MySQL InnoDB prevents this)

**Use Case**: Financial applications, inventory management.

**Example**:
```java
// Transaction 1
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void processOrder() {
    Product product = productRepository.findById(1L);
    int stock1 = product.getStock(); // Reads 100
    
    // Transaction 2 tries to update stock
    // Transaction 2 is BLOCKED until Transaction 1 completes
    
    product = productRepository.findById(1L);
    int stock2 = product.getStock(); // Still reads 100
    // Repeatable read guaranteed!
}
```

---

### Isolation Level 4: SERIALIZABLE (Level 3)

**Highest isolation, lowest performance, no problems.**

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void readData() {
    // Transactions execute as if they were serial (one after another)
}
```

**Problems**:
- ✓ Dirty Read: NO
- ✓ Non-Repeatable Read: NO
- ✓ Phantom Read: NO

**Use Case**: Critical financial transactions, audit logs.

**Example**:
```java
// Transaction 1
@Transactional(isolation = Isolation.SERIALIZABLE)
public void processOrders() {
    List<Order> orders = orderRepository.findByStatus("PENDING");
    // Count: 10
    
    // Transaction 2 tries to insert new PENDING order
    // Transaction 2 is BLOCKED
    
    orders = orderRepository.findByStatus("PENDING");
    // Count: Still 10 (no phantom reads)
}
```

---

### Isolation Levels Comparison

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance | Use Case |
|----------------|------------|---------------------|--------------|-------------|----------|
| **READ_UNCOMMITTED** | ✗ Yes | ✗ Yes | ✗ Yes | ⚡⚡⚡⚡ Fastest | Analytics, logs |
| **READ_COMMITTED** | ✓ No | ✗ Yes | ✗ Yes | ⚡⚡⚡ Fast | General apps |
| **REPEATABLE_READ** | ✓ No | ✓ No | ✗ Yes | ⚡⚡ Medium | Financial apps |
| **SERIALIZABLE** | ✓ No | ✓ No | ✓ No | ⚡ Slow | Critical transactions |

---

## Propagation Levels

Propagation defines how transactions relate to each other when one transactional method calls another.

### Propagation 1: REQUIRED (Default)

**Use existing transaction or create new one.**

```java
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    // Transaction T1 starts
    methodB(); // Uses same transaction T1
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodB() {
    // Uses existing transaction T1
}
```

**Scenario 1: No existing transaction**
```java
public void caller() {
    serviceA.methodA(); // Creates new transaction T1
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    // Runs in transaction T1
}
```

**Scenario 2: Existing transaction**
```java
@Transactional
public void caller() {
    // Transaction T1 starts
    serviceA.methodA(); // Uses same transaction T1
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    // Runs in same transaction T1
}
```

**Rollback Behavior**:
```java
@Transactional(propagation = Propagation.REQUIRED)
public void createOrder() {
    orderRepository.save(order);        // Operation 1
    paymentService.processPayment();    // Operation 2 (throws exception)
    // Both operations rolled back
}
```

---

### Propagation 2: REQUIRES_NEW

**Always create new transaction, suspend existing one.**

```java
@Transactional
public void methodA() {
    // Transaction T1
    orderRepository.save(order);
    
    methodB(); // Creates new transaction T2, suspends T1
    
    // T1 resumes
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB() {
    // Runs in new transaction T2
    auditRepository.save(audit);
}
```

**Use Case: Audit Logging**
```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        // Log audit even if order creation fails
        auditService.logOrderCreation(order);
        
        // Simulate failure
        throw new RuntimeException("Order failed");
        // Order rolled back, but audit is committed!
    }
}

@Service
public class AuditService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreation(Order order) {
        Audit audit = new Audit("Order created: " + order.getId());
        auditRepository.save(audit);
        // Commits immediately in separate transaction
    }
}
```

**Rollback Behavior**:
```java
@Transactional // T1
public void methodA() {
    save(data1); // T1
    
    methodB(); // T2 (new transaction)
    
    throw new RuntimeException(); // T1 rolls back, T2 already committed
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB() {
    save(data2); // T2 commits independently
}

// Result: data1 rolled back, data2 committed
```

---

### Propagation 3: SUPPORTS

**Use existing transaction if present, otherwise run non-transactionally.**

```java
@Transactional(propagation = Propagation.SUPPORTS)
public void methodA() {
    // If called within transaction: uses it
    // If called without transaction: runs without transaction
}
```

**Use Case: Read-only operations**
```java
@Service
public class ProductService {
    
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElse(null);
    }
}

// Scenario 1: Called without transaction
public void caller1() {
    productService.getProduct(1L); // No transaction
}

// Scenario 2: Called within transaction
@Transactional
public void caller2() {
    productService.getProduct(1L); // Uses existing transaction
}
```

---

### Propagation 4: NOT_SUPPORTED

**Always run non-transactionally, suspend existing transaction.**

```java
@Transactional
public void methodA() {
    // Transaction T1
    save(data1);
    
    methodB(); // Suspends T1, runs without transaction
    
    // T1 resumes
}

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void methodB() {
    // Runs without transaction
    // Changes are auto-committed
}
```

**Use Case: External API calls, long-running operations**
```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        // Call external API without transaction
        externalService.notifyWarehouse(order);
    }
}

@Service
public class ExternalService {
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notifyWarehouse(Order order) {
        // HTTP call to external system
        // No transaction needed
        restTemplate.postForObject(url, order, String.class);
    }
}
```

**Important**: See [Spring_Transactional_NOT_SUPPORTED_Deep_Dive.md](Spring_Transactional_NOT_SUPPORTED_Deep_Dive.md) for detailed explanation of NOT_SUPPORTED vs No @Transactional.

---

### Propagation 5: MANDATORY

**Must run within existing transaction, throw exception if none exists.**

```java
@Transactional(propagation = Propagation.MANDATORY)
public void methodA() {
    // Must be called within existing transaction
    // Otherwise throws IllegalTransactionStateException
}
```

**Use Case: Enforce transactional context**
```java
@Service
public class PaymentService {
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void processPayment(Order order) {
        // This method MUST be called within a transaction
        paymentRepository.save(payment);
    }
}

// Correct usage
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    paymentService.processPayment(order); // ✓ Works
}

// Incorrect usage
public void createOrder(Order order) {
    orderRepository.save(order);
    paymentService.processPayment(order); // ✗ Throws exception
}
```

---

### Propagation 6: NEVER

**Must run non-transactionally, throw exception if transaction exists.**

```java
@Transactional(propagation = Propagation.NEVER)
public void methodA() {
    // Must NOT be called within transaction
    // Otherwise throws IllegalTransactionStateException
}
```

**Use Case: Prevent accidental transactional calls**
```java
@Service
public class ReportService {
    
    @Transactional(propagation = Propagation.NEVER)
    public void generateReport() {
        // Long-running operation
        // Should not hold database transaction
    }
}

// Correct usage
public void caller1() {
    reportService.generateReport(); // ✓ Works
}

// Incorrect usage
@Transactional
public void caller2() {
    reportService.generateReport(); // ✗ Throws exception
}
```

---

### Propagation 7: NESTED

**Create nested transaction (savepoint) within existing transaction.**

```java
@Transactional
public void methodA() {
    // Transaction T1
    save(data1);
    
    try {
        methodB(); // Creates savepoint within T1
    } catch (Exception e) {
        // methodB rolled back to savepoint
        // data1 still valid
    }
    
    save(data3); // Continues with T1
}

@Transactional(propagation = Propagation.NESTED)
public void methodB() {
    // Runs in nested transaction (savepoint)
    save(data2);
    throw new RuntimeException(); // Rolls back to savepoint
}

// Result: data1 and data3 committed, data2 rolled back
```

**Use Case: Partial rollback**
```java
@Service
public class OrderService {
    
    @Transactional
    public void processOrders(List<Order> orders) {
        for (Order order : orders) {
            try {
                processOrder(order); // Nested transaction
            } catch (Exception e) {
                log.error("Failed to process order: " + order.getId());
                // Continue processing other orders
            }
        }
    }
    
    @Transactional(propagation = Propagation.NESTED)
    public void processOrder(Order order) {
        orderRepository.save(order);
        inventoryService.updateStock(order);
        // If fails, only this order is rolled back
    }
}
```

---

### Propagation Levels Comparison

| Propagation | Existing Transaction | No Transaction | Creates New | Use Case |
|-------------|---------------------|----------------|-------------|----------|
| **REQUIRED** (default) | Use existing | Create new | No | General purpose |
| **REQUIRES_NEW** | Suspend, create new | Create new | Yes | Audit logging |
| **SUPPORTS** | Use existing | No transaction | No | Read operations |
| **NOT_SUPPORTED** | Suspend | No transaction | No | External API calls |
| **MANDATORY** | Use existing | Throw exception | No | Enforce transaction |
| **NEVER** | Throw exception | No transaction | No | Prevent transaction |
| **NESTED** | Create savepoint | Create new | No | Partial rollback |

---

## Default Values

### Spring @Transactional Defaults

```java
@Transactional(
    propagation = Propagation.REQUIRED,           // DEFAULT
    isolation = Isolation.DEFAULT,                // DEFAULT (database default)
    timeout = -1,                                 // No timeout
    readOnly = false,                             // Read-write
    rollbackFor = {},                             // Rollback on RuntimeException
    noRollbackFor = {}                            // No exceptions excluded
)
```

### Database Default Isolation Levels

| Database | Default Isolation Level |
|----------|------------------------|
| **PostgreSQL** | READ_COMMITTED |
| **MySQL (InnoDB)** | REPEATABLE_READ |
| **Oracle** | READ_COMMITTED |
| **SQL Server** | READ_COMMITTED |
| **H2** | READ_COMMITTED |

### Isolation.DEFAULT

```java
@Transactional(isolation = Isolation.DEFAULT)
public void method() {
    // Uses database's default isolation level
    // PostgreSQL: READ_COMMITTED
    // MySQL: REPEATABLE_READ
}
```

---

## Real-World Examples

### Example 1: E-commerce Order Processing

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private AuditService auditService;
    
    @Transactional(
        isolation = Isolation.REPEATABLE_READ,
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class,
        timeout = 30
    )
    public Order createOrder(OrderRequest request) {
        // 1. Create order
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus("PENDING");
        orderRepository.save(order);
        
        // 2. Reduce inventory (same transaction)
        inventoryService.reduceStock(request.getItems());
        
        // 3. Process payment (same transaction)
        paymentService.processPayment(order);
        
        // 4. Log audit (separate transaction - always commits)
        auditService.logOrderCreation(order);
        
        order.setStatus("COMPLETED");
        return orderRepository.save(order);
    }
}

@Service
public class AuditService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreation(Order order) {
        Audit audit = new Audit();
        audit.setAction("ORDER_CREATED");
        audit.setOrderId(order.getId());
        audit.setTimestamp(LocalDateTime.now());
        auditRepository.save(audit);
        // Commits immediately, even if order creation fails
    }
}
```

### Example 2: Bank Transfer with High Isolation

```java
@Service
public class BankService {
    
    @Transactional(
        isolation = Isolation.SERIALIZABLE,  // Highest isolation
        propagation = Propagation.REQUIRED,
        timeout = 10
    )
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        // 1. Lock and read source account
        Account source = accountRepository.findByIdWithLock(fromAccount);
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        // 2. Lock and read destination account
        Account destination = accountRepository.findByIdWithLock(toAccount);
        
        // 3. Perform transfer
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        
        // 4. Save changes
        accountRepository.save(source);
        accountRepository.save(destination);
        
        // 5. Log transaction (separate transaction)
        transactionLogService.logTransfer(fromAccount, toAccount, amount);
    }
}
```

### Example 3: Batch Processing with Nested Transactions

```java
@Service
public class BatchService {
    
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED
    )
    public BatchResult processBatch(List<Record> records) {
        BatchResult result = new BatchResult();
        
        for (Record record : records) {
            try {
                processRecord(record); // Nested transaction
                result.incrementSuccess();
            } catch (Exception e) {
                log.error("Failed to process record: " + record.getId(), e);
                result.incrementFailure();
                // Continue processing other records
            }
        }
        
        return result;
    }
    
    @Transactional(propagation = Propagation.NESTED)
    public void processRecord(Record record) {
        // Process individual record
        // If fails, only this record is rolled back
        recordRepository.save(record);
        
        if (record.requiresValidation()) {
            validationService.validate(record);
        }
    }
}
```

### Example 4: Read-Only Query with SUPPORTS

```java
@Service
public class ReportService {
    
    @Transactional(
        propagation = Propagation.SUPPORTS,
        readOnly = true,
        isolation = Isolation.READ_COMMITTED
    )
    public Report generateSalesReport(LocalDate startDate, LocalDate endDate) {
        // Read-only operation
        // Uses transaction if available, otherwise runs without
        
        List<Order> orders = orderRepository.findByDateRange(startDate, endDate);
        BigDecimal totalSales = orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new Report(totalSales, orders.size());
    }
}
```

---

## Common Pitfalls

### 1. Self-Invocation Problem

```java
@Service
public class UserService {
    
    public void publicMethod() {
        // Calling transactional method from same class
        this.transactionalMethod(); // ✗ Transaction NOT applied!
    }
    
    @Transactional
    private void transactionalMethod() {
        // Transaction doesn't work!
    }
}

// Solution: Use separate service or self-inject
@Service
public class UserService {
    
    @Autowired
    private UserService self; // Self-injection
    
    public void publicMethod() {
        self.transactionalMethod(); // ✓ Transaction works!
    }
    
    @Transactional
    public void transactionalMethod() {
        // Transaction works!
    }
}
```

### 2. Checked Exception Not Rolling Back

```java
@Transactional // Only rolls back on RuntimeException by default
public void method() throws Exception {
    save(data);
    throw new Exception(); // ✗ Does NOT rollback!
}

// Solution: Specify rollbackFor
@Transactional(rollbackFor = Exception.class)
public void method() throws Exception {
    save(data);
    throw new Exception(); // ✓ Rolls back!
}
```

### 3. Transaction Timeout

```java
@Transactional(timeout = 5) // 5 seconds
public void longRunningOperation() {
    // If takes > 5 seconds, throws TransactionTimedOutException
    Thread.sleep(10000); // ✗ Timeout!
}
```

---

## Summary

### Isolation Levels (Default: READ_COMMITTED for most DBs)

| Level | Dirty Read | Non-Repeatable | Phantom | Performance |
|-------|-----------|----------------|---------|-------------|
| READ_UNCOMMITTED | ✗ | ✗ | ✗ | ⚡⚡⚡⚡ |
| READ_COMMITTED | ✓ | ✗ | ✗ | ⚡⚡⚡ |
| REPEATABLE_READ | ✓ | ✓ | ✗ | ⚡⚡ |
| SERIALIZABLE | ✓ | ✓ | ✓ | ⚡ |

### Propagation Levels (Default: REQUIRED)

| Level | Behavior | Use Case |
|-------|----------|----------|
| REQUIRED | Use existing or create new | General purpose |
| REQUIRES_NEW | Always create new | Audit logging |
| SUPPORTS | Use if exists, else none | Read operations |
| NOT_SUPPORTED | Never use transaction | External API |
| MANDATORY | Must have transaction | Enforce context |
| NEVER | Must not have transaction | Prevent transaction |
| NESTED | Create savepoint | Partial rollback |

### Best Practices

1. ✅ Use **READ_COMMITTED** for general applications
2. ✅ Use **REPEATABLE_READ** for financial transactions
3. ✅ Use **SERIALIZABLE** only when absolutely necessary
4. ✅ Use **REQUIRED** for most transactional methods
5. ✅ Use **REQUIRES_NEW** for audit logging
6. ✅ Use **NESTED** for batch processing with partial rollback
7. ✅ Always specify `rollbackFor = Exception.class` for checked exceptions
8. ✅ Use `readOnly = true` for read-only operations
9. ✅ Set appropriate `timeout` for long-running operations
10. ✅ Avoid self-invocation of transactional methods

---

## Complex Interview Questions

## Interview Questions

For complex interview questions about Spring @Transactional annotation, including common pitfalls, edge cases, and real-world scenarios, see:

**[Spring_Transactional_Interview_Questions.md](Spring_Transactional_Interview_Questions.md)**

This separate document covers 12 detailed interview questions including:
- Self-Invocation Problem
- Checked Exception Rollback
- Propagation REQUIRES_NEW Behavior
- Nested Transaction Rollback
- Read-Only Transaction Violation
- Transaction Timeout
- Multiple @Transactional Annotations
- Isolation Level and Concurrent Updates
- Propagation MANDATORY vs REQUIRED
- Transaction and Exception Handling
- @Transactional on Private Methods
- Transaction and Async Methods

---

## Additional Resources

- [Spring Transaction Management Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Spring @Transactional NOT_SUPPORTED Deep Dive](Spring_Transactional_NOT_SUPPORTED_Deep_Dive.md)
- [Spring @Transactional Interview Questions](Spring_Transactional_Interview_Questions.md)
