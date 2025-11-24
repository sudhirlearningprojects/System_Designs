# Integration Examples

Practical code examples for integrating Elasticsearch with popular programming languages and frameworks.

## Table of Contents
- [Java / Spring Boot](#java--spring-boot)
- [Python](#python)
- [Node.js](#nodejs)
- [Go](#go)
- [.NET / C#](#net--c)
- [PHP](#php)
- [Ruby](#ruby)

## Java / Spring Boot

### Dependencies (Maven)

```xml
<dependencies>
    <!-- Spring Data Elasticsearch -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
    
    <!-- Elasticsearch Java Client -->
    <dependency>
        <groupId>co.elastic.clients</groupId>
        <artifactId>elasticsearch-java</artifactId>
        <version>8.11.0</version>
    </dependency>
</dependencies>
```

### Configuration

```java
@Configuration
public class ElasticsearchConfig {
    
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")
        ).build();
        
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, 
            new JacksonJsonpMapper()
        );
        
        return new ElasticsearchClient(transport);
    }
}
```

### Entity Model

```java
@Document(indexName = "products")
public class Product {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text)
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String brand;
    
    @Field(type = FieldType.Float)
    private Double price;
    
    @Field(type = FieldType.Integer)
    private Integer stock;
    
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    // Getters and setters
}
```

### Repository Interface

```java
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    
    // Method name query
    List<Product> findByName(String name);
    
    List<Product> findByBrand(String brand);
    
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    // Custom query
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}]}}")
    List<Product> searchByName(String name);
}
```

### Service Layer

```java
@Service
public class ProductSearchService {
    
    @Autowired
    private ElasticsearchClient client;
    
    @Autowired
    private ProductRepository repository;
    
    // Index a product
    public Product indexProduct(Product product) {
        return repository.save(product);
    }
    
    // Bulk index
    public void bulkIndex(List<Product> products) {
        repository.saveAll(products);
    }
    
    // Simple search
    public List<Product> searchByName(String name) {
        return repository.findByName(name);
    }
    
    // Full-text search with Java client
    public List<Product> fullTextSearch(String query) throws IOException {
        SearchResponse<Product> response = client.search(s -> s
            .index("products")
            .query(q -> q
                .multiMatch(m -> m
                    .query(query)
                    .fields("name^3", "description^2", "brand")
                )
            )
            .from(0)
            .size(20),
            Product.class
        );
        
        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());
    }
    
    // Complex search with filters
    public List<Product> advancedSearch(String query, String brand, 
                                       Double minPrice, Double maxPrice) throws IOException {
        SearchResponse<Product> response = client.search(s -> s
            .index("products")
            .query(q -> q
                .bool(b -> b
                    .must(m -> m
                        .multiMatch(mm -> mm
                            .query(query)
                            .fields("name", "description")
                        )
                    )
                    .filter(f -> f
                        .term(t -> t
                            .field("brand")
                            .value(brand)
                        )
                    )
                    .filter(f -> f
                        .range(r -> r
                            .field("price")
                            .gte(JsonData.of(minPrice))
                            .lte(JsonData.of(maxPrice))
                        )
                    )
                )
            )
            .aggregations("brands", a -> a
                .terms(t -> t.field("brand"))
            )
            .sort(so -> so
                .score(sc -> sc.order(SortOrder.Desc))
            ),
            Product.class
        );
        
        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());
    }
    
    // Aggregations
    public Map<String, Long> getBrandCounts() throws IOException {
        SearchResponse<Product> response = client.search(s -> s
            .index("products")
            .size(0)
            .aggregations("brands", a -> a
                .terms(t -> t
                    .field("brand")
                    .size(50)
                )
            ),
            Product.class
        );
        
        Map<String, Long> brandCounts = new HashMap<>();
        response.aggregations()
            .get("brands")
            .sterms()
            .buckets()
            .array()
            .forEach(bucket -> 
                brandCounts.put(bucket.key().stringValue(), bucket.docCount())
            );
        
        return brandCounts;
    }
    
    // Autocomplete
    public List<String> autocomplete(String prefix) throws IOException {
        SearchResponse<Product> response = client.search(s -> s
            .index("products")
            .suggest(su -> su
                .suggesters("product-suggest", sg -> sg
                    .prefix(prefix)
                    .completion(c -> c
                        .field("name.suggest")
                        .size(10)
                    )
                )
            ),
            Product.class
        );
        
        return response.suggest()
            .get("product-suggest")
            .get(0)
            .completion()
            .options()
            .stream()
            .map(option -> option.text())
            .collect(Collectors.toList());
    }
}
```

### REST Controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductSearchService searchService;
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = searchService.indexProduct(product);
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(
            @RequestParam String query,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) throws IOException {
        
        List<Product> results = searchService.advancedSearch(
            query, brand, minPrice, maxPrice
        );
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String prefix) 
            throws IOException {
        List<String> suggestions = searchService.autocomplete(prefix);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/brands")
    public ResponseEntity<Map<String, Long>> getBrands() throws IOException {
        Map<String, Long> brands = searchService.getBrandCounts();
        return ResponseEntity.ok(brands);
    }
}
```

## Python

### Installation

```bash
pip install elasticsearch
```

### Basic Usage

```python
from elasticsearch import Elasticsearch
from datetime import datetime

# Connect to Elasticsearch
es = Elasticsearch(
    ['http://localhost:9200'],
    basic_auth=('elastic', 'password')  # Optional
)

# Check connection
print(es.info())

# Index a document
doc = {
    'name': 'Laptop',
    'brand': 'Dell',
    'price': 899.99,
    'description': 'High-performance laptop',
    'tags': ['electronics', 'computers'],
    'created_at': datetime.now()
}

response = es.index(index='products', id='1', document=doc)
print(f"Indexed document: {response['result']}")

# Get a document
doc = es.get(index='products', id='1')
print(doc['_source'])

# Search
query = {
    'query': {
        'match': {
            'description': 'laptop'
        }
    }
}

results = es.search(index='products', body=query)
for hit in results['hits']['hits']:
    print(hit['_source'])
```

### Advanced Search Service

```python
from typing import List, Dict, Optional
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk

class ProductSearchService:
    def __init__(self, es_host: str = 'localhost:9200'):
        self.es = Elasticsearch([f'http://{es_host}'])
        self.index_name = 'products'
    
    def create_index(self):
        """Create index with mappings"""
        mapping = {
            'mappings': {
                'properties': {
                    'name': {
                        'type': 'text',
                        'fields': {
                            'keyword': {'type': 'keyword'},
                            'suggest': {'type': 'completion'}
                        }
                    },
                    'description': {'type': 'text'},
                    'brand': {'type': 'keyword'},
                    'price': {'type': 'float'},
                    'stock': {'type': 'integer'},
                    'tags': {'type': 'keyword'},
                    'created_at': {'type': 'date'}
                }
            }
        }
        
        if not self.es.indices.exists(index=self.index_name):
            self.es.indices.create(index=self.index_name, body=mapping)
    
    def index_product(self, product: Dict) -> Dict:
        """Index a single product"""
        response = self.es.index(
            index=self.index_name,
            document=product
        )
        return response
    
    def bulk_index(self, products: List[Dict]):
        """Bulk index products"""
        actions = [
            {
                '_index': self.index_name,
                '_source': product
            }
            for product in products
        ]
        bulk(self.es, actions)
    
    def search(self, query: str, filters: Optional[Dict] = None,
               page: int = 0, size: int = 20) -> Dict:
        """Full-text search with filters"""
        
        must_clauses = [
            {
                'multi_match': {
                    'query': query,
                    'fields': ['name^3', 'description^2', 'brand'],
                    'fuzziness': 'AUTO'
                }
            }
        ]
        
        filter_clauses = []
        if filters:
            if 'brand' in filters:
                filter_clauses.append({
                    'term': {'brand': filters['brand']}
                })
            if 'min_price' in filters or 'max_price' in filters:
                range_query = {'price': {}}
                if 'min_price' in filters:
                    range_query['price']['gte'] = filters['min_price']
                if 'max_price' in filters:
                    range_query['price']['lte'] = filters['max_price']
                filter_clauses.append({'range': range_query})
        
        search_body = {
            'query': {
                'bool': {
                    'must': must_clauses,
                    'filter': filter_clauses
                }
            },
            'from': page * size,
            'size': size,
            'sort': ['_score', {'price': 'asc'}],
            'aggs': {
                'brands': {
                    'terms': {'field': 'brand', 'size': 20}
                },
                'price_ranges': {
                    'range': {
                        'field': 'price',
                        'ranges': [
                            {'to': 500},
                            {'from': 500, 'to': 1000},
                            {'from': 1000, 'to': 2000},
                            {'from': 2000}
                        ]
                    }
                }
            },
            'highlight': {
                'fields': {
                    'name': {},
                    'description': {'fragment_size': 150}
                }
            }
        }
        
        return self.es.search(index=self.index_name, body=search_body)
    
    def autocomplete(self, prefix: str) -> List[str]:
        """Autocomplete suggestions"""
        suggest_body = {
            'suggest': {
                'product-suggest': {
                    'prefix': prefix,
                    'completion': {
                        'field': 'name.suggest',
                        'size': 10,
                        'fuzzy': {'fuzziness': 'AUTO'}
                    }
                }
            }
        }
        
        response = self.es.search(index=self.index_name, body=suggest_body)
        suggestions = response['suggest']['product-suggest'][0]['options']
        return [s['text'] for s in suggestions]
    
    def get_aggregations(self) -> Dict:
        """Get aggregations"""
        agg_body = {
            'size': 0,
            'aggs': {
                'brands': {
                    'terms': {'field': 'brand', 'size': 50}
                },
                'avg_price': {
                    'avg': {'field': 'price'}
                },
                'price_stats': {
                    'stats': {'field': 'price'}
                }
            }
        }
        
        response = self.es.search(index=self.index_name, body=agg_body)
        return response['aggregations']

# Usage
service = ProductSearchService()
service.create_index()

# Index products
products = [
    {
        'name': 'Laptop',
        'brand': 'Dell',
        'price': 899.99,
        'description': 'High-performance laptop',
        'tags': ['electronics', 'computers']
    },
    {
        'name': 'Mouse',
        'brand': 'Logitech',
        'price': 29.99,
        'description': 'Wireless mouse',
        'tags': ['electronics', 'accessories']
    }
]
service.bulk_index(products)

# Search
results = service.search(
    query='laptop',
    filters={'brand': 'Dell', 'min_price': 500, 'max_price': 1000}
)

for hit in results['hits']['hits']:
    print(f"{hit['_source']['name']} - ${hit['_source']['price']}")
    if 'highlight' in hit:
        print(f"  Highlight: {hit['highlight']}")

# Autocomplete
suggestions = service.autocomplete('lap')
print(f"Suggestions: {suggestions}")

# Aggregations
aggs = service.get_aggregations()
print(f"Average price: ${aggs['avg_price']['value']:.2f}")
```

### Flask API Example

```python
from flask import Flask, request, jsonify
from product_search_service import ProductSearchService

app = Flask(__name__)
search_service = ProductSearchService()

@app.route('/api/products/search', methods=['GET'])
def search_products():
    query = request.args.get('q', '')
    brand = request.args.get('brand')
    min_price = request.args.get('min_price', type=float)
    max_price = request.args.get('max_price', type=float)
    page = request.args.get('page', 0, type=int)
    size = request.args.get('size', 20, type=int)
    
    filters = {}
    if brand:
        filters['brand'] = brand
    if min_price:
        filters['min_price'] = min_price
    if max_price:
        filters['max_price'] = max_price
    
    results = search_service.search(query, filters, page, size)
    
    return jsonify({
        'total': results['hits']['total']['value'],
        'products': [hit['_source'] for hit in results['hits']['hits']],
        'aggregations': results.get('aggregations', {})
    })

@app.route('/api/products/autocomplete', methods=['GET'])
def autocomplete():
    prefix = request.args.get('prefix', '')
    suggestions = search_service.autocomplete(prefix)
    return jsonify({'suggestions': suggestions})

@app.route('/api/products', methods=['POST'])
def create_product():
    product = request.json
    response = search_service.index_product(product)
    return jsonify(response), 201

if __name__ == '__main__':
    app.run(debug=True)
```

## Node.js

### Installation

```bash
npm install @elastic/elasticsearch
```

### Basic Usage

```javascript
const { Client } = require('@elastic/elasticsearch');

// Create client
const client = new Client({
  node: 'http://localhost:9200'
});

// Index a document
async function indexProduct() {
  const result = await client.index({
    index: 'products',
    id: '1',
    document: {
      name: 'Laptop',
      brand: 'Dell',
      price: 899.99,
      description: 'High-performance laptop',
      tags: ['electronics', 'computers'],
      created_at: new Date()
    }
  });
  
  console.log(result);
}

// Search
async function searchProducts(query) {
  const result = await client.search({
    index: 'products',
    query: {
      match: {
        description: query
      }
    }
  });
  
  return result.hits.hits.map(hit => hit._source);
}

// Run
indexProduct();
searchProducts('laptop').then(console.log);
```

### Express.js API Example

```javascript
const express = require('express');
const { Client } = require('@elastic/elasticsearch');

const app = express();
app.use(express.json());

const client = new Client({ node: 'http://localhost:9200' });

// Search endpoint
app.get('/api/products/search', async (req, res) => {
  try {
    const { q, brand, min_price, max_price, page = 0, size = 20 } = req.query;
    
    const mustClauses = [];
    const filterClauses = [];
    
    if (q) {
      mustClauses.push({
        multi_match: {
          query: q,
          fields: ['name^3', 'description^2', 'brand'],
          fuzziness: 'AUTO'
        }
      });
    }
    
    if (brand) {
      filterClauses.push({ term: { brand } });
    }
    
    if (min_price || max_price) {
      const rangeQuery = { price: {} };
      if (min_price) rangeQuery.price.gte = parseFloat(min_price);
      if (max_price) rangeQuery.price.lte = parseFloat(max_price);
      filterClauses.push({ range: rangeQuery });
    }
    
    const result = await client.search({
      index: 'products',
      from: parseInt(page) * parseInt(size),
      size: parseInt(size),
      query: {
        bool: {
          must: mustClauses,
          filter: filterClauses
        }
      },
      aggs: {
        brands: {
          terms: { field: 'brand', size: 20 }
        },
        price_ranges: {
          range: {
            field: 'price',
            ranges: [
              { to: 500 },
              { from: 500, to: 1000 },
              { from: 1000, to: 2000 },
              { from: 2000 }
            ]
          }
        }
      },
      highlight: {
        fields: {
          name: {},
          description: { fragment_size: 150 }
        }
      }
    });
    
    res.json({
      total: result.hits.total.value,
      products: result.hits.hits.map(hit => ({
        ...hit._source,
        score: hit._score,
        highlight: hit.highlight
      })),
      aggregations: result.aggregations
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Autocomplete endpoint
app.get('/api/products/autocomplete', async (req, res) => {
  try {
    const { prefix } = req.query;
    
    const result = await client.search({
      index: 'products',
      suggest: {
        'product-suggest': {
          prefix,
          completion: {
            field: 'name.suggest',
            size: 10,
            fuzzy: { fuzziness: 'AUTO' }
          }
        }
      }
    });
    
    const suggestions = result.suggest['product-suggest'][0].options
      .map(option => option.text);
    
    res.json({ suggestions });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Create product endpoint
app.post('/api/products', async (req, res) => {
  try {
    const product = req.body;
    
    const result = await client.index({
      index: 'products',
      document: product
    });
    
    res.status(201).json(result);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.listen(3000, () => {
  console.log('Server running on port 3000');
});
```

## Go

### Installation

```bash
go get github.com/elastic/go-elasticsearch/v8
```

### Example

```go
package main

import (
    "bytes"
    "context"
    "encoding/json"
    "fmt"
    "log"
    
    "github.com/elastic/go-elasticsearch/v8"
    "github.com/elastic/go-elasticsearch/v8/esapi"
)

type Product struct {
    Name        string   `json:"name"`
    Brand       string   `json:"brand"`
    Price       float64  `json:"price"`
    Description string   `json:"description"`
    Tags        []string `json:"tags"`
}

func main() {
    // Create client
    cfg := elasticsearch.Config{
        Addresses: []string{"http://localhost:9200"},
    }
    es, err := elasticsearch.NewClient(cfg)
    if err != nil {
        log.Fatal(err)
    }
    
    // Index a product
    product := Product{
        Name:        "Laptop",
        Brand:       "Dell",
        Price:       899.99,
        Description: "High-performance laptop",
        Tags:        []string{"electronics", "computers"},
    }
    
    indexProduct(es, product)
    
    // Search
    results := searchProducts(es, "laptop")
    fmt.Println(results)
}

func indexProduct(es *elasticsearch.Client, product Product) {
    data, _ := json.Marshal(product)
    
    req := esapi.IndexRequest{
        Index:      "products",
        DocumentID: "1",
        Body:       bytes.NewReader(data),
        Refresh:    "true",
    }
    
    res, err := req.Do(context.Background(), es)
    if err != nil {
        log.Fatal(err)
    }
    defer res.Body.Close()
    
    fmt.Println(res.String())
}

func searchProducts(es *elasticsearch.Client, query string) []Product {
    var buf bytes.Buffer
    searchQuery := map[string]interface{}{
        "query": map[string]interface{}{
            "match": map[string]interface{}{
                "description": query,
            },
        },
    }
    
    json.NewEncoder(&buf).Encode(searchQuery)
    
    res, err := es.Search(
        es.Search.WithContext(context.Background()),
        es.Search.WithIndex("products"),
        es.Search.WithBody(&buf),
    )
    if err != nil {
        log.Fatal(err)
    }
    defer res.Body.Close()
    
    var result map[string]interface{}
    json.NewDecoder(res.Body).Decode(&result)
    
    var products []Product
    hits := result["hits"].(map[string]interface{})["hits"].([]interface{})
    
    for _, hit := range hits {
        source := hit.(map[string]interface{})["_source"]
        data, _ := json.Marshal(source)
        
        var product Product
        json.Unmarshal(data, &product)
        products = append(products, product)
    }
    
    return products
}
```

## Summary

All examples demonstrate:
- ✅ Connecting to Elasticsearch
- ✅ Indexing documents
- ✅ Full-text search with filters
- ✅ Aggregations
- ✅ Autocomplete
- ✅ Highlighting
- ✅ REST API integration

Choose the language that fits your stack and adapt the patterns to your needs.

---

**Previous**: [← Use Cases](10_Use_Cases.md) | **Next**: [Best Practices →](12_Best_Practices.md)
