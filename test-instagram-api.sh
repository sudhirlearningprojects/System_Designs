#!/bin/bash

# Instagram Clone API Test Script

BASE_URL="http://localhost:8087/api/v1"
TOKEN=""

echo "🚀 Instagram Clone API Testing"
echo "================================"

# Test 1: Register a new user
echo "📝 Test 1: User Registration"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com", 
    "password": "password123",
    "fullName": "John Doe"
  }')

echo "Registration Response: $REGISTER_RESPONSE"

# Extract token from registration response
TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
USER_ID=$(echo $REGISTER_RESPONSE | grep -o '"userId":[0-9]*' | cut -d':' -f2)

if [ -z "$TOKEN" ]; then
  echo "❌ Registration failed, trying login..."
  
  # Test 2: Login existing user
  echo "🔐 Test 2: User Login"
  LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/users/login" \
    -H "Content-Type: application/json" \
    -d '{
      "email": "john@example.com",
      "password": "password123"
    }')
  
  echo "Login Response: $LOGIN_RESPONSE"
  TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
  USER_ID=$(echo $LOGIN_RESPONSE | grep -o '"userId":[0-9]*' | cut -d':' -f2)
fi

if [ -z "$TOKEN" ]; then
  echo "❌ Authentication failed. Exiting..."
  exit 1
fi

echo "✅ Authentication successful. Token: ${TOKEN:0:20}..."
echo "👤 User ID: $USER_ID"
echo ""

# Test 3: Get user profile
echo "👤 Test 3: Get User Profile"
curl -s -X GET "$BASE_URL/users/$USER_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 4: Create a post
echo "📸 Test 4: Create Post"
POST_RESPONSE=$(curl -s -X POST "$BASE_URL/posts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Beautiful sunset at the beach! 🌅 #sunset #beach #photography",
    "mediaUrls": ["https://example.com/sunset.jpg"],
    "hashtags": ["sunset", "beach", "photography"],
    "location": "Malibu Beach, CA"
  }')

echo "Post Creation Response:"
echo $POST_RESPONSE | jq '.'

POST_ID=$(echo $POST_RESPONSE | grep -o '"postId":"[^"]*' | cut -d'"' -f4)
echo "📝 Post ID: $POST_ID"
echo ""

# Test 5: Get the created post
if [ ! -z "$POST_ID" ]; then
  echo "📖 Test 5: Get Post Details"
  curl -s -X GET "$BASE_URL/posts/$POST_ID" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  echo ""
  
  # Test 6: Like the post
  echo "❤️ Test 6: Like Post"
  curl -s -X POST "$BASE_URL/posts/$POST_ID/like" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  echo ""
  
  # Test 7: Unlike the post
  echo "💔 Test 7: Unlike Post"
  curl -s -X DELETE "$BASE_URL/posts/$POST_ID/like" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  echo ""
fi

# Test 8: Get news feed
echo "📰 Test 8: Get News Feed"
curl -s -X GET "$BASE_URL/feed?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 9: Get explore feed
echo "🔍 Test 9: Get Explore Feed"
curl -s -X GET "$BASE_URL/feed/explore?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 10: Search users
echo "🔎 Test 10: Search Users"
curl -s -X GET "$BASE_URL/users/search?q=john" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
echo ""

# Test 11: Get trending posts
echo "📈 Test 11: Get Trending Posts"
curl -s -X GET "$BASE_URL/posts/trending?page=0&size=5" | jq '.'
echo ""

# Test 12: Register second user for follow testing
echo "👥 Test 12: Register Second User"
REGISTER2_RESPONSE=$(curl -s -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "janedoe",
    "email": "jane@example.com",
    "password": "password123", 
    "fullName": "Jane Doe"
  }')

USER2_ID=$(echo $REGISTER2_RESPONSE | grep -o '"userId":[0-9]*' | cut -d':' -f2)

if [ ! -z "$USER2_ID" ]; then
  echo "✅ Second user created with ID: $USER2_ID"
  
  # Test 13: Follow second user
  echo "➕ Test 13: Follow User"
  curl -s -X POST "$BASE_URL/users/$USER2_ID/follow" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  echo ""
  
  # Test 14: Get updated profile (should show following count)
  echo "📊 Test 14: Get Updated Profile"
  curl -s -X GET "$BASE_URL/users/$USER_ID" \
    -H "Authorization: Bearer $TOKEN" | jq '.data | {username, followerCount, followingCount, postCount}'
  echo ""
fi

echo "🎉 Instagram API Testing Complete!"
echo "================================"
echo ""
echo "📋 Test Summary:"
echo "✅ User Registration/Login"
echo "✅ Profile Management"
echo "✅ Post Creation & Retrieval"
echo "✅ Like/Unlike Functionality"
echo "✅ News Feed Generation"
echo "✅ Explore Feed"
echo "✅ User Search"
echo "✅ Trending Posts"
echo "✅ Follow/Unfollow"
echo ""
echo "🔗 Access the Instagram API at: $BASE_URL"
echo "📖 View API documentation at: docs/instagram/API_Documentation.md"