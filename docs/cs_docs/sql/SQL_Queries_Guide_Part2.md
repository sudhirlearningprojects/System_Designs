# SQL Queries Guide - Part 2

*Advanced SQL concepts covering views, joins, aggregate functions, and set operations*

## Table of Contents (Part 2)
6. [Views](#views)
7. [JOIN Operations](#joins)
8. [Aggregate Functions](#aggregate-functions)
9. [Set Operations](#set-operations)
10. [Subqueries and Nested Queries](#subqueries)

---

## 6. Views {#views}

### What are Views?

**View**: Virtual table based on the result of a SELECT statement
- **Virtual**: No physical storage of data
- **Dynamic**: Always shows current data from underlying tables
- **Security**: Hide sensitive columns/rows
- **Simplification**: Complex queries appear as simple tables

### Creating Views

**Basic View Creation**:
```sql
CREATE VIEW EmployeeView AS
SELECT EmpID, FirstName, LastName, DeptID, Salary
FROM Employee;
```

**View with JOIN**:
```sql
CREATE VIEW EmployeeDetails AS
SELECT 
    e.EmpID,
    e.FirstName,
    e.LastName,
    e.Salary,
    d.DeptName,
    d.Location
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID;
```

**View with Calculations**:
```sql
CREATE VIEW EmployeeSummary AS
SELECT 
    EmpID,
    CONCAT(FirstName, ' ', LastName) AS FullName,
    Salary,
    Salary * 12 AS AnnualSalary,
    CASE 
        WHEN Salary >= 80000 THEN 'Senior'
        WHEN Salary >= 50000 THEN 'Mid-level'
        ELSE 'Junior'
    END AS Level
FROM Employee;
```

**View with Aggregation**:
```sql
CREATE VIEW DepartmentStats AS
SELECT 
    d.DeptID,
    d.DeptName,
    COUNT(e.EmpID) AS EmployeeCount,
    AVG(e.Salary) AS AvgSalary,
    MAX(e.Salary) AS MaxSalary,
    MIN(e.Salary) AS MinSalary
FROM Department d
LEFT JOIN Employee e ON d.DeptID = e.DeptID
GROUP BY d.DeptID, d.DeptName;
```

### Using Views

**Query Views Like Tables**:
```sql
-- Simple select
SELECT * FROM EmployeeView;

-- With WHERE clause
SELECT * FROM EmployeeDetails 
WHERE DeptName = 'Engineering';

-- With ORDER BY
SELECT * FROM EmployeeSummary 
ORDER BY AnnualSalary DESC;
```

**Views in JOINs**:
```sql
SELECT ev.FullName, ds.DeptName, ds.AvgSalary
FROM EmployeeSummary ev
JOIN DepartmentStats ds ON ev.EmpID IN (
    SELECT EmpID FROM Employee WHERE DeptID = ds.DeptID
);
```

### Modifying Views

**ALTER VIEW**:
```sql
ALTER VIEW EmployeeView AS
SELECT EmpID, FirstName, LastName, Email, DeptID, Salary
FROM Employee
WHERE Salary > 40000;
```

**OR REPLACE**:
```sql
CREATE OR REPLACE VIEW EmployeeView AS
SELECT EmpID, FirstName, LastName, Email, DeptID, Salary
FROM Employee
WHERE Salary > 40000;
```

### Updatable Views

**Simple Updatable View**:
```sql
CREATE VIEW ActiveEmployees AS
SELECT EmpID, FirstName, LastName, Salary, DeptID
FROM Employee
WHERE HireDate >= '2020-01-01';

-- Can perform DML operations
UPDATE ActiveEmployees 
SET Salary = Salary * 1.1 
WHERE DeptID = 1;

INSERT INTO ActiveEmployees (EmpID, FirstName, LastName, Salary, DeptID)
VALUES (101, 'John', 'Smith', 55000, 1);
```

**View Update Rules**:
- **Single Table**: View must be based on single table
- **No Aggregates**: No GROUP BY, HAVING, aggregate functions
- **No DISTINCT**: Cannot use DISTINCT
- **No Calculated Fields**: Only direct column references
- **All Required Columns**: Must include all NOT NULL columns for INSERT

### View Security

**Column-Level Security**:
```sql
-- Hide sensitive salary information
CREATE VIEW PublicEmployeeInfo AS
SELECT EmpID, FirstName, LastName, Email, DeptID
FROM Employee;
-- Salary column not visible
```

**Row-Level Security**:
```sql
-- Show only current user's department employees
CREATE VIEW MyDepartmentEmployees AS
SELECT *
FROM Employee
WHERE DeptID = (
    SELECT DeptID 
    FROM Employee 
    WHERE EmpID = USER_ID()  -- Current user's ID
);
```

### Dropping Views

```sql
DROP VIEW EmployeeView;

-- Safe drop
DROP VIEW IF EXISTS EmployeeView;
```

### View Performance Considerations

**Advantages**:
- **Security**: Hide sensitive data
- **Simplicity**: Complex queries appear simple
- **Consistency**: Standardized data access
- **Maintenance**: Change logic in one place

**Disadvantages**:
- **Performance**: Additional layer of abstraction
- **Complexity**: Nested views can be confusing
- **Limitations**: Update restrictions

---

## 7. JOIN Operations {#joins}

### JOIN Fundamentals

**Purpose**: Combine rows from multiple tables based on related columns
**Types**: INNER, LEFT OUTER, RIGHT OUTER, FULL OUTER, CROSS

### Sample Data for JOIN Examples

```sql
-- Sample data
INSERT INTO Department VALUES 
(1, 'Engineering', 'New York', 500000),
(2, 'Sales', 'Chicago', 300000),
(3, 'Marketing', 'LA', 250000),
(4, 'HR', 'New York', 150000);

INSERT INTO Employee VALUES 
(1, 'John', 'Doe', 'john@company.com', '555-1234', '2020-01-15', 75000, 1, NULL),
(2, 'Jane', 'Smith', 'jane@company.com', '555-5678', '2019-03-20', 68000, 1, 1),
(3, 'Bob', 'Johnson', 'bob@company.com', '555-9012', '2021-06-10', 52000, 2, 1),
(4, 'Alice', 'Brown', 'alice@company.com', '555-3456', '2020-11-05', 61000, 2, 1),
(5, 'Charlie', 'Wilson', 'charlie@company.com', '555-7890', '2022-02-14', 45000, NULL, NULL);
```

### INNER JOIN

**Syntax**:
```sql
SELECT columns
FROM table1
INNER JOIN table2 ON table1.column = table2.column;
```

**Basic INNER JOIN**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    e.Salary,
    d.DeptName
FROM Employee e
INNER JOIN Department d ON e.DeptID = d.DeptID;
```

**Result**: Only employees who have a matching department
```
FirstName | LastName | Salary | DeptName
----------|----------|--------|------------
John      | Doe      | 75000  | Engineering
Jane      | Smith    | 68000  | Engineering
Bob       | Johnson  | 52000  | Sales
Alice     | Brown    | 61000  | Sales
```

**Multiple INNER JOINs**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    d.DeptName,
    p.ProjectName,
    a.Role
FROM Employee e
INNER JOIN Department d ON e.DeptID = d.DeptID
INNER JOIN Assignment a ON e.EmpID = a.EmpID
INNER JOIN Project p ON a.ProjectID = p.ProjectID;
```

**INNER JOIN with WHERE**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    d.DeptName,
    e.Salary
FROM Employee e
INNER JOIN Department d ON e.DeptID = d.DeptID
WHERE e.Salary > 60000
  AND d.Location = 'New York';
```

### LEFT OUTER JOIN (LEFT JOIN)

**Syntax**:
```sql
SELECT columns
FROM table1
LEFT JOIN table2 ON table1.column = table2.column;
```

**Basic LEFT JOIN**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    e.Salary,
    d.DeptName
FROM Employee e
LEFT JOIN Department d ON e.DeptID = d.DeptID;
```

**Result**: All employees, including those without departments
```
FirstName | LastName | Salary | DeptName
----------|----------|--------|------------
John      | Doe      | 75000  | Engineering
Jane      | Smith    | 68000  | Engineering
Bob       | Johnson  | 52000  | Sales
Alice     | Brown    | 61000  | Sales
Charlie   | Wilson   | 45000  | NULL
```

**Find Unmatched Records**:
```sql
-- Employees without departments
SELECT 
    e.FirstName,
    e.LastName
FROM Employee e
LEFT JOIN Department d ON e.DeptID = d.DeptID
WHERE d.DeptID IS NULL;
```

**LEFT JOIN with Aggregation**:
```sql
SELECT 
    d.DeptName,
    COUNT(e.EmpID) AS EmployeeCount,
    AVG(e.Salary) AS AvgSalary
FROM Department d
LEFT JOIN Employee e ON d.DeptID = e.DeptID
GROUP BY d.DeptID, d.DeptName;
```

### RIGHT OUTER JOIN (RIGHT JOIN)

**Syntax**:
```sql
SELECT columns
FROM table1
RIGHT JOIN table2 ON table1.column = table2.column;
```

**Basic RIGHT JOIN**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    d.DeptName
FROM Employee e
RIGHT JOIN Department d ON e.DeptID = d.DeptID;
```

**Result**: All departments, including those without employees
```
FirstName | LastName | DeptName
----------|----------|------------
John      | Doe      | Engineering
Jane      | Smith    | Engineering
Bob       | Johnson  | Sales
Alice     | Brown    | Sales
NULL      | NULL     | Marketing
NULL      | NULL     | HR
```

**Find Departments Without Employees**:
```sql
SELECT d.DeptName
FROM Employee e
RIGHT JOIN Department d ON e.DeptID = d.DeptID
WHERE e.EmpID IS NULL;
```

### FULL OUTER JOIN

**Syntax**:
```sql
SELECT columns
FROM table1
FULL OUTER JOIN table2 ON table1.column = table2.column;
```

**Basic FULL OUTER JOIN**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    d.DeptName
FROM Employee e
FULL OUTER JOIN Department d ON e.DeptID = d.DeptID;
```

**Result**: All employees and all departments
```
FirstName | LastName | DeptName
----------|----------|------------
John      | Doe      | Engineering
Jane      | Smith    | Engineering
Bob       | Johnson  | Sales
Alice     | Brown    | Sales
Charlie   | Wilson   | NULL
NULL      | NULL     | Marketing
NULL      | NULL     | HR
```

**MySQL Alternative** (No native FULL OUTER JOIN):
```sql
SELECT e.FirstName, e.LastName, d.DeptName
FROM Employee e
LEFT JOIN Department d ON e.DeptID = d.DeptID
UNION
SELECT e.FirstName, e.LastName, d.DeptName
FROM Employee e
RIGHT JOIN Department d ON e.DeptID = d.DeptID;
```

### CROSS JOIN

**Syntax**:
```sql
SELECT columns
FROM table1
CROSS JOIN table2;
```

**Basic CROSS JOIN**:
```sql
SELECT 
    e.FirstName,
    d.DeptName
FROM Employee e
CROSS JOIN Department d;
```

**Result**: Cartesian product (every employee with every department)
- 5 employees × 4 departments = 20 rows

**Practical Use Case**:
```sql
-- Generate all possible employee-project combinations
SELECT 
    e.FirstName,
    e.LastName,
    p.ProjectName
FROM Employee e
CROSS JOIN Project p
WHERE e.DeptID = 1;  -- Only engineering employees
```

### Self JOIN

**Purpose**: Join table with itself to find relationships within the table

**Employee-Manager Relationship**:
```sql
SELECT 
    e.FirstName + ' ' + e.LastName AS Employee,
    m.FirstName + ' ' + m.LastName AS Manager
FROM Employee e
LEFT JOIN Employee m ON e.ManagerID = m.EmpID;
```

**Find Employees in Same Department**:
```sql
SELECT 
    e1.FirstName AS Employee1,
    e2.FirstName AS Employee2,
    e1.DeptID
FROM Employee e1
JOIN Employee e2 ON e1.DeptID = e2.DeptID
WHERE e1.EmpID < e2.EmpID;  -- Avoid duplicates
```

### Advanced JOIN Techniques

**Multiple Conditions**:
```sql
SELECT *
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID 
                 AND d.Budget > 200000;
```

**JOIN with CASE**:
```sql
SELECT 
    e.FirstName,
    e.LastName,
    CASE 
        WHEN d.DeptName IS NULL THEN 'Unassigned'
        ELSE d.DeptName
    END AS Department
FROM Employee e
LEFT JOIN Department d ON e.DeptID = d.DeptID;
```

**JOIN Performance Tips**:
```sql
-- Use indexes on JOIN columns
CREATE INDEX idx_employee_deptid ON Employee(DeptID);
CREATE INDEX idx_department_deptid ON Department(DeptID);

-- Filter early with WHERE
SELECT e.FirstName, d.DeptName
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID
WHERE e.Salary > 50000;  -- Filter before JOIN when possible
```

---

## 8. Aggregate Functions {#aggregate-functions}

### Basic Aggregate Functions

**COUNT**: Count number of rows
```sql
-- Count all employees
SELECT COUNT(*) AS TotalEmployees FROM Employee;

-- Count non-null values
SELECT COUNT(ManagerID) AS EmployeesWithManager FROM Employee;

-- Count distinct values
SELECT COUNT(DISTINCT DeptID) AS UniqueDepartments FROM Employee;
```

**SUM**: Calculate total
```sql
-- Total salary expense
SELECT SUM(Salary) AS TotalSalaryExpense FROM Employee;

-- Sum with condition
SELECT SUM(CASE WHEN DeptID = 1 THEN Salary ELSE 0 END) AS EngineeringSalaries
FROM Employee;
```

**AVG**: Calculate average
```sql
-- Average salary
SELECT AVG(Salary) AS AverageSalary FROM Employee;

-- Average excluding nulls
SELECT AVG(COALESCE(Salary, 0)) AS AverageSalaryWithZeros FROM Employee;
```

**MIN/MAX**: Find minimum/maximum values
```sql
-- Salary range
SELECT 
    MIN(Salary) AS MinSalary,
    MAX(Salary) AS MaxSalary,
    MAX(Salary) - MIN(Salary) AS SalaryRange
FROM Employee;

-- Earliest and latest hire dates
SELECT 
    MIN(HireDate) AS EarliestHire,
    MAX(HireDate) AS LatestHire
FROM Employee;
```

### GROUP BY Clause

**Basic Grouping**:
```sql
-- Employee count by department
SELECT 
    DeptID,
    COUNT(*) AS EmployeeCount
FROM Employee
GROUP BY DeptID;
```

**Multiple Grouping Columns**:
```sql
-- Employee count by department and hire year
SELECT 
    DeptID,
    YEAR(HireDate) AS HireYear,
    COUNT(*) AS EmployeeCount
FROM Employee
GROUP BY DeptID, YEAR(HireDate)
ORDER BY DeptID, HireYear;
```

**GROUP BY with JOINs**:
```sql
-- Department statistics
SELECT 
    d.DeptName,
    COUNT(e.EmpID) AS EmployeeCount,
    AVG(e.Salary) AS AvgSalary,
    SUM(e.Salary) AS TotalSalary
FROM Department d
LEFT JOIN Employee e ON d.DeptID = e.DeptID
GROUP BY d.DeptID, d.DeptName;
```

### HAVING Clause

**Filter Groups**:
```sql
-- Departments with more than 2 employees
SELECT 
    DeptID,
    COUNT(*) AS EmployeeCount
FROM Employee
GROUP BY DeptID
HAVING COUNT(*) > 2;
```

**HAVING vs WHERE**:
```sql
-- WHERE filters rows before grouping
-- HAVING filters groups after grouping
SELECT 
    DeptID,
    AVG(Salary) AS AvgSalary
FROM Employee
WHERE HireDate >= '2020-01-01'  -- Filter individual rows
GROUP BY DeptID
HAVING AVG(Salary) > 60000;     -- Filter groups
```

**Complex HAVING Conditions**:
```sql
SELECT 
    DeptID,
    COUNT(*) AS EmployeeCount,
    AVG(Salary) AS AvgSalary
FROM Employee
GROUP BY DeptID
HAVING COUNT(*) >= 2 
   AND AVG(Salary) > 55000;
```

### Advanced Aggregate Functions

**String Aggregation**:
```sql
-- MySQL
SELECT 
    DeptID,
    GROUP_CONCAT(FirstName ORDER BY FirstName SEPARATOR ', ') AS EmployeeNames
FROM Employee
GROUP BY DeptID;

-- SQL Server
SELECT 
    DeptID,
    STRING_AGG(FirstName, ', ') WITHIN GROUP (ORDER BY FirstName) AS EmployeeNames
FROM Employee
GROUP BY DeptID;
```

**Statistical Functions**:
```sql
-- Standard deviation and variance
SELECT 
    DeptID,
    AVG(Salary) AS AvgSalary,
    STDDEV(Salary) AS SalaryStdDev,
    VARIANCE(Salary) AS SalaryVariance
FROM Employee
GROUP BY DeptID;
```

**Window Functions** (Advanced):
```sql
-- Running totals
SELECT 
    FirstName,
    LastName,
    Salary,
    SUM(Salary) OVER (ORDER BY EmpID) AS RunningTotal
FROM Employee;

-- Rank employees by salary within department
SELECT 
    FirstName,
    LastName,
    DeptID,
    Salary,
    RANK() OVER (PARTITION BY DeptID ORDER BY Salary DESC) AS SalaryRank
FROM Employee;
```

### Aggregate Function Examples

**Business Intelligence Queries**:
```sql
-- Monthly hiring trends
SELECT 
    YEAR(HireDate) AS HireYear,
    MONTH(HireDate) AS HireMonth,
    COUNT(*) AS NewHires
FROM Employee
GROUP BY YEAR(HireDate), MONTH(HireDate)
ORDER BY HireYear, HireMonth;

-- Salary distribution
SELECT 
    CASE 
        WHEN Salary < 50000 THEN 'Under 50K'
        WHEN Salary < 70000 THEN '50K-70K'
        WHEN Salary < 90000 THEN '70K-90K'
        ELSE 'Over 90K'
    END AS SalaryRange,
    COUNT(*) AS EmployeeCount,
    ROUND(AVG(Salary), 2) AS AvgSalary
FROM Employee
GROUP BY 
    CASE 
        WHEN Salary < 50000 THEN 'Under 50K'
        WHEN Salary < 70000 THEN '50K-70K'
        WHEN Salary < 90000 THEN '70K-90K'
        ELSE 'Over 90K'
    END
ORDER BY AvgSalary;
```

**Performance Metrics**:
```sql
-- Department efficiency (employees per budget dollar)
SELECT 
    d.DeptName,
    d.Budget,
    COUNT(e.EmpID) AS EmployeeCount,
    d.Budget / COUNT(e.EmpID) AS BudgetPerEmployee
FROM Department d
LEFT JOIN Employee e ON d.DeptID = e.DeptID
GROUP BY d.DeptID, d.DeptName, d.Budget
HAVING COUNT(e.EmpID) > 0
ORDER BY BudgetPerEmployee DESC;
```

---

## 9. Set Operations {#set-operations}

### UNION

**Purpose**: Combine results from multiple SELECT statements
**Rule**: All SELECT statements must have same number of columns with compatible data types

**Basic UNION**:
```sql
-- Combine current and former employees
SELECT FirstName, LastName, 'Current' AS Status
FROM Employee
UNION
SELECT FirstName, LastName, 'Former' AS Status
FROM FormerEmployee;
```

**UNION vs UNION ALL**:
```sql
-- UNION removes duplicates
SELECT DeptID FROM Employee
UNION
SELECT DeptID FROM Department;

-- UNION ALL keeps duplicates (faster)
SELECT DeptID FROM Employee
UNION ALL
SELECT DeptID FROM Department;
```

**Complex UNION Example**:
```sql
-- High earners from different sources
SELECT 'Employee' AS Source, FirstName, LastName, Salary
FROM Employee
WHERE Salary > 70000
UNION
SELECT 'Contractor' AS Source, FirstName, LastName, HourlyRate * 2080
FROM Contractor
WHERE HourlyRate * 2080 > 70000
ORDER BY Salary DESC;
```

**UNION with Different Tables**:
```sql
-- Contact list from multiple sources
SELECT 
    CONCAT(FirstName, ' ', LastName) AS Name,
    Email,
    'Employee' AS Type
FROM Employee
UNION
SELECT 
    ContactName AS Name,
    Email,
    'Customer' AS Type
FROM Customer
WHERE Email IS NOT NULL
ORDER BY Name;
```

### INTERSECT

**Purpose**: Return rows that exist in both result sets
**Note**: Not supported in MySQL, use JOIN or EXISTS instead

**Standard INTERSECT**:
```sql
-- Employees who are also customers (if same email)
SELECT Email FROM Employee
INTERSECT
SELECT Email FROM Customer;
```

**MySQL Alternative using JOIN**:
```sql
SELECT DISTINCT e.Email
FROM Employee e
INNER JOIN Customer c ON e.Email = c.Email;
```

**MySQL Alternative using EXISTS**:
```sql
SELECT DISTINCT Email
FROM Employee e
WHERE EXISTS (
    SELECT 1 FROM Customer c 
    WHERE c.Email = e.Email
);
```

### EXCEPT (MINUS)

**Purpose**: Return rows from first result set that don't exist in second
**Note**: Not supported in MySQL, use LEFT JOIN or NOT EXISTS instead

**Standard EXCEPT**:
```sql
-- Employees who are not customers
SELECT Email FROM Employee
EXCEPT
SELECT Email FROM Customer;
```

**MySQL Alternative using LEFT JOIN**:
```sql
SELECT DISTINCT e.Email
FROM Employee e
LEFT JOIN Customer c ON e.Email = c.Email
WHERE c.Email IS NULL;
```

**MySQL Alternative using NOT EXISTS**:
```sql
SELECT DISTINCT Email
FROM Employee e
WHERE NOT EXISTS (
    SELECT 1 FROM Customer c 
    WHERE c.Email = e.Email
);
```

### Practical Set Operation Examples

**Data Quality Checks**:
```sql
-- Find inconsistent data between tables
SELECT 'Missing in Employee' AS Issue, DeptID
FROM Department
WHERE DeptID NOT IN (SELECT DISTINCT DeptID FROM Employee WHERE DeptID IS NOT NULL)
UNION
SELECT 'Missing in Department' AS Issue, DeptID
FROM Employee
WHERE DeptID NOT IN (SELECT DeptID FROM Department)
  AND DeptID IS NOT NULL;
```

**Reporting Combinations**:
```sql
-- Quarterly sales summary
SELECT 'Q1' AS Quarter, SUM(TotalAmount) AS Revenue
FROM Orders
WHERE MONTH(OrderDate) IN (1,2,3)
UNION
SELECT 'Q2' AS Quarter, SUM(TotalAmount) AS Revenue
FROM Orders
WHERE MONTH(OrderDate) IN (4,5,6)
UNION
SELECT 'Q3' AS Quarter, SUM(TotalAmount) AS Revenue
FROM Orders
WHERE MONTH(OrderDate) IN (7,8,9)
UNION
SELECT 'Q4' AS Quarter, SUM(TotalAmount) AS Revenue
FROM Orders
WHERE MONTH(OrderDate) IN (10,11,12)
ORDER BY Quarter;
```

---

## 10. Subqueries and Nested Queries {#subqueries}

### Introduction to Subqueries

**Subquery**: Query nested inside another query
**Types**: 
- **Scalar**: Returns single value
- **Row**: Returns single row
- **Table**: Returns multiple rows/columns
- **Correlated**: References outer query
- **Non-correlated**: Independent of outer query

### Scalar Subqueries

**Single Value Return**:
```sql
-- Employees earning more than average
SELECT FirstName, LastName, Salary
FROM Employee
WHERE Salary > (SELECT AVG(Salary) FROM Employee);
```

**Subquery in SELECT**:
```sql
SELECT 
    FirstName,
    LastName,
    Salary,
    (SELECT AVG(Salary) FROM Employee) AS CompanyAvg,
    Salary - (SELECT AVG(Salary) FROM Employee) AS Difference
FROM Employee;
```

### IN Clause

**Basic IN with Subquery**:
```sql
-- Employees in high-budget departments
SELECT FirstName, LastName, DeptID
FROM Employee
WHERE DeptID IN (
    SELECT DeptID 
    FROM Department 
    WHERE Budget > 300000
);
```

**NOT IN**:
```sql
-- Employees not in specific departments
SELECT FirstName, LastName
FROM Employee
WHERE DeptID NOT IN (
    SELECT DeptID 
    FROM Department 
    WHERE DeptName IN ('HR', 'Marketing')
);
```

**IN with Multiple Columns**:
```sql
-- Employees with same department and salary as specific criteria
SELECT FirstName, LastName
FROM Employee
WHERE (DeptID, Salary) IN (
    SELECT DeptID, MAX(Salary)
    FROM Employee
    GROUP BY DeptID
);
```

### EXISTS Clause

**Basic EXISTS**:
```sql
-- Departments that have employees
SELECT DeptName
FROM Department d
WHERE EXISTS (
    SELECT 1 
    FROM Employee e 
    WHERE e.DeptID = d.DeptID
);
```

**NOT EXISTS**:
```sql
-- Departments without employees
SELECT DeptName
FROM Department d
WHERE NOT EXISTS (
    SELECT 1 
    FROM Employee e 
    WHERE e.DeptID = d.DeptID
);
```

**EXISTS vs IN Performance**:
```sql
-- EXISTS is often faster for large datasets
-- EXISTS stops at first match
-- IN processes all values

-- Use EXISTS when checking existence
SELECT CustomerID, CompanyName
FROM Customer c
WHERE EXISTS (
    SELECT 1 FROM Orders o 
    WHERE o.CustomerID = c.CustomerID
);

-- Use IN when you need specific values
SELECT FirstName, LastName
FROM Employee
WHERE DeptID IN (1, 2, 3);
```

### Correlated Subqueries

**Definition**: Subquery that references columns from outer query

**Find Highest Paid Employee per Department**:
```sql
SELECT FirstName, LastName, DeptID, Salary
FROM Employee e1
WHERE Salary = (
    SELECT MAX(Salary)
    FROM Employee e2
    WHERE e2.DeptID = e1.DeptID
);
```

**Employees Above Department Average**:
```sql
SELECT FirstName, LastName, DeptID, Salary
FROM Employee e1
WHERE Salary > (
    SELECT AVG(Salary)
    FROM Employee e2
    WHERE e2.DeptID = e1.DeptID
);
```

**Row Number Simulation**:
```sql
-- Second highest salary per department
SELECT FirstName, LastName, DeptID, Salary
FROM Employee e1
WHERE 2 = (
    SELECT COUNT(DISTINCT Salary)
    FROM Employee e2
    WHERE e2.DeptID = e1.DeptID
      AND e2.Salary >= e1.Salary
);
```

### Subqueries in Different Clauses

**Subquery in FROM (Derived Table)**:
```sql
SELECT 
    DeptStats.DeptID,
    DeptStats.AvgSalary,
    d.DeptName
FROM (
    SELECT DeptID, AVG(Salary) AS AvgSalary
    FROM Employee
    GROUP BY DeptID
) AS DeptStats
JOIN Department d ON DeptStats.DeptID = d.DeptID
WHERE DeptStats.AvgSalary > 60000;
```

**Subquery in HAVING**:
```sql
SELECT DeptID, AVG(Salary) AS AvgSalary
FROM Employee
GROUP BY DeptID
HAVING AVG(Salary) > (
    SELECT AVG(Salary) * 1.1
    FROM Employee
);
```

**Subquery in UPDATE**:
```sql
-- Give raise to employees in high-performing departments
UPDATE Employee
SET Salary = Salary * 1.1
WHERE DeptID IN (
    SELECT DeptID
    FROM Department
    WHERE Budget > 400000
);
```

**Subquery in DELETE**:
```sql
-- Delete employees from departments being closed
DELETE FROM Employee
WHERE DeptID IN (
    SELECT DeptID
    FROM Department
    WHERE Budget < 100000
);
```

### Advanced Subquery Patterns

**ANY/SOME Operator**:
```sql
-- Employees earning more than ANY employee in HR
SELECT FirstName, LastName, Salary
FROM Employee
WHERE Salary > ANY (
    SELECT Salary
    FROM Employee e
    JOIN Department d ON e.DeptID = d.DeptID
    WHERE d.DeptName = 'HR'
);
```

**ALL Operator**:
```sql
-- Employees earning more than ALL employees in HR
SELECT FirstName, LastName, Salary
FROM Employee
WHERE Salary > ALL (
    SELECT Salary
    FROM Employee e
    JOIN Department d ON e.DeptID = d.DeptID
    WHERE d.DeptName = 'HR'
);
```

**Multiple Level Nesting**:
```sql
-- Employees in departments with above-average budgets
-- in locations with multiple departments
SELECT FirstName, LastName
FROM Employee
WHERE DeptID IN (
    SELECT DeptID
    FROM Department
    WHERE Budget > (SELECT AVG(Budget) FROM Department)
      AND Location IN (
          SELECT Location
          FROM Department
          GROUP BY Location
          HAVING COUNT(*) > 1
      )
);
```

### Common Table Expressions (CTE)

**Alternative to Complex Subqueries**:
```sql
-- Using CTE instead of nested subqueries
WITH DeptAvgSalary AS (
    SELECT DeptID, AVG(Salary) AS AvgSalary
    FROM Employee
    GROUP BY DeptID
),
HighPayingDepts AS (
    SELECT DeptID
    FROM DeptAvgSalary
    WHERE AvgSalary > 60000
)
SELECT e.FirstName, e.LastName, e.Salary
FROM Employee e
JOIN HighPayingDepts h ON e.DeptID = h.DeptID;
```

**Recursive CTE** (Employee Hierarchy):
```sql
WITH EmployeeHierarchy AS (
    -- Anchor: Top-level managers
    SELECT EmpID, FirstName, LastName, ManagerID, 0 AS Level
    FROM Employee
    WHERE ManagerID IS NULL
    
    UNION ALL
    
    -- Recursive: Subordinates
    SELECT e.EmpID, e.FirstName, e.LastName, e.ManagerID, eh.Level + 1
    FROM Employee e
    JOIN EmployeeHierarchy eh ON e.ManagerID = eh.EmpID
)
SELECT * FROM EmployeeHierarchy
ORDER BY Level, LastName;
```

### Subquery Performance Tips

**1. Use EXISTS instead of IN for large datasets**:
```sql
-- Slower with large subquery results
SELECT * FROM Employee
WHERE DeptID IN (SELECT DeptID FROM LargeDepartmentTable);

-- Faster
SELECT * FROM Employee e
WHERE EXISTS (SELECT 1 FROM LargeDepartmentTable d WHERE d.DeptID = e.DeptID);
```

**2. Avoid correlated subqueries when possible**:
```sql
-- Slower (correlated)
SELECT FirstName, LastName
FROM Employee e1
WHERE Salary > (SELECT AVG(Salary) FROM Employee e2 WHERE e2.DeptID = e1.DeptID);

-- Faster (join with derived table)
SELECT e.FirstName, e.LastName
FROM Employee e
JOIN (
    SELECT DeptID, AVG(Salary) AS AvgSalary
    FROM Employee
    GROUP BY DeptID
) dept_avg ON e.DeptID = dept_avg.DeptID
WHERE e.Salary > dept_avg.AvgSalary;
```

**3. Use appropriate indexes**:
```sql
-- Index columns used in subquery conditions
CREATE INDEX idx_employee_deptid ON Employee(DeptID);
CREATE INDEX idx_employee_salary ON Employee(Salary);
```

---

*Continue to Part 3 for Advanced SQL Topics, Query Optimization, and Comprehensive MCQ Practice*