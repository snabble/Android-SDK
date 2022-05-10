package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import io.snabble.sdk.CouponType
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.SnabbleUI.executeAction
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.isNotNullOrBlank
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import kotlin.math.max

/**
 * The product confirmation dialog with the option to enter discounts and change the quantity.
 */
interface ProductConfirmationDialog {
    /**
     * Simple abstract factory to inject a custom ProductConfirmationDialog implementation
     */
    fun interface Factory {
        /** Create a new ProductConfirmationDialog instance */
        fun create(): ProductConfirmationDialog
    }

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
        /** The quantity of the product, can be null e.g. in case of user weighted products */
        val quantity = MutableLiveData<Int?>(null)
        /** The content description of the quantity, can be null */
        val quantityContentDescription = MutableLiveData<String?>()
        /** True when the quantity can be changed */
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
        /** True when the item was added to the cart */
        var wasAddedToCart: Boolean = false
            private set
        // Might be not really useful, written with compatibility in mind
        private var isDismissed = false
        // syncs the quantity property with the cart item and updates the addToCartButtonText
        private val quantityObserver = Observer<Int?> {
            try {
                it?.let {
                    cartItem.quantity = it
                }
                val existingQuantity =
                    shoppingCart.getExistingMergeableProduct(cartItem.product)?.effectiveQuantity
                        ?: 0
                val newQuantity = cartItem.effectiveQuantity
                if (quantity.value == null && cartItem.product?.type == Product.Type.Article) {
                    quantity.cycleFreeValue(existingQuantity + newQuantity)
                } else if (cartItem.product?.type == Product.Type.UserWeighed && quantity.value == 0) {
                    quantity.cycleFreeValue(null)
                }
                addToCartButtonText.postString(R.string.Snabble_Scanner_addToCart)
                quantityContentDescription.postNullableString(
                    R.string.Snabble_Scanner_Accessibility_eventQuantityUpdate,
                    quantity.value.toString(),
                    cartItem.displayName,
                    cartItem.totalPriceText
                )
                updatePrice()
                updateButtons()
            } catch (e: Exception) {
                Log.e("dddd", e.message!!)
            }
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

        fun updatePrice() {
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

        private fun updateButtons() {
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
        }

        fun addToCart() {
            if (isDismissed) return
            wasAddedToCart = true
            Telemetry.event(Telemetry.Event.ConfirmedProduct, product)
            val q = max(quantity.value ?: 0, cartItem.scannedCode.embeddedData)
            if (product.type == Product.Type.UserWeighed && q == 0) {
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

    /**
     * Show the product confirmation dialog with the given view model.
     */
    fun show(activity: FragmentActivity, viewModel: ViewModel)

    /**
     * Dismiss the product confirmation dialog
     */
    fun dismiss(addToCart: Boolean)

    /**
     * Set a dismiss listener
     * @see DialogInterface.OnDismissListener
     */
    fun setOnDismissListener(onDismissListener: OnDismissListener?)

    /**
     * Set a show listener
     * @see DialogInterface.OnShowListener
     */
    fun setOnShowListener(onShowListener: OnShowListener?)

    /**
     * Set a key listener
     * @see OnKeyListener
     */
    fun setOnKeyListener(onKeyListener: OnKeyListener?)

    /**
     * Interface used to allow the creator of a dialog to run some code when the
     * dialog is dismissed.
     */
    fun interface OnDismissListener {
        /**
         * This method will be invoked when the dialog is dismissed.
         */
        fun onDismiss()
    }

    /**
     * Interface used to allow the creator of a dialog to run some code when the
     * dialog is shown.
     */
    fun interface OnShowListener {
        /**
         * This method will be invoked when the dialog is shown.
         */
        fun onShow()
    }

    /**
     * Interface definition for a callback to be invoked when a hardware key event is
     * dispatched to this dialog. The callback will be invoked before the key event is
     * given to the view. This is only useful for hardware keyboards; a software input
     * method has no obligation to trigger this listener.
     */
    fun interface OnKeyListener {
        /**
         * Called when a hardware key is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * Key presses in software keyboards will generally NOT trigger this method,
         * although some may elect to do so in some situations. Do not assume a
         * software input method has to be key-based; even if it is, it may use key presses
         * in a different way than you expect, so there is no way to reliably catch soft
         * input key presses.
         *
         * @param keyCode The code for the physical key that was pressed.
         * @param event The KeyEvent object containing full information about the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        fun onKey(keyCode: Int, event: KeyEvent): Boolean
    }
}