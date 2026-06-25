# Module 4: Building MCP Servers (TypeScript)

## Setup

```bash
npm init -y
npm install @modelcontextprotocol/sdk zod
npm install -D typescript @types/node tsx
```

```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "Node16",
    "moduleResolution": "Node16",
    "outDir": "./dist",
    "strict": true,
    "esModuleInterop": true
  }
}
```

---

## Server Creation

```typescript
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const server = new McpServer({
  name: "my-server",
  version: "1.0.0",
  description: "A TypeScript MCP server",
});
```

---

## Tools

### Basic Tool
```typescript
server.tool(
  "add",
  "Add two numbers together",
  { a: z.number(), b: z.number() },
  async ({ a, b }) => ({
    content: [{ type: "text", text: String(a + b) }],
  })
);
```

### Tool with Complex Schema
```typescript
server.tool(
  "search_issues",
  "Search GitHub issues by query and state",
  {
    repo: z.string().describe("Repository in owner/name format"),
    query: z.string().describe("Search query"),
    state: z.enum(["open", "closed", "all"]).default("open"),
    limit: z.number().min(1).max(100).default(10),
    labels: z.array(z.string()).optional(),
  },
  async ({ repo, query, state, limit, labels }) => {
    const [owner, name] = repo.split("/");
    
    const response = await fetch(
      `https://api.github.com/repos/${owner}/${name}/issues?` +
      `state=${state}&per_page=${limit}&q=${encodeURIComponent(query)}` +
      (labels ? `&labels=${labels.join(",")}` : ""),
      { headers: { Authorization: `token ${process.env.GITHUB_TOKEN}` } }
    );
    
    if (!response.ok) {
      return {
        content: [{ type: "text", text: `GitHub API error: ${response.statusText}` }],
        isError: true,
      };
    }
    
    const issues = await response.json();
    const formatted = issues.map((i: any) => 
      `#${i.number} [${i.state}] ${i.title}\n  ${i.html_url}`
    ).join("\n\n");
    
    return { content: [{ type: "text", text: formatted || "No issues found" }] };
  }
);
```

### Async Tool with Error Handling
```typescript
server.tool(
  "execute_query",
  "Execute a read-only SQL query",
  {
    sql: z.string().describe("SQL SELECT query"),
    database: z.string().default("default"),
  },
  async ({ sql, database }) => {
    // Validate query
    const normalized = sql.trim().toUpperCase();
    if (!normalized.startsWith("SELECT")) {
      return {
        content: [{ type: "text", text: "Error: Only SELECT queries are allowed" }],
        isError: true,
      };
    }
    
    try {
      const pool = getPool(database);
      const result = await pool.query(sql);
      return {
        content: [{ type: "text", text: JSON.stringify(result.rows, null, 2) }],
      };
    } catch (error) {
      return {
        content: [{ type: "text", text: `Query error: ${(error as Error).message}` }],
        isError: true,
      };
    }
  }
);
```

---

## Resources

### Static Resource
```typescript
server.resource(
  "config",
  "config://app/settings",
  "Current application configuration",
  "application/json",
  async () => ({
    contents: [{
      uri: "config://app/settings",
      mimeType: "application/json",
      text: JSON.stringify(await loadConfig(), null, 2),
    }],
  })
);
```

### Dynamic Resource Template
```typescript
server.resource(
  "file",
  "file:///{path}",
  "Read project files",
  "text/plain",
  async (uri) => {
    const path = new URL(uri).pathname;
    const fullPath = resolve(PROJECT_ROOT, path);
    
    // Security check
    if (!fullPath.startsWith(resolve(PROJECT_ROOT))) {
      throw new Error("Access denied: path outside project");
    }
    
    const content = await readFile(fullPath, "utf-8");
    const mimeType = getMimeType(path);
    
    return {
      contents: [{ uri, mimeType, text: content }],
    };
  }
);
```

### Resource with List
```typescript
// Low-level API for dynamic resource listing
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { ListResourcesRequestSchema, ReadResourceRequestSchema } from "@modelcontextprotocol/sdk/types.js";

const server = new Server({ name: "dynamic-server", version: "1.0.0" }, {
  capabilities: { resources: { listChanged: true } }
});

server.setRequestHandler(ListResourcesRequestSchema, async () => {
  const files = await glob("**/*.md", { cwd: PROJECT_ROOT });
  return {
    resources: files.map(file => ({
      uri: `file:///${file}`,
      name: file,
      mimeType: "text/markdown",
    })),
  };
});

server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const path = new URL(request.params.uri).pathname;
  const content = await readFile(join(PROJECT_ROOT, path), "utf-8");
  return {
    contents: [{ uri: request.params.uri, mimeType: "text/markdown", text: content }],
  };
});
```

---

## Prompts

```typescript
server.prompt(
  "code_review",
  "Generate a thorough code review",
  { code: z.string(), language: z.string().default("typescript") },
  ({ code, language }) => ({
    messages: [
      {
        role: "user",
        content: {
          type: "text",
          text: `Review this ${language} code for bugs, security, and best practices:\n\n\`\`\`${language}\n${code}\n\`\`\``,
        },
      },
    ],
  })
);

server.prompt(
  "explain_error",
  "Explain an error and suggest fixes",
  {
    error: z.string(),
    stackTrace: z.string().optional(),
    context: z.string().optional(),
  },
  ({ error, stackTrace, context }) => ({
    messages: [
      ...(context ? [{
        role: "user" as const,
        content: { type: "text" as const, text: `Context:\n${context}` },
      }] : []),
      {
        role: "user" as const,
        content: {
          type: "text" as const,
          text: `Error: ${error}${stackTrace ? `\n\nStack trace:\n${stackTrace}` : ""}\n\nExplain the cause and suggest a fix.`,
        },
      },
    ],
  })
);
```

---

## Complete Example: Kubernetes MCP Server

```typescript
// k8s-server.ts
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);
const server = new McpServer({ name: "kubernetes-server", version: "1.0.0" });

async function kubectl(...args: string[]): Promise<string> {
  const { stdout, stderr } = await execAsync(`kubectl ${args.join(" ")}`, { timeout: 30000 });
  if (stderr && !stdout) throw new Error(stderr);
  return stdout.trim();
}

// === TOOLS ===

server.tool(
  "get_pods",
  "List pods in a namespace",
  {
    namespace: z.string().default("default"),
    labelSelector: z.string().optional(),
  },
  async ({ namespace, labelSelector }) => {
    const args = ["get", "pods", "-n", namespace, "-o", "json"];
    if (labelSelector) args.push("-l", labelSelector);
    const output = await kubectl(...args);
    const pods = JSON.parse(output);
    
    const summary = pods.items.map((p: any) => ({
      name: p.metadata.name,
      status: p.status.phase,
      restarts: p.status.containerStatuses?.[0]?.restartCount ?? 0,
      age: p.metadata.creationTimestamp,
    }));
    
    return { content: [{ type: "text", text: JSON.stringify(summary, null, 2) }] };
  }
);

server.tool(
  "get_logs",
  "Get logs from a pod",
  {
    pod: z.string(),
    namespace: z.string().default("default"),
    container: z.string().optional(),
    tail: z.number().default(100),
    since: z.string().default("1h"),
  },
  async ({ pod, namespace, container, tail, since }) => {
    const args = ["logs", pod, "-n", namespace, `--tail=${tail}`, `--since=${since}`];
    if (container) args.push("-c", container);
    const logs = await kubectl(...args);
    return { content: [{ type: "text", text: logs }] };
  }
);

server.tool(
  "describe_resource",
  "Describe a Kubernetes resource",
  {
    kind: z.enum(["pod", "deployment", "service", "ingress", "configmap", "secret", "node"]),
    name: z.string(),
    namespace: z.string().default("default"),
  },
  async ({ kind, name, namespace }) => {
    const output = await kubectl("describe", kind, name, "-n", namespace);
    return { content: [{ type: "text", text: output }] };
  }
);

server.tool(
  "scale_deployment",
  "Scale a deployment to N replicas (requires confirmation)",
  {
    deployment: z.string(),
    replicas: z.number().min(0).max(50),
    namespace: z.string().default("default"),
  },
  async ({ deployment, replicas, namespace }) => {
    const output = await kubectl("scale", "deployment", deployment, 
      `--replicas=${replicas}`, "-n", namespace);
    return { content: [{ type: "text", text: output }] };
  }
);

// === RESOURCES ===

server.resource(
  "cluster-info",
  "k8s://cluster/info",
  "Kubernetes cluster information",
  "application/json",
  async () => {
    const info = await kubectl("cluster-info", "--output=json");
    return { contents: [{ uri: "k8s://cluster/info", mimeType: "application/json", text: info }] };
  }
);

server.resource(
  "namespaces",
  "k8s://namespaces",
  "List all namespaces",
  "application/json",
  async () => {
    const ns = await kubectl("get", "namespaces", "-o", "json");
    return { contents: [{ uri: "k8s://namespaces", mimeType: "application/json", text: ns }] };
  }
);

// === PROMPTS ===

server.prompt(
  "troubleshoot_pod",
  "Troubleshoot a failing pod",
  { pod: z.string(), namespace: z.string().default("default") },
  ({ pod, namespace }) => ({
    messages: [{
      role: "user",
      content: {
        type: "text",
        text: `The pod "${pod}" in namespace "${namespace}" is having issues. ` +
              `Please use the available tools to:\n` +
              `1. Check pod status and events\n` +
              `2. Review recent logs\n` +
              `3. Diagnose the issue\n` +
              `4. Suggest a fix`,
      },
    }],
  })
);

// === START SERVER ===

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Kubernetes MCP server running on stdio");
}

main().catch(console.error);
```

---

## Running TypeScript Servers

```bash
# Development
npx tsx k8s-server.ts

# Build and run
npx tsc && node dist/k8s-server.js

# Claude Desktop config
# ~/Library/Application Support/Claude/claude_desktop_config.json
```
```json
{
  "mcpServers": {
    "kubernetes": {
      "command": "npx",
      "args": ["tsx", "/path/to/k8s-server.ts"],
      "env": {
        "KUBECONFIG": "/Users/me/.kube/config"
      }
    }
  }
}
```

---

## Exercises

1. Build a TypeScript MCP server wrapping the GitHub API (issues, PRs, repos)
2. Create a Docker MCP server (list containers, view logs, restart)
3. Implement a Jira/Linear integration with search, create, and update tools
4. Add resource subscriptions that notify when K8s pod status changes
5. Build a multi-tool server combining 3+ external APIs
