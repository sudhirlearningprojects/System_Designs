# Friend Recommendations - Social Network Graph

## Problem Statement

**Real-World Context**: LinkedIn, Facebook, and Instagram use this algorithm to suggest "People You May Know" based on mutual connections.

Given a social network represented as an undirected graph where nodes are users and edges are friendships, recommend friends for a given user. Recommendations should be:
1. Not already friends with the user
2. Have mutual friends with the user
3. Ranked by number of mutual friends (descending)

**Input**: 
- Graph of friendships (adjacency list)
- User ID to get recommendations for

**Output**: List of recommended user IDs sorted by mutual friend count

**Examples:**
```
Social Network:
    1 --- 2 --- 4
    |     |     |
    3 --- 5 --- 6
    
Input: userId = 1
Friends of 1: [2, 3]
Friends of 2: [1, 4, 5]
Friends of 3: [1, 5]

Recommendations for User 1:
- User 5: 2 mutual friends (2, 3)
- User 4: 1 mutual friend (2)
- User 6: 0 mutual friends (skip)

Output: [5, 4]
```

---

## Solution Approach: BFS + Mutual Friends Count

**Algorithm:**
1. Get all direct friends of the user (1-hop neighbors)
2. For each friend, get their friends (2-hop neighbors)
3. Count mutual friends for each 2-hop neighbor
4. Filter out direct friends and the user itself
5. Sort by mutual friend count (descending)

**Time Complexity:** O(V + E) for BFS traversal  
**Space Complexity:** O(V) for storing recommendations

---

## Complete Implementation

```java
import java.util.*;

public class FriendRecommendation {
    
    static class SocialNetwork {
        private Map<Integer, Set<Integer>> graph;
        
        public SocialNetwork() {
            graph = new HashMap<>();
        }
        
        public void addFriendship(int user1, int user2) {
            graph.computeIfAbsent(user1, k -> new HashSet<>()).add(user2);
            graph.computeIfAbsent(user2, k -> new HashSet<>()).add(user1);
        }
        
        public Set<Integer> getFriends(int userId) {
            return graph.getOrDefault(userId, new HashSet<>());
        }
        
        // Main recommendation algorithm
        public List<Integer> recommendFriends(int userId, int topK) {
            Set<Integer> directFriends = getFriends(userId);
            Map<Integer, Integer> mutualFriendCount = new HashMap<>();
            
            // Traverse friends of friends (2-hop neighbors)
            for (int friend : directFriends) {
                Set<Integer> friendsOfFriend = getFriends(friend);
                
                for (int candidate : friendsOfFriend) {
                    // Skip if it's the user itself or already a friend
                    if (candidate == userId || directFriends.contains(candidate)) {
                        continue;
                    }
                    
                    mutualFriendCount.put(candidate, 
                        mutualFriendCount.getOrDefault(candidate, 0) + 1);
                }
            }
            
            // Sort by mutual friend count (descending)
            List<Map.Entry<Integer, Integer>> recommendations = 
                new ArrayList<>(mutualFriendCount.entrySet());
            
            recommendations.sort((a, b) -> {
                int cmp = b.getValue().compareTo(a.getValue());
                if (cmp == 0) {
                    return a.getKey().compareTo(b.getKey()); // Tie-breaker: smaller ID
                }
                return cmp;
            });
            
            // Return top K recommendations
            return recommendations.stream()
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        // Get mutual friends between two users
        public Set<Integer> getMutualFriends(int user1, int user2) {
            Set<Integer> friends1 = getFriends(user1);
            Set<Integer> friends2 = getFriends(user2);
            
            Set<Integer> mutual = new HashSet<>(friends1);
            mutual.retainAll(friends2);
            return mutual;
        }
        
        // Get recommendation with details
        public List<Recommendation> getDetailedRecommendations(int userId, int topK) {
            Set<Integer> directFriends = getFriends(userId);
            Map<Integer, Set<Integer>> mutualFriendsMap = new HashMap<>();
            
            for (int friend : directFriends) {
                Set<Integer> friendsOfFriend = getFriends(friend);
                
                for (int candidate : friendsOfFriend) {
                    if (candidate == userId || directFriends.contains(candidate)) {
                        continue;
                    }
                    
                    mutualFriendsMap.computeIfAbsent(candidate, k -> new HashSet<>())
                        .add(friend);
                }
            }
            
            List<Recommendation> recommendations = new ArrayList<>();
            for (Map.Entry<Integer, Set<Integer>> entry : mutualFriendsMap.entrySet()) {
                recommendations.add(new Recommendation(
                    entry.getKey(),
                    entry.getValue().size(),
                    new ArrayList<>(entry.getValue())
                ));
            }
            
            recommendations.sort((a, b) -> {
                int cmp = Integer.compare(b.mutualCount, a.mutualCount);
                return cmp != 0 ? cmp : Integer.compare(a.userId, b.userId);
            });
            
            return recommendations.stream().limit(topK).collect(Collectors.toList());
        }
    }
    
    static class Recommendation {
        int userId;
        int mutualCount;
        List<Integer> mutualFriends;
        
        public Recommendation(int userId, int mutualCount, List<Integer> mutualFriends) {
            this.userId = userId;
            this.mutualCount = mutualCount;
            this.mutualFriends = mutualFriends;
        }
        
        @Override
        public String toString() {
            return String.format("User %d (%d mutual friends: %s)", 
                userId, mutualCount, mutualFriends);
        }
    }
    
    // Test cases
    public static boolean doTestsPass() {
        SocialNetwork network = new SocialNetwork();
        
        // Build test network
        network.addFriendship(1, 2);
        network.addFriendship(1, 3);
        network.addFriendship(2, 4);
        network.addFriendship(2, 5);
        network.addFriendship(3, 5);
        network.addFriendship(4, 6);
        network.addFriendship(5, 6);
        
        // Test 1: User 1 recommendations
        List<Integer> recs1 = network.recommendFriends(1, 5);
        if (!recs1.equals(Arrays.asList(5, 4, 6))) return false;
        
        // Test 2: User 4 recommendations
        List<Integer> recs2 = network.recommendFriends(4, 5);
        if (!recs2.get(0).equals(5)) return false; // 5 has most mutual friends
        
        // Test 3: Mutual friends
        Set<Integer> mutual = network.getMutualFriends(1, 5);
        if (mutual.size() != 2) return false; // Should be {2, 3}
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("✓ All tests pass\n");
        } else {
            System.out.println("✗ Tests fail\n");
        }
        
        // Demo: LinkedIn-style friend recommendations
        SocialNetwork linkedin = new SocialNetwork();
        
        // Build network
        linkedin.addFriendship(1, 2);  // Alice - Bob
        linkedin.addFriendship(1, 3);  // Alice - Charlie
        linkedin.addFriendship(2, 4);  // Bob - David
        linkedin.addFriendship(2, 5);  // Bob - Eve
        linkedin.addFriendship(3, 5);  // Charlie - Eve
        linkedin.addFriendship(4, 6);  // David - Frank
        linkedin.addFriendship(5, 6);  // Eve - Frank
        linkedin.addFriendship(5, 7);  // Eve - Grace
        
        System.out.println("=== Friend Recommendations for Alice (User 1) ===\n");
        
        List<Recommendation> recommendations = linkedin.getDetailedRecommendations(1, 5);
        for (Recommendation rec : recommendations) {
            System.out.println(rec);
        }
        
        System.out.println("\n=== Network Statistics ===");
        System.out.println("Alice's friends: " + linkedin.getFriends(1));
        System.out.println("Mutual friends (Alice & Eve): " + linkedin.getMutualFriends(1, 5));
    }
}
```

---

## Algorithm Walkthrough

### Example Network:
```
    1 (Alice)
   / \
  2   3
 /|   |
4 5---6
  |
  7
```

### Step-by-Step for User 1 (Alice):

```
Step 1: Get direct friends of Alice
  directFriends = {2, 3}

Step 2: Traverse friends of friends
  Friends of 2 (Bob): {1, 4, 5}
    - Skip 1 (self)
    - Add 4: mutualCount[4] = 1
    - Add 5: mutualCount[5] = 1
  
  Friends of 3 (Charlie): {1, 5}
    - Skip 1 (self)
    - Add 5: mutualCount[5] = 2

Step 3: Current state
  mutualCount = {4: 1, 5: 2}

Step 4: Sort by mutual count
  Sorted: [(5, 2), (4, 1)]

Step 5: Return recommendations
  Result: [5, 4]
  
Explanation:
  - User 5 (Eve): 2 mutual friends (Bob, Charlie)
  - User 4 (David): 1 mutual friend (Bob)
```

---

## Complexity Analysis

**Time Complexity:**
- Building adjacency list: O(E)
- Traversing friends of friends: O(V + E)
- Sorting recommendations: O(R log R) where R = number of recommendations
- Overall: O(V + E + R log R)

**Space Complexity:**
- Graph storage: O(V + E)
- Mutual friend map: O(R)
- Overall: O(V + E)

---

## Optimizations for Production

### 1. Caching (Redis)
```java
// Cache recommendations for 24 hours
String cacheKey = "recommendations:" + userId;
List<Integer> cached = redis.get(cacheKey);
if (cached != null) return cached;

List<Integer> recommendations = computeRecommendations(userId);
redis.setex(cacheKey, 86400, recommendations);
return recommendations;
```

### 2. Batch Processing
```java
// Precompute recommendations for all users nightly
public void batchComputeRecommendations() {
    for (int userId : allUsers) {
        List<Integer> recs = recommendFriends(userId, 20);
        cache.put("recs:" + userId, recs);
    }
}
```

### 3. Scoring Algorithm (Advanced)
```java
double score = mutualFriendCount * 1.0
             + commonInterests * 0.5
             + sameLocation * 0.3
             + sameCompany * 0.4;
```

---

## Real-World Applications

1. **LinkedIn**: "People You May Know"
2. **Facebook**: Friend suggestions
3. **Instagram**: "Suggested for You"
4. **Twitter**: "Who to Follow"
5. **Dating Apps**: Match recommendations

---

## Extensions

1. **Weighted Edges**: Consider friendship strength
2. **Multi-hop**: Explore 3-hop or 4-hop neighbors
3. **Graph Embeddings**: Use Node2Vec for ML-based recommendations
4. **Temporal**: Consider recent interactions
5. **Diversity**: Avoid echo chambers by diversifying recommendations
