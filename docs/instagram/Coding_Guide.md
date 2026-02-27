# Instagram Clone - Complete Coding Guide

## System Design Overview

**Problem**: Social media platform with posts, feed, likes, comments

**Core Features**:
1. Create posts with images
2. Follow/unfollow users
3. Generate personalized feed
4. Like and comment on posts

## SOLID Principles

- **SRP**: Post, User, Feed separate responsibilities
- **OCP**: Add new feed algorithms without modifying existing
- **Strategy**: Different feed generation strategies (chronological, ranked)

## Design Patterns

1. **Strategy Pattern**: Feed generation algorithms
2. **Observer Pattern**: Notify followers of new posts
3. **Factory Pattern**: Create different post types

## Complete Implementation

```java
import java.util.*;
import java.time.LocalDateTime;

class User {
    String id, username;
    Set<String> following = new HashSet<>();
    Set<String> followers = new HashSet<>();
    
    User(String id, String username) {
        this.id = id;
        this.username = username;
    }
}

class Post {
    String id, userId, caption, imageUrl;
    LocalDateTime createdAt;
    Set<String> likes = new HashSet<>();
    List<Comment> comments = new ArrayList<>();
    
    Post(String userId, String caption, String imageUrl) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.userId = userId;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }
}

class Comment {
    String userId, text;
    LocalDateTime createdAt;
    
    Comment(String userId, String text) {
        this.userId = userId;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}

interface FeedStrategy {
    List<Post> generateFeed(User user, Map<String, List<Post>> allPosts);
}

class ChronologicalFeed implements FeedStrategy {
    public List<Post> generateFeed(User user, Map<String, List<Post>> allPosts) {
        List<Post> feed = new ArrayList<>();
        for (String followingId : user.following) {
            feed.addAll(allPosts.getOrDefault(followingId, new ArrayList<>()));
        }
        feed.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        return feed;
    }
}

class RankedFeed implements FeedStrategy {
    public List<Post> generateFeed(User user, Map<String, List<Post>> allPosts) {
        List<Post> feed = new ArrayList<>();
        for (String followingId : user.following) {
            feed.addAll(allPosts.getOrDefault(followingId, new ArrayList<>()));
        }
        feed.sort((a, b) -> {
            int scoreA = a.likes.size() * 2 + a.comments.size();
            int scoreB = b.likes.size() * 2 + b.comments.size();
            return scoreB - scoreA;
        });
        return feed;
    }
}

class InstagramService {
    private Map<String, User> users = new HashMap<>();
    private Map<String, List<Post>> userPosts = new HashMap<>();
    private Map<String, Post> allPosts = new HashMap<>();
    private FeedStrategy feedStrategy;
    
    InstagramService(FeedStrategy strategy) {
        this.feedStrategy = strategy;
    }
    
    public User createUser(String username) {
        User user = new User(UUID.randomUUID().toString().substring(0, 8), username);
        users.put(user.id, user);
        System.out.println("Created user: " + username + " (ID: " + user.id + ")");
        return user;
    }
    
    public void follow(String followerId, String followingId) {
        User follower = users.get(followerId);
        User following = users.get(followingId);
        
        follower.following.add(followingId);
        following.followers.add(followerId);
        System.out.println(follower.username + " followed " + following.username);
    }
    
    public Post createPost(String userId, String caption, String imageUrl) {
        Post post = new Post(userId, caption, imageUrl);
        userPosts.computeIfAbsent(userId, k -> new ArrayList<>()).add(post);
        allPosts.put(post.id, post);
        
        User user = users.get(userId);
        System.out.println(user.username + " posted: " + caption);
        return post;
    }
    
    public void likePost(String postId, String userId) {
        Post post = allPosts.get(postId);
        post.likes.add(userId);
        System.out.println(users.get(userId).username + " liked post " + postId);
    }
    
    public void commentOnPost(String postId, String userId, String text) {
        Post post = allPosts.get(postId);
        post.comments.add(new Comment(userId, text));
        System.out.println(users.get(userId).username + " commented: " + text);
    }
    
    public List<Post> getFeed(String userId) {
        User user = users.get(userId);
        return feedStrategy.generateFeed(user, userPosts);
    }
    
    public void printFeed(String userId) {
        User user = users.get(userId);
        List<Post> feed = getFeed(userId);
        
        System.out.println("\n=== Feed for " + user.username + " ===");
        for (Post post : feed.subList(0, Math.min(5, feed.size()))) {
            User author = users.get(post.userId);
            System.out.println("\n@" + author.username + ": " + post.caption);
            System.out.println("  Likes: " + post.likes.size() + " | Comments: " + post.comments.size());
        }
    }
}

public class InstagramDemo {
    public static void main(String[] args) {
        System.out.println("=== Instagram Clone ===\n");
        
        InstagramService instagram = new InstagramService(new RankedFeed());
        
        // Create users
        User alice = instagram.createUser("alice");
        User bob = instagram.createUser("bob");
        User charlie = instagram.createUser("charlie");
        
        // Follow relationships
        System.out.println();
        instagram.follow(alice.id, bob.id);
        instagram.follow(alice.id, charlie.id);
        
        // Create posts
        System.out.println();
        Post p1 = instagram.createPost(bob.id, "Beautiful sunset!", "sunset.jpg");
        Post p2 = instagram.createPost(charlie.id, "Coffee time ☕", "coffee.jpg");
        Post p3 = instagram.createPost(bob.id, "Hiking adventure", "hiking.jpg");
        
        // Interactions
        System.out.println();
        instagram.likePost(p1.id, alice.id);
        instagram.likePost(p1.id, charlie.id);
        instagram.commentOnPost(p1.id, alice.id, "Amazing!");
        instagram.likePost(p2.id, alice.id);
        
        // View feed
        instagram.printFeed(alice.id);
    }
}
```

## Key Concepts

**Feed Generation**:
- Pull model: Generate on request
- Push model: Pre-compute and cache
- Hybrid: Push for active users, pull for inactive

**Scalability**:
- Cassandra for posts (time-series data)
- Redis for feed cache
- CDN for images

## Interview Questions

**Q: How to generate feed for 1M followers?**
A: Fan-out on write for regular users, fan-out on read for celebrities

**Q: Store billions of posts?**
A: Cassandra with partition by userId + timestamp

**Q: Handle hot users (celebrities)?**
A: Separate queue, cache their posts, fan-out on read

Run: https://www.jdoodle.com/online-java-compiler
