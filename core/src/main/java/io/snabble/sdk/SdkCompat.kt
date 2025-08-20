package io.snabble.sdk

import android.os.Build
import android.view.View
import java.util.Locale

fun localeOf(language: String): Locale {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> Locale.of(language)
        else -> {
            @Suppress("DEPRECATION")
            Locale(language)
        }
    }
}

fun localeOf(language: String, country: String): Locale {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> Locale.of(language, country)
        else -> {
            @Suppress("DEPRECATION")
            Locale(language, country)
        }
    }
}

fun Thread.compatId(): Long {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> threadId()
        else -> {
            (@Suppress("DEPRECATION")
            id)
        }
    }
}
fun View.announceAccessibiltyEvent(event: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        stateDescription = event
    } else {
        @Suppress("DEPRECATION")
        announceForAccessibility(event)
    }
}
