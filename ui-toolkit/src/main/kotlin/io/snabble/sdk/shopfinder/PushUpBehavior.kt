package io.snabble.sdk.shopfinder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.snabble.sdk.ui.toolkit.R
//Todo: Use for snabble and teo

@Keep
class PushUpBehavior(context: Context?, attrs: AttributeSet?) : CoordinatorLayout.Behavior<View>(context, attrs) {
    override fun layoutDependsOn(parent: CoordinatorLayout, v: View, dependency: View): Boolean {
        return dependency.id == R.id.bottom_sheet
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, v: View, dependency: View) {
        val p = v.layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(0, 0, 0, 0)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, v: View, dependency: View): Boolean {
        val paddingBottom = (-(dependency.y - parent.height)).toInt()
        val p = v.layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(0, 0, 0, paddingBottom)
        v.layoutParams = p
        return true
    }
}