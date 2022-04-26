package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Context
import io.snabble.sdk.ui.SnabbleUI.executeAction
import android.content.DialogInterface
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.SnabbleUI
import android.text.style.StrikethroughSpan
import com.squareup.picasso.Picasso
import android.os.Bundle
import io.snabble.sdk.utils.GsonHolder
import android.content.pm.PackageManager
import android.os.Vibrator
import android.text.*
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.snabble.sdk.*
import io.snabble.sdk.ui.utils.*

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
            it?.let {
                cartItem.quantity = it
            }
            val existingItem = shoppingCart.getExistingMergeableProduct(cartItem.product)
            val isMergeable = existingItem != null && existingItem.isMergeable && cartItem.isMergeable
            if (isMergeable) {
                quantity.cycleFreeValue(existingItem.effectiveQuantity + 1)
                addToCartButtonText.postString(R.string.Snabble_Scanner_updateCart)
            } else {
                val effectiveQuantity = cartItem.effectiveQuantity
                if (quantity.value != null && effectiveQuantity != 0) {
                    quantity.cycleFreeValue(cartItem.effectiveQuantity)
                }
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
            if (cartItem.product?.type == Product.Type.Article) {
                quantity.cycleFreeValue(quantity.value?.coerceAtLeast(0))
            } else if (cartItem.product?.type == Product.Type.UserWeighed && quantity.value == 0) {
                quantity.cycleFreeValue(null)
            }
        }

        fun addToCart() {
            if (isDismissed) return
            wasAddedToCart = true
            Telemetry.event(Telemetry.Event.ConfirmedProduct, product)
            val q = Math.max(quantity.value ?: 0, cartItem.scannedCode.embeddedData)
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
    fun show(viewModel: ViewModel)

    /**
     * Dismiss the product confirmation dialog
     */
    fun dismiss(addToCart: Boolean)

    /**
     * Set a dismiss listener
     * @see DialogInterface.OnDismissListener
     */
    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?)

    /**
     * Set a show listener
     * @see DialogInterface.OnShowListener
     */
    fun setOnShowListener(onShowListener: DialogInterface.OnShowListener?)

    /**
     * Set a key listener
     * @see DialogInterface.OnKeyListener
     */
    fun setOnKeyListener(onKeyListener: DialogInterface.OnKeyListener?)
}