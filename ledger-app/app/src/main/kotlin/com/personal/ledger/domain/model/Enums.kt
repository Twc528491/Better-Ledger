package com.personal.ledger.domain.model

/** 交易类型：支出 / 收入 / 转账。 */
enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/** 账户类型只表示账户性质，不决定资产/负债分组。 */
enum class AccountType {
    CASH, BANK, ALIPAY, WECHAT, CREDIT_CARD, SAVINGS, INVESTMENT, LOAN, OTHER
}

/** 分类类型，创建后不可修改。 */
enum class CategoryType { EXPENSE, INCOME }
