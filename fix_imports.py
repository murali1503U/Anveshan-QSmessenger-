import re

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "r") as f:
    content = f.read()

content = content.replace('import kotlin.math.*', 'import kotlin.math.*\nimport android.content.Context\nimport android.os.Build')

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/meshchat/data/MeshNotificationManager.kt", "r") as f:
    content = f.read()

content = content.replace('import android.app.PendingIntent', 'import android.app.PendingIntent\nimport com.example.meshchat.R')

with open("app/src/main/java/com/example/meshchat/data/MeshNotificationManager.kt", "w") as f:
    f.write(content)
