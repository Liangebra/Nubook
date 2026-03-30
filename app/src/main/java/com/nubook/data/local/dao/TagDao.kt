package com.nubook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nubook.data.local.entity.TagEntity
import com.nubook.data.local.entity.TransactionTagCrossRef

/**
 * 标签数据访问对象
 */
@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: TransactionTagCrossRef)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transaction_id = :transactionId")
    suspend fun deleteTagsForTransaction(transactionId: String)

    // 查询所有标签
    @Query("SELECT * FROM tag ORDER BY name ASC")
    fun getAllTags(): LiveData<List<TagEntity>>

    // 根据名称查找标签
    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    // 查询某笔流水的所有标签
    @Query("""
        SELECT t.* FROM tag t 
        INNER JOIN transaction_tag_cross_ref ref ON t.id = ref.tag_id 
        WHERE ref.transaction_id = :transactionId
    """)
    suspend fun getTagsForTransaction(transactionId: String): List<TagEntity>
}
