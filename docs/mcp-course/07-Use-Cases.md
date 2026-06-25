# Module 7: Real-World Use Cases

## Categories of MCP Servers

| Category | Examples | Complexity |
|----------|----------|-----------|
| Data Access | Database, file system, API wrappers | Low-Medium |
| DevOps | Kubernetes, Docker, CI/CD, monitoring | Medium |
| Productivity | Slack, email, calendar, project management | Medium |
| Development | Git, code search, testing, deployment | Medium |
| AI/ML | RAG, vector search, model management | High |
| Cloud | AWS, GCP, Azure resource management | High |

---

## 1. Database Server (PostgreSQL)

```python
from mcp.server.fastmcp import FastMCP
import asyncpg

mcp = FastMCP("postgres-server")
pool = None

@mcp.tool()
async def query(sql: str) -> str:
    """Execute read-only SQL. Only SELECT allowed."""
    if not sql.strip().upper().startswith("SELECT"):
        raise ValueError("Only SELECT queries permitted")
    rows = await pool.fetch(sql)
    return json.dumps([dict(r) for r in rows], default=str)

@mcp.tool()
async def list_tables() -> str:
    """List all tables with row counts."""
    rows = await pool.fetch("""
        SELECT tablename, n_tup_ins as row_count 
        FROM pg_stat_user_tables ORDER BY tablename
    """)
    return json.dumps([dict(r) for r in rows])

@mcp.resource("postgres://schema")
async def schema() -> str:
    """Database schema definition."""
    rows = await pool.fetch("""
        SELECT table_name, column_name, data_type 
        FROM information_schema.columns 
        WHERE table_schema='public' ORDER BY table_name
    """)
    return json.dumps([dict(r) for r in rows])
```

---

## 2. AWS Cloud Management

```python
import boto3
mcp = FastMCP("aws-server")

@mcp.tool()
def list_ec2_instances(region: str = "us-east-1") -> str:
    """List EC2 instances with status."""
    ec2 = boto3.client("ec2", region_name=region)
    response = ec2.describe_instances()
    instances = []
    for r in response["Reservations"]:
        for i in r["Instances"]:
            instances.append({
                "id": i["InstanceId"],
                "type": i["InstanceType"],
                "state": i["State"]["Name"],
                "name": next((t["Value"] for t in i.get("Tags", []) if t["Key"] == "Name"), ""),
            })
    return json.dumps(instances, indent=2)

@mcp.tool()
def get_cloudwatch_metrics(namespace: str, metric: str, period: int = 300) -> str:
    """Get CloudWatch metrics for the last hour."""
    cw = boto3.client("cloudwatch")
    response = cw.get_metric_statistics(
        Namespace=namespace, MetricName=metric,
        StartTime=datetime.utcnow() - timedelta(hours=1),
        EndTime=datetime.utcnow(),
        Period=period, Statistics=["Average"],
    )
    return json.dumps(response["Datapoints"], default=str)

@mcp.tool()
def query_logs(log_group: str, query: str, hours: int = 1) -> str:
    """Query CloudWatch Logs Insights."""
    logs = boto3.client("logs")
    response = logs.start_query(
        logGroupName=log_group, queryString=query,
        startTime=int((datetime.utcnow() - timedelta(hours=hours)).timestamp()),
        endTime=int(datetime.utcnow().timestamp()),
    )
    # Wait for results
    import time
    while True:
        result = logs.get_query_results(queryId=response["queryId"])
        if result["status"] == "Complete":
            return json.dumps(result["results"][:50])
        time.sleep(1)
```

---

## 3. Slack Integration

```python
from slack_sdk.web.async_client import AsyncWebClient
mcp = FastMCP("slack-server")
slack = AsyncWebClient(token=os.environ["SLACK_TOKEN"])

@mcp.tool()
async def send_message(channel: str, text: str) -> str:
    """Send a message to a Slack channel."""
    result = await slack.chat_postMessage(channel=channel, text=text)
    return f"Message sent to {channel}: ts={result['ts']}"

@mcp.tool()
async def search_messages(query: str, count: int = 10) -> str:
    """Search Slack messages."""
    result = await slack.search_messages(query=query, count=count)
    messages = [{"channel": m["channel"]["name"], "text": m["text"], "user": m["username"]}
                for m in result["messages"]["matches"]]
    return json.dumps(messages, indent=2)

@mcp.tool()
async def get_channel_history(channel: str, limit: int = 20) -> str:
    """Get recent messages from a channel."""
    result = await slack.conversations_history(channel=channel, limit=limit)
    return json.dumps([{"user": m.get("user"), "text": m["text"]} for m in result["messages"]])
```

---

## 4. RAG / Vector Search Server

```python
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma

mcp = FastMCP("rag-server")
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma(persist_directory="./db", embedding_function=embeddings)

@mcp.tool()
def search_knowledge_base(query: str, k: int = 5) -> str:
    """Semantic search over the knowledge base."""
    results = vectorstore.similarity_search_with_score(query, k=k)
    return json.dumps([
        {"content": doc.page_content, "source": doc.metadata.get("source"), "score": score}
        for doc, score in results
    ], indent=2)

@mcp.tool()
def ingest_document(content: str, source: str, metadata: dict = {}) -> str:
    """Add a document to the knowledge base."""
    from langchain.text_splitter import RecursiveCharacterTextSplitter
    splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
    chunks = splitter.split_text(content)
    vectorstore.add_texts(chunks, metadatas=[{**metadata, "source": source}] * len(chunks))
    return f"Ingested {len(chunks)} chunks from {source}"

@mcp.resource("rag://stats")
def kb_stats() -> str:
    """Knowledge base statistics."""
    collection = vectorstore._collection
    return json.dumps({"total_documents": collection.count()})
```

---

## 5. CI/CD Pipeline Server (GitHub Actions)

```python
import httpx
mcp = FastMCP("github-actions-server")
BASE = "https://api.github.com"
HEADERS = {"Authorization": f"token {os.environ['GITHUB_TOKEN']}"}

@mcp.tool()
async def list_workflow_runs(repo: str, status: str = "all", limit: int = 10) -> str:
    """List recent GitHub Actions workflow runs."""
    async with httpx.AsyncClient() as client:
        r = await client.get(f"{BASE}/repos/{repo}/actions/runs",
                            headers=HEADERS, params={"per_page": limit, "status": status})
        runs = r.json()["workflow_runs"]
        return json.dumps([{
            "id": run["id"], "name": run["name"],
            "status": run["status"], "conclusion": run["conclusion"],
            "branch": run["head_branch"], "url": run["html_url"],
        } for run in runs], indent=2)

@mcp.tool()
async def get_workflow_logs(repo: str, run_id: int) -> str:
    """Get logs for a specific workflow run."""
    async with httpx.AsyncClient() as client:
        r = await client.get(f"{BASE}/repos/{repo}/actions/runs/{run_id}/jobs", headers=HEADERS)
        jobs = r.json()["jobs"]
        return json.dumps([{
            "name": j["name"], "status": j["status"],
            "conclusion": j["conclusion"],
            "steps": [{"name": s["name"], "status": s["status"]} for s in j["steps"]],
        } for j in jobs], indent=2)

@mcp.tool()
async def rerun_workflow(repo: str, run_id: int) -> str:
    """Re-run a failed workflow."""
    async with httpx.AsyncClient() as client:
        r = await client.post(f"{BASE}/repos/{repo}/actions/runs/{run_id}/rerun", headers=HEADERS)
        return f"Workflow {run_id} re-run triggered" if r.status_code == 201 else f"Failed: {r.text}"
```

---

## 6. Monitoring & Alerting (Datadog/Prometheus)

```python
mcp = FastMCP("monitoring-server")

@mcp.tool()
def query_prometheus(promql: str, duration: str = "1h") -> str:
    """Execute a PromQL query."""
    r = httpx.get(f"{PROM_URL}/api/v1/query_range", params={
        "query": promql, "start": f"now()-{duration}", "end": "now()", "step": "60s"
    })
    return json.dumps(r.json()["data"]["result"][:10], indent=2)

@mcp.tool()
def get_active_alerts() -> str:
    """Get currently firing alerts."""
    r = httpx.get(f"{PROM_URL}/api/v1/alerts")
    alerts = [{"name": a["labels"]["alertname"], "severity": a["labels"].get("severity"),
               "summary": a["annotations"].get("summary")} for a in r.json()["data"]["alerts"]
              if a["state"] == "firing"]
    return json.dumps(alerts, indent=2)

@mcp.resource("monitoring://dashboards")
def dashboards() -> str:
    """List available Grafana dashboards."""
    r = httpx.get(f"{GRAFANA_URL}/api/search", headers={"Authorization": f"Bearer {GRAFANA_TOKEN}"})
    return json.dumps([{"title": d["title"], "url": d["url"]} for d in r.json()])
```

---

## Exercises

1. Build a complete database MCP server with schema, query, and explain tools
2. Create an AWS server covering EC2, S3, and CloudWatch
3. Build a monitoring server that queries Prometheus and shows active alerts
4. Implement a RAG MCP server with ingest, search, and delete tools
5. Create a multi-service DevOps server combining Git + CI/CD + Kubernetes
