package io.snabble.sdk.ui.scanner

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.view.KeyEvent
import io.snabble.sdk.*
import io.snabble.sdk.Unit
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.codes.gs1.GS1Code
import io.snabble.sdk.ui.R

import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.DelayedProgressDialog
import io.snabble.sdk.utils.Age
import io.snabble.sdk.utils.Dispatch

class ProductResolver private constructor(private val context: Context, private val project: Project) {
    private var resolveBundles = true
    private val productConfirmationDialog = ProductConfirmationDialog(context, project)
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
    private var onKeyListener: DialogInterface.OnKeyListener? = null
    private var lastProduct: Product? = null

    private fun checkMinAge(product: Product) {
        if (product.saleRestriction.isAgeRestriction) {
            val minAge = product.saleRestriction.value
            val birthday = Snabble.getInstance().userPreferences.birthday
            var isOldEnough = false
            if (birthday != null) {
                val age = Age.calculateAge(Snabble.getInstance().userPreferences.birthday)
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
        productConfirmationDialog.dismiss(false)
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
                        newGs1Code = GS1Code(scannedCode.code)
                        val code = project.getCodeTemplate("default")
                                .match(newGs1Code.gtin)
                                .buildCode()
                        if (code != null) {
                            gs1GtinScannedCodes.add(code.newBuilder()
                                    .setScannedCode(newGs1Code.code)
                                    .create())
                            break
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
            gs1EmbeddedData = gs1Code.getEmbeddedData(unit,
                    project.currency.defaultFractionDigits,
                    project.roundingMode)
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
            resolveBundles && product.bundleProducts.isNotEmpty() && !scannedCode.hasEmbeddedData() -> {
                showBundleDialog(product, scannedCode)
            }
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
        productConfirmationDialog.show(product, scannedCode)
    }

    @Deprecated("Use resolve() instead")
    fun show() = resolve()

    fun resolve() {
        lookupProductData(scannedCodes, null)
    }

    fun dismiss() {
        productConfirmationDialog.dismiss(false)
    }

    fun addResolvedItemToCart() {
        productConfirmationDialog.addToCart()
    }

    interface OnShowListener {
        fun onShow()
    }

    interface OnDismissListener {
        fun onDismiss()
    }

    interface OnSaleStopListener {
        fun onSaleStop()
    }

    interface OnNotForSaleListener {
        fun onNotForSale(product: Product)
    }

    interface OnShelfCodeScannedListener {
        fun onShelfCodeScanned()
    }

    interface OnProductNotFoundListener {
        fun onProductNotFound()
    }

    interface OnProductFoundListener {
        fun onProductFound(product: Product, scannedCode: ScannedCode)
    }

    interface OnNetworkErrorListener {
        fun onNetworkError()
    }

    interface OnAgeNotReachedListener {
        fun onAgeNotReached()
    }

    interface OnAlreadyScannedListener {
        fun onAlreadyScanned()
    }

    class Builder @JvmOverloads constructor(context: Context, private val project: Project = SnabbleUI.project) {
        private val productResolver: ProductResolver = ProductResolver(context, project)

        fun setCodes(codes: List<ScannedCode>) = apply {
            productResolver.scannedCodes = codes
        }

        fun setCode(code: ScannedCode) = apply {
            productResolver.scannedCodes = listOf(code)
        }

        fun setBarcode(barcode: Barcode) = apply {
            setCodes(ScannedCode.parse(project, barcode.text))
        }

        fun setBarcodeFormat(barcodeFormat: BarcodeFormat?) = apply {
            productResolver.barcodeFormat = barcodeFormat
        }

        fun setOnShowListener(listener: OnShowListener) = apply {
            productResolver.onShowListener = listener
        }

        fun setOnKeyListener(listener: DialogInterface.OnKeyListener) = apply {
            productResolver.onKeyListener = listener
        }

        fun setOnDismissListener(listener: OnDismissListener) = apply {
            productResolver.onDismissListener = listener
        }

        fun setOnProductNotFoundListener(listener: OnProductNotFoundListener) = apply {
            productResolver.onProductNotFoundListener = listener
        }

        fun setOnProductNotFoundListener(listener: () -> kotlin.Unit) =
                setOnProductNotFoundListener(object : OnProductNotFoundListener {
                    override fun onProductNotFound() = listener.invoke()
                })

        fun setOnProductFoundListener(listener: OnProductFoundListener) = apply {
            productResolver.onProductFoundListener = listener
        }

        fun setOnProductFoundListener(listener: (product: Product, scannedCode: ScannedCode) -> kotlin.Unit) =
                setOnProductFoundListener(object : OnProductFoundListener {
                    override fun onProductFound(product: Product, scannedCode: ScannedCode) {
                        listener.invoke(product, scannedCode)
                    }
                })

        fun setOnNetworkErrorListener(listener: OnNetworkErrorListener) = apply {
            productResolver.onNetworkErrorListener = listener
        }

        fun setOnNetworkErrorListener(listener: () -> kotlin.Unit) =
                setOnNetworkErrorListener(object : OnNetworkErrorListener {
                    override fun onNetworkError() = listener.invoke()
                })

        fun setOnSaleStopListener(listener: OnSaleStopListener) = apply {
            productResolver.onSaleStopListener = listener
        }

        fun setOnNotForSaleListener(listener: OnNotForSaleListener) = apply {
            productResolver.onNotForSaleListener = listener
        }

        fun setOnAgeNotReachedListener(listener: OnAgeNotReachedListener) = apply {
            productResolver.onAgeNotReachedListener = listener
        }

        fun setOnAlreadyScannedListener(listener: OnAlreadyScannedListener) = apply {
            productResolver.onAlreadyScannedListener = listener
        }

        fun setOnShelfCodeScannedListener(listener: OnShelfCodeScannedListener) = apply {
            productResolver.onShelfCodeScannedListener = listener
        }

        fun disableBundleSelection() = apply {
            productResolver.resolveBundles = false
        }

        fun create() = productResolver
    }

    init {
        productConfirmationDialog.setOnDismissListener {
            if (lastProduct != null && productConfirmationDialog.wasAddedToCart()) {
                checkMinAge(lastProduct!!)
            }
            onDismissListener?.onDismiss()
        }
        productConfirmationDialog.setOnKeyListener { dialog: DialogInterface?, keyCode: Int, event: KeyEvent? ->
            onKeyListener?.onKey(dialog, keyCode, event) ?: false
        }
        progressDialog = DelayedProgressDialog(context)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
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