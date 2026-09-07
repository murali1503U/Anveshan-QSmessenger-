import re

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

# Fix the dangling parenthesis and brace
content = re.sub(r'// Radio Transport Selection Sheet\s*\}\s*\)\s*\}', '// Radio Transport Selection Sheet\n', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
