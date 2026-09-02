package com.personal.ledger.data.backup

import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.BudgetEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.data.local.entity.TagEntity
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.data.local.entity.TransactionTagEntity
import kotlinx.serialization.Serializable

/** 备份格式常量。格式版本用于兼容性判断（ARCHITECTURE.md 第 8 节）。 */
object BackupConstants {
    const val FORMAT_VERSION = 1
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
    val tags: List<TagDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val transactionTags: List<TransactionTagDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
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

@Serializable
data class AccountDto(
    val id: String,
    val ledgerId: String,
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

// ---------- Entity ↔ DTO 映射 ----------

fun LedgerEntity.toDto() = LedgerDto(id, name, baseCurrency, createdAt, updatedAt, isArchived)
fun AccountEntity.toDto() = AccountDto(id, ledgerId, name, type.name, currency, initialBalance, initialBalanceTime, createdAt, updatedAt, isArchived)
fun CategoryEntity.toDto() = CategoryDto(id, ledgerId, name, type.name, parentId, sortOrder, isArchived)
fun MerchantEntity.toDto() = MerchantDto(id, ledgerId, name, createdAt, isArchived)
fun TagEntity.toDto() = TagDto(id, ledgerId, name, createdAt, isArchived)
fun TransactionEntity.toDto() = TransactionDto(id, ledgerId, type.name, amount, currency, accountId, targetAccountId, categoryId, merchantId, note, transactionTime, createdAt, updatedAt, isDeleted)
fun TransactionTagEntity.toDto() = TransactionTagDto(transactionId, tagId)
fun BudgetEntity.toDto() = BudgetDto(id, ledgerId, month, categoryId, amount, currency, createdAt, updatedAt)

fun LedgerDto.toEntity() = LedgerEntity(id, name, baseCurrency, createdAt, updatedAt, isArchived)
fun AccountDto.toEntity() = AccountEntity(
    id, ledgerId, name,
    com.personal.ledger.domain.model.AccountType.valueOf(type),
    currency, initialBalance, initialBalanceTime, createdAt, updatedAt, isArchived,
)
fun CategoryDto.toEntity() = CategoryEntity(
    id, ledgerId, name,
    com.personal.ledger.domain.model.CategoryType.valueOf(type),
    parentId, sortOrder, isArchived,
)
fun MerchantDto.toEntity() = MerchantEntity(id, ledgerId, name, createdAt, isArchived)
fun TagDto.toEntity() = TagEntity(id, ledgerId, name, createdAt, isArchived)
fun TransactionDto.toEntity() = TransactionEntity(
    id, ledgerId,
    com.personal.ledger.domain.model.TransactionType.valueOf(type),
    amount, currency, accountId, targetAccountId, categoryId, merchantId, note,
    transactionTime, createdAt, updatedAt, isDeleted,
)
fun TransactionTagDto.toEntity() = TransactionTagEntity(transactionId, tagId)
fun BudgetDto.toEntity() = BudgetEntity(id, ledgerId, month, categoryId, amount, currency, createdAt, updatedAt)
