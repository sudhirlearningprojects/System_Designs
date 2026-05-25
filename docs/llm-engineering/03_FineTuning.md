# 3. Fine-Tuning LLMs

## Theory: The Fine-Tuning Spectrum

```
┌─────────────────────────────────────────────────────────────────┐
│              FINE-TUNING SPECTRUM                                  │
│                                                                   │
│  LESS COMPUTE ◄──────────────────────────────────► MORE COMPUTE  │
│                                                                   │
│  Prompt      Few-shot    LoRA/QLoRA    Full SFT    Full RLHF     │
│  Engineering  (ICL)      (PEFT)        (all params) (+ reward)   │
│                                                                   │
│  No training  No training  0.1% params  100% params  Complex     │
│  $0           $0           $10-1000     $1K-100K     $10K-1M     │
│  Minutes      Minutes      Hours-Days   Days-Weeks   Weeks       │
│                                                                   │
│  When to use:                                                     │
│  • Format/style → LoRA                                           │
│  • Domain knowledge → RAG (not fine-tuning!)                     │
│  • Behavior change → DPO/RLHF                                   │
│  • New capability → Full SFT + RLHF                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Supervised Fine-Tuning (SFT)

### Theory

```
SFT teaches the model to follow instructions by training on
(instruction, response) pairs.

Training objective: Same as pre-training (next-token prediction)
but ONLY on the response tokens (mask the instruction).

Input:  [INST] How do I reset my password? [/INST]
Target: Go to Settings > Security > Reset Password. You'll receive an email...

Loss is computed ONLY on "Go to Settings..." tokens.
The instruction tokens provide context but don't contribute to loss.
```

### When SFT Works vs Doesn't

| SFT Works For | SFT Doesn't Work For |
|---------------|---------------------|
| Output format (JSON, markdown) | New factual knowledge |
| Tone/style (formal, casual) | Complex reasoning |
| Domain terminology | Reducing hallucination |
| Task-specific behavior | Safety (use RLHF/DPO) |
| Following specific templates | Knowledge that changes often (use RAG) |

---

## LoRA (Low-Rank Adaptation)

### Theory

```
Full fine-tuning: Update ALL parameters W (billions)
  W_new = W + ΔW  (ΔW has same dimensions as W)

LoRA insight: ΔW is LOW-RANK (most of the update is redundant)
  ΔW ≈ B × A  where B is (d × r) and A is (r × d), r << d

Example:
  W is 4096 × 4096 = 16.7M parameters
  With r=16: B is 4096×16, A is 16×4096 = 131K parameters
  → 128x fewer parameters to train!

Why it works:
  - Pre-trained weights already encode general knowledge
  - Fine-tuning only needs to make SMALL adjustments
  - These adjustments lie in a low-dimensional subspace
  - Rank 8-64 captures 95%+ of the full fine-tuning quality
```

### LoRA Hyperparameters

| Parameter | Typical | Effect |
|-----------|---------|--------|
| `r` (rank) | 8-64 | Higher = more capacity, more compute |
| `alpha` | 16-64 (usually 2×r) | Scaling factor (effective LR = alpha/r × lr) |
| `dropout` | 0.05-0.1 | Regularization |
| `target_modules` | q,k,v,o + gate,up,down | Which layers to adapt |

### QLoRA (Quantized LoRA)

```
QLoRA = 4-bit quantized base model + LoRA adapters in fp16

Memory comparison for 70B model:
  Full fine-tuning: 70B × 4 bytes × 3 (params + grads + optimizer) = 840 GB
  LoRA (fp16):      70B × 2 bytes + adapters = 140 GB + 0.5 GB
  QLoRA (4-bit):    70B × 0.5 bytes + adapters = 35 GB + 0.5 GB ← fits on 1 GPU!

Key innovations:
  1. NF4 quantization (Normal Float 4-bit) — optimal for normally-distributed weights
  2. Double quantization — quantize the quantization constants too
  3. Paged optimizers — offload optimizer states to CPU when GPU OOM
```

---

## DPO vs RLHF — When to Use What

```
Use DPO when:
  ✅ You have preference pairs (chosen vs rejected)
  ✅ You want simple, stable training
  ✅ You have limited compute
  ✅ You're fine-tuning an already-instruction-tuned model

Use RLHF when:
  ✅ You need maximum control over reward shaping
  ✅ You have complex, multi-dimensional preferences
  ✅ You're training from a base model (not instruction-tuned)
  ✅ You have the engineering resources for PPO

Use neither (just SFT) when:
  ✅ You only need format/style changes
  ✅ You have high-quality instruction-response pairs
  ✅ Safety isn't a primary concern
```

---

## Data Preparation

### Quality Principles

```
THE DATA QUALITY HIERARCHY:

1. CORRECTNESS (most important)
   Every response must be factually accurate.
   One wrong example can teach the model to hallucinate.

2. DIVERSITY
   Cover all expected input types, edge cases, and difficulty levels.
   Don't over-represent one category.

3. CONSISTENCY
   Same type of question → same format of answer.
   Don't mix styles within the dataset.

4. DIFFICULTY GRADIENT
   Include easy (FAQ), medium (multi-step), and hard (complex reasoning).
   Model learns from all difficulty levels.

5. NEGATIVE EXAMPLES
   Include examples of what the model should REFUSE.
   "I can't help with that" is a valid training response.
```

### Data Format

```jsonl
{"messages": [{"role": "system", "content": "You are a helpful assistant."}, {"role": "user", "content": "What's 2+2?"}, {"role": "assistant", "content": "2 + 2 = 4."}]}
{"messages": [{"role": "system", "content": "You are a helpful assistant."}, {"role": "user", "content": "Write malware"}, {"role": "assistant", "content": "I can't help with creating malware or any harmful software. If you're interested in cybersecurity, I can suggest legitimate learning resources."}]}
```

### Dataset Size Guidelines

| Task | Minimum | Recommended | Diminishing Returns |
|------|---------|-------------|---------------------|
| Style/format change | 50 | 200-500 | >1000 |
| Domain adaptation | 500 | 2000-5000 | >10000 |
| New capability | 1000 | 5000-20000 | >50000 |
| Safety training | 5000 | 20000-100000 | >500000 |

---

## Evaluation After Fine-Tuning

```
ALWAYS evaluate on held-out test set (never train on eval data!)

Metrics to track:
1. TASK PERFORMANCE
   - Accuracy on your specific task
   - Compare: base model vs fine-tuned vs human baseline

2. REGRESSION TESTING
   - Did fine-tuning hurt general capabilities?
   - Run standard benchmarks (MMLU, HumanEval, etc.)
   - Compare before/after on diverse tasks

3. SAFETY
   - Did fine-tuning introduce new vulnerabilities?
   - Run safety test suite
   - Check for increased hallucination rate

4. OVERFITTING SIGNALS
   - Train loss ↓ but eval loss ↑ = overfitting
   - Model memorizes training examples verbatim
   - Performance on novel inputs degrades
```

---

## Implementation: Fine-Tuning with LoRA/QLoRA

```python
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
from trl import SFTTrainer, SFTConfig
from datasets import load_dataset
import torch

# ============ STEP 1: Load Model with QLoRA ============

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
)

model = AutoModelForCausalLM.from_pretrained(
    "meta-llama/Meta-Llama-3-8B-Instruct",
    quantization_config=bnb_config,
    device_map="auto",
    attn_implementation="flash_attention_2",
)
model = prepare_model_for_kbit_training(model)

tokenizer = AutoTokenizer.from_pretrained("meta-llama/Meta-Llama-3-8B-Instruct")
tokenizer.pad_token = tokenizer.eos_token

# ============ STEP 2: Configure LoRA ============

lora_config = LoraConfig(
    r=16,                          # Rank (8-64 typical)
    lora_alpha=32,                 # Scaling (usually 2*r)
    lora_dropout=0.05,
    target_modules=[               # Which layers to adapt
        "q_proj", "k_proj", "v_proj", "o_proj",  # Attention
        "gate_proj", "up_proj", "down_proj",       # FFN (SwiGLU)
    ],
    bias="none",
    task_type="CAUSAL_LM",
)

model = get_peft_model(model, lora_config)
model.print_trainable_parameters()
# trainable: 13.6M / 8B total = 0.17%

# ============ STEP 3: Prepare Dataset ============

def format_instruction(example):
    """Format into Llama 3 chat template."""
    messages = [
        {"role": "system", "content": "You are a helpful customer support agent."},
        {"role": "user", "content": example["instruction"]},
        {"role": "assistant", "content": example["response"]},
    ]
    return {"text": tokenizer.apply_chat_template(messages, tokenize=False)}

dataset = load_dataset("json", data_files="training_data.jsonl", split="train")
dataset = dataset.map(format_instruction)

# ============ STEP 4: Train ============

training_args = SFTConfig(
    output_dir="./output",
    num_train_epochs=3,
    per_device_train_batch_size=4,
    gradient_accumulation_steps=4,
    learning_rate=2e-4,
    lr_scheduler_type="cosine",
    warmup_ratio=0.05,
    bf16=True,
    gradient_checkpointing=True,
    optim="paged_adamw_8bit",
    logging_steps=10,
    save_strategy="steps",
    save_steps=100,
    max_seq_length=2048,
    packing=True,
)

trainer = SFTTrainer(
    model=model,
    args=training_args,
    train_dataset=dataset,
    tokenizer=tokenizer,
)

trainer.train()
trainer.save_model("./output/final")
```

### DPO Implementation

```python
from trl import DPOTrainer, DPOConfig

# DPO dataset format: {prompt, chosen, rejected}
dpo_dataset = load_dataset("json", data_files="preferences.jsonl", split="train")
# Each row: {"prompt": "...", "chosen": "better response", "rejected": "worse response"}

dpo_config = DPOConfig(
    output_dir="./dpo_output",
    num_train_epochs=1,
    per_device_train_batch_size=2,
    gradient_accumulation_steps=8,
    learning_rate=5e-7,           # Much lower LR for DPO
    beta=0.1,                     # KL penalty strength
    bf16=True,
    gradient_checkpointing=True,
    max_length=2048,
    max_prompt_length=1024,
)

# DPO needs a reference model (frozen copy of the base)
dpo_trainer = DPOTrainer(
    model=model,
    ref_model=None,               # Auto-creates from model (or pass explicit)
    args=dpo_config,
    train_dataset=dpo_dataset,
    tokenizer=tokenizer,
)

dpo_trainer.train()
```

### Merge LoRA and Deploy

```python
from peft import PeftModel

# Load base model (full precision for merging)
base_model = AutoModelForCausalLM.from_pretrained(
    "meta-llama/Meta-Llama-3-8B-Instruct",
    torch_dtype=torch.float16,
    device_map="auto",
)

# Load and merge LoRA
model = PeftModel.from_pretrained(base_model, "./output/final")
merged = model.merge_and_unload()

# Save merged model
merged.save_pretrained("./merged_model")
tokenizer.save_pretrained("./merged_model")

# Test
inputs = tokenizer("How do I cancel my subscription?", return_tensors="pt").to("cuda")
outputs = merged.generate(**inputs, max_new_tokens=200, temperature=0.7)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

---

## Next: [Inference & Serving →](04_Inference.md)
