# Module 9: Production Deployment

## Deployment Models

| Model | Transport | Use Case |
|-------|-----------|----------|
| Local (stdio) | stdin/stdout | IDE plugins, Claude Desktop |
| Docker container | stdio (via docker exec) | Isolated local servers |
| Remote HTTP | Streamable HTTP | Multi-tenant, cloud-hosted |
| Serverless | Streamable HTTP | Auto-scaling, pay-per-use |

---

## Docker Deployment

```dockerfile
# Dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["python", "server.py", "--transport", "http", "--port", "8000"]
```

```json
// Claude Desktop config (Docker-based server)
{
  "mcpServers": {
    "my-server": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "-e", "API_KEY=xxx", "my-mcp-server:latest"]
    }
  }
}
```

---

## Remote HTTP Server (Streamable HTTP)

```python
from mcp.server.fastmcp import FastMCP
from starlette.applications import Starlette
from starlette.routing import Route
from mcp.server.streamable_http import StreamableHTTPServerTransport

mcp = FastMCP("remote-server")

# Define tools, resources, prompts...
@mcp.tool()
def hello(name: str) -> str:
    """Greet someone."""
    return f"Hello, {name}!"

# HTTP server setup
async def handle_mcp(request):
    transport = StreamableHTTPServerTransport("/mcp")
    await mcp._server.connect(transport)
    return await transport.handle_request(request)

app = Starlette(routes=[Route("/mcp", handle_mcp, methods=["POST", "GET"])])

# Run: uvicorn server:app --host 0.0.0.0 --port 8000
```

---

## Security Best Practices

### Input Validation
```python
import re

DANGEROUS_PATTERNS = ["DROP", "DELETE", "TRUNCATE", "ALTER", "EXEC", ";--"]

@mcp.tool()
def safe_query(sql: str) -> str:
    """Execute validated SQL."""
    normalized = sql.upper()
    for pattern in DANGEROUS_PATTERNS:
        if pattern in normalized:
            raise ValueError(f"Forbidden SQL pattern: {pattern}")
    
    if not normalized.strip().startswith("SELECT"):
        raise ValueError("Only SELECT queries allowed")
    
    # Parameterized query execution
    return execute_safe(sql)
```

### Rate Limiting
```python
from collections import defaultdict
from time import time

class RateLimiter:
    def __init__(self, max_calls: int = 60, window: int = 60):
        self.max_calls = max_calls
        self.window = window
        self.calls: dict[str, list] = defaultdict(list)
    
    def check(self, client_id: str) -> bool:
        now = time()
        self.calls[client_id] = [t for t in self.calls[client_id] if now - t < self.window]
        if len(self.calls[client_id]) >= self.max_calls:
            raise RuntimeError(f"Rate limit exceeded: {self.max_calls} calls per {self.window}s")
        self.calls[client_id].append(now)
        return True
```

### Sandboxing File Access
```python
from pathlib import Path

ALLOWED_ROOT = Path("/data/project").resolve()

def safe_read(path: str) -> str:
    """Read file with path traversal protection."""
    resolved = (ALLOWED_ROOT / path).resolve()
    if not resolved.is_relative_to(ALLOWED_ROOT):
        raise PermissionError("Access denied: path outside allowed root")
    if not resolved.exists():
        raise FileNotFoundError(f"Not found: {path}")
    return resolved.read_text()
```

---

## Monitoring

```python
from prometheus_client import Counter, Histogram

TOOL_CALLS = Counter("mcp_tool_calls_total", "Total tool calls", ["tool", "status"])
TOOL_LATENCY = Histogram("mcp_tool_latency_seconds", "Tool call latency", ["tool"])

def monitored_tool(func):
    async def wrapper(*args, **kwargs):
        import time
        start = time.time()
        try:
            result = await func(*args, **kwargs)
            TOOL_CALLS.labels(tool=func.__name__, status="success").inc()
            return result
        except Exception:
            TOOL_CALLS.labels(tool=func.__name__, status="error").inc()
            raise
        finally:
            TOOL_LATENCY.labels(tool=func.__name__).observe(time.time() - start)
    wrapper.__name__ = func.__name__
    wrapper.__doc__ = func.__doc__
    return wrapper
```

---

## Serverless Deployment (AWS Lambda)

```python
# lambda_handler.py
from mangum import Mangum
from starlette.applications import Starlette

# Wrap MCP HTTP server for Lambda
app = create_mcp_app()  # Your Starlette/FastAPI app
handler = Mangum(app)
```

```yaml
# serverless.yml (Serverless Framework)
service: mcp-server
provider:
  name: aws
  runtime: python3.12
  timeout: 30
functions:
  mcp:
    handler: lambda_handler.handler
    events:
      - httpApi:
          path: /mcp
          method: ANY
```

---

## Exercises

1. Dockerize an MCP server and configure it with Claude Desktop
2. Deploy a remote MCP server with Streamable HTTP transport
3. Add rate limiting and input validation to all tools
4. Set up Prometheus metrics for tool call monitoring
5. Deploy an MCP server to AWS Lambda with API Gateway
