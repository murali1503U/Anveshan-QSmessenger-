# Toyota Production System (TPS) & Kaizen Report: Sentinel App

## 1. Jidoka (Built-in Quality & Automation)
- **Error Stops**: Implemented `runCatching` blocks and `try-catch` structures inside `LoRaMeshService` when establishing Bluetooth connection or verifying PSK. Any mismatch stops the handshake and throws an immediate error to the ViewModel.
- **Andon Indicator**: Added a visual status indicator in `ChannelsListScreen` next to the channel name. It strictly enforces:
  - 🟢 Green: `ConnectionState.CONNECTED`
  - 🟡 Yellow: `ConnectionState.CONNECTING` / `ConnectionState.SCANNING`
  - 🔴 Red: `ConnectionState.DISCONNECTED` or fallback state.

## 2. Poka-Yoke (Mistake-Proofing)
- **Message Validation**: `ChatViewModel` checks `text.isBlank()` and `text.length > 4096` before attempting any transmission. If either fail, the transmission request is silently discarded or a log is emitted (stopping invalid requests before they hit the Bluetooth socket).
- **Mandatory PSK**: The `MeshPairingSheet` enforces `preSharedCodeInput.isNotBlank()`. Also, the `LoRaMeshService.connectToDevice` backend physically rejects connection establishment if the PSK string is blank, avoiding plaintext fallbacks.

## 3. Cryptography Hardening
- Used `SentinelCryptoConscrypt` for robust AES-GCM (128-bit) symmetric encryption on the payload during actual transit.
- Derived the payload key via `SHA-512` applied to the PSK (acting as a basic key derivation function in lieu of PBKDF2/HKDF for speed).
- Created a genuine Challenge-Response protocol upon socket connection:
  - Client sends a 16-byte nonce.
  - Server calculates `HMAC-SHA512` of the nonce using the derived PSK key.
  - Client verifies using `constantTimeEquals` to prevent timing attacks.

## 4. Kaizen & Genchi Genbutsu
- Discarded simulated models entirely (`isDemo`, mock delays).
- Replaced the Thread-based mock Bluetooth service with Coroutines/Flow state managers.
- Enforced clean disconnection boundaries (`cancelDiscovery()` inside `try-catch`) to avoid leaked Bluetooth receivers.
- Ensured all errors are safely logged without leaking the PSK.
