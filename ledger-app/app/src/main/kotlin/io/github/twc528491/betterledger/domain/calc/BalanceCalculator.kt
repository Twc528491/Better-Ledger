package io.github.twc528491.betterledger.domain.calc

import io.github.twc528491.betterledger.domain.model.TransactionType

/**
 * 纯函数：账户余额计算。
 *
 * 规则（DATA_MODEL.md 第 5 节）：
 *   balance = initialBalance + 收入 - 支出 ± 转账
 *
 * 本函数接收已折算方向的增减量列表；方向折算见 [deltaFor]。
 */
object BalanceCalculator {

    /** 计算余额。 */
    fun compute(initialBalance: Long, deltas: List<Long>): Long =
        initialBalance + deltas.sum()

    /**
     * 把一笔交易折算为对某账户余额的增减量。
     * @param accountId 目标账户（对于转账，既可能是转出方也可能是转入方）
     * @return null 表示该交易与此账户无关
     */
    fun deltaFor(
        type: TransactionType,
        amount: Long,
        accountId: String,
        targetAccountId: String?,
        account: String,
    ): Long? = when (type) {
        TransactionType.EXPENSE -> if (account == accountId) -amount else null
        TransactionType.INCOME -> if (account == accountId) amount else null
        TransactionType.TRANSFER -> when (account) {
            accountId -> -amount
            targetAccountId -> amount
            else -> null
        }
    }

    /**
     * 把按类型聚合的合计折算为增减量（账户余额汇总全部账本，见 DATA_MODEL.md 第 5 节）。
     * @param incoming true 表示该账户作为转账转入方的合计
     */
    fun deltaForTotal(type: TransactionType, incoming: Boolean, total: Long): Long = when {
        incoming -> if (type == TransactionType.TRANSFER) total else 0L
        type == TransactionType.INCOME -> total
        else -> -total // 支出与转出
    }
}
