# 📰 AI Newspaper Headlines Agent - Complete Course

A production-ready AI agent that automatically reads daily newspaper uploads from Google Drive, extracts the top 50 unique headlines, enriches them with 2-3 line summaries, and maintains a navigable, searchable Google Doc organized by date.

---

## 🎯 What You'll Build

An autonomous AI agent that:
1. **Monitors** a specific Google Drive folder for daily newspaper uploads (PDF/images)
2. **Extracts text** using OCR (Tesseract/Google Vision API) from newspaper pages
3. **Identifies headlines** using NLP and layout analysis
4. **Selects top 50 unique headlines** using AI-powered deduplication and ranking
5. **Generates 2-3 line summaries** for each headline using an LLM
6. **Creates/updates a Google Doc** with date-wise sections, navigation, and search capability
7. **Runs on schedule** or is triggered by new uploads

---

## 📚 Course Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | [Foundations & Architecture](01_Foundations_Architecture.md) | System overview, tech stack, architecture design |
| 2 | [Google Drive Integration](02_Google_Drive_Integration.md) | OAuth2, Drive API, file monitoring, download |
| 3 | [OCR & Text Extraction](03_OCR_Text_Extraction.md) | Tesseract, Google Vision API, PDF parsing |
| 4 | [Headline Detection & NLP](04_Headline_Detection_NLP.md) | Layout analysis, headline identification, deduplication |
| 5 | [AI Ranking & Summarization](05_AI_Ranking_Summarization.md) | LLM integration, headline ranking, summary generation |
| 6 | [Google Docs Integration](06_Google_Docs_Integration.md) | Doc creation, date sections, navigation, formatting |
| 7 | [Search & Navigation](07_Search_Navigation.md) | Full-text search, date navigation, indexing |
| 8 | [Agent Orchestration](08_Agent_Orchestration.md) | LangChain/LangGraph agent, tools, scheduling |
| 9 | [Deployment & Production](09_Deployment_Production.md) | Docker, Cloud Run, monitoring, error handling |
| 10 | [Complete Implementation](10_Complete_Implementation.md) | Full source code, testing, and running |
| 11a | [Govt Jobs - Detection](11a_Govt_Jobs_Detection.md) | Keyword detection, confidence scoring, LLM enrichment |
| 11b | [Govt Jobs - Integration](11b_Govt_Jobs_Integration.md) | Agent pipeline integration, doc builder update |
| 11c | [Govt Jobs - Search & CLI](11c_Govt_Jobs_Search.md) | Search, filter, CLI commands, testing |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AI NEWSPAPER AGENT                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐    ┌──────────────┐    ┌────────────────┐            │
│  │  Google   │───▶│  OCR Engine  │───▶│  Headline      │            │
│  │  Drive    │    │  (Vision/    │    │  Detector      │            │
│  │  Monitor  │    │   Tesseract) │    │  (NLP/Layout)  │            │
│  └──────────┘    └──────────────┘    └────────┬───────┘            │
│                                                │                    │
│                                                ▼                    │
│  ┌──────────┐    ┌──────────────┐    ┌────────────────┐            │
│  │  Google   │◀──│  Doc Builder │◀──│  AI Ranker &   │            │
│  │  Docs     │    │  (Formatter) │    │  Summarizer    │            │
│  │  Output   │    │              │    │  (GPT/Claude)  │            │
│  └──────────┘    └──────────────┘    └────────────────┘            │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │              Agent Orchestrator (LangGraph)                │      │
│  │  • Scheduling  • Error Handling  • State Management       │      │
│  └──────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Language | Python 3.11+ | Best AI/ML ecosystem |
| AI Framework | LangChain + LangGraph | Agent orchestration |
| LLM | OpenAI GPT-4o / Claude 3.5 | Headline ranking & summarization |
| OCR | Google Cloud Vision API + Tesseract | Accurate text extraction |
| Google APIs | Drive API v3 + Docs API v1 | File monitoring & document management |
| Scheduling | APScheduler / Cloud Scheduler | Automated daily runs |
| Storage | SQLite / Redis | Local state & caching |
| Deployment | Docker + Google Cloud Run | Serverless scaling |
| Monitoring | Structured Logging + LangSmith | Observability |

---

## 📋 Prerequisites

- Python 3.11+
- Google Cloud Platform account (free tier works)
- OpenAI API key (or Anthropic/AWS Bedrock)
- Basic understanding of:
  - Python programming
  - REST APIs
  - Google Cloud Console
  - AI/LLM concepts

---

## 🚀 Quick Start

```bash
# Clone and setup
git clone <repo-url>
cd newspaper-ai-agent

# Create virtual environment
python -m venv venv
source venv/bin/activate  # macOS/Linux

# Install dependencies
pip install -r requirements.txt

# Setup Google Cloud credentials
python setup_credentials.py

# Configure environment
cp .env.example .env
# Edit .env with your API keys

# Run the agent
python -m newspaper_agent.main
```

---

## 📁 Project Structure

```
newspaper-ai-agent/
├── newspaper_agent/
│   ├── __init__.py
│   ├── main.py                    # Entry point
│   ├── config.py                  # Configuration management
│   ├── agent/
│   │   ├── __init__.py
│   │   ├── orchestrator.py        # LangGraph agent definition
│   │   ├── state.py               # Agent state management
│   │   └── nodes.py               # Agent graph nodes
│   ├── drive/
│   │   ├── __init__.py
│   │   ├── auth.py                # Google OAuth2 authentication
│   │   ├── monitor.py             # Drive folder monitoring
│   │   └── downloader.py          # File download service
│   ├── ocr/
│   │   ├── __init__.py
│   │   ├── engine.py              # OCR engine abstraction
│   │   ├── vision_api.py          # Google Vision API client
│   │   ├── tesseract.py           # Tesseract fallback
│   │   └── preprocessor.py        # Image preprocessing
│   ├── nlp/
│   │   ├── __init__.py
│   │   ├── headline_detector.py   # Headline identification
│   │   ├── deduplicator.py        # Semantic deduplication
│   │   └── ranker.py              # Headline importance ranking
│   ├── summarizer/
│   │   ├── __init__.py
│   │   ├── llm_client.py          # LLM API client
│   │   └── headline_enricher.py   # 2-3 line summary generation
│   ├── docs/
│   │   ├── __init__.py
│   │   ├── builder.py             # Google Doc builder
│   │   ├── formatter.py           # Section formatting
│   │   ├── navigator.py           # Table of contents/navigation
│   │   └── searcher.py            # Headline search functionality
│   ├── scheduler/
│   │   ├── __init__.py
│   │   └── cron.py                # Scheduled execution
│   └── utils/
│       ├── __init__.py
│       ├── logger.py              # Structured logging
│       └── retry.py               # Retry with backoff
├── tests/
│   ├── test_drive.py
│   ├── test_ocr.py
│   ├── test_nlp.py
│   ├── test_docs.py
│   └── test_agent.py
├── setup_credentials.py           # Interactive credential setup
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 📊 Output Format (Google Doc)

The agent produces a Google Doc that looks like:

```
═══════════════════════════════════════════════
📰 DAILY HEADLINES & GOVT JOBS - Master Document
═══════════════════════════════════════════════

📑 TABLE OF CONTENTS
━━━━━━━━━━━━━━━━━━━━
• 2025-01-15 (Wednesday) .................. [Jump]
• 2025-01-14 (Tuesday) .................... [Jump]
• 2025-01-13 (Monday) ..................... [Jump]
...

═══════════════════════════════════════════════
📅 2025-01-15 (Wednesday)
═══════════════════════════════════════════════

📰 TOP HEADLINES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. [POLITI] India's GDP Growth Hits 7.2% in Q3
   India's economy grew at 7.2% in the October-December
   quarter, beating expectations of 6.8%. Manufacturing
   and services sectors drove the expansion.

2. [POLITI] Supreme Court Verdict on Electoral Bonds
   The Supreme Court declared the electoral bond scheme
   unconstitutional, ordering full disclosure of all
   donor-party transactions within two weeks.

3. ...
[continues to headline 50]

🏛️ GOVERNMENT JOBS (Permanent Posts)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. UPSC - Civil Services 2025 (1000+ Vacancies)
   Applications open for IAS/IPS/IFS. Eligibility: Graduate,
   21-32 years. Apply at upsc.gov.in
   📌 Last Date: 15 Feb 2025 | Apply: upsc.gov.in

2. SSC - CGL 2025 (8000 Vacancies)
   Staff Selection Commission announced Combined Graduate Level
   exam. Posts across ministries.
   📌 Last Date: 28 Feb 2025 | Apply: ssc.nic.in

3. Indian Railways - RRB NTPC (35,000 Vacancies)
   Railway Recruitment Board invites applications for
   non-technical popular categories. 12th pass eligible.
   📌 Last Date: 10 Mar 2025 | Apply: rrbcdg.gov.in
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

═══════════════════════════════════════════════
📅 2025-01-14 (Tuesday)
═══════════════════════════════════════════════
...
```

---

## ⏭️ Next Step

Start with **[Module 1: Foundations & Architecture](01_Foundations_Architecture.md)** to understand the complete system design before writing any code.
