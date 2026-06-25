# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 7C: Model Management, A/B Testing & Hybrid Strategy

---

## Model Versioning & Hot-Swap

In production, you need to update embedding models without downtime:

```java
package com.rag.flink.inference;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;
import java.nio.file.WatchService;

/**
 * Supports hot-swapping ONNX models without restarting the Flink job.
 * Watches a directory for new model versions and loads them atomically.
 */
public class HotSwappableEmbeddingFunction extends RichMapFunction<DocumentChunk, ChunkWithEmbedding> {
    
    private final String modelDir;
    private final AtomicReference<ModelInstance> activeModel = new AtomicReference<>();
    private transient Thread watcherThread;

    public HotSwappableEmbeddingFunction(String modelDir) {
        this.modelDir = modelDir;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        // Load initial model
        ModelInstance initial = loadModel(modelDir + "/current");
        activeModel.set(initial);
        
        // Start background watcher for new model versions
        watcherThread = new Thread(this::watchForUpdates);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    @Override
    public ChunkWithEmbedding map(DocumentChunk chunk) throws Exception {
        ModelInstance model = activeModel.get();
        float[] embedding = model.embed(chunk.getContent());
        
        return ChunkWithEmbedding.builder()
            .chunkId(chunk.getChunkId())
            .documentId(chunk.getDocumentId())
            .content(chunk.getContent())
            .embedding(embedding)
            .embeddingModel(model.getModelName())
            .dimensions(embedding.length)
            .metadata(chunk.getMetadata())
            .tenantId(chunk.getTenantId())
            .accessControl(chunk.getAccessControl())
            .timestamp(chunk.getTimestamp())
            .build();
    }

    private void watchForUpdates() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(30000); // Check every 30 seconds
                
                File versionFile = new File(modelDir + "/current/version.txt");
                String newVersion = readFile(versionFile);
                
                if (!newVersion.equals(activeModel.get().getVersion())) {
                    System.out.println("New model version detected: " + newVersion);
                    ModelInstance newModel = loadModel(modelDir + "/current");
                    
                    // Atomic swap
                    ModelInstance old = activeModel.getAndSet(newModel);
                    old.close(); // Release old model resources
                    
                    System.out.println("✅ Model hot-swapped to version: " + newVersion);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("Model watch error: " + e.getMessage());
            }
        }
    }

    private ModelInstance loadModel(String path) throws Exception {
        // Load ONNX model + tokenizer
        return new ModelInstance(path);
    }
}
```

---

## A/B Testing Embedding Models

Route traffic to different models and compare quality metrics:

```python
# flink_jobs/ab_test_embedder.py
import json
import hashlib
import random

class ABTestEmbeddingFunction(KeyedProcessFunction):
    """A/B test between two embedding models in production."""
    
    def open(self, runtime_context):
        import onnxruntime as ort
        from tokenizers import Tokenizer
        
        # Model A: Current production model
        self.model_a = self._load_model("/opt/models/bge-base-v1.5")
        self.tokenizer_a = Tokenizer.from_file("/opt/models/bge-base-v1.5/tokenizer.json")
        
        # Model B: Candidate model being tested
        self.model_b = self._load_model("/opt/models/gte-large-v1.5")
        self.tokenizer_b = Tokenizer.from_file("/opt/models/gte-large-v1.5/tokenizer.json")
        
        # A/B split: 80% model A, 20% model B
        self.model_b_percentage = 0.20
        
        self._buffer = []
    
    def _select_model(self, chunk_id: str) -> str:
        """Deterministic model selection based on chunk_id (consistent routing)."""
        hash_val = int(hashlib.md5(chunk_id.encode()).hexdigest(), 16)
        return "model_b" if (hash_val % 100) < (self.model_b_percentage * 100) else "model_a"
    
    def _flush_batch(self):
        chunks = self._buffer
        self._buffer = []
        
        # Split by model assignment
        model_a_chunks = []
        model_b_chunks = []
        
        for chunk in chunks:
            assignment = self._select_model(chunk["chunk_id"])
            if assignment == "model_b":
                model_b_chunks.append(chunk)
            else:
                model_a_chunks.append(chunk)
        
        # Batch embed with respective models
        results = []
        
        if model_a_chunks:
            texts_a = [c["content"] for c in model_a_chunks]
            embeddings_a = self._batch_embed(texts_a, self.model_a, self.tokenizer_a)
            for chunk, emb in zip(model_a_chunks, embeddings_a):
                results.append(self._build_result(chunk, emb, "bge-base-en-v1.5", "model_a"))
        
        if model_b_chunks:
            texts_b = [c["content"] for c in model_b_chunks]
            embeddings_b = self._batch_embed(texts_b, self.model_b, self.tokenizer_b)
            for chunk, emb in zip(model_b_chunks, embeddings_b):
                results.append(self._build_result(chunk, emb, "gte-large-en-v1.5", "model_b"))
        
        for result in results:
            yield json.dumps(result)
    
    def _build_result(self, chunk, embedding, model_name, variant):
        return {
            "chunk_id": chunk["chunk_id"],
            "document_id": chunk["document_id"],
            "content": chunk["content"],
            "embedding": embedding.tolist(),
            "embedding_model": model_name,
            "dimensions": len(embedding),
            "metadata": {
                **chunk["metadata"],
                "ab_variant": variant,  # Track which model was used
            },
            "tenant_id": chunk["tenant_id"],
            "access_control": chunk.get("access_control", []),
            "timestamp": chunk["timestamp"],
        }
    
    def _load_model(self, path):
        import onnxruntime as ort
        sess_options = ort.SessionOptions()
        sess_options.intra_op_num_threads = 4
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        return ort.InferenceSession(f"{path}/model.onnx", sess_options)
    
    def _batch_embed(self, texts, model, tokenizer, max_len=512):
        import numpy as np
        
        tokenizer.enable_truncation(max_length=max_len)
        tokenizer.enable_padding(length=max_len)
        
        encodings = tokenizer.encode_batch(texts)
        input_ids = np.array([e.ids for e in encodings], dtype=np.int64)
        attention_mask = np.array([e.attention_mask for e in encodings], dtype=np.int64)
        token_type_ids = np.array([e.type_ids for e in encodings], dtype=np.int64)
        
        outputs = model.run(None, {
            "input_ids": input_ids,
            "attention_mask": attention_mask,
            "token_type_ids": token_type_ids,
        })
        
        token_embeddings = outputs[0]
        mask_expanded = np.expand_dims(attention_mask, -1).astype(np.float32)
        mean_embeddings = np.sum(token_embeddings * mask_expanded, axis=1) / np.sum(mask_expanded, axis=1)
        norms = np.linalg.norm(mean_embeddings, axis=1, keepdims=True)
        return mean_embeddings / np.maximum(norms, 1e-9)
```

### Evaluating A/B Results in MongoDB
```python
# scripts/evaluate_ab_test.py
from pymongo import MongoClient

client = MongoClient("mongodb+srv://...")
collection = client["rag_db"]["document_chunks"]

def evaluate_ab_test():
    """Compare retrieval quality between model variants."""
    
    # Count documents per variant
    pipeline = [
        {"$group": {
            "_id": "$metadata.ab_variant",
            "count": {"$sum": 1},
            "avg_dimensions": {"$avg": "$dimensions"},
        }}
    ]
    
    results = list(collection.aggregate(pipeline))
    print("A/B Test Distribution:")
    for r in results:
        print(f"  {r['_id']}: {r['count']} chunks, {r['avg_dimensions']:.0f} dims")
    
    # Compare retrieval scores (requires logging search scores by variant)
    # See query-side A/B evaluation below

def query_ab_evaluation(query: str, query_embedding: list, tenant_id: str):
    """Run same query against both variants and compare."""
    
    # Search model_a embeddings
    results_a = collection.aggregate([
        {"$vectorSearch": {
            "index": "vector_index",
            "path": "embedding",
            "queryVector": query_embedding,
            "numCandidates": 50,
            "limit": 5,
            "filter": {
                "$and": [
                    {"tenant_id": {"$eq": tenant_id}},
                    {"metadata.ab_variant": {"$eq": "model_a"}},
                ]
            }
        }},
        {"$project": {"content": 1, "score": {"$meta": "vectorSearchScore"}}}
    ])
    
    # Search model_b embeddings (need query embedded with model_b too!)
    # This is the challenge: query must be embedded with same model
    # Solution: Embed query with both models, search each variant separately
    
    return {"model_a": list(results_a), "model_b": list(results_b)}
```

---

## Hybrid Strategy: Local + API Fallback

Use local ONNX for most documents, fall back to OpenAI API for complex/long texts:

```python
class HybridEmbeddingFunction(KeyedProcessFunction):
    """
    Strategy:
    - Short/medium text (<512 tokens): Local ONNX model (fast, free)
    - Long text (>512 tokens): OpenAI API (supports 8K tokens)
    - Critical/high-value docs: OpenAI text-embedding-3-large (best quality)
    """
    
    LOCAL_MAX_TOKENS = 512
    
    def open(self, runtime_context):
        import onnxruntime as ort
        from tokenizers import Tokenizer
        from openai import OpenAI
        
        # Local model for most documents
        self.local_model = ort.InferenceSession("/opt/models/bge-base-onnx/model.onnx")
        self.local_tokenizer = Tokenizer.from_file("/opt/models/bge-base-onnx/tokenizer.json")
        
        # OpenAI client for overflow/premium docs
        self.openai = OpenAI()
        
        self._local_buffer = []
        self._api_buffer = []
    
    def process_element(self, value, ctx):
        chunk = json.loads(value)
        
        # Route decision
        token_count = len(self.local_tokenizer.encode(chunk["content"]).ids)
        is_premium = chunk.get("metadata", {}).get("priority") == "high"
        
        if token_count > self.LOCAL_MAX_TOKENS or is_premium:
            self._api_buffer.append(chunk)
        else:
            self._local_buffer.append(chunk)
        
        # Flush local batch (immediately, no API delay)
        if len(self._local_buffer) >= 32:
            yield from self._flush_local()
        
        # Flush API batch (larger batches to reduce API calls)
        if len(self._api_buffer) >= 50:
            yield from self._flush_api()
    
    def _flush_local(self):
        """Fast local ONNX inference."""
        chunks = self._local_buffer
        self._local_buffer = []
        
        texts = [c["content"] for c in chunks]
        embeddings = self._local_embed(texts)
        
        for chunk, emb in zip(chunks, embeddings):
            yield json.dumps({
                **chunk,
                "embedding": emb.tolist(),
                "embedding_model": "bge-base-en-v1.5-local",
                "dimensions": len(emb),
                "metadata": {**chunk["metadata"], "embed_strategy": "local"},
            })
    
    def _flush_api(self):
        """OpenAI API for long/premium documents."""
        chunks = self._api_buffer
        self._api_buffer = []
        
        texts = [c["content"] for c in chunks]
        
        try:
            response = self.openai.embeddings.create(
                model="text-embedding-3-small",
                input=texts,
                dimensions=768,  # Match local model dimensions
            )
            
            for chunk, item in zip(chunks, response.data):
                yield json.dumps({
                    **chunk,
                    "embedding": item.embedding,
                    "embedding_model": "text-embedding-3-small-api",
                    "dimensions": len(item.embedding),
                    "metadata": {**chunk["metadata"], "embed_strategy": "api"},
                })
        except Exception as e:
            # On API failure, fall back to local (truncated)
            print(f"API fallback to local: {e}")
            for chunk in chunks:
                truncated = chunk["content"][:2000]  # Rough truncation
                emb = self._local_embed([truncated])[0]
                yield json.dumps({
                    **chunk,
                    "embedding": emb.tolist(),
                    "embedding_model": "bge-base-en-v1.5-local-fallback",
                    "dimensions": len(emb),
                    "metadata": {**chunk["metadata"], "embed_strategy": "local_fallback"},
                })
```

---

## Model Registry & Deployment Pipeline

```python
# scripts/model_registry.py
"""
Model registry for managing embedding model versions.
Stores models in S3, deploys to Flink task managers.
"""
import boto3
import json
from datetime import datetime

class ModelRegistry:
    def __init__(self, s3_bucket: str, dynamodb_table: str):
        self.s3 = boto3.client("s3")
        self.dynamodb = boto3.resource("dynamodb").Table(dynamodb_table)
        self.bucket = s3_bucket
    
    def register_model(self, model_name: str, version: str, local_path: str, metrics: dict):
        """Register a new model version."""
        # Upload to S3
        s3_key = f"models/{model_name}/{version}/"
        self._upload_directory(local_path, s3_key)
        
        # Record in registry
        self.dynamodb.put_item(Item={
            "model_name": model_name,
            "version": version,
            "s3_path": f"s3://{self.bucket}/{s3_key}",
            "status": "registered",
            "metrics": json.dumps(metrics),
            "created_at": datetime.utcnow().isoformat(),
        })
        
        print(f"✅ Registered {model_name}:{version}")
    
    def promote_to_production(self, model_name: str, version: str):
        """Promote a model version to production (triggers hot-swap)."""
        # Update status
        self.dynamodb.update_item(
            Key={"model_name": model_name, "version": version},
            UpdateExpression="SET #s = :s, promoted_at = :t",
            ExpressionAttributeNames={"#s": "status"},
            ExpressionAttributeValues={":s": "production", ":t": datetime.utcnow().isoformat()},
        )
        
        # Download to Flink task managers (via shared volume or init container)
        # This triggers the HotSwappableEmbeddingFunction to detect the new version
        self._deploy_to_flink(model_name, version)
        
        print(f"✅ Promoted {model_name}:{version} to production")
    
    def rollback(self, model_name: str):
        """Rollback to previous production version."""
        # Find previous production version
        response = self.dynamodb.query(
            KeyConditionExpression="model_name = :mn",
            FilterExpression="#s = :s",
            ExpressionAttributeNames={"#s": "status"},
            ExpressionAttributeValues={":mn": model_name, ":s": "production"},
            ScanIndexForward=False,
            Limit=2,
        )
        
        if len(response["Items"]) >= 2:
            prev_version = response["Items"][1]["version"]
            self.promote_to_production(model_name, prev_version)
            print(f"✅ Rolled back to {model_name}:{prev_version}")
```

---

## Re-Embedding Strategy (Model Migration)

When you switch models, existing embeddings in MongoDB become incompatible:

```python
# scripts/reembed_migration.py
"""
Strategy for migrating embeddings when changing models:
1. Run new model in shadow mode (embed with both old + new)
2. Backfill existing documents with new embeddings
3. Switch vector search index to new embedding field
4. Clean up old embeddings
"""

from confluent_kafka import Producer
import json

class ReembeddingMigration:
    """Publish existing chunks back to Kafka for re-embedding."""
    
    def __init__(self, mongodb_collection, kafka_producer):
        self.collection = mongodb_collection
        self.producer = kafka_producer
    
    def start_migration(self, tenant_id: str = None, batch_size: int = 1000):
        """Re-publish all chunks for re-embedding with new model."""
        
        filter_query = {}
        if tenant_id:
            filter_query["tenant_id"] = tenant_id
        
        cursor = self.collection.find(
            filter_query,
            {"embedding": 0},  # Don't fetch old embeddings
            batch_size=batch_size,
        )
        
        count = 0
        for doc in cursor:
            # Publish to chunked.documents (skipping parse/chunk stages)
            event = {
                "chunk_id": doc["_id"],
                "document_id": doc["document_id"],
                "content": doc["content"],
                "content_hash": doc.get("content_hash", ""),
                "metadata": {
                    **doc.get("metadata", {}),
                    "reembed_migration": "true",
                },
                "tenant_id": doc["tenant_id"],
                "access_control": doc.get("access_control", []),
                "timestamp": int(datetime.utcnow().timestamp() * 1000),
            }
            
            self.producer.produce(
                topic="chunked.documents",
                key=event["chunk_id"],
                value=json.dumps(event),
            )
            
            count += 1
            if count % 10000 == 0:
                self.producer.flush()
                print(f"Published {count} chunks for re-embedding...")
        
        self.producer.flush()
        print(f"✅ Migration started: {count} chunks queued for re-embedding")
```

---

## Monitoring Model Inference in Flink

```python
# Custom Flink metrics for model inference
class MonitoredOnnxEmbeddingFunction(KeyedProcessFunction):
    
    def open(self, runtime_context):
        # Register custom metrics
        self.inference_latency = runtime_context.get_metric_group() \
            .histogram("onnx_inference_latency_ms")
        self.batch_size_metric = runtime_context.get_metric_group() \
            .histogram("onnx_batch_size")
        self.throughput_counter = runtime_context.get_metric_group() \
            .counter("onnx_chunks_embedded")
        self.error_counter = runtime_context.get_metric_group() \
            .counter("onnx_inference_errors")
        
        # ... model loading ...
    
    def _flush_batch(self):
        import time
        chunks = self._buffer
        self._buffer = []
        
        self.batch_size_metric.update(len(chunks))
        start = time.time()
        
        try:
            # ... embedding logic ...
            
            latency_ms = (time.time() - start) * 1000
            self.inference_latency.update(int(latency_ms))
            self.throughput_counter.inc(len(chunks))
            
        except Exception as e:
            self.error_counter.inc(len(chunks))
            # Send to DLQ
            for chunk in chunks:
                yield json.dumps({"error": str(e), "chunk": chunk})
```

---

## Decision Matrix: Which Inference Approach to Use

| Scenario | Approach | Why |
|----------|----------|-----|
| Prototype / small scale | OpenAI API | Zero infra, fast to implement |
| Cost-sensitive production | ONNX CPU (quantized) | 95% cheaper, good throughput |
| High throughput (>5K/sec) | ONNX GPU or Triton | 10-50x throughput |
| Very large models (>1B params) | Triton + AsyncDataStream | Model doesn't fit in Flink JVM |
| Multi-model A/B testing | Local ONNX with routing | Deterministic assignment |
| Hybrid (short + long text) | Local + API fallback | Best of both worlds |
| Privacy requirements | Local ONNX only | Data never leaves infra |

---

## Summary: Complete Flink AI Inference Stack

```
┌─────────────────────────────────────────────────────────────────┐
│                    Model Management Layer                         │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │  S3 Model   │  │ Model        │  │ A/B Test              │  │
│  │  Registry   │  │ Hot-Swap     │  │ Configuration         │  │
│  └─────────────┘  └──────────────┘  └───────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                    Flink Inference Layer                          │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ ONNX Runtime│  │ Batched      │  │ Async Model Server    │  │
│  │ (In-Process)│  │ Processing   │  │ (Triton/TorchServe)   │  │
│  └─────────────┘  └──────────────┘  └───────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                    Optimization Layer                             │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ INT8 Quant  │  │ GPU Accel    │  │ Hybrid Local+API      │  │
│  │ (4x smaller)│  │ (10x faster) │  │ (Cost Optimized)      │  │
│  └─────────────┘  └──────────────┘  └───────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                    Observability Layer                            │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ Latency     │  │ Throughput   │  │ Model Quality         │  │
│  │ Histograms  │  │ Counters     │  │ Metrics               │  │
│  └─────────────┘  └──────────────┘  └───────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Back to Deep Dive

- [← Part 7B: PyFlink + GPU Inference](./kafka-flink-mongodb-rag-part7b.md)
- [← Part 1: Architecture Overview](./kafka-flink-mongodb-rag-part1.md)
- [← Course Home](../README.md)
