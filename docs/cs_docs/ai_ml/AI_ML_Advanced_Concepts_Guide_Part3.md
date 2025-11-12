# AI/ML Advanced Concepts Guide - Part 3

*TensorFlow, PyTorch, NLP, Neural Networks, and Advanced Topics*

## Table of Contents (Part 3)
9. [TensorFlow](#tensorflow)
10. [PyTorch](#pytorch)
11. [NLP and Sentiment Analysis](#nlp)
12. [Neural Networks](#neural-networks)
13. [Overfitting and Underfitting](#overfitting-underfitting)

---

## 9. TensorFlow {#tensorflow}

### TensorFlow Basics and Neural Networks

```python
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models, optimizers, callbacks
import numpy as np
import matplotlib.pyplot as plt

class TensorFlowMLPipeline:
    def __init__(self):
        self.model = None
        self.history = None
        
    def create_dense_model(self, input_shape, num_classes, hidden_layers=[128, 64]):
        """Create a dense neural network"""
        model = models.Sequential()
        
        # Input layer
        model.add(layers.Dense(hidden_layers[0], activation='relu', input_shape=input_shape))
        model.add(layers.Dropout(0.3))
        
        # Hidden layers
        for units in hidden_layers[1:]:
            model.add(layers.Dense(units, activation='relu'))
            model.add(layers.Dropout(0.3))
        
        # Output layer
        if num_classes == 2:
            model.add(layers.Dense(1, activation='sigmoid'))
            loss = 'binary_crossentropy'
            metrics = ['accuracy']
        else:
            model.add(layers.Dense(num_classes, activation='softmax'))
            loss = 'sparse_categorical_crossentropy'
            metrics = ['accuracy']
        
        model.compile(
            optimizer='adam',
            loss=loss,
            metrics=metrics
        )
        
        return model
    
    def create_cnn_model(self, input_shape, num_classes):
        """Create a CNN model for image classification"""
        model = models.Sequential([
            layers.Conv2D(32, (3, 3), activation='relu', input_shape=input_shape),
            layers.MaxPooling2D((2, 2)),
            layers.Conv2D(64, (3, 3), activation='relu'),
            layers.MaxPooling2D((2, 2)),
            layers.Conv2D(64, (3, 3), activation='relu'),
            
            layers.Flatten(),
            layers.Dense(64, activation='relu'),
            layers.Dropout(0.5),
            layers.Dense(num_classes, activation='softmax')
        ])
        
        model.compile(
            optimizer='adam',
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        
        return model
    
    def create_rnn_model(self, vocab_size, embedding_dim, max_length, num_classes):
        """Create an RNN model for text classification"""
        model = models.Sequential([
            layers.Embedding(vocab_size, embedding_dim, input_length=max_length),
            layers.LSTM(64, dropout=0.5, recurrent_dropout=0.5),
            layers.Dense(32, activation='relu'),
            layers.Dropout(0.5),
            layers.Dense(num_classes, activation='softmax')
        ])
        
        model.compile(
            optimizer='adam',
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        
        return model
    
    def train_with_callbacks(self, model, X_train, y_train, X_val, y_val, epochs=100):
        """Train model with callbacks"""
        
        # Define callbacks
        early_stopping = callbacks.EarlyStopping(
            monitor='val_loss',
            patience=10,
            restore_best_weights=True
        )
        
        reduce_lr = callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.2,
            patience=5,
            min_lr=0.001
        )
        
        model_checkpoint = callbacks.ModelCheckpoint(
            'best_model.h5',
            monitor='val_accuracy',
            save_best_only=True,
            mode='max'
        )
        
        # Train model
        history = model.fit(
            X_train, y_train,
            batch_size=32,
            epochs=epochs,
            validation_data=(X_val, y_val),
            callbacks=[early_stopping, reduce_lr, model_checkpoint],
            verbose=1
        )
        
        return history
    
    def plot_training_history(self, history):
        """Plot training history"""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4))
        
        # Plot accuracy
        ax1.plot(history.history['accuracy'], label='Training Accuracy')
        ax1.plot(history.history['val_accuracy'], label='Validation Accuracy')
        ax1.set_title('Model Accuracy')
        ax1.set_xlabel('Epoch')
        ax1.set_ylabel('Accuracy')
        ax1.legend()
        
        # Plot loss
        ax2.plot(history.history['loss'], label='Training Loss')
        ax2.plot(history.history['val_loss'], label='Validation Loss')
        ax2.set_title('Model Loss')
        ax2.set_xlabel('Epoch')
        ax2.set_ylabel('Loss')
        ax2.legend()
        
        plt.tight_layout()
        return fig

# Custom layers and models
class CustomDenseLayer(layers.Layer):
    def __init__(self, units, activation=None):
        super(CustomDenseLayer, self).__init__()
        self.units = units
        self.activation = keras.activations.get(activation)
    
    def build(self, input_shape):
        self.w = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer='random_normal',
            trainable=True
        )
        self.b = self.add_weight(
            shape=(self.units,),
            initializer='zeros',
            trainable=True
        )
    
    def call(self, inputs):
        output = tf.matmul(inputs, self.w) + self.b
        if self.activation is not None:
            output = self.activation(output)
        return output

class AttentionLayer(layers.Layer):
    def __init__(self, units):
        super(AttentionLayer, self).__init__()
        self.units = units
        
    def build(self, input_shape):
        self.W = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer='random_normal',
            trainable=True
        )
        self.b = self.add_weight(
            shape=(self.units,),
            initializer='zeros',
            trainable=True
        )
        self.u = self.add_weight(
            shape=(self.units,),
            initializer='random_normal',
            trainable=True
        )
    
    def call(self, inputs):
        # inputs shape: (batch_size, seq_len, features)
        uit = tf.tanh(tf.tensordot(inputs, self.W, axes=1) + self.b)
        ait = tf.tensordot(uit, self.u, axes=1)
        ait = tf.nn.softmax(ait, axis=1)
        ait = tf.expand_dims(ait, -1)
        weighted_input = inputs * ait
        output = tf.reduce_sum(weighted_input, axis=1)
        return output

# Transfer Learning with TensorFlow
class TransferLearningPipeline:
    def __init__(self):
        self.base_model = None
        self.model = None
    
    def create_transfer_model(self, input_shape, num_classes, base_model_name='VGG16'):
        """Create transfer learning model"""
        
        # Load pre-trained model
        if base_model_name == 'VGG16':
            base_model = keras.applications.VGG16(
                weights='imagenet',
                include_top=False,
                input_shape=input_shape
            )
        elif base_model_name == 'ResNet50':
            base_model = keras.applications.ResNet50(
                weights='imagenet',
                include_top=False,
                input_shape=input_shape
            )
        
        # Freeze base model layers
        base_model.trainable = False
        
        # Add custom top layers
        model = models.Sequential([
            base_model,
            layers.GlobalAveragePooling2D(),
            layers.Dense(128, activation='relu'),
            layers.Dropout(0.5),
            layers.Dense(num_classes, activation='softmax')
        ])
        
        model.compile(
            optimizer=optimizers.Adam(learning_rate=0.0001),
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        
        self.base_model = base_model
        self.model = model
        
        return model
    
    def fine_tune_model(self, epochs=10):
        """Fine-tune the pre-trained model"""
        # Unfreeze the base model
        self.base_model.trainable = True
        
        # Use a lower learning rate for fine-tuning
        self.model.compile(
            optimizer=optimizers.Adam(learning_rate=0.0001/10),
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )
        
        return self.model

# Example usage
def tensorflow_example():
    # Generate sample data
    from sklearn.datasets import make_classification
    from sklearn.model_selection import train_test_split
    from sklearn.preprocessing import StandardScaler
    
    X, y = make_classification(n_samples=1000, n_features=20, n_classes=3, 
                             n_informative=15, random_state=42)
    
    # Split and scale data
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    X_train, X_val, y_train, y_val = train_test_split(X_train, y_train, test_size=0.2, random_state=42)
    
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_val = scaler.transform(X_val)
    X_test = scaler.transform(X_test)
    
    # Initialize pipeline
    tf_pipeline = TensorFlowMLPipeline()
    
    # Create and train model
    model = tf_pipeline.create_dense_model(
        input_shape=(X_train.shape[1],),
        num_classes=3,
        hidden_layers=[128, 64, 32]
    )
    
    print("Model Architecture:")
    model.summary()
    
    # Train model
    history = tf_pipeline.train_with_callbacks(
        model, X_train, y_train, X_val, y_val, epochs=50
    )
    
    # Evaluate model
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"\nTest Accuracy: {test_accuracy:.3f}")
    
    # Plot training history
    # tf_pipeline.plot_training_history(history)

tensorflow_example()
```

---

## 10. PyTorch {#pytorch}

### PyTorch Neural Networks and Training

```python
import torch
import torch.nn as nn
import torch.optim as optim
import torch.nn.functional as F
from torch.utils.data import DataLoader, TensorDataset, Dataset
import numpy as np

class PyTorchMLPipeline:
    def __init__(self, device=None):
        self.device = device or torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self.model = None
        self.optimizer = None
        self.criterion = None
        
    def create_dense_model(self, input_size, hidden_sizes, num_classes, dropout_rate=0.3):
        """Create a dense neural network"""
        
        class DenseNet(nn.Module):
            def __init__(self, input_size, hidden_sizes, num_classes, dropout_rate):
                super(DenseNet, self).__init__()
                
                layers = []
                prev_size = input_size
                
                for hidden_size in hidden_sizes:
                    layers.extend([
                        nn.Linear(prev_size, hidden_size),
                        nn.ReLU(),
                        nn.Dropout(dropout_rate)
                    ])
                    prev_size = hidden_size
                
                layers.append(nn.Linear(prev_size, num_classes))
                
                self.network = nn.Sequential(*layers)
            
            def forward(self, x):
                return self.network(x)
        
        return DenseNet(input_size, hidden_sizes, num_classes, dropout_rate)
    
    def create_cnn_model(self, input_channels, num_classes):
        """Create a CNN model"""
        
        class CNN(nn.Module):
            def __init__(self, input_channels, num_classes):
                super(CNN, self).__init__()
                
                self.conv_layers = nn.Sequential(
                    nn.Conv2d(input_channels, 32, kernel_size=3, padding=1),
                    nn.ReLU(),
                    nn.MaxPool2d(2),
                    
                    nn.Conv2d(32, 64, kernel_size=3, padding=1),
                    nn.ReLU(),
                    nn.MaxPool2d(2),
                    
                    nn.Conv2d(64, 128, kernel_size=3, padding=1),
                    nn.ReLU(),
                    nn.MaxPool2d(2),
                )
                
                # Calculate the size after conv layers
                self.fc_layers = nn.Sequential(
                    nn.AdaptiveAvgPool2d((1, 1)),
                    nn.Flatten(),
                    nn.Linear(128, 256),
                    nn.ReLU(),
                    nn.Dropout(0.5),
                    nn.Linear(256, num_classes)
                )
            
            def forward(self, x):
                x = self.conv_layers(x)
                x = self.fc_layers(x)
                return x
        
        return CNN(input_channels, num_classes)
    
    def create_rnn_model(self, vocab_size, embedding_dim, hidden_dim, num_classes, num_layers=2):
        """Create an RNN model for text classification"""
        
        class RNNClassifier(nn.Module):
            def __init__(self, vocab_size, embedding_dim, hidden_dim, num_classes, num_layers):
                super(RNNClassifier, self).__init__()
                
                self.embedding = nn.Embedding(vocab_size, embedding_dim)
                self.lstm = nn.LSTM(embedding_dim, hidden_dim, num_layers, 
                                  batch_first=True, dropout=0.3)
                self.fc = nn.Linear(hidden_dim, num_classes)
                self.dropout = nn.Dropout(0.5)
            
            def forward(self, x):
                embedded = self.embedding(x)
                lstm_out, (hidden, _) = self.lstm(embedded)
                
                # Use the last hidden state
                output = self.fc(self.dropout(hidden[-1]))
                return output
        
        return RNNClassifier(vocab_size, embedding_dim, hidden_dim, num_classes, num_layers)
    
    def train_model(self, model, train_loader, val_loader, epochs=100, learning_rate=0.001):
        """Train the model"""
        
        model = model.to(self.device)
        criterion = nn.CrossEntropyLoss()
        optimizer = optim.Adam(model.parameters(), lr=learning_rate)
        scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, patience=5, factor=0.5)
        
        train_losses = []
        val_losses = []
        train_accuracies = []
        val_accuracies = []
        
        best_val_accuracy = 0
        patience_counter = 0
        patience = 10
        
        for epoch in range(epochs):
            # Training phase
            model.train()
            train_loss = 0
            train_correct = 0
            train_total = 0
            
            for batch_idx, (data, target) in enumerate(train_loader):
                data, target = data.to(self.device), target.to(self.device)
                
                optimizer.zero_grad()
                output = model(data)
                loss = criterion(output, target)
                loss.backward()
                optimizer.step()
                
                train_loss += loss.item()
                _, predicted = torch.max(output.data, 1)
                train_total += target.size(0)
                train_correct += (predicted == target).sum().item()
            
            # Validation phase
            model.eval()
            val_loss = 0
            val_correct = 0
            val_total = 0
            
            with torch.no_grad():
                for data, target in val_loader:
                    data, target = data.to(self.device), target.to(self.device)
                    output = model(data)
                    loss = criterion(output, target)
                    
                    val_loss += loss.item()
                    _, predicted = torch.max(output.data, 1)
                    val_total += target.size(0)
                    val_correct += (predicted == target).sum().item()
            
            # Calculate metrics
            train_accuracy = 100 * train_correct / train_total
            val_accuracy = 100 * val_correct / val_total
            
            train_losses.append(train_loss / len(train_loader))
            val_losses.append(val_loss / len(val_loader))
            train_accuracies.append(train_accuracy)
            val_accuracies.append(val_accuracy)
            
            # Learning rate scheduling
            scheduler.step(val_loss / len(val_loader))
            
            # Early stopping
            if val_accuracy > best_val_accuracy:
                best_val_accuracy = val_accuracy
                patience_counter = 0
                torch.save(model.state_dict(), 'best_model.pth')
            else:
                patience_counter += 1
            
            if patience_counter >= patience:
                print(f'Early stopping at epoch {epoch+1}')
                break
            
            if epoch % 10 == 0:
                print(f'Epoch {epoch+1}/{epochs}:')
                print(f'Train Loss: {train_losses[-1]:.4f}, Train Acc: {train_accuracy:.2f}%')
                print(f'Val Loss: {val_losses[-1]:.4f}, Val Acc: {val_accuracy:.2f}%')
        
        # Load best model
        model.load_state_dict(torch.load('best_model.pth'))
        
        return {
            'train_losses': train_losses,
            'val_losses': val_losses,
            'train_accuracies': train_accuracies,
            'val_accuracies': val_accuracies
        }

# Custom Dataset class
class CustomDataset(Dataset):
    def __init__(self, X, y, transform=None):
        self.X = torch.FloatTensor(X)
        self.y = torch.LongTensor(y)
        self.transform = transform
    
    def __len__(self):
        return len(self.X)
    
    def __getitem__(self, idx):
        sample = self.X[idx]
        label = self.y[idx]
        
        if self.transform:
            sample = self.transform(sample)
        
        return sample, label

# Advanced PyTorch techniques
class AdvancedPyTorchTechniques:
    def __init__(self):
        pass
    
    def create_attention_model(self, input_size, hidden_size, num_classes):
        """Create model with attention mechanism"""
        
        class AttentionModel(nn.Module):
            def __init__(self, input_size, hidden_size, num_classes):
                super(AttentionModel, self).__init__()
                
                self.lstm = nn.LSTM(input_size, hidden_size, batch_first=True)
                self.attention = nn.Linear(hidden_size, 1)
                self.classifier = nn.Linear(hidden_size, num_classes)
                
            def forward(self, x):
                lstm_out, _ = self.lstm(x)
                
                # Attention mechanism
                attention_weights = torch.softmax(self.attention(lstm_out), dim=1)
                context_vector = torch.sum(attention_weights * lstm_out, dim=1)
                
                output = self.classifier(context_vector)
                return output
        
        return AttentionModel(input_size, hidden_size, num_classes)
    
    def create_residual_block(self, in_channels, out_channels):
        """Create a residual block"""
        
        class ResidualBlock(nn.Module):
            def __init__(self, in_channels, out_channels):
                super(ResidualBlock, self).__init__()
                
                self.conv1 = nn.Conv2d(in_channels, out_channels, 3, padding=1)
                self.bn1 = nn.BatchNorm2d(out_channels)
                self.conv2 = nn.Conv2d(out_channels, out_channels, 3, padding=1)
                self.bn2 = nn.BatchNorm2d(out_channels)
                
                self.shortcut = nn.Sequential()
                if in_channels != out_channels:
                    self.shortcut = nn.Sequential(
                        nn.Conv2d(in_channels, out_channels, 1),
                        nn.BatchNorm2d(out_channels)
                    )
            
            def forward(self, x):
                residual = x
                
                out = F.relu(self.bn1(self.conv1(x)))
                out = self.bn2(self.conv2(out))
                
                out += self.shortcut(residual)
                out = F.relu(out)
                
                return out
        
        return ResidualBlock(in_channels, out_channels)
    
    def gradient_clipping_example(self, model, optimizer, max_norm=1.0):
        """Example of gradient clipping"""
        
        # During training loop
        def training_step(data, target, criterion):
            optimizer.zero_grad()
            output = model(data)
            loss = criterion(output, target)
            loss.backward()
            
            # Gradient clipping
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm)
            
            optimizer.step()
            return loss.item()
        
        return training_step

# Example usage
def pytorch_example():
    # Generate sample data
    from sklearn.datasets import make_classification
    from sklearn.model_selection import train_test_split
    from sklearn.preprocessing import StandardScaler
    
    X, y = make_classification(n_samples=1000, n_features=20, n_classes=3, 
                             n_informative=15, random_state=42)
    
    # Split and scale data
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    X_train, X_val, y_train, y_val = train_test_split(X_train, y_train, test_size=0.2, random_state=42)
    
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_val = scaler.transform(X_val)
    X_test = scaler.transform(X_test)
    
    # Create datasets and data loaders
    train_dataset = CustomDataset(X_train, y_train)
    val_dataset = CustomDataset(X_val, y_val)
    test_dataset = CustomDataset(X_test, y_test)
    
    train_loader = DataLoader(train_dataset, batch_size=32, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=32, shuffle=False)
    test_loader = DataLoader(test_dataset, batch_size=32, shuffle=False)
    
    # Initialize pipeline
    pytorch_pipeline = PyTorchMLPipeline()
    
    # Create model
    model = pytorch_pipeline.create_dense_model(
        input_size=X_train.shape[1],
        hidden_sizes=[128, 64, 32],
        num_classes=3,
        dropout_rate=0.3
    )
    
    print("Model Architecture:")
    print(model)
    
    # Train model
    history = pytorch_pipeline.train_model(
        model, train_loader, val_loader, epochs=50, learning_rate=0.001
    )
    
    # Evaluate model
    model.eval()
    test_correct = 0
    test_total = 0
    
    with torch.no_grad():
        for data, target in test_loader:
            data, target = data.to(pytorch_pipeline.device), target.to(pytorch_pipeline.device)
            output = model(data)
            _, predicted = torch.max(output.data, 1)
            test_total += target.size(0)
            test_correct += (predicted == target).sum().item()
    
    test_accuracy = 100 * test_correct / test_total
    print(f"\nTest Accuracy: {test_accuracy:.2f}%")

pytorch_example()
```

---

## 11. NLP and Sentiment Analysis {#nlp}

### Text Processing and Sentiment Analysis

```python
import re
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize, sent_tokenize
from nltk.stem import PorterStemmer, WordNetLemmatizer
from sklearn.feature_extraction.text import TfidfVectorizer, CountVectorizer
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix
import pandas as pd
import numpy as np

# Download required NLTK data
try:
    nltk.data.find('tokenizers/punkt')
    nltk.data.find('corpora/stopwords')
    nltk.data.find('corpora/wordnet')
except LookupError:
    nltk.download('punkt')
    nltk.download('stopwords')
    nltk.download('wordnet')

class TextPreprocessor:
    def __init__(self):
        self.stemmer = PorterStemmer()
        self.lemmatizer = WordNetLemmatizer()
        self.stop_words = set(stopwords.words('english'))
    
    def clean_text(self, text):
        """Basic text cleaning"""
        # Convert to lowercase
        text = text.lower()
        
        # Remove URLs
        text = re.sub(r'http\S+|www\S+|https\S+', '', text, flags=re.MULTILINE)
        
        # Remove user mentions and hashtags
        text = re.sub(r'@\w+|#\w+', '', text)
        
        # Remove special characters and digits
        text = re.sub(r'[^a-zA-Z\s]', '', text)
        
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text).strip()
        
        return text
    
    def tokenize_text(self, text):
        """Tokenize text into words"""
        return word_tokenize(text)
    
    def remove_stopwords(self, tokens):
        """Remove stopwords from tokens"""
        return [token for token in tokens if token not in self.stop_words]
    
    def stem_tokens(self, tokens):
        """Apply stemming to tokens"""
        return [self.stemmer.stem(token) for token in tokens]
    
    def lemmatize_tokens(self, tokens):
        """Apply lemmatization to tokens"""
        return [self.lemmatizer.lemmatize(token) for token in tokens]
    
    def preprocess_text(self, text, use_stemming=False, use_lemmatization=True):
        """Complete text preprocessing pipeline"""
        # Clean text
        text = self.clean_text(text)
        
        # Tokenize
        tokens = self.tokenize_text(text)
        
        # Remove stopwords
        tokens = self.remove_stopwords(tokens)
        
        # Apply stemming or lemmatization
        if use_stemming:
            tokens = self.stem_tokens(tokens)
        elif use_lemmatization:
            tokens = self.lemmatize_tokens(tokens)
        
        return ' '.join(tokens)

class SentimentAnalyzer:
    def __init__(self):
        self.vectorizer = None
        self.model = None
        self.preprocessor = TextPreprocessor()
    
    def prepare_data(self, texts, labels, test_size=0.2):
        """Prepare data for training"""
        # Preprocess texts
        processed_texts = [self.preprocessor.preprocess_text(text) for text in texts]
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            processed_texts, labels, test_size=test_size, random_state=42
        )
        
        return X_train, X_test, y_train, y_test
    
    def create_features(self, X_train, X_test, method='tfidf', max_features=5000):
        """Create features using different vectorization methods"""
        
        if method == 'tfidf':
            self.vectorizer = TfidfVectorizer(
                max_features=max_features,
                ngram_range=(1, 2),
                min_df=2,
                max_df=0.95
            )
        elif method == 'count':
            self.vectorizer = CountVectorizer(
                max_features=max_features,
                ngram_range=(1, 2),
                min_df=2,
                max_df=0.95
            )
        
        X_train_vectorized = self.vectorizer.fit_transform(X_train)
        X_test_vectorized = self.vectorizer.transform(X_test)
        
        return X_train_vectorized, X_test_vectorized
    
    def train_model(self, X_train, y_train, model_type='logistic'):
        """Train sentiment analysis model"""
        
        if model_type == 'logistic':
            self.model = LogisticRegression(random_state=42, max_iter=1000)
        elif model_type == 'naive_bayes':
            self.model = MultinomialNB()
        
        self.model.fit(X_train, y_train)
        
        return self.model
    
    def predict_sentiment(self, texts):
        """Predict sentiment for new texts"""
        if self.model is None or self.vectorizer is None:
            raise ValueError("Model not trained yet!")
        
        # Preprocess texts
        processed_texts = [self.preprocessor.preprocess_text(text) for text in texts]
        
        # Vectorize
        X_vectorized = self.vectorizer.transform(processed_texts)
        
        # Predict
        predictions = self.model.predict(X_vectorized)
        probabilities = self.model.predict_proba(X_vectorized)
        
        return predictions, probabilities
    
    def get_feature_importance(self, top_n=20):
        """Get most important features for sentiment classification"""
        if self.model is None or self.vectorizer is None:
            raise ValueError("Model not trained yet!")
        
        feature_names = self.vectorizer.get_feature_names_out()
        
        if hasattr(self.model, 'coef_'):
            # For logistic regression
            coefficients = self.model.coef_[0]
            
            # Get top positive and negative features
            top_positive_indices = coefficients.argsort()[-top_n:][::-1]
            top_negative_indices = coefficients.argsort()[:top_n]
            
            top_positive_features = [(feature_names[i], coefficients[i]) 
                                   for i in top_positive_indices]
            top_negative_features = [(feature_names[i], coefficients[i]) 
                                   for i in top_negative_indices]
            
            return top_positive_features, top_negative_features
        
        return None, None

# Advanced NLP techniques
class AdvancedNLPTechniques:
    def __init__(self):
        pass
    
    def extract_named_entities(self, text):
        """Extract named entities from text"""
        try:
            import spacy
            nlp = spacy.load("en_core_web_sm")
            doc = nlp(text)
            
            entities = [(ent.text, ent.label_) for ent in doc.ents]
            return entities
        except ImportError:
            print("spaCy not installed. Install with: pip install spacy")
            return []
    
    def extract_keywords(self, texts, method='tfidf', top_k=10):
        """Extract keywords from a collection of texts"""
        
        if method == 'tfidf':
            vectorizer = TfidfVectorizer(
                max_features=1000,
                stop_words='english',
                ngram_range=(1, 2)
            )
            
            tfidf_matrix = vectorizer.fit_transform(texts)
            feature_names = vectorizer.get_feature_names_out()
            
            # Get average TF-IDF scores
            mean_scores = np.mean(tfidf_matrix.toarray(), axis=0)
            
            # Get top keywords
            top_indices = mean_scores.argsort()[-top_k:][::-1]
            keywords = [(feature_names[i], mean_scores[i]) for i in top_indices]
            
            return keywords
    
    def topic_modeling_lda(self, texts, n_topics=5):
        """Perform topic modeling using LDA"""
        try:
            from sklearn.decomposition import LatentDirichletAllocation
            
            # Vectorize texts
            vectorizer = CountVectorizer(
                max_features=1000,
                stop_words='english',
                min_df=2,
                max_df=0.95
            )
            
            doc_term_matrix = vectorizer.fit_transform(texts)
            
            # Fit LDA model
            lda = LatentDirichletAllocation(
                n_components=n_topics,
                random_state=42,
                max_iter=10
            )
            
            lda.fit(doc_term_matrix)
            
            # Get top words for each topic
            feature_names = vectorizer.get_feature_names_out()
            topics = []
            
            for topic_idx, topic in enumerate(lda.components_):
                top_words_idx = topic.argsort()[-10:][::-1]
                top_words = [feature_names[i] for i in top_words_idx]
                topics.append(top_words)
            
            return topics, lda, vectorizer
            
        except ImportError:
            print("scikit-learn LDA not available")
            return [], None, None
    
    def sentiment_analysis_vader(self, texts):
        """Sentiment analysis using VADER"""
        try:
            from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
            
            analyzer = SentimentIntensityAnalyzer()
            results = []
            
            for text in texts:
                scores = analyzer.polarity_scores(text)
                results.append(scores)
            
            return results
            
        except ImportError:
            print("VADER not installed. Install with: pip install vaderSentiment")
            return []

# Example usage
def nlp_sentiment_example():
    # Sample data (in practice, you'd load from a dataset)
    sample_texts = [
        "I love this product! It's amazing and works perfectly.",
        "This is the worst purchase I've ever made. Terrible quality.",
        "The movie was okay, not great but not bad either.",
        "Absolutely fantastic! Highly recommend to everyone.",
        "Poor customer service and defective product. Very disappointed.",
        "Good value for money. Satisfied with the purchase.",
        "Excellent quality and fast delivery. Will buy again!",
        "Not worth the price. Expected much better quality.",
        "Average product. Nothing special but does the job.",
        "Outstanding performance! Exceeded my expectations."
    ]
    
    sample_labels = [1, 0, 1, 1, 0, 1, 1, 0, 1, 1]  # 1: positive, 0: negative
    
    # Initialize sentiment analyzer
    analyzer = SentimentAnalyzer()
    
    # Prepare data
    X_train, X_test, y_train, y_test = analyzer.prepare_data(sample_texts, sample_labels, test_size=0.3)
    
    # Create features
    X_train_vec, X_test_vec = analyzer.create_features(X_train, X_test, method='tfidf')
    
    print(f"Training set size: {X_train_vec.shape}")
    print(f"Test set size: {X_test_vec.shape}")
    
    # Train model
    model = analyzer.train_model(X_train_vec, y_train, model_type='logistic')
    
    # Evaluate model
    y_pred = model.predict(X_test_vec)
    
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, target_names=['Negative', 'Positive']))
    
    # Test on new texts
    new_texts = [
        "This product is absolutely wonderful!",
        "I hate this. It's completely useless."
    ]
    
    predictions, probabilities = analyzer.predict_sentiment(new_texts)
    
    print("\nPredictions on new texts:")
    for i, text in enumerate(new_texts):
        sentiment = "Positive" if predictions[i] == 1 else "Negative"
        confidence = max(probabilities[i])
        print(f"Text: {text}")
        print(f"Sentiment: {sentiment} (Confidence: {confidence:.3f})")
        print()
    
    # Get feature importance
    pos_features, neg_features = analyzer.get_feature_importance(top_n=5)
    
    if pos_features and neg_features:
        print("Top positive features:")
        for feature, score in pos_features:
            print(f"  {feature}: {score:.3f}")
        
        print("\nTop negative features:")
        for feature, score in neg_features:
            print(f"  {feature}: {score:.3f}")

nlp_sentiment_example()
```

This completes Part 3 covering TensorFlow, PyTorch, NLP, and Neural Networks with comprehensive implementations and practical examples.