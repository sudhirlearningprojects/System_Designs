# 4. Agents & Tools

## Theory: Agents in LlamaIndex

LlamaIndex agents combine **reasoning** (LLM) with **action** (tools) to solve complex tasks that require multiple steps, data retrieval, and external interactions.

### Agent Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    LLAMAINDEX AGENT                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  User Query → Agent Loop:                                    │
│    1. LLM reasons about what to do                          │
│    2. LLM selects tool + arguments                          │
│    3. Tool executes, returns result                         │
│    4. LLM incorporates result                               │
│    5. Repeat or generate final answer                       │
│                                                              │
│  Tools available:                                            │
│  ├── QueryEngineTool (RAG over your data)                   │
│  ├── FunctionTool (any Python function)                     │
│  ├── RetrieverTool (just retrieval, no synthesis)           │
│  └── Custom tools (APIs, databases, actions)                │
└─────────────────────────────────────────────────────────────┘
```

### Agent Types

| Agent | How It Decides | Best For |
|-------|---------------|----------|
| **ReAct** | Interleaved reasoning + action | General purpose, transparent |
| **Function Calling** | Native tool calling (Claude/GPT) | Production, reliable |
| **Structured Planning** | Plan first, then execute | Complex multi-step tasks |

---

## Tools

```python
from llama_index.core.tools import FunctionTool, QueryEngineTool
from llama_index.core import VectorStoreIndex

# Function Tool (wrap any Python function)
def get_subscription(user_id: str) -> dict:
    """Get user's current subscription details including plan, status, and renewal date."""
    return {"plan": "Pro", "status": "active", "renewal": "2024-02-15", "price": "$19.99/mo"}

def cancel_subscription(user_id: str, reason: str) -> dict:
    """Cancel a user's subscription. Requires user_id and cancellation reason."""
    return {"status": "cancelled", "effective_date": "2024-02-15"}

subscription_tool = FunctionTool.from_defaults(fn=get_subscription)
cancel_tool = FunctionTool.from_defaults(fn=cancel_subscription)

# Query Engine Tool (RAG over documents)
billing_engine = billing_index.as_query_engine(similarity_top_k=3)
billing_tool = QueryEngineTool.from_defaults(
    query_engine=billing_engine,
    name="billing_knowledge",
    description="Search billing documentation for policies, pricing, and procedures. "
                "Use for questions about refunds, charges, plan comparisons.",
)

technical_engine = technical_index.as_query_engine(similarity_top_k=5)
technical_tool = QueryEngineTool.from_defaults(
    query_engine=technical_engine,
    name="technical_docs",
    description="Search technical documentation for product features, troubleshooting, "
                "and how-to guides. Use for 'how do I...' questions.",
)
```

---

## ReAct Agent

```python
from llama_index.core.agent import ReActAgent
from llama_index.llms.anthropic import Anthropic

# Create agent with tools
agent = ReActAgent.from_tools(
    tools=[subscription_tool, cancel_tool, billing_tool, technical_tool],
    llm=Anthropic(model="claude-sonnet-4-20250514"),
    verbose=True,  # See reasoning steps
    max_iterations=10,
    system_prompt="""You are a helpful customer support agent for Adobe.
    
Rules:
- Always check subscription status before making changes
- Confirm with user before cancelling
- If unsure, search documentation first
- Be empathetic and professional""",
)

# Chat (maintains conversation history)
response = agent.chat("I want to cancel my subscription")
print(response.response)

# See reasoning
for step in response.sources:
    print(f"Tool: {step.tool_name}, Input: {step.raw_input}")
```

### Function Calling Agent (Production)

```python
from llama_index.agent.openai import OpenAIAgent  # Works with Claude too via adapter
from llama_index.core.agent import FunctionCallingAgent

# More reliable than ReAct for production
agent = FunctionCallingAgent.from_tools(
    tools=[subscription_tool, cancel_tool, billing_tool, technical_tool],
    llm=Anthropic(model="claude-sonnet-4-20250514"),
    verbose=True,
    system_prompt="You are a helpful support agent.",
)

response = agent.chat("What plan am I on and how much does it cost?")
```

---

## Multi-Tool Agent with Memory

```python
from llama_index.core.memory import ChatMemoryBuffer

# Agent with conversation memory
memory = ChatMemoryBuffer.from_defaults(token_limit=4096)

agent = ReActAgent.from_tools(
    tools=all_tools,
    llm=Anthropic(model="claude-sonnet-4-20250514"),
    memory=memory,
    verbose=True,
)

# Multi-turn conversation (agent remembers context)
agent.chat("My name is Alice and I'm on the Pro plan")
agent.chat("Can you check my renewal date?")  # Remembers user context
agent.chat("Actually, I'd like to cancel")     # Remembers previous context

# Reset memory
agent.reset()
```

---

## Agent with Query Planning

```python
from llama_index.core.agent import StructuredPlannerAgent

# Agent that creates a plan before executing
agent = StructuredPlannerAgent.from_tools(
    tools=all_tools,
    llm=Anthropic(model="claude-sonnet-4-20250514"),
)

# Complex query → agent plans steps first
response = agent.chat(
    "Compare my current Pro plan with the Enterprise plan, "
    "and tell me if upgrading would save money given my usage"
)
# Agent plans:
# 1. Get current subscription (subscription_tool)
# 2. Search Enterprise plan details (billing_tool)
# 3. Compare and recommend
```

---

## Next: [LlamaIndex Workflows →](05_Workflows.md)
