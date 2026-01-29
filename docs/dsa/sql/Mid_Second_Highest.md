# Second Highest Salary

**Level**: Mid (2-5 years)  
**Companies**: Google, Meta, Amazon  
**Concepts**: Subqueries, LIMIT/OFFSET, DISTINCT

## Problem

Find the second highest salary from employees table. Return NULL if it doesn't exist.

## Schema

```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    salary DECIMAL(10, 2)
);

INSERT INTO employees VALUES
(1, 'Alice', 100000),
(2, 'Bob', 80000),
(3, 'Charlie', 90000),
(4, 'Diana', 80000);
```

## Solutions

### Solution 1: LIMIT OFFSET
```sql
SELECT DISTINCT salary AS SecondHighestSalary
FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;
```

### Solution 2: Subquery
```sql
SELECT MAX(salary) AS SecondHighestSalary
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);
```

### Solution 3: Window Function
```sql
SELECT DISTINCT salary AS SecondHighestSalary
FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM employees
) ranked
WHERE rnk = 2;
```

### Solution 4: Handle NULL Case
```sql
SELECT (
    SELECT DISTINCT salary
    FROM employees
    ORDER BY salary DESC
    LIMIT 1 OFFSET 1
) AS SecondHighestSalary;
```

## Follow-ups

**Q: Find Nth highest salary?**
```sql
-- N = 3
SELECT DISTINCT salary
FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 2;
```

**Q: Second highest per department?**
```sql
SELECT department, salary
FROM (
    SELECT department, salary,
           DENSE_RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS rnk
    FROM employees
) ranked
WHERE rnk = 2;
```

## Complexity
- Time: O(n log n) for sorting
- Space: O(n) for distinct values
