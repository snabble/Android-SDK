package io.snabble.sdk.ui.checkout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.setTextOrHide

@Suppress("LeakingThis")
open class PaymentStatusItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    enum class State {
        NOT_EXECUTED,
        IN_PROGRESS,
        SUCCESS,
        FAILED
    }

    private val titleView: TextView
    private val contentLayout: LinearLayout
    private val progress: ProgressBar
    private val image: ImageView
    private var text: TextView
    private var action: Button

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status_item, this)
        orientation = HORIZONTAL

        titleView = findViewById(R.id.title)
        contentLayout = findViewById(R.id.content)
        progress = findViewById(R.id.progress)
        image = findViewById(R.id.image)
        text = findViewById(R.id.text)
        action = findViewById(R.id.action)
    }

    var state = State.IN_PROGRESS
        set(value) {
            field = value

            when (value) {
                State.NOT_EXECUTED -> {
                    image.visibility = View.VISIBLE
                    image.setImageResource(R.drawable.snabble_ic_payment_notdone)
                    progress.visibility = View.GONE
                }
                State.IN_PROGRESS -> {
                    image.visibility = View.GONE
                    progress.visibility = View.VISIBLE
                }
                State.SUCCESS -> {
                    image.visibility = View.VISIBLE
                    image.setImageResource(R.drawable.snabble_ic_payment_success)
                    progress.visibility = View.GONE
                }
                State.FAILED -> {
                    image.visibility = View.VISIBLE
                    image.setImageResource(R.drawable.snabble_ic_payment_error)
                    progress.visibility = View.GONE
                }
            }
        }

    fun setTitle(title: String?) {
        titleView.setTextOrHide(title)
    }

    fun setText(t: String?) {
        text.setTextOrHide(t)
    }

    fun setAction(text: String?, onClickListener: OnClickListener) {
        if (text != null) {
            action.isVisible = true
            action.text = text
            action.setOnClickListener(onClickListener)
        } else {
            action.isVisible = false
        }
    }
}