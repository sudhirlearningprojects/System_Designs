# Module 11: Government Jobs Extraction (Part 3 - Search & CLI)

## 11.10 Government Jobs Search

```python
# newspaper_agent/docs/govt_job_searcher.py

from typing import List, Dict, Optional
from newspaper_agent.drive.state_tracker import StateTracker
import logging

logger = logging.getLogger(__name__)


class GovtJobSearcher:
    """Search and filter government job notifications."""

    def __init__(self, state_tracker: StateTracker):
        self.tracker = state_tracker

    def search_by_keyword(self, query: str, limit: int = 20) -> List[Dict]:
        """Search jobs by keyword (org name, post name, etc.)."""
        return self.tracker.search_govt_jobs(query, limit)

    def search_by_organization(self, org: str) -> List[Dict]:
        """Get all jobs from a specific organization."""
        import sqlite3
        with sqlite3.connect(self.tracker.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                """SELECT * FROM govt_jobs_history
                   WHERE organization LIKE ?
                   ORDER BY date DESC""",
                (f"%{org}%",)
            )
            return [dict(row) for row in cursor.fetchall()]

    def get_upcoming_deadlines(self, days_ahead: int = 30) -> List[Dict]:
        """Get jobs with upcoming application deadlines."""
        import sqlite3
        from datetime import datetime, timedelta

        today = datetime.now().strftime('%Y-%m-%d')

        with sqlite3.connect(self.tracker.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                """SELECT * FROM govt_jobs_history
                   WHERE last_date IS NOT NULL
                   AND last_date != ''
                   ORDER BY date DESC
                   LIMIT 50"""
            )
            return [dict(row) for row in cursor.fetchall()]

    def get_jobs_by_date(self, date_str: str) -> List[Dict]:
        """Get all govt jobs found on a specific date."""
        import sqlite3
        with sqlite3.connect(self.tracker.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                """SELECT * FROM govt_jobs_history
                   WHERE date = ?
                   ORDER BY id""",
                (date_str,)
            )
            return [dict(row) for row in cursor.fetchall()]

    def get_stats(self) -> Dict:
        """Get government jobs statistics."""
        import sqlite3
        with sqlite3.connect(self.tracker.db_path) as conn:
            stats = {}

            cursor = conn.execute(
                "SELECT COUNT(*) FROM govt_jobs_history"
            )
            stats['total_jobs'] = cursor.fetchone()[0]

            cursor = conn.execute(
                """SELECT organization, COUNT(*) as count
                   FROM govt_jobs_history
                   WHERE organization IS NOT NULL
                   GROUP BY organization
                   ORDER BY count DESC
                   LIMIT 10"""
            )
            stats['top_organizations'] = [
                {'org': row[0], 'count': row[1]}
                for row in cursor.fetchall()
            ]

            cursor = conn.execute(
                "SELECT COUNT(DISTINCT date) FROM govt_jobs_history"
            )
            stats['days_with_jobs'] = cursor.fetchone()[0]

            return stats
```

---

## 11.11 Updated CLI

```python
# Add these commands to newspaper_agent/docs/search_cli.py

def main():
    parser = argparse.ArgumentParser(description="Search newspaper headlines & jobs")
    subparsers = parser.add_subparsers(dest='command')

    # ... existing commands (search, date, category, stats) ...

    # NEW: Government Jobs commands
    jobs = subparsers.add_parser('jobs', help='Search government jobs')
    jobs.add_argument('query', nargs='?', default='', help='Search query')
    jobs.add_argument('--org', help='Filter by organization')
    jobs.add_argument('--date', help='Filter by date')
    jobs.add_argument('--deadlines', action='store_true',
                      help='Show upcoming deadlines')

    args = parser.parse_args()

    if args.command == 'jobs':
        _handle_jobs_command(args)

    # ... existing handlers ...


def _handle_jobs_command(args):
    """Handle government jobs search."""
    from newspaper_agent.docs.govt_job_searcher import GovtJobSearcher

    tracker = StateTracker()
    searcher = GovtJobSearcher(tracker)

    if args.deadlines:
        results = searcher.get_upcoming_deadlines()
        print("\n📌 Upcoming Deadlines:\n")
    elif args.org:
        results = searcher.search_by_organization(args.org)
        print(f"\n🏛️ Jobs from: {args.org}\n")
    elif args.date:
        results = searcher.get_jobs_by_date(args.date)
        print(f"\n🏛️ Jobs found on: {args.date}\n")
    elif args.query:
        results = searcher.search_by_keyword(args.query)
        print(f"\n🏛️ Jobs matching: {args.query}\n")
    else:
        # Show stats
        stats = searcher.get_stats()
        print("\n📊 Government Jobs Statistics")
        print(f"   Total jobs tracked: {stats['total_jobs']}")
        print(f"   Days with updates: {stats['days_with_jobs']}")
        print("\n   Top organizations:")
        for item in stats['top_organizations']:
            print(f"     {item['org']}: {item['count']} notifications")
        return

    if not results:
        print("   No results found.")
        return

    for r in results:
        org = r.get('organization', 'N/A')
        post = r.get('post_name', 'N/A')
        vacancies = r.get('vacancies', '')
        last_date = r.get('last_date', '')
        date_found = r.get('date', '')

        title = f"  [{date_found}] {org} - {post}"
        if vacancies:
            title += f" ({vacancies} vacancies)"
        print(title)

        if r.get('summary'):
            print(f"    {r['summary'][:100]}")
        if last_date:
            print(f"    📌 Last Date: {last_date}")
        print()
```

Usage:
```bash
# Search government jobs
python -m newspaper_agent.main jobs "SSC"
python -m newspaper_agent.main jobs --org "UPSC"
python -m newspaper_agent.main jobs --date 2025-01-15
python -m newspaper_agent.main jobs --deadlines
python -m newspaper_agent.main jobs  # Shows stats
```

---

## 11.12 Agent Search Tool (Updated)

```python
# Add to newspaper_agent/agent/tools/search_tool.py

@tool
def search_govt_jobs(query: str) -> str:
    """
    Search government job notifications from newspapers.

    Args:
        query: Organization name, post name, or keyword

    Returns:
        Formatted list of matching government jobs
    """
    from newspaper_agent.docs.govt_job_searcher import GovtJobSearcher

    tracker = StateTracker()
    searcher = GovtJobSearcher(tracker)
    results = searcher.search_by_keyword(query)

    if not results:
        return f"No government jobs found for: {query}"

    output = f"Found {len(results)} government job(s):\n\n"
    for r in results:
        output += f"• {r.get('organization')} - {r.get('post_name')}\n"
        if r.get('vacancies'):
            output += f"  Vacancies: {r['vacancies']}\n"
        if r.get('last_date'):
            output += f"  Last Date: {r['last_date']}\n"
        if r.get('summary'):
            output += f"  {r['summary']}\n"
        output += "\n"

    return output
```

---

## 11.13 Example Output

After running the agent on a day with government job ads:

```
$ python -m newspaper_agent.main run

2025-01-15 08:00:01 [INFO] 🚀 Starting newspaper agent for 2025-01-15
...
2025-01-15 08:00:25 [INFO] 🏛️ Extracting government job notifications...
2025-01-15 08:00:26 [INFO] Found 7 govt job candidates
2025-01-15 08:00:30 [INFO] Extracted 5 structured govt job entries
...
2025-01-15 08:00:38 [INFO] ✅ Document updated: 50 headlines + 5 govt jobs
```

Google Doc output for that date:
```
═══════════════════════════════════════════════════
📅 2025-01-15 (Wednesday)
═══════════════════════════════════════════════════

📰 TOP HEADLINES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. [POLITI] Modi announces new education policy reforms
   The PM unveiled changes to NEP including...
...
50. [SPORTS] India wins 3rd Test match against Australia
    ...

🏛️ GOVERNMENT JOBS (Permanent Posts)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. UPSC - Civil Services Exam 2025 (1000+ Vacancies)
   Union Public Service Commission has released notification
   for IAS/IPS/IFS and other allied services. Graduates aged
   21-32 years can apply.
   📌 Last Date: 15 Feb 2025 | Apply: upsc.gov.in

2. SSC - CGL 2025 (8000 Vacancies)
   Staff Selection Commission invites applications for
   Combined Graduate Level posts across central government
   ministries and departments.
   📌 Last Date: 28 Feb 2025 | Apply: ssc.nic.in

3. Indian Railways - RRB NTPC (35,000 Vacancies)
   Applications open for Non-Technical Popular Categories
   including Commercial Cum Ticket Clerk, Station Master,
   and Goods Guard. 12th pass eligible.
   📌 Last Date: 10 Mar 2025 | Apply: rrbcdg.gov.in

4. Rajasthan PSC - RAS 2025 (500 Vacancies)
   RPSC has announced Rajasthan Administrative Service exam.
   Graduate candidates aged 21-40 can apply for various
   state administrative positions.
   📌 Last Date: 20 Feb 2025 | Apply: rpsc.rajasthan.gov.in

5. SBI - Probationary Officers (2000 Vacancies)
   State Bank of India invites applications for PO cadre.
   Graduate aged 21-30 years. Selection through preliminary,
   main exam, and interview.
   📌 Last Date: 05 Mar 2025 | Apply: sbi.co.in
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 11.14 Testing

```python
# tests/test_govt_jobs.py

from newspaper_agent.nlp.govt_job_detector import GovtJobDetector


def test_detects_upsc_notification():
    detector = GovtJobDetector()

    pages = [{
        'page_number': 1,
        'blocks': [{
            'text': 'UPSC Civil Services 2025 Notification. '
                    '1000 vacancies. Last date 15 Feb 2025. '
                    'Apply online at upsc.gov.in',
            'font_size': 14,
            'position_y': 100,
        }],
        'full_text': '',
    }]

    results = detector.detect_govt_jobs(pages)
    assert len(results) >= 1
    assert results[0]['confidence'] > 0.5


def test_ignores_private_jobs():
    detector = GovtJobDetector()

    pages = [{
        'page_number': 1,
        'blocks': [{
            'text': 'TCS hiring 5000 freshers. Walk-in interview. '
                    'Private company. Apply on careers.tcs.com',
            'font_size': 12,
            'position_y': 200,
        }],
        'full_text': '',
    }]

    results = detector.detect_govt_jobs(pages)
    assert len(results) == 0


def test_ignores_contractual():
    detector = GovtJobDetector()

    pages = [{
        'page_number': 1,
        'blocks': [{
            'text': 'District Collector office hiring on contractual basis. '
                    '10 posts of data entry operator. Temporary position.',
            'font_size': 12,
            'position_y': 200,
        }],
        'full_text': '',
    }]

    results = detector.detect_govt_jobs(pages)
    assert len(results) == 0
```

---

## 11.15 Summary of Changes

| File | Change |
|------|--------|
| `agent/state.py` | Added `raw_govt_jobs`, `structured_govt_jobs` fields |
| `agent/nodes.py` | Added `extract_govt_jobs` node |
| `agent/orchestrator.py` | Added node to graph after `rank_and_summarize` |
| `nlp/govt_job_detector.py` | NEW - Keyword detection + confidence scoring |
| `summarizer/govt_job_enricher.py` | NEW - LLM structuring of raw job text |
| `docs/simple_builder.py` | Updated to include govt jobs section per date |
| `drive/state_tracker.py` | Added `govt_jobs_history` table + search |
| `docs/govt_job_searcher.py` | NEW - Search/filter govt jobs |
| `docs/search_cli.py` | Added `jobs` command |

---

## 11.16 Updated Project Structure

```
newspaper_agent/
├── nlp/
│   ├── headline_detector.py
│   ├── deduplicator.py
│   ├── classifier.py
│   └── govt_job_detector.py      ← NEW
├── summarizer/
│   ├── llm_client.py
│   ├── headline_enricher.py
│   └── govt_job_enricher.py      ← NEW
├── docs/
│   ├── builder.py
│   ├── simple_builder.py         ← UPDATED (combined sections)
│   ├── searcher.py
│   └── govt_job_searcher.py      ← NEW
└── agent/
    ├── state.py                  ← UPDATED
    ├── nodes.py                  ← UPDATED
    └── orchestrator.py           ← UPDATED
```

---

## 🎉 Feature Complete

Each date in the Google Doc now has:
1. **📰 Top 50 Headlines** - with 2-3 line summaries
2. **🏛️ Government Jobs** - permanent posts with org, vacancies, deadlines, and apply links

Both sections are searchable via CLI and the agent's built-in search tool.
