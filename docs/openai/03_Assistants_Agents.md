# 3. Assistants API & Agents

## Theory

The Assistants API is OpenAI's managed agent framework. It handles:
- Conversation state (threads)
- Tool execution (code interpreter, file search, functions)
- File management
- Run lifecycle (queued → in_progress → completed)

```
Assistant (config) + Thread (conversation) = Run (execution)
```

## Create Assistant

```python
assistant = client.beta.assistants.create(
    name="Support Agent",
    instructions="""You are a customer support agent. 
    Search documentation before answering. 
    Be concise and empathetic.""",
    model="gpt-4o",
    tools=[
        {"type": "file_search"},      # RAG over uploaded files
        {"type": "code_interpreter"},  # Execute Python code
        {"type": "function", "function": {  # Custom tools
            "name": "get_subscription",
            "description": "Get user subscription details",
            "parameters": {
                "type": "object",
                "properties": {"user_id": {"type": "string"}},
                "required": ["user_id"],
            },
        }},
    ],
)
```

## File Search (Built-in RAG)

```python
# Create vector store and upload files
vector_store = client.beta.vector_stores.create(name="Support Docs")

# Upload files
file = client.files.create(file=open("docs/billing.pdf", "rb"), purpose="assistants")
client.beta.vector_stores.files.create(vector_store_id=vector_store.id, file_id=file.id)

# Attach to assistant
client.beta.assistants.update(
    assistant.id,
    tool_resources={"file_search": {"vector_store_ids": [vector_store.id]}},
)
```

## Conversations (Threads + Runs)

```python
# Create thread (conversation)
thread = client.beta.threads.create()

# Add message
client.beta.threads.messages.create(
    thread_id=thread.id,
    role="user",
    content="How do I cancel my subscription?",
)

# Run assistant
run = client.beta.threads.runs.create(
    thread_id=thread.id,
    assistant_id=assistant.id,
)

# Poll for completion (or use streaming)
import time
while run.status in ["queued", "in_progress"]:
    time.sleep(1)
    run = client.beta.threads.runs.retrieve(thread_id=thread.id, run_id=run.id)

# Handle tool calls
if run.status == "requires_action":
    tool_outputs = []
    for call in run.required_action.submit_tool_outputs.tool_calls:
        result = execute_tool(call.function.name, json.loads(call.function.arguments))
        tool_outputs.append({"tool_call_id": call.id, "output": json.dumps(result)})
    
    run = client.beta.threads.runs.submit_tool_outputs(
        thread_id=thread.id, run_id=run.id, tool_outputs=tool_outputs,
    )

# Get response
messages = client.beta.threads.messages.list(thread_id=thread.id)
print(messages.data[0].content[0].text.value)
```

## Streaming Runs

```python
from openai import AssistantEventHandler

class MyHandler(AssistantEventHandler):
    def on_text_delta(self, delta, snapshot):
        print(delta.value, end="", flush=True)
    
    def on_tool_call_created(self, tool_call):
        print(f"\n🔧 Using tool: {tool_call.type}")
    
    def on_tool_call_done(self, tool_call):
        if tool_call.type == "code_interpreter":
            for output in tool_call.code_interpreter.outputs:
                if output.type == "logs":
                    print(f"\n📊 Output: {output.logs}")

with client.beta.threads.runs.stream(
    thread_id=thread.id,
    assistant_id=assistant.id,
    event_handler=MyHandler(),
) as stream:
    stream.until_done()
```

---

## Next: [Fine-Tuning →](04_Fine_Tuning.md)
