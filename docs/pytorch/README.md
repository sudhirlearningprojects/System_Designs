# PyTorch — Complete Deep Learning Guide

A comprehensive, production-focused guide to PyTorch 2.x — from fundamentals to advanced architectures with senior-level projects.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Core Concepts & Tensors](01_Core_Concepts.md) | Tensors, autograd, computation graphs, CUDA, dynamic graphs |
| 2 | [nn.Module & Model Building](02_Model_Building.md) | Modules, layers, custom models, hooks, parameter management |
| 3 | [Training & Optimization](03_Training.md) | Training loops, optimizers, schedulers, mixed precision, DDP |
| 4 | [Computer Vision](04_Computer_Vision.md) | torchvision, transfer learning, detection, segmentation |
| 5 | [NLP & Transformers](05_NLP_Transformers.md) | Attention, Transformer from scratch, HuggingFace integration |
| 6 | [Advanced Architectures](06_Advanced_Architectures.md) | GANs, diffusion, NeRF, state-space models |
| 7 | [Production & Deployment](07_Production.md) | TorchScript, ONNX, TorchServe, quantization, compilation |
| 8 | [Project: LLM Fine-Tuning Pipeline](08_Project_LLM_FineTuning.md) | LoRA/QLoRA fine-tuning with distributed training |
| 9 | [Project: Real-Time Video Understanding](09_Project_Video_Understanding.md) | Video transformer with streaming inference |

## 🏗️ PyTorch Ecosystem (2024-2025)

```
┌─────────────────────────────────────────────────────────────┐
│                     PYTORCH ECOSYSTEM                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  CORE                                                        │
│  ├── torch (tensors, autograd, CUDA)                        │
│  ├── torch.nn (layers, losses, modules)                     │
│  ├── torch.optim (optimizers, schedulers)                   │
│  └── torch.compile (graph compilation, 2.x)                 │
│                                                              │
│  DOMAIN LIBRARIES                                            │
│  ├── torchvision (CV: models, transforms, datasets)         │
│  ├── torchaudio (audio processing)                          │
│  ├── torchtext (NLP utilities)                              │
│  └── torchdata (data loading pipelines)                     │
│                                                              │
│  DISTRIBUTED                                                 │
│  ├── torch.distributed (DDP, FSDP)                          │
│  ├── DeepSpeed (ZeRO, offloading)                           │
│  └── FSDP2 (fully sharded data parallel)                    │
│                                                              │
│  DEPLOYMENT                                                  │
│  ├── TorchScript (JIT compilation)                          │
│  ├── torch.export (graph export, 2.x)                       │
│  ├── ONNX Runtime (cross-platform)                          │
│  ├── TorchServe (production serving)                        │
│  └── ExecuTorch (mobile/edge)                               │
│                                                              │
│  ECOSYSTEM                                                   │
│  ├── HuggingFace Transformers                               │
│  ├── Lightning (training framework)                         │
│  ├── PEFT (parameter-efficient fine-tuning)                 │
│  └── bitsandbytes (quantization)                            │
└─────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Start

```python
import torch
import torch.nn as nn

# Check GPU
print(f"PyTorch: {torch.__version__}")
print(f"CUDA: {torch.cuda.is_available()}, Device: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU'}")

# Simple model
model = nn.Sequential(
    nn.Linear(784, 128),
    nn.ReLU(),
    nn.Dropout(0.2),
    nn.Linear(128, 10)
)

# torch.compile for 2x speedup (PyTorch 2.x)
model = torch.compile(model)
```

## 🔥 PyTorch 2.x Key Features

| Feature | Description |
|---------|-------------|
| `torch.compile` | Graph compilation for 2x speedup (TorchDynamo + TorchInductor) |
| `torch.export` | Clean graph export replacing TorchScript |
| `FSDP2` | Next-gen fully sharded data parallel |
| `FlexAttention` | Custom attention patterns with compilation |
| `torch.distributed.checkpoint` | Async distributed checkpointing |
| `scaled_dot_product_attention` | Fused attention kernel (FlashAttention) |

## PyTorch vs TensorFlow

| Aspect | PyTorch | TensorFlow |
|--------|---------|------------|
| Graph | Dynamic (eager-first) | Static-first (eager optional) |
| Debugging | Standard Python debugger | tf.print, harder to debug |
| Research | Dominant (90%+ papers) | Declining in research |
| Production | TorchServe, ONNX | TF Serving, TFLite |
| Mobile | ExecuTorch | TFLite (more mature) |
| Compilation | torch.compile (2.x) | tf.function + XLA |
| Distributed | DDP, FSDP, DeepSpeed | MirroredStrategy, TPU |
| Ecosystem | HuggingFace, Lightning | Keras, TFX |
