package io.snabble.sdk.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble

object UIPersistence {
    private var isInitialized = false
    private var application: Application
    private lateinit var sharedPreferences: SharedPreferences
    private val snabble = Snabble.getInstance()

    private val projectLiveData = MutableLiveData<Project?>()
    private var nullableProject: Project? = null

    init {
        application = snabble.application

        snabble.addOnMetadataUpdateListener(object : Snabble.OnMetadataUpdateListener {
            override fun onMetaDataUpdated() {
                if (!isInitialized) {
                    isInitialized = true
                    sharedPreferences = application.getSharedPreferences("snabble_ui_persistence", Context.MODE_PRIVATE)
                }

                update()
            }
        })
    }

    fun update() {
        nullableProject = snabble.projects.find {
            it.id == sharedPreferences.getString("id", null)
        }
    }
}