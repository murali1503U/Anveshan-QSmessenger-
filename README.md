# QSMessenger 🚀

> **Offline, Hardware-Agnostic Post-Quantum Mesh Chat**
> *Developed for the Anveshan Hackathon*

QSMessenger is a 100% offline Android messaging application designed for extreme environments. It integrates military-grade **Post-Quantum Cryptography (PQC)** and an on-device **Quantum Support Vector Machine (QSVM)** to dynamically scale security based on message context, seamlessly routing encrypted payloads over ESP32/ESP8266 and LoRa mesh networks.

---

## 🌟 Key Features

### 1. Post-Quantum Cryptography (PQC) Engine
Powered by `BouncyCastle 1.78.1`, QSMessenger secures communications against future quantum-computer attacks without relying on any external cloud servers.
* **Key Encapsulation:** Uses **ML-KEM-768** to establish secure shared secrets between peers.
* **Digital Signatures:** Uses **ML-DSA-65** to cryptographically sign messages ensuring authenticity.
* **Symmetric Encryption:** Uses **AES-256-GCM** (with HKDF-SHA512) for the actual message payload.
* **Forward Secrecy:** Session keys are auto-ratcheted and rotated every 100 messages or 24 hours via the local Room Database and Android `EncryptedSharedPreferences`.

### 2. AI QSVM (Quantum Support Vector Machine)
An on-device AI module scans outbound messages in real-time, executing zero-latency context analysis.
* **Dynamic Security Scaling:** Automatically classifies messages into Security Levels (1 to 4).
* **Smart Escalation:** If the QSVM detects sensitive data (e.g., GPS coordinates, passwords, or PII), it instantly elevates the security to **Level 4 (Maximum Protection)**, enforcing strict ML-KEM key rotation and ML-DSA signatures. Casual chatter ("hello") remains at Level 1 to conserve battery and bandwidth.

### 3. Hardware-Agnostic Mesh Transport
QSMessenger treats hardware strictly as "dumb pipes". No cryptographic operations occur on the firmware, making the system highly resilient and adaptable.
* **HybridRouter:** Routes messages seamlessly over Wi-Fi TCP, HTTP REST, Bluetooth Classic/BLE, or LoRa HAL.
* **LoRa Optimized:** Custom binary protocol (`QsMessage`) features CRC-16 integrity checks and restricts payloads to max 200 bytes for low-bandwidth LoRa transmission.
* **Mesh Routing:** Supports Global Broadcasts, Group Chats, and Direct 1-on-1 Messaging. Direct messages utilize deterministic channel generation (`direct_Alice_Bob`) to automatically bypass unrelated peers and conserve mesh bandwidth.

### 4. Modern Jetpack Compose UI
* Clean, responsive user interface built entirely with Kotlin and Jetpack Compose.
* Features a `SecurityStatusCard` that translates complex cryptographic telemetry into user-friendly badges ("Maximum", "Enhanced", "Standard") and displays ML-DSA key fingerprints.

---

## 🛠 Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Local Storage:** Room Database, EncryptedSharedPreferences
* **Cryptography:** BouncyCastle, AndroidX Security
* **Hardware Interfacing:** TCP/HTTP (Wi-Fi), Bluetooth SPP/BLE
* **Architecture:** MVVM, Coroutines, Flow

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest Version)
* JDK 17+
* An Android device running Android 8.0 (API 26) or higher.

### Building the Project
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle files to download the BouncyCastle dependencies.
4. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
   *For a production-ready, R8-minified build:*
   ```bash
   ./gradlew assembleRelease
   ```

### Hardware Setup
To test mesh functionality, deploy standard TCP or WebServer pass-through firmware onto an ESP32 or ESP8266. Connect your Android device to the ESP's Wi-Fi or Bluetooth network and utilize the in-app `MeshPairingSheet` to initialize the transport layer.

---

*Securing the future, today.*
