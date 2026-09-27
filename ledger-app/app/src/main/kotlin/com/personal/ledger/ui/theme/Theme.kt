package com.personal.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ledger.data.settings.ThemeMode

/*
 * 视觉基调（v0.3.2）：
 * - 品牌色为沉稳的深青绿，只用于主操作、选中态与少量强调；
 * - 表面为中性暖灰，不带紫色或绿色偏色，让支出红 / 收入绿 / 转账蓝更易辨认；
 * - 显式定义全部 surfaceContainer 层级，避免 Material 基线色的淡紫偏色；
 * - 不使用壁纸动态取色，保证各设备观感一致。
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF17695A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBE2),
    onPrimaryContainer = Color(0xFF00201A),
    inversePrimary = Color(0xFF85D5C0),
    secondary = Color(0xFF4A6360),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE8E5),
    onSecondaryContainer = Color(0xFF14201E),
    tertiary = Color(0xFF3F5F90),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8E2FF),
    onTertiaryContainer = Color(0xFF001A41),
    background = Color(0xFFF6F6F3),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFF6F6F3),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFE6E8E5),
    onSurfaceVariant = Color(0xFF5B615E),
    surfaceTint = Color(0xFF17695A),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDCDDDA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F1EE),
    surfaceContainerHigh = Color(0xFFEBECE9),
    surfaceContainerHighest = Color(0xFFE4E5E2),
    outline = Color(0xFF8A908D),
    outlineVariant = Color(0xFFE0E2DF),
    inverseSurface = Color(0xFF2E312F),
    inverseOnSurface = Color(0xFFF0F1EE),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE3E0),
    onErrorContainer = Color(0xFF410E0B),
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD3BD),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF1D5147),
    onPrimaryContainer = Color(0xFFBDEFE0),
    inversePrimary = Color(0xFF17695A),
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF2E3B39),
    onSecondaryContainer = Color(0xFFCDE8E3),
    tertiary = Color(0xFFAEC6FF),
    onTertiary = Color(0xFF0B2E60),
    tertiaryContainer = Color(0xFF274777),
    onTertiaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFF111312),
    onBackground = Color(0xFFE2E3E0),
    surface = Color(0xFF111312),
    onSurface = Color(0xFFE2E3E0),
    surfaceVariant = Color(0xFF2B302E),
    onSurfaceVariant = Color(0xFFA9B0AC),
    surfaceTint = Color(0xFF7FD3BD),
    surfaceBright = Color(0xFF363A38),
    surfaceDim = Color(0xFF111312),
    surfaceContainerLowest = Color(0xFF0C0E0D),
    surfaceContainerLow = Color(0xFF1A1D1C),
    surfaceContainer = Color(0xFF1E2120),
    surfaceContainerHigh = Color(0xFF262A28),
    surfaceContainerHighest = Color(0xFF303432),
    outline = Color(0xFF7D8581),
    outlineVariant = Color(0xFF2F3432),
    inverseSurface = Color(0xFFE2E3E0),
    inverseOnSurface = Color(0xFF2E312F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF5C1D19),
    onErrorContainer = Color(0xFFF9DEDC),
    scrim = Color.Black,
)

/** 系统字体；标题略收紧，正文保持默认节奏。金额另用 [LedgerNumerals] 的等宽数字。 */
private val LedgerTypography: Typography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

private val LedgerMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** 金额 / 数字统一使用等宽数字，列表中的金额右对齐时不会跳动。 */
object LedgerNumerals {
    const val FEATURES = "tnum"

    fun of(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = FEATURES)
}

/** 供语义颜色组件读取应用内实际主题，避免“手动深色”与系统主题不一致。 */
val LocalLedgerDarkTheme = staticCompositionLocalOf { false }

@Composable
fun LedgerTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalLedgerDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LedgerTypography,
            shapes = LedgerMaterialShapes,
            content = content,
        )
    }
}
