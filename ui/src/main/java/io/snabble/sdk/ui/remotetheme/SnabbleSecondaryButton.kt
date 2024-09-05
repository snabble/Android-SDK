package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R

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
