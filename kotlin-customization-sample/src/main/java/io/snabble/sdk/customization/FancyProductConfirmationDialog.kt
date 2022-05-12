package io.snabble.sdk.customization

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.ui.scanner.ProductConfirmationDialog
import io.snabble.sdk.ui.utils.bindTextOrHide

class FancyProductConfirmationDialog: DialogFragment(), ProductConfirmationDialog {
    private var onDismissListener: ProductConfirmationDialog.OnDismissListener? = null
    private var onShowListener: ProductConfirmationDialog.OnShowListener? = null
    private var onKeyListener: ProductConfirmationDialog.OnKeyListener? = null
    private lateinit var viewModel: ProductConfirmationDialog.ViewModel

    override fun show(activity: FragmentActivity, viewModel: ProductConfirmationDialog.ViewModel) {
        dismiss(false)
        this.viewModel = viewModel
        show(activity.supportFragmentManager, null)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener(onShowListener)
            setOnKeyListener(onKeyListener)
            window?.attributes?.windowAnimations = R.style.SimpleDialogAnimation
        }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.dismiss()
        onDismissListener?.onDismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View =
        inflater.inflate(R.layout.product_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val addToCart = view.findViewById<Button>(R.id.addToCart)
        addToCart.setOnClickListener {
            viewModel.addToCart()
            dismiss(true)
        }
        addToCart.bindTextOrHide(viewModel.addToCartButtonText)
        val title = view.findViewById<TextView>(R.id.title)
        title.text = viewModel.product.name
    }

    override fun dismiss(addToCart: Boolean) {
        if (isAdded) dismiss()
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