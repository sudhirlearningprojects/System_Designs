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
