# Binary Search Tree - Implementation

## Problem Statement

Implement a Binary Search Tree (BST) with the following operations:
1. **put(int value)** - Insert a value into the BST
2. **contains(int value)** - Check if a value exists in the BST
3. **Fix inOrderTraversal()** - The method has a bug

**Expected Behavior:**
```
BST: put(3), put(1), put(2), put(5)

Tree Structure:
       3
      / \
     1   5
      \
       2

inOrderTraversal() → [1, 2, 3, 5] (sorted order)
contains(3) → true
contains(4) → false
```

---

## Bug in Original Code

### The Problem

```java
private void inOrderTraversal(Node node, List<Integer> acc) {
    if (node == null) return;
    
    inOrderTraversal(node.left, acc);
    inOrderTraversal(node.right, acc);  // WRONG ORDER
    acc.add(node.val);                   // WRONG POSITION
}
```

**Issue:** Visits right subtree before adding current node (post-order, not in-order)

### The Fix

```java
private void inOrderTraversal(Node node, List<Integer> acc) {
    if (node == null) return;
    
    inOrderTraversal(node.left, acc);   // 1. Left
    acc.add(node.val);                   // 2. Root
    inOrderTraversal(node.right, acc);  // 3. Right
}
```

---

## Complete Solution

```java
static class BST {
    private Node root;

    public BST() {
        this.root = null; // Fix: root should be null initially
    }

    public void put(int value) {
        root = putRecursive(root, value);
    }

    private Node putRecursive(Node node, int value) {
        if (node == null) {
            Node newNode = new Node();
            newNode.val = value;
            return newNode;
        }

        if (value < node.val) {
            node.left = putRecursive(node.left, value);
        } else if (value > node.val) {
            node.right = putRecursive(node.right, value);
        }
        // If value == node.val, don't insert duplicate

        return node;
    }

    public boolean contains(int value) {
        return containsRecursive(root, value);
    }

    private boolean containsRecursive(Node node, int value) {
        if (node == null) {
            return false;
        }

        if (value == node.val) {
            return true;
        } else if (value < node.val) {
            return containsRecursive(node.left, value);
        } else {
            return containsRecursive(node.right, value);
        }
    }

    public List<Integer> inOrderTraversal() {
        final ArrayList<Integer> acc = new ArrayList<>();
        inOrderTraversal(root, acc);
        return acc;
    }

    private void inOrderTraversal(Node node, List<Integer> acc) {
        if (node == null) {
            return;
        }
        inOrderTraversal(node.left, acc);  // Left
        acc.add(node.val);                  // Root
        inOrderTraversal(node.right, acc); // Right
    }

    private static class Node {
        Integer val;
        Node left;
        Node right;
    }
}
```

---

## Algorithm Walkthrough

### Insert Operations

**Sequence:** put(3), put(1), put(2), put(5)

```
Step 1: put(3)
       3

Step 2: put(1)
       3
      /
     1

Step 3: put(2)
       3
      /
     1
      \
       2

Step 4: put(5)
       3
      / \
     1   5
      \
       2
```

### Contains Operations

```
contains(3):
  Start at root(3) → 3 == 3 → true

contains(2):
  Start at root(3) → 2 < 3 → go left
  At node(1) → 2 > 1 → go right
  At node(2) → 2 == 2 → true

contains(4):
  Start at root(3) → 4 > 3 → go right
  At node(5) → 4 < 5 → go left
  null → false
```

### In-Order Traversal

```
Tree:    3
        / \
       1   5
        \
         2

Traversal Order:
1. Visit left subtree of 3
   - Visit left of 1 → null
   - Add 1
   - Visit right of 1
     - Visit left of 2 → null
     - Add 2
     - Visit right of 2 → null
2. Add 3
3. Visit right subtree of 3
   - Visit left of 5 → null
   - Add 5
   - Visit right of 5 → null

Result: [1, 2, 3, 5]
```

---

## Iterative Implementations (Alternative)

### Iterative Put

```java
public void put(int value) {
    if (root == null) {
        root = new Node();
        root.val = value;
        return;
    }

    Node current = root;
    while (true) {
        if (value < current.val) {
            if (current.left == null) {
                current.left = new Node();
                current.left.val = value;
                return;
            }
            current = current.left;
        } else if (value > current.val) {
            if (current.right == null) {
                current.right = new Node();
                current.right.val = value;
                return;
            }
            current = current.right;
        } else {
            return; // Duplicate
        }
    }
}
```

### Iterative Contains

```java
public boolean contains(int value) {
    Node current = root;
    
    while (current != null) {
        if (value == current.val) {
            return true;
        } else if (value < current.val) {
            current = current.left;
        } else {
            current = current.right;
        }
    }
    
    return false;
}
```

### Iterative In-Order Traversal

```java
public List<Integer> inOrderTraversal() {
    List<Integer> result = new ArrayList<>();
    Stack<Node> stack = new Stack<>();
    Node current = root;
    
    while (current != null || !stack.isEmpty()) {
        // Go to leftmost node
        while (current != null) {
            stack.push(current);
            current = current.left;
        }
        
        // Process node
        current = stack.pop();
        result.add(current.val);
        
        // Move to right subtree
        current = current.right;
    }
    
    return result;
}
```

---

## Test Cases

```java
@Test
public void testBST() {
    BST tree = new BST();
    
    // Test empty tree
    assertFalse(tree.contains(1));
    assertEquals(Arrays.asList(), tree.inOrderTraversal());
    
    // Test single node
    tree.put(5);
    assertTrue(tree.contains(5));
    assertEquals(Arrays.asList(5), tree.inOrderTraversal());
    
    // Test multiple nodes
    tree.put(3);
    tree.put(7);
    tree.put(1);
    tree.put(4);
    
    assertTrue(tree.contains(1));
    assertTrue(tree.contains(3));
    assertTrue(tree.contains(4));
    assertTrue(tree.contains(5));
    assertTrue(tree.contains(7));
    assertFalse(tree.contains(2));
    assertFalse(tree.contains(6));
    
    // Test in-order traversal (should be sorted)
    assertEquals(Arrays.asList(1, 3, 4, 5, 7), tree.inOrderTraversal());
    
    // Test duplicates (should not insert)
    tree.put(3);
    assertEquals(Arrays.asList(1, 3, 4, 5, 7), tree.inOrderTraversal());
}
```

---

## Tree Traversal Types

### In-Order (Left → Root → Right)

```java
void inOrder(Node node) {
    if (node == null) return;
    inOrder(node.left);
    visit(node);
    inOrder(node.right);
}
```

**Result:** Sorted order for BST  
**Use Case:** Get sorted elements

### Pre-Order (Root → Left → Right)

```java
void preOrder(Node node) {
    if (node == null) return;
    visit(node);
    preOrder(node.left);
    preOrder(node.right);
}
```

**Result:** Root first  
**Use Case:** Copy tree, prefix expression

### Post-Order (Left → Right → Root)

```java
void postOrder(Node node) {
    if (node == null) return;
    postOrder(node.left);
    postOrder(node.right);
    visit(node);
}
```

**Result:** Root last  
**Use Case:** Delete tree, postfix expression

---

## Complexity Analysis

| Operation | Average | Worst Case | Space |
|-----------|---------|------------|-------|
| put | O(log n) | O(n) | O(log n) recursion |
| contains | O(log n) | O(n) | O(log n) recursion |
| inOrderTraversal | O(n) | O(n) | O(n) |

**Worst Case:** Skewed tree (linked list)
```
1
 \
  2
   \
    3
     \
      4
```

---

## Common Mistakes

1. **Wrong Traversal Order:**
   ```java
   // WRONG - Post-order, not in-order
   inOrder(node.left);
   inOrder(node.right);
   acc.add(node.val);
   
   // CORRECT - In-order
   inOrder(node.left);
   acc.add(node.val);
   inOrder(node.right);
   ```

2. **Not Handling Null Root:**
   ```java
   // WRONG
   public BST() {
       this.root = new Node(); // Creates node with null value
   }
   
   // CORRECT
   public BST() {
       this.root = null;
   }
   ```

3. **Not Returning Updated Node:**
   ```java
   // WRONG
   private void putRecursive(Node node, int value) {
       if (node == null) {
           node = new Node(); // Doesn't update parent's reference
       }
   }
   
   // CORRECT
   private Node putRecursive(Node node, int value) {
       if (node == null) {
           return new Node(value);
       }
       return node;
   }
   ```

---

## Additional BST Operations

### Find Minimum

```java
public int findMin() {
    if (root == null) throw new IllegalStateException("Empty tree");
    
    Node current = root;
    while (current.left != null) {
        current = current.left;
    }
    return current.val;
}
```

### Find Maximum

```java
public int findMax() {
    if (root == null) throw new IllegalStateException("Empty tree");
    
    Node current = root;
    while (current.right != null) {
        current = current.right;
    }
    return current.val;
}
```

### Delete Node

```java
public void delete(int value) {
    root = deleteRecursive(root, value);
}

private Node deleteRecursive(Node node, int value) {
    if (node == null) return null;
    
    if (value < node.val) {
        node.left = deleteRecursive(node.left, value);
    } else if (value > node.val) {
        node.right = deleteRecursive(node.right, value);
    } else {
        // Node to delete found
        
        // Case 1: No children
        if (node.left == null && node.right == null) {
            return null;
        }
        
        // Case 2: One child
        if (node.left == null) return node.right;
        if (node.right == null) return node.left;
        
        // Case 3: Two children
        // Find min in right subtree (successor)
        Node successor = findMinNode(node.right);
        node.val = successor.val;
        node.right = deleteRecursive(node.right, successor.val);
    }
    
    return node;
}

private Node findMinNode(Node node) {
    while (node.left != null) {
        node = node.left;
    }
    return node;
}
```

### Height of Tree

```java
public int height() {
    return heightRecursive(root);
}

private int heightRecursive(Node node) {
    if (node == null) return -1;
    return 1 + Math.max(heightRecursive(node.left), 
                        heightRecursive(node.right));
}
```

---

## Related Problems

- **LeetCode 98:** Validate Binary Search Tree
- **LeetCode 700:** Search in a Binary Search Tree
- **LeetCode 701:** Insert into a Binary Search Tree
- **LeetCode 450:** Delete Node in a BST
- **LeetCode 94:** Binary Tree Inorder Traversal

---

## Interview Tips

1. **Clarify Requirements:**
   - Handle duplicates?
   - Balanced tree needed?
   - Recursive or iterative?

2. **Start with Structure:**
   - Explain BST property (left < root < right)
   - Draw tree diagram

3. **Implement Core Operations:**
   - put: Compare and recurse
   - contains: Binary search
   - Traversal: Left-Root-Right

4. **Discuss Complexity:**
   - Average O(log n) for balanced
   - Worst O(n) for skewed

5. **Mention Improvements:**
   - Self-balancing trees (AVL, Red-Black)
   - Iterative to save stack space

---

## BST Properties

1. **Left subtree** contains only nodes with values **less than** root
2. **Right subtree** contains only nodes with values **greater than** root
3. Both left and right subtrees are also BSTs
4. **In-order traversal** produces sorted sequence
5. **No duplicate** values (typically)

---

## Key Takeaways

✅ BST property: left < root < right  
✅ In-order traversal: Left → Root → Right (sorted)  
✅ put/contains: O(log n) average, O(n) worst  
✅ Recursive solutions are cleaner but use stack space  
✅ Always handle null nodes in base case  
✅ Root initialization should be null, not empty node
