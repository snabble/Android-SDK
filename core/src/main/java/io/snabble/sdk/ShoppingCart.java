package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.codes.ScannableCode;

public class ShoppingCart {
    public static final int MAX_QUANTITY = 99999;
    public static final long TIMEOUT = TimeUnit.HOURS.toMillis(4);

    private static class Entry {
        private Product product;
        private final String sku;
        private String scannedCode;
        private int quantity;

        private Integer weight = null;
        private Integer price = null;
        private Integer amount = null;

        private Entry(Product product, int quantity, ScannableCode scannedCode) {
            this.sku = product.getSku();
            this.product = product;
            this.quantity = quantity;

            setScannedCode(scannedCode);
        }

        public void setScannedCode(ScannableCode scannedCode){
            this.scannedCode = scannedCode.getCode();

            if(scannedCode.hasWeighData()){
                weight = scannedCode.getEmbeddedData();
            } else if(scannedCode.hasPriceData()){
                price = scannedCode.getEmbeddedData();
            } else if(scannedCode.hasAmountData()){
                amount = scannedCode.getEmbeddedData();
            }
        }
    }

    private final transient Object lock = new Object();

    private String id;
    private long lastModificationTime;
    private List<Entry> items = new ArrayList<>();
    private transient List<ShoppingCartListener> listeners = new CopyOnWriteArrayList<>();
    private transient Handler handler = new Handler(Looper.getMainLooper());
    private transient SnabbleSdk sdkInstance;

    protected ShoppingCart() {
        //empty constructor for gson
    }

    ShoppingCart(SnabbleSdk sdkInstance) {
        id = UUID.randomUUID().toString();
        updateTimestamp();

        initWithSdkInstance(sdkInstance);
    }

    void initWithSdkInstance(SnabbleSdk sdkInstance) {
        this.sdkInstance = sdkInstance;
        checkForTimeout();
    }

    public String getId() {
        return id;
    }

    public void add(Product product) {
        add(product, 1);
    }

    public void add(Product product, ScannableCode scannedCode) {
        add(product, 1, scannedCode);
    }

    public void add(Product product, int quantity) {
        insert(product, items.size(), quantity);
    }

    public void add(Product product, int quantity, ScannableCode scannedCode) {
        insert(product, items.size(), quantity, scannedCode);
    }

    public void insert(Product product, int index) {
        insert(product, index, 1);
    }

    public void insert(Product product, int index, ScannableCode scannedCode) {
        insert(product, index, 1, scannedCode);
    }

    public void insert(Product product, int index, int quantity) {
        insert(product, index, quantity, null);
    }

    public void insert(Product product, int index, int quantity, ScannableCode scannedCode) {
        Entry e = getEntryBySku(product.getSku());

        if (e == null
                || product.getType() == Product.Type.UserWeighed
                || product.getType() == Product.Type.PreWeighed) {
            if(quantity > 0) {
                Entry entry = new Entry(product, quantity, scannedCode);
                addEntry(entry, index);
            }
        } else {
            setEntryQuantity(e, e.quantity + quantity);
        }
    }

    public void setQuantity(int index, int quantity) {
        setQuantity(index, quantity, null);
    }

    public void setQuantity(int index, int quantity, ScannableCode scannedCode) {
        Entry e = getEntry(index);

        if (e != null) {
            if (scannedCode != null) {
                e.setScannedCode(scannedCode);
            }

            setEntryQuantity(e, quantity);
        }
    }

    public void setQuantity(Product product, int quantity) {
        setQuantity(product, quantity, null);
    }

    public void setQuantity(Product product, int quantity, ScannableCode scannedCode) {
        if (product.getType() == Product.Type.Article) {
            Entry e = getEntryBySku(product.getSku());

            if (e != null) {
                if (scannedCode != null) {
                    e.setScannedCode(scannedCode);
                }

                setEntryQuantity(e, quantity);
            } else {
                add(product, quantity, scannedCode);
            }
        }
    }

    public void removeAll(int index) {
        Entry entry = getEntry(index);
        removeAll(entry);
    }

    /**
     * Deprecated: Use {@link ShoppingCart#getProduct} instead.
     */
    @Deprecated
    public Product getProductAtPosition(int index) {
        return getProduct(index);
    }

    public Product getProduct(int index) {
        Entry entry = getEntry(index);
        if (entry == null) {
            return null;
        }

        return entry.product;
    }

    public String getScannedCode(int index) {
        Entry entry = getEntry(index);
        if (entry == null) {
            return null;
        }

        return entry.scannedCode;
    }

    public Integer getEmbeddedWeight(int index) {
        Entry entry = getEntry(index);
        if (entry == null) {
            return null;
        }

        return entry.weight;
    }

    public Integer getEmbeddedPrice(int index) {
        Entry entry = getEntry(index);
        if (entry == null) {
            return null;
        }

        return entry.price;
    }

    public Integer getEmbeddedUnits(int index) {
        Entry entry = getEntry(index);
        if (entry == null) {
            return null;
        }

        return entry.amount;
    }

    public int getQuantity(Product product) {
        int q = 0;

        for (Entry entry : items) {
            if (entry.sku.equals(product.getSku())) {
                q += entry.quantity;
            }
        }

        return q;
    }

    public int getQuantity(int index) {
        Entry entry = getEntry(index);
        if (entry != null) {
            return entry.quantity;
        } else {
            return 0;
        }
    }

    /**
     * Removed the given entry from the list of {@link #items} regardless of the entry quantity.
     *
     * @param e the entry to remove
     */
    private void removeAll(Entry e) {
        if (e != null) {
            items.remove(e);
            notifyItemRemoved(this, e.product);
        }
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
        notifyCleared(this);
    }

    public void invalidate() {
        id = UUID.randomUUID().toString();
        clear();
    }

    public void checkForTimeout() {
        long currentTime = SystemClock.elapsedRealtime();

        if(lastModificationTime + TIMEOUT < currentTime){
            invalidate();
        }
    }

    private void setEntryQuantity(Entry e, int newQuantity) {
        if (e != null) {
            if (newQuantity > 0) {
                if (newQuantity != e.quantity) {
                    e.quantity = Math.max(0, Math.min(MAX_QUANTITY, newQuantity));
                    notifyQuantityChanged(this, e.product);
                }
            } else {
                removeAll(e);
            }
        }
    }

    private void addEntry(Entry e, int index) {
        if (contains(e)) {
            setEntryQuantity(e, e.quantity + 1);
        } else {
            items.add(index, e);
            notifyItemAdded(this, e.product);
        }
    }

    private Entry getEntry(final int index) {
        try {
            return items.get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    private Entry getEntryBySku(final String sku) {
        synchronized (lock) {
            for (Entry entry : items) {
                if (entry.sku.equals(sku)) {
                    return entry;
                }
            }

            return null;
        }
    }

    private int indexOf(Entry e) {
        return items.indexOf(e);
    }

    private boolean contains(Entry e) {
        return items.contains(e);
    }

    /**
     * Swaps the position of two entries based on their index.
     * <p>
     * Useful for swapping elements in list views.
     */
    public void swap(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < items.size() && toIndex >= 0 && toIndex < items.size() && fromIndex != toIndex) {
            Collections.swap(items, fromIndex, toIndex);
            notifyItemMoved(this, fromIndex, toIndex);
        }
    }

    public int getTotalPrice() {
        synchronized (lock) {
            int sum = 0;

            for (Entry e : items) {
                Product product = e.product;

                if(e.weight != null){
                    sum += product.getPriceForQuantity(e.weight, sdkInstance.getRoundingMode());
                } else if(e.price != null){
                    sum += e.price;
                } else if(e.amount != null){
                    sum += product.getPrice() * e.amount;
                } else {
                    sum += product.getPriceForQuantity(e.quantity, sdkInstance.getRoundingMode());
                }
            }

            sum += getTotalDepositPrice();

            return sum;
        }
    }

    public int getTotalDepositPrice() {
        synchronized (lock) {
            int sum = 0;

            for (Entry e : items) {
                Product depositProduct = e.product.getDepositProduct();
                if(depositProduct != null){
                    sum += depositProduct.getPriceForQuantity(e.quantity, sdkInstance.getRoundingMode());
                }
            }

            return sum;
        }
    }

    public int getTotalQuantity() {
        synchronized (lock) {
            int sum = 0;

            for (Entry e : items) {
                Product product = e.product;
                if (product.getType() == Product.Type.UserWeighed
                        || product.getType() == Product.Type.PreWeighed) {
                    sum += 1;
                } else {
                    sum += e.quantity;
                }
            }

            return sum;
        }
    }

    private void updateTimestamp() {
        lastModificationTime = SystemClock.elapsedRealtime();
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
        void onItemAdded(ShoppingCart list, Product product);

        void onQuantityChanged(ShoppingCart list, Product product);

        void onCleared(ShoppingCart list);

        void onItemMoved(ShoppingCart list, int fromIndex, int toIndex);

        void onItemRemoved(ShoppingCart list, Product product);
    }

    public static abstract class SimpleShoppingCartListener implements ShoppingCartListener {
        public abstract void onChanged(ShoppingCart list);

        @Override
        public void onItemAdded(ShoppingCart list, Product product) {
            onChanged(list);
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, Product product) {
            onChanged(list);
        }

        @Override
        public void onCleared(ShoppingCart list) {
            onChanged(list);
        }

        @Override
        public void onItemMoved(ShoppingCart list, int fromIndex, int toIndex) {
            onChanged(list);
        }

        @Override
        public void onItemRemoved(ShoppingCart list, Product product) {
            onChanged(list);
        }
    }

    private void notifyItemAdded(final ShoppingCart list, final Product product) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemAdded(list, product);
                }
            }
        });
    }

    private void notifyItemRemoved(final ShoppingCart list, final Product product) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemRemoved(list, product);
                }
            }
        });
    }

    private void notifyQuantityChanged(final ShoppingCart list, final Product product) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onQuantityChanged(list, product);
                }
            }
        });
    }

    private void notifyItemMoved(final ShoppingCart list, final int fromIndex, final int toIndex) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemMoved(list, fromIndex, toIndex);
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