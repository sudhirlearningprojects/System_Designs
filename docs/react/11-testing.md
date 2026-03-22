# React Testing

## Testing Libraries

### React Testing Library
```bash
npm install --save-dev @testing-library/react @testing-library/jest-dom
```

### Jest (Usually pre-configured with Create React App)
```bash
npm install --save-dev jest
```

## Basic Component Testing

### Simple Component Test
```jsx
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import Button from './Button';

test('renders button with text', () => {
  render(<Button>Click me</Button>);
  const button = screen.getByText('Click me');
  expect(button).toBeInTheDocument();
});
```

### Testing Props
```jsx
test('renders with correct props', () => {
  render(<Button variant="primary">Submit</Button>);
  const button = screen.getByRole('button');
  expect(button).toHaveClass('primary');
  expect(button).toHaveTextContent('Submit');
});
```

## User Interactions

### Click Events
```jsx
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

test('handles click event', () => {
  const handleClick = jest.fn();
  render(<Button onClick={handleClick}>Click</Button>);
  
  const button = screen.getByRole('button');
  fireEvent.click(button);
  
  expect(handleClick).toHaveBeenCalledTimes(1);
});

// Better - using userEvent
test('handles click with userEvent', async () => {
  const user = userEvent.setup();
  const handleClick = jest.fn();
  render(<Button onClick={handleClick}>Click</Button>);
  
  await user.click(screen.getByRole('button'));
  
  expect(handleClick).toHaveBeenCalledTimes(1);
});
```

### Form Input
```jsx
test('handles input change', async () => {
  const user = userEvent.setup();
  render(<Input />);
  
  const input = screen.getByRole('textbox');
  await user.type(input, 'Hello');
  
  expect(input).toHaveValue('Hello');
});
```

## Queries

### getBy (throws error if not found)
```jsx
screen.getByText('Hello');
screen.getByRole('button');
screen.getByLabelText('Email');
screen.getByPlaceholderText('Enter name');
screen.getByTestId('custom-element');
```

### queryBy (returns null if not found)
```jsx
const element = screen.queryByText('Not exists');
expect(element).not.toBeInTheDocument();
```

### findBy (async, waits for element)
```jsx
test('async element appears', async () => {
  render(<AsyncComponent />);
  const element = await screen.findByText('Loaded');
  expect(element).toBeInTheDocument();
});
```

### getAllBy (multiple elements)
```jsx
const buttons = screen.getAllByRole('button');
expect(buttons).toHaveLength(3);
```

## Testing Async Code

### Async Data Fetching
```jsx
import { render, screen, waitFor } from '@testing-library/react';

test('loads and displays data', async () => {
  render(<UserList />);
  
  expect(screen.getByText('Loading...')).toBeInTheDocument();
  
  await waitFor(() => {
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });
});
```

### Mocking API Calls
```jsx
global.fetch = jest.fn(() =>
  Promise.resolve({
    json: () => Promise.resolve({ name: 'John' })
  })
);

test('fetches user data', async () => {
  render(<User id="1" />);
  
  const name = await screen.findByText('John');
  expect(name).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith('/api/users/1');
});
```

## Testing Hooks

### Custom Hook Testing
```jsx
import { renderHook, act } from '@testing-library/react';

function useCounter() {
  const [count, setCount] = useState(0);
  const increment = () => setCount(c => c + 1);
  return { count, increment };
}

test('increments counter', () => {
  const { result } = renderHook(() => useCounter());
  
  expect(result.current.count).toBe(0);
  
  act(() => {
    result.current.increment();
  });
  
  expect(result.current.count).toBe(1);
});
```

## Testing Context

```jsx
import { render, screen } from '@testing-library/react';
import { ThemeContext } from './ThemeContext';

test('uses theme from context', () => {
  render(
    <ThemeContext.Provider value="dark">
      <ThemedButton />
    </ThemeContext.Provider>
  );
  
  const button = screen.getByRole('button');
  expect(button).toHaveClass('dark');
});
```

## Mocking

### Mock Functions
```jsx
const mockFn = jest.fn();
mockFn('arg1', 'arg2');

expect(mockFn).toHaveBeenCalled();
expect(mockFn).toHaveBeenCalledWith('arg1', 'arg2');
expect(mockFn).toHaveBeenCalledTimes(1);
```

### Mock Modules
```jsx
jest.mock('./api', () => ({
  fetchUser: jest.fn(() => Promise.resolve({ name: 'John' }))
}));

import { fetchUser } from './api';

test('calls fetchUser', async () => {
  render(<User id="1" />);
  expect(fetchUser).toHaveBeenCalledWith('1');
});
```

### Mock Return Values
```jsx
const mockFn = jest.fn();
mockFn.mockReturnValue(42);
mockFn.mockReturnValueOnce(1).mockReturnValueOnce(2);
mockFn.mockResolvedValue({ data: 'async' });
```

## Snapshot Testing

```jsx
import { render } from '@testing-library/react';

test('matches snapshot', () => {
  const { container } = render(<Button>Click</Button>);
  expect(container).toMatchSnapshot();
});
```

## Testing Best Practices

### 1. Test User Behavior, Not Implementation
```jsx
// Bad - testing implementation
test('state updates on click', () => {
  const { result } = renderHook(() => useState(0));
  // Testing internal state
});

// Good - testing behavior
test('counter increments on click', async () => {
  const user = userEvent.setup();
  render(<Counter />);
  await user.click(screen.getByRole('button'));
  expect(screen.getByText('Count: 1')).toBeInTheDocument();
});
```

### 2. Use Accessible Queries
```jsx
// Priority order
screen.getByRole('button', { name: 'Submit' });
screen.getByLabelText('Email');
screen.getByPlaceholderText('Enter email');
screen.getByText('Hello');
screen.getByTestId('custom-element'); // Last resort
```

### 3. Avoid Testing Implementation Details
```jsx
// Bad
expect(component.state.count).toBe(1);

// Good
expect(screen.getByText('Count: 1')).toBeInTheDocument();
```

### 4. Clean Up After Tests
```jsx
afterEach(() => {
  jest.clearAllMocks();
  cleanup();
});
```

## Interview Questions

**Q: What is React Testing Library?**
- Testing library focused on user behavior
- Tests components as users interact with them
- Encourages accessible queries

**Q: getBy vs queryBy vs findBy?**
- getBy: Throws error if not found (synchronous)
- queryBy: Returns null if not found (assertions)
- findBy: Async, waits for element to appear

**Q: How to test async components?**
- Use findBy queries
- Use waitFor for complex async logic
- Mock API calls with jest.fn()

**Q: What is userEvent?**
- Library for simulating user interactions
- More realistic than fireEvent
- Handles keyboard, mouse, clipboard events

**Q: How to test custom hooks?**
- Use renderHook from @testing-library/react
- Wrap state updates in act()
- Test hook behavior, not implementation

**Q: What are snapshot tests?**
- Capture component output
- Compare against saved snapshot
- Detect unintended changes
- Use sparingly (brittle)

**Q: How to mock API calls?**
- jest.mock() for modules
- jest.fn() for functions
- Mock fetch globally
- Use MSW (Mock Service Worker) for complex scenarios

**Q: What is the act() function?**
- Ensures all updates are processed
- Wraps state updates in tests
- Usually handled automatically by Testing Library

**Q: How to test Context?**
- Wrap component in Provider
- Pass test values to Provider
- Test component behavior with context
