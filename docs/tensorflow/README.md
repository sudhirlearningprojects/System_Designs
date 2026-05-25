# TensorFlow — Complete Deep Learning Guide

A comprehensive, production-focused guide to TensorFlow 2.x — from fundamentals to advanced architectures with senior-level projects.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Core Concepts & Tensors](01_Core_Concepts.md) | Tensors, eager execution, GradientTape, tf.function, variables |
| 2 | [Keras & Model Building](02_Keras_Models.md) | Sequential, Functional, Subclassing, layers, custom components |
| 3 | [Training & Optimization](03_Training.md) | Training loops, optimizers, callbacks, mixed precision, distributed |
| 4 | [CNNs & Computer Vision](04_Computer_Vision.md) | ConvNets, transfer learning, object detection, segmentation |
| 5 | [NLP & Transformers](05_NLP_Transformers.md) | Embeddings, attention, Transformer from scratch, BERT, GPT |
| 6 | [Advanced Architectures](06_Advanced_Architectures.md) | GANs, VAEs, diffusion models, graph neural networks |
| 7 | [Production & Deployment](07_Production.md) | TF Serving, TFLite, TF.js, optimization, monitoring |
| 8 | [Project: Real-Time Anomaly Detection](08_Project_Anomaly_Detection.md) | Production ML pipeline for time-series anomaly detection |
| 9 | [Project: Multi-Modal RAG System](09_Project_MultiModal_RAG.md) | Vision + text retrieval system with custom embeddings |

## 🏗️ TensorFlow Ecosystem (2024-2025)

```
┌─────────────────────────────────────────────────────────────┐
│                    TENSORFLOW ECOSYSTEM                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  MODEL BUILDING                                              │
│  ├── tf.keras (high-level API)                              │
│  ├── tf.Module (low-level custom)                           │
│  └── Keras 3.0 (multi-backend: TF, JAX, PyTorch)           │
│                                                              │
│  TRAINING                                                    │
│  ├── tf.GradientTape (custom training loops)                │
│  ├── tf.distribute (multi-GPU, multi-node, TPU)            │
│  └── Mixed Precision (fp16/bf16 training)                   │
│                                                              │
│  DATA                                                        │
│  ├── tf.data (input pipelines)                              │
│  ├── TFRecord (efficient storage)                           │
│  └── tf.io (file I/O, parsing)                              │
│                                                              │
│  DEPLOYMENT                                                  │
│  ├── TF Serving (production inference server)               │
│  ├── TFLite (mobile/edge)                                   │
│  ├── TF.js (browser/Node.js)                                │
│  └── SavedModel (portable format)                           │
│                                                              │
│  TOOLS                                                       │
│  ├── TensorBoard (visualization)                            │
│  ├── TF Profiler (performance)                              │
│  ├── TF Hub (pre-trained models)                            │
│  └── TFX (ML pipelines)                                     │
└─────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Start

```python
import tensorflow as tf

# Check GPU
print(f"TF version: {tf.__version__}")
print(f"GPUs: {tf.config.list_physical_devices('GPU')}")

# Simple model
model = tf.keras.Sequential([
    tf.keras.layers.Dense(128, activation='relu', input_shape=(784,)),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(10, activation='softmax')
])

model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(x_train, y_train, epochs=5, validation_split=0.2)
```

## 🎯 Version Info

- **TensorFlow**: 2.16+ (current stable)
- **Keras**: 3.0+ (multi-backend, default with TF 2.16+)
- **Python**: 3.9-3.12
- **CUDA**: 12.x (for GPU support)
