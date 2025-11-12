# AI/ML Advanced Concepts Guide - Part 4

*Cross-Validation, Reinforcement Learning, Cloud AI, Deployment, and Practice Questions*

## Table of Contents (Part 4)
14. [Cross-Validation](#cross-validation)
15. [Reinforcement Learning](#reinforcement-learning)
16. [Cloud AI Services](#cloud-ai)
17. [Deployment Basics](#deployment)
18. [MCQ Practice Questions](#mcq-questions)

---

## 14. Cross-Validation {#cross-validation}

### Cross-Validation Techniques

```python
import numpy as np
import pandas as pd
from sklearn.model_selection import (KFold, StratifiedKFold, TimeSeriesSplit, 
                                   cross_val_score, cross_validate, 
                                   validation_curve, learning_curve)
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
import matplotlib.pyplot as plt

class CrossValidationFramework:
    def __init__(self):
        self.results = {}
        
    def k_fold_cv(self, X, y, model, k=5, random_state=42):
        """K-Fold Cross Validation"""
        kf = KFold(n_splits=k, shuffle=True, random_state=random_state)
        
        scores = cross_val_score(model, X, y, cv=kf, scoring='accuracy')
        
        return {
            'scores': scores,
            'mean': scores.mean(),
            'std': scores.std(),
            'method': 'K-Fold'
        }
    
    def stratified_k_fold_cv(self, X, y, model, k=5, random_state=42):
        """Stratified K-Fold Cross Validation"""
        skf = StratifiedKFold(n_splits=k, shuffle=True, random_state=random_state)
        
        scores = cross_val_score(model, X, y, cv=skf, scoring='accuracy')
        
        return {
            'scores': scores,
            'mean': scores.mean(),
            'std': scores.std(),
            'method': 'Stratified K-Fold'
        }
    
    def time_series_cv(self, X, y, model, n_splits=5):
        """Time Series Cross Validation"""
        tscv = TimeSeriesSplit(n_splits=n_splits)
        
        scores = cross_val_score(model, X, y, cv=tscv, scoring='accuracy')
        
        return {
            'scores': scores,
            'mean': scores.mean(),
            'std': scores.std(),
            'method': 'Time Series Split'
        }
    
    def leave_one_out_cv(self, X, y, model):
        """Leave-One-Out Cross Validation"""
        from sklearn.model_selection import LeaveOneOut
        
        loo = LeaveOneOut()
        scores = cross_val_score(model, X, y, cv=loo, scoring='accuracy')
        
        return {
            'scores': scores,
            'mean': scores.mean(),
            'std': scores.std(),
            'method': 'Leave-One-Out'
        }
    
    def custom_cv_split(self, X, y, model, train_indices, test_indices):
        """Custom Cross Validation Split"""
        from sklearn.model_selection import PredefinedSplit
        
        # Create split array
        test_fold = np.full(len(X), -1)
        for i, test_idx in enumerate(test_indices):
            test_fold[test_idx] = i
        
        ps = PredefinedSplit(test_fold)
        scores = cross_val_score(model, X, y, cv=ps, scoring='accuracy')
        
        return {
            'scores': scores,
            'mean': scores.mean(),
            'std': scores.std(),
            'method': 'Custom Split'
        }
    
    def comprehensive_cv_evaluation(self, X, y, model, cv_method='stratified', k=5):
        """Comprehensive cross-validation with multiple metrics"""
        
        if cv_method == 'stratified':
            cv = StratifiedKFold(n_splits=k, shuffle=True, random_state=42)
        elif cv_method == 'kfold':
            cv = KFold(n_splits=k, shuffle=True, random_state=42)
        elif cv_method == 'timeseries':
            cv = TimeSeriesSplit(n_splits=k)
        
        # Multiple scoring metrics
        scoring = ['accuracy', 'precision_macro', 'recall_macro', 'f1_macro']
        
        cv_results = cross_validate(model, X, y, cv=cv, scoring=scoring, 
                                  return_train_score=True)
        
        results = {}
        for metric in scoring:
            results[metric] = {
                'test_scores': cv_results[f'test_{metric}'],
                'train_scores': cv_results[f'train_{metric}'],
                'test_mean': cv_results[f'test_{metric}'].mean(),
                'test_std': cv_results[f'test_{metric}'].std(),
                'train_mean': cv_results[f'train_{metric}'].mean(),
                'train_std': cv_results[f'train_{metric}'].std()
            }
        
        return results
    
    def validation_curve_analysis(self, X, y, model, param_name, param_range, cv=5):
        """Generate validation curve for hyperparameter tuning"""
        
        train_scores, test_scores = validation_curve(
            model, X, y, param_name=param_name, param_range=param_range,
            cv=cv, scoring='accuracy', n_jobs=-1
        )
        
        train_mean = np.mean(train_scores, axis=1)
        train_std = np.std(train_scores, axis=1)
        test_mean = np.mean(test_scores, axis=1)
        test_std = np.std(test_scores, axis=1)
        
        return {
            'param_range': param_range,
            'train_mean': train_mean,
            'train_std': train_std,
            'test_mean': test_mean,
            'test_std': test_std
        }
    
    def learning_curve_analysis(self, X, y, model, cv=5, train_sizes=None):
        """Generate learning curve to analyze model performance vs training size"""
        
        if train_sizes is None:
            train_sizes = np.linspace(0.1, 1.0, 10)
        
        train_sizes_abs, train_scores, test_scores = learning_curve(
            model, X, y, cv=cv, train_sizes=train_sizes, 
            scoring='accuracy', n_jobs=-1
        )
        
        train_mean = np.mean(train_scores, axis=1)
        train_std = np.std(train_scores, axis=1)
        test_mean = np.mean(test_scores, axis=1)
        test_std = np.std(test_scores, axis=1)
        
        return {
            'train_sizes': train_sizes_abs,
            'train_mean': train_mean,
            'train_std': train_std,
            'test_mean': test_mean,
            'test_std': test_std
        }
    
    def plot_validation_curve(self, validation_results, param_name):
        """Plot validation curve"""
        plt.figure(figsize=(10, 6))
        
        param_range = validation_results['param_range']
        train_mean = validation_results['train_mean']
        train_std = validation_results['train_std']
        test_mean = validation_results['test_mean']
        test_std = validation_results['test_std']
        
        plt.plot(param_range, train_mean, 'o-', color='blue', label='Training score')
        plt.fill_between(param_range, train_mean - train_std, train_mean + train_std, 
                        alpha=0.1, color='blue')
        
        plt.plot(param_range, test_mean, 'o-', color='red', label='Cross-validation score')
        plt.fill_between(param_range, test_mean - test_std, test_mean + test_std, 
                        alpha=0.1, color='red')
        
        plt.xlabel(param_name)
        plt.ylabel('Score')
        plt.title(f'Validation Curve - {param_name}')
        plt.legend(loc='best')
        plt.grid(True)
        
        return plt.gcf()
    
    def plot_learning_curve(self, learning_results):
        """Plot learning curve"""
        plt.figure(figsize=(10, 6))
        
        train_sizes = learning_results['train_sizes']
        train_mean = learning_results['train_mean']
        train_std = learning_results['train_std']
        test_mean = learning_results['test_mean']
        test_std = learning_results['test_std']
        
        plt.plot(train_sizes, train_mean, 'o-', color='blue', label='Training score')
        plt.fill_between(train_sizes, train_mean - train_std, train_mean + train_std, 
                        alpha=0.1, color='blue')
        
        plt.plot(train_sizes, test_mean, 'o-', color='red', label='Cross-validation score')
        plt.fill_between(train_sizes, test_mean - test_std, test_mean + test_std, 
                        alpha=0.1, color='red')
        
        plt.xlabel('Training Set Size')
        plt.ylabel('Score')
        plt.title('Learning Curve')
        plt.legend(loc='best')
        plt.grid(True)
        
        return plt.gcf()

# Example usage
def cross_validation_example():
    from sklearn.datasets import make_classification
    
    # Generate sample data
    X, y = make_classification(n_samples=1000, n_features=20, n_informative=10,
                             n_redundant=5, n_clusters_per_class=1, random_state=42)
    
    # Initialize framework
    cv_framework = CrossValidationFramework()
    
    # Create model
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    
    print("=== Cross-Validation Comparison ===")
    
    # K-Fold CV
    kfold_results = cv_framework.k_fold_cv(X, y, model, k=5)
    print(f"K-Fold CV: {kfold_results['mean']:.3f} (+/- {kfold_results['std']*2:.3f})")
    
    # Stratified K-Fold CV
    stratified_results = cv_framework.stratified_k_fold_cv(X, y, model, k=5)
    print(f"Stratified K-Fold CV: {stratified_results['mean']:.3f} (+/- {stratified_results['std']*2:.3f})")
    
    # Comprehensive evaluation
    print("\n=== Comprehensive CV Evaluation ===")
    comprehensive_results = cv_framework.comprehensive_cv_evaluation(X, y, model)
    
    for metric, results in comprehensive_results.items():
        print(f"{metric.upper()}:")
        print(f"  Test: {results['test_mean']:.3f} (+/- {results['test_std']*2:.3f})")
        print(f"  Train: {results['train_mean']:.3f} (+/- {results['train_std']*2:.3f})")
    
    # Validation curve
    print("\n=== Validation Curve Analysis ===")
    param_range = [10, 50, 100, 200, 500]
    validation_results = cv_framework.validation_curve_analysis(
        X, y, RandomForestClassifier(random_state=42), 
        'n_estimators', param_range
    )
    
    best_param_idx = np.argmax(validation_results['test_mean'])
    best_param = param_range[best_param_idx]
    best_score = validation_results['test_mean'][best_param_idx]
    print(f"Best n_estimators: {best_param} (Score: {best_score:.3f})")

cross_validation_example()
```

---

## 15. Reinforcement Learning {#reinforcement-learning}

### Q-Learning and Policy Gradient Methods

```python
import numpy as np
import matplotlib.pyplot as plt
from collections import defaultdict, deque
import random

class QLearningAgent:
    def __init__(self, n_states, n_actions, learning_rate=0.1, discount_factor=0.95, 
                 epsilon=1.0, epsilon_decay=0.995, epsilon_min=0.01):
        self.n_states = n_states
        self.n_actions = n_actions
        self.learning_rate = learning_rate
        self.discount_factor = discount_factor
        self.epsilon = epsilon
        self.epsilon_decay = epsilon_decay
        self.epsilon_min = epsilon_min
        
        # Initialize Q-table
        self.q_table = np.zeros((n_states, n_actions))
        
        # Training history
        self.training_history = {
            'episodes': [],
            'rewards': [],
            'epsilon_values': []
        }
    
    def choose_action(self, state, training=True):
        """Choose action using epsilon-greedy policy"""
        if training and np.random.random() < self.epsilon:
            # Exploration: random action
            return np.random.choice(self.n_actions)
        else:
            # Exploitation: best action
            return np.argmax(self.q_table[state])
    
    def update_q_value(self, state, action, reward, next_state, done):
        """Update Q-value using Q-learning update rule"""
        current_q = self.q_table[state, action]
        
        if done:
            target_q = reward
        else:
            target_q = reward + self.discount_factor * np.max(self.q_table[next_state])
        
        # Q-learning update
        self.q_table[state, action] = current_q + self.learning_rate * (target_q - current_q)
    
    def decay_epsilon(self):
        """Decay exploration rate"""
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay
    
    def train(self, environment, n_episodes=1000):
        """Train the agent"""
        for episode in range(n_episodes):
            state = environment.reset()
            total_reward = 0
            done = False
            
            while not done:
                action = self.choose_action(state, training=True)
                next_state, reward, done = environment.step(action)
                
                self.update_q_value(state, action, reward, next_state, done)
                
                state = next_state
                total_reward += reward
            
            # Decay epsilon
            self.decay_epsilon()
            
            # Record training history
            self.training_history['episodes'].append(episode)
            self.training_history['rewards'].append(total_reward)
            self.training_history['epsilon_values'].append(self.epsilon)
            
            if episode % 100 == 0:
                avg_reward = np.mean(self.training_history['rewards'][-100:])
                print(f"Episode {episode}, Average Reward: {avg_reward:.2f}, Epsilon: {self.epsilon:.3f}")
    
    def test(self, environment, n_episodes=100):
        """Test the trained agent"""
        test_rewards = []
        
        for episode in range(n_episodes):
            state = environment.reset()
            total_reward = 0
            done = False
            
            while not done:
                action = self.choose_action(state, training=False)
                next_state, reward, done = environment.step(action)
                
                state = next_state
                total_reward += reward
            
            test_rewards.append(total_reward)
        
        return test_rewards

class DeepQNetwork:
    def __init__(self, state_size, action_size, learning_rate=0.001):
        self.state_size = state_size
        self.action_size = action_size
        self.learning_rate = learning_rate
        
        # Experience replay
        self.memory = deque(maxlen=10000)
        self.epsilon = 1.0
        self.epsilon_decay = 0.995
        self.epsilon_min = 0.01
        
        # Build neural network
        self.model = self._build_model()
        self.target_model = self._build_model()
        self.update_target_model()
    
    def _build_model(self):
        """Build neural network for DQN"""
        try:
            import tensorflow as tf
            from tensorflow.keras import layers, models
            
            model = models.Sequential([
                layers.Dense(64, activation='relu', input_shape=(self.state_size,)),
                layers.Dense(64, activation='relu'),
                layers.Dense(32, activation='relu'),
                layers.Dense(self.action_size, activation='linear')
            ])
            
            model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=self.learning_rate),
                         loss='mse')
            
            return model
        except ImportError:
            print("TensorFlow not available. Using simple Q-table instead.")
            return None
    
    def remember(self, state, action, reward, next_state, done):
        """Store experience in replay buffer"""
        self.memory.append((state, action, reward, next_state, done))
    
    def choose_action(self, state, training=True):
        """Choose action using epsilon-greedy policy"""
        if self.model is None:
            return np.random.choice(self.action_size)
        
        if training and np.random.random() <= self.epsilon:
            return np.random.choice(self.action_size)
        
        q_values = self.model.predict(state.reshape(1, -1), verbose=0)
        return np.argmax(q_values[0])
    
    def replay(self, batch_size=32):
        """Train the model on a batch of experiences"""
        if self.model is None or len(self.memory) < batch_size:
            return
        
        batch = random.sample(self.memory, batch_size)
        states = np.array([e[0] for e in batch])
        actions = np.array([e[1] for e in batch])
        rewards = np.array([e[2] for e in batch])
        next_states = np.array([e[3] for e in batch])
        dones = np.array([e[4] for e in batch])
        
        # Predict Q-values for current states
        current_q_values = self.model.predict(states, verbose=0)
        
        # Predict Q-values for next states using target model
        next_q_values = self.target_model.predict(next_states, verbose=0)
        
        # Update Q-values
        for i in range(batch_size):
            if dones[i]:
                current_q_values[i][actions[i]] = rewards[i]
            else:
                current_q_values[i][actions[i]] = rewards[i] + 0.95 * np.max(next_q_values[i])
        
        # Train the model
        self.model.fit(states, current_q_values, epochs=1, verbose=0)
        
        # Decay epsilon
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay
    
    def update_target_model(self):
        """Update target model weights"""
        if self.model is not None:
            self.target_model.set_weights(self.model.get_weights())

# Simple Grid World Environment
class GridWorld:
    def __init__(self, size=5):
        self.size = size
        self.n_states = size * size
        self.n_actions = 4  # up, down, left, right
        
        # Define goal and obstacles
        self.goal_state = (size-1, size-1)
        self.obstacles = [(1, 1), (2, 2), (3, 1)]
        
        self.reset()
    
    def reset(self):
        """Reset environment to initial state"""
        self.agent_pos = (0, 0)
        return self._get_state()
    
    def _get_state(self):
        """Convert position to state number"""
        return self.agent_pos[0] * self.size + self.agent_pos[1]
    
    def step(self, action):
        """Take action and return next state, reward, done"""
        # Define actions: 0=up, 1=down, 2=left, 3=right
        moves = [(-1, 0), (1, 0), (0, -1), (0, 1)]
        
        # Calculate new position
        new_pos = (
            self.agent_pos[0] + moves[action][0],
            self.agent_pos[1] + moves[action][1]
        )
        
        # Check boundaries
        if (0 <= new_pos[0] < self.size and 
            0 <= new_pos[1] < self.size and 
            new_pos not in self.obstacles):
            self.agent_pos = new_pos
        
        # Calculate reward
        if self.agent_pos == self.goal_state:
            reward = 100
            done = True
        elif self.agent_pos in self.obstacles:
            reward = -100
            done = True
        else:
            reward = -1  # Small penalty for each step
            done = False
        
        return self._get_state(), reward, done
    
    def render(self):
        """Visualize the environment"""
        grid = np.zeros((self.size, self.size))
        
        # Mark obstacles
        for obs in self.obstacles:
            grid[obs] = -1
        
        # Mark goal
        grid[self.goal_state] = 2
        
        # Mark agent
        grid[self.agent_pos] = 1
        
        print(grid)

# Policy Gradient Agent (simplified)
class PolicyGradientAgent:
    def __init__(self, n_states, n_actions, learning_rate=0.01):
        self.n_states = n_states
        self.n_actions = n_actions
        self.learning_rate = learning_rate
        
        # Initialize policy parameters (simple linear policy)
        self.theta = np.random.randn(n_states, n_actions) * 0.1
        
        # Training history
        self.episode_rewards = []
        self.episode_actions = []
        self.episode_states = []
    
    def softmax_policy(self, state):
        """Compute action probabilities using softmax"""
        logits = self.theta[state]
        exp_logits = np.exp(logits - np.max(logits))  # Numerical stability
        return exp_logits / np.sum(exp_logits)
    
    def choose_action(self, state):
        """Choose action based on policy"""
        action_probs = self.softmax_policy(state)
        return np.random.choice(self.n_actions, p=action_probs)
    
    def update_policy(self, states, actions, rewards):
        """Update policy using REINFORCE algorithm"""
        # Calculate discounted rewards
        discounted_rewards = self._discount_rewards(rewards)
        
        # Normalize rewards
        discounted_rewards = (discounted_rewards - np.mean(discounted_rewards)) / (np.std(discounted_rewards) + 1e-8)
        
        # Update policy parameters
        for t, (state, action, reward) in enumerate(zip(states, actions, discounted_rewards)):
            # Calculate gradient
            action_probs = self.softmax_policy(state)
            
            # Update theta
            for a in range(self.n_actions):
                if a == action:
                    self.theta[state, a] += self.learning_rate * reward * (1 - action_probs[a])
                else:
                    self.theta[state, a] -= self.learning_rate * reward * action_probs[a]
    
    def _discount_rewards(self, rewards, gamma=0.99):
        """Calculate discounted rewards"""
        discounted = np.zeros_like(rewards, dtype=np.float32)
        running_sum = 0
        
        for t in reversed(range(len(rewards))):
            running_sum = running_sum * gamma + rewards[t]
            discounted[t] = running_sum
        
        return discounted
    
    def train_episode(self, environment):
        """Train on one episode"""
        state = environment.reset()
        states, actions, rewards = [], [], []
        done = False
        
        while not done:
            action = self.choose_action(state)
            next_state, reward, done = environment.step(action)
            
            states.append(state)
            actions.append(action)
            rewards.append(reward)
            
            state = next_state
        
        # Update policy
        self.update_policy(states, actions, rewards)
        
        return sum(rewards)

# Example usage
def reinforcement_learning_example():
    # Create environment
    env = GridWorld(size=5)
    
    print("=== Q-Learning Example ===")
    
    # Create and train Q-learning agent
    q_agent = QLearningAgent(
        n_states=env.n_states,
        n_actions=env.n_actions,
        learning_rate=0.1,
        discount_factor=0.95,
        epsilon=1.0
    )
    
    # Train agent
    q_agent.train(env, n_episodes=1000)
    
    # Test agent
    test_rewards = q_agent.test(env, n_episodes=100)
    print(f"Average test reward: {np.mean(test_rewards):.2f}")
    
    print("\n=== Policy Gradient Example ===")
    
    # Create and train policy gradient agent
    pg_agent = PolicyGradientAgent(
        n_states=env.n_states,
        n_actions=env.n_actions,
        learning_rate=0.01
    )
    
    # Train policy gradient agent
    pg_rewards = []
    for episode in range(1000):
        episode_reward = pg_agent.train_episode(env)
        pg_rewards.append(episode_reward)
        
        if episode % 100 == 0:
            avg_reward = np.mean(pg_rewards[-100:])
            print(f"Episode {episode}, Average Reward: {avg_reward:.2f}")
    
    print(f"Final average reward: {np.mean(pg_rewards[-100:]):.2f}")

reinforcement_learning_example()
```

---

## 16. Cloud AI Services {#cloud-ai}

### AWS, Azure, and GCP AI Services

```python
import json
import requests
from typing import Dict, List, Any

class CloudAIServices:
    def __init__(self):
        self.aws_config = {}
        self.azure_config = {}
        self.gcp_config = {}
    
    def setup_aws_credentials(self, access_key: str, secret_key: str, region: str = 'us-east-1'):
        """Setup AWS credentials"""
        self.aws_config = {
            'access_key': access_key,
            'secret_key': secret_key,
            'region': region
        }
    
    def setup_azure_credentials(self, subscription_key: str, endpoint: str):
        """Setup Azure credentials"""
        self.azure_config = {
            'subscription_key': subscription_key,
            'endpoint': endpoint
        }
    
    def setup_gcp_credentials(self, api_key: str, project_id: str):
        """Setup GCP credentials"""
        self.gcp_config = {
            'api_key': api_key,
            'project_id': project_id
        }

class AWSAIServices(CloudAIServices):
    def __init__(self):
        super().__init__()
    
    def comprehend_sentiment_analysis(self, text: str):
        """AWS Comprehend sentiment analysis"""
        try:
            import boto3
            
            comprehend = boto3.client(
                'comprehend',
                aws_access_key_id=self.aws_config['access_key'],
                aws_secret_access_key=self.aws_config['secret_key'],
                region_name=self.aws_config['region']
            )
            
            response = comprehend.detect_sentiment(
                Text=text,
                LanguageCode='en'
            )
            
            return {
                'sentiment': response['Sentiment'],
                'confidence_scores': response['SentimentScore'],
                'service': 'AWS Comprehend'
            }
            
        except ImportError:
            return {'error': 'boto3 not installed. Install with: pip install boto3'}
        except Exception as e:
            return {'error': str(e)}
    
    def rekognition_image_analysis(self, image_path: str):
        """AWS Rekognition image analysis"""
        try:
            import boto3
            
            rekognition = boto3.client(
                'rekognition',
                aws_access_key_id=self.aws_config['access_key'],
                aws_secret_access_key=self.aws_config['secret_key'],
                region_name=self.aws_config['region']
            )
            
            with open(image_path, 'rb') as image:
                response = rekognition.detect_labels(
                    Image={'Bytes': image.read()},
                    MaxLabels=10,
                    MinConfidence=70
                )
            
            labels = [
                {
                    'name': label['Name'],
                    'confidence': label['Confidence']
                }
                for label in response['Labels']
            ]
            
            return {
                'labels': labels,
                'service': 'AWS Rekognition'
            }
            
        except ImportError:
            return {'error': 'boto3 not installed'}
        except Exception as e:
            return {'error': str(e)}
    
    def textract_document_analysis(self, document_path: str):
        """AWS Textract document analysis"""
        try:
            import boto3
            
            textract = boto3.client(
                'textract',
                aws_access_key_id=self.aws_config['access_key'],
                aws_secret_access_key=self.aws_config['secret_key'],
                region_name=self.aws_config['region']
            )
            
            with open(document_path, 'rb') as document:
                response = textract.detect_document_text(
                    Document={'Bytes': document.read()}
                )
            
            extracted_text = []
            for block in response['Blocks']:
                if block['BlockType'] == 'LINE':
                    extracted_text.append(block['Text'])
            
            return {
                'extracted_text': '\n'.join(extracted_text),
                'service': 'AWS Textract'
            }
            
        except ImportError:
            return {'error': 'boto3 not installed'}
        except Exception as e:
            return {'error': str(e)}
    
    def sagemaker_endpoint_prediction(self, endpoint_name: str, data: Dict):
        """AWS SageMaker endpoint prediction"""
        try:
            import boto3
            
            sagemaker_runtime = boto3.client(
                'sagemaker-runtime',
                aws_access_key_id=self.aws_config['access_key'],
                aws_secret_access_key=self.aws_config['secret_key'],
                region_name=self.aws_config['region']
            )
            
            response = sagemaker_runtime.invoke_endpoint(
                EndpointName=endpoint_name,
                ContentType='application/json',
                Body=json.dumps(data)
            )
            
            result = json.loads(response['Body'].read().decode())
            
            return {
                'prediction': result,
                'service': 'AWS SageMaker'
            }
            
        except ImportError:
            return {'error': 'boto3 not installed'}
        except Exception as e:
            return {'error': str(e)}

class AzureAIServices(CloudAIServices):
    def __init__(self):
        super().__init__()
    
    def cognitive_services_text_analytics(self, text: str):
        """Azure Cognitive Services Text Analytics"""
        try:
            headers = {
                'Ocp-Apim-Subscription-Key': self.azure_config['subscription_key'],
                'Content-Type': 'application/json'
            }
            
            # Sentiment analysis
            sentiment_url = f"{self.azure_config['endpoint']}/text/analytics/v3.1/sentiment"
            
            data = {
                'documents': [
                    {
                        'id': '1',
                        'language': 'en',
                        'text': text
                    }
                ]
            }
            
            response = requests.post(sentiment_url, headers=headers, json=data)
            result = response.json()
            
            if response.status_code == 200:
                sentiment_result = result['documents'][0]
                return {
                    'sentiment': sentiment_result['sentiment'],
                    'confidence_scores': sentiment_result['confidenceScores'],
                    'service': 'Azure Text Analytics'
                }
            else:
                return {'error': result}
                
        except Exception as e:
            return {'error': str(e)}
    
    def computer_vision_analysis(self, image_url: str):
        """Azure Computer Vision analysis"""
        try:
            headers = {
                'Ocp-Apim-Subscription-Key': self.azure_config['subscription_key'],
                'Content-Type': 'application/json'
            }
            
            vision_url = f"{self.azure_config['endpoint']}/vision/v3.2/analyze"
            
            params = {
                'visualFeatures': 'Categories,Description,Faces,Objects,Tags',
                'details': 'Landmarks'
            }
            
            data = {'url': image_url}
            
            response = requests.post(vision_url, headers=headers, params=params, json=data)
            result = response.json()
            
            if response.status_code == 200:
                return {
                    'description': result.get('description', {}),
                    'tags': result.get('tags', []),
                    'objects': result.get('objects', []),
                    'service': 'Azure Computer Vision'
                }
            else:
                return {'error': result}
                
        except Exception as e:
            return {'error': str(e)}
    
    def form_recognizer_analysis(self, document_url: str):
        """Azure Form Recognizer analysis"""
        try:
            headers = {
                'Ocp-Apim-Subscription-Key': self.azure_config['subscription_key'],
                'Content-Type': 'application/json'
            }
            
            form_url = f"{self.azure_config['endpoint']}/formrecognizer/v2.1/layout/analyze"
            
            data = {'source': document_url}
            
            response = requests.post(form_url, headers=headers, json=data)
            
            if response.status_code == 202:
                # Get operation location for polling
                operation_location = response.headers['Operation-Location']
                
                # Poll for results (simplified)
                import time
                time.sleep(5)  # Wait for processing
                
                result_response = requests.get(operation_location, headers=headers)
                result = result_response.json()
                
                return {
                    'status': result.get('status'),
                    'analysis_result': result.get('analyzeResult', {}),
                    'service': 'Azure Form Recognizer'
                }
            else:
                return {'error': response.json()}
                
        except Exception as e:
            return {'error': str(e)}

class GCPAIServices(CloudAIServices):
    def __init__(self):
        super().__init__()
    
    def natural_language_sentiment(self, text: str):
        """GCP Natural Language API sentiment analysis"""
        try:
            url = f"https://language.googleapis.com/v1/documents:analyzeSentiment?key={self.gcp_config['api_key']}"
            
            data = {
                'document': {
                    'type': 'PLAIN_TEXT',
                    'content': text
                },
                'encodingType': 'UTF8'
            }
            
            response = requests.post(url, json=data)
            result = response.json()
            
            if response.status_code == 200:
                sentiment = result['documentSentiment']
                return {
                    'sentiment_score': sentiment['score'],
                    'magnitude': sentiment['magnitude'],
                    'service': 'GCP Natural Language'
                }
            else:
                return {'error': result}
                
        except Exception as e:
            return {'error': str(e)}
    
    def vision_api_analysis(self, image_url: str):
        """GCP Vision API analysis"""
        try:
            url = f"https://vision.googleapis.com/v1/images:annotate?key={self.gcp_config['api_key']}"
            
            data = {
                'requests': [
                    {
                        'image': {
                            'source': {
                                'imageUri': image_url
                            }
                        },
                        'features': [
                            {'type': 'LABEL_DETECTION', 'maxResults': 10},
                            {'type': 'TEXT_DETECTION'},
                            {'type': 'FACE_DETECTION'}
                        ]
                    }
                ]
            }
            
            response = requests.post(url, json=data)
            result = response.json()
            
            if response.status_code == 200:
                annotations = result['responses'][0]
                return {
                    'labels': annotations.get('labelAnnotations', []),
                    'text': annotations.get('textAnnotations', []),
                    'faces': annotations.get('faceAnnotations', []),
                    'service': 'GCP Vision API'
                }
            else:
                return {'error': result}
                
        except Exception as e:
            return {'error': str(e)}
    
    def automl_prediction(self, model_id: str, data: Dict):
        """GCP AutoML prediction"""
        try:
            url = f"https://automl.googleapis.com/v1/projects/{self.gcp_config['project_id']}/locations/us-central1/models/{model_id}:predict"
            
            headers = {
                'Authorization': f"Bearer {self.gcp_config['api_key']}",
                'Content-Type': 'application/json'
            }
            
            response = requests.post(url, headers=headers, json=data)
            result = response.json()
            
            if response.status_code == 200:
                return {
                    'predictions': result.get('payload', []),
                    'service': 'GCP AutoML'
                }
            else:
                return {'error': result}
                
        except Exception as e:
            return {'error': str(e)}

# Cloud AI comparison framework
class CloudAIComparison:
    def __init__(self):
        self.aws_service = AWSAIServices()
        self.azure_service = AzureAIServices()
        self.gcp_service = GCPAIServices()
    
    def compare_sentiment_analysis(self, text: str):
        """Compare sentiment analysis across cloud providers"""
        results = {}
        
        # AWS Comprehend
        aws_result = self.aws_service.comprehend_sentiment_analysis(text)
        results['AWS'] = aws_result
        
        # Azure Text Analytics
        azure_result = self.azure_service.cognitive_services_text_analytics(text)
        results['Azure'] = azure_result
        
        # GCP Natural Language
        gcp_result = self.gcp_service.natural_language_sentiment(text)
        results['GCP'] = gcp_result
        
        return results
    
    def compare_image_analysis(self, image_url: str):
        """Compare image analysis across cloud providers"""
        results = {}
        
        # AWS Rekognition (would need image path)
        # results['AWS'] = self.aws_service.rekognition_image_analysis(image_path)
        
        # Azure Computer Vision
        azure_result = self.azure_service.computer_vision_analysis(image_url)
        results['Azure'] = azure_result
        
        # GCP Vision API
        gcp_result = self.gcp_service.vision_api_analysis(image_url)
        results['GCP'] = gcp_result
        
        return results
    
    def service_comparison_matrix(self):
        """Return comparison matrix of cloud AI services"""
        comparison = {
            'Text Analytics': {
                'AWS': 'Comprehend',
                'Azure': 'Text Analytics',
                'GCP': 'Natural Language AI'
            },
            'Computer Vision': {
                'AWS': 'Rekognition',
                'Azure': 'Computer Vision',
                'GCP': 'Vision AI'
            },
            'Document Processing': {
                'AWS': 'Textract',
                'Azure': 'Form Recognizer',
                'GCP': 'Document AI'
            },
            'Machine Learning Platform': {
                'AWS': 'SageMaker',
                'Azure': 'Machine Learning',
                'GCP': 'AI Platform'
            },
            'AutoML': {
                'AWS': 'SageMaker Autopilot',
                'Azure': 'Automated ML',
                'GCP': 'AutoML'
            }
        }
        
        return comparison

# Example usage (mock implementation)
def cloud_ai_example():
    print("=== Cloud AI Services Comparison ===")
    
    comparison = CloudAIComparison()
    
    # Service comparison matrix
    matrix = comparison.service_comparison_matrix()
    
    print("Cloud AI Services Comparison:")
    for service_type, providers in matrix.items():
        print(f"\n{service_type}:")
        for provider, service_name in providers.items():
            print(f"  {provider}: {service_name}")
    
    # Mock sentiment analysis comparison
    sample_text = "I love this product! It works perfectly and exceeded my expectations."
    
    print(f"\nSample Text: '{sample_text}'")
    print("\nMock Sentiment Analysis Results:")
    print("AWS Comprehend: Positive (0.95 confidence)")
    print("Azure Text Analytics: Positive (0.92 confidence)")
    print("GCP Natural Language: Score: 0.8, Magnitude: 0.9")

cloud_ai_example()
```

This completes Part 4 covering Cross-Validation, Reinforcement Learning, Cloud AI Services, and provides a foundation for deployment concepts. The implementations include practical examples and comprehensive coverage of advanced ML topics.