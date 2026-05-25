# 3. Tools & Tool Calling

## Theory: What is Tool Use?

Tool use (function calling) allows LLMs to interact with the external world. Instead of just generating text, the model can:
- Call APIs (get real-time data)
- Execute code (calculations, data processing)
- Query databases (retrieve specific records)
- Take actions (send emails, create tickets, modify accounts)

### How Tool Calling Works

```
1. You define tools (name, description, parameters schema)
2. You send tools + user message to the LLM
3. LLM decides IF and WHICH tool(s) to call (based on user intent)
4. LLM returns a tool_call (name + arguments) instead of text
5. You execute the tool with those arguments
6. You send the tool result back to the LLM
7. LLM generates final response incorporating the tool result
```

### Why This Architecture?

| Approach | Problem |
|----------|----------|
| Hardcode API calls | Can't handle diverse user queries |
| Let LLM write code | Unsafe, unreliable, slow |
| **Tool calling** | LLM decides WHAT to call, you control HOW it executes |

### Tool Selection Theory

The LLM selects tools based on:
1. **Tool description** — semantic match between user intent and tool purpose
2. **Parameter schema** — can the required info be extracted from the query?
3. **Context** — conversation history may indicate which tool is appropriate

**Best practices for tool descriptions:**
- Be specific: "Get user's subscription plan and billing status" not "Get user info"
- Include when to use: "Use when user asks about their plan, billing, or payment"
- Include when NOT to use: "Do NOT use for password resets"
- Describe output: "Returns plan name, status, renewal date, and price"

### Parallel vs Sequential Tool Calls

```
Parallel: "What's the weather in Tokyo AND New York?"
  → Model calls get_weather("Tokyo") AND get_weather("New York") simultaneously

Sequential: "Find my subscription, then cancel it"
  → Model calls get_subscription() first
  → Uses result to call cancel_subscription(plan_id=...)
```

---

## Defining Tools

```python
from langchain_core.tools import tool, StructuredTool
from pydantic import BaseModel, Field

# Simple tool with @tool decorator
@tool
def get_subscription(user_id: str) -> dict:
    """Get user's subscription details. Use when user asks about their plan or billing."""
    # Real implementation calls your API
    return {"plan": "Pro", "status": "active", "renewal": "2024-02-15", "price": "$19.99/mo"}

@tool
def cancel_subscription(user_id: str, reason: str) -> dict:
    """Cancel a user's subscription. ALWAYS confirm with user before calling this."""
    return {"status": "cancelled", "effective_date": "2024-02-15", "refund": "$0.00"}

@tool
def search_help_docs(query: str, product: str = "all") -> list[dict]:
    """Search Adobe help documentation. Use for how-to questions and troubleshooting."""
    # Real implementation searches vector DB
    return [{"title": "Export Guide", "content": "To export PDF...", "url": "https://..."}]

# Tool with complex input schema
class SearchInput(BaseModel):
    query: str = Field(description="Search query")
    filters: dict = Field(default={}, description="Optional filters: product, category, date_range")
    max_results: int = Field(default=5, description="Maximum results to return")

@tool(args_schema=SearchInput)
def advanced_search(query: str, filters: dict = {}, max_results: int = 5) -> list:
    """Advanced search with filtering capabilities."""
    return search_engine.search(query, filters=filters, limit=max_results)
```

## Binding Tools to Models

```python
from langchain_anthropic import ChatAnthropic

model = ChatAnthropic(model="claude-sonnet-4-20250514")
tools = [get_subscription, cancel_subscription, search_help_docs]

# Bind tools (model can now call them)
model_with_tools = model.bind_tools(tools)

# Force specific tool
model_forced = model.bind_tools(tools, tool_choice={"type": "tool", "name": "search_help_docs"})

# Invoke — model decides whether to call tools
response = model_with_tools.invoke([HumanMessage(content="What's my subscription plan?")])

# Check if model wants to call a tool
if response.tool_calls:
    for tool_call in response.tool_calls:
        print(f"Tool: {tool_call['name']}, Args: {tool_call['args']}")
```

## Tool Execution Loop

```python
from langchain_core.messages import ToolMessage

def run_agent_with_tools(query: str, tools: list) -> str:
    model = ChatAnthropic(model="claude-sonnet-4-20250514").bind_tools(tools)
    tool_map = {t.name: t for t in tools}
    
    messages = [HumanMessage(content=query)]
    
    while True:
        response = model.invoke(messages)
        messages.append(response)
        
        if not response.tool_calls:
            return response.content  # Final answer
        
        # Execute tools
        for tool_call in response.tool_calls:
            tool = tool_map[tool_call["name"]]
            result = tool.invoke(tool_call["args"])
            messages.append(ToolMessage(
                content=str(result),
                tool_call_id=tool_call["id"],
            ))
```

## Structured Output (No Tools Needed)

```python
from pydantic import BaseModel, Field

class SupportTicket(BaseModel):
    category: str = Field(description="billing, technical, account, other")
    priority: str = Field(description="low, medium, high, critical")
    summary: str = Field(description="One-line summary of the issue")
    suggested_action: str = Field(description="Recommended next step")

model = ChatAnthropic(model="claude-sonnet-4-20250514")
structured = model.with_structured_output(SupportTicket)

ticket = structured.invoke("I've been charged twice and I'm furious!")
print(ticket.category)         # "billing"
print(ticket.priority)         # "high"
print(ticket.suggested_action) # "Initiate refund for duplicate charge"
```

---

## Next: [LangGraph Fundamentals →](04_LangGraph_Fundamentals.md)
