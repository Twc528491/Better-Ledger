package com.personal.ledger.ui.transaction

import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySelectionTest {
    private fun category(id: String, name: String, parent: String? = null,
        archived: Boolean = false) = Category(
        id = id, ledgerId = "ledger", name = name, type = CategoryType.EXPENSE,
        parentId = parent, sortOrder = 0, isArchived = archived,
    )

    @Test fun `historical child remains selected but only active roots are offered`() {
        val categories = listOf(
            category("food", "餐饮"), category("noodles", "面食", "food", archived = true),
            category("old", "旧类", archived = true),
        )
        val persistedId = "noodles"
        assertEquals(listOf("food"), CategorySelection.roots(categories).map { it.id })
        assertEquals("food", CategorySelection.selectedRootId(categories, persistedId))
        assertEquals("餐饮 / 面食", CategorySelection.label(categories, persistedId))
        assertEquals(null, CategorySelection.label(categories, null))
        assertEquals("noodles", persistedId)
    }

    @Test fun `choosing a root clears child while merchant scope resolves to the root`() {
        val categories = listOf(
            category("food", "餐饮"), category("noodles", "面食", "food"),
            category("coffee", "咖啡", "food"), category("old", "旧面食", "food", archived = true),
        )
        assertEquals(listOf("noodles", "coffee"), CategorySelection.children(categories, "food").map { it.id })
        assertEquals("food", CategorySelection.merchantRootId(categories, "food"))
        assertEquals("food", CategorySelection.merchantRootId(categories, "noodles"))
        assertEquals("food", CategorySelection.selectedRootId(categories, "noodles"))
    }
}
