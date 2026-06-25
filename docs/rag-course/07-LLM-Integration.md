# Module 7: LLM Integration

## Overview

The generation component of RAG. Choosing the right LLM and prompt strategy significantly impacts answer quality, cost, and latency.

---

## LLM Options (2024-2025)

| Model | Context Window | Cost (Input/Output per 1M) | Best For |
|-------|---------------|---------------------------|----------|
| GPT-4o | 128K | $2.50 / $10.00 | Best overall quality |
| GPT-4o-mini | 128K | $0.15 / $0.60 | Cost-effective production |
| Claude 3.5 Sonnet | 200K | $3.00 / $15.00 | Long context, reasoning |
| Claude 3.5 Haiku | 200K | $0.25 / $1.25 | Fast, cheap, good quality |
| Gemini 2.0 Flash | 1M | $0.075 / $0.30 | Huge context, multimodal |
| Llama 3.1 405B | 128K | Self-hosted | Privacy, no data sharing |
| Llama 3.1 70B | 128K | Self-hosted | Balance quality/cost |
| Mistral Large | 128K | $2.00 / $6.00 | European compliance |
| AWS Nova Pro | 300K | $0.80 / $3.20 | AWS ecosystem |
| AWS Nova Lite | 300K | $0.06 / $0.24 | Ultra-low cost |

---

## OpenAI Integration

```python
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

llm = ChatOpenAI(
    model="gpt-4o",
    temperature=0,          # Deterministic for RAG
    max_tokens=2048,
    timeout=30,
    max_retries=2,
)

# Structured RAG prompt
prompt = ChatPromptTemplate.from_messages([
    ("system", """You are a helpful assistant that answers questions based on the provided context.
Rules:
- Only use information from the context
- If the context doesn't contain the answer, say "I don't have enough information"
- Cite sources using [Source: filename] format
- Be concise and direct"""),
    ("human", """Context:
{context}

Question: {question}"""),
])

chain = prompt | llm | StrOutputParser()
```

---

## Anthropic Claude Integration

```python
from langchain_anthropic import ChatAnthropic

llm = ChatAnthropic(
    model="claude-sonnet-4-20250514",
    temperature=0,
    max_tokens=4096,
)

# Claude excels with XML-structured prompts
prompt = ChatPromptTemplate.from_messages([
    ("human", """<context>
{context}
</context>

<instructions>
Answer the question using ONLY the information in the context above.
If the answer isn't in the context, say so explicitly.
</instructions>

<question>{question}</question>"""),
])
```

---

## AWS Bedrock Integration

```python
from langchain_aws import ChatBedrock

# Claude via Bedrock
llm = ChatBedrock(
    model_id="anthropic.claude-sonnet-4-20250514-v1:0",
    region_name="us-east-1",
    model_kwargs={"temperature": 0, "max_tokens": 2048},
)

# Amazon Nova
llm_nova = ChatBedrock(
    model_id="amazon.nova-pro-v1:0",
    region_name="us-east-1",
    model_kwargs={"temperature": 0, "max_tokens": 2048},
)

# Bedrock Knowledge Bases (Managed RAG)
import boto3
bedrock_agent = boto3.client("bedrock-agent-runtime", region_name="us-east-1")

response = bedrock_agent.retrieve_and_generate(
    input={"text": "What is our refund policy?"},
    retrieveAndGenerateConfiguration={
        "type": "KNOWLEDGE_BASE",
        "knowledgeBaseConfiguration": {
            "knowledgeBaseId": "KB_ID",
            "modelArn": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-4-20250514-v1:0",
        }
    }
)
```

---

## Local LLM Integration (Ollama)

```python
from langchain_ollama import ChatOllama

# Run locally — no data leaves your machine
llm = ChatOllama(
    model="llama3.1:70b",  # or "mistral", "mixtral", "phi-3"
    temperature=0,
    num_ctx=8192,  # Context window
)

# For production local deployment with vLLM
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="http://localhost:8000/v1",  # vLLM server
    model="meta-llama/Llama-3.1-70B-Instruct",
    api_key="not-needed",
)
```

---

## Prompt Engineering for RAG

### 1. Basic RAG Prompt
```python
RAG_PROMPT = """Answer the question based on the following context.

Context:
{context}

Question: {question}

Answer:"""
```

### 2. Citation-Aware Prompt
```python
CITATION_PROMPT = """Answer the question using ONLY the provided sources.
For each claim, cite the source using [1], [2], etc.

Sources:
{numbered_sources}

Question: {question}

Provide your answer with inline citations:"""
```

### 3. Chain-of-Thought RAG
```python
COT_RAG_PROMPT = """Given the context below, answer the question step by step.

Context:
{context}

Question: {question}

Think through this step by step:
1. What relevant information is in the context?
2. How does it relate to the question?
3. What is the answer based on this information?

Answer:"""
```

### 4. Structured Output RAG
```python
from langchain_core.pydantic_v1 import BaseModel, Field

class RAGResponse(BaseModel):
    answer: str = Field(description="The answer to the question")
    confidence: float = Field(description="Confidence 0-1")
    sources: list[str] = Field(description="Source documents used")
    reasoning: str = Field(description="Brief reasoning")

structured_llm = llm.with_structured_output(RAGResponse)
response = structured_llm.invoke(prompt.format(context=ctx, question=q))
# response.answer, response.confidence, response.sources
```

---

## Streaming Responses

```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="gpt-4o", streaming=True)

# Stream tokens as they're generated
async def stream_rag_response(query: str, context: str):
    prompt = f"Context: {context}\n\nQuestion: {query}\nAnswer:"
    async for chunk in llm.astream(prompt):
        yield chunk.content

# FastAPI streaming endpoint
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()

@app.get("/ask")
async def ask(query: str):
    context = retriever.invoke(query)
    return StreamingResponse(
        stream_rag_response(query, context),
        media_type="text/event-stream",
    )
```

---

## Context Window Management

```python
import tiktoken

def fit_context_to_window(docs: list, query: str, max_tokens: int = 100000) -> str:
    """Fit as many relevant documents as possible within token limit."""
    encoder = tiktoken.encoding_for_model("gpt-4o")
    
    # Reserve tokens for query + response
    query_tokens = len(encoder.encode(query))
    reserved_for_response = 2048
    available = max_tokens - query_tokens - reserved_for_response
    
    context_parts = []
    total_tokens = 0
    
    for doc in docs:  # Assume docs are ranked by relevance
        doc_tokens = len(encoder.encode(doc.page_content))
        if total_tokens + doc_tokens > available:
            break
        context_parts.append(doc.page_content)
        total_tokens += doc_tokens
    
    return "\n\n---\n\n".join(context_parts)
```

---

## LLM Routing (Cost Optimization)

Route simple queries to cheap models, complex ones to powerful models:

```python
class LLMRouter:
    def __init__(self):
        self.cheap_llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
        self.powerful_llm = ChatOpenAI(model="gpt-4o", temperature=0)
        self.classifier = ChatOpenAI(model="gpt-4o-mini", temperature=0)
    
    def route_and_answer(self, query: str, context: str) -> str:
        # Classify complexity
        complexity = self.classifier.invoke(
            f"Rate query complexity 1-5 (1=simple lookup, 5=complex reasoning):\n{query}"
        ).content.strip()
        
        llm = self.powerful_llm if int(complexity) >= 4 else self.cheap_llm
        
        return llm.invoke(
            f"Context: {context}\nQuestion: {query}\nAnswer:"
        ).content
```

---

## Caching LLM Responses

```python
from langchain_community.cache import RedisSemanticCache
from langchain_openai import OpenAIEmbeddings
from langchain_core.globals import set_llm_cache

# Semantic cache: similar questions return cached answers
set_llm_cache(RedisSemanticCache(
    redis_url="redis://localhost:6379",
    embedding=OpenAIEmbeddings(model="text-embedding-3-small"),
    score_threshold=0.95,  # Only cache if very similar
))

# Now identical/similar queries hit cache instead of LLM
response = llm.invoke("What is RAG?")  # First call: hits LLM
response = llm.invoke("What is retrieval augmented generation?")  # Cache hit!
```

---

## Fallback & Error Handling

```python
from langchain_core.runnables import RunnableWithFallbacks

# Primary → Fallback chain
primary_llm = ChatOpenAI(model="gpt-4o", timeout=10)
fallback_llm = ChatAnthropic(model="claude-3-5-haiku-20241022", timeout=15)
local_fallback = ChatOllama(model="llama3.1:8b")

llm_with_fallback = primary_llm.with_fallbacks(
    [fallback_llm, local_fallback],
    exceptions_to_handle=(Exception,),
)
```

---

## Exercises

1. Compare response quality across GPT-4o, Claude 3.5 Sonnet, and Llama 3.1 70B on the same RAG pipeline
2. Implement structured output with citations and measure faithfulness
3. Build an LLM router that saves 60%+ cost while maintaining quality
4. Set up semantic caching and measure cache hit rates over 100 queries
5. Implement streaming RAG with a FastAPI endpoint
