# Module 1: MCP Foundations & Theory

## What is Model Context Protocol (MCP)?

MCP is an **open standard** (created by Anthropic, Nov 2024) that defines how AI applications communicate with external tools and data sources. It replaces the fragmented approach of custom integrations with a universal protocol.

### The Problem MCP Solves

**Before MCP:**
```
Claude → Custom Plugin A (bespoke code)
ChatGPT → Custom Plugin B (different code)
Cursor → Custom Integration C (yet another approach)

Each AI app × Each tool = M×N custom integrations
```

**After MCP:**
```
Any MCP Client (Claude, Cursor, VS Code, etc.)
        │
        │  Standard MCP Protocol (JSON-RPC 2.0)
        ▼
Any MCP Server (Database, GitHub, Slack, etc.)

M clients + N servers = M+N implementations (not M×N)
```

### Analogy: USB-C for AI

| USB-C | MCP |
|-------|-----|
| Universal physical connector | Universal AI-tool protocol |
| Any device can connect | Any LLM client can connect |
| Standard data/power protocol | Standard tool/resource protocol |
| Replaced proprietary chargers | Replaces custom AI integrations |

---

## MCP Architecture Layers

```
┌────────────────────────────────────────────────────────────┐
│                         USER                                │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                        HOST                                  │
│  The application users interact with                         │
│  (Claude Desktop, IDE, Custom AI App)                        │
│                                                              │
│  Responsibilities:                                           │
│  - User interface                                            │
│  - Manages multiple MCP client instances                     │
│  - Controls permissions & consent                            │
│  - Aggregates context from multiple servers                  │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                       CLIENT                                 │
│  Protocol-level component within the host                    │
│  (One client per server connection)                          │
│                                                              │
│  Responsibilities:                                           │
│  - Maintains stateful session with ONE server                │
│  - Handles protocol negotiation                              │
│  - Routes messages and notifications                         │
│  - Manages subscriptions                                     │
└───────────────────────────┬────────────────────────────────┘
                            │  JSON-RPC 2.0
                            │  (stdio / HTTP+SSE / Streamable HTTP)
┌───────────────────────────▼────────────────────────────────┐
│                       SERVER                                 │
│  Lightweight service exposing capabilities                   │
│                                                              │
│  Exposes:                                                    │
│  - Tools (functions LLM can call)                           │
│  - Resources (data LLM can read)                            │
│  - Prompts (templates for common tasks)                     │
│                                                              │
│  Connects to:                                                │
│  - Databases, APIs, file systems, services                  │
└────────────────────────────────────────────────────────────┘
```

---

## The Three Primitives

### 1. Tools (Model-Controlled)

Functions that the AI model can choose to invoke. The model decides when and how to use them.

```json
{
  "name": "query_database",
  "description": "Execute a SQL query against the production database",
  "inputSchema": {
    "type": "object",
    "properties": {
      "sql": {"type": "string", "description": "SQL SELECT query"},
      "database": {"type": "string", "enum": ["users", "orders", "products"]}
    },
    "required": ["sql", "database"]
  }
}
```

**Use cases**: API calls, database queries, calculations, web searches, code execution

### 2. Resources (Application-Controlled)

Data that the application exposes to the LLM as context. The host/application decides when to include them.

```json
{
  "uri": "file:///project/src/main.py",
  "name": "Main Application File",
  "mimeType": "text/x-python",
  "description": "The main entry point of the application"
}
```

**Use cases**: File contents, database schemas, configuration files, documentation, live data feeds

### 3. Prompts (User-Controlled)

Pre-built prompt templates that users can select. Users explicitly choose when to use them.

```json
{
  "name": "code_review",
  "description": "Review code for bugs, security issues, and best practices",
  "arguments": [
    {"name": "code", "description": "Code to review", "required": true},
    {"name": "language", "description": "Programming language", "required": false}
  ]
}
```

**Use cases**: Code review templates, analysis prompts, report generation, debugging workflows

---

## Control Hierarchy

```
┌─────────────────────────────────────────────────┐
│  USER (Highest Control)                          │
│  - Selects prompts                               │
│  - Approves tool calls (if configured)           │
│  - Grants permissions                            │
├─────────────────────────────────────────────────┤
│  APPLICATION / HOST                              │
│  - Decides which resources to attach             │
│  - Controls which servers are connected          │
│  - Manages security policies                     │
├─────────────────────────────────────────────────┤
│  MODEL (Lowest Control)                          │
│  - Decides when to use tools                     │
│  - Generates tool call arguments                 │
│  - Processes tool results                        │
└─────────────────────────────────────────────────┘
```

---

## Protocol Specification

MCP uses **JSON-RPC 2.0** over various transport layers:

### Message Types

| Type | Direction | Purpose |
|------|-----------|---------|
| Request | Client→Server or Server→Client | Expects a response |
| Response | Reply to request | Contains result or error |
| Notification | Either direction | Fire-and-forget, no response expected |

### Transport Layers

| Transport | Use Case | Connection |
|-----------|----------|-----------|
| **stdio** | Local servers, IDE integrations | Process stdin/stdout |
| **HTTP + SSE** | Remote servers (legacy) | HTTP POST + Server-Sent Events |
| **Streamable HTTP** | Remote servers (2025+) | Single HTTP endpoint, bidirectional |

### Connection Lifecycle

```
Client                              Server
  │                                    │
  │──── initialize ───────────────────▶│  (capabilities negotiation)
  │◀─── initialize response ──────────│
  │                                    │
  │──── initialized notification ────▶│  (ready to operate)
  │                                    │
  │──── tools/list ──────────────────▶│  (discover tools)
  │◀─── tools/list response ─────────│
  │                                    │
  │──── tools/call ──────────────────▶│  (invoke a tool)
  │◀─── tools/call response ─────────│
  │                                    │
  │──── ... (operational phase) ──────│
  │                                    │
  │──── shutdown ─────────────────────▶│  (graceful close)
  │◀─── shutdown response ────────────│
  │                                    │
```

---

## MCP vs Alternatives

| Feature | MCP | OpenAI Function Calling | LangChain Tools | Custom REST APIs |
|---------|-----|------------------------|-----------------|-----------------|
| Standardized protocol | ✅ | ❌ (proprietary) | ❌ (framework-specific) | ❌ |
| Model-agnostic | ✅ | ❌ (OpenAI only) | ✅ | ✅ |
| Bidirectional (sampling) | ✅ | ❌ | ❌ | ❌ |
| Resource discovery | ✅ | ❌ | ❌ | ❌ |
| Prompt templates | ✅ | ❌ | ❌ | ❌ |
| Multi-client support | ✅ | ❌ | ❌ | ✅ |
| Stateful sessions | ✅ | ❌ | ❌ | Depends |
| Ecosystem/registry | ✅ Growing | ✅ Mature | ✅ Mature | N/A |

---

## When to Use MCP

### ✅ Good Fit
- Building tools that multiple AI clients should access
- Need standardized interface for databases, APIs, services
- Want to expose internal tools to Claude/Cursor/VS Code
- Building AI-powered IDE features
- Need bidirectional communication (sampling)
- Want reusable, shareable tool packages

### ❌ Not Ideal
- Simple one-off function calling (use native tool calling)
- Real-time streaming data (use WebSocket)
- High-frequency trading/sub-ms latency (too much overhead)
- Purely stateless operations (REST API is simpler)

---

## Key Design Principles

1. **Servers should be simple** — Focused, lightweight services doing one thing well
2. **Security by default** — Explicit capability negotiation, no ambient authority
3. **Progressive enhancement** — Start simple, add capabilities incrementally
4. **Transport agnostic** — Protocol works over stdio, HTTP, or custom transports
5. **Human in the loop** — Users approve sensitive operations

---

## Exercises

1. Install Claude Desktop and configure a built-in MCP server (filesystem or SQLite)
2. Run the MCP Inspector against a sample server and explore capabilities
3. Map your organization's internal tools — which would benefit from MCP?
4. Compare MCP's architecture to OpenAI's function calling — list 5 differences
5. Design (on paper) an MCP server for a service you use daily
