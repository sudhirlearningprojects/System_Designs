# Round 2 — Frontend & Experience Engineering

## What They're Evaluating

- Can you build production frontends with React/Angular? (JD: hands-on experience required)
- Do you understand PWAs and SPAs deeply? (JD: explicit requirement)
- Do you know DOM manipulation and WCM (Web Content Management) core components/templates?
- Can you build streaming conversational interfaces with accessibility?
- Do you understand HTML, CSS, JavaScript fundamentals?
- Can you implement Real User Monitoring (RUM) in frontend code?

---

## 0. JD-Specific Frontend Knowledge

### PWA (Progressive Web App) for Agent Chat

```javascript
// Service Worker for offline agent support
// sw.js
const CACHE_NAME = 'agent-chat-v1';
const OFFLINE_URLS = ['/chat', '/offline.html', '/styles/chat.css', '/js/agent.js'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(OFFLINE_URLS))
  );
});

self.addEventListener('fetch', (event) => {
  // Network-first for API calls, cache-first for static assets
  if (event.request.url.includes('/api/')) {
    event.respondWith(
      fetch(event.request)
        .catch(() => {
          // Offline: queue message for later sync
          return new Response(JSON.stringify({
            status: 'queued',
            message: 'Your message will be sent when you\'re back online'
          }), { headers: { 'Content-Type': 'application/json' } });
        })
    );
  } else {
    event.respondWith(
      caches.match(event.request).then(cached => cached || fetch(event.request))
    );
  }
});

// Background Sync for queued messages
self.addEventListener('sync', (event) => {
  if (event.tag === 'send-queued-messages') {
    event.waitUntil(sendQueuedMessages());
  }
});
```

```json
// manifest.json for PWA
{
  "name": "Adobe AI Assistant",
  "short_name": "AI Help",
  "start_url": "/chat",
  "display": "standalone",
  "background_color": "#1B1B1B",
  "theme_color": "#E03C31",
  "icons": [
    { "src": "/icons/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/icons/icon-512.png", "sizes": "512x512", "type": "image/png" }
  ]
}
```

### SPA Architecture (React)

```typescript
// SPA routing for agent chat application
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense } from 'react';

const ChatView = lazy(() => import('./views/ChatView'));
const HistoryView = lazy(() => import('./views/HistoryView'));
const SettingsView = lazy(() => import('./views/SettingsView'));

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingSkeleton />}>
        <Routes>
          <Route path="/chat" element={<ChatView />} />
          <Route path="/chat/:conversationId" element={<ChatView />} />
          <Route path="/history" element={<HistoryView />} />
          <Route path="/settings" element={<SettingsView />} />
          <Route path="*" element={<Navigate to="/chat" />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

// State management for agent conversations
import { create } from 'zustand';

interface AgentStore {
  conversations: Map<string, Conversation>;
  activeConversationId: string | null;
  isStreaming: boolean;
  sendMessage: (content: string) => Promise<void>;
  cancelStream: () => void;
}

const useAgentStore = create<AgentStore>((set, get) => ({
  conversations: new Map(),
  activeConversationId: null,
  isStreaming: false,
  
  sendMessage: async (content: string) => {
    set({ isStreaming: true });
    const convId = get().activeConversationId;
    // Stream response via SSE
    const eventSource = new EventSource(`/api/v1/conversations/${convId}/stream`);
    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      // Update conversation state incrementally
      set(state => {
        const conv = state.conversations.get(convId!)!;
        conv.messages[conv.messages.length - 1].content += data.token;
        return { conversations: new Map(state.conversations) };
      });
    };
    eventSource.onerror = () => {
      eventSource.close();
      set({ isStreaming: false });
    };
  },
  
  cancelStream: () => { /* abort controller */ }
}));
```

### DOM & WCM (Web Content Management)

```javascript
// WCM Component: Reusable agent widget that can be embedded in any Adobe page
// Uses Web Components (Custom Elements) for framework-agnostic embedding

class AdobeAgentWidget extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' });
  }

  static get observedAttributes() {
    return ['product', 'context', 'theme', 'position'];
  }

  connectedCallback() {
    this.render();
    this.initializeAgent();
  }

  attributeChangedCallback(name, oldVal, newVal) {
    if (oldVal !== newVal) this.render();
  }

  render() {
    const theme = this.getAttribute('theme') || 'dark';
    this.shadowRoot.innerHTML = `
      <style>
        :host {
          --agent-bg: ${theme === 'dark' ? '#1B1B1B' : '#FFFFFF'};
          --agent-text: ${theme === 'dark' ? '#FFFFFF' : '#1B1B1B'};
          --agent-accent: #E03C31;
          position: fixed;
          bottom: 20px;
          right: 20px;
          z-index: 10000;
        }
        .agent-container {
          width: 380px;
          height: 600px;
          background: var(--agent-bg);
          color: var(--agent-text);
          border-radius: 12px;
          box-shadow: 0 8px 32px rgba(0,0,0,0.3);
          display: flex;
          flex-direction: column;
          font-family: 'Adobe Clean', sans-serif;
        }
        .messages { flex: 1; overflow-y: auto; padding: 16px; }
        .input-area { padding: 12px; border-top: 1px solid rgba(255,255,255,0.1); }
      </style>
      <div class="agent-container">
        <div class="header" role="banner">
          <span>Adobe AI Assistant</span>
          <button aria-label="Close assistant">×</button>
        </div>
        <div class="messages" role="log" aria-live="polite" aria-label="Conversation"></div>
        <div class="input-area">
          <input type="text" placeholder="Ask me anything..." aria-label="Message input" />
        </div>
      </div>
    `;
  }

  initializeAgent() {
    const product = this.getAttribute('product');
    const context = this.getAttribute('context');
    // Initialize with product context for relevant responses
    this.agentClient = new AgentClient({ product, context });
  }
}

customElements.define('adobe-agent', AdobeAgentWidget);

// Usage in any Adobe page (WCM template):
// <adobe-agent product="photoshop" context="export-dialog" theme="dark"></adobe-agent>
```

### DOM Performance Optimization

```javascript
// Efficient DOM updates for streaming chat messages
class ChatRenderer {
  constructor(container) {
    this.container = container;
    this.currentMessageEl = null;
    this.observer = new IntersectionObserver(this.handleVisibility.bind(this));
  }

  // Batch DOM updates using requestAnimationFrame
  appendToken(token) {
    if (!this.pendingTokens) {
      this.pendingTokens = '';
      requestAnimationFrame(() => {
        if (this.currentMessageEl) {
          // Use textContent for plain text (faster than innerHTML)
          this.currentMessageEl.textContent += this.pendingTokens;
          this.autoScroll();
        }
        this.pendingTokens = null;
      });
    }
    this.pendingTokens += token;
  }

  // Virtual scrolling for long conversations
  autoScroll() {
    // Only scroll if user is near bottom (don't interrupt reading)
    const { scrollTop, scrollHeight, clientHeight } = this.container;
    const isNearBottom = scrollHeight - scrollTop - clientHeight < 100;
    if (isNearBottom) {
      this.container.scrollTop = scrollHeight;
    }
  }

  // Lazy-load old messages when scrolling up
  handleVisibility(entries) {
    entries.forEach(entry => {
      if (entry.isIntersecting && entry.target.dataset.placeholder) {
        this.loadMessage(entry.target.dataset.messageId);
      }
    });
  }
}
```

---

## 1. Conversational UI Architecture

### Chat Interface Components

```
┌─────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────┐    │
│  │              CONVERSATION HEADER                      │    │
│  │  Agent Name │ Status │ Context │ Escalate Button    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              MESSAGE THREAD                           │    │
│  │                                                       │    │
│  │  [User] How do I export my Photoshop file as PDF?    │    │
│  │                                                       │    │
│  │  [Agent] ████████████ (streaming...)                 │    │
│  │  │ Thinking: Checking documentation...               │    │
│  │  │ Tool: search_help_docs("photoshop export pdf")    │    │
│  │  │                                                    │    │
│  │  [Agent] Here's how to export as PDF:                │    │
│  │  1. Go to File → Export → Export As...               │    │
│  │  2. Select PDF from format dropdown                  │    │
│  │  [📎 Related: Export settings guide]                 │    │
│  │                                                       │    │
│  │  [Feedback] 👍 👎 | Was this helpful?                │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              INPUT AREA                               │    │
│  │  [📎] [Type your message...              ] [Send ➤] │    │
│  │  Suggested: "How to batch export?" | "PDF settings"  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### State Machine for Conversation UI

```
┌──────────┐    user types    ┌──────────┐    send    ┌──────────────┐
│   IDLE   │ ───────────────► │ COMPOSING│ ─────────► │   WAITING    │
└──────────┘                  └──────────┘            └──────┬───────┘
     ▲                                                        │
     │                                                        ▼
     │                                                 ┌──────────────┐
     │         response complete                       │  STREAMING   │
     └─────────────────────────────────────────────── │  (tokens     │
                                                       │   arriving)  │
                                                       └──────┬───────┘
                                                              │
                                              ┌───────────────┼───────────┐
                                              ▼               ▼           ▼
                                       ┌──────────┐   ┌───────────┐ ┌────────┐
                                       │ COMPLETE │   │TOOL_CALL  │ │ ERROR  │
                                       └──────────┘   │(show tool │ └────────┘
                                                      │ execution)│
                                                      └───────────┘
```

---

## 2. Streaming Response Implementation

### Server-Sent Events (SSE) for Token Streaming

```typescript
// Frontend: Streaming response handler
class AgentStreamHandler {
  private abortController: AbortController;
  
  async streamResponse(conversationId: string, message: string): Promise<void> {
    this.abortController = new AbortController();
    
    const response = await fetch(`/api/v1/conversations/${conversationId}/messages`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
      body: JSON.stringify({ content: message }),
      signal: this.abortController.signal,
    });

    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split('\n\n');
      buffer = events.pop() || '';

      for (const event of events) {
        this.handleEvent(JSON.parse(event.replace('data: ', '')));
      }
    }
  }

  private handleEvent(event: AgentEvent): void {
    switch (event.type) {
      case 'token':
        this.appendToken(event.content);
        break;
      case 'thinking':
        this.showThinkingIndicator(event.thought);
        break;
      case 'tool_call':
        this.showToolExecution(event.tool, event.status);
        break;
      case 'tool_result':
        this.showToolResult(event.result);
        break;
      case 'citation':
        this.addCitation(event.source, event.url);
        break;
      case 'suggestion':
        this.showFollowUpSuggestions(event.suggestions);
        break;
      case 'error':
        this.showError(event.message, event.recoverable);
        break;
      case 'complete':
        this.finalizeMessage(event.messageId);
        break;
    }
  }

  cancel(): void {
    this.abortController?.abort();
  }
}
```

### WebSocket Implementation (Bidirectional)

```typescript
class AgentWebSocket {
  private ws: WebSocket;
  private reconnectAttempts = 0;
  private maxReconnects = 5;

  connect(conversationId: string): void {
    this.ws = new WebSocket(`wss://api.adobe.com/agent/ws/${conversationId}`);
    
    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
      this.sendHeartbeat();
    };

    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleMessage(data);
    };

    this.ws.onclose = (event) => {
      if (!event.wasClean && this.reconnectAttempts < this.maxReconnects) {
        setTimeout(() => this.connect(conversationId), 
          Math.min(1000 * 2 ** this.reconnectAttempts++, 30000));
      }
    };
  }

  sendMessage(content: string, attachments?: File[]): void {
    this.ws.send(JSON.stringify({
      type: 'message',
      content,
      attachments: attachments?.map(f => ({ name: f.name, type: f.type })),
      timestamp: Date.now(),
    }));
  }

  sendTypingIndicator(): void {
    this.ws.send(JSON.stringify({ type: 'typing' }));
  }

  sendFeedback(messageId: string, rating: 'positive' | 'negative'): void {
    this.ws.send(JSON.stringify({ type: 'feedback', messageId, rating }));
  }
}
```

---

## 3. UI/UX Patterns for AI Agents

### Transparency & Trust

| Pattern | Implementation | Why |
|---------|---------------|-----|
| **Show thinking** | Display "Searching docs..." while tool executes | Reduces perceived wait time |
| **Cite sources** | Link to help articles, docs | Builds trust, allows verification |
| **Confidence indicators** | "I'm fairly confident..." vs "I'm not sure..." | Sets expectations |
| **Show limitations** | "I can help with X but not Y" | Prevents frustration |
| **Explain actions** | "I'm going to check your subscription status" | User stays in control |

### Error States

```typescript
// Error handling hierarchy
enum AgentErrorType {
  NETWORK_ERROR = 'network',        // "Connection lost. Retrying..."
  RATE_LIMITED = 'rate_limited',    // "Too many requests. Please wait..."
  LLM_TIMEOUT = 'timeout',         // "Taking longer than expected..."
  GUARDRAIL_BLOCK = 'blocked',     // "I can't help with that specific request"
  UNKNOWN_INTENT = 'unclear',      // "Could you rephrase that?"
  SYSTEM_ERROR = 'system',         // "Something went wrong. Try again or contact support"
}

// Graceful degradation UI
function renderError(error: AgentErrorType): JSX.Element {
  switch (error) {
    case AgentErrorType.LLM_TIMEOUT:
      return (
        <ErrorCard>
          <Spinner />
          <p>This is taking longer than usual...</p>
          <Button onClick={retry}>Try again</Button>
          <Button onClick={escalate}>Talk to a person</Button>
        </ErrorCard>
      );
    case AgentErrorType.GUARDRAIL_BLOCK:
      return (
        <ErrorCard>
          <p>I'm not able to help with that specific request.</p>
          <p>Here are some things I can help with:</p>
          <SuggestionChips suggestions={alternativeSuggestions} />
        </ErrorCard>
      );
  }
}
```

### Loading & Skeleton States

```typescript
// Progressive loading for agent responses
function AgentMessage({ isStreaming, content, toolCalls }) {
  return (
    <div className="agent-message">
      {/* Show tool execution in real-time */}
      {toolCalls.map(tool => (
        <ToolCallIndicator 
          key={tool.id}
          name={tool.name}
          status={tool.status}  // pending → executing → complete
          duration={tool.duration}
        />
      ))}
      
      {/* Streaming text with cursor */}
      <div className="message-content">
        <Markdown>{content}</Markdown>
        {isStreaming && <BlinkingCursor />}
      </div>
      
      {/* Show after complete */}
      {!isStreaming && (
        <>
          <Citations sources={citations} />
          <FeedbackButtons messageId={messageId} />
          <FollowUpSuggestions suggestions={suggestions} />
        </>
      )}
    </div>
  );
}
```

---

## 4. Accessibility (Critical for Adobe)

### WCAG 2.1 AA Compliance for Chat Interfaces

```typescript
// Accessible chat message component
function ChatMessage({ message, role }: Props) {
  return (
    <article
      role="article"
      aria-label={`${role === 'agent' ? 'AI Assistant' : 'You'} said`}
      aria-live={role === 'agent' ? 'polite' : 'off'}  // Announce agent messages
      tabIndex={0}
    >
      <div aria-hidden="true" className="avatar">
        {role === 'agent' ? <AgentIcon /> : <UserIcon />}
      </div>
      
      <div className="content">
        <span className="sr-only">{role === 'agent' ? 'AI Assistant:' : 'You:'}</span>
        <div role="text">{message.content}</div>
        
        {message.citations && (
          <nav aria-label="Source citations">
            {message.citations.map(c => (
              <a href={c.url} aria-label={`Source: ${c.title}`}>{c.title}</a>
            ))}
          </nav>
        )}
      </div>
      
      {role === 'agent' && (
        <div role="group" aria-label="Message feedback">
          <button aria-label="This was helpful" aria-pressed={feedback === 'positive'}>👍</button>
          <button aria-label="This was not helpful" aria-pressed={feedback === 'negative'}>👎</button>
        </div>
      )}
    </article>
  );
}

// Announce streaming status to screen readers
function StreamingAnnouncer({ isStreaming, toolName }) {
  return (
    <div aria-live="assertive" aria-atomic="true" className="sr-only">
      {isStreaming && 'AI Assistant is typing a response'}
      {toolName && `AI Assistant is searching: ${toolName}`}
    </div>
  );
}
```

### Keyboard Navigation

```typescript
// Chat input with keyboard shortcuts
function ChatInput({ onSend, onCancel }) {
  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      onSend();
    }
    if (e.key === 'Escape') {
      onCancel(); // Cancel streaming response
    }
  };

  return (
    <div role="form" aria-label="Chat input">
      <textarea
        aria-label="Type your message"
        aria-describedby="input-help"
        onKeyDown={handleKeyDown}
        placeholder="Ask me anything about Adobe products..."
      />
      <span id="input-help" className="sr-only">
        Press Enter to send, Shift+Enter for new line, Escape to cancel
      </span>
      <button aria-label="Send message" type="submit">
        <SendIcon aria-hidden="true" />
      </button>
    </div>
  );
}
```

---

## 5. Performance Optimization

### Perceived Performance

| Technique | Implementation | Impact |
|-----------|---------------|--------|
| **Optimistic UI** | Show user message immediately before server confirms | Feels instant |
| **Streaming** | Display tokens as they arrive | 3-5s → feels like 0.5s |
| **Skeleton loading** | Show message bubble shape while waiting | Reduces perceived wait |
| **Progressive disclosure** | Show summary first, expand for details | Faster comprehension |
| **Prefetch suggestions** | Load follow-up suggestions while user reads | Zero-wait next interaction |

### Frontend Performance

```typescript
// Virtual scrolling for long conversations
import { useVirtualizer } from '@tanstack/react-virtual';

function ConversationThread({ messages }) {
  const parentRef = useRef<HTMLDivElement>(null);
  
  const virtualizer = useVirtualizer({
    count: messages.length,
    getScrollElement: () => parentRef.current,
    estimateSize: (index) => estimateMessageHeight(messages[index]),
    overscan: 5,
  });

  return (
    <div ref={parentRef} className="conversation-scroll">
      <div style={{ height: virtualizer.getTotalSize() }}>
        {virtualizer.getVirtualItems().map(virtualRow => (
          <ChatMessage
            key={messages[virtualRow.index].id}
            message={messages[virtualRow.index]}
            style={{ transform: `translateY(${virtualRow.start}px)` }}
          />
        ))}
      </div>
    </div>
  );
}

// Debounced typing indicator
const sendTypingIndicator = useMemo(
  () => debounce(() => ws.sendTypingIndicator(), 2000, { leading: true, trailing: false }),
  [ws]
);
```

---

## 6. In-Product Agent Integration (Adobe Context)

### Contextual Agent Triggers

```typescript
// Agent that understands what user is doing in Photoshop/Illustrator
interface ProductContext {
  application: 'photoshop' | 'illustrator' | 'premiere' | 'acrobat';
  currentTool: string;
  selectedLayers: Layer[];
  documentType: string;
  recentActions: Action[];  // Last 10 user actions
  errorState?: string;      // If user hit an error
}

// Proactive agent suggestions based on context
function useProactiveAgent(context: ProductContext) {
  useEffect(() => {
    // Detect user struggling (repeated undo, same action failing)
    if (detectFrustration(context.recentActions)) {
      showAgentSuggestion({
        message: "It looks like you're trying to remove the background. Would you like help?",
        action: "remove_background_tutorial",
        dismissable: true,
      });
    }
  }, [context.recentActions]);
}

// Agent with product context
function sendMessageWithContext(message: string, context: ProductContext) {
  return fetch('/api/v1/agent/message', {
    body: JSON.stringify({
      message,
      context: {
        app: context.application,
        tool: context.currentTool,
        document: context.documentType,
        // Don't send actual content — privacy!
        recentErrors: context.errorState,
      }
    })
  });
}
```

### Multi-Modal Input (Adobe-Specific)

```typescript
// Support for screenshots, file uploads, screen recordings
interface AgentInput {
  text?: string;
  screenshot?: Blob;        // "What's wrong with this export?"
  fileReference?: string;   // Reference to Creative Cloud file
  screenRecording?: Blob;   // "Watch what happens when I do this"
  selectedArea?: Rect;      // User highlighted area of screen
}

function ChatInput() {
  return (
    <div className="multi-modal-input">
      <textarea placeholder="Describe your issue..." />
      <div className="input-actions">
        <button aria-label="Attach screenshot">📷</button>
        <button aria-label="Record screen">🎥</button>
        <button aria-label="Share file from Creative Cloud">☁️</button>
        <button aria-label="Highlight area on screen">✂️</button>
      </div>
    </div>
  );
}
```

---

## 7. Design System Integration

### Adobe Spectrum Design System

```typescript
// Using Adobe's Spectrum components for consistent brand experience
import { 
  Provider, Theme, 
  ActionButton, TextField, 
  ProgressCircle, StatusLight 
} from '@adobe/react-spectrum';

function AgentWidget() {
  return (
    <Provider theme={darkTheme} colorScheme="dark">
      <div className="agent-widget">
        <StatusLight variant="positive">AI Assistant Online</StatusLight>
        
        <ConversationThread messages={messages} />
        
        {isStreaming && (
          <div className="streaming-indicator">
            <ProgressCircle aria-label="Generating response" size="S" isIndeterminate />
            <span>Thinking...</span>
          </div>
        )}
        
        <TextField
          label="Message"
          placeholder="How can I help?"
          onChange={setInput}
          onKeyDown={handleSubmit}
        />
        <ActionButton onPress={send}>Send</ActionButton>
      </div>
    </Provider>
  );
}
```

---

## 8. Practice Questions

### Frontend Coding
1. "Build a streaming chat interface that handles token-by-token rendering"
2. "Implement a conversation component with virtual scrolling for 10K+ messages"
3. "Build an accessible feedback widget for AI responses"
4. "Implement optimistic UI for message sending with error recovery"

### UX Design Thinking
1. "How would you design the experience when the AI agent doesn't know the answer?"
2. "How would you handle the transition from AI agent to human agent seamlessly?"
3. "Design the onboarding experience for a new AI assistant in Photoshop"
4. "How would you build trust with users who are skeptical of AI responses?"

### Performance
1. "How would you optimize a chat interface that handles 100+ messages?"
2. "How would you reduce perceived latency when LLM takes 5+ seconds?"
3. "How would you handle offline/poor connectivity for the agent interface?"

### Adobe-Specific
1. "How would you integrate an AI agent into the Photoshop toolbar?"
2. "Design a contextual help agent that understands what the user is doing in Premiere Pro"
3. "How would you handle multi-modal input (screenshots + text) in the agent UI?"

---

## Next: [Round 3 — Product Thinking, Outcomes & Leadership →](Round3_Product_Leadership.md)
