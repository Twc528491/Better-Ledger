package io.github.twc528491.betterledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.util.DateLabels
import java.time.YearMonth

/** 各主页面顶栏统一的设置入口：同一图标、同一位置（最右侧）。 */
@Composable
fun SettingsActionButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.action_settings))
    }
}

/** 顶栏月份切换：左右箭头直接切月，点击中间月份打开月份面板。 */
@Composable
fun MonthSwitcher(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPicker: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.core_previous_month))
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClickLabel = stringResource(R.string.core_choose_month), onClick = onOpenPicker)
                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = LedgerNumerals.of(MaterialTheme.typography.titleLarge))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.core_next_month))
        }
    }
}

/** 月份面板：年份左右切换 + 3×4 大尺寸月份格；当前月描边，已选月实心。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerSheet(
    current: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember { mutableStateOf(current.year) }
    val thisMonth = remember { YearMonth.now() }
    val locale = LocalConfiguration.current.locales[0]
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { year-- }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.core_previous_year))
                }
                Text(
                    stringResource(R.string.core_year, year),
                    style = LedgerNumerals.of(MaterialTheme.typography.titleLarge),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { year++ }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.core_next_year))
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (1..12).chunked(3).forEach { rowMonths ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowMonths.forEach { m ->
                            val ym = YearMonth.of(year, m)
                            val selected = ym == current
                            val isThisMonth = ym == thisMonth
                            val yearMonthLabel = DateLabels.yearMonth(ym, locale)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                    )
                                    .then(
                                        if (isThisMonth && !selected) Modifier.border(
                                            1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium,
                                        ) else Modifier,
                                    )
                                    .clickable(onClickLabel = yearMonthLabel) { onPick(ym) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    DateLabels.month(m, locale),
                                    style = LedgerNumerals.of(MaterialTheme.typography.titleSmall),
                                    fontWeight = if (selected || isThisMonth) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.onPrimary
                                        isThisMonth -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.action_cancel)) }
                androidx.compose.material3.FilledTonalButton(
                    onClick = { onPick(thisMonth) },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(
                        stringResource(R.string.core_back_to_this_month),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
