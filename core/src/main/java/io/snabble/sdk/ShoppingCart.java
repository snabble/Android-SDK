package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collection;
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
        for (Item item : items) {
            if (item.product.equals(product)) {
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

    public void update() {
        ProductDatabase productDatabase = project.getProductDatabase();

        if (productDatabase.isUpToDate()) {
            for (Item e : items) {
                Product product = productDatabase.findByCode(e.scannedCode);

                if (product != null) {
                    e.product = product;
                }
            }

            notifyUpdate(this);
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
                e.getTotalDepositPrice();
            }

            return sum;
        }
    }

    public int getTotalQuantity() {
        synchronized (lock) {
            int sum = 0;

            for (Item e : items) {
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
        private transient ShoppingCart cart;

        protected Item() {
            // for gson
        }

        private Item(ShoppingCart cart, Product product, ScannedCode scannedCode) {
            this.cart = cart;
            this.scannedCode = scannedCode;
            this.product = product;

            if (product.getType() == Product.Type.UserWeighed) {
                this.quantity = 0;
            } else {
                this.quantity = 1;
            }
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
            }
        }

        public boolean isEditable() {
            return !scannedCode.hasEmbeddedData() || scannedCode.getEmbeddedData() == 0;
        }

        public boolean isMergeable() {
            return product.getType() == Product.Type.Article
                    && getUnit() != PIECE
                    && product.getDiscountedPrice() != 0;
        }

        public Unit getUnit() {
            return scannedCode.getEmbeddedUnit() != null ? scannedCode.getEmbeddedUnit()
                    : product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());
        }

        public int getTotalPrice() {
            if (getUnit() == Unit.PRICE) {
                return scannedCode.getEmbeddedData();
            }

            return product.getPriceForQuantity(getEffectiveQuantity(), scannedCode, cart.project.getRoundingMode());
        }

        public int getTotalDepositPrice() {
            if (product.getDepositProduct() != null) {
                return quantity * product.getDepositProduct().getDiscountedPrice();
            }

            return 0;
        }

        public String getQuantityText() {
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
            if (product.getDiscountedPrice() > 0) {
                PriceFormatter priceFormatter = new PriceFormatter(cart.project);
                Unit unit = getUnit();

                if (unit == Unit.PRICE) {
                    return " " + priceFormatter.format(getTotalPrice());
                } else if (getEffectiveQuantity() <= 1) {
                    return " " + priceFormatter.format(product);
                } else {
                    return String.format(" * %s = %s",
                            priceFormatter.format(product),
                            priceFormatter.format(getTotalPrice()));
                }
            }

            return null;
        }
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

        void onUpdate(ShoppingCart list);
    }

    public static abstract class SimpleShoppingCartListener implements ShoppingCartListener {
        public abstract void onChanged(ShoppingCart list);

        @Override
        public void onUpdate(ShoppingCart list) {
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

    private void notifyUpdate(final ShoppingCart list) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onUpdate(list);
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