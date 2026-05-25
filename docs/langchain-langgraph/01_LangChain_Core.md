# 1. LangChain Core

## Theory: What is LangChain?

LangChain is a framework for building applications powered by language models. It provides:

1. **Abstractions**: Unified interfaces for different LLM providers (swap Claude for GPT with one line)
2. **Composition**: LCEL (LangChain Expression Language) for chaining operations
3. **Ecosystem**: 700+ integrations (vector stores, tools, document loaders)

### Core Abstraction: Runnables

Everything in LangChain is a **Runnable** — an object with `.invoke()`, `.stream()`, `.batch()`, and `.ainvoke()` methods. This means any component can be:
- Chained with `|` (pipe operator)
- Streamed token-by-token
- Batched for parallel processing
- Called asynchronously

```
Runnable Interface:
  .invoke(input)        → Single input → single output
  .stream(input)        → Single input → stream of outputs
  .batch([inputs])      → Multiple inputs → multiple outputs (parallel)
  .ainvoke(input)       → Async version of invoke
  .astream(input)       → Async version of stream
```

### LCEL (LangChain Expression Language) — Theory

LCEL is a declarative way to compose Runnables:

```python
chain = prompt | model | parser
#       ↑        ↑       ↑
#    Runnable  Runnable  Runnable
```

**Why LCEL over plain Python functions?**
- **Streaming**: Automatically streams through the entire chain
- **Parallelism**: `RunnableParallel` runs branches concurrently
- **Fallbacks**: `.with_fallbacks()` for graceful degradation
- **Retry**: `.with_retry()` for transient failures
- **Tracing**: Every step is automatically traced in LangSmith
- **Serialization**: Chains can be saved/loaded as JSON

### Message Types Theory

```
LLMs communicate via messages (not raw text):

┌───────────────────────────────────────────────────────┐
│  SystemMessage:  Persistent instructions (persona, rules)  │
│  HumanMessage:   User input                                │
│  AIMessage:      Model response (may contain tool_calls)   │
│  ToolMessage:    Result of executing a tool                 │
└───────────────────────────────────────────────────────┘

Conversation flow:
  [System] → [Human] → [AI] → [Human] → [AI(tool_call)] → [Tool] → [AI]
```

---

## Chat Models

```python
from langchain_anthropic import ChatAnthropic
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage

# Initialize models
claude = ChatAnthropic(model="claude-sonnet-4-20250514", temperature=0)
gpt4 = ChatOpenAI(model="gpt-4o", temperature=0)

# Basic invocation
response = claude.invoke([
    SystemMessage(content="You are a helpful coding assistant."),
    HumanMessage(content="Write a Python fibonacci function."),
])
print(response.content)

# Streaming
for chunk in claude.stream([HumanMessage(content="Explain Docker in 3 sentences.")]):
    print(chunk.content, end="", flush=True)

# Async
import asyncio
async def main():
    response = await claude.ainvoke([HumanMessage(content="Hello!")])
    print(response.content)

asyncio.run(main())

# Batch (parallel processing)
messages_batch = [
    [HumanMessage(content="What is Python?")],
    [HumanMessage(content="What is JavaScript?")],
    [HumanMessage(content="What is Rust?")],
]
responses = claude.batch(messages_batch, config={"max_concurrency": 3})
```

### Model Configuration

```python
model = ChatAnthropic(
    model="claude-sonnet-4-20250514",
    temperature=0.0,          # Deterministic
    max_tokens=4096,
    timeout=30,               # Request timeout
    max_retries=3,            # Auto-retry on failure
    api_key="...",            # Or use ANTHROPIC_API_KEY env var
    # Callbacks for observability
    callbacks=[LangSmithCallbackHandler()],
)

# With structured output (force JSON schema)
from pydantic import BaseModel, Field

class Classification(BaseModel):
    intent: str = Field(description="User intent category")
    confidence: float = Field(description="Confidence score 0-1")
    entities: list[str] = Field(description="Extracted entities")

structured_model = claude.with_structured_output(Classification)
result = structured_model.invoke("I want to cancel my Pro subscription")
print(result.intent)       # "cancellation"
print(result.confidence)   # 0.95
```

---

## Prompt Templates

```python
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

# Simple template
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a {role}. Respond in {language}."),
    ("human", "{query}"),
])

# Invoke with variables
messages = prompt.invoke({"role": "Python expert", "language": "English", "query": "Explain decorators"})
response = claude.invoke(messages)

# With message history (for multi-turn)
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant."),
    MessagesPlaceholder(variable_name="history"),
    ("human", "{input}"),
])

messages = prompt.invoke({
    "history": [
        HumanMessage(content="My name is Alice"),
        AIMessage(content="Hello Alice! How can I help?"),
    ],
    "input": "What's my name?",
})

# Few-shot prompt
from langchain_core.prompts import FewShotChatMessagePromptTemplate

examples = [
    {"input": "happy", "output": "sad"},
    {"input": "tall", "output": "short"},
]

few_shot = FewShotChatMessagePromptTemplate(
    example_prompt=ChatPromptTemplate.from_messages([
        ("human", "{input}"),
        ("ai", "{output}"),
    ]),
    examples=examples,
)

prompt = ChatPromptTemplate.from_messages([
    ("system", "Give the antonym of the word."),
    few_shot,
    ("human", "{input}"),
])
```

---

## LCEL (LangChain Expression Language)

The composable, streaming-first way to build chains.

### Basic Chain

```python
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

prompt = ChatPromptTemplate.from_template("Tell me a joke about {topic}")
model = ChatAnthropic(model="claude-sonnet-4-20250514")
parser = StrOutputParser()

# Chain with pipe operator
chain = prompt | model | parser

# Invoke
result = chain.invoke({"topic": "programming"})

# Stream
for chunk in chain.stream({"topic": "AI"}):
    print(chunk, end="")

# Batch
results = chain.batch([{"topic": "cats"}, {"topic": "dogs"}])

# Async
result = await chain.ainvoke({"topic": "space"})
```

### Complex Chain (RAG)

```python
from langchain_core.runnables import RunnablePassthrough, RunnableParallel

# RAG chain
retriever = vector_store.as_retriever(search_kwargs={"k": 5})

def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)

rag_prompt = ChatPromptTemplate.from_template("""Answer based on context:

Context: {context}

Question: {question}

Answer:""")

rag_chain = (
    RunnableParallel(
        context=retriever | format_docs,
        question=RunnablePassthrough(),
    )
    | rag_prompt
    | model
    | StrOutputParser()
)

answer = rag_chain.invoke("How do I cancel my subscription?")
```

### Branching and Routing

```python
from langchain_core.runnables import RunnableBranch, RunnableLambda

# Route based on input
def classify(input_dict):
    query = input_dict["query"]
    if "billing" in query.lower():
        return "billing"
    elif "technical" in query.lower():
        return "technical"
    return "general"

billing_chain = billing_prompt | model | parser
technical_chain = technical_prompt | model | parser
general_chain = general_prompt | model | parser

branch = RunnableBranch(
    (lambda x: classify(x) == "billing", billing_chain),
    (lambda x: classify(x) == "technical", technical_chain),
    general_chain,  # Default
)

result = branch.invoke({"query": "I was charged twice"})
```

### Adding Fallbacks

```python
# Fallback to cheaper model if primary fails
primary = ChatAnthropic(model="claude-sonnet-4-20250514")
fallback = ChatAnthropic(model="claude-3-5-haiku-20241022")

model_with_fallback = primary.with_fallbacks([fallback])

chain = prompt | model_with_fallback | parser
```

### Retry and Rate Limiting

```python
from langchain_core.runnables import RunnableConfig

# Retry configuration
chain_with_retry = chain.with_retry(
    stop_after_attempt=3,
    wait_exponential_jitter=True,
)

# Rate limiting
from langchain_core.rate_limiters import InMemoryRateLimiter

rate_limiter = InMemoryRateLimiter(requests_per_second=10)
model = ChatAnthropic(model="claude-sonnet-4-20250514", rate_limiter=rate_limiter)
```

---

## Output Parsers

```python
from langchain_core.output_parsers import JsonOutputParser, PydanticOutputParser
from pydantic import BaseModel, Field

# JSON output
class ActionPlan(BaseModel):
    steps: list[str] = Field(description="List of action steps")
    estimated_time: str = Field(description="Estimated completion time")
    confidence: float = Field(description="Confidence in the plan")

parser = PydanticOutputParser(pydantic_object=ActionPlan)

prompt = ChatPromptTemplate.from_template(
    "Create an action plan for: {task}\n\n{format_instructions}"
)

chain = prompt.partial(format_instructions=parser.get_format_instructions()) | model | parser
result = chain.invoke({"task": "Deploy a new microservice"})
print(result.steps)  # ['Set up CI/CD', 'Write Dockerfile', ...]

# Streaming JSON parser (parse as tokens arrive)
from langchain_core.output_parsers import JsonOutputParser

chain = prompt | model | JsonOutputParser()
async for chunk in chain.astream({"task": "Plan a migration"}):
    print(chunk)  # Partial JSON as it streams
```

---

## Message Types

```python
from langchain_core.messages import (
    SystemMessage,      # System instructions
    HumanMessage,       # User input
    AIMessage,          # Model response
    ToolMessage,        # Tool execution result
    AIMessageChunk,     # Streaming chunk
)

# Messages with metadata
msg = HumanMessage(
    content="Cancel my subscription",
    additional_kwargs={"user_id": "u-123"},
    name="Alice",
)

# Multi-modal messages
from langchain_core.messages import HumanMessage

msg = HumanMessage(content=[
    {"type": "text", "text": "What's in this image?"},
    {"type": "image_url", "image_url": {"url": "https://example.com/img.png"}},
])
```

---

## Next: [RAG with LangChain →](02_RAG.md)
