package com.nubook.ui.ledger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nubook.databinding.ActivityLedgerDetailBinding
import com.nubook.ui.input.InputActivity
import com.nubook.ui.statistics.StatisticsActivity
import com.nubook.ui.theme.ColorEngine
import com.nubook.ui.base.BaseActivity
import com.nubook.data.local.entity.TransactionType

/**
 * 账本详情 Activity (V2: 支持多选与聚合统计)
 */
class LedgerDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityLedgerDetailBinding
    private lateinit var viewModel: LedgerDetailViewModel
    private lateinit var adapter: TransactionAdapter
    private var ledgerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLedgerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ledgerId = intent.getStringExtra("ledger_id") ?: return finish()
        val ledgerName = intent.getStringExtra("ledger_name") ?: "账本"

        viewModel = ViewModelProvider(
            this,
            LedgerDetailViewModel.Factory(application, ledgerId)
        )[LedgerDetailViewModel::class.java]

        binding.tvTitle.text = ledgerName

        setupRecyclerView()
        setupButtons()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                // V2: 查看/编辑 (可选实现)
            },
            onItemLongClick = { transaction ->
                showTransactionMenu(transaction)
            },
            onSelectionChanged = { selectedIds ->
                updateSelectionUI(selectedIds)
            }
        )
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@LedgerDetailActivity)
            adapter = this@LedgerDetailActivity.adapter
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnStatistics.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java).apply {
                putExtra("ledger_id", ledgerId)
            }
            startActivity(intent)
        }

        binding.btnSearch.setOnClickListener {
            val intent = Intent(this, com.nubook.ui.search.SearchActivity::class.java).apply {
                putExtra("ledger_id", ledgerId)
            }
            startActivity(intent)
        }

        binding.btnAddTransaction.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java).apply {
                putExtra("ledger_id", ledgerId)
            }
            startActivity(intent)
        }

        binding.btnAddTransaction.backgroundTintList =
            android.content.res.ColorStateList.valueOf(ColorEngine.getPrimaryColor())

        binding.btnExitSelection.setOnClickListener { exitSelectionMode() }
        
        binding.btnSelectionDetails.setOnClickListener {
            // 这里可以弹出一个详细列表或者直接跳转到特定统计
            Toast.makeText(this, "聚合统计已应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewModel.transactions.observe(this) { transactions ->
            adapter.submitList(transactions)
            binding.layoutEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.balance.observe(this) { balance ->
            val formatted = String.format(java.util.Locale.US, "%.2f", balance)
            binding.tvBalance.text = "¥ $formatted"
            binding.tvBalance.setTextColor(if (balance >= 0) ColorEngine.getIncomeColor() else ColorEngine.getExpenseColor())
        }
    }

    private fun showTransactionMenu(transaction: com.nubook.data.local.entity.TransactionEntity) {
        val options = arrayOf("多选", "删除流水")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> enterSelectionMode(transaction)
                    1 -> showDeleteConfirm(transaction)
                }
            }
            .show()
    }

    private fun enterSelectionMode(transaction: com.nubook.data.local.entity.TransactionEntity) {
        adapter.isSelectionMode = true
        adapter.selectedIds.add(transaction.id)
        adapter.notifyDataSetChanged()
        binding.layoutSelectionBar.visibility = View.VISIBLE
        binding.fabContainer.visibility = View.GONE
        updateSelectionUI(adapter.selectedIds)
    }

    private fun exitSelectionMode() {
        adapter.isSelectionMode = false
        binding.layoutSelectionBar.visibility = View.GONE
        binding.fabContainer.visibility = View.VISIBLE
    }

    private fun updateSelectionUI(selectedIds: Set<String>) {
        binding.tvSelectionCount.text = "已选择 ${selectedIds.size} 项"
        
        // 计算所选合计
        val selectedTransactions = adapter.currentList.filter { selectedIds.contains(it.id) }
        var sum = 0.0
        selectedTransactions.forEach {
            if (it.type == TransactionType.INCOME) sum += it.amount else sum -= it.amount
        }
        
        val formatted = String.format(java.util.Locale.US, "¥ %.2f", sum)
        binding.tvSelectionSum.text = "合计: $formatted"
        binding.tvSelectionSum.setTextColor(if (sum >= 0) ColorEngine.getIncomeColor() else ColorEngine.getExpenseColor())
    }

    private fun showDeleteConfirm(transaction: com.nubook.data.local.entity.TransactionEntity) {
        AlertDialog.Builder(this)
            .setTitle("确认删除流水")
            .setMessage("确定删除这条 ¥${transaction.amount} 的记录吗？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteTransaction(transaction) }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onBackPressed() {
        if (adapter.isSelectionMode) {
            exitSelectionMode()
        } else {
            super.onBackPressed()
        }
    }
}
