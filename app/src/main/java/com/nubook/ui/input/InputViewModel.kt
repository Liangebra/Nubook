package com.nubook.ui.input

import android.app.Application
import androidx.lifecycle.*
import com.nubook.NuBookApplication
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.local.entity.TransactionType
import com.nubook.data.repository.TransactionRepository
import com.nubook.domain.usecase.ParseExpressionUseCase
import kotlinx.coroutines.launch

/**
 * 记账输入 ViewModel
 * 管理表达式状态、实时计算结果、提交逻辑
 */
class InputViewModel(
    application: Application,
    private val ledgerId: String
) : AndroidViewModel(application) {

    private val database = (application as NuBookApplication).database
    private val transactionRepo = TransactionRepository(database.transactionDao())
    private val parseExpressionUseCase = ParseExpressionUseCase()

    // 当前输入的数学表达式
    private val _expression = MutableLiveData("")
    val expression: LiveData<String> = _expression

    // 实时计算结果 (格式化字符串)
    private val _result = MutableLiveData("= 0.00")
    val result: LiveData<String> = _result

    // 提交成功通知 (V2: 使用 SingleLiveEvent 或类似机制，这里简化为 Boolean)
    private val _submitSuccess = MutableLiveData<Boolean>()
    val submitSuccess: LiveData<Boolean> = _submitSuccess

    // 错误信息通知
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 当前选定日期 (默认今天)
    private val _selectedDate = MutableLiveData(System.currentTimeMillis())
    val selectedDate: LiveData<Long> = _selectedDate

    // 历史标签列表
    val tagHistory: LiveData<List<String>> = liveData {
        emit(transactionRepo.getTagHistory())
    }

    /**
     * 追加字符到表达式
     */
    fun appendChar(char: String) {
        val current = _expression.value ?: ""
        _expression.value = current + char
        calculateResult()
    }

    /**
     * 设置日期
     */
    fun setDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    /**
     * 退格删除最后一个字符
     */
    fun backspace() {
        val current = _expression.value ?: ""
        if (current.isNotEmpty()) {
            _expression.value = current.dropLast(1)
            calculateResult()
        }
    }

    /**
     * 清空表达式
     */
    fun clear() {
        _expression.value = ""
        _result.value = "= 0.00"
    }

    /**
     * 实时计算当前表达式的结果
     */
    private fun calculateResult() {
        val expr = _expression.value ?: ""
        if (expr.isEmpty()) {
            _result.value = "= 0.00"
            return
        }

        val parseResult = parseExpressionUseCase.execute(expr)
        parseResult.onSuccess { value ->
            _result.value = "= ${parseExpressionUseCase.formatAmount(value)}"
        }.onFailure {
            // 表达式不完整或有误时保持状态，暂不报错
        }
    }

    /**
     * 提交记账 (V2: 包含标签和日期)
     */
    fun submit(type: TransactionType, tagName: String, note: String = "") {
        val expr = _expression.value ?: ""
        if (expr.isEmpty()) {
            _errorMessage.value = "请输入金额"
            return
        }

        val parseResult = parseExpressionUseCase.execute(expr)
        parseResult.onSuccess { amount ->
            if (amount <= 0) {
                _errorMessage.value = "金额必须大于零"
                return
            }

            viewModelScope.launch {
                val transaction = TransactionEntity(
                    ledgerId = ledgerId,
                    amount = amount,
                    type = type,
                    tagName = if (tagName.trim().isEmpty()) "其他" else tagName,
                    note = note,
                    timestamp = _selectedDate.value ?: System.currentTimeMillis()
                )
                transactionRepo.insert(transaction)
                _submitSuccess.postValue(true)
            }
        }.onFailure { error ->
            _errorMessage.value = "计算错误: ${error.message}"
        }
    }

    class Factory(
        private val application: Application,
        private val ledgerId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InputViewModel::class.java)) {
                return InputViewModel(application, ledgerId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
