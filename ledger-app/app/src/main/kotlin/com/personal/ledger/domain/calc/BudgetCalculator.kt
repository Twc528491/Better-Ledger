package com.personal.ledger.domain.calc

/**
 * 纯函数：预算执行计算。
 *
 * 规则（DATA_MODEL.md / MVP.md 第 8 节）：
 * - 总预算（categoryId = null）统计当月全部支出；
 * - 分类预算统计该分类树当月支出（父分类包含所有子分类）；
 * - 总预算与分类预算互相独立，不要求金额相加一致。
 */
object BudgetCalculator {

    /**
     * 把每个分类的支出金额按分类树向上汇总：
     * 每个父分类的金额 = 自身金额 + 所有直接/间接子分类金额。
     *
     * @param categoryIdToParent 分类 id → 父分类 id
     * @param categoryExpense 分类 id → 该分类直接支出
     * @return 分类 id → 分类树总支出（含自身与所有子孙）
     */
    fun rollUpToParents(
        categoryIdToParent: Map<String, String?>,
        categoryExpense: Map<String, Long>,
    ): Map<String, Long> {
        val result = HashMap(categoryExpense)
        val childrenByParent = categoryIdToParent.entries
            .filter { it.value != null }
            .groupBy({ it.value!! }, { it.key })

        fun addUp(id: String): Long {
            val total = result[id] ?: 0L
            val added = childrenByParent[id]?.sumOf { addUp(it) } ?: 0L
            val combined = total + added
            result[id] = combined
            return combined
        }

        // 从所有一级分类（parentId == null）开始递归。
        categoryIdToParent.entries
            .filter { it.value == null }
            .forEach { addUp(it.key) }
        return result
    }

    /**
     * 计算某预算（总预算或分类预算）当月的执行情况。
     *
     * @param budgetAmount 预算金额（最小货币单位），<= 0 视为未设置
     * @param spent 实际支出（总预算传当月全部支出；分类预算传分类树支出）
     * @return spent / budgetAmount 的千分比（0..1000+），预算未设置返回 null
     */
    fun progressPermille(budgetAmount: Long, spent: Long): Int? {
        if (budgetAmount <= 0) return null
        return (spent * 1000 / budgetAmount).toInt()
    }
}
