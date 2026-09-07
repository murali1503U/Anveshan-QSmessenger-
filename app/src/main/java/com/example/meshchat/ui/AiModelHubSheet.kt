package com.example.meshchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshchat.ai.QsvmClassifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelHubSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qsvm = remember { QsvmClassifier() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "QSVM Guard",
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "QSVM Quantum Guard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00C853),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Quantum Support Vector Machine • 100% Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Active QSVM Quantum Architecture Card
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Quantum Machine Learning Core",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            "All mesh messages are analyzed instantly on-device using a Quantum Support Vector Machine (QSVM). Classical payload features are mapped into a 64-dimensional Hilbert space via a 2nd-order Pauli ZZ-Feature Map.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(10.dp))

                        // Specs Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecItem(label = "Quantum Registers", value = "6 Qubits (64 states)")
                            SpecItem(label = "Feature Map", value = "Pauli ZZ-Expansion")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecItem(label = "Decision Kernel", value = "Fidelity |⟨Φ(x)|Φ(s)⟩|²")
                            SpecItem(label = "Execution Latency", value = "< 2 ms (Zero Cloud)")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quantum Security Tiers Card
                Text(
                    "Autonomous Security Tiers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                TierRow(
                    level = 1,
                    title = "Level 1: Fast (Efficiency)",
                    description = "Casual chatter. Optimized compute, 0-byte packet overhead.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                TierRow(
                    level = 2,
                    title = "Level 2: Standard",
                    description = "Protected text, attachments, emails, contact details.",
                    color = Color(0xFF2E7D32)
                )
                Spacer(Modifier.height(6.dp))
                TierRow(
                    level = 3,
                    title = "Level 3: Enhanced",
                    description = "Credentials, passcodes, location coordinates, sensitive tokens.",
                    color = Color(0xFFEF6C00)
                )
                Spacer(Modifier.height(6.dp))
                TierRow(
                    level = 4,
                    title = "Level 4: Maximum",
                    description = "Critical data, high-assurance protection.",
                    color = Color(0xFF8E24AA)
                )

                Spacer(Modifier.height(20.dp))

                // Interactive QSVM Live Test Sandbox
                Text(
                    "Interactive QSVM Kernel Sandbox",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Test how the quantum kernel maps and classifies arbitrary messages in real-time:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                var sandboxInput by remember { mutableStateOf("Meeting coordinates lat: 37.7749 lon: -122.4194 with access token 0x88FFAA") }
                var qsvmResult by remember { mutableStateOf<QsvmClassifier.QsvmResult?>(null) }

                OutlinedTextField(
                    value = sandboxInput,
                    onValueChange = { sandboxInput = it },
                    placeholder = { Text("Type any payload to test QSVM...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qsvm_sandbox_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { sandboxInput = "Hey! How is the radio signal over there?" },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Casual", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { sandboxInput = "Private memo to contact user@mesh.org regarding account numbers" },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PII / Memo", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { sandboxInput = "Deploying emergency post-quantum kyber lattice master_key defense" },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quantum", fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        qsvmResult = qsvm.classify(sandboxInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qsvm_evaluate_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Evaluate with QSVM Quantum Kernel")
                }

                // Sandbox Result Display
                if (qsvmResult != null) {
                    val res = qsvmResult!!
                    val d = res.decision
                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when (d.level) {
                            1 -> MaterialTheme.colorScheme.surfaceVariant
                            2 -> Color(0xFF1B5E20).copy(alpha = 0.18f)
                            3 -> Color(0xFFE65100).copy(alpha = 0.18f)
                            4 -> Color(0xFF4A148C).copy(alpha = 0.20f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (d.level) {
                                1 -> MaterialTheme.colorScheme.outlineVariant
                                2 -> Color(0xFF00C853)
                                3 -> Color(0xFFFF9800)
                                4 -> Color(0xFFAB47BC)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = when (d.level) {
                                            1 -> MaterialTheme.colorScheme.onSurfaceVariant
                                            2 -> Color(0xFF00C853)
                                            3 -> Color(0xFFFF9800)
                                            4 -> Color(0xFFAB47BC)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        d.levelName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = when (d.level) {
                                            1 -> MaterialTheme.colorScheme.onSurface
                                            2 -> Color(0xFF2E7D32)
                                            3 -> Color(0xFFEF6C00)
                                            4 -> Color(0xFF8E24AA)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                Text("${d.latencyMs} ms", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(d.reason, style = MaterialTheme.typography.bodySmall)

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))

                            // Quantum Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Quantum Kernel Confidence: ${(d.confidence * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Overhead: +${d.estimatedPacketOverheadBytes}B",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (d.sensitiveFactors.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Salient Factors: ${d.sensitiveFactors.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TierRow(
    level: Int,
    title: String,
    description: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("L$level", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = color)
                Text(description, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
