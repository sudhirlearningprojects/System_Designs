# How Distributed Systems Coordinate: Raft, Gossip, Locks, Leases & Version Vectors

## Why Coordination is Hard

In a distributed system, nodes can crash, messages can be delayed or lost, and clocks drift. Coordination solves:
- **Who is the leader?** (Raft)
- **What is the cluster state?** (Gossip)
- **Who owns this resource?** (Distributed Locks / Leases)
- **Which version is correct?** (Version Vectors)

---

## 1. Raft — Consensus & Leader Election

Raft ensures all nodes **agree on a sequence of values** even when some nodes fail. Used in etcd, CockroachDB, TiKV, Consul.

### Roles
```
Follower  ──► (timeout, no heartbeat) ──► Candidate ──► (majority votes) ──► Leader
   ▲                                                                              │
   └──────────────────── (heartbeat received) ───────────────────────────────────┘
```

### Leader Election
1. Follower times out → becomes Candidate, increments `term`, votes for itself
2. Sends `RequestVote` to all nodes
3. Node grants vote if: `term >= its term` AND it hasn't voted this term
4. Candidate with **majority votes** becomes Leader
5. Leader sends periodic **heartbeats** to prevent new elections

### Log Replication
```
Client ──► Leader
            │
            ├── AppendEntries ──► Follower 1 ✅
            ├── AppendEntries ──► Follower 2 ✅
            └── AppendEntries ──► Follower 3 ❌ (crashed)

Leader commits when majority (2/3) acknowledge → responds to client
Follower 3 catches up when it recovers
```

### Key Properties
- **Safety**: Only one leader per term
- **Liveness**: System makes progress if majority alive
- **Log Matching**: If two logs have same index+term, all preceding entries are identical

### Split Brain Prevention
```
5-node cluster, network partition: [2 nodes] | [3 nodes]
  - 2-node side: cannot elect leader (no majority) → stops accepting writes
  - 3-node side: elects leader → continues serving
  → Only one active leader at all times
```

---

## 2. Gossip Protocol — Cluster Membership & State Propagation

Gossip (epidemic protocol) spreads information through random peer-to-peer communication. Used in Cassandra, DynamoDB, Redis Cluster, Consul.

### How It Works
```
Every T milliseconds, each node:
  1. Picks K random peers
  2. Sends its known state (node list, versions, metadata)
  3. Merges received state with its own

Round 1: Node A tells B, C
Round 2: B tells D, E  |  C tells F, A
Round 3: D tells G...
→ Information reaches all N nodes in O(log N) rounds
```

### Convergence
```
N = 1000 nodes, K = 3 peers per round, T = 1 second
Rounds to full convergence ≈ log₃(1000) ≈ 7 seconds
```

### Failure Detection
Each node maintains a **heartbeat counter** for every other node:
```
Node A: { B: {heartbeat: 142, timestamp: now},
           C: {heartbeat: 98,  timestamp: now-5s},  ← suspicious
           D: {heartbeat: 71,  timestamp: now-12s}  ← suspected dead
         }

If no update for φ (phi) threshold → mark node as SUSPECT → DEAD
```

### SWIM Protocol (Scalable Weakly-consistent Infection-style Membership)
Direct ping fails → ask K random nodes to ping on your behalf (indirect ping) → if all fail → mark dead.

```
A ──ping──► B  (no response)
A ──ping-req──► C, D, E  "please ping B"
  C ──ping──► B  (no response)
  D ──ping──► B  (no response)
→ A marks B as FAILED, gossips to cluster
```

### Gossip vs Raft

| | Gossip | Raft |
|---|---|---|
| Consistency | Eventual | Strong |
| Latency | Low | Higher |
| Use case | Membership, metadata | Leader election, log replication |
| Failure tolerance | Very high | Majority must be alive |

---

## 3. Distributed Locks

Ensures **mutual exclusion** across nodes — only one node executes a critical section at a time.

### Redis-based Lock (Redlock Algorithm)

```
Client acquires lock:
  SET lock:resource <uuid> NX PX 30000
  (NX = only if not exists, PX = expire in 30s)

Client releases lock:
  if GET lock:resource == <uuid>:   ← check ownership first
      DEL lock:resource             ← then delete
  (use Lua script for atomicity)
```

**Redlock** (multi-node Redis):
```
5 Redis nodes (independent, no replication)
Client tries to acquire lock on all 5:
  - Must succeed on majority (3+) within validity time
  - Lock validity = min_ttl - (elapsed_time + clock_drift)
  - If majority not acquired → release all and retry with backoff
```

### ZooKeeper-based Lock (Ephemeral Sequential Nodes)

```
/locks/resource/
  ├── lock-0000000001  ← created by Client A (lowest = holds lock)
  ├── lock-0000000002  ← created by Client B (watches 0001)
  └── lock-0000000003  ← created by Client C (watches 0002)

Client A finishes → deletes 0001
→ Client B gets watch notification → acquires lock
→ No thundering herd (each watches only predecessor)
```

### Fencing Tokens — Preventing Stale Lock Issues

```
Problem:
  Client A acquires lock (token=33), pauses (GC/network)
  Lock expires → Client B acquires lock (token=34)
  Client A resumes → writes to storage with stale lock!

Solution: Fencing Token
  Storage server rejects writes with token < last seen token
  Client A (token=33) → REJECTED (last seen = 34)
  Client B (token=34) → ACCEPTED
```

---

## 4. Leases — Time-Bounded Locks

A lease is a lock with a **built-in expiry**. The holder must renew before expiry or loses ownership. Used in Kubernetes (leader election), HDFS (NameNode), Chubby.

### Lease Lifecycle
```
Client                          Server
  │──── RequestLease(ttl=30s) ──►│  grants lease, starts timer
  │◄─── LeaseGranted(token) ─────│
  │                               │
  │──── Renew(token) ────────────►│  resets timer to 30s
  │◄─── Renewed ─────────────────│
  │                               │
  │  (client crashes)             │
  │                               │  timer expires after 30s
  │                               │  lease revoked → another client can acquire
```

### Lease vs Lock

| | Lock | Lease |
|---|---|---|
| Expiry | Manual release only | Auto-expires on TTL |
| Crash safety | Lock held forever if holder crashes | Auto-released after TTL |
| Renewal | Not needed | Must renew periodically |
| Use case | Short critical sections | Long-lived ownership (leader, file handle) |

### Kubernetes Leader Election via Leases
```yaml
# Leader writes to Lease object every renewDeadline (10s)
# Candidates check: if lease not renewed within leaseDuration (15s) → try to acquire
apiVersion: coordination.k8s.io/v1
kind: Lease
metadata:
  name: my-controller
spec:
  holderIdentity: pod-abc-123
  leaseDurationSeconds: 15
  renewTime: "2024-01-01T10:00:00Z"
```

---

## 5. Version Vectors — Tracking Causality

Version vectors track **which node made which updates** to detect conflicts and establish causality. Used in DynamoDB, Riak, CouchDB.

### Structure
```
Each node maintains a vector: { nodeId → counter }

Node A writes: VA = {A:1, B:0, C:0}
Node B writes: VB = {A:0, B:1, C:0}
Node A writes again: VA = {A:2, B:0, C:0}
```

### Causality Rules
```
V1 happens-before V2  if  all V1[i] <= V2[i]  AND  at least one V1[i] < V2[i]
V1 concurrent with V2 if  V1[i] > V2[i] for some i  AND  V1[j] < V2[j] for some j
```

### Conflict Detection Example
```
Initial: {A:0, B:0}

Client 1 reads from Node A → {A:1, B:0} → updates "name=Alice"
Client 2 reads from Node B → {A:0, B:1} → updates "name=Bob"  (concurrent!)

Node A: {A:1, B:0} "Alice"
Node B: {A:0, B:1} "Bob"

Neither dominates the other → CONFLICT
→ Application must resolve (last-write-wins, merge, ask user)
```

### After Sync (No Conflict)
```
Client reads {A:1, B:0}, updates → {A:2, B:0}
Later reads {A:2, B:0}, updates → {A:3, B:0}

{A:1, B:0} happens-before {A:3, B:0} → NO conflict, {A:3} wins
```

### Dotted Version Vectors (DynamoDB)
Standard version vectors grow unboundedly with many clients. DynamoDB uses **dotted version vectors** to track per-write causality without client IDs bloating the vector.

---

## 6. How They Work Together (Real System Example)

### etcd (used by Kubernetes)
```
Raft         → Leader election, log replication, strong consistency
Leases       → TTL-based key expiry, Kubernetes leader election
Distributed  → Optimistic concurrency via revision numbers (not locks)
Locks          (compare-and-swap on revision)
```

### Cassandra
```
Gossip       → Node membership, token ring state, schema propagation
Version Vec  → Last-write-wins with timestamps (simplified vector clocks)
No Raft      → Tunable consistency (ONE, QUORUM, ALL) instead
Leases       → Not used (AP system, prefers availability)
```

### DynamoDB
```
Gossip       → Internal membership (not exposed)
Version Vec  → Dotted version vectors for conflict detection
Raft-like    → Paxos variant for single-key transactions
Leases       → Session-based locks via DynamoDB Streams + TTL
```

---

## 7. Quick Reference

```
Need strong consensus / leader election?
  └─► Raft (etcd, Consul, CockroachDB)

Need to propagate state to all nodes eventually?
  └─► Gossip (Cassandra, Redis Cluster)

Need mutual exclusion for a short operation?
  └─► Distributed Lock (Redis SET NX, ZooKeeper ephemeral node)

Need long-lived ownership that auto-expires on crash?
  └─► Lease (Kubernetes, Chubby, HDFS)

Need to detect concurrent writes / causality?
  └─► Version Vectors (DynamoDB, Riak, CouchDB)
```

---

## 8. Common Interview Questions

**Q: What happens if a Raft leader is partitioned?**
New leader elected in majority partition. Old leader can't commit (no majority ACK). Rejoins as follower, rolls back uncommitted entries.

**Q: Can two nodes both think they hold a distributed lock?**
Yes — if lock TTL expires while holder is paused (GC, network). Use **fencing tokens** to prevent stale writes from reaching storage.

**Q: How does Gossip handle a node that keeps flapping (up/down)?**
Suspicion mechanism with phi-accrual failure detector. Node must be consistently unreachable before marked dead. Hysteria dampening delays state changes for flapping nodes.

**Q: Version vectors vs vector clocks?**
Vector clocks track per-process causality for events. Version vectors track per-replica write counts for data items. Conceptually similar, different application context.

**Q: Why does Raft need an odd number of nodes?**
To always have a clear majority. With 4 nodes, a 2-2 split has no majority → no leader elected. With 3 or 5 nodes, majority is always achievable (2/3 or 3/5).
