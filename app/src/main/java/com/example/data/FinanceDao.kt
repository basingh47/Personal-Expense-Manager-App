package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // TRANSACTIONS
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE assetId = :assetId ORDER BY date DESC")
    fun getTransactionsByAsset(assetId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // ASSETS / VEHICLES
    @Query("SELECT * FROM assets ORDER BY purchaseDate DESC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: Long): Asset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset): Long

    @Delete
    suspend fun deleteAsset(asset: Asset)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    // BUDGETS
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsByMonth(monthYear: String): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    // SUBSCRIPTIONS
    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: Subscription): Long

    @Delete
    suspend fun deleteSubscription(sub: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)

    // SAVINGS GOALS
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)

    // BORROW / LEND
    @Query("SELECT * FROM borrow_lend ORDER BY dueDate ASC")
    fun getAllBorrowLends(): Flow<List<BorrowLend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrowLend(item: BorrowLend): Long

    @Delete
    suspend fun deleteBorrowLend(item: BorrowLend)

    @Query("DELETE FROM borrow_lend WHERE id = :id")
    suspend fun deleteBorrowLendById(id: Long)

    // WISHLIST
    @Query("SELECT * FROM wishlist ORDER BY targetDate ASC")
    fun getAllWishlistItems(): Flow<List<Wishlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: Wishlist): Long

    @Delete
    suspend fun deleteWishlistItem(item: Wishlist)

    @Query("DELETE FROM wishlist WHERE id = :id")
    suspend fun deleteWishlistItemById(id: Long)

    // CUSTOM CATEGORIES
    @Query("SELECT * FROM custom_categories")
    fun getAllCustomCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategory): Long

    @Delete
    suspend fun deleteCustomCategory(category: CustomCategory)

    // CREDIT CARDS
    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    fun getAllCreditCards(): Flow<List<CreditCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCard): Long

    @Delete
    suspend fun deleteCreditCard(card: CreditCard)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCardById(id: Long)

    @Query("SELECT * FROM transactions WHERE creditCardId = :creditCardId ORDER BY date DESC")
    fun getTransactionsByCreditCard(creditCardId: Long): Flow<List<Transaction>>
}
