# Design Twitter - Interview Document

## Problem Statement

Design a simplified version of Twitter where users can:
- Post tweets
- Follow/unfollow another user
- See the 10 most recent tweets in their news feed

### API Design

```java
class Twitter {
    Twitter()                                          // Initialize
    void postTweet(int userId, int tweetId)           // Post a tweet
    List<Integer> getNewsFeed(int userId)             // Get 10 most recent tweets
    void follow(int followerId, int followeeId)       // Follow a user
    void unfollow(int followerId, int followeeId)     // Unfollow a user
}
```

### Constraints
- 1 <= userId, followerId, followeeId <= 500
- 0 <= tweetId <= 10^4
- All tweets have unique IDs
- At most 3 × 10^4 calls total
- A user cannot follow himself

---

## Example Walkthrough

```
Twitter twitter = new Twitter();
twitter.postTweet(1, 5);    // User 1 posts tweet 5
twitter.getNewsFeed(1);     // → [5]
twitter.follow(1, 2);      // User 1 follows User 2
twitter.postTweet(2, 6);   // User 2 posts tweet 6
twitter.getNewsFeed(1);    // → [6, 5] (most recent first)
twitter.unfollow(1, 2);   // User 1 unfollows User 2
twitter.getNewsFeed(1);   // → [5] (no longer sees User 2's tweets)
```

---

## Approach: HashMap + Min-Heap (Merge K Sorted Lists)

### Key Insight
Each user's tweets are naturally ordered by time. Getting the news feed is essentially **merging K sorted lists** (one per followed user) and taking the top 10.

### Data Structures
1. **HashMap<Integer, Set<Integer>>** — followees for each user
2. **HashMap<Integer, List<Tweet>>** — tweets for each user (ordered by time)
3. **Global timestamp counter** — to order tweets chronologically
4. **Min-Heap (PriorityQueue)** — to efficiently merge and get top 10

### Time Complexity
| Operation | Complexity |
|-----------|-----------|
| postTweet | O(1) |
| follow | O(1) |
| unfollow | O(1) |
| getNewsFeed | O(N log K) where N=10, K=number of followees |

---

## Solution

```java
class Twitter {
    private int timestamp = 0;
    private Map<Integer, Set<Integer>> followMap;    // userId -> set of followeeIds
    private Map<Integer, List<int[]>> tweetMap;     // userId -> list of [time, tweetId]

    public Twitter() {
        followMap = new HashMap<>();
        tweetMap = new HashMap<>();
    }

    public void postTweet(int userId, int tweetId) {
        tweetMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(new int[]{timestamp++, tweetId});
    }

    public List<Integer> getNewsFeed(int userId) {
        // Max-heap: [timestamp, tweetId, userId, index in that user's tweet list]
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);

        // Add user's own latest tweet
        addLatestTweet(pq, userId);

        // Add each followee's latest tweet
        Set<Integer> followees = followMap.getOrDefault(userId, Collections.emptySet());
        for (int followeeId : followees) {
            addLatestTweet(pq, followeeId);
        }

        // Extract top 10
        List<Integer> feed = new ArrayList<>();
        while (!pq.isEmpty() && feed.size() < 10) {
            int[] top = pq.poll();
            feed.add(top[1]); // tweetId

            int uid = top[2];
            int idx = top[3] - 1; // previous tweet
            if (idx >= 0) {
                List<int[]> tweets = tweetMap.get(uid);
                pq.offer(new int[]{tweets.get(idx)[0], tweets.get(idx)[1], uid, idx});
            }
        }
        return feed;
    }

    private void addLatestTweet(PriorityQueue<int[]> pq, int userId) {
        List<int[]> tweets = tweetMap.get(userId);
        if (tweets != null && !tweets.isEmpty()) {
            int idx = tweets.size() - 1;
            pq.offer(new int[]{tweets.get(idx)[0], tweets.get(idx)[1], userId, idx});
        }
    }

    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId) return;
        followMap.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
    }

    public void unfollow(int followerId, int followeeId) {
        Set<Integer> followees = followMap.get(followerId);
        if (followees != null) followees.remove(followeeId);
    }
}
```

---

## How getNewsFeed Works (Visual)

```
User 1 follows: [2, 3]

User 1 tweets: [t=0, id=5] → [t=3, id=8]
User 2 tweets: [t=1, id=6] → [t=4, id=9]
User 3 tweets: [t=2, id=7]

Step 1: Add latest tweet from each user to max-heap
  Heap: [(t=4, id=9, user=2), (t=3, id=8, user=1), (t=2, id=7, user=3)]

Step 2: Poll max → (t=4, id=9, user=2), add user 2's previous tweet
  Feed: [9]
  Heap: [(t=3, id=8, user=1), (t=2, id=7, user=3), (t=1, id=6, user=2)]

Step 3: Poll max → (t=3, id=8, user=1), add user 1's previous tweet
  Feed: [9, 8]
  Heap: [(t=2, id=7, user=3), (t=1, id=6, user=2), (t=0, id=5, user=1)]

Step 4: Continue until feed has 10 items or heap is empty...
  Feed: [9, 8, 7, 6, 5]
```

---

## Why This Approach?

### Alternative 1: Brute Force — Collect All Tweets, Sort
```java
// Collect all tweets from user + followees, sort by time, take top 10
// Time: O(N log N) where N = total tweets from all followees
// Problem: Very slow if users have many tweets
```

### Alternative 2: Pre-compute Feed (Fan-out on Write)
```java
// On every postTweet, push to all followers' feeds
// Time: O(F) per post where F = number of followers
// Problem: Celebrity problem — millions of followers
```

### Why Merge K Sorted Lists Wins
- Only processes at most 10 tweets total (early termination)
- Each user's tweets are already sorted by time
- Heap ensures we always pick the most recent across all users
- No wasted work sorting tweets we don't need

---

## Edge Cases

1. **User has no tweets and no followees** → return empty list
2. **User follows themselves** → ignore (constraint says cannot)
3. **Unfollow someone not followed** → no-op (safe with Set.remove)
4. **User with < 10 total tweets in feed** → return all available
5. **Multiple follows/unfollows of same user** → Set handles idempotency

---

## Simpler Solution (For Quick Interviews)

If the interviewer is okay with a simpler but less optimal approach:

```java
class Twitter {
    private int time = 0;
    private Map<Integer, Set<Integer>> follows = new HashMap<>();
    private Map<Integer, List<int[]>> tweets = new HashMap<>();

    public Twitter() {}

    public void postTweet(int userId, int tweetId) {
        tweets.computeIfAbsent(userId, k -> new ArrayList<>()).add(new int[]{time++, tweetId});
    }

    public List<Integer> getNewsFeed(int userId) {
        List<int[]> candidates = new ArrayList<>();

        // Collect recent tweets from self + followees
        addRecent(candidates, userId);
        for (int fid : follows.getOrDefault(userId, Collections.emptySet())) {
            addRecent(candidates, fid);
        }

        // Sort by time descending, take top 10
        candidates.sort((a, b) -> b[0] - a[0]);
        List<Integer> feed = new ArrayList<>();
        for (int i = 0; i < Math.min(10, candidates.size()); i++) {
            feed.add(candidates.get(i)[1]);
        }
        return feed;
    }

    private void addRecent(List<int[]> candidates, int userId) {
        List<int[]> userTweets = tweets.get(userId);
        if (userTweets == null) return;
        int start = Math.max(0, userTweets.size() - 10);
        for (int i = start; i < userTweets.size(); i++) {
            candidates.add(userTweets.get(i));
        }
    }

    public void follow(int followerId, int followeeId) {
        if (followerId != followeeId)
            follows.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
    }

    public void unfollow(int followerId, int followeeId) {
        follows.getOrDefault(followerId, Collections.emptySet()).remove(followeeId);
    }
}
```

**Trade-off**: O(K × 10 × log(K×10)) for getNewsFeed vs O(10 × log K) for heap approach. Both pass LeetCode constraints.

---

## Interview Discussion Points

### 1. Scale to Production Twitter
| Aspect | In-Memory Solution | Production |
|--------|-------------------|-----------|
| Storage | HashMap | Distributed DB (Cassandra) |
| Feed | Compute on read | Hybrid: fan-out on write + pull for celebrities |
| Ordering | Global counter | Snowflake ID (timestamp-based) |
| Caching | N/A | Redis for hot feeds |

### 2. Fan-out on Write vs Fan-out on Read

```
Fan-out on Write (Push Model):
- When user posts → push to all followers' feed caches
- Fast reads, slow writes for celebrities
- Used for: Regular users (< 1000 followers)

Fan-out on Read (Pull Model):
- When user requests feed → pull from all followees
- Slow reads, fast writes
- Used for: Celebrities (millions of followers)

Twitter's Hybrid:
- Regular users: fan-out on write
- Celebrities (> 10K followers): fan-out on read
- Merge at read time
```

### 3. Why Max-Heap and Not Min-Heap?
We want the **most recent** tweets first. Max-heap gives us the largest timestamp (most recent) at the top. We could use a min-heap of size 10 if we were collecting all tweets first, but the merge-K-sorted-lists approach needs max-heap for early termination.

### 4. Follow-up: What if we need real-time updates?
- WebSocket connections for live feed updates
- Pub/Sub system (Kafka) for tweet distribution
- Server-Sent Events for new tweet notifications

---

## Complexity Summary

| Operation | Time | Space |
|-----------|------|-------|
| postTweet | O(1) | O(1) per tweet |
| follow | O(1) | O(1) |
| unfollow | O(1) | O(1) |
| getNewsFeed | O(10 × log K) | O(K) for heap |
| **Total Space** | | O(Users + Tweets + Follows) |

Where K = number of users in feed (self + followees).
