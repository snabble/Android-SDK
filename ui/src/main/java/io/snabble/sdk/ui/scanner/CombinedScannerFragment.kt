package io.snabble.sdk.ui.scanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.Product
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.CheckoutBar
import io.snabble.sdk.ui.utils.dpInPx
import io.snabble.sdk.utils.Dispatch

class CombinedScannerFragment : BaseFragment() {
    lateinit var selfScanningView: SelfScanningView
    lateinit var checkoutBar: CheckoutBar
    lateinit var container: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateActualView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.snabble_fragment_combined_scanner, container, false)
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        container = view.findViewById(R.id.container)
        checkoutBar = view.findViewById(R.id.checkout_bar);

        val gotoCartButton = view.findViewById<View>(R.id.goto_cart)
        gotoCartButton.isVisible = false

        selfScanningView = view.findViewById<SelfScanningView>(R.id.selfScanningView)
        selfScanningView.setIndicatorOffset(0, 0)

        selfScanningView.setProductConfirmationDialogFactory {
            ToastDialog()
        }
    }

    fun showCartItemOverlayView(viewModel: ProductConfirmationDialog.ViewModel) {
        val cartItemOverlayView = container.findViewById<CartItemOverlayView>(R.id.cart_item_overlay_view)
        val cartButtonView = checkoutBar.findViewById<View>(R.id.cart_button_view);

        cartItemOverlayView.isVisible = true
        cartItemOverlayView.cartItem = viewModel.cartItem
        cartItemOverlayView.onRemovedFromCartListener = object : CartItemOverlayView.OnRemovedFromCartListener {
            override fun onDismiss() {
                cartItemOverlayView.isVisible = false
            }
        }

        cartItemOverlayView.scaleX = 0.25f
        cartItemOverlayView.scaleY = 0.25f
        cartItemOverlayView.translationY = -selfScanningView.height / 2 + 60.dpInPx.toFloat()
        cartItemOverlayView.alpha = 0.25f
        cartItemOverlayView.animate()
            .setStartDelay(0)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .translationY(0.0f)
            .alpha(1.0f)
            .setDuration(500)
            .withEndAction {
                cartItemOverlayView.animate()
                    .setStartDelay(5000)
                    .scaleX(0.1f)
                    .scaleY(0.1f)
                    .translationY(120.dpInPx.toFloat())
                    .alpha(0.0f)
                    .setDuration(200)
                    .withEndAction {
                        checkoutBar.playAddToCartAnimation()
                    }
                    .start()
            }
            .start()
    }

    inner class ToastDialog : ProductConfirmationDialog {
        private val defaultProductConfirmationDialog = DefaultProductConfirmationDialog()
        private var onDismissListener: ProductConfirmationDialog.OnDismissListener? = null

        override fun show(
            activity: FragmentActivity,
            viewModel: ProductConfirmationDialog.ViewModel
        ) {
            if (viewModel.product.type == Product.Type.Article) {
                showCartItemOverlayView(viewModel)
                viewModel.addToCart()
                onDismissListener?.onDismiss()
            } else {
                defaultProductConfirmationDialog.show(activity, viewModel)
            }
        }

        override fun dismiss(addToCart: Boolean) {
            defaultProductConfirmationDialog.dismiss(addToCart)
        }

        override fun setOnDismissListener(onDismissListener: ProductConfirmationDialog.OnDismissListener?) {
            this.onDismissListener = onDismissListener
            defaultProductConfirmationDialog.setOnDismissListener(onDismissListener)
        }

        override fun setOnShowListener(onShowListener: ProductConfirmationDialog.OnShowListener?) {
            defaultProductConfirmationDialog.setOnShowListener(onShowListener)
        }

        override fun setOnKeyListener(onKeyListener: ProductConfirmationDialog.OnKeyListener?) {
            defaultProductConfirmationDialog.setOnKeyListener(onKeyListener)
        }
    }
}