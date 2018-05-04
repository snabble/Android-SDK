package io.snabble.sdk;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Class that holds all of the product information.
 */
public class Product {
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

        Type(int databaseValue){
            this.databaseValue = databaseValue;
        }

        public int getDatabaseValue(){
            return databaseValue;
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
    private String depositProductSku;
    private Product depositProduct;
    private Type type;
    private boolean isDeposit;
    private int boost;
    private String subtitle;
    private String basePrice;

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

    public String[] getWeighedItemIds() {
        return weighedItemIds;
    }

    /**
     * @return The importance index of the product. The higher the number, the more important
     * the product is.
     * <p>
     * Can be used to display promotions of products.
     */
    public int getBoost() {
        return boost;
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
     * Deprecated: use getDepositProduct instead.
     *
     * @return The sku of the deposit product.
     */
    @Deprecated
    public String getDepositProductSku() {
        return depositProductSku;
    }

    /**
     * Returns the deposit product, or null if the product has no deposit product
     */
    public Product getDepositProduct() {
        return depositProduct;
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

    public int getPriceForQuantity(int quantity) {
        return getPriceForQuantity(quantity, RoundingMode.UP);
    }

    public int getPriceForQuantity(int quantity, RoundingMode roundingMode){
        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            BigDecimal pricePerUnit = new BigDecimal(price)
                    .divide(new BigDecimal(1000));

            return pricePerUnit.multiply(new BigDecimal(quantity))
                    .setScale(0, roundingMode)
                    .intValue();
        } else {
            return quantity * price;
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

    @Override
    public String toString() {
        return "Product{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

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

        public Builder setDepositProductSku(String sku) {
            product.depositProductSku = sku;
            return this;
        }

        public Builder setDepositProduct(Product depositProduct) {
            product.depositProduct = depositProduct;
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

        public Builder setBoost(int boost) {
            product.boost = boost;
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

        public Product build() {
            if (product.scannableCodes == null) {
                product.scannableCodes = new String[0];
            }

            if (product.weighedItemIds == null) {
                product.weighedItemIds = new String[0];
            }

            return product;
        }
    }
}
