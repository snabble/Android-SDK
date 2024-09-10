package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.snabble.sdk.Snabble

/**
 * A default AppCompatTextView which automatically sets the primary color from the remote theme
 * of the current checked in project as text color.
 */
class SnabblePrimaryTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {


    init {
        setProjectAppTheme()
    }

    private fun setProjectAppTheme() {
        val project = Snabble.checkedInProject.value
        setTextColor(context.getPrimaryColorForProject(project))
    }
}
