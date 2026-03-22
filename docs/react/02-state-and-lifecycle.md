# State and Lifecycle

## State

State is a built-in object that stores component data that can change over time. When state changes, the component re-renders.

### useState Hook
```jsx
import { useState } from 'react';

function Counter() {
  const [count, setCount] = useState(0);
  
  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>Increment</button>
    </div>
  );
}
```

### Multiple State Variables
```jsx
function Form() {
  const [name, setName] = useState('');
  const [age, setAge] = useState(0);
  const [email, setEmail] = useState('');
}
```

### State with Objects
```jsx
function User() {
  const [user, setUser] = useState({ name: '', age: 0 });
  
  // Update specific property (must spread existing state)
  const updateName = (newName) => {
    setUser({ ...user, name: newName });
  };
}
```

### State with Arrays
```jsx
function TodoList() {
  const [todos, setTodos] = useState([]);
  
  const addTodo = (todo) => {
    setTodos([...todos, todo]); // Add
  };
  
  const removeTodo = (index) => {
    setTodos(todos.filter((_, i) => i !== index)); // Remove
  };
}
```

## Lifecycle Methods

### Functional Components (Hooks)

#### useEffect - Component Lifecycle
```jsx
import { useEffect } from 'react';

function Component() {
  // ComponentDidMount + ComponentDidUpdate
  useEffect(() => {
    console.log('Component mounted or updated');
  });
  
  // ComponentDidMount only (empty dependency array)
  useEffect(() => {
    console.log('Component mounted');
  }, []);
  
  // ComponentDidUpdate for specific dependencies
  useEffect(() => {
    console.log('Count changed');
  }, [count]);
  
  // ComponentWillUnmount (cleanup)
  useEffect(() => {
    const timer = setInterval(() => {}, 1000);
    
    return () => {
      clearInterval(timer); // Cleanup
    };
  }, []);
}
```

### Class Components (Legacy)

```jsx
class Component extends React.Component {
  componentDidMount() {
    // After first render
  }
  
  componentDidUpdate(prevProps, prevState) {
    // After updates
  }
  
  componentWillUnmount() {
    // Before component removal
  }
  
  shouldComponentUpdate(nextProps, nextState) {
    // Return false to prevent re-render
    return true;
  }
}
```

## Interview Questions

**Q: What's the difference between state and props?**
- State: Internal, mutable, managed by component
- Props: External, immutable, passed from parent

**Q: Why is state immutable?**
- React compares references to detect changes
- Immutability ensures predictable updates and enables time-travel debugging

**Q: What happens when setState is called?**
1. State update is scheduled (asynchronous)
2. React batches multiple setState calls
3. Component re-renders with new state
4. Virtual DOM diffing occurs
5. Real DOM updates

**Q: How to update state based on previous state?**
```jsx
// Wrong
setCount(count + 1);

// Correct (functional update)
setCount(prevCount => prevCount + 1);
```

**Q: When does useEffect run?**
- After every render (no deps)
- After mount only ([])
- After deps change ([dep1, dep2])

**Q: What's the cleanup function in useEffect?**
- Runs before component unmounts
- Runs before effect re-executes
- Used for: clearing timers, canceling subscriptions, removing listeners
