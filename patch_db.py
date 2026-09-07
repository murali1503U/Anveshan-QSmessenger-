with open("app/src/main/java/com/example/meshchat/data/SecureSession.kt", "r") as f:
    content = f.read()
content = content.replace("val isDemoSimulationEnabled: Boolean = false,", "")
with open("app/src/main/java/com/example/meshchat/data/SecureSession.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/meshchat/data/SecureSessionDao.kt", "r") as f:
    content = f.read()
import re
content = re.sub(r'@Query\("UPDATE secure_sessions SET isDemoSimulationEnabled = :enabled WHERE sessionId = :sessionId"\)\n\s*suspend fun updateDemoSimulationStatus\(sessionId: String, enabled: Boolean\)', '', content)
with open("app/src/main/java/com/example/meshchat/data/SecureSessionDao.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/meshchat/data/SessionRepository.kt", "r") as f:
    content = f.read()
content = re.sub(r'suspend fun setDemoSimulation.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'suspend fun isDemoSimulation.*?\}', '', content, flags=re.DOTALL)
with open("app/src/main/java/com/example/meshchat/data/SessionRepository.kt", "w") as f:
    f.write(content)
