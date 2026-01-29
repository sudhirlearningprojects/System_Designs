# Employee Salary Report - Basic SQL

**Level**: Junior (0-2 years)  
**Companies**: Amazon, Google, Microsoft  
**Concepts**: SELECT, WHERE, ORDER BY, Basic filtering

## Problem Statement

Write a query to find all employees earning more than $50,000, ordered by salary in descending order. Include employee name, department, and salary.

**Real-World Context**: HR departments use this for salary analysis and budget planning.

## Schema

```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    department VARCHAR(50),
    salary DECIMAL(10, 2),
    hire_date DATE
);

INSERT INTO employees VALUES
(1, 'Alice Johnson', 'Engineering', 75000.00, '2020-01-15'),
(2, 'Bob Smith', 'Sales', 45000.00, '2019-03-20'),
(3, 'Charlie Brown', 'Engineering', 85000.00, '2018-06-10'),
(4, 'Diana Prince', 'Marketing', 55000.00, '2021-02-01'),
(5, 'Eve Davis', 'Sales', 48000.00, '2020-11-05'),
(6, 'Frank Miller', 'Engineering', 95000.00, '2017-09-12'),
(7, 'Grace Lee', 'HR', 52000.00, '2019-07-18');
```

## Expected Output

```
name            | department   | salary
----------------|--------------|----------
Frank Miller    | Engineering  | 95000.00
Charlie Brown   | Engineering  | 85000.00
Alice Johnson   | Engineering  | 75000.00
Diana Prince    | Marketing    | 55000.00
Grace Lee       | HR           | 52000.00
```

---

## Solutions

### Solution 1: Basic Query (Optimal)

```sql
SELECT 
    name,
    department,
    salary
FROM employees
WHERE salary > 50000
ORDER BY salary DESC;
```

**Explanation**:
- `WHERE salary > 50000`: Filters employees earning more than $50K
- `ORDER BY salary DESC`: Sorts by salary highest to lowest

**Time Complexity**: O(n log n) for sorting  
**Space Complexity**: O(n) for result set

---

### Solution 2: With Formatting

```sql
SELECT 
    name,
    department,
    CONCAT('$', FORMAT(salary, 2)) AS formatted_salary
FROM employees
WHERE salary > 50000
ORDER BY salary DESC;
```

**Output**:
```
name            | department   | formatted_salary
----------------|--------------|------------------
Frank Miller    | Engineering  | $95,000.00
Charlie Brown   | Engineering  | $85,000.00
...
```

---

### Solution 3: With Ranking

```sql
SELECT 
    name,
    department,
    salary,
    RANK() OVER (ORDER BY salary DESC) AS salary_rank
FROM employees
WHERE salary > 50000
ORDER BY salary DESC;
```

**Output**:
```
name            | department   | salary    | salary_rank
----------------|--------------|-----------|-------------
Frank Miller    | Engineering  | 95000.00  | 1
Charlie Brown   | Engineering  | 85000.00  | 2
...
```

---

## Interview Follow-ups

### Q1: How would you find employees earning between $50K and $80K?

```sql
SELECT name, department, salary
FROM employees
WHERE salary BETWEEN 50000 AND 80000
ORDER BY salary DESC;
```

### Q2: How to include only Engineering department?

```sql
SELECT name, department, salary
FROM employees
WHERE salary > 50000 
  AND department = 'Engineering'
ORDER BY salary DESC;
```

### Q3: What if we want top 3 highest paid employees?

```sql
SELECT name, department, salary
FROM employees
WHERE salary > 50000
ORDER BY salary DESC
LIMIT 3;
```

### Q4: How to handle NULL salaries?

```sql
SELECT name, department, COALESCE(salary, 0) AS salary
FROM employees
WHERE COALESCE(salary, 0) > 50000
ORDER BY salary DESC;
```

---

## Common Mistakes

### ❌ Mistake 1: Forgetting ORDER BY
```sql
-- Wrong: Results in random order
SELECT name, salary FROM employees WHERE salary > 50000;
```

### ❌ Mistake 2: Using HAVING instead of WHERE
```sql
-- Wrong: HAVING is for aggregated data
SELECT name, salary FROM employees HAVING salary > 50000;
```

### ❌ Mistake 3: Not handling NULL values
```sql
-- Risky: NULL comparisons return NULL (not TRUE/FALSE)
WHERE salary > 50000  -- Excludes NULL salaries
```

---

## Performance Considerations

### Index Strategy
```sql
-- Create index on salary for faster filtering
CREATE INDEX idx_salary ON employees(salary);

-- Composite index for department + salary queries
CREATE INDEX idx_dept_salary ON employees(department, salary);
```

### Execution Plan (PostgreSQL)
```sql
EXPLAIN ANALYZE
SELECT name, department, salary
FROM employees
WHERE salary > 50000
ORDER BY salary DESC;
```

**Expected Plan**:
```
Sort  (cost=X..Y rows=N)
  Sort Key: salary DESC
  ->  Seq Scan on employees  (cost=X..Y rows=N)
        Filter: (salary > 50000)
```

---

## Variations

### Variation 1: Multiple Conditions
```sql
-- Employees earning > $50K hired after 2019
SELECT name, department, salary, hire_date
FROM employees
WHERE salary > 50000 
  AND hire_date > '2019-01-01'
ORDER BY salary DESC;
```

### Variation 2: Pattern Matching
```sql
-- Engineering employees earning > $50K
SELECT name, department, salary
FROM employees
WHERE salary > 50000 
  AND department LIKE '%Engineering%'
ORDER BY salary DESC;
```

### Variation 3: Case-Insensitive Search
```sql
-- Case-insensitive department search
SELECT name, department, salary
FROM employees
WHERE salary > 50000 
  AND LOWER(department) = 'engineering'
ORDER BY salary DESC;
```

---

## Production Best Practices

### 1. Parameterized Queries (Prevent SQL Injection)
```java
// Java example
String sql = "SELECT name, department, salary FROM employees " +
             "WHERE salary > ? ORDER BY salary DESC";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setDouble(1, 50000);
```

### 2. Pagination for Large Results
```sql
-- Page 1 (first 10 results)
SELECT name, department, salary
FROM employees
WHERE salary > 50000
ORDER BY salary DESC
LIMIT 10 OFFSET 0;

-- Page 2 (next 10 results)
LIMIT 10 OFFSET 10;
```

### 3. Add Filters for Soft Deletes
```sql
SELECT name, department, salary
FROM employees
WHERE salary > 50000 
  AND deleted_at IS NULL  -- Exclude soft-deleted records
ORDER BY salary DESC;
```

---

## Related Problems

1. **Find Average Salary by Department** (GROUP BY)
2. **Employees Hired in Last Year** (Date functions)
3. **Salary Percentile Calculation** (Window functions)
4. **Department with Highest Average Salary** (Subqueries)

---

## Key Takeaways

✅ Use `WHERE` for row-level filtering  
✅ Use `ORDER BY` for sorting results  
✅ Consider NULL values in comparisons  
✅ Add indexes on frequently filtered columns  
✅ Use `LIMIT` for pagination  
✅ Always use parameterized queries in production
