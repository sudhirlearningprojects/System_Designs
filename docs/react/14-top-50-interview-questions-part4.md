# Top 50 Interview Questions - Part 4 (Questions 31-40)

## 31. How do you implement a custom dropdown/select component in React?

**Answer:**

```jsx
function Dropdown({ options, value, onChange, placeholder }) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (option) => {
    onChange(option);
    setIsOpen(false);
  };

  const selectedOption = options.find(opt => opt.value === value);

  return (
    <div className="dropdown" ref={dropdownRef}>
      <button
        className="dropdown-toggle"
        onClick={() => setIsOpen(!isOpen)}
      >
        {selectedOption ? selectedOption.label : placeholder}
        <span>{isOpen ? '▲' : '▼'}</span>
      </button>

      {isOpen && (
        <ul className="dropdown-menu">
          {options.map(option => (
            <li
              key={option.value}
              onClick={() => handleSelect(option)}
              className={value === option.value ? 'selected' : ''}
            >
              {option.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

// Usage
function App() {
  const [selected, setSelected] = useState('');
  const options = [
    { value: '1', label: 'Option 1' },
    { value: '2', label: 'Option 2' },
    { value: '3', label: 'Option 3' }
  ];

  return (
    <Dropdown
      options={options}
      value={selected}
      onChange={(opt) => setSelected(opt.value)}
      placeholder="Select an option"
    />
  );
}
```

---

## 32. How do you implement autocomplete/typeahead in React?

**Answer:**

```jsx
function Autocomplete({ suggestions, onSelect }) {
  const [input, setInput] = useState('');
  const [filteredSuggestions, setFilteredSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [activeSuggestion, setActiveSuggestion] = useState(0);

  const handleChange = (e) => {
    const value = e.target.value;
    setInput(value);

    if (value) {
      const filtered = suggestions.filter(suggestion =>
        suggestion.toLowerCase().includes(value.toLowerCase())
      );
      setFilteredSuggestions(filtered);
      setShowSuggestions(true);
    } else {
      setShowSuggestions(false);
    }
  };

  const handleClick = (suggestion) => {
    setInput(suggestion);
    setShowSuggestions(false);
    onSelect(suggestion);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      setActiveSuggestion(prev =>
        prev < filteredSuggestions.length - 1 ? prev + 1 : prev
      );
    } else if (e.key === 'ArrowUp') {
      setActiveSuggestion(prev => prev > 0 ? prev - 1 : 0);
    } else if (e.key === 'Enter') {
      setInput(filteredSuggestions[activeSuggestion]);
      setShowSuggestions(false);
      onSelect(filteredSuggestions[activeSuggestion]);
    }
  };

  return (
    <div className="autocomplete">
      <input
        type="text"
        value={input}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        placeholder="Type to search..."
      />
      {showSuggestions && filteredSuggestions.length > 0 && (
        <ul className="suggestions">
          {filteredSuggestions.map((suggestion, index) => (
            <li
              key={index}
              onClick={() => handleClick(suggestion)}
              className={index === activeSuggestion ? 'active' : ''}
            >
              {suggestion}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
```

---

## 33. How do you implement a carousel/slider in React?

**Answer:**

```jsx
function Carousel({ images }) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const goToPrevious = () => {
    setCurrentIndex(prev =>
      prev === 0 ? images.length - 1 : prev - 1
    );
  };

  const goToNext = () => {
    setCurrentIndex(prev =>
      prev === images.length - 1 ? 0 : prev + 1
    );
  };

  const goToSlide = (index) => {
    setCurrentIndex(index);
  };

  useEffect(() => {
    const interval = setInterval(goToNext, 3000);
    return () => clearInterval(interval);
  }, [currentIndex]);

  return (
    <div className="carousel">
      <button className="carousel-btn prev" onClick={goToPrevious}>
        ‹
      </button>

      <div className="carousel-content">
        <img src={images[currentIndex]} alt={`Slide ${currentIndex}`} />
      </div>

      <button className="carousel-btn next" onClick={goToNext}>
        ›
      </button>

      <div className="carousel-dots">
        {images.map((_, index) => (
          <button
            key={index}
            className={index === currentIndex ? 'active' : ''}
            onClick={() => goToSlide(index)}
          />
        ))}
      </div>
    </div>
  );
}
```

---

## 34. How do you implement infinite scroll with React Query?

**Answer:**

```jsx
import { useInfiniteQuery } from '@tanstack/react-query';
import { useInView } from 'react-intersection-observer';

function InfiniteScrollList() {
  const { ref, inView } = useInView();

  const fetchPosts = async ({ pageParam = 1 }) => {
    const response = await fetch(`/api/posts?page=${pageParam}`);
    return response.json();
  };

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    status
  } = useInfiniteQuery({
    queryKey: ['posts'],
    queryFn: fetchPosts,
    getNextPageParam: (lastPage, pages) => {
      return lastPage.hasMore ? pages.length + 1 : undefined;
    }
  });

  useEffect(() => {
    if (inView && hasNextPage) {
      fetchNextPage();
    }
  }, [inView, hasNextPage, fetchNextPage]);

  if (status === 'loading') return <div>Loading...</div>;
  if (status === 'error') return <div>Error loading posts</div>;

  return (
    <div>
      {data.pages.map((page, i) => (
        <div key={i}>
          {page.posts.map(post => (
            <div key={post.id} className="post">
              <h3>{post.title}</h3>
              <p>{post.content}</p>
            </div>
          ))}
        </div>
      ))}

      <div ref={ref}>
        {isFetchingNextPage && <div>Loading more...</div>}
      </div>
    </div>
  );
}
```

---

## 35. How do you implement optimistic updates in React?

**Answer:**

```jsx
import { useMutation, useQueryClient } from '@tanstack/react-query';

function TodoList() {
  const queryClient = useQueryClient();

  const addTodoMutation = useMutation({
    mutationFn: (newTodo) => fetch('/api/todos', {
      method: 'POST',
      body: JSON.stringify(newTodo)
    }),
    onMutate: async (newTodo) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['todos'] });

      // Snapshot previous value
      const previousTodos = queryClient.getQueryData(['todos']);

      // Optimistically update
      queryClient.setQueryData(['todos'], (old) => [...old, newTodo]);

      // Return context with snapshot
      return { previousTodos };
    },
    onError: (err, newTodo, context) => {
      // Rollback on error
      queryClient.setQueryData(['todos'], context.previousTodos);
    },
    onSettled: () => {
      // Refetch after error or success
      queryClient.invalidateQueries({ queryKey: ['todos'] });
    }
  });

  const handleAddTodo = () => {
    addTodoMutation.mutate({
      id: Date.now(),
      text: 'New Todo',
      completed: false
    });
  };

  return (
    <div>
      <button onClick={handleAddTodo}>Add Todo</button>
    </div>
  );
}
```

---

## 36. How do you implement undo/redo functionality in React?

**Answer:**

```jsx
function useUndoRedo(initialState) {
  const [history, setHistory] = useState([initialState]);
  const [currentIndex, setCurrentIndex] = useState(0);

  const state = history[currentIndex];

  const setState = (newState) => {
    const newHistory = history.slice(0, currentIndex + 1);
    newHistory.push(newState);
    setHistory(newHistory);
    setCurrentIndex(newHistory.length - 1);
  };

  const undo = () => {
    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1);
    }
  };

  const redo = () => {
    if (currentIndex < history.length - 1) {
      setCurrentIndex(currentIndex + 1);
    }
  };

  const canUndo = currentIndex > 0;
  const canRedo = currentIndex < history.length - 1;

  return { state, setState, undo, redo, canUndo, canRedo };
}

// Usage
function DrawingApp() {
  const { state, setState, undo, redo, canUndo, canRedo } = useUndoRedo([]);

  const addShape = (shape) => {
    setState([...state, shape]);
  };

  return (
    <div>
      <button onClick={undo} disabled={!canUndo}>Undo</button>
      <button onClick={redo} disabled={!canRedo}>Redo</button>
      <button onClick={() => addShape({ type: 'circle' })}>Add Circle</button>
      <div>Shapes: {state.length}</div>
    </div>
  );
}
```

---

## 37. How do you implement a shopping cart in React?

**Answer:**

```jsx
const CartContext = createContext();

function CartProvider({ children }) {
  const [cart, setCart] = useState([]);

  const addToCart = (product) => {
    setCart(prev => {
      const existing = prev.find(item => item.id === product.id);
      if (existing) {
        return prev.map(item =>
          item.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      }
      return [...prev, { ...product, quantity: 1 }];
    });
  };

  const removeFromCart = (productId) => {
    setCart(prev => prev.filter(item => item.id !== productId));
  };

  const updateQuantity = (productId, quantity) => {
    if (quantity <= 0) {
      removeFromCart(productId);
      return;
    }
    setCart(prev =>
      prev.map(item =>
        item.id === productId ? { ...item, quantity } : item
      )
    );
  };

  const clearCart = () => {
    setCart([]);
  };

  const total = cart.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );

  const itemCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <CartContext.Provider
      value={{
        cart,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        total,
        itemCount
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

function useCart() {
  return useContext(CartContext);
}

// Usage
function ProductCard({ product }) {
  const { addToCart } = useCart();

  return (
    <div>
      <h3>{product.name}</h3>
      <p>${product.price}</p>
      <button onClick={() => addToCart(product)}>Add to Cart</button>
    </div>
  );
}

function Cart() {
  const { cart, updateQuantity, removeFromCart, total } = useCart();

  return (
    <div>
      {cart.map(item => (
        <div key={item.id}>
          <span>{item.name}</span>
          <input
            type="number"
            value={item.quantity}
            onChange={(e) => updateQuantity(item.id, parseInt(e.target.value))}
          />
          <span>${item.price * item.quantity}</span>
          <button onClick={() => removeFromCart(item.id)}>Remove</button>
        </div>
      ))}
      <div>Total: ${total}</div>
    </div>
  );
}
```

---

## 38. How do you implement a countdown timer in React?

**Answer:**

```jsx
function useCountdown(targetDate) {
  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());

  function calculateTimeLeft() {
    const difference = new Date(targetDate) - new Date();
    
    if (difference > 0) {
      return {
        days: Math.floor(difference / (1000 * 60 * 60 * 24)),
        hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
        minutes: Math.floor((difference / 1000 / 60) % 60),
        seconds: Math.floor((difference / 1000) % 60)
      };
    }
    
    return { days: 0, hours: 0, minutes: 0, seconds: 0 };
  }

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(calculateTimeLeft());
    }, 1000);

    return () => clearInterval(timer);
  }, [targetDate]);

  return timeLeft;
}

// Usage
function CountdownTimer({ targetDate }) {
  const { days, hours, minutes, seconds } = useCountdown(targetDate);

  return (
    <div className="countdown">
      <div className="countdown-item">
        <span className="value">{days}</span>
        <span className="label">Days</span>
      </div>
      <div className="countdown-item">
        <span className="value">{hours}</span>
        <span className="label">Hours</span>
      </div>
      <div className="countdown-item">
        <span className="value">{minutes}</span>
        <span className="label">Minutes</span>
      </div>
      <div className="countdown-item">
        <span className="value">{seconds}</span>
        <span className="label">Seconds</span>
      </div>
    </div>
  );
}
```

---

## 39. How do you implement copy to clipboard in React?

**Answer:**

```jsx
function useCopyToClipboard() {
  const [copiedText, setCopiedText] = useState(null);

  const copy = async (text) => {
    if (!navigator?.clipboard) {
      console.warn('Clipboard not supported');
      return false;
    }

    try {
      await navigator.clipboard.writeText(text);
      setCopiedText(text);
      return true;
    } catch (error) {
      console.warn('Copy failed', error);
      setCopiedText(null);
      return false;
    }
  };

  return [copiedText, copy];
}

// Usage
function CopyButton({ text }) {
  const [copiedText, copy] = useCopyToClipboard();
  const [showFeedback, setShowFeedback] = useState(false);

  const handleCopy = async () => {
    const success = await copy(text);
    if (success) {
      setShowFeedback(true);
      setTimeout(() => setShowFeedback(false), 2000);
    }
  };

  return (
    <div>
      <button onClick={handleCopy}>
        {showFeedback ? 'Copied!' : 'Copy'}
      </button>
      <code>{text}</code>
    </div>
  );
}
```

---

## 40. How do you implement a rating component in React?

**Answer:**

```jsx
function StarRating({ value, onChange, max = 5, readonly = false }) {
  const [hoverValue, setHoverValue] = useState(null);

  const handleClick = (rating) => {
    if (!readonly) {
      onChange(rating);
    }
  };

  const handleMouseEnter = (rating) => {
    if (!readonly) {
      setHoverValue(rating);
    }
  };

  const handleMouseLeave = () => {
    setHoverValue(null);
  };

  return (
    <div className="star-rating">
      {[...Array(max)].map((_, index) => {
        const rating = index + 1;
        const isFilled = rating <= (hoverValue || value);

        return (
          <button
            key={index}
            type="button"
            className={`star ${isFilled ? 'filled' : ''}`}
            onClick={() => handleClick(rating)}
            onMouseEnter={() => handleMouseEnter(rating)}
            onMouseLeave={handleMouseLeave}
            disabled={readonly}
          >
            {isFilled ? '★' : '☆'}
          </button>
        );
      })}
    </div>
  );
}

// Usage
function ProductReview() {
  const [rating, setRating] = useState(0);

  return (
    <div>
      <h3>Rate this product:</h3>
      <StarRating value={rating} onChange={setRating} />
      <p>Your rating: {rating} stars</p>
    </div>
  );
}
```

---

**Continue to Part 5 for questions 41-50...**
