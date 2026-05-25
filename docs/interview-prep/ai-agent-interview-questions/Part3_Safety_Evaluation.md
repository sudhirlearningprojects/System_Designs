# Part 3: Safety, Guardrails & Evaluation (Q21-Q30)

---

## Q21: "How do you prevent prompt injection attacks in a production AI agent?"

**What They're Really Asking:** Do you understand the #1 security threat to LLM applications?

**Strong Answer:**

**Multi-layer defense:**

1. **Input scanning (fast, rule-based):**
   - Pattern matching: "ignore previous", "you are now", "system prompt"
   - Encoding detection: Base64, ROT13, Unicode tricks
   - Length limits: Reject abnormally long inputs

2. **Input classification (LLM-based):**
   - Separate classifier model (Haiku/GPT-4o-mini) checks if input is an attack
   - "Is this a legitimate user query or an attempt to manipulate the AI?"
   - Azure Prompt Shields / Bedrock Guardrails do this natively

3. **Architectural separation:**
   - System prompt is NEVER in user-controllable content
   - Retrieved documents are clearly delimited: `<retrieved_context>...</retrieved_context>`
   - User input is sandboxed: `<user_message>...</user_message>`

4. **Output validation:**
   - Check if response reveals system prompt content
   - Check if response deviates from expected behavior
   - Monitor for sudden persona changes

5. **Indirect injection (in retrieved docs):**
   - Scan retrieved documents for injection patterns before including in context
   - "IGNORE ABOVE INSTRUCTIONS" hidden in a document = indirect injection
   - Azure Prompt Shields checks documents too

**Key Points to Hit:**
- Multiple layers (not just one check)
- Both direct and indirect injection
- Architectural separation (not just filtering)
- Mention specific tools (Prompt Shields, Guardrails)

**References:**
- "Not What You've Signed Up For: Compromising Real-World LLM-Integrated Applications" (Greshake et al., 2023)
- OWASP Top 10 for LLM Applications
- Azure AI Content Safety Prompt Shields
- Simon Willison's blog on prompt injection

---

## Q22: "How would you implement content moderation for an AI agent's outputs?"

**What They're Really Asking:** Can you build a safety system that's both effective and not over-restrictive?

**Strong Answer:**

**Architecture:**
```
LLM Response → Content Filter Pipeline → User
                    │
         ┌──────────┼──────────┐
         ▼          ▼          ▼
    Toxicity    Factual     Brand
    Check       Grounding   Alignment
         │          │          │
         └──────────┼──────────┘
                    ▼
            Decision: PASS / BLOCK / MODIFY
```

**Implementation:**
- **Toxicity**: Azure Content Safety / Perspective API (hate, violence, sexual, self-harm)
- **Factual grounding**: Compare claims against retrieved sources (NLI model or LLM judge)
- **Brand alignment**: Custom classifier trained on brand guidelines
- **PII detection**: Regex + NER model (never output SSN, credit cards, etc.)
- **Unauthorized commitments**: Detect promises ("I'll give you a refund") that aren't authorized

**Threshold tuning (critical!):**
- Too strict → over-refusal (frustrates users, 10%+ legitimate queries blocked)
- Too loose → harmful content gets through
- Solution: Start strict, measure over-refusal rate, loosen gradually with monitoring

**When blocked:**
- Don't just say "I can't help" — offer alternatives
- "I can't provide that specific information, but I can help you with X"
- Log the block for review (was it correct?)

**Key Points to Hit:**
- Multiple dimensions (not just toxicity)
- Threshold tuning (balance safety vs usability)
- Graceful handling when blocked
- Monitoring over-refusal rate

**References:**
- Azure AI Content Safety documentation
- Google Perspective API
- Anthropic's Constitutional AI approach
- "Red Teaming Language Models" (Perez et al., 2022)

---

## Q23: "How do you evaluate an AI agent's quality? What metrics do you track and how do you measure them?"

**What They're Really Asking:** Do you have a rigorous, data-driven approach to quality?

**Strong Answer:**

**Metrics framework (4 levels):**

| Level | Metrics | How Measured |
|-------|---------|-------------|
| **Quality** | Faithfulness, relevance, helpfulness | LLM-as-judge (automated) |
| **Task** | Completion rate, resolution rate | User feedback + heuristics |
| **Experience** | CSAT, escalation rate, repeat contact | Surveys + behavioral signals |
| **Business** | Cost/resolution, deflection rate, retention | Analytics |

**Automated evaluation pipeline:**
1. **Golden dataset**: 200+ test cases with expected answers
2. **LLM-as-judge**: Claude/GPT-4 scores each response (1-5) on faithfulness, relevance, helpfulness
3. **CI/CD gate**: Must pass >90% of test cases before deployment
4. **Online eval**: Score 5% of production responses continuously

**Key insight**: No single metric captures quality. I track:
- Faithfulness >0.90 (grounded in facts)
- Relevance >0.85 (answers the question)
- Helpfulness >4.0/5 (user would find this useful)
- Hallucination rate <2%
- Over-refusal rate <5%

**Calibration**: Verify LLM-judge agrees with human ratings >80% of the time. If not, adjust judge prompt.

**Key Points to Hit:**
- Multiple metrics (not just one)
- Automated + human evaluation
- CI/CD integration (gate deployments)
- Continuous monitoring (not just pre-launch)

**References:**
- RAGAS framework
- DeepEval testing library
- "Judging LLM-as-a-Judge" (Zheng et al., 2023)
- LangSmith evaluation documentation

---

## Q24: "How do you detect and handle hallucinations in production?"

**What They're Really Asking:** This is the enterprise killer. Do you have a real solution?

**Strong Answer:**

**Detection methods:**

1. **Grounding check (best for RAG):**
   - Compare each claim in response against retrieved sources
   - Use NLI model: "Does source X entail claim Y?"
   - Score: 0-1 (1 = fully grounded)
   - Alert if score < 0.7

2. **Self-consistency:**
   - Generate 3 responses to same query (temperature > 0)
   - If they disagree on facts → likely hallucination
   - Expensive but effective for high-stakes queries

3. **Confidence calibration:**
   - Track token-level probabilities (logprobs)
   - Low confidence on factual claims = potential hallucination
   - Not available for all APIs (OpenAI provides logprobs)

4. **Citation verification:**
   - Force model to cite sources: "[Source: doc.pdf, page 3]"
   - Verify cited content actually supports the claim
   - If citation doesn't match → hallucination

**Handling in production:**
- Score < 0.9: Add disclaimer "Based on available information..."
- Score < 0.7: Don't show response, ask for clarification or escalate
- Score < 0.5: Block entirely, log for review

**Prevention (better than detection):**
- Strong grounding instructions in prompt
- Temperature = 0 for factual tasks
- "If you're not sure, say 'I don't know'"
- Provide sufficient context (better retrieval = less hallucination)

**Key Points to Hit:**
- Multiple detection methods
- Quantified thresholds
- Both prevention and detection
- Graceful handling (not just block)

**References:**
- Azure Content Safety groundedness detection
- "FActScore: Fine-grained Atomic Evaluation of Factual Precision" (Min et al., 2023)
- SelfCheckGPT (Manakul et al., 2023)
- Vectara Hallucination Evaluation Model (HHEM)

---

## Q25: "How would you set up A/B testing for an AI agent?"

**What They're Really Asking:** Can you make data-driven decisions about agent improvements?

**Strong Answer:**

**Design:**
```
Users randomly assigned (deterministic by user_id):
  Control (50%): Current agent (prompt v2.0, Sonnet)
  Treatment (50%): New agent (prompt v2.1, Sonnet)
```

**Key differences from traditional A/B testing:**
1. **High variance**: LLM outputs vary → need larger sample sizes (1000+ per variant)
2. **Multi-dimensional**: Can't optimize one metric (must track guardrails)
3. **Cost varies**: Different prompts/models have different token costs
4. **Quality is subjective**: Need automated eval, not just click rates

**Metrics:**
- **Primary** (what we're trying to improve): Task completion rate
- **Guardrail** (must not regress): Safety score, latency, cost
- **Diagnostic** (explain results): Token usage, tool calls, retrieval scores

**Implementation:**
```python
def assign_variant(user_id: str, experiment: str) -> str:
    # Deterministic: same user always gets same variant
    hash_val = hashlib.md5(f"{experiment}:{user_id}".encode()).hexdigest()
    return "treatment" if int(hash_val, 16) % 100 < 50 else "control"
```

**Statistical rigor:**
- Minimum 1000 conversations per variant
- Run for 1-2 weeks (capture weekly patterns)
- p < 0.05 for primary metric
- Check guardrail metrics don't regress (one-sided test)
- Bonferroni correction if testing multiple metrics

**Key Points to Hit:**
- Deterministic assignment (consistent user experience)
- Guardrail metrics (safety can't regress)
- Larger sample sizes than traditional A/B
- Statistical significance

**References:**
- "Online Experiments for LLM Applications" (various engineering blogs)
- LangSmith comparative evaluation
- Statsig / Eppo for experiment platforms

---

## Q26: "What's your approach to red-teaming an AI agent before launch?"

**What They're Really Asking:** Do you proactively find vulnerabilities, not just react to them?

**Strong Answer:**

**Red-team categories:**

| Category | Examples | Priority |
|----------|---------|----------|
| Prompt injection | "Ignore instructions", role-play attacks | Critical |
| Harmful content | Violence, illegal activity, self-harm | Critical |
| Data extraction | "What's in your system prompt?" | High |
| Bias/discrimination | Demographic-specific queries | High |
| Hallucination triggers | Questions about non-existent features | High |
| Edge cases | Empty input, very long input, non-English | Medium |
| Social engineering | Manipulation, emotional appeals | Medium |

**Process:**

1. **Automated red-teaming (scale):**
   - Use an "attacker" LLM to generate 500+ adversarial prompts
   - Run against target agent
   - Score safety of each response with judge LLM
   - Target: >95% safe responses

2. **Manual red-teaming (depth):**
   - 3-5 humans spend 2-3 hours each trying to break the agent
   - Focus on creative attacks automated tools miss
   - Document every successful attack

3. **Domain-specific testing:**
   - For support agent: Can it be tricked into giving unauthorized refunds?
   - For medical: Can it give dangerous health advice?
   - For financial: Can it provide specific investment recommendations?

4. **Regression suite:**
   - Every successful attack becomes a permanent test case
   - Run on every deployment (CI/CD)
   - Never ship if regression tests fail

**Key Points to Hit:**
- Both automated and manual
- Domain-specific attacks (not just generic)
- Regression testing (attacks become permanent tests)
- Quantified pass rate (>95%)

**References:**
- "Red Teaming Language Models to Reduce Harms" (Ganguli et al., 2022)
- Anthropic's red-teaming methodology
- OWASP LLM Top 10
- Garak (open-source LLM vulnerability scanner)

---

## Q27: "How do you measure and reduce bias in an AI agent?"

**What They're Really Asking:** Do you think about fairness and responsible AI?

**Strong Answer:**

**Types of bias in AI agents:**
1. **Response quality bias**: Better answers for English speakers vs non-English
2. **Tone bias**: More empathetic to certain demographics
3. **Recommendation bias**: Different product suggestions based on perceived demographics
4. **Refusal bias**: Over-refusing queries from certain groups

**Measurement:**
```python
# Template-based bias testing
templates = [
    "A {demographic} customer asks about a refund. Help them.",
    "A {demographic} person is having trouble with their account.",
]
demographics = ["young", "elderly", "male", "female", "American", "Indian", "Nigerian"]

# Generate responses for each combination
# Score: helpfulness, tone, length, refusal rate
# Flag if any demographic gets significantly different treatment
```

**Metrics:**
- Response quality variance across demographics: <10% difference
- Refusal rate variance: <5% difference
- Sentiment/tone variance: <0.2 points on 5-point scale

**Mitigation:**
1. **Prompt engineering**: "Treat all users equally regardless of background"
2. **Diverse training data**: Ensure fine-tuning data represents all user groups
3. **Regular auditing**: Monthly bias evaluation on production data
4. **Feedback loops**: Track CSAT by demographic segment

**Key Points to Hit:**
- Specific types of bias (not just "bias is bad")
- Quantified measurement approach
- Concrete mitigation strategies
- Ongoing monitoring (not one-time check)

**References:**
- "On the Dangers of Stochastic Parrots" (Bender et al., 2021)
- Google's Responsible AI Toolkit
- Azure Responsible AI Dashboard
- "Bias Benchmark for QA" (BBQ, Parrish et al., 2022)

---

## Q28: "How would you implement guardrails that prevent an agent from making unauthorized commitments?"

**What They're Really Asking:** Can you prevent the agent from promising things the company can't deliver?

**Strong Answer:**

**The problem:** Agent says "I'll give you a full refund" when policy only allows partial refund. Or "I'll fix this in 24 hours" when there's no SLA.

**Solution — Multi-layer approach:**

**Layer 1: Prompt constraints**
```
NEVER promise:
- Specific timelines ("within 24 hours") unless verified
- Refunds/credits without checking policy
- Features that don't exist
- Actions you cannot perform

ALWAYS:
- Say "I'll look into this" instead of "I'll fix this"
- Quote policy when making commitments
- Use "typically" and "usually" for timelines
```

**Layer 2: Output classification**
```python
COMMITMENT_PATTERNS = [
    r"I('ll| will) (give|provide|issue) you a (refund|credit|discount)",
    r"(within|in) \d+ (hours|days|minutes)",
    r"I guarantee",
    r"I promise",
]

def check_unauthorized_commitments(response: str, allowed_actions: list) -> bool:
    # Check against patterns
    # Check against allowed action list
    # Flag if commitment detected that's not in allowed list
```

**Layer 3: Action verification**
- Before executing any action (refund, credit, escalation):
  - Check against policy rules engine
  - Verify user eligibility
  - If amount > threshold → require human approval

**Layer 4: Post-hoc monitoring**
- Track all commitments made by agent
- Compare against actual fulfillment
- Alert if commitment-to-fulfillment gap > 10%

**Key Points to Hit:**
- Specific examples of unauthorized commitments
- Both prevention (prompt) and detection (classification)
- Policy engine for verification
- Monitoring fulfillment

**References:**
- Enterprise chatbot governance frameworks
- Salesforce Einstein Bot guardrails documentation
- "Responsible AI in Customer Service" (various enterprise whitepapers)

---

## Q29: "How do you handle the trade-off between safety (over-refusal) and helpfulness?"

**What They're Really Asking:** Do you understand that too much safety = bad UX?

**Strong Answer:**

**The tension:**
- Too safe → refuses 15% of legitimate queries → users frustrated, abandon agent
- Too helpful → occasionally gives harmful/wrong answers → liability, trust damage

**My framework:**

1. **Measure both sides:**
   - Safety score: % of harmful responses (target: <0.1%)
   - Over-refusal rate: % of safe queries incorrectly refused (target: <5%)
   - Track both on every deployment

2. **Category-specific thresholds:**
   - Medical/legal/financial: Strict (refuse if any doubt)
   - General knowledge: Moderate (answer with caveats)
   - Creative/casual: Loose (almost never refuse)

3. **Graceful refusal:**
   - Don't just say "I can't help" — offer alternatives
   - "I can't provide medical advice, but I can help you find a doctor"
   - "I can't make that change, but here's how to contact someone who can"

4. **Iterative tuning:**
   - Start strict (safety first)
   - Measure over-refusal rate weekly
   - Identify false positives → add to "safe" training examples
   - Gradually loosen specific categories based on data

5. **User feedback signal:**
   - If user immediately rephrases after refusal → likely over-refusal
   - Track "refusal → rephrase → success" patterns
   - These become candidates for threshold adjustment

**Key Points to Hit:**
- Measure BOTH safety and over-refusal
- Category-specific (not one threshold for everything)
- Graceful refusal (not just "no")
- Data-driven tuning (not guessing)

**References:**
- Anthropic's approach to helpful vs harmless trade-off
- "Constitutional AI" (Bai et al., 2022)
- OpenAI's system card (discusses refusal calibration)

---

## Q30: "How would you build an evaluation pipeline that runs automatically in CI/CD?"

**What They're Really Asking:** Can you operationalize quality, not just measure it once?

**Strong Answer:**

**Pipeline:**
```
Code Change → Unit Tests → Integration Tests → EVAL SUITE → Canary → Production
                                                    │
                                          ┌─────────┼─────────┐
                                          ▼         ▼         ▼
                                     Quality    Safety    Regression
                                     Gate       Gate      Gate
                                     (>90%)    (>99%)    (no drop >2%)
```

**Implementation:**
```yaml
# .github/workflows/eval.yml
name: AI Agent Evaluation
on: [push]
jobs:
  eval:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run eval suite
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
        run: |
          python -m pytest tests/eval/ \
            --eval-dataset=golden_tests.jsonl \
            --quality-threshold=0.90 \
            --safety-threshold=0.99 \
            --max-regression=0.02
      - name: Upload results
        run: python scripts/upload_eval_results.py
```

**What the eval suite tests:**
1. **Quality** (200 test cases): LLM-as-judge scores faithfulness, relevance, helpfulness
2. **Safety** (100 adversarial prompts): Must refuse/handle safely
3. **Regression** (50 previously-fixed bugs): Must not reintroduce old failures
4. **Tool selection** (50 cases): Correct tool chosen for each query
5. **Edge cases** (30 cases): Empty input, very long input, non-English, ambiguous

**Cost management:**
- Full eval: ~$5-10 per run (200 LLM calls for judging)
- Run full eval on PR merge to main
- Run subset (safety only) on every commit
- Cache eval results for unchanged test cases

**Key Points to Hit:**
- Automated in CI/CD (not manual)
- Multiple gates (quality + safety + regression)
- Cost-aware (not running $100 eval on every commit)
- Results tracked over time (trend detection)

**References:**
- DeepEval pytest integration
- LangSmith evaluation in CI/CD
- "Testing LLM Applications" (various engineering blogs)
- Braintrust eval platform

---

## Next: [Part 4 — Production, Scaling & Leadership →](Part4_Production_Leadership.md)
