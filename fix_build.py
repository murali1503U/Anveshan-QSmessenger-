import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('id("org.jetbrains.kotlin.android") version "1.9.22"', 'id("org.jetbrains.kotlin.android")')
content = content.replace('id("com.google.devtools.ksp") version "1.9.22-1.0.17"', 'id("com.google.devtools.ksp")')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
