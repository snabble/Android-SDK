package io.snabble.sdk.customization

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.ui.scanner.ProductConfirmationDialog
import io.snabble.sdk.ui.scanner.bindTextOrHide
import io.snabble.sdk.ui.utils.LifecycleAwareAlertDialogBuilder

class FancyProductConfirmationDialog(
    private val context: Context
): ProductConfirmationDialog {
    private var alertDialog: AlertDialog? = null
    private var onDismissListener: ProductConfirmationDialog.OnDismissListener? = null
    private var onShowListener: ProductConfirmationDialog.OnShowListener? = null
    private var onKeyListener: ProductConfirmationDialog.OnKeyListener? = null

    override fun show(viewModel: ProductConfirmationDialog.ViewModel) {
        dismiss(false)
        val view = View.inflate(context, R.layout.product_dialog, null)
        alertDialog = LifecycleAwareAlertDialogBuilder(context)
            .setView(view)
            .create()
            .apply {
                setOnShowListener(onShowListener)
                setOnDismissListener {
                    viewModel.dismiss()
                    onDismissListener?.onDismiss()
                }
                setOnKeyListener(onKeyListener)
            }
        val addToCart = view.findViewById<Button>(R.id.addToCart)
        addToCart.setOnClickListener {
            viewModel.addToCart()
            dismiss(true)
        }
        addToCart.bindTextOrHide(viewModel.addToCartButtonText)
        val title = view.findViewById<TextView>(R.id.title)
        title.text = viewModel.product.name
        alertDialog?.window?.attributes?.windowAnimations = R.style.SimpleDialogAnimation
        alertDialog?.show()
    }

    override fun dismiss(addToCart: Boolean) {
        alertDialog?.dismiss()
    }

    override fun setOnDismissListener(onDismissListener: ProductConfirmationDialog.OnDismissListener?) {
        this.onDismissListener = onDismissListener
    }

    override fun setOnShowListener(onShowListener: ProductConfirmationDialog.OnShowListener?) {
        this.onShowListener = onShowListener
    }

    override fun setOnKeyListener(onKeyListener: ProductConfirmationDialog.OnKeyListener?) {
        this.onKeyListener = onKeyListener
    }
}