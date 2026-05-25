# 5. Real User Monitoring (RUM) for AI Interfaces

## Theory: Why RUM for AI Agents?

Backend metrics say "response took 2.1s" but the user experiences:
- Network latency (request travel time)
- Time-to-first-token (when streaming starts)
- Rendering time (displaying the response)
- Interaction delay (can they type the next message?)

**RUM captures the USER's experience, not the server's.**

---

## Setup (New Relic Browser Agent)

```html
<!-- Add to <head> — auto-instruments page loads, AJAX, JS errors -->
<script type="text/javascript">
  ;window.NREUM||(NREUM={});NREUM.init={distributed_tracing:{enabled:true},
  privacy:{cookies_enabled:true},ajax:{deny_list:["bam.nr-data.net"]}};
  // ... (copy full snippet from New Relic UI → Browser → Add app)
</script>
```

### SPA (React/Next.js) Configuration

```typescript
// For React SPAs, use the npm package
// npm install @newrelic/browser-agent

import { BrowserAgent } from '@newrelic/browser-agent/loaders/browser-agent';

const options = {
  init: {
    distributed_tracing: { enabled: true },
    privacy: { cookies_enabled: true },
    ajax: { deny_list: ['bam.nr-data.net'] },
    session_replay: { enabled: true, sampling_rate: 10 },  // 10% of sessions
  },
  info: {
    beacon: 'bam.nr-data.net',
    licenseKey: 'YOUR_BROWSER_KEY',
    applicationID: 'YOUR_APP_ID',
    sa: 1,
  },
  loader_config: {
    accountID: 'YOUR_ACCOUNT_ID',
    trustKey: 'YOUR_TRUST_KEY',
    agentID: 'YOUR_AGENT_ID',
    licenseKey: 'YOUR_BROWSER_KEY',
    applicationID: 'YOUR_APP_ID',
  },
};

new BrowserAgent(options);
```

---

## Custom Page Actions for AI Chat

```typescript
// Track AI-specific user interactions as Page Actions

class NewRelicAIRUM {
  
  // ============ CONVERSATION LIFECYCLE ============
  
  conversationStarted(conversationId: string): void {
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_conversation_started', {
        conversationId,
        timestamp: Date.now(),
        page: window.location.pathname,
      });
    }
  }

  messageSent(conversationId: string, messageLength: number): void {
    this.messageSentAt = performance.now();
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_message_sent', {
        conversationId,
        messageLength,
        turnNumber: this.turnCount++,
      });
    }
  }

  // ============ LATENCY TRACKING ============

  firstTokenReceived(conversationId: string): void {
    const ttft = performance.now() - this.messageSentAt;
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_first_token', {
        conversationId,
        ttft_ms: Math.round(ttft),
        // Bucket for easy filtering
        ttft_bucket: ttft < 1000 ? '<1s' : ttft < 3000 ? '1-3s' : ttft < 5000 ? '3-5s' : '>5s',
      });
    }
  }

  responseComplete(conversationId: string, responseLength: number): void {
    const totalTime = performance.now() - this.messageSentAt;
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_response_complete', {
        conversationId,
        total_ms: Math.round(totalTime),
        responseLength,
        tokensPerSecond: Math.round(responseLength / (totalTime / 1000)),
      });
    }
  }

  // ============ USER SIGNALS ============

  feedbackGiven(conversationId: string, messageId: string, rating: 'positive' | 'negative'): void {
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_feedback', {
        conversationId,
        messageId,
        rating,
        turnNumber: this.turnCount,
      });
    }
  }

  escalationRequested(conversationId: string, reason: string): void {
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_escalation', {
        conversationId,
        reason,
        turnsBeforeEscalation: this.turnCount,
        timeBeforeEscalation: Math.round(performance.now() - this.conversationStartedAt),
      });
    }
  }

  conversationAbandoned(conversationId: string): void {
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_abandoned', {
        conversationId,
        turnsBeforeAbandon: this.turnCount,
        durationMs: Math.round(performance.now() - this.conversationStartedAt),
        lastAction: this.lastAction,
      });
    }
  }

  // ============ FRUSTRATION SIGNALS ============

  rageClick(conversationId: string, element: string): void {
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_rage_click', {
        conversationId,
        element,
        possibleCause: 'slow_response_or_unresponsive_ui',
      });
    }
  }

  rapidResend(conversationId: string): void {
    // User sends same/similar message multiple times (agent didn't help)
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_rapid_resend', {
        conversationId,
        signal: 'user_not_satisfied_with_response',
      });
    }
  }

  // ============ TOOL VISIBILITY ============

  toolExecutionShown(conversationId: string, toolName: string): void {
    if (window.newrelic) {
      window.newrelic.addPageAction('ai_tool_shown', {
        conversationId,
        toolName,
      });
    }
  }

  // ============ ERROR TRACKING ============

  clientError(conversationId: string, error: string): void {
    if (window.newrelic) {
      window.newrelic.noticeError(new Error(error), {
        conversationId,
        component: 'ai_chat_widget',
      });
    }
  }
}

export const aiRUM = new NewRelicAIRUM();
```

---

## NRQL Queries for RUM Data

```sql
-- Time-to-first-token distribution
SELECT histogram(ttft_ms, 10, 20) FROM PageAction
WHERE actionName = 'ai_first_token' SINCE 24 hours ago

-- Average TTFT over time (detect degradation)
SELECT average(ttft_ms) FROM PageAction
WHERE actionName = 'ai_first_token'
SINCE 24 hours ago TIMESERIES 15 minutes

-- Abandonment rate by TTFT bucket
SELECT percentage(count(*), WHERE actionName = 'ai_abandoned')
FROM PageAction WHERE conversationId IS NOT NULL
FACET ttft_bucket SINCE 7 days ago

-- User frustration signals
SELECT count(*) FROM PageAction
WHERE actionName IN ('ai_rage_click', 'ai_rapid_resend')
SINCE 24 hours ago TIMESERIES 1 hour

-- Feedback by response time (do slow responses get worse feedback?)
SELECT average(total_ms) FROM PageAction
WHERE actionName = 'ai_response_complete'
FACET (SELECT rating FROM PageAction WHERE actionName = 'ai_feedback')
SINCE 7 days ago

-- Core Web Vitals for chat page
SELECT average(largestContentfulPaint) as 'LCP',
       average(firstInputDelay) as 'FID',
       average(cumulativeLayoutShift) as 'CLS'
FROM PageViewTiming WHERE pageUrl LIKE '%/chat%' SINCE 24 hours ago

-- Session replay for frustrated users
-- In New Relic UI: Browser → Session Replay → Filter by ai_rage_click events
```

---

## Linking RUM to Backend Traces

```typescript
// Get the distributed trace ID from the browser agent
function getTraceId(): string | null {
  if (window.newrelic && window.newrelic.interaction) {
    const interaction = window.newrelic.interaction();
    return interaction.getContext().traceId;
  }
  return null;
}

// Send trace ID with API request (links frontend → backend)
async function sendMessage(query: string): Promise<Response> {
  const traceId = getTraceId();
  
  return fetch('/api/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // New Relic automatically adds trace headers if distributed_tracing is enabled
    },
    body: JSON.stringify({ query, traceId }),
  });
}

// Now in New Relic: Click on a browser PageAction → "View related trace" → 
// See the full backend trace (LLM calls, tool calls, retrieval)
```

---

## Next: [Production Patterns →](06_Production_Patterns.md)
