# 6. Fine-Tuning Claude

## Overview

Fine-tuning creates a custom Claude model trained on your specific data, improving performance for domain-specific tasks while maintaining Claude's general capabilities.

**When to Fine-Tune vs Prompt Engineering:**

| Approach | Best For | Cost | Effort |
|----------|----------|------|--------|
| Prompt engineering | Most tasks, quick iteration | Per-token only | Low |
| Few-shot examples | Format/style consistency | Slightly more tokens | Low |
| RAG | Knowledge-intensive tasks | Embedding + retrieval | Medium |
| **Fine-tuning** | Consistent style, domain expertise, complex formats | Training + inference | High |

### When Fine-Tuning Makes Sense

- Consistent output format that's hard to achieve with prompts alone
- Domain-specific terminology and reasoning patterns
- Reducing prompt size (bake instructions into the model)
- Improving performance on specific task types by 10-30%
- Maintaining a specific persona/voice across all interactions
- Handling edge cases that general models struggle with

---

## Fine-Tuning Process

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Prepare     │────►│  Upload &    │────►│  Train       │────►│  Evaluate &  │
│  Training    │     │  Validate    │     │  Model       │     │  Deploy      │
│  Data        │     │  Dataset     │     │              │     │              │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

---

## Training Data Preparation

### Data Format (JSONL)

```jsonl
{"messages": [{"role": "system", "content": "You are Adobe's technical support agent."}, {"role": "user", "content": "How do I fix the scratch disk error in Photoshop?"}, {"role": "assistant", "content": "The scratch disk error means Photoshop has run out of temporary storage space. Here's how to fix it:\n\n1. **Free disk space**: Delete unnecessary files from your scratch disk drive\n2. **Change scratch disk**: Go to Edit > Preferences > Scratch Disks and select a drive with more space\n3. **Clear Photoshop cache**: Edit > Purge > All\n4. **Increase RAM allocation**: Edit > Preferences > Performance > Memory Usage\n\nIf Photoshop won't open at all, hold Ctrl+Alt (Windows) or Cmd+Option (Mac) while launching to reset scratch disk preferences."}]}
{"messages": [{"role": "system", "content": "You are Adobe's technical support agent."}, {"role": "user", "content": "My Illustrator keeps crashing when I try to save"}, {"role": "assistant", "content": "Illustrator crashes during save are usually caused by file corruption or resource issues. Let's troubleshoot:\n\n1. **Save to a different location**: Try saving to your Desktop instead of the current folder\n2. **Reduce file complexity**: If the file has many effects/gradients, try flattening some layers\n3. **Reset preferences**: Hold Ctrl+Alt+Shift (Windows) or Cmd+Opt+Shift (Mac) while launching\n4. **Update Illustrator**: Help > Updates — install any pending updates\n5. **Check available RAM**: Close other applications to free memory\n\nIf the issue persists, try exporting as .AI (legacy format) or .PDF as a workaround while we investigate further."}]}
```

### Data Quality Guidelines

```python
# Training data validation script
import json
from typing import List, Dict

def validate_training_data(filepath: str) -> Dict:
    issues = []
    valid_count = 0
    
    with open(filepath, 'r') as f:
        for line_num, line in enumerate(f, 1):
            try:
                example = json.loads(line)
                messages = example["messages"]
                
                # Check structure
                if not messages:
                    issues.append(f"Line {line_num}: Empty messages")
                    continue
                
                # Check roles alternate correctly
                for i, msg in enumerate(messages):
                    if msg["role"] not in ["system", "user", "assistant"]:
                        issues.append(f"Line {line_num}: Invalid role '{msg['role']}'")
                
                # Check assistant response quality
                assistant_msgs = [m for m in messages if m["role"] == "assistant"]
                for msg in assistant_msgs:
                    if len(msg["content"]) < 50:
                        issues.append(f"Line {line_num}: Assistant response too short ({len(msg['content'])} chars)")
                    if len(msg["content"]) > 10000:
                        issues.append(f"Line {line_num}: Assistant response too long ({len(msg['content'])} chars)")
                
                # Check for PII
                content = json.dumps(messages)
                if any(pattern in content for pattern in ["@gmail", "555-", "123-45-"]):
                    issues.append(f"Line {line_num}: Possible PII detected")
                
                valid_count += 1
                
            except json.JSONDecodeError:
                issues.append(f"Line {line_num}: Invalid JSON")
    
    return {
        "total_examples": line_num,
        "valid_examples": valid_count,
        "issues": issues,
        "quality_score": valid_count / line_num if line_num > 0 else 0
    }
```

### Data Generation with Claude

```python
async def generate_training_data(
    seed_examples: list,
    num_examples: int = 1000,
    domain: str = "customer_support"
) -> list:
    """Use Claude to generate diverse training examples from seed data."""
    
    client = anthropic.Anthropic()
    generated = []
    
    for i in range(0, num_examples, 10):
        # Sample seed examples for diversity
        seeds = random.sample(seed_examples, min(3, len(seed_examples)))
        
        response = client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=4096,
            messages=[{
                "role": "user",
                "content": f"""Generate 10 diverse training examples for a {domain} AI agent.

<seed_examples>
{json.dumps(seeds, indent=2)}
</seed_examples>

<requirements>
- Vary the complexity (simple questions to multi-step issues)
- Include different customer emotions (frustrated, confused, happy)
- Cover different product areas
- Make responses helpful, accurate, and empathetic
- Include edge cases and unusual scenarios
- Each response should be 100-500 words
</requirements>

Return as a JSON array of objects with "messages" field (same format as seeds).
Generate DIFFERENT scenarios from the seeds — use them only as style reference."""
            }]
        )
        
        batch = json.loads(response.content[0].text)
        generated.extend(batch)
    
    return generated[:num_examples]
```

---

## Fine-Tuning API

### Create Fine-Tune Job

```python
import anthropic

client = anthropic.Anthropic()

# Upload training file
with open("training_data.jsonl", "rb") as f:
    training_file = client.files.create(
        file=f,
        purpose="fine-tune"
    )

# Upload validation file (optional but recommended)
with open("validation_data.jsonl", "rb") as f:
    validation_file = client.files.create(
        file=f,
        purpose="fine-tune"
    )

# Create fine-tuning job
fine_tune_job = client.fine_tuning.jobs.create(
    model="claude-sonnet-4-20250514",  # Base model
    training_file=training_file.id,
    validation_file=validation_file.id,
    hyperparameters={
        "n_epochs": 3,              # Number of training passes
        "learning_rate": 1e-5,      # Lower = more conservative
        "batch_size": 8,            # Examples per training step
    },
    suffix="adobe-support-v1"       # Custom model name suffix
)

print(f"Job ID: {fine_tune_job.id}")
print(f"Status: {fine_tune_job.status}")
```

### Monitor Training

```python
# Check job status
job = client.fine_tuning.jobs.retrieve(fine_tune_job.id)
print(f"Status: {job.status}")  # queued, running, succeeded, failed
print(f"Trained tokens: {job.trained_tokens}")

# List training events
events = client.fine_tuning.jobs.list_events(fine_tune_job.id)
for event in events.data:
    print(f"{event.created_at}: {event.message}")
    if event.data:
        print(f"  Loss: {event.data.get('training_loss')}")
        print(f"  Val Loss: {event.data.get('validation_loss')}")
```

### Use Fine-Tuned Model

```python
# Use your custom model (same API, different model ID)
response = client.messages.create(
    model=f"ft:claude-sonnet-4-20250514:org-id::adobe-support-v1",
    max_tokens=2048,
    messages=[{
        "role": "user",
        "content": "My Creative Cloud subscription shows as expired but I just paid"
    }]
)
```

---

## Evaluation Framework

```python
class FineTuneEvaluator:
    """Compare fine-tuned model against base model."""
    
    def __init__(self, base_model: str, fine_tuned_model: str):
        self.client = anthropic.Anthropic()
        self.base = base_model
        self.fine_tuned = fine_tuned_model
    
    def evaluate(self, test_set: list) -> dict:
        base_results = []
        ft_results = []
        
        for example in test_set:
            query = example["messages"][-2]["content"]  # User message
            expected = example["messages"][-1]["content"]  # Expected response
            
            # Get responses from both models
            base_resp = self._get_response(self.base, example["messages"][:-1])
            ft_resp = self._get_response(self.fine_tuned, example["messages"][:-1])
            
            # Score both
            base_score = self._score(query, base_resp, expected)
            ft_score = self._score(query, ft_resp, expected)
            
            base_results.append(base_score)
            ft_results.append(ft_score)
        
        return {
            "base_model": {
                "avg_score": sum(r["overall"] for r in base_results) / len(base_results),
                "accuracy": sum(1 for r in base_results if r["overall"] >= 4) / len(base_results),
            },
            "fine_tuned": {
                "avg_score": sum(r["overall"] for r in ft_results) / len(ft_results),
                "accuracy": sum(1 for r in ft_results if r["overall"] >= 4) / len(ft_results),
            },
            "improvement": (sum(r["overall"] for r in ft_results) - sum(r["overall"] for r in base_results)) / len(test_set)
        }
    
    def _score(self, query: str, response: str, expected: str) -> dict:
        """Use Claude as judge to score response quality."""
        judge_response = self.client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=200,
            messages=[{
                "role": "user",
                "content": f"""Score this AI response on a 1-5 scale.

Query: {query}
Expected response (reference): {expected[:500]}
Actual response: {response[:500]}

Score on:
- accuracy (1-5): Is the information correct?
- helpfulness (1-5): Does it solve the user's problem?
- tone (1-5): Is it professional and empathetic?
- format (1-5): Is it well-structured and clear?
- overall (1-5): Overall quality

JSON only: {{"accuracy": N, "helpfulness": N, "tone": N, "format": N, "overall": N}}"""
            }]
        )
        return json.loads(judge_response.content[0].text)
```

---

## Training Data Best Practices

| Guideline | Recommendation |
|-----------|---------------|
| **Minimum examples** | 50-100 for style, 500+ for domain expertise |
| **Diversity** | Cover all expected input types and edge cases |
| **Quality > Quantity** | 500 excellent examples > 5000 mediocre ones |
| **Consistent format** | All examples should follow the same output structure |
| **Include edge cases** | 10-20% of data should be tricky/unusual scenarios |
| **Validation split** | 10-20% held out for evaluation |
| **No PII** | Remove all personal information from training data |
| **Balanced categories** | Don't over-represent one type of query |
| **Response length** | Match the length you want in production |
| **System prompt** | Use the same system prompt you'll use in production |

---

## Alternatives to Fine-Tuning

| Technique | When to Use | Effort |
|-----------|-------------|--------|
| **Better prompts** | First thing to try always | Minutes |
| **Few-shot examples** | Need consistent format | Hours |
| **RAG** | Need domain knowledge | Days |
| **Prompt caching** | Reduce cost of long prompts | Hours |
| **Fine-tuning** | All above insufficient | Weeks |
| **Constitutional AI** | Need specific behavioral constraints | Days |

---

## Next: [Production & Safety →](07_Production_and_Safety.md)
