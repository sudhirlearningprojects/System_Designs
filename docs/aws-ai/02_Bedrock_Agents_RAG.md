# 2. Amazon Bedrock Agents & RAG

## Theory: Bedrock's RAG Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              BEDROCK KNOWLEDGE BASES (Managed RAG)                │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  DATA SOURCES                    PROCESSING                       │
│  ┌──────────┐                   ┌──────────────────────────┐    │
│  │ S3 Bucket│──────────────────►│ Auto: chunk, embed, index │    │
│  │ (PDFs,   │                   │ (Titan Embeddings)         │    │
│  │  docs)   │                   └────────────┬─────────────┘    │
│  └──────────┘                                │                    │
│                                    ┌─────────▼──────────┐        │
│                                    │ Vector Store        │        │
│                                    │ (OpenSearch / Pinecone│       │
│                                    │  / Aurora / Redis)   │        │
│                                    └─────────┬──────────┘        │
│                                              │                    │
│  QUERY TIME                                  │                    │
│  User Query ──► Embed ──► Search ──► Retrieve ──► LLM ──► Answer │
│                                                                   │
│  ALL MANAGED — no code needed for basic RAG!                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Bedrock Knowledge Bases (Managed RAG)

### Create via AWS CLI

```bash
# 1. Create S3 bucket with documents
aws s3 mb s3://my-knowledge-base-docs
aws s3 cp ./documents/ s3://my-knowledge-base-docs/ --recursive

# 2. Create Knowledge Base
aws bedrock-agent create-knowledge-base \
  --name "customer-support-kb" \
  --description "Customer support documentation" \
  --role-arn "arn:aws:iam::123456789:role/BedrockKBRole" \
  --knowledge-base-configuration '{
    "type": "VECTOR",
    "vectorKnowledgeBaseConfiguration": {
      "embeddingModelArn": "arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-embed-text-v2:0"
    }
  }' \
  --storage-configuration '{
    "type": "OPENSEARCH_SERVERLESS",
    "opensearchServerlessConfiguration": {
      "collectionArn": "arn:aws:aoss:us-east-1:123456789:collection/my-collection",
      "vectorIndexName": "kb-index",
      "fieldMapping": {
        "vectorField": "embedding",
        "textField": "text",
        "metadataField": "metadata"
      }
    }
  }'

# 3. Create Data Source (link S3 to KB)
aws bedrock-agent create-data-source \
  --knowledge-base-id "KB_ID" \
  --name "s3-docs" \
  --data-source-configuration '{
    "type": "S3",
    "s3Configuration": {
      "bucketArn": "arn:aws:s3:::my-knowledge-base-docs"
    }
  }' \
  --vector-ingestion-configuration '{
    "chunkingConfiguration": {
      "chunkingStrategy": "SEMANTIC",
      "semanticChunkingConfiguration": {
        "maxTokens": 512,
        "bufferSize": 0,
        "breakpointPercentileThreshold": 95
      }
    }
  }'

# 4. Sync (ingest documents)
aws bedrock-agent start-ingestion-job \
  --knowledge-base-id "KB_ID" \
  --data-source-id "DS_ID"
```

### Query Knowledge Base (Python)

```python
import boto3

bedrock_agent_runtime = boto3.client("bedrock-agent-runtime", region_name="us-east-1")

# Retrieve and Generate (RAG in one call)
response = bedrock_agent_runtime.retrieve_and_generate(
    input={"text": "What is the refund policy?"},
    retrieveAndGenerateConfiguration={
        "type": "KNOWLEDGE_BASE",
        "knowledgeBaseConfiguration": {
            "knowledgeBaseId": "KB_ID",
            "modelArn": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-4-20250514-v1:0",
            "retrievalConfiguration": {
                "vectorSearchConfiguration": {
                    "numberOfResults": 5,
                    "overrideSearchType": "HYBRID",  # SEMANTIC or HYBRID
                }
            },
            "generationConfiguration": {
                "inferenceConfig": {
                    "textInferenceConfig": {
                        "maxTokens": 500,
                        "temperature": 0.0,
                    }
                },
                "promptTemplate": {
                    "textPromptTemplate": "Answer based ONLY on the search results below.\n\n$search_results$\n\nQuestion: $query$\nAnswer:"
                },
            },
        },
    },
)

print(response["output"]["text"])

# Citations (which documents were used)
for citation in response.get("citations", []):
    for ref in citation.get("retrievedReferences", []):
        print(f"Source: {ref['location']['s3Location']['uri']}")
        print(f"Content: {ref['content']['text'][:100]}...")
```

### Retrieve Only (Custom Generation)

```python
# Just retrieve — you handle generation yourself
response = bedrock_agent_runtime.retrieve(
    knowledgeBaseId="KB_ID",
    retrievalQuery={"text": "How do I cancel my subscription?"},
    retrievalConfiguration={
        "vectorSearchConfiguration": {
            "numberOfResults": 10,
            "overrideSearchType": "HYBRID",
            "filter": {
                "equals": {"key": "category", "value": "billing"}
            },
        }
    },
)

# Use retrieved chunks with your own LLM call
chunks = [r["content"]["text"] for r in response["retrievalResults"]]
scores = [r["score"] for r in response["retrievalResults"]]
```

---

## Bedrock Agents (Autonomous Agents)

### Theory

```
Bedrock Agent = LLM + Knowledge Bases + Action Groups (tools)

The agent:
1. Receives user query
2. Reasons about what to do (ReAct-style)
3. Calls Knowledge Base for information (RAG)
4. Calls Action Groups for actions (APIs, Lambda)
5. Synthesizes final response

All managed — AWS handles orchestration, memory, and tool routing.
```

### Create Agent

```bash
# Create agent
aws bedrock-agent create-agent \
  --agent-name "support-agent" \
  --foundation-model "anthropic.claude-sonnet-4-20250514-v1:0" \
  --instruction "You are a customer support agent for our SaaS product. Help users with billing, technical issues, and account management. Always be helpful and empathetic." \
  --agent-resource-role-arn "arn:aws:iam::123456789:role/BedrockAgentRole"

# Associate Knowledge Base
aws bedrock-agent associate-agent-knowledge-base \
  --agent-id "AGENT_ID" \
  --knowledge-base-id "KB_ID" \
  --description "Customer support documentation and FAQs"
```

### Action Groups (Tools via Lambda)

```python
# Lambda function for agent action group
import json

def lambda_handler(event, context):
    """Handle agent action group invocations."""
    action = event.get("actionGroup")
    api_path = event.get("apiPath")
    parameters = event.get("parameters", [])
    
    # Extract parameters
    params = {p["name"]: p["value"] for p in parameters}
    
    if api_path == "/get-subscription":
        user_id = params.get("userId")
        result = {
            "plan": "Pro",
            "status": "active",
            "renewal_date": "2024-02-15",
            "price": "$19.99/month",
        }
    elif api_path == "/cancel-subscription":
        user_id = params.get("userId")
        reason = params.get("reason", "not specified")
        result = {
            "status": "cancelled",
            "effective_date": "2024-02-15",
            "confirmation_id": "CANCEL-12345",
        }
    else:
        result = {"error": f"Unknown action: {api_path}"}
    
    return {
        "messageVersion": "1.0",
        "response": {
            "actionGroup": action,
            "apiPath": api_path,
            "httpMethod": "POST",
            "httpStatusCode": 200,
            "responseBody": {
                "application/json": {"body": json.dumps(result)}
            },
        },
    }
```

```bash
# Create action group with OpenAPI schema
aws bedrock-agent create-agent-action-group \
  --agent-id "AGENT_ID" \
  --action-group-name "subscription-management" \
  --action-group-executor '{"lambda": "arn:aws:lambda:us-east-1:123456789:function:agent-actions"}' \
  --api-schema '{
    "payload": "{\"openapi\":\"3.0.0\",\"paths\":{\"/get-subscription\":{\"post\":{\"description\":\"Get user subscription details\",\"parameters\":[{\"name\":\"userId\",\"in\":\"query\",\"required\":true,\"schema\":{\"type\":\"string\"}}]}},\"/cancel-subscription\":{\"post\":{\"description\":\"Cancel user subscription. Always confirm with user first.\",\"parameters\":[{\"name\":\"userId\",\"in\":\"query\",\"required\":true,\"schema\":{\"type\":\"string\"}},{\"name\":\"reason\",\"in\":\"query\",\"required\":true,\"schema\":{\"type\":\"string\"}}]}}}}"
  }'

# Prepare and create alias (deploy)
aws bedrock-agent prepare-agent --agent-id "AGENT_ID"
aws bedrock-agent create-agent-alias --agent-id "AGENT_ID" --agent-alias-name "production"
```

### Invoke Agent (Python)

```python
import boto3
import uuid

bedrock_agent_runtime = boto3.client("bedrock-agent-runtime", region_name="us-east-1")

# Create session (maintains conversation state)
session_id = str(uuid.uuid4())

# Invoke agent
response = bedrock_agent_runtime.invoke_agent(
    agentId="AGENT_ID",
    agentAliasId="ALIAS_ID",
    sessionId=session_id,
    inputText="I want to cancel my subscription. My user ID is u-123.",
)

# Stream response
full_response = ""
for event in response["completion"]:
    if "chunk" in event:
        chunk_text = event["chunk"]["bytes"].decode("utf-8")
        full_response += chunk_text
        print(chunk_text, end="", flush=True)

# Agent will:
# 1. Call get-subscription to check current plan
# 2. Confirm with user before cancelling
# 3. Call cancel-subscription if confirmed
# 4. Provide confirmation details

# Multi-turn (same session maintains context)
response2 = bedrock_agent_runtime.invoke_agent(
    agentId="AGENT_ID",
    agentAliasId="ALIAS_ID",
    sessionId=session_id,  # Same session!
    inputText="Yes, please proceed with the cancellation.",
)
```

---

## Bedrock Guardrails with Agents

```python
# Apply guardrails to agent
response = bedrock_agent_runtime.invoke_agent(
    agentId="AGENT_ID",
    agentAliasId="ALIAS_ID",
    sessionId=session_id,
    inputText="Give me financial investment advice",
    # Guardrail applied at agent level (configured during creation)
)
# Agent will refuse based on guardrail topic policy
```

---

## Custom RAG (Without Knowledge Bases)

```python
# For maximum control: build your own RAG with Bedrock + OpenSearch

import boto3
from opensearchpy import OpenSearch, RequestsHttpConnection
from requests_aws4auth import AWS4Auth

# 1. Embed query
bedrock = boto3.client("bedrock-runtime")
embed_response = bedrock.invoke_model(
    modelId="amazon.titan-embed-text-v2:0",
    body=json.dumps({"inputText": "refund policy", "dimensions": 1024, "normalize": True}),
)
query_embedding = json.loads(embed_response["body"].read())["embedding"]

# 2. Search OpenSearch
credentials = boto3.Session().get_credentials()
auth = AWS4Auth(credentials.access_key, credentials.secret_key, "us-east-1", "aoss",
                session_token=credentials.token)

os_client = OpenSearch(
    hosts=[{"host": "my-collection.us-east-1.aoss.amazonaws.com", "port": 443}],
    http_auth=auth, use_ssl=True, connection_class=RequestsHttpConnection,
)

search_results = os_client.search(
    index="documents",
    body={
        "size": 5,
        "query": {"knn": {"embedding": {"vector": query_embedding, "k": 5}}},
    },
)

# 3. Generate with context
context = "\n\n".join([hit["_source"]["text"] for hit in search_results["hits"]["hits"]])

response = bedrock.converse(
    modelId="anthropic.claude-sonnet-4-20250514-v1:0",
    messages=[{"role": "user", "content": [{"text": f"Context:\n{context}\n\nQuestion: What is the refund policy?"}]}],
    system=[{"text": "Answer based ONLY on the provided context. Cite sources."}],
)
print(response["output"]["message"]["content"][0]["text"])
```

---

## Next: [Amazon SageMaker →](03_SageMaker.md)
