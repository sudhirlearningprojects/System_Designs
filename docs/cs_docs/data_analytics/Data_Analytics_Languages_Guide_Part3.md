# Data Analytics Languages Guide - Part 3

*Advanced data analytics covering data mining, visualization, and R programming*

## Table of Contents (Part 3)
11. [Data Mining](#data-mining)
12. [Importing and Exporting Data](#importing-exporting)
13. [Charts and Graphs](#charts-graphs)
14. [R Programming for Data Analytics](#r-programming)
15. [MCQ Practice Questions](#mcq-questions)

---

## 11. Data Mining {#data-mining}

### Machine Learning with Scikit-learn

**Classification**:
```python
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.datasets import make_classification

# Generate sample classification data
X, y = make_classification(n_samples=1000, n_features=20, n_informative=10, 
                          n_redundant=10, n_clusters_per_class=1, random_state=42)

# Split data
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Scale features
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Train multiple models
models = {
    'Random Forest': RandomForestClassifier(n_estimators=100, random_state=42),
    'Logistic Regression': LogisticRegression(random_state=42),
    'SVM': SVC(random_state=42)
}

results = {}
for name, model in models.items():
    # Train model
    if name == 'SVM':
        model.fit(X_train_scaled, y_train)
        y_pred = model.predict(X_test_scaled)
    else:
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
    
    # Evaluate
    accuracy = accuracy_score(y_test, y_pred)
    results[name] = accuracy
    
    print(f"\n{name} Results:")
    print(f"Accuracy: {accuracy:.4f}")
    print("Classification Report:")
    print(classification_report(y_test, y_pred))

print("\nModel Comparison:")
for name, accuracy in results.items():
    print(f"{name}: {accuracy:.4f}")
```

**Regression**:
```python
from sklearn.linear_model import LinearRegression, Ridge, Lasso
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
from sklearn.datasets import make_regression

# Generate regression data
X, y = make_regression(n_samples=1000, n_features=10, noise=0.1, random_state=42)

# Split data
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Regression models
regression_models = {
    'Linear Regression': LinearRegression(),
    'Ridge Regression': Ridge(alpha=1.0),
    'Lasso Regression': Lasso(alpha=1.0),
    'Random Forest': RandomForestRegressor(n_estimators=100, random_state=42)
}

regression_results = {}
for name, model in regression_models.items():
    # Train and predict
    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    
    # Calculate metrics
    mse = mean_squared_error(y_test, y_pred)
    rmse = np.sqrt(mse)
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    
    regression_results[name] = {
        'MSE': mse,
        'RMSE': rmse,
        'MAE': mae,
        'R²': r2
    }
    
    print(f"\n{name} Results:")
    print(f"MSE: {mse:.4f}")
    print(f"RMSE: {rmse:.4f}")
    print(f"MAE: {mae:.4f}")
    print(f"R²: {r2:.4f}")
```

**Clustering**:
```python
from sklearn.cluster import KMeans, DBSCAN, AgglomerativeClustering
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score, adjusted_rand_score
from sklearn.datasets import make_blobs
import matplotlib.pyplot as plt

# Generate clustering data
X, y_true = make_blobs(n_samples=300, centers=4, cluster_std=0.60, random_state=42)

# Scale data
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# Clustering algorithms
clustering_models = {
    'K-Means': KMeans(n_clusters=4, random_state=42),
    'DBSCAN': DBSCAN(eps=0.3, min_samples=5),
    'Agglomerative': AgglomerativeClustering(n_clusters=4)
}

clustering_results = {}
for name, model in clustering_models.items():
    # Fit model
    if name == 'DBSCAN':
        labels = model.fit_predict(X_scaled)
    else:
        labels = model.fit_predict(X)
    
    # Calculate metrics
    if len(set(labels)) > 1:  # More than one cluster
        silhouette = silhouette_score(X, labels)
        ari = adjusted_rand_score(y_true, labels)
        
        clustering_results[name] = {
            'Silhouette Score': silhouette,
            'Adjusted Rand Index': ari,
            'Number of Clusters': len(set(labels))
        }
        
        print(f"\n{name} Results:")
        print(f"Silhouette Score: {silhouette:.4f}")
        print(f"Adjusted Rand Index: {ari:.4f}")
        print(f"Number of Clusters: {len(set(labels))}")
```

### Feature Selection and Engineering

**Feature Selection**:
```python
from sklearn.feature_selection import SelectKBest, f_classif, RFE
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler

# Generate data with some irrelevant features
X, y = make_classification(n_samples=1000, n_features=20, n_informative=5, 
                          n_redundant=5, n_clusters_per_class=1, random_state=42)

# Method 1: Univariate feature selection
selector_univariate = SelectKBest(score_func=f_classif, k=10)
X_selected_univariate = selector_univariate.fit_transform(X, y)

# Get selected feature indices
selected_features_univariate = selector_univariate.get_support(indices=True)
print("Univariate selected features:", selected_features_univariate)

# Method 2: Recursive Feature Elimination
rf = RandomForestClassifier(n_estimators=100, random_state=42)
selector_rfe = RFE(estimator=rf, n_features_to_select=10)
X_selected_rfe = selector_rfe.fit_transform(X, y)

selected_features_rfe = selector_rfe.get_support(indices=True)
print("RFE selected features:", selected_features_rfe)

# Method 3: Feature importance from Random Forest
rf.fit(X, y)
feature_importance = rf.feature_importances_
important_features = np.argsort(feature_importance)[-10:]  # Top 10 features
print("Random Forest important features:", important_features)

# Compare performance
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Original features
rf_original = RandomForestClassifier(n_estimators=100, random_state=42)
rf_original.fit(X_train, y_train)
accuracy_original = rf_original.score(X_test, y_test)

# Selected features (RFE)
X_train_selected = selector_rfe.transform(X_train)
X_test_selected = selector_rfe.transform(X_test)
rf_selected = RandomForestClassifier(n_estimators=100, random_state=42)
rf_selected.fit(X_train_selected, y_train)
accuracy_selected = rf_selected.score(X_test_selected, y_test)

print(f"\nAccuracy with all features: {accuracy_original:.4f}")
print(f"Accuracy with selected features: {accuracy_selected:.4f}")
```

**Dimensionality Reduction**:
```python
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
import matplotlib.pyplot as plt

# Generate high-dimensional data
X, y = make_classification(n_samples=500, n_features=50, n_informative=10, 
                          n_clusters_per_class=1, random_state=42)

# PCA
pca = PCA(n_components=2)
X_pca = pca.fit_transform(X)

print("PCA explained variance ratio:", pca.explained_variance_ratio_)
print("Total variance explained:", sum(pca.explained_variance_ratio_))

# t-SNE
tsne = TSNE(n_components=2, random_state=42)
X_tsne = tsne.fit_transform(X)

# Plot results
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))

# PCA plot
scatter1 = ax1.scatter(X_pca[:, 0], X_pca[:, 1], c=y, cmap='viridis')
ax1.set_title('PCA')
ax1.set_xlabel('First Principal Component')
ax1.set_ylabel('Second Principal Component')

# t-SNE plot
scatter2 = ax2.scatter(X_tsne[:, 0], X_tsne[:, 1], c=y, cmap='viridis')
ax2.set_title('t-SNE')
ax2.set_xlabel('t-SNE 1')
ax2.set_ylabel('t-SNE 2')

plt.tight_layout()
plt.show()
```

---

## 12. Importing and Exporting Data {#importing-exporting}

### Advanced Data Import/Export

**Multiple File Formats**:
```python
import pandas as pd
import numpy as np
import json
import pickle
import sqlite3
from pathlib import Path

# Create sample data
sample_data = pd.DataFrame({
    'id': range(1, 101),
    'name': [f'Person_{i}' for i in range(1, 101)],
    'age': np.random.randint(18, 80, 100),
    'salary': np.random.randint(30000, 120000, 100),
    'department': np.random.choice(['IT', 'Finance', 'HR', 'Marketing'], 100),
    'join_date': pd.date_range('2020-01-01', periods=100, freq='D')
})

# Create output directory
output_dir = Path('data_export')
output_dir.mkdir(exist_ok=True)

# 1. CSV Export/Import
csv_file = output_dir / 'employees.csv'
sample_data.to_csv(csv_file, index=False)
df_from_csv = pd.read_csv(csv_file, parse_dates=['join_date'])
print("CSV import successful:", df_from_csv.shape)

# 2. Excel Export/Import with multiple sheets
excel_file = output_dir / 'employees.xlsx'
with pd.ExcelWriter(excel_file, engine='openpyxl') as writer:
    sample_data.to_excel(writer, sheet_name='All_Employees', index=False)
    
    # Department-wise sheets
    for dept in sample_data['department'].unique():
        dept_data = sample_data[sample_data['department'] == dept]
        dept_data.to_excel(writer, sheet_name=dept, index=False)

# Read Excel
df_from_excel = pd.read_excel(excel_file, sheet_name='All_Employees')
print("Excel import successful:", df_from_excel.shape)

# 3. JSON Export/Import
json_file = output_dir / 'employees.json'
sample_data.to_json(json_file, orient='records', date_format='iso', indent=2)
df_from_json = pd.read_json(json_file)
print("JSON import successful:", df_from_json.shape)

# 4. Parquet Export/Import (efficient for large datasets)
parquet_file = output_dir / 'employees.parquet'
sample_data.to_parquet(parquet_file, index=False)
df_from_parquet = pd.read_parquet(parquet_file)
print("Parquet import successful:", df_from_parquet.shape)

# 5. Pickle Export/Import (preserves exact Python objects)
pickle_file = output_dir / 'employees.pkl'
sample_data.to_pickle(pickle_file)
df_from_pickle = pd.read_pickle(pickle_file)
print("Pickle import successful:", df_from_pickle.shape)
```

**Database Operations**:
```python
# SQLite operations
db_file = output_dir / 'company.db'
conn = sqlite3.connect(db_file)

# Write to database
sample_data.to_sql('employees', conn, if_exists='replace', index=False)

# Create additional tables
departments = pd.DataFrame({
    'dept_name': ['IT', 'Finance', 'HR', 'Marketing'],
    'budget': [500000, 300000, 200000, 250000],
    'location': ['Building A', 'Building B', 'Building A', 'Building C']
})
departments.to_sql('departments', conn, if_exists='replace', index=False)

# Complex SQL queries
query1 = """
SELECT department, COUNT(*) as employee_count, AVG(salary) as avg_salary
FROM employees 
GROUP BY department
ORDER BY avg_salary DESC
"""
dept_stats = pd.read_sql_query(query1, conn)
print("Department statistics:\n", dept_stats)

query2 = """
SELECT e.name, e.salary, d.budget, d.location
FROM employees e
JOIN departments d ON e.department = d.dept_name
WHERE e.salary > 70000
ORDER BY e.salary DESC
"""
high_earners = pd.read_sql_query(query2, conn)
print("High earners with department info:\n", high_earners.head())

conn.close()
```

**API Data Import**:
```python
import requests
import json

def fetch_api_data(url, params=None):
    """Fetch data from API and convert to DataFrame."""
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        
        if isinstance(data, list):
            return pd.DataFrame(data)
        elif isinstance(data, dict) and 'results' in data:
            return pd.DataFrame(data['results'])
        else:
            return pd.DataFrame([data])
    
    except requests.exceptions.RequestException as e:
        print(f"API request failed: {e}")
        return None

# Example: JSONPlaceholder API (mock data)
def demo_api_import():
    # Fetch posts
    posts_url = "https://jsonplaceholder.typicode.com/posts"
    posts_df = fetch_api_data(posts_url)
    
    if posts_df is not None:
        print("Posts data shape:", posts_df.shape)
        print("Posts columns:", posts_df.columns.tolist())
        
        # Fetch users
        users_url = "https://jsonplaceholder.typicode.com/users"
        users_df = fetch_api_data(users_url)
        
        if users_df is not None:
            print("Users data shape:", users_df.shape)
            
            # Merge posts with users
            merged_df = pd.merge(posts_df, users_df, left_on='userId', right_on='id', 
                               suffixes=('_post', '_user'))
            print("Merged data shape:", merged_df.shape)
            
            return merged_df
    
    return None

# Uncomment to run API demo
# api_data = demo_api_import()
```

**Large File Handling**:
```python
def process_large_csv(file_path, chunk_size=10000):
    """Process large CSV files in chunks."""
    results = []
    
    for chunk in pd.read_csv(file_path, chunksize=chunk_size):
        # Process each chunk
        processed_chunk = chunk.groupby('department')['salary'].mean()
        results.append(processed_chunk)
    
    # Combine results
    final_result = pd.concat(results).groupby(level=0).mean()
    return final_result

# Create large sample file
large_data = pd.DataFrame({
    'id': range(1, 100001),
    'department': np.random.choice(['IT', 'Finance', 'HR', 'Marketing'], 100000),
    'salary': np.random.randint(30000, 120000, 100000)
})

large_file = output_dir / 'large_employees.csv'
large_data.to_csv(large_file, index=False)

# Process in chunks
chunk_result = process_large_csv(large_file, chunk_size=5000)
print("Chunk processing result:\n", chunk_result)
```

---

## 13. Charts and Graphs {#charts-graphs}

### Matplotlib Visualizations

**Basic Plots**:
```python
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

# Set style
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

# Sample data
np.random.seed(42)
x = np.linspace(0, 10, 100)
y1 = np.sin(x)
y2 = np.cos(x)
y3 = np.sin(x) * np.exp(-x/10)

# Create subplots
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(12, 10))

# Line plot
ax1.plot(x, y1, label='sin(x)', linewidth=2)
ax1.plot(x, y2, label='cos(x)', linewidth=2)
ax1.plot(x, y3, label='damped sin(x)', linewidth=2)
ax1.set_title('Line Plots')
ax1.set_xlabel('X values')
ax1.set_ylabel('Y values')
ax1.legend()
ax1.grid(True, alpha=0.3)

# Scatter plot
n = 100
x_scatter = np.random.randn(n)
y_scatter = 2 * x_scatter + np.random.randn(n)
colors = np.random.rand(n)
ax2.scatter(x_scatter, y_scatter, c=colors, alpha=0.6, s=50)
ax2.set_title('Scatter Plot')
ax2.set_xlabel('X values')
ax2.set_ylabel('Y values')

# Histogram
data = np.random.normal(100, 15, 1000)
ax3.hist(data, bins=30, alpha=0.7, color='skyblue', edgecolor='black')
ax3.set_title('Histogram')
ax3.set_xlabel('Values')
ax3.set_ylabel('Frequency')

# Bar plot
categories = ['A', 'B', 'C', 'D', 'E']
values = [23, 45, 56, 78, 32]
ax4.bar(categories, values, color=['red', 'green', 'blue', 'orange', 'purple'])
ax4.set_title('Bar Plot')
ax4.set_xlabel('Categories')
ax4.set_ylabel('Values')

plt.tight_layout()
plt.show()
```

**Advanced Matplotlib**:
```python
# Create sample business data
dates = pd.date_range('2023-01-01', periods=365, freq='D')
sales_data = pd.DataFrame({
    'date': dates,
    'sales': 1000 + np.cumsum(np.random.randn(365) * 10),
    'marketing_spend': np.random.randint(500, 2000, 365),
    'temperature': 20 + 10 * np.sin(2 * np.pi * np.arange(365) / 365) + np.random.randn(365) * 2
})

# Complex visualization
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))

# Time series with dual y-axis
ax1_twin = ax1.twinx()
line1 = ax1.plot(sales_data['date'], sales_data['sales'], 'b-', label='Sales')
line2 = ax1_twin.plot(sales_data['date'], sales_data['marketing_spend'], 'r-', label='Marketing Spend')
ax1.set_xlabel('Date')
ax1.set_ylabel('Sales', color='b')
ax1_twin.set_ylabel('Marketing Spend', color='r')
ax1.set_title('Sales vs Marketing Spend Over Time')

# Add legends
lines = line1 + line2
labels = [l.get_label() for l in lines]
ax1.legend(lines, labels, loc='upper left')

# Correlation scatter with regression line
ax2.scatter(sales_data['marketing_spend'], sales_data['sales'], alpha=0.6)
z = np.polyfit(sales_data['marketing_spend'], sales_data['sales'], 1)
p = np.poly1d(z)
ax2.plot(sales_data['marketing_spend'], p(sales_data['marketing_spend']), "r--", alpha=0.8)
ax2.set_xlabel('Marketing Spend')
ax2.set_ylabel('Sales')
ax2.set_title('Sales vs Marketing Spend Correlation')

# Monthly aggregation
monthly_data = sales_data.set_index('date').resample('M').agg({
    'sales': 'sum',
    'marketing_spend': 'sum'
})
ax3.bar(range(len(monthly_data)), monthly_data['sales'], alpha=0.7)
ax3.set_xlabel('Month')
ax3.set_ylabel('Total Sales')
ax3.set_title('Monthly Sales')
ax3.set_xticks(range(0, len(monthly_data), 2))
ax3.set_xticklabels([d.strftime('%b') for d in monthly_data.index[::2]], rotation=45)

# Distribution comparison
ax4.hist(sales_data['sales'], bins=30, alpha=0.5, label='Sales', density=True)
ax4.hist(sales_data['marketing_spend'], bins=30, alpha=0.5, label='Marketing', density=True)
ax4.set_xlabel('Value')
ax4.set_ylabel('Density')
ax4.set_title('Distribution Comparison')
ax4.legend()

plt.tight_layout()
plt.show()
```

### Seaborn Visualizations

**Statistical Plots**:
```python
# Create sample dataset
np.random.seed(42)
n_samples = 500
df_viz = pd.DataFrame({
    'age': np.random.randint(18, 80, n_samples),
    'income': np.random.lognormal(10, 1, n_samples),
    'education': np.random.choice(['High School', 'Bachelor', 'Master', 'PhD'], n_samples),
    'department': np.random.choice(['IT', 'Finance', 'HR', 'Marketing', 'Sales'], n_samples),
    'experience': np.random.randint(0, 40, n_samples),
    'satisfaction': np.random.randint(1, 11, n_samples)
})

# Add some correlation
df_viz['income'] = df_viz['income'] + df_viz['age'] * 500 + df_viz['experience'] * 1000

# Seaborn plots
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))

# Box plot
sns.boxplot(data=df_viz, x='department', y='income', ax=ax1)
ax1.set_title('Income Distribution by Department')
ax1.tick_params(axis='x', rotation=45)

# Violin plot
sns.violinplot(data=df_viz, x='education', y='satisfaction', ax=ax2)
ax2.set_title('Satisfaction by Education Level')
ax2.tick_params(axis='x', rotation=45)

# Correlation heatmap
numeric_cols = ['age', 'income', 'experience', 'satisfaction']
correlation_matrix = df_viz[numeric_cols].corr()
sns.heatmap(correlation_matrix, annot=True, cmap='coolwarm', center=0, ax=ax3)
ax3.set_title('Correlation Matrix')

# Pair plot (subset)
subset_df = df_viz[['age', 'income', 'experience', 'satisfaction']].sample(200)
sns.scatterplot(data=subset_df, x='age', y='income', size='experience', 
                hue='satisfaction', ax=ax4)
ax4.set_title('Age vs Income (sized by experience, colored by satisfaction)')

plt.tight_layout()
plt.show()

# Additional Seaborn plots
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))

# Count plot
sns.countplot(data=df_viz, x='department', hue='education', ax=ax1)
ax1.set_title('Employee Count by Department and Education')
ax1.tick_params(axis='x', rotation=45)

# Regression plot
sns.regplot(data=df_viz, x='experience', y='income', ax=ax2)
ax2.set_title('Income vs Experience with Regression Line')

# Distribution plot
sns.histplot(data=df_viz, x='income', hue='education', multiple='stack', ax=ax3)
ax3.set_title('Income Distribution by Education')

# Strip plot
sns.stripplot(data=df_viz, x='department', y='satisfaction', 
              size=4, alpha=0.7, ax=ax4)
ax4.set_title('Satisfaction by Department')
ax4.tick_params(axis='x', rotation=45)

plt.tight_layout()
plt.show()
```

**Advanced Seaborn**:
```python
# Create time series data for advanced plots
dates = pd.date_range('2023-01-01', periods=365, freq='D')
time_series_data = pd.DataFrame({
    'date': dates,
    'metric1': np.cumsum(np.random.randn(365)) + 100,
    'metric2': np.cumsum(np.random.randn(365)) + 50,
    'category': np.random.choice(['A', 'B', 'C'], 365),
    'month': dates.month,
    'quarter': dates.quarter
})

# FacetGrid for multiple subplots
g = sns.FacetGrid(time_series_data, col='category', hue='quarter', 
                  col_wrap=2, height=4, aspect=1.2)
g.map(plt.scatter, 'metric1', 'metric2', alpha=0.7)
g.add_legend()
plt.show()

# Joint plot
sns.jointplot(data=df_viz, x='age', y='income', kind='hex', height=8)
plt.show()

# Pair plot
numeric_df = df_viz[['age', 'income', 'experience', 'satisfaction']].sample(200)
sns.pairplot(numeric_df, diag_kind='kde', height=2.5)
plt.show()
```

### Interactive Visualizations with Plotly

**Basic Plotly**:
```python
import plotly.graph_objects as go
import plotly.express as px
from plotly.subplots import make_subplots

# Sample data
df_plotly = px.data.iris()

# Basic scatter plot
fig1 = px.scatter(df_plotly, x='sepal_width', y='sepal_length', 
                  color='species', size='petal_length',
                  title='Iris Dataset - Interactive Scatter Plot')
fig1.show()

# 3D scatter plot
fig2 = px.scatter_3d(df_plotly, x='sepal_length', y='sepal_width', z='petal_length',
                     color='species', title='3D Scatter Plot')
fig2.show()

# Subplots
fig3 = make_subplots(rows=2, cols=2,
                     subplot_titles=('Scatter', 'Bar', 'Histogram', 'Box'))

# Add traces
fig3.add_trace(go.Scatter(x=df_plotly['sepal_length'], y=df_plotly['sepal_width'],
                          mode='markers', name='Scatter'), row=1, col=1)

species_counts = df_plotly['species'].value_counts()
fig3.add_trace(go.Bar(x=species_counts.index, y=species_counts.values,
                      name='Bar'), row=1, col=2)

fig3.add_trace(go.Histogram(x=df_plotly['petal_length'], name='Histogram'),
               row=2, col=1)

fig3.add_trace(go.Box(y=df_plotly['sepal_length'], name='Box'), row=2, col=2)

fig3.update_layout(height=600, showlegend=False, title_text="Multiple Subplots")
fig3.show()
```

---

## 14. R Programming for Data Analytics {#r-programming}

### R Basics

**Data Types and Structures**:
```r
# Basic data types
numeric_var <- 42.5
integer_var <- 42L
character_var <- "Hello R"
logical_var <- TRUE

# Vectors
numeric_vector <- c(1, 2, 3, 4, 5)
character_vector <- c("apple", "banana", "cherry")
logical_vector <- c(TRUE, FALSE, TRUE, FALSE)

# Named vectors
named_vector <- c(a = 1, b = 2, c = 3)

# Lists
my_list <- list(
  numbers = c(1, 2, 3),
  characters = c("a", "b", "c"),
  logical = TRUE
)

# Matrices
matrix_data <- matrix(1:12, nrow = 3, ncol = 4)
print(matrix_data)

# Arrays
array_data <- array(1:24, dim = c(2, 3, 4))

# Data frames
df <- data.frame(
  name = c("Alice", "Bob", "Charlie"),
  age = c(25, 30, 35),
  salary = c(50000, 60000, 70000),
  stringsAsFactors = FALSE
)
print(df)
```

**Data Manipulation with dplyr**:
```r
library(dplyr)
library(ggplot2)

# Create sample data
set.seed(42)
employees <- data.frame(
  id = 1:100,
  name = paste("Employee", 1:100),
  department = sample(c("IT", "Finance", "HR", "Marketing"), 100, replace = TRUE),
  age = sample(22:65, 100, replace = TRUE),
  salary = sample(30000:120000, 100, replace = TRUE),
  experience = sample(0:40, 100, replace = TRUE)
)

# Basic dplyr operations
# Filter
it_employees <- employees %>%
  filter(department == "IT")

# Select
basic_info <- employees %>%
  select(name, department, salary)

# Mutate (create new columns)
employees_enhanced <- employees %>%
  mutate(
    salary_category = case_when(
      salary < 50000 ~ "Low",
      salary < 80000 ~ "Medium",
      TRUE ~ "High"
    ),
    age_group = case_when(
      age < 30 ~ "Young",
      age < 50 ~ "Middle",
      TRUE ~ "Senior"
    )
  )

# Arrange (sort)
sorted_employees <- employees %>%
  arrange(desc(salary), age)

# Group by and summarize
dept_summary <- employees %>%
  group_by(department) %>%
  summarise(
    count = n(),
    avg_salary = mean(salary),
    median_age = median(age),
    total_experience = sum(experience),
    .groups = 'drop'
  )

print(dept_summary)

# Chain operations
analysis_result <- employees %>%
  filter(age > 30) %>%
  mutate(salary_per_year_exp = salary / (experience + 1)) %>%
  group_by(department) %>%
  summarise(
    avg_salary_per_exp = mean(salary_per_year_exp),
    count = n(),
    .groups = 'drop'
  ) %>%
  arrange(desc(avg_salary_per_exp))

print(analysis_result)
```

**Data Reshaping with tidyr**:
```r
library(tidyr)

# Wide to long format
wide_data <- data.frame(
  id = 1:5,
  name = paste("Person", 1:5),
  Q1 = c(100, 150, 200, 120, 180),
  Q2 = c(110, 160, 190, 130, 185),
  Q3 = c(120, 170, 210, 140, 190),
  Q4 = c(130, 180, 220, 150, 195)
)

# Pivot longer
long_data <- wide_data %>%
  pivot_longer(
    cols = starts_with("Q"),
    names_to = "quarter",
    values_to = "sales"
  )

print(head(long_data, 10))

# Pivot wider (back to wide format)
wide_again <- long_data %>%
  pivot_wider(
    names_from = quarter,
    values_from = sales
  )

print(wide_again)

# Separate and unite
data_with_names <- data.frame(
  full_name = c("John Doe", "Jane Smith", "Bob Johnson"),
  score = c(85, 92, 78)
)

# Separate
separated_data <- data_with_names %>%
  separate(full_name, into = c("first_name", "last_name"), sep = " ")

print(separated_data)

# Unite
united_data <- separated_data %>%
  unite("full_name", first_name, last_name, sep = " ")

print(united_data)
```

### R Visualization with ggplot2

**Basic ggplot2**:
```r
library(ggplot2)

# Basic scatter plot
p1 <- ggplot(employees, aes(x = age, y = salary)) +
  geom_point(aes(color = department), alpha = 0.7) +
  geom_smooth(method = "lm", se = FALSE) +
  labs(title = "Age vs Salary by Department",
       x = "Age", y = "Salary") +
  theme_minimal()

print(p1)

# Box plot
p2 <- ggplot(employees, aes(x = department, y = salary)) +
  geom_boxplot(aes(fill = department)) +
  geom_jitter(width = 0.2, alpha = 0.5) +
  labs(title = "Salary Distribution by Department",
       x = "Department", y = "Salary") +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))

print(p2)

# Histogram with facets
p3 <- ggplot(employees_enhanced, aes(x = salary)) +
  geom_histogram(bins = 20, fill = "skyblue", alpha = 0.7) +
  facet_wrap(~department, scales = "free_y") +
  labs(title = "Salary Distribution by Department",
       x = "Salary", y = "Count") +
  theme_minimal()

print(p3)

# Bar plot
p4 <- dept_summary %>%
  ggplot(aes(x = reorder(department, avg_salary), y = avg_salary)) +
  geom_col(fill = "steelblue") +
  geom_text(aes(label = round(avg_salary)), vjust = -0.5) +
  labs(title = "Average Salary by Department",
       x = "Department", y = "Average Salary") +
  theme_minimal() +
  coord_flip()

print(p4)
```

**Advanced ggplot2**:
```r
# Create time series data
dates <- seq(as.Date("2023-01-01"), as.Date("2023-12-31"), by = "day")
time_series <- data.frame(
  date = dates,
  sales = cumsum(rnorm(365, mean = 10, sd = 5)) + 1000,
  marketing = sample(500:2000, 365, replace = TRUE),
  season = case_when(
    month(dates) %in% c(12, 1, 2) ~ "Winter",
    month(dates) %in% c(3, 4, 5) ~ "Spring",
    month(dates) %in% c(6, 7, 8) ~ "Summer",
    TRUE ~ "Fall"
  )
)

# Multi-layer plot
p5 <- ggplot(time_series, aes(x = date)) +
  geom_line(aes(y = sales, color = "Sales"), size = 1) +
  geom_smooth(aes(y = sales), method = "loess", se = FALSE, color = "red") +
  scale_color_manual(values = c("Sales" = "blue")) +
  labs(title = "Sales Trend Over Time",
       x = "Date", y = "Sales", color = "Metric") +
  theme_minimal() +
  theme(legend.position = "bottom")

print(p5)

# Correlation plot
library(corrplot)
numeric_cols <- employees[, c("age", "salary", "experience")]
correlation_matrix <- cor(numeric_cols)
corrplot(correlation_matrix, method = "circle", type = "upper")
```

### Statistical Analysis in R

**Descriptive Statistics**:
```r
# Summary statistics
summary(employees$salary)

# Custom summary function
custom_summary <- function(x) {
  c(
    mean = mean(x, na.rm = TRUE),
    median = median(x, na.rm = TRUE),
    sd = sd(x, na.rm = TRUE),
    min = min(x, na.rm = TRUE),
    max = max(x, na.rm = TRUE),
    q25 = quantile(x, 0.25, na.rm = TRUE),
    q75 = quantile(x, 0.75, na.rm = TRUE)
  )
}

salary_stats <- custom_summary(employees$salary)
print(salary_stats)

# Group-wise statistics
group_stats <- employees %>%
  group_by(department) %>%
  summarise(
    across(c(age, salary, experience), 
           list(mean = mean, median = median, sd = sd),
           .names = "{.col}_{.fn}"),
    .groups = 'drop'
  )

print(group_stats)
```

**Hypothesis Testing**:
```r
# T-test
it_salaries <- employees$salary[employees$department == "IT"]
finance_salaries <- employees$salary[employees$department == "Finance"]

t_test_result <- t.test(it_salaries, finance_salaries)
print(t_test_result)

# ANOVA
anova_result <- aov(salary ~ department, data = employees)
summary(anova_result)

# Chi-square test
contingency_table <- table(employees_enhanced$department, employees_enhanced$salary_category)
chi_square_result <- chisq.test(contingency_table)
print(chi_square_result)

# Correlation test
cor_test_result <- cor.test(employees$age, employees$salary)
print(cor_test_result)
```

---

## 15. MCQ Practice Questions {#mcq-questions}

### Questions 1-10: Python Basics and Data Structures

**1. What is the output of the following Python code?**
```python
data = [1, 2, 3, 4, 5]
result = data[1:4:2]
print(result)
```
a) [1, 3]
b) [2, 4]
c) [1, 2, 3, 4]
d) [2, 3, 4]

**Answer: b) [2, 4]**
**Explanation**: Slicing [1:4:2] starts at index 1, ends before index 4, with step 2, giving elements at indices 1 and 3.

**2. Which pandas method is used to handle missing values by forward filling?**
a) fillna(method='bfill')
b) fillna(method='ffill')
c) dropna()
d) interpolate()

**Answer: b) fillna(method='ffill')**
**Explanation**: The fillna(method='ffill') method performs forward fill, using the previous valid value to fill missing values.

**3. What does the following regex pattern match: `r'\d{3}-\d{2}-\d{4}'`?**
a) Phone numbers
b) Social Security Numbers
c) Credit card numbers
d) ZIP codes

**Answer: b) Social Security Numbers**
**Explanation**: The pattern matches 3 digits, hyphen, 2 digits, hyphen, 4 digits, which is the format of US Social Security Numbers.

**4. In pandas, what is the difference between loc and iloc?**
a) No difference
b) loc uses labels, iloc uses integer positions
c) iloc uses labels, loc uses integer positions
d) Both use only integer positions

**Answer: b) loc uses labels, iloc uses integer positions**
**Explanation**: loc is label-based indexing, while iloc is integer position-based indexing.

**5. Which method converts a pandas DataFrame from wide to long format?**
a) pivot()
b) melt()
c) stack()
d) unstack()

**Answer: b) melt()**
**Explanation**: The melt() method transforms wide format data to long format by unpivoting columns into rows.

**6. What is the purpose of the `groupby()` operation in pandas?**
a) Sort data
b) Filter data
c) Split data into groups for analysis
d) Merge datasets

**Answer: c) Split data into groups for analysis**
**Explanation**: groupby() splits data into groups based on specified criteria, allowing for group-wise operations and analysis.

**7. Which Python library is primarily used for statistical modeling?**
a) NumPy
b) Pandas
c) Matplotlib
d) Scikit-learn

**Answer: d) Scikit-learn**
**Explanation**: Scikit-learn is the primary library for machine learning and statistical modeling in Python.

**8. What does the following list comprehension do?**
```python
result = [x**2 for x in range(10) if x % 2 == 0]
```
a) Squares all numbers from 0 to 9
b) Squares even numbers from 0 to 9
c) Squares odd numbers from 0 to 9
d) Creates a list of even numbers

**Answer: b) Squares even numbers from 0 to 9**
**Explanation**: The comprehension squares (x**2) only the even numbers (x % 2 == 0) from range(10).

**9. In matplotlib, which function creates subplots?**
a) subplot()
b) subplots()
c) figure()
d) plot()

**Answer: b) subplots()**
**Explanation**: The subplots() function creates a figure with multiple subplots and returns figure and axes objects.

**10. What is the correct way to read a CSV file with pandas?**
a) pd.read_csv('file.csv')
b) pd.load_csv('file.csv')
c) pd.import_csv('file.csv')
d) pd.open_csv('file.csv')

**Answer: a) pd.read_csv('file.csv')**
**Explanation**: pd.read_csv() is the correct pandas function to read CSV files.

### Questions 11-20: Advanced Data Analytics

**11. Which R package is used for data manipulation?**
a) ggplot2
b) dplyr
c) tidyr
d) Both b and c

**Answer: d) Both b and c**
**Explanation**: Both dplyr (for data manipulation) and tidyr (for data reshaping) are used for data manipulation tasks.

**12. What does PCA stand for in machine learning?**
a) Principal Component Analysis
b) Primary Classification Algorithm
c) Predictive Clustering Analysis
d) Probabilistic Component Assessment

**Answer: a) Principal Component Analysis**
**Explanation**: PCA is a dimensionality reduction technique that finds principal components in data.

**13. Which evaluation metric is appropriate for regression problems?**
a) Accuracy
b) Precision
c) Mean Squared Error
d) F1-score

**Answer: c) Mean Squared Error**
**Explanation**: MSE is a common metric for regression problems, measuring the average squared differences between predicted and actual values.

**14. In seaborn, which function creates a correlation heatmap?**
a) heatmap()
b) corrplot()
c) corr_heatmap()
d) correlation_plot()

**Answer: a) heatmap()**
**Explanation**: sns.heatmap() is used to create heatmaps, including correlation matrices.

**15. What is the purpose of cross-validation in machine learning?**
a) To increase model complexity
b) To assess model performance on unseen data
c) To reduce training time
d) To increase dataset size

**Answer: b) To assess model performance on unseen data**
**Explanation**: Cross-validation helps evaluate how well a model generalizes to unseen data by using different train/test splits.

**16. Which pandas method is used to combine DataFrames horizontally?**
a) concat() with axis=0
b) concat() with axis=1
c) merge()
d) Both b and c

**Answer: d) Both b and c**
**Explanation**: Both concat() with axis=1 and merge() can combine DataFrames horizontally, though they work differently.

**17. In R, what does the pipe operator %>% do?**
a) Creates a new variable
b) Passes the result of one function to the next
c) Performs mathematical operations
d) Creates plots

**Answer: b) Passes the result of one function to the next**
**Explanation**: The pipe operator %>% passes the output of one function as input to the next function, enabling chained operations.

**18. Which clustering algorithm requires specifying the number of clusters beforehand?**
a) DBSCAN
b) K-means
c) Hierarchical clustering
d) Mean shift

**Answer: b) K-means**
**Explanation**: K-means requires you to specify the number of clusters (k) before running the algorithm.

**19. What is the difference between supervised and unsupervised learning?**
a) Supervised uses more data
b) Supervised has labeled target variables, unsupervised doesn't
c) Unsupervised is more accurate
d) No difference

**Answer: b) Supervised has labeled target variables, unsupervised doesn't**
**Explanation**: Supervised learning uses labeled data with known outcomes, while unsupervised learning finds patterns in unlabeled data.

**20. Which file format is most efficient for storing large datasets?**
a) CSV
b) JSON
c) Parquet
d) Excel

**Answer: c) Parquet**
**Explanation**: Parquet is a columnar storage format that provides efficient compression and fast read/write operations for large datasets.

---

## Study Tips for Data Analytics Languages

### Key Areas to Focus On

**1. Python Fundamentals**
- Master data structures: lists, dictionaries, sets
- Understand list comprehensions and lambda functions
- Practice regex for data cleaning

**2. Pandas Mastery**
- Learn DataFrame operations: indexing, filtering, grouping
- Master data reshaping: pivot, melt, stack/unstack
- Practice handling missing data and merging datasets

**3. Visualization Skills**
- Understand when to use different chart types
- Master both matplotlib and seaborn
- Learn to create clear, informative visualizations

**4. R Programming**
- Master dplyr for data manipulation
- Learn ggplot2 for visualization
- Understand tidyr for data reshaping

**5. Machine Learning Basics**
- Understand supervised vs unsupervised learning
- Know common algorithms and their use cases
- Practice model evaluation and validation

### Best Practices

**1. Data Cleaning**
- Always explore data before analysis
- Handle missing values appropriately
- Validate data quality and consistency

**2. Code Organization**
- Write readable, well-commented code
- Use meaningful variable names
- Follow PEP 8 style guide for Python

**3. Visualization**
- Choose appropriate chart types for data
- Use clear titles and labels
- Consider color accessibility

**4. Analysis Workflow**
- Start with exploratory data analysis
- Document assumptions and methodology
- Validate results and check for errors

---

**End of Data Analytics Languages Guide**

This comprehensive guide covers essential data analytics concepts in both Python and R, from basic programming to advanced machine learning and visualization techniques. Practice with real datasets and focus on understanding the underlying concepts rather than just memorizing syntax.