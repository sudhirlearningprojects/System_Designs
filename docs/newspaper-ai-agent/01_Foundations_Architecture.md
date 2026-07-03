# Module 1: Foundations & Architecture

## 🎯 Learning Objectives

By the end of this module, you'll understand:
- The complete system architecture and data flow
- Why each technology was chosen
- How the AI agent makes decisions
- The state machine driving the agent
- Error handling and recovery strategies

---

## 1.1 Problem Breakdown

### What the Agent Must Do (Daily Workflow)

```
TRIGGER (New files in Drive / Scheduled time)
    │
    ▼
STEP 1: Check Google Drive folder for new newspaper files
    │    - Detect new PDFs/images since last run
    │    - Download to local temp storage
    │
    ▼
STEP 2: Extract text from newspaper pages
    │    - OCR for scanned images
    │    - PDF text extraction for digital PDFs
    │    - Preserve layout information (font size, position)
    │
    ▼
STEP 3: Identify headlines from extracted text
    │    - Use font size, boldness, position as signals
    │    - Distinguish headlines from body text, ads, captions
    │    - Extract ~100-200 candidate headlines
    │
    ▼
STEP 4: Select top 50 unique headlines
    │    - Semantic deduplication (remove near-duplicates)
    │    - Importance ranking using LLM
    │    - Diversity scoring (cover multiple topics)
    │
    ▼
STEP 5: Generate 2-3 line summaries for each
    │    - Use surrounding article context
    │    - Keep factual and concise
    │
    ▼
STEP 6: Update Google Doc
    │    - Create new date section
    │    - Format headlines with summaries
    │    - Update Table of Contents
    │    - Enable navigation links
    │
    ▼
DONE: Log results, clean up temp files
```

---

## 1.2 Architecture Deep Dive

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         NEWSPAPER AI AGENT                               │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    AGENT ORCHESTRATOR (LangGraph)                   │  │
│  │                                                                   │  │
│  │  State: {files, raw_text, headlines, ranked, summaries, doc_id}   │  │
│  │                                                                   │  │
│  │  Nodes:                                                           │  │
│  │  [Monitor] → [Download] → [OCR] → [Detect] → [Rank] →           │  │
│  │  [Summarize] → [UpdateDoc] → [END]                               │  │
│  │                                                                   │  │
│  │  Conditional Edges:                                               │  │
│  │  • No new files? → END                                           │  │
│  │  • OCR failed? → Retry with different engine                     │  │
│  │  • Doc update failed? → Retry with backoff                       │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐  │
│  │  Drive  │  │   OCR    │  │   NLP   │  │   LLM    │  │  Google  │  │
│  │  Tool   │  │   Tool   │  │   Tool  │  │   Tool   │  │  Docs    │  │
│  │         │  │          │  │         │  │          │  │  Tool    │  │
│  └─────────┘  └──────────┘  └─────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
         │              │            │            │              │
         ▼              ▼            ▼            ▼              ▼
   Google Drive    Vision API    spaCy/      OpenAI/       Google Docs
   API v3         Tesseract     NLTK        Claude         API v1
```

### Data Flow Diagram

```
┌──────────────┐     ┌──────────────────┐     ┌────────────────────┐
│   Newspaper  │     │   Temp Storage   │     │   Processed Data   │
│   Files in   │────▶│   /tmp/papers/   │────▶│   SQLite DB        │
│   Drive      │     │   date/          │     │   (tracking)       │
└──────────────┘     └──────────────────┘     └────────────────────┘
                              │
                              ▼
┌──────────────┐     ┌──────────────────┐     ┌────────────────────┐
│   Google     │     │   Headlines +    │     │   Raw OCR Text     │
│   Doc        │◀────│   Summaries      │◀────│   with Layout      │
│   Output     │     │   (top 50)       │     │   Metadata         │
└──────────────┘     └──────────────────┘     └────────────────────┘
```

---

## 1.3 Technology Decisions

### Why LangGraph over Plain Python?

| Aspect | Plain Python | LangGraph |
|--------|-------------|-----------|
| State management | Manual dict passing | Built-in typed state |
| Error recovery | Custom try/except | Automatic checkpointing |
| Retry logic | Manual implementation | Built-in with conditional edges |
| Observability | Custom logging | LangSmith integration |
| Human-in-the-loop | Complex to add | Built-in interrupt/resume |
| Parallel execution | Threading/asyncio | Built-in parallel nodes |

**Decision**: LangGraph gives us production-grade agent capabilities with minimal boilerplate.

### Why Google Vision API over Tesseract Only?

| Aspect | Tesseract | Google Vision API |
|--------|-----------|-------------------|
| Accuracy on newspapers | 70-80% | 95%+ |
| Handles skew/rotation | Poor | Excellent |
| Multi-language | Needs lang packs | Automatic |
| Layout detection | Basic | Document AI quality |
| Cost | Free | $1.50/1000 pages |
| Speed | Slow (local) | Fast (cloud) |

**Decision**: Use Vision API as primary, Tesseract as free fallback for non-critical runs.

### Why a Single Google Doc (not multiple)?

- **Navigability**: One document with a Table of Contents is easier to browse
- **Searchability**: Ctrl+F works across all dates in one doc
- **Simplicity**: No file management overhead
- **History**: All headlines in one chronological record
- **Google Docs limit**: 1.02 million characters ≈ ~2 years of daily headlines

---

## 1.4 Agent State Design

```python
from typing import TypedDict, List, Optional
from datetime import date


class NewspaperFile(TypedDict):
    file_id: str
    file_name: str
    mime_type: str
    local_path: str


class ExtractedText(TypedDict):
    file_id: str
    page_number: int
    raw_text: str
    layout_blocks: List[dict]  # {text, font_size, is_bold, position}


class Headline(TypedDict):
    text: str
    source_file: str
    page_number: int
    confidence: float
    category: str  # politics, business, sports, etc.
    context: str   # surrounding paragraph for summary generation


class RankedHeadline(TypedDict):
    headline: str
    rank: int
    summary: str  # 2-3 lines
    category: str
    importance_score: float


class AgentState(TypedDict):
    """Complete state of the newspaper agent at any point."""
    # Input
    target_date: date
    drive_folder_id: str
    doc_id: Optional[str]
    
    # Processing stages
    new_files: List[NewspaperFile]
    extracted_texts: List[ExtractedText]
    candidate_headlines: List[Headline]
    ranked_headlines: List[RankedHeadline]  # Final top 50
    
    # Output
    doc_updated: bool
    sections_added: List[str]
    
    # Metadata
    errors: List[str]
    retry_count: int
    processed_file_ids: List[str]  # Track what's been processed
```

---

## 1.5 Agent Graph Design (LangGraph)

```python
from langgraph.graph import StateGraph, END

# Define the agent workflow
workflow = StateGraph(AgentState)

# Add nodes (each is a function that takes state and returns partial state)
workflow.add_node("check_drive", check_drive_for_new_files)
workflow.add_node("download_files", download_newspaper_files)
workflow.add_node("extract_text", run_ocr_extraction)
workflow.add_node("detect_headlines", identify_headlines)
workflow.add_node("rank_and_dedupe", rank_and_deduplicate)
workflow.add_node("generate_summaries", generate_headline_summaries)
workflow.add_node("update_doc", update_google_doc)

# Define edges
workflow.set_entry_point("check_drive")

workflow.add_conditional_edges(
    "check_drive",
    has_new_files,  # returns "download" or "end"
    {"download": "download_files", "end": END}
)

workflow.add_edge("download_files", "extract_text")
workflow.add_edge("extract_text", "detect_headlines")
workflow.add_edge("detect_headlines", "rank_and_dedupe")
workflow.add_edge("rank_and_dedupe", "generate_summaries")
workflow.add_edge("generate_summaries", "update_doc")
workflow.add_edge("update_doc", END)

# Compile
agent = workflow.compile()
```

### Visual Graph

```
            ┌──────────────┐
            │  check_drive │
            └──────┬───────┘
                   │
          ┌────────┴────────┐
          │                 │
    has_new_files?     no_new_files
          │                 │
          ▼                 ▼
  ┌───────────────┐      [END]
  │download_files │
  └───────┬───────┘
          │
          ▼
  ┌───────────────┐
  │ extract_text  │
  └───────┬───────┘
          │
          ▼
  ┌────────────────┐
  │detect_headlines│
  └───────┬────────┘
          │
          ▼
  ┌────────────────┐
  │rank_and_dedupe │
  └───────┬────────┘
          │
          ▼
  ┌──────────────────┐
  │generate_summaries│
  └───────┬──────────┘
          │
          ▼
  ┌───────────────┐
  │  update_doc   │
  └───────┬───────┘
          │
          ▼
        [END]
```

---

## 1.6 Error Handling Strategy

### Retry Matrix

| Operation | Max Retries | Backoff | Fallback |
|-----------|-------------|---------|----------|
| Drive API call | 3 | Exponential (2s, 4s, 8s) | Log and skip |
| File download | 3 | Linear (5s) | Skip file, continue |
| OCR (Vision API) | 2 | Exponential (3s, 9s) | Switch to Tesseract |
| LLM API call | 3 | Exponential (2s, 4s, 8s) | Use simpler prompt |
| Doc update | 5 | Exponential (1s, 2s, 4s, 8s, 16s) | Save locally, retry later |

### Circuit Breaker Pattern

```python
class CircuitBreaker:
    """Prevent repeated calls to failing services."""
    
    def __init__(self, failure_threshold=5, recovery_timeout=60):
        self.failure_count = 0
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.state = "CLOSED"  # CLOSED, OPEN, HALF_OPEN
        self.last_failure_time = None
    
    def can_execute(self) -> bool:
        if self.state == "CLOSED":
            return True
        if self.state == "OPEN":
            if time.time() - self.last_failure_time > self.recovery_timeout:
                self.state = "HALF_OPEN"
                return True
            return False
        return True  # HALF_OPEN allows one attempt
    
    def record_success(self):
        self.failure_count = 0
        self.state = "CLOSED"
    
    def record_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.time()
        if self.failure_count >= self.failure_threshold:
            self.state = "OPEN"
```

---

## 1.7 Configuration Management

```python
# config.py
from pydantic_settings import BaseSettings
from typing import Optional


class AgentConfig(BaseSettings):
    """All configuration via environment variables."""
    
    # Google Drive
    drive_folder_id: str
    google_credentials_path: str = "credentials.json"
    
    # Google Docs
    output_doc_id: Optional[str] = None  # None = create new
    doc_title: str = "📰 Daily Headlines - Master Document"
    
    # OCR
    ocr_engine: str = "vision_api"  # vision_api | tesseract | both
    vision_api_max_pages: int = 50
    
    # LLM
    llm_provider: str = "openai"  # openai | anthropic | bedrock
    llm_model: str = "gpt-4o"
    llm_api_key: str
    llm_temperature: float = 0.3
    
    # Headlines
    max_headlines: int = 50
    dedup_similarity_threshold: float = 0.85
    
    # Scheduling
    run_schedule: str = "08:00"  # Daily at 8 AM
    timezone: str = "Asia/Kolkata"
    
    # Monitoring
    langsmith_api_key: Optional[str] = None
    log_level: str = "INFO"
    
    class Config:
        env_file = ".env"
        env_prefix = "AGENT_"
```

---

## 1.8 Cost Estimation

### Per Daily Run (assuming 20 newspaper pages)

| Service | Usage | Cost |
|---------|-------|------|
| Google Vision API | 20 pages | $0.03 |
| OpenAI GPT-4o | ~10K tokens in, ~5K out | $0.08 |
| Google Drive API | ~25 calls | Free |
| Google Docs API | ~5 calls | Free |
| **Total per day** | | **~$0.11** |
| **Monthly** | 30 runs | **~$3.30** |

### Free Tier Alternative

| Service | Alternative | Cost |
|---------|-------------|------|
| Vision API → Tesseract | Local OCR | Free |
| GPT-4o → GPT-4o-mini | Cheaper model | ~$0.01/run |
| **Monthly (budget)** | | **~$0.30** |

---

## 1.9 Security Considerations

1. **Google OAuth2 Scope Minimization**:
   - Only request `drive.readonly` for the specific folder
   - Only request `documents` scope for doc editing
   
2. **API Key Storage**:
   - Never commit `.env` to version control
   - Use Google Secret Manager in production
   
3. **Data Privacy**:
   - Newspaper content processed in memory, not stored permanently
   - Temp files deleted after processing
   - No PII extraction or storage

4. **Access Control**:
   - The agent only has access to ONE specific Drive folder
   - Service account with minimal permissions
   - Doc shared only with the owner

---

## 1.10 Key Design Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Single Doc vs Multiple | Single | Searchability, simplicity |
| Append vs Prepend | Prepend (newest first) | Latest headlines at top |
| Section format | Date headers with numbered list | Clear navigation |
| Summary length | 2-3 lines (50-80 words) | Enough context, not overwhelming |
| Headline count | 50 | Covers major news without noise |
| Deduplication | Semantic (embeddings) | Catches paraphrased duplicates |
| Scheduling | Event-driven + cron backup | Immediate + guaranteed daily |
| State persistence | SQLite | Simple, no external DB needed |

---

## ⏭️ Next Module

Proceed to **[Module 2: Google Drive Integration](02_Google_Drive_Integration.md)** to set up Google Cloud credentials and build the Drive monitoring system.
