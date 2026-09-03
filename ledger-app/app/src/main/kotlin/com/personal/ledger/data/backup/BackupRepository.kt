package com.personal.ledger.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.personal.ledger.BuildConfig
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.database.DATABASE_VERSION
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 备份与恢复（ARCHITECTURE.md 第 8 节 / MVP.md 第 9-10 节）：
 * - CSV Export：UTF-8 BOM；含普通交易与转账（转账同一行、targetAccount 有值、分类留空）；不含已删除交易。
 * - JSON Backup：含所有账本数据（含软删除交易），带备份格式版本。
 * - SQLite Backup：数据库文件完整拷贝。
 * - 恢复：整库覆盖，不做合并；恢复前自动备份当前数据；恢复后当前账本 ID 失效时回退。
 */
class BackupRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    // ---------- 导出 ----------

    /** 构建完整 JSON 备份内容。 */
    suspend fun buildBackupJson(): ByteArray = withContext(Dispatchers.IO) {
        // 多张表必须来自同一个数据库快照，避免后台备份撞上用户正在记账。
        val data = database.withTransaction {
            BackupData(
                ledgers = database.ledgerDao().getAll().map { it.toDto() },
                accounts = database.accountDao().getAll().map { it.toDto() },
                categories = database.categoryDao().getAll().map { it.toDto() },
                merchants = database.merchantDao().getAll().map { it.toDto() },
                tags = database.tagDao().getAll().map { it.toDto() },
                transactions = database.transactionDao().getAll().map { it.toDto() },
                transactionTags = database.transactionTagDao().getAll().map { it.toDto() },
                budgets = database.budgetDao().getAll().map { it.toDto() },
            )
        }
        val file = BackupFile(
            backupFormatVersion = BackupConstants.FORMAT_VERSION,
            exportedAt = TimeUtils.now(),
            appVersion = BuildConfig.VERSION_NAME,
            data = data,
        )
        json.encodeToString(BackupFile.serializer(), file).toByteArray(Charsets.UTF_8)
    }

    /** 构建 CSV 内容（UTF-8 BOM）。 */
    suspend fun buildCsv(): ByteArray = withContext(Dispatchers.IO) {
        val ledgers = database.ledgerDao().getAll().associateBy { it.id }
        val accounts = database.accountDao().getAll().associateBy { it.id }
        val categories = database.categoryDao().getAll().associateBy { it.id }
        val merchants = database.merchantDao().getAll().associateBy { it.id }
        val tagLinks = database.transactionTagDao().getAll().groupBy({ it.transactionId }, { it.tagId })
        val tags = database.tagDao().getAll().associateBy { it.id }

        val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
        val sb = StringBuilder()
        sb.append("账本,类型,金额,币种,账户,转入账户,分类,商家,标签,备注,时间\n")

        database.transactionDao().getAll()
            .filter { !it.isDeleted } // CSV 不包含已删除交易
            .sortedBy { it.transactionTime }
            .forEach { tx ->
                val typeLabel = when (tx.type) {
                    TransactionType.EXPENSE -> "支出"
                    TransactionType.INCOME -> "收入"
                    TransactionType.TRANSFER -> "转账"
                }
                val amountText = BigDecimal.valueOf(tx.amount, 2).toPlainString()
                val tagNames = (tagLinks[tx.id].orEmpty())
                    .mapNotNull { tags[it]?.name }
                    .joinToString("|")
                val row = listOf(
                    ledgers[tx.ledgerId]?.name.orEmpty(),
                    typeLabel,
                    amountText,
                    tx.currency,
                    accounts[tx.accountId]?.name.orEmpty(),
                    tx.targetAccountId?.let { accounts[it]?.name }.orEmpty(),
                    tx.categoryId?.let { categories[it]?.name }.orEmpty(),
                    tx.merchantId?.let { merchants[it]?.name }.orEmpty(),
                    tagNames,
                    tx.note.orEmpty(),
                    timeFmt.format(Instant.ofEpochMilli(tx.transactionTime)),
                )
                sb.append(Csv.row(row)).append('\n')
            }
        // UTF-8 BOM（Excel 直接打开中文）
        val content = sb.toString().toByteArray(Charsets.UTF_8)
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        bom + content
    }

    /** SQLite 完整备份：checkpoint 后拷贝数据库文件。 */
    suspend fun copyDatabaseFileTo(dest: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
            val source = context.getDatabasePath("ledger.db")
            if (!source.exists()) throw IllegalStateException("数据库文件不存在")
            source.copyTo(dest, overwrite = true)
            Unit
        }
    }

    // ---------- 恢复 ----------

    /**
     * 恢复 JSON：整库覆盖（清空后按依赖顺序插入），不做合并。
     * 恢复前自动备份当前数据。
     */
    suspend fun restoreJson(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = json.decodeFromString(BackupFile.serializer(), bytes.toString(Charsets.UTF_8))
            if (backup.backupFormatVersion > BackupConstants.FORMAT_VERSION) {
                throw IllegalStateException("备份格式版本过高（${backup.backupFormatVersion}）")
            }
            backupBeforeRestoreOrThrow()
            database.withTransaction {
                database.transactionTagDao().deleteAll()
                database.transactionDao().deleteAll()
                database.budgetDao().deleteAll()
                database.merchantDao().deleteAll()
                database.tagDao().deleteAll()
                database.categoryDao().deleteAll()
                database.accountDao().deleteAll()
                database.ledgerDao().deleteAll()

                backup.data.ledgers.forEach { database.ledgerDao().insert(it.toEntity()) }
                backup.data.accounts.forEach { database.accountDao().insert(it.toEntity()) }
                backup.data.categories.forEach { database.categoryDao().insert(it.toEntity()) }
                backup.data.merchants.forEach { database.merchantDao().insert(it.toEntity()) }
                backup.data.tags.forEach { database.tagDao().insert(it.toEntity()) }
                backup.data.transactions.forEach { database.transactionDao().insert(it.toEntity()) }
                backup.data.transactionTags.forEach { database.transactionTagDao().insertAll(listOf(it.toEntity())) }
                backup.data.budgets.forEach { database.budgetDao().insert(it.toEntity()) }
            }
        }
    }

    /** 恢复 SQLite：整库文件替换。调用后需重启应用生效。 */
    suspend fun restoreSqlite(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val candidate = File.createTempFile("ledger-restore-candidate-", ".db", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    candidate.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("无法读取备份文件")
                validateSqliteBackup(candidate)
                backupBeforeRestoreOrThrow()

                val dbFile = context.getDatabasePath("ledger.db")
                val staged = File(dbFile.parentFile, "${dbFile.name}.restore-new")
                val rollback = File(dbFile.parentFile, "${dbFile.name}.restore-old")
                candidate.copyTo(staged, overwrite = true)

                AppDatabase.closeInstance()
                rollback.delete()
                if (dbFile.exists()) moveReplacing(dbFile, rollback)
                try {
                    moveReplacing(staged, dbFile)
                    File(dbFile.path + "-wal").delete()
                    File(dbFile.path + "-shm").delete()
                    rollback.delete()
                } catch (e: Exception) {
                    dbFile.delete()
                    if (rollback.exists()) moveReplacing(rollback, dbFile)
                    throw e
                } finally {
                    staged.delete()
                }
            } finally {
                candidate.delete()
            }
            Unit
        }
    }

    // ---------- 自动备份 ----------

    /** 自动备份到 SAF 目录并执行轮换；返回是否成功。 */
    suspend fun runAutoBackup(dirUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return@runCatching false
            val now = java.time.LocalDateTime.now()
            val name = BackupRotation.fileName(now)
            val jsonBytes = buildBackupJson()
            val file = dir.createFile(BackupConstants.MIME_JSON, name)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(jsonBytes) }
                ?: return@runCatching false

            // 轮换：7 每日 + 4 每周 + 12 每月
            val names = dir.listFiles().mapNotNull { it.name }
                .filter { it.startsWith(BackupConstants.FILE_PREFIX) && it.endsWith(".json") }
            BackupRotation.selectFilesToDelete(names, java.time.LocalDate.now())
                .forEach { n -> dir.findFile(n)?.delete() }
            true
        }.getOrDefault(false)
    }

    /** 恢复前自动备份当前数据：优先写入用户选择的备份目录，否则写入应用 filesDir。 */
    private suspend fun backupBeforeRestoreOrThrow() {
        val jsonBytes = buildBackupJson()
        val name = BackupRotation.fileName(java.time.LocalDateTime.now())
        val backupDirUri = settings.backupDirUri.first()
        val savedToUserDirectory = if (backupDirUri != null) {
            runCatching {
                val dir = DocumentFile.fromTreeUri(context, Uri.parse(backupDirUri))
                val file = dir?.createFile(BackupConstants.MIME_JSON, "pre-restore-$name")
                    ?: return@runCatching false
                context.contentResolver.openOutputStream(file.uri)?.use { it.write(jsonBytes) } != null
            }.getOrDefault(false)
        } else {
            false
        }
        if (!savedToUserDirectory) {
            val dir = File(context.filesDir, "pre-restore-backups")
            if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("无法创建恢复前备份目录")
            File(dir, "pre-restore-$name").writeBytes(jsonBytes)
        }
    }

    private fun validateSqliteBackup(file: File) {
        if (file.length() == 0L) throw IllegalStateException("备份文件为空")
        val sqlite = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val quickCheck = sqlite.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (quickCheck != "ok") throw IllegalStateException("SQLite 备份完整性校验失败")
            val version = sqlite.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            if (version !in 1..DATABASE_VERSION) {
                throw IllegalStateException("不支持的数据库版本：$version")
            }
            val tables = sqlite.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table'",
                null,
            ).use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }
            val required = setOf(
                "ledger", "account", "category", "merchant", "tag",
                "transaction", "transaction_tag", "budget",
            )
            if (!tables.containsAll(required)) throw IllegalStateException("SQLite 备份缺少必要数据表")
        } finally {
            sqlite.close()
        }
    }

    private fun moveReplacing(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        /** 恢复（SQLite）后重启应用使新数据库生效。 */
        fun restartApp(context: Context) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }
}
