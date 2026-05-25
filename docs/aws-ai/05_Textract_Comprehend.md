# 5. Amazon Textract & Comprehend

## Amazon Textract (Document AI)

Extract text, tables, forms, and signatures from documents.

```python
import boto3

textract = boto3.client("textract", region_name="us-east-1")

# ============ Detect Text (OCR) ============
with open("document.pdf", "rb") as f:
    response = textract.detect_document_text(Document={"Bytes": f.read()})

for block in response["Blocks"]:
    if block["BlockType"] == "LINE":
        print(f"Text: {block['Text']} (Confidence: {block['Confidence']:.1f}%)")

# ============ Analyze Document (Tables + Forms) ============
# For multi-page PDFs, use async API with S3
response = textract.start_document_analysis(
    DocumentLocation={"S3Object": {"Bucket": "my-bucket", "Name": "report.pdf"}},
    FeatureTypes=["TABLES", "FORMS", "SIGNATURES", "LAYOUT"],
)
job_id = response["JobId"]

# Poll for results
import time
while True:
    result = textract.get_document_analysis(JobId=job_id)
    if result["JobStatus"] in ["SUCCEEDED", "FAILED"]:
        break
    time.sleep(5)

# Extract tables
for block in result["Blocks"]:
    if block["BlockType"] == "TABLE":
        print(f"Table found: {block['RowCount']} rows × {block['ColumnCount']} cols")
    elif block["BlockType"] == "KEY_VALUE_SET":
        # Form fields (key-value pairs)
        pass

# ============ Analyze Expense (Invoices/Receipts) ============
response = textract.analyze_expense(
    Document={"S3Object": {"Bucket": "my-bucket", "Name": "invoice.pdf"}}
)

for doc in response["ExpenseDocuments"]:
    for field in doc["SummaryFields"]:
        print(f"{field['Type']['Text']}: {field['ValueDetection']['Text']}")
        # VENDOR_NAME: Acme Corp
        # TOTAL: $1,234.56
        # INVOICE_RECEIPT_DATE: 2024-01-15
```

### Integration with RAG

```python
def extract_for_rag(s3_bucket: str, s3_key: str) -> list[dict]:
    """Extract document content for RAG indexing."""
    response = textract.start_document_analysis(
        DocumentLocation={"S3Object": {"Bucket": s3_bucket, "Name": s3_key}},
        FeatureTypes=["TABLES", "LAYOUT"],
    )
    # ... wait for completion ...
    
    # Convert to structured chunks for embedding
    chunks = []
    current_section = ""
    for block in result["Blocks"]:
        if block["BlockType"] == "LAYOUT_SECTION_HEADER":
            current_section = block["Text"]
        elif block["BlockType"] == "LINE":
            chunks.append({
                "text": block["Text"],
                "metadata": {"section": current_section, "page": block["Page"], "source": s3_key},
            })
    return chunks
```

---

## Amazon Comprehend (NLP)

```python
comprehend = boto3.client("comprehend", region_name="us-east-1")

text = "I'm really frustrated with the billing error on my Pro account. Please fix this ASAP!"

# ============ Sentiment Analysis ============
response = comprehend.detect_sentiment(Text=text, LanguageCode="en")
print(f"Sentiment: {response['Sentiment']}")  # NEGATIVE
print(f"Scores: {response['SentimentScore']}")
# {'Positive': 0.01, 'Negative': 0.92, 'Neutral': 0.05, 'Mixed': 0.02}

# ============ Entity Recognition ============
response = comprehend.detect_entities(Text=text, LanguageCode="en")
for entity in response["Entities"]:
    print(f"{entity['Type']}: {entity['Text']} (confidence: {entity['Score']:.2f})")
    # ORGANIZATION: Pro
    # OTHER: billing error

# ============ Key Phrases ============
response = comprehend.detect_key_phrases(Text=text, LanguageCode="en")
for phrase in response["KeyPhrases"]:
    print(f"Phrase: {phrase['Text']} ({phrase['Score']:.2f})")

# ============ PII Detection ============
response = comprehend.detect_pii_entities(Text="My email is user@example.com and SSN is 123-45-6789", LanguageCode="en")
for entity in response["Entities"]:
    print(f"PII: {entity['Type']} at offset {entity['BeginOffset']}-{entity['EndOffset']}")
    # EMAIL, SSN detected with positions

# ============ Custom Classification (train your own) ============
# Train on your labeled data for domain-specific classification
# e.g., support ticket routing: billing, technical, account, feedback
```

---

## Next: [Amazon Transcribe & Polly →](06_Transcribe_Polly.md)
