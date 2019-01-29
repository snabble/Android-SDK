package io.snabble.sdk.encodedcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class EncodedCodesGenerator {
    private StringBuilder stringBuilder;
    private EncodedCodesOptions options;
    private ArrayList<String> encodedCodes;
    private int codeCount;
    private boolean hasAgeRestrictedCode;

    public EncodedCodesGenerator(EncodedCodesOptions encodedCodesOptions) {
        encodedCodes = new ArrayList<>();
        stringBuilder = new StringBuilder();
        options = encodedCodesOptions;
    }

    public void add(String code) {
        addScannableCode(code, false);
    }

    public void add(ShoppingCart shoppingCart) {
        addProducts(shoppingCart, false);
        addProducts(shoppingCart, true);
    }

    public void clear() {
        encodedCodes = new ArrayList<>();
        stringBuilder = new StringBuilder();
    }

    public ArrayList<String> generate() {
        if (options.finalCode.length() != 0) {
            append(options.finalCode);
        }

        finishCode();
        ArrayList<String> ret = encodedCodes;
        clear();
        return ret;
    }

    private boolean hasAgeRestrictedCode(ShoppingCart shoppingCart) {
        for (int i = 0; i < shoppingCart.size(); i++) {
            Product product = shoppingCart.getProduct(i);

            if (product.getSaleRestriction().isAgeRestriction()
                    && product.getSaleRestriction().getValue() >= 16) {
                return true;
            }
        }

        return false;
    }

    private boolean isAgeRestricted(Product product) {
        if (product.getSaleRestriction().isAgeRestriction()
                && product.getSaleRestriction().getValue() >= 16) {
            return true;
        }

        return false;
    }

    private class ProductInfo {
        Product product;
        int quantity;
        String scannedCode;

        public ProductInfo(Product product, int quantity, String scannedCode) {
            this.product = product;
            this.quantity = quantity;
            this.scannedCode = scannedCode;
        }
    }

    private void addProducts(ShoppingCart shoppingCart, boolean ageRestricted) {
        hasAgeRestrictedCode = hasAgeRestrictedCode(shoppingCart);

        List<ProductInfo> productInfos = new ArrayList<>();
        for (int i = 0; i < shoppingCart.size(); i++) {
            Product product = shoppingCart.getProduct(i);
            if (ageRestricted != isAgeRestricted(product)) {
                continue;
            }

            productInfos.add(new ProductInfo(product,
                    shoppingCart.getQuantity(i),
                    shoppingCart.getScannedCode(i).getCode()));
        }

        Collections.sort(productInfos, new Comparator<ProductInfo>() {
            @Override
            public int compare(ProductInfo p1, ProductInfo p2) {
                if(p1.product.getDiscountedPrice() < p2.product.getDiscountedPrice()) {
                    return -1;
                } else if(p1.product.getDiscountedPrice() > p2.product.getDiscountedPrice()) {
                    return 1;
                }

                return 0;
            }
        });

        for (ProductInfo productInfo : productInfos) {
            if (ageRestricted != isAgeRestricted(productInfo.product)) {
                continue;
            }

            if (productInfo.product.getType() == Product.Type.UserWeighed) {
                //encoding weight in ean
                // TODO CHANGED: TEST THIS
                for (Product.Code productCode : productInfo.product.getScannableCodes()) {
                    boolean isEan13InStore = "ean13_instore".equals(productCode.template);
                    boolean isEan13InStoreWithCheck = "ean13_instore_chk".equals(productCode.template);

                    if (isEan13InStore || isEan13InStoreWithCheck) {
                        if (productCode.lookupCode.length() == 5) {
                            StringBuilder code = new StringBuilder("2");
                            code.append(productCode.lookupCode);

                            StringBuilder embeddedWeight = new StringBuilder();
                            String quantity = String.valueOf(productInfo.quantity);
                            int leadingZeros = 5 - quantity.length();
                            for (int j = 0; j < leadingZeros; j++) {
                                embeddedWeight.append('0');
                            }
                            embeddedWeight.append(quantity);

                            if (isEan13InStoreWithCheck) {
                                code.append(Character.forDigit(EAN13.internalChecksum(embeddedWeight.toString(), 0), 10));
                            } else {
                                code.append('0');
                            }

                            code.append(embeddedWeight);
                            code.append(Character.forDigit(EAN13.checksum(code.toString()), 10));

                            if (options.repeatCodes) {
                                addScannableCode(code.toString(), ageRestricted);
                            } else {
                                addScannableCode("1" + options.countSeparator + code.toString(), ageRestricted);
                            }
                        }
                        break;
                    }
                }
            } else {
                int q = productInfo.quantity;
                String transmissionCode = productInfo.product.getTransmissionCode(productInfo.scannedCode);
                if (transmissionCode != null) {
                    if (options.repeatCodes) {
                        for (int j = 0; j < q; j++) {
                            addScannableCode(transmissionCode, ageRestricted);
                        }
                    } else {
                        addScannableCode(q + options.countSeparator + transmissionCode, ageRestricted);
                    }
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

    private void addScannableCode(String scannableCode, boolean isAgeRestricted) {
        String nextCode = hasAgeRestrictedCode ? options.nextCodeWithCheck : options.nextCode;

        if (isAgeRestricted
                && hasAgeRestrictedCode
                && encodedCodes.size() == 0
                && options.nextCodeWithCheck.length() > 0) {
            append(options.nextCodeWithCheck);
            finishCode();
        }

        int charsLeft = options.maxChars - stringBuilder.length();
        int suffixCodeLength = Math.max(nextCode.length(), options.finalCode.length());
        int codesNeeded = (suffixCodeLength > 0 ? 2 : 1);
        int requiredLength = scannableCode.length()
                + suffixCodeLength
                + options.separator.length() * codesNeeded
                + options.suffix.length();

        if (charsLeft < requiredLength || (codeCount + codesNeeded) > options.maxCodes) {
            append(nextCode);
            finishCode();
        }

        append(scannableCode);
        codeCount++;
    }

    private void append(String code) {
        if (code.length() == 0) {
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
