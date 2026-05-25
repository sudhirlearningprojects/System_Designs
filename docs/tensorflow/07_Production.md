# 7. Production & Deployment

## SavedModel Export

```python
# Standard export
model.save('models/my_model')  # SavedModel format (directory)

# With custom serving signatures
@tf.function(input_signature=[tf.TensorSpec(shape=[None, 224, 224, 3], dtype=tf.float32)])
def predict(images):
    predictions = model(images, training=False)
    return {"class_id": tf.argmax(predictions, axis=1), "probabilities": predictions}

tf.saved_model.save(model, 'models/serving/1', signatures={"serving_default": predict})

# Load and verify
loaded = tf.saved_model.load('models/serving/1')
result = loaded.signatures["serving_default"](tf.random.normal([1, 224, 224, 3]))
```

---

## TensorFlow Serving

```bash
# Docker deployment
docker run -p 8501:8501 -p 8500:8500 \
  --mount type=bind,source=$(pwd)/models/serving,target=/models/my_model \
  -e MODEL_NAME=my_model \
  -e TF_CPP_MIN_LOG_LEVEL=1 \
  tensorflow/serving:latest-gpu \
  --enable_batching=true \
  --batching_parameters_file=/models/batching.config
```

```python
# REST client
import requests
import numpy as np

def predict_rest(images: np.ndarray) -> dict:
    payload = {"instances": images.tolist()}
    response = requests.post("http://localhost:8501/v1/models/my_model:predict", json=payload)
    return response.json()["predictions"]

# gRPC client (faster, binary protocol)
import grpc
from tensorflow_serving.apis import predict_pb2, prediction_service_pb2_grpc

channel = grpc.insecure_channel('localhost:8500')
stub = prediction_service_pb2_grpc.PredictionServiceStub(channel)

request = predict_pb2.PredictRequest()
request.model_spec.name = 'my_model'
request.model_spec.signature_name = 'serving_default'
request.inputs['images'].CopyFrom(tf.make_tensor_proto(images, shape=images.shape))

response = stub.Predict(request, timeout=10.0)
```

### Batching Configuration

```
# batching.config
max_batch_size { value: 64 }
batch_timeout_micros { value: 10000 }  # 10ms
num_batch_threads { value: 4 }
max_enqueued_batches { value: 100 }
```

---

## TFLite (Mobile/Edge)

```python
# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_saved_model('models/serving/1')

# Optimization options
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # Dynamic range quantization

# Full integer quantization (smallest, fastest)
def representative_dataset():
    for i in range(100):
        yield [np.random.randn(1, 224, 224, 3).astype(np.float32)]

converter.representative_dataset = representative_dataset
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8
converter.inference_output_type = tf.uint8

tflite_model = converter.convert()

# Save
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)

# Inference
interpreter = tf.lite.Interpreter(model_path='model.tflite')
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

interpreter.set_tensor(input_details[0]['index'], input_data)
interpreter.invoke()
output = interpreter.get_tensor(output_details[0]['index'])
```

---

## Model Optimization

### Pruning (Remove Unnecessary Weights)

```python
import tensorflow_model_optimization as tfmot

# Apply pruning
pruning_schedule = tfmot.sparsity.keras.PolynomialDecay(
    initial_sparsity=0.0, final_sparsity=0.8,
    begin_step=1000, end_step=5000
)

pruned_model = tfmot.sparsity.keras.prune_low_magnitude(model, pruning_schedule=pruning_schedule)
pruned_model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])

callbacks = [tfmot.sparsity.keras.UpdatePruningStep()]
pruned_model.fit(train_dataset, epochs=10, callbacks=callbacks)

# Strip pruning wrappers for export
final_model = tfmot.sparsity.keras.strip_pruning(pruned_model)
```

### Quantization-Aware Training

```python
quantize_model = tfmot.quantization.keras.quantize_model(model)
quantize_model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
quantize_model.fit(train_dataset, epochs=5)

# Convert to TFLite (will be fully quantized)
converter = tf.lite.TFLiteConverter.from_keras_model(quantize_model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()
```

### Knowledge Distillation

```python
class Distiller(keras.Model):
    def __init__(self, teacher, student, temperature=3.0, alpha=0.5):
        super().__init__()
        self.teacher = teacher
        self.student = student
        self.temperature = temperature
        self.alpha = alpha
    
    def train_step(self, data):
        x, y = data
        
        # Teacher predictions (no gradient)
        teacher_logits = self.teacher(x, training=False)
        
        with tf.GradientTape() as tape:
            student_logits = self.student(x, training=True)
            
            # Hard label loss
            hard_loss = keras.losses.sparse_categorical_crossentropy(y, student_logits, from_logits=True)
            
            # Soft label loss (knowledge distillation)
            soft_teacher = tf.nn.softmax(teacher_logits / self.temperature)
            soft_student = tf.nn.log_softmax(student_logits / self.temperature)
            soft_loss = -tf.reduce_mean(tf.reduce_sum(soft_teacher * soft_student, axis=1))
            soft_loss *= self.temperature ** 2
            
            # Combined loss
            loss = self.alpha * soft_loss + (1 - self.alpha) * tf.reduce_mean(hard_loss)
        
        gradients = tape.gradient(loss, self.student.trainable_variables)
        self.optimizer.apply_gradients(zip(gradients, self.student.trainable_variables))
        return {"loss": loss}
```

---

## Monitoring in Production

```python
# TensorBoard for training
tensorboard_callback = keras.callbacks.TensorBoard(
    log_dir='logs',
    histogram_freq=1,
    write_graph=True,
    write_images=True,
    profile_batch='10,20'  # Profile batches 10-20
)

# Custom metrics logging
summary_writer = tf.summary.create_file_writer('logs/custom')

with summary_writer.as_default():
    tf.summary.scalar('custom/inference_latency', latency, step=global_step)
    tf.summary.scalar('custom/prediction_confidence', confidence, step=global_step)
    tf.summary.histogram('custom/prediction_distribution', predictions, step=global_step)
    tf.summary.image('custom/attention_map', attention_maps, step=global_step)
```

### Profiling

```python
# Profile model performance
tf.profiler.experimental.start('logs/profiler')
model.predict(sample_batch)
tf.profiler.experimental.stop()

# Or use callback
model.fit(train_dataset, epochs=1, 
          callbacks=[keras.callbacks.TensorBoard(profile_batch='5,15')])
# View in TensorBoard: tensorboard --logdir logs
```

---

## Model Versioning & A/B Testing

```
models/
├── production/
│   ├── 1/          # Version 1 (current stable)
│   │   └── saved_model.pb
│   ├── 2/          # Version 2 (canary)
│   │   └── saved_model.pb
│   └── model.config
```

```protobuf
# model.config for A/B testing
model_config_list {
  config {
    name: "my_model"
    base_path: "/models/production"
    model_platform: "tensorflow"
    model_version_policy {
      specific { versions: 1  versions: 2 }
    }
    version_labels {
      key: "stable"
      value: 1
    }
    version_labels {
      key: "canary"
      value: 2
    }
  }
}
```

```python
# Route 10% traffic to canary
import random

def get_model_version():
    return "canary" if random.random() < 0.1 else "stable"

# Request specific version
response = requests.post(
    f"http://localhost:8501/v1/models/my_model/versions/{version}:predict",
    json=payload
)
```
