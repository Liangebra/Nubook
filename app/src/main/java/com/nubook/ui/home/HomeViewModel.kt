package com.nubook.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.nubook.NuBookApplication
import com.nubook.data.local.entity.LedgerEntity
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.repository.LedgerRepository
import com.nubook.data.repository.TransactionRepository
import kotlinx.coroutines.launch

/**
 * 主页 ViewModel
 * 管理账本列表及其最新流水、搜索和多选统计状态
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as NuBookApplication).database
    private val ledgerRepo = LedgerRepository(database.ledgerDao(), database.transactionDao())
    private val transactionRepo = TransactionRepository(database.transactionDao())

    // 搜索关键词
    private val _searchKeyword = MutableLiveData("")
    val searchKeyword: LiveData<String> = _searchKeyword

    // 账本列表数据 (V2：合并结余与最新交易)
    val ledgers: LiveData<List<LedgerListItem>> = _searchKeyword.switchMap { keyword ->
        ledgerRepo.getAllLedgersWithBalance().switchMap { ledgersWithBalance ->
            liveData {
                val list = ledgersWithBalance.map { lwb ->
                    val latest = transactionRepo.getLatestTransactionSync(lwb.ledger.id)
                    LedgerListItem(lwb.ledger, lwb.balance, latest)
                }
                // 简单的本地过滤
                val filtered = if (keyword.isNullOrBlank()) list else {
                    list.filter { it.ledger.name.contains(keyword, ignoreCase = true) }
                }
                emit(filtered)
            }
        }
    }

    // 搜索结果 (V2支持全文搜索)
    val searchResults: LiveData<List<TransactionEntity>> = _searchKeyword.switchMap { keyword ->
        if (keyword.isNullOrBlank()) {
            MutableLiveData(emptyList())
        } else {
            transactionRepo.searchTransactions(null, keyword)
        }
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 多选统计结果
    private val _aggregateStats = MutableLiveData<AggregateStats?>()
    val aggregateStats: LiveData<AggregateStats?> = _aggregateStats

    fun refresh() {
        _searchKeyword.value = _searchKeyword.value
    }

    fun updateSearchKeyword(query: String) {
        _searchKeyword.value = query
    }

    fun createLedger(name: String) {
        val index = (System.currentTimeMillis() % 8).toInt()
        viewModelScope.launch {
            ledgerRepo.createLedger(name, index)
            refresh()
        }
    }

    fun updateLedgerName(ledgerId: String, newName: String) {
        viewModelScope.launch {
            ledgerRepo.updateName(ledgerId, newName)
            refresh()
        }
    }

    fun deleteLedger(ledgerId: String) {
        viewModelScope.launch {
            ledgerRepo.deleteLedgerById(ledgerId)
            refresh()
        }
    }

    /**
     * 计算选定账本的聚合统计
     */
    fun calculateAggregation(ledgerIds: Set<String>) {
        if (ledgerIds.isEmpty()) {
            _aggregateStats.value = null
            return
        }
        viewModelScope.launch {
            val income = transactionRepo.getTotalIncomeSync(ledgerIds.toList())
            val expense = transactionRepo.getTotalExpenseSync(ledgerIds.toList())
            _aggregateStats.value = AggregateStats(income, expense, income - expense)
        }
    }
}

data class LedgerListItem(
    val ledger: LedgerEntity,
    val balance: Double,
    val latestTransaction: TransactionEntity?
)

data class AggregateStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)
