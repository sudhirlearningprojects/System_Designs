# MongoDB Complete Guide - Part 3: Aggregation Framework

## 📋 Table of Contents
1. [Aggregation Basics](#aggregation-basics)
2. [Pipeline Stages](#pipeline-stages)
3. [Aggregation Operators](#aggregation-operators)
4. [Real-World Examples](#real-world-examples)
5. [Performance Tips](#performance-tips)

---

## Aggregation Basics

Aggregation framework processes data records and returns computed results. Think of it as SQL GROUP BY on steroids.

### Basic Syntax
```javascript
db.collection.aggregate([
  { stage1 },
  { stage2 },
  { stage3 }
])
```

### Simple Example
```javascript
// Calculate total sales by user
db.orders.aggregate([
  { $match: { status: "completed" } },
  { $group: {
      _id: "$userId",
      totalAmount: { $sum: "$amount" },
      orderCount: { $sum: 1 }
  }}
])
```

---

## Pipeline Stages

### $match - Filter Documents
```javascript
// Filter before processing (uses indexes)
db.orders.aggregate([
  { $match: {
      status: "completed",
      createdAt: { $gte: ISODate("2024-01-01") }
  }}
])

// Multiple conditions
db.orders.aggregate([
  { $match: {
      $and: [
        { amount: { $gte: 100 } },
        { status: { $in: ["completed", "shipped"] } }
      ]
  }}
])
```

### $project - Select/Transform Fields
```javascript
// Select specific fields
db.users.aggregate([
  { $project: {
      name: 1,
      email: 1,
      _id: 0
  }}
])

// Computed fields
db.orders.aggregate([
  { $project: {
      orderId: "$_id",
      total: { $multiply: ["$price", "$quantity"] },
      year: { $year: "$createdAt" }
  }}
])

// Nested fields
db.users.aggregate([
  { $project: {
      fullName: { $concat: ["$profile.firstName", " ", "$profile.lastName"] },
      city: "$address.city"
  }}
])
```

### $group - Group and Aggregate
```javascript
// Group by single field
db.orders.aggregate([
  { $group: {
      _id: "$userId",
      totalSpent: { $sum: "$amount" },
      avgOrderValue: { $avg: "$amount" },
      orderCount: { $sum: 1 },
      maxOrder: { $max: "$amount" },
      minOrder: { $min: "$amount" }
  }}
])

// Group by multiple fields
db.orders.aggregate([
  { $group: {
      _id: {
        userId: "$userId",
        status: "$status"
      },
      count: { $sum: 1 }
  }}
])

// Group all documents
db.orders.aggregate([
  { $group: {
      _id: null,
      totalRevenue: { $sum: "$amount" },
      avgOrderValue: { $avg: "$amount" }
  }}
])

// Collect values into array
db.orders.aggregate([
  { $group: {
      _id: "$userId",
      orderIds: { $push: "$_id" },
      uniqueStatuses: { $addToSet: "$status" }
  }}
])
```

### $sort - Sort Documents
```javascript
// Sort ascending
db.orders.aggregate([
  { $sort: { createdAt: 1 } }
])

// Sort descending
db.orders.aggregate([
  { $sort: { amount: -1 } }
])

// Sort by multiple fields
db.orders.aggregate([
  { $sort: { status: 1, createdAt: -1 } }
])
```

### $limit & $skip - Pagination
```javascript
// Limit results
db.orders.aggregate([
  { $sort: { createdAt: -1 } },
  { $limit: 10 }
])

// Skip and limit (pagination)
db.orders.aggregate([
  { $sort: { createdAt: -1 } },
  { $skip: 20 },
  { $limit: 10 }
])
```

### $lookup - Join Collections
```javascript
// Left outer join
db.orders.aggregate([
  {
    $lookup: {
      from: "users",
      localField: "userId",
      foreignField: "_id",
      as: "userDetails"
    }
  }
])

// Unwind joined array
db.orders.aggregate([
  {
    $lookup: {
      from: "users",
      localField: "userId",
      foreignField: "_id",
      as: "userDetails"
    }
  },
  { $unwind: "$userDetails" }
])

// Complex join with pipeline
db.orders.aggregate([
  {
    $lookup: {
      from: "products",
      let: { orderItems: "$items" },
      pipeline: [
        { $match: {
            $expr: { $in: ["$_id", "$$orderItems.productId"] }
        }},
        { $project: { name: 1, price: 1 } }
      ],
      as: "productDetails"
    }
  }
])
```

### $unwind - Deconstruct Array
```javascript
// Unwind array field
db.orders.aggregate([
  { $unwind: "$items" }
])

// Before unwind
{ _id: 1, items: ["A", "B", "C"] }

// After unwind
{ _id: 1, items: "A" }
{ _id: 1, items: "B" }
{ _id: 1, items: "C" }

// Preserve null and empty arrays
db.orders.aggregate([
  { $unwind: {
      path: "$items",
      preserveNullAndEmptyArrays: true
  }}
])
```

### $addFields - Add New Fields
```javascript
// Add computed fields
db.orders.aggregate([
  { $addFields: {
      totalPrice: { $multiply: ["$price", "$quantity"] },
      year: { $year: "$createdAt" }
  }}
])

// Add conditional fields
db.users.aggregate([
  { $addFields: {
      ageGroup: {
        $switch: {
          branches: [
            { case: { $lt: ["$age", 18] }, then: "minor" },
            { case: { $lt: ["$age", 65] }, then: "adult" }
          ],
          default: "senior"
        }
      }
  }}
])
```

### $bucket - Group by Ranges
```javascript
// Group by price ranges
db.products.aggregate([
  {
    $bucket: {
      groupBy: "$price",
      boundaries: [0, 50, 100, 200, 500],
      default: "500+",
      output: {
        count: { $sum: 1 },
        products: { $push: "$name" }
      }
    }
  }
])
```

### $facet - Multiple Pipelines
```javascript
// Run multiple aggregations in parallel
db.products.aggregate([
  {
    $facet: {
      priceStats: [
        { $group: {
            _id: null,
            avgPrice: { $avg: "$price" },
            maxPrice: { $max: "$price" }
        }}
      ],
      categoryCount: [
        { $group: { _id: "$category", count: { $sum: 1 } }}
      ],
      topProducts: [
        { $sort: { sales: -1 } },
        { $limit: 5 }
      ]
    }
  }
])
```

---

## Aggregation Operators

### Arithmetic Operators
```javascript
db.orders.aggregate([
  { $project: {
      total: { $add: ["$price", "$tax"] },
      discount: { $subtract: ["$price", "$discountAmount"] },
      totalPrice: { $multiply: ["$price", "$quantity"] },
      pricePerUnit: { $divide: ["$totalPrice", "$quantity"] },
      remainder: { $mod: ["$quantity", 10] }
  }}
])
```

### Comparison Operators
```javascript
db.products.aggregate([
  { $project: {
      name: 1,
      isExpensive: { $gte: ["$price", 1000] },
      isPremium: { $and: [
          { $gte: ["$price", 500] },
          { $eq: ["$category", "electronics"] }
      ]}
  }}
])
```

### String Operators
```javascript
db.users.aggregate([
  { $project: {
      fullName: { $concat: ["$firstName", " ", "$lastName"] },
      upperName: { $toUpper: "$name" },
      lowerEmail: { $toLower: "$email" },
      nameLength: { $strLenCP: "$name" },
      firstName: { $substr: ["$name", 0, 5] },
      domain: { $arrayElemAt: [{ $split: ["$email", "@"] }, 1] }
  }}
])
```

### Date Operators
```javascript
db.orders.aggregate([
  { $project: {
      year: { $year: "$createdAt" },
      month: { $month: "$createdAt" },
      day: { $dayOfMonth: "$createdAt" },
      dayOfWeek: { $dayOfWeek: "$createdAt" },
      hour: { $hour: "$createdAt" },
      dateString: { $dateToString: {
          format: "%Y-%m-%d",
          date: "$createdAt"
      }},
      ageInDays: { $divide: [
          { $subtract: [new Date(), "$createdAt"] },
          1000 * 60 * 60 * 24
      ]}
  }}
])
```

### Array Operators
```javascript
db.orders.aggregate([
  { $project: {
      itemCount: { $size: "$items" },
      firstItem: { $arrayElemAt: ["$items", 0] },
      lastItem: { $arrayElemAt: ["$items", -1] },
      itemSlice: { $slice: ["$items", 2] },
      hasElectronics: { $in: ["electronics", "$categories"] },
      allTags: { $concatArrays: ["$tags", "$categories"] }
  }}
])
```

### Conditional Operators
```javascript
db.products.aggregate([
  { $project: {
      name: 1,
      priceCategory: {
        $cond: {
          if: { $gte: ["$price", 1000] },
          then: "expensive",
          else: "affordable"
        }
      },
      status: {
        $switch: {
          branches: [
            { case: { $eq: ["$stock", 0] }, then: "out-of-stock" },
            { case: { $lt: ["$stock", 10] }, then: "low-stock" },
            { case: { $gte: ["$stock", 10] }, then: "in-stock" }
          ],
          default: "unknown"
        }
      },
      displayPrice: { $ifNull: ["$salePrice", "$price"] }
  }}
])
```

---

## Real-World Examples

### 1. Sales Analytics Dashboard
```javascript
db.orders.aggregate([
  // Filter completed orders from last 30 days
  { $match: {
      status: "completed",
      createdAt: { $gte: new Date(Date.now() - 30*24*60*60*1000) }
  }},
  
  // Add computed fields
  { $addFields: {
      month: { $month: "$createdAt" },
      dayOfWeek: { $dayOfWeek: "$createdAt" }
  }},
  
  // Group by date
  { $group: {
      _id: {
        year: { $year: "$createdAt" },
        month: "$month",
        day: { $dayOfMonth: "$createdAt" }
      },
      revenue: { $sum: "$amount" },
      orderCount: { $sum: 1 },
      avgOrderValue: { $avg: "$amount" },
      uniqueCustomers: { $addToSet: "$userId" }
  }},
  
  // Add customer count
  { $addFields: {
      customerCount: { $size: "$uniqueCustomers" }
  }},
  
  // Sort by date
  { $sort: { "_id.year": 1, "_id.month": 1, "_id.day": 1 } },
  
  // Format output
  { $project: {
      _id: 0,
      date: {
        $dateFromParts: {
          year: "$_id.year",
          month: "$_id.month",
          day: "$_id.day"
        }
      },
      revenue: 1,
      orderCount: 1,
      avgOrderValue: { $round: ["$avgOrderValue", 2] },
      customerCount: 1
  }}
])
```

### 2. Product Recommendations
```javascript
// Find products frequently bought together
db.orders.aggregate([
  // Unwind items
  { $unwind: "$items" },
  
  // Group by product
  { $group: {
      _id: "$items.productId",
      orders: { $addToSet: "$_id" }
  }},
  
  // Self-lookup for co-purchased products
  { $lookup: {
      from: "orders",
      let: { orderIds: "$orders" },
      pipeline: [
        { $match: { $expr: { $in: ["$_id", "$$orderIds"] } }},
        { $unwind: "$items" },
        { $group: {
            _id: "$items.productId",
            count: { $sum: 1 }
        }},
        { $sort: { count: -1 } },
        { $limit: 5 }
      ],
      as: "relatedProducts"
  }},
  
  // Lookup product details
  { $lookup: {
      from: "products",
      localField: "_id",
      foreignField: "_id",
      as: "product"
  }},
  
  { $unwind: "$product" },
  
  { $project: {
      productName: "$product.name",
      relatedProducts: 1
  }}
])
```

### 3. User Segmentation
```javascript
db.users.aggregate([
  // Lookup user orders
  { $lookup: {
      from: "orders",
      localField: "_id",
      foreignField: "userId",
      as: "orders"
  }},
  
  // Calculate metrics
  { $addFields: {
      totalOrders: { $size: "$orders" },
      totalSpent: { $sum: "$orders.amount" },
      avgOrderValue: { $avg: "$orders.amount" },
      lastOrderDate: { $max: "$orders.createdAt" }
  }},
  
  // Calculate days since last order
  { $addFields: {
      daysSinceLastOrder: {
        $divide: [
          { $subtract: [new Date(), "$lastOrderDate"] },
          1000 * 60 * 60 * 24
        ]
      }
  }},
  
  // Segment users
  { $addFields: {
      segment: {
        $switch: {
          branches: [
            {
              case: {
                $and: [
                  { $gte: ["$totalSpent", 1000] },
                  { $lte: ["$daysSinceLastOrder", 30] }
                ]
              },
              then: "VIP"
            },
            {
              case: {
                $and: [
                  { $gte: ["$totalOrders", 5] },
                  { $lte: ["$daysSinceLastOrder", 90] }
                ]
              },
              then: "Loyal"
            },
            {
              case: { $gt: ["$daysSinceLastOrder", 180] },
              then: "Churned"
            }
          ],
          default: "Regular"
        }
      }
  }},
  
  // Group by segment
  { $group: {
      _id: "$segment",
      count: { $sum: 1 },
      avgSpent: { $avg: "$totalSpent" },
      avgOrders: { $avg: "$totalOrders" }
  }},
  
  { $sort: { avgSpent: -1 } }
])
```

### 4. Inventory Report
```javascript
db.products.aggregate([
  // Lookup recent orders
  { $lookup: {
      from: "orders",
      let: { productId: "$_id" },
      pipeline: [
        { $match: {
            createdAt: { $gte: new Date(Date.now() - 30*24*60*60*1000) }
        }},
        { $unwind: "$items" },
        { $match: { $expr: { $eq: ["$items.productId", "$$productId"] } }},
        { $group: {
            _id: null,
            totalSold: { $sum: "$items.quantity" }
        }}
      ],
      as: "salesData"
  }},
  
  // Calculate metrics
  { $addFields: {
      soldLast30Days: {
        $ifNull: [{ $arrayElemAt: ["$salesData.totalSold", 0] }, 0]
      }
  }},
  
  { $addFields: {
      daysUntilStockout: {
        $cond: {
          if: { $gt: ["$soldLast30Days", 0] },
          then: { $divide: [
              { $multiply: ["$stock", 30] },
              "$soldLast30Days"
          ]},
          else: null
        }
      },
      stockStatus: {
        $switch: {
          branches: [
            { case: { $eq: ["$stock", 0] }, then: "OUT_OF_STOCK" },
            { case: { $lt: ["$stock", 10] }, then: "LOW_STOCK" },
            { case: { $gte: ["$stock", 10] }, then: "IN_STOCK" }
          ],
          default: "UNKNOWN"
        }
      }
  }},
  
  // Filter low stock items
  { $match: {
      $or: [
        { stockStatus: "OUT_OF_STOCK" },
        { stockStatus: "LOW_STOCK" },
        { daysUntilStockout: { $lt: 7 } }
      ]
  }},
  
  { $sort: { daysUntilStockout: 1 } },
  
  { $project: {
      name: 1,
      sku: 1,
      stock: 1,
      soldLast30Days: 1,
      daysUntilStockout: { $round: ["$daysUntilStockout", 1] },
      stockStatus: 1
  }}
])
```

---

## Performance Tips

### 1. Filter Early
```javascript
// ❌ BAD: Filter after grouping
db.orders.aggregate([
  { $group: { _id: "$userId", total: { $sum: "$amount" } }},
  { $match: { total: { $gte: 1000 } }}
])

// ✅ GOOD: Filter before grouping
db.orders.aggregate([
  { $match: { status: "completed" } },
  { $group: { _id: "$userId", total: { $sum: "$amount" } }},
  { $match: { total: { $gte: 1000 } }}
])
```

### 2. Use Indexes
```javascript
// Create index for $match stage
db.orders.createIndex({ status: 1, createdAt: -1 })

db.orders.aggregate([
  { $match: { status: "completed" } }, // Uses index
  { $sort: { createdAt: -1 } }         // Uses index
])
```

### 3. Limit Early
```javascript
// ✅ GOOD: Limit before expensive operations
db.orders.aggregate([
  { $match: { status: "completed" } },
  { $sort: { createdAt: -1 } },
  { $limit: 100 },
  { $lookup: { from: "users", ... }}
])
```

### 4. Project Only Needed Fields
```javascript
// ❌ BAD: Process all fields
db.orders.aggregate([
  { $lookup: { from: "users", ... }},
  { $project: { userId: 1, amount: 1 }}
])

// ✅ GOOD: Project early
db.orders.aggregate([
  { $project: { userId: 1, amount: 1 }},
  { $lookup: { from: "users", ... }}
])
```

### 5. Use allowDiskUse for Large Datasets
```javascript
// Enable disk usage for large aggregations
db.orders.aggregate(
  [ /* pipeline */ ],
  { allowDiskUse: true }
)
```

---

## Next Steps

Continue to [Part 4: Transactions & Replication](./04_MongoDB_Transactions_Replication.md)
