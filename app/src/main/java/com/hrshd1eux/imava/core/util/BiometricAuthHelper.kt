package com.hrshd1eux.imava.core.util

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    private fun findFragmentActivity(context: Context): FragmentActivity? {
        var currentContext: Context? = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun canAuthenticate(context: Context, allowDeviceCredential: Boolean = true): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val bioSuccess = biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        if (bioSuccess) return true

        if (allowDeviceCredential) {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isDeviceSecure == true) return true
        }
        return false
    }

    fun authenticate(
        activity: Context,
        title: String = "Unlock Hidden Vault",
        subtitle: String = "Use fingerprint or face unlock to access your hidden photos",
        negativeButtonText: String = "Cancel",
        allowDeviceCredential: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fragmentActivity = findFragmentActivity(activity)
        if (fragmentActivity == null) {
            onError("Biometric authentication requires FragmentActivity context.")
            return
        }

        val km = fragmentActivity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = km?.isDeviceSecure == true
        val biometricManager = BiometricManager.from(fragmentActivity)
        val canBiometric = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

        if (!canBiometric && (!allowDeviceCredential || !isDeviceSecure)) {
            onError("No biometric or lock screen security setup on device.")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(fragmentActivity)
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (allowDeviceCredential && isDeviceSecure) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    promptInfoBuilder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    promptInfoBuilder.setDeviceCredentialAllowed(true)
                }
            } else {
                promptInfoBuilder.setNegativeButtonText(negativeButtonText)
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }

            val promptInfo = promptInfoBuilder.build()

            val biometricPrompt = BiometricPrompt(
                fragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Biometric authentication failed. Try again.")
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.localizedMessage ?: "Biometric error.")
        }
    }
}
