# 6. Realtime API & Speech

## Realtime API (Voice Agents)

The Realtime API enables low-latency, multi-modal conversations (speech-in, speech-out) over WebSocket.

```python
import asyncio
import websockets
import json
import base64

async def voice_agent():
    url = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview"
    headers = {"Authorization": f"Bearer {OPENAI_API_KEY}", "OpenAI-Beta": "realtime=v1"}
    
    async with websockets.connect(url, extra_headers=headers) as ws:
        # Configure session
        await ws.send(json.dumps({
            "type": "session.update",
            "session": {
                "modalities": ["text", "audio"],
                "instructions": "You are a helpful support agent. Be concise.",
                "voice": "alloy",
                "input_audio_format": "pcm16",
                "output_audio_format": "pcm16",
                "tools": [{
                    "type": "function",
                    "name": "get_subscription",
                    "description": "Get user subscription",
                    "parameters": {"type": "object", "properties": {"user_id": {"type": "string"}}},
                }],
            },
        }))
        
        # Send audio (from microphone)
        # await ws.send(json.dumps({"type": "input_audio_buffer.append", "audio": base64_audio}))
        # await ws.send(json.dumps({"type": "input_audio_buffer.commit"}))
        
        # Receive events
        async for message in ws:
            event = json.loads(message)
            match event["type"]:
                case "response.audio.delta":
                    audio_chunk = base64.b64decode(event["delta"])
                    # Play audio chunk
                case "response.text.delta":
                    print(event["delta"], end="")
                case "response.function_call_arguments.done":
                    # Execute tool, send result back
                    pass
```

## Whisper (Speech-to-Text)

```python
# Transcribe audio
with open("meeting.mp3", "rb") as f:
    transcript = client.audio.transcriptions.create(
        model="whisper-1",
        file=f,
        response_format="verbose_json",  # Includes timestamps
        timestamp_granularities=["word", "segment"],
        language="en",
    )

print(transcript.text)
for segment in transcript.segments:
    print(f"[{segment.start:.1f}s - {segment.end:.1f}s] {segment.text}")

# Translation (any language → English)
translation = client.audio.translations.create(model="whisper-1", file=open("french.mp3", "rb"))
```

## TTS (Text-to-Speech)

```python
# Generate speech
response = client.audio.speech.create(
    model="tts-1-hd",  # or "tts-1" (faster, lower quality)
    voice="nova",  # alloy, echo, fable, onyx, nova, shimmer
    input="Hello! How can I help you today?",
    speed=1.0,  # 0.25 to 4.0
)

# Save to file
response.stream_to_file("output.mp3")

# Stream audio (for real-time playback)
with client.audio.speech.with_streaming_response.create(
    model="tts-1", voice="alloy", input="Streaming audio response..."
) as response:
    for chunk in response.iter_bytes(chunk_size=1024):
        # Play chunk in real-time
        pass
```

---

## Next: [Safety & Moderation →](07_Safety.md)
