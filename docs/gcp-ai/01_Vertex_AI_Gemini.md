# 1. Vertex AI & Gemini

## Overview

Vertex AI is Google Cloud's unified ML platform. Gemini is Google's flagship multimodal model family, accessible through Vertex AI with enterprise features (VPC-SC, CMEK, SLA).

---

## Setup

```bash
# Enable APIs
gcloud services enable aiplatform.googleapis.com

# Set default region
gcloud config set ai/region us-central1

# Create service account (for production)
gcloud iam service-accounts create vertex-ai-sa \
  --display-name="Vertex AI Service Account"

gcloud projects add-iam-policy-binding my-project \
  --member="serviceAccount:vertex-ai-sa@my-project.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"
```

---

## Gemini — Text Generation

```python
# pip install google-cloud-aiplatform

import vertexai
from vertexai.generative_models import GenerativeModel, Part, SafetySetting, HarmCategory, HarmBlockThreshold

# Initialize
vertexai.init(project="my-project", location="us-central1")

# Load model
model = GenerativeModel(
    model_name="gemini-2.0-flash-001",
    system_instruction="You are a helpful customer support agent. Be concise and empathetic.",
)

# Basic generation
response = model.generate_content("Explain Vertex AI in 3 sentences.")
print(response.text)
print(f"Tokens: {response.usage_metadata.prompt_token_count} in, {response.usage_metadata.candidates_token_count} out")

# With parameters
response = model.generate_content(
    "Write a Python function to merge sorted arrays.",
    generation_config={
        "temperature": 0.2,
        "top_p": 0.8,
        "top_k": 40,
        "max_output_tokens": 2048,
        "candidate_count": 1,
    },
)
```

### Streaming

```python
# Stream response token by token
responses = model.generate_content(
    "Write a detailed explanation of microservices architecture.",
    stream=True,
)

for chunk in responses:
    print(chunk.text, end="", flush=True)
```

### Multi-Turn Chat

```python
chat = model.start_chat(history=[])

# Turn 1
response = chat.send_message("My name is Alice and I'm on the Pro plan.")
print(response.text)

# Turn 2 (remembers context)
response = chat.send_message("What plan am I on?")
print(response.text)  # "You mentioned you're on the Pro plan."

# Turn 3
response = chat.send_message("How do I cancel it?")
print(response.text)

# Access full history
for msg in chat.history:
    print(f"{msg.role}: {msg.parts[0].text[:100]}")
```

---

## Multimodal (Images, Video, Audio, PDF)

```python
import vertexai
from vertexai.generative_models import GenerativeModel, Part, Image

model = GenerativeModel("gemini-2.0-flash-001")

# ============ Image Analysis ============
# From file
image = Part.from_image(Image.load_from_file("screenshot.png"))
response = model.generate_content([image, "What error is shown in this screenshot?"])
print(response.text)

# From GCS
image = Part.from_uri("gs://my-bucket/diagram.png", mime_type="image/png")
response = model.generate_content([image, "Describe this architecture diagram."])

# Multiple images
img1 = Part.from_uri("gs://bucket/before.png", mime_type="image/png")
img2 = Part.from_uri("gs://bucket/after.png", mime_type="image/png")
response = model.generate_content([img1, img2, "What changed between these two designs?"])

# ============ Video Analysis ============
video = Part.from_uri("gs://my-bucket/meeting.mp4", mime_type="video/mp4")
response = model.generate_content([video, "Summarize the key points discussed in this video."])

# ============ Audio Analysis ============
audio = Part.from_uri("gs://my-bucket/call.mp3", mime_type="audio/mp3")
response = model.generate_content([audio, "Transcribe this audio and identify the speakers."])

# ============ PDF Analysis ============
pdf = Part.from_uri("gs://my-bucket/report.pdf", mime_type="application/pdf")
response = model.generate_content([pdf, "Extract the key financial metrics from this report."])
```

---

## Function Calling (Tool Use)

```python
from vertexai.generative_models import FunctionDeclaration, Tool

# Define tools
get_weather_func = FunctionDeclaration(
    name="get_weather",
    description="Get current weather for a city. Use when user asks about weather.",
    parameters={
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "City name"},
            "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
        },
        "required": ["city"],
    },
)

get_subscription_func = FunctionDeclaration(
    name="get_subscription",
    description="Get user's subscription details including plan, status, and renewal date.",
    parameters={
        "type": "object",
        "properties": {
            "user_id": {"type": "string", "description": "User's account ID"},
        },
        "required": ["user_id"],
    },
)

tools = Tool(function_declarations=[get_weather_func, get_subscription_func])

# Create model with tools
model = GenerativeModel("gemini-2.0-flash-001", tools=[tools])
chat = model.start_chat()

# Send message — model may call a function
response = chat.send_message("What's my subscription? My user ID is u-123.")

# Check for function call
if response.candidates[0].content.parts[0].function_call:
    fc = response.candidates[0].content.parts[0].function_call
    print(f"Function: {fc.name}, Args: {dict(fc.args)}")
    
    # Execute function
    result = get_subscription(fc.args["user_id"])
    
    # Send result back
    response = chat.send_message(
        Part.from_function_response(
            name=fc.name,
            response={"result": result},
        )
    )
    print(response.text)  # Final answer incorporating tool result
```

---

## Grounding (Reduce Hallucination)

```python
from vertexai.generative_models import GenerativeModel, Tool
from vertexai.preview.generative_models import grounding

# Ground responses in Google Search
model = GenerativeModel("gemini-2.0-flash-001")

response = model.generate_content(
    "What are the latest developments in quantum computing?",
    tools=[Tool.from_google_search_retrieval(grounding.GoogleSearchRetrieval())],
)

print(response.text)
# Response includes citations from Google Search

# Ground in your own data (Vertex AI Search datastore)
response = model.generate_content(
    "What is our refund policy?",
    tools=[Tool.from_retrieval(
        grounding.Retrieval(
            source=grounding.VertexAISearch(datastore=f"projects/my-project/locations/global/collections/default_collection/dataStores/my-datastore"),
        )
    )],
)
```

---

## Structured Output (JSON Mode)

```python
from vertexai.generative_models import GenerativeModel, GenerationConfig

model = GenerativeModel("gemini-2.0-flash-001")

# Force JSON output with schema
response = model.generate_content(
    "Extract entities from: 'John Smith ordered 3 laptops from Amazon on Jan 15'",
    generation_config=GenerationConfig(
        response_mime_type="application/json",
        response_schema={
            "type": "object",
            "properties": {
                "person": {"type": "string"},
                "quantity": {"type": "integer"},
                "product": {"type": "string"},
                "vendor": {"type": "string"},
                "date": {"type": "string"},
            },
            "required": ["person", "product"],
        },
    ),
)

import json
entities = json.loads(response.text)
print(entities)
# {"person": "John Smith", "quantity": 3, "product": "laptops", "vendor": "Amazon", "date": "Jan 15"}
```

---

## Fine-Tuning (Supervised)

```bash
# Prepare training data (JSONL in GCS)
# Format: {"messages": [{"role": "user", "content": "..."}, {"role": "model", "content": "..."}]}

# Create tuning job via gcloud
gcloud ai tuning-jobs create \
  --region=us-central1 \
  --base-model=gemini-1.5-flash-002 \
  --training-dataset-uri=gs://my-bucket/training.jsonl \
  --validation-dataset-uri=gs://my-bucket/validation.jsonl \
  --tuned-model-display-name=my-custom-model \
  --epoch-count=3 \
  --learning-rate-multiplier=1.0
```

```python
from vertexai.tuning import sft

# Python SDK
tuning_job = sft.train(
    source_model="gemini-1.5-flash-002",
    train_dataset="gs://my-bucket/training.jsonl",
    validation_dataset="gs://my-bucket/validation.jsonl",
    epochs=3,
    learning_rate_multiplier=1.0,
    tuned_model_display_name="support-agent-v1",
)

# Monitor
print(f"Job: {tuning_job.resource_name}")
tuning_job.refresh()
print(f"State: {tuning_job.state}")

# Use tuned model
tuned_model = GenerativeModel(tuning_job.tuned_model_endpoint_name)
response = tuned_model.generate_content("How do I cancel my subscription?")
```

---

## Embeddings

```python
from vertexai.language_models import TextEmbeddingModel, TextEmbeddingInput

model = TextEmbeddingModel.from_pretrained("text-embedding-005")

# Single text
embeddings = model.get_embeddings(
    [TextEmbeddingInput(text="How do I cancel?", task_type="RETRIEVAL_QUERY")]
)
vector = embeddings[0].values  # 768 dimensions
print(f"Dimension: {len(vector)}")

# Batch (up to 250 texts)
texts = [
    TextEmbeddingInput(text="Cancel subscription", task_type="RETRIEVAL_DOCUMENT"),
    TextEmbeddingInput(text="Refund policy", task_type="RETRIEVAL_DOCUMENT"),
]
embeddings = model.get_embeddings(texts)

# Task types: RETRIEVAL_QUERY, RETRIEVAL_DOCUMENT, SEMANTIC_SIMILARITY, CLASSIFICATION
```

---

## Safety Settings

```python
from vertexai.generative_models import SafetySetting, HarmCategory, HarmBlockThreshold

# Configure safety thresholds
safety_settings = [
    SafetySetting(category=HarmCategory.HARM_CATEGORY_HATE_SPEECH, threshold=HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
    SafetySetting(category=HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, threshold=HarmBlockThreshold.BLOCK_ONLY_HIGH),
    SafetySetting(category=HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, threshold=HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
    SafetySetting(category=HarmCategory.HARM_CATEGORY_HARASSMENT, threshold=HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
]

response = model.generate_content(
    "Tell me about safety protocols",
    safety_settings=safety_settings,
)

# Check safety ratings
for candidate in response.candidates:
    for rating in candidate.safety_ratings:
        print(f"{rating.category}: {rating.probability}")
```

---

## Context Caching (Cost Optimization)

```python
from vertexai.preview import caching

# Cache large context (system prompt + documents) for reuse
cached_content = caching.CachedContent.create(
    model_name="gemini-1.5-pro-002",
    system_instruction="You are an expert on our product documentation.",
    contents=[Part.from_uri("gs://bucket/large-doc.pdf", mime_type="application/pdf")],
    ttl="3600s",  # Cache for 1 hour
    display_name="product-docs-cache",
)

# Use cached content (75% cheaper for cached tokens!)
model = GenerativeModel.from_cached_content(cached_content)
response = model.generate_content("What is the refund policy?")
# Only pays for the query tokens, not the cached document tokens
```

---

## Next: [Vertex AI Search & RAG →](02_Search_RAG.md)
