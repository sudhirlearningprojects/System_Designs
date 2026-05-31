# 4. Fine-Tuning

## When to Fine-Tune

| Use Case | Fine-Tune? | Alternative |
|----------|-----------|-------------|
| Consistent output format | ✅ Yes | Few-shot examples |
| Domain terminology | ✅ Yes | System prompt |
| Specific tone/style | ✅ Yes | Prompt engineering |
| New factual knowledge | ❌ No | RAG |
| Reduce prompt length | ✅ Yes | Bake instructions into model |
| Improve on specific task | ✅ Yes | More examples in prompt |

## Data Preparation

```jsonl
{"messages": [{"role": "system", "content": "You are a support agent."}, {"role": "user", "content": "How do I cancel?"}, {"role": "assistant", "content": "Go to Settings > Subscription > Cancel Plan. You'll retain access until your billing period ends."}]}
{"messages": [{"role": "system", "content": "You are a support agent."}, {"role": "user", "content": "I was charged twice"}, {"role": "assistant", "content": "I'm sorry about the duplicate charge. Let me look into this. Can you provide your account email so I can check your billing history?"}]}
```

**Requirements:**
- Minimum 10 examples (recommended: 50-100+)
- JSONL format
- Each example: system + user + assistant messages
- Consistent system prompt across examples

## Training

```python
# Upload training file
file = client.files.create(
    file=open("training_data.jsonl", "rb"),
    purpose="fine-tune",
)

# Create fine-tuning job
job = client.fine_tuning.jobs.create(
    training_file=file.id,
    model="gpt-4o-mini-2024-07-18",  # Base model
    hyperparameters={
        "n_epochs": 3,
        "learning_rate_multiplier": 1.8,
        "batch_size": 4,
    },
    suffix="support-agent-v1",  # Custom model name suffix
)

print(f"Job ID: {job.id}")

# Monitor
job = client.fine_tuning.jobs.retrieve(job.id)
print(f"Status: {job.status}")  # validating_files → queued → running → succeeded

# List events
events = client.fine_tuning.jobs.list_events(job.id)
for event in events.data:
    print(f"{event.created_at}: {event.message}")

# Use fine-tuned model
response = client.chat.completions.create(
    model=job.fine_tuned_model,  # "ft:gpt-4o-mini-2024-07-18:org::abc123"
    messages=[{"role": "user", "content": "How do I cancel?"}],
)
```

## Evaluation

```python
# Compare base vs fine-tuned
def evaluate(model: str, test_cases: list) -> dict:
    scores = []
    for case in test_cases:
        response = client.chat.completions.create(
            model=model,
            messages=case["messages"][:-1],  # All except expected response
        )
        actual = response.choices[0].message.content
        expected = case["messages"][-1]["content"]
        score = judge_quality(actual, expected)  # LLM-as-judge
        scores.append(score)
    return {"avg_score": sum(scores) / len(scores)}

base_results = evaluate("gpt-4o-mini", test_cases)
ft_results = evaluate("ft:gpt-4o-mini:org::abc123", test_cases)
print(f"Base: {base_results['avg_score']:.2f}, Fine-tuned: {ft_results['avg_score']:.2f}")
```

---

## Next: [Embeddings & RAG →](05_Embeddings_RAG.md)
