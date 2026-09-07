package com.example.meshchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.meshchat.plugin.PluginManager
import com.example.meshchat.scripting.ScriptManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(onDismiss: () -> Unit) {
    var scriptText by remember { mutableStateOf("") }
    var scriptName by remember { mutableStateOf("user_script_${System.currentTimeMillis()}") }
    
    val loadedScripts by ScriptManager.loadedScripts.collectAsStateWithLifecycle()
    val activePlugins by PluginManager.activePluginsFlow.collectAsStateWithLifecycle()
    val allPlugins = remember(activePlugins, loadedScripts) { PluginManager.getAll() }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("🧩 Plugin & Scripting Hub") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Pre-built Quick Scripts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScriptManager.getPrebuiltScripts().keys.take(2).forEach { name ->
                                Button(
                                    onClick = { 
                                        val scriptData = ScriptManager.getPrebuiltScripts()[name]!!
                                        ScriptManager.load(name, scriptData.second, scriptData.first) 
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(name)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScriptManager.getPrebuiltScripts().keys.drop(2).forEach { name ->
                                Button(
                                    onClick = { 
                                        val scriptData = ScriptManager.getPrebuiltScripts()[name]!!
                                        ScriptManager.load(name, scriptData.second, scriptData.first) 
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    }
                    
                    item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                    
                    item {
                        Text("Custom Script Editor (Lua)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = scriptName,
                            onValueChange = { scriptName = it },
                            label = { Text("Script Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = scriptText,
                            onValueChange = { scriptText = it },
                            label = { Text("function processMessage(msg) ... end") },
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                if (scriptText.isNotBlank()) {
                                    ScriptManager.load(scriptName, scriptText) 
                                    scriptName = "user_script_${System.currentTimeMillis()}"
                                    scriptText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("▶ Compile & Load Script")
                        }
                    }
                    
                    item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                    
                    item {
                        Text("Active Plugins & Scripts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    items(allPlugins) { plugin ->
                        val isActive = activePlugins.contains(plugin.id)
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(plugin.name, fontWeight = FontWeight.Bold)
                                    Text("v${plugin.version} | ID: ${plugin.id}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    if (isActive) {
                                        Button(onClick = { PluginManager.disable(plugin.id) }) { Text("Disable") }
                                    } else {
                                        Button(onClick = { PluginManager.enable(plugin.id) }) { Text("Enable") }
                                    }
                                    if (loadedScripts.contains(plugin.id)) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = { ScriptManager.unload(plugin.id) }) { Text("Unload") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
