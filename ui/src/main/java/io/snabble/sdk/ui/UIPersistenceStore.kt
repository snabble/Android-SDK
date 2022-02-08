package io.snabble.sdk.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import java.util.concurrent.atomic.AtomicBoolean

internal object UIPersistenceStore {

    private val snabble = Snabble.getInstance()
    private var application = snabble.application
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("snabble_ui_persistence", Context.MODE_PRIVATE)

    val project = MutableLiveData<Project?>()

    init {
        snabble.addOnMetadataUpdateListener(object : Snabble.OnMetadataUpdateListener {
            override fun onMetaDataUpdated() {
                update()
            }
        })
    }

    internal fun updateProject(p: Project?) {
        project.postValue(p)
        sharedPreferences.edit().putString("id", null).apply()
    }

    private fun update() {
        project.postValue(snabble.projects.find {
            it.id == sharedPreferences.getString("id", null)
        })
    }
}