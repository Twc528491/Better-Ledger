package com.personal.ledger.domain.model

/** 账本。单个账本仅允许一种币种（V1）。 */
data class Ledger(
    val id: String,
    val name: String,
    val baseCurrency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

/**
 * 账户。全局实体，所有账本共享：某账本可使用币种等于其本位币的账户；
 * initialBalanceTime 之前的交易在 V1 中被禁止。
 */
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalance: Long,
    val initialBalanceTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
)

/** 账户 + 由原始交易（全部账本）计算得出的当前余额（最小货币单位）。 */
data class AccountWithBalance(
    val account: Account,
    val balance: Long,
)

/** 交易。amount 始终为正整数，方向由 type 决定。 */
data class Transaction(
    val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val accountId: String,
    val targetAccountId: String?,
    val categoryId: String?,
    val merchantId: String?,
    val note: String?,
    val transactionTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

/** 分类，最多两级。 */
data class Category(
    val id: String,
    val ledgerId: String,
    val name: String,
    val type: CategoryType,
    val parentId: String?,
    val sortOrder: Int,
    val isArchived: Boolean,
    /** 图标库 key；null 表示按名称自动匹配图标。 */
    val iconKey: String? = null,
)

/** 商家。允许同名。 */
data class Merchant(
    val id: String,
    val ledgerId: String,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean,
)

/** 标签。同账本内名称唯一。 */
data class Tag(
    val id: String,
    val ledgerId: String,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean,
)

/** 预算。V1 每月独立记录，month 形如 "2026-09"。 */
data class Budget(
    val id: String,
    val ledgerId: String,
    val month: String,
    val categoryId: String?,
    val amount: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
)
