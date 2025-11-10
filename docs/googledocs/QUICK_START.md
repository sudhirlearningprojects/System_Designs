# Google Docs Clone - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Prerequisites Check

Ensure you have:
```bash
java -version    # Should be 17+
mvn -version     # Should be 3.8+
docker --version # For infrastructure services
```

### Step 2: Start Infrastructure

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Verify services are running
docker ps
```

### Step 3: Configure Environment

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### Step 4: Build and Run

```bash
# Build the project
mvn clean install

# Run Google Docs system
./run-systems.sh googledocs

# Or directly with Maven
mvn spring-boot:run -Dspring-boot.run.profiles=googledocs
```

The application will start on **http://localhost:8091**

---

## 📝 Try It Out

### 1. Create a Document

```bash
curl -X POST http://localhost:8091/api/v1/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My First Document",
    "userId": "user-123"
  }'
```

**Response:**
```json
{
  "id": "doc-abc123",
  "title": "My First Document",
  "content": "",
  "ownerId": "user-123",
  "version": 1,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 2. Get the Document

```bash
curl -X GET http://localhost:8091/api/v1/documents/doc-abc123
```

### 3. Share the Document

```bash
curl -X POST http://localhost:8091/api/v1/documents/doc-abc123/share \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-456",
    "permissionType": "EDITOR",
    "grantedBy": "user-123"
  }'
```

### 4. Add a Comment

```bash
curl -X POST http://localhost:8091/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "doc-abc123",
    "userId": "user-456",
    "content": "Great document!",
    "startPosition": 0,
    "endPosition": 10
  }'
```

### 5. Create a Suggestion

```bash
curl -X POST http://localhost:8091/api/v1/suggestions \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "doc-abc123",
    "userId": "user-456",
    "startPosition": 5,
    "endPosition": 10,
    "originalText": "hello",
    "suggestedText": "Hello"
  }'
```

---

## 🔌 Real-Time Collaboration

### Connect via WebSocket

Create an HTML file (`test-websocket.html`):

```html
<!DOCTYPE html>
<html>
<head>
    <title>Google Docs Test</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <h1>Google Docs Real-Time Test</h1>
    <div id="status">Connecting...</div>
    <div id="users"></div>
    <textarea id="editor" rows="10" cols="50"></textarea>
    
    <script>
        const documentId = 'doc-abc123';
        const userId = 'user-123';
        const userName = 'Alice';
        
        // Connect to WebSocket
        const socket = new SockJS('http://localhost:8091/ws');
        const stompClient = Stomp.over(socket);
        
        stompClient.connect({}, function(frame) {
            document.getElementById('status').textContent = 'Connected!';
            
            // Subscribe to document updates
            stompClient.subscribe('/topic/document/' + documentId, function(message) {
                const operation = JSON.parse(message.body);
                console.log('Received operation:', operation);
                applyOperation(operation);
            });
            
            // Subscribe to cursor updates
            stompClient.subscribe('/topic/document/' + documentId + '/cursors', function(message) {
                const cursorData = JSON.parse(message.body);
                console.log('Cursor update:', cursorData);
            });
            
            // Subscribe to active users
            stompClient.subscribe('/topic/document/' + documentId + '/users', function(message) {
                const users = JSON.parse(message.body);
                displayUsers(users);
            });
            
            // Join the document
            stompClient.send('/app/document/' + documentId + '/join', {}, JSON.stringify({
                userId: userId,
                userName: userName
            }));
        });
        
        // Send edit operation
        const editor = document.getElementById('editor');
        let lastContent = '';
        
        editor.addEventListener('input', function(e) {
            const currentContent = editor.value;
            
            if (currentContent.length > lastContent.length) {
                // Insert operation
                const position = editor.selectionStart - 1;
                const text = currentContent[position];
                
                stompClient.send('/app/document/' + documentId + '/edit', {}, JSON.stringify({
                    type: 'INSERT',
                    position: position,
                    text: text,
                    userId: userId,
                    timestamp: Date.now(),
                    version: 1
                }));
            } else if (currentContent.length < lastContent.length) {
                // Delete operation
                const position = editor.selectionStart;
                
                stompClient.send('/app/document/' + documentId + '/edit', {}, JSON.stringify({
                    type: 'DELETE',
                    position: position,
                    length: 1,
                    userId: userId,
                    timestamp: Date.now(),
                    version: 1
                }));
            }
            
            lastContent = currentContent;
        });
        
        // Send cursor position
        editor.addEventListener('click', function(e) {
            stompClient.send('/app/document/' + documentId + '/cursor', {}, JSON.stringify({
                userId: userId,
                userName: userName,
                position: editor.selectionStart
            }));
        });
        
        function applyOperation(operation) {
            if (operation.userId === userId) return; // Skip own operations
            
            const content = editor.value;
            if (operation.type === 'INSERT') {
                editor.value = content.substring(0, operation.position) + 
                               operation.text + 
                               content.substring(operation.position);
            } else if (operation.type === 'DELETE') {
                editor.value = content.substring(0, operation.position) + 
                               content.substring(operation.position + operation.length);
            }
            lastContent = editor.value;
        }
        
        function displayUsers(users) {
            const usersDiv = document.getElementById('users');
            usersDiv.innerHTML = '<h3>Active Users:</h3>';
            Object.values(users).forEach(user => {
                usersDiv.innerHTML += `<div>${user.userName} (cursor: ${user.cursorPosition})</div>`;
            });
        }
    </script>
</body>
</html>
```

Open this file in multiple browser tabs to see real-time collaboration!

---

## 📊 Test Scenarios

### Scenario 1: Version History

```bash
# Create a document
DOC_ID=$(curl -s -X POST http://localhost:8091/api/v1/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Version Test", "userId": "user-123"}' | jq -r '.id')

# Save a version
curl -X POST http://localhost:8091/api/v1/documents/$DOC_ID/versions \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123", "description": "First draft"}'

# Get version history
curl -X GET http://localhost:8091/api/v1/documents/$DOC_ID/versions
```

### Scenario 2: Collaborative Editing

```bash
# User 1 creates document
DOC_ID=$(curl -s -X POST http://localhost:8091/api/v1/documents \
  -H "Content-Type: application/json" \
  -d '{"title": "Collab Doc", "userId": "user-123"}' | jq -r '.id')

# User 1 shares with User 2
curl -X POST http://localhost:8091/api/v1/documents/$DOC_ID/share \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-456", "permissionType": "EDITOR", "grantedBy": "user-123"}'

# User 2 adds a comment
curl -X POST http://localhost:8091/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'$DOC_ID'",
    "userId": "user-456",
    "content": "Looks good!",
    "startPosition": 0,
    "endPosition": 5
  }'

# User 2 creates a suggestion
curl -X POST http://localhost:8091/api/v1/suggestions \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'$DOC_ID'",
    "userId": "user-456",
    "startPosition": 0,
    "endPosition": 5,
    "originalText": "hello",
    "suggestedText": "Hello"
  }'
```

### Scenario 3: Comment Thread

```bash
# Add a comment
COMMENT_ID=$(curl -s -X POST http://localhost:8091/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "'$DOC_ID'",
    "userId": "user-123",
    "content": "What do you think?",
    "startPosition": 10,
    "endPosition": 20
  }' | jq -r '.id')

# Add a reply
curl -X POST http://localhost:8091/api/v1/comments/$COMMENT_ID/replies \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-456", "content": "I agree!"}'

# Add a reaction
curl -X POST http://localhost:8091/api/v1/comments/$COMMENT_ID/reactions \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-789", "emoji": "👍"}'

# Resolve the comment
curl -X PUT http://localhost:8091/api/v1/comments/$COMMENT_ID/resolve
```

---

## 🔍 Verify Installation

### Check Application Health

```bash
curl http://localhost:8091/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### Check Database Connection

```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres -d googledocs

# List tables
\dt

# Should see: documents, versions, comments, replies, suggestions, permissions
```

### Check Redis Connection

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Test Redis
PING
# Should return: PONG

# Check keys
KEYS *
```

---

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Find process using port 8091
lsof -i :8091

# Kill the process
kill -9 <PID>
```

### Database Connection Error

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Restart PostgreSQL
docker restart postgres

# Check logs
docker logs postgres
```

### Redis Connection Error

```bash
# Check Redis is running
docker ps | grep redis

# Restart Redis
docker restart redis

# Test connection
docker exec -it redis redis-cli PING
```

### WebSocket Connection Failed

1. Check CORS settings in `WebSocketConfig.java`
2. Verify WebSocket endpoint: `http://localhost:8091/ws`
3. Check browser console for errors
4. Try with SockJS fallback

---

## 📚 Next Steps

1. **Read Documentation**
   - [System Design](System_Design.md) - Architecture deep dive
   - [API Documentation](API_Documentation.md) - Complete API reference
   - [Scale Calculations](Scale_Calculations.md) - Performance analysis

2. **Explore Features**
   - Try real-time collaboration with multiple users
   - Test version history and restore
   - Experiment with suggestions and comments

3. **Customize**
   - Add authentication (JWT)
   - Implement export to PDF/Word
   - Add search functionality
   - Integrate AI features

4. **Deploy**
   - Containerize with Docker
   - Deploy to Kubernetes
   - Set up monitoring (Prometheus + Grafana)
   - Configure CDN for static assets

---

## 💡 Tips

1. **Use jq for JSON parsing**: `brew install jq`
2. **Use Postman**: Import API collection for easier testing
3. **Enable debug logging**: Set `logging.level.org.sudhir512kj.googledocs=DEBUG`
4. **Monitor Redis**: Use `redis-cli MONITOR` to see real-time commands
5. **Check WebSocket**: Use browser DevTools → Network → WS tab

---

## 🎯 Success Criteria

You've successfully set up Google Docs clone when:
- ✅ Application starts on port 8091
- ✅ Can create and retrieve documents
- ✅ Can share documents with permissions
- ✅ Can add comments and suggestions
- ✅ WebSocket connection works
- ✅ Real-time collaboration functions
- ✅ Version history is saved

---

## 📞 Need Help?

- **Documentation**: Check docs/googledocs/ folder
- **Issues**: Create GitHub issue
- **Email**: sudhir512kj@gmail.com
- **Portfolio**: [sudhirmeenaswe.netlify.app](https://sudhirmeenaswe.netlify.app/)

---

**Happy Coding! 🚀**
