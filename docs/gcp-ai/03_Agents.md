# 3. Vertex AI Agents (Agent Builder)

## Overview

Vertex AI Agent Builder creates conversational AI agents with built-in RAG, tool use, and multi-turn conversation — all managed by Google Cloud.

---

## Create Agent (Console + API)

```python
from google.cloud import dialogflowcx_v3 as dialogflow

# Create agent
agents_client = dialogflow.AgentsClient()

agent = agents_client.create_agent(
    parent=f"projects/my-project/locations/us-central1",
    agent=dialogflow.Agent(
        display_name="Support Agent",
        default_language_code="en",
        time_zone="America/New_York",
        description="Customer support agent with access to documentation and account tools.",
        gen_app_builder_settings=dialogflow.Agent.GenAppBuilderSettings(
            engine=f"projects/my-project/locations/global/collections/default_collection/engines/my-search-app",
        ),
    ),
)
```

### Agent with Tools (Playbooks)

```yaml
# Agent playbook (defines behavior)
name: "Customer Support Agent"
goal: "Help customers with billing, technical issues, and account management."
instructions:
  - "Always greet the customer warmly"
  - "Search documentation before answering technical questions"
  - "Verify user identity before making account changes"
  - "If you can't resolve the issue, offer to escalate to a human agent"

tools:
  - name: "search_docs"
    description: "Search product documentation for answers"
    type: "DATA_STORE"
    data_store: "projects/my-project/locations/global/dataStores/my-docs-store"
  
  - name: "get_account"
    description: "Get user's account and subscription details"
    type: "OPEN_API"
    open_api_spec:
      url: "https://api.myapp.com/openapi.yaml"
  
  - name: "create_ticket"
    description: "Create a support ticket for escalation"
    type: "FUNCTION"
    function:
      name: "create_support_ticket"
      parameters:
        type: "object"
        properties:
          summary: {type: "string", description: "Issue summary"}
          priority: {type: "string", enum: ["low", "medium", "high"]}
        required: ["summary"]
```

### Invoke Agent

```python
from google.cloud import dialogflowcx_v3 as dialogflow

session_client = dialogflow.SessionsClient()

session_path = session_client.session_path(
    project="my-project",
    location="us-central1",
    agent="agent-id",
    session="session-123",
)

# Send message
response = session_client.detect_intent(
    request=dialogflow.DetectIntentRequest(
        session=session_path,
        query_input=dialogflow.QueryInput(
            text=dialogflow.TextInput(text="I want to cancel my subscription"),
            language_code="en",
        ),
    )
)

# Get agent response
for msg in response.query_result.response_messages:
    if msg.text:
        print(f"Agent: {msg.text.text[0]}")

# Multi-turn (same session maintains context)
response2 = session_client.detect_intent(
    request=dialogflow.DetectIntentRequest(
        session=session_path,
        query_input=dialogflow.QueryInput(
            text=dialogflow.TextInput(text="Yes, please proceed"),
            language_code="en",
        ),
    )
)
```

---

## Vertex AI Extensions (Custom Tools)

```python
from vertexai.preview import extensions

# Create extension (connects Gemini to external APIs)
extension = extensions.Extension.create(
    display_name="Account Management API",
    manifest={
        "name": "account_api",
        "description": "Manage user accounts and subscriptions",
        "api_spec": {
            "open_api_gcs_uri": "gs://my-bucket/openapi.yaml",
        },
        "auth_config": {
            "auth_type": "OAUTH",
            "oauth_config": {"client_id": "...", "client_secret": "..."},
        },
    },
)

# Use extension with Gemini
model = GenerativeModel("gemini-2.0-flash-001", tools=[extension])
response = model.generate_content("Check the subscription status for user u-123")
```

---

## Evaluation

```python
from vertexai.evaluation import EvalTask, MetricPromptTemplate

# Evaluate agent quality
eval_task = EvalTask(
    dataset="gs://my-bucket/eval_dataset.jsonl",
    metrics=[
        "fluency",
        "coherence", 
        "groundedness",
        "fulfillment",  # Did it answer the question?
        "safety",
    ],
    experiment="agent-v2-eval",
)

results = eval_task.evaluate(
    model=GenerativeModel("gemini-2.0-flash-001"),
    prompt_template="Context: {context}\nQuestion: {query}\nAnswer:",
)

print(f"Groundedness: {results.summary_metrics['groundedness/mean']:.2f}")
print(f"Fulfillment: {results.summary_metrics['fulfillment/mean']:.2f}")
```

---

## Next: [Document AI & Vision →](04_Document_Vision.md)
