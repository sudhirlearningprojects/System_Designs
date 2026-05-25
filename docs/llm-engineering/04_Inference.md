# 4. Inference & Serving

## Theory: Why LLM Inference is Hard

```
LLM inference is MEMORY-BOUND, not compute-bound.

Problem: Autoregressive generation = one token at a time
  - Each token requires reading ALL model weights from memory
  - For a 70B model: read 140GB from GPU memory per token
  - GPU compute is idle 90%+ of the time (waiting for memory)

Key insight: The bottleneck is MEMORY BANDWIDTH, not FLOPS.
  A100 GPU: 2TB/s bandwidth, 312 TFLOPS
  70B model: 140GB per forward pass
  Max throughput: 2000GB/s ÷ 140GB = ~14 tokens/second (single user)
  
  But A100 has 312 TFLOPS available!
  Actual compute used: <5% of capacity (memory-bound)
```

---

## KV-Cache

### Theory

```
Without KV-cache: For each new token, recompute attention over ALL previous tokens
  Token 1: Compute attention for [t1]
  Token 2: Compute attention for [t1, t2]  ← recomputes t1!
  Token 3: Compute attention for [t1, t2, t3]  ← recomputes t1, t2!
  ...
  Token N: O(N²) total computation

With KV-cache: Cache the Key and Value projections from previous tokens
  Token 1: Compute K1, V1, store in cache
  Token 2: Compute K2, V2, store. Attend to [K1,K2], [V1,V2]
  Token 3: Compute K3, V3, store. Attend to [K1,K2,K3], [V1,V2,V3]
  ...
  Token N: Only compute NEW token's Q,K,V. Reuse cached K,V.
  → O(N) total computation (linear, not quadratic!)

KV-cache size:
  Per token: 2 × num_layers × num_heads × head_dim × dtype_size
  For Llama 70B (80 layers, 64 KV heads, dim 128, fp16):
    Per token: 2 × 80 × 64 × 128 × 2 bytes = 2.6 MB
    For 4K context: 4096 × 2.6 MB = 10.5 GB per sequence!
    For 128K context: 128K × 2.6 MB = 333 GB ← doesn't fit on one GPU!

Solutions:
  - GQA (fewer KV heads): Llama 3 uses 8 KV groups → 8x smaller cache
  - Quantized KV-cache: Store in int8 → 2x smaller
  - Paged attention (vLLM): Virtual memory for KV-cache
```

---

## Quantization

### Theory

```
Reduce numerical precision to save memory and increase speed.

fp32 (32-bit): Full precision, 4 bytes per parameter
fp16 (16-bit): Half precision, 2 bytes (standard for inference)
int8 (8-bit):  Integer quantization, 1 byte (2x speedup on CPU)
int4 (4-bit):  Aggressive quantization, 0.5 bytes (4x memory reduction)

Quality impact:
  fp32 → fp16: ~0% quality loss (standard practice)
  fp16 → int8: <1% quality loss (good for most tasks)
  fp16 → int4: 1-3% quality loss (acceptable for many applications)
  fp16 → int2: 5-15% quality loss (research only)
```

### Quantization Methods

| Method | Type | Quality | Speed | Use Case |
|--------|------|---------|-------|----------|
| **GPTQ** | Weight-only, post-training | Good | Fast (GPU) | Production serving |
| **AWQ** | Activation-aware weight quant | Better | Fast (GPU) | Production serving |
| **GGUF** | CPU-optimized quantization | Good | Fast (CPU) | Local/edge deployment |
| **bitsandbytes** | Dynamic quantization | Good | Medium | Training (QLoRA) |
| **SmoothQuant** | Weight + activation | Best | Medium | High-quality int8 |

---

## Speculative Decoding

### Theory

```
Problem: Large model generates 1 token at a time (slow)
Insight: Small model can DRAFT multiple tokens quickly,
         large model can VERIFY multiple tokens in parallel

Algorithm:
  1. Small model (draft): Generate K tokens quickly (e.g., K=5)
  2. Large model (verify): Check all K tokens in ONE forward pass
  3. Accept tokens that match large model's distribution
  4. Reject and regenerate from first mismatch

Example:
  Draft model generates: "The cat sat on the mat"
  Large model verifies:  "The cat sat on the" ✓✓✓✓✓ "rug" ✗ (rejects "mat")
  Result: Accept 5 tokens, regenerate from position 6
  
  Speedup: 2-3x (generate 5 tokens in time of ~2 forward passes)
  Quality: IDENTICAL to large model (mathematically proven)
```

---

## Batching Strategies

### Continuous Batching (vLLM)

```
Problem: Static batching wastes GPU when sequences finish at different times.

Static batching:
  Batch = [seq1(100 tokens), seq2(500 tokens), seq3(50 tokens)]
  GPU idle after seq3 finishes (waiting for seq2)
  Utilization: ~40%

Continuous batching:
  When seq3 finishes → immediately add seq4 to the batch
  GPU is ALWAYS processing maximum sequences
  Utilization: ~95%

Implementation: vLLM, TensorRT-LLM, TGI
```

### PagedAttention (vLLM)

```
Problem: KV-cache requires contiguous memory allocation.
  If you allocate for max_length (4096), most is wasted for short sequences.

Solution: Virtual memory for KV-cache (like OS paging)
  - Allocate KV-cache in fixed-size "pages" (blocks)
  - Pages can be non-contiguous in physical memory
  - Only allocate pages as needed (no waste)
  - Share pages across sequences (for beam search, parallel sampling)

Result: 2-4x more sequences fit in GPU memory → higher throughput
```

---

## Serving Frameworks

| Framework | Best For | Key Feature |
|-----------|----------|-------------|
| **vLLM** | High-throughput serving | PagedAttention, continuous batching |
| **TGI** (HuggingFace) | Easy deployment | Docker-ready, HF model hub |
| **TensorRT-LLM** (NVIDIA) | Maximum GPU performance | Kernel fusion, FP8 |
| **Ollama** | Local development | One-command setup |
| **llama.cpp** | CPU/edge inference | GGUF format, Apple Silicon |

### vLLM Example

```python
from vllm import LLM, SamplingParams

# Load model with optimizations
llm = LLM(
    model="meta-llama/Meta-Llama-3-8B-Instruct",
    tensor_parallel_size=2,      # Split across 2 GPUs
    dtype="bfloat16",
    max_model_len=8192,
    gpu_memory_utilization=0.9,  # Use 90% of GPU memory
)

# Batch inference
prompts = ["Explain quantum computing", "Write a haiku", "Solve: 2x+5=13"]
params = SamplingParams(temperature=0.7, top_p=0.9, max_tokens=512)

outputs = llm.generate(prompts, params)
for output in outputs:
    print(output.outputs[0].text)
```

---

## Inference Optimization Summary

| Technique | Speedup | Memory Savings | Quality Impact |
|-----------|---------|----------------|----------------|
| KV-Cache | 10-100x | Increases memory | None |
| Quantization (int4) | 2-4x | 4x reduction | 1-3% loss |
| Continuous batching | 2-5x throughput | None | None |
| PagedAttention | 2-4x throughput | 2-4x reduction | None |
| Speculative decoding | 2-3x | Slight increase | None |
| Flash Attention | 2-4x (attention) | O(N) vs O(N²) | None |
| Tensor parallelism | Linear with GPUs | Split across GPUs | None |
| GQA | 4-8x KV-cache | 4-8x KV reduction | <1% loss |

---

## Implementation: Serving with vLLM

```python
# ============ vLLM: High-Throughput Serving ============
from vllm import LLM, SamplingParams

# Load model with optimizations
llm = LLM(
    model="meta-llama/Meta-Llama-3-8B-Instruct",
    dtype="bfloat16",
    tensor_parallel_size=2,          # Split across 2 GPUs
    max_model_len=8192,
    gpu_memory_utilization=0.9,
    enable_prefix_caching=True,      # Cache common prefixes
    quantization="awq",              # Use AWQ quantized model
)

# Batch inference
prompts = [
    "Explain Docker containers in simple terms.",
    "Write a Python function to sort a list.",
    "What is the capital of France?",
]

params = SamplingParams(
    temperature=0.7,
    top_p=0.9,
    max_tokens=512,
    stop=["\n\n"],  # Stop sequences
)

outputs = llm.generate(prompts, params)
for output in outputs:
    print(f"Prompt: {output.prompt[:50]}...")
    print(f"Output: {output.outputs[0].text[:100]}...")
    print(f"Tokens/sec: {len(output.outputs[0].token_ids) / output.metrics.finished_time:.1f}")
    print()
```

### vLLM as OpenAI-Compatible Server

```bash
# Start server (drop-in replacement for OpenAI API)
python -m vllm.entrypoints.openai.api_server \
    --model meta-llama/Meta-Llama-3-8B-Instruct \
    --dtype bfloat16 \
    --tensor-parallel-size 2 \
    --max-model-len 8192 \
    --port 8000

# Client (works with any OpenAI SDK)
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Meta-Llama-3-8B-Instruct",
    "messages": [{"role": "user", "content": "Hello!"}],
    "temperature": 0.7,
    "max_tokens": 100
  }'
```

### Quantization for Deployment

```python
# ============ GPTQ Quantization (4-bit, GPU) ============
from auto_gptq import AutoGPTQForCausalLM, BaseQuantizeConfig

# Quantize model
quantize_config = BaseQuantizeConfig(
    bits=4,
    group_size=128,
    damp_percent=0.1,
    desc_act=True,
)

model = AutoGPTQForCausalLM.from_pretrained(
    "./merged_model", quantize_config
)

# Calibration data (representative inputs)
calibration_data = [tokenizer(text, return_tensors="pt") for text in calibration_texts[:128]]
model.quantize(calibration_data)
model.save_quantized("./model_4bit_gptq")

# ============ GGUF Quantization (CPU/Apple Silicon) ============
# Using llama.cpp
# python convert_hf_to_gguf.py ./merged_model --outtype f16
# ./quantize ./merged_model.gguf ./model_q4_k_m.gguf Q4_K_M

# Serve with Ollama
# ollama create mymodel -f Modelfile
# ollama run mymodel "Hello!"
```

### Production Serving with Docker

```dockerfile
# Dockerfile for vLLM serving
FROM vllm/vllm-openai:latest

# Pre-download model
RUN python -c "from huggingface_hub import snapshot_download; \
    snapshot_download('meta-llama/Meta-Llama-3-8B-Instruct', local_dir='/models/llama3')"

ENV MODEL_NAME=/models/llama3
EXPOSE 8000

CMD ["python", "-m", "vllm.entrypoints.openai.api_server", \
     "--model", "/models/llama3", \
     "--dtype", "bfloat16", \
     "--max-model-len", "8192", \
     "--port", "8000"]
```

```yaml
# docker-compose.yml
services:
  llm:
    build: .
    ports:
      - "8000:8000"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 2
              capabilities: [gpu]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

---

## Next: [Prompt Engineering →](05_Prompting.md)
