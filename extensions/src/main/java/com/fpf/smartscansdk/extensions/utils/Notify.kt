package com.fpf.smartscansdk.extensions.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// Required for callbacks
@SuppressLint("MissingPermission")
fun showNotification(context: Context,
                     title: String,
                     text: String,
                     channelId: String,
                     smallIconResId: Int,
                     id: Int = 1001
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(smallIconResId)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        notify(id, notificationBuilder.build())
    }
}
