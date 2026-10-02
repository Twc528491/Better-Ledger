package com.personal.ledger.domain.calc

import androidx.annotation.StringRes
import com.personal.ledger.R
import com.personal.ledger.domain.model.Category

/**
 * 彻底删除（purge）的纯规则（可 JVM 测试）。
 * 只有已归档的项目才能彻底删除；分类被引用时必须先指定同账本、同类型的有效替代分类。
 */
object PurgeRules {

    /** 删除分类时实际移除的范围。 */
    data class CategoryScope(
        /** 将被删除的分类 ID：目标本身 + 其已归档的二级分类（删除一级分类时）。 */
        val removedIds: List<String>,
        /** 仍在使用中的二级分类：存在时禁止删除一级分类。 */
        val activeChildIds: List<String>,
    )

    fun categoryScope(target: Category, all: List<Category>): CategoryScope {
        if (target.parentId != null) return CategoryScope(listOf(target.id), emptyList())
        val children = all.filter { it.parentId == target.id }
        return CategoryScope(
            removedIds = listOf(target.id) + children.filter { it.isArchived }.map { it.id },
            activeChildIds = children.filterNot { it.isArchived }.map { it.id },
        )
    }

    /**
     * 可作为替代的分类：同账本、同类型、未归档、不在删除范围内；二级分类的父分类也必须未归档。
     * 按一级分类排序，其二级分类紧随其后。
     */
    fun replacementCandidates(
        target: Category,
        all: List<Category>,
        removedIds: Collection<String>,
    ): List<Category> {
        val removed = removedIds.toSet()
        val active = all.filter {
            !it.isArchived && it.ledgerId == target.ledgerId && it.type == target.type && it.id !in removed
        }
        // 父分类已归档（或在删除范围内）的二级分类不会出现在任何一级分类之后，自然被排除
        val roots = active.filter { it.parentId == null }.sortedBy { it.sortOrder }
        return roots.flatMap { root ->
            listOf(root) + active.filter { it.parentId == root.id }.sortedBy { it.sortOrder }
        }
    }

    /** 校验替代分类；合法返回 null，否则返回错误文案的字符串资源 ID。 */
    @StringRes
    fun validateReplacement(
        target: Category,
        replacement: Category?,
        all: List<Category>,
        removedIds: Collection<String>,
    ): Int? = when {
        replacement == null -> R.string.err_select_replacement
        replacement.id in removedIds -> R.string.err_replacement_removed
        replacement.ledgerId != target.ledgerId -> R.string.err_replacement_other_ledger
        replacement.type != target.type -> R.string.err_replacement_type
        replacement.isArchived -> R.string.err_replacement_archived
        replacement.parentId != null &&
            all.firstOrNull { it.id == replacement.parentId }?.isArchived != false -> R.string.err_replacement_parent_archived
        else -> null
    }

    /** 替代分类的显示名：二级分类显示为 “父 / 子”。 */
    fun displayName(category: Category, all: List<Category>): String {
        val parent = category.parentId?.let { id -> all.firstOrNull { it.id == id } }
        return if (parent == null) category.name else "${parent.name} / ${category.name}"
    }

    /** 账本删除的强确认：输入的名称（去除首尾空白）必须与账本名完全一致。 */
    fun ledgerNameConfirmed(input: String, ledgerName: String): Boolean =
        input.trim().isNotEmpty() && input.trim() == ledgerName.trim()
}
