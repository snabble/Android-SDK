package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R

class SnabblePrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialButtonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        init()
    }

    private fun init() {
        val project = Snabble.checkedInProject.value
        setBackgroundColor(context.getPrimaryColorForProject(project))
        setTextColor(context.getOnPrimaryColorForProject(project))
    }
}
