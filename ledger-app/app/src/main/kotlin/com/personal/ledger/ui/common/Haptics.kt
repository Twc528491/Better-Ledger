package com.personal.ledger.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * 统一触感反馈入口。
 * 只有中央“记一笔”、长按账单操作和最终删除确认产生应用触感。
 */
@Composable
fun rememberHapticLight(): () -> Unit {
    return remember { {} }
}

@Composable
fun rememberHapticNavigation(): () -> Unit {
    return remember { {} }
}

@Composable
fun rememberHapticConfirm(): () -> Unit {
    return remember { {} }
}

/** 滚轮刻度：系统 CLOCK_TICK，极轻且遵循系统触感开关，仅用于日期滚轮换值。 */
@Composable
fun rememberHapticTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } }
}

/** 账单长按使用：两下短促触感；不支持组合时回落到系统强点击。 */
@Composable
fun rememberHapticLongPress(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        { vibrateLongPress(context, view) }
    }
}

/** 中央“记一笔”：跟随系统触感开关的轻点击。 */
@Composable
fun rememberHapticStrong(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        {
            performPredefinedHaptic(
                context,
                view,
                VibrationEffect.EFFECT_CLICK,
                HapticFeedbackConstants.VIRTUAL_KEY,
            )
        }
    }
}

/** 保存交易：一次强确认触感，遵循系统开关并在不支持时回落到系统按钮反馈。 */
@Composable
fun rememberHapticSave(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        {
            performPredefinedHaptic(
                context,
                view,
                VibrationEffect.EFFECT_HEAVY_CLICK,
                HapticFeedbackConstants.CONFIRM,
            )
        }
    }
}

/** 最终删除确认：使用 Android 公开的双击预设，避免伪造或反射“删除 App”私有效果。 */
@Composable
fun rememberHapticDelete(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        { vibrateDelete(context, view) }
    }
}

@Composable
fun rememberHapticReject(): () -> Unit {
    return remember { {} }
}

private fun performPredefinedHaptic(context: Context, view: View, effect: Int, fallback: Int) {
    if (!systemHapticsEnabled(context)) return
    val vibrator = vibrator(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator != null && vibrator.hasVibrator()) {
        val supported = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            vibrator.areEffectsSupported(effect).firstOrNull() != Vibrator.VIBRATION_EFFECT_SUPPORT_NO
        if (supported && runCatching { vibrator.vibrate(VibrationEffect.createPredefined(effect)) }.isSuccess) {
            return
        } else {
            view.performHapticFeedback(safeFallback(fallback))
        }
    } else {
        view.performHapticFeedback(safeFallback(fallback))
    }
}

private fun vibrateLongPress(context: Context, view: View) {
    if (!systemHapticsEnabled(context)) return
    val vibrator = vibrator(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.hasVibrator()) {
        val primitivesSupported = runCatching {
            vibrator.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
            ).all { it }
        }.getOrDefault(false)
        if (primitivesSupported) {
            val composed = runCatching {
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.55f, 30)
                    .compose()
            }.getOrNull()
            if (composed != null && runCatching { vibrator.vibrate(composed) }.isSuccess) return
        }
    }
    // EFFECT_HEAVY_CLICK is the fallback where either primitive is unavailable.
    performPredefinedHaptic(
        context,
        view,
        VibrationEffect.EFFECT_HEAVY_CLICK,
        HapticFeedbackConstants.LONG_PRESS,
    )
}

private fun safeFallback(constant: Int): Int =
    if ((constant == HapticFeedbackConstants.CONFIRM || constant == HapticFeedbackConstants.REJECT) &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    ) {
        HapticFeedbackConstants.VIRTUAL_KEY
    } else {
        constant
    }

private fun vibrateDelete(context: Context, fallbackView: View) {
    if (!systemHapticsEnabled(context)) return
    val vibrator = vibrator(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator != null && vibrator.hasVibrator()) {
        val supported = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            vibrator.areEffectsSupported(VibrationEffect.EFFECT_DOUBLE_CLICK).firstOrNull() !=
            Vibrator.VIBRATION_EFFECT_SUPPORT_NO
        if (supported && runCatching {
            // EFFECT_DOUBLE_CLICK 是稳定公开 API 29+ 预设，语义上最接近系统删除确认。
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }.isSuccess) {
            return
        } else {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            fallbackView.performHapticFeedback(constant)
        }
    } else {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        fallbackView.performHapticFeedback(constant)
    }
}

@Suppress("DEPRECATION")
private fun systemHapticsEnabled(context: Context): Boolean =
    Settings.System.getInt(
        context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        1,
    ) != 0

@Suppress("DEPRECATION")
private fun vibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
