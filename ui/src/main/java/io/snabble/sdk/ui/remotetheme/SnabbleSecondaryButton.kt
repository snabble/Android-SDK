package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R

/**
 * A default Materialbutton which automatically sets the primary color from the remote theme
 * of the current checked in project as text color.
 *
 * To use it as secondary button it is required to apply the Widget.Material3.Button.TextButton style.
 */
class SnabbleSecondaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialButtonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        setProjectAppTheme()
    }

    private fun setProjectAppTheme() {
        val project = Snabble.checkedInProject.value
        setTextColorFor(project)
    }

    private fun setTextColorFor(project: Project?) {
        val defaultTextColorStateList = textColors

        // Extract the default disabled and pressed colors
        val defaultDisabledTextColor =
            defaultTextColorStateList.getColorForState(intArrayOf(-android.R.attr.state_enabled), currentTextColor)
        val defaultPressedTextColor =
            defaultTextColorStateList.getColorForState(intArrayOf(android.R.attr.state_pressed), currentTextColor)

        val states2 = arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf(android.R.attr.state_enabled)
        )

        val colors2 = intArrayOf(
            defaultDisabledTextColor,
            defaultPressedTextColor,
            context.getOnPrimaryColorForProject(project)
        )

        val colorStateList2 = ColorStateList(states2, colors2)
        setTextColor(colorStateList2)
    }
}
