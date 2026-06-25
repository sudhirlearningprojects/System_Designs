# Module 6: Advanced Patterns

## 1. Sampling (Server Requests LLM)

Sampling lets the server ask the client's LLM to generate text. This enables AI-in-the-loop tool execution.

```python
from mcp.server.fastmcp import FastMCP, Context

mcp = FastMCP("smart-server")

@mcp.tool()
async def analyze_and_categorize(data: str, ctx: Context) -> str:
    """Analyze data using the client's LLM for classification."""
    
    # Server asks the client's LLM to help
    result = await ctx.session.create_message(
        messages=[{
            "role": "user",
            "content": {"type": "text", "text": f"Classify this into categories (return JSON): {data}"}
        }],
        max_tokens=500,
    )
    
    # Use LLM's classification in further processing
    classification = json.loads(result.content.text)
    
    # Do something with the classification
    stored = await store_categorized(data, classification)
    return f"Stored with categories: {classification}"
```

**Use cases**:
- Server needs AI reasoning during tool execution
- Classify/summarize data before storing
- Generate human-readable summaries of complex results
- Multi-step workflows where the server orchestrates LLM calls

---

## 2. Middleware Pattern

Wrap servers with cross-cutting concerns:

```python
from mcp.server.fastmcp import FastMCP
import time
import logging

logger = logging.getLogger(__name__)

class MCPMiddleware:
    """Middleware for logging, rate limiting, and metrics."""
    
    def __init__(self, server: FastMCP):
        self.server = server
        self.call_counts: dict[str, int] = {}
        self._wrap_tools()
    
    def _wrap_tools(self):
        """Wrap all tool handlers with middleware."""
        original_call = self.server._tool_handlers
        
        for tool_name, handler in original_call.items():
            original_call[tool_name] = self._wrap_handler(tool_name, handler)
    
    def _wrap_handler(self, name: str, handler):
        async def wrapped(*args, **kwargs):
            start = time.time()
            self.call_counts[name] = self.call_counts.get(name, 0) + 1
            
            logger.info(f"Tool called: {name} (call #{self.call_counts[name]})")
            
            try:
                result = await handler(*args, **kwargs)
                duration = time.time() - start
                logger.info(f"Tool {name} completed in {duration:.3f}s")
                return result
            except Exception as e:
                logger.error(f"Tool {name} failed: {e}")
                raise
        
        return wrapped
```

---

## 3. Authentication & Authorization

### OAuth 2.1 for Remote Servers

```typescript
// server with OAuth protection
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import express from "express";

const app = express();
const server = new McpServer({ name: "secure-server", version: "1.0.0" });

// OAuth middleware
async function validateToken(req: express.Request): Promise<User | null> {
  const token = req.headers.authorization?.replace("Bearer ", "");
  if (!token) return null;
  
  // Validate with OAuth provider
  const user = await verifyOAuthToken(token);
  return user;
}

// Permission-based tool access
server.tool(
  "admin_operation",
  "Perform an admin operation (requires admin role)",
  { action: z.string() },
  async ({ action }, context) => {
    const user = context.meta?.user;
    if (!user?.roles.includes("admin")) {
      return { content: [{ type: "text", text: "Permission denied: admin role required" }], isError: true };
    }
    // ... perform admin action
  }
);

// Express endpoint with auth
app.post("/mcp", async (req, res) => {
  const user = await validateToken(req);
  if (!user) {
    return res.status(401).json({ error: "Unauthorized" });
  }
  
  const transport = new StreamableHTTPServerTransport("/mcp");
  // Pass user context to server
  await server.connect(transport, { meta: { user } });
  transport.handleRequest(req, res);
});
```

### API Key Authentication (Simpler)
```python
import os
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("auth-server")

# Validate API key from environment (set by host)
VALID_API_KEY = os.environ.get("SERVER_API_KEY")

def require_auth(func):
    """Decorator to require authentication."""
    async def wrapper(*args, **kwargs):
        # In stdio transport, auth is handled by the host's env vars
        # In HTTP transport, check headers
        if not VALID_API_KEY:
            raise PermissionError("Server not configured with API key")
        return await func(*args, **kwargs)
    wrapper.__name__ = func.__name__
    wrapper.__doc__ = func.__doc__
    return wrapper
```

---

## 4. Multi-Server Composition

### Server-to-Server Communication
```python
# A server that itself acts as a client to other MCP servers
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("orchestrator-server")

class SubServerConnection:
    def __init__(self):
        self.sessions: dict[str, ClientSession] = {}
    
    async def connect(self, name: str, command: str, args: list):
        params = StdioServerParameters(command=command, args=args)
        read, write = await stdio_client(params).__aenter__()
        session = ClientSession(read, write)
        await session.__aenter__()
        await session.initialize()
        self.sessions[name] = session

sub_servers = SubServerConnection()

@mcp.tool()
async def orchestrated_workflow(task: str) -> str:
    """Execute a multi-step workflow across multiple servers."""
    
    # Step 1: Query database
    db_result = await sub_servers.sessions["database"].call_tool(
        "query", {"sql": f"SELECT * FROM tasks WHERE name = '{task}'"}
    )
    
    # Step 2: Process with another service
    processed = await sub_servers.sessions["processor"].call_tool(
        "process", {"data": db_result.content[0].text}
    )
    
    # Step 3: Store results
    await sub_servers.sessions["storage"].call_tool(
        "store", {"key": task, "value": processed.content[0].text}
    )
    
    return f"Workflow complete: {processed.content[0].text}"
```

---

## 5. Dynamic Tool Registration

Add/remove tools at runtime based on configuration:

```python
from mcp.server import Server
import mcp.types as types

server = Server("dynamic-server")

# Dynamic tool registry
_dynamic_tools: dict[str, dict] = {}

def register_tool(name: str, description: str, schema: dict, handler):
    """Register a tool at runtime."""
    _dynamic_tools[name] = {
        "description": description,
        "schema": schema,
        "handler": handler,
    }
    # Notify clients that tool list changed
    server.request_context.session.send_notification(
        "notifications/tools/list_changed", {}
    )

@server.list_tools()
async def list_tools() -> list[types.Tool]:
    return [
        types.Tool(name=name, description=info["description"], inputSchema=info["schema"])
        for name, info in _dynamic_tools.items()
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict):
    if name not in _dynamic_tools:
        raise ValueError(f"Tool not found: {name}")
    handler = _dynamic_tools[name]["handler"]
    return await handler(arguments)
```

---

## 6. Caching & Performance

```python
from functools import lru_cache
from datetime import datetime, timedelta
import asyncio

class CachedMCPServer:
    """MCP server with built-in caching for expensive operations."""
    
    def __init__(self):
        self._cache: dict[str, tuple[any, datetime]] = {}
        self._cache_ttl = timedelta(minutes=5)
    
    async def cached_tool_call(self, key: str, func, *args, **kwargs):
        """Cache tool results with TTL."""
        now = datetime.utcnow()
        
        if key in self._cache:
            result, cached_at = self._cache[key]
            if now - cached_at < self._cache_ttl:
                return result
        
        result = await func(*args, **kwargs)
        self._cache[key] = (result, now)
        return result

mcp = FastMCP("cached-server")

@mcp.tool()
async def get_expensive_data(query: str) -> str:
    """Fetch data with automatic caching."""
    cache_key = f"expensive:{hash(query)}"
    
    async def fetch():
        # Expensive operation
        await asyncio.sleep(2)
        return f"Result for: {query}"
    
    return await cached_server.cached_tool_call(cache_key, fetch)
```

---

## 7. Streaming Results (Long-Running Operations)

```python
@mcp.tool()
async def process_large_dataset(file_path: str, ctx: Context) -> str:
    """Process a large file with progress updates."""
    
    total_lines = count_lines(file_path)
    processed = 0
    results = []
    
    with open(file_path) as f:
        for i, line in enumerate(f):
            result = process_line(line)
            results.append(result)
            processed += 1
            
            # Report progress every 100 lines
            if processed % 100 == 0:
                await ctx.report_progress(processed, total_lines)
    
    return json.dumps({
        "total_processed": processed,
        "summary": summarize(results),
    })
```

---

## 8. Event-Driven Resource Updates

```python
import asyncio
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

class FileWatcher(FileSystemEventHandler):
    def __init__(self, server):
        self.server = server
    
    def on_modified(self, event):
        if not event.is_directory:
            # Notify clients that resource changed
            asyncio.run_coroutine_threadsafe(
                self.server.notify_resource_updated(f"file:///{event.src_path}"),
                asyncio.get_event_loop(),
            )

# Start file watcher in server lifespan
@asynccontextmanager
async def lifespan(server):
    observer = Observer()
    observer.schedule(FileWatcher(server), "/watched/path", recursive=True)
    observer.start()
    yield
    observer.stop()
```

---

## Exercises

1. Implement sampling: build a server that uses the client's LLM to summarize search results
2. Create a permission system where different tools require different user roles
3. Build an orchestrator server that coordinates 3 sub-servers
4. Implement resource subscriptions with file system watching
5. Add caching to an expensive API-backed tool and measure latency improvement
