# Module 11: Government Jobs Extraction (Part 2 - Integration)

## 11.4 Updated Agent State

Add govt jobs to the agent state:

```python
# Add to newspaper_agent/agent/state.py

class GovtJob(TypedDict):
    organization: str
    post_name: str
    vacancies: str
    eligibility: str
    last_date: str
    website: str
    summary: str


class AgentState(TypedDict):
    # ... existing fields ...

    # NEW: Government jobs
    raw_govt_jobs: List[dict]
    structured_govt_jobs: List[GovtJob]
```

---

## 11.5 New Agent Node

```python
# Add to newspaper_agent/agent/nodes.py

def extract_govt_jobs(state: AgentState) -> Dict:
    """Node 5b: Extract government job notifications."""
    logger.info("🏛️ Extracting government job notifications...")

    from newspaper_agent.nlp.govt_job_detector import GovtJobDetector
    from newspaper_agent.summarizer.govt_job_enricher import GovtJobEnricher
    import os

    # Detect raw job mentions
    detector = GovtJobDetector(min_keyword_matches=2)
    raw_jobs = detector.detect_govt_jobs(state['extracted_pages'])

    # Enrich with LLM
    from newspaper_agent.summarizer.llm_client import LLMClient
    llm = LLMClient(
        provider=os.getenv('AGENT_LLM_PROVIDER', 'openai'),
        model=os.getenv('AGENT_LLM_MODEL', 'gpt-4o'),
        api_key=os.getenv('AGENT_LLM_API_KEY', ''),
    )
    enricher = GovtJobEnricher(llm)
    structured = enricher.enrich_jobs(raw_jobs)

    logger.info(f"Found {len(structured)} government job notifications")

    return {
        'raw_govt_jobs': raw_jobs,
        'structured_govt_jobs': structured,
        'step': 'govt_jobs_complete',
    }
```

---

## 11.6 Updated Agent Graph

```python
# Updated newspaper_agent/agent/orchestrator.py

from langgraph.graph import StateGraph, END
from newspaper_agent.agent.state import AgentState
from newspaper_agent.agent.nodes import (
    check_drive,
    download_files,
    extract_text,
    detect_headlines,
    rank_and_summarize,
    extract_govt_jobs,      # NEW
    update_document,
)
from newspaper_agent.agent.conditions import has_new_files, has_candidates


def build_agent() -> StateGraph:
    """Build the newspaper agent workflow graph."""

    workflow = StateGraph(AgentState)

    # Add nodes
    workflow.add_node("check_drive", check_drive)
    workflow.add_node("download_files", download_files)
    workflow.add_node("extract_text", extract_text)
    workflow.add_node("detect_headlines", detect_headlines)
    workflow.add_node("rank_and_summarize", rank_and_summarize)
    workflow.add_node("extract_govt_jobs", extract_govt_jobs)  # NEW
    workflow.add_node("update_document", update_document)

    # Set entry point
    workflow.set_entry_point("check_drive")

    # Conditional: new files found?
    workflow.add_conditional_edges(
        "check_drive",
        has_new_files,
        {"download": "download_files", "end": END}
    )

    # Linear flow
    workflow.add_edge("download_files", "extract_text")
    workflow.add_edge("extract_text", "detect_headlines")

    workflow.add_conditional_edges(
        "detect_headlines",
        has_candidates,
        {"rank": "rank_and_summarize", "insufficient": END}
    )

    # After ranking headlines, extract govt jobs in parallel
    workflow.add_edge("rank_and_summarize", "extract_govt_jobs")  # NEW
    workflow.add_edge("extract_govt_jobs", "update_document")     # NEW
    workflow.add_edge("update_document", END)

    return workflow.compile()
```

Updated graph visualization:
```
check_drive → download → extract_text → detect_headlines
                                              │
                                         rank_and_summarize
                                              │
                                        extract_govt_jobs  ← NEW
                                              │
                                        update_document → END
```

---

## 11.7 Updated Document Builder

```python
# Update newspaper_agent/docs/simple_builder.py
# Add this method to SimpleDocBuilder class:

def prepend_daily_section_with_jobs(
    self,
    doc_id: str,
    target_date: date,
    headlines: List[Dict],
    govt_jobs: List[Dict],
):
    """
    Insert daily section with BOTH headlines and govt jobs.
    """
    doc = self.docs.documents().get(documentId=doc_id).execute()
    insert_index = self._find_after_toc(doc)

    # Build combined section
    section = self._build_combined_section(
        target_date, headlines, govt_jobs
    )

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

    logger.info(
        f"Added section: {len(headlines)} headlines + "
        f"{len(govt_jobs)} govt jobs for {target_date}"
    )

def _build_combined_section(
    self,
    target_date: date,
    headlines: List[Dict],
    govt_jobs: List[Dict],
) -> str:
    """Build section with both headlines and govt jobs."""
    lines = []

    # Date header
    lines.append("\n")
    lines.append("═" * 50)
    lines.append(
        f"📅 {target_date.strftime('%Y-%m-%d')} "
        f"({target_date.strftime('%A')})"
    )
    lines.append("═" * 50)
    lines.append("")

    # --- HEADLINES SECTION ---
    lines.append("📰 TOP HEADLINES")
    lines.append("━" * 30)
    lines.append("")

    for i, h in enumerate(headlines, 1):
        cat = h.get('category', 'GENERAL').upper()[:6]
        lines.append(f"{i}. [{cat}] {h['headline']}")
        if h.get('summary'):
            lines.append(f"   {h['summary']}")
        lines.append("")

    # --- GOVERNMENT JOBS SECTION ---
    lines.append("")
    lines.append("🏛️ GOVERNMENT JOBS (Permanent Posts)")
    lines.append("━" * 30)
    lines.append("")

    if govt_jobs:
        for i, job in enumerate(govt_jobs, 1):
            org = job.get('organization', '')
            post = job.get('post_name', '')
            vacancies = job.get('vacancies', '')
            summary = job.get('summary', '')
            last_date = job.get('last_date', '')
            website = job.get('website', '')

            # Title
            title = f"{i}. {org} - {post}"
            if vacancies:
                title += f" ({vacancies} Vacancies)"
            lines.append(title)

            # Summary
            if summary:
                lines.append(f"   {summary}")

            # Details
            details = []
            if last_date:
                details.append(f"Last Date: {last_date}")
            if website:
                details.append(f"Apply: {website}")
            if details:
                lines.append(f"   📌 {' | '.join(details)}")

            lines.append("")
    else:
        lines.append("   [No government job updates found today]")
        lines.append("")

    lines.append("━" * 50)
    lines.append("")

    return "\n".join(lines)
```

---

## 11.8 Updated update_document Node

```python
# Replace the update_document function in nodes.py:

def update_document(state: AgentState) -> Dict:
    """Node: Update Google Doc with headlines AND govt jobs."""
    logger.info("📝 Updating Google Doc...")

    from newspaper_agent.drive.auth import GoogleAuthManager
    from newspaper_agent.docs.simple_builder import SimpleDocBuilder

    auth = GoogleAuthManager()
    docs_service = auth.get_docs_service()
    builder = SimpleDocBuilder(docs_service)

    target_date = date.fromisoformat(state['target_date'])

    # Get or create document
    doc_id = state.get('doc_id')
    if not doc_id:
        doc_id = builder.create_document("📰 Daily Headlines & Govt Jobs")

    # Add combined section (headlines + govt jobs)
    builder.prepend_daily_section_with_jobs(
        doc_id=doc_id,
        target_date=target_date,
        headlines=state['final_headlines'],
        govt_jobs=state.get('structured_govt_jobs', []),
    )

    # Save to local DB
    tracker = StateTracker()
    tracker.save_headlines(state['target_date'], state['final_headlines'])
    tracker.save_govt_jobs(state['target_date'], state.get('structured_govt_jobs', []))

    # Mark files processed
    for file_info in state['new_files']:
        if file_info.get('download_success'):
            tracker.mark_file_processed(
                file_info['file_id'],
                file_info['file_name'],
                len(state['final_headlines'])
            )

    logger.info("✅ Document updated successfully")

    return {
        'doc_id': doc_id,
        'completed': True,
        'step': 'complete',
    }
```

---

## 11.9 Updated State Tracker (SQLite)

```python
# Add to newspaper_agent/drive/state_tracker.py

# Add this table in _init_db():
"""
CREATE TABLE IF NOT EXISTS govt_jobs_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    organization TEXT,
    post_name TEXT,
    vacancies TEXT,
    eligibility TEXT,
    last_date TEXT,
    website TEXT,
    summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_govt_jobs_date
ON govt_jobs_history(date);

CREATE INDEX IF NOT EXISTS idx_govt_jobs_org
ON govt_jobs_history(organization);
"""

# Add this method:
def save_govt_jobs(self, date_str: str, jobs: List[dict]):
    """Save government job entries."""
    with sqlite3.connect(self.db_path) as conn:
        for job in jobs:
            conn.execute(
                """INSERT INTO govt_jobs_history
                   (date, organization, post_name, vacancies,
                    eligibility, last_date, website, summary)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (date_str, job.get('organization'), job.get('post_name'),
                 job.get('vacancies'), job.get('eligibility'),
                 job.get('last_date'), job.get('website'),
                 job.get('summary'))
            )

def search_govt_jobs(self, query: str, limit: int = 20) -> List[dict]:
    """Search government jobs by keyword."""
    with sqlite3.connect(self.db_path) as conn:
        conn.row_factory = sqlite3.Row
        cursor = conn.execute(
            """SELECT date, organization, post_name, vacancies,
                      last_date, website, summary
               FROM govt_jobs_history
               WHERE organization LIKE ? OR post_name LIKE ?
                  OR summary LIKE ?
               ORDER BY date DESC
               LIMIT ?""",
            (f"%{query}%", f"%{query}%", f"%{query}%", limit)
        )
        return [dict(row) for row in cursor.fetchall()]
```

---

## ⏭️ Continue to [Part 3](11c_Govt_Jobs_Search.md) for search and CLI.
