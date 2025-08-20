package io.snabble.sdk.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.animation.AnimationUtils
import io.snabble.sdk.announceAccessibiltyEvent
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Utils
import io.snabble.sdk.utils.Utils.dp2px
import androidx.core.view.contains

class MessageBox @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val text: TextView by lazy { findViewById(R.id.text) }
    val cardView: CardView by lazy { findViewById(R.id.card_view) }

    init {
        inflate(context, R.layout.snabble_view_message_box, this)
        isVisible = false
    }

    fun animateViewIn() {
        post {
            isVisible = true
            startFadeInAnimation()
        }
    }

    fun animateViewOut(onEnd: Runnable) {
        startFadeOutAnimation(onEnd)
    }

    private fun startFadeInAnimation() {
        val alphaAnimator = getAlphaAnimator(0f, 1f)
        val scaleAnimator: ValueAnimator = getScaleAnimator(0.8f, 1.0f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(alphaAnimator, scaleAnimator)
        animatorSet.duration = 150
        animatorSet.start()
    }

    private fun startFadeOutAnimation(onEnd: Runnable) {
        val animator = getAlphaAnimator(1f, 0f)
        animator.duration = 75
        animator.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    onEnd.run()
                }
        })
        animator.start()
    }

    @SuppressLint("RestrictedApi")
    private fun getAlphaAnimator(vararg alphaValues: Float): ValueAnimator {
        val animator = ValueAnimator.ofFloat(*alphaValues)
        animator.interpolator = AnimationUtils.LINEAR_INTERPOLATOR
        animator.addUpdateListener { valueAnimator -> alpha = valueAnimator.animatedValue as Float }
        return animator
    }

    @SuppressLint("RestrictedApi")
    private fun getScaleAnimator(vararg scaleValues: Float): ValueAnimator {
        val animator = ValueAnimator.ofFloat(*scaleValues)
        animator.interpolator = AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
        animator.addUpdateListener { valueAnimator ->
            val scale = valueAnimator.animatedValue as Float
            scaleX = scale
            scaleY = scale
        }
        return animator
    }
}

class MessageBoxStackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.snabble_view_message_box_stack, this)
        orientation = VERTICAL
    }

    fun show(message: String,
             duration: Long = 5000,
             bgColor: Int = Color.WHITE,
             textColor: Int = Color.BLACK) {
        val tag = Utils.sha1Hex(message)
        val duplicate = findViewWithTag<MessageBox>(tag)

        if (duplicate != null) {
            removeView(duplicate)
        }

        val messageBox = MessageBox(context)
        messageBox.text.text = message
        messageBox.text.setTextColor(textColor)
        messageBox.tag = tag
        messageBox.cardView.setCardBackgroundColor(bgColor)

        val lp = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        lp.leftMargin = dp2px(context, 16.0f)
        lp.topMargin = dp2px(context, 8.0f)
        lp.rightMargin = dp2px(context, 16.0f)
        lp.bottomMargin = dp2px(context, 8.0f)
        addView(messageBox, 0, lp)

        messageBox.animateViewIn()

        Dispatch.mainThread({
            if (contains(messageBox)) {
                messageBox.animateViewOut {
                    removeView(messageBox)
                }
            }
        }, duration)
        announceAccessibiltyEvent(message)
    }
}
