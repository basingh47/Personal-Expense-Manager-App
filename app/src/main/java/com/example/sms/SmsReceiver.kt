package com.example.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PendingSmsTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: ""
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody)
        }
        val fullBody = bodyBuilder.toString()

        val parsed = SmsParser.parseSms(sender, fullBody) ?: return

        // Process in background coroutine
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.financeDao()

                // Check if card matches any registered credit card
                var matchedCreditCardId: Long? = null
                var finalPaymentMethod = parsed.paymentMethod

                if (parsed.lastFourDigits.isNotBlank()) {
                    val cards = dao.getAllCreditCardsList()
                    val matchedCard = cards.firstOrNull { card ->
                        card.lastFourDigits.isNotBlank() && (card.lastFourDigits == parsed.lastFourDigits || card.lastFourDigits.endsWith(parsed.lastFourDigits))
                    }
                    if (matchedCard != null) {
                        matchedCreditCardId = matchedCard.id
                        finalPaymentMethod = "Credit Card"
                    }
                }

                val pendingTx = PendingSmsTransaction(
                    date = System.currentTimeMillis(),
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    rawSender = parsed.rawSender,
                    rawBody = parsed.rawBody,
                    suggestedCategory = parsed.suggestedCategory,
                    paymentMethod = finalPaymentMethod,
                    creditCardId = matchedCreditCardId,
                    lastFourDigits = parsed.lastFourDigits
                )

                val generatedId = dao.insertPendingSmsTransaction(pendingTx)
                val txWithId = pendingTx.copy(id = generatedId)

                // Show notification to user
                SmsNotificationHelper.showDetectedNotification(context, txWithId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
