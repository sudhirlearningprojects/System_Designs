# Round 3 — Product Thinking, Outcomes & Leadership

## What They're Evaluating

- Can you define success metrics and measure outcomes?
- Do you think in terms of customer value, not just technical implementation?
- Can you run experiments and make data-driven decisions?
- Can you influence without authority and align cross-functional teams?
- Do you understand the business impact of AI agents?

---

## 1. Defining Success Metrics

### Metrics Framework for AI Agents

```
┌─────────────────────────────────────────────────────────────┐
│                    METRICS PYRAMID                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│                    BUSINESS OUTCOMES                          │
│                    (North Star)                               │
│              ┌─────────────────────┐                        │
│              │ Support cost/ticket │                        │
│              │ Customer retention  │                        │
│              │ NPS improvement     │                        │
│              └──────────┬──────────┘                        │
│                         │                                    │
│                  PRODUCT METRICS                              │
│           ┌─────────────┼─────────────┐                    │
│           │             │             │                     │
│     ┌─────▼────┐ ┌─────▼────┐ ┌─────▼────┐               │
│     │Resolution│ │   CSAT   │ │Deflection│               │
│     │  Rate    │ │  Score   │ │  Rate    │               │
│     └──────────┘ └──────────┘ └──────────┘               │
│                         │                                    │
│                  AGENT METRICS                                │
│     ┌───────────────────┼───────────────────┐              │
│     │                   │                   │              │
│  ┌──▼──────┐  ┌────────▼───────┐  ┌───────▼────────┐    │
│  │Task     │  │Time to         │  │Escalation      │    │
│  │Completion│  │Resolution      │  │Rate            │    │
│  └─────────┘  └────────────────┘  └────────────────┘    │
│                         │                                    │
│                  QUALITY METRICS                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │Accuracy  │  │Hallucin. │  │Latency   │  │Safety    │  │
│  │Score     │  │Rate      │  │(TTFT)    │  │Violations│  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Metric Definitions

| Metric | Definition | Target | How to Measure |
|--------|-----------|--------|----------------|
| **Task Completion Rate** | % of conversations where user's goal was achieved | >85% | User confirms resolution OR no follow-up within 24h |
| **CSAT** | Customer satisfaction score post-conversation | >4.2/5 | Post-chat survey (1-5 scale) |
| **Resolution Rate** | % resolved without human escalation | >70% | No handoff triggered AND positive feedback |
| **Time to Resolution** | Time from first message to resolution | <3 min | Timestamp of resolution confirmation - start |
| **Deflection Rate** | % of support tickets avoided by agent | >40% | Conversations that would have been tickets |
| **Escalation Rate** | % requiring human handoff | <20% | Handoff events / total conversations |
| **Containment Rate** | % staying within agent (not abandoning) | >80% | Completed conversations / started conversations |
| **Cost per Resolution** | Total cost (LLM + infra) per resolved conversation | <$0.15 | Infrastructure + API costs / resolutions |
| **Hallucination Rate** | % of responses with factual errors | <2% | Automated fact-checking + human audit sample |
| **Repeat Contact Rate** | % of users returning with same issue within 7 days | <10% | Same user + similar intent within 7d |

---

## 2. Experimentation Framework

### A/B Testing AI Agents

```
┌─────────────────────────────────────────────────────────────┐
│                    EXPERIMENT DESIGN                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Hypothesis: "Adding step-by-step tool visibility will      │
│               increase task completion by 10%"               │
│                                                              │
│  Control (50%):  Standard agent response                    │
│  Variant (50%):  Agent with visible tool execution steps    │
│                                                              │
│  Primary Metric:  Task completion rate                       │
│  Guardrail Metrics: Latency, CSAT, escalation rate          │
│  Sample Size: 10,000 conversations per variant              │
│  Duration: 2 weeks                                           │
│  Statistical Significance: p < 0.05                          │
│                                                              │
│  Decision Criteria:                                          │
│  • Ship if: +5% task completion AND no guardrail regression │
│  • Iterate if: +2-5% with guardrail concerns               │
│  • Kill if: <2% improvement OR guardrail regression         │
└─────────────────────────────────────────────────────────────┘
```

### Types of Experiments for AI Agents

| Experiment Type | What You're Testing | Example |
|----------------|--------------------|---------| 
| **Prompt A/B** | Different system prompts | Formal vs conversational tone |
| **Model A/B** | Different LLM models | GPT-4 vs Claude for specific tasks |
| **UX A/B** | Interface changes | Show/hide thinking indicators |
| **Flow A/B** | Conversation flow | Ask clarifying questions vs attempt answer |
| **Tool A/B** | Different tool strategies | RAG vs direct API lookup |
| **Guardrail A/B** | Safety threshold tuning | Strict vs relaxed content filtering |

### Experiment Lifecycle

```
1. IDENTIFY OPPORTUNITY
   "Users are abandoning 30% of subscription-related conversations"

2. FORM HYPOTHESIS
   "If we pre-fetch account info before responding, we'll reduce
    back-and-forth by 40% and increase resolution rate by 15%"

3. DESIGN EXPERIMENT
   - Control: Agent asks for account details
   - Variant: Agent proactively retrieves account info (with permission)
   - Metrics: Resolution rate, time-to-resolution, CSAT
   - Duration: 2 weeks, 5K conversations per arm

4. IMPLEMENT & LAUNCH
   - Feature flag for traffic splitting
   - Logging for all metrics
   - Real-time monitoring for guardrails

5. ANALYZE RESULTS
   - Statistical significance check
   - Segment analysis (new vs returning users, issue type)
   - Qualitative review of conversation samples

6. DECIDE & ITERATE
   - Ship / Iterate / Kill
   - Document learnings
   - Plan next experiment
```

---

## 3. Product Thinking for AI Agents

### Framework: Jobs-to-be-Done for Adobe Users

| User Segment | Job to be Done | Agent Opportunity |
|-------------|---------------|-------------------|
| Photoshop beginner | "Help me achieve this effect I saw online" | Visual tutorial agent with step-by-step guidance |
| Creative Cloud subscriber | "Fix my billing issue without waiting on hold" | Account management agent with instant resolution |
| Enterprise admin | "Manage licenses for my 500-person team" | Bulk operations agent with confirmation workflows |
| Premiere Pro editor | "Why is my export failing?" | Diagnostic agent that reads error logs and suggests fixes |
| Acrobat user | "Convert this complex document while preserving formatting" | Guided conversion agent with preview |

### Prioritization Framework (RICE)

| Feature | Reach | Impact | Confidence | Effort | Score |
|---------|-------|--------|------------|--------|-------|
| Subscription management agent | 10K/week | High (3) | 90% | 2 months | 135 |
| Creative tutorial agent | 5K/week | High (3) | 70% | 3 months | 35 |
| Error diagnostic agent | 3K/week | Medium (2) | 80% | 1 month | 48 |
| License management agent | 500/week | High (3) | 85% | 2 months | 6.4 |

### User Journey Mapping

```
BEFORE AGENT:
User has issue → Search help docs (5 min) → Can't find answer →
Submit support ticket → Wait 24-48h → Back-and-forth emails (3 days) →
Resolution (or give up)

WITH AGENT:
User has issue → Open agent chat → Agent understands context →
Agent retrieves relevant info → Provides solution (or executes action) →
Resolution in <3 minutes

METRICS IMPACT:
• Time to resolution: 3 days → 3 minutes (99.9% reduction)
• Support ticket volume: -40% deflection
• CSAT: 3.2 → 4.4 (+37%)
• Cost per resolution: $12 (human) → $0.15 (agent)
```

---

## 4. Stakeholder Communication

### How to Present to Different Audiences

| Audience | They Care About | Frame Your Work As |
|----------|----------------|-------------------|
| **Engineering** | Architecture, scalability, reliability | "Here's the system design and how it handles edge cases" |
| **Product** | User value, metrics, roadmap | "This improves task completion by 15% for 10K users/week" |
| **Design** | User experience, accessibility, brand | "Users feel confident and in control throughout" |
| **Support** | Ticket deflection, quality, escalation | "This handles 40% of subscription queries, escalates correctly" |
| **Leadership** | Business impact, cost, competitive advantage | "$2M annual savings, 37% CSAT improvement, industry-leading AI support" |

### Writing a One-Pager (Product Brief)

```markdown
## AI Agent: Subscription Management

### Problem
- 10K subscription-related support tickets/week
- Average resolution time: 2.5 days
- CSAT for subscription issues: 3.1/5
- Cost: $12/ticket × 10K = $120K/week

### Solution
AI agent that handles subscription queries end-to-end:
- View/change plan
- Cancel with retention offers
- Billing inquiries
- Payment method updates

### Success Metrics
| Metric | Current | Target (3 months) |
|--------|---------|-------------------|
| Resolution rate | 0% (no agent) | 70% |
| Time to resolution | 2.5 days | <3 minutes |
| CSAT | 3.1 | 4.2 |
| Weekly cost | $120K | $72K (-40%) |

### Risks & Mitigations
- Hallucination on billing amounts → Ground in real-time API data
- Unauthorized account changes → Require identity verification
- User frustration → Easy escalation to human at any point

### Timeline
- Week 1-2: MVP (view plan, FAQ answers)
- Week 3-4: Actions (cancel, change plan) with human approval
- Week 5-8: Full autonomy with guardrails
- Ongoing: Experiment and iterate
```

---

## 5. Leadership & Influence

### Scenarios to Prepare For

#### "Tell me about a time you drove a technical decision that impacted the product"

**STAR Format:**
- **Situation**: "Our AI agent was hallucinating billing amounts 5% of the time"
- **Task**: "I needed to reduce this to <0.5% without sacrificing response quality"
- **Action**: "I proposed a grounding architecture where every factual claim is verified against real-time API data before being included in the response. I built a prototype, ran an A/B test showing 95% hallucination reduction with only 200ms added latency, and presented the data to the team"
- **Result**: "Shipped to 100% of traffic. Hallucination rate dropped to 0.3%. CSAT improved by 0.4 points. Approach was adopted by 3 other agent teams"

#### "How do you handle disagreements with product/design?"

Framework:
1. **Understand their perspective** — What metric are they optimizing for?
2. **Find shared ground** — "We both want higher task completion"
3. **Propose experiment** — "Let's test both approaches with 5K users each"
4. **Let data decide** — Remove ego, follow the numbers
5. **Commit fully** — Once decided, execute with full energy regardless of whose idea won

#### "How do you prioritize when everything is urgent?"

Framework:
1. **Impact × Urgency matrix** — What moves the needle most?
2. **Reversibility** — Irreversible decisions get more thought
3. **Dependencies** — What unblocks other teams?
4. **Customer pain** — Severity × frequency of user impact
5. **Communicate trade-offs** — "If we do X, Y gets delayed by 2 weeks. Here's why X is higher priority..."

---

## 6. Industry Trends to Reference

### AI Agent Landscape (2024-2025)

| Trend | Implication for Adobe |
|-------|----------------------|
| **Multi-modal agents** | Agents that understand screenshots, videos, design files |
| **Agentic workflows** | Agents that take actions (not just answer questions) |
| **Tool use / Function calling** | Agents integrated with product APIs |
| **RAG (Retrieval Augmented Generation)** | Grounding responses in Adobe's knowledge base |
| **Evaluation frameworks** | Systematic quality measurement (not just vibes) |
| **Guardrails & safety** | Brand-safe, factually grounded, bias-free responses |
| **Personalization** | Agents that know user's skill level, preferences, history |
| **Proactive agents** | Agents that suggest help before user asks |

### Competitive Landscape

| Company | AI Agent Approach |
|---------|-------------------|
| **Microsoft (Copilot)** | Deep product integration, multi-modal, enterprise focus |
| **Google (Gemini)** | Multi-modal, workspace integration, search grounding |
| **Canva (Magic Studio)** | Design-specific AI, simple UX, action-oriented |
| **Figma (AI)** | Design context-aware, collaborative |
| **Adobe (opportunity)** | Creative professional focus, multi-product, enterprise + individual |

### Adobe's Unique Advantages for AI Agents

1. **Deep product knowledge** — Decades of creative tool documentation
2. **User context** — Knows what tool/feature user is using
3. **Creative Cloud ecosystem** — Cross-product workflows
4. **Enterprise relationships** — B2B support at scale
5. **Firefly/Sensei** — Existing AI infrastructure and models
6. **Trust** — Creative professionals trust Adobe with their work

---

## 7. Practice Questions

### Product Thinking
1. "If you could build one AI agent for Adobe, what would it be and why?"
2. "How would you measure if an AI agent is actually helping users vs just responding?"
3. "A PM wants to launch an agent that's 60% accurate. How do you respond?"
4. "How would you decide between improving accuracy vs reducing latency?"
5. "The agent's CSAT is 4.0 but escalation rate is 35%. What do you do?"

### Outcomes & Data
1. "How would you set up an experiment to test a new agent capability?"
2. "Your agent's resolution rate dropped 10% this week. Walk me through debugging."
3. "How do you distinguish between 'agent failed' vs 'user had unrealistic expectations'?"
4. "Define the SLOs for an AI agent system."

### Leadership
1. "How would you convince a skeptical engineering team to adopt a new agent architecture?"
2. "Design team wants a minimal UI, support team wants maximum information. How do you resolve?"
3. "You have 3 engineers and 6 months. What's your roadmap for an AI agent platform?"
4. "How do you build a culture of experimentation in an AI team?"

### Adobe-Specific
1. "How would AI agents change Adobe's support model?"
2. "What's the biggest risk of deploying AI agents for creative professionals?"
3. "How would you handle an agent giving wrong Photoshop instructions that damages a user's file?"
4. "How would you personalize the agent for a beginner vs a 20-year Photoshop veteran?"

---

## Next: [Round 4 — Director Round →](Round4_Director_Round.md)
