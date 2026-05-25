# Part 1: Agent Architecture & System Design (Q1-Q10)

---

## Q1: "Design an AI agent system that handles customer support for our product."

**What They're Really Asking:** Can you architect a complete agent system from scratch? Do you think about reliability, scalability, and user experience?

**Strong Answer:**

I'd design a multi-layer architecture:

1. **API Gateway Layer**: Rate limiting, authentication, session management
2. **Orchestration Layer**: LangGraph-based state machine with:
   - Intent classification (route to specialist)
   - ReAct loop for tool use
   - Human-in-the-loop for destructive actions
3. **Knowledge Layer**: Hybrid RAG (vector + BM25) with reranking
4. **Tool Layer**: Account APIs, billing systems, knowledge base search
5. **Safety Layer**: Input guardrails (injection detection), output guardrails (hallucination check, PII scan)
6. **Observability**: LangSmith/Phoenix for tracing, custom metrics for quality

Key design decisions:
- **Checkpointing** (Postgres) for conversation persistence
- **Model tiering**: Haiku for classification, Sonnet for generation
- **Graceful degradation**: If LLM fails → cached responses → human handoff
- **Streaming**: SSE for token-by-token delivery to reduce perceived latency

**Key Points to Hit:**
- Multi-layer architecture (not just "call an LLM")
- Safety at every layer
- Fallback strategy
- Cost awareness
- Observability from day one

**References:**
- Anthropic's "Building Effective Agents" guide
- LangGraph documentation (StateGraph, checkpointing)
- "Attention Is All You Need" (Vaswani et al., 2017) for Transformer fundamentals

**Follow-Up:** "How would you handle 10x traffic spike?" → Auto-scaling + response caching + model tiering

---

## Q2: "Explain the difference between ReAct, Plan-and-Execute, and Multi-Agent architectures. When would you use each?"

**What They're Really Asking:** Do you understand agent design patterns deeply, not just one approach?

**Strong Answer:**

| Pattern | How It Works | Best For | Weakness |
|---------|-------------|----------|----------|
| **ReAct** | Think → Act → Observe → loop | Simple tool-use, single-domain | Gets stuck in loops, no upfront planning |
| **Plan-and-Execute** | Create full plan → execute steps → replan if needed | Complex multi-step tasks | Slower (planning overhead), may over-plan |
| **Multi-Agent (Supervisor)** | Router delegates to specialist agents | Multi-domain (billing + tech + creative) | Routing errors cascade, more complex |

**When I'd use each:**
- **ReAct**: Customer asks "What's my subscription?" → single tool call, simple
- **Plan-and-Execute**: "Compare my plan with Enterprise, check if I'd save money, and switch if so" → needs 4-5 steps in sequence
- **Multi-Agent**: Support system handling billing, technical, and account queries → each specialist has different tools and prompts

In practice, I often **combine** them: a Supervisor routes to specialist agents, each of which uses ReAct internally.

**Key Points to Hit:**
- Clear trade-offs for each
- Real examples of when to use which
- Mention that they can be combined
- Acknowledge limitations of each

**References:**
- "ReAct: Synergizing Reasoning and Acting in Language Models" (Yao et al., 2022)
- "Plan-and-Solve Prompting" (Wang et al., 2023)
- LangGraph multi-agent patterns documentation
- CrewAI and AutoGen for multi-agent frameworks

---

## Q3: "How does tool calling / function calling work under the hood? How does the model decide which tool to use?"

**What They're Really Asking:** Do you understand the mechanics, not just the API?

**Strong Answer:**

Tool calling works through **constrained generation** with special tokens:

1. **Training**: Models are fine-tuned on datasets of (query, tool_call, result, response) sequences. They learn when tool use is appropriate and how to format arguments.

2. **At inference**: The model's output is parsed for tool call tokens. When the model generates a tool call:
   - It outputs a structured JSON with tool name + arguments
   - Generation stops (stop_reason = "tool_use")
   - Your code executes the tool
   - The result is appended to the conversation
   - Model continues generating with the tool result in context

3. **Selection mechanism**: The model selects tools based on:
   - **Semantic match** between query intent and tool description
   - **Parameter availability** — can it extract required args from context?
   - **Training signal** — it learned from examples when each tool is appropriate

4. **Under the hood** (for open models like Llama):
   - Special tokens: `<|tool_call|>`, `<|tool_result|>`
   - The model is trained to output valid JSON matching the tool schema
   - Constrained decoding can enforce schema compliance

**Key Points to Hit:**
- It's learned behavior from fine-tuning, not hardcoded rules
- The model generates structured output (JSON), not arbitrary text
- Tool descriptions are critical (they're in the prompt)
- Parallel vs sequential tool calls

**References:**
- OpenAI Function Calling documentation
- Anthropic Tool Use documentation
- "Toolformer: Language Models Can Teach Themselves to Use Tools" (Schick et al., 2023)
- Gorilla LLM (UC Berkeley) — specialized for API calling

---

## Q4: "How would you implement memory for a long-running AI agent that needs to remember context across sessions?"

**What They're Really Asking:** Do you understand the memory problem and its solutions beyond just "stuff everything in the context window"?

**Strong Answer:**

I implement a **multi-tier memory system**:

**Tier 1: Working Memory (Context Window)**
- Current conversation messages
- Limited by token budget (~200K for Claude)
- Managed with sliding window + summarization

**Tier 2: Session Memory (Checkpointing)**
- Full conversation state persisted in Postgres
- LangGraph checkpointer handles this automatically
- Survives server restarts, enables human-in-the-loop

**Tier 3: Long-Term Memory (Vector Store)**
- Key facts about the user stored as embeddings
- Retrieved by semantic similarity at conversation start
- Example: "User prefers concise answers", "User is on Pro plan", "User had billing issue last month"

**Tier 4: Episodic Memory (Summaries)**
- Compressed summaries of past conversations
- Enables "Last time we spoke, you asked about X"
- Stored in structured DB with timestamps

**Implementation:**
```
On conversation start:
  1. Load long-term memory (vector search for user facts)
  2. Load recent episodic memory (last 3 conversation summaries)
  3. Inject as system context

During conversation:
  4. Checkpointer saves state after each turn

On conversation end:
  5. Extract key facts → store in long-term memory
  6. Generate summary → store as episodic memory
```

**Key Points to Hit:**
- Multiple memory types (not just one)
- Concrete implementation (not just theory)
- How to manage token budget
- When to summarize vs store verbatim

**References:**
- LangGraph persistence documentation
- MemGPT paper (Packer et al., 2023) — "Operating system for LLMs"
- Zep (open-source memory layer for agents)
- "Generative Agents: Interactive Simulacra" (Park et al., 2023)

---

## Q5: "You're building an agent that needs to call 15+ different tools. How do you ensure it picks the right one?"

**What They're Really Asking:** Can you handle tool selection at scale? Do you know the failure modes?

**Strong Answer:**

With 15+ tools, naive approaches fail (model gets confused, picks wrong tool). My strategies:

**1. Hierarchical Tool Organization**
```
Instead of 15 flat tools, organize into categories:
  Router → "billing" → [get_invoice, process_refund, update_payment]
  Router → "account" → [get_profile, update_email, reset_password]
  Router → "technical" → [search_docs, check_status, create_ticket]
```
First classify intent, then present only relevant tools (3-5 per category).

**2. Tool Description Engineering**
- Be specific: "Get user's active subscription plan, billing cycle, and next renewal date" not "Get user info"
- Include when NOT to use: "Do NOT use for password resets — use reset_password instead"
- Include examples in description

**3. Two-Stage Selection**
- Stage 1: LLM selects tool (with descriptions)
- Stage 2: Validate selection (does the tool make sense for this query?)
- If validation fails, re-prompt with clarification

**4. Semantic Tool Routing**
- Embed all tool descriptions
- At query time, find top-3 most similar tools by embedding distance
- Only present those 3 tools to the model (reduces confusion)

**5. Evaluation**
- Maintain a test suite of 100+ (query → expected_tool) pairs
- Run in CI/CD — alert if tool selection accuracy drops below 95%

**Key Points to Hit:**
- Don't present all 15 tools at once
- Hierarchical routing
- Description quality matters enormously
- Test and measure tool selection accuracy

**References:**
- Anthropic's tool use best practices
- "ToolBench: An Open Platform for Evaluating LLMs as Tool Agents" (Qin et al., 2023)
- Semantic Kernel (Microsoft) — tool planning patterns

---

## Q6: "How do you handle streaming responses in an AI agent that also makes tool calls?"

**What They're Really Asking:** Do you understand the UX complexity of streaming + tool use?

**Strong Answer:**

This is tricky because tool calls interrupt the stream. My approach:

**Event-Based Streaming Protocol:**
```
Client ← Server (SSE/WebSocket):

1. {"type": "thinking", "content": "Let me check your account..."}
2. {"type": "tool_start", "tool": "get_subscription", "status": "executing"}
3. {"type": "tool_end", "tool": "get_subscription", "duration_ms": 120}
4. {"type": "token", "content": "Your"}
5. {"type": "token", "content": " subscription"}
6. {"type": "token", "content": " is"}
7. {"type": "token", "content": " active."}
8. {"type": "complete", "message_id": "msg-123"}
```

**Implementation:**
- Use Server-Sent Events (SSE) for the stream
- When model emits a tool call → send `tool_start` event to client
- Execute tool server-side (don't wait for client)
- Send `tool_end` with result summary
- Resume streaming the final response

**UX Considerations:**
- Show "Checking your account..." while tool executes (reduces perceived wait)
- Show tool execution as a collapsible step (transparency)
- Allow user to cancel mid-stream (AbortController)
- If tool takes >3s, show progress indicator

**Key Points to Hit:**
- Event types beyond just text tokens
- UX for tool execution visibility
- Cancellation support
- Error handling mid-stream

**References:**
- Anthropic streaming documentation
- Vercel AI SDK (handles streaming + tool calls elegantly)
- Server-Sent Events specification (W3C)

---

## Q7: "What's the difference between LangChain, LangGraph, LlamaIndex, and when would you use each?"

**What They're Really Asking:** Do you have breadth across the ecosystem and can you make informed technology choices?

**Strong Answer:**

| Framework | Core Strength | Use When |
|-----------|--------------|----------|
| **LangChain Core** | Composable chains (LCEL), unified model interface | Simple chains, prompt templates, output parsing |
| **LangGraph** | Stateful graph-based agent orchestration | Multi-step agents, cycles, human-in-the-loop, persistence |
| **LlamaIndex** | Data indexing and retrieval (RAG) | Document QA, complex retrieval strategies, knowledge bases |
| **Semantic Kernel** | .NET/Python SDK for AI orchestration | Microsoft ecosystem, enterprise C# apps |
| **CrewAI** | Multi-agent role-based collaboration | When agents need distinct personas/roles |

**My decision framework:**
- RAG-heavy app → **LlamaIndex** (best indexing, 10+ retrieval strategies)
- Multi-step agent with state → **LangGraph** (checkpointing, human-in-the-loop)
- Simple LLM chain → **LangChain Core** (LCEL is clean and composable)
- Both RAG + agents → **LangGraph + LlamaIndex retriever** (combine strengths)

**What I'd avoid:**
- LangChain's legacy AgentExecutor (replaced by LangGraph)
- Building from scratch when a framework handles 80% of the work
- Using a framework for a simple API call (over-engineering)

**Key Points to Hit:**
- Clear differentiation (not "they're all the same")
- When to combine them
- What's deprecated/legacy
- When NOT to use a framework

**References:**
- LangGraph documentation (StateGraph, checkpointing)
- LlamaIndex Workflows documentation
- Harrison Chase's blog posts on LangGraph design decisions
- Jerry Liu's posts on LlamaIndex philosophy

---

## Q8: "How would you implement human-in-the-loop for an AI agent that can take destructive actions?"

**What They're Really Asking:** Do you understand the safety implications of autonomous agents and how to add human oversight?

**Strong Answer:**

**Architecture:**
```
Agent proposes action → Checkpoint state → Pause execution →
Notify human → Human approves/rejects → Resume or redirect
```

**Implementation with LangGraph:**
```python
graph.compile(
    checkpointer=PostgresSaver(...),
    interrupt_before=["execute_action"],  # Pause before this node
)
```

**Approval Flow:**
1. Agent reaches "execute_action" node → graph pauses
2. State is saved to Postgres (survives server restarts)
3. Webhook/notification sent to human reviewer (Slack, email, UI)
4. Human reviews: sees proposed action, context, reasoning
5. Human approves → `graph.invoke(None, config)` resumes
6. Human rejects → `graph.update_state(config, {"rejected": True})` redirects

**What requires approval (risk-based):**
- **Auto-approve**: Search docs, check status, answer FAQ
- **Require approval**: Cancel subscription, process refund, delete account
- **Always human**: Legal advice, financial decisions, account deletion

**Timeout handling:**
- If no human response in 5 minutes → send reminder
- If no response in 30 minutes → auto-escalate to manager
- If no response in 2 hours → notify user of delay

**Key Points to Hit:**
- Risk-based categorization (not everything needs approval)
- Persistence (survives restarts)
- Timeout/escalation
- User communication during wait

**References:**
- LangGraph interrupt_before documentation
- "Practices for Governing Agentic AI Systems" (OpenAI, 2024)
- Anthropic's "Building Effective Agents" — human oversight section

---

## Q9: "How do you handle context window limitations when your agent needs access to large amounts of data?"

**What They're Really Asking:** Do you understand the practical constraints and solutions beyond "just use a bigger model"?

**Strong Answer:**

Context windows are finite (even 200K tokens ≈ 150K words). My strategies:

**1. RAG (Retrieve, Don't Stuff)**
- Don't put all data in context — retrieve only what's relevant
- Top-5 chunks (each ~500 tokens) = 2,500 tokens vs 200K for everything
- 99% cost reduction + better accuracy (less noise)

**2. Hierarchical Summarization**
- Summarize large documents into 1-page summaries
- Search summaries first, then drill into full text if needed
- Tree structure: sentence → paragraph → section → document

**3. Sliding Window + Summary**
- Keep last N messages in full
- Summarize older messages into a compressed context
- "Previous conversation summary: User asked about billing, we resolved a duplicate charge"

**4. Prompt Caching**
- Cache the static prefix (system prompt + reference docs)
- Only pay for new query tokens on each request
- 90% cost reduction for repeated context

**5. Map-Reduce for Large Documents**
- Split document into chunks
- Process each chunk independently (map)
- Combine results (reduce)
- Example: "Summarize this 100-page report" → summarize each page → combine summaries

**6. Smart Truncation**
- Priority-based: Keep system prompt + recent messages + most relevant retrieved docs
- Never truncate the user's latest message
- Truncate from the middle (LLMs attend better to beginning and end)

**Key Points to Hit:**
- RAG as primary solution (not bigger context)
- Cost implications of large contexts
- "Lost in the middle" problem
- Multiple strategies for different scenarios

**References:**
- "Lost in the Middle" (Liu et al., 2023) — LLMs struggle with middle of long contexts
- Anthropic prompt caching documentation
- LlamaIndex hierarchical retrieval patterns

---

## Q10: "Walk me through how you'd debug a conversation where the agent gave a completely wrong answer."

**What They're Really Asking:** Do you have a systematic debugging methodology for non-deterministic systems?

**Strong Answer:**

**Step-by-step debugging process:**

**1. Reproduce** — Find the exact trace (by conversation_id)

**2. Check the trace** (in LangSmith/Phoenix):
```
Was the intent classified correctly?
  YES → move to step 3
  NO → Fix classification prompt/examples

Were relevant documents retrieved?
  Check retrieval scores. If top score < 0.7 → retrieval failure
  Fix: Better chunking, add missing docs, improve embeddings

Was the right context passed to the LLM?
  Check for truncation (token limit hit?)
  Check for wrong documents (metadata filter issue?)

Did the model hallucinate despite good context?
  Context says "14 days" but model said "30 days"
  Fix: Stronger grounding instruction, lower temperature, add citation requirement

Did a tool return wrong data?
  Check tool input/output in trace
  Fix: Tool implementation bug

Did a guardrail incorrectly modify the response?
  Check guardrail trace
  Fix: Adjust threshold or add exception
```

**3. Categorize the failure:**
- Retrieval failure (40% of issues)
- Prompt/instruction issue (25%)
- Model hallucination (20%)
- Tool error (10%)
- Guardrail over-trigger (5%)

**4. Fix and prevent:**
- Add this case to evaluation dataset
- Write regression test
- Deploy fix with canary (5% traffic)
- Monitor for 24h before full rollout

**Key Points to Hit:**
- Systematic (not random guessing)
- Use observability tools (traces, not just logs)
- Categorize failure types
- Prevent recurrence (add to eval suite)

**References:**
- LangSmith trace debugging documentation
- Arize Phoenix for trace analysis
- "Debugging LLM Applications" (various blog posts from LangChain, Anthropic)

---

## Next: [Part 2 — RAG, Knowledge Systems & Retrieval →](Part2_RAG_Knowledge.md)
