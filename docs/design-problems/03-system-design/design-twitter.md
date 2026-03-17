# Design Twitter (LeetCode 355)

## Problem Statement

Design a simplified version of Twitter where users can post tweets, follow/unfollow another user, and see the 10 most recent tweets in the user's news feed.

Implement the `Twitter` class:
- `Twitter()` Initializes your twitter object.
- `void postTweet(int userId, int tweetId)` Composes a new tweet with ID tweetId by the user userId.
- `List<Integer> getNewsFeed(int userId)` Retrieves the 10 most recent tweet IDs in the user's news feed. Each item must be posted by users who the user followed or by the user themself. Tweets must be ordered from most recent to least recent.
- `void follow(int followerId, int followeeId)` The user with ID followerId started following the user with ID followeeId.
- `void unfollow(int followerId, int followeeId)` The user with ID followerId started unfollowing the user with ID followeeId.

**Constraints:**
- 1 <= userId, followerId, followeeId <= 500
- 0 <= tweetId <= 10^4
- All the tweets have unique IDs
- At most 3 * 10^4 calls will be made to postTweet, getNewsFeed, follow, and unfollow

## Approach 1: HashMap + PriorityQueue (K-way Merge)

### Intuition
- Each user maintains their own tweet list (sorted by time)
- News feed = merge K sorted lists (user + all followees)
- Use min-heap to efficiently get top 10 most recent tweets

### Implementation

```java
class Twitter {
    class Tweet {
        int id;
        int timestamp;
        Tweet next;
        
        Tweet(int id, int timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }
    
    private int timestamp = 0;
    private Map<Integer, Tweet> userTweets;      // userId -> head of tweet list
    private Map<Integer, Set<Integer>> following; // userId -> set of followees
    
    public Twitter() {
        userTweets = new HashMap<>();
        following = new HashMap<>();
    }
    
    public void postTweet(int userId, int tweetId) {
        Tweet tweet = new Tweet(tweetId, timestamp++);
        tweet.next = userTweets.get(userId);
        userTweets.put(userId, tweet);
    }
    
    public List<Integer> getNewsFeed(int userId) {
        List<Integer> feed = new ArrayList<>();
        
        // Max heap by timestamp
        PriorityQueue<Tweet> pq = new PriorityQueue<>((a, b) -> b.timestamp - a.timestamp);
        
        // Add user's own tweets
        if (userTweets.containsKey(userId)) {
            pq.offer(userTweets.get(userId));
        }
        
        // Add followees' tweets
        Set<Integer> followees = following.getOrDefault(userId, new HashSet<>());
        for (int followeeId : followees) {
            if (userTweets.containsKey(followeeId)) {
                pq.offer(userTweets.get(followeeId));
            }
        }
        
        // Get top 10 most recent
        while (!pq.isEmpty() && feed.size() < 10) {
            Tweet tweet = pq.poll();
            feed.add(tweet.id);
            if (tweet.next != null) {
                pq.offer(tweet.next);
            }
        }
        
        return feed;
    }
    
    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId) return; // Can't follow yourself
        following.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
    }
    
    public void unfollow(int followerId, int followeeId) {
        Set<Integer> followees = following.get(followerId);
        if (followees != null) {
            followees.remove(followeeId);
        }
    }
}
```

**Time Complexity**:
- postTweet: O(1)
- getNewsFeed: O(K log K + 10 log K) where K = number of followees
- follow/unfollow: O(1)

**Space Complexity**: O(U + T) where U = users, T = total tweets

### Pros
- Efficient K-way merge using heap
- Scalable for many followees
- Memory efficient (linked list for tweets)

### Cons
- Heap operations add overhead
- Complex implementation

---

## Approach 2: Simple List Merge (Brute Force)

### Intuition
- Collect all tweets from user and followees
- Sort by timestamp
- Return top 10

### Implementation

```java
class Twitter {
    class Tweet {
        int id;
        int timestamp;
        
        Tweet(int id, int timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }
    
    private int timestamp = 0;
    private Map<Integer, List<Tweet>> userTweets;
    private Map<Integer, Set<Integer>> following;
    
    public Twitter() {
        userTweets = new HashMap<>();
        following = new HashMap<>();
    }
    
    public void postTweet(int userId, int tweetId) {
        userTweets.computeIfAbsent(userId, k -> new ArrayList<>())
                  .add(new Tweet(tweetId, timestamp++));
    }
    
    public List<Integer> getNewsFeed(int userId) {
        List<Tweet> allTweets = new ArrayList<>();
        
        // Add user's tweets
        if (userTweets.containsKey(userId)) {
            allTweets.addAll(userTweets.get(userId));
        }
        
        // Add followees' tweets
        Set<Integer> followees = following.getOrDefault(userId, new HashSet<>());
        for (int followeeId : followees) {
            if (userTweets.containsKey(followeeId)) {
                allTweets.addAll(userTweets.get(followeeId));
            }
        }
        
        // Sort by timestamp (descending)
        allTweets.sort((a, b) -> b.timestamp - a.timestamp);
        
        // Return top 10
        List<Integer> feed = new ArrayList<>();
        for (int i = 0; i < Math.min(10, allTweets.size()); i++) {
            feed.add(allTweets.get(i).id);
        }
        
        return feed;
    }
    
    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId) return;
        following.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
    }
    
    public void unfollow(int followerId, int followeeId) {
        Set<Integer> followees = following.get(followerId);
        if (followees != null) {
            followees.remove(followeeId);
        }
    }
}
```

**Time Complexity**:
- postTweet: O(1)
- getNewsFeed: O(T log T) where T = total tweets from user + followees
- follow/unfollow: O(1)

**Space Complexity**: O(U + T)

### Pros
- Simple to implement
- Easy to understand
- Works well for small datasets

### Cons
- Inefficient for large number of tweets
- Sorts all tweets even though we only need 10
- Not scalable

---

## Approach 3: Pre-computed Feed (Push Model)

### Intuition
- When user posts tweet, push to all followers' feeds
- getNewsFeed just reads pre-computed feed
- Trade-off: Faster reads, slower writes

### Implementation

```java
class Twitter {
    class Tweet {
        int id;
        int timestamp;
        
        Tweet(int id, int timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }
    
    private int timestamp = 0;
    private Map<Integer, List<Tweet>> userFeeds;     // Pre-computed feeds
    private Map<Integer, Set<Integer>> followers;    // followeeId -> set of followers
    private Map<Integer, Set<Integer>> following;
    
    public Twitter() {
        userFeeds = new HashMap<>();
        followers = new HashMap<>();
        following = new HashMap<>();
    }
    
    public void postTweet(int userId, int tweetId) {
        Tweet tweet = new Tweet(tweetId, timestamp++);
        
        // Add to user's own feed
        addToFeed(userId, tweet);
        
        // Push to all followers' feeds
        Set<Integer> userFollowers = followers.getOrDefault(userId, new HashSet<>());
        for (int followerId : userFollowers) {
            addToFeed(followerId, tweet);
        }
    }
    
    private void addToFeed(int userId, Tweet tweet) {
        List<Tweet> feed = userFeeds.computeIfAbsent(userId, k -> new ArrayList<>());
        feed.add(0, tweet); // Add to front
        
        // Keep only 10 most recent
        if (feed.size() > 10) {
            feed.remove(feed.size() - 1);
        }
    }
    
    public List<Integer> getNewsFeed(int userId) {
        List<Tweet> feed = userFeeds.getOrDefault(userId, new ArrayList<>());
        return feed.stream().map(t -> t.id).collect(Collectors.toList());
    }
    
    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId) return;
        
        following.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
        followers.computeIfAbsent(followeeId, k -> new HashSet<>()).add(followerId);
        
        // Rebuild follower's feed (expensive!)
        rebuildFeed(followerId);
    }
    
    public void unfollow(int followerId, int followeeId) {
        Set<Integer> followees = following.get(followerId);
        if (followees != null) {
            followees.remove(followeeId);
        }
        
        Set<Integer> userFollowers = followers.get(followeeId);
        if (userFollowers != null) {
            userFollowers.remove(followerId);
        }
        
        // Rebuild follower's feed
        rebuildFeed(followerId);
    }
    
    private void rebuildFeed(int userId) {
        // Expensive operation - not shown for brevity
        // Would need to merge tweets from all followees
    }
}
```

**Time Complexity**:
- postTweet: O(F) where F = number of followers
- getNewsFeed: O(1)
- follow/unfollow: O(T) where T = tweets (need to rebuild feed)

**Space Complexity**: O(U * 10) for pre-computed feeds

### Pros
- Extremely fast reads (O(1))
- Good for read-heavy workloads

### Cons
- Slow writes (O(F) per tweet)
- Expensive follow/unfollow operations
- Not suitable for users with millions of followers (celebrities)

---

## Approach 4: Hybrid Push-Pull Model (Real Twitter)

### Intuition
- Regular users: Push model (pre-compute feeds)
- Celebrities: Pull model (compute on demand)
- Best of both worlds

### Implementation

```java
class Twitter {
    private static final int CELEBRITY_THRESHOLD = 1000; // followers
    
    class Tweet {
        int id;
        int timestamp;
        Tweet next;
        
        Tweet(int id, int timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }
    
    private int timestamp = 0;
    private Map<Integer, Tweet> userTweets;
    private Map<Integer, Set<Integer>> following;
    private Map<Integer, Set<Integer>> followers;
    private Map<Integer, List<Integer>> precomputedFeeds; // For non-celebrities
    
    public Twitter() {
        userTweets = new HashMap<>();
        following = new HashMap<>();
        followers = new HashMap<>();
        precomputedFeeds = new HashMap<>();
    }
    
    public void postTweet(int userId, int tweetId) {
        Tweet tweet = new Tweet(tweetId, timestamp++);
        tweet.next = userTweets.get(userId);
        userTweets.put(userId, tweet);
        
        // If user is not a celebrity, push to followers
        Set<Integer> userFollowers = followers.getOrDefault(userId, new HashSet<>());
        if (userFollowers.size() < CELEBRITY_THRESHOLD) {
            for (int followerId : userFollowers) {
                // Push to precomputed feed
                List<Integer> feed = precomputedFeeds.computeIfAbsent(followerId, k -> new ArrayList<>());
                feed.add(0, tweetId);
                if (feed.size() > 10) feed.remove(10);
            }
        }
    }
    
    public List<Integer> getNewsFeed(int userId) {
        Set<Integer> followees = following.getOrDefault(userId, new HashSet<>());
        
        // Check if any followee is a celebrity
        boolean hasCelebrity = false;
        for (int followeeId : followees) {
            if (followers.getOrDefault(followeeId, new HashSet<>()).size() >= CELEBRITY_THRESHOLD) {
                hasCelebrity = true;
                break;
            }
        }
        
        if (hasCelebrity) {
            // Pull model: compute on demand
            return getNewsFeedPull(userId);
        } else {
            // Push model: use precomputed feed
            return precomputedFeeds.getOrDefault(userId, new ArrayList<>());
        }
    }
    
    private List<Integer> getNewsFeedPull(int userId) {
        // Same as Approach 1
        List<Integer> feed = new ArrayList<>();
        PriorityQueue<Tweet> pq = new PriorityQueue<>((a, b) -> b.timestamp - a.timestamp);
        
        if (userTweets.containsKey(userId)) {
            pq.offer(userTweets.get(userId));
        }
        
        Set<Integer> followees = following.getOrDefault(userId, new HashSet<>());
        for (int followeeId : followees) {
            if (userTweets.containsKey(followeeId)) {
                pq.offer(userTweets.get(followeeId));
            }
        }
        
        while (!pq.isEmpty() && feed.size() < 10) {
            Tweet tweet = pq.poll();
            feed.add(tweet.id);
            if (tweet.next != null) {
                pq.offer(tweet.next);
            }
        }
        
        return feed;
    }
    
    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId) return;
        following.computeIfAbsent(followerId, k -> new HashSet<>()).add(followeeId);
        followers.computeIfAbsent(followeeId, k -> new HashSet<>()).add(followerId);
    }
    
    public void unfollow(int followerId, int followeeId) {
        Set<Integer> followees = following.get(followerId);
        if (followees != null) followees.remove(followeeId);
        
        Set<Integer> userFollowers = followers.get(followeeId);
        if (userFollowers != null) userFollowers.remove(followerId);
    }
}
```

**Time Complexity**:
- postTweet: O(1) for celebrities, O(F) for regular users
- getNewsFeed: O(1) for non-celebrity feeds, O(K log K) for celebrity feeds
- follow/unfollow: O(1)

**Space Complexity**: O(U + T)

### Pros
- Optimal for real-world scenarios
- Handles celebrities efficiently
- Scalable for millions of users

### Cons
- Most complex implementation
- Requires tuning threshold
- Hybrid logic adds complexity

---

## Comparison Table

| Approach | postTweet | getNewsFeed | follow/unfollow | Best For |
|----------|-----------|-------------|-----------------|----------|
| K-way Merge | O(1) | O(K log K) | O(1) | Balanced workload |
| Brute Force | O(1) | O(T log T) | O(1) | Small datasets |
| Push Model | O(F) | O(1) | O(T) | Read-heavy |
| Hybrid | O(1)/O(F) | O(1)/O(K log K) | O(1) | Production (Real Twitter) |

---

## Test Cases

```java
public class TwitterTest {
    public static void main(String[] args) {
        // Test Case 1: Basic operations
        Twitter twitter = new Twitter();
        twitter.postTweet(1, 5);
        List<Integer> feed = twitter.getNewsFeed(1);
        assert feed.equals(Arrays.asList(5));
        
        twitter.follow(1, 2);
        twitter.postTweet(2, 6);
        feed = twitter.getNewsFeed(1);
        assert feed.equals(Arrays.asList(6, 5));
        
        twitter.unfollow(1, 2);
        feed = twitter.getNewsFeed(1);
        assert feed.equals(Arrays.asList(5));
        
        // Test Case 2: Multiple tweets
        Twitter twitter2 = new Twitter();
        twitter2.postTweet(1, 1);
        twitter2.postTweet(1, 2);
        twitter2.postTweet(1, 3);
        feed = twitter2.getNewsFeed(1);
        assert feed.equals(Arrays.asList(3, 2, 1));
        
        // Test Case 3: Can't follow yourself
        Twitter twitter3 = new Twitter();
        twitter3.postTweet(1, 1);
        twitter3.follow(1, 1);
        feed = twitter3.getNewsFeed(1);
        assert feed.size() == 1; // Should not duplicate
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

1. **Q: How to scale to millions of users?**
   - Shard users by userId
   - Use distributed cache (Redis) for feeds
   - Async processing for feed generation
   - CDN for static content

2. **Q: How to handle trending topics?**
   - Use separate trending service
   - Count hashtag frequency in time windows
   - Use HyperLogLog for unique user counts
   - Cache trending topics

3. **Q: How to implement retweets?**
   ```java
   public void retweet(int userId, int originalTweetId, int originalUserId) {
       Tweet original = findTweet(originalUserId, originalTweetId);
       Tweet retweet = new Tweet(originalTweetId, timestamp++);
       retweet.isRetweet = true;
       retweet.originalUser = originalUserId;
       // Add to user's timeline
   }
   ```

4. **Q: How to implement likes and comments?**
   - Separate tables: Likes(tweetId, userId), Comments(tweetId, userId, text)
   - Denormalize like count in Tweet object
   - Use Redis sorted sets for top liked tweets

5. **Q: How to prevent spam and bots?**
   - Rate limiting per user
   - ML-based spam detection
   - CAPTCHA for suspicious activity
   - Shadow banning

---

## Real-world Optimizations

### 1. Feed Caching
```java
private LoadingCache<Integer, List<Integer>> feedCache = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(new CacheLoader<Integer, List<Integer>>() {
        public List<Integer> load(Integer userId) {
            return computeFeed(userId);
        }
    });
```

### 2. Async Feed Generation
```java
@Async
public CompletableFuture<Void> postTweetAsync(int userId, int tweetId) {
    postTweet(userId, tweetId);
    return CompletableFuture.completedFuture(null);
}
```

### 3. Database Sharding
```
User 1-1000: Shard 1
User 1001-2000: Shard 2
...
```

### 4. Message Queue for Fan-out
```java
// Producer
kafkaTemplate.send("tweet-fanout", new TweetEvent(userId, tweetId));

// Consumer
@KafkaListener(topics = "tweet-fanout")
public void handleTweetFanout(TweetEvent event) {
    // Push to followers' feeds
}
```

---

## Related Problems

- [Design Instagram (System Design)](../../03-system-design/instagram.md)
- [Design News Feed (System Design)](../../03-system-design/news-feed.md)
- [LRU Cache (LC 146)](./lru-cache.md)

---

## Real-world Applications

1. **Twitter**: 500M tweets/day, 330M MAU
2. **Facebook**: News feed algorithm
3. **Instagram**: Photo feed generation
4. **LinkedIn**: Professional network feed
5. **Reddit**: Subreddit post ranking
