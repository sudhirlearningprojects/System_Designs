# Accounts Merge
**LeetCode 721** | Medium | Union-Find / DFS

## Problem
Given a list of accounts where `accounts[i][0]` is the name and the rest are emails,
merge accounts that share at least one email. Return merged accounts sorted.

```
Input: accounts = [["John","johnsmith@mail.com","john_newyork@mail.com"],
                   ["John","johnsmith@mail.com","john00@mail.com"],
                   ["Mary","mary@mail.com"],
                   ["John","johnnybravo@mail.com"]]
Output: [["John","john00@mail.com","john_newyork@mail.com","johnsmith@mail.com"],
         ["John","johnnybravo@mail.com"],
         ["Mary","mary@mail.com"]]
```

## Approach 1: Union-Find (Optimal)
Map each email to an account index. Union accounts that share emails.

**Time:** O(N·K·α(N)) where N=accounts, K=max emails | **Space:** O(N·K)

```java
public List<List<String>> accountsMerge(List<List<String>> accounts) {
    int n = accounts.size();
    int[] parent = new int[n], rank = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;

    Map<String, Integer> emailToAccount = new HashMap<>();
    for (int i = 0; i < n; i++) {
        for (int j = 1; j < accounts.get(i).size(); j++) {
            String email = accounts.get(i).get(j);
            if (emailToAccount.containsKey(email))
                union(parent, rank, i, emailToAccount.get(email));
            else
                emailToAccount.put(email, i);
        }
    }

    // Group emails by root account
    Map<Integer, TreeSet<String>> groups = new HashMap<>();
    for (Map.Entry<String, Integer> entry : emailToAccount.entrySet()) {
        int root = find(parent, entry.getValue());
        groups.computeIfAbsent(root, k -> new TreeSet<>()).add(entry.getKey());
    }

    List<List<String>> result = new ArrayList<>();
    for (Map.Entry<Integer, TreeSet<String>> entry : groups.entrySet()) {
        List<String> account = new ArrayList<>();
        account.add(accounts.get(entry.getKey()).get(0)); // name
        account.addAll(entry.getValue());
        result.add(account);
    }
    return result;
}

private int find(int[] p, int x) { return p[x] == x ? x : (p[x] = find(p, p[x])); }
private void union(int[] p, int[] r, int x, int y) {
    int px = find(p, x), py = find(p, y);
    if (px == py) return;
    if (r[px] < r[py]) { int t = px; px = py; py = t; }
    p[py] = px; if (r[px] == r[py]) r[px]++;
}
```

## Approach 2: DFS on Email Graph
Build a graph where emails are nodes, connected if they appear in the same account.

**Time:** O(N·K·log(N·K)) | **Space:** O(N·K)

```java
public List<List<String>> accountsMerge(List<List<String>> accounts) {
    Map<String, String> emailToName = new HashMap<>();
    Map<String, List<String>> adj = new HashMap<>();

    for (List<String> account : accounts) {
        String name = account.get(0);
        for (int i = 1; i < account.size(); i++) {
            emailToName.put(account.get(i), name);
            adj.computeIfAbsent(account.get(i), k -> new ArrayList<>());
            if (i > 1) {
                adj.get(account.get(1)).add(account.get(i));
                adj.get(account.get(i)).add(account.get(1));
            }
        }
    }

    Set<String> visited = new HashSet<>();
    List<List<String>> result = new ArrayList<>();
    for (String email : adj.keySet()) {
        if (visited.contains(email)) continue;
        List<String> component = new ArrayList<>();
        dfs(adj, visited, email, component);
        Collections.sort(component);
        component.add(0, emailToName.get(email));
        result.add(component);
    }
    return result;
}

private void dfs(Map<String, List<String>> adj, Set<String> visited, String email, List<String> component) {
    visited.add(email);
    component.add(email);
    for (String nb : adj.get(email))
        if (!visited.contains(nb)) dfs(adj, visited, nb, component);
}
```

## Key Insight
Treat each account as a node. Two accounts should merge if they share an email.
Union-Find elegantly handles transitive merging: if A shares email with B, and B shares email with C,
all three merge into one group through path compression.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| No shared emails | Each account separate | No merging needed |
| All accounts share one email | One merged account | Transitive merging |
| Same name, different emails | Separate accounts | Name alone doesn’t merge |
| Different names, same email | Merged (same person) | Email is the identity |
| Single account | Returned as-is | No merging |
| Chain: A↔B↔C (B shares with both) | A+B+C merged | Transitive via B |

---

## Dry Run

**Input:**
```
accounts = [
  ["John", "a@x.com", "b@x.com"],   // account 0
  ["John", "b@x.com", "c@x.com"],   // account 1
  ["Mary", "d@x.com"]               // account 2
]
```

**Union-Find trace:**
```
Initial: parent=[0,1,2]

Process account 0:
  "a@x.com" → not seen → emailToAccount["a@x.com"]=0
  "b@x.com" → not seen → emailToAccount["b@x.com"]=0

Process account 1:
  "b@x.com" → already seen at account 0 → union(1, 0)
    find(1)=1, find(0)=0 → different → parent[1]=0
    parent=[0,0,2]
  "c@x.com" → not seen → emailToAccount["c@x.com"]=1

Process account 2:
  "d@x.com" → not seen → emailToAccount["d@x.com"]=2

Group emails by root:
  "a@x.com" → account 0, find(0)=0 → group[0]
  "b@x.com" → account 0, find(0)=0 → group[0]
  "c@x.com" → account 1, find(1)=0 → group[0]  ← merged!
  "d@x.com" → account 2, find(2)=2 → group[2]

Result:
  group[0]: name="John", emails=[a@x.com, b@x.com, c@x.com] (sorted)
  group[2]: name="Mary", emails=[d@x.com]
```

---

## Follow-up Questions

**Q: Why use `TreeSet` for emails?**
The problem requires emails to be sorted. `TreeSet` maintains sorted order automatically.

**Q: What if two accounts have the same name but should NOT be merged?**
Name is irrelevant for merging — only shared emails trigger a merge. Two "John" accounts with no shared emails remain separate.

**Q: How to handle very large inputs efficiently?**
Union-Find with path compression is O(α(n)) per operation — effectively O(1). The bottleneck is sorting emails: O(N·K·log(N·K)).

**Q: What if an email appears in 3+ accounts?**
Each occurrence unions the current account with the first account that had that email. Transitivity handles the rest.

**Related Problems:** LC 323 (Number of Connected Components), LC 684 (Redundant Connection), LC 547 (Number of Provinces)
