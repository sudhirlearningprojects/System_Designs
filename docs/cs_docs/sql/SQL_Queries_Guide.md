# SQL Queries Guide - Part 1

*Comprehensive guide covering SQL queries from basics to advanced concepts*

## Table of Contents (Part 1)
1. [Introduction to SQL](#introduction)
2. [Basic SQL Structure](#basic-structure)
3. [SELECT Statement](#select-statement)
4. [Data Manipulation Language (DML)](#dml)
5. [Data Definition Language (DDL)](#ddl)

---

## 1. Introduction to SQL {#introduction}

### What is SQL?

**SQL (Structured Query Language)**: Standard language for managing relational databases
- **Declarative**: Specify what you want, not how to get it
- **Standardized**: ANSI/ISO standard with vendor extensions
- **Comprehensive**: Data definition, manipulation, control, and querying

### SQL Categories

**1. Data Query Language (DQL)**
- **SELECT**: Retrieve data from tables
- **Most Common**: Primary focus of this guide

**2. Data Manipulation Language (DML)**
- **INSERT**: Add new records
- **UPDATE**: Modify existing records
- **DELETE**: Remove records

**3. Data Definition Language (DDL)**
- **CREATE**: Create database objects
- **ALTER**: Modify database objects
- **DROP**: Remove database objects
- **TRUNCATE**: Remove all records from table

**4. Data Control Language (DCL)**
- **GRANT**: Give permissions
- **REVOKE**: Remove permissions

**5. Transaction Control Language (TCL)**
- **COMMIT**: Save changes
- **ROLLBACK**: Undo changes
- **SAVEPOINT**: Create checkpoint

### Sample Database Schema

We'll use this schema throughout the guide:

```sql
-- Departments table
CREATE TABLE Department (
    DeptID INT PRIMARY KEY,
    DeptName VARCHAR(50) NOT NULL,
    Location VARCHAR(50),
    Budget DECIMAL(12,2)
);

-- Employees table
CREATE TABLE Employee (
    EmpID INT PRIMARY KEY,
    FirstName VARCHAR(30) NOT NULL,
    LastName VARCHAR(30) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    Phone VARCHAR(15),
    HireDate DATE,
    Salary DECIMAL(10,2),
    DeptID INT,
    ManagerID INT,
    FOREIGN KEY (DeptID) REFERENCES Department(DeptID),
    FOREIGN KEY (ManagerID) REFERENCES Employee(EmpID)
);

-- Projects table
CREATE TABLE Project (
    ProjectID INT PRIMARY KEY,
    ProjectName VARCHAR(100) NOT NULL,
    StartDate DATE,
    EndDate DATE,
    Budget DECIMAL(12,2),
    Status VARCHAR(20) DEFAULT 'Active'
);

-- Employee-Project assignment table
CREATE TABLE Assignment (
    EmpID INT,
    ProjectID INT,
    Role VARCHAR(50),
    HoursWorked DECIMAL(5,2),
    AssignDate DATE,
    PRIMARY KEY (EmpID, ProjectID),
    FOREIGN KEY (EmpID) REFERENCES Employee(EmpID),
    FOREIGN KEY (ProjectID) REFERENCES Project(ProjectID)
);

-- Customers table
CREATE TABLE Customer (
    CustomerID INT PRIMARY KEY,
    CompanyName VARCHAR(100),
    ContactName VARCHAR(50),
    City VARCHAR(50),
    Country VARCHAR(50),
    Phone VARCHAR(20)
);

-- Orders table
CREATE TABLE Orders (
    OrderID INT PRIMARY KEY,
    CustomerID INT,
    OrderDate DATE,
    ShipDate DATE,
    TotalAmount DECIMAL(10,2),
    FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID)
);
```

---

## 2. Basic SQL Structure {#basic-structure}

### SQL Syntax Rules

**Case Sensitivity**:
- **Keywords**: Not case-sensitive (SELECT = select = Select)
- **Identifiers**: Depends on database system
- **String Values**: Case-sensitive

**Statement Termination**:
- **Semicolon**: End each statement with ;
- **Optional**: In single statement execution

**Comments**:
```sql
-- Single line comment
/* Multi-line
   comment */
```

### Data Types

**Numeric Types**:
- **INT/INTEGER**: Whole numbers
- **DECIMAL(p,s)**: Fixed-point numbers
- **FLOAT/REAL**: Floating-point numbers
- **BIGINT**: Large integers

**String Types**:
- **CHAR(n)**: Fixed-length strings
- **VARCHAR(n)**: Variable-length strings
- **TEXT**: Large text data

**Date/Time Types**:
- **DATE**: Date values (YYYY-MM-DD)
- **TIME**: Time values (HH:MM:SS)
- **DATETIME/TIMESTAMP**: Date and time

**Boolean Type**:
- **BOOLEAN**: TRUE/FALSE values

---

## 3. SELECT Statement {#select-statement}

### Basic SELECT Syntax

```sql
SELECT column1, column2, ...
FROM table_name
WHERE condition
ORDER BY column
LIMIT number;
```

### Simple SELECT Examples

**Select All Columns**:
```sql
SELECT * FROM Employee;
```

**Select Specific Columns**:
```sql
SELECT FirstName, LastName, Salary 
FROM Employee;
```

**Select with Alias**:
```sql
SELECT FirstName AS 'First Name', 
       LastName AS 'Last Name',
       Salary * 12 AS 'Annual Salary'
FROM Employee;
```

### WHERE Clause

**Comparison Operators**:
```sql
-- Equal to
SELECT * FROM Employee WHERE DeptID = 1;

-- Not equal to
SELECT * FROM Employee WHERE DeptID != 1;
SELECT * FROM Employee WHERE DeptID <> 1;

-- Greater than
SELECT * FROM Employee WHERE Salary > 50000;

-- Less than or equal to
SELECT * FROM Employee WHERE Salary <= 60000;

-- Between range
SELECT * FROM Employee WHERE Salary BETWEEN 40000 AND 80000;

-- In list
SELECT * FROM Employee WHERE DeptID IN (1, 2, 3);

-- Not in list
SELECT * FROM Employee WHERE DeptID NOT IN (1, 2);
```

**String Matching**:
```sql
-- Exact match
SELECT * FROM Employee WHERE FirstName = 'John';

-- Pattern matching with LIKE
SELECT * FROM Employee WHERE FirstName LIKE 'J%';    -- Starts with J
SELECT * FROM Employee WHERE FirstName LIKE '%son';  -- Ends with son
SELECT * FROM Employee WHERE FirstName LIKE '%oh%';  -- Contains oh
SELECT * FROM Employee WHERE FirstName LIKE 'J_hn';  -- J, any char, hn

-- Case-insensitive (depends on database)
SELECT * FROM Employee WHERE UPPER(FirstName) = 'JOHN';
```

**NULL Handling**:
```sql
-- Check for NULL
SELECT * FROM Employee WHERE ManagerID IS NULL;

-- Check for NOT NULL
SELECT * FROM Employee WHERE ManagerID IS NOT NULL;

-- NULL in calculations
SELECT FirstName, Salary, COALESCE(Salary, 0) AS SafeSalary
FROM Employee;
```

### Logical Operators

**AND Operator**:
```sql
SELECT * FROM Employee 
WHERE DeptID = 1 AND Salary > 50000;
```

**OR Operator**:
```sql
SELECT * FROM Employee 
WHERE DeptID = 1 OR DeptID = 2;
```

**NOT Operator**:
```sql
SELECT * FROM Employee 
WHERE NOT (DeptID = 1);
```

**Complex Conditions**:
```sql
SELECT * FROM Employee 
WHERE (DeptID = 1 OR DeptID = 2) 
  AND Salary > 45000 
  AND HireDate >= '2020-01-01';
```

### ORDER BY Clause

**Single Column Sorting**:
```sql
-- Ascending (default)
SELECT * FROM Employee ORDER BY Salary;
SELECT * FROM Employee ORDER BY Salary ASC;

-- Descending
SELECT * FROM Employee ORDER BY Salary DESC;
```

**Multiple Column Sorting**:
```sql
SELECT * FROM Employee 
ORDER BY DeptID ASC, Salary DESC;
```

**Sorting by Expression**:
```sql
SELECT FirstName, LastName, Salary
FROM Employee 
ORDER BY Salary * 12 DESC;  -- Sort by annual salary
```

**Sorting with NULL Values**:
```sql
-- NULLs first
SELECT * FROM Employee ORDER BY ManagerID NULLS FIRST;

-- NULLs last
SELECT * FROM Employee ORDER BY ManagerID NULLS LAST;
```

### LIMIT and OFFSET

**Limit Results**:
```sql
-- Top 5 highest paid employees
SELECT * FROM Employee 
ORDER BY Salary DESC 
LIMIT 5;
```

**Pagination**:
```sql
-- Skip first 10, get next 5
SELECT * FROM Employee 
ORDER BY EmpID 
LIMIT 5 OFFSET 10;

-- Alternative syntax (MySQL)
SELECT * FROM Employee 
ORDER BY EmpID 
LIMIT 10, 5;  -- LIMIT offset, count
```

### DISTINCT

**Remove Duplicates**:
```sql
-- Unique departments
SELECT DISTINCT DeptID FROM Employee;

-- Unique combinations
SELECT DISTINCT DeptID, ManagerID FROM Employee;
```

**Count Distinct**:
```sql
SELECT COUNT(DISTINCT DeptID) AS UniqueDepartments
FROM Employee;
```

### Calculated Fields

**Arithmetic Operations**:
```sql
SELECT FirstName, LastName, 
       Salary,
       Salary * 12 AS AnnualSalary,
       Salary * 1.1 AS SalaryWithRaise,
       Salary / 12 AS MonthlySalary
FROM Employee;
```

**String Operations**:
```sql
SELECT FirstName, LastName,
       CONCAT(FirstName, ' ', LastName) AS FullName,
       UPPER(FirstName) AS UpperFirst,
       LOWER(LastName) AS LowerLast,
       LENGTH(FirstName) AS NameLength
FROM Employee;
```

**Date Operations**:
```sql
SELECT FirstName, HireDate,
       YEAR(HireDate) AS HireYear,
       MONTH(HireDate) AS HireMonth,
       DATEDIFF(CURDATE(), HireDate) AS DaysEmployed,
       DATE_ADD(HireDate, INTERVAL 1 YEAR) AS FirstAnniversary
FROM Employee;
```

### Conditional Logic

**CASE Statement**:
```sql
SELECT FirstName, LastName, Salary,
       CASE 
           WHEN Salary >= 80000 THEN 'High'
           WHEN Salary >= 50000 THEN 'Medium'
           ELSE 'Low'
       END AS SalaryCategory
FROM Employee;
```

**Simple CASE**:
```sql
SELECT FirstName, DeptID,
       CASE DeptID
           WHEN 1 THEN 'Engineering'
           WHEN 2 THEN 'Sales'
           WHEN 3 THEN 'Marketing'
           ELSE 'Other'
       END AS DepartmentName
FROM Employee;
```

**IF Function** (MySQL):
```sql
SELECT FirstName, Salary,
       IF(Salary > 60000, 'High Earner', 'Regular') AS Category
FROM Employee;
```

---

## 4. Data Manipulation Language (DML) {#dml}

### INSERT Statement

**Basic INSERT**:
```sql
INSERT INTO Department (DeptID, DeptName, Location, Budget)
VALUES (1, 'Engineering', 'New York', 500000.00);
```

**Multiple Row INSERT**:
```sql
INSERT INTO Department (DeptID, DeptName, Location, Budget)
VALUES 
    (2, 'Sales', 'Chicago', 300000.00),
    (3, 'Marketing', 'Los Angeles', 250000.00),
    (4, 'HR', 'New York', 150000.00);
```

**INSERT with SELECT**:
```sql
-- Copy high-salary employees to a backup table
INSERT INTO HighEarners (EmpID, FirstName, LastName, Salary)
SELECT EmpID, FirstName, LastName, Salary
FROM Employee
WHERE Salary > 75000;
```

**INSERT with Default Values**:
```sql
INSERT INTO Project (ProjectID, ProjectName, StartDate)
VALUES (1, 'Website Redesign', '2024-01-15');
-- Status will use default value 'Active'
```

### UPDATE Statement

**Basic UPDATE**:
```sql
UPDATE Employee 
SET Salary = 65000 
WHERE EmpID = 1;
```

**Multiple Column UPDATE**:
```sql
UPDATE Employee 
SET Salary = Salary * 1.1,
    Email = 'john.doe@company.com'
WHERE EmpID = 1;
```

**Conditional UPDATE**:
```sql
UPDATE Employee 
SET Salary = CASE 
    WHEN DeptID = 1 THEN Salary * 1.15  -- Engineering 15% raise
    WHEN DeptID = 2 THEN Salary * 1.10  -- Sales 10% raise
    ELSE Salary * 1.05                  -- Others 5% raise
END;
```

**UPDATE with JOIN**:
```sql
UPDATE Employee e
JOIN Department d ON e.DeptID = d.DeptID
SET e.Salary = e.Salary * 1.1
WHERE d.DeptName = 'Engineering';
```

**UPDATE with Subquery**:
```sql
UPDATE Employee 
SET Salary = Salary * 1.2
WHERE DeptID IN (
    SELECT DeptID 
    FROM Department 
    WHERE Budget > 400000
);
```

### DELETE Statement

**Basic DELETE**:
```sql
DELETE FROM Employee 
WHERE EmpID = 1;
```

**Conditional DELETE**:
```sql
DELETE FROM Employee 
WHERE HireDate < '2020-01-01' 
  AND Salary < 40000;
```

**DELETE with JOIN**:
```sql
DELETE e
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID
WHERE d.DeptName = 'Temp Department';
```

**DELETE with Subquery**:
```sql
DELETE FROM Employee 
WHERE DeptID IN (
    SELECT DeptID 
    FROM Department 
    WHERE Budget < 100000
);
```

**Safe DELETE Practices**:
```sql
-- Always test with SELECT first
SELECT * FROM Employee 
WHERE HireDate < '2020-01-01';

-- Then perform DELETE
DELETE FROM Employee 
WHERE HireDate < '2020-01-01';
```

### TRUNCATE Statement

**TRUNCATE vs DELETE**:

**TRUNCATE**:
```sql
TRUNCATE TABLE Employee;
```

**Characteristics**:
- **Faster**: No row-by-row processing
- **No WHERE**: Removes all rows
- **Reset Identity**: Auto-increment resets
- **No Triggers**: Doesn't fire DELETE triggers
- **No Rollback**: Cannot be rolled back (in some databases)

**DELETE**:
```sql
DELETE FROM Employee;
```

**Characteristics**:
- **Slower**: Row-by-row processing
- **Conditional**: Can use WHERE clause
- **Preserve Identity**: Auto-increment continues
- **Triggers**: Fires DELETE triggers
- **Rollback**: Can be rolled back

**When to Use Each**:
- **TRUNCATE**: Remove all data quickly, reset table
- **DELETE**: Remove specific rows, need transaction control

---

## 5. Data Definition Language (DDL) {#ddl}

### CREATE Statement

**Create Database**:
```sql
CREATE DATABASE CompanyDB;
USE CompanyDB;
```

**Create Table**:
```sql
CREATE TABLE Employee (
    EmpID INT PRIMARY KEY AUTO_INCREMENT,
    FirstName VARCHAR(30) NOT NULL,
    LastName VARCHAR(30) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    HireDate DATE DEFAULT CURDATE(),
    Salary DECIMAL(10,2) CHECK (Salary > 0),
    DeptID INT,
    INDEX idx_dept (DeptID),
    FOREIGN KEY (DeptID) REFERENCES Department(DeptID)
);
```

**Create Index**:
```sql
-- Single column index
CREATE INDEX idx_employee_lastname ON Employee(LastName);

-- Composite index
CREATE INDEX idx_employee_name ON Employee(FirstName, LastName);

-- Unique index
CREATE UNIQUE INDEX idx_employee_email ON Employee(Email);
```

**Create View**:
```sql
CREATE VIEW EmployeeDetails AS
SELECT e.FirstName, e.LastName, e.Salary, d.DeptName
FROM Employee e
JOIN Department d ON e.DeptID = d.DeptID;
```

### ALTER Statement

**Add Column**:
```sql
ALTER TABLE Employee 
ADD COLUMN BirthDate DATE;

ALTER TABLE Employee 
ADD COLUMN (
    Phone VARCHAR(15),
    Address TEXT
);
```

**Modify Column**:
```sql
-- Change data type
ALTER TABLE Employee 
MODIFY COLUMN Salary DECIMAL(12,2);

-- Change column name and type
ALTER TABLE Employee 
CHANGE COLUMN Phone PhoneNumber VARCHAR(20);
```

**Drop Column**:
```sql
ALTER TABLE Employee 
DROP COLUMN BirthDate;
```

**Add Constraints**:
```sql
-- Add primary key
ALTER TABLE Employee 
ADD PRIMARY KEY (EmpID);

-- Add foreign key
ALTER TABLE Employee 
ADD CONSTRAINT fk_employee_dept 
FOREIGN KEY (DeptID) REFERENCES Department(DeptID);

-- Add check constraint
ALTER TABLE Employee 
ADD CONSTRAINT chk_salary CHECK (Salary > 0);

-- Add unique constraint
ALTER TABLE Employee 
ADD CONSTRAINT uk_employee_email UNIQUE (Email);
```

**Drop Constraints**:
```sql
ALTER TABLE Employee 
DROP CONSTRAINT fk_employee_dept;

ALTER TABLE Employee 
DROP INDEX idx_employee_lastname;
```

**Rename Table**:
```sql
ALTER TABLE Employee 
RENAME TO Staff;

-- Or
RENAME TABLE Employee TO Staff;
```

### DROP Statement

**Drop Table**:
```sql
DROP TABLE Employee;

-- Drop if exists (safe)
DROP TABLE IF EXISTS Employee;
```

**Drop Database**:
```sql
DROP DATABASE CompanyDB;

-- Drop if exists (safe)
DROP DATABASE IF EXISTS CompanyDB;
```

**Drop Index**:
```sql
DROP INDEX idx_employee_lastname ON Employee;
```

**Drop View**:
```sql
DROP VIEW EmployeeDetails;
```

### Common DDL Patterns

**Create Table with All Constraints**:
```sql
CREATE TABLE Employee (
    EmpID INT AUTO_INCREMENT,
    FirstName VARCHAR(30) NOT NULL,
    LastName VARCHAR(30) NOT NULL,
    Email VARCHAR(100),
    HireDate DATE DEFAULT CURDATE(),
    Salary DECIMAL(10,2),
    DeptID INT,
    ManagerID INT,
    
    -- Primary key
    PRIMARY KEY (EmpID),
    
    -- Unique constraints
    UNIQUE KEY uk_email (Email),
    
    -- Check constraints
    CHECK (Salary > 0),
    CHECK (HireDate <= CURDATE()),
    
    -- Foreign keys
    FOREIGN KEY (DeptID) REFERENCES Department(DeptID)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    FOREIGN KEY (ManagerID) REFERENCES Employee(EmpID)
        ON DELETE SET NULL,
    
    -- Indexes
    INDEX idx_dept (DeptID),
    INDEX idx_manager (ManagerID),
    INDEX idx_hire_date (HireDate)
);
```

**Temporary Tables**:
```sql
-- Session-specific temporary table
CREATE TEMPORARY TABLE TempEmployee (
    EmpID INT,
    FirstName VARCHAR(30),
    Salary DECIMAL(10,2)
);

-- Automatically dropped when session ends
```

**Table from Query**:
```sql
-- Create table with data
CREATE TABLE HighEarners AS
SELECT * FROM Employee 
WHERE Salary > 75000;

-- Create empty table with same structure
CREATE TABLE EmployeeBackup 
LIKE Employee;
```

---

*Continue to Part 2 for Views, Joins, Aggregate Functions, Set Operations, and Advanced Queries*