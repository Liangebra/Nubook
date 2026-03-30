package com.nubook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nubook.data.local.dao.LedgerDao
import com.nubook.data.local.dao.ReminderDao
import com.nubook.data.local.dao.TagDao
import com.nubook.data.local.dao.TransactionDao
import com.nubook.data.local.entity.*

/**
 * NuBook Room 数据库
 * 单例模式，包含所有数据表
 */
@Database(
    entities = [
        LedgerEntity::class,
        TransactionEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NuBookDatabase : RoomDatabase() {

    abstract fun ledgerDao(): LedgerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: NuBookDatabase? = null

        fun getInstance(context: Context): NuBookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NuBookDatabase::class.java,
                    "nubook_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
