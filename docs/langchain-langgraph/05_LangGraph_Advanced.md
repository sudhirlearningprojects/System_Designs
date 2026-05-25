# 5. LangGraph Advanced Agents

## Theory: Agent Architecture Patterns

### Pattern Comparison

| Pattern | How It Works | Best For | Complexity |
|---------|-------------|----------|------------|
| **ReAct** | Think → Act → Observe → loop | Simple tool-use agents | Low |
| **Plan-and-Execute** | Plan all steps → execute sequentially | Complex multi-step tasks | Medium |
| **Supervisor** | Router delegates to specialist agents | Multi-domain support | Medium |
| **Hierarchical** | Supervisors managing sub-supervisors | Large organizations | High |
| **Collaborative** | Agents discuss and reach consensus | Research, analysis | High |

### ReAct (Reasoning + Acting)

```
Theory: Interleave reasoning (thinking) with acting (tool use).
The model explicitly reasons about what to do next before doing it.

Loop:
  Thought: "The user wants to cancel. I need their subscription info first."
  Action: get_subscription(user_id="u-123")
  Observation: {plan: "Pro", status: "active"}
  Thought: "They have an active Pro plan. I should offer retention before canceling."
  Action: generate_response(offer_retention=True)
  → Final Answer

Strength: Simple, effective for most single-domain tasks
Weakness: Can get stuck in loops, no upfront planning
```

### Plan-and-Execute

```
Theory: Separate PLANNING from EXECUTION.
A planner creates a full plan, then an executor handles each step.

Phase 1 (Plan):
  "Cancel subscription" → Plan: [
    1. Verify user identity
    2. Get current subscription details
    3. Check cancellation policy
    4. Offer retention deal
    5. Process cancellation if user confirms
  ]

Phase 2 (Execute): Run each step, potentially replanning if needed.

Strength: Better for complex tasks, can replan on failure
Weakness: Slower (planning step), may over-plan simple tasks
```

### Supervisor (Multi-Agent)

```
Theory: A supervisor agent routes queries to specialist agents.
Each specialist has deep expertise in one domain.

                    ┌──────────────┐
                    │  Supervisor  │
                    │  (Router)    │
                    └─────┬────────┘
                          │
              ┌──────────┼──────────┐
              ▼           ▼           ▼
        ┌────────┐ ┌────────┐ ┌────────┐
        │Billing │ │Technical│ │Creative│
        │Agent   │ │Agent    │ │Agent   │
        └────────┘ └────────┘ └────────┘

Strength: Scalable, each agent is focused and testable
Weakness: Routing errors cascade, supervisor is a bottleneck
```

### Memory Theory for Agents

```
Human memory has multiple systems. AI agents should too:

1. WORKING MEMORY (context window)
   - Current conversation messages
   - Limited by token budget
   - Lost when conversation ends

2. SHORT-TERM MEMORY (checkpointing)
   - Persists across messages in same conversation
   - Stored in checkpointer (Postgres)
   - Lost when conversation/thread is deleted

3. LONG-TERM MEMORY (vector store / knowledge base)
   - Facts about the user across all conversations
   - Stored in separate database
   - Persists indefinitely
   - Retrieved by semantic similarity

4. EPISODIC MEMORY (conversation summaries)
   - Compressed summaries of past conversations
   - Enables "last time you asked about X..."
   - Prevents context window overflow
```

### Human-in-the-Loop Theory

```
Not all agent actions should be autonomous. High-stakes actions need human approval:

┌─────────────────────────────────────────────────────────┐
│  AUTONOMY SPECTRUM                                          │
├─────────────────────────────────────────────────────────┤
│                                                             │
│  FULL AUTO          APPROVE           HUMAN DOES IT         │
│  ───────────────────────────────────────────────────  │
│  Answer FAQ     Cancel subscription    Legal advice          │
│  Search docs    Process refund         Account deletion      │
│  Check status   Change plan            Escalate complaint    │
│                                                             │
│  Low risk       Medium risk            High risk             │
└─────────────────────────────────────────────────────────┘

LangGraph implements this with `interrupt_before`:
- Graph pauses before the specified node
- State is saved to checkpointer
- Human reviews and approves/rejects
- Graph resumes from checkpoint
```

---

## Multi-Agent System (Supervisor Pattern)

```python
from typing import Annotated, TypedDict, Literal
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages

class State(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    next_agent: str

# Supervisor decides which specialist to route to
def supervisor(state: State) -> dict:
    model = ChatAnthropic(model="claude-sonnet-4-20250514")
    
    response = model.invoke([
        SystemMessage(content="""You are a supervisor routing customer queries.
Route to:
- "billing" for payment/subscription issues
- "technical" for product bugs/how-to questions
- "creative" for design help/tutorials
- "FINISH" if the query is fully resolved

Respond with ONLY the routing decision."""),
        *state["messages"],
    ])
    
    route = response.content.strip().lower()
    return {"next_agent": route, "messages": [response]}

# Specialist agents
def billing_agent(state: State) -> dict:
    model = ChatAnthropic(model="claude-sonnet-4-20250514")
    response = model.invoke([
        SystemMessage(content="You are a billing specialist. Help with payment and subscription issues."),
        *state["messages"],
    ])
    return {"messages": [response]}

def technical_agent(state: State) -> dict:
    model = ChatAnthropic(model="claude-sonnet-4-20250514")
    response = model.invoke([
        SystemMessage(content="You are a technical support specialist. Help with product issues."),
        *state["messages"],
    ])
    return {"messages": [response]}

def creative_agent(state: State) -> dict:
    model = ChatAnthropic(model="claude-sonnet-4-20250514")
    response = model.invoke([
        SystemMessage(content="You are a creative assistant. Help with design and tutorials."),
        *state["messages"],
    ])
    return {"messages": [response]}

# Routing function
def route_to_agent(state: State) -> str:
    next_agent = state.get("next_agent", "").lower()
    if "billing" in next_agent:
        return "billing"
    elif "technical" in next_agent:
        return "technical"
    elif "creative" in next_agent:
        return "creative"
    return END

# Build graph
graph = StateGraph(State)

graph.add_node("supervisor", supervisor)
graph.add_node("billing", billing_agent)
graph.add_node("technical", technical_agent)
graph.add_node("creative", creative_agent)

graph.add_edge(START, "supervisor")
graph.add_conditional_edges("supervisor", route_to_agent)
graph.add_edge("billing", "supervisor")     # Report back to supervisor
graph.add_edge("technical", "supervisor")
graph.add_edge("creative", "supervisor")

app = graph.compile(checkpointer=MemorySaver())
```

---

## Subgraphs (Composable Agents)

```python
from langgraph.graph import StateGraph

# Define a reusable RAG subgraph
class RAGState(TypedDict):
    messages: Annotated[list, add_messages]
    query: str
    documents: list[str]
    answer: str

def retrieve(state: RAGState) -> dict:
    docs = vector_store.similarity_search(state["query"], k=5)
    return {"documents": [d.page_content for d in docs]}

def generate(state: RAGState) -> dict:
    context = "\n".join(state["documents"])
    response = model.invoke([
        SystemMessage(content=f"Answer based on context:\n{context}"),
        HumanMessage(content=state["query"]),
    ])
    return {"answer": response.content, "messages": [response]}

# Build RAG subgraph
rag_graph = StateGraph(RAGState)
rag_graph.add_node("retrieve", retrieve)
rag_graph.add_node("generate", generate)
rag_graph.add_edge(START, "retrieve")
rag_graph.add_edge("retrieve", "generate")
rag_graph.add_edge("generate", END)
rag_subgraph = rag_graph.compile()

# Use subgraph in parent graph
class ParentState(TypedDict):
    messages: Annotated[list, add_messages]
    query: str

def call_rag(state: ParentState) -> dict:
    """Invoke RAG subgraph."""
    result = rag_subgraph.invoke({"query": state["query"], "messages": state["messages"]})
    return {"messages": result["messages"]}

parent_graph = StateGraph(ParentState)
parent_graph.add_node("rag", call_rag)
parent_graph.add_edge(START, "rag")
parent_graph.add_edge("rag", END)
```

---

## Plan-and-Execute Agent

```python
from typing import Annotated, TypedDict
from pydantic import BaseModel, Field

class Plan(BaseModel):
    steps: list[str] = Field(description="Steps to complete the task")

class PlanExecuteState(TypedDict):
    messages: Annotated[list, add_messages]
    plan: list[str]
    current_step: int
    step_results: list[str]
    final_answer: str

def create_plan(state: PlanExecuteState) -> dict:
    """Create a plan for the task."""
    model = ChatAnthropic(model="claude-sonnet-4-20250514").with_structured_output(Plan)
    query = state["messages"][-1].content
    
    plan = model.invoke([
        SystemMessage(content="Break this task into 3-5 sequential steps. Be specific."),
        HumanMessage(content=query),
    ])
    return {"plan": plan.steps, "current_step": 0}

def execute_step(state: PlanExecuteState) -> dict:
    """Execute the current step."""
    step = state["plan"][state["current_step"]]
    previous_results = "\n".join(state["step_results"])
    
    model = ChatAnthropic(model="claude-sonnet-4-20250514")
    response = model.invoke([
        SystemMessage(content=f"Execute this step. Previous results:\n{previous_results}"),
        HumanMessage(content=step),
    ])
    
    return {
        "step_results": [response.content],
        "current_step": state["current_step"] + 1,
    }

def synthesize(state: PlanExecuteState) -> dict:
    """Synthesize final answer from all step results."""
    all_results = "\n\n".join(state["step_results"])
    query = state["messages"][-1].content
    
    model = ChatAnthropic(model="claude-sonnet-4-20250514")
    response = model.invoke([
        SystemMessage(content="Synthesize a comprehensive answer from these step results."),
        HumanMessage(content=f"Original question: {query}\n\nStep results:\n{all_results}"),
    ])
    return {"final_answer": response.content, "messages": [response]}

def should_continue(state: PlanExecuteState) -> str:
    if state["current_step"] >= len(state["plan"]):
        return "synthesize"
    return "execute"

# Build graph
graph = StateGraph(PlanExecuteState)
graph.add_node("plan", create_plan)
graph.add_node("execute", execute_step)
graph.add_node("synthesize", synthesize)

graph.add_edge(START, "plan")
graph.add_edge("plan", "execute")
graph.add_conditional_edges("execute", should_continue)
graph.add_edge("synthesize", END)

app = graph.compile()
```

---

## Production Agent with Error Handling

```python
from langgraph.graph import StateGraph, START, END
from langgraph.errors import GraphRecursionError

class ProductionState(TypedDict):
    messages: Annotated[list, add_messages]
    error: Optional[str]
    retry_count: int
    escalated: bool

def agent_node(state: ProductionState) -> dict:
    """Main agent with error handling."""
    try:
        model = ChatAnthropic(model="claude-sonnet-4-20250514")
        response = model.invoke(state["messages"])
        return {"messages": [response], "error": None}
    except Exception as e:
        return {"error": str(e), "retry_count": state.get("retry_count", 0) + 1}

def error_handler(state: ProductionState) -> dict:
    """Handle errors with retry or escalation."""
    if state["retry_count"] >= 3:
        return {
            "escalated": True,
            "messages": [AIMessage(content="I'm having trouble. Let me connect you with a specialist.")],
        }
    return {}  # Will retry

def should_retry_or_end(state: ProductionState) -> str:
    if state.get("error"):
        if state.get("retry_count", 0) >= 3:
            return "escalate"
        return "retry"
    
    # Check if agent wants to use tools
    last_msg = state["messages"][-1]
    if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
        return "tools"
    return END

graph = StateGraph(ProductionState)
graph.add_node("agent", agent_node)
graph.add_node("tools", ToolNode(tools))
graph.add_node("error_handler", error_handler)

graph.add_edge(START, "agent")
graph.add_conditional_edges("agent", should_retry_or_end, {
    "tools": "tools",
    "retry": "agent",
    "escalate": "error_handler",
    END: END,
})
graph.add_edge("tools", "agent")
graph.add_edge("error_handler", END)

# Compile with recursion limit (prevent infinite loops)
app = graph.compile(
    checkpointer=PostgresSaver.from_conn_string(DATABASE_URL),
)

# Invoke with recursion limit
result = app.invoke(
    {"messages": [HumanMessage(content="Help me")]},
    config={"configurable": {"thread_id": "t-1"}, "recursion_limit": 25},
)
```

---

## Memory Patterns

### Short-Term (Conversation) Memory

```python
# Built-in with checkpointer — each thread_id maintains full history
app = graph.compile(checkpointer=MemorySaver())

# Conversation 1
config = {"configurable": {"thread_id": "user-123-session-1"}}
app.invoke({"messages": [HumanMessage(content="I'm Alice")]}, config)
app.invoke({"messages": [HumanMessage(content="What's my name?")]}, config)
# → "Your name is Alice"
```

### Long-Term Memory (Cross-Session)

```python
from langgraph.store.memory import InMemoryStore

# Store for long-term facts about users
store = InMemoryStore()

class StateWithMemory(TypedDict):
    messages: Annotated[list, add_messages]
    user_id: str

def load_user_memory(state: StateWithMemory, store) -> dict:
    """Load user's long-term memory at start of conversation."""
    user_id = state["user_id"]
    memories = store.search(("users", user_id))
    
    if memories:
        memory_text = "\n".join(m.value["fact"] for m in memories)
        system_msg = SystemMessage(content=f"Known facts about this user:\n{memory_text}")
        return {"messages": [system_msg]}
    return {}

def save_user_memory(state: StateWithMemory, store) -> dict:
    """Extract and save important facts from conversation."""
    # Use LLM to extract facts worth remembering
    model = ChatAnthropic(model="claude-3-5-haiku-20241022")
    response = model.invoke([
        SystemMessage(content="Extract key facts about the user from this conversation. Return as JSON list of strings."),
        *state["messages"][-10:],  # Last 10 messages
    ])
    
    facts = json.loads(response.content)
    for fact in facts:
        store.put(("users", state["user_id"]), str(uuid4()), {"fact": fact})
    
    return {}
```

### Summary Memory (Compress Long Conversations)

```python
def summarize_if_needed(state: AgentState) -> dict:
    """Summarize old messages when conversation gets too long."""
    messages = state["messages"]
    
    if len(messages) > 20:
        # Summarize first 15 messages
        old_messages = messages[:15]
        model = ChatAnthropic(model="claude-3-5-haiku-20241022")
        
        summary = model.invoke([
            SystemMessage(content="Summarize this conversation in 2-3 sentences."),
            *old_messages,
        ])
        
        # Replace old messages with summary
        new_messages = [
            SystemMessage(content=f"[Conversation summary: {summary.content}]"),
            *messages[15:],
        ]
        return {"messages": new_messages}
    
    return {}
```

---

## Deployment Configuration

```python
# Production-ready graph compilation
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver

async def create_production_agent():
    checkpointer = await AsyncPostgresSaver.from_conn_string(
        "postgresql://user:pass@host:5432/langgraph"
    )
    
    app = graph.compile(
        checkpointer=checkpointer,
        interrupt_before=["dangerous_action"],  # Human approval
    )
    
    return app

# Run with configuration
result = await app.ainvoke(
    {"messages": [HumanMessage(content="Cancel subscription")]},
    config={
        "configurable": {
            "thread_id": "conv-abc-123",
            "user_id": "user-456",
        },
        "recursion_limit": 30,
        "tags": ["production", "support"],
        "metadata": {"version": "2.1.0"},
    },
)
```

---

## Next: [LangSmith (Observability) →](06_LangSmith.md)
