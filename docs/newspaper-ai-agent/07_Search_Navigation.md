# Module 7: Search & Navigation

## 🎯 Learning Objectives

- Build headline search functionality
- Implement date-based navigation
- Create Table of Contents with bookmarks
- Use Google Docs outline for native navigation
- Build a CLI/API search interface for the agent

---

## 7.1 Navigation Strategy

Google Docs provides several navigation mechanisms:

1. **Document Outline** (built-in) - Uses heading styles (H1, H2, H3)
2. **Bookmarks** - Named anchors you can link to
3. **Table of Contents** - Auto-generated from headings
4. **Ctrl+F** - Native find in document
5. **Agent-powered search** - LLM-enhanced search via our tool

```
Navigation Hierarchy:
├── Document Title (H1)
├── TABLE OF CONTENTS (auto-links)
│   ├── 2025-01-15 (Wednesday) → [bookmark link]
│   ├── 2025-01-14 (Tuesday) → [bookmark link]
│   └── ...
├── Date Section (H2) ← Shows in Doc Outline sidebar
│   ├── Headline 1 (Normal text, bold)
│   ├── Headline 2
│   └── ...
└── Date Section (H2)
    └── ...
```

---

## 7.2 Bookmark-Based Navigation

```python
# newspaper_agent/docs/navigator.py

from googleapiclient.discovery import Resource
from typing import List, Dict, Optional
from datetime import date
import logging

logger = logging.getLogger(__name__)


class DocNavigator:
    """
    Manage navigation within the headlines document.
    Creates bookmarks for date sections and links in TOC.
    """

    def __init__(self, docs_service: Resource):
        self.docs = docs_service

    def create_bookmark_for_date(
        self,
        doc_id: str,
        target_date: date,
        index: int
    ) -> str:
        """
        Create a bookmark at the date section header.
        Returns bookmark ID for linking.
        """
        bookmark_id = f"date-{target_date.isoformat()}"

        requests = [{
            'createNamedRange': {
                'name': bookmark_id,
                'range': {
                    'startIndex': index,
                    'endIndex': index + 1,
                }
            }
        }]

        self.docs.documents().batchUpdate(
            documentId=doc_id,
            body={'requests': requests}
        ).execute()

        return bookmark_id

    def add_heading_style(
        self,
        doc_id: str,
        start_index: int,
        end_index: int,
        heading_level: str = "HEADING_2"
    ):
        """
        Apply heading style to make text appear in Document Outline.
        This gives FREE native navigation in Google Docs.
        """
        requests = [{
            'updateParagraphStyle': {
                'range': {
                    'startIndex': start_index,
                    'endIndex': end_index,
                },
                'paragraphStyle': {
                    'namedStyleType': heading_level,
                },
                'fields': 'namedStyleType',
            }
        }]

        self.docs.documents().batchUpdate(
            documentId=doc_id,
            body={'requests': requests}
        ).execute()

    def get_all_date_sections(self, doc_id: str) -> List[Dict]:
        """
        Find all date sections in the document.
        Returns list of {date, start_index, end_index}.
        """
        doc = self.docs.documents().get(documentId=doc_id).execute()
        content = doc['body']['content']

        sections = []
        current_pos = 0

        for element in content:
            if 'paragraph' in element:
                text = ""
                for elem in element['paragraph'].get('elements', []):
                    text += elem.get('textRun', {}).get('content', '')

                # Check if this is a date header
                if text.strip().startswith("📅"):
                    # Extract date from "📅 2025-01-15 (Wednesday)"
                    import re
                    match = re.search(r'(\d{4}-\d{2}-\d{2})', text)
                    if match:
                        sections.append({
                            'date': match.group(1),
                            'start_index': element['startIndex'],
                            'end_index': element['endIndex'],
                            'text': text.strip(),
                        })

        return sections

    def jump_to_date(self, doc_id: str, target_date: str) -> Optional[str]:
        """
        Get the URL to jump directly to a date section.
        Uses heading ID for direct navigation.
        """
        sections = self.get_all_date_sections(doc_id)

        for section in sections:
            if section['date'] == target_date:
                # Google Docs URL with heading anchor
                return (
                    f"https://docs.google.com/document/d/{doc_id}/edit"
                    f"#heading=h.{section['start_index']}"
                )

        return None
```

---

## 7.3 Search Implementation

```python
# newspaper_agent/docs/searcher.py

from typing import List, Dict, Optional
from datetime import date, datetime
from newspaper_agent.drive.state_tracker import StateTracker
from newspaper_agent.summarizer.llm_client import LLMClient
import logging

logger = logging.getLogger(__name__)


class HeadlineSearcher:
    """
    Search headlines across all dates.
    Supports keyword search, date filtering, and semantic search.
    """

    def __init__(
        self,
        state_tracker: StateTracker,
        llm_client: Optional[LLMClient] = None
    ):
        self.tracker = state_tracker
        self.llm = llm_client

    def search_by_keyword(
        self,
        query: str,
        limit: int = 20
    ) -> List[Dict]:
        """
        Simple keyword search across all headlines.
        Searches in headline text and summaries.
        """
        return self.tracker.search_headlines(query, limit)

    def search_by_date(
        self,
        target_date: str
    ) -> List[Dict]:
        """Get all headlines for a specific date."""
        from sqlite3 import connect

        with connect(self.tracker.db_path) as conn:
            conn.row_factory = lambda c, r: dict(
                zip([col[0] for col in c.description], r)
            )
            cursor = conn.execute(
                """SELECT headline, summary, category, rank
                   FROM headlines_history
                   WHERE date = ?
                   ORDER BY rank""",
                (target_date,)
            )
            return cursor.fetchall()

    def search_by_date_range(
        self,
        start_date: str,
        end_date: str,
        query: Optional[str] = None
    ) -> List[Dict]:
        """Search within a date range, optionally with keyword."""
        from sqlite3 import connect

        with connect(self.tracker.db_path) as conn:
            conn.row_factory = lambda c, r: dict(
                zip([col[0] for col in c.description], r)
            )

            if query:
                cursor = conn.execute(
                    """SELECT date, headline, summary, category, rank
                       FROM headlines_history
                       WHERE date BETWEEN ? AND ?
                       AND (headline LIKE ? OR summary LIKE ?)
                       ORDER BY date DESC, rank""",
                    (start_date, end_date, f"%{query}%", f"%{query}%")
                )
            else:
                cursor = conn.execute(
                    """SELECT date, headline, summary, category, rank
                       FROM headlines_history
                       WHERE date BETWEEN ? AND ?
                       ORDER BY date DESC, rank""",
                    (start_date, end_date)
                )

            return cursor.fetchall()

    def search_by_category(
        self,
        category: str,
        limit: int = 20
    ) -> List[Dict]:
        """Get headlines by category."""
        from sqlite3 import connect

        with connect(self.tracker.db_path) as conn:
            conn.row_factory = lambda c, r: dict(
                zip([col[0] for col in c.description], r)
            )
            cursor = conn.execute(
                """SELECT date, headline, summary, category, rank
                   FROM headlines_history
                   WHERE category = ?
                   ORDER BY date DESC
                   LIMIT ?""",
                (category, limit)
            )
            return cursor.fetchall()

    def semantic_search(
        self,
        query: str,
        limit: int = 10
    ) -> List[Dict]:
        """
        AI-powered semantic search using LLM.
        Useful for queries like "news about climate change impact on agriculture"
        """
        if not self.llm:
            logger.warning("No LLM client, falling back to keyword search")
            return self.search_by_keyword(query, limit)

        # Get recent headlines as context
        all_headlines = self.search_by_date_range(
            start_date="2020-01-01",
            end_date="2030-12-31"
        )

        if not all_headlines:
            return []

        # Format for LLM
        headlines_text = "\n".join(
            f"[{h['date']}] {h['headline']}"
            for h in all_headlines[:200]  # Limit context
        )

        prompt = f"""Given these headlines, find the ones most relevant to the query: "{query}"

Headlines:
{headlines_text}

Return the top {limit} most relevant headlines as a JSON array:
[{{"date": "YYYY-MM-DD", "headline": "exact text"}}]

Return ONLY relevant ones. If none match, return empty array []."""

        response = self.llm.complete(
            system_prompt="You are a news search engine. Match headlines to queries accurately.",
            user_prompt=prompt,
            max_tokens=2000
        )

        import json
        try:
            text = response.strip()
            if text.startswith("```"):
                text = text.split("\n", 1)[1].rsplit("```", 1)[0]
            results = json.loads(text)

            # Enrich with full data
            enriched = []
            for r in results:
                full_data = next(
                    (h for h in all_headlines
                     if h['headline'] == r.get('headline')),
                    r
                )
                enriched.append(full_data)

            return enriched[:limit]
        except (json.JSONDecodeError, Exception) as e:
            logger.error(f"Semantic search parse error: {e}")
            return self.search_by_keyword(query, limit)

    def get_statistics(self) -> Dict:
        """Get headline database statistics."""
        from sqlite3 import connect

        with connect(self.tracker.db_path) as conn:
            stats = {}

            cursor = conn.execute("SELECT COUNT(*) FROM headlines_history")
            stats['total_headlines'] = cursor.fetchone()[0]

            cursor = conn.execute(
                "SELECT COUNT(DISTINCT date) FROM headlines_history"
            )
            stats['total_days'] = cursor.fetchone()[0]

            cursor = conn.execute(
                """SELECT category, COUNT(*) as count
                   FROM headlines_history
                   GROUP BY category
                   ORDER BY count DESC"""
            )
            stats['by_category'] = dict(cursor.fetchall())

            cursor = conn.execute(
                "SELECT MIN(date), MAX(date) FROM headlines_history"
            )
            row = cursor.fetchone()
            stats['date_range'] = {'from': row[0], 'to': row[1]}

            return stats
```

---

## 7.4 CLI Search Interface

```python
# newspaper_agent/docs/search_cli.py

import argparse
from newspaper_agent.docs.searcher import HeadlineSearcher
from newspaper_agent.drive.state_tracker import StateTracker


def main():
    parser = argparse.ArgumentParser(description="Search newspaper headlines")
    subparsers = parser.add_subparsers(dest='command')

    # Keyword search
    kw = subparsers.add_parser('search', help='Search by keyword')
    kw.add_argument('query', help='Search query')
    kw.add_argument('--limit', type=int, default=10)

    # Date search
    dt = subparsers.add_parser('date', help='Get headlines for a date')
    dt.add_argument('date', help='Date in YYYY-MM-DD format')

    # Category search
    cat = subparsers.add_parser('category', help='Search by category')
    cat.add_argument('category', choices=[
        'politics', 'business', 'sports', 'technology',
        'world', 'health', 'science', 'entertainment', 'general'
    ])
    cat.add_argument('--limit', type=int, default=10)

    # Stats
    subparsers.add_parser('stats', help='Show statistics')

    args = parser.parse_args()
    tracker = StateTracker()
    searcher = HeadlineSearcher(tracker)

    if args.command == 'search':
        results = searcher.search_by_keyword(args.query, args.limit)
        _print_results(results)

    elif args.command == 'date':
        results = searcher.search_by_date(args.date)
        _print_results(results)

    elif args.command == 'category':
        results = searcher.search_by_category(args.category, args.limit)
        _print_results(results)

    elif args.command == 'stats':
        stats = searcher.get_statistics()
        print(f"\n📊 Headlines Database Statistics")
        print(f"   Total headlines: {stats['total_headlines']}")
        print(f"   Total days: {stats['total_days']}")
        print(f"   Date range: {stats['date_range']['from']} to {stats['date_range']['to']}")
        print(f"\n   By category:")
        for cat, count in stats['by_category'].items():
            print(f"     {cat}: {count}")


def _print_results(results):
    if not results:
        print("No results found.")
        return

    print(f"\n📰 Found {len(results)} results:\n")
    for r in results:
        date_str = r.get('date', 'N/A')
        headline = r.get('headline', 'N/A')
        summary = r.get('summary', '')
        category = r.get('category', '')

        print(f"  [{date_str}] [{category.upper()}] {headline}")
        if summary:
            print(f"    → {summary[:100]}...")
        print()


if __name__ == '__main__':
    main()
```

Usage:
```bash
# Search by keyword
python -m newspaper_agent.docs.search_cli search "GDP growth"

# Get headlines for a date
python -m newspaper_agent.docs.search_cli date 2025-01-15

# Search by category
python -m newspaper_agent.docs.search_cli category business --limit 20

# Show statistics
python -m newspaper_agent.docs.search_cli stats
```

---

## 7.5 Agent Search Tool (for LangGraph)

```python
# newspaper_agent/agent/tools/search_tool.py

from langchain_core.tools import tool
from newspaper_agent.docs.searcher import HeadlineSearcher
from newspaper_agent.drive.state_tracker import StateTracker


@tool
def search_headlines(query: str, search_type: str = "keyword") -> str:
    """
    Search through all stored newspaper headlines.
    
    Args:
        query: The search query (keyword, date, or category)
        search_type: One of "keyword", "date", "category"
    
    Returns:
        Formatted search results
    """
    tracker = StateTracker()
    searcher = HeadlineSearcher(tracker)

    if search_type == "date":
        results = searcher.search_by_date(query)
    elif search_type == "category":
        results = searcher.search_by_category(query)
    else:
        results = searcher.search_by_keyword(query)

    if not results:
        return f"No headlines found for: {query}"

    output = f"Found {len(results)} results:\n"
    for r in results:
        output += f"- [{r.get('date')}] {r.get('headline')}\n"
        if r.get('summary'):
            output += f"  {r['summary'][:80]}...\n"

    return output
```

---

## 7.6 Testing

```python
# tests/test_search.py

from newspaper_agent.docs.searcher import HeadlineSearcher
from newspaper_agent.drive.state_tracker import StateTracker


def test_keyword_search():
    tracker = StateTracker(":memory:")
    tracker.save_headlines("2025-01-15", [
        {"headline": "India GDP grows 7.2%", "summary": "Economy expanded", "category": "business", "rank": 1},
        {"headline": "Supreme Court verdict", "summary": "Bonds banned", "category": "politics", "rank": 2},
    ])

    searcher = HeadlineSearcher(tracker)
    results = searcher.search_by_keyword("GDP")
    assert len(results) == 1
    assert "GDP" in results[0]['headline']


def test_date_search():
    tracker = StateTracker(":memory:")
    tracker.save_headlines("2025-01-15", [
        {"headline": "H1", "summary": "S1", "category": "c1", "rank": 1},
        {"headline": "H2", "summary": "S2", "category": "c2", "rank": 2},
    ])

    searcher = HeadlineSearcher(tracker)
    results = searcher.search_by_date("2025-01-15")
    assert len(results) == 2
```

---

## ⏭️ Next Module

Proceed to **[Module 8: Agent Orchestration](08_Agent_Orchestration.md)** to wire everything together with LangGraph.
