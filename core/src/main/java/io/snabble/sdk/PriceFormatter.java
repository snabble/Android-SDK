package io.snabble.sdk;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;

import io.snabble.sdk.codes.ScannedCode;

public class PriceFormatter {
    private Project project;

    public PriceFormatter(Project project) {
        this.project = project;
    }

    public String format(int price) {
        if (price == 0) {
            return "";
        }

        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(project.getCurrencyLocale());
        Currency currency = project.getCurrency();
        numberFormat.setCurrency(currency);

        int fractionDigits = project.getCurrencyFractionDigits();
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);

        BigDecimal bigDecimal = new BigDecimal(price);
        BigDecimal divider = new BigDecimal(10).pow(fractionDigits);
        BigDecimal dividedPrice = bigDecimal.divide(divider, fractionDigits, project.getRoundingMode());

        String formattedPrice = numberFormat.format(dividedPrice.doubleValue());

        // Android 4.x and 6 (but not 5 and 7+) are shipping with ICU versions
        // that have the currency symbol set to HUF instead of Ft for Locale hu_HU
        //
        // including the whole ICU library as a dependency increased APK size by 10MB
        // so we are overriding the result here instead for consistency
        if (currency.getCurrencyCode().equals("HUF")) {
            return formattedPrice.replace("HUF", "Ft");
        }

        return formattedPrice;
    }

    public String format(Product product) {
        return format(product, true);
    }

    public String format(Product product, boolean discountedPrice) {
        return format(product, discountedPrice, null);
    }

    public String format(Product product, boolean discountedPrice, ScannedCode scannedCode) {
        int price = product.getPrice();

        if (discountedPrice) {
            price = product.getDiscountedPrice();
        }

        if (scannedCode != null && scannedCode.hasPrice()) {
            price = scannedCode.getPrice();
        }

        String formattedString = format(price);
        Product.Type type = product.getType();

        Unit referenceUnit = product.getReferenceUnit();
        if (referenceUnit == null) {
            referenceUnit = Unit.KILOGRAM;
        }

        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            formattedString += " / " + referenceUnit.getDisplayValue();
        }

        return formattedString;
    }
}
