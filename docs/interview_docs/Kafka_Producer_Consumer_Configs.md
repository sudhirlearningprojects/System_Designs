# Kafka Producer & Consumer Important Configs

---

## Producer Configs

### Connection & Identity
| Config | Default | Description |
|--------|---------|-------------|
| `bootstrap.servers` | — | Comma-separated list of `host:port` brokers to bootstrap the connection. Only a few are needed; Kafka discovers the rest automatically. |
| `client.id` | `""` | Logical name for the producer, used in logs and metrics. Helps trace which application sent a message. |

### Serialization
| Config | Default | Description |
|--------|---------|-------------|
| `key.serializer` | — | Class to serialize the message key (e.g., `StringSerializer`). Must match the deserializer on the consumer side. |
| `value.serializer` | — | Class to serialize the message value. Common choices: `StringSerializer`, `JsonSerializer`, `AvroSerializer`. |

### Reliability & Acknowledgement
| Config | Default | Description |
|--------|---------|-------------|
| `acks` | `1` | Number of broker acknowledgements required before a send is considered successful. `0`=fire-and-forget, `1`=leader only, `all`=all in-sync replicas (safest). |
| `retries` | `2147483647` | How many times the producer retries a failed send before giving up. Works together with `retry.backoff.ms`. |
| `retry.backoff.ms` | `100` | Wait time between retry attempts. Prevents hammering a struggling broker. |
| `delivery.timeout.ms` | `120000` | Upper bound on total time for a record to be delivered (including retries). Producer fails the record after this timeout. |
| `enable.idempotence` | `true` (Kafka 3+) | Ensures exactly-once delivery per partition by deduplicating retried messages. Requires `acks=all`. |
| `transactional.id` | `null` | Enables transactional producer for exactly-once semantics across multiple partitions/topics. Must be unique per producer instance. |

### Batching & Throughput
| Config | Default | Description |
|--------|---------|-------------|
| `batch.size` | `16384` (16KB) | Maximum bytes to batch together before sending. Larger batches improve throughput but increase latency. |
| `linger.ms` | `0` | Time the producer waits to fill a batch before sending. Setting `5–100ms` significantly improves throughput at the cost of slight latency. |
| `buffer.memory` | `33554432` (32MB) | Total memory available for buffering records waiting to be sent. Producer blocks or throws if this is exhausted. |
| `max.block.ms` | `60000` | How long `send()` blocks when the buffer is full. After this, a `TimeoutException` is thrown. |
| `compression.type` | `none` | Compression algorithm for batches: `none`, `gzip`, `snappy`, `lz4`, `zstd`. Reduces network and disk usage. |

### Ordering
| Config | Default | Description |
|--------|---------|-------------|
| `max.in.flight.requests.per.connection` | `5` | Max unacknowledged requests per broker connection. Set to `1` to guarantee ordering without idempotence; with idempotence, up to `5` is safe. |

### Partitioning
| Config | Default | Description |
|--------|---------|-------------|
| `partitioner.class` | `DefaultPartitioner` | Class that decides which partition a record goes to. Custom partitioners allow business-logic-based routing. |

---

## Consumer Configs

### Connection & Identity
| Config | Default | Description |
|--------|---------|-------------|
| `bootstrap.servers` | — | Same as producer — entry point to discover the Kafka cluster. |
| `group.id` | — | Consumer group name. All consumers with the same `group.id` share partition assignments and track offsets together. |
| `client.id` | `""` | Logical name for the consumer instance, useful for monitoring and debugging. |

### Serialization
| Config | Default | Description |
|--------|---------|-------------|
| `key.deserializer` | — | Class to deserialize the message key. Must match the serializer used by the producer. |
| `value.deserializer` | — | Class to deserialize the message value. Must match the serializer used by the producer. |

### Offset Management
| Config | Default | Description |
|--------|---------|-------------|
| `auto.offset.reset` | `latest` | What to do when no committed offset exists: `earliest` (read from beginning) or `latest` (skip old messages). |
| `enable.auto.commit` | `true` | If `true`, offsets are committed automatically in the background. Set to `false` for manual control to avoid data loss or duplication. |
| `auto.commit.interval.ms` | `5000` | Frequency of automatic offset commits when `enable.auto.commit=true`. Lower values reduce reprocessing on crash. |

### Polling & Session
| Config | Default | Description |
|--------|---------|-------------|
| `max.poll.records` | `500` | Max records returned in a single `poll()` call. Tune this based on processing time to avoid session timeouts. |
| `max.poll.interval.ms` | `300000` | Max time between two `poll()` calls before the consumer is considered dead and triggers a rebalance. |
| `session.timeout.ms` | `45000` | Time the broker waits for a heartbeat before marking the consumer dead. Should be less than `max.poll.interval.ms`. |
| `heartbeat.interval.ms` | `3000` | Frequency at which the consumer sends heartbeats to the broker. Should be ~1/3 of `session.timeout.ms`. |
| `fetch.min.bytes` | `1` | Minimum data the broker must have before responding to a fetch request. Increasing this reduces fetch frequency and improves throughput. |
| `fetch.max.wait.ms` | `500` | Max time the broker waits to accumulate `fetch.min.bytes` before responding. Balances latency vs throughput. |
| `fetch.max.bytes` | `52428800` (50MB) | Max bytes returned per fetch request across all partitions. Limits memory usage per poll. |

### Isolation & Transactions
| Config | Default | Description |
|--------|---------|-------------|
| `isolation.level` | `read_uncommitted` | Set to `read_committed` to only consume messages from committed transactions, enabling exactly-once consumption. |

---

## Quick Reference: Key Configs by Goal

| Goal | Producer Config | Consumer Config |
|------|----------------|-----------------|
| No data loss | `acks=all`, `enable.idempotence=true` | `enable.auto.commit=false` |
| High throughput | `linger.ms=20`, `batch.size=65536`, `compression.type=lz4` | `fetch.min.bytes=1024`, `max.poll.records=1000` |
| Strict ordering | `max.in.flight.requests.per.connection=1` or idempotence | Single partition or same key |
| Exactly-once | `transactional.id`, `enable.idempotence=true` | `isolation.level=read_committed` |
| Avoid rebalance storms | — | Tune `max.poll.interval.ms` > processing time |
