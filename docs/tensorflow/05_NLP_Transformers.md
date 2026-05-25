# 5. NLP & Transformers

## Attention Mechanism — Theory

### Scaled Dot-Product Attention

```
Attention(Q, K, V) = softmax(QK^T / √d_k) × V

Where:
- Q (Query): What am I looking for?
- K (Key): What do I contain?
- V (Value): What information do I provide?
- d_k: Dimension of keys (scaling factor prevents softmax saturation)
```

```python
def scaled_dot_product_attention(q, k, v, mask=None):
    """Core attention computation."""
    d_k = tf.cast(tf.shape(k)[-1], tf.float32)
    scores = tf.matmul(q, k, transpose_b=True) / tf.math.sqrt(d_k)
    
    if mask is not None:
        scores += (mask * -1e9)  # Mask future tokens (causal)
    
    attention_weights = tf.nn.softmax(scores, axis=-1)
    output = tf.matmul(attention_weights, v)
    return output, attention_weights
```

### Multi-Head Attention

```python
class MultiHeadAttention(keras.layers.Layer):
    def __init__(self, d_model, num_heads, **kwargs):
        super().__init__(**kwargs)
        self.num_heads = num_heads
        self.d_model = d_model
        self.depth = d_model // num_heads
        
        self.wq = layers.Dense(d_model)
        self.wk = layers.Dense(d_model)
        self.wv = layers.Dense(d_model)
        self.dense = layers.Dense(d_model)
    
    def split_heads(self, x, batch_size):
        x = tf.reshape(x, (batch_size, -1, self.num_heads, self.depth))
        return tf.transpose(x, perm=[0, 2, 1, 3])
    
    def call(self, q, k, v, mask=None):
        batch_size = tf.shape(q)[0]
        
        q = self.split_heads(self.wq(q), batch_size)
        k = self.split_heads(self.wk(k), batch_size)
        v = self.split_heads(self.wv(v), batch_size)
        
        attention_output, weights = scaled_dot_product_attention(q, k, v, mask)
        
        attention_output = tf.transpose(attention_output, perm=[0, 2, 1, 3])
        concat = tf.reshape(attention_output, (batch_size, -1, self.d_model))
        
        return self.dense(concat)
```

---

## Full Transformer (Encoder-Decoder) from Scratch

```python
class PositionalEncoding(keras.layers.Layer):
    """Sinusoidal positional encoding."""
    
    def __init__(self, max_len, d_model, **kwargs):
        super().__init__(**kwargs)
        positions = np.arange(max_len)[:, np.newaxis]
        dims = np.arange(d_model)[np.newaxis, :]
        
        angles = positions / np.power(10000, (2 * (dims // 2)) / d_model)
        angles[:, 0::2] = np.sin(angles[:, 0::2])
        angles[:, 1::2] = np.cos(angles[:, 1::2])
        
        self.pos_encoding = tf.constant(angles[np.newaxis, :, :], dtype=tf.float32)
    
    def call(self, x):
        seq_len = tf.shape(x)[1]
        return x + self.pos_encoding[:, :seq_len, :]


class TransformerEncoderLayer(keras.layers.Layer):
    def __init__(self, d_model, num_heads, dff, dropout_rate=0.1, **kwargs):
        super().__init__(**kwargs)
        self.mha = MultiHeadAttention(d_model, num_heads)
        self.ffn = keras.Sequential([
            layers.Dense(dff, activation='gelu'),
            layers.Dense(d_model)
        ])
        self.norm1 = layers.LayerNormalization(epsilon=1e-6)
        self.norm2 = layers.LayerNormalization(epsilon=1e-6)
        self.dropout1 = layers.Dropout(dropout_rate)
        self.dropout2 = layers.Dropout(dropout_rate)
    
    def call(self, x, mask=None, training=False):
        attn = self.mha(x, x, x, mask)
        attn = self.dropout1(attn, training=training)
        x = self.norm1(x + attn)
        
        ffn = self.ffn(x)
        ffn = self.dropout2(ffn, training=training)
        x = self.norm2(x + ffn)
        return x


class TransformerDecoderLayer(keras.layers.Layer):
    def __init__(self, d_model, num_heads, dff, dropout_rate=0.1, **kwargs):
        super().__init__(**kwargs)
        self.masked_mha = MultiHeadAttention(d_model, num_heads)
        self.cross_mha = MultiHeadAttention(d_model, num_heads)
        self.ffn = keras.Sequential([
            layers.Dense(dff, activation='gelu'),
            layers.Dense(d_model)
        ])
        self.norm1 = layers.LayerNormalization(epsilon=1e-6)
        self.norm2 = layers.LayerNormalization(epsilon=1e-6)
        self.norm3 = layers.LayerNormalization(epsilon=1e-6)
        self.dropout1 = layers.Dropout(dropout_rate)
        self.dropout2 = layers.Dropout(dropout_rate)
        self.dropout3 = layers.Dropout(dropout_rate)
    
    def call(self, x, encoder_output, look_ahead_mask=None, padding_mask=None, training=False):
        # Masked self-attention (causal)
        attn1 = self.masked_mha(x, x, x, look_ahead_mask)
        attn1 = self.dropout1(attn1, training=training)
        x = self.norm1(x + attn1)
        
        # Cross-attention (attend to encoder output)
        attn2 = self.cross_mha(x, encoder_output, encoder_output, padding_mask)
        attn2 = self.dropout2(attn2, training=training)
        x = self.norm2(x + attn2)
        
        # Feed-forward
        ffn = self.ffn(x)
        ffn = self.dropout3(ffn, training=training)
        x = self.norm3(x + ffn)
        return x


class Transformer(keras.Model):
    """Full encoder-decoder Transformer for sequence-to-sequence tasks."""
    
    def __init__(self, num_layers, d_model, num_heads, dff,
                 input_vocab_size, target_vocab_size, max_len, dropout_rate=0.1):
        super().__init__()
        self.d_model = d_model
        
        # Encoder
        self.encoder_embedding = layers.Embedding(input_vocab_size, d_model)
        self.encoder_pos = PositionalEncoding(max_len, d_model)
        self.encoder_layers = [
            TransformerEncoderLayer(d_model, num_heads, dff, dropout_rate)
            for _ in range(num_layers)
        ]
        
        # Decoder
        self.decoder_embedding = layers.Embedding(target_vocab_size, d_model)
        self.decoder_pos = PositionalEncoding(max_len, d_model)
        self.decoder_layers = [
            TransformerDecoderLayer(d_model, num_heads, dff, dropout_rate)
            for _ in range(num_layers)
        ]
        
        self.final_layer = layers.Dense(target_vocab_size)
        self.dropout = layers.Dropout(dropout_rate)
    
    def call(self, inputs, training=False):
        inp, tar = inputs
        
        # Create masks
        look_ahead_mask = self._create_look_ahead_mask(tf.shape(tar)[1])
        
        # Encode
        enc = self.encoder_embedding(inp) * tf.math.sqrt(tf.cast(self.d_model, tf.float32))
        enc = self.encoder_pos(enc)
        enc = self.dropout(enc, training=training)
        for layer in self.encoder_layers:
            enc = layer(enc, training=training)
        
        # Decode
        dec = self.decoder_embedding(tar) * tf.math.sqrt(tf.cast(self.d_model, tf.float32))
        dec = self.decoder_pos(dec)
        dec = self.dropout(dec, training=training)
        for layer in self.decoder_layers:
            dec = layer(dec, enc, look_ahead_mask, training=training)
        
        return self.final_layer(dec)
    
    def _create_look_ahead_mask(self, size):
        mask = 1 - tf.linalg.band_part(tf.ones((size, size)), -1, 0)
        return mask

# Usage: Machine Translation
transformer = Transformer(
    num_layers=6, d_model=512, num_heads=8, dff=2048,
    input_vocab_size=30000, target_vocab_size=30000, max_len=512
)
```

---

## Text Classification with Pre-trained Models

```python
import tensorflow_hub as hub

# Using Universal Sentence Encoder
embed = hub.load("https://tfhub.dev/google/universal-sentence-encoder/4")

class TextClassifier(keras.Model):
    def __init__(self, num_classes):
        super().__init__()
        self.encoder = hub.KerasLayer(
            "https://tfhub.dev/google/universal-sentence-encoder/4",
            trainable=False
        )
        self.classifier = keras.Sequential([
            layers.Dense(256, activation='relu'),
            layers.Dropout(0.3),
            layers.Dense(num_classes, activation='softmax')
        ])
    
    def call(self, texts, training=False):
        embeddings = self.encoder(texts)
        return self.classifier(embeddings, training=training)
```

---

## Next: [Advanced Architectures →](06_Advanced_Architectures.md)
