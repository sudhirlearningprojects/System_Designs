# Convert Infix to Prefix Notation

## Problem Statement

Convert an infix expression to prefix notation (Polish Notation). Infix notation places operators between operands (A + B), while prefix notation places operators before operands (+ A B).

**Input:** Infix expression as string  
**Output:** Prefix expression as string

**Examples:**
```
Input:  "A+B"
Output: "+AB"

Input:  "A+B*C"
Output: "+A*BC"

Input:  "(A+B)*C"
Output: "*+ABC"

Input:  "A*(B+C)/D"
Output: "/*A+BCD"

Input:  "((A+B)*C-D)/E"
Output: "/-*+ABCDE"
```

---

## Solution Approaches

### Approach 1: Reverse and Convert to Postfix (Optimal)

**Algorithm:**
1. Reverse the infix expression
2. Replace '(' with ')' and vice versa
3. Convert to postfix using standard algorithm
4. Reverse the result to get prefix

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String infixToPrefix(String infix) {
    String reversed = new StringBuilder(infix).reverse().toString();
    reversed = reversed.replace('(', '#').replace(')', '(').replace('#', ')');
    String postfix = infixToPostfix(reversed);
    return new StringBuilder(postfix).reverse().toString();
}

private static String infixToPostfix(String infix) {
    StringBuilder result = new StringBuilder();
    Stack<Character> stack = new Stack<>();
    
    for (char c : infix.toCharArray()) {
        if (Character.isLetterOrDigit(c)) {
            result.append(c);
        } else if (c == '(') {
            stack.push(c);
        } else if (c == ')') {
            while (!stack.isEmpty() && stack.peek() != '(') {
                result.append(stack.pop());
            }
            stack.pop();
        } else {
            while (!stack.isEmpty() && precedence(c) < precedence(stack.peek())) {
                result.append(stack.pop());
            }
            stack.push(c);
        }
    }
    
    while (!stack.isEmpty()) {
        result.append(stack.pop());
    }
    
    return result.toString();
}

private static int precedence(char op) {
    if (op == '+' || op == '-') return 1;
    if (op == '*' || op == '/') return 2;
    if (op == '^') return 3;
    return 0;
}
```

---

### Approach 2: Direct Stack-Based Conversion

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String infixToPrefixDirect(String infix) {
    StringBuilder result = new StringBuilder();
    Stack<Character> stack = new Stack<>();
    
    for (int i = infix.length() - 1; i >= 0; i--) {
        char c = infix.charAt(i);
        
        if (Character.isLetterOrDigit(c)) {
            result.append(c);
        } else if (c == ')') {
            stack.push(c);
        } else if (c == '(') {
            while (!stack.isEmpty() && stack.peek() != ')') {
                result.append(stack.pop());
            }
            stack.pop();
        } else {
            while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                result.append(stack.pop());
            }
            stack.push(c);
        }
    }
    
    while (!stack.isEmpty()) {
        result.append(stack.pop());
    }
    
    return result.reverse().toString();
}
```

---

## Algorithm Walkthrough

### Example: "(A+B)*C"

**Step-by-Step Conversion:**

```
Original Infix: (A+B)*C

Step 1: Reverse the expression
  Reversed: C*)B+A(

Step 2: Swap parentheses
  Modified: C*(B+A)

Step 3: Convert to Postfix
  
  Process 'C': operand → output = "C"
  Process '*': operator → stack = [*]
  Process '(': push → stack = [*, (]
  Process 'B': operand → output = "CB"
  Process '+': operator → stack = [*, (, +]
  Process 'A': operand → output = "CBA"
  Process ')': pop until '(' → output = "CBA+", stack = [*]
  End: pop all → output = "CBA+*"

Step 4: Reverse the postfix
  Result: *+ABC

Final Prefix: *+ABC
```

### Example: "A+B*C"

```
Original Infix: A+B*C

Step 1: Reverse
  Reversed: C*B+A

Step 2: Swap parentheses (none)
  Modified: C*B+A

Step 3: Convert to Postfix
  
  Process 'C': operand → output = "C"
  Process '*': operator → stack = [*]
  Process 'B': operand → output = "CB"
  Process '+': precedence(*) > precedence(+)
               pop * → output = "CB*", stack = [+]
  Process 'A': operand → output = "CB*A"
  End: pop all → output = "CB*A+"

Step 4: Reverse
  Result: +A*BC

Final Prefix: +A*BC
```

### Example: "A*(B+C)/D"

```
Original Infix: A*(B+C)/D

Step 1: Reverse
  Reversed: D/)C+B(*A

Step 2: Swap parentheses
  Modified: D/(C+B)*A

Step 3: Convert to Postfix
  
  Process 'D': output = "D"
  Process '/': stack = [/]
  Process '(': stack = [/, (]
  Process 'C': output = "DC"
  Process '+': stack = [/, (, +]
  Process 'B': output = "DCB"
  Process ')': pop until '(' → output = "DCB+", stack = [/]
  Process '*': precedence(*) = precedence(/)
               pop / → output = "DCB+/", stack = [*]
  Process 'A': output = "DCB+/A"
  End: pop all → output = "DCB+/A*"

Step 4: Reverse
  Result: *A/+BCD

Final Prefix: /*A+BCD
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Reverse and Convert to Postfix (Optimal)
    public static String infixToPrefix(String infix) {
        String reversed = new StringBuilder(infix).reverse().toString();
        reversed = reversed.replace('(', '#').replace(')', '(').replace('#', ')');
        String postfix = infixToPostfix(reversed);
        return new StringBuilder(postfix).reverse().toString();
    }
    
    private static String infixToPostfix(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        
        for (char c : infix.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.pop());
                }
                if (!stack.isEmpty()) stack.pop();
            } else {
                while (!stack.isEmpty() && precedence(c) < precedence(stack.peek())) {
                    result.append(stack.pop());
                }
                stack.push(c);
            }
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        return result.toString();
    }
    
    // Approach 2: Direct Stack-Based Conversion
    public static String infixToPrefixDirect(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        
        for (int i = infix.length() - 1; i >= 0; i--) {
            char c = infix.charAt(i);
            
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else if (c == ')') {
                stack.push(c);
            } else if (c == '(') {
                while (!stack.isEmpty() && stack.peek() != ')') {
                    result.append(stack.pop());
                }
                if (!stack.isEmpty()) stack.pop();
            } else {
                while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                    result.append(stack.pop());
                }
                stack.push(c);
            }
        }
        
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        return result.reverse().toString();
    }
    
    private static int precedence(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        if (op == '^') return 3;
        return 0;
    }
    
    // Helper: Evaluate prefix expression
    public static int evaluatePrefix(String prefix) {
        Stack<Integer> stack = new Stack<>();
        
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            
            if (Character.isDigit(c)) {
                stack.push(c - '0');
            } else {
                int op1 = stack.pop();
                int op2 = stack.pop();
                
                switch (c) {
                    case '+': stack.push(op1 + op2); break;
                    case '-': stack.push(op1 - op2); break;
                    case '*': stack.push(op1 * op2); break;
                    case '/': stack.push(op1 / op2); break;
                }
            }
        }
        
        return stack.pop();
    }
    
    public static boolean doTestsPass() {
        // Test 1: Simple addition
        if (!infixToPrefix("A+B").equals("+AB")) return false;
        
        // Test 2: Operator precedence
        if (!infixToPrefix("A+B*C").equals("+A*BC")) return false;
        
        // Test 3: Parentheses
        if (!infixToPrefix("(A+B)*C").equals("*+ABC")) return false;
        
        // Test 4: Complex expression
        if (!infixToPrefix("A*(B+C)/D").equals("/*A+BCD")) return false;
        
        // Test 5: Nested parentheses
        if (!infixToPrefix("((A+B)*C-D)/E").equals("/-*+ABCDE")) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        String[] tests = {
            "A+B",
            "A+B*C",
            "(A+B)*C",
            "A*(B+C)/D",
            "((A+B)*C-D)/E",
            "A+B-C*D/E"
        };
        
        for (String infix : tests) {
            String prefix = infixToPrefix(infix);
            System.out.println("Infix:  " + infix);
            System.out.println("Prefix: " + prefix);
            System.out.println();
        }
        
        // Evaluate numeric expression
        String numericInfix = "2+3*4";
        String numericPrefix = infixToPrefix(numericInfix);
        int result = evaluatePrefix(numericPrefix);
        System.out.println("Evaluate: " + numericInfix + " = " + result);
    }
}
```

---

## Test Cases

```java
@Test
public void testInfixToPrefix() {
    // Test 1: Simple operations
    assertEquals("+AB", infixToPrefix("A+B"));
    assertEquals("-AB", infixToPrefix("A-B"));
    assertEquals("*AB", infixToPrefix("A*B"));
    assertEquals("/AB", infixToPrefix("A/B"));
    
    // Test 2: Operator precedence
    assertEquals("+A*BC", infixToPrefix("A+B*C"));
    assertEquals("*+ABC", infixToPrefix("A+B*C"));
    assertEquals("-A/BC", infixToPrefix("A-B/C"));
    
    // Test 3: Parentheses
    assertEquals("*+ABC", infixToPrefix("(A+B)*C"));
    assertEquals("+A*BC", infixToPrefix("A+(B*C)"));
    assertEquals("/+ABC", infixToPrefix("(A+B)/C"));
    
    // Test 4: Complex expressions
    assertEquals("/*A+BCD", infixToPrefix("A*(B+C)/D"));
    assertEquals("/-*+ABCDE", infixToPrefix("((A+B)*C-D)/E"));
    assertEquals("+A-B/*CDE", infixToPrefix("A+B-C*D/E"));
    
    // Test 5: Single operand
    assertEquals("A", infixToPrefix("A"));
    
    // Test 6: Exponentiation
    assertEquals("^A+BC", infixToPrefix("A^(B+C)"));
    assertEquals("+A^BC", infixToPrefix("A+B^C"));
    
    // Test 7: Numeric evaluation
    assertEquals(14, evaluatePrefix(infixToPrefix("2+3*4")));
    assertEquals(20, evaluatePrefix(infixToPrefix("(2+3)*4")));
}
```

---

## Visual Representation

### Operator Precedence Table

```
Operator | Precedence | Associativity
---------|------------|---------------
   ^     |     3      | Right-to-Left
  *, /   |     2      | Left-to-Right
  +, -   |     1      | Left-to-Right
  (, )   |     0      | N/A
```

### Conversion Process Diagram

```
Infix:    (A + B) * C
          ↓
Reverse:  C * ) B + A (
          ↓
Swap ():  C * ( B + A )
          ↓
Postfix:  C B A + *
          ↓
Reverse:  * + A B C
          ↓
Prefix:   *+ABC
```

### Stack Trace for "(A+B)*C"

```
Input (reversed): C*(B+A)

Step | Char | Stack    | Output
-----|------|----------|--------
  1  |  C   |          | C
  2  |  *   | *        | C
  3  |  (   | *(       | C
  4  |  B   | *(       | CB
  5  |  +   | *(+      | CB
  6  |  A   | *(+      | CBA
  7  |  )   | *        | CBA+
  8  | END  |          | CBA+*

Reverse: *+ABC
```

---

## Edge Cases

1. **Empty string:** Return empty string
2. **Single operand:** Return same operand
3. **No operators:** Return same string
4. **Nested parentheses:** Handle multiple levels
5. **All operators same precedence:** Follow associativity
6. **Spaces in input:** Remove or handle
7. **Invalid characters:** Validate input
8. **Unbalanced parentheses:** Error handling

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Reverse + Postfix | O(n) | O(n) | **Most intuitive** |
| Direct Stack | O(n) | O(n) | Single pass, right-to-left |

**Time Complexity Breakdown:**
- Reverse string: O(n)
- Convert to postfix: O(n)
- Reverse result: O(n)
- Total: O(n) + O(n) + O(n) = O(n)

**Space Complexity:**
- Stack: O(n) worst case (all operators)
- Result string: O(n)
- Total: O(n)

---

## Related Problems

1. **Infix to Postfix** - Convert to postfix notation
2. **Postfix to Infix** - Reverse conversion
3. **Prefix to Infix** - Reverse conversion
4. **Evaluate Prefix Expression** - Calculate result
5. **Evaluate Postfix Expression** - Calculate result
6. **Expression Tree** - Build tree from expression

---

## Interview Tips

### Clarification Questions
1. What operators are supported? (+, -, *, /, ^)
2. Are parentheses included? (Yes)
3. Are operands single characters? (Usually yes)
4. Handle spaces in input? (Usually remove)
5. What about invalid expressions? (Assume valid)

### Approach Explanation
1. "I'll reverse the infix expression"
2. "Swap opening and closing parentheses"
3. "Convert to postfix using standard algorithm"
4. "Reverse the postfix to get prefix"
5. "Time O(n), Space O(n) for stack"

### Common Mistakes
1. **Forgetting to swap parentheses** - Must swap ( and )
2. **Wrong precedence comparison** - Use < for postfix, <= for prefix
3. **Not reversing final result** - Must reverse postfix
4. **Ignoring associativity** - Right-to-left for ^
5. **Stack underflow** - Check empty before pop

### Follow-up Questions
1. "Can you evaluate the prefix expression?" - Use stack, scan right-to-left
2. "What about postfix conversion?" - Don't reverse, scan left-to-right
3. "How to handle multi-digit numbers?" - Use delimiter or tokenize
4. "Space optimization?" - In-place not possible, need O(n) space

---

## Real-World Applications

1. **Compilers** - Expression parsing and optimization
2. **Calculators** - Scientific calculator implementations
3. **Database Query Optimization** - Query tree construction
4. **Spreadsheet Formulas** - Excel/Google Sheets
5. **Mathematical Software** - Mathematica, MATLAB
6. **Programming Languages** - Lisp uses prefix notation

---

## Key Takeaways

1. **Prefix = Polish Notation:** Operator before operands (+ A B)
2. **Reverse trick:** Reverse infix → convert to postfix → reverse result
3. **Parentheses swap:** Must swap ( and ) after reversing
4. **Precedence matters:** Higher precedence operators evaluated first
5. **Stack-based:** Use stack to track operators
6. **O(n) time:** Single pass through expression
7. **Evaluation:** Scan right-to-left, push operands, apply operators

---

## Comparison: Infix vs Prefix vs Postfix

```
Expression: A + B * C

Infix:    A + B * C
          (Human-readable, needs precedence rules)

Prefix:   + A * B C
          (Operator first, no parentheses needed)

Postfix:  A B C * +
          (Operator last, easy to evaluate)

With Parentheses: (A + B) * C

Infix:    (A + B) * C
Prefix:   * + A B C
Postfix:  A B + C *
```

---

## Optimization Notes

### Why Reverse Approach?
- **Reuses postfix algorithm:** Well-known and tested
- **Easy to understand:** Clear transformation steps
- **Minimal code:** Leverages existing postfix conversion

### Direct Approach Benefits
- **Single pass:** No multiple reversals
- **Slightly faster:** Fewer string operations
- **More complex:** Harder to understand and debug

### Best Practice
```java
// Use reverse approach for clarity
public static String infixToPrefix(String infix) {
    // 1. Reverse and swap parentheses
    String reversed = reverseAndSwap(infix);
    // 2. Convert to postfix
    String postfix = infixToPostfix(reversed);
    // 3. Reverse result
    return new StringBuilder(postfix).reverse().toString();
}
```
