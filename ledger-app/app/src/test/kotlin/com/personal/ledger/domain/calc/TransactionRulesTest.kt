package com.personal.ledger.domain.calc

import com.personal.ledger.R
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
        assertEquals(R.string.err_amount_positive, TransactionRules.validateAmount(0))
        assertEquals(R.string.err_amount_positive, TransactionRules.validateAmount(-1))
        assertEquals(R.string.err_amount_too_large, TransactionRules.validateAmount(TransactionRules.MAX_AMOUNT + 1))
    }

    @Test
    fun `支出收入必须分类且类型匹配`() {
        assertNull(TransactionRules.validateCategoryForType(TransactionType.EXPENSE, CategoryType.EXPENSE))
        assertEquals(R.string.err_select_category, TransactionRules.validateCategoryForType(TransactionType.EXPENSE, null))
        assertEquals(R.string.err_category_type_mismatch, TransactionRules.validateCategoryForType(TransactionType.EXPENSE, CategoryType.INCOME))
        assertNull(TransactionRules.validateCategoryForType(TransactionType.INCOME, CategoryType.INCOME))
        assertEquals(R.string.err_select_category, TransactionRules.validateCategoryForType(TransactionType.INCOME, null))
        assertEquals(R.string.err_category_type_mismatch, TransactionRules.validateCategoryForType(TransactionType.INCOME, CategoryType.EXPENSE))
    }

    @Test
    fun `转账不设置分类`() {
        assertNull(TransactionRules.validateCategoryForType(TransactionType.TRANSFER, null))
        assertEquals(R.string.err_transfer_no_category, TransactionRules.validateCategoryForType(TransactionType.TRANSFER, CategoryType.EXPENSE))
    }

    @Test
    fun `转账账户规则`() {
        assertNull(TransactionRules.validateTransferAccounts("a", "b"))
        assertEquals(R.string.err_select_source_account, TransactionRules.validateTransferAccounts(null, "b"))
        assertEquals(R.string.err_select_target_account, TransactionRules.validateTransferAccounts("a", null))
        assertEquals(R.string.err_same_transfer_accounts, TransactionRules.validateTransferAccounts("a", "a"))
    }
}
