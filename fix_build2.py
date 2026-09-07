import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('id("org.jetbrains.kotlin.android") version "2.2.10"', 'id("org.jetbrains.kotlin.android")')
content = content.replace('id("com.google.devtools.ksp") version "2.3.5"', 'alias(libs.plugins.google.devtools.ksp)')

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
