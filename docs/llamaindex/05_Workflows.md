# 5. LlamaIndex Workflows

## Theory: Why Workflows?

LlamaIndex Workflows are the **next-generation orchestration system** — an event-driven, async-first alternative to traditional agent loops.

### The Problem with Traditional Agents

```
Traditional Agent (ReAct loop):
  while not done:
      thought = llm.think(state)
      action = llm.decide_action(thought)
      result = execute(action)
      state.update(result)
      done = llm.should_stop(state)

Problems:
  1. Opaque: Hard to see what's happening inside the loop
  2. Rigid: Can't easily add steps, branching, or parallel execution
  3. Error-prone: One bad LLM decision derails everything
  4. Hard to test: Can't test individual steps in isolation
  5. No persistence: Can't pause/resume mid-execution
```

### Workflows Solution

```
Workflow = Collection of STEPS connected by EVENTS

Step 1: ClassifyIntent
    │ emits IntentClassified(intent="billing")
    ▼
Step 2: RetrieveContext
    │ emits ContextRetrieved(docs=[...])
    ▼
Step 3: GenerateResponse
    │ emits ResponseGenerated(text="...")
    ▼
Step 4: ValidateResponse
    │ emits StopEvent(result="...")
    ▼
Done

Benefits:
  ✅ Each step is independently testable
  ✅ Steps communicate via typed events (clear contracts)
  ✅ Async-first (non-blocking I/O)
  ✅ Can run steps in parallel
  ✅ Easy to add/remove/reorder steps
  ✅ Built-in streaming support
  ✅ Visualization of execution flow
```

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Workflow** | Container for steps; manages event routing |
| **Step** | A function decorated with `@step`; receives events, emits events |
| **Event** | Typed message passed between steps (Pydantic model) |
| **Context** | Shared state accessible by all steps within a run |
| **StartEvent** | Triggers the workflow (input) |
| **StopEvent** | Terminates the workflow (output) |

---

## Basic Workflow

```python
from llama_index.core.workflow import (
    Workflow, StartEvent, StopEvent, step, Event, Context
)
from llama_index.llms.anthropic import Anthropic

# Define custom events
class IntentClassified(Event):
    intent: str
    confidence: float

class ContextRetrieved(Event):
    documents: list[str]
    query: str

class ResponseGenerated(Event):
    response: str
    sources: list[str]

# Define workflow
class SupportAgentWorkflow(Workflow):
    
    @step
    async def classify_intent(self, ev: StartEvent) -> IntentClassified:
        """Step 1: Classify user intent."""
        query = ev.query
        llm = Anthropic(model="claude-3-5-haiku-20241022")
        
        response = await llm.acomplete(
            f"Classify this query into one category (billing/technical/general): {query}"
        )
        
        intent = response.text.strip().lower()
        return IntentClassified(intent=intent, confidence=0.9)
    
    @step
    async def retrieve_context(self, ev: IntentClassified) -> ContextRetrieved:
        """Step 2: Retrieve relevant documents based on intent."""
        # Use intent to select appropriate index
        index = self.get_index_for_intent(ev.intent)
        retriever = index.as_retriever(similarity_top_k=5)
        
        nodes = await retriever.aretrieve(ev.query if hasattr(ev, 'query') else "")
        docs = [node.text for node in nodes]
        
        return ContextRetrieved(documents=docs, query=ev.query)
    
    @step
    async def generate_response(self, ev: ContextRetrieved) -> StopEvent:
        """Step 3: Generate grounded response."""
        llm = Anthropic(model="claude-sonnet-4-20250514")
        
        context = "\n\n".join(ev.documents)
        prompt = f"""Answer based on context only.
        
Context: {context}

Question: {ev.query}

Answer:"""
        
        response = await llm.acomplete(prompt)
        return StopEvent(result=response.text)

# Run workflow
workflow = SupportAgentWorkflow()
result = await workflow.run(query="How do I cancel my subscription?")
print(result)
```

---

## Workflow with Context (Shared State)

```python
from llama_index.core.workflow import Context

class AgentWorkflow(Workflow):
    
    @step
    async def initialize(self, ctx: Context, ev: StartEvent) -> IntentClassified:
        """Store shared state in context."""
        # Context persists across all steps in this run
        await ctx.set("user_id", ev.user_id)
        await ctx.set("query", ev.query)
        await ctx.set("start_time", time.time())
        
        # Classify intent
        intent = await self.classify(ev.query)
        return IntentClassified(intent=intent, confidence=0.9)
    
    @step
    async def process(self, ctx: Context, ev: IntentClassified) -> StopEvent:
        """Access shared context from any step."""
        user_id = await ctx.get("user_id")
        query = await ctx.get("query")
        
        # Use context for personalization
        user_history = await self.get_user_history(user_id)
        response = await self.generate(query, ev.intent, user_history)
        
        # Track metrics
        start_time = await ctx.get("start_time")
        latency = time.time() - start_time
        
        return StopEvent(result={"response": response, "latency": latency})
```

---

## Conditional Routing

```python
class RoutedWorkflow(Workflow):
    
    @step
    async def classify(self, ev: StartEvent) -> IntentClassified | EscalateEvent:
        """Route based on classification result."""
        intent, confidence = await self.classify_intent(ev.query)
        
        if confidence < 0.5:
            # Low confidence → escalate to human
            return EscalateEvent(reason="low_confidence", query=ev.query)
        
        return IntentClassified(intent=intent, confidence=confidence)
    
    @step
    async def handle_billing(self, ev: IntentClassified) -> StopEvent | None:
        """Only handles billing intents."""
        if ev.intent != "billing":
            return None  # Skip this step (event not consumed)
        
        response = await self.billing_agent(ev)
        return StopEvent(result=response)
    
    @step
    async def handle_technical(self, ev: IntentClassified) -> StopEvent | None:
        """Only handles technical intents."""
        if ev.intent != "technical":
            return None
        
        response = await self.technical_agent(ev)
        return StopEvent(result=response)
    
    @step
    async def handle_escalation(self, ev: EscalateEvent) -> StopEvent:
        """Handle escalation to human."""
        ticket = await self.create_support_ticket(ev.query, ev.reason)
        return StopEvent(result=f"Escalated to human support. Ticket: {ticket.id}")
```

---

## Parallel Execution

```python
class ParallelWorkflow(Workflow):
    
    @step
    async def fan_out(self, ev: StartEvent) -> SearchWeb | SearchDocs | SearchDB:
        """Emit multiple events → triggers parallel steps."""
        query = ev.query
        # All three events are emitted simultaneously
        self.send_event(SearchWeb(query=query))
        self.send_event(SearchDocs(query=query))
        self.send_event(SearchDB(query=query))
    
    @step
    async def search_web(self, ev: SearchWeb) -> SearchResult:
        results = await web_search(ev.query)
        return SearchResult(source="web", results=results)
    
    @step
    async def search_docs(self, ev: SearchDocs) -> SearchResult:
        results = await doc_search(ev.query)
        return SearchResult(source="docs", results=results)
    
    @step
    async def search_db(self, ev: SearchDB) -> SearchResult:
        results = await db_search(ev.query)
        return SearchResult(source="db", results=results)
    
    @step(num_workers=3)  # Collect 3 SearchResult events before proceeding
    async def synthesize(self, ctx: Context, ev: SearchResult) -> StopEvent | None:
        """Collect all search results, then synthesize."""
        # Accumulate results in context
        results = await ctx.get("results", default=[])
        results.append({"source": ev.source, "data": ev.results})
        await ctx.set("results", results)
        
        # Wait for all 3 sources
        if len(results) < 3:
            return None  # Not ready yet
        
        # All results collected — synthesize
        answer = await self.generate_answer(results)
        return StopEvent(result=answer)
```

---

## Streaming

```python
class StreamingWorkflow(Workflow):
    
    @step
    async def generate(self, ev: StartEvent) -> StopEvent:
        """Stream tokens as they're generated."""
        llm = Anthropic(model="claude-sonnet-4-20250514")
        
        # Stream response
        response_gen = await llm.astream_complete(ev.query)
        
        full_response = ""
        async for chunk in response_gen:
            full_response += chunk.delta
            # Emit intermediate event for streaming to client
            self.send_event(TokenEvent(token=chunk.delta))
        
        return StopEvent(result=full_response)

# Consume stream
workflow = StreamingWorkflow()
handler = workflow.run(query="Explain quantum computing")

async for event in handler.stream_events():
    if isinstance(event, TokenEvent):
        print(event.token, end="", flush=True)

# Get final result
result = await handler
```

---

## Workflow with Tools

```python
from llama_index.core.tools import FunctionTool

# Define tools
def get_subscription(user_id: str) -> dict:
    """Get user subscription details."""
    return {"plan": "Pro", "status": "active", "price": "$19.99"}

def cancel_subscription(user_id: str, reason: str) -> dict:
    """Cancel user subscription."""
    return {"status": "cancelled", "effective_date": "2024-02-15"}

tools = [
    FunctionTool.from_defaults(fn=get_subscription),
    FunctionTool.from_defaults(fn=cancel_subscription),
]

class ToolWorkflow(Workflow):
    
    @step
    async def agent_step(self, ctx: Context, ev: StartEvent | ToolResultEvent) -> ToolCallEvent | StopEvent:
        """ReAct-style agent step with tool use."""
        messages = await ctx.get("messages", default=[])
        
        if isinstance(ev, StartEvent):
            messages.append({"role": "user", "content": ev.query})
        elif isinstance(ev, ToolResultEvent):
            messages.append({"role": "tool", "content": ev.result})
        
        await ctx.set("messages", messages)
        
        # Call LLM with tools
        llm = Anthropic(model="claude-sonnet-4-20250514")
        response = await llm.achat(messages, tools=tools)
        
        if response.tool_calls:
            return ToolCallEvent(tool_calls=response.tool_calls)
        
        return StopEvent(result=response.message.content)
    
    @step
    async def execute_tools(self, ev: ToolCallEvent) -> ToolResultEvent:
        """Execute tool calls and return results."""
        results = []
        for call in ev.tool_calls:
            tool = self.get_tool(call.name)
            result = tool.call(**call.arguments)
            results.append(str(result))
        
        return ToolResultEvent(result="\n".join(results))
```

---

## Visualization

```python
from llama_index.utils.workflow import draw_all_possible_flows

# Generate Mermaid diagram
draw_all_possible_flows(SupportAgentWorkflow, filename="workflow.html")

# Or get as string
mermaid = SupportAgentWorkflow.get_mermaid_diagram()
print(mermaid)
```

---

## Workflows vs LangGraph

| Feature | LlamaIndex Workflows | LangGraph |
|---------|---------------------|-----------|
| Paradigm | Event-driven | Graph-based |
| State | Context object | TypedDict with reducers |
| Routing | Event types + conditional returns | Conditional edges |
| Parallelism | Multiple events + num_workers | RunnableParallel |
| Persistence | Coming soon | Checkpointing (mature) |
| Human-in-the-loop | Manual (emit event, wait) | Built-in (interrupt_before) |
| Streaming | Native (stream_events) | Native (astream_events) |
| Visualization | Mermaid | Mermaid |
| Maturity | Newer (2024) | More mature |

**Use Workflows when**: Your app is RAG-heavy and you're already in LlamaIndex ecosystem.
**Use LangGraph when**: You need complex state management, human-in-the-loop, or multi-agent systems.

---

## Next: [Advanced RAG Patterns →](06_Advanced_RAG.md)
