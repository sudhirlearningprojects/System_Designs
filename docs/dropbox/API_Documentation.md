# Cloud Storage System - API Documentation

## Authentication

All API endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Base URL
```
https://api.cloudstorage.com/v1
```

## File Operations API

### 1. Upload File

**Endpoint:** `POST /files/upload`

**Description:** Upload a new file to the cloud storage

**Request:**
```http
POST /files/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <binary_file_data>
path: /documents/report.pdf
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "report.pdf",
  "path": "/documents/report.pdf",
  "size": 1048576,
  "contentHash": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
  "mimeType": "application/pdf",
  "version": 1,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### 2. Download File

**Endpoint:** `GET /files/{fileId}/download`

**Description:** Download a file by its ID

**Request:**
```http
GET /files/550e8400-e29b-41d4-a716-446655440000/download
Authorization: Bearer <token>
```

**Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="report.pdf"
Content-Length: 1048576

<binary_file_data>
```

### 3. Delete File

**Endpoint:** `DELETE /files/{fileId}`

**Description:** Mark a file as deleted

**Request:**
```http
DELETE /files/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <token>
```

**Response:**
```http
HTTP/1.1 200 OK
```

### 4. List Files

**Endpoint:** `GET /files`

**Description:** List user's files with optional filtering

**Query Parameters:**
- `path` (optional): Filter by path prefix
- `limit` (optional): Number of results (default: 50)
- `offset` (optional): Pagination offset (default: 0)

**Request:**
```http
GET /files?path=/documents&limit=20&offset=0
Authorization: Bearer <token>
```

**Response:**
```json
{
  "files": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "report.pdf",
      "path": "/documents/report.pdf",
      "size": 1048576,
      "mimeType": "application/pdf",
      "version": 1,
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalCount": 1,
  "hasMore": false
}
```

### 5. Get File Metadata

**Endpoint:** `GET /files/{fileId}`

**Description:** Get file metadata without downloading content

**Request:**
```http
GET /files/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "report.pdf",
  "path": "/documents/report.pdf",
  "size": 1048576,
  "contentHash": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
  "mimeType": "application/pdf",
  "version": 1,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

## Synchronization API

### 1. Get Changes Since Timestamp

**Endpoint:** `GET /sync/changes`

**Description:** Get all file changes since a specific timestamp

**Query Parameters:**
- `since`: ISO 8601 timestamp

**Request:**
```http
GET /sync/changes?since=2024-01-15T10:00:00Z
Authorization: Bearer <token>
```

**Response:**
```json
{
  "changes": [
    {
      "fileId": "550e8400-e29b-41d4-a716-446655440000",
      "operation": "UPLOAD",
      "timestamp": "2024-01-15T10:30:00Z",
      "file": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "report.pdf",
        "path": "/documents/report.pdf",
        "size": 1048576,
        "version": 1
      }
    }
  ],
  "lastSyncTimestamp": "2024-01-15T10:30:00Z"
}
```

### 2. Resolve Conflict

**Endpoint:** `POST /sync/conflicts/resolve`

**Description:** Resolve a file conflict

**Request:**
```json
{
  "fileId": "550e8400-e29b-41d4-a716-446655440000",
  "conflictId": "660e8400-e29b-41d4-a716-446655440001",
  "resolution": "keep_both" // or "keep_latest", "manual"
}
```

**Response:**
```json
{
  "resolved": true,
  "action": "renamed_conflict_file",
  "newFileName": "report_conflict_1642248600.pdf"
}
```

## Sharing API

### 1. Share File

**Endpoint:** `POST /files/{fileId}/share`

**Description:** Share a file with another user

**Request:**
```json
{
  "sharedWithEmail": "user@example.com",
  "permissionLevel": "read", // read, write, admin
  "expiresAt": "2024-02-15T10:30:00Z" // optional
}
```

**Response:**
```json
{
  "shareId": "770e8400-e29b-41d4-a716-446655440000",
  "sharedWithUser": {
    "id": "880e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  },
  "permissionLevel": "read",
  "expiresAt": "2024-02-15T10:30:00Z",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 2. List Shared Files

**Endpoint:** `GET /files/shared`

**Description:** List files shared with the current user

**Request:**
```http
GET /files/shared
Authorization: Bearer <token>
```

**Response:**
```json
{
  "sharedFiles": [
    {
      "shareId": "770e8400-e29b-41d4-a716-446655440000",
      "file": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "report.pdf",
        "path": "/documents/report.pdf",
        "size": 1048576
      },
      "sharedBy": {
        "id": "990e8400-e29b-41d4-a716-446655440000",
        "email": "owner@example.com"
      },
      "permissionLevel": "read",
      "sharedAt": "2024-01-15T10:30:00Z"
    }
  ]
}
```

## User Management API

### 1. Get User Profile

**Endpoint:** `GET /users/profile`

**Description:** Get current user's profile information

**Request:**
```http
GET /users/profile
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "storageQuota": 15000000000,
  "storageUsed": 5000000000,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### 2. Update Storage Quota

**Endpoint:** `PUT /users/quota`

**Description:** Update user's storage quota (admin only)

**Request:**
```json
{
  "newQuota": 50000000000
}
```

**Response:**
```json
{
  "success": true,
  "newQuota": 50000000000
}
```

## WebSocket Events

### Connection

**Endpoint:** `wss://api.cloudstorage.com/ws/sync`

**Authentication:** Include JWT token in connection headers

### Event Types

#### 1. File Upload Event
```json
{
  "type": "file.uploaded",
  "fileId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "report.pdf",
  "path": "/documents/report.pdf",
  "timestamp": "2024-01-15T10:30:00Z",
  "userId": "990e8400-e29b-41d4-a716-446655440000"
}
```

#### 2. File Delete Event
```json
{
  "type": "file.deleted",
  "fileId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "report.pdf",
  "path": "/documents/report.pdf",
  "timestamp": "2024-01-15T10:30:00Z",
  "userId": "990e8400-e29b-41d4-a716-446655440000"
}
```

#### 3. Sync Required Event
```json
{
  "type": "sync.required",
  "reason": "file_modified",
  "fileId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Error Responses

### Standard Error Format
```json
{
  "error": {
    "code": "FILE_NOT_FOUND",
    "message": "The requested file was not found",
    "details": {
      "fileId": "550e8400-e29b-41d4-a716-446655440000"
    },
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Invalid or missing authentication token |
| `FORBIDDEN` | 403 | Insufficient permissions for the operation |
| `FILE_NOT_FOUND` | 404 | Requested file does not exist |
| `STORAGE_QUOTA_EXCEEDED` | 413 | User has exceeded storage quota |
| `FILE_TOO_LARGE` | 413 | File exceeds maximum size limit |
| `INVALID_FILE_TYPE` | 415 | File type not supported |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests in time window |
| `INTERNAL_ERROR` | 500 | Internal server error |

## Rate Limiting

API endpoints are rate limited per user:

- **File Operations**: 100 requests per minute
- **Sync Operations**: 1000 requests per minute  
- **Upload Operations**: 10 requests per minute

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248660
```