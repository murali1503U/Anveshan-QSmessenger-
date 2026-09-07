import re

with open("build.gradle.kts", "r") as f:
    content = f.read()

if "alias(libs.plugins.kotlin.android)" not in content:
    content = content.replace("plugins {", 'plugins {\n  id("org.jetbrains.kotlin.android") version "1.9.22" apply false')

with open("build.gradle.kts", "w") as f:
    f.write(content)
