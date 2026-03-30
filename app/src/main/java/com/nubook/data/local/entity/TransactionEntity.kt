package com.nubook.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 交易类型枚举
 */
enum class TransactionType {
    INCOME,  // 收入
    EXPENSE  // 支出
}

/**
 * 流水（交易）实体
 * 对应数据库中的 transaction_record 表
 * 每条记录绑定金额、类型（收入/支出）、时间戳和备注等元数据
 */
@Entity(
    tableName = "transaction_record",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledger_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ledger_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["tag_name"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "ledger_id")
    val ledgerId: String,

    // 金额，双精度浮点以防精度丢失
    @ColumnInfo(name = "amount")
    val amount: Double,

    // 交易类型（收入/支出）
    @ColumnInfo(name = "type")
    val type: TransactionType,

    // 精确到毫秒的时间戳
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    // 标签名称（主分类）
    @ColumnInfo(name = "tag_name")
    val tagName: String = "",

    // 备注文本
    @ColumnInfo(name = "note")
    val note: String = ""
)
