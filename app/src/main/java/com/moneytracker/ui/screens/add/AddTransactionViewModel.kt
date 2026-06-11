package com.moneytracker.ui.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.MoneyTrackerApp
import com.moneytracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val amountDisplay: String = "", // user types "50", displays "50"
    val rawAmount: Long = 0L,       // stored as "50000" (50 * 1000)
    val note: String = "",
    val quantity: Int = 1,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val editId: Long? = null,
    val selectedDateMs: Long = System.currentTimeMillis(),
    val suggestions: List<String> = emptyList(),
    val showSuggestions: Boolean = false
)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MoneyTrackerApp).repository
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init { loadSuggestions() }

    private fun loadSuggestions() {
        viewModelScope.launch {
            repository.getAllTransactions().first().let { list ->
                val names = list.map { it.note.ifEmpty { it.category } }.distinct().sorted()
                _uiState.value = _uiState.value.copy(suggestions = names)
            }
        }
    }

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(id) ?: return@launch
            // For edit: display the amount divided by 1000
            val displayAmt = if (tx.amount % 1000L == 0L) (tx.amount / 1000L).toString() else tx.amount.toString()
            _uiState.value = _uiState.value.copy(
                editId = id,
                amountDisplay = displayAmt,
                rawAmount = tx.amount,
                note = tx.note,
                quantity = tx.quantity,
                selectedDateMs = tx.date
            )
        }
    }

    fun updateAmount(display: String) {
        val clean = display.replace("\\D".toRegex(), "")
        val raw = if (clean.isEmpty()) 0L else (clean.toLongOrNull() ?: 0L) * 1000L
        _uiState.value = _uiState.value.copy(amountDisplay = clean, rawAmount = raw, error = null)
    }

    fun updateNote(note: String) {
        val suggestions = _uiState.value.suggestions.filter {
            it.lowercase().contains(note.lowercase())
        }.take(5)
        _uiState.value = _uiState.value.copy(note = note, showSuggestions = note.isNotEmpty() && suggestions.isNotEmpty())
    }

    fun selectSuggestion(text: String) {
        _uiState.value = _uiState.value.copy(note = text, showSuggestions = false)
    }

    fun hideSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = false)
    }

    fun updateQuantity(quantity: Int) {
        _uiState.value = _uiState.value.copy(quantity = quantity.coerceAtLeast(1))
    }

    fun updateDate(millis: Long) {
        _uiState.value = _uiState.value.copy(selectedDateMs = millis)
    }

    fun saveTransaction() {
        val state = _uiState.value
        if (state.rawAmount <= 0) {
            _uiState.value = state.copy(error = "Vui lòng nhập số tiền")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val name = state.note.ifBlank { "Không có tên" }
            val transaction = TransactionEntity(
                id = state.editId ?: 0L,
                amount = state.rawAmount,
                note = name,
                category = "",
                type = "expense",
                date = state.selectedDateMs,
                quantity = state.quantity
            )
            if (state.editId != null) repository.update(transaction)
            else repository.insert(transaction)
            _uiState.value = state.copy(isSaving = false, saved = true)
        }
    }
}
