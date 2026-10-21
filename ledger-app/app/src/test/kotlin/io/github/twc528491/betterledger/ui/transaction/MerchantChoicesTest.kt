package io.github.twc528491.betterledger.ui.transaction

import io.github.twc528491.betterledger.domain.model.Merchant
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantChoicesTest {
    private fun merchant(id: String, name: String) = Merchant(
        id = id, ledgerId = "ledger", name = name, createdAt = 0L, isArchived = false,
    )

    @Test fun `same name ignoring case and spaces is listed once in first position`() {
        val list = listOf(merchant("a", "美团"), merchant("b", "Costa"),
            merchant("c", " 美团 "), merchant("d", "costa"))
        assertEquals(listOf("a", "b"), MerchantChoices.distinctByName(list, null).map { it.id })
    }

    @Test fun `selected duplicate replaces the representative of its group`() {
        val list = listOf(merchant("a", "美团"), merchant("b", "盒马"), merchant("c", "美团"))
        assertEquals(listOf("c", "b"), MerchantChoices.distinctByName(list, "c").map { it.id })
    }
}
