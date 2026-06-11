package com.moneytracker.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.MoneyTrackerApp
import com.moneytracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

data class BarData(
    val label: String = "",
    val amount: Long = 0L,
    val isCurrent: Boolean = false,
    val startTime: Long = 0L,
    val endTime: Long = 0L
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentDateText: String = "",
    val tab: String = "day",
    val bars: List<BarData> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val totalExpense: Long = 0L,
    val searchQuery: String = "",
    val periodOffset: Int = 0,
    val selectedBarStart: Long = 0L,
    val selectedBarEnd: Long = 0L
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MoneyTrackerApp).repository
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { observeTransactions() }

    private fun observeTransactions() {
        viewModelScope.launch {
            repository.getAllTransactions().collect { all ->
                _uiState.value = _uiState.value.copy(isLoading = false, allTransactions = all)
                val selectedStart = _uiState.value.selectedBarStart
                val selectedEnd = _uiState.value.selectedBarEnd
                if (selectedStart == 0L && selectedEnd == 0L) {
                    computeDisplay(all)
                } else {
                    computeBars(all)
                    val filtered = all.filter { it.date >= selectedStart && it.date <= selectedEnd }
                    updateFiltered(filtered)
                }
            }
        }
    }

    fun setTab(tab: String) {
        _uiState.value = _uiState.value.copy(tab = tab, periodOffset = 0, selectedBarStart = 0L, selectedBarEnd = 0L)
        refreshFromCache()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        refreshFromCache()
    }

    fun previousPeriod() {
        _uiState.value = _uiState.value.copy(periodOffset = _uiState.value.periodOffset - 1, selectedBarStart = 0L, selectedBarEnd = 0L)
        refreshFromCache()
    }

    fun nextPeriod() {
        _uiState.value = _uiState.value.copy(periodOffset = _uiState.value.periodOffset + 1, selectedBarStart = 0L, selectedBarEnd = 0L)
        refreshFromCache()
    }

    private fun refreshFromCache() {
        val all = _uiState.value.allTransactions
        if (all.isNotEmpty()) {
            val selectedStart = _uiState.value.selectedBarStart
            if (selectedStart == 0L) {
                computeDisplay(all)
            } else {
                computeBars(all)
            }
        }
    }

    fun selectBar(bar: BarData) {
        _uiState.value = _uiState.value.copy(selectedBarStart = bar.startTime, selectedBarEnd = bar.endTime)
        val all = _uiState.value.allTransactions
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filtered = all.filter { it.date >= bar.startTime && it.date <= bar.endTime }
        val result = if (query.isEmpty()) filtered else filtered.filter { it.note.lowercase().contains(query) }
        _uiState.value = _uiState.value.copy(
            transactions = result,
            totalExpense = result.sumOf { it.amount * it.quantity }
        )
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch { repository.delete(tx) }
    }

    private fun computeDisplay(all: List<TransactionEntity>) {
        computeBars(all)
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) all
            else all.filter { it.note.lowercase().contains(query) }
        updateFiltered(filtered)
    }

    private fun updateFiltered(list: List<TransactionEntity>) {
        _uiState.value = _uiState.value.copy(
            transactions = list,
            totalExpense = list.sumOf { it.amount * it.quantity }
        )
    }

    private fun computeBars(all: List<TransactionEntity>) {
        val state = _uiState.value
        val offset = state.periodOffset
        val day = 86400000L
        val sdfDate = SimpleDateFormat("dd/MM", Locale("vi", "VN"))

        val bars: List<BarData>
        val dateText: String

        when (state.tab) {
            "day" -> {
                val cal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, offset) }
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val monStart = cal.timeInMillis
                val todayCal = Calendar.getInstance()
                val todayDow = todayCal.get(Calendar.DAY_OF_WEEK)
                val names = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                bars = (0..6).map { i ->
                    val s = monStart + i * day; val e = s + day - 1
                    val amt = all.filter { it.date >= s && it.date <= e }.sumOf { it.amount * it.quantity }
                    val currentIdx = if (todayDow == Calendar.SUNDAY) 6 else todayDow - 2
                    BarData(names[i], amt, offset == 0 && i == currentIdx, s, e)
                }
                val endCal = Calendar.getInstance().apply { timeInMillis = monStart + 6 * day }
                dateText = "${sdfDate.format(cal.time)} - ${sdfDate.format(endCal.time)}"
            }
            "week" -> {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
                val month = cal.get(Calendar.MONTH); val year = cal.get(Calendar.YEAR)
                val totalDays = Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
                val now = Calendar.getInstance()
                val weekBars = mutableListOf<BarData>()
                var dayStart = 1
                while (dayStart <= totalDays) {
                    val dayEnd = minOf(dayStart + 6, totalDays)
                    val sCal = Calendar.getInstance().apply { set(year, month, dayStart, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                    val eCal = Calendar.getInstance().apply { set(year, month, dayEnd, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                    val s = sCal.timeInMillis; val e = eCal.timeInMillis
                    val amt = all.filter { it.date >= s && it.date <= e }.sumOf { it.amount * it.quantity }
                    val isCurrent = offset == 0 && now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month && now.get(Calendar.DAY_OF_MONTH) in dayStart..dayEnd
                    weekBars.add(BarData("${sdfDate.format(Date(s))} - ${sdfDate.format(Date(e))}", amt, isCurrent, s, e))
                    dayStart += 7
                }
                bars = weekBars
                dateText = "Tháng ${SimpleDateFormat("MM/yyyy", Locale("vi", "VN")).format(cal.time)}"
            }
            "month" -> {
                val cal = Calendar.getInstance().apply { add(Calendar.YEAR, offset) }
                val year = cal.get(Calendar.YEAR)
                val curMonth = Calendar.getInstance().get(Calendar.MONTH)
                bars = (0..11).map { m ->
                    val mc = Calendar.getInstance().apply { set(year, m, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                    val ms = mc.timeInMillis; mc.add(Calendar.MONTH, 1); mc.add(Calendar.DAY_OF_MONTH, -1)
                    val me = mc.timeInMillis + day - 1
                    val amt = all.filter { it.date >= ms && it.date <= me }.sumOf { it.amount * it.quantity }
                    BarData("T${m + 1}", amt, offset == 0 && m == curMonth, ms, me)
                }
                dateText = "Năm $year"
            }
            "year" -> {
                val cal = Calendar.getInstance().apply { add(Calendar.YEAR, offset) }
                val year = cal.get(Calendar.YEAR)
                val curYear = Calendar.getInstance().get(Calendar.YEAR)
                bars = (-2..2).map { y ->
                    val y2 = year + y
                    val yc = Calendar.getInstance().apply { set(y2, 0, 1, 0, 0, 0) }; val ys = yc.timeInMillis
                    yc.set(y2, 11, 31, 23, 59, 59); val ye = yc.timeInMillis
                    val amt = all.filter { it.date >= ys && it.date <= ye }.sumOf { it.amount * it.quantity }
                    BarData("$y2", amt, offset == 0 && y2 == curYear, ys, ye)
                }
                dateText = "${year - 2} - ${year + 2}"
            }
            else -> { bars = emptyList(); dateText = "" }
        }
        _uiState.value = _uiState.value.copy(bars = bars, currentDateText = dateText)
    }
}
