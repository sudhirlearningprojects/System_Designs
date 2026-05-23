# Kubernetes Production Guide

A comprehensive guide to Kubernetes architecture, production deployment, and best practices.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Core Concepts](01_Core_Concepts.md) | Architecture, components, objects, and fundamentals |
| 2 | [Workloads & Scheduling](02_Workloads_and_Scheduling.md) | Pods, Deployments, StatefulSets, DaemonSets, Jobs, scheduling |
| 3 | [Networking](03_Networking.md) | Services, Ingress, DNS, Network Policies, Service Mesh |
| 4 | [Storage](04_Storage.md) | Volumes, PV/PVC, StorageClasses, CSI drivers |
| 5 | [Security](05_Security.md) | RBAC, Pod Security, Secrets, Network Policies, Supply Chain |
| 6 | [Production Best Practices](06_Production_Best_Practices.md) | Resource management, HA, autoscaling, health checks |
| 7 | [Observability](07_Observability.md) | Monitoring, logging, tracing, alerting |
| 8 | [CI/CD & GitOps](08_CICD_and_GitOps.md) | Deployment strategies, ArgoCD, Flux, Helm |
| 9 | [Troubleshooting](09_Troubleshooting.md) | Debugging, common issues, disaster recovery |

## 🎯 Who Is This For?

- Engineers preparing for CKA/CKAD/CKS certifications
- Teams moving workloads to Kubernetes in production
- Architects designing cloud-native platforms
- SREs managing Kubernetes clusters at scale

## 🏗️ Production Stack Reference

```
┌─────────────────────────────────────────────────────┐
│                   Ingress (NGINX/Istio)              │
├─────────────────────────────────────────────────────┤
│              Service Mesh (Istio/Linkerd)            │
├─────────────────────────────────────────────────────┤
│   Deployments │ StatefulSets │ DaemonSets │ Jobs    │
├─────────────────────────────────────────────────────┤
│         Kubernetes Control Plane (HA)               │
├─────────────────────────────────────────────────────┤
│   Monitoring  │  Logging  │  Tracing  │  Alerting  │
├─────────────────────────────────────────────────────┤
│        Infrastructure (AWS EKS / GKE / AKS)         │
└─────────────────────────────────────────────────────┘
```
