# 4. Document AI & Vision

## Document AI (OCR + Extraction)

```python
from google.cloud import documentai_v1 as documentai

client = documentai.DocumentProcessorServiceClient()

# Process document (OCR + layout + tables)
processor_name = f"projects/my-project/locations/us/processors/PROCESSOR_ID"

with open("invoice.pdf", "rb") as f:
    raw_document = documentai.RawDocument(content=f.read(), mime_type="application/pdf")

request = documentai.ProcessRequest(name=processor_name, raw_document=raw_document)
result = client.process_document(request=request)
document = result.document

# Full text
print(document.text)

# Entities (for specialized processors: invoice, receipt, W2, etc.)
for entity in document.entities:
    print(f"{entity.type_}: {entity.mention_text} (confidence: {entity.confidence:.2f})")
    # INVOICE_DATE: 2024-01-15
    # TOTAL_AMOUNT: $1,234.56
    # VENDOR_NAME: Acme Corp

# Tables
for page in document.pages:
    for table in page.tables:
        print(f"Table: {len(table.header_rows)} headers, {len(table.body_rows)} rows")
        for row in table.body_rows:
            cells = [cell.layout.text_anchor.content for cell in row.cells]
            print(f"  Row: {cells}")

# Layout (paragraphs, blocks, sections)
for page in document.pages:
    for paragraph in page.paragraphs:
        text = get_text(paragraph.layout, document)
        print(f"Paragraph: {text[:100]}")
```

### Batch Processing

```bash
# Process many documents asynchronously
gcloud document-ai processors batch-process \
  --processor=PROCESSOR_ID \
  --location=us \
  --input-documents=gs://my-bucket/input/ \
  --output-gcs-destination=gs://my-bucket/output/
```

---

## Cloud Vision API

```python
from google.cloud import vision

client = vision.ImageAnnotatorClient()

# Object detection
with open("image.jpg", "rb") as f:
    image = vision.Image(content=f.read())

response = client.object_localization(image=image)
for obj in response.localized_object_annotations:
    print(f"{obj.name}: {obj.score:.2f} at {obj.bounding_poly}")

# Text detection (OCR)
response = client.text_detection(image=image)
print(f"Full text: {response.text_annotations[0].description}")

# Label detection
response = client.label_detection(image=image)
for label in response.label_annotations:
    print(f"{label.description}: {label.score:.2f}")

# Face detection
response = client.face_detection(image=image)
for face in response.face_annotations:
    print(f"Joy: {face.joy_likelihood}, Anger: {face.anger_likelihood}")

# Safe search (content moderation)
response = client.safe_search_detection(image=image)
safe = response.safe_search_annotation
print(f"Adult: {safe.adult}, Violence: {safe.violence}")
```

---

## Next: [Speech & Translation →](05_Speech_Translation.md)
