import re

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "r") as f:
    content = f.read()

# Add fast math methods to QsvmClassifier
fast_math = """    private fun fastSin(theta: Double): Double {
        var normalized = theta % (2 * Math.PI)
        if (normalized < 0) normalized += 2 * Math.PI
        val idx = (normalized * 1000).toInt().coerceIn(0, 6283)
        return com.example.meshchat.perf.SentinelLUT.SIN[idx].toDouble()
    }

    private fun fastCos(theta: Double): Double {
        var normalized = theta % (2 * Math.PI)
        if (normalized < 0) normalized += 2 * Math.PI
        val idx = (normalized * 1000).toInt().coerceIn(0, 6283)
        return com.example.meshchat.perf.SentinelLUT.COS[idx].toDouble()
    }"""

content = content.replace("class QsvmClassifier {", "class QsvmClassifier {\n" + fast_math)
content = content.replace("val cosHalf = cos(theta / 2.0)", "val cosHalf = fastCos(theta / 2.0)")
content = content.replace("val sinHalf = sin(theta / 2.0)", "val sinHalf = fastSin(theta / 2.0)")
content = content.replace("val cosHalf = cos(phi / 2.0)", "val cosHalf = fastCos(phi / 2.0)")
content = content.replace("val sinHalf = sin(phi / 2.0)", "val sinHalf = fastSin(phi / 2.0)")

with open("app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt", "w") as f:
    f.write(content)

