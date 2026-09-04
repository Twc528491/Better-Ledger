package com.personal.ledger.ui.common

import android.os.Build
import android.content.Context
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
 * 普通交互使用系统语义预设，只有中央“记一笔”和最终删除确认使用持续脉冲。
 */
@Composable
fun rememberHapticLight(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { performSystemHaptic(view, HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackConstants.VIRTUAL_KEY) }
    }
}

@Composable
fun rememberHapticNavigation(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { performSystemHaptic(view, HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.VIRTUAL_KEY) }
    }
}

@Composable
fun rememberHapticConfirm(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { performSystemHaptic(view, HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.VIRTUAL_KEY) }
    }
}

/** 账单长按使用：比普通点击更明确，但保持清脆而非持续嗡鸣。 */
@Composable
fun rememberHapticLongPress(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { performSystemHaptic(view, HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.LONG_PRESS) }
    }
}

/** 中央“记一笔”和最终危险确认共用：持续但较 beta-v0.2.2 更短、更弱。 */
@Composable
fun rememberHapticStrong(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { vibrateOneShot(context, durationMs = 76L, amplitude = 205) } }
}

@Composable
fun rememberHapticReject(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { performSystemHaptic(view, HapticFeedbackConstants.REJECT, HapticFeedbackConstants.LONG_PRESS) }
    }
}

private fun performSystemHaptic(view: View, primary: Int, fallback: Int) {
    // CONFIRM/REJECT were added in API 30; use a crisp legacy preset below it.
    val api30Only = primary == HapticFeedbackConstants.CONFIRM || primary == HapticFeedbackConstants.REJECT
    val constant = if (!api30Only || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) primary else fallback
    view.performHapticFeedback(constant)
}

private fun vibrateOneShot(context: Context, durationMs: Long, amplitude: Int) {
    if (!systemHapticsEnabled(context)) return
    vibrator(context)?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
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
