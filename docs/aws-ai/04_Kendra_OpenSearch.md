# 4. Amazon Kendra & OpenSearch

## Amazon Kendra (Enterprise Search)

Fully managed intelligent search with 40+ connectors (S3, SharePoint, Salesforce, databases).

```python
import boto3

kendra = boto3.client("kendra", region_name="us-east-1")

# Query Kendra index
response = kendra.query(
    IndexId="your-index-id",
    QueryText="What is the refund policy?",
    QueryResultTypeFilter="DOCUMENT",  # DOCUMENT, QUESTION_ANSWER, ANSWER
    PageSize=5,
)

for result in response["ResultItems"]:
    print(f"Type: {result['Type']}")
    print(f"Title: {result.get('DocumentTitle', {}).get('Text', 'N/A')}")
    print(f"Excerpt: {result.get('DocumentExcerpt', {}).get('Text', '')[:200]}")
    print(f"Score: {result['ScoreAttributes']['ScoreConfidence']}")
    print()

# Use with Bedrock for RAG
context = "\n\n".join([r["DocumentExcerpt"]["Text"] for r in response["ResultItems"]])
# Pass context to Bedrock LLM for answer generation
```

---

## Amazon OpenSearch Serverless (Vector Search)

```python
from opensearchpy import OpenSearch, RequestsHttpConnection
from requests_aws4auth import AWS4Auth
import boto3

# Connect to OpenSearch Serverless
credentials = boto3.Session().get_credentials()
auth = AWS4Auth(credentials.access_key, credentials.secret_key, "us-east-1", "aoss",
                session_token=credentials.token)

client = OpenSearch(
    hosts=[{"host": "my-collection.us-east-1.aoss.amazonaws.com", "port": 443}],
    http_auth=auth, use_ssl=True, verify_certs=True,
    connection_class=RequestsHttpConnection,
)

# Create vector index
client.indices.create("documents", body={
    "settings": {"index": {"knn": True}},
    "mappings": {
        "properties": {
            "embedding": {"type": "knn_vector", "dimension": 1024, "method": {
                "name": "hnsw", "engine": "faiss", "parameters": {"m": 16, "ef_construction": 512}
            }},
            "text": {"type": "text"},
            "metadata": {"type": "object"},
        }
    }
})

# Index document with embedding
client.index(index="documents", body={
    "text": "Refunds are available within 14 days of purchase.",
    "embedding": embedding_vector,  # From Bedrock Titan Embeddings
    "metadata": {"source": "billing-faq.md", "category": "billing"},
})

# Vector search (KNN)
results = client.search(index="documents", body={
    "size": 5,
    "query": {"knn": {"embedding": {"vector": query_embedding, "k": 5}}},
})

# Hybrid search (vector + keyword)
results = client.search(index="documents", body={
    "size": 5,
    "query": {
        "hybrid": {
            "queries": [
                {"knn": {"embedding": {"vector": query_embedding, "k": 20}}},
                {"match": {"text": "refund policy"}},
            ]
        }
    }
})
```

---

## Next: [Amazon Textract & Comprehend →](05_Textract_Comprehend.md)
