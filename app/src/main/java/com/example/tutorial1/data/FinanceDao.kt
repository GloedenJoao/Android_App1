package com.example.tutorial1.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts")
    fun observeAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM credit_cards LIMIT 1")
    fun observeCreditCard(): Flow<CreditCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCreditCard(card: CreditCard)

    @Query("SELECT * FROM salary LIMIT 1")
    fun observeSalary(): Flow<Salary?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalary(salary: Salary)

    @Query("SELECT * FROM vale_balances")
    fun observeVales(): Flow<List<ValeBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVale(balance: ValeBalance)

    @Query("SELECT * FROM transactions")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transfers")
    fun observeTransfers(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity)

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM transfers")
    suspend fun clearTransfers()

    @Query("UPDATE credit_cards SET openAmount = :amount WHERE id = :cardId")
    suspend fun updateCardAmount(cardId: Int, amount: Double)

    @Query("UPDATE accounts SET balance = :balance WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Int, balance: Double)

    @Query("UPDATE vale_balances SET balance = :balance WHERE type = :type")
    suspend fun updateValeBalance(type: ValeType, balance: Double)
}
