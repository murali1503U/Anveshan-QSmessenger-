import re

with open("app/src/main/java/com/example/meshchat/ai/GeminiService.kt", "r") as f:
    content = f.read()

content = content.replace("BuildConfig.GEMINI_API_KEY", "\"AIzaSySentinelTestKey\"")

with open("app/src/main/java/com/example/meshchat/ai/GeminiService.kt", "w") as f:
    f.write(content)
