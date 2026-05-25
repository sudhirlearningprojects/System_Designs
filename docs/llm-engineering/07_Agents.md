# 7. AI Agents & Tool Use

> **Full coverage**: See [LangGraph → Advanced Agents](../langchain-langgraph/05_LangGraph_Advanced.md) and [Claude AI → Building Agents](../claude-ai/05_Building_Agents.md) for comprehensive agent implementation guides.

## Theory: What Makes an Agent?

```
LLM alone: Input text → Output text (one-shot, no actions)

Agent = LLM + Tools + Memory + Planning

  LLM: The "brain" — reasons about what to do
  Tools: The "hands" — interact with external world
  Memory: The "notebook" — remember past interactions
  Planning: The "strategy" — decompose complex tasks
```

### Agent Architectures

```
1. REACT (Reasoning + Acting)
   Loop: Think → Act → Observe → Think → Act → ... → Answer
   Best for: Simple tool-use tasks

2. PLAN-AND-EXECUTE
   Plan all steps → Execute each → Replan if needed
   Best for: Complex multi-step tasks

3. MULTI-AGENT (Supervisor)
   Router → Specialist agents → Synthesize
   Best for: Multi-domain applications

4. HIERARCHICAL
   Manager agents → Worker agents → Sub-workers
   Best for: Large-scale enterprise systems
```

### Tool Use Theory

```
The model doesnt EXECUTE tools — it DECIDES which tool to call.
Your code executes the tool and returns the result.

Flow:
  1. User: "Whats the weather in Tokyo?"
  2. LLM: tool_call(get_weather, city="Tokyo")  ← LLM DECIDES
  3. Your code: result = weather_api.get("Tokyo")  ← YOU EXECUTE
  4. LLM: "The weather in Tokyo is 72°F and sunny."  ← LLM SYNTHESIZES

This separation is critical for:
  - Security (you control what actually executes)
  - Reliability (you handle errors, retries, timeouts)
  - Auditability (you log every tool execution)
```

### Memory Systems

```
SHORT-TERM: Current conversation (context window)
  → Limited by token budget
  → Solution: Summarize old messages

LONG-TERM: Facts about user across sessions
  → Stored in vector DB or key-value store
  → Retrieved by semantic similarity

EPISODIC: Past conversation summaries
  → "Last time you asked about X, we resolved it by Y"
  → Enables continuity across sessions

WORKING: Intermediate reasoning state
  → Scratchpad for multi-step planning
  → Discarded after task completion
```

---

## Next: [Safety & Alignment →](08_Safety.md)
