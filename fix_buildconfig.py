import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

if "buildConfig = true" not in content:
    content = content.replace("buildFeatures {", "buildFeatures {\n        buildConfig = true")

if "alias(libs.plugins.kotlin.android)" not in content:
    content = content.replace("plugins {", "plugins {\n    alias(libs.plugins.kotlin.android)")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
