package io.snabble.sdk.ui

import io.snabble.sdk.Snabble.instance
import android.os.Build
import android.app.KeyguardManager
import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.utils.Dispatch
import java.util.concurrent.Executors

object Keyguard {
    private var currentCallback: Callback? = null

    @JvmStatic
    fun unlock(activity: FragmentActivity, callback: Callback?) {
        Dispatch.mainThread {
            currentCallback = callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BiometricPrompt.Builder(activity).apply {
                    setTitle(activity.getString(R.string.Snabble_Keyguard_title))
                    setDescription(activity.getString(R.string.Snabble_Keyguard_message))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    } else {
                        @Suppress("deprecation") // on that runtime it was not deprecated
                        setDeviceCredentialAllowed(true)
                    }
                }.build()
                    .authenticate(
                        CancellationSignal(), Executors.newSingleThreadExecutor(),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                error(callback)
                            }

                            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                                error(callback)
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                success(callback)
                            }

                            override fun onAuthenticationFailed() {}
                        })
            } else {
                unlockWithKeyguard(activity)
            }
        }
    }

    private fun unlockWithKeyguard(activity: FragmentActivity) {
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardSecure) {
            val fm = activity.supportFragmentManager
            val fragment = ActivityResultFragment()
            fm.beginTransaction()
                .add(fragment, "ActivityResultFragment")
                .commit()
            fm.executePendingTransactions()
            @Suppress("deprecation") // this is a legacy code path, on that runtime it was not deprecated
            val authIntent = km.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.Snabble_Keyguard_title),
                activity.getString(R.string.Snabble_Keyguard_message)
            )
            fragment.activityResult.launch(authIntent)
        } else {
            success(null)
        }
    }

    private fun success(callback: Callback?) {
        Dispatch.mainThread {
            if (callback != null && currentCallback !== callback) {
                return@mainThread
            }

            // migrate key store stored values to rsa stored values
            instance.paymentCredentialsStore.maybeMigrateKeyStoreCredentials()
            currentCallback?.success()
            currentCallback = null
        }
    }

    private fun error(callback: Callback?) {
        Dispatch.mainThread {
            if (callback != null && currentCallback !== callback) {
                return@mainThread
            }
            currentCallback?.error()
            currentCallback = null
        }
    }

    interface Callback {
        fun success()
        fun error()
    }

    class ActivityResultFragment : Fragment() {
        val activityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                success(null)
            } else {
                error(null)
            }
        }
    }
}