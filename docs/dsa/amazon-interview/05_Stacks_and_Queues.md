# Amazon Interview - Stacks & Queues Problems

## Problem 24: Implement Queue using Stacks (LeetCode 232) ⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: High  
**Pattern**: Stack

### Solution
```java
class MyQueue {
    private Stack<Integer> input;
    private Stack<Integer> output;
    
    public MyQueue() {
        input = new Stack<>();
        output = new Stack<>();
    }
    
    public void push(int x) {
        input.push(x);
    }
    
    public int pop() {
        peek();
        return output.pop();
    }
    
    public int peek() {
        if (output.isEmpty()) {
            while (!input.isEmpty()) {
                output.push(input.pop());
            }
        }
        return output.peek();
    }
    
    public boolean empty() {
        return input.isEmpty() && output.isEmpty();
    }
}
```
**Time**: O(1) amortized, **Space**: O(n)

---

## Problem 25: Min Stack (LeetCode 155) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: Stack

### Solution
```java
class MinStack {
    private Stack<Integer> stack;
    private Stack<Integer> minStack;
    
    public MinStack() {
        stack = new Stack<>();
        minStack = new Stack<>();
    }
    
    public void push(int val) {
        stack.push(val);
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        }
    }
    
    public void pop() {
        int val = stack.pop();
        if (val == minStack.peek()) {
            minStack.pop();
        }
    }
    
    public int top() {
        return stack.peek();
    }
    
    public int getMin() {
        return minStack.peek();
    }
}
```
**Time**: O(1) all operations, **Space**: O(n)

---

## Problem 26: Evaluate Reverse Polish Notation (LeetCode 150) ⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: Medium  
**Pattern**: Stack

### Solution
```java
public int evalRPN(String[] tokens) {
    Stack<Integer> stack = new Stack<>();
    
    for (String token : tokens) {
        if (token.equals("+")) {
            stack.push(stack.pop() + stack.pop());
        } else if (token.equals("-")) {
            int b = stack.pop();
            int a = stack.pop();
            stack.push(a - b);
        } else if (token.equals("*")) {
            stack.push(stack.pop() * stack.pop());
        } else if (token.equals("/")) {
            int b = stack.pop();
            int a = stack.pop();
            stack.push(a / b);
        } else {
            stack.push(Integer.parseInt(token));
        }
    }
    
    return stack.pop();
}
```
**Time**: O(n), **Space**: O(n)

---

**Next**: [Design Problems](06_Design_Problems.md)
