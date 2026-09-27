package com.personal.ledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.ledger.ui.theme.LedgerNumerals
import com.personal.ledger.ui.theme.LedgerSpacing
import com.personal.ledger.util.Money

/** 卡片 / 分组统一圆角，与 MaterialTheme.shapes.large 保持一致。 */
private val CardRadius = 18.dp

/** 全应用统一的内容容器；扁平、低层级，不额外制造强阴影。 */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    containerColor: Color = cardColor(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(LedgerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.sm),
            content = content,
        )
    }
}

@Composable
fun LedgerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    prominent: Boolean = false,
    style: TextStyle? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = LedgerNumerals.of(
            style ?: if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
        ),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 符号在前的金额文本，如 +¥3,000.00 / -¥1,980.00（UI.md：负债明确使用负号）。
 * 零值不带符号。
 */
fun signedMoney(minor: Long, currency: String = "CNY", explicitPlus: Boolean = true): String {
    val body = Money.formatSignedMinor(minor).removePrefix("-")
    val sign = when {
        minor < 0 -> "-"
        minor > 0 && explicitPlus -> "+"
        else -> ""
    }
    return sign + Money.symbolFor(currency) + body
}

/**
 * 主卡片（本月概览 / 净资产）：品牌深青绿底 + 内容色文字。
 * 卡片内一律使用 [LocalContentColor] 的透明度变体，不叠加红绿语义色。
 */
@Composable
fun LedgerHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = highlightCardColor(),
        contentColor = highlightCardContentColor(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LedgerSpacing.lg, vertical = 18.dp),
            content = content,
        )
    }
}

/** 主卡片内的次要文字色（标签 / 说明）。 */
@Composable
fun heroSecondaryColor(): Color = LocalContentColor.current.copy(alpha = 0.72f)

/** 主卡片内的「标签 + 金额」块；[large] 为页面主数字。 */
@Composable
fun HeroFigure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = heroSecondaryColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(if (large) 4.dp else 2.dp))
        Text(
            value,
            style = LedgerNumerals.of(
                if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
            ),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 扁平圆角进度条（替代带停止点的 M3 进度条），可读屏为进度信息。 */
@Composable
fun LedgerProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 6.dp,
) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** 设置类分组上方的小标题。 */
@Composable
fun LedgerGroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = LedgerSpacing.xxs, top = LedgerSpacing.xxs, bottom = LedgerSpacing.xs),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 非懒加载的分组：白色卡片内直接堆叠 [LedgerListRow]，不加分隔线。 */
@Composable
fun LedgerGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) LedgerGroupLabel(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = cardColor(),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = LedgerSpacing.xxs), content = content)
        }
    }
}

/** 懒加载列表中连续条目拼成一张卡片时，第 [index] / [count] 项的外形。 */
fun ledgerGroupShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(CardRadius)
    index == 0 -> RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius)
    index >= count - 1 -> RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius)
    else -> RectangleShape
}

/**
 * LazyColumn 中的分组条目：相邻条目共同组成一张白色卡片。
 * 使用时 LazyColumn 不要设置 verticalArrangement 间距，组间距离由调用方显式留白。
 */
@Composable
fun LedgerGroupItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ledgerGroupShape(index, count),
        color = cardColor(),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                top = if (index == 0) LedgerSpacing.xxs else 0.dp,
                bottom = if (index >= count - 1) LedgerSpacing.xxs else 0.dp,
            ),
            content = content,
        )
    }
}

/** 圆角方形的浅色图标底，用于设置 / 功能入口。 */
@Composable
fun LedgerIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.56f))
    }
}

/**
 * 统一列表行：最小 56dp；可选前置图标、副标题、右侧数值、自定义尾部与箭头。
 * 需要长按时由调用方在 [modifier] 中传入 combinedClickable，并将 [onClick] 留空。
 */
@Composable
fun LedgerListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    titleColor: Color = Color.Unspecified,
    titleWeight: FontWeight? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = LedgerSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(LedgerSpacing.sm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                fontWeight = titleWeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(LedgerSpacing.xs))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(LedgerSpacing.xs))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(LedgerSpacing.xxs))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
