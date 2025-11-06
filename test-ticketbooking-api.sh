#!/bin/bash

# Ticket Booking Platform API Test Script
# Tests the core booking workflow: search events -> hold tickets -> confirm booking

BASE_URL="http://localhost:8080/api"
USER_ID=123
EVENT_ID=1
TICKET_TYPE_ID=2

echo "🎫 Testing Ticket Booking Platform APIs"
echo "========================================"

# Test 1: Search Events
echo "1. Testing Event Search..."
curl -s -X GET "$BASE_URL/events/search?city=Mumbai&genre=Concert" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 2: Get Event Details
echo "2. Testing Get Event Details..."
curl -s -X GET "$BASE_URL/events/$EVENT_ID" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 3: Get Ticket Types
echo "3. Testing Get Ticket Types..."
curl -s -X GET "$BASE_URL/events/$EVENT_ID/ticket-types" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 4: Hold Tickets
echo "4. Testing Ticket Hold..."
HOLD_RESPONSE=$(curl -s -X POST "$BASE_URL/bookings/hold" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": $USER_ID,
    \"eventId\": $EVENT_ID,
    \"ticketTypeId\": $TICKET_TYPE_ID,
    \"quantity\": 2
  }")

echo "$HOLD_RESPONSE" | jq '.'

# Extract booking ID for confirmation
BOOKING_ID=$(echo "$HOLD_RESPONSE" | jq -r '.data.bookingId // empty')

echo -e "\n"

# Test 5: Confirm Booking (if hold was successful)
if [ ! -z "$BOOKING_ID" ] && [ "$BOOKING_ID" != "null" ]; then
  echo "5. Testing Booking Confirmation..."
  PAYMENT_ID="pay_test_$(date +%s)"
  
  curl -s -X POST "$BASE_URL/bookings/$BOOKING_ID/confirm?paymentId=$PAYMENT_ID" \
    -H "Content-Type: application/json" | jq '.'
  
  echo -e "\n"
  
  # Test 6: Get User Bookings
  echo "6. Testing Get User Bookings..."
  curl -s -X GET "$BASE_URL/bookings/user/$USER_ID" \
    -H "Content-Type: application/json" | jq '.'
else
  echo "5. Skipping booking confirmation - hold failed"
  echo "6. Skipping user bookings test"
fi

echo -e "\n"

# Test 7: Health Check
echo "7. Testing System Health..."
curl -s -X GET "$BASE_URL/actuator/health" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"
echo "✅ Ticket Booking API tests completed!"
echo "Note: Some tests may fail if the database is not properly initialized with test data."