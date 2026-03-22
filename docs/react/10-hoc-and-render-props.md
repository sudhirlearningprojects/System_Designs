# Higher-Order Components (HOC) and Render Props

## Higher-Order Components (HOC)

A HOC is a function that takes a component and returns a new component with additional props or behavior.

### Basic HOC Pattern

```jsx
function withLoading(Component) {
  return function WithLoadingComponent({ isLoading, ...props }) {
    if (isLoading) {
      return <div>Loading...</div>;
    }
    return <Component {...props} />;
  };
}

// Usage
const UserListWithLoading = withLoading(UserList);

function App() {
  const [isLoading, setIsLoading] = useState(true);
  return <UserListWithLoading isLoading={isLoading} users={[]} />;
}
```

### HOC with Data Fetching

```jsx
function withData(Component, url) {
  return function WithDataComponent(props) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    
    useEffect(() => {
      fetch(url)
        .then(res => res.json())
        .then(data => {
          setData(data);
          setLoading(false);
        });
    }, []);
    
    return <Component data={data} loading={loading} {...props} />;
  };
}

// Usage
const UserList = ({ data, loading }) => {
  if (loading) return <div>Loading...</div>;
  return <ul>{data.map(user => <li key={user.id}>{user.name}</li>)}</ul>;
};

const UserListWithData = withData(UserList, '/api/users');
```

### HOC with Authentication

```jsx
function withAuth(Component) {
  return function WithAuthComponent(props) {
    const { isAuthenticated } = useAuth();
    
    if (!isAuthenticated) {
      return <Navigate to="/login" />;
    }
    
    return <Component {...props} />;
  };
}

// Usage
const Dashboard = () => <div>Dashboard</div>;
const ProtectedDashboard = withAuth(Dashboard);
```

### Composing Multiple HOCs

```jsx
const enhance = compose(
  withAuth,
  withLoading,
  withData('/api/users')
);

const EnhancedComponent = enhance(UserList);
```

## Render Props

A component with a render prop takes a function that returns a React element and calls it instead of implementing its own render logic.

### Basic Render Props

```jsx
function Mouse({ render }) {
  const [position, setPosition] = useState({ x: 0, y: 0 });
  
  const handleMouseMove = (e) => {
    setPosition({ x: e.clientX, y: e.clientY });
  };
  
  useEffect(() => {
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);
  
  return render(position);
}

// Usage
function App() {
  return (
    <Mouse render={({ x, y }) => (
      <div>Mouse position: {x}, {y}</div>
    )} />
  );
}
```

### Using Children as Function

```jsx
function Mouse({ children }) {
  const [position, setPosition] = useState({ x: 0, y: 0 });
  
  const handleMouseMove = (e) => {
    setPosition({ x: e.clientX, y: e.clientY });
  };
  
  useEffect(() => {
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);
  
  return children(position);
}

// Usage
function App() {
  return (
    <Mouse>
      {({ x, y }) => (
        <div>Mouse position: {x}, {y}</div>
      )}
    </Mouse>
  );
}
```

### Data Fetching with Render Props

```jsx
function DataFetcher({ url, children }) {
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
  
  return children({ data, loading, error });
}

// Usage
function App() {
  return (
    <DataFetcher url="/api/users">
      {({ data, loading, error }) => {
        if (loading) return <div>Loading...</div>;
        if (error) return <div>Error: {error.message}</div>;
        return <ul>{data.map(user => <li key={user.id}>{user.name}</li>)}</ul>;
      }}
    </DataFetcher>
  );
}
```

## HOC vs Render Props vs Hooks

### Same Logic - Different Patterns

#### HOC
```jsx
const withCounter = (Component) => {
  return function WithCounter(props) {
    const [count, setCount] = useState(0);
    return <Component count={count} setCount={setCount} {...props} />;
  };
};

const Counter = withCounter(({ count, setCount }) => (
  <button onClick={() => setCount(count + 1)}>{count}</button>
));
```

#### Render Props
```jsx
function Counter({ children }) {
  const [count, setCount] = useState(0);
  return children({ count, setCount });
}

// Usage
<Counter>
  {({ count, setCount }) => (
    <button onClick={() => setCount(count + 1)}>{count}</button>
  )}
</Counter>
```

#### Custom Hook (Modern Approach)
```jsx
function useCounter() {
  const [count, setCount] = useState(0);
  return { count, setCount };
}

// Usage
function Counter() {
  const { count, setCount } = useCounter();
  return <button onClick={() => setCount(count + 1)}>{count}</button>;
}
```

## HOC Best Practices

### 1. Don't Mutate Original Component
```jsx
// Bad
function withData(Component) {
  Component.prototype.componentDidMount = function() {
    // Mutation
  };
  return Component;
}

// Good
function withData(Component) {
  return function WithData(props) {
    // Return new component
    return <Component {...props} />;
  };
}
```

### 2. Pass Unrelated Props
```jsx
function withData(Component) {
  return function WithData({ specialProp, ...props }) {
    // Use specialProp
    return <Component {...props} />; // Pass rest
  };
}
```

### 3. Maximize Composability
```jsx
// Good - returns function
const withData = (url) => (Component) => {
  return function WithData(props) {
    // Implementation
  };
};

// Usage
const enhance = compose(
  withAuth,
  withData('/api/users')
);
```

### 4. Display Name for Debugging
```jsx
function withData(Component) {
  function WithData(props) {
    return <Component {...props} />;
  }
  
  WithData.displayName = `WithData(${Component.displayName || Component.name})`;
  
  return WithData;
}
```

## Interview Questions

**Q: What is a Higher-Order Component?**
- Function that takes a component and returns a new component
- Used for code reuse, logic abstraction
- Similar to higher-order functions

**Q: What are Render Props?**
- Pattern where component takes a function prop that returns React element
- Function receives data/behavior from component
- Alternative to HOCs for sharing code

**Q: HOC vs Render Props?**
- HOC: Wraps component, adds props
- Render Props: Component calls function with data
- Both solve same problem (code reuse)

**Q: When to use HOC?**
- Cross-cutting concerns (auth, logging)
- Enhancing multiple components
- Legacy codebases

**Q: When to use Render Props?**
- Need more control over rendering
- Dynamic composition
- Avoid HOC nesting issues

**Q: Why are Hooks preferred over HOC/Render Props?**
- Simpler syntax
- No wrapper hell
- Better TypeScript support
- Easier to understand

**Q: What is wrapper hell?**
- Multiple HOCs/Render Props nested deeply
- Hard to debug and understand
- Hooks solve this problem

**Q: Can HOCs modify props?**
- Yes, can add, remove, or modify props
- Should pass unrelated props through
- Don't mutate original component

**Q: What's the displayName in HOC?**
- Property for debugging in React DevTools
- Shows component hierarchy clearly
- Format: WithHOC(ComponentName)
