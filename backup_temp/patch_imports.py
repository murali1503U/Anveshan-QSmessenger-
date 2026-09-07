import re

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.material.icons.filled.Tune", "import androidx.compose.material.icons.filled.Tune\nimport androidx.compose.material.icons.filled.Extension\nimport androidx.compose.material.icons.filled.ChevronRight")

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "w") as f:
    f.write(content)
