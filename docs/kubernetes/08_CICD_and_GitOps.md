# 8. CI/CD & GitOps

## GitOps Principles

```
┌─────────────────────────────────────────────────────────────┐
│                     GitOps Workflow                           │
│                                                              │
│  Developer → Git Push → CI Pipeline → Image Build → Push    │
│                              │                               │
│                              ▼                               │
│  Git Repo (manifests) ← Update image tag                    │
│         │                                                    │
│         ▼                                                    │
│  GitOps Controller (ArgoCD/Flux) → Sync → Kubernetes        │
│         │                                                    │
│         ▼                                                    │
│  Drift Detection → Auto-heal if cluster != Git              │
└─────────────────────────────────────────────────────────────┘
```

**Core Principles:**
1. **Git as single source of truth** — all desired state in Git
2. **Declarative** — describe what, not how
3. **Automated reconciliation** — controller syncs cluster to Git
4. **Self-healing** — drift is automatically corrected

---

## Repository Structure

### Monorepo Approach

```
├── apps/
│   ├── payment-service/
│   │   ├── base/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── hpa.yaml
│   │   │   └── kustomization.yaml
│   │   └── overlays/
│   │       ├── development/
│   │       │   ├── kustomization.yaml
│   │       │   └── patches/
│   │       ├── staging/
│   │       │   ├── kustomization.yaml
│   │       │   └── patches/
│   │       └── production/
│   │           ├── kustomization.yaml
│   │           └── patches/
│   └── user-service/
│       └── ...
├── infrastructure/
│   ├── monitoring/
│   ├── ingress/
│   └── cert-manager/
└── clusters/
    ├── dev-cluster/
    ├── staging-cluster/
    └── prod-cluster/
```

---

## Kustomize

### Base

```yaml
# apps/payment-service/base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
  - service.yaml
  - hpa.yaml
  - pdb.yaml
commonLabels:
  app.kubernetes.io/name: payment-service
  app.kubernetes.io/managed-by: kustomize
```

### Production Overlay

```yaml
# apps/payment-service/overlays/production/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: production
resources:
  - ../../base
patches:
  - target:
      kind: Deployment
      name: payment-service
    patch: |
      - op: replace
        path: /spec/replicas
        value: 5
      - op: replace
        path: /spec/template/spec/containers/0/image
        value: registry.company.com/payment-service:3.2.1
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/cpu
        value: 500m
      - op: replace
        path: /spec/template/spec/containers/0/resources/requests/memory
        value: 512Mi
configMapGenerator:
  - name: payment-config
    envs:
      - config.env
secretGenerator:
  - name: payment-secrets
    envs:
      - secrets.env  # Use sealed-secrets or external-secrets in practice
images:
  - name: payment-service
    newName: registry.company.com/payment-service
    newTag: "3.2.1"
```

---

## Helm

### Chart Structure

```
payment-service/
├── Chart.yaml
├── values.yaml
├── values-production.yaml
├── templates/
│   ├── _helpers.tpl
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── hpa.yaml
│   ├── pdb.yaml
│   ├── serviceaccount.yaml
│   ├── servicemonitor.yaml
│   └── ingress.yaml
└── charts/           # Dependencies
```

### values-production.yaml

```yaml
replicaCount: 5

image:
  repository: registry.company.com/payment-service
  tag: "3.2.1"
  pullPolicy: IfNotPresent

resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: "1"
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 5
  maxReplicas: 50
  targetCPUUtilizationPercentage: 70

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: api.example.com
      paths:
        - path: /v1/payments
          pathType: Prefix
  tls:
    - secretName: api-tls
      hosts:
        - api.example.com

serviceMonitor:
  enabled: true
  interval: 15s

podDisruptionBudget:
  enabled: true
  minAvailable: 2
```

### Helm Template (deployment.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "payment-service.fullname" . }}
  labels:
    {{- include "payment-service.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "payment-service.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      annotations:
        checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
      labels:
        {{- include "payment-service.selectorLabels" . | nindent 8 }}
    spec:
      serviceAccountName: {{ include "payment-service.serviceAccountName" . }}
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          ports:
            - containerPort: 8080
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
```

---

## ArgoCD

### Application Definition

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: payment-service-production
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: production
  source:
    repoURL: https://github.com/company/k8s-manifests.git
    targetRevision: main
    path: apps/payment-service/overlays/production
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true        # Delete resources removed from Git
      selfHeal: true     # Revert manual changes
      allowEmpty: false
    syncOptions:
      - CreateNamespace=true
      - PrunePropagationPolicy=foreground
      - PruneLast=true
      - ApplyOutOfSyncOnly=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
  ignoreDifferences:
    - group: apps
      kind: Deployment
      jsonPointers:
        - /spec/replicas  # Ignore HPA-managed replicas
```

### ApplicationSet (Multi-Environment)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: payment-service
  namespace: argocd
spec:
  generators:
    - list:
        elements:
          - cluster: dev
            url: https://dev-cluster.example.com
            namespace: development
            revision: develop
          - cluster: staging
            url: https://staging-cluster.example.com
            namespace: staging
            revision: release
          - cluster: production
            url: https://prod-cluster.example.com
            namespace: production
            revision: main
  template:
    metadata:
      name: "payment-service-{{cluster}}"
    spec:
      project: "{{cluster}}"
      source:
        repoURL: https://github.com/company/k8s-manifests.git
        targetRevision: "{{revision}}"
        path: "apps/payment-service/overlays/{{cluster}}"
      destination:
        server: "{{url}}"
        namespace: "{{namespace}}"
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
```

### ArgoCD Project (RBAC)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: production
  namespace: argocd
spec:
  description: Production applications
  sourceRepos:
    - "https://github.com/company/k8s-manifests.git"
    - "https://charts.company.com"
  destinations:
    - namespace: production
      server: https://kubernetes.default.svc
  clusterResourceWhitelist:
    - group: ""
      kind: Namespace
  namespaceResourceWhitelist:
    - group: "*"
      kind: "*"
  roles:
    - name: deployer
      policies:
        - p, proj:production:deployer, applications, sync, production/*, allow
        - p, proj:production:deployer, applications, get, production/*, allow
      groups:
        - platform-team
```

---

## CI Pipeline (GitHub Actions)

```yaml
name: Build and Deploy
on:
  push:
    branches: [main]
    paths: ["src/payment-service/**"]

env:
  REGISTRY: registry.company.com
  IMAGE_NAME: payment-service

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.version }}
    steps:
      - uses: actions/checkout@v4

      - name: Run tests
        run: ./gradlew test

      - name: Build image
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

      - name: Scan image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
          severity: "CRITICAL,HIGH"
          exit-code: "1"

      - name: Sign image
        run: cosign sign --key env://COSIGN_KEY ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          repository: company/k8s-manifests
          token: ${{ secrets.GIT_TOKEN }}

      - name: Update image tag
        run: |
          cd apps/payment-service/overlays/production
          kustomize edit set image payment-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

      - name: Commit and push
        run: |
          git config user.name "CI Bot"
          git config user.email "ci@company.com"
          git commit -am "deploy: payment-service ${{ github.sha }}"
          git push
        # ArgoCD detects the change and syncs automatically
```

---

## Progressive Delivery (Argo Rollouts)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: payment-service
spec:
  replicas: 10
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
        - name: payment
          image: registry.company.com/payment-service:3.2.1
  strategy:
    canary:
      canaryService: payment-service-canary
      stableService: payment-service-stable
      trafficRouting:
        istio:
          virtualServices:
            - name: payment-service-vsvc
              routes:
                - primary
      steps:
        - setWeight: 5
        - pause: {duration: 5m}
        - analysis:
            templates:
              - templateName: success-rate
            args:
              - name: service-name
                value: payment-service-canary
        - setWeight: 20
        - pause: {duration: 10m}
        - analysis:
            templates:
              - templateName: success-rate
        - setWeight: 50
        - pause: {duration: 10m}
        - setWeight: 100
---
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  args:
    - name: service-name
  metrics:
    - name: success-rate
      interval: 1m
      count: 5
      successCondition: result[0] >= 0.99
      failureLimit: 3
      provider:
        prometheus:
          address: http://prometheus.monitoring:9090
          query: |
            sum(rate(http_server_requests_seconds_count{service="{{args.service-name}}",status!~"5.."}[5m]))
            / sum(rate(http_server_requests_seconds_count{service="{{args.service-name}}"}[5m]))
```

---

## Sealed Secrets (GitOps-Safe Secrets)

```bash
# Encrypt secret for Git storage
kubeseal --format yaml \
  --cert https://sealed-secrets-controller.kube-system/v1/cert.pem \
  < secret.yaml > sealed-secret.yaml
```

```yaml
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: db-credentials
  namespace: production
spec:
  encryptedData:
    password: AgBy3i4OJSWK+PiTySYZZA9rO43cGDEq...
    username: AgCtr8OJSWK+PiTySYZZA9rO43cGDEq...
  template:
    metadata:
      name: db-credentials
      namespace: production
    type: Opaque
```

---

## Deployment Best Practices

1. **Never deploy directly** — always through Git + GitOps controller
2. **Image tags are immutable** — use SHA or semantic versions, never `latest`
3. **Separate app and infra repos** — different change velocity
4. **Progressive delivery** — canary with automated analysis
5. **Automated rollback** — on failed health checks or metrics
6. **Drift detection** — alert on manual cluster changes
7. **Environment promotion** — dev → staging → production via Git
8. **Secrets in external stores** — never in Git (use External Secrets or Sealed Secrets)

---

## Next: [Troubleshooting →](09_Troubleshooting.md)
