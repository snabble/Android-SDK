package io.snabble.sdk.ui.scanner

import android.content.Context
import io.snabble.sdk.ui.SnabbleUI.executeAction
import android.widget.EditText
import android.widget.TextView
import android.content.DialogInterface
import io.snabble.sdk.ui.R
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import io.snabble.sdk.ui.telemetry.Telemetry
import android.view.Gravity
import io.snabble.sdk.ui.SnabbleUI
import android.view.KeyEvent
import android.view.View
import android.view.animation.TranslateAnimation
import android.view.animation.CycleInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.snabble.sdk.*
import io.snabble.sdk.ui.accessibility
import io.snabble.sdk.ui.utils.*
import io.snabble.sdk.utils.Dispatch
import java.lang.NumberFormatException

/**
 * The default implementation of the product confirmation dialog with the option to enter discounts
 * and change the quantity.
 */
class DefaultProductConfirmationDialog(
    private val context: Context,
) : ProductConfirmationDialog {
    private var alertDialog: AlertDialog? = null
    private lateinit var quantity: EditText
    private lateinit var quantityTextInput: View
    private lateinit var price: TextView
    private lateinit var originalPrice: TextView
    private lateinit var depositPrice: TextView
    private lateinit var quantityAnnotation: TextView
    private lateinit var addToCart: AppCompatButton
    private lateinit var plusLayout: View
    private lateinit var minusLayout: View
    private lateinit var enterReducedPrice: Button
    private var onDismissListener: DialogInterface.OnDismissListener? = null
    private var onShowListener: DialogInterface.OnShowListener? = null
    private var onKeyListener: DialogInterface.OnKeyListener? = null
    var wasAddedToCart = false
        private set

    /**
     * Show the dialog for the given project and the scanned code.
     */
    override fun show(viewModel: ProductConfirmationDialog.ViewModel) {
        dismiss(false)
        val view = View.inflate(context, R.layout.snabble_dialog_product_confirmation, null)
        alertDialog = LifecycleAwareAlertDialogBuilder(context)
            .setView(view)
            .create()
            .apply {
                setOnShowListener(onShowListener)
                setOnDismissListener {
                    viewModel.dismiss()
                    onDismissListener?.onDismiss(it)
                }
                setOnKeyListener(onKeyListener)
            }
        quantity = view.findViewById(R.id.quantity)
        quantityTextInput = view.findViewById(R.id.quantity_text_input)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
        val name = view.findViewById<TextView>(R.id.name)
        price = view.findViewById(R.id.price)
        originalPrice = view.findViewById(R.id.originalPrice)
        depositPrice = view.findViewById(R.id.depositPrice)
        quantityAnnotation = view.findViewById(R.id.quantity_annotation)
        addToCart = view.findViewById(R.id.addToCart)
        val close = view.findViewById<View>(R.id.close)
        val plus = view.findViewById<View>(R.id.plus)
        val minus = view.findViewById<View>(R.id.minus)
        plusLayout = view.findViewById(R.id.plus_layout)
        minusLayout = view.findViewById(R.id.minus_layout)
        enterReducedPrice = view.findViewById(R.id.enterReducedPrice)

        close.accessibility {
            onInitializeAccessibilityNodeInfo { info ->
                info.setTraversalAfter(addToCart)
            }

            onPopulateAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { _, _, event ->
                var contextHint = close.resources.getString(R.string.Snabble_Scanner_Accessibility_eventBarcodeDetected)
                // Special case where the edit view is directly focused, we need to provide more context
                if (viewModel.product.type == Product.Type.UserWeighed) {
                    // Add full product name
                    contextHint += ". " + viewModel.product.subtitle + ". " + viewModel.product.name + " "
                    // Add price
                    contextHint += viewModel.priceContentDescription.value + ". "
                    // Add what to enter
                    // FIXME (low prio) talkback will say "Please enter the quantity in 'g'" instead of saying "gramme", but this should be good enough for now
                    contextHint += close.resources.getString(R.string.Snabble_Scanner_Accessibility_enterQuantity, viewModel.cartItem.unit?.displayValue)
                }
                event.text.add(contextHint)
            }
        }

        viewModel.quantity.observe(requireNotNull(quantity.findViewTreeLifecycleOwner())) { count ->
            if (getQuantity() != count) {
                quantity.setText(count?.toString())
            }
            count?.let {
                quantity.announceForAccessibility(
                    quantity.resources.getString(
                        R.string.Snabble_Scanner_Accessibility_eventQuantityUpdate,
                        count,
                        viewModel.cartItem.displayName,
                        viewModel.cartItem.totalPriceText
                    )
                )
            }
        }

        originalPrice.bindTextOrHide(viewModel.originalPrice)
        depositPrice.bindTextOrHide(viewModel.depositPrice)

        enterReducedPrice.bindTextOrHide(viewModel.enterReducedPriceButtonText)
        enterReducedPrice.setOnClickListener {
            val fragmentManager = UIUtils.getHostFragmentActivity(context).supportFragmentManager
            SelectReducedPriceDialogFragment(viewModel).show(fragmentManager, null)
        }
        plusLayout.bindVisibility(viewModel.quantityButtonsVisible)
        plusLayout.bindEnabledState(viewModel.quantityCanBeIncreased)
        plus.bindEnabledState(viewModel.quantityCanBeIncreased)
        minusLayout.bindVisibility(viewModel.quantityButtonsVisible)
        minusLayout.bindEnabledState(viewModel.quantityCanBeDecreased)
        minus.bindEnabledState(viewModel.quantityCanBeDecreased)

        name.text = viewModel.product.name
        subtitle.setTextOrHide(viewModel.product.subtitle)
        quantity.bindEnabledState(viewModel.quantityCanBeChanged)
        quantityTextInput.bindVisibility(viewModel.quantityVisible)
        quantityAnnotation.bindVisibility(viewModel.quantityVisible)
        price.isVisible = true
        quantity.clearFocus()
        quantityAnnotation.setTextOrHide(viewModel.cartItem.unit?.displayValue)
        quantity.filters = arrayOf(InputFilterMinMax(1, ShoppingCart.MAX_QUANTITY))
        quantity.setOnEditorActionListener { _, actionId: Int, event: KeyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                viewModel.addToCart()
                return@setOnEditorActionListener true
            }
            false
        }
        quantity.addTextChangedListener { s ->
            val number = s.toString().toIntOrNull()
            viewModel.quantity.postWhenChanged(number)
        }
        price.bindText(viewModel.price)
        plus.setOnClickListener {
            var q = getQuantity()
            if (q < ShoppingCart.MAX_QUANTITY) {
                viewModel.quantity.postValue(++q)
            } else {
                plus.announceForAccessibility(plus.resources.getString(R.string.Snabble_Scanner_Accessibility_eventMaxQuantityReached))
            }
        }
        minus.setOnClickListener {
            var q = getQuantity()
            viewModel.quantity.postValue(--q)
        }
        addToCart.setOnClickListener {
            viewModel.addToCart()
            alertDialog?.dismiss()
        }
        close.setOnClickListener {
            Telemetry.event(Telemetry.Event.RejectedProduct, viewModel.product)
            dismiss(false)
        }
        val window = alertDialog?.window
        if (window == null) {
            alertDialog?.dismiss()
            return
        }
        val layoutParams = window.attributes
        layoutParams.y = 48.dpInPx
        window.setBackgroundDrawableResource(R.drawable.snabble_scanner_dialog_background)
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        window.attributes = layoutParams
        alertDialog?.show()
        if (viewModel.product.type == Product.Type.UserWeighed) {
            quantity.requestFocus()
            Dispatch.mainThread {
                val inputMethodManager = context.applicationContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(quantity, 0)
            }
        }
        executeAction(context, SnabbleUI.Event.PRODUCT_CONFIRMATION_SHOWN)
    }

    private fun shake() {
        val shake = TranslateAnimation(0f, 3.dp, 0f, 0f)
        shake.duration = 500
        shake.interpolator = CycleInterpolator(7f)
        quantity.startAnimation(shake)
    }

    private fun getQuantity() = try {
        quantity.text.toString().toInt()
    } catch (e: NumberFormatException) {
        0
    }

    override fun dismiss(addToCart: Boolean) {
        wasAddedToCart = addToCart
        alertDialog?.let { alertDialog ->
            alertDialog.dismiss()
            alertDialog.setOnDismissListener(null)
            this.alertDialog = null
            if (!addToCart) {
                executeAction(context, SnabbleUI.Event.PRODUCT_CONFIRMATION_HIDDEN)
            }
        }
    }

    override fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        this.onDismissListener = onDismissListener
    }

    override fun setOnShowListener(onShowListener: DialogInterface.OnShowListener?) {
        this.onShowListener = onShowListener
    }

    override fun setOnKeyListener(onKeyListener: DialogInterface.OnKeyListener?) {
        this.onKeyListener = onKeyListener
    }
}