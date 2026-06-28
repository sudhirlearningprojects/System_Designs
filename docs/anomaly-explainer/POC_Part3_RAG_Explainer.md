# POC Implementation - Part 3: RAG Pipeline & LLM Explanation

## 1. RAG Explainer Service (FastAPI)

```python
# explainer_service.py
import os
import json
import uuid
from datetime import datetime, timezone

import psycopg2
import redis
from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel
from openai import OpenAI
from kafka import KafkaConsumer
import threading
import httpx

app = FastAPI(title="Anomaly Explainer Service")
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
redis_client = redis.from_url(os.getenv("REDIS_URL", "redis://localhost:6379"))

EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-3-small")
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-4o")
VECTOR_SEARCH_TOP_K = int(os.getenv("VECTOR_SEARCH_TOP_K", "5"))
SLACK_WEBHOOK_URL = os.getenv("SLACK_WEBHOOK_URL", "")


# --- Models ---

class AnomalyEvent(BaseModel):
    anomaly_id: str
    timestamp: str
    service: str
    severity: str
    anomaly_type: str
    primary_metric: str
    current_value: float
    baseline_mean: float
    baseline_stddev: float
    z_score: float
    correlated_signals: list[dict] = []
    all_metrics: dict = {}
    labels: dict = {}


class Explanation(BaseModel):
    anomaly_id: str
    root_cause: str
    explanation: str
    immediate_fix: list[str]
    long_term_fix: str
    confidence: str
    similar_incident_id: str | None
    estimated_impact: str


# --- Database ---

def get_db_connection():
    return psycopg2.connect(
        host=os.getenv("POSTGRES_HOST", "localhost"),
        port=int(os.getenv("POSTGRES_PORT", "5432")),
        dbname=os.getenv("POSTGRES_DB", "anomaly_explainer"),
        user=os.getenv("POSTGRES_USER", "admin"),
        password=os.getenv("POSTGRES_PASSWORD", "admin123"),
    )


# --- Embedding ---

def get_embedding(text: str) -> list[float]:
    response = openai_client.embeddings.create(model=EMBEDDING_MODEL, input=text)
    return response.data[0].embedding


# --- Vector Search ---

def search_similar_incidents(anomaly: AnomalyEvent, top_k: int = VECTOR_SEARCH_TOP_K) -> list[dict]:
    """Search vector DB for similar past incidents."""
    # Build search query text from anomaly context
    query_text = (
        f"{anomaly.service} {anomaly.anomaly_type}. "
        f"{anomaly.primary_metric} at {anomaly.current_value} "
        f"(baseline {anomaly.baseline_mean}). "
        f"Correlated: {', '.join(s['metric'] for s in anomaly.correlated_signals)}"
    )

    query_embedding = get_embedding(query_text)
    
    conn = get_db_connection()
    cur = conn.cursor()

    # Vector similarity search with service boost
    cur.execute("""
        SELECT id, title, description, root_cause, resolution, service, severity, metrics_snapshot,
               1 - (embedding <=> %s::vector) AS similarity
        FROM incidents
        ORDER BY embedding <=> %s::vector
        LIMIT %s
    """, (str(query_embedding), str(query_embedding), top_k))

    results = []
    for row in cur.fetchall():
        results.append({
            "id": str(row[0]),
            "title": row[1],
            "description": row[2],
            "root_cause": row[3],
            "resolution": row[4],
            "service": row[5],
            "severity": row[6],
            "metrics_snapshot": row[7],
            "similarity": round(row[8], 3),
        })

    cur.close()
    conn.close()
    return results


def search_runbooks(service: str, anomaly_type: str, top_k: int = 3) -> list[dict]:
    """Search for relevant runbooks."""
    query_text = f"{service} {anomaly_type} troubleshooting runbook"
    query_embedding = get_embedding(query_text)

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT id, title, content, service,
               1 - (embedding <=> %s::vector) AS similarity
        FROM runbooks
        WHERE service = %s OR service = 'general'
        ORDER BY embedding <=> %s::vector
        LIMIT %s
    """, (str(query_embedding), service, str(query_embedding), top_k))

    results = []
    for row in cur.fetchall():
        results.append({
            "id": str(row[0]),
            "title": row[1],
            "content": row[2][:500],  # Truncate for context window
            "service": row[3],
            "similarity": round(row[4], 3),
        })

    cur.close()
    conn.close()
    return results


# --- LLM Explanation ---

PROMPT_TEMPLATE = """You are an expert SRE analyzing a production anomaly. Based on the current anomaly 
and similar past incidents, provide a root cause explanation and suggested fix.

## Current Anomaly
- Service: {service}
- Type: {anomaly_type}
- Metric: {primary_metric} = {current_value} (baseline: {baseline_mean} ± {baseline_stddev})
- Z-Score: {z_score}
- Severity: {severity}
- All Metrics: {all_metrics}
- Correlated Signals: {correlated_signals}

## Similar Past Incidents (from knowledge base)
{similar_incidents_text}

## Relevant Runbooks
{runbooks_text}

## Instructions
1. Identify the most likely root cause by pattern-matching with past incidents
2. Explain WHY this is happening (not just what)
3. Provide immediate mitigation steps (ordered by priority)
4. Suggest long-term fix
5. Rate confidence: HIGH (strong match), MEDIUM (partial match), LOW (speculative)

Respond ONLY in this JSON format:
{{
  "root_cause": "One sentence root cause",
  "explanation": "2-3 sentence detailed explanation of why this is happening",
  "immediate_fix": ["step 1", "step 2", "step 3"],
  "long_term_fix": "Long-term prevention strategy",
  "confidence": "HIGH|MEDIUM|LOW",
  "similar_incident_id": "ID of most relevant past incident or null",
  "estimated_impact": "Estimated user/business impact"
}}"""


def generate_explanation(anomaly: AnomalyEvent, similar_incidents: list[dict], runbooks: list[dict]) -> Explanation:
    """Generate root cause explanation using LLM."""
    
    # Format similar incidents for prompt
    incidents_text = ""
    for i, inc in enumerate(similar_incidents, 1):
        incidents_text += (
            f"\n### Incident {i} (similarity: {inc['similarity']})\n"
            f"- Title: {inc['title']}\n"
            f"- Service: {inc['service']}\n"
            f"- Root Cause: {inc['root_cause']}\n"
            f"- Resolution: {inc['resolution']}\n"
            f"- Metrics: {inc['metrics_snapshot']}\n"
        )

    # Format runbooks
    runbooks_text = ""
    for rb in runbooks:
        runbooks_text += f"\n### {rb['title']}\n{rb['content']}\n"

    if not runbooks_text:
        runbooks_text = "No specific runbooks found for this scenario."

    prompt = PROMPT_TEMPLATE.format(
        service=anomaly.service,
        anomaly_type=anomaly.anomaly_type,
        primary_metric=anomaly.primary_metric,
        current_value=anomaly.current_value,
        baseline_mean=anomaly.baseline_mean,
        baseline_stddev=anomaly.baseline_stddev,
        z_score=anomaly.z_score,
        severity=anomaly.severity,
        all_metrics=json.dumps(anomaly.all_metrics),
        correlated_signals=json.dumps(anomaly.correlated_signals),
        similar_incidents_text=incidents_text or "No similar past incidents found.",
        runbooks_text=runbooks_text,
    )

    response = openai_client.chat.completions.create(
        model=LLM_MODEL,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.2,  # Low temperature for factual responses
        response_format={"type": "json_object"},
    )

    result = json.loads(response.choices[0].message.content)

    return Explanation(
        anomaly_id=anomaly.anomaly_id,
        root_cause=result["root_cause"],
        explanation=result["explanation"],
        immediate_fix=result["immediate_fix"],
        long_term_fix=result["long_term_fix"],
        confidence=result["confidence"],
        similar_incident_id=result.get("similar_incident_id"),
        estimated_impact=result.get("estimated_impact", "Unknown"),
    )


# --- Notification ---

def send_slack_notification(anomaly: AnomalyEvent, explanation: Explanation):
    """Send rich Slack notification with explanation."""
    if not SLACK_WEBHOOK_URL:
        print(f"[Slack] Would send: {explanation.root_cause}")
        return

    severity_emoji = {"CRITICAL": "🔴", "HIGH": "🟠", "MEDIUM": "🟡", "LOW": "🟢"}
    confidence_emoji = {"HIGH": "✅", "MEDIUM": "⚠️", "LOW": "❓"}

    blocks = [
        {
            "type": "header",
            "text": {"type": "plain_text", "text": f"{severity_emoji.get(anomaly.severity, '⚪')} Anomaly Detected: {anomaly.service}"}
        },
        {
            "type": "section",
            "fields": [
                {"type": "mrkdwn", "text": f"*Type:* {anomaly.anomaly_type}"},
                {"type": "mrkdwn", "text": f"*Severity:* {anomaly.severity}"},
                {"type": "mrkdwn", "text": f"*Metric:* {anomaly.primary_metric} = {anomaly.current_value}"},
                {"type": "mrkdwn", "text": f"*Z-Score:* {anomaly.z_score}"},
            ]
        },
        {"type": "divider"},
        {
            "type": "section",
            "text": {"type": "mrkdwn", "text": f"*🧠 Root Cause* {confidence_emoji.get(explanation.confidence, '')}\n{explanation.root_cause}"}
        },
        {
            "type": "section",
            "text": {"type": "mrkdwn", "text": f"*📝 Explanation*\n{explanation.explanation}"}
        },
        {
            "type": "section",
            "text": {"type": "mrkdwn", "text": f"*🔧 Immediate Fix*\n" + "\n".join(f"• {step}" for step in explanation.immediate_fix)}
        },
        {
            "type": "section",
            "text": {"type": "mrkdwn", "text": f"*🛡️ Long-term Fix*\n{explanation.long_term_fix}"}
        },
        {
            "type": "actions",
            "elements": [
                {"type": "button", "text": {"type": "plain_text", "text": "👍 Helpful"}, "action_id": "feedback_helpful", "value": anomaly.anomaly_id},
                {"type": "button", "text": {"type": "plain_text", "text": "👎 Not Helpful"}, "action_id": "feedback_not_helpful", "value": anomaly.anomaly_id},
            ]
        }
    ]

    httpx.post(SLACK_WEBHOOK_URL, json={"blocks": blocks})


# --- Storage ---

def store_explanation(anomaly: AnomalyEvent, explanation: Explanation, similar_ids: list[str]):
    """Store explanation for feedback loop and analytics."""
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        INSERT INTO anomaly_explanations (anomaly_id, service, anomaly_type, explanation, similar_incidents)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (anomaly_id) DO NOTHING
    """, (
        anomaly.anomaly_id, anomaly.service, anomaly.anomaly_type,
        json.dumps(explanation.dict()), similar_ids
    ))

    conn.commit()
    cur.close()
    conn.close()


# --- Main Pipeline ---

def process_anomaly(anomaly: AnomalyEvent) -> Explanation:
    """Full RAG pipeline: search → assemble context → generate explanation → notify."""
    
    # 1. Check dedup (don't explain same anomaly twice within 5 min)
    dedup_key = f"anomaly:{anomaly.service}:{anomaly.anomaly_type}"
    if redis_client.get(dedup_key):
        print(f"[Dedup] Skipping duplicate anomaly for {anomaly.service}")
        return None
    redis_client.setex(dedup_key, 300, "1")  # 5 min TTL

    # 2. Search similar incidents
    similar_incidents = search_similar_incidents(anomaly)
    print(f"[RAG] Found {len(similar_incidents)} similar incidents")

    # 3. Search runbooks
    runbooks = search_runbooks(anomaly.service, anomaly.anomaly_type)
    print(f"[RAG] Found {len(runbooks)} relevant runbooks")

    # 4. Generate explanation
    explanation = generate_explanation(anomaly, similar_incidents, runbooks)
    print(f"[LLM] Explanation generated (confidence: {explanation.confidence})")

    # 5. Notify
    send_slack_notification(anomaly, explanation)

    # 6. Store for feedback loop
    similar_ids = [inc["id"] for inc in similar_incidents[:3]]
    store_explanation(anomaly, explanation, similar_ids)

    return explanation


# --- API Endpoints ---

@app.post("/api/v1/explain")
async def explain_anomaly(anomaly: AnomalyEvent, background_tasks: BackgroundTasks):
    """Manually trigger explanation for an anomaly."""
    explanation = process_anomaly(anomaly)
    if explanation:
        return {"status": "explained", "explanation": explanation.dict()}
    return {"status": "deduplicated", "message": "Recent anomaly already explained"}


@app.post("/api/v1/feedback/{anomaly_id}")
async def submit_feedback(anomaly_id: str, feedback: str):
    """Submit feedback on explanation quality."""
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute(
        "UPDATE anomaly_explanations SET feedback = %s WHERE anomaly_id = %s",
        (feedback, anomaly_id)
    )
    conn.commit()
    cur.close()
    conn.close()
    return {"status": "feedback_recorded"}


@app.get("/api/v1/health")
async def health():
    return {"status": "healthy", "timestamp": datetime.now(timezone.utc).isoformat()}


# --- Kafka Consumer (Background Thread) ---

def kafka_consumer_loop():
    """Background thread consuming anomalies from Kafka."""
    consumer = KafkaConsumer(
        "anomalies.detected",
        bootstrap_servers=[os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")],
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        group_id="anomaly-explainer",
        auto_offset_reset="latest",
    )

    print("📡 Explainer Service listening on 'anomalies.detected' topic...")

    for message in consumer:
        try:
            anomaly = AnomalyEvent(**message.value)
            process_anomaly(anomaly)
        except Exception as e:
            print(f"[ERROR] Failed to process anomaly: {e}")


@app.on_event("startup")
async def startup():
    thread = threading.Thread(target=kafka_consumer_loop, daemon=True)
    thread.start()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8200)
```

## 2. Running the Complete Pipeline

```bash
#!/bin/bash
# run_poc.sh

echo "🚀 Starting Real-Time Anomaly Explainer POC"
echo "============================================="

# Step 1: Start infrastructure
echo "\n📦 Starting infrastructure..."
docker-compose up -d
sleep 10

# Step 2: Create Kafka topics
echo "\n📝 Creating Kafka topics..."
docker exec -it $(docker ps -q -f name=kafka) kafka-topics \
    --create --topic metrics.raw --partitions 6 --replication-factor 1 \
    --bootstrap-server localhost:9092 2>/dev/null

docker exec -it $(docker ps -q -f name=kafka) kafka-topics \
    --create --topic anomalies.detected --partitions 3 --replication-factor 1 \
    --bootstrap-server localhost:9092 2>/dev/null

# Step 3: Seed knowledge base
echo "\n🌱 Seeding knowledge base..."
python seed_knowledge_base.py

# Step 4: Start anomaly detector
echo "\n🔍 Starting anomaly detector..."
python flink_anomaly_detector.py &
DETECTOR_PID=$!

# Step 5: Start explainer service
echo "\n🧠 Starting explainer service..."
python explainer_service.py &
EXPLAINER_PID=$!

sleep 3

# Step 6: Start metrics producer (anomaly after 60s)
echo "\n📊 Starting metrics producer (anomaly in 60s)..."
python metrics_producer.py --anomaly-after 60 --service payment-service --type cpu_spike &
PRODUCER_PID=$!

echo "\n✅ All services running!"
echo "   Detector PID: $DETECTOR_PID"
echo "   Explainer PID: $EXPLAINER_PID"
echo "   Producer PID: $PRODUCER_PID"
echo "\n⏳ Anomaly will be injected in ~60 seconds..."
echo "   Watch for 🚨 ANOMALY DETECTED and 🧠 Explanation"

wait
```

## 3. Sample Output

When the pipeline detects and explains an anomaly:

```
📊 [Producer] Starting metrics stream. Anomaly will trigger after 60s
🔍 Anomaly Detector running. Waiting for metrics...
📡 Explainer Service listening on 'anomalies.detected' topic...

... (60 seconds of normal metrics) ...

🚨 ANOMALY DETECTED!
   Service: payment-service
   Type: CPU_SPIKE
   Severity: HIGH
   Z-Score: 5.9
   Value: 95.2 (baseline: 45.0 ± 8.5)
   Correlated: ['latency_p99_ms', 'error_rate']

[RAG] Found 5 similar incidents
[RAG] Found 2 relevant runbooks
[LLM] Explanation generated (confidence: HIGH)

┌─────────────────────────────────────────────────────────┐
│ 🧠 ROOT CAUSE EXPLANATION                               │
├─────────────────────────────────────────────────────────┤
│ Root Cause: Database connection pool exhaustion         │
│                                                         │
│ Explanation: The CPU spike on payment-service closely   │
│ matches INC-2847 where unclosed DB connections during   │
│ batch processing caused pool exhaustion. The correlated │
│ latency spike (z=4.1) and error rate increase (z=3.2)  │
│ confirm connection starvation pattern.                  │
│                                                         │
│ Immediate Fix:                                          │
│   1. Restart payment-service pods to release connections│
│   2. Check if batch reconciliation job is running       │
│   3. Monitor connection pool metrics in Grafana         │
│                                                         │
│ Long-term: Add try-with-resources, connection pool      │
│ monitoring at 80% threshold, circuit breaker.           │
│                                                         │
│ Confidence: HIGH                                        │
│ Similar to: INC-2847 (March 3rd)                       │
└─────────────────────────────────────────────────────────┘
```
