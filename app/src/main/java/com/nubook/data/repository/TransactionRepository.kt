package com.nubook.data.repository

import androidx.lifecycle.LiveData
import com.nubook.data.local.dao.TagAmount
import com.nubook.data.local.dao.PeriodTypeAmount
import com.nubook.data.local.dao.TransactionDao
import com.nubook.data.local.entity.TransactionEntity
import com.nubook.data.local.entity.TransactionType

/**
 * 流水仓库 (V2: 已迁移至标签统计)
 * 统一管理流水数据的增删改查及聚合计算
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    // 插入流水
    suspend fun insert(transaction: TransactionEntity) = transactionDao.insert(transaction)

    // 批量插入（替换策略）
    suspend fun insertAll(transactions: List<TransactionEntity>) =
        transactionDao.insertAll(transactions)

    // 更新或插入（合并策略）
    suspend fun upsert(transaction: TransactionEntity) = transactionDao.upsert(transaction)

    // 批量合并
    suspend fun upsertAll(transactions: List<TransactionEntity>) =
        transactionDao.upsertAll(transactions)

    // 删除
    suspend fun delete(transaction: TransactionEntity) = transactionDao.delete(transaction)

    // 根据ID删除
    suspend fun deleteById(id: String) = transactionDao.deleteById(id)

    // 获取某账本的流水列表
    fun getTransactionsByLedger(ledgerId: String): LiveData<List<TransactionEntity>> =
        transactionDao.getTransactionsByLedger(ledgerId)

    // 同步获取
    suspend fun getTransactionsByLedgerSync(ledgerId: String): List<TransactionEntity> =
        transactionDao.getTransactionsByLedgerSync(ledgerId)

    // 获取时间段内的流水
    fun getTransactionsInRange(
        ledgerId: String,
        startTime: Long,
        endTime: Long
    ): LiveData<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(ledgerId, startTime, endTime)

    // 单账本结余
    fun getBalance(ledgerId: String): LiveData<Double> = transactionDao.getBalance(ledgerId)

    suspend fun getBalanceSync(ledgerId: String): Double = transactionDao.getBalanceSync(ledgerId)

    // 时间段内结余
    fun getBalanceInRange(ledgerId: String, startTime: Long, endTime: Long): LiveData<Double> =
        transactionDao.getBalanceInRange(ledgerId, startTime, endTime)

    suspend fun getBalanceInRangeSync(ledgerId: String, startTime: Long, endTime: Long): Double =
        transactionDao.getBalanceInRangeSync(ledgerId, startTime, endTime)

    // 多账本合并结余
    suspend fun getMultiLedgerBalance(ledgerIds: List<String>): Double =
        transactionDao.getMultiLedgerBalance(ledgerIds)

    // 多账本时间段合并结余
    suspend fun getMultiLedgerBalanceInRange(
        ledgerIds: List<String>,
        startTime: Long,
        endTime: Long
    ): Double = transactionDao.getMultiLedgerBalanceInRange(ledgerIds, startTime, endTime)

    // 总收入
    suspend fun getTotalIncome(ledgerIds: List<String>, startTime: Long, endTime: Long): Double =
        transactionDao.getTotalIncome(ledgerIds, startTime, endTime)

    // 总支出
    suspend fun getTotalExpense(ledgerIds: List<String>, startTime: Long, endTime: Long): Double =
        transactionDao.getTotalExpense(ledgerIds, startTime, endTime)

    // 按标签分组（扇形图数据）- V2 已从 Note 更名为 Tag
    suspend fun getGroupedByTag(
        ledgerIds: List<String>,
        type: TransactionType,
        startTime: Long,
        endTime: Long
    ): List<TagAmount> = transactionDao.getGroupedByTag(ledgerIds, type, startTime, endTime)

    // 按月分组（折线图数据）
    suspend fun getMonthlyGrouped(
        ledgerIds: List<String>,
        startTime: Long,
        endTime: Long
    ): List<PeriodTypeAmount> = transactionDao.getMonthlyGrouped(ledgerIds, startTime, endTime)

    // 全局搜索
    fun searchTransactions(ledgerId: String?, keyword: String): LiveData<List<TransactionEntity>> =
        transactionDao.searchTransactions(ledgerId, keyword)

    // 标签历史
    suspend fun getTagHistory(): List<String> = transactionDao.getTagHistory()

    // 最近记录
    suspend fun getLatestTransaction(ledgerId: String): TransactionEntity? =
        transactionDao.getLatestTransaction(ledgerId)

    // 同步获取最近记录（别名）
    suspend fun getLatestTransactionSync(ledgerId: String): TransactionEntity? =
        transactionDao.getLatestTransaction(ledgerId)

    // 总收入（跨账本）
    suspend fun getTotalIncomeSync(ledgerIds: List<String>): Double =
        transactionDao.getTotalIncomeSync(ledgerIds)

    // 总支出（跨账本）
    suspend fun getTotalExpenseSync(ledgerIds: List<String>): Double =
        transactionDao.getTotalExpenseSync(ledgerIds)
}
