package io.snabble.sdk;

import static io.snabble.sdk.Unit.PIECE;
import static io.snabble.sdk.Unit.PRICE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.auth.AppUser;
import io.snabble.sdk.checkout.LineItem;
import io.snabble.sdk.checkout.LineItemType;
import io.snabble.sdk.checkout.PaymentMethodInfo;
import io.snabble.sdk.checkout.PriceModifier;
import io.snabble.sdk.checkout.Violation;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.coupons.Coupon;
import io.snabble.sdk.coupons.CouponType;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;

/**
 * Class representing the snabble shopping cart
 */
public class ShoppingCart implements Iterable<ShoppingCart.Item> {
    public static final int MAX_QUANTITY = 99999;

    /**
     * Enum describing the type of item
     */
    public enum ItemType {
        PRODUCT,
        LINE_ITEM,
        COUPON
    }

    /**
     * Enum describing the type of taxation
     */
    public enum Taxation {
        UNDECIDED("undecided"),
        IN_HOUSE("inHouse"),
        TAKEAWAY("takeaway");

        private final String value;

        Taxation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private ShoppingCartData data = new ShoppingCartData();
    private ShoppingCartData oldData = new ShoppingCartData();

    private transient List<ShoppingCartListener> listeners;
    private transient Project project;
    private transient ShoppingCartUpdater updater;
    private transient PriceFormatter priceFormatter;

    protected ShoppingCart() {
        // for gson
    }

    ShoppingCart(Project project) {
        data = new ShoppingCartData();
        updateTimestamp();

        initWithProject(project);
    }

    void initWithProject(Project project) {
        this.project = project;
        this.updater = new ShoppingCartUpdater(project, this);
        this.priceFormatter = project.getPriceFormatter();
        this.listeners = new CopyOnWriteArrayList<>();

        checkForTimeout();

        data.applyShoppingCart(this);

        if (oldData != null) {
            oldData.applyShoppingCart(this);
        }

        if (data.uuid == null) {
            generateNewUUID();
        }

        updatePrices(false);
    }

    /**
     * The id used to identify this cart session
     */
    public String getId() {
        return data.id;
    }

    /**
     * Create a new cart item using a product and a scanned code
     */
    public Item newItem(Product product, ScannedCode scannedCode) {
        return new Item(this, product, scannedCode);
    }

    /**
     * Create a new cart item using a coupon and a scanned code
     */
    public Item newItem(Coupon coupon, ScannedCode scannedCode) {
        return new Item(this, coupon, scannedCode);
    }

    /**
     * Create a new cart item using a line item of a checkout info
     */
    public Item newItem(LineItem lineItem) {
        return new Item(this, lineItem);
    }

    /**
     * Add a item to the cart
     */
    public void add(Item item) {
        insert(item, 0);
    }

    /**
     * Adds coupons without adding a scanned code to it, you can use this function to quickly
     * add DIGITAL coupons that do not have a barcode associated with them
     */
    public void addCoupon(Coupon coupon) {
        add(newItem(coupon, null));
    }

    /**
     * Adds coupons with a scanned code to it, you can use this function to quickly
     * add PRINTED coupons
     */
    public void addCoupon(Coupon coupon, ScannedCode scannedCode) {
        add(newItem(coupon, scannedCode));
    }

    /**
     * Insert a cart item into the shopping cart at a specific index
     */
    public void insert(Item item, int index) {
        insert(item, index, true);
    }

    void insert(Item item, int index, boolean update) {
        if (item.isMergeable()) {
            Item existing = getExistingMergeableProduct(item.getProduct());
            if (existing != null) {
                data.items.remove(existing);
                data.items.add(index, item);
                data.modCount++;
                generateNewUUID();
                checkLimits();

                notifyQuantityChanged(this, item);

                if (update) {
                    invalidateOnlinePrices();
                    updatePrices(true);
                }
                return;
            }
        }

        data.items.add(index, item);

        clearBackup();
        checkLimits();
        notifyItemAdded(this, item);

        // sort coupons to bottom
        Collections.sort(data.items, (o1, o2) -> {
            ItemType t1 = o1.getType();
            ItemType t2 = o2.getType();

            if (t2 == ItemType.COUPON && t1 == ItemType.PRODUCT) {
                return -1;
            } else if (t1 == ItemType.COUPON && t2 == ItemType.PRODUCT) {
                return 1;
            } else {
                return 0;
            }
        });

        if (update) {
            data.addCount++;
            data.modCount++;
            generateNewUUID();
            invalidateOnlinePrices();
            updatePrices(true);
        }
    }

    /**
     * Gets the cart item a specific index
     */
    public Item get(int index) {
        return data.items.get(index);
    }

    @NonNull
    @Override
    public Iterator<Item> iterator() {
        return data.items.iterator();
    }

    /**
     * Returns a cart item that contains the given product, if that cart item
     * can be merged.
     *
     * A cart item is not mergeable if it uses encoded data of a scanned code (e.g. a different price)
     */
    public Item getExistingMergeableProduct(Product product) {
        if (product == null) {
            return null;
        }

        for (Item item : data.items) {
            if (product.equals(item.product) && item.isMergeable()) {
                return item;
            }
        }

        return null;
    }

    /**
     * Find a cart item by it's id
     */
    public Item getByItemId(String itemId) {
        if (itemId == null) {
            return null;
        }

        for (Item item : data.items) {
            if (itemId.equals(item.id)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Gets the current index of a cart item
     */
    public int indexOf(Item item) {
        return data.items.indexOf(item);
    }

    /**
     * Removed a cart item from the cart by its index
     */
    public void remove(int index) {
        data.modCount++;
        generateNewUUID();
        Item item = data.items.remove(index);
        checkLimits();
        updatePrices(size() != 0);
        invalidateOnlinePrices();
        notifyItemRemoved(this, item, index);
    }

    /**
     * The number items in the cart.
     *
     * This is not the sum of articles.
     */
    public int size() {
        return data.items.size();
    }

    /**
     * Check if the cart is empty
     */
    public boolean isEmpty() {
        return data.items.isEmpty();
    }

    /**
     * Backups the cart, so it can be restured using {@link #restore()} later.
     *
     * A cart is restorable for up to 5 minutes.
     */
    public void backup() {
        if (data.items.size() > 0) {
            oldData = data.deepCopy();
            data.backupTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Check if the cart is backed up by {@link #backup()} and still in the 5 minute time window
     */
    public boolean isRestorable() {
        return oldData != null && data.backupTimestamp > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
    }

    /**
     * Clears the backup storage of the cart
     */
    public void clearBackup() {
        oldData = null;
        data.backupTimestamp = 0;
    }

    /**
     * Restores the cart previously backed up by {@link #backup()}
     */
    public void restore() {
        if (isRestorable()) {
            data = oldData;
            data.applyShoppingCart(this);
            
            clearBackup();
            checkLimits();
            updatePrices(false);
            notifyProductsUpdate(this);
        }
    }

    /**
     * The last time the cart was backed up by using {@link #backup()}
     * @return
     */
    public long getBackupTimestamp() {
        return data.backupTimestamp;
    }

    /**
     * Clears the cart of all items
     */
    public void clear() {
        data.items = new ArrayList<>();
        data.modCount = 0;
        data.addCount = 0;
        generateNewUUID();
        data.onlineTotalPrice = null;

        checkLimits();
        updatePrices(false);
        notifyCleared(this);
    }

    /**
     * Gets the current {@link Taxation} type of the shopping cart
     */
    public Taxation getTaxation() {
        // migration for old shopping carts
        if (data.taxation == null) {
            return Taxation.UNDECIDED;
        }
        return data.taxation;
    }

    /**
     * Sets the current taxation type of the cart
     */
    public void setTaxation(Taxation taxation) {
        this.data.taxation = taxation;
        notifyTaxationChanged(this, taxation);
    }

    /**
     * Clears the cart and generated a cart new session.
     */
    public void invalidate() {
        data.id = UUID.randomUUID().toString();
        generateNewUUID();
        clear();
    }

    /**
     * Updates each items products in the shopping cart
     */
    public void updateProducts() {
        ProductDatabase productDatabase = project.getProductDatabase();

        if (productDatabase.isUpToDate()) {
            for (Item e : data.items) {
                Product product = productDatabase.findByCode(e.scannedCode);

                if (product != null) {
                    e.product = product;
                }
            }

            notifyProductsUpdate(this);
        }

        updatePrices(false);
    }

    /**
     * Resets the cart to the state before it was updated by the backend
     */
    public void invalidateOnlinePrices() {
        data.invalidProducts = null;
        data.invalidDepositReturnVoucher = false;
        data.onlineTotalPrice = null;

        // reverse-order because we are removing items
        for (int i = data.items.size() - 1; i >= 0; i--) {
            Item item = data.items.get(i);
            if (item.getType() == ItemType.LINE_ITEM) {
                data.items.remove(i);
            } else {
                item.lineItem = null;
                item.isManualCouponApplied = false;
            }
        }

        checkLimits();
        notifyPriceUpdate(this);
    }

    /**
     * Update all prices of the cart
     *
     * @param debounce if set to true delays the updating and batches
     *                 multiple {@link #updatePrices(boolean)} calls together
     */
    public void updatePrices(boolean debounce) {
        if (debounce) {
            updater.dispatchUpdate();
        } else {
            updater.update(true);
        }
    }

    void checkForTimeout() {
        long currentTime = System.currentTimeMillis();

        long timeout = Snabble.getInstance().getConfig().maxShoppingCartAge;

        if (data.lastModificationTime + timeout < currentTime) {
            clearBackup();
            invalidate();
        }
    }

    /**
     * Returns the number of times items in the shopping cart were added
     */

    public int getAddCount() {
        return data.addCount;
    }

    /**
     * Returns the number of times items in the shopping cart were modified
     */
    public int getModCount() {
        return data.modCount;
    }

    /**
     * Generate a new uuid.
     *
     * UUID's are used to uniquely identify a specific purchase made by the user. If a new UUID
     * is generated a new checkout can be made.
     *
     * If a checkout already exist with the same UUID, the checkout will get continued.
     */
    public void generateNewUUID() {
        data.uuid = UUID.randomUUID().toString();
        notifyProductsUpdate(this);
    }

    /**
     * The UUID of the cart
     *
     * UUID's are used to uniquely identify a specific purchase made by the user. If a new UUID
     * is generated a new checkout can be made.
     *
     * If a checkout already exist with the same UUID, the checkout will get continued.
     */
    public String getUUID() {
        return data.uuid;
    }

    void setOnlineTotalPrice(int totalPrice) {
        data.onlineTotalPrice = totalPrice;
    }

    /**
     * Returns true of the carts price is calculated by the backend
     */
    public boolean isOnlinePrice() {
        return data.onlineTotalPrice != null;
    }

    void setInvalidProducts(List<Product> invalidProducts) {
        this.data.invalidProducts = invalidProducts;
    }

    void setInvalidDepositReturnVoucher(boolean invalidDepositReturnVoucher) {
        this.data.invalidDepositReturnVoucher = invalidDepositReturnVoucher;
    }

    /**
     * Gets a list of invalid products that were rejected by the backend.
     */
    public List<Product> getInvalidProducts() {
        if (data.invalidProducts == null) {
            return Collections.emptyList();
        }

        return data.invalidProducts;
    }

    public boolean hasInvalidDepositReturnVoucher() {
        return data.invalidDepositReturnVoucher;
    }

    /**
     * Returns the total price of the cart.
     *
     * If the cart was updated by the backend, the online price is used. If no update was made
     * a locally calculated price will be used
     */
    public int getTotalPrice() {
        if (data.onlineTotalPrice != null) {
            return data.onlineTotalPrice;
        }

        int sum = 0;

        for (Item e : data.items) {
            sum += e.getTotalPrice();
        }

        sum += getTotalDepositPrice();

        return sum;
    }

    /**
     * Returns the total sum of deposit
     */
    public int getTotalDepositPrice() {
        int sum = 0;
        int vPOSsum = 0;

        for (Item e : data.items) {
            if (e.getType() == ItemType.LINE_ITEM) {
                vPOSsum += e.getTotalDepositPrice();
            } else {
                sum += e.getTotalDepositPrice();
            }
        }

        return Math.max(vPOSsum, sum);
    }

    /**
     * The quantity of items in the cart.
     */
    public int getTotalQuantity() {
        int sum = 0;

        for (Item e : data.items) {
            if (e.getType() == ItemType.LINE_ITEM) {
                if (e.lineItem.getType() == LineItemType.DEFAULT) {
                    sum += e.lineItem.getAmount();
                }
                continue;
            } else if (e.getType() == ItemType.PRODUCT) {
                Product product = e.product;
                if (product.getType() == Product.Type.UserWeighed
                        || product.getType() == Product.Type.PreWeighed
                        || product.getReferenceUnit() == PIECE) {
                    sum += 1;
                } else {
                    sum += e.quantity;
                }
            }
        }

        return sum;
    }

    /**
     * Returns true if the shopping cart is over the current set limit
     */
    public boolean hasReachedMaxCheckoutLimit() {
        return data.hasRaisedMaxCheckoutLimit;
    }

    /**
     * Returns true if the shopping cart is over the current set limit for online checkouts
     */
    public boolean hasReachedMaxOnlinePaymentLimit() {
        return data.hasRaisedMaxOnlinePaymentLimit;
    }

    private void updateTimestamp() {
        data.lastModificationTime = System.currentTimeMillis();
    }

    void checkLimits() {
        int totalPrice = getTotalPrice();
        if (totalPrice < project.getMaxCheckoutLimit()) {
            data.hasRaisedMaxCheckoutLimit = false;
        }

        if (totalPrice < project.getMaxOnlinePaymentLimit()) {
            data.hasRaisedMaxOnlinePaymentLimit = false;
        }

        if (!data.hasRaisedMaxCheckoutLimit && project.getMaxCheckoutLimit() > 0
                && totalPrice >= project.getMaxCheckoutLimit()) {
            data.hasRaisedMaxCheckoutLimit = true;
            notifyCheckoutLimitReached(this);
        } else if (!data.hasRaisedMaxOnlinePaymentLimit && project.getMaxOnlinePaymentLimit() > 0
                && totalPrice >= project.getMaxOnlinePaymentLimit()) {
            data.hasRaisedMaxOnlinePaymentLimit = true;
            notifyOnlinePaymentLimitReached(this);
        }
    }

    /**
     * Returns the current minimum age required to purchase all items of the shopping cart
     */
    public int getMinimumAge() {
        int minimumAge = 0;

        for (Item item : data.items) {
            minimumAge = Math.max(minimumAge, item.getMinimumAge());
        }

        return minimumAge;
    }

    /**
     * Checks if the provided scanned code is contained inside the shopping cart
     */
    public boolean containsScannedCode(ScannedCode scannedCode) {
        for (Item item : data.items) {
            if (item.scannedCode != null && item.scannedCode.getCode().equals(scannedCode.getCode())) {
                return true;
            }
        }

        return false;
    }

    public static class CouponItem {
        private final Coupon coupon;
        private final ScannedCode scannedCode;

        public CouponItem(Coupon coupon, ScannedCode scannedCode) {
            this.coupon = coupon;
            this.scannedCode = scannedCode;
        }

        public Coupon getCoupon() {
            return coupon;
        }

        public ScannedCode getScannedCode() {
            return scannedCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CouponItem that = (CouponItem) o;
            return Objects.equals(coupon, that.coupon) &&
                    Objects.equals(scannedCode, that.scannedCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(coupon, scannedCode);
        }
    }

    /**
     * Class describing a shopping cart item
     */
    public static class Item {
        private Product product;
        private ScannedCode scannedCode;
        private int quantity;
        private LineItem lineItem;
        private String id;
        private boolean isUsingSpecifiedQuantity;
        transient ShoppingCart cart;
        private boolean isManualCouponApplied;
        private Coupon coupon;
        // The local generated UUID of a coupon which which will be used by the backend
        private String backendCouponId;

        protected Item() {
            // for gson
        }

        private Item(ShoppingCart cart, Coupon coupon, ScannedCode scannedCode) {
            this.id = UUID.randomUUID().toString();
            this.cart = cart;
            this.scannedCode = scannedCode;
            this.coupon = coupon;
            this.backendCouponId = UUID.randomUUID().toString();
        }

        private Item(ShoppingCart cart, Product product, ScannedCode scannedCode) {
            this.id = UUID.randomUUID().toString();
            this.cart = cart;
            this.scannedCode = scannedCode;
            this.product = product;

            if (product.getType() == Product.Type.UserWeighed) {
                this.quantity = 0;
            } else {
                for (Product.Code code : product.getScannableCodes()) {
                    if (code.template != null && code.template.equals(scannedCode.getTemplateName())
                            && code.lookupCode != null && code.lookupCode.equals(scannedCode.getLookupCode())) {
                        this.quantity = code.specifiedQuantity;

                        if (!code.isPrimary && code.specifiedQuantity > 1) {
                            isUsingSpecifiedQuantity = true;
                        }
                    }
                }

                if (this.quantity == 0) {
                    this.quantity = 1;
                }
            }

            if (scannedCode.hasEmbeddedData() && product.getType() == Product.Type.DepositReturnVoucher) {
                ScannedCode.Builder builder = scannedCode.newBuilder();

                if (scannedCode.hasEmbeddedData()) {
                    builder.setEmbeddedData(scannedCode.getEmbeddedData() * -1);
                }
                if (scannedCode.hasEmbeddedDecimalData()) {
                    builder.setEmbeddedDecimalData(scannedCode.getEmbeddedDecimalData().multiply(new BigDecimal(-1)));
                }

                this.scannedCode = builder.create();
            }
        }

        private Item(ShoppingCart cart, LineItem lineItem) {
            this.id = UUID.randomUUID().toString();
            this.cart = cart;
            this.lineItem = lineItem;
        }

        /**
         * Returns the id of the shopping cart item
         */
        public String getId() {
            return id;
        }

        void setLineItem(LineItem lineItem) {
            this.lineItem = lineItem;
        }

        /**
         * Returns the product associated with the shopping cart item.
         */
        @Nullable
        public Product getProduct() {
            return product;
        }

        /**
         * Returns the scanned code which was used when scanning the product and adding it to the shopping cart
         */
        @Nullable
        public ScannedCode getScannedCode() {
            return scannedCode;
        }

        /**
         * Returns the effective quantity (embedded weight OR embedded price)
         * depending on the type
         */
        public int getEffectiveQuantity() {
            return getEffectiveQuantity(false);
        }

        private int getEffectiveQuantity(boolean ignoreLineItem) {
            return scannedCode != null
                    && scannedCode.hasEmbeddedData()
                    && scannedCode.getEmbeddedData() != 0 ? scannedCode.getEmbeddedData() : getQuantity(ignoreLineItem);
        }

        /**
         * Returns the quantity of the cart item
         */
        public int getQuantity() {
            return getQuantity(false);
        }

        /**
         * Returns the quantity of the cart item
         *
         * @param ignoreLineItem if set to true, only return the local quantity before backend updates
         */
        public int getQuantity(boolean ignoreLineItem) {
            if (lineItem != null && !ignoreLineItem) {
                if (lineItem.getWeight() != null) {
                    return lineItem.getWeight();
                } else if (lineItem.getUnits() != null) {
                    return lineItem.getUnits();
                } else {
                    return lineItem.getAmount();
                }
            }

            return quantity;
        }

        /**
         * Set the quantity of the cart item
         */
        public void setQuantity(int quantity) {
            if (scannedCode.hasEmbeddedData() && scannedCode.getEmbeddedData() != 0) {
                return;
            }

            this.quantity = Math.max(0, Math.min(MAX_QUANTITY, quantity));

            int index = cart.data.items.indexOf(this);
            if (index != -1) {
                if (quantity == 0) {
                    cart.data.items.remove(this);
                    cart.notifyItemRemoved(cart, this, index);
                } else {
                    cart.notifyQuantityChanged(cart, this);
                }

                cart.data.modCount++;
                cart.generateNewUUID();
                cart.invalidateOnlinePrices();
                cart.updatePrices(true);
            }
        }

        /**
         * Returns the {@link ItemType} of the cart item
         */
        public ItemType getType() {
            if (product != null) {
                return ItemType.PRODUCT;
            } else if (coupon != null) {
                return ItemType.COUPON;
            } else if (lineItem != null) {
                return ItemType.LINE_ITEM;
            }

            return null;
        }

        /**
         * Sets if a manual coupon (coupon applied by the user after scanning) is applied
         */
        public void setManualCouponApplied(boolean manualCouponApplied) {
            isManualCouponApplied = manualCouponApplied;
        }

        /**
         * Returns true if a manual coupon (coupon applied by the user after scanning) is applied
         */
        public boolean isManualCouponApplied() {
            return isManualCouponApplied;
        }

        /**
         * Returns true if the item is editable by the user
         */
        public boolean isEditable() {
            if (coupon != null && coupon.getType() != CouponType.MANUAL) {
                return false;
            }

            return isEditableInDialog();
        }

        /**
         * Returns true if the item is editable by the user while scanning
         */
        public boolean isEditableInDialog() {
            if (lineItem != null) return lineItem.getType() == LineItemType.DEFAULT
                    && (!scannedCode.hasEmbeddedData() || scannedCode.getEmbeddedData() == 0);

            return (!scannedCode.hasEmbeddedData() || scannedCode.getEmbeddedData() == 0) &&
                    product.getPrice(cart.project.getCustomerCardId()) != 0;
        }

        /**
         * Returns true if the item can be merged with items that conain the same product and type
         */
        public boolean isMergeable() {
            if (product == null && lineItem != null) return false;
            if (coupon != null) return false;

            boolean b = product.getType() == Product.Type.Article
                    && getUnit() != PIECE
                    && product.getPrice(cart.project.getCustomerCardId()) != 0
                    && scannedCode.getEmbeddedData() == 0
                    && !isUsingSpecifiedQuantity;
            return b;
        }

        /**
         * Returns the unit associated with the cart item, or null if the cart item has no unit
         */
        @Nullable
        public Unit getUnit() {
            if (getType() == ItemType.PRODUCT) {
                return scannedCode.getEmbeddedUnit() != null ? scannedCode.getEmbeddedUnit()
                        : product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());
            } else if (getType() == ItemType.LINE_ITEM) {
                if (lineItem.getWeightUnit() != null) {
                    return Unit.fromString(lineItem.getWeightUnit());
                }
            }

            return null;
        }

        /**
         * Gets the total price of the item
         */
        public int getTotalPrice() {
            if (lineItem != null) {
                return lineItem.getTotalPrice();
            }

            return getLocalTotalPrice();
        }

        /**
         * Gets the total price of the items, ignoring the backend response
         */
        public int getLocalTotalPrice() {
            if (getType() == ItemType.PRODUCT) {
                if (getUnit() == Unit.PRICE) {
                    return scannedCode.getEmbeddedData();
                }

                int price = product.getPriceForQuantity(getEffectiveQuantity(),
                        scannedCode,
                        cart.project.getRoundingMode(),
                        cart.project.getCustomerCardId());

                return price;
            } else {
                return 0;
            }
        }

        /**
         * Gets the total deposit of the item
         */
        public int getTotalDepositPrice() {
            if (lineItem != null && lineItem.getType() == LineItemType.DEPOSIT) {
                return lineItem.getTotalPrice();
            }

            if (product != null && product.getDepositProduct() != null) {
                return quantity * product.getDepositProduct().getPrice(cart.project.getCustomerCardId());
            }

            return 0;
        }

        /**
         * Returns true if the item should be displayed as a discount
         */
        public boolean isDiscount() {
            return lineItem != null && lineItem.getType() == LineItemType.DISCOUNT;
        }

        /**
         * Returns true if the item should be displayed as a giveaway
         */
        public boolean isGiveaway() {
            return lineItem != null && lineItem.getType() == LineItemType.GIVEAWAY;
        }

        /**
         * Gets the price after applying all price modifiers
         */
        public int getModifiedPrice() {
            int sum = 0;

            if (lineItem != null && lineItem.getPriceModifiers() != null) {
                for (PriceModifier priceModifier : lineItem.getPriceModifiers()) {
                    sum += lineItem.getAmount() * priceModifier.getPrice();
                }
            }

            return sum;
        }

        /**
         * Gets the displayed name of the product
         */
        public String getDisplayName() {
            if (lineItem != null) {
                return lineItem.getName();
            } else {
                if (getType() == ItemType.COUPON) {
                    return coupon.getName();
                } else {
                    return product.getName();
                }
            }
        }

        /**
         * Gets text displaying quantity, can be a weight or price depending on the type
         *
         * E.g. "1" or "100g" or "2,03 €"
         */
        public String getQuantityText() {
            if (getType() == ItemType.LINE_ITEM) {
                return String.valueOf(lineItem.getAmount());
            }

            Unit unit = getUnit();
            if (unit == PRICE || (unit == PIECE && scannedCode.getEmbeddedData() > 0)) {
                if (lineItem != null && lineItem.getUnits() != null) {
                    return String.valueOf(lineItem.getUnits());
                } else {
                    return "1";
                }
            }

            int q = getEffectiveQuantity();
            if (q > 0) {
                return q + (unit != null ? unit.getDisplayValue() : "");
            } else {
                return "1";
            }
        }

        /**
         * Gets text displaying price, including the calculation.
         *
         * E.g. "2 * 3,99 € = 7,98 €"
         */
        public String getFullPriceText() {
            String priceText = getPriceText();
            if (priceText != null) {
                String quantityText = getQuantityText();
                if (quantityText.equals("1")) {
                    return getPriceText();
                } else {
                    return quantityText + " " + getPriceText();
                }
            }

            return null;
        }

        private String getReducedPriceText() {
            if (lineItem.getPriceModifiers() != null && lineItem.getPriceModifiers().size() > 0) {
                return cart.priceFormatter.format(getTotalPrice(), true);
            }

            return null;
        }

        private String getExtendedPriceText() {
            String reducedPriceText = getReducedPriceText();
            if (reducedPriceText != null) {
                return getReducedPriceText();
            } else {
                return String.format("\u00D7 %s = %s",
                        cart.priceFormatter.format(product, lineItem.getPrice()),
                        cart.priceFormatter.format(getTotalPrice()));
            }
        }

        /**
         * Gets the displayed total price
         */
        public String getTotalPriceText() {
            return cart.priceFormatter.format(getTotalPrice());
        }

        /**
         * Gets text displaying price, including the calculation.
         *
         * E.g. "3,99 €" or "2,99 € /kg = 0,47 €"
         */
        public String getPriceText() {
            if (lineItem != null) {
                if (lineItem.getPrice() != 0) {
                    if (product != null && lineItem.getUnits() != null && lineItem.getUnits() > 1
                            || (getUnit() != Unit.PRICE
                            && (getUnit() != PIECE || scannedCode.getEmbeddedData() == 0)
                            && getEffectiveQuantity() > 1)) {
                        return getExtendedPriceText();
                    } else {
                        if (lineItem.getUnits() != null) {
                            return getExtendedPriceText();
                        } else {
                            String reducedPriceText = getReducedPriceText();
                            if (reducedPriceText != null) {
                                return reducedPriceText;
                            } else {
                                return cart.priceFormatter.format(getTotalPrice(), true);
                            }
                        }
                    }
                }
            }

            if (product == null) {
                return null;
            }

            if (product.getPrice(cart.project.getCustomerCardId()) > 0 || scannedCode.hasEmbeddedData()) {
                Unit unit = getUnit();

                if ((unit == Unit.PRICE || unit == PIECE) && !(scannedCode.hasEmbeddedData() && scannedCode.getEmbeddedData() == 0)) {
                    return cart.priceFormatter.format(getTotalPrice());
                } else if (getEffectiveQuantity() <= 1) {
                    return cart.priceFormatter.format(product);
                } else {
                    return String.format("\u00D7 %s = %s",
                            cart.priceFormatter.format(product),
                            cart.priceFormatter.format(getTotalPrice()));
                }
            }

            return null;
        }

        /**
         * Gets the minimum age required to purchase this item
         */
        public int getMinimumAge() {
            if (product != null) {
                Product.SaleRestriction saleRestriction = product.getSaleRestriction();
                if (saleRestriction != null && saleRestriction.isAgeRestriction()) {
                    return (int) saleRestriction.getValue();
                }
            }

            return 0;
        }

        /**
         * Associate a user applied coupon with this item. E.g. manual price reductions.
         */
        public void setCoupon(Coupon coupon) {
            if (coupon == null) {
                this.coupon = null;
                return;
            }

            if (coupon.getType() != CouponType.MANUAL) {
                throw new RuntimeException("Only manual coupons can be added");
            }

            this.coupon = coupon;
        }

        /**
         * Gets the user associated coupon of this item
         */
        public Coupon getCoupon() {
            return this.coupon;
        }

        void replace(Product product, ScannedCode scannedCode, int quantity) {
            this.product = product;
            this.scannedCode = scannedCode;
            this.quantity = quantity;
        }
    }

    public List<PaymentMethodInfo> getAvailablePaymentMethods() {
        return updater.getLastAvailablePaymentMethods();
    }

    public boolean isVerifiedOnline() {
        return updater.isUpdated();
    }

    public String toJson() {
        return GsonHolder.get().toJson(this);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class BackendCart implements Events.Payload {
        public String session;
        @SerializedName("shopID")
        public String shopId;
        @SerializedName("clientID")
        public String clientId;
        @SerializedName("appUserID")
        public String appUserId;
        public BackendCartCustomer customer;
        public BackendCartItem[] items;
        public List<BackendCartRequiredInformation> requiredInformation;

        @Override
        public Events.EventType getEventType() {
            return Events.EventType.CART;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class BackendCartCustomer {
        String loyaltyCard;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class BackendCartRequiredInformation {
        String id;
        String value;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class BackendCartItem {
        String id;
        String sku;
        String scannedCode;
        int amount;
        String weightUnit;
        Integer price;
        Integer weight;
        Integer units;
        String refersTo;
        String couponID;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public BackendCart toBackendCart() {
        BackendCart backendCart = new BackendCart();
        backendCart.session = getId();
        backendCart.shopId = "unknown";

        UserPreferences userPreferences = Snabble.getInstance().getUserPreferences();
        backendCart.clientId = userPreferences.getClientId();

        AppUser appUser = userPreferences.getAppUser();
        if (appUser != null) {
            backendCart.appUserId = appUser.id;
        }

        String loyaltyCardId = project.getCustomerCardId();
        if (loyaltyCardId != null) {
            backendCart.customer = new BackendCartCustomer();
            backendCart.customer.loyaltyCard = loyaltyCardId;
        }

        if (backendCart.requiredInformation == null) {
            backendCart.requiredInformation = new ArrayList<>();
        }

        if (data.taxation != Taxation.UNDECIDED) {
            BackendCartRequiredInformation requiredInformation = new BackendCartRequiredInformation();
            requiredInformation.id = "taxation";
            requiredInformation.value = data.taxation.getValue();
            backendCart.requiredInformation.add(requiredInformation);
        }

        Shop shop = Snabble.getInstance().getCheckedInShop();
        if (shop != null) {
            String id = shop.getId();
            if (id != null) {
                backendCart.shopId = id;
            }
        }

        List<BackendCartItem> items = new ArrayList<>();

        for (int i = 0; i < size(); i++) {
            ShoppingCart.Item cartItem = get(i);
            if (cartItem.getType() == ItemType.PRODUCT) {
                BackendCartItem item = new BackendCartItem();

                Product product = cartItem.getProduct();
                int quantity = cartItem.getQuantity();

                ScannedCode scannedCode = cartItem.getScannedCode();
                Unit encodingUnit = product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());

                if (scannedCode.getEmbeddedUnit() != null) {
                    encodingUnit = scannedCode.getEmbeddedUnit();
                }

                item.id = cartItem.id;
                item.sku = String.valueOf(product.getSku());
                item.scannedCode = scannedCode.getCode();

                if (product.getPrimaryCode() != null) {
                    item.scannedCode = product.getPrimaryCode().lookupCode;
                }

                if (encodingUnit != null) {
                    item.weightUnit = encodingUnit.getId();
                }

                item.amount = 1;

                if (cartItem.getUnit() == Unit.PIECE) {
                    item.units = cartItem.getEffectiveQuantity(true);
                } else if (cartItem.getUnit() == Unit.PRICE) {
                    item.price = cartItem.getLocalTotalPrice();
                } else if (cartItem.getUnit() != null) {
                    item.weight = cartItem.getEffectiveQuantity(true);
                } else if (product.getType() == Product.Type.UserWeighed) {
                    item.weight = quantity;
                } else {
                    item.amount = quantity;
                }

                if (item.price == null && scannedCode.hasPrice()) {
                    item.price = scannedCode.getPrice();
                }

                // reencode user input from scanned code with 0 amount
                if (cartItem.getUnit() == Unit.PIECE && scannedCode.getEmbeddedData() == 0) {
                    CodeTemplate codeTemplate = project.getCodeTemplate(scannedCode.getTemplateName());

                    if (codeTemplate != null) {
                        ScannedCode newCode = codeTemplate.code(scannedCode.getLookupCode())
                                .embed(cartItem.getEffectiveQuantity())
                                .buildCode();

                        if (newCode != null) {
                            item.scannedCode = newCode.getCode();
                        }
                    }
                }

                items.add(item);

                if (cartItem.coupon != null) {
                    BackendCartItem couponItem = new BackendCartItem();
                    couponItem.id = cartItem.coupon.getId();
                    couponItem.refersTo = item.id;
                    couponItem.amount = 1;
                    couponItem.couponID = cartItem.coupon.getId();
                    items.add(couponItem);
                }
            } else if (cartItem.getType() == ItemType.COUPON) {
                BackendCartItem item = new BackendCartItem();
                item.id = cartItem.backendCouponId;
                item.amount = 1;

                ScannedCode scannedCode = cartItem.getScannedCode();
                if (scannedCode != null) {
                    item.scannedCode = scannedCode.getCode();
                }

                item.couponID = cartItem.coupon.getId();
                items.add(item);
            }
        }

        backendCart.items = items.toArray(new BackendCartItem[0]);

        return backendCart;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    void resolveViolations(List<Violation> violations) {
        for (Violation violation : violations) {
            for (int i = data.items.size() - 1; i >= 0; i--) {
                if (data.items.get(i).coupon != null && data.items.get(i).backendCouponId.equals(violation.getRefersTo())) {
                    Item item = data.items.get(i);
                    data.items.remove(item);
                    boolean found = false;
                    for (ViolationNotification notification : data.violationNotifications) {
                        if (notification.getRefersTo().equals(violation.getRefersTo())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        data.violationNotifications.add(new ViolationNotification(
                                item.coupon.getName(),
                                violation.getRefersTo(),
                                violation.getType(),
                                violation.getMessage()
                        ));
                    }
                }
            }
        }
        notifyViolations();
    }

    /**
     * Remove the handled ViolationNotifications.
     *
     * @param violations the handled ViolationNotifications.
     */
    public void removeViolationNotification(List<ViolationNotification> violations) {
        data.violationNotifications.removeAll(violations);
    }

    @NonNull
    public List<ViolationNotification> getViolationNotifications() {
        return data.violationNotifications;
    }

    /**
     * Adds a {@link ShoppingCartListener} to the list of listeners if it does not already exist.
     *
     * @param listener the listener to addNamedOnly
     */
    public void addListener(ShoppingCartListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        if (!data.violationNotifications.isEmpty()) {
            listener.onViolationDetected(data.violationNotifications);
        }
    }

    /**
     * Removes a given {@link ShoppingCartListener} from the list of listeners.
     *
     * @param listener the listener to remove
     */
    public void removeListener(ShoppingCartListener listener) {
        listeners.remove(listener);
    }

    /**
     * Shopping list listener that detects various changes to the shopping list.
     */
    public interface ShoppingCartListener {
        void onItemAdded(ShoppingCart list, Item item);

        void onQuantityChanged(ShoppingCart list, Item item);

        void onCleared(ShoppingCart list);

        void onItemRemoved(ShoppingCart list, Item item, int pos);

        void onProductsUpdated(ShoppingCart list);

        void onPricesUpdated(ShoppingCart list);

        void onCheckoutLimitReached(ShoppingCart list);

        void onOnlinePaymentLimitReached(ShoppingCart list);

        void onTaxationChanged(ShoppingCart list, Taxation taxation);

        void onViolationDetected(@NonNull List<ViolationNotification> violations);

        void onShopChanged(ShoppingCart list);
    }

    public static abstract class SimpleShoppingCartListener implements ShoppingCartListener {
        public abstract void onChanged(ShoppingCart list);

        @Override
        public void onProductsUpdated(ShoppingCart list) {
            onChanged(list);
        }

        @Override
        public void onItemAdded(ShoppingCart list, Item item) {
            onChanged(list);
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, Item item) {
            onChanged(list);
        }

        @Override
        public void onCleared(ShoppingCart list) {
            onChanged(list);
        }

        @Override
        public void onItemRemoved(ShoppingCart list, Item item, int pos) {
            onChanged(list);
        }

        @Override
        public void onPricesUpdated(ShoppingCart list) {
            onChanged(list);
        }

        @Override
        public void onTaxationChanged(ShoppingCart list, Taxation taxation) {
            onChanged(list);
        }

        @Override
        public void onCheckoutLimitReached(ShoppingCart list) {}

        @Override
        public void onOnlinePaymentLimitReached(ShoppingCart list) {}

        @Override
        public void onViolationDetected(@NonNull List<ViolationNotification> violations) {}

        @Override
        public void onShopChanged(ShoppingCart list) {}
    }

    private void notifyItemAdded(final ShoppingCart list, final Item item) {
        updateTimestamp();

        Dispatch.mainThread(() -> {
            if (list.data.items.contains(item)) {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemAdded(list, item);
                }
            }
        });
    }

    private void notifyItemRemoved(final ShoppingCart list, final Item item, final int pos) {
        updateTimestamp();

        Dispatch.mainThread(() -> {
            if (list.data.items.contains(item)) {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemRemoved(list, item, pos);
                }
            }
        });
    }

    private void notifyQuantityChanged(final ShoppingCart list, final Item item) {
        updateTimestamp();

        Dispatch.mainThread(() -> {
            if (list.data.items.contains(item)) {
                for (ShoppingCartListener listener : listeners) {
                    listener.onQuantityChanged(list, item);
                }
            }
        });
    }

    private void notifyProductsUpdate(final ShoppingCart list) {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onProductsUpdated(list);
            }
        });
    }

    void notifyPriceUpdate(final ShoppingCart list) {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onPricesUpdated(list);
            }
        });
    }

    void notifyTaxationChanged(final ShoppingCart list, final Taxation taxation) {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onTaxationChanged(list, taxation);
            }
        });
    }

    void notifyCheckoutLimitReached(final ShoppingCart list) {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onCheckoutLimitReached(list);
            }
        });
    }

    void notifyOnlinePaymentLimitReached(final ShoppingCart list) {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onOnlinePaymentLimitReached(list);
            }
        });
    }

    void notifyViolations() {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onViolationDetected(data.violationNotifications);
            }
        });
    }

    private void notifyShopChanged(final ShoppingCart list) {
        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onShopChanged(list);
            }
        });
    }

    /**
     * Notifies all {@link #listeners} that the shopping list was cleared of all entries.
     *
     * @param list the {@link ShoppingCart}
     */
    private void notifyCleared(final ShoppingCart list) {
        updateTimestamp();

        Dispatch.mainThread(() -> {
            for (ShoppingCartListener listener : listeners) {
                listener.onCleared(list);
            }
        });
    }
}