package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.export.*
import com.example.sms.SmsNotificationHelper
import com.example.sms.SmsParser
import com.example.subscription.SubscriptionNotificationHelper
import com.example.ui.util.NumberFormatConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DeleteConfirmationRequest(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    
    private fun initFirebaseIfPossible(): Boolean {
        if (FirebaseApp.getApps(getApplication<Application>()).isNotEmpty()) {
            return true
        }
        return try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:abcdef")
                .setProjectId("personal-expense-manager")
                .setApiKey("AIzaSyDummyKeyForOfflineGracefulFallback")
                .build()
            FirebaseApp.initializeApp(getApplication(), options)
            true
        } catch (e: Exception) {
            false
        }
    }

    private val isFirebaseAvailable: Boolean = initFirebaseIfPossible()

    val firebaseAuth: FirebaseAuth? = if (isFirebaseAvailable) {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    } else null

    val firestore: FirebaseFirestore? = if (isFirebaseAvailable) {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    } else null

    private val sharedPrefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val currentFirebaseUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    private inline fun withCloud(block: (user: FirebaseUser, store: FirebaseFirestore) -> Unit) {
        val user = currentFirebaseUser ?: return
        val store = firestore ?: return
        try {
            block(user, store)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(currentFirebaseUser)
    val currentUser = _currentUser.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
        
        firebaseAuth?.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            if (auth.currentUser != null) {
                syncDataWithCloud()
            }
        }
        
        viewModelScope.launch {
            // Populate standard/system categories into custom_categories table if it is empty
            repository.allCustomCategories.first().let { list ->
                if (list.isEmpty()) {
                    CategoryData.expenseCategories.forEach { cat ->
                        repository.insertCustomCategory(
                            CustomCategory(
                                name = cat.name,
                                icon = cat.icon,
                                type = "EXPENSE",
                                parentCategory = null
                            )
                        )
                        cat.subcategories.forEach { sub ->
                            repository.insertCustomCategory(
                                CustomCategory(
                                    name = sub,
                                    icon = "",
                                    type = "EXPENSE",
                                    parentCategory = cat.name
                                )
                            )
                        }
                    }
                    CategoryData.incomeCategories.forEach { cat ->
                        repository.insertCustomCategory(
                            CustomCategory(
                                name = cat.name,
                                icon = cat.icon,
                                type = "INCOME",
                                parentCategory = null
                            )
                        )
                        cat.subcategories.forEach { sub ->
                            repository.insertCustomCategory(
                                CustomCategory(
                                    name = sub,
                                    icon = "",
                                    type = "INCOME",
                                    parentCategory = cat.name
                                )
                            )
                        }
                    }
                }
            }

            // Default first-run setup: ensure clean ledger (no demo data auto-seeded)
            sharedPrefs.edit().putBoolean("has_seeded_sample_data", true).apply()
        }
    }

    // Reactively observe tables
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assets = repository.allAssets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.allBudgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subscriptions = repository.allSubscriptions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savingsGoals = repository.allSavingsGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val borrowLends = repository.allBorrowLends.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wishlist = repository.allWishlistItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customCategories = repository.allCustomCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val creditCards = repository.allCreditCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bankAccounts = repository.allBankAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingSmsTransactions = repository.allPendingSmsTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search & Filter state
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _selectedFilterType = MutableStateFlow<String?>(null) // null (All), "EXPENSE", "INCOME", "TRANSFER", "REFUND"
    val selectedFilterType = _selectedFilterType.asStateFlow()

    private val _selectedFilterCategory = MutableStateFlow<String?>(null)
    val selectedFilterCategory = _selectedFilterCategory.asStateFlow()

    private val _selectedFilterMethod = MutableStateFlow<String?>(null)
    val selectedFilterMethod = _selectedFilterMethod.asStateFlow()

    private val _selectedFilterBankAccount = MutableStateFlow<Long?>(null)
    val selectedFilterBankAccount = _selectedFilterBankAccount.asStateFlow()

    private val _selectedFilterMonth = MutableStateFlow<String?>(null) // Format: "yyyy-MM"
    val selectedFilterMonth = _selectedFilterMonth.asStateFlow()

    private val _selectedFilterDate = MutableStateFlow<String?>(null) // Format: "yyyy-MM-dd"
    val selectedFilterDate = _selectedFilterDate.asStateFlow()

    private val _selectedFilterTag = MutableStateFlow<String?>(null)
    val selectedFilterTag = _selectedFilterTag.asStateFlow()

    // Multi-Select & Bulk Actions State
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds = _selectedTransactionIds.asStateFlow()

    private val _deleteConfirmation = MutableStateFlow<DeleteConfirmationRequest?>(null)
    val deleteConfirmation = _deleteConfirmation.asStateFlow()

    private val _excludeCreditCards = MutableStateFlow(sharedPrefs.getBoolean("cash_only_mode", false))
    val excludeCreditCards = _excludeCreditCards.asStateFlow()

    private val _numberFormatPreference = MutableStateFlow(sharedPrefs.getString("number_format_preference", "AUTO") ?: "AUTO")
    val numberFormatPreference = _numberFormatPreference.asStateFlow()

    private val _currencySymbol = MutableStateFlow(sharedPrefs.getString("currency_symbol", "₹") ?: "₹")
    val currencySymbol = _currencySymbol.asStateFlow()

    private val _currencyCode = MutableStateFlow(sharedPrefs.getString("currency_code", "INR") ?: "INR")
    val currencyCode = _currencyCode.asStateFlow()

    init {
        NumberFormatConfig.activePreference = _numberFormatPreference.value
        NumberFormatConfig.currencySymbol = _currencySymbol.value
        NumberFormatConfig.currencyCode = _currencyCode.value
    }

    private val _isSmsDetectionEnabled = MutableStateFlow(sharedPrefs.getBoolean("sms_detection_enabled", true))
    val isSmsDetectionEnabled = _isSmsDetectionEnabled.asStateFlow()

    fun setCurrency(symbol: String, code: String = "", defaultNumberFormat: String? = null) {
        _currencySymbol.value = symbol
        NumberFormatConfig.currencySymbol = symbol
        sharedPrefs.edit().putString("currency_symbol", symbol).apply()

        if (code.isNotBlank()) {
            _currencyCode.value = code
            NumberFormatConfig.currencyCode = code
            sharedPrefs.edit().putString("currency_code", code).apply()
        }

        if (defaultNumberFormat != null && _numberFormatPreference.value == "AUTO") {
            // Keep AUTO but ensure NumberFormatConfig has correct context
            NumberFormatConfig.activePreference = "AUTO"
        }

        val user = firebaseAuth?.currentUser
        if (user != null) {
            val prefData = mutableMapOf<String, Any>(
                "currencySymbol" to symbol,
                "currencyCode" to _currencyCode.value,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore!!.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .set(prefData, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun setNumberFormatPreference(format: String) {
        _numberFormatPreference.value = format
        NumberFormatConfig.activePreference = format
        sharedPrefs.edit().putString("number_format_preference", format).apply()
        val user = firebaseAuth?.currentUser
        if (user != null) {
            val prefData = mapOf(
                "numberFormatPreference" to format,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore!!.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .set(prefData, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun setSmsDetectionEnabled(enabled: Boolean) {
        _isSmsDetectionEnabled.value = enabled
        sharedPrefs.edit().putBoolean("sms_detection_enabled", enabled).apply()
        val user = firebaseAuth?.currentUser
        if (user != null) {
            val prefData = mapOf(
                "smsDetectionEnabled" to enabled,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore!!.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .set(prefData, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun setExcludeCreditCards(exclude: Boolean) {
        _excludeCreditCards.value = exclude
        sharedPrefs.edit().putBoolean("cash_only_mode", exclude).apply()
        val user = firebaseAuth?.currentUser
        if (user != null) {
            val prefData = mapOf(
                "cashOnlyMode" to exclude,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore!!.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .set(prefData, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun requestDeleteConfirmation(title: String, message: String, onConfirm: () -> Unit) {
        _deleteConfirmation.value = DeleteConfirmationRequest(title, message, onConfirm)
    }

    fun dismissDeleteConfirmation() {
        _deleteConfirmation.value = null
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun setFilterType(type: String?) {
        _selectedFilterType.value = type
    }

    fun setFilterCategory(cat: String?) {
        _selectedFilterCategory.value = cat
    }

    fun setFilterMethod(method: String?) {
        _selectedFilterMethod.value = method
    }

    fun setFilterBankAccount(bankId: Long?) {
        _selectedFilterBankAccount.value = bankId
    }

    fun setFilterMonth(month: String?) {
        _selectedFilterMonth.value = month
    }

    fun setFilterDate(dateStr: String?) {
        _selectedFilterDate.value = dateStr
    }

    fun setFilterTag(tag: String?) {
        _selectedFilterTag.value = tag
    }

    fun clearFilters() {
        _searchText.value = ""
        _selectedFilterType.value = null
        _selectedFilterCategory.value = null
        _selectedFilterMethod.value = null
        _selectedFilterBankAccount.value = null
        _selectedFilterMonth.value = null
        _selectedFilterDate.value = null
        _selectedFilterTag.value = null
    }

    // MULTI-SELECT & BULK ACTIONS
    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun toggleTransactionSelection(id: Long) {
        val current = _selectedTransactionIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedTransactionIds.value = current
        if (current.isNotEmpty() && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        } else if (current.isEmpty() && _isSelectionMode.value) {
            // Keep selection mode active or let user cancel explicitly
        }
    }

    fun selectAllTransactions(ids: Collection<Long>) {
        _selectedTransactionIds.value = ids.toSet()
        if (ids.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
        _isSelectionMode.value = false
    }

    // BULK OPERATIONS
    fun bulkDeleteSelected() {
        val idsToDelete = _selectedTransactionIds.value.toList()
        if (idsToDelete.isEmpty()) return

        requestDeleteConfirmation(
            title = "Delete ${idsToDelete.size} Transactions",
            message = "Are you sure you want to delete these ${idsToDelete.size} selected transactions? This cannot be undone.",
            onConfirm = {
                viewModelScope.launch {
                    repository.deleteTransactionsByIds(idsToDelete)
                    val user = firebaseAuth?.currentUser
                    if (user != null) {
                        idsToDelete.forEach { id ->
                            firestore!!.collection("users").document(user.uid).collection("transactions")
                                .document(id.toString()).delete()
                        }
                    }
                    clearSelection()
                }
            }
        )
    }

    fun bulkUpdateCategory(targetIds: Set<Long>, newCategory: String, newSubcategory: String) {
        if (targetIds.isEmpty()) return
        viewModelScope.launch {
            val allList = repository.allTransactions.first()
            val updatedList = allList.filter { targetIds.contains(it.id) }.map { tx ->
                tx.copy(category = newCategory, subcategory = newSubcategory)
            }
            if (updatedList.isNotEmpty()) {
                repository.insertTransactions(updatedList)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    updatedList.forEach { tx ->
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update(
                                mapOf(
                                    "category" to tx.category,
                                    "subcategory" to tx.subcategory
                                )
                            )
                    }
                }
            }
            clearSelection()
        }
    }

    fun bulkUpdateAccountAndMethod(
        targetIds: Set<Long>,
        bankAccountId: Long?,
        creditCardId: Long?,
        paymentMethod: String
    ) {
        if (targetIds.isEmpty()) return
        viewModelScope.launch {
            val allList = repository.allTransactions.first()
            val updatedList = allList.filter { targetIds.contains(it.id) }.map { tx ->
                tx.copy(
                    paymentMethod = paymentMethod,
                    bankAccountId = bankAccountId,
                    creditCardId = creditCardId
                )
            }
            if (updatedList.isNotEmpty()) {
                repository.insertTransactions(updatedList)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    updatedList.forEach { tx ->
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update(
                                mapOf(
                                    "paymentMethod" to tx.paymentMethod,
                                    "bankAccountId" to tx.bankAccountId,
                                    "creditCardId" to tx.creditCardId
                                )
                            )
                    }
                }
            }
            clearSelection()
        }
    }

    fun bulkAddTag(targetIds: Set<Long>, tagToAdd: String) {
        val cleanTag = if (tagToAdd.trim().startsWith("#")) tagToAdd.trim() else "#${tagToAdd.trim()}"
        if (cleanTag.length <= 1 || targetIds.isEmpty()) return

        viewModelScope.launch {
            val allList = repository.allTransactions.first()
            val updatedList = allList.filter { targetIds.contains(it.id) }.map { tx ->
                val currentTags = parseTags(tx.tagsString).toMutableList()
                if (!currentTags.any { it.equals(cleanTag, ignoreCase = true) }) {
                    currentTags.add(cleanTag)
                }
                tx.copy(tagsString = currentTags.joinToString(", "))
            }
            if (updatedList.isNotEmpty()) {
                repository.insertTransactions(updatedList)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    updatedList.forEach { tx ->
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("tagsString", tx.tagsString)
                    }
                }
            }
            clearSelection()
        }
    }

    fun bulkRemoveTag(targetIds: Set<Long>, tagToRemove: String) {
        val cleanTag = if (tagToRemove.trim().startsWith("#")) tagToRemove.trim() else "#${tagToRemove.trim()}"
        if (targetIds.isEmpty()) return

        viewModelScope.launch {
            val allList = repository.allTransactions.first()
            val updatedList = allList.filter { targetIds.contains(it.id) }.map { tx ->
                val currentTags = parseTags(tx.tagsString).filterNot { it.equals(cleanTag, ignoreCase = true) }
                tx.copy(tagsString = currentTags.joinToString(", "))
            }
            if (updatedList.isNotEmpty()) {
                repository.insertTransactions(updatedList)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    updatedList.forEach { tx ->
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("tagsString", tx.tagsString)
                    }
                }
            }
            clearSelection()
        }
    }

    fun bulkSetTags(targetIds: Set<Long>, newTagsString: String) {
        if (targetIds.isEmpty()) return
        val formatted = parseTags(newTagsString).joinToString(", ")
        viewModelScope.launch {
            val allList = repository.allTransactions.first()
            val updatedList = allList.filter { targetIds.contains(it.id) }.map { tx ->
                tx.copy(tagsString = formatted)
            }
            if (updatedList.isNotEmpty()) {
                repository.insertTransactions(updatedList)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    updatedList.forEach { tx ->
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("tagsString", tx.tagsString)
                    }
                }
            }
            clearSelection()
        }
    }

    // TAG ENGINE & PROJECT SUMMARIES
    companion object {
        fun parseTags(raw: String): List<String> {
            if (raw.isBlank()) return emptyList()
            return raw.split(",", ";", " ")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { if (it.startsWith("#")) it else "#$it" }
                .distinct()
        }
    }

    val allUniqueTags: StateFlow<List<String>> = transactions.map { list ->
        list.flatMap { parseTags(it.tagsString) }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class TagProjectSummary(
        val tag: String,
        val totalExpense: Double,
        val totalIncome: Double,
        val netSpent: Double,
        val transactionCount: Int,
        val categoryBreakdown: Map<String, Double>,
        val firstDate: Long,
        val lastDate: Long
    )

    val tagProjectSummaries: StateFlow<List<TagProjectSummary>> = transactions.map { list ->
        val tagMap = mutableMapOf<String, MutableList<Transaction>>()
        list.forEach { tx ->
            val tags = parseTags(tx.tagsString)
            tags.forEach { tag ->
                tagMap.getOrPut(tag) { mutableListOf() }.add(tx)
            }
        }

        tagMap.map { (tag, txList) ->
            var totalExpense = 0.0
            var totalIncome = 0.0
            val catBreakdown = mutableMapOf<String, Double>()
            var minDate = Long.MAX_VALUE
            var maxDate = Long.MIN_VALUE

            txList.forEach { tx ->
                if (tx.date < minDate) minDate = tx.date
                if (tx.date > maxDate) maxDate = tx.date

                when (tx.type) {
                    "INCOME" -> totalIncome += tx.amount
                    "REFUND" -> {
                        totalExpense -= tx.amount
                        val current = catBreakdown.getOrDefault(tx.category, 0.0)
                        catBreakdown[tx.category] = (current - tx.amount).coerceAtLeast(0.0)
                    }
                    "TRANSFER" -> {
                        // Exclude pure transfers from project expense/income totals unless tagged explicitly
                    }
                    else -> {
                        // EXPENSE
                        totalExpense += tx.amount
                        val current = catBreakdown.getOrDefault(tx.category, 0.0)
                        catBreakdown[tx.category] = current + tx.amount
                    }
                }
            }

            TagProjectSummary(
                tag = tag,
                totalExpense = totalExpense.coerceAtLeast(0.0),
                totalIncome = totalIncome,
                netSpent = (totalExpense - totalIncome),
                transactionCount = txList.size,
                categoryBreakdown = catBreakdown.filter { it.value > 0 },
                firstDate = if (minDate == Long.MAX_VALUE) System.currentTimeMillis() else minDate,
                lastDate = if (maxDate == Long.MIN_VALUE) System.currentTimeMillis() else maxDate
            )
        }.sortedByDescending { it.totalExpense }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterState(
        val type: String?,
        val cat: String?,
        val method: String?,
        val bankId: Long?,
        val month: String?,
        val dateStr: String?,
        val tag: String?
    )

    private val filterOptions = combine(
        selectedFilterType,
        selectedFilterCategory,
        selectedFilterMethod,
        selectedFilterBankAccount,
        selectedFilterMonth,
        selectedFilterDate,
        selectedFilterTag
    ) { args: Array<Any?> ->
        FilterState(
            type = args[0] as? String,
            cat = args[1] as? String,
            method = args[2] as? String,
            bankId = args[3] as? Long,
            month = args[4] as? String,
            dateStr = args[5] as? String,
            tag = args[6] as? String
        )
    }

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        searchText,
        filterOptions
    ) { list, search, filters ->
        val trimmedSearch = search.trim()
        val searchWithoutCommas = trimmedSearch.replace(",", "")
        val searchCleanedAmount = searchWithoutCommas.replace("₹", "").replace("$", "").replace("€", "").replace("£", "").trim()

        list.filter { tx ->
            val amountStr = tx.amount.toString()
            val amountIntStr = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else ""
            val amountFormatted = String.format(Locale.US, "%.2f", tx.amount)

            val matchAmount = if (searchCleanedAmount.isNotEmpty()) {
                amountStr.contains(searchCleanedAmount) ||
                (amountIntStr.isNotEmpty() && amountIntStr.contains(searchCleanedAmount)) ||
                amountFormatted.contains(searchCleanedAmount)
            } else false

            val matchSearch = trimmedSearch.isBlank() || 
                tx.merchant.contains(trimmedSearch, ignoreCase = true) ||
                tx.category.contains(trimmedSearch, ignoreCase = true) ||
                tx.subcategory.contains(trimmedSearch, ignoreCase = true) ||
                tx.notes.contains(trimmedSearch, ignoreCase = true) ||
                tx.tagsString.contains(trimmedSearch, ignoreCase = true) ||
                matchAmount

            val matchType = filters.type == null || tx.type == filters.type
            val matchCategory = filters.cat == null || tx.category == filters.cat
            val matchMethod = filters.method == null || tx.paymentMethod == filters.method
            val matchBank = filters.bankId == null || tx.bankAccountId == filters.bankId || tx.toBankAccountId == filters.bankId
            val matchMonth = filters.month == null || SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(tx.date)) == filters.month
            val matchDate = filters.dateStr == null || SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.date)) == filters.dateStr
            val matchTag = filters.tag == null || run {
                val txTags = parseTags(tx.tagsString)
                val cleanQuery = if (filters.tag.startsWith("#")) filters.tag else "#${filters.tag}"
                txTags.any { it.equals(cleanQuery, ignoreCase = true) }
            }

            matchSearch && matchType && matchCategory && matchMethod && matchBank && matchMonth && matchDate && matchTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            val id = repository.insertTransaction(tx)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "date" to tx.date,
                    "amount" to tx.amount,
                    "category" to tx.category,
                    "subcategory" to tx.subcategory,
                    "paymentMethod" to tx.paymentMethod,
                    "merchant" to tx.merchant,
                    "notes" to tx.notes,
                    "tagsString" to tx.tagsString,
                    "type" to tx.type,
                    "assetId" to tx.assetId,
                    "creditCardId" to tx.creditCardId,
                    "bankAccountId" to tx.bankAccountId
                )
                firestore!!.collection("users").document(user.uid).collection("transactions")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("transactions")
                    .document(tx.id.toString()).delete()
            }
        }
    }

    // PENDING SMS TRANSACTIONS ACTIONS
    fun confirmPendingSms(
        item: PendingSmsTransaction,
        chosenCategory: String,
        chosenSubcategory: String = "",
        chosenPaymentMethod: String = item.paymentMethod,
        chosenCreditCardId: Long? = item.creditCardId,
        chosenBankAccountId: Long? = item.bankAccountId
    ) {
        viewModelScope.launch {
            val newTx = Transaction(
                amount = item.amount,
                category = chosenCategory,
                subcategory = chosenSubcategory,
                paymentMethod = chosenPaymentMethod,
                merchant = item.merchant,
                notes = if (item.rawBody.isNotBlank()) "Auto-detected from SMS: ${item.rawBody.take(120)}" else "Auto-detected from SMS",
                type = item.type,
                date = item.date,
                creditCardId = chosenCreditCardId,
                bankAccountId = chosenBankAccountId
            )
            addTransaction(newTx)
            repository.deletePendingSmsTransaction(item)
        }
    }

    fun dismissPendingSms(item: PendingSmsTransaction) {
        viewModelScope.launch {
            repository.deletePendingSmsTransaction(item)
        }
    }

    fun simulateSmsReceived(sender: String, body: String) {
        viewModelScope.launch {
            val customCats = repository.getAllCustomCategoriesList()
            val parsed = SmsParser.parseSms(sender, body, customCats) ?: return@launch
            val bankList = repository.getAllBankAccountsList()
            val cards = repository.getAllCreditCardsList()

            var matchedBankAccountId: Long? = null
            var matchedCreditCardId: Long? = null
            var finalPaymentMethod = parsed.paymentMethod

            if (parsed.lastFourDigits.isNotBlank()) {
                val matchedCard = cards.firstOrNull { card ->
                    card.lastFourDigits.isNotBlank() && (card.lastFourDigits == parsed.lastFourDigits || card.lastFourDigits.endsWith(parsed.lastFourDigits))
                }
                if (matchedCard != null) {
                    matchedCreditCardId = matchedCard.id
                    finalPaymentMethod = "Credit Card"
                }
            }

            val matchedBank = if (parsed.lastFourDigits.isNotBlank()) {
                bankList.firstOrNull { bank ->
                    bank.accountNumberLast4.isNotBlank() && (bank.accountNumberLast4 == parsed.lastFourDigits || bank.accountNumberLast4.endsWith(parsed.lastFourDigits))
                }
            } else null ?: bankList.firstOrNull { bank ->
                val bankKey = bank.bankName.lowercase().replace("bank", "").trim()
                bankKey.length >= 3 && (sender.lowercase().contains(bankKey) || body.lowercase().contains(bankKey))
            }

            // If bank has SMS detection disabled, ignore!
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

            val insertedId = repository.insertPendingSmsTransaction(pendingTx)
            val txWithId = pendingTx.copy(id = insertedId)
            SmsNotificationHelper.showDetectedNotification(getApplication(), txWithId)
        }
    }

    // ==========================================
    // DATA RESET & SELECTIVE PURGE ACTIONS
    // ==========================================

    fun deleteTransactionsByMonth(yearMonth: String, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.deleteTransactionsByYearMonth(yearMonth)
            sharedPrefs.edit().putBoolean("has_seeded_sample_data", true).apply()
            val user = firebaseAuth?.currentUser
            if (user != null) {
                // Also remove corresponding transactions from firestore
                try {
                    val snapshot = firestore!!.collection("users").document(user.uid)
                        .collection("transactions").get().await()
                    val batch = firestore!!.batch()
                    var batchCount = 0
                    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    for (doc in snapshot.documents) {
                        val dateLong = doc.getLong("date") ?: 0L
                        val docYearMonth = sdf.format(Date(dateLong))
                        if (docYearMonth == yearMonth) {
                            batch.delete(doc.reference)
                            batchCount++
                        }
                    }
                    if (batchCount > 0) {
                        batch.commit().await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onComplete?.invoke(count)
        }
    }

    fun clearAllPendingSmsQueue(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllPendingSmsTransactions()
            onComplete?.invoke()
        }
    }

    fun clearAllTransactionsOnly(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllTransactions()
            sharedPrefs.edit().putBoolean("has_seeded_sample_data", true).apply()
            val user = firebaseAuth?.currentUser
            if (user != null) {
                try {
                    val snapshot = firestore!!.collection("users").document(user.uid)
                        .collection("transactions").get().await()
                    val batch = firestore!!.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onComplete?.invoke()
        }
    }

    fun executeFullFactoryReset(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.fullFactoryReset()
            sharedPrefs.edit().clear().apply()
            sharedPrefs.edit().putBoolean("has_seeded_sample_data", true).apply()
            _isSmsDetectionEnabled.value = true
            _excludeCreditCards.value = false
            
            // Cloud wipe if authenticated
            val user = firebaseAuth?.currentUser
            if (user != null) {
                try {
                    val collections = listOf(
                        "transactions", "credit_cards", "bank_accounts", "assets", "budgets",
                        "subscriptions", "savings_goals", "borrow_lend", "wishlist", "custom_categories"
                    )
                    for (col in collections) {
                        val snap = firestore!!.collection("users").document(user.uid).collection(col).get().await()
                        val batch = firestore!!.batch()
                        for (doc in snap.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onComplete?.invoke()
        }
    }

    fun addCustomCategory(name: String, icon: String, type: String, parentCategory: String? = null) {
        viewModelScope.launch {
            val cat = CustomCategory(
                name = name,
                icon = icon,
                type = type,
                parentCategory = parentCategory
            )
            val id = repository.insertCustomCategory(cat)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to name,
                    "icon" to icon,
                    "type" to type,
                    "parentCategory" to parentCategory
                )
                firestore!!.collection("users").document(user.uid).collection("custom_categories")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun updateCustomCategory(oldCategory: CustomCategory, newCategory: CustomCategory) {
        viewModelScope.launch {
            val id = repository.insertCustomCategory(newCategory)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to newCategory.name,
                    "icon" to newCategory.icon,
                    "type" to newCategory.type,
                    "parentCategory" to newCategory.parentCategory
                )
                firestore!!.collection("users").document(user.uid).collection("custom_categories")
                    .document(id.toString()).set(data)
            }
            
            // If main category name changed, update child subcategories, transactions, and budgets
            if (oldCategory.parentCategory == null && oldCategory.name != newCategory.name) {
                val childSubs = customCategories.value.filter { it.parentCategory == oldCategory.name }
                childSubs.forEach { child ->
                    repository.insertCustomCategory(child.copy(parentCategory = newCategory.name))
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("custom_categories")
                            .document(child.id.toString()).update("parentCategory", newCategory.name)
                    }
                }
                
                // Update transactions
                val txsToUpdate = transactions.value.filter { it.category == oldCategory.name }
                txsToUpdate.forEach { tx ->
                    repository.insertTransaction(tx.copy(category = newCategory.name))
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("category", newCategory.name)
                    }
                }
                
                // Update budgets
                val budgetsToUpdate = budgets.value.filter { it.category == oldCategory.name }
                budgetsToUpdate.forEach { b ->
                    repository.insertBudget(b.copy(category = newCategory.name))
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("budgets")
                            .document(b.id.toString()).update("category", newCategory.name)
                    }
                }
            }
            
            // If subcategory name changed
            if (oldCategory.parentCategory != null && oldCategory.name != newCategory.name) {
                // Update transactions referencing this subcategory
                val txsToUpdate = transactions.value.filter { it.category == oldCategory.parentCategory && it.subcategory == oldCategory.name }
                txsToUpdate.forEach { tx ->
                    repository.insertTransaction(tx.copy(subcategory = newCategory.name))
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("subcategory", newCategory.name)
                    }
                }
            }
        }
    }

    fun deleteCustomCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.deleteCustomCategory(category)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("custom_categories")
                    .document(category.id.toString()).delete()
            }
            if (category.parentCategory == null) {
                // If it's a parent category, delete all custom subcategories that have this as parent
                val childSubs = customCategories.value.filter { it.parentCategory == category.name }
                childSubs.forEach { child ->
                    repository.deleteCustomCategory(child)
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("custom_categories")
                            .document(child.id.toString()).delete()
                    }
                }
                
                // Delete budgets for this category
                val budgetsToDelete = budgets.value.filter { it.category == category.name }
                budgetsToDelete.forEach { b ->
                    repository.deleteBudget(b)
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("budgets")
                            .document(b.id.toString()).delete()
                    }
                }
            }
        }
    }

    fun getIconForCategory(category: String): String {
        val custom = customCategories.value.firstOrNull { it.name.equals(category, ignoreCase = true) && it.parentCategory == null }
        if (custom != null) return custom.icon
        return CategoryData.getIconForCategory(category)
    }

    fun addAsset(asset: Asset, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertAsset(asset)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to asset.name,
                    "type" to asset.type,
                    "purchaseDate" to asset.purchaseDate,
                    "purchasePrice" to asset.purchasePrice,
                    "insuranceDetails" to asset.insuranceDetails,
                    "warrantyDetails" to asset.warrantyDetails,
                    "notes" to asset.notes
                )
                firestore!!.collection("users").document(user.uid).collection("assets")
                    .document(id.toString()).set(data)
            }
            onComplete?.invoke(id)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("assets")
                    .document(asset.id.toString()).delete()
            }
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            val id = repository.insertBudget(budget)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "category" to budget.category,
                    "limitAmount" to budget.limitAmount,
                    "monthYear" to budget.monthYear
                )
                firestore!!.collection("users").document(user.uid).collection("budgets")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("budgets")
                    .document(budget.id.toString()).delete()
            }
        }
    }

    fun addSubscription(sub: Subscription) {
        viewModelScope.launch {
            val id = repository.insertSubscription(sub)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to sub.name,
                    "cost" to sub.cost,
                    "renewalDate" to sub.renewalDate,
                    "billingCycle" to sub.billingCycle,
                    "isActive" to sub.isActive,
                    "category" to sub.category,
                    "notes" to sub.notes,
                    "isAutoRenew" to sub.isAutoRenew,
                    "reminderDaysInAdvance" to sub.reminderDaysInAdvance,
                    "paymentAccountId" to (sub.paymentAccountId ?: 0L),
                    "paymentCardId" to (sub.paymentCardId ?: 0L),
                    "paymentMethodName" to sub.paymentMethodName,
                    "lastPaidDate" to (sub.lastPaidDate ?: 0L)
                )
                firestore!!.collection("users").document(user.uid).collection("subscriptions")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun toggleSubscriptionActive(sub: Subscription) {
        viewModelScope.launch {
            val updated = sub.copy(isActive = !sub.isActive)
            repository.insertSubscription(updated)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).update("isActive", updated.isActive)
            }
        }
    }

    fun toggleSubscriptionAutoRenew(sub: Subscription) {
        viewModelScope.launch {
            val updated = sub.copy(isAutoRenew = !sub.isAutoRenew)
            repository.insertSubscription(updated)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).update("isAutoRenew", updated.isAutoRenew)
            }
        }
    }

    fun updateSubscriptionReminderDays(sub: Subscription, days: Int) {
        viewModelScope.launch {
            val updated = sub.copy(reminderDaysInAdvance = days)
            repository.insertSubscription(updated)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).update("reminderDaysInAdvance", days)
            }
        }
    }

    fun advanceSubscriptionRenewal(
        sub: Subscription,
        recordPaymentTransaction: Boolean = true,
        paymentAccountId: Long? = null,
        paymentCardId: Long? = null,
        paymentMethod: String = "Bank"
    ) {
        viewModelScope.launch {
            val nextRenewal = SubscriptionNotificationHelper.getNextCycleTimestamp(sub.renewalDate, sub.billingCycle)
            val updatedSub = sub.copy(
                renewalDate = nextRenewal,
                lastPaidDate = System.currentTimeMillis()
            )
            repository.insertSubscription(updatedSub)

            if (recordPaymentTransaction) {
                val tx = Transaction(
                    date = System.currentTimeMillis(),
                    amount = sub.cost,
                    category = if (sub.category.isNotBlank()) sub.category else "Subscriptions",
                    subcategory = "Auto-Renewal",
                    paymentMethod = paymentMethod,
                    merchant = sub.name,
                    notes = "Recorded subscription payment for ${sub.name} (Next renewal: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(nextRenewal))})",
                    tagsString = "Subscription,AutoRenew",
                    type = "EXPENSE",
                    bankAccountId = paymentAccountId ?: sub.paymentAccountId,
                    creditCardId = paymentCardId ?: sub.paymentCardId
                )
                repository.insertTransaction(tx)
            }

            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).update(
                        mapOf(
                            "renewalDate" to nextRenewal,
                            "lastPaidDate" to updatedSub.lastPaidDate
                        )
                    )
            }
        }
    }

    fun triggerUpcomingSubscriptionCheck(context: Context) {
        viewModelScope.launch {
            val subs = repository.allSubscriptions.first()
            SubscriptionNotificationHelper.checkAndTriggerRenewalAlerts(context, subs)
        }
    }

    fun testSubscriptionAlert(context: Context, sub: Subscription) {
        SubscriptionNotificationHelper.testRenewalNotification(context, sub)
    }

    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(sub)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).delete()
            }
        }
    }

    fun addSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val id = repository.insertSavingsGoal(goal)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to goal.name,
                    "targetAmount" to goal.targetAmount,
                    "currentAmount" to goal.currentAmount,
                    "targetDate" to goal.targetDate,
                    "notes" to goal.notes
                )
                firestore!!.collection("users").document(user.uid).collection("savings_goals")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun updateSavingsGoalProgress(goal: SavingsGoal, addAmount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + addAmount)
            repository.insertSavingsGoal(updated)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("savings_goals")
                    .document(goal.id.toString()).update("currentAmount", updated.currentAmount)
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("savings_goals")
                    .document(goal.id.toString()).delete()
            }
        }
    }

    fun addBorrowLend(item: BorrowLend) {
        viewModelScope.launch {
            val id = repository.insertBorrowLend(item)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "contactName" to item.contactName,
                    "amount" to item.amount,
                    "type" to item.type,
                    "dueDate" to item.dueDate,
                    "isPaid" to item.isPaid,
                    "notes" to item.notes
                )
                firestore!!.collection("users").document(user.uid).collection("borrow_lend")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun toggleBorrowLendPaid(item: BorrowLend) {
        viewModelScope.launch {
            val updated = item.copy(isPaid = !item.isPaid)
            repository.insertBorrowLend(updated)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("borrow_lend")
                    .document(item.id.toString()).update("isPaid", updated.isPaid)
            }
        }
    }

    fun deleteBorrowLend(item: BorrowLend) {
        viewModelScope.launch {
            repository.deleteBorrowLend(item)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("borrow_lend")
                    .document(item.id.toString()).delete()
            }
        }
    }

    fun addWishlistItem(item: Wishlist) {
        viewModelScope.launch {
            val id = repository.insertWishlistItem(item)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to item.name,
                    "price" to item.price,
                    "priority" to item.priority,
                    "targetDate" to item.targetDate,
                    "notes" to item.notes,
                    "isPurchased" to item.isPurchased,
                    "purchasedTransactionId" to item.purchasedTransactionId
                )
                firestore!!.collection("users").document(user.uid).collection("wishlist")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun toggleWishlistItemPurchased(item: Wishlist) {
        viewModelScope.launch {
            val willBePurchased = !item.isPurchased
            if (willBePurchased) {
                // Record expense in transaction ledger and link its id to the wishlist item
                val newTx = Transaction(
                    amount = item.price,
                    category = "Shopping",
                    subcategory = "Wishlist",
                    paymentMethod = "UPI",
                    merchant = item.name,
                    notes = if (item.notes.isNotBlank()) "Purchased from Wishlist: ${item.notes}" else "Purchased from Wishlist",
                    type = "EXPENSE",
                    date = System.currentTimeMillis()
                )
                val txId = repository.insertTransaction(newTx)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    val txData = mapOf(
                        "id" to txId,
                        "date" to newTx.date,
                        "amount" to newTx.amount,
                        "category" to newTx.category,
                        "subcategory" to newTx.subcategory,
                        "paymentMethod" to newTx.paymentMethod,
                        "merchant" to newTx.merchant,
                        "notes" to newTx.notes,
                        "tagsString" to newTx.tagsString,
                        "type" to newTx.type,
                        "assetId" to newTx.assetId,
                        "creditCardId" to newTx.creditCardId,
                        "bankAccountId" to newTx.bankAccountId
                    )
                    firestore!!.collection("users").document(user.uid).collection("transactions")
                        .document(txId.toString()).set(txData)
                }

                val updated = item.copy(isPurchased = true, purchasedTransactionId = txId)
                repository.insertWishlistItem(updated)
                if (user != null) {
                    val wishData = mapOf(
                        "id" to updated.id,
                        "name" to updated.name,
                        "price" to updated.price,
                        "priority" to updated.priority,
                        "targetDate" to updated.targetDate,
                        "notes" to updated.notes,
                        "isPurchased" to true,
                        "purchasedTransactionId" to txId
                    )
                    firestore!!.collection("users").document(user.uid).collection("wishlist")
                        .document(item.id.toString()).set(wishData)
                }
            } else {
                // Moving back to Active Wish: Remove the linked expense from transactions & firestore
                val linkedTxId = item.purchasedTransactionId
                if (linkedTxId != null && linkedTxId > 0L) {
                    repository.deleteTransactionsByIds(listOf(linkedTxId))
                    val user = firebaseAuth?.currentUser
                    if (user != null) {
                        firestore!!.collection("users").document(user.uid).collection("transactions")
                            .document(linkedTxId.toString()).delete()
                    }
                }

                val updated = item.copy(isPurchased = false, purchasedTransactionId = null)
                repository.insertWishlistItem(updated)
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    val wishData = mapOf(
                        "id" to updated.id,
                        "name" to updated.name,
                        "price" to updated.price,
                        "priority" to updated.priority,
                        "targetDate" to updated.targetDate,
                        "notes" to updated.notes,
                        "isPurchased" to false,
                        "purchasedTransactionId" to null
                    )
                    firestore!!.collection("users").document(user.uid).collection("wishlist")
                        .document(item.id.toString()).set(wishData)
                }
            }
        }
    }

    fun deleteWishlistItem(item: Wishlist, deleteLinkedExpense: Boolean = true) {
        viewModelScope.launch {
            if (deleteLinkedExpense && item.purchasedTransactionId != null && item.purchasedTransactionId > 0L) {
                repository.deleteTransactionsByIds(listOf(item.purchasedTransactionId))
                val user = firebaseAuth?.currentUser
                if (user != null) {
                    firestore!!.collection("users").document(user.uid).collection("transactions")
                        .document(item.purchasedTransactionId.toString()).delete()
                }
            }
            repository.deleteWishlistItem(item)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("wishlist")
                    .document(item.id.toString()).delete()
            }
        }
    }

    fun addCreditCard(card: CreditCard) {
        viewModelScope.launch {
            val id = repository.insertCreditCard(card)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "cardName" to card.cardName,
                    "billingDay" to card.billingDay,
                    "dueDay" to card.dueDay,
                    "cardLimit" to card.cardLimit,
                    "lastFourDigits" to card.lastFourDigits
                )
                firestore!!.collection("users").document(user.uid).collection("credit_cards")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("credit_cards")
                    .document(card.id.toString()).delete()
            }
        }
    }

    // CREDIT CARD CALCULATIONS WITH REFUND & PAYMENT OFFSET
    fun calculateCreditCardOutstanding(cardId: Long, transactions: List<Transaction>): Double {
        val expenses = transactions.filter { it.creditCardId == cardId && it.type == "EXPENSE" }.sumOf { it.amount }
        val refundsAndCredits = transactions.filter {
            (it.creditCardId == cardId && (it.type == "REFUND" || it.type == "INCOME")) ||
            (it.toCreditCardId == cardId && it.type == "TRANSFER")
        }.sumOf { it.amount }
        return maxOf(0.0, expenses - refundsAndCredits)
    }

    fun calculateCreditCardAvailableLimit(card: CreditCard, transactions: List<Transaction>): Double {
        val outstanding = calculateCreditCardOutstanding(card.id, transactions)
        return maxOf(0.0, card.cardLimit - outstanding)
    }

    fun calculateCreditCardUtilization(card: CreditCard, transactions: List<Transaction>): Double {
        if (card.cardLimit <= 0) return 0.0
        val outstanding = calculateCreditCardOutstanding(card.id, transactions)
        return (outstanding / card.cardLimit).coerceIn(0.0, 1.0)
    }

    // BANK ACCOUNTS ACTIONS
    fun addBankAccount(bank: BankAccount) {
        viewModelScope.launch {
            val id = repository.insertBankAccount(bank)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "bankName" to bank.bankName,
                    "accountNickname" to bank.accountNickname,
                    "accountNumberLast4" to bank.accountNumberLast4,
                    "accountType" to bank.accountType,
                    "initialBalance" to bank.initialBalance,
                    "isSmsDetectionEnabled" to bank.isSmsDetectionEnabled,
                    "isHiddenFromSummary" to bank.isHiddenFromSummary
                )
                firestore!!.collection("users").document(user.uid).collection("bank_accounts")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun updateBankAccount(bank: BankAccount) {
        viewModelScope.launch {
            repository.insertBankAccount(bank)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to bank.id,
                    "bankName" to bank.bankName,
                    "accountNickname" to bank.accountNickname,
                    "accountNumberLast4" to bank.accountNumberLast4,
                    "accountType" to bank.accountType,
                    "initialBalance" to bank.initialBalance,
                    "isSmsDetectionEnabled" to bank.isSmsDetectionEnabled,
                    "isHiddenFromSummary" to bank.isHiddenFromSummary
                )
                firestore!!.collection("users").document(user.uid).collection("bank_accounts")
                    .document(bank.id.toString()).set(data)
            }
        }
    }

    fun deleteBankAccount(bank: BankAccount) {
        viewModelScope.launch {
            repository.deleteBankAccount(bank)
            val user = firebaseAuth?.currentUser
            if (user != null) {
                firestore!!.collection("users").document(user.uid).collection("bank_accounts")
                    .document(bank.id.toString()).delete()
            }
        }
    }

    fun toggleBankSmsDetection(bank: BankAccount) {
        updateBankAccount(bank.copy(isSmsDetectionEnabled = !bank.isSmsDetectionEnabled))
    }

    fun toggleBankHideFromSummary(bank: BankAccount) {
        updateBankAccount(bank.copy(isHiddenFromSummary = !bank.isHiddenFromSummary))
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onFailure("Firebase is not configured or initialized")
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
                syncDataWithCloud()
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Sign in failed")
            }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onFailure("Firebase is not configured or initialized")
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
                syncDataWithCloud()
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Registration failed")
            }
    }

    fun logout() {
        firebaseAuth?.signOut()
    }

    fun syncDataWithCloud() {
        val user = firebaseAuth?.currentUser ?: return
        val store = firestore ?: return
        val userId = user.uid
        val demoEntityIds = sharedPrefs.getStringSet("demo_entity_ids", emptySet()) ?: emptySet()
        viewModelScope.launch {
            try {
                // 1. Transactions
                val localTransactions = repository.allTransactions.first()
                val transactionsRef = store.collection("users").document(userId).collection("transactions")
                transactionsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudTransactionsMap = snapshot.documents.associateBy { it.id }
                        localTransactions.forEach { local ->
                            // Never upload demo transactions to cloud
                            if (local.tagsString.contains("Demo", ignoreCase = true) ||
                                local.notes.contains("[Demo Data]", ignoreCase = true) ||
                                demoEntityIds.contains("tx_${local.id}")
                            ) {
                                return@forEach
                            }
                            val cloudDoc = cloudTransactionsMap[local.id.toString()]
                            if (cloudDoc == null) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "date" to local.date,
                                    "amount" to local.amount,
                                    "category" to local.category,
                                    "subcategory" to local.subcategory,
                                    "paymentMethod" to local.paymentMethod,
                                    "merchant" to local.merchant,
                                    "notes" to local.notes,
                                    "tagsString" to local.tagsString,
                                    "type" to local.type,
                                    "assetId" to local.assetId,
                                    "creditCardId" to local.creditCardId,
                                    "bankAccountId" to local.bankAccountId,
                                    "toBankAccountId" to local.toBankAccountId,
                                    "toCreditCardId" to local.toCreditCardId
                                )
                                transactionsRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            val exists = localTransactions.any { it.id == id }
                            if (!exists) {
                                val transaction = Transaction(
                                    id = id,
                                    date = doc.getLong("date") ?: System.currentTimeMillis(),
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    category = doc.getString("category") ?: "",
                                    subcategory = doc.getString("subcategory") ?: "",
                                    paymentMethod = doc.getString("paymentMethod") ?: "Cash",
                                    merchant = doc.getString("merchant") ?: "",
                                    notes = doc.getString("notes") ?: "",
                                    tagsString = doc.getString("tagsString") ?: "",
                                    type = doc.getString("type") ?: "EXPENSE",
                                    assetId = doc.getLong("assetId"),
                                    creditCardId = doc.getLong("creditCardId"),
                                    bankAccountId = doc.getLong("bankAccountId"),
                                    toBankAccountId = doc.getLong("toBankAccountId"),
                                    toCreditCardId = doc.getLong("toCreditCardId")
                                )
                                viewModelScope.launch {
                                    repository.insertTransaction(transaction)
                                }
                            }
                        }
                    }
                }

                // 2. Budgets
                val localBudgets = repository.allBudgets.first()
                val budgetsRef = store.collection("users").document(userId).collection("budgets")
                budgetsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localBudgets.forEach { local ->
                            if (demoEntityIds.contains("budget_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "category" to local.category,
                                    "limitAmount" to local.limitAmount,
                                    "monthYear" to local.monthYear
                                )
                                budgetsRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localBudgets.none { it.id == id }) {
                                val budget = Budget(
                                    id = id,
                                    category = doc.getString("category") ?: "",
                                    limitAmount = doc.getDouble("limitAmount") ?: 0.0,
                                    monthYear = doc.getString("monthYear") ?: ""
                                )
                                viewModelScope.launch { repository.insertBudget(budget) }
                            }
                        }
                    }
                }

                // 3. Assets
                val localAssets = repository.allAssets.first()
                val assetsRef = store.collection("users").document(userId).collection("assets")
                assetsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localAssets.forEach { local ->
                            if (local.notes.contains("[Demo Data]", ignoreCase = true) || demoEntityIds.contains("asset_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "name" to local.name,
                                    "type" to local.type,
                                    "purchaseDate" to local.purchaseDate,
                                    "purchasePrice" to local.purchasePrice,
                                    "insuranceDetails" to local.insuranceDetails,
                                    "warrantyDetails" to local.warrantyDetails,
                                    "notes" to local.notes
                                )
                                assetsRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localAssets.none { it.id == id }) {
                                val asset = Asset(
                                    id = id,
                                    name = doc.getString("name") ?: "",
                                    type = doc.getString("type") ?: "VEHICLE",
                                    purchaseDate = doc.getLong("purchaseDate") ?: 0L,
                                    purchasePrice = doc.getDouble("purchasePrice") ?: 0.0,
                                    insuranceDetails = doc.getString("insuranceDetails") ?: "",
                                    warrantyDetails = doc.getString("warrantyDetails") ?: "",
                                    notes = doc.getString("notes") ?: ""
                                )
                                viewModelScope.launch { repository.insertAsset(asset) }
                            }
                        }
                    }
                }

                // 4. Subscriptions
                val localSubs = repository.allSubscriptions.first()
                val subsRef = store.collection("users").document(userId).collection("subscriptions")
                subsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localSubs.forEach { local ->
                            if (local.notes.contains("[Demo Data]", ignoreCase = true) || demoEntityIds.contains("sub_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "name" to local.name,
                                    "cost" to local.cost,
                                    "renewalDate" to local.renewalDate,
                                    "billingCycle" to local.billingCycle,
                                    "isActive" to local.isActive,
                                    "category" to local.category,
                                    "notes" to local.notes,
                                    "isAutoRenew" to local.isAutoRenew,
                                    "reminderDaysInAdvance" to local.reminderDaysInAdvance,
                                    "paymentAccountId" to (local.paymentAccountId ?: 0L),
                                    "paymentCardId" to (local.paymentCardId ?: 0L),
                                    "paymentMethodName" to local.paymentMethodName,
                                    "lastPaidDate" to (local.lastPaidDate ?: 0L)
                                )
                                subsRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localSubs.none { it.id == id }) {
                                val sub = Subscription(
                                    id = id,
                                    name = doc.getString("name") ?: "",
                                    cost = doc.getDouble("cost") ?: 0.0,
                                    renewalDate = doc.getLong("renewalDate") ?: 0L,
                                    billingCycle = doc.getString("billingCycle") ?: "MONTHLY",
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    category = doc.getString("category") ?: "",
                                    notes = doc.getString("notes") ?: "",
                                    isAutoRenew = doc.getBoolean("isAutoRenew") ?: true,
                                    reminderDaysInAdvance = (doc.getLong("reminderDaysInAdvance") ?: 2L).toInt(),
                                    paymentAccountId = doc.getLong("paymentAccountId")?.takeIf { it > 0L },
                                    paymentCardId = doc.getLong("paymentCardId")?.takeIf { it > 0L },
                                    paymentMethodName = doc.getString("paymentMethodName") ?: "",
                                    lastPaidDate = doc.getLong("lastPaidDate")?.takeIf { it > 0L }
                                )
                                viewModelScope.launch { repository.insertSubscription(sub) }
                            }
                        }
                    }
                }

                // 5. Savings Goals
                val localGoals = repository.allSavingsGoals.first()
                val goalsRef = store.collection("users").document(userId).collection("savings_goals")
                goalsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localGoals.forEach { local ->
                            if (local.notes.contains("[Demo Data]", ignoreCase = true) || demoEntityIds.contains("goal_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "name" to local.name,
                                    "targetAmount" to local.targetAmount,
                                    "currentAmount" to local.currentAmount,
                                    "targetDate" to local.targetDate,
                                    "notes" to local.notes
                                )
                                goalsRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localGoals.none { it.id == id }) {
                                val goal = SavingsGoal(
                                    id = id,
                                    name = doc.getString("name") ?: "",
                                    targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                                    currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                                    targetDate = doc.getLong("targetDate") ?: 0L,
                                    notes = doc.getString("notes") ?: ""
                                )
                                viewModelScope.launch { repository.insertSavingsGoal(goal) }
                            }
                        }
                    }
                }

                // 6. Borrow Lend
                val localBL = repository.allBorrowLends.first()
                val blRef = store.collection("users").document(userId).collection("borrow_lend")
                blRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localBL.forEach { local ->
                            if (local.notes.contains("[Demo Data]", ignoreCase = true) || demoEntityIds.contains("bl_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "contactName" to local.contactName,
                                    "amount" to local.amount,
                                    "type" to local.type,
                                    "dueDate" to local.dueDate,
                                    "isPaid" to local.isPaid,
                                    "notes" to local.notes
                                )
                                blRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localBL.none { it.id == id }) {
                                val bl = BorrowLend(
                                    id = id,
                                    contactName = doc.getString("contactName") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    type = doc.getString("type") ?: "LENT",
                                    dueDate = doc.getLong("dueDate") ?: 0L,
                                    isPaid = doc.getBoolean("isPaid") ?: false,
                                    notes = doc.getString("notes") ?: ""
                                )
                                viewModelScope.launch { repository.insertBorrowLend(bl) }
                            }
                        }
                    }
                }

                // 7. Wishlist
                val localWishlist = repository.allWishlistItems.first()
                val wishlistRef = store.collection("users").document(userId).collection("wishlist")
                wishlistRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localWishlist.forEach { local ->
                            if (local.notes.contains("[Demo Data]", ignoreCase = true) || demoEntityIds.contains("wish_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "name" to local.name,
                                    "price" to local.price,
                                    "priority" to local.priority,
                                    "targetDate" to local.targetDate,
                                    "notes" to local.notes,
                                    "isPurchased" to local.isPurchased
                                )
                                wishlistRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localWishlist.none { it.id == id }) {
                                val wish = Wishlist(
                                    id = id,
                                    name = doc.getString("name") ?: "",
                                    price = doc.getDouble("price") ?: 0.0,
                                    priority = doc.getString("priority") ?: "MEDIUM",
                                    targetDate = doc.getLong("targetDate") ?: 0L,
                                    notes = doc.getString("notes") ?: "",
                                    isPurchased = doc.getBoolean("isPurchased") ?: false,
                                    purchasedTransactionId = doc.getLong("purchasedTransactionId")
                                )
                                viewModelScope.launch { repository.insertWishlistItem(wish) }
                            }
                        }
                    }
                }

                // 8. Custom Categories
                val localCategories = repository.allCustomCategories.first()
                val categoriesRef = store.collection("users").document(userId).collection("custom_categories")
                categoriesRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localCategories.forEach { local ->
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "name" to local.name,
                                    "icon" to local.icon,
                                    "type" to local.type,
                                    "parentCategory" to local.parentCategory
                                )
                                categoriesRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localCategories.none { it.id == id }) {
                                val cat = CustomCategory(
                                    id = id,
                                    name = doc.getString("name") ?: "",
                                    icon = doc.getString("icon") ?: "",
                                    type = doc.getString("type") ?: "EXPENSE",
                                    parentCategory = doc.getString("parentCategory")
                                )
                                viewModelScope.launch { repository.insertCustomCategory(cat) }
                            }
                        }
                    }
                }

                // 9. Credit Cards
                val localCards = repository.allCreditCards.first()
                val cardsRef = store.collection("users").document(userId).collection("credit_cards")
                cardsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localCards.forEach { local ->
                            if (local.cardName.contains("(Demo)", ignoreCase = true) || demoEntityIds.contains("card_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "cardName" to local.cardName,
                                    "billingDay" to local.billingDay,
                                    "dueDay" to local.dueDay,
                                    "cardLimit" to local.cardLimit,
                                    "lastFourDigits" to local.lastFourDigits
                                )
                                cardsRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localCards.none { it.id == id }) {
                                val card = CreditCard(
                                    id = id,
                                    cardName = doc.getString("cardName") ?: "",
                                    billingDay = doc.getLong("billingDay")?.toInt() ?: 20,
                                    dueDay = doc.getLong("dueDay")?.toInt() ?: 9,
                                    cardLimit = doc.getDouble("cardLimit") ?: 0.0,
                                    lastFourDigits = doc.getString("lastFourDigits") ?: ""
                                )
                                viewModelScope.launch { repository.insertCreditCard(card) }
                            }
                        }
                    }
                }

                // 10. Bank Accounts
                val localBanks = repository.allBankAccounts.first()
                val banksRef = store.collection("users").document(userId).collection("bank_accounts")
                banksRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localBanks.forEach { local ->
                            if (local.accountNickname.contains("(Demo)", ignoreCase = true) || demoEntityIds.contains("bank_${local.id}")) return@forEach
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "bankName" to local.bankName,
                                    "accountNickname" to local.accountNickname,
                                    "accountNumberLast4" to local.accountNumberLast4,
                                    "accountType" to local.accountType,
                                    "initialBalance" to local.initialBalance,
                                    "isSmsDetectionEnabled" to local.isSmsDetectionEnabled,
                                    "isHiddenFromSummary" to local.isHiddenFromSummary
                                )
                                banksRef.document(local.id.toString()).set(data)
                            }
                        }
                        snapshot.documents.forEach { doc ->
                            val id = doc.getLong("id") ?: return@forEach
                            if (localBanks.none { it.id == id }) {
                                val bank = BankAccount(
                                    id = id,
                                    bankName = doc.getString("bankName") ?: "",
                                    accountNickname = doc.getString("accountNickname") ?: "",
                                    accountNumberLast4 = doc.getString("accountNumberLast4") ?: "",
                                    accountType = doc.getString("accountType") ?: "SAVINGS",
                                    initialBalance = doc.getDouble("initialBalance") ?: 0.0,
                                    isSmsDetectionEnabled = doc.getBoolean("isSmsDetectionEnabled") ?: true,
                                    isHiddenFromSummary = doc.getBoolean("isHiddenFromSummary") ?: false
                                )
                                viewModelScope.launch { repository.insertBankAccount(bank) }
                            }
                        }
                    }
                }

                // 11. User Preferences & Settings (Cash-Only Mode, SMS Detection & Number Format)
                val settingsRef = store.collection("users").document(userId).collection("settings").document("preferences")
                settingsRef.get().addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val cloudCashOnly = doc.getBoolean("cashOnlyMode")
                        if (cloudCashOnly != null) {
                            _excludeCreditCards.value = cloudCashOnly
                            sharedPrefs.edit().putBoolean("cash_only_mode", cloudCashOnly).apply()
                        }
                        val cloudSmsEnabled = doc.getBoolean("smsDetectionEnabled")
                        if (cloudSmsEnabled != null) {
                            _isSmsDetectionEnabled.value = cloudSmsEnabled
                            sharedPrefs.edit().putBoolean("sms_detection_enabled", cloudSmsEnabled).apply()
                        }
                        val cloudNumberFormat = doc.getString("numberFormatPreference")
                        if (!cloudNumberFormat.isNullOrBlank()) {
                            _numberFormatPreference.value = cloudNumberFormat
                            NumberFormatConfig.activePreference = cloudNumberFormat
                            sharedPrefs.edit().putString("number_format_preference", cloudNumberFormat).apply()
                        }
                        val cloudCurrencySymbol = doc.getString("currencySymbol")
                        if (!cloudCurrencySymbol.isNullOrBlank()) {
                            _currencySymbol.value = cloudCurrencySymbol
                            NumberFormatConfig.currencySymbol = cloudCurrencySymbol
                            sharedPrefs.edit().putString("currency_symbol", cloudCurrencySymbol).apply()
                        }
                        val cloudCurrencyCode = doc.getString("currencyCode")
                        if (!cloudCurrencyCode.isNullOrBlank()) {
                            _currencyCode.value = cloudCurrencyCode
                            NumberFormatConfig.currencyCode = cloudCurrencyCode
                            sharedPrefs.edit().putString("currency_code", cloudCurrencyCode).apply()
                        }
                    } else {
                        val initialPrefData = mapOf(
                            "cashOnlyMode" to _excludeCreditCards.value,
                            "smsDetectionEnabled" to _isSmsDetectionEnabled.value,
                            "numberFormatPreference" to _numberFormatPreference.value,
                            "currencySymbol" to _currencySymbol.value,
                            "currencyCode" to _currencyCode.value,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        settingsRef.set(initialPrefData, com.google.firebase.firestore.SetOptions.merge())
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // METRICS & COMPUTED PROPERTIES

    // Today's spending (Net of refunds, transfers completely excluded)
    val todaySpending = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val normalExpenses = if (exclude) {
            val exps = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= todayStart }.sumOf { it.amount }
            val cardPayments = visibleList.filter { it.category == "Credit Card Payment" && it.date >= todayStart }.sumOf { it.amount }
            exps + cardPayments
        } else {
            visibleList.filter { it.type == "EXPENSE" && it.date >= todayStart }.sumOf { it.amount }
        }
        val refunds = visibleList.filter { it.type == "REFUND" && it.date >= todayStart }.sumOf { it.amount }
        maxOf(0.0, normalExpenses - refunds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // This month's spending (Net of refunds, transfers completely excluded)
    val thisMonthSpending = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val normalExpenses = if (exclude) {
            val exps = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }.sumOf { it.amount }
            val cardPayments = visibleList.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.sumOf { it.amount }
            exps + cardPayments
        } else {
            visibleList.filter { it.type == "EXPENSE" && it.date >= monthStart }.sumOf { it.amount }
        }
        val refunds = visibleList.filter { it.type == "REFUND" && it.date >= monthStart }.sumOf { it.amount }
        maxOf(0.0, normalExpenses - refunds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // This month's income (Transfers & Refunds are NOT income)
    val thisMonthIncome = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, _ ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Exclude Credit Card Payment, Transfers and Refunds from monthly income
        visibleList.filter { it.type == "INCOME" && it.category != "Credit Card Payment" && it.date >= monthStart }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Average daily spending for the current month
    val averageDailySpending = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val monthStart = cal.timeInMillis
        
        val totalSpentThisMonth = if (exclude) {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }.sumOf { it.amount }
            val cardPayments = visibleList.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.sumOf { it.amount }
            val refunds = visibleList.filter { it.type == "REFUND" && it.date >= monthStart }.sumOf { it.amount }
            maxOf(0.0, normalExpenses + cardPayments - refunds)
        } else {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.date >= monthStart }.sumOf { it.amount }
            val refunds = visibleList.filter { it.type == "REFUND" && it.date >= monthStart }.sumOf { it.amount }
            maxOf(0.0, normalExpenses - refunds)
        }
        if (currentDay > 0) totalSpentThisMonth / currentDay else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Compare this month vs last month spending
    val percentSpendingChange = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val cal = Calendar.getInstance()
        
        // This month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val thisMonthStart = cal.timeInMillis
        val thisMonthSum = if (exclude) {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= thisMonthStart }.sumOf { it.amount }
            val cardPayments = visibleList.filter { it.category == "Credit Card Payment" && it.date >= thisMonthStart }.sumOf { it.amount }
            val refunds = visibleList.filter { it.type == "REFUND" && it.date >= thisMonthStart }.sumOf { it.amount }
            maxOf(0.0, normalExpenses + cardPayments - refunds)
        } else {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.date >= thisMonthStart }.sumOf { it.amount }
            val refunds = visibleList.filter { it.type == "REFUND" && it.date >= thisMonthStart }.sumOf { it.amount }
            maxOf(0.0, normalExpenses - refunds)
        }
        
        // Last month
        cal.add(Calendar.MONTH, -1)
        val lastMonthStart = cal.timeInMillis
        val lastMonthSum = if (exclude) {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            val cardPayments = visibleList.filter { it.category == "Credit Card Payment" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            val refunds = visibleList.filter { it.type == "REFUND" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            maxOf(0.0, normalExpenses + cardPayments - refunds)
        } else {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            val refunds = visibleList.filter { it.type == "REFUND" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            maxOf(0.0, normalExpenses - refunds)
        }
        
        if (lastMonthSum == 0.0) {
            0.0
        } else {
            ((thisMonthSum - lastMonthSum) / lastMonthSum) * 100.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category spending chart data (Offset by refunds in the same category)
    val categorySpendingData = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        
        val filteredExpenses = if (exclude) {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }
            val cardPaymentsMapped = visibleList.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.map {
                it.copy(type = "EXPENSE")
            }
            normalExpenses + cardPaymentsMapped
        } else {
            visibleList.filter { it.type == "EXPENSE" && it.date >= monthStart }
        }
        
        val expenseMap = filteredExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val refundMap = visibleList.filter { it.type == "REFUND" && it.date >= monthStart }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val allCategories = expenseMap.keys + refundMap.keys
        allCategories.associateWith { cat ->
            val exp = expenseMap[cat] ?: 0.0
            val ref = refundMap[cat] ?: 0.0
            maxOf(0.0, exp - ref)
        }.filterValues { it > 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Monthly category spending for Budgets (Excluding transactions from accounts hidden from global summary/analytics, restricted to current month)
    val budgetCategorySpendings = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val filteredExpenses = if (exclude) {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }
            val cardPaymentsMapped = visibleList.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.map {
                it.copy(type = "EXPENSE")
            }
            normalExpenses + cardPaymentsMapped
        } else {
            visibleList.filter { it.type == "EXPENSE" && it.date >= monthStart }
        }

        val expenseMap = filteredExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val refundMap = visibleList.filter { it.type == "REFUND" && it.date >= monthStart }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val allCategories = expenseMap.keys + refundMap.keys
        allCategories.associateWith { cat ->
            val exp = expenseMap[cat] ?: 0.0
            val ref = refundMap[cat] ?: 0.0
            maxOf(0.0, exp - ref)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Highest expense of the month
    val highestExpenseOfMonth = combine(transactions, bankAccounts, excludeCreditCards) { list, banks, exclude ->
        val hiddenBankIds = banks.filter { it.isHiddenFromSummary }.map { it.id }.toSet()
        val visibleList = list.filter { it.bankAccountId == null || it.bankAccountId !in hiddenBankIds }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        
        val filteredList = if (exclude) {
            val normalExpenses = visibleList.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }
            val cardPaymentsMapped = visibleList.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.map {
                it.copy(type = "EXPENSE")
            }
            normalExpenses + cardPaymentsMapped
        } else {
            visibleList.filter { it.type == "EXPENSE" && it.date >= monthStart }
        }
        
        filteredList.maxByOrNull { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Upcoming reminders
    val upcomingReminders = combine(
        subscriptions, borrowLends
    ) { subs, borrows ->
        val reminders = mutableListOf<ReminderItem>()
        val now = System.currentTimeMillis()
        val thirtyDaysFromNow = now + (30L * 24 * 60 * 60 * 1000)

        // Subscription renewals in next 30 days or overdue
        subs.filter { it.isActive }.forEach { sub ->
            if (sub.renewalDate < thirtyDaysFromNow) {
                reminders.add(
                    ReminderItem(
                        id = "sub_${sub.id}",
                        title = "${sub.name} Subscription Renewal",
                        dueDate = sub.renewalDate,
                        amount = sub.cost,
                        type = "SUBSCRIPTION",
                        notes = "Renewal cost: ${NumberFormatConfig.currencySymbol}${sub.cost}"
                    )
                )
            }
        }

        // Borrow Lends upcoming in next 30 days or unpaid
        borrows.filter { !it.isPaid }.forEach { bl ->
            if (bl.dueDate < thirtyDaysFromNow || bl.dueDate < now) {
                reminders.add(
                    ReminderItem(
                        id = "bl_${bl.id}",
                        title = if (bl.type == "LENT") "Collect from ${bl.contactName}" else "Pay ${bl.contactName}",
                        dueDate = bl.dueDate,
                        amount = bl.amount,
                        type = bl.type,
                        notes = bl.notes
                    )
                )
            }
        }

        reminders.sortedBy { it.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadSampleDemoData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            populateSampleData()
            onComplete()
        }
    }

    // Populate default database entries (Strictly marked as demo, never uploaded to cloud)
    private suspend fun populateSampleData() {
        val demoIds = (sharedPrefs.getStringSet("demo_entity_ids", emptySet()) ?: emptySet()).toMutableSet()

        // Add Bank Accounts
        val hdfcId = repository.insertBankAccount(
            BankAccount(
                bankName = "HDFC Bank",
                accountNickname = "Salary Account (Demo)",
                accountNumberLast4 = "4589",
                accountType = "SALARY",
                initialBalance = 45000.0,
                isSmsDetectionEnabled = true,
                isHiddenFromSummary = false
            )
        )
        demoIds.add("bank_$hdfcId")

        val sbiId = repository.insertBankAccount(
            BankAccount(
                bankName = "State Bank of India",
                accountNickname = "Savings Fund (Demo)",
                accountNumberLast4 = "1204",
                accountType = "SAVINGS",
                initialBalance = 120000.0,
                isSmsDetectionEnabled = true,
                isHiddenFromSummary = false
            )
        )
        demoIds.add("bank_$sbiId")

        // Add Scooter Asset
        val scooterId = repository.insertAsset(
            Asset(
                name = "Ola S1 Pro Scooter",
                type = "VEHICLE",
                purchaseDate = Calendar.getInstance().apply { set(2025, Calendar.JANUARY, 15) }.timeInMillis,
                purchasePrice = 145000.0,
                insuranceDetails = "HDFC Ergo, Policy #OLA9921, Renews Jan 2027",
                notes = "Personal commuter vehicle [Demo Data]"
            )
        )
        demoIds.add("asset_$scooterId")

        // Scooter Timeline Expenses
        val tx1Id = repository.insertTransaction(
            Transaction(
                amount = 460.0,
                category = "Vehicle",
                subcategory = "Repair",
                paymentMethod = "Cash",
                merchant = "Local Garage",
                notes = "Brake pad replacement & alignment [Demo Data]",
                date = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 10) }.timeInMillis,
                type = "EXPENSE",
                assetId = scooterId,
                tagsString = "Demo"
            )
        )
        demoIds.add("tx_$tx1Id")

        val tx2Id = repository.insertTransaction(
            Transaction(
                amount = 350.0,
                category = "Vehicle",
                subcategory = "Fuel", // electricity charging
                paymentMethod = "UPI",
                merchant = "BESCOM Charging",
                notes = "Monthly fast charging [Demo Data]",
                date = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 22) }.timeInMillis,
                type = "EXPENSE",
                assetId = scooterId,
                tagsString = "Demo"
            )
        )
        demoIds.add("tx_$tx2Id")

        val tx3Id = repository.insertTransaction(
            Transaction(
                amount = 2000.0,
                category = "Vehicle",
                subcategory = "Service",
                paymentMethod = "Credit Card",
                merchant = "Ola Experience Centre",
                notes = "Annual full service & battery health diagnostic [Demo Data]",
                date = Calendar.getInstance().apply { set(2026, Calendar.JULY, 1) }.timeInMillis,
                type = "EXPENSE",
                assetId = scooterId,
                tagsString = "Demo"
            )
        )
        demoIds.add("tx_$tx3Id")

        // General Expenses
        val tx4Id = repository.insertTransaction(
            Transaction(
                amount = 220.0,
                category = "Food",
                subcategory = "Lunch",
                paymentMethod = "UPI",
                merchant = "Domino's",
                notes = "Team lunch [Demo Data]",
                date = System.currentTimeMillis() - 3 * 3600 * 1000, // 3 hrs ago
                type = "EXPENSE",
                tagsString = "Office,Demo"
            )
        )
        demoIds.add("tx_$tx4Id")

        val tx5Id = repository.insertTransaction(
            Transaction(
                amount = 85.0,
                category = "Food",
                subcategory = "Tea/Coffee",
                paymentMethod = "UPI",
                merchant = "Third Wave Coffee",
                notes = "Morning cappuccino [Demo Data]",
                date = System.currentTimeMillis() - 6 * 3600 * 1000, // 6 hrs ago
                type = "EXPENSE",
                tagsString = "Office,Demo"
            )
        )
        demoIds.add("tx_$tx5Id")

        val tx6Id = repository.insertTransaction(
            Transaction(
                amount = 15000.0,
                category = "Home",
                subcategory = "Rent",
                paymentMethod = "Bank",
                merchant = "Owner",
                notes = "July House Rent [Demo Data]",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
                type = "EXPENSE",
                tagsString = "Family,Demo"
            )
        )
        demoIds.add("tx_$tx6Id")

        val tx7Id = repository.insertTransaction(
            Transaction(
                amount = 1800.0,
                category = "Home",
                subcategory = "Internet",
                paymentMethod = "Credit Card",
                merchant = "ACT Fibernet",
                notes = "Broadband high-speed internet renewal [Demo Data]",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 2) }.timeInMillis,
                type = "EXPENSE",
                tagsString = "Demo"
            )
        )
        demoIds.add("tx_$tx7Id")

        // Incomes
        val tx8Id = repository.insertTransaction(
            Transaction(
                amount = 95000.0,
                category = "Salary",
                subcategory = "Primary Job",
                paymentMethod = "Bank",
                merchant = "TechCorp Inc.",
                notes = "Monthly Salary Deposit [Demo Data]",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
                type = "INCOME",
                tagsString = "Demo"
            )
        )
        demoIds.add("tx_$tx8Id")

        val tx9Id = repository.insertTransaction(
            Transaction(
                amount = 12500.0,
                category = "Freelancing",
                subcategory = "Web Dev",
                paymentMethod = "UPI",
                merchant = "Upwork Project",
                notes = "API integration payout [Demo Data]",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 4) }.timeInMillis,
                type = "INCOME",
                tagsString = "Demo"
            )
        )
        demoIds.add("tx_$tx9Id")

        // Budgets
        val b1 = repository.insertBudget(Budget(category = "Food", limitAmount = 8000.0, monthYear = "2026-07"))
        val b2 = repository.insertBudget(Budget(category = "Vehicle", limitAmount = 5000.0, monthYear = "2026-07"))
        val b3 = repository.insertBudget(Budget(category = "Home", limitAmount = 20000.0, monthYear = "2026-07"))
        val b4 = repository.insertBudget(Budget(category = "Shopping", limitAmount = 10000.0, monthYear = "2026-07"))
        demoIds.add("budget_$b1")
        demoIds.add("budget_$b2")
        demoIds.add("budget_$b3")
        demoIds.add("budget_$b4")

        // Subscriptions
        val s1 = repository.insertSubscription(
            Subscription(
                name = "Netflix Premium",
                cost = 649.0,
                renewalDate = System.currentTimeMillis() + 2 * 24 * 3600 * 1000L, // 2 days later (upcoming alert)
                billingCycle = "MONTHLY",
                category = "Entertainment",
                isAutoRenew = true,
                reminderDaysInAdvance = 2,
                paymentMethodName = "HDFC Regalia Card",
                notes = "[Demo Data]"
            )
        )
        val s2 = repository.insertSubscription(
            Subscription(
                name = "Spotify Duo",
                cost = 149.0,
                renewalDate = System.currentTimeMillis() + 5 * 24 * 3600 * 1000L,
                billingCycle = "MONTHLY",
                category = "Entertainment",
                isAutoRenew = true,
                reminderDaysInAdvance = 3,
                paymentMethodName = "SBI Savings Account",
                notes = "[Demo Data]"
            )
        )
        val s3 = repository.insertSubscription(
            Subscription(
                name = "ChatGPT Plus",
                cost = 1999.0,
                renewalDate = System.currentTimeMillis() + 1 * 24 * 3600 * 1000L, // 1 day later (Tomorrow!)
                billingCycle = "MONTHLY",
                category = "Work & Software",
                isAutoRenew = true,
                reminderDaysInAdvance = 3,
                paymentMethodName = "ICICI Amazon Pay Card",
                notes = "[Demo Data]"
            )
        )
        val s4 = repository.insertSubscription(
            Subscription(
                name = "Google One 2TB",
                cost = 650.0,
                renewalDate = System.currentTimeMillis() + 18 * 24 * 3600 * 1000L,
                billingCycle = "MONTHLY",
                category = "Cloud Storage",
                isAutoRenew = true,
                reminderDaysInAdvance = 5,
                paymentMethodName = "UPI AutoPay",
                notes = "[Demo Data]"
            )
        )
        demoIds.add("sub_$s1")
        demoIds.add("sub_$s2")
        demoIds.add("sub_$s3")
        demoIds.add("sub_$s4")

        // Savings Goals
        val g1 = repository.insertSavingsGoal(
            SavingsGoal(
                name = "M3 MacBook Pro",
                targetAmount = 180000.0,
                currentAmount = 120000.0,
                targetDate = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.timeInMillis,
                notes = "Development laptop upgrade [Demo Data]"
            )
        )
        val g2 = repository.insertSavingsGoal(
            SavingsGoal(
                name = "Emergency Fund",
                targetAmount = 200000.0,
                currentAmount = 85000.0,
                targetDate = Calendar.getInstance().apply { add(Calendar.MONTH, 12) }.timeInMillis,
                notes = "6 months of essential living expenses [Demo Data]"
            )
        )
        demoIds.add("goal_$g1")
        demoIds.add("goal_$g2")

        // Borrow & Lend
        val bl1 = repository.insertBorrowLend(
            BorrowLend(
                contactName = "Rohan Sharma",
                amount = 2500.0,
                type = "LENT",
                dueDate = System.currentTimeMillis() + 5 * 24 * 3600 * 1000L, // 5 days later
                isPaid = false,
                notes = "Weekend trip expense share [Demo Data]"
            )
        )
        val bl2 = repository.insertBorrowLend(
            BorrowLend(
                contactName = "Shikha Verma",
                amount = 1200.0,
                type = "BORROWED",
                dueDate = System.currentTimeMillis() + 8 * 24 * 3600 * 1000L,
                isPaid = false,
                notes = "Bought concert ticket [Demo Data]"
            )
        )
        demoIds.add("bl_$bl1")
        demoIds.add("bl_$bl2")

        // Wishlist
        val w1 = repository.insertWishlistItem(
            Wishlist(
                name = "Bose QuietComfort Headphones",
                price = 28000.0,
                priority = "HIGH",
                targetDate = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
                notes = "Noise cancellation for deep work [Demo Data]"
            )
        )
        val w2 = repository.insertWishlistItem(
            Wishlist(
                name = "Steelcase Gesture Chair",
                price = 85000.0,
                priority = "MEDIUM",
                targetDate = System.currentTimeMillis() + 90L * 24 * 3600 * 1000,
                notes = "Ergonomic workspace seating [Demo Data]"
            )
        )
        demoIds.add("wish_$w1")
        demoIds.add("wish_$w2")

        sharedPrefs.edit().putStringSet("demo_entity_ids", demoIds).apply()
    }

    // ==========================================
    // BACKUP, RESTORE & AUDIT EXPORT METHODS
    // ==========================================

    fun exportFullBackup(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val isDark = if (sharedPrefs.contains("is_dark_theme")) sharedPrefs.getBoolean("is_dark_theme", true) else null
                val userPrefs = UserPreferencesBackup(
                    cashOnlyMode = _excludeCreditCards.value,
                    numberFormatPreference = _numberFormatPreference.value,
                    smsDetectionEnabled = _isSmsDetectionEnabled.value,
                    currencySymbol = _currencySymbol.value,
                    currencyCode = _currencyCode.value,
                    isDarkTheme = isDark
                )

                val data = FullBackupData(
                    transactions = repository.allTransactions.first(),
                    bankAccounts = repository.allBankAccounts.first(),
                    creditCards = repository.allCreditCards.first(),
                    customCategories = repository.allCustomCategories.first(),
                    budgets = repository.allBudgets.first(),
                    assets = repository.allAssets.first(),
                    subscriptions = repository.allSubscriptions.first(),
                    savingsGoals = repository.allSavingsGoals.first(),
                    borrowLends = repository.allBorrowLends.first(),
                    wishlists = repository.allWishlistItems.first(),
                    userPreferences = userPrefs
                )
                val success = BackupEngine.exportAndShareBackup(context, data)
                onComplete?.invoke(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }
    }

    fun restoreFullBackup(
        jsonString: String,
        isMergeMode: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val parsed = BackupEngine.parseBackupJson(jsonString)
                if (parsed == null) {
                    onResult(false, "Invalid or corrupted backup JSON file format.")
                    return@launch
                }
                val (data, stats) = parsed

                if (!isMergeMode) {
                    // Clean wipe
                    repository.fullFactoryReset()
                }

                // Restore entities
                data.customCategories.forEach { repository.insertCustomCategory(it) }
                data.bankAccounts.forEach { repository.insertBankAccount(it) }
                data.creditCards.forEach { repository.insertCreditCard(it) }
                data.budgets.forEach { repository.insertBudget(it) }
                data.assets.forEach { repository.insertAsset(it) }
                data.subscriptions.forEach { repository.insertSubscription(it) }
                data.savingsGoals.forEach { repository.insertSavingsGoal(it) }
                data.borrowLends.forEach { repository.insertBorrowLend(it) }
                data.wishlists.forEach { repository.insertWishlistItem(it) }
                data.transactions.forEach { repository.insertTransaction(it) }

                // Restore user preferences if present
                data.userPreferences?.let { prefs ->
                    setExcludeCreditCards(prefs.cashOnlyMode)
                    setNumberFormatPreference(prefs.numberFormatPreference)
                    setSmsDetectionEnabled(prefs.smsDetectionEnabled)
                    setCurrency(prefs.currencySymbol, prefs.currencyCode)
                    if (prefs.isDarkTheme != null) {
                        sharedPrefs.edit().putBoolean("is_dark_theme", prefs.isDarkTheme).apply()
                    }
                }

                if (currentUser.value != null) {
                    syncDataWithCloud()
                }

                val modeText = if (isMergeMode) "Merged" else "Clean Restored"
                val prefsNote = if (data.userPreferences != null) " with custom preferences restored." else "."
                val summary = "$modeText ${stats.totalTransactions} transactions, ${stats.totalBankAccounts} banks, ${stats.totalCreditCards} cards, ${stats.totalCategories} categories$prefsNote"
                onResult(true, summary)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Restore failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportPdfStatement(
        context: Context,
        filter: StatementFilter,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val allTx = repository.allTransactions.first()
                val banks = repository.allBankAccounts.first()
                val cards = repository.allCreditCards.first()

                val success = PdfStatementGenerator.exportAndSharePdfStatement(
                    context = context,
                    allTransactions = allTx,
                    bankAccounts = banks,
                    creditCards = cards,
                    filter = filter
                )
                onComplete?.invoke(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }
    }

    fun exportCsvReport(
        context: Context,
        filterDescription: String = "All Transactions",
        filter: StatementFilter? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val allTx = repository.allTransactions.first()
                val banks = repository.allBankAccounts.first()
                val cards = repository.allCreditCards.first()

                val filteredTx = if (filter != null) {
                    allTx.filter { tx ->
                        val matchStart = filter.startDate == null || tx.date >= filter.startDate
                        val matchEnd = filter.endDate == null || tx.date <= filter.endDate
                        val matchBank = filter.bankAccountId == null || tx.bankAccountId == filter.bankAccountId || tx.toBankAccountId == filter.bankAccountId
                        val matchCard = filter.creditCardId == null || tx.creditCardId == filter.creditCardId || tx.toCreditCardId == filter.creditCardId
                        val matchTag = filter.tagFilter.isNullOrBlank() || tx.tags.any { it.equals(filter.tagFilter, ignoreCase = true) }
                        val matchCat = filter.categoryFilter.isNullOrBlank() || tx.category.equals(filter.categoryFilter, ignoreCase = true)
                        matchStart && matchEnd && matchBank && matchCard && matchTag && matchCat
                    }
                } else {
                    allTx
                }

                val success = CsvExportEngine.exportAndShareCsv(
                    context = context,
                    transactions = filteredTx,
                    bankAccounts = banks,
                    creditCards = cards,
                    filterDescription = filterDescription
                )
                onComplete?.invoke(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }
    }
}

// Visual layout helper for Reminders list
data class ReminderItem(
    val id: String,
    val title: String,
    val dueDate: Long,
    val amount: Double,
    val type: String, // "SUBSCRIPTION", "LENT", "BORROWED"
    val notes: String
)
