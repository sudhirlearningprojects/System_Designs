# Natural Language Processing Theory

## Language Modeling Fundamentals

### Statistical Language Models

**N-gram Models**
```
P(w_n|w_1...w_{n-1}) ≈ P(w_n|w_{n-N+1}...w_{n-1})
```

**Smoothing Techniques**:
- **Add-k Smoothing**: Add k to all counts
- **Good-Turing**: Redistribute probability mass
- **Kneser-Ney**: Interpolation-based smoothing

**Perplexity**
```
PP(W) = P(w_1w_2...w_N)^{-1/N} = ∏P(w_i|w_1...w_{i-1})^{-1/N}
```

### Neural Language Models

**Feedforward Neural LM**
```
P(w_t|w_{t-n+1}...w_{t-1}) = softmax(W·h + b)
```

**Recurrent Neural LM**
```
h_t = f(W_h·h_{t-1} + W_x·x_t + b)
P(w_t|w_1...w_{t-1}) = softmax(W_o·h_t + b_o)
```

**LSTM Language Model**
- **Long-term Dependencies**: Gating mechanisms
- **Gradient Flow**: Cell state highway
- **Bidirectional**: Forward + backward context

## Word Representations

### Distributional Semantics
**Harris Hypothesis**: Words in similar contexts have similar meanings

**Co-occurrence Matrix**
- **Context Window**: Fixed-size window around target word
- **PMI**: Pointwise Mutual Information weighting
- **SVD**: Dimensionality reduction

### Word Embeddings

**Word2Vec**
- **Skip-gram**: Predict context from word
- **CBOW**: Predict word from context
- **Negative Sampling**: Efficient training
- **Hierarchical Softmax**: Tree-based probability

**Skip-gram Objective**
```
J = -1/T ∑∑ log P(w_{t+j}|w_t)
```

**GloVe (Global Vectors)**
```
J = ∑f(X_{ij})(w_i^T w̃_j + b_i + b̃_j - log X_{ij})²
```

**FastText**
- **Subword Information**: Character n-grams
- **OOV Handling**: Compositional representations
- **Morphological Awareness**: Prefix/suffix patterns

### Contextual Embeddings

**ELMo (Embeddings from Language Models)**
- **Bidirectional LSTM**: Forward + backward LM
- **Context-dependent**: Different contexts → different embeddings
- **Layer Combination**: Weighted sum of layer outputs

**BERT (Bidirectional Encoder Representations)**
- **Masked Language Model**: Predict masked tokens
- **Next Sentence Prediction**: Sentence relationship
- **Transformer Architecture**: Self-attention mechanism

## Sequence-to-Sequence Models

### Encoder-Decoder Architecture
```
Encoder: h = f(x_1, x_2, ..., x_n)
Decoder: y_t = g(h, y_1, ..., y_{t-1})
```

### Attention Mechanism

**Additive Attention**
```
e_{ij} = v^T tanh(W_h h_i + W_s s_j)
α_{ij} = softmax(e_{ij})
c_j = ∑α_{ij} h_i
```

**Multiplicative Attention**
```
e_{ij} = h_i^T W s_j
```

**Self-Attention**
```
Attention(Q,K,V) = softmax(QK^T/√d_k)V
```

### Transformer Architecture

**Multi-Head Attention**
```
MultiHead(Q,K,V) = Concat(head_1,...,head_h)W^O
head_i = Attention(QW_i^Q, KW_i^K, VW_i^V)
```

**Positional Encoding**
```
PE(pos,2i) = sin(pos/10000^{2i/d_{model}})
PE(pos,2i+1) = cos(pos/10000^{2i/d_{model}})
```

**Layer Normalization**
```
LayerNorm(x) = γ(x-μ)/σ + β
```

## Text Classification Theory

### Feature Extraction
- **Bag of Words**: Term frequency representation
- **TF-IDF**: Term frequency × inverse document frequency
- **N-grams**: Sequence patterns
- **Word Embeddings**: Dense representations

### Classification Algorithms
- **Naive Bayes**: Independence assumption
- **SVM**: Maximum margin classification
- **Logistic Regression**: Linear decision boundary
- **Neural Networks**: Non-linear transformations

### Evaluation Metrics
- **Accuracy**: Correct predictions / total predictions
- **Precision**: TP / (TP + FP)
- **Recall**: TP / (TP + FN)
- **F1-Score**: Harmonic mean of precision and recall

## Information Extraction

### Named Entity Recognition
- **BIO Tagging**: Begin-Inside-Outside scheme
- **CRF**: Conditional Random Fields
- **BiLSTM-CRF**: Neural + structured prediction

### Relation Extraction
- **Pattern-based**: Hand-crafted rules
- **Supervised**: Labeled training data
- **Distant Supervision**: Automatic labeling
- **Neural**: End-to-end learning

### Dependency Parsing
- **Transition-based**: Arc-standard, arc-eager
- **Graph-based**: Maximum spanning tree
- **Neural**: BiLSTM + attention

## Language Generation

### Text Generation Models
- **Template-based**: Fill-in-the-blank approach
- **Statistical**: N-gram based generation
- **Neural**: RNN/Transformer language models

### Decoding Strategies
- **Greedy**: Always pick highest probability
- **Beam Search**: Keep top-k hypotheses
- **Sampling**: Random selection from distribution
- **Top-k/Top-p**: Truncated sampling

### Evaluation Metrics
- **BLEU**: N-gram overlap with references
- **ROUGE**: Recall-oriented evaluation
- **METEOR**: Alignment-based metric
- **Perplexity**: Language model quality

## Advanced Topics

### Transfer Learning in NLP
- **Pre-training**: Large corpus language modeling
- **Fine-tuning**: Task-specific adaptation
- **Feature Extraction**: Fixed representations
- **Multi-task Learning**: Joint training

### Large Language Models
- **GPT**: Generative pre-training
- **T5**: Text-to-text transfer transformer
- **Scaling Laws**: Performance vs model size
- **Emergent Abilities**: Capabilities from scale

### Multilingual NLP
- **Cross-lingual Embeddings**: Shared representation space
- **Zero-shot Transfer**: No target language data
- **Code-switching**: Mixed language processing
- **Universal Dependencies**: Cross-lingual parsing

This covers the theoretical foundations of natural language processing.