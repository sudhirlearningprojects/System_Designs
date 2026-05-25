# 7. Safety & Responsible AI

## Gemini Safety Filters

```python
from vertexai.generative_models import GenerativeModel, SafetySetting, HarmCategory, HarmBlockThreshold

# Configure safety thresholds per category
safety_settings = [
    SafetySetting(category=HarmCategory.HARM_CATEGORY_HATE_SPEECH, threshold=HarmBlockThreshold.BLOCK_LOW_AND_ABOVE),
    SafetySetting(category=HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, threshold=HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
    SafetySetting(category=HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, threshold=HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
    SafetySetting(category=HarmCategory.HARM_CATEGORY_HARASSMENT, threshold=HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE),
]

model = GenerativeModel("gemini-2.0-flash-001")
response = model.generate_content("Tell me about safety", safety_settings=safety_settings)

# Check if blocked
if response.candidates[0].finish_reason.name == "SAFETY":
    print("Response blocked by safety filter")
    for rating in response.candidates[0].safety_ratings:
        if rating.blocked:
            print(f"  Blocked by: {rating.category} (probability: {rating.probability})")
```

## Grounding Check (Hallucination Detection)

```python
from vertexai.evaluation import EvalTask

# Evaluate groundedness of responses
eval_task = EvalTask(
    dataset=[
        {
            "context": "Our refund policy allows returns within 14 days.",
            "response": "You can get a refund within 30 days of purchase.",  # Hallucination!
        }
    ],
    metrics=["groundedness"],
)

results = eval_task.evaluate(model=GenerativeModel("gemini-2.0-flash-001"))
print(f"Groundedness: {results.summary_metrics['groundedness/mean']:.2f}")
# Low score = hallucination detected
```

## Responsible AI Toolkit

```python
from google.cloud import aiplatform

# Model evaluation with fairness metrics
eval_job = aiplatform.ModelEvaluation.create(
    model=model_resource,
    evaluation_config={
        "metrics": ["accuracy", "f1_score"],
        "slicing_specs": [
            {"feature_name": "gender"},   # Check performance across genders
            {"feature_name": "age_group"},  # Check across age groups
        ],
    },
    test_dataset="gs://bucket/test_data.csv",
)

# View fairness metrics in Vertex AI console
# Identifies if model performs differently for different demographic groups
```

---

## Next: [Production Patterns →](08_Production_Patterns.md)
