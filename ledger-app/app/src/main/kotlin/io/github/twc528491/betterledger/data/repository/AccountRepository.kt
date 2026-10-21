package io.github.twc528491.betterledger.data.repository

import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.local.dao.AccountDao
import io.github.twc528491.betterledger.data.local.dao.TransactionDao
import io.github.twc528491.betterledger.data.local.entity.AccountEntity
import io.github.twc528491.betterledger.domain.calc.BalanceCalculator
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.domain.model.AccountType
import io.github.twc528491.betterledger.domain.model.AccountWithBalance
import io.github.twc528491.betterledger.domain.model.Transaction
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UserFacingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 账户（全局实体，所有账本共享）。
 * 余额 = 初始余额 + 全部账本中涉及该账户的正常交易（作为账户或转入账户）。
 */
class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) {

    /** 全部账户（含已归档）。 */
    fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 某币种的全部账户（含已归档），即该币种账本可使用的范围；调用方按需过滤已归档。 */
    fun observeByCurrency(currency: String): Flow<List<Account>> =
        accountDao.observeByCurrency(currency).map { list -> list.map { it.toDomain() } }

    fun observeAccount(id: String): Flow<Account?> = accountDao.observeById(id).map { it?.toDomain() }

    /** 全部账户 + 实时余额（由全部账本的原始交易在数据库中聚合计算）。 */
    fun observeWithBalances(): Flow<List<AccountWithBalance>> =
        combine(
            accountDao.observeAll(),
            transactionDao.observeAccountTotals(),
        ) { accounts, totals ->
            val deltaOf = totals.groupBy({ it.accountId }) {
                BalanceCalculator.deltaForTotal(it.type, it.incoming, it.total)
            }
            accounts.map { acc ->
                AccountWithBalance(
                    account = acc.toDomain(),
                    balance = BalanceCalculator.compute(acc.initialBalance, deltaOf[acc.id].orEmpty()),
                )
            }
        }

    /** 涉及某账户的正常交易（全部账本，不含回收站），按时间倒序。 */
    fun observeAccountTransactions(accountId: String): Flow<List<Transaction>> =
        transactionDao.observeByAccount(accountId).map { list -> list.map { it.toDomain() } }

    suspend fun getAccount(id: String): Account? = accountDao.getById(id)?.toDomain()

    /** 新建账户（全局可用；[currency] 一般取当前账本的本位币）。 */
    suspend fun addAccount(
        name: String,
        type: AccountType,
        currency: String,
        initialBalance: Long,
        initialBalanceTime: Long,
    ): Result<Account> {
        val now = TimeUtils.now()
        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
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
     * 修改 initialBalanceTime 不得使其晚于该账户已有交易时间（全部账本，含回收站）。
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
            ?: return Result.failure(UserFacingException(R.string.err_account_not_found))
        val earliestTx = transactionDao.earliestTimeForAccount(entity.id)
        if (earliestTx != null && newInitialBalanceTime > earliestTx) {
            return Result.failure(UserFacingException(R.string.err_initial_time_after_transactions))
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

    /** 归档 / 恢复（全局生效：归档后所有账本都不再提供该账户）。 */
    suspend fun setArchived(id: String, archived: Boolean): Result<Unit> {
        val entity = accountDao.getById(id) ?: return Result.failure(UserFacingException(R.string.err_account_not_found))
        return try {
            accountDao.update(entity.copy(isArchived = archived, updatedAt = TimeUtils.now()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
