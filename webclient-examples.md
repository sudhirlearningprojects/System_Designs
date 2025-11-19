# Spring WebClient Reactive Examples

This demo showcases multiple Spring WebClient features using JSONPlaceholder API.

## Features Demonstrated

### 1. Basic Configuration
- Custom WebClient bean with base URL
- Default headers and memory limits
- Timeout configuration

### 2. HTTP Operations
- **GET**: Retrieve single post
- **POST**: Create new post with retry logic
- **Filtering**: Get posts with title length > 10

### 3. Reactive Features
- **Mono**: Single value operations
- **Flux**: Stream operations
- **Parallel requests**: Combine multiple API calls
- **Backpressure**: Handle streaming with buffer
- **Error handling**: Fallback values and retry

### 4. Advanced Operations
- **Transformation**: Map and filter data
- **Delay**: Rate-limited streaming
- **Server-Sent Events**: Real-time streaming endpoint

## Running the Demo

```bash
./run-webclient.sh
```

## Testing Endpoints

```bash
# Get single post
curl http://localhost:8092/api/posts/1

# Get filtered posts (first 5 with title > 10 chars)
curl http://localhost:8092/api/posts

# Create post (with retry on failure)
curl -X POST http://localhost:8092/api/posts \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"title":"Test Post","body":"Test content"}'

# Get stats (parallel requests)
curl http://localhost:8092/api/posts/stats

# Stream titles (Server-Sent Events)
curl http://localhost:8092/api/posts/stream
```

## Key WebClient Features Used

1. **Builder Pattern**: Custom configuration
2. **Reactive Types**: Mono/Flux for async operations
3. **Error Handling**: onErrorReturn, retryWhen
4. **Transformation**: map, filter, take
5. **Composition**: Mono.zip for parallel requests
6. **Streaming**: Server-Sent Events with backpressure
7. **Timeout**: Request timeout configuration
8. **Logging**: Debug level for HTTP requests