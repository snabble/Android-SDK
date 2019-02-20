package io.snabble.sdk.ui.cart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

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

import com.squareup.picasso.Picasso;

import java.math.RoundingMode;
import java.util.List;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.ShoppingCart2;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class ShoppingCartView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private RecyclerView recyclerView;
    private Adapter recyclerViewAdapter;
    private ShoppingCart2 cart;
    private Checkout checkout;
    private PriceFormatter priceFormatter;
    private Button pay;
    private View coordinatorLayout;
    private ViewGroup emptyState;
    private Snackbar snackbar;
    private DelayedProgressDialog progressDialog;
    private boolean hasAnyImages;

    private ShoppingCart2.ShoppingCartListener shoppingCartListener = new ShoppingCart2.ShoppingCartListener() {
        @Override
        public void onItemAdded(ShoppingCart2 list, ShoppingCart2.Item  product) {
            onCartUpdated();
        }

        @Override
        public void onQuantityChanged(ShoppingCart2 list, ShoppingCart2.Item  product) {

        }

        @Override
        public void onCleared(ShoppingCart2 list) {
            onCartUpdated();
        }

        @Override
        public void onItemRemoved(ShoppingCart2 list, ShoppingCart2.Item item) {

        }

        @Override
        public void onUpdate(ShoppingCart2 list) {
            onCartUpdated();
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

        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new Adapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(itemDecoration);

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

        Project project = SnabbleUI.getProject();

        if (cart != null) {
            cart.removeListener(shoppingCartListener);
        }

        cart = project.getShoppingCart();
        checkout = project.getCheckout();
        priceFormatter = new PriceFormatter(project);

        updatePayText();
        updateEmptyState();
        scanForImages();
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
                if (recyclerView.getAdapter().getItemViewType(viewHolder.getAdapterPosition())
                        == Adapter.TYPE_DEPOSIT) {
                    return 0;
                }

                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                ViewHolder holder = (ViewHolder) viewHolder;
                holder.hideInput();

                final int pos = viewHolder.getAdapterPosition();

                ShoppingCart2.Item item = cart.get(pos);

                removeAndShowUndoSnackbar(pos, item);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void removeAndShowUndoSnackbar(final int pos, final ShoppingCart2.Item item) {
        cart.remove(pos);
        Telemetry.event(Telemetry.Event.DeletedFromCart, item.getProduct());
        recyclerViewAdapter.notifyItemRemoved(pos);
        update();

        snackbar = UIUtils.snackbar(coordinatorLayout,
                R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
        snackbar.setAction(R.string.Snabble_undo, new OnClickListener() {
            @Override
            public void onClick(View v) {
                cart.insert(pos, item.getProduct(), item.getScannedCode()).setQuantity(pos);
                recyclerView.getAdapter().notifyDataSetChanged();
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
                pay.setText(getResources().getQuantityString(R.plurals.Snabble_Shoppingcart_buyProducts,
                                quantity, quantity, formattedPrice));
            } else {
                pay.setText(R.string.Snabble_Shoppingcart_buyProducts_now);
            }
        }
    }

    private void onCartUpdated() {
        update();

        recyclerViewAdapter.notifyDataSetChanged();
    }

    private void update() {
        updatePayText();
        updateEmptyState();
        scanForImages();

        recyclerViewAdapter.updateDeposit();

        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private void scanForImages() {
        hasAnyImages = false;

        for (int i = 0; i < cart.size(); i++) {
            Product product = cart.get(i).getProduct();
            String url = product.getImageUrl();
            if (url != null && url.length() > 0) {
                hasAnyImages = true;
                break;
            }
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

        onCartUpdated();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        registerListeners();
                        onCartUpdated();
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
                    }
                }
            };

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
        public void bindTo(final int position) {
            final Project project = SnabbleUI.getProject();
            final ShoppingCart2.Item item = cart.get(position);

            final Product product = item.getProduct();
            final int quantity = item.getQuantity();

            if (product != null) {
                final ScannedCode scannedCode = item.getScannedCode();

                String encodingDisplayValue = "g";

                Unit encodingUnit = scannedCode.getEmbeddedUnit();
                if (encodingUnit == null) {
                    encodingUnit = product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());
                }

                if (encodingUnit != null) {
                    encodingDisplayValue = encodingUnit.getDisplayValue();
                }

                name.setText(product.getName());
                quantityAnnotation.setText(encodingDisplayValue);
                priceTextView.setText(item.getPriceText());

                quantityTextView.setText(item.getQuantityText());

                if (product.getSubtitle() == null || product.getSubtitle().equals("")) {
                    subtitle.setVisibility(View.GONE);
                } else {
                    subtitle.setText(product.getSubtitle());
                }

                if (item.isEditable()) {
                    if (item.getProduct().getType() == Product.Type.UserWeighed) {
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
                        item.setQuantity(item.getQuantity() + 1);
                        recyclerViewAdapter.notifyItemChanged(getAdapterPosition());
                        update();
                    }
                });

                minus.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int p = getAdapterPosition();

                        int newQuantity = item.getQuantity() - 1;
                        if (newQuantity <= 0) {
                            removeAndShowUndoSnackbar(p, item);
                        } else {
                            item.setQuantity(newQuantity);
                            recyclerViewAdapter.notifyItemChanged(getAdapterPosition());
                            update();
                        }
                    }
                });

                quantityEditApply.setOnClickListener(new OneShotClickListener() {
                    @Override
                    public void click() {
                        int pos = getAdapterPosition();
                        item.setQuantity(getQuantityEditValue());
                        recyclerViewAdapter.notifyItemChanged(pos);
                        update();

                        hideInput();
                    }
                });

                quantityEdit.setText(String.valueOf(quantity));
                itemView.setFocusable(true);
                itemView.setFocusableInTouchMode(true);

                if (position == 0) {
                    itemView.requestFocus();
                }

                quantityEdit.removeTextChangedListener(textWatcher);
                textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        updateQuantityEditApplyVisibility(quantity);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                };

                updateQuantityEditApplyVisibility(quantity);

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

                String imageUrl = product.getImageUrl();
                if (imageUrl != null && !imageUrl.equals("")) {
                    image.setVisibility(View.VISIBLE);
                    Picasso.with(getContext()).load(imageUrl).into(image);
                } else {
                    image.setVisibility(hasAnyImages ? View.INVISIBLE : View.GONE);
                    image.setImageBitmap(null);
                }
            } else {
                itemView.setVisibility(View.GONE);
            }
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

        public void update() {
            PriceFormatter priceFormatter = new PriceFormatter(SnabbleUI.getProject());
            String depositText = priceFormatter.format(cart.getTotalDepositPrice());
            deposit.setText(depositText);
        }
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public static final int TYPE_ITEM = 0;
        public static final int TYPE_DEPOSIT = 1;

        @Override
        public int getItemViewType(int position) {
            if (cart.getTotalDepositPrice() > 0 && position == getItemCount() - 1) {
                return TYPE_DEPOSIT;
            }

            return TYPE_ITEM;
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
            if (getItemViewType(position) == TYPE_DEPOSIT) {
                DepositViewHolder viewHolder = (DepositViewHolder) holder;
                viewHolder.update();
            } else {
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.bindTo(position);
            }
        }

        public void updateDeposit() {
            for (int i = 0; i < getItemCount(); i++) {
                if (getItemViewType(i) == TYPE_DEPOSIT) {
                    notifyItemChanged(i);
                }
            }
        }

        @Override
        public int getItemCount() {
            if (cart.getTotalDepositPrice() > 0) {
                return cart.size() + 1;
            }

            return cart.size();
        }
    }
}
