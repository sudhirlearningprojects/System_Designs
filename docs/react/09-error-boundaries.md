# Error Boundaries

## What are Error Boundaries?

Error boundaries are React components that catch JavaScript errors anywhere in their child component tree, log those errors, and display a fallback UI instead of crashing the entire app.

## Creating an Error Boundary

Error boundaries must be class components (no hook equivalent yet).

```jsx
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }
  
  static getDerivedStateFromError(error) {
    // Update state so next render shows fallback UI
    return { hasError: true };
  }
  
  componentDidCatch(error, errorInfo) {
    // Log error to error reporting service
    console.error('Error caught:', error, errorInfo);
    this.setState({ error, errorInfo });
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <div>
          <h1>Something went wrong.</h1>
          <details>
            {this.state.error && this.state.error.toString()}
            <br />
            {this.state.errorInfo.componentStack}
          </details>
        </div>
      );
    }
    
    return this.props.children;
  }
}
```

## Usage

```jsx
function App() {
  return (
    <ErrorBoundary>
      <MyComponent />
    </ErrorBoundary>
  );
}
```

## What Error Boundaries Catch

✅ Errors in:
- Render methods
- Lifecycle methods
- Constructors of child components

❌ Do NOT catch:
- Event handlers
- Asynchronous code (setTimeout, promises)
- Server-side rendering
- Errors in error boundary itself

## Handling Event Handler Errors

```jsx
function MyComponent() {
  const [error, setError] = useState(null);
  
  const handleClick = () => {
    try {
      // Code that might throw
      throw new Error('Button error');
    } catch (error) {
      setError(error);
    }
  };
  
  if (error) {
    return <div>Error: {error.message}</div>;
  }
  
  return <button onClick={handleClick}>Click me</button>;
}
```

## Multiple Error Boundaries

```jsx
function App() {
  return (
    <ErrorBoundary fallback={<div>App Error</div>}>
      <Header />
      
      <ErrorBoundary fallback={<div>Sidebar Error</div>}>
        <Sidebar />
      </ErrorBoundary>
      
      <ErrorBoundary fallback={<div>Content Error</div>}>
        <Content />
      </ErrorBoundary>
    </ErrorBoundary>
  );
}
```

## Error Boundary with Reset

```jsx
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }
  
  static getDerivedStateFromError(error) {
    return { hasError: true };
  }
  
  componentDidCatch(error, errorInfo) {
    console.error(error, errorInfo);
  }
  
  resetError = () => {
    this.setState({ hasError: false });
  };
  
  render() {
    if (this.state.hasError) {
      return (
        <div>
          <h1>Something went wrong.</h1>
          <button onClick={this.resetError}>Try again</button>
        </div>
      );
    }
    
    return this.props.children;
  }
}
```

## React Error Boundary Library

```jsx
import { ErrorBoundary } from 'react-error-boundary';

function ErrorFallback({ error, resetErrorBoundary }) {
  return (
    <div>
      <h1>Something went wrong:</h1>
      <pre>{error.message}</pre>
      <button onClick={resetErrorBoundary}>Try again</button>
    </div>
  );
}

function App() {
  return (
    <ErrorBoundary
      FallbackComponent={ErrorFallback}
      onReset={() => {
        // Reset app state
      }}
      onError={(error, errorInfo) => {
        // Log to error reporting service
      }}
    >
      <MyComponent />
    </ErrorBoundary>
  );
}
```

## Best Practices

### 1. Granular Error Boundaries
```jsx
// Wrap individual features
<ErrorBoundary>
  <UserProfile />
</ErrorBoundary>

<ErrorBoundary>
  <Comments />
</ErrorBoundary>
```

### 2. Error Logging
```jsx
componentDidCatch(error, errorInfo) {
  // Send to error tracking service
  logErrorToService(error, errorInfo);
}
```

### 3. User-Friendly Messages
```jsx
render() {
  if (this.state.hasError) {
    return (
      <div>
        <h2>Oops! Something went wrong.</h2>
        <p>We're working on fixing this. Please try again later.</p>
        <button onClick={this.resetError}>Reload</button>
      </div>
    );
  }
  return this.props.children;
}
```

## Interview Questions

**Q: What are Error Boundaries?**
- React components that catch errors in child component tree
- Display fallback UI instead of crashing
- Log errors for debugging

**Q: What errors do Error Boundaries NOT catch?**
- Event handlers
- Asynchronous code
- Server-side rendering
- Errors in error boundary itself

**Q: Why must Error Boundaries be class components?**
- getDerivedStateFromError and componentDidCatch are class-only lifecycle methods
- No hook equivalent yet (as of React 18)

**Q: How to handle errors in event handlers?**
- Use try-catch blocks
- Store error in state
- Display error UI conditionally

**Q: Where should you place Error Boundaries?**
- Top level (catch all errors)
- Around individual features (granular error handling)
- Multiple boundaries for better UX

**Q: What is getDerivedStateFromError?**
- Static lifecycle method
- Called during render phase
- Returns new state to show fallback UI

**Q: What is componentDidCatch?**
- Lifecycle method for side effects
- Called during commit phase
- Used for error logging

**Q: Can you have multiple Error Boundaries?**
- Yes, recommended for granular error handling
- Inner boundary catches first
- Prevents entire app from crashing
