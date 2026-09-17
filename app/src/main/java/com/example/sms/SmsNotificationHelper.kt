package com.example.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PendingSmsTransaction
import com.example.ui.util.NumberFormatConfig

object SmsNotificationHelper {

    const val CHANNEL_ID = "sms_expense_alerts"
    const val CHANNEL_NAME = "Bank Transaction Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when new bank or card transactions are detected from SMS"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDetectedNotification(context: Context, tx: PendingSmsTransaction) {
        try {
            // Android 13+ permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // Cannot show notification without permission
                }
            }

            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("PENDING_SMS_ID", tx.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (tx.id.takeIf { it > 0 } ?: System.currentTimeMillis()).toInt(),
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val sign = if (tx.type == "INCOME") "+" else "-"
            val formattedAmount = "$sign ${NumberFormatConfig.formatAmount(tx.amount)}"
            val title = "⚡ $formattedAmount detected: ${tx.merchant}"
            val subtitle = "Paid via ${tx.paymentMethod} • Suggested: ${tx.suggestedCategory}"
            val expandedText = "$subtitle\n\nOriginal SMS:\n\"${tx.rawBody}\"\n\nTap to open Expense Manager and confirm category."

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationId = (tx.id.takeIf { it > 0 } ?: (System.currentTimeMillis() % 100000)).toInt()
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
