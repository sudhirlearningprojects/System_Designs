# Instagram Clone - Architecture Diagrams

## Understanding System Architecture

### What is System Architecture?
System architecture is the conceptual model that defines the structure, behavior, and views of a system. For Instagram, we need to handle billions of users, massive amounts of media, and real-time interactions. This requires careful design of how components communicate, store data, and scale.

### Key Architectural Principles
1. **Microservices**: Break the system into small, independent services
2. **Scalability**: Design for horizontal scaling (add more servers)
3. **Reliability**: Ensure the system works even when parts fail
4. **Performance**: Optimize for speed and low latency
5. **Security**: Protect user data and prevent unauthorized access

## 1. Overall System Architecture

### Theory: Microservices Architecture
Instead of building one large application (monolith), we split Instagram into multiple small services. Each service has a specific responsibility:
- **User Service**: Handles user accounts and profiles
- **Post Service**: Manages posts, likes, and comments
- **Feed Service**: Generates personalized timelines
- **Media Service**: Processes and stores photos/videos
- **Notification Service**: Sends real-time notifications

### Why Microservices?
- **Independent Scaling**: Scale only the services that need it
- **Technology Diversity**: Use different databases for different needs
- **Team Independence**: Different teams can work on different services
- **Fault Isolation**: If one service fails, others continue working

```mermaid
graph TB
    subgraph "Client Applications"
        iOS[iOS App]
        Android[Android App]
        Web[Web Application]
        API[Third-party APIs]
    end
    
    subgraph "Load Balancer & API Gateway"
        LB[Load Balancer]
        Gateway[API Gateway]
        Auth[Auth Service]
    end
    
    subgraph "Microservices Layer"
        UserSvc[User Service]
        PostSvc[Post Service]
        FeedSvc[Feed Service]
        MediaSvc[Media Service]
        NotifSvc[Notification Service]
        SearchSvc[Search Service]
        MsgSvc[Messaging Service]
        StorySvc[Story Service]
    end
    
    subgraph "Data Storage Layer"
        UserDB[(PostgreSQL<br/>User Data)]
        PostDB[(Cassandra<br/>Posts & Comments)]
        GraphDB[(Neo4j<br/>Social Graph)]
        MessageDB[(MongoDB<br/>Messages)]
        SearchDB[(Elasticsearch<br/>Search Index)]
    end
    
    subgraph "Caching Layer"
        Redis[(Redis Cluster<br/>Cache & Sessions)]
        FeedCache[(Redis<br/>Feed Cache)]
    end
    
    subgraph "Storage & CDN"
        S3[(Amazon S3<br/>Media Storage)]
        CDN[CloudFront CDN]
    end
    
    subgraph "Message Queue"
        Kafka[Apache Kafka]
    end
    
    iOS --> LB
    Android --> LB
    Web --> LB
    API --> LB
    
    LB --> Gateway
    Gateway --> Auth
    
    Gateway --> UserSvc
    Gateway --> PostSvc
    Gateway --> FeedSvc
    Gateway --> MediaSvc
    Gateway --> NotifSvc
    Gateway --> SearchSvc
    Gateway --> MsgSvc
    Gateway --> StorySvc
    
    UserSvc --> UserDB
    UserSvc --> GraphDB
    UserSvc --> Redis
    
    PostSvc --> PostDB
    PostSvc --> Kafka
    PostSvc --> Redis
    
    FeedSvc --> FeedCache
    FeedSvc --> PostDB
    FeedSvc --> GraphDB
    
    MediaSvc --> S3
    MediaSvc --> CDN
    MediaSvc --> Kafka
    
    NotifSvc --> Kafka
    NotifSvc --> Redis
    
    SearchSvc --> SearchDB
    
    MsgSvc --> MessageDB
    MsgSvc --> Redis
    
    StorySvc --> Redis
    StorySvc --> S3
```

### Database Selection Strategy
- **PostgreSQL**: For user data (ACID properties needed for consistency)
- **Cassandra**: For posts (handles massive write loads, eventual consistency OK)
- **Redis**: For caching (in-memory, super fast reads)
- **Neo4j**: For social graph (optimized for relationship queries)
- **MongoDB**: For messages (flexible schema, good for chat data)
- **Elasticsearch**: For search (full-text search capabilities)

### Load Balancer & API Gateway
- **Load Balancer**: Distributes incoming requests across multiple servers
- **API Gateway**: Single entry point, handles authentication, rate limiting
- **Why needed**: Prevents any single server from being overwhelmed

## 2. Feed Generation Architecture

### Theory: The Feed Generation Problem
Generating a personalized feed is Instagram's core challenge. When a user opens the app, we need to show them the most relevant posts from people they follow. This seems simple but becomes complex at scale:

### The Challenge
- User follows 1000 people
- Each person posts 5 times per day
- That's 5000 potential posts to consider
- Multiply by 100M daily active users
- We need to generate 500 billion personalized feeds per day!

### Solution Approaches

#### 1. Pull Model (Lazy Loading)
- **How it works**: When user requests feed, fetch posts from all followed users
- **Pros**: Always fresh data, no storage overhead
- **Cons**: Slow (must query many users), expensive at scale
- **Best for**: Users who follow many people, infrequent users

#### 2. Push Model (Pre-computation)
- **How it works**: When someone posts, immediately add to all followers' feeds
- **Pros**: Fast feed generation (pre-computed)
- **Cons**: Storage overhead, celebrity problem (1M+ followers)
- **Best for**: Regular users with moderate follower counts

#### 3. Hybrid Model (Best of Both)
- **How it works**: Push for regular users, pull for celebrities
- **Why**: Celebrities have millions of followers, pushing to all would be expensive
- **Implementation**: If user has >1M followers, use pull model for their posts

```mermaid
graph TB
    subgraph "Feed Generation System"
        FeedAPI[Feed API]
        FeedService[Feed Service]
        
        subgraph "Feed Generation Strategies"
            PullModel[Pull Model<br/>On-demand]
            PushModel[Push Model<br/>Pre-computed]
            HybridModel[Hybrid Model<br/>Celebrity handling]
        end
        
        subgraph "Ranking Algorithm"
            MLModel[ML Ranking Model]
            EngagementScore[Engagement Scoring]
            Personalization[Personalization Engine]
        end
    end
    
    subgraph "Data Sources"
        FollowingGraph[(Following Graph<br/>Neo4j)]
        PostsDB[(Posts Database<br/>Cassandra)]
        UserActivity[(User Activity<br/>Redis)]
        FeedCache[(Pre-computed Feeds<br/>Redis)]
    end
    
    subgraph "Real-time Processing"
        KafkaStream[Kafka Streams]
        PostProcessor[Post Processor]
        FeedUpdater[Feed Updater]
    end
    
    FeedAPI --> FeedService
    
    FeedService --> PullModel
    FeedService --> PushModel
    FeedService --> HybridModel
    
    PullModel --> FollowingGraph
    PullModel --> PostsDB
    PullModel --> MLModel
    
    PushModel --> FeedCache
    PushModel --> EngagementScore
    
    HybridModel --> PullModel
    HybridModel --> PushModel
    
    MLModel --> Personalization
    EngagementScore --> MLModel
    
    KafkaStream --> PostProcessor
    PostProcessor --> FeedUpdater
    FeedUpdater --> FeedCache
    
    PostsDB --> KafkaStream
    UserActivity --> Personalization
```

### Machine Learning in Feed Ranking
- **Engagement Scoring**: Predict how likely user is to interact with a post
- **Factors**: User's past behavior, post type, time of day, relationship strength
- **Personalization**: Each user gets a unique ranking based on their preferences

### Real-time Processing with Kafka Streams
- **Stream Processing**: Handle events as they happen (likes, comments, posts)
- **Why Kafka**: High throughput, fault-tolerant, maintains event order
- **Use Cases**: Update feed caches, trigger notifications, analytics

## 3. Media Processing Pipeline

### Theory: Handling Media at Scale
Instagram processes billions of photos and videos. Each upload needs multiple operations:

### The Media Challenge
- **Original Quality**: Store user's original upload (legal/backup)
- **Multiple Formats**: Generate different sizes for different devices
- **Compression**: Reduce file size while maintaining quality
- **Global Distribution**: Serve media from locations close to users

### Async Processing Benefits
- **User Experience**: User sees "uploaded" immediately, processing happens in background
- **Scalability**: Can process media on separate servers
- **Reliability**: If processing fails, can retry without affecting user

### Processing Steps Explained
1. **Upload**: User uploads original file to temporary storage
2. **Queue**: Add processing job to Kafka queue
3. **Validation**: Check file type, size, content (no malware)
4. **Processing**: Generate thumbnails, compress, create different formats
5. **Storage**: Move processed files to permanent storage (S3)
6. **CDN**: Distribute to global edge locations for fast access

### Why Multiple Workers?
- **Parallel Processing**: Handle multiple uploads simultaneously
- **Fault Tolerance**: If one worker fails, others continue
- **Scalability**: Add more workers during peak times

```mermaid
graph TB
    subgraph "Media Upload Flow"
        Client[Client App]
        MediaAPI[Media Upload API]
        TempStorage[Temporary Storage]
        
        subgraph "Processing Pipeline"
            Validator[Media Validator]
            Processor[Media Processor]
            Transcoder[Video Transcoder]
            Thumbnail[Thumbnail Generator]
            Compressor[Image Compressor]
        end
        
        subgraph "Storage & CDN"
            S3Original[S3 Original]
            S3Processed[S3 Processed]
            CDN[CloudFront CDN]
        end
        
        subgraph "Async Processing"
            Queue[Kafka Queue]
            Worker1[Processing Worker 1]
            Worker2[Processing Worker 2]
            WorkerN[Processing Worker N]
        end
    end
    
    Client --> MediaAPI
    MediaAPI --> TempStorage
    MediaAPI --> Queue
    
    Queue --> Worker1
    Queue --> Worker2
    Queue --> WorkerN
    
    Worker1 --> Validator
    Worker2 --> Validator
    WorkerN --> Validator
    
    Validator --> Processor
    
    Processor --> Transcoder
    Processor --> Thumbnail
    Processor --> Compressor
    
    TempStorage --> S3Original
    Transcoder --> S3Processed
    Thumbnail --> S3Processed
    Compressor --> S3Processed
    
    S3Processed --> CDN
```

### Content Delivery Network (CDN)
- **Purpose**: Serve media files from servers close to users
- **How it works**: Copy files to edge servers worldwide
- **Benefits**: Faster loading, reduced server load, better user experience
- **Example**: User in Tokyo gets images from Tokyo server, not US server

## 4. Real-time Notification System

### Theory: Real-time Communication
Users expect instant notifications when someone likes their post or sends a message. This requires real-time communication between server and client.

### The Notification Challenge
- **Scale**: 100M users online simultaneously
- **Types**: Likes, comments, follows, messages, story views
- **Delivery**: Push notifications, in-app notifications, email, SMS
- **Personalization**: User preferences (what notifications they want)

### WebSocket vs HTTP
- **HTTP**: Request-response model, client must ask for updates
- **WebSocket**: Persistent connection, server can push updates anytime
- **Why WebSocket**: Real-time updates without constant polling

### Notification Processing Flow
1. **Event Occurs**: User likes a post
2. **Event Published**: Like event sent to Kafka
3. **Processing**: Notification service processes the event
4. **Filtering**: Check user preferences (does recipient want like notifications?)
5. **Aggregation**: Combine similar notifications ("John and 5 others liked your post")
6. **Delivery**: Send via WebSocket, push notification, etc.

### Why Kafka for Notifications?
- **Reliability**: Ensures no notifications are lost
- **Ordering**: Notifications processed in correct order
- **Scalability**: Can handle millions of events per second
- **Decoupling**: Notification service independent of other services

```mermaid
graph TB
    subgraph "Notification System"
        NotifAPI[Notification API]
        NotifService[Notification Service]
        
        subgraph "Notification Types"
            Like[Like Notification]
            Comment[Comment Notification]
            Follow[Follow Notification]
            Message[Message Notification]
            Story[Story Notification]
        end
        
        subgraph "Delivery Channels"
            WebSocket[WebSocket]
            PushNotif[Push Notification]
            Email[Email Service]
            SMS[SMS Service]
        end
        
        subgraph "Processing"
            Queue[Kafka Queue]
            Processor[Notification Processor]
            Filter[Notification Filter]
            Aggregator[Notification Aggregator]
        end
    end
    
    subgraph "Storage"
        NotifDB[(Notification DB<br/>MongoDB)]
        UserPrefs[(User Preferences<br/>Redis)]
        DeviceTokens[(Device Tokens<br/>Redis)]
    end
    
    NotifAPI --> NotifService
    
    NotifService --> Like
    NotifService --> Comment
    NotifService --> Follow
    NotifService --> Message
    NotifService --> Story
    
    Like --> Queue
    Comment --> Queue
    Follow --> Queue
    Message --> Queue
    Story --> Queue
    
    Queue --> Processor
    Processor --> Filter
    Filter --> Aggregator
    
    Filter --> UserPrefs
    Aggregator --> NotifDB
    
    Aggregator --> WebSocket
    Aggregator --> PushNotif
    Aggregator --> Email
    Aggregator --> SMS
    
    PushNotif --> DeviceTokens
```

### Device Token Management
- **Purpose**: Send push notifications to mobile devices
- **How it works**: Each app installation gets unique token
- **Storage**: Store tokens in Redis for fast lookup
- **Updates**: Tokens can change, need to keep them updated

## 5. Search Architecture

### Theory: Search at Scale
Users want to find other users, posts, and hashtags quickly. Traditional database queries become too slow at Instagram's scale.

### The Search Problem
- **Volume**: Billions of posts, millions of users
- **Speed**: Results must appear instantly (<150ms)
- **Relevance**: Most relevant results first
- **Types**: Users, posts, hashtags, locations

### Why Elasticsearch?
- **Full-text Search**: Can search inside post captions
- **Fast**: Optimized for search queries
- **Scalable**: Can distribute across multiple servers
- **Flexible**: Supports complex queries and filters

### Search Types Explained

#### User Search
- **Index**: Username, full name, bio
- **Ranking**: Follower count, verification status, mutual connections
- **Features**: Autocomplete, typo tolerance

#### Post Search
- **Index**: Caption text, hashtags, location
- **Ranking**: Engagement (likes, comments), recency, user relationship
- **Filters**: Date range, location, user

#### Hashtag Search
- **Index**: Hashtag name, post count
- **Ranking**: Popularity, trending status
- **Features**: Related hashtags, trending indicators

### Search Optimization Techniques
- **Caching**: Cache popular search results in Redis
- **Autocomplete**: Suggest searches as user types
- **Personalization**: Boost results from followed users
- **Indexing Strategy**: Update index in real-time vs batch updates

```mermaid
graph TB
    subgraph "Search System"
        SearchAPI[Search API]
        SearchService[Search Service]
        
        subgraph "Search Types"
            UserSearch[User Search]
            PostSearch[Post Search]
            HashtagSearch[Hashtag Search]
            LocationSearch[Location Search]
        end
        
        subgraph "Search Processing"
            QueryProcessor[Query Processor]
            Ranker[Search Ranker]
            Autocomplete[Autocomplete]
            Suggestions[Search Suggestions]
        end
    end
    
    subgraph "Search Infrastructure"
        Elasticsearch[(Elasticsearch<br/>Search Index)]
        Redis[(Redis<br/>Search Cache)]
        
        subgraph "Data Indexing"
            Indexer[Data Indexer]
            UserIndexer[User Indexer]
            PostIndexer[Post Indexer]
            HashtagIndexer[Hashtag Indexer]
        end
    end
    
    subgraph "Data Sources"
        UserDB[(User Database)]
        PostDB[(Post Database)]
        ActivityDB[(Activity Database)]
    end
    
    SearchAPI --> SearchService
    
    SearchService --> UserSearch
    SearchService --> PostSearch
    SearchService --> HashtagSearch
    SearchService --> LocationSearch
    
    UserSearch --> QueryProcessor
    PostSearch --> QueryProcessor
    HashtagSearch --> QueryProcessor
    LocationSearch --> QueryProcessor
    
    QueryProcessor --> Ranker
    QueryProcessor --> Autocomplete
    QueryProcessor --> Suggestions
    
    Ranker --> Elasticsearch
    Autocomplete --> Redis
    Suggestions --> Redis
    
    UserDB --> UserIndexer
    PostDB --> PostIndexer
    ActivityDB --> HashtagIndexer
    
    UserIndexer --> Indexer
    PostIndexer --> Indexer
    HashtagIndexer --> Indexer
    
    Indexer --> Elasticsearch
```

### Data Indexing Strategy
- **Real-time**: Index new posts immediately for fresh results
- **Batch**: Rebuild entire index periodically for consistency
- **Incremental**: Update only changed documents
- **Sharding**: Distribute index across multiple Elasticsearch nodes

## 6. Messaging System Architecture

### Theory: Direct Messaging at Scale
Instagram's direct messaging allows private conversations between users. This requires different considerations than public posts.

### Messaging Challenges
- **Privacy**: Messages must be secure and private
- **Real-time**: Messages should appear instantly
- **Reliability**: Messages must not be lost
- **Scale**: Billions of messages per day
- **Features**: Read receipts, online status, message history

### Message Storage Strategy
- **MongoDB**: Flexible schema for different message types
- **Document Structure**: Each conversation is a document
- **Sharding**: Distribute conversations across multiple servers
- **Indexing**: Fast queries by conversation ID and timestamp

### Real-time Message Delivery
1. **Send**: User A sends message to User B
2. **Storage**: Message stored in MongoDB
3. **Routing**: Message router finds User B's connection
4. **Delivery**: Message sent via WebSocket if online
5. **Acknowledgment**: Confirm delivery to sender
6. **Push Notification**: If User B offline, send push notification

### Message Types
- **Text**: Simple text messages
- **Media**: Photos, videos, voice messages
- **System**: "User joined group", "Message deleted"
- **Reactions**: Emoji reactions to messages

### Encryption & Security
- **In Transit**: TLS encryption for network communication
- **At Rest**: Encrypt messages in database
- **End-to-End**: Optional E2E encryption for sensitive conversations

```mermaid
graph TB
    subgraph "Messaging System"
        MsgAPI[Messaging API]
        MsgService[Message Service]
        
        subgraph "Message Types"
            TextMsg[Text Message]
            ImageMsg[Image Message]
            VideoMsg[Video Message]
            VoiceMsg[Voice Message]
        end
        
        subgraph "Real-time Communication"
            WebSocketGateway[WebSocket Gateway]
            ConnectionManager[Connection Manager]
            MessageRouter[Message Router]
        end
    end
    
    subgraph "Message Processing"
        Queue[Message Queue<br/>Kafka]
        Processor[Message Processor]
        Encryptor[Message Encryptor]
        Validator[Message Validator]
    end
    
    subgraph "Storage"
        MessageDB[(Messages<br/>MongoDB)]
        ConversationDB[(Conversations<br/>MongoDB)]
        MediaStorage[(Media Storage<br/>S3)]
    end
    
    subgraph "Delivery & Status"
        DeliveryTracker[Delivery Tracker]
        ReadReceipts[Read Receipts]
        OnlineStatus[Online Status<br/>Redis]
    end
    
    MsgAPI --> MsgService
    
    MsgService --> TextMsg
    MsgService --> ImageMsg
    MsgService --> VideoMsg
    MsgService --> VoiceMsg
    
    TextMsg --> Queue
    ImageMsg --> Queue
    VideoMsg --> Queue
    VoiceMsg --> Queue
    
    Queue --> Processor
    Processor --> Validator
    Validator --> Encryptor
    
    Encryptor --> MessageDB
    Encryptor --> ConversationDB
    
    ImageMsg --> MediaStorage
    VideoMsg --> MediaStorage
    VoiceMsg --> MediaStorage
    
    Processor --> MessageRouter
    MessageRouter --> WebSocketGateway
    WebSocketGateway --> ConnectionManager
    
    ConnectionManager --> OnlineStatus
    MessageRouter --> DeliveryTracker
    DeliveryTracker --> ReadReceipts
```

### Connection Management
- **WebSocket Gateway**: Manages millions of concurrent connections
- **Connection Pooling**: Efficiently handle connections per server
- **Load Balancing**: Distribute connections across multiple servers
- **Heartbeat**: Keep connections alive, detect disconnections

## 7. Story System Architecture

### Theory: Ephemeral Content (Stories)
Stories are temporary posts that disappear after 24 hours. This creates unique technical challenges.

### Story Characteristics
- **Temporary**: Auto-delete after 24 hours
- **Sequential**: Stories from same user shown in order
- **View Tracking**: Who viewed each story
- **Privacy**: Respect user's privacy settings

### Technical Challenges
- **Storage**: Temporary but needs to be fast
- **Expiry**: Automatic cleanup of old stories
- **Views**: Track who viewed without impacting performance
- **Privacy**: Hide stories from blocked users

### Storage Strategy
- **Active Stories**: Store in Redis for fast access
- **Media Files**: Store in S3 with expiry policy
- **View Data**: Store in Cassandra for analytics
- **Archive**: Optional permanent storage for user

### Story Lifecycle
1. **Creation**: User creates story, media processed
2. **Distribution**: Story added to followers' story feeds
3. **Viewing**: Track views, update view count
4. **Expiry**: Automatic deletion after 24 hours
5. **Cleanup**: Remove from all caches and storage

### Expiry Management
- **TTL (Time To Live)**: Redis automatically deletes expired stories
- **Scheduled Jobs**: Background job cleans up media files
- **Lazy Deletion**: Remove expired stories when accessed

### Privacy Filtering
- **Blocked Users**: Don't show stories to blocked users
- **Private Accounts**: Only followers see stories
- **Close Friends**: Special story category for selected followers

```mermaid
graph TB
    subgraph "Story System"
        StoryAPI[Story API]
        StoryService[Story Service]
        
        subgraph "Story Features"
            CreateStory[Create Story]
            ViewStory[View Story]
            StoryViews[Story Views]
            StoryExpiry[Story Expiry]
        end
    end
    
    subgraph "Story Processing"
        MediaProcessor[Media Processor]
        ExpiryScheduler[Expiry Scheduler]
        ViewTracker[View Tracker]
        PrivacyFilter[Privacy Filter]
    end
    
    subgraph "Storage"
        StoryCache[(Active Stories<br/>Redis)]
        StoryArchive[(Expired Stories<br/>S3)]
        ViewsDB[(Story Views<br/>Cassandra)]
        MediaStorage[(Story Media<br/>S3)]
    end
    
    subgraph "Real-time Updates"
        StoryQueue[Story Queue<br/>Kafka]
        ViewUpdater[View Updater]
        NotificationSender[Notification Sender]
    end
    
    StoryAPI --> StoryService
    
    StoryService --> CreateStory
    StoryService --> ViewStory
    StoryService --> StoryViews
    StoryService --> StoryExpiry
    
    CreateStory --> MediaProcessor
    MediaProcessor --> MediaStorage
    MediaProcessor --> StoryCache
    
    ViewStory --> PrivacyFilter
    PrivacyFilter --> StoryCache
    PrivacyFilter --> ViewTracker
    
    ViewTracker --> ViewsDB
    ViewTracker --> StoryQueue
    
    StoryQueue --> ViewUpdater
    ViewUpdater --> NotificationSender
    
    StoryExpiry --> ExpiryScheduler
    ExpiryScheduler --> StoryArchive
    ExpiryScheduler --> StoryCache
```

### View Analytics
- **Real-time Updates**: Update view counts immediately
- **Batch Processing**: Aggregate view data for analytics
- **Privacy**: Viewers list only visible to story creator

## 8. Security Architecture

### Theory: Security in Social Media
Security is critical for social media platforms handling personal data and private communications.

### Security Layers (Defense in Depth)
1. **Network Security**: Firewalls, DDoS protection
2. **Application Security**: Input validation, authentication
3. **Data Security**: Encryption, access controls
4. **Infrastructure Security**: Secure servers, monitoring

### Authentication vs Authorization
- **Authentication**: "Who are you?" (login with username/password)
- **Authorization**: "What can you do?" (can you see this private post?)

### JWT (JSON Web Tokens)
- **Stateless**: Server doesn't need to store session data
- **Scalable**: Works across multiple servers
- **Secure**: Cryptographically signed
- **Contains**: User ID, permissions, expiry time

### Security Threats & Mitigations

#### SQL Injection
- **Threat**: Malicious SQL in user input
- **Mitigation**: Parameterized queries, input validation

#### Cross-Site Scripting (XSS)
- **Threat**: Malicious JavaScript in user content
- **Mitigation**: Input sanitization, Content Security Policy

#### DDoS Attacks
- **Threat**: Overwhelming server with requests
- **Mitigation**: Rate limiting, CDN, load balancers

#### Data Breaches
- **Threat**: Unauthorized access to user data
- **Mitigation**: Encryption, access controls, monitoring

### Content Moderation
- **AI Filtering**: Automatically detect inappropriate content
- **Human Review**: Complex cases reviewed by humans
- **User Reports**: Users can report problematic content
- **Appeals Process**: Users can appeal moderation decisions

```mermaid
graph TB
    subgraph "Security Layer"
        WAF[Web Application Firewall]
        RateLimit[Rate Limiter]
        AuthGateway[Authentication Gateway]
        
        subgraph "Authentication"
            JWT[JWT Service]
            OAuth[OAuth2 Service]
            MFA[Multi-Factor Auth]
            SessionMgr[Session Manager]
        end
        
        subgraph "Authorization"
            RBAC[Role-Based Access Control]
            PermissionEngine[Permission Engine]
            PrivacyEngine[Privacy Engine]
        end
    end
    
    subgraph "Data Security"
        Encryption[Data Encryption]
        KeyMgmt[Key Management<br/>AWS KMS]
        DataMasking[Data Masking]
        AuditLog[Audit Logging]
    end
    
    subgraph "Content Security"
        ContentModerator[Content Moderator]
        AIFilter[AI Content Filter]
        ReportSystem[Report System]
        BlockSystem[Block System]
    end
    
    subgraph "Infrastructure Security"
        VPC[Virtual Private Cloud]
        SecurityGroups[Security Groups]
        NetworkACL[Network ACLs]
        Monitoring[Security Monitoring]
    end
    
    WAF --> RateLimit
    RateLimit --> AuthGateway
    
    AuthGateway --> JWT
    AuthGateway --> OAuth
    AuthGateway --> MFA
    AuthGateway --> SessionMgr
    
    JWT --> RBAC
    OAuth --> RBAC
    RBAC --> PermissionEngine
    PermissionEngine --> PrivacyEngine
    
    PrivacyEngine --> Encryption
    Encryption --> KeyMgmt
    Encryption --> DataMasking
    DataMasking --> AuditLog
    
    ContentModerator --> AIFilter
    AIFilter --> ReportSystem
    ReportSystem --> BlockSystem
    
    VPC --> SecurityGroups
    SecurityGroups --> NetworkACL
    NetworkACL --> Monitoring
```

### Compliance & Privacy
- **GDPR**: European data protection regulation
- **CCPA**: California privacy law
- **Data Retention**: How long to keep user data
- **Right to Deletion**: Users can request data deletion

## 9. Monitoring & Observability

### Theory: Observability in Distributed Systems
With multiple microservices, we need visibility into system health and performance.

### Three Pillars of Observability
1. **Metrics**: Numerical data (CPU usage, request count)
2. **Logs**: Text records of events (error messages, user actions)
3. **Traces**: Request journey across multiple services

### Why Monitoring Matters
- **Early Problem Detection**: Find issues before users notice
- **Performance Optimization**: Identify bottlenecks
- **Capacity Planning**: Know when to add more servers
- **Debugging**: Understand what went wrong

### Key Metrics to Monitor

#### Business Metrics
- **Daily Active Users (DAU)**: How many users use the app daily
- **Engagement Rate**: Likes, comments, shares per user
- **Content Creation Rate**: Posts, stories created per day
- **Revenue Metrics**: Ad clicks, purchases

#### Technical Metrics
- **Latency**: How long requests take (P50, P95, P99)
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **Resource Utilization**: CPU, memory, disk usage

#### Infrastructure Metrics
- **Server Health**: CPU, memory, disk, network
- **Database Performance**: Query time, connection pool
- **Cache Hit Rate**: Percentage of requests served from cache
- **Queue Length**: Backlog in message queues

### Alerting Strategy
- **SLA-based**: Alert when SLA targets are missed
- **Threshold-based**: Alert when metrics exceed limits
- **Anomaly Detection**: Alert on unusual patterns
- **Escalation**: Route alerts to appropriate teams

```mermaid
graph TB
    subgraph "Application Monitoring"
        APM[Application Performance Monitoring]
        Metrics[Metrics Collection<br/>Micrometer]
        Tracing[Distributed Tracing<br/>Jaeger]
        Logging[Centralized Logging<br/>ELK Stack]
    end
    
    subgraph "Infrastructure Monitoring"
        InfraMetrics[Infrastructure Metrics<br/>Prometheus]
        NodeExporter[Node Exporter]
        ContainerMetrics[Container Metrics<br/>cAdvisor]
        NetworkMonitoring[Network Monitoring]
    end
    
    subgraph "Business Monitoring"
        BusinessMetrics[Business Metrics]
        UserAnalytics[User Analytics]
        EngagementMetrics[Engagement Metrics]
        RevenueMetrics[Revenue Metrics]
    end
    
    subgraph "Alerting & Visualization"
        Grafana[Grafana Dashboards]
        AlertManager[Alert Manager]
        PagerDuty[PagerDuty Integration]
        Slack[Slack Notifications]
    end
    
    subgraph "Data Pipeline"
        DataCollector[Data Collector]
        StreamProcessor[Stream Processor<br/>Kafka Streams]
        DataWarehouse[Data Warehouse<br/>Snowflake]
        Analytics[Analytics Engine]
    end
    
    APM --> Metrics
    Metrics --> InfraMetrics
    Tracing --> Logging
    
    InfraMetrics --> NodeExporter
    InfraMetrics --> ContainerMetrics
    InfraMetrics --> NetworkMonitoring
    
    BusinessMetrics --> UserAnalytics
    UserAnalytics --> EngagementMetrics
    EngagementMetrics --> RevenueMetrics
    
    Metrics --> Grafana
    InfraMetrics --> Grafana
    BusinessMetrics --> Grafana
    
    Grafana --> AlertManager
    AlertManager --> PagerDuty
    AlertManager --> Slack
    
    Metrics --> DataCollector
    BusinessMetrics --> DataCollector
    DataCollector --> StreamProcessor
    StreamProcessor --> DataWarehouse
    DataWarehouse --> Analytics
```

### Data Pipeline for Analytics
- **Real-time**: Process events as they happen (Kafka Streams)
- **Batch**: Process large datasets periodically (Spark)
- **Data Warehouse**: Store processed data for analysis (Snowflake)
- **Machine Learning**: Train models on user behavior data

## 10. Deployment Architecture

### Theory: Cloud Deployment at Scale
Deploying Instagram requires global infrastructure to serve users worldwide with low latency.

### Multi-Region Deployment
- **Purpose**: Serve users from nearby servers
- **Benefits**: Lower latency, disaster recovery, compliance
- **Challenges**: Data consistency, increased complexity

### Availability Zones (AZ)
- **Definition**: Isolated data centers within a region
- **Purpose**: Protect against single data center failures
- **Strategy**: Deploy across multiple AZs for high availability

### Kubernetes Benefits
- **Container Orchestration**: Manage thousands of containers
- **Auto-scaling**: Automatically add/remove servers based on load
- **Self-healing**: Restart failed containers automatically
- **Rolling Updates**: Deploy new versions without downtime

### Database Deployment Strategy

#### PostgreSQL (User Data)
- **Master-Slave**: One write server, multiple read replicas
- **Sharding**: Split users across multiple databases
- **Backup**: Continuous backup to prevent data loss

#### Cassandra (Posts)
- **Multi-Region**: Replicate data across regions
- **Eventual Consistency**: Accept temporary inconsistency for performance
- **Auto-sharding**: Automatically distribute data

#### Redis (Cache)
- **Cluster Mode**: Multiple Redis nodes for scalability
- **Replication**: Backup cache data
- **Persistence**: Optional disk storage for important cache data

### Global Load Balancing
- **DNS-based**: Route users to nearest region
- **Health Checks**: Detect and avoid unhealthy regions
- **Failover**: Automatically switch to backup region

### Content Delivery Network (CDN)
- **Edge Locations**: 200+ locations worldwide
- **Caching**: Store popular content at edge
- **Origin Shield**: Protect origin servers from load

```mermaid
graph TB
    subgraph "Production Environment"
        subgraph "US-East Region"
            USEastAZ1[Availability Zone 1]
            USEastAZ2[Availability Zone 2]
            USEastAZ3[Availability Zone 3]
        end
        
        subgraph "US-West Region"
            USWestAZ1[Availability Zone 1]
            USWestAZ2[Availability Zone 2]
        end
        
        subgraph "EU Region"
            EUAZ1[Availability Zone 1]
            EUAZ2[Availability Zone 2]
        end
    end
    
    subgraph "Kubernetes Clusters"
        K8sUSEast[EKS Cluster US-East]
        K8sUSWest[EKS Cluster US-West]
        K8sEU[EKS Cluster EU]
    end
    
    subgraph "Database Clusters"
        PostgreSQLCluster[PostgreSQL Cluster<br/>Multi-AZ]
        CassandraCluster[Cassandra Cluster<br/>Multi-Region]
        RedisCluster[Redis Cluster<br/>Sharded]
        Neo4jCluster[Neo4j Cluster<br/>Causal Cluster]
    end
    
    subgraph "Global Services"
        Route53[Route 53 DNS]
        CloudFront[CloudFront CDN]
        S3Global[S3 Cross-Region Replication]
    end
    
    USEastAZ1 --> K8sUSEast
    USEastAZ2 --> K8sUSEast
    USEastAZ3 --> K8sUSEast
    
    USWestAZ1 --> K8sUSWest
    USWestAZ2 --> K8sUSWest
    
    EUAZ1 --> K8sEU
    EUAZ2 --> K8sEU
    
    K8sUSEast --> PostgreSQLCluster
    K8sUSWest --> PostgreSQLCluster
    K8sEU --> PostgreSQLCluster
    
    K8sUSEast --> CassandraCluster
    K8sUSWest --> CassandraCluster
    K8sEU --> CassandraCluster
    
    K8sUSEast --> RedisCluster
    K8sUSWest --> RedisCluster
    K8sEU --> RedisCluster
    
    Route53 --> CloudFront
    CloudFront --> K8sUSEast
    CloudFront --> K8sUSWest
    CloudFront --> K8sEU
    
    S3Global --> CloudFront
```

These architecture diagrams provide a comprehensive view of the Instagram clone system, showing how different components interact and scale to handle billions of users and massive amounts of content while maintaining high performance and availability.