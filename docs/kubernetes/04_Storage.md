# 4. Storage

## Storage Architecture

```
┌─────────────────────────────────────────────────────┐
│                      POD                             │
│  ┌───────────┐     ┌──────────────────────────┐    │
│  │ Container │────→│ volumeMount: /data        │    │
│  └───────────┘     └──────────┬───────────────┘    │
└────────────────────────────────┼────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────┐
│              VOLUME (in Pod spec)                     │
│  Types: emptyDir, configMap, secret, PVC, hostPath  │
└────────────────────────────────┬────────────────────┘
                                 │ (if PVC)
┌────────────────────────────────▼────────────────────┐
│         PersistentVolumeClaim (PVC)                   │
│         "I need 100Gi of fast SSD storage"           │
└────────────────────────────────┬────────────────────┘
                                 │ (bound)
┌────────────────────────────────▼────────────────────┐
│          PersistentVolume (PV)                        │
│          Actual storage resource                     │
└────────────────────────────────┬────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────┐
│         StorageClass (Dynamic Provisioner)            │
│         CSI Driver → Cloud Provider API              │
└─────────────────────────────────────────────────────┘
```

---

## Volume Types

### emptyDir (Ephemeral)

Temporary storage that lives with the Pod. Deleted when Pod is removed.

```yaml
spec:
  containers:
    - name: app
      volumeMounts:
        - name: cache
          mountPath: /tmp/cache
        - name: shared-data
          mountPath: /data
    - name: sidecar
      volumeMounts:
        - name: shared-data
          mountPath: /data
  volumes:
    - name: cache
      emptyDir:
        sizeLimit: 1Gi
    - name: shared-data
      emptyDir:
        medium: Memory  # tmpfs (RAM-backed, faster)
        sizeLimit: 256Mi
```

### ConfigMap and Secret Volumes

```yaml
spec:
  containers:
    - name: app
      volumeMounts:
        - name: config
          mountPath: /etc/config
          readOnly: true
        - name: certs
          mountPath: /etc/tls
          readOnly: true
  volumes:
    - name: config
      configMap:
        name: app-config
        items:
          - key: application.yaml
            path: application.yaml
    - name: certs
      secret:
        secretName: tls-certs
        defaultMode: 0400  # Read-only for owner
```

### Projected Volumes (Multiple Sources)

```yaml
spec:
  volumes:
    - name: all-config
      projected:
        sources:
          - configMap:
              name: app-config
          - secret:
              name: app-secrets
          - serviceAccountToken:
              path: token
              expirationSeconds: 3600
              audience: vault
          - downwardAPI:
              items:
                - path: labels
                  fieldRef:
                    fieldPath: metadata.labels
```

---

## PersistentVolumes and Claims

### PersistentVolume (PV)

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-database
  labels:
    type: ssd
    environment: production
spec:
  capacity:
    storage: 500Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: gp3-encrypted
  csi:
    driver: ebs.csi.aws.com
    volumeHandle: vol-0abc123def456
    fsType: ext4
  nodeAffinity:
    required:
      nodeSelectorTerms:
        - matchExpressions:
            - key: topology.kubernetes.io/zone
              operator: In
              values: ["us-east-1a"]
```

### PersistentVolumeClaim (PVC)

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data
  namespace: database
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3-encrypted
  resources:
    requests:
      storage: 500Gi
  selector:
    matchLabels:
      type: ssd
```

### Access Modes

| Mode | Abbreviation | Description |
|------|-------------|-------------|
| ReadWriteOnce | RWO | Single node read-write |
| ReadOnlyMany | ROX | Multiple nodes read-only |
| ReadWriteMany | RWX | Multiple nodes read-write |
| ReadWriteOncePod | RWOP | Single pod read-write (K8s 1.27+) |

### Reclaim Policies

| Policy | Behavior |
|--------|----------|
| Retain | Keep PV after PVC deletion (manual cleanup) |
| Delete | Delete PV and underlying storage |
| Recycle | Deprecated — use dynamic provisioning |

---

## StorageClasses

Dynamic provisioning — automatically create PVs when PVCs are created.

### AWS EBS (gp3)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-encrypted
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
  kmsKeyId: "arn:aws:kms:us-east-1:123456789:key/abc-123"
reclaimPolicy: Delete
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer  # Bind when Pod is scheduled
mountOptions:
  - noatime
```

### AWS EFS (Shared Filesystem)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: efs-shared
provisioner: efs.csi.aws.com
parameters:
  provisioningMode: efs-ap
  fileSystemId: fs-0abc123
  directoryPerms: "700"
  basePath: "/dynamic_provisioning"
  subPathPattern: "${.PVC.namespace}/${.PVC.name}"
  ensureUniqueDirectory: "true"
mountOptions:
  - tls
  - iam
```

### High-Performance (io2)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: io2-high-perf
provisioner: ebs.csi.aws.com
parameters:
  type: io2
  iops: "64000"
  encrypted: "true"
reclaimPolicy: Retain
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer
```

---

## Volume Expansion

```yaml
# StorageClass must have: allowVolumeExpansion: true

# Expand PVC (online for most CSI drivers)
kubectl patch pvc postgres-data -p '{"spec":{"resources":{"requests":{"storage":"1Ti"}}}}'
```

---

## Volume Snapshots

```yaml
# Create snapshot
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: postgres-snapshot-20240101
spec:
  volumeSnapshotClassName: ebs-snapshot-class
  source:
    persistentVolumeClaimName: postgres-data
---
# Restore from snapshot
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data-restored
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3-encrypted
  resources:
    requests:
      storage: 500Gi
  dataSource:
    name: postgres-snapshot-20240101
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
---
# VolumeSnapshotClass
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshotClass
metadata:
  name: ebs-snapshot-class
driver: ebs.csi.aws.com
deletionPolicy: Retain
parameters:
  tagSpecification_1: "backup=true"
```

---

## CSI Drivers

Container Storage Interface — standard for storage plugins.

### Common CSI Drivers

| Driver | Storage | Features |
|--------|---------|----------|
| ebs.csi.aws.com | AWS EBS | Block, snapshots, encryption |
| efs.csi.aws.com | AWS EFS | Shared filesystem (NFS) |
| pd.csi.storage.gke.io | GCP PD | Block, regional PDs |
| disk.csi.azure.com | Azure Disk | Block, snapshots |
| secrets-store.csi.k8s.io | Secrets | Mount secrets from Vault/AWS SM |

### Secrets Store CSI Driver

Mount secrets from external stores (Vault, AWS Secrets Manager) as volumes:

```yaml
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: aws-secrets
spec:
  provider: aws
  parameters:
    objects: |
      - objectName: "production/db-password"
        objectType: "secretsmanager"
      - objectName: "/production/api-key"
        objectType: "ssmparameter"
  secretObjects:
    - secretName: db-credentials
      type: Opaque
      data:
        - objectName: "production/db-password"
          key: password
---
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: app-sa  # IRSA for AWS auth
  containers:
    - name: app
      volumeMounts:
        - name: secrets
          mountPath: /mnt/secrets
          readOnly: true
  volumes:
    - name: secrets
      csi:
        driver: secrets-store.csi.k8s.io
        readOnly: true
        volumeAttributes:
          secretProviderClass: aws-secrets
```

---

## Ephemeral Volumes (K8s 1.25+)

For temporary storage that follows Pod lifecycle but needs CSI features:

```yaml
spec:
  containers:
    - name: app
      volumeMounts:
        - name: scratch
          mountPath: /tmp/work
  volumes:
    - name: scratch
      ephemeral:
        volumeClaimTemplate:
          spec:
            accessModes: ["ReadWriteOnce"]
            storageClassName: gp3-encrypted
            resources:
              requests:
                storage: 50Gi
```

---

## Storage Best Practices

1. **Always use StorageClasses** — avoid manual PV creation
2. **Use `WaitForFirstConsumer`** — ensures PV is in same AZ as Pod
3. **Enable volume expansion** — avoid PVC recreation
4. **Use `Retain` for databases** — prevent accidental data loss
5. **Snapshot before upgrades** — backup critical data
6. **Use EFS/NFS for shared access** — when multiple Pods need RWX
7. **Set resource limits on emptyDir** — prevent node disk exhaustion
8. **Encrypt at rest** — use KMS-encrypted StorageClasses
9. **Monitor PV usage** — alert before running out of space
10. **Use RWOP for databases** — prevents accidental multi-attach

---

## Next: [Security →](05_Security.md)
