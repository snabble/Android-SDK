package io.snabble.sdk.ui

import io.snabble.sdk.ui.Keyguard
import android.annotation.SuppressLint
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationError
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.Snabble

object Keyguard {
    @JvmStatic
    fun unlock(activity: FragmentActivity, callback: Callback) {
        Dispatch.mainThread {
            val prompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    @AuthenticationError errorCode: Int, errString: CharSequence
                ) {
                    callback.error()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // start migration when we are having authentication
                    Snabble.getInstance().paymentCredentialsStore.migrateKeyStoreCredentials()

                    callback.success()
                }

                override fun onAuthenticationFailed() {}
            })

            prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .setTitle(activity.getString(R.string.Snabble_Keyguard_title))
                .setDescription(activity.getString(R.string.Snabble_Keyguard_message))
                .build())
        }
    }

    interface Callback {
        fun success()
        fun error()
    }
}