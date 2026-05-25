# 2. Keras & Model Building

## Three Ways to Build Models

| API | Use Case | Flexibility | Complexity |
|-----|----------|-------------|------------|
| **Sequential** | Linear stack of layers | Low | Simplest |
| **Functional** | Multi-input/output, shared layers, DAGs | Medium | Moderate |
| **Subclassing** | Full control, dynamic architectures | Highest | Most complex |

---

## Sequential API

```python
import tensorflow as tf
from tensorflow import keras
from keras import layers

model = keras.Sequential([
    layers.Input(shape=(784,)),
    layers.Dense(512, activation='relu', kernel_regularizer=keras.regularizers.l2(0.001)),
    layers.BatchNormalization(),
    layers.Dropout(0.3),
    layers.Dense(256, activation='relu'),
    layers.BatchNormalization(),
    layers.Dropout(0.3),
    layers.Dense(10, activation='softmax')
], name='mnist_classifier')

model.summary()
```

---

## Functional API

For complex architectures: multi-input, multi-output, residual connections, shared layers.

### ResNet-Style Block

```python
def residual_block(x, filters, stride=1):
    """Residual block with skip connection."""
    shortcut = x
    
    # Main path
    x = layers.Conv2D(filters, 3, strides=stride, padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU()(x)
    x = layers.Conv2D(filters, 3, padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    
    # Adjust shortcut dimensions if needed
    if stride != 1 or shortcut.shape[-1] != filters:
        shortcut = layers.Conv2D(filters, 1, strides=stride, use_bias=False)(shortcut)
        shortcut = layers.BatchNormalization()(shortcut)
    
    # Skip connection
    x = layers.Add()([x, shortcut])
    x = layers.ReLU()(x)
    return x

# Build model
inputs = keras.Input(shape=(224, 224, 3))
x = layers.Conv2D(64, 7, strides=2, padding='same', use_bias=False)(inputs)
x = layers.BatchNormalization()(x)
x = layers.ReLU()(x)
x = layers.MaxPooling2D(3, strides=2, padding='same')(x)

# Residual blocks
x = residual_block(x, 64)
x = residual_block(x, 64)
x = residual_block(x, 128, stride=2)
x = residual_block(x, 128)
x = residual_block(x, 256, stride=2)
x = residual_block(x, 256)

x = layers.GlobalAveragePooling2D()(x)
x = layers.Dense(256, activation='relu')(x)
x = layers.Dropout(0.5)(x)
outputs = layers.Dense(1000, activation='softmax')(x)

model = keras.Model(inputs, outputs, name='custom_resnet')
```

### Multi-Input Multi-Output

```python
# Image + metadata → classification + bounding box
image_input = keras.Input(shape=(224, 224, 3), name='image')
metadata_input = keras.Input(shape=(10,), name='metadata')

# Image branch
x = layers.Conv2D(32, 3, activation='relu')(image_input)
x = layers.MaxPooling2D()(x)
x = layers.Conv2D(64, 3, activation='relu')(x)
x = layers.GlobalAveragePooling2D()(x)
x = layers.Dense(128, activation='relu')(x)

# Metadata branch
m = layers.Dense(64, activation='relu')(metadata_input)
m = layers.Dense(32, activation='relu')(m)

# Merge
combined = layers.Concatenate()([x, m])
combined = layers.Dense(128, activation='relu')(combined)

# Two outputs
class_output = layers.Dense(10, activation='softmax', name='classification')(combined)
bbox_output = layers.Dense(4, activation='sigmoid', name='bounding_box')(combined)

model = keras.Model(
    inputs=[image_input, metadata_input],
    outputs=[class_output, bbox_output]
)

model.compile(
    optimizer='adam',
    loss={
        'classification': 'sparse_categorical_crossentropy',
        'bounding_box': 'mse'
    },
    loss_weights={'classification': 1.0, 'bounding_box': 0.5},
    metrics={'classification': 'accuracy'}
)
```

---

## Model Subclassing

Full control over forward pass — required for dynamic architectures.

### Transformer Encoder Block

```python
class MultiHeadSelfAttention(keras.layers.Layer):
    def __init__(self, embed_dim, num_heads, **kwargs):
        super().__init__(**kwargs)
        self.embed_dim = embed_dim
        self.num_heads = num_heads
        self.head_dim = embed_dim // num_heads
        
        self.query = layers.Dense(embed_dim)
        self.key = layers.Dense(embed_dim)
        self.value = layers.Dense(embed_dim)
        self.output_proj = layers.Dense(embed_dim)
    
    def call(self, x, mask=None):
        batch_size = tf.shape(x)[0]
        seq_len = tf.shape(x)[1]
        
        # Project to Q, K, V
        q = self.query(x)  # (batch, seq, embed)
        k = self.key(x)
        v = self.value(x)
        
        # Reshape to (batch, heads, seq, head_dim)
        q = tf.reshape(q, [batch_size, seq_len, self.num_heads, self.head_dim])
        q = tf.transpose(q, [0, 2, 1, 3])
        k = tf.reshape(k, [batch_size, seq_len, self.num_heads, self.head_dim])
        k = tf.transpose(k, [0, 2, 1, 3])
        v = tf.reshape(v, [batch_size, seq_len, self.num_heads, self.head_dim])
        v = tf.transpose(v, [0, 2, 1, 3])
        
        # Scaled dot-product attention
        scores = tf.matmul(q, k, transpose_b=True) / tf.math.sqrt(
            tf.cast(self.head_dim, tf.float32)
        )
        
        if mask is not None:
            scores += (mask * -1e9)
        
        attention_weights = tf.nn.softmax(scores, axis=-1)
        context = tf.matmul(attention_weights, v)
        
        # Reshape back
        context = tf.transpose(context, [0, 2, 1, 3])
        context = tf.reshape(context, [batch_size, seq_len, self.embed_dim])
        
        return self.output_proj(context)


class TransformerBlock(keras.layers.Layer):
    def __init__(self, embed_dim, num_heads, ff_dim, dropout_rate=0.1, **kwargs):
        super().__init__(**kwargs)
        self.attention = MultiHeadSelfAttention(embed_dim, num_heads)
        self.ffn = keras.Sequential([
            layers.Dense(ff_dim, activation='gelu'),
            layers.Dense(embed_dim),
        ])
        self.norm1 = layers.LayerNormalization(epsilon=1e-6)
        self.norm2 = layers.LayerNormalization(epsilon=1e-6)
        self.dropout1 = layers.Dropout(dropout_rate)
        self.dropout2 = layers.Dropout(dropout_rate)
    
    def call(self, x, training=False, mask=None):
        # Pre-norm architecture (more stable training)
        attn_output = self.attention(self.norm1(x), mask=mask)
        attn_output = self.dropout1(attn_output, training=training)
        x = x + attn_output  # Residual connection
        
        ffn_output = self.ffn(self.norm2(x))
        ffn_output = self.dropout2(ffn_output, training=training)
        x = x + ffn_output  # Residual connection
        
        return x


class TransformerEncoder(keras.Model):
    def __init__(self, vocab_size, max_len, embed_dim, num_heads, ff_dim, 
                 num_layers, num_classes, dropout_rate=0.1, **kwargs):
        super().__init__(**kwargs)
        self.token_embedding = layers.Embedding(vocab_size, embed_dim)
        self.position_embedding = layers.Embedding(max_len, embed_dim)
        self.transformer_blocks = [
            TransformerBlock(embed_dim, num_heads, ff_dim, dropout_rate)
            for _ in range(num_layers)
        ]
        self.dropout = layers.Dropout(dropout_rate)
        self.global_pool = layers.GlobalAveragePooling1D()
        self.classifier = layers.Dense(num_classes, activation='softmax')
    
    def call(self, x, training=False):
        seq_len = tf.shape(x)[1]
        positions = tf.range(start=0, limit=seq_len, delta=1)
        
        x = self.token_embedding(x) + self.position_embedding(positions)
        x = self.dropout(x, training=training)
        
        for block in self.transformer_blocks:
            x = block(x, training=training)
        
        x = self.global_pool(x)
        return self.classifier(x)

# Usage
model = TransformerEncoder(
    vocab_size=30000, max_len=512, embed_dim=256,
    num_heads=8, ff_dim=512, num_layers=6, num_classes=5
)
```

---

## Custom Layers

```python
class SpectralNormalization(keras.layers.Layer):
    """Spectral normalization for stable GAN training."""
    
    def __init__(self, layer, power_iterations=1, **kwargs):
        super().__init__(**kwargs)
        self.layer = layer
        self.power_iterations = power_iterations
    
    def build(self, input_shape):
        self.layer.build(input_shape)
        self.u = self.add_weight(
            name='u', shape=(1, self.layer.kernel.shape[-1]),
            initializer='truncated_normal', trainable=False
        )
    
    def call(self, inputs):
        w = self.layer.kernel
        w_shape = w.shape
        w_mat = tf.reshape(w, [-1, w_shape[-1]])
        
        u_hat = self.u
        for _ in range(self.power_iterations):
            v_hat = tf.nn.l2_normalize(tf.matmul(u_hat, tf.transpose(w_mat)))
            u_hat = tf.nn.l2_normalize(tf.matmul(v_hat, w_mat))
        
        sigma = tf.matmul(tf.matmul(v_hat, w_mat), tf.transpose(u_hat))
        self.u.assign(u_hat)
        
        self.layer.kernel.assign(w / sigma)
        return self.layer(inputs)
```

---

## Custom Loss Functions

```python
class FocalLoss(keras.losses.Loss):
    """Focal loss for imbalanced classification."""
    
    def __init__(self, gamma=2.0, alpha=0.25, **kwargs):
        super().__init__(**kwargs)
        self.gamma = gamma
        self.alpha = alpha
    
    def call(self, y_true, y_pred):
        y_pred = tf.clip_by_value(y_pred, 1e-7, 1 - 1e-7)
        
        # Binary cross entropy
        bce = -y_true * tf.math.log(y_pred) - (1 - y_true) * tf.math.log(1 - y_pred)
        
        # Focal weight
        p_t = y_true * y_pred + (1 - y_true) * (1 - y_pred)
        focal_weight = (1 - p_t) ** self.gamma
        
        # Alpha weighting
        alpha_t = y_true * self.alpha + (1 - y_true) * (1 - self.alpha)
        
        return tf.reduce_mean(alpha_t * focal_weight * bce)


class ContrastiveLoss(keras.losses.Loss):
    """Contrastive loss for siamese networks."""
    
    def __init__(self, margin=1.0, **kwargs):
        super().__init__(**kwargs)
        self.margin = margin
    
    def call(self, y_true, y_pred):
        # y_true: 1 if same class, 0 if different
        # y_pred: euclidean distance between embeddings
        square_dist = tf.square(y_pred)
        margin_dist = tf.square(tf.maximum(self.margin - y_pred, 0))
        return tf.reduce_mean(y_true * square_dist + (1 - y_true) * margin_dist)
```

---

## Custom Metrics

```python
class F1Score(keras.metrics.Metric):
    def __init__(self, name='f1_score', **kwargs):
        super().__init__(name=name, **kwargs)
        self.precision = keras.metrics.Precision()
        self.recall = keras.metrics.Recall()
    
    def update_state(self, y_true, y_pred, sample_weight=None):
        self.precision.update_state(y_true, y_pred, sample_weight)
        self.recall.update_state(y_true, y_pred, sample_weight)
    
    def result(self):
        p = self.precision.result()
        r = self.recall.result()
        return 2 * (p * r) / (p + r + keras.backend.epsilon())
    
    def reset_state(self):
        self.precision.reset_state()
        self.recall.reset_state()
```

---

## Next: [Training & Optimization →](03_Training.md)
