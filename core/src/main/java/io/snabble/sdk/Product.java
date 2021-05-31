package io.snabble.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.utils.GsonHolder;

/**
 * Class that holds all of the product information.
 */
public class Product implements Serializable, Parcelable {
    public enum Type {
        /**
         * A basic product with price information.
         */
        Article(0),

        /**
         * A product that is pre weighed. The codes from {@link Product#getScannableCodes()} are reference codes that do not
         * contain any price information.
         * <p>
         * Scanned codes usually have a different code the code you get from {@link Product#getScannableCodes()}, containing the
         * price information.
         */
        PreWeighed(1),

        /**
         * A product that needs to be user weighed. The price from {@link Product#getListPrice()}
         * is a base price of 1000g
         */
        UserWeighed(2),

        /**
         * A product that is used for deposit return voucher's
         */
        DepositReturnVoucher(3);

        private int databaseValue;

        Type(int databaseValue) {
            this.databaseValue = databaseValue;
        }

        public int getDatabaseValue() {
            return databaseValue;
        }
    }

    public enum SaleRestriction {
        @SerializedName("")
        NONE(0, 0),
        @SerializedName("min_age_6")
        MIN_AGE_6(1, 6),
        @SerializedName("min_age_12")
        MIN_AGE_12(1, 12),
        @SerializedName("min_age_14")
        MIN_AGE_14(1, 14),
        @SerializedName("min_age_16")
        MIN_AGE_16(1, 16),
        @SerializedName("min_age_18")
        MIN_AGE_18(1, 18),
        @SerializedName("min_age_21")
        MIN_AGE_21(1, 21);

        private long databaseType;
        private long value;

        private static SaleRestriction[] values = SaleRestriction.values();

        SaleRestriction(long dbType, long value) {
            this.databaseType = dbType;
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public boolean isAgeRestriction() {
            return databaseType == 1;
        }

        public static SaleRestriction fromDatabaseField(long dbType, long value) {
            for (SaleRestriction sr : values) {
                if (sr.databaseType == dbType && sr.value == value) {
                    return sr;
                }
            }

            return SaleRestriction.NONE;
        }
    }

    public enum Availability {
        IN_STOCK,
        LISTED,
        NOT_AVAILABLE;
    }

    public static class Code implements Serializable {
        public final String lookupCode;
        public final String transmissionCode;
        public final String template;
        public final String transmissionTemplate;
        public final Unit encodingUnit;
        public final boolean isPrimary;
        public final int specifiedQuantity;

        public Code(String lookupCode,
                    String transmissionCode,
                    String template,
                    String transmissionTemplate,
                    Unit encodingUnit,
                    boolean isPrimary,
                    int specifiedQuantity) {
            this.lookupCode = lookupCode;
            this.transmissionCode = transmissionCode;
            this.template = template;
            this.transmissionTemplate = transmissionTemplate;
            this.encodingUnit = encodingUnit;
            this.isPrimary = isPrimary;
            this.specifiedQuantity = specifiedQuantity;
        }

        @Override
        public String toString() {
            return "Code{" +
                    "lookupCode='" + lookupCode + '\'' +
                    ", transmissionCode='" + transmissionCode + '\'' +
                    ", template='" + template + '\'' +
                    ", transmissionTemplate='" + transmissionTemplate + '\'' +
                    ", encodingUnit=" + encodingUnit +
                    ", isPrimary=" + isPrimary +
                    ", specifiedQuantity=" + specifiedQuantity +
                    '}';
        }
    }

    private String sku;
    private String name;
    private String description;
    private Code[] scannableCodes;
    private int price;
    private int discountedPrice;
    private int customerCardPrice;
    private String imageUrl;
    private Product depositProduct;
    private Product[] bundleProducts;
    private Type type;
    private boolean isDeposit;
    private String subtitle;
    private String basePrice;
    private String scanMessage;
    private SaleRestriction saleRestriction = SaleRestriction.NONE;
    private Availability availability = Availability.IN_STOCK;
    private Unit referenceUnit;
    private Unit encodingUnit;
    private boolean saleStop;
    private boolean notForSale;

    public Product() {

    }

    /**
     * @return The unique identifier of the product. Usually the same identifier
     * the retailer uses internally.
     */
    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Code[] getScannableCodes() {
        return scannableCodes;
    }

    @Deprecated // use getListPrice instead
    public int getPrice() {
        return getListPrice();
    }

    public int getListPrice() {
        return price;
    }

    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Gets the discounted price, or the default price if this product has no discounted price,
     */
    public int getDiscountedPrice() {
        return discountedPrice == 0 ? getListPrice() : discountedPrice;
    }

    public int getPrice(String customerCard) {
        return customerCard != null ? getCustomerCardPrice() : getDiscountedPrice();
    }

    public int getCustomerCardPrice() {
        return customerCardPrice == 0 ? getDiscountedPrice() : customerCardPrice;
    }


    @Deprecated // will be removed in a future version
    public boolean isDiscounted() {
        return discountedPrice > 0;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Returns the deposit product, or null if the product has no deposit product
     */
    public Product getDepositProduct() {
        return depositProduct;
    }

    public Product[] getBundleProducts() {
        return bundleProducts;
    }

    /**
     * Indicates if this product is a deposit product.
     *
     * @return true, if this product is a deposit product.
     */
    public boolean isDeposit() {
        return isDeposit;
    }

    /**
     * A price description, usually containing more information of how the price was calculated.
     * <p>
     * For example "1.99€ / 100g".
     *
     * @return a string containing the base price information.
     */
    public String getBasePrice() {
        return basePrice;
    }

    /**
     * @return The {@link Type} of the product.
     */
    public Type getType() {
        return type;
    }

    /**
     * @return The [@link SaleRestriction} of the product.
     */
    public SaleRestriction getSaleRestriction() {
        return saleRestriction;
    }

    public Unit getReferenceUnit() {
        return referenceUnit;
    }

    public Unit getEncodingUnit(String lookupCode) {
        return getEncodingUnit(null, lookupCode);
    }

    public Unit getEncodingUnit(String templateName, String lookupCode) {
        for (Code code : scannableCodes) {
            if (code.lookupCode.equals(lookupCode)) {
                if ((templateName != null && templateName.equals(code.template)) || "default".equals(code.template)) {
                    if (code.encodingUnit != null) {
                        return code.encodingUnit;
                    }
                }
            }
        }

        return encodingUnit;
    }

    public String getTransmissionCode(Project project, String templateName, String lookupCode, int embeddedData) {
        for (Code code : scannableCodes) {
            if (code.lookupCode.equals(lookupCode)) {
                if ((templateName != null && templateName.equals(code.template)) || "default".equals(code.template)) {
                    if (project != null && code.transmissionTemplate != null) {
                        CodeTemplate codeTemplate = project.getCodeTemplate(code.transmissionTemplate);
                        if (codeTemplate != null && embeddedData != 0) {
                            codeTemplate.code(lookupCode).embed(embeddedData).buildCode().getCode();
                        } else {
                            return code.transmissionCode;
                        }
                    } else {
                        return code.transmissionCode;
                    }

                }
            }
        }

        return null;
    }

    public Product.Code getPrimaryCode() {
        for (Code code : scannableCodes) {
            if (code.isPrimary) {
                return code;
            }
        }

        return null;
    }
    /**
     *
     * @return returns true if this product should not be available for sale anymore.
     */
    public boolean getSaleStop() {
        return saleStop;
    }

    public boolean getNotForSale() {
        return notForSale;
    }

    public Availability getAvailability() {
        return availability;
    }

    /** Returns the identifier of the scan message to look up in i18n resources **/
    public String getScanMessage() {
        return scanMessage;
    }

    public int getPriceForQuantity(int quantity, ScannedCode scannedCode, RoundingMode roundingMode) {
        return getPriceForQuantity(quantity, scannedCode, roundingMode, null);
    }

    public int getPriceForQuantity(int quantity, ScannedCode scannedCode, RoundingMode roundingMode, String customerCardId) {
        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            String lookupCode = scannedCode != null ? scannedCode.getLookupCode() : null;

            Unit referenceUnit = this.referenceUnit;
            Unit encodingUnit = null;

            if (scannedCode != null) {
                encodingUnit = scannedCode.getEmbeddedUnit();
            }

            if (encodingUnit == null) {
                encodingUnit = getEncodingUnit(lookupCode);
            }

            if (referenceUnit == null) {
                referenceUnit = Unit.KILOGRAM;
            }

            if (encodingUnit == null) {
                encodingUnit = Unit.GRAM;
            }

            int price = getPrice(customerCardId);
            if (scannedCode != null && scannedCode.hasPrice()) {
                price = scannedCode.getPrice();
            }

            BigDecimal pricePerReferenceUnit = new BigDecimal(price);
            BigDecimal pricePerUnit = Unit.convert(pricePerReferenceUnit, encodingUnit, referenceUnit);

            return pricePerUnit.multiply(new BigDecimal(quantity))
                    .setScale(0, roundingMode)
                    .intValue();
        } else {
            return quantity * getPrice(customerCardId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;
        return sku.equals(product.sku);
    }

    @Override
    public int hashCode() {
        return sku.hashCode();
    }

    public String toShortString() {
        return "Product{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public String toString() {
        return "Product{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", scannableCodes=" + Arrays.toString(scannableCodes) +
                ", price=" + price +
                ", discountedPrice=" + discountedPrice +
                ", imageUrl='" + imageUrl + '\'' +
                ", depositProduct=" + depositProduct +
                ", bundleProducts=" + Arrays.toString(bundleProducts) +
                ", type=" + type +
                ", isDeposit=" + isDeposit +
                ", subtitle='" + subtitle + '\'' +
                ", basePrice='" + basePrice + '\'' +
                ", saleRestriction=" + saleRestriction +
                ", referenceUnit=" + referenceUnit +
                ", saleStop=" + saleStop +
                ", notForSale=" + notForSale +
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(GsonHolder.get().toJson(this));
    }

    protected Product(Parcel in) {
        InstanceCreator<Product> creator = new InstanceCreator<Product>() {
            public Product createInstance(java.lang.reflect.Type type) { return Product.this; }
        };

        Gson gson = new GsonBuilder().registerTypeAdapter(Product.class, creator).create();
        gson.fromJson(in.readString(), Product.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    public static class Builder {
        private Product product = new Product();

        public Builder setName(String name) {
            product.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            product.description = description;
            return this;
        }

        public Builder setScannableCodes(Code[] scannableCodes) {
            product.scannableCodes = scannableCodes;
            return this;
        }

        public Builder setSku(String sku) {
            product.sku = sku;
            return this;
        }

        public Builder setPrice(int price) {
            product.price = price;
            return this;
        }

        public Builder setDiscountedPrice(int discountedPrice) {
            product.discountedPrice = discountedPrice;
            return this;
        }

        public Builder setCustomerCardPrice(int customerCardPrice) {
            product.customerCardPrice = customerCardPrice;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            product.imageUrl = imageUrl == null || imageUrl.trim().length() == 0 ? null : imageUrl;
            return this;
        }

        public Builder setDepositProduct(Product depositProduct) {
            product.depositProduct = depositProduct;
            return this;
        }

        public Builder setBundleProducts(Product[] bundleProducts) {
            product.bundleProducts = bundleProducts;
            return this;
        }

        public Builder setIsDeposit(boolean isDeposit) {
            product.isDeposit = isDeposit;
            return this;
        }

        public Builder setType(Type type) {
            product.type = type;
            return this;
        }

        public Builder setSubtitle(String subtitle) {
            product.subtitle = subtitle;
            return this;
        }

        public Builder setBasePrice(String basePrice) {
            product.basePrice = basePrice;
            return this;
        }

        public Builder setSaleRestriction(SaleRestriction saleRestriction) {
            product.saleRestriction = saleRestriction;
            return this;
        }

        public Builder setSaleStop(boolean saleStop) {
            product.saleStop = saleStop;
            return this;
        }

        public Builder setNotForSale(boolean notForSale) {
            product.notForSale = notForSale;
            return this;
        }

        public Builder setAvailability(Availability availability) {
            product.availability = availability;
            return this;
        }

        public Builder setScanMessage(String scanMessage) {
            product.scanMessage = scanMessage;
            return this;
        }

        public Builder setReferenceUnit(Unit referenceUnit) {
            product.referenceUnit = referenceUnit;
            return this;
        }

        public Builder setEncodingUnit(Unit encodingUnit) {
            product.encodingUnit = encodingUnit;
            return this;
        }

        public Product build() {
            if (product.scannableCodes == null) {
                product.scannableCodes = new Code[0];
            }

            if (product.bundleProducts == null) {
                product.bundleProducts = new Product[0];
            }

            return product;
        }
    }
}
