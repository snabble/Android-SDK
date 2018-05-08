package io.snabble.sdk.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;

import io.snabble.sdk.Product;
import io.snabble.sdk.SnabbleSdk;

public class PriceFormatter {
    private SnabbleSdk sdkInstance;

    public PriceFormatter(SnabbleSdk sdkInstance) {
        this.sdkInstance = sdkInstance;
    }

    public String format(int price) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(sdkInstance.getCurrencyLocale());
        Currency currency = sdkInstance.getCurrency();
        numberFormat.setCurrency(currency);

        int fractionDigits = sdkInstance.getCurrencyFractionDigits();
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);

        BigDecimal bigDecimal = new BigDecimal(price);
        BigDecimal divider = new BigDecimal(10).pow(fractionDigits);
        BigDecimal dividedPrice = bigDecimal.divide(divider, fractionDigits, sdkInstance.getRoundingMode());

        return numberFormat.format(dividedPrice.doubleValue());
    }

    public String format(Product product) {
        return format(product, true);
    }

    public String format(Product product, boolean discountedPrice) {
        int price = product.getPrice();

        if (discountedPrice) {
            price = product.getDiscountedPrice();
        }

        String formattedString = format(price);
        Product.Type type = product.getType();

        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            formattedString += " / kg";
        }

        return formattedString;
    }
}
