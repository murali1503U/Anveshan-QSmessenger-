import re

with open("app/src/main/java/com/example/meshchat/data/MeshNotificationManager.kt", "r") as f:
    content = f.read()

content = content.replace("import android.R", "// import android.R")
content = content.replace("import com.example.meshchat.R", "import com.aistudio.meshchat.kxmpzq.R")

with open("app/src/main/java/com/example/meshchat/data/MeshNotificationManager.kt", "w") as f:
    f.write(content)
