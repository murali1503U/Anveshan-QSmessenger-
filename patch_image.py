import re

with open("app/src/main/java/com/example/meshchat/data/SentinelImagePacker.kt", "r") as f:
    content = f.read()

target = "object SentinelImagePacker {"
repl = """object SentinelImagePacker {
    
    // Decompression via SentinelMemory for aggressive 50% RAM reduction (RGB_565)
    fun decompressImage(data: ByteArray) = com.example.meshchat.perf.SentinelMemory.loadBitmapOptimized(data)
"""
content = content.replace(target, repl)

with open("app/src/main/java/com/example/meshchat/data/SentinelImagePacker.kt", "w") as f:
    f.write(content)

