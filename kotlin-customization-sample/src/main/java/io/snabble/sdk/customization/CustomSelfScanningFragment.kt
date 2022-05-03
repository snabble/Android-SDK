package io.snabble.sdk.customization

import io.snabble.sdk.ui.scanner.SelfScanningFragment
import io.snabble.sdk.ui.scanner.SelfScanningView

class CustomSelfScanningFragment: SelfScanningFragment() {
    // Inject custom ProductConfirmationDialog with a simple factory
    override fun onSelfScanningViewCreated(selfScanningView: SelfScanningView) {
        selfScanningView.setProductConfirmationDialogFactory {
            FancyProductConfirmationDialog()
        }
    }
}