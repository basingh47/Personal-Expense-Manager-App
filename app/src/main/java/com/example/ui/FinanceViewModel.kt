package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
    
    val firebaseAuth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
        
        firebaseAuth.addAuthStateListener { auth ->
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

            // Populate sample data if DB is completely empty
            repository.allTransactions.first().let { list ->
                if (list.isEmpty()) {
                    populateSampleData()
                }
            }
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

    // Search & Filter state
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _selectedFilterCategory = MutableStateFlow<String?>(null)
    val selectedFilterCategory = _selectedFilterCategory.asStateFlow()

    private val _selectedFilterMethod = MutableStateFlow<String?>(null)
    val selectedFilterMethod = _selectedFilterMethod.asStateFlow()

    private val _selectedFilterMonth = MutableStateFlow<String?>(null) // Format: "yyyy-MM"
    val selectedFilterMonth = _selectedFilterMonth.asStateFlow()

    private val _selectedFilterDate = MutableStateFlow<String?>(null) // Format: "yyyy-MM-dd"
    val selectedFilterDate = _selectedFilterDate.asStateFlow()

    private val _deleteConfirmation = MutableStateFlow<DeleteConfirmationRequest?>(null)
    val deleteConfirmation = _deleteConfirmation.asStateFlow()

    private val _excludeCreditCards = MutableStateFlow(false)
    val excludeCreditCards = _excludeCreditCards.asStateFlow()

    fun setExcludeCreditCards(exclude: Boolean) {
        _excludeCreditCards.value = exclude
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

    fun setFilterCategory(cat: String?) {
        _selectedFilterCategory.value = cat
    }

    fun setFilterMethod(method: String?) {
        _selectedFilterMethod.value = method
    }

    fun setFilterMonth(month: String?) {
        _selectedFilterMonth.value = month
    }

    fun setFilterDate(dateStr: String?) {
        _selectedFilterDate.value = dateStr
    }

    fun clearFilters() {
        _searchText.value = ""
        _selectedFilterCategory.value = null
        _selectedFilterMethod.value = null
        _selectedFilterMonth.value = null
        _selectedFilterDate.value = null
    }

    // Filtered Transactions
    val filteredTransactions = combine(
        transactions,
        searchText,
        selectedFilterCategory,
        selectedFilterMethod,
        combine(selectedFilterMonth, selectedFilterDate) { m, d -> Pair(m, d) }
    ) { list, search, cat, method, monthDate ->
        val (month, dateStr) = monthDate
        list.filter { tx ->
            val matchSearch = search.isBlank() || 
                tx.merchant.contains(search, ignoreCase = true) ||
                tx.category.contains(search, ignoreCase = true) ||
                tx.subcategory.contains(search, ignoreCase = true) ||
                tx.notes.contains(search, ignoreCase = true) ||
                tx.tagsString.contains(search, ignoreCase = true) ||
                tx.amount.toString().contains(search)

            val matchCategory = cat == null || tx.category == cat
            val matchMethod = method == null || tx.paymentMethod == method
            val matchMonth = month == null || SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(tx.date)) == month
            val matchDate = dateStr == null || SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.date)) == dateStr

            matchSearch && matchCategory && matchMethod && matchMonth && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            val id = repository.insertTransaction(tx)
            val user = firebaseAuth.currentUser
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
                    "creditCardId" to tx.creditCardId
                )
                firestore.collection("users").document(user.uid).collection("transactions")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("transactions")
                    .document(tx.id.toString()).delete()
            }
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
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to name,
                    "icon" to icon,
                    "type" to type,
                    "parentCategory" to parentCategory
                )
                firestore.collection("users").document(user.uid).collection("custom_categories")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun updateCustomCategory(oldCategory: CustomCategory, newCategory: CustomCategory) {
        viewModelScope.launch {
            val id = repository.insertCustomCategory(newCategory)
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to newCategory.name,
                    "icon" to newCategory.icon,
                    "type" to newCategory.type,
                    "parentCategory" to newCategory.parentCategory
                )
                firestore.collection("users").document(user.uid).collection("custom_categories")
                    .document(id.toString()).set(data)
            }
            
            // If main category name changed, update child subcategories, transactions, and budgets
            if (oldCategory.parentCategory == null && oldCategory.name != newCategory.name) {
                val childSubs = customCategories.value.filter { it.parentCategory == oldCategory.name }
                childSubs.forEach { child ->
                    repository.insertCustomCategory(child.copy(parentCategory = newCategory.name))
                    if (user != null) {
                        firestore.collection("users").document(user.uid).collection("custom_categories")
                            .document(child.id.toString()).update("parentCategory", newCategory.name)
                    }
                }
                
                // Update transactions
                val txsToUpdate = transactions.value.filter { it.category == oldCategory.name }
                txsToUpdate.forEach { tx ->
                    repository.insertTransaction(tx.copy(category = newCategory.name))
                    if (user != null) {
                        firestore.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("category", newCategory.name)
                    }
                }
                
                // Update budgets
                val budgetsToUpdate = budgets.value.filter { it.category == oldCategory.name }
                budgetsToUpdate.forEach { b ->
                    repository.insertBudget(b.copy(category = newCategory.name))
                    if (user != null) {
                        firestore.collection("users").document(user.uid).collection("budgets")
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
                        firestore.collection("users").document(user.uid).collection("transactions")
                            .document(tx.id.toString()).update("subcategory", newCategory.name)
                    }
                }
            }
        }
    }

    fun deleteCustomCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.deleteCustomCategory(category)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("custom_categories")
                    .document(category.id.toString()).delete()
            }
            if (category.parentCategory == null) {
                // If it's a parent category, delete all custom subcategories that have this as parent
                val childSubs = customCategories.value.filter { it.parentCategory == category.name }
                childSubs.forEach { child ->
                    repository.deleteCustomCategory(child)
                    if (user != null) {
                        firestore.collection("users").document(user.uid).collection("custom_categories")
                            .document(child.id.toString()).delete()
                    }
                }
                
                // Delete budgets for this category
                val budgetsToDelete = budgets.value.filter { it.category == category.name }
                budgetsToDelete.forEach { b ->
                    repository.deleteBudget(b)
                    if (user != null) {
                        firestore.collection("users").document(user.uid).collection("budgets")
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
            val user = firebaseAuth.currentUser
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
                firestore.collection("users").document(user.uid).collection("assets")
                    .document(id.toString()).set(data)
            }
            onComplete?.invoke(id)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("assets")
                    .document(asset.id.toString()).delete()
            }
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            val id = repository.insertBudget(budget)
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "category" to budget.category,
                    "limitAmount" to budget.limitAmount,
                    "monthYear" to budget.monthYear
                )
                firestore.collection("users").document(user.uid).collection("budgets")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("budgets")
                    .document(budget.id.toString()).delete()
            }
        }
    }

    fun addSubscription(sub: Subscription) {
        viewModelScope.launch {
            val id = repository.insertSubscription(sub)
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to sub.name,
                    "cost" to sub.cost,
                    "renewalDate" to sub.renewalDate,
                    "billingCycle" to sub.billingCycle,
                    "isActive" to sub.isActive,
                    "category" to sub.category,
                    "notes" to sub.notes
                )
                firestore.collection("users").document(user.uid).collection("subscriptions")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun toggleSubscriptionActive(sub: Subscription) {
        viewModelScope.launch {
            val updated = sub.copy(isActive = !sub.isActive)
            repository.insertSubscription(updated)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).update("isActive", updated.isActive)
            }
        }
    }

    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(sub)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("subscriptions")
                    .document(sub.id.toString()).delete()
            }
        }
    }

    fun addSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val id = repository.insertSavingsGoal(goal)
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to goal.name,
                    "targetAmount" to goal.targetAmount,
                    "currentAmount" to goal.currentAmount,
                    "targetDate" to goal.targetDate,
                    "notes" to goal.notes
                )
                firestore.collection("users").document(user.uid).collection("savings_goals")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun updateSavingsGoalProgress(goal: SavingsGoal, addAmount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + addAmount)
            repository.insertSavingsGoal(updated)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("savings_goals")
                    .document(goal.id.toString()).update("currentAmount", updated.currentAmount)
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("savings_goals")
                    .document(goal.id.toString()).delete()
            }
        }
    }

    fun addBorrowLend(item: BorrowLend) {
        viewModelScope.launch {
            val id = repository.insertBorrowLend(item)
            val user = firebaseAuth.currentUser
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
                firestore.collection("users").document(user.uid).collection("borrow_lend")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun toggleBorrowLendPaid(item: BorrowLend) {
        viewModelScope.launch {
            val updated = item.copy(isPaid = !item.isPaid)
            repository.insertBorrowLend(updated)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("borrow_lend")
                    .document(item.id.toString()).update("isPaid", updated.isPaid)
            }
        }
    }

    fun deleteBorrowLend(item: BorrowLend) {
        viewModelScope.launch {
            repository.deleteBorrowLend(item)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("borrow_lend")
                    .document(item.id.toString()).delete()
            }
        }
    }

    fun addWishlistItem(item: Wishlist) {
        viewModelScope.launch {
            val id = repository.insertWishlistItem(item)
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "name" to item.name,
                    "price" to item.price,
                    "priority" to item.priority,
                    "targetDate" to item.targetDate,
                    "notes" to item.notes,
                    "isPurchased" to item.isPurchased
                )
                firestore.collection("users").document(user.uid).collection("wishlist")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun toggleWishlistItemPurchased(item: Wishlist) {
        viewModelScope.launch {
            val updated = item.copy(isPurchased = !item.isPurchased)
            val id = repository.insertWishlistItem(updated)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("wishlist")
                    .document(item.id.toString()).update("isPurchased", updated.isPurchased)
            }
            
            // Proactively offer to record as an expense if purchased!
            if (updated.isPurchased) {
                addTransaction(
                    Transaction(
                        amount = updated.price,
                        category = "Shopping",
                        subcategory = "Electronics",
                        paymentMethod = "UPI",
                        merchant = updated.name,
                        notes = "Purchased from Wishlist: ${updated.notes}",
                        type = "EXPENSE"
                    )
                )
            }
        }
    }

    fun deleteWishlistItem(item: Wishlist) {
        viewModelScope.launch {
            repository.deleteWishlistItem(item)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("wishlist")
                    .document(item.id.toString()).delete()
            }
        }
    }

    fun addCreditCard(card: CreditCard) {
        viewModelScope.launch {
            val id = repository.insertCreditCard(card)
            val user = firebaseAuth.currentUser
            if (user != null) {
                val data = mapOf(
                    "id" to id,
                    "cardName" to card.cardName,
                    "billingDay" to card.billingDay,
                    "dueDay" to card.dueDay,
                    "cardLimit" to card.cardLimit,
                    "lastFourDigits" to card.lastFourDigits
                )
                firestore.collection("users").document(user.uid).collection("credit_cards")
                    .document(id.toString()).set(data)
            }
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
            val user = firebaseAuth.currentUser
            if (user != null) {
                firestore.collection("users").document(user.uid).collection("credit_cards")
                    .document(card.id.toString()).delete()
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
                syncDataWithCloud()
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Sign in failed")
            }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
                syncDataWithCloud()
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Registration failed")
            }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun syncDataWithCloud() {
        val user = firebaseAuth.currentUser ?: return
        val userId = user.uid
        viewModelScope.launch {
            try {
                // 1. Transactions
                val localTransactions = repository.allTransactions.first()
                val transactionsRef = firestore.collection("users").document(userId).collection("transactions")
                transactionsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudTransactionsMap = snapshot.documents.associateBy { it.id }
                        localTransactions.forEach { local ->
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
                                    "creditCardId" to local.creditCardId
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
                                    creditCardId = doc.getLong("creditCardId")
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
                val budgetsRef = firestore.collection("users").document(userId).collection("budgets")
                budgetsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localBudgets.forEach { local ->
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
                val assetsRef = firestore.collection("users").document(userId).collection("assets")
                assetsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localAssets.forEach { local ->
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
                val subsRef = firestore.collection("users").document(userId).collection("subscriptions")
                subsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localSubs.forEach { local ->
                            if (!cloudMap.containsKey(local.id.toString())) {
                                val data = mapOf(
                                    "id" to local.id,
                                    "name" to local.name,
                                    "cost" to local.cost,
                                    "renewalDate" to local.renewalDate,
                                    "billingCycle" to local.billingCycle,
                                    "isActive" to local.isActive,
                                    "category" to local.category,
                                    "notes" to local.notes
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
                                    notes = doc.getString("notes") ?: ""
                                )
                                viewModelScope.launch { repository.insertSubscription(sub) }
                            }
                        }
                    }
                }

                // 5. Savings Goals
                val localGoals = repository.allSavingsGoals.first()
                val goalsRef = firestore.collection("users").document(userId).collection("savings_goals")
                goalsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localGoals.forEach { local ->
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
                val blRef = firestore.collection("users").document(userId).collection("borrow_lend")
                blRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localBL.forEach { local ->
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
                val wishlistRef = firestore.collection("users").document(userId).collection("wishlist")
                wishlistRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localWishlist.forEach { local ->
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
                                    isPurchased = doc.getBoolean("isPurchased") ?: false
                                )
                                viewModelScope.launch { repository.insertWishlistItem(wish) }
                            }
                        }
                    }
                }

                // 8. Custom Categories
                val localCategories = repository.allCustomCategories.first()
                val categoriesRef = firestore.collection("users").document(userId).collection("custom_categories")
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
                val cardsRef = firestore.collection("users").document(userId).collection("credit_cards")
                cardsRef.get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        val cloudMap = snapshot.documents.associateBy { it.id }
                        localCards.forEach { local ->
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

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // METRICS & COMPUTED PROPERTIES

    // Today's spending
    val todaySpending = combine(transactions, excludeCreditCards) { list, exclude ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= todayStart }.sumOf { it.amount }
            val cardPayments = list.filter { it.category == "Credit Card Payment" && it.date >= todayStart }.sumOf { it.amount }
            normalExpenses + cardPayments
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= todayStart }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // This month's spending
    val thisMonthSpending = combine(transactions, excludeCreditCards) { list, exclude ->
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }.sumOf { it.amount }
            val cardPayments = list.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.sumOf { it.amount }
            normalExpenses + cardPayments
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= monthStart }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // This month's income
    val thisMonthIncome = combine(transactions, excludeCreditCards) { list, exclude ->
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // We ALWAYS exclude Credit Card Payment from This Month Income because it's not a real income (even if type == "INCOME" in database to reduce balance).
        list.filter { it.type == "INCOME" && it.category != "Credit Card Payment" && it.date >= monthStart }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Average daily spending for the current month
    val averageDailySpending = combine(transactions, excludeCreditCards) { list, exclude ->
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val monthStart = cal.timeInMillis
        
        val totalSpentThisMonth = if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }.sumOf { it.amount }
            val cardPayments = list.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.sumOf { it.amount }
            normalExpenses + cardPayments
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= monthStart }.sumOf { it.amount }
        }
        if (currentDay > 0) totalSpentThisMonth / currentDay else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Compare this month vs last month spending
    val percentSpendingChange = combine(transactions, excludeCreditCards) { list, exclude ->
        val cal = Calendar.getInstance()
        
        // This month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val thisMonthStart = cal.timeInMillis
        val thisMonthSum = if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= thisMonthStart }.sumOf { it.amount }
            val cardPayments = list.filter { it.category == "Credit Card Payment" && it.date >= thisMonthStart }.sumOf { it.amount }
            normalExpenses + cardPayments
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= thisMonthStart }.sumOf { it.amount }
        }
        
        // Last month
        cal.add(Calendar.MONTH, -1)
        val lastMonthStart = cal.timeInMillis
        val lastMonthSum = if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            val cardPayments = list.filter { it.category == "Credit Card Payment" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
            normalExpenses + cardPayments
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.amount }
        }
        
        if (lastMonthSum == 0.0) {
            0.0
        } else {
            ((thisMonthSum - lastMonthSum) / lastMonthSum) * 100.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category spending chart data
    val categorySpendingData = combine(transactions, excludeCreditCards) { list, exclude ->
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        
        val filteredList = if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }
            val cardPaymentsMapped = list.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.map {
                it.copy(type = "EXPENSE")
            }
            normalExpenses + cardPaymentsMapped
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= monthStart }
        }
        
        filteredList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Highest expense of the month
    val highestExpenseOfMonth = combine(transactions, excludeCreditCards) { list, exclude ->
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        
        val filteredList = if (exclude) {
            val normalExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "Credit Card" && it.date >= monthStart }
            val cardPaymentsMapped = list.filter { it.category == "Credit Card Payment" && it.date >= monthStart }.map {
                it.copy(type = "EXPENSE")
            }
            normalExpenses + cardPaymentsMapped
        } else {
            list.filter { it.type == "EXPENSE" && it.date >= monthStart }
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
                        notes = "Renewal cost: ₹${sub.cost}"
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

    // Populate default database entries
    private suspend fun populateSampleData() {
        // Add Scooter Asset
        val scooterId = repository.insertAsset(
            Asset(
                name = "Ola S1 Pro Scooter",
                type = "VEHICLE",
                purchaseDate = Calendar.getInstance().apply { set(2025, Calendar.JANUARY, 15) }.timeInMillis,
                purchasePrice = 145000.0,
                insuranceDetails = "HDFC Ergo, Policy #OLA9921, Renews Jan 2027",
                notes = "Personal commuter vehicle"
            )
        )

        // Scooter Timeline Expenses
        repository.insertTransaction(
            Transaction(
                amount = 460.0,
                category = "Vehicle",
                subcategory = "Repair",
                paymentMethod = "Cash",
                merchant = "Local Garage",
                notes = "Brake pad replacement & alignment",
                date = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 10) }.timeInMillis,
                type = "EXPENSE",
                assetId = scooterId
            )
        )

        repository.insertTransaction(
            Transaction(
                amount = 350.0,
                category = "Vehicle",
                subcategory = "Fuel", // electricity charging
                paymentMethod = "UPI",
                merchant = "BESCOM Charging",
                notes = "Monthly fast charging",
                date = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 22) }.timeInMillis,
                type = "EXPENSE",
                assetId = scooterId
            )
        )

        repository.insertTransaction(
            Transaction(
                amount = 2000.0,
                category = "Vehicle",
                subcategory = "Service",
                paymentMethod = "Credit Card",
                merchant = "Ola Experience Centre",
                notes = "Annual full service & battery health diagnostic",
                date = Calendar.getInstance().apply { set(2026, Calendar.JULY, 1) }.timeInMillis,
                type = "EXPENSE",
                assetId = scooterId
            )
        )

        // General Expenses
        repository.insertTransaction(
            Transaction(
                amount = 220.0,
                category = "Food",
                subcategory = "Lunch",
                paymentMethod = "UPI",
                merchant = "Domino's",
                notes = "Team lunch",
                date = System.currentTimeMillis() - 3 * 3600 * 1000, // 3 hrs ago
                type = "EXPENSE",
                tagsString = "Office"
            )
        )

        repository.insertTransaction(
            Transaction(
                amount = 85.0,
                category = "Food",
                subcategory = "Tea/Coffee",
                paymentMethod = "UPI",
                merchant = "Third Wave Coffee",
                notes = "Morning cappuccino",
                date = System.currentTimeMillis() - 6 * 3600 * 1000, // 6 hrs ago
                type = "EXPENSE",
                tagsString = "Office"
            )
        )

        repository.insertTransaction(
            Transaction(
                amount = 15000.0,
                category = "Home",
                subcategory = "Rent",
                paymentMethod = "Bank",
                merchant = "Owner",
                notes = "July House Rent",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
                type = "EXPENSE",
                tagsString = "Family"
            )
        )

        repository.insertTransaction(
            Transaction(
                amount = 1800.0,
                category = "Home",
                subcategory = "Internet",
                paymentMethod = "Credit Card",
                merchant = "ACT Fibernet",
                notes = "Broadband high-speed internet renewal",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 2) }.timeInMillis,
                type = "EXPENSE"
            )
        )

        // Incomes
        repository.insertTransaction(
            Transaction(
                amount = 95000.0,
                category = "Salary",
                subcategory = "Primary Job",
                paymentMethod = "Bank",
                merchant = "TechCorp Inc.",
                notes = "Monthly Salary Deposit",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
                type = "INCOME"
            )
        )

        repository.insertTransaction(
            Transaction(
                amount = 12500.0,
                category = "Freelancing",
                subcategory = "Web Dev",
                paymentMethod = "UPI",
                merchant = "Upwork Project",
                notes = "API integration payout",
                date = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 4) }.timeInMillis,
                type = "INCOME"
            )
        )

        // Budgets
        repository.insertBudget(Budget(category = "Food", limitAmount = 8000.0, monthYear = "2026-07"))
        repository.insertBudget(Budget(category = "Vehicle", limitAmount = 5000.0, monthYear = "2026-07"))
        repository.insertBudget(Budget(category = "Home", limitAmount = 20000.0, monthYear = "2026-07"))
        repository.insertBudget(Budget(category = "Shopping", limitAmount = 10000.0, monthYear = "2026-07"))

        // Subscriptions
        repository.insertSubscription(
            Subscription(
                name = "Netflix Premium",
                cost = 649.0,
                renewalDate = System.currentTimeMillis() + 4 * 24 * 3600 * 1000L, // 4 days later
                billingCycle = "MONTHLY",
                category = "Entertainment"
            )
        )
        repository.insertSubscription(
            Subscription(
                name = "Spotify Duo",
                cost = 149.0,
                renewalDate = System.currentTimeMillis() + 10 * 24 * 3600 * 1000L,
                billingCycle = "MONTHLY",
                category = "Entertainment"
            )
        )
        repository.insertSubscription(
            Subscription(
                name = "ChatGPT Plus",
                cost = 1999.0,
                renewalDate = System.currentTimeMillis() + 2 * 24 * 3600 * 1000L, // 2 days later
                billingCycle = "MONTHLY",
                category = "Work"
            )
        )

        // Savings Goals
        repository.insertSavingsGoal(
            SavingsGoal(
                name = "M3 MacBook Pro",
                targetAmount = 180000.0,
                currentAmount = 120000.0,
                targetDate = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.timeInMillis,
                notes = "Development laptop upgrade"
            )
        )
        repository.insertSavingsGoal(
            SavingsGoal(
                name = "Emergency Fund",
                targetAmount = 200000.0,
                currentAmount = 85000.0,
                targetDate = Calendar.getInstance().apply { add(Calendar.MONTH, 12) }.timeInMillis,
                notes = "6 months of essential living expenses"
            )
        )

        // Borrow & Lend
        repository.insertBorrowLend(
            BorrowLend(
                contactName = "Rohan Sharma",
                amount = 2500.0,
                type = "LENT",
                dueDate = System.currentTimeMillis() + 5 * 24 * 3600 * 1000L, // 5 days later
                isPaid = false,
                notes = "Weekend trip expense share"
            )
        )
        repository.insertBorrowLend(
            BorrowLend(
                contactName = "Shikha Verma",
                amount = 1200.0,
                type = "BORROWED",
                dueDate = System.currentTimeMillis() + 8 * 24 * 3600 * 1000L,
                isPaid = false,
                notes = "Bought concert ticket"
            )
        )

        // Wishlist
        repository.insertWishlistItem(
            Wishlist(
                name = "Bose QuietComfort Headphones",
                price = 28000.0,
                priority = "HIGH",
                targetDate = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
                notes = "Noise cancellation for deep work"
            )
        )
        repository.insertWishlistItem(
            Wishlist(
                name = "Steelcase Gesture Chair",
                price = 85000.0,
                priority = "MEDIUM",
                targetDate = System.currentTimeMillis() + 90L * 24 * 3600 * 1000,
                notes = "Ergonomic workspace seating"
            )
        )
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
