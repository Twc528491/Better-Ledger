package com.personal.ledger.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.personal.ledger.R
import com.personal.ledger.util.LocaleContext
import com.personal.ledger.util.UserFacingException

/**
 * 需要持久化（数据库 / DataStore）、之后再显示给用户的消息：
 * 存稳定代码而不是已翻译文本，显示时按当前语言解析。
 *
 * 编码格式：`@msg:<CODE>`，可带一段详情 `@msg:<CODE>\u001F<detail>`（详情本身可再是编码消息或原文）。
 * 旧版本写入的中文原文没有前缀，[describe] 原样显示。
 *
 * 代码一经写入用户数据就不能改名；新增消息只能追加。
 */
enum class StoredMessage(@StringRes val res: Int) {
    // 备份警告（DataStore）
    BACKUP_CUSTOM_FOLDER_LOST(R.string.data_backup_custom_folder_lost),
    BACKUP_FAILED(R.string.data_backup_last_failed),
    BACKUP_UNKNOWN_ERROR(R.string.data_backup_unknown_error),

    // 定时扣款生成失败（recurring_charge.lastError）
    RECURRING_GENERATE_FAILED(R.string.err_recurring_generate_failed),
    RECURRING_LEDGER_UNAVAILABLE(R.string.err_recurring_ledger_unavailable),
    RECURRING_ACCOUNT_UNAVAILABLE(R.string.err_recurring_account_unavailable),
    RECURRING_CATEGORY_UNAVAILABLE(R.string.err_recurring_category_unavailable),

    // 交易校验（定时扣款生成交易时可能出现）
    AMOUNT_POSITIVE(R.string.err_amount_positive),
    AMOUNT_TOO_LARGE(R.string.err_amount_too_large),
    SELECT_CATEGORY(R.string.err_select_category),
    CATEGORY_TYPE_MISMATCH(R.string.err_category_type_mismatch),
    TRANSFER_NO_CATEGORY(R.string.err_transfer_no_category),
    SELECT_SOURCE_ACCOUNT(R.string.err_select_source_account),
    SELECT_TARGET_ACCOUNT(R.string.err_select_target_account),
    SAME_TRANSFER_ACCOUNTS(R.string.err_same_transfer_accounts),
    LEDGER_NOT_FOUND(R.string.err_ledger_not_found),
    CURRENCY_MISMATCH(R.string.err_currency_mismatch),
    ACCOUNT_NOT_FOUND(R.string.err_account_not_found),
    // v0.6 起账户跨账本共享，不再产生；保留以显示旧版本写入的 lastError。
    ACCOUNT_NOT_IN_LEDGER(R.string.err_account_not_in_ledger),
    TIME_BEFORE_ACCOUNT_START(R.string.err_time_before_account_start),
    CATEGORY_NOT_IN_LEDGER(R.string.err_category_not_in_ledger),
    TARGET_ACCOUNT_NOT_FOUND(R.string.err_target_account_not_found),
    // 同上，仅用于显示旧版本写入的 lastError。
    TARGET_ACCOUNT_NOT_IN_LEDGER(R.string.err_target_account_not_in_ledger),
    TIME_BEFORE_TARGET_START(R.string.err_time_before_target_start),
    MERCHANT_NOT_FOUND(R.string.err_merchant_not_found),
    MERCHANT_NOT_IN_LEDGER(R.string.err_merchant_not_in_ledger),
    TAG_NOT_FOUND(R.string.err_tag_not_found),
    TAG_NOT_IN_LEDGER(R.string.err_tag_not_in_ledger),

    // 备份存储（备份失败详情）
    DB_FILE_MISSING(R.string.err_db_file_missing),
    CREATE_BACKUP_DIR(R.string.err_create_backup_dir),
    WRITE_BACKUP_FILE(R.string.err_write_backup_file),
    CREATE_DEFAULT_BACKUP(R.string.err_create_default_backup),
    ACCESS_BACKUP_FOLDER(R.string.err_access_backup_folder),
    CREATE_IN_BACKUP_FOLDER(R.string.err_create_in_backup_folder),
    CUSTOM_FOLDER_INACCESSIBLE(R.string.err_custom_folder_inaccessible),

    // v0.6：账户跨账本共享后的交易校验
    ACCOUNT_CURRENCY_MISMATCH(R.string.err_account_currency_mismatch),
    TARGET_ACCOUNT_CURRENCY_MISMATCH(R.string.err_target_account_currency_mismatch),
    ;

    companion object {
        fun fromRes(@StringRes res: Int): StoredMessage? = entries.firstOrNull { it.res == res }
        fun fromCode(code: String): StoredMessage? = entries.firstOrNull { it.name == code }
    }
}

object StoredMessages {
    const val PREFIX = "@msg:"
    private const val SEPARATOR = '\u001F'

    fun encode(message: StoredMessage, detail: String? = null): String =
        PREFIX + message.name + (detail?.let { "$SEPARATOR$it" } ?: "")

    /**
     * 异常 → 可持久化文本：无参数的已登记 [UserFacingException] 存代码；
     * 其他异常存原始 message（系统文本，不翻译）；都没有时存 [fallback] 代码。
     */
    fun encode(error: Throwable, fallback: StoredMessage): String {
        if (error is UserFacingException) {
            val known = StoredMessage.fromRes(error.messageRes)
            return if (known != null && error.formatArgs.isEmpty()) encode(known) else encode(fallback)
        }
        return error.message?.takeIf { it.isNotBlank() } ?: encode(fallback)
    }

    fun isEncoded(stored: String?): Boolean = stored?.startsWith(PREFIX) == true

    /** 按当前应用语言显示；旧版中文原文与无法识别的代码原样 / 兜底显示。 */
    fun describe(context: Context, stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        val body = stored.removePrefix(PREFIX)
        val code = body.substringBefore(SEPARATOR)
        val detail = body.substringAfter(SEPARATOR, missingDelimiterValue = "").takeIf { SEPARATOR in body }
        val localized = LocaleContext.of(context)
        val message = StoredMessage.fromCode(code)
            ?: return localized.getString(R.string.error_unknown)
        return if (message == StoredMessage.BACKUP_FAILED) {
            val detailText = detail?.takeIf { it.isNotBlank() }?.let { describe(context, it) }
                ?: localized.getString(R.string.data_backup_unknown_error)
            localized.getString(message.res, detailText)
        } else {
            localized.getString(message.res)
        }
    }
}

/** 定时扣款 lastError 的显示（同时兼容旧版存入的中文原文）。 */
object RecurringErrors {
    fun describe(context: Context, stored: String): String = StoredMessages.describe(context, stored)
}

/** Compose 中显示持久化消息。 */
@Composable
fun storedMessageText(stored: String): String = StoredMessages.describe(LocalContext.current, stored)
