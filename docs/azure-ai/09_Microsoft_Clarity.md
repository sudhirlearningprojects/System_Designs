# Microsoft Clarity — Complete Guide

## What is Microsoft Clarity?

Microsoft Clarity is a **free** behavioral analytics and session replay tool that helps you understand how users interact with your website/web app. For AI agent developers, it's critical for understanding how users interact with chat interfaces, where they get frustrated, and how to improve the agent UX.

### Why Clarity Matters for AI Agents

```
Traditional analytics: "User visited /chat page" (WHAT happened)
Clarity: "User typed a question, waited 8 seconds, rage-clicked the send button,
          then abandoned the conversation" (WHY it happened)
```

| Feature | What It Reveals for AI Agents |
|---------|-------------------------------|
| **Session Replay** | Watch exactly how users interact with your chat widget |
| **Heatmaps** | Where users click/scroll on the agent interface |
| **Rage Clicks** | User frustration (clicking repeatedly = agent too slow or unresponsive) |
| **Dead Clicks** | Clicking elements that don't respond (broken UI) |
| **Scroll Depth** | Are users reading long agent responses? |
| **Quick Backs** | User immediately leaves after agent response (bad answer?) |
| **JavaScript Errors** | Frontend bugs in your chat interface |
| **Custom Tags** | Tag sessions by agent version, user segment, conversation outcome |

---

## Setup

### Basic Installation (Script Tag)

```html
<!-- Add to <head> of your page -->
<script type="text/javascript">
    (function(c,l,a,r,i,t,y){
        c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
        t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
        y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
    })(window, document, "clarity", "script", "YOUR_PROJECT_ID");
</script>
```

### React Integration

```typescript
// clarity.ts — Clarity wrapper for React apps
declare global {
  interface Window {
    clarity: (...args: any[]) => void;
  }
}

export class ClarityTracker {
  private projectId: string;
  private initialized: boolean = false;

  constructor(projectId: string) {
    this.projectId = projectId;
  }

  init(): void {
    if (this.initialized || typeof window === 'undefined') return;

    // Load Clarity script
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.async = true;
    script.src = `https://www.clarity.ms/tag/${this.projectId}`;
    document.head.appendChild(script);

    // Initialize clarity function
    (window as any).clarity = (window as any).clarity || function() {
      ((window as any).clarity.q = (window as any).clarity.q || []).push(arguments);
    };

    this.initialized = true;
  }

  // ============ CUSTOM TAGS (Critical for AI Agent Analytics) ============

  /**
   * Tag session with custom metadata for filtering in Clarity dashboard.
   * Use to segment sessions by agent behavior.
   */
  setTag(key: string, value: string): void {
    if (window.clarity) {
      window.clarity('set', key, value);
    }
  }

  /**
   * Identify user (links sessions across devices).
   * IMPORTANT: Never pass PII directly — use hashed user IDs.
   */
  identify(userId: string, sessionId?: string, pageId?: string): void {
    if (window.clarity) {
      window.clarity('identify', userId, sessionId, pageId);
    }
  }

  /**
   * Track custom events (appears in Clarity dashboard).
   */
  trackEvent(eventName: string): void {
    if (window.clarity) {
      window.clarity('event', eventName);
    }
  }

  /**
   * Upgrade session priority (ensures this session is recorded).
   * Use for important sessions: errors, escalations, rage clicks.
   */
  upgrade(reason: string): void {
    if (window.clarity) {
      window.clarity('upgrade', reason);
    }
  }

  /**
   * Consent management (GDPR compliance).
   */
  setConsent(hasConsent: boolean): void {
    if (window.clarity) {
      window.clarity('consent', hasConsent);
    }
  }
}

// Singleton instance
export const clarity = new ClarityTracker('YOUR_PROJECT_ID');
```

### React Hook

```typescript
// useClarityTracking.ts
import { useEffect } from 'react';
import { clarity } from './clarity';

export function useClarityTracking() {
  useEffect(() => {
    clarity.init();
  }, []);

  return {
    tagSession: (key: string, value: string) => clarity.setTag(key, value),
    identify: (userId: string) => clarity.identify(userId),
    trackEvent: (event: string) => clarity.trackEvent(event),
    upgrade: (reason: string) => clarity.upgrade(reason),
  };
}

// Usage in component
function ChatWidget() {
  const { tagSession, trackEvent, upgrade } = useClarityTracking();

  useEffect(() => {
    tagSession('agent_version', 'v2.1');
    tagSession('user_segment', 'pro_subscriber');
  }, []);

  const handleAgentError = () => {
    trackEvent('agent_error');
    upgrade('agent_error');  // Ensure this session is fully recorded
  };

  const handleEscalation = () => {
    trackEvent('agent_escalation');
    tagSession('escalated', 'true');
  };

  // ...
}
```

### Next.js / App Router

```typescript
// app/layout.tsx
import Script from 'next/script';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html>
      <head>
        <Script id="clarity-script" strategy="afterInteractive">
          {`(function(c,l,a,r,i,t,y){
            c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
            t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/YOUR_PROJECT_ID";
            y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
          })(window, document, "clarity", "script", "YOUR_PROJECT_ID");`}
        </Script>
      </head>
      <body>{children}</body>
    </html>
  );
}
```

---

## Tracking AI Agent Interactions

### Custom Events for Agent UX

```typescript
class AgentClarityTracker {
  /**
   * Track the full lifecycle of an agent conversation.
   */

  // User starts a conversation
  conversationStarted(conversationId: string): void {
    clarity.trackEvent('conversation_started');
    clarity.setTag('conversation_id', conversationId);
    clarity.setTag('conversation_status', 'active');
  }

  // User sends a message
  messageSent(messageLength: number): void {
    clarity.trackEvent('message_sent');
    clarity.setTag('last_message_length', String(messageLength));
  }

  // Agent starts responding (time-to-first-token)
  agentResponseStarted(): void {
    clarity.trackEvent('agent_response_started');
  }

  // Agent finishes responding
  agentResponseComplete(latencyMs: number, tokenCount: number): void {
    clarity.trackEvent('agent_response_complete');
    clarity.setTag('response_latency', this.bucketLatency(latencyMs));
    clarity.setTag('response_tokens', this.bucketTokens(tokenCount));
  }

  // User gives feedback (thumbs up/down)
  feedbackGiven(rating: 'positive' | 'negative', messageId: string): void {
    clarity.trackEvent(`feedback_${rating}`);
    clarity.setTag('last_feedback', rating);
    
    if (rating === 'negative') {
      // Upgrade session priority — we want to watch negative feedback sessions
      clarity.upgrade('negative_feedback');
    }
  }

  // User escalates to human
  escalated(reason: string): void {
    clarity.trackEvent('escalation');
    clarity.setTag('escalation_reason', reason);
    clarity.setTag('conversation_status', 'escalated');
    clarity.upgrade('escalation');
  }

  // User abandons conversation (closes without resolution)
  abandoned(messageCount: number, durationSeconds: number): void {
    clarity.trackEvent('conversation_abandoned');
    clarity.setTag('conversation_status', 'abandoned');
    clarity.setTag('messages_before_abandon', String(messageCount));
    clarity.setTag('duration_before_abandon', this.bucketDuration(durationSeconds));
    clarity.upgrade('abandonment');
  }

  // Agent error occurred
  agentError(errorType: string): void {
    clarity.trackEvent('agent_error');
    clarity.setTag('error_type', errorType);
    clarity.upgrade('agent_error');
  }

  // Tool execution visible to user
  toolExecutionShown(toolName: string): void {
    clarity.trackEvent('tool_shown');
    clarity.setTag('last_tool', toolName);
  }

  // User frustration signals
  frustrationDetected(signal: 'rage_click' | 'rapid_resend' | 'long_wait'): void {
    clarity.trackEvent(`frustration_${signal}`);
    clarity.setTag('frustration_signal', signal);
    clarity.upgrade('user_frustration');
  }

  // Conversation resolved successfully
  resolved(resolutionType: string): void {
    clarity.trackEvent('conversation_resolved');
    clarity.setTag('conversation_status', 'resolved');
    clarity.setTag('resolution_type', resolutionType);
  }

  // Helper: bucket latency into ranges for filtering
  private bucketLatency(ms: number): string {
    if (ms < 1000) return '<1s';
    if (ms < 3000) return '1-3s';
    if (ms < 5000) return '3-5s';
    if (ms < 10000) return '5-10s';
    return '>10s';
  }

  private bucketTokens(tokens: number): string {
    if (tokens < 100) return 'short';
    if (tokens < 500) return 'medium';
    return 'long';
  }

  private bucketDuration(seconds: number): string {
    if (seconds < 60) return '<1min';
    if (seconds < 300) return '1-5min';
    return '>5min';
  }
}

export const agentClarity = new AgentClarityTracker();
```

---

## Clarity Dashboard — What to Analyze

### Key Filters for AI Agent Sessions

```
In Clarity Dashboard → Filters:

1. FRUSTRATED SESSIONS (find UX problems):
   Filter: Rage Clicks > 0 OR Dead Clicks > 0
   → Watch replays to see what frustrated users

2. ABANDONED CONVERSATIONS:
   Filter: Custom Tag "conversation_status" = "abandoned"
   → Understand why users leave without resolution

3. SLOW RESPONSES:
   Filter: Custom Tag "response_latency" = ">10s"
   → Identify when agent is too slow

4. NEGATIVE FEEDBACK:
   Filter: Custom Event "feedback_negative"
   → Watch what happened before negative feedback

5. ESCALATIONS:
   Filter: Custom Event "escalation"
   → Understand what the agent couldn't handle

6. ERRORS:
   Filter: Custom Event "agent_error" OR JavaScript Errors > 0
   → Find and fix bugs

7. A/B TEST COMPARISON:
   Filter: Custom Tag "agent_version" = "v2.0" vs "v2.1"
   → Compare user behavior between versions
```

### Heatmap Analysis for Chat UI

```
CLICK HEATMAP reveals:
- Are users clicking the feedback buttons? (if not → make them more visible)
- Are users clicking suggested follow-up questions? (if yes → good feature)
- Are users trying to click on citations/sources? (if yes → make them clickable)
- Dead clicks on agent response text? (users may want to copy/select)

SCROLL HEATMAP reveals:
- Do users scroll to read long agent responses? (if not → responses too long)
- Do users scroll up to re-read earlier messages? (if yes → agent may be inconsistent)
- Scroll depth on the chat widget → optimal widget height
```

---

## Integration with AI Observability Stack

### Linking Clarity Sessions to LangSmith Traces

```typescript
// When agent responds, link the Clarity session to the LangSmith trace
function linkClarityToTrace(claritySessionId: string, langsmithTraceId: string): void {
  // Tag Clarity session with trace ID
  clarity.setTag('trace_id', langsmithTraceId);
  
  // Also store Clarity session ID in your backend logs
  // So you can go: LangSmith trace → find Clarity session → watch replay
  logger.info('session_linked', {
    clarity_session: claritySessionId,
    langsmith_trace: langsmithTraceId,
  });
}

// Get Clarity session ID (from cookie)
function getClaritySessionId(): string | null {
  // Clarity stores session in _clsk cookie
  const cookies = document.cookie.split(';');
  const clsk = cookies.find(c => c.trim().startsWith('_clsk='));
  return clsk ? clsk.split('=')[1].split('|')[0] : null;
}
```

### Debugging Workflow

```
1. User reports: "The agent gave me a wrong answer"
2. Find in LangSmith: Search by user_id → find the trace
3. See trace_id → find Clarity session
4. Watch Clarity replay:
   - Did user actually read the response?
   - Did they try to interact with citations?
   - Did they show frustration (rage clicks)?
   - What did they do AFTER the bad answer?
5. Combined insight: "User asked about refund, agent hallucinated 30 days
   (should be 14), user rage-clicked, then escalated"
```

---

## Clarity API (Programmatic Access)

```python
# Clarity doesn't have a public REST API for data export,
# but you can use the Clarity Dashboard Export feature
# or integrate with Google Analytics 4 / Azure Application Insights

# For programmatic analysis, export Clarity data to:
# 1. Google Analytics 4 (native integration)
# 2. Azure Application Insights (via custom events)

# Clarity → Google Analytics 4 integration (automatic):
# In Clarity Settings → Integrations → Connect GA4
# All Clarity events appear as GA4 events

# Custom export via Clarity's data sharing:
# Clarity Settings → Data Export → Enable
# Data available in Azure Blob Storage for custom analysis
```

---

## Clarity vs Other Tools

| Feature | Microsoft Clarity | Hotjar | FullStory | LogRocket |
|---------|------------------|--------|-----------|-----------|
| **Price** | **Free (unlimited)** | Free tier limited | Expensive | Expensive |
| **Session Replay** | ✅ | ✅ | ✅ | ✅ |
| **Heatmaps** | ✅ | ✅ | ✅ | ❌ |
| **Rage Clicks** | ✅ (auto-detected) | ❌ | ✅ | ✅ |
| **Dead Clicks** | ✅ (auto-detected) | ❌ | ✅ | ❌ |
| **JS Error Tracking** | ✅ | ❌ | ✅ | ✅ |
| **Custom Tags** | ✅ | Limited | ✅ | ✅ |
| **AI Insights** | ✅ (Copilot) | ❌ | ❌ | ❌ |
| **GDPR Compliant** | ✅ | ✅ | ✅ | ✅ |
| **Data Retention** | 30 days | 365 days | Custom | Custom |
| **Sampling** | None (records all) | Samples at scale | Samples | Samples |
| **GA4 Integration** | ✅ (native) | ❌ | ❌ | ❌ |

**Why Clarity for AI agents:**
- **Free** — no budget needed, even at scale
- **No sampling** — records every session (important for debugging rare issues)
- **Rage click detection** — automatically finds frustrated users
- **Custom tags** — segment by agent version, conversation outcome, user type
- **Session upgrade** — ensure important sessions (errors, escalations) are always recorded

---

## Clarity Copilot (AI-Powered Insights)

Clarity includes an AI assistant that analyzes your session data:

```
Ask Clarity Copilot:
- "What are the top frustration points in my chat widget?"
- "Which pages have the highest rage click rate?"
- "Compare user behavior between agent v2.0 and v2.1"
- "What do users do after receiving a negative agent response?"
- "Show me sessions where users abandoned within 30 seconds"
```

Copilot generates:
- Automatic summaries of user behavior patterns
- Insights about UX issues
- Recommendations for improvement
- Comparison between segments

---

## Privacy & Compliance

### Masking Sensitive Content

```html
<!-- Clarity automatically masks input fields, but you can control it -->

<!-- Mask specific elements (content hidden in replay) -->
<div data-clarity-mask="true">
  <p>User's account balance: $1,234.56</p>
</div>

<!-- Unmask elements that are safe to show -->
<div data-clarity-unmask="true">
  <p>Public product information</p>
</div>

<!-- Mask entire regions -->
<section id="account-details" data-clarity-mask="true">
  <!-- All content here is masked in session replays -->
</section>
```

```typescript
// Programmatic masking
// Mask the agent's response if it contains sensitive info
function maskSensitiveResponse(responseElement: HTMLElement, containsPII: boolean): void {
  if (containsPII) {
    responseElement.setAttribute('data-clarity-mask', 'true');
  }
}
```

### GDPR/CCPA Compliance

```typescript
// Only initialize Clarity after user consents to analytics
function handleCookieConsent(analyticsConsented: boolean): void {
  if (analyticsConsented) {
    clarity.init();
    clarity.setConsent(true);
  } else {
    clarity.setConsent(false);
    // Clarity won't record this session
  }
}
```

---

## Best Practices for AI Agent Teams

1. **Tag every session** with agent version, user segment, and conversation outcome
2. **Upgrade important sessions** (errors, escalations, negative feedback) — ensures they're recorded in full
3. **Review rage click sessions weekly** — these reveal UX problems your metrics miss
4. **Compare A/B test variants** in Clarity — see behavioral differences, not just metric differences
5. **Link to traces** — connect Clarity session IDs to LangSmith/Phoenix traces for full debugging
6. **Mask PII** — agent responses may contain sensitive info; mask in replays
7. **Use Copilot** — ask AI to summarize patterns across thousands of sessions
8. **Set up alerts** — Clarity can notify you when rage clicks spike (indicates a deployment issue)

---

## Quick Setup Checklist

- [ ] Add Clarity script to your web app
- [ ] Configure content masking for sensitive areas
- [ ] Set up custom tags: agent_version, user_segment, conversation_status
- [ ] Track custom events: message_sent, response_complete, feedback, escalation, abandonment
- [ ] Upgrade priority for error/escalation sessions
- [ ] Link Clarity sessions to backend traces (LangSmith/Phoenix)
- [ ] Set up weekly review of frustrated sessions (rage clicks + dead clicks)
- [ ] Connect to GA4 for combined analytics
- [ ] Configure GDPR consent flow
- [ ] Share dashboard with product/design team
