# Module 2: Setup & Configuration

---

## 2.1 Prerequisites

### Atlas Cluster Requirements

| Requirement | Details |
|-------------|---------|
| Cluster Tier | M10+ (Vector Search not available on M0/M2/M5) |
| MongoDB Version | 7.0.2+ (8.0 recommended for quantization) |
| Region | Any Atlas region |
| Cloud Provider | AWS, GCP, or Azure |

### Local Development (Atlas CLI)

```bash
# Install Atlas CLI
brew install mongodb-atlas-cli

# Login
atlas auth login

# Create a free cluster for testing (limited vector search)
atlas clusters create myCluster --provider AWS --region US_EAST_1 --tier M10
```

---

## 2.2 Collection Setup

### Schema Design for Vector Search

```javascript
// Optimal document structure
{
  _id: ObjectId("..."),
  
  // Your operational data
  title: "Introduction to MongoDB Vector Search",
  content: "MongoDB Atlas Vector Search enables...",
  category: "technology",
  author: "John Doe",
  publishedAt: ISODate("2025-01-15"),
  tags: ["mongodb", "vector-search", "ai"],
  
  // Vector embedding field
  embedding: [0.12, -0.34, 0.56, ...],  // 1536 dimensions
  
  // Metadata for filtering
  metadata: {
    source: "blog",
    language: "en",
    wordCount: 1250,
    tenantId: "tenant-abc"
  }
}
```

### Create Collection with Validation

```javascript
db.createCollection("documents", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["title", "content", "embedding"],
      properties: {
        title: { bsonType: "string" },
        content: { bsonType: "string" },
        embedding: {
          bsonType: "array",
          items: { bsonType: "double" },
          description: "Vector embedding array"
        }
      }
    }
  }
});
```

---

## 2.3 Creating Vector Search Indexes

### Method 1: Atlas UI

1. Navigate to **Atlas Search** → **Create Search Index**
2. Select **Atlas Vector Search** (JSON Editor)
3. Paste index definition

### Method 2: Atlas CLI

```bash
atlas clusters search indexes create \
  --clusterName myCluster \
  --db mydb \
  --collection documents \
  --file vector-index.json
```

### Method 3: MongoDB Driver (Programmatic)

```python
from pymongo import MongoClient
from pymongo.operations import SearchIndexModel

client = MongoClient("mongodb+srv://...")
collection = client["mydb"]["documents"]

# Create vector search index
index_model = SearchIndexModel(
    definition={
        "fields": [
            {
                "type": "vector",
                "path": "embedding",
                "numDimensions": 1536,
                "similarity": "cosine"
            },
            {
                "type": "filter",
                "path": "category"
            },
            {
                "type": "filter",
                "path": "metadata.tenantId"
            },
            {
                "type": "filter",
                "path": "publishedAt"
            }
        ]
    },
    name="vector_index",
    type="vectorSearch"
)

collection.create_search_index(model=index_model)
```

### Method 4: Node.js Driver

```javascript
const { MongoClient } = require('mongodb');

const client = new MongoClient(process.env.MONGODB_URI);
const collection = client.db('mydb').collection('documents');

await collection.createSearchIndex({
  name: 'vector_index',
  type: 'vectorSearch',
  definition: {
    fields: [
      {
        type: 'vector',
        path: 'embedding',
        numDimensions: 1536,
        similarity: 'cosine'
      },
      { type: 'filter', path: 'category' },
      { type: 'filter', path: 'metadata.tenantId' }
    ]
  }
});
```

---

## 2.4 Index Definition Reference

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 1536,
      "similarity": "cosine",
      "quantization": "scalar"
    },
    {
      "type": "filter",
      "path": "category"
    },
    {
      "type": "filter",
      "path": "metadata.tenantId"
    },
    {
      "type": "filter",
      "path": "publishedAt"
    },
    {
      "type": "filter",
      "path": "tags"
    }
  ]
}
```

### Field Options

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | ✅ | `"vector"` or `"filter"` |
| `path` | string | ✅ | Document field path (dot notation supported) |
| `numDimensions` | int | ✅ (vector) | 1-4096 |
| `similarity` | string | ✅ (vector) | `"cosine"`, `"euclidean"`, or `"dotProduct"` |
| `quantization` | string | ❌ | `"none"`, `"scalar"`, or `"binary"` (MongoDB 8.0+) |

### Filter Field Types Supported

- `string` / `objectId` — equality and `$in` filters
- `boolean` — equality filters
- `number` (int, long, double) — range and equality filters
- `date` — range and equality filters
- `uuid` — equality filters

---

## 2.5 Managing Indexes

### List Indexes

```python
indexes = collection.list_search_indexes()
for index in indexes:
    print(f"Name: {index['name']}, Status: {index['status']}")
```

### Update Index

```python
collection.update_search_index(
    name="vector_index",
    definition={
        "fields": [
            {
                "type": "vector",
                "path": "embedding",
                "numDimensions": 1536,
                "similarity": "cosine",
                "quantization": "scalar"  # Add quantization
            },
            {"type": "filter", "path": "category"},
            {"type": "filter", "path": "metadata.tenantId"},
            {"type": "filter", "path": "status"}  # New filter field
        ]
    }
)
```

### Delete Index

```python
collection.drop_search_index("vector_index")
```

### Index Status

| Status | Meaning |
|--------|---------|
| `NOT_STARTED` | Index creation queued |
| `IN_PROGRESS` | Building index |
| `READY` | Index is queryable |
| `STALE` | Index update in progress (old version still queryable) |
| `FAILED` | Index creation failed |

---

## 2.6 SDK Setup

### Python (pymongo + openai)

```bash
pip install pymongo[srv] openai python-dotenv
```

```python
# config.py
import os
from pymongo import MongoClient
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

mongo_client = MongoClient(os.environ["MONGODB_URI"])
db = mongo_client[os.environ["MONGODB_DATABASE"]]
collection = db["documents"]

openai_client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

def get_embedding(text: str, model="text-embedding-3-small") -> list[float]:
    text = text.replace("\n", " ").strip()
    return openai_client.embeddings.create(input=text, model=model).data[0].embedding
```

### Node.js (mongodb + openai)

```bash
npm install mongodb openai dotenv
```

```javascript
// config.js
const { MongoClient } = require('mongodb');
const OpenAI = require('openai');
require('dotenv').config();

const mongoClient = new MongoClient(process.env.MONGODB_URI);
const db = mongoClient.db(process.env.MONGODB_DATABASE);
const collection = db.collection('documents');

const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

async function getEmbedding(text, model = 'text-embedding-3-small') {
  const response = await openai.embeddings.create({ input: text.replace(/\n/g, ' '), model });
  return response.data[0].embedding;
}

module.exports = { collection, getEmbedding };
```

### Java (Spring Boot)

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.1.0</version>
</dependency>
```

```java
@Configuration
public class MongoVectorConfig {
    
    @Bean
    public MongoCollection<Document> vectorCollection(MongoClient mongoClient) {
        return mongoClient.getDatabase("mydb").getCollection("documents");
    }
}
```

---

## 2.7 Environment Variables

```bash
# .env
MONGODB_URI=mongodb+srv://<user>:<password>@cluster.mongodb.net/?retryWrites=true&w=majority
MONGODB_DATABASE=vector_search_db
OPENAI_API_KEY=sk-...
COHERE_API_KEY=...
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_DIMENSIONS=1536
```

---

## 2.8 Verifying Setup

```python
# verify_setup.py
from config import collection, get_embedding

# 1. Test embedding generation
embedding = get_embedding("Hello, MongoDB Vector Search!")
print(f"✅ Embedding generated: {len(embedding)} dimensions")

# 2. Insert test document
collection.insert_one({
    "title": "Test Document",
    "content": "This is a test for vector search setup.",
    "embedding": embedding
})
print("✅ Document inserted with embedding")

# 3. Test vector search
results = list(collection.aggregate([
    {
        "$vectorSearch": {
            "index": "vector_index",
            "path": "embedding",
            "queryVector": embedding,
            "numCandidates": 10,
            "limit": 5
        }
    }
]))
print(f"✅ Vector search returned {len(results)} results")

# 4. Cleanup
collection.delete_many({"title": "Test Document"})
print("✅ Setup verified successfully!")
```

---

## Next: [Module 3 — Index Types & Architecture →](03_Index_Architecture.md)
