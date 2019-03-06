package io.snabble.sdk.ui.cart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.snabble.sdk.Checkout;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Unit;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class ShoppingCartView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private RecyclerView recyclerView;
    private Adapter recyclerViewAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShoppingCart cart;
    private Checkout checkout;
    private PriceFormatter priceFormatter;
    private Button pay;
    private View coordinatorLayout;
    private ViewGroup emptyState;
    private Snackbar snackbar;
    private DelayedProgressDialog progressDialog;
    private boolean hasAnyImages;
    private ArrayList<Row> currentList;

    private ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.ShoppingCartListener() {
        @Override
        public void onProductsUpdated(ShoppingCart list) {
            submitList();
            update();
        }

        @Override
        public void onItemAdded(ShoppingCart list, ShoppingCart.Item item) {
            submitList();
            update();
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, ShoppingCart.Item item) {
            int index = list.indexOf(item);
            currentList.set(index, createProductRow(item));
            recyclerViewAdapter.submitList(new ArrayList<>(currentList));
            update();
        }

        @Override
        public void onCleared(ShoppingCart list) {
            submitList();
            update();
        }

        @Override
        public void onItemRemoved(ShoppingCart list, ShoppingCart.Item item, int pos) {
            submitList();
            update();
        }

        @Override
        public void onPricesUpdated(ShoppingCart list) {
            swipeRefreshLayout.setRefreshing(false);
            submitList();
            update();
        }
    };

    public ShoppingCartView(Context context) {
        super(context);
        inflateView(context, null);
    }

    public ShoppingCartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView(context, attrs);
    }

    public ShoppingCartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView(context, attrs);
    }

    private void inflateView(Context context, AttributeSet attrs) {
        inflate(getContext(), R.layout.view_shopping_cart, this);

        Project project = SnabbleUI.getProject();

        if (cart != null) {
            cart.removeListener(shoppingCartListener);
        }

        cart = project.getShoppingCart();
        checkout = project.getCheckout();
        priceFormatter = new PriceFormatter(project);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new Adapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(itemAnimator);

        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(itemDecoration);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                cart.updatePrices(false);
            }
        });

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        emptyState = findViewById(R.id.empty_state);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    checkout.cancel();
                    return true;
                }
                return false;
            }
        });

        pay = findViewById(R.id.pay);
        pay.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                checkout.checkout();
                Telemetry.event(Telemetry.Event.ClickCheckout);
            }
        });

        View scanProducts = findViewById(R.id.scan_products);
        scanProducts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showScannerWithCode(null);
                }
            }
        });

        createItemTouchHelper();
        submitList();
    }

    private void createItemTouchHelper() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (!recyclerViewAdapter.isDismissable(viewHolder.getAdapterPosition())) {
                    return 0;
                }

                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                ViewHolder holder = (ViewHolder) viewHolder;
                holder.hideInput();

                final int pos = viewHolder.getAdapterPosition();

                ShoppingCart.Item item = cart.get(pos);

                removeAndShowUndoSnackbar(pos, item);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void removeAndShowUndoSnackbar(final int pos, final ShoppingCart.Item item) {
        cart.remove(pos);
        Telemetry.event(Telemetry.Event.DeletedFromCart, item.getProduct());

        snackbar = UIUtils.snackbar(coordinatorLayout,
                R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
        snackbar.setAction(R.string.Snabble_undo, new OnClickListener() {
            @Override
            public void onClick(View v) {
                cart.insert(item, pos);
                Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.getProduct());
            }
        });

        snackbar.show();
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == Checkout.State.HANDSHAKING) {
            progressDialog.showAfterDelay(500);
        } else if (state == Checkout.State.REQUEST_PAYMENT_METHOD || state == Checkout.State.WAIT_FOR_APPROVAL) {
            SnabbleUICallback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.showCheckout();
            }
            progressDialog.dismiss();
        } else if (state == Checkout.State.INVALID_PRODUCTS) {
            List<Product> invalidProducts = checkout.getInvalidProducts();
            if (invalidProducts != null && invalidProducts.size() > 0) {
                Resources res = getResources();
                StringBuilder sb = new StringBuilder();
                if (invalidProducts.size() == 1) {
                    sb.append(res.getString(R.string.Snabble_saleStop_errorMsg_one));
                } else {
                    sb.append(res.getString(R.string.Snabble_saleStop_errorMsg));
                }

                sb.append("\n\n");

                for(Product product : invalidProducts) {
                    if (product.getSubtitle() != null) {
                        sb.append(product.getSubtitle());
                        sb.append(" ");
                    }

                    sb.append(product.getName());
                    sb.append("\n");
                }

                new AlertDialog.Builder(getContext())
                        .setCancelable(false)
                        .setTitle(R.string.Snabble_saleStop_errorMsg_title)
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .show();
            } else {
                UIUtils.snackbar(coordinatorLayout, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show();
            }

            progressDialog.dismiss();
        } else if (state == Checkout.State.CONNECTION_ERROR) {
            UIUtils.snackbar(coordinatorLayout, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                    .show();
            progressDialog.dismiss();
        } else if (state != Checkout.State.VERIFYING_PAYMENT_METHOD) {
            progressDialog.dismiss();
        }
    }

    private void updateEmptyState() {
        if (cart.size() > 0) {
            emptyState.setVisibility(View.GONE);
            pay.setVisibility(checkout.isAvailable() ? View.VISIBLE : View.GONE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
            pay.setVisibility(View.GONE);
        }
    }

    private void updatePayText() {
        if (cart != null) {
            int quantity = cart.getTotalQuantity();
            int price = cart.getTotalPrice();
            if (price > 0) {
                String formattedPrice = priceFormatter.format(price);
                String text = getResources().getQuantityString(R.plurals.Snabble_Shoppingcart_buyProducts,
                        quantity, quantity, formattedPrice);
                pay.setText(text);
            } else {
                pay.setText(R.string.Snabble_Shoppingcart_buyProducts_now);
            }
        }
    }

    private void update() {
        updatePayText();
        updateEmptyState();
        scanForImages();
    }

    private void scanForImages() {
        boolean lastHasAnyImages = hasAnyImages;

        hasAnyImages = false;

        for (int i = 0; i < cart.size(); i++) {
            ShoppingCart.Item item = cart.get(i);
            if (item.isLineItem()) continue;

            Product product = item.getProduct();
            String url = product.getImageUrl();
            if (url != null && url.length() > 0) {
                hasAnyImages = true;
                break;
            }
        }

        if (hasAnyImages != lastHasAnyImages) {
            submitList();
            update();
        }
    }

    public void registerListeners() {
        SnabbleUI.getProject().getCheckout().cancelSilently();

        cart.addListener(shoppingCartListener);
        checkout.addOnCheckoutStateChangedListener(ShoppingCartView.this);
    }

    public void unregisterListeners() {
        cart.removeListener(shoppingCartListener);
        checkout.removeOnCheckoutStateChangedListener(ShoppingCartView.this);

        progressDialog.dismiss();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        unregisterListeners();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        submitList();
        update();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        registerListeners();

                        submitList();
                        update();
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
                    }
                }
            };

    private String sanitize(String input) {
        if (input != null && input.equals("")) return null;
        return input;
    }

    private void submitList() {
        long start = SystemClock.currentThreadTimeMillis();

        currentList = new ArrayList<>(cart.size() + 1);

        for (int i = 0; i < cart.size(); i++) {
            final ProductRow row = createProductRow(cart.get(i));
            currentList.add(row);
        }

        if (cart.getTotalDepositPrice() > 0) {
            DepositRow row = new DepositRow();
            row.depositPrice = cart.getTotalDepositPrice();
            currentList.add(row);
        }

        Logger.d("submitList creation took %dms", SystemClock.currentThreadTimeMillis() - start);

        long start2 = SystemClock.currentThreadTimeMillis();
        recyclerViewAdapter.submitList(new ArrayList<>(currentList));
        Logger.d("submitList submit took %dms", SystemClock.currentThreadTimeMillis() - start2);
    }

    @NonNull
    private ProductRow createProductRow(ShoppingCart.Item item) {
        final ProductRow row = new ProductRow();
        final Product product = item.getProduct();
        final int quantity = item.getQuantity();

        if (product != null) {
            row.subtitle = sanitize(product.getSubtitle());
            row.imageUrl = sanitize(product.getImageUrl());
        }

        row.name = sanitize(item.getDisplayName());
        row.encodingUnit = item.getUnit();
        row.priceText = sanitize(item.getPriceText());
        row.quantity = quantity;
        row.quantityText = sanitize(item.getQuantityText());
        row.editable = item.isEditable();
        row.item = item;
        return row;
    }

    private void setTextOrHide(TextView textView, String text, int hideVisibility) {
        if (text != null) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(hideVisibility);
        }
    }

    private interface Row {}

    private class ProductRow implements Row {
        ShoppingCart.Item item;

        String name;
        String subtitle;
        String imageUrl;
        Unit encodingUnit;
        String priceText;
        String quantityText;
        int quantity;
        boolean editable;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ProductRow that = (ProductRow) o;

            if (quantity != that.quantity) return false;
            if (editable != that.editable) return false;
            if (item != null ? !item.equals(that.item) : that.item != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (subtitle != null ? !subtitle.equals(that.subtitle) : that.subtitle != null)
                return false;
            if (imageUrl != null ? !imageUrl.equals(that.imageUrl) : that.imageUrl != null)
                return false;
            if (encodingUnit != that.encodingUnit) return false;
            if (priceText != null ? !priceText.equals(that.priceText) : that.priceText != null)
                return false;
            return quantityText != null ? quantityText.equals(that.quantityText) : that.quantityText == null;
        }

        @Override
        public int hashCode() {
            int result = item != null ? item.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (subtitle != null ? subtitle.hashCode() : 0);
            result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
            result = 31 * result + (encodingUnit != null ? encodingUnit.hashCode() : 0);
            result = 31 * result + (priceText != null ? priceText.hashCode() : 0);
            result = 31 * result + (quantityText != null ? quantityText.hashCode() : 0);
            result = 31 * result + quantity;
            result = 31 * result + (editable ? 1 : 0);
            return result;
        }
    }

    private class DepositRow implements Row {
        int depositPrice;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DepositRow row = (DepositRow) o;

            return depositPrice == row.depositPrice;
        }

        @Override
        public int hashCode() {
            return depositPrice;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        TextView subtitle;
        TextView quantityTextView;
        TextView priceTextView;
        View plus;
        View minus;
        EditText quantityEdit;
        View controlsUserWeighed;
        View controlsDefault;
        View quantityEditApply;
        TextView quantityAnnotation;
        TextWatcher textWatcher;

        public ViewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.name);
            subtitle = itemView.findViewById(R.id.subtitle);
            quantityTextView = itemView.findViewById(R.id.quantity);
            priceTextView = itemView.findViewById(R.id.price);
            plus = itemView.findViewById(R.id.plus);
            minus = itemView.findViewById(R.id.minus);
            controlsUserWeighed = itemView.findViewById(R.id.controls_user_weighed);
            controlsDefault = itemView.findViewById(R.id.controls_default);
            quantityEdit = itemView.findViewById(R.id.quantity_edit);
            quantityEditApply = itemView.findViewById(R.id.quantity_edit_apply);
            quantityAnnotation = itemView.findViewById(R.id.quantity_annotation);
        }

        @SuppressLint("SetTextI18n")
        public void bindTo(final ProductRow row) {
            setTextOrHide(subtitle, row.subtitle, View.GONE);
            setTextOrHide(name, row.name, View.GONE);
            setTextOrHide(priceTextView, row.priceText, View.GONE);
            setTextOrHide(quantityTextView, row.quantityText, View.GONE);

            if (row.imageUrl != null) {
                image.setVisibility(View.VISIBLE);
                Picasso.with(getContext()).load(row.imageUrl).into(image);
            } else {
                image.setVisibility(hasAnyImages ? View.INVISIBLE : View.GONE);
                image.setImageBitmap(null);
            }

            String encodingDisplayValue = "g";
            Unit encodingUnit = row.encodingUnit;
            if (encodingUnit != null) {
                encodingDisplayValue = encodingUnit.getDisplayValue();
            }
            quantityAnnotation.setText(encodingDisplayValue);

            if (row.editable) {
                if (row.item.getProduct().getType() == Product.Type.UserWeighed) {
                    controlsDefault.setVisibility(View.GONE);
                    controlsUserWeighed.setVisibility(View.VISIBLE);
                } else {
                    controlsDefault.setVisibility(View.VISIBLE);
                    controlsUserWeighed.setVisibility(View.GONE);
                }
            } else {
                controlsDefault.setVisibility(View.GONE);
                controlsUserWeighed.setVisibility(View.GONE);
            }

            plus.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    row.item.setQuantity(row.item.getQuantity() + 1);
                }
            });

            minus.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int p = getAdapterPosition();

                    int newQuantity = row.item.getQuantity() - 1;
                    if (newQuantity <= 0) {
                        removeAndShowUndoSnackbar(p, row.item);
                    } else {
                        row.item.setQuantity(newQuantity);
                    }
                }
            });

            quantityEditApply.setOnClickListener(new OneShotClickListener() {
                @Override
                public void click() {
                    row.item.setQuantity(getQuantityEditValue());
                    hideInput();
                }
            });

            quantityEdit.setText(Integer.toString(row.quantity));
            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);

            if (getAdapterPosition() == 0) {
                itemView.requestFocus();
            }

            quantityEdit.removeTextChangedListener(textWatcher);
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateQuantityEditApplyVisibility(row.quantity);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            };

            updateQuantityEditApplyVisibility(row.quantity);

            quantityEdit.addTextChangedListener(textWatcher);
            quantityEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE
                            || (event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        quantityEditApply.callOnClick();
                        return true;
                    }

                    return false;
                }
            });
        }

        private void hideInput() {
            InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(quantityEdit.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }

            quantityEdit.clearFocus();
        }

        private void updateQuantityEditApplyVisibility(int quantity) {
            int value = getQuantityEditValue();
            if (value > 0 && value != quantity) {
                quantityEditApply.setVisibility(View.VISIBLE);
            } else {
                quantityEditApply.setVisibility(View.GONE);
            }
        }

        public int getQuantityEditValue() {
            try {
                return Integer.parseInt(quantityEdit.getText().toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private class DepositViewHolder extends RecyclerView.ViewHolder {
        TextView deposit;
        AppCompatImageView image;

        public DepositViewHolder(View itemView) {
            super(itemView);

            deposit = itemView.findViewById(R.id.deposit);
            image = itemView.findViewById(R.id.image);
        }

        public void update(DepositRow row) {
            PriceFormatter priceFormatter = new PriceFormatter(SnabbleUI.getProject());
            String depositText = priceFormatter.format(row.depositPrice);
            deposit.setText(depositText);
        }
    }

    public static final DiffUtil.ItemCallback<Row> DIFF_CALLBACK = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(
                @NonNull Row oldRow, @NonNull Row newRow) {
            if (oldRow instanceof ProductRow && newRow instanceof ProductRow) {
                ProductRow productOldRow = (ProductRow) oldRow;
                ProductRow productNewRow = (ProductRow) newRow;

                return productOldRow.item == productNewRow.item;
            } else if (oldRow instanceof DepositRow && newRow instanceof DepositRow) {
                return true;
            }

            return false;
        }

        @Override
        public boolean areContentsTheSame(
                @NonNull Row oldRow, @NonNull Row newRow) {
            return oldRow.equals(newRow);
        }
    };

    private class Adapter extends ListAdapter<Row, RecyclerView.ViewHolder> {
        public static final int TYPE_ITEM = 0;
        public static final int TYPE_DEPOSIT = 1;

        public Adapter() {
            super(DIFF_CALLBACK);
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) instanceof DepositRow) {
                return TYPE_DEPOSIT;
            }

            return TYPE_ITEM;
        }

        public boolean isDismissable(int position) {
            return getItemViewType(position) != TYPE_DEPOSIT && !((ProductRow)getItem(position)).item.isLineItem();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_DEPOSIT) {
                View v = View.inflate(getContext(), R.layout.item_shoppingcart_deposit, null);
                return new DepositViewHolder(v);
            } else {
                View v = View.inflate(getContext(), R.layout.item_shoppingcart_product, null);
                return new ViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if (type == TYPE_ITEM) {
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.bindTo((ProductRow) getItem(position));
            } else {
                DepositViewHolder viewHolder = (DepositViewHolder) holder;
                viewHolder.update((DepositRow) getItem(position));
            }
        }
    }
}
