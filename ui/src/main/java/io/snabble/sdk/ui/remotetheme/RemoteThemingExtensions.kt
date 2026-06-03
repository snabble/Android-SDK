@file:JvmName("RemoteThemingHelper")

package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.Project
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.getColorByAttribute
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR

fun Context.getPrimaryColorForProject(project: Project?): Int {
    val lightColor = project?.appTheme?.lightModeColors?.primaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.primaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(AppCompatR.attr.colorPrimary)
        else -> lightColor ?: getColorByAttribute(AppCompatR.attr.colorPrimary)
    }
}

@JvmOverloads
fun Context.primaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.primaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.primaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(AppCompatR.attr.colorPrimary)
        else -> lightColor ?: getColorByAttribute(AppCompatR.attr.colorPrimary)
    }.also { action?.invoke(it) }
}

@JvmOverloads
fun Context.onPrimaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.onPrimaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.onPrimaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(MaterialR.attr.colorOnPrimary)
        else -> lightColor ?: getColorByAttribute(MaterialR.attr.colorOnPrimary)
    }.also { action?.invoke(it) }
}

fun Context.secondaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.secondaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.secondaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(MaterialR.attr.colorSecondary)
        else -> lightColor ?: getColorByAttribute(MaterialR.attr.colorSecondary)
    }.also { action?.invoke(it) }
}

fun Context.onSecondaryColorForProject(project: Project?, action: ((Int) -> Unit)? = null): Int {
    val lightColor = project?.appTheme?.lightModeColors?.onSecondaryColor?.asColor()
    val darkColor = project?.appTheme?.darkModeColors?.onSecondaryColor?.asColor()
    return when {
        isDarkMode() -> darkColor ?: lightColor ?: getColorByAttribute(MaterialR.attr.colorOnSecondary)
        else -> lightColor ?: getColorByAttribute(MaterialR.attr.colorOnSecondary)
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

private data class ToolbarColors(
    @SerializedName("colorAppBar_light") val lightToolbarColor: String?,
    @SerializedName("colorAppBar_dark") val darkToolbarColor: String?,
    @SerializedName("colorOnAppBar_light") val lightOnToolbarColor: String?,
    @SerializedName("colorOnAppBar_dark") val darkOnToolbarColor: String?,
)

fun Context.toolBarColorForProject(project: Project?): Int? =
    GsonHolder.get().fromJson(project?.customizationConfig, ToolbarColors::class.java)?.let {
        val lightColor = it.lightToolbarColor?.asColor()
        val darkColor = it.darkToolbarColor?.asColor()
        when {
            isDarkMode() -> darkColor ?: lightColor
            else -> lightColor
        }
    }

fun Context.onToolBarColorForProject(project: Project?): Int? =
    GsonHolder.get().fromJson(project?.customizationConfig, ToolbarColors::class.java)?.let {
        val lightColor = it.lightOnToolbarColor?.asColor()
        val darkColor = it.darkOnToolbarColor?.asColor()
        when {
            isDarkMode() -> darkColor ?: lightColor
            else -> lightColor
        }
    }
