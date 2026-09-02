package com.personal.ledger.domain.calc

import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionRulesTest {

    @Test
    fun `金额必须为正整数`() {
        assertNull(TransactionRules.validateAmount(1))
        assertNull(TransactionRules.validateAmount(TransactionRules.MAX_AMOUNT))
        assertEquals("金额必须大于 0", TransactionRules.validateAmount(0))
        assertEquals("金额必须大于 0", TransactionRules.validateAmount(-1))
        assertEquals("金额过大", TransactionRules.validateAmount(TransactionRules.MAX_AMOUNT + 1))
    }

    @Test
    fun `支出收入必须分类且类型匹配`() {
        assertNull(TransactionRules.validateCategoryForType(TransactionType.EXPENSE, CategoryType.EXPENSE))
        assertEquals("请选择分类", TransactionRules.validateCategoryForType(TransactionType.EXPENSE, null))
        assertEquals("分类类型与交易类型不一致", TransactionRules.validateCategoryForType(TransactionType.EXPENSE, CategoryType.INCOME))
        assertNull(TransactionRules.validateCategoryForType(TransactionType.INCOME, CategoryType.INCOME))
        assertEquals("请选择分类", TransactionRules.validateCategoryForType(TransactionType.INCOME, null))
        assertEquals("分类类型与交易类型不一致", TransactionRules.validateCategoryForType(TransactionType.INCOME, CategoryType.EXPENSE))
    }

    @Test
    fun `转账不设置分类`() {
        assertNull(TransactionRules.validateCategoryForType(TransactionType.TRANSFER, null))
        assertEquals("转账不设置分类", TransactionRules.validateCategoryForType(TransactionType.TRANSFER, CategoryType.EXPENSE))
    }

    @Test
    fun `转账账户规则`() {
        assertNull(TransactionRules.validateTransferAccounts("a", "b"))
        assertEquals("请选择转出账户", TransactionRules.validateTransferAccounts(null, "b"))
        assertEquals("请选择转入账户", TransactionRules.validateTransferAccounts("a", null))
        assertEquals("转出与转入账户不能相同", TransactionRules.validateTransferAccounts("a", "a"))
    }
}
