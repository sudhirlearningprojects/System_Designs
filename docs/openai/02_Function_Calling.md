# 2. Function Calling & Tools

## Theory

Function calling lets GPT-4o decide WHEN to call tools and WITH WHAT arguments. You execute the tool and return results.

## Defining Tools

```python
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_subscription",
            "description": "Get user's subscription details. Use when user asks about their plan.",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "string", "description": "User's account ID"},
                },
                "required": ["user_id"],
                "additionalProperties": False,
            },
            "strict": True,  # Enforce schema compliance
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_docs",
            "description": "Search help documentation. Use for how-to questions.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Search query"},
                    "category": {"type": "string", "enum": ["billing", "technical", "account"]},
                },
                "required": ["query"],
                "additionalProperties": False,
            },
            "strict": True,
        },
    },
]
```

## Tool Use Loop

```python
import json

def run_agent(query: str, user_id: str) -> str:
    messages = [
        {"role": "system", "content": "You are a helpful support agent."},
        {"role": "user", "content": query},
    ]
    
    while True:
        response = client.chat.completions.create(
            model="gpt-4o",
            messages=messages,
            tools=tools,
            tool_choice="auto",
        )
        
        msg = response.choices[0].message
        
        # No tool calls — return final answer
        if not msg.tool_calls:
            return msg.content
        
        # Process tool calls
        messages.append(msg)  # Add assistant message with tool_calls
        
        for tool_call in msg.tool_calls:
            name = tool_call.function.name
            args = json.loads(tool_call.function.arguments)
            
            # Execute tool
            if name == "get_subscription":
                result = get_subscription_api(args["user_id"])
            elif name == "search_docs":
                result = search_docs_api(args["query"], args.get("category"))
            else:
                result = {"error": f"Unknown tool: {name}"}
            
            # Add tool result
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,
                "content": json.dumps(result),
            })

# Usage
answer = run_agent("What plan am I on? My ID is u-123.", "u-123")
```

## Parallel Tool Calls

```python
# GPT-4o can call multiple tools simultaneously
# "What's the weather in Tokyo AND New York?"
# → Two tool_calls in one response

for tool_call in msg.tool_calls:  # May have 2+ calls
    # Execute all in parallel
    pass
```

## Forcing Tool Use

```python
# Force specific tool
response = client.chat.completions.create(
    model="gpt-4o",
    messages=messages,
    tools=tools,
    tool_choice={"type": "function", "function": {"name": "search_docs"}},
)

# Force any tool (must use at least one)
tool_choice="required"

# No tools (disable)
tool_choice="none"
```

---

## Next: [Assistants API & Agents →](03_Assistants_Agents.md)
