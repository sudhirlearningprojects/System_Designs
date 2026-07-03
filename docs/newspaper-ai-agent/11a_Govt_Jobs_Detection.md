# Module 11: Government Jobs Extraction (Part 1 - Detection)

## 🎯 What This Adds

A dedicated section in the Google Doc for each date that captures all **permanent government job notifications** found in newspapers:
- SSC, UPSC, State PSC recruitment notices
- Railway, Banking, Defence job openings
- Teaching, Police, Medical government posts
- Application deadlines, eligibility, vacancies count

---

## 11.1 Document Output Format

The Google Doc will now have TWO sections per date:

```
═══════════════════════════════════════
📅 2025-01-15 (Wednesday)
═══════════════════════════════════════

📰 TOP HEADLINES (50)
━━━━━━━━━━━━━━━━━━━━
1. [POLITICS] India GDP grows 7.2%...
2. ...

🏛️ GOVERNMENT JOBS
━━━━━━━━━━━━━━━━━━━━
1. UPSC Civil Services 2025 - 1000+ Vacancies
   Applications open for IAS/IPS/IFS. Eligibility: Graduate,
   21-32 years. Last date: 15 Feb 2025. Apply at upsc.gov.in

2. SSC CGL 2025 Notification Released - 8000 Posts
   Staff Selection Commission announced Combined Graduate Level
   exam. Posts across ministries. Last date: 28 Feb 2025.

3. Indian Railways RRB NTPC - 35,000 Vacancies
   Railway Recruitment Board invites applications for
   non-technical popular categories. 12th pass eligible.
   Last date: 10 Mar 2025.

4. [No government job updates found today]
━━━━━━━━━━━━━━━━━━━━
```

---

## 11.2 Government Job Detector

```python
# newspaper_agent/nlp/govt_job_detector.py

from typing import List, Dict
import re
import logging

logger = logging.getLogger(__name__)

# Keywords that indicate government job advertisements
GOVT_JOB_KEYWORDS = [
    # Recruiting bodies
    'upsc', 'ssc', 'ibps', 'rrb', 'rpsc', 'uppsc', 'mppsc', 'bpsc',
    'ukpsc', 'hppsc', 'jpsc', 'appsc', 'tspsc', 'kpsc', 'mpsc',
    'staff selection', 'public service commission', 'railway recruitment',
    'banking recruitment', 'defence recruitment',

    # Job types
    'vacancy', 'vacancies', 'recruitment', 'notification',
    'bharti', 'naukri', 'government job', 'govt job', 'sarkari',
    'permanent post', 'regular post', 'group a', 'group b',
    'group c', 'group d',

    # Specific exams/posts
    'civil services', 'ias', 'ips', 'ifs', 'cgl', 'chsl', 'mts',
    'constable', 'sub inspector', 'assistant professor',
    'clerk', 'probationary officer', 'specialist officer',
    'junior engineer', 'assistant engineer', 'scientist',
    'ntpc', 'je', 'sse', 'alp', 'technician',

    # Application terms
    'last date', 'apply online', 'application form',
    'eligibility', 'age limit', 'educational qualification',
    'admit card', 'exam date',
]

# Patterns that indicate it's NOT a govt job (filter noise)
EXCLUDE_PATTERNS = [
    r'private\s+(company|sector|firm|job)',
    r'(fresher|intern|trainee)\s+opening',
    r'(startup|mnc|it company)',
    r'walk[\s-]?in\s+interview',  # Usually private
    r'contractual\s+(basis|post)',  # Not permanent
    r'temporary\s+(post|position)',
    r'ad\s*hoc',
    r'outsourc',
]


class GovtJobDetector:
    """
    Detect government job notifications from newspaper text.
    Focuses on PERMANENT government positions only.
    """

    def __init__(self, min_keyword_matches: int = 2):
        self.min_matches = min_keyword_matches
        self._exclude_patterns = [
            re.compile(p, re.IGNORECASE) for p in EXCLUDE_PATTERNS
        ]

    def detect_govt_jobs(self, pages: List[Dict]) -> List[Dict]:
        """
        Scan all extracted pages for government job notifications.

        Args:
            pages: OCR output pages with 'blocks' and 'full_text'

        Returns:
            List of detected govt job entries
        """
        candidates = []

        for page in pages:
            # Strategy 1: Check individual blocks
            for block in page.get('blocks', []):
                text = block.get('text', '')
                if self._is_govt_job(text):
                    candidates.append({
                        'text': text,
                        'source_page': page.get('page_number', 0),
                        'source_file': page.get('source_file', ''),
                        'confidence': self._calculate_confidence(text),
                        'context': self._get_surrounding_text(
                            block, page.get('blocks', [])
                        ),
                    })

            # Strategy 2: Check full page text for job sections
            full_text = page.get('full_text', '')
            job_sections = self._extract_job_sections(full_text)
            for section in job_sections:
                if not self._is_duplicate(section, candidates):
                    candidates.append({
                        'text': section,
                        'source_page': page.get('page_number', 0),
                        'source_file': page.get('source_file', ''),
                        'confidence': self._calculate_confidence(section),
                        'context': '',
                    })

        logger.info(f"Found {len(candidates)} govt job candidates")
        return candidates

    def _is_govt_job(self, text: str) -> bool:
        """Check if a text block is about a government job."""
        if not text or len(text) < 30:
            return False

        text_lower = text.lower()

        # Check exclusions first
        for pattern in self._exclude_patterns:
            if pattern.search(text_lower):
                return False

        # Count keyword matches
        matches = sum(
            1 for kw in GOVT_JOB_KEYWORDS
            if kw in text_lower
        )

        return matches >= self.min_matches

    def _calculate_confidence(self, text: str) -> float:
        """Score how likely this is a real govt job notification."""
        text_lower = text.lower()
        score = 0.0

        # Strong indicators
        strong = ['vacancy', 'vacancies', 'recruitment', 'notification',
                  'last date', 'apply online', 'upsc', 'ssc', 'rrb', 'ibps']
        for kw in strong:
            if kw in text_lower:
                score += 0.15

        # Has numbers (vacancy count, age limit, salary)
        if re.search(r'\d+\s*(post|vacanc|seat)', text_lower):
            score += 0.2

        # Has a date (last date to apply)
        if re.search(r'(last\s*date|deadline).*\d{1,2}', text_lower):
            score += 0.15

        # Has official website
        if re.search(r'\.(gov|nic)\.in', text_lower):
            score += 0.1

        return min(1.0, score)

    def _extract_job_sections(self, full_text: str) -> List[str]:
        """
        Extract job notification sections from full page text.
        Newspapers often have a dedicated "Employment" page.
        """
        sections = []

        # Split by common separators
        # Look for blocks that start with org names
        org_patterns = [
            r'(UPSC|SSC|IBPS|RRB|[A-Z]+PSC)[\s\S]{50,500}',
            r'(Recruitment|Vacancy|Notification)[\s\S]{50,400}',
        ]

        for pattern in org_patterns:
            matches = re.findall(pattern, full_text)
            for match in matches:
                if isinstance(match, tuple):
                    match = match[0]
                if len(match) > 50 and self._is_govt_job(match):
                    sections.append(match[:500])  # Cap length

        return sections

    def _get_surrounding_text(
        self, block: Dict, all_blocks: List[Dict]
    ) -> str:
        """Get text around a job notification block for context."""
        block_y = block.get('position_y', 0)
        context_parts = []

        for b in all_blocks:
            b_y = b.get('position_y', 0)
            if abs(b_y - block_y) < 200 and b.get('text') != block.get('text'):
                context_parts.append(b.get('text', ''))

        return ' '.join(context_parts)[:500]

    def _is_duplicate(
        self, text: str, existing: List[Dict]
    ) -> bool:
        """Check if this job is already captured."""
        text_lower = text.lower()[:100]
        for item in existing:
            if text_lower in item['text'].lower()[:100]:
                return True
        return False
```

---

## 11.3 Government Job Enricher (LLM)

```python
# newspaper_agent/summarizer/govt_job_enricher.py

from typing import List, Dict
from newspaper_agent.summarizer.llm_client import LLMClient
import json
import logging

logger = logging.getLogger(__name__)

GOVT_JOB_SYSTEM_PROMPT = """You are an expert at extracting structured government 
job information from Indian newspaper text. Extract ONLY permanent government jobs.

Ignore: contractual, temporary, ad-hoc, private sector, internship positions.

Return valid JSON only."""

GOVT_JOB_EXTRACT_PROMPT = """From the following newspaper text blocks, extract all 
PERMANENT government job notifications.

For each job, provide:
- organization: Recruiting body (e.g., UPSC, SSC, Railways, State PSC)
- post_name: Name of the post/exam
- vacancies: Number of vacancies (if mentioned)
- eligibility: Key eligibility (education, age)
- last_date: Last date to apply (if mentioned)
- website: Official website (if mentioned)
- summary: 2-3 line summary of the notification

Text blocks:
{job_texts}

Return JSON array (empty [] if no valid permanent govt jobs found):
[
  {{
    "organization": "UPSC",
    "post_name": "Civil Services 2025",
    "vacancies": "1000+",
    "eligibility": "Graduate, 21-32 years",
    "last_date": "15 Feb 2025",
    "website": "upsc.gov.in",
    "summary": "2-3 line description"
  }}
]

Return ONLY the JSON array."""


class GovtJobEnricher:
    """Use LLM to structure and enrich government job data."""

    def __init__(self, llm_client: LLMClient):
        self.llm = llm_client

    def enrich_jobs(self, raw_jobs: List[Dict]) -> List[Dict]:
        """
        Process raw job detections through LLM for structured output.
        """
        if not raw_jobs:
            return []

        # Combine all job texts for a single LLM call
        job_texts = ""
        for i, job in enumerate(raw_jobs, 1):
            job_texts += f"\n--- Block {i} ---\n"
            job_texts += job['text'][:400]
            if job.get('context'):
                job_texts += f"\nContext: {job['context'][:200]}"
            job_texts += "\n"

        response = self.llm.complete(
            system_prompt=GOVT_JOB_SYSTEM_PROMPT,
            user_prompt=GOVT_JOB_EXTRACT_PROMPT.format(job_texts=job_texts),
            max_tokens=3000
        )

        structured = self._parse_response(response)
        logger.info(f"Extracted {len(structured)} structured govt job entries")
        return structured

    def _parse_response(self, response: str) -> List[Dict]:
        """Parse LLM response into structured jobs."""
        text = response.strip()
        if text.startswith("```"):
            text = text.split("\n", 1)[1]
            text = text.rsplit("```", 1)[0]

        try:
            data = json.loads(text)
            if isinstance(data, list):
                return data
        except json.JSONDecodeError:
            import re
            match = re.search(r'\[[\s\S]*\]', text)
            if match:
                try:
                    return json.loads(match.group())
                except json.JSONDecodeError:
                    pass

        logger.warning("Failed to parse govt job LLM response")
        return []

    def format_for_doc(self, jobs: List[Dict]) -> str:
        """Format structured jobs for Google Doc section."""
        if not jobs:
            return "   [No government job updates found today]\n"

        lines = []
        for i, job in enumerate(jobs, 1):
            org = job.get('organization', 'Unknown')
            post = job.get('post_name', 'Unknown Post')
            vacancies = job.get('vacancies', 'N/A')
            summary = job.get('summary', '')
            last_date = job.get('last_date', '')
            website = job.get('website', '')

            # Title line
            title = f"{i}. {org} - {post}"
            if vacancies != 'N/A':
                title += f" ({vacancies} Vacancies)"
            lines.append(title)

            # Summary
            if summary:
                lines.append(f"   {summary}")

            # Key details
            details = []
            if last_date:
                details.append(f"Last Date: {last_date}")
            if website:
                details.append(f"Apply: {website}")
            if details:
                lines.append(f"   📌 {' | '.join(details)}")

            lines.append("")  # Blank line between entries

        return "\n".join(lines)
```

---

## ⏭️ Continue to [Part 2](11b_Govt_Jobs_Integration.md) for integration with the main agent pipeline.
