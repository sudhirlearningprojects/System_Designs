# Find Most Frequent IP Address in Logs

## Problem Statement

Given a log file as an array of strings, return the IP address(es) that access the site most often.

**Input:** Array of log entries (strings)  
**Output:** IP address with highest frequency

**Example:**
```
Input: [
  "10.0.0.1 - log entry 1 11",
  "10.0.0.1 - log entry 2 213",
  "10.0.0.2 - log entry 133132"
]

Frequency:
  10.0.0.1: 2 times
  10.0.0.2: 1 time

Output: "10.0.0.1"
```

---

## Solution Approach

### Optimal: HashMap to Count Frequencies

**Time Complexity:** O(n)  
**Space Complexity:** O(k) where k = unique IPs

```java
public static String findTopIpaddress(String[] lines) {
    Map<String, Integer> ipCount = new HashMap<>();
    
    // Count occurrences of each IP
    for (String line : lines) {
        String ip = line.split(" ")[0];
        ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
    }
    
    // Find IP with max count
    String topIp = null;
    int maxCount = 0;
    
    for (Map.Entry<String, Integer> entry : ipCount.entrySet()) {
        if (entry.getValue() > maxCount) {
            maxCount = entry.getValue();
            topIp = entry.getKey();
        }
    }
    
    return topIp;
}
```

---

## Algorithm Walkthrough

### Example: Given Test Case

```
Input:
[
  "10.0.0.1 - log entry 1 11",
  "10.0.0.1 - log entry 2 213",
  "10.0.0.2 - log entry 133132"
]

Step 1: Count IPs
  Line 1: "10.0.0.1 - log entry 1 11"
    Extract IP: "10.0.0.1"
    ipCount = {"10.0.0.1": 1}
  
  Line 2: "10.0.0.1 - log entry 2 213"
    Extract IP: "10.0.0.1"
    ipCount = {"10.0.0.1": 2}
  
  Line 3: "10.0.0.2 - log entry 133132"
    Extract IP: "10.0.0.2"
    ipCount = {"10.0.0.1": 2, "10.0.0.2": 1}

Step 2: Find maximum
  "10.0.0.1": 2 (max)
  "10.0.0.2": 1

Result: "10.0.0.1"
```

---

## Complete Implementation

```java
public class Solution {
    
    // Approach 1: HashMap (Recommended)
    public static String findTopIpaddress(String[] lines) {
        if (lines == null || lines.length == 0) {
            return null;
        }
        
        Map<String, Integer> ipCount = new HashMap<>();
        
        // Count occurrences
        for (String line : lines) {
            String ip = line.split(" ")[0];
            ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
        }
        
        // Find max
        String topIp = null;
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : ipCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topIp = entry.getKey();
            }
        }
        
        return topIp;
    }
    
    // Approach 2: Return all IPs with max frequency (handle ties)
    public static List<String> findAllTopIpaddresses(String[] lines) {
        if (lines == null || lines.length == 0) {
            return new ArrayList<>();
        }
        
        Map<String, Integer> ipCount = new HashMap<>();
        
        for (String line : lines) {
            String ip = line.split(" ")[0];
            ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
        }
        
        int maxCount = Collections.max(ipCount.values());
        
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ipCount.entrySet()) {
            if (entry.getValue() == maxCount) {
                result.add(entry.getKey());
            }
        }
        
        return result;
    }
    
    // Approach 3: Stream API
    public static String findTopIpaddressStream(String[] lines) {
        if (lines == null || lines.length == 0) {
            return null;
        }
        
        return Arrays.stream(lines)
            .map(line -> line.split(" ")[0])
            .collect(Collectors.groupingBy(ip -> ip, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    // Approach 4: With IP validation
    public static String findTopIpaddressValidated(String[] lines) {
        if (lines == null || lines.length == 0) {
            return null;
        }
        
        Map<String, Integer> ipCount = new HashMap<>();
        
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length > 0 && isValidIP(parts[0])) {
                String ip = parts[0];
                ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
            }
        }
        
        return ipCount.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    private static boolean isValidIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
    
    // Bonus: Return top N IPs
    public static List<String> findTopNIps(String[] lines, int n) {
        Map<String, Integer> ipCount = new HashMap<>();
        
        for (String line : lines) {
            String ip = line.split(" ")[0];
            ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
        }
        
        return ipCount.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(n)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    // Bonus: Return IP with count
    public static Map<String, Integer> getTopIpWithCount(String[] lines) {
        Map<String, Integer> ipCount = new HashMap<>();
        
        for (String line : lines) {
            String ip = line.split(" ")[0];
            ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
        }
        
        Map.Entry<String, Integer> maxEntry = ipCount.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
        
        if (maxEntry == null) return new HashMap<>();
        
        Map<String, Integer> result = new HashMap<>();
        result.put(maxEntry.getKey(), maxEntry.getValue());
        return result;
    }
    
    public static boolean doTestsPass() {
        String[] lines = {
            "10.0.0.1 - log entry 1 11",
            "10.0.0.1 - log entry 2 213",
            "10.0.0.2 - log entry 133132"
        };
        
        return findTopIpaddress(lines).equals("10.0.0.1");
    }
    
    public static void main(String[] args) {
        String lines[] = {
            "10.0.0.1 - log entry 1 11",
            "10.0.0.1 - log entry 2 213",
            "10.0.0.2 - log entry 133132"
        };
        
        String result = findTopIpaddress(lines);
        
        if (result.equals("10.0.0.1")) {
            System.out.println("Test passed");
        } else {
            System.out.println("Test failed");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testFindTopIpaddress() {
    // Basic case
    String[] logs1 = {
        "10.0.0.1 - log entry 1",
        "10.0.0.1 - log entry 2",
        "10.0.0.2 - log entry 3"
    };
    assertEquals("10.0.0.1", findTopIpaddress(logs1));
    
    // All different IPs (return any)
    String[] logs2 = {
        "10.0.0.1 - log entry 1",
        "10.0.0.2 - log entry 2",
        "10.0.0.3 - log entry 3"
    };
    assertNotNull(findTopIpaddress(logs2));
    
    // Single log entry
    String[] logs3 = {"192.168.1.1 - log entry"};
    assertEquals("192.168.1.1", findTopIpaddress(logs3));
    
    // Multiple IPs with same max frequency
    String[] logs4 = {
        "10.0.0.1 - log 1",
        "10.0.0.1 - log 2",
        "10.0.0.2 - log 3",
        "10.0.0.2 - log 4"
    };
    String result = findTopIpaddress(logs4);
    assertTrue(result.equals("10.0.0.1") || result.equals("10.0.0.2"));
    
    // Empty array
    assertNull(findTopIpaddress(new String[]{}));
    
    // IPv6 format
    String[] logs5 = {
        "2001:0db8:85a3::8a2e:0370:7334 - log 1",
        "2001:0db8:85a3::8a2e:0370:7334 - log 2",
        "192.168.1.1 - log 3"
    };
    assertEquals("2001:0db8:85a3::8a2e:0370:7334", findTopIpaddress(logs5));
}
```

---

## Visual Representation

```
Log Entries:
┌─────────────────────────────────┐
│ 10.0.0.1 - log entry 1 11       │
│ 10.0.0.1 - log entry 2 213      │
│ 10.0.0.2 - log entry 133132     │
└─────────────────────────────────┘
         ↓ Extract IPs
┌──────────┬───────┐
│    IP    │ Count │
├──────────┼───────┤
│ 10.0.0.1 │   2   │ ← Maximum
│ 10.0.0.2 │   1   │
└──────────┴───────┘

Result: 10.0.0.1
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `[]` | `null` | Empty array |
| `["10.0.0.1 - log"]` | `"10.0.0.1"` | Single entry |
| All unique IPs | Any IP | All have count 1 |
| Tie (multiple max) | Any of them | Return first found |
| Invalid format | Handle gracefully | Skip or validate |

---

## Common Mistakes

1. **Not Handling Empty Input:**
   ```java
   // WRONG - NullPointerException
   for (String line : lines)
   
   // CORRECT
   if (lines == null || lines.length == 0) return null;
   ```

2. **Incorrect String Parsing:**
   ```java
   // WRONG - assumes specific format
   String ip = line.substring(0, 10);
   
   // CORRECT - split by space
   String ip = line.split(" ")[0];
   ```

3. **Not Handling Ties:**
   ```java
   // Consider returning List<String> for ties
   // Or specify behavior in requirements
   ```

4. **Array Index Out of Bounds:**
   ```java
   // WRONG - if line doesn't have space
   String ip = line.split(" ")[0];
   
   // CORRECT - check length
   String[] parts = line.split(" ");
   if (parts.length > 0) {
       String ip = parts[0];
   }
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap | O(n) | O(k) | k = unique IPs |
| Stream API | O(n) | O(k) | More concise |
| Sorting | O(n log n) | O(k) | Unnecessary |

**Where:**
- n = number of log entries
- k = number of unique IPs

---

## Optimization: Single Pass

```java
public static String findTopIpaddressSinglePass(String[] lines) {
    Map<String, Integer> ipCount = new HashMap<>();
    String topIp = null;
    int maxCount = 0;
    
    for (String line : lines) {
        String ip = line.split(" ")[0];
        int count = ipCount.getOrDefault(ip, 0) + 1;
        ipCount.put(ip, count);
        
        if (count > maxCount) {
            maxCount = count;
            topIp = ip;
        }
    }
    
    return topIp;
}
```

---

## Handling Ties

### Return All IPs with Max Frequency

```java
public static List<String> findAllTopIps(String[] lines) {
    Map<String, Integer> ipCount = new HashMap<>();
    
    for (String line : lines) {
        String ip = line.split(" ")[0];
        ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
    }
    
    int maxCount = Collections.max(ipCount.values());
    
    return ipCount.entrySet()
        .stream()
        .filter(e -> e.getValue() == maxCount)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

---

## Real-World Enhancements

### 1. Parse Apache/Nginx Log Format

```java
// Apache Common Log Format:
// 127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326

public static String extractIP(String logLine) {
    // IP is always first field
    return logLine.split(" ")[0];
}
```

### 2. Filter by Time Range

```java
public static String findTopIpInTimeRange(String[] lines, 
                                          LocalDateTime start, 
                                          LocalDateTime end) {
    Map<String, Integer> ipCount = new HashMap<>();
    
    for (String line : lines) {
        String[] parts = line.split(" ");
        String ip = parts[0];
        LocalDateTime timestamp = parseTimestamp(parts);
        
        if (timestamp.isAfter(start) && timestamp.isBefore(end)) {
            ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
        }
    }
    
    return findMaxIP(ipCount);
}
```

### 3. Detect DDoS Attacks

```java
public static List<String> detectSuspiciousIPs(String[] lines, int threshold) {
    Map<String, Integer> ipCount = new HashMap<>();
    
    for (String line : lines) {
        String ip = line.split(" ")[0];
        ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
    }
    
    return ipCount.entrySet()
        .stream()
        .filter(e -> e.getValue() > threshold)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

---

## Related Problems

- **LeetCode 811:** Subdomain Visit Count
- **Log file analysis**
- **Frequency counting**
- **Top K frequent elements**

---

## Interview Tips

1. **Clarify Requirements:**
   - Return one IP or all with max frequency?
   - Handle empty input?
   - Log format specification?
   - IPv4 only or IPv6 too?

2. **Start with HashMap:**
   - Count frequencies
   - Find maximum

3. **Discuss Optimization:**
   - Single pass vs two pass
   - Space complexity

4. **Walk Through Example:**
   - Show counting process
   - Explain max finding

5. **Handle Edge Cases:**
   - Empty logs
   - Single entry
   - Ties in frequency

---

## Real-World Applications

- **Web Analytics:** Track visitor IPs
- **Security:** Detect DDoS attacks
- **Load Balancing:** Identify heavy users
- **Rate Limiting:** IP-based throttling
- **Fraud Detection:** Suspicious activity patterns
- **Network Monitoring:** Traffic analysis

---

## Key Takeaways

✅ Use HashMap to count IP frequencies - O(n) time  
✅ Extract IP with `line.split(" ")[0]`  
✅ Find max by iterating through map entries  
✅ Handle empty input gracefully  
✅ Consider ties (multiple IPs with same max count)  
✅ Single pass optimization possible  
✅ O(n) time, O(k) space where k = unique IPs  
✅ Can extend to top N IPs, time ranges, validation
