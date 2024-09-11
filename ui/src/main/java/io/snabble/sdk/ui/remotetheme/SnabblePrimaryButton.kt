package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.ThemeWrapper
import kotlinx.coroutines.flow.MutableStateFlow

class SnabblePrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var isButtonEnabled = MutableStateFlow(isEnabled)
    private var textRes: MutableStateFlow<String> = MutableStateFlow(getDefaultString(attrs))
    private var setHeight: MutableStateFlow<Dp?> = MutableStateFlow(null)

    private var clickListener: OnClickListener? = null

    init {
        init()
    }

    private fun init() {
        inflate(context, R.layout.snabble_primary_button, this).apply {
            val container = findViewById<ComposeView>(R.id.button_container)
            container.apply {
                setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ThemeWrapper {
                        val isEnable = isButtonEnabled.collectAsStateWithLifecycle().value
                        val text = textRes.collectAsStateWithLifecycle().value
                        val height = setHeight.collectAsStateWithLifecycle().value

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height ?: ButtonDefaults.MinHeight),
                            onClick = { clickListener?.onClick(this) },
                            enabled = isEnable
                        ) {
                            Text(text = text)
                        }
                    }
                }
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = l
    }

    private fun getDefaultString(attrs: AttributeSet?): String {
        context.theme.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text), 0, 0)
            .apply {
                try {
                    return getString(0) ?: ""
                } finally {
                    recycle()
                }
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            setHeight.value = pxToDp(context, layoutParams.height).dp
        }
    }

    fun setText(@StringRes value: Int) {
        textRes.value = context.getString(value)
    }

    fun setText(value: String) {
        textRes.value = value
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isButtonEnabled.value = enabled
    }

    private fun pxToDp(context: Context, px: Int): Float {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return px / (metrics.densityDpi / BASELINE_DENSITY)
    }

    companion object {

        private const val BASELINE_DENSITY = 160f
    }
}
