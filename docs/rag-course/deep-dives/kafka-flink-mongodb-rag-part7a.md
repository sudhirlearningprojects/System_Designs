# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 7A: Flink AI Model Inference — Overview & ONNX Runtime

---

## Why Run Models Inside Flink?

The current Part 3 implementation calls OpenAI's embedding API from within Flink. This has limitations:

| Issue | External API (Current) | Flink AI Inference (This Part) |
|-------|----------------------|-------------------------------|
| **Cost** | $0.02-$0.13 per 1M tokens | ~$0 (self-hosted after infra) |
| **Rate limits** | 3000-10000 RPM | Unlimited (your hardware) |
| **Latency** | 100-500ms per batch (network) | 5-50ms per batch (local) |
| **Data privacy** | Data sent to external API | Data never leaves your infra |
| **Availability** | Depends on OpenAI uptime | Your infra availability |
| **Backpressure** | Complex rate limit handling | Natural Flink backpressure |

### Flink AI Model Inference (Flink 1.19+)

Apache Flink introduced native model inference support allowing you to:
- Load ONNX, TensorFlow SavedModel, or PyTorch TorchScript models
- Run inference as a Flink operator (MapFunction, AsyncFunction)
- Leverage GPU acceleration on task managers
- Batch inference for throughput optimization
- Auto-scale with Flink's parallelism

---

## Architecture Change

```
BEFORE (External API):
┌─────────────┐      HTTP       ┌──────────────┐
│ Flink Job   │ ──────────────▶ │ OpenAI API   │
│ (Embedder)  │ ◀────────────── │ (External)   │
└─────────────┘   100-500ms     └──────────────┘

AFTER (Embedded Inference):
┌─────────────────────────────────────────┐
│ Flink Task Manager                       │
│                                          │
│  ┌─────────────┐    ┌────────────────┐  │
│  │ Flink Job   │───▶│ ONNX Runtime   │  │
│  │ (Embedder)  │◀───│ (In-Process)   │  │
│  └─────────────┘    └────────────────┘  │
│                       5-50ms             │
│                       No network hop     │
└─────────────────────────────────────────┘
```

---

## Supported Models for RAG Embeddings

| Model | Parameters | Dimensions | MTEB Score | ONNX Export | Recommended |
|-------|-----------|------------|------------|-------------|-------------|
| all-MiniLM-L6-v2 | 22M | 384 | 56.3 | ✅ Easy | Dev/prototype |
| bge-base-en-v1.5 | 109M | 768 | 63.6 | ✅ Easy | Production (EN) |
| bge-m3 | 568M | 1024 | 65.0 | ✅ Medium | Production (multilingual) |
| nomic-embed-text-v1.5 | 137M | 768 | 62.2 | ✅ Easy | Cost-effective |
| gte-large-en-v1.5 | 434M | 1024 | 65.4 | ✅ Medium | High quality |
| e5-large-v2 | 335M | 1024 | 62.0 | ✅ Easy | General purpose |

---

## Step 1: Export Model to ONNX

```python
# scripts/export_to_onnx.py
"""Export a HuggingFace embedding model to ONNX format for Flink inference."""

from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer
import os

MODEL_NAME = "BAAI/bge-base-en-v1.5"
OUTPUT_DIR = "./models/bge-base-onnx"

def export_model():
    print(f"Exporting {MODEL_NAME} to ONNX...")
    
    # Export with optimum (handles ONNX conversion + optimization)
    model = ORTModelForFeatureExtraction.from_pretrained(
        MODEL_NAME,
        export=True,
        provider="CPUExecutionProvider",  # or "CUDAExecutionProvider" for GPU
    )
    
    # Save model + tokenizer
    model.save_pretrained(OUTPUT_DIR)
    
    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
    tokenizer.save_pretrained(OUTPUT_DIR)
    
    print(f"Model exported to {OUTPUT_DIR}")
    print(f"Files: {os.listdir(OUTPUT_DIR)}")
    
    # Verify
    verify_onnx_model(OUTPUT_DIR)

def verify_onnx_model(model_dir: str):
    """Verify the exported model works correctly."""
    import numpy as np
    
    tokenizer = AutoTokenizer.from_pretrained(model_dir)
    model = ORTModelForFeatureExtraction.from_pretrained(model_dir)
    
    test_texts = ["Hello world", "Retrieval augmented generation"]
    inputs = tokenizer(test_texts, padding=True, truncation=True, return_tensors="np")
    
    outputs = model(**inputs)
    embeddings = outputs.last_hidden_state  # [batch, seq_len, hidden_dim]
    
    # Mean pooling
    attention_mask = inputs["attention_mask"]
    mask_expanded = np.expand_dims(attention_mask, -1)
    sum_embeddings = np.sum(embeddings * mask_expanded, axis=1)
    sum_mask = np.sum(mask_expanded, axis=1)
    mean_embeddings = sum_embeddings / sum_mask
    
    # Normalize
    norms = np.linalg.norm(mean_embeddings, axis=1, keepdims=True)
    normalized = mean_embeddings / norms
    
    print(f"Embedding shape: {normalized.shape}")
    print(f"Cosine similarity: {np.dot(normalized[0], normalized[1]):.4f}")
    print("✅ ONNX model verified successfully")

if __name__ == "__main__":
    export_model()
```

### Optimize ONNX for Production
```python
# scripts/optimize_onnx.py
"""Optimize ONNX model for faster inference."""

from optimum.onnxruntime import ORTOptimizer, ORTQuantizer
from optimum.onnxruntime.configuration import OptimizationConfig, AutoQuantizationConfig

MODEL_DIR = "./models/bge-base-onnx"
OPTIMIZED_DIR = "./models/bge-base-onnx-optimized"

def optimize():
    # Step 1: Graph optimization (operator fusion, constant folding)
    optimizer = ORTOptimizer.from_pretrained(MODEL_DIR)
    optimization_config = OptimizationConfig(
        optimization_level=99,  # Max optimization
        optimize_for_gpu=False,  # Set True for GPU deployment
        fp16=False,  # Set True for GPU with FP16 support
    )
    optimizer.optimize(save_dir=OPTIMIZED_DIR, optimization_config=optimization_config)
    print("✅ Graph optimization complete")

def quantize():
    # Step 2: INT8 quantization (4x smaller, 2-3x faster on CPU)
    quantizer = ORTQuantizer.from_pretrained(OPTIMIZED_DIR)
    quantization_config = AutoQuantizationConfig.avx512_vnni(
        is_static=False,  # Dynamic quantization (no calibration data needed)
        per_channel=True,
    )
    quantizer.quantize(save_dir=f"{OPTIMIZED_DIR}-int8", quantization_config=quantization_config)
    print("✅ INT8 quantization complete")
    print("Model size reduction: ~4x")
    print("Inference speedup: ~2-3x on CPU")

if __name__ == "__main__":
    optimize()
    quantize()
```

---

## Step 2: Flink ONNX Inference Operator (Java)

### Maven Dependencies

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Flink Core -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java</artifactId>
        <version>1.19.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-connector-kafka</artifactId>
        <version>3.1.0-1.19</version>
    </dependency>
    
    <!-- ONNX Runtime -->
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.17.0</version>
    </dependency>
    <!-- For GPU: onnxruntime_gpu instead -->
    
    <!-- Tokenizer (HuggingFace tokenizers Java binding) -->
    <dependency>
        <groupId>ai.djl.huggingface</groupId>
        <artifactId>tokenizers</artifactId>
        <version>0.27.0</version>
    </dependency>
    
    <!-- DJL (Deep Java Library) for tensor operations -->
    <dependency>
        <groupId>ai.djl</groupId>
        <artifactId>api</artifactId>
        <version>0.27.0</version>
    </dependency>
</dependencies>
```

### ONNX Embedding Operator

```java
package com.rag.flink.inference;

import ai.onnxruntime.*;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.Map;

public class OnnxEmbeddingFunction extends RichMapFunction<DocumentChunk, ChunkWithEmbedding> {

    private transient OrtEnvironment env;
    private transient OrtSession session;
    private transient HuggingFaceTokenizer tokenizer;
    private final String modelPath;
    private final int maxLength;

    public OnnxEmbeddingFunction(String modelPath, int maxLength) {
        this.modelPath = modelPath;
        this.maxLength = maxLength;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        // Initialize ONNX Runtime (once per task manager slot)
        env = OrtEnvironment.getEnvironment();
        
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setIntraOpNumThreads(4);  // CPU threads per inference
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        opts.addConfigEntry("session.intra_op.allow_spinning", "0");
        
        // For GPU:
        // opts.addCUDA(0);  // GPU device 0
        
        session = env.createSession(modelPath + "/model.onnx", opts);
        tokenizer = HuggingFaceTokenizer.newInstance(Path.of(modelPath + "/tokenizer.json"));
        
        LOG.info("ONNX model loaded: {} dimensions, {} max tokens",
            session.getOutputInfo().get(0).getInfo().toString(), maxLength);
    }

    @Override
    public ChunkWithEmbedding map(DocumentChunk chunk) throws Exception {
        float[] embedding = generateEmbedding(chunk.getContent());
        
        return ChunkWithEmbedding.builder()
            .chunkId(chunk.getChunkId())
            .documentId(chunk.getDocumentId())
            .content(chunk.getContent())
            .embedding(embedding)
            .embeddingModel("bge-base-en-v1.5-onnx")
            .dimensions(embedding.length)
            .metadata(chunk.getMetadata())
            .tenantId(chunk.getTenantId())
            .accessControl(chunk.getAccessControl())
            .timestamp(chunk.getTimestamp())
            .build();
    }

    private float[] generateEmbedding(String text) throws OrtException {
        // Prepend instruction prefix for BGE models
        String prefixedText = "Represent this document for retrieval: " + text;
        
        // Tokenize
        var encoding = tokenizer.encode(prefixedText);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] tokenTypeIds = encoding.getTypeIds();
        
        // Truncate to max length
        int seqLen = Math.min(inputIds.length, maxLength);
        inputIds = java.util.Arrays.copyOf(inputIds, seqLen);
        attentionMask = java.util.Arrays.copyOf(attentionMask, seqLen);
        tokenTypeIds = java.util.Arrays.copyOf(tokenTypeIds, seqLen);
        
        // Create ONNX tensors
        long[] shape = {1, seqLen};
        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env,
            LongBuffer.wrap(inputIds), shape);
        OnnxTensor attMaskTensor = OnnxTensor.createTensor(env,
            LongBuffer.wrap(attentionMask), shape);
        OnnxTensor tokenTypeTensor = OnnxTensor.createTensor(env,
            LongBuffer.wrap(tokenTypeIds), shape);
        
        Map<String, OnnxTensor> inputs = Map.of(
            "input_ids", inputIdsTensor,
            "attention_mask", attMaskTensor,
            "token_type_ids", tokenTypeTensor
        );
        
        // Run inference
        try (OrtSession.Result result = session.run(inputs)) {
            // Output shape: [1, seq_len, hidden_dim]
            float[][][] output = (float[][][]) result.get(0).getValue();
            
            // Mean pooling with attention mask
            float[] pooled = meanPooling(output[0], attentionMask);
            
            // L2 normalize
            return l2Normalize(pooled);
        } finally {
            inputIdsTensor.close();
            attMaskTensor.close();
            tokenTypeTensor.close();
        }
    }

    private float[] meanPooling(float[][] tokenEmbeddings, long[] attentionMask) {
        int hiddenDim = tokenEmbeddings[0].length;
        float[] summed = new float[hiddenDim];
        float maskSum = 0;
        
        for (int i = 0; i < tokenEmbeddings.length; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < hiddenDim; j++) {
                    summed[j] += tokenEmbeddings[i][j];
                }
                maskSum += 1;
            }
        }
        
        for (int j = 0; j < hiddenDim; j++) {
            summed[j] /= maskSum;
        }
        return summed;
    }

    private float[] l2Normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

    @Override
    public void close() throws Exception {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}
```

---

## Step 3: Batched ONNX Inference (Higher Throughput)

```java
package com.rag.flink.inference;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;

public class BatchedOnnxEmbeddingFunction 
    extends KeyedProcessFunction<String, DocumentChunk, ChunkWithEmbedding> {

    private static final int BATCH_SIZE = 32;
    private static final long BATCH_TIMEOUT_MS = 2000;
    
    private transient OrtEnvironment env;
    private transient OrtSession session;
    private transient HuggingFaceTokenizer tokenizer;
    private transient ListState<DocumentChunk> buffer;
    private transient boolean timerRegistered;
    
    private final String modelPath;

    public BatchedOnnxEmbeddingFunction(String modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        // Same ONNX initialization as above
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setIntraOpNumThreads(4);
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        session = env.createSession(modelPath + "/model.onnx", opts);
        tokenizer = HuggingFaceTokenizer.newInstance(Path.of(modelPath + "/tokenizer.json"));
        
        buffer = getRuntimeContext().getListState(
            new ListStateDescriptor<>("chunk-buffer", DocumentChunk.class)
        );
    }

    @Override
    public void processElement(DocumentChunk chunk, Context ctx, Collector<ChunkWithEmbedding> out) 
        throws Exception {
        
        buffer.add(chunk);
        
        // Register timeout timer
        if (!timerRegistered) {
            ctx.timerService().registerProcessingTimeTimer(
                ctx.timerService().currentProcessingTime() + BATCH_TIMEOUT_MS
            );
            timerRegistered = true;
        }
        
        // Check batch size
        List<DocumentChunk> buffered = collectBuffer();
        if (buffered.size() >= BATCH_SIZE) {
            flushBatch(buffered, out);
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<ChunkWithEmbedding> out) 
        throws Exception {
        
        List<DocumentChunk> buffered = collectBuffer();
        if (!buffered.isEmpty()) {
            flushBatch(buffered, out);
        }
        timerRegistered = false;
    }

    private void flushBatch(List<DocumentChunk> chunks, Collector<ChunkWithEmbedding> out) 
        throws Exception {
        
        // Batch tokenize
        String[] texts = chunks.stream()
            .map(c -> "Represent this document for retrieval: " + c.getContent())
            .toArray(String[]::new);
        
        // Batch inference (much faster than individual calls)
        float[][] embeddings = batchEmbed(texts);
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            out.collect(ChunkWithEmbedding.builder()
                .chunkId(chunk.getChunkId())
                .documentId(chunk.getDocumentId())
                .content(chunk.getContent())
                .embedding(embeddings[i])
                .embeddingModel("bge-base-en-v1.5-onnx")
                .dimensions(embeddings[i].length)
                .metadata(chunk.getMetadata())
                .tenantId(chunk.getTenantId())
                .accessControl(chunk.getAccessControl())
                .timestamp(chunk.getTimestamp())
                .build());
        }
        
        buffer.clear();
    }

    private float[][] batchEmbed(String[] texts) throws OrtException {
        int batchSize = texts.length;
        int maxSeqLen = 512;
        
        // Batch tokenize and pad
        long[][] allInputIds = new long[batchSize][maxSeqLen];
        long[][] allAttentionMasks = new long[batchSize][maxSeqLen];
        long[][] allTokenTypeIds = new long[batchSize][maxSeqLen];
        
        for (int i = 0; i < batchSize; i++) {
            var encoding = tokenizer.encode(texts[i]);
            long[] ids = encoding.getIds();
            long[] mask = encoding.getAttentionMask();
            long[] types = encoding.getTypeIds();
            
            int len = Math.min(ids.length, maxSeqLen);
            System.arraycopy(ids, 0, allInputIds[i], 0, len);
            System.arraycopy(mask, 0, allAttentionMasks[i], 0, len);
            System.arraycopy(types, 0, allTokenTypeIds[i], 0, len);
        }
        
        // Create batch tensors
        long[] shape = {batchSize, maxSeqLen};
        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, allInputIds);
        OnnxTensor attMaskTensor = OnnxTensor.createTensor(env, allAttentionMasks);
        OnnxTensor tokenTypeTensor = OnnxTensor.createTensor(env, allTokenTypeIds);
        
        Map<String, OnnxTensor> inputs = Map.of(
            "input_ids", inputIdsTensor,
            "attention_mask", attMaskTensor,
            "token_type_ids", tokenTypeTensor
        );
        
        try (OrtSession.Result result = session.run(inputs)) {
            float[][][] output = (float[][][]) result.get(0).getValue();
            
            float[][] embeddings = new float[batchSize][];
            for (int i = 0; i < batchSize; i++) {
                float[] pooled = meanPooling(output[i], allAttentionMasks[i]);
                embeddings[i] = l2Normalize(pooled);
            }
            return embeddings;
        } finally {
            inputIdsTensor.close();
            attMaskTensor.close();
            tokenTypeTensor.close();
        }
    }
    
    private List<DocumentChunk> collectBuffer() throws Exception {
        List<DocumentChunk> list = new ArrayList<>();
        buffer.get().forEach(list::add);
        return list;
    }
}
```

---

## Step 4: Complete Flink Job with ONNX Inference

```java
package com.rag.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.sink.KafkaSink;

public class OnnxEmbeddingJob {
    
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Checkpointing for exactly-once
        env.enableCheckpointing(60000);
        env.setParallelism(8);  // Match to available CPU cores
        
        // Source: chunked documents from Kafka
        KafkaSource<DocumentChunk> source = KafkaSource.<DocumentChunk>builder()
            .setBootstrapServers("localhost:9092")
            .setTopics("chunked.documents")
            .setGroupId("flink-onnx-embedder")
            .setValueOnlyDeserializer(new DocumentChunkDeserializer())
            .build();
        
        DataStream<DocumentChunk> chunks = env.fromSource(
            source, WatermarkStrategy.noWatermarks(), "Chunked Docs"
        );
        
        // ONNX Model Inference (embedded, no external API)
        DataStream<ChunkWithEmbedding> embeddings = chunks
            .keyBy(DocumentChunk::getTenantId)
            .process(new BatchedOnnxEmbeddingFunction("/opt/models/bge-base-onnx-optimized"));
        
        // Sink to Kafka → MongoDB
        KafkaSink<ChunkWithEmbedding> sink = KafkaSink.<ChunkWithEmbedding>builder()
            .setBootstrapServers("localhost:9092")
            .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                    .setTopic("embeddings")
                    .setValueSerializationSchema(new EmbeddingSerializer())
                    .build()
            )
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .build();
        
        embeddings.sinkTo(sink);
        
        env.execute("RAG ONNX Embedding Job");
    }
}
```

---

## Next: [Part 7B — PyFlink + GPU Inference & DJL](./kafka-flink-mongodb-rag-part7b.md)
