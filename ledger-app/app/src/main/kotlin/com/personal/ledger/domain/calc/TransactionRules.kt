package com.personal.ledger.domain.calc

import androidx.annotation.StringRes
import com.personal.ledger.R
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType

/**
 * 交易规则校验（纯函数，可 JVM 测试）。
 * DATA_MODEL.md 第 7 节中不依赖数据库的部分；
 * 依赖数据库的检查（实体存在性、归属账本、初始时间）在 Repository 层完成。
 * 校验失败返回错误文案的字符串资源 ID，合法返回 null。
 */
object TransactionRules {

    /** 金额上限：防止溢出（最小货币单位）。 */
    const val MAX_AMOUNT = Long.MAX_VALUE / 1000

    /** amount 始终为正整数（方向由交易类型决定）。 */
    @StringRes
    fun validateAmount(amount: Long): Int? = when {
        amount <= 0 -> R.string.err_amount_positive
        amount > MAX_AMOUNT -> R.string.err_amount_too_large
        else -> null
    }

    /**
     * 分类规则：
     * - EXPENSE / INCOME 必须填分类，且分类类型与交易类型一致；
     * - TRANSFER 不填分类。
     *
     * @param categoryType 已解析的分类类型；无分类时为 null
     */
    @StringRes
    fun validateCategoryForType(
        type: TransactionType,
        categoryType: CategoryType?,
    ): Int? = when (type) {
        TransactionType.EXPENSE ->
            when (categoryType) {
                null -> R.string.err_select_category
                CategoryType.EXPENSE -> null
                CategoryType.INCOME -> R.string.err_category_type_mismatch
            }

        TransactionType.INCOME ->
            when (categoryType) {
                null -> R.string.err_select_category
                CategoryType.INCOME -> null
                CategoryType.EXPENSE -> R.string.err_category_type_mismatch
            }

        TransactionType.TRANSFER ->
            if (categoryType != null) R.string.err_transfer_no_category else null
    }

    /**
     * 转账账户规则：
     * - 必须填转入账户；
     * - 转出与转入账户不能相同。
     */
    @StringRes
    fun validateTransferAccounts(
        accountId: String?,
        targetAccountId: String?,
    ): Int? = when {
        accountId.isNullOrEmpty() -> R.string.err_select_source_account
        targetAccountId.isNullOrEmpty() -> R.string.err_select_target_account
        targetAccountId == accountId -> R.string.err_same_transfer_accounts
        else -> null
    }
}
