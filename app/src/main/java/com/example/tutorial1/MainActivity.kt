package com.example.tutorial1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutorial1.data.Account
import com.example.tutorial1.data.DailyProjection
import com.example.tutorial1.data.FutureEvent
import com.example.tutorial1.data.ValeBalance
import com.example.tutorial1.ui.theme.Tutorial1Theme
import java.time.LocalDate
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Tutorial1Theme {
                val viewModel: FinanceViewModel = viewModel(factory = FinanceViewModel.provideFactory(application))
                FinanceApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    val state = viewModel.uiState
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Painel", "Simulação", "Dashboard")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Planejamento financeiro") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Settings, contentDescription = null)
                                1 -> Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                                else -> Icon(Icons.Default.Payment, contentDescription = null)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewScreen(state = state, viewModel = viewModel)
                1 -> SimulationScreen(state = state, viewModel = viewModel)
                2 -> DashboardScreen(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OverviewScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    val corrente = state.accounts.firstOrNull { it.type == "corrente" }
    val caixinhas = state.accounts.filter { it.type == "caixinha" }
    val valeRefeicao = state.valeBalances.firstOrNull { it.valeType == "vale_refeicao" }
    val valeAlimentacao = state.valeBalances.firstOrNull { it.valeType == "vale_alimentacao" }
    val card = state.creditCards.firstOrNull()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Painel", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryCard("Conta corrente", corrente?.balance ?: 0.0)
                SummaryCard("Caixinhas", caixinhas.sumOf { it.balance })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryCard("Fatura cartão", card?.openAmount ?: 0.0)
                SummaryCard("Vales", (valeRefeicao?.balance ?: 0.0) + (valeAlimentacao?.balance ?: 0.0))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Atualizar valores", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item { AccountForm(corrente, viewModel) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { CaixinhaForm(caixinhas, viewModel) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { CreditCardForm(card, viewModel) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { SalaryForm(state.salary, viewModel) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { ValeForm(valeRefeicao, valeAlimentacao, viewModel) }
    }
}

@Composable
fun SummaryCard(title: String, amount: Double) {
    Card(modifier = Modifier.weight(1f).padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "R$ %.2f".format(amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (amount >= 0) MaterialTheme.colorScheme.primary else Color.Red
            )
        }
    }
}

@Composable
fun AccountForm(corrente: Account?, viewModel: FinanceViewModel) {
    var balance by remember { mutableStateOf(corrente?.balance?.toString() ?: "") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Conta Corrente")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Saldo") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val numeric = balance.toDoubleOrNull() ?: 0.0
                viewModel.saveAccount(name = "Conta Corrente", type = "corrente", balance = numeric, id = corrente?.id ?: 0)
            }) {
                Text("Salvar saldo")
            }
        }
    }
}

@Composable
fun CaixinhaForm(caixinhas: List<Account>, viewModel: FinanceViewModel) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Caixinhas de CDB")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
            OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Saldo") })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val numeric = balance.toDoubleOrNull() ?: 0.0
                viewModel.saveAccount(name = name.ifEmpty { "Caixinha" }, type = "caixinha", balance = numeric)
                name = ""
                balance = ""
            }) {
                Text("Adicionar/Atualizar")
            }
            Spacer(modifier = Modifier.height(8.dp))
            caixinhas.forEach { caixinha ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(caixinha.name, fontWeight = FontWeight.Bold)
                        Text("R$ %.2f".format(caixinha.balance))
                    }
                    IconButton(onClick = { viewModel.deleteAccount(caixinha.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover")
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardForm(card: com.example.tutorial1.data.CreditCard?, viewModel: FinanceViewModel) {
    var name by remember { mutableStateOf(card?.name ?: "") }
    var dueDay by remember { mutableStateOf(card?.dueDay?.toString() ?: "") }
    var openAmount by remember { mutableStateOf(card?.openAmount?.toString() ?: "") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Cartão de crédito")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
            OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("Dia de vencimento") })
            OutlinedTextField(value = openAmount, onValueChange = { openAmount = it }, label = { Text("Valor fatura (negativo)") })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val day = dueDay.toIntOrNull() ?: 1
                val amount = openAmount.toDoubleOrNull() ?: 0.0
                viewModel.saveCreditCard(name.ifEmpty { "Cartão" }, day, amount, id = card?.id ?: 0)
            }) {
                Text("Salvar cartão")
            }
        }
    }
}

@Composable
fun SalaryForm(salary: com.example.tutorial1.data.Salary?, viewModel: FinanceViewModel) {
    var amount by remember { mutableStateOf(salary?.amount?.toString() ?: "") }
    var payday by remember { mutableStateOf(salary?.payday?.toString() ?: "") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Salário")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Valor") })
            OutlinedTextField(value = payday, onValueChange = { payday = it }, label = { Text("Dia do pagamento") })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                viewModel.saveSalary(amount.toDoubleOrNull() ?: 0.0, payday.toIntOrNull() ?: 1)
            }) {
                Text("Registrar salário")
            }
        }
    }
}

@Composable
fun ValeForm(refeicao: ValeBalance?, alimentacao: ValeBalance?, viewModel: FinanceViewModel) {
    var refeicaoValue by remember { mutableStateOf(refeicao?.balance?.toString() ?: "") }
    var alimentacaoValue by remember { mutableStateOf(alimentacao?.balance?.toString() ?: "") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Vales")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = refeicaoValue, onValueChange = { refeicaoValue = it }, label = { Text("Vale Refeição") })
            OutlinedTextField(value = alimentacaoValue, onValueChange = { alimentacaoValue = it }, label = { Text("Vale Alimentação") })
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.saveVale("vale_refeicao", refeicaoValue.toDoubleOrNull() ?: 0.0) }) { Text("Salvar VR") }
                Button(onClick = { viewModel.saveVale("vale_alimentacao", alimentacaoValue.toDoubleOrNull() ?: 0.0) }) { Text("Salvar VA") }
            }
        }
    }
}

@Composable
fun SimulationScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("account") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var transferDescription by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var transferStart by remember { mutableStateOf("") }
    var transferEnd by remember { mutableStateOf("") }
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var daysInput by remember { mutableStateOf(state.daysToSimulate.toString()) }

    val accounts = state.accounts

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Nova transação", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") })
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Valor") })
            OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Data inicial (AAAA-MM-DD)") })
            OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("Data final opcional") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                Text("Crédito? (desmarque para débito)")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetChip("Conta/Caixinha", target == "account") { target = "account" }
                TargetChip("Cartão", target == "credit_card") { target = "credit_card" }
                TargetChip("VR", target == "vale_refeicao") { target = "vale_refeicao" }
                TargetChip("VA", target == "vale_alimentacao") { target = "vale_alimentacao" }
            }
            if (target == "account") {
                AccountSelector(accounts, onSelect = { selectedAccountId = it })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val parsedStart = parseDate(startDate)
                if (parsedStart != null) {
                    val parsedEnd = parseDate(endDate)
                    val signedAmount = if (isCredit) amount.toDoubleOrNull() ?: 0.0 else -(amount.toDoubleOrNull() ?: 0.0)
                    viewModel.addTransaction(description, signedAmount, parsedStart, parsedEnd, target, selectedAccountId)
                }
            }) { Text("Registrar transação") }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Transferência", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = transferDescription, onValueChange = { transferDescription = it }, label = { Text("Descrição") })
            OutlinedTextField(value = transferAmount, onValueChange = { transferAmount = it }, label = { Text("Valor") })
            OutlinedTextField(value = transferStart, onValueChange = { transferStart = it }, label = { Text("Data inicial (AAAA-MM-DD)") })
            OutlinedTextField(value = transferEnd, onValueChange = { transferEnd = it }, label = { Text("Data final opcional") })
            Text("Conta de origem")
            AccountSelector(accounts, onSelect = { fromAccountId = it })
            Text("Conta de destino")
            AccountSelector(accounts, onSelect = { toAccountId = it })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val parsedStart = parseDate(transferStart)
                val parsedEnd = parseDate(transferEnd)
                if (parsedStart != null && fromAccountId != null && toAccountId != null) {
                    viewModel.addTransfer(
                        transferDescription,
                        transferAmount.toDoubleOrNull() ?: 0.0,
                        parsedStart,
                        parsedEnd,
                        fromAccountId!!,
                        toAccountId!!
                    )
                }
            }) { Text("Agendar transferência") }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Log de eventos futuros", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FutureEventsLog(state)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Simulações lançadas", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SimulationList(state = state, onRemove = { viewModel.deleteFutureEvent(it) })
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Tabela diária", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = daysInput, onValueChange = { daysInput = it }, label = { Text("Dias para simular") })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.refreshProjections(daysInput.toIntOrNull() ?: state.daysToSimulate) }) {
                Text("Atualizar tabela")
            }
            Spacer(modifier = Modifier.height(8.dp))
            DailyTable(state.dailyProjections)
        }
    }
}

@Composable
fun TargetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)) {
        Text(label)
    }
}

@Composable
fun AccountSelector(accounts: List<Account>, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf("Selecionar") }
    Box {
        Button(onClick = { expanded = !expanded }) { Text(selectedName) }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        selectedName = account.name
                        expanded = false
                        onSelect(account.id)
                    }
                )
            }
        }
    }
}

@Composable
fun FutureEventsLog(state: FinanceUiState) {
    val salaryEvents = state.salary?.let { salary ->
        val firstDate = state.startDate
        (0 until state.daysToSimulate).mapNotNull { index ->
            val date = firstDate.plusDays(index.toLong())
            if (date.dayOfMonth == salary.payday) {
                FutureEvent(description = "Salário", amount = salary.amount, date = date, target = "Conta Corrente", source = "Folha")
            } else null
        }
    } ?: emptyList()

    val cardEvents = state.creditCards.firstOrNull()?.let { card ->
        val firstDate = state.startDate
        (0 until state.daysToSimulate).mapNotNull { index ->
            val date = firstDate.plusDays(index.toLong())
            if (date.dayOfMonth == card.dueDay) {
                FutureEvent(description = "Vencimento cartão", amount = card.openAmount, date = date, target = "credit_card", source = card.name)
            } else null
        }
    } ?: emptyList()

    val baseEvents = state.futureEvents + salaryEvents + cardEvents +
            state.transactions.map { FutureEvent(it.id, it.date, it.description, it.amount, it.targetType, "Transação") } +
            state.transfers.map { FutureEvent(it.id, it.date, it.description, it.amount, "account", "Transferência") }

    LazyColumn {
        items(baseEvents.sortedBy { it.date }) { event ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(event.date.toString(), fontWeight = FontWeight.Bold)
                        Text(event.description)
                        Text("Origem/Destino: ${event.source ?: event.target}")
                    }
                    Text("R$ %.2f".format(event.amount), color = if (event.amount >= 0) MaterialTheme.colorScheme.primary else Color.Red)
                }
            }
        }
    }
}

@Composable
fun SimulationList(state: FinanceUiState, onRemove: (Long) -> Unit) {
    val grouped = state.futureEvents.groupBy { it.description to it.amount }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.forEach { (key, items) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(key.first, fontWeight = FontWeight.Bold)
                        Text("Quantidade: ${items.size}")
                        Text("Valor unitário: R$ %.2f".format(key.second))
                    }
                    Row {
                        IconButton(onClick = { items.forEach { onRemove(it.id) } }) { Icon(Icons.Default.Delete, contentDescription = "Remover todos") }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTable(projections: List<DailyProjection>) {
    if (projections.isEmpty()) {
        Text("Nenhuma projeção disponível")
        return
    }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        projections.forEach { projection ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(projection.date.toString(), fontWeight = FontWeight.Bold)
                    projection.accountBalances.forEach { (name, value) ->
                        Text("$name: R$ %.2f".format(value))
                    }
                    projection.valeBalances.forEach { (name, value) ->
                        Text("$name: R$ %.2f".format(value))
                    }
                    Text("Fatura cartão: R$ %.2f".format(projection.creditCardAmount))
                    Text("Total contas: R$ %.2f".format(projection.totalAccounts))
                    Text("Total vales: R$ %.2f".format(projection.totalVales))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    var startInput by remember { mutableStateOf(state.startDate.toString()) }
    var endInput by remember { mutableStateOf(state.startDate.plusDays(state.daysToSimulate.toLong()).toString()) }

    LaunchedEffect(state.startDate, state.daysToSimulate) {
        startInput = state.startDate.toString()
        endInput = state.startDate.plusDays(state.daysToSimulate.toLong()).toString()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Filtros", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = startInput, onValueChange = { startInput = it }, label = { Text("Início (AAAA-MM-DD)") })
            OutlinedTextField(value = endInput, onValueChange = { endInput = it }, label = { Text("Fim (max 365 dias)") })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val startDate = parseDate(startInput) ?: state.startDate
            val endDate = parseDate(endInput) ?: startDate.plusDays(state.daysToSimulate.toLong())
            val cappedEnd = if (startDate.plusDays(365) < endDate) startDate.plusDays(365) else endDate
            viewModel.updateStartDate(startDate)
            viewModel.refreshProjections(((cappedEnd.toEpochDay() - startDate.toEpochDay()).toInt()).coerceAtLeast(1))
        }) { Text("Aplicar filtros") }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Contas exibidas")
        state.accounts.forEach { account ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.selectedAccounts.contains(account.name), onCheckedChange = { viewModel.selectAccount(account.name, it) })
                Text(account.name)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SummaryCardsDashboard(state)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Gráficos", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LineCharts(state)
    }
}

@Composable
fun SummaryCardsDashboard(state: FinanceUiState) {
    if (state.dailyProjections.isEmpty()) return
    val first = state.dailyProjections.first()
    val last = state.dailyProjections.last()
    val variation = last.totalAccounts - first.totalAccounts
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Total contas + cartão")
                Text("R$ %.2f".format(last.totalAccounts + last.creditCardAmount), fontWeight = FontWeight.Bold)
                Text("Variação: R$ %.2f".format(variation))
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Vales finais")
                Text("R$ %.2f".format(last.totalVales), fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class ChartSeries(val label: String, val color: Color, val points: List<Float>)

@Composable
fun LineCharts(state: FinanceUiState) {
    val projections = state.dailyProjections
    if (projections.isEmpty()) {
        Text("Sem dados para gráficos")
        return
    }
    val colors = listOf(Color(0xFF1B998B), Color(0xFF2D3047), Color(0xFFFFA62B), Color(0xFFE4572E), Color(0xFF669BBC))
    val selectedAccounts = state.selectedAccounts
    val accountSeries = projections.first().accountBalances.keys.filter { selectedAccounts.contains(it) }.mapIndexed { index, name ->
        ChartSeries(
            label = name,
            color = colors[index % colors.size],
            points = projections.map { it.accountBalances[name]?.toFloat() ?: 0f }
        )
    }
    val totalSeries = ChartSeries("Total", Color.Blue, projections.map { it.totalAccounts.toFloat() })
    val valesSeries = listOf(
        ChartSeries("Vale refeição", Color(0xFF6A0572), projections.map { it.valeBalances["vale_refeicao"]?.toFloat() ?: 0f }),
        ChartSeries("Vale alimentação", Color(0xFF0EAD69), projections.map { it.valeBalances["vale_alimentacao"]?.toFloat() ?: 0f }),
        ChartSeries("Total vales", Color(0xFF1A659E), projections.map { it.totalVales.toFloat() })
    )

    Text("Saldos diários")
    LineChart(series = accountSeries + totalSeries)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Variação percentual acumulada")
    val base = max(1f, projections.first().totalAccounts.toFloat())
    val percentSeries = ChartSeries("Variação %", Color.Red, projections.map { ((it.totalAccounts.toFloat() - base) / base) * 100f })
    LineChart(series = listOf(percentSeries))
    Spacer(modifier = Modifier.height(12.dp))
    Text("Vales")
    LineChart(series = valesSeries)
    Spacer(modifier = Modifier.height(12.dp))
    val valesBase = max(1f, projections.first().totalVales.toFloat())
    val valesPercent = ChartSeries("Variação vales %", Color.Magenta, projections.map { ((it.totalVales.toFloat() - valesBase) / valesBase) * 100f })
    LineChart(series = listOf(valesPercent))
}

@Composable
fun LineChart(series: List<ChartSeries>, modifier: Modifier = Modifier, height: Int = 180) {
    if (series.isEmpty()) return
    val maxPoints = series.flatMap { it.points }.maxOrNull() ?: 0f
    val minPoints = series.flatMap { it.points }.minOrNull() ?: 0f
    val range = max(1f, maxPoints - minPoints)
    val itemCount = series.first().points.size

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val widthStep = if (itemCount > 1) size.width / (itemCount - 1) else size.width
            series.forEach { chart ->
                val pathPoints = chart.points.mapIndexed { index, value ->
                    val normalized = (value - minPoints) / range
                    Offset(x = widthStep * index, y = size.height - (normalized * size.height))
                }
                for (i in 0 until pathPoints.lastIndex) {
                    drawLine(chart.color, start = pathPoints[i], end = pathPoints[i + 1], strokeWidth = 4f)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            series.forEach { chart ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.height(10.dp).background(chart.color).padding(horizontal = 10.dp))
                    Text(chart.label)
                }
            }
        }
    }
}

fun parseDate(input: String): LocalDate? = runCatching { LocalDate.parse(input) }.getOrNull()

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Tutorial1Theme {
        SummaryCard(title = "Saldo", amount = 123.0)
    }
}
