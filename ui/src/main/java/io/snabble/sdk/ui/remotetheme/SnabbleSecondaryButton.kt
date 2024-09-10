package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
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
        setTextColor(context.getPrimaryColorForProject(project))
    }
}
