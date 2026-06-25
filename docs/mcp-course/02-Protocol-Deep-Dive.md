# Module 2: Protocol Deep Dive

## JSON-RPC 2.0 Foundation

All MCP communication uses JSON-RPC 2.0 messages:

### Request
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "query_database",
    "arguments": {"sql": "SELECT * FROM users LIMIT 10"}
  }
}
```

### Response (Success)
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {"type": "text", "text": "[{\"id\": 1, \"name\": \"Alice\"}, ...]"}
    ]
  }
}
```

### Response (Error)
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32602,
    "message": "Invalid SQL: only SELECT queries allowed",
    "data": {"sql": "DROP TABLE users"}
  }
}
```

### Notification (No Response Expected)
```json
{
  "jsonrpc": "2.0",
  "method": "notifications/resources/updated",
  "params": {"uri": "file:///data/config.json"}
}
```

---

## Connection Lifecycle

### Phase 1: Initialization

```
Client → Server: initialize
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {
      "roots": {"listChanged": true},
      "sampling": {}
    },
    "clientInfo": {
      "name": "claude-desktop",
      "version": "1.5.0"
    }
  }
}

Server → Client: initialize response
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2025-03-26",
    "capabilities": {
      "tools": {"listChanged": true},
      "resources": {"subscribe": true, "listChanged": true},
      "prompts": {"listChanged": true},
      "logging": {}
    },
    "serverInfo": {
      "name": "my-database-server",
      "version": "2.1.0"
    }
  }
}

Client → Server: initialized (notification)
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}
```

### Phase 2: Operation

After initialization, the client can:
- List and call tools
- List and read resources
- List and use prompts
- Subscribe to resource updates
- Receive notifications

### Phase 3: Shutdown

```
Client → Server: close connection (transport-specific)
```

---

## Capabilities Negotiation

Capabilities are declared during initialization. Only declared capabilities are available.

### Client Capabilities
```json
{
  "roots": {"listChanged": true},     // Client can provide file system roots
  "sampling": {}                        // Client supports LLM sampling requests
}
```

### Server Capabilities
```json
{
  "tools": {"listChanged": true},       // Server provides tools
  "resources": {
    "subscribe": true,                   // Supports resource subscriptions
    "listChanged": true                  // Notifies when resource list changes
  },
  "prompts": {"listChanged": true},     // Server provides prompts
  "logging": {}                          // Server supports logging
}
```

---

## Tool Protocol

### List Tools
```json
// Request
{"jsonrpc": "2.0", "id": 2, "method": "tools/list"}

// Response
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "get_weather",
        "description": "Get current weather for a city",
        "inputSchema": {
          "type": "object",
          "properties": {
            "city": {"type": "string", "description": "City name"},
            "units": {"type": "string", "enum": ["celsius", "fahrenheit"], "default": "celsius"}
          },
          "required": ["city"]
        }
      }
    ]
  }
}
```

### Call Tool
```json
// Request
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": {"city": "San Francisco", "units": "celsius"}
  }
}

// Response
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {"type": "text", "text": "San Francisco: 18°C, partly cloudy, humidity 65%"}
    ],
    "isError": false
  }
}
```

### Tool Result Content Types
```json
// Text content
{"type": "text", "text": "Result string"}

// Image content
{"type": "image", "data": "base64...", "mimeType": "image/png"}

// Embedded resource
{"type": "resource", "resource": {"uri": "file:///output.csv", "text": "..."}}
```

---

## Resource Protocol

### List Resources
```json
// Request
{"jsonrpc": "2.0", "id": 4, "method": "resources/list"}

// Response
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "resources": [
      {
        "uri": "postgres://localhost/mydb/schema",
        "name": "Database Schema",
        "description": "Current database table definitions",
        "mimeType": "application/json"
      },
      {
        "uri": "config://app/settings",
        "name": "Application Settings",
        "mimeType": "application/json"
      }
    ]
  }
}
```

### Read Resource
```json
// Request
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "resources/read",
  "params": {"uri": "postgres://localhost/mydb/schema"}
}

// Response
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "contents": [
      {
        "uri": "postgres://localhost/mydb/schema",
        "mimeType": "application/json",
        "text": "{\"tables\": [{\"name\": \"users\", \"columns\": [...]}]}"
      }
    ]
  }
}
```

### Resource Templates (Dynamic URIs)
```json
{
  "uriTemplate": "file:///{path}",
  "name": "Project Files",
  "description": "Read any file in the project directory"
}
```

### Resource Subscriptions
```json
// Subscribe to changes
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "resources/subscribe",
  "params": {"uri": "config://app/settings"}
}

// Server notifies on change
{
  "jsonrpc": "2.0",
  "method": "notifications/resources/updated",
  "params": {"uri": "config://app/settings"}
}
```

---

## Prompt Protocol

### List Prompts
```json
// Response
{
  "result": {
    "prompts": [
      {
        "name": "debug_error",
        "description": "Help debug an error message",
        "arguments": [
          {"name": "error_message", "description": "The error to debug", "required": true},
          {"name": "language", "description": "Programming language", "required": false}
        ]
      }
    ]
  }
}
```

### Get Prompt
```json
// Request
{
  "method": "prompts/get",
  "params": {
    "name": "debug_error",
    "arguments": {"error_message": "NullPointerException at line 42", "language": "java"}
  }
}

// Response
{
  "result": {
    "messages": [
      {
        "role": "user",
        "content": {
          "type": "text",
          "text": "I'm getting this error in Java:\n\nNullPointerException at line 42\n\nPlease help me debug it. Explain the cause and suggest a fix."
        }
      }
    ]
  }
}
```

---

## Sampling (Server → Client LLM Request)

Allows the server to request the client's LLM to generate text:

```json
// Server → Client: Request LLM completion
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "sampling/createMessage",
  "params": {
    "messages": [
      {"role": "user", "content": {"type": "text", "text": "Summarize this data: [...]"}}
    ],
    "modelPreferences": {
      "hints": [{"name": "claude-sonnet-4-20250514"}],
      "costPriority": 0.5,
      "speedPriority": 0.8
    },
    "maxTokens": 500
  }
}

// Client → Server: LLM response
{
  "jsonrpc": "2.0",
  "id": 7,
  "result": {
    "role": "assistant",
    "content": {"type": "text", "text": "The data shows..."},
    "model": "claude-sonnet-4-20250514"
  }
}
```

**Use cases**: Server needs AI reasoning during tool execution (e.g., classifying data before returning results).

---

## Transport Layers

### stdio (Local)
```
Host Process
  └── spawns child process (MCP Server)
       └── communicates via stdin/stdout
           └── one JSON-RPC message per line
```

**Best for**: IDE integrations, local tools, Claude Desktop

### Streamable HTTP (Remote, 2025+)
```
Client ─── POST /mcp ───▶ Server
       ◀── SSE stream ─── Server (for server-initiated messages)
```

**Best for**: Remote/cloud servers, multi-tenant deployments, web applications

### SSE (Legacy Remote)
```
Client ─── GET /sse ────▶ Server (establish SSE stream)
       ─── POST /message ▶ Server (send requests)
       ◀── SSE events ─── Server (receive responses)
```

**Status**: Deprecated in favor of Streamable HTTP

---

## Error Handling

### Standard JSON-RPC Error Codes
| Code | Meaning |
|------|---------|
| -32700 | Parse error |
| -32600 | Invalid request |
| -32601 | Method not found |
| -32602 | Invalid params |
| -32603 | Internal error |

### MCP-Specific Error Codes
| Code | Meaning |
|------|---------|
| -32001 | Resource not found |
| -32002 | Tool execution failed |
| -32003 | Permission denied |

---

## Progress Reporting

For long-running tool calls:

```json
// Server sends progress notifications during tool execution
{
  "jsonrpc": "2.0",
  "method": "notifications/progress",
  "params": {
    "progressToken": "task-123",
    "progress": 45,
    "total": 100,
    "message": "Processing row 45 of 100..."
  }
}
```

---

## Logging

```json
// Server sends log messages to client
{
  "jsonrpc": "2.0",
  "method": "notifications/message",
  "params": {
    "level": "info",  // debug, info, warning, error, critical
    "logger": "database",
    "data": "Connected to PostgreSQL on port 5432"
  }
}
```

---

## Exercises

1. Write a JSON-RPC initialization handshake by hand (both sides)
2. Trace the full message flow for a tool call using MCP Inspector
3. Design the capability set for a Kubernetes MCP server
4. Implement a minimal JSON-RPC message parser in your language of choice
5. Compare stdio vs Streamable HTTP — when would you choose each?
