# 4. LangGraph Fundamentals

## Theory: Why Graphs for AI Agents?

### The Problem with Linear Chains

Traditional LangChain chains are **DAGs** (Directed Acyclic Graphs) — they flow in one direction. But real agents need:

```
Linear Chain:  A → B → C → D (done)

Agent Reality: A → B → C → B → C → B → D (loops!)
               Think → Act → Observe → Think again → Act again...
```

**Agents are fundamentally cyclic** — they think, act, observe, and decide whether to continue or stop. This requires **graphs with cycles**, not linear chains.

### LangGraph vs AgentExecutor

| Feature | AgentExecutor (Legacy) | LangGraph |
|---------|----------------------|------------|
| Cycles | Limited (while loop) | First-class (graph edges) |
| State | Messages only | Custom typed state |
| Control flow | Black box | Explicit conditional edges |
| Human-in-the-loop | Not supported | Built-in (interrupt + resume) |
| Persistence | Not supported | Checkpointing (Postgres, SQLite) |
| Streaming | Basic | Granular (node-level, token-level) |
| Multi-agent | Hacky | Subgraphs, supervisor pattern |
| Debugging | Hard | Time-travel, state inspection |
| Production | Fragile | Retry, fallback, error handling |

### Graph Theory for Agents

```
A LangGraph agent is a FINITE STATE MACHINE:

- States: The typed data flowing through the graph
- Transitions: Edges (normal or conditional)
- Nodes: Functions that transform state
- Cycles: Allow iterative reasoning (ReAct loop)
- Terminal: END node (agent is done)

Key insight: By making the execution graph EXPLICIT,
you gain control, debuggability, and reliability.
```

### Mental Model

```
┌───────────────────────────────────────────────────────────┐
│  Think of LangGraph like a FLOWCHART:                        │
│                                                             │
│  START → [Classify Intent]                                  │
│              │                                               │
│         ┌────┼────┐                                          │
│         ▼    ▼    ▼                                          │
│      [Billing] [Tech] [General]                             │
│         │    │    │                                          │
│         ▼    ▼    ▼                                          │
│      [Need more info?] ── YES ──► [Ask Clarification] ─┐    │
│         │                                            │    │
│         NO                                           │    │
│         ▼                                            │    │
│      [Generate Response] ◄────────────────────────┘    │
│         │                                                 │
│         ▼                                                 │
│       END                                                  │
└───────────────────────────────────────────────────────────┘
```

### State Reducers — Theory

When multiple nodes update the same state field, **reducers** define how updates are merged:

| Reducer | Behavior | Use Case |
|---------|----------|----------|
| `add_messages` | Append new messages (deduplicate by ID) | Conversation history |
| `operator.add` | Sum values | Token counters, costs |
| Last-write-wins | Overwrite with latest value | Current intent, status |
| Custom function | Any merge logic | Keep last N items, merge dicts |

**Why reducers matter**: In a graph with cycles, a node might be called multiple times. Without reducers, you'd lose previous state. With `add_messages`, each LLM response is appended, building up the full conversation.

### Checkpointing — Theory

Checkpointing saves the graph's state after every node execution:

```
Step 1: [START] → state saved → {messages: [user_msg]}
Step 2: [agent] → state saved → {messages: [user_msg, ai_msg_with_tool_call]}
Step 3: [tools] → state saved → {messages: [..., tool_result]}
Step 4: [agent] → state saved → {messages: [..., final_response]}
Step 5: [END]
```

**This enables:**
- **Human-in-the-loop**: Pause at step 3, wait for approval, resume
- **Time-travel debugging**: Inspect state at any step
- **Fault tolerance**: If crash at step 4, resume from step 3
- **Multi-turn conversations**: Each invoke continues from last checkpoint

---

## Why LangGraph?

LangChain agents (AgentExecutor) are limited:
- No cycles (can't loop back)
- No persistent state across interactions
- No human-in-the-loop
- Hard to control execution flow

**LangGraph** solves this with a graph-based orchestration framework:
- **Cycles**: Agents can loop (think → act → observe → think again)
- **State**: Persistent, typed state across nodes
- **Checkpointing**: Save/resume at any point (human-in-the-loop)
- **Streaming**: First-class streaming of state updates
- **Subgraphs**: Compose complex multi-agent systems

---

## Core Concepts

```
┌─────────────────────────────────────────────────────────────┐
│                    LANGGRAPH CONCEPTS                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  STATE: TypedDict that flows through the graph               │
│  ├── Messages, context, intermediate results                 │
│  └── Reducers define how state is updated                    │
│                                                              │
│  NODES: Functions that read/write state                      │
│  ├── Each node receives state, returns state updates         │
│  └── Can call LLMs, tools, APIs, other graphs               │
│                                                              │
│  EDGES: Connections between nodes                            │
│  ├── Normal edges (always follow)                            │
│  ├── Conditional edges (route based on state)                │
│  └── Entry/exit points (START, END)                          │
│                                                              │
│  CHECKPOINTER: Saves state at each step                     │
│  ├── Enables human-in-the-loop (pause/resume)               │
│  ├── Enables time-travel debugging                           │
│  └── Backends: memory, SQLite, Postgres                      │
└─────────────────────────────────────────────────────────────┘
```

---

## Basic ReAct Agent

```python
from typing import Annotated, TypedDict
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode, tools_condition

# 1. Define State
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]  # Reducer: append messages

# 2. Define Tools
from langchain_core.tools import tool

@tool
def get_weather(city: str) -> str:
    """Get current weather for a city."""
    # Real implementation would call weather API
    return f"Weather in {city}: 72°F, sunny"

@tool
def search_docs(query: str) -> str:
    """Search internal documentation."""
    # Real implementation would search vector DB
    return f"Found: Documentation about '{query}'..."

tools = [get_weather, search_docs]

# 3. Define Model
model = ChatAnthropic(model="claude-sonnet-4-20250514").bind_tools(tools)

# 4. Define Nodes
def call_model(state: AgentState) -> dict:
    """Node that calls the LLM."""
    response = model.invoke(state["messages"])
    return {"messages": [response]}

# 5. Build Graph
graph = StateGraph(AgentState)

# Add nodes
graph.add_node("agent", call_model)
graph.add_node("tools", ToolNode(tools))

# Add edges
graph.add_edge(START, "agent")
graph.add_conditional_edges(
    "agent",
    tools_condition,  # Routes to "tools" if tool call, else END
)
graph.add_edge("tools", "agent")  # After tools, go back to agent (cycle!)

# 6. Compile
app = graph.compile()

# 7. Run
result = app.invoke({"messages": [HumanMessage(content="What's the weather in Tokyo?")]})
print(result["messages"][-1].content)
```

### Execution Flow

```
START → agent (LLM decides to call tool)
      → tools (execute get_weather)
      → agent (LLM generates final response with tool result)
      → END
```

---

## State Management

### Custom State with Multiple Fields

```python
from typing import Annotated, TypedDict, Optional
from langgraph.graph.message import add_messages
from operator import add

class AgentState(TypedDict):
    # Messages (append-only via reducer)
    messages: Annotated[list[BaseMessage], add_messages]
    
    # Custom fields
    user_id: str
    intent: Optional[str]
    context: list[str]
    tool_calls_count: Annotated[int, add]  # Reducer: sum
    should_escalate: bool
    
    # Accumulated results
    retrieved_docs: list[dict]
    actions_taken: list[str]

# Nodes update specific fields
def classify_intent(state: AgentState) -> dict:
    """Only update the fields you need."""
    query = state["messages"][-1].content
    intent = classify(query)  # Your classification logic
    return {"intent": intent}

def check_escalation(state: AgentState) -> dict:
    """Decide if human handoff is needed."""
    if state["tool_calls_count"] > 5 or state["intent"] == "complaint":
        return {"should_escalate": True}
    return {"should_escalate": False}
```

### Reducers

```python
from typing import Annotated
from operator import add

class State(TypedDict):
    # add_messages: Appends new messages (handles deduplication by ID)
    messages: Annotated[list, add_messages]
    
    # add: Sums integers
    total_tokens: Annotated[int, add]
    
    # Custom reducer: keep last N items
    def keep_last_5(existing: list, new: list) -> list:
        return (existing + new)[-5:]
    
    recent_actions: Annotated[list, keep_last_5]
    
    # No reducer: last write wins (overwrite)
    current_intent: str
```

---

## Conditional Routing

```python
from langgraph.graph import StateGraph, START, END

def route_by_intent(state: AgentState) -> str:
    """Conditional edge function — returns next node name."""
    intent = state.get("intent")
    
    if intent == "billing":
        return "billing_agent"
    elif intent == "technical":
        return "technical_agent"
    elif intent == "escalate":
        return "human_handoff"
    else:
        return "general_agent"

# Build graph with conditional routing
graph = StateGraph(AgentState)

graph.add_node("classifier", classify_intent)
graph.add_node("billing_agent", handle_billing)
graph.add_node("technical_agent", handle_technical)
graph.add_node("general_agent", handle_general)
graph.add_node("human_handoff", escalate_to_human)

graph.add_edge(START, "classifier")
graph.add_conditional_edges(
    "classifier",
    route_by_intent,
    {
        "billing_agent": "billing_agent",
        "technical_agent": "technical_agent",
        "general_agent": "general_agent",
        "human_handoff": "human_handoff",
    }
)
graph.add_edge("billing_agent", END)
graph.add_edge("technical_agent", END)
graph.add_edge("general_agent", END)
graph.add_edge("human_handoff", END)

app = graph.compile()
```

---

## Checkpointing (Persistence)

```python
from langgraph.checkpoint.memory import MemorySaver
from langgraph.checkpoint.sqlite import SqliteSaver
from langgraph.checkpoint.postgres import PostgresSaver

# In-memory (development)
memory = MemorySaver()

# SQLite (single-server production)
checkpointer = SqliteSaver.from_conn_string("checkpoints.db")

# PostgreSQL (multi-server production)
checkpointer = PostgresSaver.from_conn_string("postgresql://user:pass@host/db")

# Compile with checkpointer
app = graph.compile(checkpointer=memory)

# Each conversation gets a thread_id
config = {"configurable": {"thread_id": "conversation-123"}}

# First message
result = app.invoke(
    {"messages": [HumanMessage(content="Hi, I'm Alice")]},
    config=config,
)

# Second message (remembers context!)
result = app.invoke(
    {"messages": [HumanMessage(content="What's my name?")]},
    config=config,
)
# → "Your name is Alice"

# Different thread = different conversation
config2 = {"configurable": {"thread_id": "conversation-456"}}
result = app.invoke(
    {"messages": [HumanMessage(content="What's my name?")]},
    config=config2,
)
# → "I don't know your name yet"
```

---

## Streaming

```python
# Stream state updates (see each node's output as it happens)
async for event in app.astream_events(
    {"messages": [HumanMessage(content="Search for Docker docs")]},
    config=config,
    version="v2",
):
    kind = event["event"]
    
    if kind == "on_chat_model_stream":
        # Token-by-token streaming from LLM
        print(event["data"]["chunk"].content, end="")
    
    elif kind == "on_chain_start":
        print(f"\n--- Starting: {event['name']} ---")
    
    elif kind == "on_tool_start":
        print(f"\n🔧 Tool: {event['name']}({event['data']['input']})")
    
    elif kind == "on_tool_end":
        print(f"\n✅ Result: {event['data']['output'][:100]}")

# Stream state updates (node-level)
for state_update in app.stream(
    {"messages": [HumanMessage(content="Hello")]},
    config=config,
    stream_mode="updates",  # or "values" for full state
):
    node_name = list(state_update.keys())[0]
    print(f"Node '{node_name}' produced: {state_update[node_name]}")
```

---

## Human-in-the-Loop

```python
from langgraph.graph import StateGraph, START, END

class State(TypedDict):
    messages: Annotated[list, add_messages]
    action_approved: Optional[bool]

def propose_action(state: State) -> dict:
    """Agent proposes an action (e.g., cancel subscription)."""
    # LLM decides what action to take
    response = model.invoke(state["messages"])
    return {"messages": [response]}

def execute_action(state: State) -> dict:
    """Execute the approved action."""
    # Only runs if human approved
    return {"messages": [AIMessage(content="Action executed successfully.")]}

def should_continue(state: State) -> str:
    last_msg = state["messages"][-1]
    if last_msg.tool_calls:
        return "request_approval"
    return END

graph = StateGraph(State)
graph.add_node("agent", propose_action)
graph.add_node("execute", execute_action)

graph.add_edge(START, "agent")
graph.add_conditional_edges("agent", should_continue)
graph.add_edge("execute", END)

# Compile with interrupt (pauses before "execute" node)
app = graph.compile(
    checkpointer=MemorySaver(),
    interrupt_before=["execute"],  # Pause here for human approval
)

config = {"configurable": {"thread_id": "thread-1"}}

# Step 1: Agent proposes action (pauses before execute)
result = app.invoke(
    {"messages": [HumanMessage(content="Cancel my subscription")]},
    config=config,
)
print("Agent proposes:", result["messages"][-1].content)

# Step 2: Human reviews and approves
# (In real app, this would be a UI interaction)
# Resume execution
result = app.invoke(None, config=config)  # Continue from checkpoint
print("Result:", result["messages"][-1].content)

# Or reject: update state and re-route
app.update_state(config, {"action_approved": False})
```

---

## Visualization

```python
# Generate Mermaid diagram of the graph
print(app.get_graph().draw_mermaid())

# Or save as PNG
from IPython.display import Image
Image(app.get_graph().draw_mermaid_png())
```

Output:
```mermaid
graph TD
    __start__ --> classifier
    classifier -->|billing| billing_agent
    classifier -->|technical| technical_agent
    classifier -->|general| general_agent
    billing_agent --> __end__
    technical_agent --> __end__
    general_agent --> __end__
```

---

## Next: [LangGraph Advanced Agents →](05_LangGraph_Advanced.md)
