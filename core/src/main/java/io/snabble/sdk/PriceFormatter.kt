package io.snabble.sdk

import android.util.LruCache
import kotlin.jvm.JvmOverloads
import io.snabble.sdk.codes.ScannedCode
import java.math.BigDecimal
import java.text.NumberFormat

class PriceFormatter(private val project: Project) {
    private val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(project.currencyLocale)
    private val cache: LruCache<Int, String> = LruCache(100)

    init {
        numberFormat.currency = project.currency
        val fractionDigits = project.getCurrencyFractionDigits()
        numberFormat.minimumFractionDigits = fractionDigits
        numberFormat.maximumFractionDigits = fractionDigits
    }

    @JvmOverloads
    fun format(price: Int, allowZeroPrice: Boolean = true): String {
        if (price == 0 && !allowZeroPrice) {
            return ""
        }

        val cachedValue = cache[price]
        if (cachedValue != null) {
            return cachedValue
        }

        val fractionDigits = project.getCurrencyFractionDigits()
        val bigDecimal = BigDecimal(price)
        val divider = BigDecimal(10).pow(fractionDigits)
        val dividedPrice = bigDecimal.divide(divider, fractionDigits, project.roundingMode)
        val formattedPrice = numberFormat.format(dividedPrice)

        // Android 4.x and 6 (but not 5 and 7+) are shipping with ICU versions
        // that have the currency symbol set to HUF instead of Ft for Locale hu_HU
        //
        // including the whole ICU library as a dependency increased APK size by 10MB
        // so we are overriding the result here instead for consistency
        if (project.currency.currencyCode == "HUF") {
            return formattedPrice.replace("HUF", "Ft")
        }

        cache.put(price, formattedPrice)
        return formattedPrice
    }

    fun format(product: Product, price: Int): String {
        var formattedString = format(price, false)
        val type = product.type
        var referenceUnit = product.referenceUnit
        if (referenceUnit == null) {
            referenceUnit = Unit.KILOGRAM
        }
        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            formattedString += " / " + referenceUnit.displayValue
        }
        return formattedString
    }

    @JvmOverloads
    fun format(product: Product, discountedPrice: Boolean = true, scannedCode: ScannedCode? = null): String {
        var price = product.listPrice
        if (discountedPrice) {
            price = product.getPrice(project.customerCardId)
        }
        if (scannedCode != null && scannedCode.hasPrice()) {
            price = scannedCode.price
        }
        return format(product, price)
    }


}