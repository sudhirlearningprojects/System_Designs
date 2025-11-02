#!/bin/bash

# Test Digital Payment Platform API endpoints

BASE_URL="http://localhost:8084/api/payments"

echo "Testing Digital Payment Platform APIs..."
echo ""

# Test payment initiation
echo "1. Testing payment initiation..."
PAYMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/initiate" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user123",
    "receiverId": "user456",
    "amount": 1000.00,
    "type": "P2P",
    "paymentMethod": "WALLET",
    "description": "Test payment",
    "idempotencyKey": "test-payment-001"
  }')

echo "Payment Response: $PAYMENT_RESPONSE"
echo ""

# Extract transaction ID (assuming JSON response)
TRANSACTION_ID=$(echo $PAYMENT_RESPONSE | grep -o '"transactionId":"[^"]*' | cut -d'"' -f4)

# Test transaction status
if [ ! -z "$TRANSACTION_ID" ]; then
  echo "2. Testing transaction status for: $TRANSACTION_ID"
  STATUS_RESPONSE=$(curl -s -X GET "$BASE_URL/status/$TRANSACTION_ID")
  echo "Status Response: $STATUS_RESPONSE"
else
  echo "2. Skipping status test - no transaction ID found"
fi
echo ""

# Test wallet balance
echo "3. Testing wallet balance check..."
BALANCE_RESPONSE=$(curl -s -X GET "$BASE_URL/balance/user123")
echo "Balance Response: $BALANCE_RESPONSE"
echo ""

# Test transaction history
echo "4. Testing transaction history..."
HISTORY_RESPONSE=$(curl -s -X GET "$BASE_URL/history/user123?page=0&size=5")
echo "History Response: $HISTORY_RESPONSE"
echo ""

# Test duplicate payment (idempotency)
echo "5. Testing idempotency with duplicate payment..."
DUPLICATE_RESPONSE=$(curl -s -X POST "$BASE_URL/initiate" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user123",
    "receiverId": "user456",
    "amount": 1000.00,
    "type": "P2P",
    "paymentMethod": "WALLET",
    "description": "Test payment",
    "idempotencyKey": "test-payment-001"
  }')

echo "Duplicate Payment Response: $DUPLICATE_RESPONSE"
echo ""

echo "API testing completed!"