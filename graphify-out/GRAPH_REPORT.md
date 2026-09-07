# Graph Report - amrita hackathon anveshan  (2026-09-06)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 868 nodes · 1314 edges · 123 communities (24 shown, 13 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 19 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- ByteArray
- Message
- PluginManager
- RadioTransport
- LoRaMeshService
- SecureSessionRepository
- ChatChannel
- QsvmClassifier
- SentinelSecurity
- ChatViewModel
- SentinelPacket
- CliLine
- BitChatInspectorSheet.kt
- LoRaHAL
- .loadBitmapOptimized
- GeminiService.kt
- Permission
- SentinelEdgeCases
- PluginHotSwap
- RecoveryType
- PrioritizedTask
- SentinelPerformance.kt
- PluginResult
- AppDatabase
- PluginAudit
- SentinelCompression
- SentinelSecurityHardening
- SentinelExecutor
- SentinelFastCollections
- SentinelMatrix
- PluginIntegrity
- SentinelCryptoUtils
- SentinelDispatchers
- SentinelMessageQueue
- PluginPermissionManager
- ExampleUnitTest
- fix_service.sh

## God Nodes (most connected - your core abstractions)
1. `ChatViewModel` - 62 edges
2. `Message` - 53 edges
3. `QsvmClassifier` - 28 edges
4. `LoRaMeshService` - 26 edges
5. `SecureSessionRepository` - 23 edges
6. `PluginManager` - 21 edges
7. `SecureSessionDao` - 20 edges
8. `SentinelPlugin` - 19 edges
9. `IoTDevice` - 19 edges
10. `ChatChannel` - 19 edges

## Surprising Connections (you probably didn't know these)
- `LoRaMeshService` --calls--> `HybridRouter`  [INFERRED]
  app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt → app/src/main/java/com/example/meshchat/data/HybridRouter.kt
- `QsvmSecurityEngine` --calls--> `QsvmClassifier`  [INFERRED]
  app/src/main/java/com/example/meshchat/ai/QsvmSecurityEngine.kt → app/src/main/java/com/example/meshchat/ai/QsvmClassifier.kt
- `LoRaMeshService` --calls--> `SX1278LoRa`  [INFERRED]
  app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt → app/src/main/java/com/example/meshchat/data/LoRaHAL.kt
- `ChatScreen()` --calls--> `BitChatInspectorSheet()`  [INFERRED]
  app/src/main/java/com/example/meshchat/ui/ChatScreen.kt → app/src/main/java/com/example/meshchat/ui/BitChatInspectorSheet.kt
- `ChatScreen()` --calls--> `SafetyNumberDialog()`  [INFERRED]
  app/src/main/java/com/example/meshchat/ui/ChatScreen.kt → app/src/main/java/com/example/meshchat/ui/SafetyNumberDialog.kt

## Import Cycles
- None detected.

## Communities (123 total, 13 thin omitted)

### Community 0 - "ByteArray"
Cohesion: 0.06
Nodes (8): CpuMathProvider, GpuMathProvider, ByteArray, Context, FloatArray, MathProvider, SentinelMath, SentinelMathEngine

### Community 1 - "Message"
Cohesion: 0.05
Nodes (14): BleConstants, HybridRouter, ByteArray, ByteArray, LoRaPacketData, LoRaQueue, Message, Flow (+6 more)

### Community 2 - "PluginManager"
Cohesion: 0.05
Nodes (19): StateFlow, PluginEntry, PluginManager, ByteArray, MessageProcessor, PluginContext, SecurityProvider, SentinelPlugin (+11 more)

### Community 3 - "RadioTransport"
Cohesion: 0.07
Nodes (31): androidx, Intent, MainActivity, MeshNotificationManager, RadioTransport, BLUETOOTH_DIRECT, HYBRID_AUTO, LORA_MESH (+23 more)

### Community 4 - "LoRaMeshService"
Cohesion: 0.09
Nodes (21): ConnectionState, CONNECTED, CONNECTING, DISCONNECTED, SCANNING, IoTDevice, BluetoothAdapter, Context (+13 more)

### Community 5 - "SecureSessionRepository"
Cohesion: 0.06
Nodes (6): SecureSession, Flow, SecureSessionDao, Flow, SecureSessionRepository, SafetyNumberDialog()

### Community 6 - "ChatChannel"
Cohesion: 0.07
Nodes (10): ChannelType, BROADCAST, DIRECT, GROUP, ChatChannel, ChatChannelDao, Flow, ChatChannelRepository (+2 more)

### Community 7 - "QsvmClassifier"
Cohesion: 0.09
Nodes (8): Complex, QsvmClassifier, QsvmResult, DebugQsvmTest, QsvmClassifierRigorousTest, QsvmExtremeAdversarialTest, DoubleArray, Interpreter

### Community 8 - "SentinelSecurity"
Cohesion: 0.06
Nodes (15): ByteArray, Context, SentinelSecurity, JaninoEngine, LuaEngine, DynamicScriptPlugin, MessageProcessor, MessageProcessor (+7 more)

### Community 9 - "ChatViewModel"
Cohesion: 0.07
Nodes (7): AndroidViewModel, AssistantMessage, ChatViewModel, Context, StateFlow, com, Uri

### Community 10 - "SentinelPacket"
Cohesion: 0.08
Nodes (16): ByteArray, SentinelPacker, ByteArray, PacketType, ACK, FILE, IMAGE, LOCATION (+8 more)

### Community 11 - "CliLine"
Cohesion: 0.11
Nodes (13): QsvmSecurityEngine, SecurityDecision, CliLine, CliLineType, ACCENT, COMMAND, ERROR, OUTPUT (+5 more)

### Community 12 - "BitChatInspectorSheet.kt"
Cohesion: 0.16
Nodes (11): BitChatMeshEngine, BitChatPacket, BitChatSamplePeer, RelayTraceStep, ByteArray, SentinelCryptoConscrypt, BitChatInspectorSheet(), DynamicComputeExplanationCard() (+3 more)

### Community 13 - "LoRaHAL"
Cohesion: 0.12
Nodes (3): ByteArray, LoRaHAL, SX1278LoRa

### Community 14 - ".loadBitmapOptimized"
Cohesion: 0.18
Nodes (8): Bitmap, ByteArray, SentinelImagePacker, ByteBufferPool, Bitmap, BitmapFactory, ByteArray, SentinelMemory

### Community 15 - "GeminiService.kt"
Cohesion: 0.26
Nodes (9): Candidate, Content, GeminiApiService, GeminiSecurityAnalyzer, GenerateContentRequest, GenerateContentResponse, MeshAssistant, Part (+1 more)

### Community 16 - "Permission"
Cohesion: 0.18
Nodes (10): Permission, ACCESS_BLUETOOTH, ACCESS_CAMERA, ACCESS_FILES, ACCESS_GPS, ACCESS_MICROPHONE, ACCESS_NETWORK, RECEIVE_MESSAGES (+2 more)

### Community 17 - "SentinelEdgeCases"
Cohesion: 0.22
Nodes (6): Bitmap, BitmapFactory, BluetoothAdapter, ByteArray, Context, SentinelEdgeCases

### Community 18 - "PluginHotSwap"
Cohesion: 0.29
Nodes (3): Context, PluginHotSwap, FileObserver

### Community 19 - "RecoveryType"
Cohesion: 0.27
Nodes (8): Context, PluginRecovery, RecoveryEvent, RecoveryType, CRASH_RECOVERY, MANUAL_RECOVERY, MEMORY_RECOVERY, TIMEOUT_RECOVERY

### Community 20 - "PrioritizedTask"
Cohesion: 0.27
Nodes (4): PrioritizedTask, SentinelPriorityQueue, Comparable, Runnable

### Community 21 - "SentinelPerformance.kt"
Cohesion: 0.24
Nodes (3): SentinelJankDetector, SentinelMemory, SentinelUiThread

### Community 22 - "PluginResult"
Cohesion: 0.31
Nodes (6): Error, T, PluginContainer, PluginResult, Success, Timeout

### Community 23 - "AppDatabase"
Cohesion: 0.32
Nodes (3): AppDatabase, Context, RoomDatabase

## Knowledge Gaps
- **40 isolated node(s):** `Candidate`, `Error`, `Timeout`, `ACK`, `FILE` (+35 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 325 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Message` connect `Message` to `PluginManager`, `RadioTransport`, `LoRaMeshService`, `ChatChannel`, `SentinelSecurity`, `ChatViewModel`, `CliLine`, `BitChatInspectorSheet.kt`?**
  _High betweenness centrality (0.157) - this node is a cross-community bridge._
- **Why does `ChatViewModel` connect `ChatViewModel` to `Message`, `RadioTransport`, `LoRaMeshService`, `SecureSessionRepository`, `ChatChannel`, `CliLine`, `BitChatInspectorSheet.kt`, `GeminiService.kt`?**
  _High betweenness centrality (0.143) - this node is a cross-community bridge._
- **Why does `LoRaMeshService` connect `LoRaMeshService` to `Message`, `RadioTransport`, `ChatChannel`, `ChatViewModel`, `CliLine`, `LoRaHAL`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `LoRaMeshService` (e.g. with `HybridRouter` and `SX1278LoRa`) actually correct?**
  _`LoRaMeshService` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Candidate`, `Error`, `Timeout` to the rest of the system?**
  _40 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ByteArray` be split into smaller, more focused modules?**
  _Cohesion score 0.059907834101382486 - nodes in this community are weakly interconnected._
- **Should `Message` be split into smaller, more focused modules?**
  _Cohesion score 0.05254237288135593 - nodes in this community are weakly interconnected._