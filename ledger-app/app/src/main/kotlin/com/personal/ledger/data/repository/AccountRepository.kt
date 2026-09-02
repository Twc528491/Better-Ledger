package com.personal.ledger.data.repository

import com.personal.ledger.data.local.dao.AccountDao
import com.personal.ledger.data.local.dao.TransactionDao
import com.personal.ledger.data.local.entity.AccountEntity
import com.personal.ledger.domain.calc.BalanceCalculator
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.AccountType
import com.personal.ledger.domain.model.AccountWithBalance
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) {

    fun observeByLedger(ledgerId: String): Flow<List<Account>> =
        accountDao.observeByLedger(ledgerId).map { list -> list.map { it.toDomain() } }

    /** 账户 + 实时余额（由原始交易计算）。 */
    fun observeWithBalances(ledgerId: String): Flow<List<AccountWithBalance>> =
        combine(
            accountDao.observeByLedger(ledgerId),
            transactionDao.observeByLedger(ledgerId),
        ) { accounts, transactions ->
            accounts.map { acc ->
                val deltas = transactions.mapNotNull { tx ->
                    BalanceCalculator.deltaFor(tx.type, tx.amount, tx.accountId, tx.targetAccountId, acc.id)
                }
                AccountWithBalance(
                    account = acc.toDomain(),
                    balance = BalanceCalculator.compute(acc.initialBalance, deltas),
                )
            }
        }

    suspend fun getAccount(id: String): Account? = accountDao.getById(id)?.toDomain()

    /** 新建账户。 */
    suspend fun addAccount(
        ledgerId: String,
        name: String,
        type: AccountType,
        currency: String,
        initialBalance: Long,
        initialBalanceTime: Long,
    ): Result<Account> {
        val now = TimeUtils.now()
        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            ledgerId = ledgerId,
            name = name.trim(),
            type = type,
            currency = currency,
            initialBalance = initialBalance,
            initialBalanceTime = initialBalanceTime,
            createdAt = now,
            updatedAt = now,
            isArchived = false,
        )
        return try {
            accountDao.insert(account)
            Result.success(account.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 修改账户。约束（DATA_MODEL.md 第 7 节）：
     * 修改 initialBalanceTime 不得使其晚于该账户已有交易时间。
     */
    suspend fun updateAccount(
        account: Account,
        newName: String,
        newType: AccountType,
        newInitialBalance: Long,
        newInitialBalanceTime: Long,
        newIsArchived: Boolean = account.isArchived,
    ): Result<Account> {
        val entity = accountDao.getById(account.id)
            ?: return Result.failure(NoSuchElementException("账户不存在"))
        val txs = transactionDao.getByAccount(entity.ledgerId, entity.id)
        val earliestTx = txs.minOfOrNull { it.transactionTime }
        if (earliestTx != null && newInitialBalanceTime > earliestTx) {
            return Result.failure(IllegalArgumentException("初始时间不能晚于已有交易时间"))
        }
        val updated = entity.copy(
            name = newName.trim(),
            type = newType,
            initialBalance = newInitialBalance,
            initialBalanceTime = newInitialBalanceTime,
            isArchived = newIsArchived,
            updatedAt = TimeUtils.now(),
        )
        return try {
            accountDao.update(updated)
            Result.success(updated.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setArchived(id: String, archived: Boolean): Result<Unit> {
        val entity = accountDao.getById(id) ?: return Result.failure(NoSuchElementException("账户不存在"))
        return try {
            accountDao.update(entity.copy(isArchived = archived, updatedAt = TimeUtils.now()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
