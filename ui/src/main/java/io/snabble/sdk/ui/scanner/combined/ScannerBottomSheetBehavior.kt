package io.snabble.sdk.ui.scanner.combined

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ScannerBottomSheetBehavior(
    private val view: ScannerBottomSheetView,
) : BottomSheetBehavior<ScannerBottomSheetView>(view.context, null) {

    private var slideSlop = 0f
    private var enableSlideSlop = false

    init {
        isHideable = false
        state = STATE_COLLAPSED
        isFitToContents = false
        halfExpandedRatio = 0.5f
        isGestureInsetBottomIgnored = true

        addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // reset halfExpandedRatio after min or max size
                if (newState == STATE_COLLAPSED || newState == STATE_EXPANDED) {
                    halfExpandedRatio = 0.5f
                }

                if (newState == STATE_HALF_EXPANDED) {
                    enableSlideSlop = true
                } else if (newState == STATE_EXPANDED) {
                    enableSlideSlop = false
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (enableSlideSlop && slideSlop > 0 && slideOffset < slideSlop && state == STATE_SETTLING) {
                    state = STATE_COLLAPSED
                    slideSlop = 0f
                    return
                }

                if (slideOffset > halfExpandedRatio) {
                    slideSlop = 0f
                }

                if (state == STATE_SETTLING && slideOffset < halfExpandedRatio) {
                    slideSlop = slideOffset
                }

                // fixes the stupid bug where layout changes while animating
                // results in slideOffsets that are negative
                if (slideOffset < 0.0f) {
                    state = STATE_COLLAPSED
                }
            }
        })
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: ScannerBottomSheetView, parentWidthMeasureSpec: Int,
        widthUsed: Int, parentHeightMeasureSpec: Int,
        heightUsed: Int,
    ): Boolean {
        peekHeight = view.peekHeight

        return super.onMeasureChild(
            parent,
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed
        )
    }
}
