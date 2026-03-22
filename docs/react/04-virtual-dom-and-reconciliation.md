# Virtual DOM and Reconciliation

## What is Virtual DOM?

Virtual DOM is a lightweight JavaScript representation of the actual DOM. It's a tree of JavaScript objects that mirrors the real DOM structure.

### Why Virtual DOM?

1. **Performance**: Direct DOM manipulation is slow
2. **Batching**: Multiple updates can be batched together
3. **Diffing**: Only changed elements are updated in real DOM
4. **Cross-platform**: Same code can target different platforms (web, mobile)

## How Virtual DOM Works

```
1. State/Props Change
   ↓
2. Create New Virtual DOM Tree
   ↓
3. Diff with Previous Virtual DOM (Reconciliation)
   ↓
4. Calculate Minimal Changes
   ↓
5. Update Real DOM (Commit Phase)
```

## Reconciliation Algorithm

React uses a heuristic O(n) algorithm instead of O(n³) tree diff.

### Key Assumptions

1. **Different element types produce different trees**
```jsx
// Old
<div><Counter /></div>

// New
<span><Counter /></span>

// Result: Entire subtree destroyed and rebuilt
```

2. **Keys help identify stable elements**
```jsx
// Without keys - inefficient
<ul>
  <li>A</li>
  <li>B</li>
</ul>

// With keys - efficient
<ul>
  <li key="a">A</li>
  <li key="b">B</li>
</ul>
```

### Diffing Process

#### Same Element Type
```jsx
// Old
<div className="before" title="old" />

// New
<div className="after" title="new" />

// Result: Only update changed attributes
```

#### Different Element Type
```jsx
// Old
<div><Counter /></div>

// New
<span><Counter /></span>

// Result: Unmount old, mount new (Counter loses state)
```

#### Component Elements
```jsx
// Old
<Counter count={1} />

// New
<Counter count={2} />

// Result: Update props, component re-renders
```

## Keys in Lists

### Why Keys Matter
```jsx
// Bad - using index as key
{items.map((item, index) => (
  <Item key={index} data={item} />
))}

// Good - using unique ID
{items.map(item => (
  <Item key={item.id} data={item} />
))}
```

### Problems with Index as Key
- Reordering breaks component state
- Performance issues on insertions/deletions
- Incorrect component reuse

### Key Rules
1. Keys must be unique among siblings
2. Keys should be stable (not random)
3. Keys should be predictable (not generated on render)

## React Fiber

Modern reconciliation engine (React 16+).

### Features

1. **Incremental Rendering**: Break work into chunks
2. **Pause/Resume**: Pause work and resume later
3. **Priority**: Assign priority to different updates
4. **Concurrency**: Handle multiple state updates

### Phases

#### 1. Render Phase (Interruptible)
- Build work-in-progress tree
- Calculate changes
- Can be paused/aborted

#### 2. Commit Phase (Synchronous)
- Apply changes to DOM
- Run lifecycle methods
- Cannot be interrupted

## Optimization Techniques

### 1. React.memo
Prevents re-render if props haven't changed.

```jsx
const MyComponent = React.memo(function MyComponent(props) {
  return <div>{props.value}</div>;
});

// Custom comparison
const MyComponent = React.memo(Component, (prevProps, nextProps) => {
  return prevProps.id === nextProps.id;
});
```

### 2. PureComponent (Class)
```jsx
class MyComponent extends React.PureComponent {
  // Shallow prop/state comparison
}
```

### 3. shouldComponentUpdate (Class)
```jsx
shouldComponentUpdate(nextProps, nextState) {
  return this.props.id !== nextProps.id;
}
```

### 4. useMemo & useCallback
```jsx
const memoizedValue = useMemo(() => computeExpensive(a, b), [a, b]);
const memoizedCallback = useCallback(() => doSomething(a, b), [a, b]);
```

## Interview Questions

**Q: What is Virtual DOM?**
- JavaScript representation of real DOM
- Enables efficient updates through diffing
- Allows React to batch updates

**Q: How does reconciliation work?**
1. Create new Virtual DOM tree
2. Compare with previous tree (diffing)
3. Calculate minimal changes
4. Update real DOM

**Q: Why are keys important in lists?**
- Help React identify which items changed
- Preserve component state during reorders
- Improve performance by reusing DOM nodes

**Q: What's the difference between Virtual DOM and Shadow DOM?**
- Virtual DOM: React concept for performance optimization
- Shadow DOM: Browser feature for encapsulation (Web Components)

**Q: What is React Fiber?**
- New reconciliation engine (React 16+)
- Enables incremental rendering
- Supports priority-based updates
- Allows pausing/resuming work

**Q: When does React re-render?**
- State changes (setState/useState)
- Props changes
- Parent re-renders (unless optimized)
- Context value changes
- forceUpdate() called

**Q: How to prevent unnecessary re-renders?**
- React.memo for functional components
- PureComponent for class components
- useMemo/useCallback for values/functions
- Proper key usage in lists
- Code splitting and lazy loading
