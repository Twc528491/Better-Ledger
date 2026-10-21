package io.github.twc528491.betterledger.data

import android.content.Context
import androidx.annotation.StringRes
import io.github.twc528491.betterledger.util.LocaleContext

/**
 * 数据层按当前应用语言取字符串（预置分类名、定时扣款备注、CSV 表头等“生成时定稿”的文本）。
 * Repository 中为可空参数：测试里不传时使用中文原始种子值。
 */
fun interface TextResolver {
    fun get(@StringRes id: Int, vararg args: Any): String

    companion object {
        /** 使用 Application Context，每次调用时按当前应用语言解析。 */
        fun of(context: Context): TextResolver {
            val app = context.applicationContext ?: context
            return TextResolver { id, args -> LocaleContext.getString(app, id, *args) }
        }
    }
}
