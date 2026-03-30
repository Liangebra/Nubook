package com.nubook.ui.statistics

import android.app.Application
import androidx.lifecycle.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.nubook.NuBookApplication
import com.nubook.data.local.entity.TransactionType
import com.nubook.data.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 统计 ViewModel (V2: 支持多账本聚合)
 * 负责加载由多个账本 ID 聚合而成的统计数据
 */
class StatisticsViewModel(
    application: Application,
    private val initialLedgerIds: List<String>
) : AndroidViewModel(application) {

    private val transactionRepo = TransactionRepository(
        NuBookApplication.instance.database.transactionDao()
    )

    // 当前显示的账本列表 (V2支持多个)
    private val _ledgerIds = MutableLiveData(initialLedgerIds)

    // 当前扇形图类型 (默认支出)
    private val _currentChartType = MutableLiveData(TransactionType.EXPENSE)
    val currentChartType: LiveData<TransactionType> = _currentChartType

    private val _totalIncome = MutableLiveData(0.0)
    val totalIncome: LiveData<Double> = _totalIncome

    private val _totalExpense = MutableLiveData(0.0)
    val totalExpense: LiveData<Double> = _totalExpense

    private val _pieData = MutableLiveData<List<PieEntry>>()
    val pieData: LiveData<List<PieEntry>> = _pieData

    // 折线图数据: (收入条目列表, 支出条目列表, X轴标签列表)
    private val _lineData = MutableLiveData<Triple<List<Entry>, List<Entry>, List<String>>>()
    val lineData: LiveData<Triple<List<Entry>, List<Entry>, List<String>>> = _lineData

    init {
        loadStatistics()
    }

    /**
     * 设置显示的账本并刷新
     */
    fun setLedgers(ids: List<String>) {
        _ledgerIds.value = ids
        loadStatistics()
    }

    /**
     * 切换扇形图展示类型
     */
    fun toggleChartType(type: TransactionType) {
        if (_currentChartType.value != type) {
            _currentChartType.value = type
            loadStatistics()
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val ids = _ledgerIds.value ?: initialLedgerIds
            val type = _currentChartType.value ?: TransactionType.EXPENSE

            // 时间范围：最近12个月
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.YEAR, -1)
            val startTime = calendar.timeInMillis

            // 总收入/支出
            _totalIncome.value = transactionRepo.getTotalIncome(ids, startTime, endTime)
            _totalExpense.value = transactionRepo.getTotalExpense(ids, startTime, endTime)

            // 扇形图：按标签分组 (V2)
            val tagGroups = transactionRepo.getGroupedByTag(
                ids, type, startTime, endTime
            )
            _pieData.value = tagGroups.map { PieEntry(it.total.toFloat(), it.tagName.ifEmpty { "其他" }) }

            // 折线图
            val monthlyData = transactionRepo.getMonthlyGrouped(ids, startTime, endTime)

            val incomeEntries = mutableListOf<Entry>()
            val expenseEntries = mutableListOf<Entry>()
            val labels = mutableListOf<String>()
            val periodSet = monthlyData.map { it.period }.distinct().sorted()

            periodSet.forEachIndexed { index, period ->
                labels.add(period)
                val inc = monthlyData.firstOrNull { it.period == period && it.type == TransactionType.INCOME }?.total ?: 0.0
                val exp = monthlyData.firstOrNull { it.period == period && it.type == TransactionType.EXPENSE }?.total ?: 0.0

                incomeEntries.add(Entry(index.toFloat(), inc.toFloat()))
                expenseEntries.add(Entry(index.toFloat(), exp.toFloat()))
            }
            _lineData.value = Triple(incomeEntries, expenseEntries, labels)
        }
    }

    class Factory(
        private val application: Application,
        private val ledgerIds: List<String>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(application, ledgerIds) as T
        }
    }
}
