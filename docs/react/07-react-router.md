# React Router

## Installation

```bash
npm install react-router-dom
```

## Basic Setup

```jsx
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/">Home</Link>
        <Link to="/about">About</Link>
      </nav>
      
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
```

## Navigation

### Link Component
```jsx
import { Link } from 'react-router-dom';

<Link to="/about">About</Link>
<Link to="/user/123">User Profile</Link>
```

### NavLink (Active Styling)
```jsx
import { NavLink } from 'react-router-dom';

<NavLink 
  to="/about"
  className={({ isActive }) => isActive ? 'active' : ''}
>
  About
</NavLink>
```

### Programmatic Navigation
```jsx
import { useNavigate } from 'react-router-dom';

function Component() {
  const navigate = useNavigate();
  
  const handleClick = () => {
    navigate('/about');
    // navigate(-1); // Go back
    // navigate(1); // Go forward
  };
  
  return <button onClick={handleClick}>Go to About</button>;
}
```

## Route Parameters

### URL Parameters
```jsx
import { useParams } from 'react-router-dom';

// Route definition
<Route path="/user/:id" element={<User />} />

// Component
function User() {
  const { id } = useParams();
  return <div>User ID: {id}</div>;
}
```

### Query Parameters
```jsx
import { useSearchParams } from 'react-router-dom';

function Search() {
  const [searchParams, setSearchParams] = useSearchParams();
  
  const query = searchParams.get('q');
  const page = searchParams.get('page');
  
  const updateQuery = (newQuery) => {
    setSearchParams({ q: newQuery, page: 1 });
  };
  
  return <div>Search: {query}, Page: {page}</div>;
}

// URL: /search?q=react&page=2
```

## Nested Routes

```jsx
function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="about" element={<About />} />
        <Route path="users" element={<Users />}>
          <Route path=":id" element={<UserDetail />} />
        </Route>
      </Route>
    </Routes>
  );
}

function Layout() {
  return (
    <div>
      <nav>Navigation</nav>
      <Outlet /> {/* Child routes render here */}
    </div>
  );
}
```

## Protected Routes

```jsx
import { Navigate } from 'react-router-dom';

function ProtectedRoute({ children }) {
  const isAuthenticated = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return children;
}

// Usage
<Route 
  path="/dashboard" 
  element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  } 
/>
```

## Lazy Loading Routes

```jsx
import { lazy, Suspense } from 'react';

const About = lazy(() => import('./pages/About'));
const Dashboard = lazy(() => import('./pages/Dashboard'));

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Routes>
        <Route path="/about" element={<About />} />
        <Route path="/dashboard" element={<Dashboard />} />
      </Routes>
    </Suspense>
  );
}
```

## Hooks

### useLocation
```jsx
import { useLocation } from 'react-router-dom';

function Component() {
  const location = useLocation();
  
  console.log(location.pathname); // "/about"
  console.log(location.search); // "?q=react"
  console.log(location.hash); // "#section"
  console.log(location.state); // Passed state
  
  return <div>Current path: {location.pathname}</div>;
}
```

### useNavigate with State
```jsx
function ListPage() {
  const navigate = useNavigate();
  
  const handleClick = () => {
    navigate('/detail', { state: { from: 'list' } });
  };
}

function DetailPage() {
  const location = useLocation();
  const from = location.state?.from; // 'list'
}
```

### useMatch
```jsx
import { useMatch } from 'react-router-dom';

function Component() {
  const match = useMatch('/users/:id');
  
  if (match) {
    console.log(match.params.id);
  }
}
```

## Route Configuration

```jsx
const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'about', element: <About /> },
      {
        path: 'users',
        element: <Users />,
        children: [
          { path: ':id', element: <UserDetail /> }
        ]
      }
    ]
  }
];

function App() {
  return <RouterProvider router={createBrowserRouter(routes)} />;
}
```

## Interview Questions

**Q: What is React Router?**
- Library for routing in React applications
- Enables navigation between views
- Manages browser history

**Q: BrowserRouter vs HashRouter?**
- BrowserRouter: Uses HTML5 history API (clean URLs)
- HashRouter: Uses URL hash (#) (works without server config)

**Q: What is the difference between Link and NavLink?**
- Link: Basic navigation
- NavLink: Adds active class/styling when route matches

**Q: How to pass data between routes?**
- URL parameters: /user/:id
- Query parameters: /search?q=react
- State: navigate('/path', { state: data })
- Context API

**Q: What is Outlet?**
- Placeholder for child routes in nested routing
- Renders matched child route component

**Q: How to implement protected routes?**
- Create wrapper component
- Check authentication
- Redirect to login if not authenticated

**Q: What is lazy loading in routing?**
- Load route components on demand
- Reduces initial bundle size
- Uses React.lazy() and Suspense

**Q: How to get current route?**
- useLocation() hook
- Returns pathname, search, hash, state

**Q: What is the replace prop in Navigate?**
- Replaces current history entry instead of adding new one
- User can't go back to previous page
