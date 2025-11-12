# AI/ML Advanced Concepts Guide - Part 1

*Comprehensive guide covering machine learning fundamentals, supervised/unsupervised learning, and data preprocessing*

## Table of Contents (Part 1)
1. [Machine Learning Fundamentals](#ml-fundamentals)
2. [Supervised Learning](#supervised-learning)
3. [Unsupervised Learning](#unsupervised-learning)
4. [Data Preprocessing](#data-preprocessing)
5. [Model Evaluation](#model-evaluation)

---

## 1. Machine Learning Fundamentals {#ml-fundamentals}

### What is Machine Learning?

**Definition**: Machine Learning is a subset of AI that enables computers to learn and make decisions from data without being explicitly programmed for every task.

**Types of Machine Learning**:
1. **Supervised Learning**: Learning with labeled data
2. **Unsupervised Learning**: Finding patterns in unlabeled data
3. **Reinforcement Learning**: Learning through interaction and rewards
4. **Semi-supervised Learning**: Combination of labeled and unlabeled data

### ML Workflow

```python
# Complete ML Pipeline Example
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report

# 1. Data Collection and Loading
def load_data():
    # Example with built-in dataset
    from sklearn.datasets import load_iris
    data = load_iris()
    X = pd.DataFrame(data.data, columns=data.feature_names)
    y = pd.Series(data.target, name='target')
    return X, y

# 2. Data Preprocessing
def preprocess_data(X, y):
    # Handle missing values
    X = X.fillna(X.mean())
    
    # Feature scaling
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    return X_scaled, y, scaler

# 3. Model Training and Evaluation
def train_evaluate_model(X, y):
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    
    # Train model
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)
    
    # Evaluate
    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    
    return model, accuracy, y_test, y_pred

# Complete pipeline
X, y = load_data()
X_processed, y_processed, scaler = preprocess_data(X, y)
model, accuracy, y_test, y_pred = train_evaluate_model(X_processed, y_processed)

print(f"Model Accuracy: {accuracy:.3f}")
print("\nClassification Report:")
print(classification_report(y_test, y_pred))
```

### Key ML Concepts

**Bias-Variance Tradeoff**:
```python
import matplotlib.pyplot as plt
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error

def bias_variance_demo():
    # Generate synthetic data
    np.random.seed(42)
    X = np.linspace(0, 1, 100).reshape(-1, 1)
    y = 0.5 * X.ravel() + 0.3 * np.sin(2 * np.pi * X.ravel()) + np.random.normal(0, 0.1, 100)
    
    # Different model complexities
    complexities = [1, 5, 10, 50, 100]
    bias_scores = []
    variance_scores = []
    
    for n_estimators in complexities:
        model = RandomForestRegressor(n_estimators=n_estimators, random_state=42)
        
        # Multiple training runs to measure variance
        predictions = []
        for seed in range(10):
            # Bootstrap sampling
            indices = np.random.choice(len(X), size=len(X), replace=True)
            X_boot, y_boot = X[indices], y[indices]
            
            model.fit(X_boot, y_boot)
            pred = model.predict(X)
            predictions.append(pred)
        
        predictions = np.array(predictions)
        
        # Calculate bias and variance
        mean_pred = np.mean(predictions, axis=0)
        bias = np.mean((mean_pred - y) ** 2)
        variance = np.mean(np.var(predictions, axis=0))
        
        bias_scores.append(bias)
        variance_scores.append(variance)
    
    return complexities, bias_scores, variance_scores

# complexities, bias, variance = bias_variance_demo()
```

---

## 2. Supervised Learning {#supervised-learning}

### Linear Regression

```python
import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, r2_score
import matplotlib.pyplot as plt

class LinearRegressionFromScratch:
    def __init__(self):
        self.weights = None
        self.bias = None
        
    def fit(self, X, y):
        # Add bias term
        X_with_bias = np.c_[np.ones(X.shape[0]), X]
        
        # Normal equation: θ = (X^T X)^(-1) X^T y
        self.weights = np.linalg.inv(X_with_bias.T @ X_with_bias) @ X_with_bias.T @ y
        self.bias = self.weights[0]
        self.weights = self.weights[1:]
        
    def predict(self, X):
        return X @ self.weights + self.bias
    
    def score(self, X, y):
        y_pred = self.predict(X)
        ss_res = np.sum((y - y_pred) ** 2)
        ss_tot = np.sum((y - np.mean(y)) ** 2)
        return 1 - (ss_res / ss_tot)

# Example usage
def linear_regression_example():
    # Generate sample data
    np.random.seed(42)
    X = np.random.randn(100, 1)
    y = 2 * X.ravel() + 1 + np.random.randn(100) * 0.1
    
    # Custom implementation
    model_custom = LinearRegressionFromScratch()
    model_custom.fit(X, y)
    
    # Scikit-learn implementation
    model_sklearn = LinearRegression()
    model_sklearn.fit(X, y)
    
    # Predictions
    y_pred_custom = model_custom.predict(X)
    y_pred_sklearn = model_sklearn.predict(X)
    
    print("Custom Implementation:")
    print(f"Weight: {model_custom.weights[0]:.3f}, Bias: {model_custom.bias:.3f}")
    print(f"R² Score: {model_custom.score(X, y):.3f}")
    
    print("\nScikit-learn Implementation:")
    print(f"Weight: {model_sklearn.coef_[0]:.3f}, Bias: {model_sklearn.intercept_:.3f}")
    print(f"R² Score: {model_sklearn.score(X, y):.3f}")

linear_regression_example()
```

### Logistic Regression

```python
from sklearn.linear_model import LogisticRegression
from sklearn.datasets import make_classification
from sklearn.metrics import accuracy_score, confusion_matrix, roc_auc_score

class LogisticRegressionFromScratch:
    def __init__(self, learning_rate=0.01, max_iterations=1000):
        self.learning_rate = learning_rate
        self.max_iterations = max_iterations
        self.weights = None
        self.bias = None
        
    def sigmoid(self, z):
        # Clip z to prevent overflow
        z = np.clip(z, -500, 500)
        return 1 / (1 + np.exp(-z))
    
    def fit(self, X, y):
        n_samples, n_features = X.shape
        
        # Initialize parameters
        self.weights = np.zeros(n_features)
        self.bias = 0
        
        # Gradient descent
        for i in range(self.max_iterations):
            # Forward pass
            z = X @ self.weights + self.bias
            y_pred = self.sigmoid(z)
            
            # Compute cost
            cost = -np.mean(y * np.log(y_pred + 1e-15) + (1 - y) * np.log(1 - y_pred + 1e-15))
            
            # Compute gradients
            dw = (1 / n_samples) * X.T @ (y_pred - y)
            db = (1 / n_samples) * np.sum(y_pred - y)
            
            # Update parameters
            self.weights -= self.learning_rate * dw
            self.bias -= self.learning_rate * db
            
    def predict_proba(self, X):
        z = X @ self.weights + self.bias
        return self.sigmoid(z)
    
    def predict(self, X):
        return (self.predict_proba(X) >= 0.5).astype(int)

# Example usage
def logistic_regression_example():
    # Generate sample data
    X, y = make_classification(n_samples=1000, n_features=2, n_redundant=0, 
                             n_informative=2, n_clusters_per_class=1, random_state=42)
    
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Custom implementation
    model_custom = LogisticRegressionFromScratch(learning_rate=0.1, max_iterations=1000)
    model_custom.fit(X_train, y_train)
    
    # Scikit-learn implementation
    model_sklearn = LogisticRegression(random_state=42)
    model_sklearn.fit(X_train, y_train)
    
    # Predictions
    y_pred_custom = model_custom.predict(X_test)
    y_pred_sklearn = model_sklearn.predict(X_test)
    
    print("Custom Logistic Regression:")
    print(f"Accuracy: {accuracy_score(y_test, y_pred_custom):.3f}")
    print(f"AUC: {roc_auc_score(y_test, model_custom.predict_proba(X_test)):.3f}")
    
    print("\nScikit-learn Logistic Regression:")
    print(f"Accuracy: {accuracy_score(y_test, y_pred_sklearn):.3f}")
    print(f"AUC: {roc_auc_score(y_test, model_sklearn.predict_proba(X_test)[:, 1]):.3f}")

logistic_regression_example()
```

### Decision Trees

```python
from sklearn.tree import DecisionTreeClassifier, export_text
from sklearn.datasets import load_iris
import numpy as np

class DecisionTreeNode:
    def __init__(self, feature=None, threshold=None, left=None, right=None, value=None):
        self.feature = feature
        self.threshold = threshold
        self.left = left
        self.right = right
        self.value = value
        
    def is_leaf(self):
        return self.value is not None

class DecisionTreeFromScratch:
    def __init__(self, max_depth=10, min_samples_split=2):
        self.max_depth = max_depth
        self.min_samples_split = min_samples_split
        self.root = None
        
    def gini_impurity(self, y):
        """Calculate Gini impurity"""
        classes, counts = np.unique(y, return_counts=True)
        probabilities = counts / len(y)
        return 1 - np.sum(probabilities ** 2)
    
    def information_gain(self, X_column, y, threshold):
        """Calculate information gain for a split"""
        # Parent impurity
        parent_gini = self.gini_impurity(y)
        
        # Split data
        left_mask = X_column <= threshold
        right_mask = ~left_mask
        
        if np.sum(left_mask) == 0 or np.sum(right_mask) == 0:
            return 0
        
        # Weighted average of children impurities
        n = len(y)
        left_gini = self.gini_impurity(y[left_mask])
        right_gini = self.gini_impurity(y[right_mask])
        
        weighted_gini = (np.sum(left_mask) / n) * left_gini + (np.sum(right_mask) / n) * right_gini
        
        return parent_gini - weighted_gini
    
    def best_split(self, X, y):
        """Find the best split"""
        best_gain = -1
        best_feature = None
        best_threshold = None
        
        n_features = X.shape[1]
        
        for feature in range(n_features):
            thresholds = np.unique(X[:, feature])
            
            for threshold in thresholds:
                gain = self.information_gain(X[:, feature], y, threshold)
                
                if gain > best_gain:
                    best_gain = gain
                    best_feature = feature
                    best_threshold = threshold
        
        return best_feature, best_threshold, best_gain
    
    def build_tree(self, X, y, depth=0):
        """Recursively build the decision tree"""
        n_samples, n_features = X.shape
        n_classes = len(np.unique(y))
        
        # Stopping criteria
        if (depth >= self.max_depth or 
            n_samples < self.min_samples_split or 
            n_classes == 1):
            # Create leaf node
            most_common_class = np.bincount(y).argmax()
            return DecisionTreeNode(value=most_common_class)
        
        # Find best split
        best_feature, best_threshold, best_gain = self.best_split(X, y)
        
        if best_gain == 0:
            # No good split found
            most_common_class = np.bincount(y).argmax()
            return DecisionTreeNode(value=most_common_class)
        
        # Split data
        left_mask = X[:, best_feature] <= best_threshold
        right_mask = ~left_mask
        
        # Recursively build subtrees
        left_subtree = self.build_tree(X[left_mask], y[left_mask], depth + 1)
        right_subtree = self.build_tree(X[right_mask], y[right_mask], depth + 1)
        
        return DecisionTreeNode(best_feature, best_threshold, left_subtree, right_subtree)
    
    def fit(self, X, y):
        self.root = self.build_tree(X, y)
    
    def predict_sample(self, x, node):
        """Predict a single sample"""
        if node.is_leaf():
            return node.value
        
        if x[node.feature] <= node.threshold:
            return self.predict_sample(x, node.left)
        else:
            return self.predict_sample(x, node.right)
    
    def predict(self, X):
        return np.array([self.predict_sample(x, self.root) for x in X])

# Example usage
def decision_tree_example():
    # Load data
    data = load_iris()
    X, y = data.data, data.target
    
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Custom implementation
    model_custom = DecisionTreeFromScratch(max_depth=5)
    model_custom.fit(X_train, y_train)
    
    # Scikit-learn implementation
    model_sklearn = DecisionTreeClassifier(max_depth=5, random_state=42)
    model_sklearn.fit(X_train, y_train)
    
    # Predictions
    y_pred_custom = model_custom.predict(X_test)
    y_pred_sklearn = model_sklearn.predict(X_test)
    
    print("Custom Decision Tree:")
    print(f"Accuracy: {accuracy_score(y_test, y_pred_custom):.3f}")
    
    print("\nScikit-learn Decision Tree:")
    print(f"Accuracy: {accuracy_score(y_test, y_pred_sklearn):.3f}")
    
    # Print tree structure (sklearn)
    print("\nTree Structure (first few rules):")
    tree_rules = export_text(model_sklearn, feature_names=data.feature_names)
    print(tree_rules[:500] + "...")

decision_tree_example()
```

---

## 3. Unsupervised Learning {#unsupervised-learning}

### K-Means Clustering

```python
from sklearn.cluster import KMeans
from sklearn.datasets import make_blobs
import matplotlib.pyplot as plt

class KMeansFromScratch:
    def __init__(self, k=3, max_iterations=100, random_state=None):
        self.k = k
        self.max_iterations = max_iterations
        self.random_state = random_state
        
    def initialize_centroids(self, X):
        """Initialize centroids randomly"""
        if self.random_state:
            np.random.seed(self.random_state)
        
        n_samples, n_features = X.shape
        centroids = np.zeros((self.k, n_features))
        
        for i in range(self.k):
            centroid = X[np.random.choice(n_samples)]
            centroids[i] = centroid
            
        return centroids
    
    def assign_clusters(self, X, centroids):
        """Assign each point to the closest centroid"""
        distances = np.sqrt(((X - centroids[:, np.newaxis])**2).sum(axis=2))
        return np.argmin(distances, axis=0)
    
    def update_centroids(self, X, labels):
        """Update centroids based on current assignments"""
        centroids = np.zeros((self.k, X.shape[1]))
        
        for i in range(self.k):
            if np.sum(labels == i) > 0:
                centroids[i] = X[labels == i].mean(axis=0)
            
        return centroids
    
    def fit(self, X):
        # Initialize centroids
        self.centroids = self.initialize_centroids(X)
        
        for _ in range(self.max_iterations):
            # Assign points to clusters
            labels = self.assign_clusters(X, self.centroids)
            
            # Update centroids
            new_centroids = self.update_centroids(X, labels)
            
            # Check for convergence
            if np.allclose(self.centroids, new_centroids):
                break
                
            self.centroids = new_centroids
        
        self.labels_ = labels
        return self
    
    def predict(self, X):
        return self.assign_clusters(X, self.centroids)

# Example usage
def kmeans_example():
    # Generate sample data
    X, y_true = make_blobs(n_samples=300, centers=4, cluster_std=0.60, 
                          random_state=42)
    
    # Custom implementation
    kmeans_custom = KMeansFromScratch(k=4, random_state=42)
    kmeans_custom.fit(X)
    
    # Scikit-learn implementation
    kmeans_sklearn = KMeans(n_clusters=4, random_state=42, n_init=10)
    kmeans_sklearn.fit(X)
    
    print("Custom K-Means:")
    print(f"Centroids shape: {kmeans_custom.centroids.shape}")
    print(f"Unique labels: {np.unique(kmeans_custom.labels_)}")
    
    print("\nScikit-learn K-Means:")
    print(f"Centroids shape: {kmeans_sklearn.cluster_centers_.shape}")
    print(f"Inertia: {kmeans_sklearn.inertia_:.2f}")

kmeans_example()
```

### Hierarchical Clustering

```python
from sklearn.cluster import AgglomerativeClustering
from scipy.cluster.hierarchy import dendrogram, linkage
from scipy.spatial.distance import pdist, squareform

class HierarchicalClustering:
    def __init__(self, linkage_method='ward'):
        self.linkage_method = linkage_method
        self.linkage_matrix = None
        
    def fit(self, X):
        """Perform hierarchical clustering"""
        # Calculate distance matrix
        if self.linkage_method == 'ward':
            self.linkage_matrix = linkage(X, method='ward')
        else:
            distances = pdist(X)
            self.linkage_matrix = linkage(distances, method=self.linkage_method)
        
        return self
    
    def get_clusters(self, n_clusters):
        """Get cluster labels for specified number of clusters"""
        from scipy.cluster.hierarchy import fcluster
        return fcluster(self.linkage_matrix, n_clusters, criterion='maxclust') - 1

# Example usage
def hierarchical_clustering_example():
    # Generate sample data
    X, _ = make_blobs(n_samples=50, centers=3, cluster_std=1.0, random_state=42)
    
    # Custom implementation
    hc_custom = HierarchicalClustering(linkage_method='ward')
    hc_custom.fit(X)
    labels_custom = hc_custom.get_clusters(3)
    
    # Scikit-learn implementation
    hc_sklearn = AgglomerativeClustering(n_clusters=3, linkage='ward')
    labels_sklearn = hc_sklearn.fit_predict(X)
    
    print("Custom Hierarchical Clustering:")
    print(f"Unique labels: {np.unique(labels_custom)}")
    
    print("\nScikit-learn Hierarchical Clustering:")
    print(f"Unique labels: {np.unique(labels_sklearn)}")

hierarchical_clustering_example()
```

---

## 4. Data Preprocessing {#data-preprocessing}

### Feature Scaling and Normalization

```python
from sklearn.preprocessing import StandardScaler, MinMaxScaler, RobustScaler
import pandas as pd

class DataPreprocessor:
    def __init__(self):
        self.scalers = {}
        self.encoders = {}
        
    def handle_missing_values(self, df, strategy='mean'):
        """Handle missing values"""
        df_processed = df.copy()
        
        for column in df_processed.columns:
            if df_processed[column].isnull().any():
                if df_processed[column].dtype in ['int64', 'float64']:
                    if strategy == 'mean':
                        df_processed[column].fillna(df_processed[column].mean(), inplace=True)
                    elif strategy == 'median':
                        df_processed[column].fillna(df_processed[column].median(), inplace=True)
                    elif strategy == 'mode':
                        df_processed[column].fillna(df_processed[column].mode()[0], inplace=True)
                else:
                    # Categorical data
                    df_processed[column].fillna(df_processed[column].mode()[0], inplace=True)
        
        return df_processed
    
    def detect_outliers(self, df, method='iqr'):
        """Detect outliers using IQR or Z-score method"""
        outliers = {}
        
        for column in df.select_dtypes(include=[np.number]).columns:
            if method == 'iqr':
                Q1 = df[column].quantile(0.25)
                Q3 = df[column].quantile(0.75)
                IQR = Q3 - Q1
                lower_bound = Q1 - 1.5 * IQR
                upper_bound = Q3 + 1.5 * IQR
                
                outliers[column] = df[(df[column] < lower_bound) | 
                                    (df[column] > upper_bound)].index.tolist()
            
            elif method == 'zscore':
                z_scores = np.abs((df[column] - df[column].mean()) / df[column].std())
                outliers[column] = df[z_scores > 3].index.tolist()
        
        return outliers
    
    def scale_features(self, X_train, X_test=None, method='standard'):
        """Scale features using different methods"""
        if method == 'standard':
            scaler = StandardScaler()
        elif method == 'minmax':
            scaler = MinMaxScaler()
        elif method == 'robust':
            scaler = RobustScaler()
        
        X_train_scaled = scaler.fit_transform(X_train)
        
        if X_test is not None:
            X_test_scaled = scaler.transform(X_test)
            return X_train_scaled, X_test_scaled, scaler
        
        return X_train_scaled, scaler
    
    def encode_categorical(self, df, columns, method='onehot'):
        """Encode categorical variables"""
        df_encoded = df.copy()
        
        if method == 'onehot':
            df_encoded = pd.get_dummies(df_encoded, columns=columns, drop_first=True)
        
        elif method == 'label':
            from sklearn.preprocessing import LabelEncoder
            for column in columns:
                le = LabelEncoder()
                df_encoded[column] = le.fit_transform(df_encoded[column])
                self.encoders[column] = le
        
        return df_encoded

# Example usage
def preprocessing_example():
    # Create sample data with missing values and outliers
    np.random.seed(42)
    data = {
        'age': np.random.normal(30, 10, 1000),
        'income': np.random.normal(50000, 15000, 1000),
        'category': np.random.choice(['A', 'B', 'C'], 1000),
        'score': np.random.normal(75, 15, 1000)
    }
    
    # Introduce missing values
    missing_indices = np.random.choice(1000, 50, replace=False)
    data['age'][missing_indices[:25]] = np.nan
    data['income'][missing_indices[25:]] = np.nan
    
    # Introduce outliers
    data['income'][np.random.choice(1000, 10, replace=False)] = np.random.normal(200000, 10000, 10)
    
    df = pd.DataFrame(data)
    
    # Initialize preprocessor
    preprocessor = DataPreprocessor()
    
    # Handle missing values
    df_clean = preprocessor.handle_missing_values(df, strategy='mean')
    print(f"Missing values before: {df.isnull().sum().sum()}")
    print(f"Missing values after: {df_clean.isnull().sum().sum()}")
    
    # Detect outliers
    outliers = preprocessor.detect_outliers(df_clean, method='iqr')
    print(f"\nOutliers detected:")
    for col, indices in outliers.items():
        print(f"{col}: {len(indices)} outliers")
    
    # Encode categorical variables
    df_encoded = preprocessor.encode_categorical(df_clean, ['category'], method='onehot')
    print(f"\nColumns after encoding: {df_encoded.columns.tolist()}")
    
    # Scale features
    numeric_columns = ['age', 'income', 'score']
    X = df_encoded[numeric_columns]
    X_scaled, scaler = preprocessor.scale_features(X, method='standard')
    
    print(f"\nOriginal data stats:")
    print(f"Mean: {X.mean().values}")
    print(f"Std: {X.std().values}")
    
    print(f"\nScaled data stats:")
    print(f"Mean: {X_scaled.mean(axis=0)}")
    print(f"Std: {X_scaled.std(axis=0)}")

preprocessing_example()
```

---

## 5. Model Evaluation {#model-evaluation}

### Classification Metrics

```python
from sklearn.metrics import (accuracy_score, precision_score, recall_score, 
                           f1_score, confusion_matrix, roc_auc_score, 
                           classification_report, roc_curve)

class ModelEvaluator:
    def __init__(self):
        self.metrics = {}
    
    def evaluate_classification(self, y_true, y_pred, y_pred_proba=None):
        """Comprehensive classification evaluation"""
        metrics = {}
        
        # Basic metrics
        metrics['accuracy'] = accuracy_score(y_true, y_pred)
        metrics['precision'] = precision_score(y_true, y_pred, average='weighted')
        metrics['recall'] = recall_score(y_true, y_pred, average='weighted')
        metrics['f1'] = f1_score(y_true, y_pred, average='weighted')
        
        # Confusion matrix
        metrics['confusion_matrix'] = confusion_matrix(y_true, y_pred)
        
        # ROC AUC (for binary classification)
        if len(np.unique(y_true)) == 2 and y_pred_proba is not None:
            metrics['roc_auc'] = roc_auc_score(y_true, y_pred_proba)
        
        return metrics
    
    def evaluate_regression(self, y_true, y_pred):
        """Comprehensive regression evaluation"""
        from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
        
        metrics = {}
        metrics['mse'] = mean_squared_error(y_true, y_pred)
        metrics['rmse'] = np.sqrt(metrics['mse'])
        metrics['mae'] = mean_absolute_error(y_true, y_pred)
        metrics['r2'] = r2_score(y_true, y_pred)
        
        # Mean Absolute Percentage Error
        metrics['mape'] = np.mean(np.abs((y_true - y_pred) / y_true)) * 100
        
        return metrics
    
    def plot_confusion_matrix(self, y_true, y_pred, classes=None):
        """Plot confusion matrix"""
        cm = confusion_matrix(y_true, y_pred)
        
        plt.figure(figsize=(8, 6))
        plt.imshow(cm, interpolation='nearest', cmap=plt.cm.Blues)
        plt.title('Confusion Matrix')
        plt.colorbar()
        
        if classes is not None:
            tick_marks = np.arange(len(classes))
            plt.xticks(tick_marks, classes, rotation=45)
            plt.yticks(tick_marks, classes)
        
        # Add text annotations
        thresh = cm.max() / 2.
        for i, j in np.ndindex(cm.shape):
            plt.text(j, i, format(cm[i, j], 'd'),
                    horizontalalignment="center",
                    color="white" if cm[i, j] > thresh else "black")
        
        plt.ylabel('True label')
        plt.xlabel('Predicted label')
        plt.tight_layout()
        return plt.gcf()
    
    def plot_roc_curve(self, y_true, y_pred_proba):
        """Plot ROC curve"""
        fpr, tpr, _ = roc_curve(y_true, y_pred_proba)
        auc = roc_auc_score(y_true, y_pred_proba)
        
        plt.figure(figsize=(8, 6))
        plt.plot(fpr, tpr, color='darkorange', lw=2, 
                label=f'ROC curve (AUC = {auc:.2f})')
        plt.plot([0, 1], [0, 1], color='navy', lw=2, linestyle='--')
        plt.xlim([0.0, 1.0])
        plt.ylim([0.0, 1.05])
        plt.xlabel('False Positive Rate')
        plt.ylabel('True Positive Rate')
        plt.title('Receiver Operating Characteristic (ROC) Curve')
        plt.legend(loc="lower right")
        plt.grid(True)
        return plt.gcf()

# Example usage
def model_evaluation_example():
    # Generate sample classification data
    from sklearn.datasets import make_classification
    from sklearn.ensemble import RandomForestClassifier
    
    X, y = make_classification(n_samples=1000, n_features=20, n_informative=10,
                             n_redundant=10, n_clusters_per_class=1, random_state=42)
    
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Train model
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)
    
    # Predictions
    y_pred = model.predict(X_test)
    y_pred_proba = model.predict_proba(X_test)[:, 1]
    
    # Evaluate
    evaluator = ModelEvaluator()
    metrics = evaluator.evaluate_classification(y_test, y_pred, y_pred_proba)
    
    print("Classification Metrics:")
    print(f"Accuracy: {metrics['accuracy']:.3f}")
    print(f"Precision: {metrics['precision']:.3f}")
    print(f"Recall: {metrics['recall']:.3f}")
    print(f"F1-Score: {metrics['f1']:.3f}")
    print(f"ROC AUC: {metrics['roc_auc']:.3f}")
    
    print("\nConfusion Matrix:")
    print(metrics['confusion_matrix'])
    
    # Detailed classification report
    print("\nDetailed Classification Report:")
    print(classification_report(y_test, y_pred))

model_evaluation_example()
```

This completes Part 1 of the AI/ML Advanced Concepts Guide, covering machine learning fundamentals, supervised learning algorithms, unsupervised learning techniques, comprehensive data preprocessing, and model evaluation methods with practical implementations.