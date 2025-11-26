# Saga Pattern - Deep Dive

## Table of Contents
1. [Introduction](#introduction)
2. [Theory and Concepts](#theory-and-concepts)
3. [Types of Sagas](#types-of-sagas)
4. [Implementation Patterns](#implementation-patterns)
5. [Practical Examples](#practical-examples)
6. [Best Practices](#best-practices)
7. [Common Pitfalls](#common-pitfalls)

---

## Introduction

The **Saga Pattern** is a design pattern for managing distributed transactions across multiple microservices. Instead of using traditional ACID transactions, Sagas break down a transaction into a series of local transactions, each with a compensating transaction to undo changes if something goes wrong.

### Why Saga Pattern?

In a monolithic application, you can use database transactions (ACID) to ensure data consistency. However, in microservices:
- Each service has its own database
- Distributed transactions (2PC) are slow and complex
- Network failures are common
- Services may be unavailable

**Saga Pattern solves this by:**
- Breaking transactions into smaller, local transactions
- Providing compensating transactions for rollback
- Maintaining eventual consistency
- Improving system resilience

---

## Theory and Concepts

### Core Principles

1. **Local Transactions**: Each service executes its own local transaction
2. **Compensating Transactions**: Undo operations to rollback changes
3. **Eventual Consistency**: System reaches consistent state eventually
4. **Idempotency**: Operations can be safely retried

### Saga Execution Flow

```
Success Flow:
Service A → Service B → Service C → Service D ✓

Failure Flow (Rollback):
Service A → Service B → Service C ✗ → Compensate C → Compensate B → Compensate A
```

### Key Components

1. **Saga Orchestrator/Coordinator**: Manages saga execution
2. **Saga Participants**: Services that execute local transactions
3. **Compensating Transactions**: Rollback operations
4. **Saga Log**: Tracks saga state for recovery

---

## Types of Sagas

### 1. Choreography-Based Saga

Services communicate through events without a central coordinator.

**How it works:**
- Each service listens to events and publishes new events
- No central orchestrator
- Services know what to do based on events

**Pros:**
- Simple for small sagas
- No single point of failure
- Loose coupling

**Cons:**
- Hard to understand flow
- Difficult to debug
- Cyclic dependencies risk

**Example Flow:**
```
Order Service → OrderCreated Event
    ↓
Payment Service → PaymentProcessed Event
    ↓
Inventory Service → InventoryReserved Event
    ↓
Shipping Service → ShipmentScheduled Event
```

### 2. Orchestration-Based Saga

A central orchestrator coordinates the saga execution.

**How it works:**
- Orchestrator sends commands to services
- Services respond with success/failure
- Orchestrator decides next step

**Pros:**
- Clear flow and easy to understand
- Centralized logic
- Easy to add new steps
- Better monitoring

**Cons:**
- Single point of failure
- Orchestrator can become complex
- Additional service to maintain

**Example Flow:**
```
Saga Orchestrator
    ↓ Command: CreateOrder
Order Service → Response: Success
    ↓ Command: ProcessPayment
Payment Service → Response: Success
    ↓ Command: ReserveInventory
Inventory Service → Response: Failure
    ↓ Command: RefundPayment
Payment Service → Response: Success
    ↓ Command: CancelOrder
Order Service → Response: Success
```

---

## Implementation Patterns

### Pattern 1: Orchestration with State Machine

```java
public class OrderSagaOrchestrator {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final SagaRepository sagaRepository;
    
    public void executeOrderSaga(OrderRequest request) {
        SagaInstance saga = new SagaInstance(UUID.randomUUID().toString());
        saga.setState(SagaState.STARTED);
        sagaRepository.save(saga);
        
        try {
            // Step 1: Create Order
            saga.setState(SagaState.ORDER_CREATED);
            Order order = orderService.createOrder(request);
            saga.setOrderId(order.getId());
            sagaRepository.save(saga);
            
            // Step 2: Process Payment
            saga.setState(SagaState.PAYMENT_PROCESSING);
            Payment payment = paymentService.processPayment(order.getTotalAmount(), request.getPaymentMethod());
            saga.setPaymentId(payment.getId());
            sagaRepository.save(saga);
            
            // Step 3: Reserve Inventory
            saga.setState(SagaState.INVENTORY_RESERVING);
            inventoryService.reserveItems(order.getItems());
            sagaRepository.save(saga);
            
            // Step 4: Schedule Shipping
            saga.setState(SagaState.SHIPPING_SCHEDULING);
            shippingService.scheduleShipment(order);
            saga.setState(SagaState.COMPLETED);
            sagaRepository.save(saga);
            
        } catch (Exception e) {
            // Compensate in reverse order
            compensate(saga);
        }
    }
    
    private void compensate(SagaInstance saga) {
        try {
            switch (saga.getState()) {
                case SHIPPING_SCHEDULING:
                    shippingService.cancelShipment(saga.getOrderId());
                case INVENTORY_RESERVING:
                    inventoryService.releaseItems(saga.getOrderId());
                case PAYMENT_PROCESSING:
                    paymentService.refundPayment(saga.getPaymentId());
                case ORDER_CREATED:
                    orderService.cancelOrder(saga.getOrderId());
                    break;
            }
            saga.setState(SagaState.COMPENSATED);
        } catch (Exception e) {
            saga.setState(SagaState.COMPENSATION_FAILED);
            // Alert operations team
        }
        sagaRepository.save(saga);
    }
}

enum SagaState {
    STARTED,
    ORDER_CREATED,
    PAYMENT_PROCESSING,
    INVENTORY_RESERVING,
    SHIPPING_SCHEDULING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}
```

### Pattern 2: Event-Driven Choreography

```java
// Order Service
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Transactional
    public void createOrder(OrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);
        
        // Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            order.getItems()
        );
        kafkaTemplate.send("order-events", event);
    }
    
    @KafkaListener(topics = "payment-events")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Order order = orderRepository.findById(event.getOrderId());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}

// Payment Service
@Service
public class PaymentService {
    
    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    @KafkaListener(topics = "order-events")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            Payment payment = processPayment(event.getAmount(), event.getCustomerId());
            
            PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                event.getOrderId(),
                payment.getId(),
                payment.getAmount()
            );
            kafkaTemplate.send("payment-events", successEvent);
            
        } catch (PaymentException e) {
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("payment-events", failedEvent);
        }
    }
    
    @KafkaListener(topics = "inventory-events")
    public void handleInventoryFailed(InventoryFailedEvent event) {
        // Compensate: Refund payment
        Payment payment = paymentRepository.findByOrderId(event.getOrderId());
        refundPayment(payment.getId());
    }
}

// Inventory Service
@Service
public class InventoryService {
    
    @KafkaListener(topics = "payment-events")
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        try {
            reserveInventory(event.getOrderId());
            
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                event.getOrderId()
            );
            kafkaTemplate.send("inventory-events", reservedEvent);
            
        } catch (InsufficientStockException e) {
            InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("inventory-events", failedEvent);
        }
    }
}
```

### Pattern 3: Saga with Timeout and Retry

```java
@Service
public class ResilientSagaOrchestrator {
    
    private static final int MAX_RETRIES = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    public Mono<SagaResult> executeWithRetry(OrderRequest request) {
        return Mono.defer(() -> executeSaga(request))
            .timeout(TIMEOUT)
            .retry(MAX_RETRIES)
            .onErrorResume(this::handleSagaFailure);
    }
    
    private Mono<SagaResult> executeSaga(OrderRequest request) {
        return orderService.createOrder(request)
            .flatMap(order -> paymentService.processPayment(order)
                .flatMap(payment -> inventoryService.reserve(order)
                    .flatMap(inventory -> shippingService.schedule(order)
                        .map(shipping -> SagaResult.success(order, payment, inventory, shipping))
                    )
                    .onErrorResume(e -> compensateInventory(order, payment))
                )
                .onErrorResume(e -> compensatePayment(order))
            )
            .onErrorResume(e -> compensateOrder(request));
    }
    
    private Mono<SagaResult> compensateInventory(Order order, Payment payment) {
        return inventoryService.release(order.getId())
            .then(paymentService.refund(payment.getId()))
            .then(orderService.cancel(order.getId()))
            .then(Mono.just(SagaResult.failed("Inventory reservation failed")));
    }
}
```

---

## Practical Examples

### Example 1: E-Commerce Order Processing

**Scenario**: Customer places an order

**Steps:**
1. Create order in Order Service
2. Charge payment in Payment Service
3. Reserve inventory in Inventory Service
4. Schedule shipping in Shipping Service

**Compensating Transactions:**
1. Cancel shipment
2. Release inventory
3. Refund payment
4. Cancel order

```java
@Service
public class EcommerceSaga {
    
    public void processOrder(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        SagaLog log = new SagaLog(sagaId);
        
        try {
            // Step 1: Create Order
            Order order = orderService.createOrder(request);
            log.addStep("CREATE_ORDER", order.getId(), this::cancelOrder);
            
            // Step 2: Process Payment
            Payment payment = paymentService.charge(order.getTotalAmount());
            log.addStep("PROCESS_PAYMENT", payment.getId(), this::refundPayment);
            
            // Step 3: Reserve Inventory
            inventoryService.reserve(order.getItems());
            log.addStep("RESERVE_INVENTORY", order.getId(), this::releaseInventory);
            
            // Step 4: Schedule Shipping
            shippingService.schedule(order);
            log.addStep("SCHEDULE_SHIPPING", order.getId(), this::cancelShipping);
            
            log.markCompleted();
            
        } catch (Exception e) {
            log.compensate(); // Execute compensating transactions in reverse
            throw new SagaException("Order processing failed", e);
        }
    }
}
```

### Example 2: Travel Booking Saga

**Scenario**: Book flight, hotel, and car rental

```java
@Service
public class TravelBookingSaga {
    
    public BookingResult bookTrip(TravelRequest request) {
        SagaInstance saga = sagaRepository.create();
        
        try {
            // Book Flight
            Flight flight = flightService.book(request.getFlightDetails());
            saga.recordStep("FLIGHT_BOOKED", flight.getId());
            
            // Book Hotel
            Hotel hotel = hotelService.book(request.getHotelDetails());
            saga.recordStep("HOTEL_BOOKED", hotel.getId());
            
            // Book Car
            Car car = carService.book(request.getCarDetails());
            saga.recordStep("CAR_BOOKED", car.getId());
            
            saga.complete();
            return BookingResult.success(flight, hotel, car);
            
        } catch (Exception e) {
            // Compensate in reverse order
            if (saga.hasStep("CAR_BOOKED")) {
                carService.cancel(saga.getStepData("CAR_BOOKED"));
            }
            if (saga.hasStep("HOTEL_BOOKED")) {
                hotelService.cancel(saga.getStepData("HOTEL_BOOKED"));
            }
            if (saga.hasStep("FLIGHT_BOOKED")) {
                flightService.cancel(saga.getStepData("FLIGHT_BOOKED"));
            }
            
            saga.markFailed();
            return BookingResult.failed(e.getMessage());
        }
    }
}
```

### Example 3: Money Transfer Saga

**Scenario**: Transfer money between accounts

```java
@Service
public class MoneyTransferSaga {
    
    @Transactional
    public TransferResult transfer(String fromAccount, String toAccount, BigDecimal amount) {
        SagaContext context = new SagaContext();
        
        try {
            // Step 1: Debit from source account
            accountService.debit(fromAccount, amount);
            context.addCompensation(() -> accountService.credit(fromAccount, amount));
            
            // Step 2: Credit to destination account
            accountService.credit(toAccount, amount);
            context.addCompensation(() -> accountService.debit(toAccount, amount));
            
            // Step 3: Record transaction
            Transaction txn = transactionService.record(fromAccount, toAccount, amount);
            context.addCompensation(() -> transactionService.reverse(txn.getId()));
            
            // Step 4: Send notification
            notificationService.sendTransferNotification(fromAccount, toAccount, amount);
            
            return TransferResult.success(txn);
            
        } catch (Exception e) {
            context.executeCompensations(); // Rollback
            return TransferResult.failed(e.getMessage());
        }
    }
}
```

---

## Best Practices

### 1. Design Idempotent Operations

```java
@Service
public class IdempotentPaymentService {
    
    @Transactional
    public Payment processPayment(String idempotencyKey, PaymentRequest request) {
        // Check if already processed
        Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing; // Return existing result
        }
        
        // Process payment
        Payment payment = new Payment(request);
        payment.setIdempotencyKey(idempotencyKey);
        return paymentRepository.save(payment);
    }
}
```

### 2. Implement Saga Log for Recovery

```java
@Entity
public class SagaLog {
    @Id
    private String sagaId;
    private SagaState state;
    private List<SagaStep> steps;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public void addStep(String stepName, String stepData, CompensatingAction compensation) {
        steps.add(new SagaStep(stepName, stepData, compensation));
    }
    
    public void compensate() {
        // Execute compensations in reverse order
        for (int i = steps.size() - 1; i >= 0; i--) {
            SagaStep step = steps.get(i);
            try {
                step.getCompensation().execute();
                step.setCompensated(true);
            } catch (Exception e) {
                step.setCompensationFailed(true);
                // Log and alert
            }
        }
    }
}
```

### 3. Use Timeouts and Deadlines

```java
@Service
public class TimeoutAwareSaga {
    
    private static final Duration SAGA_TIMEOUT = Duration.ofMinutes(5);
    
    public CompletableFuture<SagaResult> executeWithTimeout(OrderRequest request) {
        CompletableFuture<SagaResult> future = CompletableFuture.supplyAsync(() -> 
            executeSaga(request)
        );
        
        return future.orTimeout(SAGA_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    compensate(request);
                    return SagaResult.timeout();
                }
                return SagaResult.failed(ex.getMessage());
            });
    }
}
```

### 4. Monitor Saga Execution

```java
@Aspect
@Component
public class SagaMonitoringAspect {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Around("@annotation(Saga)")
    public Object monitorSaga(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        String sagaName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder("saga.execution")
                .tag("saga", sagaName)
                .tag("status", "success")
                .register(meterRegistry));
            return result;
            
        } catch (Exception e) {
            sample.stop(Timer.builder("saga.execution")
                .tag("saga", sagaName)
                .tag("status", "failed")
                .register(meterRegistry));
            
            meterRegistry.counter("saga.compensation", "saga", sagaName).increment();
            throw e;
        }
    }
}
```

### 5. Handle Partial Failures

```java
@Service
public class PartialFailureHandler {
    
    public void handlePartialFailure(SagaInstance saga) {
        // Retry failed step
        if (saga.getRetryCount() < MAX_RETRIES) {
            saga.incrementRetryCount();
            retryFailedStep(saga);
        } else {
            // Move to manual intervention queue
            manualInterventionQueue.add(saga);
            alertOps(saga);
        }
    }
    
    private void retryFailedStep(SagaInstance saga) {
        SagaStep failedStep = saga.getLastFailedStep();
        try {
            failedStep.retry();
            saga.markStepSuccess(failedStep);
            continueFromStep(saga, failedStep);
        } catch (Exception e) {
            handlePartialFailure(saga);
        }
    }
}
```

---

## Common Pitfalls

### 1. ❌ Non-Idempotent Compensations

**Problem:**
```java
// BAD: Not idempotent
public void refundPayment(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId);
    payment.setAmount(payment.getAmount().negate()); // Can be called multiple times!
    paymentRepository.save(payment);
}
```

**Solution:**
```java
// GOOD: Idempotent
public void refundPayment(String paymentId) {
    Payment payment = paymentRepository.findById(paymentId);
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}
```

### 2. ❌ Missing Saga State Persistence

**Problem:**
```java
// BAD: State only in memory
public void executeSaga(OrderRequest request) {
    Order order = createOrder(request); // If crash here, no way to recover
    Payment payment = processPayment(order);
    reserveInventory(order);
}
```

**Solution:**
```java
// GOOD: Persist state after each step
public void executeSaga(OrderRequest request) {
    SagaInstance saga = sagaRepository.save(new SagaInstance());
    
    Order order = createOrder(request);
    saga.recordStep("ORDER_CREATED", order.getId());
    sagaRepository.save(saga);
    
    Payment payment = processPayment(order);
    saga.recordStep("PAYMENT_PROCESSED", payment.getId());
    sagaRepository.save(saga);
}
```

### 3. ❌ Ignoring Compensation Failures

**Problem:**
```java
// BAD: Silent failure
try {
    compensate(saga);
} catch (Exception e) {
    // Ignored!
}
```

**Solution:**
```java
// GOOD: Handle compensation failures
try {
    compensate(saga);
} catch (Exception e) {
    saga.setState(SagaState.COMPENSATION_FAILED);
    sagaRepository.save(saga);
    alertOpsTeam(saga, e);
    moveToManualQueue(saga);
}
```

### 4. ❌ Circular Dependencies in Choreography

**Problem:**
```
Order Service → Payment Service → Inventory Service → Order Service (cycle!)
```

**Solution:**
- Use orchestration for complex flows
- Design clear event chains
- Avoid bidirectional dependencies

### 5. ❌ Not Handling Duplicate Messages

**Problem:**
```java
// BAD: No duplicate check
@KafkaListener(topics = "order-events")
public void handleOrderCreated(OrderCreatedEvent event) {
    processPayment(event.getOrderId()); // Can be called multiple times!
}
```

**Solution:**
```java
// GOOD: Idempotency check
@KafkaListener(topics = "order-events")
public void handleOrderCreated(OrderCreatedEvent event) {
    if (paymentRepository.existsByOrderId(event.getOrderId())) {
        return; // Already processed
    }
    processPayment(event.getOrderId());
}
```

---

## Comparison: Saga vs 2PC

| Aspect | Saga Pattern | Two-Phase Commit (2PC) |
|--------|--------------|------------------------|
| **Consistency** | Eventual | Strong (ACID) |
| **Performance** | High | Low (blocking) |
| **Availability** | High | Low (coordinator SPOF) |
| **Complexity** | Medium | High |
| **Failure Handling** | Compensating transactions | Rollback |
| **Use Case** | Microservices | Monoliths, distributed DBs |

---

## Real-World Examples

### Netflix - Choreography-Based Saga
- Uses event-driven architecture
- Services publish domain events
- Other services react to events
- No central orchestrator

### Uber - Orchestration-Based Saga
- Trip booking saga
- Central orchestrator coordinates ride booking
- Handles driver assignment, payment, and notifications

### Amazon - Hybrid Approach
- Order fulfillment uses orchestration
- Inventory management uses choreography
- Combines both patterns based on use case

---

## Conclusion

The Saga Pattern is essential for managing distributed transactions in microservices. Choose:
- **Choreography** for simple, loosely coupled flows
- **Orchestration** for complex, coordinated flows

Key takeaways:
- Always design idempotent operations
- Persist saga state for recovery
- Handle compensation failures gracefully
- Monitor saga execution
- Test failure scenarios thoroughly
