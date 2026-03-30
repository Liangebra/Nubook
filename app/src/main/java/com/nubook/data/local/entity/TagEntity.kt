package com.nubook.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 标签实体
 * 对应数据库中的 tag 表
 */
@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String
)

/**
 * 流水与标签的交叉关联表
 * 一笔流水可以携带多个标签（多对多关系）
 */
@Entity(
    tableName = "transaction_tag_cross_ref",
    primaryKeys = ["transaction_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tag_id"])
    ]
)
data class TransactionTagCrossRef(
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "tag_id")
    val tagId: String
)
