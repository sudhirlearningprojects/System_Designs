# 9. Troubleshooting

## Debugging Workflow

```
Problem Detected
       │
       ▼
┌─────────────────┐     ┌──────────────────┐
│ Check Pod Status│────►│ Pending?         │──► Check events, scheduling
└────────┬────────┘     │ CrashLoopBackOff?│──► Check logs, probes
         │              │ ImagePullBackOff? │──► Check image, registry
         │              │ OOMKilled?       │──► Increase memory limits
         │              └──────────────────┘
         ▼
┌─────────────────┐
│ Check Service   │──► Endpoints exist? Selector matches?
└────────┬────────┘
         ▼
┌─────────────────┐
│ Check Ingress   │──► TLS valid? Backend healthy? Path correct?
└────────┬────────┘
         ▼
┌─────────────────┐
│ Check Network   │──► NetworkPolicy blocking? DNS resolving?
└────────┬────────┘
         ▼
┌─────────────────┐
│ Check Node      │──► Disk pressure? Memory pressure? PID pressure?
└─────────────────┘
```

---

## Essential Commands

### Pod Debugging

```bash
# Get pod status and events
kubectl get pods -n production -o wide
kubectl describe pod <pod-name> -n production

# Check logs
kubectl logs <pod-name> -n production
kubectl logs <pod-name> -n production --previous  # Previous crashed container
kubectl logs <pod-name> -n production -c <container>  # Specific container
kubectl logs -l app=payment-service -n production --tail=100  # By label

# Stream logs
kubectl logs -f <pod-name> -n production

# Execute into pod
kubectl exec -it <pod-name> -n production -- /bin/sh

# Debug with ephemeral container (no shell in image)
kubectl debug -it <pod-name> -n production --image=busybox:1.36 --target=app

# Copy files from pod
kubectl cp production/<pod-name>:/tmp/heap-dump.hprof ./heap-dump.hprof

# Port forward for local debugging
kubectl port-forward <pod-name> 8080:8080 -n production
kubectl port-forward svc/payment-service 8080:80 -n production
```

### Resource Inspection

```bash
# Get all resources in namespace
kubectl get all -n production

# Wide output with node info
kubectl get pods -o wide -n production

# Custom columns
kubectl get pods -o custom-columns=\
NAME:.metadata.name,\
STATUS:.status.phase,\
NODE:.spec.nodeName,\
IP:.status.podIP,\
RESTARTS:.status.containerStatuses[0].restartCount

# JSON path queries
kubectl get pods -o jsonpath='{.items[*].status.containerStatuses[*].restartCount}'

# Sort by restart count
kubectl get pods --sort-by='.status.containerStatuses[0].restartCount' -n production
```

### Node Debugging

```bash
# Node status
kubectl get nodes -o wide
kubectl describe node <node-name>

# Node resource usage
kubectl top nodes
kubectl top pods -n production --sort-by=memory

# Check node conditions
kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.conditions[?(@.type=="Ready")].status}{"\n"}{end}'

# Debug node (creates privileged pod on node)
kubectl debug node/<node-name> -it --image=ubuntu

# Drain node for maintenance
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data --grace-period=60

# Cordon (prevent new scheduling)
kubectl cordon <node-name>
kubectl uncordon <node-name>
```

---

## Common Issues and Solutions

### 1. Pod Stuck in Pending

**Causes:**
| Cause | Diagnosis | Fix |
|-------|-----------|-----|
| Insufficient resources | Events show "Insufficient cpu/memory" | Add nodes or reduce requests |
| No matching nodes | Affinity/nodeSelector mismatch | Fix selectors or label nodes |
| PVC not bound | PVC in Pending state | Check StorageClass, AZ |
| Taint not tolerated | Events show taint rejection | Add toleration or remove taint |
| ResourceQuota exceeded | Events show quota exceeded | Increase quota or reduce usage |

```bash
# Diagnose
kubectl describe pod <pod-name> | grep -A 20 Events
kubectl get events --sort-by='.lastTimestamp' -n production
```

### 2. CrashLoopBackOff

**Causes:**
- Application error on startup
- Missing config/secrets
- Failing health probes
- OOMKilled

```bash
# Check exit code
kubectl describe pod <pod-name> | grep -A 5 "Last State"

# Check logs from crashed container
kubectl logs <pod-name> --previous

# Common exit codes:
# 0   = Success (shouldn't restart)
# 1   = Application error
# 137 = OOMKilled (128 + 9 SIGKILL)
# 143 = SIGTERM (128 + 15)
```

**Fix OOMKilled:**
```yaml
resources:
  limits:
    memory: 1Gi  # Increase limit
# Also check for memory leaks in application
```

### 3. ImagePullBackOff

```bash
# Check events
kubectl describe pod <pod-name> | grep -A 5 Events

# Common causes:
# - Image doesn't exist (typo in tag)
# - Private registry without imagePullSecrets
# - Rate limited (Docker Hub)
```

**Fix:**
```yaml
spec:
  imagePullSecrets:
    - name: registry-credentials
  containers:
    - image: registry.company.com/app:1.0  # Verify exact tag exists
```

### 4. Service Not Routing Traffic

```bash
# Check endpoints exist
kubectl get endpoints <service-name> -n production

# If empty endpoints:
# 1. Verify selector matches pod labels
kubectl get svc <service-name> -o yaml | grep -A 5 selector
kubectl get pods -l app=payment-service -n production

# 2. Check pod readiness
kubectl get pods -n production | grep payment

# 3. Test from within cluster
kubectl run debug --rm -it --image=busybox -- wget -qO- http://payment-service.production:80/health
```

### 5. DNS Resolution Failures

```bash
# Test DNS from a pod
kubectl run dns-test --rm -it --image=busybox -- nslookup payment-service.production.svc.cluster.local

# Check CoreDNS pods
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl logs -n kube-system -l k8s-app=kube-dns

# Check CoreDNS config
kubectl get configmap coredns -n kube-system -o yaml

# Common fix: restart CoreDNS
kubectl rollout restart deployment coredns -n kube-system
```

### 6. Ingress Not Working

```bash
# Check ingress status
kubectl describe ingress <ingress-name> -n production

# Check ingress controller logs
kubectl logs -n ingress -l app.kubernetes.io/name=ingress-nginx

# Verify backend service
kubectl get svc <backend-service> -n production
kubectl get endpoints <backend-service> -n production

# Test from ingress controller pod
kubectl exec -it -n ingress <nginx-pod> -- curl http://payment-service.production:80/health
```

### 7. PVC Stuck in Pending

```bash
# Check PVC events
kubectl describe pvc <pvc-name> -n production

# Common causes:
# - StorageClass doesn't exist
# - No available PV matching requirements
# - AZ mismatch (WaitForFirstConsumer helps)
# - Quota exceeded

# Check StorageClass
kubectl get storageclass
kubectl describe storageclass <class-name>
```

### 8. Node NotReady

```bash
# Check node conditions
kubectl describe node <node-name> | grep -A 20 Conditions

# Common conditions:
# MemoryPressure  = Node running out of memory
# DiskPressure    = Node running out of disk
# PIDPressure     = Too many processes
# NetworkUnavailable = Network not configured

# SSH to node and check
systemctl status kubelet
journalctl -u kubelet --since "10 minutes ago"
df -h  # Disk space
free -m  # Memory
```

### 9. RBAC Permission Denied

```bash
# Check if user/SA can perform action
kubectl auth can-i create deployments -n production --as=system:serviceaccount:ci-cd:deployer
kubectl auth can-i --list --as=user@example.com -n production

# Check role bindings
kubectl get rolebindings -n production
kubectl get clusterrolebindings | grep <user-or-group>

# Describe binding to see subjects
kubectl describe rolebinding <binding-name> -n production
```

### 10. High Pod Latency

```bash
# Check resource throttling
kubectl top pods -n production
# If CPU usage near limit → being throttled

# Check if HPA is maxed out
kubectl get hpa -n production

# Check node resource pressure
kubectl top nodes

# Network issues
kubectl exec -it <pod> -- ping <other-pod-ip>
kubectl exec -it <pod> -- curl -w "@curl-format.txt" http://service/endpoint
```

---

## Disaster Recovery

### etcd Backup and Restore

```bash
# Backup
ETCDCTL_API=3 etcdctl snapshot save /backup/etcd-$(date +%Y%m%d-%H%M).db \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/etcd/ca.crt \
  --cert=/etc/etcd/server.crt \
  --key=/etc/etcd/server.key

# Verify backup
etcdctl snapshot status /backup/etcd-20240115-1000.db --write-out=table

# Restore (stop API server first!)
etcdctl snapshot restore /backup/etcd-20240115-1000.db \
  --data-dir=/var/lib/etcd-restored \
  --initial-cluster="etcd1=https://etcd1:2380,etcd2=https://etcd2:2380,etcd3=https://etcd3:2380" \
  --initial-cluster-token=etcd-cluster-1 \
  --initial-advertise-peer-urls=https://etcd1:2380
```

### Velero (Cluster Backup)

```bash
# Install Velero with AWS plugin
velero install \
  --provider aws \
  --plugins velero/velero-plugin-for-aws:v1.8.0 \
  --bucket velero-backups \
  --backup-location-config region=us-east-1 \
  --snapshot-location-config region=us-east-1

# Create backup
velero backup create production-backup \
  --include-namespaces production \
  --include-resources deployments,services,configmaps,secrets,pvc

# Schedule daily backups
velero schedule create daily-production \
  --schedule="0 2 * * *" \
  --include-namespaces production \
  --ttl 720h

# Restore
velero restore create --from-backup production-backup \
  --include-namespaces production
```

### DR Strategy

| RPO/RTO | Strategy | Tools |
|---------|----------|-------|
| RPO=0, RTO<5min | Active-Active multi-region | Global LB + replicated state |
| RPO<1h, RTO<30min | Active-Passive with replication | Velero + cross-region backup |
| RPO<24h, RTO<4h | Scheduled backups + restore | Velero + etcd snapshots |

---

## Performance Debugging

### Memory Leak Detection

```bash
# Watch memory growth over time
kubectl top pods -n production --sort-by=memory -w

# Get heap dump (Java)
kubectl exec <pod> -- jcmd 1 GC.heap_dump /tmp/heap.hprof
kubectl cp production/<pod>:/tmp/heap.hprof ./heap.hprof

# Check OOMKill history
kubectl get events -n production --field-selector reason=OOMKilling
```

### Network Debugging

```bash
# DNS lookup timing
kubectl exec <pod> -- time nslookup payment-service.production.svc.cluster.local

# TCP connectivity
kubectl exec <pod> -- nc -zv payment-service 8080

# HTTP timing
kubectl exec <pod> -- curl -w "DNS: %{time_namelookup}s\nConnect: %{time_connect}s\nTTFB: %{time_starttransfer}s\nTotal: %{time_total}s\n" -o /dev/null -s http://payment-service:80/health

# Packet capture (requires privileged debug pod)
kubectl debug node/<node> -it --image=nicolaka/netshoot -- tcpdump -i any port 8080 -w /tmp/capture.pcap
```

---

## Useful Debugging Tools

| Tool | Purpose | Command |
|------|---------|---------|
| **kubectl debug** | Ephemeral debug containers | `kubectl debug -it pod/x --image=busybox` |
| **netshoot** | Network debugging | `kubectl run net --rm -it --image=nicolaka/netshoot -- bash` |
| **k9s** | Terminal UI for K8s | `k9s -n production` |
| **stern** | Multi-pod log tailing | `stern payment -n production` |
| **kubectx/kubens** | Context/namespace switching | `kubens production` |
| **kube-capacity** | Resource usage overview | `kube-capacity --util` |

---

## Runbook Template

```markdown
## Alert: PaymentServiceHighErrorRate

### Severity: Critical
### SLO Impact: Availability

### Symptoms
- Error rate > 5% for payment-service
- Users seeing payment failures

### Diagnosis Steps
1. Check pod health: `kubectl get pods -n production -l app=payment-service`
2. Check recent deployments: `kubectl rollout history deployment/payment-service -n production`
3. Check logs: `stern payment-service -n production --since 10m | grep ERROR`
4. Check dependencies: `kubectl exec <pod> -- curl http://postgres:5432`
5. Check metrics: Grafana dashboard → Payment Service

### Resolution
1. If recent deployment → rollback: `kubectl rollout undo deployment/payment-service -n production`
2. If dependency down → check dependency service
3. If resource exhaustion → scale: `kubectl scale deployment/payment-service --replicas=10 -n production`
4. If unknown → escalate to on-call engineer

### Prevention
- Add integration tests for payment flow
- Improve canary analysis thresholds
- Add circuit breaker for downstream dependencies
```

---

## Summary

This guide covers the complete Kubernetes production lifecycle:
1. **Core Concepts** — Architecture and fundamentals
2. **Workloads** — Deployments, StatefulSets, Jobs
3. **Networking** — Services, Ingress, Network Policies
4. **Storage** — PV/PVC, StorageClasses, CSI
5. **Security** — RBAC, Pod Security, Secrets
6. **Production** — HA, Autoscaling, Resource Management
7. **Observability** — Metrics, Logging, Tracing
8. **CI/CD** — GitOps, ArgoCD, Progressive Delivery
9. **Troubleshooting** — Debugging, DR, Common Issues

---

## Quick Reference

```bash
# Most used commands
kubectl get pods -n production -o wide
kubectl describe pod <pod> -n production
kubectl logs <pod> -n production --tail=100
kubectl exec -it <pod> -n production -- /bin/sh
kubectl top pods -n production --sort-by=cpu
kubectl get events --sort-by='.lastTimestamp' -n production
kubectl rollout status deployment/<name> -n production
kubectl rollout undo deployment/<name> -n production
```
