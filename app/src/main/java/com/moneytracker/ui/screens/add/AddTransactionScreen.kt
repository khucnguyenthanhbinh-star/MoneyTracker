package com.moneytracker.ui.screens.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionId: Long? = null,
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN")) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hasSaved = remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) viewModel.loadTransaction(transactionId)
    }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved && !hasSaved.value) {
            hasSaved.value = true
            snackbarHostState.showSnackbar(if (uiState.editId != null) "Đã cập nhật" else "Đã thêm")
            kotlinx.coroutines.delay(500)
            onBack()
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = uiState.selectedDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dpState.selectedDateMillis?.let { viewModel.updateDate(it) }; showDatePicker = false }) { Text("Chọn") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = dpState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.editId != null) "Sửa giao dịch" else "Thêm giao dịch") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.amountDisplay,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Số tiền (VD: 50 = 50.000đ)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.error != null,
                supportingText = uiState.error?.let { { Text(it) } }
            )
            if (uiState.rawAmount > 0) {
                val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(uiState.rawAmount)
                Text("= $formatted" + "đ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = { viewModel.updateNote(it) },
                    label = { Text("Tên / Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (uiState.showSuggestions) {
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            uiState.suggestions.take(5).forEach { text ->
                                Text(text, modifier = Modifier.fillMaxWidth().clickable { viewModel.selectSuggestion(text) }.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(dateFormat.format(Date(uiState.selectedDateMs)))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Số lượng:", Modifier.weight(1f))
                OutlinedButton(onClick = { viewModel.updateQuantity(uiState.quantity - 1) }) { Text("-") }
                OutlinedTextField(
                    value = uiState.quantity.toString(),
                    onValueChange = { viewModel.updateQuantity(it.toIntOrNull() ?: 1) },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge
                )
                OutlinedButton(onClick = { viewModel.updateQuantity(uiState.quantity + 1) }) { Text("+") }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (uiState.editId != null) "Lưu thay đổi" else "Lưu")
                }
            }
        }
    }
}
