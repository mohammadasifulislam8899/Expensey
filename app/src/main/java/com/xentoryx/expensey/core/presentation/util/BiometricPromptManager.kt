package com.xentoryx.expensey.core.presentation.util

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricPromptManager(
    private val activity: FragmentActivity
) {
    fun showBiometricPrompt(
        title: String,
        description: String,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (Int, CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val manager = BiometricManager.from(activity)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        
        val canAuthenticate = manager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onError(canAuthenticate, "Biometric authentication is not available or not set up.")
            return
        }

        val executor: Executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.mainExecutor
        } else {
            androidx.core.content.ContextCompat.getMainExecutor(activity)
        }

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(description)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
