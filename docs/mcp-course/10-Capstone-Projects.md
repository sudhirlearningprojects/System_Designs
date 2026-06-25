# Module 10: Capstone Projects

## Project 1: Full-Stack DevOps MCP Server

Build a unified DevOps assistant combining Git, CI/CD, Kubernetes, and monitoring.

### Requirements
- Git operations (status, diff, log, blame)
- GitHub Actions management (list runs, view logs, re-run)
- Kubernetes operations (pods, logs, scale, describe)
- Prometheus metrics querying
- Incident response prompt templates

### Skeleton
```python
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("devops-server")

# Tools: git_status, git_diff, get_pods, get_logs, scale_deployment,
#         list_ci_runs, query_metrics, get_alerts
# Resources: k8s://cluster/info, git://branches, monitoring://dashboards
# Prompts: incident_response, deploy_checklist, troubleshoot_pod
```

---

## Project 2: Multi-Tenant SaaS Data Server

Build an MCP server that provides secure, tenant-isolated database access.

### Requirements
- Multi-tenant with row-level security
- Schema exploration per tenant
- Safe query execution (read-only)
- Query result caching
- Audit logging of all operations

### Skeleton
```python
mcp = FastMCP("saas-data-server")

@mcp.tool()
async def query(sql: str, tenant_id: str) -> str:
    """Execute query scoped to tenant."""
    # Inject tenant filter into all queries
    safe_sql = inject_tenant_filter(sql, tenant_id)
    # Validate, execute, cache, audit log
    ...
```

---

## Project 3: AI-Powered Code Review Server

Build an MCP server that provides intelligent code analysis.

### Requirements
- Parse code files (AST analysis)
- Detect security vulnerabilities
- Suggest performance improvements
- Generate unit test templates
- Track code complexity metrics

### Key Tools
```python
@mcp.tool()
def analyze_file(path: str) -> str:
    """Static analysis: complexity, security, patterns."""

@mcp.tool()
def suggest_tests(path: str) -> str:
    """Generate test cases for functions in file."""

@mcp.tool()
def find_vulnerabilities(path: str) -> str:
    """Scan for common security issues (OWASP Top 10)."""

@mcp.resource("code://metrics/{path}")
def complexity_metrics(path: str) -> str:
    """Cyclomatic complexity and code quality metrics."""
```

---

## Project 4: Knowledge Management Server

Build an MCP server for organizational knowledge (wiki + RAG + memory).

### Requirements
- Ingest documents (PDF, Markdown, HTML)
- Semantic search across knowledge base
- Persistent memory (remember facts across sessions)
- Tag-based organization
- Access control per document

### Key Tools
```python
@mcp.tool()
def search(query: str, tags: list[str] = []) -> str:
    """Semantic search with optional tag filtering."""

@mcp.tool()
def remember(fact: str, tags: list[str] = []) -> str:
    """Store a fact in persistent memory."""

@mcp.tool()
def recall(topic: str) -> str:
    """Recall stored facts about a topic."""

@mcp.tool()
def ingest(url: str) -> str:
    """Ingest a document from URL into the knowledge base."""
```

---

## Project 5: Remote MCP Server Platform

Build a platform that hosts multiple MCP servers with auth, monitoring, and a registry.

### Requirements
- User authentication (OAuth 2.1)
- Server registry (discover available servers)
- Usage metrics and billing
- Rate limiting per user/tier
- Admin dashboard

### Architecture
```
Client → API Gateway (Auth + Rate Limit) → MCP Router → Server Pool
                                                    ↓
                                              Metrics + Audit Log
```

---

## Evaluation Criteria

| Criterion | Weight |
|-----------|--------|
| Working tools with proper error handling | 30% |
| Security (input validation, access control) | 20% |
| Testing (unit + integration tests) | 20% |
| Documentation (README, tool descriptions) | 15% |
| Production readiness (Docker, monitoring) | 15% |

---

## Submission Checklist

- [ ] Working server with `mcp dev` and Claude Desktop
- [ ] Unit tests (>80% coverage)
- [ ] Dockerfile for containerized deployment
- [ ] README with setup instructions and tool documentation
- [ ] Security: input validation, sandboxing, no credential leaks
- [ ] At least 3 tools, 2 resources, 1 prompt
