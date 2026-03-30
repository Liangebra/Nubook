package com.nubook.ui.search

import android.app.Application
import androidx.lifecycle.*
import com.nubook.NuBookApplication
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * 搜索 ViewModel
 * 基于 StateFlow 实现防抖实时搜索
 */
class SearchViewModel(
    application: Application,
    private val ledgerId: String? // 为空表示全局搜索
) : AndroidViewModel(application) {

    private val database = (application as NuBookApplication).database
    private val repository = TransactionRepository(database.transactionDao())

    private val _searchQuery = MutableStateFlow("")
    
    // 搜索结果
    val searchResults: LiveData<List<TransactionEntity>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .asLiveData()
        .switchMap { query ->
            if (query.isNullOrBlank()) {
                MutableLiveData(emptyList())
            } else {
                repository.searchTransactions(ledgerId, query)
            }
        }

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    class Factory(
        private val application: Application,
        private val ledgerId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(application, ledgerId) as T
        }
    }
}
