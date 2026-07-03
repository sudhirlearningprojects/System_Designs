# Module 6: Google Docs Integration

## 🎯 Learning Objectives

- Create and update Google Docs programmatically
- Format headlines with proper styling (headings, bold, colors)
- Build date-wise sections with navigation
- Maintain Table of Contents
- Handle document size limits

---

## 6.1 Google Docs API Concepts

The Docs API uses a **batch update** model:
- A document is a sequence of structural elements
- You modify it by sending a list of requests (insert, delete, format)
- Each request specifies exact character positions (indices)

```
Document Structure:
├── Body
│   ├── Paragraph (Title)
│   ├── Paragraph (TOC Header)
│   ├── Paragraph (TOC Entry 1)
│   ├── Paragraph (TOC Entry 2)
│   ├── SectionBreak
│   ├── Paragraph (Date Header: 2025-01-15)
│   ├── Paragraph (Headline 1)
│   ├── Paragraph (Summary 1)
│   ├── Paragraph (Headline 2)
│   ├── Paragraph (Summary 2)
│   └── ...
```

---

## 6.2 Document Builder

```python
# newspaper_agent/docs/builder.py

from googleapiclient.discovery import Resource
from typing import List, Dict, Optional
from datetime import date, datetime
import logging

logger = logging.getLogger(__name__)


class GoogleDocBuilder:
    """Create and manage the headlines Google Doc."""

    def __init__(self, docs_service: Resource, drive_service: Resource):
        self.docs = docs_service
        self.drive = drive_service

    def create_document(self, title: str) -> str:
        """Create a new Google Doc and return its ID."""
        doc = self.docs.documents().create(
            body={'title': title}
        ).execute()

        doc_id = doc['documentId']
        logger.info(f"Created document: {title} (ID: {doc_id})")

        # Initialize with title and TOC structure
        self._initialize_document(doc_id, title)

        return doc_id

    def get_or_create_document(
        self,
        doc_id: Optional[str],
        title: str
    ) -> str:
        """Get existing doc or create new one."""
        if doc_id:
            try:
                doc = self.docs.documents().get(
                    documentId=doc_id
                ).execute()
                logger.info(f"Using existing doc: {doc['title']}")
                return doc_id
            except Exception:
                logger.warning(f"Doc {doc_id} not found, creating new")

        return self.create_document(title)

    def _initialize_document(self, doc_id: str, title: str):
        """Set up initial document structure."""
        requests = [
            # Insert title
            self._insert_text(1, f"📰 {title}\n\n"),
            # Format title
            self._format_text(1, len(title) + 3, {
                'bold': True,
                'fontSize': {'magnitude': 24, 'unit': 'PT'},
            }),
            # Insert TOC header
            self._insert_text(
                len(title) + 5,
                "📑 TABLE OF CONTENTS\n"
                "━━━━━━━━━━━━━━━━━━━━\n\n"
                "═══════════════════════════════════════\n\n"
            ),
        ]

        self.docs.documents().batchUpdate(
            documentId=doc_id,
            body={'requests': requests}
        ).execute()

    def add_daily_section(
        self,
        doc_id: str,
        target_date: date,
        headlines: List[Dict]
    ):
        """
        Add a new date section with headlines to the document.
        Inserts at the top (after TOC) so newest is first.
        """
        # Get current document to find insertion point
        doc = self.docs.documents().get(documentId=doc_id).execute()
        body = doc['body']['content']

        # Find insertion point (after the separator line)
        insert_index = self._find_content_start(body)

        # Build the section text
        section_text = self._format_section(target_date, headlines)

        # Build requests
        requests = []

        # Insert section text
        requests.append(self._insert_text(insert_index, section_text))

        # Apply formatting after insertion
        requests.extend(
            self._build_section_formatting(
                insert_index, target_date, headlines, section_text
            )
        )

        # Update TOC
        toc_requests = self._update_toc(doc_id, target_date, body)
        requests.extend(toc_requests)

        # Execute all updates
        self.docs.documents().batchUpdate(
            documentId=doc_id,
            body={'requests': requests}
        ).execute()

        logger.info(
            f"Added section for {target_date} with {len(headlines)} headlines"
        )

    def _format_section(self, target_date: date, headlines: List[Dict]) -> str:
        """Format a date section as plain text (formatting applied separately)."""
        day_name = target_date.strftime('%A')
        date_str = target_date.strftime('%Y-%m-%d')

        lines = []
        lines.append(f"═══════════════════════════════════════")
        lines.append(f"📅 {date_str} ({day_name})")
        lines.append(f"═══════════════════════════════════════")
        lines.append("")

        for i, h in enumerate(headlines, 1):
            headline = h.get('headline', '')
            summary = h.get('summary', '')
            category = h.get('category', '').upper()

            lines.append(f"{i}. [{category}] {headline}")
            if summary:
                # Indent summary
                lines.append(f"   {summary}")
            lines.append("")

        lines.append("")  # Extra spacing between sections
        return "\n".join(lines)

    def _find_content_start(self, body_content: list) -> int:
        """Find where to insert new content (after TOC separator)."""
        full_text = ""
        for element in body_content:
            if 'paragraph' in element:
                for elem in element['paragraph'].get('elements', []):
                    text = elem.get('textRun', {}).get('content', '')
                    full_text += text

        # Find the separator after TOC
        separator = "═══════════════════════════════════════"
        idx = full_text.find(separator)
        if idx > 0:
            # Insert after the separator + newline
            return idx + len(separator) + 2

        # Fallback: insert at end
        return len(full_text) + 1

    def _build_section_formatting(
        self,
        start_index: int,
        target_date: date,
        headlines: List[Dict],
        section_text: str
    ) -> list:
        """Build formatting requests for the section."""
        requests = []
        current_pos = start_index

        lines = section_text.split('\n')
        for line in lines:
            line_start = current_pos
            line_end = current_pos + len(line)

            # Format date header (bold, larger font)
            if line.startswith("📅"):
                requests.append(self._format_text(line_start, line_end, {
                    'bold': True,
                    'fontSize': {'magnitude': 16, 'unit': 'PT'},
                    'foregroundColor': {
                        'color': {'rgbColor': {'red': 0.1, 'green': 0.3, 'blue': 0.6}}
                    },
                }))

            # Format headline numbers (bold)
            elif line and line[0].isdigit() and '. [' in line:
                # Bold the headline part
                bracket_end = line.find('] ') + 2
                requests.append(self._format_text(
                    line_start, line_start + bracket_end, {
                        'bold': True,
                        'fontSize': {'magnitude': 11, 'unit': 'PT'},
                    }
                ))
                # Bold headline text
                requests.append(self._format_text(
                    line_start + bracket_end, line_end, {
                        'bold': True,
                        'fontSize': {'magnitude': 11, 'unit': 'PT'},
                    }
                ))

            # Format summary (gray, smaller)
            elif line.startswith("   ") and line.strip():
                requests.append(self._format_text(line_start, line_end, {
                    'fontSize': {'magnitude': 10, 'unit': 'PT'},
                    'foregroundColor': {
                        'color': {'rgbColor': {'red': 0.3, 'green': 0.3, 'blue': 0.3}}
                    },
                }))

            current_pos = line_end + 1  # +1 for newline

        return requests

    def _update_toc(
        self,
        doc_id: str,
        target_date: date,
        body_content: list
    ) -> list:
        """Add entry to Table of Contents."""
        day_name = target_date.strftime('%A')
        date_str = target_date.strftime('%Y-%m-%d')
        toc_entry = f"• {date_str} ({day_name})\n"

        # Find TOC insertion point (after "━━━━" line)
        full_text = self._get_full_text(body_content)
        toc_marker = "━━━━━━━━━━━━━━━━━━━━"
        toc_idx = full_text.find(toc_marker)

        if toc_idx > 0:
            insert_at = toc_idx + len(toc_marker) + 1
            return [self._insert_text(insert_at, toc_entry)]

        return []

    def _get_full_text(self, body_content: list) -> str:
        """Extract full text from document body."""
        text = ""
        for element in body_content:
            if 'paragraph' in element:
                for elem in element['paragraph'].get('elements', []):
                    text += elem.get('textRun', {}).get('content', '')
        return text

    # --- Helper methods for building requests ---

    def _insert_text(self, index: int, text: str) -> dict:
        """Build insertText request."""
        return {
            'insertText': {
                'location': {'index': index},
                'text': text,
            }
        }

    def _format_text(self, start: int, end: int, style: dict) -> dict:
        """Build updateTextStyle request."""
        fields = ','.join(style.keys())
        return {
            'updateTextStyle': {
                'range': {
                    'startIndex': start,
                    'endIndex': end,
                },
                'textStyle': style,
                'fields': fields,
            }
        }
```

---

## 6.3 Document Formatter

```python
# newspaper_agent/docs/formatter.py

from typing import List, Dict
from datetime import date


class SectionFormatter:
    """Format headline sections for Google Docs."""

    def format_headlines_plain(
        self,
        target_date: date,
        headlines: List[Dict]
    ) -> str:
        """
        Format headlines as plain text for document insertion.
        
        Output format per headline:
        1. [CATEGORY] Headline Text Here
           Two to three line summary providing context about the
           news, key people involved, and significance.
        """
        day_name = target_date.strftime('%A')
        date_str = target_date.strftime('%Y-%m-%d')

        sections = []
        sections.append("═" * 50)
        sections.append(f"📅 {date_str} ({day_name})")
        sections.append("═" * 50)
        sections.append("")

        for i, h in enumerate(headlines, 1):
            headline = h.get('headline', 'Unknown')
            summary = h.get('summary', '')
            category = h.get('category', 'general').upper()[:8]

            # Headline line
            sections.append(f"{i:2d}. [{category}] {headline}")

            # Summary (indented, wrapped at ~70 chars)
            if summary:
                wrapped = self._wrap_text(summary, width=67, indent="    ")
                sections.append(wrapped)

            sections.append("")  # Blank line between entries

        return "\n".join(sections)

    def format_toc_entry(self, target_date: date) -> str:
        """Format a single TOC entry."""
        day_name = target_date.strftime('%A')
        date_str = target_date.strftime('%Y-%m-%d')
        return f"  • {date_str} ({day_name})"

    def _wrap_text(self, text: str, width: int = 70, indent: str = "") -> str:
        """Wrap text to specified width with indent."""
        words = text.split()
        lines = []
        current_line = indent

        for word in words:
            if len(current_line) + len(word) + 1 > width:
                lines.append(current_line)
                current_line = indent + word
            else:
                if current_line == indent:
                    current_line += word
                else:
                    current_line += " " + word

        if current_line.strip():
            lines.append(current_line)

        return "\n".join(lines)
```

---

## 6.4 Simplified Document Update (Alternative Approach)

If the full formatting API is complex, here's a simpler approach using plain text:

```python
# newspaper_agent/docs/simple_builder.py

from googleapiclient.discovery import Resource
from typing import List, Dict, Optional
from datetime import date
import logging

logger = logging.getLogger(__name__)


class SimpleDocBuilder:
    """
    Simplified doc builder using appendText approach.
    Less formatting but more reliable.
    """

    def __init__(self, docs_service: Resource):
        self.docs = docs_service

    def append_daily_headlines(
        self,
        doc_id: str,
        target_date: date,
        headlines: List[Dict]
    ):
        """
        Append today's headlines section to the end of the document.
        Uses HEADING styles for navigation.
        """
        # Get document end index
        doc = self.docs.documents().get(documentId=doc_id).execute()
        end_index = doc['body']['content'][-1]['endIndex'] - 1

        # Build content
        section = self._build_section_content(target_date, headlines)

        # Insert at end
        requests = [
            {
                'insertText': {
                    'location': {'index': end_index},
                    'text': section,
                }
            }
        ]

        # Apply heading style to date header
        # (makes it show up in Google Docs outline/navigation)
        date_line = f"📅 {target_date.strftime('%Y-%m-%d')} ({target_date.strftime('%A')})"
        date_start = end_index
        date_end = date_start + len(date_line)

        requests.append({
            'updateParagraphStyle': {
                'range': {'startIndex': date_start, 'endIndex': date_end},
                'paragraphStyle': {
                    'namedStyleType': 'HEADING_2',
                },
                'fields': 'namedStyleType',
            }
        })

        self.docs.documents().batchUpdate(
            documentId=doc_id,
            body={'requests': requests}
        ).execute()

        logger.info(f"Appended {len(headlines)} headlines for {target_date}")

    def prepend_daily_headlines(
        self,
        doc_id: str,
        target_date: date,
        headlines: List[Dict],
        insert_after_toc: bool = True
    ):
        """
        Prepend today's headlines after TOC (newest first).
        """
        doc = self.docs.documents().get(documentId=doc_id).execute()

        # Find insertion point
        if insert_after_toc:
            insert_index = self._find_after_toc(doc)
        else:
            insert_index = 1

        section = self._build_section_content(target_date, headlines)

        requests = [
            {
                'insertText': {
                    'location': {'index': insert_index},
                    'text': section,
                }
            }
        ]

        self.docs.documents().batchUpdate(
            documentId=doc_id,
            body={'requests': requests}
        ).execute()

    def _build_section_content(
        self,
        target_date: date,
        headlines: List[Dict]
    ) -> str:
        """Build section content as text."""
        lines = []
        lines.append("\n")
        lines.append("━" * 50)
        lines.append(
            f"📅 {target_date.strftime('%Y-%m-%d')} "
            f"({target_date.strftime('%A')})"
        )
        lines.append("━" * 50)
        lines.append("")

        for i, h in enumerate(headlines, 1):
            cat = h.get('category', 'GENERAL').upper()[:6]
            lines.append(f"{i}. [{cat}] {h['headline']}")
            if h.get('summary'):
                lines.append(f"   {h['summary']}")
            lines.append("")

        return "\n".join(lines)

    def _find_after_toc(self, doc: dict) -> int:
        """Find index position after Table of Contents."""
        content = doc['body']['content']
        full_text = ""

        for element in content:
            if 'paragraph' in element:
                for elem in element['paragraph'].get('elements', []):
                    full_text += elem.get('textRun', {}).get('content', '')

        # Look for first separator after TOC
        markers = ["━" * 20, "═" * 20]
        for marker in markers:
            idx = full_text.find(marker)
            if idx > 0:
                # Find end of this line
                newline_after = full_text.find('\n', idx)
                if newline_after > 0:
                    return newline_after + 2

        return len(full_text)
```

---

## 6.5 Document Size Management

```python
# newspaper_agent/docs/size_manager.py

from googleapiclient.discovery import Resource
import logging

logger = logging.getLogger(__name__)

# Google Docs limit: ~1.02 million characters
MAX_DOC_CHARS = 1_000_000
ARCHIVE_THRESHOLD = 900_000  # Start new doc at 90%


class DocSizeManager:
    """Manage document size and archival."""

    def __init__(self, docs_service: Resource, drive_service: Resource):
        self.docs = docs_service
        self.drive = drive_service

    def get_document_size(self, doc_id: str) -> int:
        """Get current character count of document."""
        doc = self.docs.documents().get(documentId=doc_id).execute()
        content = doc['body']['content']

        char_count = 0
        for element in content:
            if 'paragraph' in element:
                for elem in element['paragraph'].get('elements', []):
                    text = elem.get('textRun', {}).get('content', '')
                    char_count += len(text)

        return char_count

    def needs_new_document(self, doc_id: str) -> bool:
        """Check if document is approaching size limit."""
        size = self.get_document_size(doc_id)
        logger.info(f"Document size: {size:,} / {MAX_DOC_CHARS:,} chars")
        return size >= ARCHIVE_THRESHOLD

    def archive_and_create_new(
        self,
        old_doc_id: str,
        title: str
    ) -> str:
        """
        Rename old doc as archive and create new one.
        
        Returns: new document ID
        """
        # Rename old doc
        from datetime import datetime
        archive_name = f"{title} (Archive - before {datetime.now().strftime('%Y-%m-%d')})"

        self.drive.files().update(
            fileId=old_doc_id,
            body={'name': archive_name}
        ).execute()

        logger.info(f"Archived old doc as: {archive_name}")

        # Create new doc (will be set up by builder)
        return None  # Signal to create new
```

---

## 6.6 Dependencies

```bash
pip install google-api-python-client==2.100.0
pip install google-auth-httplib2==0.1.1
pip install google-auth-oauthlib==1.1.0
```

---

## 6.7 Testing

```python
# tests/test_docs.py

from newspaper_agent.docs.formatter import SectionFormatter
from datetime import date


def test_section_formatting():
    formatter = SectionFormatter()
    headlines = [
        {
            'headline': 'India GDP grows 7.2% in Q3',
            'summary': 'The economy expanded at 7.2% beating estimates. Manufacturing and services drove growth.',
            'category': 'business',
        },
        {
            'headline': 'SC bans electoral bonds',
            'summary': 'Supreme Court declared electoral bond scheme unconstitutional.',
            'category': 'politics',
        },
    ]

    output = formatter.format_headlines_plain(date(2025, 1, 15), headlines)
    assert '2025-01-15' in output
    assert 'Wednesday' in output
    assert 'GDP' in output
    assert '[BUSINESS]' in output
    assert '[POLITICS]' in output
```

---

## ⏭️ Next Module

Proceed to **[Module 7: Search & Navigation](07_Search_Navigation.md)** to add search and date navigation capabilities.
