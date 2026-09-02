package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType

/**
 * 交易规则校验（纯函数，可 JVM 测试）。
 * DATA_MODEL.md 第 7 节中不依赖数据库的部分；
 * 依赖数据库的检查（实体存在性、归属账本、初始时间）在 Repository 层完成。
 */
object TransactionRules {

    /** 金额上限：防止溢出（最小货币单位）。 */
    const val MAX_AMOUNT = Long.MAX_VALUE / 1000

    /** amount 始终为正整数（方向由交易类型决定）。 */
    fun validateAmount(amount: Long): String? = when {
        amount <= 0 -> "金额必须大于 0"
        amount > MAX_AMOUNT -> "金额过大"
        else -> null
    }

    /**
     * 分类规则：
     * - EXPENSE / INCOME 必须填分类，且分类类型与交易类型一致；
     * - TRANSFER 不填分类。
     *
     * @param categoryType 已解析的分类类型；无分类时为 null
     */
    fun validateCategoryForType(
        type: TransactionType,
        categoryType: CategoryType?,
    ): String? = when (type) {
        TransactionType.EXPENSE ->
            when (categoryType) {
                null -> "请选择分类"
                CategoryType.EXPENSE -> null
                CategoryType.INCOME -> "分类类型与交易类型不一致"
            }

        TransactionType.INCOME ->
            when (categoryType) {
                null -> "请选择分类"
                CategoryType.INCOME -> null
                CategoryType.EXPENSE -> "分类类型与交易类型不一致"
            }

        TransactionType.TRANSFER ->
            if (categoryType != null) "转账不设置分类" else null
    }

    /**
     * 转账账户规则：
     * - 必须填转入账户；
     * - 转出与转入账户不能相同。
     */
    fun validateTransferAccounts(
        accountId: String?,
        targetAccountId: String?,
    ): String? = when {
        accountId.isNullOrEmpty() -> "请选择转出账户"
        targetAccountId.isNullOrEmpty() -> "请选择转入账户"
        targetAccountId == accountId -> "转出与转入账户不能相同"
        else -> null
    }
}
