package io.snabble.sdk.ui.scanner

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import io.snabble.sdk.ui.SnabbleUI.executeAction
import android.widget.EditText
import android.widget.TextView
import android.content.DialogInterface
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.ui.R
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import io.snabble.sdk.ui.telemetry.Telemetry
import android.view.Gravity
import io.snabble.sdk.ui.SnabbleUI
import android.text.style.StrikethroughSpan
import com.squareup.picasso.Picasso
import android.os.Bundle
import io.snabble.sdk.utils.GsonHolder
import android.content.pm.PackageManager
import android.os.Vibrator
import android.text.*
import android.view.KeyEvent
import android.view.View
import android.view.animation.TranslateAnimation
import android.view.animation.CycleInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import io.snabble.sdk.*
import io.snabble.sdk.Unit
import io.snabble.sdk.ui.accessibility
import io.snabble.sdk.ui.utils.*
import io.snabble.sdk.utils.Dispatch
import java.lang.NumberFormatException

/**
 * The product confirmation dialog with the option to enter discounts and change the quantity.
 */
class ProductConfirmationDialog(
    private val context: Context,
    private val project: Project
) {
    private var alertDialog: AlertDialog? = null
    private val shoppingCart = project.shoppingCart
    private val priceFormatter = project.priceFormatter
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
    private var cartItem: ShoppingCart.Item? = null
    private var onDismissListener: DialogInterface.OnDismissListener? = null
    private var onShowListener: DialogInterface.OnShowListener? = null
    private var onKeyListener: DialogInterface.OnKeyListener? = null
    var wasAddedToCart = false
        private set

    /**
     * Show the dialog for the given project and the scanned code.
     */
    fun show(product: Product, scannedCode: ScannedCode) {
        dismiss(false)
        val view = View.inflate(context, R.layout.snabble_dialog_product_confirmation, null)
        alertDialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
            .apply {
                setOnShowListener(onShowListener)
                setOnDismissListener(onDismissListener)
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

            onAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { _, _, event ->
                event.text.add(close.resources.getString(R.string.Snabble_Scanner_Accessibility_eventBarcodeDetected))
            }
        }

        name.text = product.name
        subtitle.setTextOrHide(product.subtitle)
        if (scannedCode.hasEmbeddedData() && scannedCode.embeddedData > 0) {
            quantity.isEnabled = false
        }
        price.isVisible = true
        quantity.clearFocus()
        cartItem = shoppingCart.newItem(product, scannedCode)
        quantityAnnotation.setTextOrHide(cartItem?.unit?.displayValue)
        updateQuantityText()
        quantity.filters = arrayOf(InputFilterMinMax(1, ShoppingCart.MAX_QUANTITY))
        quantity.setOnEditorActionListener { _, actionId: Int, event: KeyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                addToCart()
                return@setOnEditorActionListener true
            }
            false
        }
        quantity.addTextChangedListener { s ->
            // its possible that the callback gets called before a dismiss is dispatched
            // and when that happens the product is already null
            if (cartItem == null) {
                dismiss(false)
            } else {
                val number = try {
                    s.toString().toInt()
                } catch (e: NumberFormatException) {
                    0
                }
                cartItem?.quantity = number
                updatePrice()
            }
        }
        updatePrice()
        plus.setOnClickListener {
            var q = getQuantity()
            if (q < ShoppingCart.MAX_QUANTITY) {
                setQuantity(++q)
            } else {
                plus.announceForAccessibility(plus.resources.getString(R.string.Snabble_Scanner_Accessibility_eventMaxQuantityReached))
            }
        }
        minus.setOnClickListener { v: View? ->
            var q = getQuantity()
            setQuantity(--q)
        }
        addToCart.setOnClickListener { addToCart() }
        close.setOnClickListener {
            Telemetry.event(Telemetry.Event.RejectedProduct, cartItem?.product)
            cartItem = null
            dismiss(false)
        }
        val window = alertDialog?.window
        if (window == null) {
            cartItem = null
            return
        }
        val layoutParams = window.attributes
        layoutParams.y = 48.dpInPx
        window.setBackgroundDrawableResource(R.drawable.snabble_scanner_dialog_background)
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        window.attributes = layoutParams
        alertDialog?.show()
        if (product.type == Product.Type.UserWeighed) {
            quantity.requestFocus()
            Dispatch.mainThread {
                val inputMethodManager = context
                    .applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(quantity, 0)
            }
        }
        executeAction(context, SnabbleUI.Event.PRODUCT_CONFIRMATION_SHOWN)
    }

    fun updateQuantityText() {
        val existingItem = shoppingCart.getExistingMergeableProduct(cartItem!!.product)
        val isMergeable = existingItem != null && existingItem.isMergeable && cartItem!!.isMergeable
        if (isMergeable) {
            setQuantity(existingItem.effectiveQuantity + 1)
            addToCart.setText(R.string.Snabble_Scanner_updateCart)
        } else {
            setQuantity(cartItem!!.effectiveQuantity)
            addToCart.setText(R.string.Snabble_Scanner_addToCart)
        }
    }

    fun updatePrice() {
        val cartItem = requireNotNull(this.cartItem)
        val product = requireNotNull(cartItem.product)
        val fullPriceText = cartItem.fullPriceText
        if (fullPriceText != null) {
            price.text = cartItem.fullPriceText
            price.contentDescription = price.resources.getString(
                R.string.Snabble_Shoppingcart_Accessibility_descriptionForPrice,
                cartItem.fullPriceText
            )
            price.visibility = View.VISIBLE
            if (product.listPrice > product.getPrice(project.customerCardId)) {
                val originalPriceText = SpannableString(priceFormatter.format(product.listPrice))
                originalPriceText.setSpan(
                    StrikethroughSpan(),
                    0,
                    originalPriceText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                originalPrice.visibility = View.VISIBLE
                originalPrice.text = originalPriceText
            } else {
                originalPrice.visibility = View.GONE
            }
        } else {
            price.visibility = View.GONE
            originalPrice.visibility = View.GONE
        }
        var cartItemDepositPrice = cartItem.totalDepositPrice
        if (cartItemDepositPrice == 0) {
            cartItemDepositPrice = product.depositProduct.getPrice(project.customerCardId)
        }
        if (cartItemDepositPrice > 0) {
            val price = priceFormatter.format(cartItemDepositPrice)
            val text = context.resources.getString(R.string.Snabble_Scanner_plusDeposit, price)
            depositPrice.text = text
            depositPrice.visibility = View.VISIBLE
        } else {
            depositPrice.visibility = View.GONE
        }
        val manualCoupons = project.coupons.filter(CouponType.MANUAL)
        enterReducedPrice.isVisible = manualCoupons.isNotEmpty()
        enterReducedPrice.setOnClickListener {
            val fragmentActivity = UIUtils.getHostFragmentActivity(context)
            SelectReducedPriceDialogFragment(this@ProductConfirmationDialog, cartItem, shoppingCart)
                .show(fragmentActivity.supportFragmentManager, null)
        }
        if (cartItem.coupon != null) {
            enterReducedPrice.text = cartItem.coupon.name
        } else {
            enterReducedPrice.setText(R.string.Snabble_addDiscount)
        }
    }

    fun addToCart() {
        // its possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        cartItem?.let { cartItem ->
            Telemetry.event(Telemetry.Event.ConfirmedProduct, cartItem.product)
            val q = Math.max(getQuantity(), cartItem.scannedCode.embeddedData)
            if (cartItem.product!!.type == Product.Type.UserWeighed && q == 0) {
                shake()
                return
            }
            if (shoppingCart.indexOf(cartItem) == -1) {
                shoppingCart.add(cartItem)
            }
            if (cartItem.product?.type == Product.Type.UserWeighed) {
                cartItem.quantity = q
            }
            shoppingCart.updatePrices(false)

            // warm up the image cache
            val imageUrl = cartItem.product?.imageUrl
            if (imageUrl.isNotNullOrBlank()) {
                Picasso.get().load(imageUrl).fetch()
            }
            val args = Bundle()
            args.putString("cartItem", GsonHolder.get().toJson(cartItem))
            executeAction(context, SnabbleUI.Event.PRODUCT_CONFIRMATION_HIDDEN, args)
            dismiss(true)
            if (Snabble.config.vibrateToConfirmCartFilled &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                // noinspection MissingPermission, check is above
                vibrator.vibrate(200L)
            }
        } ?: dismiss(false)
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

    fun setQuantity(number: Int) {
        var count = number
        // It's possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        cartItem?.let { cartItem ->
            if (cartItem.isEditableInDialog) {
                quantity.isEnabled = true
                if (cartItem.product?.type == Product.Type.UserWeighed) {
                    plusLayout.visibility = View.GONE
                    minusLayout.visibility = View.GONE
                } else {
                    plusLayout.visibility = View.VISIBLE
                    minusLayout.visibility = View.VISIBLE
                    quantity.visibility = View.VISIBLE
                    quantityTextInput.visibility = View.VISIBLE
                }
            } else {
                quantity.isEnabled = false
                plusLayout.visibility = View.GONE
                minusLayout.visibility = View.GONE
                quantity.visibility = View.GONE
                quantityTextInput.visibility = View.GONE
                quantityAnnotation.visibility = View.GONE
            }
            if (cartItem.product?.type != Product.Type.UserWeighed) {
                if (cartItem.product?.type == Product.Type.Article) {
                    count = count.coerceAtLeast(1)
                }
                quantity.setText(count.toString())
                quantity.setSelection(quantity.text.length)
            } else {
                quantity.setText("")
            }
            if (cartItem.unit == Unit.PRICE) {
                quantity.setText(cartItem.priceText)
            }
            cartItem.quantity = count
            updatePrice()
            quantity.announceForAccessibility(
                quantity.resources.getString(
                    R.string.Snabble_Scanner_Accessibility_eventQuantityUpdate,
                    count,
                    cartItem.displayName,
                    cartItem.totalPriceText
                )
            )
        } ?: dismiss(false)
    }

    fun dismiss(addToCart: Boolean) {
        wasAddedToCart = addToCart
        alertDialog?.let { alertDialog ->
            alertDialog.dismiss()
            alertDialog.setOnDismissListener(null)
            this.alertDialog = null
            if (!addToCart) {
                executeAction(context, SnabbleUI.Event.PRODUCT_CONFIRMATION_HIDDEN)
            }
        }
        cartItem = null
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        this.onDismissListener = onDismissListener
    }

    fun setOnShowListener(onShowListener: DialogInterface.OnShowListener?) {
        this.onShowListener = onShowListener
    }

    fun setOnKeyListener(onKeyListener: DialogInterface.OnKeyListener?) {
        this.onKeyListener = onKeyListener
    }
}