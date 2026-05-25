# 5. Prompt Engineering & In-Context Learning

> **Full coverage**: See [Claude AI → Prompt Engineering](../claude-ai/02_Prompt_Engineering.md) for comprehensive prompt engineering guide with code examples.

## Theory: How Prompting Works

### In-Context Learning (ICL)

```
LLMs can learn new tasks from examples in the prompt WITHOUT any weight updates.

Zero-shot: "Translate to French: Hello" → "Bonjour"
  (Model uses pre-trained knowledge)

One-shot: "English: Hello → French: Bonjour\nEnglish: Goodbye → French:"
  (Model learns the pattern from one example)

Few-shot: Multiple examples → model infers the pattern
  (More examples = more reliable pattern matching)

Theory: ICL works because pre-training on diverse text teaches the model
to recognize and continue patterns. The prompt creates a "task context"
that activates relevant knowledge in the model weights.
```

### Chain-of-Thought (CoT)

```
Theory: Forcing the model to show intermediate reasoning steps
improves accuracy on complex tasks.

Without CoT: "What is 23 × 47?" → "1081" (often wrong)
With CoT: "What is 23 × 47? Think step by step."
  → "23 × 47 = 23 × 40 + 23 × 7 = 920 + 161 = 1081" ✓

Why it works:
  - Breaks complex reasoning into simpler sub-problems
  - Each step is easier for the model to get right
  - Errors are visible and can be caught
  - Activates "reasoning mode" in the model
```

### Key Techniques

| Technique | When to Use | Example |
|-----------|-------------|---------|
| Zero-shot | Simple, well-defined tasks | "Summarize this text:" |
| Few-shot | Need specific format/style | Show 3 examples of desired output |
| Chain-of-thought | Complex reasoning, math | "Think step by step" |
| Self-consistency | High-stakes decisions | Generate N answers, take majority vote |
| Tree-of-thought | Multi-path exploration | Explore multiple reasoning paths |
| ReAct | Tool use, multi-step | Interleave thinking and acting |
| Structured output | Need JSON/schema | Provide schema + examples |

---

## Next: [RAG & Grounding →](06_RAG.md)
