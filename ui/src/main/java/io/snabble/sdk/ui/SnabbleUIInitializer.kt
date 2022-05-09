package io.snabble.sdk.ui

import android.content.Context
import androidx.startup.Initializer

class SnabbleUIInitializerDummy

class SnabbleUIInitializer : Initializer<SnabbleUIInitializerDummy> {
    override fun create(context: Context): SnabbleUIInitializerDummy {
        SnabbleUI.init(context)
        return SnabbleUIInitializerDummy()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}