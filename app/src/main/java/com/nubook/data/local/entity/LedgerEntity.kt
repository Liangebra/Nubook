package com.nubook.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 账本实体
 * 对应数据库中的 ledger 表
 */
@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // 主题色索引（0-7，对应8种基准色）
    @ColumnInfo(name = "theme_color_index")
    val themeColorIndex: Int = 4 // 默认湖水青
)
