package com.nubook.ui.ledger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nubook.R
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.local.entity.TransactionType
import com.nubook.ui.theme.ColorEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 流水列表适配器 (V2: 支持多选与聚合)
 */
class TransactionAdapter(
    private val onItemClick: (TransactionEntity) -> Unit,
    private val onItemLongClick: (TransactionEntity) -> Unit,
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) selectedIds.clear()
            notifyDataSetChanged()
        }

    val selectedIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)
        private val tvNote: TextView = itemView.findViewById(R.id.tv_note)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(transaction: TransactionEntity) {
            val isIncome = transaction.type == TransactionType.INCOME

            tvType.text = if (isIncome) "收入" else "支出"
            tvType.setTextColor(if (isIncome) ColorEngine.getIncomeColor() else ColorEngine.getExpenseColor())
            tvNote.text = if (transaction.note.isNotEmpty()) transaction.note else "无备注"

            val formatted = String.format(Locale.US, "%.2f", transaction.amount)
            tvAmount.text = if (isIncome) "+¥ $formatted" else "-¥ $formatted"
            tvAmount.setTextColor(if (isIncome) ColorEngine.getIncomeColor() else ColorEngine.getExpenseColor())

            val dateFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
            tvTime.text = dateFormat.format(Date(transaction.timestamp))

            // 多选处理 (V2)
            if (isSelectionMode) {
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = selectedIds.contains(transaction.id)
                // 隐藏右侧金额/时间以防遮挡 (用户要求)
                tvAmount.visibility = View.GONE
                tvTime.visibility = View.GONE
                
                itemView.setOnClickListener {
                    if (selectedIds.contains(transaction.id)) {
                        selectedIds.remove(transaction.id)
                    } else {
                        selectedIds.add(transaction.id)
                    }
                    checkBox.isChecked = selectedIds.contains(transaction.id)
                    onSelectionChanged(selectedIds)
                }
            } else {
                checkBox.visibility = View.GONE
                tvAmount.visibility = View.VISIBLE
                tvTime.visibility = View.VISIBLE
                itemView.setOnClickListener { onItemClick(transaction) }
                itemView.setOnLongClickListener {
                    onItemLongClick(transaction)
                    true
                }
            }
        }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
    override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem == newItem
}
