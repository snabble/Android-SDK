package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.os.Vibrator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.KeyEvent
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import io.snabble.sdk.*
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.isNotNullOrBlank
import io.snabble.sdk.utils.GsonHolder
import kotlinx.parcelize.Parcelize
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
        private val scannedCode: ScannedCode,
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
        /** A applied coupon like a manual discount, can be null */
        val appliedCoupon = MutableLiveData<Coupon?>(null)
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
        }

        init {
            quantity.observeForever(quantityObserver)
            updatePrice()
            updateButtons()
            appliedCoupon.observeForever {
                cartItem.coupon = it
                updatePrice()
            }
        }

        /**
         * Restore the ViewModel from a context and the savedInstanceState.
         * Can throw IllegalArgumentException when the project cannot be found or onSaveInstanceState was not called.
         */
        constructor(context: Context, savedInstanceState: Bundle): this(
            context,
            requireNotNull(savedInstanceState.getParcelable<Restorer>("model")) { "Cannot restore state" }
        )

        // Helper constructor to take the data from a restorer.
        private constructor(context: Context, restorer: Restorer):
                this(context, restorer.project, restorer.product, restorer.scannedCode) {
            quantity.postValue(restorer.quantity)
            quantityContentDescription.postValue(restorer.quantityContentDescription)
            quantityCanBeChanged.postValue(restorer.quantityCanBeChanged)
            restorer.addToCartButtonText?.let { addToCartButtonText.postValue(it) }
            restorer.price?.let { price.postValue(it) }
            restorer.priceContentDescription?.let { priceContentDescription.postValue(it) }
            originalPrice.postValue(restorer.originalPrice)
            depositPrice.postValue(restorer.depositPrice)
            enterReducedPriceButtonText.postValue(restorer.enterReducedPriceButtonText)
            appliedCoupon.postValue(restorer.appliedCoupon)
            quantityCanBeIncreased.postValue(restorer.quantityCanBeIncreased)
            quantityCanBeDecreased.postValue(restorer.quantityCanBeDecreased)
            quantityVisible.postValue(restorer.quantityVisible)
            quantityButtonsVisible.postValue(restorer.quantityButtonsVisible)
            wasAddedToCart = restorer.wasAddedToCart
            isDismissed = restorer.isDismissed
        }

        @Parcelize
        private data class Restorer(
            val projectId: String,
            val product: Product,
            val scannedCode: ScannedCode,
            val quantity: Int?,
            val quantityContentDescription: String?,
            val quantityCanBeChanged: Boolean,
            val addToCartButtonText: String?,
            val price: String?,
            val priceContentDescription: String?,
            val originalPrice: CharSequence?,
            val depositPrice: String?,
            val enterReducedPriceButtonText: String?,
            val appliedCoupon: Coupon?,
            val quantityCanBeIncreased: Boolean,
            val quantityCanBeDecreased: Boolean,
            val quantityVisible: Boolean,
            val quantityButtonsVisible: Boolean,
            val wasAddedToCart: Boolean,
            val isDismissed: Boolean
        ) : Parcelable {
            val project: Project
                get() = Snabble.getProjectById(projectId) ?: throw IllegalStateException("Project not found")
       }

        /**
         * Called to ask the view model to save its current dynamic state, so it
         * can later be reconstructed in a new instance if its process is
         * restarted.
         *
         * <p>This corresponds to {@link Activity#onSaveInstanceState(Bundle)
         * Activity.onSaveInstanceState(Bundle)} and most of the discussion there
         * applies here as well.  Note however: you must call this method, there is
         * no automatic to save the state.</p>
         *
         * @param outState Bundle in which to place your saved state.
         */
        @MainThread
        fun onSaveInstanceState(outState: Bundle) {
            // Note: Those fields with a bang operator are initialized with a value and never can be null
            outState.putParcelable("model", Restorer(
                projectId = project.id,
                product = product,
                scannedCode = scannedCode,
                quantity = quantity.value,
                quantityContentDescription = quantityContentDescription.value,
                quantityCanBeChanged = quantityCanBeChanged.value!!,
                addToCartButtonText = addToCartButtonText.value,
                price = price.value,
                priceContentDescription = priceContentDescription.value,
                originalPrice = originalPrice.value,
                depositPrice = depositPrice.value,
                enterReducedPriceButtonText = enterReducedPriceButtonText.value,
                appliedCoupon = appliedCoupon.value,
                quantityCanBeIncreased = quantityCanBeIncreased.value!!,
                quantityCanBeDecreased = quantityCanBeDecreased.value!!,
                quantityVisible = quantityVisible.value!!,
                quantityButtonsVisible = quantityButtonsVisible.value!!,
                wasAddedToCart = wasAddedToCart,
                isDismissed = isDismissed
            ))
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
        }

        fun addToCart() {
            if (isDismissed) return
            wasAddedToCart = true
            Telemetry.event(Telemetry.Event.ConfirmedProduct, product)
            val q = max(quantity.value ?: 0, cartItem.scannedCode?.embeddedData ?: 0)
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