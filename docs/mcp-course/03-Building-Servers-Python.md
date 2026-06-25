# Module 3: Building MCP Servers (Python)

## Setup

```bash
# Install official MCP Python SDK
pip install mcp[cli]

# Additional dependencies
pip install httpx pydantic sqlalchemy
```

---

## FastMCP (High-Level API)

The recommended way to build MCP servers in Python:

```python
from mcp.server.fastmcp import FastMCP

# Create server instance
mcp = FastMCP(
    name="my-server",
    version="1.0.0",
    description="A sample MCP server",
)
```

---

## Tools

### Basic Tool
```python
@mcp.tool()
def calculate_bmi(weight_kg: float, height_m: float) -> str:
    """Calculate Body Mass Index from weight and height."""
    bmi = weight_kg / (height_m ** 2)
    category = (
        "underweight" if bmi < 18.5
        else "normal" if bmi < 25
        else "overweight" if bmi < 30
        else "obese"
    )
    return f"BMI: {bmi:.1f} ({category})"
```

### Tool with Complex Input
```python
from pydantic import BaseModel, Field
from typing import Optional

class SearchParams(BaseModel):
    query: str = Field(description="Search query string")
    max_results: int = Field(default=10, ge=1, le=100)
    filters: Optional[dict] = Field(default=None, description="Key-value filters")

@mcp.tool()
def search_documents(params: SearchParams) -> str:
    """Search the document store with optional filters."""
    results = document_store.search(
        query=params.query,
        limit=params.max_results,
        filters=params.filters,
    )
    return json.dumps([{"title": r.title, "snippet": r.snippet} for r in results])
```

### Tool with Error Handling
```python
@mcp.tool()
def query_database(sql: str, database: str = "default") -> str:
    """Execute a read-only SQL query against the database.
    
    Only SELECT queries are allowed. Returns results as JSON.
    """
    # Validate: prevent dangerous operations
    normalized = sql.strip().upper()
    if not normalized.startswith("SELECT"):
        raise ValueError("Only SELECT queries are allowed")
    
    if any(keyword in normalized for keyword in ["DROP", "DELETE", "UPDATE", "INSERT", "ALTER"]):
        raise ValueError("Mutation queries are not permitted")
    
    try:
        conn = get_connection(database)
        result = conn.execute(sql)
        rows = [dict(row) for row in result.fetchall()]
        return json.dumps(rows, indent=2, default=str)
    except Exception as e:
        raise RuntimeError(f"Query failed: {e}")
```

### Async Tool
```python
import httpx

@mcp.tool()
async def fetch_url(url: str, method: str = "GET") -> str:
    """Fetch content from a URL. Supports GET and HEAD methods."""
    if method not in ("GET", "HEAD"):
        raise ValueError("Only GET and HEAD methods are allowed")
    
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.request(method, url)
        response.raise_for_status()
        
        if method == "HEAD":
            return json.dumps(dict(response.headers))
        
        content_type = response.headers.get("content-type", "")
        if "json" in content_type:
            return json.dumps(response.json(), indent=2)
        return response.text[:10000]  # Limit response size
```

### Tool Returning Multiple Content Types
```python
from mcp.types import TextContent, ImageContent

@mcp.tool()
def generate_chart(data: list[dict], chart_type: str = "bar") -> list:
    """Generate a chart from data. Returns both the image and a text description."""
    import matplotlib.pyplot as plt
    import base64
    import io
    
    fig, ax = plt.subplots()
    # ... create chart ...
    
    buf = io.BytesIO()
    fig.savefig(buf, format='png')
    buf.seek(0)
    image_b64 = base64.b64encode(buf.read()).decode()
    
    return [
        TextContent(type="text", text=f"Generated {chart_type} chart with {len(data)} data points"),
        ImageContent(type="image", data=image_b64, mimeType="image/png"),
    ]
```

---

## Resources

### Static Resource
```python
@mcp.resource("config://app/settings")
def get_app_settings() -> str:
    """Current application configuration."""
    settings = load_settings()
    return json.dumps(settings, indent=2)
```

### Dynamic Resource with URI Template
```python
@mcp.resource("file:///{path}")
def read_file(path: str) -> str:
    """Read a file from the project directory."""
    # Security: restrict to project root
    full_path = Path(PROJECT_ROOT) / path
    if not full_path.resolve().is_relative_to(Path(PROJECT_ROOT).resolve()):
        raise ValueError("Access denied: path outside project root")
    
    if not full_path.exists():
        raise FileNotFoundError(f"File not found: {path}")
    
    return full_path.read_text()
```

### Database Schema Resource
```python
@mcp.resource("postgres://schema")
def get_database_schema() -> str:
    """Get the current database schema as SQL DDL."""
    conn = get_connection()
    tables = conn.execute("""
        SELECT table_name, column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_schema = 'public'
        ORDER BY table_name, ordinal_position
    """).fetchall()
    
    schema = {}
    for row in tables:
        table = row["table_name"]
        if table not in schema:
            schema[table] = []
        schema[table].append({
            "column": row["column_name"],
            "type": row["data_type"],
            "nullable": row["is_nullable"] == "YES",
        })
    
    return json.dumps(schema, indent=2)
```

### Resource with MIME Types
```python
@mcp.resource("metrics://dashboard", mime_type="application/json")
def get_metrics() -> str:
    """Real-time application metrics."""
    return json.dumps({
        "requests_per_second": 1250,
        "error_rate": 0.02,
        "p95_latency_ms": 145,
        "active_connections": 890,
    })
```

---

## Prompts

### Simple Prompt
```python
@mcp.prompt()
def review_code(code: str, language: str = "python") -> str:
    """Generate a code review prompt."""
    return f"""Please review the following {language} code for:
1. Bugs and potential errors
2. Security vulnerabilities
3. Performance issues
4. Code style and best practices

```{language}
{code}
```

Provide specific line-by-line feedback."""
```

### Multi-Message Prompt
```python
from mcp.types import PromptMessage, TextContent

@mcp.prompt()
def debug_error(error: str, context: str = "") -> list[PromptMessage]:
    """Help debug an error with optional context."""
    messages = []
    
    if context:
        messages.append(PromptMessage(
            role="user",
            content=TextContent(type="text", text=f"Here's the relevant code context:\n\n{context}")
        ))
    
    messages.append(PromptMessage(
        role="user",
        content=TextContent(
            type="text",
            text=f"I'm getting this error:\n\n```\n{error}\n```\n\n"
                 f"Please explain what's causing it and how to fix it."
        )
    ))
    
    return messages
```

---

## Complete Server Example: Git MCP Server

```python
# git_server.py
import subprocess
import json
from pathlib import Path
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("git-server", description="MCP server for Git operations")

REPO_PATH = Path.cwd()

def run_git(*args: str) -> str:
    """Run a git command and return output."""
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_PATH,
        capture_output=True,
        text=True,
        timeout=30,
    )
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed: {result.stderr}")
    return result.stdout.strip()


# === TOOLS ===

@mcp.tool()
def git_status() -> str:
    """Get the current git status (modified, staged, untracked files)."""
    return run_git("status", "--porcelain")

@mcp.tool()
def git_log(n: int = 10, oneline: bool = True) -> str:
    """Get recent git commits."""
    args = ["log", f"-{n}"]
    if oneline:
        args.append("--oneline")
    return run_git(*args)

@mcp.tool()
def git_diff(staged: bool = False, file: str = "") -> str:
    """Show git diff for working directory or staged changes."""
    args = ["diff"]
    if staged:
        args.append("--staged")
    if file:
        args.append(file)
    return run_git(*args)

@mcp.tool()
def git_blame(file: str, start_line: int = 1, end_line: int = 50) -> str:
    """Show git blame for a file (who changed each line)."""
    return run_git("blame", f"-L{start_line},{end_line}", file)

@mcp.tool()
def git_show(commit: str = "HEAD") -> str:
    """Show details of a specific commit."""
    return run_git("show", commit, "--stat")


# === RESOURCES ===

@mcp.resource("git://status")
def resource_status() -> str:
    """Current repository status."""
    return run_git("status")

@mcp.resource("git://branches")
def resource_branches() -> str:
    """List all branches."""
    return run_git("branch", "-a", "--format=%(refname:short) %(upstream:short) %(committerdate:relative)")

@mcp.resource("git://remotes")
def resource_remotes() -> str:
    """List configured remotes."""
    return run_git("remote", "-v")


# === PROMPTS ===

@mcp.prompt()
def commit_message(diff: str = "") -> str:
    """Generate a commit message based on the current diff."""
    if not diff:
        diff = run_git("diff", "--staged")
    return f"""Based on the following git diff, write a concise and descriptive commit message 
following conventional commits format (feat:, fix:, docs:, refactor:, etc.).

Diff:
```
{diff[:5000]}
```

Write only the commit message, nothing else."""


if __name__ == "__main__":
    mcp.run()
```

---

## Server Configuration & Lifecycle

### Context and Lifespan
```python
from contextlib import asynccontextmanager
from mcp.server.fastmcp import FastMCP
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession

@asynccontextmanager
async def app_lifespan(server: FastMCP):
    """Manage server lifecycle: initialize and cleanup resources."""
    # Startup
    engine = create_async_engine("postgresql+asyncpg://user:pass@localhost/db")
    server.state["db_engine"] = engine
    
    yield  # Server runs
    
    # Shutdown
    await engine.dispose()

mcp = FastMCP("db-server", lifespan=app_lifespan)

@mcp.tool()
async def query(sql: str) -> str:
    """Query the database."""
    engine = mcp.state["db_engine"]
    async with AsyncSession(engine) as session:
        result = await session.execute(text(sql))
        return json.dumps([dict(row) for row in result.mappings()])
```

### Dependencies and Context
```python
from mcp.server.fastmcp import FastMCP, Context

mcp = FastMCP("context-server")

@mcp.tool()
async def long_running_task(data: str, ctx: Context) -> str:
    """A tool that reports progress."""
    total_steps = 10
    
    for i in range(total_steps):
        # Report progress to client
        await ctx.report_progress(i, total_steps, f"Processing step {i+1}")
        await asyncio.sleep(1)  # Simulate work
    
    # Log messages visible to client
    await ctx.info(f"Task completed successfully for {len(data)} chars")
    
    return f"Processed {len(data)} characters in {total_steps} steps"
```

---

## Running the Server

### Development (with Inspector)
```bash
# Start with MCP Inspector for interactive testing
mcp dev git_server.py

# With environment variables
mcp dev git_server.py -e DATABASE_URL=postgres://localhost/mydb
```

### Install in Claude Desktop
```bash
# Auto-configures Claude Desktop
mcp install git_server.py

# With custom name
mcp install git_server.py --name "Git Tools"

# With env vars
mcp install git_server.py -e REPO_PATH=/path/to/repo
```

### Manual Claude Desktop Configuration
```json
// ~/Library/Application Support/Claude/claude_desktop_config.json (macOS)
{
  "mcpServers": {
    "git-server": {
      "command": "python",
      "args": ["/path/to/git_server.py"],
      "env": {
        "REPO_PATH": "/Users/me/projects/myapp"
      }
    }
  }
}
```

### Programmatic Server (Low-Level)
```python
from mcp.server import Server
from mcp.server.stdio import stdio_server
import mcp.types as types

server = Server("my-server")

@server.list_tools()
async def list_tools() -> list[types.Tool]:
    return [
        types.Tool(
            name="add",
            description="Add two numbers",
            inputSchema={
                "type": "object",
                "properties": {
                    "a": {"type": "number"},
                    "b": {"type": "number"},
                },
                "required": ["a", "b"],
            },
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> list[types.TextContent]:
    if name == "add":
        result = arguments["a"] + arguments["b"]
        return [types.TextContent(type="text", text=str(result))]
    raise ValueError(f"Unknown tool: {name}")

async def main():
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream, server.create_initialization_options())

if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
```

---

## Exercises

1. Build an MCP server that wraps a REST API you use (e.g., weather, news, stocks)
2. Create a file system server with read/write/search tools and security restrictions
3. Build a database server with schema introspection and query execution
4. Add prompt templates for common queries to your database server
5. Use the Context object to implement progress reporting for a batch operation
