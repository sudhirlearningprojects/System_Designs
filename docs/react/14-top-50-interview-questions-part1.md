# Top 50 Interview Questions for Senior Full-Stack Engineers - Part 1

## React & Frontend (Questions 1-15)

### 1. Explain the Virtual DOM and how React's reconciliation algorithm works. How does React Fiber improve this?

**Answer:**
Virtual DOM is a lightweight JavaScript representation of the actual DOM. When state changes:
1. New Virtual DOM tree is created
2. Diffing algorithm compares with previous tree
3. Minimal changes calculated
4. Real DOM updated efficiently

**React Fiber** (React 16+) improvements:
- **Incremental rendering**: Break work into chunks
- **Pause/Resume**: Can pause work and resume later
- **Priority-based updates**: Urgent updates processed first
- **Two phases**: Render phase (interruptible) and Commit phase (synchronous)

**Key optimizations:**
- O(n) algorithm instead of O(n³)
- Same element type = update props only
- Different element type = destroy and rebuild
- Keys help identify stable elements

---

### 2. What are the differences between useMemo, useCallback, and React.memo? When would you use each?

**Answer:**

**useMemo**: Memoizes computed values
```jsx
const sortedList = useMemo(() => {
  return items.sort((a, b) => a - b);
}, [items]);
```
Use when: Expensive computations, referential equality needed

**useCallback**: Memoizes function references
```jsx
const handleClick = useCallback(() => {
  doSomething(a, b);
}, [a, b]);
```
Use when: Passing callbacks to optimized child components

**React.memo**: Memoizes entire component
```jsx
const MyComponent = React.memo(({ data }) => {
  return <div>{data}</div>;
});
```
Use when: Component re-renders with same props

**Key difference**: useMemo returns memoized value, useCallback returns memoized function, React.memo wraps component.

---

### 3. Explain the difference between Controlled and Uncontrolled components. Which is preferred and why?

**Answer:**

**Controlled Components:**
- Form data handled by React state
- Single source of truth
- Enables validation, conditional rendering, dynamic behavior

```jsx
const [value, setValue] = useState('');
<input value={value} onChange={(e) => setValue(e.target.value)} />
```

**Uncontrolled Components:**
- Form data handled by DOM
- Use refs to access values
- Less React code

```jsx
const inputRef = useRef();
<input ref={inputRef} defaultValue="initial" />
// Access: inputRef.current.value
```

**Preferred**: Controlled components
- Better control and validation
- Easier testing
- Consistent with React's declarative nature
- Exception: File inputs (always uncontrolled)

---

### 4. How would you optimize a React application with performance issues? Describe your approach.

**Answer:**

**1. Identify bottlenecks:**
- React DevTools Profiler
- Chrome Performance tab
- Lighthouse audit

**2. Component-level optimizations:**
```jsx
// Memoize expensive components
const ExpensiveComponent = React.memo(Component);

// Memoize values and callbacks
const value = useMemo(() => compute(data), [data]);
const callback = useCallback(() => action(), [deps]);
```

**3. Code splitting:**
```jsx
const HeavyComponent = lazy(() => import('./Heavy'));
<Suspense fallback={<Loading />}>
  <HeavyComponent />
</Suspense>
```

**4. List optimization:**
- Use proper keys (not index)
- Virtualization (react-window) for long lists
- Pagination or infinite scroll

**5. Bundle optimization:**
- Tree shaking
- Dynamic imports
- Analyze bundle with webpack-bundle-analyzer

**6. State management:**
- Split contexts to prevent unnecessary re-renders
- Use state colocation (keep state close to where it's used)
- Consider state management libraries for complex apps

**7. Network optimization:**
- API response caching
- Debounce/throttle API calls
- Prefetch data
- Use CDN for static assets

---

### 5. Explain React's Context API. What are its limitations and when would you use Redux instead?

**Answer:**

**Context API:**
```jsx
const ThemeContext = createContext();

function App() {
  const [theme, setTheme] = useState('light');
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <Component />
    </ThemeContext.Provider>
  );
}
```

**Limitations:**
1. **Performance**: All consumers re-render when context value changes
2. **No middleware**: Can't intercept actions
3. **No DevTools**: Limited debugging capabilities
4. **No time-travel**: Can't replay state changes
5. **Boilerplate**: Need to create providers for each context

**Use Redux when:**
- Complex state logic with many actions
- Need middleware (logging, async, persistence)
- Time-travel debugging required
- Large team needs predictable state management
- State shared across many components

**Use Context when:**
- Simple global state (theme, auth, locale)
- Low-frequency updates
- Small to medium applications
- Want to avoid external dependencies

**Hybrid approach**: Context for simple state, Redux for complex business logic

---

### 6. What are Higher-Order Components (HOC)? Why are hooks preferred over HOCs now?

**Answer:**

**HOC**: Function that takes a component and returns enhanced component
```jsx
function withAuth(Component) {
  return function WithAuth(props) {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) return <Redirect to="/login" />;
    return <Component {...props} />;
  };
}

const ProtectedDashboard = withAuth(Dashboard);
```

**Problems with HOCs:**
1. **Wrapper hell**: Multiple HOCs create deep nesting
2. **Prop collisions**: HOCs might override props
3. **Static composition**: Must wrap at definition time
4. **Ref forwarding**: Need forwardRef for refs
5. **Hard to debug**: DevTools show wrapper components

**Why Hooks are preferred:**
```jsx
function Dashboard() {
  const { isAuthenticated } = useAuth(); // Cleaner!
  if (!isAuthenticated) return <Redirect to="/login" />;
  return <div>Dashboard</div>;
}
```

**Advantages:**
- No wrapper components
- Easier to understand and debug
- Better TypeScript support
- Compose logic without nesting
- Reuse stateful logic easily

**When to still use HOCs:**
- Legacy codebases
- Third-party libraries
- Cross-cutting concerns affecting render

---

### 7. Explain React's reconciliation process and the importance of keys in lists.

**Answer:**

**Reconciliation Process:**
1. State/props change triggers re-render
2. New Virtual DOM tree created
3. Diffing algorithm compares trees
4. Minimal set of DOM operations calculated
5. Real DOM updated

**Key Heuristics:**
- Different element types → destroy and rebuild
- Same element type → update props only
- Component elements → update props, re-render

**Keys in Lists:**

**Without keys (inefficient):**
```jsx
// React can't track which items changed
{items.map(item => <Item data={item} />)}
```

**With index as key (problematic):**
```jsx
// Breaks when list reorders
{items.map((item, i) => <Item key={i} data={item} />)}
```

**With unique ID (correct):**
```jsx
// React efficiently tracks changes
{items.map(item => <Item key={item.id} data={item} />)}
```

**Why keys matter:**
1. **Performance**: React reuses DOM nodes instead of recreating
2. **State preservation**: Component state maintained correctly
3. **Correct updates**: Prevents bugs during reordering

**Key requirements:**
- Unique among siblings
- Stable (don't use Math.random())
- Predictable (don't generate on render)

---

### 8. How do you handle error boundaries in React? What errors do they NOT catch?

**Answer:**

**Error Boundary Implementation:**
```jsx
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  
  static getDerivedStateFromError(error) {
    return { hasError: true };
  }
  
  componentDidCatch(error, errorInfo) {
    // Log to error service
    logErrorToService(error, errorInfo);
  }
  
  render() {
    if (this.state.hasError) {
      return <ErrorFallback />;
    }
    return this.props.children;
  }
}

// Usage
<ErrorBoundary>
  <App />
</ErrorBoundary>
```

**Errors NOT caught:**
- Event handlers (use try-catch)
- Asynchronous code (setTimeout, promises)
- Server-side rendering
- Errors in error boundary itself

**Event handler error handling:**
```jsx
function Component() {
  const [error, setError] = useState(null);
  
  const handleClick = () => {
    try {
      riskyOperation();
    } catch (err) {
      setError(err);
    }
  };
  
  if (error) return <ErrorDisplay error={error} />;
  return <button onClick={handleClick}>Click</button>;
}
```

**Best practices:**
- Multiple error boundaries for granular control
- Log errors to monitoring service
- User-friendly error messages
- Provide recovery mechanism (reset button)

---

### 9. Explain React 18's concurrent features: useTransition and useDeferredValue.

**Answer:**

**useTransition**: Mark updates as non-urgent
```jsx
function SearchResults() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [isPending, startTransition] = useTransition();
  
  const handleChange = (e) => {
    setQuery(e.target.value); // Urgent - update input immediately
    
    startTransition(() => {
      // Non-urgent - can be interrupted
      const filtered = expensiveFilter(e.target.value);
      setResults(filtered);
    });
  };
  
  return (
    <>
      <input value={query} onChange={handleChange} />
      {isPending && <Spinner />}
      <List results={results} />
    </>
  );
}
```

**useDeferredValue**: Defer value updates
```jsx
function SearchResults() {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  
  // deferredQuery updates with lower priority
  const results = useMemo(() => {
    return expensiveFilter(deferredQuery);
  }, [deferredQuery]);
  
  return (
    <>
      <input value={query} onChange={(e) => setQuery(e.target.value)} />
      <List results={results} />
    </>
  );
}
```

**Key differences:**
- **useTransition**: Control when update happens (wrap setState)
- **useDeferredValue**: Defer value itself (wrap value)
- **useTransition**: Provides isPending flag
- **useDeferredValue**: Simpler API, no pending state

**Use cases:**
- Heavy computations
- Large list filtering
- Complex UI updates
- Keeping input responsive

---

### 10. How would you implement code splitting and lazy loading in a React application?

**Answer:**

**1. Route-based code splitting:**
```jsx
import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

const Home = lazy(() => import('./pages/Home'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Profile = lazy(() => import('./pages/Profile'));

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
```

**2. Component-based code splitting:**
```jsx
const HeavyChart = lazy(() => import('./HeavyChart'));

function Dashboard() {
  const [showChart, setShowChart] = useState(false);
  
  return (
    <div>
      <button onClick={() => setShowChart(true)}>Show Chart</button>
      {showChart && (
        <Suspense fallback={<ChartLoader />}>
          <HeavyChart />
        </Suspense>
      )}
    </div>
  );
}
```

**3. Named exports:**
```jsx
// components.js
export const ComponentA = () => <div>A</div>;
export const ComponentB = () => <div>B</div>;

// App.js
const ComponentA = lazy(() => 
  import('./components').then(module => ({ default: module.ComponentA }))
);
```

**4. Preloading:**
```jsx
const HeavyComponent = lazy(() => import('./Heavy'));

// Preload on hover
<button onMouseEnter={() => import('./Heavy')}>
  Show Heavy Component
</button>
```

**5. Error boundaries with Suspense:**
```jsx
<ErrorBoundary fallback={<ErrorPage />}>
  <Suspense fallback={<Loading />}>
    <LazyComponent />
  </Suspense>
</ErrorBoundary>
```

**Benefits:**
- Smaller initial bundle
- Faster initial load
- Better performance
- Load on demand

---

### 11. Explain the differences between server-side rendering (SSR), static site generation (SSG), and client-side rendering (CSR).

**Answer:**

**Client-Side Rendering (CSR):**
```jsx
// Traditional React app
// HTML: <div id="root"></div>
// JavaScript renders everything
```
- **Pros**: Rich interactions, no server load, easy deployment
- **Cons**: Slow initial load, poor SEO, blank page until JS loads
- **Use case**: Dashboards, admin panels, authenticated apps

**Server-Side Rendering (SSR):**
```jsx
// Next.js with getServerSideProps
export async function getServerSideProps(context) {
  const data = await fetchData();
  return { props: { data } };
}

function Page({ data }) {
  return <div>{data}</div>;
}
```
- **Pros**: Fast initial load, good SEO, dynamic content
- **Cons**: Server load, slower navigation, complex caching
- **Use case**: E-commerce, news sites, personalized content

**Static Site Generation (SSG):**
```jsx
// Next.js with getStaticProps
export async function getStaticProps() {
  const data = await fetchData();
  return { props: { data }, revalidate: 60 };
}

function Page({ data }) {
  return <div>{data}</div>;
}
```
- **Pros**: Fastest load, great SEO, cheap hosting (CDN)
- **Cons**: Build time increases, stale data, rebuild for updates
- **Use case**: Blogs, documentation, marketing sites

**Incremental Static Regeneration (ISR):**
```jsx
export async function getStaticProps() {
  return {
    props: { data },
    revalidate: 60 // Regenerate every 60 seconds
  };
}
```
- Best of both worlds: static + fresh data

**Comparison:**

| Feature | CSR | SSR | SSG |
|---------|-----|-----|-----|
| Initial Load | Slow | Fast | Fastest |
| SEO | Poor | Good | Best |
| Server Load | None | High | None |
| Dynamic Data | Easy | Easy | Hard |
| Cost | Low | High | Low |

---

### 12. How do you manage global state in a large React application? Compare different approaches.

**Answer:**

**1. Context API:**
```jsx
const AppContext = createContext();

function AppProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  return (
    <AppContext.Provider value={{ state, dispatch }}>
      {children}
    </AppContext.Provider>
  );
}
```
- **Pros**: Built-in, no dependencies, simple
- **Cons**: Performance issues, no middleware, limited DevTools
- **Use case**: Small to medium apps, simple state

**2. Redux:**
```jsx
// Store
const store = configureStore({
  reducer: {
    user: userReducer,
    posts: postsReducer
  },
  middleware: [thunk, logger]
});

// Component
const user = useSelector(state => state.user);
const dispatch = useDispatch();
```
- **Pros**: Predictable, middleware, DevTools, time-travel
- **Cons**: Boilerplate, learning curve, overkill for simple apps
- **Use case**: Large apps, complex state, team collaboration

**3. Zustand:**
```jsx
const useStore = create((set) => ({
  count: 0,
  increment: () => set((state) => ({ count: state.count + 1 }))
}));

function Component() {
  const { count, increment } = useStore();
}
```
- **Pros**: Minimal boilerplate, no providers, good performance
- **Cons**: Less ecosystem, smaller community
- **Use case**: Medium apps, want simplicity with power

**4. Recoil:**
```jsx
const countState = atom({
  key: 'count',
  default: 0
});

function Component() {
  const [count, setCount] = useRecoilState(countState);
}
```
- **Pros**: Atomic state, derived state, async support
- **Cons**: Experimental, Facebook-specific patterns
- **Use case**: Complex derived state, async state

**5. Jotai:**
```jsx
const countAtom = atom(0);

function Component() {
  const [count, setCount] = useAtom(countAtom);
}
```
- **Pros**: Minimal, atomic, TypeScript-first
- **Cons**: Newer, smaller ecosystem
- **Use case**: Modern apps, TypeScript projects

**Recommendation for large apps:**
- **Redux Toolkit**: Complex business logic, large teams
- **Zustand**: Balance of simplicity and power
- **Context + useReducer**: Simple global state
- **Combination**: Context for UI state, Redux for business logic

---

### 13. What are React Server Components? How do they differ from traditional components?

**Answer:**

**Server Components** (React 18+):
```jsx
// UserProfile.server.js
async function UserProfile({ userId }) {
  // Runs ONLY on server
  const user = await db.users.find(userId);
  const posts = await db.posts.findByUser(userId);
  
  return (
    <div>
      <h1>{user.name}</h1>
      <PostList posts={posts} />
    </div>
  );
}
```

**Client Components:**
```jsx
'use client'; // Explicit directive

function InteractiveButton() {
  const [count, setCount] = useState(0);
  return <button onClick={() => setCount(count + 1)}>{count}</button>;
}
```

**Key Differences:**

| Feature | Server Components | Client Components |
|---------|------------------|-------------------|
| Runs on | Server only | Client (browser) |
| Bundle size | Zero JS to client | Included in bundle |
| Data fetching | Direct DB access | API calls |
| State | No state | useState, useReducer |
| Effects | No effects | useEffect |
| Event handlers | No | Yes |
| Browser APIs | No | Yes |

**Benefits:**
1. **Zero bundle size**: Server components don't ship to client
2. **Direct data access**: Query DB directly, no API layer
3. **Automatic code splitting**: Only client components bundled
4. **Better performance**: Less JavaScript to download
5. **Security**: Keep sensitive logic on server

**Limitations:**
- No state or effects
- No event handlers
- No browser APIs
- Can't use Context (yet)

**Composition:**
```jsx
// Server Component
async function Page() {
  const data = await fetchData();
  
  return (
    <div>
      <ServerComponent data={data} />
      <ClientComponent initialData={data} />
    </div>
  );
}
```

**Use cases:**
- Data-heavy pages
- SEO-critical content
- Reducing bundle size
- Secure operations

---

### 14. How would you implement infinite scrolling in React? What are the performance considerations?

**Answer:**

**1. Intersection Observer approach:**
```jsx
function InfiniteScroll() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const observerRef = useRef();
  
  const lastItemRef = useCallback((node) => {
    if (loading) return;
    if (observerRef.current) observerRef.current.disconnect();
    
    observerRef.current = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prev => prev + 1);
      }
    });
    
    if (node) observerRef.current.observe(node);
  }, [loading, hasMore]);
  
  useEffect(() => {
    setLoading(true);
    fetch(`/api/items?page=${page}`)
      .then(res => res.json())
      .then(data => {
        setItems(prev => [...prev, ...data.items]);
        setHasMore(data.hasMore);
        setLoading(false);
      });
  }, [page]);
  
  return (
    <div>
      {items.map((item, index) => {
        if (items.length === index + 1) {
          return <div ref={lastItemRef} key={item.id}>{item.name}</div>;
        }
        return <div key={item.id}>{item.name}</div>;
      })}
      {loading && <Spinner />}
    </div>
  );
}
```

**2. With react-infinite-scroll-component:**
```jsx
import InfiniteScroll from 'react-infinite-scroll-component';

function Feed() {
  const [items, setItems] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  
  const fetchMore = () => {
    fetch(`/api/items?page=${items.length / 20}`)
      .then(res => res.json())
      .then(data => {
        setItems([...items, ...data.items]);
        setHasMore(data.hasMore);
      });
  };
  
  return (
    <InfiniteScroll
      dataLength={items.length}
      next={fetchMore}
      hasMore={hasMore}
      loader={<Spinner />}
      endMessage={<p>No more items</p>}
    >
      {items.map(item => <Item key={item.id} data={item} />)}
    </InfiniteScroll>
  );
}
```

**Performance Considerations:**

**1. Virtualization for large lists:**
```jsx
import { FixedSizeList } from 'react-window';

function VirtualizedList({ items }) {
  return (
    <FixedSizeList
      height={600}
      itemCount={items.length}
      itemSize={100}
      width="100%"
    >
      {({ index, style }) => (
        <div style={style}>{items[index].name}</div>
      )}
    </FixedSizeList>
  );
}
```

**2. Debounce scroll events:**
```jsx
const debouncedScroll = useMemo(
  () => debounce(handleScroll, 200),
  []
);
```

**3. Memoize list items:**
```jsx
const Item = React.memo(({ data }) => {
  return <div>{data.name}</div>;
});
```

**4. Pagination strategy:**
- Load 20-50 items per page
- Preload next page when 80% scrolled
- Cache previous pages

**5. Memory management:**
- Remove items far from viewport
- Limit total items in memory
- Use virtual scrolling for 1000+ items

---

### 15. Explain React's Suspense and how it works with data fetching.

**Answer:**

**Basic Suspense:**
```jsx
import { Suspense, lazy } from 'react';

const LazyComponent = lazy(() => import('./Heavy'));

function App() {
  return (
    <Suspense fallback={<Loading />}>
      <LazyComponent />
    </Suspense>
  );
}
```

**Suspense for Data Fetching (React 18+):**
```jsx
// Resource that suspends
function fetchUser(userId) {
  let status = 'pending';
  let result;
  
  const suspender = fetch(`/api/users/${userId}`)
    .then(res => res.json())
    .then(data => {
      status = 'success';
      result = data;
    })
    .catch(err => {
      status = 'error';
      result = err;
    });
  
  return {
    read() {
      if (status === 'pending') throw suspender;
      if (status === 'error') throw result;
      return result;
    }
  };
}

// Component that suspends
function UserProfile({ userId }) {
  const user = userResource.read(); // Suspends here
  return <div>{user.name}</div>;
}

// Usage
function App() {
  return (
    <Suspense fallback={<Loading />}>
      <UserProfile userId="123" />
    </Suspense>
  );
}
```

**Nested Suspense:**
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

**Suspense with Transitions:**
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
      <Tabs onSelect={selectTab} />
      <Suspense fallback={<Spinner />}>
        {isPending && <InlineSpinner />}
        {tab === 'home' && <Home />}
        {tab === 'profile' && <Profile />}
      </Suspense>
    </>
  );
}
```

**With React Query (practical approach):**
```jsx
import { Suspense } from 'react';
import { useQuery } from '@tanstack/react-query';

function UserProfile({ userId }) {
  const { data } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => fetchUser(userId),
    suspense: true // Enable Suspense
  });
  
  return <div>{data.name}</div>;
}

function App() {
  return (
    <Suspense fallback={<Loading />}>
      <UserProfile userId="123" />
    </Suspense>
  );
}
```

**Benefits:**
- Declarative loading states
- Automatic error boundaries
- Better UX with transitions
- Coordinate multiple async operations
- Avoid loading state management

**Current limitations:**
- Not all data fetching libraries support it
- Server-side rendering complexity
- Error handling needs error boundaries

---

**Continue to Part 2 for Backend, System Design, and Architecture questions (16-50)...**
