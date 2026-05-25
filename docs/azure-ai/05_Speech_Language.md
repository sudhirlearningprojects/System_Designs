# 5. Azure AI Speech & Language

## Speech-to-Text (Real-Time & Batch)

```python
# pip install azure-cognitiveservices-speech

import azure.cognitiveservices.speech as speechsdk

# Real-time speech recognition
speech_config = speechsdk.SpeechConfig(subscription="your-key", region="eastus")
speech_config.speech_recognition_language = "en-US"

# From microphone
recognizer = speechsdk.SpeechRecognizer(speech_config=speech_config)
result = recognizer.recognize_once()
print(f"Recognized: {result.text}")

# From audio file
audio_config = speechsdk.AudioConfig(filename="meeting.wav")
recognizer = speechsdk.SpeechRecognizer(speech_config=speech_config, audio_config=audio_config)

# Continuous recognition (for long audio)
all_text = []
def handle_result(evt):
    all_text.append(evt.result.text)

recognizer.recognized.connect(handle_result)
recognizer.start_continuous_recognition()
# ... wait for completion
recognizer.stop_continuous_recognition()
transcript = " ".join(all_text)
```

## Text-to-Speech

```python
speech_config = speechsdk.SpeechConfig(subscription="your-key", region="eastus")
speech_config.speech_synthesis_voice_name = "en-US-JennyNeural"

synthesizer = speechsdk.SpeechSynthesizer(speech_config=speech_config)
result = synthesizer.speak_text("Hello! How can I help you today?")

# Save to file
audio_config = speechsdk.AudioConfig(filename="output.wav")
synthesizer = speechsdk.SpeechSynthesizer(speech_config=speech_config, audio_config=audio_config)
synthesizer.speak_text("Your subscription has been cancelled.")

# SSML for fine control (pitch, rate, emphasis)
ssml = """
<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="en-US">
  <voice name="en-US-JennyNeural">
    <prosody rate="+10%" pitch="+5%">
      I'm sorry to hear about your issue. Let me help you with that.
    </prosody>
  </voice>
</speak>"""
synthesizer.speak_ssml(ssml)
```

## Language Understanding (CLU)

```python
# pip install azure-ai-language-conversations

from azure.ai.language.conversations import ConversationAnalysisClient
from azure.core.credentials import AzureKeyCredential

client = ConversationAnalysisClient(
    endpoint="https://my-language.cognitiveservices.azure.com/",
    credential=AzureKeyCredential("your-key"),
)

# Analyze conversation intent + entities
result = client.analyze_conversation(
    task={
        "kind": "Conversation",
        "analysisInput": {
            "conversationItem": {
                "id": "1",
                "text": "I want to cancel my Pro subscription",
                "participantId": "user",
            }
        },
        "parameters": {
            "projectName": "support-intents",
            "deploymentName": "production",
        },
    }
)

intent = result["result"]["prediction"]["topIntent"]
entities = result["result"]["prediction"]["entities"]
print(f"Intent: {intent}")  # "CancelSubscription"
print(f"Entities: {entities}")  # [{"category": "PlanType", "text": "Pro"}]
```

---

## Next: [Azure Machine Learning →](06_Azure_ML.md)
