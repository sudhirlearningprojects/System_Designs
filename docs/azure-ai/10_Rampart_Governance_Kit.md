# Microsoft Rampart & Agent Governance Kit

## Overview

These are Microsoft's enterprise tools for **securing and governing AI agents** in production:

- **Rampart**: Runtime safety layer that sits between your agent and users — enforces guardrails, detects attacks, and ensures compliance in real-time
- **Agent Governance Kit**: Framework for managing the lifecycle of AI agents — registration, approval workflows, monitoring, auditing, and decommissioning

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI AGENT GOVERNANCE STACK                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  RUNTIME SAFETY (Rampart)                                        │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Input Shield │ Output Shield │ Tool Guard │ Data Guard   │    │
│  │ (injection)    (hallucination)  (auth)      (PII/secrets)│    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  GOVERNANCE (Agent Governance Kit)                                │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Registry │ Approval │ Policies │ Audit │ Monitoring      │    │
│  │ (catalog)  (workflow)  (rules)    (logs)   (compliance)  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  INFRASTRUCTURE                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Azure OpenAI │ AI Search │ Entra ID │ Key Vault │ Monitor│    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

# Part 1: Microsoft Rampart (AI Safety Runtime)

## Theory: Why a Safety Runtime?

```
Problem: Safety built INTO the agent is fragile.
  - Prompt-based safety can be bypassed (jailbreaks)
  - Model updates can silently change safety behavior
  - Different agents need different safety policies
  - No centralized visibility into safety events

Solution: Safety as an EXTERNAL runtime layer.
  - Sits between user and agent (like a firewall)
  - Enforces policies regardless of agent implementation
  - Centralized logging and alerting
  - Can be updated without redeploying agents
  - Works with ANY LLM/agent framework

Analogy:
  Agent safety in prompt = Security in application code (fragile)
  Rampart = WAF/firewall (defense-in-depth, centralized)
```

## Architecture

```
┌──────────┐     ┌──────────────────────────────┐     ┌──────────┐
│  User    │────►│         RAMPART               │────►│  Agent   │
│  Input   │     │                                │     │  (LLM)   │
└──────────┘     │  ┌────────────────────────┐   │     └──────────┘
                 │  │ INPUT SHIELDS           │   │          │
                 │  │ • Prompt injection      │   │          │
                 │  │ • Jailbreak detection   │   │          │
                 │  │ • PII detection         │   │          │
                 │  │ • Topic blocking        │   │          │
                 │  │ • Rate limiting         │   │          │
                 │  └────────────────────────┘   │          │
                 │                                │          │
                 │  ┌────────────────────────┐   │          │
                 │  │ OUTPUT SHIELDS          │◄──┼──────────┘
                 │  │ • Hallucination check   │   │
                 │  │ • PII/secret leakage    │   │
                 │  │ • Brand compliance      │   │
                 │  │ • Toxicity filtering    │   │
                 │  │ • Commitment detection  │   │
                 │  └────────────────────────┘   │
                 │                                │
                 │  ┌────────────────────────┐   │
                 │  │ TOOL GUARDS            │   │
                 │  │ • Authorization check   │   │
                 │  │ • Parameter validation  │   │
                 │  │ • Rate limiting per tool│   │
                 │  │ • Audit logging         │   │
                 │  └────────────────────────┘   │
                 │                                │
                 │  ┌────────────────────────┐   │
                 │  │ DATA GUARDS            │   │
                 │  │ • Context filtering     │   │
                 │  │ • Document access control│  │
                 │  │ • PII redaction in RAG  │   │
                 │  └────────────────────────┘   │
                 └──────────────────────────────┘
```

## Setup & Configuration

### Install

```bash
# pip install ms-rampart (hypothetical — use actual package name when available)
# Or deploy as Azure Container App / sidecar

# Azure CLI deployment
az containerapp create \
  --name rampart-gateway \
  --resource-group rg-ai \
  --image mcr.microsoft.com/rampart:latest \
  --target-port 8080 \
  --env-vars \
    AZURE_OPENAI_ENDPOINT=https://my-aoai.openai.azure.com/ \
    CONTENT_SAFETY_ENDPOINT=https://my-safety.cognitiveservices.azure.com/ \
    POLICY_CONFIG_PATH=/config/policies.yaml
```

### Policy Configuration

```yaml
# rampart-policies.yaml
version: "1.0"
name: "production-support-agent"

input_shields:
  prompt_injection:
    enabled: true
    action: block  # block | warn | log
    sensitivity: high  # low | medium | high
    
  jailbreak_detection:
    enabled: true
    action: block
    # Uses Azure AI Content Safety Prompt Shields under the hood
    
  pii_detection:
    enabled: true
    action: redact  # block | redact | warn | log
    categories:
      - email
      - phone
      - ssn
      - credit_card
    # Redacts PII before it reaches the agent
    
  topic_blocking:
    enabled: true
    action: block
    blocked_topics:
      - name: "financial_advice"
        description: "Specific investment or financial planning advice"
      - name: "competitor_discussion"
        description: "Discussing competitor products or pricing"
      - name: "internal_information"
        description: "Requests for internal company data or roadmap"
    
  rate_limiting:
    enabled: true
    limits:
      - scope: per_user
        requests: 60
        window_seconds: 60
      - scope: per_session
        requests: 200
        window_seconds: 3600

output_shields:
  hallucination_check:
    enabled: true
    action: block  # block response if not grounded
    grounding_threshold: 0.7  # Minimum grounding score
    # Requires context/sources to be passed for verification
    
  pii_leakage:
    enabled: true
    action: redact
    # Scans agent output for PII that shouldn't be exposed
    
  toxicity:
    enabled: true
    action: block
    categories:
      hate: medium  # block medium and above
      violence: medium
      sexual: high
      self_harm: low  # block everything
      
  brand_compliance:
    enabled: true
    action: warn
    rules:
      - "Never mention competitor products by name"
      - "Never make promises about timelines"
      - "Always use approved company terminology"
      
  unauthorized_commitments:
    enabled: true
    action: block
    patterns:
      - "I (will|guarantee|promise) .* (refund|credit|discount)"
      - "within \\d+ (hours|days|minutes)"
      - "I can (delete|remove|cancel) your"

tool_guards:
  authorization:
    enabled: true
    rules:
      - tool: "cancel_subscription"
        requires: ["user_verified", "confirmation_received"]
      - tool: "process_refund"
        requires: ["user_verified", "amount_under_limit"]
        max_amount: 100.00
      - tool: "delete_account"
        requires: ["user_verified", "manager_approval"]
        
  parameter_validation:
    enabled: true
    rules:
      - tool: "query_database"
        blocked_patterns: ["DROP", "DELETE", "UPDATE", "INSERT"]
      - tool: "send_email"
        allowed_domains: ["@company.com"]

data_guards:
  context_filtering:
    enabled: true
    # Filter retrieved documents before they reach the agent
    rules:
      - remove_if_contains: ["INTERNAL ONLY", "CONFIDENTIAL"]
      - redact_patterns: ["\\b\\d{3}-\\d{2}-\\d{4}\\b"]  # SSN in docs
      
  access_control:
    enabled: true
    # Only retrieve documents the user has permission to see
    enforce_document_acl: true
    user_attribute: "department"

logging:
  level: all  # all | violations_only | none
  destination: azure_monitor  # azure_monitor | log_analytics | custom
  include_content: false  # Don't log actual messages (privacy)
  include_metadata: true  # Log: shield triggered, action taken, latency

alerts:
  - condition: "input_shields.prompt_injection.count > 10 in 5m"
    action: notify
    channel: "security-team-slack"
  - condition: "output_shields.hallucination_check.block_rate > 0.1"
    action: notify
    channel: "ai-quality-team"
```

## SDK Integration

```python
from rampart import RampartClient, ShieldResult

# Initialize Rampart client
rampart = RampartClient(
    endpoint="https://rampart-gateway.azurecontainerapps.io",
    policy="production-support-agent",
    api_key="...",  # Or use Managed Identity
)

# ============ FULL AGENT LOOP WITH RAMPART ============

async def handle_user_message(user_input: str, user_context: dict) -> str:
    
    # 1. INPUT SHIELD — Check user input before processing
    input_result: ShieldResult = await rampart.check_input(
        text=user_input,
        user_id=user_context["user_id"],
        session_id=user_context["session_id"],
        metadata={"channel": "web", "user_tier": "pro"},
    )
    
    if input_result.blocked:
        return input_result.safe_response  # Pre-configured safe response
        # e.g., "I can't help with that request. Is there something else I can assist with?"
    
    # Input may have been modified (PII redacted)
    sanitized_input = input_result.sanitized_text or user_input
    
    # 2. RETRIEVE CONTEXT (with data guard)
    retrieved_docs = await search_knowledge_base(sanitized_input)
    
    # Data guard filters documents
    filtered_docs = await rampart.filter_context(
        documents=retrieved_docs,
        user_permissions=user_context["permissions"],
    )
    
    # 3. CALL AGENT (your existing agent logic)
    agent_response = await my_agent.generate(
        query=sanitized_input,
        context=filtered_docs,
    )
    
    # 4. TOOL GUARD — If agent wants to call tools
    if agent_response.tool_calls:
        for tool_call in agent_response.tool_calls:
            tool_result = await rampart.check_tool_call(
                tool_name=tool_call.name,
                parameters=tool_call.args,
                user_context=user_context,
            )
            if tool_result.blocked:
                # Tool not authorized — tell agent to try different approach
                agent_response = await my_agent.generate(
                    query=sanitized_input,
                    context=filtered_docs,
                    system_override="The tool call was blocked. Explain to the user why and offer alternatives.",
                )
                break
    
    # 5. OUTPUT SHIELD — Check agent response before sending to user
    output_result = await rampart.check_output(
        text=agent_response.text,
        context=filtered_docs,  # For grounding check
        query=sanitized_input,  # For relevance check
    )
    
    if output_result.blocked:
        # Response failed safety check
        if output_result.reason == "hallucination":
            return "I'm not confident in my answer. Let me connect you with a specialist."
        elif output_result.reason == "pii_leakage":
            return output_result.redacted_text  # PII removed version
        else:
            return "I encountered an issue generating a response. Please try rephrasing."
    
    # 6. Return safe response
    return output_result.final_text  # May have minor modifications (PII redacted)


# ============ MONITORING ============

# Get safety metrics
metrics = await rampart.get_metrics(
    time_range="last_24h",
    group_by="shield_type",
)
print(f"Input blocks: {metrics['input_shields']['total_blocks']}")
print(f"Output blocks: {metrics['output_shields']['total_blocks']}")
print(f"Hallucination rate: {metrics['output_shields']['hallucination_check']['block_rate']:.2%}")
print(f"Injection attempts: {metrics['input_shields']['prompt_injection']['detections']}")
```

## Monitoring Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  RAMPART SAFETY DASHBOARD                                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  INPUT SHIELDS (last 24h)                                    │
│  ├── Requests processed: 45,230                             │
│  ├── Prompt injection blocked: 127 (0.28%)                  │
│  ├── Jailbreak attempts: 43 (0.10%)                         │
│  ├── PII redacted: 892 (1.97%)                              │
│  ├── Topic blocked: 234 (0.52%)                             │
│  └── Rate limited: 56 (0.12%)                               │
│                                                              │
│  OUTPUT SHIELDS (last 24h)                                   │
│  ├── Responses checked: 44,100                              │
│  ├── Hallucination blocked: 312 (0.71%)                     │
│  ├── PII leakage prevented: 23 (0.05%)                      │
│  ├── Toxicity blocked: 8 (0.02%)                            │
│  └── Commitment blocked: 67 (0.15%)                         │
│                                                              │
│  TOOL GUARDS (last 24h)                                      │
│  ├── Tool calls checked: 12,450                             │
│  ├── Unauthorized blocked: 34 (0.27%)                       │
│  └── Parameter violations: 12 (0.10%)                       │
│                                                              │
│  ⚠️ ALERTS                                                  │
│  • [WARN] Hallucination rate trending up (0.71% → 0.9%)    │
│  • [INFO] 3 new jailbreak patterns detected                 │
└─────────────────────────────────────────────────────────────┘
```

---

# Part 2: Microsoft Agent Governance Kit

## Theory: Why Agent Governance?

```
As organizations deploy dozens of AI agents, they face:

1. VISIBILITY: "How many agents do we have? What can they do?"
2. CONTROL: "Who approved this agent? What data can it access?"
3. COMPLIANCE: "Does this agent meet our security/privacy policies?"
4. LIFECYCLE: "How do we update, version, and retire agents?"
5. ACCOUNTABILITY: "When something goes wrong, who is responsible?"

Agent Governance Kit provides:
  - Central registry of all agents
  - Approval workflows before deployment
  - Policy enforcement (what agents can/can't do)
  - Audit trail (every action logged)
  - Monitoring and compliance reporting
```

## Core Concepts

```
┌─────────────────────────────────────────────────────────────┐
│              AGENT GOVERNANCE LIFECYCLE                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. REGISTER    → Agent added to central catalog            │
│  2. REVIEW      → Security/compliance team reviews          │
│  3. APPROVE     → Meets policies → approved for deployment  │
│  4. DEPLOY      → Deployed with Rampart safety layer        │
│  5. MONITOR     → Continuous compliance monitoring          │
│  6. UPDATE      → Version changes go through review again   │
│  7. RETIRE      → Decommission with data cleanup            │
│                                                              │
│  At each stage: audit log, notifications, policy checks     │
└─────────────────────────────────────────────────────────────┘
```

## Agent Registry

```yaml
# agent-registration.yaml
apiVersion: governance.microsoft.com/v1
kind: AgentRegistration
metadata:
  name: customer-support-agent
  namespace: production
  
spec:
  # Basic info
  displayName: "Customer Support Agent"
  description: "Handles billing, technical, and account queries for Pro subscribers"
  version: "2.1.0"
  owner: "ai-platform-team"
  contact: "ai-team@company.com"
  
  # Classification
  riskLevel: medium  # low | medium | high | critical
  dataClassification: confidential  # public | internal | confidential | restricted
  
  # Capabilities
  capabilities:
    - name: "answer_questions"
      description: "Answer customer questions from knowledge base"
      riskLevel: low
    - name: "view_account"
      description: "View customer account and subscription details"
      riskLevel: low
    - name: "modify_subscription"
      description: "Cancel, upgrade, or downgrade subscriptions"
      riskLevel: medium
    - name: "process_refund"
      description: "Issue refunds up to $100"
      riskLevel: high
      
  # Data access
  dataAccess:
    reads:
      - "customer_accounts"
      - "subscription_database"
      - "knowledge_base_billing"
      - "knowledge_base_technical"
    writes:
      - "subscription_database"
      - "refund_ledger"
    piiAccess: true
    
  # Model configuration
  model:
    provider: "azure_openai"
    model: "gpt-4o"
    deployment: "gpt-4o-production"
    temperature: 0.3
    maxTokens: 2048
    
  # Safety configuration
  safety:
    rampartPolicy: "production-support-agent"
    contentFiltering: "strict"
    humanInTheLoop:
      - action: "process_refund"
        condition: "amount > 50"
      - action: "delete_account"
        condition: "always"
        
  # Compliance
  compliance:
    frameworks:
      - "SOC2"
      - "GDPR"
      - "HIPAA"
    dataRetention: "90_days"
    auditLogging: true
    
  # Deployment
  deployment:
    environment: "production"
    regions: ["eastus", "westeurope"]
    scaling:
      minInstances: 2
      maxInstances: 20
    monitoring:
      sla: "99.9%"
      alerting: true
```

## Approval Workflow

```python
from governance_kit import GovernanceClient, ApprovalRequest

governance = GovernanceClient(endpoint="https://governance.company.com")

# Submit agent for approval
approval = await governance.submit_for_review(
    agent_id="customer-support-agent",
    version="2.1.0",
    changes=[
        "Added refund processing capability",
        "Updated system prompt for better empathy",
        "Increased max refund amount from $50 to $100",
    ],
    artifacts={
        "registration": "agent-registration.yaml",
        "safety_policy": "rampart-policies.yaml",
        "eval_results": "eval-report-v2.1.json",
        "red_team_report": "red-team-v2.1.pdf",
    },
)

print(f"Approval ID: {approval.id}")
print(f"Status: {approval.status}")  # pending_review
print(f"Required approvers: {approval.required_approvers}")
# ['security-team', 'compliance-team', 'product-owner']

# Check approval status
status = await governance.get_approval_status(approval.id)
print(f"Approved by: {status.approved_by}")
print(f"Pending: {status.pending_approvers}")
print(f"Comments: {status.comments}")
```

### Approval Policies

```yaml
# governance-policies.yaml
approval_policies:
  # Low risk agents: auto-approve if eval passes
  - risk_level: low
    auto_approve: true
    conditions:
      - eval_score >= 0.90
      - safety_score >= 0.99
      - no_new_data_access
      
  # Medium risk: requires team lead approval
  - risk_level: medium
    required_approvers:
      - role: "team_lead"
        count: 1
      - role: "security_reviewer"
        count: 1
    conditions:
      - eval_score >= 0.90
      - safety_score >= 0.99
      - red_team_completed: true
      
  # High risk: requires multiple approvals
  - risk_level: high
    required_approvers:
      - role: "team_lead"
        count: 1
      - role: "security_reviewer"
        count: 1
      - role: "compliance_officer"
        count: 1
      - role: "director"
        count: 1
    conditions:
      - eval_score >= 0.95
      - safety_score >= 0.999
      - red_team_completed: true
      - penetration_test_completed: true
      - legal_review_completed: true
      
  # Critical: board-level approval
  - risk_level: critical
    required_approvers:
      - role: "ciso"
      - role: "cto"
      - role: "legal_counsel"
```

## Audit Trail

```python
# Every agent action is logged
audit_log = await governance.get_audit_log(
    agent_id="customer-support-agent",
    time_range="last_7_days",
    event_types=["tool_execution", "data_access", "safety_violation"],
)

for entry in audit_log:
    print(f"[{entry.timestamp}] {entry.event_type}: {entry.description}")
    print(f"  User: {entry.user_id}")
    print(f"  Action: {entry.action}")
    print(f"  Result: {entry.result}")
    print(f"  Risk: {entry.risk_level}")

# Example output:
# [2024-01-15 10:23:45] tool_execution: process_refund
#   User: user-456
#   Action: Refund $75.00 to credit card ending 4532
#   Result: approved (human-in-the-loop confirmed)
#   Risk: high
#
# [2024-01-15 10:25:12] safety_violation: hallucination_detected
#   User: user-789
#   Action: Agent claimed "30-day refund policy" (actual: 14 days)
#   Result: blocked, safe response sent
#   Risk: medium
```

## Compliance Reporting

```python
# Generate compliance report
report = await governance.generate_compliance_report(
    framework="SOC2",
    period="2024-Q1",
    agents=["customer-support-agent", "billing-agent", "technical-agent"],
)

print(f"Compliance Score: {report.overall_score:.1%}")
print(f"Controls Passed: {report.controls_passed}/{report.total_controls}")
print(f"Findings: {len(report.findings)}")

for finding in report.findings:
    print(f"  [{finding.severity}] {finding.control}: {finding.description}")
    print(f"    Remediation: {finding.remediation}")

# Example:
# Compliance Score: 94.2%
# Controls Passed: 49/52
# Findings:
#   [MEDIUM] CC6.1: 3 agents missing encryption-at-rest for conversation logs
#     Remediation: Enable CMEK for Log Analytics workspace
#   [LOW] CC7.2: Audit log retention set to 90 days (policy requires 365)
#     Remediation: Update retention policy in governance config
```

## Policy Enforcement

```python
# Governance Kit continuously checks agents against policies

# Define policy
policy = {
    "name": "data-access-policy",
    "rules": [
        {
            "description": "Agents must not access PII without explicit user consent",
            "check": "pii_access_requires_consent",
            "action": "block_and_alert",
        },
        {
            "description": "All tool calls must be logged",
            "check": "tool_calls_audited",
            "action": "alert_if_missing",
        },
        {
            "description": "Agents must use approved models only",
            "check": "model_in_approved_list",
            "approved_models": ["gpt-4o", "gpt-4o-mini", "claude-sonnet-4"],
            "action": "block",
        },
        {
            "description": "Maximum response length",
            "check": "response_token_limit",
            "max_tokens": 4096,
            "action": "truncate",
        },
    ],
}

# Check agent compliance
compliance_result = await governance.check_compliance(
    agent_id="customer-support-agent",
    policies=["data-access-policy", "safety-policy", "cost-policy"],
)

if not compliance_result.compliant:
    print(f"Non-compliant! Violations:")
    for violation in compliance_result.violations:
        print(f"  - {violation.policy}: {violation.description}")
```

---

## Integration: Rampart + Governance Kit Together

```python
# Production setup: Governance manages lifecycle, Rampart enforces at runtime

class GovernedAgent:
    """Agent with full governance and safety integration."""
    
    def __init__(self, agent_id: str):
        self.agent_id = agent_id
        self.governance = GovernanceClient(...)
        self.rampart = RampartClient(...)
        self.agent = load_agent(agent_id)
    
    async def handle_request(self, user_input: str, user_context: dict) -> str:
        # 1. Governance: Check agent is approved and active
        status = await self.governance.get_agent_status(self.agent_id)
        if status != "active":
            return "This service is temporarily unavailable."
        
        # 2. Rampart: Input safety check
        input_check = await self.rampart.check_input(user_input, user_context)
        if input_check.blocked:
            await self.governance.log_event("input_blocked", input_check.reason)
            return input_check.safe_response
        
        # 3. Agent: Process request
        response = await self.agent.generate(input_check.sanitized_text)
        
        # 4. Rampart: Output safety check
        output_check = await self.rampart.check_output(response.text)
        if output_check.blocked:
            await self.governance.log_event("output_blocked", output_check.reason)
            return "I'm unable to provide that information. Can I help with something else?"
        
        # 5. Governance: Audit log
        await self.governance.log_event("request_completed", {
            "user_id": user_context["user_id"],
            "latency_ms": response.latency_ms,
            "tokens_used": response.tokens,
            "tools_called": response.tool_calls,
        })
        
        return output_check.final_text
```

---

## When to Use What

| Need | Tool | Why |
|------|------|-----|
| Block prompt injection in real-time | **Rampart** | Runtime enforcement, <50ms latency |
| Prevent hallucinated responses | **Rampart** (output shield) | Checks grounding before user sees response |
| Track which agents exist and what they do | **Governance Kit** (registry) | Central catalog with metadata |
| Require approval before deploying new agent | **Governance Kit** (approval) | Workflow with role-based approvers |
| Audit what an agent did last month | **Governance Kit** (audit) | Complete action history |
| Prove SOC2/HIPAA compliance | **Governance Kit** (reporting) | Automated compliance reports |
| Enforce "agents can only use approved models" | **Governance Kit** (policies) | Continuous policy checking |
| Block unauthorized tool calls at runtime | **Rampart** (tool guards) | Real-time authorization |
| Manage agent versions and rollbacks | **Governance Kit** (lifecycle) | Version control + approval |

---

## Enterprise Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    ENTERPRISE AI AGENT PLATFORM                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  DEVELOPER WORKFLOW                                               │
│  Code → Test → Eval → Register → Review → Approve → Deploy      │
│                          │                    │                    │
│                    Governance Kit         Governance Kit           │
│                    (registry)             (approval)               │
│                                                                   │
│  RUNTIME                                                         │
│  User → API Gateway → Rampart → Agent → Rampart → User          │
│                       (input)           (output)                  │
│                                                                   │
│  MONITORING                                                      │
│  Rampart metrics → Azure Monitor → Alerts → Governance Reports   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Takeaways

| Aspect | Rampart | Governance Kit |
|--------|---------|----------------|
| **When** | Runtime (every request) | Lifecycle (deploy, update, retire) |
| **What** | Safety enforcement | Policy & compliance management |
| **Who uses** | Runs automatically | Security/compliance teams |
| **Latency** | <50ms per check | Async (approval workflows) |
| **Analogy** | WAF/firewall for AI | Change management for AI |
| **Without it** | Agents can be jailbroken, hallucinate, leak data | No visibility, no control, compliance risk |
