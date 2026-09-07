with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if skip:
        skip = False
        continue
    if "initialValue = emptyList()" in line and ")" in lines[i+1]:
        # we have )
        new_lines.append(line.replace(")", "").strip() + "\n")
        skip = True
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.writelines(new_lines)
