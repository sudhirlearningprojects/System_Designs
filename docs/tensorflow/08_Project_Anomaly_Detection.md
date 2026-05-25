# 8. Project: Real-Time Anomaly Detection System

## Overview

A production-grade time-series anomaly detection system using TensorFlow for detecting anomalies in infrastructure metrics (CPU, memory, network, application latency) in real-time.

**Architecture**: Variational Autoencoder (VAE) + Temporal Convolutional Network (TCN) with online learning.

```
┌─────────────────────────────────────────────────────────────────┐
│                 ANOMALY DETECTION PIPELINE                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Metrics Stream → Preprocessing → Model Inference → Alert Engine │
│  (Kafka/Kinesis)   (windowing,     (VAE + TCN)      (threshold,  │
│                     normalization)                    notification)│
│                                                                   │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────┐ │
│  │ Ingestion│→ │ Feature Eng  │→ │ TF Serving   │→ │ Alerting│ │
│  │ (stream) │  │ (tf.data)    │  │ (SavedModel) │  │ (rules) │ │
│  └──────────┘  └──────────────┘  └──────────────┘  └─────────┘ │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Online Learning: Retrain on new normal patterns hourly    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Model Architecture

### Temporal Convolutional VAE

```python
import tensorflow as tf
from tensorflow import keras
from keras import layers
import numpy as np

class CausalConv1D(layers.Layer):
    """Causal convolution — only looks at past data (no future leakage)."""
    
    def __init__(self, filters, kernel_size, dilation_rate=1, **kwargs):
        super().__init__(**kwargs)
        self.padding_size = (kernel_size - 1) * dilation_rate
        self.conv = layers.Conv1D(
            filters, kernel_size, padding='causal',
            dilation_rate=dilation_rate, activation=None
        )
        self.norm = layers.LayerNormalization()
        self.activation = layers.Activation('gelu')
    
    def call(self, x):
        return self.activation(self.norm(self.conv(x)))


class TCNBlock(layers.Layer):
    """Temporal Convolutional Network block with residual connection."""
    
    def __init__(self, filters, kernel_size, dilation_rate, dropout_rate=0.1, **kwargs):
        super().__init__(**kwargs)
        self.conv1 = CausalConv1D(filters, kernel_size, dilation_rate)
        self.conv2 = CausalConv1D(filters, kernel_size, dilation_rate)
        self.dropout = layers.Dropout(dropout_rate)
        self.residual_conv = None  # Set in build
        self.filters = filters
    
    def build(self, input_shape):
        if input_shape[-1] != self.filters:
            self.residual_conv = layers.Conv1D(self.filters, 1)
    
    def call(self, x, training=False):
        residual = x
        out = self.conv1(x)
        out = self.dropout(out, training=training)
        out = self.conv2(out)
        out = self.dropout(out, training=training)
        
        if self.residual_conv:
            residual = self.residual_conv(residual)
        
        return out + residual


class Sampling(layers.Layer):
    """Reparameterization trick for VAE."""
    
    def call(self, inputs):
        z_mean, z_log_var = inputs
        epsilon = tf.random.normal(shape=tf.shape(z_mean))
        return z_mean + tf.exp(0.5 * z_log_var) * epsilon


class TemporalVAE(keras.Model):
    """Variational Autoencoder with Temporal Convolutional encoder/decoder.
    
    Detects anomalies by measuring reconstruction error — normal patterns
    are reconstructed well, anomalies have high reconstruction error.
    """
    
    def __init__(self, window_size, num_features, latent_dim=32, 
                 tcn_filters=64, num_tcn_layers=4, **kwargs):
        super().__init__(**kwargs)
        self.window_size = window_size
        self.num_features = num_features
        self.latent_dim = latent_dim
        
        # Encoder
        self.encoder_tcn_blocks = []
        for i in range(num_tcn_layers):
            dilation = 2 ** i
            self.encoder_tcn_blocks.append(
                TCNBlock(tcn_filters, kernel_size=3, dilation_rate=dilation)
            )
        
        self.encoder_pool = layers.GlobalAveragePooling1D()
        self.z_mean_dense = layers.Dense(latent_dim)
        self.z_log_var_dense = layers.Dense(latent_dim)
        self.sampling = Sampling()
        
        # Decoder
        self.decoder_dense = layers.Dense(window_size * tcn_filters)
        self.decoder_reshape = layers.Reshape((window_size, tcn_filters))
        self.decoder_tcn_blocks = []
        for i in range(num_tcn_layers):
            dilation = 2 ** (num_tcn_layers - 1 - i)
            self.decoder_tcn_blocks.append(
                TCNBlock(tcn_filters, kernel_size=3, dilation_rate=dilation)
            )
        self.output_dense = layers.Dense(num_features)
        
        # Loss trackers
        self.total_loss_tracker = keras.metrics.Mean(name="total_loss")
        self.reconstruction_loss_tracker = keras.metrics.Mean(name="reconstruction_loss")
        self.kl_loss_tracker = keras.metrics.Mean(name="kl_loss")
    
    def encode(self, x, training=False):
        h = x
        for block in self.encoder_tcn_blocks:
            h = block(h, training=training)
        h = self.encoder_pool(h)
        z_mean = self.z_mean_dense(h)
        z_log_var = self.z_log_var_dense(h)
        z = self.sampling([z_mean, z_log_var])
        return z_mean, z_log_var, z
    
    def decode(self, z, training=False):
        h = self.decoder_dense(z)
        h = self.decoder_reshape(h)
        for block in self.decoder_tcn_blocks:
            h = block(h, training=training)
        return self.output_dense(h)
    
    def call(self, x, training=False):
        z_mean, z_log_var, z = self.encode(x, training)
        reconstruction = self.decode(z, training)
        return reconstruction
    
    def train_step(self, data):
        with tf.GradientTape() as tape:
            z_mean, z_log_var, z = self.encode(data, training=True)
            reconstruction = self.decode(z, training=True)
            
            # Reconstruction loss (MSE)
            reconstruction_loss = tf.reduce_mean(
                tf.reduce_sum(tf.square(data - reconstruction), axis=[1, 2])
            )
            
            # KL divergence loss
            kl_loss = -0.5 * tf.reduce_mean(
                tf.reduce_sum(1 + z_log_var - tf.square(z_mean) - tf.exp(z_log_var), axis=1)
            )
            
            # Total loss with KL annealing
            total_loss = reconstruction_loss + 0.1 * kl_loss
        
        gradients = tape.gradient(total_loss, self.trainable_variables)
        self.optimizer.apply_gradients(zip(gradients, self.trainable_variables))
        
        self.total_loss_tracker.update_state(total_loss)
        self.reconstruction_loss_tracker.update_state(reconstruction_loss)
        self.kl_loss_tracker.update_state(kl_loss)
        
        return {
            "total_loss": self.total_loss_tracker.result(),
            "reconstruction_loss": self.reconstruction_loss_tracker.result(),
            "kl_loss": self.kl_loss_tracker.result(),
        }
    
    @property
    def metrics(self):
        return [self.total_loss_tracker, self.reconstruction_loss_tracker, self.kl_loss_tracker]
```

---

## Data Pipeline

```python
class AnomalyDataPipeline:
    """Production data pipeline for time-series anomaly detection."""
    
    def __init__(self, window_size=60, stride=1, num_features=10):
        self.window_size = window_size
        self.stride = stride
        self.num_features = num_features
        self.scaler_mean = None
        self.scaler_std = None
    
    def fit_transform(self, data: np.ndarray) -> tf.data.Dataset:
        """Fit scaler and create windowed dataset from training data."""
        # Fit normalization parameters
        self.scaler_mean = data.mean(axis=0)
        self.scaler_std = data.std(axis=0) + 1e-8
        
        normalized = (data - self.scaler_mean) / self.scaler_std
        return self._create_dataset(normalized)
    
    def transform(self, data: np.ndarray) -> tf.data.Dataset:
        """Transform new data using fitted scaler."""
        normalized = (data - self.scaler_mean) / self.scaler_std
        return self._create_dataset(normalized)
    
    def _create_dataset(self, data: np.ndarray) -> tf.data.Dataset:
        """Create sliding window dataset."""
        dataset = tf.keras.utils.timeseries_dataset_from_array(
            data=data,
            targets=None,
            sequence_length=self.window_size,
            sequence_stride=self.stride,
            batch_size=64,
            shuffle=True
        )
        return dataset.prefetch(tf.data.AUTOTUNE)
    
    def create_streaming_window(self, buffer: np.ndarray) -> tf.Tensor:
        """Create single window for real-time inference."""
        normalized = (buffer - self.scaler_mean) / self.scaler_std
        return tf.expand_dims(tf.constant(normalized, dtype=tf.float32), 0)
```

---

## Anomaly Scoring & Detection

```python
class AnomalyDetector:
    """Production anomaly detector with adaptive thresholding."""
    
    def __init__(self, model: TemporalVAE, pipeline: AnomalyDataPipeline,
                 contamination_ratio: float = 0.01):
        self.model = model
        self.pipeline = pipeline
        self.contamination = contamination_ratio
        self.threshold = None
        self.score_history = []
    
    def calibrate(self, normal_data: np.ndarray):
        """Set threshold using normal data (no anomalies)."""
        dataset = self.pipeline.transform(normal_data)
        scores = []
        
        for batch in dataset:
            reconstruction = self.model(batch, training=False)
            batch_scores = tf.reduce_mean(tf.square(batch - reconstruction), axis=[1, 2])
            scores.extend(batch_scores.numpy())
        
        # Set threshold at (1 - contamination) percentile
        self.threshold = np.percentile(scores, (1 - self.contamination) * 100)
        print(f"Calibrated threshold: {self.threshold:.4f}")
        return self.threshold
    
    def detect(self, window: np.ndarray) -> dict:
        """Detect anomaly in a single window. Returns score and label."""
        input_tensor = self.pipeline.create_streaming_window(window)
        reconstruction = self.model(input_tensor, training=False)
        
        # Reconstruction error as anomaly score
        score = tf.reduce_mean(tf.square(input_tensor - reconstruction)).numpy()
        
        # Per-feature scores (for root cause analysis)
        feature_scores = tf.reduce_mean(
            tf.square(input_tensor - reconstruction), axis=1
        ).numpy()[0]
        
        is_anomaly = score > self.threshold
        
        self.score_history.append(score)
        
        return {
            "score": float(score),
            "threshold": float(self.threshold),
            "is_anomaly": bool(is_anomaly),
            "severity": self._compute_severity(score),
            "feature_contributions": {
                f"feature_{i}": float(s) for i, s in enumerate(feature_scores)
            }
        }
    
    def _compute_severity(self, score: float) -> str:
        if score < self.threshold:
            return "normal"
        ratio = score / self.threshold
        if ratio < 2.0:
            return "low"
        elif ratio < 5.0:
            return "medium"
        elif ratio < 10.0:
            return "high"
        return "critical"
```

---

## Training Pipeline

```python
def train_anomaly_detector(
    train_data: np.ndarray,
    val_data: np.ndarray,
    window_size: int = 60,
    num_features: int = 10,
    epochs: int = 100,
    model_path: str = "models/anomaly_detector"
):
    """End-to-end training pipeline."""
    
    # Data pipeline
    pipeline = AnomalyDataPipeline(window_size=window_size, num_features=num_features)
    train_dataset = pipeline.fit_transform(train_data)
    val_dataset = pipeline.transform(val_data)
    
    # Model
    model = TemporalVAE(
        window_size=window_size,
        num_features=num_features,
        latent_dim=32,
        tcn_filters=64,
        num_tcn_layers=5
    )
    
    # Compile with learning rate schedule
    lr_schedule = keras.optimizers.schedules.CosineDecay(
        initial_learning_rate=1e-3,
        decay_steps=epochs * len(train_dataset),
        alpha=1e-5
    )
    model.compile(optimizer=keras.optimizers.Adam(lr_schedule))
    
    # Callbacks
    callbacks = [
        keras.callbacks.EarlyStopping(
            monitor='val_total_loss', patience=10, restore_best_weights=True
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_total_loss', factor=0.5, patience=5
        ),
        keras.callbacks.TensorBoard(log_dir='logs/anomaly_detector'),
        keras.callbacks.ModelCheckpoint(
            f'{model_path}/best_model.keras', save_best_only=True
        ),
    ]
    
    # Train
    history = model.fit(
        train_dataset,
        validation_data=val_dataset,
        epochs=epochs,
        callbacks=callbacks
    )
    
    # Calibrate detector
    detector = AnomalyDetector(model, pipeline)
    detector.calibrate(val_data)
    
    # Export for TF Serving
    model.save(f'{model_path}/saved_model')
    
    return model, detector, pipeline, history
```

---

## Deployment with TF Serving

```python
# Export model with serving signature
@tf.function(input_signature=[tf.TensorSpec(shape=[None, 60, 10], dtype=tf.float32)])
def serve_predict(input_window):
    reconstruction = model(input_window, training=False)
    anomaly_score = tf.reduce_mean(tf.square(input_window - reconstruction), axis=[1, 2])
    return {"reconstruction": reconstruction, "anomaly_score": anomaly_score}

tf.saved_model.save(model, "models/serving/1", signatures={"serving_default": serve_predict})
```

```bash
# Deploy with TF Serving (Docker)
docker run -p 8501:8501 \
  --mount type=bind,source=$(pwd)/models/serving,target=/models/anomaly_detector \
  -e MODEL_NAME=anomaly_detector \
  tensorflow/serving:latest
```

```python
# Client inference
import requests

def detect_anomaly_remote(window: np.ndarray) -> dict:
    payload = {"instances": window.tolist()}
    response = requests.post(
        "http://localhost:8501/v1/models/anomaly_detector:predict",
        json=payload
    )
    result = response.json()
    score = result["predictions"][0]["anomaly_score"]
    return {"score": score, "is_anomaly": score > threshold}
```

---

## Key Design Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Architecture | VAE + TCN | VAE gives probabilistic anomaly scores; TCN captures temporal patterns without RNN overhead |
| Loss | MSE + KL | Reconstruction error for detection; KL for regularized latent space |
| Windowing | 60 timesteps | Captures 1-minute patterns at 1Hz sampling |
| Threshold | Percentile-based | Adaptive to data distribution; no manual tuning |
| Deployment | TF Serving | Low-latency gRPC/REST, auto-batching, model versioning |
| Online learning | Hourly retrain | Adapts to concept drift without catastrophic forgetting |

---

## Next: [Project: Multi-Modal RAG System →](09_Project_MultiModal_RAG.md)
