# Fraud Detection - Suspicious Transactions

**Level**: Senior (5+ years)  
**Companies**: Stripe, PayPal, Square  
**Concepts**: Pattern matching, Complex conditions, Window functions

## Problem

Detect potentially fraudulent transactions:
1. Multiple transactions from same card within 10 minutes
2. Transactions from different cities within 1 hour (impossible travel)
3. Transaction amount > 3x user's average

## Schema

```sql
CREATE TABLE transactions (
    transaction_id INT PRIMARY KEY,
    user_id INT,
    card_number VARCHAR(16),
    amount DECIMAL(10, 2),
    city VARCHAR(50),
    transaction_time TIMESTAMP
);

INSERT INTO transactions VALUES
(1, 101, '1234', 100.00, 'NYC', '2024-01-01 10:00:00'),
(2, 101, '1234', 150.00, 'NYC', '2024-01-01 10:05:00'),
(3, 101, '1234', 200.00, 'LA', '2024-01-01 10:30:00'),
(4, 102, '5678', 50.00, 'Chicago', '2024-01-01 11:00:00'),
(5, 101, '1234', 5000.00, 'NYC', '2024-01-01 12:00:00');
```

## Solution

```sql
WITH user_avg AS (
    SELECT user_id, AVG(amount) AS avg_amount
    FROM transactions
    GROUP BY user_id
),
rapid_transactions AS (
    SELECT 
        t1.transaction_id,
        t1.user_id,
        COUNT(*) AS txn_count_10min
    FROM transactions t1
    JOIN transactions t2 
        ON t1.card_number = t2.card_number
        AND t2.transaction_time BETWEEN t1.transaction_time - INTERVAL '10 minutes' 
                                    AND t1.transaction_time
    GROUP BY t1.transaction_id, t1.user_id
    HAVING COUNT(*) >= 3
),
impossible_travel AS (
    SELECT DISTINCT
        t1.transaction_id,
        t1.user_id,
        t1.city AS city1,
        t2.city AS city2,
        EXTRACT(EPOCH FROM (t2.transaction_time - t1.transaction_time))/3600 AS hours_diff
    FROM transactions t1
    JOIN transactions t2 
        ON t1.user_id = t2.user_id
        AND t1.transaction_id < t2.transaction_id
        AND t1.city != t2.city
        AND t2.transaction_time BETWEEN t1.transaction_time 
                                    AND t1.transaction_time + INTERVAL '1 hour'
),
high_amount AS (
    SELECT t.transaction_id, t.user_id
    FROM transactions t
    JOIN user_avg ua ON t.user_id = ua.user_id
    WHERE t.amount > 3 * ua.avg_amount
)
SELECT DISTINCT
    t.transaction_id,
    t.user_id,
    t.amount,
    t.city,
    t.transaction_time,
    CASE 
        WHEN rt.transaction_id IS NOT NULL THEN 'Rapid transactions'
        WHEN it.transaction_id IS NOT NULL THEN 'Impossible travel'
        WHEN ha.transaction_id IS NOT NULL THEN 'Unusually high amount'
    END AS fraud_reason
FROM transactions t
LEFT JOIN rapid_transactions rt ON t.transaction_id = rt.transaction_id
LEFT JOIN impossible_travel it ON t.transaction_id = it.transaction_id
LEFT JOIN high_amount ha ON t.transaction_id = ha.transaction_id
WHERE rt.transaction_id IS NOT NULL 
   OR it.transaction_id IS NOT NULL 
   OR ha.transaction_id IS NOT NULL
ORDER BY t.transaction_time;
```

## Output
```
transaction_id | user_id | amount  | city | transaction_time    | fraud_reason
---------------|---------|---------|------|---------------------|------------------
3              | 101     | 200.00  | LA   | 2024-01-01 10:30:00 | Impossible travel
5              | 101     | 5000.00 | NYC  | 2024-01-01 12:00:00 | Unusually high amount
```

## Optimized Version with Window Functions

```sql
WITH transaction_analysis AS (
    SELECT 
        transaction_id,
        user_id,
        amount,
        city,
        transaction_time,
        LAG(city) OVER (PARTITION BY user_id ORDER BY transaction_time) AS prev_city,
        LAG(transaction_time) OVER (PARTITION BY user_id ORDER BY transaction_time) AS prev_time,
        AVG(amount) OVER (PARTITION BY user_id) AS user_avg_amount,
        COUNT(*) OVER (
            PARTITION BY card_number 
            ORDER BY transaction_time 
            RANGE BETWEEN INTERVAL '10 minutes' PRECEDING AND CURRENT ROW
        ) AS recent_txn_count
    FROM transactions
)
SELECT 
    transaction_id,
    user_id,
    amount,
    city,
    transaction_time,
    ARRAY_AGG(DISTINCT fraud_type) AS fraud_reasons
FROM (
    SELECT *,
        CASE 
            WHEN recent_txn_count >= 3 THEN 'Rapid transactions'
            WHEN prev_city IS NOT NULL 
                 AND city != prev_city 
                 AND EXTRACT(EPOCH FROM (transaction_time - prev_time))/3600 < 1 
            THEN 'Impossible travel'
            WHEN amount > 3 * user_avg_amount THEN 'High amount'
        END AS fraud_type
    FROM transaction_analysis
) flagged
WHERE fraud_type IS NOT NULL
GROUP BY transaction_id, user_id, amount, city, transaction_time;
```

## Follow-ups

**Q: Add fraud score (0-100)?**
```sql
SELECT 
    transaction_id,
    (CASE WHEN rapid_flag THEN 30 ELSE 0 END +
     CASE WHEN travel_flag THEN 40 ELSE 0 END +
     CASE WHEN amount_flag THEN 30 ELSE 0 END) AS fraud_score
FROM fraud_analysis;
```

## Complexity
- Time: O(n²) for self-joins, O(n log n) with window functions
- Space: O(n) for intermediate results
