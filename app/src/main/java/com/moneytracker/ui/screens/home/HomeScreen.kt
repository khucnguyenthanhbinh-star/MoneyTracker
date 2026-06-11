package com.moneytracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    onSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MoneyTracker", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Cài đặt") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddTransaction) { Icon(Icons.Default.Add, "Thêm") } }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
                // Search bar
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Tìm kiếm giao dịch...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, "Xoá")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Tabs
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("day" to "Ngày", "month" to "Tháng", "year" to "Năm").forEach { (k, l) ->
                            FilterChip(selected = uiState.tab == k, onClick = { viewModel.setTab(k) }, label = { Text(l) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Date text + nav buttons
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.previousPeriod() }) {
                            @Suppress("DEPRECATION")
                            Icon(Icons.Default.NavigateBefore, "Trước")
                        }
                        Text(uiState.currentDateText, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        IconButton(onClick = { viewModel.nextPeriod() }) {
                            @Suppress("DEPRECATION")
                            Icon(Icons.Default.NavigateNext, "Sau")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Bar chart
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Spacer(Modifier.height(12.dp))

                            val maxAmt = uiState.bars.maxOfOrNull { it.amount }?.toFloat() ?: 1f

                            if (uiState.bars.isEmpty()) {
                                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                    Text("Chưa có dữ liệu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Row(Modifier.fillMaxWidth()) {
                                    // Bars
                                    Row(
                                        Modifier.weight(1f).height(140.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        uiState.bars.forEach { bar ->
                                            val barH = if (maxAmt > 0) ((bar.amount.toFloat() / maxAmt) * 100f).coerceAtLeast(4f) else 4f
                                            val color = if (bar.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            Column(
                                                Modifier.weight(1f).clickable { viewModel.selectBar(bar) }.padding(top = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (bar.amount > 0) {
                                                    Text(formatShort(bar.amount), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Box(Modifier.fillMaxWidth().height(barH.dp).background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                                Spacer(Modifier.height(2.dp))
                                                Text(bar.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), textAlign = TextAlign.Center, maxLines = 1,
                                                    fontWeight = if (bar.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (bar.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Total for selected period
                item {
                    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
                        Text("Tổng: ${formatter.format(uiState.totalExpense)}đ", Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Giao dịch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        val sel = uiState.selectedBarStart
                        if (sel > 0L) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
                            if (uiState.tab == "day") {
                                Text(sdf.format(Date(sel)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("${sdf.format(Date(sel))} - ${sdf.format(Date(uiState.selectedBarEnd))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("Chưa có giao dịch", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                } else {
                    items(uiState.transactions) { tx -> TransactionItem(tx, onClick = { onEditTransaction(tx.id) }, onDelete = { viewModel.deleteTransaction(tx) }) }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: TransactionEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tx.note, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(tx.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tx.quantity > 1) { Spacer(Modifier.width(8.dp)); Text("x${tx.quantity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Text("-${formatter.format(tx.amount * tx.quantity)}đ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa giao dịch") },
            text = { Text("Xóa \"${tx.note}\"?") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Xóa", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") } }
        )
    }
}

private fun formatDate(ts: Long): String {
    val cal = Calendar.getInstance()
    cal.time = Date(ts)
    val today = Calendar.getInstance()
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> "Hôm qua"
        else -> SimpleDateFormat("dd/MM", Locale("vi", "VN")).format(Date(ts))
    }
}

private fun formatShort(amount: Long): String {
    return when {
        amount >= 1_000_000 -> "${amount / 1_000_000}tr"
        amount >= 1_000 -> "${amount / 1_000}k"
        else -> "${amount}"
    }
}
