import re

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

# Remove the banner for radio transport selection
banner_regex = r'Surface\(\s*modifier = Modifier\.fillMaxWidth\(\).*?clickable \{ showRadioTransportSheet = true \}.*?Text\(\s*text = when \(selectedRadioTransport\).*?\}\s*\)\s*\}\s*\}'
content = re.sub(banner_regex, '', content, flags=re.DOTALL)

# Remove the showRadioTransportSheet state
content = re.sub(r'var showRadioTransportSheet by remember \{ mutableStateOf\(false\) \}', '', content)

# Remove RadioTransportSelectionSheet invocation
sheet_regex = r'if \(showRadioTransportSheet\) \{.*?onDismiss = \{ showRadioTransportSheet = false \}\s*\)'
content = re.sub(sheet_regex, '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
