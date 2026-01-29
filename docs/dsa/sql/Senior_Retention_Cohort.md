# User Retention Cohort Analysis

**Level**: Senior (5+ years)  
**Companies**: Netflix, Spotify, Airbnb  
**Concepts**: Cohort analysis, Complex date logic, Self joins

## Problem

Calculate monthly retention rate for user cohorts. Show what % of users from each signup month are still active in subsequent months.

## Schema

```sql
CREATE TABLE users (
    user_id INT PRIMARY KEY,
    signup_date DATE
);

CREATE TABLE user_activity (
    user_id INT,
    activity_date DATE
);

INSERT INTO users VALUES
(1, '2024-01-05'),
(2, '2024-01-15'),
(3, '2024-02-10'),
(4, '2024-02-20');

INSERT INTO user_activity VALUES
(1, '2024-01-05'), (1, '2024-02-10'), (1, '2024-03-15'),
(2, '2024-01-15'), (2, '2024-02-20'),
(3, '2024-02-10'), (3, '2024-03-05'),
(4, '2024-02-20');
```

## Solution

```sql
WITH cohorts AS (
    SELECT 
        user_id,
        DATE_TRUNC('month', signup_date) AS cohort_month
    FROM users
),
user_months AS (
    SELECT DISTINCT
        user_id,
        DATE_TRUNC('month', activity_date) AS activity_month
    FROM user_activity
),
cohort_activity AS (
    SELECT 
        c.cohort_month,
        um.activity_month,
        COUNT(DISTINCT c.user_id) AS active_users,
        EXTRACT(MONTH FROM AGE(um.activity_month, c.cohort_month)) AS months_since_signup
    FROM cohorts c
    JOIN user_months um ON c.user_id = um.user_id
    GROUP BY c.cohort_month, um.activity_month
),
cohort_sizes AS (
    SELECT 
        cohort_month,
        COUNT(*) AS cohort_size
    FROM cohorts
    GROUP BY cohort_month
)
SELECT 
    ca.cohort_month,
    ca.months_since_signup,
    ca.active_users,
    cs.cohort_size,
    ROUND(100.0 * ca.active_users / cs.cohort_size, 2) AS retention_rate
FROM cohort_activity ca
JOIN cohort_sizes cs ON ca.cohort_month = cs.cohort_month
ORDER BY ca.cohort_month, ca.months_since_signup;
```

## Output
```
cohort_month | months_since_signup | active_users | cohort_size | retention_rate
-------------|---------------------|--------------|-------------|---------------
2024-01-01   | 0                   | 2            | 2           | 100.00
2024-01-01   | 1                   | 2            | 2           | 100.00
2024-01-01   | 2                   | 1            | 2           | 50.00
2024-02-01   | 0                   | 2            | 2           | 100.00
2024-02-01   | 1                   | 1            | 2           | 50.00
```

## Pivot Format

```sql
SELECT 
    cohort_month,
    MAX(CASE WHEN months_since_signup = 0 THEN retention_rate END) AS month_0,
    MAX(CASE WHEN months_since_signup = 1 THEN retention_rate END) AS month_1,
    MAX(CASE WHEN months_since_signup = 2 THEN retention_rate END) AS month_2
FROM (
    -- Previous query here
) retention_data
GROUP BY cohort_month;
```

## Follow-ups

**Q: Calculate Day 1, Day 7, Day 30 retention?**
```sql
WITH first_activity AS (
    SELECT user_id, MIN(activity_date) AS first_date
    FROM user_activity
    GROUP BY user_id
)
SELECT 
    DATE_TRUNC('day', first_date) AS cohort_date,
    COUNT(DISTINCT CASE WHEN activity_date = first_date + 1 THEN user_id END) * 100.0 / COUNT(DISTINCT user_id) AS day_1_retention,
    COUNT(DISTINCT CASE WHEN activity_date = first_date + 7 THEN user_id END) * 100.0 / COUNT(DISTINCT user_id) AS day_7_retention
FROM first_activity fa
JOIN user_activity ua ON fa.user_id = ua.user_id
GROUP BY cohort_date;
```

## Complexity
- Time: O(n log n) for joins and aggregations
- Space: O(n) for CTEs
