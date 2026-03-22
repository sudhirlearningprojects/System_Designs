# Forms and Controlled Components

## Controlled Components

Components where form data is handled by React state.

### Basic Input
```jsx
function Form() {
  const [name, setName] = useState('');
  
  const handleSubmit = (e) => {
    e.preventDefault();
    console.log('Submitted:', name);
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <button type="submit">Submit</button>
    </form>
  );
}
```

### Multiple Inputs
```jsx
function Form() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    age: ''
  });
  
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  return (
    <form>
      <input name="name" value={formData.name} onChange={handleChange} />
      <input name="email" value={formData.email} onChange={handleChange} />
      <input name="age" value={formData.age} onChange={handleChange} />
    </form>
  );
}
```

### Textarea
```jsx
function Form() {
  const [text, setText] = useState('');
  
  return (
    <textarea
      value={text}
      onChange={(e) => setText(e.target.value)}
    />
  );
}
```

### Select
```jsx
function Form() {
  const [selected, setSelected] = useState('apple');
  
  return (
    <select value={selected} onChange={(e) => setSelected(e.target.value)}>
      <option value="apple">Apple</option>
      <option value="banana">Banana</option>
      <option value="orange">Orange</option>
    </select>
  );
}
```

### Checkbox
```jsx
function Form() {
  const [checked, setChecked] = useState(false);
  
  return (
    <input
      type="checkbox"
      checked={checked}
      onChange={(e) => setChecked(e.target.checked)}
    />
  );
}
```

### Radio Buttons
```jsx
function Form() {
  const [selected, setSelected] = useState('option1');
  
  return (
    <>
      <input
        type="radio"
        value="option1"
        checked={selected === 'option1'}
        onChange={(e) => setSelected(e.target.value)}
      />
      <input
        type="radio"
        value="option2"
        checked={selected === 'option2'}
        onChange={(e) => setSelected(e.target.value)}
      />
    </>
  );
}
```

## Uncontrolled Components

Form data is handled by the DOM itself using refs.

```jsx
function Form() {
  const nameRef = useRef();
  const emailRef = useRef();
  
  const handleSubmit = (e) => {
    e.preventDefault();
    console.log(nameRef.current.value);
    console.log(emailRef.current.value);
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input ref={nameRef} defaultValue="John" />
      <input ref={emailRef} type="email" />
      <button type="submit">Submit</button>
    </form>
  );
}
```

## Form Validation

### Basic Validation
```jsx
function Form() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  
  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!email.includes('@')) {
      setError('Invalid email');
      return;
    }
    
    setError('');
    // Submit form
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      {error && <span>{error}</span>}
      <button type="submit">Submit</button>
    </form>
  );
}
```

### Custom Hook for Form Handling
```jsx
function useForm(initialValues, validate) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  
  const handleChange = (e) => {
    const { name, value } = e.target;
    setValues(prev => ({ ...prev, [name]: value }));
  };
  
  const handleSubmit = (callback) => (e) => {
    e.preventDefault();
    const validationErrors = validate(values);
    setErrors(validationErrors);
    
    if (Object.keys(validationErrors).length === 0) {
      callback();
    }
  };
  
  return { values, errors, handleChange, handleSubmit };
}

// Usage
function Form() {
  const validate = (values) => {
    const errors = {};
    if (!values.email.includes('@')) {
      errors.email = 'Invalid email';
    }
    return errors;
  };
  
  const { values, errors, handleChange, handleSubmit } = useForm(
    { email: '', password: '' },
    validate
  );
  
  const onSubmit = () => {
    console.log('Form submitted:', values);
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input name="email" value={values.email} onChange={handleChange} />
      {errors.email && <span>{errors.email}</span>}
      
      <input name="password" type="password" value={values.password} onChange={handleChange} />
      {errors.password && <span>{errors.password}</span>}
      
      <button type="submit">Submit</button>
    </form>
  );
}
```

## Form Libraries

### React Hook Form
```jsx
import { useForm } from 'react-hook-form';

function Form() {
  const { register, handleSubmit, formState: { errors } } = useForm();
  
  const onSubmit = (data) => {
    console.log(data);
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email', { required: true, pattern: /^\S+@\S+$/i })} />
      {errors.email && <span>Email is required</span>}
      
      <input {...register('password', { required: true, minLength: 6 })} />
      {errors.password && <span>Password must be at least 6 characters</span>}
      
      <button type="submit">Submit</button>
    </form>
  );
}
```

### Formik
```jsx
import { Formik, Form, Field, ErrorMessage } from 'formik';

function MyForm() {
  return (
    <Formik
      initialValues={{ email: '', password: '' }}
      validate={values => {
        const errors = {};
        if (!values.email) {
          errors.email = 'Required';
        }
        return errors;
      }}
      onSubmit={(values) => {
        console.log(values);
      }}
    >
      <Form>
        <Field name="email" type="email" />
        <ErrorMessage name="email" component="div" />
        
        <Field name="password" type="password" />
        <ErrorMessage name="password" component="div" />
        
        <button type="submit">Submit</button>
      </Form>
    </Formik>
  );
}
```

## Interview Questions

**Q: Controlled vs Uncontrolled components?**
- Controlled: React state manages form data (recommended)
- Uncontrolled: DOM manages form data via refs

**Q: When to use uncontrolled components?**
- File inputs (always uncontrolled)
- Integrating with non-React code
- Simple forms without validation

**Q: How to handle multiple inputs?**
- Use single state object
- Use name attribute to identify inputs
- Update state dynamically using computed property names

**Q: What is the purpose of preventDefault()?**
- Prevents default form submission behavior
- Stops page reload
- Allows custom handling

**Q: How to validate forms in React?**
- Manual validation in submit handler
- Custom validation hooks
- Libraries: React Hook Form, Formik, Yup

**Q: What are the benefits of controlled components?**
- Single source of truth (React state)
- Easy validation
- Conditional rendering
- Dynamic form behavior

**Q: How to handle file uploads?**
- Use uncontrolled component with ref
- Access files via ref.current.files
- Use FormData for submission
