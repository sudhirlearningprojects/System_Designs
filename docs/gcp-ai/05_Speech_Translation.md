# 5. Speech & Translation

## Cloud Speech-to-Text

```python
from google.cloud import speech_v2 as speech

client = speech.SpeechClient()

# Transcribe audio file
with open("audio.wav", "rb") as f:
    audio_content = f.read()

config = speech.RecognitionConfig(
    auto_decoding_config=speech.AutoDetectDecodingConfig(),
    language_codes=["en-US"],
    model="long",  # "short", "long", "chirp" (latest)
    features=speech.RecognitionFeatures(
        enable_automatic_punctuation=True,
        enable_word_time_offsets=True,
        enable_spoken_punctuation=True,
    ),
)

request = speech.RecognizeRequest(
    recognizer=f"projects/my-project/locations/global/recognizers/_",
    config=config,
    content=audio_content,
)

response = client.recognize(request=request)
for result in response.results:
    print(f"Transcript: {result.alternatives[0].transcript}")
    print(f"Confidence: {result.alternatives[0].confidence:.2f}")
    for word in result.alternatives[0].words:
        print(f"  [{word.start_offset.total_seconds():.1f}s] {word.word}")

# Streaming (real-time)
import queue
import threading

def stream_audio():
    """Stream microphone audio for real-time transcription."""
    streaming_config = speech.StreamingRecognitionConfig(
        config=config,
        streaming_features=speech.StreamingRecognitionFeatures(
            interim_results=True,  # Get partial results
        ),
    )
    
    # Send audio chunks as they arrive
    requests = generate_audio_requests(streaming_config)
    responses = client.streaming_recognize(requests=requests)
    
    for response in responses:
        for result in response.results:
            if result.is_final:
                print(f"Final: {result.alternatives[0].transcript}")
            else:
                print(f"Interim: {result.alternatives[0].transcript}", end="\r")
```

---

## Cloud Text-to-Speech

```python
from google.cloud import texttospeech

client = texttospeech.TextToSpeechClient()

# Synthesize speech
response = client.synthesize_speech(
    input=texttospeech.SynthesisInput(text="Hello! How can I help you today?"),
    voice=texttospeech.VoiceSelectionParams(
        language_code="en-US",
        name="en-US-Journey-F",  # Neural2 voice
        ssml_gender=texttospeech.SsmlVoiceGender.FEMALE,
    ),
    audio_config=texttospeech.AudioConfig(
        audio_encoding=texttospeech.AudioEncoding.MP3,
        speaking_rate=1.0,
        pitch=0.0,
    ),
)

with open("output.mp3", "wb") as f:
    f.write(response.audio_content)

# SSML for fine control
ssml = """<speak>
  <prosody rate="medium" pitch="+2st">
    I'm sorry to hear about your issue.
    <break time="300ms"/>
    Let me look into that for you.
  </prosody>
</speak>"""

response = client.synthesize_speech(
    input=texttospeech.SynthesisInput(ssml=ssml),
    voice=texttospeech.VoiceSelectionParams(language_code="en-US", name="en-US-Neural2-C"),
    audio_config=texttospeech.AudioConfig(audio_encoding=texttospeech.AudioEncoding.MP3),
)
```

---

## Cloud Translation

```python
from google.cloud import translate_v3 as translate

client = translate.TranslationServiceClient()
parent = f"projects/my-project/locations/global"

# Translate text
response = client.translate_text(
    parent=parent,
    contents=["Hello, how can I help you?", "What is your refund policy?"],
    target_language_code="es",  # Spanish
    source_language_code="en",
)

for translation in response.translations:
    print(f"Translated: {translation.translated_text}")

# Detect language
response = client.detect_language(parent=parent, content="Bonjour, comment allez-vous?")
for language in response.languages:
    print(f"Language: {language.language_code}, Confidence: {language.confidence:.2f}")
```

---

## Next: [Vertex AI ML Platform →](06_ML_Platform.md)
