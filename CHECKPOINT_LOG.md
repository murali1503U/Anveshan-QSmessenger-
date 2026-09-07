# Sentinel App - Development & Checkpoint Log

This file tracks all modifications, architectural decisions, and feature completions for the Sentinel (Amrita Hackathon Anveshan) project to ensure a clear, traceable history.

## 🚩 Checkpoint 0: Context & Architecture Alignment
**Date:** 2026-09-06
**Status:** Completed
**Details:**
- Completely reviewed `senthinel app.md`, `README.md`, `KAIZEN.md`, and the codebase.
- **Architectural Lock-in:** Unified Phone-to-ESP transport strictly over **Wi-Fi TCP (Port 8266)**. Bluetooth/BLE is discarded for ESP-12E compatibility.
- **ESP Role Definition:** ESP32 and ESP-12E will both act as local Wi-Fi APs + TCP servers, routing packets over LoRa SX1276.
- **Subagent Invoked:** Spawend a dedicated subagent to research, install, and configure `graphiti`/`graphify` to map the codebase into a searchable knowledge graph.

## 🚩 Checkpoint 1: Physical Device Connected & Verified
**Date:** 2026-09-06
**Status:** Completed
**Device Info:**
- **Model**: Realme (RMX3710)
- **Android OS**: Android 15 (API Level 35)
- **Serial**: `Z56TB6NZ59OFFY9P`
- **State**: Authorized & Active over ADB USB
- **Capabilities Verified**: ADB shell execution, live logcat streaming, APK deployment, layout tree inspection, UI automation.

## 🚩 Checkpoint 2: Wi-Fi TCP Transport, Binary Framing & Arduino/ESP Firmware
**Date:** 2026-09-06
**Status:** Completed
**Details:**
- **Binary Codec (`SentinelBinaryCodec.kt`)**: 0xA5 framing, CRC16-CCITT integrity, HMAC-SHA256 PSK challenge-response authentication, multi-board identification (`0x01` ESP32, `0x02` ESP-12E, `0x03` Arduino LoRa).
- **Wi-Fi TCP Client (`WifiTcpTransport.kt`)**: Kotlin Coroutines async socket engine with keepalive ping (10s), round-trip latency tracking, automatic reconnect, and frame reception.
- **Node Scanner (`WifiNodeScanner.kt`)**: Subnet discovery on port 8266.
- **Hybrid Router (`HybridRouter.kt`)**: Seamless prioritization of `WIFI_TCP_NODE` transport over Bluetooth/LoRa.
- **Firmware Delivery**:
  - `esp_sentinel_wifi.ino`: Phase 1 SoftAP (`SENTINEL_ESP32` / `SENTINEL_ESP12`) + TCP Server with HMAC auth and binary packet handling.
  - `esp_sentinel_wifi_lora.ino`: Phase 2 Wi-Fi TCP to SX1276 LoRa transceiver bridge.

## 🚩 Checkpoint 3: Realme Physical Device UI & Onboarding Live Verification
**Date:** 2026-09-06
**Status:** Completed
**Details:**
- **Target Device**: Realme RMX3710 (`Z56TB6NZ59OFFY9P`), Android 15.
- **Build & Run**: Fixed compileSdk 36 and TFLite namespace compatibility; compiled with Gradle 9.3.1.
- **Deployment**: App deployed and launched successfully.
- **Live Verification**: Completed permission onboarding; verified Main Channels screen and Hardware Transceiver Pairing sheet with dual tabs (Wi-Fi TCP Node & Bluetooth Classic/BLE), Quick Connect (`192.168.4.1:8266`), PSK authentication input, and real-time telemetry.

## 🚩 Checkpoint 4: QSVM Quantum Security, Hardware Cryptography & GPU Acceleration
**Date:** 2026-09-06
**Status:** Completed
**Details:**
- **QSVM 6-Qubit Hilbert Space Engine (`QsvmClassifier.kt`)**:
  - Full Pauli ZZ-Feature Map (2nd order) mapped into 64-dimensional Hilbert space.
  - Quantum State Normalization ($\sum |c_i|^2 = 1.0$), Hermitian symmetric quantum kernel fidelity $K_Q(x, s)$.
  - Multi-tier dynamic classification (Level 1 Fast, Level 2 Standard E2EE, Level 3 Forward Secrecy Ratchets, Level 4 Post-Quantum Lattice).
  - Robust adversarial normalization (word-boundary tokenization, leetspeak resolution, zero-width space stripping, false-positive context suppression for harmless science discussions).
- **GPU Hardware Acceleration**:
  - TensorFlow Lite GPU Delegate (`GpuDelegate`) dynamic capability detection (`CompatibilityList`) with automatic CPU fallback.
  - Verified live on connected Realme (`RMX3710`): `QSVM: Hardware Acceleration: GPU Delegate Enabled`.
- **Hardware Cryptography (`SentinelCryptoConscrypt.kt`, `BitChatMeshEngine.kt`)**:
  - Conscrypt OpenSSL-backed AES-256-GCM AEAD encryption/decryption with 128-bit authentication tags and 96-bit IVs.
  - Tamper resistance: GCM AEAD authentication tag validation strictly rejects modified ciphertext.
  - HMAC-SHA256 and HMAC-SHA512 integrity and Double-Ratchet key derivation.
  - Constant-time byte comparison (`SentinelCryptoUtils.constantTimeEquals`) to eliminate side-channel timing attacks.
  - Sentinel binary packet CRC16-CCITT and HMAC-SHA256 challenge-response handshake verification.
- **Verification & Testing**:
  - Created [`QsvmAndCryptoComprehensiveTest.kt`](file:///c:/Users/Sathwik/Documents/amrita%20hackathon%20anveshan/app/src/test/java/com/example/meshchat/QsvmAndCryptoComprehensiveTest.kt).
  - Executed `./gradlew test` -> **100% Passed (45/45 tests green)**.
  - Deployed updated APK to Realme device and verified live GPU execution.

## 🚩 Checkpoint 5: Wi-Fi Gateway Auto-Detection, Dual HTTP/TCP Bridge & Bluetooth Enforcement
**Date:** 2026-09-06
**Status:** Completed
**Details:**
- **Wi-Fi Gateway Auto-Detection (`WifiNodeScanner.kt`)**:
  - Implemented `getDhcpGatewayIp(context)` utilizing Android 10+ `LinkProperties` default IPv4 route filtering and fallback to `WifiManager.dhcpInfo`.
  - Automated candidate gateway probing: `[dhcpGateway, 192.168.4.1, 192.168.10.1, 192.168.20.1]` across HTTP Port 80 and TCP Binary Port 8266.
- **HTTP REST Bridge Transport (`HttpNodeTransport.kt`)**:
  - Stdlib `HttpURLConnection` non-blocking client on `Dispatchers.IO` (zero external dependencies).
  - Outgoing packets: `GET http://<ip>:<port>/send?msg=<urlEncodedPayload>`.
  - Incoming packets: Background polling loop every 800ms on `GET http://<ip>:<port>/message` with deduplication and emission to `receivedMessages` Flow.
  - Telemetry: Real-time RTT latency measurement, Tx/Rx packet counters, and error recovery.
- **Bluetooth Transport & ESP8266 Blocking (`RealBluetoothManager.kt`)**:
  - Mandatory Pre-Shared Key (PSK) challenge-response enforcement.
  - Device identification filter: explicitly rejects connection attempts to ESP8266 targets (which have no Bluetooth hardware).
- **Hybrid Routing Integration (`HybridRouter.kt`, `LoRaMeshService.kt`, `ChatViewModel.kt`)**:
  - Multi-tier priority: `ESP HTTP REST Bridge` / `ESP TCP Socket` -> `LoRa HAL` -> `Bluetooth Direct` -> `Store-and-Forward`.
  - Outgoing messages pass through QSVM Security Engine and AES-GCM encryption before dispatch.
- **UI Updates (`MeshPairingSheet.kt`)**:
  - Auto-Detect Wi-Fi Gateway with one-tap scan & connect button.
  - Quick Presets: `192.168.4.1:80`, `ESP32 (192.168.10.1:80)`, `ESP8266 (192.168.20.1:80)`.
  - Real-time connected telemetry cards for HTTP REST bridge and TCP socket.
- **Verification & Testing**:
  - Created [`WifiAndHttpTransportTest.kt`](file:///c:/Users/Sathwik/Documents/amrita%20hackathon%20anveshan/app/src/test/java/com/example/meshchat/WifiAndHttpTransportTest.kt).
  - Executed `./gradlew test` -> **100% Passed (50/50 tests green)**.
  - Built debug APK and installed live onto Realme RMX3710 (`Z56TB6NZ59OFFY9P`).
  - Verified UI rendering and gateway scanning via live logcat and ADB screencap.

---
*(Future checkpoints will be appended below as we execute features.)*
