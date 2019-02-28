package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.codes.ScannedCode;

import static io.snabble.sdk.Unit.PIECE;
import static io.snabble.sdk.Unit.PRICE;

public class ShoppingCart {
    public static final int MAX_QUANTITY = 99999;
    public static final long TIMEOUT = TimeUnit.HOURS.toMillis(4);

    private final transient Object lock = new Object();

    private String id;
    private long lastModificationTime;
    private List<Item> items = new ArrayList<>();
    private int modCount = 0;
    private int addCount = 0;
    private transient List<ShoppingCartListener> listeners = new CopyOnWriteArrayList<>();
    private transient Handler handler = new Handler(Looper.getMainLooper());
    private transient Project project;
    private transient ShoppingCartUpdater updater;
    private transient PriceFormatter priceFormatter;

    protected ShoppingCart() {
        // for gson
    }

    ShoppingCart(Project project) {
        id = UUID.randomUUID().toString();
        updateTimestamp();

        initWithProject(project);
    }

    void initWithProject(Project project) {
        this.project = project;
        this.updater = new ShoppingCartUpdater(project, this);
        this.priceFormatter = new PriceFormatter(project);

        checkForTimeout();

        for (Item item : items) {
            item.cart = this;
        }
    }

    public String getId() {
        return id;
    }

    public Item newItem(Product product, ScannedCode scannedCode) {
        return new Item(this, product, scannedCode);
    }

    public Item newItem(CheckoutApi.LineItem lineItem) {
        return new Item(this, lineItem);
    }

    public void add(Item item) {
        insert(item, 0);
    }

    public void insert(Item item, int index) {
        if (item.isMergeable()) {
            Item existing = getByProduct(item.getProduct());
            if (existing != null) {
                items.remove(existing);
                items.add(index, item);
                modCount++;
                notifyQuantityChanged(this, item);
                return;
            }
        }

        addCount++;
        modCount++;
        items.add(index, item);
        notifyItemAdded(this, item);
    }

    public Item get(int index) {
        return items.get(index);
    }

    public Item getByProduct(Product product) {
        if (product == null) {
            return null;
        }

        for (Item item : items) {
            if (product.equals(item.product)) {
                return item;
            }
        }

        return null;
    }

    public Item getByItemId(String itemId) {
        if (itemId == null) {
            return null;
        }

        for (Item item : items) {
            if (itemId.equals(item.id)) {
                return item;
            }
        }

        return null;
    }

    public int indexOf(Item item) {
        return items.indexOf(item);
    }

    public void remove(int index) {
        modCount++;
        notifyItemRemoved(this, items.remove(index), index);
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
        modCount = 0;
        addCount = 0;
        notifyCleared(this);
    }

    public void invalidate() {
        id = UUID.randomUUID().toString();
        clear();
    }

    public void updateProducts() {
        ProductDatabase productDatabase = project.getProductDatabase();

        if (productDatabase.isUpToDate()) {
            for (Item e : items) {
                Product product = productDatabase.findByCode(e.scannedCode);

                if (product != null) {
                    e.product = product;
                }
            }

            notifyProductsUpdate(this);
        }
    }

    public void updatePrices(boolean debounce) {
        // reverse-order because we are removing items
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item.isLineItem()) {
                items.remove(i);
                notifyItemRemoved(this, item, i);
            } else {
                item.lineItem = null; // TODO ugh...
                notifyQuantityChanged(this, item);
            }
        }

        if(debounce) {
            updater.dispatchUpdate();
        } else {
            updater.update();
        }
    }

    public void checkForTimeout() {
        long currentTime = System.currentTimeMillis();

        if (lastModificationTime + TIMEOUT < currentTime) {
            invalidate();
        }
    }

    public int getAddCount() {
        return addCount;
    }

    public int getModCount() {
        return modCount;
    }

    public int getTotalPrice() {
        synchronized (lock) {
            int sum = 0;

            for (Item e : items) {
                sum += e.getTotalPrice();
            }

            sum += getTotalDepositPrice();

            return sum;
        }
    }

    public int getTotalDepositPrice() {
        synchronized (lock) {
            int sum = 0;

            for (Item e : items) {
                sum += e.getTotalDepositPrice();
            }

            return sum;
        }
    }

    public int getTotalQuantity() {
        synchronized (lock) {
            int sum = 0;

            for (Item e : items) {
                if (e.isLineItem()) {
                    sum += e.lineItem.amount;
                    continue;
                }

                Product product = e.product;
                if (product.getType() == Product.Type.UserWeighed
                        || product.getType() == Product.Type.PreWeighed
                        || product.getReferenceUnit() == PIECE) {
                    sum += 1;
                } else {
                    sum += e.quantity;
                }
            }

            return sum;
        }
    }

    private void updateTimestamp() {
        lastModificationTime = System.currentTimeMillis();
    }

    public static class Item {
        private Product product;
        private ScannedCode scannedCode;
        private int quantity;
        private CheckoutApi.LineItem lineItem;
        private String id;
        private transient ShoppingCart cart;

        protected Item() {
            // for gson
        }

        private Item(ShoppingCart cart, Product product, ScannedCode scannedCode) {
            this.id = UUID.randomUUID().toString();
            this.cart = cart;
            this.scannedCode = scannedCode;
            this.product = product;

            if (product.getType() == Product.Type.UserWeighed) {
                this.quantity = 0;
            } else {
                this.quantity = 1;
            }
        }

        private Item(ShoppingCart cart, CheckoutApi.LineItem lineItem) {
            this.id = UUID.randomUUID().toString();
            this.cart = cart;
            this.lineItem = lineItem;
        }

        public String getId() {
            return id;
        }

        void setLineItem(CheckoutApi.LineItem lineItem) {
            this.lineItem = lineItem;
            cart.notifyQuantityChanged(cart, this);
        }

        public CheckoutApi.LineItem getLineItem() {
            return lineItem;
        }

        public Product getProduct() {
            return product;
        }

        public ScannedCode getScannedCode() {
            return scannedCode;
        }

        public int getEffectiveQuantity() {
            return scannedCode.hasEmbeddedData() && scannedCode.getEmbeddedData() != 0 ? scannedCode.getEmbeddedData() : quantity;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            if (scannedCode.hasEmbeddedData() && scannedCode.getEmbeddedData() != 0) {
                return;
            }

            this.quantity = Math.max(0, Math.min(MAX_QUANTITY, quantity));

            int index = cart.items.indexOf(this);
            if (index != -1) {
                if (quantity == 0) {
                    cart.items.remove(this);
                    cart.notifyItemRemoved(cart, this, index);
                } else {
                    cart.notifyQuantityChanged(cart, this);
                }

                cart.modCount++;
                cart.updatePrices(true);
            }
        }

        public boolean isLineItem() {
            return product == null && lineItem != null;
        }

        public boolean isEditable() {
            if (isLineItem()) return false;

            return !scannedCode.hasEmbeddedData() || scannedCode.getEmbeddedData() == 0;
        }

        public boolean isMergeable() {
            if (isLineItem()) return false;

            return product.getType() == Product.Type.Article
                    && getUnit() != PIECE
                    && product.getDiscountedPrice() != 0;
        }

        public Unit getUnit() {
            if (isLineItem()) return null;

            return scannedCode.getEmbeddedUnit() != null ? scannedCode.getEmbeddedUnit()
                    : product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());
        }

        public int getTotalPrice() {
            if (lineItem != null) {
                return lineItem.totalPrice;
            }

            if (getUnit() == Unit.PRICE) {
                return scannedCode.getEmbeddedData();
            }

            return product.getPriceForQuantity(getEffectiveQuantity(), scannedCode, cart.project.getRoundingMode());
        }

        public int getTotalDepositPrice() {
            if (isLineItem()) return 0;

            if (product.getDepositProduct() != null) {
                return quantity * product.getDepositProduct().getDiscountedPrice();
            }

            return 0;
        }

        public String getDisplayName() {
            if (isLineItem()) {
                return lineItem.name;
            } else {
                return product.getName();
            }
        }

        public String getQuantityText() {
            if (isLineItem()) {
                return String.valueOf(lineItem.amount);
            }

            Unit unit = getUnit();
            if (unit == PRICE) {
                return "1";
            }

            int q = getEffectiveQuantity();
            if (q > 0) {
                return String.valueOf(q) + (unit != null ? unit.getDisplayValue() : "");
            } else {
                return "1";
            }
        }

        public String getFullPriceText() {
            String priceText = getPriceText();
            if (priceText != null) {
                String quantityText = getQuantityText();
                if (quantityText.equals("1")) {
                    return getPriceText().trim();
                } else {
                    return quantityText + getPriceText();
                }
            }

            return null;
        }

        public String getPriceText() {
            if (lineItem != null) {
                if (lineItem.amount > 1) {
                    return String.format(" * %s = %s",
                            cart.priceFormatter.format(lineItem.price),
                            cart.priceFormatter.format(getTotalPrice()));
                } else {
                    return " " + cart.priceFormatter.format(getTotalPrice(), true);
                }
            }

            if (product.getDiscountedPrice() > 0 || (scannedCode.hasEmbeddedData() && scannedCode.getEmbeddedData() > 0)) {
                Unit unit = getUnit();

                if (unit == Unit.PRICE) {
                    return " " + cart.priceFormatter.format(getTotalPrice());
                } else if (getEffectiveQuantity() <= 1) {
                    return " " + cart.priceFormatter.format(product) + "*";
                } else {
                    return String.format(" * %s = %s*",
                            cart.priceFormatter.format(product),
                            cart.priceFormatter.format(getTotalPrice()));
                }
            }

            return null;
        }
    }

    public BackendCart toBackendCart() {
        ShoppingCart shoppingCart = project.getShoppingCart();

        BackendCart backendCart = new BackendCart();
        backendCart.session = shoppingCart.getId();
        backendCart.shopId = "unknown";

        String loyaltyCardId = project.getCustomerCardId();
        if (loyaltyCardId != null) {
            backendCart.customer = new BackendCartCustomer();
            backendCart.customer.loyaltyCard = loyaltyCardId;
        }

        Shop shop = project.getCheckedInShop();
        if (shop != null) {
            String id = shop.getId();
            if (id != null) {
                backendCart.shopId = id;
            }
        }

        List<BackendCartItem> items = new ArrayList<>();

        for (int i = 0; i < shoppingCart.size(); i++) {
            ShoppingCart.Item cartItem = shoppingCart.get(i);
            if (cartItem.isLineItem()) continue;

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

            if (encodingUnit != null) {
                item.weightUnit = encodingUnit.getId();
            }

            item.amount = quantity;

            if (cartItem.getUnit() == Unit.PIECE) {
                item.units = cartItem.getEffectiveQuantity();
            } else if (cartItem.getUnit() == Unit.PRICE) {
                item.price = cartItem.getTotalPrice();
            } else if (cartItem.getUnit() != null) {
                item.weight = cartItem.getEffectiveQuantity();
            }

            if (product.getType() == Product.Type.UserWeighed) {
                item.amount = 1;
                item.weight = quantity;
            }

            if (item.units == null && product.getReferenceUnit() == Unit.PIECE) {
                item.amount = 1;
                item.units = quantity;
            }

            if (item.price == null && item.units != null && scannedCode.hasPrice()) {
                item.price = item.units * scannedCode.getPrice();
            }

            items.add(item);
        }

        backendCart.items = items.toArray(new BackendCartItem[items.size()]);

        return backendCart;
    }

    public static class BackendCart implements Events.Payload {
        private String session;
        @SerializedName("shopID")
        private String shopId;
        private BackendCartCustomer customer;
        private BackendCartItem[] items;

        @Override
        public Events.EventType getEventType() {
            return Events.EventType.CART;
        }
    }

    public static class BackendCartCustomer {
        private String loyaltyCard;
    }

    public static class BackendCartItem {
        private String id;
        private String sku;
        private String scannedCode;
        private int amount;
        private String weightUnit;
        private Integer price;
        private Integer weight;
        private Integer units;
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
    }

    private void notifyItemAdded(final ShoppingCart list, final Item item) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemAdded(list, item);
                }
            }
        });
    }

    private void notifyItemRemoved(final ShoppingCart list, final Item item, final int pos) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemRemoved(list, item, pos);
                }
            }
        });
    }

    private void notifyQuantityChanged(final ShoppingCart list, final Item item) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onQuantityChanged(list, item);
                }
            }
        });
    }

    private void notifyProductsUpdate(final ShoppingCart list) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onProductsUpdated(list);
                }
            }
        });
    }

    void notifyPriceUpdate(final ShoppingCart list) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onPricesUpdated(list);
                }
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

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onCleared(list);
                }
            }
        });
    }
}