package com.nubook.ui.statistics

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nubook.databinding.ActivityStatisticsBinding
import com.nubook.ui.theme.ColorEngine

import com.nubook.ui.base.BaseActivity

/**
 * 统计页面 Activity
 * 展示扇形图、折线图和聚合统计数据
 * 使用 MPAndroidChart，去除默认阴影，使用柔和变量色填充
 */
class StatisticsActivity : BaseActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var viewModel: StatisticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ledgerIds = intent.getStringArrayExtra("ledger_ids")?.toList()
            ?: listOf(intent.getStringExtra("ledger_id") ?: return finish())

        viewModel = ViewModelProvider(
            this,
            StatisticsViewModel.Factory(application, ledgerIds)
        )[StatisticsViewModel::class.java]

        setupCharts()
        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        // V2: 点击切换收支扇形图
        binding.layoutTotalIncome.setOnClickListener {
            viewModel.toggleChartType(com.nubook.data.local.entity.TransactionType.INCOME)
        }
        binding.layoutTotalExpense.setOnClickListener {
            viewModel.toggleChartType(com.nubook.data.local.entity.TransactionType.EXPENSE)
        }
    }

    /**
     * 配置图表样式（扁平无阴影）
     */
    private fun setupCharts() {
        // 扇形图配置
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 50f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            transparentCircleRadius = 54f
            setDrawEntryLabels(false)
            setDrawSliceText(false)
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            // 无阴影
            animateXY(500, 500)
        }

        // 折线图配置
        binding.lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = android.graphics.Color.parseColor("#F0F0F0")
            }
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            }
            animateX(500)
        }
    }

    private fun observeData() {
        viewModel.currentChartType.observe(this) { type ->
            val isExpense = type == com.nubook.data.local.entity.TransactionType.EXPENSE
            binding.tvPieTitle.text = if (isExpense) "支出分布" else "收入分布"
            
            // 简单高亮选中卡片
            binding.layoutTotalExpense.alpha = if (isExpense) 1.0f else 0.6f
            binding.layoutTotalIncome.alpha = if (!isExpense) 1.0f else 0.6f
        }

        viewModel.totalIncome.observe(this) { income ->
            binding.tvTotalIncome.text = String.format(java.util.Locale.US, "¥ %.2f", income)
            binding.tvTotalIncome.setTextColor(ColorEngine.getIncomeColor())
        }

        viewModel.totalExpense.observe(this) { expense ->
            binding.tvTotalExpense.text = String.format(java.util.Locale.US, "¥ %.2f", expense)
            binding.tvTotalExpense.setTextColor(ColorEngine.getExpenseColor())
        }

        viewModel.pieData.observe(this) { entries ->
            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "")
                val colors = ColorEngine.getSoftColorPalette(entries.size)
                dataSet.colors = colors
                dataSet.setDrawValues(true)
                dataSet.valueTextSize = 11f
                dataSet.valueTextColor = android.graphics.Color.BLACK
                dataSet.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${String.format(java.util.Locale.US, "%.1f", value)}%"
                    }
                }
                binding.pieChart.data = PieData(dataSet)
                binding.pieChart.animateXY(500, 500)
                binding.pieChart.invalidate()
                binding.pieChart.visibility = View.VISIBLE
            } else {
                binding.pieChart.visibility = View.GONE
            }
        }

        viewModel.lineData.observe(this) { (incomeEntries, expenseEntries, labels) ->
            if (incomeEntries.isNotEmpty() || expenseEntries.isNotEmpty()) {
                val dataSets = mutableListOf<LineDataSet>()

                if (incomeEntries.isNotEmpty()) {
                    val incomeDataSet = LineDataSet(incomeEntries, "收入")
                    incomeDataSet.color = ColorEngine.getIncomeColor()
                    incomeDataSet.setCircleColor(ColorEngine.getIncomeColor())
                    incomeDataSet.lineWidth = 2f
                    incomeDataSet.circleRadius = 3f
                    incomeDataSet.setDrawValues(false)
                    dataSets.add(incomeDataSet)
                }

                if (expenseEntries.isNotEmpty()) {
                    val expenseDataSet = LineDataSet(expenseEntries, "支出")
                    expenseDataSet.color = ColorEngine.getExpenseColor()
                    expenseDataSet.setCircleColor(ColorEngine.getExpenseColor())
                    expenseDataSet.lineWidth = 2f
                    expenseDataSet.circleRadius = 3f
                    expenseDataSet.setDrawValues(false)
                    dataSets.add(expenseDataSet)
                }

                binding.lineChart.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index in labels.indices) labels[index] else ""
                    }
                }

                binding.lineChart.data = LineData(dataSets.toList())
                binding.lineChart.invalidate()
                binding.lineChart.visibility = View.VISIBLE
            } else {
                binding.lineChart.visibility = View.GONE
            }
        }
    }
}
