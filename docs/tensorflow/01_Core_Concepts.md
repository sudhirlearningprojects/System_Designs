# 1. Core Concepts & Tensors

## What is TensorFlow?

TensorFlow is an end-to-end open-source platform for machine learning. It provides:
- **Computation engine**: Efficient numerical computation on CPUs, GPUs, TPUs
- **Automatic differentiation**: Compute gradients for any differentiable computation
- **Model building**: High-level APIs (Keras) and low-level primitives
- **Production deployment**: Serving, mobile, browser, edge devices

---

## Tensors

Tensors are multi-dimensional arrays — the fundamental data structure in TensorFlow.

### Creating Tensors

```python
import tensorflow as tf
import numpy as np

# Constants (immutable)
scalar = tf.constant(3.14)                          # Shape: ()
vector = tf.constant([1, 2, 3, 4])                  # Shape: (4,)
matrix = tf.constant([[1, 2], [3, 4], [5, 6]])      # Shape: (3, 2)
tensor_3d = tf.constant([[[1, 2], [3, 4]], [[5, 6], [7, 8]]])  # Shape: (2, 2, 2)

# From NumPy
np_array = np.random.randn(3, 4).astype(np.float32)
tensor = tf.constant(np_array)

# Special tensors
zeros = tf.zeros([3, 4])
ones = tf.ones([2, 3])
eye = tf.eye(4)                                     # Identity matrix
random_normal = tf.random.normal([3, 4], mean=0.0, stddev=1.0)
random_uniform = tf.random.uniform([3, 4], minval=0, maxval=1)

# Tensor properties
print(f"Shape: {tensor.shape}")        # (3, 4)
print(f"Dtype: {tensor.dtype}")        # float32
print(f"Device: {tensor.device}")      # /job:localhost/replica:0/task:0/device:GPU:0
print(f"Rank: {tf.rank(tensor)}")      # 2
print(f"Size: {tf.size(tensor)}")      # 12
```

### Tensor Operations

```python
a = tf.constant([[1., 2.], [3., 4.]])
b = tf.constant([[5., 6.], [7., 8.]])

# Arithmetic
add = a + b                    # Element-wise addition
mul = a * b                    # Element-wise multiplication
matmul = a @ b                 # Matrix multiplication (same as tf.matmul(a, b))
power = a ** 2                 # Element-wise power

# Reduction
total = tf.reduce_sum(a)                    # Sum all elements
row_sum = tf.reduce_sum(a, axis=1)          # Sum along rows
col_mean = tf.reduce_mean(a, axis=0)        # Mean along columns
max_val = tf.reduce_max(a)                  # Maximum value

# Reshaping
reshaped = tf.reshape(a, [4, 1])            # Reshape to (4, 1)
transposed = tf.transpose(a)                # Transpose
expanded = tf.expand_dims(a, axis=0)        # Add batch dimension: (1, 2, 2)
squeezed = tf.squeeze(expanded)             # Remove size-1 dimensions

# Indexing and slicing
row = a[0]                                  # First row
element = a[1, 1]                           # Element at (1,1)
sliced = a[:, 0:1]                          # First column

# Concatenation
concat = tf.concat([a, b], axis=0)          # Stack vertically: (4, 2)
stacked = tf.stack([a, b], axis=0)          # New dimension: (2, 2, 2)

# Broadcasting
c = tf.constant([[1.], [2.]])               # Shape: (2, 1)
broadcast_result = a + c                    # (2, 2) + (2, 1) → (2, 2)
```

### Data Types and Casting

```python
# Common dtypes
float_tensor = tf.constant([1.0, 2.0], dtype=tf.float32)
int_tensor = tf.constant([1, 2], dtype=tf.int32)
bool_tensor = tf.constant([True, False])
string_tensor = tf.constant(["hello", "world"])

# Casting
casted = tf.cast(int_tensor, tf.float32)
to_int = tf.cast(float_tensor, tf.int32)

# Mixed precision (for training efficiency)
half_tensor = tf.cast(float_tensor, tf.float16)
bfloat_tensor = tf.cast(float_tensor, tf.bfloat16)
```

---

## Variables (Mutable Tensors)

Variables hold mutable state — used for model parameters (weights, biases).

```python
# Create variables
weights = tf.Variable(tf.random.normal([784, 128]), name='weights')
bias = tf.Variable(tf.zeros([128]), name='bias')

# Modify variables
weights.assign(tf.random.normal([784, 128]))       # Full replacement
weights.assign_add(tf.ones([784, 128]) * 0.01)     # In-place addition
weights[0, :].assign(tf.zeros([128]))              # Partial assignment

# Variable properties
print(f"Name: {weights.name}")
print(f"Shape: {weights.shape}")
print(f"Trainable: {weights.trainable}")

# Non-trainable variable (e.g., batch norm running mean)
step_counter = tf.Variable(0, trainable=False, name='step')
```

---

## Automatic Differentiation (GradientTape)

The core of training neural networks — compute gradients of any computation.

### Basic Gradient Computation

```python
x = tf.Variable(3.0)

with tf.GradientTape() as tape:
    y = x ** 2 + 2 * x + 1  # y = x² + 2x + 1

# dy/dx = 2x + 2 = 2(3) + 2 = 8
grad = tape.gradient(y, x)
print(f"Gradient: {grad.numpy()}")  # 8.0
```

### Gradients for Neural Network

```python
# Simple linear model
W = tf.Variable(tf.random.normal([2, 1]))
b = tf.Variable(tf.zeros([1]))

x = tf.constant([[1.0, 2.0], [3.0, 4.0], [5.0, 6.0]])
y_true = tf.constant([[3.0], [7.0], [11.0]])

with tf.GradientTape() as tape:
    y_pred = x @ W + b
    loss = tf.reduce_mean((y_true - y_pred) ** 2)

# Compute gradients of loss w.r.t. all trainable variables
gradients = tape.gradient(loss, [W, b])
print(f"dL/dW shape: {gradients[0].shape}")  # (2, 1)
print(f"dL/db shape: {gradients[1].shape}")  # (1,)

# Apply gradients (manual SGD)
learning_rate = 0.01
W.assign_sub(learning_rate * gradients[0])
b.assign_sub(learning_rate * gradients[1])
```

### Persistent Tape (Multiple Gradients)

```python
x = tf.Variable(3.0)

with tf.GradientTape(persistent=True) as tape:
    y = x ** 3
    z = 2 * y

# Can call gradient multiple times with persistent=True
dy_dx = tape.gradient(y, x)   # 3x² = 27
dz_dx = tape.gradient(z, x)   # 6x² = 54

del tape  # Must manually delete persistent tape
```

### Higher-Order Gradients

```python
x = tf.Variable(2.0)

with tf.GradientTape() as tape2:
    with tf.GradientTape() as tape1:
        y = x ** 4  # y = x⁴
    
    # First derivative: dy/dx = 4x³
    dy_dx = tape1.gradient(y, x)

# Second derivative: d²y/dx² = 12x²
d2y_dx2 = tape2.gradient(dy_dx, x)
print(f"f''(2) = {d2y_dx2.numpy()}")  # 48.0
```

### Gradient Tape with Custom Training Loop

```python
model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu'),
    tf.keras.layers.Dense(10)
])

optimizer = tf.keras.optimizers.Adam(learning_rate=0.001)
loss_fn = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)

@tf.function  # Compile for performance
def train_step(x_batch, y_batch):
    with tf.GradientTape() as tape:
        logits = model(x_batch, training=True)
        loss = loss_fn(y_batch, logits)
    
    gradients = tape.gradient(loss, model.trainable_variables)
    optimizer.apply_gradients(zip(gradients, model.trainable_variables))
    return loss
```

---

## tf.function (Graph Compilation)

Converts Python functions to TensorFlow graphs for performance.

```python
@tf.function
def compute(x):
    return tf.reduce_sum(x ** 2)

# First call: traces the function and builds a graph
result = compute(tf.constant([1.0, 2.0, 3.0]))

# Subsequent calls: executes the compiled graph (much faster)
result = compute(tf.constant([4.0, 5.0, 6.0]))
```

### When to Use tf.function

```python
# ✅ GOOD: Compute-heavy operations
@tf.function
def train_step(x, y):
    with tf.GradientTape() as tape:
        predictions = model(x, training=True)
        loss = loss_fn(y, predictions)
    gradients = tape.gradient(loss, model.trainable_variables)
    optimizer.apply_gradients(zip(gradients, model.trainable_variables))
    return loss

# ❌ BAD: Python side effects, print statements, file I/O
@tf.function
def bad_function(x):
    print("This only prints during tracing!")  # Won't print on subsequent calls
    return x + 1

# ✅ Use tf.print for debugging inside tf.function
@tf.function
def debug_function(x):
    tf.print("Value:", x)  # This prints every call
    return x + 1
```

### Input Signatures (Avoid Retracing)

```python
@tf.function(input_signature=[tf.TensorSpec(shape=[None, 784], dtype=tf.float32)])
def predict(x):
    return model(x)

# Now accepts any batch size without retracing
predict(tf.random.normal([1, 784]))
predict(tf.random.normal([32, 784]))
predict(tf.random.normal([128, 784]))
```

---

## tf.data (Input Pipelines)

Efficient, scalable data loading.

```python
# From tensors
dataset = tf.data.Dataset.from_tensor_slices((x_train, y_train))

# Pipeline
dataset = (
    dataset
    .shuffle(buffer_size=10000)
    .batch(32)
    .prefetch(tf.data.AUTOTUNE)  # Overlap data loading with training
)

# From files (TFRecord)
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(parse_fn, num_parallel_calls=tf.data.AUTOTUNE)
dataset = dataset.batch(32).prefetch(tf.data.AUTOTUNE)

# From generator
def data_generator():
    for i in range(1000):
        yield np.random.randn(224, 224, 3).astype(np.float32), np.random.randint(0, 10)

dataset = tf.data.Dataset.from_generator(
    data_generator,
    output_signature=(
        tf.TensorSpec(shape=(224, 224, 3), dtype=tf.float32),
        tf.TensorSpec(shape=(), dtype=tf.int32)
    )
)

# Performance optimization
dataset = (
    dataset
    .cache()                                    # Cache in memory after first epoch
    .shuffle(1000)
    .batch(32, drop_remainder=True)             # Fixed batch size for XLA
    .map(augment_fn, num_parallel_calls=tf.data.AUTOTUNE)
    .prefetch(tf.data.AUTOTUNE)
)
```

---

## Device Placement

```python
# Check available devices
print(tf.config.list_physical_devices())

# Explicit placement
with tf.device('/GPU:0'):
    a = tf.constant([[1.0, 2.0], [3.0, 4.0]])
    b = tf.constant([[5.0, 6.0], [7.0, 8.0]])
    c = tf.matmul(a, b)

# Memory growth (prevent TF from allocating all GPU memory)
gpus = tf.config.list_physical_devices('GPU')
for gpu in gpus:
    tf.config.experimental.set_memory_growth(gpu, True)

# Limit GPU memory
tf.config.set_logical_device_configuration(
    gpus[0],
    [tf.config.LogicalDeviceConfiguration(memory_limit=4096)]  # 4GB
)
```

---

## Next: [Keras & Model Building →](02_Keras_Models.md)
