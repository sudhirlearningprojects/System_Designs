# Robot Walk - Final Position

## Problem Statement

A robot starts at position (0, 0) and follows a path of movement commands. Calculate the robot's final position.

**Commands:**
- `U` - Move up (y + 1)
- `D` - Move down (y - 1)
- `L` - Move left (x - 1)
- `R` - Move right (x + 1)
- Other characters - Ignore

**Input:** String path  
**Output:** int[] {x, y} final position

**Examples:**
```
walk("UR")      → [1, 1]
walk("UDLR")    → [0, 0]
walk("UUU")     → [0, 3]
walk("UURDDL")  → [0, 0]
walk("UXYZR")   → [1, 1]  // Ignores X, Y, Z
```

---

## Solution Approach

### Optimal: Track Position Changes

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int[] walk(String path) {
    int x = 0, y = 0;
    
    for (char c : path.toCharArray()) {
        if (c == 'U') y++;
        else if (c == 'D') y--;
        else if (c == 'L') x--;
        else if (c == 'R') x++;
    }
    
    return new int[]{x, y};
}
```

---

## Algorithm Walkthrough

### Example: "UURDDL"

```
Initial position: (0, 0)

Step 1: 'U' → y++ → (0, 1)
Step 2: 'U' → y++ → (0, 2)
Step 3: 'R' → x++ → (1, 2)
Step 4: 'D' → y-- → (1, 1)
Step 5: 'D' → y-- → (1, 0)
Step 6: 'L' → x-- → (0, 0)

Final position: [0, 0]
```

### Example: "UXYZR" (with invalid characters)

```
Initial position: (0, 0)

Step 1: 'U' → y++ → (0, 1)
Step 2: 'X' → ignore → (0, 1)
Step 3: 'Y' → ignore → (0, 1)
Step 4: 'Z' → ignore → (0, 1)
Step 5: 'R' → x++ → (1, 1)

Final position: [1, 1]
```

---

## Complete Implementation

```java
public class RobotWalk {
    
    // Approach 1: Basic implementation (Recommended)
    public static int[] walk(String path) {
        int x = 0, y = 0;
        
        for (char c : path.toCharArray()) {
            if (c == 'U') y++;
            else if (c == 'D') y--;
            else if (c == 'L') x--;
            else if (c == 'R') x++;
        }
        
        return new int[]{x, y};
    }
    
    // Approach 2: Using switch statement
    public static int[] walkSwitch(String path) {
        int x = 0, y = 0;
        
        for (char c : path.toCharArray()) {
            switch (c) {
                case 'U': y++; break;
                case 'D': y--; break;
                case 'L': x--; break;
                case 'R': x++; break;
            }
        }
        
        return new int[]{x, y};
    }
    
    // Approach 3: With validation
    public static int[] walkValidated(String path) {
        if (path == null || path.isEmpty()) {
            return new int[]{0, 0};
        }
        
        int x = 0, y = 0;
        
        for (char c : path.toCharArray()) {
            switch (c) {
                case 'U': y++; break;
                case 'D': y--; break;
                case 'L': x--; break;
                case 'R': x--; break;
                default: // Ignore invalid characters
            }
        }
        
        return new int[]{x, y};
    }
    
    // Approach 4: Return as Point object
    static class Point {
        int x, y;
        
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
    
    public static Point walkAsPoint(String path) {
        int x = 0, y = 0;
        
        for (char c : path.toCharArray()) {
            if (c == 'U') y++;
            else if (c == 'D') y--;
            else if (c == 'L') x--;
            else if (c == 'R') x++;
        }
        
        return new Point(x, y);
    }
    
    // Bonus: Check if robot returns to origin
    public static boolean returnsToOrigin(String path) {
        int[] pos = walk(path);
        return pos[0] == 0 && pos[1] == 0;
    }
    
    // Bonus: Calculate Manhattan distance from origin
    public static int distanceFromOrigin(String path) {
        int[] pos = walk(path);
        return Math.abs(pos[0]) + Math.abs(pos[1]);
    }
    
    // Bonus: Get path trace (all positions visited)
    public static List<int[]> getPathTrace(String path) {
        List<int[]> trace = new ArrayList<>();
        int x = 0, y = 0;
        trace.add(new int[]{x, y});
        
        for (char c : path.toCharArray()) {
            if (c == 'U') y++;
            else if (c == 'D') y--;
            else if (c == 'L') x--;
            else if (c == 'R') x++;
            
            trace.add(new int[]{x, y});
        }
        
        return trace;
    }
    
    // Bonus: Check if path crosses itself
    public static boolean pathCrossesItself(String path) {
        Set<String> visited = new HashSet<>();
        int x = 0, y = 0;
        visited.add(x + "," + y);
        
        for (char c : path.toCharArray()) {
            if (c == 'U') y++;
            else if (c == 'D') y--;
            else if (c == 'L') x--;
            else if (c == 'R') x++;
            
            String pos = x + "," + y;
            if (visited.contains(pos)) {
                return true;
            }
            visited.add(pos);
        }
        
        return false;
    }
}
```

---

## Test Cases

```java
@Test
public void testRobotWalk() {
    // Basic movements
    assertArrayEquals(new int[]{1, 1}, walk("UR"));
    assertArrayEquals(new int[]{0, 0}, walk("UDLR"));
    assertArrayEquals(new int[]{0, 3}, walk("UUU"));
    
    // Return to origin
    assertArrayEquals(new int[]{0, 0}, walk("UURDDL"));
    
    // With invalid characters
    assertArrayEquals(new int[]{1, 1}, walk("UXYZR"));
    assertArrayEquals(new int[]{2, 0}, walk("RXR"));
    
    // Empty path
    assertArrayEquals(new int[]{0, 0}, walk(""));
    
    // Only invalid characters
    assertArrayEquals(new int[]{0, 0}, walk("XYZ"));
    
    // All directions
    assertArrayEquals(new int[]{1, 1}, walk("RRRUUULLD"));
    
    // Negative coordinates
    assertArrayEquals(new int[]{-2, -3}, walk("LLDDD"));
}

@Test
public void testBonusFunctions() {
    // Returns to origin
    assertTrue(returnsToOrigin("UDLR"));
    assertFalse(returnsToOrigin("UR"));
    
    // Manhattan distance
    assertEquals(2, distanceFromOrigin("UR"));
    assertEquals(0, distanceFromOrigin("UDLR"));
    assertEquals(5, distanceFromOrigin("UUURR"));
    
    // Path crosses itself
    assertTrue(pathCrossesItself("URDL"));
    assertFalse(pathCrossesItself("UURR"));
}
```

---

## Visual Representation

### Example: "UURDDL"

```
Grid (y-axis up, x-axis right):

  2  . (0,2) → (1,2)
     ↑         ↓
  1  (0,1)   (1,1)
     ↑         ↓
  0  (0,0) ← (1,0)
     
     0   1   x-axis

Path trace:
(0,0) → (0,1) → (0,2) → (1,2) → (1,1) → (1,0) → (0,0)
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `[0, 0]` | Empty path |
| `"U"` | `[0, 1]` | Single move |
| `"UDLR"` | `[0, 0]` | Return to origin |
| `"XYZ"` | `[0, 0]` | All invalid |
| `"UXYZR"` | `[1, 1]` | Mixed valid/invalid |
| `"LLLL"` | `[-4, 0]` | Negative coordinates |

---

## Common Mistakes

1. **Wrong Coordinate System:**
   ```java
   // WRONG - inconsistent directions
   if (c == 'U') x++;  // Should be y++
   
   // CORRECT
   if (c == 'U') y++;
   ```

2. **Not Ignoring Invalid Characters:**
   ```java
   // WRONG - throws error on invalid char
   switch (c) {
       case 'U': y++; break;
       default: throw new Exception();
   }
   
   // CORRECT - silently ignore
   switch (c) {
       case 'U': y++; break;
       // No default needed
   }
   ```

3. **Not Handling Empty String:**
   ```java
   if (path == null || path.isEmpty()) {
       return new int[]{0, 0};
   }
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Basic | O(n) | O(1) | Optimal |
| With trace | O(n) | O(n) | Stores all positions |
| Cross detection | O(n) | O(n) | Uses HashSet |

**Where n = path length**

---

## Coordinate System

```
Standard Cartesian:
     y
     ↑
     |
─────┼────→ x
     |
     
U: y + 1 (up)
D: y - 1 (down)
L: x - 1 (left)
R: x + 1 (right)
```

---

## Optimization: Count Moves

```java
// Alternative: Count net movements
public static int[] walkOptimized(String path) {
    int up = 0, down = 0, left = 0, right = 0;
    
    for (char c : path.toCharArray()) {
        if (c == 'U') up++;
        else if (c == 'D') down++;
        else if (c == 'L') left++;
        else if (c == 'R') right++;
    }
    
    return new int[]{right - left, up - down};
}
```

---

## Related Problems

- **LeetCode 657:** Robot Return to Origin
- **LeetCode 1041:** Robot Bounded In Circle
- **Grid traversal problems**
- **Path finding algorithms**

---

## Interview Tips

1. **Clarify Requirements:**
   - Coordinate system (which direction is positive)?
   - Handle invalid characters?
   - Return format (array, object, string)?

2. **Start Simple:**
   - Track x and y separately
   - Process each character

3. **Explain Coordinate System:**
   - Y increases upward
   - X increases rightward

4. **Walk Through Example:**
   - Use "UURDDL"
   - Show position after each step

5. **Discuss Extensions:**
   - Return to origin check
   - Manhattan distance
   - Path crossing detection

---

## Real-World Applications

- **Robotics:** Path planning and navigation
- **Game Development:** Character movement
- **Warehouse Automation:** Robot navigation
- **Drone Control:** Flight path execution
- **GPS Navigation:** Route tracking
- **Grid-based Games:** Chess, checkers movement

---

## Extensions

### 1. Diagonal Movements
```java
// Add diagonal: NE, NW, SE, SW
case 'Q': x--; y++; break;  // NW
case 'E': x++; y++; break;  // NE
case 'Z': x--; y--; break;  // SW
case 'C': x++; y--; break;  // SE
```

### 2. With Obstacles
```java
public static int[] walkWithObstacles(String path, Set<String> obstacles) {
    int x = 0, y = 0;
    
    for (char c : path.toCharArray()) {
        int newX = x, newY = y;
        
        if (c == 'U') newY++;
        else if (c == 'D') newY--;
        else if (c == 'L') newX--;
        else if (c == 'R') newX++;
        
        // Only move if not blocked
        if (!obstacles.contains(newX + "," + newY)) {
            x = newX;
            y = newY;
        }
    }
    
    return new int[]{x, y};
}
```

### 3. Bounded Grid
```java
public static int[] walkBounded(String path, int minX, int maxX, int minY, int maxY) {
    int x = 0, y = 0;
    
    for (char c : path.toCharArray()) {
        if (c == 'U' && y < maxY) y++;
        else if (c == 'D' && y > minY) y--;
        else if (c == 'L' && x > minX) x--;
        else if (c == 'R' && x < maxX) x++;
    }
    
    return new int[]{x, y};
}
```

---

## Key Takeaways

✅ Track x and y coordinates separately  
✅ Start at origin (0, 0)  
✅ U/D modify y, L/R modify x  
✅ Ignore invalid characters  
✅ O(n) time, O(1) space - optimal  
✅ Can extend with obstacles, bounds, diagonals  
✅ Useful for grid-based navigation problems
