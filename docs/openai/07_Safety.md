# 7. Safety & Moderation

## Moderation API

Free API to check text for harmful content.

```python
# Check text for policy violations
response = client.moderations.create(
    model="omni-moderation-latest",
    input="I want to learn about cybersecurity best practices.",
)

result = response.results[0]
print(f"Flagged: {result.flagged}")

for category, score in result.category_scores.model_dump().items():
    if score > 0.5:
        print(f"  {category}: {score:.3f}")

# Categories:
# harassment, harassment/threatening
# hate, hate/threatening
# illicit, illicit/violent
# self-harm, self-harm/intent, self-harm/instructions
# sexual, sexual/minors
# violence, violence/graphic
```

## Production Safety Pattern

```python
class OpenAISafetyPipeline:
    def __init__(self):
        self.client = OpenAI()
    
    async def check_input(self, text: str) -> dict:
        """Check user input before sending to LLM."""
        mod = self.client.moderations.create(model="omni-moderation-latest", input=text)
        if mod.results[0].flagged:
            categories = [k for k, v in mod.results[0].categories.model_dump().items() if v]
            return {"safe": False, "reason": f"Content policy: {categories}"}
        return {"safe": True}
    
    async def check_output(self, text: str) -> dict:
        """Check LLM output before showing to user."""
        mod = self.client.moderations.create(model="omni-moderation-latest", input=text)
        if mod.results[0].flagged:
            return {"safe": False, "reason": "Output flagged by moderation"}
        return {"safe": True}
    
    async def safe_completion(self, messages: list) -> str:
        """Full pipeline: check input → generate → check output."""
        # Input check
        user_msg = messages[-1]["content"]
        input_check = await self.check_input(user_msg)
        if not input_check["safe"]:
            return "I can't help with that request."
        
        # Generate
        response = self.client.chat.completions.create(model="gpt-4o", messages=messages)
        output = response.choices[0].message.content
        
        # Output check
        output_check = await self.check_output(output)
        if not output_check["safe"]:
            return "I generated a response that didn't meet safety standards. Let me try differently."
        
        return output
```

## Image Moderation

```python
# Check images for policy violations
response = client.moderations.create(
    model="omni-moderation-latest",
    input=[
        {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg"}},
    ],
)
print(f"Image flagged: {response.results[0].flagged}")
```

---

## Next: [Production Patterns →](08_Production.md)
