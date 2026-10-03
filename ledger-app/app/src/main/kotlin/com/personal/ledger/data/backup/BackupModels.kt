package com.personal.ledger.data.backup

import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.BudgetEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.data.local.entity.MerchantCategoryEntity
import com.personal.ledger.data.local.entity.TagEntity
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.data.local.entity.TransactionTagEntity
import com.personal.ledger.data.local.entity.RecurringChargeEntity
import com.personal.ledger.data.local.entity.RecurringOccurrenceEntity
import com.personal.ledger.domain.calc.SharedAccountNames
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 备份格式常量。格式版本用于兼容性判断（ARCHITECTURE.md 第 8 节）。 */
object BackupConstants {
    const val FORMAT_VERSION = 5
    const val FILE_PREFIX = "ledger-backup-"
    const val MIME_JSON = "application/json"
    const val MIME_CSV = "text/csv"
    const val MIME_SQLITE = "application/octet-stream"
}

/**
 * JSON 完整备份：包含所有账本数据（含软删除交易）。
 * App 设置（主题、安全锁、当前账本、备份目录等）不参与备份与恢复。
 */
@Serializable
data class BackupFile(
    val backupFormatVersion: Int = BackupConstants.FORMAT_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val ledgers: List<LedgerDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val merchants: List<MerchantDto> = emptyList(),
    /** Missing in v1/v2 backups; old relationships are inferred from transactions on restore. */
    val merchantCategories: List<MerchantCategoryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val transactionTags: List<TransactionTagDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    /** Missing in v1 backups; default-empty keeps those files readable. */
    val recurringCharges: List<RecurringChargeDto> = emptyList(),
    val recurringOccurrences: List<RecurringOccurrenceDto> = emptyList(),
)

@Serializable
data class LedgerDto(
    val id: String,
    val name: String,
    val baseCurrency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

/**
 * v5 起账户为全局实体，不再写出 `ledgerId`；读取 v1–v4 备份时保留旧归属，
 * 仅用于恢复时按 [com.personal.ledger.domain.calc.SharedAccountNames] 处理跨账本同名。
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AccountDto(
    val id: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val ledgerId: String? = null,
    val name: String,
    val type: String,
    val currency: String,
    val initialBalance: Long,
    val initialBalanceTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

@Serializable
data class CategoryDto(
    val id: String,
    val ledgerId: String,
    val name: String,
    val type: String,
    val parentId: String? = null,
    val sortOrder: Int,
    val isArchived: Boolean,
    /** v4 新增：图标库 key。v1–v3 备份缺省为 null（按名称自动匹配图标）。 */
    val iconKey: String? = null,
)

@Serializable
data class MerchantDto(
    val id: String,
    val ledgerId: String,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean,
)

@Serializable
data class MerchantCategoryDto(
    val merchantId: String,
    val categoryId: String,
)

@Serializable
data class TagDto(
    val id: String,
    val ledgerId: String,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean,
)

@Serializable
data class TransactionDto(
    val id: String,
    val ledgerId: String,
    val type: String,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val targetAccountId: String? = null,
    val categoryId: String? = null,
    val merchantId: String? = null,
    val note: String? = null,
    val transactionTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

@Serializable
data class TransactionTagDto(
    val transactionId: String,
    val tagId: String,
)

@Serializable
data class BudgetDto(
    val id: String,
    val ledgerId: String,
    val month: String,
    val categoryId: String? = null,
    val amount: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class RecurringChargeDto(
    val id: String,
    val ledgerId: String,
    val name: String,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val categoryId: String,
    val frequency: String,
    val startDate: String,
    val minuteOfDay: Int,
    val zoneId: String,
    val nextOccurrenceIndex: Long,
    val nextDueAt: Long,
    val isPaused: Boolean,
    val lastError: String? = null,
    val updatedAt: Long,
)

@Serializable
data class RecurringOccurrenceDto(
    val ruleId: String,
    val dueAt: Long,
    val transactionId: String,
)

// ---------- Entity ↔ DTO 映射 ----------

fun LedgerEntity.toDto() = LedgerDto(id, name, baseCurrency, createdAt, updatedAt, isArchived)
fun AccountEntity.toDto() = AccountDto(
    id = id, name = name, type = type.name, currency = currency, initialBalance = initialBalance,
    initialBalanceTime = initialBalanceTime, createdAt = createdAt, updatedAt = updatedAt, isArchived = isArchived,
)
fun CategoryEntity.toDto() = CategoryDto(id, ledgerId, name, type.name, parentId, sortOrder, isArchived, iconKey)
fun MerchantEntity.toDto() = MerchantDto(id, ledgerId, name, createdAt, isArchived)
fun MerchantCategoryEntity.toDto() = MerchantCategoryDto(merchantId, categoryId)
fun TagEntity.toDto() = TagDto(id, ledgerId, name, createdAt, isArchived)
fun TransactionEntity.toDto() = TransactionDto(id, ledgerId, type.name, amount, currency, accountId, targetAccountId, categoryId, merchantId, note, transactionTime, createdAt, updatedAt, isDeleted)
fun TransactionTagEntity.toDto() = TransactionTagDto(transactionId, tagId)
fun BudgetEntity.toDto() = BudgetDto(id, ledgerId, month, categoryId, amount, currency, createdAt, updatedAt)
fun RecurringChargeEntity.toDto() = RecurringChargeDto(
    id, ledgerId, name, amount, currency, accountId, categoryId, frequency, startDate, minuteOfDay,
    zoneId, nextOccurrenceIndex, nextDueAt, isPaused, lastError, updatedAt,
)
fun RecurringOccurrenceEntity.toDto() = RecurringOccurrenceDto(ruleId, dueAt, transactionId)

fun LedgerDto.toEntity() = LedgerEntity(id, name, baseCurrency, createdAt, updatedAt, isArchived)
fun AccountDto.toEntity() = AccountEntity(
    id, name,
    com.personal.ledger.domain.model.AccountType.valueOf(type),
    currency, initialBalance, initialBalanceTime, createdAt, updatedAt, isArchived,
)
fun CategoryDto.toEntity() = CategoryEntity(
    id, ledgerId, name,
    com.personal.ledger.domain.model.CategoryType.valueOf(type),
    parentId, sortOrder, isArchived, iconKey,
)
fun MerchantDto.toEntity() = MerchantEntity(id, ledgerId, name, createdAt, isArchived)
fun MerchantCategoryDto.toEntity() = MerchantCategoryEntity(merchantId, categoryId)
fun TagDto.toEntity() = TagEntity(id, ledgerId, name, createdAt, isArchived)
fun TransactionDto.toEntity() = TransactionEntity(
    id, ledgerId,
    com.personal.ledger.domain.model.TransactionType.valueOf(type),
    amount, currency, accountId, targetAccountId, categoryId, merchantId, note,
    transactionTime, createdAt, updatedAt, isDeleted,
)
fun TransactionTagDto.toEntity() = TransactionTagEntity(transactionId, tagId)
fun BudgetDto.toEntity() = BudgetEntity(id, ledgerId, month, categoryId, amount, currency, createdAt, updatedAt)
fun RecurringChargeDto.toEntity() = RecurringChargeEntity(
    id, ledgerId, name, amount, currency, accountId, categoryId, frequency, startDate, minuteOfDay,
    zoneId, nextOccurrenceIndex, nextDueAt, isPaused, lastError, updatedAt,
)
fun RecurringOccurrenceDto.toEntity() = RecurringOccurrenceEntity(ruleId, dueAt, transactionId)

/** Reconstruct libraries from old backups without assigning every old merchant to every category. */
internal fun deriveLegacyMerchantCategories(data: BackupData): List<MerchantCategoryDto> {
    val merchants = data.merchants.associateBy { it.id }
    val categories = data.categories.associateBy { it.id }
    return data.transactions.mapNotNull { transaction ->
        val merchant = transaction.merchantId?.let { merchants[it] } ?: return@mapNotNull null
        val category = transaction.categoryId?.let { categories[it] } ?: return@mapNotNull null
        if (transaction.type !in setOf("EXPENSE", "INCOME") ||
            transaction.type != category.type ||
            merchant.ledgerId != transaction.ledgerId ||
            category.ledgerId != transaction.ledgerId
        ) return@mapNotNull null
        val root = category.parentId?.let { categories[it] } ?: category
        if (root.ledgerId != transaction.ledgerId || root.parentId != null ||
            root.type != category.type
        ) return@mapNotNull null
        MerchantCategoryDto(merchant.id, root.id)
    }.distinct()
}

/**
 * 旧版备份（格式 1–4）中的账户属于各自账本；恢复为全局账户时，
 * 不同账本中的同名账户按与 Room 迁移 4→5 相同的规则改名为 `名称（账本名）`。
 */
internal fun sharedAccountsFromLegacy(data: BackupData): List<AccountDto> {
    val renames = SharedAccountNames.renames(
        accounts = data.accounts.map { SharedAccountNames.AccountRef(it.id, it.ledgerId, it.name, it.createdAt) },
        ledgers = data.ledgers.map { SharedAccountNames.LedgerRef(it.id, it.name, it.createdAt) },
    )
    return data.accounts.map { dto -> renames[dto.id]?.let { dto.copy(name = it) } ?: dto }
}
