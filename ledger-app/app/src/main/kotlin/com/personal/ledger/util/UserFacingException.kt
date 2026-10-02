package com.personal.ledger.util

import android.content.Context
import androidx.annotation.StringRes
import com.personal.ledger.R

/**
 * 面向用户的错误：只携带字符串资源 ID 与格式参数，
 * 由 UI 层按当前应用语言解析（数据层不持有 Context，也不缓存已翻译文本）。
 */
class UserFacingException(
    @StringRes val messageRes: Int,
    vararg val formatArgs: Any,
) : IllegalArgumentException()

/** 解析错误文本：优先资源化消息，其次原始 message，最后通用兜底。 */
fun Throwable.userMessage(context: Context): String = when (this) {
    is UserFacingException -> context.getString(messageRes, *formatArgs)
    else -> message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.error_unknown)
}
