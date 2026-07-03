# Module 8: Agent Orchestration

## 🎯 Learning Objectives

- Build the complete agent using LangGraph
- Define state, nodes, and conditional edges
- Implement scheduling (daily automated runs)
- Add human-in-the-loop for quality checks
- Handle errors and retries at the orchestration level

---

## 8.1 Agent State Definition

```python
# newspaper_agent/agent/state.py

from typing import TypedDict, List, Optional, Annotated
from datetime import date, datetime
from langgraph.graph.message import add_messages


class NewspaperFile(TypedDict):
    file_id: str
    file_name: str
    mime_type: str
    local_path: Optional[str]
    download_success: bool


class ProcessedHeadline(TypedDict):
    headline: str
    rank: int
    summary: str
    category: str
    confidence: float
    page_number: int


class AgentState(TypedDict):
    """Complete agent state passed between nodes."""
    # Configuration
    target_date: str
    drive_folder_id: str
    doc_id: Optional[str]

    # Pipeline data
    new_files: List[NewspaperFile]
    extracted_pages: List[dict]
    candidate_headlines: List[dict]
    final_headlines: List[ProcessedHeadline]

    # Status
    step: str
    error: Optional[str]
    retry_count: int
    completed: bool

    # Metrics
    files_processed: int
    headlines_found: int
    run_start_time: str
```

---

## 8.2 Agent Nodes (Processing Steps)

```python
# newspaper_agent/agent/nodes.py

from typing import Dict
from datetime import date, datetime
from newspaper_agent.agent.state import AgentState
from newspaper_agent.drive import DriveService
from newspaper_agent.ocr.engine import OCREngine
from newspaper_agent.nlp.headline_detector import HeadlineDetector
from newspaper_agent.nlp.deduplicator import HeadlineDeduplicator
from newspaper_agent.nlp.classifier import HeadlineCategorizer
from newspaper_agent.summarizer import HeadlinePipeline
from newspaper_agent.docs.simple_builder import SimpleDocBuilder
from newspaper_agent.drive.state_tracker import StateTracker
import logging

logger = logging.getLogger(__name__)


def check_drive(state: AgentState) -> Dict:
    """Node 1: Check Google Drive for new newspaper files."""
    logger.info("🔍 Checking Drive for new files...")

    drive = DriveService(
        folder_id=state['drive_folder_id'],
    )

    new_files = drive.check_for_new_files()

    return {
        'new_files': new_files,
        'step': 'check_drive_complete',
        'error': None,
    }


def download_files(state: AgentState) -> Dict:
    """Node 2: Download newspaper files from Drive."""
    logger.info(f"📥 Downloading {len(state['new_files'])} files...")

    drive = DriveService(folder_id=state['drive_folder_id'])
    downloaded = drive.download_files(state['new_files'])

    successful = [f for f in downloaded if f.get('download_success')]

    return {
        'new_files': downloaded,
        'files_processed': len(successful),
        'step': 'download_complete',
    }


def extract_text(state: AgentState) -> Dict:
    """Node 3: Run OCR on downloaded files."""
    logger.info("📄 Extracting text from newspapers...")

    engine = OCREngine(primary="vision_api")
    all_pages = []

    for file_info in state['new_files']:
        if not file_info.get('download_success'):
            continue

        try:
            pages = engine.extract_from_file(file_info['local_path'])
            for page in pages:
                page['source_file'] = file_info['file_name']
            all_pages.extend(pages)
        except Exception as e:
            logger.error(f"OCR failed for {file_info['file_name']}: {e}")

    logger.info(f"Extracted text from {len(all_pages)} pages")

    return {
        'extracted_pages': all_pages,
        'step': 'extract_complete',
    }


def detect_headlines(state: AgentState) -> Dict:
    """Node 4: Identify headline candidates from OCR text."""
    logger.info("🔎 Detecting headlines...")

    detector = HeadlineDetector()
    deduplicator = HeadlineDeduplicator(similarity_threshold=0.85)
    categorizer = HeadlineCategorizer()

    # Detect candidates
    candidates = detector.detect_headlines(state['extracted_pages'])
    logger.info(f"Found {len(candidates)} raw candidates")

    # Deduplicate
    unique = deduplicator.deduplicate(candidates)
    logger.info(f"After dedup: {len(unique)} unique headlines")

    # Categorize
    categorized = categorizer.categorize_batch(unique)

    return {
        'candidate_headlines': categorized,
        'step': 'detect_complete',
    }


def rank_and_summarize(state: AgentState) -> Dict:
    """Node 5: Rank headlines and generate summaries using LLM."""
    logger.info("🏆 Ranking headlines and generating summaries...")

    import os
    pipeline = HeadlinePipeline(
        api_key=os.getenv('AGENT_LLM_API_KEY', ''),
        provider=os.getenv('AGENT_LLM_PROVIDER', 'openai'),
        model=os.getenv('AGENT_LLM_MODEL', 'gpt-4o'),
        target_headlines=50,
    )

    final = pipeline.process(state['candidate_headlines'])

    return {
        'final_headlines': final,
        'headlines_found': len(final),
        'step': 'rank_complete',
    }


def update_document(state: AgentState) -> Dict:
    """Node 6: Update Google Doc with today's headlines."""
    logger.info("📝 Updating Google Doc...")

    from newspaper_agent.drive.auth import GoogleAuthManager

    auth = GoogleAuthManager()
    docs_service = auth.get_docs_service()

    builder = SimpleDocBuilder(docs_service)

    # Parse target date
    target_date = date.fromisoformat(state['target_date'])

    # Get or create document
    doc_id = state.get('doc_id')
    if not doc_id:
        doc_id = builder.create_document("📰 Daily Headlines")

    # Add today's section
    builder.prepend_daily_headlines(
        doc_id=doc_id,
        target_date=target_date,
        headlines=state['final_headlines'],
    )

    # Save headlines to local DB for search
    tracker = StateTracker()
    tracker.save_headlines(
        state['target_date'],
        state['final_headlines']
    )

    # Mark files as processed
    for file_info in state['new_files']:
        if file_info.get('download_success'):
            tracker.mark_file_processed(
                file_info['file_id'],
                file_info['file_name'],
                len(state['final_headlines'])
            )

    logger.info(
        f"✅ Document updated with {len(state['final_headlines'])} headlines"
    )

    return {
        'doc_id': doc_id,
        'completed': True,
        'step': 'complete',
    }
```

---

## 8.3 Conditional Edge Functions

```python
# newspaper_agent/agent/conditions.py

from newspaper_agent.agent.state import AgentState


def has_new_files(state: AgentState) -> str:
    """Route based on whether new files were found."""
    if state.get('new_files') and len(state['new_files']) > 0:
        return "download"
    return "end"


def should_retry(state: AgentState) -> str:
    """Route based on error state and retry count."""
    if state.get('error') and state.get('retry_count', 0) < 3:
        return "retry"
    elif state.get('error'):
        return "fail"
    return "continue"


def has_candidates(state: AgentState) -> str:
    """Check if headline candidates were found."""
    candidates = state.get('candidate_headlines', [])
    if len(candidates) >= 5:  # Need at least 5 to be useful
        return "rank"
    return "insufficient"
```

---

## 8.4 Complete Agent Graph

```python
# newspaper_agent/agent/orchestrator.py

from langgraph.graph import StateGraph, END
from newspaper_agent.agent.state import AgentState
from newspaper_agent.agent.nodes import (
    check_drive,
    download_files,
    extract_text,
    detect_headlines,
    rank_and_summarize,
    update_document,
)
from newspaper_agent.agent.conditions import has_new_files, has_candidates
import logging

logger = logging.getLogger(__name__)


def build_agent() -> StateGraph:
    """Build the newspaper agent workflow graph."""

    workflow = StateGraph(AgentState)

    # Add nodes
    workflow.add_node("check_drive", check_drive)
    workflow.add_node("download_files", download_files)
    workflow.add_node("extract_text", extract_text)
    workflow.add_node("detect_headlines", detect_headlines)
    workflow.add_node("rank_and_summarize", rank_and_summarize)
    workflow.add_node("update_document", update_document)

    # Set entry point
    workflow.set_entry_point("check_drive")

    # Conditional: new files found?
    workflow.add_conditional_edges(
        "check_drive",
        has_new_files,
        {
            "download": "download_files",
            "end": END,
        }
    )

    # Linear flow after download
    workflow.add_edge("download_files", "extract_text")
    workflow.add_edge("extract_text", "detect_headlines")

    # Conditional: enough candidates?
    workflow.add_conditional_edges(
        "detect_headlines",
        has_candidates,
        {
            "rank": "rank_and_summarize",
            "insufficient": END,  # Not enough headlines found
        }
    )

    workflow.add_edge("rank_and_summarize", "update_document")
    workflow.add_edge("update_document", END)

    return workflow.compile()


def run_agent(
    drive_folder_id: str,
    doc_id: str = None,
    target_date: str = None,
):
    """Run the newspaper agent."""
    from datetime import date as date_type

    if not target_date:
        target_date = date_type.today().isoformat()

    initial_state = {
        'target_date': target_date,
        'drive_folder_id': drive_folder_id,
        'doc_id': doc_id,
        'new_files': [],
        'extracted_pages': [],
        'candidate_headlines': [],
        'final_headlines': [],
        'step': 'starting',
        'error': None,
        'retry_count': 0,
        'completed': False,
        'files_processed': 0,
        'headlines_found': 0,
        'run_start_time': datetime.now().isoformat(),
    }

    agent = build_agent()

    logger.info(f"🚀 Starting newspaper agent for {target_date}")

    # Run the agent
    from datetime import datetime
    final_state = agent.invoke(initial_state)

    if final_state.get('completed'):
        logger.info(
            f"✅ Agent completed successfully!\n"
            f"   Files processed: {final_state['files_processed']}\n"
            f"   Headlines found: {final_state['headlines_found']}\n"
            f"   Document: https://docs.google.com/document/d/{final_state['doc_id']}/edit"
        )
    else:
        logger.info("ℹ️ Agent completed - no new files to process")

    return final_state
```

---

## 8.5 Scheduling

```python
# newspaper_agent/scheduler/cron.py

from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger
from newspaper_agent.agent.orchestrator import run_agent
import os
import logging

logger = logging.getLogger(__name__)


def scheduled_run():
    """Scheduled daily run of the agent."""
    logger.info("⏰ Scheduled run triggered")

    try:
        result = run_agent(
            drive_folder_id=os.getenv('AGENT_DRIVE_FOLDER_ID'),
            doc_id=os.getenv('AGENT_OUTPUT_DOC_ID'),
        )
        logger.info(f"Scheduled run completed: {result.get('step')}")
    except Exception as e:
        logger.error(f"Scheduled run failed: {e}", exc_info=True)


def start_scheduler():
    """Start the scheduled execution."""
    scheduler = BlockingScheduler()

    # Run daily at configured time
    run_time = os.getenv('AGENT_RUN_SCHEDULE', '08:00')
    hour, minute = run_time.split(':')
    timezone = os.getenv('AGENT_TIMEZONE', 'Asia/Kolkata')

    scheduler.add_job(
        scheduled_run,
        trigger=CronTrigger(
            hour=int(hour),
            minute=int(minute),
            timezone=timezone
        ),
        id='daily_newspaper_run',
        name='Daily Newspaper Headlines Extraction',
    )

    logger.info(
        f"📅 Scheduler started. Next run at {run_time} {timezone}"
    )

    try:
        scheduler.start()
    except (KeyboardInterrupt, SystemExit):
        logger.info("Scheduler stopped")
```

---

## 8.6 Main Entry Point

```python
# newspaper_agent/main.py

import argparse
import os
import logging
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=os.getenv('AGENT_LOG_LEVEL', 'INFO'),
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
)


def main():
    parser = argparse.ArgumentParser(description="Newspaper AI Agent")
    subparsers = parser.add_subparsers(dest='command')

    # Run once
    run_parser = subparsers.add_parser('run', help='Run agent once')
    run_parser.add_argument('--date', help='Target date (YYYY-MM-DD)')

    # Start scheduler
    subparsers.add_parser('schedule', help='Start scheduled runs')

    # Search
    search_parser = subparsers.add_parser('search', help='Search headlines')
    search_parser.add_argument('query', help='Search query')

    # Verify setup
    subparsers.add_parser('verify', help='Verify configuration')

    args = parser.parse_args()

    if args.command == 'run':
        from newspaper_agent.agent.orchestrator import run_agent
        run_agent(
            drive_folder_id=os.getenv('AGENT_DRIVE_FOLDER_ID'),
            doc_id=os.getenv('AGENT_OUTPUT_DOC_ID'),
            target_date=args.date,
        )

    elif args.command == 'schedule':
        from newspaper_agent.scheduler.cron import start_scheduler
        start_scheduler()

    elif args.command == 'search':
        from newspaper_agent.docs.search_cli import main as search_main
        search_main()

    elif args.command == 'verify':
        _verify_setup()

    else:
        parser.print_help()


def _verify_setup():
    """Verify all configuration and connectivity."""
    print("🔍 Verifying setup...\n")

    checks = []

    # Check env vars
    required = ['AGENT_DRIVE_FOLDER_ID', 'AGENT_LLM_API_KEY']
    for var in required:
        if os.getenv(var):
            checks.append(f"✅ {var} is set")
        else:
            checks.append(f"❌ {var} is missing")

    # Check credentials file
    cred_path = os.getenv('AGENT_GOOGLE_CREDENTIALS_PATH', 'credentials.json')
    if os.path.exists(cred_path):
        checks.append(f"✅ Credentials file found: {cred_path}")
    else:
        checks.append(f"❌ Credentials file missing: {cred_path}")

    # Check Drive access
    try:
        from newspaper_agent.drive import DriveService
        drive = DriveService(folder_id=os.getenv('AGENT_DRIVE_FOLDER_ID', ''))
        if drive.verify_setup():
            checks.append("✅ Google Drive access verified")
        else:
            checks.append("❌ Google Drive access failed")
    except Exception as e:
        checks.append(f"❌ Drive check error: {e}")

    print("\n".join(checks))


if __name__ == '__main__':
    main()
```

Usage:
```bash
# Run once (today)
python -m newspaper_agent.main run

# Run for specific date
python -m newspaper_agent.main run --date 2025-01-15

# Start scheduler (runs daily at configured time)
python -m newspaper_agent.main schedule

# Verify setup
python -m newspaper_agent.main verify
```

---

## 8.7 Dependencies

```bash
pip install langgraph==0.0.40
pip install langchain-core==0.1.30
pip install apscheduler==3.10.4
pip install python-dotenv==1.0.0
```

---

## ⏭️ Next Module

Proceed to **[Module 9: Deployment & Production](09_Deployment_Production.md)** for containerization and cloud deployment.
