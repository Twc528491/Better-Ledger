package com.personal.ledger.domain.calc

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * 纯函数：分类支出环比（本月 vs 上月）。
 * 只比较同一分类 id，不做父子分类汇总，与统计页「完整分类明细」口径一致。
 */
object CategoryChangeCalculator {

    data class Change(
        val categoryId: String,
        val current: Long,
        val previous: Long,
    ) {
        /** 正数表示本月花得更多。 */
        val delta: Long get() = current - previous

        /** 环比百分比（四舍五入）；上月为 0 时没有可比基数，返回 null。 */
        val percent: Long?
            get() = if (previous <= 0L) null else (delta.toDouble() * 100.0 / previous.toDouble()).roundToLong()
    }

    /**
     * 合并两个月的分类支出，按变化幅度（绝对值）从大到小排序；
     * 幅度相同时本月金额大的在前，再按 id 保证顺序稳定。
     *
     * @param includeUnchanged 为 false 时剔除两月金额相同（含均为 0）的分类
     */
    fun compare(
        current: Map<String, Long>,
        previous: Map<String, Long>,
        includeUnchanged: Boolean = false,
    ): List<Change> =
        (current.keys + previous.keys)
            .map { id -> Change(id, current[id] ?: 0L, previous[id] ?: 0L) }
            .filter { includeUnchanged || it.delta != 0L }
            .filter { it.current != 0L || it.previous != 0L }
            .sortedWith(
                compareByDescending<Change> { abs(it.delta) }
                    .thenByDescending { it.current }
                    .thenBy { it.categoryId },
            )
}
