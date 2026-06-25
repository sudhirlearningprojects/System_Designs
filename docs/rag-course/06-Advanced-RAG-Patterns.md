# Module 6: Advanced RAG Patterns

## Overview

Advanced RAG patterns go beyond simple retrieve-and-generate to handle complex queries, improve accuracy, and enable autonomous reasoning.

---

## 1. Agentic RAG (LangGraph)

The agent decides whether to retrieve, what to retrieve, and when to stop.

```python
from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from typing import TypedDict, Annotated
import operator

class AgentState(TypedDict):
    query: str
    documents: list
    generation: str
    retry_count: int

llm = ChatOpenAI(model="gpt-4o", temperature=0)

def should_retrieve(state: AgentState) -> str:
    """Decide if retrieval is needed."""
    response = llm.invoke(f"""Given this query, do you need to search for information?
    Query: {state['query']}
    Answer YES or NO.""")
    return "retrieve" if "YES" in response.content.upper() else "generate"

def retrieve(state: AgentState) -> AgentState:
    """Retrieve relevant documents."""
    docs = retriever.invoke(state["query"])
    return {"documents": docs}

def grade_documents(state: AgentState) -> str:
    """Grade retrieved documents for relevance."""
    relevant_docs = []
    for doc in state["documents"]:
        response = llm.invoke(
            f"Is this document relevant to '{state['query']}'?\n"
            f"Document: {doc.page_content[:500]}\nAnswer YES or NO."
        )
        if "YES" in response.content.upper():
            relevant_docs.append(doc)
    
    state["documents"] = relevant_docs
    if not relevant_docs:
        return "rewrite_query"
    return "generate"

def rewrite_query(state: AgentState) -> AgentState:
    """Rewrite query for better retrieval."""
    new_query = llm.invoke(
        f"Rewrite this query to get better search results: {state['query']}"
    ).content
    return {"query": new_query, "retry_count": state.get("retry_count", 0) + 1}

def generate(state: AgentState) -> AgentState:
    """Generate answer from context."""
    context = "\n".join([d.page_content for d in state["documents"]])
    response = llm.invoke(
        f"Answer based on context:\n{context}\n\nQuestion: {state['query']}"
    )
    return {"generation": response.content}

# Build graph
workflow = StateGraph(AgentState)
workflow.add_node("retrieve", retrieve)
workflow.add_node("grade", grade_documents)
workflow.add_node("rewrite", rewrite_query)
workflow.add_node("generate", generate)

workflow.set_conditional_entry_point(should_retrieve, {"retrieve": "retrieve", "generate": "generate"})
workflow.add_edge("retrieve", "grade")
workflow.add_conditional_edges("grade", grade_documents, {"rewrite_query": "rewrite", "generate": "generate"})
workflow.add_edge("rewrite", "retrieve")
workflow.add_edge("generate", END)

app = workflow.compile()
result = app.invoke({"query": "How does HNSW indexing work?", "retry_count": 0})
```

---

## 2. Corrective RAG (CRAG)

Automatically detects and corrects poor retrieval results:

```python
class CorrectiveRAG:
    """Implements CRAG: evaluates retrieval quality and falls back to web search."""
    
    def __init__(self, retriever, llm):
        self.retriever = retriever
        self.llm = llm
    
    def answer(self, query: str) -> str:
        # Step 1: Retrieve
        docs = self.retriever.invoke(query)
        
        # Step 2: Evaluate retrieval quality
        relevance_scores = self._score_relevance(query, docs)
        
        # Step 3: Decide action based on scores
        avg_score = sum(relevance_scores) / len(relevance_scores)
        
        if avg_score > 0.7:
            # CORRECT: Use retrieved docs directly
            context = self._extract_relevant(query, docs)
        elif avg_score > 0.4:
            # AMBIGUOUS: Refine with web search supplement
            web_results = self._web_search(query)
            context = self._merge_sources(docs, web_results)
        else:
            # INCORRECT: Fall back entirely to web search
            web_results = self._web_search(query)
            context = "\n".join([r["content"] for r in web_results])
        
        # Step 4: Generate with validated context
        return self._generate(query, context)
    
    def _score_relevance(self, query: str, docs: list) -> list[float]:
        scores = []
        for doc in docs:
            response = self.llm.invoke(
                f"Rate relevance 0-1.\nQuery: {query}\nDoc: {doc.page_content[:500]}\nScore:"
            )
            scores.append(float(response.content.strip()))
        return scores
    
    def _web_search(self, query: str) -> list:
        from langchain_community.tools import TavilySearchResults
        search = TavilySearchResults(max_results=3)
        return search.invoke(query)
```

---

## 3. Self-RAG (Self-Reflective RAG)

The model decides when to retrieve and self-evaluates its generations:

```python
class SelfRAG:
    """Self-RAG: Retrieve → Generate → Reflect → Iterate."""
    
    def __init__(self, retriever, llm):
        self.retriever = retriever
        self.llm = llm
    
    def answer(self, query: str, max_iterations: int = 3) -> str:
        for i in range(max_iterations):
            # Decide: do we need retrieval?
            if self._needs_retrieval(query):
                docs = self.retriever.invoke(query)
                context = "\n".join([d.page_content for d in docs])
            else:
                context = ""
            
            # Generate
            response = self._generate(query, context)
            
            # Self-critique
            critique = self._critique(query, response, context)
            
            if critique["is_supported"] and critique["is_useful"]:
                return response
            
            # Refine query based on critique
            query = self._refine_query(query, critique["feedback"])
        
        return response  # Return best effort
    
    def _needs_retrieval(self, query: str) -> bool:
        response = self.llm.invoke(
            f"Does answering this require external knowledge? Query: {query}\nYES/NO"
        )
        return "YES" in response.content.upper()
    
    def _critique(self, query: str, answer: str, context: str) -> dict:
        response = self.llm.invoke(f"""Evaluate this answer:
        Query: {query}
        Context: {context[:2000]}
        Answer: {answer}
        
        Return JSON: {{"is_supported": bool, "is_useful": bool, "feedback": "..."}}""")
        return parse_json(response.content)
```

---

## 4. Graph RAG (Knowledge Graph + RAG)

Combines structured knowledge graphs with vector retrieval for complex multi-hop questions:

```python
from langchain_community.graphs import Neo4jGraph
from langchain_openai import ChatOpenAI

# Connect to Neo4j knowledge graph
graph = Neo4jGraph(url="bolt://localhost:7687", username="neo4j", password="password")

class GraphRAG:
    """Combines vector retrieval with knowledge graph traversal."""
    
    def __init__(self, vectorstore, graph, llm):
        self.vectorstore = vectorstore
        self.graph = graph
        self.llm = llm
    
    def answer(self, query: str) -> str:
        # Step 1: Extract entities from query
        entities = self._extract_entities(query)
        
        # Step 2: Retrieve from knowledge graph (structured)
        graph_context = self._graph_retrieval(entities)
        
        # Step 3: Retrieve from vector store (unstructured)
        vector_context = self.vectorstore.similarity_search(query, k=5)
        
        # Step 4: Combine both contexts
        combined_context = f"""
        Structured Knowledge (Graph):
        {graph_context}
        
        Unstructured Knowledge (Documents):
        {chr(10).join([d.page_content for d in vector_context])}
        """
        
        # Step 5: Generate
        return self.llm.invoke(
            f"Answer using both structured and unstructured context:\n"
            f"{combined_context}\n\nQuestion: {query}"
        ).content
    
    def _extract_entities(self, query: str) -> list[str]:
        response = self.llm.invoke(
            f"Extract key entities from: {query}\nReturn as JSON array."
        )
        return parse_json(response.content)
    
    def _graph_retrieval(self, entities: list[str]) -> str:
        results = []
        for entity in entities:
            # Find related nodes and relationships
            cypher = f"""
            MATCH (n)-[r]-(m) 
            WHERE n.name CONTAINS '{entity}' 
            RETURN n.name, type(r), m.name LIMIT 10
            """
            result = self.graph.query(cypher)
            results.extend(result)
        return str(results)
```

### Microsoft GraphRAG (2024)
```python
# Microsoft's approach: Build community summaries from knowledge graph
# pip install graphrag

from graphrag.index import create_pipeline
from graphrag.query import LocalSearch, GlobalSearch

# Index: Extract entities → Build graph → Detect communities → Summarize
# Query: Route to local (specific) or global (broad) search

# Local search: For specific entity questions
local_search = LocalSearch(llm=llm, context_builder=context_builder)
result = local_search.search("What partnerships does Company X have?")

# Global search: For broad thematic questions  
global_search = GlobalSearch(llm=llm, context_builder=context_builder)
result = global_search.search("What are the major trends in AI?")
```

---

## 5. RAG Fusion

Multiple queries → parallel retrieval → reciprocal rank fusion:

```python
from langchain_core.runnables import RunnablePassthrough

def reciprocal_rank_fusion(results_lists: list[list], k: int = 60) -> list:
    """Combine multiple ranked lists using RRF."""
    fused_scores = {}
    
    for results in results_lists:
        for rank, doc in enumerate(results):
            doc_id = doc.page_content[:100]  # Use content prefix as ID
            if doc_id not in fused_scores:
                fused_scores[doc_id] = {"doc": doc, "score": 0}
            fused_scores[doc_id]["score"] += 1 / (rank + k)
    
    # Sort by fused score
    sorted_results = sorted(fused_scores.values(), key=lambda x: x["score"], reverse=True)
    return [item["doc"] for item in sorted_results]

class RAGFusion:
    def __init__(self, retriever, llm):
        self.retriever = retriever
        self.llm = llm
    
    def retrieve(self, query: str, k: int = 5) -> list:
        # Generate multiple query perspectives
        queries = self._generate_queries(query)
        
        # Parallel retrieval
        all_results = [self.retriever.invoke(q) for q in queries]
        
        # Reciprocal Rank Fusion
        fused = reciprocal_rank_fusion(all_results)
        return fused[:k]
    
    def _generate_queries(self, query: str) -> list[str]:
        response = self.llm.invoke(
            f"Generate 4 different search queries to answer: {query}\nReturn as JSON array."
        )
        return [query] + parse_json(response.content)
```

---

## 6. Adaptive RAG (Router-Based)

Route queries to the most appropriate retrieval strategy:

```python
from langchain_core.prompts import ChatPromptTemplate

class AdaptiveRAG:
    """Routes queries to optimal retrieval strategy."""
    
    def __init__(self, vectorstore, llm):
        self.vectorstore = vectorstore
        self.llm = llm
        self.strategies = {
            "simple_lookup": self._simple_retrieval,
            "multi_step": self._multi_step_retrieval,
            "aggregation": self._aggregation_retrieval,
            "comparison": self._comparison_retrieval,
            "no_retrieval": self._direct_answer,
        }
    
    def answer(self, query: str) -> str:
        # Classify query type
        strategy = self._classify_query(query)
        
        # Execute appropriate strategy
        return self.strategies[strategy](query)
    
    def _classify_query(self, query: str) -> str:
        response = self.llm.invoke(f"""Classify this query into one category:
        - simple_lookup: Direct factual question
        - multi_step: Requires multiple pieces of information
        - aggregation: Needs to combine/summarize multiple sources
        - comparison: Comparing two or more things
        - no_retrieval: General knowledge, no docs needed
        
        Query: {query}
        Category:""")
        return response.content.strip().lower()
    
    def _multi_step_retrieval(self, query: str) -> str:
        """Decompose and retrieve for each sub-question."""
        sub_questions = self._decompose(query)
        all_context = []
        for sq in sub_questions:
            docs = self.vectorstore.similarity_search(sq, k=3)
            all_context.extend(docs)
        
        context = "\n".join([d.page_content for d in all_context])
        return self.llm.invoke(f"Context: {context}\n\nAnswer: {query}").content
```

---

## 7. Iterative Retrieval (Multi-Turn)

For complex questions that require multiple rounds of retrieval:

```python
class IterativeRetriever:
    """Retrieves iteratively, refining based on intermediate findings."""
    
    def retrieve_and_answer(self, query: str, max_rounds: int = 3) -> str:
        accumulated_context = []
        current_query = query
        
        for round_num in range(max_rounds):
            # Retrieve
            docs = self.retriever.invoke(current_query)
            accumulated_context.extend(docs)
            
            # Check if we have enough information
            context_text = "\n".join([d.page_content for d in accumulated_context])
            
            completeness = self.llm.invoke(f"""
            Can you fully answer this question with the available context?
            Question: {query}
            Context: {context_text[:3000]}
            
            Reply: SUFFICIENT or NEED_MORE with what's missing.""")
            
            if "SUFFICIENT" in completeness.content:
                break
            
            # Generate follow-up query for missing information
            current_query = self.llm.invoke(
                f"Original: {query}\nMissing: {completeness.content}\n"
                f"Generate a search query for the missing information:"
            ).content
        
        # Final generation
        return self._generate(query, accumulated_context)
```

---

## 8. Late Chunking (2024 - Jina AI)

Embed the entire document first, then chunk — preserving cross-chunk context:

```python
# Traditional: Chunk → Embed each chunk independently (loses context)
# Late Chunking: Embed full doc → Extract chunk embeddings from token positions

# This preserves references like "it", "the system", "as mentioned above"
# that would lose meaning if embedded in isolation

from transformers import AutoModel, AutoTokenizer
import torch

model = AutoModel.from_pretrained("jinaai/jina-embeddings-v2-base-en", trust_remote_code=True)
tokenizer = AutoTokenizer.from_pretrained("jinaai/jina-embeddings-v2-base-en")

def late_chunking(text: str, chunk_boundaries: list[tuple[int, int]]) -> list:
    """Embed full text, then extract embeddings for each chunk span."""
    # Tokenize full document
    inputs = tokenizer(text, return_tensors="pt", max_length=8192, truncation=True)
    
    with torch.no_grad():
        outputs = model(**inputs)
        token_embeddings = outputs.last_hidden_state[0]  # [seq_len, hidden_dim]
    
    # Extract chunk embeddings by averaging token embeddings within boundaries
    chunk_embeddings = []
    for start, end in chunk_boundaries:
        chunk_emb = token_embeddings[start:end].mean(dim=0)
        chunk_embeddings.append(chunk_emb.numpy())
    
    return chunk_embeddings
```

---

## Pattern Selection Guide

| Query Type | Best Pattern | Why |
|-----------|-------------|-----|
| Simple factual | Naive RAG + Rerank | Fast, sufficient |
| Ambiguous/vague | HyDE + Multi-Query | Better recall |
| Multi-hop reasoning | Agentic RAG or Iterative | Needs multiple steps |
| Entity-relationship | Graph RAG | Structured relationships |
| Broad thematic | RAG Fusion | Diverse perspectives |
| Quality-critical | Self-RAG + CRAG | Self-validation |
| Mixed complexity | Adaptive RAG | Routes to best strategy |

---

## Exercises

1. Implement Corrective RAG with web search fallback and measure answer quality improvement
2. Build a Graph RAG system using Neo4j for a knowledge domain of your choice
3. Compare RAG Fusion vs single-query retrieval on 20 complex questions
4. Implement an Adaptive RAG router and evaluate routing accuracy
5. Build a full Agentic RAG pipeline with LangGraph including retry and self-correction
