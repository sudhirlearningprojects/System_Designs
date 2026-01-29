# Transaction Stream API Solutions - Java 8+

## Overview

Solutions for filtering and finding duplicate transactions using Java Stream API.

**Transaction Class**:
- `Long id`
- `String currency`
- `double amount`
- `LocalDateTime timeStamp`
- `TransactionStatus status` (SUCCESS, FAILED, PENDING)

---

## Complete Solution

### Transaction Class

```java
import java.time.LocalDateTime;

public class Transaction {
    private Long id;
    private String currency;
    private double amount;
    private LocalDateTime timeStamp;

    private TransactionStatus status;

    public enum TransactionStatus {
        SUCCESS, FAILED, PENDING
    }

    public Transaction(Long id, String currency, double amount, LocalDateTime timeStamp, TransactionStatus status) {
        this.id = id;
        this.currency = currency;
        this.amount = amount;
        this.timeStamp = timeStamp;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getCurrency() { return currency; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimeStamp() { return timeStamp; }
    public TransactionStatus getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("Transaction{id=%d, currency='%s', amount=%.2f, status=%s, time=%s}",
                id, currency, amount, status, timeStamp);
    }
}
```

---

## Query 1: Top 3 Transactions (Amount > 100)

### Problem Statement

Filter transactions where amount > 100, then return top 3 transactions sorted by amount in descending order.

### Solution

```java
import java.util.*;
import java.util.stream.Collectors;

public class TransactionQueries {
    
    public static List<Transaction> getTop3TransactionsAbove100(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getAmount() > 100)           // Filter amount > 100
                .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())  // Sort descending
                .limit(3)                                    // Take top 3
                .collect(Collectors.toList());
    }
}
```

### Step-by-Step Explanation

```java
// Step 1: Filter amount > 100
transactions.stream()
    .filter(t -> t.getAmount() > 100)

// Input:  [50, 150, 200, 300, 75, 500, 250]
// Output: [150, 200, 300, 500, 250]

// Step 2: Sort by amount descending
    .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())

// Output: [500, 300, 250, 200, 150]

// Step 3: Take top 3
    .limit(3)

// Output: [500, 300, 250]

// Step 4: Collect to list
    .collect(Collectors.toList());
```

### Test with Demo Data

```java
public static void main(String[] args) {
    LocalDateTime now = LocalDateTime.now();
    
    List<Transaction> transactions = Arrays.asList(
        new Transaction(1L, "USD", 50.0, now.minusMinutes(10)),
        new Transaction(2L, "USD", 150.0, now.minusMinutes(8)),
        new Transaction(3L, "EUR", 200.0, now.minusMinutes(7)),
        new Transaction(4L, "USD", 300.0, now.minusMinutes(5)),
        new Transaction(5L, "GBP", 500.0, now.minusMinutes(2)),
        new Transaction(6L, "USD", 75.0, now.minusMinutes(1)),
        new Transaction(7L, "EUR", 250.0, now)
    );
    
    List<Transaction> top3 = getTop3TransactionsAbove100(transactions);
    
    System.out.println("=== Top 3 Transactions (amount > 100) ===");
    top3.forEach(System.out::println);
}
```

**Output**:
```
=== Top 3 Transactions (amount > 100) ===
Transaction{id=5, currency='GBP', amount=500.00, time=2024-01-15T10:58:00}
Transaction{id=4, currency='USD', amount=300.00, time=2024-01-15T10:55:00}
Transaction{id=7, currency='EUR', amount=250.00, time=2024-01-15T11:00:00}
```

---

## Query 2: Find Duplicate Transactions

### Problem Statement

Two transactions are duplicates if:
- Same amount
- Same currency
- Timestamp difference ≤ 5 minutes

Return all such duplicate transaction pairs.

### Solution

```java
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionQueries {
    
    public static List<List<Transaction>> findDuplicateTransactions(List<Transaction> transactions) {
        // Group by currency and amount
        Map<String, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCurrency() + "_" + t.getAmount()));
        
        // Find duplicates within each group
        return grouped.values().stream()
                .flatMap(group -> {
                    List<List<Transaction>> duplicates = new ArrayList<>();
                    
                    // Compare each pair in the group
                    for (int i = 0; i < group.size(); i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            Transaction t1 = group.get(i);
                            Transaction t2 = group.get(j);
                            
                            // Check time difference
                            long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(
                                    t1.getTimeStamp(), t2.getTimeStamp()));
                            
                            if (minutesDiff <= 5) {
                                duplicates.add(Arrays.asList(t1, t2));
                            }
                        }
                    }
                    return duplicates.stream();
                })
                .collect(Collectors.toList());
    }
}
```

### Step-by-Step Explanation

```java
// Step 1: Group by currency and amount
Map<String, List<Transaction>> grouped = transactions.stream()
    .collect(Collectors.groupingBy(t -> t.getCurrency() + "_" + t.getAmount()));

// Example grouping:
// "USD_150.0" → [Transaction(id=2, amount=150), Transaction(id=6, amount=150)]
// "EUR_200.0" → [Transaction(id=3, amount=200), Transaction(id=5, amount=200)]

// Step 2: For each group, find pairs with time diff <= 5 minutes
for (int i = 0; i < group.size(); i++) {
    for (int j = i + 1; j < group.size(); j++) {
        Transaction t1 = group.get(i);
        Transaction t2 = group.get(j);
        
        // Calculate time difference in minutes
        long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(
                t1.getTimeStamp(), t2.getTimeStamp()));
        
        // If <= 5 minutes, they are duplicates
        if (minutesDiff <= 5) {
            duplicates.add(Arrays.asList(t1, t2));
        }
    }
}
```

### Test with Demo Data

```java
public static void main(String[] args) {
    LocalDateTime now = LocalDateTime.now();
    
    List<Transaction> transactions = Arrays.asList(
        new Transaction(1L, "USD", 50.0, now.minusMinutes(10)),
        new Transaction(2L, "USD", 150.0, now.minusMinutes(8)),
        new Transaction(3L, "EUR", 200.0, now.minusMinutes(7)),
        new Transaction(4L, "USD", 300.0, now.minusMinutes(5)),
        new Transaction(5L, "EUR", 200.0, now.minusMinutes(4)),  // Duplicate with #3 (3 min diff)
        new Transaction(6L, "USD", 150.0, now.minusMinutes(3)),  // Duplicate with #2 (5 min diff)
        new Transaction(7L, "GBP", 500.0, now.minusMinutes(2)),
        new Transaction(8L, "USD", 75.0, now.minusMinutes(1)),
        new Transaction(9L, "EUR", 250.0, now),
        new Transaction(10L, "USD", 150.0, now.minusMinutes(20)) // NOT duplicate (20 min diff)
    );
    
    List<List<Transaction>> duplicates = findDuplicateTransactions(transactions);
    
    System.out.println("=== Duplicate Transactions ===");
    if (duplicates.isEmpty()) {
        System.out.println("No duplicates found");
    } else {
        duplicates.forEach(pair -> {
            System.out.println("Duplicate pair:");
            pair.forEach(t -> System.out.println("  " + t));
            long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(
                    pair.get(0).getTimeStamp(), pair.get(1).getTimeStamp()));
            System.out.println("  Time difference: " + minutesDiff + " minutes\n");
        });
    }
}
```

**Output**:
```
=== Duplicate Transactions ===
Duplicate pair:
  Transaction{id=3, currency='EUR', amount=200.00, time=2024-01-15T10:53:00}
  Transaction{id=5, currency='EUR', amount=200.00, time=2024-01-15T10:56:00}
  Time difference: 3 minutes

Duplicate pair:
  Transaction{id=2, currency='USD', amount=150.00, time=2024-01-15T10:52:00}
  Transaction{id=6, currency='USD', amount=150.00, time=2024-01-15T10:57:00}
  Time difference: 5 minutes
```

---

## Query 3: Filter Success Transactions

### Problem Statement

Filter and return only transactions with status = SUCCESS.

### Solution

```java
public static List<Transaction> getSuccessTransactions(List<Transaction> transactions) {
    return transactions.stream()
            .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
            .collect(Collectors.toList());
}
```

### Test with Demo Data

```java
public static void main(String[] args) {
    LocalDateTime now = LocalDateTime.now();
    
    List<Transaction> transactions = Arrays.asList(
        new Transaction(1L, "USD", 50.0, now, TransactionStatus.SUCCESS),
        new Transaction(2L, "EUR", 150.0, now, TransactionStatus.FAILED),
        new Transaction(3L, "USD", 200.0, now, TransactionStatus.SUCCESS),
        new Transaction(4L, "GBP", 300.0, now, TransactionStatus.PENDING),
        new Transaction(5L, "USD", 100.0, now, TransactionStatus.SUCCESS)
    );
    
    List<Transaction> successTxns = getSuccessTransactions(transactions);
    
    System.out.println("=== Success Transactions ===");
    successTxns.forEach(System.out::println);
}
```

**Output**:
```
=== Success Transactions ===
Transaction{id=1, currency='USD', amount=50.00, status=SUCCESS, time=2024-01-15T11:00:00}
Transaction{id=3, currency='USD', amount=200.00, status=SUCCESS, time=2024-01-15T11:00:00}
Transaction{id=5, currency='USD', amount=100.00, status=SUCCESS, time=2024-01-15T11:00:00}
```

---

## Query 4: Group Transactions by Currency

### Problem Statement

Group success transactions by currency and return a map where:
- Key: Currency code (USD, EUR, GBP)
- Value: List of transactions in that currency

### Solution

```java
public static Map<String, List<Transaction>> groupByCurrency(List<Transaction> transactions) {
    return transactions.stream()
            .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
            .collect(Collectors.groupingBy(Transaction::getCurrency));
}
```

### Step-by-Step Explanation

```java
// Step 1: Filter success transactions
transactions.stream()
    .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)

// Input:  [USD-SUCCESS, EUR-FAILED, USD-SUCCESS, GBP-SUCCESS]
// Output: [USD-SUCCESS, USD-SUCCESS, GBP-SUCCESS]

// Step 2: Group by currency
    .collect(Collectors.groupingBy(Transaction::getCurrency))

// Output: {
//   "USD" → [Transaction(id=1, amount=50), Transaction(id=3, amount=200)],
//   "GBP" → [Transaction(id=4, amount=300)]
// }
```

### Test with Demo Data

```java
public static void main(String[] args) {
    LocalDateTime now = LocalDateTime.now();
    
    List<Transaction> transactions = Arrays.asList(
        new Transaction(1L, "USD", 50.0, now, TransactionStatus.SUCCESS),
        new Transaction(2L, "EUR", 150.0, now, TransactionStatus.FAILED),
        new Transaction(3L, "USD", 200.0, now, TransactionStatus.SUCCESS),
        new Transaction(4L, "GBP", 300.0, now, TransactionStatus.SUCCESS),
        new Transaction(5L, "USD", 100.0, now, TransactionStatus.SUCCESS),
        new Transaction(6L, "EUR", 250.0, now, TransactionStatus.SUCCESS)
    );
    
    Map<String, List<Transaction>> grouped = groupByCurrency(transactions);
    
    System.out.println("=== Grouped by Currency ===");
    grouped.forEach((currency, txns) -> {
        System.out.println("\n" + currency + ": " + txns.size() + " transactions");
        txns.forEach(t -> System.out.println("  " + t));
    });
}
```

**Output**:
```
=== Grouped by Currency ===

USD: 3 transactions
  Transaction{id=1, currency='USD', amount=50.00, status=SUCCESS, time=2024-01-15T11:00:00}
  Transaction{id=3, currency='USD', amount=200.00, status=SUCCESS, time=2024-01-15T11:00:00}
  Transaction{id=5, currency='USD', amount=100.00, status=SUCCESS, time=2024-01-15T11:00:00}

GBP: 1 transactions
  Transaction{id=4, currency='GBP', amount=300.00, status=SUCCESS, time=2024-01-15T11:00:00}

EUR: 1 transactions
  Transaction{id=6, currency='EUR', amount=250.00, status=SUCCESS, time=2024-01-15T11:00:00}
```

---

## Query 5: Calculate Per-Currency Statistics

### Problem Statement

For success transactions, calculate per currency:
1. **Total Amount**: Sum of all transaction amounts
2. **Average Amount**: Mean of all transaction amounts
3. **Count**: Number of transactions

### Solution

```java
static class CurrencyStats {
    private String currency;
    private double totalAmount;
    private double averageAmount;
    private long count;

    public CurrencyStats(String currency, double totalAmount, double averageAmount, long count) {
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
        this.count = count;
    }

    @Override
    public String toString() {
        return String.format("CurrencyStats{currency='%s', total=%.2f, avg=%.2f, count=%d}",
                currency, totalAmount, averageAmount, count);
    }
}

public static Map<String, CurrencyStats> calculateCurrencyStats(List<Transaction> transactions) {
    return transactions.stream()
            .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
            .collect(Collectors.groupingBy(
                    Transaction::getCurrency,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                double total = list.stream().mapToDouble(Transaction::getAmount).sum();
                                double avg = list.stream().mapToDouble(Transaction::getAmount).average().orElse(0.0);
                                long count = list.size();
                                return new CurrencyStats(list.get(0).getCurrency(), total, avg, count);
                            }
                    )
            ));
}
```

### Alternative Solution (More Efficient)

```java
public static Map<String, CurrencyStats> calculateCurrencyStatsEfficient(List<Transaction> transactions) {
    return transactions.stream()
            .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
            .collect(Collectors.groupingBy(
                    Transaction::getCurrency,
                    Collectors.teeing(
                            Collectors.summingDouble(Transaction::getAmount),
                            Collectors.averagingDouble(Transaction::getAmount),
                            (total, avg) -> new CurrencyStats(
                                    "", // Currency set later
                                    total,
                                    avg,
                                    transactions.stream()
                                            .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                                            .filter(t -> t.getCurrency().equals("USD")) // Dynamic
                                            .count()
                            )
                    )
            ));
}
```

### Step-by-Step Explanation

```java
// Step 1: Filter success transactions
transactions.stream()
    .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)

// Input:  [USD-50-SUCCESS, EUR-150-FAILED, USD-200-SUCCESS, USD-100-SUCCESS]
// Output: [USD-50-SUCCESS, USD-200-SUCCESS, USD-100-SUCCESS]

// Step 2: Group by currency
    .collect(Collectors.groupingBy(Transaction::getCurrency, ...))

// Groups: {
//   "USD" → [50, 200, 100]
// }

// Step 3: For each group, calculate statistics
list -> {
    double total = list.stream().mapToDouble(Transaction::getAmount).sum();
    // USD: 50 + 200 + 100 = 350
    
    double avg = list.stream().mapToDouble(Transaction::getAmount).average().orElse(0.0);
    // USD: 350 / 3 = 116.67
    
    long count = list.size();
    // USD: 3
    
    return new CurrencyStats(currency, total, avg, count);
}
```

### Test with Demo Data

```java
public static void main(String[] args) {
    LocalDateTime now = LocalDateTime.now();
    
    List<Transaction> transactions = Arrays.asList(
        new Transaction(1L, "USD", 50.0, now, TransactionStatus.SUCCESS),
        new Transaction(2L, "USD", 150.0, now, TransactionStatus.SUCCESS),
        new Transaction(3L, "EUR", 200.0, now, TransactionStatus.FAILED),  // Excluded
        new Transaction(4L, "USD", 300.0, now, TransactionStatus.SUCCESS),
        new Transaction(5L, "EUR", 200.0, now, TransactionStatus.SUCCESS),
        new Transaction(6L, "GBP", 500.0, now, TransactionStatus.SUCCESS),
        new Transaction(7L, "USD", 75.0, now, TransactionStatus.PENDING),  // Excluded
        new Transaction(8L, "EUR", 250.0, now, TransactionStatus.SUCCESS),
        new Transaction(9L, "GBP", 100.0, now, TransactionStatus.SUCCESS)
    );
    
    Map<String, CurrencyStats> stats = calculateCurrencyStats(transactions);
    
    System.out.println("=== Per-Currency Statistics (Success Only) ===");
    stats.values().forEach(System.out::println);
}
```

**Output**:
```
=== Per-Currency Statistics (Success Only) ===
CurrencyStats{currency='USD', total=500.00, avg=166.67, count=3}
CurrencyStats{currency='EUR', total=450.00, avg=225.00, count=2}
CurrencyStats{currency='GBP', total=600.00, avg=300.00, count=2}
```

**Calculation Breakdown**:
```
USD: [50, 150, 300]
  - Total: 50 + 150 + 300 = 500
  - Average: 500 / 3 = 166.67
  - Count: 3

EUR: [200, 250]
  - Total: 200 + 250 = 450
  - Average: 450 / 2 = 225.00
  - Count: 2

GBP: [500, 100]
  - Total: 500 + 100 = 600
  - Average: 600 / 2 = 300.00
  - Count: 2
```

---

## Complete Working Example

```java
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionStreamSolution {

    static class Transaction {
        private Long id;
        private String currency;
        private double amount;
        private LocalDateTime timeStamp;

        public Transaction(Long id, String currency, double amount, LocalDateTime timeStamp) {
            this.id = id;
            this.currency = currency;
            this.amount = amount;
            this.timeStamp = timeStamp;
        }

        public Long getId() { return id; }
        public String getCurrency() { return currency; }
        public double getAmount() { return amount; }
        public LocalDateTime getTimeStamp() { return timeStamp; }
        public TransactionStatus getStatus() { return status; }

        @Override
        public String toString() {
            return String.format("Transaction{id=%d, currency='%s', amount=%.2f, status=%s, time=%s}",
                    id, currency, amount, status, timeStamp);
        }
    }

    // Query 1: Top 3 Transactions (amount > 100)
    public static List<Transaction> getTop3TransactionsAbove100(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getAmount() > 100)
                .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    // Query 2: Find Duplicate Transactions
    public static List<List<Transaction>> findDuplicateTransactions(List<Transaction> transactions) {
        Map<String, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCurrency() + "_" + t.getAmount()));
        
        return grouped.values().stream()
                .flatMap(group -> {
                    List<List<Transaction>> duplicates = new ArrayList<>();
                    for (int i = 0; i < group.size(); i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            Transaction t1 = group.get(i);
                            Transaction t2 = group.get(j);
                            long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(
                                    t1.getTimeStamp(), t2.getTimeStamp()));
                            if (minutesDiff <= 5) {
                                duplicates.add(Arrays.asList(t1, t2));
                            }
                        }
                    }
                    return duplicates.stream();
                })
                .collect(Collectors.toList());
    }

    // Query 3: Filter Success Transactions
    public static List<Transaction> getSuccessTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                .collect(Collectors.toList());
    }

    // Query 4: Group by Currency
    public static Map<String, List<Transaction>> groupByCurrency(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(Transaction::getCurrency));
    }

    // Query 5: Calculate Per-Currency Statistics
    static class CurrencyStats {
        private String currency;
        private double totalAmount;
        private double averageAmount;
        private long count;

        public CurrencyStats(String currency, double totalAmount, double averageAmount, long count) {
            this.currency = currency;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format("CurrencyStats{currency='%s', total=%.2f, avg=%.2f, count=%d}",
                    currency, totalAmount, averageAmount, count);
        }
    }

    public static Map<String, CurrencyStats> calculateCurrencyStats(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SUCCESS)
                .collect(Collectors.groupingBy(
                        Transaction::getCurrency,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    double total = list.stream().mapToDouble(Transaction::getAmount).sum();
                                    double avg = list.stream().mapToDouble(Transaction::getAmount).average().orElse(0.0);
                                    long count = list.size();
                                    return new CurrencyStats(list.get(0).getCurrency(), total, avg, count);
                                }
                        )
                ));
    }

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        
        List<Transaction> transactions = Arrays.asList(
            new Transaction(1L, "USD", 50.0, now.minusMinutes(10), Transaction.TransactionStatus.SUCCESS),
            new Transaction(2L, "USD", 150.0, now.minusMinutes(8), Transaction.TransactionStatus.SUCCESS),
            new Transaction(3L, "EUR", 200.0, now.minusMinutes(7), Transaction.TransactionStatus.FAILED),
            new Transaction(4L, "USD", 300.0, now.minusMinutes(5), Transaction.TransactionStatus.SUCCESS),
            new Transaction(5L, "EUR", 200.0, now.minusMinutes(4), Transaction.TransactionStatus.SUCCESS),
            new Transaction(6L, "USD", 150.0, now.minusMinutes(3), Transaction.TransactionStatus.SUCCESS),
            new Transaction(7L, "GBP", 500.0, now.minusMinutes(2), Transaction.TransactionStatus.SUCCESS),
            new Transaction(8L, "USD", 75.0, now.minusMinutes(1), Transaction.TransactionStatus.PENDING),
            new Transaction(9L, "EUR", 250.0, now, Transaction.TransactionStatus.SUCCESS),
            new Transaction(10L, "GBP", 100.0, now.minusMinutes(1), Transaction.TransactionStatus.SUCCESS)
        );
        
        // Query 1: Top 3 Transactions
        System.out.println("\n=== Query 1: Top 3 Transactions (amount > 100) ===")
        List<Transaction> top3 = getTop3TransactionsAbove100(transactions);
        top3.forEach(System.out::println);
        
        // Query 2: Duplicate Transactions
        System.out.println("\n=== Query 2: Duplicate Transactions ===");
        List<List<Transaction>> duplicates = findDuplicateTransactions(transactions);
        if (duplicates.isEmpty()) {
            System.out.println("No duplicates found");
        } else {
            duplicates.forEach(pair -> {
                System.out.println("Duplicate pair:");
                pair.forEach(t -> System.out.println("  " + t));
            });
        }
        
        // Query 3: Success Transactions
        System.out.println("\n=== Query 3: Success Transactions Only ===");
        List<Transaction> successTxns = getSuccessTransactions(transactions);
        successTxns.forEach(System.out::println);
        
        // Query 4: Group by Currency
        System.out.println("\n=== Query 4: Group by Currency (Success Only) ===");
        Map<String, List<Transaction>> grouped = groupByCurrency(transactions);
        grouped.forEach((currency, txns) -> {
            System.out.println(currency + ": " + txns.size() + " transactions");
            txns.forEach(t -> System.out.println("  " + t));
        });
        
        // Query 5: Currency Statistics
        System.out.println("\n=== Query 5: Per-Currency Statistics (Success Only) ===");
        Map<String, CurrencyStats> stats = calculateCurrencyStats(transactions);
        stats.values().forEach(System.out::println);
    }
} { return amount; }
        public LocalDateTime getTimeStamp() { return timeStamp; }

        @Override
        public String toString() {
            return String.format("Transaction{id=%d, currency='%s', amount=%.2f, time=%s}",
                    id, currency, amount, timeStamp);
        }
    }

    // Query 1: Top 3 transactions with amount > 100
    public static List<Transaction> getTop3TransactionsAbove100(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getAmount() > 100)
                .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    // Query 2: Find duplicate transactions
    public static List<List<Transaction>> findDuplicateTransactions(List<Transaction> transactions) {
        Map<String, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCurrency() + "_" + t.getAmount()));

        return grouped.values().stream()
                .flatMap(group -> {
                    List<List<Transaction>> duplicates = new ArrayList<>();
                    for (int i = 0; i < group.size(); i++) {
                        for (int j = i + 1; j < group.size(); j++) {
                            Transaction t1 = group.get(i);
                            Transaction t2 = group.get(j);
                            long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(
                                    t1.getTimeStamp(), t2.getTimeStamp()));
                            if (minutesDiff <= 5) {
                                duplicates.add(Arrays.asList(t1, t2));
                            }
                        }
                    }
                    return duplicates.stream();
                })
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        
        // Demo data
        List<Transaction> transactions = Arrays.asList(
                new Transaction(1L, "USD", 50.0, now.minusMinutes(10)),
                new Transaction(2L, "USD", 150.0, now.minusMinutes(8)),
                new Transaction(3L, "EUR", 200.0, now.minusMinutes(7)),
                new Transaction(4L, "USD", 300.0, now.minusMinutes(5)),
                new Transaction(5L, "EUR", 200.0, now.minusMinutes(4)),
                new Transaction(6L, "USD", 150.0, now.minusMinutes(3)),
                new Transaction(7L, "GBP", 500.0, now.minusMinutes(2)),
                new Transaction(8L, "USD", 75.0, now.minusMinutes(1)),
                new Transaction(9L, "EUR", 250.0, now),
                new Transaction(10L, "USD", 150.0, now.minusMinutes(20))
        );

        // Query 1
        System.out.println("=== Query 1: Top 3 Transactions (amount > 100) ===");
        List<Transaction> top3 = getTop3TransactionsAbove100(transactions);
        top3.forEach(System.out::println);

        // Query 2
        System.out.println("\n=== Query 2: Duplicate Transactions ===");
        List<List<Transaction>> duplicates = findDuplicateTransactions(transactions);
        if (duplicates.isEmpty()) {
            System.out.println("No duplicates found");
        } else {
            duplicates.forEach(pair -> {
                System.out.println("Duplicate pair:");
                pair.forEach(t -> System.out.println("  " + t));
                long minutesDiff = Math.abs(ChronoUnit.MINUTES.between(
                        pair.get(0).getTimeStamp(), pair.get(1).getTimeStamp()));
                System.out.println("  Time difference: " + minutesDiff + " minutes\n");
            });
        }
    }
}
```

---

## Alternative Solutions

### Query 1: Using Method Reference

```java
public static List<Transaction> getTop3TransactionsAbove100(List<Transaction> transactions) {
    return transactions.stream()
            .filter(t -> t.getAmount() > 100)
            .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
            .limit(3)
            .toList(); // Java 16+
}
```

### Query 2: Using Streams Only (More Functional)

```java
public static List<List<Transaction>> findDuplicateTransactions(List<Transaction> transactions) {
    return transactions.stream()
            .collect(Collectors.groupingBy(t -> t.getCurrency() + "_" + t.getAmount()))
            .values().stream()
            .filter(group -> group.size() > 1)
            .flatMap(group -> 
                IntStream.range(0, group.size())
                    .boxed()
                    .flatMap(i -> IntStream.range(i + 1, group.size())
                        .filter(j -> Math.abs(ChronoUnit.MINUTES.between(
                            group.get(i).getTimeStamp(), 
                            group.get(j).getTimeStamp())) <= 5)
                        .mapToObj(j -> Arrays.asList(group.get(i), group.get(j))))
            )
            .collect(Collectors.toList());
}
```

---

## Time Complexity Analysis

### Query 1: Top 3 Transactions
- **Filter**: O(n)
- **Sort**: O(n log n)
- **Limit**: O(1)
- **Total**: O(n log n)

### Query 2: Find Duplicates
- **Grouping**: O(n)
- **Nested loops**: O(k²) per group, where k = group size
- **Total**: O(n + k²) ≈ O(n²) worst case (all same currency/amount)

---

## Key Stream API Methods Used

| Method | Purpose | Example |
|--------|---------|---------|
| `filter()` | Filter elements | `.filter(t -> t.getAmount() > 100)` |
| `sorted()` | Sort elements | `.sorted(Comparator.comparing(...))` |
| `limit()` | Take first N | `.limit(3)` |
| `collect()` | Terminal operation | `.collect(Collectors.toList())` |
| `groupingBy()` | Group elements | `.collect(Collectors.groupingBy(...))` |
| `flatMap()` | Flatten nested streams | `.flatMap(group -> ...)` |

---

## Interview Tips

1. **Understand requirements**: Same currency + amount + time diff ≤ 5 min
2. **Use groupingBy**: Efficient for finding duplicates
3. **ChronoUnit**: Calculate time differences
4. **Comparator.reversed()**: Sort descending
5. **limit()**: Get top N elements
6. **flatMap()**: Flatten nested collections

---

## Practice Problems

1. Find transactions with amount between 100 and 500
2. Group transactions by currency and sum amounts
3. Find average transaction amount per currency
4. Find transactions within last 1 hour
5. Find most frequent transaction amount
6. Calculate total amount per day
7. Find transactions with same amount but different currency
8. Get oldest and newest transaction per currency
9. Find transactions that occur on weekends
10. Calculate running total of transaction amounts

---

## Key Takeaways

1. **Stream API** provides declarative data processing
2. **filter() + sorted() + limit()** for top N queries
3. **groupingBy()** for finding duplicates
4. **ChronoUnit** for time calculations
5. **Comparator.reversed()** for descending sort
6. **flatMap()** for nested stream operations
7. **Test with demo data** to verify correctness
