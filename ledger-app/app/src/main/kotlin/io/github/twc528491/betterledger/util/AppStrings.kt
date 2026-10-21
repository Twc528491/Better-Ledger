package io.github.twc528491.betterledger.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * 进程级字符串解析（按当前应用语言）。供少数无法拿到 Context、又需保持既有签名的纯函数使用
 * （如 [io.github.twc528491.betterledger.data.backup.BackupCatalog] 的显示文案）。
 * 由 [io.github.twc528491.betterledger.LedgerApplication.onCreate] 初始化；新代码优先在 UI 层用 stringResource。
 */
object AppStrings {

    @Volatile
    private var app: Context? = null

    fun init(context: Context) {
        app = context.applicationContext ?: context
    }

    fun get(@StringRes id: Int, vararg args: Any): String {
        val context = checkNotNull(app) { "AppStrings 尚未初始化" }
        return LocaleContext.getString(context, id, *args)
    }
}
