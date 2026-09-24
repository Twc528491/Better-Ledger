package com.personal.ledger.data.repository

import androidx.room.withTransaction
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.RecurringChargeEntity
import com.personal.ledger.data.local.entity.RecurringOccurrenceEntity
import com.personal.ledger.domain.calc.RecurringSchedule
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.RecurrenceFrequency
import com.personal.ledger.domain.model.RecurringCharge
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class RecurringChargeDraft(
    val ledgerId: String,
    val name: String,
    val amount: Long,
    val accountId: String,
    val categoryId: String,
    val frequency: RecurrenceFrequency,
    val startDate: LocalDate,
    val minuteOfDay: Int,
    val zoneId: ZoneId,
)

data class RecurringProcessResult(val processed: Int, val hasMoreDue: Boolean)

private class RecurringDependencyException(message: String) : IllegalStateException(message)

/**
 * Subscription rules create ordinary validated expense rows. The occurrence marker, inserted
 * transaction, and next schedule position share one SQLite transaction so retries are idempotent.
 */
class RecurringChargeRepository(
    private val database: AppDatabase,
    private val transactionRepository: TransactionRepository,
) {
    private val dao = database.recurringChargeDao()

    fun observeByLedger(ledgerId: String): Flow<List<RecurringCharge>> =
        dao.observeByLedger(ledgerId).map { entities -> entities.map(RecurringChargeEntity::toDomain) }

    suspend fun save(id: String?, draft: RecurringChargeDraft): Result<RecurringCharge> =
        resultOf {
            database.withTransaction {
                validateDraft(draft)
                val existing = id?.let { dao.getById(it) }
                    ?: if (id == null) null else throw NoSuchElementException("定时扣款不存在")
                if (existing != null && existing.ledgerId != draft.ledgerId) {
                    throw IllegalArgumentException("定时扣款不能移动到其他账本")
                }
                val now = TimeUtils.now()
                val scheduleChanged = existing == null ||
                    existing.frequency != draft.frequency.name ||
                    existing.startDate != draft.startDate.toString() ||
                    existing.minuteOfDay != draft.minuteOfDay ||
                    existing.zoneId != draft.zoneId.id
                val account = database.accountDao().getById(draft.accountId)
                    ?: throw IllegalArgumentException("账户不存在")
                val index = if (existing == null) {
                    RecurringSchedule.firstIndexAtOrAfter(
                        draft.startDate,
                        draft.minuteOfDay,
                        draft.zoneId,
                        draft.frequency,
                        account.initialBalanceTime,
                    )
                } else if (scheduleChanged) {
                    RecurringSchedule.firstIndexAtOrAfter(
                        draft.startDate,
                        draft.minuteOfDay,
                        draft.zoneId,
                        draft.frequency,
                        maxOf(now, account.initialBalanceTime),
                    )
                } else {
                    existing.nextOccurrenceIndex
                }
                val entity = RecurringChargeEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    ledgerId = draft.ledgerId,
                    name = draft.name.trim(),
                    amount = draft.amount,
                    currency = database.ledgerDao().getById(draft.ledgerId)!!.baseCurrency,
                    accountId = draft.accountId,
                    categoryId = draft.categoryId,
                    frequency = draft.frequency.name,
                    startDate = draft.startDate.toString(),
                    minuteOfDay = draft.minuteOfDay,
                    zoneId = draft.zoneId.id,
                    nextOccurrenceIndex = index,
                    nextDueAt = RecurringSchedule.dueAt(
                        draft.startDate, draft.minuteOfDay, draft.zoneId, draft.frequency, index,
                    ),
                    isPaused = existing?.isPaused ?: false,
                    lastError = null,
                    updatedAt = now,
                )
                if (existing == null) dao.insert(entity) else dao.update(entity)
                entity.toDomain()
            }
        }

    suspend fun setPaused(id: String, paused: Boolean): Result<Unit> = resultOf {
        database.withTransaction {
            val entity = dao.getById(id) ?: throw NoSuchElementException("定时扣款不存在")
            val now = TimeUtils.now()
            if (paused) {
                dao.update(entity.copy(isPaused = true, updatedAt = now))
            } else {
                val zone = ZoneId.of(entity.zoneId)
                val index = RecurringSchedule.firstIndexAtOrAfter(
                    LocalDate.parse(entity.startDate),
                    entity.minuteOfDay,
                    zone,
                    RecurrenceFrequency.valueOf(entity.frequency),
                    now,
                    entity.nextOccurrenceIndex,
                )
                dao.update(
                    entity.copy(
                        isPaused = false,
                        nextOccurrenceIndex = index,
                        nextDueAt = RecurringSchedule.dueAt(
                            LocalDate.parse(entity.startDate), entity.minuteOfDay, zone,
                            RecurrenceFrequency.valueOf(entity.frequency), index,
                        ),
                        lastError = null,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    suspend fun retry(id: String): Result<Unit> = resultOf {
        database.withTransaction {
            val entity = dao.getById(id) ?: throw NoSuchElementException("定时扣款不存在")
            dao.update(entity.copy(lastError = null, updatedAt = TimeUtils.now()))
        }
    }

    suspend fun delete(id: String): Result<Unit> = resultOf {
        dao.deleteById(id)
    }

    suspend fun processDue(nowMs: Long = TimeUtils.now(), limit: Int = MAX_OCCURRENCES_PER_RUN): RecurringProcessResult {
        require(limit > 0)
        var processed = 0
        while (processed < limit) {
            val candidates = dao.getDue(nowMs, limit - processed)
            if (candidates.isEmpty()) break
            var madeProgress = false
            for (candidate in candidates) {
                if (processed >= limit) break
                try {
                    val outcome = database.withTransaction {
                        val rule = dao.getById(candidate.id) ?: return@withTransaction false
                        if (rule.isPaused || rule.lastError != null || rule.nextDueAt > nowMs) {
                            return@withTransaction false
                        }
                        val dueAt = rule.nextDueAt
                        if (dao.getOccurrence(rule.id, dueAt) != null) {
                            advance(rule)
                            return@withTransaction true
                        }
                        val ledger = database.ledgerDao().getById(rule.ledgerId)
                        if (ledger == null || ledger.isArchived) {
                            throw RecurringDependencyException("账本已归档或不存在，请恢复账本后重试")
                        }
                        val account = database.accountDao().getById(rule.accountId)
                        if (account == null || account.ledgerId != rule.ledgerId || account.isArchived) {
                            throw RecurringDependencyException("账户已归档或不可用，请恢复账户或编辑扣款规则")
                        }
                        val category = database.categoryDao().getById(rule.categoryId)
                        if (category == null || category.ledgerId != rule.ledgerId ||
                            category.type != CategoryType.EXPENSE || category.isArchived
                        ) {
                            throw RecurringDependencyException("支出分类已归档或不可用，请恢复分类或编辑扣款规则")
                        }
                        if (dueAt < account.initialBalanceTime) {
                            val zone = ZoneId.of(rule.zoneId)
                            val frequency = RecurrenceFrequency.valueOf(rule.frequency)
                            val index = RecurringSchedule.firstIndexAtOrAfter(
                                LocalDate.parse(rule.startDate), rule.minuteOfDay, zone, frequency,
                                account.initialBalanceTime, rule.nextOccurrenceIndex,
                            )
                            dao.update(
                                rule.copy(
                                    nextOccurrenceIndex = index,
                                    nextDueAt = RecurringSchedule.dueAt(
                                        LocalDate.parse(rule.startDate), rule.minuteOfDay, zone, frequency, index,
                                    ),
                                    updatedAt = TimeUtils.now(),
                                ),
                            )
                            return@withTransaction true
                        }
                        val added = transactionRepository.add(
                            TransactionDraft(
                                ledgerId = rule.ledgerId,
                                type = TransactionType.EXPENSE,
                                amount = rule.amount,
                                currency = rule.currency,
                                accountId = rule.accountId,
                                categoryId = rule.categoryId,
                                note = "定时扣款：${rule.name}",
                                transactionTime = dueAt,
                            ),
                        )
                        val transaction = added.getOrElse { error ->
                            if (error is CancellationException) throw error
                            throw error
                        }
                        // This primary key remains even if the user soft-deletes the generated row.
                        dao.insertOccurrence(
                            RecurringOccurrenceEntity(ruleId = rule.id, dueAt = dueAt, transactionId = transaction.id),
                        )
                        advance(rule)
                        true
                    }
                    if (outcome) {
                        processed++
                        madeProgress = true
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    persistError(candidate, error.message ?: "生成扣款失败")
                    processed++
                    madeProgress = true
                }
            }
            if (!madeProgress) break
        }
        return RecurringProcessResult(processed, dao.hasDue(nowMs))
    }

    private suspend fun validateDraft(draft: RecurringChargeDraft) {
        val name = draft.name.trim()
        if (name.isEmpty()) throw IllegalArgumentException("名称不能为空")
        if (name.length > 80) throw IllegalArgumentException("名称不能超过 80 个字符")
        if (draft.amount <= 0) throw IllegalArgumentException("金额必须大于 0")
        if (draft.minuteOfDay !in 0..1439) throw IllegalArgumentException("请选择有效的扣款时间")
        val ledger = database.ledgerDao().getById(draft.ledgerId)
            ?: throw IllegalArgumentException("账本不存在")
        if (ledger.isArchived) throw IllegalArgumentException("账本已归档")
        val account = database.accountDao().getById(draft.accountId)
            ?: throw IllegalArgumentException("账户不存在")
        if (account.ledgerId != draft.ledgerId) throw IllegalArgumentException("账户不属于当前账本")
        if (account.isArchived) throw IllegalArgumentException("不能使用已归档账户")
        if (account.currency != ledger.baseCurrency) throw IllegalArgumentException("账户币种与账本不一致")
        val category = database.categoryDao().getById(draft.categoryId)
            ?: throw IllegalArgumentException("支出分类不存在")
        if (category.ledgerId != draft.ledgerId) throw IllegalArgumentException("分类不属于当前账本")
        if (category.type != CategoryType.EXPENSE) throw IllegalArgumentException("请选择支出分类")
        if (category.isArchived) throw IllegalArgumentException("不能使用已归档分类")
    }

    private suspend fun advance(rule: RecurringChargeEntity) {
        val nextIndex = Math.addExact(rule.nextOccurrenceIndex, 1L)
        val anchor = LocalDate.parse(rule.startDate)
        val zone = ZoneId.of(rule.zoneId)
        val frequency = RecurrenceFrequency.valueOf(rule.frequency)
        dao.update(
            rule.copy(
                nextOccurrenceIndex = nextIndex,
                nextDueAt = RecurringSchedule.dueAt(anchor, rule.minuteOfDay, zone, frequency, nextIndex),
                lastError = null,
                updatedAt = TimeUtils.now(),
            ),
        )
    }

    private suspend fun persistError(candidate: RecurringChargeEntity, message: String) {
        database.withTransaction {
            dao.getById(candidate.id)?.let { current ->
                // A user may have fixed/rescheduled this rule while the failed attempt unwound.
                if (current.nextDueAt == candidate.nextDueAt &&
                    current.updatedAt == candidate.updatedAt &&
                    !current.isPaused &&
                    current.lastError == null
                ) {
                    dao.update(current.copy(lastError = message.take(300), updatedAt = TimeUtils.now()))
                }
            }
        }
    }

    private suspend fun <T> resultOf(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }

    companion object {
        const val MAX_OCCURRENCES_PER_RUN = 200
    }
}
