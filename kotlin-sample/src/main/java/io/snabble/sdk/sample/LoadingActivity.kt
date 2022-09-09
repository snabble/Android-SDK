package io.snabble.sdk.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSdk()
    }

    private fun initSdk() {
        Snabble.setDebugLoggingEnabled(BuildConfig.DEBUG)
        Snabble.initializationState.observe(this) {
            when (it) {
                InitializationState.UNINITIALIZED,
                InitializationState.INITIALIZING,
                null -> {
                }
                InitializationState.INITIALIZED -> {
                    startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
                    finish()
                }
                InitializationState.ERROR -> {
                    runOnUiThread {
                        AlertDialog.Builder(this@LoadingActivity)
                            .setMessage(Snabble.error?.name)
                            .setPositiveButton("Retry") { _, _ ->
                                Snabble.setup()
                            }
                            .setNegativeButton("Exit") { _, _ ->
                                finish()
                            }
                            .create()
                            .show()
                    }
                }
            }
        }
    }
}
