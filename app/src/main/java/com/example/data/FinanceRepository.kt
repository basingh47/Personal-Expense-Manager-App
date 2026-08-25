package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allAssets: Flow<List<Asset>> = financeDao.getAllAssets()
    val allBudgets: Flow<List<Budget>> = financeDao.getAllBudgets()
    val allSubscriptions: Flow<List<Subscription>> = financeDao.getAllSubscriptions()
    val allSavingsGoals: Flow<List<SavingsGoal>> = financeDao.getAllSavingsGoals()
    val allBorrowLends: Flow<List<BorrowLend>> = financeDao.getAllBorrowLends()
    val allWishlistItems: Flow<List<Wishlist>> = financeDao.getAllWishlistItems()
    val allCustomCategories: Flow<List<CustomCategory>> = financeDao.getAllCustomCategories()

    fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        financeDao.getTransactionsByType(type)

    fun getTransactionsByAsset(assetId: Long): Flow<List<Transaction>> =
        financeDao.getTransactionsByAsset(assetId)

    fun getBudgetsByMonth(monthYear: String): Flow<List<Budget>> =
        financeDao.getBudgetsByMonth(monthYear)

    suspend fun getAssetById(id: Long): Asset? = financeDao.getAssetById(id)

    // INSERT / UPDATE
    suspend fun insertTransaction(transaction: Transaction): Long = financeDao.insertTransaction(transaction)
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long> = financeDao.insertTransactions(transactions)
    suspend fun deleteTransaction(transaction: Transaction) = financeDao.deleteTransaction(transaction)
    suspend fun deleteTransactions(transactions: List<Transaction>) = financeDao.deleteTransactions(transactions)
    suspend fun deleteTransactionsByIds(ids: List<Long>) = financeDao.deleteTransactionsByIds(ids)
    suspend fun deleteTransactionById(id: Long) = financeDao.deleteTransactionById(id)

    suspend fun insertAsset(asset: Asset): Long = financeDao.insertAsset(asset)
    suspend fun deleteAsset(asset: Asset) = financeDao.deleteAsset(asset)
    suspend fun deleteAssetById(id: Long) = financeDao.deleteAssetById(id)

    suspend fun insertBudget(budget: Budget): Long = financeDao.insertBudget(budget)
    suspend fun deleteBudget(budget: Budget) = financeDao.deleteBudget(budget)
    suspend fun deleteBudgetById(id: Long) = financeDao.deleteBudgetById(id)

    suspend fun insertSubscription(sub: Subscription): Long = financeDao.insertSubscription(sub)
    suspend fun deleteSubscription(sub: Subscription) = financeDao.deleteSubscription(sub)
    suspend fun deleteSubscriptionById(id: Long) = financeDao.deleteSubscriptionById(id)

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long = financeDao.insertSavingsGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoal) = financeDao.deleteSavingsGoal(goal)
    suspend fun deleteSavingsGoalById(id: Long) = financeDao.deleteSavingsGoalById(id)

    suspend fun insertBorrowLend(item: BorrowLend): Long = financeDao.insertBorrowLend(item)
    suspend fun deleteBorrowLend(item: BorrowLend) = financeDao.deleteBorrowLend(item)
    suspend fun deleteBorrowLendById(id: Long) = financeDao.deleteBorrowLendById(id)

    suspend fun insertWishlistItem(item: Wishlist): Long = financeDao.insertWishlistItem(item)
    suspend fun deleteWishlistItem(item: Wishlist) = financeDao.deleteWishlistItem(item)
    suspend fun deleteWishlistItemById(id: Long) = financeDao.deleteWishlistItemById(id)

    suspend fun insertCustomCategory(category: CustomCategory): Long = financeDao.insertCustomCategory(category)
    suspend fun deleteCustomCategory(category: CustomCategory) = financeDao.deleteCustomCategory(category)

    // CREDIT CARDS
    val allCreditCards: Flow<List<CreditCard>> = financeDao.getAllCreditCards()
    fun getTransactionsByCreditCard(creditCardId: Long): Flow<List<Transaction>> = financeDao.getTransactionsByCreditCard(creditCardId)
    suspend fun insertCreditCard(card: CreditCard): Long = financeDao.insertCreditCard(card)
    suspend fun deleteCreditCard(card: CreditCard) = financeDao.deleteCreditCard(card)
    suspend fun deleteCreditCardById(id: Long) = financeDao.deleteCreditCardById(id)
    suspend fun getAllCreditCardsList(): List<CreditCard> = financeDao.getAllCreditCardsList()

    // BANK ACCOUNTS
    val allBankAccounts: Flow<List<BankAccount>> = financeDao.getAllBankAccounts()
    fun getTransactionsByBankAccount(bankAccountId: Long): Flow<List<Transaction>> = financeDao.getTransactionsByBankAccount(bankAccountId)
    suspend fun insertBankAccount(bankAccount: BankAccount): Long = financeDao.insertBankAccount(bankAccount)
    suspend fun deleteBankAccount(bankAccount: BankAccount) = financeDao.deleteBankAccount(bankAccount)
    suspend fun deleteBankAccountById(id: Long) = financeDao.deleteBankAccountById(id)
    suspend fun getBankAccountById(id: Long): BankAccount? = financeDao.getBankAccountById(id)
    suspend fun getAllBankAccountsList(): List<BankAccount> = financeDao.getAllBankAccountsList()

    // PENDING SMS TRANSACTIONS
    val allPendingSmsTransactions: Flow<List<PendingSmsTransaction>> = financeDao.getAllPendingSmsTransactions()
    suspend fun insertPendingSmsTransaction(item: PendingSmsTransaction): Long = financeDao.insertPendingSmsTransaction(item)
    suspend fun deletePendingSmsTransaction(item: PendingSmsTransaction) = financeDao.deletePendingSmsTransaction(item)
    suspend fun deletePendingSmsTransactionById(id: Long) = financeDao.deletePendingSmsTransactionById(id)
    suspend fun clearAllPendingSmsTransactions() = financeDao.clearAllPendingSmsTransactions()

    // PURGE & RESET ACTIONS
    suspend fun deleteTransactionsByYearMonth(yearMonth: String): Int = financeDao.deleteTransactionsByYearMonth(yearMonth)
    suspend fun clearAllTransactions() = financeDao.clearAllTransactions()

    suspend fun fullFactoryReset() {
        financeDao.clearAllTransactions()
        financeDao.clearAllCreditCards()
        financeDao.clearAllBankAccounts()
        financeDao.clearAllAssets()
        financeDao.clearAllBudgets()
        financeDao.clearAllSubscriptions()
        financeDao.clearAllSavingsGoals()
        financeDao.clearAllBorrowLends()
        financeDao.clearAllWishlist()
        financeDao.clearAllCustomCategories()
        financeDao.clearAllPendingSmsTransactions()
    }
}

