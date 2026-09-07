package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meshchat.data.MeshNotificationManager
import com.example.meshchat.ui.ChatScreen
import com.example.meshchat.ui.ChatViewModel
import com.example.meshchat.ui.SetupWizardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var targetChannelIdFromIntent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable hardware acceleration
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        // Start jank detection
        com.example.meshchat.util.SentinelJankDetector.startMonitoring()
        
        targetChannelIdFromIntent = intent?.getStringExtra(MeshNotificationManager.EXTRA_CHANNEL_ID)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            // Notification Permission request for Android 13+ (Tiramisu)
            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Permission granted or denied */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
                    var isInsideChatConversation by remember { mutableStateOf(false) }

                    // Check if launched from a notification
                    LaunchedEffect(targetChannelIdFromIntent) {
                        targetChannelIdFromIntent?.let { chId ->
                            viewModel.selectChannel(chId)
                            isInsideChatConversation = true
                            targetChannelIdFromIntent = null
                        }
                    }
                    
                    var showGlobalSettingsSheet by remember { mutableStateOf(false) }
                    var showGlobalPairingSheet by remember { mutableStateOf(false) }
                    var showGlobalQsvmSheet by remember { mutableStateOf(false) }

                    if (activeSession != null && !activeSession!!.isSetupCompleted) {
                        SetupWizardScreen(
                            viewModel = viewModel,
                            onSetupComplete = {
                                // Handled via StateFlow update in Room
                            }
                        )
                    } else {
                        if (!isInsideChatConversation) {
                            com.example.meshchat.ui.ChannelsListScreen(
                                viewModel = viewModel,
                                onChannelClick = { channel ->
                                    viewModel.selectChannel(channel.channelId)
                                    isInsideChatConversation = true
                                },
                                onOpenSettings = { showGlobalSettingsSheet = true },
                                onOpenPairing = {
                                    showGlobalPairingSheet = true
                                    viewModel.startScanning()
                                }
                            )
                        } else {
                            ChatScreen(
                                viewModel = viewModel,
                                onBackToChannels = { isInsideChatConversation = false }
                            )
                        }
                    }

                    // Global Settings Sheet
                    if (showGlobalSettingsSheet) {
                        com.example.meshchat.ui.MeshSettingsSheet(
                            viewModel = viewModel,
                            onDismiss = { showGlobalSettingsSheet = false },
                            onOpenPairing = {
                                showGlobalPairingSheet = true
                                viewModel.startScanning()
                            },
                            onOpenQsvmInspector = {
                                showGlobalQsvmSheet = true
                            }
                        )
                    }

                    // Global Bluetooth / LoRa Pairing Sheet
                    if (showGlobalPairingSheet) {
                        com.example.meshchat.ui.MeshPairingSheet(
                            viewModel = viewModel,
                            onDismiss = { showGlobalPairingSheet = false }
                        )
                    }

                    // Global QSVM Inspector Sheet
                    if (showGlobalQsvmSheet) {
                        com.example.meshchat.ui.AiModelHubSheet(
                            viewModel = viewModel,
                            onDismiss = { showGlobalQsvmSheet = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(MeshNotificationManager.EXTRA_CHANNEL_ID)?.let { chId ->
            targetChannelIdFromIntent = chId
        }
    }
}

