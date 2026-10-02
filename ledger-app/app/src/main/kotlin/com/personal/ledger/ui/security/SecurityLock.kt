package com.personal.ledger.ui.security

import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.personal.ledger.LedgerApplication
import com.personal.ledger.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 本地安全锁（MVP.md 第 10 节 / ARCHITECTURE.md 第 9 节）：
 * 仅使用系统 BiometricPrompt（生物识别 / 设备凭据），不自行实现 PIN。
 * 默认关闭；开启后冷启动和较长时间（>30 秒）离开 App 后要求验证。
 */
class SecurityLock(private val activity: FragmentActivity) {

    /** 应用切后台的时间点（首次启动为 0，表示冷启动）。 */
    private var backgroundedAt = 0L
    private var prompting = false

    fun onStopped() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** 在 onStart 时调用：决定是否需要验证。 */
    fun checkAndPromptIfNeeded() {
        val settings = (activity.application as LedgerApplication).container.settings
        activity.lifecycleScope.launch {
            if (prompting) return@launch
            if (!settings.securityLockEnabled.first()) return@launch

            val gap = if (backgroundedAt == 0L) Long.MAX_VALUE else SystemClock.elapsedRealtime() - backgroundedAt
            if (backgroundedAt == 0L || gap > LOCK_TIMEOUT_MS) {
                prompt()
            }
        }
    }

    private fun prompt() {
        if (BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            // 安全锁已启用时不能因验证方式临时不可用而绕过锁。
            activity.finishAffinity()
            return
        }
        prompting = true
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    prompting = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    prompting = false
                    // 任何取消或错误都关闭任务，避免返回键绕过系统验证。
                    activity.finishAffinity()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.core_lock_title))
            .setSubtitle(activity.getString(R.string.core_lock_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        /** 离开应用超过该时长后需要重新验证。 */
        const val LOCK_TIMEOUT_MS = 30_000L
    }
}

/**
 * 开启安全锁前的一次性身份验证：验证成功后才写入启用标记，防止误触锁死。
 */
fun promptForEnrollment(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    if (BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) != BiometricManager.BIOMETRIC_SUCCESS
    ) {
        onDismiss()
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onDismiss()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.core_lock_enable_title))
        .setSubtitle(activity.getString(R.string.core_lock_enable_subtitle))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    prompt.authenticate(info)
}
