package com.personal.ledger.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import com.personal.ledger.BuildConfig
import com.personal.ledger.R
import com.personal.ledger.data.StoredMessage
import com.personal.ledger.data.StoredMessages
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.database.DATABASE_VERSION
import com.personal.ledger.data.local.database.RecurringDatabaseGate
import com.personal.ledger.data.repository.CategoryIconKeys
import com.personal.ledger.data.repository.toDomain
import com.personal.ledger.data.settings.SettingsRepository
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.Money
import com.personal.ledger.util.LocaleContext
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.util.UserFacingException
import com.personal.ledger.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val stores: BackupStores = BackupStores(context),
) {

    /** 立即备份与自动备份互斥，避免同一秒生成同名文件或并发轮换。 */
    private val backupMutex = Mutex()

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
                merchantCategories = database.merchantCategoryDao().getAll().map { it.toDto() },
                tags = database.tagDao().getAll().map { it.toDto() },
                transactions = database.transactionDao().getAll().map { it.toDto() },
                transactionTags = database.transactionTagDao().getAll().map { it.toDto() },
                budgets = database.budgetDao().getAll().map { it.toDto() },
                recurringCharges = database.recurringChargeDao().getAllCharges().map { it.toDto() },
                recurringOccurrences = database.recurringChargeDao().getAllOccurrences().map { it.toDto() },
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
        // 表头与类型按导出时的应用语言生成
        val res = LocaleContext.of(context)
        val expenseLabel = res.getString(R.string.type_expense)
        val incomeLabel = res.getString(R.string.type_income)
        val transferLabel = res.getString(R.string.type_transfer)
        val sb = StringBuilder()
        sb.append(res.getString(R.string.data_csv_header)).append('\n')

        database.transactionDao().getAll()
            .filter { !it.isDeleted } // CSV 不包含已删除交易
            .sortedBy { it.transactionTime }
            .forEach { tx ->
                val typeLabel = when (tx.type) {
                    TransactionType.EXPENSE -> expenseLabel
                    TransactionType.INCOME -> incomeLabel
                    TransactionType.TRANSFER -> transferLabel
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
            if (!source.exists()) throw UserFacingException(R.string.err_db_file_missing)
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
            if (backup.backupFormatVersion !in 1..BackupConstants.FORMAT_VERSION) {
                throw UserFacingException(R.string.err_backup_format_version, backup.backupFormatVersion)
            }
            validateRecurringBackup(backup.data)
            val merchantCategories = if (backup.backupFormatVersion < 3) {
                deriveLegacyMerchantCategories(backup.data)
            } else {
                backup.data.merchantCategories
            }
            validateMerchantCategories(backup.data, merchantCategories)
            RecurringDatabaseGate.mutex.withLock {
                backupBeforeRestoreOrThrow()
                database.withTransaction {
                    database.recurringChargeDao().deleteAllOccurrences()
                    database.recurringChargeDao().deleteAllCharges()
                    database.transactionTagDao().deleteAll()
                    database.transactionDao().deleteAll()
                    database.budgetDao().deleteAll()
                    database.merchantCategoryDao().deleteAll()
                    database.merchantDao().deleteAll()
                    database.tagDao().deleteAll()
                    database.categoryDao().deleteAll()
                    database.accountDao().deleteAll()
                    database.ledgerDao().deleteAll()

                    backup.data.ledgers.forEach { database.ledgerDao().insert(it.toEntity()) }
                    backup.data.accounts.forEach { database.accountDao().insert(it.toEntity()) }
                    // 图标仅用于显示：非法 key 不阻断恢复，回退为“自动”（按名称匹配）。
                    backup.data.categories.forEach {
                        val icon = it.iconKey?.trim()?.takeIf(CategoryIconKeys::isValid)
                        database.categoryDao().insert(it.copy(iconKey = icon).toEntity())
                    }
                    backup.data.merchants.forEach { database.merchantDao().insert(it.toEntity()) }
                    merchantCategories.forEach { database.merchantCategoryDao().insert(it.toEntity()) }
                    backup.data.tags.forEach { database.tagDao().insert(it.toEntity()) }
                    backup.data.transactions.forEach { database.transactionDao().insert(it.toEntity()) }
                    backup.data.transactionTags.forEach { database.transactionTagDao().insertAll(listOf(it.toEntity())) }
                    backup.data.budgets.forEach { database.budgetDao().insert(it.toEntity()) }
                    backup.data.recurringCharges.forEach { database.recurringChargeDao().insert(it.toEntity()) }
                    backup.data.recurringOccurrences.forEach {
                        database.recurringChargeDao().insertOccurrence(it.toEntity())
                    }
                }
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
                } ?: throw UserFacingException(R.string.err_backup_read)
                validateSqliteBackup(candidate)
                RecurringDatabaseGate.mutex.withLock {
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
                }
            } finally {
                candidate.delete()
            }
            Unit
        }
    }

    // ---------- 备份位置 / 立即备份 / 自动备份 ----------

    /** 当前备份位置（默认目录或自定义文件夹）及其可用状态。 */
    suspend fun currentLocation(): BackupLocationState = stores.resolve(settings.backupDirUri.first())

    /** 使用自定义 SAF 文件夹：持久化读写授权后保存。 */
    suspend fun useCustomFolder(treeUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val previous = settings.backupDirUri.first()
            settings.setBackupDirUri(treeUri.toString())
            settings.recordBackupResult(succeededAt = null, warning = null)
            if (previous != null && previous != treeUri.toString()) stores.customStore(previous).releasePermission()
        }
    }

    /** 恢复为默认目录：清除自定义文件夹并释放其授权（文件夹中的已有文件不受影响）。 */
    suspend fun useDefaultFolder() {
        settings.backupDirUri.first()?.let { stores.customStore(it).releasePermission() }
        settings.setBackupDirUri(null)
        settings.recordBackupResult(succeededAt = null, warning = null)
    }

    /**
     * 立即备份：把完整 JSON 备份写入当前备份位置（无需选择器），随后执行轮换。
     * 自动备份 Worker 也走这里。
     *
     * 自定义文件夹授权丢失 / 写入失败时回退写入默认目录，并记录警告供界面提示；
     * 不会静默清除用户的自定义设置，由用户决定重新选择或恢复默认。
     */
    suspend fun backupNow(): Result<BackupRunResult> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            val result = runCatching {
                val location = currentLocation()
                val bytes = buildBackupJson()
                val name = BackupRotation.fileName(java.time.LocalDateTime.now())
                var store = if (location.customUnavailable) stores.defaultStore() else location.store
                var fellBack = location.customUnavailable
                val entry = try {
                    store.write(name, bytes)
                } catch (e: Exception) {
                    if (!location.isCustom || fellBack) throw e
                    store = stores.defaultStore()
                    fellBack = true
                    store.write(name, bytes)
                }
                val rotated = runCatching { rotate(store) }.getOrDefault(0)
                BackupRunResult(entry, store.describe(), fellBack, rotated)
            }
            result.onSuccess {
                settings.recordBackupResult(
                    succeededAt = TimeUtils.now(),
                    warning = if (it.fellBackToDefault) CUSTOM_FOLDER_LOST_WARNING else null,
                )
            }.onFailure {
                settings.recordBackupResult(
                    succeededAt = null,
                    warning = StoredMessages.encode(
                        StoredMessage.BACKUP_FAILED,
                        StoredMessages.encode(it, StoredMessage.BACKUP_UNKNOWN_ERROR),
                    ),
                )
            }
            result
        }
    }

    /** 自动备份入口（WorkManager）；返回是否成功。 */
    suspend fun runAutoBackup(): Boolean = backupNow().isSuccess

    /** 轮换：只考虑严格命名的 ledger-backup-*.json，恢复前备份与其他文件不会被删除。返回删除数量。 */
    private suspend fun rotate(store: BackupStore): Int {
        val entries = store.list()
        val toDelete = BackupRotation.selectFilesToDelete(
            BackupCatalog.rotationCandidates(entries),
            java.time.LocalDate.now(),
        ).toSet()
        return entries.filter { it.name in toDelete }.count { store.delete(it) }
    }

    /** 列出当前位置中的备份与应用内部的恢复前备份（均为从新到旧）。 */
    suspend fun listBackups(): BackupListing = withContext(Dispatchers.IO) {
        val location = currentLocation()
        val storeResult = if (location.customUnavailable) {
            Result.failure(UserFacingException(R.string.err_custom_folder_inaccessible))
        } else {
            runCatching { BackupCatalog.visibleNewestFirst(location.store.list()) }
        }
        val internal = stores.internalPreRestoreStore()
        val internalEntries = runCatching {
            BackupCatalog.visibleNewestFirst(internal.list())
        }.getOrDefault(emptyList())
        BackupListing(
            location = location,
            entries = storeResult.getOrDefault(emptyList()),
            listError = storeResult.exceptionOrNull()?.userMessage(LocaleContext.of(context)),
            internalStore = internal,
            internalEntries = internalEntries,
        )
    }

    /** 把备份文件另存到用户选择的位置（SAF CreateDocument）。 */
    suspend fun copyBackupTo(store: BackupStore, entry: BackupEntry, target: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                store.open(entry).use { input ->
                    context.contentResolver.openOutputStream(target, "w")?.use { output -> input.copyTo(output) }
                        ?: throw UserFacingException(R.string.err_write_target)
                }
                Unit
            }
        }

    /**
     * 从任意 URI 恢复（content:// 或 file://）：按文件名或首字节判断 JSON / SQLite。
     * JSON 恢复后调用方需回退无效的当前账本；SQLite 恢复后需重启应用。
     */
    suspend fun restoreFromUri(uri: Uri, displayName: String? = null): Result<RestoreOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = displayName ?: uri.lastPathSegment.orEmpty()
                val isJson = name.endsWith(".json", ignoreCase = true) ||
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val buf = ByteArray(1)
                            input.read(buf) >= 0 && buf[0] == '{'.code.toByte()
                        } == true
                    }.getOrDefault(false)
                if (isJson) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw UserFacingException(R.string.err_backup_read)
                    restoreJson(bytes).getOrThrow()
                    RestoreOutcome.JSON_RESTORED
                } else {
                    restoreSqlite(uri).getOrThrow()
                    RestoreOutcome.SQLITE_RESTART_REQUIRED
                }
            }
        }

    /**
     * 恢复前自动备份当前数据：优先写入当前备份位置（默认目录或可用的自定义文件夹），
     * 失败时写入应用内部 filesDir/pre-restore-backups。
     */
    private suspend fun backupBeforeRestoreOrThrow() {
        val jsonBytes = buildBackupJson()
        val name = BackupCatalog.preRestoreFileName(java.time.LocalDateTime.now())
        val savedToStore = runCatching {
            val location = currentLocation()
            if (location.customUnavailable) {
                false
            } else {
                location.store.write(name, jsonBytes)
                true
            }
        }.getOrDefault(false)
        if (!savedToStore) {
            stores.internalPreRestoreStore().write(name, jsonBytes)
        }
    }

    private fun validateSqliteBackup(file: File) {
        if (file.length() == 0L) throw UserFacingException(R.string.err_backup_empty)
        val sqlite = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val quickCheck = sqlite.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (quickCheck != "ok") throw UserFacingException(R.string.err_sqlite_integrity)
            val version = sqlite.rawQuery("PRAGMA user_version", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            if (version !in 1..DATABASE_VERSION) {
                throw UserFacingException(R.string.err_db_version, version)
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
            if (!tables.containsAll(required)) throw UserFacingException(R.string.err_sqlite_missing_tables)
            if (version >= 2 && !tables.containsAll(setOf("recurring_charge", "recurring_occurrence"))) {
                throw UserFacingException(R.string.err_sqlite_v2_missing)
            }
            if (version >= 3 && "merchant_category" !in tables) {
                throw UserFacingException(R.string.err_sqlite_v3_missing)
            }
            if (version >= 4) {
                val categoryColumns = sqlite.rawQuery("PRAGMA table_info(`category`)", null).use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(nameIndex)) }
                }
                if ("iconKey" !in categoryColumns) {
                    throw UserFacingException(R.string.err_sqlite_v4_missing)
                }
            }
            // v1–v3 备份替换后由 Room 在下次打开时按显式 Migration 升级到当前版本。
        } finally {
            sqlite.close()
        }
    }

    private fun validateRecurringBackup(data: BackupData) {
        val charges = data.recurringCharges.map { it.toEntity() }
        val chargeIds = charges.map { it.id }.toSet()
        if (chargeIds.size != charges.size) throw UserFacingException(R.string.err_backup_recurring_dup_id)
        val ledgers = data.ledgers.associateBy { it.id }
        val accounts = data.accounts.associateBy { it.id }
        val categories = data.categories.associateBy { it.id }
        val transactionIds = data.transactions.map { it.id }.toSet()
        if (transactionIds.size != data.transactions.size) throw UserFacingException(R.string.err_backup_tx_dup_id)
        charges.forEach { charge ->
            val domain = try {
                charge.toDomain()
            } catch (_: Exception) {
                throw UserFacingException(R.string.err_backup_recurring_invalid_rule)
            }
            if (charge.amount <= 0) throw UserFacingException(R.string.err_backup_recurring_amount)
            if (charge.minuteOfDay !in 0..1439) throw UserFacingException(R.string.err_backup_recurring_time)
            if (charge.nextOccurrenceIndex < 0) throw UserFacingException(R.string.err_backup_recurring_index)
            val expected = try {
                com.personal.ledger.domain.calc.RecurringSchedule.dueAt(
                    domain.startDate,
                    domain.minuteOfDay,
                    domain.zoneId,
                    domain.frequency,
                    domain.nextOccurrenceIndex,
                )
            } catch (_: Exception) {
                throw UserFacingException(R.string.err_backup_recurring_date_range)
            }
            if (expected != charge.nextDueAt) throw UserFacingException(R.string.err_backup_recurring_date_mismatch)
            val ledger = ledgers[charge.ledgerId]
                ?: throw UserFacingException(R.string.err_backup_recurring_ledger)
            val account = accounts[charge.accountId]
                ?: throw UserFacingException(R.string.err_backup_recurring_account)
            val category = categories[charge.categoryId]
                ?: throw UserFacingException(R.string.err_backup_recurring_category)
            if (account.ledgerId != charge.ledgerId || account.currency != ledger.baseCurrency) {
                throw UserFacingException(R.string.err_backup_recurring_account_mismatch)
            }
            if (charge.currency != ledger.baseCurrency) throw UserFacingException(R.string.err_backup_recurring_currency)
            if (category.ledgerId != charge.ledgerId || category.type != "EXPENSE") {
                throw UserFacingException(R.string.err_backup_recurring_expense)
            }
        }
        val occurrenceKeys = mutableSetOf<Pair<String, Long>>()
        val occurrenceTransactionIds = mutableSetOf<String>()
        data.recurringOccurrences.forEach { occurrence ->
            if (occurrence.ruleId !in chargeIds) throw UserFacingException(R.string.err_backup_occurrence_rule)
            if (occurrence.transactionId !in transactionIds) throw UserFacingException(R.string.err_backup_occurrence_tx)
            val charge = charges.first { it.id == occurrence.ruleId }
            val transaction = data.transactions.first { it.id == occurrence.transactionId }
            if (transaction.ledgerId != charge.ledgerId) {
                throw UserFacingException(R.string.err_backup_occurrence_ledger)
            }
            if (!occurrenceKeys.add(occurrence.ruleId to occurrence.dueAt)) {
                throw UserFacingException(R.string.err_backup_occurrence_dup)
            }
            if (!occurrenceTransactionIds.add(occurrence.transactionId)) {
                throw UserFacingException(R.string.err_backup_occurrence_tx_dup)
            }
        }
    }

    private fun validateMerchantCategories(data: BackupData, links: List<MerchantCategoryDto>) {
        val merchants = data.merchants.associateBy { it.id }
        val categories = data.categories.associateBy { it.id }
        if (links.map { it.merchantId to it.categoryId }.toSet().size != links.size) {
            throw UserFacingException(R.string.err_backup_merchant_link_dup)
        }
        links.forEach { link ->
            val merchant = merchants[link.merchantId]
                ?: throw UserFacingException(R.string.err_backup_merchant_link_merchant)
            val category = categories[link.categoryId]
                ?: throw UserFacingException(R.string.err_backup_merchant_link_category)
            if (merchant.ledgerId != category.ledgerId || category.parentId != null ||
                category.type !in setOf("EXPENSE", "INCOME")
            ) {
                throw UserFacingException(R.string.err_backup_merchant_link_root)
            }
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
        /** 存入设置的警告代码；[SettingsRepository.lastBackupWarning] 读取时按当前语言解析。 */
        val CUSTOM_FOLDER_LOST_WARNING: String = StoredMessages.encode(StoredMessage.BACKUP_CUSTOM_FOLDER_LOST)

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

/** 一次立即 / 自动备份的结果。 */
data class BackupRunResult(
    val entry: BackupEntry,
    val locationLabel: String,
    /** 自定义文件夹不可用，本次已回退写入默认目录。 */
    val fellBackToDefault: Boolean,
    /** 轮换删除的旧备份数量。 */
    val rotatedCount: Int,
)

/** 备份管理页的列表数据。 */
data class BackupListing(
    val location: BackupLocationState,
    val entries: List<BackupEntry>,
    /** 列出失败的原因（已按列出时的应用语言解析）。 */
    val listError: String?,
    val internalStore: BackupStore,
    val internalEntries: List<BackupEntry>,
)

enum class RestoreOutcome {
    /** JSON 已恢复，需回退无效的当前账本。 */
    JSON_RESTORED,

    /** SQLite 文件已替换，需重启应用。 */
    SQLITE_RESTART_REQUIRED,
}
