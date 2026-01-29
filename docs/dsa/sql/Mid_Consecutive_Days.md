# Consecutive Login Days

**Level**: Mid (2-5 years)  
**Companies**: Meta, LinkedIn, Uber  
**Concepts**: Window functions, LAG/LEAD, Date arithmetic

## Problem

Find users who logged in for 3 or more consecutive days.

## Schema

```sql
CREATE TABLE user_logins (
    user_id INT,
    login_date DATE
);

INSERT INTO user_logins VALUES
(1, '2024-01-01'),
(1, '2024-01-02'),
(1, '2024-01-03'),
(1, '2024-01-05'),
(2, '2024-01-01'),
(2, '2024-01-03'),
(3, '2024-01-01'),
(3, '2024-01-02'),
(3, '2024-01-03'),
(3, '2024-01-04');
```

## Solution 1: LAG Window Function

```sql
WITH login_gaps AS (
    SELECT 
        user_id,
        login_date,
        LAG(login_date) OVER (PARTITION BY user_id ORDER BY login_date) AS prev_date,
        login_date - LAG(login_date) OVER (PARTITION BY user_id ORDER BY login_date) AS day_diff
    FROM user_logins
),
consecutive_groups AS (
    SELECT 
        user_id,
        login_date,
        SUM(CASE WHEN day_diff = 1 THEN 0 ELSE 1 END) 
            OVER (PARTITION BY user_id ORDER BY login_date) AS grp
    FROM login_gaps
),
streak_counts AS (
    SELECT 
        user_id,
        grp,
        COUNT(*) AS consecutive_days
    FROM consecutive_groups
    GROUP BY user_id, grp
)
SELECT DISTINCT user_id
FROM streak_counts
WHERE consecutive_days >= 3;
```

## Solution 2: Self Join

```sql
SELECT DISTINCT l1.user_id
FROM user_logins l1
JOIN user_logins l2 ON l1.user_id = l2.user_id 
    AND l2.login_date = l1.login_date + INTERVAL '1 day'
JOIN user_logins l3 ON l1.user_id = l3.user_id 
    AND l3.login_date = l1.login_date + INTERVAL '2 days';
```

## Solution 3: Row Number Trick

```sql
WITH numbered AS (
    SELECT 
        user_id,
        login_date,
        ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS rn,
        login_date - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS grp
    FROM (SELECT DISTINCT user_id, login_date FROM user_logins) t
)
SELECT user_id
FROM numbered
GROUP BY user_id, grp
HAVING COUNT(*) >= 3;
```

## Output
```
user_id
-------
1
3
```

## Follow-ups

**Q: Find longest streak per user?**
```sql
WITH numbered AS (
    SELECT user_id, login_date,
           login_date - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS grp
    FROM (SELECT DISTINCT user_id, login_date FROM user_logins) t
)
SELECT user_id, MAX(streak) AS longest_streak
FROM (
    SELECT user_id, grp, COUNT(*) AS streak
    FROM numbered
    GROUP BY user_id, grp
) streaks
GROUP BY user_id;
```

## Complexity
- Time: O(n log n) for window functions
- Space: O(n) for intermediate results
