package io.snabble.sdk.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.SnabbleUI

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSdk()
    }

    fun initSdk() {
        // config {
        val config = Snabble.Config()
        config.endpointBaseUrl = getString(R.string.endpoint)
        config.secret = getString(R.string.secret)
        config.appId = getString(R.string.app_id)
        //}

        Snabble.setDebugLoggingEnabled(BuildConfig.DEBUG)

        val snabble = Snabble.getInstance()
        snabble.setup(application, config, object : Snabble.SetupCompletionListener {
            override fun onReady() {
                //snabble.userPreferences.setRequireKeyguardAuthenticationForPayment(true)

                // an application can have multiple projects
                val project = snabble.projects.first()
                SnabbleUI.useProject(project)
                project.checkedInShop = project.shops.first()

                // this is done on the background and can be done at any time
                project.productDatabase.update()

                runOnUiThread {
                    startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
                    finish()
                }
            }

            override fun onError(error: Snabble.Error) {
                runOnUiThread {
                    AlertDialog.Builder(this@LoadingActivity)
                        .setMessage(error.name)
                        .setPositiveButton("Retry") { _, _ ->
                            initSdk()
                        }
                        .setNegativeButton("Exit") { _, _ ->
                            finish()
                        }
                        .create()
                        .show()
                }
            }
        })
    }
}