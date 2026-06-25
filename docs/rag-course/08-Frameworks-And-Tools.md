# Module 8: Frameworks & Tools

## Overview

Modern RAG frameworks abstract complexity and provide production-ready components. Choose based on your use case, team expertise, and deployment requirements.

## Framework Comparison

| Framework | Best For | Maturity | Learning Curve | Flexibility |
|-----------|----------|----------|----------------|-------------|
| LangChain v0.3 | General RAG, agents | ⭐⭐⭐⭐⭐ | Medium | High |
| LlamaIndex v0.11 | Data-intensive RAG | ⭐⭐⭐⭐⭐ | Medium | High |
| Haystack 2.x | Pipeline-based RAG | ⭐⭐⭐⭐ | Low | Medium |
| Semantic Kernel | Enterprise/.NET | ⭐⭐⭐ | Medium | Medium |
| DSPy | Prompt optimization | ⭐⭐⭐ | High | High |
| AWS Bedrock KB | Managed RAG | ⭐⭐⭐⭐ | Low | Low |

---

## LangChain v0.3 (LCEL)

LangChain Expression Language (LCEL) provides composable, streaming-native pipelines.

### Complete RAG Pipeline
```python
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough, RunnableParallel
from langchain_core.output_parsers import StrOutputParser

# Components
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma(embedding_function=embeddings, persist_directory="./db")
retriever = vectorstore.as_retriever(search_type="mmr", search_kwargs={"k": 5})
llm = ChatOpenAI(model="gpt-4o", temperature=0)

prompt = ChatPromptTemplate.from_template("""
Answer based on context. Cite sources.
Context: {context}
Question: {question}
Answer:""")

def format_docs(docs):
    return "\n\n".join(f"[{d.metadata.get('source', 'unknown')}]: {d.page_content}" for d in docs)

# LCEL chain - composable and streaming
rag_chain = (
    RunnableParallel(context=retriever | format_docs, question=RunnablePassthrough())
    | prompt
    | llm
    | StrOutputParser()
)

# Usage
response = rag_chain.invoke("How do I deploy to production?")

# Streaming
for chunk in rag_chain.stream("How do I deploy to production?"):
    print(chunk, end="", flush=True)
```

### LangGraph (Agentic RAG)
```python
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode
from langchain_core.tools import tool

@tool
def search_docs(query: str) -> str:
    """Search documentation for relevant information."""
    docs = retriever.invoke(query)
    return format_docs(docs)

@tool
def search_web(query: str) -> str:
    """Search the web for current information."""
    from langchain_community.tools import TavilySearchResults
    return TavilySearchResults(max_results=3).invoke(query)

# Build agentic RAG with tool selection
tools = [search_docs, search_web]
llm_with_tools = llm.bind_tools(tools)

# Agent decides which tool to use based on query
```

---

## LlamaIndex v0.11

LlamaIndex excels at data ingestion and complex retrieval strategies.

### Basic RAG
```python
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader, Settings
from llama_index.llms.openai import OpenAI
from llama_index.embeddings.openai import OpenAIEmbedding

# Configure global settings
Settings.llm = OpenAI(model="gpt-4o", temperature=0)
Settings.embed_model = OpenAIEmbedding(model_name="text-embedding-3-small")

# Load and index
documents = SimpleDirectoryReader("./data").load_data()
index = VectorStoreIndex.from_documents(documents)

# Query
query_engine = index.as_query_engine(similarity_top_k=5)
response = query_engine.query("What is the deployment process?")
print(response.response)
print(response.source_nodes)  # Source documents with scores
```

### Sub-Question Query Engine (Multi-Step)
```python
from llama_index.core.query_engine import SubQuestionQueryEngine
from llama_index.core.tools import QueryEngineTool, ToolMetadata

# Multiple indexes for different data sources
docs_tool = QueryEngineTool(
    query_engine=docs_index.as_query_engine(),
    metadata=ToolMetadata(name="documentation", description="Product documentation"),
)
api_tool = QueryEngineTool(
    query_engine=api_index.as_query_engine(),
    metadata=ToolMetadata(name="api_reference", description="API reference docs"),
)

# Automatically decomposes complex queries into sub-questions
engine = SubQuestionQueryEngine.from_defaults(query_engine_tools=[docs_tool, api_tool])
response = engine.query("Compare the REST API and SDK authentication methods")
```

### Recursive Retrieval (Hierarchical)
```python
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.schema import IndexNode

# Create summary index over document summaries
# Then drill down into detailed chunks

# Level 1: Document summaries
summary_nodes = [IndexNode(text=doc.summary, index_id=doc.id) for doc in documents]
summary_index = VectorStoreIndex(summary_nodes)

# Level 2: Detailed chunks per document
# When a summary matches, retrieve detailed chunks from that document
```

### Property Graph Index (2024)
```python
from llama_index.core.indices.property_graph import PropertyGraphIndex

# Automatically extracts entities and relationships
index = PropertyGraphIndex.from_documents(
    documents,
    llm=OpenAI(model="gpt-4o-mini"),
    embed_model=OpenAIEmbedding(),
)

# Query with graph-aware retrieval
query_engine = index.as_query_engine(include_text=True)
response = query_engine.query("What are the relationships between services?")
```

---

## Haystack 2.x

Pipeline-based architecture with strong typing and modular components.

```python
from haystack import Pipeline
from haystack.components.converters import PyPDFToDocument
from haystack.components.preprocessors import DocumentCleaner, DocumentSplitter
from haystack.components.embedders import OpenAIDocumentEmbedder, OpenAITextEmbedder
from haystack.components.writers import DocumentWriter
from haystack.components.retrievers.in_memory import InMemoryEmbeddingRetriever
from haystack.components.builders import PromptBuilder
from haystack.components.generators import OpenAIGenerator
from haystack.document_stores.in_memory import InMemoryDocumentStore

# Document Store
store = InMemoryDocumentStore()

# Indexing Pipeline
indexing = Pipeline()
indexing.add_component("converter", PyPDFToDocument())
indexing.add_component("cleaner", DocumentCleaner())
indexing.add_component("splitter", DocumentSplitter(split_by="sentence", split_length=5))
indexing.add_component("embedder", OpenAIDocumentEmbedder(model="text-embedding-3-small"))
indexing.add_component("writer", DocumentWriter(document_store=store))

indexing.connect("converter", "cleaner")
indexing.connect("cleaner", "splitter")
indexing.connect("splitter", "embedder")
indexing.connect("embedder", "writer")

indexing.run({"converter": {"sources": ["document.pdf"]}})

# Query Pipeline
template = """Answer based on context:\n{% for doc in documents %}{{ doc.content }}\n{% endfor %}\nQuestion: {{ question }}"""

query_pipeline = Pipeline()
query_pipeline.add_component("embedder", OpenAITextEmbedder(model="text-embedding-3-small"))
query_pipeline.add_component("retriever", InMemoryEmbeddingRetriever(document_store=store, top_k=5))
query_pipeline.add_component("prompt", PromptBuilder(template=template))
query_pipeline.add_component("llm", OpenAIGenerator(model="gpt-4o"))

query_pipeline.connect("embedder.embedding", "retriever.query_embedding")
query_pipeline.connect("retriever", "prompt.documents")
query_pipeline.connect("prompt", "llm")

result = query_pipeline.run({
    "embedder": {"text": "How to deploy?"},
    "prompt": {"question": "How to deploy?"},
})
```

---

## DSPy (Prompt Optimization)

DSPy automatically optimizes prompts and few-shot examples for RAG:

```python
import dspy
from dspy.retrieve import ChromadbRM

# Configure
lm = dspy.LM("openai/gpt-4o-mini", temperature=0)
retriever = ChromadbRM(collection_name="docs", persist_directory="./db", k=5)
dspy.configure(lm=lm, rm=retriever)

# Define RAG as a DSPy module
class RAG(dspy.Module):
    def __init__(self):
        self.retrieve = dspy.Retrieve(k=5)
        self.generate = dspy.ChainOfThought("context, question -> answer")
    
    def forward(self, question):
        context = self.retrieve(question).passages
        return self.generate(context=context, question=question)

# Compile with optimization (auto-tunes prompts)
from dspy.teleprompt import BootstrapFewShot

trainset = [
    dspy.Example(question="What is RAG?", answer="RAG combines retrieval with generation..."),
    # ... more examples
]

optimizer = BootstrapFewShot(metric=dspy.evaluate.answer_exact_match)
optimized_rag = optimizer.compile(RAG(), trainset=trainset)

# Use optimized pipeline
result = optimized_rag("How does vector search work?")
```

---

## AWS Bedrock Knowledge Bases (Managed RAG)

Zero-infrastructure RAG:

```python
import boto3

bedrock_agent = boto3.client("bedrock-agent-runtime", region_name="us-east-1")

# Retrieve and Generate (fully managed)
response = bedrock_agent.retrieve_and_generate(
    input={"text": "What is our refund policy?"},
    retrieveAndGenerateConfiguration={
        "type": "KNOWLEDGE_BASE",
        "knowledgeBaseConfiguration": {
            "knowledgeBaseId": "YOUR_KB_ID",
            "modelArn": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-4-20250514-v1:0",
            "retrievalConfiguration": {
                "vectorSearchConfiguration": {
                    "numberOfResults": 5,
                    "overrideSearchType": "HYBRID",  # SEMANTIC or HYBRID
                }
            },
        }
    }
)

answer = response["output"]["text"]
citations = response["citations"]  # Source references
```

### Setting Up Bedrock Knowledge Base
```python
bedrock_agent_client = boto3.client("bedrock-agent", region_name="us-east-1")

# Create Knowledge Base
kb = bedrock_agent_client.create_knowledge_base(
    name="product-docs-kb",
    roleArn="arn:aws:iam::123456789:role/BedrockKBRole",
    knowledgeBaseConfiguration={
        "type": "VECTOR",
        "vectorKnowledgeBaseConfiguration": {
            "embeddingModelArn": "arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-embed-text-v2:0",
        }
    },
    storageConfiguration={
        "type": "OPENSEARCH_SERVERLESS",
        "opensearchServerlessConfiguration": {
            "collectionArn": "arn:aws:aoss:us-east-1:123456789:collection/xyz",
            "vectorIndexName": "docs-index",
            "fieldMapping": {
                "vectorField": "embedding",
                "textField": "text",
                "metadataField": "metadata",
            }
        }
    }
)

# Add S3 data source
bedrock_agent_client.create_data_source(
    knowledgeBaseId=kb["knowledgeBase"]["knowledgeBaseId"],
    name="s3-docs",
    dataSourceConfiguration={
        "type": "S3",
        "s3Configuration": {"bucketArn": "arn:aws:s3:::my-docs-bucket"}
    },
    vectorIngestionConfiguration={
        "chunkingConfiguration": {
            "chunkingStrategy": "SEMANTIC",  # or FIXED_SIZE, HIERARCHICAL
            "semanticChunkingConfiguration": {
                "maxTokens": 1000,
                "bufferSize": 0,
                "breakpointPercentileThreshold": 95,
            }
        }
    }
)
```

---

## Framework Selection Guide

| Scenario | Recommendation |
|----------|---------------|
| Prototype/MVP | LangChain + ChromaDB |
| Complex data pipelines | LlamaIndex |
| Enterprise production | LangChain + LangGraph or Haystack |
| AWS-native deployment | Bedrock Knowledge Bases |
| Prompt optimization research | DSPy |
| .NET/C# team | Semantic Kernel |
| Maximum control | Custom (no framework) |

---

## Exercises

1. Implement the same RAG pipeline in LangChain and LlamaIndex — compare developer experience
2. Build a Haystack pipeline with custom components for domain-specific preprocessing
3. Use DSPy to optimize your RAG prompt and measure quality improvement
4. Set up AWS Bedrock Knowledge Base with S3 data source
5. Build an agentic RAG with LangGraph that can search docs, web, and databases
