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
 * 普通交互使用系统语义预设；中央“记一笔”保留短持续脉冲，最终删除确认使用公开的双击预设。
 */
@Composable
fun rememberHapticLight(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        {
            performPredefinedHaptic(
                context,
                view,
                VibrationEffect.EFFECT_CLICK,
                HapticFeedbackConstants.CONTEXT_CLICK,
            )
        }
    }
}

@Composable
fun rememberHapticNavigation(): () -> Unit {
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

@Composable
fun rememberHapticConfirm(): () -> Unit {
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

/** 账单长按使用：比普通点击更明确，但保持清脆而非持续嗡鸣。 */
@Composable
fun rememberHapticLongPress(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        {
            performPredefinedHaptic(
                context,
                view,
                VibrationEffect.EFFECT_HEAVY_CLICK,
                HapticFeedbackConstants.LONG_PRESS,
            )
        }
    }
}

/** 中央“记一笔”：持续但较 beta-v0.2.2 更短、更弱。 */
@Composable
fun rememberHapticStrong(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { vibrateOneShot(context, durationMs = 76L, amplitude = 205) } }
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
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        { performSystemHaptic(context, view, HapticFeedbackConstants.REJECT, HapticFeedbackConstants.LONG_PRESS) }
    }
}

private fun performPredefinedHaptic(context: Context, view: View, effect: Int, fallback: Int) {
    if (!systemHapticsEnabled(context)) return
    val vibrator = vibrator(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator != null && vibrator.hasVibrator()) {
        runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        }.onFailure {
            view.performHapticFeedback(safeFallback(fallback))
        }
    } else {
        view.performHapticFeedback(safeFallback(fallback))
    }
}

private fun performSystemHaptic(context: Context, view: View, primary: Int, fallback: Int) {
    if (!systemHapticsEnabled(context)) return
    // CONFIRM/REJECT were added in API 30; use a crisp legacy preset below it.
    val api30Only = primary == HapticFeedbackConstants.CONFIRM || primary == HapticFeedbackConstants.REJECT
    val constant = if (!api30Only || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) primary else fallback
    view.performHapticFeedback(safeFallback(constant))
}

private fun safeFallback(constant: Int): Int =
    if ((constant == HapticFeedbackConstants.CONFIRM || constant == HapticFeedbackConstants.REJECT) &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    ) {
        HapticFeedbackConstants.VIRTUAL_KEY
    } else {
        constant
    }

private fun vibrateOneShot(context: Context, durationMs: Long, amplitude: Int) {
    if (!systemHapticsEnabled(context)) return
    vibrator(context)?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
}

private fun vibrateDelete(context: Context, fallbackView: View) {
    if (!systemHapticsEnabled(context)) return
    val vibrator = vibrator(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator != null && vibrator.hasVibrator()) {
        runCatching {
            // EFFECT_DOUBLE_CLICK 是稳定公开 API 29+ 预设，语义上最接近系统删除确认。
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }.onFailure {
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
