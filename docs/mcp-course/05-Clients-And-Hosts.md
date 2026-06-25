# Module 5: MCP Clients & Hosts

## Understanding Clients vs Hosts

- **Host**: The user-facing application (Claude Desktop, IDE, your app)
- **Client**: Protocol-level component that manages one server connection

A host can have multiple clients, each connected to a different MCP server.

---

## Building an MCP Client (Python)

```python
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def run_client():
    # Connect to a server via stdio
    server_params = StdioServerParameters(
        command="python",
        args=["my_server.py"],
        env={"API_KEY": "xxx"},
    )
    
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # Initialize connection
            await session.initialize()
            
            # List available tools
            tools = await session.list_tools()
            print(f"Available tools: {[t.name for t in tools.tools]}")
            
            # Call a tool
            result = await session.call_tool("add", arguments={"a": 5, "b": 3})
            print(f"Result: {result.content[0].text}")
            
            # List resources
            resources = await session.list_resources()
            for r in resources.resources:
                print(f"Resource: {r.uri} - {r.name}")
            
            # Read a resource
            content = await session.read_resource("config://app/settings")
            print(f"Config: {content.contents[0].text}")
            
            # List prompts
            prompts = await session.list_prompts()
            for p in prompts.prompts:
                print(f"Prompt: {p.name} - {p.description}")
            
            # Get a prompt
            prompt = await session.get_prompt("review_code", arguments={"code": "x = 1"})
            print(f"Prompt messages: {prompt.messages}")

import asyncio
asyncio.run(run_client())
```

---

## Building an MCP Client (TypeScript)

```typescript
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

async function main() {
  const transport = new StdioClientTransport({
    command: "python",
    args: ["my_server.py"],
  });
  
  const client = new Client({ name: "my-client", version: "1.0.0" }, {
    capabilities: { sampling: {} },  // Support sampling requests from server
  });
  
  await client.connect(transport);
  
  // List tools
  const { tools } = await client.listTools();
  console.log("Tools:", tools.map(t => t.name));
  
  // Call tool
  const result = await client.callTool({ name: "add", arguments: { a: 5, b: 3 } });
  console.log("Result:", result.content);
  
  // Read resource
  const resource = await client.readResource({ uri: "config://app/settings" });
  console.log("Resource:", resource.contents);
  
  // Handle notifications from server
  client.setNotificationHandler("notifications/resources/updated", (params) => {
    console.log("Resource updated:", params.uri);
  });
  
  await client.close();
}

main();
```

---

## Building an MCP Host (Multi-Server)

A host manages multiple server connections and aggregates their capabilities:

```python
import asyncio
from dataclasses import dataclass
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

@dataclass
class ServerConfig:
    name: str
    command: str
    args: list[str]
    env: dict = None

class MCPHost:
    """Host that manages multiple MCP server connections."""
    
    def __init__(self):
        self.sessions: dict[str, ClientSession] = {}
        self.tools: dict[str, tuple[str, dict]] = {}  # tool_name -> (server_name, schema)
    
    async def connect_server(self, config: ServerConfig):
        """Connect to an MCP server."""
        params = StdioServerParameters(
            command=config.command,
            args=config.args,
            env=config.env,
        )
        
        read, write = await stdio_client(params).__aenter__()
        session = ClientSession(read, write)
        await session.__aenter__()
        await session.initialize()
        
        self.sessions[config.name] = session
        
        # Index tools
        tools_response = await session.list_tools()
        for tool in tools_response.tools:
            self.tools[tool.name] = (config.name, tool)
        
        print(f"Connected to {config.name}: {len(tools_response.tools)} tools")
    
    async def call_tool(self, tool_name: str, arguments: dict) -> str:
        """Route a tool call to the correct server."""
        if tool_name not in self.tools:
            raise ValueError(f"Unknown tool: {tool_name}. Available: {list(self.tools.keys())}")
        
        server_name, tool_schema = self.tools[tool_name]
        session = self.sessions[server_name]
        
        result = await session.call_tool(tool_name, arguments=arguments)
        return result.content[0].text
    
    def get_all_tool_schemas(self) -> list[dict]:
        """Get schemas for all tools across all servers (for LLM function calling)."""
        schemas = []
        for tool_name, (server, tool) in self.tools.items():
            schemas.append({
                "name": tool.name,
                "description": tool.description,
                "parameters": tool.inputSchema,
                "server": server,
            })
        return schemas
    
    async def disconnect_all(self):
        for name, session in self.sessions.items():
            await session.__aexit__(None, None, None)


# Usage
async def main():
    host = MCPHost()
    
    await host.connect_server(ServerConfig(
        name="database", command="python", args=["db_server.py"]
    ))
    await host.connect_server(ServerConfig(
        name="github", command="npx", args=["tsx", "github_server.ts"]
    ))
    await host.connect_server(ServerConfig(
        name="filesystem", command="python", args=["fs_server.py"]
    ))
    
    # All tools from all servers available via single interface
    all_tools = host.get_all_tool_schemas()
    print(f"Total tools available: {len(all_tools)}")
    
    # Route tool calls transparently
    result = await host.call_tool("query_database", {"sql": "SELECT count(*) FROM users"})
    print(result)
    
    await host.disconnect_all()
```

---

## Integrating with LLMs (Host + LLM + MCP)

```python
from openai import AsyncOpenAI

class AIAssistantWithMCP:
    """Complete AI assistant using MCP servers for tool execution."""
    
    def __init__(self):
        self.host = MCPHost()
        self.llm = AsyncOpenAI()
    
    async def setup(self, servers: list[ServerConfig]):
        for server in servers:
            await self.host.connect_server(server)
    
    async def chat(self, user_message: str) -> str:
        # Convert MCP tools to OpenAI function format
        functions = []
        for tool_name, (server, tool) in self.host.tools.items():
            functions.append({
                "type": "function",
                "function": {
                    "name": tool.name,
                    "description": tool.description,
                    "parameters": tool.inputSchema,
                }
            })
        
        messages = [{"role": "user", "content": user_message}]
        
        # LLM decides which tools to call
        response = await self.llm.chat.completions.create(
            model="gpt-4o",
            messages=messages,
            tools=functions,
        )
        
        # Execute tool calls via MCP
        message = response.choices[0].message
        if message.tool_calls:
            messages.append(message)
            
            for tool_call in message.tool_calls:
                result = await self.host.call_tool(
                    tool_call.function.name,
                    json.loads(tool_call.function.arguments),
                )
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": result,
                })
            
            # Get final response with tool results
            final = await self.llm.chat.completions.create(
                model="gpt-4o",
                messages=messages,
            )
            return final.choices[0].message.content
        
        return message.content
```

---

## Claude Desktop Integration

### Configuration File Locations
```
macOS:   ~/Library/Application Support/Claude/claude_desktop_config.json
Windows: %APPDATA%\Claude\claude_desktop_config.json
Linux:   ~/.config/Claude/claude_desktop_config.json
```

### Multi-Server Configuration
```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/me/projects"],
      "env": {}
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres", "postgresql://localhost/mydb"],
      "env": {}
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "<token>"
      }
    },
    "custom-server": {
      "command": "python",
      "args": ["/path/to/my_server.py"],
      "env": {
        "API_KEY": "<key>"
      }
    }
  }
}
```

---

## Streamable HTTP Client (Remote Servers)

```typescript
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/sdk/client/streamableHttp.js";

// Connect to a remote MCP server via HTTP
const transport = new StreamableHTTPClientTransport(
  new URL("https://my-mcp-server.example.com/mcp")
);

const client = new Client({ name: "remote-client", version: "1.0.0" });
await client.connect(transport);

// Use normally — same API as stdio
const tools = await client.listTools();
```

---

## Exercises

1. Build a Python MCP client that connects to two servers simultaneously
2. Create an AI assistant that uses MCP tools with OpenAI function calling
3. Configure Claude Desktop with 3+ MCP servers and test tool routing
4. Implement a remote MCP client using Streamable HTTP transport
5. Build a host that dynamically discovers and connects to available servers
