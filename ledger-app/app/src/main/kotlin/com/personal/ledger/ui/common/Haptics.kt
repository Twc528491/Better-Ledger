package com.personal.ledger.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * 统一触感反馈入口，强度顺序：选择 < 导航 < 确认 < 强调。
 * 所有持续脉冲都尊重系统“触感反馈”开关。
 */
@Composable
fun rememberHapticLight(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
    }
}

@Composable
fun rememberHapticNavigation(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { vibrateOneShot(context, durationMs = 32L, amplitude = 145) } }
}

@Composable
fun rememberHapticConfirm(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { vibrateOneShot(context, durationMs = 52L, amplitude = 205) } }
}

/** 中央“记一笔”和最终危险确认使用：全应用最强、持续时间最长。 */
@Composable
fun rememberHapticStrong(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { vibrateOneShot(context, durationMs = 120L, amplitude = 255) } }
}

@Composable
fun rememberHapticReject(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            if (systemHapticsEnabled(context)) {
                vibrator(context)?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0L, 36L, 42L, 62L),
                        intArrayOf(0, 190, 0, 245),
                        -1,
                    ),
                )
            }
        }
    }
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
