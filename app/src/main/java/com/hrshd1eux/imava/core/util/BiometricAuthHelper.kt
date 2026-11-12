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
        return try {
            val biometricManager = BiometricManager.from(context)
            val canBio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && allowDeviceCredential) {
                biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            }
            if (canBio) return true

            if (allowDeviceCredential) {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (km?.isDeviceSecure == true) return true
            }
            false
        } catch (_: Exception) {
            try {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                km?.isDeviceSecure == true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun authenticate(
        activity: Context,
        title: String = "",
        subtitle: String = "",
        negativeButtonText: String = "",
        allowDeviceCredential: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val resolvedTitle = title.ifBlank {
            LocaleHelper.getString(activity, com.hrshd1eux.imava.R.string.biometric_default_title, "Unlock Hidden Vault")
        }
        val resolvedSubtitle = subtitle.ifBlank {
            LocaleHelper.getString(
                activity,
                com.hrshd1eux.imava.R.string.biometric_default_subtitle,
                "Use fingerprint or face unlock to access your hidden photos"
            )
        }
        val resolvedNegative = negativeButtonText.ifBlank {
            LocaleHelper.getString(activity, com.hrshd1eux.imava.R.string.action_cancel, "Cancel")
        }

        val fragmentActivity = findFragmentActivity(activity)
        if (fragmentActivity == null) {
            onError(
                LocaleHelper.getString(
                    activity,
                    com.hrshd1eux.imava.R.string.biometric_err_activity,
                    "Biometric authentication requires FragmentActivity context."
                )
            )
            return
        }

        val km = fragmentActivity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = km?.isDeviceSecure == true
        val biometricManager = BiometricManager.from(fragmentActivity)
        val canBiometric = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

        if (!canBiometric && (!allowDeviceCredential || !isDeviceSecure)) {
            onError(
                LocaleHelper.getString(
                    activity,
                    com.hrshd1eux.imava.R.string.biometric_err_not_setup,
                    "No biometric or lock screen security setup on device."
                )
            )
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(fragmentActivity)
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(resolvedTitle)
                .setSubtitle(resolvedSubtitle)

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
                promptInfoBuilder.setNegativeButtonText(resolvedNegative)
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
                        onError(
                            LocaleHelper.getString(
                                activity,
                                com.hrshd1eux.imava.R.string.biometric_err_failed,
                                "Biometric authentication failed. Try again."
                            )
                        )
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
            onError(
                e.localizedMessage ?: LocaleHelper.getString(
                    activity,
                    com.hrshd1eux.imava.R.string.biometric_err_generic,
                    "Biometric error."
                )
            )
        }
    }
}
