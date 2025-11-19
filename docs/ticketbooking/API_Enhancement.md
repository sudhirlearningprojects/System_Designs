# BookMyShow Clone - Enhanced API Documentation

## 🎯 Complete API Reference

### Event Discovery & Search APIs

#### Advanced Event Search
```http
GET /api/events/search
```

**Query Parameters:**
- `city` (string): Filter by city name
- `category` (enum): MOVIES, LIVE_EVENTS, SPORTS, PLAYS, WORKSHOPS
- `genre` (string): Event genre (Action, Comedy, Drama, etc.)
- `language` (string): Content language
- `fromDate` (date): Start date filter
- `toDate` (date): End date filter
- `minPrice` (decimal): Minimum ticket price
- `maxPrice` (decimal): Maximum ticket price
- `sortBy` (string): rating, price, date, popularity

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Avengers: Endgame",
      "category": "MOVIES",
      "genre": "Action",
      "language": "English",
      "duration": "3h 1m",
      "rating": 4.5,
      "posterUrl": "https://cdn.bookmyshow.com/poster1.jpg",
      "city": "Mumbai",
      "minPrice": 150.00,
      "maxPrice": 500.00
    }
  ],
  "totalElements": 150,
  "totalPages": 15
}
```

#### Get Event Details
```http
GET /api/events/{id}
```

**Response:**
```json
{
  "id": 1,
  "name": "Avengers: Endgame",
  "description": "Epic superhero movie...",
  "category": "MOVIES",
  "genre": "Action",
  "language": "English",
  "duration": "3h 1m",
  "ageRating": "UA",
  "rating": 4.5,
  "reviewCount": 1250,
  "posterUrl": "https://cdn.bookmyshow.com/poster1.jpg",
  "trailerUrl": "https://youtube.com/watch?v=xyz",
  "galleryUrls": ["url1.jpg", "url2.jpg"],
  "cast": [
    {"name": "Robert Downey Jr.", "role": "Tony Stark"},
    {"name": "Chris Evans", "role": "Steve Rogers"}
  ],
  "crew": [
    {"name": "Anthony Russo", "role": "Director"},
    {"name": "Joe Russo", "role": "Director"}
  ]
}
```

### Show & Seat Selection APIs

#### Get Show Seats Layout
```http
GET /api/shows/{showId}/seats
```

**Response:**
```json
{
  "showId": 123,
  "venue": {
    "name": "PVR Cinemas",
    "totalSeats": 200,
    "layout": {
      "sections": [
        {
          "name": "Premium",
          "rows": [
            {
              "name": "A",
              "seats": [
                {
                  "id": 1,
                  "number": "A1",
                  "type": "PREMIUM",
                  "price": 300.00,
                  "status": "AVAILABLE"
                },
                {
                  "id": 2,
                  "number": "A2",
                  "type": "PREMIUM",
                  "price": 300.00,
                  "status": "BOOKED"
                }
              ]
            }
          ]
        }
      ]
    }
  }
}
```

#### Hold Seats
```http
POST /api/shows/{showId}/seats/hold
```

**Request:**
```json
{
  "userId": 456,
  "seatIds": [1, 2, 3]
}
```

**Response:**
```json
{
  "holdId": "hold_789_1640995200",
  "expiresAt": "2024-01-15T10:40:00Z",
  "totalAmount": 900.00,
  "seats": [
    {"id": 1, "number": "A1", "price": 300.00},
    {"id": 2, "number": "A2", "price": 300.00},
    {"id": 3, "number": "A3", "price": 300.00}
  ]
}
```

### Booking Workflow APIs

#### Initiate Booking
```http
POST /api/bookings/initiate
```

**Request:**
```json
{
  "userId": 456,
  "showId": 123,
  "seatIds": [1, 2, 3]
}
```

**Response:**
```json
{
  "bookingId": 789,
  "bookingReference": "BMS789123456",
  "totalAmount": 900.00,
  "holdExpiresAt": "2024-01-15T10:40:00Z",
  "status": "HELD"
}
```

#### Add F&B Items
```http
POST /api/bookings/{bookingId}/add-food
```

**Request:**
```json
{
  "items": [
    {"foodId": 10, "quantity": 2},
    {"foodId": 15, "quantity": 1}
  ]
}
```

**Response:**
```json
{
  "bookingId": 789,
  "ticketAmount": 900.00,
  "foodAmount": 450.00,
  "totalAmount": 1350.00,
  "foodItems": [
    {"name": "Popcorn Large", "quantity": 2, "price": 200.00},
    {"name": "Coke", "quantity": 1, "price": 50.00}
  ]
}
```

#### Apply Offer
```http
POST /api/bookings/{bookingId}/apply-offer
```

**Request:**
```json
{
  "offerCode": "SAVE20"
}
```

**Response:**
```json
{
  "bookingId": 789,
  "originalAmount": 1350.00,
  "discountAmount": 270.00,
  "finalAmount": 1080.00,
  "offerDetails": {
    "code": "SAVE20",
    "description": "20% off on total booking",
    "discountType": "PERCENTAGE",
    "discountValue": 20
  }
}
```

#### Initiate Payment
```http
POST /api/bookings/{bookingId}/payment
```

**Request:**
```json
{
  "paymentMethod": "CREDIT_CARD",
  "cardDetails": {
    "number": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123"
  }
}
```

**Response:**
```json
{
  "paymentId": "pay_abc123",
  "status": "INITIATED",
  "gatewayUrl": "https://payment.gateway.com/pay/abc123",
  "amount": 1080.00
}
```

### User Management APIs

#### User Registration
```http
POST /api/users/register
```

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123",
  "phoneNumber": "+919876543210",
  "city": "Mumbai",
  "dateOfBirth": "1990-05-15"
}
```

#### Get User Bookings
```http
GET /api/users/{userId}/bookings?status=CONFIRMED&page=0&size=10
```

**Response:**
```json
{
  "content": [
    {
      "bookingId": 789,
      "bookingReference": "BMS789123456",
      "eventName": "Avengers: Endgame",
      "showDate": "2024-01-15T19:30:00Z",
      "venueName": "PVR Cinemas",
      "seatNumbers": ["A1", "A2", "A3"],
      "totalAmount": 1080.00,
      "status": "CONFIRMED",
      "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
    }
  ]
}
```

### Venue & F&B APIs

#### Search Venues
```http
GET /api/venues/search?city=Mumbai&type=CINEMA_HALL
```

#### Get Venue Menu
```http
GET /api/venues/{venueId}/menu
```

**Response:**
```json
{
  "venueId": 10,
  "venueName": "PVR Cinemas",
  "menu": [
    {
      "category": "SNACKS",
      "items": [
        {
          "id": 1,
          "name": "Popcorn Large",
          "description": "Buttered popcorn",
          "price": 200.00,
          "isVegetarian": true,
          "preparationTime": 5
        }
      ]
    },
    {
      "category": "BEVERAGES",
      "items": [
        {
          "id": 2,
          "name": "Coke",
          "price": 50.00,
          "isVegetarian": true,
          "preparationTime": 2
        }
      ]
    }
  ]
}
```

### Offers & Promotions APIs

#### Get Active Offers
```http
GET /api/offers/active?city=Mumbai&category=MOVIES
```

**Response:**
```json
[
  {
    "id": 1,
    "title": "Weekend Special",
    "description": "20% off on weekend bookings",
    "offerCode": "WEEKEND20",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "minBookingAmount": 500.00,
    "maxDiscountAmount": 200.00,
    "validUntil": "2024-01-31T23:59:59Z"
  }
]
```

#### Validate Offer
```http
POST /api/offers/validate
```

**Request:**
```json
{
  "offerCode": "WEEKEND20",
  "bookingAmount": 1000.00,
  "userId": 456,
  "eventId": 123
}
```

**Response:**
```json
{
  "isValid": true,
  "discountAmount": 200.00,
  "finalAmount": 800.00,
  "message": "Offer applied successfully"
}
```

### Admin APIs

#### Create Event
```http
POST /api/events
```

**Request:**
```json
{
  "name": "Spider-Man: No Way Home",
  "description": "Latest Spider-Man movie",
  "category": "MOVIES",
  "genre": "Action",
  "language": "English",
  "duration": "2h 28m",
  "ageRating": "UA",
  "posterUrl": "https://cdn.example.com/spiderman.jpg",
  "cast": [
    {"name": "Tom Holland", "role": "Spider-Man"}
  ]
}
```

#### Create Show
```http
POST /api/shows
```

**Request:**
```json
{
  "eventId": 123,
  "venueId": 456,
  "showDate": "2024-01-15T19:30:00Z",
  "endDate": "2024-01-15T22:00:00Z"
}
```

## 🔐 Authentication & Security

### JWT Token Authentication
All protected endpoints require JWT token in Authorization header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Rate Limiting
- **Search APIs**: 100 requests/minute per user
- **Booking APIs**: 10 requests/minute per user
- **Payment APIs**: 5 requests/minute per user

### Input Validation
- All inputs are validated and sanitized
- SQL injection prevention
- XSS protection
- CSRF tokens for state-changing operations

This enhanced API provides comprehensive BookMyShow functionality with proper error handling, validation, and security measures.