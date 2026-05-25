# 1. LLM Theory & Architecture

## The Transformer — Foundation of All Modern LLMs

### Historical Context

```
Timeline:
2017: "Attention Is All You Need" (Vaswani et al.) — Transformer invented
2018: GPT-1 (117M params) — Decoder-only, generative pre-training
2018: BERT (340M params) — Encoder-only, bidirectional
2019: GPT-2 (1.5B params) — Scaling works!
2020: GPT-3 (175B params) — In-context learning emerges
2022: ChatGPT — RLHF makes models conversational
2023: GPT-4, Claude, Llama 2 — Multimodal, open-source race
2024: Claude 3.5, GPT-4o, Llama 3, Gemini — Efficiency + capability
2025: Claude 4, reasoning models (o1), agents — Agentic AI
```

### Why Transformers Won

| Architecture | Problem | Transformer Solution |
|-------------|---------|---------------------|
| RNN/LSTM | Sequential (can't parallelize) | Fully parallel attention |
| RNN/LSTM | Vanishing gradients (forgets long context) | Direct attention to any position |
| CNN | Fixed receptive field | Global attention (any token → any token) |
| All prior | Hard to scale | Scales predictably with compute |

---

## Transformer Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    TRANSFORMER (Decoder-Only)                 │
│                    (GPT, Claude, Llama style)                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Input: "The cat sat on the"                                │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  TOKEN EMBEDDING + POSITIONAL ENCODING               │   │
│  │  "The" → [0.1, -0.3, ...] + position_encoding       │   │
│  └──────────────────────────────────────────────────────┘   │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  TRANSFORMER BLOCK × N (e.g., 32 layers)             │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │  Layer Norm                                     │  │   │
│  │  │  ↓                                             │  │   │
│  │  │  MASKED MULTI-HEAD SELF-ATTENTION              │  │   │
│  │  │  (each token attends to previous tokens only)  │  │   │
│  │  │  ↓ + Residual Connection                       │  │   │
│  │  │  Layer Norm                                     │  │   │
│  │  │  ↓                                             │  │   │
│  │  │  FEED-FORWARD NETWORK (MLP)                    │  │   │
│  │  │  (expand → activate → contract)               │  │   │
│  │  │  ↓ + Residual Connection                       │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  OUTPUT HEAD (Linear → Softmax over vocabulary)      │   │
│  │  → P("mat") = 0.35, P("rug") = 0.20, ...           │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  Output: Next token prediction → "mat" (highest probability)│
└─────────────────────────────────────────────────────────────┘
```

---

## Self-Attention — The Core Mechanism

### Intuition

Attention answers: "When processing token X, how much should I focus on each other token?"

```
Sentence: "The animal didn't cross the street because it was too tired"

When processing "it":
  Attention to "animal" = HIGH (0.7)  ← "it" refers to "animal"
  Attention to "street" = LOW (0.1)
  Attention to "tired" = MEDIUM (0.3)

This is LEARNED — the model discovers these relationships during training.
```

### Mathematics

```
Attention(Q, K, V) = softmax(QK^T / √d_k) × V

Where:
  Q = X × W_Q  (Query: "What am I looking for?")
  K = X × W_K  (Key: "What do I contain?")
  V = X × W_V  (Value: "What information do I provide?")
  d_k = dimension of keys (scaling prevents softmax saturation)

Step by step:
1. Compute similarity: QK^T → (seq_len × seq_len) matrix
2. Scale: ÷ √d_k → prevents gradients from vanishing in softmax
3. Mask: Set future positions to -∞ (causal/autoregressive)
4. Normalize: softmax → attention weights (sum to 1 per row)
5. Aggregate: Multiply weights × Values → weighted combination
```

### Multi-Head Attention

```
Instead of one attention function, use H parallel "heads":

head_i = Attention(Q × W_Q_i, K × W_K_i, V × W_V_i)

MultiHead = Concat(head_1, ..., head_H) × W_O

Why multiple heads?
- Each head can attend to different types of relationships
- Head 1: syntactic (subject-verb agreement)
- Head 2: semantic (coreference resolution)
- Head 3: positional (nearby tokens)
- Head 4: long-range dependencies

Typical: 32-128 heads, each with dimension d_model/num_heads
```

### Causal Masking (Decoder-Only)

```
For autoregressive generation, each token can ONLY attend to
previous tokens (not future ones):

Attention mask:
     The  cat  sat  on  the
The  [1    0    0    0    0 ]
cat  [1    1    0    0    0 ]
sat  [1    1    1    0    0 ]
on   [1    1    1    1    0 ]
the  [1    1    1    1    1 ]

0 = masked (set to -∞ before softmax → attention weight = 0)
1 = visible (can attend)

This ensures the model can't "cheat" by looking at the answer.
```

---

## Feed-Forward Network (MLP)

```
FFN(x) = Activation(x × W_1 + b_1) × W_2 + b_2

Typical dimensions:
  Input: d_model (e.g., 4096)
  Hidden: 4 × d_model (e.g., 16384) — "expansion"
  Output: d_model (e.g., 4096) — "contraction"

Modern variants:
  - SwiGLU: FFN(x) = (x × W_1 ⊙ Swish(x × W_gate)) × W_2
    (Used in Llama, Mistral — better than ReLU)
  
  - MoE (Mixture of Experts): Route to top-K of N expert FFNs
    (Used in Mixtral, DeepSeek — more params, same compute)
```

### Why FFN Matters

The FFN is where **knowledge is stored**. Attention finds relevant context; FFN transforms it using learned knowledge.

```
Attention: "Which tokens are relevant to this position?"
FFN: "Given this context, what facts/patterns apply?"

Analogy:
  Attention = Looking up relevant pages in a book
  FFN = Understanding and applying what's on those pages
```

---

## Positional Encoding

Transformers have no inherent notion of position (unlike RNNs). Position must be explicitly encoded.

### Rotary Position Embeddings (RoPE) — Modern Standard

```
Used by: Llama, Mistral, Qwen, most modern LLMs

Idea: Encode position as a ROTATION in embedding space.
  - Token at position i is rotated by angle i × θ
  - Relative position (i-j) is captured by rotation difference
  - Naturally extends to longer sequences than training length

Benefits:
  - Captures relative positions (not just absolute)
  - Decays with distance (nearby tokens have stronger signal)
  - Extrapolates to longer sequences (with NTK-aware scaling)
```

### Context Length Extension

```
Problem: Model trained on 4K context, need 128K at inference.

Solutions:
1. RoPE scaling (NTK-aware): Adjust rotation frequencies
2. YaRN: Yet another RoPE extension (interpolation + extrapolation)
3. ALiBi: Attention with Linear Biases (no position embedding, just bias)
4. Sliding window attention: Only attend to last W tokens per layer
   (Used in Mistral: window=4096, but stacked layers give effective 128K)
```

---

## Training Objective

### Next-Token Prediction (Causal Language Modeling)

```
The ONLY training objective for decoder-only LLMs:

Given tokens [t_1, t_2, ..., t_n], predict t_{n+1}

Loss = -Σ log P(t_i | t_1, ..., t_{i-1})
     = Cross-entropy between predicted distribution and actual next token

Example:
  Input:  "The cat sat on the"
  Target: "mat"
  
  Model predicts: P("mat") = 0.35, P("rug") = 0.20, P("dog") = 0.05, ...
  Loss = -log(0.35) = 1.05

This simple objective, at scale, produces:
  - Grammar understanding
  - World knowledge
  - Reasoning ability
  - Code generation
  - Translation
  - Summarization
  - ... all emergent from next-token prediction!
```

### Why Next-Token Prediction Works

```
To predict the next token well, the model must:
  1. Understand syntax (grammar, structure)
  2. Understand semantics (meaning, context)
  3. Have world knowledge (facts, relationships)
  4. Reason (logic, causality, math)
  5. Follow instructions (if trained on instruction data)

The training signal is DENSE — every token provides a learning signal.
Compare to classification: one label per entire document.
```

---

## Scaling Laws

### Chinchilla Scaling Laws (2022)

```
Key finding: For a fixed compute budget C, optimal allocation is:

  N (parameters) ∝ C^0.5
  D (data tokens) ∝ C^0.5

  → Parameters and data should scale EQUALLY

Chinchilla optimal ratios:
  N parameters → need ~20N training tokens

  1B params → 20B tokens
  7B params → 140B tokens
  70B params → 1.4T tokens
  400B params → 8T tokens

Implication: Most early LLMs were UNDERTRAINED (too many params, too little data)
  GPT-3 (175B) trained on 300B tokens → should have been 3.5T tokens
  Llama 2 (70B) trained on 2T tokens → close to optimal
```

### Emergent Abilities

```
Some capabilities only appear above certain scale thresholds:

  < 1B params: Basic text completion, simple patterns
  1-10B params: Instruction following, basic reasoning
  10-100B params: Complex reasoning, code generation, few-shot learning
  > 100B params: Advanced math, multi-step planning, self-correction

These are "emergent" — they appear suddenly, not gradually.
(Though recent work suggests this may be a measurement artifact)
```

---

## Modern Architecture Variants

| Feature | GPT-4 | Claude | Llama 3 | Mistral | DeepSeek V3 |
|---------|-------|--------|---------|---------|-------------|
| Attention | MHA | MHA | GQA | GQA + Sliding | MHA + MoE |
| Position | Unknown | Unknown | RoPE | RoPE | RoPE |
| FFN | Unknown | Unknown | SwiGLU | SwiGLU | SwiGLU + MoE |
| Norm | Unknown | Unknown | RMSNorm (pre) | RMSNorm (pre) | RMSNorm (pre) |
| Context | 128K | 200K | 128K | 128K | 128K |
| Vocab | ~100K | ~100K | 128K | 32K | 128K |

### Grouped-Query Attention (GQA)

```
Standard MHA: Each head has its own Q, K, V projections
  → 32 heads = 32 Q + 32 K + 32 V matrices
  → Large KV-cache during inference

GQA: Share K, V across groups of heads
  → 32 Q heads, but only 8 K/V groups (4 Q heads share 1 K/V)
  → 4x smaller KV-cache = faster inference, longer context

Used by: Llama 3, Mistral, Gemma
```

### Mixture of Experts (MoE)

```
Standard FFN: Every token goes through the same FFN
MoE: Route each token to top-K of N expert FFNs

Example (Mixtral 8x7B):
  - 8 expert FFNs, each ~7B parameters
  - Router selects top-2 experts per token
  - Total params: ~47B, but only ~13B active per token
  - Result: Quality of 47B model, speed of 13B model

Benefits: More knowledge capacity without proportional compute increase
Challenges: Load balancing, training stability, communication overhead
```

---

## Key Hyperparameters

| Parameter | Typical Range | Effect |
|-----------|--------------|--------|
| d_model (hidden dim) | 2048-8192 | Model capacity |
| n_layers (depth) | 24-80 | Reasoning depth |
| n_heads | 32-128 | Attention diversity |
| d_ff (FFN hidden) | 4×d_model | Knowledge storage |
| vocab_size | 32K-128K | Token granularity |
| context_length | 4K-2M | How much text model can process |
| batch_size | 1M-4M tokens | Training stability |
| learning_rate | 1e-4 to 3e-4 | Training speed vs stability |

### Parameter Count Formula

```
For a standard Transformer with L layers, d dimensions, V vocab:

Embedding: V × d
Per layer: 4d² (attention) + 8d² (FFN with 4x expansion) = 12d²
Output head: d × V (often tied with embedding)

Total ≈ 2Vd + 12Ld²

Example (Llama 3 8B):
  d=4096, L=32, V=128000
  ≈ 2(128000)(4096) + 12(32)(4096²)
  ≈ 1.05B + 6.44B ≈ 7.5B ✓
```

---

## Implementation: Transformer from Scratch (PyTorch)

```python
import torch
import torch.nn as nn
import torch.nn.functional as F
import math

class RMSNorm(nn.Module):
    """Root Mean Square Layer Normalization (used in Llama, Mistral)."""
    def __init__(self, dim, eps=1e-6):
        super().__init__()
        self.eps = eps
        self.weight = nn.Parameter(torch.ones(dim))
    
    def forward(self, x):
        norm = torch.rsqrt(x.pow(2).mean(-1, keepdim=True) + self.eps)
        return x * norm * self.weight


class RotaryPositionalEmbedding(nn.Module):
    """RoPE — Rotary Position Embeddings (Llama, Mistral, Qwen)."""
    def __init__(self, dim, max_seq_len=8192, base=10000):
        super().__init__()
        inv_freq = 1.0 / (base ** (torch.arange(0, dim, 2).float() / dim))
        self.register_buffer('inv_freq', inv_freq)
        self._build_cache(max_seq_len)
    
    def _build_cache(self, seq_len):
        t = torch.arange(seq_len)
        freqs = torch.outer(t, self.inv_freq)
        emb = torch.cat((freqs, freqs), dim=-1)
        self.register_buffer('cos_cached', emb.cos())
        self.register_buffer('sin_cached', emb.sin())
    
    def forward(self, x, seq_len):
        return self.cos_cached[:seq_len], self.sin_cached[:seq_len]


def rotate_half(x):
    x1, x2 = x.chunk(2, dim=-1)
    return torch.cat((-x2, x1), dim=-1)

def apply_rotary_pos_emb(q, k, cos, sin):
    q_embed = (q * cos) + (rotate_half(q) * sin)
    k_embed = (k * cos) + (rotate_half(k) * sin)
    return q_embed, k_embed


class GroupedQueryAttention(nn.Module):
    """GQA — Grouped Query Attention (Llama 3, Mistral).
    Shares K/V heads across groups of Q heads for efficiency."""
    
    def __init__(self, dim, n_heads, n_kv_heads, max_seq_len=8192):
        super().__init__()
        self.n_heads = n_heads
        self.n_kv_heads = n_kv_heads
        self.n_groups = n_heads // n_kv_heads  # Q heads per KV group
        self.head_dim = dim // n_heads
        
        self.wq = nn.Linear(dim, n_heads * self.head_dim, bias=False)
        self.wk = nn.Linear(dim, n_kv_heads * self.head_dim, bias=False)
        self.wv = nn.Linear(dim, n_kv_heads * self.head_dim, bias=False)
        self.wo = nn.Linear(n_heads * self.head_dim, dim, bias=False)
        
        self.rope = RotaryPositionalEmbedding(self.head_dim, max_seq_len)
    
    def forward(self, x, mask=None):
        B, T, C = x.shape
        
        q = self.wq(x).view(B, T, self.n_heads, self.head_dim).transpose(1, 2)
        k = self.wk(x).view(B, T, self.n_kv_heads, self.head_dim).transpose(1, 2)
        v = self.wv(x).view(B, T, self.n_kv_heads, self.head_dim).transpose(1, 2)
        
        # Apply RoPE
        cos, sin = self.rope(x, T)
        cos = cos.unsqueeze(0).unsqueeze(0)  # (1, 1, T, head_dim)
        sin = sin.unsqueeze(0).unsqueeze(0)
        q, k = apply_rotary_pos_emb(q, k, cos, sin)
        
        # Expand KV heads to match Q heads (GQA)
        k = k.repeat_interleave(self.n_groups, dim=1)
        v = v.repeat_interleave(self.n_groups, dim=1)
        
        # Scaled dot-product attention (uses FlashAttention when available)
        out = F.scaled_dot_product_attention(q, k, v, attn_mask=mask, is_causal=True)
        
        out = out.transpose(1, 2).contiguous().view(B, T, -1)
        return self.wo(out)


class SwiGLU(nn.Module):
    """SwiGLU FFN — used in Llama, Mistral (better than ReLU FFN)."""
    def __init__(self, dim, hidden_dim):
        super().__init__()
        self.w1 = nn.Linear(dim, hidden_dim, bias=False)  # Gate
        self.w2 = nn.Linear(hidden_dim, dim, bias=False)  # Down
        self.w3 = nn.Linear(dim, hidden_dim, bias=False)  # Up
    
    def forward(self, x):
        return self.w2(F.silu(self.w1(x)) * self.w3(x))


class TransformerBlock(nn.Module):
    """Single transformer block (Llama-style: pre-norm, GQA, SwiGLU)."""
    def __init__(self, dim, n_heads, n_kv_heads, ff_dim):
        super().__init__()
        self.attention = GroupedQueryAttention(dim, n_heads, n_kv_heads)
        self.feed_forward = SwiGLU(dim, ff_dim)
        self.norm1 = RMSNorm(dim)
        self.norm2 = RMSNorm(dim)
    
    def forward(self, x, mask=None):
        # Pre-norm architecture (more stable than post-norm)
        x = x + self.attention(self.norm1(x), mask)
        x = x + self.feed_forward(self.norm2(x))
        return x


class LlamaStyleLLM(nn.Module):
    """Complete Llama-style LLM implementation."""
    
    def __init__(self, vocab_size=128000, dim=4096, n_layers=32,
                 n_heads=32, n_kv_heads=8, ff_dim=14336, max_seq_len=8192):
        super().__init__()
        self.tok_embeddings = nn.Embedding(vocab_size, dim)
        
        self.layers = nn.ModuleList([
            TransformerBlock(dim, n_heads, n_kv_heads, ff_dim)
            for _ in range(n_layers)
        ])
        
        self.norm = RMSNorm(dim)
        self.output = nn.Linear(dim, vocab_size, bias=False)
        
        # Weight tying (embedding = output projection)
        self.output.weight = self.tok_embeddings.weight
        
        # Initialize weights
        self.apply(self._init_weights)
        print(f"Model parameters: {sum(p.numel() for p in self.parameters()):,}")
    
    def _init_weights(self, module):
        if isinstance(module, nn.Linear):
            nn.init.normal_(module.weight, std=0.02)
        elif isinstance(module, nn.Embedding):
            nn.init.normal_(module.weight, std=0.02)
    
    def forward(self, tokens, targets=None):
        x = self.tok_embeddings(tokens)
        
        for layer in self.layers:
            x = layer(x)
        
        x = self.norm(x)
        logits = self.output(x)
        
        loss = None
        if targets is not None:
            loss = F.cross_entropy(
                logits.view(-1, logits.size(-1)),
                targets.view(-1),
                ignore_index=-1,  # Ignore padding
            )
        
        return logits, loss
    
    @torch.no_grad()
    def generate(self, tokens, max_new_tokens=100, temperature=0.8, top_k=50):
        """Autoregressive generation with KV-cache (simplified)."""
        for _ in range(max_new_tokens):
            # Crop to max context
            logits, _ = self(tokens[:, -8192:])
            logits = logits[:, -1, :] / temperature
            
            # Top-k sampling
            if top_k:
                v, _ = torch.topk(logits, top_k)
                logits[logits < v[:, [-1]]] = float('-inf')
            
            probs = F.softmax(logits, dim=-1)
            next_token = torch.multinomial(probs, num_samples=1)
            tokens = torch.cat([tokens, next_token], dim=1)
        
        return tokens


# Instantiate a small model for demonstration
model = LlamaStyleLLM(
    vocab_size=32000, dim=512, n_layers=8,
    n_heads=8, n_kv_heads=2, ff_dim=1376, max_seq_len=2048
)
# Output: Model parameters: ~50M (small demo model)

# Training step
optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4, weight_decay=0.1)

tokens = torch.randint(0, 32000, (4, 512))  # Batch of 4, seq_len 512
targets = torch.randint(0, 32000, (4, 512))

logits, loss = model(tokens, targets)
loss.backward()
optimizer.step()
optimizer.zero_grad()
print(f"Loss: {loss.item():.4f}")
```

### Key Implementation Details

| Component | What We Implemented | Production Equivalent |
|-----------|--------------------|-----------------------|
| RMSNorm | Simpler than LayerNorm, no mean subtraction | Same (Llama, Mistral) |
| RoPE | Rotary embeddings for position | Same + NTK scaling for long context |
| GQA | 8 KV heads shared across 32 Q heads | Same (4x smaller KV-cache) |
| SwiGLU | Gated FFN with SiLU activation | Same (better than ReLU) |
| Pre-norm | Normalize before attention/FFN | Same (more stable training) |
| Weight tying | Embedding = output projection | Same (saves parameters) |

---

## Next: [Pre-Training →](02_PreTraining.md)
