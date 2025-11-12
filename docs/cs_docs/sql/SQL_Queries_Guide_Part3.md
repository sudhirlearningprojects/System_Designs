# SQL Queries Guide - Part 3

*Advanced SQL topics, optimization techniques, and comprehensive MCQ practice*

## Table of Contents (Part 3)
11. [Advanced SQL Functions](#advanced-functions)
12. [Query Optimization](#query-optimization)
13. [Stored Procedures and Functions](#stored-procedures)
14. [Triggers and Events](#triggers-events)
15. [Performance Tuning](#performance-tuning)
16. [MCQ Practice Questions](#mcq-questions)

---

## 11. Advanced SQL Functions {#advanced-functions}

### Window Functions

**ROW_NUMBER()**:
```sql
-- Assign unique row numbers
SELECT 
    FirstName,
    LastName,
    Salary,
    ROW_NUMBER() OVER (ORDER BY Salary DESC) AS SalaryRank
FROM Employee;

-- Row numbers within partitions
SELECT 
    FirstName,
    LastName,
    DeptID,
    Salary,
    ROW_NUMBER() OVER (PARTITION BY DeptID ORDER BY Salary DESC) AS DeptRank
FROM Employee;
```

**RANK() and DENSE_RANK()**:
```sql
SELECT 
    FirstName,
    LastName,
    Salary,
    RANK() OVER (ORDER BY Salary DESC) AS Rank,
    DENSE_RANK() OVER (ORDER BY Salary DESC) AS DenseRank
FROM Employee;

-- Results show difference:
-- Salary: 80000, 75000, 75000, 60000
-- Rank:   1,     2,     2,     4
-- Dense:  1,     2,     2,     3
```

**LAG() and LEAD()**:
```sql
-- Compare with previous/next row
SELECT 
    FirstName,
    LastName,
    Salary,
    LAG(Salary, 1) OVER (ORDER BY HireDate) AS PrevSalary,
    LEAD(Salary, 1) OVER (ORDER BY HireDate) AS NextSalary,
    Salary - LAG(Salary, 1) OVER (ORDER BY HireDate) AS SalaryDiff
FROM Employee;
```

**FIRST_VALUE() and LAST_VALUE()**:
```sql
SELECT 
    FirstName,
    LastName,
    DeptID,
    Salary,
    FIRST_VALUE(Salary) OVER (PARTITION BY DeptID ORDER BY Salary DESC) AS HighestInDept,
    LAST_VALUE(Salary) OVER (
        PARTITION BY DeptID 
        ORDER BY Salary DESC 
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS LowestInDept
FROM Employee;
```

**NTILE()**:
```sql
-- Divide employees into salary quartiles
SELECT 
    FirstName,
    LastName,
    Salary,
    NTILE(4) OVER (ORDER BY Salary) AS SalaryQuartile
FROM Employee;
```

### Analytical Functions

**Running Totals**:
```sql
SELECT 
    FirstName,
    LastName,
    Salary,
    SUM(Salary) OVER (ORDER BY EmpID) AS RunningTotal,
    AVG(Salary) OVER (ORDER BY EmpID ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS MovingAvg3
FROM Employee;
```

**Percentage Calculations**:
```sql
SELECT 
    FirstName,
    LastName,
    DeptID,
    Salary,
    SUM(Salary) OVER (PARTITION BY DeptID) AS DeptTotal,
    ROUND(Salary * 100.0 / SUM(Salary) OVER (PARTITION BY DeptID), 2) AS PctOfDeptSalary
FROM Employee;
```

### Date and Time Functions

**Date Arithmetic**:
```sql
SELECT 
    FirstName,
    HireDate,
    CURDATE() AS Today,
    DATEDIFF(CURDATE(), HireDate) AS DaysEmployed,
    TIMESTAMPDIFF(YEAR, HireDate, CURDATE()) AS YearsEmployed,
    DATE_ADD(HireDate, INTERVAL 1 YEAR) AS FirstAnniversary,
    LAST_DAY(HireDate) AS EndOfHireMonth
FROM Employee;
```

**Date Formatting**:
```sql
SELECT 
    FirstName,
    HireDate,
    DATE_FORMAT(HireDate, '%Y-%m-%d') AS FormattedDate,
    DATE_FORMAT(HireDate, '%M %d, %Y') AS LongFormat,
    DAYNAME(HireDate) AS HireDay,
    MONTHNAME(HireDate) AS HireMonth,
    QUARTER(HireDate) AS HireQuarter
FROM Employee;
```

**Time Zones** (MySQL):
```sql
SELECT 
    NOW() AS LocalTime,
    UTC_TIMESTAMP() AS UTCTime,
    CONVERT_TZ(NOW(), 'US/Eastern', 'US/Pacific') AS PacificTime;
```

### String Functions

**Advanced String Manipulation**:
```sql
SELECT 
    FirstName,
    LastName,
    CONCAT(FirstName, ' ', LastName) AS FullName,
    UPPER(LEFT(FirstName, 1)) + LOWER(SUBSTRING(FirstName, 2)) AS ProperCase,
    REVERSE(LastName) AS ReversedLast,
    REPLACE(Email, '@company.com', '@newcompany.com') AS NewEmail,
    SUBSTRING_INDEX(Email, '@', 1) AS Username
FROM Employee;
```

**Pattern Matching**:
```sql
-- Regular expressions (MySQL)
SELECT FirstName, LastName
FROM Employee
WHERE FirstName REGEXP '^[A-C].*n$';  -- Starts with A-C, ends with n

-- Soundex (phonetic matching)
SELECT FirstName, LastName
FROM Employee
WHERE SOUNDEX(FirstName) = SOUNDEX('Jon');  -- Sounds like "Jon"
```

### Conditional Functions

**Advanced CASE Statements**:
```sql
SELECT 
    FirstName,
    LastName,
    Salary,
    CASE 
        WHEN Salary >= 80000 THEN 'Executive'
        WHEN Salary >= 60000 THEN 'Senior'
        WHEN Salary >= 40000 THEN 'Mid-level'
        ELSE 'Entry-level'
    END AS Level,
    CASE 
        WHEN TIMESTAMPDIFF(YEAR, HireDate, CURDATE()) >= 5 THEN 'Veteran'
        WHEN TIMESTAMPDIFF(YEAR, HireDate, CURDATE()) >= 2 THEN 'Experienced'
        ELSE 'New'
    END AS Experience
FROM Employee;
```

**COALESCE and NULLIF**:
```sql
SELECT 
    FirstName,
    LastName,
    COALESCE(Phone, Email, 'No Contact') AS ContactInfo,
    NULLIF(Salary, 0) AS NonZeroSalary,  -- Returns NULL if Salary is 0
    COALESCE(NULLIF(Salary, 0), 50000) AS SalaryWithDefault
FROM Employee;
```

---

## 12. Query Optimization {#query-optimization}

### Understanding Query Execution

**Query Execution Order**:
1. **FROM** - Identify tables
2. **JOIN** - Combine tables
3. **WHERE** - Filter rows
4. **GROUP BY** - Group rows
5. **HAVING** - Filter groups
6. **SELECT** - Choose columns
7. **DISTINCT** - Remove duplicates
8. **ORDER BY** - Sort results
9. **LIMIT** - Restrict rows returned

### EXPLAIN Statement

**Analyze Query Performance**:
```sql
EXPLAIN SELECT e.FirstName, e.LastName, d.DeptName
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID
WHERE e.Salary > 60000;
```

**Key EXPLAIN Columns**:
- **id**: Query identifier
- **select_type**: Type of SELECT
- **table**: Table being accessed
- **type**: Join type (system, const, eq_ref, ref, range, index, ALL)
- **key**: Index used
- **rows**: Estimated rows examined
- **Extra**: Additional information

**Join Types (Best to Worst)**:
1. **system**: Table has 0 or 1 row
2. **const**: Table has at most 1 matching row
3. **eq_ref**: One row read for each row from previous table
4. **ref**: Multiple rows with matching index value
5. **range**: Rows retrieved using index range
6. **index**: Full index scan
7. **ALL**: Full table scan (worst)

### Index Optimization

**Creating Effective Indexes**:
```sql
-- Single column index
CREATE INDEX idx_employee_salary ON Employee(Salary);

-- Composite index (order matters)
CREATE INDEX idx_employee_dept_salary ON Employee(DeptID, Salary);

-- Covering index (includes all needed columns)
CREATE INDEX idx_employee_covering ON Employee(DeptID, FirstName, LastName, Salary);

-- Partial index (with condition)
CREATE INDEX idx_active_employees ON Employee(DeptID) WHERE Salary > 0;
```

**Index Usage Rules**:
```sql
-- Good: Uses index
SELECT * FROM Employee WHERE DeptID = 1;

-- Good: Uses composite index
SELECT * FROM Employee WHERE DeptID = 1 AND Salary > 50000;

-- Bad: Cannot use composite index efficiently
SELECT * FROM Employee WHERE Salary > 50000;  -- DeptID not specified

-- Bad: Function on column prevents index usage
SELECT * FROM Employee WHERE UPPER(FirstName) = 'JOHN';

-- Good: Function on value
SELECT * FROM Employee WHERE FirstName = UPPER('john');
```

### Query Rewriting Techniques

**Subquery to JOIN Conversion**:
```sql
-- Slower subquery
SELECT FirstName, LastName
FROM Employee
WHERE DeptID IN (
    SELECT DeptID FROM Department WHERE Budget > 300000
);

-- Faster JOIN
SELECT DISTINCT e.FirstName, e.LastName
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID
WHERE d.Budget > 300000;
```

**EXISTS vs IN Optimization**:
```sql
-- Use EXISTS for large subquery results
SELECT FirstName, LastName
FROM Employee e
WHERE EXISTS (
    SELECT 1 FROM Assignment a 
    WHERE a.EmpID = e.EmpID AND a.HoursWorked > 40
);

-- Use IN for small, static lists
SELECT FirstName, LastName
FROM Employee
WHERE DeptID IN (1, 2, 3);
```

**UNION vs OR Optimization**:
```sql
-- Sometimes UNION is faster than OR
SELECT FirstName, LastName FROM Employee WHERE DeptID = 1
UNION
SELECT FirstName, LastName FROM Employee WHERE Salary > 80000;

-- Equivalent OR query
SELECT FirstName, LastName FROM Employee 
WHERE DeptID = 1 OR Salary > 80000;
```

### WHERE Clause Optimization

**Selective Conditions First**:
```sql
-- Good: Most selective condition first
SELECT * FROM Employee 
WHERE EmpID = 123 AND DeptID = 1 AND Salary > 50000;

-- Index on (EmpID, DeptID, Salary) would be optimal
```

**Avoid Functions in WHERE**:
```sql
-- Bad: Function prevents index usage
SELECT * FROM Employee WHERE YEAR(HireDate) = 2020;

-- Good: Range condition uses index
SELECT * FROM Employee 
WHERE HireDate >= '2020-01-01' AND HireDate < '2021-01-01';
```

**Use LIMIT for Large Results**:
```sql
-- Add LIMIT to prevent excessive memory usage
SELECT * FROM Employee 
ORDER BY Salary DESC 
LIMIT 100;
```

---

## 13. Stored Procedures and Functions {#stored-procedures}

### Stored Procedures

**Basic Stored Procedure**:
```sql
DELIMITER //
CREATE PROCEDURE GetEmployeesByDept(IN dept_id INT)
BEGIN
    SELECT FirstName, LastName, Salary
    FROM Employee
    WHERE DeptID = dept_id
    ORDER BY Salary DESC;
END //
DELIMITER ;

-- Call procedure
CALL GetEmployeesByDept(1);
```

**Procedure with Multiple Parameters**:
```sql
DELIMITER //
CREATE PROCEDURE GetEmployeesBySalaryRange(
    IN min_salary DECIMAL(10,2),
    IN max_salary DECIMAL(10,2),
    OUT employee_count INT
)
BEGIN
    SELECT FirstName, LastName, Salary
    FROM Employee
    WHERE Salary BETWEEN min_salary AND max_salary;
    
    SELECT COUNT(*) INTO employee_count
    FROM Employee
    WHERE Salary BETWEEN min_salary AND max_salary;
END //
DELIMITER ;

-- Call with output parameter
CALL GetEmployeesBySalaryRange(50000, 80000, @count);
SELECT @count AS EmployeeCount;
```

**Procedure with Control Flow**:
```sql
DELIMITER //
CREATE PROCEDURE GiveRaise(IN emp_id INT, IN raise_percent DECIMAL(5,2))
BEGIN
    DECLARE current_salary DECIMAL(10,2);
    DECLARE new_salary DECIMAL(10,2);
    
    -- Get current salary
    SELECT Salary INTO current_salary
    FROM Employee
    WHERE EmpID = emp_id;
    
    -- Calculate new salary
    SET new_salary = current_salary * (1 + raise_percent / 100);
    
    -- Validate raise
    IF new_salary > current_salary * 1.5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Raise too large';
    ELSE
        UPDATE Employee
        SET Salary = new_salary
        WHERE EmpID = emp_id;
        
        SELECT CONCAT('Salary updated from ', current_salary, ' to ', new_salary) AS Result;
    END IF;
END //
DELIMITER ;
```

### User-Defined Functions

**Scalar Function**:
```sql
DELIMITER //
CREATE FUNCTION CalculateBonus(salary DECIMAL(10,2)) 
RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE bonus DECIMAL(10,2);
    
    IF salary >= 80000 THEN
        SET bonus = salary * 0.15;
    ELSEIF salary >= 60000 THEN
        SET bonus = salary * 0.10;
    ELSE
        SET bonus = salary * 0.05;
    END IF;
    
    RETURN bonus;
END //
DELIMITER ;

-- Use function in query
SELECT 
    FirstName,
    LastName,
    Salary,
    CalculateBonus(Salary) AS Bonus
FROM Employee;
```

**Table-Valued Function** (SQL Server):
```sql
CREATE FUNCTION GetDepartmentEmployees(@DeptID INT)
RETURNS TABLE
AS
RETURN (
    SELECT FirstName, LastName, Salary
    FROM Employee
    WHERE DeptID = @DeptID
);

-- Use function
SELECT * FROM GetDepartmentEmployees(1);
```

### Error Handling

**Exception Handling in Procedures**:
```sql
DELIMITER //
CREATE PROCEDURE SafeUpdateSalary(IN emp_id INT, IN new_salary DECIMAL(10,2))
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    UPDATE Employee
    SET Salary = new_salary
    WHERE EmpID = emp_id;
    
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Employee not found';
    END IF;
    
    COMMIT;
END //
DELIMITER ;
```

---

## 14. Triggers and Events {#triggers-events}

### Triggers

**BEFORE INSERT Trigger**:
```sql
DELIMITER //
CREATE TRIGGER before_employee_insert
BEFORE INSERT ON Employee
FOR EACH ROW
BEGIN
    -- Validate email format
    IF NEW.Email NOT LIKE '%@%.%' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid email format';
    END IF;
    
    -- Set default hire date
    IF NEW.HireDate IS NULL THEN
        SET NEW.HireDate = CURDATE();
    END IF;
    
    -- Validate salary
    IF NEW.Salary <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Salary must be positive';
    END IF;
END //
DELIMITER ;
```

**AFTER UPDATE Trigger**:
```sql
DELIMITER //
CREATE TRIGGER after_employee_update
AFTER UPDATE ON Employee
FOR EACH ROW
BEGIN
    -- Log salary changes
    IF OLD.Salary != NEW.Salary THEN
        INSERT INTO SalaryHistory (EmpID, OldSalary, NewSalary, ChangeDate, ChangedBy)
        VALUES (NEW.EmpID, OLD.Salary, NEW.Salary, NOW(), USER());
    END IF;
    
    -- Update department budget if salary changed
    IF OLD.Salary != NEW.Salary THEN
        UPDATE Department
        SET Budget = Budget - OLD.Salary + NEW.Salary
        WHERE DeptID = NEW.DeptID;
    END IF;
END //
DELIMITER ;
```

**BEFORE DELETE Trigger**:
```sql
DELIMITER //
CREATE TRIGGER before_employee_delete
BEFORE DELETE ON Employee
FOR EACH ROW
BEGIN
    -- Prevent deletion of managers with subordinates
    IF EXISTS (SELECT 1 FROM Employee WHERE ManagerID = OLD.EmpID) THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Cannot delete employee with subordinates';
    END IF;
    
    -- Archive employee data
    INSERT INTO EmployeeArchive
    SELECT *, NOW() AS DeletedDate, USER() AS DeletedBy
    FROM Employee
    WHERE EmpID = OLD.EmpID;
END //
DELIMITER ;
```

### Event Scheduler

**Recurring Events**:
```sql
-- Enable event scheduler
SET GLOBAL event_scheduler = ON;

-- Monthly salary report
DELIMITER //
CREATE EVENT monthly_salary_report
ON SCHEDULE EVERY 1 MONTH
STARTS '2024-01-01 09:00:00'
DO
BEGIN
    INSERT INTO SalaryReports (ReportDate, TotalSalary, AvgSalary, EmployeeCount)
    SELECT 
        CURDATE(),
        SUM(Salary),
        AVG(Salary),
        COUNT(*)
    FROM Employee;
END //
DELIMITER ;
```

**One-time Events**:
```sql
-- Cleanup old records
DELIMITER //
CREATE EVENT cleanup_old_logs
ON SCHEDULE AT '2024-12-31 23:59:59'
DO
BEGIN
    DELETE FROM ActivityLog 
    WHERE LogDate < DATE_SUB(NOW(), INTERVAL 1 YEAR);
END //
DELIMITER ;
```

---

## 15. Performance Tuning {#performance-tuning}

### Database Design for Performance

**Normalization vs Denormalization**:
```sql
-- Normalized (better for updates, less storage)
CREATE TABLE Employee (EmpID, FirstName, LastName, DeptID);
CREATE TABLE Department (DeptID, DeptName, Location);

-- Denormalized (better for reads, more storage)
CREATE TABLE EmployeeFlat (
    EmpID, FirstName, LastName, 
    DeptID, DeptName, Location  -- Duplicated data
);
```

**Partitioning Large Tables**:
```sql
-- Range partitioning by date
CREATE TABLE Orders (
    OrderID INT,
    OrderDate DATE,
    CustomerID INT,
    Amount DECIMAL(10,2)
)
PARTITION BY RANGE (YEAR(OrderDate)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024)
);
```

### Query Performance Patterns

**Efficient Pagination**:
```sql
-- Inefficient: OFFSET gets slower with large offsets
SELECT * FROM Employee ORDER BY EmpID LIMIT 1000 OFFSET 50000;

-- Efficient: Use WHERE with last seen ID
SELECT * FROM Employee 
WHERE EmpID > 50000 
ORDER BY EmpID 
LIMIT 1000;
```

**Batch Processing**:
```sql
-- Process large updates in batches
DELIMITER //
CREATE PROCEDURE BatchUpdateSalaries()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE rows_updated INT;
    
    REPEAT
        UPDATE Employee 
        SET Salary = Salary * 1.05 
        WHERE LastUpdated < '2024-01-01'
        LIMIT batch_size;
        
        SET rows_updated = ROW_COUNT();
        
        -- Small delay to prevent lock contention
        SELECT SLEEP(0.1);
        
    UNTIL rows_updated < batch_size END REPEAT;
END //
DELIMITER ;
```

**Efficient Counting**:
```sql
-- Slow for large tables
SELECT COUNT(*) FROM Employee;

-- Faster alternatives
SELECT TABLE_ROWS 
FROM information_schema.TABLES 
WHERE TABLE_NAME = 'Employee';

-- Or maintain count in separate table
CREATE TABLE TableCounts (
    TableName VARCHAR(50),
    RowCount INT,
    LastUpdated TIMESTAMP
);
```

### Caching Strategies

**Query Result Caching**:
```sql
-- Use query cache (MySQL)
SELECT SQL_CACHE FirstName, LastName FROM Employee;

-- Disable cache for specific query
SELECT SQL_NO_CACHE * FROM Employee;
```

**Materialized Views** (Simulated):
```sql
-- Create summary table
CREATE TABLE DepartmentSummary AS
SELECT 
    DeptID,
    COUNT(*) AS EmployeeCount,
    AVG(Salary) AS AvgSalary,
    SUM(Salary) AS TotalSalary
FROM Employee
GROUP BY DeptID;

-- Refresh periodically
TRUNCATE DepartmentSummary;
INSERT INTO DepartmentSummary
SELECT DeptID, COUNT(*), AVG(Salary), SUM(Salary)
FROM Employee
GROUP BY DeptID;
```

---

## 16. MCQ Practice Questions {#mcq-questions}

### Questions 1-10: Basic SQL and SELECT

**1. Which SQL clause is used to filter rows before grouping?**
a) HAVING
b) WHERE
c) ORDER BY
d) GROUP BY

**Answer: b) WHERE**
**Explanation**: WHERE filters individual rows before grouping, while HAVING filters groups after GROUP BY.

**2. What is the difference between TRUNCATE and DELETE?**
a) TRUNCATE is faster and resets auto-increment
b) DELETE is faster and resets auto-increment
c) They are identical in functionality
d) TRUNCATE can use WHERE clause

**Answer: a) TRUNCATE is faster and resets auto-increment**
**Explanation**: TRUNCATE removes all rows quickly without logging individual row deletions and resets auto-increment counters.

**3. Which function returns the current date and time?**
a) CURDATE()
b) NOW()
c) CURRENT_TIMESTAMP()
d) Both b and c

**Answer: d) Both b and c**
**Explanation**: Both NOW() and CURRENT_TIMESTAMP() return the current date and time.

**4. What does the DISTINCT keyword do?**
a) Sorts the results
b) Removes duplicate rows
c) Filters rows
d) Groups rows

**Answer: b) Removes duplicate rows**
**Explanation**: DISTINCT eliminates duplicate rows from the result set.

**5. Which operator is used for pattern matching in SQL?**
a) =
b) LIKE
c) IN
d) BETWEEN

**Answer: b) LIKE**
**Explanation**: LIKE is used with wildcards (% and _) for pattern matching in strings.

**6. What is the correct syntax for a column alias?**
a) SELECT column AS 'alias' FROM table
b) SELECT column alias FROM table
c) SELECT column AS alias FROM table
d) All of the above

**Answer: d) All of the above**
**Explanation**: All three syntaxes are valid for creating column aliases.

**7. Which clause is used to sort query results?**
a) SORT BY
b) ORDER BY
c) GROUP BY
d) ARRANGE BY

**Answer: b) ORDER BY**
**Explanation**: ORDER BY is used to sort query results in ascending or descending order.

**8. What does the LIMIT clause do?**
a) Limits the number of columns returned
b) Limits the number of rows returned
c) Limits the query execution time
d) Limits the number of tables joined

**Answer: b) Limits the number of rows returned**
**Explanation**: LIMIT restricts the number of rows returned by a query.

**9. Which SQL statement is used to modify existing records?**
a) INSERT
b) UPDATE
c) ALTER
d) MODIFY

**Answer: b) UPDATE**
**Explanation**: UPDATE is used to modify existing records in a table.

**10. What is the purpose of the COALESCE function?**
a) Concatenate strings
b) Return the first non-NULL value
c) Count NULL values
d) Convert data types

**Answer: b) Return the first non-NULL value**
**Explanation**: COALESCE returns the first non-NULL value from a list of expressions.

### Questions 11-20: JOINs and Relationships

**11. Which JOIN returns all rows from both tables?**
a) INNER JOIN
b) LEFT JOIN
c) RIGHT JOIN
d) FULL OUTER JOIN

**Answer: d) FULL OUTER JOIN**
**Explanation**: FULL OUTER JOIN returns all rows from both tables, with NULLs for non-matching rows.

**12. What does a LEFT JOIN return?**
a) Only matching rows from both tables
b) All rows from left table, matching rows from right table
c) All rows from right table, matching rows from left table
d) All rows from both tables

**Answer: b) All rows from left table, matching rows from right table**
**Explanation**: LEFT JOIN returns all rows from the left table and matching rows from the right table.

**13. Which JOIN type is most commonly used?**
a) CROSS JOIN
b) FULL OUTER JOIN
c) INNER JOIN
d) RIGHT JOIN

**Answer: c) INNER JOIN**
**Explanation**: INNER JOIN is the most commonly used join type, returning only matching rows.

**14. What does a CROSS JOIN produce?**
a) Only matching rows
b) Cartesian product of both tables
c) All rows from left table
d) No rows

**Answer: b) Cartesian product of both tables**
**Explanation**: CROSS JOIN produces a Cartesian product, combining every row from the first table with every row from the second table.

**15. In a self-join, what must be used?**
a) Different table names
b) Table aliases
c) INNER JOIN only
d) WHERE clause

**Answer: b) Table aliases**
**Explanation**: Self-joins require table aliases to distinguish between the same table used multiple times.

**16. Which is faster for checking existence?**
a) IN with subquery
b) EXISTS with subquery
c) JOIN
d) They are always the same speed

**Answer: b) EXISTS with subquery**
**Explanation**: EXISTS is often faster because it stops at the first match, while IN processes all values.

**17. What happens when you JOIN tables without a WHERE or ON clause?**
a) Error occurs
b) No rows returned
c) Cartesian product
d) Only first row from each table

**Answer: c) Cartesian product**
**Explanation**: Without join conditions, you get a Cartesian product of all rows.

**18. Which JOIN keyword is optional in most databases?**
a) LEFT
b) RIGHT
c) INNER
d) OUTER

**Answer: c) INNER**
**Explanation**: INNER is optional; just writing JOIN defaults to INNER JOIN.

**19. What does the ON clause specify in a JOIN?**
a) Which columns to select
b) Join condition
c) Sort order
d) Group by columns

**Answer: b) Join condition**
**Explanation**: The ON clause specifies the condition for joining tables.

**20. Which JOIN would you use to find records that exist in one table but not another?**
a) INNER JOIN
b) LEFT JOIN with WHERE IS NULL
c) CROSS JOIN
d) FULL OUTER JOIN

**Answer: b) LEFT JOIN with WHERE IS NULL**
**Explanation**: LEFT JOIN with WHERE IS NULL finds records in the left table that don't have matches in the right table.

### Questions 21-30: Aggregate Functions and Advanced Queries

**21. Which aggregate function ignores NULL values?**
a) COUNT(*)
b) COUNT(column)
c) SUM(column)
d) Both b and c

**Answer: d) Both b and c**
**Explanation**: COUNT(column) and SUM(column) ignore NULL values, while COUNT(*) counts all rows.

**22. What is the difference between HAVING and WHERE?**
a) No difference
b) HAVING filters groups, WHERE filters rows
c) WHERE filters groups, HAVING filters rows
d) HAVING is faster than WHERE

**Answer: b) HAVING filters groups, WHERE filters rows**
**Explanation**: WHERE filters individual rows before grouping, HAVING filters groups after GROUP BY.

**23. Which function would you use to get the number of distinct values?**
a) COUNT(*)
b) COUNT(DISTINCT column)
c) DISTINCT COUNT(column)
d) UNIQUE(column)

**Answer: b) COUNT(DISTINCT column)**
**Explanation**: COUNT(DISTINCT column) returns the number of unique non-NULL values.

**24. What does the GROUP BY clause do?**
a) Sorts the results
b) Groups rows with same values
c) Filters rows
d) Joins tables

**Answer: b) Groups rows with same values**
**Explanation**: GROUP BY groups rows that have the same values in specified columns.

**25. Which aggregate function calculates the average?**
a) MEAN()
b) AVERAGE()
c) AVG()
d) MEDIAN()

**Answer: c) AVG()**
**Explanation**: AVG() calculates the arithmetic mean of numeric values.

**26. What happens if you use an aggregate function without GROUP BY?**
a) Error occurs
b) Returns one row with aggregate of all rows
c) Returns NULL
d) Returns first row only

**Answer: b) Returns one row with aggregate of all rows**
**Explanation**: Without GROUP BY, aggregate functions operate on all rows and return a single result.

**27. Which clause must come after GROUP BY?**
a) WHERE
b) ORDER BY
c) HAVING
d) SELECT

**Answer: c) HAVING**
**Explanation**: HAVING must come after GROUP BY to filter the grouped results.

**28. What does UNION do?**
a) Joins tables horizontally
b) Combines result sets vertically
c) Intersects two queries
d) Subtracts one query from another

**Answer: b) Combines result sets vertically**
**Explanation**: UNION combines the results of two or more SELECT statements vertically.

**29. What is the difference between UNION and UNION ALL?**
a) No difference
b) UNION removes duplicates, UNION ALL keeps them
c) UNION ALL removes duplicates, UNION keeps them
d) UNION is faster than UNION ALL

**Answer: b) UNION removes duplicates, UNION ALL keeps them**
**Explanation**: UNION removes duplicate rows, while UNION ALL includes all rows including duplicates.

**30. Which window function assigns sequential numbers to rows?**
a) RANK()
b) DENSE_RANK()
c) ROW_NUMBER()
d) NTILE()

**Answer: c) ROW_NUMBER()**
**Explanation**: ROW_NUMBER() assigns unique sequential numbers to rows within a partition.

---

## Study Tips for SQL Mastery

### Practice Strategy

**1. Start with Basics**
- Master SELECT, WHERE, ORDER BY
- Practice with simple datasets
- Understand data types and NULL handling

**2. Progress to Intermediate**
- Learn all JOIN types thoroughly
- Practice aggregate functions and GROUP BY
- Master subqueries and EXISTS

**3. Advance to Complex Topics**
- Window functions and CTEs
- Query optimization techniques
- Stored procedures and triggers

### Common Mistakes to Avoid

**1. JOIN Mistakes**
- Forgetting JOIN conditions (Cartesian product)
- Using wrong JOIN type
- Not handling NULL values in JOINs

**2. Aggregate Function Errors**
- Mixing aggregates with non-grouped columns
- Using WHERE instead of HAVING with aggregates
- Not understanding NULL behavior

**3. Performance Issues**
- Not using indexes effectively
- Writing inefficient subqueries
- Using functions in WHERE clauses

### Best Practices

**1. Code Style**
- Use consistent formatting
- Meaningful table and column aliases
- Comment complex queries

**2. Performance**
- Create appropriate indexes
- Use EXPLAIN to analyze queries
- Avoid SELECT * in production

**3. Security**
- Use parameterized queries
- Implement proper access controls
- Validate input data

---

**End of SQL Queries Guide**

This comprehensive guide covers all essential SQL concepts from basic queries to advanced optimization techniques. Practice with real databases and gradually increase complexity to master SQL querying skills.