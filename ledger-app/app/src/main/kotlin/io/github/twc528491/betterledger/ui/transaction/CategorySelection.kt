package io.github.twc528491.betterledger.ui.transaction

import io.github.twc528491.betterledger.domain.model.Category

internal object CategorySelection {
    fun roots(categories: List<Category>): List<Category> =
        categories.filter { it.parentId == null && !it.isArchived }

    fun children(categories: List<Category>, rootId: String): List<Category> =
        categories.filter { it.parentId == rootId && !it.isArchived }

    /** Merchant libraries are associated with a root even when the transaction stores a child. */
    fun merchantRootId(categories: List<Category>, categoryId: String?): String? {
        if (categoryId == null) return null
        val category = categories.firstOrNull { it.id == categoryId } ?: return categoryId
        return category.parentId ?: category.id
    }

    fun selectedRootId(categories: List<Category>, categoryId: String?): String? {
        val selected = categories.firstOrNull { it.id == categoryId } ?: return null
        return selected.parentId ?: selected.id
    }

    /** 已选分类的显示名（"父 / 子"）；未选择时返回 null，由界面显示本地化占位文本。 */
    fun label(categories: List<Category>, categoryId: String?): String? {
        val selected = categories.firstOrNull { it.id == categoryId } ?: return null
        val parent = categories.firstOrNull { it.id == selected.parentId } ?: return selected.name
        return parent.name + " / " + selected.name
    }
}
