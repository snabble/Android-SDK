package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R

/**
 * A default Materialbutton which automatically sets the remote theme colors of the
 * current checked in project.
 *
 * To disable this behaviour override the given style and set `usePrimaryColors` to `false`
 */
class SnabblePrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        if (useProjectColors(attrs)) {
            setProjectAppTheme()
        }
    }

    private fun setProjectAppTheme() {

        val project = Snabble.checkedInProject.value

        setBackgroundColorFor(project)
        setTextColorFor(project)
    }

    private fun useProjectColors(attrs: AttributeSet?): Boolean {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SnabblePrimaryButton,
            0,
            0
        ).apply {
            return try {
                getBoolean(R.styleable.SnabblePrimaryButton_usePrimaryColors, true)
            } finally {
                recycle()
            }
        }
    }

    private fun setBackgroundColorFor(project: Project?) {
        // Get the existing ColorStateList for the button's background tint
        val defaultBackgroundTintList = backgroundTintList

        // Fallback to current background color if there's no existing tint list
        val currentBackgroundColor = backgroundTintList?.defaultColor ?: currentTextColor

        // Extract the default disabled color
        val defaultDisabledBackgroundColor = defaultBackgroundTintList?.getColorForState(
            intArrayOf(-android.R.attr.state_enabled),
            currentBackgroundColor
        )

        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_enabled)
        )

        val colors = intArrayOf(
            defaultDisabledBackgroundColor ?: currentBackgroundColor,
            context.getPrimaryColorForProject(project)
        )

        backgroundTintList = ColorStateList(states, colors)
    }

    private fun setTextColorFor(project: Project?) {
        val defaultTextColorStateList = textColors

        // Extract the default disabled and pressed colors
        val defaultDisabledTextColor =
            defaultTextColorStateList.getColorForState(intArrayOf(-android.R.attr.state_enabled), currentTextColor)

        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_enabled)
        )

        val colors = intArrayOf(
            defaultDisabledTextColor,
            context.getOnPrimaryColorForProject(project)
        )

        setTextColor(ColorStateList(states, colors))
    }
}
