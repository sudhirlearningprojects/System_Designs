# Module 2: Google Drive Integration

## 🎯 Learning Objectives

- Set up Google Cloud Project with Drive API
- Implement OAuth2 authentication for the agent
- Build a folder monitoring system (detect new files)
- Download newspaper files (PDF/images) programmatically
- Handle rate limits and API quotas

---

## 2.1 Google Cloud Project Setup

### Step 1: Create Project

```bash
# Using gcloud CLI (or do via console.cloud.google.com)
gcloud projects create newspaper-agent-project --name="Newspaper AI Agent"
gcloud config set project newspaper-agent-project
```

### Step 2: Enable Required APIs

```bash
# Enable all needed APIs
gcloud services enable drive.googleapis.com
gcloud services enable docs.googleapis.com
gcloud services enable vision.googleapis.com
```

### Step 3: Create Service Account (Recommended for Automation)

```bash
# Create service account
gcloud iam service-accounts create newspaper-agent \
    --display-name="Newspaper Agent Service Account"

# Download key
gcloud iam service-accounts keys create credentials.json \
    --iam-account=newspaper-agent@newspaper-agent-project.iam.gserviceaccount.com
```

### Step 4: Share Drive Folder with Service Account

1. Go to Google Drive
2. Create a folder called "Daily Newspapers"
3. Right-click → Share
4. Add the service account email: `newspaper-agent@newspaper-agent-project.iam.gserviceaccount.com`
5. Give "Viewer" access (read-only is sufficient for input folder)

### Alternative: OAuth2 for Personal Account

If you want to use your personal Google account instead of a service account:

```bash
# Create OAuth2 credentials
# Go to console.cloud.google.com → APIs & Credentials → Create Credentials
# Choose "OAuth 2.0 Client ID" → Desktop Application
# Download as client_secrets.json
```

---

## 2.2 Authentication Implementation

### Service Account Authentication (Preferred)

```python
# newspaper_agent/drive/auth.py

from google.oauth2 import service_account
from googleapiclient.discovery import build
from typing import Optional
import os

# Scopes define what the agent can access
SCOPES = [
    'https://www.googleapis.com/auth/drive.readonly',      # Read Drive files
    'https://www.googleapis.com/auth/documents',           # Read/write Docs
]


class GoogleAuthManager:
    """Manages Google API authentication for the agent."""
    
    def __init__(self, credentials_path: str = "credentials.json"):
        self.credentials_path = credentials_path
        self._credentials = None
        self._drive_service = None
        self._docs_service = None
    
    def _get_credentials(self):
        """Load service account credentials."""
        if self._credentials is None:
            if not os.path.exists(self.credentials_path):
                raise FileNotFoundError(
                    f"Credentials file not found: {self.credentials_path}\n"
                    "Run 'python setup_credentials.py' to configure."
                )
            
            self._credentials = service_account.Credentials.from_service_account_file(
                self.credentials_path,
                scopes=SCOPES
            )
        return self._credentials
    
    def get_drive_service(self):
        """Get authenticated Google Drive service."""
        if self._drive_service is None:
            credentials = self._get_credentials()
            self._drive_service = build('drive', 'v3', credentials=credentials)
        return self._drive_service
    
    def get_docs_service(self):
        """Get authenticated Google Docs service."""
        if self._docs_service is None:
            credentials = self._get_credentials()
            self._docs_service = build('docs', 'v1', credentials=credentials)
        return self._docs_service
    
    def verify_access(self, folder_id: str) -> bool:
        """Verify the agent can access the target folder."""
        try:
            service = self.get_drive_service()
            result = service.files().get(
                fileId=folder_id,
                fields='id, name, mimeType'
            ).execute()
            print(f"✅ Access verified: Folder '{result['name']}'")
            return True
        except Exception as e:
            print(f"❌ Access failed: {e}")
            return False
```

### OAuth2 for Personal Account (Interactive)

```python
# newspaper_agent/drive/auth_oauth.py

from google_auth_oauthlib.flow import InstalledAppFlow
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
import os
import pickle

SCOPES = [
    'https://www.googleapis.com/auth/drive.readonly',
    'https://www.googleapis.com/auth/documents',
]

TOKEN_PATH = 'token.pickle'


class OAuthManager:
    """OAuth2 flow for personal Google accounts."""
    
    def __init__(self, client_secrets_path: str = "client_secrets.json"):
        self.client_secrets_path = client_secrets_path
    
    def get_credentials(self) -> Credentials:
        """Get or refresh OAuth2 credentials with interactive login."""
        creds = None
        
        # Load saved token
        if os.path.exists(TOKEN_PATH):
            with open(TOKEN_PATH, 'rb') as token:
                creds = pickle.load(token)
        
        # If no valid credentials, do OAuth flow
        if not creds or not creds.valid:
            if creds and creds.expired and creds.refresh_token:
                creds.refresh(Request())
            else:
                flow = InstalledAppFlow.from_client_secrets_file(
                    self.client_secrets_path, SCOPES
                )
                creds = flow.run_local_server(port=0)
            
            # Save for next run
            with open(TOKEN_PATH, 'wb') as token:
                pickle.dump(creds, token)
        
        return creds
```

---

## 2.3 Drive Folder Monitor

The monitor detects new newspaper files that haven't been processed yet.

```python
# newspaper_agent/drive/monitor.py

from googleapiclient.discovery import Resource
from datetime import datetime, timedelta
from typing import List, Optional
import logging

logger = logging.getLogger(__name__)


class DriveMonitor:
    """Monitors a Google Drive folder for new newspaper uploads."""
    
    # Supported file types for newspapers
    SUPPORTED_MIME_TYPES = [
        'application/pdf',
        'image/jpeg',
        'image/png',
        'image/tiff',
        'image/webp',
    ]
    
    def __init__(self, drive_service: Resource, folder_id: str):
        self.service = drive_service
        self.folder_id = folder_id
    
    def list_files(
        self,
        since: Optional[datetime] = None,
        mime_types: Optional[List[str]] = None
    ) -> List[dict]:
        """
        List files in the monitored folder.
        
        Args:
            since: Only return files modified after this time
            mime_types: Filter by MIME types (default: all supported)
        
        Returns:
            List of file metadata dicts
        """
        mime_types = mime_types or self.SUPPORTED_MIME_TYPES
        
        # Build query
        query_parts = [
            f"'{self.folder_id}' in parents",
            "trashed = false",
        ]
        
        # Add time filter
        if since:
            # Google Drive API uses RFC 3339 format
            time_str = since.strftime('%Y-%m-%dT%H:%M:%S')
            query_parts.append(f"modifiedTime > '{time_str}'")
        
        # Add MIME type filter
        mime_filter = " or ".join(
            f"mimeType = '{mt}'" for mt in mime_types
        )
        query_parts.append(f"({mime_filter})")
        
        query = " and ".join(query_parts)
        
        logger.info(f"Querying Drive with: {query}")
        
        # Execute query with pagination
        files = []
        page_token = None
        
        while True:
            response = self.service.files().list(
                q=query,
                spaces='drive',
                fields='nextPageToken, files(id, name, mimeType, modifiedTime, size)',
                pageToken=page_token,
                orderBy='modifiedTime desc',
                pageSize=100
            ).execute()
            
            files.extend(response.get('files', []))
            page_token = response.get('nextPageToken')
            
            if not page_token:
                break
        
        logger.info(f"Found {len(files)} files in Drive folder")
        return files
    
    def get_new_files(self, processed_ids: List[str]) -> List[dict]:
        """
        Get files that haven't been processed yet.
        
        Args:
            processed_ids: List of file IDs already processed
        
        Returns:
            List of unprocessed file metadata
        """
        all_files = self.list_files()
        new_files = [f for f in all_files if f['id'] not in processed_ids]
        
        logger.info(
            f"Total files: {len(all_files)}, "
            f"Already processed: {len(processed_ids)}, "
            f"New files: {len(new_files)}"
        )
        
        return new_files
    
    def get_todays_files(self) -> List[dict]:
        """Get files uploaded/modified today."""
        today_start = datetime.now().replace(
            hour=0, minute=0, second=0, microsecond=0
        )
        return self.list_files(since=today_start)
    
    def get_files_since_last_run(self, last_run_time: datetime) -> List[dict]:
        """Get files uploaded since the last agent run."""
        return self.list_files(since=last_run_time)
```

---

## 2.4 File Downloader

```python
# newspaper_agent/drive/downloader.py

from googleapiclient.discovery import Resource
from googleapiclient.http import MediaIoBaseDownload
from pathlib import Path
from typing import Optional
import io
import os
import logging
import tempfile

logger = logging.getLogger(__name__)


class FileDownloader:
    """Downloads newspaper files from Google Drive."""
    
    def __init__(
        self,
        drive_service: Resource,
        download_dir: Optional[str] = None
    ):
        self.service = drive_service
        self.download_dir = download_dir or tempfile.mkdtemp(prefix="newspaper_")
        Path(self.download_dir).mkdir(parents=True, exist_ok=True)
    
    def download_file(self, file_id: str, file_name: str) -> str:
        """
        Download a single file from Drive.
        
        Args:
            file_id: Google Drive file ID
            file_name: Name to save the file as
        
        Returns:
            Local path to downloaded file
        """
        local_path = os.path.join(self.download_dir, file_name)
        
        # Skip if already downloaded
        if os.path.exists(local_path):
            logger.info(f"File already exists: {local_path}")
            return local_path
        
        logger.info(f"Downloading: {file_name} ({file_id})")
        
        request = self.service.files().get_media(fileId=file_id)
        
        with io.FileIO(local_path, 'wb') as fh:
            downloader = MediaIoBaseDownload(fh, request)
            done = False
            while not done:
                status, done = downloader.next_chunk()
                if status:
                    logger.debug(
                        f"Download progress: {int(status.progress() * 100)}%"
                    )
        
        file_size = os.path.getsize(local_path)
        logger.info(
            f"Downloaded: {file_name} ({file_size / 1024:.1f} KB)"
        )
        
        return local_path
    
    def download_batch(self, files: list) -> list:
        """
        Download multiple files.
        
        Args:
            files: List of dicts with 'id' and 'name' keys
        
        Returns:
            List of dicts with added 'local_path' key
        """
        results = []
        
        for file_info in files:
            try:
                local_path = self.download_file(
                    file_info['id'],
                    file_info['name']
                )
                results.append({
                    **file_info,
                    'local_path': local_path,
                    'download_success': True
                })
            except Exception as e:
                logger.error(f"Failed to download {file_info['name']}: {e}")
                results.append({
                    **file_info,
                    'local_path': None,
                    'download_success': False,
                    'error': str(e)
                })
        
        successful = sum(1 for r in results if r['download_success'])
        logger.info(f"Downloaded {successful}/{len(files)} files successfully")
        
        return results
    
    def cleanup(self):
        """Remove all downloaded temp files."""
        import shutil
        if os.path.exists(self.download_dir):
            shutil.rmtree(self.download_dir)
            logger.info(f"Cleaned up: {self.download_dir}")
    
    def export_google_doc_as_pdf(self, file_id: str, file_name: str) -> str:
        """
        Export a Google Doc/Slides as PDF (in case newspapers are in Doc format).
        """
        local_path = os.path.join(self.download_dir, f"{file_name}.pdf")
        
        request = self.service.files().export_media(
            fileId=file_id,
            mimeType='application/pdf'
        )
        
        with io.FileIO(local_path, 'wb') as fh:
            downloader = MediaIoBaseDownload(fh, request)
            done = False
            while not done:
                status, done = downloader.next_chunk()
        
        return local_path
```

---

## 2.5 State Tracking (SQLite)

Track which files have been processed to avoid reprocessing.

```python
# newspaper_agent/drive/state_tracker.py

import sqlite3
from datetime import datetime
from typing import List, Optional
from pathlib import Path


class StateTracker:
    """Tracks processed files and agent run history."""
    
    def __init__(self, db_path: str = "agent_state.db"):
        self.db_path = db_path
        self._init_db()
    
    def _init_db(self):
        """Create tables if they don't exist."""
        with sqlite3.connect(self.db_path) as conn:
            conn.executescript("""
                CREATE TABLE IF NOT EXISTS processed_files (
                    file_id TEXT PRIMARY KEY,
                    file_name TEXT NOT NULL,
                    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    headlines_extracted INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'success'
                );
                
                CREATE TABLE IF NOT EXISTS agent_runs (
                    run_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP,
                    files_processed INTEGER DEFAULT 0,
                    headlines_found INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'running',
                    error_message TEXT
                );
                
                CREATE TABLE IF NOT EXISTS headlines_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    headline TEXT NOT NULL,
                    summary TEXT,
                    category TEXT,
                    rank INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                
                CREATE INDEX IF NOT EXISTS idx_headlines_date 
                ON headlines_history(date);
                
                CREATE INDEX IF NOT EXISTS idx_headlines_text 
                ON headlines_history(headline);
            """)
    
    def get_processed_file_ids(self) -> List[str]:
        """Get all file IDs that have been processed."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute(
                "SELECT file_id FROM processed_files WHERE status = 'success'"
            )
            return [row[0] for row in cursor.fetchall()]
    
    def mark_file_processed(
        self,
        file_id: str,
        file_name: str,
        headlines_count: int,
        status: str = "success"
    ):
        """Mark a file as processed."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """INSERT OR REPLACE INTO processed_files 
                   (file_id, file_name, headlines_extracted, status)
                   VALUES (?, ?, ?, ?)""",
                (file_id, file_name, headlines_count, status)
            )
    
    def start_run(self) -> int:
        """Record start of an agent run."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute(
                "INSERT INTO agent_runs (status) VALUES ('running')"
            )
            return cursor.lastrowid
    
    def complete_run(
        self,
        run_id: int,
        files_processed: int,
        headlines_found: int,
        status: str = "success",
        error: Optional[str] = None
    ):
        """Record completion of an agent run."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """UPDATE agent_runs 
                   SET completed_at = CURRENT_TIMESTAMP,
                       files_processed = ?,
                       headlines_found = ?,
                       status = ?,
                       error_message = ?
                   WHERE run_id = ?""",
                (files_processed, headlines_found, status, error, run_id)
            )
    
    def get_last_run_time(self) -> Optional[datetime]:
        """Get the timestamp of the last successful run."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute(
                """SELECT completed_at FROM agent_runs 
                   WHERE status = 'success' 
                   ORDER BY completed_at DESC LIMIT 1"""
            )
            row = cursor.fetchone()
            if row and row[0]:
                return datetime.fromisoformat(row[0])
            return None
    
    def save_headlines(self, date_str: str, headlines: List[dict]):
        """Save extracted headlines for searchability."""
        with sqlite3.connect(self.db_path) as conn:
            for h in headlines:
                conn.execute(
                    """INSERT INTO headlines_history 
                       (date, headline, summary, category, rank)
                       VALUES (?, ?, ?, ?, ?)""",
                    (date_str, h['headline'], h.get('summary'),
                     h.get('category'), h.get('rank'))
                )
    
    def search_headlines(self, query: str, limit: int = 20) -> List[dict]:
        """Search headlines by keyword."""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                """SELECT date, headline, summary, category, rank
                   FROM headlines_history
                   WHERE headline LIKE ? OR summary LIKE ?
                   ORDER BY date DESC
                   LIMIT ?""",
                (f"%{query}%", f"%{query}%", limit)
            )
            return [dict(row) for row in cursor.fetchall()]
```

---

## 2.6 Putting It Together: Drive Service

```python
# newspaper_agent/drive/__init__.py

from .auth import GoogleAuthManager
from .monitor import DriveMonitor
from .downloader import FileDownloader
from .state_tracker import StateTracker


class DriveService:
    """
    High-level service combining auth, monitoring, and downloading.
    
    Usage:
        service = DriveService(folder_id="your-folder-id")
        new_files = service.check_for_new_files()
        downloaded = service.download_files(new_files)
    """
    
    def __init__(
        self,
        folder_id: str,
        credentials_path: str = "credentials.json",
        db_path: str = "agent_state.db"
    ):
        self.auth = GoogleAuthManager(credentials_path)
        self.monitor = DriveMonitor(
            self.auth.get_drive_service(),
            folder_id
        )
        self.downloader = FileDownloader(self.auth.get_drive_service())
        self.state = StateTracker(db_path)
    
    def check_for_new_files(self) -> list:
        """Check Drive for unprocessed newspaper files."""
        processed_ids = self.state.get_processed_file_ids()
        return self.monitor.get_new_files(processed_ids)
    
    def download_files(self, files: list) -> list:
        """Download a list of files from Drive."""
        return self.downloader.download_batch(files)
    
    def mark_processed(self, file_id: str, file_name: str, headlines: int):
        """Mark a file as successfully processed."""
        self.state.mark_file_processed(file_id, file_name, headlines)
    
    def cleanup(self):
        """Clean up temporary files."""
        self.downloader.cleanup()
    
    def verify_setup(self) -> bool:
        """Verify the agent can access the Drive folder."""
        return self.auth.verify_access(self.monitor.folder_id)
```

---

## 2.7 Testing the Drive Integration

```python
# tests/test_drive.py

import pytest
from unittest.mock import MagicMock, patch
from newspaper_agent.drive.monitor import DriveMonitor
from newspaper_agent.drive.state_tracker import StateTracker


class TestDriveMonitor:
    """Tests for Drive folder monitoring."""
    
    def setup_method(self):
        self.mock_service = MagicMock()
        self.monitor = DriveMonitor(self.mock_service, "test-folder-id")
    
    def test_list_files_returns_pdfs(self):
        """Should return PDF files from the folder."""
        self.mock_service.files().list().execute.return_value = {
            'files': [
                {'id': '1', 'name': 'newspaper_2025_01_15.pdf',
                 'mimeType': 'application/pdf', 'modifiedTime': '2025-01-15T08:00:00Z'},
            ],
            'nextPageToken': None
        }
        
        files = self.monitor.list_files()
        assert len(files) == 1
        assert files[0]['mimeType'] == 'application/pdf'
    
    def test_get_new_files_excludes_processed(self):
        """Should exclude already-processed file IDs."""
        self.mock_service.files().list().execute.return_value = {
            'files': [
                {'id': '1', 'name': 'paper1.pdf'},
                {'id': '2', 'name': 'paper2.pdf'},
                {'id': '3', 'name': 'paper3.pdf'},
            ],
            'nextPageToken': None
        }
        
        new_files = self.monitor.get_new_files(processed_ids=['1', '2'])
        assert len(new_files) == 1
        assert new_files[0]['id'] == '3'


class TestStateTracker:
    """Tests for state persistence."""
    
    def setup_method(self):
        self.tracker = StateTracker(":memory:")
    
    def test_mark_and_retrieve_processed(self):
        """Should track processed files."""
        self.tracker.mark_file_processed("file-1", "paper.pdf", 50)
        ids = self.tracker.get_processed_file_ids()
        assert "file-1" in ids
    
    def test_search_headlines(self):
        """Should find headlines by keyword."""
        self.tracker.save_headlines("2025-01-15", [
            {"headline": "India GDP grows 7.2%", "summary": "...", "rank": 1},
            {"headline": "Supreme Court verdict on bonds", "summary": "...", "rank": 2},
        ])
        
        results = self.tracker.search_headlines("GDP")
        assert len(results) == 1
        assert "GDP" in results[0]['headline']
```

---

## 2.8 Google Drive Push Notifications (Optional: Real-time)

For real-time detection instead of polling:

```python
# newspaper_agent/drive/push_notification.py

from googleapiclient.discovery import Resource
import uuid


class DrivePushNotification:
    """
    Set up Google Drive push notifications (webhook).
    When a file is added to the folder, Google pings our endpoint.
    
    Requirements:
    - Public HTTPS endpoint (use ngrok for local dev)
    - Domain verified with Google
    """
    
    def __init__(self, drive_service: Resource, webhook_url: str):
        self.service = drive_service
        self.webhook_url = webhook_url
    
    def subscribe_to_folder_changes(self, folder_id: str) -> dict:
        """
        Subscribe to changes in a Drive folder.
        Notifications expire after 24h and must be renewed.
        """
        channel_id = str(uuid.uuid4())
        
        body = {
            'id': channel_id,
            'type': 'web_hook',
            'address': self.webhook_url,
            'expiration': None,  # Max 24 hours
        }
        
        response = self.service.files().watch(
            fileId=folder_id,
            body=body
        ).execute()
        
        return {
            'channel_id': channel_id,
            'resource_id': response.get('resourceId'),
            'expiration': response.get('expiration'),
        }
    
    def unsubscribe(self, channel_id: str, resource_id: str):
        """Stop receiving notifications."""
        body = {
            'id': channel_id,
            'resourceId': resource_id,
        }
        self.service.channels().stop(body=body).execute()
```

---

## 2.9 Rate Limiting & Quotas

### Google Drive API Limits

| Quota | Limit | Strategy |
|-------|-------|----------|
| Queries per 100 seconds | 1,000 | Batch requests |
| Queries per day | 1,000,000,000 | Not a concern |
| Upload bandwidth | 750 GB/day | Not applicable (download only) |
| File download | 10 GB/file | Newspaper files are small |

### Rate Limiter Implementation

```python
# newspaper_agent/utils/rate_limiter.py

import time
from functools import wraps


def rate_limit(max_calls: int, period: float):
    """
    Decorator to rate-limit function calls.
    
    Args:
        max_calls: Maximum number of calls allowed
        period: Time period in seconds
    """
    calls = []
    
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            now = time.time()
            # Remove old calls outside the window
            while calls and calls[0] < now - period:
                calls.pop(0)
            
            if len(calls) >= max_calls:
                sleep_time = calls[0] - (now - period)
                time.sleep(sleep_time)
            
            calls.append(time.time())
            return func(*args, **kwargs)
        
        return wrapper
    return decorator
```

---

## 2.10 Checklist Before Moving On

- [ ] Google Cloud project created with Drive API enabled
- [ ] Service account created and key downloaded as `credentials.json`
- [ ] Drive folder created and shared with service account
- [ ] `DriveMonitor` can list files in the folder
- [ ] `FileDownloader` can download PDFs/images
- [ ] `StateTracker` persists processed file IDs
- [ ] Tests pass for monitoring and state tracking
- [ ] Folder ID noted in `.env` file

---

## ⏭️ Next Module

Proceed to **[Module 3: OCR & Text Extraction](03_OCR_Text_Extraction.md)** to extract text from newspaper PDFs and images.
