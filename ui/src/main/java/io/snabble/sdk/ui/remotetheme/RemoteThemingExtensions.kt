package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.graphics.Color
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.getColorByAttribute

fun Context.getPrimaryColorForProject(project: Project?): Int {
    val lightColor = project?.appTheme?.lightMode?.primaryColor?.asColor()
    val darkColor = project?.appTheme?.darkMode?.primaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorPrimary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorPrimary)
    }
}

fun Context.getOnPrimaryColorForProject(project: Project?): Int {
    val lightColor = project?.appTheme?.lightMode?.onPrimaryColor?.asColor()
    val darkColor = project?.appTheme?.darkMode?.onPrimaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorOnPrimary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorOnPrimary)
    }
}

fun Context.getSecondaryColorForProject(project: Project?): Int {
    val lightColor = project?.appTheme?.lightMode?.secondaryColor?.asColor()
    val darkColor = project?.appTheme?.darkMode?.secondaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorSecondary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorSecondary)
    }
}

fun Context.getOnSecondaryColorForProject(project: Project?): Int {
    val lightColor = project?.appTheme?.lightMode?.onSecondaryColor?.asColor()
    val darkColor = project?.appTheme?.darkMode?.onSecondaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorOnSecondary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorOnSecondary)
    }
}

fun String.asColor() = Color.parseColor(this)


fun Context.isDarkMode(): Boolean {
    val currentNightMode =
        resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}
