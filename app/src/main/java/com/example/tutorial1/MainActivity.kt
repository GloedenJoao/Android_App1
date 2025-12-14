package com.example.tutorial1

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

private const val DB_NAME = "finance.db"
private const val DB_VERSION = 1

class FinanceDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                type TEXT,
                balance REAL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE credit_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                due_day INTEGER,
                open_amount REAL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE salary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL,
                payday INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE vale_balances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vale_type TEXT,
                balance REAL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT,
                amount REAL,
                date TEXT,
                target_type TEXT,
                account_id INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE transfers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT,
                amount REAL,
                date TEXT,
                from_account_id INTEGER,
                to_account_id INTEGER
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE future_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                description TEXT,
                amount REAL,
                target TEXT,
                source TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS accounts")
        db.execSQL("DROP TABLE IF EXISTS credit_cards")
        db.execSQL("DROP TABLE IF EXISTS salary")
        db.execSQL("DROP TABLE IF EXISTS vale_balances")
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS transfers")
        db.execSQL("DROP TABLE IF EXISTS future_events")
        onCreate(db)
    }
}

data class Account(val id: Int, val name: String, val type: String, val balance: Double)
data class CreditCard(val id: Int, val name: String, val dueDay: Int, val openAmount: Double)
data class Salary(val id: Int, val amount: Double, val payday: Int)
data class ValeBalance(val id: Int, val valeType: String, val balance: Double)
data class FinanceTransaction(val id: Int, val description: String, val amount: Double, val date: String, val targetType: String, val accountId: Int?)
data class Transfer(val id: Int, val description: String, val amount: Double, val date: String, val fromAccountId: Int, val toAccountId: Int)
data class FutureEvent(val id: Int, val date: String, val description: String, val amount: Double, val target: String, val source: String)

data class Overview(val corrente: Double, val caixinhas: Double, val creditCard: Double, val vales: Double)
data class DailySnapshot(
    val date: LocalDate,
    val accountBalances: Map<Int, Double>,
    val valeBalances: Map<String, Double>,
    val creditCardBalance: Double
)

data class DashboardSummary(
    val totalStart: Double,
    val totalEnd: Double,
    val variation: Double,
    val valeTotalStart: Double,
    val valeTotalEnd: Double,
    val valeVariation: Double,
    val daily: List<DailySnapshot>
)

class FinanceRepository(private val context: Context) {
    private val helper = FinanceDatabaseHelper(context)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    private suspend fun ensureMainAccount() = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT id FROM accounts WHERE type='corrente' LIMIT 1", null)
            cursor.use {
                if (!it.moveToFirst()) {
                    val values = ContentValues().apply {
                        put("name", "Conta Corrente")
                        put("type", "corrente")
                        put("balance", 0.0)
                    }
                    db.insert("accounts", null, values)
                }
            }
        }
    }

    suspend fun upsertCurrentAccount(balance: Double) {
        ensureMainAccount()
        withContext(Dispatchers.IO) {
            helper.writableDatabase.use { db ->
                db.execSQL("UPDATE accounts SET balance=? WHERE type='corrente'", arrayOf(balance))
            }
        }
    }

    suspend fun addOrUpdateCaixinha(name: String, balance: Double) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT id FROM accounts WHERE name=? AND type='caixinha'", arrayOf(name))
            val exists = cursor.use { it.moveToFirst() }
            val values = ContentValues().apply {
                put("name", name)
                put("type", "caixinha")
                put("balance", balance)
            }
            if (exists) {
                db.update("accounts", values, "name=? AND type='caixinha'", arrayOf(name))
            } else {
                db.insert("accounts", null, values)
            }
        }
    }

    suspend fun setCreditCard(name: String, dueDay: Int, openAmount: Double) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT id FROM credit_cards LIMIT 1", null)
            val exists = cursor.use { it.moveToFirst() }
            val values = ContentValues().apply {
                put("name", name)
                put("due_day", dueDay)
                put("open_amount", openAmount)
            }
            if (exists) {
                db.update("credit_cards", values, "id=?", arrayOf("1"))
            } else {
                db.insert("credit_cards", null, values)
            }
        }
    }

    suspend fun setSalary(amount: Double, payday: Int) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            db.delete("salary", null, null)
            val values = ContentValues().apply {
                put("amount", amount)
                put("payday", payday)
            }
            db.insert("salary", null, values)
        }
    }

    suspend fun updateVale(type: String, balance: Double) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT id FROM vale_balances WHERE vale_type=?", arrayOf(type))
            val exists = cursor.use { it.moveToFirst() }
            val values = ContentValues().apply {
                put("vale_type", type)
                put("balance", balance)
            }
            if (exists) {
                db.update("vale_balances", values, "vale_type=?", arrayOf(type))
            } else {
                db.insert("vale_balances", null, values)
            }
        }
    }

    suspend fun addTransaction(description: String, amount: Double, date: String, targetType: String, accountId: Int?) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("description", description)
                put("amount", amount)
                put("date", date)
                put("target_type", targetType)
                accountId?.let { put("account_id", it) }
            }
            db.insert("transactions", null, values)
        }
    }

    suspend fun addTransfer(description: String, amount: Double, date: String, from: Int, to: Int) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("description", description)
                put("amount", amount)
                put("date", date)
                put("from_account_id", from)
                put("to_account_id", to)
            }
            db.insert("transfers", null, values)
        }
    }

    suspend fun addFutureEvent(date: String, description: String, amount: Double, target: String, source: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("date", date)
                put("description", description)
                put("amount", amount)
                put("target", target)
                put("source", source)
            }
            db.insert("future_events", null, values)
        }
    }

    suspend fun deleteTransaction(id: Int) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { it.delete("transactions", "id=?", arrayOf(id.toString())) }
    }

    suspend fun deleteTransfer(id: Int) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { it.delete("transfers", "id=?", arrayOf(id.toString())) }
    }

    suspend fun deleteFutureEvent(id: Int) = withContext(Dispatchers.IO) {
        helper.writableDatabase.use { it.delete("future_events", "id=?", arrayOf(id.toString())) }
    }

    suspend fun loadOverview(): Overview = withContext(Dispatchers.IO) {
        ensureMainAccount()
        helper.readableDatabase.use { db ->
            val corrente = db.rawQuery("SELECT balance FROM accounts WHERE type='corrente' LIMIT 1", null).use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
            val caixinhas = db.rawQuery("SELECT SUM(balance) FROM accounts WHERE type='caixinha'", null).use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
            val credit = db.rawQuery("SELECT open_amount FROM credit_cards LIMIT 1", null).use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
            val vales = db.rawQuery("SELECT SUM(balance) FROM vale_balances", null).use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
            Overview(corrente, caixinhas, credit, vales)
        }
    }

    private fun Cursor.toAccountList(): List<Account> {
        val accounts = mutableListOf<Account>()
        while (moveToNext()) {
            accounts.add(Account(getInt(0), getString(1), getString(2), getDouble(3)))
        }
        return accounts
    }

    private fun Cursor.toCreditCards(): List<CreditCard> {
        val cards = mutableListOf<CreditCard>()
        while (moveToNext()) {
            cards.add(CreditCard(getInt(0), getString(1), getInt(2), getDouble(3)))
        }
        return cards
    }

    suspend fun loadAccounts(): List<Account> = withContext(Dispatchers.IO) {
        ensureMainAccount()
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,name,type,balance FROM accounts", null).use { cursor ->
                cursor.toAccountList()
            }
        }
    }

    suspend fun loadCreditCards(): List<CreditCard> = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,name,due_day,open_amount FROM credit_cards", null).use { it.toCreditCards() }
        }
    }

    suspend fun loadSalary(): Salary? = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,amount,payday FROM salary LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) Salary(cursor.getInt(0), cursor.getDouble(1), cursor.getInt(2)) else null
            }
        }
    }

    suspend fun loadVales(): List<ValeBalance> = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,vale_type,balance FROM vale_balances", null).use { cursor ->
                val list = mutableListOf<ValeBalance>()
                while (cursor.moveToNext()) list.add(ValeBalance(cursor.getInt(0), cursor.getString(1), cursor.getDouble(2)))
                list
            }
        }
    }

    suspend fun loadTransactions(): List<FinanceTransaction> = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,description,amount,date,target_type,account_id FROM transactions ORDER BY date", null).use { cursor ->
                val list = mutableListOf<FinanceTransaction>()
                while (cursor.moveToNext()) {
                    val accountId = if (cursor.isNull(5)) null else cursor.getInt(5)
                    list.add(
                        FinanceTransaction(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getDouble(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            accountId
                        )
                    )
                }
                list
            }
        }
    }

    suspend fun loadTransfers(): List<Transfer> = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,description,amount,date,from_account_id,to_account_id FROM transfers ORDER BY date", null).use { cursor ->
                val list = mutableListOf<Transfer>()
                while (cursor.moveToNext()) {
                    list.add(
                        Transfer(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getDouble(2),
                            cursor.getString(3),
                            cursor.getInt(4),
                            cursor.getInt(5)
                        )
                    )
                }
                list
            }
        }
    }

    suspend fun loadFutureEvents(): List<FutureEvent> = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { db ->
            db.rawQuery("SELECT id,date,description,amount,target,source FROM future_events ORDER BY date", null).use { cursor ->
                val list = mutableListOf<FutureEvent>()
                while (cursor.moveToNext()) {
                    list.add(
                        FutureEvent(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getDouble(3),
                            cursor.getString(4),
                            cursor.getString(5)
                        )
                    )
                }
                list
            }
        }
    }

    suspend fun simulate(days: Int, selectedAccountIds: Set<Int>): DashboardSummary = withContext(Dispatchers.IO) {
        ensureMainAccount()
        val accounts = loadAccounts()
        val vales = loadVales()
        val transactions = loadTransactions()
        val transfers = loadTransfers()
        val events = loadFutureEvents()
        val salary = loadSalary()
        val creditCard = loadCreditCards().firstOrNull()
        val baseAccounts = accounts.associate { it.id to it.balance }.toMutableMap()
        val valeBalances = vales.associate { it.valeType to it.balance }.toMutableMap()
        var cardBalance = creditCard?.openAmount ?: 0.0
        val startTotal = baseAccounts.filterKeys { selectedAccountIds.isEmpty() || selectedAccountIds.contains(it) }.values.sum() + cardBalance
        val startVale = valeBalances.values.sum()
        val snapshots = mutableListOf<DailySnapshot>()

        repeat(days) { offset ->
            val date = LocalDate.now().plusDays(offset.toLong())
            val dayString = date.format(formatter)
            if (salary != null && date.dayOfMonth == salary.payday) {
                val correnteId = accounts.first { it.type == "corrente" }.id
                baseAccounts[correnteId] = (baseAccounts[correnteId] ?: 0.0) + salary.amount
            }
            transactions.filter { it.date == dayString }.forEach { transaction ->
                when (transaction.targetType) {
                    "account", "caixinha", "corrente" -> {
                        val accountId = transaction.accountId ?: accounts.first { it.type == "corrente" }.id
                        baseAccounts[accountId] = (baseAccounts[accountId] ?: 0.0) + transaction.amount
                    }
                    "credit_card" -> cardBalance += transaction.amount
                    "vale_refeicao", "vale_alimentacao" -> {
                        val key = if (transaction.targetType == "vale_refeicao") "vale_refeicao" else "vale_alimentacao"
                        valeBalances[key] = (valeBalances[key] ?: 0.0) + transaction.amount
                    }
                }
            }
            transfers.filter { it.date == dayString }.forEach { transfer ->
                baseAccounts[transfer.fromAccountId] = (baseAccounts[transfer.fromAccountId] ?: 0.0) - transfer.amount
                baseAccounts[transfer.toAccountId] = (baseAccounts[transfer.toAccountId] ?: 0.0) + transfer.amount
            }
            events.filter { it.date == dayString }.forEach { event ->
                if (event.target.contains("vale")) {
                    valeBalances[event.target] = (valeBalances[event.target] ?: 0.0) + event.amount
                } else {
                    val accountId = accounts.firstOrNull { it.name == event.target }?.id ?: accounts.first { it.type == "corrente" }.id
                    baseAccounts[accountId] = (baseAccounts[accountId] ?: 0.0) + event.amount
                }
                if (event.source.contains("conta")) {
                    val accountId = accounts.first { it.type == "corrente" }.id
                    baseAccounts[accountId] = (baseAccounts[accountId] ?: 0.0) - event.amount.absoluteValue
                }
            }
            creditCard?.let { card ->
                if (date.dayOfMonth == card.dueDay && cardBalance < 0) {
                    val correnteId = accounts.first { it.type == "corrente" }.id
                    baseAccounts[correnteId] = (baseAccounts[correnteId] ?: 0.0) + cardBalance
                    cardBalance = 0.0
                }
            }
            snapshots.add(
                DailySnapshot(
                    date,
                    baseAccounts.toMap(),
                    valeBalances.toMap(),
                    cardBalance
                )
            )
        }
        val endTotal = snapshots.lastOrNull()?.let { snap ->
            snap.accountBalances.filterKeys { selectedAccountIds.isEmpty() || selectedAccountIds.contains(it) }.values.sum() + snap.creditCardBalance
        } ?: startTotal
        val endVale = snapshots.lastOrNull()?.valeBalances?.values?.sum() ?: startVale
        DashboardSummary(
            totalStart = startTotal,
            totalEnd = endTotal,
            variation = endTotal - startTotal,
            valeTotalStart = startVale,
            valeTotalEnd = endVale,
            valeVariation = endVale - startVale,
            daily = snapshots
        )
    }
}

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinanceRepository(application.applicationContext)

    var overview by mutableStateOf(Overview(0.0, 0.0, 0.0, 0.0))
        private set
    var accounts by mutableStateOf(listOf<Account>())
        private set
    var creditCards by mutableStateOf(listOf<CreditCard>())
        private set
    var salary by mutableStateOf<Salary?>(null)
        private set
    var vales by mutableStateOf(listOf<ValeBalance>())
        private set
    var transactions by mutableStateOf(listOf<FinanceTransaction>())
        private set
    var transfers by mutableStateOf(listOf<Transfer>())
        private set
    var futureEvents by mutableStateOf(listOf<FutureEvent>())
        private set
    var dashboard by mutableStateOf<DashboardSummary?>(null)
        private set

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            overview = repo.loadOverview()
            accounts = repo.loadAccounts()
            creditCards = repo.loadCreditCards()
            salary = repo.loadSalary()
            vales = repo.loadVales()
            transactions = repo.loadTransactions()
            transfers = repo.loadTransfers()
            futureEvents = repo.loadFutureEvents()
            dashboard = repo.simulate(30, emptySet())
        }
    }

    fun updateCorrente(balance: Double) {
        viewModelScope.launch {
            repo.upsertCurrentAccount(balance)
            refreshAll()
        }
    }

    fun updateCaixinha(name: String, balance: Double) {
        viewModelScope.launch {
            repo.addOrUpdateCaixinha(name, balance)
            refreshAll()
        }
    }

    fun updateCreditCard(name: String, dueDay: Int, openAmount: Double) {
        viewModelScope.launch {
            repo.setCreditCard(name, dueDay, openAmount)
            refreshAll()
        }
    }

    fun updateSalary(amount: Double, payday: Int) {
        viewModelScope.launch {
            repo.setSalary(amount, payday)
            refreshAll()
        }
    }

    fun updateVale(type: String, balance: Double) {
        viewModelScope.launch {
            repo.updateVale(type, balance)
            refreshAll()
        }
    }

    fun saveTransaction(description: String, amount: Double, date: String, targetType: String, accountId: Int?) {
        viewModelScope.launch {
            repo.addTransaction(description, amount, date, targetType, accountId)
            refreshAll()
        }
    }

    fun saveTransfer(description: String, amount: Double, date: String, from: Int, to: Int) {
        viewModelScope.launch {
            repo.addTransfer(description, amount, date, from, to)
            refreshAll()
        }
    }

    fun saveFutureEvent(date: String, description: String, amount: Double, target: String, source: String) {
        viewModelScope.launch {
            repo.addFutureEvent(date, description, amount, target, source)
            refreshAll()
        }
    }

    fun removeTransaction(id: Int) {
        viewModelScope.launch { repo.deleteTransaction(id); refreshAll() }
    }

    fun removeTransfer(id: Int) {
        viewModelScope.launch { repo.deleteTransfer(id); refreshAll() }
    }

    fun removeFutureEvent(id: Int) {
        viewModelScope.launch { repo.deleteFutureEvent(id); refreshAll() }
    }

    fun recalcDashboard(days: Int, selectedIds: Set<Int>) {
        viewModelScope.launch { dashboard = repo.simulate(days, selectedIds) }
    }

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FinanceViewModel(app) as T
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: FinanceViewModel = viewModel(factory = FinanceViewModel.provideFactory(application))
                FinanceApp(viewModel)
            }
        }
    }
}

@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Painel", "Configuração", "Simulação", "Dashboard")
    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }
            when (selectedTab) {
                0 -> OverviewScreen(viewModel)
                1 -> ConfigScreen(viewModel)
                2 -> SimulationScreen(viewModel)
                3 -> DashboardScreen(viewModel)
            }
        }
    }
}

@Composable
fun OverviewScreen(viewModel: FinanceViewModel) {
    val overview = viewModel.overview
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Painel de saldos", style = MaterialTheme.typography.titleLarge)
        }
        item { SummaryCard("Conta corrente", overview.corrente) }
        item { SummaryCard("Caixinhas", overview.caixinhas) }
        item { SummaryCard("Fatura em aberto", overview.creditCard) }
        item { SummaryCard("Vales", overview.vales) }
    }
}

@Composable
fun SummaryCard(title: String, value: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("R$ %.2f".format(value), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfigScreen(viewModel: FinanceViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Atualizar valores", style = MaterialTheme.typography.titleLarge) }
        item { CorrenteForm(viewModel) }
        item { CaixinhaForm(viewModel) }
        item { CreditCardForm(viewModel) }
        item { SalaryForm(viewModel) }
        item { ValesForm(viewModel) }
    }
}

@Composable
fun CorrenteForm(viewModel: FinanceViewModel) {
    var balance by remember { mutableStateOf(viewModel.overview.corrente.toString()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Saldo da conta corrente", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Saldo") })
            Button(onClick = { viewModel.updateCorrente(balance.toDoubleOrNull() ?: 0.0) }) { Text("Salvar") }
        }
    }
}

@Composable
fun CaixinhaForm(viewModel: FinanceViewModel) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("0.0") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Caixinhas de CDB", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
            OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Saldo") })
            Button(onClick = { if (name.isNotBlank()) viewModel.updateCaixinha(name, balance.toDoubleOrNull() ?: 0.0) }) {
                Text("Adicionar/Atualizar")
            }
            Text("Caixinhas existentes:")
            viewModel.accounts.filter { it.type == "caixinha" }.forEach { account ->
                Text("- ${account.name}: R$ %.2f".format(account.balance))
            }
        }
    }
}

@Composable
fun CreditCardForm(viewModel: FinanceViewModel) {
    var name by remember { mutableStateOf(viewModel.creditCards.firstOrNull()?.name ?: "") }
    var dueDay by remember { mutableStateOf(viewModel.creditCards.firstOrNull()?.dueDay?.toString() ?: "1") }
    var openAmount by remember { mutableStateOf(viewModel.creditCards.firstOrNull()?.openAmount?.toString() ?: "0.0") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cartão de crédito", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
            OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("Dia de vencimento") })
            OutlinedTextField(value = openAmount, onValueChange = { openAmount = it }, label = { Text("Valor da fatura (negativo)") })
            Button(onClick = { viewModel.updateCreditCard(name, dueDay.toIntOrNull() ?: 1, openAmount.toDoubleOrNull() ?: 0.0) }) {
                Text("Salvar")
            }
        }
    }
}

@Composable
fun SalaryForm(viewModel: FinanceViewModel) {
    var amount by remember { mutableStateOf(viewModel.salary?.amount?.toString() ?: "0.0") }
    var payday by remember { mutableStateOf(viewModel.salary?.payday?.toString() ?: "1") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Salário", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Valor") })
            OutlinedTextField(value = payday, onValueChange = { payday = it }, label = { Text("Dia do pagamento") })
            Button(onClick = { viewModel.updateSalary(amount.toDoubleOrNull() ?: 0.0, payday.toIntOrNull() ?: 1) }) { Text("Salvar") }
        }
    }
}

@Composable
fun ValesForm(viewModel: FinanceViewModel) {
    var refeicao by remember { mutableStateOf(viewModel.vales.firstOrNull { it.valeType == "vale_refeicao" }?.balance?.toString() ?: "0.0") }
    var alimentacao by remember { mutableStateOf(viewModel.vales.firstOrNull { it.valeType == "vale_alimentacao" }?.balance?.toString() ?: "0.0") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vales", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = refeicao, onValueChange = { refeicao = it }, label = { Text("Vale refeição") })
            OutlinedTextField(value = alimentacao, onValueChange = { alimentacao = it }, label = { Text("Vale alimentação") })
            Button(onClick = {
                viewModel.updateVale("vale_refeicao", refeicao.toDoubleOrNull() ?: 0.0)
                viewModel.updateVale("vale_alimentacao", alimentacao.toDoubleOrNull() ?: 0.0)
            }) { Text("Salvar vales") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(viewModel: FinanceViewModel) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("0.0") }
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var targetType by remember { mutableStateOf("account") }
    var accountId by remember { mutableStateOf(viewModel.accounts.firstOrNull()?.id?.toString() ?: "") }
    var transferDescription by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("0.0") }
    var transferDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var fromAccount by remember { mutableStateOf(viewModel.accounts.firstOrNull()?.id?.toString() ?: "") }
    var toAccount by remember { mutableStateOf(viewModel.accounts.firstOrNull { it.type == "caixinha" }?.id?.toString() ?: "") }
    var eventDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var eventDescription by remember { mutableStateOf("") }
    var eventAmount by remember { mutableStateOf("0.0") }
    var target by remember { mutableStateOf("Conta Corrente") }
    var source by remember { mutableStateOf("Automático") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Nova transação", style = MaterialTheme.typography.titleLarge) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") })
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Valor") })
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (AAAA-MM-DD)") })
                    OutlinedTextField(value = targetType, onValueChange = { targetType = it }, label = { Text("Destino (account/credit_card/vale_refeicao/vale_alimentacao)") })
                    OutlinedTextField(value = accountId, onValueChange = { accountId = it }, label = { Text("ID da conta (opcional)") })
                    Button(onClick = {
                        viewModel.saveTransaction(description, amount.toDoubleOrNull() ?: 0.0, date, targetType, accountId.toIntOrNull())
                        description = ""; amount = "0.0"
                    }) { Text("Registrar transação") }
                }
            }
        }
        item { Text("Transferência", style = MaterialTheme.typography.titleLarge) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = transferDescription, onValueChange = { transferDescription = it }, label = { Text("Descrição") })
                    OutlinedTextField(value = transferAmount, onValueChange = { transferAmount = it }, label = { Text("Valor") })
                    OutlinedTextField(value = transferDate, onValueChange = { transferDate = it }, label = { Text("Data (AAAA-MM-DD)") })
                    OutlinedTextField(value = fromAccount, onValueChange = { fromAccount = it }, label = { Text("Conta origem (ID)") })
                    OutlinedTextField(value = toAccount, onValueChange = { toAccount = it }, label = { Text("Conta destino (ID)") })
                    Button(onClick = {
                        val from = fromAccount.toIntOrNull()
                        val to = toAccount.toIntOrNull()
                        if (from != null && to != null) {
                            viewModel.saveTransfer(transferDescription, transferAmount.toDoubleOrNull() ?: 0.0, transferDate, from, to)
                        }
                    }) { Text("Registrar transferência") }
                }
            }
        }
        item { Text("Eventos futuros", style = MaterialTheme.typography.titleLarge) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = eventDescription, onValueChange = { eventDescription = it }, label = { Text("Descrição") })
                    OutlinedTextField(value = eventAmount, onValueChange = { eventAmount = it }, label = { Text("Valor") })
                    OutlinedTextField(value = eventDate, onValueChange = { eventDate = it }, label = { Text("Data (AAAA-MM-DD)") })
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Destino") })
                    OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Origem") })
                    Button(onClick = {
                        viewModel.saveFutureEvent(eventDate, eventDescription, eventAmount.toDoubleOrNull() ?: 0.0, target, source)
                    }) { Text("Adicionar evento") }
                }
            }
        }
        item { Text("Log de eventos futuros", style = MaterialTheme.typography.titleMedium) }
        items(viewModel.futureEvents) { event ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${event.date} - ${event.description}")
                    Text("Valor: %.2f | Destino: ${event.target}".format(event.amount))
                }
                IconButton(onClick = { viewModel.removeFutureEvent(event.id) }) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        }
        item { Divider() }
        item { Text("Transações lançadas", style = MaterialTheme.typography.titleMedium) }
        items(viewModel.transactions) { txn ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${txn.date} - ${txn.description}")
                    Text("Destino: ${txn.targetType} | Valor: %.2f".format(txn.amount))
                }
                IconButton(onClick = { viewModel.removeTransaction(txn.id) }) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        }
        item { Divider() }
        item { Text("Transferências", style = MaterialTheme.typography.titleMedium) }
        items(viewModel.transfers) { transfer ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${transfer.date} - ${transfer.description}")
                    Text("${transfer.fromAccountId} -> ${transfer.toAccountId} | Valor: %.2f".format(transfer.amount))
                }
                IconButton(onClick = { viewModel.removeTransfer(transfer.id) }) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        }
        item { Divider() }
        item { Text("Tabela diária", style = MaterialTheme.typography.titleLarge) }
        item { DailyTable(viewModel.dashboard?.daily ?: emptyList()) }
    }
}

@Composable
fun DailyTable(days: List<DailySnapshot>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(day.date.toString(), fontWeight = FontWeight.Bold)
                    Text("Contas: " + day.accountBalances.entries.joinToString { "${it.key}: %.2f".format(it.value) })
                    Text("Vales: " + day.valeBalances.entries.joinToString { "${it.key}: %.2f".format(it.value) })
                    Text("Fatura: %.2f".format(day.creditCardBalance))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    var days by remember { mutableIntStateOf(30) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    val accounts = viewModel.accounts
    val dashboard = viewModel.dashboard
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Filtros", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = days.toString(), onValueChange = { days = it.toIntOrNull()?.coerceIn(1,365) ?: 30 }, label = { Text("Dias (máx 365)") }, modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.recalcDashboard(days, selected) }) { Text("Aplicar") }
        }
        Text("Selecione contas para exibir")
        accounts.forEach { account ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(account.name, modifier = Modifier.weight(1f))
                val isSelected = selected.contains(account.id)
                TextButton(onClick = {
                    selected = if (isSelected) selected - account.id else selected + account.id
                }) { Text(if (isSelected) "Ocultar" else "Mostrar") }
            }
        }
        dashboard?.let { data ->
            SummaryCard("Total contas + cartão", data.totalEnd)
            SummaryCard("Variação", data.variation)
            SummaryCard("Vales", data.valeTotalEnd)
            SummaryCard("Variação vales", data.valeVariation)
            Text("Gráficos", style = MaterialTheme.typography.titleMedium)
            LineChart(data.daily.mapIndexed { index, snap -> index.toFloat() to snap.accountBalances.values.sum().toFloat() }, label = "Contas")
            LineChart(data.daily.mapIndexed { index, snap -> index.toFloat() to snap.valeBalances.values.sum().toFloat() }, label = "Vales")
        }
    }
}

@Composable
fun LineChart(points: List<Pair<Float, Float>>, label: String) {
    if (points.isEmpty()) return
    val maxY = points.maxOf { it.second }
    val minY = points.minOf { it.second }
    Card(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val widthStep = if (points.size > 1) size.width / (points.size - 1) else size.width
                    val normalized = points.mapIndexed { index, point ->
                        val yRange = (maxY - minY).takeIf { it != 0f } ?: 1f
                        val x = widthStep * index
                        val y = size.height - ((point.second - minY) / yRange) * size.height
                        x to y
                    }
                    val path = Path()
                    normalized.firstOrNull()?.let { (x, y) -> path.moveTo(x, y) }
                    normalized.drop(1).forEach { (x, y) -> path.lineTo(x, y) }
                    drawPath(path = path, color = Color(0xFF1976D2))
                }
            }
        }
    }
}
