package com.personal.ledger.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 统一触感反馈入口。
 * - [rememberHapticConfirm]：确认级（保存、删除、切换账本等）；
 * - [rememberHapticLight]：轻量级（选择分类、切换筛选、翻月等）。
 */
@Composable
fun rememberHapticConfirm(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember { { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }
}

@Composable
fun rememberHapticLight(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember { { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}
