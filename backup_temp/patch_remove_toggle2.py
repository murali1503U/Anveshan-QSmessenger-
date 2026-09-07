import re

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "r") as f:
    content = f.read()

# Remove RadioTransportSelectionSheet invocation
sheet_regex = r'if \(showRadioSelection\) \{.*?onDismiss = \{ showRadioSelection = false \}\s*\)'
content = re.sub(sheet_regex, '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "w") as f:
    f.write(content)
