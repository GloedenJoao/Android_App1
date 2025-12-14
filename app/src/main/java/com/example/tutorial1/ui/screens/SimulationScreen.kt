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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tutorial1.data.Account
import com.example.tutorial1.data.DestinationType
import com.example.tutorial1.data.TransactionEntity
import com.example.tutorial1.data.TransferEntity
import com.example.tutorial1.data.ValeType
import com.example.tutorial1.ui.FinanceViewModel
import com.example.tutorial1.ui.SectionTitle
import com.example.tutorial1.ui.theme.DarkCard
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun SimulationScreen(viewModel: FinanceViewModel, padding: PaddingValues) {
    val accounts by viewModel.accounts.collectAsState()
    val simulation by viewModel.simulation.collectAsState()
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("0.0") }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var end by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf(DestinationType.ACCOUNT) }
    var accountId by remember { mutableStateOf(0) }

    var transferDesc by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("0.0") }
    var transferStart by remember { mutableStateOf(LocalDate.now().toString()) }
    var transferEnd by remember { mutableStateOf("") }
    var originId by remember { mutableStateOf(0) }
    var destId by remember { mutableStateOf(0) }

    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
        item { SectionTitle("Adicionar transação futura") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descrição") })
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Valor (use negativo para débito)") })
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Data inicial (YYYY-MM-DD)") })
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("Data final opcional") })
                    OutlinedTextField(value = destination.name, onValueChange = { value ->
                        destination = DestinationType.values().firstOrNull { it.name.equals(value, true) } ?: DestinationType.ACCOUNT
                    }, label = { Text("Destino: account, credit_card, vale_refeicao, vale_alimentacao") })
                    OutlinedTextField(value = accountId.toString(), onValueChange = { accountId = it.toIntOrNull() ?: 0 }, label = { Text("Id da conta (quando aplicável)") })
                    Button(onClick = {
                        viewModel.saveTransaction(
                            TransactionEntity(
                                description = desc,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                startDate = LocalDate.parse(start),
                                endDate = end.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                                destinationType = destination,
                                accountId = accountId.takeIf { it != 0 }
                            )
                        )
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Salvar transação") }
                }
            }
        }

        item { SectionTitle("Agendar transferência") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(value = transferDesc, onValueChange = { transferDesc = it }, label = { Text("Descrição") })
                    OutlinedTextField(value = transferAmount, onValueChange = { transferAmount = it }, label = { Text("Valor") })
                    OutlinedTextField(value = transferStart, onValueChange = { transferStart = it }, label = { Text("Data inicial") })
                    OutlinedTextField(value = transferEnd, onValueChange = { transferEnd = it }, label = { Text("Data final opcional") })
                    OutlinedTextField(value = originId.toString(), onValueChange = { originId = it.toIntOrNull() ?: 0 }, label = { Text("Conta de origem") })
                    OutlinedTextField(value = destId.toString(), onValueChange = { destId = it.toIntOrNull() ?: 0 }, label = { Text("Conta de destino") })
                    Button(onClick = {
                        viewModel.saveTransfer(
                            TransferEntity(
                                description = transferDesc,
                                amount = transferAmount.toDoubleOrNull() ?: 0.0,
                                startDate = LocalDate.parse(transferStart),
                                endDate = transferEnd.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                                originAccountId = originId,
                                destinationAccountId = destId
                            )
                        )
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Salvar transferência") }
                }
            }
        }

        item { SectionTitle("Eventos futuros") }
        items(simulation.events) { event ->
            Text("${event.date}: ${event.description} (${currency.format(event.amount)}) -> ${event.destination}", modifier = Modifier.padding(vertical = 4.dp))
        }

        item { SectionTitle("Tabela diária") }
        items(simulation.days.take(120)) { day ->
            val totalAccounts = day.accounts.values.sum()
            Text("${day.date}: contas ${currency.format(totalAccounts)} | refeição ${currency.format(day.vales[ValeType.VALE_REFEICAO] ?: 0.0)} | alimentação ${currency.format(day.vales[ValeType.VALE_ALIMENTACAO] ?: 0.0)} | cartão ${currency.format(day.cardBalance)}",
                modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
