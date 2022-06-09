package io.snabble.sdk.ui.scanner

import android.app.Dialog
import android.content.Context
import io.snabble.sdk.ui.SnabbleUI.executeAction
import android.widget.EditText
import android.widget.TextView
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import io.snabble.sdk.ui.R
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.SnabbleUI
import android.view.animation.TranslateAnimation
import android.view.animation.CycleInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import io.snabble.sdk.*
import io.snabble.sdk.ui.accessibility
import io.snabble.sdk.ui.utils.*
import io.snabble.sdk.utils.Dispatch
import java.lang.NumberFormatException

/**
 * The default implementation of the product confirmation dialog with the option to enter discounts
 * and change the quantity.
 */
class DefaultProductConfirmationDialog : DialogFragment(), ProductConfirmationDialog {
    private lateinit var quantity: EditText
    private lateinit var quantityContainer: View
    private lateinit var quantityTextInput: TextInputLayout
    private lateinit var price: TextView
    private lateinit var originalPrice: TextView
    private lateinit var depositPrice: TextView
    private lateinit var addToCart: AppCompatButton
    private lateinit var plusLayout: View
    private lateinit var minusLayout: View
    private lateinit var enterReducedPrice: Button
    private var onDismissListener: ProductConfirmationDialog.OnDismissListener? = null
    private var onShowListener: ProductConfirmationDialog.OnShowListener? = null
    private var onKeyListener: ProductConfirmationDialog.OnKeyListener? = null
    var wasAddedToCart = false
        private set
    private lateinit var viewModel: ProductConfirmationDialog.ViewModel

    /**
     * Show the dialog for the given project and the scanned code.
     */
    override fun show(activity: FragmentActivity, viewModel: ProductConfirmationDialog.ViewModel) {
        dismiss(false)
        this.viewModel = viewModel
        show(activity.supportFragmentManager, null)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener(onShowListener)
            setOnKeyListener(onKeyListener)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.dismiss()
        onDismissListener?.onDismiss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View =
        inflater.inflate(R.layout.snabble_dialog_product_confirmation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // restore the view model
        savedInstanceState?.let { state ->
            viewModel = ProductConfirmationDialog.ViewModel(view.context, state)
        }
        quantity = view.findViewById(R.id.quantity)
        quantityContainer = view.findViewById(R.id.quantity_container)
        quantityTextInput = view.findViewById(R.id.quantity_text_input)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
        val name = view.findViewById<TextView>(R.id.name)
        price = view.findViewById(R.id.price)
        originalPrice = view.findViewById(R.id.originalPrice)
        depositPrice = view.findViewById(R.id.depositPrice)
        addToCart = view.findViewById(R.id.addToCart)
        val close = view.findViewById<View>(R.id.close)
        val plus = view.findViewById<MaterialButton>(R.id.plus)
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
                    // FIXME (low prio) talkback will say "Please enter the quantity in 'g'" instead
                    // of saying "gramme", but this should be good enough for now
                    contextHint += close.resources.getString(
                        R.string.Snabble_Scanner_Accessibility_enterQuantity,
                        viewModel.cartItem.unit?.displayValue)
                }
                event.text.add(contextHint)
            }
        }

        viewModel.quantity.observe(viewLifecycleOwner) { count ->
            if (getQuantity() != count) {
                val newCount = count?.toString().orEmpty()
                // keep the selection if possible
                val selectionStart = quantity.selectionStart.coerceAtMost(newCount.length)
                val selectionEnd = quantity.selectionEnd.coerceAtMost(newCount.length)
                quantity.setText(newCount)
                quantity.setSelection(selectionStart, selectionEnd)
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
        price.bindTextOrHide(viewModel.price)

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
        quantityContainer.bindVisibility(viewModel.quantityVisible)
        quantity.clearFocus()
        quantityTextInput.suffixText = viewModel.cartItem.unit?.displayValue
        quantity.filters = arrayOf(InputFilterMinMax(1, ShoppingCart.MAX_QUANTITY))
        quantity.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                return@setOnEditorActionListener checkAddToCart()
            }
            false
        }
        quantity.addTextChangedListener { s ->
            // edge case where the user cleared the field
            if (s?.isNotEmpty() == true) {
                val number = s.toString().toIntOrNull()
                viewModel.quantity.postWhenChanged(number)
            }
            quantityTextInput.setBoxStrokeColorStateList(plus.strokeColor)
        }
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
            if (checkAddToCart()) {
                dismiss()
            }
        }
        close.setOnClickListener {
            Telemetry.event(Telemetry.Event.RejectedProduct, viewModel.product)
            dismiss(false)
        }
        val window = dialog?.window
        if (window == null) {
            dismiss()
            return
        }
        val layoutParams = window.attributes
        layoutParams.y = 48.dpInPx
        window.setBackgroundDrawableResource(R.drawable.snabble_scanner_dialog_background)
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        window.attributes = layoutParams
        if (viewModel.product.type == Product.Type.UserWeighed) {
            quantity.requestFocus()
            Dispatch.mainThread {
                val inputMethodManager = requireContext().applicationContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(quantity, 0)
            }
        }
    }

    private fun checkAddToCart(): Boolean {
        if (getQuantity() > 0) {
            viewModel.addToCart()
        } else {
            quantityTextInput.boxStrokeColor = UIUtils.getColorByAttribute(context, R.attr.colorError)
            shake()
        }
        return getQuantity() > 0
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
        if (isAdded) {
            dismiss()
        }
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