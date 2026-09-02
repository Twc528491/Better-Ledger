package com.personal.ledger.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
}
