package io.snabble.sdk.ui.scanner.combined

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.scanner.BarcodeScannerView
import io.snabble.sdk.ui.scanner.SelfScanningView

class ScannerParallaxBehavior(private val barcodeScannerView: BarcodeScannerView) :
    CoordinatorLayout.Behavior<SelfScanningView>(barcodeScannerView.context, null) {

    override fun layoutDependsOn(parent: CoordinatorLayout, scanner: SelfScanningView, dependency: View): Boolean {
        return dependency.id == R.id.cart
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        scanner: SelfScanningView,
        dependency: View,
    ): Boolean {
        val translationY = (dependency.y - parent.height) / 2
        barcodeScannerView.translationY = translationY
        return true
    }
}
