package com.nubook.data.export

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nubook.NuBookApplication
import com.nubook.data.local.entity.LedgerEntity
import com.nubook.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据导出管理器 (V2.1)
 * 导出到外部存储 NuBook 文件夹，导入从该文件夹或系统文件选择器读取
 * 使用 FileProvider 生成安全的 content:// URI 进行分享
 */
class ExportManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val database = NuBookApplication.instance.database
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    companion object {
        // 外部存储中的 NuBook 文件夹名
        const val EXPORT_FOLDER_NAME = "NuBook"

        /**
         * 获取 NuBook 导出文件夹 (外部存储/Documents/NuBook)
         */
        fun getExportDir(context: Context): File {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val nuBookDir = File(documentsDir, EXPORT_FOLDER_NAME)
            if (!nuBookDir.exists()) nuBookDir.mkdirs()
            return nuBookDir
        }

        /**
         * 列出NuBook文件夹中的可导入文件
         */
        fun listImportableFiles(context: Context): Array<File> {
            val dir = getExportDir(context)
            return dir.listFiles { file ->
                file.isFile && (file.extension in listOf("json", "jsonl", "csv"))
            } ?: emptyArray()
        }
    }

    /**
     * 导出特定账本的数据
     */
    fun exportLedger(ledger: LedgerEntity, format: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = database.transactionDao().getTransactionsByLedgerSync(ledger.id)
                val file = when (format) {
                    "json" -> exportJson(listOf(ledger), mapOf(ledger.id to transactions))
                    "jsonl" -> exportJsonl(listOf(ledger), mapOf(ledger.id to transactions))
                    "csv" -> exportCsv(listOf(ledger), mapOf(ledger.id to transactions))
                    else -> return@launch
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已导出到: Documents/NuBook/${file.name}", Toast.LENGTH_LONG).show()
                    shareFile(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 导出所有数据
     */
    fun exportAll(format: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ledgers = database.ledgerDao().getAllLedgersSync()
                val allTransactions = mutableMapOf<String, List<TransactionEntity>>()
                ledgers.forEach { ledger ->
                    allTransactions[ledger.id] =
                        database.transactionDao().getTransactionsByLedgerSync(ledger.id)
                }
                val file = when (format) {
                    "json" -> exportJson(ledgers, allTransactions)
                    "jsonl" -> exportJsonl(ledgers, allTransactions)
                    "csv" -> exportCsv(ledgers, allTransactions)
                    else -> return@launch
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已导出到: Documents/NuBook/${file.name}", Toast.LENGTH_LONG).show()
                    shareFile(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * JSON 格式导出
     */
    private fun exportJson(
        ledgers: List<LedgerEntity>,
        transactions: Map<String, List<TransactionEntity>>
    ): File {
        val data = mapOf(
            "export_time" to System.currentTimeMillis(),
            "version" to "2.1",
            "ledgers" to ledgers.map { ledger ->
                mapOf(
                    "id" to ledger.id,
                    "name" to ledger.name,
                    "transactions" to (transactions[ledger.id] ?: emptyList()).map { t ->
                        mapOf(
                            "amount" to t.amount,
                            "type" to t.type.name,
                            "tag" to t.tagName,
                            "timestamp" to t.timestamp,
                            "note" to t.note
                        )
                    }
                )
            }
        )
        val file = createExportFile("NuBook_${dateFormat.format(Date())}.json")
        FileWriter(file).use { it.write(gson.toJson(data)) }
        return file
    }

    /**
     * JSONL 格式导出
     */
    private fun exportJsonl(
        ledgers: List<LedgerEntity>,
        transactions: Map<String, List<TransactionEntity>>
    ): File {
        val file = createExportFile("NuBook_${dateFormat.format(Date())}.jsonl")
        FileWriter(file).use { writer ->
            ledgers.forEach { ledger ->
                (transactions[ledger.id] ?: emptyList()).forEach { t ->
                    val line = gson.toJson(
                        mapOf(
                            "ledger_name" to ledger.name,
                            "amount" to t.amount,
                            "type" to t.type.name,
                            "tag" to t.tagName,
                            "timestamp" to t.timestamp,
                            "note" to t.note
                        )
                    )
                    writer.write(line + "\n")
                }
            }
        }
        return file
    }

    /**
     * CSV 格式导出
     */
    private fun exportCsv(
        ledgers: List<LedgerEntity>,
        transactions: Map<String, List<TransactionEntity>>
    ): File {
        val file = createExportFile("NuBook_${dateFormat.format(Date())}.csv")
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        FileWriter(file).use { writer ->
            writer.write("\uFEFF")
            writer.write("账本,金额,类型,标签,时间,备注\n")
            ledgers.forEach { ledger ->
                (transactions[ledger.id] ?: emptyList()).forEach { t ->
                    val typeStr = if (t.type.name == "INCOME") "收入" else "支出"
                    val note = t.note.replace(",", "，").replace("\n", " ")
                    val tag = t.tagName.replace(",", "，")
                    writer.write("${ledger.name},${t.amount},$typeStr,$tag,${timeFmt.format(Date(t.timestamp))},$note\n")
                }
            }
        }
        return file
    }

    /**
     * 解析导入的内容（支持 JSONL 逐行、完整的 JSON 导出对象、CSV 格式）
     */
    private fun parseImportContent(content: String, targetLedgerId: String): Int {
        var count = 0
        val trimmed = content.trimStart()
        
        if (trimmed.startsWith("{") && trimmed.contains("\"ledgers\"")) {
            // 这是一个完整的 JSON 导出文件
            val root = gson.fromJson(trimmed, Map::class.java)
            val ledgersList = root["ledgers"] as? List<Map<String, Any>> ?: emptyList()
            for (ledgerMap in ledgersList) {
                val txs = ledgerMap["transactions"] as? List<Map<String, Any>> ?: emptyList()
                for (map in txs) {
                    val transaction = TransactionEntity(
                        ledgerId = targetLedgerId,
                        amount = (map["amount"] as? Double) ?: 0.0,
                        type = if (map["type"] == "INCOME") com.nubook.data.local.entity.TransactionType.INCOME
                               else com.nubook.data.local.entity.TransactionType.EXPENSE,
                        tagName = (map["tag"] as? String) ?: (map["ledger_name"] as? String) ?: "其他",
                        note = (map["note"] as? String) ?: "",
                        timestamp = ((map["timestamp"] as? Number)?.toLong()) ?: System.currentTimeMillis()
                    )
                    database.transactionDao().insertSync(transaction)
                    count++
                }
            }
        } else if (trimmed.startsWith("\uFEFF账本") || trimmed.startsWith("账本")) {
            // CSV 格式文件
            val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            content.lines().forEachIndexed { index, line ->
                if (index > 0 && line.trim().isNotEmpty()) {
                    val parts = line.split(",")
                    if (parts.size >= 6) {
                        try {
                            val amount = parts[1].toDoubleOrNull() ?: 0.0
                            val type = if (parts[2] == "收入") com.nubook.data.local.entity.TransactionType.INCOME 
                                       else com.nubook.data.local.entity.TransactionType.EXPENSE
                            val timestamp = try {
                                timeFmt.parse(parts[4])?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            val transaction = TransactionEntity(
                                ledgerId = targetLedgerId,
                                amount = amount,
                                type = type,
                                tagName = parts[3].replace("，", ","),
                                note = parts[5].replace("，", ","),
                                timestamp = timestamp
                            )
                            database.transactionDao().insertSync(transaction)
                            count++
                        } catch (e: Exception) {
                            // 跳过无效行
                        }
                    }
                }
            }
        } else {
            // 当作 JSONL 逐行解析
            content.lines().forEach { line ->
                if (line.trim().isNotEmpty()) {
                    try {
                        val map = gson.fromJson(line, Map::class.java)
                        val transaction = TransactionEntity(
                            ledgerId = targetLedgerId,
                            amount = (map["amount"] as? Double) ?: 0.0,
                            type = if (map["type"] == "INCOME") com.nubook.data.local.entity.TransactionType.INCOME
                                   else com.nubook.data.local.entity.TransactionType.EXPENSE,
                            tagName = (map["tag"] as? String) ?: (map["ledger_name"] as? String) ?: "其他",
                            note = (map["note"] as? String) ?: "",
                            timestamp = ((map["timestamp"] as? Number)?.toLong()) ?: System.currentTimeMillis()
                        )
                        database.transactionDao().insertSync(transaction)
                        count++
                    } catch (e: Exception) {
                        // 忽略解析失败的行
                    }
                }
            }
        }
        return count
    }

    /**
     * 从 URI 导入数据 (自动判断 JSON 或 JSONL)
     */
    suspend fun importJsonl(uri: android.net.Uri, targetLedgerId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("无法打开文件"))
            val content = inputStream.bufferedReader().use { it.readText() }
            val count = parseImportContent(content, targetLedgerId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从本地文件直接导入数据
     */
    suspend fun importFromFile(file: File, targetLedgerId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val count = parseImportContent(content, targetLedgerId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建导出文件 (存放到外部 Documents/NuBook 文件夹)
     */
    private fun createExportFile(filename: String): File {
        val exportDir = getExportDir(context)
        return File(exportDir, filename)
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            val extension = file.extension
            type = when (extension) {
                "json" -> "application/json"
                "jsonl" -> "application/json"
                "csv" -> "text/csv"
                else -> "application/octet-stream"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享导出数据"))
    }
}
