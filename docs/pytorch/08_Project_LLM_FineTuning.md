# 8. Project: LLM Fine-Tuning Pipeline

## Overview

A production-grade pipeline for fine-tuning large language models (7B-70B parameters) using Parameter-Efficient Fine-Tuning (PEFT) with LoRA/QLoRA, distributed training, and evaluation.

**Use Case**: Fine-tune Llama 3 8B for domain-specific instruction following (customer support, code generation, medical QA).

```
┌─────────────────────────────────────────────────────────────────┐
│                 LLM FINE-TUNING PIPELINE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Data Prep → Tokenization → Training (LoRA) → Eval → Merge → Deploy │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌────────────┐  │
│  │ Dataset  │→ │Tokenizer │→ │ QLoRA Train  │→ │ Merge &    │  │
│  │ (JSONL)  │  │+ Packing │  │ (FSDP/DS)   │  │ Quantize   │  │
│  └──────────┘  └──────────┘  └──────────────┘  └────────────┘  │
│                                                                   │
│  Infrastructure: Multi-GPU (4-8x A100/H100), DeepSpeed ZeRO-3   │
└─────────────────────────────────────────────────────────────────┘
```

---

## LoRA Theory

### Low-Rank Adaptation

```
Standard fine-tuning: Update all W (billions of parameters)
LoRA: W' = W + ΔW = W + BA

Where:
- W: Original frozen weights (d × k)
- B: Low-rank matrix (d × r), r << min(d, k)
- A: Low-rank matrix (r × k)
- ΔW = BA: Low-rank update (rank r, typically 8-64)

Parameters saved: d×k → d×r + r×k
Example: 4096×4096 = 16.7M → 4096×16 + 16×4096 = 131K (128x reduction!)
```

### QLoRA (Quantized LoRA)

```
1. Quantize base model to 4-bit (NF4 format)
2. Add LoRA adapters in float16/bfloat16
3. Train only LoRA parameters
4. Result: Fine-tune 70B model on single 48GB GPU!

Memory: 70B × 4-bit = ~35GB (vs 280GB for float32)
```

---

## Implementation

### Data Preparation

```python
import json
from datasets import Dataset
from transformers import AutoTokenizer

def prepare_dataset(data_path: str, tokenizer, max_length: int = 2048):
    """Prepare instruction-following dataset."""
    
    with open(data_path) as f:
        raw_data = [json.loads(line) for line in f]
    
    def format_conversation(example):
        """Format into chat template."""
        messages = []
        if example.get("system"):
            messages.append({"role": "system", "content": example["system"]})
        messages.append({"role": "user", "content": example["instruction"]})
        messages.append({"role": "assistant", "content": example["response"]})
        
        # Apply chat template
        text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=False)
        return {"text": text}
    
    dataset = Dataset.from_list(raw_data)
    dataset = dataset.map(format_conversation, remove_columns=dataset.column_names)
    
    def tokenize(examples):
        tokenized = tokenizer(
            examples["text"],
            truncation=True,
            max_length=max_length,
            padding=False,
        )
        tokenized["labels"] = tokenized["input_ids"].copy()
        return tokenized
    
    dataset = dataset.map(tokenize, batched=True, remove_columns=["text"])
    return dataset


# Data format (JSONL)
# {"system": "You are a helpful assistant.", "instruction": "Explain Docker containers", "response": "Docker containers are..."}
```

### LoRA Configuration

```python
from peft import LoraConfig, get_peft_model, TaskType, prepare_model_for_kbit_training
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
import torch

def setup_model_and_lora(
    model_name: str = "meta-llama/Meta-Llama-3-8B-Instruct",
    lora_r: int = 16,
    lora_alpha: int = 32,
    lora_dropout: float = 0.05,
    use_4bit: bool = True,
):
    """Load model with QLoRA configuration."""
    
    # Quantization config (4-bit NF4)
    bnb_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_compute_dtype=torch.bfloat16,
        bnb_4bit_use_double_quant=True,  # Nested quantization
    ) if use_4bit else None
    
    # Load base model
    model = AutoModelForCausalLM.from_pretrained(
        model_name,
        quantization_config=bnb_config,
        device_map="auto",
        torch_dtype=torch.bfloat16,
        attn_implementation="flash_attention_2",  # FlashAttention for speed
    )
    
    # Prepare for k-bit training
    if use_4bit:
        model = prepare_model_for_kbit_training(model, use_gradient_checkpointing=True)
    
    # LoRA config
    lora_config = LoraConfig(
        r=lora_r,
        lora_alpha=lora_alpha,
        lora_dropout=lora_dropout,
        target_modules=[
            "q_proj", "k_proj", "v_proj", "o_proj",  # Attention
            "gate_proj", "up_proj", "down_proj",       # MLP
        ],
        bias="none",
        task_type=TaskType.CAUSAL_LM,
    )
    
    # Apply LoRA
    model = get_peft_model(model, lora_config)
    model.print_trainable_parameters()
    # trainable params: 13,631,488 || all params: 8,043,847,680 || trainable%: 0.1695
    
    # Tokenizer
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    tokenizer.pad_token = tokenizer.eos_token
    tokenizer.padding_side = "right"
    
    return model, tokenizer
```

### Training with SFTTrainer

```python
from trl import SFTTrainer, SFTConfig

def train(model, tokenizer, train_dataset, eval_dataset, output_dir="./output"):
    """Fine-tune with SFTTrainer (Supervised Fine-Tuning)."""
    
    training_args = SFTConfig(
        output_dir=output_dir,
        
        # Training hyperparameters
        num_train_epochs=3,
        per_device_train_batch_size=4,
        per_device_eval_batch_size=4,
        gradient_accumulation_steps=4,  # Effective batch = 4 * 4 = 16
        
        # Optimizer
        learning_rate=2e-4,
        weight_decay=0.01,
        optim="paged_adamw_8bit",  # Memory-efficient optimizer
        lr_scheduler_type="cosine",
        warmup_ratio=0.05,
        
        # Precision
        bf16=True,
        
        # Logging
        logging_steps=10,
        eval_strategy="steps",
        eval_steps=100,
        save_strategy="steps",
        save_steps=100,
        save_total_limit=3,
        
        # Performance
        gradient_checkpointing=True,
        gradient_checkpointing_kwargs={"use_reentrant": False},
        max_seq_length=2048,
        packing=True,  # Pack multiple short examples into one sequence
        
        # Misc
        report_to="wandb",
        seed=42,
    )
    
    trainer = SFTTrainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=eval_dataset,
        tokenizer=tokenizer,
    )
    
    trainer.train()
    trainer.save_model(f"{output_dir}/final")
    
    return trainer
```

### Custom Training Loop (Advanced Control)

```python
import torch
from torch.utils.data import DataLoader
from transformers import get_cosine_schedule_with_warmup
from torch.cuda.amp import autocast, GradScaler

def custom_train_loop(model, tokenizer, train_dataset, config):
    """Custom training loop for maximum control."""
    
    dataloader = DataLoader(
        train_dataset, batch_size=config.batch_size,
        shuffle=True, collate_fn=DataCollatorForLanguageModeling(tokenizer, mlm=False)
    )
    
    optimizer = torch.optim.AdamW(
        model.parameters(), lr=config.lr, weight_decay=config.weight_decay
    )
    
    total_steps = len(dataloader) * config.epochs // config.gradient_accumulation
    scheduler = get_cosine_schedule_with_warmup(
        optimizer, num_warmup_steps=int(0.05 * total_steps), num_training_steps=total_steps
    )
    
    model.train()
    global_step = 0
    
    for epoch in range(config.epochs):
        for step, batch in enumerate(dataloader):
            batch = {k: v.to(model.device) for k, v in batch.items()}
            
            with autocast(device_type='cuda', dtype=torch.bfloat16):
                outputs = model(**batch)
                loss = outputs.loss / config.gradient_accumulation
            
            loss.backward()
            
            if (step + 1) % config.gradient_accumulation == 0:
                torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
                optimizer.step()
                scheduler.step()
                optimizer.zero_grad()
                global_step += 1
                
                if global_step % config.log_every == 0:
                    print(f"Step {global_step} | Loss: {loss.item() * config.gradient_accumulation:.4f} | LR: {scheduler.get_last_lr()[0]:.6f}")
                
                if global_step % config.eval_every == 0:
                    eval_loss = evaluate(model, eval_dataloader)
                    print(f"  Eval Loss: {eval_loss:.4f} | Perplexity: {torch.exp(torch.tensor(eval_loss)):.2f}")
                    model.train()
```

---

## Evaluation

```python
import torch
from transformers import pipeline

class LLMEvaluator:
    """Evaluate fine-tuned LLM quality."""
    
    def __init__(self, model, tokenizer, device='cuda'):
        self.model = model
        self.tokenizer = tokenizer
        self.device = device
    
    def generate(self, prompt: str, max_new_tokens: int = 512, temperature: float = 0.7) -> str:
        messages = [{"role": "user", "content": prompt}]
        input_text = self.tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        inputs = self.tokenizer(input_text, return_tensors="pt").to(self.device)
        
        with torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_new_tokens=max_new_tokens,
                temperature=temperature,
                top_p=0.9,
                do_sample=True,
                repetition_penalty=1.1,
            )
        
        response = self.tokenizer.decode(outputs[0][inputs.input_ids.shape[1]:], skip_special_tokens=True)
        return response
    
    def evaluate_perplexity(self, eval_dataset) -> float:
        """Compute perplexity on evaluation set."""
        self.model.eval()
        total_loss = 0
        total_tokens = 0
        
        dataloader = DataLoader(eval_dataset, batch_size=4)
        
        with torch.no_grad():
            for batch in dataloader:
                batch = {k: v.to(self.device) for k, v in batch.items()}
                outputs = self.model(**batch)
                total_loss += outputs.loss.item() * batch['input_ids'].numel()
                total_tokens += batch['input_ids'].numel()
        
        avg_loss = total_loss / total_tokens
        return torch.exp(torch.tensor(avg_loss)).item()
    
    def evaluate_on_benchmarks(self, test_cases: list) -> dict:
        """Evaluate on custom test cases with LLM-as-judge."""
        results = []
        
        for case in test_cases:
            response = self.generate(case["prompt"])
            score = self._judge_response(case["prompt"], response, case.get("reference"))
            results.append({"prompt": case["prompt"], "response": response, "score": score})
        
        avg_score = sum(r["score"] for r in results) / len(results)
        return {"average_score": avg_score, "results": results}
```

---

## Merge and Deploy

```python
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer

def merge_and_save(base_model_name: str, adapter_path: str, output_path: str):
    """Merge LoRA weights into base model for deployment."""
    
    # Load base model (full precision for merging)
    base_model = AutoModelForCausalLM.from_pretrained(
        base_model_name, torch_dtype=torch.float16, device_map="auto"
    )
    
    # Load LoRA adapter
    model = PeftModel.from_pretrained(base_model, adapter_path)
    
    # Merge LoRA into base weights
    merged_model = model.merge_and_unload()
    
    # Save merged model
    merged_model.save_pretrained(output_path)
    
    tokenizer = AutoTokenizer.from_pretrained(base_model_name)
    tokenizer.save_pretrained(output_path)
    
    print(f"Merged model saved to {output_path}")
    return merged_model

# Quantize for efficient serving
def quantize_for_serving(model_path: str, output_path: str):
    """Quantize merged model to GPTQ/AWQ for fast inference."""
    from auto_gptq import AutoGPTQForCausalLM, BaseQuantizeConfig
    
    quantize_config = BaseQuantizeConfig(bits=4, group_size=128, damp_percent=0.1)
    
    model = AutoGPTQForCausalLM.from_pretrained(model_path, quantize_config)
    model.quantize(calibration_dataset)
    model.save_quantized(output_path)
```

---

## Multi-GPU Training with DeepSpeed

```yaml
# deepspeed_config.yaml (ZeRO Stage 3)
{
  "bf16": {"enabled": true},
  "zero_optimization": {
    "stage": 3,
    "offload_optimizer": {"device": "cpu", "pin_memory": true},
    "offload_param": {"device": "cpu", "pin_memory": true},
    "overlap_comm": true,
    "contiguous_gradients": true,
    "reduce_bucket_size": 5e7,
    "stage3_prefetch_bucket_size": 5e7,
    "stage3_param_persistence_threshold": 1e5
  },
  "gradient_accumulation_steps": 4,
  "gradient_clipping": 1.0,
  "train_batch_size": "auto",
  "train_micro_batch_size_per_gpu": "auto"
}
```

```bash
# Launch distributed training
deepspeed --num_gpus=4 train.py \
  --deepspeed deepspeed_config.yaml \
  --model_name meta-llama/Meta-Llama-3-8B-Instruct \
  --dataset ./data/train.jsonl \
  --output_dir ./output \
  --epochs 3 \
  --lr 2e-4
```

---

## Key Design Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| PEFT method | LoRA (r=16, alpha=32) | Best quality/efficiency trade-off; proven at scale |
| Quantization | QLoRA (NF4) | Enables 8B model on single 24GB GPU |
| Target modules | All attention + MLP | Better than attention-only for instruction tuning |
| Optimizer | Paged AdamW 8-bit | 2x memory savings vs standard AdamW |
| Precision | BFloat16 | Better training stability than FP16, no loss scaling |
| Attention | FlashAttention 2 | 2-4x speedup, O(N) memory vs O(N²) |
| Packing | Enabled | 30-50% training speedup by reducing padding |
| Gradient checkpointing | Enabled | 60% memory reduction at 30% speed cost |

---

## Next: [Project: Real-Time Video Understanding →](09_Project_Video_Understanding.md)
