import re

with open("app/src/main/java/com/example/meshchat/data/SentinelPacket.kt", "r") as f:
    content = f.read()

target = """            dos.write(payload)
            dos.toByteArray()
        }
    }"""
repl = """            dos.write(payload)
            bos.toByteArray()
        }
    }"""
content = content.replace(target, repl)

with open("app/src/main/java/com/example/meshchat/data/SentinelPacket.kt", "w") as f:
    f.write(content)

