# Find Managers of Employees - MySQL Query

## Problem Statement

Given an employee table with a self-referencing manager_id column, find the manager of each employee.

**Table Structure:**
```sql
employees
├── employee_id (INT, PRIMARY KEY)
├── employee_name (VARCHAR)
├── manager_id (INT, FOREIGN KEY references employee_id)
└── department (VARCHAR)
```

---

## Solution: Self-Join

### Basic Query

```sql
SELECT 
    e.employee_id,
    e.employee_name AS employee,
    m.employee_name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id;
```

---

## Sample Data

```sql
CREATE TABLE employees (
    employee_id INT PRIMARY KEY,
    employee_name VARCHAR(100),
    manager_id INT,
    department VARCHAR(50)
);

INSERT INTO employees VALUES
(1, 'Alice', NULL, 'Executive'),      -- CEO, no manager
(2, 'Bob', 1, 'Engineering'),         -- Reports to Alice
(3, 'Charlie', 1, 'Sales'),           -- Reports to Alice
(4, 'David', 2, 'Engineering'),       -- Reports to Bob
(5, 'Eve', 2, 'Engineering'),         -- Reports to Bob
(6, 'Frank', 3, 'Sales');             -- Reports to Charlie
```

**Hierarchy:**
```
Alice (CEO)
├── Bob (Engineering Manager)
│   ├── David
│   └── Eve
└── Charlie (Sales Manager)
    └── Frank
```

---

## Query Results

### Basic Query Output

```sql
SELECT 
    e.employee_id,
    e.employee_name AS employee,
    m.employee_name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id;
```

**Result:**
```
+-------------+----------+---------+
| employee_id | employee | manager |
+-------------+----------+---------+
|           1 | Alice    | NULL    |
|           2 | Bob      | Alice   |
|           3 | Charlie  | Alice   |
|           4 | David    | Bob     |
|           5 | Eve      | Bob     |
|           6 | Frank    | Charlie |
+-------------+----------+---------+
```

---

## Query Variations

### 1. Include Department

```sql
SELECT 
    e.employee_id,
    e.employee_name AS employee,
    e.department,
    m.employee_name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id;
```

**Result:**
```
+-------------+----------+-------------+---------+
| employee_id | employee | department  | manager |
+-------------+----------+-------------+---------+
|           1 | Alice    | Executive   | NULL    |
|           2 | Bob      | Engineering | Alice   |
|           3 | Charlie  | Sales       | Alice   |
|           4 | David    | Engineering | Bob     |
|           5 | Eve      | Engineering | Bob     |
|           6 | Frank    | Sales       | Charlie |
+-------------+----------+-------------+---------+
```

---

### 2. Exclude Employees Without Managers (CEO)

```sql
SELECT 
    e.employee_id,
    e.employee_name AS employee,
    m.employee_name AS manager
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id;
```

**Result:**
```
+-------------+----------+---------+
| employee_id | employee | manager |
+-------------+----------+---------+
|           2 | Bob      | Alice   |
|           3 | Charlie  | Alice   |
|           4 | David    | Bob     |
|           5 | Eve      | Bob     |
|           6 | Frank    | Charlie |
+-------------+----------+---------+
```

---

### 3. Show Manager's Department

```sql
SELECT 
    e.employee_id,
    e.employee_name AS employee,
    e.department AS employee_dept,
    m.employee_name AS manager,
    m.department AS manager_dept
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id;
```

---

### 4. Count Direct Reports per Manager

```sql
SELECT 
    m.employee_name AS manager,
    COUNT(e.employee_id) AS direct_reports
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id
GROUP BY m.employee_id, m.employee_name
ORDER BY direct_reports DESC;
```

**Result:**
```
+---------+----------------+
| manager | direct_reports |
+---------+----------------+
| Alice   |              2 |
| Bob     |              2 |
| Charlie |              1 |
+---------+----------------+
```

---

### 5. Find Employees and Their Manager's Manager

```sql
SELECT 
    e.employee_name AS employee,
    m1.employee_name AS manager,
    m2.employee_name AS manager_of_manager
FROM employees e
LEFT JOIN employees m1 ON e.manager_id = m1.employee_id
LEFT JOIN employees m2 ON m1.manager_id = m2.employee_id;
```

**Result:**
```
+----------+---------+--------------------+
| employee | manager | manager_of_manager |
+----------+---------+--------------------+
| Alice    | NULL    | NULL               |
| Bob      | Alice   | NULL               |
| Charlie  | Alice   | NULL               |
| David    | Bob     | Alice              |
| Eve      | Bob     | Alice              |
| Frank    | Charlie | Alice              |
+----------+---------+--------------------+
```

---

### 6. Hierarchical Path (Recursive CTE)

```sql
WITH RECURSIVE hierarchy AS (
    -- Base case: employees with no manager
    SELECT 
        employee_id,
        employee_name,
        manager_id,
        employee_name AS path,
        0 AS level
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    -- Recursive case: employees with managers
    SELECT 
        e.employee_id,
        e.employee_name,
        e.manager_id,
        CONCAT(h.path, ' -> ', e.employee_name) AS path,
        h.level + 1 AS level
    FROM employees e
    INNER JOIN hierarchy h ON e.manager_id = h.employee_id
)
SELECT 
    employee_id,
    employee_name,
    level,
    path
FROM hierarchy
ORDER BY level, employee_name;
```

**Result:**
```
+-------------+--------------+-------+---------------------------+
| employee_id | employee_name| level | path                      |
+-------------+--------------+-------+---------------------------+
|           1 | Alice        |     0 | Alice                     |
|           2 | Bob          |     1 | Alice -> Bob              |
|           3 | Charlie      |     1 | Alice -> Charlie          |
|           4 | David        |     2 | Alice -> Bob -> David     |
|           5 | Eve          |     2 | Alice -> Bob -> Eve       |
|           6 | Frank        |     2 | Alice -> Charlie -> Frank |
+-------------+--------------+-------+---------------------------+
```

---

### 7. Find All Subordinates of a Manager

```sql
WITH RECURSIVE subordinates AS (
    -- Base case: the manager
    SELECT employee_id, employee_name, manager_id
    FROM employees
    WHERE employee_id = 2  -- Bob's ID
    
    UNION ALL
    
    -- Recursive case: direct and indirect reports
    SELECT e.employee_id, e.employee_name, e.manager_id
    FROM employees e
    INNER JOIN subordinates s ON e.manager_id = s.employee_id
)
SELECT employee_name
FROM subordinates
WHERE employee_id != 2;  -- Exclude the manager himself
```

**Result (Bob's subordinates):**
```
+--------------+
| employee_name|
+--------------+
| David        |
| Eve          |
+--------------+
```

---

### 8. Employees Without Managers (Top-Level)

```sql
SELECT 
    employee_id,
    employee_name,
    department
FROM employees
WHERE manager_id IS NULL;
```

**Result:**
```
+-------------+--------------+-----------+
| employee_id | employee_name| department|
+-------------+--------------+-----------+
|           1 | Alice        | Executive |
+-------------+--------------+-----------+
```

---

### 9. Manager Chain (Up to 3 Levels)

```sql
SELECT 
    e.employee_name AS employee,
    m1.employee_name AS level_1_manager,
    m2.employee_name AS level_2_manager,
    m3.employee_name AS level_3_manager
FROM employees e
LEFT JOIN employees m1 ON e.manager_id = m1.employee_id
LEFT JOIN employees m2 ON m1.manager_id = m2.employee_id
LEFT JOIN employees m3 ON m2.manager_id = m3.employee_id;
```

---

### 10. Employees with Same Manager (Peers)

```sql
SELECT 
    e1.employee_name AS employee,
    e2.employee_name AS peer,
    m.employee_name AS common_manager
FROM employees e1
INNER JOIN employees e2 ON e1.manager_id = e2.manager_id 
    AND e1.employee_id < e2.employee_id
INNER JOIN employees m ON e1.manager_id = m.employee_id;
```

**Result:**
```
+----------+----------+----------------+
| employee | peer     | common_manager |
+----------+----------+----------------+
| Bob      | Charlie  | Alice          |
| David    | Eve      | Bob            |
+----------+----------+----------------+
```

---

## Key Concepts

### Self-Join Explanation

```sql
FROM employees e          -- Alias 'e' for employee
LEFT JOIN employees m     -- Alias 'm' for manager
ON e.manager_id = m.employee_id
```

**Why LEFT JOIN?**
- Includes employees without managers (CEO, top-level)
- INNER JOIN would exclude them

**Table Aliases:**
- `e` = employee table
- `m` = manager table (same physical table)

---

## Common Patterns

### Pattern 1: Direct Manager
```sql
LEFT JOIN employees m ON e.manager_id = m.employee_id
```

### Pattern 2: Manager's Manager
```sql
LEFT JOIN employees m1 ON e.manager_id = m1.employee_id
LEFT JOIN employees m2 ON m1.manager_id = m2.employee_id
```

### Pattern 3: Recursive Hierarchy
```sql
WITH RECURSIVE cte AS (
    SELECT ... WHERE manager_id IS NULL  -- Base
    UNION ALL
    SELECT ... JOIN cte ...              -- Recursive
)
```

---

## Performance Considerations

### Index Recommendations

```sql
-- Primary key (already indexed)
CREATE INDEX idx_employee_id ON employees(employee_id);

-- Foreign key for manager lookup
CREATE INDEX idx_manager_id ON employees(manager_id);

-- Composite index for common queries
CREATE INDEX idx_manager_dept ON employees(manager_id, department);
```

### Query Optimization

```sql
-- Use EXPLAIN to analyze
EXPLAIN SELECT 
    e.employee_name,
    m.employee_name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id;
```

---

## Edge Cases

### 1. Circular Reference (Data Issue)
```sql
-- Detect circular references
SELECT e1.employee_id, e1.employee_name
FROM employees e1
JOIN employees e2 ON e1.manager_id = e2.employee_id
WHERE e2.manager_id = e1.employee_id;
```

### 2. Orphaned Employees
```sql
-- Find employees with invalid manager_id
SELECT e.employee_id, e.employee_name, e.manager_id
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id
WHERE e.manager_id IS NOT NULL AND m.employee_id IS NULL;
```

---

## Complete Example Script

```sql
-- Create table
CREATE TABLE employees (
    employee_id INT PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    manager_id INT,
    department VARCHAR(50),
    salary DECIMAL(10, 2),
    FOREIGN KEY (manager_id) REFERENCES employees(employee_id)
);

-- Insert sample data
INSERT INTO employees VALUES
(1, 'Alice', NULL, 'Executive', 150000),
(2, 'Bob', 1, 'Engineering', 120000),
(3, 'Charlie', 1, 'Sales', 110000),
(4, 'David', 2, 'Engineering', 90000),
(5, 'Eve', 2, 'Engineering', 95000),
(6, 'Frank', 3, 'Sales', 85000);

-- Basic query: Find managers
SELECT 
    e.employee_id,
    e.employee_name AS employee,
    e.department,
    e.salary,
    m.employee_name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id
ORDER BY e.employee_id;

-- Count reports per manager
SELECT 
    m.employee_name AS manager,
    COUNT(e.employee_id) AS team_size,
    AVG(e.salary) AS avg_team_salary
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id
GROUP BY m.employee_id, m.employee_name;

-- Full hierarchy with recursive CTE
WITH RECURSIVE org_chart AS (
    SELECT 
        employee_id,
        employee_name,
        manager_id,
        0 AS level,
        CAST(employee_name AS CHAR(200)) AS path
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    SELECT 
        e.employee_id,
        e.employee_name,
        e.manager_id,
        oc.level + 1,
        CONCAT(oc.path, ' -> ', e.employee_name)
    FROM employees e
    INNER JOIN org_chart oc ON e.manager_id = oc.employee_id
)
SELECT * FROM org_chart ORDER BY level, employee_name;
```

---

## Related SQL Patterns

- **Hierarchical Queries:** Organization charts, category trees
- **Graph Traversal:** Social networks, dependencies
- **Recursive CTEs:** Bill of materials, file systems
- **Self-Joins:** Finding duplicates, comparing rows

---

## Interview Questions & Answers

### Question 1: Basic Manager Query (Easy)

**Q:** Write a query to find each employee and their direct manager.

**A:**
```sql
SELECT 
    e.employee_name AS employee,
    m.employee_name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id;
```

**Follow-up:** Why LEFT JOIN instead of INNER JOIN?

**A:** LEFT JOIN includes employees without managers (CEO/top-level). INNER JOIN would exclude them.

---

### Question 2: Count Direct Reports (Medium)

**Q:** Find how many direct reports each manager has.

**A:**
```sql
SELECT 
    m.employee_name AS manager,
    COUNT(e.employee_id) AS direct_reports
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id
GROUP BY m.employee_id, m.employee_name
ORDER BY direct_reports DESC;
```

---

### Question 3: Find Manager's Manager (Medium)

**Q:** Show each employee, their manager, and their manager's manager.

**A:**
```sql
SELECT 
    e.employee_name AS employee,
    m1.employee_name AS manager,
    m2.employee_name AS manager_of_manager
FROM employees e
LEFT JOIN employees m1 ON e.manager_id = m1.employee_id
LEFT JOIN employees m2 ON m1.manager_id = m2.employee_id;
```

---

### Question 4: Employees Without Managers (Easy)

**Q:** Find all top-level employees (those without managers).

**A:**
```sql
SELECT employee_name
FROM employees
WHERE manager_id IS NULL;
```

---

### Question 5: Full Organizational Hierarchy (Hard)

**Q:** Display the complete organizational hierarchy with levels and paths.

**A:**
```sql
WITH RECURSIVE hierarchy AS (
    SELECT 
        employee_id,
        employee_name,
        manager_id,
        employee_name AS path,
        0 AS level
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    SELECT 
        e.employee_id,
        e.employee_name,
        e.manager_id,
        CONCAT(h.path, ' -> ', e.employee_name),
        h.level + 1
    FROM employees e
    INNER JOIN hierarchy h ON e.manager_id = h.employee_id
)
SELECT * FROM hierarchy ORDER BY level, employee_name;
```

---

### Question 6: Find All Subordinates (Hard)

**Q:** Find all direct and indirect subordinates of a given manager (e.g., employee_id = 2).

**A:**
```sql
WITH RECURSIVE subordinates AS (
    SELECT employee_id, employee_name
    FROM employees
    WHERE employee_id = 2
    
    UNION ALL
    
    SELECT e.employee_id, e.employee_name
    FROM employees e
    INNER JOIN subordinates s ON e.manager_id = s.employee_id
)
SELECT employee_name
FROM subordinates
WHERE employee_id != 2;
```

---

### Question 7: Employees with Same Manager (Medium)

**Q:** Find pairs of employees who report to the same manager (peers).

**A:**
```sql
SELECT 
    e1.employee_name AS employee1,
    e2.employee_name AS employee2,
    m.employee_name AS common_manager
FROM employees e1
INNER JOIN employees e2 
    ON e1.manager_id = e2.manager_id 
    AND e1.employee_id < e2.employee_id
INNER JOIN employees m ON e1.manager_id = m.employee_id;
```

---

### Question 8: Salary Comparison with Manager (Medium)

**Q:** Find employees who earn more than their manager.

**A:**
```sql
SELECT 
    e.employee_name AS employee,
    e.salary AS employee_salary,
    m.employee_name AS manager,
    m.salary AS manager_salary
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id
WHERE e.salary > m.salary;
```

---

### Question 9: Detect Circular References (Hard)

**Q:** Find any circular manager relationships (data integrity issue).

**A:**
```sql
SELECT 
    e1.employee_id,
    e1.employee_name,
    e1.manager_id
FROM employees e1
INNER JOIN employees e2 ON e1.manager_id = e2.employee_id
WHERE e2.manager_id = e1.employee_id;
```

---

### Question 10: Orphaned Employees (Medium)

**Q:** Find employees whose manager_id doesn't exist in the table.

**A:**
```sql
SELECT 
    e.employee_id,
    e.employee_name,
    e.manager_id
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.employee_id
WHERE e.manager_id IS NOT NULL 
  AND m.employee_id IS NULL;
```

---

### Question 11: Average Team Salary (Medium)

**Q:** For each manager, show the average salary of their direct reports.

**A:**
```sql
SELECT 
    m.employee_name AS manager,
    COUNT(e.employee_id) AS team_size,
    AVG(e.salary) AS avg_team_salary,
    MIN(e.salary) AS min_salary,
    MAX(e.salary) AS max_salary
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id
GROUP BY m.employee_id, m.employee_name;
```

---

### Question 12: Hierarchy Level Count (Medium)

**Q:** Count how many employees are at each level of the organization.

**A:**
```sql
WITH RECURSIVE levels AS (
    SELECT employee_id, 0 AS level
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    SELECT e.employee_id, l.level + 1
    FROM employees e
    INNER JOIN levels l ON e.manager_id = l.employee_id
)
SELECT 
    level,
    COUNT(*) AS employee_count
FROM levels
GROUP BY level
ORDER BY level;
```

---

### Question 13: Manager Chain (Medium)

**Q:** Show the complete management chain for a specific employee (e.g., employee_id = 6).

**A:**
```sql
WITH RECURSIVE chain AS (
    SELECT 
        employee_id,
        employee_name,
        manager_id,
        employee_name AS chain
    FROM employees
    WHERE employee_id = 6
    
    UNION ALL
    
    SELECT 
        m.employee_id,
        m.employee_name,
        m.manager_id,
        CONCAT(m.employee_name, ' -> ', c.chain)
    FROM employees m
    INNER JOIN chain c ON c.manager_id = m.employee_id
)
SELECT chain
FROM chain
WHERE manager_id IS NULL;
```

---

### Question 14: Span of Control (Medium)

**Q:** Find managers with more than N direct reports (e.g., N = 3).

**A:**
```sql
SELECT 
    m.employee_name AS manager,
    COUNT(e.employee_id) AS direct_reports
FROM employees e
INNER JOIN employees m ON e.manager_id = m.employee_id
GROUP BY m.employee_id, m.employee_name
HAVING COUNT(e.employee_id) > 3;
```

---

### Question 15: Department Manager Hierarchy (Hard)

**Q:** Show each department with its manager and the manager's manager.

**A:**
```sql
SELECT DISTINCT
    e.department,
    m1.employee_name AS dept_manager,
    m2.employee_name AS senior_manager
FROM employees e
INNER JOIN employees m1 
    ON e.manager_id = m1.employee_id
LEFT JOIN employees m2 
    ON m1.manager_id = m2.employee_id
WHERE e.department IS NOT NULL
ORDER BY e.department;
```

---

## Interview Tips

### Common Follow-up Questions

1. **"Why use LEFT JOIN vs INNER JOIN?"**
   - LEFT JOIN: Includes employees without managers
   - INNER JOIN: Excludes top-level employees

2. **"How would you optimize this query?"**
   - Add index on manager_id
   - Use EXPLAIN to analyze query plan
   - Consider materialized views for complex hierarchies

3. **"What if there are millions of employees?"**
   - Partition table by department
   - Use covering indexes
   - Cache frequently accessed hierarchies
   - Consider NoSQL for deep hierarchies

4. **"How to prevent circular references?"**
   - Add CHECK constraint
   - Use triggers to validate
   - Application-level validation

5. **"What's the maximum hierarchy depth?"**
   - MySQL recursive CTE default: 1000 levels
   - Set with: SET SESSION cte_max_recursion_depth = 10000;

---

## Key Takeaways

✅ Use **LEFT JOIN** for self-join to include top-level employees  
✅ Alias tables as `e` (employee) and `m` (manager)  
✅ Join condition: `e.manager_id = m.employee_id`  
✅ Use **INNER JOIN** to exclude employees without managers  
✅ **Recursive CTE** for full hierarchy traversal  
✅ Index `manager_id` for performance  
✅ Handle NULL managers (CEO/top-level)  
✅ Check for circular references and orphaned records
