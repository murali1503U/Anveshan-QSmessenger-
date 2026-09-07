import re

with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()
if "kotlin-android =" not in content:
    content = content.replace("[plugins]", '[plugins]\nkotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }')
with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)

with open("build.gradle.kts", "r") as f:
    content = f.read()
content = re.sub(r'id\("org.jetbrains.kotlin.android"\).*?apply false\n?', '', content)
if "alias(libs.plugins.kotlin.android)" not in content:
    content = content.replace("plugins {", 'plugins {\n  alias(libs.plugins.kotlin.android) apply false')
with open("build.gradle.kts", "w") as f:
    f.write(content)

with open("app/build.gradle.kts", "r") as f:
    content = f.read()
content = content.replace('id("org.jetbrains.kotlin.android")', 'alias(libs.plugins.kotlin.android)')
with open("app/build.gradle.kts", "w") as f:
    f.write(content)
