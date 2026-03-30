package com.nubook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nubook.data.local.entity.LedgerEntity

/**
 * 账本数据访问对象
 */
@Dao
interface LedgerDao {

    // 插入新账本
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: LedgerEntity)

    // 更新或插入（合并策略）
    @Upsert
    suspend fun upsert(ledger: LedgerEntity)

    // 删除账本
    @Delete
    suspend fun delete(ledger: LedgerEntity)

    // 根据ID删除
    @Query("DELETE FROM ledger WHERE id = :ledgerId")
    suspend fun deleteById(ledgerId: String)

    // 更新账本名称
    @Query("UPDATE ledger SET name = :newName WHERE id = :ledgerId")
    suspend fun updateName(ledgerId: String, newName: String)

    // 更新主题色
    @Query("UPDATE ledger SET theme_color_index = :colorIndex WHERE id = :ledgerId")
    suspend fun updateThemeColor(ledgerId: String, colorIndex: Int)

    // 查询所有账本（按创建时间降序）
    @Query("SELECT * FROM ledger ORDER BY created_at DESC")
    fun getAllLedgers(): LiveData<List<LedgerEntity>>

    // 同步查询所有账本（用于导出）
    @Query("SELECT * FROM ledger ORDER BY created_at DESC")
    suspend fun getAllLedgersSync(): List<LedgerEntity>

    // 根据ID查询单个账本
    @Query("SELECT * FROM ledger WHERE id = :ledgerId")
    suspend fun getLedgerById(ledgerId: String): LedgerEntity?

    // 根据ID查询（LiveData）
    @Query("SELECT * FROM ledger WHERE id = :ledgerId")
    fun getLedgerByIdLive(ledgerId: String): LiveData<LedgerEntity?>

    // 搜索账本（按名称模糊匹配）
    @Query("SELECT * FROM ledger WHERE name LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    fun searchLedgers(keyword: String): LiveData<List<LedgerEntity>>

    // 获取账本及其当前结余（用于主页展示）
    @Query("""
        SELECT *, 
        (SELECT COALESCE(SUM(amount), 0.0) FROM transaction_record WHERE ledger_id = l.id AND type = 'INCOME') - 
        (SELECT COALESCE(SUM(amount), 0.0) FROM transaction_record WHERE ledger_id = l.id AND type = 'EXPENSE') as balance
        FROM ledger as l ORDER BY created_at DESC
    """)
    fun getAllLedgersWithBalance(): LiveData<List<LedgerWithBalance>>
}

/**
 * 账本及其结余信息的组合类
 */
data class LedgerWithBalance(
    @Embedded val ledger: LedgerEntity,
    val balance: Double
)
