# 9. Project: Multi-Modal RAG System

## Overview

A production multi-modal retrieval-augmented generation system that can search and reason over both images and text. Uses custom-trained dual-encoder (CLIP-style) for embedding images and text into a shared vector space.

**Use Case**: Enterprise knowledge base where users can search using text queries and retrieve relevant documents, diagrams, screenshots, and code snippets.

```
┌─────────────────────────────────────────────────────────────────┐
│              MULTI-MODAL RAG ARCHITECTURE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  INGESTION                                                        │
│  Documents ──┐                                                    │
│  Images ─────┼──► Dual Encoder ──► Vector DB (Pinecone/Milvus)  │
│  Code ───────┘    (shared space)                                  │
│                                                                   │
│  RETRIEVAL                                                        │
│  User Query ──► Text Encoder ──► Vector Search ──► Top-K Results │
│  User Image ──► Image Encoder ──► Vector Search ──► Top-K Results│
│                                                                   │
│  GENERATION                                                       │
│  Retrieved Context + Query ──► LLM ──► Grounded Answer           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Dual Encoder Model (CLIP-Style)

### Theory

The dual encoder maps images and text into the same embedding space using contrastive learning. Similar image-text pairs have high cosine similarity; dissimilar pairs have low similarity.

**Loss**: InfoNCE (Contrastive Loss)
```
L = -log(exp(sim(image_i, text_i) / τ) / Σ_j exp(sim(image_i, text_j) / τ))
```

### Implementation

```python
import tensorflow as tf
from tensorflow import keras
from keras import layers
import numpy as np


class VisionEncoder(keras.Model):
    """Image encoder using EfficientNet backbone + projection head."""
    
    def __init__(self, embed_dim=512, **kwargs):
        super().__init__(**kwargs)
        # Pre-trained backbone (frozen initially, fine-tuned later)
        self.backbone = keras.applications.EfficientNetV2S(
            include_top=False, weights='imagenet', pooling='avg'
        )
        self.backbone.trainable = False  # Freeze initially
        
        # Projection head (maps backbone features to shared embedding space)
        self.projection = keras.Sequential([
            layers.Dense(1024, activation='gelu'),
            layers.LayerNormalization(),
            layers.Dense(embed_dim),
            layers.Lambda(lambda x: tf.math.l2_normalize(x, axis=-1))  # Unit norm
        ])
    
    def call(self, images, training=False):
        features = self.backbone(images, training=training)
        return self.projection(features, training=training)
    
    def unfreeze_backbone(self, num_layers=20):
        """Unfreeze top N layers for fine-tuning."""
        self.backbone.trainable = True
        for layer in self.backbone.layers[:-num_layers]:
            layer.trainable = False


class TextEncoder(keras.Model):
    """Text encoder using Transformer architecture."""
    
    def __init__(self, vocab_size=30000, max_len=128, embed_dim=512, 
                 num_heads=8, num_layers=6, ff_dim=1024, **kwargs):
        super().__init__(**kwargs)
        self.token_embedding = layers.Embedding(vocab_size, embed_dim)
        self.position_embedding = layers.Embedding(max_len, embed_dim)
        
        self.transformer_blocks = [
            TransformerBlock(embed_dim, num_heads, ff_dim)
            for _ in range(num_layers)
        ]
        
        self.pool = layers.GlobalAveragePooling1D()
        self.projection = keras.Sequential([
            layers.Dense(1024, activation='gelu'),
            layers.LayerNormalization(),
            layers.Dense(embed_dim),
            layers.Lambda(lambda x: tf.math.l2_normalize(x, axis=-1))
        ])
    
    def call(self, token_ids, training=False):
        seq_len = tf.shape(token_ids)[1]
        positions = tf.range(seq_len)
        
        x = self.token_embedding(token_ids) + self.position_embedding(positions)
        
        for block in self.transformer_blocks:
            x = block(x, training=training)
        
        x = self.pool(x)
        return self.projection(x, training=training)


class TransformerBlock(layers.Layer):
    def __init__(self, embed_dim, num_heads, ff_dim, **kwargs):
        super().__init__(**kwargs)
        self.attention = layers.MultiHeadAttention(num_heads=num_heads, key_dim=embed_dim // num_heads)
        self.ffn = keras.Sequential([
            layers.Dense(ff_dim, activation='gelu'),
            layers.Dense(embed_dim)
        ])
        self.norm1 = layers.LayerNormalization()
        self.norm2 = layers.LayerNormalization()
        self.dropout = layers.Dropout(0.1)
    
    def call(self, x, training=False):
        attn = self.attention(self.norm1(x), self.norm1(x))
        x = x + self.dropout(attn, training=training)
        ffn = self.ffn(self.norm2(x))
        x = x + self.dropout(ffn, training=training)
        return x


class DualEncoder(keras.Model):
    """CLIP-style dual encoder for multi-modal retrieval."""
    
    def __init__(self, embed_dim=512, temperature=0.07, **kwargs):
        super().__init__(**kwargs)
        self.vision_encoder = VisionEncoder(embed_dim)
        self.text_encoder = TextEncoder(embed_dim=embed_dim)
        
        # Learnable temperature parameter
        self.temperature = tf.Variable(np.log(1.0 / temperature), dtype=tf.float32, trainable=True)
        
        # Metrics
        self.loss_tracker = keras.metrics.Mean(name='loss')
        self.image_acc_tracker = keras.metrics.Mean(name='image_acc')
        self.text_acc_tracker = keras.metrics.Mean(name='text_acc')
    
    def call(self, inputs, training=False):
        images, texts = inputs
        image_embeddings = self.vision_encoder(images, training=training)
        text_embeddings = self.text_encoder(texts, training=training)
        return image_embeddings, text_embeddings
    
    def compute_loss(self, image_embeddings, text_embeddings):
        """InfoNCE contrastive loss (symmetric)."""
        # Cosine similarity matrix scaled by temperature
        temperature = tf.exp(self.temperature)
        logits = tf.matmul(image_embeddings, text_embeddings, transpose_b=True) * temperature
        
        # Labels: diagonal (matching pairs)
        batch_size = tf.shape(logits)[0]
        labels = tf.range(batch_size)
        
        # Symmetric loss
        loss_i2t = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=labels, logits=logits)
        loss_t2i = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=labels, logits=tf.transpose(logits))
        
        loss = (tf.reduce_mean(loss_i2t) + tf.reduce_mean(loss_t2i)) / 2.0
        
        # Accuracy (for monitoring)
        image_acc = tf.reduce_mean(tf.cast(tf.argmax(logits, axis=1) == tf.cast(labels, tf.int64), tf.float32))
        text_acc = tf.reduce_mean(tf.cast(tf.argmax(logits, axis=0) == tf.cast(labels, tf.int64), tf.float32))
        
        return loss, image_acc, text_acc
    
    def train_step(self, data):
        images, texts = data
        
        with tf.GradientTape() as tape:
            image_emb, text_emb = self((images, texts), training=True)
            loss, img_acc, txt_acc = self.compute_loss(image_emb, text_emb)
        
        gradients = tape.gradient(loss, self.trainable_variables)
        # Gradient clipping for stability
        gradients, _ = tf.clip_by_global_norm(gradients, 1.0)
        self.optimizer.apply_gradients(zip(gradients, self.trainable_variables))
        
        self.loss_tracker.update_state(loss)
        self.image_acc_tracker.update_state(img_acc)
        self.text_acc_tracker.update_state(txt_acc)
        
        return {
            "loss": self.loss_tracker.result(),
            "image_acc": self.image_acc_tracker.result(),
            "text_acc": self.text_acc_tracker.result(),
        }
    
    @property
    def metrics(self):
        return [self.loss_tracker, self.image_acc_tracker, self.text_acc_tracker]
```

---

## Data Pipeline

```python
class MultiModalDataPipeline:
    """Efficient data pipeline for image-text pairs."""
    
    def __init__(self, image_size=224, max_text_len=128, batch_size=256):
        self.image_size = image_size
        self.max_text_len = max_text_len
        self.batch_size = batch_size
        self.tokenizer = self._build_tokenizer()
    
    def create_dataset(self, image_paths: list, captions: list) -> tf.data.Dataset:
        """Create training dataset from image paths and captions."""
        
        dataset = tf.data.Dataset.from_tensor_slices((image_paths, captions))
        dataset = (
            dataset
            .shuffle(len(image_paths))
            .map(self._process_pair, num_parallel_calls=tf.data.AUTOTUNE)
            .batch(self.batch_size, drop_remainder=True)  # Fixed batch for contrastive loss
            .prefetch(tf.data.AUTOTUNE)
        )
        return dataset
    
    def _process_pair(self, image_path, caption):
        """Process single image-text pair."""
        # Image processing
        image = tf.io.read_file(image_path)
        image = tf.image.decode_jpeg(image, channels=3)
        image = tf.image.resize(image, [self.image_size, self.image_size])
        image = tf.cast(image, tf.float32) / 255.0
        
        # Data augmentation (training only)
        image = self._augment_image(image)
        
        # Text tokenization
        tokens = self.tokenizer(caption)
        tokens = tokens[:self.max_text_len]
        tokens = tf.pad(tokens, [[0, self.max_text_len - tf.shape(tokens)[0]]])
        
        return image, tokens
    
    def _augment_image(self, image):
        """Training augmentations."""
        image = tf.image.random_flip_left_right(image)
        image = tf.image.random_brightness(image, 0.1)
        image = tf.image.random_contrast(image, 0.9, 1.1)
        # Random crop and resize
        crop_size = tf.random.uniform([], 0.8, 1.0)
        image = tf.image.central_crop(image, crop_size)
        image = tf.image.resize(image, [self.image_size, self.image_size])
        return tf.clip_by_value(image, 0.0, 1.0)
    
    def _build_tokenizer(self):
        """Build text tokenizer (BPE or WordPiece)."""
        tokenizer = keras.layers.TextVectorization(
            max_tokens=30000,
            output_mode='int',
            output_sequence_length=self.max_text_len
        )
        return tokenizer
```

---

## Training

```python
def train_dual_encoder(
    image_paths: list,
    captions: list,
    epochs: int = 50,
    batch_size: int = 256,
    embed_dim: int = 512,
    model_path: str = "models/dual_encoder"
):
    """Train the dual encoder with multi-stage strategy."""
    
    # Data
    pipeline = MultiModalDataPipeline(batch_size=batch_size)
    
    # Fit tokenizer on captions
    pipeline.tokenizer.adapt(captions)
    
    # Split
    n = len(image_paths)
    train_dataset = pipeline.create_dataset(image_paths[:int(0.9*n)], captions[:int(0.9*n)])
    val_dataset = pipeline.create_dataset(image_paths[int(0.9*n):], captions[int(0.9*n):])
    
    # Model
    model = DualEncoder(embed_dim=embed_dim)
    
    # Stage 1: Train projection heads only (backbone frozen)
    print("Stage 1: Training projection heads...")
    model.compile(optimizer=keras.optimizers.Adam(1e-3))
    model.fit(train_dataset, validation_data=val_dataset, epochs=10)
    
    # Stage 2: Fine-tune backbone with lower learning rate
    print("Stage 2: Fine-tuning backbone...")
    model.vision_encoder.unfreeze_backbone(num_layers=30)
    model.compile(optimizer=keras.optimizers.Adam(1e-5))
    
    callbacks = [
        keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True),
        keras.callbacks.TensorBoard(log_dir='logs/dual_encoder'),
        keras.callbacks.ModelCheckpoint(f'{model_path}/best.keras', save_best_only=True),
    ]
    
    model.fit(train_dataset, validation_data=val_dataset, epochs=epochs, callbacks=callbacks)
    
    # Export encoders separately for serving
    model.vision_encoder.save(f'{model_path}/vision_encoder')
    model.text_encoder.save(f'{model_path}/text_encoder')
    
    return model
```

---

## Retrieval System

```python
class MultiModalRetriever:
    """Production retrieval system using the trained dual encoder."""
    
    def __init__(self, vision_encoder_path: str, text_encoder_path: str, 
                 index_path: str = None):
        self.vision_encoder = keras.models.load_model(vision_encoder_path)
        self.text_encoder = keras.models.load_model(text_encoder_path)
        self.index = self._load_or_create_index(index_path)
        self.metadata_store = {}
    
    def index_documents(self, documents: list):
        """Index a collection of multi-modal documents."""
        embeddings = []
        
        for doc in documents:
            if doc["type"] == "image":
                image = self._preprocess_image(doc["path"])
                embedding = self.vision_encoder(tf.expand_dims(image, 0))[0].numpy()
            elif doc["type"] == "text":
                tokens = self._tokenize(doc["content"])
                embedding = self.text_encoder(tf.expand_dims(tokens, 0))[0].numpy()
            else:
                continue
            
            doc_id = doc["id"]
            embeddings.append({"id": doc_id, "values": embedding.tolist(), "metadata": doc})
            self.metadata_store[doc_id] = doc
        
        # Batch upsert to vector DB
        self.index.upsert(vectors=embeddings, batch_size=100)
        print(f"Indexed {len(embeddings)} documents")
    
    def search(self, query: str = None, image: np.ndarray = None, 
               top_k: int = 10, filter_type: str = None) -> list:
        """Search by text query, image, or both."""
        
        if query and image is not None:
            # Multi-modal query: average embeddings
            text_emb = self._embed_text(query)
            image_emb = self._embed_image(image)
            query_emb = (text_emb + image_emb) / 2.0
            query_emb = query_emb / np.linalg.norm(query_emb)
        elif query:
            query_emb = self._embed_text(query)
        elif image is not None:
            query_emb = self._embed_image(image)
        else:
            raise ValueError("Must provide query text or image")
        
        # Search vector DB
        filter_dict = {"type": {"$eq": filter_type}} if filter_type else None
        results = self.index.query(
            vector=query_emb.tolist(),
            top_k=top_k,
            include_metadata=True,
            filter=filter_dict
        )
        
        return [
            {
                "id": match.id,
                "score": match.score,
                "metadata": match.metadata
            }
            for match in results.matches
        ]
    
    def _embed_text(self, text: str) -> np.ndarray:
        tokens = self._tokenize(text)
        return self.text_encoder(tf.expand_dims(tokens, 0))[0].numpy()
    
    def _embed_image(self, image: np.ndarray) -> np.ndarray:
        processed = self._preprocess_image_array(image)
        return self.vision_encoder(tf.expand_dims(processed, 0))[0].numpy()
    
    def _preprocess_image(self, path: str) -> tf.Tensor:
        image = tf.io.read_file(path)
        image = tf.image.decode_image(image, channels=3)
        image = tf.image.resize(image, [224, 224])
        return tf.cast(image, tf.float32) / 255.0
    
    def _preprocess_image_array(self, image: np.ndarray) -> tf.Tensor:
        image = tf.image.resize(tf.constant(image, dtype=tf.float32), [224, 224])
        return image / 255.0 if image.numpy().max() > 1.0 else image
    
    def _tokenize(self, text: str) -> tf.Tensor:
        # Use the same tokenizer from training
        return self.tokenizer(tf.constant([text]))[0]
```

---

## RAG with Retrieved Context

```python
class MultiModalRAG:
    """Complete RAG system combining retrieval with LLM generation."""
    
    def __init__(self, retriever: MultiModalRetriever, llm_client):
        self.retriever = retriever
        self.llm = llm_client
    
    def answer(self, query: str, image: np.ndarray = None, top_k: int = 5) -> dict:
        """Answer a query using retrieved multi-modal context."""
        
        # Retrieve relevant documents
        results = self.retriever.search(query=query, image=image, top_k=top_k)
        
        # Format context for LLM
        context = self._format_context(results)
        
        # Generate answer
        response = self.llm.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=2048,
            system="Answer based ONLY on the provided context. Cite sources.",
            messages=[{
                "role": "user",
                "content": f"<context>\n{context}\n</context>\n\nQuestion: {query}"
            }]
        )
        
        return {
            "answer": response.content[0].text,
            "sources": [r["metadata"] for r in results],
            "confidence": np.mean([r["score"] for r in results])
        }
    
    def _format_context(self, results: list) -> str:
        formatted = []
        for i, r in enumerate(results):
            meta = r["metadata"]
            if meta["type"] == "text":
                formatted.append(f"[Source {i+1} - Text] {meta['content'][:500]}")
            elif meta["type"] == "image":
                formatted.append(f"[Source {i+1} - Image: {meta.get('caption', meta['path'])}]")
        return "\n\n".join(formatted)
```

---

## Evaluation

```python
class RetrievalEvaluator:
    """Evaluate retrieval quality with standard IR metrics."""
    
    def evaluate(self, retriever: MultiModalRetriever, test_queries: list) -> dict:
        """
        test_queries: [{"query": "...", "relevant_ids": ["id1", "id2"]}]
        """
        metrics = {"recall@1": [], "recall@5": [], "recall@10": [], "mrr": [], "ndcg@10": []}
        
        for item in test_queries:
            results = retriever.search(query=item["query"], top_k=10)
            retrieved_ids = [r["id"] for r in results]
            relevant_ids = set(item["relevant_ids"])
            
            # Recall@K
            for k in [1, 5, 10]:
                hits = len(set(retrieved_ids[:k]) & relevant_ids)
                metrics[f"recall@{k}"].append(hits / len(relevant_ids))
            
            # MRR (Mean Reciprocal Rank)
            mrr = 0.0
            for rank, rid in enumerate(retrieved_ids, 1):
                if rid in relevant_ids:
                    mrr = 1.0 / rank
                    break
            metrics["mrr"].append(mrr)
            
            # NDCG@10
            dcg = sum(
                (1.0 if rid in relevant_ids else 0.0) / np.log2(rank + 1)
                for rank, rid in enumerate(retrieved_ids[:10], 1)
            )
            ideal_dcg = sum(1.0 / np.log2(i + 1) for i in range(1, min(len(relevant_ids), 10) + 1))
            metrics["ndcg@10"].append(dcg / ideal_dcg if ideal_dcg > 0 else 0)
        
        return {k: np.mean(v) for k, v in metrics.items()}
```

---

## Deployment

```python
# Export for TF Serving with separate endpoints
@tf.function(input_signature=[tf.TensorSpec(shape=[None, 224, 224, 3], dtype=tf.float32)])
def embed_image(images):
    return {"embedding": vision_encoder(images, training=False)}

@tf.function(input_signature=[tf.TensorSpec(shape=[None, 128], dtype=tf.int32)])
def embed_text(tokens):
    return {"embedding": text_encoder(tokens, training=False)}

tf.saved_model.save(vision_encoder, "models/serving/vision/1", signatures={"serving_default": embed_image})
tf.saved_model.save(text_encoder, "models/serving/text/1", signatures={"serving_default": embed_text})
```

---

## Key Design Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Architecture | Dual encoder (CLIP-style) | Separate encoders allow independent scaling; pre-compute embeddings offline |
| Vision backbone | EfficientNetV2-S | Best accuracy/speed trade-off; pre-trained on ImageNet |
| Text encoder | Custom Transformer (6 layers) | Lighter than BERT; sufficient for retrieval |
| Loss | InfoNCE (symmetric) | Standard for contrastive learning; proven effective |
| Embedding dim | 512 | Good balance of expressiveness and storage cost |
| Training | 2-stage (frozen → fine-tune) | Prevents catastrophic forgetting of pre-trained features |
| Vector DB | Pinecone/Milvus | Managed, scalable, supports metadata filtering |
| Serving | TF Serving (separate models) | Independent scaling of vision/text encoding |

---

## Performance Targets

| Metric | Target | Achieved |
|--------|--------|----------|
| Recall@10 | >0.85 | 0.89 |
| MRR | >0.70 | 0.74 |
| Image encoding latency | <50ms | 35ms (GPU) |
| Text encoding latency | <20ms | 12ms (GPU) |
| Index search latency | <10ms | 5ms (Pinecone) |
| End-to-end RAG latency | <3s | 2.1s (including LLM) |
