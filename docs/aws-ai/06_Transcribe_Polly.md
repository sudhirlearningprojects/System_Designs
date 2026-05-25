# 6. Amazon Transcribe & Polly

## Amazon Transcribe (Speech-to-Text)

```python
import boto3

transcribe = boto3.client("transcribe", region_name="us-east-1")

# Start transcription job (async, for files in S3)
transcribe.start_transcription_job(
    TranscriptionJobName="meeting-2024-01-15",
    Media={"MediaFileUri": "s3://my-bucket/audio/meeting.mp4"},
    MediaFormat="mp4",
    LanguageCode="en-US",
    Settings={
        "ShowSpeakerLabels": True,
        "MaxSpeakerLabels": 5,
        "ShowAlternatives": True,
        "MaxAlternatives": 3,
    },
    Subtitles={"Formats": ["vtt", "srt"]},
)

# Real-time streaming transcription
import asyncio
from amazon_transcribe.client import TranscribeStreamingClient
from amazon_transcribe.handlers import TranscriptResultStreamHandler

class MyHandler(TranscriptResultStreamHandler):
    async def handle_transcript_event(self, transcript_event):
        results = transcript_event.transcript.results
        for result in results:
            if not result.is_partial:
                print(f"[{result.start_time:.1f}s] {result.alternatives[0].transcript}")

async def stream_transcribe():
    client = TranscribeStreamingClient(region="us-east-1")
    stream = await client.start_stream_transcription(
        language_code="en-US",
        media_sample_rate_hz=16000,
        media_encoding="pcm",
    )
    handler = MyHandler(stream.output_stream)
    # Feed audio chunks to stream.input_stream
    await handler.handle_events()
```

---

## Amazon Polly (Text-to-Speech)

```python
polly = boto3.client("polly", region_name="us-east-1")

# Synthesize speech
response = polly.synthesize_speech(
    Text="Hello! How can I help you today?",
    OutputFormat="mp3",
    VoiceId="Joanna",  # Neural voice
    Engine="neural",
)

# Save audio
with open("output.mp3", "wb") as f:
    f.write(response["AudioStream"].read())

# SSML for fine control
ssml_text = """
<speak>
  <prosody rate="medium" pitch="+5%">
    I'm sorry to hear about your issue.
    <break time="500ms"/>
    Let me help you with that right away.
  </prosody>
</speak>
"""
response = polly.synthesize_speech(
    Text=ssml_text, TextType="ssml",
    OutputFormat="mp3", VoiceId="Joanna", Engine="neural",
)

# List available voices
voices = polly.describe_voices(Engine="neural", LanguageCode="en-US")
for voice in voices["Voices"]:
    print(f"{voice['Id']}: {voice['Name']} ({voice['Gender']})")
```

---

## Next: [Amazon Q →](07_Amazon_Q.md)
