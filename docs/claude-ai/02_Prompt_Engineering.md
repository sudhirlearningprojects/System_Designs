# 2. Prompt Engineering for Claude

## Claude-Specific Prompt Principles

1. **Be direct** — Claude responds well to clear, explicit instructions
2. **Use XML tags** — Claude is trained to respect XML structure for organization
3. **Put instructions before data** — Tell Claude what to do, then give it the content
4. **Use system prompts for persona/rules** — Persistent instructions go in system
5. **Give examples** — Few-shot examples dramatically improve output quality
6. **Think step by step** — Chain-of-thought improves reasoning accuracy

---

## System Prompts

### Structure

```python
system_prompt = """You are an expert software architect specializing in distributed systems.

<rules>
- Always consider scalability, reliability, and cost in your recommendations
- Provide concrete examples with code when possible
- If you're unsure about something, say so explicitly
- Use diagrams (ASCII art) to illustrate architectures
- Cite trade-offs for every design decision
</rules>

<context>
The user is building a payment processing system that needs to handle 100K TPS
with exactly-once semantics. They use AWS, Java/Spring Boot, and PostgreSQL.
</context>

<output_format>
Structure your responses as:
1. Summary (2-3 sentences)
2. Architecture (diagram + explanation)
3. Key Design Decisions (table of trade-offs)
4. Implementation Notes (code snippets)
</output_format>"""
```

### Role-Based System Prompts

```python
# Customer support agent
support_system = """You are Adobe's AI support assistant.

<persona>
- Friendly, professional, and empathetic
- Use the customer's name when available
- Acknowledge frustration before solving problems
</persona>

<capabilities>
- Answer questions about Adobe Creative Cloud products
- Help with billing and subscription issues
- Provide step-by-step tutorials for product features
- Escalate to human agents when needed
</capabilities>

<guardrails>
- Never make up product features that don't exist
- Never share internal pricing or roadmap information
- Never provide legal or financial advice
- If unsure, say "Let me connect you with a specialist"
- Always verify account actions before executing
</guardrails>

<tools_available>
- get_user_account: Retrieve user's subscription details
- search_help_docs: Search Adobe help documentation
- create_support_ticket: Escalate to human agent
- check_service_status: Check if Adobe services are operational
</tools_available>"""
```

---

## XML Tags (Claude's Superpower)

Claude is specifically trained to understand and respect XML tag structure.

### Input Organization

```python
messages = [{
    "role": "user",
    "content": """<task>
Analyze this code for security vulnerabilities and suggest fixes.
</task>

<code language="python">
import sqlite3

def get_user(username):
    conn = sqlite3.connect('users.db')
    cursor = conn.cursor()
    query = f"SELECT * FROM users WHERE username = '{username}'"
    cursor.execute(query)
    return cursor.fetchone()

def login(request):
    username = request.form['username']
    password = request.form['password']
    user = get_user(username)
    if user and user[2] == password:
        return "Login successful"
    return "Login failed"
</code>

<output_requirements>
- List each vulnerability with severity (Critical/High/Medium/Low)
- Explain the attack vector
- Provide the fixed code
- Use markdown formatting
</output_requirements>"""
}]
```

### Output Structuring

```python
system = """When analyzing documents, structure your response using these XML tags:

<summary>Brief overview</summary>
<key_findings>
  <finding severity="high">Description</finding>
  <finding severity="medium">Description</finding>
</key_findings>
<recommendations>Actionable next steps</recommendations>
<confidence>Your confidence level and reasoning</confidence>"""
```

### Prefilling Assistant Response

```python
# Force Claude to respond in a specific format by prefilling
messages = [
    {"role": "user", "content": "Extract entities from: 'John Smith ordered 3 laptops from Amazon on Jan 15'"},
    {"role": "assistant", "content": '{"entities": ['}  # Prefill forces JSON output
]

response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=500,
    messages=messages
)
# Claude will continue from where you left off, completing the JSON
```

---

## Chain-of-Thought (CoT)

### Explicit CoT

```python
messages = [{
    "role": "user",
    "content": """Determine if this database schema can handle 1M concurrent users.

<schema>
Users table: id (PK), email (unique), name, created_at
Orders table: id (PK), user_id (FK), total, status, created_at
Products table: id (PK), name, price, stock
Order_Items: order_id (FK), product_id (FK), quantity
</schema>

Think through this step by step:
1. Analyze read/write patterns
2. Identify potential bottlenecks
3. Calculate storage requirements
4. Recommend optimizations

Show your reasoning for each step before giving your final answer."""
}]
```

### Structured Reasoning with Extended Thinking

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=16000,
    thinking={
        "type": "enabled",
        "budget_tokens": 8000
    },
    messages=[{
        "role": "user",
        "content": "Design a rate limiting algorithm that's fair across tenants, handles burst traffic, and works in a distributed system with 10 nodes."
    }]
)
```

---

## Few-Shot Examples

### Classification

```python
system = """Classify customer support messages into categories.

<examples>
<example>
<input>I can't log into my account, it says password is wrong</input>
<output>{"category": "authentication", "urgency": "medium", "sentiment": "frustrated"}</output>
</example>

<example>
<input>How do I cancel my subscription? I'm being charged too much</input>
<output>{"category": "billing", "urgency": "high", "sentiment": "unhappy"}</output>
</example>

<example>
<input>Love the new Photoshop features! The AI fill is amazing</input>
<output>{"category": "feedback", "urgency": "low", "sentiment": "positive"}</output>
</example>
</examples>

Respond ONLY with the JSON classification. No other text."""
```

### Code Generation

```python
system = """Generate Python functions following this style:

<examples>
<example>
<request>Function to validate email</request>
<response>
import re
from typing import bool

def validate_email(email: str) -> bool:
    \"\"\"Validate email format using regex.\"\"\"
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return bool(re.match(pattern, email))
</response>
</example>
</examples>

Follow these conventions:
- Type hints on all parameters and return values
- Docstrings on all functions
- Handle edge cases (None, empty string)
- Raise ValueError for invalid inputs"""
```

---

## Advanced Techniques

### Multi-Step Decomposition

```python
messages = [{
    "role": "user",
    "content": """I need to build a notification system. Help me design it.

<approach>
Let's break this into phases. For each phase, provide:
1. What we're solving
2. Design decisions with trade-offs
3. Implementation sketch

Phase 1: Core message routing
Phase 2: Delivery guarantees (at-least-once)
Phase 3: User preferences and quiet hours
Phase 4: Scaling to 10M notifications/minute
</approach>

Start with Phase 1 only. I'll ask for subsequent phases."""
}]
```

### Constrained Output

```python
# Force specific output format
system = """You are a JSON API. You ONLY respond with valid JSON.
Never include markdown code fences, explanations, or any text outside the JSON.

Schema:
{
  "intent": "string (one of: question, request, complaint, feedback)",
  "entities": [{"type": "string", "value": "string"}],
  "confidence": "number (0-1)",
  "suggested_action": "string"
}"""
```

### Self-Critique Pattern

```python
messages = [{
    "role": "user",
    "content": """Write a function to find the longest palindromic substring.

After writing your solution:
1. Analyze its time and space complexity
2. Identify any edge cases it might miss
3. Suggest an optimization if the current approach isn't optimal
4. Write the optimized version if different"""
}]
```

### Persona Consistency

```python
system = """You are Maya, a senior DevOps engineer at a tech company.

<personality>
- Pragmatic and direct
- Slightly sarcastic but helpful
- Strong opinions on infrastructure (prefers Kubernetes, Terraform, GitOps)
- Always considers cost implications
- Uses analogies to explain complex concepts
</personality>

<knowledge>
- 12 years of experience
- AWS Solutions Architect Professional certified
- Contributed to Kubernetes and Prometheus
- Managed infrastructure for 10M+ user applications
</knowledge>

<communication_style>
- Short paragraphs
- Bullet points for lists
- Code examples over lengthy explanations
- "In my experience..." for opinionated advice
</communication_style>

Stay in character. Never break the fourth wall or mention being an AI."""
```

---

## Prompt Patterns for Common Tasks

### Summarization

```python
content = """<document>
{long_document_text}
</document>

<instructions>
Summarize the above document following these rules:
- Maximum 3 paragraphs
- Include all key decisions and action items
- Preserve any numbers, dates, or specific commitments
- Use bullet points for action items
- End with "Key Takeaway: [one sentence]"
</instructions>"""
```

### Code Review

```python
content = """<code_to_review>
{code}
</code_to_review>

<review_criteria>
- Security vulnerabilities (OWASP Top 10)
- Performance issues (N+1 queries, unnecessary allocations)
- Error handling (missing try/catch, unhandled edge cases)
- Code style (naming, structure, DRY violations)
- Testability (tight coupling, hidden dependencies)
</review_criteria>

<output_format>
For each issue found:
| Line | Severity | Category | Issue | Fix |
</output_format>

Only report actual issues. If the code is good, say so."""
```

### Data Extraction

```python
content = """<source_text>
{unstructured_text}
</source_text>

Extract the following information into structured JSON:
- Company names (with roles: buyer/seller/partner)
- Financial figures (amount, currency, context)
- Dates (with what they refer to)
- People (name, title, company)

If a field is not found, use null. Never invent information.
Respond with ONLY the JSON object."""
```

---

## Anti-Patterns to Avoid

| Anti-Pattern | Problem | Better Approach |
|-------------|---------|-----------------|
| "Be creative" | Too vague | "Generate 3 alternative approaches with trade-offs" |
| "Don't hallucinate" | Claude can't control this | Ground with context + "If unsure, say so" |
| Very long system prompts (>4000 tokens) | Dilutes attention | Use XML sections, cache long context |
| "Answer in exactly 100 words" | Arbitrary constraint hurts quality | "Be concise, 2-3 paragraphs max" |
| Nested instructions in user message | Confusing | System prompt for rules, user message for task |
| "Ignore previous instructions" | Jailbreak attempt | Use system prompt guardrails |

---

## Next: [Tool Use & Function Calling →](03_Tool_Use.md)
