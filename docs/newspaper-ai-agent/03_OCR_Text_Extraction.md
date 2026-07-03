# Module 3: OCR & Text Extraction

## 🎯 Learning Objectives

- Extract text from newspaper PDFs and images
- Use Google Vision API for high-accuracy OCR
- Implement Tesseract as free fallback
- Preserve layout metadata (font size, position, boldness)
- Preprocess images for better OCR accuracy

---

## 3.1 OCR Strategy

```
Newspaper File (PDF/Image)
         │
         ▼
┌─────────────────┐     ┌──────────────────┐
│ Is it a PDF?    │─Yes─▶│ Extract embedded │
│                 │      │ text (PyMuPDF)   │
└────────┬────────┘      └────────┬─────────┘
         │ No (Image)             │
         ▼                        ▼
┌─────────────────┐     ┌──────────────────┐
│ Preprocess      │     │ Has text layer?  │
│ (deskew, clean) │     │                  │
└────────┬────────┘     └───┬──────────┬───┘
         │                  │ Yes      │ No (scanned)
         ▼                  ▼          ▼
┌─────────────────┐   Return     Convert pages
│ Google Vision   │   text       to images, OCR
│ API / Tesseract │              each page
└────────┬────────┘
         │
         ▼
  Structured Text with Layout Info
```

---

## 3.2 Image Preprocessing

```python
# newspaper_agent/ocr/preprocessor.py

from PIL import Image, ImageFilter, ImageEnhance
import numpy as np
from pathlib import Path
from typing import List
import logging

logger = logging.getLogger(__name__)


class ImagePreprocessor:
    """Preprocess newspaper images for better OCR accuracy."""

    def preprocess(self, image_path: str) -> str:
        """
        Apply preprocessing pipeline to improve OCR quality.
        Returns path to processed image.
        """
        img = Image.open(image_path)

        # Convert to grayscale
        img = img.convert('L')

        # Increase contrast
        enhancer = ImageEnhance.Contrast(img)
        img = enhancer.enhance(1.5)

        # Sharpen
        img = img.filter(ImageFilter.SHARPEN)

        # Binarize (threshold)
        img = img.point(lambda x: 0 if x < 140 else 255, '1')

        # Save processed
        output_path = str(Path(image_path).with_suffix('.processed.png'))
        img.save(output_path)
        logger.info(f"Preprocessed: {image_path} → {output_path}")
        return output_path

    def deskew(self, image_path: str) -> str:
        """Fix rotation/skew in scanned newspapers."""
        try:
            import cv2
            img = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)

            # Detect edges and find angle
            edges = cv2.Canny(img, 50, 150, apertureSize=3)
            lines = cv2.HoughLinesP(
                edges, 1, np.pi/180, 100,
                minLineLength=100, maxLineGap=10
            )

            if lines is not None:
                angles = []
                for line in lines:
                    x1, y1, x2, y2 = line[0]
                    angle = np.degrees(np.arctan2(y2-y1, x2-x1))
                    if abs(angle) < 10:  # Only near-horizontal lines
                        angles.append(angle)

                if angles:
                    median_angle = np.median(angles)
                    if abs(median_angle) > 0.5:
                        # Rotate to correct
                        h, w = img.shape
                        center = (w//2, h//2)
                        M = cv2.getRotationMatrix2D(center, median_angle, 1.0)
                        rotated = cv2.warpAffine(img, M, (w, h),
                                                 borderValue=255)
                        output_path = str(
                            Path(image_path).with_suffix('.deskewed.png')
                        )
                        cv2.imwrite(output_path, rotated)
                        return output_path

            return image_path
        except ImportError:
            logger.warning("OpenCV not installed, skipping deskew")
            return image_path

    def split_columns(self, image_path: str) -> List[str]:
        """
        Split multi-column newspaper layout into separate columns.
        Helps OCR read in correct order.
        """
        img = Image.open(image_path)
        width, height = img.size

        # Simple 2-column split (heuristic)
        if width > height * 1.2:  # Landscape = likely multi-column
            mid = width // 2
            left = img.crop((0, 0, mid, height))
            right = img.crop((mid, 0, width, height))

            left_path = str(Path(image_path).with_suffix('.col1.png'))
            right_path = str(Path(image_path).with_suffix('.col2.png'))
            left.save(left_path)
            right.save(right_path)
            return [left_path, right_path]

        return [image_path]
```

---

## 3.3 Google Vision API OCR

```python
# newspaper_agent/ocr/vision_api.py

from google.cloud import vision
from google.oauth2 import service_account
from typing import List, Dict
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


class VisionAPIClient:
    """Google Cloud Vision API for high-accuracy OCR."""

    def __init__(self, credentials_path: str = "credentials.json"):
        credentials = service_account.Credentials.from_service_account_file(
            credentials_path
        )
        self.client = vision.ImageAnnotatorClient(credentials=credentials)

    def extract_text(self, image_path: str) -> Dict:
        """
        Extract text with full layout information.

        Returns:
            {
                'full_text': str,
                'blocks': [
                    {
                        'text': str,
                        'confidence': float,
                        'block_type': str,  # TEXT, TABLE, etc.
                        'bounding_box': {...},
                        'paragraphs': [...]
                    }
                ]
            }
        """
        with open(image_path, 'rb') as f:
            content = f.read()

        image = vision.Image(content=content)

        # Use document_text_detection for better newspaper handling
        response = self.client.document_text_detection(image=image)

        if response.error.message:
            raise Exception(f"Vision API error: {response.error.message}")

        full_text = response.full_text_annotation

        blocks = []
        if full_text and full_text.pages:
            for page in full_text.pages:
                for block in page.blocks:
                    block_text = self._extract_block_text(block)
                    blocks.append({
                        'text': block_text,
                        'confidence': block.confidence,
                        'block_type': block.block_type.name,
                        'bounding_box': self._get_bounds(block.bounding_box),
                        'paragraphs': self._extract_paragraphs(block),
                    })

        result = {
            'full_text': full_text.text if full_text else '',
            'blocks': blocks,
        }

        logger.info(
            f"Vision API: extracted {len(blocks)} blocks, "
            f"{len(result['full_text'])} chars from {image_path}"
        )
        return result

    def extract_text_from_pdf(self, pdf_path: str) -> List[Dict]:
        """
        Extract text from multi-page PDF using async batch.

        Returns list of page results.
        """
        from google.cloud import vision_v1

        with open(pdf_path, 'rb') as f:
            content = f.read()

        # For PDFs up to 5 pages, use sync
        input_config = vision_v1.InputConfig(
            content=content,
            mime_type='application/pdf'
        )

        # Process up to 5 pages per request
        feature = vision_v1.Feature(
            type_=vision_v1.Feature.Type.DOCUMENT_TEXT_DETECTION
        )

        request = vision_v1.AnnotateFileRequest(
            input_config=input_config,
            features=[feature],
            pages=[i for i in range(1, 21)]  # First 20 pages
        )

        response = self.client.batch_annotate_files(requests=[request])

        pages_result = []
        for page_response in response.responses[0].responses:
            full_text = page_response.full_text_annotation
            pages_result.append({
                'full_text': full_text.text if full_text else '',
                'page_number': len(pages_result) + 1,
            })

        return pages_result

    def _extract_block_text(self, block) -> str:
        """Extract all text from a block."""
        text_parts = []
        for paragraph in block.paragraphs:
            for word in paragraph.words:
                word_text = ''.join(
                    symbol.text for symbol in word.symbols
                )
                text_parts.append(word_text)
        return ' '.join(text_parts)

    def _extract_paragraphs(self, block) -> List[Dict]:
        """Extract paragraph-level details."""
        paragraphs = []
        for para in block.paragraphs:
            words = []
            for word in para.words:
                word_text = ''.join(s.text for s in word.symbols)
                words.append(word_text)
            paragraphs.append({
                'text': ' '.join(words),
                'confidence': para.confidence,
                'bounding_box': self._get_bounds(para.bounding_box),
            })
        return paragraphs

    def _get_bounds(self, bounding_box) -> Dict:
        """Convert bounding box to dict."""
        vertices = bounding_box.vertices
        return {
            'top_left': {'x': vertices[0].x, 'y': vertices[0].y},
            'top_right': {'x': vertices[1].x, 'y': vertices[1].y},
            'bottom_right': {'x': vertices[2].x, 'y': vertices[2].y},
            'bottom_left': {'x': vertices[3].x, 'y': vertices[3].y},
        }
```

---

## 3.4 Tesseract Fallback

```python
# newspaper_agent/ocr/tesseract.py

import pytesseract
from PIL import Image
from typing import Dict, List
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


class TesseractClient:
    """Tesseract OCR as free fallback engine."""

    def __init__(self, lang: str = 'eng'):
        self.lang = lang
        # Verify tesseract is installed
        try:
            pytesseract.get_tesseract_version()
        except Exception:
            raise RuntimeError(
                "Tesseract not installed. Install with:\n"
                "  macOS: brew install tesseract\n"
                "  Ubuntu: sudo apt install tesseract-ocr"
            )

    def extract_text(self, image_path: str) -> Dict:
        """
        Extract text with basic layout info using Tesseract.
        """
        img = Image.open(image_path)

        # Get detailed data including bounding boxes
        data = pytesseract.image_to_data(
            img, lang=self.lang, output_type=pytesseract.Output.DICT
        )

        # Reconstruct blocks from Tesseract output
        blocks = self._group_into_blocks(data)

        # Get full text
        full_text = pytesseract.image_to_string(img, lang=self.lang)

        return {
            'full_text': full_text,
            'blocks': blocks,
        }

    def extract_with_layout(self, image_path: str) -> List[Dict]:
        """
        Extract text preserving reading order and font size hints.
        Uses hOCR output for richer metadata.
        """
        img = Image.open(image_path)

        # Get hOCR output (HTML with bounding boxes)
        hocr = pytesseract.image_to_pdf_or_hocr(
            img, lang=self.lang, extension='hocr'
        )

        # Parse hOCR for layout info
        return self._parse_hocr(hocr.decode('utf-8'))

    def _group_into_blocks(self, data: Dict) -> List[Dict]:
        """Group Tesseract word-level output into text blocks."""
        blocks = {}

        for i in range(len(data['text'])):
            text = data['text'][i].strip()
            if not text:
                continue

            block_num = data['block_num'][i]
            if block_num not in blocks:
                blocks[block_num] = {
                    'text': '',
                    'confidence': 0,
                    'word_count': 0,
                    'bounding_box': {
                        'top_left': {'x': data['left'][i], 'y': data['top'][i]},
                        'width': data['width'][i],
                        'height': data['height'][i],
                    }
                }

            blocks[block_num]['text'] += ' ' + text
            blocks[block_num]['confidence'] += float(data['conf'][i])
            blocks[block_num]['word_count'] += 1

        # Average confidence
        result = []
        for block in blocks.values():
            if block['word_count'] > 0:
                block['confidence'] /= block['word_count']
                block['text'] = block['text'].strip()
                result.append(block)

        return result

    def _parse_hocr(self, hocr_html: str) -> List[Dict]:
        """Parse hOCR HTML for layout information."""
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(hocr_html, 'html.parser')
        blocks = []

        for area in soup.find_all('div', class_='ocr_carea'):
            block_text = area.get_text(separator=' ').strip()
            if block_text:
                # Extract bounding box from title attribute
                title = area.get('title', '')
                bbox = self._parse_bbox(title)
                blocks.append({
                    'text': block_text,
                    'bounding_box': bbox,
                    'confidence': 0.7,  # Tesseract default estimate
                })

        return blocks

    def _parse_bbox(self, title: str) -> Dict:
        """Parse bbox from hOCR title string."""
        import re
        match = re.search(r'bbox (\d+) (\d+) (\d+) (\d+)', title)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            return {
                'top_left': {'x': x1, 'y': y1},
                'bottom_right': {'x': x2, 'y': y2},
            }
        return {}
```

---

## 3.5 PDF Text Extraction (Digital PDFs)

```python
# newspaper_agent/ocr/pdf_extractor.py

import fitz  # PyMuPDF
from typing import List, Dict
from pathlib import Path
import logging

logger = logging.getLogger(__name__)


class PDFExtractor:
    """Extract text from digital PDFs (non-scanned)."""

    def extract(self, pdf_path: str) -> List[Dict]:
        """
        Extract text page by page with font metadata.

        Returns list of page dicts with text blocks including
        font size info (critical for headline detection).
        """
        doc = fitz.open(pdf_path)
        pages = []

        for page_num, page in enumerate(doc, 1):
            blocks = self._extract_page_blocks(page)
            full_text = page.get_text("text")

            pages.append({
                'page_number': page_num,
                'full_text': full_text,
                'blocks': blocks,
                'has_text': len(full_text.strip()) > 50,
            })

        doc.close()

        # Check if PDF is scanned (no text layer)
        text_pages = sum(1 for p in pages if p['has_text'])
        is_scanned = text_pages < len(pages) * 0.3

        logger.info(
            f"PDF: {len(pages)} pages, "
            f"{'SCANNED' if is_scanned else 'DIGITAL'} "
            f"({text_pages} pages with text)"
        )

        return pages, is_scanned

    def _extract_page_blocks(self, page) -> List[Dict]:
        """
        Extract text blocks with font size metadata.
        Font size is KEY for headline detection.
        """
        blocks = []
        block_dict = page.get_text("dict")

        for block in block_dict.get("blocks", []):
            if block["type"] != 0:  # Skip image blocks
                continue

            block_text = ""
            max_font_size = 0
            is_bold = False
            fonts_used = set()

            for line in block.get("lines", []):
                for span in line.get("spans", []):
                    block_text += span["text"] + " "
                    max_font_size = max(max_font_size, span["size"])
                    fonts_used.add(span["font"])

                    # Detect bold from font name
                    font_lower = span["font"].lower()
                    if "bold" in font_lower or "heavy" in font_lower:
                        is_bold = True

            block_text = block_text.strip()
            if block_text:
                blocks.append({
                    'text': block_text,
                    'font_size': max_font_size,
                    'is_bold': is_bold,
                    'fonts': list(fonts_used),
                    'bounding_box': {
                        'top_left': {'x': block["bbox"][0], 'y': block["bbox"][1]},
                        'bottom_right': {'x': block["bbox"][2], 'y': block["bbox"][3]},
                    },
                    'position_y': block["bbox"][1],  # Vertical position
                })

        return blocks

    def convert_to_images(self, pdf_path: str, dpi: int = 200) -> List[str]:
        """
        Convert PDF pages to images for OCR (for scanned PDFs).
        """
        doc = fitz.open(pdf_path)
        image_paths = []

        for page_num, page in enumerate(doc, 1):
            mat = fitz.Matrix(dpi/72, dpi/72)
            pix = page.get_pixmap(matrix=mat)

            output_path = str(
                Path(pdf_path).parent / f"page_{page_num:03d}.png"
            )
            pix.save(output_path)
            image_paths.append(output_path)

        doc.close()
        logger.info(f"Converted {len(image_paths)} PDF pages to images")
        return image_paths
```

---

## 3.6 Unified OCR Engine

```python
# newspaper_agent/ocr/engine.py

from typing import List, Dict
from pathlib import Path
from .preprocessor import ImagePreprocessor
from .vision_api import VisionAPIClient
from .tesseract import TesseractClient
from .pdf_extractor import PDFExtractor
import logging

logger = logging.getLogger(__name__)


class OCREngine:
    """
    Unified OCR engine that handles PDFs and images.
    Chooses the best extraction method automatically.
    """

    def __init__(
        self,
        primary: str = "vision_api",
        credentials_path: str = "credentials.json"
    ):
        self.preprocessor = ImagePreprocessor()
        self.pdf_extractor = PDFExtractor()

        if primary == "vision_api":
            self.primary_ocr = VisionAPIClient(credentials_path)
            self.fallback_ocr = TesseractClient()
        else:
            self.primary_ocr = TesseractClient()
            self.fallback_ocr = None

    def extract_from_file(self, file_path: str) -> List[Dict]:
        """
        Extract text from any supported file.

        Returns:
            List of page results, each with:
            - page_number: int
            - full_text: str
            - blocks: list of text blocks with metadata
        """
        path = Path(file_path)
        mime = self._detect_mime(path)

        if mime == 'application/pdf':
            return self._process_pdf(file_path)
        elif mime.startswith('image/'):
            return self._process_image(file_path)
        else:
            raise ValueError(f"Unsupported file type: {mime}")

    def _process_pdf(self, pdf_path: str) -> List[Dict]:
        """Process PDF - try text extraction first, fall back to OCR."""
        pages, is_scanned = self.pdf_extractor.extract(pdf_path)

        if not is_scanned:
            logger.info("PDF has text layer, using direct extraction")
            return pages

        # Scanned PDF - convert to images and OCR
        logger.info("Scanned PDF detected, converting to images for OCR")
        image_paths = self.pdf_extractor.convert_to_images(pdf_path)

        results = []
        for i, img_path in enumerate(image_paths, 1):
            page_result = self._process_image(img_path)
            if page_result:
                page_result[0]['page_number'] = i
                results.extend(page_result)

        return results

    def _process_image(self, image_path: str) -> List[Dict]:
        """Process a single image with preprocessing and OCR."""
        # Preprocess
        processed = self.preprocessor.preprocess(image_path)

        # Try primary OCR
        try:
            result = self.primary_ocr.extract_text(processed)
            return [{
                'page_number': 1,
                'full_text': result['full_text'],
                'blocks': result['blocks'],
            }]
        except Exception as e:
            logger.warning(f"Primary OCR failed: {e}")

            # Try fallback
            if self.fallback_ocr:
                logger.info("Trying fallback OCR (Tesseract)")
                result = self.fallback_ocr.extract_text(processed)
                return [{
                    'page_number': 1,
                    'full_text': result['full_text'],
                    'blocks': result['blocks'],
                }]

            raise

    def _detect_mime(self, path: Path) -> str:
        """Detect MIME type from file extension."""
        ext_map = {
            '.pdf': 'application/pdf',
            '.jpg': 'image/jpeg',
            '.jpeg': 'image/jpeg',
            '.png': 'image/png',
            '.tiff': 'image/tiff',
            '.tif': 'image/tiff',
            '.webp': 'image/webp',
        }
        return ext_map.get(path.suffix.lower(), 'application/octet-stream')
```

---

## 3.7 Installation & Dependencies

```bash
# System dependencies
# macOS
brew install tesseract
brew install poppler  # For PDF rendering

# Ubuntu/Debian
sudo apt install tesseract-ocr
sudo apt install poppler-utils

# Python dependencies
pip install google-cloud-vision==3.5.0
pip install pytesseract==0.3.10
pip install PyMuPDF==1.23.0
pip install Pillow==10.0.0
pip install opencv-python==4.8.0  # Optional, for deskew
pip install beautifulsoup4==4.12.0
```

---

## 3.8 Testing OCR

```python
# tests/test_ocr.py

from newspaper_agent.ocr.engine import OCREngine
from newspaper_agent.ocr.pdf_extractor import PDFExtractor


def test_pdf_text_extraction():
    """Test that digital PDFs extract text with font sizes."""
    extractor = PDFExtractor()
    pages, is_scanned = extractor.extract("tests/fixtures/sample_newspaper.pdf")

    assert len(pages) > 0
    assert not is_scanned

    # Check blocks have font size info
    for page in pages:
        for block in page['blocks']:
            assert 'font_size' in block
            assert 'is_bold' in block
            assert len(block['text']) > 0


def test_ocr_engine_fallback():
    """Test that engine falls back to Tesseract when Vision fails."""
    engine = OCREngine(primary="tesseract")
    results = engine.extract_from_file("tests/fixtures/sample_page.png")

    assert len(results) > 0
    assert len(results[0]['full_text']) > 100
```

---

## ⏭️ Next Module

Proceed to **[Module 4: Headline Detection & NLP](04_Headline_Detection_NLP.md)** to identify headlines from the extracted text.
