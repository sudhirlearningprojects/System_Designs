# 5. Security

## Security Layers

```
┌─────────────────────────────────────────────────────────┐
│                  SUPPLY CHAIN SECURITY                    │
│         Image scanning, signing, SBOM, admission         │
├─────────────────────────────────────────────────────────┤
│                  CLUSTER SECURITY                         │
│         RBAC, API audit, etcd encryption                 │
├─────────────────────────────────────────────────────────┤
│                  NETWORK SECURITY                         │
│         Network Policies, mTLS, encryption in transit    │
├─────────────────────────────────────────────────────────┤
│                  WORKLOAD SECURITY                        │
│         Pod Security, SecurityContext, Secrets            │
├─────────────────────────────────────────────────────────┤
│                  RUNTIME SECURITY                         │
│         Falco, seccomp, AppArmor, read-only rootfs       │
├─────────────────────────────────────────────────────────┤
│                  DATA SECURITY                            │
│         Encryption at rest, secrets management           │
└─────────────────────────────────────────────────────────┘
```

---

## RBAC (Role-Based Access Control)

### Core Concepts

| Object | Scope | Purpose |
|--------|-------|---------|
| Role | Namespace | Define permissions in a namespace |
| ClusterRole | Cluster | Define permissions cluster-wide |
| RoleBinding | Namespace | Grant Role to user/group/SA |
| ClusterRoleBinding | Cluster | Grant ClusterRole cluster-wide |

### Role and RoleBinding

```yaml
# Role: what can be done
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: deployment-manager
  namespace: production
rules:
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch", "create", "update", "patch"]
  - apiGroups: [""]
    resources: ["pods", "pods/log"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["configmaps"]
    verbs: ["get", "list"]
    resourceNames: ["app-config"]  # Specific resource only
---
# RoleBinding: who gets the role
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: dev-team-deployment-manager
  namespace: production
subjects:
  - kind: Group
    name: dev-team
    apiGroup: rbac.authorization.k8s.io
  - kind: ServiceAccount
    name: ci-deployer
    namespace: ci-cd
roleRef:
  kind: Role
  name: deployment-manager
  apiGroup: rbac.authorization.k8s.io
```

### ClusterRole (Cluster-Wide)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: readonly-all
rules:
  - apiGroups: ["*"]
    resources: ["*"]
    verbs: ["get", "list", "watch"]
  - nonResourceURLs: ["/healthz", "/metrics"]
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: sre-readonly
subjects:
  - kind: Group
    name: sre-team
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: readonly-all
  apiGroup: rbac.authorization.k8s.io
```

### Aggregated ClusterRoles

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring-view
  labels:
    rbac.authorization.k8s.io/aggregate-to-view: "true"  # Auto-added to 'view'
rules:
  - apiGroups: ["monitoring.coreos.com"]
    resources: ["prometheuses", "alertmanagers", "servicemonitors"]
    verbs: ["get", "list", "watch"]
```

### ServiceAccount for Workloads

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: payment-service-sa
  namespace: production
  annotations:
    # AWS IRSA (IAM Roles for Service Accounts)
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789:role/payment-service-role
automountServiceAccountToken: false  # Disable auto-mount
---
# Pod uses the ServiceAccount
spec:
  serviceAccountName: payment-service-sa
  automountServiceAccountToken: true  # Explicitly enable if needed
```

### RBAC Best Practices

1. **Principle of least privilege** — grant minimum required permissions
2. **Use Groups** — bind roles to groups, not individual users
3. **Namespace isolation** — use Roles over ClusterRoles when possible
4. **Audit regularly** — `kubectl auth can-i --list --as=user@example.com`
5. **Disable default SA token** — set `automountServiceAccountToken: false`
6. **Use IRSA/Workload Identity** — for cloud API access from Pods

---

## Pod Security

### Pod Security Standards (PSS)

Replaced PodSecurityPolicy (removed in K8s 1.25).

| Level | Description |
|-------|-------------|
| **Privileged** | No restrictions (system workloads) |
| **Baseline** | Prevents known privilege escalations |
| **Restricted** | Hardened, follows best practices |

### Enforce via Namespace Labels

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    # Enforce restricted (reject non-compliant pods)
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: latest
    # Warn on baseline violations
    pod-security.kubernetes.io/warn: restricted
    pod-security.kubernetes.io/warn-version: latest
    # Audit for logging
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/audit-version: latest
```

### SecurityContext (Pod Level)

```yaml
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    runAsGroup: 3000
    fsGroup: 2000
    fsGroupChangePolicy: OnRootMismatch
    seccompProfile:
      type: RuntimeDefault
    supplementalGroups: [4000]
```

### SecurityContext (Container Level)

```yaml
spec:
  containers:
    - name: app
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        runAsNonRoot: true
        runAsUser: 1000
        capabilities:
          drop:
            - ALL
          add:
            - NET_BIND_SERVICE  # Only if binding to port < 1024
        seccompProfile:
          type: RuntimeDefault
      volumeMounts:
        - name: tmp
          mountPath: /tmp
  volumes:
    - name: tmp
      emptyDir: {}  # Writable tmp since rootfs is read-only
```

### Production-Hardened Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: hardened-app
spec:
  serviceAccountName: app-sa
  automountServiceAccountToken: false
  securityContext:
    runAsNonRoot: true
    runAsUser: 65534  # nobody
    fsGroup: 65534
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: app
      image: registry.company.com/app:1.0@sha256:abc123...  # Pin by digest
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop: ["ALL"]
      resources:
        requests:
          cpu: 100m
          memory: 128Mi
        limits:
          cpu: 200m
          memory: 256Mi
      volumeMounts:
        - name: tmp
          mountPath: /tmp
  volumes:
    - name: tmp
      emptyDir:
        sizeLimit: 100Mi
  hostNetwork: false
  hostPID: false
  hostIPC: false
```

---

## Secrets Management

### Kubernetes Secrets (Basic)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: production
type: Opaque
data:
  username: cG9zdGdyZXM=      # base64 encoded (NOT encrypted!)
  password: c3VwZXJzZWNyZXQ=
---
# Use in Pod
spec:
  containers:
    - name: app
      env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
      # Or mount as file
      volumeMounts:
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
  volumes:
    - name: secrets
      secret:
        secretName: db-credentials
```

### Encryption at Rest

```yaml
# /etc/kubernetes/encryption-config.yaml
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
  - resources:
      - secrets
    providers:
      - aescbc:
          keys:
            - name: key1
              secret: <base64-encoded-32-byte-key>
      - identity: {}  # Fallback for reading unencrypted
```

### External Secrets Operator (Production)

```yaml
# Connect to AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: ClusterSecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
            namespace: external-secrets
---
# Sync secret from AWS to K8s
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-credentials
  namespace: production
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: db-credentials
    creationPolicy: Owner
  data:
    - secretKey: password
      remoteRef:
        key: production/database
        property: password
    - secretKey: username
      remoteRef:
        key: production/database
        property: username
```

### HashiCorp Vault Integration

```yaml
# Vault Agent Injector annotations
metadata:
  annotations:
    vault.hashicorp.com/agent-inject: "true"
    vault.hashicorp.com/role: "payment-service"
    vault.hashicorp.com/agent-inject-secret-db: "secret/data/production/db"
    vault.hashicorp.com/agent-inject-template-db: |
      {{- with secret "secret/data/production/db" -}}
      export DB_HOST={{ .Data.data.host }}
      export DB_PASSWORD={{ .Data.data.password }}
      {{- end }}
```

---

## Admission Controllers

### Validating Webhook (OPA Gatekeeper)

```yaml
# Constraint Template
apiVersion: templates.gatekeeper.sh/v1
kind: ConstraintTemplate
metadata:
  name: k8srequiredlabels
spec:
  crd:
    spec:
      names:
        kind: K8sRequiredLabels
      validation:
        openAPIV3Schema:
          type: object
          properties:
            labels:
              type: array
              items:
                type: string
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package k8srequiredlabels
        violation[{"msg": msg}] {
          provided := {label | input.review.object.metadata.labels[label]}
          required := {label | label := input.parameters.labels[_]}
          missing := required - provided
          count(missing) > 0
          msg := sprintf("Missing required labels: %v", [missing])
        }
---
# Constraint (enforce the template)
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sRequiredLabels
metadata:
  name: require-team-label
spec:
  match:
    kinds:
      - apiGroups: ["apps"]
        kinds: ["Deployment"]
    namespaces: ["production"]
  parameters:
    labels:
      - "team"
      - "app.kubernetes.io/name"
```

### Kyverno Policies

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: disallow-latest-tag
spec:
  validationFailureAction: Enforce
  rules:
    - name: require-image-tag
      match:
        any:
          - resources:
              kinds: ["Pod"]
      validate:
        message: "Image tag 'latest' is not allowed. Use specific version tags."
        pattern:
          spec:
            containers:
              - image: "!*:latest"
    - name: require-image-digest
      match:
        any:
          - resources:
              kinds: ["Pod"]
              namespaces: ["production"]
      validate:
        message: "Production images must use digest (@sha256:...)"
        pattern:
          spec:
            containers:
              - image: "*@sha256:*"
```

---

## Supply Chain Security

### Image Signing (Cosign/Sigstore)

```bash
# Sign image
cosign sign --key cosign.key registry.company.com/app:1.0

# Verify in admission controller (Kyverno)
```

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: verify-image-signature
spec:
  validationFailureAction: Enforce
  rules:
    - name: verify-signature
      match:
        any:
          - resources:
              kinds: ["Pod"]
      verifyImages:
        - imageReferences:
            - "registry.company.com/*"
          attestors:
            - entries:
                - keys:
                    publicKeys: |
                      -----BEGIN PUBLIC KEY-----
                      MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...
                      -----END PUBLIC KEY-----
```

### Image Scanning (Trivy)

```yaml
# Trivy Operator - auto-scan all images in cluster
apiVersion: aquasecurity.github.io/v1alpha1
kind: VulnerabilityReport
metadata:
  name: payment-service-vuln
spec:
  scanner:
    name: Trivy
    version: 0.45.0
  registry:
    server: registry.company.com
  artifact:
    repository: payment-service
    tag: "3.2.1"
```

---

## Audit Logging

```yaml
# Audit policy
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
  # Log all secret access at Metadata level
  - level: Metadata
    resources:
      - group: ""
        resources: ["secrets"]
  # Log all changes to deployments
  - level: RequestResponse
    resources:
      - group: "apps"
        resources: ["deployments"]
    verbs: ["create", "update", "patch", "delete"]
  # Don't log read-only requests to certain resources
  - level: None
    resources:
      - group: ""
        resources: ["events", "endpoints"]
  # Log everything else at Metadata level
  - level: Metadata
    omitStages:
      - RequestReceived
```

---

## Security Checklist

### Cluster Level
- [ ] Enable RBAC (default since 1.8)
- [ ] Encrypt etcd at rest
- [ ] Enable audit logging
- [ ] Restrict API server access (firewall/VPN)
- [ ] Use private cluster (no public API endpoint)
- [ ] Rotate certificates regularly
- [ ] Keep Kubernetes version updated

### Workload Level
- [ ] Run as non-root
- [ ] Read-only root filesystem
- [ ] Drop all capabilities
- [ ] Use SecurityContext on all Pods
- [ ] Pin images by digest
- [ ] Scan images for vulnerabilities
- [ ] Use Pod Security Standards (restricted)
- [ ] Set resource limits

### Network Level
- [ ] Default deny NetworkPolicies
- [ ] Enable mTLS (service mesh)
- [ ] Encrypt traffic in transit
- [ ] Restrict egress traffic

### Secrets Level
- [ ] Use external secrets manager (Vault/AWS SM)
- [ ] Enable encryption at rest
- [ ] Rotate secrets regularly
- [ ] Never commit secrets to Git
- [ ] Use short-lived tokens (IRSA/Workload Identity)

---

## Next: [Production Best Practices →](06_Production_Best_Practices.md)
