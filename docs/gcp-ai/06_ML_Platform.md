# 6. Vertex AI ML Platform

## Training Custom Models

```python
from google.cloud import aiplatform

aiplatform.init(project="my-project", location="us-central1")

# Custom training job
job = aiplatform.CustomTrainingJob(
    display_name="my-model-training",
    script_path="train.py",
    container_uri="us-docker.pkg.dev/vertex-ai/training/pytorch-gpu.2-1:latest",
    requirements=["transformers", "peft", "bitsandbytes"],
)

model = job.run(
    replica_count=1,
    machine_type="n1-standard-8",
    accelerator_type="NVIDIA_TESLA_A100",
    accelerator_count=2,
    args=["--epochs=3", "--lr=2e-4", "--model=meta-llama/Llama-3-8B"],
    base_output_dir="gs://my-bucket/output/",
)
```

## Deploy to Endpoint

```python
# Deploy model for online prediction
endpoint = aiplatform.Endpoint.create(display_name="my-model-endpoint")

endpoint.deploy(
    model=model,
    machine_type="n1-standard-4",
    accelerator_type="NVIDIA_TESLA_T4",
    accelerator_count=1,
    min_replica_count=1,
    max_replica_count=5,
    traffic_percentage=100,
)

# Predict
prediction = endpoint.predict(instances=[{"text": "Hello world"}])
print(prediction.predictions)

# Auto-scaling (based on GPU utilization)
# Configured via min/max replica count — Vertex AI handles scaling
```

## Vertex AI Pipelines (MLOps)

```python
from kfp import dsl
from kfp.dsl import component
from google.cloud.aiplatform import pipeline_jobs

@component(base_image="python:3.11", packages_to_install=["pandas", "scikit-learn"])
def preprocess_data(input_path: str, output_path: str):
    import pandas as pd
    df = pd.read_csv(input_path)
    # ... preprocessing ...
    df.to_csv(output_path, index=False)

@component(base_image="us-docker.pkg.dev/vertex-ai/training/pytorch-gpu.2-1:latest")
def train_model(data_path: str, model_path: str, epochs: int = 3):
    # Training logic
    pass

@component
def evaluate_model(model_path: str) -> float:
    # Evaluation logic
    return 0.92

@dsl.pipeline(name="ml-pipeline")
def my_pipeline(input_data: str):
    preprocess_task = preprocess_data(input_path=input_data, output_path="gs://bucket/processed/")
    train_task = train_model(data_path=preprocess_task.output, model_path="gs://bucket/model/")
    eval_task = evaluate_model(model_path=train_task.output)

# Submit pipeline
job = pipeline_jobs.PipelineJob(
    display_name="training-pipeline",
    template_path="pipeline.json",
    parameter_values={"input_data": "gs://bucket/raw/data.csv"},
)
job.run(service_account="vertex-ai-sa@my-project.iam.gserviceaccount.com")
```

---

## Next: [Safety & Responsible AI →](07_Safety.md)
