package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID_INTERACTIONS = "fadx_interactions_channel"
    const val CHANNEL_ID_MESSAGES = "fadx_messages_channel"
    private const val CHANNEL_NAME_INTERACTIONS = "Likes, Comments & Social"
    private const val CHANNEL_NAME_MESSAGES = "Direct Messages"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val interactionChannel = NotificationChannel(
                CHANNEL_ID_INTERACTIONS,
                CHANNEL_NAME_INTERACTIONS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for likes, comments, friend requests and mentions"
                enableLights(true)
                enableVibration(true)
            }

            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for direct messages and group chats"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(interactionChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_INTERACTIONS
    ) {
        initNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted yet on Android 13+
        }
    }
}
