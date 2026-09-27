package com.personal.ledger.data.backup

import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.local.entity.MerchantCategoryEntity
import com.personal.ledger.data.local.entity.MerchantEntity
import com.personal.ledger.data.local.entity.RecurringChargeEntity
import com.personal.ledger.data.local.entity.RecurringOccurrenceEntity
import com.personal.ledger.data.local.entity.TransactionEntity
import com.personal.ledger.data.repository.CategoryIconKeys
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.TransactionType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

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
                recurringCharges = listOf(
                    RecurringChargeEntity(
                        id = "r1",
                        ledgerId = "l1",
                        name = "云盘",
                        amount = 1200,
                        currency = "CNY",
                        accountId = "a1",
                        categoryId = "c1",
                        frequency = "MONTHLY",
                        startDate = "2025-01-31",
                        minuteOfDay = 9 * 60,
                        zoneId = "UTC",
                        nextOccurrenceIndex = 2,
                        nextDueAt = Instant.parse("2025-03-31T09:00:00Z").toEpochMilli(),
                        isPaused = false,
                        lastError = null,
                        updatedAt = 1234L,
                    ).toDto(),
                ),
                recurringOccurrences = listOf(RecurringOccurrenceEntity("r1", 1_743_411_600_000L, "t1").toDto()),
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
        assertEquals(file.data.recurringCharges, decoded.data.recurringCharges)
        assertEquals(file.data.recurringOccurrences, decoded.data.recurringOccurrences)
    }

    @Test
    fun versionThreeRoundTripsMerchantLibraries() {
        val link = MerchantCategoryEntity("merchant", "root").toDto()
        val backup = BackupFile(
            exportedAt = 1L,
            appVersion = "0.3.0",
            data = BackupData(merchantCategories = listOf(link)),
        )
        val decoded = json.decodeFromString(
            BackupFile.serializer(),
            json.encodeToString(BackupFile.serializer(), backup),
        )
        assertEquals(BackupConstants.FORMAT_VERSION, decoded.backupFormatVersion)
        assertEquals(listOf(link), decoded.data.merchantCategories)
        assertEquals(MerchantCategoryEntity("merchant", "root"), link.toEntity())
    }

    @Test
    fun oldBackupsInferValidTopLevelLinksFromHistory() {
        val old = """{"backupFormatVersion":2,"exportedAt":1,"appVersion":"0.3.0","data":{}}"""
        assertTrue(json.decodeFromString(BackupFile.serializer(), old).data.merchantCategories.isEmpty())
        val data = BackupData(
            merchants = listOf(
                MerchantEntity("m1", "l1", "商家", 1L, false).toDto(),
                MerchantEntity("unused", "l1", "未使用", 1L, false).toDto(),
            ),
            categories = listOf(
                CategoryEntity("root", "l1", "餐饮", CategoryType.EXPENSE, null, 0, true).toDto(),
                CategoryEntity("child", "l1", "午餐", CategoryType.EXPENSE, "root", 1, true).toDto(),
                CategoryEntity("foreign", "l2", "其他", CategoryType.EXPENSE, null, 0, false).toDto(),
            ),
            transactions = listOf(
                TransactionDto("t1", "l1", "EXPENSE", 10, "CNY", "a1",
                    categoryId = "child", merchantId = "m1", transactionTime = 1,
                    createdAt = 1, updatedAt = 1, isDeleted = true),
                TransactionDto("t2", "l1", "EXPENSE", 10, "CNY", "a1",
                    categoryId = "foreign", merchantId = "m1", transactionTime = 1,
                    createdAt = 1, updatedAt = 1, isDeleted = false),
            ),
        )
        assertEquals(listOf(MerchantCategoryDto("m1", "root")), deriveLegacyMerchantCategories(data))
    }

    @Test
    fun versionOneBackupDefaultsRecurringDataToEmpty() {
        val versionOne = """{"backupFormatVersion":1,"exportedAt":1234,"appVersion":"0.2.4","data":{}}"""

        val decoded = json.decodeFromString(BackupFile.serializer(), versionOne)

        assertEquals(1, decoded.backupFormatVersion)
        assertTrue(decoded.data.recurringCharges.isEmpty())
        assertTrue(decoded.data.recurringOccurrences.isEmpty())
    }

    @Test
    fun versionFourRoundTripsCategoryIconKey() {
        val custom = CategoryEntity("c1", "l1", "猫粮", CategoryType.EXPENSE, null, 0, false, iconKey = "pets")
        val automatic = CategoryEntity("c2", "l1", "餐饮", CategoryType.EXPENSE, null, 1, false)
        val backup = BackupFile(
            exportedAt = 1L,
            appVersion = "0.3.3",
            data = BackupData(categories = listOf(custom.toDto(), automatic.toDto())),
        )
        val encoded = json.encodeToString(BackupFile.serializer(), backup)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)

        assertEquals(4, decoded.backupFormatVersion)
        assertTrue(encoded.contains("\"iconKey\": \"pets\""))
        assertEquals(listOf(custom, automatic), decoded.data.categories.map { it.toEntity() })
        assertNull(decoded.data.categories[1].iconKey)
    }

    @Test
    fun versionThreeBackupWithoutIconKeyDecodesAsAutomaticIcon() {
        val versionThree = """
            {"backupFormatVersion":3,"exportedAt":1,"appVersion":"0.3.2","data":{
              "categories":[{"id":"c1","ledgerId":"l1","name":"餐饮","type":"EXPENSE",
                "parentId":null,"sortOrder":0,"isArchived":false}]
            }}
        """.trimIndent()

        val decoded = json.decodeFromString(BackupFile.serializer(), versionThree)

        assertEquals(3, decoded.backupFormatVersion)
        val category = decoded.data.categories.single()
        assertNull(category.iconKey)
        assertEquals(
            CategoryEntity("c1", "l1", "餐饮", CategoryType.EXPENSE, null, 0, false, iconKey = null),
            category.toEntity(),
        )
    }

    @Test
    fun categoryIconKeyValidationAcceptsOnlyNullOrShortNonBlankKeys() {
        assertTrue(CategoryIconKeys.isValid(null))
        assertTrue(CategoryIconKeys.isValid("pets"))
        assertFalse(CategoryIconKeys.isValid(""))
        assertFalse(CategoryIconKeys.isValid("   "))
        assertFalse(CategoryIconKeys.isValid(" pets"))
        assertFalse(CategoryIconKeys.isValid("x".repeat(CategoryIconKeys.MAX_LENGTH + 1)))
        assertNull(CategoryIconKeys.normalize(null))
        assertEquals("pets", CategoryIconKeys.normalize("  pets "))
        assertTrue(runCatching { CategoryIconKeys.normalize(" ") }.isFailure)
    }
}
