package com.example.tutorial1.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.tutorial1.data.Account
import com.example.tutorial1.data.AccountType
import com.example.tutorial1.data.CreditCard
import com.example.tutorial1.data.Salary
import com.example.tutorial1.data.ValeBalance
import com.example.tutorial1.data.ValeType
import com.example.tutorial1.ui.FinanceViewModel
import com.example.tutorial1.ui.LabeledValueCard
import com.example.tutorial1.ui.SectionTitle
import com.example.tutorial1.ui.theme.DarkCard
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: FinanceViewModel, padding: PaddingValues) {
    var selectedTab by remember { mutableStateOf(0) }
    val accounts by viewModel.accounts.collectAsState()
    val card by viewModel.card.collectAsState()
    val salary by viewModel.salary.collectAsState()
    val vales by viewModel.vales.collectAsState()

    Column(modifier = Modifier.padding(padding)) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Painel financeiro") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Atualizar valores") })
        }

        when (selectedTab) {
            0 -> FinancialPanel(accounts, card, salary, vales)
            else -> UpdateValuesTab(viewModel, accounts, card, salary, vales)
        }
    }
}

@Composable
private fun FinancialPanel(
    accounts: List<Account>,
    card: CreditCard?,
    salary: Salary?,
    vales: List<ValeBalance>
) {
    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        item {
            val consolidated = accounts.sumOf { it.balance }
            LabeledValueCard("Saldo consolidado (conta + caixinhas)", currency.format(consolidated), consolidated >= 0)
            val salaryLabel = salary?.let { "${currency.format(it.amount)} no dia ${it.payDay}" } ?: "Não configurado"
            LabeledValueCard("Salário mensal", salaryLabel)
            card?.let {
                LabeledValueCard(
                    title = "Dívida atual do cartão",
                    value = "${currency.format(it.openAmount)} - vencimento dia ${it.dueDay}",
                    positive = it.openAmount >= 0
                )
            }
            val refeicao = vales.firstOrNull { it.type == ValeType.VALE_REFEICAO }?.balance ?: 0.0
            val alimentacao = vales.firstOrNull { it.type == ValeType.VALE_ALIMENTACAO }?.balance ?: 0.0
            LabeledValueCard("Vale Refeição", currency.format(refeicao), refeicao >= 0)
            LabeledValueCard("Vale Alimentação", currency.format(alimentacao), alimentacao >= 0)
        }
    }
}

@Composable
private fun UpdateValuesTab(
    viewModel: FinanceViewModel,
    accounts: List<Account>,
    card: CreditCard?,
    salary: Salary?,
    vales: List<ValeBalance>
) {
    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    var accountName by remember { mutableStateOf("") }
    var accountBalance by remember { mutableStateOf("0.0") }
    var accountType by remember { mutableStateOf(AccountType.CORRENTE) }

    var cardName by remember { mutableStateOf(card?.name ?: "Cartão") }
    var dueDay by remember { mutableStateOf((card?.dueDay ?: 5).toString()) }
    var cardAmount by remember { mutableStateOf((card?.openAmount ?: -0.0).toString()) }

    var salaryAmount by remember { mutableStateOf((salary?.amount ?: 0.0).toString()) }
    var payDay by remember { mutableStateOf((salary?.payDay ?: 5).toString()) }

    var refeicaoBalance by remember { mutableStateOf((vales.firstOrNull { it.type == ValeType.VALE_REFEICAO }?.balance ?: 0.0).toString()) }
    var alimentacaoBalance by remember { mutableStateOf((vales.firstOrNull { it.type == ValeType.VALE_ALIMENTACAO }?.balance ?: 0.0).toString()) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        item { SectionTitle("Conta corrente e caixinhas") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Nome") })
                    OutlinedTextField(value = accountBalance, onValueChange = { accountBalance = it }, label = { Text("Saldo") })
                    OutlinedTextField(
                        value = accountType.name,
                        onValueChange = { accountType = if (it.lowercase().startsWith("cai")) AccountType.CAIXINHA else AccountType.CORRENTE },
                        label = { Text("Tipo (corrente ou caixinha)") }
                    )
                    Button(onClick = {
                        val balance = accountBalance.toDoubleOrNull() ?: 0.0
                        viewModel.saveAccount(Account(name = accountName, balance = balance, type = accountType))
                        accountName = ""
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Salvar conta") }
                }
            }
        }
        items(accounts) { account ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(account.name)
                    Text("Tipo: ${account.type}")
                    Text("Saldo: ${currency.format(account.balance)}")
                    Button(onClick = { viewModel.deleteAccount(account) }, modifier = Modifier.padding(top = 6.dp)) { Text("Excluir") }
                }
            }
        }

        item { SectionTitle("Cartão de crédito") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(value = cardName, onValueChange = { cardName = it }, label = { Text("Nome") })
                    OutlinedTextField(value = dueDay, onValueChange = { dueDay = it }, label = { Text("Dia de vencimento") })
                    OutlinedTextField(value = cardAmount, onValueChange = { cardAmount = it }, label = { Text("Valor atual da fatura (negativo)") })
                    Button(onClick = {
                        viewModel.saveCreditCard(
                            CreditCard(
                                id = card?.id ?: 0,
                                name = cardName,
                                dueDay = dueDay.toIntOrNull() ?: 5,
                                openAmount = cardAmount.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Salvar cartão") }
                }
            }
        }

        item { SectionTitle("Salário") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(value = salaryAmount, onValueChange = { salaryAmount = it }, label = { Text("Valor do salário") })
                    OutlinedTextField(value = payDay, onValueChange = { payDay = it }, label = { Text("Dia do pagamento") })
                    Button(onClick = {
                        viewModel.saveSalary(Salary(amount = salaryAmount.toDoubleOrNull() ?: 0.0, payDay = payDay.toIntOrNull() ?: 5))
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Salvar salário") }
                }
            }
        }

        item { SectionTitle("Vales") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(value = refeicaoBalance, onValueChange = { refeicaoBalance = it }, label = { Text("Vale Refeição") })
                    OutlinedTextField(value = alimentacaoBalance, onValueChange = { alimentacaoBalance = it }, label = { Text("Vale Alimentação") })
                    Button(onClick = {
                        viewModel.saveVale(ValeBalance(ValeType.VALE_REFEICAO, refeicaoBalance.toDoubleOrNull() ?: 0.0))
                        viewModel.saveVale(ValeBalance(ValeType.VALE_ALIMENTACAO, alimentacaoBalance.toDoubleOrNull() ?: 0.0))
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Salvar vales") }
                }
            }
        }
    }
}
