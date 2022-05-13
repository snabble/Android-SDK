package io.snabble.sdk.customization

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

    fun initSdk() {
        Snabble.setDebugLoggingEnabled(BuildConfig.DEBUG)
        Snabble.initializationState.observe(this) {
            when (it) {
                InitializationState.INITIALIZING -> {}
                InitializationState.INITIALIZED -> {
                    // an application can have multiple projects, for example for
                    // multiple independent regions / countries
                    val project = Snabble.projects.first()

                    // check in to the first shop - you can use CheckInManager if you want
                    // to use geofencing
                    Snabble.checkedInShop = project.shops.first()

                    // this is done on the background and can be done at any time
                    // a fully downloaded product database allows for scanning products while
                    // being offline
                    //
                    // if the product database is still downloading or you did not call update()
                    // online request will be used in the mean time
                    project.productDatabase.update()

                    startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
                    finish()
                }
                InitializationState.ERROR -> {
                    runOnUiThread {
                        AlertDialog.Builder(this@LoadingActivity)
                            .setMessage("SDK initialization error")
                            .setPositiveButton("Retry") { _, _ ->
                                Snabble.setup(application, null, null)
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