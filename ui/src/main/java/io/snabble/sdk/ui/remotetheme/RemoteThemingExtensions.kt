package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.graphics.Color
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.getColorByAttribute

fun Context.getPrimaryColorForProject(project: Project?) =
    project?.appTheme?.primaryColor?.asColor() ?: getColorByAttribute(R.attr.colorPrimary)

fun Context.getSecondaryColorForProject(project: Project?) =
    project?.appTheme?.secondaryColor?.asColor() ?: getColorByAttribute(R.attr.colorSecondary)

fun String.asColor() = Color.parseColor(this)
