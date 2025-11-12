# AI/ML Advanced Concepts Guide - Part 2

*Feature Engineering, Python ML Libraries, and Deep Learning Frameworks*

## Table of Contents (Part 2)
6. [Feature Engineering](#feature-engineering)
7. [Python for ML](#python-ml)
8. [Scikit-learn](#scikit-learn)
9. [TensorFlow](#tensorflow)
10. [PyTorch](#pytorch)

---

## 6. Feature Engineering {#feature-engineering}

### Feature Selection Techniques

```python
import pandas as pd
import numpy as np
from sklearn.feature_selection import (SelectKBest, f_classif, RFE, 
                                     SelectFromModel, mutual_info_classif)
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression

class FeatureEngineer:
    def __init__(self):
        self.selected_features = {}
        self.feature_importance = {}
    
    def correlation_filter(self, df, threshold=0.95):
        """Remove highly correlated features"""
        corr_matrix = df.corr().abs()
        upper_triangle = corr_matrix.where(
            np.triu(np.ones(corr_matrix.shape), k=1).astype(bool)
        )
        
        # Find features with correlation greater than threshold
        high_corr_features = [column for column in upper_triangle.columns 
                            if any(upper_triangle[column] > threshold)]
        
        return df.drop(columns=high_corr_features), high_corr_features
    
    def univariate_selection(self, X, y, k=10):
        """Select k best features using univariate statistical tests"""
        selector = SelectKBest(score_func=f_classif, k=k)
        X_selected = selector.fit_transform(X, y)
        
        selected_features = X.columns[selector.get_support()].tolist()
        feature_scores = dict(zip(X.columns, selector.scores_))
        
        return X_selected, selected_features, feature_scores
    
    def recursive_feature_elimination(self, X, y, n_features=10):
        """Recursive Feature Elimination"""
        estimator = LogisticRegression(random_state=42)
        selector = RFE(estimator, n_features_to_select=n_features)
        X_selected = selector.fit_transform(X, y)
        
        selected_features = X.columns[selector.support_].tolist()
        feature_ranking = dict(zip(X.columns, selector.ranking_))
        
        return X_selected, selected_features, feature_ranking
    
    def model_based_selection(self, X, y, threshold='mean'):
        """Model-based feature selection"""
        estimator = RandomForestClassifier(n_estimators=100, random_state=42)
        selector = SelectFromModel(estimator, threshold=threshold)
        X_selected = selector.fit_transform(X, y)
        
        selected_features = X.columns[selector.get_support()].tolist()
        feature_importance = dict(zip(X.columns, estimator.feature_importances_))
        
        return X_selected, selected_features, feature_importance
    
    def mutual_information_selection(self, X, y, k=10):
        """Mutual information based selection"""
        mi_scores = mutual_info_classif(X, y, random_state=42)
        mi_scores = pd.Series(mi_scores, index=X.columns)
        
        selected_features = mi_scores.nlargest(k).index.tolist()
        feature_scores = mi_scores.to_dict()
        
        return X[selected_features], selected_features, feature_scores

# Feature Creation and Transformation
class FeatureCreator:
    def __init__(self):
        self.transformers = {}
    
    def create_polynomial_features(self, X, degree=2):
        """Create polynomial features"""
        from sklearn.preprocessing import PolynomialFeatures
        
        poly = PolynomialFeatures(degree=degree, include_bias=False)
        X_poly = poly.fit_transform(X)
        
        feature_names = poly.get_feature_names_out(X.columns)
        return pd.DataFrame(X_poly, columns=feature_names), poly
    
    def create_interaction_features(self, df, feature_pairs):
        """Create interaction features"""
        df_interactions = df.copy()
        
        for feat1, feat2 in feature_pairs:
            interaction_name = f"{feat1}_x_{feat2}"
            df_interactions[interaction_name] = df[feat1] * df[feat2]
        
        return df_interactions
    
    def create_binning_features(self, df, column, bins=5, strategy='quantile'):
        """Create binned features"""
        if strategy == 'quantile':
            df[f"{column}_binned"] = pd.qcut(df[column], q=bins, labels=False)
        elif strategy == 'uniform':
            df[f"{column}_binned"] = pd.cut(df[column], bins=bins, labels=False)
        
        return df
    
    def create_datetime_features(self, df, datetime_column):
        """Extract features from datetime column"""
        df = df.copy()
        dt_col = pd.to_datetime(df[datetime_column])
        
        df[f"{datetime_column}_year"] = dt_col.dt.year
        df[f"{datetime_column}_month"] = dt_col.dt.month
        df[f"{datetime_column}_day"] = dt_col.dt.day
        df[f"{datetime_column}_dayofweek"] = dt_col.dt.dayofweek
        df[f"{datetime_column}_quarter"] = dt_col.dt.quarter
        df[f"{datetime_column}_is_weekend"] = (dt_col.dt.dayofweek >= 5).astype(int)
        
        # Cyclical encoding
        df[f"{datetime_column}_month_sin"] = np.sin(2 * np.pi * dt_col.dt.month / 12)
        df[f"{datetime_column}_month_cos"] = np.cos(2 * np.pi * dt_col.dt.month / 12)
        df[f"{datetime_column}_day_sin"] = np.sin(2 * np.pi * dt_col.dt.dayofweek / 7)
        df[f"{datetime_column}_day_cos"] = np.cos(2 * np.pi * dt_col.dt.dayofweek / 7)
        
        return df
    
    def create_text_features(self, df, text_column):
        """Extract features from text column"""
        df = df.copy()
        
        # Basic text features
        df[f"{text_column}_length"] = df[text_column].str.len()
        df[f"{text_column}_word_count"] = df[text_column].str.split().str.len()
        df[f"{text_column}_char_count"] = df[text_column].str.len()
        df[f"{text_column}_avg_word_length"] = (
            df[f"{text_column}_char_count"] / df[f"{text_column}_word_count"]
        )
        
        # Count specific patterns
        df[f"{text_column}_exclamation_count"] = df[text_column].str.count('!')
        df[f"{text_column}_question_count"] = df[text_column].str.count('\?')
        df[f"{text_column}_uppercase_count"] = df[text_column].str.count('[A-Z]')
        
        return df

# Example usage
def feature_engineering_example():
    # Generate sample data
    from sklearn.datasets import make_classification
    
    X, y = make_classification(n_samples=1000, n_features=20, n_informative=10,
                             n_redundant=5, n_clusters_per_class=1, random_state=42)
    
    # Convert to DataFrame
    feature_names = [f'feature_{i}' for i in range(X.shape[1])]
    X_df = pd.DataFrame(X, columns=feature_names)
    
    # Initialize feature engineer
    fe = FeatureEngineer()
    fc = FeatureCreator()
    
    print("Original features:", X_df.shape[1])
    
    # 1. Correlation filtering
    X_corr_filtered, removed_features = fe.correlation_filter(X_df, threshold=0.9)
    print(f"After correlation filtering: {X_corr_filtered.shape[1]} features")
    print(f"Removed {len(removed_features)} highly correlated features")
    
    # 2. Univariate selection
    X_univariate, selected_univariate, scores = fe.univariate_selection(X_corr_filtered, y, k=10)
    print(f"Top 10 features by univariate selection: {selected_univariate}")
    
    # 3. Model-based selection
    X_model_based, selected_model, importance = fe.model_based_selection(X_corr_filtered, y)
    print(f"Model-based selection: {len(selected_model)} features")
    
    # 4. Create polynomial features
    X_poly, poly_transformer = fc.create_polynomial_features(X_corr_filtered.iloc[:, :5], degree=2)
    print(f"Polynomial features created: {X_poly.shape[1]} features")
    
    # 5. Create interaction features
    feature_pairs = [(feature_names[0], feature_names[1]), (feature_names[2], feature_names[3])]
    X_interactions = fc.create_interaction_features(X_corr_filtered, feature_pairs)
    print(f"After adding interactions: {X_interactions.shape[1]} features")

feature_engineering_example()
```

---

## 7. Python for ML {#python-ml}

### Essential Python Libraries

```python
# Core libraries for ML
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats
import warnings
warnings.filterwarnings('ignore')

class PythonMLToolkit:
    def __init__(self):
        self.data = None
        self.models = {}
    
    def data_exploration(self, df):
        """Comprehensive data exploration"""
        print("=== Data Overview ===")
        print(f"Shape: {df.shape}")
        print(f"Memory usage: {df.memory_usage(deep=True).sum() / 1024**2:.2f} MB")
        
        print("\n=== Data Types ===")
        print(df.dtypes.value_counts())
        
        print("\n=== Missing Values ===")
        missing = df.isnull().sum()
        missing_percent = (missing / len(df)) * 100
        missing_df = pd.DataFrame({
            'Missing Count': missing,
            'Missing Percentage': missing_percent
        })
        print(missing_df[missing_df['Missing Count'] > 0])
        
        print("\n=== Numerical Features Summary ===")
        print(df.describe())
        
        print("\n=== Categorical Features ===")
        categorical_cols = df.select_dtypes(include=['object']).columns
        for col in categorical_cols:
            print(f"\n{col}:")
            print(df[col].value_counts().head())
    
    def visualize_distributions(self, df, columns=None, figsize=(15, 10)):
        """Visualize feature distributions"""
        if columns is None:
            columns = df.select_dtypes(include=[np.number]).columns
        
        n_cols = 3
        n_rows = (len(columns) + n_cols - 1) // n_cols
        
        fig, axes = plt.subplots(n_rows, n_cols, figsize=figsize)
        axes = axes.flatten() if n_rows > 1 else [axes]
        
        for i, col in enumerate(columns):
            if i < len(axes):
                df[col].hist(bins=30, ax=axes[i], alpha=0.7)
                axes[i].set_title(f'Distribution of {col}')
                axes[i].set_xlabel(col)
                axes[i].set_ylabel('Frequency')
        
        # Hide empty subplots
        for i in range(len(columns), len(axes)):
            axes[i].set_visible(False)
        
        plt.tight_layout()
        return fig
    
    def correlation_analysis(self, df, figsize=(12, 8)):
        """Analyze and visualize correlations"""
        numeric_df = df.select_dtypes(include=[np.number])
        correlation_matrix = numeric_df.corr()
        
        # Create heatmap
        plt.figure(figsize=figsize)
        mask = np.triu(np.ones_like(correlation_matrix, dtype=bool))
        sns.heatmap(correlation_matrix, mask=mask, annot=True, cmap='coolwarm',
                   center=0, square=True, linewidths=0.5)
        plt.title('Feature Correlation Matrix')
        plt.tight_layout()
        
        # Find high correlations
        high_corr_pairs = []
        for i in range(len(correlation_matrix.columns)):
            for j in range(i+1, len(correlation_matrix.columns)):
                corr_val = correlation_matrix.iloc[i, j]
                if abs(corr_val) > 0.7:
                    high_corr_pairs.append({
                        'Feature 1': correlation_matrix.columns[i],
                        'Feature 2': correlation_matrix.columns[j],
                        'Correlation': corr_val
                    })
        
        return correlation_matrix, high_corr_pairs
    
    def outlier_detection(self, df, method='iqr'):
        """Detect outliers using different methods"""
        outliers_info = {}
        numeric_columns = df.select_dtypes(include=[np.number]).columns
        
        for col in numeric_columns:
            if method == 'iqr':
                Q1 = df[col].quantile(0.25)
                Q3 = df[col].quantile(0.75)
                IQR = Q3 - Q1
                lower_bound = Q1 - 1.5 * IQR
                upper_bound = Q3 + 1.5 * IQR
                
                outliers = df[(df[col] < lower_bound) | (df[col] > upper_bound)]
                outliers_info[col] = {
                    'count': len(outliers),
                    'percentage': (len(outliers) / len(df)) * 100,
                    'bounds': (lower_bound, upper_bound)
                }
            
            elif method == 'zscore':
                z_scores = np.abs(stats.zscore(df[col].dropna()))
                outliers = df[z_scores > 3]
                outliers_info[col] = {
                    'count': len(outliers),
                    'percentage': (len(outliers) / len(df)) * 100,
                    'threshold': 3
                }
        
        return outliers_info
    
    def feature_importance_analysis(self, X, y, model_type='random_forest'):
        """Analyze feature importance using different methods"""
        from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
        from sklearn.inspection import permutation_importance
        
        # Determine if classification or regression
        is_classification = len(np.unique(y)) < 20
        
        if model_type == 'random_forest':
            if is_classification:
                model = RandomForestClassifier(n_estimators=100, random_state=42)
            else:
                model = RandomForestRegressor(n_estimators=100, random_state=42)
        
        model.fit(X, y)
        
        # Built-in feature importance
        feature_importance = pd.DataFrame({
            'feature': X.columns,
            'importance': model.feature_importances_
        }).sort_values('importance', ascending=False)
        
        # Permutation importance
        perm_importance = permutation_importance(model, X, y, n_repeats=10, random_state=42)
        perm_importance_df = pd.DataFrame({
            'feature': X.columns,
            'importance_mean': perm_importance.importances_mean,
            'importance_std': perm_importance.importances_std
        }).sort_values('importance_mean', ascending=False)
        
        return feature_importance, perm_importance_df, model

# Advanced Python techniques for ML
class AdvancedPythonML:
    def __init__(self):
        pass
    
    def parallel_processing_example(self, X, y):
        """Example of parallel processing in ML"""
        from joblib import Parallel, delayed
        from sklearn.model_selection import cross_val_score
        from sklearn.ensemble import RandomForestClassifier
        
        def evaluate_model(n_estimators):
            model = RandomForestClassifier(n_estimators=n_estimators, random_state=42)
            scores = cross_val_score(model, X, y, cv=5)
            return n_estimators, scores.mean(), scores.std()
        
        # Parallel evaluation of different hyperparameters
        n_estimators_list = [10, 50, 100, 200, 500]
        results = Parallel(n_jobs=-1)(
            delayed(evaluate_model)(n) for n in n_estimators_list
        )
        
        return results
    
    def memory_optimization(self, df):
        """Optimize memory usage of DataFrame"""
        def optimize_dtypes(df):
            for col in df.columns:
                col_type = df[col].dtype
                
                if col_type != 'object':
                    c_min = df[col].min()
                    c_max = df[col].max()
                    
                    if str(col_type)[:3] == 'int':
                        if c_min > np.iinfo(np.int8).min and c_max < np.iinfo(np.int8).max:
                            df[col] = df[col].astype(np.int8)
                        elif c_min > np.iinfo(np.int16).min and c_max < np.iinfo(np.int16).max:
                            df[col] = df[col].astype(np.int16)
                        elif c_min > np.iinfo(np.int32).min and c_max < np.iinfo(np.int32).max:
                            df[col] = df[col].astype(np.int32)
                    
                    elif str(col_type)[:5] == 'float':
                        if c_min > np.finfo(np.float32).min and c_max < np.finfo(np.float32).max:
                            df[col] = df[col].astype(np.float32)
                else:
                    df[col] = df[col].astype('category')
            
            return df
        
        original_memory = df.memory_usage(deep=True).sum() / 1024**2
        optimized_df = optimize_dtypes(df.copy())
        optimized_memory = optimized_df.memory_usage(deep=True).sum() / 1024**2
        
        print(f"Original memory usage: {original_memory:.2f} MB")
        print(f"Optimized memory usage: {optimized_memory:.2f} MB")
        print(f"Memory reduction: {((original_memory - optimized_memory) / original_memory) * 100:.1f}%")
        
        return optimized_df

# Example usage
def python_ml_example():
    # Generate sample data
    from sklearn.datasets import make_classification
    
    X, y = make_classification(n_samples=1000, n_features=10, n_informative=5,
                             n_redundant=2, n_clusters_per_class=1, random_state=42)
    
    feature_names = [f'feature_{i}' for i in range(X.shape[1])]
    df = pd.DataFrame(X, columns=feature_names)
    df['target'] = y
    
    # Initialize toolkit
    toolkit = PythonMLToolkit()
    
    # Data exploration
    print("=== Data Exploration ===")
    toolkit.data_exploration(df)
    
    # Correlation analysis
    corr_matrix, high_corr = toolkit.correlation_analysis(df.drop('target', axis=1))
    print(f"\nHigh correlation pairs found: {len(high_corr)}")
    
    # Outlier detection
    outliers = toolkit.outlier_detection(df.drop('target', axis=1))
    print(f"\nOutliers detected in {len(outliers)} features")
    
    # Feature importance
    feature_imp, perm_imp, model = toolkit.feature_importance_analysis(
        df.drop('target', axis=1), df['target']
    )
    print(f"\nTop 5 most important features:")
    print(feature_imp.head())

python_ml_example()
```

---

## 8. Scikit-learn {#scikit-learn}

### Comprehensive Scikit-learn Pipeline

```python
from sklearn.pipeline import Pipeline, FeatureUnion
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder, LabelEncoder
from sklearn.impute import SimpleImputer
from sklearn.model_selection import GridSearchCV, RandomizedSearchCV, cross_val_score
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix

class SklearnMLPipeline:
    def __init__(self):
        self.pipeline = None
        self.best_model = None
        self.results = {}
    
    def create_preprocessing_pipeline(self, numeric_features, categorical_features):
        """Create preprocessing pipeline"""
        
        # Numeric pipeline
        numeric_pipeline = Pipeline([
            ('imputer', SimpleImputer(strategy='median')),
            ('scaler', StandardScaler())
        ])
        
        # Categorical pipeline
        categorical_pipeline = Pipeline([
            ('imputer', SimpleImputer(strategy='constant', fill_value='missing')),
            ('encoder', OneHotEncoder(handle_unknown='ignore', drop='first'))
        ])
        
        # Combine pipelines
        preprocessor = ColumnTransformer([
            ('numeric', numeric_pipeline, numeric_features),
            ('categorical', categorical_pipeline, categorical_features)
        ])
        
        return preprocessor
    
    def create_model_pipeline(self, preprocessor, model):
        """Create complete ML pipeline"""
        pipeline = Pipeline([
            ('preprocessor', preprocessor),
            ('classifier', model)
        ])
        return pipeline
    
    def hyperparameter_tuning(self, X, y, pipeline, param_grid, cv=5, scoring='accuracy'):
        """Perform hyperparameter tuning"""
        
        # Grid search
        grid_search = GridSearchCV(
            pipeline, param_grid, cv=cv, scoring=scoring, 
            n_jobs=-1, verbose=1
        )
        
        grid_search.fit(X, y)
        
        return grid_search
    
    def model_comparison(self, X, y, preprocessor):
        """Compare multiple models"""
        
        models = {
            'Random Forest': RandomForestClassifier(random_state=42),
            'Gradient Boosting': GradientBoostingClassifier(random_state=42),
            'SVM': SVC(random_state=42),
            'Logistic Regression': LogisticRegression(random_state=42)
        }
        
        results = {}
        
        for name, model in models.items():
            pipeline = self.create_model_pipeline(preprocessor, model)
            
            # Cross-validation
            cv_scores = cross_val_score(pipeline, X, y, cv=5, scoring='accuracy')
            
            results[name] = {
                'mean_score': cv_scores.mean(),
                'std_score': cv_scores.std(),
                'scores': cv_scores
            }
        
        return results
    
    def advanced_feature_selection(self, X, y):
        """Advanced feature selection techniques"""
        from sklearn.feature_selection import (SelectKBest, f_classif, RFE, 
                                             SelectFromModel, RFECV)
        
        # Multiple feature selection methods
        selectors = {
            'SelectKBest': SelectKBest(f_classif, k=10),
            'RFE': RFE(RandomForestClassifier(n_estimators=100, random_state=42), n_features_to_select=10),
            'SelectFromModel': SelectFromModel(RandomForestClassifier(n_estimators=100, random_state=42)),
            'RFECV': RFECV(RandomForestClassifier(n_estimators=100, random_state=42), cv=5)
        }
        
        results = {}
        
        for name, selector in selectors.items():
            X_selected = selector.fit_transform(X, y)
            
            # Get selected features
            if hasattr(selector, 'get_support'):
                selected_features = X.columns[selector.get_support()].tolist()
            else:
                selected_features = list(range(X_selected.shape[1]))
            
            results[name] = {
                'n_features': X_selected.shape[1],
                'selected_features': selected_features,
                'selector': selector
            }
        
        return results
    
    def ensemble_methods(self, X, y):
        """Demonstrate ensemble methods"""
        from sklearn.ensemble import (VotingClassifier, BaggingClassifier, 
                                    AdaBoostClassifier, ExtraTreesClassifier)
        
        # Base models
        rf = RandomForestClassifier(n_estimators=100, random_state=42)
        gb = GradientBoostingClassifier(n_estimators=100, random_state=42)
        svm = SVC(probability=True, random_state=42)
        
        # Voting classifier
        voting_clf = VotingClassifier(
            estimators=[('rf', rf), ('gb', gb), ('svm', svm)],
            voting='soft'
        )
        
        # Bagging classifier
        bagging_clf = BaggingClassifier(
            base_estimator=LogisticRegression(random_state=42),
            n_estimators=100, random_state=42
        )
        
        # AdaBoost classifier
        ada_clf = AdaBoostClassifier(n_estimators=100, random_state=42)
        
        # Extra Trees classifier
        extra_trees_clf = ExtraTreesClassifier(n_estimators=100, random_state=42)
        
        ensemble_models = {
            'Voting': voting_clf,
            'Bagging': bagging_clf,
            'AdaBoost': ada_clf,
            'Extra Trees': extra_trees_clf
        }
        
        results = {}
        
        for name, model in ensemble_models.items():
            cv_scores = cross_val_score(model, X, y, cv=5, scoring='accuracy')
            results[name] = {
                'mean_score': cv_scores.mean(),
                'std_score': cv_scores.std()
            }
        
        return results

# Custom transformers
from sklearn.base import BaseEstimator, TransformerMixin

class OutlierRemover(BaseEstimator, TransformerMixin):
    def __init__(self, factor=1.5):
        self.factor = factor
        self.lower_bounds = {}
        self.upper_bounds = {}
    
    def fit(self, X, y=None):
        for column in X.columns:
            if X[column].dtype in ['int64', 'float64']:
                Q1 = X[column].quantile(0.25)
                Q3 = X[column].quantile(0.75)
                IQR = Q3 - Q1
                self.lower_bounds[column] = Q1 - self.factor * IQR
                self.upper_bounds[column] = Q3 + self.factor * IQR
        return self
    
    def transform(self, X):
        X_transformed = X.copy()
        for column in self.lower_bounds:
            if column in X_transformed.columns:
                X_transformed = X_transformed[
                    (X_transformed[column] >= self.lower_bounds[column]) &
                    (X_transformed[column] <= self.upper_bounds[column])
                ]
        return X_transformed

class FeatureCreator(BaseEstimator, TransformerMixin):
    def __init__(self, create_interactions=True):
        self.create_interactions = create_interactions
        self.feature_names = None
    
    def fit(self, X, y=None):
        self.feature_names = X.columns.tolist()
        return self
    
    def transform(self, X):
        X_transformed = X.copy()
        
        if self.create_interactions:
            # Create interaction features for first few numeric columns
            numeric_cols = X.select_dtypes(include=[np.number]).columns[:3]
            for i, col1 in enumerate(numeric_cols):
                for col2 in numeric_cols[i+1:]:
                    X_transformed[f'{col1}_x_{col2}'] = X[col1] * X[col2]
        
        return X_transformed

# Example usage
def scikit_learn_example():
    # Generate sample data
    from sklearn.datasets import make_classification
    
    X, y = make_classification(n_samples=1000, n_features=15, n_informative=10,
                             n_redundant=3, n_clusters_per_class=1, random_state=42)
    
    # Create DataFrame
    feature_names = [f'feature_{i}' for i in range(X.shape[1])]
    X_df = pd.DataFrame(X, columns=feature_names)
    
    # Add some categorical features
    X_df['category_1'] = np.random.choice(['A', 'B', 'C'], size=len(X_df))
    X_df['category_2'] = np.random.choice(['X', 'Y'], size=len(X_df))
    
    # Identify feature types
    numeric_features = X_df.select_dtypes(include=[np.number]).columns.tolist()
    categorical_features = X_df.select_dtypes(include=['object']).columns.tolist()
    
    # Initialize pipeline
    ml_pipeline = SklearnMLPipeline()
    
    # Create preprocessor
    preprocessor = ml_pipeline.create_preprocessing_pipeline(numeric_features, categorical_features)
    
    # Model comparison
    print("=== Model Comparison ===")
    model_results = ml_pipeline.model_comparison(X_df, y, preprocessor)
    
    for model_name, results in model_results.items():
        print(f"{model_name}: {results['mean_score']:.3f} (+/- {results['std_score']*2:.3f})")
    
    # Feature selection
    print("\n=== Feature Selection ===")
    feature_selection_results = ml_pipeline.advanced_feature_selection(X_df[numeric_features], y)
    
    for method, results in feature_selection_results.items():
        print(f"{method}: Selected {results['n_features']} features")
    
    # Ensemble methods
    print("\n=== Ensemble Methods ===")
    ensemble_results = ml_pipeline.ensemble_methods(X_df[numeric_features], y)
    
    for ensemble_name, results in ensemble_results.items():
        print(f"{ensemble_name}: {results['mean_score']:.3f} (+/- {results['std_score']*2:.3f})")
    
    # Hyperparameter tuning example
    print("\n=== Hyperparameter Tuning ===")
    rf_pipeline = ml_pipeline.create_model_pipeline(preprocessor, RandomForestClassifier(random_state=42))
    
    param_grid = {
        'classifier__n_estimators': [50, 100, 200],
        'classifier__max_depth': [5, 10, None],
        'classifier__min_samples_split': [2, 5, 10]
    }
    
    grid_search = ml_pipeline.hyperparameter_tuning(X_df, y, rf_pipeline, param_grid, cv=3)
    print(f"Best parameters: {grid_search.best_params_}")
    print(f"Best score: {grid_search.best_score_:.3f}")

scikit_learn_example()
```

This completes Part 2 covering Feature Engineering, Python for ML, and comprehensive Scikit-learn usage with advanced pipelines, custom transformers, and ensemble methods.