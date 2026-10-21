package io.github.twc528491.betterledger.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 不持有 Context 的可本地化文本：ViewModel / 数据层用它代替已翻译的 String，
 * 由 UI 在组合时按当前语言解析。
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    /** 用户数据或系统返回的原文（如分类名、异常原文），不翻译。 */
    data class Raw(val text: String) : UiText

    fun resolve(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
        is Raw -> text
    }

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

@Composable
fun UiText.asString(): String = resolve(LocalContext.current)

/** 异常 → UiText：资源化异常取其资源；有原文时保留原文；否则用 [fallback]。 */
fun Throwable.toUiText(@StringRes fallback: Int): UiText = when (this) {
    is UserFacingException -> UiText.Res(messageRes, formatArgs.toList())
    else -> message?.takeIf { it.isNotBlank() }?.let(UiText::Raw) ?: UiText.Res(fallback)
}
