# 2. Pre-Training

## Theory: Training LLMs from Scratch

### Data Pipeline

```
Internet crawl (Common Crawl, ~100TB raw)
    → Deduplication (exact + fuzzy, removes ~30%)
    → Quality filtering (perplexity, classifier, heuristics)
    → Toxicity filtering (remove harmful content)
    → PII removal (emails, phone numbers, addresses)
    → Domain mixing (web 60%, books 15%, code 15%, academic 10%)
    → Tokenization (BPE/SentencePiece, 32K-128K vocab)
    → Final dataset: 1-15 trillion tokens
```

### Training Infrastructure

```
Typical setup for 70B model:
  - 2048 GPUs (H100 80GB)
  - 3D parallelism: Data × Tensor × Pipeline
  - Training time: 3-6 months
  - Cost: $10M-$50M
  - Power: ~2 MW continuous

Key challenges:
  - Hardware failures (mean time between failures: hours at this scale)
  - Checkpointing (save every 1000 steps, ~140GB per checkpoint)
  - Learning rate scheduling (warmup → cosine decay)
  - Loss spikes (require manual intervention or automatic rollback)
```

### Tokenization

```
BPE (Byte-Pair Encoding):
  1. Start with character-level vocabulary
  2. Find most frequent adjacent pair → merge into new token
  3. Repeat until vocabulary size reached (32K-128K)

Example:
  "lower" → ["l", "o", "w", "e", "r"]
  Most frequent pair: "e"+"r" → "er"
  → ["l", "o", "w", "er"]
  Most frequent pair: "l"+"o" → "lo"
  → ["lo", "w", "er"]

Modern tokenizers: SentencePiece (Llama), tiktoken (OpenAI)
Vocabulary size trade-off:
  Smaller (32K): More tokens per text, slower inference
  Larger (128K): Fewer tokens, better multilingual, larger embedding table
```

### Training Objective

```
Causal Language Modeling (next-token prediction):
  Loss = -1/N × Σ log P(t_i | t_1, ..., t_{i-1})

This single objective, at sufficient scale, produces:
  - Language understanding
  - World knowledge
  - Reasoning
  - Code generation
  - Translation
  - Summarization
  All as emergent capabilities!
```

### Scaling Laws (Chinchilla)

```
For compute budget C:
  Optimal parameters N ∝ C^0.5
  Optimal data D ∝ C^0.5
  
Rule of thumb: Train on ~20× parameters in tokens
  7B model → 140B tokens
  70B model → 1.4T tokens
  405B model → 8T tokens (Llama 3.1 used 15T!)
```

---

## Next: [Fine-Tuning →](03_FineTuning.md)
