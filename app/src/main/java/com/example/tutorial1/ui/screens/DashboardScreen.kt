package com.example.tutorial1.ui.screens

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tutorial1.data.AccountType
import com.example.tutorial1.data.ValeType
import com.example.tutorial1.ui.FinanceViewModel
import com.example.tutorial1.ui.SectionTitle
import com.example.tutorial1.ui.theme.DarkCard
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: FinanceViewModel, padding: PaddingValues) {
    val simulation by viewModel.simulation.collectAsState()
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(60)) }
    var selectedIds by remember { mutableStateOf("") }

    val context = LocalContext.current
    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    LaunchedEffect(startDate, endDate) {
        val days = kotlin.math.min(365, ChronoUnit.DAYS.between(startDate, endDate).toInt().coerceAtLeast(1))
        viewModel.simulationDays.value = days
    }

    val daysFiltered = simulation.days.filter { it.date >= startDate && it.date <= endDate }
    val selectedAccountIds = selectedIds.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    val consolidatedStart = daysFiltered.firstOrNull()
    val consolidatedEnd = daysFiltered.lastOrNull()

    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
        SectionTitle("Filtros")
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(value = startDate.toString(), onValueChange = { startDate = LocalDate.parse(it) }, label = { Text("Data inicial") })
                OutlinedTextField(value = endDate.toString(), onValueChange = { endDate = LocalDate.parse(it) }, label = { Text("Data final (até 365 dias)") })
                OutlinedTextField(value = selectedIds, onValueChange = { selectedIds = it }, label = { Text("Ids de contas (se vazio, todas)") })
                Button(onClick = {
                    if (endDate.isBefore(startDate)) endDate = startDate.plusDays(1)
                    if (endDate.isBefore(LocalDate.now())) endDate = LocalDate.now().plusDays(1)
                }, modifier = Modifier.padding(top = 8.dp)) { Text("Aplicar ajustes") }
            }
        }

        SectionTitle("Resumo")
        consolidatedEnd?.let { end ->
            val start = consolidatedStart ?: end
            val totalStart = start.accounts.filterKeys { selectedAccountIds.isEmpty() || selectedAccountIds.contains(it) }.values.sum() + start.cardBalance + start.vales.values.sum()
            val totalEnd = end.accounts.filterKeys { selectedAccountIds.isEmpty() || selectedAccountIds.contains(it) }.values.sum() + end.cardBalance + end.vales.values.sum()
            val diff = totalEnd - totalStart
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total consolidado: ${currency.format(totalEnd)} (variação ${currency.format(diff)})")
                    end.accounts.filter { selectedAccountIds.isEmpty() || selectedAccountIds.contains(it.key) }.forEach { (id, balance) ->
                        Text("Conta $id: ${currency.format(balance)}")
                    }
                    Text("Vales: ${currency.format(end.vales.values.sum())}")
                }
            }
        }

        SectionTitle("Gráfico de saldos")
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            factory = {
                LineChart(context).apply {
                    description = Description().apply { text = "" }
                }
            },
            update = { chart ->
                val consolidatedEntries = daysFiltered.mapIndexed { index, day ->
                    val total = day.accounts.filterKeys { selectedAccountIds.isEmpty() || selectedAccountIds.contains(it) }.values.sum() + day.cardBalance + day.vales.values.sum()
                    Entry(index.toFloat(), total.toFloat())
                }
                val valeEntries = daysFiltered.mapIndexed { index, day ->
                    Entry(index.toFloat(), day.vales.values.sum().toFloat())
                }
                val dataSets = listOf(
                    LineDataSet(consolidatedEntries, "Consolidado").apply { color = Color.GREEN },
                    LineDataSet(valeEntries, "Vales").apply { color = Color.CYAN }
                )
                chart.data = LineData(dataSets)
                chart.invalidate()
            }
        )
    }
}
