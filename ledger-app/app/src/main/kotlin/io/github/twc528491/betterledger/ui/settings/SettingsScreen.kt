package io.github.twc528491.betterledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.data.settings.ThemeMode
import io.github.twc528491.betterledger.navigation.Routes
import io.github.twc528491.betterledger.ui.common.LedgerGroup
import io.github.twc528491.betterledger.ui.common.LedgerIconTile
import io.github.twc528491.betterledger.ui.common.LedgerListRow
import io.github.twc528491.betterledger.ui.common.rememberHapticLight
import io.github.twc528491.betterledger.ui.common.rememberHapticNavigation
import io.github.twc528491.betterledger.ui.theme.LedgerSpacing
import io.github.twc528491.betterledger.util.AppLanguage
import io.github.twc528491.betterledger.viewmodel.SettingsViewModel

/**
 * 设置总入口：记账项、外观与安全、数据管理均从这里进入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, navController: NavHostController) {
    val vm: SettingsViewModel = viewModel {
        SettingsViewModel(container.ledgerRepository, container.settings)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    // 语言切换会重建 Activity，这里读取一次即可。
    val language = remember { AppLanguage.current(context) }
    val hapticLight = rememberHapticLight()
    val hapticNavigation = rememberHapticNavigation()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticNavigation()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.md,
                top = LedgerSpacing.xs,
                end = LedgerSpacing.md,
                bottom = LedgerSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.lg),
        ) {
            item {
                LedgerGroup(title = stringResource(R.string.set_group_bookkeeping)) {
                    LedgerListRow(
                        title = stringResource(R.string.set_bookkeeping_title),
                        subtitle = stringResource(R.string.set_bookkeeping_subtitle),
                        leading = { LedgerIconTile(Icons.AutoMirrored.Outlined.ReceiptLong) },
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            navController.navigate(Routes.BOOKKEEPING_SETTINGS)
                        },
                    )
                    LedgerListRow(
                        title = stringResource(R.string.set_recurring_title),
                        subtitle = stringResource(R.string.set_recurring_entry_subtitle),
                        leading = { LedgerIconTile(Icons.Outlined.EventRepeat) },
                        showChevron = true,
                        onClick = {
                            navController.navigate(Routes.RECURRING_CHARGES)
                        },
                    )
                }
            }

            item {
                LedgerGroup(title = stringResource(R.string.set_group_data)) {
                    LedgerListRow(
                        title = stringResource(R.string.set_data_title),
                        subtitle = stringResource(R.string.set_data_subtitle),
                        leading = { LedgerIconTile(Icons.Outlined.Storage) },
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            navController.navigate(Routes.DATA)
                        },
                    )
                }
            }

            item {
                LedgerGroup(title = stringResource(R.string.set_group_app)) {
                    LedgerListRow(
                        title = stringResource(R.string.set_dark_mode),
                        leading = { LedgerIconTile(Icons.Outlined.DarkMode) },
                        value = themeLabel(state.themeMode),
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            showThemeDialog = true
                        },
                    )
                    LedgerListRow(
                        title = stringResource(R.string.settings_language),
                        leading = { LedgerIconTile(Icons.Outlined.Language) },
                        value = languageLabel(language),
                        showChevron = true,
                        onClick = {
                            hapticNavigation()
                            showLanguageDialog = true
                        },
                    )
                    LedgerListRow(
                        title = stringResource(R.string.set_security_lock),
                        subtitle = stringResource(R.string.set_security_lock_subtitle),
                        leading = { LedgerIconTile(Icons.Outlined.Lock) },
                        trailing = {
                            Switch(
                                checked = state.securityLockEnabled,
                                onCheckedChange = { enabled ->
                                    hapticLight()
                                    if (enabled) {
                                        if (activity != null) {
                                            io.github.twc528491.betterledger.ui.security.promptForEnrollment(
                                                activity = activity,
                                                onSuccess = { vm.setSecurityLockEnabled(true) },
                                            )
                                        } else {
                                            vm.setSecurityLockEnabled(true)
                                        }
                                    } else {
                                        vm.setSecurityLockEnabled(false)
                                    }
                                },
                            )
                        },
                    )
                    LedgerListRow(
                        title = stringResource(R.string.set_about),
                        leading = { LedgerIconTile(Icons.Outlined.Info) },
                        value = stringResource(
                            R.string.set_about_version,
                            stringResource(R.string.app_name),
                            io.github.twc528491.betterledger.BuildConfig.VERSION_NAME,
                        ),
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    AppLanguage.Option.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticLight()
                                    showLanguageDialog = false
                                    if (option != language && activity != null) {
                                        AppLanguage.apply(activity, option)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = language == option, onClick = null)
                            Text(languageLabel(option), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(
                        stringResource(R.string.language_note),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.set_dark_mode)) },
            text = {
                Column {
                    listOf(
                        ThemeMode.SYSTEM,
                        ThemeMode.LIGHT,
                        ThemeMode.DARK,
                    ).forEach { mode ->
                        val label = themeLabel(mode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticLight()
                                    vm.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = null,
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}

@Composable
private fun languageLabel(option: AppLanguage.Option): String = when (option) {
    AppLanguage.Option.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.Option.CHINESE -> stringResource(R.string.language_chinese)
    AppLanguage.Option.ENGLISH -> stringResource(R.string.language_english)
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.set_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.set_theme_light)
    ThemeMode.DARK -> stringResource(R.string.set_theme_dark)
}
