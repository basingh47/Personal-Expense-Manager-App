package com.example.subscription

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
import com.example.data.Subscription
import com.example.ui.util.NumberFormatConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SubscriptionNotificationHelper {

    const val CHANNEL_ID = "subscription_renewal_alerts"
    const val CHANNEL_NAME = "Subscription & Auto-Renew Alerts"
    const val CHANNEL_DESC = "Advance reminders for upcoming subscription renewals and auto-debits"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 200, 100, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Calculates the number of days remaining until the renewal date.
     * Normalized to calendar day start.
     */
    fun calculateDaysUntilRenewal(renewalDateMillis: Long): Int {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = renewalDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - nowCal.timeInMillis
        return (diffMillis / (24 * 3600 * 1000L)).toInt()
    }

    /**
     * Human-friendly label for days left.
     */
    fun getFormattedDaysLeft(daysUntilRenewal: Int): String {
        return when {
            daysUntilRenewal < 0 -> "Overdue by ${-daysUntilRenewal} ${if (-daysUntilRenewal == 1) "day" else "days"}"
            daysUntilRenewal == 0 -> "Renews Today"
            daysUntilRenewal == 1 -> "Renews Tomorrow"
            else -> "Renews in $daysUntilRenewal days"
        }
    }

    /**
     * Advances date to the next billing cycle.
     */
    fun getNextCycleTimestamp(currentRenewalMillis: Long, billingCycle: String): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentRenewalMillis
        }
        val months = when (billingCycle) {
            "MONTHLY" -> 1
            "2_MONTHS" -> 2
            "3_MONTHS" -> 3
            "4_MONTHS" -> 4
            "5_MONTHS" -> 5
            "6_MONTHS" -> 6
            "YEARLY" -> 12
            else -> {
                if (billingCycle.endsWith("_MONTHS")) {
                    billingCycle.substringBefore("_MONTHS").toIntOrNull() ?: 1
                } else {
                    1
                }
            }
        }
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    /**
     * Shows a real system notification for an upcoming subscription renewal.
     */
    fun showRenewalNotification(context: Context, sub: Subscription, daysUntilRenewal: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_SCREEN", "subscriptions")
                putExtra("SUBSCRIPTION_ID", sub.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (sub.id.takeIf { it > 0 } ?: System.currentTimeMillis()).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(sub.renewalDate))
            val formattedCost = NumberFormatConfig.formatAmount(sub.cost)

            val title = when {
                daysUntilRenewal <= 0 -> "⚠️ Renewal Alert: ${sub.name} is due today ($formattedCost)"
                daysUntilRenewal == 1 -> "⏰ Tomorrow: ${sub.name} Renews ($formattedCost)"
                else -> "🔔 Advance Alert: ${sub.name} renews in $daysUntilRenewal days ($formattedCost)"
            }

            val autoRenewText = if (sub.isAutoRenew) "⚡ Auto-Renew is ACTIVE" else "📝 Manual Payment Required"
            val paymentSourceText = if (sub.paymentMethodName.isNotBlank()) "Linked: ${sub.paymentMethodName}" else "Payment method: Default"

            val shortContent = "$formattedCost due on $formattedDate • $autoRenewText"
            val expandedText = buildString {
                append("• Amount: $formattedCost\n")
                append("• Renewal Date: $formattedDate\n")
                append("• Status: $autoRenewText\n")
                append("• $paymentSourceText\n")
                append("• Advance Reminder Setting: ${sub.reminderDaysInAdvance} days prior\n\n")
                if (sub.isAutoRenew) {
                    append("💡 Auto-debit will be initiated. Please ensure adequate funds in your account or cancel prior if no longer needed.")
                } else {
                    append("💡 Remember to make payment before $formattedDate to prevent service interruption.")
                }
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(shortContent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notifId = (20000 + sub.id).toInt()
            notificationManager.notify(notifId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Test notification trigger for manual verification from UI.
     */
    fun testRenewalNotification(context: Context, sub: Subscription) {
        val days = calculateDaysUntilRenewal(sub.renewalDate)
        showRenewalNotification(context, sub, days)
    }

    /**
     * Checks all subscriptions and fires notifications for those within their reminder window.
     */
    fun checkAndTriggerRenewalAlerts(context: Context, subscriptions: List<Subscription>) {
        subscriptions.filter { it.isActive }.forEach { sub ->
            val daysLeft = calculateDaysUntilRenewal(sub.renewalDate)
            // Fire if within configured reminder days in advance (e.g. 0..sub.reminderDaysInAdvance)
            if (daysLeft in 0..sub.reminderDaysInAdvance) {
                showRenewalNotification(context, sub, daysLeft)
            }
        }
    }
}
