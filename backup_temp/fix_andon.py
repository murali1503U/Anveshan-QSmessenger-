with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "Color(0xFFFFD600)" in line:
        continue
    if "else MaterialTheme.colorScheme.outline" in line:
        continue
    if "                                    )" in line and "Spacer(Modifier.width(6.dp))" in new_lines[-1]:
        continue # skip the extra paren
    new_lines.append(line)
    
with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.writelines(new_lines)
