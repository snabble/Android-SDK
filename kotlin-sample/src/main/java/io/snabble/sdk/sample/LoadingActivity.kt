package io.snabble.sdk.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.snabble.sdk.Config
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.SnabbleUI

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSdk()
    }

    fun initSdk() {
        // config {
        val config = Config(
            endpointBaseUrl = getString(R.string.endpoint),
            appId = getString(R.string.app_id),
            secret = getString(R.string.secret),
        )
        //}

        Snabble.setDebugLoggingEnabled(BuildConfig.DEBUG)

        Snabble.setup(application, config, object : Snabble.SetupCompletionListener {
            override fun onReady() {
                // an application can have multiple projects, for example for
                // multiple independent regions / countries
                val project = Snabble.projects.first()

                // register the project to our UI components
                SnabbleUI.project = project

                // check in to the first shop - you can use CheckInManager if you want
                // to use geofencing
                project.checkedInShop = project.shops.first()

                // this is done on the background and can be done at any time
                // a fully downloaded product database allows for scanning products while
                // being offline
                //
                // if the product database is still downloading or you did not call update()
                // online request will be used in the mean time
                project.productDatabase.update()

                runOnUiThread {
                    startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
                    finish()
                }
            }

            override fun onError(error: Snabble.Error?) {
                runOnUiThread {
                    AlertDialog.Builder(this@LoadingActivity)
                        .setMessage(error?.name)
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