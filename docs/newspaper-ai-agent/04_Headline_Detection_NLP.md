# Module 4: Headline Detection & NLP

## 🎯 Learning Objectives

- Identify headlines from OCR output using layout heuristics
- Distinguish headlines from body text, ads, captions
- Semantic deduplication using embeddings
- Category classification for diversity scoring

---

## 4.1 Headline Detection Strategy

Headlines in newspapers have distinct characteristics:
- **Larger font size** (1.5-3x body text)
- **Bold or heavy weight fonts**
- **Positioned at top of columns/articles**
- **Short length** (typically 5-15 words)
- **No punctuation mid-sentence** (except commas, colons)
- **Not ALL CAPS ads** or section headers

```
OCR Output with Layout
         │
         ▼
┌────────────────────┐
│ Font-Size Analysis │──── Blocks with font > 1.5x median
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ Length Filter       │──── 3-20 words, 20-150 chars
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ Position Filter    │──── Top of text regions
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ Content Filter     │──── Not ads, not section names
└────────┬───────────┘
         │
         ▼
  Candidate Headlines (100-200)
```

---

## 4.2 Headline Detector Implementation

```python
# newspaper_agent/nlp/headline_detector.py

from typing import List, Dict, Tuple
import re
import statistics
import logging

logger = logging.getLogger(__name__)

# Common section headers and non-headline patterns to exclude
EXCLUDE_PATTERNS = [
    r'^(SPORTS|BUSINESS|WORLD|NATIONAL|LOCAL|OPINION|EDITORIAL|LIFESTYLE)$',
    r'^(CLASSIFIED|ADVERTISEMENT|ADVERTORIAL|SPONSORED)$',
    r'^(WEATHER|HOROSCOPE|CROSSWORD|SUDOKU|COMIC)$',
    r'^(PAGE|VOL|ISSUE|EDITION|CONTINUED|CONTD)[\s\d]',
    r'^\d+$',  # Just numbers
    r'^[A-Z\s]{2,}$',  # ALL CAPS short text (likely section header)
    r'www\.|\.com|\.in|@',  # URLs and emails
    r'^\s*(Rs|₹|\$|€|£)\s*\d',  # Prices
    r'(phone|call|contact|toll.?free).*\d{5,}',  # Phone numbers
]


class HeadlineDetector:
    """
    Detects newspaper headlines from OCR text blocks.
    Uses font size, position, and content heuristics.
    """

    def __init__(
        self,
        font_size_multiplier: float = 1.4,
        min_words: int = 3,
        max_words: int = 20,
        min_chars: int = 15,
        max_chars: int = 150,
    ):
        self.font_size_multiplier = font_size_multiplier
        self.min_words = min_words
        self.max_words = max_words
        self.min_chars = min_chars
        self.max_chars = max_chars
        self._compiled_excludes = [
            re.compile(p, re.IGNORECASE) for p in EXCLUDE_PATTERNS
        ]

    def detect_headlines(self, pages: List[Dict]) -> List[Dict]:
        """
        Detect headlines from all pages of OCR output.

        Args:
            pages: List of page dicts from OCR engine, each with 'blocks'

        Returns:
            List of candidate headline dicts
        """
        all_candidates = []

        for page in pages:
            blocks = page.get('blocks', [])
            if not blocks:
                continue

            # Calculate median font size for this page
            font_sizes = [
                b['font_size'] for b in blocks
                if b.get('font_size', 0) > 0
            ]

            if not font_sizes:
                # No font info available, use text-based heuristics
                candidates = self._detect_by_text_heuristics(
                    blocks, page.get('page_number', 0)
                )
            else:
                median_font = statistics.median(font_sizes)
                candidates = self._detect_by_font_and_position(
                    blocks, median_font, page.get('page_number', 0)
                )

            all_candidates.extend(candidates)

        logger.info(f"Detected {len(all_candidates)} candidate headlines")
        return all_candidates

    def _detect_by_font_and_position(
        self,
        blocks: List[Dict],
        median_font: float,
        page_number: int
    ) -> List[Dict]:
        """Detect headlines using font size as primary signal."""
        candidates = []
        threshold = median_font * self.font_size_multiplier

        for block in blocks:
            text = block.get('text', '').strip()
            font_size = block.get('font_size', 0)
            is_bold = block.get('is_bold', False)

            # Skip if font size is below threshold
            if font_size < threshold and not is_bold:
                continue

            # Apply content filters
            if not self._is_valid_headline(text):
                continue

            # Calculate confidence score
            confidence = self._calculate_confidence(
                text, font_size, median_font, is_bold, block
            )

            # Extract context (next block as potential article body)
            context = self._get_context(block, blocks)

            candidates.append({
                'text': self._clean_headline(text),
                'confidence': confidence,
                'font_size': font_size,
                'is_bold': is_bold,
                'page_number': page_number,
                'position_y': block.get('position_y', 0),
                'context': context,
            })

        return candidates

    def _detect_by_text_heuristics(
        self,
        blocks: List[Dict],
        page_number: int
    ) -> List[Dict]:
        """
        Fallback: detect headlines when font info is unavailable.
        Uses text patterns and position.
        """
        candidates = []

        for i, block in enumerate(blocks):
            text = block.get('text', '').strip()

            if not self._is_valid_headline(text):
                continue

            # Heuristic: shorter text blocks followed by longer ones
            # are likely headlines followed by article body
            is_short = len(text.split()) <= self.max_words
            next_block_longer = (
                i + 1 < len(blocks) and
                len(blocks[i+1].get('text', '')) > len(text) * 2
            )

            # Check if text looks like a headline
            looks_like_headline = (
                is_short and
                (next_block_longer or text[0].isupper()) and
                not text.endswith('.')  # Headlines rarely end with period
            )

            if looks_like_headline:
                candidates.append({
                    'text': self._clean_headline(text),
                    'confidence': 0.5,  # Lower confidence without font info
                    'font_size': 0,
                    'is_bold': False,
                    'page_number': page_number,
                    'position_y': block.get('bounding_box', {}).get(
                        'top_left', {}
                    ).get('y', 0),
                    'context': blocks[i+1].get('text', '')[:500] if i+1 < len(blocks) else '',
                })

        return candidates

    def _is_valid_headline(self, text: str) -> bool:
        """Check if text passes basic headline validity filters."""
        # Length checks
        word_count = len(text.split())
        if word_count < self.min_words or word_count > self.max_words:
            return False
        if len(text) < self.min_chars or len(text) > self.max_chars:
            return False

        # Exclude patterns (ads, section headers, etc.)
        for pattern in self._compiled_excludes:
            if pattern.search(text):
                return False

        # Must start with uppercase letter
        if not text[0].isupper():
            return False

        return True

    def _calculate_confidence(
        self,
        text: str,
        font_size: float,
        median_font: float,
        is_bold: bool,
        block: Dict
    ) -> float:
        """Calculate headline confidence score (0-1)."""
        score = 0.0

        # Font size contribution (0-0.4)
        font_ratio = font_size / median_font if median_font > 0 else 1
        score += min(0.4, (font_ratio - 1) * 0.2)

        # Bold contribution (0-0.2)
        if is_bold:
            score += 0.2

        # Length contribution (0-0.2) - prefer 5-12 words
        word_count = len(text.split())
        if 5 <= word_count <= 12:
            score += 0.2
        elif 3 <= word_count <= 15:
            score += 0.1

        # Position contribution (0-0.2) - higher on page = more likely headline
        position_y = block.get('position_y', 500)
        if position_y < 200:  # Top of page
            score += 0.2
        elif position_y < 400:
            score += 0.1

        return min(1.0, score)

    def _clean_headline(self, text: str) -> str:
        """Clean up headline text."""
        # Remove extra whitespace
        text = ' '.join(text.split())
        # Remove trailing punctuation (except ? and !)
        text = text.rstrip('.,;:')
        # Remove leading bullet points or numbers
        text = re.sub(r'^[\d•●○■□▪▫\-–—]\s*', '', text)
        return text.strip()

    def _get_context(self, headline_block: Dict, all_blocks: List[Dict]) -> str:
        """Get surrounding text for article context."""
        headline_y = headline_block.get('position_y', 0)

        # Find blocks immediately below the headline
        context_blocks = []
        for block in all_blocks:
            block_y = block.get('position_y', 0)
            if block_y > headline_y and block_y < headline_y + 300:
                if block.get('text') != headline_block.get('text'):
                    context_blocks.append(block.get('text', ''))

        return ' '.join(context_blocks)[:500]
```

---

## 4.3 Semantic Deduplication

```python
# newspaper_agent/nlp/deduplicator.py

from typing import List, Dict, Tuple
from sentence_transformers import SentenceTransformer
import numpy as np
import logging

logger = logging.getLogger(__name__)


class HeadlineDeduplicator:
    """
    Remove near-duplicate headlines using semantic similarity.
    Newspapers often report same news with slightly different wording.
    """

    def __init__(
        self,
        similarity_threshold: float = 0.85,
        model_name: str = "all-MiniLM-L6-v2"
    ):
        self.threshold = similarity_threshold
        self.model = SentenceTransformer(model_name)

    def deduplicate(self, headlines: List[Dict]) -> List[Dict]:
        """
        Remove semantically similar headlines, keeping highest confidence.

        Args:
            headlines: List of headline dicts with 'text' and 'confidence'

        Returns:
            Deduplicated list of headlines
        """
        if len(headlines) <= 1:
            return headlines

        # Sort by confidence (highest first)
        sorted_headlines = sorted(
            headlines, key=lambda h: h['confidence'], reverse=True
        )

        # Generate embeddings for all headlines
        texts = [h['text'] for h in sorted_headlines]
        embeddings = self.model.encode(texts, show_progress_bar=False)

        # Greedy deduplication: keep highest confidence, remove similar ones
        kept_indices = []
        removed_count = 0

        for i in range(len(sorted_headlines)):
            is_duplicate = False

            for kept_idx in kept_indices:
                similarity = self._cosine_similarity(
                    embeddings[i], embeddings[kept_idx]
                )
                if similarity >= self.threshold:
                    is_duplicate = True
                    removed_count += 1
                    break

            if not is_duplicate:
                kept_indices.append(i)

        result = [sorted_headlines[i] for i in kept_indices]

        logger.info(
            f"Deduplication: {len(headlines)} → {len(result)} "
            f"(removed {removed_count} duplicates)"
        )

        return result

    def _cosine_similarity(self, a: np.ndarray, b: np.ndarray) -> float:
        """Calculate cosine similarity between two vectors."""
        return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)))

    def find_similar_pairs(
        self,
        headlines: List[str],
        threshold: float = 0.8
    ) -> List[Tuple[int, int, float]]:
        """Find all pairs of similar headlines (for debugging)."""
        embeddings = self.model.encode(headlines)
        pairs = []

        for i in range(len(headlines)):
            for j in range(i+1, len(headlines)):
                sim = self._cosine_similarity(embeddings[i], embeddings[j])
                if sim >= threshold:
                    pairs.append((i, j, sim))

        return pairs
```

---

## 4.4 Category Classification

```python
# newspaper_agent/nlp/classifier.py

from typing import List, Dict

# Keyword-based quick classification (fast, no API call)
CATEGORY_KEYWORDS = {
    'politics': [
        'government', 'minister', 'parliament', 'election', 'party',
        'congress', 'bjp', 'opposition', 'vote', 'legislature',
        'president', 'prime minister', 'policy', 'bill', 'law'
    ],
    'business': [
        'market', 'stock', 'shares', 'company', 'revenue', 'profit',
        'economy', 'gdp', 'inflation', 'rbi', 'bank', 'investment',
        'startup', 'ipo', 'merger', 'acquisition', 'sensex', 'nifty'
    ],
    'sports': [
        'cricket', 'football', 'match', 'team', 'player', 'score',
        'win', 'tournament', 'ipl', 'olympic', 'medal', 'league',
        'coach', 'champion', 'final', 'innings', 'goal'
    ],
    'technology': [
        'ai', 'tech', 'software', 'google', 'apple', 'microsoft',
        'data', 'digital', 'app', 'startup', 'cyber', 'robot',
        'chip', 'semiconductor', 'internet', 'cloud'
    ],
    'world': [
        'us', 'china', 'russia', 'ukraine', 'israel', 'gaza',
        'european', 'united nations', 'nato', 'global', 'international',
        'foreign', 'diplomat', 'war', 'conflict', 'treaty'
    ],
    'health': [
        'health', 'hospital', 'doctor', 'disease', 'covid', 'vaccine',
        'medical', 'patient', 'drug', 'treatment', 'who', 'surgery'
    ],
    'science': [
        'nasa', 'isro', 'space', 'research', 'study', 'scientist',
        'discovery', 'climate', 'environment', 'species', 'planet'
    ],
    'entertainment': [
        'film', 'movie', 'bollywood', 'actor', 'actress', 'music',
        'concert', 'album', 'oscar', 'award', 'netflix', 'series'
    ],
}


class HeadlineCategorizer:
    """Classify headlines into news categories."""

    def categorize(self, headline: str) -> str:
        """
        Assign category to headline using keyword matching.
        Fast and doesn't require API calls.
        """
        headline_lower = headline.lower()

        scores = {}
        for category, keywords in CATEGORY_KEYWORDS.items():
            score = sum(
                1 for kw in keywords if kw in headline_lower
            )
            if score > 0:
                scores[category] = score

        if scores:
            return max(scores, key=scores.get)
        return 'general'

    def categorize_batch(self, headlines: List[Dict]) -> List[Dict]:
        """Add category to each headline dict."""
        for h in headlines:
            h['category'] = self.categorize(h['text'])
        return headlines

    def ensure_diversity(
        self,
        headlines: List[Dict],
        max_per_category: int = 15,
        total_limit: int = 50
    ) -> List[Dict]:
        """
        Ensure topic diversity in final selection.
        No single category dominates the top 50.
        """
        # Group by category
        by_category = {}
        for h in headlines:
            cat = h.get('category', 'general')
            by_category.setdefault(cat, []).append(h)

        # Select with diversity constraint
        selected = []
        category_counts = {cat: 0 for cat in by_category}

        # Sort all by confidence
        all_sorted = sorted(
            headlines, key=lambda h: h['confidence'], reverse=True
        )

        for h in all_sorted:
            if len(selected) >= total_limit:
                break

            cat = h.get('category', 'general')
            if category_counts[cat] < max_per_category:
                selected.append(h)
                category_counts[cat] += 1

        return selected
```

---

## 4.5 Dependencies

```bash
pip install sentence-transformers==2.2.2
pip install numpy==1.24.0
pip install spacy==3.7.0

# Download spaCy model (optional, for NER)
python -m spacy download en_core_web_sm
```

---

## 4.6 Testing

```python
# tests/test_nlp.py

from newspaper_agent.nlp.headline_detector import HeadlineDetector
from newspaper_agent.nlp.deduplicator import HeadlineDeduplicator
from newspaper_agent.nlp.classifier import HeadlineCategorizer


def test_headline_detection():
    detector = HeadlineDetector()

    pages = [{
        'page_number': 1,
        'blocks': [
            {'text': 'India GDP Grows 7.2% in Q3', 'font_size': 24,
             'is_bold': True, 'position_y': 50},
            {'text': 'The economy showed strong growth...', 'font_size': 12,
             'is_bold': False, 'position_y': 100},
            {'text': 'ADVERTISEMENT', 'font_size': 18,
             'is_bold': True, 'position_y': 400},
        ]
    }]

    candidates = detector.detect_headlines(pages)
    assert len(candidates) == 1
    assert 'GDP' in candidates[0]['text']


def test_deduplication():
    dedup = HeadlineDeduplicator(similarity_threshold=0.8)

    headlines = [
        {'text': 'India GDP growth hits 7.2% in Q3', 'confidence': 0.9},
        {'text': 'Indian economy grows at 7.2% in third quarter', 'confidence': 0.7},
        {'text': 'Supreme Court bans electoral bonds', 'confidence': 0.85},
    ]

    result = dedup.deduplicate(headlines)
    assert len(result) == 2  # Two unique topics


def test_categorization():
    cat = HeadlineCategorizer()
    assert cat.categorize("Sensex hits all-time high of 75000") == 'business'
    assert cat.categorize("India wins cricket World Cup") == 'sports'
    assert cat.categorize("PM Modi meets US President") == 'politics'
```

---

## ⏭️ Next Module

Proceed to **[Module 5: AI Ranking & Summarization](05_AI_Ranking_Summarization.md)** to rank headlines by importance and generate summaries using LLMs.
