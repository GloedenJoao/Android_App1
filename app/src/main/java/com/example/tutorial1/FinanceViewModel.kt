package com.example.tutorial1

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tutorial1.data.Account
import com.example.tutorial1.data.CreditCard
import com.example.tutorial1.data.DailyProjection
import com.example.tutorial1.data.FinanceRepository
import com.example.tutorial1.data.FutureEvent
import com.example.tutorial1.data.Salary
import com.example.tutorial1.data.Transaction
import com.example.tutorial1.data.Transfer
import com.example.tutorial1.data.ValeBalance
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

data class FinanceUiState(
    val accounts: List<Account> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val salary: Salary? = null,
    val valeBalances: List<ValeBalance> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val futureEvents: List<FutureEvent> = emptyList(),
    val dailyProjections: List<DailyProjection> = emptyList(),
    val daysToSimulate: Int = 30,
    val startDate: LocalDate = LocalDate.now(),
    val selectedAccounts: Set<String> = emptySet()
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(application.applicationContext)

    var uiState: FinanceUiState by mutableStateOf(FinanceUiState())
        private set

    init {
        viewModelScope.launch { refreshData() }
    }

    fun refreshProjections(days: Int = uiState.daysToSimulate) {
        viewModelScope.launch {
            val cappedDays = max(1, minOf(days, 365))
            uiState = uiState.copy(daysToSimulate = cappedDays)
            recomputeDailyProjection()
        }
    }

    fun selectAccount(name: String, selected: Boolean) {
        val updated = uiState.selectedAccounts.toMutableSet()
        if (selected) updated.add(name) else updated.remove(name)
        uiState = uiState.copy(selectedAccounts = updated)
    }

    fun updateStartDate(date: LocalDate) {
        uiState = uiState.copy(startDate = date)
        refreshProjections(uiState.daysToSimulate)
    }

    fun saveAccount(name: String, type: String, balance: Double, id: Long = 0L) {
        viewModelScope.launch {
            repository.upsertAccount(Account(id = id, name = name, type = type, balance = balance))
            refreshData()
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            refreshData()
        }
    }

    fun saveCreditCard(name: String, dueDay: Int, openAmount: Double, id: Long = 0L) {
        viewModelScope.launch {
            repository.upsertCreditCard(CreditCard(id, name, dueDay, openAmount))
            refreshData()
        }
    }

    fun saveSalary(amount: Double, payday: Int) {
        viewModelScope.launch {
            val existing = uiState.salary
            repository.saveSalary(
                Salary(
                    id = existing?.id ?: 0,
                    amount = amount,
                    payday = payday
                )
            )
            refreshData()
        }
    }

    fun saveVale(type: String, balance: Double) {
        viewModelScope.launch {
            val current = uiState.valeBalances.firstOrNull { it.valeType == type }
            repository.saveValeBalance(
                ValeBalance(
                    id = current?.id ?: 0,
                    valeType = type,
                    balance = balance
                )
            )
            refreshData()
        }
    }

    fun addTransaction(description: String, amount: Double, start: LocalDate, end: LocalDate?, targetType: String, accountId: Long?) {
        viewModelScope.launch {
            var date = start
            val endDate = end ?: start
            while (!date.isAfter(endDate)) {
                repository.insertTransaction(
                    Transaction(
                        description = description,
                        amount = amount,
                        date = date,
                        targetType = targetType,
                        accountId = accountId
                    )
                )
                date = date.plusDays(1)
            }
            refreshData()
        }
    }

    fun addTransfer(description: String, amount: Double, start: LocalDate, end: LocalDate?, fromId: Long, toId: Long) {
        viewModelScope.launch {
            var date = start
            val endDate = end ?: start
            while (!date.isAfter(endDate)) {
                repository.insertTransfer(
                    Transfer(
                        description = description,
                        amount = amount,
                        date = date,
                        fromAccountId = fromId,
                        toAccountId = toId
                    )
                )
                date = date.plusDays(1)
            }
            refreshData()
        }
    }

    fun addFutureEvent(date: LocalDate, description: String, amount: Double, target: String, source: String?) {
        viewModelScope.launch {
            repository.insertFutureEvent(
                FutureEvent(
                    date = date,
                    description = description,
                    amount = amount,
                    target = target,
                    source = source
                )
            )
            refreshData()
        }
    }

    fun deleteFutureEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteFutureEvent(id)
            refreshData()
        }
    }

    private suspend fun refreshData() {
        val accounts = repository.getAccounts()
        val cards = repository.getCreditCards()
        val salary = repository.getSalary()
        val vales = repository.getValeBalances()
        val transactions = repository.getTransactions()
        val transfers = repository.getTransfers()
        val futureEvents = repository.getFutureEvents()

        val selected = if (uiState.selectedAccounts.isEmpty()) {
            accounts.map { it.name }.toSet()
        } else uiState.selectedAccounts

        uiState = uiState.copy(
            accounts = accounts,
            creditCards = cards,
            salary = salary,
            valeBalances = vales,
            transactions = transactions,
            transfers = transfers,
            futureEvents = futureEvents,
            selectedAccounts = selected
        )
        recomputeDailyProjection()
    }

    private suspend fun recomputeDailyProjection() {
        val startDate = uiState.startDate
        val days = uiState.daysToSimulate
        val accounts = uiState.accounts.associateBy { it.id }
        val accountBalances = accounts.mapValues { it.value.balance }.toMutableMap()
        val valeBalances = uiState.valeBalances.associate { it.valeType to it.balance }.toMutableMap()
        var creditCardAmount = uiState.creditCards.firstOrNull()?.openAmount ?: 0.0

        val projections = mutableListOf<DailyProjection>()
        var currentDate = startDate

        repeat(days) {
            val dayTransactions = uiState.transactions.filter { it.date == currentDate }
            val dayTransfers = uiState.transfers.filter { it.date == currentDate }
            val dayEvents = uiState.futureEvents.filter { it.date == currentDate }.toMutableList()

            // salary event
            uiState.salary?.let { salary ->
                if (salary.payday == currentDate.dayOfMonth) {
                    val mainAccount = accounts.values.firstOrNull { it.type == "corrente" }
                    if (mainAccount != null) {
                        val updated = (accountBalances[mainAccount.id] ?: 0.0) + salary.amount
                        accountBalances[mainAccount.id] = updated
                        dayEvents.add(
                            FutureEvent(
                                description = "Salário",
                                amount = salary.amount,
                                date = currentDate,
                                target = mainAccount.name,
                                source = "Folha"
                            )
                        )
                    }
                }
            }

            // credit card payment on due day
            uiState.creditCards.firstOrNull()?.let { card ->
                if (currentDate.dayOfMonth == card.dueDay && creditCardAmount != 0.0) {
                    val mainAccount = accounts.values.firstOrNull { it.type == "corrente" }
                    if (mainAccount != null) {
                        val updated = (accountBalances[mainAccount.id] ?: 0.0) + creditCardAmount
                        accountBalances[mainAccount.id] = updated
                        creditCardAmount = 0.0
                    }
                }
            }

            dayTransactions.forEach { transaction ->
                when (transaction.targetType) {
                    "account", "caixinha" -> {
                        val id = transaction.accountId
                        if (id != null) {
                            accountBalances[id] = (accountBalances[id] ?: 0.0) + transaction.amount
                        }
                    }

                    "credit_card" -> creditCardAmount += transaction.amount
                    "vale_refeicao", "vale_alimentacao" -> {
                        val current = valeBalances[transaction.targetType] ?: 0.0
                        valeBalances[transaction.targetType] = current + transaction.amount
                    }
                }
            }

            dayTransfers.forEach { transfer ->
                val fromBalance = (accountBalances[transfer.fromAccountId] ?: 0.0) - transfer.amount
                val toBalance = (accountBalances[transfer.toAccountId] ?: 0.0) + transfer.amount
                accountBalances[transfer.fromAccountId] = fromBalance
                accountBalances[transfer.toAccountId] = toBalance
            }

            dayEvents.forEach { event ->
                val positiveTargets = setOf("account", "caixinha")
                when (event.target) {
                    in positiveTargets -> {
                        val account = accounts.values.firstOrNull { it.name == (event.source ?: it.name) || it.name == event.target }
                        if (account != null) {
                            accountBalances[account.id] = (accountBalances[account.id] ?: 0.0) + event.amount
                        }
                    }

                    "credit_card" -> creditCardAmount += event.amount
                    "vale_refeicao", "vale_alimentacao" -> {
                        valeBalances[event.target] = (valeBalances[event.target] ?: 0.0) + event.amount
                    }
                }
            }

            val namedAccounts = accounts.values.associate { it.name to (accountBalances[it.id] ?: 0.0) }
            val totalAccounts = namedAccounts.values.sum()
            val totalVales = valeBalances.values.sum()

            projections.add(
                DailyProjection(
                    date = currentDate,
                    accountBalances = namedAccounts,
                    valeBalances = valeBalances.toMap(),
                    creditCardAmount = creditCardAmount,
                    totalAccounts = totalAccounts,
                    totalVales = totalVales
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        uiState = uiState.copy(dailyProjections = projections)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return FinanceViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
