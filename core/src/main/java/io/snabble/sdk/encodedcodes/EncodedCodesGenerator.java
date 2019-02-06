package io.snabble.sdk.encodedcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.codes.templates.groups.EmbedGroup;

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
        ScannedCode scannedCode;

        public ProductInfo(Product product, int quantity, ScannedCode scannedCode) {
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
                    shoppingCart.getScannedCode(i)));
        }

        Collections.sort(productInfos, new Comparator<ProductInfo>() {
            @Override
            public int compare(ProductInfo p1, ProductInfo p2) {
                int price1 = p1.product.getDiscountedPrice();
                if (p1.scannedCode.hasPrice()) {
                    price1 = p1.scannedCode.getPrice();
                }

                int price2 = p2.product.getDiscountedPrice();
                if (p2.scannedCode.hasPrice()) {
                    price2 = p2.scannedCode.getPrice();
                }

                if (price1 < price2) {
                    return -1;
                } else if (price1 > price2) {
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
                // encoding weight in ean
                Product.Code[] codes = productInfo.product.getScannableCodes();
                for (Product.Code code : codes) {
                    if ("default".equals(code.template)) {
                        continue;
                    }

                    CodeTemplate codeTemplate = options.project.getCodeTemplate(code.template);
                    if (codeTemplate != null && codeTemplate.getGroup(EmbedGroup.class) != null) {
                        ScannedCode scannedCode = codeTemplate.code(code.lookupCode)
                                .embed(productInfo.quantity)
                                .buildCode();

                        if (options.repeatCodes) {
                            addScannableCode(scannedCode.getCode(), ageRestricted);
                        } else {
                            addScannableCode("1" + options.countSeparator + scannedCode.getCode(), ageRestricted);
                        }
                        break;
                    }
                }
            } else if (productInfo.product.getType() == Product.Type.PreWeighed) {
                if (options.repeatCodes) {
                    addScannableCode(productInfo.scannedCode.getCode(), ageRestricted);
                } else {
                    addScannableCode("1" + options.countSeparator + productInfo.scannedCode.getCode(), ageRestricted);
                }
            } else {
                int q = productInfo.quantity;
                String transmissionCode = productInfo.product.getTransmissionCode(productInfo.scannedCode.getLookupCode());

                if (transmissionCode == null) {
                    transmissionCode = productInfo.scannedCode.getCode();
                }

                CodeTemplate codeTemplate = options.project.getTransformationTemplate(productInfo.scannedCode.getTransformationTemplateName());
                if (codeTemplate != null) {
                    ScannedCode scannedCode = codeTemplate
                            .override(productInfo.scannedCode.getTransformationCode())
                            .code(productInfo.scannedCode.getLookupCode())
                            .embed(productInfo.scannedCode.getEmbeddedData())
                            .buildCode();
                    transmissionCode = scannedCode.getCode();
                }

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
