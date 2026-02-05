# Cost Optimization Guide - Free Tier Strategy

## 🎯 Objective
Run the Smart Receipt Analyzer with GenAI capabilities at **$0 additional cost** using free tiers.

## 🆓 Free Tier Options

### Option 1: Google Gemini Free Tier (Recommended)

**Limits:**
- **Gemini 1.5 Flash**: 15 RPM, 1M TPM, 1500 RPD
- **Gemini 1.5 Pro**: 2 RPM, 32K TPM, 50 RPD

**Strategy:**
```
Use Gemini Flash for everything:
- Receipt analysis: 1500/day = 45K/month ✅
- Chat queries: Within 1M tokens/min ✅
- Fraud detection: Batch process nightly ✅
- Insights: Weekly reports only ✅

Cost: $0/month
```

### Option 2: AWS Bedrock Free Tier

**Limits:**
- First 2 months free (limited tokens)
- Then pay-as-you-go

**Models:**
- Claude 3 Haiku: $0.25/$1.25 per 1M tokens
- Titan Text: $0.20/$0.60 per 1M tokens

**Cost:** $0 (first 2 months), then ~$30/month

### Option 3: OpenAI Free Credits

**Limits:**
- $5 free credits for new accounts
- GPT-3.5 Turbo: $0.50/$1.50 per 1M tokens

**Cost:** $0 (until credits exhausted), then ~$40/month

## 💡 Optimization Strategies

### 1. Batch Processing
```python
# Instead of real-time fraud detection
# Run nightly batch job

def nightly_fraud_check():
    """Process all today's receipts in one batch"""
    today_receipts = get_receipts_by_date(today)
    
    # Single API call for all receipts
    fraud_results = analyze_batch(today_receipts)
    
    # Update DynamoDB
    update_fraud_scores(fraud_results)

# Schedule with EventBridge
# Rate: cron(0 2 * * ? *)  # 2 AM daily
```

### 2. Caching Strategy
```python
import hashlib
from functools import lru_cache

# Cache merchant categorization
@lru_cache(maxsize=1000)
def categorize_merchant(merchant_name):
    """Cache category for known merchants"""
    return call_gemini_api(f"Categorize: {merchant_name}")

# Cache common queries
QUERY_CACHE = {}

def get_cached_response(query, user_id):
    cache_key = hashlib.md5(f"{user_id}:{query}".encode()).hexdigest()
    
    if cache_key in QUERY_CACHE:
        return QUERY_CACHE[cache_key]
    
    response = call_gemini_api(query)
    QUERY_CACHE[cache_key] = response
    return response
```

### 3. Smart Fallbacks
```python
def analyze_receipt_smart(image_bytes):
    """Use Gemini only when needed"""
    
    # Try Textract first (already paid for)
    textract_result = extract_with_textract(image_bytes)
    
    # Use Gemini only for complex receipts
    if textract_result['confidence'] < 0.8:
        return analyze_with_gemini(image_bytes)
    
    return textract_result
```

### 4. Rate Limiting
```python
import time
from collections import deque

class RateLimiter:
    def __init__(self, max_per_minute=15):
        self.max_per_minute = max_per_minute
        self.requests = deque()
    
    def wait_if_needed(self):
        now = time.time()
        
        # Remove requests older than 1 minute
        while self.requests and self.requests[0] < now - 60:
            self.requests.popleft()
        
        # Wait if at limit
        if len(self.requests) >= self.max_per_minute:
            sleep_time = 60 - (now - self.requests[0])
            time.sleep(sleep_time)
        
        self.requests.append(time.time())

limiter = RateLimiter(max_per_minute=15)

def call_gemini_with_limit(prompt):
    limiter.wait_if_needed()
    return client.models.generate_content(prompt)
```

### 5. Prompt Optimization
```python
# ❌ Bad: Verbose prompt (more tokens)
prompt = """
Please analyze this receipt image carefully and extract all the information 
you can find including the merchant name, total amount, date, items purchased,
payment method, and any other relevant details. Be as detailed as possible.
"""

# ✅ Good: Concise prompt (fewer tokens)
prompt = """Extract from receipt:
- merchant, amount, date, items, payment_method
Return JSON only."""
```

### 6. Selective AI Usage
```python
def should_use_ai(receipt_data):
    """Decide if AI is needed"""
    
    # Use AI only for:
    # 1. Low confidence OCR
    if receipt_data.get('confidence', 1.0) < 0.8:
        return True
    
    # 2. High-value receipts (fraud risk)
    if receipt_data.get('amount', 0) > 500:
        return True
    
    # 3. Unknown merchants
    if receipt_data.get('merchant') not in KNOWN_MERCHANTS:
        return True
    
    return False

def process_receipt(receipt_data):
    if should_use_ai(receipt_data):
        return analyze_with_gemini(receipt_data)
    else:
        return basic_processing(receipt_data)
```

## 📊 Free Tier Capacity

### Gemini Flash (15 RPM, 1500 RPD)

**Daily Capacity:**
```
1500 requests/day = 45,000 requests/month

Allocation:
- Receipt analysis: 1000/day (30K/month)
- Chat queries: 400/day (12K/month)
- Fraud detection: 50/day (1.5K/month)
- Insights: 50/day (1.5K/month)

Total: 1500/day ✅
```

**If Exceeding Free Tier:**
```
Option A: Queue requests for next day
Option B: Use fallback (Textract only)
Option C: Upgrade to paid tier ($20/month)
```

## 🔄 Hybrid Approach (Recommended)

```python
class AIService:
    def __init__(self):
        self.daily_quota = 1500
        self.used_today = 0
        self.reset_time = None
    
    def analyze_receipt(self, image_bytes):
        # Check quota
        if self.used_today >= self.daily_quota:
            # Fallback to Textract
            return self.textract_fallback(image_bytes)
        
        # Use Gemini
        self.used_today += 1
        return self.gemini_analysis(image_bytes)
    
    def reset_quota_if_needed(self):
        now = datetime.now()
        if self.reset_time is None or now > self.reset_time:
            self.used_today = 0
            self.reset_time = now.replace(hour=0, minute=0) + timedelta(days=1)
```

## 💰 Cost Comparison

| Strategy | Monthly Cost | Receipts/Month | Features |
|----------|--------------|----------------|----------|
| **Free Tier Only** | **$0** | 45K | All features, some delays |
| **Hybrid (Free + Textract)** | **$0** | Unlimited | Fast, reliable |
| **Paid Tier** | $20 | Unlimited | Real-time, no limits |
| **AWS Bedrock** | $30 | Unlimited | AWS-native |

## 🎯 Recommended Setup for MVP

```python
# config.py
AI_CONFIG = {
    'provider': 'gemini',
    'model': 'gemini-1.5-flash',
    'free_tier': True,
    'daily_limit': 1500,
    'fallback': 'textract',
    'cache_enabled': True,
    'batch_processing': True
}

# Use AI selectively
ENABLE_AI_FOR = {
    'receipt_analysis': True,      # High value
    'fraud_detection': True,       # Critical
    'chat_assistant': True,        # User engagement
    'insights': False,             # Use scheduled reports
    'enrichment': False            # Nice-to-have
}
```

## 📈 Scaling Strategy

**Phase 1: MVP (0-1K users)**
- Use 100% free tier
- Cost: $0/month

**Phase 2: Growth (1K-10K users)**
- Free tier + caching
- Cost: $0-10/month

**Phase 3: Scale (10K-100K users)**
- Hybrid (free + paid)
- Cost: $20-50/month

**Phase 4: Enterprise (100K+ users)**
- Full paid tier or AWS Bedrock
- Cost: $200-500/month

## ✅ Action Items

1. **Sign up for Gemini free tier** (no credit card)
2. **Implement rate limiting** (15 RPM)
3. **Enable caching** (reduce API calls)
4. **Use batch processing** (nightly jobs)
5. **Monitor quota usage** (CloudWatch metrics)
6. **Set up fallbacks** (Textract when quota exceeded)

## 🔗 Resources

- Gemini Free API: https://aistudio.google.com/app/apikey
- AWS Bedrock Pricing: https://aws.amazon.com/bedrock/pricing/
- OpenAI Pricing: https://openai.com/pricing

---

**Bottom Line:** You can run the entire system with GenAI capabilities at **$0 additional cost** using the free tier! 🎉
