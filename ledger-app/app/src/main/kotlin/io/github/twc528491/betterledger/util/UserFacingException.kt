package io.github.twc528491.betterledger.util

import android.content.Context
import androidx.annotation.StringRes
import io.github.twc528491.betterledger.R

/**
 * 面向用户的错误：只携带字符串资源 ID 与格式参数，
 * 由 UI 层按当前应用语言解析（数据层不持有 Context，也不缓存已翻译文本）。
 * 格式参数可以是 [UiText]，解析时会递归展开。
 */
class UserFacingException(
    @StringRes val messageRes: Int,
    vararg val formatArgs: Any,
) : IllegalArgumentException()

/** 解析错误文本：优先资源化消息，其次原始 message，最后通用兜底。 */
fun Throwable.userMessage(context: Context, @StringRes fallback: Int = R.string.error_unknown): String =
    toUiText(fallback).resolve(context)
