# 1. Core Concepts & Tensors

## Tensors

PyTorch tensors are multi-dimensional arrays with GPU acceleration and automatic differentiation support.

### Creating Tensors

```python
import torch
import numpy as np

# From data
t = torch.tensor([1, 2, 3, 4])                          # int64
t = torch.tensor([[1.0, 2.0], [3.0, 4.0]])              # float32
t = torch.tensor([1, 2, 3], dtype=torch.float32)         # explicit dtype

# Special tensors
zeros = torch.zeros(3, 4)
ones = torch.ones(2, 3)
eye = torch.eye(4)
rand = torch.rand(3, 4)                                  # Uniform [0, 1)
randn = torch.randn(3, 4)                                # Normal(0, 1)
arange = torch.arange(0, 10, 2)                          # [0, 2, 4, 6, 8]
linspace = torch.linspace(0, 1, steps=5)                  # [0, 0.25, 0.5, 0.75, 1]
full = torch.full((3, 4), fill_value=3.14)

# From NumPy (shared memory — zero copy!)
np_array = np.array([1.0, 2.0, 3.0])
t = torch.from_numpy(np_array)                            # Shares memory
t_copy = torch.tensor(np_array)                           # Copies data

# Like another tensor (same shape, dtype, device)
t2 = torch.zeros_like(t)
t3 = torch.randn_like(t)

# Tensor properties
print(f"Shape: {t.shape}")          # torch.Size([3])
print(f"Dtype: {t.dtype}")          # torch.float64
print(f"Device: {t.device}")        # cpu or cuda:0
print(f"Requires grad: {t.requires_grad}")
print(f"Contiguous: {t.is_contiguous()}")
```

### Tensor Operations

```python
a = torch.tensor([[1., 2.], [3., 4.]])
b = torch.tensor([[5., 6.], [7., 8.]])

# Arithmetic (element-wise)
add = a + b                         # or torch.add(a, b)
mul = a * b                         # Element-wise
matmul = a @ b                      # Matrix multiplication (torch.matmul)
power = a ** 2

# In-place operations (underscore suffix)
a.add_(1)                           # a = a + 1 (modifies a)
a.mul_(2)                           # a = a * 2
a.zero_()                           # Fill with zeros

# Reduction
total = a.sum()                     # Sum all
row_sum = a.sum(dim=1)              # Sum along dim 1
col_mean = a.mean(dim=0)            # Mean along dim 0
max_val, max_idx = a.max(dim=1)     # Max with indices

# Reshaping
reshaped = a.view(4, 1)            # Reshape (must be contiguous)
reshaped = a.reshape(4, 1)         # Reshape (works always)
flat = a.flatten()                  # Flatten to 1D
unsqueezed = a.unsqueeze(0)        # Add dim: (1, 2, 2)
squeezed = unsqueezed.squeeze(0)   # Remove dim: (2, 2)
transposed = a.T                    # Transpose
permuted = a.permute(1, 0)         # Permute dimensions

# Concatenation
cat = torch.cat([a, b], dim=0)     # Along existing dim: (4, 2)
stacked = torch.stack([a, b])      # New dim: (2, 2, 2)

# Indexing
row = a[0]                          # First row
elem = a[1, 1]                      # Element at (1,1)
mask = a > 2                        # Boolean mask
filtered = a[mask]                  # Masked selection
a[a > 2] = 0                       # Conditional assignment

# Broadcasting
c = torch.tensor([[1.], [2.]])      # (2, 1)
result = a + c                      # (2, 2) + (2, 1) → (2, 2)
```

---

## CUDA (GPU Computing)

```python
# Check CUDA availability
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Device: {device}")
print(f"GPU: {torch.cuda.get_device_name(0)}")
print(f"Memory: {torch.cuda.get_device_properties(0).total_memory / 1e9:.1f} GB")

# Move tensors to GPU
t = torch.randn(1000, 1000, device='cuda')          # Create on GPU
t_cpu = torch.randn(1000, 1000)
t_gpu = t_cpu.to('cuda')                             # Move to GPU
t_gpu = t_cpu.cuda()                                  # Shorthand

# Move model to GPU
model = model.to(device)

# Mixed device operations (will error!)
# result = t_cpu + t_gpu  # RuntimeError!
result = t_cpu.to(device) + t_gpu  # OK

# Memory management
torch.cuda.empty_cache()                              # Free cached memory
print(f"Allocated: {torch.cuda.memory_allocated() / 1e6:.1f} MB")
print(f"Cached: {torch.cuda.memory_reserved() / 1e6:.1f} MB")

# Multiple GPUs
if torch.cuda.device_count() > 1:
    t0 = torch.randn(100, device='cuda:0')
    t1 = torch.randn(100, device='cuda:1')
```

---

## Autograd (Automatic Differentiation)

PyTorch builds a dynamic computation graph on-the-fly and computes gradients via backpropagation.

### Basic Gradient Computation

```python
# Scalar function
x = torch.tensor(3.0, requires_grad=True)
y = x ** 2 + 2 * x + 1  # y = x² + 2x + 1

y.backward()              # Compute dy/dx
print(x.grad)             # tensor(8.) → dy/dx = 2x + 2 = 8

# Vector function
x = torch.tensor([1.0, 2.0, 3.0], requires_grad=True)
y = (x ** 2).sum()        # Scalar output required for backward()

y.backward()
print(x.grad)             # tensor([2., 4., 6.]) → dy/dx_i = 2x_i
```

### Gradient for Neural Networks

```python
# Manual linear layer
W = torch.randn(2, 1, requires_grad=True)
b = torch.zeros(1, requires_grad=True)

x = torch.tensor([[1.0, 2.0], [3.0, 4.0], [5.0, 6.0]])
y_true = torch.tensor([[3.0], [7.0], [11.0]])

# Forward pass
y_pred = x @ W + b
loss = ((y_true - y_pred) ** 2).mean()

# Backward pass
loss.backward()

# Gradients available
print(f"dL/dW: {W.grad}")
print(f"dL/db: {b.grad}")

# Manual SGD update
with torch.no_grad():  # Don't track these operations
    W -= 0.01 * W.grad
    b -= 0.01 * b.grad

# IMPORTANT: Zero gradients before next iteration
W.grad.zero_()
b.grad.zero_()
```

### Gradient Control

```python
# Disable gradient tracking (inference, non-differentiable ops)
with torch.no_grad():
    output = model(input)

# Detach from computation graph
detached = tensor.detach()  # New tensor, no grad history

# Stop gradient flow selectively
x = torch.randn(3, requires_grad=True)
y = x * 2
z = y.detach() * 3  # Gradient won't flow through z to x

# Gradient accumulation (useful for large effective batch)
for i, (x, y) in enumerate(dataloader):
    loss = criterion(model(x), y) / accumulation_steps
    loss.backward()  # Gradients accumulate
    
    if (i + 1) % accumulation_steps == 0:
        optimizer.step()
        optimizer.zero_grad()

# Higher-order gradients
x = torch.tensor(2.0, requires_grad=True)
y = x ** 4

# First derivative
grad1 = torch.autograd.grad(y, x, create_graph=True)[0]  # 4x³ = 32

# Second derivative
grad2 = torch.autograd.grad(grad1, x)[0]  # 12x² = 48
```

---

## torch.compile (PyTorch 2.x)

The biggest PyTorch 2.x feature — compiles models for 2x+ speedup.

```python
model = MyModel()

# Basic compilation (recommended default)
compiled_model = torch.compile(model)

# With options
compiled_model = torch.compile(
    model,
    mode="reduce-overhead",    # Best for small models, reduces Python overhead
    # mode="max-autotune",     # Best for large models, tries many optimizations
    # mode="default",          # Balanced (default)
    fullgraph=True,            # Ensure entire model is captured as one graph
    dynamic=True,              # Handle dynamic shapes (variable batch size)
)

# Usage is identical to eager mode
output = compiled_model(input_tensor)

# Compile individual functions
@torch.compile
def train_step(model, x, y, optimizer, criterion):
    optimizer.zero_grad()
    output = model(x)
    loss = criterion(output, y)
    loss.backward()
    optimizer.step()
    return loss
```

**How it works:**
1. **TorchDynamo**: Captures Python bytecode into FX graph
2. **TorchInductor**: Compiles FX graph to optimized Triton/C++ kernels
3. **Result**: Fused operations, reduced memory, GPU kernel optimization

---

## Data Loading

```python
from torch.utils.data import Dataset, DataLoader

class CustomDataset(Dataset):
    def __init__(self, data, labels, transform=None):
        self.data = data
        self.labels = labels
        self.transform = transform
    
    def __len__(self):
        return len(self.data)
    
    def __getitem__(self, idx):
        x = self.data[idx]
        y = self.labels[idx]
        if self.transform:
            x = self.transform(x)
        return x, y

# DataLoader with performance options
dataloader = DataLoader(
    dataset,
    batch_size=64,
    shuffle=True,
    num_workers=4,              # Parallel data loading
    pin_memory=True,            # Faster CPU→GPU transfer
    persistent_workers=True,    # Keep workers alive between epochs
    prefetch_factor=2,          # Prefetch 2 batches per worker
    drop_last=True,             # Drop incomplete last batch (for DDP)
)

# Iterate
for batch_idx, (x, y) in enumerate(dataloader):
    x, y = x.to(device), y.to(device)
    # ... training step
```

---

## Next: [nn.Module & Model Building →](02_Model_Building.md)
