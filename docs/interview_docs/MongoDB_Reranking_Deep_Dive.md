# MongoDB Reranking - Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [What is Reranking?](#what-is-reranking)
3. [Vector Search Basics](#vector-search-basics)
4. [Reranking Strategies](#reranking-strategies)
5. [MongoDB Atlas Vector Search](#mongodb-atlas-vector-search)
6. [Hybrid Search with Reranking](#hybrid-search-with-reranking)
7. [Real-World Implementation](#real-world-implementation)
8. [Performance Optimization](#performance-optimization)
9. [Best Practices](#best-practices)

---

## Introduction

**Reranking** is a technique to improve search relevance by re-ordering initial search results using additional signals, models, or business logic.

### Why Reranking?

**Problem**: Initial search (vector or text) may not capture all relevance factors
- Vector search: Semantic similarity only
- Text search: Keyword matching only
- Missing: Recency, popularity, user preferences, business rules

**Solution**: Two-stage retrieval
1. **Stage 1 (Retrieval)**: Fast, broad search (1000s of candidates)
2. **Stage 2 (Reranking)**: Precise, expensive scoring (top 100)

---

## What is Reranking?

### Traditional Search Flow
```
Query → Search Index → Results (sorted by score)
```

### Reranking Flow
```
Query → Search Index → Candidates (1000) → Reranker → Final Results (20)
```

### Key Concepts

**1. Candidate Generation**: Fast retrieval of potential matches
**2. Reranking**: Expensive but accurate re-scoring
**3. Fusion**: Combining multiple ranking signals

---

## Vector Search Basics

### What are Vector Embeddings?

Embeddings convert text/images into numerical vectors that capture semantic meaning.

```javascript
// Text to vector
"MongoDB is a NoSQL database" → [0.23, -0.45, 0.67, ..., 0.12] // 768 dimensions

// Similar texts have similar vectors
"MongoDB is a document database" → [0.25, -0.43, 0.65, ..., 0.14]
```

### Vector Similarity Metrics

**1. Cosine Similarity** (Most common)
```javascript
similarity = (A · B) / (||A|| × ||B||)
Range: [-1, 1] where 1 = identical, 0 = orthogonal, -1 = opposite
```

**2. Euclidean Distance**
```javascript
distance = √(Σ(Ai - Bi)²)
Range: [0, ∞] where 0 = identical
```

**3. Dot Product**
```javascript
similarity = Σ(Ai × Bi)
Range: [-∞, ∞]
```

---

## Reranking Strategies

### 1. Score-Based Reranking

Combine multiple scores with weights:

```javascript
finalScore = w1 × vectorScore + w2 × textScore + w3 × popularityScore + w4 × recencyScore

// Example
finalScore = 0.5 × 0.85 + 0.2 × 0.70 + 0.2 × 0.90 + 0.1 × 0.60
           = 0.425 + 0.14 + 0.18 + 0.06
           = 0.805
```

### 2. Cross-Encoder Reranking

Use a transformer model to score query-document pairs:

```javascript
// Stage 1: Bi-encoder (fast, approximate)
query_embedding = encode(query)
candidates = vector_search(query_embedding, top_k=1000)

// Stage 2: Cross-encoder (slow, accurate)
for (doc in candidates) {
    score = cross_encoder(query, doc.text)
    doc.rerank_score = score
}
results = sort(candidates, by=rerank_score, limit=20)
```

### 3. Learning to Rank (LTR)

Machine learning model trained on relevance judgments:

```javascript
features = [
    vector_score,
    text_score,
    doc_length,
    query_term_coverage,
    click_through_rate,
    dwell_time,
    recency_days,
    author_reputation
]

rerank_score = LTR_model.predict(features)
```

### 4. Reciprocal Rank Fusion (RRF)

Combine rankings from multiple sources:

```javascript
// Formula
RRF_score = Σ(1 / (k + rank_i))

// Example: Combine vector search + text search
// Document appears at rank 3 in vector search, rank 5 in text search
k = 60 (constant)
RRF_score = 1/(60+3) + 1/(60+5) = 0.0159 + 0.0154 = 0.0313
```

---

## MongoDB Atlas Vector Search

### Setup Vector Search Index

```javascript
// Create vector search index
db.products.createSearchIndex({
  name: "vector_index",
  type: "vectorSearch",
  definition: {
    fields: [{
      type: "vector",
      path: "embedding",
      numDimensions: 768,
      similarity: "cosine"
    }]
  }
});
```

### Basic Vector Search Query

```javascript
db.products.aggregate([
  {
    $vectorSearch: {
      index: "vector_index",
      path: "embedding",
      queryVector: [0.23, -0.45, 0.67, ...], // 768 dimensions
      numCandidates: 1000,  // Candidates to consider
      limit: 20             // Final results
    }
  },
  {
    $project: {
      _id: 1,
      name: 1,
      description: 1,
      score: { $meta: "vectorSearchScore" }
    }
  }
]);
```

### Vector Search with Filters

```javascript
db.products.aggregate([
  {
    $vectorSearch: {
      index: "vector_index",
      path: "embedding",
      queryVector: queryEmbedding,
      numCandidates: 1000,
      limit: 20,
      filter: {
        category: "electronics",
        price: { $lte: 1000 },
        inStock: true
      }
    }
  }
]);
```

---

## Hybrid Search with Reranking

### Strategy 1: Weighted Score Fusion

```javascript
// Step 1: Vector search
const vectorResults = await db.products.aggregate([
  {
    $vectorSearch: {
      index: "vector_index",
      path: "embedding",
      queryVector: queryEmbedding,
      numCandidates: 1000,
      limit: 100
    }
  },
  {
    $project: {
      _id: 1,
      name: 1,
      description: 1,
      vectorScore: { $meta: "vectorSearchScore" }
    }
  }
]).toArray();

// Step 2: Text search
const textResults = await db.products.aggregate([
  {
    $search: {
      index: "text_index",
      text: {
        query: queryText,
        path: ["name", "description"]
      }
    }
  },
  {
    $limit: 100
  },
  {
    $project: {
      _id: 1,
      name: 1,
      description: 1,
      textScore: { $meta: "searchScore" }
    }
  }
]).toArray();

// Step 3: Merge and rerank
const mergedResults = mergeAndRerank(vectorResults, textResults, {
  vectorWeight: 0.6,
  textWeight: 0.4
});

function mergeAndRerank(vectorResults, textResults, weights) {
  const scoreMap = new Map();
  
  // Normalize and combine scores
  vectorResults.forEach(doc => {
    scoreMap.set(doc._id.toString(), {
      ...doc,
      finalScore: doc.vectorScore * weights.vectorWeight
    });
  });
  
  textResults.forEach(doc => {
    const existing = scoreMap.get(doc._id.toString());
    if (existing) {
      existing.finalScore += doc.textScore * weights.textWeight;
    } else {
      scoreMap.set(doc._id.toString(), {
        ...doc,
        finalScore: doc.textScore * weights.textWeight
      });
    }
  });
  
  // Sort by final score
  return Array.from(scoreMap.values())
    .sort((a, b) => b.finalScore - a.finalScore)
    .slice(0, 20);
}
```

### Strategy 2: Reciprocal Rank Fusion (RRF)

```javascript
async function hybridSearchWithRRF(queryText, queryEmbedding) {
  // Get vector search results
  const vectorResults = await db.products.aggregate([
    {
      $vectorSearch: {
        index: "vector_index",
        path: "embedding",
        queryVector: queryEmbedding,
        numCandidates: 1000,
        limit: 100
      }
    },
    { $addFields: { vectorRank: { $add: [{ $indexOfArray: ["$$ROOT", "$_id"] }, 1] } } }
  ]).toArray();
  
  // Get text search results
  const textResults = await db.products.aggregate([
    {
      $search: {
        index: "text_index",
        text: { query: queryText, path: ["name", "description"] }
      }
    },
    { $limit: 100 },
    { $addFields: { textRank: { $add: [{ $indexOfArray: ["$$ROOT", "$_id"] }, 1] } } }
  ]).toArray();
  
  // Apply RRF
  const k = 60;
  const rrfScores = new Map();
  
  vectorResults.forEach((doc, index) => {
    const rank = index + 1;
    rrfScores.set(doc._id.toString(), {
      ...doc,
      rrfScore: 1 / (k + rank)
    });
  });
  
  textResults.forEach((doc, index) => {
    const rank = index + 1;
    const existing = rrfScores.get(doc._id.toString());
    if (existing) {
      existing.rrfScore += 1 / (k + rank);
    } else {
      rrfScores.set(doc._id.toString(), {
        ...doc,
        rrfScore: 1 / (k + rank)
      });
    }
  });
  
  return Array.from(rrfScores.values())
    .sort((a, b) => b.rrfScore - a.rrfScore)
    .slice(0, 20);
}
```

### Strategy 3: Business Logic Reranking

```javascript
async function rerankWithBusinessLogic(candidates) {
  return candidates.map(doc => {
    let finalScore = doc.vectorScore;
    
    // Boost recent items (within 30 days)
    const daysSinceCreated = (Date.now() - doc.createdAt) / (1000 * 60 * 60 * 24);
    if (daysSinceCreated <= 30) {
      finalScore *= 1.2;
    }
    
    // Boost popular items
    if (doc.viewCount > 10000) {
      finalScore *= 1.15;
    }
    
    // Boost high-rated items
    if (doc.rating >= 4.5) {
      finalScore *= 1.1;
    }
    
    // Penalize out-of-stock
    if (!doc.inStock) {
      finalScore *= 0.5;
    }
    
    // Boost premium sellers
    if (doc.sellerTier === 'premium') {
      finalScore *= 1.05;
    }
    
    return { ...doc, finalScore };
  })
  .sort((a, b) => b.finalScore - a.finalScore)
  .slice(0, 20);
}
```

---

## Real-World Implementation

### Use Case: E-commerce Product Search

```javascript
class ProductSearchService {
  
  async search(query, options = {}) {
    const {
      category = null,
      priceRange = null,
      inStockOnly = false,
      limit = 20
    } = options;
    
    // Step 1: Generate query embedding
    const queryEmbedding = await this.generateEmbedding(query);
    
    // Step 2: Vector search (semantic)
    const vectorCandidates = await this.vectorSearch(queryEmbedding, {
      category,
      priceRange,
      inStockOnly,
      limit: 100
    });
    
    // Step 3: Text search (keyword)
    const textCandidates = await this.textSearch(query, {
      category,
      priceRange,
      inStockOnly,
      limit: 100
    });
    
    // Step 4: Merge with RRF
    const mergedResults = this.applyRRF(vectorCandidates, textCandidates);
    
    // Step 5: Apply business logic reranking
    const rerankedResults = this.applyBusinessLogic(mergedResults);
    
    // Step 6: Personalization (optional)
    const personalizedResults = await this.personalizeResults(
      rerankedResults,
      options.userId
    );
    
    return personalizedResults.slice(0, limit);
  }
  
  async vectorSearch(queryEmbedding, filters) {
    const pipeline = [
      {
        $vectorSearch: {
          index: "product_vector_index",
          path: "embedding",
          queryVector: queryEmbedding,
          numCandidates: 1000,
          limit: filters.limit || 100
        }
      }
    ];
    
    // Add filters
    if (filters.category) {
      pipeline.push({ $match: { category: filters.category } });
    }
    if (filters.priceRange) {
      pipeline.push({
        $match: {
          price: {
            $gte: filters.priceRange.min,
            $lte: filters.priceRange.max
          }
        }
      });
    }
    if (filters.inStockOnly) {
      pipeline.push({ $match: { inStock: true } });
    }
    
    pipeline.push({
      $project: {
        _id: 1,
        name: 1,
        description: 1,
        price: 1,
        category: 1,
        rating: 1,
        viewCount: 1,
        inStock: 1,
        createdAt: 1,
        vectorScore: { $meta: "vectorSearchScore" }
      }
    });
    
    return await db.products.aggregate(pipeline).toArray();
  }
  
  async textSearch(query, filters) {
    const pipeline = [
      {
        $search: {
          index: "product_text_index",
          compound: {
            must: [{
              text: {
                query: query,
                path: ["name", "description", "brand"],
                fuzzy: { maxEdits: 1 }
              }
            }]
          }
        }
      },
      { $limit: filters.limit || 100 }
    ];
    
    // Add filters
    const matchConditions = {};
    if (filters.category) matchConditions.category = filters.category;
    if (filters.priceRange) {
      matchConditions.price = {
        $gte: filters.priceRange.min,
        $lte: filters.priceRange.max
      };
    }
    if (filters.inStockOnly) matchConditions.inStock = true;
    
    if (Object.keys(matchConditions).length > 0) {
      pipeline.push({ $match: matchConditions });
    }
    
    pipeline.push({
      $project: {
        _id: 1,
        name: 1,
        description: 1,
        price: 1,
        category: 1,
        rating: 1,
        viewCount: 1,
        inStock: 1,
        createdAt: 1,
        textScore: { $meta: "searchScore" }
      }
    });
    
    return await db.products.aggregate(pipeline).toArray();
  }
  
  applyRRF(vectorResults, textResults, k = 60) {
    const rrfScores = new Map();
    
    vectorResults.forEach((doc, index) => {
      rrfScores.set(doc._id.toString(), {
        ...doc,
        rrfScore: 1 / (k + index + 1),
        sources: ['vector']
      });
    });
    
    textResults.forEach((doc, index) => {
      const id = doc._id.toString();
      const existing = rrfScores.get(id);
      if (existing) {
        existing.rrfScore += 1 / (k + index + 1);
        existing.sources.push('text');
      } else {
        rrfScores.set(id, {
          ...doc,
          rrfScore: 1 / (k + index + 1),
          sources: ['text']
        });
      }
    });
    
    return Array.from(rrfScores.values())
      .sort((a, b) => b.rrfScore - a.rrfScore);
  }
  
  applyBusinessLogic(results) {
    return results.map(doc => {
      let boostFactor = 1.0;
      
      // Recency boost
      const daysOld = (Date.now() - new Date(doc.createdAt)) / (1000 * 60 * 60 * 24);
      if (daysOld <= 7) boostFactor *= 1.3;
      else if (daysOld <= 30) boostFactor *= 1.15;
      
      // Popularity boost
      if (doc.viewCount > 50000) boostFactor *= 1.25;
      else if (doc.viewCount > 10000) boostFactor *= 1.15;
      
      // Rating boost
      if (doc.rating >= 4.8) boostFactor *= 1.2;
      else if (doc.rating >= 4.5) boostFactor *= 1.1;
      
      // Stock penalty
      if (!doc.inStock) boostFactor *= 0.3;
      
      // Multi-source boost (appears in both vector and text)
      if (doc.sources.length > 1) boostFactor *= 1.1;
      
      return {
        ...doc,
        finalScore: doc.rrfScore * boostFactor
      };
    })
    .sort((a, b) => b.finalScore - a.finalScore);
  }
  
  async personalizeResults(results, userId) {
    if (!userId) return results;
    
    // Get user preferences
    const userProfile = await db.users.findOne({ _id: userId });
    if (!userProfile) return results;
    
    return results.map(doc => {
      let personalBoost = 1.0;
      
      // Preferred categories
      if (userProfile.preferredCategories?.includes(doc.category)) {
        personalBoost *= 1.15;
      }
      
      // Price range preference
      if (doc.price >= userProfile.minPrice && doc.price <= userProfile.maxPrice) {
        personalBoost *= 1.1;
      }
      
      // Previously viewed brands
      if (userProfile.viewedBrands?.includes(doc.brand)) {
        personalBoost *= 1.05;
      }
      
      return {
        ...doc,
        finalScore: doc.finalScore * personalBoost
      };
    })
    .sort((a, b) => b.finalScore - a.finalScore);
  }
  
  async generateEmbedding(text) {
    // Use OpenAI, Cohere, or local model
    const response = await openai.embeddings.create({
      model: "text-embedding-3-small",
      input: text
    });
    return response.data[0].embedding;
  }
}
```


### Use Case: Content Recommendation System

```javascript
class ContentRecommendationService {
  
  async recommendArticles(userId, limit = 10) {
    // Get user's reading history
    const userHistory = await db.userActivity.find({
      userId,
      action: 'read'
    }).sort({ timestamp: -1 }).limit(50).toArray();
    
    // Generate user interest embedding (average of read articles)
    const userEmbedding = await this.generateUserEmbedding(userHistory);
    
    // Find similar content
    const candidates = await db.articles.aggregate([
      {
        $vectorSearch: {
          index: "article_vector_index",
          path: "embedding",
          queryVector: userEmbedding,
          numCandidates: 500,
          limit: 100,
          filter: {
            publishedAt: { $gte: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000) }
          }
        }
      },
      {
        $lookup: {
          from: "articleStats",
          localField: "_id",
          foreignField: "articleId",
          as: "stats"
        }
      },
      { $unwind: "$stats" },
      {
        $project: {
          _id: 1,
          title: 1,
          author: 1,
          category: 1,
          publishedAt: 1,
          readTime: 1,
          vectorScore: { $meta: "vectorSearchScore" },
          views: "$stats.views",
          likes: "$stats.likes",
          shares: "$stats.shares",
          avgReadTime: "$stats.avgReadTime"
        }
      }
    ]).toArray();
    
    // Rerank with engagement signals
    const reranked = this.rerankByEngagement(candidates);
    
    // Filter out already read
    const readArticleIds = new Set(userHistory.map(h => h.articleId.toString()));
    const filtered = reranked.filter(a => !readArticleIds.has(a._id.toString()));
    
    return filtered.slice(0, limit);
  }
  
  rerankByEngagement(articles) {
    return articles.map(article => {
      let score = article.vectorScore;
      
      // Engagement rate boost
      const engagementRate = (article.likes + article.shares) / article.views;
      score *= (1 + engagementRate * 0.5);
      
      // Completion rate boost
      const completionRate = article.avgReadTime / article.readTime;
      if (completionRate > 0.8) score *= 1.2;
      
      // Recency boost
      const hoursOld = (Date.now() - article.publishedAt) / (1000 * 60 * 60);
      if (hoursOld <= 24) score *= 1.3;
      else if (hoursOld <= 168) score *= 1.15;
      
      return { ...article, finalScore: score };
    })
    .sort((a, b) => b.finalScore - a.finalScore);
  }
}
```

---

## Performance Optimization

### 1. Index Optimization

```javascript
// Compound index for filtered vector search
db.products.createSearchIndex({
  name: "optimized_vector_index",
  type: "vectorSearch",
  definition: {
    fields: [
      {
        type: "vector",
        path: "embedding",
        numDimensions: 768,
        similarity: "cosine"
      },
      {
        type: "filter",
        path: "category"
      },
      {
        type: "filter",
        path: "inStock"
      },
      {
        type: "filter",
        path: "price"
      }
    ]
  }
});
```

### 2. Caching Strategy

```javascript
class CachedSearchService {
  constructor() {
    this.cache = new Map();
    this.cacheTTL = 5 * 60 * 1000; // 5 minutes
  }
  
  async search(query, filters) {
    const cacheKey = this.generateCacheKey(query, filters);
    
    // Check cache
    const cached = this.cache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < this.cacheTTL) {
      return cached.results;
    }
    
    // Perform search
    const results = await this.performSearch(query, filters);
    
    // Cache results
    this.cache.set(cacheKey, {
      results,
      timestamp: Date.now()
    });
    
    return results;
  }
  
  generateCacheKey(query, filters) {
    return `${query}:${JSON.stringify(filters)}`;
  }
}
```

### 3. Batch Processing

```javascript
async function batchRerank(queries) {
  // Generate all embeddings in parallel
  const embeddings = await Promise.all(
    queries.map(q => generateEmbedding(q.text))
  );
  
  // Batch vector search
  const allCandidates = await Promise.all(
    embeddings.map((emb, i) => 
      vectorSearch(emb, queries[i].filters)
    )
  );
  
  // Batch reranking
  return allCandidates.map((candidates, i) => 
    rerank(candidates, queries[i])
  );
}
```

### 4. Approximate Nearest Neighbors (ANN)

```javascript
// Use HNSW (Hierarchical Navigable Small World) for faster search
db.products.createSearchIndex({
  name: "hnsw_vector_index",
  type: "vectorSearch",
  definition: {
    fields: [{
      type: "vector",
      path: "embedding",
      numDimensions: 768,
      similarity: "cosine",
      // HNSW parameters
      indexOptions: {
        type: "hnsw",
        m: 16,              // Number of connections per layer
        efConstruction: 64  // Size of dynamic candidate list
      }
    }]
  }
});

// Query with ef parameter
db.products.aggregate([
  {
    $vectorSearch: {
      index: "hnsw_vector_index",
      path: "embedding",
      queryVector: queryEmbedding,
      numCandidates: 1000,
      limit: 20,
      // Search-time parameter
      ef: 100  // Higher = more accurate but slower
    }
  }
]);
```

---

## Best Practices

### 1. Two-Stage Retrieval

```javascript
// ✅ Good: Retrieve many, rerank few
const candidates = await vectorSearch(query, { limit: 1000 });
const reranked = await expensiveReranker(candidates.slice(0, 100));
return reranked.slice(0, 20);

// ❌ Bad: Expensive reranking on all results
const allResults = await vectorSearch(query, { limit: 10000 });
const reranked = await expensiveReranker(allResults);
```

### 2. Normalize Scores

```javascript
function normalizeScores(results, scoreField) {
  const scores = results.map(r => r[scoreField]);
  const min = Math.min(...scores);
  const max = Math.max(...scores);
  const range = max - min;
  
  return results.map(r => ({
    ...r,
    [scoreField]: range > 0 ? (r[scoreField] - min) / range : 0
  }));
}

// Use before combining scores
const normalizedVector = normalizeScores(vectorResults, 'vectorScore');
const normalizedText = normalizeScores(textResults, 'textScore');
const combined = combineScores(normalizedVector, normalizedText);
```

### 3. A/B Testing Reranking Strategies

```javascript
class ABTestingSearchService {
  async search(query, userId) {
    const variant = this.getVariant(userId);
    
    switch (variant) {
      case 'A': // Control: Vector only
        return await this.vectorSearch(query);
      
      case 'B': // Variant 1: RRF
        return await this.hybridSearchRRF(query);
      
      case 'C': // Variant 2: Weighted fusion
        return await this.hybridSearchWeighted(query);
      
      case 'D': // Variant 3: Business logic heavy
        return await this.businessLogicRerank(query);
    }
  }
  
  getVariant(userId) {
    const hash = this.hashUserId(userId);
    const bucket = hash % 100;
    
    if (bucket < 25) return 'A';
    if (bucket < 50) return 'B';
    if (bucket < 75) return 'C';
    return 'D';
  }
}
```

### 4. Monitor Reranking Impact

```javascript
async function logSearchMetrics(query, results, userId) {
  await db.searchMetrics.insertOne({
    query,
    userId,
    timestamp: new Date(),
    resultsCount: results.length,
    topResultScore: results[0]?.finalScore,
    avgScore: results.reduce((sum, r) => sum + r.finalScore, 0) / results.length,
    rerankingStrategy: 'RRF',
    latencyMs: Date.now() - startTime,
    // Track user engagement
    clicked: null,  // Updated when user clicks
    clickPosition: null,
    dwellTime: null
  });
}
```

### 5. Fallback Strategies

```javascript
async function searchWithFallback(query) {
  try {
    // Try hybrid search with reranking
    return await hybridSearchWithReranking(query);
  } catch (error) {
    console.error('Hybrid search failed:', error);
    
    try {
      // Fallback to vector search only
      return await vectorSearchOnly(query);
    } catch (error2) {
      console.error('Vector search failed:', error2);
      
      // Final fallback to text search
      return await textSearchOnly(query);
    }
  }
}
```

### 6. Embedding Model Selection

```javascript
// Different models for different use cases
const EMBEDDING_MODELS = {
  // Fast, good for general use
  'text-embedding-3-small': {
    dimensions: 1536,
    costPer1M: 0.02,
    latency: 'low'
  },
  
  // High quality, slower
  'text-embedding-3-large': {
    dimensions: 3072,
    costPer1M: 0.13,
    latency: 'medium'
  },
  
  // Multilingual
  'multilingual-e5-large': {
    dimensions: 1024,
    costPer1M: 0,  // Self-hosted
    latency: 'medium'
  }
};

function selectEmbeddingModel(useCase) {
  switch (useCase) {
    case 'realtime-search':
      return 'text-embedding-3-small';
    case 'high-accuracy':
      return 'text-embedding-3-large';
    case 'multilingual':
      return 'multilingual-e5-large';
  }
}
```

---

## Advanced Techniques

### 1. Cross-Encoder Reranking

```javascript
class CrossEncoderReranker {
  constructor() {
    this.model = loadModel('cross-encoder/ms-marco-MiniLM-L-6-v2');
  }
  
  async rerank(query, candidates) {
    // Create query-document pairs
    const pairs = candidates.map(doc => ({
      query,
      document: doc.text,
      metadata: doc
    }));
    
    // Batch score with cross-encoder
    const scores = await this.model.predict(
      pairs.map(p => [p.query, p.document])
    );
    
    // Combine with original scores
    return candidates.map((doc, i) => ({
      ...doc,
      crossEncoderScore: scores[i],
      finalScore: 0.7 * doc.vectorScore + 0.3 * scores[i]
    }))
    .sort((a, b) => b.finalScore - a.finalScore);
  }
}
```

### 2. Query Expansion

```javascript
async function expandQuery(originalQuery) {
  // Use LLM to generate related queries
  const expandedQueries = await llm.complete({
    prompt: `Generate 3 related search queries for: "${originalQuery}"`,
    maxTokens: 100
  });
  
  // Search with all queries
  const allResults = await Promise.all([
    search(originalQuery),
    ...expandedQueries.map(q => search(q))
  ]);
  
  // Merge with RRF
  return mergeWithRRF(allResults);
}
```

### 3. Semantic Caching

```javascript
class SemanticCache {
  constructor() {
    this.cache = [];
    this.threshold = 0.95; // Cosine similarity threshold
  }
  
  async get(queryEmbedding) {
    for (const entry of this.cache) {
      const similarity = cosineSimilarity(queryEmbedding, entry.embedding);
      if (similarity >= this.threshold) {
        return entry.results;
      }
    }
    return null;
  }
  
  set(queryEmbedding, results) {
    this.cache.push({
      embedding: queryEmbedding,
      results,
      timestamp: Date.now()
    });
    
    // Keep cache size limited
    if (this.cache.length > 1000) {
      this.cache.shift();
    }
  }
}
```

### 4. Multi-Vector Search

```javascript
// Store multiple embeddings per document
db.products.insertOne({
  _id: "prod123",
  name: "iPhone 15 Pro",
  description: "Latest Apple smartphone...",
  embeddings: {
    title: [0.1, 0.2, ...],      // Embedding of title
    description: [0.3, 0.4, ...], // Embedding of description
    reviews: [0.5, 0.6, ...]      // Embedding of aggregated reviews
  }
});

// Search across multiple embeddings
async function multiVectorSearch(query) {
  const queryEmb = await generateEmbedding(query);
  
  const results = await db.products.aggregate([
    {
      $vectorSearch: {
        index: "multi_vector_index",
        queryVector: queryEmb,
        path: "embeddings.title",
        numCandidates: 500,
        limit: 50
      }
    },
    {
      $unionWith: {
        coll: "products",
        pipeline: [
          {
            $vectorSearch: {
              index: "multi_vector_index",
              queryVector: queryEmb,
              path: "embeddings.description",
              numCandidates: 500,
              limit: 50
            }
          }
        ]
      }
    },
    {
      $group: {
        _id: "$_id",
        doc: { $first: "$$ROOT" },
        maxScore: { $max: "$score" }
      }
    },
    { $sort: { maxScore: -1 } },
    { $limit: 20 }
  ]).toArray();
  
  return results;
}
```

---

## Real-World Performance Metrics

### Latency Benchmarks

```
Operation                          | Latency (p50) | Latency (p99)
-----------------------------------|---------------|---------------
Vector Search (1000 candidates)    | 15ms          | 45ms
Text Search                        | 10ms          | 30ms
RRF Merge (200 docs)              | 2ms           | 5ms
Business Logic Rerank (100 docs)   | 5ms           | 15ms
Cross-Encoder Rerank (100 docs)    | 150ms         | 300ms
Total Hybrid Search                | 35ms          | 100ms
```

### Accuracy Improvements

```
Strategy                    | NDCG@10 | MRR    | Recall@20
----------------------------|---------|--------|----------
Vector Search Only          | 0.65    | 0.58   | 0.72
Text Search Only            | 0.58    | 0.52   | 0.68
RRF (Vector + Text)         | 0.73    | 0.67   | 0.81
+ Business Logic            | 0.78    | 0.72   | 0.83
+ Cross-Encoder             | 0.82    | 0.76   | 0.86
+ Personalization           | 0.85    | 0.79   | 0.88
```

### Cost Analysis

```javascript
// Monthly cost for 10M searches
const costs = {
  vectorSearch: {
    atlasVectorSearch: 150,  // $150/month for Atlas tier
    embeddingAPI: 200,       // $0.02 per 1M tokens
    total: 350
  },
  
  hybridWithReranking: {
    atlasVectorSearch: 150,
    atlasTextSearch: 50,
    embeddingAPI: 200,
    crossEncoderCompute: 300,  // Self-hosted GPU
    total: 700
  },
  
  // ROI: 2x cost, 30% better relevance → 50% more conversions
  roi: '2.5x'
};
```

---

## Summary

### Key Takeaways

1. **Two-Stage Retrieval**: Fast candidate generation + expensive reranking
2. **Hybrid Search**: Combine vector (semantic) + text (keyword) search
3. **RRF**: Simple, effective way to merge multiple rankings
4. **Business Logic**: Domain-specific signals improve relevance
5. **Personalization**: User context boosts engagement
6. **Monitor & Iterate**: A/B test strategies, track metrics

### Reranking Decision Tree

```
Start
  │
  ├─ Simple keyword search needed?
  │   └─ Yes → Text search only
  │
  ├─ Semantic understanding needed?
  │   └─ Yes → Vector search
  │
  ├─ Both keyword + semantic?
  │   └─ Yes → Hybrid search (RRF)
  │
  ├─ Domain-specific signals?
  │   └─ Yes → + Business logic reranking
  │
  ├─ High accuracy critical?
  │   └─ Yes → + Cross-encoder reranking
  │
  └─ User-specific results?
      └─ Yes → + Personalization
```

### When to Use Each Strategy

| Strategy | Use When | Latency | Accuracy |
|----------|----------|---------|----------|
| **Vector Only** | Semantic search, no keywords | Low | Medium |
| **Text Only** | Exact keyword matching | Low | Medium |
| **RRF Hybrid** | Best of both worlds | Medium | High |
| **+ Business Logic** | Domain signals available | Medium | Higher |
| **+ Cross-Encoder** | Accuracy critical, latency OK | High | Highest |
| **+ Personalization** | User data available | Medium | Highest |

---

## References

- [MongoDB Atlas Vector Search Docs](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [Reciprocal Rank Fusion Paper](https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf)
- [Cross-Encoders for Reranking](https://www.sbert.net/examples/applications/cross-encoder/README.html)
- [Learning to Rank](https://en.wikipedia.org/wiki/Learning_to_rank)

---

**Pro Tip**: Start simple (vector or text), measure baseline metrics, then incrementally add reranking strategies while A/B testing each change! 🚀
