package com.example.tutorial1.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FinanceRepository(private val dao: FinanceDao) {
    val accounts: Flow<List<Account>> = dao.observeAccounts()
    val card: Flow<CreditCard?> = dao.observeCreditCard()
    val salary: Flow<Salary?> = dao.observeSalary()
    val valeBalances: Flow<List<ValeBalance>> = dao.observeVales()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val transfers: Flow<List<TransferEntity>> = dao.observeTransfers()

    suspend fun saveAccount(account: Account) = dao.insertAccount(account)
    suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    suspend fun saveCreditCard(card: CreditCard) = dao.upsertCreditCard(card)
    suspend fun saveSalary(salary: Salary) = dao.upsertSalary(salary)
    suspend fun saveVale(balance: ValeBalance) = dao.upsertVale(balance)
    suspend fun saveTransaction(transaction: TransactionEntity) = dao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)
    suspend fun saveTransfer(transfer: TransferEntity) = dao.insertTransfer(transfer)
    suspend fun deleteTransfer(transfer: TransferEntity) = dao.deleteTransfer(transfer)
    suspend fun clearTransactions() = dao.clearTransactions()
    suspend fun clearTransfers() = dao.clearTransfers()

    suspend fun ensureDefaults() {
        if (card.first() == null) {
            saveCreditCard(CreditCard(name = "Cartão", dueDay = 5, openAmount = -0.0))
        }
        if (salary.first() == null) {
            saveSalary(Salary(amount = 0.0, payDay = 5))
        }
        val existingVales = valeBalances.first().associateBy { it.type }
        ValeType.values().forEach { type ->
            if (existingVales[type] == null) {
                saveVale(ValeBalance(type = type, balance = 0.0))
            }
        }
    }

    suspend fun updateCardAmount(amount: Double) {
        card.first()?.let { dao.updateCardAmount(it.id, amount) }
    }

    suspend fun updateValeBalance(type: ValeType, balance: Double) {
        dao.updateValeBalance(type, balance)
    }

    suspend fun updateAccountBalance(accountId: Int, balance: Double) {
        dao.updateAccountBalance(accountId, balance)
    }
}
