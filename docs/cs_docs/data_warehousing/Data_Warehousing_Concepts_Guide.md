# Data Warehousing Concepts Guide - Part 1

*Comprehensive guide covering ETL processes, data modeling, and warehousing fundamentals*

## Table of Contents (Part 1)
1. [Introduction to Data Warehousing](#introduction)
2. [Data Warehousing Architecture](#architecture)
3. [Data Extraction](#data-extraction)
4. [Data Cleaning](#data-cleaning)
5. [Data Transformation](#data-transformation)

---

## 1. Introduction to Data Warehousing {#introduction}

### What is Data Warehousing?

**Definition**: A data warehouse is a centralized repository that stores integrated data from multiple sources, optimized for analysis and reporting rather than transaction processing.

**Key Characteristics**:
- **Subject-Oriented**: Organized around business subjects (sales, inventory, customers)
- **Integrated**: Data from multiple sources combined consistently
- **Time-Variant**: Historical data preserved with timestamps
- **Non-Volatile**: Data is stable once loaded, not frequently updated

### OLTP vs OLAP Comparison

| Aspect | OLTP (Online Transaction Processing) | OLAP (Online Analytical Processing) |
|--------|-------------------------------------|-------------------------------------|
| Purpose | Day-to-day operations | Analysis and reporting |
| Data Type | Current, detailed | Historical, summarized |
| Queries | Simple, frequent | Complex, ad-hoc |
| Users | Many concurrent users | Fewer analytical users |
| Response Time | Milliseconds | Seconds to minutes |
| Database Design | Normalized (3NF) | Denormalized (Star/Snowflake) |

### Benefits of Data Warehousing

1. **Improved Decision Making**: Historical trends and patterns
2. **Data Integration**: Single source of truth
3. **Performance**: Optimized for analytical queries
4. **Data Quality**: Cleaned and validated data
5. **Historical Analysis**: Time-series data preservation

---

## 2. Data Warehousing Architecture {#architecture}

### Three-Tier Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Data Sources  │    │  Data Warehouse │    │  Presentation   │
│                 │    │                 │    │     Layer       │
│ • OLTP Systems  │───▶│ • ETL Process   │───▶│ • Reports       │
│ • External Data │    │ • Data Storage  │    │ • Dashboards    │
│ • Files/APIs    │    │ • Metadata      │    │ • OLAP Tools    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Components Explained

**1. Data Sources Layer**:
- Operational databases (CRM, ERP, HR systems)
- External data (market research, social media)
- Files (CSV, XML, JSON)
- Real-time streams

**2. Data Integration Layer**:
- ETL (Extract, Transform, Load) processes
- Data staging areas
- Data quality tools
- Metadata management

**3. Data Storage Layer**:
- Data warehouse database
- Data marts (departmental subsets)
- Operational data store (ODS)
- Archive storage

**4. Presentation Layer**:
- Business intelligence tools
- Reporting applications
- OLAP cubes
- Data mining tools

### Data Warehouse Schemas

**Star Schema**:
```
        ┌─────────────┐
        │   Product   │
        │ Dimension   │
        └──────┬──────┘
               │
┌──────────────┼──────────────┐
│              │              │
▼              ▼              ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│  Time   │ │  Sales  │ │Customer │
│Dimension│ │  Fact   │ │Dimension│
└─────────┘ │ Table   │ └─────────┘
            └─────────┘
```

**Snowflake Schema**:
- Normalized dimension tables
- Reduces storage but increases complexity
- Multiple levels of dimension tables

---

## 3. Data Extraction {#data-extraction}

### Extraction Methods

**1. Full Extraction**:
```python
import pandas as pd
import sqlite3
from datetime import datetime

class DataExtractor:
    def __init__(self, source_db_path, target_db_path):
        self.source_conn = sqlite3.connect(source_db_path)
        self.target_conn = sqlite3.connect(target_db_path)
        self.extraction_log = []
    
    def full_extraction(self, table_name):
        """Extract all data from source table"""
        try:
            # Extract all records
            query = f"SELECT * FROM {table_name}"
            df = pd.read_sql_query(query, self.source_conn)
            
            # Add extraction metadata
            df['extraction_date'] = datetime.now()
            df['extraction_type'] = 'FULL'
            
            # Load to staging
            df.to_sql(f"staging_{table_name}", self.target_conn, 
                     if_exists='replace', index=False)
            
            self.log_extraction(table_name, 'FULL', len(df), 'SUCCESS')
            return df
            
        except Exception as e:
            self.log_extraction(table_name, 'FULL', 0, f'ERROR: {str(e)}')
            raise
    
    def incremental_extraction(self, table_name, timestamp_column, last_extract_time):
        """Extract only new/modified records"""
        try:
            query = f"""
            SELECT * FROM {table_name} 
            WHERE {timestamp_column} > '{last_extract_time}'
            """
            
            df = pd.read_sql_query(query, self.source_conn)
            
            if not df.empty:
                df['extraction_date'] = datetime.now()
                df['extraction_type'] = 'INCREMENTAL'
                
                # Append to staging
                df.to_sql(f"staging_{table_name}", self.target_conn, 
                         if_exists='append', index=False)
            
            self.log_extraction(table_name, 'INCREMENTAL', len(df), 'SUCCESS')
            return df
            
        except Exception as e:
            self.log_extraction(table_name, 'INCREMENTAL', 0, f'ERROR: {str(e)}')
            raise
    
    def change_data_capture(self, table_name):
        """Extract using CDC (Change Data Capture)"""
        try:
            # Simulate CDC by checking change log table
            query = f"""
            SELECT t.*, cl.operation_type, cl.change_timestamp
            FROM {table_name} t
            JOIN change_log cl ON t.id = cl.record_id
            WHERE cl.processed = 0
            """
            
            df = pd.read_sql_query(query, self.source_conn)
            
            if not df.empty:
                df['extraction_date'] = datetime.now()
                df['extraction_type'] = 'CDC'
                
                df.to_sql(f"staging_{table_name}", self.target_conn, 
                         if_exists='append', index=False)
                
                # Mark as processed
                record_ids = df['id'].tolist()
                placeholders = ','.join(['?' for _ in record_ids])
                update_query = f"""
                UPDATE change_log 
                SET processed = 1 
                WHERE record_id IN ({placeholders})
                """
                self.source_conn.execute(update_query, record_ids)
                self.source_conn.commit()
            
            self.log_extraction(table_name, 'CDC', len(df), 'SUCCESS')
            return df
            
        except Exception as e:
            self.log_extraction(table_name, 'CDC', 0, f'ERROR: {str(e)}')
            raise
    
    def log_extraction(self, table_name, extraction_type, record_count, status):
        """Log extraction activity"""
        log_entry = {
            'timestamp': datetime.now(),
            'table_name': table_name,
            'extraction_type': extraction_type,
            'record_count': record_count,
            'status': status
        }
        self.extraction_log.append(log_entry)
        
        # Store in database
        log_df = pd.DataFrame([log_entry])
        log_df.to_sql('extraction_log', self.target_conn, 
                     if_exists='append', index=False)
```

**2. Incremental Extraction**:
- Extract only new or modified records
- Uses timestamps or sequence numbers
- More efficient for large datasets

**3. Change Data Capture (CDC)**:
- Real-time or near-real-time extraction
- Captures insert, update, delete operations
- Minimal impact on source systems

### Extraction Challenges

**1. Data Volume**:
```python
class VolumeHandler:
    def __init__(self, chunk_size=10000):
        self.chunk_size = chunk_size
    
    def chunked_extraction(self, query, connection):
        """Extract large datasets in chunks"""
        offset = 0
        all_data = []
        
        while True:
            chunked_query = f"{query} LIMIT {self.chunk_size} OFFSET {offset}"
            chunk = pd.read_sql_query(chunked_query, connection)
            
            if chunk.empty:
                break
                
            all_data.append(chunk)
            offset += self.chunk_size
            
            # Optional: Process chunk immediately
            self.process_chunk(chunk)
        
        return pd.concat(all_data, ignore_index=True) if all_data else pd.DataFrame()
    
    def process_chunk(self, chunk):
        """Process individual chunk"""
        # Apply transformations
        # Load to staging
        pass
```

**2. Data Variety**:
```python
class MultiSourceExtractor:
    def extract_from_api(self, api_url, headers=None):
        """Extract from REST API"""
        import requests
        
        response = requests.get(api_url, headers=headers)
        if response.status_code == 200:
            return pd.DataFrame(response.json())
        else:
            raise Exception(f"API Error: {response.status_code}")
    
    def extract_from_csv(self, file_path):
        """Extract from CSV file"""
        return pd.read_csv(file_path)
    
    def extract_from_xml(self, file_path):
        """Extract from XML file"""
        import xml.etree.ElementTree as ET
        
        tree = ET.parse(file_path)
        root = tree.getroot()
        
        data = []
        for child in root:
            record = {}
            for elem in child:
                record[elem.tag] = elem.text
            data.append(record)
        
        return pd.DataFrame(data)
```

---

## 4. Data Cleaning {#data-cleaning}

### Data Quality Issues

**Common Problems**:
1. **Missing Values**: NULL, empty strings, default values
2. **Inconsistent Formats**: Date formats, case sensitivity
3. **Duplicates**: Exact and fuzzy duplicates
4. **Outliers**: Statistical anomalies
5. **Invalid Data**: Out-of-range values, wrong data types

### Data Cleaning Implementation

```python
import pandas as pd
import numpy as np
from datetime import datetime
import re

class DataCleaner:
    def __init__(self):
        self.cleaning_log = []
        self.quality_metrics = {}
    
    def handle_missing_values(self, df, strategy='default'):
        """Handle missing values with various strategies"""
        original_nulls = df.isnull().sum().sum()
        
        if strategy == 'drop':
            # Drop rows with any missing values
            df_cleaned = df.dropna()
        
        elif strategy == 'fill_mean':
            # Fill numeric columns with mean
            numeric_cols = df.select_dtypes(include=[np.number]).columns
            df_cleaned = df.copy()
            for col in numeric_cols:
                df_cleaned[col].fillna(df_cleaned[col].mean(), inplace=True)
        
        elif strategy == 'fill_mode':
            # Fill categorical columns with mode
            df_cleaned = df.copy()
            for col in df.columns:
                if df[col].dtype == 'object':
                    mode_value = df[col].mode()
                    if not mode_value.empty:
                        df_cleaned[col].fillna(mode_value[0], inplace=True)
        
        elif strategy == 'forward_fill':
            # Forward fill (useful for time series)
            df_cleaned = df.fillna(method='ffill')
        
        else:  # default strategy
            df_cleaned = df.copy()
            # Custom logic for different columns
            for col in df.columns:
                if df[col].dtype == 'object':
                    df_cleaned[col].fillna('Unknown', inplace=True)
                else:
                    df_cleaned[col].fillna(0, inplace=True)
        
        final_nulls = df_cleaned.isnull().sum().sum()
        
        self.log_cleaning_step('Missing Values', 
                              f'Reduced from {original_nulls} to {final_nulls} nulls')
        
        return df_cleaned
    
    def standardize_formats(self, df):
        """Standardize data formats"""
        df_cleaned = df.copy()
        
        # Standardize text columns
        text_columns = df.select_dtypes(include=['object']).columns
        for col in text_columns:
            if col.lower() in ['name', 'city', 'country', 'product_name']:
                # Title case for names
                df_cleaned[col] = df_cleaned[col].str.title()
            elif col.lower() in ['email']:
                # Lowercase for emails
                df_cleaned[col] = df_cleaned[col].str.lower()
            else:
                # Strip whitespace
                df_cleaned[col] = df_cleaned[col].str.strip()
        
        # Standardize date columns
        date_columns = ['date', 'created_at', 'updated_at', 'birth_date']
        for col in date_columns:
            if col in df.columns:
                df_cleaned[col] = pd.to_datetime(df_cleaned[col], errors='coerce')
        
        # Standardize phone numbers
        if 'phone' in df.columns:
            df_cleaned['phone'] = df_cleaned['phone'].apply(self.standardize_phone)
        
        self.log_cleaning_step('Format Standardization', 'Applied format standards')
        return df_cleaned
    
    def remove_duplicates(self, df, subset=None, keep='first'):
        """Remove duplicate records"""
        original_count = len(df)
        
        if subset:
            df_cleaned = df.drop_duplicates(subset=subset, keep=keep)
        else:
            df_cleaned = df.drop_duplicates(keep=keep)
        
        duplicates_removed = original_count - len(df_cleaned)
        
        self.log_cleaning_step('Duplicate Removal', 
                              f'Removed {duplicates_removed} duplicates')
        
        return df_cleaned
    
    def handle_outliers(self, df, method='iqr'):
        """Handle outliers in numeric columns"""
        df_cleaned = df.copy()
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        
        outliers_removed = 0
        
        for col in numeric_cols:
            if method == 'iqr':
                Q1 = df[col].quantile(0.25)
                Q3 = df[col].quantile(0.75)
                IQR = Q3 - Q1
                lower_bound = Q1 - 1.5 * IQR
                upper_bound = Q3 + 1.5 * IQR
                
                # Count outliers
                outliers = ((df[col] < lower_bound) | (df[col] > upper_bound)).sum()
                outliers_removed += outliers
                
                # Remove outliers
                df_cleaned = df_cleaned[
                    (df_cleaned[col] >= lower_bound) & 
                    (df_cleaned[col] <= upper_bound)
                ]
            
            elif method == 'zscore':
                from scipy import stats
                z_scores = np.abs(stats.zscore(df[col].dropna()))
                threshold = 3
                
                # Remove rows where z-score > threshold
                outlier_mask = z_scores > threshold
                outliers_removed += outlier_mask.sum()
                df_cleaned = df_cleaned[~outlier_mask]
        
        self.log_cleaning_step('Outlier Handling', 
                              f'Removed {outliers_removed} outliers using {method}')
        
        return df_cleaned
    
    def validate_data_types(self, df, schema):
        """Validate and convert data types"""
        df_cleaned = df.copy()
        conversion_errors = 0
        
        for col, expected_type in schema.items():
            if col in df.columns:
                try:
                    if expected_type == 'int':
                        df_cleaned[col] = pd.to_numeric(df_cleaned[col], errors='coerce').astype('Int64')
                    elif expected_type == 'float':
                        df_cleaned[col] = pd.to_numeric(df_cleaned[col], errors='coerce')
                    elif expected_type == 'datetime':
                        df_cleaned[col] = pd.to_datetime(df_cleaned[col], errors='coerce')
                    elif expected_type == 'string':
                        df_cleaned[col] = df_cleaned[col].astype(str)
                    elif expected_type == 'category':
                        df_cleaned[col] = df_cleaned[col].astype('category')
                
                except Exception as e:
                    conversion_errors += 1
                    self.log_cleaning_step('Type Conversion Error', 
                                          f'Error converting {col}: {str(e)}')
        
        self.log_cleaning_step('Data Type Validation', 
                              f'Completed with {conversion_errors} errors')
        
        return df_cleaned
    
    def standardize_phone(self, phone):
        """Standardize phone number format"""
        if pd.isna(phone):
            return phone
        
        # Remove all non-digits
        digits = re.sub(r'\D', '', str(phone))
        
        # Format as (XXX) XXX-XXXX for 10-digit US numbers
        if len(digits) == 10:
            return f"({digits[:3]}) {digits[3:6]}-{digits[6:]}"
        elif len(digits) == 11 and digits[0] == '1':
            return f"({digits[1:4]}) {digits[4:7]}-{digits[7:]}"
        else:
            return phone  # Return original if can't standardize
    
    def comprehensive_cleaning(self, df, schema=None):
        """Perform comprehensive data cleaning"""
        print("Starting comprehensive data cleaning...")
        
        # Step 1: Handle missing values
        df = self.handle_missing_values(df, strategy='default')
        
        # Step 2: Standardize formats
        df = self.standardize_formats(df)
        
        # Step 3: Remove duplicates
        df = self.remove_duplicates(df)
        
        # Step 4: Handle outliers
        df = self.handle_outliers(df, method='iqr')
        
        # Step 5: Validate data types
        if schema:
            df = self.validate_data_types(df, schema)
        
        # Calculate quality metrics
        self.calculate_quality_metrics(df)
        
        print("Data cleaning completed!")
        return df
    
    def calculate_quality_metrics(self, df):
        """Calculate data quality metrics"""
        total_cells = df.shape[0] * df.shape[1]
        null_cells = df.isnull().sum().sum()
        
        self.quality_metrics = {
            'completeness': ((total_cells - null_cells) / total_cells) * 100,
            'total_records': len(df),
            'total_columns': len(df.columns),
            'null_percentage': (null_cells / total_cells) * 100,
            'duplicate_percentage': (df.duplicated().sum() / len(df)) * 100
        }
    
    def log_cleaning_step(self, step_name, description):
        """Log cleaning steps"""
        log_entry = {
            'timestamp': datetime.now(),
            'step': step_name,
            'description': description
        }
        self.cleaning_log.append(log_entry)
    
    def get_cleaning_report(self):
        """Generate cleaning report"""
        report = {
            'cleaning_steps': self.cleaning_log,
            'quality_metrics': self.quality_metrics
        }
        return report
```

### Data Profiling

```python
class DataProfiler:
    def __init__(self):
        self.profile_results = {}
    
    def profile_dataset(self, df, dataset_name):
        """Generate comprehensive data profile"""
        profile = {
            'dataset_name': dataset_name,
            'basic_info': self.get_basic_info(df),
            'column_profiles': self.get_column_profiles(df),
            'data_quality': self.assess_data_quality(df),
            'relationships': self.analyze_relationships(df)
        }
        
        self.profile_results[dataset_name] = profile
        return profile
    
    def get_basic_info(self, df):
        """Get basic dataset information"""
        return {
            'row_count': len(df),
            'column_count': len(df.columns),
            'memory_usage': df.memory_usage(deep=True).sum(),
            'data_types': df.dtypes.value_counts().to_dict()
        }
    
    def get_column_profiles(self, df):
        """Profile each column"""
        profiles = {}
        
        for col in df.columns:
            col_profile = {
                'data_type': str(df[col].dtype),
                'null_count': df[col].isnull().sum(),
                'null_percentage': (df[col].isnull().sum() / len(df)) * 100,
                'unique_count': df[col].nunique(),
                'unique_percentage': (df[col].nunique() / len(df)) * 100
            }
            
            if df[col].dtype in ['int64', 'float64']:
                col_profile.update({
                    'min': df[col].min(),
                    'max': df[col].max(),
                    'mean': df[col].mean(),
                    'median': df[col].median(),
                    'std': df[col].std()
                })
            
            elif df[col].dtype == 'object':
                col_profile.update({
                    'most_frequent': df[col].mode().iloc[0] if not df[col].mode().empty else None,
                    'avg_length': df[col].astype(str).str.len().mean()
                })
            
            profiles[col] = col_profile
        
        return profiles
    
    def assess_data_quality(self, df):
        """Assess overall data quality"""
        total_cells = df.shape[0] * df.shape[1]
        null_cells = df.isnull().sum().sum()
        
        return {
            'completeness_score': ((total_cells - null_cells) / total_cells) * 100,
            'consistency_score': self.calculate_consistency_score(df),
            'validity_score': self.calculate_validity_score(df),
            'overall_quality_score': 0  # Will be calculated based on above scores
        }
    
    def calculate_consistency_score(self, df):
        """Calculate consistency score"""
        # Simple consistency check based on data type consistency
        consistency_issues = 0
        total_checks = 0
        
        for col in df.columns:
            if df[col].dtype == 'object':
                # Check for mixed case in categorical data
                if df[col].nunique() < len(df) * 0.5:  # Likely categorical
                    case_variations = df[col].str.lower().nunique() - df[col].nunique()
                    if case_variations != 0:
                        consistency_issues += 1
                total_checks += 1
        
        if total_checks == 0:
            return 100
        
        return ((total_checks - consistency_issues) / total_checks) * 100
    
    def calculate_validity_score(self, df):
        """Calculate validity score"""
        # Simple validity checks
        validity_issues = 0
        total_checks = 0
        
        for col in df.columns:
            if 'email' in col.lower():
                # Check email format
                email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
                invalid_emails = ~df[col].str.match(email_pattern, na=False)
                validity_issues += invalid_emails.sum()
                total_checks += len(df)
            
            elif 'phone' in col.lower():
                # Check phone format (basic)
                phone_pattern = r'^\(\d{3}\) \d{3}-\d{4}$'
                invalid_phones = ~df[col].str.match(phone_pattern, na=False)
                validity_issues += invalid_phones.sum()
                total_checks += len(df)
        
        if total_checks == 0:
            return 100
        
        return ((total_checks - validity_issues) / total_checks) * 100
    
    def analyze_relationships(self, df):
        """Analyze relationships between columns"""
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        
        if len(numeric_cols) > 1:
            correlation_matrix = df[numeric_cols].corr()
            
            # Find high correlations
            high_correlations = []
            for i in range(len(correlation_matrix.columns)):
                for j in range(i+1, len(correlation_matrix.columns)):
                    corr_value = correlation_matrix.iloc[i, j]
                    if abs(corr_value) > 0.7:  # High correlation threshold
                        high_correlations.append({
                            'column1': correlation_matrix.columns[i],
                            'column2': correlation_matrix.columns[j],
                            'correlation': corr_value
                        })
            
            return {
                'high_correlations': high_correlations,
                'correlation_matrix': correlation_matrix.to_dict()
            }
        
        return {'message': 'Insufficient numeric columns for correlation analysis'}
```

---

## 5. Data Transformation {#data-transformation}

### Transformation Types

**1. Structural Transformations**:
- Pivoting and unpivoting
- Aggregation and summarization
- Joining and merging
- Splitting and combining columns

**2. Data Type Transformations**:
- Format conversions
- Unit conversions
- Encoding and decoding

**3. Business Rule Transformations**:
- Calculated fields
- Derived attributes
- Business logic application

### Transformation Implementation

```python
import pandas as pd
import numpy as np
from datetime import datetime, timedelta

class DataTransformer:
    def __init__(self):
        self.transformation_log = []
        self.business_rules = {}
    
    def pivot_data(self, df, index_cols, value_cols, aggfunc='sum'):
        """Pivot data for dimensional modeling"""
        try:
            pivoted = df.pivot_table(
                index=index_cols,
                values=value_cols,
                aggfunc=aggfunc,
                fill_value=0
            ).reset_index()
            
            self.log_transformation('Pivot', 
                                  f'Pivoted on {index_cols} with {value_cols}')
            return pivoted
            
        except Exception as e:
            self.log_transformation('Pivot Error', str(e))
            raise
    
    def create_date_dimensions(self, start_date, end_date):
        """Create date dimension table"""
        date_range = pd.date_range(start=start_date, end=end_date, freq='D')
        
        date_dim = pd.DataFrame({
            'date_key': date_range.strftime('%Y%m%d').astype(int),
            'full_date': date_range,
            'year': date_range.year,
            'quarter': date_range.quarter,
            'month': date_range.month,
            'month_name': date_range.strftime('%B'),
            'day': date_range.day,
            'day_of_week': date_range.dayofweek + 1,
            'day_name': date_range.strftime('%A'),
            'week_of_year': date_range.isocalendar().week,
            'is_weekend': (date_range.dayofweek >= 5).astype(int),
            'is_holiday': 0  # To be populated with holiday logic
        })
        
        # Add fiscal year (assuming April-March)
        date_dim['fiscal_year'] = np.where(
            date_dim['month'] >= 4,
            date_dim['year'],
            date_dim['year'] - 1
        )
        
        # Add season
        date_dim['season'] = date_dim['month'].map({
            12: 'Winter', 1: 'Winter', 2: 'Winter',
            3: 'Spring', 4: 'Spring', 5: 'Spring',
            6: 'Summer', 7: 'Summer', 8: 'Summer',
            9: 'Fall', 10: 'Fall', 11: 'Fall'
        })
        
        self.log_transformation('Date Dimension', 
                              f'Created {len(date_dim)} date records')
        return date_dim
    
    def create_customer_segments(self, df):
        """Create customer segmentation based on RFM analysis"""
        # Calculate Recency, Frequency, Monetary
        current_date = df['order_date'].max()
        
        rfm = df.groupby('customer_id').agg({
            'order_date': lambda x: (current_date - x.max()).days,  # Recency
            'order_id': 'count',  # Frequency
            'order_amount': 'sum'  # Monetary
        }).rename(columns={
            'order_date': 'recency',
            'order_id': 'frequency',
            'order_amount': 'monetary'
        })
        
        # Create quintiles for each metric
        rfm['r_score'] = pd.qcut(rfm['recency'], 5, labels=[5,4,3,2,1])
        rfm['f_score'] = pd.qcut(rfm['frequency'].rank(method='first'), 5, labels=[1,2,3,4,5])
        rfm['m_score'] = pd.qcut(rfm['monetary'], 5, labels=[1,2,3,4,5])
        
        # Combine scores
        rfm['rfm_score'] = rfm['r_score'].astype(str) + rfm['f_score'].astype(str) + rfm['m_score'].astype(str)
        
        # Define segments
        def segment_customers(row):
            if row['rfm_score'] in ['555', '554', '544', '545', '454', '455', '445']:
                return 'Champions'
            elif row['rfm_score'] in ['543', '444', '435', '355', '354', '345', '344', '335']:
                return 'Loyal Customers'
            elif row['rfm_score'] in ['512', '511', '422', '421', '412', '411', '311']:
                return 'Potential Loyalists'
            elif row['rfm_score'] in ['533', '532', '531', '523', '522', '521', '515', '514', '513', '425', '424', '413', '414', '415', '315', '314', '313']:
                return 'New Customers'
            elif row['rfm_score'] in ['155', '154', '144', '214', '215', '115', '114']:
                return 'At Risk'
            elif row['rfm_score'] in ['255', '254', '245', '244', '253', '252', '243', '242', '235', '234', '225', '224']:
                return 'Cannot Lose Them'
            elif row['rfm_score'] in ['155', '154', '144', '214', '215', '115', '114']:
                return 'Hibernating'
            else:
                return 'Others'
        
        rfm['segment'] = rfm.apply(segment_customers, axis=1)
        
        self.log_transformation('Customer Segmentation', 
                              f'Created segments for {len(rfm)} customers')
        return rfm.reset_index()
    
    def apply_business_rules(self, df, rules_config):
        """Apply business transformation rules"""
        df_transformed = df.copy()
        
        for rule_name, rule_config in rules_config.items():
            try:
                if rule_config['type'] == 'calculated_field':
                    # Create calculated field
                    df_transformed[rule_config['target_column']] = df_transformed.eval(
                        rule_config['formula']
                    )
                
                elif rule_config['type'] == 'conditional_mapping':
                    # Apply conditional mapping
                    conditions = rule_config['conditions']
                    df_transformed[rule_config['target_column']] = np.select(
                        [df_transformed.eval(cond['condition']) for cond in conditions],
                        [cond['value'] for cond in conditions],
                        default=rule_config.get('default_value', None)
                    )
                
                elif rule_config['type'] == 'lookup':
                    # Apply lookup transformation
                    lookup_dict = rule_config['lookup_table']
                    df_transformed[rule_config['target_column']] = df_transformed[
                        rule_config['source_column']
                    ].map(lookup_dict)
                
                self.log_transformation(f'Business Rule: {rule_name}', 
                                      f'Applied {rule_config["type"]} transformation')
                
            except Exception as e:
                self.log_transformation(f'Business Rule Error: {rule_name}', str(e))
                continue
        
        return df_transformed
    
    def normalize_data(self, df, method='min_max'):
        """Normalize numeric data"""
        df_normalized = df.copy()
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        
        for col in numeric_cols:
            if method == 'min_max':
                # Min-Max normalization (0-1)
                min_val = df[col].min()
                max_val = df[col].max()
                df_normalized[col] = (df[col] - min_val) / (max_val - min_val)
            
            elif method == 'z_score':
                # Z-score normalization
                mean_val = df[col].mean()
                std_val = df[col].std()
                df_normalized[col] = (df[col] - mean_val) / std_val
            
            elif method == 'robust':
                # Robust normalization using median and IQR
                median_val = df[col].median()
                q75 = df[col].quantile(0.75)
                q25 = df[col].quantile(0.25)
                iqr = q75 - q25
                df_normalized[col] = (df[col] - median_val) / iqr
        
        self.log_transformation('Normalization', 
                              f'Applied {method} normalization to {len(numeric_cols)} columns')
        return df_normalized
    
    def encode_categorical_data(self, df, encoding_method='label'):
        """Encode categorical data"""
        df_encoded = df.copy()
        categorical_cols = df.select_dtypes(include=['object', 'category']).columns
        
        for col in categorical_cols:
            if encoding_method == 'label':
                # Label encoding
                from sklearn.preprocessing import LabelEncoder
                le = LabelEncoder()
                df_encoded[f'{col}_encoded'] = le.fit_transform(df[col].astype(str))
            
            elif encoding_method == 'onehot':
                # One-hot encoding
                dummies = pd.get_dummies(df[col], prefix=col)
                df_encoded = pd.concat([df_encoded, dummies], axis=1)
            
            elif encoding_method == 'target':
                # Target encoding (requires target variable)
                # This is a simplified version
                if 'target' in df.columns:
                    target_mean = df.groupby(col)['target'].mean()
                    df_encoded[f'{col}_target_encoded'] = df[col].map(target_mean)
        
        self.log_transformation('Categorical Encoding', 
                              f'Applied {encoding_method} encoding to {len(categorical_cols)} columns')
        return df_encoded
    
    def create_time_based_features(self, df, date_column):
        """Create time-based features from date column"""
        df_time = df.copy()
        
        # Ensure date column is datetime
        df_time[date_column] = pd.to_datetime(df_time[date_column])
        
        # Extract time components
        df_time[f'{date_column}_year'] = df_time[date_column].dt.year
        df_time[f'{date_column}_month'] = df_time[date_column].dt.month
        df_time[f'{date_column}_day'] = df_time[date_column].dt.day
        df_time[f'{date_column}_dayofweek'] = df_time[date_column].dt.dayofweek
        df_time[f'{date_column}_quarter'] = df_time[date_column].dt.quarter
        df_time[f'{date_column}_weekofyear'] = df_time[date_column].dt.isocalendar().week
        
        # Create cyclical features
        df_time[f'{date_column}_month_sin'] = np.sin(2 * np.pi * df_time[f'{date_column}_month'] / 12)
        df_time[f'{date_column}_month_cos'] = np.cos(2 * np.pi * df_time[f'{date_column}_month'] / 12)
        df_time[f'{date_column}_day_sin'] = np.sin(2 * np.pi * df_time[f'{date_column}_dayofweek'] / 7)
        df_time[f'{date_column}_day_cos'] = np.cos(2 * np.pi * df_time[f'{date_column}_dayofweek'] / 7)
        
        # Create lag features (if data is sorted by date)
        df_time = df_time.sort_values(date_column)
        if 'value' in df.columns:  # Assuming there's a value column
            df_time['value_lag_1'] = df_time['value'].shift(1)
            df_time['value_lag_7'] = df_time['value'].shift(7)
            df_time['value_rolling_mean_7'] = df_time['value'].rolling(window=7).mean()
        
        self.log_transformation('Time Features', 
                              f'Created time-based features from {date_column}')
        return df_time
    
    def aggregate_data(self, df, group_by_cols, agg_config):
        """Aggregate data based on configuration"""
        try:
            aggregated = df.groupby(group_by_cols).agg(agg_config).reset_index()
            
            # Flatten column names if multi-level
            if isinstance(aggregated.columns, pd.MultiIndex):
                aggregated.columns = ['_'.join(col).strip() if col[1] else col[0] 
                                    for col in aggregated.columns.values]
            
            self.log_transformation('Aggregation', 
                                  f'Aggregated by {group_by_cols} with {len(agg_config)} metrics')
            return aggregated
            
        except Exception as e:
            self.log_transformation('Aggregation Error', str(e))
            raise
    
    def log_transformation(self, step_name, description):
        """Log transformation steps"""
        log_entry = {
            'timestamp': datetime.now(),
            'step': step_name,
            'description': description
        }
        self.transformation_log.append(log_entry)
    
    def get_transformation_report(self):
        """Generate transformation report"""
        return {
            'transformation_steps': self.transformation_log,
            'total_transformations': len(self.transformation_log)
        }

# Example usage and configuration
def example_transformation_pipeline():
    """Example of complete transformation pipeline"""
    
    # Sample business rules configuration
    business_rules = {
        'customer_lifetime_value': {
            'type': 'calculated_field',
            'target_column': 'clv',
            'formula': 'total_spent * purchase_frequency * customer_lifespan'
        },
        'customer_tier': {
            'type': 'conditional_mapping',
            'target_column': 'tier',
            'conditions': [
                {'condition': 'total_spent >= 10000', 'value': 'Platinum'},
                {'condition': 'total_spent >= 5000', 'value': 'Gold'},
                {'condition': 'total_spent >= 1000', 'value': 'Silver'}
            ],
            'default_value': 'Bronze'
        },
        'region_mapping': {
            'type': 'lookup',
            'source_column': 'state',
            'target_column': 'region',
            'lookup_table': {
                'CA': 'West', 'NY': 'East', 'TX': 'South', 'IL': 'Midwest'
            }
        }
    }
    
    # Aggregation configuration
    agg_config = {
        'sales_amount': ['sum', 'mean', 'count'],
        'quantity': ['sum', 'mean'],
        'discount': ['mean', 'max']
    }
    
    return business_rules, agg_config
```

This completes Part 1 of the Data Warehousing guide. The content covers introduction, architecture, data extraction, cleaning, and transformation with comprehensive code examples and practical implementations.