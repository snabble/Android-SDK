@file:JvmName("RemoteThemingHelper")

package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.getColorByAttribute

fun Context.getPrimaryColorForProject(project: Project?): Int {
    val lightColor = project?.appTheme?.lightModeColors?.primaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.primaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorPrimary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorPrimary)
    }
}

@JvmOverloads
fun Context.primaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.primaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.primaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorPrimary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorPrimary)
    }.also { action?.invoke(it) }
}

@JvmOverloads
fun Context.onPrimaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.onPrimaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.onPrimaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorOnPrimary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorOnPrimary)
    }.also { action?.invoke(it) }
}

fun Context.secondaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.secondaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.secondaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorSecondary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorSecondary)
    }.also { action?.invoke(it) }
}

fun Context.onSecondaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.onSecondaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.onSecondaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(R.attr.colorOnSecondary)
        else -> lightColor ?: getColorByAttribute(R.attr.colorOnSecondary)
    }.also { action?.invoke(it) }
}

fun String.asColor() = Color.parseColor(this)

fun Context.isDarkMode(): Boolean {
    val currentNightMode =
        resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}

fun AlertDialog.changeButtonColorFor(project: Project?): AlertDialog {
    val primaryColor = context.primaryColorForProject(project)
    setOnShowListener {
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(primaryColor)
        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(primaryColor)
    }
    return this
}
