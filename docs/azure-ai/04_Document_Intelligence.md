# 4. Azure Document Intelligence (formerly Form Recognizer)

## Overview

Azure Document Intelligence extracts text, tables, key-value pairs, and structure from documents (PDFs, images, Office files) using AI. Critical for RAG pipelines that need to process complex documents.

---

## Setup

```bash
az cognitiveservices account create \
  --name my-doc-intel \
  --resource-group rg-ai \
  --kind FormRecognizer \
  --sku S0 \
  --location eastus
```

---

## Pre-Built Models

| Model | Extracts | Use Case |
|-------|----------|----------|
| `prebuilt-read` | Text, paragraphs, languages | General OCR |
| `prebuilt-layout` | Text + tables + figures + sections | Complex documents |
| `prebuilt-invoice` | Invoice fields (vendor, total, line items) | AP automation |
| `prebuilt-receipt` | Receipt fields (merchant, total, items) | Expense management |
| `prebuilt-idDocument` | ID fields (name, DOB, address) | Identity verification |
| `prebuilt-tax.us.w2` | W-2 tax form fields | Tax processing |

---

## Layout Analysis (Best for RAG)

```python
# pip install azure-ai-documentintelligence

from azure.ai.documentintelligence import DocumentIntelligenceClient
from azure.ai.documentintelligence.models import AnalyzeDocumentRequest
from azure.core.credentials import AzureKeyCredential

client = DocumentIntelligenceClient(
    endpoint="https://my-doc-intel.cognitiveservices.azure.com/",
    credential=AzureKeyCredential("your-key"),
)

# Analyze PDF with layout model (extracts tables, figures, sections)
with open("complex_report.pdf", "rb") as f:
    poller = client.begin_analyze_document(
        "prebuilt-layout",
        body=f,
        content_type="application/pdf",
        output_content_format="markdown",  # Get markdown output!
    )

result = poller.result()

# Get markdown representation (perfect for RAG chunking)
print(result.content)  # Full document as markdown with tables preserved

# Access structured elements
for table in result.tables:
    print(f"Table: {table.row_count} rows × {table.column_count} cols")
    for cell in table.cells:
        print(f"  [{cell.row_index},{cell.column_index}]: {cell.content}")

for paragraph in result.paragraphs:
    print(f"Paragraph (role={paragraph.role}): {paragraph.content[:100]}")

# Figures with captions
for figure in result.figures:
    print(f"Figure: {figure.caption.content if figure.caption else 'No caption'}")
```

### Integration with RAG Pipeline

```python
from llama_index.core import Document

def extract_documents_for_rag(pdf_path: str) -> list[Document]:
    """Extract PDF content using Document Intelligence for RAG indexing."""
    with open(pdf_path, "rb") as f:
        poller = client.begin_analyze_document(
            "prebuilt-layout", body=f, output_content_format="markdown"
        )
    result = poller.result()
    
    # Create LlamaIndex documents with rich metadata
    documents = []
    for i, page in enumerate(result.pages):
        page_content = extract_page_content(result, page_number=i+1)
        documents.append(Document(
            text=page_content,
            metadata={
                "source": pdf_path,
                "page": i + 1,
                "total_pages": len(result.pages),
                "has_tables": any(t.bounding_regions[0].page_number == i+1 for t in result.tables),
            }
        ))
    
    return documents
```

---

## Next: [Azure AI Speech & Language →](05_Speech_Language.md)
