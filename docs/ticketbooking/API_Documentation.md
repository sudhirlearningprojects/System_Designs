# Ticket Booking Platform - API Documentation

## Base URL
```
Production: https://api.ticketbooking.com/v1
Staging: https://staging-api.ticketbooking.com/v1
Development: http://localhost:8080/api
```

## Authentication
All API requests require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Rate Limiting
- **Authenticated users**: 1000 requests per hour
- **Anonymous users**: 100 requests per hour
- **Admin users**: 5000 requests per hour

## Response Format
All API responses follow this standard format:
```json
{
  "success": true,
  "data": {},
  "message": "Success",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

Error responses:
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_TICKETS",
    "message": "Not enough tickets available",
    "details": {}
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 1. Event Management APIs

### 1.1 Search Events
Search for events with filters and pagination.

**Endpoint**: `GET /events/search`

**Parameters**:
- `city` (optional): Filter by city name
- `genre` (optional): Filter by event genre
- `name` (optional): Search by event name
- `page` (optional, default: 0): Page number
- `size` (optional, default: 20): Page size
- `sort` (optional, default: eventDate): Sort field

**Example Request**:
```bash
GET /events/search?city=Mumbai&genre=Concert&page=0&size=10&sort=eventDate,asc
```

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Rock Concert 2024",
        "description": "Amazing rock concert featuring top artists",
        "venue": "Mumbai Stadium",
        "city": "Mumbai",
        "genre": "Concert",
        "eventDate": "2024-03-15T19:00:00Z",
        "createdAt": "2024-01-10T10:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

### 1.2 Get Event Details
Retrieve detailed information about a specific event.

**Endpoint**: `GET /events/{eventId}`

**Example Request**:
```bash
GET /events/1
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Rock Concert 2024",
    "description": "Amazing rock concert featuring top artists",
    "venue": "Mumbai Stadium",
    "city": "Mumbai",
    "genre": "Concert",
    "eventDate": "2024-03-15T19:00:00Z",
    "createdAt": "2024-01-10T10:00:00Z"
  }
}
```

### 1.3 Get Ticket Types
Retrieve available ticket types for an event.

**Endpoint**: `GET /events/{eventId}/ticket-types`

**Example Request**:
```bash
GET /events/1/ticket-types
```

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "VIP",
      "price": 5000.00,
      "totalQuantity": 100,
      "availableQuantity": 85
    },
    {
      "id": 2,
      "name": "General",
      "price": 2000.00,
      "totalQuantity": 500,
      "availableQuantity": 342
    }
  ]
}
```

### 1.4 Create Event (Admin Only)
Create a new event.

**Endpoint**: `POST /events`

**Request Body**:
```json
{
  "name": "Jazz Night 2024",
  "description": "Smooth jazz evening with renowned artists",
  "venue": "Blue Note Club",
  "city": "Delhi",
  "genre": "Jazz",
  "eventDate": "2024-04-20T20:00:00Z"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 2,
    "name": "Jazz Night 2024",
    "description": "Smooth jazz evening with renowned artists",
    "venue": "Blue Note Club",
    "city": "Delhi",
    "genre": "Jazz",
    "eventDate": "2024-04-20T20:00:00Z",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

---

## 2. Booking Management APIs

### 2.1 Hold Tickets
Hold tickets for a limited time before payment.

**Endpoint**: `POST /bookings/hold`

**Request Body**:
```json
{
  "userId": 123,
  "eventId": 1,
  "ticketTypeId": 2,
  "quantity": 2
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "bookingId": 456,
    "holdId": "hold_abc123def456",
    "status": "HELD",
    "holdExpiresAt": "2024-01-15T10:40:00Z",
    "totalAmount": 4000.00
  }
}
```

**Error Responses**:
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_TICKETS",
    "message": "Only 1 ticket available, requested 2"
  }
}
```

### 2.2 Confirm Booking
Confirm a held booking after successful payment.

**Endpoint**: `POST /bookings/{bookingId}/confirm`

**Parameters**:
- `paymentId`: Payment transaction ID from payment gateway

**Example Request**:
```bash
POST /bookings/456/confirm?paymentId=pay_xyz789abc123
```

**Response**:
```json
{
  "success": true,
  "data": {
    "bookingId": 456,
    "status": "CONFIRMED",
    "totalAmount": 4000.00
  }
}
```

**Error Responses**:
```json
{
  "success": false,
  "error": {
    "code": "BOOKING_EXPIRED",
    "message": "Booking hold has expired"
  }
}
```

### 2.3 Get User Bookings
Retrieve all bookings for a specific user.

**Endpoint**: `GET /bookings/user/{userId}`

**Example Request**:
```bash
GET /bookings/user/123
```

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 456,
      "event": {
        "id": 1,
        "name": "Rock Concert 2024",
        "venue": "Mumbai Stadium",
        "eventDate": "2024-03-15T19:00:00Z"
      },
      "ticketType": {
        "id": 2,
        "name": "General",
        "price": 2000.00
      },
      "quantity": 2,
      "totalAmount": 4000.00,
      "status": "CONFIRMED",
      "createdAt": "2024-01-15T10:30:00Z",
      "paymentId": "pay_xyz789abc123"
    }
  ]
}
```

### 2.4 Cancel Booking
Cancel a confirmed booking (if cancellation is allowed).

**Endpoint**: `DELETE /bookings/{bookingId}`

**Example Request**:
```bash
DELETE /bookings/456
```

**Response**:
```json
{
  "success": true,
  "data": {
    "bookingId": 456,
    "status": "CANCELLED",
    "refundAmount": 3600.00,
    "refundId": "ref_abc123def456"
  }
}
```

---

## 3. User Management APIs

### 3.1 User Registration
Register a new user account.

**Endpoint**: `POST /users/register`

**Request Body**:
```json
{
  "email": "user@example.com",
  "name": "John Doe",
  "password": "securePassword123",
  "phoneNumber": "+91-9876543210"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 123,
    "email": "user@example.com",
    "name": "John Doe",
    "phoneNumber": "+91-9876543210",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 3.2 User Login
Authenticate user and receive JWT token.

**Endpoint**: `POST /users/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 123,
      "email": "user@example.com",
      "name": "John Doe"
    },
    "expiresAt": "2024-01-16T10:30:00Z"
  }
}
```

### 3.3 Get User Profile
Retrieve user profile information.

**Endpoint**: `GET /users/profile`

**Headers**: `Authorization: Bearer <jwt_token>`

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 123,
    "email": "user@example.com",
    "name": "John Doe",
    "phoneNumber": "+91-9876543210",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 3.4 Update User Profile
Update user profile information.

**Endpoint**: `PUT /users/profile`

**Headers**: `Authorization: Bearer <jwt_token>`

**Request Body**:
```json
{
  "name": "John Smith",
  "phoneNumber": "+91-9876543211"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 123,
    "email": "user@example.com",
    "name": "John Smith",
    "phoneNumber": "+91-9876543211",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

---

## 4. Payment APIs

### 4.1 Initiate Payment
Initiate payment process for a booking.

**Endpoint**: `POST /payments/initiate`

**Request Body**:
```json
{
  "bookingId": 456,
  "amount": 4000.00,
  "currency": "INR",
  "paymentMethod": "card"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "paymentId": "pay_xyz789abc123",
    "clientSecret": "pi_xyz_secret_abc123",
    "amount": 4000.00,
    "currency": "INR",
    "status": "requires_payment_method"
  }
}
```

### 4.2 Payment Callback
Handle payment gateway callback (webhook).

**Endpoint**: `POST /payments/callback`

**Request Body** (from payment gateway):
```json
{
  "paymentId": "pay_xyz789abc123",
  "status": "succeeded",
  "amount": 4000.00,
  "currency": "INR",
  "metadata": {
    "bookingId": "456"
  }
}
```

**Response**:
```json
{
  "success": true,
  "message": "Payment processed successfully"
}
```

---

## 5. Admin APIs

### 5.1 Get Booking Analytics
Retrieve booking analytics and statistics.

**Endpoint**: `GET /admin/analytics/bookings`

**Parameters**:
- `startDate`: Start date for analytics (ISO 8601)
- `endDate`: End date for analytics (ISO 8601)
- `eventId` (optional): Filter by specific event

**Example Request**:
```bash
GET /admin/analytics/bookings?startDate=2024-01-01T00:00:00Z&endDate=2024-01-31T23:59:59Z
```

**Response**:
```json
{
  "success": true,
  "data": {
    "totalBookings": 1250,
    "totalRevenue": 2500000.00,
    "averageBookingValue": 2000.00,
    "bookingsByStatus": {
      "CONFIRMED": 1100,
      "CANCELLED": 50,
      "EXPIRED": 100
    },
    "topEvents": [
      {
        "eventId": 1,
        "eventName": "Rock Concert 2024",
        "bookings": 500,
        "revenue": 1000000.00
      }
    ]
  }
}
```

### 5.2 Manage Event Inventory
Update ticket inventory for an event.

**Endpoint**: `PUT /admin/events/{eventId}/inventory`

**Request Body**:
```json
{
  "ticketTypeId": 2,
  "additionalQuantity": 50
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "ticketTypeId": 2,
    "previousQuantity": 500,
    "newQuantity": 550,
    "availableQuantity": 392
  }
}
```

---

## 6. Error Codes

| Code | Description | HTTP Status |
|------|-------------|-------------|
| `INVALID_REQUEST` | Request validation failed | 400 |
| `UNAUTHORIZED` | Authentication required | 401 |
| `FORBIDDEN` | Insufficient permissions | 403 |
| `EVENT_NOT_FOUND` | Event does not exist | 404 |
| `USER_NOT_FOUND` | User does not exist | 404 |
| `BOOKING_NOT_FOUND` | Booking does not exist | 404 |
| `INSUFFICIENT_TICKETS` | Not enough tickets available | 409 |
| `BOOKING_EXPIRED` | Booking hold has expired | 409 |
| `PAYMENT_FAILED` | Payment processing failed | 422 |
| `DUPLICATE_BOOKING` | Duplicate booking attempt | 409 |
| `RATE_LIMIT_EXCEEDED` | Too many requests | 429 |
| `INTERNAL_ERROR` | Server error | 500 |
| `SERVICE_UNAVAILABLE` | Service temporarily unavailable | 503 |

---

## 7. Webhooks

### 7.1 Payment Status Webhook
Notifies about payment status changes.

**URL**: `POST /webhooks/payment-status`

**Payload**:
```json
{
  "eventType": "payment.succeeded",
  "paymentId": "pay_xyz789abc123",
  "bookingId": 456,
  "amount": 4000.00,
  "currency": "INR",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

### 7.2 Booking Status Webhook
Notifies about booking status changes.

**URL**: `POST /webhooks/booking-status`

**Payload**:
```json
{
  "eventType": "booking.confirmed",
  "bookingId": 456,
  "userId": 123,
  "eventId": 1,
  "status": "CONFIRMED",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

---

## 8. SDK Examples

### 8.1 JavaScript/Node.js
```javascript
const TicketBookingAPI = require('@ticketbooking/api-client');

const client = new TicketBookingAPI({
  baseURL: 'https://api.ticketbooking.com/v1',
  apiKey: 'your-api-key'
});

// Search events
const events = await client.events.search({
  city: 'Mumbai',
  genre: 'Concert',
  page: 0,
  size: 10
});

// Hold tickets
const booking = await client.bookings.hold({
  userId: 123,
  eventId: 1,
  ticketTypeId: 2,
  quantity: 2
});

// Confirm booking
const confirmed = await client.bookings.confirm(booking.bookingId, {
  paymentId: 'pay_xyz789abc123'
});
```

### 8.2 Python
```python
from ticketbooking import TicketBookingClient

client = TicketBookingClient(
    base_url='https://api.ticketbooking.com/v1',
    api_key='your-api-key'
)

# Search events
events = client.events.search(
    city='Mumbai',
    genre='Concert',
    page=0,
    size=10
)

# Hold tickets
booking = client.bookings.hold(
    user_id=123,
    event_id=1,
    ticket_type_id=2,
    quantity=2
)

# Confirm booking
confirmed = client.bookings.confirm(
    booking_id=booking['bookingId'],
    payment_id='pay_xyz789abc123'
)
```

This API documentation provides comprehensive coverage of all endpoints, request/response formats, error handling, and integration examples for the ticket booking platform.