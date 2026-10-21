package io.github.twc528491.betterledger.ui.common

/**
 * 列表多选状态（纯逻辑，便于单元测试）。
 * [active] 表示是否处于选择模式；选择模式下即使 0 项被选中也保持，直到用户关闭或列表清空。
 */
data class MultiSelection(
    val active: Boolean = false,
    val selected: Set<String> = emptySet(),
) {
    val count: Int get() = selected.size

    operator fun contains(id: String): Boolean = id in selected

    /** 长按：未在选择模式时进入并选中该项；已在选择模式时等同于点按切换。 */
    fun longPress(id: String): MultiSelection =
        if (active) toggle(id) else MultiSelection(active = true, selected = setOf(id))

    fun toggle(id: String): MultiSelection =
        copy(selected = if (id in selected) selected - id else selected + id)

    /** [ids] 非空且全部已选中。 */
    fun allSelected(ids: Collection<String>): Boolean =
        ids.isNotEmpty() && selected.containsAll(ids)

    /** 全选 / 取消全选：[ids] 已全部选中时取消这些项，否则补齐选中。也用于按天整组选择。 */
    fun toggleAll(ids: Collection<String>): MultiSelection =
        if (allSelected(ids)) copy(selected = selected - ids.toSet()) else copy(selected = selected + ids)

    /** 列表变化后丢弃已不可见的 id；列表变为空时自动退出选择模式。 */
    fun retain(visible: Collection<String>): MultiSelection {
        if (!active) return this
        if (visible.isEmpty()) return MultiSelection()
        val kept = selected.intersect(visible.toSet())
        return if (kept.size == selected.size) this else copy(selected = kept)
    }

    /** 按 [order] 的顺序返回已选 id（便于批量操作与撤销保持稳定顺序）。 */
    fun selectedIn(order: List<String>): List<String> = order.filter { it in selected }

    companion object {
        val None = MultiSelection()
    }
}
