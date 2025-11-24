# Search & Query DSL

Comprehensive guide to Elasticsearch's powerful Query DSL (Domain Specific Language) for searching and filtering data.

## Table of Contents
- [Query Context vs Filter Context](#query-context-vs-filter-context)
- [Full-Text Queries](#full-text-queries)
- [Term-Level Queries](#term-level-queries)
- [Compound Queries](#compound-queries)
- [Geo Queries](#geo-queries)
- [Relevance Scoring](#relevance-scoring)
- [Pagination & Sorting](#pagination--sorting)
- [Highlighting](#highlighting)

## Query Context vs Filter Context

### Query Context
**Answers:** "How well does this document match?"
- Calculates relevance score (_score)
- Slower (scoring overhead)
- Not cached

```json
GET /products/_search
{
  "query": {
    "match": {
      "description": "laptop"
    }
  }
}
```

### Filter Context
**Answers:** "Does this document match?" (Yes/No)
- No scoring (faster)
- Cached automatically
- Use for exact matches, ranges, exists checks

```json
GET /products/_search
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "category": "electronics" } },
        { "range": { "price": { "gte": 100, "lte": 1000 } } }
      ]
    }
  }
}
```

### Performance Comparison

```
Query Context:
- match query on 1M docs: ~50ms
- Calculates score for each match

Filter Context:
- term filter on 1M docs: ~5ms
- Binary yes/no, cached
```

## Full-Text Queries

### 1. Match Query (Most Common)

**Use case:** Full-text search with analysis

```json
GET /products/_search
{
  "query": {
    "match": {
      "description": "gaming laptop"
    }
  }
}
```

**How it works:**
1. Analyzes query: "gaming laptop" → ["gaming", "laptop"]
2. Searches for documents containing either term (OR by default)
3. Scores by relevance

**Match with AND operator:**
```json
{
  "query": {
    "match": {
      "description": {
        "query": "gaming laptop",
        "operator": "and"
      }
    }
  }
}
```

### 2. Match Phrase Query

**Use case:** Exact phrase matching

```json
GET /products/_search
{
  "query": {
    "match_phrase": {
      "description": "high performance laptop"
    }
  }
}
```

**Matches:**
- ✅ "This is a high performance laptop"
- ❌ "This laptop has high performance" (words not in order)

**With slop (word distance tolerance):**
```json
{
  "query": {
    "match_phrase": {
      "description": {
        "query": "high performance",
        "slop": 2
      }
    }
  }
}
```

**Matches:**
- ✅ "high quality performance" (1 word between)
- ✅ "high end gaming performance" (2 words between)
- ❌ "high end quality gaming performance" (3 words between)

### 3. Multi-Match Query

**Use case:** Search across multiple fields

```json
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "laptop",
      "fields": ["name", "description", "brand"]
    }
  }
}
```

**With field boosting:**
```json
{
  "query": {
    "multi_match": {
      "query": "laptop",
      "fields": ["name^3", "description^2", "brand"]
    }
  }
}
```
- `name^3`: 3x importance
- `description^2`: 2x importance
- `brand`: 1x importance

**Types of multi_match:**

```json
// best_fields (default): Best matching field wins
{
  "query": {
    "multi_match": {
      "query": "laptop",
      "fields": ["name", "description"],
      "type": "best_fields"
    }
  }
}

// most_fields: Combine scores from all fields
{
  "query": {
    "multi_match": {
      "query": "laptop",
      "fields": ["name", "description"],
      "type": "most_fields"
    }
  }
}

// phrase: Match phrase across fields
{
  "query": {
    "multi_match": {
      "query": "gaming laptop",
      "fields": ["name", "description"],
      "type": "phrase"
    }
  }
}
```

### 4. Query String Query

**Use case:** Advanced search with operators (like Google search)

```json
GET /products/_search
{
  "query": {
    "query_string": {
      "query": "(laptop OR notebook) AND brand:dell AND price:[500 TO 1000]",
      "default_field": "description"
    }
  }
}
```

**Operators:**
- `AND`, `OR`, `NOT`
- `+` (must match), `-` (must not match)
- `field:value` (field-specific search)
- `[min TO max]` (range)
- `*` (wildcard)
- `~` (fuzzy)
- `"phrase"` (exact phrase)

**Examples:**
```
"laptop"                    # Simple search
"gaming laptop"             # Multiple terms (OR)
"gaming AND laptop"         # Both terms required
"laptop NOT refurbished"    # Exclude refurbished
brand:dell                  # Field-specific
price:[500 TO 1000]         # Range
name:lap*                   # Wildcard
name:laptop~2               # Fuzzy (2 edits)
"gaming laptop"~3           # Phrase with slop
```

### 5. Fuzzy Query

**Use case:** Typo tolerance

```json
GET /products/_search
{
  "query": {
    "fuzzy": {
      "name": {
        "value": "laptp",
        "fuzziness": "AUTO"
      }
    }
  }
}
```

**Fuzziness levels:**
- `0`: No typos allowed
- `1`: 1 character edit (insert, delete, substitute)
- `2`: 2 character edits
- `AUTO`: Automatic based on term length
  - 0-2 chars: 0 edits
  - 3-5 chars: 1 edit
  - 6+ chars: 2 edits

**Examples:**
```
Query: "laptop" (fuzziness=1)
Matches: "laptp", "lapto", "laptap", "loptop"

Query: "laptop" (fuzziness=2)
Matches: "laptp", "lapto", "lptop", "lappop"
```

## Term-Level Queries

### 1. Term Query

**Use case:** Exact match (not analyzed)

```json
GET /products/_search
{
  "query": {
    "term": {
      "category.keyword": "Electronics"
    }
  }
}
```

**Important:** Use `.keyword` field for exact match on text fields.

### 2. Terms Query

**Use case:** Match any of multiple values (IN clause)

```json
GET /products/_search
{
  "query": {
    "terms": {
      "brand.keyword": ["Dell", "HP", "Lenovo"]
    }
  }
}
```

### 3. Range Query

**Use case:** Numeric or date ranges

```json
GET /products/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 500,
        "lte": 1000
      }
    }
  }
}
```

**Operators:**
- `gte`: Greater than or equal
- `gt`: Greater than
- `lte`: Less than or equal
- `lt`: Less than

**Date range:**
```json
{
  "query": {
    "range": {
      "created_at": {
        "gte": "2024-01-01",
        "lte": "2024-12-31",
        "format": "yyyy-MM-dd"
      }
    }
  }
}
```

**Relative dates:**
```json
{
  "query": {
    "range": {
      "created_at": {
        "gte": "now-7d/d",
        "lte": "now/d"
      }
    }
  }
}
```
- `now`: Current time
- `now-7d`: 7 days ago
- `/d`: Round to day

### 4. Exists Query

**Use case:** Check if field exists

```json
GET /products/_search
{
  "query": {
    "exists": {
      "field": "discount"
    }
  }
}
```

### 5. Prefix Query

**Use case:** Starts with pattern

```json
GET /products/_search
{
  "query": {
    "prefix": {
      "name.keyword": "Mac"
    }
  }
}
```

**Matches:** "MacBook", "Mac Mini", "Mac Pro"

### 6. Wildcard Query

**Use case:** Pattern matching

```json
GET /products/_search
{
  "query": {
    "wildcard": {
      "name.keyword": "Mac*Pro"
    }
  }
}
```

**Patterns:**
- `*`: Zero or more characters
- `?`: Single character

**Examples:**
- `Mac*`: MacBook, Mac Mini
- `Mac?Pro`: Mac Pro, MacXPro
- `*Book`: MacBook, NoteBook

### 7. Regexp Query

**Use case:** Regular expression matching

```json
GET /products/_search
{
  "query": {
    "regexp": {
      "name.keyword": "Mac.*Pro"
    }
  }
}
```

## Compound Queries

### 1. Bool Query (Most Important)

**Use case:** Combine multiple queries with boolean logic

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "description": "laptop" } }
      ],
      "filter": [
        { "term": { "category": "electronics" } },
        { "range": { "price": { "gte": 500, "lte": 1000 } } }
      ],
      "should": [
        { "term": { "brand": "Dell" } },
        { "term": { "brand": "HP" } }
      ],
      "must_not": [
        { "term": { "condition": "refurbished" } }
      ],
      "minimum_should_match": 1
    }
  }
}
```

**Clauses:**

| Clause | Behavior | Scoring | Use Case |
|--------|----------|---------|----------|
| `must` | Document MUST match | ✅ Yes | Required conditions with scoring |
| `filter` | Document MUST match | ❌ No | Required conditions without scoring (faster) |
| `should` | Document SHOULD match | ✅ Yes | Optional conditions (boost score) |
| `must_not` | Document MUST NOT match | ❌ No | Exclusions |

**Real-world example: E-commerce search**

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "gaming laptop",
            "fields": ["name^3", "description"]
          }
        }
      ],
      "filter": [
        { "term": { "in_stock": true } },
        { "range": { "price": { "gte": 800, "lte": 2000 } } },
        { "term": { "category": "computers" } }
      ],
      "should": [
        { "term": { "brand": "Dell" } },
        { "term": { "brand": "HP" } },
        { "range": { "rating": { "gte": 4.5 } } }
      ],
      "must_not": [
        { "term": { "condition": "refurbished" } }
      ],
      "minimum_should_match": 1
    }
  }
}
```

**Translation:**
- MUST: Contains "gaming laptop" in name or description
- FILTER: In stock, price $800-$2000, category is computers
- SHOULD: Prefer Dell or HP, or rating >= 4.5 (at least 1)
- MUST NOT: Exclude refurbished items

### 2. Boosting Query

**Use case:** Demote documents without excluding them

```json
GET /products/_search
{
  "query": {
    "boosting": {
      "positive": {
        "match": { "description": "laptop" }
      },
      "negative": {
        "term": { "condition": "refurbished" }
      },
      "negative_boost": 0.3
    }
  }
}
```

**Effect:** Refurbished laptops still appear but with 30% of original score.

### 3. Constant Score Query

**Use case:** Wrap filter in query context with fixed score

```json
GET /products/_search
{
  "query": {
    "constant_score": {
      "filter": {
        "term": { "category": "electronics" }
      },
      "boost": 1.2
    }
  }
}
```

## Geo Queries

### 1. Geo Distance Query

**Use case:** Find locations within radius

```json
GET /stores/_search
{
  "query": {
    "bool": {
      "filter": {
        "geo_distance": {
          "distance": "5km",
          "location": {
            "lat": 37.7749,
            "lon": -122.4194
          }
        }
      }
    }
  }
}
```

**Distance units:**
- `km`: Kilometers
- `mi`: Miles
- `m`: Meters

### 2. Geo Bounding Box Query

**Use case:** Find locations within rectangle

```json
GET /stores/_search
{
  "query": {
    "bool": {
      "filter": {
        "geo_bounding_box": {
          "location": {
            "top_left": {
              "lat": 40.73,
              "lon": -74.1
            },
            "bottom_right": {
              "lat": 40.01,
              "lon": -71.12
            }
          }
        }
      }
    }
  }
}
```

### 3. Geo Polygon Query

**Use case:** Find locations within custom polygon

```json
GET /stores/_search
{
  "query": {
    "bool": {
      "filter": {
        "geo_polygon": {
          "location": {
            "points": [
              { "lat": 40.73, "lon": -74.1 },
              { "lat": 40.01, "lon": -71.12 },
              { "lat": 38.5, "lon": -73.0 }
            ]
          }
        }
      }
    }
  }
}
```

**Real-world example: Uber driver search**

```json
GET /drivers/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "geo_distance": {
            "distance": "2km",
            "location": {
              "lat": 37.7749,
              "lon": -122.4194
            }
          }
        },
        { "term": { "status": "available" } },
        { "term": { "vehicle_type": "sedan" } }
      ]
    }
  },
  "sort": [
    {
      "_geo_distance": {
        "location": {
          "lat": 37.7749,
          "lon": -122.4194
        },
        "order": "asc",
        "unit": "km"
      }
    }
  ]
}
```

## Relevance Scoring

### TF-IDF (Term Frequency-Inverse Document Frequency)

**Formula:**
```
score = TF × IDF × field_norm

TF (Term Frequency): How often term appears in document
IDF (Inverse Document Frequency): How rare term is across all documents
field_norm: Normalization factor (shorter fields score higher)
```

**Example:**

```
Document 1: "laptop laptop laptop"
Document 2: "laptop"
Document 3: "computer"

Query: "laptop"

TF scores:
- Doc 1: 3 (appears 3 times)
- Doc 2: 1 (appears 1 time)
- Doc 3: 0 (doesn't appear)

IDF: log(3 / 2) = 0.176
(3 total docs, 2 contain "laptop")

Final scores:
- Doc 1: 3 × 0.176 × norm = higher
- Doc 2: 1 × 0.176 × norm = lower
```

### BM25 (Best Match 25) - Default in ES 5.0+

**Improved scoring algorithm:**
- Diminishing returns for term frequency
- Considers document length
- Tunable parameters (k1, b)

```json
PUT /products
{
  "settings": {
    "index": {
      "similarity": {
        "default": {
          "type": "BM25",
          "k1": 1.2,
          "b": 0.75
        }
      }
    }
  }
}
```

### Explain API

**See why document scored the way it did:**

```json
GET /products/_explain/1
{
  "query": {
    "match": {
      "description": "laptop"
    }
  }
}
```

**Response:**
```json
{
  "matched": true,
  "explanation": {
    "value": 2.3,
    "description": "sum of:",
    "details": [
      {
        "value": 1.5,
        "description": "weight(description:laptop in 0) [BM25]"
      },
      {
        "value": 0.8,
        "description": "field norm"
      }
    ]
  }
}
```

### Function Score Query

**Use case:** Custom scoring logic

```json
GET /products/_search
{
  "query": {
    "function_score": {
      "query": {
        "match": { "description": "laptop" }
      },
      "functions": [
        {
          "filter": { "term": { "featured": true } },
          "weight": 2
        },
        {
          "field_value_factor": {
            "field": "popularity",
            "factor": 1.2,
            "modifier": "log1p"
          }
        },
        {
          "gauss": {
            "price": {
              "origin": "1000",
              "scale": "200"
            }
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply"
    }
  }
}
```

**Functions:**
- `weight`: Multiply score by constant
- `field_value_factor`: Use field value in scoring
- `gauss/exp/linear`: Decay functions
- `random_score`: Randomize results
- `script_score`: Custom script

## Pagination & Sorting

### From/Size Pagination

```json
GET /products/_search
{
  "from": 0,
  "size": 20,
  "query": {
    "match": { "category": "electronics" }
  }
}
```

**Limitations:**
- Max `from + size` = 10,000 (default)
- Deep pagination is expensive

### Search After (Recommended for Deep Pagination)

```json
GET /products/_search
{
  "size": 20,
  "query": {
    "match": { "category": "electronics" }
  },
  "sort": [
    { "price": "asc" },
    { "_id": "asc" }
  ]
}

// Next page using last document's sort values
GET /products/_search
{
  "size": 20,
  "query": {
    "match": { "category": "electronics" }
  },
  "search_after": [899.99, "doc_123"],
  "sort": [
    { "price": "asc" },
    { "_id": "asc" }
  ]
}
```

### Scroll API (For Exporting Data)

```json
// Initial request
POST /products/_search?scroll=1m
{
  "size": 1000,
  "query": {
    "match_all": {}
  }
}

// Subsequent requests
POST /_search/scroll
{
  "scroll": "1m",
  "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAD4WYm9laVYtZndUQlNsdDcwakFMNjU1QQ=="
}
```

### Sorting

```json
GET /products/_search
{
  "query": {
    "match": { "category": "electronics" }
  },
  "sort": [
    { "price": { "order": "asc" } },
    { "rating": { "order": "desc" } },
    "_score"
  ]
}
```

**Sort by geo distance:**
```json
{
  "sort": [
    {
      "_geo_distance": {
        "location": {
          "lat": 37.7749,
          "lon": -122.4194
        },
        "order": "asc",
        "unit": "km"
      }
    }
  ]
}
```

## Highlighting

**Use case:** Show matching text snippets

```json
GET /products/_search
{
  "query": {
    "match": {
      "description": "gaming laptop"
    }
  },
  "highlight": {
    "fields": {
      "description": {}
    }
  }
}
```

**Response:**
```json
{
  "hits": {
    "hits": [
      {
        "_source": {
          "description": "High performance gaming laptop with RTX 4090"
        },
        "highlight": {
          "description": [
            "High performance <em>gaming</em> <em>laptop</em> with RTX 4090"
          ]
        }
      }
    ]
  }
}
```

**Custom tags:**
```json
{
  "highlight": {
    "pre_tags": ["<strong>"],
    "post_tags": ["</strong>"],
    "fields": {
      "description": {
        "fragment_size": 150,
        "number_of_fragments": 3
      }
    }
  }
}
```

---

**Previous**: [← Indexing](03_Indexing.md) | **Next**: [Aggregations →](05_Aggregations.md)
