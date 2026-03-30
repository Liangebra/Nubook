package com.nubook.ui.input

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.nubook.R
import com.nubook.data.local.entity.TransactionType
import com.nubook.databinding.ActivityInputBinding
import com.nubook.ui.base.BaseActivity
import com.nubook.ui.theme.ColorEngine
import java.text.SimpleDateFormat
import java.util.*

/**
 * 记账输入 Activity (V2: 支持括号、标签、日期选择)
 * 底部计算器键盘，顶部显示金额表达式和实时计算结果
 */
class InputActivity : BaseActivity() {

    private lateinit var binding: ActivityInputBinding
    private val viewModel: InputViewModel by viewModels {
        val ledgerId = intent.getStringExtra("ledger_id") ?: ""
        InputViewModel.Factory(application, ledgerId)
    }

    private var currentType = TransactionType.EXPENSE
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // 返回按钮 (V2: 布局中 ID 为 btn_close)
        binding.btnClose.setOnClickListener { finish() }

        // 收支切换 (V2: 布局中 ID 为 btn_income, btn_expense)
        binding.btnIncome.setOnClickListener { switchType(TransactionType.INCOME) }
        binding.btnExpense.setOnClickListener { switchType(TransactionType.EXPENSE) }
        switchType(TransactionType.EXPENSE) // 默认支出

        // 日期选择 (V2: 布局中 ID 为 btn_date)
        binding.btnDate.setOnClickListener { showDatePicker() }

        // 键盘按钮绑定
        setupKeyboard()

        // 标签联想
        binding.etTag.setOnClickListener {
            // 可展开历史标签列表
        }

        // 完成/保存 (V2: 布局中 ID 为 btn_done)
        binding.btnDone.setOnClickListener {
            submitTransaction()
        }
    }

    private fun setupKeyboard() {
        val buttons = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnDot to ".",
            binding.btnPlus to "+", binding.btnMinus to "-",
            binding.btnMultiply to "*", binding.btnDivide to "/",
            binding.btnLeftBracket to "(", binding.btnRightBracket to ")"
        )

        buttons.forEach { (btn, value) ->
            btn.setOnClickListener { viewModel.appendChar(value) }
        }

        binding.btnClear.setOnClickListener { viewModel.clear() }
        binding.btnBackspace.setOnClickListener { viewModel.backspace() }
    }

    private fun observeViewModel() {
        // 观察算式
        viewModel.expression.observe(this) { expr ->
            binding.tvExpression.text = if (expr.isNullOrEmpty()) "0" else expr
        }

        // 观察结果
        viewModel.result.observe(this) { res ->
            binding.tvResult.text = res
        }

        // 观察日期
        viewModel.selectedDate.observe(this) { timestamp ->
            binding.btnDate.text = if (timestamp == null || timestamp == 0L) {
                "今天" 
            } else {
                dateFormat.format(Date(timestamp))
            }
        }

        // 观察提交成功
        viewModel.submitSuccess.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, "记账成功", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // 观察错误
        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun switchType(type: TransactionType) {
        currentType = type
        if (type == TransactionType.INCOME) {
            binding.btnIncome.setTextColor(ColorEngine.getPrimaryColor()) // 用主色调或特定收入色
            binding.indicatorIncome.visibility = android.view.View.VISIBLE
            binding.btnExpense.setTextColor(ColorEngine.getColor(this, R.color.text_hint))
            binding.indicatorExpense.visibility = android.view.View.INVISIBLE
            binding.btnDone.setBackgroundColor(ColorEngine.getColor(this, R.color.income_color))
        } else {
            binding.btnIncome.setTextColor(ColorEngine.getColor(this, R.color.text_hint))
            binding.indicatorIncome.visibility = android.view.View.INVISIBLE
            binding.btnExpense.setTextColor(ColorEngine.getPrimaryColor())
            binding.indicatorExpense.visibility = android.view.View.VISIBLE
            binding.btnDone.setBackgroundColor(ColorEngine.getColor(this, R.color.expense_color))
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val current = viewModel.selectedDate.value ?: System.currentTimeMillis()
        calendar.timeInMillis = current

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                viewModel.setDate(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitTransaction() {
        val tagName = binding.etTag.text.toString()
        val note = binding.etNote.text.toString()
        viewModel.submit(currentType, tagName, note)
    }
}
