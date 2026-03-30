package com.nubook.data.repository

import androidx.lifecycle.LiveData
import com.nubook.data.local.dao.LedgerDao
import com.nubook.data.local.dao.LedgerWithBalance
import com.nubook.data.local.dao.TransactionDao
import com.nubook.data.local.entity.LedgerEntity
import java.util.UUID

/**
 * 账本仓库
 * 管理账本的 CRUD
 */
class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val transactionDao: TransactionDao
) {

    // 获取所有账本
    fun getAllLedgers(): LiveData<List<LedgerEntity>> = ledgerDao.getAllLedgers()

    // 带结余获取所有账本
    fun getAllLedgersWithBalance(): LiveData<List<LedgerWithBalance>> =
        ledgerDao.getAllLedgersWithBalance()

    // 创建账本
    suspend fun createLedger(name: String, themeColorIndex: Int) {
        val ledger = LedgerEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            themeColorIndex = themeColorIndex,
            createdAt = System.currentTimeMillis()
        )
        ledgerDao.insert(ledger)
    }

    // 更新名称
    suspend fun updateName(id: String, name: String) {
        ledgerDao.updateName(id, name)
    }

    // 删除账本
    suspend fun deleteLedger(ledger: LedgerEntity) {
        ledgerDao.delete(ledger)
    }

    // 根据ID删除账本 (V2)
    suspend fun deleteLedgerById(id: String) {
        ledgerDao.deleteById(id)
    }

    // 搜索账本
    fun searchLedgers(keyword: String): LiveData<List<LedgerEntity>> = 
        ledgerDao.searchLedgers("%$keyword%")
}
