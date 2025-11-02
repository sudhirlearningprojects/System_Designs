#!/bin/bash

# Test Parking Lot API endpoints

BASE_URL="http://localhost:8080/api/parking"

echo "Testing Parking Lot Management System APIs..."
echo ""

# Test vehicle entry
echo "1. Testing vehicle entry..."
ENTRY_RESPONSE=$(curl -s -X POST "$BASE_URL/entry" \
  -H "Content-Type: application/json" \
  -d '{
    "licensePlate": "ABC123",
    "vehicleType": "CAR"
  }')

echo "Entry Response: $ENTRY_RESPONSE"
echo ""

# Extract ticket ID (assuming JSON response)
TICKET_ID=$(echo $ENTRY_RESPONSE | grep -o '"ticketId":"[^"]*' | cut -d'"' -f4)

# Test availability check
echo "2. Testing availability check for floor 1..."
AVAILABILITY_RESPONSE=$(curl -s -X GET "$BASE_URL/availability/1")
echo "Availability Response: $AVAILABILITY_RESPONSE"
echo ""

# Test vehicle exit (if ticket ID was extracted)
if [ ! -z "$TICKET_ID" ]; then
  echo "3. Testing vehicle exit with ticket: $TICKET_ID"
  EXIT_RESPONSE=$(curl -s -X POST "$BASE_URL/exit" \
    -H "Content-Type: application/json" \
    -d "{
      \"ticketId\": \"$TICKET_ID\"
    }")
  echo "Exit Response: $EXIT_RESPONSE"
else
  echo "3. Skipping exit test - no ticket ID found"
fi

echo ""
echo "API testing completed!"