package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.codes.ScannedCode;

public class ShoppingCart2 {
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

    protected ShoppingCart2() {
        //empty constructor for gson
    }

    ShoppingCart2(Project project) {
        id = UUID.randomUUID().toString();
        updateTimestamp();

        initWithProject(project);
    }

    void initWithProject(Project project) {
        this.project = project;
        checkForTimeout();
    }

    public String getId() {
        return id;
    }

    public void add(Item item) {
        items.add(item);
        notifyItemAdded(this, item);
    }

    public Item get(int index) {
        return items.get(index);
    }

    public void remove(int index) {
        notifyItemRemoved(this, items.remove(index));
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
                        || product.getReferenceUnit() == Unit.PIECE) {
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

        public Item(Product product, ScannedCode scannedCode) {
            this.scannedCode = scannedCode;
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }

        public ScannedCode getScannedCode() {
            return scannedCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity; // TODO impl
        }

        public boolean isEditable() {
            return true; // TODO impl
        }

        public boolean isMergeAllowed() {
            return true; // TODO impl
        }

        public int getTotalPrice() {
            return quantity * product.getDiscountedPrice(); // TODO impl
        }

        public int getTotalDepositPrice() {
            if (product.getDepositProduct() != null) {
                return quantity * product.getDepositProduct().getDiscountedPrice(); // TODO impl
            }

            return 0;
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
        void onItemAdded(ShoppingCart2 list, Item item);

        void onQuantityChanged(ShoppingCart2 list, Item item);

        void onCleared(ShoppingCart2 list);

        void onItemRemoved(ShoppingCart2 list, Item item);

        void onUpdate(ShoppingCart2 list);
    }

    public static abstract class SimpleShoppingCartListener implements ShoppingCartListener {
        public abstract void onChanged(ShoppingCart2 list);

        @Override
        public void onUpdate(ShoppingCart2 list) {
            onChanged(list);
        }

        @Override
        public void onItemAdded(ShoppingCart2 list, Item item) {
            onChanged(list);
        }

        @Override
        public void onQuantityChanged(ShoppingCart2 list, Item item) {
            onChanged(list);
        }

        @Override
        public void onCleared(ShoppingCart2 list) {
            onChanged(list);
        }

        @Override
        public void onItemRemoved(ShoppingCart2 list, Item item) {
            onChanged(list);
        }
    }

    private void notifyItemAdded(final ShoppingCart2 list, final Item item) {
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

    private void notifyItemRemoved(final ShoppingCart2 list, final Item item) {
        updateTimestamp();

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ShoppingCartListener listener : listeners) {
                    listener.onItemRemoved(list, item);
                }
            }
        });
    }

    private void notifyQuantityChanged(final ShoppingCart2 list, final Item item) {
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

    private void notifyUpdate(final ShoppingCart2 list) {
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
     * @param list the {@link ShoppingCart2}
     */
    private void notifyCleared(final ShoppingCart2 list) {
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