# Uber Clone - API Documentation

## Base URL
```
Production: https://api.uber-clone.com
Development: http://localhost:8090
```

## Authentication
All APIs require JWT token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## Rider APIs

### 1. Request Ride
**Endpoint**: `POST /api/v1/rides/request`

**Request Body**:
```json
{
  "riderId": "uuid",
  "pickupLocation": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "address": "123 Market St, San Francisco"
  },
  "dropoffLocation": {
    "latitude": 37.8044,
    "longitude": -122.2712,
    "address": "456 Broadway, Oakland"
  },
  "vehicleType": "UBERX"
}
```

**Response**: `200 OK`
```json
{
  "rideId": "uuid",
  "status": "REQUESTED",
  "estimatedFare": 25.50,
  "requestedAt": "2024-01-15T10:30:00"
}
```

### 2. Get Ride Details
**Endpoint**: `GET /api/v1/rides/{rideId}`

**Response**: `200 OK`
```json
{
  "rideId": "uuid",
  "riderId": "uuid",
  "driverId": "uuid",
  "status": "STARTED",
  "pickupLocation": {...},
  "dropoffLocation": {...},
  "estimatedFare": 25.50,
  "actualFare": 27.30,
  "startedAt": "2024-01-15T10:35:00"
}
```

### 3. Cancel Ride
**Endpoint**: `PUT /api/v1/rides/{rideId}/cancel`

**Response**: `200 OK`

---

## Driver APIs

### 1. Update Location
**Endpoint**: `POST /api/v1/drivers/{driverId}/location`

**Request Body**:
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194
}
```

**Response**: `200 OK`

### 2. Accept Ride
**Endpoint**: `POST /api/v1/drivers/rides/{rideId}/accept?driverId={driverId}`

**Response**: `200 OK`

### 3. Start Ride
**Endpoint**: `PUT /api/v1/drivers/rides/{rideId}/start`

**Response**: `200 OK`

### 4. Complete Ride
**Endpoint**: `PUT /api/v1/drivers/rides/{rideId}/complete?actualFare=27.30`

**Response**: `200 OK`

---

## Error Responses

```json
{
  "error": "RIDE_NOT_FOUND",
  "message": "Ride with id xyz not found",
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error Codes**:
- `400` - Bad Request
- `401` - Unauthorized
- `404` - Not Found
- `500` - Internal Server Error
