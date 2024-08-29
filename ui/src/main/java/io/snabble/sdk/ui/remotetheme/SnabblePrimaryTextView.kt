package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.snabble.sdk.Snabble

class SnabblePrimaryTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {


    init {
        init()
    }

    private fun init() {
        val project = Snabble.checkedInProject.value
        setTextColor(context.getPrimaryColorForProject(project))
    }
}
