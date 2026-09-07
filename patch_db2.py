import re

with open("app/src/main/java/com/example/meshchat/data/SecureSessionDao.kt", "r") as f:
    content = f.read()

content = re.sub(r'@Query\("UPDATE secure_sessions SET isDemoSimulationEnabled = :enabled WHERE sessionId = :sessionId"\)\n\s*suspend fun updateDemoSimulationStatus\(sessionId: String, enabled: Boolean\)', '', content)

with open("app/src/main/java/com/example/meshchat/data/SecureSessionDao.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = re.sub(r'val initialIsDemo = initialSession\.isDemoSimulationEnabled\n', '', content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
