package com.nubook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.local.entity.TransactionType

/**
 * 流水（交易）数据访问对象
 * 提供聚合查询、时间段过滤、跨账本合并等高级功能
 */
@Dao
interface TransactionDao {

    // 插入新流水 (同步，由导入等背景任务使用)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(transaction: TransactionEntity)

    // 插入新流水 (异步)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    // 批量插入（替换策略）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    // 更新或插入（合并策略）
    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    // 批量更新或插入
    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    // 删除流水
    @Delete
    suspend fun delete(transaction: TransactionEntity)

    // 根据ID删除
    @Query("DELETE FROM transaction_record WHERE id = :transactionId")
    suspend fun deleteById(transactionId: String)

    // 删除某账本下的所有流水
    @Query("DELETE FROM transaction_record WHERE ledger_id = :ledgerId")
    suspend fun deleteByLedgerId(ledgerId: String)

    // 查询某账本的所有流水（按时间降序）
    @Query("SELECT * FROM transaction_record WHERE ledger_id = :ledgerId ORDER BY timestamp DESC")
    fun getTransactionsByLedger(ledgerId: String): LiveData<List<TransactionEntity>>

    // 同步查询某账本的所有流水（用于导出）
    @Query("SELECT * FROM transaction_record WHERE ledger_id = :ledgerId ORDER BY timestamp DESC")
    suspend fun getTransactionsByLedgerSync(ledgerId: String): List<TransactionEntity>

    // 查询某账本在时间段内的流水
    @Query("""
        SELECT * FROM transaction_record 
        WHERE ledger_id = :ledgerId 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsInRange(
        ledgerId: String,
        startTime: Long,
        endTime: Long
    ): LiveData<List<TransactionEntity>>

    // 计算某账本的总结余（收入-支出）
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'INCOME') -
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'EXPENSE'),
            0.0
        )
    """)
    fun getBalance(ledgerId: String): LiveData<Double>

    // 同步计算某账本的总结余
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'INCOME') -
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'EXPENSE'),
            0.0
        )
    """)
    suspend fun getBalanceSync(ledgerId: String): Double

    // 计算某账本在时间段内的结余
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'INCOME' AND timestamp BETWEEN :startTime AND :endTime) -
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'EXPENSE' AND timestamp BETWEEN :startTime AND :endTime),
            0.0
        )
    """)
    fun getBalanceInRange(ledgerId: String, startTime: Long, endTime: Long): LiveData<Double>

    // 同步计算时间段内结余
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'INCOME' AND timestamp BETWEEN :startTime AND :endTime) -
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id = :ledgerId AND type = 'EXPENSE' AND timestamp BETWEEN :startTime AND :endTime),
            0.0
        )
    """)
    suspend fun getBalanceInRangeSync(ledgerId: String, startTime: Long, endTime: Long): Double

    // 计算多账本合并结余（跨账本聚合）
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id IN (:ledgerIds) AND type = 'INCOME') -
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id IN (:ledgerIds) AND type = 'EXPENSE'),
            0.0
        )
    """)
    suspend fun getMultiLedgerBalance(ledgerIds: List<String>): Double

    // 计算多账本在时间段内的合并结余
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id IN (:ledgerIds) AND type = 'INCOME' AND timestamp BETWEEN :startTime AND :endTime) -
            (SELECT SUM(amount) FROM transaction_record WHERE ledger_id IN (:ledgerIds) AND type = 'EXPENSE' AND timestamp BETWEEN :startTime AND :endTime),
            0.0
        )
    """)
    suspend fun getMultiLedgerBalanceInRange(
        ledgerIds: List<String>,
        startTime: Long,
        endTime: Long
    ): Double

    // 计算总收入（跨账本）
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_record WHERE ledger_id IN (:ledgerIds) AND type = 'INCOME'")
    suspend fun getTotalIncomeSync(ledgerIds: List<String>): Double

    // 计算总支出（跨账本）
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_record WHERE ledger_id IN (:ledgerIds) AND type = 'EXPENSE'")
    suspend fun getTotalExpenseSync(ledgerIds: List<String>): Double

    // 计算总收入（时间段内）
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transaction_record 
        WHERE ledger_id IN (:ledgerIds) AND type = 'INCOME' 
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalIncome(ledgerIds: List<String>, startTime: Long, endTime: Long): Double

    // 计算总支出（时间段内）
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transaction_record 
        WHERE ledger_id IN (:ledgerIds) AND type = 'EXPENSE' 
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalExpense(ledgerIds: List<String>, startTime: Long, endTime: Long): Double

    // 按标签分组统计金额（用于扇形图）
    @Query("""
        SELECT tag_name as tagName, SUM(amount) as total FROM transaction_record 
        WHERE ledger_id IN (:ledgerIds) AND type = :type 
        AND timestamp BETWEEN :startTime AND :endTime 
        GROUP BY tag_name ORDER BY total DESC
    """)
    suspend fun getGroupedByTag(
        ledgerIds: List<String>,
        type: TransactionType,
        startTime: Long,
        endTime: Long
    ): List<TagAmount>

    // 按月份分组统计（用于折线图）
    @Query("""
        SELECT strftime('%Y-%m', timestamp / 1000, 'unixepoch', 'localtime') as period, 
               type, SUM(amount) as total 
        FROM transaction_record 
        WHERE ledger_id IN (:ledgerIds) AND timestamp BETWEEN :startTime AND :endTime 
        GROUP BY period, type ORDER BY period ASC
    """)
    suspend fun getMonthlyGrouped(
        ledgerIds: List<String>,
        startTime: Long,
        endTime: Long
    ): List<PeriodTypeAmount>

    // 全局搜索：支持可选的账本过滤 (V2)
    @Query("""
        SELECT * FROM transaction_record 
        WHERE (:ledgerId IS NULL OR ledger_id = :ledgerId)
        AND (note LIKE '%' || :keyword || '%' OR tag_name LIKE '%' || :keyword || '%')
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(ledgerId: String?, keyword: String): LiveData<List<TransactionEntity>>

    // 获取历史标签列表（按使用频率排序）
    @Query("SELECT tag_name FROM transaction_record GROUP BY tag_name ORDER BY COUNT(*) DESC")
    suspend fun getTagHistory(): List<String>

    // 获取某账本最近一条流水（用于主页预览）
    @Query("""
        SELECT * FROM transaction_record 
        WHERE ledger_id = :ledgerId 
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLatestTransaction(ledgerId: String): TransactionEntity?

    // 获取某账本最近一条流水（LiveData）
    @Query("""
        SELECT * FROM transaction_record 
        WHERE ledger_id = :ledgerId 
        ORDER BY timestamp DESC LIMIT 1
    """)
    fun getLatestTransactionLive(ledgerId: String): LiveData<TransactionEntity?>
}

/**
 * 按标签分组的金额统计结果
 */
data class TagAmount(
    val tagName: String,
    val total: Double
)

/**
 * 按时间段和类型分组的金额统计结果
 */
data class PeriodTypeAmount(
    val period: String,
    val type: TransactionType,
    val total: Double
)
