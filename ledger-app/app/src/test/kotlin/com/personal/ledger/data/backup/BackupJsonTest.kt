package com.personal.ledger.data.backup

import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    @Test
    fun `备份 JSON 往返一致（含软删除交易）`() {
        val ledger = LedgerEntity("l1", "账本A", "CNY", 1L, 2L, false)
        val account = AccountEntity("a1", "l1", "现金", AccountType.CASH, "CNY", 0L, 1L, 1L, 1L, false)
        val category = CategoryEntity("c1", "l1", "餐饮", CategoryType.EXPENSE, null, 0, false)
        val tx = TransactionEntity(
            id = "t1", ledgerId = "l1", type = TransactionType.EXPENSE, amount = 2500,
            currency = "CNY", accountId = "a1", targetAccountId = null, categoryId = "c1",
            merchantId = null, note = "午饭", transactionTime = 1000L,
            createdAt = 1000L, updatedAt = 1000L, isDeleted = true,
        )
        val file = BackupFile(
            exportedAt = 1234L,
            appVersion = "0.1.0",
            data = BackupData(
                ledgers = listOf(ledger.toDto()),
                accounts = listOf(account.toDto()),
                categories = listOf(category.toDto()),
                transactions = listOf(tx.toDto()),
            ),
        )

        val encoded = json.encodeToString(BackupFile.serializer(), file)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)

        assertEquals(BackupConstants.FORMAT_VERSION, decoded.backupFormatVersion)
        assertEquals("账本A", decoded.data.ledgers.first().name)
        assertEquals("现金", decoded.data.accounts.first().name)
        assertEquals("餐饮", decoded.data.categories.first().name)
        val txDto = decoded.data.transactions.first()
        assertEquals("EXPENSE", txDto.type)
        assertEquals(2500L, txDto.amount)
        assertTrue(txDto.isDeleted) // 软删除交易包含在备份中且状态保留
        assertEquals(txDto, tx.toDto())
        assertEquals(decoded.data.ledgers.first().toEntity(), ledger)
    }
}
