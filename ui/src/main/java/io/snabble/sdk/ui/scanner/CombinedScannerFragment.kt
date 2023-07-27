package io.snabble.sdk.ui.scanner

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.Snabble
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.GestureHandler
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.ShoppingCartView
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

                val cartAdapter = ShoppingCartView.ShoppingCartAdapter(scannerBottomSheetView, it.shoppingCart)
                scannerBottomSheetView.shoppingCartAdapter = cartAdapter
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
        createItemTouchHelper()
    }

    private fun createItemTouchHelper() {
        val gestureHandler: GestureHandler<Void> = object : GestureHandler<Void>(resources) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                cart?.get(pos)?.let { item ->
                    scannerBottomSheetView.shoppingCartAdapter?.removeAndShowUndoSnackbar(pos, item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(gestureHandler)
        gestureHandler.setItemTouchHelper(itemTouchHelper)
        itemTouchHelper.attachToRecyclerView(scannerBottomSheetView.recyclerView)
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
        override fun onChanged(list: ShoppingCart?) {
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
