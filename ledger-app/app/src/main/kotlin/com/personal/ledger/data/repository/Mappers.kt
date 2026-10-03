package com.personal.ledger.data.repository

import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.BudgetEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.data.local.entity.TagEntity
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Budget
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.Ledger
import com.personal.ledger.domain.model.Merchant
import com.personal.ledger.domain.model.Tag
import com.personal.ledger.domain.model.Transaction

/** Room Entity 与业务 Model 的映射（架构要求两者分离）。 */

fun LedgerEntity.toDomain() = Ledger(
    id = id,
    name = name,
    baseCurrency = baseCurrency,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
)

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = type,
    currency = currency,
    initialBalance = initialBalance,
    initialBalanceTime = initialBalanceTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    ledgerId = ledgerId,
    type = type,
    amount = amount,
    currency = currency,
    accountId = accountId,
    targetAccountId = targetAccountId,
    categoryId = categoryId,
    merchantId = merchantId,
    note = note,
    transactionTime = transactionTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    ledgerId = ledgerId,
    name = name,
    type = type,
    parentId = parentId,
    sortOrder = sortOrder,
    isArchived = isArchived,
    iconKey = iconKey,
)

fun MerchantEntity.toDomain() = Merchant(
    id = id,
    ledgerId = ledgerId,
    name = name,
    createdAt = createdAt,
    isArchived = isArchived,
)

fun TagEntity.toDomain() = Tag(
    id = id,
    ledgerId = ledgerId,
    name = name,
    createdAt = createdAt,
    isArchived = isArchived,
)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    ledgerId = ledgerId,
    month = month,
    categoryId = categoryId,
    amount = amount,
    currency = currency,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
