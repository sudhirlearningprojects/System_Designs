# Module 9: Deployment & Production

## 🎯 Learning Objectives

- Containerize the agent with Docker
- Deploy to Google Cloud Run
- Set up Cloud Scheduler for daily triggers
- Implement monitoring and alerting
- Handle production error scenarios

---

## 9.1 Docker Configuration

```dockerfile
# Dockerfile

FROM python:3.11-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    poppler-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY newspaper_agent/ ./newspaper_agent/
COPY credentials.json .

# Create temp directory
RUN mkdir -p /tmp/newspaper_agent

ENV PYTHONPATH=/app
ENV AGENT_LOG_LEVEL=INFO

# Entry point
ENTRYPOINT ["python", "-m", "newspaper_agent.main"]
CMD ["run"]
```

```yaml
# docker-compose.yml

version: '3.8'

services:
  newspaper-agent:
    build: .
    environment:
      - AGENT_DRIVE_FOLDER_ID=${AGENT_DRIVE_FOLDER_ID}
      - AGENT_OUTPUT_DOC_ID=${AGENT_OUTPUT_DOC_ID}
      - AGENT_LLM_API_KEY=${AGENT_LLM_API_KEY}
      - AGENT_LLM_PROVIDER=openai
      - AGENT_LLM_MODEL=gpt-4o
      - AGENT_OCR_ENGINE=vision_api
      - AGENT_RUN_SCHEDULE=08:00
      - AGENT_TIMEZONE=Asia/Kolkata
      - GOOGLE_APPLICATION_CREDENTIALS=/app/credentials.json
    volumes:
      - ./credentials.json:/app/credentials.json:ro
      - ./agent_state.db:/app/agent_state.db
    restart: unless-stopped
```

Build and run:
```bash
docker build -t newspaper-agent .
docker-compose up -d

# Run once
docker-compose run newspaper-agent run

# Start scheduler
docker-compose run -d newspaper-agent schedule
```

---

## 9.2 Google Cloud Run Deployment

```bash
# Deploy to Cloud Run
gcloud builds submit --tag gcr.io/PROJECT_ID/newspaper-agent

gcloud run deploy newspaper-agent \
    --image gcr.io/PROJECT_ID/newspaper-agent \
    --platform managed \
    --region asia-south1 \
    --memory 1Gi \
    --timeout 600 \
    --max-instances 1 \
    --set-env-vars "AGENT_DRIVE_FOLDER_ID=xxx,AGENT_LLM_PROVIDER=openai" \
    --set-secrets "AGENT_LLM_API_KEY=llm-api-key:latest"
```

### Cloud Scheduler (Daily Trigger)

```bash
# Create Cloud Scheduler job
gcloud scheduler jobs create http daily-newspaper-agent \
    --location=asia-south1 \
    --schedule="0 8 * * *" \
    --time-zone="Asia/Kolkata" \
    --uri="https://newspaper-agent-xxx.run.app/run" \
    --http-method=POST \
    --oidc-service-account-email=scheduler@PROJECT_ID.iam.gserviceaccount.com
```

### HTTP Trigger Endpoint (for Cloud Run)

```python
# newspaper_agent/http_handler.py

from flask import Flask, jsonify, request
from newspaper_agent.agent.orchestrator import run_agent
import os
import logging

app = Flask(__name__)
logger = logging.getLogger(__name__)


@app.route('/run', methods=['POST'])
def trigger_run():
    """HTTP endpoint to trigger agent run."""
    target_date = request.json.get('date') if request.json else None

    try:
        result = run_agent(
            drive_folder_id=os.getenv('AGENT_DRIVE_FOLDER_ID'),
            doc_id=os.getenv('AGENT_OUTPUT_DOC_ID'),
            target_date=target_date,
        )
        return jsonify({
            'status': 'success',
            'files_processed': result.get('files_processed', 0),
            'headlines_found': result.get('headlines_found', 0),
        })
    except Exception as e:
        logger.error(f"Run failed: {e}", exc_info=True)
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'healthy'})


if __name__ == '__main__':
    port = int(os.getenv('PORT', 8080))
    app.run(host='0.0.0.0', port=port)
```

---

## 9.3 Environment Configuration

```bash
# .env.example

# Google Drive
AGENT_DRIVE_FOLDER_ID=your-folder-id-here
AGENT_GOOGLE_CREDENTIALS_PATH=credentials.json

# Google Docs Output
AGENT_OUTPUT_DOC_ID=          # Leave empty to create new doc
AGENT_DOC_TITLE=📰 Daily Headlines

# LLM Configuration
AGENT_LLM_PROVIDER=openai     # openai | anthropic
AGENT_LLM_MODEL=gpt-4o        # gpt-4o | gpt-4o-mini | claude-3-5-sonnet
AGENT_LLM_API_KEY=sk-your-key-here

# OCR
AGENT_OCR_ENGINE=vision_api   # vision_api | tesseract

# Headlines
AGENT_MAX_HEADLINES=50
AGENT_DEDUP_THRESHOLD=0.85

# Scheduling
AGENT_RUN_SCHEDULE=08:00
AGENT_TIMEZONE=Asia/Kolkata

# Monitoring
AGENT_LOG_LEVEL=INFO
AGENT_LANGSMITH_API_KEY=       # Optional
```

---

## 9.4 Monitoring & Alerting

```python
# newspaper_agent/utils/logger.py

import logging
import json
from datetime import datetime


class StructuredFormatter(logging.Formatter):
    """JSON structured logging for production."""

    def format(self, record):
        log_entry = {
            'timestamp': datetime.utcnow().isoformat(),
            'level': record.levelname,
            'module': record.module,
            'message': record.getMessage(),
        }

        if record.exc_info:
            log_entry['exception'] = self.formatException(record.exc_info)

        # Add custom fields
        if hasattr(record, 'metrics'):
            log_entry['metrics'] = record.metrics

        return json.dumps(log_entry)


def setup_logging(level: str = "INFO"):
    """Configure structured logging."""
    handler = logging.StreamHandler()
    handler.setFormatter(StructuredFormatter())

    root = logging.getLogger()
    root.setLevel(level)
    root.addHandler(handler)

    # Reduce noise from libraries
    logging.getLogger('googleapiclient').setLevel(logging.WARNING)
    logging.getLogger('urllib3').setLevel(logging.WARNING)
```

### Metrics Tracking

```python
# newspaper_agent/utils/metrics.py

from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict
import json


@dataclass
class RunMetrics:
    """Track metrics for each agent run."""
    start_time: datetime = field(default_factory=datetime.now)
    end_time: datetime = None
    files_found: int = 0
    files_downloaded: int = 0
    pages_ocr_processed: int = 0
    candidates_detected: int = 0
    candidates_after_dedup: int = 0
    final_headlines: int = 0
    llm_tokens_used: int = 0
    estimated_cost_usd: float = 0.0
    errors: list = field(default_factory=list)

    def complete(self):
        self.end_time = datetime.now()

    @property
    def duration_seconds(self) -> float:
        if self.end_time:
            return (self.end_time - self.start_time).total_seconds()
        return 0

    def to_dict(self) -> Dict:
        return {
            'duration_seconds': self.duration_seconds,
            'files_found': self.files_found,
            'files_downloaded': self.files_downloaded,
            'pages_processed': self.pages_ocr_processed,
            'candidates': self.candidates_detected,
            'after_dedup': self.candidates_after_dedup,
            'final_count': self.final_headlines,
            'tokens_used': self.llm_tokens_used,
            'cost_usd': self.estimated_cost_usd,
            'errors': len(self.errors),
        }

    def log_summary(self):
        import logging
        logger = logging.getLogger('metrics')
        logger.info(f"Run metrics: {json.dumps(self.to_dict())}")
```

---

## 9.5 Error Recovery

```python
# newspaper_agent/utils/retry.py

import time
import logging
from functools import wraps
from typing import Type, Tuple

logger = logging.getLogger(__name__)


def retry_with_backoff(
    max_retries: int = 3,
    base_delay: float = 1.0,
    max_delay: float = 60.0,
    exceptions: Tuple[Type[Exception], ...] = (Exception,),
):
    """Decorator for retry with exponential backoff."""

    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(max_retries + 1):
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    if attempt == max_retries:
                        logger.error(
                            f"{func.__name__} failed after {max_retries} retries: {e}"
                        )
                        raise

                    delay = min(base_delay * (2 ** attempt), max_delay)
                    logger.warning(
                        f"{func.__name__} failed (attempt {attempt+1}/{max_retries}): {e}. "
                        f"Retrying in {delay:.1f}s..."
                    )
                    time.sleep(delay)

        return wrapper
    return decorator
```

---

## 9.6 Health Checks

```python
# newspaper_agent/utils/health.py

import os
from datetime import datetime, timedelta
from newspaper_agent.drive.state_tracker import StateTracker


def check_health() -> dict:
    """Comprehensive health check."""
    checks = {}

    # Check last successful run
    tracker = StateTracker()
    last_run = tracker.get_last_run_time()
    if last_run:
        hours_since = (datetime.now() - last_run).total_seconds() / 3600
        checks['last_run'] = {
            'time': last_run.isoformat(),
            'hours_ago': round(hours_since, 1),
            'healthy': hours_since < 26,  # Should run daily
        }

    # Check credentials
    cred_path = os.getenv('AGENT_GOOGLE_CREDENTIALS_PATH', 'credentials.json')
    checks['credentials'] = {
        'exists': os.path.exists(cred_path),
        'healthy': os.path.exists(cred_path),
    }

    # Check disk space
    import shutil
    total, used, free = shutil.disk_usage('/')
    checks['disk'] = {
        'free_gb': round(free / (1024**3), 2),
        'healthy': free > 1024**3,  # At least 1GB free
    }

    overall = all(c.get('healthy', False) for c in checks.values())
    return {'healthy': overall, 'checks': checks}
```

---

## 9.7 Cost-Optimized Deployment Options

| Option | Monthly Cost | Best For |
|--------|-------------|----------|
| Local Mac (cron) | ~$3 (API only) | Personal use |
| Cloud Run + Scheduler | ~$5 | Reliable automation |
| EC2 t4g.nano + cron | ~$4 | AWS users |
| Raspberry Pi | ~$3 (API only) | Always-on home setup |
| GitHub Actions (scheduled) | Free | Simplest deployment |

### GitHub Actions (Free Tier)

```yaml
# .github/workflows/daily-headlines.yml

name: Daily Headlines Agent
on:
  schedule:
    - cron: '30 2 * * *'  # 8 AM IST (UTC+5:30)
  workflow_dispatch:  # Manual trigger

jobs:
  run-agent:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Install deps
        run: |
          sudo apt-get install -y tesseract-ocr
          pip install -r requirements.txt

      - name: Run agent
        env:
          AGENT_DRIVE_FOLDER_ID: ${{ secrets.DRIVE_FOLDER_ID }}
          AGENT_LLM_API_KEY: ${{ secrets.LLM_API_KEY }}
          AGENT_OUTPUT_DOC_ID: ${{ secrets.DOC_ID }}
          GOOGLE_APPLICATION_CREDENTIALS: credentials.json
        run: |
          echo '${{ secrets.GOOGLE_CREDENTIALS }}' > credentials.json
          python -m newspaper_agent.main run
```

---

## 9.8 Production Checklist

- [ ] All secrets in Secret Manager (not env vars)
- [ ] Structured logging enabled
- [ ] Health check endpoint exposed
- [ ] Retry logic on all external calls
- [ ] Circuit breaker for API failures
- [ ] Alerting on missed daily runs
- [ ] Document size monitoring
- [ ] Cost monitoring (LLM usage)
- [ ] Backup of SQLite state DB
- [ ] Error notification (email/Slack)

---

## ⏭️ Next Module

Proceed to **[Module 10: Complete Implementation](10_Complete_Implementation.md)** for the full requirements.txt and final integration guide.
