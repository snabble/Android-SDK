package io.snabble.sdk.ui.cart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
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
import java.util.Objects;

import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.Unit;
import io.snabble.sdk.ui.GestureHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.InputFilterMinMax;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.SnackbarUtils;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class ShoppingCartView extends FrameLayout {
    private RecyclerView recyclerView;
    private ShoppingCartAdapter recyclerViewAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShoppingCart cart;
    private ViewGroup emptyState;
    private View restore;
    private TextView scanProducts;
    private boolean hasAnyImages;
    private List<Product> lastInvalidProducts;
    private AlertDialog alertDialog;
    private View paymentContainer;
    private boolean hasAlreadyShownInvalidDeposit;

    private final ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.SimpleShoppingCartListener() {

        @Override
        public void onChanged(ShoppingCart cart) {
            swipeRefreshLayout.setRefreshing(false);
            submitList();
            update();
        }

        @Override
        public void onCheckoutLimitReached(ShoppingCart list) {
            if (alertDialog != null) {
                alertDialog.dismiss();
            }

            Project project = SnabbleUI.getProject();
            String message = getResources().getString(R.string.Snabble_limitsAlert_checkoutNotAvailable,
                    project.getPriceFormatter().format(project.getMaxCheckoutLimit()));

            alertDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.Snabble_limitsAlert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .create();
            alertDialog.show();
        }

        @Override
        public void onOnlinePaymentLimitReached(ShoppingCart list) {
            if (alertDialog != null) {
                alertDialog.dismiss();
            }

            Project project = SnabbleUI.getProject();
            String message = getResources().getString(R.string.Snabble_limitsAlert_notAllMethodsAvailable,
                    project.getPriceFormatter().format(project.getMaxOnlinePaymentLimit()));

            alertDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.Snabble_limitsAlert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .create();
            alertDialog.show();
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
        if(isInEditMode()) return;
        final Project project = SnabbleUI.getProject();

        if (cart != null) {
            cart.removeListener(shoppingCartListener);
        }

        cart = project.getShoppingCart();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new ShoppingCartAdapter(recyclerView, cart);
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
        swipeRefreshLayout.setOnRefreshListener(() -> cart.updatePrices(false));

        emptyState = findViewById(R.id.empty_state);

        paymentContainer = findViewById(R.id.bottom_payment_container);

        scanProducts = findViewById(R.id.scan_products);
        scanProducts.setOnClickListener(view -> {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.SHOW_SCANNER, null);
            }
        });

        restore = findViewById(R.id.restore);
        restore.setOnClickListener(v -> cart.restore());

        PaymentSelectionHelper
                .getInstance()
                .getSelectedEntry()
                .observe((FragmentActivity)UIUtils.getHostActivity(getContext()), entry -> update());

        createItemTouchHelper(context.getResources());
        submitList();
        update();
    }

    private void createItemTouchHelper(Resources resources) {
        GestureHandler<Void> gestureHandler = new GestureHandler<Void>(resources) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (viewHolder instanceof ViewHolder) {
                    ViewHolder holder = (ViewHolder) viewHolder;
                    holder.hideInput();
                }

                final int pos = viewHolder.getBindingAdapterPosition();
                ShoppingCart.Item item = cart.get(pos);
                recyclerViewAdapter.removeAndShowUndoSnackbar(pos, item);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(gestureHandler);
        gestureHandler.setItemTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void updateEmptyState() {
        if (cart.size() > 0) {
            paymentContainer.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        } else {
            paymentContainer.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        }

        if (cart.isRestorable()) {
            restore.setVisibility(View.VISIBLE);
            scanProducts.setText(R.string.Snabble_Shoppingcart_emptyState_restartButtonTitle);
        } else {
            restore.setVisibility(View.GONE);
            scanProducts.setText(R.string.Snabble_Shoppingcart_emptyState_buttonTitle);
        }
    }

    private void update() {
        updateEmptyState();
        scanForImages();
        checkSaleStop();
        checkDepositReturnVoucher();
    }

    private void checkSaleStop() {
        List<Product> invalidProducts = cart.getInvalidProducts();

        if (invalidProducts.size() > 0 && !invalidProducts.equals(lastInvalidProducts)) {
            Resources res = getResources();
            StringBuilder sb = new StringBuilder();
            if (invalidProducts.size() == 1) {
                sb.append(res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_saleStop_errorMsg_one)));
            } else {
                sb.append(res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_saleStop_errorMsg)));
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
                    .setTitle(I18nUtils.getIdentifier(getResources(), R.string.Snabble_saleStop_errorMsg_title))
                    .setMessage(sb.toString())
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .show();

            lastInvalidProducts = invalidProducts;
        }
    }

    private void checkDepositReturnVoucher() {
        if (cart.hasInvalidDepositReturnVoucher() && !hasAlreadyShownInvalidDeposit) {
            new AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setTitle(I18nUtils.getIdentifier(getResources(), R.string.Snabble_saleStop_errorMsg_title))
                    .setMessage(I18nUtils.getIdentifier(getResources(), R.string.Snabble_invalidDepositVoucher_errorMsg))
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .show();
            hasAlreadyShownInvalidDeposit = true;
        }
    }

    private void scanForImages() {
        boolean lastHasAnyImages = hasAnyImages;

        hasAnyImages = false;

        for (int i = 0; i < cart.size(); i++) {
            ShoppingCart.Item item = cart.get(i);
            if (item.getType() != ShoppingCart.ItemType.PRODUCT) continue;

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
        cart.addListener(shoppingCartListener);
        submitList();
        update();
    }

    private void unregisterListeners() {
        cart.removeListener(shoppingCartListener);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(!isInEditMode()) {
            Application application = (Application) getContext().getApplicationContext();
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

            registerListeners();
        }
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

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
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

    private static String sanitize(String input) {
        if (input != null && input.equals("")) return null;
        return input;
    }

    public static List<Row> buildRows(Resources resources, ShoppingCart cart) {
        List<Row> rows = new ArrayList<>(cart.size() + 1);

        for (int i = 0; i < cart.size(); i++) {
            ShoppingCart.Item item = cart.get(i);

            if (item.getType() == ShoppingCart.ItemType.LINE_ITEM) {
                if (item.isDiscount()) {
                    SimpleRow row = new SimpleRow();
                    row.title = resources.getString(R.string.Snabble_Shoppingcart_discounts);
                    row.imageResId = R.drawable.snabble_ic_percent;
                    row.text = sanitize(item.getPriceText());
                    rows.add(row);
                } else if (item.isGiveaway()) {
                    SimpleRow row = new SimpleRow();
                    row.title = item.getDisplayName();
                    row.imageResId = R.drawable.snabble_ic_gift;
                    row.text = resources.getString(R.string.Snabble_Shoppingcart_giveaway);
                    rows.add(row);
                }
            } else if (item.getType() == ShoppingCart.ItemType.COUPON) {
                SimpleRow row = new SimpleRow();
                row.title = resources.getString(R.string.Snabble_Shoppingcart_coupon);
                row.text = item.getDisplayName();
                row.isDismissible = true;
                rows.add(row);
            } else if (item.getType() == ShoppingCart.ItemType.PRODUCT) {
                final ProductRow row = new ProductRow();
                final Product product = item.getProduct();
                final int quantity = item.getQuantity();

                if (product != null) {
                    row.subtitle = sanitize(product.getSubtitle());
                    row.imageUrl = sanitize(product.getImageUrl());
                }

                row.name = sanitize(item.getDisplayName());
                row.encodingUnit = item.getUnit();
                row.priceText = sanitize(item.getTotalPriceText());
                row.quantity = quantity;
                row.quantityText = sanitize(item.getQuantityText());
                row.editable = item.isEditable();
                row.isDismissible = true;
                row.manualDiscountApplied = item.isManualCouponApplied();
                row.item = item;
                rows.add(row);
            }
        }

        int cartTotal = cart.getTotalDepositPrice();
        if (cartTotal > 0) {
            SimpleRow row = new SimpleRow();
            PriceFormatter priceFormatter = SnabbleUI.getProject().getPriceFormatter();
            row.title = resources.getString(R.string.Snabble_Shoppingcart_deposit);
            row.imageResId = R.drawable.snabble_ic_deposit;
            row.text = priceFormatter.format(cartTotal);
            rows.add(row);
        }

        return rows;
    }

    private void submitList() {
        recyclerViewAdapter.submitList(buildRows(getResources(), cart), hasAnyImages);
    }

    private static void setTextOrHide(TextView textView, String text) {
        if (text != null) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private static abstract class Row {
        boolean isDismissible;
    }

    private static class ProductRow extends Row {

        ShoppingCart.Item item;

        String name;
        String subtitle;
        String imageUrl;
        Unit encodingUnit;
        String priceText;
        String quantityText;
        int quantity;
        boolean editable;
        boolean manualDiscountApplied;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductRow that = (ProductRow) o;
            return quantity == that.quantity &&
                    editable == that.editable &&
                    manualDiscountApplied == that.manualDiscountApplied &&
                    Objects.equals(item, that.item) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(subtitle, that.subtitle) &&
                    Objects.equals(imageUrl, that.imageUrl) &&
                    encodingUnit == that.encodingUnit &&
                    Objects.equals(priceText, that.priceText) &&
                    Objects.equals(quantityText, that.quantityText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, name, subtitle, imageUrl, encodingUnit, priceText, quantityText, quantity, editable, manualDiscountApplied);
        }
    }

    private static class SimpleRow extends Row {
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
        TextView redLabel;
        TextWatcher textWatcher;
        private final UndoHelper undoHelper;
        private final Picasso picasso;

        ViewHolder(View itemView, UndoHelper undoHelper) {
            super(itemView);
            this.undoHelper = undoHelper;
            this.picasso = Picasso.get();

            image = itemView.findViewById(R.id.helper_image);
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
            redLabel = itemView.findViewById(R.id.red_label);
        }

        @SuppressLint("SetTextI18n")
        public void bindTo(final ProductRow row, boolean hasAnyImages) {
            setTextOrHide(name, row.name);
            setTextOrHide(priceTextView, row.priceText);
            setTextOrHide(quantityTextView, row.quantityText);

            if (row.imageUrl != null) {
                image.setVisibility(View.VISIBLE);
                picasso.load(row.imageUrl).into(image);
            } else {
                image.setVisibility(hasAnyImages ? View.INVISIBLE : View.GONE);
                image.setImageBitmap(null);
            }

            boolean hasCoupon = row.item.getCoupon() != null;
            boolean isAgeRestricted = false;

            redLabel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff0000")));

            if (row.item.getProduct() != null) {
                isAgeRestricted = row.item.getProduct().getSaleRestriction().isAgeRestriction();
            }

            redLabel.setVisibility(hasCoupon || isAgeRestricted ? View.VISIBLE : View.GONE);

            if (hasCoupon) {
                if (!row.manualDiscountApplied) {
                    redLabel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#999999")));
                }

                redLabel.setText("%");
            } else {
                long age = row.item.getProduct().getSaleRestriction().getValue();

                if (age > 0) {
                    redLabel.setText(String.valueOf(age));
                } else {
                    redLabel.setVisibility(View.GONE);
                }
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

            plus.setOnClickListener(v -> {
                row.item.setQuantity(row.item.getQuantity() + 1);
                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
            });

            minus.setOnClickListener(v -> {
                int p = getBindingAdapterPosition();

                int newQuantity = row.item.getQuantity() - 1;
                if (newQuantity <= 0) {
                    undoHelper.removeAndShowUndoSnackbar(p, row.item);
                } else {
                    row.item.setQuantity(newQuantity);
                    Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
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

            if (getBindingAdapterPosition() == 0) {
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
            quantityEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    quantityEditApply.callOnClick();
                    return true;
                }

                return false;
            });

            quantityEdit.setFilters(new InputFilter[]{ new InputFilterMinMax(0, ShoppingCart.MAX_QUANTITY) });
        }

        private void hideInput() {
            InputMethodManager imm = (InputMethodManager) quantityEdit.getContext()
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

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView text;
        ImageView image;

        SimpleViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.helper_image);
        }

        public void update(SimpleRow row, boolean hasAnyImages) {
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

    public static class ShoppingCartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements UndoHelper, GestureHandler.DismissibleAdapter {
        private static final int TYPE_PRODUCT = 0;
        private static final int TYPE_SIMPLE = 1;
        private List<Row> list = Collections.emptyList();
        private final Context context;
        private final ShoppingCart cart;
        private final View parentView;
        private boolean hasAnyImages = false;

        public ShoppingCartAdapter(View parentView, ShoppingCart cart) {
            super();
            this.context = parentView.getContext();
            this.parentView = parentView;
            this.cart = cart;
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) instanceof SimpleRow) {
                return TYPE_SIMPLE;
            }

            return TYPE_PRODUCT;
        }

        @Override
        public boolean isDismissible(int position) {
            return getItem(position).isDismissible;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        // for fetching the data from outside of this view
        public void fetchFrom(ShoppingCart cart) {
            hasAnyImages = false;

            for (int i = 0; i < cart.size(); i++) {
                ShoppingCart.Item item = cart.get(i);
                if (item.getType() != ShoppingCart.ItemType.PRODUCT) continue;

                Product product = item.getProduct();
                String url = product.getImageUrl();
                if (url != null && url.length() > 0) {
                    hasAnyImages = true;
                    break;
                }
            }

            submitList(buildRows(context.getResources(), cart), hasAnyImages);
        }

        public void submitList(final List<Row> newList, boolean hasAnyImages) {
            this.hasAnyImages = hasAnyImages;
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

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_SIMPLE) {
                View v = View.inflate(context, R.layout.snabble_item_shoppingcart_simple, null);
                v.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                return new SimpleViewHolder(v);
            } else {
                View v = View.inflate(context, R.layout.snabble_item_shoppingcart_product, null);
                v.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                return new ViewHolder(v, this);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if (type == TYPE_PRODUCT) {
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.bindTo((ProductRow) getItem(position), hasAnyImages);
            } else {
                SimpleViewHolder viewHolder = (SimpleViewHolder) holder;
                viewHolder.update((SimpleRow) getItem(position), hasAnyImages);
            }
        }

        @Override
        public void removeAndShowUndoSnackbar(int adapterPosition, ShoppingCart.Item item) {
            if (adapterPosition == -1) {
                Logger.d("Invalid adapter position, ignoring");
                return;
            }

            cart.remove(adapterPosition);
            Telemetry.event(Telemetry.Event.DeletedFromCart, item.getProduct());

            Snackbar snackbar = SnackbarUtils.make(parentView,
                    R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
            snackbar.setAction(R.string.Snabble_undo, v -> {
                cart.insert(item, adapterPosition);
                Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.getProduct());
            });
            snackbar.show();
        }
    }
}
