# 6. Azure Machine Learning

## Overview

Azure ML is the enterprise ML platform for training, deploying, and managing models at scale — from custom models to LLM fine-tuning.

---

## Setup

```bash
# Create workspace
az ml workspace create --name my-ml-workspace -g rg-ai --location eastus

# Create compute cluster (for training)
az ml compute create --name gpu-cluster --type AmlCompute \
  --size Standard_NC24ads_A100_v4 --min-instances 0 --max-instances 4 \
  --workspace-name my-ml-workspace -g rg-ai

# Create managed online endpoint (for serving)
az ml online-endpoint create --name my-model-endpoint \
  --workspace-name my-ml-workspace -g rg-ai
```

---

## Training with Azure ML SDK v2

```python
# pip install azure-ai-ml

from azure.ai.ml import MLClient, command, Input, Output
from azure.ai.ml.entities import Environment, AmlCompute
from azure.identity import DefaultAzureCredential

# Connect to workspace
ml_client = MLClient(
    DefaultAzureCredential(),
    subscription_id="...",
    resource_group_name="rg-ai",
    workspace_name="my-ml-workspace",
)

# Define training job
training_job = command(
    code="./src",
    command="python train.py --epochs ${{inputs.epochs}} --lr ${{inputs.lr}} --data ${{inputs.data}}",
    inputs={
        "epochs": 10,
        "lr": 0.001,
        "data": Input(type="uri_folder", path="azureml://datastores/training_data/paths/dataset/"),
    },
    outputs={
        "model": Output(type="uri_folder", path="azureml://datastores/models/paths/output/"),
    },
    environment="azureml:AzureML-pytorch-2.1-cuda12@latest",
    compute="gpu-cluster",
    instance_count=1,
    distribution={"type": "PyTorch", "process_count_per_instance": 4},  # Multi-GPU
)

# Submit
returned_job = ml_client.jobs.create_or_update(training_job)
print(f"Job URL: {returned_job.studio_url}")

# Monitor
ml_client.jobs.stream(returned_job.name)
```

---

## Deploy Model as Endpoint

```python
from azure.ai.ml.entities import (
    ManagedOnlineEndpoint, ManagedOnlineDeployment, Model, CodeConfiguration,
)

# Register model
model = ml_client.models.create_or_update(
    Model(name="my-classifier", path="./model/", type="custom_model")
)

# Create endpoint
endpoint = ManagedOnlineEndpoint(name="classifier-endpoint", auth_mode="key")
ml_client.online_endpoints.begin_create_or_update(endpoint).result()

# Create deployment
deployment = ManagedOnlineDeployment(
    name="blue",
    endpoint_name="classifier-endpoint",
    model=model,
    code_configuration=CodeConfiguration(code="./src", scoring_script="score.py"),
    environment="azureml:AzureML-pytorch-2.1-cuda12@latest",
    instance_type="Standard_DS3_v2",
    instance_count=2,
)
ml_client.online_deployments.begin_create_or_update(deployment).result()

# Test
result = ml_client.online_endpoints.invoke(
    endpoint_name="classifier-endpoint",
    request_file="sample_request.json",
)
```

### Scoring Script

```python
# score.py
import torch
import json
import os

def init():
    global model
    model_path = os.path.join(os.environ["AZUREML_MODEL_DIR"], "model.pt")
    model = torch.jit.load(model_path)
    model.eval()

def run(raw_data):
    data = json.loads(raw_data)
    input_tensor = torch.tensor(data["input"])
    with torch.no_grad():
        prediction = model(input_tensor)
    return {"prediction": prediction.tolist()}
```

---

## Responsible AI Dashboard

```python
from azure.ai.ml.entities import ResponsibleAiInsights

# Create Responsible AI dashboard (fairness, explainability, error analysis)
rai_insights = ResponsibleAiInsights(
    name="model-rai-analysis",
    model=model,
    test_data=Input(path="azureml://datastores/data/paths/test.csv"),
    components=[
        "error_analysis",      # Find failure patterns
        "explanation",         # Feature importance (SHAP)
        "fairness",           # Demographic parity
        "counterfactual",     # What-if analysis
    ],
)

ml_client.insights.begin_create_or_update(rai_insights).result()
# View in Azure ML Studio → Models → Responsible AI tab
```

---

## MLflow Integration

```python
import mlflow

# Azure ML workspace as MLflow tracking server (automatic)
mlflow.set_tracking_uri(ml_client.tracking_uri)

with mlflow.start_run():
    mlflow.log_param("model", "gpt-4o-mini")
    mlflow.log_param("temperature", 0.7)
    mlflow.log_metric("accuracy", 0.92)
    mlflow.log_metric("latency_p95", 2.3)
    mlflow.log_metric("cost_per_query", 0.008)
    
    # Log model
    mlflow.pytorch.log_model(model, "model")
```

---

## Next: [Azure AI Content Safety →](07_Content_Safety.md)
