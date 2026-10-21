package io.github.twc528491.betterledger.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList

/**
 * 按“当前应用语言”取资源的 Context，供数据层 / Worker 等只持有 Application Context 的地方使用。
 *
 * - Android 13+：系统把应用语言应用到整个进程（含 Application Context），原样返回。
 * - Android 8–12：Application 的 Context 在进程启动时按当时的语言包装，之后在设置里切换语言
 *   只会 recreate Activity；这里按 [AppLanguage] 保存的选择重新生成配置 Context（结果按语言缓存）。
 */
object LocaleContext {

    @Volatile
    private var cached: Pair<LocaleList, Context>? = null

    fun of(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val locales = AppLanguage.current(context).tag
            ?.let { LocaleList.forLanguageTags(it) }
            ?: Resources.getSystem().configuration.locales
        if (context.resources.configuration.locales == locales) return context
        cached?.let { (cachedLocales, localized) -> if (cachedLocales == locales) return localized }
        val config = Configuration(context.resources.configuration).apply { setLocales(locales) }
        return context.createConfigurationContext(config).also { cached = locales to it }
    }

    fun getString(context: Context, id: Int, vararg args: Any): String =
        of(context).getString(id, *args)
}
