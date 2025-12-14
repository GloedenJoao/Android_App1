package com.example.tutorial1.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tutorial1.data.Account
import com.example.tutorial1.data.AccountType
import com.example.tutorial1.data.CreditCard
import com.example.tutorial1.data.DestinationType
import com.example.tutorial1.data.FinanceRepository
import com.example.tutorial1.data.Salary
import com.example.tutorial1.data.TransactionEntity
import com.example.tutorial1.data.TransferEntity
import com.example.tutorial1.data.ValeBalance
import com.example.tutorial1.data.ValeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val card = repository.card.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val salary = repository.salary.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val vales = repository.valeBalances.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transactions = repository.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transfers = repository.transfers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val simulationDays = MutableStateFlow(60)

    init {
        viewModelScope.launch { repository.ensureDefaults() }
    }

    fun saveAccount(account: Account) = viewModelScope.launch { repository.saveAccount(account) }
    fun deleteAccount(account: Account) = viewModelScope.launch { repository.deleteAccount(account) }
    fun saveCreditCard(card: CreditCard) = viewModelScope.launch { repository.saveCreditCard(card) }
    fun saveSalary(salary: Salary) = viewModelScope.launch { repository.saveSalary(salary) }
    fun saveVale(balance: ValeBalance) = viewModelScope.launch { repository.saveVale(balance) }
    fun saveTransaction(transaction: TransactionEntity) = viewModelScope.launch { repository.saveTransaction(transaction) }
    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch { repository.deleteTransaction(transaction) }
    fun saveTransfer(transfer: TransferEntity) = viewModelScope.launch { repository.saveTransfer(transfer) }
    fun deleteTransfer(transfer: TransferEntity) = viewModelScope.launch { repository.deleteTransfer(transfer) }
    fun clearTransactions() = viewModelScope.launch { repository.clearTransactions() }
    fun clearTransfers() = viewModelScope.launch { repository.clearTransfers() }

    data class DayBalances(
        val date: LocalDate,
        val accounts: Map<Int, Double>,
        val vales: Map<ValeType, Double>,
        val cardBalance: Double
    )

    data class SimulationEvent(
        val date: LocalDate,
        val description: String,
        val amount: Double,
        val destination: String
    )

    val simulation = combine(
        accounts,
        card,
        salary,
        vales,
        transactions,
        transfers,
        simulationDays
    ) { accounts, card, salary, vales, transactions, transfers, days ->
        computeSimulation(days, accounts, card, salary, vales, transactions, transfers)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SimulationResult(emptyList(), emptyList()))

    data class SimulationResult(
        val days: List<DayBalances>,
        val events: List<SimulationEvent>
    )

    private fun computeSimulation(
        days: Int,
        accounts: List<Account>,
        card: CreditCard?,
        salary: Salary?,
        vales: List<ValeBalance>,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>
    ): SimulationResult {
        if (accounts.isEmpty() || card == null || salary == null) {
            return SimulationResult(emptyList(), emptyList())
        }
        val checking = accounts.firstOrNull { it.type == AccountType.CORRENTE } ?: accounts.first()
        val accountBalances = accounts.associate { it.id to it.balance }.toMutableMap()
        val valeBalances = vales.associate { it.type to it.balance }.toMutableMap()
        var cardBalance = card.openAmount
        val resultDays = mutableListOf<DayBalances>()
        val eventLog = mutableListOf<SimulationEvent>()

        repeat(days) { index ->
            val date = today.plusDays(index.toLong())
            // Salary
            if (isSalaryDay(date, salary.payDay)) {
                accountBalances[checking.id] = (accountBalances[checking.id] ?: 0.0) + salary.amount
                eventLog.add(SimulationEvent(date, "Salário", salary.amount, checking.name))
            }
            // Vale credits
            ValeType.values().forEach { type ->
                val creditDay = penultimateBusinessDay(date.year, date.monthValue)
                if (date == creditDay) {
                    val value = when (type) {
                        ValeType.VALE_REFEICAO -> 1236.40
                        ValeType.VALE_ALIMENTACAO -> 974.16
                    }
                    valeBalances[type] = (valeBalances[type] ?: 0.0) + value
                    eventLog.add(SimulationEvent(date, "Crédito ${type.name.lowercase()}", value, type.name))
                }
            }
            // Card payment
            if (date.dayOfMonth == card.dueDay) {
                val payment = -cardBalance
                accountBalances[checking.id] = (accountBalances[checking.id] ?: 0.0) - payment
                cardBalance = 0.0
                eventLog.add(SimulationEvent(date, "Pagamento fatura", -payment, checking.name))
            }
            // Transactions
            transactions.forEach { transaction ->
                if (isDateInRange(date, transaction.startDate, transaction.endDate)) {
                    when (transaction.destinationType) {
                        DestinationType.ACCOUNT -> transaction.accountId?.let { id ->
                            accountBalances[id] = (accountBalances[id] ?: 0.0) + transaction.amount
                        }
                        DestinationType.CREDIT_CARD -> {
                            cardBalance += transaction.amount
                        }
                        DestinationType.VALE_REFEICAO -> {
                            valeBalances[ValeType.VALE_REFEICAO] = (valeBalances[ValeType.VALE_REFEICAO]
                                ?: 0.0) + transaction.amount
                        }
                        DestinationType.VALE_ALIMENTACAO -> {
                            valeBalances[ValeType.VALE_ALIMENTACAO] = (valeBalances[ValeType.VALE_ALIMENTACAO]
                                ?: 0.0) + transaction.amount
                        }
                    }
                    eventLog.add(
                        SimulationEvent(
                            date,
                            transaction.description,
                            transaction.amount,
                            transaction.destinationType.name
                        )
                    )
                }
            }
            // Transfers
            transfers.forEach { transfer ->
                if (isDateInRange(date, transfer.startDate, transfer.endDate)) {
                    val originBalance = (accountBalances[transfer.originAccountId] ?: 0.0) - transfer.amount
                    val destBalance = (accountBalances[transfer.destinationAccountId] ?: 0.0) + transfer.amount
                    accountBalances[transfer.originAccountId] = originBalance
                    accountBalances[transfer.destinationAccountId] = destBalance
                    eventLog.add(SimulationEvent(date, transfer.description, transfer.amount, "Transferência"))
                }
            }
            resultDays.add(
                DayBalances(
                    date,
                    accountBalances.toMap(),
                    valeBalances.toMap(),
                    cardBalance
                )
            )
        }

        return SimulationResult(resultDays, eventLog.sortedBy { it.date })
    }

    private fun isSalaryDay(date: LocalDate, payDay: Int): Boolean {
        var payDate = date.withDayOfMonth(payDay.coerceAtMost(date.lengthOfMonth()))
        if (payDate.dayOfWeek == DayOfWeek.SATURDAY) {
            payDate = payDate.minusDays(1)
        } else if (payDate.dayOfWeek == DayOfWeek.SUNDAY) {
            payDate = payDate.minusDays(2)
        }
        return date == payDate
    }

    private fun penultimateBusinessDay(year: Int, month: Int): LocalDate {
        val yearMonth = YearMonth.of(year, month)
        var date = yearMonth.atEndOfMonth()
        var businessDays = 0
        while (businessDays < 2) {
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                businessDays++
            }
            if (businessDays < 2) {
                date = date.minusDays(1)
            }
        }
        // date currently at penultimate business day
        return date
    }

    private fun isDateInRange(date: LocalDate, start: LocalDate, end: LocalDate?): Boolean {
        val endDate = end ?: start
        return !date.isBefore(start) && !date.isAfter(endDate)
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
