package com.example.meshchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SecurityStatusCard(
    sessionState: String,
    securityLevelBadge: String,
    transportType: String,
    messagesSinceRotation: Int,
    sessionAgeSeconds: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Security Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            
            Text(text = "Session: $sessionState", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Security Level: $securityLevelBadge", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Transport: $transportType", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Messages Since Key Rotation: $messagesSinceRotation", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Session Age: ${sessionAgeSeconds}s", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
