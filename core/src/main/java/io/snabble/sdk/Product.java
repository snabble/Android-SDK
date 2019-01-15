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
import java.util.HashMap;
import java.util.Map;

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
         * A product that needs to be user weighed. The price from {@link Product#getPrice()}
         * is a base price of 1000g
         */
        UserWeighed(2);

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

    private String sku;
    private String name;
    private String description;
    private String[] scannableCodes;
    private String[] weighedItemIds;
    private int price;
    private int discountedPrice;
    private String imageUrl;
    private Product depositProduct;
    private Product[] bundleProducts;
    private Type type;
    private boolean isDeposit;
    private String subtitle;
    private String basePrice;
    private SaleRestriction saleRestriction = SaleRestriction.NONE;
    private Unit referenceUnit;
    private Unit encodingUnit;
    private Map<String, String> transmissionCodes;
    private boolean saleStop;

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

    public String[] getScannableCodes() {
        return scannableCodes;
    }

    public String getTransmissionCode(String code) {
        if (transmissionCodes != null) {
            String newCode = transmissionCodes.get(code);
            if (newCode != null) {
                return newCode;
            }
        }

        return code;
    }

    public String[] getWeighedItemIds() {
        return weighedItemIds;
    }

    public int getPrice() {
        return price;
    }

    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Gets the discounted price, or the default price if this product has no discounted price,
     */
    public int getDiscountedPrice() {
        return discountedPrice == 0 ? getPrice() : discountedPrice;
    }

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

    public Unit getEncodingUnit() {
        return encodingUnit;
    }

    /**
     * @return returns true if this product should not be available for sale anymore.
     */
    public boolean getSaleStop() {
        return saleStop;
    }

    public int getPriceForQuantity(int quantity, RoundingMode roundingMode) {
        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            Unit referenceUnit = this.referenceUnit;
            Unit encodingUnit = this.encodingUnit;

            if (referenceUnit == null) {
                referenceUnit = Unit.KILOGRAM;
            }

            if (encodingUnit == null) {
                encodingUnit = Unit.GRAM;
            }

            BigDecimal pricePerReferenceUnit = new BigDecimal(getDiscountedPrice());
            BigDecimal pricePerUnit = Unit.convert(pricePerReferenceUnit,
                    referenceUnit, encodingUnit, 16, roundingMode);

            return pricePerUnit.multiply(new BigDecimal(quantity))
                    .setScale(0, roundingMode)
                    .intValue();
        } else {
            return quantity * getDiscountedPrice();
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
                ", weighedItemIds=" + Arrays.toString(weighedItemIds) +
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
                ", transmissionCodes=" + transmissionCodes +
                ", saleStop=" + saleStop +
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

        public Builder setScannableCodes(String[] scannableCodes) {
            product.scannableCodes = scannableCodes;
            return this;
        }

        public Builder setWeighedItemIds(String[] weighedItemIds) {
            product.weighedItemIds = weighedItemIds;
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

        public Builder setImageUrl(String imageUrl) {
            product.imageUrl = imageUrl;
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

        public Builder setReferenceUnit(Unit referenceUnit) {
            product.referenceUnit = referenceUnit;
            return this;
        }

        public Builder setEncodingUnit(Unit encodingUnit) {
            product.encodingUnit = encodingUnit;
            return this;
        }

        public Builder addTransmissionCode(String fromCode, String transmissionCode) {
            if (product.transmissionCodes == null) {
                product.transmissionCodes = new HashMap<>();
            }

            product.transmissionCodes.put(fromCode, transmissionCode);
            return this;
        }

        public Product build() {
            if (product.scannableCodes == null) {
                product.scannableCodes = new String[0];
            }

            if (product.weighedItemIds == null) {
                product.weighedItemIds = new String[0];
            }

            if (product.bundleProducts == null) {
                product.bundleProducts = new Product[0];
            }

            return product;
        }
    }
}
