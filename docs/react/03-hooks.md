# React Hooks

## Rules of Hooks

1. Only call hooks at the top level (not in loops, conditions, or nested functions)
2. Only call hooks from React functions (components or custom hooks)

## Built-in Hooks

### 1. useState
```jsx
const [state, setState] = useState(initialValue);

// Lazy initialization (expensive computation)
const [state, setState] = useState(() => {
  return expensiveComputation();
});
```

### 2. useEffect
```jsx
useEffect(() => {
  // Side effect code
  return () => {
    // Cleanup
  };
}, [dependencies]);
```

### 3. useContext
```jsx
const ThemeContext = React.createContext('light');

function Component() {
  const theme = useContext(ThemeContext);
  return <div className={theme}>Content</div>;
}
```

### 4. useReducer
```jsx
const initialState = { count: 0 };

function reducer(state, action) {
  switch (action.type) {
    case 'increment':
      return { count: state.count + 1 };
    case 'decrement':
      return { count: state.count - 1 };
    default:
      return state;
  }
}

function Counter() {
  const [state, dispatch] = useReducer(reducer, initialState);
  
  return (
    <>
      Count: {state.count}
      <button onClick={() => dispatch({ type: 'increment' })}>+</button>
    </>
  );
}
```

### 5. useCallback
Memoizes callback functions to prevent unnecessary re-renders.

```jsx
const memoizedCallback = useCallback(() => {
  doSomething(a, b);
}, [a, b]);

// Example
function Parent() {
  const [count, setCount] = useState(0);
  
  const handleClick = useCallback(() => {
    console.log('Clicked');
  }, []); // Function reference stays same
  
  return <Child onClick={handleClick} />;
}
```

### 6. useMemo
Memoizes computed values to avoid expensive recalculations.

```jsx
const memoizedValue = useMemo(() => {
  return expensiveComputation(a, b);
}, [a, b]);

// Example
function Component({ items }) {
  const sortedItems = useMemo(() => {
    return items.sort((a, b) => a - b);
  }, [items]);
  
  return <List items={sortedItems} />;
}
```

### 7. useRef
Creates a mutable reference that persists across renders.

```jsx
function Component() {
  const inputRef = useRef(null);
  const countRef = useRef(0);
  
  const focusInput = () => {
    inputRef.current.focus();
  };
  
  // Doesn't cause re-render
  countRef.current += 1;
  
  return <input ref={inputRef} />;
}
```

### 8. useLayoutEffect
Similar to useEffect but fires synchronously after DOM mutations.

```jsx
useLayoutEffect(() => {
  // Runs before browser paint
  // Use for DOM measurements
}, []);
```

### 9. useImperativeHandle
Customizes the instance value exposed to parent when using ref.

```jsx
const FancyInput = forwardRef((props, ref) => {
  const inputRef = useRef();
  
  useImperativeHandle(ref, () => ({
    focus: () => {
      inputRef.current.focus();
    }
  }));
  
  return <input ref={inputRef} />;
});
```

## Custom Hooks

```jsx
// useFetch custom hook
function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetch(url)
      .then(res => res.json())
      .then(data => {
        setData(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err);
        setLoading(false);
      });
  }, [url]);
  
  return { data, loading, error };
}

// Usage
function Component() {
  const { data, loading, error } = useFetch('/api/users');
  
  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  return <div>{JSON.stringify(data)}</div>;
}
```

## Interview Questions

**Q: useCallback vs useMemo?**
- useCallback: Memoizes functions
- useMemo: Memoizes values

**Q: When to use useReducer vs useState?**
- useState: Simple state, few updates
- useReducer: Complex state logic, multiple sub-values, next state depends on previous

**Q: useEffect vs useLayoutEffect?**
- useEffect: Asynchronous, after paint (most cases)
- useLayoutEffect: Synchronous, before paint (DOM measurements, prevent flicker)

**Q: Why use useRef instead of useState for some values?**
- useRef doesn't trigger re-renders when updated
- Useful for: DOM references, storing mutable values, previous values

**Q: What's the dependency array in useEffect?**
- Empty []: Run once on mount
- [dep]: Run when dep changes
- No array: Run after every render

**Q: How to create a custom hook?**
- Function name starts with "use"
- Can use other hooks inside
- Returns values/functions for component use
