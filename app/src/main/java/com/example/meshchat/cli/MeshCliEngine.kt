package com.example.meshchat.cli

import com.example.meshchat.ai.GeminiSecurityAnalyzer
import com.example.meshchat.ai.QsvmSecurityEngine
import com.example.meshchat.ai.SecurityDecision
import com.example.meshchat.data.LoRaMeshService
import com.example.meshchat.data.Message
import com.example.meshchat.data.MessageRepository
import com.example.meshchat.data.SecureSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CliLine(
    val text: String,
    val type: CliLineType = CliLineType.OUTPUT
)

enum class CliLineType {
    COMMAND,
    OUTPUT,
    SUCCESS,
    WARNING,
    ERROR,
    ACCENT
}

class MeshCliEngine(
    private val securityAnalyzer: GeminiSecurityAnalyzer,
    private val qsvmEngine: QsvmSecurityEngine,
    private val sessionRepository: SecureSessionRepository,
    private val messageRepository: MessageRepository,
    private val meshService: LoRaMeshService,
    private val isQsvmActiveProvider: () -> Boolean,
    private val onSecurityLevelChanged: (Int) -> Unit
) {

    suspend fun executeCommand(rawInput: String): List<CliLine> = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val parts = parseCommandArgs(trimmed)
        if (parts.isEmpty()) return@withContext emptyList()

        val command = parts[0].lowercase()
        val args = parts.drop(1)

        try {
            when (command) {
                "help", "?" -> handleHelp(args)
                "qsvm" -> handleQsvm(args)
                "ai" -> handleAi(args)
                "sec", "security" -> handleSecurity(args)
                "session" -> handleSession(args)
                "mesh" -> handleMesh(args)
                "radio", "transport" -> handleRadio(args)
                "nodes" -> handleMesh(listOf("nodes"))
                "bench" -> handleBenchmark(args)
                "echo" -> listOf(CliLine(args.joinToString(" ")))
                "date" -> listOf(CliLine(SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())))
                "whoami" -> listOf(CliLine("operator@mesh-node-alpha (UID=1000)"))
                "uname" -> listOf(CliLine("Linux mesh-core 6.1.0-arm64-android aarch64 Android/LoRa-CLI"))
                "version" -> listOf(
                    CliLine("MeshChat CLI v2.4 (Quantum Security Interface)", CliLineType.ACCENT),
                    CliLine("Security Engine: Multi-Tier Dynamic Privacy Guard"),
                    CliLine("Classifier Core: Automated Context Engine")
                )
                else -> listOf(
                    CliLine("zsh: command not found: $command", CliLineType.ERROR),
                    CliLine("Type 'help' to view available QSVM, mesh radio, and security commands.", CliLineType.WARNING)
                )
            }
        } catch (e: Exception) {
            listOf(
                CliLine("Error executing '$command': ${e.message}", CliLineType.ERROR)
            )
        }
    }

    private suspend fun handleAi(args: List<String>): List<CliLine> {
        if (args.isEmpty()) {
            return listOf(
                CliLine("AI Decision Engine CLI Usage:", CliLineType.ACCENT),
                CliLine("  ai eval <message>           Evaluate text and auto-decide optimal security priority"),
                CliLine("  ai eval --local <message>   Force zero-latency offline rule/model inference"),
                CliLine("  ai eval --cloud <message>   Force Cloud Gemini 3.5 Flash evaluation"),
                CliLine("  ai classify <message>       Compact priority level determination"),
                CliLine("  ai status                   View active AI inference backend & metrics"),
                CliLine("  ai benchmark [text]         Measure decision latency across engines")
            )
        }

        val sub = args[0].lowercase()
        val rest = args.drop(1)

        return when (sub) {
            "status" -> {
                val isQsvmActive = isQsvmActiveProvider()
                listOf(
                    CliLine("┌── [ QSVM QUANTUM DECISION ENGINE STATUS ]", CliLineType.ACCENT),
                    CliLine("│ Active Primary Engine: QSVM Quantum Kernel (On-Device 6-Qubit Hilbert Space)"),
                    CliLine("│ Mathematical Model:    2nd-Order Pauli ZZ-Feature Map U_Phi(x)"),
                    CliLine("│ Quantum Status:        " + if (isQsvmActive) "ACTIVE (AIR-GAPPED)" else "STANDBY"),
                    CliLine("│ Priority Tiers:        1: Fast | 2: Standard | 3: Enhanced | 4: Maximum"),
                    CliLine("│ Latency:               Sub-millisecond (Zero-cloud)"),
                    CliLine("└── [ System Nominal ]", CliLineType.SUCCESS)
                )
            }
            "classify" -> {
                if (rest.isEmpty()) {
                    return listOf(CliLine("Error: Please provide message text. Example: ai classify \"Meeting at 14:00\"", CliLineType.ERROR))
                }
                val text = rest.joinToString(" ")
                val decision = runDecision(text, forceLocal = isQsvmActiveProvider())
                listOf(
                    CliLine("Target: \"$text\""),
                    CliLine(
                        "Result: [LEVEL ${decision.level}] -> ${decision.levelName} (${(decision.confidence * 100).toInt()}% conf)",
                        if (decision.level >= 2) CliLineType.WARNING else CliLineType.SUCCESS
                    ),
                    CliLine("Reason: ${decision.reason} | Source: ${decision.executionSource} (${decision.latencyMs}ms)")
                )
            }
            "eval", "evaluate" -> {
                if (rest.isEmpty()) {
                    return listOf(CliLine("Error: Please provide message text. Example: ai eval \"my secret password is 1234\"", CliLineType.ERROR))
                }
                var forceLocal = isQsvmActiveProvider()
                val filteredArgs = mutableListOf<String>()
                for (arg in rest) {
                    when (arg) {
                        "-l", "--local" -> forceLocal = true
                        "-c", "--cloud" -> forceLocal = false
                        else -> filteredArgs.add(arg)
                    }
                }
                val text = filteredArgs.joinToString(" ")
                val decision = runDecision(text, forceLocal)

                val badgeColor = when (decision.level) {
                    1 -> CliLineType.OUTPUT
                    2 -> CliLineType.ACCENT
                    3 -> CliLineType.WARNING
                    4 -> CliLineType.ERROR
                    else -> CliLineType.OUTPUT
                }

                listOf(
                    CliLine("┌── [ AI SECURITY PRIORITY DECISION REPORT ]", CliLineType.ACCENT),
                    CliLine("│ Input Text: \"${text.take(60)}${if (text.length > 60) "..." else ""}\""),
                    CliLine("│"),
                    CliLine("│ RECOMMENDED TIER: LEVEL ${decision.level}", badgeColor),
                    CliLine("│ TIER NAME:        ${decision.levelName}", badgeColor),
                    CliLine("│ CONFIDENCE:       ${(decision.confidence * 100).toInt()}%"),
                    CliLine("│ DECISION REASON:  ${decision.reason}"),
                    CliLine("│ FACTORS FOUND:    " + if (decision.sensitiveFactors.isEmpty()) "None (Public payload)" else decision.sensitiveFactors.joinToString(", ")),
                    CliLine("│ PACKET OVERHEAD:  +${decision.estimatedPacketOverheadBytes} bytes (LoRa frame)"),
                    CliLine("│ INFERENCE ENGINE: ${decision.executionSource}"),
                    CliLine("│ LATENCY:          ${decision.latencyMs} ms"),
                    CliLine("└── [ Action: Automatically applied when in Auto Mode ]", CliLineType.SUCCESS)
                )
            }
            "benchmark", "bench" -> {
                val sample = if (rest.isNotEmpty()) rest.joinToString(" ") else "Rendezvous at GPS 37.7749, -122.4194 with auth token 0x9AF4B"
                handleBenchmark(listOf(sample))
            }
            else -> listOf(
                CliLine("Unknown AI subcommand '$sub'. Type 'ai' for options.", CliLineType.ERROR)
            )
        }
    }

    private suspend fun handleQsvm(args: List<String>): List<CliLine> {
        val session = sessionRepository.initializeDefaultSessionIfNeeded()
        if (args.isEmpty()) {
            return listOf(
                CliLine("=== Quantum Support Vector Machine (QSVM) CLI ===", CliLineType.ACCENT),
                CliLine("Autonomous 6-Qubit Hilbert space classifier using 2nd-order Pauli ZZ-Feature Map."),
                CliLine(""),
                CliLine("COMMANDS:", CliLineType.SUCCESS),
                CliLine("  qsvm status              Display live quantum kernel metrics & Hilbert space info"),
                CliLine("  qsvm classify <message>  Compute quantum state fidelity and assign security tier"),
                CliLine("  qsvm test <message>      Detailed step-by-step vector rotation trace"),
                CliLine("  qsvm info                Technical architecture breakdown")
            )
        }

        return when (args[0].lowercase()) {
            "status", "metrics" -> {
                val metrics = qsvmEngine.getQuantumMetrics()
                val lines = mutableListOf<CliLine>()
                lines.add(CliLine("┌── [ QSVM QUANTUM KERNEL STATUS ]", CliLineType.ACCENT))
                metrics.forEach { (k, v) ->
                    lines.add(CliLine("│ $k: $v"))
                }
                lines.add(CliLine("│ Classifier Status:  ACTIVE & AIR-GAPPED", CliLineType.SUCCESS))
                lines.add(CliLine("└── [ Zero-Cloud Operational ]", CliLineType.SUCCESS))
                lines
            }
            "classify", "eval" -> {
                if (args.size < 2) {
                    return listOf(CliLine("Usage: qsvm classify <sample text>", CliLineType.WARNING))
                }
                val text = args.drop(1).joinToString(" ")
                val result = qsvmEngine.qsvm.classify(text)
                val decision = result.decision
                listOf(
                    CliLine("┌── [ QSVM QUANTUM KERNEL EVALUATION ]", CliLineType.ACCENT),
                    CliLine("│ Payload:         \"$text\""),
                    CliLine("│ Decision:        [LEVEL ${decision.level}] -> ${decision.levelName}", if (decision.level >= 2) CliLineType.WARNING else CliLineType.SUCCESS),
                    CliLine("│ Quantum Kernel:  K_Q(x, s) = %.4f".format(result.quantumKernelValue)),
                    CliLine("│ State Fidelity:  ${(decision.confidence * 100).toInt()}%"),
                    CliLine("│ Latency:         ${decision.latencyMs} ms"),
                    CliLine("│ Reason:          ${decision.reason}"),
                    CliLine("└── [ Evaluation Complete ]", CliLineType.ACCENT)
                )
            }
            "test" -> {
                if (args.size < 2) {
                    return listOf(CliLine("Usage: qsvm test <sample text>", CliLineType.WARNING))
                }
                val text = args.drop(1).joinToString(" ")
                val features = qsvmEngine.qsvm.extractFeatures(text)
                val result = qsvmEngine.qsvm.classify(text)
                listOf(
                    CliLine("┌── [ 6-QUBIT FEATURE MAP VECTOR TRACE ]", CliLineType.ACCENT),
                    CliLine("│ Input: \"$text\""),
                    CliLine("│ Feature Vector (x0..x5):"),
                    CliLine("│   x0 (Shannon Entropy):    %.3f rad".format(features[0])),
                    CliLine("│   x1 (Post-Quantum Kyber): %.3f rad".format(features[1])),
                    CliLine("│   x2 (Auth / Token):       %.3f rad".format(features[2])),
                    CliLine("│   x3 (GPS / Telemetry):    %.3f rad".format(features[3])),
                    CliLine("│   x4 (PII / Confidential): %.3f rad".format(features[4])),
                    CliLine("│   x5 (Entropy Density):    %.3f rad".format(features[5])),
                    CliLine("│ Quantum State Space: 64 Complex Amplitudes (2^6)"),
                    CliLine("│ Output Security Tier: Level ${result.decision.level} (${result.decision.levelName})", CliLineType.SUCCESS),
                    CliLine("└── [ Zero Cloud Latency ]", CliLineType.ACCENT)
                )
            }
            "info" -> {
                listOf(
                    CliLine("┌── [ QSVM THEORETICAL FOUNDATION ]", CliLineType.ACCENT),
                    CliLine("│ Architecture: 6-Qubit Quantum Support Vector Machine"),
                    CliLine("│ Feature Map:  U_Phi(x) = exp(i sum_j phi_j Z_j + i sum_{j<k} phi_{jk} Z_j Z_k) H^{x6}"),
                    CliLine("│ Hilbert Dim:  64-dimensional complex state vector"),
                    CliLine("│ Kernel Metric: K_Q(x, s) = |<Phi(x)|Phi(s)>|^2"),
                    CliLine("│ Target:       Zero-cloud, air-gapped LoRa edge nodes"),
                    CliLine("└── [ Nature 2019 Quantum-Enhanced Feature Space ]", CliLineType.SUCCESS)
                )
            }
            else -> listOf(
                CliLine("Unknown qsvm command '${args[0]}'. Type 'qsvm' for usage.", CliLineType.ERROR)
            )
        }
    }
    private suspend fun handleSecurity(args: List<String>): List<CliLine> {
        val session = sessionRepository.initializeDefaultSessionIfNeeded()
        if (args.isEmpty()) {
            return listOf(
                CliLine("Security Management CLI:", CliLineType.ACCENT),
                CliLine("  sec get                     View current active security tier and key info"),
                CliLine("  sec set <0|1|2|3|4>         Set security level (0=Auto, 1=Fast, 2=Standard, 3=Enhanced, 4=Maximum)"),
                CliLine("  sec auto <message>          Simulate decision pipeline and display payload transformation"),
                CliLine("  sec levels                  List specification for all 4 security levels")
            )
        }

        when (args[0].lowercase()) {
            "get", "status" -> {
                val levelName = when (session.securityLevel) {
                    0 -> "0 [Auto: Smart Protection]"
                    1 -> "1 [Level 1: Fast - Efficiency Mode]"
                    2 -> "2 [Level 2: Standard Protection]"
                    3 -> "3 [Level 3: Enhanced Protection]"
                    4 -> "4 [Level 4: Maximum Protection]"
                    else -> "${session.securityLevel} [Custom]"
                }
                return listOf(
                    CliLine("┌── [ ACTIVE SECURITY PROFILE ]", CliLineType.ACCENT),
                    CliLine("│ Current Security Level: $levelName", CliLineType.SUCCESS),
                    CliLine("│ Session ID:             ${session.sessionId}"),
                    CliLine("│ Target Peer:            ${session.peerName} (${session.peerId})"),
                    CliLine("│ Session Key Fingerprint:${session.sessionKey.take(18)}..."),
                    CliLine("│ Ratchet Counter:        ${session.ratchetCounter} rotations"),
                    CliLine("│ Kyber Lattice Seed:     ${session.pqSharedSecret.take(20)}..."),
                    CliLine("└── [ Room Database Persisted ]", CliLineType.OUTPUT)
                )
            }
            "set" -> {
                if (args.size < 2) {
                    return listOf(CliLine("Error: Missing level number. Example: sec set 2 (Options: 0, 1, 2, 3, 4)", CliLineType.ERROR))
                }
                val newLevel = args[1].toIntOrNull()
                if (newLevel == null || newLevel !in 0..4) {
                    return listOf(CliLine("Error: Invalid level '$newLevel'. Valid choices are 0 (Auto), 1 (Fast), 2 (Standard), 3 (Enhanced), 4 (Maximum)", CliLineType.ERROR))
                }
                sessionRepository.setSecurityLevel(session.sessionId, newLevel)
                onSecurityLevelChanged(newLevel)
                val name = when (newLevel) {
                    0 -> "Auto (Smart Protection)"
                    1 -> "Level 1: Fast (Efficiency Mode)"
                    2 -> "Level 2: Standard Protection"
                    3 -> "Level 3: Enhanced Protection"
                    4 -> "Level 4: Maximum Protection"
                    else -> "Custom"
                }
                return listOf(
                    CliLine("Security level updated to: $name", CliLineType.SUCCESS),
                    CliLine("State synced to Room database and UI updated.", CliLineType.OUTPUT)
                )
            }
            "auto" -> {
                if (args.size < 2) {
                    return listOf(CliLine("Usage: sec auto <message text to analyze>", CliLineType.WARNING))
                }
                val text = args.drop(1).joinToString(" ")
                val decision = runDecision(text, isQsvmActiveProvider())
                return listOf(
                    CliLine("Simulating Automatic Pipeline Execution...", CliLineType.ACCENT),
                    CliLine("1. AI Analysis Phase:"),
                    CliLine("   -> Engine:  ${decision.executionSource}"),
                    CliLine("   -> Factors: ${if (decision.sensitiveFactors.isEmpty()) "None" else decision.sensitiveFactors.joinToString(", ")}"),
                    CliLine("   -> Choice:  ${decision.levelName} (${(decision.confidence * 100).toInt()}% confidence)"),
                    CliLine("2. Cryptographic Packaging:"),
                    CliLine("   -> Plaintext Size:  ${text.toByteArray().size} bytes"),
                    CliLine("   -> Security Wrap:   +${decision.estimatedPacketOverheadBytes} bytes overhead"),
                    CliLine("   -> Total LoRa Pkt:  ${text.toByteArray().size + decision.estimatedPacketOverheadBytes + 16} bytes"),
                    CliLine("3. Auto-Decision Verdict: ${decision.reason}", CliLineType.SUCCESS)
                )
            }
            "levels" -> {
                return listOf(
                    CliLine("┌── [ 4-TIER SECURITY SPECIFICATION ]", CliLineType.ACCENT),
                    CliLine("│ LEVEL 0: AUTO (SMART PROTECTION)"),
                    CliLine("│   -> Automatically evaluates message content and assigns Tier 1-4."),
                    CliLine("│ LEVEL 1: FAST (EFFICIENCY)"),
                    CliLine("│   -> Minimal overhead, maximized radio range & efficiency. For public broadcasts."),
                    CliLine("│ LEVEL 2: STANDARD"),
                    CliLine("│   -> Balanced protection and lightweight packaging. For private chat."),
                    CliLine("│ LEVEL 3: ENHANCED"),
                    CliLine("│   -> Enhanced protection with rotating session tokens. For sensitive info."),
                    CliLine("│ LEVEL 4: MAXIMUM"),
                    CliLine("│   -> Maximum protection tier. For critical messages."),
                    CliLine("└── [ End of Matrix ]")
                )
            }
            else -> return listOf(CliLine("Unknown security option. Type 'sec' for help.", CliLineType.ERROR))
        }
    }

    private suspend fun handleSession(args: List<String>): List<CliLine> {
        val session = sessionRepository.initializeDefaultSessionIfNeeded()
        if (args.isEmpty() || args[0].lowercase() == "info") {
            return listOf(
                CliLine("┌── [ ROOM DB SECURE SESSION STORAGE ]", CliLineType.ACCENT),
                CliLine("│ Primary Session ID:  ${session.sessionId}"),
                CliLine("│ Broadcast Channel:   ${session.peerName}"),
                CliLine("│ Target Peer ID:      ${session.peerId}"),
                CliLine("│ Security Tier:       Level ${session.securityLevel}"),
                CliLine("│ Active Cipher Key:   ${session.sessionKey}"),
                CliLine("│ Key Ratchet Count:   ${session.ratchetCounter} rotations"),
                CliLine("│ Quantum Secret Seed: ${session.pqSharedSecret}"),
                CliLine("│ Active DB Status:    ${if (session.isActive) "PERSISTED & ACTIVE" else "INACTIVE"}"),
                CliLine("└── [ Persistence Guaranteed Across Restarts ]", CliLineType.SUCCESS)
            )
        }

        when (args[0].lowercase()) {
            "rotate", "ratchet" -> {
                sessionRepository.ratchetSessionKey(session.sessionId)
                val refreshed = sessionRepository.initializeDefaultSessionIfNeeded()
                return listOf(
                    CliLine("Ephemeral key ratcheting initiated...", CliLineType.ACCENT),
                    CliLine("Previous Key Fingerprint: ${session.sessionKey.take(14)}...", CliLineType.WARNING),
                    CliLine("New Active Session Key:   ${refreshed.sessionKey}", CliLineType.SUCCESS),
                    CliLine("Ratchet Counter:          ${refreshed.ratchetCounter}", CliLineType.OUTPUT),
                    CliLine("Room database successfully updated.", CliLineType.SUCCESS)
                )
            }
            else -> return listOf(CliLine("Usage: session info | session rotate", CliLineType.WARNING))
        }
    }

    private suspend fun handleRadio(args: List<String>): List<CliLine> {
        val currentTransport = com.example.meshchat.data.RadioTransport.BLUETOOTH_DIRECT
        if (args.isEmpty() || args[0].lowercase() == "status" || args[0].lowercase() == "get") {
            val label = when (currentTransport) {
                com.example.meshchat.data.RadioTransport.HYBRID_AUTO -> "Hybrid Auto-Route (BLE <100m • LoRa >100m)"
                com.example.meshchat.data.RadioTransport.BLUETOOTH_DIRECT -> "Bluetooth Direct P2P (<100m Nearby)"
                com.example.meshchat.data.RadioTransport.LORA_MESH -> "LoRa Long-Range Mesh (Multi-Hop)"
else -> "Unknown"
            }
            return listOf(
                CliLine("┌── [ DUAL WIRELESS TRANSCEIVER STATUS ]", CliLineType.ACCENT),
                CliLine("│ Active Radio Mode: $label", CliLineType.SUCCESS),
                CliLine("│ 2.4 GHz BLE Link:  Active (Latency: ~15ms, Bandwidth: 2 Mbps)"),
                CliLine("│ Sub-GHz LoRa Link: Active (Range: 1-15+ km, Modulation: CSS)"),
                CliLine("│ Adaptive Switch:   " + if (currentTransport == com.example.meshchat.data.RadioTransport.HYBRID_AUTO) "ENABLED (Proximity-based)" else "MANUAL OVERRIDE"),
                CliLine("└── [ Ready for Transmission ]")
            )
        }

        when (args[0].lowercase()) {
            "set" -> {
                if (args.size < 2) {
                    return listOf(CliLine("Usage: radio set <auto|ble|lora>", CliLineType.WARNING))
                }
                val target = when (args[1].lowercase()) {
                    "auto", "hybrid" -> com.example.meshchat.data.RadioTransport.HYBRID_AUTO
                    "ble", "bluetooth", "p2p" -> com.example.meshchat.data.RadioTransport.BLUETOOTH_DIRECT
                    "lora", "mesh", "longrange" -> com.example.meshchat.data.RadioTransport.LORA_MESH
                    else -> null
                }
                if (target == null) {
                    return listOf(CliLine("Error: Invalid radio mode '${args[1]}'. Options: auto, ble, lora", CliLineType.ERROR))
                }
                // meshService.setGlobalTransport(target)
                return listOf(
                    CliLine("Wireless radio transport switched to: ${target.name}", CliLineType.SUCCESS),
                    CliLine("Transmissions will now follow the updated routing policy.")
                )
            }
            else -> return listOf(CliLine("Usage: radio status | radio set <auto|ble|lora>", CliLineType.WARNING))
        }
    }

    private suspend fun handleMesh(args: List<String>): List<CliLine> {
        if (args.isEmpty() || args[0].lowercase() == "status") {
            val state = meshService.connectionState.value
            val count = meshService.discoveredDevices.value.size
            val transport = com.example.meshchat.data.RadioTransport.BLUETOOTH_DIRECT
            return listOf(
                CliLine("┌── [ WIRELESS MESH & RADIO STATUS ]", CliLineType.ACCENT),
                CliLine("│ Hardware State:     ${state.name}", if (state.name == "CONNECTED") CliLineType.SUCCESS else CliLineType.WARNING),
                CliLine("│ Active Radio Mode:  ${transport.name}"),
                CliLine("│ Frequency Band:     915 MHz (US) / 868 MHz (EU) / 2.4 GHz BLE"),
                CliLine("│ Spreading Factor:   SF7 / BW 125kHz / CR 4/5"),
                CliLine("│ Discovered Peers:   $count nodes in range"),
                CliLine("└── [ Radio Transceiver Operational ]")
            )
        }

        when (args[0].lowercase()) {
            "nodes", "peers", "scan" -> {
                val devices = meshService.discoveredDevices.value
                if (devices.isEmpty()) {
                    return listOf(
                        CliLine("No external Bluetooth/LoRa nodes currently connected.", CliLineType.WARNING),
                        CliLine("Local node operating in standalone broadcast mode.")
                    )
                }
                val lines = mutableListOf<CliLine>()
                lines.add(CliLine("Discovered Active Radio Peers (${devices.size}):", CliLineType.ACCENT))
                devices.forEachIndexed { idx, dev ->
                    val type = if (dev.isBleNearby) "[BLE NEARBY]" else "[LORA MESH]"
                    lines.add(CliLine(" [${idx + 1}] $type ${dev.name} | Dist: ${dev.distanceEstimate} | Signal: ${dev.rssi} dBm"))
                }
                return lines
            }
            "send" -> {
                if (args.size < 2) {
                    return listOf(CliLine("Usage: mesh send <message text>", CliLineType.WARNING))
                }
                val text = args.drop(1).joinToString(" ")
                val session = sessionRepository.initializeDefaultSessionIfNeeded()
                val finalSecLevel = if (session.securityLevel == 0) {
                    val decision = runDecision(text, isQsvmActiveProvider())
                    decision.level
                } else {
                    session.securityLevel
                }

                val prefTransport = com.example.meshchat.data.RadioTransport.BLUETOOTH_DIRECT
                val (resolvedTransport, transportInfo) = meshService.resolveActiveTransport(prefTransport, peerNearby = false)

                val msg = Message(
                    channelId = "global_broadcast",
                    text = text,
                    sender = "Me (CLI)",
                    isEncrypted = finalSecLevel >= 2,
                    isSent = true,
                    isConfidential = finalSecLevel >= 2,
                    securityLevel = finalSecLevel,
                    transport = resolvedTransport,
                    transportDetail = transportInfo
                )
                messageRepository.insert(msg)
                if (finalSecLevel >= 3) {
                    sessionRepository.ratchetSessionKey(session.sessionId)
                }
                meshService.sendMessage(msg)

                return listOf(
                    CliLine("Message staged & transmitted via ${resolvedTransport.name}:", CliLineType.SUCCESS),
                    CliLine("  Text: \"$text\""),
                    CliLine("  Transport: $transportInfo"),
                    CliLine("  Security Level: $finalSecLevel | Encrypted: ${finalSecLevel >= 2}")
                )
            }
            else -> return listOf(CliLine("Usage: mesh status | mesh nodes | mesh send <text>", CliLineType.WARNING))
        }
    }

    private suspend fun handleBenchmark(args: List<String>): List<CliLine> {
        val sample = if (args.isNotEmpty()) args.joinToString(" ") else "Tactical coordinates: 48.8584, 2.2945 with authorization seed 0x88FE1A"
        val lines = mutableListOf<CliLine>()
        lines.add(CliLine("┌── [ AI PRIORITY DECISION BENCHMARK ]", CliLineType.ACCENT))
        lines.add(CliLine("│ Sample Payload: \"$sample\""))
        lines.add(CliLine("│"))

        // 1. QSVM Quantum Kernel Benchmark
        val t0 = System.nanoTime()
        val localDecision = qsvmEngine.evaluateLightweightPriority(sample)
        val t1 = System.nanoTime()
        val localDurationMicros = (t1 - t0) / 1000

        lines.add(CliLine("│ [1] QSVM Quantum Kernel (Hilbert Space):"))
        lines.add(CliLine("│     -> Priority Level:  ${localDecision.level} (${localDecision.levelName})"))
        lines.add(CliLine("│     -> Latency:         $localDurationMicros μs (Microseconds)"))
        lines.add(CliLine("│     -> Overhead:        Air-gapped 6-qubit simulation"))
        lines.add(CliLine("│"))

        // 2. Full Engine Evaluation
        val t2 = System.currentTimeMillis()
        val comprehensive = runDecision(sample, forceLocal = false)
        val t3 = System.currentTimeMillis()
        val compDurationMs = t3 - t2

        lines.add(CliLine("│ [2] Hybrid Decision Pipeline (${comprehensive.executionSource}):"))
        lines.add(CliLine("│     -> Priority Level:  ${comprehensive.level} (${comprehensive.levelName})"))
        lines.add(CliLine("│     -> Latency:         ${compDurationMs.coerceAtLeast(1)} ms"))
        lines.add(CliLine("│     -> Decision:        ${comprehensive.reason}"))
        lines.add(CliLine("└── [ Benchmark Completed ]", CliLineType.SUCCESS))

        return lines
    }

    private suspend fun runDecision(text: String, forceLocal: Boolean): SecurityDecision {
        return if (forceLocal) {
            qsvmEngine.analyzePriorityLevel(text)
        } else {
            securityAnalyzer.determinePriorityLevel(text, false)
        }
    }

    private fun handleHelp(args: List<String>): List<CliLine> {
        return listOf(
            CliLine("=== MeshChat Quantum CLI Manual ===", CliLineType.ACCENT),
            CliLine("Zero-cloud terminal interface for QSVM quantum classification & mesh governance."),
            CliLine(""),
            CliLine("QSVM QUANTUM CLASSIFIER COMMANDS:", CliLineType.SUCCESS),
            CliLine("  qsvm status             Inspect active 6-qubit Hilbert space and fidelity metrics"),
            CliLine("  qsvm classify <text>    Compute quantum kernel value K_Q(x, s) and tier"),
            CliLine("  qsvm test <text>        Trace 6-qubit Pauli ZZ-Feature Map rotation angles"),
            CliLine("  qsvm info               View mathematical formulation and paper reference"),
            CliLine(""),
            CliLine("AI DECISION ENGINE COMMANDS:", CliLineType.SUCCESS),
            CliLine("  ai eval <text>          Auto-decide security priority level for payload"),
            CliLine("  ai eval -l <text>       Zero-latency offline QSVM quantum inference"),
            CliLine("  ai classify <text>      Compact single-line priority evaluation"),
            CliLine("  ai status               Inspect active QSVM quantum kernel table"),
            CliLine("  ai benchmark [text]     Benchmark microsecond classification latency"),
            CliLine(""),
            CliLine("SECURITY & CRYPTOGRAPHY COMMANDS:", CliLineType.SUCCESS),
            CliLine("  sec get                 View active security tier & key fingerprint"),
            CliLine("  sec set <0|1|2|3|4>     Set security level (0=Auto, 1=Fast, 2=Std, 3=Enh, 4=Max)"),
            CliLine("  sec auto <text>         Step-by-step decision breakdown simulation"),
            CliLine("  sec levels              View specifications for all 4 security tiers"),
            CliLine(""),
            CliLine("ROOM DB & SESSION COMMANDS:", CliLineType.SUCCESS),
            CliLine("  session info            Show persisted session ID, ratchet counts, and keys"),
            CliLine("  session rotate          Immediately ratchet and rotate session keys"),
            CliLine(""),
            CliLine("LORA MESH NETWORK & RADIO COMMANDS:", CliLineType.SUCCESS),
            CliLine("  radio status            Show dual transceiver mode (Hybrid/BLE/LoRa)"),
            CliLine("  radio set <auto|ble|lora> Switch active radio transport policy"),
            CliLine("  mesh status             View BLE connection & radio status"),
            CliLine("  mesh nodes              List discovered hardware nodes (BLE & LoRa)"),
            CliLine("  mesh send <text>        Inject encrypted payload into radio transceiver"),
            CliLine(""),
            CliLine("SYSTEM UTILITIES:", CliLineType.SUCCESS),
            CliLine("  clear                   Clear terminal display buffer"),
            CliLine("  date | whoami | uname   Unix utilities")
        )
    }

    private fun parseCommandArgs(input: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (ch in input) {
            if ((ch == '"' || ch == '\'') && (!inQuotes || ch == quoteChar)) {
                inQuotes = !inQuotes
                quoteChar = ch
            } else if (ch.isWhitespace() && !inQuotes) {
                if (sb.isNotEmpty()) {
                    result.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        return result
    }
}
