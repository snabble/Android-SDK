package io.snabble.sdk.ui.cart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.Editable;
import android.text.InputFilter;
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

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.Unit;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.checkout.CheckoutHelper;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.InputFilterMinMax;
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
    private View restore;
    private TextView scanProducts;
    private Snackbar snackbar;
    private DelayedProgressDialog progressDialog;
    private boolean hasAnyImages;
    private Picasso picasso;
    private List<Product> lastInvalidProducts;

    private ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.SimpleShoppingCartListener() {
        @Override
        public void onChanged(ShoppingCart cart) {
            swipeRefreshLayout.setRefreshing(false);
            submitList();
            update();
        }

        @Override
        public void onCheckoutLimitReached(ShoppingCart list) {
            if (snackbar != null) {
                snackbar.dismiss();
            }

            Project project = SnabbleUI.getProject();
            String message = getResources().getString(R.string.Snabble_limitsAlert_checkoutNotAvailable,
                    project.getPriceFormatter().format(project.getMaxCheckoutLimit()));
            snackbar = UIUtils.snackbar(coordinatorLayout, message, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
            snackbar.show();
        }

        @Override
        public void onOnlinePaymentLimitReached(ShoppingCart list) {
            if (snackbar != null) {
                snackbar.dismiss();
            }

            Project project = SnabbleUI.getProject();
            String message = getResources().getString(R.string.Snabble_limitsAlert_notAllMethodsAvailable,
                    project.getPriceFormatter().format(project.getMaxOnlinePaymentLimit()));
            snackbar = UIUtils.snackbar(coordinatorLayout, message, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
            snackbar.show();
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
        Snabble.getInstance()._setCurrentActivity(UIUtils.getHostActivity(getContext()));

        inflate(getContext(), R.layout.snabble_view_shopping_cart, this);
        picasso = Picasso.with(getContext());
        final Project project = SnabbleUI.getProject();

        if (cart != null) {
            cart.removeListener(shoppingCartListener);
        }

        cart = project.getShoppingCart();
        checkout = project.getCheckout();
        priceFormatter = project.getPriceFormatter();

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
                    checkout.abort();
                    return true;
                }
                return false;
            }
        });

        pay = findViewById(R.id.pay);
        pay.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                if (cart.hasReachedMaxCheckoutLimit()) {
                    Project project = SnabbleUI.getProject();
                    String message = getResources().getString(R.string.Snabble_limitsAlert_checkoutNotAvailable,
                            project.getPriceFormatter().format(project.getMaxCheckoutLimit()));
                    snackbar = UIUtils.snackbar(coordinatorLayout, message, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
                    snackbar.show();
                } else {
                    checkout.checkout();
                    Telemetry.event(Telemetry.Event.ClickCheckout);
                }
            }
        });


        scanProducts = findViewById(R.id.scan_products);
        scanProducts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.execute(SnabbleUI.Action.SHOW_SCANNER, null);
                }
            }
        });

        restore = findViewById(R.id.restore);
        restore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cart.restore();
            }
        });

        createItemTouchHelper();
        submitList();
        update();
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

    private void removeAndShowUndoSnackbar(final int adapterPosition, final ShoppingCart.Item item) {
        if (adapterPosition == -1) {
            Logger.d("Invalid adapter position, ignoring");
            return;
        }

        cart.remove(adapterPosition);
        Telemetry.event(Telemetry.Event.DeletedFromCart, item.getProduct());

        snackbar = UIUtils.snackbar(coordinatorLayout,
                R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
        snackbar.setAction(R.string.Snabble_undo, new OnClickListener() {
            @Override
            public void onClick(View v) {
                cart.insert(item, adapterPosition);
                Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.getProduct());
            }
        });

        snackbar.show();
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == Checkout.State.HANDSHAKING) {
            progressDialog.showAfterDelay(500);
        } else if (state == Checkout.State.REQUEST_PAYMENT_METHOD) {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.SHOW_PAYMENT_SELECTION, null);
            }
            progressDialog.dismiss();
        } else if (state == Checkout.State.WAIT_FOR_APPROVAL) {
            CheckoutHelper.displayPaymentView(checkout);
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
        } else if (state == Checkout.State.CONNECTION_ERROR || state == Checkout.State.NO_SHOP) {
            UIUtils.snackbar(coordinatorLayout, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                    .show();
            progressDialog.dismiss();
        } else if (state == Checkout.State.NO_PAYMENT_METHOD_AVAILABLE) {
            new AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setTitle(R.string.Snabble_saleStop_errorMsg_title)
                    .setMessage(R.string.Snabble_Payment_noMethodAvailable)
                    .setPositiveButton(R.string.Snabble_OK, null)
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

        if (cart.isRestorable() && cart.getBackupTimestamp() > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)) {
            restore.setVisibility(View.VISIBLE);
            scanProducts.setText(R.string.Snabble_Shoppingcart_emptyState_restartButtonTitle);
        } else {
            restore.setVisibility(View.GONE);
            scanProducts.setText(R.string.Snabble_Shoppingcart_emptyState_buttonTitle);
        }
    }

    private void updatePayText() {
        if (cart != null) {
            int quantity = cart.getTotalQuantity();
            int price = cart.getTotalPrice();
            if (price != 0) {
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
        checkSaleStop();
    }

    private void checkSaleStop() {
        List<Product> invalidProducts = cart.getInvalidProducts();

        if (invalidProducts.size() > 0 && !invalidProducts.equals(lastInvalidProducts)) {
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

            lastInvalidProducts = invalidProducts;
        }
    }

    private void scanForImages() {
        boolean lastHasAnyImages = hasAnyImages;

        hasAnyImages = false;

        for (int i = 0; i < cart.size(); i++) {
            ShoppingCart.Item item = cart.get(i);
            if (item.isOnlyLineItem()) continue;

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

    private void registerListeners() {
        // IDK why this is here
        // SnabbleUI.getProject().getCheckout().abortSilently();

        cart.addListener(shoppingCartListener);
        checkout.addOnCheckoutStateChangedListener(ShoppingCartView.this);
        submitList();
        update();
    }

    private void unregisterListeners() {
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
        List<Row> rows = new ArrayList<>(cart.size() + 1);

        for (int i = 0; i < cart.size(); i++) {
            ShoppingCart.Item item = cart.get(i);

            if (item.isOnlyLineItem() && item.getTotalDepositPrice() > 0) {
                continue;
            }

            if (item.isDiscount()) {
                SimpleRow row = new SimpleRow();
                row.title = getResources().getString(R.string.Snabble_Shoppingcart_discounts);
                row.imageResId = R.drawable.snabble_ic_percent;
                row.text = sanitize(item.getPriceText());
                rows.add(row);
                continue;
            }

            if (item.isGiveaway()) {
                SimpleRow row = new SimpleRow();
                row.title = item.getDisplayName();
                row.imageResId = R.drawable.snabble_ic_gift;
                row.text = getResources().getString(R.string.Snabble_Shoppingcart_giveaway);
                rows.add(row);
                continue;
            }

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
            rows.add(row);
        }

        int cartTotal = cart.getTotalDepositPrice();
        if (cartTotal > 0) {
            SimpleRow row = new SimpleRow();
            PriceFormatter priceFormatter = SnabbleUI.getProject().getPriceFormatter();
            row.title = getResources().getString(R.string.Snabble_Shoppingcart_deposit);
            row.imageResId = R.drawable.snabble_ic_deposit;
            row.text = priceFormatter.format(cartTotal);
            rows.add(row);
        }

        recyclerViewAdapter.submitList(rows);
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

    private class SimpleRow implements Row {
        String text;
        String title;
        @DrawableRes int imageResId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleRow simpleRow = (SimpleRow) o;

            if (imageResId != simpleRow.imageResId) return false;
            if (text != null ? !text.equals(simpleRow.text) : simpleRow.text != null) return false;
            return title != null ? title.equals(simpleRow.title) : simpleRow.title == null;
        }

        @Override
        public int hashCode() {
            int result = text != null ? text.hashCode() : 0;
            result = 31 * result + (title != null ? title.hashCode() : 0);
            result = 31 * result + imageResId;
            return result;
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
                picasso.load(row.imageUrl).into(image);
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
                    Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
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
                        Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
                    }
                }
            });

            quantityEditApply.setOnClickListener(new OneShotClickListener() {
                @Override
                public void click() {
                    row.item.setQuantity(getQuantityEditValue());
                    hideInput();
                    Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
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

            quantityEdit.setFilters(new InputFilter[]{ new InputFilterMinMax(0, ShoppingCart.MAX_QUANTITY) });
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

    private class SimpleViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView text;
        ImageView image;

        public SimpleViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.image);
        }

        public void update(SimpleRow row) {
            title.setText(row.title);
            text.setText(row.text);
            image.setImageResource(row.imageResId);

            if (hasAnyImages) {
                image.setVisibility(View.VISIBLE);
            } else {
                image.setVisibility(View.GONE);
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_PRODUCT = 0;
        private static final int TYPE_SIMPLE = 1;
        private List<Row> list = Collections.emptyList();

        Adapter() {
            super();
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) instanceof SimpleRow) {
                return TYPE_SIMPLE;
            }

            return TYPE_PRODUCT;
        }

        public boolean isDismissable(int position) {
            return getItemViewType(position) != TYPE_SIMPLE && !((ProductRow)getItem(position)).item.isOnlyLineItem();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void submitList(final List<Row> newList) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return list.size();
                }

                @Override
                public int getNewListSize() {
                    return newList.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    Row oldRow = list.get(oldItemPosition);
                    Row newRow = newList.get(newItemPosition);

                    if (oldRow instanceof ProductRow && newRow instanceof ProductRow) {
                        ProductRow productOldRow = (ProductRow) oldRow;
                        ProductRow productNewRow = (ProductRow) newRow;

                        return productOldRow.item == productNewRow.item;
                    } else if (oldRow instanceof SimpleRow && newRow instanceof SimpleRow) {
                        return true;
                    }

                    return false;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Row oldRow = list.get(oldItemPosition);
                    Row newRow = newList.get(newItemPosition);

                    return oldRow.equals(newRow);
                }
            });

            list = newList;
            diffResult.dispatchUpdatesTo(this);
        }

        public Row getItem(int position) {
            return list.get(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_SIMPLE) {
                View v = View.inflate(getContext(), R.layout.snabble_item_shoppingcart_simple, null);
                return new SimpleViewHolder(v);
            } else {
                View v = View.inflate(getContext(), R.layout.snabble_item_shoppingcart_product, null);
                return new ViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if (type == TYPE_PRODUCT) {
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.bindTo((ProductRow) getItem(position));
            } else {
                SimpleViewHolder viewHolder = (SimpleViewHolder) holder;
                viewHolder.update((SimpleRow) getItem(position));
            }
        }
    }
}
