package io.snabble.sdk.encodedcodes;

import java.util.ArrayList;

import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.codes.EAN13;

public class EncodedCodesGenerator {
    private StringBuilder stringBuilder;
    private EncodedCodesOptions options;
    private boolean addedCodeWithCheck;

    private ArrayList<String> encodedCodes;
    private int codeCount;

    public EncodedCodesGenerator(EncodedCodesOptions encodedCodesOptions) {
        encodedCodes = new ArrayList<>();
        stringBuilder = new StringBuilder();
        options = encodedCodesOptions;
    }

    public void add(String code) {
        addScannableCode(code);
    }

    public void add(ShoppingCart shoppingCart) {
        addProducts(shoppingCart, false);
        addProducts(shoppingCart, true);
    }

    public ArrayList<String> generate() {
        if(options.finalCode.length() != 0) {
            append(options.finalCode);
        }

        finishCode();
        ArrayList<String> ret = encodedCodes;
        encodedCodes = new ArrayList<>();
        return ret;
    }

    private void addProducts(ShoppingCart shoppingCart, boolean ageRestricted) {
        for (int i = 0; i < shoppingCart.size(); i++) {
            Product product = shoppingCart.getProduct(i);
            if(ageRestricted != product.getSaleRestriction().isAgeRestriction()) {
                continue;
            }

            if (ageRestricted
                    && options.nextCodeWithCheck.length() != 0
                    && !addedCodeWithCheck
                    && product.getSaleRestriction().isAgeRestriction()
                    && product.getSaleRestriction().getValue() >= 16) {
                addedCodeWithCheck = true;
                append(options.nextCodeWithCheck);
                finishCode();
            }

            if (product.getType() == Product.Type.UserWeighed) {
                //encoding weight in ean
                String[] weighItemIds = product.getWeighedItemIds();
                if (weighItemIds != null && weighItemIds.length > 0) {
                    StringBuilder code = new StringBuilder(weighItemIds[0]);
                    if (code.length() == 13) {
                        StringBuilder embeddedWeight = new StringBuilder();
                        String quantity = String.valueOf(shoppingCart.getQuantity(i));
                        int leadingZeros = 5 - quantity.length();
                        for (int j = 0; j < leadingZeros; j++) {
                            embeddedWeight.append('0');
                        }
                        embeddedWeight.append(quantity);
                        code.replace(7, 12, embeddedWeight.toString());
                        code.setCharAt(6, Character.forDigit(EAN13.internalChecksum(code.toString()), 10));
                        code.setCharAt(12, Character.forDigit(EAN13.checksum(code.substring(0, 12)), 10));
                        addScannableCode(code.toString());
                    }
                }
            } else {
                int q = shoppingCart.getQuantity(i);
                for (int j = 0; j < q; j++) {
                    addScannableCode(product.getTransmissionCode(shoppingCart.getScannedCode(i)));
                }
            }
        }
    }

    private void finishCode() {
        stringBuilder.append(options.suffix);
        String code = stringBuilder.toString();
        encodedCodes.add(code);
        stringBuilder = new StringBuilder();
        codeCount = 0;
    }

    private void addScannableCode(String scannableCode) {
        int charsLeft = options.maxChars - stringBuilder.length();
        int suffixCodeLength = Math.max(options.nextCode.length(), options.finalCode.length());
        int requiredLength = scannableCode.length()
                + suffixCodeLength
                + options.separator.length() * (suffixCodeLength > 0 ? 2 : 1)
                + options.suffix.length();

        if (charsLeft < requiredLength || (codeCount + (suffixCodeLength > 0 ? 2 : 1)) > options.maxCodes) {
            append(options.nextCode);
            finishCode();
        }

        append(scannableCode);
        codeCount++;
    }

    private void append(String code) {
        if(code.length() == 0) {
            return;
        }

        if (stringBuilder.length() == 0) {
            stringBuilder.append(options.prefix);
        } else {
            stringBuilder.append(options.separator);
        }

        stringBuilder.append(code);
    }
}
