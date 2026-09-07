
with open('unified_esp_mesh/unified_esp_mesh.ino', 'r') as f:
    code = f.read()
code = code.replace('server.setNoDelay(true);\n}', 'server.setNoDelay(true);\n  Serial.println(F("[SENTINEL] Ready! AP Started. Listening on port 8266."));\n}')
with open('unified_esp_mesh/unified_esp_mesh.ino', 'w') as f:
    f.write(code)

