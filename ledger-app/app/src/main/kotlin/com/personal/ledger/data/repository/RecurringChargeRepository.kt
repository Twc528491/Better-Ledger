package com.personal.ledger.data.repository

import androidx.room.withTransaction
import com.personal.ledger.R
import com.personal.ledger.data.StoredMessage
import com.personal.ledger.data.StoredMessages
import com.personal.ledger.data.TextResolver
import com.personal.ledger.data.local.database.AppDatabase
import com.personal.ledger.data.local.entity.RecurringChargeEntity
import com.personal.ledger.data.local.entity.RecurringOccurrenceEntity
import com.personal.ledger.domain.calc.RecurringSchedule
import com.personal.ledger.domain.model.CategoryType
import com.personal.ledger.domain.model.RecurrenceFrequency
import com.personal.ledger.domain.model.RecurringCharge
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.util.UserFacingException
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

/**
 * Subscription rules create ordinary validated expense rows. The occurrence marker, inserted
 * transaction, and next schedule position share one SQLite transaction so retries are idempotent.
 */
class RecurringChargeRepository(
    private val database: AppDatabase,
    private val transactionRepository: TransactionRepository,
    /** 生成交易备注的语言；为 null（测试）时使用中文。 */
    private val text: TextResolver? = null,
) {
    private val dao = database.recurringChargeDao()

    fun observeByLedger(ledgerId: String): Flow<List<RecurringCharge>> =
        dao.observeByLedger(ledgerId).map { entities -> entities.map(RecurringChargeEntity::toDomain) }

    suspend fun save(id: String?, draft: RecurringChargeDraft): Result<RecurringCharge> =
        resultOf {
            database.withTransaction {
                validateDraft(draft)
                val existing = id?.let { dao.getById(it) }
                    ?: if (id == null) null else throw UserFacingException(R.string.err_recurring_not_found)
                if (existing != null && existing.ledgerId != draft.ledgerId) {
                    throw UserFacingException(R.string.err_recurring_move_ledger)
                }
                val now = TimeUtils.now()
                val scheduleChanged = existing == null ||
                    existing.frequency != draft.frequency.name ||
                    existing.startDate != draft.startDate.toString() ||
                    existing.minuteOfDay != draft.minuteOfDay ||
                    existing.zoneId != draft.zoneId.id
                val account = database.accountDao().getById(draft.accountId)
                    ?: throw UserFacingException(R.string.err_account_not_found)
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
            val entity = dao.getById(id) ?: throw UserFacingException(R.string.err_recurring_not_found)
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
            val entity = dao.getById(id) ?: throw UserFacingException(R.string.err_recurring_not_found)
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
                            throw UserFacingException(R.string.err_recurring_ledger_unavailable)
                        }
                        val account = database.accountDao().getById(rule.accountId)
                        if (account == null || account.ledgerId != rule.ledgerId || account.isArchived) {
                            throw UserFacingException(R.string.err_recurring_account_unavailable)
                        }
                        val category = database.categoryDao().getById(rule.categoryId)
                        if (category == null || category.ledgerId != rule.ledgerId ||
                            category.type != CategoryType.EXPENSE || category.isArchived
                        ) {
                            throw UserFacingException(R.string.err_recurring_category_unavailable)
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
                                note = recurringNote(rule.name),
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
                    persistError(candidate, StoredMessages.encode(error, StoredMessage.RECURRING_GENERATE_FAILED))
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
        if (name.isEmpty()) throw UserFacingException(R.string.err_name_empty)
        if (name.length > MAX_NAME_LENGTH) throw UserFacingException(R.string.err_name_too_long, MAX_NAME_LENGTH)
        if (draft.amount <= 0) throw UserFacingException(R.string.err_amount_positive)
        if (draft.minuteOfDay !in 0..1439) throw UserFacingException(R.string.err_select_valid_time)
        val ledger = database.ledgerDao().getById(draft.ledgerId)
            ?: throw UserFacingException(R.string.err_ledger_not_found)
        if (ledger.isArchived) throw UserFacingException(R.string.err_ledger_archived)
        val account = database.accountDao().getById(draft.accountId)
            ?: throw UserFacingException(R.string.err_account_not_found)
        if (account.ledgerId != draft.ledgerId) throw UserFacingException(R.string.err_account_not_in_ledger)
        if (account.isArchived) throw UserFacingException(R.string.err_account_archived_unusable)
        if (account.currency != ledger.baseCurrency) throw UserFacingException(R.string.err_account_currency_mismatch)
        val category = database.categoryDao().getById(draft.categoryId)
            ?: throw UserFacingException(R.string.err_expense_category_not_found)
        if (category.ledgerId != draft.ledgerId) throw UserFacingException(R.string.err_category_not_in_ledger)
        if (category.type != CategoryType.EXPENSE) throw UserFacingException(R.string.err_select_expense_category)
        if (category.isArchived) throw UserFacingException(R.string.err_category_archived_unusable)
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

    /** 生成交易的备注按生成时的应用语言写入（之后是用户数据，不再翻译）。 */
    private fun recurringNote(name: String): String =
        text?.get(R.string.data_recurring_note, name) ?: (DEFAULT_NOTE_PREFIX + name)

    /**
     * [message] 为 [StoredMessages] 编码（或系统异常原文），界面用
     * [com.personal.ledger.data.RecurringErrors.describe] 按当前语言显示。
     */
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
        const val MAX_NAME_LENGTH = 80

        /** 未注入 [TextResolver] 时的备注前缀（与中文资源 data_recurring_note 一致）。 */
        private const val DEFAULT_NOTE_PREFIX = "定时扣款："
    }
}
