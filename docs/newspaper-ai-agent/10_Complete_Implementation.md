# Module 10: Complete Implementation

## 🎯 Final Integration

This module ties everything together with complete dependency files, configuration, and a step-by-step setup guide to get the agent running.

---

## 10.1 Requirements

```txt
# requirements.txt

# Google APIs
google-api-python-client==2.100.0
google-auth-httplib2==0.1.1
google-auth-oauthlib==1.1.0
google-cloud-vision==3.5.0

# AI / LLM
openai==1.12.0
anthropic==0.18.0
langchain-core==0.1.30
langgraph==0.0.40
tiktoken==0.5.2

# OCR
pytesseract==0.3.10
PyMuPDF==1.23.0
Pillow==10.0.0

# NLP
sentence-transformers==2.2.2
numpy==1.24.0

# Scheduling & Web
apscheduler==3.10.4
flask==3.0.0

# Utilities
python-dotenv==1.0.0
pydantic-settings==2.1.0

# Optional (for advanced preprocessing)
# opencv-python==4.8.0
# beautifulsoup4==4.12.0
```

---

## 10.2 Setup Script

```python
# setup_credentials.py
"""Interactive setup wizard for the Newspaper AI Agent."""

import os
import json


def main():
    print("=" * 60)
    print("📰 Newspaper AI Agent - Setup Wizard")
    print("=" * 60)
    print()

    config = {}

    # Step 1: Google Credentials
    print("Step 1: Google Cloud Credentials")
    print("-" * 40)
    cred_path = input("Path to credentials.json [credentials.json]: ").strip()
    config['AGENT_GOOGLE_CREDENTIALS_PATH'] = cred_path or 'credentials.json'

    if not os.path.exists(config['AGENT_GOOGLE_CREDENTIALS_PATH']):
        print(f"⚠️  File not found. Please download from Google Cloud Console.")
        print("   Go to: console.cloud.google.com → APIs → Credentials")
        print()

    # Step 2: Drive Folder
    print("\nStep 2: Google Drive Folder")
    print("-" * 40)
    print("Create a folder in Google Drive and share it with your service account.")
    print("The folder ID is in the URL: drive.google.com/drive/folders/FOLDER_ID")
    folder_id = input("Drive Folder ID: ").strip()
    config['AGENT_DRIVE_FOLDER_ID'] = folder_id

    # Step 3: LLM API Key
    print("\nStep 3: LLM Configuration")
    print("-" * 40)
    provider = input("LLM Provider [openai/anthropic]: ").strip() or 'openai'
    config['AGENT_LLM_PROVIDER'] = provider

    if provider == 'openai':
        config['AGENT_LLM_MODEL'] = 'gpt-4o'
        api_key = input("OpenAI API Key (sk-...): ").strip()
    else:
        config['AGENT_LLM_MODEL'] = 'claude-3-5-sonnet-20241022'
        api_key = input("Anthropic API Key: ").strip()

    config['AGENT_LLM_API_KEY'] = api_key

    # Step 4: Schedule
    print("\nStep 4: Schedule")
    print("-" * 40)
    schedule = input("Daily run time [08:00]: ").strip() or '08:00'
    timezone = input("Timezone [Asia/Kolkata]: ").strip() or 'Asia/Kolkata'
    config['AGENT_RUN_SCHEDULE'] = schedule
    config['AGENT_TIMEZONE'] = timezone

    # Step 5: Output Doc
    print("\nStep 5: Output Document")
    print("-" * 40)
    doc_id = input("Existing Doc ID (or press Enter to create new): ").strip()
    config['AGENT_OUTPUT_DOC_ID'] = doc_id
    config['AGENT_DOC_TITLE'] = '📰 Daily Headlines'

    # Defaults
    config['AGENT_OCR_ENGINE'] = 'vision_api'
    config['AGENT_MAX_HEADLINES'] = '50'
    config['AGENT_DEDUP_THRESHOLD'] = '0.85'
    config['AGENT_LOG_LEVEL'] = 'INFO'

    # Write .env
    env_content = "\n".join(f"{k}={v}" for k, v in config.items())

    with open('.env', 'w') as f:
        f.write(env_content)

    print("\n" + "=" * 60)
    print("✅ Setup complete! Configuration saved to .env")
    print("=" * 60)
    print("\nNext steps:")
    print("  1. Verify: python -m newspaper_agent.main verify")
    print("  2. Run once: python -m newspaper_agent.main run")
    print("  3. Schedule: python -m newspaper_agent.main schedule")


if __name__ == '__main__':
    main()
```

---

## 10.3 Complete Step-by-Step Setup

### Prerequisites

```bash
# macOS
brew install python@3.11 tesseract poppler

# Ubuntu
sudo apt install python3.11 tesseract-ocr poppler-utils
```

### Setup Steps

```bash
# 1. Create project directory
mkdir newspaper-ai-agent && cd newspaper-ai-agent

# 2. Create virtual environment
python3.11 -m venv venv
source venv/bin/activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Set up Google Cloud
#    a. Go to console.cloud.google.com
#    b. Create project "newspaper-agent"
#    c. Enable APIs: Drive, Docs, Vision
#    d. Create service account → Download credentials.json
#    e. Place credentials.json in project root

# 5. Set up Google Drive
#    a. Create folder "Daily Newspapers" in Google Drive
#    b. Share folder with service account email (Viewer access)
#    c. Copy folder ID from URL
#    d. Create/share output doc with service account (Editor access)

# 6. Run setup wizard
python setup_credentials.py

# 7. Verify everything works
python -m newspaper_agent.main verify

# 8. Upload a test newspaper PDF to your Drive folder

# 9. Run the agent
python -m newspaper_agent.main run

# 10. Check output
#     Open the Google Doc to see extracted headlines!
```

---

## 10.4 Example Run Output

```
$ python -m newspaper_agent.main run

2025-01-15 08:00:01 [INFO] agent: 🚀 Starting newspaper agent for 2025-01-15
2025-01-15 08:00:02 [INFO] monitor: Found 3 files in Drive folder
2025-01-15 08:00:02 [INFO] monitor: Total: 3, Processed: 0, New: 3
2025-01-15 08:00:05 [INFO] downloader: Downloaded: TOI_15Jan.pdf (4.2 MB)
2025-01-15 08:00:08 [INFO] downloader: Downloaded: HT_15Jan.pdf (3.8 MB)
2025-01-15 08:00:10 [INFO] downloader: Downloaded: Hindu_15Jan.pdf (5.1 MB)
2025-01-15 08:00:10 [INFO] pdf_extractor: PDF: 20 pages, DIGITAL
2025-01-15 08:00:15 [INFO] pdf_extractor: PDF: 18 pages, DIGITAL
2025-01-15 08:00:19 [INFO] pdf_extractor: PDF: 24 pages, DIGITAL
2025-01-15 08:00:19 [INFO] detector: Found 187 raw candidates
2025-01-15 08:00:22 [INFO] deduplicator: 187 → 94 (removed 93 duplicates)
2025-01-15 08:00:25 [INFO] ranker: Ranking 94 candidates → top 50
2025-01-15 08:00:35 [INFO] enricher: Generated summaries for 50 headlines
2025-01-15 08:00:38 [INFO] builder: Document updated with 50 headlines
2025-01-15 08:00:38 [INFO] agent: ✅ Agent completed successfully!
   Files processed: 3
   Headlines found: 50
   Duration: 37.2s
   Cost: ~$0.09
   Document: https://docs.google.com/document/d/1abc.../edit
```

---

## 10.5 Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| "Access denied" on Drive | Service account not shared | Share folder with SA email |
| "Vision API disabled" | API not enabled | Enable in Cloud Console |
| "No text extracted" | Scanned PDF, low quality | Use `vision_api` engine, preprocess |
| "Too few headlines" | Small/single newspaper | Lower `min_words` threshold |
| "Rate limit exceeded" | Too many API calls | Add delays, reduce batch size |
| "Doc update failed" | Doc not shared with SA | Share doc with Editor access |
| "LLM returned invalid JSON" | Model hallucination | Retry, use lower temperature |

---

## 10.6 Extending the Agent

### Add More Newspapers
Simply upload more newspaper files (PDF/images) to the Drive folder. The agent processes all new files.

### Add Email Summary
```python
# Add after update_document node
def send_email_summary(state: AgentState) -> Dict:
    """Send top 10 headlines via email."""
    import smtplib
    from email.mime.text import MIMEText

    top_10 = state['final_headlines'][:10]
    body = "\n".join(
        f"{h['rank']}. {h['headline']}\n   {h['summary']}\n"
        for h in top_10
    )

    msg = MIMEText(body)
    msg['Subject'] = f"📰 Top Headlines - {state['target_date']}"
    # ... send email
```

### Add Telegram Bot
```python
# Send headlines to Telegram channel
import requests

def send_to_telegram(headlines, bot_token, chat_id):
    text = "📰 *Today's Top Headlines*\n\n"
    for h in headlines[:10]:
        text += f"*{h['rank']}.* {h['headline']}\n"
        text += f"_{h['summary'][:100]}_\n\n"

    requests.post(
        f"https://api.telegram.org/bot{bot_token}/sendMessage",
        json={'chat_id': chat_id, 'text': text, 'parse_mode': 'Markdown'}
    )
```

### Multi-language Support
```python
# For Hindi/regional newspapers
ocr_engine = OCREngine(primary="vision_api")  # Vision API auto-detects language

# For Tesseract
tesseract_client = TesseractClient(lang='hin+eng')  # Hindi + English
```

---

## 10.7 Performance Benchmarks

| Metric | Value |
|--------|-------|
| 3 newspapers (60 pages) | ~40 seconds total |
| OCR (Vision API, 20 pages) | ~5 seconds |
| OCR (Tesseract, 20 pages) | ~30 seconds |
| Headline detection | ~1 second |
| Deduplication (200 candidates) | ~3 seconds |
| LLM ranking (100 headlines) | ~8 seconds |
| LLM summarization (50 headlines) | ~15 seconds |
| Doc update | ~2 seconds |
| **Total pipeline** | **~35-45 seconds** |

---

## 10.8 Final Architecture Recap

```
┌─────────────────────────────────────────────────────────────────┐
│                    NEWSPAPER AI AGENT                             │
│                                                                 │
│   Trigger: Cloud Scheduler (8 AM daily)                         │
│            OR manual: python -m newspaper_agent.main run         │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │  LangGraph Orchestrator                                  │   │
│   │                                                         │   │
│   │  check_drive → download → OCR → detect →                │   │
│   │  rank → summarize → update_doc → END                    │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   External Services:                                            │
│   • Google Drive API (input files)                              │
│   • Google Vision API (OCR)                                     │
│   • OpenAI GPT-4o (ranking + summaries)                         │
│   • Google Docs API (output document)                           │
│                                                                 │
│   Storage:                                                      │
│   • SQLite (processed files, headline history, search index)    │
│   • Google Doc (output for human consumption)                   │
│                                                                 │
│   Deployment:                                                   │
│   • Docker → Cloud Run (serverless)                             │
│   • Cloud Scheduler (daily trigger)                             │
│   • ~$5/month total cost                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10.9 What You've Built

✅ A complete AI agent that:
1. Monitors Google Drive for daily newspaper uploads
2. Extracts text using OCR (Vision API + Tesseract fallback)
3. Detects headlines using font analysis and NLP heuristics
4. Removes duplicates using semantic similarity (embeddings)
5. Ranks top 50 by importance using GPT-4o
6. Generates 2-3 line summaries for each headline
7. Creates a navigable Google Doc with date sections
8. Supports keyword and semantic search across all headlines
9. Runs automatically on schedule
10. Handles errors gracefully with retries

---

## 🎉 Course Complete!

You now have a production-ready AI newspaper agent. Key skills gained:
- Google API integration (Drive, Docs, Vision)
- OCR and document processing
- NLP (headline detection, deduplication, classification)
- LLM integration (prompt engineering, JSON parsing, batching)
- Agent architecture (LangGraph, state machines)
- Production deployment (Docker, Cloud Run, scheduling)
- Search and navigation systems
