# Distributed Consensus Algorithms - Deep Dive

## What is Distributed Consensus?

**Definition**: Agreement among distributed nodes on a single value/state, even in the presence of failures.

### The Problem

```
Scenario: 5 servers need to agree on "who is the leader?"

Server 1: "I vote for Server 3"
Server 2: "I vote for Server 3"
Server 3: "I vote for Server 5"
Server 4: (crashed)
Server 5: "I vote for Server 3"

Question: How do they reach consensus despite Server 4 being down?
```

### Why It's Hard

1. **Network partitions** - Nodes can't communicate
2. **Node failures** - Servers crash
3. **Message delays** - Network latency varies
4. **Byzantine failures** - Malicious nodes send wrong data
5. **Split-brain** - Multiple leaders elected

---

## CAP Theorem (Foundation)

```
CAP Theorem: You can only have 2 out of 3:

C - Consistency: All nodes see the same data
A - Availability: Every request gets a response
P - Partition Tolerance: System works despite network failures

Real-world choice:
- CP: Consistency + Partition Tolerance (sacrifice Availability)
  Examples: HBase, MongoDB, Redis Cluster
  
- AP: Availability + Partition Tolerance (sacrifice Consistency)
  Examples: Cassandra, DynamoDB, Riak
  
- CA: Consistency + Availability (no Partition Tolerance)
  Examples: Traditional RDBMS (single node)
  Reality: Not possible in distributed systems
```

---

## 1. Paxos Algorithm

### Overview

**Invented by**: Leslie Lamport (1989)
**Used by**: Google Chubby, Apache ZooKeeper (inspiration)
**Guarantee**: Safety (never returns wrong result)

### The Problem Paxos Solves

```
Multiple nodes propose different values:
Node 1 proposes: "X"
Node 2 proposes: "Y"
Node 3 proposes: "X"

Goal: All nodes agree on ONE value (either X or Y)
```

### Paxos Roles

```
1. Proposer: Proposes values
2. Acceptor: Votes on proposals
3. Learner: Learns the chosen value

(A node can play multiple roles)
```

### Paxos Phases

#### Phase 1: Prepare

```
Proposer → Acceptors: "Prepare(n)"
  n = proposal number (must be unique and increasing)

Acceptors respond:
  - "Promise" to not accept proposals < n
  - Send highest accepted proposal (if any)

Example:
Proposer 1: Prepare(1)
Acceptor A: Promise(1), no previous proposal
Acceptor B: Promise(1), no previous proposal
Acceptor C: Promise(1), no previous proposal

Majority (2/3) promised → proceed to Phase 2
```

#### Phase 2: Accept

```
Proposer → Acceptors: "Accept(n, value)"
  value = proposed value (or highest from Phase 1)

Acceptors respond:
  - "Accepted(n, value)" if n >= promised number
  - Reject if n < promised number

Example:
Proposer 1: Accept(1, "X")
Acceptor A: Accepted(1, "X")
Acceptor B: Accepted(1, "X")
Acceptor C: Accepted(1, "X")

Majority (2/3) accepted → value "X" is chosen
```

### Paxos Implementation

```java
public class PaxosNode {
    
    private long promisedProposalNumber = 0;
    private long acceptedProposalNumber = 0;
    private String acceptedValue = null;
    
    // Phase 1: Prepare
    public PrepareResponse prepare(long proposalNumber) {
        if (proposalNumber > promisedProposalNumber) {
            promisedProposalNumber = proposalNumber;
            return new PrepareResponse(true, acceptedProposalNumber, acceptedValue);
        }
        return new PrepareResponse(false, 0, null);
    }
    
    // Phase 2: Accept
    public AcceptResponse accept(long proposalNumber, String value) {
        if (proposalNumber >= promisedProposalNumber) {
            promisedProposalNumber = proposalNumber;
            acceptedProposalNumber = proposalNumber;
            acceptedValue = value;
            return new AcceptResponse(true);
        }
        return new AcceptResponse(false);
    }
}

public class PaxosProposer {
    
    private List<PaxosNode> acceptors;
    private long proposalNumber = 0;
    
    public String propose(String value) {
        proposalNumber++;
        
        // Phase 1: Prepare
        List<PrepareResponse> prepareResponses = new ArrayList<>();
        for (PaxosNode acceptor : acceptors) {
            PrepareResponse response = acceptor.prepare(proposalNumber);
            if (response.isPromised()) {
                prepareResponses.add(response);
            }
        }
        
        // Check majority
        if (prepareResponses.size() < (acceptors.size() / 2 + 1)) {
            return null; // Failed to get majority
        }
        
        // Use highest accepted value (if any)
        String proposedValue = value;
        long highestAcceptedNumber = 0;
        for (PrepareResponse response : prepareResponses) {
            if (response.getAcceptedProposalNumber() > highestAcceptedNumber) {
                highestAcceptedNumber = response.getAcceptedProposalNumber();
                proposedValue = response.getAcceptedValue();
            }
        }
        
        // Phase 2: Accept
        int acceptCount = 0;
        for (PaxosNode acceptor : acceptors) {
            AcceptResponse response = acceptor.accept(proposalNumber, proposedValue);
            if (response.isAccepted()) {
                acceptCount++;
            }
        }
        
        // Check majority
        if (acceptCount >= (acceptors.size() / 2 + 1)) {
            return proposedValue; // Consensus reached
        }
        
        return null; // Failed
    }
}
```

### Paxos Example Scenario

```
Scenario: 5 nodes, 2 proposers

Time 0:
Proposer A: Prepare(1)
Acceptors: Promise(1) [3/5 majority]

Time 1:
Proposer B: Prepare(2) [higher number!]
Acceptors: Promise(2) [3/5 majority]

Time 2:
Proposer A: Accept(1, "X")
Acceptors: Reject (already promised 2)

Time 3:
Proposer B: Accept(2, "Y")
Acceptors: Accepted(2, "Y") [3/5 majority]

Result: "Y" is chosen
```

### Paxos Challenges

1. **Livelock**: Proposers keep interrupting each other
2. **Complexity**: Hard to understand and implement
3. **Performance**: Multiple round trips (2 phases)

---

## 2. Raft Algorithm

### Overview

**Invented by**: Diego Ongaro & John Ousterhout (2014)
**Used by**: etcd, Consul, CockroachDB
**Goal**: Easier to understand than Paxos

### Raft Key Concepts

```
1. Leader Election: One leader at a time
2. Log Replication: Leader replicates log to followers
3. Safety: Committed entries never lost
```

### Raft Roles

```
1. Leader: Handles all client requests, replicates log
2. Follower: Passive, responds to leader/candidate
3. Candidate: Tries to become leader during election
```

### Raft Terms

```
Term: Logical clock (monotonically increasing)

Term 1: Leader A
Term 2: Leader B (A crashed)
Term 3: Leader C (B crashed)

Each term has at most ONE leader
```

### Raft Leader Election

```
Step 1: Follower timeout (no heartbeat from leader)
  → Becomes Candidate
  → Increments term
  → Votes for itself
  → Requests votes from others

Step 2: Other nodes vote
  → Vote for first candidate in this term
  → Only one vote per term

Step 3: Candidate receives majority votes
  → Becomes Leader
  → Sends heartbeats to all followers

Example:
Node A: Timeout → Candidate (Term 2)
Node A → Nodes B,C,D,E: RequestVote(Term 2)
Nodes B,C: Vote for A
Node A: 3/5 votes → Leader (Term 2)
```

### Raft Implementation

```java
public class RaftNode {
    
    private enum State { FOLLOWER, CANDIDATE, LEADER }
    
    private State state = State.FOLLOWER;
    private long currentTerm = 0;
    private String votedFor = null;
    private List<LogEntry> log = new ArrayList<>();
    private long commitIndex = 0;
    private long lastApplied = 0;
    
    // Leader election timeout
    private long electionTimeout = 150 + new Random().nextInt(150); // 150-300ms
    private long lastHeartbeat = System.currentTimeMillis();
    
    // Check if election timeout
    @Scheduled(fixedRate = 10)
    public void checkElectionTimeout() {
        if (state == State.FOLLOWER || state == State.CANDIDATE) {
            long elapsed = System.currentTimeMillis() - lastHeartbeat;
            if (elapsed > electionTimeout) {
                startElection();
            }
        }
    }
    
    // Start leader election
    private void startElection() {
        state = State.CANDIDATE;
        currentTerm++;
        votedFor = this.nodeId;
        int votesReceived = 1; // Vote for self
        
        // Request votes from other nodes
        for (RaftNode peer : peers) {
            VoteResponse response = peer.requestVote(
                currentTerm,
                this.nodeId,
                log.size() - 1,
                log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm()
            );
            
            if (response.isVoteGranted()) {
                votesReceived++;
            }
        }
        
        // Check majority
        if (votesReceived > (peers.size() + 1) / 2) {
            becomeLeader();
        }
    }
    
    // Handle vote request
    public VoteResponse requestVote(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
        // Update term if higher
        if (term > currentTerm) {
            currentTerm = term;
            state = State.FOLLOWER;
            votedFor = null;
        }
        
        // Grant vote if:
        // 1. Haven't voted in this term
        // 2. Candidate's log is at least as up-to-date
        boolean voteGranted = false;
        if (term == currentTerm && votedFor == null) {
            long myLastLogIndex = log.size() - 1;
            long myLastLogTerm = log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm();
            
            if (lastLogTerm > myLastLogTerm || 
                (lastLogTerm == myLastLogTerm && lastLogIndex >= myLastLogIndex)) {
                votedFor = candidateId;
                voteGranted = true;
                lastHeartbeat = System.currentTimeMillis();
            }
        }
        
        return new VoteResponse(currentTerm, voteGranted);
    }
    
    // Become leader
    private void becomeLeader() {
        state = State.LEADER;
        log.info("Node {} became leader for term {}", nodeId, currentTerm);
        
        // Send heartbeats
        sendHeartbeats();
    }
    
    // Send heartbeats (empty AppendEntries)
    @Scheduled(fixedRate = 50) // Every 50ms
    public void sendHeartbeats() {
        if (state == State.LEADER) {
            for (RaftNode peer : peers) {
                peer.appendEntries(currentTerm, nodeId, log.size() - 1, 
                    log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm(),
                    Collections.emptyList(), commitIndex);
            }
        }
    }
    
    // Handle append entries (heartbeat or log replication)
    public AppendEntriesResponse appendEntries(long term, String leaderId, 
        long prevLogIndex, long prevLogTerm, List<LogEntry> entries, long leaderCommit) {
        
        // Update term if higher
        if (term > currentTerm) {
            currentTerm = term;
            state = State.FOLLOWER;
            votedFor = null;
        }
        
        // Reset election timeout
        lastHeartbeat = System.currentTimeMillis();
        
        // Reject if term is old
        if (term < currentTerm) {
            return new AppendEntriesResponse(currentTerm, false);
        }
        
        // Check log consistency
        if (prevLogIndex >= 0) {
            if (log.size() <= prevLogIndex || log.get((int) prevLogIndex).getTerm() != prevLogTerm) {
                return new AppendEntriesResponse(currentTerm, false);
            }
        }
        
        // Append new entries
        if (!entries.isEmpty()) {
            log.addAll(entries);
        }
        
        // Update commit index
        if (leaderCommit > commitIndex) {
            commitIndex = Math.min(leaderCommit, log.size() - 1);
        }
        
        return new AppendEntriesResponse(currentTerm, true);
    }
}
```

### Raft Log Replication

```
Client → Leader: Write("X")

Leader:
1. Append to local log (uncommitted)
2. Send AppendEntries to followers
3. Wait for majority to acknowledge
4. Commit entry
5. Apply to state machine
6. Respond to client

Example:
Leader: [1:X] (uncommitted)
Leader → Followers: AppendEntries([1:X])
Follower A: [1:X] → ACK
Follower B: [1:X] → ACK
Leader: 3/5 majority → Commit [1:X]
Leader → Client: Success
```

### Raft Safety Properties

```
1. Election Safety: At most one leader per term
2. Leader Append-Only: Leader never overwrites log
3. Log Matching: If two logs contain same entry, all preceding entries are identical
4. Leader Completeness: If entry committed in term T, it will be in leader's log for all terms > T
5. State Machine Safety: If a server applies log entry at index i, no other server will apply different entry at i
```

---

## 3. Two-Phase Commit (2PC)

### Overview

**Use Case**: Distributed transactions (ACID across multiple databases)
**Guarantee**: Atomicity (all or nothing)

### 2PC Phases

#### Phase 1: Prepare (Voting)

```
Coordinator → Participants: "Prepare to commit transaction T"

Participants:
1. Lock resources
2. Write to undo/redo log
3. Vote: "Yes" (ready) or "No" (abort)

Example:
Coordinator → DB1: Prepare(T)
Coordinator → DB2: Prepare(T)
DB1 → Coordinator: Yes
DB2 → Coordinator: Yes
```

#### Phase 2: Commit/Abort

```
If all voted "Yes":
  Coordinator → Participants: "Commit"
  Participants: Commit transaction, release locks

If any voted "No":
  Coordinator → Participants: "Abort"
  Participants: Rollback, release locks

Example:
Coordinator → DB1: Commit
Coordinator → DB2: Commit
DB1: Committed
DB2: Committed
```

### 2PC Implementation

```java
public class TwoPhaseCommitCoordinator {
    
    private List<Participant> participants;
    
    public boolean executeTransaction(Transaction transaction) {
        String transactionId = UUID.randomUUID().toString();
        
        // Phase 1: Prepare
        List<CompletableFuture<Boolean>> prepareFutures = participants.stream()
            .map(participant -> CompletableFuture.supplyAsync(() -> 
                participant.prepare(transactionId, transaction)
            ))
            .collect(Collectors.toList());
        
        // Wait for all votes
        List<Boolean> votes = prepareFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // Check if all voted "Yes"
        boolean allYes = votes.stream().allMatch(vote -> vote);
        
        // Phase 2: Commit or Abort
        if (allYes) {
            // Commit
            participants.forEach(participant -> 
                participant.commit(transactionId)
            );
            return true;
        } else {
            // Abort
            participants.forEach(participant -> 
                participant.abort(transactionId)
            );
            return false;
        }
    }
}

public class Participant {
    
    private Map<String, Transaction> preparedTransactions = new ConcurrentHashMap<>();
    
    // Phase 1: Prepare
    public boolean prepare(String transactionId, Transaction transaction) {
        try {
            // Lock resources
            lockResources(transaction);
            
            // Write to undo log
            writeUndoLog(transaction);
            
            // Store prepared transaction
            preparedTransactions.put(transactionId, transaction);
            
            return true; // Vote "Yes"
        } catch (Exception e) {
            return false; // Vote "No"
        }
    }
    
    // Phase 2: Commit
    public void commit(String transactionId) {
        Transaction transaction = preparedTransactions.remove(transactionId);
        
        // Apply changes
        applyChanges(transaction);
        
        // Release locks
        releaseLocks(transaction);
        
        // Clear undo log
        clearUndoLog(transaction);
    }
    
    // Phase 2: Abort
    public void abort(String transactionId) {
        Transaction transaction = preparedTransactions.remove(transactionId);
        
        // Rollback using undo log
        rollback(transaction);
        
        // Release locks
        releaseLocks(transaction);
    }
}
```

### 2PC Problems

```
1. Blocking: If coordinator crashes after Phase 1, participants are blocked
2. Single point of failure: Coordinator crash = system stuck
3. Performance: Synchronous, high latency

Example of blocking:
Coordinator: Prepare → (CRASH)
Participants: Waiting forever (locks held)
```

---

## 4. Three-Phase Commit (3PC)

### Overview

**Improvement over 2PC**: Non-blocking
**Added Phase**: Pre-commit (timeout-based recovery)

### 3PC Phases

```
Phase 1: CanCommit (Voting)
  Coordinator → Participants: "Can you commit?"
  Participants: "Yes" or "No"

Phase 2: PreCommit (Prepare)
  If all "Yes":
    Coordinator → Participants: "PreCommit"
    Participants: Lock resources, write logs
  Else:
    Coordinator → Participants: "Abort"

Phase 3: DoCommit (Commit)
  Coordinator → Participants: "DoCommit"
  Participants: Commit, release locks
```

### 3PC Timeout Recovery

```
If participant doesn't hear from coordinator:
  - After Phase 1: Abort (safe)
  - After Phase 2: Commit (coordinator likely sent DoCommit)

This prevents indefinite blocking
```

---

## 5. Gossip Protocol

### Overview

**Use Case**: Eventual consistency, cluster membership
**Used by**: Cassandra, Riak, Consul

### How Gossip Works

```
Every node periodically:
1. Picks random peer
2. Exchanges state information
3. Merges state

Example:
Node A: {A: v1, B: v2, C: v1}
Node B: {A: v1, B: v3, C: v2}

A gossips with B:
A receives: B: v3, C: v2
A updates: {A: v1, B: v3, C: v2}

Eventually all nodes converge to same state
```

### Gossip Implementation

```java
public class GossipNode {
    
    private String nodeId;
    private Map<String, NodeState> clusterState = new ConcurrentHashMap<>();
    private List<String> peers;
    
    @Scheduled(fixedRate = 1000) // Every 1 second
    public void gossip() {
        // Pick random peer
        String peer = peers.get(ThreadLocalRandom.current().nextInt(peers.size()));
        
        // Send state to peer
        Map<String, NodeState> peerState = sendStateAndReceive(peer, clusterState);
        
        // Merge states
        mergeState(peerState);
    }
    
    private void mergeState(Map<String, NodeState> peerState) {
        peerState.forEach((nodeId, peerNodeState) -> {
            NodeState myNodeState = clusterState.get(nodeId);
            
            if (myNodeState == null || peerNodeState.getVersion() > myNodeState.getVersion()) {
                // Peer has newer state
                clusterState.put(nodeId, peerNodeState);
            }
        });
    }
    
    // Handle incoming gossip
    public Map<String, NodeState> receiveGossip(Map<String, NodeState> senderState) {
        // Merge sender's state
        mergeState(senderState);
        
        // Return my state
        return new HashMap<>(clusterState);
    }
}
```

### Gossip Convergence

```
Time to converge: O(log N) rounds
  N = number of nodes

Example: 1000 nodes
  Round 1: 1 node knows
  Round 2: 2 nodes know
  Round 3: 4 nodes know
  Round 4: 8 nodes know
  ...
  Round 10: 1024 nodes know (all)
```

---

## 6. Vector Clocks

### Overview

**Use Case**: Detect causality and conflicts in distributed systems
**Used by**: Riak, Voldemort

### How Vector Clocks Work

```
Each node maintains a vector of counters:
Node A: [A:1, B:0, C:0]
Node B: [A:0, B:1, C:0]
Node C: [A:0, B:0, C:1]

On write:
1. Increment own counter
2. Attach vector clock to data

On read:
1. Compare vector clocks
2. Determine causality
```

### Vector Clock Comparison

```
V1 = [A:2, B:1, C:0]
V2 = [A:1, B:1, C:0]

V1 > V2 (V1 happened after V2)
  Because: V1[A] > V2[A] and V1[B] >= V2[B] and V1[C] >= V2[C]

V1 = [A:2, B:1, C:0]
V2 = [A:1, B:2, C:0]

V1 || V2 (concurrent, conflict!)
  Because: V1[A] > V2[A] but V1[B] < V2[B]
```

### Vector Clock Implementation

```java
public class VectorClock {
    
    private Map<String, Long> clock = new HashMap<>();
    
    // Increment own counter
    public void increment(String nodeId) {
        clock.put(nodeId, clock.getOrDefault(nodeId, 0L) + 1);
    }
    
    // Merge with another vector clock
    public void merge(VectorClock other) {
        other.clock.forEach((nodeId, timestamp) -> {
            clock.put(nodeId, Math.max(clock.getOrDefault(nodeId, 0L), timestamp));
        });
    }
    
    // Compare with another vector clock
    public Ordering compare(VectorClock other) {
        boolean allLessOrEqual = true;
        boolean allGreaterOrEqual = true;
        
        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(clock.keySet());
        allNodes.addAll(other.clock.keySet());
        
        for (String nodeId : allNodes) {
            long myTimestamp = clock.getOrDefault(nodeId, 0L);
            long otherTimestamp = other.clock.getOrDefault(nodeId, 0L);
            
            if (myTimestamp > otherTimestamp) {
                allLessOrEqual = false;
            }
            if (myTimestamp < otherTimestamp) {
                allGreaterOrEqual = false;
            }
        }
        
        if (allLessOrEqual && allGreaterOrEqual) {
            return Ordering.EQUAL;
        } else if (allLessOrEqual) {
            return Ordering.BEFORE; // This happened before other
        } else if (allGreaterOrEqual) {
            return Ordering.AFTER; // This happened after other
        } else {
            return Ordering.CONCURRENT; // Conflict!
        }
    }
}
```

---

## Comparison Table

| Algorithm | Use Case | Latency | Fault Tolerance | Complexity |
|-----------|----------|---------|-----------------|------------|
| **Paxos** | Leader election, consensus | High (2 RTT) | High (majority) | Very High |
| **Raft** | Leader election, log replication | Medium (1-2 RTT) | High (majority) | Medium |
| **2PC** | Distributed transactions | High (2 RTT) | Low (blocking) | Low |
| **3PC** | Distributed transactions | High (3 RTT) | Medium (non-blocking) | Medium |
| **Gossip** | Eventual consistency | Low (async) | Very High | Low |
| **Vector Clocks** | Conflict detection | Low | High | Medium |

---

## Real-World Usage

### Paxos
- **Google Chubby**: Distributed lock service
- **Apache ZooKeeper**: Coordination service (Paxos-inspired)

### Raft
- **etcd**: Kubernetes configuration store
- **Consul**: Service discovery
- **CockroachDB**: Distributed SQL database

### 2PC/3PC
- **MySQL XA**: Distributed transactions
- **PostgreSQL**: Two-phase commit
- **Oracle**: Distributed transactions

### Gossip
- **Cassandra**: Cluster membership, failure detection
- **Riak**: Anti-entropy, replication
- **Consul**: Service discovery

### Vector Clocks
- **Riak**: Conflict resolution
- **Voldemort**: Versioning

---

## Key Takeaways

### When to Use Each

**Paxos/Raft**:
- Need strong consistency
- Leader election
- Replicated state machines
- Can tolerate 2-3 RTT latency

**2PC/3PC**:
- ACID transactions across databases
- Strong consistency required
- Low write throughput acceptable

**Gossip**:
- Eventual consistency acceptable
- High availability required
- Large clusters (1000+ nodes)
- Failure detection

**Vector Clocks**:
- Need to detect conflicts
- Multi-master replication
- Eventual consistency

### Trade-offs

```
Strong Consistency (Paxos/Raft):
  ✅ Linearizability
  ✅ No conflicts
  ❌ Higher latency
  ❌ Lower availability

Eventual Consistency (Gossip):
  ✅ Low latency
  ✅ High availability
  ❌ Conflicts possible
  ❌ Stale reads
```

---

## Interview Answer Summary

**Question**: Explain distributed consensus algorithms

**Answer**:

**Paxos** (1989):
- 2-phase protocol (Prepare, Accept)
- Majority voting
- Hard to understand
- Used by: Google Chubby

**Raft** (2014):
- Easier alternative to Paxos
- Leader election + log replication
- Strong consistency
- Used by: etcd, Consul, CockroachDB

**2PC** (Two-Phase Commit):
- Distributed transactions
- Blocking (coordinator crash = stuck)
- Used by: MySQL XA, PostgreSQL

**Gossip Protocol**:
- Eventual consistency
- Epidemic-style propagation
- O(log N) convergence
- Used by: Cassandra, Riak

**Vector Clocks**:
- Detect causality and conflicts
- Multi-master replication
- Used by: Riak, Voldemort

**Key Trade-off**: Strong consistency (Paxos/Raft) vs High availability (Gossip)
