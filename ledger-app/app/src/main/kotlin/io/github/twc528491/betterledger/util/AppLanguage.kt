package io.github.twc528491.betterledger.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 应用内语言（跟随系统 / 简体中文 / English）。
 *
 * - Android 13+：交给系统 LocaleManager 保存，系统“应用语言”设置里也能看到并修改；
 *   切换后系统会自动重建 Activity。
 * - Android 8–12：存到 SharedPreferences（attachBaseContext 需要同步读取，不能用 DataStore），
 *   由 [wrap] 在 Application / Activity 的 attachBaseContext 中套用，切换后手动 recreate。
 *
 * 属于 App 设置，不参与备份与恢复。
 */
object AppLanguage {

    enum class Option(val tag: String?) {
        SYSTEM(null),
        CHINESE("zh-CN"),
        ENGLISH("en"),
    }

    private const val PREFS = "app_language"
    private const val KEY_TAG = "language_tag"

    fun current(context: Context): Option {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeIf { !it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
        } else {
            storedTag(context)
        }
        return fromTag(tag)
    }

    fun apply(activity: Activity, option: Option) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                option.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .apply { if (option.tag == null) remove(KEY_TAG) else putString(KEY_TAG, option.tag) }
                .commit()
            activity.recreate()
        }
    }

    /** Android 12 及以下：按已保存的语言包装 Context；13+ 由系统处理，原样返回。 */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = storedTag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    internal fun fromTag(tag: String?): Option = when {
        tag.isNullOrBlank() -> Option.SYSTEM
        tag.startsWith("zh", ignoreCase = true) -> Option.CHINESE
        tag.startsWith("en", ignoreCase = true) -> Option.ENGLISH
        else -> Option.SYSTEM
    }

    private fun storedTag(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)
}
