import re

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "r") as f:
    content = f.read()

content = re.sub(r'\s*// Re-run Setup Wizard Option[\s\S]*?Text\("Re-run Setup Wizard", fontWeight = FontWeight.SemiBold\)[\s\S]*?\}\n\s*\)', '', content)

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "w") as f:
    f.write(content)
