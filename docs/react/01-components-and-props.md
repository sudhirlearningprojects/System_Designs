# Components and Props

## What are Components?

Components are the building blocks of React applications. They are reusable, independent pieces of UI that can be composed together.

## Types of Components

### 1. Functional Components
```jsx
function Welcome(props) {
  return <h1>Hello, {props.name}</h1>;
}

// Arrow function syntax
const Welcome = (props) => {
  return <h1>Hello, {props.name}</h1>;
};
```

### 2. Class Components (Legacy)
```jsx
class Welcome extends React.Component {
  render() {
    return <h1>Hello, {this.props.name}</h1>;
  }
}
```

## Props (Properties)

Props are arguments passed to components, similar to function parameters. They are **read-only** and flow down from parent to child (unidirectional data flow).

### Passing Props
```jsx
function App() {
  return <Welcome name="Sara" age={25} />;
}

function Welcome(props) {
  return <h1>Hello, {props.name}, you are {props.age}</h1>;
}
```

### Destructuring Props
```jsx
function Welcome({ name, age }) {
  return <h1>Hello, {name}, you are {age}</h1>;
}
```

### Default Props
```jsx
function Welcome({ name = "Guest", age = 0 }) {
  return <h1>Hello, {name}, you are {age}</h1>;
}
```

### Props.children
```jsx
function Card({ children }) {
  return <div className="card">{children}</div>;
}

// Usage
<Card>
  <h1>Title</h1>
  <p>Content</p>
</Card>
```

## Key Interview Questions

**Q: What's the difference between props and state?**
- Props are passed from parent, immutable in child component
- State is managed within component, mutable

**Q: Can you modify props?**
- No, props are read-only. Modifying them violates React's one-way data flow

**Q: How to pass data from child to parent?**
- Pass a callback function as prop from parent to child
```jsx
function Parent() {
  const handleData = (data) => console.log(data);
  return <Child onData={handleData} />;
}

function Child({ onData }) {
  return <button onClick={() => onData("Hello")}>Send</button>;
}
```

**Q: What is prop drilling?**
- Passing props through multiple levels of components
- Solution: Context API, Redux, or component composition
