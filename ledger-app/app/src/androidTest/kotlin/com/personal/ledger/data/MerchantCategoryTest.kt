package com.personal.ledger.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.CategoryEntity
import com.personal.ledger.data.local.entity.LedgerEntity
import com.personal.ledger.data.repository.MerchantRepository
import com.personal.ledger.domain.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MerchantCategoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: MerchantRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repo = MerchantRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun differentLibrariesMultipleLinksAndChildUsesRoot() = runTest {
        db.ledgerDao().insert(LedgerEntity("l1", "one", "CNY", 1, 1, false))
        db.ledgerDao().insert(LedgerEntity("l2", "two", "CNY", 1, 1, false))
        db.categoryDao().insert(CategoryEntity("food", "l1", "food", CategoryType.EXPENSE, null, 0, false))
        db.categoryDao().insert(CategoryEntity("lunch", "l1", "lunch", CategoryType.EXPENSE, "food", 1, false))
        db.categoryDao().insert(CategoryEntity("travel", "l1", "travel", CategoryType.EXPENSE, null, 2, false))
        db.categoryDao().insert(CategoryEntity("salary", "l1", "salary", CategoryType.INCOME, null, 3, false))
        db.categoryDao().insert(CategoryEntity("foreign", "l2", "foreign", CategoryType.EXPENSE, null, 0, false))

        val shared = repo.addMerchant("l1", "Shared", setOf("lunch", "salary")).getOrThrow()
        val foodOnly = repo.addMerchant("l1", "Food", setOf("food")).getOrThrow()
        assertEquals(setOf("food", "salary"), repo.getCategoryIdsByMerchant(shared.id))
        assertEquals(setOf(shared.id, foodOnly.id), repo.getByCategory("l1", "lunch").map { it.id }.toSet())
        assertEquals(listOf(shared.id), repo.getByCategory("l1", "salary").map { it.id })
        assertTrue(repo.getByCategory("l1", "travel").isEmpty())
        assertTrue(repo.getByCategory("l2", "foreign").isEmpty())
        assertTrue(repo.assignToCategory(shared.id, "foreign").isFailure)
        assertTrue(repo.addMerchant("l1", "Invalid", setOf("foreign")).isFailure)
        assertEquals(2, repo.getByLedger("l1").size)

        repo.assignToCategory(shared.id, "travel").getOrThrow()
        assertEquals(listOf(shared.id), repo.observeByCategory("l1", "travel").first().map { it.id })
        repo.replaceCategories(shared.id, setOf("salary")).getOrThrow()
        assertEquals(setOf("salary"), repo.getCategoryIdsByMerchant(shared.id))
        assertEquals(listOf(foodOnly.id), repo.getByCategory("l1", "food").map { it.id })
    }

    @Test
    fun archivedCategoryCannotBeNewlyAssignedButHistoricalLinkSurvives() = runTest {
        db.ledgerDao().insert(LedgerEntity("l1", "one", "CNY", 1, 1, false))
        val category = CategoryEntity("old", "l1", "old", CategoryType.EXPENSE, null, 0, false)
        db.categoryDao().insert(category)
        val merchant = repo.addMerchant("l1", "History", setOf("old")).getOrThrow()
        db.categoryDao().update(category.copy(isArchived = true))
        assertTrue(repo.getByCategory("l1", "old").isEmpty())
        assertTrue(repo.assignToCategory(merchant.id, "old").isFailure)
        repo.replaceCategories(merchant.id, emptySet()).getOrThrow()
        assertEquals(setOf("old"), repo.getCategoryIdsByMerchant(merchant.id))
    }
}
