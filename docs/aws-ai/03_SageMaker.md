# 3. Amazon SageMaker

## Overview

SageMaker is AWS's ML platform for training, fine-tuning, and deploying models at scale.

---

## JumpStart (Pre-trained Models)

```python
from sagemaker.jumpstart.model import JumpStartModel

# Deploy Llama 3 from JumpStart
model = JumpStartModel(
    model_id="meta-textgeneration-llama-3-1-8b-instruct",
    instance_type="ml.g5.2xlarge",
)
predictor = model.deploy(initial_instance_count=1)

# Invoke
response = predictor.predict({
    "inputs": "Explain quantum computing in simple terms.",
    "parameters": {"max_new_tokens": 256, "temperature": 0.7},
})
print(response[0]["generated_text"])

# Cleanup
predictor.delete_endpoint()
```

## Fine-Tuning on SageMaker

```python
from sagemaker.jumpstart.estimator import JumpStartEstimator

# Fine-tune Llama 3 with your data
estimator = JumpStartEstimator(
    model_id="meta-textgeneration-llama-3-1-8b-instruct",
    instance_type="ml.g5.12xlarge",
    instance_count=1,
    hyperparameters={
        "epoch": 3,
        "learning_rate": 2e-4,
        "lora_r": 16,
        "lora_alpha": 32,
        "per_device_train_batch_size": 4,
    },
)

# Training data in S3 (JSONL format)
estimator.fit({"training": "s3://my-bucket/training-data/"})

# Deploy fine-tuned model
predictor = estimator.deploy(instance_type="ml.g5.2xlarge", initial_instance_count=1)
```

## Real-Time Endpoints

```python
import sagemaker
from sagemaker.huggingface import HuggingFaceModel

# Deploy any HuggingFace model
model = HuggingFaceModel(
    model_data="s3://my-bucket/model.tar.gz",
    role=sagemaker.get_execution_role(),
    transformers_version="4.37",
    pytorch_version="2.1",
    py_version="py310",
    env={"HF_MODEL_ID": "sentence-transformers/all-MiniLM-L6-v2"},
)

predictor = model.deploy(
    instance_type="ml.g5.xlarge",
    initial_instance_count=2,
    endpoint_name="embedding-endpoint",
)

# Auto-scaling
client = boto3.client("application-autoscaling")
client.register_scalable_target(
    ServiceNamespace="sagemaker",
    ResourceId="endpoint/embedding-endpoint/variant/AllTraffic",
    ScalableDimension="sagemaker:variant:DesiredInstanceCount",
    MinCapacity=1, MaxCapacity=10,
)
client.put_scaling_policy(
    PolicyName="scale-on-invocations",
    ServiceNamespace="sagemaker",
    ResourceId="endpoint/embedding-endpoint/variant/AllTraffic",
    ScalableDimension="sagemaker:variant:DesiredInstanceCount",
    PolicyType="TargetTrackingScaling",
    TargetTrackingScalingPolicyConfiguration={
        "TargetValue": 1000,  # Scale when >1000 invocations/min
        "PredefinedMetricSpecification": {"PredefinedMetricType": "SageMakerVariantInvocationsPerInstance"},
    },
)
```

---

## Next: [Amazon Kendra & OpenSearch →](04_Kendra_OpenSearch.md)
