package com.nubook.ui.home

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nubook.R
import com.nubook.data.export.ExportManager
import com.nubook.databinding.ActivityHomeBinding
import com.nubook.ui.base.BaseActivity
import com.nubook.ui.ledger.LedgerDetailActivity
import com.nubook.ui.settings.SettingsActivity
import com.nubook.ui.theme.ColorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主页 Activity (V2.1)
 * 展示账本列表、搜索入口、多选聚合统计、导入导出
 */
class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: LedgerAdapter

    // 当前正在执行导入的目标账本ID
    private var importTargetLedgerId: String? = null

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImportFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ColorEngine.init(this)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnAdd.setOnClickListener {
            showAddLedgerDialog()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 搜索入口
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this, com.nubook.ui.search.SearchActivity::class.java))
        }

        // 退出按钮
        binding.btnExit.setOnClickListener {
            finishAffinity()
        }
        
        // 动态适配主题色
        val primaryColor = ColorEngine.getPrimaryColor()
        binding.btnAdd.background.setTint(primaryColor)
        binding.btnExit.setColorFilter(primaryColor)
        binding.btnSettings.setColorFilter(primaryColor)
    }

    private fun setupRecyclerView() {
        adapter = LedgerAdapter(
            onItemClick = { item -> handleItemClick(item) },
            onItemLongClick = { item -> showLedgerMenu(item) },
            onSelectionChanged = { selectedIds -> updateFabState(selectedIds) }
        )

        binding.rvLedgers.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.ledgers.observe(this) { list ->
            adapter.submitList(list)
            binding.layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.aggregateStats.observe(this) { stats ->
            if (stats != null) {
                showAggregationResult(stats)
            }
        }
    }

    private fun handleItemClick(item: LedgerListItem) {
        val intent = Intent(this, LedgerDetailActivity::class.java).apply {
            putExtra("ledger_id", item.ledger.id)
            putExtra("ledger_name", item.ledger.name)
        }
        startActivity(intent)
    }

    private fun showLedgerMenu(item: LedgerListItem): Boolean {
        val options = arrayOf("详情", "重命名", "导出数据", "导入数据", "删除", "多选模式")
        AlertDialog.Builder(this)
            .setTitle(item.ledger.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> handleItemClick(item)
                    1 -> showRenameDialog(item)
                    2 -> showExportDialog(item)
                    3 -> startImport(item)
                    4 -> showDeleteConfirm(item)
                    5 -> enterSelectionMode(item)
                }
            }
            .show()
        return true
    }

    // 导出：选择格式后导出
    private fun showExportDialog(item: LedgerListItem) {
        val formats = arrayOf("JSON", "CSV")
        AlertDialog.Builder(this)
            .setTitle("选择导出格式")
            .setItems(formats) { _, which ->
                val format = if (which == 0) "json" else "csv"
                ExportManager(this).exportLedger(item.ledger, format)
            }
            .show()
    }

    // 导入：先检查 NuBook 文件夹的文件，再提供系统文件选择器
    private fun startImport(item: LedgerListItem) {
        importTargetLedgerId = item.ledger.id
        val files = ExportManager.listImportableFiles(this)
        if (files.isEmpty()) {
            // 文件夹为空，直接打开系统文件选择器
            filePickerLauncher.launch("*/*")
        } else {
            // 显示文件夹中的文件列表
            val fileNames = files.map { it.name }.toMutableList()
            fileNames.add("从其他位置选择...")
            AlertDialog.Builder(this)
                .setTitle("选择导入文件")
                .setItems(fileNames.toTypedArray()) { _, which ->
                    if (which < files.size) {
                        // 从 NuBook 文件夹导入
                        importFromLocalFile(files[which])
                    } else {
                        // 打开系统文件选择器
                        filePickerLauncher.launch("*/*")
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    // 从本地文件导入
    private fun importFromLocalFile(file: java.io.File) {
        val ledgerId = importTargetLedgerId ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val result = ExportManager(this@HomeActivity).importFromFile(file, ledgerId)
            result.onSuccess { count ->
                Toast.makeText(this@HomeActivity, "成功导入 $count 条记录", Toast.LENGTH_SHORT).show()
                viewModel.refresh()
            }.onFailure { e ->
                Toast.makeText(this@HomeActivity, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 处理系统文件选择器返回的文件
    private fun handleImportFile(uri: Uri) {
        val ledgerId = importTargetLedgerId ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val result = ExportManager(this@HomeActivity).importJsonl(uri, ledgerId)
            result.onSuccess { count ->
                Toast.makeText(this@HomeActivity, "成功导入 $count 条记录", Toast.LENGTH_SHORT).show()
                viewModel.refresh()
            }.onFailure { e ->
                Toast.makeText(this@HomeActivity, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enterSelectionMode(item: LedgerListItem) {
        adapter.isSelectionMode = true
        adapter.selectedIds.add(item.ledger.id)
        adapter.notifyDataSetChanged()
        updateFabState(adapter.selectedIds)
    }

    private fun updateFabState(selectedIds: Set<String>) {
        if (adapter.isSelectionMode) {
            binding.btnAdd.setImageResource(R.drawable.ic_statistics)
            binding.btnAdd.setOnClickListener {
                if (selectedIds.isEmpty()) {
                    exitSelectionMode()
                } else {
                    viewModel.calculateAggregation(selectedIds)
                }
            }
            binding.btnSettings.setImageResource(R.drawable.ic_close)
            binding.btnSettings.setOnClickListener {
                exitSelectionMode()
            }
        } else {
            binding.btnAdd.setImageResource(R.drawable.ic_add)
            binding.btnAdd.setOnClickListener { showAddLedgerDialog() }
            binding.btnSettings.setImageResource(R.drawable.ic_settings)
            binding.btnSettings.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    private fun exitSelectionMode() {
        adapter.isSelectionMode = false
        updateFabState(emptySet())
    }

    private fun showAddLedgerDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_create_ledger)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 设置装饰色为当前主题色
        val accentLine = dialog.findViewById<View>(R.id.view_accent_line)
        accentLine.setBackgroundColor(ColorEngine.getPrimaryColor())

        val confirmBtn = dialog.findViewById<TextView>(R.id.btn_confirm)
        confirmBtn.background.setTint(ColorEngine.getPrimaryColor())

        val input = dialog.findViewById<EditText>(R.id.et_ledger_name)

        confirmBtn.setOnClickListener {
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.createLedger(name)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "请输入账本名称", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRenameDialog(item: LedgerListItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_create_ledger)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val title = dialog.findViewById<TextView>(R.id.tv_dialog_title)
        title.text = "重命名账本"

        val accentLine = dialog.findViewById<View>(R.id.view_accent_line)
        accentLine.setBackgroundColor(ColorEngine.getPrimaryColor())

        val confirmBtn = dialog.findViewById<TextView>(R.id.btn_confirm)
        confirmBtn.text = "确定"
        confirmBtn.background.setTint(ColorEngine.getPrimaryColor())

        val input = dialog.findViewById<EditText>(R.id.et_ledger_name)
        input.setText(item.ledger.name)
        input.hint = "新名称"

        confirmBtn.setOnClickListener {
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.updateLedgerName(item.ledger.id, name)
                dialog.dismiss()
            }
        }

        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirm(item: LedgerListItem) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除「${item.ledger.name}」吗？流水也将一并删除。")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteLedger(item.ledger.id) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAggregationResult(stats: AggregateStats) {
        val message = "总收入: ¥${String.format("%.2f", stats.totalIncome)}\n" +
                      "总支出: ¥${String.format("%.2f", stats.totalExpense)}\n" +
                      "总结余: ¥${String.format("%.2f", stats.balance)}"

        AlertDialog.Builder(this)
            .setTitle("聚合统计结果")
            .setMessage(message)
            .setPositiveButton("进入详情") { _, _ ->
                val intent = Intent(this, com.nubook.ui.statistics.StatisticsActivity::class.java)
                intent.putExtra("ledger_ids", adapter.selectedIds.toTypedArray())
                startActivity(intent)
            }
            .setNegativeButton("返回", null)
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
