# MongoDB Complete Guide - Part 1: Fundamentals & CRUD Operations

## 📋 Table of Contents
1. [Introduction](#introduction)
2. [Installation & Setup](#installation--setup)
3. [Database & Collections](#database--collections)
4. [CRUD Operations](#crud-operations)
5. [Query Operators](#query-operators)
6. [Update Operators](#update-operators)

---

## Introduction

MongoDB is a NoSQL document database designed for scalability and developer agility. Unlike relational databases, MongoDB stores data in flexible, JSON-like documents.

### Key Features
- **Document-Oriented**: Store data in BSON (Binary JSON) format
- **Schema-less**: No predefined schema required
- **Horizontal Scalability**: Built-in sharding support
- **High Availability**: Replica sets for automatic failover
- **Rich Query Language**: Powerful aggregation framework

### When to Use MongoDB
✅ Flexible schema requirements  
✅ Rapid development and iteration  
✅ Horizontal scaling needs  
✅ Real-time analytics  
✅ Content management systems  

❌ Complex transactions (use PostgreSQL)  
❌ Strong ACID requirements across multiple documents  
❌ Heavy JOIN operations  

---

## Installation & Setup

### Using Docker (Recommended)
```bash
# Pull MongoDB image
docker pull mongo:7.0

# Run MongoDB container
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password \
  -v mongodb_data:/data/db \
  mongo:7.0

# Connect to MongoDB shell
docker exec -it mongodb mongosh -u admin -p password
```

### Using Homebrew (macOS)
```bash
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0
mongosh
```

### Connection String
```javascript
// Local connection
mongodb://localhost:27017

// With authentication
mongodb://admin:password@localhost:27017

// MongoDB Atlas (Cloud)
mongodb+srv://username:password@cluster.mongodb.net/mydb
```

---

## Database & Collections

### Create/Switch Database
```javascript
// Switch to database (creates if doesn't exist)
use ecommerce

// Show current database
db

// List all databases
show dbs

// Drop database
db.dropDatabase()
```

### Collections
```javascript
// Create collection explicitly
db.createCollection("users")

// Create with options
db.createCollection("orders", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "items", "totalAmount"],
      properties: {
        userId: { bsonType: "string" },
        items: { bsonType: "array" },
        totalAmount: { bsonType: "number", minimum: 0 }
      }
    }
  }
})

// List collections
show collections

// Drop collection
db.users.drop()
```

---

## CRUD Operations

### Insert Documents

```javascript
// Insert single document
db.users.insertOne({
  name: "John Doe",
  email: "john@example.com",
  age: 30,
  address: {
    street: "123 Main St",
    city: "New York",
    zipCode: "10001"
  },
  interests: ["coding", "music"],
  createdAt: new Date()
})

// Insert multiple documents
db.users.insertMany([
  {
    name: "Jane Smith",
    email: "jane@example.com",
    age: 28,
    interests: ["reading", "travel"]
  },
  {
    name: "Bob Johnson",
    email: "bob@example.com",
    age: 35,
    interests: ["sports", "cooking"]
  }
])

// Insert with custom _id
db.products.insertOne({
  _id: "PROD-001",
  name: "Laptop",
  price: 999.99,
  stock: 50
})
```

### Read Documents

```javascript
// Find all documents
db.users.find()

// Find with pretty print
db.users.find().pretty()

// Find one document
db.users.findOne({ email: "john@example.com" })

// Find with conditions
db.users.find({ age: { $gte: 30 } })

// Find with projection (select specific fields)
db.users.find(
  { age: { $gte: 30 } },
  { name: 1, email: 1, _id: 0 }
)

// Count documents
db.users.countDocuments({ age: { $gte: 30 } })

// Limit and skip (pagination)
db.users.find().limit(10).skip(20)

// Sort documents
db.users.find().sort({ age: -1 }) // descending
db.users.find().sort({ name: 1 })  // ascending
```

### Update Documents

```javascript
// Update single document
db.users.updateOne(
  { email: "john@example.com" },
  { $set: { age: 31, updatedAt: new Date() } }
)

// Update multiple documents
db.users.updateMany(
  { age: { $lt: 30 } },
  { $set: { category: "young" } }
)

// Replace entire document
db.users.replaceOne(
  { email: "john@example.com" },
  {
    name: "John Doe",
    email: "john@example.com",
    age: 31,
    status: "active"
  }
)

// Upsert (update or insert)
db.users.updateOne(
  { email: "new@example.com" },
  { $set: { name: "New User", age: 25 } },
  { upsert: true }
)

// Find and modify
db.users.findOneAndUpdate(
  { email: "john@example.com" },
  { $inc: { age: 1 } },
  { returnDocument: "after" }
)
```

### Delete Documents

```javascript
// Delete single document
db.users.deleteOne({ email: "john@example.com" })

// Delete multiple documents
db.users.deleteMany({ age: { $lt: 18 } })

// Find and delete
db.users.findOneAndDelete({ email: "john@example.com" })

// Delete all documents in collection
db.users.deleteMany({})
```

---

## Query Operators

### Comparison Operators

```javascript
// $eq - Equal to
db.products.find({ price: { $eq: 999.99 } })

// $ne - Not equal to
db.products.find({ status: { $ne: "discontinued" } })

// $gt, $gte - Greater than, Greater than or equal
db.products.find({ price: { $gt: 100 } })
db.products.find({ stock: { $gte: 10 } })

// $lt, $lte - Less than, Less than or equal
db.products.find({ price: { $lt: 50 } })
db.products.find({ stock: { $lte: 5 } })

// $in - Match any value in array
db.products.find({ category: { $in: ["electronics", "computers"] } })

// $nin - Not in array
db.products.find({ status: { $nin: ["discontinued", "out-of-stock"] } })
```

### Logical Operators

```javascript
// $and - All conditions must be true
db.products.find({
  $and: [
    { price: { $gte: 100 } },
    { stock: { $gt: 0 } }
  ]
})

// Implicit AND (same as above)
db.products.find({ price: { $gte: 100 }, stock: { $gt: 0 } })

// $or - At least one condition must be true
db.products.find({
  $or: [
    { category: "electronics" },
    { price: { $lt: 50 } }
  ]
})

// $nor - None of the conditions should be true
db.products.find({
  $nor: [
    { status: "discontinued" },
    { stock: 0 }
  ]
})

// $not - Negates the condition
db.products.find({ price: { $not: { $gt: 100 } } })
```

### Element Operators

```javascript
// $exists - Check if field exists
db.users.find({ phone: { $exists: true } })

// $type - Check field type
db.users.find({ age: { $type: "number" } })
db.users.find({ age: { $type: ["number", "string"] } })
```

### Array Operators

```javascript
// $all - Array contains all specified elements
db.users.find({ interests: { $all: ["coding", "music"] } })

// $elemMatch - Array element matches all conditions
db.orders.find({
  items: {
    $elemMatch: { price: { $gt: 100 }, quantity: { $gte: 2 } }
  }
})

// $size - Array has specific length
db.users.find({ interests: { $size: 3 } })
```

### String Operators

```javascript
// $regex - Pattern matching
db.users.find({ name: { $regex: /^John/i } })
db.users.find({ email: { $regex: "@gmail.com$" } })

// Text search (requires text index)
db.products.createIndex({ name: "text", description: "text" })
db.products.find({ $text: { $search: "laptop computer" } })
```

---

## Update Operators

### Field Update Operators

```javascript
// $set - Set field value
db.users.updateOne(
  { _id: userId },
  { $set: { status: "active", lastLogin: new Date() } }
)

// $unset - Remove field
db.users.updateOne(
  { _id: userId },
  { $unset: { tempField: "" } }
)

// $rename - Rename field
db.users.updateMany(
  {},
  { $rename: { "addr": "address" } }
)

// $inc - Increment numeric value
db.products.updateOne(
  { _id: productId },
  { $inc: { stock: -1, soldCount: 1 } }
)

// $mul - Multiply numeric value
db.products.updateOne(
  { _id: productId },
  { $mul: { price: 1.1 } } // 10% increase
)

// $min - Update if new value is less than current
db.products.updateOne(
  { _id: productId },
  { $min: { lowestPrice: 99.99 } }
)

// $max - Update if new value is greater than current
db.products.updateOne(
  { _id: productId },
  { $max: { highestPrice: 199.99 } }
)

// $currentDate - Set to current date
db.users.updateOne(
  { _id: userId },
  { $currentDate: { lastModified: true } }
)
```

### Array Update Operators

```javascript
// $push - Add element to array
db.users.updateOne(
  { _id: userId },
  { $push: { interests: "photography" } }
)

// $push with $each - Add multiple elements
db.users.updateOne(
  { _id: userId },
  { $push: { interests: { $each: ["gaming", "reading"] } } }
)

// $push with $position - Insert at specific position
db.users.updateOne(
  { _id: userId },
  {
    $push: {
      interests: {
        $each: ["cooking"],
        $position: 0
      }
    }
  }
)

// $addToSet - Add only if not exists (no duplicates)
db.users.updateOne(
  { _id: userId },
  { $addToSet: { interests: "coding" } }
)

// $pop - Remove first or last element
db.users.updateOne(
  { _id: userId },
  { $pop: { interests: 1 } } // 1 for last, -1 for first
)

// $pull - Remove all matching elements
db.users.updateOne(
  { _id: userId },
  { $pull: { interests: "gaming" } }
)

// $pullAll - Remove multiple values
db.users.updateOne(
  { _id: userId },
  { $pullAll: { interests: ["gaming", "sports"] } }
)

// Update array element by position
db.orders.updateOne(
  { _id: orderId },
  { $set: { "items.0.quantity": 5 } }
)

// Update array element with $ positional operator
db.orders.updateOne(
  { _id: orderId, "items.productId": "PROD-001" },
  { $set: { "items.$.quantity": 3 } }
)

// Update all array elements with $[]
db.orders.updateOne(
  { _id: orderId },
  { $inc: { "items.$[].quantity": 1 } }
)

// Update filtered array elements with $[identifier]
db.orders.updateOne(
  { _id: orderId },
  { $set: { "items.$[elem].discount": 10 } },
  { arrayFilters: [{ "elem.price": { $gte: 100 } }] }
)
```

---

## Practical Examples

### E-commerce Product Management

```javascript
// Insert products
db.products.insertMany([
  {
    sku: "LAPTOP-001",
    name: "MacBook Pro 16",
    price: 2499.99,
    stock: 25,
    category: "electronics",
    tags: ["laptop", "apple", "premium"],
    specs: {
      cpu: "M3 Pro",
      ram: "32GB",
      storage: "1TB SSD"
    },
    reviews: [],
    createdAt: new Date()
  },
  {
    sku: "PHONE-001",
    name: "iPhone 15 Pro",
    price: 999.99,
    stock: 50,
    category: "electronics",
    tags: ["phone", "apple", "5g"],
    specs: {
      display: "6.1 inch",
      camera: "48MP",
      battery: "3274mAh"
    },
    reviews: [],
    createdAt: new Date()
  }
])

// Find products in price range
db.products.find({
  price: { $gte: 500, $lte: 1500 },
  stock: { $gt: 0 }
})

// Add review to product
db.products.updateOne(
  { sku: "LAPTOP-001" },
  {
    $push: {
      reviews: {
        userId: "user123",
        rating: 5,
        comment: "Excellent laptop!",
        createdAt: new Date()
      }
    }
  }
)

// Decrease stock after purchase
db.products.updateOne(
  { sku: "LAPTOP-001", stock: { $gte: 1 } },
  { $inc: { stock: -1 } }
)

// Find products by tag
db.products.find({ tags: "apple" })

// Search products by name
db.products.find({ name: { $regex: /macbook/i } })
```

### User Management

```javascript
// Create user with profile
db.users.insertOne({
  email: "john@example.com",
  password: "$2b$10$hashed_password",
  profile: {
    firstName: "John",
    lastName: "Doe",
    phone: "+1234567890",
    avatar: "https://cdn.example.com/avatars/john.jpg"
  },
  addresses: [
    {
      type: "home",
      street: "123 Main St",
      city: "New York",
      state: "NY",
      zipCode: "10001",
      isDefault: true
    }
  ],
  preferences: {
    newsletter: true,
    notifications: {
      email: true,
      sms: false,
      push: true
    }
  },
  status: "active",
  createdAt: new Date(),
  lastLogin: null
})

// Update user profile
db.users.updateOne(
  { email: "john@example.com" },
  {
    $set: {
      "profile.phone": "+9876543210",
      lastLogin: new Date()
    }
  }
)

// Add new address
db.users.updateOne(
  { email: "john@example.com" },
  {
    $push: {
      addresses: {
        type: "work",
        street: "456 Office Blvd",
        city: "San Francisco",
        state: "CA",
        zipCode: "94102",
        isDefault: false
      }
    }
  }
)

// Set default address
db.users.updateOne(
  { email: "john@example.com" },
  {
    $set: {
      "addresses.$[].isDefault": false,
      "addresses.$[elem].isDefault": true
    }
  },
  { arrayFilters: [{ "elem.type": "work" }] }
)
```

---

## Next Steps

Continue to [Part 2: Indexing & Performance](./02_MongoDB_Indexing_Performance.md) to learn about:
- Index types and strategies
- Query optimization
- Explain plans
- Performance tuning
