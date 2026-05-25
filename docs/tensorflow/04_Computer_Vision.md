# 4. CNNs & Computer Vision

## Convolutional Neural Networks — Theory

### Convolution Operation

```
Input (6×6×1)          Filter (3×3)         Output (4×4)
┌─────────────────┐    ┌─────────┐          ┌───────────┐
│ 1  0  1  0  1  0│    │ 1  0  1 │          │ 4  3  4  1│
│ 0  1  0  1  0  1│    │ 0  1  0 │    →     │ 2  4  3  3│
│ 1  0  1  0  1  0│    │ 1  0  1 │          │ 2  3  4  1│
│ 0  1  0  1  0  1│    └─────────┘          │ 2  2  3  3│
│ 1  0  1  0  1  0│                          └───────────┘
│ 0  1  0  1  0  1│
└─────────────────┘

Output size = (Input - Filter + 2*Padding) / Stride + 1
           = (6 - 3 + 0) / 1 + 1 = 4
```

**Key Concepts:**
- **Filters/Kernels**: Learnable feature detectors (edges, textures, shapes)
- **Stride**: Step size of the sliding window
- **Padding**: `'same'` (output = input size) or `'valid'` (no padding)
- **Receptive Field**: Region of input that influences one output pixel
- **Feature Maps**: Output of applying filters (channels in deeper layers)

---

## Modern CNN Architectures

```python
import tensorflow as tf
from tensorflow import keras
from keras import layers

# EfficientNetV2 (state-of-the-art efficiency)
base_model = keras.applications.EfficientNetV2S(
    include_top=False, weights='imagenet', input_shape=(224, 224, 3)
)

model = keras.Sequential([
    base_model,
    layers.GlobalAveragePooling2D(),
    layers.Dropout(0.3),
    layers.Dense(256, activation='relu'),
    layers.Dense(num_classes, activation='softmax')
])
```

---

## Transfer Learning (Production Pattern)

```python
def build_transfer_model(num_classes, input_shape=(224, 224, 3), fine_tune_layers=30):
    # Pre-trained backbone
    base = keras.applications.EfficientNetV2S(
        include_top=False, weights='imagenet', input_shape=input_shape
    )
    
    # Freeze all layers initially
    base.trainable = False
    
    # Build classifier head
    inputs = keras.Input(shape=input_shape)
    x = base(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dense(512, activation='relu', kernel_regularizer=keras.regularizers.l2(1e-4))(x)
    x = layers.Dropout(0.4)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)
    
    model = keras.Model(inputs, outputs)
    return model, base

model, base = build_transfer_model(num_classes=100)

# Stage 1: Train head only
model.compile(optimizer=keras.optimizers.Adam(1e-3), loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(train_ds, epochs=10, validation_data=val_ds)

# Stage 2: Fine-tune top layers of backbone
base.trainable = True
for layer in base.layers[:-30]:
    layer.trainable = False

model.compile(optimizer=keras.optimizers.Adam(1e-5), loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(train_ds, epochs=20, validation_data=val_ds)
```

---

## Data Augmentation

```python
augmentation = keras.Sequential([
    layers.RandomFlip("horizontal"),
    layers.RandomRotation(0.1),
    layers.RandomZoom(0.1),
    layers.RandomContrast(0.1),
    layers.RandomBrightness(0.1),
    layers.RandomTranslation(0.1, 0.1),
], name="augmentation")

# CutMix / MixUp (advanced augmentation)
def cutmix(images, labels, alpha=1.0):
    batch_size = tf.shape(images)[0]
    lam = tf.random.uniform([], 0, 1)
    
    # Random bounding box
    cut_ratio = tf.sqrt(1.0 - lam)
    h, w = tf.shape(images)[1], tf.shape(images)[2]
    cut_h = tf.cast(tf.cast(h, tf.float32) * cut_ratio, tf.int32)
    cut_w = tf.cast(tf.cast(w, tf.float32) * cut_ratio, tf.int32)
    
    cx = tf.random.uniform([], 0, w, dtype=tf.int32)
    cy = tf.random.uniform([], 0, h, dtype=tf.int32)
    
    # Shuffle batch for mixing
    indices = tf.random.shuffle(tf.range(batch_size))
    shuffled_images = tf.gather(images, indices)
    shuffled_labels = tf.gather(labels, indices)
    
    # Apply cutmix (simplified)
    mixed_images = images  # Apply bounding box replacement
    mixed_labels = lam * tf.cast(labels, tf.float32) + (1 - lam) * tf.cast(shuffled_labels, tf.float32)
    
    return mixed_images, mixed_labels
```

---

## Object Detection (YOLOv8-style)

```python
class DetectionHead(keras.layers.Layer):
    """Detection head for anchor-free object detection."""
    
    def __init__(self, num_classes, num_anchors=1, **kwargs):
        super().__init__(**kwargs)
        self.num_classes = num_classes
        
        # Classification branch
        self.cls_convs = keras.Sequential([
            layers.Conv2D(256, 3, padding='same', activation='relu'),
            layers.Conv2D(256, 3, padding='same', activation='relu'),
            layers.Conv2D(num_classes * num_anchors, 1, activation='sigmoid')
        ])
        
        # Regression branch (bounding box)
        self.reg_convs = keras.Sequential([
            layers.Conv2D(256, 3, padding='same', activation='relu'),
            layers.Conv2D(256, 3, padding='same', activation='relu'),
            layers.Conv2D(4 * num_anchors, 1)  # x, y, w, h
        ])
    
    def call(self, features):
        cls_output = self.cls_convs(features)
        reg_output = self.reg_convs(features)
        return cls_output, reg_output
```

---

## Semantic Segmentation (U-Net)

```python
def unet(input_shape=(256, 256, 3), num_classes=21):
    inputs = keras.Input(shape=input_shape)
    
    # Encoder (downsampling)
    c1 = layers.Conv2D(64, 3, activation='relu', padding='same')(inputs)
    c1 = layers.Conv2D(64, 3, activation='relu', padding='same')(c1)
    p1 = layers.MaxPooling2D()(c1)
    
    c2 = layers.Conv2D(128, 3, activation='relu', padding='same')(p1)
    c2 = layers.Conv2D(128, 3, activation='relu', padding='same')(c2)
    p2 = layers.MaxPooling2D()(c2)
    
    c3 = layers.Conv2D(256, 3, activation='relu', padding='same')(p2)
    c3 = layers.Conv2D(256, 3, activation='relu', padding='same')(c3)
    p3 = layers.MaxPooling2D()(c3)
    
    # Bottleneck
    c4 = layers.Conv2D(512, 3, activation='relu', padding='same')(p3)
    c4 = layers.Conv2D(512, 3, activation='relu', padding='same')(c4)
    
    # Decoder (upsampling with skip connections)
    u3 = layers.UpSampling2D()(c4)
    u3 = layers.Concatenate()([u3, c3])  # Skip connection
    c5 = layers.Conv2D(256, 3, activation='relu', padding='same')(u3)
    c5 = layers.Conv2D(256, 3, activation='relu', padding='same')(c5)
    
    u2 = layers.UpSampling2D()(c5)
    u2 = layers.Concatenate()([u2, c2])
    c6 = layers.Conv2D(128, 3, activation='relu', padding='same')(u2)
    c6 = layers.Conv2D(128, 3, activation='relu', padding='same')(c6)
    
    u1 = layers.UpSampling2D()(c6)
    u1 = layers.Concatenate()([u1, c1])
    c7 = layers.Conv2D(64, 3, activation='relu', padding='same')(u1)
    c7 = layers.Conv2D(64, 3, activation='relu', padding='same')(c7)
    
    outputs = layers.Conv2D(num_classes, 1, activation='softmax')(c7)
    
    return keras.Model(inputs, outputs, name='unet')
```

---

## Next: [NLP & Transformers →](05_NLP_Transformers.md)
