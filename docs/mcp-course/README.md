# 🔌 Complete MCP (Model Context Protocol) Course

A comprehensive course on building, deploying, and integrating MCP Servers — the open standard for connecting AI models to external tools, data sources, and services.

## 📚 Course Overview

MCP (Model Context Protocol) is an open protocol created by Anthropic that standardizes how AI applications connect to external data sources and tools. Think of it as **USB-C for AI** — a universal interface that any LLM client can use to interact with any compatible server.

**Prerequisites**: Python 3.10+ or TypeScript/Node.js 18+, basic understanding of LLMs and APIs

**Duration**: ~30 hours across 10 modules

---

## 🗂️ Course Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | [Foundations & Theory](./01-Foundations.md) | What is MCP, architecture, protocol specification |
| 2 | [Protocol Deep Dive](./02-Protocol-Deep-Dive.md) | Messages, capabilities, lifecycle, transport layers |
| 3 | [Building MCP Servers (Python)](./03-Building-Servers-Python.md) | Tools, resources, prompts with Python SDK |
| 4 | [Building MCP Servers (TypeScript)](./04-Building-Servers-TypeScript.md) | Tools, resources, prompts with TypeScript SDK |
| 5 | [MCP Clients & Hosts](./05-Clients-And-Hosts.md) | Building clients, host integration, Claude Desktop |
| 6 | [Advanced Patterns](./06-Advanced-Patterns.md) | Sampling, multi-server, auth, middleware |
| 7 | [Real-World Use Cases](./07-Use-Cases.md) | Database, file system, API, cloud integrations |
| 8 | [Testing & Debugging](./08-Testing-And-Debugging.md) | MCP Inspector, unit testing, debugging strategies |
| 9 | [Production Deployment](./09-Production-Deployment.md) | Security, scaling, Docker, monitoring |
| 10 | [Capstone Projects](./10-Capstone-Projects.md) | End-to-end MCP server implementations |

---

## 🛠️ Technology Stack (2024-2025)

| Category | Tools |
|----------|-------|
| **Protocol** | MCP Specification v2025-03-26 (latest) |
| **Python SDK** | `mcp` (official), FastMCP |
| **TypeScript SDK** | `@modelcontextprotocol/sdk` |
| **Transports** | stdio, SSE (HTTP+Server-Sent Events), Streamable HTTP |
| **Clients** | Claude Desktop, Cursor, VS Code (Copilot), Windsurf, Zed |
| **Testing** | MCP Inspector, pytest, Jest |
| **Deployment** | Docker, AWS Lambda, Cloudflare Workers |
| **Auth** | OAuth 2.1, API Keys, mTLS |

---

## 🏗️ MCP Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      MCP HOST                            │
│  (Claude Desktop, IDE, Custom App)                      │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │                  MCP CLIENT                       │   │
│  │  (Maintains 1:1 connection with server)          │   │
│  └───────────┬──────────────────────┬───────────────┘   │
│              │                      │                    │
└──────────────┼──────────────────────┼────────────────────┘
               │ MCP Protocol         │ MCP Protocol
               │ (JSON-RPC 2.0)       │ (JSON-RPC 2.0)
               ▼                      ▼
┌──────────────────────┐  ┌──────────────────────┐
│    MCP SERVER A       │  │    MCP SERVER B       │
│  (e.g., Database)     │  │  (e.g., GitHub)       │
│                        │  │                        │
│  ┌─────────────────┐  │  │  ┌─────────────────┐  │
│  │ Tools           │  │  │  │ Tools           │  │
│  │ Resources       │  │  │  │ Resources       │  │
│  │ Prompts         │  │  │  │ Prompts         │  │
│  └─────────────────┘  │  │  └─────────────────┘  │
│          │             │  │          │             │
│          ▼             │  │          ▼             │
│  ┌─────────────────┐  │  │  ┌─────────────────┐  │
│  │ Local/Remote    │  │  │  │ External APIs   │  │
│  │ Data Sources    │  │  │  │ & Services      │  │
│  └─────────────────┘  │  └──└─────────────────┘  │
└────────────────────────┘  └────────────────────────┘
```

---

## 🔑 Core Concepts

| Concept | Description | Direction |
|---------|-------------|-----------|
| **Tools** | Functions the LLM can invoke (model-controlled) | Client → Server |
| **Resources** | Data the LLM can read (application-controlled) | Client → Server |
| **Prompts** | Reusable prompt templates (user-controlled) | Client → Server |
| **Sampling** | Server requests LLM completion | Server → Client |
| **Roots** | File system boundaries for server access | Client → Server |
| **Notifications** | Async event updates | Bidirectional |

---

## 🚀 Quick Start (5-Minute MCP Server)

### Python
```bash
pip install mcp[cli] httpx
```

```python
# server.py
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("demo-server")

@mcp.tool()
def add(a: int, b: int) -> int:
    """Add two numbers together."""
    return a + b

@mcp.resource("greeting://{name}")
def get_greeting(name: str) -> str:
    """Get a personalized greeting."""
    return f"Hello, {name}! Welcome to MCP."

@mcp.prompt()
def review_code(code: str) -> str:
    """Generate a code review prompt."""
    return f"Please review this code for bugs and improvements:\n\n```\n{code}\n```"

if __name__ == "__main__":
    mcp.run()
```

```bash
# Test with MCP Inspector
mcp dev server.py

# Install in Claude Desktop
mcp install server.py
```

### TypeScript
```bash
npm init -y && npm install @modelcontextprotocol/sdk zod
```

```typescript
// server.ts
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const server = new McpServer({ name: "demo-server", version: "1.0.0" });

server.tool("add", { a: z.number(), b: z.number() }, async ({ a, b }) => ({
  content: [{ type: "text", text: String(a + b) }],
}));

const transport = new StdioServerTransport();
await server.connect(transport);
```

---

## 📊 MCP Ecosystem (2025)

### Popular MCP Servers
| Server | Purpose | Maintainer |
|--------|---------|-----------|
| `@modelcontextprotocol/server-filesystem` | File system access | Anthropic |
| `@modelcontextprotocol/server-github` | GitHub integration | Anthropic |
| `@modelcontextprotocol/server-postgres` | PostgreSQL queries | Anthropic |
| `@modelcontextprotocol/server-slack` | Slack messaging | Anthropic |
| `@modelcontextprotocol/server-memory` | Knowledge graph memory | Anthropic |
| `@modelcontextprotocol/server-puppeteer` | Browser automation | Anthropic |
| `@modelcontextprotocol/server-brave-search` | Web search | Anthropic |
| `mcp-server-sqlite` | SQLite database | Community |
| `mcp-server-docker` | Docker management | Community |
| `mcp-server-kubernetes` | K8s operations | Community |
| `mcp-server-aws` | AWS services | Community |

### MCP-Compatible Clients
| Client | Status | Transport |
|--------|--------|-----------|
| Claude Desktop | ✅ Full support | stdio |
| Claude.ai (web) | ✅ Remote servers | Streamable HTTP |
| Cursor | ✅ Full support | stdio |
| VS Code (GitHub Copilot) | ✅ (2025) | stdio |
| Windsurf | ✅ Full support | stdio |
| Zed Editor | ✅ Full support | stdio |
| Amazon Q Developer | ✅ Full support | stdio |
| Continue.dev | ✅ Full support | stdio |

---

## 📖 How to Use This Course

1. **Sequential**: Modules 1-10 in order for comprehensive understanding
2. **Language-specific**: Skip to Module 3 (Python) or Module 4 (TypeScript) for hands-on coding
3. **Use-case driven**: Jump to Module 7 for specific integration patterns
4. **Production focus**: Modules 8-9 for testing and deployment

---

## 🔗 Official Resources

- [MCP Specification](https://spec.modelcontextprotocol.io)
- [MCP Documentation](https://modelcontextprotocol.io)
- [Python SDK](https://github.com/modelcontextprotocol/python-sdk)
- [TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)
- [MCP Servers Repository](https://github.com/modelcontextprotocol/servers)
- [MCP Inspector](https://github.com/modelcontextprotocol/inspector)
