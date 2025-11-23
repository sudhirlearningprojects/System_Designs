#!/bin/bash

BASE_URL="http://localhost:8097/api/v1/db"

echo "=== Testing Distributed Database System ==="
echo ""

echo "1. Register Shard 1 (Hash-based)"
curl -X POST $BASE_URL/shards \
  -H "Content-Type: application/json" \
  -d '{
    "shardId": "shard-1",
    "type": "HASH",
    "strategy": "CONSISTENT_HASH",
    "primaryNode": "node-1",
    "replicaNodes": ["node-2", "node-3"],
    "region": "US-EAST"
  }'
echo -e "\n"

echo "2. Register Shard 2 (Geo-based)"
curl -X POST $BASE_URL/shards \
  -H "Content-Type: application/json" \
  -d '{
    "shardId": "shard-2",
    "type": "GEO",
    "strategy": "GEO_PROXIMITY",
    "primaryNode": "node-4",
    "replicaNodes": ["node-5"],
    "region": "EU-WEST"
  }'
echo -e "\n"

echo "3. Get All Shards"
curl -X GET $BASE_URL/shards
echo -e "\n"

echo "4. Execute Query (Single Shard)"
curl -X POST $BASE_URL/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT 1 as test",
    "type": "SELECT",
    "shardingKey": "user123",
    "consistencyLevel": "STRONG"
  }'
echo -e "\n"

echo "5. Begin Transaction"
TXN_ID=$(curl -s -X POST $BASE_URL/transaction/begin | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
echo "Transaction ID: $TXN_ID"
echo ""

echo "6. Execute in Transaction"
curl -X POST $BASE_URL/transaction/$TXN_ID/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "INSERT INTO test VALUES (1)",
    "type": "INSERT",
    "shardingKey": "user123"
  }'
echo -e "\n"

echo "7. Commit Transaction"
curl -X POST $BASE_URL/transaction/$TXN_ID/commit
echo -e "\n"

echo "=== Tests Complete ==="
