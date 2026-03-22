# Advanced React Patterns

## Compound Components

Components that work together to form a complete UI, sharing implicit state.

```jsx
// Tabs component using compound pattern
const TabsContext = createContext();

function Tabs({ children, defaultValue }) {
  const [activeTab, setActiveTab] = useState(defaultValue);
  
  return (
    <TabsContext.Provider value={{ activeTab, setActiveTab }}>
      <div className="tabs">{children}</div>
    </TabsContext.Provider>
  );
}

function TabList({ children }) {
  return <div className="tab-list">{children}</div>;
}

function Tab({ value, children }) {
  const { activeTab, setActiveTab } = useContext(TabsContext);
  const isActive = activeTab === value;
  
  return (
    <button
      className={isActive ? 'active' : ''}
      onClick={() => setActiveTab(value)}
    >
      {children}
    </button>
  );
}

function TabPanel({ value, children }) {
  const { activeTab } = useContext(TabsContext);
  return activeTab === value ? <div>{children}</div> : null;
}

// Attach sub-components
Tabs.List = TabList;
Tabs.Tab = Tab;
Tabs.Panel = TabPanel;

// Usage
function App() {
  return (
    <Tabs defaultValue="tab1">
      <Tabs.List>
        <Tabs.Tab value="tab1">Tab 1</Tabs.Tab>
        <Tabs.Tab value="tab2">Tab 2</Tabs.Tab>
      </Tabs.List>
      <Tabs.Panel value="tab1">Content 1</Tabs.Panel>
      <Tabs.Panel value="tab2">Content 2</Tabs.Panel>
    </Tabs>
  );
}
```

## Control Props Pattern

Let users control component state externally.

```jsx
function Counter({ value, onChange, defaultValue = 0 }) {
  const [internalValue, setInternalValue] = useState(defaultValue);
  
  // Controlled if value is provided
  const isControlled = value !== undefined;
  const count = isControlled ? value : internalValue;
  
  const handleIncrement = () => {
    const newValue = count + 1;
    if (!isControlled) {
      setInternalValue(newValue);
    }
    onChange?.(newValue);
  };
  
  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={handleIncrement}>Increment</button>
    </div>
  );
}

// Uncontrolled usage
<Counter defaultValue={0} onChange={(val) => console.log(val)} />

// Controlled usage
function App() {
  const [count, setCount] = useState(0);
  return <Counter value={count} onChange={setCount} />;
}
```

## State Reducer Pattern

Give users control over state updates.

```jsx
function useCounter({ initial = 0, reducer = (state, action) => state }) {
  const [count, setCount] = useState(initial);
  
  const dispatch = (action) => {
    const newState = reducer(count, action);
    setCount(newState);
  };
  
  return { count, dispatch };
}

// Usage with custom reducer
function App() {
  const customReducer = (state, action) => {
    switch (action.type) {
      case 'increment':
        return Math.min(state + 1, 10); // Max 10
      case 'decrement':
        return Math.max(state - 1, 0); // Min 0
      default:
        return state;
    }
  };
  
  const { count, dispatch } = useCounter({ initial: 0, reducer: customReducer });
  
  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={() => dispatch({ type: 'increment' })}>+</button>
      <button onClick={() => dispatch({ type: 'decrement' })}>-</button>
    </div>
  );
}
```

## Props Getters Pattern

Provide props bundles for common use cases.

```jsx
function useDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  
  const getToggleProps = ({ onClick, ...props } = {}) => ({
    onClick: (e) => {
      setIsOpen(!isOpen);
      onClick?.(e);
    },
    'aria-expanded': isOpen,
    ...props
  });
  
  const getMenuProps = (props = {}) => ({
    hidden: !isOpen,
    ...props
  });
  
  return { isOpen, getToggleProps, getMenuProps };
}

// Usage
function Dropdown() {
  const { isOpen, getToggleProps, getMenuProps } = useDropdown();
  
  return (
    <div>
      <button {...getToggleProps()}>
        Toggle {isOpen ? '▲' : '▼'}
      </button>
      <ul {...getMenuProps()}>
        <li>Item 1</li>
        <li>Item 2</li>
      </ul>
    </div>
  );
}
```

## Provider Pattern

Share data across component tree without prop drilling.

```jsx
const UserContext = createContext();

function UserProvider({ children }) {
  const [user, setUser] = useState(null);
  
  const login = (userData) => setUser(userData);
  const logout = () => setUser(null);
  
  const value = useMemo(() => ({
    user,
    login,
    logout,
    isAuthenticated: !!user
  }), [user]);
  
  return (
    <UserContext.Provider value={value}>
      {children}
    </UserContext.Provider>
  );
}

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
  return <button onClick={logout}>Logout {user.name}</button>;
}
```

## Container/Presentational Pattern

Separate logic from UI.

```jsx
// Container (logic)
function UserListContainer() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    fetch('/api/users')
      .then(res => res.json())
      .then(data => {
        setUsers(data);
        setLoading(false);
      });
  }, []);
  
  return <UserListPresentation users={users} loading={loading} />;
}

// Presentational (UI)
function UserListPresentation({ users, loading }) {
  if (loading) return <div>Loading...</div>;
  
  return (
    <ul>
      {users.map(user => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```

## Hooks Pattern (Modern Approach)

Replace container pattern with custom hooks.

```jsx
// Custom hook (logic)
function useUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetch('/api/users')
      .then(res => res.json())
      .then(data => {
        setUsers(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err);
        setLoading(false);
      });
  }, []);
  
  return { users, loading, error };
}

// Component (UI)
function UserList() {
  const { users, loading, error } = useUsers();
  
  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  
  return (
    <ul>
      {users.map(user => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```

## Composition Pattern

Build complex UIs from simple components.

```jsx
function Card({ children }) {
  return <div className="card">{children}</div>;
}

function CardHeader({ children }) {
  return <div className="card-header">{children}</div>;
}

function CardBody({ children }) {
  return <div className="card-body">{children}</div>;
}

function CardFooter({ children }) {
  return <div className="card-footer">{children}</div>;
}

// Usage
function UserCard() {
  return (
    <Card>
      <CardHeader>
        <h2>John Doe</h2>
      </CardHeader>
      <CardBody>
        <p>Software Engineer</p>
      </CardBody>
      <CardFooter>
        <button>Contact</button>
      </CardFooter>
    </Card>
  );
}
```

## Interview Questions

**Q: What are Compound Components?**
- Components that work together sharing implicit state
- Flexible and composable API
- Example: Tabs, Accordion, Dropdown

**Q: What is the Control Props pattern?**
- Allow external control of component state
- Support both controlled and uncontrolled modes
- Similar to form inputs

**Q: What is the State Reducer pattern?**
- Give users control over state updates
- Users provide custom reducer function
- Enables complex state logic customization

**Q: What are Props Getters?**
- Functions that return prop objects
- Bundle related props together
- Simplify component API

**Q: Container vs Presentational components?**
- Container: Logic, data fetching, state
- Presentational: UI, styling, props
- Modern approach: Custom hooks instead

**Q: When to use Compound Components?**
- Related components that share state
- Flexible composition needed
- Clear parent-child relationship

**Q: What is the Provider pattern?**
- Share data across component tree
- Avoid prop drilling
- Use Context API

**Q: Composition vs Inheritance?**
- React favors composition over inheritance
- Build complex UIs from simple components
- More flexible and reusable
