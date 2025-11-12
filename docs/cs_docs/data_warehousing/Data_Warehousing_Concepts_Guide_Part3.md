# Data Warehousing Concepts Guide - Part 3

*Data Marts, Data Models, and Practice Questions*

## Table of Contents (Part 3)
10. [Data Mart](#data-mart)
11. [Data Models](#data-models)
12. [MCQ Practice Questions](#mcq-questions)

---

## 10. Data Mart {#data-mart}

### Data Mart Overview

**Definition**: A data mart is a subset of a data warehouse focused on a specific business area, department, or subject.

**Types of Data Marts**:
1. **Dependent Data Mart**: Created from existing data warehouse
2. **Independent Data Mart**: Built directly from operational sources
3. **Hybrid Data Mart**: Combines data from warehouse and operational sources

### Data Mart Implementation

```python
import pandas as pd
import sqlite3
from datetime import datetime

class DataMart:
    def __init__(self, mart_name, subject_area):
        self.mart_name = mart_name
        self.subject_area = subject_area
        self.conn = sqlite3.connect(f"{mart_name}_mart.db")
        self.tables = {}
        
    def create_sales_mart(self, warehouse_conn):
        """Create sales-focused data mart"""
        
        # Extract sales-related dimensions
        customers = pd.read_sql("""
            SELECT customer_id, customer_name, region, segment
            FROM dim_customer
        """, warehouse_conn)
        
        products = pd.read_sql("""
            SELECT product_id, product_name, category, subcategory
            FROM dim_product
        """, warehouse_conn)
        
        # Extract sales facts
        sales_facts = pd.read_sql("""
            SELECT f.*, d.year, d.quarter, d.month
            FROM fact_sales f
            JOIN dim_date d ON f.date_key = d.date_key
            WHERE d.year >= 2023
        """, warehouse_conn)
        
        # Load to mart
        customers.to_sql('dim_customer', self.conn, if_exists='replace', index=False)
        products.to_sql('dim_product', self.conn, if_exists='replace', index=False)
        sales_facts.to_sql('fact_sales', self.conn, if_exists='replace', index=False)
        
        self.tables = {'dim_customer': customers, 'dim_product': products, 'fact_sales': sales_facts}
        
    def create_aggregated_views(self):
        """Create pre-aggregated views for performance"""
        
        # Monthly sales summary
        self.conn.execute("""
            CREATE VIEW IF NOT EXISTS monthly_sales AS
            SELECT 
                year, month,
                SUM(sales_amount) as total_sales,
                SUM(quantity) as total_quantity,
                COUNT(*) as transaction_count
            FROM fact_sales
            GROUP BY year, month
        """)
        
        # Product performance
        self.conn.execute("""
            CREATE VIEW IF NOT EXISTS product_performance AS
            SELECT 
                p.category,
                p.product_name,
                SUM(f.sales_amount) as total_sales,
                AVG(f.profit_margin) as avg_margin
            FROM fact_sales f
            JOIN dim_product p ON f.product_id = p.product_id
            GROUP BY p.category, p.product_name
        """)
        
    def get_mart_metrics(self):
        """Get data mart performance metrics"""
        metrics = {}
        
        for table_name in self.tables.keys():
            cursor = self.conn.execute(f"SELECT COUNT(*) FROM {table_name}")
            metrics[f"{table_name}_count"] = cursor.fetchone()[0]
            
        return metrics
```

---

## 11. Data Models {#data-models}

### Star Schema

```python
class StarSchema:
    def __init__(self):
        self.fact_table = None
        self.dimension_tables = {}
        
    def create_sales_star_schema(self):
        """Create star schema for sales analysis"""
        
        # Fact table
        fact_sales = {
            'sales_key': 'PRIMARY KEY',
            'date_key': 'FOREIGN KEY -> dim_date',
            'customer_key': 'FOREIGN KEY -> dim_customer', 
            'product_key': 'FOREIGN KEY -> dim_product',
            'sales_amount': 'MEASURE',
            'quantity': 'MEASURE',
            'discount': 'MEASURE'
        }
        
        # Dimension tables
        dim_date = {
            'date_key': 'PRIMARY KEY',
            'full_date': 'DATE',
            'year': 'INTEGER',
            'quarter': 'INTEGER',
            'month': 'INTEGER',
            'day': 'INTEGER'
        }
        
        dim_customer = {
            'customer_key': 'PRIMARY KEY',
            'customer_id': 'BUSINESS KEY',
            'customer_name': 'VARCHAR(100)',
            'region': 'VARCHAR(50)',
            'segment': 'VARCHAR(50)'
        }
        
        dim_product = {
            'product_key': 'PRIMARY KEY',
            'product_id': 'BUSINESS KEY',
            'product_name': 'VARCHAR(100)',
            'category': 'VARCHAR(50)',
            'subcategory': 'VARCHAR(50)'
        }
        
        return {
            'fact_sales': fact_sales,
            'dim_date': dim_date,
            'dim_customer': dim_customer,
            'dim_product': dim_product
        }
```

### Snowflake Schema

```python
class SnowflakeSchema:
    def create_normalized_dimensions(self):
        """Create normalized dimension tables"""
        
        # Customer dimension - normalized
        dim_customer = {
            'customer_key': 'PRIMARY KEY',
            'customer_id': 'BUSINESS KEY',
            'customer_name': 'VARCHAR(100)',
            'region_key': 'FOREIGN KEY -> dim_region'
        }
        
        dim_region = {
            'region_key': 'PRIMARY KEY',
            'region_name': 'VARCHAR(50)',
            'country_key': 'FOREIGN KEY -> dim_country'
        }
        
        dim_country = {
            'country_key': 'PRIMARY KEY',
            'country_name': 'VARCHAR(50)',
            'continent': 'VARCHAR(50)'
        }
        
        return {
            'dim_customer': dim_customer,
            'dim_region': dim_region,
            'dim_country': dim_country
        }
```

### Galaxy Schema

```python
class GalaxySchema:
    def create_multiple_fact_tables(self):
        """Create galaxy schema with multiple fact tables"""
        
        # Sales fact table
        fact_sales = {
            'sales_key': 'PRIMARY KEY',
            'date_key': 'FOREIGN KEY',
            'customer_key': 'FOREIGN KEY',
            'product_key': 'FOREIGN KEY',
            'sales_amount': 'MEASURE'
        }
        
        # Inventory fact table
        fact_inventory = {
            'inventory_key': 'PRIMARY KEY',
            'date_key': 'FOREIGN KEY',
            'product_key': 'FOREIGN KEY',
            'warehouse_key': 'FOREIGN KEY',
            'quantity_on_hand': 'MEASURE'
        }
        
        # Shared dimensions
        dim_date = {'date_key': 'PRIMARY KEY', 'full_date': 'DATE'}
        dim_product = {'product_key': 'PRIMARY KEY', 'product_name': 'VARCHAR'}
        
        return {
            'fact_sales': fact_sales,
            'fact_inventory': fact_inventory,
            'dim_date': dim_date,
            'dim_product': dim_product
        }
```

---

## 12. MCQ Practice Questions {#mcq-questions}

### Questions 1-10: Fundamentals

**1. What is the primary purpose of a data warehouse?**
a) Real-time transaction processing
b) Historical data analysis and reporting
c) Data entry and validation
d) System backup and recovery

**Answer: b) Historical data analysis and reporting**
*Explanation: Data warehouses are designed for analytical processing of historical data, not operational transactions.*

**2. Which characteristic is NOT typical of a data warehouse?**
a) Subject-oriented
b) Integrated
c) Volatile
d) Time-variant

**Answer: c) Volatile**
*Explanation: Data warehouses are non-volatile - data is stable once loaded and not frequently updated.*

**3. In ETL process, what does the 'T' stand for?**
a) Transfer
b) Transform
c) Translate
d) Transpose

**Answer: b) Transform**
*Explanation: ETL stands for Extract, Transform, Load - Transform involves data cleaning and conversion.*

**4. What is the main difference between OLTP and OLAP?**
a) OLTP is for analysis, OLAP is for transactions
b) OLTP is for transactions, OLAP is for analysis
c) Both are used for the same purpose
d) OLTP is newer than OLAP

**Answer: b) OLTP is for transactions, OLAP is for analysis**
*Explanation: OLTP handles operational transactions, OLAP handles analytical processing.*

**5. Which data cleaning technique handles missing values?**
a) Normalization
b) Imputation
c) Aggregation
d) Indexing

**Answer: b) Imputation**
*Explanation: Imputation involves filling missing values with estimated or calculated values.*

**6. What is a fact table in dimensional modeling?**
a) A table containing dimension attributes
b) A table containing measurable business events
c) A table containing metadata
d) A table containing lookup values

**Answer: b) A table containing measurable business events**
*Explanation: Fact tables store quantitative measures of business processes.*

**7. In a star schema, how are dimension tables related to the fact table?**
a) Many-to-many relationship
b) One-to-one relationship
c) Dimension tables are not related
d) Many-to-one relationship

**Answer: d) Many-to-one relationship**
*Explanation: Multiple fact records can relate to one dimension record.*

**8. What is the purpose of a staging area in ETL?**
a) Final data storage
b) Temporary data storage during processing
c) User interface display
d) Backup storage

**Answer: b) Temporary data storage during processing**
*Explanation: Staging areas hold data temporarily during ETL processing.*

**9. Which type of data mart is created from an existing data warehouse?**
a) Independent data mart
b) Dependent data mart
c) Hybrid data mart
d) Operational data mart

**Answer: b) Dependent data mart**
*Explanation: Dependent data marts are subsets created from existing data warehouses.*

**10. What is metadata in data warehousing context?**
a) Data about data
b) Actual business data
c) Backup data
d) Temporary data

**Answer: a) Data about data**
*Explanation: Metadata provides information about data structure, lineage, and definitions.*

### Questions 11-20: Advanced Concepts

**11. Which OLAP operation aggregates data to a higher level?**
a) Drill-down
b) Roll-up
c) Slice
d) Dice

**Answer: b) Roll-up**
*Explanation: Roll-up aggregates data from detailed to summary level.*

**12. What is the main advantage of a snowflake schema over a star schema?**
a) Better query performance
b) Reduced storage space
c) Simpler design
d) Faster loading

**Answer: b) Reduced storage space**
*Explanation: Normalization in snowflake schema reduces data redundancy and storage.*

**13. In SCD Type 2, what happens when a dimension attribute changes?**
a) Update the existing record
b) Create a new record and keep the old one
c) Delete the old record
d) Ignore the change

**Answer: b) Create a new record and keep the old one**
*Explanation: SCD Type 2 maintains history by creating new records for changes.*

**14. What is data lineage?**
a) The age of data
b) The path data takes from source to destination
c) The size of data
d) The quality of data

**Answer: b) The path data takes from source to destination**
*Explanation: Data lineage tracks data flow and transformations through systems.*

**15. Which loading strategy is most efficient for large datasets?**
a) Full load
b) Incremental load
c) Real-time load
d) Batch load

**Answer: b) Incremental load**
*Explanation: Incremental loading processes only new/changed data, making it efficient.*

**16. What is the purpose of a data cube?**
a) Data storage
b) Multi-dimensional analysis
c) Data backup
d) Data entry

**Answer: b) Multi-dimensional analysis**
*Explanation: Data cubes enable analysis across multiple dimensions simultaneously.*

**17. Which component manages data warehouse metadata?**
a) ETL tool
b) OLAP server
c) Metadata repository
d) Data mart

**Answer: c) Metadata repository**
*Explanation: Metadata repositories store and manage information about data structures and processes.*

**18. What is the main characteristic of a conformed dimension?**
a) Used in only one fact table
b) Used across multiple fact tables with consistent meaning
c) Contains only numeric data
d) Changes frequently

**Answer: b) Used across multiple fact tables with consistent meaning**
*Explanation: Conformed dimensions ensure consistency across different business processes.*

**19. Which technique is used for handling slowly changing dimensions?**
a) Normalization
b) Denormalization
c) SCD strategies (Type 1, 2, 3)
d) Indexing

**Answer: c) SCD strategies (Type 1, 2, 3)**
*Explanation: SCD strategies manage how dimension changes are handled over time.*

**20. What is the primary benefit of data partitioning in a data warehouse?**
a) Improved data quality
b) Better query performance
c) Reduced storage cost
d) Enhanced security

**Answer: b) Better query performance**
*Explanation: Partitioning improves performance by allowing parallel processing and reducing scan time.*

### Study Tips

**Key Areas to Focus:**
1. **ETL Processes**: Understand extraction methods, transformation techniques, and loading strategies
2. **Dimensional Modeling**: Master star, snowflake, and galaxy schemas
3. **OLAP Operations**: Know slice, dice, roll-up, drill-down, and pivot operations
4. **Data Quality**: Understand profiling, cleansing, and validation techniques
5. **Metadata Management**: Know types and importance of metadata
6. **Performance Optimization**: Understand indexing, partitioning, and aggregation strategies

**Exam Preparation Strategy:**
- Practice dimensional modeling with real scenarios
- Understand the trade-offs between different schema designs
- Know when to use different ETL strategies
- Memorize OLAP operation definitions and examples
- Understand SCD types and their applications

---

## Summary

This comprehensive Data Warehousing guide covers:

**Part 1**: Introduction, Architecture, Data Extraction, Cleaning, and Transformation
**Part 2**: Data Loading, Metadata Management, and Data Cubes  
**Part 3**: Data Marts, Data Models, and Practice Questions

**Key Takeaways:**
- Data warehousing enables historical analysis and business intelligence
- ETL processes are crucial for data integration and quality
- Dimensional modeling provides intuitive data structures for analysis
- OLAP operations enable flexible multi-dimensional analysis
- Metadata management ensures data governance and lineage
- Data marts provide focused, departmental views of enterprise data

The guide provides both theoretical understanding and practical implementation examples suitable for freshers and exam preparation.