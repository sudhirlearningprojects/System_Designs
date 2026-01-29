# SQL Interview Problems - All Levels

Real-world SQL problems asked at top tech companies (Google, Amazon, Meta, Netflix, Uber).

## 📋 Problems by Level

### Junior Developer (0-2 years)
1. [Employee Salary Report](Junior_Employee_Salary.md) - Basic SELECT, WHERE, ORDER BY
2. [Customer Orders Count](Junior_Customer_Orders.md) - GROUP BY, COUNT, HAVING
3. [Product Inventory](Junior_Product_Inventory.md) - JOINs, Basic aggregations
4. [Active Users](Junior_Active_Users.md) - Date functions, filtering
5. [Department Statistics](Junior_Department_Stats.md) - GROUP BY with multiple columns

### Mid-Level Developer (2-5 years)
6. [Second Highest Salary](Mid_Second_Highest.md) - Subqueries, LIMIT/OFFSET
7. [Consecutive Login Days](Mid_Consecutive_Days.md) - Window functions, LAG/LEAD
8. [Employee Manager Hierarchy](Mid_Manager_Hierarchy.md) - Self joins, recursive CTEs
9. [Monthly Revenue Growth](Mid_Revenue_Growth.md) - Window functions, date calculations
10. [Top N per Category](Mid_Top_N_Category.md) - ROW_NUMBER, PARTITION BY

### Senior Developer (5+ years)
11. [Median Salary by Department](Senior_Median_Salary.md) - Advanced window functions
12. [User Retention Cohort](Senior_Retention_Cohort.md) - Complex date logic, cohort analysis
13. [Fraud Detection](Senior_Fraud_Detection.md) - Pattern matching, complex conditions
14. [Running Total with Reset](Senior_Running_Total.md) - Advanced window functions
15. [Graph Traversal in SQL](Senior_Graph_Traversal.md) - Recursive CTEs, hierarchies

### Staff/Principal Engineer
16. [Query Optimization](Staff_Query_Optimization.md) - Explain plans, indexing strategies
17. [Data Warehouse Design](Staff_DWH_Design.md) - Star schema, dimensional modeling
18. [Streaming Aggregations](Staff_Streaming_Agg.md) - Time-series, sliding windows
19. [Distributed Query Planning](Staff_Distributed.md) - Sharding, partitioning
20. [Real-time Analytics](Staff_Realtime_Analytics.md) - Materialized views, incremental updates

## 🎯 Key Concepts Covered

### Basic SQL
- SELECT, WHERE, ORDER BY, LIMIT
- Aggregate functions (COUNT, SUM, AVG, MIN, MAX)
- GROUP BY, HAVING
- Basic JOINs (INNER, LEFT, RIGHT, FULL)

### Intermediate SQL
- Subqueries (correlated and non-correlated)
- Window functions (ROW_NUMBER, RANK, DENSE_RANK)
- CTEs (Common Table Expressions)
- CASE statements
- Date/Time functions

### Advanced SQL
- Recursive CTEs
- Advanced window functions (LAG, LEAD, FIRST_VALUE, LAST_VALUE)
- Pivot/Unpivot
- JSON functions
- Full-text search

### Performance & Optimization
- Index strategies (B-tree, Hash, Bitmap)
- Query execution plans
- Partitioning strategies
- Materialized views
- Query rewriting

## 📚 Database Systems

Problems include solutions for:
- **PostgreSQL** (primary)
- **MySQL**
- **SQL Server**
- **Oracle**
- **SQLite**

## 🏢 Company-Specific Patterns

### FAANG Companies
- **Google**: Complex aggregations, window functions
- **Amazon**: Inventory management, time-series
- **Meta**: Social graph queries, user engagement
- **Netflix**: Recommendation systems, viewing patterns
- **Apple**: Privacy-focused queries, data masking

### Unicorns
- **Uber**: Geospatial queries, ride matching
- **Airbnb**: Booking systems, availability
- **Stripe**: Payment reconciliation, fraud detection
- **Databricks**: Large-scale analytics, optimization

## 🎓 Learning Path

1. **Week 1-2**: Master basic SELECT, JOINs, GROUP BY
2. **Week 3-4**: Learn subqueries and CTEs
3. **Week 5-6**: Window functions and advanced aggregations
4. **Week 7-8**: Recursive queries and optimization
5. **Week 9+**: Real-world problem solving and system design

## 💡 Interview Tips

### Preparation
1. Understand the schema before writing queries
2. Start with simple queries, then optimize
3. Consider edge cases (NULL values, empty results)
4. Think about performance implications

### During Interview
1. Clarify requirements and constraints
2. Explain your thought process
3. Write readable, formatted SQL
4. Discuss trade-offs and alternatives
5. Mention indexing strategies

### Common Mistakes to Avoid
- Forgetting NULL handling
- Not considering duplicates
- Inefficient subqueries
- Missing edge cases
- Poor query formatting

## 📊 Schema Conventions

All problems use consistent naming:
- Tables: `snake_case` (e.g., `user_orders`)
- Columns: `snake_case` (e.g., `created_at`)
- Primary keys: `id` or `{table}_id`
- Foreign keys: `{referenced_table}_id`
- Timestamps: `created_at`, `updated_at`

## 🔧 Setup Instructions

### PostgreSQL
```sql
-- Install PostgreSQL
brew install postgresql

-- Start service
brew services start postgresql

-- Create database
createdb interview_practice

-- Connect
psql interview_practice
```

### MySQL
```sql
-- Install MySQL
brew install mysql

-- Start service
brew services start mysql

-- Create database
mysql -u root -e "CREATE DATABASE interview_practice;"
```

## 📝 Problem Format

Each problem includes:
- **Problem Statement** with real-world context
- **Schema Definition** with sample data
- **Expected Output** with examples
- **Multiple Solutions** (basic to optimal)
- **Complexity Analysis** (time/space)
- **Interview Follow-ups**
- **Production Considerations**

---

**Note**: Practice on [LeetCode](https://leetcode.com/problemset/database/), [HackerRank](https://www.hackerrank.com/domains/sql), and [SQLZoo](https://sqlzoo.net/) for additional problems.
