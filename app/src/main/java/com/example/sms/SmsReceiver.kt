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

        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("sms_detection_enabled", true)
        if (!isEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: ""
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody)
        }
        val fullBody = bodyBuilder.toString()

        // Process in background coroutine
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.financeDao()

                // Fetch active custom categories to intelligently match incoming SMS
                val customCategories = dao.getAllCustomCategoriesList()
                val parsed = SmsParser.parseSms(sender, fullBody, customCategories) ?: return@launch

                // Check registered Bank Accounts and Credit Cards
                val bankAccounts = dao.getAllBankAccountsList()
                val cards = dao.getAllCreditCardsList()

                var matchedBankAccountId: Long? = null
                var matchedCreditCardId: Long? = null
                var finalPaymentMethod = parsed.paymentMethod

                // 1. Check Credit Card match first
                if (parsed.lastFourDigits.isNotBlank()) {
                    val matchedCard = cards.firstOrNull { card ->
                        card.lastFourDigits.isNotBlank() && (card.lastFourDigits == parsed.lastFourDigits || card.lastFourDigits.endsWith(parsed.lastFourDigits))
                    }
                    if (matchedCard != null) {
                        matchedCreditCardId = matchedCard.id
                        finalPaymentMethod = "Credit Card"
                    }
                }

                // 2. Check Bank Account match (by digits or by sender name)
                val matchedBank = if (parsed.lastFourDigits.isNotBlank()) {
                    bankAccounts.firstOrNull { bank ->
                        bank.accountNumberLast4.isNotBlank() && (bank.accountNumberLast4 == parsed.lastFourDigits || bank.accountNumberLast4.endsWith(parsed.lastFourDigits))
                    }
                } else null ?: bankAccounts.firstOrNull { bank ->
                    val bankKey = bank.bankName.lowercase().replace("bank", "").trim()
                    bankKey.length >= 3 && (sender.lowercase().contains(bankKey) || fullBody.lowercase().contains(bankKey))
                }

                // 3. Bank-Level SMS Filtering Rule:
                // If a bank is matched and SMS detection is disabled for this bank, ignore this SMS!
                if (matchedBank != null) {
                    if (!matchedBank.isSmsDetectionEnabled) {
                        return@launch
                    }
                    matchedBankAccountId = matchedBank.id
                    if (matchedCreditCardId == null && finalPaymentMethod != "Credit Card") {
                        if (finalPaymentMethod != "Bank" && finalPaymentMethod != "UPI") {
                            finalPaymentMethod = "Bank"
                        }
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
                    bankAccountId = matchedBankAccountId,
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
