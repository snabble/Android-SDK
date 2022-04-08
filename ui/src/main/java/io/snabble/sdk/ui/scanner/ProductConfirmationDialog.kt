package io.snabble.sdk.ui.scanner

import android.Manifest
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
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
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
) {
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
     * The view model of the product confirmation dialog, should be helpful if you want to customize
     * that dialog.
     */
    class ViewModel(
        private val context: Context,
        private val project: Project,
        /** The product raw data */
        val product: Product,
        scannedCode: ScannedCode,
    ) {
        private val priceFormatter = project.priceFormatter
        /** The related shopping cart */
        val shoppingCart = project.shoppingCart
        /** The quantity of the product, can be null */
        val quantity = MutableLiveData(1)
        /** The content description of the quantity, can be null */
        val quantityContentDescription = MutableLiveData<String?>()
        /** TODO check if the default value must be inverted */
        val quantityCanBeChanged: LiveData<Boolean> = MutableLiveData(!(scannedCode.hasEmbeddedData() && scannedCode.embeddedData > 0))
        /** The raw cart item */
        val cartItem = shoppingCart.newItem(product, scannedCode)
        /** The add to cart button text (varies if the user updates the quantity) */
        val addToCartButtonText: LiveData<String> = MutableLiveData()
        /** The price to display */
        val price: LiveData<String> = MutableLiveData()
        /** The content description of the price */
        val priceContentDescription: LiveData<String> = MutableLiveData()
        /** The original price of the product, can be null */
        val originalPrice: LiveData<CharSequence?> = MutableLiveData()
        /** The deposit price of the product, can be null */
        val depositPrice: LiveData<String?> = MutableLiveData()
        /** The button text do enter manual discounts, can be null when no coupons are available */
        val enterReducedPriceButtonText: LiveData<String?> = MutableLiveData()
        /** True when the quantity can be increased. Can be false when there is no quantity or the limit is reached */
        val quantityCanBeIncreased: LiveData<Boolean> = MutableLiveData(true)
        /** True when the quantity can be decreased. Can be false when there is no quantity or the quantity is 1 */
        val quantityCanBeDecreased: LiveData<Boolean> = MutableLiveData(false)
        /** True when the quantity can be changed */
        val quantityVisible: LiveData<Boolean> = MutableLiveData(true)
        /** True when the plus and minus buttons should be shown */
        val quantityButtonsVisible: LiveData<Boolean> = MutableLiveData(true)
        // Might be not really useful, written with compatibility in mind
        private var isDismissed = false
        // syncs the quantity property with the cart item and updates the addToCartButtonText
        private val quantityObserver = Observer<Int?> {
            it?.let {
                cartItem.quantity = it
            }
            val existingItem = shoppingCart.getExistingMergeableProduct(cartItem.product)
            val isMergeable = existingItem != null && existingItem.isMergeable && cartItem.isMergeable
            if (isMergeable) {
                quantity.postValue(existingItem.effectiveQuantity + 1)
                addToCartButtonText.postString(R.string.Snabble_Scanner_updateCart)
            } else {
                quantity.postValue(cartItem.effectiveQuantity)
                addToCartButtonText.postString(R.string.Snabble_Scanner_addToCart)
            }
            quantityContentDescription.postNullableString(
                R.string.Snabble_Scanner_Accessibility_eventQuantityUpdate,
                quantity.value.toString(),
                cartItem.displayName,
                cartItem.totalPriceText
            )
            updatePrice()
            updateButtons()
        }

        init {
            quantity.observeForever(quantityObserver)
            updatePrice()
            updateButtons()
        }

        private fun LiveData<String>.postString(@StringRes string: Int, vararg args: Any?) {
            (this as MutableLiveData<String>).postValue(context.getString(string, *args))
        }

        private fun LiveData<String?>.postNullableString(@StringRes string: Int, vararg args: Any?) {
            (this as MutableLiveData<String?>).postValue(context.getString(string, *args))
        }

        private fun <T> LiveData<T>.postValue(value: T) {
            (this as MutableLiveData<T>).postValue(value)
        }

        private fun <T> LiveData<T>.cycleFreeValue(value: T) {
            if (this.value != value) {
                (this as MutableLiveData<T>).postValue(value)
            }
        }

        /** Call this when the dialog is dismissed to cleanup internal resources */
        fun dismiss() {
            quantity.removeObserver(quantityObserver)
            isDismissed = true
        }

        private fun updatePrice() {
            val fullPriceText = cartItem.fullPriceText
            if (fullPriceText != null) {
                price.postValue(cartItem.fullPriceText)
                priceContentDescription.postString(
                    R.string.Snabble_Shoppingcart_Accessibility_descriptionForPrice,
                    cartItem.fullPriceText
                )
                if (product.listPrice > product.getPrice(project.customerCardId)) {
                    val originalPriceText = SpannableString(priceFormatter.format(product.listPrice))
                    originalPriceText.setSpan(
                        StrikethroughSpan(),
                        0,
                        originalPriceText.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    originalPrice.postValue(originalPriceText)
                } else {
                    originalPrice.postValue(null)
                }
            }
            var cartItemDepositPrice = cartItem.totalDepositPrice
            if (cartItemDepositPrice == 0) {
                cartItemDepositPrice = product.depositProduct?.getPrice(project.customerCardId) ?: 0
            }
            if (cartItemDepositPrice > 0) {
                val price = priceFormatter.format(cartItemDepositPrice)
                val text = context.resources.getString(R.string.Snabble_Scanner_plusDeposit, price)
                depositPrice.postValue(text)
            } else {
                depositPrice.postValue(null)
            }
            val manualCoupons = project.coupons.filter(CouponType.MANUAL)
            when {
                manualCoupons.isEmpty() -> enterReducedPriceButtonText.postValue(null)
                cartItem.coupon != null -> enterReducedPriceButtonText.postValue(cartItem.coupon.name)
                else -> enterReducedPriceButtonText.postNullableString(R.string.Snabble_addDiscount)
            }
        }

        // TODO this has to be verified with all the cases
        private fun updateButtons() {
            //if (cartItem.isEditableInDialog) {
            //    quantity.isEnabled = true
            //    if (cartItem.product?.type == Product.Type.UserWeighed) {
            //        plusLayout.visibility = View.GONE
            //        minusLayout.visibility = View.GONE
            //    } else {
            //        plusLayout.visibility = View.VISIBLE
            //        minusLayout.visibility = View.VISIBLE
            //        quantity.visibility = View.VISIBLE
            //        quantityTextInput.visibility = View.VISIBLE
            //    }
            //} else {
            //    quantity.isEnabled = false
            //    plusLayout.visibility = View.GONE
            //    minusLayout.visibility = View.GONE
            //    quantity.visibility = View.GONE
            //    quantityTextInput.visibility = View.GONE
            //    quantityAnnotation.visibility = View.GONE
            //}

            if (cartItem.isEditableInDialog) {
                quantityCanBeChanged.postValue(true)
                quantityVisible.postValue(true)
                quantityButtonsVisible.postValue(cartItem.product?.type != Product.Type.UserWeighed)
            } else {
                quantityVisible.postValue(false)
                quantityCanBeChanged.postValue(false)
                quantityButtonsVisible.postValue(false)
            }
            quantityCanBeIncreased.postValue(true)
            quantityCanBeDecreased.postValue(quantity.value ?: 0 > 1)
            if (cartItem.product?.type != Product.Type.UserWeighed) {
                if (cartItem.product?.type == Product.Type.Article) {
                    // This should be impossible IMHO, currently that can only happen when the quantity of the view cannot be parsed AFIK
                    quantity.cycleFreeValue(quantity.value?.coerceAtLeast(0))
                }
            } else {
                //quantity.cycleFreeValue(null)
            }
            if (cartItem.unit == Unit.PRICE) {
                // FIXME the quantity is the price!?
                //quantity.setText(cartItem.priceText)
            }
        }

        fun addToCart() {
            if (isDismissed) return
            Telemetry.event(Telemetry.Event.ConfirmedProduct, product)
            val q = Math.max(quantity.value ?: 0, cartItem.scannedCode.embeddedData)
            if (product.type == Product.Type.UserWeighed && q == 0) {
                // FIXME: shake()
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
            // TODO check if this is not required anymore: dismiss(true)
            if (Snabble.config.vibrateToConfirmCartFilled &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                // noinspection MissingPermission, check is above
                vibrator.vibrate(200L)
            }
        }
    }

    // Extension functions for easier view model usage

    /**
     * Extension function to bind a LiveData String to the text of a TextView (or its derived
     * classes like Button)
     *
     * Might throw a IllegalArgumentException when the view is not attached.
     */
    fun TextView.bindText(source: LiveData<String>) {
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
            "LifecycleOwner not found, please make sure that this view is already attached"
        }
        source.observe(lifecycleOwner) {
            text = it
        }
    }

    /**
     * Extension function to bind a LiveData String to the text of a TextView (or its derived
     * classes like Button), when the String is null or empty the view will be hidden.
     *
     * Might throw a IllegalArgumentException when the view is not attached.
     */
    fun TextView.bindTextOrHide(source: LiveData<out CharSequence?>) {
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
            "LifecycleOwner not found, please make sure that this view is already attached"
        }
        source.observe(lifecycleOwner) {
            setTextOrHide(it)
        }
    }

    /**
     * Extension function to bind a LiveData String to the context description of a TextView (or
     * its derived classes like Button)
     *
     * Might throw a IllegalArgumentException when the view is not attached.
     */
    fun TextView.bindContentDescription(source: LiveData<String>) {
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
            "LifecycleOwner not found, please make sure that this view is already attached"
        }
        source.observe(lifecycleOwner) {
            contentDescription = it
        }
    }

    /**
     * Extension function to bind a LiveData Boolean to the visibility of any View.
     *
     * Might throw a IllegalArgumentException when the view is not attached.
     */
    fun View.bindVisibility(source: LiveData<Boolean>) {
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
            "LifecycleOwner not found, please make sure that this view is already attached"
        }
        source.observe(lifecycleOwner) {
            isVisible = it
        }
    }

    /**
     * Extension function to bind a LiveData Boolean to the enabled state of any View.
     *
     * Might throw a IllegalArgumentException when the view is not attached.
     */
    fun View.bindEnabledState(source: LiveData<Boolean>) {
        val lifecycleOwner = requireNotNull(findViewTreeLifecycleOwner()) {
            "LifecycleOwner not found, please make sure that this view is already attached"
        }
        source.observe(lifecycleOwner) {
            isEnabled = it
        }
    }

    /**
     * Show the dialog for the given project and the scanned code.
     */
    fun show(viewModel: ViewModel) {
        dismiss(false)
        val view = View.inflate(context, R.layout.snabble_dialog_product_confirmation, null)
        alertDialog = LifecycleAwareAlertDialogBuilder(context)
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

        viewModel.quantity.observe(requireNotNull(quantity.findViewTreeLifecycleOwner())) { count ->
            if (getQuantity() != count) {
                quantity.setText(count.toString())
            }
            quantity.announceForAccessibility(
                quantity.resources.getString(
                    R.string.Snabble_Scanner_Accessibility_eventQuantityUpdate,
                    count,
                    viewModel.cartItem.displayName,
                    viewModel.cartItem.totalPriceText
                )
            )
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
            val number = try {
                s.toString().toInt()
            } catch (e: NumberFormatException) {
                0 // FIXME bad default?
            }
            viewModel.quantity.postValue(number)
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
            viewModel.dismiss()
            dismiss(false)
        }
        val window = alertDialog?.window
        if (window == null) {
            viewModel.dismiss()
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