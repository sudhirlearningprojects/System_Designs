# Module 8: Testing & Debugging

## MCP Inspector

The official debugging tool for MCP servers:

```bash
# Install and run
npx @modelcontextprotocol/inspector

# Or point directly at your server
npx @modelcontextprotocol/inspector python my_server.py

# With environment variables
npx @modelcontextprotocol/inspector -e API_KEY=xxx python my_server.py
```

Inspector provides:
- Interactive tool calling with argument forms
- Resource browsing and reading
- Prompt testing with argument filling
- Full JSON-RPC message log
- Connection lifecycle visualization

---

## Unit Testing (Python)

```python
# test_server.py
import pytest
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

@pytest.fixture
async def mcp_session():
    """Create a test session connected to the server."""
    params = StdioServerParameters(command="python", args=["my_server.py"])
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            yield session

@pytest.mark.asyncio
async def test_list_tools(mcp_session):
    result = await mcp_session.list_tools()
    tool_names = [t.name for t in result.tools]
    assert "query_database" in tool_names
    assert "list_tables" in tool_names

@pytest.mark.asyncio
async def test_tool_execution(mcp_session):
    result = await mcp_session.call_tool("add", arguments={"a": 5, "b": 3})
    assert result.content[0].text == "8"
    assert not result.isError

@pytest.mark.asyncio
async def test_tool_validation(mcp_session):
    """Test that invalid inputs are rejected."""
    result = await mcp_session.call_tool("query_database", arguments={"sql": "DROP TABLE users"})
    assert result.isError

@pytest.mark.asyncio
async def test_resource_read(mcp_session):
    result = await mcp_session.read_resource("config://app/settings")
    data = json.loads(result.contents[0].text)
    assert "database" in data

@pytest.mark.asyncio
async def test_prompt(mcp_session):
    result = await mcp_session.get_prompt("review_code", arguments={"code": "x = 1"})
    assert len(result.messages) > 0
    assert "review" in result.messages[0].content.text.lower()
```

---

## Unit Testing (TypeScript)

```typescript
// test/server.test.ts
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { describe, it, beforeAll, afterAll, expect } from "vitest";

let client: Client;

beforeAll(async () => {
  const transport = new StdioClientTransport({ command: "npx", args: ["tsx", "server.ts"] });
  client = new Client({ name: "test-client", version: "1.0.0" });
  await client.connect(transport);
});

afterAll(async () => { await client.close(); });

describe("Tools", () => {
  it("lists all expected tools", async () => {
    const { tools } = await client.listTools();
    expect(tools.map(t => t.name)).toContain("search_issues");
  });

  it("executes tool successfully", async () => {
    const result = await client.callTool({ name: "add", arguments: { a: 2, b: 3 } });
    expect(result.content[0].text).toBe("5");
  });

  it("returns error for invalid input", async () => {
    const result = await client.callTool({ name: "query", arguments: { sql: "DELETE FROM x" } });
    expect(result.isError).toBe(true);
  });
});

describe("Resources", () => {
  it("lists resources", async () => {
    const { resources } = await client.listResources();
    expect(resources.length).toBeGreaterThan(0);
  });

  it("reads resource content", async () => {
    const result = await client.readResource({ uri: "config://settings" });
    expect(result.contents[0].text).toBeDefined();
  });
});
```

---

## Integration Testing

```python
# test_integration.py
"""Test server against real (or mocked) external services."""
import pytest
from unittest.mock import patch, AsyncMock

@pytest.mark.asyncio
async def test_database_query_with_mock():
    """Test database tool with mocked connection."""
    mock_rows = [{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]
    
    with patch("my_server.pool.fetch", new_callable=AsyncMock, return_value=mock_rows):
        params = StdioServerParameters(command="python", args=["my_server.py"])
        async with stdio_client(params) as (read, write):
            async with ClientSession(read, write) as session:
                await session.initialize()
                result = await session.call_tool("query", {"sql": "SELECT * FROM users"})
                data = json.loads(result.content[0].text)
                assert len(data) == 2
                assert data[0]["name"] == "Alice"
```

---

## Debugging Strategies

### 1. Enable Server Logging
```python
import logging
logging.basicConfig(level=logging.DEBUG, stream=sys.stderr)
# MCP uses stderr for logs (stdout is reserved for protocol messages)
```

### 2. JSON-RPC Message Tracing
```python
# Add to server startup
import sys

class DebugTransport:
    """Wrap transport to log all messages."""
    def __init__(self, transport):
        self._transport = transport
    
    async def read(self):
        msg = await self._transport.read()
        print(f"← RECV: {msg}", file=sys.stderr)
        return msg
    
    async def write(self, msg):
        print(f"→ SEND: {msg}", file=sys.stderr)
        await self._transport.write(msg)
```

### 3. Common Issues & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| Server not found | Wrong command/path | Check `which python`, use absolute paths |
| Tools not showing | Initialization failed | Check stderr for errors, run with Inspector |
| Timeout errors | Slow tool execution | Increase timeout, add progress reporting |
| Schema validation | Wrong argument types | Check inputSchema matches Zod/Pydantic |
| Empty responses | Tool returns None | Always return a string or content list |

### 4. Claude Desktop Logs
```bash
# macOS
tail -f ~/Library/Logs/Claude/mcp*.log

# Check server process
ps aux | grep "python.*my_server"
```

---

## Exercises

1. Write unit tests covering all tools, resources, and prompts of your server
2. Set up the MCP Inspector and trace a full tool call lifecycle
3. Add structured logging to your server and debug a failing tool
4. Create integration tests with mocked external services
5. Test error handling for all edge cases (invalid inputs, timeouts, auth failures)
