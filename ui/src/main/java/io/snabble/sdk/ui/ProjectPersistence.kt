package io.snabble.sdk.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Dispatch
import java.util.concurrent.atomic.AtomicBoolean

internal object ProjectPersistence {
    private val snabble = Snabble.getInstance()
    private var application = snabble.application
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("snabble_ui_persistence", Context.MODE_PRIVATE)

    val projectAsLiveData = MutableLiveData<Project?>()
    var project: Project? = null

    init {
        Dispatch.mainThread {
            snabble.initializationState.observeForever {
                update()
            }

            snabble.addOnMetadataUpdateListener(object : Snabble.OnMetadataUpdateListener {
                override fun onMetaDataUpdated() {
                    update()
                }
            })
        }
    }

    internal fun post(p: Project?) {
        project = p
        projectAsLiveData.postValue(p)
        sharedPreferences.edit().putString("id", p?.id).apply()
    }

    private fun update() {
        val p = snabble.projects.find {
            it.id == sharedPreferences.getString("id", null)
        }
        project = p
        projectAsLiveData.postValue(p)
    }
}