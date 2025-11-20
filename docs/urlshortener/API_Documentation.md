# URL Shortener API Documentation

## Overview
This document provides comprehensive API documentation for the TinyURL Clone - URL Shortener service.

**Base URL**: `http://localhost:8092`

## Authentication
Currently, the API does not require authentication. In production, implement API key-based authentication.

## Rate Limiting
- **Default**: 1000 requests per hour per IP
- **Headers**: Rate limit information is returned in response headers

## API Endpoints

### 1. Shorten URL

**Endpoint**: `POST /api/v1/urls/shorten`

**Description**: Convert a long URL into a shortened version.

**Request Body**:
```json
{
    "longUrl": "https://www.example.com/very/long/path/to/resource",
    "customAlias": "my-custom-link",  // optional
    "expiresAt": "2024-12-31T23:59:59"  // optional
}
```

**Response**:
```json
{
    "shortUrl": "http://localhost:8092/abc123",
    "longUrl": "https://www.example.com/very/long/path/to/resource",
    "expiresAt": "2024-12-31T23:59:59",
    "createdAt": "2024-01-15T10:30:00"
}
```

**Status Codes**:
- `200 OK`: URL shortened successfully
- `400 Bad Request`: Invalid URL format or custom alias already exists
- `429 Too Many Requests`: Rate limit exceeded

**Example cURL**:
```bash
curl -X POST http://localhost:8092/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.google.com/search?q=system+design",
    "customAlias": "google-search"
  }'
```

### 2. Redirect Short URL

**Endpoint**: `GET /{shortUrl}`

**Description**: Redirect to the original long URL.

**Parameters**:
- `shortUrl` (path): The short URL identifier (e.g., "abc123")

**Response**:
- `301 Moved Permanently`: Successful redirect
- `404 Not Found`: Short URL not found or expired

**Headers**:
- `Location`: The original long URL

**Example**:
```bash
curl -I http://localhost:8092/abc123
```

**Response**:
```
HTTP/1.1 301 Moved Permanently
Location: https://www.example.com/very/long/path/to/resource
```

### 3. Get URL Analytics (Future Enhancement)

**Endpoint**: `GET /api/v1/urls/{shortUrl}/analytics`

**Description**: Get analytics data for a short URL.

**Response**:
```json
{
    "shortUrl": "abc123",
    "totalClicks": 1500,
    "clicksByCountry": {
        "US": 800,
        "IN": 400,
        "UK": 300
    },
    "clicksByDate": {
        "2024-01-15": 100,
        "2024-01-16": 150
    },
    "topReferrers": [
        {"domain": "google.com", "clicks": 500},
        {"domain": "facebook.com", "clicks": 300}
    ]
}
```

## Error Responses

All error responses follow this format:

```json
{
    "timestamp": "2024-01-15T10:30:00.000Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Invalid URL format",
    "path": "/api/v1/urls/shorten"
}
```

## Request/Response Examples

### Successful URL Shortening

**Request**:
```bash
POST /api/v1/urls/shorten
Content-Type: application/json

{
    "longUrl": "https://github.com/sudhir512kj/system-designs"
}
```

**Response**:
```json
{
    "shortUrl": "http://localhost:8092/a1b2c3",
    "longUrl": "https://github.com/sudhir512kj/system-designs",
    "expiresAt": null,
    "createdAt": "2024-01-15T10:30:00"
}
```

### Custom Alias

**Request**:
```bash
POST /api/v1/urls/shorten
Content-Type: application/json

{
    "longUrl": "https://docs.spring.io/spring-boot/docs/current/reference/html/",
    "customAlias": "spring-docs"
}
```

**Response**:
```json
{
    "shortUrl": "http://localhost:8092/spring-docs",
    "longUrl": "https://docs.spring.io/spring-boot/docs/current/reference/html/",
    "expiresAt": null,
    "createdAt": "2024-01-15T10:30:00"
}
```

### URL with Expiration

**Request**:
```bash
POST /api/v1/urls/shorten
Content-Type: application/json

{
    "longUrl": "https://example.com/temporary-offer",
    "expiresAt": "2024-02-01T00:00:00"
}
```

**Response**:
```json
{
    "shortUrl": "http://localhost:8092/x9y8z7",
    "longUrl": "https://example.com/temporary-offer",
    "expiresAt": "2024-02-01T00:00:00",
    "createdAt": "2024-01-15T10:30:00"
}
```

## Testing the API

### Using cURL

1. **Shorten a URL**:
```bash
curl -X POST http://localhost:8092/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com"}'
```

2. **Test the redirect**:
```bash
curl -I http://localhost:8092/{returned-short-url}
```

### Using Postman

1. Import the following collection:

```json
{
    "info": {
        "name": "URL Shortener API",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "Shorten URL",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"longUrl\": \"https://www.example.com\"\n}"
                },
                "url": {
                    "raw": "http://localhost:8092/api/v1/urls/shorten",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8092",
                    "path": ["api", "v1", "urls", "shorten"]
                }
            }
        }
    ]
}
```

## Performance Considerations

- **Caching**: URLs are cached in Redis for fast retrieval
- **Database**: Uses connection pooling for optimal performance
- **Async Processing**: Analytics tracking is performed asynchronously

## Security Features

- **URL Validation**: Validates URL format and blocks malicious domains
- **Rate Limiting**: Prevents abuse with configurable rate limits
- **Input Sanitization**: All inputs are validated and sanitized

## Monitoring

The API exposes metrics at `/actuator/metrics` for monitoring:
- Request count and response times
- Cache hit/miss ratios
- Database connection pool status