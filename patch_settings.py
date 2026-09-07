import re

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "r") as f:
    content = f.read()

# Remove the notification test card
content = re.sub(r'\s*// Notifications Status & Test Card[\s\S]*?(?=// Re-run Setup Wizard Option)', '', content)

# Remove the re-run setup wizard option
content = re.sub(r'\s*// Re-run Setup Wizard Option[\s\S]*?(?=// Danger Zone)', '', content)

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "w") as f:
    f.write(content)
