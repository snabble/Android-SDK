package io.snabble.sdk.ui.scanner

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.view.KeyEvent
import io.snabble.sdk.*
import io.snabble.sdk.Unit
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.codes.gs1.GS1Code
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.scanner.ProductResolver.*
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.DelayedProgressDialog
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Age
import io.snabble.sdk.utils.Dispatch
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.ReplaceWith
import kotlin.apply

/**
 * The product resolver parses Barcodes e.g. GTINs and provides all meta data about the scanned
 * product like age restrictions, sale stop and embedded units. You can also observe errors like
 * network errors or product not found errors.
 *
 * You can create a new instance by using the Builder.
 *
 * Here is a basic example how to show just the ProductConfirmationDialog:
 * <pre>
 * Builder(context)
 *     .setCodes(ScannedCode.parse(SnabbleUI.project, code))
 *     .create()
 *     .resolve()
 * </pre>
 *
 * You can find an advanced sample in the SelfScanningView.lookupAndShowProduct(...)
 */
class ProductResolver private constructor(private val context: Context, private val project: Project) {
    private var resolveBundles = true
    private var productConfirmationDialog: ProductConfirmationDialog? = null
    private var productDialogViewModel: ProductConfirmationDialog.ViewModel? = null
    var scannedCodes: List<ScannedCode> = emptyList()
        private set
    private val progressDialog: DelayedProgressDialog
    var onShowListener: OnShowListener? = null
    var onDismissListener: OnDismissListener? = null
    private var onSaleStopListener: OnSaleStopListener? = null
    private var onNotForSaleListener: OnNotForSaleListener? = null
    private var onShelfCodeScannedListener: OnShelfCodeScannedListener? = null
    private var onProductNotFoundListener: OnProductNotFoundListener? = null
    private var onProductFoundListener: OnProductFoundListener? = null
    private var onAgeNotReachedListener: OnAgeNotReachedListener? = null
    private var onNetworkErrorListener: OnNetworkErrorListener? = null
    private var onAlreadyScannedListener: OnAlreadyScannedListener? = null
    var barcodeFormat: BarcodeFormat? = null
        private set
    private var onKeyListener: ProductConfirmationDialog.OnKeyListener? = null
    private var lastProduct: Product? = null

    private fun checkMinAge(product: Product) {
        if (product.saleRestriction.isAgeRestriction) {
            val minAge = product.saleRestriction.value
            val birthday = Snabble.userPreferences.birthday
            var isOldEnough = false
            if (birthday != null) {
                val age = Age.calculateAge(Snabble.userPreferences.birthday)
                if (age.years >= minAge) {
                    isOldEnough = true
                }
            }
            if (!isOldEnough) {
                onAgeNotReachedListener?.onAgeNotReached()

            }
        }
    }

    private data class Result(
        var product: Product? = null,
        var wasOnlineProduct: Boolean = false,
        var code: ScannedCode? = null,
        var error: Boolean = false,
        var matchCount: Int = 0
    )

    private fun lookupProductData(scannedCodes: List<ScannedCode>, gs1Code: GS1Code?) {
        productConfirmationDialog?.dismiss(false)
        progressDialog.showAfterDelay(300)
        onShowListener?.onShow()

        val productDatabase = project.productDatabase

        // check if its available local first
        if (productDatabase.isUpToDate) {
            for (scannedCode in scannedCodes) {
                val product = productDatabase.findByCode(scannedCode)
                if (product != null) {
                    handleProductAvailable(product, false, scannedCode, gs1Code)
                    return
                }
            }
        }

        // create multiple online requests asynchronously and wait for all to succeed or fail
        Dispatch.background {
            val countDownLatch = CountDownLatch(scannedCodes.size)
            val result = Result()
            for (i in scannedCodes.indices) {
                val scannedCode = scannedCodes[i]
                productDatabase.findByCodeOnline(scannedCode, object : OnProductAvailableListener {
                    override fun onProductAvailable(product: Product, wasOnlineProduct: Boolean) {
                        result.product = product
                        result.wasOnlineProduct = wasOnlineProduct
                        result.code = scannedCode
                        result.matchCount++
                        countDownLatch.countDown()
                    }

                    override fun onProductNotFound() {
                        if (result.code == null) {
                            result.code = scannedCode
                        }
                        countDownLatch.countDown()
                    }

                    override fun onError() {
                        result.error = true
                        countDownLatch.countDown()
                    }
                }, true)
            }
            try {
                countDownLatch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            Dispatch.mainThread {
                if (result.matchCount > 1) {
                    project.logErrorEvent("Multiple code matches for product " + result.product?.sku)
                }
                if (result.product != null) {
                    handleProductAvailable(result.product!!, result.wasOnlineProduct, result.code!!, gs1Code)
                } else if (result.error) {
                    handleProductError()
                } else {
                    val gs1GtinScannedCodes = mutableListOf<ScannedCode>()
                    var newGs1Code: GS1Code? = null
                    for (scannedCode in scannedCodes) {
                        val codeContents = scannedCode.code
                        if (codeContents != null) {
                            newGs1Code = GS1Code(codeContents)
                            val code = project.getCodeTemplate("default")
                                ?.match(newGs1Code.gtin)
                                ?.buildCode()
                            if (code != null) {
                                gs1GtinScannedCodes.add(
                                    code.newBuilder()
                                        .setScannedCode(newGs1Code.code)
                                        .create()
                                )
                                break
                            }
                        }
                    }
                    if (gs1GtinScannedCodes.isNotEmpty() && gs1Code == null) {
                        lookupProductData(gs1GtinScannedCodes, newGs1Code)
                    } else {
                        project.productDatabase.findBySkuOnline(scannedCodes[0].lookupCode, object : OnProductAvailableListener {
                            override fun onProductAvailable(product: Product, wasOnline: Boolean) {
                                handleProductAvailable(product, wasOnline, scannedCodes[0], null)
                            }

                            override fun onProductNotFound() {
                                project.events.productNotFound(scannedCodes)
                                handleProductNotFound(scannedCodes[0])
                            }

                            override fun onError() {
                                handleProductError()
                            }
                        })
                    }
                }
            }
        }
    }

    private fun handleProductAvailable(product: Product, wasOnlineProduct: Boolean, scannedCode: ScannedCode, gs1Code: GS1Code?) {
        progressDialog.dismiss()
        val unit = product.getEncodingUnit(scannedCode.templateName, scannedCode.lookupCode)
        var gs1EmbeddedData: BigDecimal? = null

        if (gs1Code != null) {
            gs1EmbeddedData = gs1Code.getEmbeddedData(
                unit,
                project.currency.defaultFractionDigits,
                project.roundingMode
            )
        }

        if (scannedCode.hasEmbeddedDecimalData() || gs1EmbeddedData != null) {
            if (unit != null) {
                val decimal = gs1EmbeddedData ?: scannedCode.embeddedDecimalData
                if (unit == Unit.PIECE || unit == Unit.PRICE) {
                    scannedCode.embeddedData = decimal.scaleByPowerOfTen(decimal.scale()).toInt()
                    scannedCode.embeddedUnit = unit
                } else {
                    val converted = Unit.convert(decimal, unit, unit.smallestUnit)
                    scannedCode.embeddedData = converted.toInt()
                    scannedCode.embeddedUnit = unit.smallestUnit
                }
            }
        } else if (scannedCode.embeddedUnit == null) {
            scannedCode.embeddedUnit = product.getEncodingUnit(scannedCode.templateName, scannedCode.lookupCode)
        }

        when {
            resolveBundles && !product.bundleProducts.isNullOrEmpty() && !scannedCode.hasEmbeddedData() -> {
                showBundleDialog(product, scannedCode)
            }
            else -> {
                showProduct(product, scannedCode)
                val event = if (wasOnlineProduct) {
                    Telemetry.Event.ScannedOnlineProduct
                } else {
                    Telemetry.Event.ScannedProduct
                }
                Telemetry.event(event, product)
            }
        }
    }

    private fun showBundleDialog(product: Product, scannedCode: ScannedCode) {
        if (handleProductFlags(product, scannedCode)) {
            SelectBundleDialog.show(context, product, object : SelectBundleDialog.Callback {
                override fun onProductSelected(product: Product) {
                    Telemetry.event(Telemetry.Event.SelectedBundleProduct, product)
                    val codes = product.scannableCodes
                    if (codes.isNotEmpty() && codes[0].lookupCode != null) {
                        val newCodes = ScannedCode.parse(project, codes[0].lookupCode)
                        if (newCodes.size > 0) {
                            var defaultCode = newCodes[0]
                            for (newCode in newCodes) {
                                if (newCode.templateName == "default") {
                                    defaultCode = newCode
                                }
                            }
                            showProduct(product, defaultCode)
                        } else {
                            showProduct(product, scannedCode)
                        }
                    }
                }

                override fun onDismissed() {
                    onDismissListener?.onDismiss()
                }
            })
        }
    }

    private fun handleProductNotFound(scannedCode: ScannedCode) {
        progressDialog.dismiss()
        Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.code)
        onDismissListener?.onDismiss()
        onProductNotFoundListener?.onProductNotFound()
    }

    private fun handleProductError() {
        progressDialog.dismiss()
        onDismissListener?.onDismiss()
        onNetworkErrorListener?.onNetworkError()
    }

    private fun showProduct(product: Product?, scannedCode: ScannedCode?) {
        lastProduct = product

        if (product != null && scannedCode != null) {
            if (handleProductFlags(product, scannedCode)) {
                val model = ProductConfirmationDialog.ViewModel(context, SnabbleUI.project, product, scannedCode)
                productDialogViewModel = model
                productConfirmationDialog?.show(UIUtils.getHostFragmentActivity(context), model)
            }
        }
    }

    private fun handleProductFlags(product: Product, scannedCode: ScannedCode) : Boolean {
        when {
            product.saleStop -> {
                onSaleStopListener?.onSaleStop()
                progressDialog.dismiss()
                onDismissListener?.onDismiss()
            }
            product.notForSale -> {
                onNotForSaleListener?.onNotForSale(product)
                progressDialog.dismiss()
                onDismissListener?.onDismiss()
            }
            product.availability == Product.Availability.NOT_AVAILABLE -> {
                handleProductNotFound(scannedCode)
            }
            product.type == Product.Type.PreWeighed
                    && (!scannedCode.hasEmbeddedData() || scannedCode.embeddedData == 0) -> {
                onShelfCodeScannedListener?.onShelfCodeScanned()
                progressDialog.dismiss()
                onDismissListener?.onDismiss()
            }
            product.type == Product.Type.DepositReturnVoucher
                    && project.shoppingCart.containsScannedCode(scannedCode) -> {
                onAlreadyScannedListener?.onAlreadyScanned()
                progressDialog.dismiss()
                onDismissListener?.onDismiss()
            }
            onProductFoundListener != null -> {
                onProductFoundListener?.onProductFound(product, scannedCode)
            }
            else -> return true
        }

        return false
    }

    // @hide
    @Deprecated("Use resolve() instead", ReplaceWith("resolve()"))
    fun show() = resolve()

    /**
     * Resolve the product
     */
    fun resolve() {
        lookupProductData(scannedCodes, null)
    }

    /**
     * Dismiss the product confirmation dialog.
     */
    fun dismiss() {
        productConfirmationDialog?.dismiss(false)
    }

    /**
     * Add the resolved product to the cart.
     */
    fun addResolvedItemToCart() {
        // TODO check the bang operator
        productDialogViewModel!!.addToCart()
    }

    /**
     * Listener for showing the product confirmation dialog.
     * We use this internal to stop the camera preview.
     */
    fun interface OnShowListener {
        /**
         * Dialog is shown.
         */
        fun onShow()
    }

    /**
     * Listener for dismissing the product confirmation dialog.
     * We use this internal to continue the camera preview.
     */
    fun interface OnDismissListener {
        /**
         * Dialog was dismissed.
         */
        fun onDismiss()
    }

    /**
     * Sale stop listener.
     */
    fun interface OnSaleStopListener {
        /**
         * Will be invoked when the product has the sale stop flag set.
         */
        fun onSaleStop()
    }

    /**
     * Not for sale listener.
     */
    fun interface OnNotForSaleListener {
        /**
         * Will be invoked when the product has the not for sale flag set.
         */
        fun onNotForSale(product: Product)
    }

    /**
     * The shelf code scanned listener.
     */
    fun interface OnShelfCodeScannedListener {
        /**
         * TODO
         */
        fun onShelfCodeScanned()
    }

    /**
     * The product not found listener.
     */
    fun interface OnProductNotFoundListener {
        /**
         * Will be invoked when the product was not found.
         */
        fun onProductNotFound()
    }

    /**
     * The product found listener.
     */
    fun interface OnProductFoundListener {
        /**
         * Provides the found product with its resolved scanned code.
         */
        fun onProductFound(product: Product, scannedCode: ScannedCode)
    }

    /**
     * The network error listener.
     */
    fun interface OnNetworkErrorListener {
        /**
         * Will be invoked when a network error accrued.
         */
        fun onNetworkError()
    }

    /**
     * The age not reached listener.
     */
    fun interface OnAgeNotReachedListener {
        /**
         * TODO
         */
        fun onAgeNotReached()
    }

    /**
     * The already scanned listener.
     */
    fun interface OnAlreadyScannedListener {
        /**
         * Will be invoked when the product was already scanned.
         */
        fun onAlreadyScanned()
    }

    /**
     * The builder class to create a new instance of the product resolver.
     * @param context The context for the product resolver
     * @param project The optional project of the product resolver, by default the current project will be used
     */
    class Builder @JvmOverloads constructor(
        context: Context,
        private val project: Project = requireNotNull(Snabble.checkedInProject.value)
    ) {
        private val productResolver = ProductResolver(context, project)
        private var factory = ProductConfirmationDialog.Factory {
            DefaultProductConfirmationDialog()
        }

        /**
         * Set the scanned codes to analyse.
         */
        fun setCodes(codes: List<ScannedCode>) = apply {
            productResolver.scannedCodes = codes
        }

        /**
         * Set the scanned code to analyse.
         */
        fun setCode(code: ScannedCode) = apply {
            productResolver.scannedCodes = listOf(code)
        }

        /**
         * Set the barcode to analyse.
         */
        fun setBarcode(barcode: Barcode) = apply {
            setCodes(ScannedCode.parse(project, barcode.text))
        }

        /**
         * Set the barcode format.
         */
        fun setBarcodeFormat(barcodeFormat: BarcodeFormat?) = apply {
            productResolver.barcodeFormat = barcodeFormat
        }

        /**
         * Set a listener which is invoked when the product confirmation dialog is shown.
         */
        fun setOnShowListener(listener: OnShowListener) = apply {
            productResolver.onShowListener = listener
        }

        /**
         * Set a key listener.
         * @see DialogInterface.OnKeyListener
         */
        fun setOnKeyListener(listener: ProductConfirmationDialog.OnKeyListener) = apply {
            productResolver.onKeyListener = listener
        }

        /**
         * Set a listener which is invoked when the product confirmation dialog is dismissed.
         */
        fun setOnDismissListener(listener: OnDismissListener) = apply {
            productResolver.onDismissListener = listener
        }

        /**
         * Set a product not found listener.
         */
        fun setOnProductNotFoundListener(listener: OnProductNotFoundListener) = apply {
            productResolver.onProductNotFoundListener = listener
        }

        /**
         * Set a product not found listener.
         */
        fun setOnProductNotFoundListener(listener: () -> kotlin.Unit) =
            setOnProductNotFoundListener(OnProductNotFoundListener { listener.invoke() })

        /**
         * Set a product found listener.
         */
        fun setOnProductFoundListener(listener: OnProductFoundListener) = apply {
            productResolver.onProductFoundListener = listener
        }

        /**
         * Set a network error listener.
         */
        fun setOnNetworkErrorListener(listener: OnNetworkErrorListener) = apply {
            productResolver.onNetworkErrorListener = listener
        }

        /**
         * Set a SaleStop listener.
         */
        fun setOnSaleStopListener(listener: OnSaleStopListener) = apply {
            productResolver.onSaleStopListener = listener
        }

        /**
         * Set a NotForSale listener.
         */
        fun setOnNotForSaleListener(listener: OnNotForSaleListener) = apply {
            productResolver.onNotForSaleListener = listener
        }

        /**
         * Set an age not reached listener.
         */
        fun setOnAgeNotReachedListener(listener: OnAgeNotReachedListener) = apply {
            productResolver.onAgeNotReachedListener = listener
        }

        /**
         * Set an already scanned listener.
         */
        fun setOnAlreadyScannedListener(listener: OnAlreadyScannedListener) = apply {
            productResolver.onAlreadyScannedListener = listener
        }

        /**
         * Set a shelf code scanned listener.
         */
        fun setOnShelfCodeScannedListener(listener: OnShelfCodeScannedListener) = apply {
            productResolver.onShelfCodeScannedListener = listener
        }

        /**
         * Disable bundle selection.
         */
        fun disableBundleSelection() = apply {
            productResolver.resolveBundles = false
        }

        /**
         * Set a factory for a custom DialogConfirmationDialog implementation.
         */
        fun setDialogConfirmationDialogFactory(factory: ProductConfirmationDialog.Factory?) = apply {
            this.factory = factory ?: ProductConfirmationDialog.Factory {
                DefaultProductConfirmationDialog()
            }
        }

        /**
         * Create the product resolver.
         */
        fun create() = productResolver.apply {
            productConfirmationDialog = requireNotNull(factory.create())
            productConfirmationDialog?.setOnDismissListener {
                if (lastProduct != null && productDialogViewModel?.wasAddedToCart == true) {
                    checkMinAge(lastProduct!!)
                }
                onDismissListener?.onDismiss()
            }
            productConfirmationDialog?.setOnKeyListener { keyCode, event ->
                onKeyListener?.onKey(keyCode, event) ?: false
            }
        }
    }

    init {
        progressDialog = DelayedProgressDialog(context)
        progressDialog.setMessage(context.getString(R.string.Snabble_loadingProductInformation))
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                progressDialog.dismiss()
                return@setOnKeyListener true
            }
            false
        }
    }
}
