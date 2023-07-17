package io.snabble.sdk.ui.cart;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ViolationNotification;
import io.snabble.sdk.ui.GestureHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.cart.adapter.ProductRow;
import io.snabble.sdk.ui.cart.adapter.Row;
import io.snabble.sdk.ui.cart.adapter.viewholder.ShoppingCartItemViewHolder;
import io.snabble.sdk.ui.cart.adapter.viewholder.SnabbleSimpleRow;
import io.snabble.sdk.ui.cart.adapter.viewholder.SimpleViewHolder;
import io.snabble.sdk.ui.checkout.ViolationNotificationUtils;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.SnackbarUtils;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.ui.utils.ViewUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

/**
 * This class is DEPRECATED and shall be removed in future versions.
 * Use the io.snabble.sdk.ui.scanner.ScannerBottomSheetView or the richer
 * io.snabble.sdk.ui.scanner.CombinedScannerFragment instead.
 */
@Deprecated
public class ShoppingCartView extends FrameLayout {
    private View rootView;
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
    private Project project;
    private boolean isRegistered;

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

            String message = getResources().getString(R.string.Snabble_LimitsAlert_checkoutNotAvailable,
                    project.getPriceFormatter().format(project.getMaxCheckoutLimit()));

            alertDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.Snabble_LimitsAlert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Snabble_ok, null)
                    .create();
            alertDialog.show();
        }

        @Override
        public void onOnlinePaymentLimitReached(ShoppingCart list) {
            if (alertDialog != null) {
                alertDialog.dismiss();
            }

            String message = getResources().getString(R.string.Snabble_LimitsAlert_notAllMethodsAvailable,
                    project.getPriceFormatter().format(project.getMaxOnlinePaymentLimit()));

            alertDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.Snabble_LimitsAlert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Snabble_ok, null)
                    .create();
            alertDialog.show();
        }

        @Override
        public void onViolationDetected(@NonNull List<ViolationNotification> violations) {
            ViolationNotificationUtils.showNotificationOnce(violations, getContext(), cart);
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
        inflate(getContext(), R.layout.snabble_view_shopping_cart, this);

        rootView = findViewById(R.id.root);

        if (isInEditMode()) return;

        ViewUtils.observeView(Snabble.getInstance().getCheckedInProject(), this, p -> {
            if (p != null) {
                initViewState(p);
            }
        });

        Project currentProject = Snabble.getInstance().getCheckedInProject().getValue();
        if (currentProject != null) {
            initViewState(currentProject);
        }
    }

    private void initViewState(Project p) {
        if (p != project) {
            rootView.setVisibility(View.VISIBLE);
            unregisterListeners();
            project = p;

            if (cart != null) {
                cart.removeListener(shoppingCartListener);
            }

            cart = project.getShoppingCart();
            resetViewState(getContext());
            registerListeners();
        }
    }

    private void resetViewState(Context context) {
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
        scanProducts.setOnClickListener(view -> SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_SCANNER));

        restore = findViewById(R.id.restore);
        restore.setOnClickListener(v -> cart.restore());

        PaymentSelectionHelper
                .getInstance()
                .getSelectedEntry()
                .observe((FragmentActivity) UIUtils.getHostActivity(getContext()), entry -> update());

        createItemTouchHelper(context.getResources());
        submitList();
        update();
    }

    private void createItemTouchHelper(Resources resources) {
        GestureHandler<Void> gestureHandler = new GestureHandler<Void>(resources) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (viewHolder instanceof ShoppingCartItemViewHolder) {
                    ShoppingCartItemViewHolder holder = (ShoppingCartItemViewHolder) viewHolder;
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
            scanProducts.setText(R.string.Snabble_Shoppingcart_EmptyState_restartButtonTitle);
        } else {
            restore.setVisibility(View.GONE);
            scanProducts.setText(R.string.Snabble_Shoppingcart_EmptyState_buttonTitle);
        }
    }

    private void update() {
        if (project != null) {
            updateEmptyState();
            scanForImages();
            checkSaleStop();
            checkDepositReturnVoucher();
        }
    }

    private void checkSaleStop() {
        List<Product> invalidProducts = cart.getInvalidProducts();

        if (invalidProducts.size() > 0 && !invalidProducts.equals(lastInvalidProducts)) {
            Resources res = getResources();
            StringBuilder sb = new StringBuilder();
            if (invalidProducts.size() == 1) {
                sb.append(res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_SaleStop_ErrorMsg_one)));
            } else {
                sb.append(res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_SaleStop_errorMsg)));
            }

            sb.append("\n\n");

            for (Product product : invalidProducts) {
                if (product.getSubtitle() != null) {
                    sb.append(product.getSubtitle());
                    sb.append(" ");
                }

                sb.append(product.getName());
                sb.append("\n");
            }

            new AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setTitle(I18nUtils.getIdentifier(getResources(), R.string.Snabble_SaleStop_ErrorMsg_title))
                    .setMessage(sb.toString())
                    .setPositiveButton(R.string.Snabble_ok, null)
                    .show();

            lastInvalidProducts = invalidProducts;
        }
    }

    private void checkDepositReturnVoucher() {
        if (cart.hasInvalidDepositReturnVoucher() && !hasAlreadyShownInvalidDeposit) {
            new AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setTitle(I18nUtils.getIdentifier(getResources(), R.string.Snabble_SaleStop_ErrorMsg_title))
                    .setMessage(I18nUtils.getIdentifier(getResources(), R.string.Snabble_InvalidDepositVoucher_errorMsg))
                    .setPositiveButton(R.string.Snabble_ok, null)
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
        if (!isRegistered && project != null) {
            isRegistered = true;
            cart.addListener(shoppingCartListener);
            submitList();
            update();
        }
    }

    private void unregisterListeners() {
        if (isRegistered) {
            isRegistered = false;
            cart.removeListener(shoppingCartListener);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isInEditMode()) {
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

    @Nullable
    private static String sanitize(@Nullable String input) {
        if (input != null && input.equals("")) return null;
        return input;
    }

    public static List<Row> buildRows(Resources resources, ShoppingCart cart) {
        List<Row> rows = new ArrayList<>(cart.size() + 1);

        for (int i = 0; i < cart.size(); i++) {
            ShoppingCart.Item item = cart.get(i);

            if (item.getType() == ShoppingCart.ItemType.LINE_ITEM) {
                if (item.isDiscount()) {
                    final SnabbleSimpleRow row = new SnabbleSimpleRow(
                            item,
                            false,
                            sanitize(item.getPriceText()),
                            resources.getString(R.string.Snabble_Shoppingcart_discounts),
                            R.drawable.snabble_ic_percent
                    );
                    rows.add(row);
                } else if (item.isGiveaway()) {
                    final SnabbleSimpleRow row = new SnabbleSimpleRow(
                            item,
                            false,
                            resources.getString(R.string.Snabble_Shoppingcart_giveaway),
                            item.getDisplayName(),
                            R.drawable.snabble_ic_gift
                    );
                    rows.add(row);
                }
            } else if (item.getType() == ShoppingCart.ItemType.COUPON) {
                final SnabbleSimpleRow row = new SnabbleSimpleRow(
                        item,
                        true,
                        item.getDisplayName(),
                        resources.getString(R.string.Snabble_Shoppingcart_coupon),
                        null
                );
                rows.add(row);
            } else if (item.getType() == ShoppingCart.ItemType.PRODUCT) {
                final Product product = item.getProduct();

                final ProductRow row = new ProductRow(
                        item,
                        true,
                        sanitize(item.getDisplayName()),
                        product != null ? sanitize(product.getSubtitle()) : null,
                        product != null ? sanitize(product.getImageUrl()) : null,
                        item.getUnit(),
                        sanitize(item.getTotalPriceText()),
                        sanitize(item.getQuantityText()),
                        item.getQuantity(),
                        item.isEditable(),
                        item.isManualCouponApplied()
                );
                rows.add(row);
            }
        }

        int cartTotal = cart.getTotalDepositPrice();
        if (cartTotal > 0) {
            PriceFormatter priceFormatter = Snabble.getInstance().getCheckedInProject().getValue().getPriceFormatter();
            final SnabbleSimpleRow row = new SnabbleSimpleRow(
                    null,
                    false,
                    priceFormatter.format(cartTotal),
                    resources.getString(R.string.Snabble_Shoppingcart_deposit),
                    R.drawable.snabble_ic_deposit
            );
            rows.add(row);
        }

        return rows;
    }

    private void submitList() {
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.submitList(buildRows(getResources(), cart), hasAnyImages);
        }
    }

    public static class ShoppingCartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements UndoHelper, GestureHandler.DismissibleAdapter {
        private static final int TYPE_PRODUCT = 0;
        private static final int TYPE_SIMPLE = 1;
        private List<Row> list = Collections.emptyList();
        private final Context context;
        private ShoppingCart cart;
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
            if (getItem(position) instanceof SnabbleSimpleRow) {
                return TYPE_SIMPLE;
            }

            return TYPE_PRODUCT;
        }

        @Override
        public boolean isDismissible(int position) {
            return getItem(position).isDismissible();
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

                    if (oldRow.getItem() == null || newRow.getItem() == null) {
                        return false;
                    }

                    return oldRow.getItem() == newRow.getItem();
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
                return new ShoppingCartItemViewHolder(v, this);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if (type == TYPE_PRODUCT) {
                ShoppingCartItemViewHolder viewHolder = (ShoppingCartItemViewHolder) holder;
                viewHolder.bindTo((ProductRow) getItem(position), hasAnyImages);
            } else {
                SimpleViewHolder viewHolder = (SimpleViewHolder) holder;
                viewHolder.update((SnabbleSimpleRow) getItem(position), hasAnyImages);
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
                fetchFrom(cart);
                Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.getProduct());
            });
            snackbar.show();
            fetchFrom(cart);
        }
    }
}
