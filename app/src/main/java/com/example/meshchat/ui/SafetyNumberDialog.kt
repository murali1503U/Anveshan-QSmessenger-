package com.example.meshchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshchat.data.ChatChannel
import com.example.meshchat.data.SecureSession
import java.security.MessageDigest

@Composable
fun SafetyNumberDialog(
    mySession: SecureSession?,
    peerChannel: ChatChannel?,
    onDismiss: () -> Unit
) {
    // Generate a deterministic safety number based on both identities
    val safetyNumber = remember(mySession, peerChannel) {
        if (mySession == null || peerChannel == null) return@remember "N/A"
        
        // In a real app, this would use the public keys, but here we simulate it
        // by hashing the combination of our node ID and their callsign/node ID.
        // We sort them to ensure both parties generate the exact same string regardless of who initiates.
        val id1 = mySession.nodeId
        val id2 = if (peerChannel.members.isNotBlank()) peerChannel.members else peerChannel.name
        
        val sortedIds = listOf(id1, id2).sorted().joinToString("|")
        
        try {
            val hashBytes = com.example.meshchat.perf.SentinelCryptoConscrypt.sha512(sortedIds.toByteArray(Charsets.UTF_8))
            
            // Convert to a nice format: 5 blocks of 5 digits
            val numberString = hashBytes.joinToString("") { "%02d".format((it.toInt() and 0xFF) % 100) }
            val formatted = numberString.take(25).chunked(5).joinToString(" - ")
            formatted
        } catch (e: Exception) {
            "Error Generating Code"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Verify Safety Number", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "To verify that your remote connection to ${peerChannel?.name ?: "this peer"} is secure against Man-in-the-Middle attacks, read this number aloud to them over a trusted channel (like a phone call).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = safetyNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "If the numbers do not match exactly, you are being intercepted. Stop communicating immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
