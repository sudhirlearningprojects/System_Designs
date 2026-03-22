# Top 50 Interview Questions for Senior Full-Stack Engineers - Part 2

## React & Frontend (Questions 16-30)

### 16. How do you handle authentication and authorization in a React application?

**Answer:**

**1. JWT Token-based Authentication:**
```jsx
// Auth Context
const AuthContext = createContext();

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    // Check if user is logged in on mount
    const token = localStorage.getItem('token');
    if (token) {
      verifyToken(token).then(user => {
        setUser(user);
        setLoading(false);
      });
    } else {
      setLoading(false);
    }
  }, []);
  
  const login = async (email, password) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    
    const { token, user } = await response.json();
    localStorage.setItem('token', token);
    setUser(user);
  };
  
  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };
  
  const value = {
    user,
    login,
    logout,
    isAuthenticated: !!user
  };
  
  if (loading) return <LoadingScreen />;
  
  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

function useAuth() {
  return useContext(AuthContext);
}
```

**2. Protected Routes:**
```jsx
function ProtectedRoute({ children, requiredRole }) {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/unauthorized" replace />;
  }
  
  return children;
}

// Usage
<Routes>
  <Route path="/login" element={<Login />} />
  <Route path="/dashboard" element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  } />
  <Route path="/admin" element={
    <ProtectedRoute requiredRole="admin">
      <AdminPanel />
    </ProtectedRoute>
  } />
</Routes>
```

**3. API Interceptor with Axios:**
```jsx
import axios from 'axios';

const api = axios.create({
  baseURL: '/api'
});

// Request interceptor - add token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
```

**4. Refresh Token Pattern:**
```jsx
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }
      
      originalRequest._retry = true;
      isRefreshing = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        
        localStorage.setItem('token', data.token);
        api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`;
        processQueue(null, data.token);
        
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }
    
    return Promise.reject(error);
  }
);
```

**5. Role-Based Component Rendering:**
```jsx
function Can({ perform, children }) {
  const { user } = useAuth();
  
  const permissions = {
    admin: ['create', 'read', 'update', 'delete'],
    editor: ['create', 'read', 'update'],
    viewer: ['read']
  };
  
  const userPermissions = permissions[user?.role] || [];
  
  if (userPermissions.includes(perform)) {
    return children;
  }
  
  return null;
}

// Usage
<Can perform="delete">
  <button onClick={handleDelete}>Delete</button>
</Can>
```

**Best Practices:**
- Store tokens in httpOnly cookies (more secure than localStorage)
- Implement token refresh mechanism
- Use HTTPS only
- Set token expiration
- Implement CSRF protection
- Clear sensitive data on logout

---

### 17. Explain different ways to style React components. What are the pros and cons of each?

**Answer:**

**1. CSS Modules:**
```jsx
// Button.module.css
.button {
  background: blue;
  color: white;
}

.button:hover {
  background: darkblue;
}

// Button.jsx
import styles from './Button.module.css';

function Button() {
  return <button className={styles.button}>Click</button>;
}
```
**Pros:** Scoped styles, no naming conflicts, standard CSS
**Cons:** No dynamic styling, separate files

**2. Styled Components (CSS-in-JS):**
```jsx
import styled from 'styled-components';

const Button = styled.button`
  background: ${props => props.primary ? 'blue' : 'gray'};
  color: white;
  padding: 10px 20px;
  
  &:hover {
    background: darkblue;
  }
`;

// Usage
<Button primary>Click</Button>
```
**Pros:** Dynamic styling, scoped, theming, no class names
**Cons:** Runtime overhead, larger bundle, learning curve

**3. Emotion:**
```jsx
import { css } from '@emotion/react';

const buttonStyle = css`
  background: blue;
  color: white;
  &:hover {
    background: darkblue;
  }
`;

function Button() {
  return <button css={buttonStyle}>Click</button>;
}
```
**Pros:** Similar to styled-components, better performance
**Cons:** Runtime overhead

**4. Tailwind CSS:**
```jsx
function Button() {
  return (
    <button className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
      Click
    </button>
  );
}
```
**Pros:** Utility-first, fast development, small bundle, consistent design
**Cons:** Verbose HTML, learning curve, purge config needed

**5. Inline Styles:**
```jsx
function Button() {
  const style = {
    background: 'blue',
    color: 'white',
    padding: '10px 20px'
  };
  
  return <button style={style}>Click</button>;
}
```
**Pros:** Dynamic, component-scoped, no build step
**Cons:** No pseudo-classes, no media queries, performance

**6. Sass/SCSS:**
```scss
// Button.scss
.button {
  background: blue;
  color: white;
  
  &:hover {
    background: darken(blue, 10%);
  }
  
  &--primary {
    background: green;
  }
}
```
**Pros:** Variables, nesting, mixins, mature ecosystem
**Cons:** Not scoped by default, build step required

**Recommendation:**
- **Small projects:** CSS Modules or Tailwind
- **Large projects:** Styled Components or Emotion
- **Design systems:** Tailwind or Styled Components
- **Performance-critical:** CSS Modules or Tailwind (zero runtime)

---

### 18. How do you implement real-time features in React (WebSockets, Server-Sent Events)?

**Answer:**

**1. WebSocket Implementation:**
```jsx
function useWebSocket(url) {
  const [messages, setMessages] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const ws = useRef(null);
  
  useEffect(() => {
    ws.current = new WebSocket(url);
    
    ws.current.onopen = () => {
      console.log('Connected');
      setIsConnected(true);
    };
    
    ws.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      setMessages(prev => [...prev, message]);
    };
    
    ws.current.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
    
    ws.current.onclose = () => {
      console.log('Disconnected');
      setIsConnected(false);
    };
    
    return () => {
      ws.current?.close();
    };
  }, [url]);
  
  const sendMessage = useCallback((message) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(message));
    }
  }, []);
  
  return { messages, isConnected, sendMessage };
}

// Usage
function Chat() {
  const { messages, isConnected, sendMessage } = useWebSocket('ws://localhost:8080');
  const [input, setInput] = useState('');
  
  const handleSend = () => {
    sendMessage({ text: input, timestamp: Date.now() });
    setInput('');
  };
  
  return (
    <div>
      <div>Status: {isConnected ? 'Connected' : 'Disconnected'}</div>
      <div>
        {messages.map((msg, i) => (
          <div key={i}>{msg.text}</div>
        ))}
      </div>
      <input value={input} onChange={(e) => setInput(e.target.value)} />
      <button onClick={handleSend}>Send</button>
    </div>
  );
}
```

**2. Socket.IO (more features):**
```jsx
import { io } from 'socket.io-client';

function useSocketIO(url) {
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  
  useEffect(() => {
    const socketInstance = io(url, {
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: 5
    });
    
    socketInstance.on('connect', () => {
      setIsConnected(true);
    });
    
    socketInstance.on('disconnect', () => {
      setIsConnected(false);
    });
    
    setSocket(socketInstance);
    
    return () => {
      socketInstance.disconnect();
    };
  }, [url]);
  
  const emit = useCallback((event, data) => {
    socket?.emit(event, data);
  }, [socket]);
  
  const on = useCallback((event, callback) => {
    socket?.on(event, callback);
    return () => socket?.off(event, callback);
  }, [socket]);
  
  return { socket, isConnected, emit, on };
}

// Usage
function Chat() {
  const { isConnected, emit, on } = useSocketIO('http://localhost:3000');
  const [messages, setMessages] = useState([]);
  
  useEffect(() => {
    const unsubscribe = on('message', (message) => {
      setMessages(prev => [...prev, message]);
    });
    
    return unsubscribe;
  }, [on]);
  
  const sendMessage = (text) => {
    emit('message', { text, userId: 'user123' });
  };
  
  return (
    <div>
      <div>Status: {isConnected ? '🟢' : '🔴'}</div>
      {messages.map((msg, i) => (
        <div key={i}>{msg.text}</div>
      ))}
    </div>
  );
}
```

**3. Server-Sent Events (SSE):**
```jsx
function useSSE(url) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    const eventSource = new EventSource(url);
    
    eventSource.onmessage = (event) => {
      const newData = JSON.parse(event.data);
      setData(newData);
    };
    
    eventSource.onerror = (err) => {
      console.error('SSE error:', err);
      setError(err);
      eventSource.close();
    };
    
    return () => {
      eventSource.close();
    };
  }, [url]);
  
  return { data, error };
}

// Usage
function LiveFeed() {
  const { data, error } = useSSE('/api/live-updates');
  
  if (error) return <div>Error: {error.message}</div>;
  
  return <div>Latest: {data?.message}</div>;
}
```

**4. Reconnection Logic:**
```jsx
function useWebSocketWithReconnect(url, options = {}) {
  const {
    reconnectInterval = 3000,
    maxReconnectAttempts = 5
  } = options;
  
  const [isConnected, setIsConnected] = useState(false);
  const reconnectAttempts = useRef(0);
  const ws = useRef(null);
  
  const connect = useCallback(() => {
    ws.current = new WebSocket(url);
    
    ws.current.onopen = () => {
      setIsConnected(true);
      reconnectAttempts.current = 0;
    };
    
    ws.current.onclose = () => {
      setIsConnected(false);
      
      if (reconnectAttempts.current < maxReconnectAttempts) {
        setTimeout(() => {
          reconnectAttempts.current++;
          connect();
        }, reconnectInterval);
      }
    };
  }, [url, reconnectInterval, maxReconnectAttempts]);
  
  useEffect(() => {
    connect();
    return () => ws.current?.close();
  }, [connect]);
  
  return { isConnected, ws: ws.current };
}
```

**Best Practices:**
- Handle reconnection gracefully
- Show connection status to user
- Implement heartbeat/ping-pong
- Clean up connections on unmount
- Handle errors and edge cases
- Use message queuing for offline support

---

### 19. How do you handle forms with complex validation in React?

**Answer:**

**1. React Hook Form (Recommended):**
```jsx
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';

const schema = yup.object({
  email: yup.string().email().required('Email is required'),
  password: yup.string().min(8).required('Password is required'),
  confirmPassword: yup.string()
    .oneOf([yup.ref('password')], 'Passwords must match')
    .required('Confirm password is required'),
  age: yup.number().positive().integer().min(18).required(),
  terms: yup.boolean().oneOf([true], 'Must accept terms')
}).required();

function RegistrationForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    watch,
    reset
  } = useForm({
    resolver: yupResolver(schema),
    mode: 'onBlur' // Validate on blur
  });
  
  const onSubmit = async (data) => {
    try {
      await api.post('/register', data);
      reset();
    } catch (error) {
      console.error(error);
    }
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <input {...register('email')} placeholder="Email" />
        {errors.email && <span>{errors.email.message}</span>}
      </div>
      
      <div>
        <input {...register('password')} type="password" placeholder="Password" />
        {errors.password && <span>{errors.password.message}</span>}
      </div>
      
      <div>
        <input {...register('confirmPassword')} type="password" placeholder="Confirm" />
        {errors.confirmPassword && <span>{errors.confirmPassword.message}</span>}
      </div>
      
      <div>
        <input {...register('age')} type="number" placeholder="Age" />
        {errors.age && <span>{errors.age.message}</span>}
      </div>
      
      <div>
        <input {...register('terms')} type="checkbox" />
        <label>Accept terms</label>
        {errors.terms && <span>{errors.terms.message}</span>}
      </div>
      
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Submitting...' : 'Submit'}
      </button>
    </form>
  );
}
```

**2. Custom Validation Hook:**
```jsx
function useFormValidation(initialState, validate) {
  const [values, setValues] = useState(initialState);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setValues(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };
  
  const handleBlur = (e) => {
    const { name } = e.target;
    setTouched(prev => ({ ...prev, [name]: true }));
    
    const fieldErrors = validate({ [name]: values[name] });
    setErrors(prev => ({ ...prev, ...fieldErrors }));
  };
  
  const handleSubmit = async (callback) => {
    return async (e) => {
      e.preventDefault();
      
      const validationErrors = validate(values);
      setErrors(validationErrors);
      
      if (Object.keys(validationErrors).length === 0) {
        setIsSubmitting(true);
        try {
          await callback(values);
        } finally {
          setIsSubmitting(false);
        }
      }
    };
  };
  
  return {
    values,
    errors,
    touched,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit
  };
}

// Usage
function Form() {
  const validate = (values) => {
    const errors = {};
    
    if (!values.email) {
      errors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(values.email)) {
      errors.email = 'Email is invalid';
    }
    
    if (!values.password) {
      errors.password = 'Password is required';
    } else if (values.password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }
    
    return errors;
  };
  
  const {
    values,
    errors,
    touched,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit
  } = useFormValidation({ email: '', password: '' }, validate);
  
  const onSubmit = async (data) => {
    await api.post('/login', data);
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input
        name="email"
        value={values.email}
        onChange={handleChange}
        onBlur={handleBlur}
      />
      {touched.email && errors.email && <span>{errors.email}</span>}
      
      <input
        name="password"
        type="password"
        value={values.password}
        onChange={handleChange}
        onBlur={handleBlur}
      />
      {touched.password && errors.password && <span>{errors.password}</span>}
      
      <button type="submit" disabled={isSubmitting}>Submit</button>
    </form>
  );
}
```

**3. Async Validation (username availability):**
```jsx
function useAsyncValidation(value, validator, delay = 500) {
  const [isValidating, setIsValidating] = useState(false);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    if (!value) return;
    
    setIsValidating(true);
    const timer = setTimeout(async () => {
      try {
        await validator(value);
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setIsValidating(false);
      }
    }, delay);
    
    return () => clearTimeout(timer);
  }, [value, validator, delay]);
  
  return { isValidating, error };
}

// Usage
function UsernameField() {
  const [username, setUsername] = useState('');
  
  const checkUsername = async (value) => {
    const response = await api.get(`/check-username/${value}`);
    if (!response.data.available) {
      throw new Error('Username already taken');
    }
  };
  
  const { isValidating, error } = useAsyncValidation(username, checkUsername);
  
  return (
    <div>
      <input
        value={username}
        onChange={(e) => setUsername(e.target.value)}
      />
      {isValidating && <span>Checking...</span>}
      {error && <span>{error}</span>}
    </div>
  );
}
```

**4. Multi-step Form:**
```jsx
function useMultiStepForm(steps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState({});
  
  const next = (data) => {
    setFormData(prev => ({ ...prev, ...data }));
    setCurrentStep(prev => Math.min(prev + 1, steps.length - 1));
  };
  
  const back = () => {
    setCurrentStep(prev => Math.max(prev - 1, 0));
  };
  
  const goTo = (step) => {
    setCurrentStep(step);
  };
  
  return {
    currentStep,
    formData,
    next,
    back,
    goTo,
    isFirstStep: currentStep === 0,
    isLastStep: currentStep === steps.length - 1,
    progress: ((currentStep + 1) / steps.length) * 100
  };
}

// Usage
function MultiStepRegistration() {
  const steps = ['Personal', 'Account', 'Preferences'];
  const { currentStep, formData, next, back, isFirstStep, isLastStep, progress } = 
    useMultiStepForm(steps);
  
  const handleSubmit = async (data) => {
    if (isLastStep) {
      await api.post('/register', { ...formData, ...data });
    } else {
      next(data);
    }
  };
  
  return (
    <div>
      <div>Progress: {progress}%</div>
      
      {currentStep === 0 && <PersonalInfoForm onSubmit={handleSubmit} />}
      {currentStep === 1 && <AccountForm onSubmit={handleSubmit} />}
      {currentStep === 2 && <PreferencesForm onSubmit={handleSubmit} />}
      
      {!isFirstStep && <button onClick={back}>Back</button>}
    </div>
  );
}
```

**Best Practices:**
- Use React Hook Form for complex forms
- Validate on blur for better UX
- Show errors only after field is touched
- Debounce async validation
- Disable submit during validation
- Provide clear error messages
- Use schema validation (Yup, Zod)

---

### 20. Explain React's rendering behavior and how to debug performance issues.

**Answer:**

**React Rendering Triggers:**
1. State changes (useState, useReducer)
2. Props changes
3. Parent component re-renders
4. Context value changes
5. forceUpdate() called

**Debugging Tools:**

**1. React DevTools Profiler:**
```jsx
import { Profiler } from 'react';

function onRenderCallback(
  id,
  phase,
  actualDuration,
  baseDuration,
  startTime,
  commitTime
) {
  console.log(`${id} (${phase}) took ${actualDuration}ms`);
}

function App() {
  return (
    <Profiler id="App" onRender={onRenderCallback}>
      <Component />
    </Profiler>
  );
}
```

**2. Why Did You Render:**
```jsx
import whyDidYouRender from '@welldone-software/why-did-you-render';

if (process.env.NODE_ENV === 'development') {
  whyDidYouRender(React, {
    trackAllPureComponents: true,
    trackHooks: true,
    logOnDifferentValues: true
  });
}

// Mark component for tracking
MyComponent.whyDidYouRender = true;
```

**3. Custom Performance Hook:**
```jsx
function useRenderCount(componentName) {
  const renderCount = useRef(0);
  
  useEffect(() => {
    renderCount.current++;
    console.log(`${componentName} rendered ${renderCount.current} times`);
  });
}

// Usage
function MyComponent() {
  useRenderCount('MyComponent');
  return <div>Content</div>;
}
```

**4. Trace Updates:**
```jsx
function useTraceUpdate(props) {
  const prev = useRef(props);
  
  useEffect(() => {
    const changedProps = Object.entries(props).reduce((acc, [key, value]) => {
      if (prev.current[key] !== value) {
        acc[key] = {
          from: prev.current[key],
          to: value
        };
      }
      return acc;
    }, {});
    
    if (Object.keys(changedProps).length > 0) {
      console.log('Changed props:', changedProps);
    }
    
    prev.current = props;
  });
}

// Usage
function MyComponent(props) {
  useTraceUpdate(props);
  return <div>{props.data}</div>;
}
```

**Common Performance Issues:**

**1. Unnecessary Re-renders:**
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

**2. Inline Functions/Objects:**
```jsx
// Problem: New reference on every render
function Parent() {
  return (
    <Child 
      onClick={() => console.log('click')}
      style={{ color: 'red' }}
    />
  );
}

// Solution: Memoize
function Parent() {
  const handleClick = useCallback(() => {
    console.log('click');
  }, []);
  
  const style = useMemo(() => ({ color: 'red' }), []);
  
  return <Child onClick={handleClick} style={style} />;
}
```

**3. Large Lists:**
```jsx
// Problem: Rendering 10,000 items
function List({ items }) {
  return (
    <div>
      {items.map(item => <Item key={item.id} data={item} />)}
    </div>
  );
}

// Solution: Virtualization
import { FixedSizeList } from 'react-window';

function VirtualList({ items }) {
  return (
    <FixedSizeList
      height={600}
      itemCount={items.length}
      itemSize={50}
      width="100%"
    >
      {({ index, style }) => (
        <div style={style}>{items[index].name}</div>
      )}
    </FixedSizeList>
  );
}
```

**4. Context Performance:**
```jsx
// Problem: All consumers re-render
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

// Solution: Split contexts
const UserContext = createContext();
const ThemeContext = createContext();

function Provider({ children }) {
  const [user, setUser] = useState(null);
  const [theme, setTheme] = useState('light');
  
  const userValue = useMemo(() => ({ user, setUser }), [user]);
  const themeValue = useMemo(() => ({ theme, setTheme }), [theme]);
  
  return (
    <UserContext.Provider value={userValue}>
      <ThemeContext.Provider value={themeValue}>
        {children}
      </ThemeContext.Provider>
    </UserContext.Provider>
  );
}
```

**Performance Checklist:**
- ✅ Use React.memo for expensive components
- ✅ Memoize callbacks with useCallback
- ✅ Memoize computed values with useMemo
- ✅ Use proper keys in lists
- ✅ Virtualize long lists
- ✅ Code split with React.lazy
- ✅ Split contexts by concern
- ✅ Avoid inline functions/objects in props
- ✅ Use production build
- ✅ Profile with React DevTools

---

**Continue to Part 3 for more React questions (21-35)...**
