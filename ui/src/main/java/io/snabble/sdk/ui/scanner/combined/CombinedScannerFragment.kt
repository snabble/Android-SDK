package io.snabble.sdk.ui.scanner.combined

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.snabble.sdk.Snabble
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.scanner.BarcodeScannerView
import io.snabble.sdk.ui.scanner.ScannerBottomSheetBehavior
import io.snabble.sdk.ui.scanner.ScannerBottomSheetView
import io.snabble.sdk.ui.scanner.ScannerParallaxBehavior
import io.snabble.sdk.ui.scanner.SelfScanningFragment
import io.snabble.sdk.ui.utils.SnackbarUtils
import io.snabble.sdk.ui.utils.behavior
import io.snabble.sdk.utils.Utils
import kotlin.math.min

class CombinedScannerFragment : SelfScanningFragment() {

    private lateinit var coordinatorView: CoordinatorLayout
    private lateinit var scannerBottomSheetView: ScannerBottomSheetView
    private var scanHint: Snackbar? = null
    private var isPaused = false
    private var cart: ShoppingCart? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allowShowingHints = false
    }

    override fun onResume() {
        super.onResume()

        Snabble.checkedInProject.observe(viewLifecycleOwner) { project ->
            cart?.removeListener(shoppingCartListener)
            cart = project?.shoppingCart
            cart?.addListener(shoppingCartListener)
        }

        if (hasSelfScanningView) {
            selfScanningView?.let { selfScanningView ->
                selfScanningView.setDefaultButtonVisibility(false)

                val barcodeScannerView = selfScanningView.findViewById<BarcodeScannerView>(R.id.barcode_scanner_view)
                selfScanningView.behavior = ScannerParallaxBehavior(barcodeScannerView)
                selfScanningView.setIndicatorOffset(0, 0)

                if (isPaused) {
                    selfScanningView.pause()
                } else {
                    selfScanningView.resume()
                }
            }
            scannerBottomSheetView.isVisible =
                (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onCreateActualView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        coordinatorView = inflater
            .inflate(R.layout.snabble_fragment_combined_scanner, container, false) as CoordinatorLayout
        coordinatorView.keepScreenOn = true
        return coordinatorView
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)
        scannerBottomSheetView = view.findViewById(R.id.cart)
        scannerBottomSheetView.isVisible =
            (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
        Snabble.checkedInProject.observe(viewLifecycleOwner) { project ->
            project?.let {
                scannerBottomSheetView.cart = it.shoppingCart
            }
        }
        scannerBottomSheetView.behavior = ScannerBottomSheetBehavior(scannerBottomSheetView).apply {
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        selfScanningView?.pause()
                        isPaused = true
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (isPaused) {
                        selfScanningView?.resume()
                        isPaused = false
                    }
                }
            })
        }

        if (cart?.isEmpty == true) {
            scanHint = SnackbarUtils.make(view, getString(R.string.Snabble_Scanner_firstScan), 30000)
                .apply {
                    setAction(android.R.string.ok) { dismiss() }
                    gravity = Gravity.TOP
                    show()
                }
        }

        scannerBottomSheetView.onItemsChangedListener += ::cartChanged
    }

    private fun cartChanged(cart: ShoppingCart) {
        val behavior = scannerBottomSheetView.behavior as ScannerBottomSheetBehavior
        if (cart.isEmpty && behavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            behavior.halfExpandedRatio = 0.5f
        } else {
            if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                var itemHeight = scannerBottomSheetView.checkout.height.toFloat()
                for (position in 0..min(4, cart.size())) {
                    itemHeight += Utils.dp2px(context, 48.0f)
                }
                behavior.halfExpandedRatio = (itemHeight / rootView.height).coerceAtMost(0.5f)
                behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
        }
    }

    private val shoppingCartListener: ShoppingCartListener = object : SimpleShoppingCartListener() {
        override fun onChanged(cart: ShoppingCart) {
            scanHint?.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        cart?.addListener(shoppingCartListener)
    }

    override fun onStop() {
        super.onStop()

        cart?.removeListener(shoppingCartListener)
    }
}
