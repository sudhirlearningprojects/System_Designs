# PostgreSQL Complete Guide - Part 3: Advanced Queries & JOINs

## 📋 Table of Contents
1. [JOIN Operations](#join-operations)
2. [Subqueries](#subqueries)
3. [Window Functions](#window-functions)
4. [Common Table Expressions (CTEs)](#common-table-expressions-ctes)
5. [Aggregate Functions](#aggregate-functions)

---

## JOIN Operations

### INNER JOIN
```sql
-- Returns only matching rows from both tables
SELECT u.name, o.id as order_id, o.total_amount
FROM users u
INNER JOIN orders o ON u.id = o.user_id;

-- Multiple JOINs
SELECT u.name, o.id, oi.product_id, oi.quantity
FROM users u
INNER JOIN orders o ON u.id = o.user_id
INNER JOIN order_items oi ON o.id = oi.order_id;
```

### LEFT JOIN (LEFT OUTER JOIN)
```sql
-- Returns all rows from left table + matching rows from right
SELECT u.name, COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name;

-- Find users with no orders
SELECT u.name
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.id IS NULL;
```

### RIGHT JOIN (RIGHT OUTER JOIN)
```sql
-- Returns all rows from right table + matching rows from left
SELECT u.name, o.id as order_id
FROM users u
RIGHT JOIN orders o ON u.id = o.user_id;
```

### FULL OUTER JOIN
```sql
-- Returns all rows from both tables
SELECT u.name, o.id as order_id
FROM users u
FULL OUTER JOIN orders o ON u.id = o.user_id;
```

### CROSS JOIN
```sql
-- Cartesian product (all combinations)
SELECT u.name, p.name
FROM users u
CROSS JOIN products p;
```

### SELF JOIN
```sql
-- Join table to itself
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    manager_id INTEGER REFERENCES employees(id)
);

-- Find employees and their managers
SELECT e.name as employee, m.name as manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

---

## Subqueries

### Scalar Subquery
```sql
-- Returns single value
SELECT name, 
       (SELECT COUNT(*) FROM orders WHERE user_id = u.id) as order_count
FROM users u;
```

### IN Subquery
```sql
-- Find users who placed orders
SELECT * FROM users
WHERE id IN (SELECT DISTINCT user_id FROM orders);

-- Find products never ordered
SELECT * FROM products
WHERE id NOT IN (SELECT DISTINCT product_id FROM order_items);
```

### EXISTS Subquery
```sql
-- More efficient than IN for large datasets
SELECT * FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- Find users with no orders
SELECT * FROM users u
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
```

### Correlated Subquery
```sql
-- Subquery references outer query
SELECT u.name,
       (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) as order_count
FROM users u;

-- Find users with above-average spending
SELECT u.name, 
       (SELECT SUM(total_amount) FROM orders WHERE user_id = u.id) as total_spent
FROM users u
WHERE (SELECT SUM(total_amount) FROM orders WHERE user_id = u.id) > 
      (SELECT AVG(total_amount) FROM orders);
```

### FROM Subquery
```sql
-- Subquery in FROM clause
SELECT avg_price_by_category.category, avg_price_by_category.avg_price
FROM (
    SELECT category, AVG(price) as avg_price
    FROM products
    GROUP BY category
) as avg_price_by_category
WHERE avg_price_by_category.avg_price > 100;
```

---

## Window Functions

### ROW_NUMBER
```sql
-- Assign unique row number
SELECT name, price,
       ROW_NUMBER() OVER (ORDER BY price DESC) as row_num
FROM products;

-- Row number within partition
SELECT category, name, price,
       ROW_NUMBER() OVER (PARTITION BY category ORDER BY price DESC) as rank_in_category
FROM products;
```

### RANK and DENSE_RANK
```sql
-- RANK: Gaps in ranking for ties
SELECT name, price,
       RANK() OVER (ORDER BY price DESC) as rank
FROM products;

-- DENSE_RANK: No gaps in ranking
SELECT name, price,
       DENSE_RANK() OVER (ORDER BY price DESC) as dense_rank
FROM products;
```

### LAG and LEAD
```sql
-- Access previous row value
SELECT created_at, total_amount,
       LAG(total_amount) OVER (ORDER BY created_at) as prev_amount,
       total_amount - LAG(total_amount) OVER (ORDER BY created_at) as diff
FROM orders;

-- Access next row value
SELECT created_at, total_amount,
       LEAD(total_amount) OVER (ORDER BY created_at) as next_amount
FROM orders;
```

### FIRST_VALUE and LAST_VALUE
```sql
-- First value in window
SELECT category, name, price,
       FIRST_VALUE(name) OVER (PARTITION BY category ORDER BY price DESC) as most_expensive
FROM products;

-- Last value in window
SELECT category, name, price,
       LAST_VALUE(name) OVER (
           PARTITION BY category 
           ORDER BY price DESC
           ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
       ) as least_expensive
FROM products;
```

### Running Totals
```sql
-- Cumulative sum
SELECT created_at, total_amount,
       SUM(total_amount) OVER (ORDER BY created_at) as running_total
FROM orders;

-- Moving average (last 7 days)
SELECT created_at, total_amount,
       AVG(total_amount) OVER (
           ORDER BY created_at
           ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       ) as moving_avg_7days
FROM orders;
```

---

## Common Table Expressions (CTEs)

### Basic CTE
```sql
-- Named temporary result set
WITH high_value_orders AS (
    SELECT * FROM orders WHERE total_amount > 1000
)
SELECT u.name, hvo.total_amount
FROM users u
JOIN high_value_orders hvo ON u.id = hvo.user_id;
```

### Multiple CTEs
```sql
WITH 
active_users AS (
    SELECT * FROM users WHERE status = 'active'
),
recent_orders AS (
    SELECT * FROM orders WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
)
SELECT au.name, COUNT(ro.id) as order_count
FROM active_users au
LEFT JOIN recent_orders ro ON au.id = ro.user_id
GROUP BY au.id, au.name;
```

### Recursive CTE
```sql
-- Hierarchical data (employee hierarchy)
WITH RECURSIVE employee_hierarchy AS (
    -- Base case: Top-level employees
    SELECT id, name, manager_id, 1 as level
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    -- Recursive case: Employees with managers
    SELECT e.id, e.name, e.manager_id, eh.level + 1
    FROM employees e
    JOIN employee_hierarchy eh ON e.manager_id = eh.id
)
SELECT * FROM employee_hierarchy ORDER BY level, name;

-- Category tree
WITH RECURSIVE category_tree AS (
    SELECT id, name, parent_id, 1 as depth
    FROM categories
    WHERE parent_id IS NULL
    
    UNION ALL
    
    SELECT c.id, c.name, c.parent_id, ct.depth + 1
    FROM categories c
    JOIN category_tree ct ON c.parent_id = ct.id
)
SELECT * FROM category_tree;
```

---

## Aggregate Functions

### Basic Aggregates
```sql
-- COUNT, SUM, AVG, MIN, MAX
SELECT 
    COUNT(*) as total_orders,
    SUM(total_amount) as total_revenue,
    AVG(total_amount) as avg_order_value,
    MIN(total_amount) as min_order,
    MAX(total_amount) as max_order
FROM orders;
```

### GROUP BY
```sql
-- Group by single column
SELECT status, COUNT(*) as count
FROM orders
GROUP BY status;

-- Group by multiple columns
SELECT user_id, status, COUNT(*) as count
FROM orders
GROUP BY user_id, status;

-- With aggregate functions
SELECT category, 
       COUNT(*) as product_count,
       AVG(price) as avg_price,
       MIN(price) as min_price,
       MAX(price) as max_price
FROM products
GROUP BY category;
```

### HAVING
```sql
-- Filter after aggregation
SELECT user_id, COUNT(*) as order_count
FROM orders
GROUP BY user_id
HAVING COUNT(*) > 5;

-- Multiple conditions
SELECT category, AVG(price) as avg_price
FROM products
GROUP BY category
HAVING AVG(price) > 100 AND COUNT(*) > 10;
```

### DISTINCT
```sql
-- Unique values
SELECT DISTINCT category FROM products;

-- Count unique values
SELECT COUNT(DISTINCT user_id) as unique_customers
FROM orders;
```

### String Aggregation
```sql
-- Concatenate strings
SELECT user_id, STRING_AGG(product_name, ', ') as products
FROM order_items
GROUP BY user_id;

-- With ordering
SELECT user_id, 
       STRING_AGG(product_name, ', ' ORDER BY product_name) as products
FROM order_items
GROUP BY user_id;
```

### Array Aggregation
```sql
-- Collect values into array
SELECT user_id, ARRAY_AGG(product_id) as product_ids
FROM order_items
GROUP BY user_id;
```

### JSON Aggregation
```sql
-- Aggregate as JSON
SELECT category,
       JSON_AGG(JSON_BUILD_OBJECT('name', name, 'price', price)) as products
FROM products
GROUP BY category;
```

---

## Real-World Examples

### 1. Customer Analytics
```sql
-- Customer lifetime value and segmentation
WITH customer_stats AS (
    SELECT 
        u.id,
        u.name,
        u.email,
        COUNT(o.id) as total_orders,
        COALESCE(SUM(o.total_amount), 0) as lifetime_value,
        MAX(o.created_at) as last_order_date,
        EXTRACT(DAY FROM CURRENT_DATE - MAX(o.created_at)) as days_since_last_order
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    GROUP BY u.id, u.name, u.email
)
SELECT 
    name,
    email,
    total_orders,
    lifetime_value,
    CASE 
        WHEN lifetime_value >= 10000 AND days_since_last_order <= 30 THEN 'VIP'
        WHEN total_orders >= 10 AND days_since_last_order <= 90 THEN 'Loyal'
        WHEN days_since_last_order > 180 THEN 'Churned'
        ELSE 'Regular'
    END as segment
FROM customer_stats
ORDER BY lifetime_value DESC;
```

### 2. Product Performance Report
```sql
-- Top products by revenue with ranking
WITH product_revenue AS (
    SELECT 
        p.id,
        p.name,
        p.category,
        COUNT(oi.id) as times_ordered,
        SUM(oi.quantity) as total_quantity_sold,
        SUM(oi.quantity * oi.price) as total_revenue
    FROM products p
    LEFT JOIN order_items oi ON p.id = oi.product_id
    GROUP BY p.id, p.name, p.category
)
SELECT 
    name,
    category,
    times_ordered,
    total_quantity_sold,
    total_revenue,
    RANK() OVER (ORDER BY total_revenue DESC) as revenue_rank,
    RANK() OVER (PARTITION BY category ORDER BY total_revenue DESC) as category_rank
FROM product_revenue
WHERE total_revenue > 0
ORDER BY total_revenue DESC
LIMIT 20;
```

### 3. Sales Trend Analysis
```sql
-- Daily sales with moving average
WITH daily_sales AS (
    SELECT 
        DATE(created_at) as sale_date,
        COUNT(*) as order_count,
        SUM(total_amount) as daily_revenue
    FROM orders
    WHERE status = 'completed'
    GROUP BY DATE(created_at)
)
SELECT 
    sale_date,
    order_count,
    daily_revenue,
    AVG(daily_revenue) OVER (
        ORDER BY sale_date
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) as moving_avg_7days,
    daily_revenue - LAG(daily_revenue) OVER (ORDER BY sale_date) as day_over_day_change
FROM daily_sales
ORDER BY sale_date DESC;
```

### 4. Inventory Alert System
```sql
-- Products needing restock
WITH product_sales AS (
    SELECT 
        p.id,
        p.name,
        p.stock,
        COALESCE(SUM(oi.quantity), 0) as sold_last_30_days
    FROM products p
    LEFT JOIN order_items oi ON p.id = oi.product_id
    LEFT JOIN orders o ON oi.order_id = o.id
    WHERE o.created_at > CURRENT_DATE - INTERVAL '30 days'
    GROUP BY p.id, p.name, p.stock
)
SELECT 
    name,
    stock,
    sold_last_30_days,
    CASE 
        WHEN sold_last_30_days > 0 
        THEN ROUND(stock::NUMERIC / (sold_last_30_days / 30.0), 1)
        ELSE NULL
    END as days_until_stockout,
    CASE 
        WHEN stock = 0 THEN 'OUT_OF_STOCK'
        WHEN stock < 10 THEN 'LOW_STOCK'
        WHEN sold_last_30_days > 0 AND stock / (sold_last_30_days / 30.0) < 7 THEN 'RESTOCK_SOON'
        ELSE 'OK'
    END as status
FROM product_sales
WHERE stock < 10 OR (sold_last_30_days > 0 AND stock / (sold_last_30_days / 30.0) < 7)
ORDER BY days_until_stockout NULLS LAST;
```

### 5. User Cohort Analysis
```sql
-- Monthly cohort retention
WITH user_cohorts AS (
    SELECT 
        id as user_id,
        DATE_TRUNC('month', created_at) as cohort_month
    FROM users
),
user_orders AS (
    SELECT 
        user_id,
        DATE_TRUNC('month', created_at) as order_month
    FROM orders
)
SELECT 
    uc.cohort_month,
    COUNT(DISTINCT uc.user_id) as cohort_size,
    COUNT(DISTINCT CASE WHEN uo.order_month = uc.cohort_month THEN uo.user_id END) as month_0,
    COUNT(DISTINCT CASE WHEN uo.order_month = uc.cohort_month + INTERVAL '1 month' THEN uo.user_id END) as month_1,
    COUNT(DISTINCT CASE WHEN uo.order_month = uc.cohort_month + INTERVAL '2 months' THEN uo.user_id END) as month_2,
    COUNT(DISTINCT CASE WHEN uo.order_month = uc.cohort_month + INTERVAL '3 months' THEN uo.user_id END) as month_3
FROM user_cohorts uc
LEFT JOIN user_orders uo ON uc.user_id = uo.user_id
GROUP BY uc.cohort_month
ORDER BY uc.cohort_month DESC;
```

---

## Next Steps

Continue to [Part 4: Transactions & Concurrency](./04_PostgreSQL_Transactions.md)
