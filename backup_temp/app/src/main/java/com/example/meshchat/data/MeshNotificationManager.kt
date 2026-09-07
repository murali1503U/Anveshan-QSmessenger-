package com.example.meshchat.data
import com.aistudio.meshchat.kxmpzq.R

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import com.aistudio.meshchat.kxmpzq.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class MeshNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_MESSAGES_ID = "mesh_chat_messages"
        const val CHANNEL_EMERGENCY_ID = "mesh_chat_emergency"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "Mesh Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming peer and group mesh communications"
                enableVibration(true)
                setShowBadge(true)
            }

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY_ID,
                "Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority emergency distress and broadcast alerts"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.let {
                it.createNotificationChannel(messageChannel)
                it.createNotificationChannel(emergencyChannel)
            }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showIncomingMessageNotification(message: Message, channelName: String) {
        if (!hasNotificationPermission()) return

        val isEmergency = message.channelId == "emergency_sos" || message.text.contains("SOS", ignoreCase = true)
        val channelId = if (isEmergency) CHANNEL_EMERGENCY_ID else CHANNEL_MESSAGES_ID

        // Intent to launch MainActivity and open target channel
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CHANNEL_ID, message.channelId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            message.channelId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (channelName.isNotBlank() && channelName != message.sender) {
            "${message.sender} • $channelName"
        } else {
            message.sender
        }

        val bodyText = if (message.mediaBase64 != null) {
            "📷 [Encrypted Image Transmitted]"
        } else {
            message.text
        }

        val subText = when (message.securityLevel) {
            4 -> "🛡️ Kyber-768 PQ"
            3 -> "🔑 Forward Secrecy"
            2 -> "🔒 AES-256 E2EE"
            else -> "📡 Plaintext"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_mesh)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setSubText(subText)
            .setAutoCancel(true)
            .setPriority(if (isEmergency) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isEmergency) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationId = (message.channelId.hashCode() xor message.timestamp.toInt())

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked at runtime
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
