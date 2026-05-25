# 7. Amazon Q

## Amazon Q Business (Enterprise AI Assistant)

Amazon Q Business connects to your enterprise data (40+ connectors) and provides an AI assistant that answers questions grounded in your company's knowledge.

```bash
# Create Amazon Q Business application
aws qbusiness create-application \
  --display-name "Company Knowledge Assistant" \
  --role-arn "arn:aws:iam::123456789:role/QBusinessRole"

# Add data source (e.g., S3, Confluence, SharePoint)
aws qbusiness create-data-source \
  --application-id "app-id" \
  --index-id "index-id" \
  --display-name "Internal Docs" \
  --configuration '{
    "type": "S3",
    "s3Configuration": {
      "bucketName": "company-docs",
      "inclusionPrefixes": ["policies/", "procedures/", "faq/"]
    }
  }'

# Sync data
aws qbusiness start-data-source-sync-job \
  --application-id "app-id" \
  --index-id "index-id" \
  --data-source-id "ds-id"
```

### Query via API

```python
import boto3

q_client = boto3.client("qbusiness", region_name="us-east-1")

response = q_client.chat_sync(
    applicationId="app-id",
    userId="user@company.com",
    userMessage="What is our PTO policy?",
)

print(response["systemMessage"])
# Grounded answer from your company docs

for source in response.get("sourceAttributions", []):
    print(f"Source: {source['title']} - {source['url']}")
```

---

## Amazon Q Developer (Code Assistant)

Amazon Q Developer (formerly CodeWhisperer) provides AI-powered code generation, debugging, and transformation directly in your IDE.

### Key Features

| Feature | Description |
|---------|-------------|
| **Code completion** | Real-time suggestions as you type |
| **Chat** | Ask questions about code, AWS, debugging |
| **Code transformation** | Upgrade Java 8→17, Python 2→3 |
| **Security scanning** | Find vulnerabilities in code |
| **Agent for software development** | `/dev` command for multi-file changes |

### IDE Integration

```bash
# Available in:
# - VS Code (Amazon Q extension)
# - JetBrains IDEs
# - AWS Cloud9
# - CLI (amazon-q-cli)

# CLI usage
amazon-q chat "How do I set up a VPC with private subnets in CDK?"
amazon-q transform --source ./legacy-app --target-version java17
```

### Security Scanning

```bash
# Scan code for vulnerabilities
amazon-q scan --path ./src

# Output:
# CRITICAL: SQL Injection in src/db.py:45
# HIGH: Hardcoded credentials in src/config.py:12
# MEDIUM: Insecure random in src/auth.py:78
```

---

## Next: [Production Patterns →](08_Production_Patterns.md)
