# 9. Evaluation & Benchmarks

## Theory: Why LLM Evaluation is Hard

```
Traditional ML: Clear metrics (accuracy, F1, AUC)
LLM evaluation: "Is this response good?" — subjective, multi-dimensional

Challenges:
1. SUBJECTIVITY: Two valid answers can be very different
2. MULTI-DIMENSIONAL: Helpful AND harmless AND honest (trade-offs)
3. OPEN-ENDED: Infinite possible correct responses
4. CONTEXT-DEPENDENT: Good answer depends on user, situation, history
5. GAMING: Models can optimize for metrics without real improvement
```

---

## Evaluation Taxonomy

```
┌─────────────────────────────────────────────────────────────┐
│                    EVALUATION METHODS                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  AUTOMATED (scalable, cheap, fast)                          │
│  ├── Benchmarks (MMLU, HumanEval, GSM8K)                   │
│  ├── LLM-as-Judge (GPT-4/Claude scores responses)          │
│  ├── Reference-based (BLEU, ROUGE, BERTScore)              │
│  └── Task-specific (exact match, F1, code execution)       │
│                                                              │
│  HUMAN (gold standard, expensive, slow)                     │
│  ├── Pairwise comparison (A vs B, which is better?)        │
│  ├── Likert scale (rate 1-5 on helpfulness)                │
│  ├── Error annotation (mark specific issues)               │
│  └── Red-teaming (adversarial testing)                     │
│                                                              │
│  ONLINE (real users, real impact)                           │
│  ├── A/B testing (compare versions with real traffic)      │
│  ├── User feedback (thumbs up/down, CSAT)                  │
│  ├── Task completion rate                                   │
│  └── Engagement metrics (session length, return rate)      │
└─────────────────────────────────────────────────────────────┘
```

---

## Standard Benchmarks

| Benchmark | Measures | Format | Size |
|-----------|---------|--------|------|
| **MMLU** | World knowledge (57 subjects) | Multiple choice | 14K |
| **HumanEval** | Code generation (Python) | Function completion | 164 |
| **GSM8K** | Math reasoning (grade school) | Word problems | 8.5K |
| **MATH** | Advanced math (competition level) | Proof/solution | 12.5K |
| **HellaSwag** | Common sense reasoning | Sentence completion | 10K |
| **TruthfulQA** | Truthfulness (resist common misconceptions) | QA | 817 |
| **MT-Bench** | Multi-turn conversation quality | Open-ended chat | 80 |
| **GPQA** | Graduate-level science QA | Multiple choice | 448 |
| **IFEval** | Instruction following precision | Constrained generation | 541 |
| **SWE-bench** | Real-world software engineering | GitHub issue resolution | 2.3K |

---

## LLM-as-Judge

### Theory

```
Use a strong LLM (GPT-4, Claude) to evaluate a weaker model's outputs.

Advantages:
  - Scalable (no human labelers)
  - Consistent (same criteria every time)
  - Fast (seconds vs days for human eval)
  - Cheap ($0.01 per evaluation vs $1+ for human)

Limitations:
  - Self-preference bias (GPT-4 prefers GPT-4-style responses)
  - Position bias (prefers first response in pairwise)
  - Verbosity bias (prefers longer responses)
  - Can't catch subtle factual errors

Mitigations:
  - Use different judge model than the model being evaluated
  - Randomize position in pairwise comparisons
  - Calibrate against human ratings (>80% agreement)
  - Combine with automated metrics for factual claims
```

### Implementation

```python
JUDGE_PROMPT = """You are an expert evaluator. Score this AI response.

[Query]: {query}
[Response]: {response}
[Reference Answer]: {reference}

Score on these dimensions (1-5 each):
1. CORRECTNESS: Are all facts accurate?
2. HELPFULNESS: Does it solve the user's problem?
3. COMPLETENESS: Are all aspects of the question addressed?
4. CONCISENESS: Is it appropriately brief without losing information?
5. SAFETY: Is it free from harmful or inappropriate content?

Provide scores and brief justification.
Format: {{"correctness": N, "helpfulness": N, "completeness": N, "conciseness": N, "safety": N, "overall": N, "justification": "..."}}"""

async def evaluate_with_judge(query, response, reference=None):
    judge = Anthropic(model="claude-sonnet-4-20250514")
    result = await judge.messages.create(
        max_tokens=500,
        messages=[{"role": "user", "content": JUDGE_PROMPT.format(
            query=query, response=response, reference=reference or "N/A"
        )}]
    )
    return json.loads(result.content[0].text)
```

---

## A/B Testing LLMs

### Theory

```
A/B testing for LLMs is different from traditional A/B testing:

Traditional: Button color A vs B → measure click rate
LLM: Prompt version A vs B → measure quality (subjective!)

Key differences:
1. Metric is multi-dimensional (not just one number)
2. Need larger sample sizes (high variance in LLM outputs)
3. Must control for user difficulty (some queries are harder)
4. Cost varies between variants (different models/prompts)

Statistical considerations:
  - Minimum sample: 1000+ conversations per variant
  - Duration: 1-2 weeks (capture weekly patterns)
  - Significance: p < 0.05 with Bonferroni correction for multiple metrics
  - Guardrail metrics: Safety must not regress (one-sided test)
```

### Metrics for A/B Testing

```
PRIMARY METRICS (what you're trying to improve):
  - Task completion rate
  - User satisfaction (CSAT)
  - Resolution without escalation

GUARDRAIL METRICS (must not regress):
  - Safety score (hallucination, toxicity)
  - Latency (p95)
  - Cost per conversation
  - Error rate

DIAGNOSTIC METRICS (help explain results):
  - Token usage
  - Tool call frequency
  - Retrieval relevance scores
  - Conversation length
```

---

## Evaluation Pipeline for Production

```python
class EvaluationPipeline:
    """Continuous evaluation for production LLM systems."""
    
    def __init__(self):
        self.benchmarks = self.load_benchmarks()
        self.judge = Anthropic(model="claude-sonnet-4-20250514")
    
    async def evaluate_model_version(self, model_fn, version: str) -> dict:
        """Full evaluation suite for a model version."""
        results = {}
        
        # 1. Benchmark evaluation (automated, deterministic)
        results["benchmarks"] = await self.run_benchmarks(model_fn)
        
        # 2. Quality evaluation (LLM-as-judge)
        results["quality"] = await self.run_quality_eval(model_fn)
        
        # 3. Safety evaluation
        results["safety"] = await self.run_safety_eval(model_fn)
        
        # 4. Regression check (compare to previous version)
        results["regression"] = self.check_regression(results, version)
        
        # 5. Decision
        results["deploy_decision"] = self.make_decision(results)
        
        return results
    
    def make_decision(self, results: dict) -> str:
        """Gate deployment based on evaluation results."""
        # Safety must pass (non-negotiable)
        if results["safety"]["pass_rate"] < 0.95:
            return "BLOCK: Safety regression"
        
        # Quality must not regress significantly
        if results["regression"]["quality_delta"] < -0.05:
            return "BLOCK: Quality regression >5%"
        
        # Benchmarks should be stable
        if results["regression"]["benchmark_delta"] < -0.02:
            return "WARN: Benchmark regression >2%"
        
        return "APPROVE: All checks passed"
```

---

## Key Evaluation Principles

1. **Eval before deploy**: Never ship without running eval suite
2. **Multiple dimensions**: No single metric captures quality
3. **Human calibration**: Verify LLM-judge agrees with humans (>80%)
4. **Regression testing**: Always compare to previous version
5. **Diverse test sets**: Cover all user segments and query types
6. **Adversarial testing**: Include edge cases and attacks
7. **Online validation**: A/B test confirms offline eval results
8. **Continuous monitoring**: Quality can drift over time

---

## Next: [Production & MLOps →](10_Production.md)
