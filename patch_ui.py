import re

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

replacement1 = """
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val loRaState by viewModel.loRaState.collectAsStateWithLifecycle()
"""
content = re.sub(r'val connectionState by viewModel\.connectionState\.collectAsStateWithLifecycle\(\)', replacement1.strip(), content)

replacement2 = """
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.SatelliteAlt,
                                contentDescription = "LoRa Status",
                                tint = when(loRaState) {
                                    ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                    ConnectionState.SCANNING -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "LoRa PRIORITY • ${if (loRaState == ConnectionState.CONNECTED) "Active" else "Unavailable"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            ConnectionState.CONNECTED -> Color(0xFF2196F3) // Blue for BT Fallback
                                            ConnectionState.SCANNING, ConnectionState.CONNECTING -> Color(0xFFFFC107)
                                            else -> Color(0xFF9E9E9E)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (connectionState == ConnectionState.CONNECTED) {
                                    "BT Relay: ${connectedDevice?.name ?: "Node"}"
                                } else if (connectionState == ConnectionState.SCANNING) {
                                    "Scanning BT..."
                                } else {
                                    "BT Relay Offline"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
"""
content = re.sub(
    r'Row\(verticalAlignment = Alignment\.CenterVertically\) \{\s*Box\(\s*modifier = Modifier\s*\.size\(8\.dp\).*?color = MaterialTheme\.colorScheme\.onSurfaceVariant\s*\)\s*\}',
    replacement2.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
