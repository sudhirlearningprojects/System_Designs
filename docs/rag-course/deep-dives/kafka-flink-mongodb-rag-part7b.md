# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 7B: PyFlink + GPU Inference & DJL (Deep Java Library)

---

## PyFlink with Local ONNX Inference

For teams preferring Python, you can run ONNX models within PyFlink operators:

```python
# flink_jobs/pyflink_onnx_embedder.py

from pyflink.datastream import StreamExecutionEnvironment
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaOffsetsInitializer
from pyflink.datastream.functions import KeyedProcessFunction
from pyflink.common.serialization import SimpleStringSchema
import json
import numpy as np

class OnnxEmbeddingFunction(KeyedProcessFunction):
    """Run ONNX embedding model directly inside Flink (no external API)."""
    
    BATCH_SIZE = 32
    BATCH_TIMEOUT_MS = 2000
    MODEL_PATH = "/opt/models/bge-base-onnx-optimized"
    
    def open(self, runtime_context):
        import onnxruntime as ort
        from tokenizers import Tokenizer
        
        # Load ONNX model
        sess_options = ort.SessionOptions()
        sess_options.intra_op_num_threads = 4
        sess_options.inter_op_num_threads = 1
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        
        # CPU provider (use CUDAExecutionProvider for GPU)
        providers = ['CPUExecutionProvider']
        # providers = ['CUDAExecutionProvider', 'CPUExecutionProvider']  # GPU
        
        self.session = ort.InferenceSession(
            f"{self.MODEL_PATH}/model_optimized.onnx",
            sess_options,
            providers=providers,
        )
        
        # Load tokenizer
        self.tokenizer = Tokenizer.from_file(f"{self.MODEL_PATH}/tokenizer.json")
        self.tokenizer.enable_truncation(max_length=512)
        self.tokenizer.enable_padding(length=512)
        
        # Buffer for batching
        self._buffer = []
        self._timer_registered = False
        
        print(f"✅ ONNX model loaded: {self.MODEL_PATH}")
        print(f"   Providers: {self.session.get_providers()}")
        print(f"   Input: {[i.name for i in self.session.get_inputs()]}")
        print(f"   Output: {[o.name for o in self.session.get_outputs()]}")
    
    def process_element(self, value, ctx):
        chunk = json.loads(value)
        self._buffer.append(chunk)
        
        # Register timeout timer for partial batch flush
        if not self._timer_registered:
            ctx.timer_service().register_processing_time_timer(
                ctx.timer_service().current_processing_time() + self.BATCH_TIMEOUT_MS
            )
            self._timer_registered = True
        
        # Flush if batch full
        if len(self._buffer) >= self.BATCH_SIZE:
            yield from self._flush_batch()
    
    def on_timer(self, timestamp, ctx):
        if self._buffer:
            yield from self._flush_batch()
        self._timer_registered = False
    
    def _flush_batch(self):
        """Generate embeddings for buffered chunks using ONNX."""
        chunks = self._buffer
        self._buffer = []
        self._timer_registered = False
        
        # Prepare texts with instruction prefix
        texts = [f"Represent this document for retrieval: {c['content']}" for c in chunks]
        
        # Batch tokenize
        encodings = self.tokenizer.encode_batch(texts)
        
        input_ids = np.array([e.ids for e in encodings], dtype=np.int64)
        attention_mask = np.array([e.attention_mask for e in encodings], dtype=np.int64)
        token_type_ids = np.array([e.type_ids for e in encodings], dtype=np.int64)
        
        # Run ONNX inference
        outputs = self.session.run(
            None,  # All outputs
            {
                "input_ids": input_ids,
                "attention_mask": attention_mask,
                "token_type_ids": token_type_ids,
            }
        )
        
        # outputs[0] shape: [batch_size, seq_len, hidden_dim]
        token_embeddings = outputs[0]
        
        # Mean pooling
        mask_expanded = np.expand_dims(attention_mask, -1).astype(np.float32)
        sum_embeddings = np.sum(token_embeddings * mask_expanded, axis=1)
        sum_mask = np.sum(mask_expanded, axis=1)
        mean_embeddings = sum_embeddings / np.maximum(sum_mask, 1e-9)
        
        # L2 normalize
        norms = np.linalg.norm(mean_embeddings, axis=1, keepdims=True)
        normalized = mean_embeddings / np.maximum(norms, 1e-9)
        
        # Yield results
        for chunk, embedding in zip(chunks, normalized):
            result = {
                "chunk_id": chunk["chunk_id"],
                "document_id": chunk["document_id"],
                "content": chunk["content"],
                "embedding": embedding.tolist(),
                "embedding_model": "bge-base-en-v1.5-onnx",
                "dimensions": len(embedding),
                "metadata": chunk["metadata"],
                "tenant_id": chunk["tenant_id"],
                "access_control": chunk.get("access_control", []),
                "timestamp": chunk["timestamp"],
            }
            yield json.dumps(result)
    
    def close(self):
        if hasattr(self, 'session'):
            del self.session


def run_onnx_embedding_job():
    env = StreamExecutionEnvironment.get_execution_environment()
    env.set_parallelism(8)
    env.enable_checkpointing(60000)
    
    # Source
    source = KafkaSource.builder() \
        .set_bootstrap_servers("localhost:9092") \
        .set_topics("chunked.documents") \
        .set_group_id("flink-onnx-embedder") \
        .set_starting_offsets(KafkaOffsetsInitializer.committed_offsets()) \
        .set_value_only_deserializer(SimpleStringSchema()) \
        .build()
    
    # Sink
    sink = KafkaSink.builder() \
        .set_bootstrap_servers("localhost:9092") \
        .set_record_serializer(
            KafkaRecordSerializationSchema.builder()
                .set_topic("embeddings")
                .set_value_serialization_schema(SimpleStringSchema())
                .build()
        ) \
        .build()
    
    # Pipeline
    ds = env.from_source(source, WatermarkStrategy.no_watermarks(), "Chunks")
    
    ds.key_by(lambda x: json.loads(x).get("tenant_id", "default")) \
      .process(OnnxEmbeddingFunction()) \
      .sink_to(sink)
    
    env.execute("RAG PyFlink ONNX Embedding Job")


if __name__ == "__main__":
    run_onnx_embedding_job()
```

---

## GPU Inference with CUDA Provider

```python
class GpuOnnxEmbeddingFunction(KeyedProcessFunction):
    """GPU-accelerated ONNX inference for 10x throughput."""
    
    BATCH_SIZE = 128  # Larger batches for GPU efficiency
    
    def open(self, runtime_context):
        import onnxruntime as ort
        from tokenizers import Tokenizer
        
        sess_options = ort.SessionOptions()
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        
        # GPU configuration
        providers = [
            ('CUDAExecutionProvider', {
                'device_id': 0,
                'arena_extend_strategy': 'kSameAsRequested',
                'gpu_mem_limit': 4 * 1024 * 1024 * 1024,  # 4GB GPU memory limit
                'cudnn_conv_algo_search': 'EXHAUSTIVE',
            }),
            'CPUExecutionProvider',  # Fallback
        ]
        
        self.session = ort.InferenceSession(
            "/opt/models/bge-base-onnx/model.onnx",
            sess_options,
            providers=providers,
        )
        
        active_provider = self.session.get_providers()[0]
        print(f"✅ Running on: {active_provider}")
        if 'CUDA' in active_provider:
            print("   GPU inference enabled - expect 10x throughput")
        
        self.tokenizer = Tokenizer.from_file("/opt/models/bge-base-onnx/tokenizer.json")
        self.tokenizer.enable_truncation(max_length=512)
        self.tokenizer.enable_padding(length=512)
        self._buffer = []
```

### Docker Setup for GPU Flink

```dockerfile
# Dockerfile.gpu
FROM nvidia/cuda:12.2.0-runtime-ubuntu22.04

# Install Python + ONNX Runtime GPU
RUN apt-get update && apt-get install -y python3 python3-pip
RUN pip3 install onnxruntime-gpu==1.17.0 tokenizers numpy apache-flink

# Copy model
COPY models/bge-base-onnx /opt/models/bge-base-onnx

# Copy Flink job
COPY flink_jobs/ /opt/flink-jobs/

CMD ["python3", "/opt/flink-jobs/pyflink_onnx_embedder.py"]
```

```yaml
# docker-compose.gpu.yml (Flink task manager with GPU)
services:
  flink-taskmanager-gpu:
    build:
      context: .
      dockerfile: Dockerfile.gpu
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    environment:
      FLINK_PROPERTIES: |
        jobmanager.rpc.address: flink-jobmanager
        taskmanager.numberOfTaskSlots: 2
```

---

## DJL (Deep Java Library) Approach

DJL provides a higher-level Java API for model inference without manual ONNX tensor handling:

```java
package com.rag.flink.inference;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.DefaultVocabulary;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

public class DjlEmbeddingFunction extends RichMapFunction<DocumentChunk, ChunkWithEmbedding> {
    
    private transient ZooModel<String[], float[][]> model;
    private transient Predictor<String[], float[][]> predictor;

    @Override
    public void open(Configuration parameters) throws Exception {
        // Load model using DJL's model zoo
        Criteria<String[], float[][]> criteria = Criteria.builder()
            .setTypes(String[].class, float[][].class)
            .optModelPath(java.nio.file.Path.of("/opt/models/bge-base-onnx"))
            .optEngine("OnnxRuntime")  // or "PyTorch", "TensorFlow"
            .optTranslator(new EmbeddingTranslator())
            .build();
        
        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    @Override
    public ChunkWithEmbedding map(DocumentChunk chunk) throws Exception {
        String[] input = {"Represent this document for retrieval: " + chunk.getContent()};
        float[][] embeddings = predictor.predict(input);
        
        return ChunkWithEmbedding.builder()
            .chunkId(chunk.getChunkId())
            .documentId(chunk.getDocumentId())
            .content(chunk.getContent())
            .embedding(embeddings[0])
            .embeddingModel("bge-base-en-v1.5-djl")
            .dimensions(embeddings[0].length)
            .metadata(chunk.getMetadata())
            .tenantId(chunk.getTenantId())
            .build();
    }

    @Override
    public void close() throws Exception {
        if (predictor != null) predictor.close();
        if (model != null) model.close();
    }
}

/**
 * Custom translator for sentence embedding models.
 */
class EmbeddingTranslator implements Translator<String[], float[][]> {
    
    private HuggingFaceTokenizer tokenizer;

    @Override
    public void prepare(TranslatorContext ctx) throws Exception {
        tokenizer = HuggingFaceTokenizer.newInstance(
            ctx.getModel().getModelPath().resolve("tokenizer.json")
        );
    }

    @Override
    public NDList processInput(TranslatorContext ctx, String[] input) {
        NDManager manager = ctx.getNDManager();
        
        var encodings = tokenizer.batchEncode(java.util.Arrays.asList(input));
        
        long[][] inputIds = new long[input.length][];
        long[][] attentionMask = new long[input.length][];
        
        for (int i = 0; i < input.length; i++) {
            inputIds[i] = encodings.get(i).getIds();
            attentionMask[i] = encodings.get(i).getAttentionMask();
        }
        
        NDArray ids = manager.create(inputIds);
        NDArray mask = manager.create(attentionMask);
        
        return new NDList(ids, mask);
    }

    @Override
    public float[][] processOutput(TranslatorContext ctx, NDList output) {
        NDArray lastHidden = output.get(0);  // [batch, seq_len, hidden]
        
        // Mean pooling (simplified - in production use attention mask)
        NDArray meanPooled = lastHidden.mean(new int[]{1});  // [batch, hidden]
        
        // L2 normalize
        NDArray norms = meanPooled.norm(new int[]{1}, true);
        NDArray normalized = meanPooled.div(norms);
        
        return normalized.toFloatArray2D();
    }
}
```

---

## Async Inference with Flink AsyncDataStream

For maximum throughput when using external model servers (e.g., Triton Inference Server):

```java
package com.rag.flink.inference;

import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.flink.streaming.api.functions.async.ResultFuture;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Async inference against a Triton/TorchServe model server.
 * Use when model is too large for in-process (e.g., BGE-M3 568M params).
 */
public class AsyncModelInferenceFunction 
    extends RichAsyncFunction<DocumentChunk, ChunkWithEmbedding> {
    
    private transient HttpClient httpClient;
    private final String modelServerUrl;

    public AsyncModelInferenceFunction(String modelServerUrl) {
        this.modelServerUrl = modelServerUrl;
    }

    @Override
    public void open(Configuration parameters) {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();
    }

    @Override
    public void asyncInvoke(DocumentChunk chunk, ResultFuture<ChunkWithEmbedding> resultFuture) {
        // Non-blocking HTTP call to model server
        String requestBody = String.format(
            "{\"inputs\": [{\"name\": \"text\", \"data\": [\"%s\"]}]}",
            escapeJson(chunk.getContent())
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(modelServerUrl + "/v2/models/bge-base/infer"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        CompletableFuture<HttpResponse<String>> future = 
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        future.thenAccept(response -> {
            float[] embedding = parseEmbeddingFromResponse(response.body());
            
            ChunkWithEmbedding result = ChunkWithEmbedding.builder()
                .chunkId(chunk.getChunkId())
                .documentId(chunk.getDocumentId())
                .content(chunk.getContent())
                .embedding(embedding)
                .embeddingModel("bge-base-triton")
                .dimensions(embedding.length)
                .metadata(chunk.getMetadata())
                .tenantId(chunk.getTenantId())
                .build();
            
            resultFuture.complete(Collections.singleton(result));
        }).exceptionally(ex -> {
            resultFuture.completeExceptionally(ex);
            return null;
        });
    }

    @Override
    public void timeout(DocumentChunk chunk, ResultFuture<ChunkWithEmbedding> resultFuture) {
        resultFuture.completeExceptionally(
            new RuntimeException("Model inference timeout for chunk: " + chunk.getChunkId())
        );
    }
}

// Usage in Flink job:
DataStream<ChunkWithEmbedding> embeddings = AsyncDataStream.unorderedWait(
    chunks,
    new AsyncModelInferenceFunction("http://triton-server:8000"),
    30, TimeUnit.SECONDS,  // Timeout
    100                     // Max concurrent requests
);
```

---

## Triton Inference Server Setup (for large models)

```yaml
# docker-compose.triton.yml
services:
  triton-server:
    image: nvcr.io/nvidia/tritonserver:24.01-py3
    ports:
      - "8000:8000"  # HTTP
      - "8001:8001"  # gRPC
      - "8002:8002"  # Metrics
    volumes:
      - ./model_repository:/models
    command: tritonserver --model-repository=/models --strict-model-config=false
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
```

```
# model_repository/bge-base/config.pbtxt
name: "bge-base"
platform: "onnxruntime_onnx"
max_batch_size: 64
input [
  { name: "input_ids", data_type: TYPE_INT64, dims: [-1] },
  { name: "attention_mask", data_type: TYPE_INT64, dims: [-1] },
  { name: "token_type_ids", data_type: TYPE_INT64, dims: [-1] }
]
output [
  { name: "last_hidden_state", data_type: TYPE_FP32, dims: [-1, 768] }
]
dynamic_batching {
  preferred_batch_size: [16, 32]
  max_queue_delay_microseconds: 5000
}
instance_group [
  { count: 2, kind: KIND_GPU }
]
```

---

## Performance Comparison

| Approach | Throughput (chunks/sec) | Latency (P95) | Hardware | Cost/1M chunks |
|----------|------------------------|---------------|----------|----------------|
| OpenAI API | ~200 | 500ms | None (API) | $0.02 |
| ONNX CPU (4 threads) | ~500 | 50ms | 4 vCPU | ~$0.001 |
| ONNX CPU INT8 quantized | ~1200 | 20ms | 4 vCPU | ~$0.0004 |
| ONNX GPU (T4) | ~5000 | 10ms | 1x T4 GPU | ~$0.0002 |
| Triton GPU (batched) | ~10000 | 15ms | 1x A10G | ~$0.0001 |
| Triton Multi-GPU | ~40000 | 20ms | 4x A10G | ~$0.00005 |

---

## Next: [Part 7C — Model Management, A/B Testing & Hybrid Strategy](./kafka-flink-mongodb-rag-part7c.md)
