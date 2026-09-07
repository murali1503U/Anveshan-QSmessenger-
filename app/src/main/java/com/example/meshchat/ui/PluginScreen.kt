package com.example.meshchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshchat.plugin.PluginManager
import com.example.meshchat.plugin.PluginHotSwap
import com.example.meshchat.plugin.SentinelPlugin

@Composable
fun PluginScreen() {
    val plugins = remember { PluginManager.getAllPlugins() }
    var selectedPlugin by remember { mutableStateOf<SentinelPlugin?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            Text("🧩 Plugins", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { PluginHotSwap.scanForNewPlugins() }) {
                Text("Scan")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { PluginHotSwap.reloadAll() }) {
                Text("Reload All")
            }
        }
        
        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(plugins) { entry ->
                PluginCard(
                    plugin = entry.plugin,
                    entry = entry,
                    onToggle = { enabled ->
                        if (enabled) PluginManager.enable(entry.plugin.id)
                        else PluginManager.disable(entry.plugin.id)
                    },
                    onUninstall = { PluginHotSwap.uninstallPlugin(entry.plugin.id) },
                    onClick = { selectedPlugin = entry.plugin }
                )
            }
        }
    }
}

@Composable
fun PluginCard(
    plugin: SentinelPlugin,
    entry: PluginManager.PluginEntry,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(plugin.name, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    if (PluginManager.isPluginActive(plugin.id)) {
                        Text("● Active", color = Color.Green, fontSize = 12.sp)
                    } else {
                        Text("○ Inactive", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Text("v${plugin.version}", fontSize = 12.sp, color = Color.Gray)
                Text("ID: ${plugin.id}", fontSize = 10.sp, color = Color.Gray)
                if (entry.crashCount > 0) {
                    Text("⚠️ Crashes: ${entry.crashCount}", fontSize = 10.sp, color = Color.Red)
                }
            }
            Row {
                Switch(
                    checked = PluginManager.isPluginActive(plugin.id),
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onUninstall) {
                    Icon(Icons.Default.Delete, contentDescription = "Uninstall")
                }
            }
        }
    }
}
