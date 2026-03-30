package com.nubook.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nubook.R
import com.nubook.data.local.entity.TransactionType
import com.nubook.ui.theme.ColorEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账本列表适配器 (V2: 支持多选)
 * 微信风格的会话列表，用于展示账本及最新流水
 */
class LedgerAdapter(
    private val onItemClick: (LedgerListItem) -> Unit,
    private val onItemLongClick: (LedgerListItem) -> Boolean,
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<LedgerListItem, LedgerAdapter.LedgerViewHolder>(LedgerDiffCallback()) {

    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) selectedIds.clear()
            notifyDataSetChanged()
        }

    val selectedIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ledger, parent, false)
        return LedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarText: TextView = itemView.findViewById(R.id.tv_avatar)
        private val avatarBg: View = itemView.findViewById(R.id.view_avatar_bg)
        private val ledgerName: TextView = itemView.findViewById(R.id.tv_ledger_name)
        private val latestNote: TextView = itemView.findViewById(R.id.tv_latest_note)
        private val balance: TextView = itemView.findViewById(R.id.tv_balance)
        private val date: TextView = itemView.findViewById(R.id.tv_date)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(item: LedgerListItem) {
            val ledger = item.ledger

            avatarText.text = if (ledger.name.isNotEmpty()) ledger.name.first().toString() else "?"
            avatarBg.background.setTint(ColorEngine.getSoftColor(ledger.themeColorIndex))
            avatarText.setTextColor(ColorEngine.getPrimaryColor(ledger.themeColorIndex))

            ledgerName.text = ledger.name

            val latest = item.latestTransaction
            if (latest != null) {
                val notePrefix = if (latest.note.isNotEmpty()) latest.note else {
                    if (latest.type == TransactionType.INCOME) "收入" else "支出"
                }
                latestNote.text = "最后记录: $notePrefix"
                date.text = SimpleDateFormat("M月d日", Locale.CHINA).format(Date(latest.timestamp))
                date.visibility = View.VISIBLE
            } else {
                latestNote.text = "暂无记录"
                date.visibility = View.GONE
            }

            val balanceVal = item.balance
            val formattedBalance = String.format(Locale.US, "%.2f", kotlin.math.abs(balanceVal))
            balance.text = if (balanceVal >= 0) "¥ $formattedBalance" else "-¥ $formattedBalance"
            balance.setTextColor(if (balanceVal >= 0) ColorEngine.getIncomeColor() else ColorEngine.getExpenseColor())
            balance.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

            // 多选处理 (V2)
            if (isSelectionMode) {
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = selectedIds.contains(ledger.id)
                itemView.setOnClickListener {
                    if (selectedIds.contains(ledger.id)) {
                        selectedIds.remove(ledger.id)
                    } else {
                        selectedIds.add(ledger.id)
                    }
                    checkBox.isChecked = selectedIds.contains(ledger.id)
                    onSelectionChanged(selectedIds)
                }
            } else {
                checkBox.visibility = View.GONE
                itemView.setOnClickListener { onItemClick(item) }
            }

            itemView.setOnLongClickListener { onItemLongClick(item) }
        }
    }
}

class LedgerDiffCallback : DiffUtil.ItemCallback<LedgerListItem>() {
    override fun areItemsTheSame(oldItem: LedgerListItem, newItem: LedgerListItem) = oldItem.ledger.id == newItem.ledger.id
    override fun areContentsTheSame(oldItem: LedgerListItem, newItem: LedgerListItem) = oldItem == newItem
}
