# Data Analytics Languages Guide - Part 2

*Advanced data analytics concepts covering DataFrames, file management, and object-oriented programming*

## Table of Contents (Part 2)
6. [Advanced Slicing Techniques](#slicing)
7. [DataFrames (Pandas)](#dataframes)
8. [File Management](#file-management)
9. [Classes and Functions](#classes-functions)
10. [Data Reshaping](#data-reshaping)

---

## 6. Advanced Slicing Techniques {#slicing}

### NumPy Array Slicing

**Multi-dimensional Array Slicing**:
```python
import numpy as np

# Create 3D array
arr_3d = np.random.randint(0, 10, (3, 4, 5))
print("Shape:", arr_3d.shape)

# Basic 3D slicing
print("First matrix:\n", arr_3d[0])
print("First row of each matrix:\n", arr_3d[:, 0, :])
print("First column of each matrix:\n", arr_3d[:, :, 0])

# Advanced slicing
print("Subarray:\n", arr_3d[1:3, 1:3, 2:4])

# Boolean indexing
arr = np.array([[1, 2, 3], [4, 5, 6], [7, 8, 9]])
mask = arr > 5
print("Elements > 5:", arr[mask])  # [6 7 8 9]

# Fancy indexing
indices = np.array([0, 2])
print("Rows 0 and 2:\n", arr[indices])
```

**Conditional Slicing**:
```python
import numpy as np

# Sample data
data = np.array([1, 5, 3, 8, 2, 9, 4, 7, 6])

# Multiple conditions
condition = (data > 3) & (data < 8)
filtered_data = data[condition]
print("Values between 3 and 8:", filtered_data)  # [5 4 7 6]

# Using np.where
result = np.where(data > 5, data, 0)  # Replace values <= 5 with 0
print("Conditional replacement:", result)  # [0 0 0 8 0 9 0 7 6]

# Complex conditions
matrix = np.random.randint(0, 10, (5, 5))
# Find positions where value > 5
positions = np.where(matrix > 5)
print("Positions (row, col) where value > 5:")
for row, col in zip(positions[0], positions[1]):
    print(f"({row}, {col}): {matrix[row, col]}")
```

### Pandas Series and DataFrame Slicing

**Series Slicing**:
```python
import pandas as pd
import numpy as np

# Create series with custom index
dates = pd.date_range('2024-01-01', periods=10, freq='D')
series = pd.Series(np.random.randn(10), index=dates)

# Label-based slicing
print("First 5 days:\n", series['2024-01-01':'2024-01-05'])

# Position-based slicing
print("First 3 elements:\n", series.iloc[:3])

# Boolean slicing
print("Positive values:\n", series[series > 0])

# Time-based slicing
print("January data:\n", series['2024-01'])
```

---

## 7. DataFrames (Pandas) {#dataframes}

### DataFrame Creation and Basic Operations

**Creating DataFrames**:
```python
import pandas as pd
import numpy as np

# From dictionary
data = {
    'Name': ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve'],
    'Age': [25, 30, 35, 28, 32],
    'City': ['New York', 'London', 'Tokyo', 'Paris', 'Sydney'],
    'Salary': [70000, 80000, 90000, 75000, 85000],
    'Department': ['IT', 'Finance', 'IT', 'HR', 'Finance']
}
df = pd.DataFrame(data)
print(df)

# From lists
columns = ['A', 'B', 'C', 'D']
data_matrix = np.random.randn(5, 4)
df_matrix = pd.DataFrame(data_matrix, columns=columns)

# From CSV (simulation)
df_from_dict = pd.DataFrame({
    'product': ['A', 'B', 'C', 'D', 'E'],
    'price': [10.5, 15.2, 8.9, 12.3, 9.8],
    'quantity': [100, 150, 200, 120, 180]
})
```

**DataFrame Information and Inspection**:
```python
# Basic information
print("Shape:", df.shape)                    # (5, 5)
print("Columns:", df.columns.tolist())       # ['Name', 'Age', 'City', 'Salary', 'Department']
print("Data types:\n", df.dtypes)
print("Info:")
df.info()

# Statistical summary
print("Describe:\n", df.describe())
print("Describe all:\n", df.describe(include='all'))

# First and last rows
print("Head:\n", df.head(3))
print("Tail:\n", df.tail(2))

# Sample rows
print("Sample:\n", df.sample(3))
```

### DataFrame Indexing and Selection

**Column Selection**:
```python
# Single column
names = df['Name']
print(type(names))  # pandas.Series

# Multiple columns
subset = df[['Name', 'Age', 'Salary']]
print(subset)

# Column operations
df['Salary_K'] = df['Salary'] / 1000  # Create new column
df['Age_Group'] = df['Age'].apply(lambda x: 'Young' if x < 30 else 'Senior')
```

**Row Selection**:
```python
# By index position
first_row = df.iloc[0]
first_three = df.iloc[:3]
specific_rows = df.iloc[[0, 2, 4]]

# By label (if custom index)
df_indexed = df.set_index('Name')
alice_data = df_indexed.loc['Alice']

# Boolean indexing
high_salary = df[df['Salary'] > 75000]
it_employees = df[df['Department'] == 'IT']
young_high_earners = df[(df['Age'] < 30) & (df['Salary'] > 70000)]
```

**Advanced Selection**:
```python
# loc and iloc
print("Using loc (label-based):")
print(df.loc[0:2, 'Name':'City'])

print("Using iloc (position-based):")
print(df.iloc[0:3, 1:4])

# Query method
result = df.query('Age > 30 and Salary > 80000')
print("Query result:\n", result)

# isin method
cities_of_interest = ['New York', 'London', 'Tokyo']
filtered = df[df['City'].isin(cities_of_interest)]
print("Filtered by cities:\n", filtered)
```

### DataFrame Operations

**Sorting and Ranking**:
```python
# Sort by single column
df_sorted = df.sort_values('Age')
print("Sorted by Age:\n", df_sorted)

# Sort by multiple columns
df_multi_sort = df.sort_values(['Department', 'Salary'], ascending=[True, False])
print("Sorted by Department and Salary:\n", df_multi_sort)

# Ranking
df['Salary_Rank'] = df['Salary'].rank(ascending=False)
df['Age_Rank'] = df['Age'].rank()
print("With rankings:\n", df[['Name', 'Salary', 'Salary_Rank', 'Age', 'Age_Rank']])
```

**Grouping and Aggregation**:
```python
# Group by single column
dept_groups = df.groupby('Department')
print("Average salary by department:")
print(dept_groups['Salary'].mean())

# Multiple aggregations
agg_result = df.groupby('Department').agg({
    'Salary': ['mean', 'max', 'min', 'count'],
    'Age': ['mean', 'std']
})
print("Multiple aggregations:\n", agg_result)

# Custom aggregation
def salary_range(series):
    return series.max() - series.min()

custom_agg = df.groupby('Department')['Salary'].agg([
    ('avg_salary', 'mean'),
    ('salary_range', salary_range),
    ('count', 'count')
])
print("Custom aggregation:\n", custom_agg)
```

**Missing Data Handling**:
```python
# Create DataFrame with missing values
df_missing = pd.DataFrame({
    'A': [1, 2, np.nan, 4, 5],
    'B': [np.nan, 2, 3, 4, np.nan],
    'C': [1, 2, 3, np.nan, 5],
    'D': ['a', 'b', 'c', 'd', 'e']
})

# Check for missing values
print("Missing values:\n", df_missing.isnull().sum())
print("Any missing values:", df_missing.isnull().any().any())

# Drop missing values
df_dropped = df_missing.dropna()  # Drop rows with any NaN
df_dropped_cols = df_missing.dropna(axis=1)  # Drop columns with any NaN
df_dropped_thresh = df_missing.dropna(thresh=3)  # Keep rows with at least 3 non-NaN

# Fill missing values
df_filled = df_missing.fillna(0)  # Fill with 0
df_filled_method = df_missing.fillna(method='ffill')  # Forward fill
df_filled_mean = df_missing.fillna(df_missing.mean())  # Fill with mean

# Interpolation
df_interpolated = df_missing.interpolate()
print("Interpolated:\n", df_interpolated)
```

### DataFrame Merging and Joining

**Concatenation**:
```python
# Create sample DataFrames
df1 = pd.DataFrame({'A': [1, 2], 'B': [3, 4]})
df2 = pd.DataFrame({'A': [5, 6], 'B': [7, 8]})
df3 = pd.DataFrame({'C': [9, 10], 'D': [11, 12]})

# Vertical concatenation
vertical = pd.concat([df1, df2], ignore_index=True)
print("Vertical concat:\n", vertical)

# Horizontal concatenation
horizontal = pd.concat([df1, df3], axis=1)
print("Horizontal concat:\n", horizontal)
```

**Merging DataFrames**:
```python
# Sample data for merging
employees = pd.DataFrame({
    'emp_id': [1, 2, 3, 4],
    'name': ['Alice', 'Bob', 'Charlie', 'Diana'],
    'dept_id': [10, 20, 10, 30]
})

departments = pd.DataFrame({
    'dept_id': [10, 20, 30, 40],
    'dept_name': ['IT', 'Finance', 'HR', 'Marketing'],
    'location': ['NY', 'London', 'Tokyo', 'Paris']
})

# Inner join (default)
inner_merge = pd.merge(employees, departments, on='dept_id')
print("Inner merge:\n", inner_merge)

# Left join
left_merge = pd.merge(employees, departments, on='dept_id', how='left')
print("Left merge:\n", left_merge)

# Outer join
outer_merge = pd.merge(employees, departments, on='dept_id', how='outer')
print("Outer merge:\n", outer_merge)

# Merge on different column names
salaries = pd.DataFrame({
    'employee_id': [1, 2, 3, 4],
    'salary': [70000, 80000, 75000, 85000]
})

merged_diff_cols = pd.merge(employees, salaries, 
                           left_on='emp_id', right_on='employee_id')
print("Merge on different columns:\n", merged_diff_cols)
```

---

## 8. File Management {#file-management}

### Reading Different File Formats

**CSV Files**:
```python
import pandas as pd
import numpy as np

# Create sample CSV data
sample_data = {
    'Date': pd.date_range('2024-01-01', periods=100, freq='D'),
    'Product': np.random.choice(['A', 'B', 'C'], 100),
    'Sales': np.random.randint(100, 1000, 100),
    'Region': np.random.choice(['North', 'South', 'East', 'West'], 100)
}
df_sample = pd.DataFrame(sample_data)

# Save to CSV
df_sample.to_csv('sample_data.csv', index=False)

# Read CSV with various options
df_read = pd.read_csv('sample_data.csv')
df_read_custom = pd.read_csv('sample_data.csv', 
                            parse_dates=['Date'],
                            dtype={'Product': 'category'},
                            nrows=50)  # Read only first 50 rows

# Read with custom separator
df_sample.to_csv('sample_data_semicolon.csv', sep=';', index=False)
df_semicolon = pd.read_csv('sample_data_semicolon.csv', sep=';')

# Handle missing values while reading
df_with_na = df_sample.copy()
df_with_na.loc[10:15, 'Sales'] = np.nan
df_with_na.to_csv('data_with_na.csv', index=False)
df_na_read = pd.read_csv('data_with_na.csv', na_values=['', 'NULL', 'N/A'])
```

**Excel Files**:
```python
# Write to Excel
with pd.ExcelWriter('sample_data.xlsx', engine='openpyxl') as writer:
    df_sample.to_excel(writer, sheet_name='Sales_Data', index=False)
    df_sample.groupby('Product')['Sales'].sum().to_excel(writer, sheet_name='Summary')

# Read Excel
df_excel = pd.read_excel('sample_data.xlsx', sheet_name='Sales_Data')
df_summary = pd.read_excel('sample_data.xlsx', sheet_name='Summary')

# Read specific range
df_range = pd.read_excel('sample_data.xlsx', sheet_name='Sales_Data', 
                        usecols='A:C', nrows=20)
```

**JSON Files**:
```python
import json

# Create sample JSON data
json_data = {
    'employees': [
        {'id': 1, 'name': 'Alice', 'department': 'IT', 'salary': 70000},
        {'id': 2, 'name': 'Bob', 'department': 'Finance', 'salary': 80000},
        {'id': 3, 'name': 'Charlie', 'department': 'IT', 'salary': 75000}
    ],
    'metadata': {
        'created_date': '2024-03-15',
        'version': '1.0'
    }
}

# Write JSON
with open('employees.json', 'w') as f:
    json.dump(json_data, f, indent=2)

# Read JSON
with open('employees.json', 'r') as f:
    loaded_data = json.load(f)

# Convert to DataFrame
df_from_json = pd.DataFrame(loaded_data['employees'])
print("DataFrame from JSON:\n", df_from_json)

# Direct pandas JSON operations
df_sample.to_json('sample_data.json', orient='records', date_format='iso')
df_json_read = pd.read_json('sample_data.json')
```

**Database Connections**:
```python
import sqlite3
import pandas as pd

# Create SQLite database
conn = sqlite3.connect('sample_database.db')

# Write DataFrame to database
df_sample.to_sql('sales_data', conn, if_exists='replace', index=False)

# Read from database
df_from_db = pd.read_sql_query("SELECT * FROM sales_data WHERE Sales > 500", conn)
print("Data from database:\n", df_from_db.head())

# More complex query
query = """
SELECT Product, Region, AVG(Sales) as avg_sales, COUNT(*) as count
FROM sales_data 
GROUP BY Product, Region
ORDER BY avg_sales DESC
"""
df_aggregated = pd.read_sql_query(query, conn)
print("Aggregated data:\n", df_aggregated)

conn.close()
```

### File Operations and Path Management

**Working with Paths**:
```python
import os
from pathlib import Path
import glob

# Current working directory
current_dir = os.getcwd()
print("Current directory:", current_dir)

# Create directories
os.makedirs('data/processed', exist_ok=True)
os.makedirs('data/raw', exist_ok=True)

# Using pathlib (recommended)
data_path = Path('data')
processed_path = data_path / 'processed'
raw_path = data_path / 'raw'

# Check if path exists
if processed_path.exists():
    print("Processed directory exists")

# List files
csv_files = list(Path('.').glob('*.csv'))
print("CSV files:", csv_files)

# File information
for file_path in csv_files:
    stat = file_path.stat()
    print(f"File: {file_path.name}, Size: {stat.st_size} bytes")
```

**Batch File Processing**:
```python
import pandas as pd
from pathlib import Path
import glob

# Create multiple sample files
for i in range(3):
    sample_df = pd.DataFrame({
        'id': range(i*10, (i+1)*10),
        'value': np.random.randn(10),
        'category': np.random.choice(['A', 'B'], 10)
    })
    sample_df.to_csv(f'data/raw/file_{i}.csv', index=False)

# Process multiple files
def process_files(input_dir, output_dir):
    input_path = Path(input_dir)
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    all_data = []
    
    for file_path in input_path.glob('*.csv'):
        print(f"Processing {file_path.name}")
        df = pd.read_csv(file_path)
        
        # Add source file column
        df['source_file'] = file_path.name
        
        # Some processing
        df['value_squared'] = df['value'] ** 2
        
        all_data.append(df)
        
        # Save processed file
        output_file = output_path / f"processed_{file_path.name}"
        df.to_csv(output_file, index=False)
    
    # Combine all files
    combined_df = pd.concat(all_data, ignore_index=True)
    combined_df.to_csv(output_path / 'combined_data.csv', index=False)
    
    return combined_df

# Process files
combined_data = process_files('data/raw', 'data/processed')
print("Combined data shape:", combined_data.shape)
```

---

## 9. Classes and Functions {#classes-functions}

### Functions for Data Analysis

**Basic Functions**:
```python
def calculate_statistics(data):
    """Calculate basic statistics for a list of numbers."""
    if not data:
        return None
    
    stats = {
        'count': len(data),
        'mean': sum(data) / len(data),
        'min': min(data),
        'max': max(data),
        'range': max(data) - min(data)
    }
    
    # Calculate median
    sorted_data = sorted(data)
    n = len(sorted_data)
    if n % 2 == 0:
        stats['median'] = (sorted_data[n//2 - 1] + sorted_data[n//2]) / 2
    else:
        stats['median'] = sorted_data[n//2]
    
    return stats

# Example usage
sample_data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
stats = calculate_statistics(sample_data)
print("Statistics:", stats)
```

**Advanced Functions with Decorators**:
```python
import time
import functools
from typing import List, Dict, Any

def timing_decorator(func):
    """Decorator to measure function execution time."""
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        print(f"{func.__name__} took {end_time - start_time:.4f} seconds")
        return result
    return wrapper

def validate_input(func):
    """Decorator to validate input data."""
    @functools.wraps(func)
    def wrapper(data, *args, **kwargs):
        if not isinstance(data, (list, tuple)):
            raise TypeError("Data must be a list or tuple")
        if not data:
            raise ValueError("Data cannot be empty")
        if not all(isinstance(x, (int, float)) for x in data):
            raise TypeError("All data elements must be numeric")
        return func(data, *args, **kwargs)
    return wrapper

@timing_decorator
@validate_input
def advanced_statistics(data: List[float]) -> Dict[str, Any]:
    """Calculate advanced statistics with validation and timing."""
    import math
    
    n = len(data)
    mean = sum(data) / n
    
    # Variance and standard deviation
    variance = sum((x - mean) ** 2 for x in data) / (n - 1)
    std_dev = math.sqrt(variance)
    
    # Skewness
    skewness = sum((x - mean) ** 3 for x in data) / (n * std_dev ** 3)
    
    return {
        'count': n,
        'mean': mean,
        'std_dev': std_dev,
        'variance': variance,
        'skewness': skewness
    }

# Example usage
data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20]
result = advanced_statistics(data)
print("Advanced statistics:", result)
```

**Lambda Functions and Functional Programming**:
```python
import pandas as pd
from functools import reduce

# Lambda functions
square = lambda x: x ** 2
is_even = lambda x: x % 2 == 0

numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

# Map, filter, reduce
squared_numbers = list(map(square, numbers))
even_numbers = list(filter(is_even, numbers))
sum_of_numbers = reduce(lambda x, y: x + y, numbers)

print("Squared:", squared_numbers)
print("Even numbers:", even_numbers)
print("Sum:", sum_of_numbers)

# DataFrame operations with lambda
df = pd.DataFrame({
    'A': [1, 2, 3, 4, 5],
    'B': [10, 20, 30, 40, 50],
    'C': [100, 200, 300, 400, 500]
})

# Apply lambda to columns
df['A_squared'] = df['A'].apply(lambda x: x ** 2)
df['B_category'] = df['B'].apply(lambda x: 'High' if x > 25 else 'Low')

# Apply lambda to rows
df['row_sum'] = df[['A', 'B', 'C']].apply(lambda row: row.sum(), axis=1)
print("DataFrame with lambda operations:\n", df)
```

### Object-Oriented Programming for Data Analysis

**Basic Data Analysis Class**:
```python
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from typing import Optional, List, Dict, Any

class DataAnalyzer:
    """A class for basic data analysis operations."""
    
    def __init__(self, data: Optional[pd.DataFrame] = None):
        self.data = data
        self.analysis_history = []
    
    def load_data(self, file_path: str, file_type: str = 'csv') -> None:
        """Load data from file."""
        try:
            if file_type.lower() == 'csv':
                self.data = pd.read_csv(file_path)
            elif file_type.lower() == 'excel':
                self.data = pd.read_excel(file_path)
            elif file_type.lower() == 'json':
                self.data = pd.read_json(file_path)
            else:
                raise ValueError(f"Unsupported file type: {file_type}")
            
            self.analysis_history.append(f"Loaded data from {file_path}")
            print(f"Data loaded successfully. Shape: {self.data.shape}")
            
        except Exception as e:
            print(f"Error loading data: {e}")
    
    def basic_info(self) -> Dict[str, Any]:
        """Get basic information about the dataset."""
        if self.data is None:
            raise ValueError("No data loaded")
        
        info = {
            'shape': self.data.shape,
            'columns': self.data.columns.tolist(),
            'dtypes': self.data.dtypes.to_dict(),
            'missing_values': self.data.isnull().sum().to_dict(),
            'memory_usage': self.data.memory_usage(deep=True).sum()
        }
        
        self.analysis_history.append("Generated basic info")
        return info
    
    def summary_statistics(self, columns: Optional[List[str]] = None) -> pd.DataFrame:
        """Generate summary statistics."""
        if self.data is None:
            raise ValueError("No data loaded")
        
        if columns:
            data_subset = self.data[columns]
        else:
            data_subset = self.data.select_dtypes(include=[np.number])
        
        summary = data_subset.describe()
        self.analysis_history.append(f"Generated summary statistics for {len(data_subset.columns)} columns")
        return summary
    
    def correlation_analysis(self, method: str = 'pearson') -> pd.DataFrame:
        """Calculate correlation matrix."""
        if self.data is None:
            raise ValueError("No data loaded")
        
        numeric_data = self.data.select_dtypes(include=[np.number])
        correlation_matrix = numeric_data.corr(method=method)
        
        self.analysis_history.append(f"Calculated {method} correlation matrix")
        return correlation_matrix
    
    def detect_outliers(self, column: str, method: str = 'iqr') -> pd.Series:
        """Detect outliers in a column."""
        if self.data is None:
            raise ValueError("No data loaded")
        
        if column not in self.data.columns:
            raise ValueError(f"Column '{column}' not found")
        
        series = self.data[column]
        
        if method == 'iqr':
            Q1 = series.quantile(0.25)
            Q3 = series.quantile(0.75)
            IQR = Q3 - Q1
            lower_bound = Q1 - 1.5 * IQR
            upper_bound = Q3 + 1.5 * IQR
            outliers = series[(series < lower_bound) | (series > upper_bound)]
        
        elif method == 'zscore':
            z_scores = np.abs((series - series.mean()) / series.std())
            outliers = series[z_scores > 3]
        
        else:
            raise ValueError("Method must be 'iqr' or 'zscore'")
        
        self.analysis_history.append(f"Detected {len(outliers)} outliers in '{column}' using {method}")
        return outliers
    
    def get_analysis_history(self) -> List[str]:
        """Get history of analysis operations."""
        return self.analysis_history.copy()

# Example usage
analyzer = DataAnalyzer()

# Create sample data
sample_data = pd.DataFrame({
    'age': np.random.normal(35, 10, 1000),
    'income': np.random.normal(50000, 15000, 1000),
    'score': np.random.normal(75, 12, 1000),
    'category': np.random.choice(['A', 'B', 'C'], 1000)
})

# Add some outliers
sample_data.loc[0:5, 'income'] = [150000, 200000, 180000, 160000, 175000, 190000]

analyzer.data = sample_data

# Perform analysis
info = analyzer.basic_info()
print("Basic info:", info['shape'])

summary = analyzer.summary_statistics()
print("Summary statistics:\n", summary)

outliers = analyzer.detect_outliers('income', method='iqr')
print(f"Income outliers: {len(outliers)} found")

history = analyzer.get_analysis_history()
print("Analysis history:", history)
```

**Advanced Data Processing Class**:
```python
class DataProcessor(DataAnalyzer):
    """Extended class for data processing operations."""
    
    def __init__(self, data: Optional[pd.DataFrame] = None):
        super().__init__(data)
        self.processed_data = None
    
    def clean_data(self, 
                   drop_duplicates: bool = True,
                   handle_missing: str = 'drop',
                   remove_outliers: bool = False,
                   outlier_columns: Optional[List[str]] = None) -> pd.DataFrame:
        """Clean the dataset."""
        if self.data is None:
            raise ValueError("No data loaded")
        
        cleaned_data = self.data.copy()
        
        # Remove duplicates
        if drop_duplicates:
            initial_shape = cleaned_data.shape
            cleaned_data = cleaned_data.drop_duplicates()
            removed_duplicates = initial_shape[0] - cleaned_data.shape[0]
            self.analysis_history.append(f"Removed {removed_duplicates} duplicate rows")
        
        # Handle missing values
        if handle_missing == 'drop':
            cleaned_data = cleaned_data.dropna()
        elif handle_missing == 'fill_mean':
            numeric_columns = cleaned_data.select_dtypes(include=[np.number]).columns
            cleaned_data[numeric_columns] = cleaned_data[numeric_columns].fillna(
                cleaned_data[numeric_columns].mean()
            )
        elif handle_missing == 'fill_median':
            numeric_columns = cleaned_data.select_dtypes(include=[np.number]).columns
            cleaned_data[numeric_columns] = cleaned_data[numeric_columns].fillna(
                cleaned_data[numeric_columns].median()
            )
        
        # Remove outliers
        if remove_outliers and outlier_columns:
            for column in outlier_columns:
                if column in cleaned_data.columns:
                    outliers = self.detect_outliers(column)
                    cleaned_data = cleaned_data[~cleaned_data.index.isin(outliers.index)]
        
        self.processed_data = cleaned_data
        self.analysis_history.append(f"Data cleaned. New shape: {cleaned_data.shape}")
        return cleaned_data
    
    def feature_engineering(self, operations: Dict[str, Any]) -> pd.DataFrame:
        """Perform feature engineering operations."""
        if self.processed_data is None:
            data = self.data.copy()
        else:
            data = self.processed_data.copy()
        
        for operation, params in operations.items():
            if operation == 'create_bins':
                column = params['column']
                bins = params['bins']
                labels = params.get('labels', None)
                new_column = params.get('new_column', f"{column}_binned")
                data[new_column] = pd.cut(data[column], bins=bins, labels=labels)
            
            elif operation == 'create_dummies':
                columns = params['columns']
                dummy_df = pd.get_dummies(data[columns], prefix=columns)
                data = pd.concat([data, dummy_df], axis=1)
                data = data.drop(columns=columns)
            
            elif operation == 'normalize':
                columns = params['columns']
                for col in columns:
                    data[f"{col}_normalized"] = (data[col] - data[col].mean()) / data[col].std()
        
        self.processed_data = data
        self.analysis_history.append("Feature engineering completed")
        return data

# Example usage
processor = DataProcessor(sample_data)

# Clean data
cleaned = processor.clean_data(
    drop_duplicates=True,
    handle_missing='fill_mean',
    remove_outliers=True,
    outlier_columns=['income']
)

# Feature engineering
operations = {
    'create_bins': {
        'column': 'age',
        'bins': [0, 25, 35, 50, 100],
        'labels': ['Young', 'Adult', 'Middle-aged', 'Senior'],
        'new_column': 'age_group'
    },
    'create_dummies': {
        'columns': ['category']
    },
    'normalize': {
        'columns': ['income', 'score']
    }
}

processed = processor.feature_engineering(operations)
print("Processed data shape:", processed.shape)
print("New columns:", [col for col in processed.columns if col not in sample_data.columns])
```

---

## 10. Data Reshaping {#data-reshaping}

### Pivot Tables and Reshaping

**Pivot Tables**:
```python
import pandas as pd
import numpy as np

# Create sample sales data
np.random.seed(42)
sales_data = pd.DataFrame({
    'Date': pd.date_range('2024-01-01', periods=365, freq='D'),
    'Product': np.random.choice(['A', 'B', 'C', 'D'], 365),
    'Region': np.random.choice(['North', 'South', 'East', 'West'], 365),
    'Salesperson': np.random.choice(['Alice', 'Bob', 'Charlie', 'Diana'], 365),
    'Sales': np.random.randint(100, 1000, 365),
    'Quantity': np.random.randint(1, 20, 365)
})

# Add month column
sales_data['Month'] = sales_data['Date'].dt.month
sales_data['Quarter'] = sales_data['Date'].dt.quarter

# Basic pivot table
pivot_basic = sales_data.pivot_table(
    values='Sales',
    index='Product',
    columns='Region',
    aggfunc='sum'
)
print("Basic pivot table:\n", pivot_basic)

# Multiple aggregations
pivot_multi = sales_data.pivot_table(
    values=['Sales', 'Quantity'],
    index='Product',
    columns='Region',
    aggfunc={'Sales': 'sum', 'Quantity': 'mean'}
)
print("Multi-aggregation pivot:\n", pivot_multi)

# Hierarchical pivot
pivot_hierarchical = sales_data.pivot_table(
    values='Sales',
    index=['Product', 'Salesperson'],
    columns=['Region', 'Quarter'],
    aggfunc='sum',
    fill_value=0
)
print("Hierarchical pivot shape:", pivot_hierarchical.shape)
```

**Melting and Reshaping**:
```python
# Wide format data
wide_data = pd.DataFrame({
    'ID': [1, 2, 3, 4],
    'Name': ['Alice', 'Bob', 'Charlie', 'Diana'],
    'Q1_Sales': [100, 150, 200, 120],
    'Q2_Sales': [110, 160, 190, 130],
    'Q3_Sales': [120, 170, 210, 140],
    'Q4_Sales': [130, 180, 220, 150]
})

# Melt to long format
long_data = pd.melt(
    wide_data,
    id_vars=['ID', 'Name'],
    value_vars=['Q1_Sales', 'Q2_Sales', 'Q3_Sales', 'Q4_Sales'],
    var_name='Quarter',
    value_name='Sales'
)

# Clean up quarter column
long_data['Quarter'] = long_data['Quarter'].str.replace('_Sales', '')
print("Long format data:\n", long_data.head(10))

# Pivot back to wide format
wide_again = long_data.pivot_table(
    values='Sales',
    index=['ID', 'Name'],
    columns='Quarter',
    aggfunc='first'
).reset_index()

# Flatten column names
wide_again.columns.name = None
print("Back to wide format:\n", wide_again)
```

**Stack and Unstack Operations**:
```python
# Create MultiIndex DataFrame
arrays = [
    ['A', 'A', 'B', 'B'],
    ['one', 'two', 'one', 'two']
]
index = pd.MultiIndex.from_arrays(arrays, names=['first', 'second'])
df_multi = pd.DataFrame(np.random.randn(4, 3), index=index, columns=['X', 'Y', 'Z'])
print("MultiIndex DataFrame:\n", df_multi)

# Stack - pivot columns to rows
stacked = df_multi.stack()
print("Stacked:\n", stacked)

# Unstack - pivot rows to columns
unstacked = stacked.unstack()
print("Unstacked:\n", unstacked)

# Unstack specific level
unstacked_level = df_multi.unstack(level='second')
print("Unstacked by level:\n", unstacked_level)
```

**Advanced Reshaping Techniques**:
```python
# Cross-tabulation
crosstab = pd.crosstab(
    sales_data['Product'],
    sales_data['Region'],
    values=sales_data['Sales'],
    aggfunc='sum',
    margins=True
)
print("Cross-tabulation:\n", crosstab)

# Percentage cross-tabulation
crosstab_pct = pd.crosstab(
    sales_data['Product'],
    sales_data['Region'],
    normalize='index'
) * 100
print("Percentage cross-tabulation:\n", crosstab_pct)

# Groupby with unstack
grouped_unstacked = sales_data.groupby(['Product', 'Region'])['Sales'].sum().unstack(fill_value=0)
print("Grouped and unstacked:\n", grouped_unstacked)

# Multiple column reshaping
sales_summary = sales_data.groupby(['Product', 'Month']).agg({
    'Sales': ['sum', 'mean', 'count'],
    'Quantity': ['sum', 'mean']
}).round(2)

# Flatten column names
sales_summary.columns = ['_'.join(col).strip() for col in sales_summary.columns]
sales_summary = sales_summary.reset_index()
print("Sales summary columns:", sales_summary.columns.tolist())
```

---

*Continue to Part 3 for Data Mining, Importing/Exporting Data, Charts and Graphs, and R Programming*