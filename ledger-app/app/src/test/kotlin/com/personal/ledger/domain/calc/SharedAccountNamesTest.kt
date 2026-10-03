package com.personal.ledger.domain.calc

import com.personal.ledger.domain.calc.SharedAccountNames.AccountRef
import com.personal.ledger.domain.calc.SharedAccountNames.LedgerRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedAccountNamesTest {

    private val personal = LedgerRef("L1", "个人账本", createdAt = 1)
    private val travel = LedgerRef("L2", "旅行", createdAt = 2)
    private val family = LedgerRef("L3", "家庭", createdAt = 3)
    private val ledgers = listOf(personal, travel, family)

    private fun acc(id: String, ledgerId: String?, name: String, createdAt: Long = 0) =
        AccountRef(id, ledgerId, name, createdAt)

    @Test
    fun `没有跨账本同名时不改名`() {
        val accounts = listOf(acc("a", "L1", "现金"), acc("b", "L2", "银行卡"), acc("c", "L3", "支付宝"))
        assertTrue(SharedAccountNames.renames(accounts, ledgers).isEmpty())
    }

    @Test
    fun `跨账本同名（忽略首尾空白与大小写）全部改为名称加账本名`() {
        val accounts = listOf(
            acc("a", "L1", "现金"),
            acc("b", "L2", " 现金 "),
            acc("c", "L1", "Card"),
            acc("d", "L3", "card"),
            acc("e", "L2", "支付宝"),
        )
        assertEquals(
            mapOf(
                "a" to "现金（个人账本）",
                "b" to "现金（旅行）",
                "c" to "Card（个人账本）",
                "d" to "card（家庭）",
            ),
            SharedAccountNames.renames(accounts, ledgers),
        )
    }

    @Test
    fun `仅同一账本内重名保持原样`() {
        val accounts = listOf(acc("a", "L1", "现金"), acc("b", "L1", "现金"), acc("c", "L2", "银行卡"))
        assertTrue(SharedAccountNames.renames(accounts, ledgers).isEmpty())
    }

    @Test
    fun `改名结果仍冲突时在括号内追加序号且结果与输入顺序无关`() {
        val twin = LedgerRef("L4", "旅行", createdAt = 4) // 与 L2 同名的账本
        val accounts = listOf(
            acc("x", "L4", "现金", createdAt = 1),
            acc("a", "L1", "现金", createdAt = 5),
            acc("b", "L2", "现金", createdAt = 2),
            acc("taken", "L3", "现金（个人账本）"), // 已有账户的名字不能被占用
        )
        val expected = mapOf(
            "a" to "现金（个人账本 2）",
            "b" to "现金（旅行）",
            "x" to "现金（旅行 2）",
        )
        val all = ledgers + twin
        assertEquals(expected, SharedAccountNames.renames(accounts, all))
        assertEquals(expected, SharedAccountNames.renames(accounts.reversed(), all.reversed()))
    }

    @Test
    fun `同一账本内的多个同名账户与其他账本冲突时用序号区分`() {
        val accounts = listOf(
            acc("a1", "L1", "现金", createdAt = 1),
            acc("a2", "L1", "现金", createdAt = 2),
            acc("b", "L2", "现金"),
        )
        assertEquals(
            mapOf("a1" to "现金（个人账本）", "a2" to "现金（个人账本 2）", "b" to "现金（旅行）"),
            SharedAccountNames.renames(accounts, ledgers),
        )
    }

    @Test
    fun `所属账本未知的账户不参与改名`() {
        val accounts = listOf(acc("a", "L1", "现金"), acc("b", null, "现金"), acc("c", "missing", "现金"))
        assertTrue(SharedAccountNames.renames(accounts, ledgers).isEmpty())
    }
}
