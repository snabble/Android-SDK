package io.snabble.sdk.ui.scanner

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ShoppingCart.ShoppingCartListener
import io.snabble.sdk.ShoppingCart.SimpleShoppingCartListener
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.GestureHandler
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.CheckoutBar
import io.snabble.sdk.ui.cart.ShoppingCartView
import io.snabble.sdk.ui.utils.SnackbarUtils
import io.snabble.sdk.ui.utils.behavior
import io.snabble.sdk.ui.utils.bindTextOrHide
import io.snabble.sdk.utils.Utils
import kotlin.math.min


class CombinedScannerFragment : BaseFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateActualView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.snabble_fragment_combined_scanner, container, false)
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        val gotoCartButton = view.findViewById<View>(R.id.goto_cart)
        gotoCartButton.isVisible = false

        val selfScanningView = view.findViewById<SelfScanningView>(R.id.selfScanningView)
        selfScanningView.setIndicatorOffset(0, 0)

        selfScanningView.setProductConfirmationDialogFactory(object : ProductConfirmationDialog.Factory() {
            override fun create(): ProductConfirmationDialog {

            }
        })
    }
}