# Spring @Transactional - Complex Interview Questions

## Overview

This document contains 12 complex interview questions about Spring @Transactional annotation, covering common pitfalls, edge cases, and real-world scenarios.

---

## Question 1: Self-Invocation Problem

**Q: What happens in this code? Will the transaction work?**

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public void createUser(User user) {
        validateUser(user);
        saveUser(user); // Will transaction work?
    }
    
    @Transactional
    private void saveUser(User user) {
        userRepository.save(user);
    }
}
```

**Answer**: ❌ **Transaction will NOT work!**

**Reason**: Spring uses **AOP proxies** to implement @Transactional. When you call a method from within the same class (self-invocation), the proxy is bypassed, so the transaction interceptor is never invoked.

**Flow**:
```
Client → Proxy (with @Transactional) → Target Object

Self-invocation:
Client → Proxy → createUser() → saveUser() (direct call, bypasses proxy)
                                    ↑
                                No transaction!
```

**Solutions**:

```java
// Solution 1: Self-injection (Spring 4.3+)
@Service
public class UserService {
    
    @Autowired
    private UserService self;
    
    public void createUser(User user) {
        validateUser(user);
        self.saveUser(user); // ✓ Transaction works!
    }
    
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}

// Solution 2: Separate service
@Service
public class UserService {
    
    @Autowired
    private UserTransactionService transactionService;
    
    public void createUser(User user) {
        validateUser(user);
        transactionService.saveUser(user); // ✓ Transaction works!
    }
}

@Service
public class UserTransactionService {
    
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}

// Solution 3: Make calling method transactional
@Service
public class UserService {
    
    @Transactional // ✓ Entire method is transactional
    public void createUser(User user) {
        validateUser(user);
        saveUser(user);
    }
    
    private void saveUser(User user) {
        userRepository.save(user);
    }
}
```

---

## Question 2: Checked Exception Rollback

**Q: Will this transaction rollback?**

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) throws Exception {
        orderRepository.save(order);
        
        if (order.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new Exception("Invalid amount"); // Will it rollback?
        }
        
        inventoryService.updateStock(order);
    }
}
```

**Answer**: ❌ **Transaction will NOT rollback!**

**Reason**: By default, @Transactional only rolls back on **RuntimeException** and **Error**, not on checked exceptions.

**Default Rollback Rules**:
- ✓ RuntimeException → Rollback
- ✓ Error → Rollback
- ✗ Checked Exception (Exception) → Commit

**Solution**:

```java
// Solution 1: Specify rollbackFor
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) throws Exception {
    orderRepository.save(order);
    throw new Exception("Invalid amount"); // ✓ Rolls back!
}

// Solution 2: Use RuntimeException
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    throw new RuntimeException("Invalid amount"); // ✓ Rolls back!
}

// Solution 3: Custom exception
@Transactional(rollbackFor = {Exception.class, CustomException.class})
public void createOrder(Order order) throws CustomException {
    orderRepository.save(order);
    throw new CustomException("Invalid amount"); // ✓ Rolls back!
}
```

---

## Question 3: Propagation REQUIRES_NEW Behavior

**Q: What will be the final state of the database?**

```java
@Service
public class OrderService {
    
    @Autowired
    private AuditService auditService;
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order); // Order ID: 1
        
        auditService.logAudit("Order created"); // Audit ID: 1
        
        throw new RuntimeException("Order failed");
    }
}

@Service
public class AuditService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(String message) {
        Audit audit = new Audit(message);
        auditRepository.save(audit);
    }
}
```

**Answer**: 
- ❌ Order: **NOT saved** (rolled back)
- ✓ Audit: **Saved** (committed in separate transaction)

**Explanation**:
```
Transaction T1 (createOrder):
  - Save Order (ID: 1)
  - Call logAudit()
    Transaction T2 (logAudit - REQUIRES_NEW):
      - Suspend T1
      - Save Audit (ID: 1)
      - Commit T2 ✓
    Resume T1
  - Throw RuntimeException
  - Rollback T1 ✗

Final State:
- orders table: Empty
- audits table: 1 record
```

**Use Case**: This is perfect for audit logging where you want to keep the audit trail even if the main transaction fails.

---

## Question 4: Nested Transaction Rollback

**Q: What records will be saved?**

```java
@Service
public class BatchService {
    
    @Transactional
    public void processBatch() {
        save("Record 1"); // ID: 1
        
        try {
            processRecord2();
        } catch (Exception e) {
            log.error("Record 2 failed");
        }
        
        save("Record 3"); // ID: 3
    }
    
    @Transactional(propagation = Propagation.NESTED)
    public void processRecord2() {
        save("Record 2"); // ID: 2
        throw new RuntimeException("Failed");
    }
}
```

**Answer**: 
- ✓ Record 1: **Saved**
- ❌ Record 2: **NOT saved** (rolled back to savepoint)
- ✓ Record 3: **Saved**

**Explanation**:
```
Transaction T1:
  - Save Record 1 ✓
  - Create Savepoint S1
    Nested Transaction:
      - Save Record 2
      - Throw exception
      - Rollback to Savepoint S1 ✗
  - Catch exception
  - Save Record 3 ✓
  - Commit T1 ✓

Final State:
- Record 1: Saved
- Record 2: Rolled back
- Record 3: Saved
```

---

## Question 5: Read-Only Transaction Violation

**Q: What happens when you try to modify data in a read-only transaction?**

```java
@Service
public class UserService {
    
    @Transactional(readOnly = true)
    public void updateUser(User user) {
        userRepository.save(user); // What happens?
    }
}
```

**Answer**: **Depends on the database and JPA provider!**

**Hibernate Behavior**:
- ✗ Throws `TransientPropertyValueException` or similar
- Hibernate's flush mode is set to `MANUAL` for read-only transactions
- Changes are not flushed to the database

**Database Behavior**:
- Some databases (PostgreSQL) may throw exception
- Some databases may silently ignore the write

**Best Practice**: Never modify data in read-only transactions!

```java
// Correct usage
@Transactional(readOnly = true)
public User getUser(Long id) {
    return userRepository.findById(id).orElse(null); // ✓ Read-only
}

@Transactional // Read-write by default
public void updateUser(User user) {
    userRepository.save(user); // ✓ Can modify
}
```

---

## Question 6: Transaction Timeout

**Q: What happens when transaction exceeds timeout?**

```java
@Service
public class ReportService {
    
    @Transactional(timeout = 5) // 5 seconds
    public void generateReport() {
        // Query 1: Takes 3 seconds
        List<Order> orders = orderRepository.findAll();
        
        // Query 2: Takes 3 seconds
        List<Customer> customers = customerRepository.findAll();
        
        // Total: 6 seconds (exceeds timeout)
    }
}
```

**Answer**: ❌ **Throws `TransactionTimedOutException`**

**Explanation**:
- Timeout starts when transaction begins
- If any operation exceeds timeout, transaction is rolled back
- Timeout is checked before each database operation

**Timeline**:
```
T=0s:  Transaction starts
T=3s:  Query 1 completes ✓
T=5s:  Timeout reached!
T=6s:  Query 2 attempts to execute → TransactionTimedOutException ✗
```

**Solution**:
```java
// Solution 1: Increase timeout
@Transactional(timeout = 10)
public void generateReport() {
    // Now has 10 seconds
}

// Solution 2: Remove transaction (if not needed)
public void generateReport() {
    // No transaction, no timeout
}

// Solution 3: Use read-only transaction
@Transactional(readOnly = true, timeout = 30)
public void generateReport() {
    // Read-only with longer timeout
}
```

---

## Question 7: Multiple @Transactional Annotations

**Q: Which @Transactional annotation takes precedence?**

```java
@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class OrderService {
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createOrder(Order order) {
        orderRepository.save(order);
    }
    
    public void updateOrder(Order order) {
        orderRepository.save(order);
    }
}
```

**Answer**: **Method-level annotation overrides class-level annotation**

**Behavior**:
- `createOrder()`: Uses `READ_COMMITTED` (method-level)
- `updateOrder()`: Uses `SERIALIZABLE` (class-level)

**Precedence Order**:
1. Method-level @Transactional (highest priority)
2. Class-level @Transactional
3. Interface-level @Transactional (lowest priority)

```java
@Transactional(isolation = Isolation.SERIALIZABLE) // Priority 3
public interface OrderService {
    void createOrder(Order order);
}

@Service
@Transactional(isolation = Isolation.REPEATABLE_READ) // Priority 2
public class OrderServiceImpl implements OrderService {
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED) // Priority 1 (wins!)
    public void createOrder(Order order) {
        orderRepository.save(order);
    }
}
```

---

## Question 8: Isolation Level and Concurrent Updates

**Q: What happens when two transactions try to update the same record?**

```java
// Transaction 1
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateStock1() {
    Product product = productRepository.findById(1L); // Stock: 100
    product.setStock(product.getStock() - 10); // Stock: 90
    Thread.sleep(5000); // Simulate delay
    productRepository.save(product);
}

// Transaction 2 (starts 1 second after T1)
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateStock2() {
    Product product = productRepository.findById(1L); // Stock: 100
    product.setStock(product.getStock() - 20); // Stock: 80
    productRepository.save(product);
}
```

**Answer**: ❌ **Lost Update Problem!**

**Timeline**:
```
T=0s:  T1 reads stock: 100
T=0s:  T1 calculates: 100 - 10 = 90
T=1s:  T2 reads stock: 100 (T1 not committed yet)
T=1s:  T2 calculates: 100 - 20 = 80
T=2s:  T2 saves: 80 and commits ✓
T=5s:  T1 saves: 90 and commits ✓

Final stock: 90 (should be 70!)
Lost update: 20 units
```

**Solutions**:

```java
// Solution 1: Pessimistic Locking
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateStock() {
    Product product = productRepository.findByIdWithLock(1L); // SELECT ... FOR UPDATE
    product.setStock(product.getStock() - 10);
    productRepository.save(product);
    // Other transactions wait until this commits
}

// Solution 2: Optimistic Locking
@Entity
public class Product {
    @Id
    private Long id;
    
    private Integer stock;
    
    @Version
    private Long version; // Optimistic lock
}

@Transactional
public void updateStock() {
    Product product = productRepository.findById(1L);
    product.setStock(product.getStock() - 10);
    productRepository.save(product);
    // Throws OptimisticLockException if version changed
}

// Solution 3: Higher Isolation Level
@Transactional(isolation = Isolation.SERIALIZABLE)
public void updateStock() {
    Product product = productRepository.findById(1L);
    product.setStock(product.getStock() - 10);
    productRepository.save(product);
    // Transactions execute serially
}
```

---

## Question 9: Propagation MANDATORY vs REQUIRED

**Q: What's the difference between MANDATORY and REQUIRED?**

```java
// Scenario 1: REQUIRED
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    // What happens?
}

// Scenario 2: MANDATORY
@Transactional(propagation = Propagation.MANDATORY)
public void methodB() {
    // What happens?
}

// Called without transaction
public void caller() {
    methodA(); // ?
    methodB(); // ?
}
```

**Answer**:

| Propagation | No Existing Transaction | Existing Transaction |
|-------------|------------------------|---------------------|
| **REQUIRED** | ✓ Creates new transaction | ✓ Uses existing |
| **MANDATORY** | ✗ Throws exception | ✓ Uses existing |

**Behavior**:
```java
public void caller() {
    methodA(); // ✓ Creates new transaction T1
    methodB(); // ✗ Throws IllegalTransactionStateException
}

@Transactional
public void caller() {
    // Transaction T1 starts
    methodA(); // ✓ Uses T1
    methodB(); // ✓ Uses T1
}
```

**Use Case for MANDATORY**:
```java
@Service
public class PaymentService {
    
    // This method MUST be called within a transaction
    @Transactional(propagation = Propagation.MANDATORY)
    public void processPayment(Order order) {
        // Ensures this is always part of a larger transaction
        paymentRepository.save(payment);
    }
}

@Service
public class OrderService {
    
    @Transactional // ✓ Correct: Creates transaction
    public void createOrder(Order order) {
        orderRepository.save(order);
        paymentService.processPayment(order); // ✓ Works
    }
}

// ✗ Incorrect: No transaction
public void createOrder(Order order) {
    orderRepository.save(order);
    paymentService.processPayment(order); // ✗ Throws exception
}
```

---

## Question 10: Transaction and Exception Handling

**Q: What will be saved in the database?**

```java
@Service
public class UserService {
    
    @Transactional
    public void createUsers() {
        userRepository.save(new User("Alice")); // User 1
        
        try {
            userRepository.save(new User("Bob")); // User 2
            throw new RuntimeException("Error");
        } catch (Exception e) {
            log.error("Error creating Bob");
        }
        
        userRepository.save(new User("Charlie")); // User 3
    }
}
```

**Answer**: ❌ **Nothing is saved! All rolled back!**

**Reason**: Even though the exception is caught, the transaction is marked for rollback. Spring sets the transaction status to "rollback-only" when a RuntimeException occurs.

**Explanation**:
```
Transaction T1:
  - Save Alice ✓
  - Save Bob ✓
  - RuntimeException thrown
  - Transaction marked as rollback-only
  - Exception caught (but transaction still marked for rollback)
  - Save Charlie ✓
  - Commit attempted
  - UnexpectedRollbackException thrown
  - All changes rolled back ✗

Final State: No users saved
```

**Solutions**:

```java
// Solution 1: Use REQUIRES_NEW for independent transactions
@Transactional
public void createUsers() {
    createUser("Alice"); // Separate transaction
    
    try {
        createUser("Bob"); // Separate transaction
    } catch (Exception e) {
        log.error("Error creating Bob");
    }
    
    createUser("Charlie"); // Separate transaction
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void createUser(String name) {
    userRepository.save(new User(name));
    if (name.equals("Bob")) {
        throw new RuntimeException("Error");
    }
}
// Result: Alice and Charlie saved, Bob rolled back

// Solution 2: Use NESTED for savepoints
@Transactional
public void createUsers() {
    userRepository.save(new User("Alice"));
    
    try {
        createUserNested("Bob");
    } catch (Exception e) {
        log.error("Error creating Bob");
    }
    
    userRepository.save(new User("Charlie"));
}

@Transactional(propagation = Propagation.NESTED)
public void createUserNested(String name) {
    userRepository.save(new User(name));
    throw new RuntimeException("Error");
}
// Result: Alice and Charlie saved, Bob rolled back to savepoint

// Solution 3: Don't throw exception
@Transactional
public void createUsers() {
    userRepository.save(new User("Alice"));
    
    try {
        userRepository.save(new User("Bob"));
        // Don't throw exception
    } catch (Exception e) {
        log.error("Error creating Bob");
    }
    
    userRepository.save(new User("Charlie"));
}
// Result: All users saved
```

---

## Question 11: @Transactional on Private Methods

**Q: Will @Transactional work on private methods?**

```java
@Service
public class UserService {
    
    public void createUser(User user) {
        saveUser(user);
    }
    
    @Transactional
    private void saveUser(User user) {
        userRepository.save(user);
    }
}
```

**Answer**: ❌ **No, @Transactional does NOT work on private methods!**

**Reason**: Spring uses **proxy-based AOP** (either JDK dynamic proxies or CGLIB). Proxies can only intercept public method calls.

**Why Private Methods Don't Work**:
```
Proxy can only intercept:
- Public methods (JDK Proxy + CGLIB)
- Protected methods (CGLIB only)
- Package-private methods (CGLIB only)

Proxy CANNOT intercept:
- Private methods
- Final methods
- Static methods
```

**Solution**: Make the method public or protected

```java
@Service
public class UserService {
    
    public void createUser(User user) {
        saveUser(user);
    }
    
    @Transactional
    public void saveUser(User user) { // ✓ Public
        userRepository.save(user);
    }
}
```

---

## Question 12: Transaction and Async Methods

**Q: What happens when you combine @Transactional with @Async?**

```java
@Service
public class NotificationService {
    
    @Transactional
    @Async
    public void sendNotification(User user) {
        notificationRepository.save(new Notification(user));
        emailService.sendEmail(user.getEmail());
    }
}
```

**Answer**: ⚠️ **Transaction may not work as expected!**

**Reason**: @Async executes the method in a separate thread. The transaction is bound to the calling thread, not the async thread.

**Problem**:
```
Main Thread:
  - Calls sendNotification()
  - Returns immediately
  - Transaction commits (but method hasn't executed yet!)

Async Thread:
  - Executes sendNotification()
  - No transaction context!
  - May fail or behave unexpectedly
```

**Solution**: Move @Transactional inside the async method

```java
@Service
public class NotificationService {
    
    @Autowired
    private NotificationTransactionService transactionService;
    
    @Async
    public void sendNotification(User user) {
        transactionService.saveNotification(user); // ✓ Transaction in async thread
    }
}

@Service
public class NotificationTransactionService {
    
    @Transactional
    public void saveNotification(User user) {
        notificationRepository.save(new Notification(user));
        emailService.sendEmail(user.getEmail());
    }
}
```

---

## Summary Table

| Question | Key Concept | Common Mistake |
|----------|-------------|----------------|
| Self-Invocation | AOP Proxy | Calling @Transactional method from same class |
| Checked Exception | Rollback Rules | Not specifying rollbackFor |
| REQUIRES_NEW | Independent Transaction | Expecting rollback of inner transaction |
| NESTED | Savepoints | Confusing with REQUIRES_NEW |
| Read-Only | Optimization | Trying to modify data |
| Timeout | Transaction Duration | Long-running operations |
| Multiple Annotations | Precedence | Not knowing method-level overrides class-level |
| Concurrent Updates | Lost Updates | Not using locking |
| MANDATORY vs REQUIRED | Transaction Requirement | Using wrong propagation |
| Exception Handling | Rollback Marking | Catching exception but transaction still rolls back |
| Private Methods | Proxy Limitation | Using @Transactional on private methods |
| Async Methods | Thread Context | Combining @Transactional with @Async |

---

## Quick Reference

### When Transaction Rolls Back
- ✓ RuntimeException (unchecked)
- ✓ Error
- ✗ Checked Exception (unless specified in rollbackFor)

### When Transaction Doesn't Work
- ✗ Self-invocation (calling from same class)
- ✗ Private methods
- ✗ Final methods
- ✗ Static methods
- ✗ Non-Spring managed beans

### Common Solutions
1. **Self-Invocation**: Use self-injection or separate service
2. **Checked Exception**: Add `rollbackFor = Exception.class`
3. **Private Methods**: Make method public
4. **Async Methods**: Move @Transactional to separate service
5. **Lost Updates**: Use pessimistic or optimistic locking
6. **Exception Handling**: Use REQUIRES_NEW or NESTED for partial rollback

---

## Best Practices

1. ✅ Always use `rollbackFor = Exception.class` for checked exceptions
2. ✅ Avoid self-invocation of transactional methods
3. ✅ Use @Transactional only on public methods
4. ✅ Don't combine @Transactional with @Async on same method
5. ✅ Use appropriate isolation level for your use case
6. ✅ Set timeout for long-running operations
7. ✅ Use REQUIRES_NEW for audit logging
8. ✅ Use NESTED for batch processing with partial rollback
9. ✅ Use pessimistic/optimistic locking for concurrent updates
10. ✅ Test transaction behavior thoroughly

---

## Additional Resources

- [Spring Transaction Management Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Spring @Transactional Deep Dive](Spring_Transactional_Deep_Dive.md)
- [Spring @Transactional NOT_SUPPORTED Deep Dive](Spring_Transactional_NOT_SUPPORTED_Deep_Dive.md)
