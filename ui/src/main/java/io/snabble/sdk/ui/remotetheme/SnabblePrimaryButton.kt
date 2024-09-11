package io.snabble.sdk.ui.remotetheme

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.ThemeWrapper
import kotlinx.coroutines.flow.MutableStateFlow


class SnabblePrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var isButtonEnabled: MutableStateFlow<Boolean> = MutableStateFlow(isEnabled)
    private var buttonLabel: MutableStateFlow<String> = MutableStateFlow(getDefaultString(attrs))
    private var buttonHeight: MutableStateFlow<Dp?> = MutableStateFlow(null)
    private var buttonFontSize: MutableStateFlow<Float?> =
        MutableStateFlow(getFontSize(attrs)?.let(::pxToSp))

    private var clickListener: OnClickListener? = null

    init {
        init()
    }

    private fun init() {
        inflate(context, R.layout.snabble_primary_button, this).apply {
            val container: ComposeView? = findViewById(R.id.button_container)
            container?.apply {
                setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ThemeWrapper {
                        val isEnable = isButtonEnabled.collectAsStateWithLifecycle().value
                        val text = buttonLabel.collectAsStateWithLifecycle().value
                        val height = buttonHeight.collectAsStateWithLifecycle().value
                        val fontSize = buttonFontSize.collectAsStateWithLifecycle().value

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height ?: ButtonDefaults.MinHeight),
                            onClick = { clickListener?.onClick(this) },
                            enabled = isEnable
                        ) {
                            Text(
                                text = text,
                                fontSize = fontSize?.sp ?: TextUnit.Unspecified
                            )
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

    private fun getFontSize(attrs: AttributeSet?): Float? {
        context.theme.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize), 0, 0)
            .apply {
                return try {
                    val pixel = getDimensionPixelSize(0, 0).toFloat()
                    when {
                        pixel <= 0f -> null
                        else -> pixel
                    }
                } finally {
                    recycle()
                }
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            val height = layoutParams.height
            if (height > 0) {
                buttonHeight.value = pxToDp(height).dp
            }
        }
    }

    fun setText(@StringRes value: Int) {
        buttonLabel.value = context.getString(value)
    }

    fun setText(value: String) {
        buttonLabel.value = value
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isButtonEnabled.value = enabled
    }

    private fun pxToDp(px: Int): Float {
        val metrics: DisplayMetrics = resources.displayMetrics
        return px / (metrics.densityDpi / BASELINE_DENSITY)
    }

    private fun pxToSp(px: Float): Float {
        val dm = resources.displayMetrics
        val scaledDensity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)
        } else {
            @Suppress("DEPRECATION")
            dm.scaledDensity
        }
        return px / scaledDensity
    }

    companion object {

        private const val BASELINE_DENSITY = 160f
    }
}
