# Real-World Use Cases

Comprehensive guide to how major companies use Elasticsearch in production, with architecture patterns and implementation details.

## Table of Contents
- [E-commerce Product Search](#e-commerce-product-search)
- [Log Analytics & Monitoring](#log-analytics--monitoring)
- [Security Analytics (SIEM)](#security-analytics-siem)
- [Social Media Search](#social-media-search)
- [Geo-Location Services](#geo-location-services)
- [Content Discovery](#content-discovery)
- [Business Intelligence](#business-intelligence)
- [Fraud Detection](#fraud-detection)

## E-commerce Product Search

### Companies: Amazon, eBay, Walmart, Shopify

### Use Case Overview
Fast, relevant product search with filters, facets, and personalization across millions of products.

### Architecture

```
┌─────────────┐
│   User      │
└──────┬──────┘
       │ Search: "gaming laptop"
       ▼
┌─────────────────────────────────┐
│   Application Layer             │
│   - Query building              │
│   - Personalization             │
│   - A/B testing                 │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│   Elasticsearch Cluster         │
│   - 50+ nodes                   │
│   - 100M+ products              │
│   - Multi-region deployment     │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│   Primary Database              │
│   - PostgreSQL/MySQL            │
│   - Source of truth             │
└─────────────────────────────────┘
```

### Implementation Example

**Index mapping:**
```json
PUT /products
{
  "settings": {
    "number_of_shards": 10,
    "number_of_replicas": 2,
    "analysis": {
      "analyzer": {
        "product_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding", "synonym", "stemmer"]
        }
      },
      "filter": {
        "synonym": {
          "type": "synonym",
          "synonyms": [
            "laptop, notebook, portable computer",
            "phone, mobile, smartphone",
            "tv, television"
          ]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "name": {
        "type": "text",
        "analyzer": "product_analyzer",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": { "type": "completion" }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "product_analyzer"
      },
      "brand": {
        "type": "keyword"
      },
      "category": {
        "type": "keyword"
      },
      "price": { "type": "float" },
      "rating": { "type": "float" },
      "review_count": { "type": "integer" },
      "in_stock": { "type": "boolean" },
      "tags": { "type": "keyword" },
      "created_at": { "type": "date" },
      "popularity_score": { "type": "float" }
    }
  }
}
```

**Search query with filters and facets:**
```json
GET /products/_search
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "gaming laptop",
                "fields": ["name^3", "description^2", "brand", "tags"],
                "type": "best_fields",
                "fuzziness": "AUTO"
              }
            }
          ],
          "filter": [
            { "term": { "in_stock": true } },
            { "range": { "price": { "gte": 500, "lte": 2000 } } },
            { "terms": { "brand": ["Dell", "HP", "Lenovo"] } }
          ],
          "should": [
            { "range": { "rating": { "gte": 4.0 } } }
          ]
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "popularity_score",
            "factor": 1.2,
            "modifier": "log1p"
          }
        },
        {
          "gauss": {
            "created_at": {
              "origin": "now",
              "scale": "30d",
              "decay": 0.5
            }
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply"
    }
  },
  "aggs": {
    "brands": {
      "terms": { "field": "brand", "size": 20 }
    },
    "categories": {
      "terms": { "field": "category", "size": 10 }
    },
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          { "to": 500 },
          { "from": 500, "to": 1000 },
          { "from": 1000, "to": 2000 },
          { "from": 2000 }
        ]
      }
    },
    "avg_rating": {
      "avg": { "field": "rating" }
    }
  },
  "highlight": {
    "fields": {
      "name": {},
      "description": { "fragment_size": 150 }
    }
  },
  "from": 0,
  "size": 20,
  "sort": [
    "_score",
    { "popularity_score": "desc" }
  ]
}
```

### Key Features

1. **Autocomplete/Typeahead:**
```json
GET /products/_search
{
  "suggest": {
    "product-suggest": {
      "prefix": "gam",
      "completion": {
        "field": "name.suggest",
        "size": 10,
        "fuzzy": {
          "fuzziness": "AUTO"
        }
      }
    }
  }
}
```

2. **Personalization:**
```json
{
  "query": {
    "function_score": {
      "query": { "match": { "name": "laptop" } },
      "functions": [
        {
          "filter": { "terms": { "category": ["user_browsing_history"] } },
          "weight": 2
        },
        {
          "filter": { "terms": { "brand": ["user_favorite_brands"] } },
          "weight": 1.5
        }
      ]
    }
  }
}
```

3. **Similar Products (More Like This):**
```json
GET /products/_search
{
  "query": {
    "more_like_this": {
      "fields": ["name", "description", "tags"],
      "like": [
        {
          "_index": "products",
          "_id": "laptop-001"
        }
      ],
      "min_term_freq": 1,
      "max_query_terms": 12
    }
  }
}
```

### Performance Metrics
- **Search latency:** < 50ms (p95)
- **Indexing rate:** 10K-50K products/sec
- **Query throughput:** 5K-10K queries/sec
- **Index size:** 100M products ≈ 500GB

## Log Analytics & Monitoring

### Companies: Netflix, Uber, LinkedIn, Airbnb

### Use Case Overview
Centralized logging, real-time monitoring, and troubleshooting across distributed systems.

### Architecture (ELK Stack)

```
┌──────────────────────────────────────────────────┐
│   Application Servers (1000s)                    │
│   - Microservices                                │
│   - APIs                                         │
│   - Background jobs                              │
└────────┬─────────────────────────────────────────┘
         │ Logs
         ▼
┌──────────────────────────────────────────────────┐
│   Filebeat / Logstash                            │
│   - Parse logs                                   │
│   - Enrich data                                  │
│   - Filter and transform                         │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│   Elasticsearch Cluster                          │
│   - Time-series indices                          │
│   - Hot-warm-cold architecture                   │
│   - 150+ nodes (Netflix scale)                   │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│   Kibana                                         │
│   - Dashboards                                   │
│   - Alerting                                     │
│   - Log exploration                              │
└──────────────────────────────────────────────────┘
```

### Implementation Example

**Index template for logs:**
```json
PUT /_index_template/logs-template
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 5,
      "number_of_replicas": 1,
      "refresh_interval": "5s",
      "index.lifecycle.name": "logs-policy"
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "service": { "type": "keyword" },
        "host": { "type": "keyword" },
        "message": { "type": "text" },
        "trace_id": { "type": "keyword" },
        "user_id": { "type": "keyword" },
        "request_id": { "type": "keyword" },
        "duration_ms": { "type": "integer" },
        "status_code": { "type": "integer" },
        "error": {
          "properties": {
            "type": { "type": "keyword" },
            "message": { "type": "text" },
            "stack_trace": { "type": "text" }
          }
        }
      }
    }
  }
}
```

**Index Lifecycle Management (ILM):**
```json
PUT /_ilm/policy/logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

**Common log queries:**

1. **Error tracking:**
```json
GET /logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "level": "ERROR" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "aggs": {
    "errors_by_service": {
      "terms": { "field": "service", "size": 20 }
    },
    "errors_over_time": {
      "date_histogram": {
        "field": "@timestamp",
        "fixed_interval": "5m"
      }
    }
  }
}
```

2. **Trace analysis:**
```json
GET /logs-*/_search
{
  "query": {
    "term": { "trace_id": "abc123xyz" }
  },
  "sort": [
    { "@timestamp": "asc" }
  ]
}
```

3. **Slow requests:**
```json
GET /logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "range": { "duration_ms": { "gte": 1000 } } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "aggs": {
    "slow_endpoints": {
      "terms": { "field": "endpoint.keyword", "size": 10 },
      "aggs": {
        "avg_duration": {
          "avg": { "field": "duration_ms" }
        }
      }
    }
  }
}
```

### Netflix's Logging Architecture

**Scale:**
- 150+ Elasticsearch clusters
- 3,500+ nodes
- 800TB+ data
- 1.3 trillion events/day

**Key patterns:**
1. **Time-series indices:** `logs-2024-01-15`
2. **Hot-warm-cold architecture:** Recent logs on fast SSDs, old logs on cheaper storage
3. **Cross-cluster search:** Query multiple clusters simultaneously
4. **Alerting:** Watcher for anomaly detection

## Security Analytics (SIEM)

### Companies: Cisco, IBM, Palo Alto Networks

### Use Case Overview
Security Information and Event Management (SIEM) for threat detection, compliance, and incident response.

### Architecture

```
┌──────────────────────────────────────────────────┐
│   Security Data Sources                          │
│   - Firewalls                                    │
│   - IDS/IPS                                      │
│   - Endpoint agents                              │
│   - Network traffic                              │
│   - Authentication logs                          │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│   Logstash / Beats                               │
│   - Parse security events                        │
│   - Enrich with threat intelligence              │
│   - Normalize data                               │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│   Elasticsearch                                  │
│   - Security events index                        │
│   - Machine learning for anomalies               │
│   - Correlation rules                            │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│   Kibana Security                                │
│   - Security dashboards                          │
│   - Alert management                             │
│   - Incident investigation                       │
└──────────────────────────────────────────────────┘
```

### Implementation Example

**Security events mapping:**
```json
PUT /security-events
{
  "mappings": {
    "properties": {
      "@timestamp": { "type": "date" },
      "event_type": { "type": "keyword" },
      "severity": { "type": "keyword" },
      "source_ip": { "type": "ip" },
      "destination_ip": { "type": "ip" },
      "source_port": { "type": "integer" },
      "destination_port": { "type": "integer" },
      "user": { "type": "keyword" },
      "action": { "type": "keyword" },
      "result": { "type": "keyword" },
      "threat_score": { "type": "float" },
      "geo": {
        "properties": {
          "country": { "type": "keyword" },
          "city": { "type": "keyword" },
          "location": { "type": "geo_point" }
        }
      }
    }
  }
}
```

**Threat detection queries:**

1. **Brute force detection:**
```json
GET /security-events/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "event_type": "authentication_failure" } },
        { "range": { "@timestamp": { "gte": "now-5m" } } }
      ]
    }
  },
  "aggs": {
    "failed_logins_by_user": {
      "terms": {
        "field": "user",
        "min_doc_count": 10
      },
      "aggs": {
        "unique_ips": {
          "cardinality": { "field": "source_ip" }
        }
      }
    }
  }
}
```

2. **Anomalous network traffic:**
```json
GET /security-events/_search
{
  "query": {
    "bool": {
      "must": [
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "aggs": {
    "traffic_by_destination": {
      "terms": {
        "field": "destination_ip",
        "size": 100
      },
      "aggs": {
        "total_bytes": {
          "sum": { "field": "bytes" }
        },
        "unique_sources": {
          "cardinality": { "field": "source_ip" }
        }
      }
    }
  }
}
```

3. **Geo-location anomalies:**
```json
GET /security-events/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "user": "john.doe" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "aggs": {
    "login_locations": {
      "terms": { "field": "geo.country" }
    }
  }
}
```

**Machine Learning for anomaly detection:**
```json
PUT _ml/anomaly_detectors/security-anomalies
{
  "analysis_config": {
    "bucket_span": "15m",
    "detectors": [
      {
        "function": "high_count",
        "by_field_name": "source_ip"
      },
      {
        "function": "rare",
        "by_field_name": "destination_port"
      }
    ]
  },
  "data_description": {
    "time_field": "@timestamp"
  }
}
```

## Social Media Search

### Companies: LinkedIn, Twitter, Reddit

### Use Case Overview
Search across posts, profiles, and content with real-time updates and personalization.

### LinkedIn's Implementation

**Scale:**
- 100+ Elasticsearch clusters
- 800M+ members
- Billions of documents
- Sub-100ms search latency

**Use cases:**
1. **People search:** Find professionals by skills, location, company
2. **Job search:** Match candidates to jobs
3. **Content search:** Posts, articles, comments
4. **Company search:** Find companies and insights

**People search example:**
```json
GET /members/_search
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "multi_match": {
                "query": "software engineer",
                "fields": ["headline^3", "skills^2", "experience.title"]
              }
            }
          ],
          "filter": [
            { "term": { "location.country": "US" } },
            { "range": { "connections": { "gte": 500 } } }
          ]
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "profile_views",
            "modifier": "log1p"
          }
        },
        {
          "filter": { "term": { "premium": true } },
          "weight": 1.5
        }
      ]
    }
  },
  "aggs": {
    "by_location": {
      "terms": { "field": "location.city", "size": 20 }
    },
    "by_company": {
      "terms": { "field": "current_company", "size": 20 }
    },
    "by_skills": {
      "terms": { "field": "skills", "size": 50 }
    }
  }
}
```

## Geo-Location Services

### Companies: Uber, Lyft, Tinder, Airbnb

### Use Case Overview
Real-time location-based search and matching.

### Uber's Driver Matching

**Architecture:**
```
┌─────────────┐
│   Rider     │
│   Request   │
└──────┬──────┘
       │ Location: (37.7749, -122.4194)
       ▼
┌─────────────────────────────────┐
│   Elasticsearch                 │
│   - Geo-spatial index           │
│   - Real-time driver locations  │
└──────┬──────────────────────────┘
       │ Find nearby drivers
       ▼
┌─────────────────────────────────┐
│   Matching Algorithm            │
│   - Distance                    │
│   - Rating                      │
│   - Vehicle type                │
└─────────────────────────────────┘
```

**Implementation:**
```json
GET /drivers/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "status": "available" } },
        { "term": { "vehicle_type": "uberx" } }
      ],
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
    },
    { "rating": "desc" }
  ],
  "size": 10
}
```

### Tinder's Location-Based Matching

```json
GET /users/_search
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            { "term": { "gender": "female" } },
            { "range": { "age": { "gte": 25, "lte": 35 } } }
          ],
          "filter": {
            "geo_distance": {
              "distance": "50km",
              "location": {
                "lat": 37.7749,
                "lon": -122.4194
              }
            }
          },
          "must_not": [
            { "terms": { "user_id": ["already_swiped_users"] } }
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "location": {
              "origin": { "lat": 37.7749, "lon": -122.4194 },
              "scale": "10km",
              "decay": 0.5
            }
          }
        },
        {
          "field_value_factor": {
            "field": "popularity_score",
            "modifier": "log1p"
          }
        }
      ]
    }
  }
}
```

## Content Discovery

### Companies: Medium, Pinterest, YouTube

### Use Case Overview
Personalized content recommendations and discovery.

### Medium's Article Recommendations

```json
GET /articles/_search
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "should": [
            {
              "more_like_this": {
                "fields": ["title", "content", "tags"],
                "like": [
                  { "_index": "articles", "_id": "user_reading_history" }
                ],
                "min_term_freq": 1
              }
            },
            {
              "terms": {
                "tags": ["user_interests"]
              }
            }
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "published_at": {
              "origin": "now",
              "scale": "7d",
              "decay": 0.5
            }
          }
        },
        {
          "field_value_factor": {
            "field": "claps",
            "modifier": "log1p",
            "factor": 0.1
          }
        }
      ]
    }
  }
}
```

## Business Intelligence

### Companies: Salesforce, HubSpot, Tableau

### Use Case Overview
Real-time analytics dashboards and business metrics.

**Sales analytics example:**
```json
GET /sales/_search
{
  "size": 0,
  "query": {
    "range": {
      "date": {
        "gte": "2024-01-01",
        "lte": "2024-12-31"
      }
    }
  },
  "aggs": {
    "revenue_over_time": {
      "date_histogram": {
        "field": "date",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_revenue": {
          "sum": { "field": "amount" }
        }
      }
    },
    "top_products": {
      "terms": {
        "field": "product_id",
        "size": 10
      },
      "aggs": {
        "revenue": {
          "sum": { "field": "amount" }
        }
      }
    },
    "sales_by_region": {
      "terms": { "field": "region" },
      "aggs": {
        "revenue": {
          "sum": { "field": "amount" }
        }
      }
    }
  }
}
```

## Fraud Detection

### Companies: PayPal, Stripe, Square

### Use Case Overview
Real-time fraud detection using pattern matching and anomaly detection.

**Fraud detection query:**
```json
GET /transactions/_search
{
  "query": {
    "bool": {
      "must": [
        { "range": { "@timestamp": { "gte": "now-5m" } } }
      ],
      "should": [
        {
          "script": {
            "script": {
              "source": "doc['amount'].value > params.threshold",
              "params": { "threshold": 10000 }
            }
          }
        },
        {
          "term": { "country": "high_risk_country" }
        }
      ],
      "minimum_should_match": 1
    }
  },
  "aggs": {
    "suspicious_users": {
      "terms": {
        "field": "user_id",
        "min_doc_count": 5
      },
      "aggs": {
        "total_amount": {
          "sum": { "field": "amount" }
        },
        "unique_merchants": {
          "cardinality": { "field": "merchant_id" }
        }
      }
    }
  }
}
```

## Summary Table

| Use Case | Companies | Scale | Key Features |
|----------|-----------|-------|--------------|
| **E-commerce Search** | Amazon, eBay | 100M+ products | Facets, autocomplete, personalization |
| **Log Analytics** | Netflix, Uber | 1T+ events/day | Time-series, ILM, hot-warm-cold |
| **Security (SIEM)** | Cisco, IBM | Billions of events | Threat detection, ML anomalies |
| **Social Media** | LinkedIn, Twitter | Billions of docs | Real-time, personalization |
| **Geo-Location** | Uber, Tinder | Millions of locations | Geo-spatial, real-time matching |
| **Content Discovery** | Medium, Pinterest | Millions of articles | Recommendations, ML |
| **Business Intelligence** | Salesforce, Tableau | Millions of records | Real-time dashboards, aggregations |
| **Fraud Detection** | PayPal, Stripe | Millions of transactions | Pattern matching, anomaly detection |

---

**Previous**: [← Elastic Stack](09_Elastic_Stack.md) | **Next**: [Integration Examples →](11_Integration_Examples.md)
