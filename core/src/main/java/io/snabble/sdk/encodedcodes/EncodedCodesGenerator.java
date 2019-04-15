package io.snabble.sdk.encodedcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Unit;
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
        if (options.repeatCodes) {
            addScannableCode(code, false);
        } else {
            addScannableCode("1" + options.countSeparator + code, false);
        }
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

        ArrayList<String> ret = new ArrayList<>();
        for (int i=0; i<encodedCodes.size(); i++) {
            String code = encodedCodes.get(i);
            code = code.replace("{qrCodeCount}", String.valueOf(encodedCodes.size()));
            ret.add(code);
        }

        clear();
        return ret;
    }

    private boolean hasAgeRestrictedCode(ShoppingCart shoppingCart) {
        for (int i = 0; i < shoppingCart.size(); i++) {
            Product product = shoppingCart.get(i).getProduct();

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
            ShoppingCart.Item item = shoppingCart.get(i);
            Product product = item.getProduct();
            if (ageRestricted != isAgeRestricted(product)) {
                continue;
            }

            productInfos.add(new ProductInfo(product,
                    item.getQuantity(),
                    item.getScannedCode()));
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
                            .code(productInfo.scannedCode.getTransformationCode())
                            .embed(productInfo.scannedCode.getEmbeddedData())
                            .buildCode();
                    transmissionCode = scannedCode.getCode();
                }

                // zero amount products
                Unit unit = productInfo.product.getEncodingUnit(productInfo.scannedCode.getTemplateName(), productInfo.scannedCode.getLookupCode());
                CodeTemplate template = options.project.getCodeTemplate(productInfo.scannedCode.getTemplateName());
                if (unit == Unit.PIECE) {
                    ScannedCode scannedCode = template.match(productInfo.scannedCode.getCode()).buildCode();
                    if (scannedCode != null) {
                        if (scannedCode.getEmbeddedData() == 0) {
                            ScannedCode code = template
                                    .code(scannedCode.getLookupCode())
                                    .embed(q)
                                    .buildCode();

                            if (code != null) {
                                transmissionCode = code.getCode();
                                q = 1;
                            }
                        }
                    }
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
        code = code.replace("{count}", String.valueOf(codeCount));
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
