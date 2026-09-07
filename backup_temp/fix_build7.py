import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

deps = """
    // Play Services
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
"""

content = content.replace("implementation(\"com.google.mlkit:barcode-scanning:17.2.0\")", deps)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
