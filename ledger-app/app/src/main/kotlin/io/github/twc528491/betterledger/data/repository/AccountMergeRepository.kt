package io.github.twc528491.betterledger.data.repository

import androidx.room.withTransaction
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.local.database.AppDatabase
import io.github.twc528491.betterledger.data.local.database.RecurringDatabaseGate
import io.github.twc528491.betterledger.data.local.entity.AccountEntity
import io.github.twc528491.betterledger.data.settings.SettingsRepository
import io.github.twc528491.betterledger.domain.calc.AccountRules
import io.github.twc528491.betterledger.domain.model.Account
import io.github.twc528491.betterledger.util.TimeUtils
import io.github.twc528491.betterledger.util.UserFacingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

/** 合并前的影响评估（均跨全部账本、含回收站）。 */
data class AccountMergePlan(
    val source: Account,
    val target: Account,
    /** 改挂到目标账户的交易数。 */
    val movedTransactions: Int,
    /** 源与目标之间的转账数：合并后会成为自己转给自己，将被删除。 */
    val selfTransfers: Int,
    /** 改用目标账户的定时扣款数。 */
    val recurringCharges: Int,
    /** 合并后目标账户的初始余额 / 初始时间。 */
    val mergedInitialBalance: Long,
    val mergedInitialTime: Long,
)

/**
 * 合并账户（“合并到…”）：把源账户的全部交易（含回收站，作为账户或转入账户）与定时扣款
 * 改挂到目标账户；目标初始余额加上源初始余额、初始时间取较早者；随后删除源账户。
 * 两者之间的转账合并后会变成自己转给自己（净额为零），直接删除。
 *
 * 单个数据库事务内完成，并持有 [RecurringDatabaseGate] 避免与后台生成定时扣款交错。不可撤销。
 */
class AccountMergeRepository(
    private val database: AppDatabase,
    /** 用于把“最近使用账户”从源账户改为目标账户；测试可为 null。 */
    private val settings: SettingsRepository? = null,
) {
    private val dao = database.accountMergeDao()

    /** 计算合并影响；校验失败时抛出 [UserFacingException]。 */
    suspend fun plan(sourceId: String, targetId: String): AccountMergePlan {
        val source = database.accountDao().getById(sourceId)
        val target = database.accountDao().getById(targetId)
        return buildPlan(source, target).first
    }

    suspend fun merge(sourceId: String, targetId: String): Result<AccountMergePlan> = try {
        val plan = RecurringDatabaseGate.mutex.withLock {
            database.withTransaction {
                val source = database.accountDao().getById(sourceId)
                val target = database.accountDao().getById(targetId)
                val (plan, selfIds) = buildPlan(source, target)
                val now = TimeUtils.now()
                val txDao = database.transactionDao()

                // 1. 源 ↔ 目标之间的转账：先清理依赖行再删除（含回收站）。
                selfIds.chunked(BATCH_CHUNK).forEach { chunk ->
                    txDao.deleteTagLinksOf(chunk)
                    txDao.deleteRecurringOccurrencesOf(chunk)
                    dao.deleteTransactions(chunk)
                }
                // 2. 其余交易与定时扣款改挂到目标账户。
                dao.moveSourceAccount(sourceId, targetId, now)
                dao.moveTargetAccount(sourceId, targetId, now)
                dao.moveRecurring(sourceId, targetId, now)
                // 3. 目标账户吸收源账户的初始余额，初始时间取较早者；删除源账户。
                database.accountDao().update(
                    target!!.copy(
                        initialBalance = plan.mergedInitialBalance,
                        initialBalanceTime = plan.mergedInitialTime,
                        updatedAt = now,
                    ),
                )
                dao.deleteAccount(sourceId)
                plan
            }
        }
        settings?.let { s ->
            runCatching { if (s.lastUsedAccountId.first() == sourceId) s.setLastUsedAccountId(targetId) }
        }
        Result.success(plan)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** @return 影响评估 + 将被删除的源 ↔ 目标转账 ID。 */
    private suspend fun buildPlan(
        source: AccountEntity?,
        target: AccountEntity?,
    ): Pair<AccountMergePlan, List<String>> {
        AccountRules.validateMerge(source?.toDomain(), target?.toDomain())?.let { throw UserFacingException(it) }
        source!!
        target!!
        val txPlan = AccountRules.mergePlan(
            source.id,
            target.id,
            database.transactionDao().getByAccount(source.id).map {
                AccountRules.MergeTx(it.id, it.type, it.accountId, it.targetAccountId)
            },
        )
        val opening = try {
            AccountRules.mergedOpening(
                source.initialBalance, source.initialBalanceTime,
                target.initialBalance, target.initialBalanceTime,
            )
        } catch (_: ArithmeticException) {
            throw UserFacingException(R.string.err_amount_too_large)
        }
        val plan = AccountMergePlan(
            source = source.toDomain(),
            target = target.toDomain(),
            movedTransactions = txPlan.movedIds.size,
            selfTransfers = txPlan.selfTransferIds.size,
            recurringCharges = dao.countRecurringForAccount(source.id),
            mergedInitialBalance = opening.first,
            mergedInitialTime = opening.second,
        )
        return plan to txPlan.selfTransferIds
    }

    private companion object {
        /** 低于旧版 SQLite 999 个绑定参数上限，留出余量。 */
        const val BATCH_CHUNK = 500
    }
}
