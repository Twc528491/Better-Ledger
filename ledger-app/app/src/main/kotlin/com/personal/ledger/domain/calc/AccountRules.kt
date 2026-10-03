package com.personal.ledger.domain.calc

import androidx.annotation.StringRes
import com.personal.ledger.R
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.TransactionType

/**
 * 全局账户的纯规则（可 JVM 测试）：账本可用范围与合并账户。
 * 账户为所有账本共享；V1 不做汇率，账本只能使用币种等于其本位币的账户。
 */
object AccountRules {

    /**
     * 某账本可使用的账户：币种等于账本本位币，且未归档；
     * [keepIds] 中的已归档账户（如编辑旧账单时的原账户）仍保留，避免一打开就显示“未选择”。
     */
    fun usableIn(
        accounts: List<Account>,
        ledgerCurrency: String,
        keepIds: Set<String> = emptySet(),
    ): List<Account> = accounts.filter {
        it.currency == ledgerCurrency && (!it.isArchived || it.id in keepIds)
    }

    /** 可作为合并目标的账户：同币种、未归档、不是源账户本身。 */
    fun mergeCandidates(source: Account, all: List<Account>): List<Account> =
        all.filter { it.id != source.id && it.currency == source.currency && !it.isArchived }

    /** 校验合并；合法返回 null，否则返回错误文案的字符串资源 ID。 */
    @StringRes
    fun validateMerge(source: Account?, target: Account?): Int? = when {
        source == null -> R.string.err_account_not_found
        target == null -> R.string.err_merge_target_not_found
        source.id == target.id -> R.string.err_merge_same_account
        source.currency != target.currency -> R.string.err_merge_currency
        target.isArchived -> R.string.err_merge_target_archived
        else -> null
    }

    /** 合并计算所需的交易字段。 */
    data class MergeTx(
        val id: String,
        val type: TransactionType,
        val accountId: String,
        val targetAccountId: String?,
    )

    data class MergePlan(
        /** 改挂到目标账户的交易（作为账户或转入账户涉及源账户，含回收站）。 */
        val movedIds: List<String>,
        /** 源与目标之间的转账：合并后会变成自己转给自己，对合并后的余额净额为零，直接删除。 */
        val selfTransferIds: List<String>,
    )

    /** 计算把 [sourceId] 合并到 [targetId] 时各交易的去向（[transactions] 可包含无关交易）。 */
    fun mergePlan(sourceId: String, targetId: String, transactions: List<MergeTx>): MergePlan {
        val involved = transactions.filter { it.accountId == sourceId || it.targetAccountId == sourceId }
        val (self, moved) = involved.partition { tx ->
            tx.type == TransactionType.TRANSFER &&
                ((tx.accountId == sourceId && tx.targetAccountId == targetId) ||
                    (tx.accountId == targetId && tx.targetAccountId == sourceId))
        }
        return MergePlan(movedIds = moved.map { it.id }, selfTransferIds = self.map { it.id })
    }

    /**
     * 合并后目标账户的初始余额与初始时间：初始余额相加，初始时间取较早者
     * （两账户的既有交易都不早于各自的初始时间，因此仍满足约束）。
     * 初始余额溢出时抛出 [ArithmeticException]。
     */
    fun mergedOpening(
        sourceBalance: Long,
        sourceTime: Long,
        targetBalance: Long,
        targetTime: Long,
    ): Pair<Long, Long> = Math.addExact(targetBalance, sourceBalance) to minOf(sourceTime, targetTime)
}
