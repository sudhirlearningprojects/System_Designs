# Performance Optimization

## React Rendering Behavior

React re-renders a component when:
1. State changes (setState/useState)
2. Props change
3. Parent component re-renders
4. Context value changes
5. forceUpdate() is called

## Optimization Techniques

### 1. React.memo

Prevents re-renders if props haven't changed (shallow comparison).

```jsx
const ExpensiveComponent = React.memo(function ExpensiveComponent({ data }) {
  console.log('Rendering...');
  return <div>{data}</div>;
});

// Custom comparison function
const Component = React.memo(
  function Component({ user }) {
    return <div>{user.name}</div>;
  },
  (prevProps, nextProps) => {
    // Return true if props are equal (skip render)
    return prevProps.user.id === nextProps.user.id;
  }
);
```

### 2. useMemo

Memoizes expensive computations.

```jsx
function Component({ items }) {
  // Only recalculates when items change
  const sortedItems = useMemo(() => {
    console.log('Sorting...');
    return items.sort((a, b) => a.value - b.value);
  }, [items]);
  
  return <List items={sortedItems} />;
}
```

### 3. useCallback

Memoizes function references.

```jsx
function Parent() {
  const [count, setCount] = useState(0);
  
  // Without useCallback - new function on every render
  const handleClick = () => {
    console.log('Clicked');
  };
  
  // With useCallback - same function reference
  const handleClickMemo = useCallback(() => {
    console.log('Clicked');
  }, []);
  
  return <Child onClick={handleClickMemo} />;
}

const Child = React.memo(function Child({ onClick }) {
  console.log('Child rendered');
  return <button onClick={onClick}>Click</button>;
});
```

### 4. Code Splitting

Split code into smaller chunks loaded on demand.

```jsx
import { lazy, Suspense } from 'react';

// Lazy load component
const HeavyComponent = lazy(() => import('./HeavyComponent'));

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <HeavyComponent />
    </Suspense>
  );
}
```

### 5. Virtualization

Render only visible items in long lists.

```jsx
import { FixedSizeList } from 'react-window';

function VirtualList({ items }) {
  const Row = ({ index, style }) => (
    <div style={style}>{items[index]}</div>
  );
  
  return (
    <FixedSizeList
      height={600}
      itemCount={items.length}
      itemSize={35}
      width="100%"
    >
      {Row}
    </FixedSizeList>
  );
}
```

### 6. Debouncing and Throttling

```jsx
import { useState, useCallback } from 'react';
import { debounce } from 'lodash';

function SearchInput() {
  const [query, setQuery] = useState('');
  
  // Debounce API call
  const debouncedSearch = useCallback(
    debounce((value) => {
      // API call
      fetch(`/api/search?q=${value}`);
    }, 500),
    []
  );
  
  const handleChange = (e) => {
    const value = e.target.value;
    setQuery(value);
    debouncedSearch(value);
  };
  
  return <input value={query} onChange={handleChange} />;
}
```

### 7. Avoid Inline Functions and Objects

```jsx
// Bad - creates new function/object on every render
function Parent() {
  return (
    <Child 
      onClick={() => console.log('click')}
      style={{ color: 'red' }}
    />
  );
}

// Good - stable references
function Parent() {
  const handleClick = useCallback(() => {
    console.log('click');
  }, []);
  
  const style = useMemo(() => ({ color: 'red' }), []);
  
  return <Child onClick={handleClick} style={style} />;
}
```

### 8. Key Prop Optimization

```jsx
// Bad - using index
{items.map((item, index) => (
  <Item key={index} data={item} />
))}

// Good - using unique ID
{items.map(item => (
  <Item key={item.id} data={item} />
))}
```

### 9. Lazy State Initialization

```jsx
// Bad - runs on every render
const [state, setState] = useState(expensiveComputation());

// Good - runs only once
const [state, setState] = useState(() => expensiveComputation());
```

### 10. Fragment Instead of Div

```jsx
// Bad - extra DOM node
return (
  <div>
    <Child1 />
    <Child2 />
  </div>
);

// Good - no extra DOM node
return (
  <>
    <Child1 />
    <Child2 />
  </>
);
```

## Profiling Performance

### React DevTools Profiler

```jsx
import { Profiler } from 'react';

function onRenderCallback(
  id, // component id
  phase, // "mount" or "update"
  actualDuration, // time spent rendering
  baseDuration, // estimated time without memoization
  startTime,
  commitTime,
  interactions
) {
  console.log(`${id} took ${actualDuration}ms`);
}

function App() {
  return (
    <Profiler id="App" onRender={onRenderCallback}>
      <Component />
    </Profiler>
  );
}
```

## Common Performance Pitfalls

### 1. Unnecessary Re-renders
```jsx
// Problem: Parent re-renders cause child re-renders
function Parent() {
  const [count, setCount] = useState(0);
  return (
    <>
      <button onClick={() => setCount(count + 1)}>{count}</button>
      <ExpensiveChild /> {/* Re-renders unnecessarily */}
    </>
  );
}

// Solution: Memoize child
const ExpensiveChild = React.memo(function ExpensiveChild() {
  return <div>Expensive computation</div>;
});
```

### 2. Large Bundle Size
```jsx
// Problem: Import entire library
import _ from 'lodash';

// Solution: Import only what you need
import debounce from 'lodash/debounce';
```

### 3. Unoptimized Images
```jsx
// Use next/image or lazy loading
<img loading="lazy" src="image.jpg" alt="description" />
```

## Interview Questions

**Q: What is React.memo?**
- HOC that memoizes component
- Prevents re-render if props unchanged
- Uses shallow comparison by default

**Q: useMemo vs useCallback?**
- useMemo: Memoizes computed values
- useCallback: Memoizes function references
- Both take dependencies array

**Q: When to use useMemo?**
- Expensive computations
- Referential equality needed for child props
- Avoid premature optimization

**Q: What is code splitting?**
- Breaking bundle into smaller chunks
- Load code on demand
- Improves initial load time

**Q: How to optimize list rendering?**
- Use proper keys (not index)
- Virtualization for long lists
- Memoize list items
- Pagination or infinite scroll

**Q: What causes unnecessary re-renders?**
- Parent re-renders
- New object/function references in props
- Context value changes
- Inline functions/objects

**Q: How to measure React performance?**
- React DevTools Profiler
- Chrome DevTools Performance tab
- Lighthouse
- Web Vitals

**Q: What is lazy loading?**
- Loading components/resources on demand
- Reduces initial bundle size
- Uses React.lazy() and Suspense
