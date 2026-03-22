# Context API

## What is Context?

Context provides a way to pass data through the component tree without manually passing props at every level. It solves the "prop drilling" problem.

## When to Use Context

- Theme data (dark/light mode)
- User authentication
- Language/locale preferences
- Global app settings
- Shared state across many components

## Creating and Using Context

### Basic Example

```jsx
import { createContext, useContext, useState } from 'react';

// 1. Create Context
const ThemeContext = createContext('light');

// 2. Provider Component
function App() {
  const [theme, setTheme] = useState('light');
  
  return (
    <ThemeContext.Provider value={theme}>
      <Toolbar />
    </ThemeContext.Provider>
  );
}

// 3. Consumer Component
function ThemedButton() {
  const theme = useContext(ThemeContext);
  return <button className={theme}>Click me</button>;
}
```

### Context with State Management

```jsx
// Create context
const UserContext = createContext();

// Provider component
function UserProvider({ children }) {
  const [user, setUser] = useState(null);
  
  const login = (userData) => setUser(userData);
  const logout = () => setUser(null);
  
  const value = {
    user,
    login,
    logout,
    isAuthenticated: !!user
  };
  
  return (
    <UserContext.Provider value={value}>
      {children}
    </UserContext.Provider>
  );
}

// Custom hook for consuming context
function useUser() {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUser must be used within UserProvider');
  }
  return context;
}

// Usage
function App() {
  return (
    <UserProvider>
      <Dashboard />
    </UserProvider>
  );
}

function Dashboard() {
  const { user, logout } = useUser();
  
  return (
    <div>
      <h1>Welcome, {user?.name}</h1>
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

## Multiple Contexts

```jsx
const ThemeContext = createContext();
const UserContext = createContext();

function App() {
  return (
    <ThemeContext.Provider value="dark">
      <UserContext.Provider value={{ name: "John" }}>
        <Content />
      </UserContext.Provider>
    </ThemeContext.Provider>
  );
}

function Content() {
  const theme = useContext(ThemeContext);
  const user = useContext(UserContext);
  
  return <div className={theme}>Hello, {user.name}</div>;
}
```

## Context with useReducer

```jsx
const StateContext = createContext();
const DispatchContext = createContext();

const initialState = { count: 0, user: null };

function reducer(state, action) {
  switch (action.type) {
    case 'increment':
      return { ...state, count: state.count + 1 };
    case 'setUser':
      return { ...state, user: action.payload };
    default:
      return state;
  }
}

function AppProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  
  return (
    <StateContext.Provider value={state}>
      <DispatchContext.Provider value={dispatch}>
        {children}
      </DispatchContext.Provider>
    </StateContext.Provider>
  );
}

// Custom hooks
function useAppState() {
  return useContext(StateContext);
}

function useAppDispatch() {
  return useContext(DispatchContext);
}

// Usage
function Counter() {
  const { count } = useAppState();
  const dispatch = useAppDispatch();
  
  return (
    <button onClick={() => dispatch({ type: 'increment' })}>
      Count: {count}
    </button>
  );
}
```

## Default Values

```jsx
const ThemeContext = createContext({
  theme: 'light',
  toggleTheme: () => {}
});

// Used when no Provider is found
function Component() {
  const { theme } = useContext(ThemeContext);
  // theme will be 'light' if no Provider exists
}
```

## Context Best Practices

### 1. Split Contexts by Concern
```jsx
// Bad - one large context
const AppContext = createContext();

// Good - separate contexts
const ThemeContext = createContext();
const UserContext = createContext();
const SettingsContext = createContext();
```

### 2. Memoize Context Value
```jsx
function Provider({ children }) {
  const [state, setState] = useState(initialState);
  
  // Memoize to prevent unnecessary re-renders
  const value = useMemo(() => ({
    state,
    setState
  }), [state]);
  
  return (
    <Context.Provider value={value}>
      {children}
    </Context.Provider>
  );
}
```

### 3. Create Custom Hooks
```jsx
function useTheme() {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}
```

### 4. Separate State and Dispatch
```jsx
// Prevents re-renders when only dispatch is needed
const StateContext = createContext();
const DispatchContext = createContext();
```

## Performance Considerations

### Problem: All consumers re-render when context changes
```jsx
// Every component using this context re-renders
const AppContext = createContext();

function Provider({ children }) {
  const [user, setUser] = useState(null);
  const [theme, setTheme] = useState('light');
  
  return (
    <AppContext.Provider value={{ user, setUser, theme, setTheme }}>
      {children}
    </AppContext.Provider>
  );
}
```

### Solution 1: Split contexts
```jsx
const UserContext = createContext();
const ThemeContext = createContext();
```

### Solution 2: Memoize value
```jsx
const value = useMemo(() => ({ user, setUser }), [user]);
```

### Solution 3: Use React.memo
```jsx
const Component = React.memo(function Component() {
  const { theme } = useContext(ThemeContext);
  return <div className={theme}>Content</div>;
});
```

## Interview Questions

**Q: What is Context API?**
- Way to share data across component tree without prop drilling
- Provides global state management
- Alternative to Redux for simpler use cases

**Q: When should you use Context?**
- Data needed by many components at different nesting levels
- Theme, locale, authentication
- Avoid for frequently changing data (performance issues)

**Q: Context vs Redux?**
- Context: Built-in, simpler, good for low-frequency updates
- Redux: More features (middleware, devtools), better for complex state

**Q: How to optimize Context performance?**
- Split contexts by concern
- Memoize context value
- Separate state and dispatch contexts
- Use React.memo on consumers

**Q: What's the default value in createContext?**
- Used when component has no matching Provider above it
- Useful for testing components in isolation

**Q: Can you have multiple Providers for same Context?**
- Yes, inner Provider overrides outer Provider
- Useful for nested themes or scoped state

**Q: What happens if useContext is called without Provider?**
- Returns default value from createContext
- If no default, returns undefined
