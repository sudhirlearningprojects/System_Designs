# 3. Tool Use & Function Calling

## How Tool Use Works

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  User    │────►│  Claude  │────►│  Your    │────►│  Claude  │
│  Message │     │  (thinks)│     │  Code    │     │  (final) │
└──────────┘     └────┬─────┘     └────┬─────┘     └──────────┘
                      │                 │
                 tool_use block    Execute tool,
                 (name + input)   return result
```

1. You send a message with tool definitions
2. Claude decides which tool(s) to call and with what arguments
3. You execute the tool and return the result
4. Claude uses the result to formulate its final response

---

## Defining Tools

```python
tools = [
    {
        "name": "get_weather",
        "description": "Get current weather for a location. Use this when the user asks about weather conditions.",
        "input_schema": {
            "type": "object",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "City name, e.g., 'San Francisco, CA'"
                },
                "unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "description": "Temperature unit"
                }
            },
            "required": ["location"]
        }
    },
    {
        "name": "search_database",
        "description": "Search the product database. Returns matching products with prices and availability.",
        "input_schema": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "Search query"
                },
                "category": {
                    "type": "string",
                    "enum": ["electronics", "clothing", "books", "home"],
                    "description": "Product category filter"
                },
                "max_results": {
                    "type": "integer",
                    "description": "Maximum number of results (default: 5)"
                }
            },
            "required": ["query"]
        }
    },
    {
        "name": "execute_code",
        "description": "Execute Python code in a sandboxed environment. Use for calculations, data processing, or generating visualizations.",
        "input_schema": {
            "type": "object",
            "properties": {
                "code": {
                    "type": "string",
                    "description": "Python code to execute"
                },
                "timeout_seconds": {
                    "type": "integer",
                    "description": "Maximum execution time (default: 30)"
                }
            },
            "required": ["code"]
        }
    }
]
```

---

## Tool Use Loop (Complete Pattern)

```python
import anthropic
import json

client = anthropic.Anthropic()

def run_agent(user_message: str, tools: list, system: str = "") -> str:
    messages = [{"role": "user", "content": user_message}]
    
    while True:
        response = client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=4096,
            system=system,
            tools=tools,
            messages=messages,
        )
        
        # If Claude is done (no more tool calls), return the text
        if response.stop_reason == "end_turn":
            return extract_text(response)
        
        # Process tool calls
        if response.stop_reason == "tool_use":
            # Add Claude's response (with tool_use blocks) to messages
            messages.append({"role": "assistant", "content": response.content})
            
            # Execute each tool call and collect results
            tool_results = []
            for block in response.content:
                if block.type == "tool_use":
                    result = execute_tool(block.name, block.input)
                    tool_results.append({
                        "type": "tool_result",
                        "tool_use_id": block.id,
                        "content": json.dumps(result) if isinstance(result, dict) else str(result)
                    })
            
            # Add tool results to messages
            messages.append({"role": "user", "content": tool_results})

def execute_tool(name: str, input_data: dict) -> dict:
    """Route tool calls to actual implementations."""
    match name:
        case "get_weather":
            return get_weather_api(input_data["location"], input_data.get("unit", "celsius"))
        case "search_database":
            return search_products(input_data["query"], input_data.get("category"))
        case "execute_code":
            return run_sandboxed_code(input_data["code"])
        case _:
            return {"error": f"Unknown tool: {name}"}

def extract_text(response) -> str:
    return "".join(block.text for block in response.content if block.type == "text")
```

---

## Parallel Tool Use

Claude can call multiple tools simultaneously when they're independent.

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=4096,
    tools=tools,
    messages=[{
        "role": "user",
        "content": "What's the weather in Tokyo and New York? Also search for 'umbrella' in our store."
    }]
)

# response.content might contain:
# [
#   TextBlock("Let me check that for you."),
#   ToolUseBlock(name="get_weather", input={"location": "Tokyo"}),
#   ToolUseBlock(name="get_weather", input={"location": "New York"}),
#   ToolUseBlock(name="search_database", input={"query": "umbrella"})
# ]

# Execute all tools in parallel
import asyncio

async def execute_tools_parallel(tool_blocks):
    tasks = [execute_tool_async(block.name, block.input) for block in tool_blocks]
    return await asyncio.gather(*tasks)
```

### Disable Parallel Tool Use

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=4096,
    tools=tools,
    tool_choice={"type": "auto", "disable_parallel_tool_use": True},
    messages=messages,
)
```

---

## Tool Choice (Controlling Tool Selection)

```python
# Auto: Claude decides whether to use tools (default)
tool_choice = {"type": "auto"}

# Any: Force Claude to use at least one tool
tool_choice = {"type": "any"}

# Specific: Force a specific tool
tool_choice = {"type": "tool", "name": "search_database"}
```

---

## Streaming with Tool Use

```python
with client.messages.stream(
    model="claude-sonnet-4-20250514",
    max_tokens=4096,
    tools=tools,
    messages=[{"role": "user", "content": "What's the weather in London?"}]
) as stream:
    current_tool = None
    tool_input_json = ""
    
    for event in stream:
        match event.type:
            case "content_block_start":
                if event.content_block.type == "tool_use":
                    current_tool = event.content_block.name
                    tool_input_json = ""
                    print(f"\n🔧 Calling tool: {current_tool}")
            
            case "content_block_delta":
                if event.delta.type == "text_delta":
                    print(event.delta.text, end="")
                elif event.delta.type == "input_json_delta":
                    tool_input_json += event.delta.partial_json
            
            case "content_block_stop":
                if current_tool:
                    input_data = json.loads(tool_input_json)
                    print(f"\n  Input: {input_data}")
                    current_tool = None
```

---

## Error Handling in Tools

```python
def execute_tool_safely(name: str, input_data: dict) -> dict:
    try:
        result = execute_tool(name, input_data)
        return {"status": "success", "data": result}
    except TimeoutError:
        return {"status": "error", "error": "Tool execution timed out. Please try again."}
    except PermissionError as e:
        return {"status": "error", "error": f"Permission denied: {e}"}
    except Exception as e:
        return {"status": "error", "error": f"Tool failed: {str(e)}"}

# Return error as tool_result — Claude will handle gracefully
tool_results.append({
    "type": "tool_result",
    "tool_use_id": block.id,
    "content": json.dumps({"error": "Database connection timeout"}),
    "is_error": True  # Tells Claude this is an error
})
```

---

## Complex Tool Definitions (Real-World)

### Database Query Tool

```python
{
    "name": "query_database",
    "description": """Execute a read-only SQL query against the application database.
    
    Available tables:
    - users (id, email, name, plan, created_at)
    - subscriptions (id, user_id, plan_name, status, expires_at)
    - invoices (id, user_id, amount, currency, paid_at, status)
    
    IMPORTANT: Only SELECT queries are allowed. Never modify data.""",
    "input_schema": {
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "SQL SELECT query to execute"
            },
            "params": {
                "type": "array",
                "items": {"type": "string"},
                "description": "Parameterized query values (for safety)"
            }
        },
        "required": ["query"]
    }
}
```

### Multi-Step Action Tool

```python
{
    "name": "manage_subscription",
    "description": """Manage a user's subscription. Requires user verification first.
    
    Actions:
    - view: Show current subscription details
    - upgrade: Upgrade to a higher plan
    - downgrade: Downgrade to a lower plan  
    - cancel: Cancel subscription (with retention offer)
    
    IMPORTANT: Always confirm with user before executing upgrade/downgrade/cancel.""",
    "input_schema": {
        "type": "object",
        "properties": {
            "user_id": {"type": "string", "description": "User's account ID"},
            "action": {
                "type": "string",
                "enum": ["view", "upgrade", "downgrade", "cancel"]
            },
            "new_plan": {
                "type": "string",
                "enum": ["free", "pro", "enterprise"],
                "description": "Target plan (required for upgrade/downgrade)"
            },
            "reason": {
                "type": "string",
                "description": "Reason for change (required for cancel/downgrade)"
            }
        },
        "required": ["user_id", "action"]
    }
}
```

---

## Tool Use Best Practices

1. **Descriptive names**: `search_help_docs` not `search`
2. **Rich descriptions**: Include what the tool does, when to use it, and constraints
3. **Enum for fixed options**: Use `enum` instead of free-text for known values
4. **Required vs optional**: Only mark truly required fields as required
5. **Error messages**: Return helpful errors that Claude can relay to users
6. **Idempotency**: Tools that modify state should be idempotent
7. **Confirmation for destructive actions**: Have Claude confirm before delete/cancel
8. **Limit tool count**: 5-15 tools is optimal; too many confuses selection

---

## Next: [RAG & Knowledge Systems →](04_RAG_and_Knowledge.md)
