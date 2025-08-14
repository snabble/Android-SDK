package io.snabble.sdk.encodedcodes;

import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.Product;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.coupons.Coupon;
import io.snabble.sdk.coupons.CouponCode;
import io.snabble.sdk.shoppingcart.ShoppingCart;
import io.snabble.sdk.shoppingcart.data.item.ItemType;

/**
 * Class for encoding scanned codes into one or multiple combined codes (e.g. a QR-Code)
 */
public class EncodedCodesGenerator {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public class ProductInfo {
        Product product;
        int quantity;
        ScannedCode scannedCode;

        public ProductInfo(Product product,
                           int quantity,
                           ScannedCode scannedCode) {
            this.product = product;
            this.quantity = quantity;
            this.scannedCode = scannedCode;
        }
    }

    private StringBuilder stringBuilder;
    private final EncodedCodesOptions options;
    private ArrayList<String> encodedCodes;
    private int codeCount;
    private boolean hasAgeRestrictedCode;

    public EncodedCodesGenerator(EncodedCodesOptions encodedCodesOptions) {
        encodedCodes = new ArrayList<>();
        stringBuilder = new StringBuilder();
        options = encodedCodesOptions;
    }

    /**
     * Add a arbitrary code to the encoded code generator
     */
    public void add(String code) {
        if (code == null) {
            return;
        }

        if (options.repeatCodes) {
            addScannableCode(code, false);
        } else {
            addScannableCode("1" + options.countSeparator + code, false);
        }
    }

    /**
     * Add a shopping cart and all its products to the encoded code
     */
    public void add(ShoppingCart shoppingCart) {
        List<ProductInfo> productInfos = new ArrayList<>();
        List<String> coupons = new ArrayList<>();

        for (int i = 0; i < shoppingCart.size(); i++) {
            ShoppingCart.Item item = shoppingCart.get(i);

            if (item.getType() == ItemType.COUPON) {
                ScannedCode scannedCode = item.getScannedCode();
                if (scannedCode != null) {
                    coupons.add(scannedCode.getCode());
                } else {
                    Coupon coupon = item.getCoupon();
                    if (coupon != null) {
                        List<CouponCode> codes = coupon.getCodes();
                        if (codes.size() > 0) {
                            coupons.add(codes.get(0).getCode());
                        }
                    }
                }
            } else {
                Product product = item.getProduct();
                if (product == null) {
                    continue;
                }

                productInfos.add(new ProductInfo(product, item.getUnitBasedQuantity(), item.getScannedCode()));
            }
        }

        addProducts(productInfos, false);
        addProducts(productInfos, true);

        for (String couponCode : coupons) {
            add(couponCode);
        }
    }

    /**
     * Clears the encoded codes generator
     */
    public void clear() {
        encodedCodes = new ArrayList<>();
        stringBuilder = new StringBuilder();
    }

    /**
     * Generate a list of encoded codes
     *
     * Supported placeholders:
     *
     * {qrCodeCount}: Number of encoded codes
     * {qrCodeIndex}: Current index
     */
    public ArrayList<String> generate() {
        return generate(null);
    }

    /**
     * Generate a list of encoded codes, including adding the checkout id if supported.
     *
     * Supported placeholders:
     *
     * {qrCodeCount}: Number of encoded codes
     * {qrCodeIndex}: Current index
     * {checkoutId}: The passed checkout id
     */
    public ArrayList<String> generate(String checkoutId) {
        if (options.finalCode != null && !options.finalCode.isEmpty()) {
            if (getCountSeparatorLength() > 0) {
                append("1" + options.countSeparator + options.finalCode);
            } else {
                append(options.finalCode);
            }
        }

        finishCode();

        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < encodedCodes.size(); i++) {
            String code = encodedCodes.get(i);
            code = code.replace("{qrCodeCount}", String.valueOf(encodedCodes.size()));
            code = code.replace("{qrCodeIndex}", String.valueOf(i + 1));
            if (checkoutId == null) {
                checkoutId = "";
            }

            if (checkoutId.equals("")) {
                code = code.replace(";{checkoutId}", "");
            }

            code = code.replace("{checkoutId}", checkoutId);

            ret.add(code);
        }

        clear();
        return ret;
    }

    private boolean hasAgeRestrictedCode(List<ProductInfo> productInfos) {
        for (int i = 0; i < productInfos.size(); i++) {
            Product product = productInfos.get(i).product;

            if (product.getSaleRestriction().isAgeRestriction()
                    && product.getSaleRestriction().getValue() >= 16) {
                return true;
            }
        }

        return false;
    }

    private boolean isAgeRestricted(Product product) {
        return product.getSaleRestriction().isAgeRestriction()
                && product.getSaleRestriction().getValue() >= 16;
    }

    private void addProducts(final List<ProductInfo> productInfos, boolean ageRestricted) {
        hasAgeRestrictedCode = hasAgeRestrictedCode(productInfos);

        for (ProductInfo productInfo : productInfos) {
            if (ageRestricted != isAgeRestricted(productInfo.product)) {
                continue;
            }

            if (productInfo.product.getType() == Product.Type.PreWeighed) {
                String transmissionCode = productInfo.product.getTransmissionCode(
                        options.project,
                        productInfo.scannedCode.getTemplateName(),
                        productInfo.scannedCode.getLookupCode(),
                        productInfo.scannedCode.getEmbeddedData());

                if (transmissionCode == null) {
                    transmissionCode = productInfo.scannedCode.getCode();
                }

                if (options.repeatCodes) {
                    addScannableCode(transmissionCode, ageRestricted);
                } else {
                    addScannableCode("1" + options.countSeparator + productInfo.scannedCode.getCode(), ageRestricted);
                }
            } else {
                int q = productInfo.quantity;
                String transmissionCode = productInfo.product.getTransmissionCode(
                        options.project,
                        productInfo.scannedCode.getTemplateName(),
                        productInfo.scannedCode.getLookupCode(),
                        productInfo.scannedCode.getEmbeddedData());

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

    private int getCountSeparatorLength() {
        if (options.repeatCodes) {
            return 0;
        } else {
            return options.countSeparator.length();
        }
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

        if (options.finalCode.length() > 0 && getCountSeparatorLength() > 0) {
            suffixCodeLength += getCountSeparatorLength() + 1;
        }

        int suffixCodes = 0;

        if (options.finalCode.length() > 0) {
            suffixCodes++;
        }

        int codesNeeded = 1 + suffixCodes;
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
            stringBuilder.append(options.prefixMap.get(encodedCodes.size(), options.prefix));
        } else {
            stringBuilder.append(options.separator);
        }

        stringBuilder.append(code);
    }
}
