package com.personal.ledger.domain.calc

import java.util.Locale

/**
 * 账户跨账本共享后的同名处理（纯函数，可 JVM 测试）。
 *
 * v0.6 之前账户属于单个账本，不同账本中常有同名账户（如每个账本各有一个“现金”）。
 * 合并为全局列表后无法区分，因此 Room 迁移 4→5 与恢复旧版 JSON 备份（格式 1–4）时
 * 统一按以下规则改名：
 * - 名称比较忽略首尾空白与大小写；
 * - 同名组跨越两个及以上账本时，组内每个账户改为 `名称（账本名）`；
 *   仅在同一账本内重名的账户保持原样（旧版本中即已共存）；
 * - 改名结果仍与其他账户重名（如两个账本本身同名）时，在括号内追加序号：`名称（账本名 2）`；
 * - 处理顺序固定（账本创建时间、账本 ID、账户创建时间、账户 ID），同样的输入总得到同样的结果。
 * 括号使用全角符号，与语言无关（迁移时无法得知界面语言，且结果需确定）。
 */
object SharedAccountNames {

    data class LedgerRef(val id: String, val name: String, val createdAt: Long)

    data class AccountRef(
        val id: String,
        /** 旧版本中所属账本；未知时为 null（不参与改名）。 */
        val ledgerId: String?,
        val name: String,
        val createdAt: Long,
    )

    /** 名称比较键：忽略首尾空白与大小写。 */
    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    /** @return 需要改名的账户 ID → 新名称；没有冲突时为空。 */
    fun renames(accounts: List<AccountRef>, ledgers: List<LedgerRef>): Map<String, String> {
        val ledgerById = ledgers.associateBy { it.id }
        val toRename = accounts
            .filter { it.ledgerId != null && it.ledgerId in ledgerById }
            .groupBy { normalize(it.name) }
            .values
            .filter { group -> group.map { it.ledgerId }.distinct().size >= 2 }
            .flatten()
        if (toRename.isEmpty()) return emptyMap()

        val renamedIds = toRename.map { it.id }.toSet()
        val taken = accounts.filter { it.id !in renamedIds }.map { normalize(it.name) }.toMutableSet()
        val ordered = toRename.sortedWith(
            compareBy<AccountRef>(
                { ledgerById.getValue(it.ledgerId!!).createdAt },
                { it.ledgerId },
                { it.createdAt },
                { it.id },
            ),
        )
        val result = LinkedHashMap<String, String>()
        ordered.forEach { account ->
            val base = account.name.trim()
            val ledgerName = ledgerById.getValue(account.ledgerId!!).name.trim()
            var candidate = "$base（$ledgerName）"
            var ordinal = 2
            while (normalize(candidate) in taken) {
                candidate = "$base（$ledgerName $ordinal）"
                ordinal++
            }
            taken += normalize(candidate)
            result[account.id] = candidate
        }
        return result
    }
}
