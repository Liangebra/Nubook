package com.nubook.ui.ledger

import android.app.Application
import androidx.lifecycle.*
import com.nubook.NuBookApplication
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.repository.TransactionRepository
import kotlinx.coroutines.launch

/**
 * 账本详情 ViewModel
 */
class LedgerDetailViewModel(
    application: Application,
    private val ledgerId: String
) : AndroidViewModel(application) {

    private val database = (application as NuBookApplication).database
    private val transactionRepo = TransactionRepository(database.transactionDao())

    val transactions: LiveData<List<TransactionEntity>> =
        transactionRepo.getTransactionsByLedger(ledgerId)

    val balance: LiveData<Double> = transactionRepo.getBalance(ledgerId)

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepo.delete(transaction)
        }
    }

    class Factory(
        private val application: Application,
        private val ledgerId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LedgerDetailViewModel(application, ledgerId) as T
        }
    }
}
