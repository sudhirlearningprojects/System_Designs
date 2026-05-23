# Round 4 — Director Round

## What They're Evaluating

- Do you have a strategic vision for AI agents at Adobe scale?
- Can you think beyond your immediate scope?
- Do you demonstrate executive-level communication?
- Can you balance innovation with pragmatism?
- Do you understand Adobe's business, customers, and competitive position?
- Are you someone they'd trust to represent the team to senior leadership?

---

## 1. Strategic Vision Questions

### "What's your vision for AI agents at Adobe over the next 2-3 years?"

**Framework for answering:**

```
PHASE 1 (Now - 6 months): ASSIST
├── Reactive support agents (answer questions, resolve issues)
├── Knowledge-grounded (RAG on help docs, community forums)
├── Human-in-the-loop for actions (agent suggests, human approves)
└── Metrics: Deflection rate, CSAT, resolution time

PHASE 2 (6-12 months): ACT
├── Autonomous agents that take actions (cancel, upgrade, fix)
├── Multi-step workflows (diagnose → plan → execute → verify)
├── Cross-product agents (Photoshop + Lightroom + Creative Cloud)
├── Proactive agents (detect issues before user reports)
└── Metrics: Task completion, cost savings, retention impact

PHASE 3 (12-24 months): ANTICIPATE
├── Personalized creative assistants (know your style, skill level)
├── Workflow automation agents (batch operations, repetitive tasks)
├── Collaborative agents (work alongside user in real-time)
├── Multi-modal (understand screenshots, videos, design files)
└── Metrics: User productivity gain, feature adoption, NPS

PHASE 4 (24+ months): AUGMENT
├── Creative co-pilots (generate ideas, iterate on designs)
├── Enterprise workflow orchestration (cross-team, cross-tool)
├── Self-improving agents (learn from every interaction)
├── Agent marketplace (custom agents for specific workflows)
└── Metrics: Revenue attribution, market differentiation
```

---

### "How would you build an AI agent platform vs individual agents?"

**Platform Thinking:**

```
┌─────────────────────────────────────────────────────────────┐
│                    AGENT PLATFORM                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  AGENT LAYER (domain-specific)                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ Support  │ │ Creative │ │ Account  │ │ Enterprise   │  │
│  │ Agent    │ │ Assistant│ │ Manager  │ │ Admin Agent  │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘  │
│       └─────────────┼───────────┼───────────────┘           │
│                     ▼                                        │
│  PLATFORM LAYER (shared infrastructure)                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Orchestration │ Memory │ Tools │ Guardrails │ Eval   │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ LLM Gateway │ Vector DB │ Caching │ Rate Limiting    │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ Observability │ A/B Testing │ Analytics │ Audit Log  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  INTEGRATION LAYER                                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Adobe APIs │ Identity │ Entitlements │ Knowledge Base│   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Key argument**: "Building a platform means the 2nd, 3rd, 10th agent takes weeks instead of months. Shared guardrails ensure consistent quality. Shared observability gives us fleet-wide visibility."

---

## 2. Business Impact Framing

### Cost-Benefit Analysis

```
CURRENT STATE (without AI agents):
├── Support tickets: 500K/month
├── Average cost per ticket: $12 (human agent time)
├── Monthly support cost: $6M
├── Average resolution time: 2.5 days
├── CSAT: 3.2/5
└── Support team size: 2,000 agents

WITH AI AGENTS (12-month projection):
├── AI-resolved: 200K/month (40% deflection)
├── Cost per AI resolution: $0.15
├── AI resolution cost: $30K/month
├── Human tickets remaining: 300K/month
├── Human cost: $3.6M/month
├── Total monthly cost: $3.63M
├── Monthly savings: $2.37M
├── Annual savings: $28.4M
├── Average resolution time: 3 minutes (AI) / 1.5 days (human, less volume)
└── CSAT: 4.2/5 (AI) / 4.0/5 (human, more complex cases)

INVESTMENT:
├── Platform development: $3M (team of 15 for 12 months)
├── LLM API costs: $360K/year
├── Infrastructure: $200K/year
├── Total year 1 investment: $3.56M
└── ROI: 8x in year 1, 15x+ in year 2
```

### Revenue Impact (Beyond Cost Savings)

| Impact Area | Mechanism | Estimated Value |
|-------------|-----------|-----------------|
| Reduced churn | Faster resolution → happier customers | $5M/year (0.5% churn reduction) |
| Upsell/cross-sell | Agent suggests relevant products | $2M/year |
| Feature adoption | Agent teaches features → stickier product | $3M/year (reduced churn) |
| Support team reallocation | Humans handle complex/high-value cases | Qualitative improvement |
| Competitive differentiation | Best-in-class AI support | Market positioning |

---

## 3. Risk Management

### "What could go wrong with AI agents at Adobe?"

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| **Hallucination damages user's work** | Critical | Medium | Ground in verified docs, never suggest destructive actions without confirmation |
| **Brand damage from inappropriate response** | High | Low | Multi-layer guardrails, human review for edge cases |
| **Data privacy violation** | Critical | Low | Never access file contents, strict data boundaries, audit logging |
| **Over-reliance leading to skill atrophy** | Medium | Medium | Position as assistant, not replacement; teach concepts |
| **Cost explosion from LLM usage** | Medium | Medium | Token budgets, caching, model tiering, cost alerts |
| **Agent gives wrong billing info** | High | Medium | Always ground in real-time API data, never hallucinate amounts |
| **Competitor ships better agent first** | Medium | High | Move fast with platform approach, iterate weekly |

### Incident Response Plan

```
SEVERITY 1: Agent causes data loss or financial harm
→ Immediately disable agent for affected category
→ Notify affected users within 1 hour
→ Root cause analysis within 24 hours
→ Fix + additional guardrails before re-enabling

SEVERITY 2: Agent consistently giving wrong answers (>10% error rate)
→ Increase human review sampling to 100%
→ Identify root cause (prompt drift, knowledge base stale, model regression)
→ Fix within 24 hours or disable affected capability

SEVERITY 3: Agent quality degradation (CSAT drop, completion rate drop)
→ Investigate within 48 hours
→ A/B test fix
→ Ship within 1 week
```

---

## 4. Team & Culture

### "How would you build and lead an AI agent team?"

**Team Composition (15 people):**

```
AI Agent Team
├── Engineering (8)
│   ├── 2 Backend (orchestration, APIs, infrastructure)
│   ├── 2 ML/AI (prompt engineering, evaluation, fine-tuning)
│   ├── 2 Frontend (chat UI, streaming, accessibility)
│   ├── 1 Platform/DevOps (CI/CD, monitoring, deployment)
│   └── 1 Data (analytics, experimentation, metrics)
│
├── Product (2)
│   ├── 1 Product Manager (roadmap, prioritization, stakeholders)
│   └── 1 Designer (UX research, interaction design)
│
├── Quality (2)
│   ├── 1 QA/Eval (test suites, golden datasets, regression)
│   └── 1 Content/Knowledge (knowledge base, prompt quality)
│
└── You (Tech Lead / Staff Engineer)
    ├── Architecture decisions
    ├── Technical mentorship
    ├── Cross-team alignment
    └── Hands-on coding (30-40% of time)
```

### Team Operating Principles

1. **Ship weekly** — Small, measurable improvements every week
2. **Experiment everything** — No opinion without data
3. **Customer conversations** — Every team member talks to users monthly
4. **Blameless postmortems** — Learn from failures, don't punish
5. **Platform first** — Build reusable, not one-off
6. **Quality is non-negotiable** — Never ship something that could harm users

---

## 5. Cross-Functional Alignment

### Stakeholder Map

```
┌─────────────────────────────────────────────────────────────┐
│                    STAKEHOLDER MAP                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  HIGH INFLUENCE, HIGH INTEREST (Manage Closely)             │
│  • VP of Customer Experience (your skip-level)              │
│  • Head of Support Operations                               │
│  • Product Directors (Creative Cloud, Document Cloud)        │
│                                                              │
│  HIGH INFLUENCE, LOW INTEREST (Keep Satisfied)              │
│  • Legal/Compliance (data privacy, AI governance)           │
│  • Security team (access controls, audit)                   │
│  • Finance (budget approval, ROI tracking)                  │
│                                                              │
│  LOW INFLUENCE, HIGH INTEREST (Keep Informed)               │
│  • Support agents (their workflow changes)                  │
│  • Content/docs team (knowledge base quality)               │
│  • Community team (user feedback)                           │
│                                                              │
│  LOW INFLUENCE, LOW INTEREST (Monitor)                      │
│  • Other engineering teams                                   │
│  • Marketing                                                 │
└─────────────────────────────────────────────────────────────┘
```

### Communication Cadence

| Audience | Frequency | Format | Content |
|----------|-----------|--------|---------|
| Director/VP | Weekly | 5-min standup or Slack update | Metrics, blockers, decisions needed |
| Cross-functional partners | Bi-weekly | 30-min sync | Roadmap alignment, dependencies |
| Team | Daily | Standup + async | Sprint progress, technical decisions |
| Broader org | Monthly | Demo + newsletter | Wins, learnings, upcoming features |
| Executive | Quarterly | Business review deck | ROI, strategic progress, next quarter plan |

---

## 6. Questions to Ask the Director

Asking thoughtful questions demonstrates strategic thinking:

1. "What does success look like for AI agents at Adobe in 12 months? What metrics would make you say 'this was a great investment'?"

2. "How do you see AI agents fitting into Adobe's broader AI strategy alongside Firefly and Sensei?"

3. "What's the biggest organizational challenge you foresee in scaling AI agents across multiple product lines?"

4. "How much autonomy would this role have in making architectural decisions vs following established patterns?"

5. "What's the current relationship between the AI agent team and the support operations team? How do you envision that evolving?"

6. "Are there any failed attempts at AI agents that I should learn from? What went wrong?"

---

## 7. Practice Questions for Director Round

### Vision & Strategy
1. "Where do you see AI agents in 5 years? How does Adobe win?"
2. "Should we build our own LLM or use third-party? Why?"
3. "How do you balance innovation speed with quality/safety for creative professionals?"
4. "What's your framework for deciding build vs buy vs partner?"

### Business Acumen
1. "How would you justify a $3M investment in an AI agent platform to the CFO?"
2. "If you had to cut the team by 50%, what would you keep and what would you cut?"
3. "How do you think about AI agents as a competitive moat vs table stakes?"

### Leadership & People
1. "How do you handle a situation where your best engineer disagrees with the product direction?"
2. "How do you maintain team morale during a period of high ambiguity?"
3. "Tell me about a time you had to make a decision with incomplete information"
4. "How do you develop senior engineers into tech leads?"

### Adobe-Specific
1. "How would you ensure AI agents maintain Adobe's premium brand perception?"
2. "Creative professionals are skeptical of AI. How do you build trust?"
3. "How would you handle the tension between 'AI replacing support jobs' narrative and reality?"
4. "What's unique about building AI agents for creative workflows vs generic customer support?"

---

## 8. Your "Story Arc" for the Interview

Structure your entire interview day around a consistent narrative:

```
THEME: "I build AI systems that measurably improve customer outcomes,
        with production-grade reliability and data-driven iteration."

Round 1 (Engineering): "Here's HOW I build it — architecture, code, observability"
Round 2 (Frontend): "Here's how users EXPERIENCE it — intuitive, accessible, trustworthy"
Round 3 (Product): "Here's how I MEASURE and IMPROVE it — metrics, experiments, outcomes"
Round 4 (Director): "Here's the VISION and how I'd LEAD the team to get there"
```

### Key Stories to Have Ready (3-4 polished stories)

1. **Technical depth**: A complex system you designed and built (architecture decisions, trade-offs)
2. **Customer impact**: A time your work directly improved user outcomes (with metrics)
3. **Leadership/influence**: A time you drove a decision across teams without authority
4. **Failure & learning**: A time something went wrong and how you handled it
5. **Innovation**: A time you introduced a new approach that became the standard

For each story, prepare:
- 30-second version (elevator pitch)
- 2-minute version (standard)
- 5-minute version (deep dive with follow-up questions)

---

## Final Preparation Checklist

- [ ] Research Adobe's recent AI announcements (Firefly, Sensei, GenStudio)
- [ ] Understand Adobe's product portfolio and customer segments
- [ ] Prepare 3-4 polished STAR stories
- [ ] Practice explaining technical concepts to non-technical audience
- [ ] Have a clear 90-day plan ready ("What would you do in your first 90 days?")
- [ ] Prepare thoughtful questions for each interviewer
- [ ] Know your metrics: numbers from past projects (latency, accuracy, cost, team size)
- [ ] Practice whiteboarding system design (AI agent architecture)
- [ ] Review Adobe's values and culture (creativity, innovation, involvement)
- [ ] Prepare for "Why Adobe?" with genuine, specific reasons
