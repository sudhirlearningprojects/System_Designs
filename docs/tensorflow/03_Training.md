# 3. Training & Optimization

## Custom Training Loop

```python
import tensorflow as tf
from tensorflow import keras

model = build_model()
optimizer = keras.optimizers.AdamW(learning_rate=1e-3, weight_decay=0.01)
loss_fn = keras.losses.SparseCategoricalCrossentropy(from_logits=True)

train_acc = keras.metrics.SparseCategoricalAccuracy()
val_acc = keras.metrics.SparseCategoricalAccuracy()

@tf.function
def train_step(x, y):
    with tf.GradientTape() as tape:
        logits = model(x, training=True)
        loss = loss_fn(y, logits)
        # Add regularization losses
        loss += sum(model.losses)
    
    gradients = tape.gradient(loss, model.trainable_variables)
    # Gradient clipping
    gradients, global_norm = tf.clip_by_global_norm(gradients, 1.0)
    optimizer.apply_gradients(zip(gradients, model.trainable_variables))
    
    train_acc.update_state(y, logits)
    return loss

@tf.function
def val_step(x, y):
    logits = model(x, training=False)
    val_acc.update_state(y, logits)

# Training loop
for epoch in range(100):
    train_acc.reset_state()
    val_acc.reset_state()
    
    for x_batch, y_batch in train_dataset:
        loss = train_step(x_batch, y_batch)
    
    for x_batch, y_batch in val_dataset:
        val_step(x_batch, y_batch)
    
    print(f"Epoch {epoch}: loss={loss:.4f}, train_acc={train_acc.result():.4f}, val_acc={val_acc.result():.4f}")
```

---

## Optimizers

```python
# AdamW (recommended for most tasks)
optimizer = keras.optimizers.AdamW(learning_rate=1e-3, weight_decay=0.01)

# SGD with momentum (for fine-tuning, large batch)
optimizer = keras.optimizers.SGD(learning_rate=0.1, momentum=0.9, nesterov=True)

# LAMB (for very large batch training)
optimizer = keras.optimizers.experimental.LAMB(learning_rate=1e-3)
```

### Learning Rate Schedules

```python
# Cosine decay with warmup
class WarmupCosineDecay(keras.optimizers.schedules.LearningRateSchedule):
    def __init__(self, initial_lr, warmup_steps, total_steps):
        self.initial_lr = initial_lr
        self.warmup_steps = warmup_steps
        self.total_steps = total_steps
    
    def __call__(self, step):
        step = tf.cast(step, tf.float32)
        warmup_lr = self.initial_lr * (step / self.warmup_steps)
        
        decay_steps = self.total_steps - self.warmup_steps
        progress = (step - self.warmup_steps) / decay_steps
        cosine_lr = self.initial_lr * 0.5 * (1 + tf.cos(np.pi * progress))
        
        return tf.where(step < self.warmup_steps, warmup_lr, cosine_lr)

schedule = WarmupCosineDecay(initial_lr=1e-3, warmup_steps=1000, total_steps=50000)
optimizer = keras.optimizers.Adam(learning_rate=schedule)
```

---

## Mixed Precision Training

Train with float16 for 2-3x speedup on modern GPUs.

```python
# Enable mixed precision globally
keras.mixed_precision.set_global_policy('mixed_float16')

# Model automatically uses float16 for compute, float32 for accumulation
model = build_model()

# Loss scaling (prevents underflow in float16 gradients)
optimizer = keras.optimizers.Adam(1e-3)
# Keras handles loss scaling automatically with mixed precision policy

model.compile(optimizer=optimizer, loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(train_dataset, epochs=10)
```

---

## Distributed Training

### Multi-GPU (MirroredStrategy)

```python
strategy = tf.distribute.MirroredStrategy()  # All available GPUs
print(f"Number of devices: {strategy.num_replicas_in_sync}")

with strategy.scope():
    model = build_model()
    model.compile(
        optimizer=keras.optimizers.Adam(1e-3 * strategy.num_replicas_in_sync),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

# Batch size scales with number of GPUs
global_batch_size = 64 * strategy.num_replicas_in_sync
model.fit(train_dataset.batch(global_batch_size), epochs=10)
```

### Multi-Node (MultiWorkerMirroredStrategy)

```python
strategy = tf.distribute.MultiWorkerMirroredStrategy()

with strategy.scope():
    model = build_model()
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy')

model.fit(train_dataset, epochs=10)
```

### TPU Training

```python
resolver = tf.distribute.cluster_resolver.TPUClusterResolver()
tf.config.experimental_connect_to_cluster(resolver)
tf.tpu.experimental.initialize_tpu_system(resolver)
strategy = tf.distribute.TPUStrategy(resolver)

with strategy.scope():
    model = build_model()
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy')

model.fit(train_dataset, epochs=10)
```

---

## Callbacks

```python
callbacks = [
    # Early stopping
    keras.callbacks.EarlyStopping(
        monitor='val_loss', patience=10, restore_best_weights=True
    ),
    # Learning rate reduction
    keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.5, patience=5, min_lr=1e-7
    ),
    # Model checkpointing
    keras.callbacks.ModelCheckpoint(
        'models/best.keras', monitor='val_accuracy', save_best_only=True
    ),
    # TensorBoard
    keras.callbacks.TensorBoard(log_dir='logs', histogram_freq=1, profile_batch='10,20'),
    # CSV logging
    keras.callbacks.CSVLogger('training_log.csv'),
    # Custom callback
    keras.callbacks.LambdaCallback(
        on_epoch_end=lambda epoch, logs: print(f"LR: {optimizer.learning_rate.numpy():.6f}")
    ),
]
```

---

## Gradient Accumulation (Large Effective Batch)

```python
class GradientAccumulator:
    """Accumulate gradients over multiple steps for large effective batch size."""
    
    def __init__(self, model, optimizer, loss_fn, accumulation_steps=4):
        self.model = model
        self.optimizer = optimizer
        self.loss_fn = loss_fn
        self.accumulation_steps = accumulation_steps
        self.gradient_accumulator = [
            tf.Variable(tf.zeros_like(v), trainable=False) 
            for v in model.trainable_variables
        ]
    
    @tf.function
    def train_step(self, x, y, step):
        with tf.GradientTape() as tape:
            predictions = self.model(x, training=True)
            loss = self.loss_fn(y, predictions) / self.accumulation_steps
        
        gradients = tape.gradient(loss, self.model.trainable_variables)
        
        # Accumulate
        for acc, grad in zip(self.gradient_accumulator, gradients):
            acc.assign_add(grad)
        
        # Apply when accumulated enough
        if (step + 1) % self.accumulation_steps == 0:
            self.optimizer.apply_gradients(
                zip(self.gradient_accumulator, self.model.trainable_variables)
            )
            for acc in self.gradient_accumulator:
                acc.assign(tf.zeros_like(acc))
        
        return loss * self.accumulation_steps
```

---

## Next: [CNNs & Computer Vision →](04_Computer_Vision.md)
