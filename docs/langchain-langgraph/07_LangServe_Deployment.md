# 7. LangServe & Deployment

## LangServe (Deploy Chains as REST APIs)

```python
# pip install langserve[all]

from fastapi import FastAPI
from langserve import add_routes
from langchain_anthropic import ChatAnthropic
from langchain_core.prompts import ChatPromptTemplate

app = FastAPI(title="AI Agent API")

# Define chain
prompt = ChatPromptTemplate.from_template("You are a helpful assistant. {query}")
model = ChatAnthropic(model="claude-sonnet-4-20250514")
chain = prompt | model

# Add routes (auto-generates /invoke, /stream, /batch endpoints)
add_routes(app, chain, path="/chat")

# Run: uvicorn server:app --host 0.0.0.0 --port 8000
```

### Client Usage

```python
from langserve import RemoteRunnable

# Connect to deployed chain
chain = RemoteRunnable("http://localhost:8000/chat")

# Invoke
result = chain.invoke({"query": "Hello!"})

# Stream
for chunk in chain.stream({"query": "Tell me a story"}):
    print(chunk, end="")

# Batch
results = chain.batch([{"query": "Hi"}, {"query": "Bye"}])
```

---

## LangGraph Cloud (Managed Deployment)

### langgraph.json Configuration

```json
{
  "dependencies": ["./"],
  "graphs": {
    "support_agent": "./agent.py:graph"
  },
  "env": ".env"
}
```

### Deploy

```bash
# Install CLI
pip install langgraph-cli

# Test locally
langgraph dev

# Deploy to LangGraph Cloud
langgraph deploy --app ./my-agent
```

### LangGraph Cloud SDK

```python
from langgraph_sdk import get_client

client = get_client(url="https://your-deployment.langgraph.cloud")

# Create thread (conversation)
thread = await client.threads.create()

# Send message
run = await client.runs.create(
    thread["thread_id"],
    "support_agent",
    input={"messages": [{"role": "user", "content": "Help me cancel"}]},
)

# Stream response
async for event in client.runs.stream(thread["thread_id"], run["run_id"]):
    print(event)

# Get thread state (for debugging)
state = await client.threads.get_state(thread["thread_id"])
```

---

## Docker Deployment

```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

```yaml
# docker-compose.yml
services:
  agent:
    build: .
    ports:
      - "8000:8000"
    environment:
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - LANGCHAIN_TRACING_V2=true
      - LANGCHAIN_API_KEY=${LANGCHAIN_API_KEY}
      - DATABASE_URL=postgresql://user:pass@postgres:5432/langgraph
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: langgraph
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  pgdata:
```

---

## Production Configuration

```python
from langchain_anthropic import ChatAnthropic
from langchain_core.rate_limiters import InMemoryRateLimiter
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver

# Rate limiting
rate_limiter = InMemoryRateLimiter(requests_per_second=50)

# Model with production settings
model = ChatAnthropic(
    model="claude-sonnet-4-20250514",
    temperature=0,
    max_tokens=4096,
    timeout=30,
    max_retries=3,
    rate_limiter=rate_limiter,
)

# Persistent checkpointer
async def get_checkpointer():
    return await AsyncPostgresSaver.from_conn_string(
        "postgresql://user:pass@host:5432/langgraph",
        pool_size=20,
    )

# Compile for production
app = graph.compile(
    checkpointer=await get_checkpointer(),
    interrupt_before=["dangerous_actions"],
)
```

---

## Next: [Production Patterns →](08_Production_Patterns.md)
