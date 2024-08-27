package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R

class SnabblePrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.buttonStyle,
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        init()
    }

    private fun init() {
        Snabble.checkedInProject.observeForever {
            it?.appTheme?.let { appTheme ->
                setBackgroundColor(appTheme.primaryColor.asColor())
                setTextColor(appTheme.secondaryColor.asColor())
            }
        }
    }
}

private fun String.asColor() = Color.parseColor(this)
