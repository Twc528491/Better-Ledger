package com.personal.ledger.data.settings

import android.content.Context
import com.personal.ledger.data.StoredMessages
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 主题模式：跟随系统 / 浅色 / 深色。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App 设置（DataStore）。属于 App 设置，不参与备份与恢复。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val CURRENT_LEDGER_ID = stringPreferencesKey("current_ledger_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SECURITY_LOCK_ENABLED = booleanPreferencesKey("security_lock_enabled")
        val BACKUP_DIR_URI = stringPreferencesKey("backup_dir_uri")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_USED_ACCOUNT_ID = stringPreferencesKey("last_used_account_id")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_WARNING = stringPreferencesKey("last_backup_warning")
    }

    val currentLedgerId: Flow<String?> =
        context.dataStore.data.map { it[Keys.CURRENT_LEDGER_ID] }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val securityLockEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SECURITY_LOCK_ENABLED] ?: false }

    val backupDirUri: Flow<String?> =
        context.dataStore.data.map { it[Keys.BACKUP_DIR_URI] }

    val autoBackupEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_BACKUP_ENABLED] ?: false }

    val lastUsedAccountId: Flow<String?> =
        context.dataStore.data.map { it[Keys.LAST_USED_ACCOUNT_ID] }

    /** 最近一次成功写入备份（自动或立即备份）的时间；从未备份为 null。 */
    val lastBackupAt: Flow<Long?> =
        context.dataStore.data.map { it[Keys.LAST_BACKUP_AT] }

    /** 最近一次备份的警告 / 失败说明（如自定义文件夹不可用已改存默认目录）；正常时为 null。 */
    /**
     * 最近一次备份警告，已按当前应用语言解析（存储的是 [StoredMessages] 代码；旧版中文原文原样显示）。
     * 切换语言会重建界面并重新收集，因此总是显示当前语言。
     */
    val lastBackupWarning: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.LAST_BACKUP_WARNING]?.let { StoredMessages.describe(context, it) }
        }

    suspend fun setCurrentLedgerId(id: String?) {
        context.dataStore.edit { if (id == null) it.remove(Keys.CURRENT_LEDGER_ID) else it[Keys.CURRENT_LEDGER_ID] = id }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setSecurityLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SECURITY_LOCK_ENABLED] = enabled }
    }

    suspend fun setBackupDirUri(uri: String?) {
        context.dataStore.edit { if (uri == null) it.remove(Keys.BACKUP_DIR_URI) else it[Keys.BACKUP_DIR_URI] = uri }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastUsedAccountId(id: String?) {
        context.dataStore.edit { if (id == null) it.remove(Keys.LAST_USED_ACCOUNT_ID) else it[Keys.LAST_USED_ACCOUNT_ID] = id }
    }

    /**
     * 记录一次备份结果：[succeededAt] 非空时更新最近备份时间；[warning] 为 null 时清除警告。
     * [warning] 应为 [StoredMessages.encode] 生成的代码，显示时再按当前语言解析。
     */
    suspend fun recordBackupResult(succeededAt: Long?, warning: String?) {
        context.dataStore.edit {
            if (succeededAt != null) it[Keys.LAST_BACKUP_AT] = succeededAt
            if (warning == null) it.remove(Keys.LAST_BACKUP_WARNING) else it[Keys.LAST_BACKUP_WARNING] = warning
        }
    }
}
