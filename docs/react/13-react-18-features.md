# React 18 Features

## Concurrent Rendering

React 18 introduces concurrent rendering, allowing React to work on multiple tasks simultaneously and interrupt rendering work.

### Benefits
- Better user experience
- Smoother interactions
- Automatic batching
- Transitions for non-urgent updates

## Automatic Batching

React 18 batches all state updates, even in async functions, timeouts, and event handlers.

### React 17 (No Batching in Async)
```jsx
// React 17 - 2 re-renders
setTimeout(() => {
  setCount(c => c + 1); // Re-render 1
  setFlag(f => !f);     // Re-render 2
}, 1000);
```

### React 18 (Automatic Batching)
```jsx
// React 18 - 1 re-render
setTimeout(() => {
  setCount(c => c + 1);
  setFlag(f => !f);
  // Batched into single re-render
}, 1000);
```

### Opt-out of Batching
```jsx
import { flushSync } from 'react-dom';

flushSync(() => {
  setCount(c => c + 1);
}); // Re-render immediately

setFlag(f => !f); // Re-render again
```

## Transitions

Mark updates as non-urgent to keep UI responsive.

### useTransition Hook
```jsx
import { useState, useTransition } from 'react';

function SearchResults() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [isPending, startTransition] = useTransition();
  
  const handleChange = (e) => {
    const value = e.target.value;
    setQuery(value); // Urgent update
    
    startTransition(() => {
      // Non-urgent update
      const filtered = filterResults(value);
      setResults(filtered);
    });
  };
  
  return (
    <>
      <input value={query} onChange={handleChange} />
      {isPending && <Spinner />}
      <ResultList results={results} />
    </>
  );
}
```

### useDeferredValue Hook
```jsx
import { useState, useDeferredValue } from 'react';

function SearchResults() {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  
  // deferredQuery updates with lower priority
  const results = useMemo(() => {
    return filterResults(deferredQuery);
  }, [deferredQuery]);
  
  return (
    <>
      <input value={query} onChange={(e) => setQuery(e.target.value)} />
      <ResultList results={results} />
    </>
  );
}
```

## Suspense Improvements

### Suspense for Data Fetching
```jsx
import { Suspense } from 'react';

function App() {
  return (
    <Suspense fallback={<Loading />}>
      <UserProfile />
    </Suspense>
  );
}

// Component that suspends
function UserProfile() {
  const user = use(fetchUser()); // Suspends until data loads
  return <div>{user.name}</div>;
}
```

### Nested Suspense
```jsx
function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Header />
      <Suspense fallback={<SidebarLoader />}>
        <Sidebar />
      </Suspense>
      <Suspense fallback={<ContentLoader />}>
        <Content />
      </Suspense>
    </Suspense>
  );
}
```

### Suspense with Transitions
```jsx
function App() {
  const [tab, setTab] = useState('home');
  const [isPending, startTransition] = useTransition();
  
  const selectTab = (nextTab) => {
    startTransition(() => {
      setTab(nextTab);
    });
  };
  
  return (
    <>
      <button onClick={() => selectTab('home')}>Home</button>
      <button onClick={() => selectTab('profile')}>Profile</button>
      
      <Suspense fallback={<Spinner />}>
        {isPending && <Spinner />}
        {tab === 'home' && <Home />}
        {tab === 'profile' && <Profile />}
      </Suspense>
    </>
  );
}
```

## New Root API

### React 17 (Legacy)
```jsx
import ReactDOM from 'react-dom';

ReactDOM.render(<App />, document.getElementById('root'));
```

### React 18 (Concurrent)
```jsx
import { createRoot } from 'react-dom/client';

const root = createRoot(document.getElementById('root'));
root.render(<App />);
```

## useId Hook

Generate unique IDs for accessibility.

```jsx
import { useId } from 'react';

function FormField() {
  const id = useId();
  
  return (
    <>
      <label htmlFor={id}>Email</label>
      <input id={id} type="email" />
    </>
  );
}

// Multiple fields
function Form() {
  const id = useId();
  
  return (
    <>
      <label htmlFor={`${id}-email`}>Email</label>
      <input id={`${id}-email`} type="email" />
      
      <label htmlFor={`${id}-password`}>Password</label>
      <input id={`${id}-password`} type="password" />
    </>
  );
}
```

## useSyncExternalStore

Subscribe to external stores (for library authors).

```jsx
import { useSyncExternalStore } from 'react';

function useOnlineStatus() {
  const isOnline = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot
  );
  
  return isOnline;
}

function subscribe(callback) {
  window.addEventListener('online', callback);
  window.addEventListener('offline', callback);
  return () => {
    window.removeEventListener('online', callback);
    window.removeEventListener('offline', callback);
  };
}

function getSnapshot() {
  return navigator.onLine;
}

function getServerSnapshot() {
  return true; // Always online during SSR
}

// Usage
function App() {
  const isOnline = useOnlineStatus();
  return <div>{isOnline ? 'Online' : 'Offline'}</div>;
}
```

## useInsertionEffect

For CSS-in-JS libraries (runs before DOM mutations).

```jsx
import { useInsertionEffect } from 'react';

function useCSS(rule) {
  useInsertionEffect(() => {
    // Insert CSS before DOM updates
    const style = document.createElement('style');
    style.textContent = rule;
    document.head.appendChild(style);
    
    return () => {
      document.head.removeChild(style);
    };
  }, [rule]);
}
```

## Strict Mode Improvements

React 18 Strict Mode simulates mounting, unmounting, and remounting components to catch bugs.

```jsx
import { StrictMode } from 'react';

function App() {
  return (
    <StrictMode>
      <Component />
    </StrictMode>
  );
}

// Component lifecycle in Strict Mode (dev only)
// 1. Mount
// 2. Unmount
// 3. Remount
```

## Server Components (Experimental)

```jsx
// Server Component (runs on server)
async function UserProfile({ userId }) {
  const user = await db.users.find(userId);
  return <div>{user.name}</div>;
}

// Client Component
'use client';

function InteractiveButton() {
  const [count, setCount] = useState(0);
  return <button onClick={() => setCount(count + 1)}>{count}</button>;
}
```

## Interview Questions

**Q: What's new in React 18?**
- Concurrent rendering
- Automatic batching
- Transitions (useTransition, useDeferredValue)
- Suspense improvements
- New hooks (useId, useSyncExternalStore, useInsertionEffect)

**Q: What is concurrent rendering?**
- React can work on multiple tasks simultaneously
- Can interrupt and resume rendering
- Improves responsiveness and user experience

**Q: What is automatic batching?**
- Multiple state updates batched into single re-render
- Works in async functions, timeouts, events
- Improves performance

**Q: useTransition vs useDeferredValue?**
- useTransition: Mark state updates as non-urgent
- useDeferredValue: Defer value updates
- Both keep UI responsive during heavy updates

**Q: What is the new root API?**
- createRoot() replaces ReactDOM.render()
- Enables concurrent features
- Required for React 18 features

**Q: What is useId for?**
- Generate unique IDs for accessibility
- Consistent between server and client
- Useful for form fields

**Q: What is useSyncExternalStore?**
- Subscribe to external stores
- For library authors (Redux, Zustand)
- Ensures consistency with concurrent rendering

**Q: What is Strict Mode in React 18?**
- Simulates mount/unmount/remount
- Helps catch bugs with effects
- Development mode only

**Q: What are Server Components?**
- Components that run on server
- Zero JavaScript sent to client
- Direct database access
- Experimental feature
