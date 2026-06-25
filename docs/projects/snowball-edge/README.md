# AWS Snowball Edge - Deep Dive Learning Project

## Edge Data Processing & Migration Lab

A comprehensive hands-on project to learn AWS Snowball Edge capabilities, compare with alternatives, and understand edge computing patterns.

---

## 📚 Document Structure

| Part | Title | Description |
|------|-------|-------------|
| [Part 1](Part1_Overview_and_Architecture.md) | Overview & Architecture | Project overview, use cases, architecture, and device specifications |
| [Part 2](Part2_Setup_and_Data_Transfer.md) | Setup & Data Transfer | Device setup, S3 interface, NFS gateway, and bulk data ingestion |
| [Part 3](Part3_Edge_Computing.md) | Edge Computing | Lambda@Edge, EC2 on Snowball, EKS Anywhere, and ML inference |
| [Part 4](Part4_Advanced_Features.md) | Advanced Features | Clustering, IoT Greengrass, OpsHub, and operational patterns |
| [Part 5](Part5_Comparison_and_Alternatives.md) | Comparison & Alternatives | AWS vs Azure vs GCP, cost analysis, and decision framework |
| [Part 6](Part6_Local_Simulation_and_Testing.md) | Local Simulation & Testing | Test without physical device using LocalStack, MinIO, Docker |

---

## 🎯 Learning Objectives

After completing this project, you will understand:

1. **When to use Snowball Edge** vs network transfer vs other appliances
2. **Edge compute patterns** - Running Lambda, EC2, and Kubernetes disconnected from the cloud
3. **Data migration strategies** - S3 interface, NFS, clustering for large datasets
4. **Hybrid architectures** - Combining edge and cloud processing
5. **Security model** - Encryption, tamper-evidence, IAM on device
6. **Operational management** - OpsHub, monitoring, troubleshooting
7. **Multi-cloud comparison** - Azure Data Box, Azure Stack Edge, GCP Transfer Appliance

---

## 🏗️ Project Scenario

**Simulate a remote industrial site** (factory, oil rig, mining operation, or research vessel) that:
- Generates 50-100TB of IoT sensor data per month
- Has limited/no internet connectivity
- Requires local ML inference for real-time decisions
- Periodically ships data to the cloud for long-term analytics

---

## ⚡ Quick Start

If you want to jump in immediately:

1. Start with [Part 1](Part1_Overview_and_Architecture.md) for context
2. Skip to [Part 6](Part6_Local_Simulation_and_Testing.md) for hands-on simulation without a physical device
3. Then explore Parts 2-4 for detailed device operations
4. Finish with [Part 5](Part5_Comparison_and_Alternatives.md) for architectural decision-making

---

## 📋 Prerequisites

- AWS Account (for ordering device / using console)
- Python 3.9+ with boto3
- Docker & Docker Compose
- Basic understanding of S3, Lambda, EC2
- (Optional) Kubernetes knowledge for EKS Anywhere sections
