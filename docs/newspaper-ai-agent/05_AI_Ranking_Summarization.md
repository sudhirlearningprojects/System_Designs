# Module 5: AI Ranking & Summarization

## 🎯 Learning Objectives

- Rank headlines by importance using LLM
- Generate concise 2-3 line summaries
- Handle LLM rate limits and errors
- Optimize token usage and cost
- Batch processing for efficiency

---

## 5.1 LLM Client Abstraction

```python
# newspaper_agent/summarizer/llm_client.py

from typing import List, Dict, Optional
from openai import OpenAI
from anthropic import Anthropic
import json
import logging
import time

logger = logging.getLogger(__name__)


class LLMClient:
    """Unified LLM client supporting OpenAI and Anthropic."""

    def __init__(
        self,
        provider: str = "openai",
        model: str = "gpt-4o",
        api_key: str = "",
        temperature: float = 0.3,
        max_retries: int = 3,
    ):
        self.provider = provider
        self.model = model
        self.temperature = temperature
        self.max_retries = max_retries

        if provider == "openai":
            self.client = OpenAI(api_key=api_key)
        elif provider == "anthropic":
            self.client = Anthropic(api_key=api_key)
        else:
            raise ValueError(f"Unsupported provider: {provider}")

    def complete(
        self,
        system_prompt: str,
        user_prompt: str,
        max_tokens: int = 4096
    ) -> str:
        """Send a completion request with retry logic."""
        for attempt in range(self.max_retries):
            try:
                if self.provider == "openai":
                    return self._openai_complete(
                        system_prompt, user_prompt, max_tokens
                    )
                elif self.provider == "anthropic":
                    return self._anthropic_complete(
                        system_prompt, user_prompt, max_tokens
                    )
            except Exception as e:
                wait = 2 ** attempt
                logger.warning(
                    f"LLM call failed (attempt {attempt+1}): {e}. "
                    f"Retrying in {wait}s..."
                )
                if attempt < self.max_retries - 1:
                    time.sleep(wait)
                else:
                    raise

    def _openai_complete(
        self, system: str, user: str, max_tokens: int
    ) -> str:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            temperature=self.temperature,
            max_tokens=max_tokens,
        )
        return response.choices[0].message.content

    def _anthropic_complete(
        self, system: str, user: str, max_tokens: int
    ) -> str:
        response = self.client.messages.create(
            model=self.model,
            system=system,
            messages=[{"role": "user", "content": user}],
            temperature=self.temperature,
            max_tokens=max_tokens,
        )
        return response.content[0].text
```

---

## 5.2 Headline Ranker

```python
# newspaper_agent/nlp/ranker.py

from typing import List, Dict
from newspaper_agent.summarizer.llm_client import LLMClient
import json
import logging

logger = logging.getLogger(__name__)

RANKING_SYSTEM_PROMPT = """You are a senior newspaper editor. Your job is to rank 
headlines by their news importance and public interest.

Ranking criteria (in order of importance):
1. Impact on citizens/public life
2. National/international significance
3. Economic impact
4. Uniqueness (not routine news)
5. Timeliness and relevance

You must return valid JSON only."""

RANKING_USER_PROMPT = """Below are {count} candidate headlines from today's newspapers.
Select and rank the TOP {target} most important and unique headlines.

Remove any duplicates or near-similar headlines (keep the better-worded one).
Ensure diversity across topics (politics, business, sports, tech, world, etc.).

Headlines:
{headlines_list}

Return JSON array with exactly {target} items:
[
  {{"rank": 1, "headline": "exact headline text", "category": "politics|business|sports|tech|world|health|science|entertainment|general"}},
  ...
]

Return ONLY the JSON array, no other text."""


class HeadlineRanker:
    """Rank headlines by importance using LLM."""

    def __init__(self, llm_client: LLMClient, target_count: int = 50):
        self.llm = llm_client
        self.target_count = target_count

    def rank_headlines(self, candidates: List[Dict]) -> List[Dict]:
        """
        Rank candidate headlines and select top N.
        
        Processes in batches if candidates > 100 (to stay within token limits).
        """
        if len(candidates) <= self.target_count:
            # If fewer candidates than target, rank all
            return self._rank_batch(candidates, self.target_count)

        if len(candidates) <= 100:
            # Single batch
            return self._rank_batch(candidates, self.target_count)

        # Multiple batches for large candidate sets
        logger.info(
            f"Large candidate set ({len(candidates)}), processing in batches"
        )
        return self._rank_multi_batch(candidates)

    def _rank_batch(
        self,
        candidates: List[Dict],
        target: int
    ) -> List[Dict]:
        """Rank a single batch of headlines."""
        # Format headlines for prompt
        headlines_text = "\n".join(
            f"{i+1}. {h['text']}"
            for i, h in enumerate(candidates)
        )

        prompt = RANKING_USER_PROMPT.format(
            count=len(candidates),
            target=min(target, len(candidates)),
            headlines_list=headlines_text
        )

        response = self.llm.complete(
            system_prompt=RANKING_SYSTEM_PROMPT,
            user_prompt=prompt,
            max_tokens=4096
        )

        # Parse JSON response
        ranked = self._parse_ranking_response(response)

        # Merge with original data (context, confidence, etc.)
        return self._merge_with_originals(ranked, candidates)

    def _rank_multi_batch(self, candidates: List[Dict]) -> List[Dict]:
        """
        Process large candidate sets in batches.
        Round 1: Get top 30 from each batch of 80
        Round 2: Final ranking from combined shortlist
        """
        batch_size = 80
        shortlisted = []

        # Round 1: Shortlist from batches
        for i in range(0, len(candidates), batch_size):
            batch = candidates[i:i + batch_size]
            top_from_batch = self._rank_batch(batch, 30)
            shortlisted.extend(top_from_batch)

        # Round 2: Final selection
        logger.info(f"Round 2: Selecting top {self.target_count} from {len(shortlisted)} shortlisted")
        return self._rank_batch(shortlisted, self.target_count)

    def _parse_ranking_response(self, response: str) -> List[Dict]:
        """Parse LLM JSON response, handling common issues."""
        # Clean response
        text = response.strip()

        # Remove markdown code blocks if present
        if text.startswith("```"):
            text = text.split("\n", 1)[1]
            text = text.rsplit("```", 1)[0]

        try:
            data = json.loads(text)
            if isinstance(data, list):
                return data
            elif isinstance(data, dict) and 'headlines' in data:
                return data['headlines']
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse LLM response: {e}")
            logger.debug(f"Response was: {text[:500]}")

            # Try to extract JSON array from response
            import re
            match = re.search(r'\[[\s\S]*\]', text)
            if match:
                try:
                    return json.loads(match.group())
                except json.JSONDecodeError:
                    pass

        return []

    def _merge_with_originals(
        self,
        ranked: List[Dict],
        originals: List[Dict]
    ) -> List[Dict]:
        """Merge ranked results with original headline data."""
        # Build lookup by headline text
        original_lookup = {
            h['text'].lower().strip(): h for h in originals
        }

        merged = []
        for r in ranked:
            headline_text = r.get('headline', '')
            original = original_lookup.get(headline_text.lower().strip(), {})

            merged.append({
                'headline': headline_text,
                'rank': r.get('rank', len(merged) + 1),
                'category': r.get('category', original.get('category', 'general')),
                'confidence': original.get('confidence', 0.5),
                'context': original.get('context', ''),
                'page_number': original.get('page_number', 0),
            })

        return merged
```

---

## 5.3 Summary Generator

```python
# newspaper_agent/summarizer/headline_enricher.py

from typing import List, Dict
from newspaper_agent.summarizer.llm_client import LLMClient
import json
import logging

logger = logging.getLogger(__name__)

SUMMARY_SYSTEM_PROMPT = """You are a news summarizer. For each headline, write a 
concise 2-3 line summary (50-80 words) that:
- Explains what happened
- Mentions key people/organizations involved
- States the significance or impact

Be factual, neutral, and concise. No opinions.
Return valid JSON only."""

SUMMARY_BATCH_PROMPT = """Generate 2-3 line summaries for each headline below.
Use the provided context to write accurate summaries.

Headlines with context:
{headlines_with_context}

Return JSON array:
[
  {{"headline": "exact headline", "summary": "2-3 line summary here"}},
  ...
]

Return ONLY the JSON array."""


class HeadlineEnricher:
    """Generate 2-3 line summaries for ranked headlines."""

    def __init__(self, llm_client: LLMClient, batch_size: int = 10):
        self.llm = llm_client
        self.batch_size = batch_size

    def enrich_headlines(self, ranked_headlines: List[Dict]) -> List[Dict]:
        """
        Generate summaries for all ranked headlines.
        Processes in batches to manage token limits.
        """
        all_enriched = []

        for i in range(0, len(ranked_headlines), self.batch_size):
            batch = ranked_headlines[i:i + self.batch_size]
            enriched_batch = self._enrich_batch(batch)
            all_enriched.extend(enriched_batch)

        logger.info(f"Generated summaries for {len(all_enriched)} headlines")
        return all_enriched

    def _enrich_batch(self, batch: List[Dict]) -> List[Dict]:
        """Generate summaries for a batch of headlines."""
        # Format headlines with their context
        formatted = ""
        for i, h in enumerate(batch, 1):
            formatted += f"\n{i}. Headline: {h['headline']}"
            if h.get('context'):
                # Limit context to 200 chars to save tokens
                context = h['context'][:200]
                formatted += f"\n   Context: {context}"
            formatted += "\n"

        prompt = SUMMARY_BATCH_PROMPT.format(
            headlines_with_context=formatted
        )

        response = self.llm.complete(
            system_prompt=SUMMARY_SYSTEM_PROMPT,
            user_prompt=prompt,
            max_tokens=3000
        )

        summaries = self._parse_summaries(response)

        # Merge summaries back into headline dicts
        summary_lookup = {
            s['headline'].lower().strip(): s['summary']
            for s in summaries
        }

        enriched = []
        for h in batch:
            summary = summary_lookup.get(
                h['headline'].lower().strip(),
                self._generate_fallback_summary(h)
            )
            enriched.append({
                **h,
                'summary': summary,
            })

        return enriched

    def _parse_summaries(self, response: str) -> List[Dict]:
        """Parse summary JSON response."""
        text = response.strip()
        if text.startswith("```"):
            text = text.split("\n", 1)[1]
            text = text.rsplit("```", 1)[0]

        try:
            return json.loads(text)
        except json.JSONDecodeError:
            import re
            match = re.search(r'\[[\s\S]*\]', text)
            if match:
                try:
                    return json.loads(match.group())
                except json.JSONDecodeError:
                    pass
        return []

    def _generate_fallback_summary(self, headline: Dict) -> str:
        """Fallback summary when LLM parsing fails."""
        context = headline.get('context', '')
        if context:
            # Use first 2 sentences of context
            sentences = context.split('.')[:2]
            return '. '.join(s.strip() for s in sentences if s.strip()) + '.'
        return f"[Summary pending for: {headline['headline']}]"
```

---

## 5.4 Token Usage Optimization

```python
# newspaper_agent/summarizer/token_optimizer.py

import tiktoken
from typing import List, Dict


class TokenOptimizer:
    """Optimize LLM token usage to minimize costs."""

    def __init__(self, model: str = "gpt-4o"):
        self.encoder = tiktoken.encoding_for_model(model)
        # GPT-4o pricing (per 1M tokens)
        self.input_cost = 2.50   # $2.50 per 1M input
        self.output_cost = 10.00  # $10.00 per 1M output

    def count_tokens(self, text: str) -> int:
        """Count tokens in text."""
        return len(self.encoder.encode(text))

    def estimate_cost(
        self,
        headlines_count: int,
        avg_context_length: int = 200
    ) -> Dict:
        """Estimate API cost for processing headlines."""
        # Ranking call
        rank_input = headlines_count * 15  # ~15 tokens per headline
        rank_output = headlines_count * 10  # ~10 tokens per ranked item

        # Summary calls (batches of 10)
        summary_batches = (headlines_count + 9) // 10
        summary_input_per_batch = 10 * (15 + avg_context_length // 4)
        summary_output_per_batch = 10 * 60  # ~60 tokens per summary

        total_input = rank_input + (summary_batches * summary_input_per_batch)
        total_output = rank_output + (summary_batches * summary_output_per_batch)

        input_cost = (total_input / 1_000_000) * self.input_cost
        output_cost = (total_output / 1_000_000) * self.output_cost

        return {
            'total_input_tokens': total_input,
            'total_output_tokens': total_output,
            'input_cost_usd': round(input_cost, 4),
            'output_cost_usd': round(output_cost, 4),
            'total_cost_usd': round(input_cost + output_cost, 4),
        }

    def truncate_context(self, context: str, max_tokens: int = 100) -> str:
        """Truncate context to fit token budget."""
        tokens = self.encoder.encode(context)
        if len(tokens) <= max_tokens:
            return context
        truncated_tokens = tokens[:max_tokens]
        return self.encoder.decode(truncated_tokens) + "..."
```

---

## 5.5 Complete Ranking + Summarization Pipeline

```python
# newspaper_agent/summarizer/__init__.py

from typing import List, Dict
from .llm_client import LLMClient
from .headline_enricher import HeadlineEnricher
from ..nlp.ranker import HeadlineRanker
import logging

logger = logging.getLogger(__name__)


class HeadlinePipeline:
    """Complete pipeline: Rank → Dedupe → Summarize → Output."""

    def __init__(
        self,
        api_key: str,
        provider: str = "openai",
        model: str = "gpt-4o",
        target_headlines: int = 50,
    ):
        self.llm = LLMClient(
            provider=provider,
            model=model,
            api_key=api_key,
        )
        self.ranker = HeadlineRanker(self.llm, target_headlines)
        self.enricher = HeadlineEnricher(self.llm)

    def process(self, candidates: List[Dict]) -> List[Dict]:
        """
        Full pipeline: rank candidates and generate summaries.
        
        Input: 100-200 candidate headlines
        Output: Top 50 with summaries
        """
        logger.info(f"Pipeline input: {len(candidates)} candidates")

        # Step 1: Rank and select top 50
        ranked = self.ranker.rank_headlines(candidates)
        logger.info(f"After ranking: {len(ranked)} headlines selected")

        # Step 2: Generate summaries
        enriched = self.enricher.enrich_headlines(ranked)
        logger.info(f"After enrichment: {len(enriched)} headlines with summaries")

        return enriched
```

---

## 5.6 Dependencies

```bash
pip install openai==1.12.0
pip install anthropic==0.18.0
pip install tiktoken==0.5.2
```

---

## 5.7 Testing

```python
# tests/test_summarizer.py

from newspaper_agent.summarizer.llm_client import LLMClient
from newspaper_agent.nlp.ranker import HeadlineRanker
from unittest.mock import MagicMock, patch


def test_ranking_response_parsing():
    ranker = HeadlineRanker(MagicMock(), 5)

    response = '''[
        {"rank": 1, "headline": "India GDP grows 7.2%", "category": "business"},
        {"rank": 2, "headline": "SC bans electoral bonds", "category": "politics"}
    ]'''

    parsed = ranker._parse_ranking_response(response)
    assert len(parsed) == 2
    assert parsed[0]['rank'] == 1


def test_fallback_summary():
    from newspaper_agent.summarizer.headline_enricher import HeadlineEnricher
    enricher = HeadlineEnricher(MagicMock())

    headline = {
        'headline': 'Test headline',
        'context': 'First sentence here. Second sentence here. Third.'
    }

    summary = enricher._generate_fallback_summary(headline)
    assert 'First sentence' in summary
```

---

## ⏭️ Next Module

Proceed to **[Module 6: Google Docs Integration](06_Google_Docs_Integration.md)** to build the document creation and formatting system.
