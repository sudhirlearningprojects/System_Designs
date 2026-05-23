# Round 2 — Frontend & Experience Engineering

## What They're Evaluating

- Can you build intuitive conversational interfaces?
- Do you understand UX principles for AI-powered products?
- Can you handle streaming responses, real-time updates, and accessibility?
- Do you think about error states, loading states, and edge cases in AI UIs?

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
