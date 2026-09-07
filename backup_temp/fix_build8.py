import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace("android {", "android {\n    buildFeatures {\n        buildConfig = true\n    }\n")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
