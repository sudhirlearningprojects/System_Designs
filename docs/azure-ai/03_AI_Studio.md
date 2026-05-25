# 3. Azure AI Studio

## Overview

Azure AI Studio is the unified portal for building AI applications — model catalog, prompt flow, evaluation, and deployment in one place.

---

## Model Catalog

```bash
# Deploy open-source models (Llama 3, Mistral, Phi-3) as serverless endpoints
# Azure Portal → AI Studio → Model Catalog → Deploy

# Or via CLI
az ml serverless-endpoint create \
  --name llama3-endpoint \
  --model-id azureml://registries/azureml-meta/models/Meta-Llama-3.1-8B-Instruct

# Use like any other endpoint
from azure.ai.inference import ChatCompletionsClient
from azure.core.credentials import AzureKeyCredential

client = ChatCompletionsClient(
    endpoint="https://llama3-endpoint.eastus.models.ai.azure.com",
    credential=AzureKeyCredential("your-key"),
)

response = client.complete(
    messages=[{"role": "user", "content": "Hello!"}],
    model="Meta-Llama-3.1-8B-Instruct",
    temperature=0.7,
    max_tokens=500,
)
print(response.choices[0].message.content)
```

---

## Prompt Flow (Visual Agent Builder)

```yaml
# flow.dag.yaml — Define agent as a DAG
inputs:
  query:
    type: string

outputs:
  answer:
    type: string
    reference: ${generate.output}

nodes:
  - name: classify
    type: llm
    source:
      type: code
      path: classify.py
    inputs:
      query: ${inputs.query}

  - name: retrieve
    type: python
    source:
      type: code
      path: retrieve.py
    inputs:
      query: ${inputs.query}
      intent: ${classify.output}

  - name: generate
    type: llm
    source:
      type: code
      path: generate.py
    inputs:
      query: ${inputs.query}
      context: ${retrieve.output}
```

```python
# classify.py (Prompt Flow node)
from promptflow import tool
from promptflow.connections import AzureOpenAIConnection

@tool
def classify(query: str, connection: AzureOpenAIConnection) -> str:
    # Uses Azure OpenAI to classify intent
    from openai import AzureOpenAI
    client = AzureOpenAI(
        azure_endpoint=connection.api_base,
        api_key=connection.api_key,
        api_version="2024-10-21",
    )
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "Classify: billing, technical, general"},
            {"role": "user", "content": query},
        ],
    )
    return response.choices[0].message.content
```

```bash
# Run locally
pf flow test --flow . --inputs query="How do I cancel?"

# Deploy to Azure
pf flow build --source . --output ./deploy --format docker
az ml online-endpoint create --name my-agent-endpoint -f endpoint.yaml
az ml online-deployment create --name blue -f deployment.yaml
```

---

## Evaluation in AI Studio

```python
# pip install azure-ai-evaluation

from azure.ai.evaluation import evaluate, RelevanceEvaluator, GroundednessEvaluator, FluencyEvaluator

# Evaluate your AI application
results = evaluate(
    data="test_dataset.jsonl",  # {query, context, response, ground_truth}
    evaluators={
        "relevance": RelevanceEvaluator(model_config=azure_openai_config),
        "groundedness": GroundednessEvaluator(model_config=azure_openai_config),
        "fluency": FluencyEvaluator(model_config=azure_openai_config),
    },
)

print(f"Relevance: {results['relevance.score']:.2f}")
print(f"Groundedness: {results['groundedness.score']:.2f}")
print(f"Fluency: {results['fluency.score']:.2f}")
```

---

## Next: [Azure Document Intelligence →](04_Document_Intelligence.md)
