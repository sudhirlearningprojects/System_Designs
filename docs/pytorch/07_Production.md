# 7. Production & Deployment

## Model Export

### torch.export (PyTorch 2.x — Recommended)

```python
import torch

# Export model to portable graph
exported = torch.export.export(model, (sample_input,))

# Save
torch.export.save(exported, "model_exported.pt2")

# Load (no Python dependency needed)
loaded = torch.export.load("model_exported.pt2")
result = loaded.module()(input_tensor)
```

### TorchScript (Legacy but Stable)

```python
# Tracing (for models without control flow)
traced = torch.jit.trace(model, example_input)
traced.save("model_traced.pt")

# Scripting (for models with control flow)
scripted = torch.jit.script(model)
scripted.save("model_scripted.pt")

# Load without Python model definition
loaded = torch.jit.load("model_traced.pt")
output = loaded(input_tensor)
```

### ONNX Export

```python
import torch.onnx

torch.onnx.export(
    model,
    sample_input,
    "model.onnx",
    input_names=["input"],
    output_names=["output"],
    dynamic_axes={"input": {0: "batch_size"}, "output": {0: "batch_size"}},
    opset_version=17,
)

# Inference with ONNX Runtime
import onnxruntime as ort

session = ort.InferenceSession("model.onnx", providers=['CUDAExecutionProvider'])
result = session.run(None, {"input": input_numpy})
```

---

## TorchServe

```bash
# Package model
torch-model-archiver --model-name my_model \
  --version 1.0 \
  --serialized-file model.pt \
  --handler handler.py \
  --export-path model_store

# Start server
torchserve --start --model-store model_store --models my_model=my_model.mar

# Inference
curl -X POST http://localhost:8080/predictions/my_model -T input.jpg
```

### Custom Handler

```python
from ts.torch_handler.base_handler import BaseHandler
import torch

class MyHandler(BaseHandler):
    def initialize(self, context):
        self.model = torch.jit.load("model.pt").eval().cuda()
        self.model = torch.compile(self.model)
    
    def preprocess(self, data):
        images = [self.transform(d["body"]) for d in data]
        return torch.stack(images).cuda()
    
    def inference(self, input_batch):
        with torch.no_grad(), torch.cuda.amp.autocast():
            return self.model(input_batch)
    
    def postprocess(self, output):
        probs = torch.softmax(output, dim=1)
        return [{"class": p.argmax().item(), "confidence": p.max().item()} for p in probs]
```

---

## Quantization

### Dynamic Quantization (Easiest)

```python
quantized_model = torch.quantization.quantize_dynamic(
    model, {torch.nn.Linear}, dtype=torch.qint8
)
# 2-4x speedup on CPU, ~1% accuracy loss
```

### Static Quantization (Best Quality)

```python
model.eval()
model.qconfig = torch.quantization.get_default_qconfig('x86')
prepared = torch.quantization.prepare(model)

# Calibrate with representative data
with torch.no_grad():
    for batch in calibration_loader:
        prepared(batch)

quantized = torch.quantization.convert(prepared)
```

### GPTQ (LLM Quantization)

```python
from auto_gptq import AutoGPTQForCausalLM, BaseQuantizeConfig

config = BaseQuantizeConfig(bits=4, group_size=128)
model = AutoGPTQForCausalLM.from_pretrained(model_path, config)
model.quantize(calibration_data)
model.save_quantized("model_4bit")
# 4x memory reduction, <1% perplexity increase
```

---

## torch.compile Optimization

```python
# Default (good balance)
model = torch.compile(model)

# Maximum performance (longer compile, faster runtime)
model = torch.compile(model, mode="max-autotune")

# Reduce overhead (best for small models / inference)
model = torch.compile(model, mode="reduce-overhead")

# Full graph capture (errors if graph breaks)
model = torch.compile(model, fullgraph=True)

# Profile compilation
torch._dynamo.config.log_level = logging.DEBUG
torch._dynamo.config.verbose = True
```

### Common Graph Breaks (and Fixes)

| Issue | Cause | Fix |
|-------|-------|-----|
| Data-dependent control flow | `if tensor.item() > 0` | Use `torch.where` |
| Python side effects | `print()`, `list.append()` | Remove or use `torch._dynamo.allow_in_graph` |
| Unsupported ops | Custom C++ extensions | Register as custom op |
| Dynamic shapes | Variable sequence lengths | Use `dynamic=True` |

---

## Inference Optimization Checklist

```python
# 1. Set eval mode
model.eval()

# 2. Disable gradient computation
with torch.no_grad():
    # or use @torch.inference_mode (stricter, faster)
    with torch.inference_mode():
        output = model(input)

# 3. Use torch.compile
model = torch.compile(model, mode="reduce-overhead")

# 4. Use mixed precision
with torch.cuda.amp.autocast(dtype=torch.bfloat16):
    output = model(input)

# 5. Batch inputs (GPU utilization)
# Process 32 inputs at once instead of 1

# 6. Use CUDA graphs (eliminate kernel launch overhead)
# For fixed-size inputs:
static_input = torch.randn(32, 3, 224, 224, device='cuda')
s = torch.cuda.Stream()
s.wait_stream(torch.cuda.current_stream())
with torch.cuda.stream(s):
    for _ in range(3):  # Warmup
        output = model(static_input)
torch.cuda.current_stream().wait_stream(s)

g = torch.cuda.CUDAGraph()
with torch.cuda.graph(g):
    output = model(static_input)

# Replay (ultra-fast, no Python overhead)
static_input.copy_(real_input)
g.replay()
```

---

## Performance Comparison

| Technique | Speedup | Memory | Accuracy Impact |
|-----------|---------|--------|-----------------|
| torch.compile | 1.5-3x | Same | None |
| BFloat16 | 2x | 0.5x | Negligible |
| Quantization (INT8) | 2-4x (CPU) | 0.25x | <1% |
| CUDA Graphs | 1.5-2x | Same | None |
| Batching (1→32) | 10-20x throughput | Linear | None |
| FlashAttention | 2-4x (attention) | O(N) vs O(N²) | None |
| torch.compile + all above | 5-10x | 0.25-0.5x | <1% |
