package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
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
    defStyleAttr: Int = R.attr.buttonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        setProjectAppTheme()
    }

    private fun setProjectAppTheme() {
        val project = Snabble.checkedInProject.value
        setRippleColor(project)
        setTextColorFor(project)
        setStrokeColor(project)
    }

    private fun setStrokeColor(project: Project?) {
        strokeColor = ColorStateList.valueOf(context.primaryColorForProject(project))
    }

    private fun setRippleColor(project: Project?) {
        val highlightColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorControlHighlight,
            Color.TRANSPARENT
        )

        val rippleColorStateList = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(
                context.primaryColorForProject(project), Color.alpha(highlightColor)
            )
        )

        rippleColor = rippleColorStateList
    }

    private fun setTextColorFor(project: Project?) {
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_enabled),
        )

        val colors = intArrayOf(
            context.primaryColorForProject(project),
            context.primaryColorForProject(project),
        )

        setTextColor(ColorStateList(states, colors))
    }

}
